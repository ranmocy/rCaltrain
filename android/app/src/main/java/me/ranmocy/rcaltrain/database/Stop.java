package me.ranmocy.rcaltrain.database;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;
import me.ranmocy.rcaltrain.models.DayTime;

@Entity(
    tableName = "stops",
    foreignKeys = {
      @ForeignKey(
          entity = Trip.class,
          parentColumns = "id",
          childColumns = "trip_id",
          onDelete = ForeignKey.CASCADE),
      @ForeignKey(
          entity = Station.class,
          parentColumns = "id",
          childColumns = "station_id",
          onDelete = ForeignKey.CASCADE)
    })
public final class Stop {

  public Stop(@NonNull String tripId, int sequence, int stationId, @NonNull DayTime time) {
    this.tripId = tripId;
    this.sequence = sequence;
    this.stationId = stationId;
    this.time = time;
  }

  @PrimaryKey(autoGenerate = true)
  @ColumnInfo(name = "id")
  public int id;

  @NonNull
  @ColumnInfo(name = "trip_id", index = true)
  public String tripId;

  @ColumnInfo(name = "sequence")
  public int sequence;

  @ColumnInfo(name = "station_id", index = true)
  public int stationId;

  @NonNull
  @ColumnInfo(name = "time")
  public DayTime time;
}
