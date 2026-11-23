package id.bukuberbagi.ui.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.bukuberbagi.data.model.Book
import id.bukuberbagi.data.model.Category
import id.bukuberbagi.data.model.DonationRequest
import id.bukuberbagi.data.repository.CatalogRepository
import id.bukuberbagi.data.repository.DonationRequestRepository
import id.bukuberbagi.util.toUserMessage
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CatalogUiState(
    val query: String = "",
    val selectedCategoryId: String? = null,
    val categories: List<Category> = emptyList(),
    val books: List<Book> = emptyList(),
    val loading: Boolean = true,
    val message: String? = null
) {
    val selectedCategoryName: String? get() = categories.firstOrNull { it.id == selectedCategoryId }?.name
}

/**
 * ANGGOTA 2 - CatalogViewModel
 * Katalog buku tersedia, pencarian judul/penulis, dan filter kategori. Semua langsung dari database.
 */
@OptIn(FlowPreview::class)
class CatalogViewModel : ViewModel() {
    private val repo = CatalogRepository()

    private val _state = MutableStateFlow(CatalogUiState())
    val state: StateFlow<CatalogUiState> = _state.asStateFlow()

    private val queryFlow = MutableStateFlow("")
    private val categoryFlow = MutableStateFlow<String?>(null)
    private var started = false

    fun start(initialCategoryId: String?) {
        if (started) return
        started = true
        categoryFlow.value = initialCategoryId
        _state.update { it.copy(selectedCategoryId = initialCategoryId) }
        loadCategories()
        viewModelScope.launch {
            combine(queryFlow.debounce(350), categoryFlow) { q, c -> q to c }
                .collectLatest { (q, c) -> loadBooks(q, c) }
        }
    }

    private fun loadCategories() = viewModelScope.launch {
        runCatching { repo.getCategories() }
            .onSuccess { cats -> _state.update { it.copy(categories = cats) } }
            .onFailure { e -> _state.update { it.copy(message = e.toUserMessage()) } }
    }

    private suspend fun loadBooks(query: String, categoryId: String?) {
        _state.update { it.copy(loading = true) }
        runCatching { repo.getAvailableBooks(query, categoryId) }
            .onSuccess { books -> _state.update { it.copy(loading = false, books = books) } }
            .onFailure { e -> _state.update { it.copy(loading = false, message = e.toUserMessage()) } }
    }

    fun onQueryChange(value: String) {
        _state.update { it.copy(query = value) }
        queryFlow.value = value
    }

    fun selectCategory(id: String?) {
        _state.update { it.copy(selectedCategoryId = id) }
        categoryFlow.value = id
    }

    fun refresh() = viewModelScope.launch {
        loadBooks(queryFlow.value, categoryFlow.value)
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }
}

// =====================================================================

data class BookDetailUiState(
    val loading: Boolean = true,
    val book: Book? = null,
    val myActiveRequest: DonationRequest? = null,
    val currentUserId: String? = null,
    val message: String? = null
) {
    val isOwner: Boolean get() = book != null && book.userId == currentUserId
}

/** ANGGOTA 2 - detail buku dari katalog. */
class BookDetailViewModel : ViewModel() {
    private val repo = CatalogRepository()
    private val requestRepo = DonationRequestRepository()

    private val _state = MutableStateFlow(BookDetailUiState())
    val state: StateFlow<BookDetailUiState> = _state.asStateFlow()

    fun load(bookId: String) = viewModelScope.launch {
        _state.update { it.copy(loading = true, currentUserId = requestRepo.currentUserId()) }
        runCatching {
            val book = repo.getBook(bookId)
            val active = runCatching { requestRepo.getMyActiveRequestFor(bookId) }.getOrNull()
            book to active
        }.onSuccess { (book, active) ->
            _state.update { it.copy(loading = false, book = book, myActiveRequest = active) }
        }.onFailure { e ->
            _state.update { it.copy(loading = false, message = e.toUserMessage()) }
        }
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }
}
