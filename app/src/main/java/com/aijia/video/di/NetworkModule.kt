package com.aijia.video.di

import com.aijia.video.data.remote.ApiSecurity
import com.aijia.video.data.remote.ApiService
import com.aijia.video.data.repository.SessionManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import android.content.Context
import com.aijia.video.util.GifUrlReader
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.Inet4Address
import java.net.InetAddress
import java.net.Socket
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import javax.net.SocketFactory

/**
 * 网络依赖注入模块
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .setLenient()
            .serializeNulls()
            .create()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(sessionManager: SessionManager): OkHttpClient {
        return OkHttpClient.Builder()
            // ── 拦截器1：JWT Token注入（保持原逻辑不变） ──
            .addInterceptor { chain ->
                val original = chain.request()
                val token = sessionManager.getToken()
                val request = if (!token.isNullOrBlank()) {
                    original.newBuilder()
                        .addHeader("Authorization", token)
                        .build()
                } else {
                    original
                }
                chain.proceed(request)
            }
            // ── 拦截器2：HMAC-SHA256签名 + Nonce防重放（新增） ──
            .addInterceptor { chain ->
                val original  = chain.request()
                val timestamp = System.currentTimeMillis() / 1000
                val nonce     = ApiSecurity.generateNonce()
                val method    = original.method
                val url       = original.url.encodedPath

                // 读取请求body字节（不消耗原始流）
                val bodyBytes = original.body?.let { body ->
                    val buf = okio.Buffer()
                    body.writeTo(buf)
                    buf.readByteArray()
                } ?: ByteArray(0)

                val signature = ApiSecurity.sign(method, url, timestamp, nonce, bodyBytes)

                val request = original.newBuilder()
                    .addHeader("X-Timestamp", timestamp.toString())
                    .addHeader("X-Nonce",     nonce)
                    .addHeader("X-Signature", signature)
                    .build()
                chain.proceed(request)
            }
            // ── 日志拦截器：正式版关闭敏感日志 ──
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = if (com.aijia.video.BuildConfig.DEBUG) {
                        HttpLoggingInterceptor.Level.BODY
                    } else {
                        HttpLoggingInterceptor.Level.NONE
                    }
                }
            )
            // ── DNS过滤：域名解析只返回IPv4 ──
            .dns(object : Dns {
                override fun lookup(hostname: String): List<InetAddress> {
                    val addresses = Dns.SYSTEM.lookup(hostname)
                    val ipv4 = addresses.filter { it is Inet4Address }
                    return ipv4.ifEmpty { addresses }
                }
            })
            .socketFactory(object : SocketFactory() {
                override fun createSocket(): Socket =
                    Socket().also {
                        it.bind(java.net.InetSocketAddress(
                            Inet4Address.getByAddress(ByteArray(4)), 0))
                    }
                override fun createSocket(host: String, port: Int) = Socket(host, port)
                override fun createSocket(host: String, port: Int, localHost: InetAddress, localPort: Int) = Socket(host, port, localHost, localPort)
                override fun createSocket(address: InetAddress, port: Int) = Socket(address, port)
                override fun createSocket(address: InetAddress, port: Int, localAddress: InetAddress, localPort: Int) = Socket(address, port, localAddress, localPort)
            })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        gson: Gson,
        @ApplicationContext context: Context
    ): Retrofit {
        val baseUrl = GifUrlReader.readUrl(context)
            ?: throw IllegalStateException("BASE_URL not found")
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }
}
