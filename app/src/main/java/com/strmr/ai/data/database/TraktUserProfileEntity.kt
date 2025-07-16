package com.strmr.ai.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

@Entity(tableName = "trakt_user_profile")
data class TraktUserProfileEntity(
    @PrimaryKey val username: String,
    val private: Boolean,
    val name: String?,
    val vip: Boolean,
    val vipEp: Boolean,
    val slug: String,
    val uuid: String
) 