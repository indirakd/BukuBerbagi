package id.bukuberbagi.ui.navigation

/** Semua rute halaman aplikasi. */
object Routes {
    // Autentikasi
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"

    // Pengguna
    const val HOME = "home"
    const val CATALOG = "catalog?categoryId={categoryId}&focusSearch={focusSearch}"
    const val BOOK_DETAIL = "book/{bookId}"
    const val ADD_BOOK = "add_book"
    const val MY_BOOKS = "my_books"
    const val EDIT_BOOK = "edit_book/{bookId}"
    const val REQUEST_FORM = "request_form/{bookId}"
    const val REQUESTS = "requests?tab={tab}"
    const val REQUEST_DETAIL = "request/{requestId}"
    const val HANDOVER = "handover/{requestId}"
    const val PROFILE = "profile"
    const val EDIT_PROFILE = "edit_profile"

    // Admin
    const val ADMIN_DASHBOARD = "admin_dashboard"
    const val ADMIN_CATEGORIES = "admin_categories"
    const val CATEGORY_FORM = "category_form?categoryId={categoryId}"
    const val ADMIN_BOOKS = "admin_books"

    fun catalog(categoryId: String? = null, focusSearch: Boolean = false) =
        "catalog?categoryId=${categoryId.orEmpty()}&focusSearch=$focusSearch"
    fun bookDetail(id: String) = "book/$id"
    fun editBook(id: String) = "edit_book/$id"
    fun requestForm(bookId: String) = "request_form/$bookId"
    fun requests(tab: String = "mine") = "requests?tab=$tab"
    fun requestDetail(id: String) = "request/$id"
    fun handover(id: String) = "handover/$id"
    fun categoryForm(id: String? = null) = "category_form?categoryId=${id.orEmpty()}"

    val authRoutes = setOf(SPLASH, LOGIN, REGISTER)
}
