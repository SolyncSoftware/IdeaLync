package org.solync.idealync.modules

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.channel.ForumChannel
import dev.kord.core.entity.channel.thread.ThreadChannel
import dev.kord.core.event.interaction.ButtonInteractionCreateEvent
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.channel.thread.applyTag
import dev.kord.rest.builder.component.actionRow
import dev.kord.rest.builder.interaction.string
import org.solync.idealync.IdeaLyncModule
import org.solync.idealync.ideaLyncConfig
import java.util.Optional
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

object PitchForwarding : IdeaLyncModule {
    private const val IDEALYNC_FORWARD_CONFIRM_ID = "idealync:forward_confirm"
    private const val IDEALYNC_FORWARD_CANCEL_ID = "idealync:forward_cancel"

    private val addedMsgCache: Cache<Snowflake, Optional<String>> = CacheBuilder.newBuilder()
        .maximumSize(10)
        .expireAfterAccess(1.minutes.toJavaDuration())
        .build()

    private fun getAddedMsg(interactionId: Snowflake): Optional<String>? {
        val res = addedMsgCache.getIfPresent(interactionId)
        addedMsgCache.invalidate(interactionId)
        return res
    }

    override suspend fun onRegister(kord: Kord) {
        kord.createGlobalChatInputCommand("forward", "Forward a pitch from the pitching board to the project board.") {
            string("added_message", "added_message") {
                required = false
            }
        }
        kord.on<ChatInputCommandInteractionCreateEvent> {
            if (interaction.command.rootName == "forward") {
                forwardCmdCallback()
            }
        }
        kord.on<ButtonInteractionCreateEvent> {
            when (interaction.component.customId) {
                IDEALYNC_FORWARD_CONFIRM_ID -> forwardConfirmed()
                IDEALYNC_FORWARD_CANCEL_ID -> forwardCancelled()
            }
        }
    }

    private suspend fun ChatInputCommandInteractionCreateEvent.forwardCmdCallback() {
        val channel = interaction.channel.asChannel()
        if (channel !is ThreadChannel || channel.parentId != ideaLyncConfig.pitchingBoardForumId) {
            interaction.respondEphemeral {
                content = "Please run this command in the pitching board!"
            }
            return
        }

        addedMsgCache.put(interaction.id, Optional.ofNullable(interaction.command.strings["added_message"]))

        interaction.respondEphemeral {
            content = "Are you sure you want to forward this pitch to the project board?"
            actionRow {
                interactionButton(ButtonStyle.Success, IDEALYNC_FORWARD_CONFIRM_ID) {
                    label = "Confirm"
                }
                interactionButton(ButtonStyle.Danger, IDEALYNC_FORWARD_CANCEL_ID) {
                    label = "Cancel"
                }
            }
        }
    }

    private suspend fun ButtonInteractionCreateEvent.forwardConfirmed() {
        val deferredResponse = interaction.deferEphemeralResponse()
        val channel = interaction.channel.asChannel() as ThreadChannel
        val rootMsg = channel.getMessage(channel.id)

        val board = requireNotNull(kord.getChannel(ideaLyncConfig.projectBoardForumId)) { "Project board channel id is invalid" }
        require(board is ForumChannel) { "Project board channel needs to be a forum." }

        val addedMsg = interaction.message.interaction?.id?.let(::getAddedMsg)
        if (addedMsg == null) {
            deferredResponse.respond { content = "Timed out." }
            return
        }

        val newThread = board.startPublicThread(channel.name) {
            message(buildString {
                appendLine(rootMsg.content)
                addedMsg.ifPresent { appendLine(it) }
                appendLine()
                append("\\- Forwarded by IdeaLync.")
            })
            applyTag(ideaLyncConfig.helpWantedTagId)
            applyTag(ideaLyncConfig.pendingTagId)
        }
        deferredResponse.respond { content = "Forwarded your pitch to ${newThread.mention}" }
    }

    private suspend fun ButtonInteractionCreateEvent.forwardCancelled() {
        interaction.respondEphemeral {
            content = "nevermind..."
        }
    }
}