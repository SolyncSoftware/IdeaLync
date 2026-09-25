package org.solync.idealync

import dev.kord.core.Kord
import org.solync.idealync.modules.MeetingScheduler
import org.solync.idealync.modules.MemberEventLogger
import org.solync.idealync.modules.PitchCreator
import org.solync.idealync.modules.PitchForwarding
import org.solync.idealync.modules.RoleSelection

interface IdeaLyncModule {
    suspend fun onRegister(kord: Kord) {
    }

    suspend fun onReady(kord: Kord) {
    }
}

val ideaLyncModules = listOf<IdeaLyncModule>(
    RoleSelection,
    PitchCreator,
    PitchForwarding,
    MemberEventLogger,
    MeetingScheduler
)
