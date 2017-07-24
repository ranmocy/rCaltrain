package me.ranmocy.rcaltrain.database;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.PrimaryKey;

import me.ranmocy.rcaltrain.models.DayTime;

@Entity(tableName = "stops", foreignKeys = {
        @ForeignKey(entity = Trip.class, parentColumns = "trip_id", childColumns = "trip_id"),
        @ForeignKey(entity = Station.class, parentColumns = "station_id", childColumns = "station_id")
})
public final class Stop {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    public int id;

    @ColumnInfo(name = "trip_id", index = true)
    public String tripId;

    @ColumnInfo(name = "index")
    public int index;

    @ColumnInfo(name = "station_id", index = true)
    public int stationId;

    @ColumnInfo(name = "time")
    public DayTime time;
}
