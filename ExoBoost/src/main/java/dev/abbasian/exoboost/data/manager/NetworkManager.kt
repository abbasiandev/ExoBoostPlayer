package dev.abbasian.exoboost.data.manager

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import dev.abbasian.exoboost.util.ExoBoostLogger
import okhttp3.OkHttpClient
import okhttp3.Protocol
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

@UnstableApi
class NetworkManager(
    private val context: Context,
    private val logger: ExoBoostLogger
) {

    companion object {
        private const val TAG = "NetworkManager"
    }

    private val connectTimeoutMs = 10000
    private val readTimeoutMs = 20000
    private val writeTimeoutMs = 10000

    private var cachedClient: OkHttpClient? = null

    fun createHttpDataSourceFactory(allowUnsafeSSL: Boolean = false): HttpDataSource.Factory {
        return try {
            createOkHttpDataSourceFactory(allowUnsafeSSL)
        } catch (e: Exception) {
            logger.warning(TAG, "OkHttp failed, falling back to default: ${e.message}")
            createDefaultHttpDataSourceFactory()
        }
    }

    private fun createOkHttpDataSourceFactory(allowUnsafeSSL: Boolean): HttpDataSource.Factory {
        val client = cachedClient ?: run {
            val clientBuilder = OkHttpClient.Builder()
                .connectTimeout(connectTimeoutMs.toLong(), TimeUnit.MILLISECONDS)
                .readTimeout(readTimeoutMs.toLong(), TimeUnit.MILLISECONDS)
                .writeTimeout(writeTimeoutMs.toLong(), TimeUnit.MILLISECONDS)
                .retryOnConnectionFailure(true)
                .followRedirects(true)
                .followSslRedirects(allowUnsafeSSL)
                .protocols(listOf(Protocol.HTTP_1_1, Protocol.HTTP_2))
                .connectionPool(okhttp3.ConnectionPool(5, 5, TimeUnit.MINUTES))
                .dns(okhttp3.Dns.SYSTEM)

            if (allowUnsafeSSL) {
                configureTrustAllSSL(clientBuilder)
            }

            clientBuilder.build().also { cachedClient = it }
        }

        return OkHttpDataSource.Factory(client)
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
            "User-Agent" to "ExoBoost/1.0 (Android ${Build.VERSION.RELEASE}; ${Build.MODEL})",
            "Accept" to "*/*",
            "Accept-Encoding" to "gzip, deflate",
            "Connection" to "keep-alive",
            "Cache-Control" to "no-cache, no-store, must-revalidate",
            "Pragma" to "no-cache"
        )
    }

    private fun configureTrustAllSSL(clientBuilder: OkHttpClient.Builder) {
        try {
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
                    // TODO: Implement proper certificate pinning here
                }
                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
                    // TODO: Implement proper certificate pinning here
                }
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })

            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())

            clientBuilder.sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            clientBuilder.hostnameVerifier { _, _ -> true }

            logger.warning(TAG, "SSL trust-all configured - NOT RECOMMENDED FOR PRODUCTION")
        } catch (e: Exception) {
            logger.error(TAG, "Failed to configure SSL: ${e.message}", e)
        }
    }

    fun isNetworkAvailable(): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val network = connectivityManager?.activeNetwork ?: return false
            val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
                    networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
        } catch (e: Exception) {
            logger.error(TAG, "Error checking network availability", e)
            false
        }
    }

    fun getNetworkType(): NetworkType {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val network = connectivityManager?.activeNetwork ?: return NetworkType.NONE
            val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return NetworkType.NONE

            when {
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.CELLULAR
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> NetworkType.VPN
                else -> NetworkType.OTHER
            }
        } catch (e: Exception) {
            logger.error(TAG, "Error getting network type", e)
            NetworkType.NONE
        }
    }

    fun getConnectionQuality(): ConnectionQuality {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val network = connectivityManager?.activeNetwork ?: return ConnectionQuality.UNKNOWN
            val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return ConnectionQuality.UNKNOWN

            when {
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                    // For WiFi, assume good quality unless we can determine otherwise
                    ConnectionQuality.HIGH
                }
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val linkDownstreamBandwidth = networkCapabilities.linkDownstreamBandwidthKbps
                        when {
                            linkDownstreamBandwidth > 5000 -> ConnectionQuality.HIGH
                            linkDownstreamBandwidth > 1000 -> ConnectionQuality.MEDIUM
                            else -> ConnectionQuality.LOW
                        }
                    } else {
                        ConnectionQuality.MEDIUM
                    }
                }
                else -> ConnectionQuality.MEDIUM
            }
        } catch (e: Exception) {
            logger.error(TAG, "Error getting connection quality", e)
            ConnectionQuality.UNKNOWN
        }
    }

    fun cleanup() {
        try {
            cachedClient?.dispatcher?.executorService?.shutdown()
            cachedClient?.connectionPool?.evictAll()
            cachedClient = null
        } catch (e: Exception) {
            logger.error(TAG, "Error during cleanup", e)
        }
    }

    enum class NetworkType {
        WIFI, CELLULAR, ETHERNET, VPN, OTHER, NONE
    }

    enum class ConnectionQuality {
        HIGH, MEDIUM, LOW, UNKNOWN
    }
}