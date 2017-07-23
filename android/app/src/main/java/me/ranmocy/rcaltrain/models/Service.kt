package me.ranmocy.rcaltrain.models

import java.util.*

/** Service model. */
class Service private constructor(private val weekday: Boolean, private val saturday: Boolean, private val sunday: Boolean, private val startDate: Calendar, private val endDate: Calendar) {

    private enum class ExceptionType {
        ADD,
        REMOVE
    }

    private inner class ExceptionDate internal constructor(val date: Calendar, val type: ExceptionType)

    private val exceptionDates = ArrayList<ExceptionDate>()
    internal val trips = ArrayList<Trip>()

    fun addAdditionalDate(date: Calendar) {
        exceptionDates.add(ExceptionDate(date, ExceptionType.ADD))
    }

    fun addExceptionDate(date: Calendar) {
        exceptionDates.add(ExceptionDate(date, ExceptionType.REMOVE))
    }

    fun addTrip(id: String): Trip {
        val trip = Trip(id, this)
        trips.add(trip)
        return trip
    }

    fun isInServiceOn(current: Calendar): Boolean {
        // (startDate <= current <= endDate
        // && none(exceptionDate == current, exceptionType == REMOVE)
        // || any(exceptionDate == current, exceptionType == ADD)
        exceptionDates
                .filter { it.date == current }
                .forEach {
                    when (it.type) {
                        Service.ExceptionType.ADD -> return true
                        Service.ExceptionType.REMOVE -> return false
                        else -> throw RuntimeException("Unexpected exception type!")
                    }
                }
        return !startDate.after(current) && !endDate.before(current)
    }

    private fun isUnderScheduleType(scheduleType: ScheduleType): Boolean {
        when (scheduleType) {
            ScheduleType.NOW -> {
                val current = Calendar.getInstance()
                val dayOfWeek = current.get(Calendar.DAY_OF_WEEK)
                when (dayOfWeek) {
                    Calendar.SUNDAY -> return sunday
                    Calendar.SATURDAY -> return saturday
                    Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY -> return weekday
                    else -> throw RuntimeException("Unexpected dayOfWeek:" + dayOfWeek)
                }
            }
            ScheduleType.WEEKDAY -> return weekday
            ScheduleType.SATURDAY -> return saturday
            ScheduleType.SUNDAY -> return sunday
            else -> throw RuntimeException("Unexpected schedule type:" + scheduleType)
        }
    }

    companion object {

        private val SERVICE_MAP = HashMap<String, Service>()

        fun addService(serviceId: String, weekday: Boolean, saturday: Boolean, sunday: Boolean, startDate: Calendar, endDate: Calendar): Service {
            val service = Service(weekday, saturday, sunday, startDate, endDate)
            SERVICE_MAP.put(serviceId, service)
            return service
        }

        fun getService(serviceId: String): Service {
            return SERVICE_MAP[serviceId]!!
        }

        fun getAllValidServices(scheduleType: ScheduleType): List<Service> {
            val current = Calendar.getInstance()
            val validServices = SERVICE_MAP.values.filter {
                it.isInServiceOn(current) && it.isUnderScheduleType(scheduleType)
            }
            return validServices
        }
    }
}
