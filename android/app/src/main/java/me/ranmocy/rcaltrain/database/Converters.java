package me.ranmocy.rcaltrain.database;

import java.util.Calendar;

import androidx.room.TypeConverter;
import me.ranmocy.rcaltrain.models.DayTime;

final class Converters {
  @TypeConverter
  public static Calendar getCalendar(Long value) {
    if (value == null) {
      return null;
    }
    int year = (int) (value / 10000);
    int month = (int) (value / 100 % 100 - 1); // month is 0-based
    int day = (int) (value % 100);
    Calendar calendar = Calendar.getInstance();
    calendar.clear();
    calendar.set(year, month, day);
    return calendar;
  }

  @TypeConverter
  public static Long fromCalendar(Calendar date) {
    if (date == null) return null;
    return date.get(Calendar.YEAR) * 10000L
        + (date.get(Calendar.MONTH) + 1) * 100
        + date.get(Calendar.DAY_OF_MONTH);
  }

  @TypeConverter
  public static DayTime toDayTime(Long value) {
    return value == null ? null : new DayTime(value);
  }

  @TypeConverter
  public static Long fromDayTime(DayTime dayTime) {
    return dayTime == null ? null : dayTime.toSecondsSinceMidnight();
  }
}
