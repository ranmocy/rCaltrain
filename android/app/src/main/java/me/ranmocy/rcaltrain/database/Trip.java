package me.ranmocy.rcaltrain.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(tableName = "trips", foreignKeys = {
        @ForeignKey(entity = Service.class, parentColumns = "id", childColumns = "service_id", onDelete = ForeignKey.CASCADE)
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
