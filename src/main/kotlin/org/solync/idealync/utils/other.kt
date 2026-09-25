package org.solync.idealync.utils

import dev.kord.core.behavior.channel.TextChannelBehavior
import dev.kord.core.entity.GuildScheduledEvent
import kotlinx.coroutines.flow.any
import kotlinx.coroutines.flow.take

val GuildScheduledEvent.url get() = "https://discord.com/events/${guildId}/${id}"
