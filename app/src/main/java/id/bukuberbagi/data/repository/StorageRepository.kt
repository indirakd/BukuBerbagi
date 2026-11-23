package id.bukuberbagi.data.repository

import id.bukuberbagi.data.SupabaseProvider
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType

/**
 * Upload gambar ke Supabase Storage dan mengembalikan URL publiknya.
 * File disimpan di folder milik user: <bucket>/<user_id>/<nama>.jpg
 * (sesuai policy di 02_storage.sql).
 */
class StorageRepository(private val client: SupabaseClient = SupabaseProvider.client) {

    suspend fun uploadJpeg(bucket: String, bytes: ByteArray, prefix: String): String {
        val userId = client.auth.currentUserOrNull()?.id ?: error("Sesi login berakhir. Silakan login lagi.")
        val path = "$userId/${prefix}_${System.currentTimeMillis()}.jpg"
        val storageBucket = client.storage.from(bucket)
        storageBucket.upload(path, bytes) {
            upsert = true
            contentType = ContentType.Image.JPEG
        }
        return storageBucket.publicUrl(path)
    }

    companion object {
        const val BOOK_IMAGES = "book-images"
        const val AVATARS = "avatars"
    }
}
