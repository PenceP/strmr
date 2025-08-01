package com.strmr.ai.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.strmr.ai.data.CollectionMovie
import com.strmr.ai.data.database.converters.ListConverter

@Entity(tableName = "collections")
@TypeConverters(ListConverter::class)
data class CollectionEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val overview: String?,
    val posterPath: String?,
    val backdropPath: String?,
    val parts: List<CollectionMovie> = emptyList(),
    val lastUpdated: Long = 0L, // for cache expiry
)
