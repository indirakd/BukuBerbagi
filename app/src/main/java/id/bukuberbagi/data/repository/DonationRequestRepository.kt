package id.bukuberbagi.data.repository

import id.bukuberbagi.data.SupabaseProvider
import id.bukuberbagi.data.model.DonationRequest
import id.bukuberbagi.data.model.DonationRequestInput
import id.bukuberbagi.data.model.HandoverConfirmation
import id.bukuberbagi.data.model.RequestStatus
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * ANGGOTA 3 - Donation Request Management
 * Tabel: donation_requests (Create, Read) dan handover_confirmations (Read).
 * Semua perubahan status lewat RPC agar divalidasi di backend (lihat 01_schema.sql).
 */
class DonationRequestRepository(private val client: SupabaseClient = SupabaseProvider.client) {

    fun currentUserId(): String? = client.auth.currentUserOrNull()?.id
    private fun uid() = currentUserId() ?: error("Sesi login berakhir. Silakan login lagi.")

    /** CREATE: ajukan permintaan. Trigger database memvalidasi aturan bisnis 1-3. */
    suspend fun createRequest(bookId: String, donorId: String, reason: String) {
        client.from(TABLE).insert(DonationRequestInput(bookId, uid(), donorId, reason))
    }

    /** READ: permintaan yang dibuat pengguna (tab "Permintaan saya"). */
    suspend fun getMyRequests(): List<DonationRequest> =
        client.from(TABLE).select(Columns.raw(REQUEST_COLUMNS)) {
            filter { eq("requester_id", uid()) }
            order("created_at", Order.DESCENDING)
        }.decodeList<DonationRequest>()

    /** READ: permintaan yang masuk ke donatur (tab "Permintaan masuk"). */
    suspend fun getIncomingRequests(): List<DonationRequest> =
        client.from(TABLE).select(Columns.raw(REQUEST_COLUMNS)) {
            filter { eq("donor_id", uid()) }
            order("created_at", Order.DESCENDING)
        }.decodeList<DonationRequest>()

    suspend fun getRequest(id: String): DonationRequest =
        client.from(TABLE).select(Columns.raw(REQUEST_COLUMNS)) {
            filter { eq("id", id) }
        }.decodeSingle<DonationRequest>()

    /** Permintaan aktif pengguna untuk buku tertentu (untuk tombol di detail buku). */
    suspend fun getMyActiveRequestFor(bookId: String): DonationRequest? =
        client.from(TABLE).select {
            filter {
                eq("book_id", bookId)
                eq("requester_id", uid())
                isIn("status", RequestStatus.ACTIVE)
            }
        }.decodeList<DonationRequest>().firstOrNull()

    suspend fun getHandover(requestId: String): HandoverConfirmation? =
        client.from(HANDOVER_TABLE).select {
            filter { eq("request_id", requestId) }
        }.decodeList<HandoverConfirmation>().firstOrNull()

    // ------------------------------ RPC ------------------------------

    suspend fun approve(requestId: String) = callRpc("approve_request", requestId)
    suspend fun reject(requestId: String) = callRpc("reject_request", requestId)
    suspend fun cancel(requestId: String) = callRpc("cancel_request", requestId)
    suspend fun confirmHandover(requestId: String) = callRpc("confirm_handover", requestId)

    suspend fun setHandoverSchedule(requestId: String, method: String, dateIso: String, notes: String?) {
        client.postgrest.rpc("set_handover_schedule", buildJsonObject {
            put("p_request_id", requestId)
            put("p_method", method)
            put("p_date", dateIso)
            put("p_notes", notes ?: "")
        })
    }

    private suspend fun callRpc(function: String, requestId: String) {
        client.postgrest.rpc(function, buildJsonObject { put("p_request_id", requestId) })
    }

    companion object {
        const val TABLE = "donation_requests"
        const val HANDOVER_TABLE = "handover_confirmations"
        const val REQUEST_COLUMNS =
            "*, books(id, title, author, image_url, status, location), " +
                "requester:profiles!donation_requests_requester_id_fkey(full_name, profile_image_url, location), " +
                "donor:profiles!donation_requests_donor_id_fkey(full_name, profile_image_url, location)"
    }
}
