package com.strmr.ai.data.database

import androidx.room.*

@Dao
interface CollectionDao {
    @Query("SELECT * FROM collections WHERE id = :collectionId")
    suspend fun getCollectionById(collectionId: Int): CollectionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollection(collection: CollectionEntity)

    @Query("DELETE FROM collections WHERE id = :collectionId")
    suspend fun deleteCollection(collectionId: Int)

    @Query("DELETE FROM collections WHERE lastUpdated < :timestamp")
    suspend fun deleteOldCollections(timestamp: Long)
}
