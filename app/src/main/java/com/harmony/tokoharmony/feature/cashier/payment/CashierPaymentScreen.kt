package com.harmony.tokoharmony.feature.cashier.payment

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.ChangeCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.harmony.tokoharmony.core.common.formatRupiah
import com.harmony.tokoharmony.domain.model.PaymentMethod
import com.harmony.tokoharmony.ui.theme.AmberTertiary
import com.harmony.tokoharmony.ui.theme.CoralError
import com.harmony.tokoharmony.ui.theme.CoralErrorContainer
import com.harmony.tokoharmony.ui.theme.CoralOnError
import com.harmony.tokoharmony.ui.theme.CoralOnErrorContainer
import com.harmony.tokoharmony.ui.theme.EmeraldOnPrimary
import com.harmony.tokoharmony.ui.theme.EmeraldPrimary
import com.harmony.tokoharmony.ui.theme.EmeraldPrimaryContainer
import com.harmony.tokoharmony.ui.theme.EmeraldPrimaryFixed
import com.harmony.tokoharmony.ui.theme.MintOnSecondary
import com.harmony.tokoharmony.ui.theme.MintOnSecondaryContainer
import com.harmony.tokoharmony.ui.theme.MintSecondary
import com.harmony.tokoharmony.ui.theme.MintSecondaryContainer
import com.harmony.tokoharmony.ui.theme.OutlineBorder
import com.harmony.tokoharmony.ui.theme.OutlineVariant
import com.harmony.tokoharmony.ui.theme.SurfaceBackground
import com.harmony.tokoharmony.ui.theme.SurfaceContainer
import com.harmony.tokoharmony.ui.theme.SurfaceContainerHigh
import com.harmony.tokoharmony.ui.theme.SurfaceContainerHighest
import com.harmony.tokoharmony.ui.theme.SurfaceLow
import com.harmony.tokoharmony.ui.theme.SurfaceLowest
import com.harmony.tokoharmony.ui.theme.TextOnSurface
import com.harmony.tokoharmony.ui.theme.TextOnSurfaceVariant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashierPaymentScreen(
    onNavigateBack: () -> Unit,
    onPaymentSuccess: (String) -> Unit,
    viewModel: CashierPaymentViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    LaunchedEffect(viewModel.eventFlow) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is CashierPaymentEvent.PaymentSuccess -> {
                    onPaymentSuccess(event.transactionId)
                }
            }
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearErrorMessage()
        }
    }

    Scaffold(
        containerColor = SurfaceBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Pembayaran",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextOnSurface
                        )
                        uiState.cart?.transaction?.let { tx ->
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = SurfaceLow
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(AmberTertiary.copy(alpha = pulseAlpha))
                                    )
                                    Text(
                                        text = tx.transactionNumber,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextOnSurface,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali ke Keranjang",
                            tint = TextOnSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceLowest,
                    titleContentColor = TextOnSurface
                )
            )
        }
    ) { paddingValues ->
        val scrollState = rememberScrollState()
        val totalAmount = uiState.cart?.totalAmount ?: 0L

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Total Bill Highlight Card with Glow
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceLowest),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "TOTAL TAGIHAN",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextOnSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Rp",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary
                                )
                                Text(
                                    text = formatNumber(totalAmount),
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextOnSurface
                                )
                            }
                        }

                        val prodCount = uiState.cart?.items?.size ?: 0
                        val itemCount = uiState.cart?.itemCount ?: 0
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = EmeraldPrimaryFixed.copy(alpha = 0.4f)
                        ) {
                            Text(
                                text = "$prodCount Produk ($itemCount Item)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Mini items preview summary
                    if (uiState.cart?.items?.isNotEmpty() == true) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = SurfaceLow
                        ) {
                            val itemScrollState = rememberScrollState()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(itemScrollState)
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                uiState.cart!!.items.forEach { itm ->
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = SurfaceLowest,
                                        shadowElevation = 1.dp
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = itm.productNameSnapshot,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextOnSurface
                                            )
                                            val qtyLabel = if (itm.sellingUnit.equals("kg", ignoreCase = true)) {
                                                if (itm.quantity % 1000L == 0L) "${itm.quantity / 1000L}kg"
                                                else String.format("%.1fkg", itm.quantity / 1000.0).replace('.', ',')
                                            } else {
                                                "×${itm.quantity}"
                                            }
                                            Text(
                                                text = qtyLabel,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = EmeraldPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Payment Method Tabs (BR-025 MVP)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Metode Pembayaran",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextOnSurfaceVariant
                )

                val selectedMethod = uiState.selectedPaymentMethod ?: PaymentMethod.CASH

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = SurfaceLow
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Cash Tab
                        val isCash = selectedMethod == PaymentMethod.CASH
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clickable { viewModel.selectPaymentMethod(PaymentMethod.CASH) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isCash) SurfaceLowest else Color.Transparent,
                            shadowElevation = if (isCash) 2.dp else 0.dp
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Payments,
                                    contentDescription = null,
                                    tint = if (isCash) EmeraldPrimary else TextOnSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Tunai (Cash)",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (isCash) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isCash) EmeraldPrimary else TextOnSurfaceVariant
                                )
                            }
                        }

                        // QRIS Tab
                        val isQris = selectedMethod == PaymentMethod.QRIS
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clickable { viewModel.selectPaymentMethod(PaymentMethod.QRIS) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isQris) SurfaceLowest else Color.Transparent,
                            shadowElevation = if (isQris) 2.dp else 0.dp
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = null,
                                    tint = if (isQris) EmeraldPrimary else TextOnSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "QRIS Toko",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (isQris) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isQris) EmeraldPrimary else TextOnSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // 3. Conditional Flow
            if (uiState.selectedPaymentMethod != PaymentMethod.QRIS) {
                // CASH FLOW MODULE
                // Tendered Amount Field Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLowest),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Uang Diterima / Nominal Kasir",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextOnSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.clickable {
                                    viewModel.onAmountReceivedChanged("")
                                },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                                    contentDescription = "Bersihkan",
                                    tint = CoralError,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Bersihkan",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CoralError,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Display formatted tendered amount
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = SurfaceLow
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Rp",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextOnSurfaceVariant
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    val amountText = if (uiState.amountReceivedInput.isBlank()) "0"
                                    else formatNumber(uiState.amountReceived ?: 0L)
                                    Text(
                                        text = amountText,
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextOnSurface
                                    )
                                    Box(
                                        modifier = Modifier
                                            .width(2.dp)
                                            .height(20.dp)
                                            .background(EmeraldPrimary.copy(alpha = pulseAlpha))
                                    )
                                }
                            }
                        }

                        // Quick Cash Chips (4 Chips Grid)
                        val quickAmounts = calculateQuickAmounts(totalAmount)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            quickAmounts.take(4).forEach { amt ->
                                val isPas = amt == totalAmount
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { viewModel.setQuickAmount(amt) },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isPas) EmeraldPrimaryFixed.copy(alpha = 0.5f) else SurfaceContainer,
                                    shadowElevation = 0.dp
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = if (isPas) "Uang Pas" else "+${formatShortNumber(amt - totalAmount)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isPas) EmeraldPrimary else TextOnSurfaceVariant
                                        )
                                        Text(
                                            text = formatShortNumber(amt),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = TextOnSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Live Auto Change Calculation Box (BR-028)
                if (uiState.isPaymentValid && uiState.amountReceived != null) {
                    // Sufficient Funds Box (Mint Green)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = MintSecondaryContainer,
                        shadowElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MintSecondary,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.ChangeCircle,
                                            contentDescription = null,
                                            tint = MintOnSecondary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = "KEMBALIAN KASIR",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MintOnSecondaryContainer,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = formatRupiah(uiState.changeAmount),
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MintOnSecondaryContainer
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = SurfaceLowest.copy(alpha = 0.85f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = MintSecondary,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "Mencukupi",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MintSecondary
                                        )
                                    }
                                }
                                Text(
                                    text = if (uiState.changeAmount == 0L) "Uang pas, tanpa kembalian" else "Siapkan uang kembalian",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = MintOnSecondaryContainer.copy(alpha = 0.8f),
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                } else if (uiState.shortageAmount != null) {
                    // Shortage Box (Coral Alert)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = CoralErrorContainer,
                        shadowElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = CoralError,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = CoralOnError,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = "KURANG BAYAR",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CoralOnErrorContainer,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = "Kurang ${formatRupiah(uiState.shortageAmount!!)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = CoralOnErrorContainer
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(50),
                                color = SurfaceLowest.copy(alpha = 0.85f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Error,
                                        contentDescription = null,
                                        tint = CoralError,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "Kurang Bayar",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = CoralError
                                    )
                                }
                            }
                        }
                    }
                }

                // Tactical Touch POS Numpad
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLowest),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val numpadRows = listOf(
                            listOf("1", "2", "3"),
                            listOf("4", "5", "6"),
                            listOf("7", "8", "9"),
                            listOf("000", "0", "DEL")
                        )

                        numpadRows.forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                row.forEach { key ->
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(50.dp)
                                            .clickable {
                                                when (key) {
                                                    "DEL" -> {
                                                        val cur = uiState.amountReceivedInput
                                                        if (cur.isNotEmpty()) {
                                                            viewModel.onAmountReceivedChanged(cur.dropLast(1))
                                                        }
                                                    }
                                                    "000" -> {
                                                        val cur = uiState.amountReceivedInput
                                                        if (cur.isNotEmpty() && cur != "0") {
                                                            viewModel.onAmountReceivedChanged(cur + "000")
                                                        }
                                                    }
                                                    else -> {
                                                        val cur = uiState.amountReceivedInput
                                                        val next = if (cur == "0") key else cur + key
                                                        if (next.length <= 10) {
                                                            viewModel.onAmountReceivedChanged(next)
                                                        }
                                                    }
                                                }
                                            },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (key == "DEL") SurfaceContainer else SurfaceLow,
                                        shadowElevation = 0.5.dp
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            if (key == "DEL") {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                                                    contentDescription = "Hapus",
                                                    modifier = Modifier.size(20.dp),
                                                    tint = TextOnSurface
                                                )
                                            } else {
                                                Text(
                                                    text = key,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextOnSurface
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Completion Button
                Button(
                    onClick = { viewModel.completeCashPayment() },
                    enabled = uiState.isPaymentValid && !uiState.isSubmitting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldPrimary,
                        contentColor = EmeraldOnPrimary
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    if (uiState.isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = EmeraldOnPrimary
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.TaskAlt, contentDescription = null)
                            val label = if (uiState.isPaymentValid && uiState.changeAmount >= 0L) {
                                "SELESAIKAN TRANSAKSI (${formatRupiah(uiState.changeAmount)})"
                            } else {
                                "NOMINAL BELUM MENCUKUPI"
                            }
                            Text(
                                text = label,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                // QRIS FLOW MODULE
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLowest),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = SurfaceLow,
                            modifier = Modifier.size(160.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.QrCode2,
                                    contentDescription = "QRIS Code",
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(120.dp)
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Pindai QRIS Toko Harmony",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextOnSurface
                            )
                            Text(
                                text = "NMID: ID1020039201991 • Sesuai Nominal ${formatRupiah(totalAmount)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextOnSurfaceVariant
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = SurfaceContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(MintSecondary.copy(alpha = pulseAlpha))
                                )
                                Text(
                                    text = "Menunggu konfirmasi pembayaran kasir...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextOnSurfaceVariant
                                )
                            }
                        }

                        Button(
                            onClick = { viewModel.completeQrisPayment() },
                            enabled = !uiState.isSubmitting,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MintSecondary,
                                contentColor = MintOnSecondary
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                        ) {
                            if (uiState.isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MintOnSecondary
                                )
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                                    Text(
                                        text = "KONFIRMASI BAYAR QRIS (${formatRupiah(totalAmount)})",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("KEMBALI KE KERANJANG")
                        }
                    }
                }
            }
        }
    }
}

private fun formatNumber(num: Long): String {
    return String.format("%,d", num).replace(',', '.')
}

private fun formatShortNumber(num: Long): String {
    return formatNumber(num)
}

private fun calculateQuickAmounts(total: Long): List<Long> {
    if (total <= 0) return emptyList()
    val list = mutableListOf(total) // Uang Pas

    val standardDenominations = listOf(10000L, 20000L, 50000L, 100000L, 200000L)
    standardDenominations.filter { it > total }.forEach {
        if (!list.contains(it)) list.add(it)
    }

    val next10k = ((total + 9999L) / 10000L) * 10000L
    if (next10k > total && !list.contains(next10k)) list.add(next10k)

    val next50k = ((total + 49999L) / 50000L) * 50000L
    if (next50k > total && !list.contains(next50k)) list.add(next50k)

    return list.sorted().distinct()
}
