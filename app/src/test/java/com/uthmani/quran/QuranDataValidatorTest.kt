package com.uthmani.quran

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuranDataValidatorTest {

    private fun validSurahs() = listOf(
        QuranDataValidator.SurahMeta(1, 3),
        QuranDataValidator.SurahMeta(2, 4),
    )

    private fun validAyahs() = listOf(
        QuranDataValidator.AyahRow(1, 1, "one"),
        QuranDataValidator.AyahRow(1, 2, "two"),
        QuranDataValidator.AyahRow(1, 3, "three"),
        QuranDataValidator.AyahRow(2, 1, "four"),
        QuranDataValidator.AyahRow(2, 2, "five"),
        QuranDataValidator.AyahRow(2, 3, "six"),
        QuranDataValidator.AyahRow(2, 4, "seven"),
    )

    private fun validate(surahs: List<QuranDataValidator.SurahMeta> = validSurahs(),
                         ayahs: List<QuranDataValidator.AyahRow> = validAyahs()) =
        QuranDataValidator.validate(surahs, ayahs, expectedSurahs = 2, expectedTotal = 7)

    @Test
    fun `valid data passes`() {
        val result = validate()
        assertTrue(result.errors.toString(), result.isValid)
        assertEquals(2, result.checkedSurahs)
        assertEquals(7, result.checkedAyahs)
    }

    @Test
    fun `missing aya fails`() {
        val ayahs = validAyahs().filterNot { it.surah == 2 && it.ayah == 3 }
        val result = validate(ayahs = ayahs)
        assertTrue(!result.isValid)
        assertTrue(result.errors.any { it.contains("metadata says") || it.contains("consecutive") })
    }

    @Test
    fun `count mismatch with metadata fails`() {
        val surahs = listOf(
            QuranDataValidator.SurahMeta(1, 3),
            QuranDataValidator.SurahMeta(2, 5), // wrong on purpose
        )
        val result = validate(surahs = surahs)
        assertTrue(!result.isValid)
        assertTrue(result.errors.any { it.contains("surah 2") })
    }

    @Test
    fun `duplicate pair fails`() {
        val ayahs = validAyahs() + QuranDataValidator.AyahRow(1, 1, "dup")
        val result = validate(ayahs = ayahs)
        assertTrue(!result.isValid)
        assertTrue(result.errors.any { it.contains("duplicate") })
    }

    @Test
    fun `empty text fails`() {
        val ayahs = validAyahs().map { if (it.surah == 1 && it.ayah == 1) it.copy(text = "   ") else it }
        val result = validate(ayahs = ayahs)
        assertTrue(!result.isValid)
        assertTrue(result.errors.any { it.contains("empty ayah") })
    }

    @Test
    fun `gap in ayah numbering fails`() {
        val ayahs = validAyahs().map { if (it.surah == 2 && it.ayah == 4) it.copy(ayah = 5) else it }
        val result = validate(ayahs = ayahs)
        assertTrue(!result.isValid)
        assertTrue(result.errors.any { it.contains("consecutive") })
    }

    @Test
    fun `total mismatch fails`() {
        val result = QuranDataValidator.validate(
            validSurahs(),
            validAyahs(),
            expectedSurahs = 2,
            expectedTotal = 6236,
        )
        assertTrue(!result.isValid)
        assertTrue(result.errors.any { it.contains("total ayat") })
    }
}
