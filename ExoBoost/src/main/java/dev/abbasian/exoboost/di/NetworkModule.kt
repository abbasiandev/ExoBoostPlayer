package dev.abbasian.exoboost.di

import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import dev.abbasian.exoboost.data.manager.NetworkManager
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

@UnstableApi
val networkModule = module {
    single { NetworkManager(androidContext(), get()) }

    single<HttpDataSource.Factory> {
        get<NetworkManager>().createHttpDataSourceFactory(allowUnsafeSSL = false)
    }
}