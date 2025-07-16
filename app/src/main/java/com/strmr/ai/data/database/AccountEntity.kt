package com.strmr.ai.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val accountType: String, // "trakt", "premiumize", "realdebrid"
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: Long, // Timestamp when token expires
    val username: String? = null,
    val lastSyncTimestamp: Long = 0L
) 