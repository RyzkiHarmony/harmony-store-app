package com.harmony.tokoharmony.feature.admin.inventory.initial

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.harmony.tokoharmony.domain.model.Product
import com.harmony.tokoharmony.domain.model.QuantityType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InitialStockSetupScreen(
    onNavigateBack: () -> Unit,
    viewModel: InitialStockSetupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Penetapan Stok Awal", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.products.isEmpty()) {
                Text(
                    text = "Belum ada produk fisik terdaftar.",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Pilih produk untuk menetapkan stok awal fisik:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    items(uiState.products, key = { it.productId }) { product ->
                        val isInitialized = uiState.initializedMap[product.productId] == true
                        val currentStock = uiState.stockMap[product.productId] ?: 0L

                        InitialStockProductItem(
                            product = product,
                            isInitialized = isInitialized,
                            currentStock = currentStock,
                            onClick = {
                                if (!isInitialized) {
                                    viewModel.selectProduct(product)
                                }
                            }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            // Dialog for inputting initial stock
            uiState.selectedProduct?.let { product ->
                val isGram = product.quantityType == QuantityType.GRAM
                val unitLabel = if (isGram) "gram (cth: 500, 1000)" else product.stockUnit

                AlertDialog(
                    onDismissRequest = { viewModel.clearSelectedProduct() },
                    title = {
                        Text("Stok Awal: ${product.name}", fontWeight = FontWeight.Bold)
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "Tentukan saldo awal stok fisik untuk produk ini. Nilai ini hanya dapat ditetapkan satu kali sebagai baseline.",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            OutlinedTextField(
                                value = uiState.initialStockInput,
                                onValueChange = { viewModel.onInitialStockChanged(it) },
                                label = { Text("Jumlah Stok Awal ($unitLabel)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.saveInitialStock() },
                            enabled = !uiState.isSubmitting && uiState.initialStockInput.isNotBlank()
                        ) {
                            if (uiState.isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White
                                )
                            } else {
                                Text("SIMPAN")
                            }
                        }
                    },
                    dismissButton = {
                        OutlinedButton(
                            onClick = { viewModel.clearSelectedProduct() },
                            enabled = !uiState.isSubmitting
                        ) {
                            Text("BATAL")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun InitialStockProductItem(
    product: Product,
    isInitialized: Boolean,
    currentStock: Long,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isInitialized, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isInitialized) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(8.dp),
                color = if (isInitialized) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    imageVector = if (isInitialized) Icons.Default.CheckCircle else Icons.Default.Inventory,
                    contentDescription = null,
                    modifier = Modifier.padding(8.dp),
                    tint = if (isInitialized) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                val unit = if (product.quantityType == QuantityType.GRAM) "gram" else product.stockUnit
                Text(
                    text = if (isInitialized) "Stok saat ini: $currentStock $unit"
                    else "Belum ada stok awal • Ketuk untuk atur",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isInitialized) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                )
            }

            if (isInitialized) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFFE8F5E9)
                ) {
                    Text(
                        text = "TERTETAPKAN",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = "ATUR",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
