package com.harmony.tokoharmony.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Cashier : Screen("cashier")
    data object CashierSearch : Screen("cashier_search")
    data object BarcodeScanner : Screen("barcode_scanner")
    data object AdminPinAuth : Screen("admin_pin_auth?targetDestination={targetDestination}&barcode={barcode}") {
        fun createRoute(targetDestination: String? = null, barcode: String? = null): String {
            val params = mutableListOf<String>()
            if (targetDestination != null) params.add("targetDestination=$targetDestination")
            if (barcode != null) params.add("barcode=$barcode")
            return if (params.isNotEmpty()) "admin_pin_auth?${params.joinToString("&")}" else "admin_pin_auth"
        }
    }
    data object AdminDashboard : Screen("admin_dashboard")
    data object AdminProductList : Screen("admin_product_list")
    data object AdminProductCreate : Screen("admin_product_create?barcode={barcode}") {
        fun createRoute(barcode: String? = null): String {
            return if (barcode != null) "admin_product_create?barcode=$barcode" else "admin_product_create"
        }
    }
    data object AdminProductDetail : Screen("admin_product_detail/{productId}") {
        fun createRoute(productId: String) = "admin_product_detail/$productId"
    }
    data object AdminProductEdit : Screen("admin_product_edit/{productId}") {
        fun createRoute(productId: String) = "admin_product_edit/$productId"
    }
    data object CashierPayment : Screen("cashier_payment")
    data object TransactionSuccess : Screen("transaction_success/{transactionId}") {
        fun createRoute(transactionId: String) = "transaction_success/$transactionId"
    }
    data object AdminInitialStock : Screen("admin_initial_stock")
    data object AdminStockIn : Screen("admin_stock_in")
    data object AdminStockOpname : Screen("admin_stock_opname")
    data object AdminStockHistory : Screen("admin_stock_history")
    data object AdminTransactionHistory : Screen("admin_transaction_history")
    data object AdminTransactionDetail : Screen("admin_transaction_detail/{transactionId}") {
        fun createRoute(transactionId: String) = "admin_transaction_detail/$transactionId"
    }
    data object AdminSync : Screen("admin_sync")
}
