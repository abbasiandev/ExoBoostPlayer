package dev.abbasian.exoboost.di

import dev.abbasian.exoboost.data.ai.AudioAnalysisEngine
import dev.abbasian.exoboost.data.ai.ChapterGenerator
import dev.abbasian.exoboost.data.ai.HighlightScoringEngine
import dev.abbasian.exoboost.data.ai.MLKitFaceDetector
import dev.abbasian.exoboost.data.ai.MotionAnalysisEngine
import dev.abbasian.exoboost.data.ai.SceneDetectionEngine
import dev.abbasian.exoboost.data.ai.VideoAnalysisCoordinator
import dev.abbasian.exoboost.domain.usecase.GenerateVideoHighlightsUseCase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val aiModule =
    module {
        single { SceneDetectionEngine(logger = get()) }

        single { MotionAnalysisEngine(logger = get()) }

        single { AudioAnalysisEngine(logger = get()) }

        single { HighlightScoringEngine(logger = get()) }

        single { ChapterGenerator(logger = get()) }

        single { MLKitFaceDetector(logger = get()) }

        single {
            VideoAnalysisCoordinator(
                context = androidContext(),
                sceneDetector = get(),
                audioAnalyzer = get(),
                motionAnalyzer = get(),
                faceDetector = get(),
                scoringEngine = get(),
                chapterGenerator = get(),
                logger = get(),
            )
        }

        single {
            GenerateVideoHighlightsUseCase(
                context = androidContext(),
                coordinator = get(),
                logger = get(),
            )
        }
    }
