package me.ranmocy.rcaltrain.database;

import android.arch.persistence.room.TypeConverter;

import java.util.Date;

import me.ranmocy.rcaltrain.models.DayTime;

public final class Converters {
    @TypeConverter
    public static Date fromTimestamp(Long value) {
        return value == null ? null : new Date(value);
    }

    @TypeConverter
    public static Long dateToTimestamp(Date date) {
        return date == null ? null : date.getTime();
    }

    @TypeConverter
    public static DayTime fromSecondsOfDay(Long value) {
        return value == null ? null : new DayTime(value);
    }

    @TypeConverter
    public static Long dayTimeToSecondsOfDay(DayTime dayTime) {
        return dayTime == null ? null : dayTime.toSecondsSinceMidnight();
    }
}
