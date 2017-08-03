package me.ranmocy.rcaltrain

import android.util.Log
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import junit.framework.Assert.assertEquals
import me.ranmocy.rcaltrain.models.ScheduleResult
import me.ranmocy.rcaltrain.shadows.ShadowAlertDialogV7
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.*

/**
 * To work on unit tests, switch the Test Artifact in the Build Variants view.
 */
@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class, sdk = intArrayOf(25), shadows = arrayOf(ShadowAlertDialogV7::class))
class HomeActivityUnitTest {

    private val gson: Gson by lazy { Gson() }

    private val activity: HomeActivity by lazy { Robolectric.setupActivity(HomeActivity::class.java) }
    private val btnWeek: Button by lazy { activity.findViewById<Button>(R.id.btn_week) }
    private val btnSat: Button by lazy { activity.findViewById<Button>(R.id.btn_sat) }
    private val btnSun: Button by lazy { activity.findViewById<Button>(R.id.btn_sun) }
    private val departureInput: TextView by lazy { activity.findViewById<TextView>(R.id.input_departure) }
    private val arrivalInput: TextView by lazy { activity.findViewById<TextView>(R.id.input_arrival) }
    private val results: ListView by lazy { activity.findViewById<ListView>(R.id.results) }

    @Before
    fun setup() {
        gson // init
        activity // init
    }

    @Test
    fun test_stationList() {
        testStationList(getData("weekday_nb_tt.json"))
        testStationList(getData("weekday_sb_tt.json"))
        testStationList(getData("weekend_nb_tt.json"))
        testStationList(getData("weekend_sb_tt.json"))
    }

    @Test
    fun test_schedule_weekdayNB() {
        btnWeek.performClick()
        testSchedule(getData("weekday_nb_tt.json"), false)
    }

    @Test
    fun test_schedule_weekdaySB() {
        btnWeek.performClick()
        testSchedule(getData("weekday_sb_tt.json"), false)
    }

    @Test
    fun test_schedule_saturdayNB() {
        btnSat.performClick()
        testSchedule(getData("weekend_nb_tt.json"), true)
    }

    @Test
    fun test_schedule_saturdaySB() {
        btnSat.performClick()
        testSchedule(getData("weekend_sb_tt.json"), true)
    }

    @Test
    fun test_schedule_sundayNB() {
        btnSun.performClick()
        testSchedule(getData("weekend_nb_tt.json"), false)
    }

    @Test
    fun test_schedule_sundaySB() {
        btnSun.performClick()
        testSchedule(getData("weekend_sb_tt.json"), false)
    }

    private fun testStationList(schedules: List<ScheduleRow>) {
        for (i in schedules.indices.reversed()) {
            val stationName = schedules[i].name!!

            arrivalInput.performClick()
            clickStation(stationName)

            departureInput.performClick()
            clickStation(stationName)
        }
    }

    private fun testSchedule(schedules: List<ScheduleRow>, isSaturday: Boolean) {
        for (i in schedules.indices.reversed()) {
            val to = schedules[i]
            val toName = to.name
            val toStops = to.stop_times
            Log.i("test", "Testing to:" + toName!!)

            arrivalInput.text = toName

            for (j in i - 1 downTo 0) {
                val from = schedules[j]
                val fromName = from.name
                val fromStops = from.stop_times
                Log.i("test", "Testing to:$toName, from:$fromName")
                System.out.printf("Testing to:%s, from:%s%n", toName, fromName)

                departureInput.text = fromName
                activity.reschedule()

                assertEquals(fromStops!!.size, toStops!!.size)

                val expects = ArrayList<TimeResult>()
                for (k in fromStops.indices.reversed()) {
                    assertEquals(fromStops[k].service_type, toStops[k].service_type)
                    if (fromStops[k].isSatOnly && !isSaturday) {
                        continue
                    }
                    if (fromStops[k].time != null && toStops[k].time != null) {
                        expects.add(TimeResult(fromStops[k].time!!, toStops[k].time!!))
                    }
                }

                assertEquals(expects.size, results.count)

                // sort expects
                Collections.sort(expects, Comparator<TimeResult> { o1, o2 ->
                    val t = o1.departureSorting.compareTo(o2.departureSorting)
                    if (t != 0) {
                        return@Comparator t
                    }
                    o1.arrivalSorting.compareTo(o2.arrivalSorting)
                })

                val resultsAdapter = results.adapter
                for (l in expects.indices.reversed()) {
                    val expect = expects[l]
                    val result = resultsAdapter.getItem(l) as ScheduleResult
                    assertEquals(expect.departureDisplay, result.departureTime.toString())
                    assertEquals(expect.arrivalDisplay, result.arrivalTime.toString())
                }
            }
        }
    }

    private fun clickStation(station: String) {
        //        Shadows.shadowOf(ShadowAlertDialogV7.getLatestAlertDialog().getListView()).clickFirstItemContainingText(station);
        val shadowAlertDialog = ShadowAlertDialogV7.latestShadowAlertDialog!!
        val adapter = shadowAlertDialog.adapter
        for (l in adapter.count - 1 downTo 0) {
            if (station == adapter.getItem(l)) {
                shadowAlertDialog.shadowListView.performItemClick(l)
                return
            }
        }
        throw AssertionError("Can't find station:" + station)
    }

    private fun getData(filename: String): List<ScheduleRow> {
        val inputStream = HomeActivityUnitTest::class.java.classLoader.getResourceAsStream(filename)
        val s = Scanner(inputStream).useDelimiter("\\A")
        val result = if (s.hasNext()) s.next() else ""

        return gson.fromJson<List<ScheduleRow>>(result, object : TypeToken<Collection<ScheduleRow>>() {}.type)
    }

    private class ScheduleRow {
        internal var name: String? = null
        internal var stop_times: Array<StopTime>? = null
    }

    private class StopTime {
        internal var service_type: String? = null
        internal var time: String? = null

        internal val isSatOnly: Boolean
            get() = "SatOnly" == service_type
    }

    private class TimeResult internal constructor(internal val departureSorting: String, internal val arrivalSorting: String) {
        internal val departureDisplay: String = fixTime(departureSorting)
        internal val arrivalDisplay: String = fixTime(arrivalSorting)

        // fix time format 24:05 => 00:05
        private fun fixTime(time: String): String {
            val split = time.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val hours = Integer.parseInt(split[0]) % 24
            return String.format(Locale.US, "%02d:%s", hours, split[1])
        }
    }
}
