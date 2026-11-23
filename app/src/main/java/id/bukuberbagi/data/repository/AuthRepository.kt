package id.bukuberbagi.data.repository

import id.bukuberbagi.data.SupabaseProvider
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Login, register, logout dengan Supabase Auth (fitur bersama semua anggota). */
class AuthRepository(private val client: SupabaseClient = SupabaseProvider.client) {

    val sessionStatus: StateFlow<SessionStatus> get() = client.auth.sessionStatus

    fun currentUserId(): String? = client.auth.currentUserOrNull()?.id

    suspend fun signIn(email: String, password: String) {
        client.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    /**
     * Mendaftarkan akun baru. Nama lengkap dikirim sebagai metadata, lalu trigger
     * handle_new_user di database membuat baris di tabel profiles.
     * @return true jika langsung login (konfirmasi email dimatikan di Supabase).
     */
    suspend fun signUp(fullName: String, email: String, password: String): Boolean {
        client.auth.signUpWith(Email) {
            this.email = email
            this.password = password
            data = buildJsonObject { put("full_name", fullName) }
        }
        return client.auth.currentSessionOrNull() != null
    }

    suspend fun signOut() {
        client.auth.signOut()
    }
}
