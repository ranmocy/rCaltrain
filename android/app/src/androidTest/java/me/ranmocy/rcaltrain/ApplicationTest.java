package me.ranmocy.rcaltrain;

import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

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

    @Test
    public void test_schedule_weekdays() {
        onView(withId(R.id.input_departure)).perform(click());
        onData(allOf(is(instanceOf(Station.class)), new StationMatcher("San Francisco"))).perform(click());

        onView(withId(R.id.input_arrival)).perform(click());
        onData(allOf(is(instanceOf(Station.class)), new StationMatcher("Sunnyvale"))).perform(click());

        onView(withId(R.id.btn_week)).perform(click());

        onView(withId(R.id.results)).check(matches(new ResultListMatcher(32)));

        onData(anything()).inAdapterView(withId(R.id.results))
                .atPosition(0)
                .check(matches(new ResultViewMatcher("04:55", "06:10")));
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

        private int count;

        ResultListMatcher(int count) {
            this.count = count;
        }

        @Override
        protected boolean matchesSafely(View item) {
            ListView listView = (ListView) item;
            return listView.getCount() == count;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("list view has count ").appendValue(count);
        }
    }

    private static final class ResultViewMatcher extends TypeSafeMatcher<View> {

        private final String expectedDeparture;
        private final String expectedArrival;

        ResultViewMatcher(String expectedDeparture, String expectedArrival) {
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
            description.appendText("result should have time:" + expectedDeparture + ":" + expectedArrival);
        }
    }
}
