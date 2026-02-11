package com.anonforge.data.remote.simplelogin

import com.anonforge.data.remote.simplelogin.dto.AliasListResponseDto
import com.anonforge.data.remote.simplelogin.dto.AliasOptionsDto
import com.anonforge.data.remote.simplelogin.dto.AliasResponseDto
import com.anonforge.data.remote.simplelogin.dto.CreateAliasRequest
import com.anonforge.data.remote.simplelogin.dto.CreateCustomAliasRequest
import com.anonforge.data.remote.simplelogin.dto.UserInfoDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * SimpleLogin API interface.
 *
 * Authentication is handled by SimpleLoginInterceptor.
 * All endpoints use the API key from ApiKeyManager.
 *
 * @see SimpleLoginInterceptor
 */
interface SimpleLoginApi {

    companion object {
        const val BASE_URL = "https://app.simplelogin.io/"
    }

    /**
     * Get current user info including quota.
     */
    @GET("api/user_info")
    suspend fun getUserInfo(): Response<UserInfoDto>

    /**
     * Get available alias options (suffixes, etc.).
     */
    @GET("api/v5/alias/options")
    suspend fun getAliasOptions(
        @Query("hostname") hostname: String = "anonforge.app"
    ): Response<AliasOptionsDto>

    /**
     * Create a random alias.
     */
    @POST("api/alias/random/new")
    suspend fun createRandomAlias(
        @Body request: CreateAliasRequest = CreateAliasRequest(),
        @Query("hostname") hostname: String = "anonforge.app",
        @Query("mode") mode: String = "uuid"
    ): Response<AliasResponseDto>

    /**
     * Create a custom alias with specific prefix.
     */
    @POST("api/v3/alias/custom/new")
    suspend fun createCustomAlias(
        @Body request: CreateCustomAliasRequest,
        @Query("hostname") hostname: String = "anonforge.app"
    ): Response<AliasResponseDto>

    /**
     * Get list of existing aliases.
     * Authentication header is added by SimpleLoginInterceptor.
     */
    @GET("api/v2/aliases")
    suspend fun getAliases(
        @Query("page_id") pageId: Int = 0
    ): Response<AliasListResponseDto>
}