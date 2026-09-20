package org.solync.idealync

import dev.kord.core.Kord
import org.solync.idealync.modules.PitchCreator
import org.solync.idealync.modules.RoleSelection

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
