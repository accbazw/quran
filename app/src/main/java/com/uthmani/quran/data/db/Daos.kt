package com.uthmani.quran.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SurahDao {

    @Query("SELECT * FROM surahs ORDER BY number")
    fun observeAll(): Flow<List<SurahEntity>>

    @Query("SELECT * FROM surahs WHERE number = :number")
    fun observeByNumber(number: Int): Flow<SurahEntity?>

    @Query("SELECT * FROM surahs WHERE number = :number")
    suspend fun byNumber(number: Int): SurahEntity?
}

@Dao
interface AyahDao {

    @Query("SELECT * FROM ayahs WHERE surah = :surah ORDER BY ayah")
    fun observeBySurah(surah: Int): Flow<List<AyahEntity>>

    @Query("SELECT * FROM ayahs WHERE surah = :surah AND ayah = :ayah")
    suspend fun getByNumber(surah: Int, ayah: Int): AyahEntity?

    @Query("SELECT text FROM ayahs WHERE surah = 1 AND ayah = 1")
    suspend fun fatihaFirst(): String?

    @Query(
        """
        SELECT a.surah AS surah,
               a.ayah  AS ayah,
               a.text  AS text,
               s.name_persian AS namePersian,
               s.name_arabic  AS nameArabic
        FROM ayahs a
        JOIN surahs s ON s.number = a.surah
        WHERE a.text_simple LIKE '%' || :query || '%' ESCAPE '\'
        ORDER BY a.surah, a.ayah
        LIMIT :limit
        """
    )
    suspend fun search(query: String, limit: Int): List<SearchRow>
}

@Dao
interface BookmarkDao {

    @Query(
        """
        SELECT b.surah AS surah,
               b.ayah  AS ayah,
               b.created_at AS createdAt,
               s.name_persian AS surahNamePersian,
               s.name_arabic  AS surahNameArabic,
               a.text AS ayahText
        FROM bookmarks b
        JOIN surahs s ON s.number = b.surah
        JOIN ayahs  a ON a.surah = b.surah AND a.ayah = b.ayah
        ORDER BY b.created_at DESC
        """
    )
    fun observeAll(): Flow<List<BookmarkRow>>

    @Query("SELECT ayah FROM bookmarks WHERE surah = :surah ORDER BY ayah")
    fun observeAyahNumbers(surah: Int): Flow<List<Int>>

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE surah = :surah AND ayah = :ayah)")
    fun isBookmarked(surah: Int, ayah: Int): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(bookmark: BookmarkEntity): Long

    @Query("DELETE FROM bookmarks WHERE surah = :surah AND ayah = :ayah")
    suspend fun delete(surah: Int, ayah: Int)
}

@Dao
interface TranslationDao {

    @Query("SELECT * FROM translations WHERE translator = :translator AND surah = :surah ORDER BY ayah")
    suspend fun forSurah(translator: String, surah: Int): List<TranslationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<TranslationEntity>)
}
