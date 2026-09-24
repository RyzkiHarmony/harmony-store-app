package com.harmony.tokoharmony.data.local.mapper

import com.harmony.tokoharmony.core.database.entity.CategoryEntity
import com.harmony.tokoharmony.domain.model.Category

fun CategoryEntity.toDomain(): Category {
    return Category(
        categoryId = categoryId,
        name = name,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun Category.toEntity(): CategoryEntity {
    return CategoryEntity(
        categoryId = categoryId,
        name = name,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
