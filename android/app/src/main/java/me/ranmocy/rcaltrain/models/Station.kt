package me.ranmocy.rcaltrain.models

import java.util.*

/** Station model. */
class Station private constructor(val name: String) {
    companion object {

        private val NAME_MAP = HashMap<String, Station>()
        private val ID_MAP = HashMap<Int, Station>()
        private val ALL_STATIONS = ArrayList<Station>()

        fun addStation(name: String, ids: List<Int>): Station {
            val station = Station(name)
            NAME_MAP.put(name, station)
            for (id in ids) {
                ID_MAP.put(id, station)
            }
            ALL_STATIONS.add(station)
            return station
        }

        fun getStation(name: String): Station {
            return NAME_MAP[name]!!
        }

        fun getStation(id: Int): Station {
            return ID_MAP[id]!!
        }

        val allStations: List<Station>
            get() = ALL_STATIONS
    }
}
