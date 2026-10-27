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
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.app.pro.mayurone.data.local.entity.EstimateItemEntity
import com.app.pro.mayurone.data.local.entity.TenantEntity
import com.app.pro.mayurone.data.local.model.CalculationBasis
import com.app.pro.mayurone.data.local.model.EstimateStatus
import com.app.pro.mayurone.data.repository.EstimateItemDraft
import com.app.pro.mayurone.domain.CalculationEngine
import com.app.pro.mayurone.ui.theme.MayurOneTheme
import com.app.pro.mayurone.ui.viewmodel.MayurOneViewModel
import java.util.Locale

@Composable
fun EstimateFormScreen(
    viewModel: MayurOneViewModel,
    existingEstimateId: String? = null,
    onNavigateBack: () -> Unit
) {
    val activeTenant by viewModel.activeTenant.collectAsState()
    val useTabularForm by viewModel.useTabularForm.collectAsState()

    val existingRevisions by remember(existingEstimateId) {
        if (existingEstimateId != null) {
            viewModel.getRevisionsForEstimateFlow(existingEstimateId)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }.collectAsState(initial = emptyList())

    val latestRevision = existingRevisions.firstOrNull()

    val existingItems by remember(latestRevision?.revisionId) {
        if (latestRevision != null) {
            viewModel.getItemsForRevisionFlow(latestRevision.revisionId)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }.collectAsState(initial = emptyList())

    EstimateFormScreen(
        activeTenant = activeTenant,
        existingEstimateId = existingEstimateId,
        initialCustomerName = latestRevision?.customerName ?: "",
        initialCustomerPhone = latestRevision?.customerPhone ?: "",
        initialCustomerAddress = latestRevision?.customerAddress ?: "",
        initialNotes = latestRevision?.notes ?: "",
        initialItems = existingItems,
        nextRevisionNumber = (latestRevision?.revisionNumber ?: 0) + 1,
        useTabularForm = useTabularForm,
        onToggleFormStyle = { viewModel.setUseTabularForm(!useTabularForm) },
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
    initialCustomerName: String = "",
    initialCustomerPhone: String = "",
    initialCustomerAddress: String = "",
    initialNotes: String = "",
    initialItems: List<EstimateItemEntity> = emptyList(),
    nextRevisionNumber: Int = 1,
    useTabularForm: Boolean = true,
    onToggleFormStyle: () -> Unit = {},
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
    var customerName by remember(initialCustomerName) { mutableStateOf(initialCustomerName) }
    var customerPhone by remember(initialCustomerPhone) { mutableStateOf(initialCustomerPhone) }
    var customerAddress by remember(initialCustomerAddress) { mutableStateOf(initialCustomerAddress) }
    val notes = initialNotes

    val itemDrafts = remember { mutableStateListOf<EstimateItemDraftState>() }

    // Populate existing revision if editing / creating new revision
    LaunchedEffect(initialItems, existingEstimateId) {
        if (initialItems.isNotEmpty()) {
            itemDrafts.clear()
            initialItems.forEach { item ->
                val qStr = if (item.quantity % 1.0 == 0.0) item.quantity.toLong().toString() else item.quantity.toString()
                val wStr = if (item.weight % 1.0 == 0.0) item.weight.toLong().toString() else String.format(Locale.US, "%.2f", item.weight)
                val rStr = if (item.rate % 1.0 == 0.0) item.rate.toLong().toString() else String.format(Locale.US, "%.2f", item.rate)

                itemDrafts.add(
                    EstimateItemDraftState(
                        description = item.description,
                        quantity = qStr,
                        weight = wStr,
                        rate = rStr,
                        calculationBasis = item.calculationBasis
                    )
                )
            }
        } else if (itemDrafts.isEmpty() && existingEstimateId == null) {
            itemDrafts.add(
                EstimateItemDraftState(
                    description = "Mild Steel Frame",
                    quantity = "1",
                    rate = "1500",
                    calculationBasis = CalculationBasis.QTY_X_RATE
                )
            )
        }
    }

    val totalAmount = CalculationEngine.calculateTotal(
        itemDrafts.map { it.calculatedAmount }
    )

    var showValidationErrorDialog by remember { mutableStateOf(false) }
    var pendingSaveStatus by remember { mutableStateOf<EstimateStatus?>(null) }
    var showValidationErrors by remember { mutableStateOf(false) }

    fun proceedSave(status: EstimateStatus) {
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

    fun handleSave(status: EstimateStatus) {
        val hasInvalidRows = itemDrafts.isEmpty() || itemDrafts.any { !it.isValid }

        if (hasInvalidRows) {
            showValidationErrors = true
            pendingSaveStatus = status
            showValidationErrorDialog = true
        } else {
            proceedSave(status)
        }
    }

    if (showValidationErrorDialog) {
        val invalidCount = itemDrafts.count { !it.isValid }
        val dialogMessage = if (itemDrafts.isEmpty()) {
            "Please add at least one line item before saving."
        } else {
            "There ${if (invalidCount == 1) "is 1 row" else "are $invalidCount rows"} with missing or zero Quantity, Item Details, Rate, or Weight.\n\nIncomplete fields are highlighted with RED borders. Would you like to remove the incomplete rows or fix them?"
        }

        AlertDialog(
            onDismissRequest = { showValidationErrorDialog = false },
            title = { Text("Incomplete Item Rows", fontWeight = FontWeight.Bold) },
            text = { Text(dialogMessage) },
            confirmButton = {
                if (itemDrafts.isNotEmpty()) {
                    Button(
                        onClick = {
                            showValidationErrorDialog = false
                            itemDrafts.removeAll { !it.isValid }
                            val targetStatus = pendingSaveStatus
                            if (itemDrafts.isNotEmpty() && targetStatus != null) {
                                proceedSave(targetStatus)
                            }
                        }
                    ) {
                        Text("Remove Empty Rows & Save")
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showValidationErrorDialog = false
                    }
                ) {
                    Text(if (itemDrafts.isEmpty()) "OK" else "Fix Details")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existingEstimateId != null) "New Revision (#$nextRevisionNumber)" else "Create Estimate") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onToggleFormStyle) {
                        Icon(
                            imageVector = if (useTabularForm) Icons.Default.ViewAgenda else Icons.Default.TableChart,
                            contentDescription = if (useTabularForm) "Switch to Card View" else "Switch to Tabular Grid"
                        )
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
                    .padding(6.dp),
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

                if (useTabularForm) {
                    item {
                        Text(
                            "Bill-Book Tabular Items",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        TabularItemGrid(
                            items = itemDrafts,
                            currencySymbol = activeTenant?.currencySymbol ?: "₹",
                            showValidationErrors = showValidationErrors,
                            onAddItem = { itemDrafts.add(EstimateItemDraftState()) },
                            onRemoveItem = { index -> itemDrafts.removeAt(index) }
                        )
                    }
                } else {
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
                            showValidationErrors = showValidationErrors,
                            onRemove = { itemDrafts.removeAt(index) }
                        )
                    }
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
    showValidationErrors: Boolean = false,
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
                isError = showValidationErrors && !state.isDescriptionValid,
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
                        onValueChange = { newValue ->
                            state.quantity = newValue.filter { it.isDigit() }
                        },
                        label = { Text("Quantity") },
                        isError = showValidationErrors && !state.isQuantityValid,
                        modifier = Modifier
                            .weight(1f)
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused && (state.quantity == "1" || state.quantity == "1.0" || state.quantity == "0" || state.quantity == "0.0")) {
                                    state.quantity = ""
                                }
                            },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                } else {
                    OutlinedTextField(
                        value = state.weight,
                        onValueChange = { newValue ->
                            val filtered = newValue.filter { it.isDigit() || it == '.' }
                            val parts = filtered.split(".")
                            if (parts.size <= 2) {
                                state.weight = if (parts.size == 2 && parts[1].length > 2) {
                                    "${parts[0]}.${parts[1].take(2)}"
                                } else {
                                    filtered
                                }
                            }
                        },
                        label = { Text("Weight (kg)") },
                        isError = showValidationErrors && !state.isWeightValid,
                        modifier = Modifier
                            .weight(1f)
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused && (state.weight == "0" || state.weight == "0.0" || state.weight == "0.00")) {
                                    state.weight = ""
                                }
                            },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = state.rate,
                    onValueChange = { newValue ->
                        val filtered = newValue.filter { it.isDigit() || it == '.' }
                        val parts = filtered.split(".")
                        if (parts.size <= 2) {
                            state.rate = if (parts.size == 2 && parts[1].length > 2) {
                                "${parts[0]}.${parts[1].take(2)}"
                            } else {
                                filtered
                            }
                        }
                    },
                    label = { Text("Rate (₹)") },
                    isError = showValidationErrors && !state.isRateValid,
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused && (state.rate == "0" || state.rate == "0.0" || state.rate == "0.00")) {
                                state.rate = ""
                            }
                        },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
    quantity: String = "1",
    weight: String = "0.00",
    rate: String = "0.00",
    calculationBasis: CalculationBasis = CalculationBasis.QTY_X_RATE
) {
    var description by mutableStateOf(description)
    var quantity by mutableStateOf(quantity)
    var weight by mutableStateOf(weight)
    var rate by mutableStateOf(rate)
    var calculationBasis by mutableStateOf(calculationBasis)

    val isQuantityValid: Boolean
        get() = quantity.isNotBlank() && (quantity.toDoubleOrNull() ?: 0.0) > 0.0

    val isDescriptionValid: Boolean
        get() = description.isNotBlank()

    val isRateValid: Boolean
        get() = rate.isNotBlank() && (rate.toDoubleOrNull() ?: 0.0) > 0.0

    val isWeightValid: Boolean
        get() = if (calculationBasis == CalculationBasis.WEIGHT_X_RATE) {
            weight.isNotBlank() && (weight.toDoubleOrNull() ?: 0.0) > 0.0
        } else true

    val isValid: Boolean
        get() = isDescriptionValid && isQuantityValid && isRateValid && isWeightValid

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

@Composable
fun TabularItemGrid(
    items: List<EstimateItemDraftState>,
    currencySymbol: String,
    showValidationErrors: Boolean = false,
    onAddItem: () -> Unit,
    onRemoveItem: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Table Header Row
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.small
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Qty",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(48.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Item details",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Weight",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(58.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Rate",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(58.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Amount",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(72.dp)
                    )
                    Spacer(modifier = Modifier.width(28.dp))
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Table Body Rows
            items.forEachIndexed { index, state ->
                TabularItemRow(
                    state = state,
                    currencySymbol = currencySymbol,
                    showValidationErrors = showValidationErrors,
                    onRemove = { onRemoveItem(index) }
                )
                if (index < items.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Table Footer Row: Add Item Button & TOTAL Block
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onAddItem,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.height(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Row")
                }

                // Total Highlight Box (Gold/Accent Yellow box like Screenshot 1)
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = MaterialTheme.shapes.small,
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TOTAL: ",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = CalculationEngine.formatCurrency(
                                CalculationEngine.calculateTotal(items.map { it.calculatedAmount }),
                                currencySymbol
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TabularItemRow(
    state: EstimateItemDraftState,
    currencySymbol: String,
    showValidationErrors: Boolean = false,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Qty
        OutlinedTextField(
            value = state.quantity,
            onValueChange = { newValue ->
                state.quantity = newValue.filter { it.isDigit() }
            },
            isError = showValidationErrors && !state.isQuantityValid,
            modifier = Modifier
                .width(48.dp)
                .onFocusChanged { focusState ->
                    if (focusState.isFocused && (state.quantity == "1" || state.quantity == "1.0" || state.quantity == "0")) {
                        state.quantity = ""
                    }
                },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.width(4.dp))

        // Item Details / Particulars
        OutlinedTextField(
            value = state.description,
            onValueChange = { state.description = it },
            placeholder = { Text("Item...", style = MaterialTheme.typography.bodySmall) },
            isError = showValidationErrors && !state.isDescriptionValid,
            modifier = Modifier.weight(1f),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.width(4.dp))

        // Weight
        OutlinedTextField(
            value = state.weight,
            onValueChange = { newValue ->
                val filtered = newValue.filter { it.isDigit() || it == '.' }
                val parts = filtered.split(".")
                if (parts.size <= 2) {
                    state.weight = if (parts.size == 2 && parts[1].length > 2) {
                        "${parts[0]}.${parts[1].take(2)}"
                    } else {
                        filtered
                    }
                    if (state.weight.isNotBlank() && (state.weight.toDoubleOrNull() ?: 0.0) > 0) {
                        state.calculationBasis = CalculationBasis.WEIGHT_X_RATE
                    } else {
                        state.calculationBasis = CalculationBasis.QTY_X_RATE
                    }
                }
            },
            isError = showValidationErrors && !state.isWeightValid,
            modifier = Modifier
                .width(58.dp)
                .onFocusChanged { focusState ->
                    if (focusState.isFocused && (state.weight == "0" || state.weight == "0.0" || state.weight == "0.00")) {
                        state.weight = ""
                    }
                },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.width(4.dp))

        // Rate
        OutlinedTextField(
            value = state.rate,
            onValueChange = { newValue ->
                val filtered = newValue.filter { it.isDigit() || it == '.' }
                val parts = filtered.split(".")
                if (parts.size <= 2) {
                    state.rate = if (parts.size == 2 && parts[1].length > 2) {
                        "${parts[0]}.${parts[1].take(2)}"
                    } else {
                        filtered
                    }
                }
            },
            isError = showValidationErrors && !state.isRateValid,
            modifier = Modifier
                .width(58.dp)
                .onFocusChanged { focusState ->
                    if (focusState.isFocused && (state.rate == "0" || state.rate == "0.0" || state.rate == "0.00")) {
                        state.rate = ""
                    }
                },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.width(4.dp))

        // Amount
        Text(
            text = CalculationEngine.formatCurrency(state.calculatedAmount, currencySymbol),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(72.dp)
        )

        // Remove Icon
        IconButton(
            onClick = onRemove,
            modifier = Modifier.width(28.dp)
        ) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete Row",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.height(18.dp)
            )
        }
    }
}



