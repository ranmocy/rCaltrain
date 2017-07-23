package me.ranmocy.rcaltrain

import android.content.Context
import android.content.res.XmlResourceParser
import android.support.annotation.XmlRes
import android.util.Log
import me.ranmocy.rcaltrain.models.DayTime
import me.ranmocy.rcaltrain.models.Service
import me.ranmocy.rcaltrain.models.Station
import org.xmlpull.v1.XmlPullParser.*
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.util.*

/** Loader loads scheduling data from xml files. */
class DataLoader private constructor(context: Context, @XmlRes resId: Int) {

    private val parser: XmlResourceParser = context.resources.getXml(resId)

    /**
     * stops:
     * stop_name => [stop_id1, stop_id2]
     * "San Francisco" => [70021, 70022]
     */
    @Throws(XmlPullParserException::class, IOException::class)
    private fun loadStops() {
        startDoc()
        startTag(MAP)
        while (isTag(KEY)) {
            val stationName = key

            startTag(VALUE)
            startTag(ARRAY)
            val stationIds = ArrayList<Int>()
            while (isTag(ELEM)) {
                stationIds.add(getInt(ELEM))
            }
            endTag(ARRAY)
            endTag(VALUE)

            Station.addStation(stationName, stationIds)
        }
        endTag(MAP)
        endDoc()
    }

    /**
     * calendar:
     * service_id => {weekday: bool, saturday: bool, sunday: bool, start_date: date, end_date: date}
     * CT-16APR-Caltrain-Weekday-01 => {weekday: false, saturday: true, sunday: false, start_date: 20160404, end_date: 20190331}
     */
    @Throws(IOException::class, XmlPullParserException::class)
    private fun loadCalendar() {
        startDoc()
        startTag(MAP)
        while (isTag(KEY)) {
            val serviceId = key

            startTag(VALUE)
            startTag(MAP)
            if ("weekday" != key) throw AssertionError()
            val weekday = getBoolean(VALUE)
            if ("saturday" != key) throw AssertionError()
            val saturday = getBoolean(VALUE)
            if ("sunday" != key) throw AssertionError()
            val sunday = getBoolean(VALUE)
            if ("start_date" != key) throw AssertionError()
            val startDate = getDate(VALUE)
            if ("end_date" != key) throw AssertionError()
            val endDate = getDate(VALUE)
            endTag(MAP)
            endTag(VALUE)

            Service.addService(serviceId, weekday, saturday, sunday, startDate, endDate)
        }
        endTag(MAP)
        endDoc()
    }

    /**
     * calendar_dates:
     * service_id => [[date, exception_type]]
     * CT-16APR-Caltrain-Weekday-01 => [[20160530,2]]
     */
    @Throws(IOException::class, XmlPullParserException::class)
    private fun loadCalendarDates() {
        startDoc()
        startTag(MAP)
        while (isTag(KEY)) {
            val serviceId = key
            val service = Service.getService(serviceId)

            startTag(VALUE)
            startTag(ARRAY)
            while (isTag(ELEM)) {
                startTag(ELEM)
                startTag(ARRAY)
                val date = getDate(ELEM)
                val type = getInt(ELEM)
                if (type == 1) {
                    service.addAdditionalDate(date)
                } else if (type == 2) {
                    service.addExceptionDate(date)
                } else {
                    throw RuntimeException("Unexpected exception dates type:" + type)
                }
                endTag(ARRAY)
                endTag(ELEM)
            }
            endTag(ARRAY)
            endTag(VALUE)
        }
        endTag(MAP)
        endDoc()
    }

    /**
     * routes:
     * { route_id => { service_id => { trip_id => [[stop_id, arrival_time/departure_time(in seconds)]] } } }
     * { "Bullet" => { "CT-14OCT-XXX" => { "650770-CT-14OCT-XXX" => [[70012, 29700], ...] } } }
     */
    @Throws(IOException::class, XmlPullParserException::class)
    private fun loadRoutes() {
        startDoc()
        startTag(MAP)
        while (isTag(KEY)) {
            key // routeId

            startTag(VALUE)
            startTag(MAP)
            while (isTag(KEY)) {
                val serviceId = key
                val service = Service.getService(serviceId)

                startTag(VALUE)
                startTag(MAP)
                while (isTag(KEY)) {
                    val tripId = key
                    val trip = service.addTrip(tripId)

                    startTag(VALUE)
                    startTag(ARRAY)
                    while (isTag(ELEM)) {
                        startTag(ELEM)
                        startTag(ARRAY)
                        val stationId = getInt(ELEM)
                        val station = Station.getStation(stationId)
                        val stopTime = getTime(ELEM)
                        trip.addStop(station, stopTime)
                        endTag(ARRAY)
                        endTag(ELEM)
                    }
                    endTag(ARRAY)
                    endTag(VALUE)
                }
                endTag(MAP)
                endTag(VALUE)
            }
            endTag(MAP)
            endTag(VALUE)
        }
        endTag(MAP)
        endDoc()
    }

    @Throws(XmlPullParserException::class)
    private fun isTag(tagName: String): Boolean {
        return parser.eventType == START_TAG && parser.name == tagName
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun startDoc() {
        parser.require(START_DOCUMENT, null, null)
        parser.next()
        parser.next()
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun endDoc() {
        parser.require(END_DOCUMENT, null, null)
        parser.next()
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun startTag(tagName: String) {
        parser.require(START_TAG, null, tagName)
        parser.next()
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun endTag(tagName: String) {
        parser.require(END_TAG, null, tagName)
        parser.next()
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun getText(tagName: String): String {
        startTag(tagName)
        parser.require(TEXT, null, null)
        val result = parser.text.trim { it <= ' ' }
        parser.next()
        endTag(tagName)
        return result
    }

    private val key: String
        @Throws(IOException::class, XmlPullParserException::class)
        get() = getText(KEY)

    @Throws(IOException::class, XmlPullParserException::class)
    private fun getInt(tagName: String): Int {
        return Integer.parseInt(getText(tagName))
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun getBoolean(tagName: String): Boolean {
        return java.lang.Boolean.parseBoolean(getText(tagName))
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun getDate(tagName: String): Calendar {
        val dateInt = Integer.parseInt(getText(tagName))
        val year = dateInt / 10000
        val month = dateInt / 100 % 100 - 1 // month is 0-based
        val day = dateInt % 100
        val calendar = Calendar.getInstance()
        calendar.clear()
        calendar.set(year, month, day)
        return calendar
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun getTime(tagName: String): DayTime {
        val dayTime = Integer.parseInt(getText(tagName)) // seconds since midnight
        return DayTime(dayTime.toLong())
    }

    companion object {

        private val TAG = "DataLoader"
        private val MAP = "map"
        private val KEY = "key"
        private val VALUE = "value"
        private val ARRAY = "array"
        private val ELEM = "elem"

        private var loaded = false

        fun loadDataIfNot(context: Context) {
            if (loaded) {
                Log.i(TAG, "Data have loaded, skip.")
                return
            }

            Log.i(TAG, "Loading data.")
            try {
                DataLoader(context, R.xml.stops).loadStops()
                DataLoader(context, R.xml.calendar).loadCalendar()
                DataLoader(context, R.xml.calendar_dates).loadCalendarDates()
                DataLoader(context, R.xml.routes).loadRoutes()
            } catch (e: XmlPullParserException) {
                // TODO: show dialog
                throw RuntimeException(e)
            } catch (e: IOException) {
                throw RuntimeException(e)
            }

            Log.i(TAG, "Data loaded.")
            loaded = true
        }
    }
}
