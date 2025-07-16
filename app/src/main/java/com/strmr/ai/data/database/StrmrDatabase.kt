package com.strmr.ai.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.strmr.ai.data.database.converters.ListConverter
import com.strmr.ai.data.database.PlaybackEntity
import com.strmr.ai.data.database.TraktUserProfileEntity
import com.strmr.ai.data.database.TraktUserStatsEntity
import com.strmr.ai.data.database.PlaybackDao
import com.strmr.ai.data.database.TraktUserProfileDao
import com.strmr.ai.data.database.TraktUserStatsDao
import com.strmr.ai.data.database.OmdbRatingsEntity
import com.strmr.ai.data.database.OmdbRatingsDao
import com.strmr.ai.data.database.SeasonEntity
import com.strmr.ai.data.database.EpisodeEntity
import com.strmr.ai.data.database.SeasonDao
import com.strmr.ai.data.database.EpisodeDao

@Database(
    entities = [
        MovieEntity::class,
        TvShowEntity::class,
        AccountEntity::class,
        PlaybackEntity::class,
        TraktUserProfileEntity::class,
        TraktUserStatsEntity::class,
        OmdbRatingsEntity::class,
        SeasonEntity::class,
        EpisodeEntity::class
    ],
    version = 9, // bumped for schema changes - added similar field
    exportSchema = false
)
@TypeConverters(ListConverter::class)
abstract class StrmrDatabase : RoomDatabase() {

    abstract fun movieDao(): MovieDao
    abstract fun tvShowDao(): TvShowDao
    abstract fun accountDao(): AccountDao
    abstract fun playbackDao(): PlaybackDao
    abstract fun traktUserProfileDao(): TraktUserProfileDao
    abstract fun traktUserStatsDao(): TraktUserStatsDao
    abstract fun omdbRatingsDao(): OmdbRatingsDao
    abstract fun seasonDao(): SeasonDao
    abstract fun episodeDao(): EpisodeDao

    companion object {
        @Volatile
        private var INSTANCE: StrmrDatabase? = null

        // Migration from version 8 to 9
        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add similar column to movies table with default empty list
                database.execSQL("ALTER TABLE movies ADD COLUMN similar TEXT NOT NULL DEFAULT '[]'")
                
                // Add similar column to tv_shows table with default empty list
                database.execSQL("ALTER TABLE tv_shows ADD COLUMN similar TEXT NOT NULL DEFAULT '[]'")
            }
        }

        // Migration from version 3 to 4
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add trendingOrder column to movies table
                database.execSQL("ALTER TABLE movies ADD COLUMN trendingOrder INTEGER")
                
                // Add popularOrder column to movies table
                database.execSQL("ALTER TABLE movies ADD COLUMN popularOrder INTEGER")
                
                // Add trendingOrder column to tv_shows table
                database.execSQL("ALTER TABLE tv_shows ADD COLUMN trendingOrder INTEGER")
            }
        }
        
        // Migration from version 2 to 3
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add popularOrder column to tv_shows table
                database.execSQL("ALTER TABLE tv_shows ADD COLUMN popularOrder INTEGER")
            }
        }

        fun getDatabase(context: Context): StrmrDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StrmrDatabase::class.java,
                    "strmr_database"
                )
                .addMigrations(MIGRATION_8_9) // Add the new migration
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
} 