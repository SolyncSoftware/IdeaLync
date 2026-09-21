package org.solync.idealync.cogs

import dev.kord.common.Color
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.event.interaction.GuildSelectMenuInteractionCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.component.actionRow
import dev.kord.rest.builder.component.option
import dev.kord.rest.builder.message.embed
import org.solync.idealync.IdeaLyncModule
import org.solync.idealync.hasSelfEmbed
import org.solync.idealync.ideaLyncConfig

object RoleSelection : IdeaLyncModule {
    override suspend fun onRegister(kord: Kord) {
        kord.on<GuildSelectMenuInteractionCreateEvent> {
            if (interaction.componentId != "idealync:role_select") return@on

            val response = interaction.deferEphemeralResponse()
            val value = interaction.values[0]
            val addRoleId = if (value == "member") ideaLyncConfig.memberRoleId else ideaLyncConfig.observerRoleId
            val removeRoleId = if (value == "member") ideaLyncConfig.observerRoleId else ideaLyncConfig.memberRoleId

            val addRole = interaction.guild.getRoleOrNull(addRoleId)
            if (addRole == null) {
                interaction.respondEphemeral {
                    content = "That role is not available right now."
                }
                return@on
            }

            if (!interaction.user.roleIds.contains(addRoleId)) {
                interaction.user.addRole(addRoleId, "Selected role from prompt")
            }
            if (interaction.user.roleIds.contains(removeRoleId)) {
                interaction.user.removeRole(removeRoleId, "Switched role selection")
            }

            response.respond {
                content = "You are now set as ${addRole.name}."
            }
        }
    }

    override suspend fun onReady(kord: Kord) {
        ensureRolePrompt(kord)
    }

    private suspend fun ensureRolePrompt(kord: Kord) {
        val channel = requireNotNull(kord.getChannel(ideaLyncConfig.roleChannelId)) { "Role channel does not exist" }
        require(channel is TextChannel) { "Role channel is not a text channel." }

        if (channel.hasSelfEmbed("Choose your role")) {
            return
        }

        channel.createMessage {
            embed {
                title = "Choose your role"
                description="Use the dropdown below to switch between Member and Observer/Inactive."
                color = Color(0xF36647)
            }
            actionRow {
                stringSelect("idealync:role_select") {
                    option("Member", "member") {
                        description = "Members are also known as active contributors"
                    }
                    option("Observer/Inactive", "observer") {
                        description = "Observer/Inactive are Members who are inactive for 1+ months and are unable to contribute for now"
                    }
                }
            }
        }
    }
}