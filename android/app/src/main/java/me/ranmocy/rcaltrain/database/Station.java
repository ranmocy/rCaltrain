package me.ranmocy.rcaltrain.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "stations")
public final class Station {

    public Station(int id, String name) {
        this.id = id;
        this.name = name;
    }

    @PrimaryKey
    @ColumnInfo(name = "id")
    public int id;

    @ColumnInfo(name = "name")
    public String name;
}
