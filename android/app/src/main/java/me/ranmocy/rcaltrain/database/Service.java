package me.ranmocy.rcaltrain.database;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

import java.util.Date;

@Entity(tableName = "services")
public final class Service {

    @PrimaryKey
    @ColumnInfo(name = "service_id")
    public String serviceId;

    @ColumnInfo(name = "weekday")
    public boolean weekday;

    @ColumnInfo(name = "saturday")
    public boolean saturday;

    @ColumnInfo(name = "sunday")
    public boolean sunday;

    @ColumnInfo(name = "start_date")
    public Date startDate;

    @ColumnInfo(name = "end_date")
    public Date endDate;
}
