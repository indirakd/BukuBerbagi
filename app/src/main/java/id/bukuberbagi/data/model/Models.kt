package id.bukuberbagi.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------------------------------------------------------------------
// Konstanta status (harus sama dengan CHECK constraint di 01_schema.sql)
// ---------------------------------------------------------------------
object BookStatus {
    const val AVAILABLE = "AVAILABLE"
    const val RESERVED = "RESERVED"
    const val DONATED = "DONATED"
}

object RequestStatus {
    const val PENDING = "PENDING"
    const val APPROVED = "APPROVED"
    const val REJECTED = "REJECTED"
    const val CANCELLED = "CANCELLED"
    const val COMPLETED = "COMPLETED"
    val ACTIVE = listOf(PENDING, APPROVED)
}

enum class BookCondition(val code: String, val label: String) {
    BARU("BARU", "Baru"),
    SEPERTI_BARU("SEPERTI_BARU", "Seperti baru"),
    LAYAK_BACA("LAYAK_BACA", "Layak baca");

    companion object {
        fun labelOf(code: String) = entries.firstOrNull { it.code == code }?.label ?: code
    }
}

enum class HandoverMethod(val code: String, val label: String) {
    AMBIL_DI_LOKASI("AMBIL_DI_LOKASI", "Ambil di lokasi"),
    COD("COD", "Bertemu (COD)"),
    DIKIRIM("DIKIRIM", "Dikirim");

    companion object {
        fun labelOf(code: String?) = entries.firstOrNull { it.code == code }?.label ?: "-"
    }
}

// ---------------------------------------------------------------------
// Tabel profiles
// ---------------------------------------------------------------------
@Serializable
data class Profile(
    val id: String,
    @SerialName("full_name") val fullName: String,
    val email: String? = null,
    @SerialName("profile_image_url") val profileImageUrl: String? = null,
    val location: String? = null,
    val role: String = "USER",
    @SerialName("created_at") val createdAt: String? = null
) {
    val isAdmin: Boolean get() = role == "ADMIN"
    val firstName: String get() = fullName.trim().substringBefore(" ")
}

@Serializable
data class ProfileUpdate(
    @SerialName("full_name") val fullName: String,
    val location: String?,
    @SerialName("profile_image_url") val profileImageUrl: String?
)

/** Potongan profil yang ikut di-join ke data buku / permintaan. */
@Serializable
data class ProfileRef(
    @SerialName("full_name") val fullName: String = "",
    @SerialName("profile_image_url") val profileImageUrl: String? = null,
    val location: String? = null
)

// ---------------------------------------------------------------------
// Tabel categories (Anggota 2)
// ---------------------------------------------------------------------
@Serializable
data class Category(
    val id: String,
    @SerialName("category_name") val name: String,
    val description: String? = null,
    val icon: String = "bookmark",
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class CategoryInput(
    @SerialName("category_name") val name: String,
    val description: String?,
    val icon: String
)

@Serializable
data class CategoryRef(
    @SerialName("category_name") val name: String = "",
    val icon: String = "bookmark"
)

// ---------------------------------------------------------------------
// Tabel books (Anggota 1)
// ---------------------------------------------------------------------
@Serializable
data class Book(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("category_id") val categoryId: String,
    val title: String,
    val author: String,
    val description: String? = null,
    val condition: String,
    @SerialName("image_url") val imageUrl: String,
    val location: String? = null,
    val status: String = BookStatus.AVAILABLE,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("categories") val category: CategoryRef? = null,
    @SerialName("profiles") val owner: ProfileRef? = null
) {
    val conditionLabel: String get() = BookCondition.labelOf(condition)
    val city: String get() = location?.substringAfterLast(",")?.trim().orEmpty()
}

@Serializable
data class BookInput(
    @SerialName("user_id") val userId: String,
    @SerialName("category_id") val categoryId: String,
    val title: String,
    val author: String,
    val description: String?,
    val condition: String,
    @SerialName("image_url") val imageUrl: String,
    val location: String?
)

@Serializable
data class BookUpdate(
    @SerialName("category_id") val categoryId: String,
    val title: String,
    val author: String,
    val description: String?,
    val condition: String,
    @SerialName("image_url") val imageUrl: String,
    val location: String?
)

@Serializable
data class BookRef(
    val id: String = "",
    val title: String = "",
    val author: String = "",
    @SerialName("image_url") val imageUrl: String? = null,
    val status: String = BookStatus.AVAILABLE,
    val location: String? = null
)

// ---------------------------------------------------------------------
// Tabel donation_requests & handover_confirmations (Anggota 3)
// ---------------------------------------------------------------------
@Serializable
data class DonationRequest(
    val id: String,
    @SerialName("book_id") val bookId: String,
    @SerialName("requester_id") val requesterId: String,
    @SerialName("donor_id") val donorId: String,
    val reason: String,
    val status: String,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("books") val book: BookRef? = null,
    val requester: ProfileRef? = null,
    val donor: ProfileRef? = null
)

@Serializable
data class DonationRequestInput(
    @SerialName("book_id") val bookId: String,
    @SerialName("requester_id") val requesterId: String,
    @SerialName("donor_id") val donorId: String,
    val reason: String
)

@Serializable
data class HandoverConfirmation(
    val id: String,
    @SerialName("request_id") val requestId: String,
    @SerialName("donor_confirmed") val donorConfirmed: Boolean = false,
    @SerialName("requester_confirmed") val requesterConfirmed: Boolean = false,
    @SerialName("handover_method") val handoverMethod: String? = null,
    @SerialName("handover_date") val handoverDate: String? = null,
    val notes: String? = null
)

// ---------------------------------------------------------------------
// Hasil RPC admin_stats()
// ---------------------------------------------------------------------
@Serializable
data class AdminStats(
    @SerialName("total_users") val totalUsers: Long = 0,
    @SerialName("available_books") val availableBooks: Long = 0,
    @SerialName("active_requests") val activeRequests: Long = 0,
    @SerialName("donated_books") val donatedBooks: Long = 0
)
