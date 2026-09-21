package org.solync.idealync.modules

import dev.kord.common.Color
import dev.kord.common.entity.GuildScheduledEventPrivacyLevel
import dev.kord.common.entity.ScheduledEntityType
import dev.kord.common.entity.TextInputStyle
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.createScheduledEvent
import dev.kord.core.behavior.interaction.modal
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.core.event.interaction.GuildModalSubmitInteractionCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.message.embed
import org.solync.idealync.IdeaLyncModule
import org.solync.idealync.ideaLyncConfig
import org.solync.idealync.utils.requireNotNull
import org.solync.idealync.utils.url
import kotlin.time.Instant

object MeetingScheduler : IdeaLyncModule {
    private const val IDEALYNC_SCHEDULE_MEETING_MODAL_ID = "idealync:schedule_meeting_modal"
    private const val IDEALYNC_SCHEDULE_MEETING_TITLE_ARG = "idealync:schedule_meeting_title"
    private const val IDEALYNC_SCHEDULE_MEETING_DESCRIPTION_ARG = "idealync:schedule_meeting_description"
    private const val IDEALYNC_SCHEDULE_MEETING_USERS_ARG = "idealync:schedule_meeting_users"
    private const val IDEALYNC_SCHEDULE_MEETING_TIME_ARG = "idealync:schedule_meeting_time"

    override suspend fun onRegister(kord: Kord) {
        kord.createGlobalChatInputCommand("schedule", "Schedule a meeting")
        kord.on<ChatInputCommandInteractionCreateEvent> {
            if (interaction.command.rootName == "schedule") {
                scheduleCmdCallback()
            }
        }
        kord.on<GuildModalSubmitInteractionCreateEvent> {
            if (interaction.modalId == IDEALYNC_SCHEDULE_MEETING_MODAL_ID) {
                scheduleModalSubmit()
            }
        }
    }

    private suspend fun ChatInputCommandInteractionCreateEvent.scheduleCmdCallback() {
        interaction.modal("Schedule a meeting...", IDEALYNC_SCHEDULE_MEETING_MODAL_ID) {
            label("Title") {
                textInput(TextInputStyle.Short, IDEALYNC_SCHEDULE_MEETING_TITLE_ARG) {
                    allowedLength = 1..4000
                }
            }
            label("Description") {
                textInput(TextInputStyle.Paragraph, IDEALYNC_SCHEDULE_MEETING_DESCRIPTION_ARG) {
                    allowedLength = 1..4000
                }
            }
            label("Who's attending?") {
                userSelect(IDEALYNC_SCHEDULE_MEETING_USERS_ARG) {
                    allowedValues = 2..25
                }
            }
            label("Timestamp (ISO 8601)") {
                textInput(TextInputStyle.Short, IDEALYNC_SCHEDULE_MEETING_TIME_ARG) {
                    allowedLength = 1..100
                }
            }
        }
    }

    private suspend fun GuildModalSubmitInteractionCreateEvent.scheduleModalSubmit() {
        val response = interaction.deferEphemeralResponse()

        val title = response.requireNotNull(interaction.textInputs[IDEALYNC_SCHEDULE_MEETING_TITLE_ARG]?.value) { "Title is required for a meeting" }
        val description = response.requireNotNull(interaction.textInputs[IDEALYNC_SCHEDULE_MEETING_DESCRIPTION_ARG]?.value) { "Description is required for a meeting" }
        val users = response.requireNotNull(interaction.userSelects[IDEALYNC_SCHEDULE_MEETING_USERS_ARG]?.valueIds) { "Attendees are required for a meeting" }
        val timestampStr = response.requireNotNull(interaction.textInputs[IDEALYNC_SCHEDULE_MEETING_TIME_ARG]?.value) { "Timestamp is required for a meeting" }

        val instant = try {
            Instant.parse(timestampStr)
        } catch (_: IllegalArgumentException) {
            response.respond { content = "couldn't parse timestamp, did you spell something wrong?" }
            return
        }
        val attendeePings = users.joinToString(", ") { "<@$it>" }

        val event = try {
            interaction.guild.createScheduledEvent(
                title,
                GuildScheduledEventPrivacyLevel.GuildOnly,
                instant,
                ScheduledEntityType.Voice
            ) {
                channelId = ideaLyncConfig.meetingVoiceChannelId
                this.description = "$description\n\nAttending: $attendeePings"
            }
        } catch (_: Throwable) {
            response.respond { content = "bot doesn't have permissions to create an event" }
            return
        }

        val announceChannel = kord.getChannel(ideaLyncConfig.meetingAnnounceId)
        require(announceChannel is TextChannel) { "Meeting announce channel id is invalid" }

        announceChannel.createMessage {
            content = attendeePings
            embed {
                this.title = "Meeting scheduled: $title"
                this.description = description
                color = Color(0xF36647)
                url = event.url

                field("Time", value = { "<t:${instant.epochSeconds}:F> (<t:${instant.epochSeconds}:R>)  " })
                field("Location", value = { "<#${ideaLyncConfig.meetingVoiceChannelId}>" })
                field("Attendees", value = { attendeePings })
            }
        }

        response.respond { content = "Event $title has been created" }
    }
}
