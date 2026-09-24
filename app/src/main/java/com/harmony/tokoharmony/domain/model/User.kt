package com.harmony.tokoharmony.domain.model

data class User(
    val userId: String,
    val displayName: String,
    val role: Role,
    val pinHash: String?,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
