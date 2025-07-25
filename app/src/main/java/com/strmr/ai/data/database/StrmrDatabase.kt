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
import com.strmr.ai.data.database.ContinueWatchingEntity
import com.strmr.ai.data.database.TraktUserProfileEntity
import com.strmr.ai.data.database.TraktUserStatsEntity
import com.strmr.ai.data.database.PlaybackDao
import com.strmr.ai.data.database.ContinueWatchingDao
import com.strmr.ai.data.database.TraktUserProfileDao
import com.strmr.ai.data.database.TraktUserStatsDao
import com.strmr.ai.data.database.OmdbRatingsEntity
import com.strmr.ai.data.database.OmdbRatingsDao
import com.strmr.ai.data.database.SeasonEntity
import com.strmr.ai.data.database.EpisodeEntity
import com.strmr.ai.data.database.SeasonDao
import com.strmr.ai.data.database.EpisodeDao
import com.strmr.ai.data.database.TraktRatingsEntity
import com.strmr.ai.data.database.TraktRatingsDao

@Database(
    entities = [
        MovieEntity::class,
        TvShowEntity::class,
        AccountEntity::class,
        PlaybackEntity::class,
        ContinueWatchingEntity::class,
        TraktUserProfileEntity::class,
        TraktUserStatsEntity::class,
        OmdbRatingsEntity::class,
        SeasonEntity::class,
        EpisodeEntity::class,
        CollectionEntity::class,
        TraktRatingsEntity::class,
        IntermediateViewEntity::class,
        IntermediateViewItemEntity::class
    ],
    version = 12, // bumped for intermediate view caching tables
    exportSchema = false
)
@TypeConverters(ListConverter::class)
abstract class StrmrDatabase : RoomDatabase() {

    abstract fun movieDao(): MovieDao
    abstract fun tvShowDao(): TvShowDao
    abstract fun accountDao(): AccountDao
    abstract fun playbackDao(): PlaybackDao
    abstract fun continueWatchingDao(): ContinueWatchingDao
    abstract fun traktUserProfileDao(): TraktUserProfileDao
    abstract fun traktUserStatsDao(): TraktUserStatsDao
    abstract fun omdbRatingsDao(): OmdbRatingsDao
    abstract fun seasonDao(): SeasonDao
    abstract fun episodeDao(): EpisodeDao
    abstract fun collectionDao(): CollectionDao
    abstract fun traktRatingsDao(): TraktRatingsDao
    abstract fun intermediateViewDao(): IntermediateViewDao

    companion object {
        @Volatile
        private var INSTANCE: StrmrDatabase? = null

        // Migration from version 11 to 12
        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create intermediate_views table
                database.execSQL("""
                    CREATE TABLE intermediate_views (
                        id TEXT PRIMARY KEY NOT NULL,
                        viewType TEXT NOT NULL,
                        itemId TEXT NOT NULL,
                        itemName TEXT NOT NULL,
                        itemBackgroundUrl TEXT,
                        dataUrl TEXT,
                        lastUpdated INTEGER NOT NULL,
                        totalItems INTEGER NOT NULL DEFAULT 0,
                        page INTEGER NOT NULL DEFAULT 1,
                        pageSize INTEGER NOT NULL DEFAULT 20
                    )
                """)
                
                // Create intermediate_view_items table
                database.execSQL("""
                    CREATE TABLE intermediate_view_items (
                        intermediateViewId TEXT NOT NULL,
                        mediaType TEXT NOT NULL,
                        tmdbId INTEGER NOT NULL,
                        orderIndex INTEGER NOT NULL,
                        addedAt INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY (intermediateViewId, mediaType, tmdbId)
                    )
                """)
                
                // Create indices for better performance
                database.execSQL("CREATE INDEX idx_intermediate_views_type ON intermediate_views(viewType)")
                database.execSQL("CREATE INDEX idx_intermediate_views_updated ON intermediate_views(lastUpdated)")
                database.execSQL("CREATE INDEX idx_intermediate_view_items_view ON intermediate_view_items(intermediateViewId)")
                database.execSQL("CREATE INDEX idx_intermediate_view_items_order ON intermediate_view_items(intermediateViewId, orderIndex)")
            }
        }

        // Migration from version 10 to 11
        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create continue_watching table
                database.execSQL("""
                    CREATE TABLE continue_watching (
                        id TEXT PRIMARY KEY NOT NULL,
                        type TEXT NOT NULL,
                        lastWatchedAt TEXT NOT NULL,
                        progress REAL,
                        movieTitle TEXT,
                        movieTmdbId INTEGER,
                        movieTraktId INTEGER,
                        movieYear INTEGER,
                        showTitle TEXT,
                        showTmdbId INTEGER,
                        showTraktId INTEGER,
                        showYear INTEGER,
                        episodeTitle TEXT,
                        episodeSeason INTEGER,
                        episodeNumber INTEGER,
                        episodeTmdbId INTEGER,
                        episodeTraktId INTEGER,
                        isNextEpisode INTEGER NOT NULL DEFAULT 0,
                        isInProgress INTEGER NOT NULL DEFAULT 0
                    )
                """)
            }
        }

        // Migration from version 9 to 10
        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add belongsToCollection column to movies table
                database.execSQL("ALTER TABLE movies ADD COLUMN belongsToCollection TEXT")
                
                // Create collections table
                database.execSQL("""
                    CREATE TABLE collections (
                        id INTEGER PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL,
                        overview TEXT,
                        posterPath TEXT,
                        backdropPath TEXT,
                        parts TEXT NOT NULL DEFAULT '[]',
                        lastUpdated INTEGER NOT NULL DEFAULT 0
                    )
                """)
            }
        }

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
                .addMigrations(MIGRATION_11_12, MIGRATION_10_11, MIGRATION_9_10, MIGRATION_8_9) // Add the new migrations
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
} 