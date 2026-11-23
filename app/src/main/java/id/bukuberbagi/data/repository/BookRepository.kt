package id.bukuberbagi.data.repository

import id.bukuberbagi.data.SupabaseProvider
import id.bukuberbagi.data.model.Book
import id.bukuberbagi.data.model.BookInput
import id.bukuberbagi.data.model.BookUpdate
import id.bukuberbagi.data.model.Category
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order

/**
 * ANGGOTA 1 - Book Management
 * Tabel: books (Create, Read, Update, Delete) + upload foto ke bucket book-images.
 */
class BookRepository(
    private val client: SupabaseClient = SupabaseProvider.client,
    private val storage: StorageRepository = StorageRepository(client)
) {
    private fun uid() = client.auth.currentUserOrNull()?.id ?: error("Sesi login berakhir. Silakan login lagi.")

    /** READ: daftar buku milik pengguna yang sedang login. */
    suspend fun getMyBooks(): List<Book> =
        client.from(TABLE).select(Columns.raw(BOOK_COLUMNS)) {
            filter { eq("user_id", uid()) }
            order("created_at", Order.DESCENDING)
        }.decodeList<Book>()

    suspend fun getBook(id: String): Book =
        client.from(TABLE).select(Columns.raw(BOOK_COLUMNS)) {
            filter { eq("id", id) }
        }.decodeSingle<Book>()

    /** Kategori untuk pilihan di form (dibaca dari tabel milik Anggota 2). */
    suspend fun getCategories(): List<Category> =
        client.from("categories").select {
            order("category_name", Order.ASCENDING)
        }.decodeList<Category>()

    /** CREATE: upload foto lalu simpan baris baru di tabel books. */
    suspend fun addBook(
        categoryId: String,
        title: String,
        author: String,
        description: String?,
        condition: String,
        location: String?,
        photo: ByteArray
    ): Book {
        val imageUrl = storage.uploadJpeg(StorageRepository.BOOK_IMAGES, photo, "buku")
        val input = BookInput(
            userId = uid(),
            categoryId = categoryId,
            title = title,
            author = author,
            description = description,
            condition = condition,
            imageUrl = imageUrl,
            location = location
        )
        return client.from(TABLE).insert(input) { select() }.decodeSingle<Book>()
    }

    /** UPDATE: perbarui informasi buku (foto baru opsional). */
    suspend fun updateBook(
        book: Book,
        categoryId: String,
        title: String,
        author: String,
        description: String?,
        condition: String,
        location: String?,
        newPhoto: ByteArray?
    ) {
        val imageUrl = if (newPhoto != null) {
            storage.uploadJpeg(StorageRepository.BOOK_IMAGES, newPhoto, "buku")
        } else book.imageUrl
        val update = BookUpdate(categoryId, title, author, description, condition, imageUrl, location)
        client.from(TABLE).update(update) {
            filter {
                eq("id", book.id)
                eq("user_id", uid())
            }
        }
    }

    /**
     * DELETE: hapus buku. Database menolak jika buku sudah punya permintaan yang disetujui
     * (trigger guard_book_delete). Permintaan yang masih menunggu ikut terhapus (ON DELETE CASCADE).
     */
    suspend fun deleteBook(id: String) {
        client.from(TABLE).delete {
            filter {
                eq("id", id)
                eq("user_id", uid())
            }
        }
    }

    companion object {
        const val TABLE = "books"
        const val BOOK_COLUMNS = "*, categories(category_name, icon), profiles(full_name, profile_image_url, location)"
    }
}
