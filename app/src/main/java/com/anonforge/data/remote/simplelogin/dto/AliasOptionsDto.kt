package com.anonforge.data.remote.simplelogin.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO for available alias domain options.
 */
data class AliasOptionsDto(
    @SerializedName("can_create")
    val canCreate: Boolean,
    
    @SerializedName("suffixes")
    val suffixes: List<SuffixDto>,
    
    @SerializedName("prefix_suggestion")
    val prefixSuggestion: String?
)

data class SuffixDto(
    @SerializedName("suffix")
    val suffix: String,
    
    @SerializedName("signed_suffix")
    val signedSuffix: String,
    
    @SerializedName("is_custom")
    val isCustom: Boolean,
    
    @SerializedName("is_premium")
    val isPremium: Boolean
)
