package com.anonforge.data.remote.simplelogin.dto

import com.google.gson.annotations.SerializedName

data class AliasListResponseDto(
    @SerializedName("aliases") val aliases: List<AliasDetailDto>,
    @SerializedName("total") val total: Int? = null,
    @SerializedName("page_id") val pageId: Int? = null
)

data class AliasDetailDto(
    @SerializedName("id") val id: Int,
    @SerializedName("email") val email: String,
    @SerializedName("name") val name: String? = null,
    @SerializedName("note") val note: String? = null,
    @SerializedName("enabled") val enabled: Boolean = true,
    @SerializedName("creation_timestamp") val creationTimestamp: Long? = null,
    @SerializedName("nb_forward") val nbForward: Int = 0,
    @SerializedName("nb_block") val nbBlock: Int = 0
)
