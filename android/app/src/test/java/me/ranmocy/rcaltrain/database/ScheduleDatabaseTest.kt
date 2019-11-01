package me.ranmocy.rcaltrain.database

import android.util.Log
import android.util.Pair
import androidx.room.Room
import com.google.common.reflect.TypeToken
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.google.gson.Gson
import me.ranmocy.rcaltrain.ScheduleLoader
import me.ranmocy.rcaltrain.database.ScheduleDao.ServiceType
import me.ranmocy.rcaltrain.models.DayTime
import me.ranmocy.rcaltrain.models.ScheduleResult
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers
import java.util.*
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ScheduleDatabaseTest {

    private var today: Calendar? = null
    private var now: DayTime? = null
    private var db: ScheduleDatabase? = null

    @Before
    fun setup() {
        today = Calendar.getInstance()
        now = DayTime((60 * 60 * 10 - 1).toLong()) // 1 second to 10:00
        val context = RuntimeEnvironment.application
        synchronized(ScheduleDatabase::class.java) {
            db = Room.inMemoryDatabaseBuilder(context, ScheduleDatabase::class.java)
                    .allowMainThreadQueries()
                    .build()
            ReflectionHelpers.setStaticField(ScheduleDatabase::class.java, "instance", db)
        }

        val dd = ScheduleDatabase.get(context)
        assertThat(ReflectionHelpers.getField<Any>(dd, "mAllowMainThreadQueries") as Boolean).isTrue()

        // Even app would load it, we load again here to wait for result
        ScheduleLoader.load(context, db!!)
    }

    @After
    fun cleanup() {
        synchronized(ScheduleDatabase::class.java) {
            db!!.close()
            ReflectionHelpers.setStaticField(ScheduleDatabase::class.java, "instance", null)
        }
    }

    @Test
    fun test_stationName() {
        var schedules = getData("weekday_sb_tt.json")
        val weekday = ArrayList<String>()
        for (schedule in schedules!!) {
            weekday.add(schedule.name!!)
        }
        schedules = getData("weekend_sb_tt.json")
        val weekend = ArrayList<String>()
        for (schedule in schedules!!) {
            weekend.add(schedule.name!!)
        }

        val actual = db!!.stationNamesTesting
        assertThat(actual).containsNoDuplicates()
        assertThat(actual).containsAtLeastElementsIn(weekday).inOrder()
        assertThat(actual).containsAtLeastElementsIn(weekend).inOrder()
    }

    @Test
    fun test_today() {
        today = Calendar.getInstance()
        testSchedule(ServiceType.SERVICE_NOW)
    }

    @Test
    fun test_whenWeekday() {
        setToday(ServiceType.SERVICE_WEEKDAY)
        testSchedule(ServiceType.SERVICE_WEEKDAY)
        testSchedule(ServiceType.SERVICE_SATURDAY)
        testSchedule(ServiceType.SERVICE_SUNDAY)
    }

    @Test
    fun test_whenSaturday() {
        setToday(ServiceType.SERVICE_SATURDAY)
        testSchedule(ServiceType.SERVICE_WEEKDAY)
        testSchedule(ServiceType.SERVICE_SATURDAY)
        testSchedule(ServiceType.SERVICE_SUNDAY)
    }

    @Test
    fun test_whenSunday() {
        setToday(ServiceType.SERVICE_SUNDAY)
        testSchedule(ServiceType.SERVICE_WEEKDAY)
        testSchedule(ServiceType.SERVICE_SATURDAY)
        testSchedule(ServiceType.SERVICE_SUNDAY)
    }

    @Test
    fun test_now() {
        today!!.clear()
        today!!.set(2019, Calendar.OCTOBER, 29)
        assertThat(Converters.fromCalendar(today)).isEqualTo(20191029)
        assertThat(today!!.get(Calendar.DAY_OF_WEEK)).isEqualTo(Calendar.TUESDAY)

        assertThat(now!!.toString()).isEqualTo("09:59")

        val fromStationName = "San Francisco"
        val toStationName = "22nd Street"

        val stationNames = db!!.stationNamesTesting
        assertThat(stationNames).contains(fromStationName)
        assertThat(stationNames).contains(toStationName)

        val results = mapResults(
                db!!.getResultsTesting(
                        fromStationName, toStationName, ServiceType.SERVICE_NOW, today, now))

        assertThat(results)
                .containsExactly(
                        "10:00 => 10:04",
                        "11:00 => 11:04",
                        "12:00 => 12:04",
                        "13:00 => 13:04",
                        "14:00 => 14:04",
                        "15:00 => 15:04",
                        "16:32 => 16:36",
                        "17:32 => 17:36",
                        "18:32 => 18:36",
                        "19:30 => 19:34",
                        "20:30 => 20:34",
                        "21:30 => 21:34",
                        "22:30 => 22:34",
                        "00:05 => 00:10")
                .inOrder()
    }

    /** Set today to given type just in the future of real today.  */
    private fun setToday(@ServiceType type: Int) {
        val day: Int
        when (type) {
            ServiceType.SERVICE_WEEKDAY -> {
                today!!.clear()
                today!!.set(2017, Calendar.OCTOBER, 4)
                day = Calendar.WEDNESDAY
            }
            ServiceType.SERVICE_SATURDAY -> {
                today!!.clear()
                today!!.set(2017, Calendar.OCTOBER, 7)
                day = Calendar.SATURDAY
            }
            ServiceType.SERVICE_SUNDAY -> {
                today!!.clear()
                today!!.set(2017, Calendar.OCTOBER, 8)
                day = Calendar.SUNDAY
            }
            ServiceType.SERVICE_NOW -> return
            else -> return
        }
        assertThat(today!!.get(Calendar.DAY_OF_WEEK)).isEqualTo(day)
        // Set to the nearest day with same day_of_week
        val diff = Calendar.getInstance().timeInMillis - today!!.timeInMillis
        val days = Math.floor(TimeUnit.MILLISECONDS.toDays(diff) * 1.0 / 7).toInt()
        today!!.timeInMillis = today!!.timeInMillis + TimeUnit.DAYS.toMillis((days * 7).toLong())
        assertThat(today!!.get(Calendar.DAY_OF_WEEK)).isEqualTo(day)
    }

    private fun getData(filename: String): List<ScheduleRow>? {
        val inputStream = ScheduleDatabaseTest::class.java.classLoader!!.getResourceAsStream(filename)
        val s = Scanner(inputStream).useDelimiter("\\A")
        val result = if (s.hasNext()) s.next() else ""
        return GSON.fromJson<List<ScheduleRow>>(result, object: TypeToken<List<ScheduleRow>>(){}.type)
    }

    // fix time format 24:05 => 00:05
    private fun fixTime(time: String): String {
        val split = time.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val hours = Integer.parseInt(split[0]) % 24
        return String.format(Locale.US, "%02d:%s", hours, split[1])
    }

    private fun mapResults(results: List<ScheduleResult>): List<String> {
        val resultTimes = ArrayList<String>()
        for (result in results) {
            resultTimes.add(result.toString())
        }
        return resultTimes
    }

    private fun testSchedule(@ServiceType type: Int) {
        when (type) {
            ServiceType.SERVICE_WEEKDAY -> {
                testSchedule("weekday_nb_tt.json", ServiceType.SERVICE_WEEKDAY)
                testSchedule("weekday_sb_tt.json", ServiceType.SERVICE_WEEKDAY)
            }
            ServiceType.SERVICE_SATURDAY -> {
                testSchedule("weekend_nb_tt.json", ServiceType.SERVICE_SATURDAY)
                testSchedule("weekend_sb_tt.json", ServiceType.SERVICE_SATURDAY)
            }
            ServiceType.SERVICE_SUNDAY -> {
                testSchedule("weekend_nb_tt.json", ServiceType.SERVICE_SUNDAY)
                testSchedule("weekend_sb_tt.json", ServiceType.SERVICE_SUNDAY)
            }
            ServiceType.SERVICE_NOW -> when (today!!.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY -> testSchedule(ServiceType.SERVICE_WEEKDAY)
                Calendar.SATURDAY -> testSchedule(ServiceType.SERVICE_SATURDAY)
                Calendar.SUNDAY -> testSchedule(ServiceType.SERVICE_SUNDAY)
                else -> throw IllegalStateException("Unknown day of week")
            }
            else -> throw IllegalStateException("Unknown day of week")
        }
    }

    private fun testSchedule(filename: String, @ServiceType type: Int) {
        val schedules = getData(filename)
        val isSaturday = type == ServiceType.SERVICE_SATURDAY

        for (i in schedules!!.indices.reversed()) {
            val to = schedules[i]
            val toName = to.name
            val toStops = to.stop_times
            Log.i("test", "Testing to:" + toName!!)

            for (j in i - 1 downTo 0) {
                val from = schedules[j]
                val fromName = from.name
                val fromStops = from.stop_times
                Log.i("test", "Testing to:$toName, from:$fromName")

                assertThat(fromStops!!.size).isEqualTo(toStops!!.size)

                // get expects
                val expects = ArrayList<Pair<String, String>>()
                for (k in fromStops.indices.reversed()) {
                    val fromStop = fromStops[k]
                    val toStop = toStops[k]
                    assertThat(fromStop.service_type).isEqualTo(toStop.service_type)
                    if ("SatOnly" == fromStop.service_type && !isSaturday) {
                        continue
                    }
                    if (fromStop.time != null && toStop.time != null) {
                        expects.add(Pair<String, String>(fromStop.time, toStop.time))
                    }
                }
                expects.sortWith(Comparator { a, b ->
                    val result = a.first.compareTo(b.first)
                    if (result != 0) result else a.second.compareTo(b.second)
                })
                val expectTimes = ArrayList<String>()
                for (expect in expects) {
                    expectTimes.add(String.format("%s => %s", fixTime(expect.first), fixTime(expect.second)))
                }

                // get results
                val resultTimes = mapResults(db!!.getResultsTesting(fromName, toName, type, today, now))

                assertWithMessage(String.format("(%s -> %s)", fromName, toName))
                        .that(resultTimes)
                        .containsExactlyElementsIn(expectTimes)
                        .inOrder()
            }
        }
    }

    private class StopTime {
        internal var service_type: String? = null
        internal var time: String? = null
    }

    private class ScheduleRow {
        internal var name: String? = null
        internal var stop_times: List<StopTime>? = null
    }

    companion object {

        private val GSON = Gson()
    }
}
