package id.bukuberbagi.ui.request

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.bukuberbagi.data.model.Book
import id.bukuberbagi.data.model.DonationRequest
import id.bukuberbagi.data.model.HandoverConfirmation
import id.bukuberbagi.data.model.RequestStatus
import id.bukuberbagi.data.repository.CatalogRepository
import id.bukuberbagi.data.repository.DonationRequestRepository
import id.bukuberbagi.util.DateUtils
import id.bukuberbagi.util.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class RequestUiState(
    val loading: Boolean = false,
    val currentUserId: String? = null,
    // daftar
    val myRequests: List<DonationRequest> = emptyList(),
    val incomingRequests: List<DonationRequest> = emptyList(),
    // form permintaan
    val book: Book? = null,
    val reasonError: String? = null,
    val submitting: Boolean = false,
    val createdEvent: Boolean = false,
    // detail & serah terima
    val request: DonationRequest? = null,
    val handover: HandoverConfirmation? = null,
    val actionLoading: Boolean = false,
    val scheduleError: String? = null,
    val message: String? = null
) {
    val isDonor: Boolean get() = request != null && request.donorId == currentUserId
    val isRequester: Boolean get() = request != null && request.requesterId == currentUserId
    val myActiveCount: Int get() = myRequests.count { it.status in RequestStatus.ACTIVE }
    val incomingPendingCount: Int get() = incomingRequests.count { it.status == RequestStatus.PENDING }
}

/**
 * ANGGOTA 3 - DonationRequestViewModel
 * Ajukan permintaan, daftar Permintaan saya / Permintaan masuk, setujui, tolak, batalkan,
 * atur jadwal, dan konfirmasi serah terima.
 */
class DonationRequestViewModel : ViewModel() {
    private val repo = DonationRequestRepository()
    private val catalogRepo = CatalogRepository()

    private val _state = MutableStateFlow(RequestUiState(currentUserId = repo.currentUserId()))
    val state: StateFlow<RequestUiState> = _state.asStateFlow()

    // ------------------------------ Daftar ------------------------------
    /** READ: Permintaan saya + Permintaan masuk. */
    fun loadLists() = viewModelScope.launch {
        _state.update { it.copy(loading = true, currentUserId = repo.currentUserId()) }
        runCatching { repo.getMyRequests() to repo.getIncomingRequests() }
            .onSuccess { (mine, incoming) ->
                _state.update { it.copy(loading = false, myRequests = mine, incomingRequests = incoming) }
            }
            .onFailure { e -> _state.update { it.copy(loading = false, message = e.toUserMessage()) } }
    }

    // ------------------------------ Form permintaan ------------------------------
    fun loadBook(bookId: String) = viewModelScope.launch {
        _state.update { it.copy(loading = true) }
        runCatching { catalogRepo.getBook(bookId) }
            .onSuccess { b -> _state.update { it.copy(loading = false, book = b) } }
            .onFailure { e -> _state.update { it.copy(loading = false, message = e.toUserMessage()) } }
    }

    /** CREATE: simpan permintaan baru (status PENDING). */
    fun submitRequest(reason: String) {
        val book = _state.value.book ?: return
        val text = reason.trim()
        if (text.length < 10) {
            _state.update { it.copy(reasonError = "Tulis alasan minimal 10 karakter") }
            return
        }
        _state.update { it.copy(submitting = true, reasonError = null) }
        viewModelScope.launch {
            runCatching { repo.createRequest(book.id, book.userId, text) }
                .onSuccess {
                    _state.update {
                        it.copy(submitting = false, createdEvent = true, message = "Permintaan terkirim. Cek di Permintaan saya.")
                    }
                }
                .onFailure { e -> _state.update { it.copy(submitting = false, reasonError = e.toUserMessage()) } }
        }
    }

    fun consumeCreated() = _state.update { it.copy(createdEvent = false) }

    // ------------------------------ Detail ------------------------------
    fun loadDetail(requestId: String) = viewModelScope.launch {
        _state.update { it.copy(loading = it.request == null, currentUserId = repo.currentUserId()) }
        runCatching {
            val r = repo.getRequest(requestId)
            val h = if (r.status == RequestStatus.APPROVED || r.status == RequestStatus.COMPLETED) repo.getHandover(requestId) else null
            r to h
        }.onSuccess { (r, h) ->
            _state.update { it.copy(loading = false, request = r, handover = h) }
        }.onFailure { e -> _state.update { it.copy(loading = false, message = e.toUserMessage()) } }
    }

    fun approve() = runAction("Permintaan disetujui. Buku sekarang berstatus Dipesan.") { repo.approve(it) }
    fun reject() = runAction("Permintaan ditolak. Buku tetap tersedia.") { repo.reject(it) }
    fun cancel() = runAction("Permintaan dibatalkan") { repo.cancel(it) }

    /** Donatur menyimpan tanggal, metode, dan catatan penyerahan. */
    fun saveSchedule(method: String?, date: LocalDate?, notes: String) {
        if (method == null || date == null) {
            _state.update { it.copy(scheduleError = "Pilih tanggal dan metode penyerahan") }
            return
        }
        _state.update { it.copy(scheduleError = null) }
        runAction("Jadwal penyerahan disimpan") {
            repo.setHandoverSchedule(it, method, DateUtils.toIsoForServer(date), notes.trim().ifBlank { null })
        }
    }

    /** Konfirmasi oleh donatur atau penerima. Jika keduanya sudah, status menjadi COMPLETED. */
    fun confirmHandover() {
        val before = _state.value.request?.status
        val requestId = _state.value.request?.id ?: return
        _state.update { it.copy(actionLoading = true) }
        viewModelScope.launch {
            runCatching { repo.confirmHandover(requestId) }
                .onSuccess {
                    loadDetail(requestId).join()
                    val after = _state.value.request?.status
                    val msg = if (before != RequestStatus.COMPLETED && after == RequestStatus.COMPLETED) {
                        "Serah terima selesai. Buku sudah berpindah tangan."
                    } else "Konfirmasi tercatat. Menunggu pihak lain."
                    _state.update { it.copy(actionLoading = false, message = msg) }
                }
                .onFailure { e -> _state.update { it.copy(actionLoading = false, message = e.toUserMessage()) } }
        }
    }

    private fun runAction(successMessage: String, block: suspend (String) -> Unit) {
        val requestId = _state.value.request?.id ?: return
        _state.update { it.copy(actionLoading = true) }
        viewModelScope.launch {
            runCatching { block(requestId) }
                .onSuccess {
                    loadDetail(requestId).join()
                    _state.update { it.copy(actionLoading = false, message = successMessage) }
                }
                .onFailure { e -> _state.update { it.copy(actionLoading = false, message = e.toUserMessage()) } }
        }
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }
}
