package me.ranmocy.rcaltrain.database;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "stations")
public final class Station {

    public Station(int id, @NonNull String name) {
        this.id = id;
        this.name = name;
    }

    @PrimaryKey
    @ColumnInfo(name = "id")
    public int id;

    @NonNull
    @ColumnInfo(name = "name")
    public String name;
}
