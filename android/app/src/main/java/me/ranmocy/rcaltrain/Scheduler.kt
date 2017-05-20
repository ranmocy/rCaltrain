package me.ranmocy.rcaltrain

import android.util.Log
import me.ranmocy.rcaltrain.models.*
import java.util.*

/** Scheduler calculates scheduling results. */
internal object Scheduler {
    private const val TAG = "Scheduler"

    fun schedule(
            fromName: String, toName: String, scheduleType: ScheduleType): List<ScheduleResult> {
        Log.i(TAG, String.format("from:%s, to:%s, type:%s", fromName, toName, scheduleType))

        val resultList = ArrayList<ScheduleResult>()

        // check service time
        val possibleTrips = ArrayList<Trip>()
        for (service in Service.getAllValidServices(scheduleType)) {
            possibleTrips.addAll(service.trips)
        }

        // check station
        val departureStation = Station.getStation(fromName)
        val arrivalStation = Station.getStation(toName)
        for (trip in possibleTrips) {
            val stationList = trip.stationList
            val departureIndex = stationList.indexOf(departureStation)
            val arrivalIndex = stationList.indexOf(arrivalStation)

            if (departureIndex >= 0 && arrivalIndex >= 0 && departureIndex < arrivalIndex) {
                val stopList = trip.stopList
                val departureTime = stopList[departureIndex].time
                val arrivalTime = stopList[arrivalIndex].time

                // check current time
                if (scheduleType == ScheduleType.NOW && DayTime.now().after(departureTime)) {
                    continue
                }
                resultList.add(ScheduleResult(departureTime, arrivalTime))
            }
        }

        Collections.sort(resultList)

        return resultList
    }
}
