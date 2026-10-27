package com.app.pro.mayurone.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.app.pro.mayurone.data.local.converter.Converters
import com.app.pro.mayurone.data.local.dao.CustomerDao
import com.app.pro.mayurone.data.local.dao.EstimateDao
import com.app.pro.mayurone.data.local.dao.SyncQueueDao
import com.app.pro.mayurone.data.local.dao.TenantDao
import com.app.pro.mayurone.data.local.entity.CustomerEntity
import com.app.pro.mayurone.data.local.entity.EstimateEntity
import com.app.pro.mayurone.data.local.entity.EstimateItemEntity
import com.app.pro.mayurone.data.local.entity.EstimateRevisionEntity
import com.app.pro.mayurone.data.local.entity.ShareEventEntity
import com.app.pro.mayurone.data.local.entity.SyncQueueEntity
import com.app.pro.mayurone.data.local.entity.TenantEntity
import com.app.pro.mayurone.data.local.entity.UserEntity

@Database(
    entities = [
        TenantEntity::class,
        UserEntity::class,
        CustomerEntity::class,
        EstimateEntity::class,
        EstimateRevisionEntity::class,
        EstimateItemEntity::class,
        ShareEventEntity::class,
        SyncQueueEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class MayurOneDatabase : RoomDatabase() {

    abstract fun tenantDao(): TenantDao
    abstract fun customerDao(): CustomerDao
    abstract fun estimateDao(): EstimateDao
    abstract fun syncQueueDao(): SyncQueueDao

    companion object {
        @Volatile
        private var INSTANCE: MayurOneDatabase? = null

        fun getDatabase(context: Context): MayurOneDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MayurOneDatabase::class.java,
                    "mayurone_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
