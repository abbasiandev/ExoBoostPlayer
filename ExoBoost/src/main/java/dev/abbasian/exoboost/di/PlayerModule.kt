package dev.abbasian.exoboost.di

import androidx.media3.common.util.UnstableApi
import dev.abbasian.exoboost.data.manager.ExoPlayerManager
import dev.abbasian.exoboost.data.repository.MediaRepositoryImpl
import dev.abbasian.exoboost.domain.repository.MediaRepository
import dev.abbasian.exoboost.domain.usecase.CacheVideoUseCase
import dev.abbasian.exoboost.domain.usecase.PlayMediaUseCase
import dev.abbasian.exoboost.domain.usecase.RetryMediaUseCase
import dev.abbasian.exoboost.presentation.viewmodel.MediaPlayerViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

@UnstableApi
val playerModule = module {
    single { ExoPlayerManager(androidContext(), get(), get()) }

    single<MediaRepository> { MediaRepositoryImpl(get(), get()) }

    factory { PlayMediaUseCase(get()) }
    factory { CacheVideoUseCase(get()) }
    factory { RetryMediaUseCase(get()) }

    viewModel {
        MediaPlayerViewModel(
            playMediaUseCase = get(),
            cacheVideoUseCase = get(),
            retryMediaUseCase = get()
        )
    }
}