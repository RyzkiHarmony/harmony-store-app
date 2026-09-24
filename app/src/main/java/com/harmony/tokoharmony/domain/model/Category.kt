package com.harmony.tokoharmony.domain.model

data class Category(
    val categoryId: String,
    val name: String,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
