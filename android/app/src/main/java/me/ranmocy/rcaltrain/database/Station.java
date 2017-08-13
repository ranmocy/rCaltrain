package me.ranmocy.rcaltrain.database;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

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
