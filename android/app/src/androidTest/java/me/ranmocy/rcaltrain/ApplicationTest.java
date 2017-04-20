package me.ranmocy.rcaltrain;

import android.support.test.espresso.DataInteraction;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

import me.ranmocy.rcaltrain.models.Station;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.anything;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class ApplicationTest {

    @Rule
    public ActivityTestRule<HomeActivity> mActivityRule = new ActivityTestRule<>(HomeActivity.class);


    private static Gson gson;
    private static List<ScheduleRow> weekdayNB;

    @BeforeClass
    public static void loadData() {
        gson = new Gson();
        weekdayNB = getData("weekday_nb_tt.json");
    }

    private static List<ScheduleRow> getData(String filename) {
        InputStream inputStream = ApplicationTest.class.getClassLoader().getResourceAsStream(filename);
        Scanner s = new Scanner(inputStream).useDelimiter("\\A");
        String result = s.hasNext() ? s.next() : "";

        return gson.fromJson(result, new TypeToken<Collection<ScheduleRow>>() {}.getType());
    }

    private static final class ScheduleRow {
        private String name = null;
        private StopTime[] stop_times = null;
    }

    private static final class StopTime {
        private String service_type = null;
        private String time = null;
    }


    @Test
    public void test_schedule_weekdayNB() {
        onView(withId(R.id.btn_week)).perform(click());

        for (int i = weekdayNB.size() - 1; i >= 0; i--) {
            ScheduleRow to = weekdayNB.get(i);
            String toName = to.name;
            StopTime[] toStops = to.stop_times;
            Log.i("test", "Testing to:" + toName);

            onView(withId(R.id.input_arrival)).perform(click());
            onData(allOf(is(instanceOf(Station.class)), new StationMatcher(toName))).perform(click());

            for (int j = i - 1; j >= 0; j--) {
                ScheduleRow from = weekdayNB.get(j);
                String fromName = from.name;
                StopTime[] fromStops = from.stop_times;
                Log.i("test", "Testing to:" + toName + ", from:" + fromName);

                onView(withId(R.id.input_departure)).perform(click());
                onData(allOf(is(instanceOf(Station.class)), new StationMatcher(fromName))).perform(click());

                if (fromStops.length != toStops.length) throw new AssertionError();
                List<TimeResult> expects = new ArrayList<>();
                for (int k = fromStops.length - 1; k >= 0; k--) {
                    if (!fromStops[k].service_type.equals(toStops[k].service_type)) throw new AssertionError();
                    if (fromStops[k].time != null && toStops[k].time != null) {
                        expects.add(new TimeResult(fromStops[k].time, toStops[k].time));
                    }
                }

                onView(withId(R.id.results)).check(matches(new ResultListMatcher(fromName, toName, expects.size())));

                // sort expects
                Collections.sort(expects, new Comparator<TimeResult>() {
                    @Override
                    public int compare(TimeResult o1, TimeResult o2) {
                        int t = o1.departureSorting.compareTo(o2.departureSorting);
                        if (t != 0) {
                            return t;
                        }
                        return o1.arrivalSorting.compareTo(o2.arrivalSorting);
                    }
                });

                DataInteraction interaction = onData(anything()).inAdapterView(withId(R.id.results));
                for (int l = expects.size() - 1; l >= 0; l--) {
                    TimeResult expect = expects.get(l);
                    interaction.atPosition(l)
                            .check(matches(new ResultViewMatcher(
                                    fromName, toName, l, expect.departureDisplay, expect.arrivalDisplay)));
                }
            }
        }
    }

    private static final class TimeResult {
        private final String departureSorting;
        private final String arrivalSorting;
        private final String departureDisplay;
        private final String arrivalDisplay;

        TimeResult(String departure, String arrival) {
            this.departureSorting = departure;
            this.arrivalSorting = arrival;
            this.departureDisplay = fixTime(departure);
            this.arrivalDisplay = fixTime(arrival);
        }

        // fix time format 24:05 => 00:05
        private String fixTime(String time) {
            String[] split = time.split(":");
            int hours = Integer.parseInt(split[0]) % 24;
            return String.format(Locale.US, "%02d:%s", hours, split[1]);
        }
    }

    private static final class StationMatcher extends TypeSafeMatcher<Station> {

        private String targetName;

        StationMatcher(String targetName) {
            assert targetName != null;
            this.targetName = targetName;
        }

        @Override
        protected boolean matchesSafely(Station item) {
            return targetName.equals(item.getName());
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("station with name ").appendValue(targetName);
        }
    }

    private static final class ResultListMatcher extends TypeSafeMatcher<View> {

        private final String fromName;
        private final String toName;
        private final int count;

        ResultListMatcher(String fromName, String toName, int count) {
            this.fromName = fromName;
            this.toName = toName;
            this.count = count;
        }

        @Override
        protected boolean matchesSafely(View item) {
            ListView listView = (ListView) item;
            return listView.getCount() == count;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(String.format("list view of %s to %s should has count %s", fromName, toName, count));
        }
    }

    private static final class ResultViewMatcher extends TypeSafeMatcher<View> {

        private final String fromName;
        private final String toName;
        private int position;
        private final String expectedDeparture;
        private final String expectedArrival;

        ResultViewMatcher(
                String fromName, String toName, int position, String expectedDeparture, String expectedArrival) {
            this.fromName = fromName;
            this.toName = toName;
            this.position = position;
            this.expectedDeparture = expectedDeparture;
            this.expectedArrival = expectedArrival;
        }

        @Override
        protected boolean matchesSafely(View layout) {
            TextView departureTextView = (TextView) layout.findViewById(R.id.departure_time);
            TextView arrivalTextView = (TextView) layout.findViewById(R.id.arrival_time);

            return expectedDeparture.equals(departureTextView.getText()) &&
                    expectedArrival.equals(arrivalTextView.getText());
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(String.format(
                    "result from %s to %s at position %s should have time:%s:%s",
                    fromName, toName, position, expectedDeparture, expectedArrival));
        }
    }
}
