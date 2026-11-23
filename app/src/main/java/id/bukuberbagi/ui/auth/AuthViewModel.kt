package id.bukuberbagi.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.bukuberbagi.data.SupabaseProvider
import id.bukuberbagi.data.model.Profile
import id.bukuberbagi.data.repository.AuthRepository
import id.bukuberbagi.data.repository.ProfileRepository
import id.bukuberbagi.util.toUserMessage
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface SessionState {
    data object Checking : SessionState
    data object LoggedOut : SessionState
    data class LoggedIn(val profile: Profile) : SessionState
}

data class AuthFormState(
    val loading: Boolean = false,
    val nameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmError: String? = null,
    val error: String? = null,
    val info: String? = null
)

/**
 * ViewModel autentikasi (fitur bersama): login, register, logout, menjaga sesi,
 * dan menyimpan profil pengguna aktif (termasuk role USER/ADMIN untuk navigasi).
 */
class AuthViewModel : ViewModel() {

    private val _session = MutableStateFlow<SessionState>(SessionState.Checking)
    val session: StateFlow<SessionState> = _session.asStateFlow()

    private val _form = MutableStateFlow(AuthFormState())
    val form: StateFlow<AuthFormState> = _form.asStateFlow()

    private val authRepo by lazy { AuthRepository() }
    private val profileRepo by lazy { ProfileRepository() }

    init {
        if (!SupabaseProvider.isConfigured) {
            _session.value = SessionState.LoggedOut
            _form.update {
                it.copy(error = "Supabase belum dikonfigurasi. Isi SUPABASE_URL dan SUPABASE_ANON_KEY di local.properties, lalu jalankan ulang.")
            }
        } else {
            viewModelScope.launch {
                authRepo.sessionStatus.collect { status ->
                    when (status) {
                        is SessionStatus.Authenticated -> {
                            val current = _session.value
                            if (current !is SessionState.LoggedIn || current.profile.id != status.session.user?.id) {
                                loadProfile()
                            }
                        }
                        is SessionStatus.NotAuthenticated -> _session.value = SessionState.LoggedOut
                        is SessionStatus.RefreshFailure -> {
                            // Token gagal diperbarui (biasanya karena offline). Tetap pakai profil terakhir bila ada.
                            if (_session.value is SessionState.Checking) _session.value = SessionState.LoggedOut
                        }
                        else -> Unit // Initializing
                    }
                }
            }
        }
    }

    private suspend fun loadProfile() {
        // Profil dibuat oleh trigger database; beri kesempatan beberapa kali bila baru mendaftar.
        repeat(3) { attempt ->
            val result = runCatching { profileRepo.getMyProfile() }
            if (result.isSuccess) {
                _session.value = SessionState.LoggedIn(result.getOrThrow())
                _form.value = AuthFormState()
                return
            }
            if (attempt == 2) {
                _form.update { it.copy(loading = false, error = result.exceptionOrNull()?.toUserMessage()) }
                runCatching { authRepo.signOut() }
                _session.value = SessionState.LoggedOut
            } else delay(700)
        }
    }

    fun refreshProfile() = viewModelScope.launch {
        runCatching { profileRepo.getMyProfile() }.onSuccess { _session.value = SessionState.LoggedIn(it) }
    }

    fun updateLocalProfile(profile: Profile) {
        _session.value = SessionState.LoggedIn(profile)
    }

    fun login(email: String, password: String) {
        val e = email.trim()
        val emailError = if (!EMAIL.matches(e)) "Format email belum benar" else null
        val passError = if (password.length < 6) "Kata sandi minimal 6 karakter" else null
        if (emailError != null || passError != null) {
            _form.value = AuthFormState(emailError = emailError, passwordError = passError)
            return
        }
        if (!SupabaseProvider.isConfigured) return
        _form.value = AuthFormState(loading = true)
        viewModelScope.launch {
            runCatching { authRepo.signIn(e, password) }
                .onFailure { err -> _form.value = AuthFormState(error = err.toUserMessage()) }
            // Jika berhasil, sessionStatus menjadi Authenticated lalu profil dimuat otomatis.
        }
    }

    fun register(name: String, email: String, password: String, confirm: String) {
        val n = name.trim()
        val e = email.trim()
        val state = AuthFormState(
            nameError = if (n.isEmpty()) "Nama wajib diisi" else null,
            emailError = if (!EMAIL.matches(e)) "Format email belum benar" else null,
            passwordError = if (password.length < 6) "Kata sandi minimal 6 karakter" else null,
            confirmError = if (password != confirm) "Kata sandi tidak sama" else null
        )
        if (listOf(state.nameError, state.emailError, state.passwordError, state.confirmError).any { it != null }) {
            _form.value = state
            return
        }
        if (!SupabaseProvider.isConfigured) return
        _form.value = AuthFormState(loading = true)
        viewModelScope.launch {
            runCatching { authRepo.signUp(n, e, password) }
                .onSuccess { loggedIn ->
                    if (!loggedIn) {
                        _form.value = AuthFormState(
                            info = "Akun berhasil dibuat. Buka email konfirmasi dari Supabase, lalu login."
                        )
                    }
                }
                .onFailure { err -> _form.value = AuthFormState(error = err.toUserMessage()) }
        }
    }

    fun logout() = viewModelScope.launch {
        runCatching { authRepo.signOut() }
        _session.value = SessionState.LoggedOut
    }

    fun clearFormMessages() {
        _form.value = AuthFormState()
    }

    companion object {
        private val EMAIL = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    }
}
