package id.bukuberbagi.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.bukuberbagi.data.model.Book
import id.bukuberbagi.data.model.Category
import id.bukuberbagi.data.model.RequestStatus
import id.bukuberbagi.data.repository.CatalogRepository
import id.bukuberbagi.data.repository.DonationRequestRepository
import id.bukuberbagi.util.toUserMessage
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val loading: Boolean = true,
    val categories: List<Category> = emptyList(),
    val latestBooks: List<Book> = emptyList(),
    val myActiveRequests: Int = 0,
    val incomingPending: Int = 0,
    val message: String? = null
)

/** Beranda: kategori, buku terbaru, dan ringkasan aktivitas permintaan. */
class HomeViewModel : ViewModel() {
    private val catalogRepo = CatalogRepository()
    private val requestRepo = DonationRequestRepository()

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    fun load() = viewModelScope.launch {
        _state.update { it.copy(loading = it.latestBooks.isEmpty()) }
        runCatching {
            coroutineScope {
                val cats = async { catalogRepo.getCategories() }
                val books = async { catalogRepo.getAvailableBooks(maxItems = 10) }
                val mine = async { requestRepo.getMyRequests() }
                val incoming = async { requestRepo.getIncomingRequests() }
                HomeUiState(
                    loading = false,
                    categories = cats.await(),
                    latestBooks = books.await(),
                    myActiveRequests = mine.await().count { it.status in RequestStatus.ACTIVE },
                    incomingPending = incoming.await().count { it.status == RequestStatus.PENDING }
                )
            }
        }.onSuccess { loaded -> _state.value = loaded }
            .onFailure { e -> _state.update { it.copy(loading = false, message = e.toUserMessage()) } }
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }
}
