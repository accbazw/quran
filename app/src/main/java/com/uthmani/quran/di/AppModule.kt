package com.uthmani.quran.di

import android.content.Context
import androidx.room.Room
import com.uthmani.quran.data.db.AppDatabase
import com.uthmani.quran.data.db.AyahDao
import com.uthmani.quran.data.db.BookmarkDao
import com.uthmani.quran.data.db.SurahDao
import com.uthmani.quran.data.db.TranslationDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "quran.db")
            .createFromAsset("databases/quran.db")
            .build()

    @Provides
    fun provideSurahDao(db: AppDatabase): SurahDao = db.surahDao()

    @Provides
    fun provideAyahDao(db: AppDatabase): AyahDao = db.ayahDao()

    @Provides
    fun provideBookmarkDao(db: AppDatabase): BookmarkDao = db.bookmarkDao()

    @Provides
    fun provideTranslationDao(db: AppDatabase): TranslationDao = db.translationDao()
}
