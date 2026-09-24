package com.harmony.tokoharmony.feature.admin.product

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.harmony.tokoharmony.core.common.Formatters
import com.harmony.tokoharmony.ui.theme.CoralError
import com.harmony.tokoharmony.ui.theme.EmeraldOnPrimary
import com.harmony.tokoharmony.ui.theme.EmeraldPrimary
import com.harmony.tokoharmony.ui.theme.SurfaceLow
import com.harmony.tokoharmony.ui.theme.SurfaceLowest
import com.harmony.tokoharmony.ui.theme.TextOnSurface
import com.harmony.tokoharmony.ui.theme.TextOnSurfaceVariant

@Composable
fun PriceChangeDialog(
    productName: String,
    currentPrice: Long,
    sellingUnit: String,
    onDismiss: () -> Unit,
    onConfirmPriceChange: (Long) -> Unit
) {
    var newPriceText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceLowest,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Ubah Harga Master",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextOnSurface
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = productName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TextOnSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Harga saat ini: ${Formatters.formatRupiah(currentPrice)} / $sellingUnit",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextOnSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = newPriceText,
                    onValueChange = {
                        val filtered = it.filter { c -> c.isDigit() }
                        newPriceText = filtered
                        errorMessage = null
                    },
                    label = { Text("Harga Baru (Rp)") },
                    placeholder = { Text("Masukkan harga baru") },
                    prefix = { Text("Rp ") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let { { Text(it, color = CoralError) } },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        cursorColor = EmeraldPrimary,
                        focusedLabelColor = EmeraldPrimary,
                        unfocusedContainerColor = SurfaceLow,
                        focusedContainerColor = SurfaceLowest
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val priceLong = newPriceText.toLongOrNull()
                    if (priceLong == null || priceLong < 0) {
                        errorMessage = "Masukkan nominal harga valid (>= 0)"
                        return@Button
                    }
                    onConfirmPriceChange(priceLong)
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldPrimary,
                    contentColor = EmeraldOnPrimary
                )
            ) {
                Text("Simpan Perubahan", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = TextOnSurfaceVariant
                )
            ) {
                Text("Batal", fontWeight = FontWeight.SemiBold)
            }
        }
    )
}
