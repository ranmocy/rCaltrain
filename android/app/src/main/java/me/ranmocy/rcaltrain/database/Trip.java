package me.ranmocy.rcaltrain.database;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.PrimaryKey;

@Entity(tableName = "trips", foreignKeys = {
        @ForeignKey(entity = Service.class, parentColumns = "id", childColumns = "service_id")
})
public final class Trip {

    public Trip(String id, String serviceId) {
        this.id = id;
        this.serviceId = serviceId;
    }

    @PrimaryKey
    @ColumnInfo(name = "id")
    public String id;

    @ColumnInfo(name = "service_id", index = true)
    public String serviceId;
}
