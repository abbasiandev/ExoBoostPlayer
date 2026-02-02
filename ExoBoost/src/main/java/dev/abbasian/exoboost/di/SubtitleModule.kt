package dev.abbasian.exoboost.di

import dev.abbasian.exoboost.data.api.sources.OpenSubtitlesApi
import dev.abbasian.exoboost.data.api.sources.PodnapisiApi
import dev.abbasian.exoboost.data.api.sources.SubtitleApiAggregator
import dev.abbasian.exoboost.data.api.sources.YifySubtitlesApi
import dev.abbasian.exoboost.data.local.store.SubtitlePreferencesManager
import dev.abbasian.exoboost.data.manager.SubtitleManager
import dev.abbasian.exoboost.data.parser.SubtitleParser
import dev.abbasian.exoboost.data.repository.SubtitleRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val subtitleModule =
    module {

        single { SubtitleParser(logger = get()) }

        single { OpenSubtitlesApi(client = get(), logger = get()) }
        single { YifySubtitlesApi(client = get(), logger = get()) }
        single { PodnapisiApi(client = get(), logger = get()) }

        single {
            SubtitleApiAggregator(
                openSubtitlesApi = get(),
                yifySubtitlesApi = get(),
                podnapisiApi = get(),
                logger = get(),
            )
        }

        single {
            SubtitleManager(
                context = androidContext(),
                subtitleApiAggregator = get(),
                subtitleParser = get(),
                client = get(),
                logger = get(),
            )
        }

        single { SubtitlePreferencesManager(context = androidContext()) }

        single {
            SubtitleRepository(
                subtitleManager = get(),
                preferencesManager = get(),
            )
        }
    }
