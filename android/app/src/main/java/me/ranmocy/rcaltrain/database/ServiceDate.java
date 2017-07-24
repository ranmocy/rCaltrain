package me.ranmocy.rcaltrain.database;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.PrimaryKey;

import java.util.Date;

/**
 * Date exceptions for {@link Service}.
 */
@Entity(tableName = "service_dates", foreignKeys = {
        @ForeignKey(entity = Service.class, parentColumns = "service_id", childColumns = "service_id")
})
public final class ServiceDate {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    public int id;

    @ColumnInfo(name = "service_id", index = true)
    public String serviceId;

    @ColumnInfo(name = "date")
    public Date date;

    @ColumnInfo(name = "type")
    public int type;
}
