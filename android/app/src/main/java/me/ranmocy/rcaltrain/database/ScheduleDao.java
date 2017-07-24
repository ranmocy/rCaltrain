package me.ranmocy.rcaltrain.database;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

@Dao
public interface ScheduleDao {
    @Query("SELECT * FROM services")
    List<Service> getAllServices();

    @Insert
    void insertAll(Service... services);
}
