package org.solync.idealync.modules

import dev.kord.core.Kord
import dev.kord.core.event.guild.MemberJoinEvent
import dev.kord.core.event.guild.MemberUpdateEvent
import dev.kord.core.on
import org.slf4j.LoggerFactory
import org.solync.idealync.IdeaLyncModule

object MemberEventLogger : IdeaLyncModule {
    private val LOGGER = LoggerFactory.getLogger(MemberEventLogger.javaClass)

    override suspend fun onRegister(kord: Kord) {
        kord.on<MemberJoinEvent> {
            LOGGER.info("Member {} joined", member.username)
        }
        kord.on<MemberUpdateEvent> {
            LOGGER.info("Member {} updated roles", member.username)
        }
    }
}