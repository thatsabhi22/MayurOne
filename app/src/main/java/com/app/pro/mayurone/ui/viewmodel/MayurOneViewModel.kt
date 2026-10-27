package com.app.pro.mayurone.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.app.pro.mayurone.data.local.MayurOneDatabase
import com.app.pro.mayurone.data.local.entity.CustomerEntity
import com.app.pro.mayurone.data.local.entity.EstimateEntity
import com.app.pro.mayurone.data.local.entity.EstimateItemEntity
import com.app.pro.mayurone.data.local.entity.EstimateRevisionEntity
import com.app.pro.mayurone.data.local.entity.ShareEventEntity
import com.app.pro.mayurone.data.local.entity.SyncQueueEntity
import com.app.pro.mayurone.data.local.entity.TenantEntity
import com.app.pro.mayurone.data.local.model.EstimateStatus
import com.app.pro.mayurone.data.local.model.ShareChannel
import com.app.pro.mayurone.data.repository.EstimateItemDraft
import com.app.pro.mayurone.data.repository.EstimateRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class MayurOneViewModel(application: Application) : AndroidViewModel(application) {

    private val database = MayurOneDatabase.getDatabase(application)
    val repository = EstimateRepository(database)

    val activeTenant: StateFlow<TenantEntity?> = repository.activeTenantFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val estimates: StateFlow<List<EstimateEntity>> = activeTenant.flatMapLatest { tenant ->
        if (tenant != null) repository.getEstimatesFlow(tenant.tenantId) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customers: StateFlow<List<CustomerEntity>> = activeTenant.flatMapLatest { tenant ->
        if (tenant != null) repository.getCustomersFlow(tenant.tenantId) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingSyncCount: StateFlow<Int> = activeTenant.flatMapLatest { tenant ->
        if (tenant != null) repository.getPendingSyncCountFlow(tenant.tenantId) else flowOf(0)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val syncQueue: StateFlow<List<SyncQueueEntity>> = activeTenant.flatMapLatest { tenant ->
        if (tenant != null) repository.getSyncQueueFlow(tenant.tenantId) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.ensureDefaultTenant()
        }
    }

    fun updateTenant(businessName: String, ownerName: String, phone: String, address: String) {
        val current = activeTenant.value ?: return
        viewModelScope.launch {
            repository.updateTenant(
                current.copy(
                    businessName = businessName,
                    ownerName = ownerName,
                    phone = phone,
                    address = address
                )
            )
        }
    }

    fun saveCustomer(name: String, phone: String, address: String, notes: String) {
        val tenantId = activeTenant.value?.tenantId ?: return
        viewModelScope.launch {
            val customer = CustomerEntity(
                customerId = java.util.UUID.randomUUID().toString(),
                tenantId = tenantId,
                name = name,
                phone = phone,
                address = address,
                notes = notes,
                updatedAt = System.currentTimeMillis()
            )
            repository.saveCustomer(customer)
        }
    }

    fun createEstimate(
        customerName: String,
        customerPhone: String,
        customerAddress: String,
        notes: String,
        items: List<EstimateItemDraft>,
        status: EstimateStatus,
        onCreated: (String) -> Unit
    ) {
        val tenantId = activeTenant.value?.tenantId ?: return
        viewModelScope.launch {
            val estimateId = repository.createEstimate(
                tenantId = tenantId,
                customerName = customerName,
                customerPhone = customerPhone,
                customerAddress = customerAddress,
                notes = notes,
                items = items,
                status = status
            )
            onCreated(estimateId)
        }
    }

    fun createNewRevision(
        estimateId: String,
        customerName: String,
        customerPhone: String,
        customerAddress: String,
        notes: String,
        items: List<EstimateItemDraft>,
        status: EstimateStatus,
        onCreated: (String) -> Unit
    ) {
        val tenantId = activeTenant.value?.tenantId ?: return
        viewModelScope.launch {
            val revisionId = repository.createNewRevision(
                estimateId = estimateId,
                tenantId = tenantId,
                customerName = customerName,
                customerPhone = customerPhone,
                customerAddress = customerAddress,
                notes = notes,
                items = items,
                status = status
            )
            onCreated(revisionId)
        }
    }

    fun logShareEvent(estimateId: String, revisionId: String, channel: ShareChannel) {
        val tenantId = activeTenant.value?.tenantId ?: return
        viewModelScope.launch {
            repository.logShareEvent(tenantId, estimateId, revisionId, channel)
        }
    }

    fun getEstimateFlow(estimateId: String): Flow<EstimateEntity?> =
        repository.getEstimateFlow(estimateId)

    fun getRevisionFlow(revisionId: String): Flow<EstimateRevisionEntity?> =
        repository.getRevisionFlow(revisionId)

    fun getRevisionsForEstimateFlow(estimateId: String): Flow<List<EstimateRevisionEntity>> =
        repository.getRevisionsForEstimateFlow(estimateId)

    fun getItemsForRevisionFlow(revisionId: String): Flow<List<EstimateItemEntity>> =
        repository.getItemsForRevisionFlow(revisionId)

    fun getShareEventsFlow(estimateId: String): Flow<List<ShareEventEntity>> =
        repository.getShareEventsFlow(estimateId)
}

class MayurOneViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MayurOneViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MayurOneViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
