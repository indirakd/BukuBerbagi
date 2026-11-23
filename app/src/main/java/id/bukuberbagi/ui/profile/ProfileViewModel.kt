package id.bukuberbagi.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.bukuberbagi.data.model.Profile
import id.bukuberbagi.data.model.RequestStatus
import id.bukuberbagi.data.repository.BookRepository
import id.bukuberbagi.data.repository.DonationRequestRepository
import id.bukuberbagi.data.repository.ProfileRepository
import id.bukuberbagi.util.toUserMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ProfileUiState(
    val postedCount: Int = 0,
    val donatedCount: Int = 0,
    val receivedCount: Int = 0,
    val saving: Boolean = false,
    val nameError: String? = null,
    val savedProfile: Profile? = null,
    val message: String? = null
)

/** Profil & edit profil + upload foto ke bucket avatars (fitur bersama). */
class ProfileViewModel : ViewModel() {
    private val profileRepo = ProfileRepository()
    private val bookRepo = BookRepository()
    private val requestRepo = DonationRequestRepository()

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    fun loadStats() = viewModelScope.launch {
        runCatching {
            Triple(
                bookRepo.getMyBooks().size,
                requestRepo.getIncomingRequests().count { it.status == RequestStatus.COMPLETED },
                requestRepo.getMyRequests().count { it.status == RequestStatus.COMPLETED }
            )
        }.onSuccess { (posted, donated, received) ->
            _state.update { it.copy(postedCount = posted, donatedCount = donated, receivedCount = received) }
        }
    }

    fun save(current: Profile, name: String, location: String, newPhoto: (suspend () -> ByteArray)?) {
        if (name.isBlank()) {
            _state.update { it.copy(nameError = "Nama wajib diisi") }
            return
        }
        _state.update { it.copy(saving = true, nameError = null) }
        viewModelScope.launch {
            runCatching {
                val bytes = newPhoto?.let { withContext(Dispatchers.IO) { it() } }
                profileRepo.updateMyProfile(name.trim(), location.trim().ifBlank { null }, bytes, current.profileImageUrl)
            }.onSuccess { updated ->
                _state.update { it.copy(saving = false, savedProfile = updated, message = "Profil diperbarui") }
            }.onFailure { e ->
                _state.update { it.copy(saving = false, message = e.toUserMessage()) }
            }
        }
    }

    fun consumeSaved() = _state.update { it.copy(savedProfile = null) }
    fun consumeMessage() = _state.update { it.copy(message = null) }
}
