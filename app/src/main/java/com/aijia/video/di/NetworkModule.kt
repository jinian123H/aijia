package com.aijia.video.di

import android.content.Context
import com.aijia.video.data.remote.ApiSecurity
import com.aijia.video.data.remote.ApiService
import com.aijia.video.data.repository.SessionManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Dns
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.Inet4Address
import java.net.InetAddress
import java.net.Socket
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import javax.net.SocketFactory

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
            // JWT Token注入
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
            // 请求加密 + 响应解密
            .addInterceptor(EncryptionInterceptor())
            // 日志
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
            )
            // DNS过滤：只返回IPv4
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
        val baseUrl = readBaseUrl(context)
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

    /**
     * 从assets/api_config.json读取后端URL
     */
    private fun readBaseUrl(context: Context): String {
        val json = context.assets.open("api_config.json").bufferedReader().use { it.readText() }
        val obj = JSONObject(json)
        var url = obj.getString("url")
        if (!url.endsWith("/")) url += "/"
        return url
    }
}

/**
 * 请求加密 + 响应解密拦截器
 * POST/PUT/PATCH请求体加密为 {"data":"<base64_encrypted>"}
 * JSON响应从 {"data":"<base64_encrypted>"} 解密为原始JSON
 */
class EncryptionInterceptor : Interceptor {

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val method = original.method
        val path = original.url.encodedPath

        // 跳过不需要加密的路径
        if (!path.startsWith("/api/v1/") ||
            path.startsWith("/api/v1/admin/") ||
            path.startsWith("/api/v1/install/")) {
            return chain.proceed(original)
        }

        var request = original

        // 请求体加密（POST/PUT/PATCH）
        if ((method == "POST" || method == "PUT" || method == "PATCH") && original.body != null) {
            val buffer = okio.Buffer()
            original.body!!.writeTo(buffer)
            val bodyStr = buffer.readUtf8()

            if (bodyStr.isNotBlank()) {
                val encrypted = ApiSecurity.encrypt(bodyStr)
                val json = """{"data":"$encrypted"}"""
                request = original.newBuilder()
                    .method(method, json.toRequestBody(jsonMediaType))
                    .build()
            }
        }

        // 执行请求
        val response = chain.proceed(request)

        // 响应体解密
        val responseBody = response.body ?: return response
        val contentType = responseBody.contentType()
        if (contentType?.subtype == "json") {
            val bodyString = responseBody.string()
            try {
                val jsonObj = JSONObject(bodyString)
                val encryptedData = if (jsonObj.has("data") && !jsonObj.isNull("data")) jsonObj.getString("data") else null
                if (!encryptedData.isNullOrBlank()) {
                    val decrypted = ApiSecurity.decrypt(encryptedData)
                    if (decrypted != null) {
                        return response.newBuilder()
                            .body(decrypted.toResponseBody(contentType))
                            .build()
                    }
                }
            } catch (_: Exception) {
                // 非加密格式，原样返回
            }
            // 回写原始响应
            return response.newBuilder()
                .body(bodyString.toResponseBody(contentType))
                .build()
        }

        return response
    }
}
