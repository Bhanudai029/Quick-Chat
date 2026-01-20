package com.example.chattingapp.utils

import java.util.UUID

/**
 * Simple Auth Helper - stores user locally for now
 * Supabase integration can be added later once app is running
 */
object SupabaseAuthHelper {

    interface AuthCallback {
        fun onSuccess(userId: String)
        fun onError(message: String)
    }

    fun signInAndSaveProfile(name: String, callback: AuthCallback) {
        try {
            // Generate a unique user ID for this device
            val userId = UUID.randomUUID().toString()
            
            // For now, just return success with the generated ID
            // Supabase sync can be added later
            callback.onSuccess(userId)
            
        } catch (e: Exception) {
            callback.onError(e.message ?: "Unknown error")
        }
    }
}
