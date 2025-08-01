package com.strmr.ai.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface SeasonDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSeasons(seasons: List<SeasonEntity>)

    @Query("SELECT * FROM seasons WHERE showTmdbId = :showTmdbId ORDER BY seasonNumber ASC")
    suspend fun getSeasonsForShow(showTmdbId: Int): List<SeasonEntity>

    @Query("DELETE FROM seasons WHERE showTmdbId = :showTmdbId")
    suspend fun deleteSeasonsForShow(showTmdbId: Int)

    @Update
    suspend fun updateSeason(season: SeasonEntity)
}
