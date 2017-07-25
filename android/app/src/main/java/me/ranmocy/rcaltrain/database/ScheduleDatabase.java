package me.ranmocy.rcaltrain.database;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.TypeConverters;
import android.content.Context;

import org.jetbrains.annotations.NotNull;

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
            @NotNull final List<Station> stations,
            @NotNull final List<Service> services,
            @NotNull final List<ServiceDate> serviceDates,
            @NotNull final List<Trip> trips,
            @NotNull final List<Stop> stops) {
        runInTransaction(new Runnable() {
            @Override
            public void run() {
                SupportSQLiteDatabase db = getOpenHelper().getWritableDatabase();
                db.beginTransaction();
                try {
                    db.delete("stations", null, null);
                    db.delete("services", null, null);
                    db.delete("service_dates", null, null);
                    db.delete("trips", null, null);
                    db.delete("stops", null, null);
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                scheduleDao().insert(stations, services, serviceDates, trips, stops);
            }
        });
    }
}
