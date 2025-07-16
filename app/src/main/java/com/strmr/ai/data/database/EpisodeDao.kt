package com.strmr.ai.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface EpisodeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEpisodes(episodes: List<EpisodeEntity>)

    @Query("SELECT * FROM episodes WHERE showTmdbId = :showTmdbId AND seasonNumber = :seasonNumber ORDER BY episodeNumber ASC")
    suspend fun getEpisodesForSeason(showTmdbId: Int, seasonNumber: Int): List<EpisodeEntity>

    @Query("SELECT * FROM episodes WHERE showTmdbId = :showTmdbId AND seasonNumber = :seasonNumber AND episodeNumber = :episodeNumber LIMIT 1")
    suspend fun getEpisodeByDetails(showTmdbId: Int, seasonNumber: Int, episodeNumber: Int): EpisodeEntity?

    @Query("DELETE FROM episodes WHERE showTmdbId = :showTmdbId AND seasonNumber = :seasonNumber")
    suspend fun deleteEpisodesForSeason(showTmdbId: Int, seasonNumber: Int)

    @Update
    suspend fun updateEpisode(episode: EpisodeEntity)
} 