package me.ranmocy.rcaltrain.models

import java.util.*

/** Result object of scheduling. */
class ScheduleResult(val departureTime: DayTime, private val arrivalTime: DayTime) : Comparable<ScheduleResult> {
    private val interval: Long = departureTime.toInMinutes(arrivalTime)

    val departureTimeString: String
        get() = departureTime.toString()

    val arrivalTimeString: String
        get() = arrivalTime.toString()

    val intervalTimeString: String
        get() = String.format(Locale.getDefault(), "%d min", interval)

    override fun compareTo(other: ScheduleResult): Int {
        return this.departureTime.compareTo(other.departureTime)
    }
}
