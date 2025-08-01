package com.strmr.ai.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TraktUserProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(profile: TraktUserProfileEntity)

    @Query("SELECT * FROM trakt_user_profile WHERE username = :username LIMIT 1")
    fun getUserProfile(username: String): Flow<TraktUserProfileEntity?>
}
