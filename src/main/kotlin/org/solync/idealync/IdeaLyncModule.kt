package org.solync.idealync

import dev.kord.core.Kord
import org.solync.idealync.cogs.PitchCreator
import org.solync.idealync.cogs.RoleSelection

interface IdeaLyncModule {
    suspend fun onRegister(kord: Kord) {
    }

    suspend fun onReady(kord: Kord) {
    }
}

val ideaLyncModules = listOf<IdeaLyncModule>(
    RoleSelection,
    PitchCreator
)
