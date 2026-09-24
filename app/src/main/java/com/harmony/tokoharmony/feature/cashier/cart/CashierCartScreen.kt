package com.harmony.tokoharmony.feature.cashier.cart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.layout.size
import com.harmony.tokoharmony.domain.model.DigitalTransaction
import com.harmony.tokoharmony.domain.model.ProductKind
import com.harmony.tokoharmony.feature.cashier.digital.DigitalTransactionDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.harmony.tokoharmony.core.common.formatRupiah
import com.harmony.tokoharmony.domain.model.TransactionItem
import com.harmony.tokoharmony.feature.cashier.weight.WeightSelectionDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashierCartScreen(
    viewModel: CashierCartViewModel,
    onNavigateToSearch: () -> Unit,
    onNavigateToScanner: () -> Unit,
    onNavigateToAdminAuth: () -> Unit,
    onNavigateToPayment: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { errorMsg ->
            snackbarHostState.showSnackbar(errorMsg)
            viewModel.clearErrorMessage()
        }
    }

    if (uiState.weightEditItem != null) {
        val (product, item) = uiState.weightEditItem!!
        WeightSelectionDialog(
            productName = product.name,
            pricePerKg = product.currentPrice,
            initialGrams = item.quantity,
            onConfirm = { newGrams ->
                viewModel.saveWeightEdit(newGrams)
            },
            onDismiss = {
                viewModel.dismissWeightEditDialog()
            }
        )
    }

    if (uiState.digitalEditItem != null) {
        val (product, item) = uiState.digitalEditItem!!
        val existingDt = uiState.digitalTransactionsMap[item.itemId]
        DigitalTransactionDialog(
            productName = product.name,
            defaultNominal = product.currentPrice,
            existingTransaction = existingDt,
            onConfirm = { serviceType, customerNumber, nominal, providerRef, status ->
                viewModel.saveDigitalTransaction(serviceType, customerNumber, nominal, providerRef, status)
            },
            onDismiss = {
                viewModel.dismissDigitalEditDialog()
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("TRANSAKSI BARU", fontWeight = FontWeight.Bold)
                        uiState.cart?.transaction?.let { tx ->
                            Text(
                                text = tx.transactionNumber,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    if (uiState.cart?.items?.isNotEmpty() == true) {
                        IconButton(onClick = { viewModel.clearCart() }) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Kosongkan Keranjang"
                            )
                        }
                    }
                    IconButton(onClick = onNavigateToAdminAuth) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = "Mode Orang Tua"
                        )
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "TOTAL",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatRupiah(uiState.cart?.totalAmount ?: 0L),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = onNavigateToScanner,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Scan")
                            }
                            Button(
                                onClick = onNavigateToSearch,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Search, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Cari")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onNavigateToPayment,
                        enabled = uiState.cart != null && !uiState.cart!!.isEmpty,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "PROSES PEMBAYARAN",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.cart == null || uiState.cart!!.isEmpty -> {
                    EmptyCartView(
                        onNavigateToScanner = onNavigateToScanner,
                        onNavigateToSearch = onNavigateToSearch,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = uiState.cart!!.items,
                            key = { it.itemId }
                        ) { item ->
                            val product = uiState.productsMap[item.productId]
                            val isDigital = product?.productKind == ProductKind.DIGITAL
                            val dt = uiState.digitalTransactionsMap[item.itemId]
                            CartItemRow(
                                item = item,
                                isDigital = isDigital,
                                digitalTransaction = dt,
                                onIncrement = { viewModel.incrementQuantity(item) },
                                onDecrement = { viewModel.decrementQuantity(item) },
                                onRemove = { viewModel.removeItem(item) },
                                onEditWeight = { viewModel.openWeightEditDialog(item) },
                                onEditDigital = { viewModel.openDigitalEditDialog(item) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CartItemRow(
    item: TransactionItem,
    isDigital: Boolean = false,
    digitalTransaction: DigitalTransaction? = null,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onRemove: () -> Unit,
    onEditWeight: () -> Unit,
    onEditDigital: () -> Unit = {}
) {
    val isWeighted = item.sellingUnit.equals("kg", ignoreCase = true)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.productNameSnapshot,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    if (isDigital) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = "PRODUK DIGITAL",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        if (digitalTransaction != null) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${digitalTransaction.serviceType}: ${digitalTransaction.customerNumber} (${digitalTransaction.status})",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    val quantityText = if (isWeighted) {
                        val kgString = if (item.quantity % 1000L == 0L) {
                            "${item.quantity / 1000L} kg"
                        } else {
                            String.format("%.1f kg", item.quantity / 1000.0).replace('.', ',')
                        }
                        "$kgString × ${formatRupiah(item.unitPrice)} / kg"
                    } else {
                        "${item.quantity} × ${formatRupiah(item.unitPrice)}"
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = quantityText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = formatRupiah(item.subtotal),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isDigital) {
                    Button(
                        onClick = onEditDigital,
                        shape = RoundedCornerShape(8.dp),
                        colors = if (digitalTransaction != null) ButtonDefaults.filledTonalButtonColors()
                                 else ButtonDefaults.buttonColors()
                    ) {
                        Icon(Icons.Default.PhoneAndroid, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (digitalTransaction != null) "Ubah Data" else "Catat Data Digital")
                    }
                } else if (isWeighted) {
                    FilledTonalButton(
                        onClick = onEditWeight,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Scale, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ubah Berat")
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDecrement,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Kurang")
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Text(
                                text = "${item.quantity}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }

                        OutlinedButton(
                            onClick = onIncrement,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Tambah")
                        }
                    }
                }

                IconButton(
                    onClick = onRemove,
                    colors = androidx.compose.material3.IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Hapus")
                }
            }
        }
    }
}

@Composable
fun EmptyCartView(
    onNavigateToScanner: () -> Unit,
    onNavigateToSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.ShoppingCart,
            contentDescription = null,
            modifier = Modifier.padding(bottom = 16.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        Text(
            text = "Belum ada barang di keranjang",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Scan barcode produk atau gunakan pencarian manual untuk menambahkan barang.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onNavigateToScanner,
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Scan Barcode")
            }
            Button(
                onClick = onNavigateToSearch,
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Search, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Cari Produk")
            }
        }
    }
}
