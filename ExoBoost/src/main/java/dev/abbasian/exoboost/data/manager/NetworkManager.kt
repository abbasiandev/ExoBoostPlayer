package dev.abbasian.exoboost.data.manager

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import okhttp3.OkHttpClient
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

@UnstableApi
class NetworkManager(private val context: Context) {

    private val connectTimeoutMs = 15000
    private val readTimeoutMs = 30000
    private val writeTimeoutMs = 15000

    fun createHttpDataSourceFactory(allowUnsafeSSL: Boolean = false): HttpDataSource.Factory {
        return try {
            createOkHttpDataSourceFactory(allowUnsafeSSL)
        } catch (e: Exception) {
            Log.w("NetworkManager", "OkHttp failed, falling back to default: ${e.message}")
            createDefaultHttpDataSourceFactory()
        }
    }

    private fun createOkHttpDataSourceFactory(allowUnsafeSSL: Boolean): HttpDataSource.Factory {
        val clientBuilder = OkHttpClient.Builder()
            .connectTimeout(connectTimeoutMs.toLong(), TimeUnit.MILLISECONDS)
            .readTimeout(readTimeoutMs.toLong(), TimeUnit.MILLISECONDS)
            .writeTimeout(writeTimeoutMs.toLong(), TimeUnit.MILLISECONDS)
            .retryOnConnectionFailure(true)
            .followRedirects(true)
            .followSslRedirects(true)

        if (allowUnsafeSSL) {
            configureTrustAllSSL(clientBuilder)
        }

        return OkHttpDataSource.Factory(clientBuilder.build())
            .setDefaultRequestProperties(getDefaultHeaders())
    }

    private fun createDefaultHttpDataSourceFactory(): HttpDataSource.Factory {
        return DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(connectTimeoutMs)
            .setReadTimeoutMs(readTimeoutMs)
            .setAllowCrossProtocolRedirects(true)
            .setKeepPostFor302Redirects(true)
            .setDefaultRequestProperties(getDefaultHeaders())
    }

    private fun getDefaultHeaders(): Map<String, String> {
        return mapOf(
            "User-Agent" to "BeautifulVideoPlayer/1.0 (Android)",
            "Accept" to "*/*",
            "Accept-Encoding" to "gzip, deflate, br",
            "Connection" to "keep-alive",
            "Cache-Control" to "no-cache"
        )
    }

    private fun configureTrustAllSSL(clientBuilder: OkHttpClient.Builder) {
        try {
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })

            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())

            clientBuilder.sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            clientBuilder.hostnameVerifier { _, _ -> true }

            Log.d("NetworkManager", "SSL trust-all configured")
        } catch (e: Exception) {
            Log.e("NetworkManager", "Failed to configure SSL: ${e.message}")
        }
    }

    fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    fun getNetworkType(): NetworkType {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return NetworkType.NONE
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return NetworkType.NONE

        return when {
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.CELLULAR
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
            else -> NetworkType.OTHER
        }
    }

    enum class NetworkType {
        WIFI, CELLULAR, ETHERNET, OTHER, NONE
    }
}
