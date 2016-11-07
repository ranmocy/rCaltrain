package me.ranmocy.rcaltrain;

import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.ranmocy.rcaltrain.models.DayTime;
import me.ranmocy.rcaltrain.models.ScheduleResult;
import me.ranmocy.rcaltrain.models.ScheduleType;
import me.ranmocy.rcaltrain.models.Service;
import me.ranmocy.rcaltrain.models.Station;
import me.ranmocy.rcaltrain.models.Trip;

/**
 * Scheduler calculates scheduling results.
 */
public final class Scheduler {

    private static final String TAG = "Scheduler";

    public static List<ScheduleResult> schedule(
            String fromName, String toName, ScheduleType scheduleType) {
        Log.i(TAG, String.format("from:%s, to:%s, type:%s", fromName, toName, scheduleType));

        List<ScheduleResult> resultList = new ArrayList<>();

        // check service time
        List<Trip> possibleTrips = new ArrayList<>();
        for (Service service : Service.getAllValidServices(scheduleType)) {
            possibleTrips.addAll(service.getTrips());
        }

        // check station
        Station departureStation = Station.getStation(fromName);
        Station arrivalStation = Station.getStation(toName);
        for (Trip trip : possibleTrips) {
            List<Station> stationList = trip.getStationList();
            int departureIndex = stationList.indexOf(departureStation);
            int arrivalIndex = stationList.indexOf(arrivalStation);

            if (departureIndex >= 0 && arrivalIndex >= 0 && departureIndex < arrivalIndex) {
                List<Trip.Stop> stopList = trip.getStopList();
                DayTime departureTime = stopList.get(departureIndex).getTime();
                DayTime arrivalTime = stopList.get(arrivalIndex).getTime();

                // check current time
                if (scheduleType == ScheduleType.NOW && DayTime.now().after(departureTime)) {
                    continue;
                }
                resultList.add(new ScheduleResult(departureTime, arrivalTime));
            }
        }

        Collections.sort(resultList);

        return resultList;
    }
}
