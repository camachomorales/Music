package com.example.music.data.model

enum class AccountType {
    GUEST,      // Usuario no autenticado
    FREE,       // Usuario registrado gratuito
    PREMIUM,    // Usuario premium (para futuro)
    LOCAL,      // Usuario con cuenta local (sin Firebase)
    ADMIN       // Desarrollador/Admin
}