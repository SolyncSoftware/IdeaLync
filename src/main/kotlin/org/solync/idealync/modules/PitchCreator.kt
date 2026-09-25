package org.solync.idealync.modules

import dev.kord.common.Color
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.TextInputStyle
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.interaction.modal
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.channel.ForumChannel
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.event.interaction.ButtonInteractionCreateEvent
import dev.kord.core.event.interaction.ModalSubmitInteractionCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.channel.thread.applyTag
import dev.kord.rest.builder.component.actionRow
import dev.kord.rest.builder.message.embed
import kotlinx.coroutines.flow.filter
import org.solync.idealync.IdeaLyncModule
import org.solync.idealync.ideaLyncConfig


object PitchCreator : IdeaLyncModule {
    private const val IDEALYNC_SUBMIT_PITCH_BUTTON_ID = "idealync:submit_pitch_button"
    private const val IDEALYNC_SUBMIT_PITCH_MODAL_ID = "idealync:submit_pitch_modal"

    private const val IDEALYNC_PITCH_TITLE_ID = "idealync:pitch_title"
    private const val IDEALYNC_PITCH_DESCRIPTION_ID = "idealync:pitch_description"
    private const val IDEALYNC_PITCH_MEMBERS_ID = "idealync:pitch_members"

    override suspend fun onRegister(kord: Kord) {
        kord.on<ButtonInteractionCreateEvent> {
            if (interaction.componentId == IDEALYNC_SUBMIT_PITCH_BUTTON_ID) {
                submitButtonCallback()
            }
        }
        kord.on<ModalSubmitInteractionCreateEvent> {
            if (interaction.modalId == IDEALYNC_SUBMIT_PITCH_MODAL_ID) {
                modalCallback()
            }
        }
    }

    override suspend fun onReady(kord: Kord) {
        ensurePitchPrompt(kord)
    }

    private suspend fun ensurePitchPrompt(kord: Kord) {
        val channel = requireNotNull(kord.getChannel(ideaLyncConfig.pitchingChannelId)) { "Pitching channel does not exist" }
        require(channel is TextChannel) { "Pitching channel is not a text channel." }
        channel.messages
            .filter { it.author?.isSelf == true }
            .collect { it.delete() }

        channel.createMessage {
            embed {
                title = "Pitch an idea!"
                description = """
                    Add a title and description. Ping members to see if they're interested. Projects we want to move on can move into the project channel using `/forward`. *(think of pitching threads as preproduction, idea space)*

                    **remember to think of this:**
                    - What would this solve?
                    - Why now?
                    - Will anything change if we don't follow through?
                    - Scope of project
                    - Will it block or help with other work?
                """.trimIndent()
                color = Color(0xF36647)
            }

            actionRow {
                interactionButton(ButtonStyle.Primary, IDEALYNC_SUBMIT_PITCH_BUTTON_ID) {
                    label = "Submit Pitch"
                }
            }
        }
    }

    private suspend fun ButtonInteractionCreateEvent.submitButtonCallback() {
        interaction.modal("Submit a Pitch", IDEALYNC_SUBMIT_PITCH_MODAL_ID) {
            label("Title") {
                textInput(TextInputStyle.Short, IDEALYNC_PITCH_TITLE_ID) {
                    allowedLength = 1..45
                    placeholder = "Title for your idea"
                }
            }
            label("Description") {
                textInput(TextInputStyle.Paragraph, IDEALYNC_PITCH_DESCRIPTION_ID) {
                    allowedLength = 1..4000
                    placeholder = "Explain the idea, problem solved, scope, and impact..."
                }
            }
            label("Recruit members (this will ping them)") {
                userSelect(IDEALYNC_PITCH_MEMBERS_ID) {
                    placeholder = "check members to ping for feedback"
                    // cant have minimum of zero for some reason
                    allowedValues = 1..25
                }
            }
        }
    }

    private suspend fun ModalSubmitInteractionCreateEvent.modalCallback() {
        val response = interaction.deferEphemeralResponse()
        val channel = kord.getChannel(ideaLyncConfig.pitchingBoardForumId)
        require(channel is ForumChannel) { "Configured pitching board forum channel is invalid." }

        val title = requireNotNull(interaction.textInputs[IDEALYNC_PITCH_TITLE_ID]?.value) { "Pitching title was null" }
        val description = requireNotNull(interaction.textInputs[IDEALYNC_PITCH_DESCRIPTION_ID]?.value) { "Pitching description was null" }
        val members = interaction.userSelects[IDEALYNC_PITCH_MEMBERS_ID]?.valueIds

        val thread = channel.startPublicThread(title) {
            message("""
                **Submitted by:** ${interaction.user.mention}
                
                ### Description
                $description
                
                Remember to use `/forward` once you're done brainstorming!
            """.trimIndent())
            applyTag(ideaLyncConfig.brainstormingTagId)
        }

        if (!members.isNullOrEmpty()) {
            val mentions = members.map { id ->
                kord.getUser(id)!!
                    .mention
            }.joinToString(separator = " ")
            thread.createMessage("You have been recruited for this pitch!\n$mentions")
        }

        response.respond {
            content = "Your pitch **$title** was posted to ${thread.mention}"
        }
    }
}
