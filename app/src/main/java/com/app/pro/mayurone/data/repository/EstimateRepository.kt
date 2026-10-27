package com.app.pro.mayurone.data.repository

import com.app.pro.mayurone.data.local.MayurOneDatabase
import com.app.pro.mayurone.data.local.entity.CustomerEntity
import com.app.pro.mayurone.data.local.entity.EstimateEntity
import com.app.pro.mayurone.data.local.entity.EstimateItemEntity
import com.app.pro.mayurone.data.local.entity.EstimateRevisionEntity
import com.app.pro.mayurone.data.local.entity.ShareEventEntity
import com.app.pro.mayurone.data.local.entity.SyncQueueEntity
import com.app.pro.mayurone.data.local.entity.TenantEntity
import com.app.pro.mayurone.data.local.entity.UserEntity
import com.app.pro.mayurone.data.local.model.CalculationBasis
import com.app.pro.mayurone.data.local.model.EstimateStatus
import com.app.pro.mayurone.data.local.model.ShareChannel
import com.app.pro.mayurone.data.local.model.SyncStatus
import com.app.pro.mayurone.data.local.model.UserRole
import com.app.pro.mayurone.domain.CalculationEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.util.UUID

class EstimateRepository(private val database: MayurOneDatabase) {

    private val tenantDao = database.tenantDao()
    private val customerDao = database.customerDao()
    private val estimateDao = database.estimateDao()
    private val syncQueueDao = database.syncQueueDao()

    val activeTenantFlow: Flow<TenantEntity?> = tenantDao.getActiveTenantFlow()

    suspend fun ensureDefaultTenant(): TenantEntity {
        val existing = activeTenantFlow.firstOrNull()
        if (existing != null) return existing

        val defaultTenant = TenantEntity(
            tenantId = UUID.randomUUID().toString(),
            businessName = "Mayur Fabrication Shop",
            ownerName = "Shop Owner",
            phone = "+91 98765 43210",
            address = "Industrial Area, Phase 1",
            currencySymbol = "₹"
        )
        tenantDao.insertTenant(defaultTenant)

        val defaultUser = UserEntity(
            userId = UUID.randomUUID().toString(),
            tenantId = defaultTenant.tenantId,
            displayName = defaultTenant.ownerName,
            phone = defaultTenant.phone,
            role = UserRole.OWNER
        )
        tenantDao.insertUser(defaultUser)

        return defaultTenant
    }

    suspend fun updateTenant(tenant: TenantEntity) {
        tenantDao.updateTenant(tenant)
        enqueueSync(tenant.tenantId, "Tenant", "UPDATE", tenant.tenantId)
    }

    fun getCustomersFlow(tenantId: String): Flow<List<CustomerEntity>> {
        return customerDao.getCustomersFlow(tenantId)
    }

    suspend fun saveCustomer(customer: CustomerEntity) {
        customerDao.insertCustomer(customer)
        enqueueSync(customer.tenantId, "Customer", "INSERT", customer.customerId)
    }

    fun getEstimatesFlow(tenantId: String): Flow<List<EstimateEntity>> {
        return estimateDao.getEstimatesFlow(tenantId)
    }

    fun getEstimateFlow(estimateId: String): Flow<EstimateEntity?> {
        return estimateDao.getEstimateFlow(estimateId)
    }

    fun getRevisionFlow(revisionId: String): Flow<EstimateRevisionEntity?> {
        return estimateDao.getRevisionFlow(revisionId)
    }

    fun getRevisionsForEstimateFlow(estimateId: String): Flow<List<EstimateRevisionEntity>> {
        return estimateDao.getRevisionsForEstimateFlow(estimateId)
    }

    fun getItemsForRevisionFlow(revisionId: String): Flow<List<EstimateItemEntity>> {
        return estimateDao.getItemsForRevisionFlow(revisionId)
    }

    fun getShareEventsFlow(estimateId: String): Flow<List<ShareEventEntity>> {
        return estimateDao.getShareEventsFlow(estimateId)
    }

    suspend fun createEstimate(
        tenantId: String,
        customerName: String,
        customerPhone: String,
        customerAddress: String,
        notes: String,
        items: List<EstimateItemDraft>,
        status: EstimateStatus = EstimateStatus.DRAFT
    ): String {
        val nextNumber = generateNextEstimateNumber(tenantId)
        val estimateId = UUID.randomUUID().toString()
        val revisionId = UUID.randomUUID().toString()

        val calculatedItems = items.mapIndexed { index, draft ->
            val itemAmount = CalculationEngine.calculateItemAmount(
                basis = draft.calculationBasis,
                quantity = draft.quantity,
                weight = draft.weight,
                rate = draft.rate
            )
            EstimateItemEntity(
                itemId = UUID.randomUUID().toString(),
                revisionId = revisionId,
                tenantId = tenantId,
                itemOrder = index + 1,
                description = draft.description,
                quantity = draft.quantity,
                rate = draft.rate,
                weight = draft.weight,
                calculationBasis = draft.calculationBasis,
                amount = itemAmount
            )
        }

        val totalAmount = CalculationEngine.calculateTotal(calculatedItems.map { it.amount })

        val revision = EstimateRevisionEntity(
            revisionId = revisionId,
            estimateId = estimateId,
            tenantId = tenantId,
            revisionNumber = 1,
            parentRevisionId = null,
            customerName = customerName,
            customerPhone = customerPhone,
            customerAddress = customerAddress,
            notes = notes,
            totalAmount = totalAmount,
            createdAt = System.currentTimeMillis()
        )

        val estimate = EstimateEntity(
            estimateId = estimateId,
            tenantId = tenantId,
            estimateNumber = nextNumber,
            status = status,
            currentRevisionId = revisionId,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        estimateDao.createEstimateWithRevisionAndItems(estimate, revision, calculatedItems)
        enqueueSync(tenantId, "Estimate", "INSERT", estimateId)

        return estimateId
    }

    suspend fun createNewRevision(
        estimateId: String,
        tenantId: String,
        customerName: String,
        customerPhone: String,
        customerAddress: String,
        notes: String,
        items: List<EstimateItemDraft>,
        status: EstimateStatus = EstimateStatus.FINAL
    ): String {
        val existingEstimate = estimateDao.getEstimateById(estimateId)
            ?: throw IllegalArgumentException("Estimate not found: $estimateId")

        val currentRevision = existingEstimate.currentRevisionId?.let { estimateDao.getRevisionById(it) }
        val nextRevNumber = (currentRevision?.revisionNumber ?: 0) + 1
        val newRevisionId = UUID.randomUUID().toString()

        val calculatedItems = items.mapIndexed { index, draft ->
            val itemAmount = CalculationEngine.calculateItemAmount(
                basis = draft.calculationBasis,
                quantity = draft.quantity,
                weight = draft.weight,
                rate = draft.rate
            )
            EstimateItemEntity(
                itemId = UUID.randomUUID().toString(),
                revisionId = newRevisionId,
                tenantId = tenantId,
                itemOrder = index + 1,
                description = draft.description,
                quantity = draft.quantity,
                rate = draft.rate,
                weight = draft.weight,
                calculationBasis = draft.calculationBasis,
                amount = itemAmount
            )
        }

        val totalAmount = CalculationEngine.calculateTotal(calculatedItems.map { it.amount })

        val newRevision = EstimateRevisionEntity(
            revisionId = newRevisionId,
            estimateId = estimateId,
            tenantId = tenantId,
            revisionNumber = nextRevNumber,
            parentRevisionId = currentRevision?.revisionId,
            customerName = customerName,
            customerPhone = customerPhone,
            customerAddress = customerAddress,
            notes = notes,
            totalAmount = totalAmount,
            createdAt = System.currentTimeMillis()
        )

        val updatedEstimate = existingEstimate.copy(
            status = status,
            currentRevisionId = newRevisionId,
            updatedAt = System.currentTimeMillis()
        )

        estimateDao.addRevisionToEstimate(updatedEstimate, newRevision, calculatedItems)
        enqueueSync(tenantId, "EstimateRevision", "INSERT", newRevisionId)

        return newRevisionId
    }

    suspend fun logShareEvent(
        tenantId: String,
        estimateId: String,
        revisionId: String,
        channel: ShareChannel
    ) {
        val shareEvent = ShareEventEntity(
            shareId = UUID.randomUUID().toString(),
            tenantId = tenantId,
            estimateId = estimateId,
            revisionId = revisionId,
            channel = channel,
            sharedAt = System.currentTimeMillis()
        )
        estimateDao.insertShareEvent(shareEvent)
        enqueueSync(tenantId, "ShareEvent", "INSERT", shareEvent.shareId)
    }

    fun getSyncQueueFlow(tenantId: String): Flow<List<SyncQueueEntity>> {
        return syncQueueDao.getSyncQueueFlow(tenantId)
    }

    fun getPendingSyncCountFlow(tenantId: String): Flow<Int> {
        return syncQueueDao.getPendingSyncCountFlow(tenantId)
    }

    private suspend fun generateNextEstimateNumber(tenantId: String): String {
        val lastNumber = estimateDao.getLastEstimateNumber(tenantId)
        if (lastNumber == null) return "EST-0001"

        val digits = lastNumber.substringAfterLast("-", "").toIntOrNull()
        return if (digits != null) {
            "EST-${String.format(java.util.Locale.US, "%04d", digits + 1)}"
        } else {
            "EST-${UUID.randomUUID().toString().take(4).uppercase()}"
        }
    }

    private suspend fun enqueueSync(tenantId: String, entityType: String, operationType: String, entityId: String) {
        val syncOp = SyncQueueEntity(
            operationId = UUID.randomUUID().toString(),
            tenantId = tenantId,
            entityType = entityType,
            operationType = operationType,
            payloadJson = "{\"entityId\": \"$entityId\"}",
            syncStatus = SyncStatus.PENDING,
            createdAt = System.currentTimeMillis()
        )
        syncQueueDao.enqueueOperation(syncOp)
    }
}

data class EstimateItemDraft(
    val description: String,
    val quantity: Double = 1.0,
    val rate: Double = 0.0,
    val weight: Double = 0.0,
    val calculationBasis: CalculationBasis = CalculationBasis.QTY_X_RATE
)
