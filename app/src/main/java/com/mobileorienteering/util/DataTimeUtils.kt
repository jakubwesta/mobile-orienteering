package com.mobileorienteering.util

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

fun String.toInstantOrNull(): Instant? {
    return try {
        Instant.parse(this)
    } catch (_: Exception) {
        try {
            val localDateTime = LocalDateTime.parse(this)
            localDateTime.toInstant(ZoneOffset.UTC)
        } catch (_: Exception) {
            try {
                Instant.parse("${this}Z")
            } catch (_: Exception) {
                null
            }
        }
    }
}

fun String.toInstant(): Instant {
    return toInstantOrNull()
        ?: throw IllegalArgumentException("Unable to parse date-time string: $this")
}
