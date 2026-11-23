package id.bukuberbagi.util

import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Mengubah error dari Supabase menjadi pesan yang ramah pengguna.
 * Pesan dari database yang diawali "BB:" (lihat 01_schema.sql) ditampilkan apa adanya.
 */
fun Throwable.toUserMessage(): String {
    val raw = buildString {
        append(message.orEmpty())
        cause?.message?.let { append(" ").append(it) }
    }
    Regex("BB: ([^\\n\"\\\\]+)").find(raw)?.let { return it.groupValues[1].trim() }

    return when {
        this is UnknownHostException || raw.contains("Unable to resolve host", true) ->
            "Tidak ada koneksi internet. Periksa jaringanmu."
        this is SocketTimeoutException || raw.contains("timeout", true) ->
            "Server terlalu lama merespons. Coba lagi."
        raw.contains("Invalid login credentials", true) -> "Email atau kata sandi salah"
        raw.contains("already registered", true) || raw.contains("already been registered", true) ->
            "Email sudah terdaftar"
        raw.contains("Email not confirmed", true) ->
            "Email belum dikonfirmasi. Buka email konfirmasi dari Supabase terlebih dulu."
        raw.contains("Password should be", true) -> "Kata sandi minimal 6 karakter"
        raw.contains("rate limit", true) -> "Terlalu banyak percobaan. Tunggu sebentar lalu coba lagi."
        raw.contains("categories_name", true) || raw.contains("categories_category_name_key", true) ->
            "Nama kategori sudah ada"
        raw.contains("row-level security", true) || raw.contains("permission denied", true) ->
            "Kamu tidak punya akses untuk melakukan aksi ini"
        else -> "Terjadi kesalahan. Coba lagi."
    }
}
