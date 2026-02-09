package dev.abbasian.exoboost.di

import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import dev.abbasian.exoboost.data.manager.CacheManager
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

@UnstableApi
val cacheModule =
    module {
        single { CacheManager(androidContext(), get()) }

        single<Cache> { get<CacheManager>().getCache() }

        factory<DataSource.Factory> {
            val defaultDataSourceFactory =
                DefaultDataSource.Factory(
                    androidContext(),
                    get<HttpDataSource.Factory>(),
                )

            CacheDataSource
                .Factory()
                .setCache(get())
                .setUpstreamDataSourceFactory(defaultDataSourceFactory)
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
                .setCacheWriteDataSinkFactory(null)
        }
    }
