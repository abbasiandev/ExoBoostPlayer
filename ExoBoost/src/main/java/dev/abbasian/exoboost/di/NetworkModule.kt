package dev.abbasian.exoboost.di

import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import dev.abbasian.exoboost.data.manager.NetworkManager
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import java.util.concurrent.TimeUnit

@UnstableApi
val networkModule =
    module {
        single {
            OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()
        }

        single { NetworkManager(androidContext(), get()) }

        single<HttpDataSource.Factory> {
            get<NetworkManager>().createHttpDataSourceFactory(allowUnsafeSSL = false)
        }
    }
