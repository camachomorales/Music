package com.example.music.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val email: String,
    val name: String,
    val passwordHash: String,
    val createdAt: Long,
    val isAdmin: Boolean = false
)