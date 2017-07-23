package me.ranmocy.rcaltrain.models

import java.util.*

/** Trip model. */
class Trip(val id: String, val service: Service) {

    inner class Stop internal constructor(val station: Station, val time: DayTime)

    private val stops = ArrayList<Stop>()

    fun addStop(station: Station, time: DayTime) {
        stops.add(Stop(station, time))
    }

    val stopList: List<Stop>
        get() = stops

    val stationList: List<Station>
        get() = stops.map { it.station }
}
