package com.harmony.tokoharmony.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.harmony.tokoharmony.feature.admin.dashboard.AdminDashboardScreen
import com.harmony.tokoharmony.feature.admin.inventory.history.StockHistoryScreen
import com.harmony.tokoharmony.feature.admin.inventory.initial.InitialStockSetupScreen
import com.harmony.tokoharmony.feature.admin.inventory.opname.StockOpnameScreen
import com.harmony.tokoharmony.feature.admin.inventory.stockin.StockInScreen
import com.harmony.tokoharmony.feature.admin.product.ProductDetailScreen
import com.harmony.tokoharmony.feature.admin.product.ProductFormScreen
import com.harmony.tokoharmony.feature.admin.product.ProductListScreen
import com.harmony.tokoharmony.feature.admin.sync.AdminSyncScreen
import com.harmony.tokoharmony.feature.admin.transaction.AdminTransactionDetailScreen
import com.harmony.tokoharmony.feature.admin.transaction.AdminTransactionHistoryScreen
import com.harmony.tokoharmony.feature.auth.AdminPinAuthScreen
import com.harmony.tokoharmony.feature.cashier.cart.CashierCartScreen
import com.harmony.tokoharmony.feature.cashier.cart.CashierCartViewModel
import com.harmony.tokoharmony.feature.cashier.payment.CashierPaymentScreen
import com.harmony.tokoharmony.feature.cashier.scanner.BarcodeScannerScreen
import com.harmony.tokoharmony.feature.cashier.scanner.BarcodeScannerViewModel
import com.harmony.tokoharmony.feature.cashier.search.CashierSearchScreen
import com.harmony.tokoharmony.feature.cashier.search.CashierSearchViewModel
import com.harmony.tokoharmony.feature.cashier.success.TransactionSuccessScreen
import com.harmony.tokoharmony.feature.home.HomeScreen

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToCashier = {
                    navController.navigate(Screen.Cashier.route)
                },
                onNavigateToAdmin = {
                    navController.navigate(Screen.AdminPinAuth.createRoute())
                }
            )
        }

        composable(Screen.Cashier.route) {
            val cartViewModel: CashierCartViewModel = hiltViewModel()
            CashierCartScreen(
                viewModel = cartViewModel,
                onNavigateToSearch = {
                    navController.navigate(Screen.CashierSearch.route)
                },
                onNavigateToScanner = {
                    navController.navigate(Screen.BarcodeScanner.route)
                },
                onNavigateToAdminAuth = {
                    navController.navigate(Screen.AdminPinAuth.createRoute())
                },
                onNavigateToPayment = {
                    navController.navigate(Screen.CashierPayment.route)
                }
            )
        }

        composable(Screen.CashierPayment.route) {
            CashierPaymentScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onPaymentSuccess = { transactionId ->
                    navController.navigate(Screen.TransactionSuccess.createRoute(transactionId)) {
                        popUpTo(Screen.Cashier.route) { inclusive = false }
                    }
                }
            )
        }

        composable(
            route = Screen.TransactionSuccess.route,
            arguments = listOf(
                navArgument("transactionId") { type = NavType.StringType }
            )
        ) {
            TransactionSuccessScreen(
                onNewTransaction = {
                    navController.navigate(Screen.Cashier.route) {
                        popUpTo(Screen.Cashier.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.CashierSearch.route) {
            val searchViewModel: CashierSearchViewModel = hiltViewModel()
            CashierSearchScreen(
                viewModel = searchViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.BarcodeScanner.route) {
            val scannerViewModel: BarcodeScannerViewModel = hiltViewModel()
            BarcodeScannerScreen(
                viewModel = scannerViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToRegisterProduct = { barcode ->
                    navController.navigate(
                        Screen.AdminPinAuth.createRoute(
                            targetDestination = "register_product",
                            barcode = barcode
                        )
                    )
                }
            )
        }

        composable(
            route = Screen.AdminPinAuth.route,
            arguments = listOf(
                navArgument("targetDestination") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("barcode") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val targetDestination = backStackEntry.arguments?.getString("targetDestination")
            val barcode = backStackEntry.arguments?.getString("barcode")

            AdminPinAuthScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onAuthSuccess = {
                    if (targetDestination == "register_product") {
                        navController.navigate(Screen.AdminProductCreate.createRoute(barcode = barcode)) {
                            popUpTo(Screen.AdminPinAuth.route) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.AdminDashboard.route) {
                            popUpTo(Screen.AdminPinAuth.route) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(Screen.AdminDashboard.route) {
            AdminDashboardScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToProducts = {
                    navController.navigate(Screen.AdminProductList.route)
                },
                onNavigateToInitialStock = {
                    navController.navigate(Screen.AdminInitialStock.route)
                },
                onNavigateToStockIn = {
                    navController.navigate(Screen.AdminStockIn.route)
                },
                onNavigateToStockOpname = {
                    navController.navigate(Screen.AdminStockOpname.route)
                },
                onNavigateToStockHistory = {
                    navController.navigate(Screen.AdminStockHistory.route)
                },
                onNavigateToTransactions = {
                    navController.navigate(Screen.AdminTransactionHistory.route)
                },
                onNavigateToSync = {
                    navController.navigate(Screen.AdminSync.route)
                }
            )
        }

        composable(Screen.AdminProductList.route) {
            ProductListScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToCreateProduct = {
                    navController.navigate(Screen.AdminProductCreate.createRoute())
                },
                onNavigateToProductDetail = { productId ->
                    navController.navigate(Screen.AdminProductDetail.createRoute(productId))
                }
            )
        }

        composable(
            route = Screen.AdminProductCreate.route,
            arguments = listOf(
                navArgument("barcode") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            ProductFormScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onSaveSuccess = { _ ->
                    val popped = navController.popBackStack()
                    if (!popped) {
                        navController.navigate(Screen.Cashier.route)
                    }
                }
            )
        }

        composable(
            route = Screen.AdminProductDetail.route,
            arguments = listOf(
                navArgument("productId") { type = NavType.StringType }
            )
        ) {
            ProductDetailScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToEditProduct = { productId ->
                    navController.navigate(Screen.AdminProductEdit.createRoute(productId))
                }
            )
        }

        composable(
            route = Screen.AdminProductEdit.route,
            arguments = listOf(
                navArgument("productId") { type = NavType.StringType }
            )
        ) {
            ProductFormScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onSaveSuccess = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.AdminInitialStock.route) {
            InitialStockSetupScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.AdminStockIn.route) {
            StockInScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.AdminStockOpname.route) {
            StockOpnameScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.AdminStockHistory.route) {
            StockHistoryScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.AdminTransactionHistory.route) {
            AdminTransactionHistoryScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToDetail = { transactionId ->
                    navController.navigate(Screen.AdminTransactionDetail.createRoute(transactionId))
                }
            )
        }

        composable(
            route = Screen.AdminTransactionDetail.route,
            arguments = listOf(
                navArgument("transactionId") { type = NavType.StringType }
            )
        ) {
            AdminTransactionDetailScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.AdminSync.route) {
            AdminSyncScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
