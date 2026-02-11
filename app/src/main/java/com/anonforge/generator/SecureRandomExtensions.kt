package com.anonforge.generator

import java.security.SecureRandom

/**
 * Extensions for SecureRandom to enable Kotlin-friendly random operations.
 *
 * Extracted to shared file to avoid code duplication across generators.
 * Used by: AddressGenerator, PhoneGenerator
 */

/**
 * Converts SecureRandom to Kotlin Random for use with stdlib random functions.
 * Provides cryptographically secure random numbers via Kotlin's random API.
 */
fun SecureRandom.asKotlinRandom(): kotlin.random.Random = object : kotlin.random.Random() {
    override fun nextBits(bitCount: Int): Int = this@asKotlinRandom.nextInt(1 shl bitCount)
}