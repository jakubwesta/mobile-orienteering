package com.mobileorienteering.data.local.converter

import androidx.room.TypeConverter
import com.mobileorienteering.data.model.ActivityStatus
import com.mobileorienteering.data.model.ControlPoint
import com.mobileorienteering.data.model.PathPoint
import com.mobileorienteering.data.model.VisitedControlPoint
import com.mobileorienteering.util.toInstant
import com.squareup.moshi.FromJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.time.Instant

class Converters {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .add(InstantAdapter())
        .build()

    @TypeConverter
    fun fromInstant(value: Instant?): Long? {
        return value?.toEpochMilli()
    }

    @TypeConverter
    fun toInstant(value: Long?): Instant? {
        return value?.let { Instant.ofEpochMilli(it) }
    }

    @TypeConverter
    fun fromPathPointList(value: List<PathPoint>?): String? {
        if (value == null) return null
        val type = Types.newParameterizedType(List::class.java, PathPoint::class.java)
        val adapter = moshi.adapter<List<PathPoint>>(type)
        return adapter.toJson(value)
    }

    @TypeConverter
    fun toPathPointList(value: String?): List<PathPoint>? {
        if (value == null) return null
        val type = Types.newParameterizedType(List::class.java, PathPoint::class.java)
        val adapter = moshi.adapter<List<PathPoint>>(type)
        return adapter.fromJson(value)
    }

    @TypeConverter
    fun fromControlPointList(value: List<ControlPoint>?): String? {
        if (value == null) return null
        val type = Types.newParameterizedType(List::class.java, ControlPoint::class.java)
        val adapter = moshi.adapter<List<ControlPoint>>(type)
        return adapter.toJson(value)
    }

    @TypeConverter
    fun toControlPointList(value: String?): List<ControlPoint>? {
        if (value == null) return null
        val type = Types.newParameterizedType(List::class.java, ControlPoint::class.java)
        val adapter = moshi.adapter<List<ControlPoint>>(type)
        return adapter.fromJson(value)
    }

    @TypeConverter
    fun fromVisitedCheckpointList(value: List<VisitedControlPoint>?): String? {
        if (value == null) return null
        val type = Types.newParameterizedType(List::class.java, VisitedControlPoint::class.java)
        val adapter = moshi.adapter<List<VisitedControlPoint>>(type)
        return adapter.toJson(value)
    }

    @TypeConverter
    fun toVisitedCheckpointList(value: String?): List<VisitedControlPoint>? {
        if (value == null) return null
        val type = Types.newParameterizedType(List::class.java, VisitedControlPoint::class.java)
        val adapter = moshi.adapter<List<VisitedControlPoint>>(type)
        return adapter.fromJson(value)
    }

    @TypeConverter
    fun fromActivityStatus(value: ActivityStatus?): String? {
        return value?.name
    }

    @TypeConverter
    fun toActivityStatus(value: String?): ActivityStatus? {
        return value?.let { ActivityStatus.valueOf(it) }
    }
}

class InstantAdapter {
    @ToJson
    fun toJson(instant: Instant): String {
        return instant.toString()
    }

    @FromJson
    fun fromJson(json: String): Instant {
        return json.toInstant()
    }
}