package me.ranmocy.rcaltrain.database;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.TypeConverters;
import android.content.Context;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import org.jetbrains.annotations.NotNull;

import java.util.Calendar;
import java.util.List;

import me.ranmocy.rcaltrain.models.DayTime;
import me.ranmocy.rcaltrain.models.ScheduleResult;

@Database(version = 1, entities = {
        Service.class,
        ServiceDate.class,
        Station.class,
        Stop.class,
        Trip.class
})
@TypeConverters({Converters.class})
public abstract class ScheduleDatabase extends RoomDatabase {

    private static final String TAG = "ScheduleDatabase";
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

    public LiveData<List<ScheduleResult>> getResults(String from, String to, @ScheduleDao.ServiceType int serviceType) {
        Log.i(TAG, "query results");
        Calendar today = Calendar.getInstance();
        DayTime now = DayTime.Companion.now();
        Input input = getInput(serviceType, today, now);
        return scheduleDao().getResults(from, to, input.serviceType, today, input.now);
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    List<ScheduleResult> getResultsTesting(
            String from, String to, @ScheduleDao.ServiceType int serviceType, Calendar today, DayTime now) {
        Input input = getInput(serviceType, today, now);
        return scheduleDao().getResultsSync(from, to, input.serviceType, today, input.now);
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
                Log.i("DATABASE", "data updated");
            }
        });
    }

    private Input getInput(@ScheduleDao.ServiceType int serviceType, Calendar today, DayTime now) {
        if (serviceType == ScheduleDao.SERVICE_NOW) {
            int dayOfWeek = today.get(Calendar.DAY_OF_WEEK);
            switch (dayOfWeek) {
                case Calendar.MONDAY:
                case Calendar.TUESDAY:
                case Calendar.WEDNESDAY:
                case Calendar.THURSDAY:
                case Calendar.FRIDAY:
                    serviceType = ScheduleDao.SERVICE_WEEKDAY;
                    break;
                case Calendar.SATURDAY:
                    serviceType = ScheduleDao.SERVICE_SATURDAY;
                    break;
                case Calendar.SUNDAY:
                    serviceType = ScheduleDao.SERVICE_SUNDAY;
                    break;
                default:
                    throw new RuntimeException("Unexpected dayOfWeek:" + dayOfWeek);
            }
        } else {
            now = null;
        }
        return new Input(serviceType, now);
    }

    private class Input {
        private int serviceType;
        @Nullable
        private DayTime now;

        Input(int serviceType, @Nullable DayTime now) {
            this.serviceType = serviceType;
            this.now = now;
        }
    }
}
