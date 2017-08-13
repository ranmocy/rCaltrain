package me.ranmocy.rcaltrain;

import android.support.test.espresso.DataInteraction;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.CoreMatchers.anything;
import static org.hamcrest.CoreMatchers.is;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class ApplicationTest {

    @Rule
    public ActivityTestRule<HomeActivity> mActivityRule = new ActivityTestRule<>(HomeActivity.class);

    @Test
    public void test_schedule_weekday() {
        String fromName = "San Francisco";
        String toName = "22nd St";

        onView(withId(R.id.btn_week)).perform(click());

        onView(withId(R.id.input_departure)).perform(click());
        onData(is(fromName)).perform(click());

        onView(withId(R.id.input_arrival)).perform(click());
        onData(is(toName)).perform(click());

        List<String> expects = Arrays.asList("04:55 => 04:59",
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
                                             "22:40 => 22:44",
                                             "00:05 => 00:10");

        onView(withId(R.id.results)).check(matches(new ResultListMatcher(fromName,
                                                                         toName,
                                                                         expects.size())));

        DataInteraction interaction = onData(anything()).inAdapterView(withId(R.id.results));
        for (int l = expects.size() - 1; l >= 0; l--) {
            String expect = expects.get(l);
            interaction
                    .atPosition(l)
                    .check(matches(new ResultViewMatcher(fromName, toName, l, expect)));
        }
    }

    @Test
    public void test_schedule_saturday() {
        String fromName = "San Francisco";
        String toName = "22nd St";

        onView(withId(R.id.btn_sat)).perform(click());

        onView(withId(R.id.input_departure)).perform(click());
        onData(is(fromName)).perform(click());

        onView(withId(R.id.input_arrival)).perform(click());
        onData(is(toName)).perform(click());

        List<String> expects = Arrays.asList("08:07 => 08:11",
                                             "09:37 => 09:41",
                                             "11:07 => 11:11",
                                             "12:37 => 12:41",
                                             "14:07 => 14:11",
                                             "15:37 => 15:41",
                                             "17:07 => 17:11",
                                             "18:37 => 18:41",
                                             "20:07 => 20:11",
                                             "21:37 => 21:41",
                                             "22:51 => 22:55",
                                             "00:05 => 00:10");

        onView(withId(R.id.results)).check(matches(new ResultListMatcher(fromName,
                                                                         toName,
                                                                         expects.size())));

        DataInteraction interaction = onData(anything()).inAdapterView(withId(R.id.results));
        for (int l = expects.size() - 1; l >= 0; l--) {
            String expect = expects.get(l);
            interaction
                    .atPosition(l)
                    .check(matches(new ResultViewMatcher(fromName, toName, l, expect)));
        }
    }

    @Test
    public void test_schedule_sunday() {
        String fromName = "San Francisco";
        String toName = "22nd St";

        onView(withId(R.id.btn_sun)).perform(click());

        onView(withId(R.id.input_departure)).perform(click());
        onData(is(fromName)).perform(click());

        onView(withId(R.id.input_arrival)).perform(click());
        onData(is(toName)).perform(click());

        List<String> expects = Arrays.asList("08:07 => 08:11",
                                             "09:37 => 09:41",
                                             "11:07 => 11:11",
                                             "12:37 => 12:41",
                                             "14:07 => 14:11",
                                             "15:37 => 15:41",
                                             "17:07 => 17:11",
                                             "18:37 => 18:41",
                                             "20:07 => 20:11",
                                             "21:37 => 21:41");

        onView(withId(R.id.results)).check(matches(new ResultListMatcher(fromName,
                                                                         toName,
                                                                         expects.size())));

        DataInteraction interaction = onData(anything()).inAdapterView(withId(R.id.results));
        for (int l = expects.size() - 1; l >= 0; l--) {
            String expect = expects.get(l);
            interaction
                    .atPosition(l)
                    .check(matches(new ResultViewMatcher(fromName, toName, l, expect)));
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
            Log.i("TEST", "Count matching:" + listView.getCount() + ", " + count);
            return listView.getCount() == count;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(String.format("list view of %s to %s should has count %s",
                                                 fromName,
                                                 toName,
                                                 count));
        }
    }

    private static final class ResultViewMatcher extends TypeSafeMatcher<View> {

        private final String fromName;
        private final String toName;
        private final int position;
        private final String expect;

        ResultViewMatcher(String fromName, String toName, int position, String expect) {
            this.fromName = fromName;
            this.toName = toName;
            this.position = position;
            this.expect = expect;
        }

        @Override
        protected boolean matchesSafely(View layout) {
            TextView departureTextView = layout.findViewById(R.id.departure_time);
            TextView arrivalTextView = layout.findViewById(R.id.arrival_time);

            return expect.equals(String.format("%s => %s",
                                               departureTextView.getText(),
                                               arrivalTextView.getText()));
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(String.format(
                    "result from %s to %s at position %s should have time:%s",
                    fromName,
                    toName,
                    position,
                    expect));
        }
    }
}
