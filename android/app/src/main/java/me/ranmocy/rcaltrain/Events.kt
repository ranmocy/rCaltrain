package me.ranmocy.rcaltrain

import android.os.Bundle

import me.ranmocy.rcaltrain.models.ScheduleType

/** Events used for FirebaseAnalytics */
internal object Events {

    internal val EVENT_SCHEDULE = "event_schedule"
    internal val EVENT_ON_CLICK = "event_on_click"

    private val PARAM_DEPARTURE = "param_departure"
    private val PARAM_ARRIVAL = "param_arrival"
    private val PARAM_SCHEDULE = "param_schedule"
    private val PARAM_CLICKED_ELEM = "param_clicked_elem"

    private val VALUE_CLICK_DEPARTURE = "value_click_departure"
    private val VALUE_CLICK_ARRIVAL = "value_click_arrival"
    private val VALUE_CLICK_SWITCH = "value_click_switch"
    private val VALUE_CLICK_SCHEDULE = "value_click_schedule"

    fun getScheduleEvent(departure: String, arrival: String, schedule: ScheduleType): Bundle {
        val data = Bundle()
        data.putString(PARAM_DEPARTURE, departure)
        data.putString(PARAM_ARRIVAL, arrival)
        data.putString(PARAM_SCHEDULE, schedule.name)
        return data
    }

    val clickDepartureEvent: Bundle
        get() {
            val data = Bundle()
            data.putString(PARAM_CLICKED_ELEM, VALUE_CLICK_DEPARTURE)
            return data
        }

    val clickArrivalEvent: Bundle
        get() {
            val data = Bundle()
            data.putString(PARAM_CLICKED_ELEM, VALUE_CLICK_ARRIVAL)
            return data
        }

    val clickSwitchEvent: Bundle
        get() {
            val data = Bundle()
            data.putString(PARAM_CLICKED_ELEM, VALUE_CLICK_SWITCH)
            return data
        }

    fun getClickScheduleEvent(scheduleType: ScheduleType): Bundle {
        val data = Bundle()
        data.putString(PARAM_CLICKED_ELEM, VALUE_CLICK_SCHEDULE)
        data.putString(PARAM_SCHEDULE, scheduleType.name)
        return data
    }
}
