package org.solync.idealync.modules

import dev.kord.common.Color
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.entity.interaction.SubCommand
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.interaction.subCommand
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
import java.net.HttpURLConnection
import java.net.URI

object PeerFeedbackNotifier : IdeaLyncModule {
    private const val GUILD_ID = 1487558897513398427L
    private const val ROLE_ID = 1487558897513398432L
    private val cacheFile = File("data/sent_peerfeedback.txt")

    override suspend fun onRegister(kord: Kord) {
        val guildSnowflake = Snowflake(GUILD_ID)
        val roleSnowflake = Snowflake(ROLE_ID)

        kord.createGuildChatInputCommand(guildSnowflake, "round", "Peer feedback round management") {
            subCommand("start", "Start a feedback round")
            subCommand("end", "End the active feedback round")
        }

        kord.on<GuildChatInputCommandInteractionCreateEvent> {
            if (interaction.command.rootName != "round") return@on
            if (interaction.guildId != guildSnowflake) return@on

            val response = interaction.deferEphemeralResponse()
            val member = interaction.user.asMember(guildSnowflake)
            if (!member.roleIds.contains(roleSnowflake)) {
                response.respond { content = "missing permissions" }
                return@on
            }

            val subName = (interaction.command as? SubCommand)?.name ?: return@on
            val action = if (subName == "end") "end" else "start"
            val env = dotenv { ignoreIfMissing = true }
            val endpoint = (env["PEERFEEDBACK_API_URL"] ?: System.getenv("PEERFEEDBACK_API_URL") ?: "http://localhost:5173").trimEnd('/')

            try {
                val conn = URI.create("$endpoint/api/session/$action").toURL().openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.connectTimeout = 10_000
                conn.readTimeout = 10_000
                val raw = conn.inputStream.bufferedReader().use { it.readText() }
                val obj = Json.parseToJsonElement(raw).jsonObject
                val sessionId = obj["session"]?.jsonObject?.get("id")?.jsonPrimitive?.contentOrNull
                if (sessionId != null) {
                    val label = if (action == "start") "started" else "ended"
                    response.respond { content = "round $label. session id: $sessionId" }
                } else {
                    response.respond { content = "round $action failed" }
                }
            } catch (_: Throwable) {
                response.respond { content = "round $action failed" }
            }
        }
    }

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
