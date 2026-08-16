package com.example.auth

import com.example.data.entity.UserEntity
import com.example.data.remote.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class AuthManager {
    private val supabase = SupabaseClient.client
    private val auth = supabase.auth
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser

    init {
        auth.sessionStatus.onEach { status ->
            when (status) {
                is SessionStatus.Authenticated -> {
                    val user = status.session.user
                    _currentUser.value = UserEntity(
                        id = user?.id ?: "",
                        username = user?.email ?: "unknown",
                        name = user?.userMetadata?.get("full_name")?.toString() ?: "ISP Staff",
                        mobile = user?.phone ?: "",
                        role = "Super Admin" // Default role for now
                    )
                }
                else -> {
                    _currentUser.value = null
                }
            }
        }.launchIn(scope)
    }

    suspend fun signIn(emailInput: String, passwordInput: String): Result<Unit> {
        return try {
            auth.signInWith(Email) {
                email = emailInput
                password = passwordInput
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInAnonymously(): Result<Unit> {
        android.util.Log.d("AuthManager", "Attempting Supabase Anonymous Sign-in...")
        return try {
            auth.signInAnonymously()
            android.util.Log.i("AuthManager", "Supabase Anonymous Sign-in SUCCESS")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("AuthManager", "Supabase Anonymous Sign-in FAILED: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun signOut() {
        auth.signOut()
    }

    fun isLoggedIn(): Boolean = auth.currentSessionOrNull() != null
}
