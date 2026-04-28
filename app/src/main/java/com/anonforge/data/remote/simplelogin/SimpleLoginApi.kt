package com.anonforge.data.remote.simplelogin

import com.anonforge.data.remote.simplelogin.dto.AliasDetailDto
import com.anonforge.data.remote.simplelogin.dto.AliasListResponseDto
import com.anonforge.data.remote.simplelogin.dto.AliasOptionsDto
import com.anonforge.data.remote.simplelogin.dto.AliasResponseDto
import com.anonforge.data.remote.simplelogin.dto.CreateAliasRequest
import com.anonforge.data.remote.simplelogin.dto.CreateCustomAliasRequest
import com.anonforge.data.remote.simplelogin.dto.ToggleAliasResponseDto
import com.anonforge.data.remote.simplelogin.dto.UpdateAliasRequest
import com.anonforge.data.remote.simplelogin.dto.UserInfoDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * SimpleLogin API interface.
 *
 * Authentication is handled by SimpleLoginInterceptor.
 * Base URL is rewritten at request time by SimpleLoginHostInterceptor when
 * the user has configured a self-hosted instance.
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
     * @param mode "uuid" (default) or "word" — both are supported by SimpleLogin.
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
     * Get list of existing aliases (paginated, 20 per page).
     */
    @GET("api/v2/aliases")
    suspend fun getAliases(
        @Query("page_id") pageId: Int = 0
    ): Response<AliasListResponseDto>

    /**
     * Get details of a single alias by id.
     * Returns live forward/block stats.
     */
    @GET("api/aliases/{alias_id}")
    suspend fun getAlias(
        @Path("alias_id") aliasId: Int
    ): Response<AliasDetailDto>

    /**
     * Toggle (enable/disable) an alias. Returns the new enabled state.
     */
    @POST("api/aliases/{alias_id}/toggle")
    suspend fun toggleAlias(
        @Path("alias_id") aliasId: Int
    ): Response<ToggleAliasResponseDto>

    /**
     * Delete an alias permanently.
     */
    @DELETE("api/aliases/{alias_id}")
    suspend fun deleteAlias(
        @Path("alias_id") aliasId: Int
    ): Response<Unit>

    /**
     * Update an alias (note, name, mailboxes...).
     */
    @PATCH("api/aliases/{alias_id}")
    suspend fun updateAlias(
        @Path("alias_id") aliasId: Int,
        @Body request: UpdateAliasRequest
    ): Response<Unit>
}