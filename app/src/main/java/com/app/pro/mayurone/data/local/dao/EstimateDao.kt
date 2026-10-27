package com.app.pro.mayurone.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.app.pro.mayurone.data.local.entity.EstimateEntity
import com.app.pro.mayurone.data.local.entity.EstimateItemEntity
import com.app.pro.mayurone.data.local.entity.EstimateRevisionEntity
import com.app.pro.mayurone.data.local.entity.ShareEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EstimateDao {

    @Query("SELECT * FROM estimates WHERE tenantId = :tenantId ORDER BY updatedAt DESC")
    fun getEstimatesFlow(tenantId: String): Flow<List<EstimateEntity>>

    @Query("SELECT * FROM estimates WHERE estimateId = :estimateId")
    suspend fun getEstimateById(estimateId: String): EstimateEntity?

    @Query("SELECT * FROM estimates WHERE estimateId = :estimateId")
    fun getEstimateFlow(estimateId: String): Flow<EstimateEntity?>

    @Query("SELECT * FROM estimate_revisions WHERE revisionId = :revisionId")
    suspend fun getRevisionById(revisionId: String): EstimateRevisionEntity?

    @Query("SELECT * FROM estimate_revisions WHERE revisionId = :revisionId")
    fun getRevisionFlow(revisionId: String): Flow<EstimateRevisionEntity?>

    @Query("SELECT * FROM estimate_revisions WHERE estimateId = :estimateId ORDER BY revisionNumber DESC")
    fun getRevisionsForEstimateFlow(estimateId: String): Flow<List<EstimateRevisionEntity>>

    @Query("SELECT * FROM estimate_items WHERE revisionId = :revisionId ORDER BY itemOrder ASC")
    fun getItemsForRevisionFlow(revisionId: String): Flow<List<EstimateItemEntity>>

    @Query("SELECT * FROM estimate_items WHERE revisionId = :revisionId ORDER BY itemOrder ASC")
    suspend fun getItemsForRevision(revisionId: String): List<EstimateItemEntity>

    @Query("SELECT * FROM share_events WHERE estimateId = :estimateId ORDER BY sharedAt DESC")
    fun getShareEventsFlow(estimateId: String): Flow<List<ShareEventEntity>>

    @Query("SELECT estimateNumber FROM estimates WHERE tenantId = :tenantId ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLastEstimateNumber(tenantId: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEstimate(estimate: EstimateEntity)

    @Update
    suspend fun updateEstimate(estimate: EstimateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRevision(revision: EstimateRevisionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<EstimateItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShareEvent(shareEvent: ShareEventEntity)

    @Transaction
    suspend fun createEstimateWithRevisionAndItems(
        estimate: EstimateEntity,
        revision: EstimateRevisionEntity,
        items: List<EstimateItemEntity>
    ) {
        insertEstimate(estimate)
        insertRevision(revision)
        insertItems(items)
    }

    @Transaction
    suspend fun addRevisionToEstimate(
        estimate: EstimateEntity,
        revision: EstimateRevisionEntity,
        items: List<EstimateItemEntity>
    ) {
        insertRevision(revision)
        insertItems(items)
        updateEstimate(estimate)
    }
}
