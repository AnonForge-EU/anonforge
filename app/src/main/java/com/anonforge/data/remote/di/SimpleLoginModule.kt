package com.anonforge.data.remote.di

import com.anonforge.data.remote.simplelogin.SimpleLoginApi
import com.anonforge.data.remote.simplelogin.SimpleLoginHostInterceptor
import com.anonforge.data.remote.simplelogin.SimpleLoginInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SimpleLoginModule {
    
    @Provides
    @Singleton
    @Named("SimpleLoginClient")
    fun provideSimpleLoginOkHttpClient(
        authInterceptor: SimpleLoginInterceptor,
        hostInterceptor: SimpleLoginHostInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            // Host rewrite must run BEFORE the auth interceptor so the auth
            // header is added on the final URL.
            .addInterceptor(hostInterceptor)
            .addInterceptor(authInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            // Security: Don't follow redirects to prevent URL manipulation
            .followRedirects(false)
            .followSslRedirects(false)
            .build()
    }
    
    @Provides
    @Singleton
    fun provideSimpleLoginApi(
        @Named("SimpleLoginClient") okHttpClient: OkHttpClient
    ): SimpleLoginApi {
        return Retrofit.Builder()
            .baseUrl(SimpleLoginApi.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SimpleLoginApi::class.java)
    }
}
