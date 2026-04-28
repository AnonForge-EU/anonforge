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
 *
 * mailboxIds is required by SimpleLogin when the user has multiple mailboxes;
 * the server returns 400 otherwise. Pass an empty list to use the default
 * mailbox (the server falls back to it).
 */
data class CreateCustomAliasRequest(
    @SerializedName("alias_prefix")
    val aliasPrefix: String,

    @SerializedName("signed_suffix")
    val signedSuffix: String,

    @SerializedName("mailbox_ids")
    val mailboxIds: List<Int> = emptyList(),

    @SerializedName("note")
    val note: String = "AnonForge generated identity"
)

/**
 * Response from POST /api/aliases/{id}/toggle.
 */
data class ToggleAliasResponseDto(
    @SerializedName("enabled")
    val enabled: Boolean
)

/**
 * Request body for PATCH /api/aliases/{id}.
 * All fields are optional — only set the ones you want to update.
 */
data class UpdateAliasRequest(
    @SerializedName("note")
    val note: String? = null,

    @SerializedName("name")
    val name: String? = null,

    @SerializedName("mailbox_ids")
    val mailboxIds: List<Int>? = null,

    @SerializedName("disable_pgp")
    val disablePgp: Boolean? = null,

    @SerializedName("pinned")
    val pinned: Boolean? = null
)
