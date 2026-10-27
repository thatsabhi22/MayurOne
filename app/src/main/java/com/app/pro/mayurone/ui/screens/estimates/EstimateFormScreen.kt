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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.app.pro.mayurone.data.local.entity.TenantEntity
import com.app.pro.mayurone.data.local.model.CalculationBasis
import com.app.pro.mayurone.data.local.model.EstimateStatus
import com.app.pro.mayurone.data.repository.EstimateItemDraft
import com.app.pro.mayurone.domain.CalculationEngine
import com.app.pro.mayurone.ui.theme.MayurOneTheme
import com.app.pro.mayurone.ui.viewmodel.MayurOneViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EstimateFormScreen(
    viewModel: MayurOneViewModel,
    existingEstimateId: String? = null,
    onNavigateBack: () -> Unit
) {
    val activeTenant by viewModel.activeTenant.collectAsState()

    EstimateFormScreen(
        activeTenant = activeTenant,
        existingEstimateId = existingEstimateId,
        onNavigateBack = onNavigateBack,
        onSaveEstimate = { name, phone, address, notes, items, status ->
            if (existingEstimateId != null) {
                viewModel.createNewRevision(
                    estimateId = existingEstimateId,
                    customerName = name,
                    customerPhone = phone,
                    customerAddress = address,
                    notes = notes,
                    items = items,
                    status = status,
                    onCreated = { onNavigateBack() }
                )
            } else {
                viewModel.createEstimate(
                    customerName = name,
                    customerPhone = phone,
                    customerAddress = address,
                    notes = notes,
                    items = items,
                    status = status,
                    onCreated = { onNavigateBack() }
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EstimateFormScreen(
    activeTenant: TenantEntity?,
    existingEstimateId: String? = null,
    onNavigateBack: () -> Unit = {},
    onSaveEstimate: (
        name: String,
        phone: String,
        address: String,
        notes: String,
        items: List<EstimateItemDraft>,
        status: EstimateStatus
    ) -> Unit = { _, _, _, _, _, _ -> }
) {
    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var customerAddress by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("Valid for 15 days. Payment terms: 50% advance.") }

    val itemDrafts = remember { mutableStateListOf<EstimateItemDraftState>() }

    // Populate existing revision if editing / creating new revision
    LaunchedEffect(existingEstimateId) {
        if (existingEstimateId != null) {
            // Load latest revision details if editing
        } else if (itemDrafts.isEmpty()) {
            itemDrafts.add(
                EstimateItemDraftState(
                    description = "Mild Steel Frame",
                    quantity = "1.0",
                    rate = "1500.0",
                    calculationBasis = CalculationBasis.QTY_X_RATE
                )
            )
        }
    }

    val totalAmount = CalculationEngine.calculateTotal(
        itemDrafts.map { it.calculatedAmount }
    )

    fun handleSave(status: EstimateStatus) {
        val drafts = itemDrafts.map {
            EstimateItemDraft(
                description = it.description.ifBlank { "Unspecified Item" },
                quantity = it.quantity.toDoubleOrNull() ?: 1.0,
                weight = it.weight.toDoubleOrNull() ?: 0.0,
                rate = it.rate.toDoubleOrNull() ?: 0.0,
                calculationBasis = it.calculationBasis
            )
        }

        val name = customerName.ifBlank { "Walk-in Customer" }

        onSaveEstimate(name, customerPhone, customerAddress, notes, drafts, status)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existingEstimateId != null) "New Revision" else "Create Estimate") },
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
                item {
                    Text(
                        "Customer Information",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { customerName = it },
                        label = { Text("Customer Name *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = customerPhone,
                            onValueChange = { customerPhone = it },
                            label = { Text("Phone Number") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = customerAddress,
                            onValueChange = { customerAddress = it },
                            label = { Text("Address / Location") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Estimate Line Items",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedButton(
                            onClick = {
                                itemDrafts.add(EstimateItemDraftState())
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Item")
                        }
                    }
                }

                itemsIndexed(itemDrafts) { index, itemState ->
                    ItemDraftCard(
                        index = index + 1,
                        state = itemState,
                        onRemove = { itemDrafts.removeAt(index) }
                    )
                }

                item {
                    Text(
                        "Notes & Terms",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            }

            // Summary Footer
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Total Estimated Amount:",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = CalculationEngine.formatCurrency(
                                totalAmount,
                                activeTenant?.currencySymbol ?: "₹"
                            ),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { handleSave(EstimateStatus.DRAFT) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Save Draft")
                        }

                        Button(
                            onClick = { handleSave(EstimateStatus.FINAL) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Finalize Estimate")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ItemDraftCard(
    index: Int,
    state: EstimateItemDraftState,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Item #$index",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete Item",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            OutlinedTextField(
                value = state.description,
                onValueChange = { state.description = it },
                label = { Text("Item Description *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text("Calculation Basis:", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.calculationBasis == CalculationBasis.QTY_X_RATE,
                    onClick = { state.calculationBasis = CalculationBasis.QTY_X_RATE },
                    label = { Text("Quantity × Rate") }
                )
                FilterChip(
                    selected = state.calculationBasis == CalculationBasis.WEIGHT_X_RATE,
                    onClick = { state.calculationBasis = CalculationBasis.WEIGHT_X_RATE },
                    label = { Text("Weight (kg) × Rate") }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.calculationBasis == CalculationBasis.QTY_X_RATE) {
                    OutlinedTextField(
                        value = state.quantity,
                        onValueChange = { state.quantity = it },
                        label = { Text("Quantity") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                } else {
                    OutlinedTextField(
                        value = state.weight,
                        onValueChange = { state.weight = it },
                        label = { Text("Weight (kg)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = state.rate,
                    onValueChange = { state.rate = it },
                    label = { Text("Rate (₹)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "Amount: ₹ ${state.calculatedAmount}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

class EstimateItemDraftState(
    description: String = "",
    quantity: String = "1.0",
    weight: String = "0.0",
    rate: String = "0.0",
    calculationBasis: CalculationBasis = CalculationBasis.QTY_X_RATE
) {
    var description by mutableStateOf(description)
    var quantity by mutableStateOf(quantity)
    var weight by mutableStateOf(weight)
    var rate by mutableStateOf(rate)
    var calculationBasis by mutableStateOf(calculationBasis)

    val calculatedAmount: Double
        get() {
            val q = quantity.toDoubleOrNull() ?: 0.0
            val w = weight.toDoubleOrNull() ?: 0.0
            val r = rate.toDoubleOrNull() ?: 0.0
            return CalculationEngine.calculateItemAmount(calculationBasis, q, w, r)
        }
}

@Preview(showBackground = true)
@Composable
fun EstimateFormScreenPreview() {
    val sampleTenant = TenantEntity(
        tenantId = "tenant_1",
        businessName = "Mayur Traders",
        ownerName = "Mayur Patel",
        phone = "9876543210",
        address = "123 Market Yard, Ahmedabad",
        currencySymbol = "₹"
    )

    MayurOneTheme {
        EstimateFormScreen(
            activeTenant = sampleTenant
        )
    }
}

