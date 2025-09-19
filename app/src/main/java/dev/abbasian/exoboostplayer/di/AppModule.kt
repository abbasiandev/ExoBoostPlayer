package dev.abbasian.exoboostplayer.di

import dev.abbasian.exoboostplayer.presentation.viewmodel.MainViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    viewModel { MainViewModel() }
}