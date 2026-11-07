package com.uthmani.quran.util

private val ARABIC_INDIC_DIGITS = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')

/** Converts 0-9 digits to Arabic-Indic digits (display only). */
fun Int.toArabicDigits(): String = buildString {
    for (ch in this@toArabicDigits.toString()) {
        append(if (ch in '0'..'9') ARABIC_INDIC_DIGITS[ch - '0'] else ch)
    }
}
