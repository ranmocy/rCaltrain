package me.ranmocy.rcaltrain.database;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.TypeConverters;
import android.content.Context;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

@Database(version = 1, entities = {
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

    abstract ScheduleDao scheduleDao();

    public LiveData<List<ScheduleDao.ScheduleResult>> getResults(
            String from, String to, Calendar now, @ScheduleDao.ServiceType int serviceType) {
        return scheduleDao().getResults(from, to, now, serviceType);
    }

    public void updateData(
            @NotNull List<Station> stations,
            @NotNull List<Service> services,
            @NotNull List<ServiceDate> serviceDates,
            @NotNull ArrayList<Trip> trips,
            @NotNull ArrayList<Stop> stops) {
        scheduleDao().insert(stations, services, serviceDates, trips, stops);
    }
}
