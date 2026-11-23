package id.bukuberbagi.ui.book

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.bukuberbagi.data.model.Book
import id.bukuberbagi.data.model.Category
import id.bukuberbagi.data.model.RequestStatus
import id.bukuberbagi.data.repository.BookRepository
import id.bukuberbagi.data.repository.DonationRequestRepository
import id.bukuberbagi.util.toUserMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Isi form tambah/edit buku. */
data class BookForm(
    val title: String = "",
    val author: String = "",
    val categoryId: String? = null,
    val condition: String? = null,
    val description: String = "",
    val location: String = ""
)

data class BookFormErrors(
    val photo: String? = null,
    val title: String? = null,
    val author: String? = null,
    val category: String? = null,
    val condition: String? = null
) {
    val hasError get() = listOf(photo, title, author, category, condition).any { it != null }
}

data class BookUiState(
    val loading: Boolean = false,
    val saving: Boolean = false,
    val myBooks: List<Book> = emptyList(),
    val pendingCounts: Map<String, Int> = emptyMap(),
    val categories: List<Category> = emptyList(),
    val editingBook: Book? = null,
    val errors: BookFormErrors = BookFormErrors(),
    val message: String? = null,
    val savedEvent: Boolean = false
)

/**
 * ANGGOTA 1 - BookViewModel
 * Mengelola daftar buku saya, tambah, edit, dan hapus buku.
 */
class BookViewModel : ViewModel() {
    private val repo = BookRepository()
    private val requestRepo = DonationRequestRepository()

    private val _state = MutableStateFlow(BookUiState())
    val state: StateFlow<BookUiState> = _state.asStateFlow()

    /** READ: buku milik saya + jumlah permintaan masuk yang menunggu. */
    fun loadMyBooks() = viewModelScope.launch {
        _state.update { it.copy(loading = true) }
        runCatching {
            val books = repo.getMyBooks()
            val pending = runCatching { requestRepo.getIncomingRequests() }.getOrDefault(emptyList())
                .filter { it.status == RequestStatus.PENDING }
                .groupingBy { it.bookId }.eachCount()
            books to pending
        }.onSuccess { (books, pending) ->
            _state.update { it.copy(loading = false, myBooks = books, pendingCounts = pending) }
        }.onFailure { e ->
            _state.update { it.copy(loading = false, message = e.toUserMessage()) }
        }
    }

    /** Siapkan form: muat kategori dan (jika edit) data buku. */
    fun loadForm(editBookId: String?) = viewModelScope.launch {
        _state.update { it.copy(loading = true, errors = BookFormErrors()) }
        runCatching {
            val categories = repo.getCategories()
            val book = editBookId?.let { repo.getBook(it) }
            categories to book
        }.onSuccess { (cats, book) ->
            _state.update { it.copy(loading = false, categories = cats, editingBook = book) }
        }.onFailure { e ->
            _state.update { it.copy(loading = false, message = e.toUserMessage()) }
        }
    }

    /**
     * CREATE / UPDATE.
     * [photoBytes] dipanggil di Dispatchers.IO untuk mengompres foto yang dipilih (null jika tidak ada foto baru).
     */
    fun save(form: BookForm, hasNewPhoto: Boolean, photoBytes: suspend () -> ByteArray?) {
        val editing = _state.value.editingBook
        val errors = BookFormErrors(
            photo = if (editing == null && !hasNewPhoto) "Foto buku wajib diunggah" else null,
            title = if (form.title.isBlank()) "Judul wajib diisi" else null,
            author = if (form.author.isBlank()) "Penulis wajib diisi" else null,
            category = if (form.categoryId == null) "Pilih kategori" else null,
            condition = if (form.condition == null) "Pilih kondisi buku" else null
        )
        if (errors.hasError) {
            _state.update { it.copy(errors = errors) }
            return
        }
        _state.update { it.copy(saving = true, errors = BookFormErrors()) }
        viewModelScope.launch {
            runCatching {
                val bytes = withContext(Dispatchers.IO) { photoBytes() }
                val description = form.description.trim().ifBlank { null }
                val location = form.location.trim().ifBlank { null }
                if (editing == null) {
                    repo.addBook(
                        categoryId = form.categoryId!!,
                        title = form.title.trim(),
                        author = form.author.trim(),
                        description = description,
                        condition = form.condition!!,
                        location = location,
                        photo = bytes ?: error("Foto buku wajib diunggah")
                    )
                    "Buku berhasil diunggah. Foto tersimpan di Supabase Storage."
                } else {
                    repo.updateBook(
                        book = editing,
                        categoryId = form.categoryId!!,
                        title = form.title.trim(),
                        author = form.author.trim(),
                        description = description,
                        condition = form.condition!!,
                        location = location,
                        newPhoto = bytes
                    )
                    "Perubahan buku disimpan"
                }
            }.onSuccess { msg ->
                _state.update { it.copy(saving = false, message = msg, savedEvent = true) }
            }.onFailure { e ->
                _state.update { it.copy(saving = false, message = e.toUserMessage()) }
            }
        }
    }

    /** DELETE: ditolak database bila sudah ada permintaan yang disetujui. */
    fun delete(book: Book) = viewModelScope.launch {
        runCatching { repo.deleteBook(book.id) }
            .onSuccess {
                _state.update { s ->
                    s.copy(
                        myBooks = s.myBooks.filterNot { it.id == book.id },
                        message = "Buku dihapus. Permintaan yang menunggu ikut dibatalkan."
                    )
                }
            }
            .onFailure { e -> _state.update { it.copy(message = e.toUserMessage()) } }
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }
    fun consumeSaved() = _state.update { it.copy(savedEvent = false) }
}
