@file:OptIn(ExperimentalContracts::class)

package org.solync.idealync.utils

import dev.kord.core.behavior.interaction.ActionInteractionBehavior
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.response.DeferredMessageInteractionResponseBehavior
import dev.kord.core.behavior.interaction.response.respond
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

suspend inline fun <T> DeferredMessageInteractionResponseBehavior.requireNotNull(v: T?, message: () -> String): T {
    contract {
        returns() implies (v != null)
    }

    if (v == null) {
        val msg = message()
        respond { content = "Internal error: $msg" }
        throw IllegalArgumentException(msg)
    }
    return v
}

suspend inline fun <T> ActionInteractionBehavior.requireNotNull(v: T?, message: () -> String): T {
    contract {
        returns() implies (v != null)
    }

    if (v == null) {
        val msg = message()
        this.respondEphemeral { content = "Internal error: $msg" }
        throw IllegalArgumentException(msg)
    }
    return v
}

suspend inline fun DeferredMessageInteractionResponseBehavior.require(v: Boolean, message: () -> String) {
    contract {
        returns() implies v
    }

    if (!v) {
        val msg = message()
        respond { content = "Internal error: $msg" }
        throw IllegalArgumentException(msg)
    }
}

suspend inline fun ActionInteractionBehavior.require(v: Boolean, message: () -> String) {
    contract {
        returns() implies v
    }

    if (!v) {
        val msg = message()
        this.respondEphemeral { content = "Internal error: $msg" }
        throw IllegalArgumentException(msg)
    }
}
