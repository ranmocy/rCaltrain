package me.ranmocy.rcaltrain.database;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.TypeConverters;
import android.content.Context;

@Database(version = 1, exportSchema = true, entities = {
        Service.class,
        ServiceDate.class,
        Station.class,
        Stop.class,
        Trip.class
})
@TypeConverters({Converters.class})
public abstract class ScheduleDatabase extends RoomDatabase {

    private static ScheduleDatabase instance = null;

    public static ScheduleDatabase get(Context context) {
        if (instance == null) {
            synchronized (ScheduleDatabase.class) {
                if (instance == null) {
                    instance = Room
                            .databaseBuilder(context.getApplicationContext(), ScheduleDatabase.class, "schedule")
                            .build();
                }
            }
        }
        return instance;
    }

    public abstract ScheduleDao scheduleDao();
}
