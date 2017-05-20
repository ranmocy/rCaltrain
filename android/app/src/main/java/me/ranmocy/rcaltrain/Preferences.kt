package me.ranmocy.rcaltrain

import android.content.Context
import android.content.SharedPreferences

import me.ranmocy.rcaltrain.models.ScheduleType

/** Preferences manages [SharedPreferences]. */
class Preferences(context: Context) {
    companion object {
        private val PREFERENCES_NAMESPACE = "rCaltrainPreferences"
        private val LAST_DEPARTURE_STATION_NAME = "pref_last_source_station_name"
        private val LAST_DESTINATION_STATION_NAME = "pref_last_dest_station_name"
        private val LAST_SCHEDULE_TYPE = "pref_last_schedule_type"
    }

    private val preferences: SharedPreferences = context.getSharedPreferences(PREFERENCES_NAMESPACE, Context.MODE_PRIVATE)

    var lastDepartureStationName: String
        get() = preferences.getString(LAST_DEPARTURE_STATION_NAME, "")
        set(stationName) = preferences.edit().putString(LAST_DEPARTURE_STATION_NAME, stationName).apply()

    var lastDestinationStationName: String
        get() = preferences.getString(LAST_DESTINATION_STATION_NAME, "")
        set(stationName) = preferences.edit().putString(LAST_DESTINATION_STATION_NAME, stationName).apply()

    var lastScheduleType: ScheduleType
        get() {
            var type = preferences.getInt(LAST_SCHEDULE_TYPE, ScheduleType.NOW.ordinal)
            val types = ScheduleType.values()
            if (type < 0 || type >= types.size) {
                type = 0
            }
            return types[type]
        }
        set(type) = preferences.edit().putInt(LAST_SCHEDULE_TYPE, type.ordinal).apply()
}
