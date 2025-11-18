package dev.abbasian.exoboost.di

import androidx.room.Room
import dev.abbasian.exoboost.data.local.database.ExoBoostDatabase
import dev.abbasian.exoboost.data.local.store.EqualizerPreferencesManager
import dev.abbasian.exoboost.data.local.store.HighlightPreferences
import dev.abbasian.exoboost.data.repository.HighlightCacheRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val dataModule =
    module {
        single<EqualizerPreferencesManager> { EqualizerPreferencesManager(get()) }

        single { HighlightPreferences(androidContext()) }

        single {
            HighlightCacheRepository(
                highlightDao = get(),
                preferences = get(),
                logger = get(),
            )
        }

        single {
            Room
                .databaseBuilder(
                    androidContext(),
                    ExoBoostDatabase::class.java,
                    ExoBoostDatabase.DATABASE_NAME,
                ).fallbackToDestructiveMigration()
                .build()
        }

        single { get<ExoBoostDatabase>().videoHighlightDao() }
    }
