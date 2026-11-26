package org.solync.idealync.modules

import dev.kord.common.Color
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.entity.channel.TextChannel
import dev.kord.rest.builder.message.embed
import io.github.cdimascio.dotenv.dotenv
import jakarta.mail.Flags
import jakarta.mail.Folder
import jakarta.mail.Message
import jakarta.mail.Multipart
import jakarta.mail.Part
import jakarta.mail.Session
import jakarta.mail.internet.InternetAddress
import jakarta.mail.search.FlagTerm
import io.ktor.client.request.forms.ChannelProvider
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jsoup.Jsoup
import org.solync.idealync.IdeaLyncModule
import java.io.File
import java.security.MessageDigest
import java.util.Properties

/**
 * imap-to-discord bridge: polls the mailbox in imap_email over imaps and posts every new
 * mail to the channel in mail_channel_id as an embed
 *
 * junk is polled alongside inbox on purpose: rspamd files spam there instead of rejecting
 * it at smtp time, and this bridge forwards all mail, spam included
 *
 * config comes from env (see .env.example): imap_host, imap_port, imap_email,
 * imap_password (a mailcow app password, never the real login), mail_channel_id
 */
object MailBridge : IdeaLyncModule {
    /** message-ids already posted, persists across restarts */
    private val sentFile = File("data/sent_mail.txt")
    private val orange = Color(0xF36647)

    /**
     * starts the poll loop once the gateway is ready. returns early when any required env
     * var is missing so the rest of the bot still boots without mail configured
     *
     * @param kord the kord client, used to resolve the target channel
     */
    override suspend fun onReady(kord: Kord) {
        val env = dotenv { ignoreIfMissing = true }
        fun e(k: String) = env[k] ?: System.getenv(k)
        val host = e("IMAP_HOST") ?: return
        val port = e("IMAP_PORT") ?: "993"
        val email = e("IMAP_EMAIL") ?: return
        val pass = e("IMAP_PASSWORD") ?: return
        val channelId = e("MAIL_CHANNEL_ID")?.toLongOrNull() ?: return

        CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                try {
                    poll(kord, host, port, email, pass, channelId)
                } catch (t: Throwable) {
                    println("mail poll failed: $t")
                }
                delay(20_000)
            }
        }
    }

    /**
     * does one poll pass over inbox and junk
     *
     * reconnects on every call because long-lived imap connections drop silently; the loop
     * in onReady just invokes this again. already-posted mails are skipped via the
     * message-id file plus the seen flag, so a crash between posting and marking the mail
     * seen cannot double-post
     *
     * @param kord the kord client
     * @param host imap host, e.g. mx.solync.org
     * @param port imap port, 993 unless overridden
     * @param email mailbox address, also rendered as the to: line in the embed
     * @param pass mailcow app password for the mailbox
     * @param channelId discord channel id to post into
     */
    private suspend fun poll(kord: Kord, host: String, port: String, email: String, pass: String, channelId: Long) {
        val props = Properties().apply {
            put("mail.store.protocol", "imaps")
            put("mail.imaps.host", host)
            put("mail.imaps.port", port)
            put("mail.imaps.ssl.trust", host)
        }
        val store = Session.getInstance(props).getStore("imaps")
        store.connect(host, email, pass)
        try {
            for (name in listOf("INBOX", "Junk")) {
                val folder = store.getFolder(name)
                folder.open(Folder.READ_WRITE)
                try {
                    val sent = if (sentFile.exists()) sentFile.readLines().toSet() else emptySet()
                    for (m in folder.search(FlagTerm(Flags(Flags.Flag.SEEN), false))) {
                        val id = m.getHeader("Message-ID")?.firstOrNull() ?: "${m.sentDate}-${m.subject}"
                        val key = MessageDigest.getInstance("SHA-256").digest(id.toByteArray()).joinToString("") { "%02x".format(it) }
                        if (key !in sent) {
                            post(kord, channelId, email, m)
                            sentFile.parentFile?.mkdirs()
                            sentFile.appendText("$key\n")
                        }
                        m.setFlag(Flags.Flag.SEEN, true)
                    }
                } finally {
                    folder.close()
                }
            }
        } finally {
            store.close()
        }
    }

    /**
     * posts one mail to discord as a single embed with the fixed layout
     * from: / to: / body: lines, the mail's attachments appended as files, and
     * original.html attached when the mail contained html
     *
     * @param kord the kord client
     * @param channelId discord channel id to post into
     * @param email mailbox address used for the to: line, read from env not hardcoded
     * @param m the mime message to post
     */
    private suspend fun post(kord: Kord, channelId: Long, email: String, m: Message) {
        val channel = kord.getChannelOf<TextChannel>(Snowflake(channelId))
        if (channel == null) {
            println("mail channel $channelId not found or not a text channel")
            return
        }
        val (body, html, files) = parse(m)
        val from = (m.from?.firstOrNull() as? InternetAddress)?.address ?: "unknown"
        channel.createMessage {
            embed {
                title = m.subject?.take(256)
                color = orange
                description = sanitize(buildString {
                    appendLine("from: $from")
                    appendLine("to: $email")
                    appendLine("body:")
                    append(body)
                    if (html != null) append("\n-# email contained html. original html is in the attachment below")
                }).take(4096)
            }
            files.forEach { (n, b) -> addFile(n, ChannelProvider(b.size.toLong()) { ByteReadChannel(b) }) }
            if (html != null) addFile("original.html", ChannelProvider(html.size.toLong()) { ByteReadChannel(html) })
        }
    }

    /**
     * walks the mime tree and extracts the display body, the raw html and the attachments
     * prefers text/plain and only falls back to jsoup-stripped html when no plain part
     * exists. attachments are capped at 9 because discord allows 10 files per message and
     * original.html takes the last slot
     *
     * @param m the mime message to dissect
     * @return triple of (body text, raw html bytes or null when the mail was plain only,
     * attachment name + bytes pairs)
     */
    private fun parse(m: Message): Triple<String, ByteArray?, List<Pair<String, ByteArray>>> {
        var text: String? = null
        var html: ByteArray? = null
        val files = mutableListOf<Pair<String, ByteArray>>()
        fun walk(p: Part) {
            when {
                p.isMimeType("text/plain") && p.disposition != Part.ATTACHMENT && p.fileName == null -> text = readCapped(p.inputStream)?.toString(Charsets.UTF_8)
                p.isMimeType("text/html") -> html = readCapped(p.inputStream)
                p.disposition == Part.ATTACHMENT || p.fileName != null -> readCapped(p.inputStream)?.let { if (!isBinary(p.fileName ?: "attachment.bin", it)) files += (p.fileName ?: "attachment.bin") to it }
                p.content is Multipart -> {
                    val mp = p.content as Multipart
                    for (i in 0 until mp.count) walk(mp.getBodyPart(i))
                }
            }
        }
        walk(m)
        val body = text ?: html?.let { htmlToText(String(it)) } ?: ""
        return Triple(body, html, files.take(9))
    }

    private fun readCapped(s: java.io.InputStream, max: Int = 24 * 1024 * 1024): ByteArray? {
        val b = s.readNBytes(max + 1)
        return if (b.size > max) null else b
    }

    private fun isBinary(name: String, b: ByteArray): Boolean {
        val ext = name.substringAfterLast('.', "").lowercase()
        if (ext in setOf("exe", "elf", "so", "dll", "bin", "msi", "bat", "cmd", "sh", "jar", "class", "deb", "rpm", "dmg", "apk", "vbs", "ps1", "scr")) return true
        return b.size >= 4 && ((b[0] == 0x7F.toByte() && b[1] == 69.toByte() && b[2] == 76.toByte() && b[3] == 70.toByte()) || (b[0] == 77.toByte() && b[1] == 90.toByte()))
    }

    private fun htmlToText(raw: String): String {
        val s = raw
            .replace(Regex("(?i)<br\\s*/?>"), "\n")
            .replace(Regex("(?i)</(p|div|h[1-6]|li|tr|blockquote)>"), "\n\n")
            .replace(Regex("(?i)<(b|strong)>"), "**")
            .replace(Regex("(?i)</(b|strong)>"), "**")
        return Jsoup.parse(s).wholeText().replace(Regex("\n{3,}"), "\n\n").trim()
    }

    /**
     * makes untrusted mail content safe to paste into discord: strips control chars and
     * defuses @everyone/@here so a mail cannot ping the whole server
     *
     * @param s raw text
     * @return sanitized text
     */
    private fun sanitize(s: String) = s.replace(Regex("[\\p{Cc}]"), "")
        .replace("@everyone", "@\u200beveryone")
        .replace("@here", "@\u200bhere")
}
