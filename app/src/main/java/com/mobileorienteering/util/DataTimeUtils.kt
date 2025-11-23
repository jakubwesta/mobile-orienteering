package com.mobileorienteering.util

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

fun String.toInstantOrNull(): Instant? {
    return try {
        Instant.parse(this)
    } catch (e: Exception) {
        try {
            val localDateTime = LocalDateTime.parse(this)
            localDateTime.toInstant(ZoneOffset.UTC)
        } catch (e2: Exception) {
            try {
                Instant.parse("${this}Z")
            } catch (e3: Exception) {
                null
            }
        }
    }
}

fun String.toInstant(): Instant {
    return toInstantOrNull()
        ?: throw IllegalArgumentException("Unable to parse date-time string: $this")
}
