package com.uthmani.quran.util

/**
 * Pure-JVM integrity checks mirroring scripts/validate_quran.py.
 * Unit-tested with synthetic fixtures; the Python script remains the
 * authoritative gate that runs against the real Tanzil files.
 */
object QuranDataValidator {

    data class SurahMeta(val number: Int, val ayahCount: Int)
    data class AyahRow(val surah: Int, val ayah: Int, val text: String)

    data class Result(
        val errors: List<String>,
        val checkedSurahs: Int,
        val checkedAyahs: Int,
    ) {
        val isValid: Boolean get() = errors.isEmpty()
    }

    fun validate(
        surahs: List<SurahMeta>,
        ayahs: List<AyahRow>,
        expectedSurahs: Int = 114,
        expectedTotal: Int = 6236,
    ): Result {
        val errors = mutableListOf<String>()

        // sura indices must be exactly 1..N without gaps/duplicates
        val surahNumbers = surahs.map { it.number }.sorted()
        if (surahNumbers != (1..expectedSurahs).toList()) {
            errors.add("surah indices are not exactly 1..$expectedSurahs")
        }
        val metaByNumber = surahs.associateBy { it.number }

        // duplicates (surah, ayah)
        val seen = HashSet<Pair<Int, Int>>()
        for (row in ayahs) {
            if (!seen.add(row.surah to row.ayah)) {
                errors.add("duplicate (surah, ayah) pair: ${row.surah}:${row.ayah}")
            }
        }

        // per-surah checks
        val bySurah = ayahs.groupBy { it.surah }
        var total = 0
        for ((number, rows) in bySurah) {
            val meta = metaByNumber[number]
            if (meta == null) {
                errors.add("ayah rows reference unknown surah $number")
                continue
            }
            if (rows.size != meta.ayahCount) {
                errors.add("surah $number: ${rows.size} ayat, metadata says ${meta.ayahCount}")
            }
            val numbers = rows.map { it.ayah }.sorted()
            if (numbers != (1..rows.size).toList()) {
                errors.add("surah $number: ayah numbers are not consecutive 1..${rows.size}")
            }
            if (rows.any { it.text.isBlank() }) {
                errors.add("surah $number: contains an empty ayah")
            }
            total += rows.size
        }
        if (total != expectedTotal) {
            errors.add("total ayat = $total, expected $expectedTotal")
        }

        return Result(errors, metaByNumber.size, total)
    }
}
