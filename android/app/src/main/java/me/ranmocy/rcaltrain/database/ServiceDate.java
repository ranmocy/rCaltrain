package me.ranmocy.rcaltrain.database;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.PrimaryKey;

import java.util.Calendar;

/**
 * Date exceptions for {@link Service}.
 */
@Entity(tableName = "service_dates", foreignKeys = {
        @ForeignKey(entity = Service.class, parentColumns = "id", childColumns = "service_id", onDelete = ForeignKey.CASCADE)
})
public final class ServiceDate {

    public ServiceDate(String serviceId, Calendar date, int type) {
        this.serviceId = serviceId;
        this.date = date;
        this.type = type;
    }

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    public int id;

    @ColumnInfo(name = "service_id", index = true)
    public String serviceId;

    @ColumnInfo(name = "date")
    public Calendar date;

    @ColumnInfo(name = "type")
    public int type;
}
