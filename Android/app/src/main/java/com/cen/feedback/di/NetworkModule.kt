package com.cen.feedback.di

import com.cen.feedback.BuildConfig
import com.cen.feedback.data.api.ApiService
import com.cen.feedback.data.local.TokenStore
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    @Provides
    @Singleton
    fun provideOkHttp(tokenStore: TokenStore): OkHttpClient {
        val logger = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
            else HttpLoggingInterceptor.Level.NONE
        }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val cachedToken = tokenStore.tokenFlow.stateIn(scope, SharingStarted.Eagerly, null)

        return OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            // 大文件下载/视频流读取，最长 5 分钟
            .readTimeout(5, TimeUnit.MINUTES)
            // 大视频上传可能耗时较久，设为 10 分钟
            .writeTimeout(10, TimeUnit.MINUTES)
            // 整体调用上限，30 分钟（防止半永久挂起）
            .callTimeout(30, TimeUnit.MINUTES)
            .retryOnConnectionFailure(true)
            .addInterceptor { chain ->
                val token = cachedToken.value
                val req = chain.request().newBuilder()
                    .apply { if (!token.isNullOrBlank()) header("Authorization", token) }
                    .build()
                chain.proceed(req)
            }
            .addInterceptor(logger)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, moshi: Moshi): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi).asLenient())
            .build()

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService =
        retrofit.create(ApiService::class.java)
}
