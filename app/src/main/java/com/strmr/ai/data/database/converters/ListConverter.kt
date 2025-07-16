package com.strmr.ai.data.database.converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.strmr.ai.data.Actor

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
} 