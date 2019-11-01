package me.ranmocy.rcaltrain.database;

import java.util.Calendar;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "services")
public final class Service {

    public Service(
        @NonNull String id,
        boolean weekday,
        boolean saturday,
        boolean sunday,
        @NonNull Calendar startDate,
        @NonNull Calendar endDate) {
      this.id = id;
      this.weekday = weekday;
      this.saturday = saturday;
      this.sunday = sunday;
      this.startDate = startDate;
      this.endDate = endDate;
    }

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    public String id;

    @ColumnInfo(name = "weekday")
    public boolean weekday;

    @ColumnInfo(name = "saturday")
    public boolean saturday;

    @ColumnInfo(name = "sunday")
    public boolean sunday;

    @NonNull
    @ColumnInfo(name = "start_date")
    public Calendar startDate;

    @NonNull
    @ColumnInfo(name = "end_date")
    public Calendar endDate;
}
