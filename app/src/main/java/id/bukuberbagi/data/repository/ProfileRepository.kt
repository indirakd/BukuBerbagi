package id.bukuberbagi.data.repository

import id.bukuberbagi.data.SupabaseProvider
import id.bukuberbagi.data.model.Profile
import id.bukuberbagi.data.model.ProfileUpdate
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from

/** Tabel profiles: baca profil sendiri dan edit profil + foto (fitur bersama). */
class ProfileRepository(
    private val client: SupabaseClient = SupabaseProvider.client,
    private val storage: StorageRepository = StorageRepository(client)
) {
    private fun uid() = client.auth.currentUserOrNull()?.id ?: error("Sesi login berakhir. Silakan login lagi.")

    suspend fun getMyProfile(): Profile =
        client.from(TABLE).select {
            filter { eq("id", uid()) }
        }.decodeSingle<Profile>()

    suspend fun updateMyProfile(fullName: String, location: String?, newPhoto: ByteArray?, currentPhotoUrl: String?): Profile {
        val photoUrl = if (newPhoto != null) {
            storage.uploadJpeg(StorageRepository.AVATARS, newPhoto, "avatar")
        } else currentPhotoUrl

        client.from(TABLE).update(ProfileUpdate(fullName, location, photoUrl)) {
            filter { eq("id", uid()) }
        }
        return getMyProfile()
    }

    companion object {
        const val TABLE = "profiles"
    }
}
