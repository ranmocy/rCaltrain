package me.ranmocy.rcaltrain.database;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Query;
import android.support.annotation.IntDef;

import java.util.Date;
import java.util.List;

import me.ranmocy.rcaltrain.models.DayTime;

@Dao
public interface ScheduleDao {

    int SERVICE_WEEKDAY = 1;
    int SERVICE_SATURDAY = 2;
    int SERVICE_SUNDAY = 3;

    @IntDef({
            SERVICE_WEEKDAY,
            SERVICE_SATURDAY,
            SERVICE_SUNDAY
    })
    @interface ServiceType {
    }

    @Query(QueriesKt.QUERY)
    LiveData<List<ScheduleResult>> getResults(String from, String to, Date now, @ServiceType int serviceType);

    final class ScheduleResult {
        DayTime departureTime;
        DayTime arrivalTime;
    }
}
