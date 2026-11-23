package id.bukuberbagi.ui.navigation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import id.bukuberbagi.data.model.Profile
import id.bukuberbagi.ui.admin.AddCategoryScreen
import id.bukuberbagi.ui.admin.AdminBooksScreen
import id.bukuberbagi.ui.admin.AdminDashboardScreen
import id.bukuberbagi.ui.admin.CategoryManagementScreen
import id.bukuberbagi.ui.auth.AuthViewModel
import id.bukuberbagi.ui.auth.LoginScreen
import id.bukuberbagi.ui.auth.RegisterScreen
import id.bukuberbagi.ui.auth.SessionState
import id.bukuberbagi.ui.auth.SplashScreen
import id.bukuberbagi.ui.book.AddBookScreen
import id.bukuberbagi.ui.book.EditBookScreen
import id.bukuberbagi.ui.book.MyBooksScreen
import id.bukuberbagi.ui.catalog.BookDetailScreen
import id.bukuberbagi.ui.catalog.CatalogScreen
import id.bukuberbagi.ui.components.LocalMessenger
import id.bukuberbagi.ui.components.bounceClick
import id.bukuberbagi.ui.home.HomeScreen
import id.bukuberbagi.ui.profile.EditProfileScreen
import id.bukuberbagi.ui.profile.ProfileScreen
import id.bukuberbagi.ui.request.HandoverConfirmationScreen
import id.bukuberbagi.ui.request.RequestDetailScreen
import id.bukuberbagi.ui.request.RequestDonationScreen
import id.bukuberbagi.ui.request.RequestsScreen
import id.bukuberbagi.ui.theme.Cream
import id.bukuberbagi.ui.theme.Coral
import id.bukuberbagi.ui.theme.Ink
import id.bukuberbagi.ui.theme.MutedLight
import id.bukuberbagi.ui.theme.Paper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class NavItem(val pattern: String, val route: String, val icon: ImageVector, val label: String)

private val userTabs = listOf(
    NavItem(Routes.HOME, Routes.HOME, Icons.Outlined.Home, "Beranda"),
    NavItem(Routes.CATALOG, Routes.catalog(), Icons.Outlined.MenuBook, "Katalog"),
    NavItem(Routes.ADD_BOOK, Routes.ADD_BOOK, Icons.Outlined.Add, "Donasikan"),
    NavItem(Routes.REQUESTS, Routes.requests(), Icons.Outlined.SwapHoriz, "Permintaan"),
    NavItem(Routes.PROFILE, Routes.PROFILE, Icons.Outlined.Person, "Profil")
)

private val adminTabs = listOf(
    NavItem(Routes.ADMIN_DASHBOARD, Routes.ADMIN_DASHBOARD, Icons.Outlined.Dashboard, "Dasbor"),
    NavItem(Routes.ADMIN_CATEGORIES, Routes.ADMIN_CATEGORIES, Icons.Outlined.Category, "Kategori"),
    NavItem(Routes.ADMIN_BOOKS, Routes.ADMIN_BOOKS, Icons.Outlined.LibraryBooks, "Semua buku"),
    NavItem(Routes.PROFILE, Routes.PROFILE, Icons.Outlined.Person, "Profil")
)

private fun homeOf(profile: Profile?) = if (profile?.isAdmin == true) Routes.ADMIN_DASHBOARD else Routes.HOME

private fun NavHostController.navigateTab(route: String, home: String, restore: Boolean = true) {
    navigate(route) {
        popUpTo(home) { saveState = restore }
        launchSingleTop = true
        restoreState = restore
    }
}

@Composable
fun BukuBerbagiApp() {
    val authViewModel: AuthViewModel = viewModel()
    val session by authViewModel.session.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val messenger: (String) -> Unit = remember {
        { msg ->
            scope.launch {
                snackbar.currentSnackbarData?.dismiss()
                snackbar.showSnackbar(msg)
            }
        }
    }

    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val profile = (session as? SessionState.LoggedIn)?.profile
    val tabs = if (profile?.isAdmin == true) adminTabs else userTabs
    val showBottomBar = profile != null && tabs.any { it.pattern == currentRoute }

    // Navigasi otomatis berdasarkan status sesi (login / logout / role)
    LaunchedEffect(session) {
        when (val s = session) {
            is SessionState.LoggedIn -> {
                val current = nav.currentDestination?.route
                if (current == null || current in Routes.authRoutes) {
                    if (current == Routes.SPLASH) delay(900)
                    nav.navigate(homeOf(s.profile)) { popUpTo(nav.graph.id) { inclusive = true } }
                }
            }
            SessionState.LoggedOut -> {
                val current = nav.currentDestination?.route
                if (current != Routes.LOGIN && current != Routes.REGISTER) {
                    if (current == Routes.SPLASH) delay(900)
                    nav.navigate(Routes.LOGIN) { popUpTo(nav.graph.id) { inclusive = true } }
                }
            }
            SessionState.Checking -> Unit
        }
    }

    CompositionLocalProvider(LocalMessenger provides messenger) {
        Scaffold(
            containerColor = Cream,
            snackbarHost = {
                SnackbarHost(snackbar) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = Ink,
                        contentColor = Cream,
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            },
            bottomBar = {
                if (showBottomBar) {
                    BottomBar(tabs, currentRoute) { item ->
                        nav.navigateTab(item.route, homeOf(profile))
                    }
                }
            }
        ) { padding ->
            NavHost(
                navController = nav,
                startDestination = Routes.SPLASH,
                modifier = Modifier.fillMaxSize().padding(padding),
                enterTransition = { fadeIn() },
                exitTransition = { fadeOut() }
            ) {
                // ------------------------- Autentikasi -------------------------
                composable(Routes.SPLASH) { SplashScreen() }
                composable(Routes.LOGIN) {
                    LoginScreen(authViewModel, onRegister = {
                        authViewModel.clearFormMessages()
                        nav.navigate(Routes.REGISTER)
                    })
                }
                composable(Routes.REGISTER) {
                    RegisterScreen(authViewModel, onBack = {
                        authViewModel.clearFormMessages()
                        nav.popBackStack()
                    })
                }

                // ------------------------- Pengguna -------------------------
                composable(Routes.HOME) {
                    profile?.let { p ->
                        HomeScreen(
                            profile = p,
                            onSearch = { nav.navigateTab(Routes.catalog(focusSearch = true), Routes.HOME, restore = false) },
                            onCategory = { id -> nav.navigateTab(Routes.catalog(id), Routes.HOME, restore = false) },
                            onSeeAll = { nav.navigateTab(Routes.catalog(), Routes.HOME, restore = false) },
                            onDonate = { nav.navigateTab(Routes.ADD_BOOK, Routes.HOME) },
                            onOpenBook = { nav.navigate(Routes.bookDetail(it)) },
                            onMyRequests = { nav.navigateTab(Routes.requests("mine"), Routes.HOME, restore = false) },
                            onIncomingRequests = { nav.navigateTab(Routes.requests("in"), Routes.HOME, restore = false) }
                        )
                    }
                }
                composable(
                    Routes.CATALOG,
                    arguments = listOf(
                        navArgument("categoryId") { type = NavType.StringType; defaultValue = "" },
                        navArgument("focusSearch") { type = NavType.BoolType; defaultValue = false }
                    )
                ) { entry ->
                    CatalogScreen(
                        initialCategoryId = entry.arguments?.getString("categoryId")?.ifBlank { null },
                        focusSearch = entry.arguments?.getBoolean("focusSearch") ?: false,
                        onOpenBook = { nav.navigate(Routes.bookDetail(it)) }
                    )
                }
                composable(Routes.BOOK_DETAIL, arguments = listOf(navArgument("bookId") { type = NavType.StringType })) { entry ->
                    BookDetailScreen(
                        bookId = entry.arguments?.getString("bookId").orEmpty(),
                        isAdmin = profile?.isAdmin == true,
                        onBack = { nav.popBackStack() },
                        onRequest = { nav.navigate(Routes.requestForm(it)) },
                        onEdit = { nav.navigate(Routes.editBook(it)) },
                        onOpenRequest = { nav.navigate(Routes.requestDetail(it)) }
                    )
                }
                composable(Routes.ADD_BOOK) {
                    AddBookScreen(
                        onSaved = { nav.navigate(Routes.MY_BOOKS) },
                        onMyBooks = { nav.navigate(Routes.MY_BOOKS) }
                    )
                }
                composable(Routes.MY_BOOKS) {
                    MyBooksScreen(
                        onBack = { nav.popBackStack() },
                        onAdd = { if (!nav.popBackStack(Routes.ADD_BOOK, inclusive = false)) nav.navigateTab(Routes.ADD_BOOK, Routes.HOME) },
                        onEdit = { nav.navigate(Routes.editBook(it)) },
                        onOpen = { nav.navigate(Routes.bookDetail(it)) }
                    )
                }
                composable(Routes.EDIT_BOOK, arguments = listOf(navArgument("bookId") { type = NavType.StringType })) { entry ->
                    EditBookScreen(
                        bookId = entry.arguments?.getString("bookId").orEmpty(),
                        onBack = { nav.popBackStack() },
                        onSaved = { nav.popBackStack() }
                    )
                }
                composable(Routes.REQUEST_FORM, arguments = listOf(navArgument("bookId") { type = NavType.StringType })) { entry ->
                    RequestDonationScreen(
                        bookId = entry.arguments?.getString("bookId").orEmpty(),
                        onBack = { nav.popBackStack() },
                        onCreated = { nav.navigateTab(Routes.requests("mine"), Routes.HOME, restore = false) }
                    )
                }
                composable(
                    Routes.REQUESTS,
                    arguments = listOf(navArgument("tab") { type = NavType.StringType; defaultValue = "mine" })
                ) { entry ->
                    RequestsScreen(
                        initialTab = entry.arguments?.getString("tab") ?: "mine",
                        onOpenRequest = { nav.navigate(Routes.requestDetail(it)) },
                        onBrowse = { nav.navigateTab(Routes.catalog(), Routes.HOME, restore = false) }
                    )
                }
                composable(Routes.REQUEST_DETAIL, arguments = listOf(navArgument("requestId") { type = NavType.StringType })) { entry ->
                    RequestDetailScreen(
                        requestId = entry.arguments?.getString("requestId").orEmpty(),
                        onBack = { nav.popBackStack() },
                        onHandover = { nav.navigate(Routes.handover(it)) }
                    )
                }
                composable(Routes.HANDOVER, arguments = listOf(navArgument("requestId") { type = NavType.StringType })) { entry ->
                    HandoverConfirmationScreen(
                        requestId = entry.arguments?.getString("requestId").orEmpty(),
                        onBack = { nav.popBackStack() }
                    )
                }
                composable(Routes.PROFILE) {
                    profile?.let { p ->
                        ProfileScreen(
                            profile = p,
                            onEditProfile = { nav.navigate(Routes.EDIT_PROFILE) },
                            onMyBooks = { nav.navigate(Routes.MY_BOOKS) },
                            onLogout = { authViewModel.logout() }
                        )
                    }
                }
                composable(Routes.EDIT_PROFILE) {
                    profile?.let { p ->
                        EditProfileScreen(
                            profile = p,
                            onBack = { nav.popBackStack() },
                            onSaved = { updated ->
                                authViewModel.updateLocalProfile(updated)
                                nav.popBackStack()
                            }
                        )
                    }
                }

                // ------------------------- Admin -------------------------
                composable(Routes.ADMIN_DASHBOARD) {
                    profile?.let { p ->
                        AdminDashboardScreen(
                            profile = p,
                            onManageCategories = { nav.navigateTab(Routes.ADMIN_CATEGORIES, Routes.ADMIN_DASHBOARD) },
                            onOpenBook = { nav.navigate(Routes.bookDetail(it)) }
                        )
                    }
                }
                composable(Routes.ADMIN_CATEGORIES) {
                    CategoryManagementScreen(
                        onAdd = { nav.navigate(Routes.categoryForm()) },
                        onEdit = { nav.navigate(Routes.categoryForm(it)) }
                    )
                }
                composable(
                    Routes.CATEGORY_FORM,
                    arguments = listOf(navArgument("categoryId") { type = NavType.StringType; defaultValue = "" })
                ) { entry ->
                    AddCategoryScreen(
                        categoryId = entry.arguments?.getString("categoryId")?.ifBlank { null },
                        onBack = { nav.popBackStack() },
                        onSaved = { nav.popBackStack() }
                    )
                }
                composable(Routes.ADMIN_BOOKS) {
                    AdminBooksScreen(onOpenBook = { nav.navigate(Routes.bookDetail(it)) })
                }
            }
        }
    }
}

/** Bottom navigation: menu aktif ditandai lingkaran koral yang "meletup". */
@Composable
private fun BottomBar(items: List<NavItem>, currentRoute: String?, onSelect: (NavItem) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Paper)
            .navigationBarsPadding()
            .padding(horizontal = 6.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { item ->
            val selected = item.pattern == currentRoute
            val scale by animateFloatAsState(
                if (selected) 1f else 0.85f,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                label = "navPop"
            )
            Box(
                Modifier
                    .bounceClick(pressedScale = 0.85f) { onSelect(item) }
                    .size(46.dp)
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .clip(CircleShape)
                    .background(if (selected) Coral else Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                Icon(item.icon, contentDescription = item.label, tint = if (selected) Color.White else MutedLight)
            }
        }
    }
}
