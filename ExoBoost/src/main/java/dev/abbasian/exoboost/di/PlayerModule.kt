package dev.abbasian.exoboost.di

import androidx.media3.common.util.UnstableApi
import dev.abbasian.exoboost.data.manager.ExoPlayerManager
import dev.abbasian.exoboost.data.repository.MediaRepositoryImpl
import dev.abbasian.exoboost.domain.repository.MediaRepository
import dev.abbasian.exoboost.domain.usecase.CacheVideoUseCase
import dev.abbasian.exoboost.domain.usecase.PlayVideoUseCase
import dev.abbasian.exoboost.domain.usecase.RetryVideoUseCase
import dev.abbasian.exoboost.presentation.viewmodel.VideoPlayerViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

@UnstableApi
val playerModule = module {
    single { ExoPlayerManager(androidContext(), get(), get()) }

    single<MediaRepository> { MediaRepositoryImpl(get(), get()) }

    factory { PlayVideoUseCase(get()) }
    factory { CacheVideoUseCase(get()) }
    factory { RetryVideoUseCase(get()) }

    viewModel {
        VideoPlayerViewModel(
            playVideoUseCase = get(),
            cacheVideoUseCase = get(),
            retryVideoUseCase = get()
        )
    }
}