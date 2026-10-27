package org.solync.idealync.modules

import dev.kord.common.Color
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.entity.channel.TextChannel
import dev.kord.rest.builder.message.embed
import io.github.cdimascio.dotenv.dotenv
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.solync.idealync.IdeaLyncModule
import java.io.File
import java.net.URI

object PeerFeedbackNotifier : IdeaLyncModule {
    private val cacheFile = File("data/sent_peerfeedback.txt")

    override suspend fun onReady(kord: Kord) {
        val env = dotenv { ignoreIfMissing = true }
        val channelIdRaw = env["PEERFEEDBACK_CHANNEL_ID"] ?: System.getenv("PEERFEEDBACK_CHANNEL_ID") ?: return
        val channelId = channelIdRaw.toLongOrNull() ?: return
        val endpointRaw = env["PEERFEEDBACK_API_URL"] ?: System.getenv("PEERFEEDBACK_API_URL") ?: return
        val endpoint = endpointRaw.trimEnd('/')

        CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                try {
                    val conn = URI.create("$endpoint/api/session/status").toURL().openConnection()
                    conn.connectTimeout = 10_000
                    conn.readTimeout = 10_000
                    val raw = conn.getInputStream().bufferedReader().use { it.readText() }
                    val obj = Json.parseToJsonElement(raw).jsonObject
                    val status = obj["status"]?.jsonPrimitive?.contentOrNull
                    val sessionId = obj["sessionId"]?.jsonPrimitive?.contentOrNull
                    val summary = obj["summary"]?.jsonPrimitive?.contentOrNull
                    val sentIds = if (cacheFile.exists()) cacheFile.readLines().map { it.trim() }.filter { it.isNotEmpty() }.toSet() else emptySet()
                    if (status == "ended" && sessionId != null && summary != null && sessionId !in sentIds) {
                        val channel = kord.getChannelOf<TextChannel>(Snowflake(channelId))
                        if (channel != null) {
                            channel.createMessage {
                                embed {
                                    description = summary
                                    color = Color(0xF36647)
                                }
                            }
                            cacheFile.parentFile?.mkdirs()
                            cacheFile.appendText("$sessionId\n")
                        }
                    }
                } catch (_: Throwable) {
                }
                delay(30_000)
            }
        }
    }
}
