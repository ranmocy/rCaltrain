package me.ranmocy.rcaltrain.database;

import java.util.Calendar;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

/** Date exceptions for {@link Service}. */
@Entity(
    tableName = "service_dates",
    foreignKeys = {
      @ForeignKey(
          entity = Service.class,
          parentColumns = "id",
          childColumns = "service_id",
          onDelete = ForeignKey.CASCADE)
    })
public final class ServiceDate {

  public ServiceDate(@NonNull String serviceId, @NonNull Calendar date, int type) {
    this.serviceId = serviceId;
    this.date = date;
    this.type = type;
  }

  @PrimaryKey(autoGenerate = true)
  @ColumnInfo(name = "id")
  public int id;

  @NonNull
  @ColumnInfo(name = "service_id", index = true)
  public String serviceId;

  @NonNull
  @ColumnInfo(name = "date")
  public Calendar date;

  @ColumnInfo(name = "type")
  public int type;
}
