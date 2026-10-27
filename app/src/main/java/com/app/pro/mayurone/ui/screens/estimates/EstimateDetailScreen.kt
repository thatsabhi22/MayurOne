package com.app.pro.mayurone.ui.screens.estimates

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.app.pro.mayurone.data.local.model.CalculationBasis
import com.app.pro.mayurone.data.local.model.ShareChannel
import com.app.pro.mayurone.domain.CalculationEngine
import com.app.pro.mayurone.pdf.PdfGenerator
import com.app.pro.mayurone.pdf.ShareHelper
import com.app.pro.mayurone.ui.viewmodel.MayurOneViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EstimateDetailScreen(
    viewModel: MayurOneViewModel,
    estimateId: String,
    onNavigateBack: () -> Unit,
    onNavigateToNewRevision: (String) -> Unit
) {
    val context = LocalContext.current
    val activeTenant by viewModel.activeTenant.collectAsState()

    val estimate by viewModel.getEstimateFlow(estimateId).collectAsState(initial = null)
    val revisions by viewModel.getRevisionsForEstimateFlow(estimateId).collectAsState(initial = emptyList())
    val shareEvents by viewModel.getShareEventsFlow(estimateId).collectAsState(initial = emptyList())

    val currentRevision = revisions.firstOrNull()
    val items by currentRevision?.let {
        viewModel.getItemsForRevisionFlow(it.revisionId).collectAsState(initial = emptyList())
    } ?: remember { androidx.compose.runtime.mutableStateOf(emptyList()) }

    fun sharePdf(targetWhatsApp: Boolean) {
        val t = activeTenant ?: return
        val e = estimate ?: return
        val r = currentRevision ?: return

        val file = PdfGenerator.generateEstimatePdf(context, t, e, r, items)
        val fileUri = PdfGenerator.getFileUri(context, file)

        val channel = if (targetWhatsApp) ShareChannel.WHATSAPP else ShareChannel.SYSTEM_SHARE
        viewModel.logShareEvent(e.estimateId, r.revisionId, channel)

        ShareHelper.sharePdf(context, fileUri, targetWhatsApp)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(estimate?.estimateNumber ?: "Estimate Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                currentRevision?.let { rev ->
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Revision #${rev.revisionNumber} (${estimate?.status})",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                                    Text(
                                        text = dateFormat.format(Date(rev.createdAt)),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Total: ${CalculationEngine.formatCurrency(rev.totalAmount, activeTenant?.currencySymbol ?: "₹")}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("CUSTOMER DETAILS", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Name: ${rev.customerName}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                if (rev.customerPhone.isNotBlank()) {
                                    Text("Phone: ${rev.customerPhone}", style = MaterialTheme.typography.bodyMedium)
                                }
                                if (rev.customerAddress.isNotBlank()) {
                                    Text("Address: ${rev.customerAddress}", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }

                    item {
                        Text("LINE ITEMS", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }

                    items(items, key = { it.itemId }) { item ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.description, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                    val basisLabel = if (item.calculationBasis == CalculationBasis.QTY_X_RATE) {
                                        "${item.quantity} × ₹${item.rate}"
                                    } else {
                                        "${item.weight} kg × ₹${item.rate}"
                                    }
                                    Text(basisLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(
                                    text = "₹ ${item.amount}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    if (rev.notes.isNotBlank()) {
                        item {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Notes & Terms", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(rev.notes, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }

                    if (shareEvents.isNotEmpty()) {
                        item {
                            Text("SHARE ACTIVITY LOG", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                        items(shareEvents, key = { it.shareId }) { share ->
                            val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                            Text(
                                text = "• Shared via ${share.channel.name} on ${dateFormat.format(Date(share.sharedAt))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Bottom Actions Bar
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { sharePdf(targetWhatsApp = false) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share PDF")
                        }

                        Button(
                            onClick = { sharePdf(targetWhatsApp = true) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("WhatsApp")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { onNavigateToNewRevision(estimateId) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create New Revision")
                    }
                }
            }
        }
    }
}
