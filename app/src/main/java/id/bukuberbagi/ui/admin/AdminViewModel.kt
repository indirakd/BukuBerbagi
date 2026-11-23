package id.bukuberbagi.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.bukuberbagi.data.model.AdminStats
import id.bukuberbagi.data.model.Book
import id.bukuberbagi.data.model.Category
import id.bukuberbagi.data.model.DonationRequest
import id.bukuberbagi.data.repository.CatalogRepository
import id.bukuberbagi.util.toUserMessage
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminUiState(
    val loading: Boolean = false,
    val stats: AdminStats = AdminStats(),
    val categories: List<Category> = emptyList(),
    val books: List<Book> = emptyList(),
    val recentRequests: List<DonationRequest> = emptyList(),
    val statusFilter: String? = null,
    // form kategori
    val editingCategory: Category? = null,
    val saving: Boolean = false,
    val nameError: String? = null,
    val savedEvent: Boolean = false,
    val message: String? = null
) {
    val countByCategory: Map<String, Int> get() = books.groupingBy { it.categoryId }.eachCount()
    val filteredBooks: List<Book> get() = if (statusFilter == null) books else books.filter { it.status == statusFilter }
}

/**
 * ANGGOTA 2 - AdminViewModel
 * Dasbor admin, pengelolaan kategori (Create, Read, Update, Delete), dan daftar semua buku.
 */
class AdminViewModel : ViewModel() {
    private val repo = CatalogRepository()

    private val _state = MutableStateFlow(AdminUiState())
    val state: StateFlow<AdminUiState> = _state.asStateFlow()

    fun loadDashboard() = viewModelScope.launch {
        _state.update { it.copy(loading = true) }
        runCatching {
            // coroutineScope agar error dari salah satu async tertangkap runCatching (tidak membuat crash)
            coroutineScope {
                val stats = async { repo.getAdminStats() }
                val cats = async { repo.getCategories() }
                val books = async { repo.getAllBooks() }
                val recent = async { repo.getRecentRequests() }
                AdminUiState(stats = stats.await(), categories = cats.await(), books = books.await(), recentRequests = recent.await())
            }
        }.onSuccess { loaded ->
            _state.update {
                it.copy(
                    loading = false, stats = loaded.stats, categories = loaded.categories,
                    books = loaded.books, recentRequests = loaded.recentRequests
                )
            }
        }.onFailure { e -> _state.update { it.copy(loading = false, message = e.toUserMessage()) } }
    }

    /** READ kategori + jumlah buku per kategori. */
    fun loadCategories() = viewModelScope.launch {
        _state.update { it.copy(loading = true) }
        runCatching { repo.getCategories() to repo.getAllBooks() }
            .onSuccess { (cats, books) -> _state.update { it.copy(loading = false, categories = cats, books = books) } }
            .onFailure { e -> _state.update { it.copy(loading = false, message = e.toUserMessage()) } }
    }

    fun loadBooks() = viewModelScope.launch {
        _state.update { it.copy(loading = true) }
        runCatching { repo.getAllBooks() }
            .onSuccess { books -> _state.update { it.copy(loading = false, books = books) } }
            .onFailure { e -> _state.update { it.copy(loading = false, message = e.toUserMessage()) } }
    }

    fun setStatusFilter(status: String?) = _state.update { it.copy(statusFilter = status) }

    fun loadCategoryForEdit(id: String?) = viewModelScope.launch {
        if (id.isNullOrBlank()) {
            _state.update { it.copy(editingCategory = null) }
            return@launch
        }
        runCatching { repo.getCategory(id) }
            .onSuccess { c -> _state.update { it.copy(editingCategory = c) } }
            .onFailure { e -> _state.update { it.copy(message = e.toUserMessage()) } }
    }

    /** CREATE / UPDATE kategori. */
    fun saveCategory(name: String, description: String, icon: String) {
        val n = name.trim()
        if (n.isEmpty()) {
            _state.update { it.copy(nameError = "Nama kategori wajib diisi") }
            return
        }
        val editing = _state.value.editingCategory
        _state.update { it.copy(saving = true, nameError = null) }
        viewModelScope.launch {
            runCatching {
                val desc = description.trim().ifBlank { null }
                if (editing == null) {
                    repo.addCategory(n, desc, icon)
                    "Kategori \"$n\" ditambahkan dan langsung muncul di katalog"
                } else {
                    repo.updateCategory(editing.id, n, desc, icon)
                    "Kategori diperbarui"
                }
            }.onSuccess { msg ->
                _state.update { it.copy(saving = false, savedEvent = true, message = msg) }
            }.onFailure { e ->
                val msg = e.toUserMessage()
                _state.update {
                    if (msg == "Nama kategori sudah ada") it.copy(saving = false, nameError = msg)
                    else it.copy(saving = false, message = msg)
                }
            }
        }
    }

    /** DELETE kategori (ditolak database bila masih dipakai buku). */
    fun deleteCategory(category: Category) = viewModelScope.launch {
        runCatching { repo.deleteCategory(category.id) }
            .onSuccess {
                _state.update { s ->
                    s.copy(categories = s.categories.filterNot { it.id == category.id }, message = "Kategori dihapus")
                }
            }
            .onFailure { e -> _state.update { it.copy(message = e.toUserMessage()) } }
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }
    fun consumeSaved() = _state.update { it.copy(savedEvent = false) }
}
