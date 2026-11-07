package com.uthmani.quran.data

/**
 * Bismillah handling — DISPLAY LAYER ONLY.
 *
 * The strings below were copied VERBATIM from data/tanzil/quran-uthmani.txt
 * (output of scripts/build_db.py; verified by scripts/validate_quran.py).
 * The stored text is never modified: sura-1 ayah-1 is the bismillah itself,
 * and for all other suras (except sura 9) the bismillah is attached to the
 * beginning of ayah 1 and is only stripped for display.
 *
 * Note: in the data, the bismillah of suras 95 and 97 appears in a second
 * form (BISMILLAH_FORM_2, with shadda on the first letter). This is simply
 * how it is present in the downloaded Tanzil file — no claim is made here
 * about orthography.
 */
object QuranText {

    const val BISMILLAH_FORM_1 = "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ"
    const val BISMILLAH_FORM_2 = "بِّسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ"

    private val ALL_FORMS = listOf(BISMILLAH_FORM_1, BISMILLAH_FORM_2)

    /** Sura 1: bismillah is aya 1 itself. Sura 9: no bismillah. */
    private val SURAS_WITHOUT_BISMILLAH_HEADER = setOf(1, 9)

    /** Header text shown above the first aya (null = no header). */
    fun bismillahHeader(surahNumber: Int): String? =
        if (surahNumber in SURAS_WITHOUT_BISMILLAH_HEADER) null else BISMILLAH_FORM_1

    /** Display text of an aya: strips an attached bismillah prefix, if present. */
    fun displayAyahText(surahNumber: Int, ayahNumber: Int, storedText: String): String {
        if (ayahNumber != 1 || surahNumber in SURAS_WITHOUT_BISMILLAH_HEADER) return storedText
        for (form in ALL_FORMS) {
            if (storedText.startsWith(form)) {
                return storedText.removePrefix(form).trimStart()
            }
        }
        return storedText
    }
}
