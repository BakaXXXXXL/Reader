package com.reader.core.database.converter

import androidx.room.TypeConverter
import com.reader.core.model.BookFormat
import com.reader.core.model.PageTurnAnimation
import com.reader.core.model.ReaderThemePreset

/**
 * Room 类型转换器集合。
 * 处理枚举、复合结构与 SQLite 原生数据类型之间的映射。
 */
class Converters {

    @TypeConverter
    fun fromBookFormat(format: BookFormat?): String? {
        return format?.name
    }

    @TypeConverter
    fun toBookFormat(value: String?): BookFormat? {
        if (value.isNullOrBlank()) return null
        return try {
            BookFormat.valueOf(value)
        } catch (_: IllegalArgumentException) {
            BookFormat.entries.firstOrNull { it.extension.equals(value, ignoreCase = true) }
        }
    }

    @TypeConverter
    fun fromPageTurnAnimation(animation: PageTurnAnimation?): String? {
        return animation?.name
    }

    @TypeConverter
    fun toPageTurnAnimation(value: String?): PageTurnAnimation? {
        if (value.isNullOrBlank()) return null
        return try {
            PageTurnAnimation.valueOf(value)
        } catch (_: IllegalArgumentException) {
            PageTurnAnimation.fromId(value)
        }
    }

    @TypeConverter
    fun fromReaderThemePreset(theme: ReaderThemePreset?): String? {
        return theme?.name
    }

    @TypeConverter
    fun toReaderThemePreset(value: String?): ReaderThemePreset? {
        if (value.isNullOrBlank()) return null
        return try {
            ReaderThemePreset.valueOf(value)
        } catch (_: IllegalArgumentException) {
            ReaderThemePreset.fromId(value)
        }
    }
}
