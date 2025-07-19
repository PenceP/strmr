package com.strmr.ai.data.database.converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.strmr.ai.data.Actor
import com.strmr.ai.data.SimilarContent
import com.strmr.ai.data.BelongsToCollection
import com.strmr.ai.data.CollectionMovie

class ListConverter {
    @TypeConverter
    fun fromString(value: String?): List<String>? {
        if (value == null) {
            return null
        }
        val listType = object : TypeToken<List<String>>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromList(list: List<String>?): String? {
        if (list == null) {
            return null
        }
        val gson = Gson()
        return gson.toJson(list)
    }

    @TypeConverter
    fun fromActorList(list: List<Actor>?): String? {
        if (list == null) return null
        return Gson().toJson(list)
    }

    @TypeConverter
    fun toActorList(value: String?): List<Actor>? {
        if (value == null) return null
        val listType = object : TypeToken<List<Actor>>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromSimilarContentList(list: List<SimilarContent>?): String? {
        if (list == null) return null
        return Gson().toJson(list)
    }

    @TypeConverter
    fun toSimilarContentList(value: String?): List<SimilarContent>? {
        if (value == null) return null
        val listType = object : TypeToken<List<SimilarContent>>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromBelongsToCollection(collection: BelongsToCollection?): String? {
        if (collection == null) return null
        return Gson().toJson(collection)
    }

    @TypeConverter
    fun toBelongsToCollection(value: String?): BelongsToCollection? {
        if (value == null) return null
        return Gson().fromJson(value, BelongsToCollection::class.java)
    }

    @TypeConverter
    fun fromCollectionMovieList(list: List<CollectionMovie>?): String? {
        if (list == null) return null
        return Gson().toJson(list)
    }

    @TypeConverter
    fun toCollectionMovieList(value: String?): List<CollectionMovie>? {
        if (value == null) return null
        val listType = object : TypeToken<List<CollectionMovie>>() {}.type
        return Gson().fromJson(value, listType)
    }
    
    @TypeConverter
    fun fromStringIntMap(map: Map<String, Int?>?): String? {
        if (map == null) return null
        return Gson().toJson(map)
    }

    @TypeConverter
    fun toStringIntMap(value: String?): Map<String, Int?>? {
        if (value == null) return null
        val mapType = object : TypeToken<Map<String, Int?>>() {}.type
        return Gson().fromJson(value, mapType)
    }
} 