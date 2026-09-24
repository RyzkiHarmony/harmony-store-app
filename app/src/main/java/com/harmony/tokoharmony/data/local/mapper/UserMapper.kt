package com.harmony.tokoharmony.data.local.mapper

import com.harmony.tokoharmony.core.database.entity.UserEntity
import com.harmony.tokoharmony.domain.model.Role
import com.harmony.tokoharmony.domain.model.User

fun UserEntity.toDomain(): User {
    return User(
        userId = userId,
        displayName = displayName,
        role = try {
            Role.valueOf(role)
        } catch (_: Exception) {
            Role.CASHIER
        },
        pinHash = pinHash,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun User.toEntity(): UserEntity {
    return UserEntity(
        userId = userId,
        displayName = displayName,
        role = role.name,
        pinHash = pinHash,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
