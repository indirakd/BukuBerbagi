package id.bukuberbagi.data.repository

import id.bukuberbagi.data.SupabaseProvider
import id.bukuberbagi.data.model.AdminStats
import id.bukuberbagi.data.model.Book
import id.bukuberbagi.data.model.BookStatus
import id.bukuberbagi.data.model.Category
import id.bukuberbagi.data.model.CategoryInput
import id.bukuberbagi.data.model.DonationRequest
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order

/**
 * ANGGOTA 2 - Catalog Management
 * Tabel: books (Read katalog + pencarian) dan categories (Create, Read, Update, Delete oleh admin).
 */
class CatalogRepository(private val client: SupabaseClient = SupabaseProvider.client) {

    /** READ katalog: hanya buku AVAILABLE, bisa dicari per judul/penulis dan difilter kategori. */
    suspend fun getAvailableBooks(query: String = "", categoryId: String? = null, maxItems: Long? = null): List<Book> {
        val keyword = query.trim().replace(Regex("[,()%*]"), " ").trim()
        return client.from(BookRepository.TABLE).select(Columns.raw(BookRepository.BOOK_COLUMNS)) {
            filter {
                eq("status", BookStatus.AVAILABLE)
                if (categoryId != null) eq("category_id", categoryId)
                if (keyword.isNotEmpty()) {
                    or {
                        ilike("title", "%$keyword%")
                        ilike("author", "%$keyword%")
                    }
                }
            }
            order("created_at", Order.DESCENDING)
            if (maxItems != null) limit(maxItems)
        }.decodeList<Book>()
    }

    suspend fun getBook(id: String): Book =
        client.from(BookRepository.TABLE).select(Columns.raw(BookRepository.BOOK_COLUMNS)) {
            filter { eq("id", id) }
        }.decodeSingle<Book>()

    /** Admin: semua buku, opsional difilter status. */
    suspend fun getAllBooks(status: String? = null): List<Book> =
        client.from(BookRepository.TABLE).select(Columns.raw(BookRepository.BOOK_COLUMNS)) {
            if (status != null) filter { eq("status", status) }
            order("created_at", Order.DESCENDING)
        }.decodeList<Book>()

    // ------------------------------ Kategori ------------------------------

    /** READ kategori. */
    suspend fun getCategories(): List<Category> =
        client.from(CATEGORY_TABLE).select {
            order("category_name", Order.ASCENDING)
        }.decodeList<Category>()

    suspend fun getCategory(id: String): Category =
        client.from(CATEGORY_TABLE).select {
            filter { eq("id", id) }
        }.decodeSingle<Category>()

    /** CREATE kategori (RLS: hanya admin). */
    suspend fun addCategory(name: String, description: String?, icon: String): Category =
        client.from(CATEGORY_TABLE).insert(CategoryInput(name, description, icon)) { select() }
            .decodeSingle<Category>()

    /** UPDATE kategori (RLS: hanya admin). */
    suspend fun updateCategory(id: String, name: String, description: String?, icon: String) {
        client.from(CATEGORY_TABLE).update(CategoryInput(name, description, icon)) {
            filter { eq("id", id) }
        }
    }

    /** DELETE kategori. Ditolak database bila masih dipakai buku. */
    suspend fun deleteCategory(id: String) {
        client.from(CATEGORY_TABLE).delete {
            filter { eq("id", id) }
        }
    }

    // ------------------------------ Admin ------------------------------

    suspend fun getAdminStats(): AdminStats =
        client.postgrest.rpc("admin_stats").decodeList<AdminStats>().firstOrNull() ?: AdminStats()

    suspend fun getRecentRequests(maxItems: Long = 5): List<DonationRequest> =
        client.from(DonationRequestRepository.TABLE).select(Columns.raw(DonationRequestRepository.REQUEST_COLUMNS)) {
            order("created_at", Order.DESCENDING)
            limit(maxItems)
        }.decodeList<DonationRequest>()

    companion object {
        const val CATEGORY_TABLE = "categories"
    }
}
