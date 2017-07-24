package me.ranmocy.rcaltrain.database;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

@Entity(tableName = "stations")
public final class Station {

    @PrimaryKey
    @ColumnInfo(name = "station_id")
    public int stationId;

    @ColumnInfo(name = "station_name")
    public String stationName;
}
