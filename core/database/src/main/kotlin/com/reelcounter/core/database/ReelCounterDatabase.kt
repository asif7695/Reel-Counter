package com.reelcounter.core.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import com.reelcounter.core.database.dao.DailyUsageDao
import com.reelcounter.core.database.dao.DayCoverageDao
import com.reelcounter.core.database.dao.IngestStateDao
import com.reelcounter.core.database.dao.PlatformSettingsDao
import com.reelcounter.core.database.dao.UsageSessionDao
import com.reelcounter.core.database.dao.ZoneChangeDao
import com.reelcounter.core.database.model.DailyUsageEntity
import com.reelcounter.core.database.model.DayCoverageEntity
import com.reelcounter.core.database.model.IngestStateEntity
import com.reelcounter.core.database.model.PlatformSettingsEntity
import com.reelcounter.core.database.model.UsageSessionEntity
import com.reelcounter.core.database.model.ZoneChangeEntity

@Database(
    entities = [
        UsageSessionEntity::class,
        DailyUsageEntity::class,
        DayCoverageEntity::class,
        PlatformSettingsEntity::class,
        ZoneChangeEntity::class,
        IngestStateEntity::class,
    ],
    // v2 (Phase 5): daily_usage.sync_error, ingest_state.last_revision; additive, so automatic.
    version = 2,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
    exportSchema = true,
)
abstract class ReelCounterDatabase : RoomDatabase() {
    abstract fun usageSessionDao(): UsageSessionDao
    abstract fun dailyUsageDao(): DailyUsageDao
    abstract fun dayCoverageDao(): DayCoverageDao
    abstract fun platformSettingsDao(): PlatformSettingsDao
    abstract fun zoneChangeDao(): ZoneChangeDao
    abstract fun ingestStateDao(): IngestStateDao
}
