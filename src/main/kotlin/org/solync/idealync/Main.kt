package org.solync.idealync

import dev.kord.core.Kord
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.on
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import io.github.cdimascio.dotenv.dotenv
import org.koin.core.context.startKoin
import org.koin.dsl.module

suspend fun setupKord(token: String) {
    val kord = Kord(token)

    ideaLyncModules.forEach { it.onRegister(kord) }

    kord.on<ReadyEvent> {
        ideaLyncModules.forEach { it.onReady(kord) }
    }

    @OptIn(PrivilegedIntent::class)
    kord.login {
        intents += Intent.MessageContent
        intents += Intent.GuildMembers
    }
}

fun appModule(config: () -> IdeaLyncConfig) = module {
    single { config() }
}

suspend fun main() {
    val denv = dotenv {
        ignoreIfMissing = true
    }

    startKoin {
        modules(appModule(denv::getIdeaLyncConfig))
    }

    setupKord(denv["APP_DISCORD_TOKEN"])
}