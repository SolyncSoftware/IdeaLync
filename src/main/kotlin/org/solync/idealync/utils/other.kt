package org.solync.idealync.utils

import dev.kord.core.behavior.channel.TextChannelBehavior
import dev.kord.core.entity.GuildScheduledEvent
import kotlinx.coroutines.flow.any
import kotlinx.coroutines.flow.take

suspend fun TextChannelBehavior.hasSelfEmbed(title: String): Boolean =
    messages
        .take(20)
        .any { msg ->
            msg.author?.isSelf == true
                    && msg.embeds.any { it.title == title }
        }

val GuildScheduledEvent.url get() = "https://discord.com/events/${guildId}/${id}"
