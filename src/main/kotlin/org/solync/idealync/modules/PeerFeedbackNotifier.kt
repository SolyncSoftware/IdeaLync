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
import org.solync.idealync.ideaLyncConfig
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

object PeerFeedbackNotifier : IdeaLyncModule {
    private val cacheFile = File("data/sent_peerfeedback.txt")
    private val httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()

    override suspend fun onRegister(kord: Kord) {
        val guildSnowflake = ideaLyncConfig.guildId
        val roleSnowflake = ideaLyncConfig.memberRoleId

        if (guildSnowflake.value != 0uL) {
            kord.createGuildChatInputCommand(guildSnowflake, "round", "Peer feedback round management") {
                subCommand("start", "Start a feedback round")
                subCommand("end", "End the active feedback round")
            }
        }

        kord.on<GuildChatInputCommandInteractionCreateEvent> {
            if (interaction.command.rootName != "round") return@on
            if (guildSnowflake.value != 0uL && interaction.guildId != guildSnowflake) return@on

            val response = interaction.deferEphemeralResponse()
            val member = interaction.user.asMember(interaction.guildId)
            if (!member.roleIds.contains(roleSnowflake)) {
                response.respond { content = "missing permissions" }
                return@on
            }

            val subName = (interaction.command as? SubCommand)?.name ?: return@on
            val action = if (subName == "end") "end" else "start"
            val env = dotenv { ignoreIfMissing = true }
            val endpoint = (env["PEERFEEDBACK_API_URL"] ?: System.getenv("PEERFEEDBACK_API_URL"))?.trimEnd('/') ?: run {
                response.respond { content = "round $action failed: PEERFEEDBACK_API_URL missing" }
                return@on
            }

            try {
                val req = HttpRequest.newBuilder(URI.create("$endpoint/api/session/$action"))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .timeout(Duration.ofSeconds(10))
                    .header("User-Agent", "IdeaLync/1.0")
                    .header("Accept", "application/json")
                    .build()
                val res = httpClient.send(req, HttpResponse.BodyHandlers.ofString())
                val raw = res.body()
                val obj = runCatching { Json.parseToJsonElement(raw).jsonObject }.getOrNull()
                val sessionId = obj?.get("session")?.jsonObject?.get("id")?.jsonPrimitive?.contentOrNull
                val errorMsg = obj?.get("error")?.jsonPrimitive?.contentOrNull

                if (sessionId != null) {
                    val label = if (action == "start") "started" else "ended"
                    response.respond { content = "round $label. session id: $sessionId" }
                } else {
                    val detail = errorMsg ?: if (raw.isNotBlank()) raw.take(150) else "HTTP ${res.statusCode()}"
                    response.respond { content = "round $action failed: $detail" }
                }
            } catch (t: Throwable) {
                response.respond { content = "round $action failed: ${t.message}" }
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
                    val req = HttpRequest.newBuilder(URI.create("$endpoint/api/session/status"))
                        .GET()
                        .timeout(Duration.ofSeconds(10))
                        .header("User-Agent", "IdeaLync/1.0")
                        .header("Accept", "application/json")
                        .build()
                    val res = httpClient.send(req, HttpResponse.BodyHandlers.ofString())
                    val obj = Json.parseToJsonElement(res.body()).jsonObject
                    val status = obj["status"]?.jsonPrimitive?.contentOrNull
                    val sessionId = obj["sessionId"]?.jsonPrimitive?.contentOrNull
                    val summary = obj["summary"]?.jsonPrimitive?.contentOrNull
                    val sentIds = if (cacheFile.exists()) cacheFile.readLines().map { it.trim() }.filter { it.isNotEmpty() }.toSet() else emptySet()
                    if (status == "ended" && sessionId != null && summary != null && sessionId !in sentIds) {
                        val channel = kord.getChannelOf<TextChannel>(Snowflake(channelId))
                        if (channel != null) {
                            channel.createMessage {
                                embed {
                                    description = "$summary\n-# check results in peerfeedback [here]($endpoint/feedback/$sessionId)"
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
