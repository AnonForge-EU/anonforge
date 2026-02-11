package com.anonforge.data.remote.simplelogin.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO for alias creation response.
 */
data class AliasResponseDto(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("email")
    val email: String,
    
    @SerializedName("creation_timestamp")
    val creationTimestamp: Long,
    
    @SerializedName("enabled")
    val enabled: Boolean,
    
    @SerializedName("note")
    val note: String?
)

/**
 * Request body for creating random alias.
 */
data class CreateAliasRequest(
    @SerializedName("note")
    val note: String = "AnonForge generated identity"
)

/**
 * Request body for creating custom alias with specific prefix.
 */
data class CreateCustomAliasRequest(
    @SerializedName("alias_prefix")
    val aliasPrefix: String,
    
    @SerializedName("signed_suffix")
    val signedSuffix: String,
    
    @SerializedName("note")
    val note: String = "AnonForge generated identity"
)
