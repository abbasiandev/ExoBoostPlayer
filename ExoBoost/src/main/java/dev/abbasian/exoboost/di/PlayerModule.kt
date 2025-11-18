package dev.abbasian.exoboost.di

import androidx.media3.common.util.UnstableApi
import dev.abbasian.exoboost.data.ai.AudioAnalysisEngine
import dev.abbasian.exoboost.data.ai.ChapterGenerator
import dev.abbasian.exoboost.data.ai.HighlightScoringEngine
import dev.abbasian.exoboost.data.ai.MotionAnalysisEngine
import dev.abbasian.exoboost.data.ai.SceneDetectionEngine
import dev.abbasian.exoboost.data.manager.AutoRecoveryManager
import dev.abbasian.exoboost.data.manager.ExoPlayerManager
import dev.abbasian.exoboost.data.repository.MediaRepositoryImpl
import dev.abbasian.exoboost.domain.repository.MediaRepository
import dev.abbasian.exoboost.domain.usecase.ManageHighlightCacheUseCase
import dev.abbasian.exoboost.domain.usecase.PlayMediaUseCase
import dev.abbasian.exoboost.domain.usecase.RetryMediaUseCase
import dev.abbasian.exoboost.presentation.viewmodel.MediaPlayerViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

@UnstableApi
val playerModule =
    module {
        single { AutoRecoveryManager(get()) }

        single { ExoPlayerManager(androidContext(), get(), get(), get(), get()) }

        single { SceneDetectionEngine(logger = get()) }

        single { MotionAnalysisEngine(logger = get()) }

        single { AudioAnalysisEngine(logger = get()) }

        single { HighlightScoringEngine(logger = get()) }

        single { ChapterGenerator(logger = get()) }

        single<MediaRepository> { MediaRepositoryImpl(get(), get()) }

        factory { PlayMediaUseCase(get()) }
        factory { RetryMediaUseCase(get()) }
        factory { ManageHighlightCacheUseCase(get(), get()) }

        viewModel {
            MediaPlayerViewModel(
                playMediaUseCase = get(),
                retryMediaUseCase = get(),
                generateHighlightsUseCase = get(),
                manageHighlightCacheUseCase = get(),
                errorClassifier = get(),
                logger = get(),
            )
        }
    }
