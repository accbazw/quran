package com.uthmani.quran.data

import com.uthmani.quran.data.db.AyahDao
import com.uthmani.quran.data.db.AyahEntity
import com.uthmani.quran.data.db.BookmarkDao
import com.uthmani.quran.data.db.BookmarkEntity
import com.uthmani.quran.data.db.BookmarkRow
import com.uthmani.quran.data.db.SearchRow
import com.uthmani.quran.data.db.SurahDao
import com.uthmani.quran.data.db.SurahEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuranRepository @Inject constructor(
    private val surahDao: SurahDao,
    private val ayahDao: AyahDao,
) {
    fun observeSurahs(): Flow<List<SurahEntity>> = surahDao.observeAll()

    fun observeSurah(number: Int): Flow<SurahEntity?> = surahDao.observeByNumber(number)

    fun observeAyahs(surah: Int): Flow<List<AyahEntity>> = ayahDao.observeBySurah(surah)

    suspend fun ayah(surah: Int, ayah: Int): AyahEntity? = ayahDao.getByNumber(surah, ayah)

    suspend fun fatihaFirstText(): String? = ayahDao.fatihaFirst()
}

@Singleton
class SearchRepository @Inject constructor(
    private val ayahDao: AyahDao,
) {
    /**
     * Diacritic-insensitive search: the query is normalized with the exact same
     * rules that produced `ayahs.text_simple` (see QuranTextNormalizer).
     * LIKE wildcards in user input are escaped (see ESCAPE in AyahDao.search).
     */
    suspend fun search(rawQuery: String, limit: Int = 200): List<SearchRow> {
        val query = QuranTextNormalizer.normalize(rawQuery).trim()
        if (query.length < 2) return emptyList()
        return ayahDao.search(escapeLike(query), limit)
    }

    private fun escapeLike(s: String): String =
        s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
}

@Singleton
class BookmarkRepository @Inject constructor(
    private val bookmarkDao: BookmarkDao,
) {
    fun observeBookmarks(): Flow<List<BookmarkRow>> = bookmarkDao.observeAll()

    fun observeBookmarkedAyahs(surah: Int): Flow<List<Int>> =
        bookmarkDao.observeAyahNumbers(surah)

    suspend fun toggle(surah: Int, ayah: Int): Boolean {
        val inserted = bookmarkDao.insert(
            BookmarkEntity(surah = surah, ayah = ayah, createdAt = System.currentTimeMillis()),
        )
        return if (inserted != -1L) {
            true
        } else {
            bookmarkDao.delete(surah, ayah)
            false
        }
    }

    suspend fun delete(surah: Int, ayah: Int) = bookmarkDao.delete(surah, ayah)
}
