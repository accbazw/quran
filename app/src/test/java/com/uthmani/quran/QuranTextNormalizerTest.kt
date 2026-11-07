package com.uthmani.quran

import org.junit.Assert.assertEquals
import org.junit.Test

class QuranTextNormalizerTest {

    @Test
    fun `removes diacritics`() {
        assertEquals("بسم", QuranTextNormalizer.normalize("بِسْمِ"))
    }

    @Test
    fun `removes quranic pause marks and small letters`() {
        // 2|2 contains U+06DB pause marks in the Uthmani text
        val normalized = QuranTextNormalizer.normalize("ذَٰلِكَ ٱلْكِتَـٰبُ لَا رَيْبَ ۛ فِيهِ ۛ")
        assertEquals("ذلك الكتاب لا ريب فيه", normalized)
    }

    @Test
    fun `unifies alef wasla and hamza forms`() {
        assertEquals("الله", QuranTextNormalizer.normalize("ٱللَّهِ"))
        assertEquals("اب", QuranTextNormalizer.normalize("أَب"))
        assertEquals("اسرا", QuranTextNormalizer.normalize("إِسْرَا"))
    }

    @Test
    fun `unifies taa marbuta with haa`() {
        assertEquals("مدرسه", QuranTextNormalizer.normalize("مَدْرَسَة"))
    }

    @Test
    fun `unifies yeh and kaf variants`() {
        assertEquals("كتاب", QuranTextNormalizer.normalize("کِتَاب"))
        assertEquals("يوسف", QuranTextNormalizer.normalize("یُوسُف"))
        assertEquals("علي", QuranTextNormalizer.normalize("عَلَىٰ"))
    }

    @Test
    fun `keeps spaces so multiword search works`() {
        assertEquals("بسم الله", QuranTextNormalizer.normalize("بِسْمِ ٱللَّهِ"))
    }

    @Test
    fun `normalizeWithMap maps indices back to the original`() {
        val (normalized, map) = QuranTextNormalizer.normalizeWithMap("بِسْمِ")
        assertEquals("بسم", normalized)
        assertEquals(listOf(0, 2, 4), map)
    }

    @Test
    fun `matchRanges finds diacritic-insensitive matches in the original text`() {
        val ranges = QuranTextNormalizer.matchRanges("بِسْمِ ٱللَّهِ", "بسم")
        assertEquals(listOf(0..4), ranges)
        assertEquals("بِسْمِ", "بِسْمِ ٱللَّهِ".substring(ranges.first()))
    }

    @Test
    fun `matchRanges finds all occurrences`() {
        val ranges = QuranTextNormalizer.matchRanges("قالوا سمعا واطاعوا قال", "قال")
        assertEquals(2, ranges.size)
    }

    @Test
    fun `matchRanges with empty or short query is empty`() {
        assertEquals(emptyList<IntRange>(), QuranTextNormalizer.matchRanges("بِسْمِ", ""))
        assertEquals(emptyList<IntRange>(), QuranTextNormalizer.matchRanges("بِسْمِ", " "))
    }
}
