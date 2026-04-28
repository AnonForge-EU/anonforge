package com.anonforge.domain.model

/**
 * Live details for a SimpleLogin alias including forward/block counters.
 * Returned by GET /api/aliases/{id}.
 */
data class AliasDetails(
    val id: Int,
    val email: String,
    val isEnabled: Boolean,
    val name: String?,
    val note: String?,
    val createdAt: Long?,
    val forwardCount: Int,
    val blockCount: Int
)
