package me.ranmocy.rcaltrain.models;

import android.support.annotation.NonNull;

import java.util.Locale;

import me.ranmocy.rcaltrain.ui.ResultsListAdapter;

/**
 * Result object of scheduling.
 */
public class ScheduleResult implements Comparable<ScheduleResult> {
    private final DayTime departureTime;
    private final DayTime arrivalTime;
    private final long interval;

    public ScheduleResult(DayTime departureTime, DayTime arrivalTime) {
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.interval = departureTime.toInMinutes(arrivalTime);
    }

    public DayTime getDepartureTime() {
        return departureTime;
    }

    public String getDepartureTimeString() {
        return departureTime.toString();
    }

    public String getArrivalTimeString() {
        return arrivalTime.toString();
    }

    public String getIntervalTimeString() {
        return String.format(Locale.getDefault(), "%d min", interval);
    }

    @Override
    public int compareTo(@NonNull ScheduleResult another) {
        return this.departureTime.compareTo(another.departureTime);
    }
}
