package com.example.music.data.model

data class User(
    val id: String,
    val userName: String,
    val email: String,
    val isAdmin: Boolean = false
)