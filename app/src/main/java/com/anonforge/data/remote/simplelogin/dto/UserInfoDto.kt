package com.anonforge.data.remote.simplelogin.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO for SimpleLogin user info response.
 * Contains quota information for alias limits.
 */
data class UserInfoDto(
    @SerializedName("name")
    val name: String?,

    @SerializedName("email")
    val email: String,

    @SerializedName("is_premium")
    val isPremium: Boolean,

    @SerializedName("in_trial")
    val inTrial: Boolean,

    @SerializedName("max_alias_free_account")
    val maxAliasFreeAccount: Int,

    @SerializedName("alias_count")
    val aliasCount: Int
) {
    val remainingAliases: Int
        get() = if (isPremium) Int.MAX_VALUE else (maxAliasFreeAccount - aliasCount).coerceAtLeast(0)

    /**
     * Check if user has reached their alias quota limit.
     */
    @Suppress("unused") // Public API for quota limit warning in alias creation dialog
    val isAtLimit: Boolean
        get() = !isPremium && aliasCount >= maxAliasFreeAccount
}