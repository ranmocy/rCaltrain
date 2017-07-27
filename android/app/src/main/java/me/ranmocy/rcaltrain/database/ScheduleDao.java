package me.ranmocy.rcaltrain.database;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.support.annotation.IntDef;
import android.support.annotation.VisibleForTesting;

import java.util.Calendar;
import java.util.List;

import me.ranmocy.rcaltrain.models.DayTime;
import me.ranmocy.rcaltrain.models.ScheduleResult;

@Dao
public interface ScheduleDao {

    int SERVICE_NOW = 0;
    int SERVICE_WEEKDAY = 1;
    int SERVICE_SATURDAY = 2;
    int SERVICE_SUNDAY = 3;

    @IntDef({SERVICE_NOW, SERVICE_WEEKDAY, SERVICE_SATURDAY, SERVICE_SUNDAY})
    @interface ServiceType {
    }

    @Query(QueriesKt.QUERY)
    List<ScheduleResult> getResults(
            String from, String to, @ServiceType int serviceType, Calendar today, DayTime now);

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    @Query(QueriesKt.QUERY)
    List<ScheduleResult> getResultsSync(
            String from, String to, @ServiceType int serviceType, Calendar today, DayTime now);

    @Insert
    void insert(
            List<Station> stations, List<Service> services, List<ServiceDate> serviceDates,
            List<Trip> trips, List<Stop> stops);
}
