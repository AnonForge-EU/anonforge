package com.anonforge

import com.anonforge.domain.model.Nationality
import com.anonforge.generator.PhoneGenerator
import java.security.SecureRandom
import org.junit.Test
import kotlin.test.assertTrue

/**
 * Tests for [PhoneGenerator] — the real production class.
 *
 * Validates the international (E.164-style) format produced per nationality:
 * - FR: +33 then mobile prefix 6/7 + 8 digits
 * - EN (UK): +44 then mobile prefix 7 + 9 digits
 * - DE: +49 then mobile prefix 15x/16x/17x + 8 digits
 */
class PhoneGeneratorTest {

    private val generator = PhoneGenerator(SecureRandom())

    private companion object {
        const val ITERATIONS = 200
        val FR_FORMAT = Regex("""^\+33[67]\d{8}$""")
        val UK_FORMAT = Regex("""^\+447\d{9}$""")
        val DE_FORMAT = Regex("""^\+49(15|16|17)\d{9}$""")
    }

    @Test
    fun `french numbers match +33 mobile format`() {
        repeat(ITERATIONS) {
            val phone = generator.generatePhone(Nationality.FR).value
            assertTrue(FR_FORMAT.matches(phone), "Invalid FR number: $phone")
        }
    }

    @Test
    fun `uk numbers match +44 mobile format`() {
        repeat(ITERATIONS) {
            val phone = generator.generatePhone(Nationality.EN).value
            assertTrue(UK_FORMAT.matches(phone), "Invalid UK number: $phone")
        }
    }

    @Test
    fun `german numbers match +49 mobile format`() {
        repeat(ITERATIONS) {
            val phone = generator.generatePhone(Nationality.DE).value
            assertTrue(DE_FORMAT.matches(phone), "Invalid DE number: $phone")
        }
    }

    @Test
    fun `legacy no-arg overload produces a supported format`() {
        repeat(ITERATIONS) {
            val phone = generator.generatePhone().value
            assertTrue(
                FR_FORMAT.matches(phone) || UK_FORMAT.matches(phone) || DE_FORMAT.matches(phone),
                "Legacy random-nationality overload produced unknown format: $phone"
            )
        }
    }
}
