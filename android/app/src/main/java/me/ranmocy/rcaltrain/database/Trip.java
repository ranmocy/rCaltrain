package me.ranmocy.rcaltrain.database;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.PrimaryKey;

@Entity(tableName = "trips", foreignKeys = {
        @ForeignKey(entity = Service.class, parentColumns = "service_id", childColumns = "service_id")
})
public final class Trip {

    @PrimaryKey
    @ColumnInfo(name = "trip_id")
    public String tripId;

    @ColumnInfo(name = "service_id", index = true)
    public String serviceId;
}
