package dev.abbasian.exoboost.di

import dev.abbasian.exoboost.data.store.EqualizerPreferencesManager
import org.koin.dsl.module

val dataModule =
    module {
        single<EqualizerPreferencesManager> { EqualizerPreferencesManager(get()) }
    }
