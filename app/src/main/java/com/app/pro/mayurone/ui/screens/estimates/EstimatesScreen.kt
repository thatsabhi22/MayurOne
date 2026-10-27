package com.app.pro.mayurone.ui.screens.estimates

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.app.pro.mayurone.data.local.entity.EstimateEntity
import com.app.pro.mayurone.data.local.entity.EstimateRevisionEntity
import com.app.pro.mayurone.data.local.model.EstimateStatus
import com.app.pro.mayurone.domain.CalculationEngine
import com.app.pro.mayurone.ui.theme.MayurOneTheme
import com.app.pro.mayurone.ui.viewmodel.MayurOneViewModel
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun EstimatesScreen(
    viewModel: MayurOneViewModel,
    onNavigateToCreate: () -> Unit,
    onNavigateToDetail: (String) -> Unit
) {
    val estimates by viewModel.estimates.collectAsState()
    val activeTenant by viewModel.activeTenant.collectAsState()

    EstimatesScreen(
        estimates = estimates,
        currencySymbol = activeTenant?.currencySymbol ?: "₹",
        getRevisionFlow = { viewModel.getRevisionFlow(it) },
        onNavigateToCreate = onNavigateToCreate,
        onNavigateToDetail = onNavigateToDetail
    )
}

enum class DateFilter(val label: String) {
    ALL("All Dates"),
    TODAY("Today"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month")
}

@Composable
fun EstimatesScreen(
    estimates: List<EstimateEntity>,
    currencySymbol: String = "₹",
    getRevisionFlow: ((String) -> Flow<EstimateRevisionEntity?>)? = null,
    getRevision: ((String) -> EstimateRevisionEntity?)? = null,
    onNavigateToCreate: () -> Unit = {},
    onNavigateToDetail: (String) -> Unit = {}
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedDateFilter by remember { mutableStateOf(DateFilter.ALL) }

    val tabs = listOf("All", "Drafts", "Final")

    val revisionMap = remember { mutableStateMapOf<String, EstimateRevisionEntity>() }

    estimates.forEach { estimate ->
        estimate.currentRevisionId?.let { revId ->
            if (getRevision != null) {
                getRevision(revId)?.let { revisionMap[revId] = it }
            } else if (getRevisionFlow != null) {
                val revState = getRevisionFlow(revId).collectAsState(initial = null)
                revState.value?.let { revisionMap[revId] = it }
            }
        }
    }

    val filteredEstimates = estimates.filter { estimate ->
        val revision = estimate.currentRevisionId?.let { revisionMap[it] }

        val matchesTab = when (selectedTabIndex) {
            1 -> estimate.status == EstimateStatus.DRAFT
            2 -> estimate.status == EstimateStatus.FINAL
            else -> true
        }

        val dateMs = revision?.createdAt ?: estimate.createdAt
        val matchesDate = matchesDateFilter(dateMs, selectedDateFilter)

        val matchesSearch = matchesSearchQuery(estimate, revision, searchQuery)

        matchesTab && matchesDate && matchesSearch
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToCreate,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New Estimate") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search ...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true
            )

            // Date Quick Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DateFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = selectedDateFilter == filter,
                        onClick = { selectedDateFilter = filter },
                        label = { Text(filter.label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tabs
            PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }

            if (filteredEstimates.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            modifier = Modifier
                                .height(64.dp)
                                .width(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotBlank() || selectedDateFilter != DateFilter.ALL) {
                                "No matching estimates found"
                            } else {
                                "No estimates found"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (searchQuery.isNotBlank() || selectedDateFilter != DateFilter.ALL) {
                                "Try adjusting your search query or date filter"
                            } else {
                                "Tap '+ New Estimate' to create your first estimate"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )

                        if (searchQuery.isNotBlank() || selectedDateFilter != DateFilter.ALL) {
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = {
                                    searchQuery = ""
                                    selectedDateFilter = DateFilter.ALL
                                }
                            ) {
                                Text("Clear Filters")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
                ) {
                    items(filteredEstimates, key = { it.estimateId }) { estimate ->
                        EstimateCard(
                            estimate = estimate,
                            currencySymbol = currencySymbol,
                            getRevisionFlow = getRevisionFlow,
                            getRevision = getRevision,
                            onClick = { onNavigateToDetail(estimate.estimateId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EstimateCard(
    estimate: EstimateEntity,
    currencySymbol: String,
    getRevisionFlow: ((String) -> Flow<EstimateRevisionEntity?>)? = null,
    getRevision: ((String) -> EstimateRevisionEntity?)? = null,
    onClick: () -> Unit
) {
    val currentRevision = estimate.currentRevisionId?.let { revId ->
        getRevision?.invoke(revId) ?: getRevisionFlow?.invoke(revId)
            ?.collectAsState(initial = null)?.value
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = estimate.estimateNumber,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val statusColor = if (estimate.status == EstimateStatus.FINAL) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.secondary
                    }
                    Text(
                        text = estimate.status.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = statusColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            currentRevision?.let { rev ->
                Text(
                    text = "Customer: ${rev.customerName}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                    Text(
                        text = "Rev #${rev.revisionNumber} • ${dateFormat.format(Date(rev.createdAt))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = CalculationEngine.formatCurrency(rev.totalAmount, currencySymbol),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EstimatesScreenPreview() {
    val sampleEstimates = listOf(
        EstimateEntity(
            estimateId = "est_1",
            tenantId = "tenant_1",
            estimateNumber = "EST-2023-001",
            status = EstimateStatus.DRAFT,
            currentRevisionId = "rev_1",
            createdAt = System.currentTimeMillis() - 86400000L
        ),
        EstimateEntity(
            estimateId = "est_2",
            tenantId = "tenant_1",
            estimateNumber = "EST-2023-002",
            status = EstimateStatus.FINAL,
            currentRevisionId = "rev_2",
            createdAt = System.currentTimeMillis()
        )
    )

    val sampleRevisions = mapOf(
        "rev_1" to EstimateRevisionEntity(
            revisionId = "rev_1",
            estimateId = "est_1",
            tenantId = "tenant_1",
            revisionNumber = 1,
            customerName = "Acme Corp",
            customerPhone = "555-0100",
            customerAddress = "123 Business Rd",
            notes = "Draft estimate for office supplies",
            totalAmount = 15000.0,
            createdAt = System.currentTimeMillis() - 86400000L
        ),
        "rev_2" to EstimateRevisionEntity(
            revisionId = "rev_2",
            estimateId = "est_2",
            tenantId = "tenant_1",
            revisionNumber = 2,
            customerName = "Global Logistics",
            customerPhone = "555-0200",
            customerAddress = "456 Commerce St",
            notes = "Finalized estimate",
            totalAmount = 48500.50,
            createdAt = System.currentTimeMillis()
        )
    )

    MayurOneTheme {
        EstimatesScreen(
            estimates = sampleEstimates,
            currencySymbol = "₹",
            getRevision = { revisionId -> sampleRevisions[revisionId] }
        )
    }
}

private fun matchesDateFilter(dateMs: Long, dateFilter: DateFilter): Boolean {
    if (dateFilter == DateFilter.ALL) return true

    val now = Calendar.getInstance()
    val itemCal = Calendar.getInstance().apply { timeInMillis = dateMs }

    return when (dateFilter) {
        DateFilter.TODAY -> {
            now.get(Calendar.YEAR) == itemCal.get(Calendar.YEAR) &&
                    now.get(Calendar.DAY_OF_YEAR) == itemCal.get(Calendar.DAY_OF_YEAR)
        }
        DateFilter.THIS_WEEK -> {
            now.get(Calendar.YEAR) == itemCal.get(Calendar.YEAR) &&
                    now.get(Calendar.WEEK_OF_YEAR) == itemCal.get(Calendar.WEEK_OF_YEAR)
        }
        DateFilter.THIS_MONTH -> {
            now.get(Calendar.YEAR) == itemCal.get(Calendar.YEAR) &&
                    now.get(Calendar.MONTH) == itemCal.get(Calendar.MONTH)
        }
    }
}

private fun matchesSearchQuery(
    estimate: EstimateEntity,
    revision: EstimateRevisionEntity?,
    query: String
): Boolean {
    if (query.isBlank()) return true
    val q = query.trim()

    // 1. Estimate Number
    if (estimate.estimateNumber.contains(q, ignoreCase = true)) return true

    // 2. Customer Details
    if (revision != null) {
        if (revision.customerName.contains(q, ignoreCase = true)) return true
        if (revision.customerPhone.contains(q, ignoreCase = true)) return true
        if (revision.notes.contains(q, ignoreCase = true)) return true
    }

    // 3. Formatted Date
    val dateMs = revision?.createdAt ?: estimate.createdAt
    val dateObj = Date(dateMs)

    val formatFull = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(dateObj)
    val formatShort = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(dateObj)
    val formatMonth = SimpleDateFormat("MMMM", Locale.getDefault()).format(dateObj)
    val formatDayMonth = SimpleDateFormat("dd MMM", Locale.getDefault()).format(dateObj)

    if (formatFull.contains(q, ignoreCase = true)) return true
    if (formatShort.contains(q, ignoreCase = true)) return true
    if (formatMonth.contains(q, ignoreCase = true)) return true
    if (formatDayMonth.contains(q, ignoreCase = true)) return true

    // 4. Relative terms
    val now = Calendar.getInstance()
    val itemCal = Calendar.getInstance().apply { timeInMillis = dateMs }

    val isToday = now.get(Calendar.YEAR) == itemCal.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == itemCal.get(Calendar.DAY_OF_YEAR)

    val isYesterday = now.get(Calendar.YEAR) == itemCal.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) - itemCal.get(Calendar.DAY_OF_YEAR) == 1

    if (isToday && "today".contains(q, ignoreCase = true)) return true
    if (isYesterday && "yesterday".contains(q, ignoreCase = true)) return true

    return false
}


