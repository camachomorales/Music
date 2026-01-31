package com.example.music.data.model

data class UserSession(
    val userId: String,
    val name: String,
    val email: String,
    val isAdmin: Boolean = false,
    val accountType: AccountType = AccountType.FREE,
    val createdAt: Long = System.currentTimeMillis(),
    val passwordHash: String? = null
)