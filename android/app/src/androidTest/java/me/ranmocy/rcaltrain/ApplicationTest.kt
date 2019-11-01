package me.ranmocy.rcaltrain

import android.util.Log
import android.view.View
import android.widget.ListView
import android.widget.TextView
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.anything
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

@RunWith(AndroidJUnit4::class)
@LargeTest
class ApplicationTest {

    @Rule
    var mActivityRule = ActivityTestRule(HomeActivity::class.java)

    @Test
    fun test_schedule_weekday() {
        onView(withId(R.id.btn_week)).perform(click())

        onView(withId(R.id.input_departure)).perform(click())
        onData(`is`(FROM_STATION_NAME)).perform(click())

        onView(withId(R.id.input_arrival)).perform(click())
        onData(`is`(TO_STATION_NAME)).perform(click())

        val expects = Arrays.asList(
                "04:55 => 04:59",
                "05:25 => 05:29",
                "06:05 => 06:09",
                "06:15 => 06:19",
                "06:35 => 06:39",
                "06:45 => 06:51",
                "06:59 => 07:03",
                "07:05 => 07:10",
                "07:15 => 07:19",
                "07:35 => 07:39",
                "07:45 => 07:51",
                "07:59 => 08:03",
                "08:05 => 08:10",
                "08:15 => 08:19",
                "08:35 => 08:39",
                "08:45 => 08:49",
                "09:00 => 09:05",
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

        onView(withId(R.id.results))
                .check(matches(ResultListMatcher(FROM_STATION_NAME, TO_STATION_NAME, expects.size)))

        val interaction = onData(anything()).inAdapterView(withId(R.id.results))
        for (l in expects.indices.reversed()) {
            val expect = expects[l]
            interaction
                    .atPosition(l)
                    .check(matches(ResultViewMatcher(FROM_STATION_NAME, TO_STATION_NAME, l, expect)))
        }
    }

    @Test
    fun test_schedule_saturday() {
        onView(withId(R.id.btn_sat)).perform(click())

        onView(withId(R.id.input_departure)).perform(click())
        onData(`is`(FROM_STATION_NAME)).perform(click())

        onView(withId(R.id.input_arrival)).perform(click())
        onData(`is`(TO_STATION_NAME)).perform(click())

        val expects = Arrays.asList(
                "08:07 => 08:11",
                "09:37 => 09:41",
                "11:07 => 11:11",
                "12:37 => 12:41",
                "14:07 => 14:11",
                "15:37 => 15:41",
                "17:07 => 17:11",
                "18:37 => 18:41",
                "20:07 => 20:11",
                "21:37 => 21:41",
                "22:50 => 22:54",
                "00:05 => 00:10")

        onView(withId(R.id.results))
                .check(matches(ResultListMatcher(FROM_STATION_NAME, TO_STATION_NAME, expects.size)))

        val interaction = onData(anything()).inAdapterView(withId(R.id.results))
        for (l in expects.indices.reversed()) {
            val expect = expects[l]
            interaction
                    .atPosition(l)
                    .check(matches(ResultViewMatcher(FROM_STATION_NAME, TO_STATION_NAME, l, expect)))
        }
    }

    @Test
    fun test_schedule_sunday() {
        onView(withId(R.id.btn_sun)).perform(click())

        onView(withId(R.id.input_departure)).perform(click())
        onData(`is`(FROM_STATION_NAME)).perform(click())

        onView(withId(R.id.input_arrival)).perform(click())
        onData(`is`(TO_STATION_NAME)).perform(click())

        val expects = Arrays.asList(
                "08:07 => 08:11",
                "09:37 => 09:41",
                "11:07 => 11:11",
                "12:37 => 12:41",
                "14:07 => 14:11",
                "15:37 => 15:41",
                "17:07 => 17:11",
                "18:37 => 18:41",
                "20:07 => 20:11",
                "21:37 => 21:41")

        onView(withId(R.id.results))
                .check(matches(ResultListMatcher(FROM_STATION_NAME, TO_STATION_NAME, expects.size)))

        val interaction = onData(anything()).inAdapterView(withId(R.id.results))
        for (l in expects.indices.reversed()) {
            val expect = expects[l]
            interaction
                    .atPosition(l)
                    .check(matches(ResultViewMatcher(FROM_STATION_NAME, TO_STATION_NAME, l, expect)))
        }
    }

    private class ResultListMatcher internal constructor(private val fromName: String, private val toName: String, private val count: Int) : TypeSafeMatcher<View>() {

        override fun matchesSafely(item: View): Boolean {
            val listView = item as ListView
            Log.i("TEST", "Count matching:" + listView.count + ", " + count)
            return listView.count == count
        }

        override fun describeTo(description: Description) {
            description.appendText(
                    String.format("list view of %s to %s should has count %s", fromName, toName, count))
        }
    }

    private class ResultViewMatcher internal constructor(private val fromName: String, private val toName: String, private val position: Int, private val expect: String) : TypeSafeMatcher<View>() {

        override fun matchesSafely(layout: View): Boolean {
            val departureTextView = layout.findViewById<TextView>(R.id.departure_time)
            val arrivalTextView = layout.findViewById<TextView>(R.id.arrival_time)

            return expect == String.format("%s => %s", departureTextView.text, arrivalTextView.text)
        }

        override fun describeTo(description: Description) {
            description.appendText(
                    String.format(
                            "result from %s to %s at position %s should have time:%s",
                            fromName, toName, position, expect))
        }
    }

    companion object {

        private val FROM_STATION_NAME = "San Francisco"
        private val TO_STATION_NAME = "22nd Street"
    }
}
