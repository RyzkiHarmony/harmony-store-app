package com.harmony.tokoharmony.feature.admin.inventory.opname

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
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
fun StockOpnameScreen(
    onNavigateBack: () -> Unit,
    viewModel: StockOpnameViewModel = hiltViewModel()
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
                title = { Text("Stock Opname & Koreksi", fontWeight = FontWeight.Bold) },
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
                    text = "Belum ada produk fisik aktif.",
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
                            text = "Pilih produk untuk memeriksa dan menyesuaikan stok fisik:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    items(uiState.products, key = { it.productId }) { product ->
                        val currentStock = uiState.stockMap[product.productId] ?: 0L
                        StockOpnameProductItem(
                            product = product,
                            currentStock = currentStock,
                            onClick = { viewModel.selectProduct(product) }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            // Dialog for stock opname & adjustment
            uiState.selectedProduct?.let { product ->
                val isGram = product.quantityType == QuantityType.GRAM
                val unitLabel = if (isGram) "gram" else product.stockUnit
                val diff = uiState.difference
                var expandedReasonDropdown by remember { mutableStateOf(false) }

                AlertDialog(
                    onDismissRequest = { viewModel.clearSelectedProduct() },
                    title = {
                        Text("Opname: ${product.name}", fontWeight = FontWeight.Bold)
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // System Stock Info
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Stok Sistem:", style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        "${uiState.systemStock} $unitLabel",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Physical Stock Input
                            OutlinedTextField(
                                value = uiState.physicalStockInput,
                                onValueChange = { viewModel.onPhysicalStockChanged(it) },
                                label = { Text("Stok Fisik Sebenarnya ($unitLabel)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            // Calculated Difference Box
                            if (diff != null) {
                                val diffColor = when {
                                    diff > 0 -> Color(0xFF2E7D32)
                                    diff < 0 -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                                val diffBg = when {
                                    diff > 0 -> Color(0xFFE8F5E9)
                                    diff < 0 -> MaterialTheme.colorScheme.errorContainer
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                                val diffText = when {
                                    diff > 0 -> "+$diff $unitLabel (Surplus)"
                                    diff < 0 -> "$diff $unitLabel (Defisit/Kurang)"
                                    else -> "0 $unitLabel (Sesuai)"
                                }

                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = diffBg,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Selisih Koreksi:", style = MaterialTheme.typography.bodyMedium, color = diffColor)
                                        Text(
                                            diffText,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = diffColor
                                        )
                                    }
                                }
                            }

                            Divider()

                            // Reason Dropdown
                            ExposedDropdownMenuBox(
                                expanded = expandedReasonDropdown,
                                onExpandedChange = { expandedReasonDropdown = !expandedReasonDropdown }
                            ) {
                                OutlinedTextField(
                                    value = uiState.selectedReason,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Alasan Koreksi") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedReasonDropdown) },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                )

                                ExposedDropdownMenu(
                                    expanded = expandedReasonDropdown,
                                    onDismissRequest = { expandedReasonDropdown = false }
                                ) {
                                    ADJUSTMENT_REASONS.forEach { reason ->
                                        DropdownMenuItem(
                                            text = { Text(reason) },
                                            onClick = {
                                                viewModel.onReasonSelected(reason)
                                                expandedReasonDropdown = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Optional Note
                            OutlinedTextField(
                                value = uiState.noteInput,
                                onValueChange = { viewModel.onNoteChanged(it) },
                                label = { Text("Catatan Tambahan (Opsional)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.saveAdjustment() },
                            enabled = !uiState.isSubmitting && uiState.physicalStock != null
                        ) {
                            if (uiState.isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White
                                )
                            } else {
                                Text("SIMPAN KOREKSI")
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
private fun StockOpnameProductItem(
    product: Product,
    currentStock: Long,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
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
                color = MaterialTheme.colorScheme.tertiaryContainer
            ) {
                Icon(
                    imageVector = Icons.Default.Assessment,
                    contentDescription = null,
                    modifier = Modifier.padding(8.dp),
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
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
                    text = "Stok sistem: $currentStock $unit",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.primary
            ) {
                Text(
                    text = "OPNAME",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
