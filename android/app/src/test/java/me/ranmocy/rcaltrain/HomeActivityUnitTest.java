package me.ranmocy.rcaltrain;

import android.util.Log;
import android.widget.Adapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

import me.ranmocy.rcaltrain.models.ScheduleResult;
import me.ranmocy.rcaltrain.models.Station;

import static junit.framework.Assert.assertEquals;

/**
 * To work on unit tests, switch the Test Artifact in the Build Variants view.
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, shadows = {ShadowAlertDialogV7.class})
public class HomeActivityUnitTest {

    private static Gson gson;
    private static List<ScheduleRow> weekdayNB;

    @BeforeClass
    public static void loadData() {
        gson = new Gson();
        weekdayNB = getData("weekday_nb_tt.json");
    }

    private static List<ScheduleRow> getData(String filename) {
        InputStream inputStream = HomeActivityUnitTest.class.getClassLoader().getResourceAsStream(filename);
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


    private HomeActivity activity;
    private Button btnWeek;
    private Button btnSat;
    private Button btnSun;
    private TextView departureInput;
    private TextView arrivalInput;
    private ListView results;

    @Before
    public void setup() {
        activity = Robolectric.setupActivity(HomeActivity.class);
        btnWeek = (Button) activity.findViewById(R.id.btn_week);
        btnSat = (Button) activity.findViewById(R.id.btn_sat);
        btnSun = (Button) activity.findViewById(R.id.btn_sun);
        departureInput = (TextView) activity.findViewById(R.id.input_departure);
        arrivalInput = (TextView) activity.findViewById(R.id.input_arrival);
        results = (ListView) activity.findViewById(R.id.results);
    }

    @Test
    public void test_stationList() {
        // check station name is in station list
        for (int i = weekdayNB.size() - 1; i >= 0; i--) {
            String stationName = weekdayNB.get(i).name;

            arrivalInput.performClick();
            clickStation(stationName);

            departureInput.performClick();
            clickStation(stationName);
        }
    }

    @Test
    public void test_schedule_weekdayNB() {
        btnWeek.performClick();

        for (int i = weekdayNB.size() - 1; i >= 0; i--) {
            ScheduleRow to = weekdayNB.get(i);
            String toName = to.name;
            StopTime[] toStops = to.stop_times;
            Log.i("test", "Testing to:" + toName);

            arrivalInput.setText(toName);

            for (int j = i - 1; j >= 0; j--) {
                ScheduleRow from = weekdayNB.get(j);
                String fromName = from.name;
                StopTime[] fromStops = from.stop_times;
                Log.i("test", "Testing to:" + toName + ", from:" + fromName);
                System.out.printf("Testing to:%s, from:%s%n", toName, fromName);

                departureInput.setText(fromName);
                activity.reschedule();

                assertEquals(fromStops.length, toStops.length);

                List<TimeResult> expects = new ArrayList<>();
                for (int k = fromStops.length - 1; k >= 0; k--) {
                    assertEquals(fromStops[k].service_type, toStops[k].service_type);
                    if (fromStops[k].time != null && toStops[k].time != null) {
                        expects.add(new TimeResult(fromStops[k].time, toStops[k].time));
                    }
                }

                assertEquals(expects.size(), results.getCount());

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

                ListAdapter resultsAdapter = results.getAdapter();
                for (int l = expects.size() - 1; l >= 0; l--) {
                    TimeResult expect = expects.get(l);
                    ScheduleResult result = (ScheduleResult) resultsAdapter.getItem(l);
                    assertEquals(expect.departureDisplay, result.getDepartureTimeString());
                    assertEquals(expect.arrivalDisplay, result.getArrivalTimeString());
                }
            }
        }
    }

    private void clickStation(String station) {
//        Shadows.shadowOf(ShadowAlertDialogV7.getLatestAlertDialog().getListView()).clickFirstItemContainingText(station);
        ShadowAlertDialogV7 shadowAlertDialog = ShadowAlertDialogV7.getLatestShadowAlertDialog();
        Adapter adapter = shadowAlertDialog.getAdapter();
        for (int l = adapter.getCount() - 1; l >= 0; l--) {
            Station item = (Station) adapter.getItem(l);
            if (station.equals(item.getName())) {
                shadowAlertDialog.getShadowListView().performItemClick(l);
                return;
            }
        }
        throw new AssertionError("Can't find station:" + station);
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
}
