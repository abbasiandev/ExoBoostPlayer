package dev.abbasian.exoboost.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import dev.abbasian.exoboost.data.local.VideoHighlightEntity
import dev.abbasian.exoboost.data.local.converter.HighlightConverters
import dev.abbasian.exoboost.data.local.dao.VideoHighlightDao

@Database(
    entities = [VideoHighlightEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(HighlightConverters::class)
abstract class ExoBoostDatabase : RoomDatabase() {
    abstract fun videoHighlightDao(): VideoHighlightDao

    companion object {
        const val DATABASE_NAME = "exoboost_database"
    }
}
