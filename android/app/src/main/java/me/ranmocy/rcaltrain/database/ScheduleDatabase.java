package me.ranmocy.rcaltrain.database;

import android.content.Context;
import android.util.Log;

import org.jetbrains.annotations.NotNull;

import java.util.Calendar;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.LiveData;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.sqlite.db.SupportSQLiteDatabase;
import me.ranmocy.rcaltrain.database.ScheduleDao.ServiceType;
import me.ranmocy.rcaltrain.models.DayTime;
import me.ranmocy.rcaltrain.models.ScheduleResult;

@Database(
    version = 1,
    entities = {Service.class, ServiceDate.class, Station.class, Stop.class, Trip.class})
@TypeConverters({Converters.class})
public abstract class ScheduleDatabase extends RoomDatabase {

  private static final String TAG = "ScheduleDatabase";
  private static volatile ScheduleDatabase instance = null;

  public static ScheduleDatabase get(Context context) {
    if (instance == null) {
      synchronized (ScheduleDatabase.class) {
        if (instance == null) {
          instance =
              Room.databaseBuilder(
                      context.getApplicationContext(), ScheduleDatabase.class, "schedule")
                  .fallbackToDestructiveMigration()
                  .build();
        }
      }
    }
    return instance;
  }

  abstract ScheduleDao scheduleDao();

  public LiveData<List<String>> getStationNames() {
    return scheduleDao().getStationNames();
  }

  public LiveData<List<ScheduleResult>> getResults(
      String from, String to, @ServiceType int serviceType) {
    Log.i(TAG, "query results");
    Calendar today = Calendar.getInstance();
    DayTime now = DayTime.Companion.now();
    Input input = getInput(serviceType, today, now);
    return scheduleDao().getResults(from, to, input.serviceType, today, input.now);
  }

  @VisibleForTesting(otherwise = VisibleForTesting.NONE)
  List<String> getStationNamesTesting() {
    return scheduleDao().getStationNamesSync();
  }

  @VisibleForTesting(otherwise = VisibleForTesting.NONE)
  List<ScheduleResult> getResultsTesting(
      String from, String to, @ServiceType int serviceType, Calendar today, DayTime now) {
    Input input = getInput(serviceType, today, now);
    return scheduleDao().getResultsSync(from, to, input.serviceType, today, input.now);
  }

  public void updateData(
      @NotNull final List<Station> stations,
      @NotNull final List<Service> services,
      @NotNull final List<ServiceDate> serviceDates,
      @NotNull final List<Trip> trips,
      @NotNull final List<Stop> stops) {
    runInTransaction(
        new Runnable() {
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

  private Input getInput(@ServiceType int serviceType, Calendar today, DayTime now) {
    int dayOfWeek = today.get(Calendar.DAY_OF_WEEK);
    if (serviceType == ServiceType.SERVICE_NOW) {
      switch (dayOfWeek) {
        case Calendar.MONDAY:
        case Calendar.TUESDAY:
        case Calendar.WEDNESDAY:
        case Calendar.THURSDAY:
        case Calendar.FRIDAY:
          serviceType = ServiceType.SERVICE_WEEKDAY;
          break;
        case Calendar.SATURDAY:
          serviceType = ServiceType.SERVICE_SATURDAY;
          break;
        case Calendar.SUNDAY:
          serviceType = ServiceType.SERVICE_SUNDAY;
          break;
        default:
          throw new RuntimeException("Unexpected dayOfWeek:" + dayOfWeek);
      }
    } else {
      now = null;
      int targetDayOfWeek;
      switch (serviceType) {
        case ServiceType.SERVICE_SATURDAY:
          targetDayOfWeek = Calendar.SATURDAY;
          break;
        case ServiceType.SERVICE_SUNDAY:
          targetDayOfWeek = Calendar.SUNDAY;
          break;
        case ServiceType.SERVICE_WEEKDAY:
          targetDayOfWeek = Calendar.FRIDAY;
          break;
          //noinspection ConstantConditions
        case ServiceType.SERVICE_NOW:
        default:
          throw new RuntimeException("Unexpected dayOfWeek:" + dayOfWeek);
      }
      int diff = (targetDayOfWeek + 7 - dayOfWeek) % 7;
      today.add(Calendar.DATE, diff);
    }
    return new Input(serviceType, now);
  }

  private class Input {
    private int serviceType;
    @Nullable private DayTime now;

    Input(int serviceType, @Nullable DayTime now) {
      this.serviceType = serviceType;
      this.now = now;
    }
  }
}
