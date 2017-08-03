package me.ranmocy.rcaltrain.models;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;

import kotlin.jvm.internal.Intrinsics;

public final class ScheduleResult implements Comparable<ScheduleResult> {

    public final DayTime departureTime;
    public final DayTime arrivalTime;

    public ScheduleResult(DayTime departureTime, DayTime arrivalTime) {
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
    }

    public final String getIntervalTimeString() {
        return String.format(Locale.getDefault(), "%d min", departureTime.toInMinutes(arrivalTime));
    }

    public int compareTo(@NotNull ScheduleResult other) {
        Intrinsics.checkParameterIsNotNull(other, "other");
        return this.departureTime.compareTo(other.departureTime);
    }

    @Override
    public String toString() {
        return String.format("%s => %s", departureTime, arrivalTime);
    }
}
