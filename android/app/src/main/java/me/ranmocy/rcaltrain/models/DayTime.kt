package me.ranmocy.rcaltrain.models

import java.util.*

/** Represents the time since midnight. */
class DayTime : Comparable<DayTime> {

    private val minutesSinceMidnight: Long

    constructor(secondsSinceMidnight: Long) {
        this.minutesSinceMidnight = secondsSinceMidnight / 60
    }

    private constructor(hours: Int, minutes: Int) {
        this.minutesSinceMidnight = (hours * 60 + minutes).toLong()
    }

    override fun compareTo(other: DayTime): Int {
        return (this.minutesSinceMidnight - other.minutesSinceMidnight).toInt()
    }

    override fun toString(): String {
        return String.format(Locale.getDefault(), "%02d:%02d",
                minutesSinceMidnight / 60 % 24, minutesSinceMidnight % 60)
    }

    fun toSecondsSinceMidnight(): Long {
        return minutesSinceMidnight * 60
    }

    /**
     * Returns the interval time in minutes from this [DayTime] to the given [DayTime].
     */
    fun toInMinutes(another: DayTime): Long {
        return another.minutesSinceMidnight - this.minutesSinceMidnight
    }

    fun after(another: DayTime): Boolean {
        return this > another
    }

    companion object {

        fun now(): DayTime {
            val calendar = Calendar.getInstance()
            return DayTime(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE))
        }
    }
}
