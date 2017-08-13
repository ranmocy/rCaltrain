package me.ranmocy.rcaltrain.database;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

import java.util.Calendar;

@Entity(tableName = "services")
public final class Service {

    public Service(String id, boolean weekday, boolean saturday, boolean sunday,
                   Calendar startDate, Calendar endDate) {
        this.id = id;
        this.weekday = weekday;
        this.saturday = saturday;
        this.sunday = sunday;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    @PrimaryKey
    @ColumnInfo(name = "id")
    public String id;

    @ColumnInfo(name = "weekday")
    public boolean weekday;

    @ColumnInfo(name = "saturday")
    public boolean saturday;

    @ColumnInfo(name = "sunday")
    public boolean sunday;

    @ColumnInfo(name = "start_date")
    public Calendar startDate;

    @ColumnInfo(name = "end_date")
    public Calendar endDate;
}
