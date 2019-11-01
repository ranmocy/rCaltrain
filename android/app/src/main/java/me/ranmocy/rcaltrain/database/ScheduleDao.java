package me.ranmocy.rcaltrain.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.annotation.IntDef;
import androidx.annotation.VisibleForTesting;

import java.util.Calendar;
import java.util.List;

import me.ranmocy.rcaltrain.models.DayTime;
import me.ranmocy.rcaltrain.models.ScheduleResult;

@Dao
public interface ScheduleDao {

    @IntDef({
            ServiceType.SERVICE_NOW,
            ServiceType.SERVICE_WEEKDAY,
            ServiceType.SERVICE_SATURDAY,
            ServiceType.SERVICE_SUNDAY
    })
    @interface ServiceType {
        int SERVICE_NOW = 0;
        int SERVICE_WEEKDAY = 1;
        int SERVICE_SATURDAY = 2;
        int SERVICE_SUNDAY = 3;
    }

    @Insert
    void insert(
            List<Station> stations, List<Service> services, List<ServiceDate> serviceDates,
            List<Trip> trips, List<Stop> stops);

    @Query("SELECT DISTINCT name from stations ORDER BY id")
    LiveData<List<String>> getStationNames();

    @Query(QueriesKt.QUERY)
    LiveData<List<ScheduleResult>> getResults(
            String from, String to, @ServiceType int serviceType, Calendar today, DayTime now);

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    @Query("SELECT DISTINCT name from stations ORDER BY id")
    List<String> getStationNamesSync();

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    @Query(QueriesKt.QUERY)
    List<ScheduleResult> getResultsSync(
            String from, String to, @ServiceType int serviceType, Calendar today, DayTime now);
}
