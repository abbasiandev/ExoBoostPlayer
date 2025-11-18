package dev.abbasian.exoboost.data.local.converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dev.abbasian.exoboost.data.local.HighlightSegmentEntity
import dev.abbasian.exoboost.data.local.VideoChapterEntity

class HighlightConverters {
    private val gson = Gson()

    @TypeConverter
    fun fromHighlightSegmentList(value: List<HighlightSegmentEntity>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toHighlightSegmentList(value: String): List<HighlightSegmentEntity> {
        val listType = object : TypeToken<List<HighlightSegmentEntity>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun fromVideoChapterList(value: List<VideoChapterEntity>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toVideoChapterList(value: String): List<VideoChapterEntity> {
        val listType = object : TypeToken<List<VideoChapterEntity>>() {}.type
        return gson.fromJson(value, listType)
    }
}