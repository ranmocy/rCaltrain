package me.ranmocy.rcaltrain;

import android.widget.Adapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import me.ranmocy.rcaltrain.models.DayTime;
import me.ranmocy.rcaltrain.models.ScheduleResult;
import me.ranmocy.rcaltrain.shadows.ShadowAlertDialogV7;
import me.ranmocy.rcaltrain.shadows.ShadowScheduleDatabase;

import static com.google.common.truth.Truth.assertThat;

@RunWith(RobolectricTestRunner.class)
@Config(
        constants = BuildConfig.class,
        sdk = 25,
        shadows = {ShadowAlertDialogV7.class, ShadowScheduleDatabase.class}
)
public final class HomeActivityUnitTest {

    private Calendar today;
    private DayTime now;

    private Button btnWeek;
    private Button btnSat;
    private Button btnSun;
    private TextView departureInput;
    private TextView arrivalInput;
    private ListView results;

    @Before
    public void setup() {
        today = Calendar.getInstance();
        now = new DayTime(60 * 60 * 10 - 1); // 1 second to 10:00
        // Even app would load it, we load again here to wait for result
        DataLoader.Companion.loadDataAlways(RuntimeEnvironment.application);

        HomeActivity activity = Robolectric.setupActivity(HomeActivity.class);
        btnWeek = activity.findViewById(R.id.btn_week);
        btnSat = activity.findViewById(R.id.btn_sat);
        btnSun = activity.findViewById(R.id.btn_sun);
        departureInput = activity.findViewById(R.id.input_departure);
        arrivalInput = activity.findViewById(R.id.input_arrival);
        results = activity.findViewById(R.id.results);
    }

    @Test
    public void test() {
        today.clear();
        today.set(2017, 7/*0-based*/, 5); // 20170805
        assertThat(today.get(Calendar.DAY_OF_WEEK)).isEqualTo(Calendar.SATURDAY);

        arrivalInput.performClick();
        clickStation("22nd St");
        departureInput.performClick();
        clickStation("San Francisco");
        btnSat.performClick();

        ListAdapter resultsAdapter = results.getAdapter();
        List<String> resultTimes = new ArrayList<>();
        for (int i = resultsAdapter.getCount() - 1; i >= 0; i--) {
            ScheduleResult result = (ScheduleResult) resultsAdapter.getItem(i);
            resultTimes.add(result.toString());
        }

        assertThat(resultTimes)
                .containsExactly(
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
                        "22:51 => 22:55",
                        "00:05 => 00:10")
                .inOrder();
    }

    private void clickStation(String station) {
        //        Shadows.shadowOf(ShadowAlertDialogV7.getLatestAlertDialog().getListView()).clickFirstItemContainingText(station);
        ShadowAlertDialogV7 shadowAlertDialog = ShadowAlertDialogV7.Companion.getLatestShadowAlertDialog();
        assertThat(shadowAlertDialog).isNotNull();
        Adapter adapter = shadowAlertDialog.getAdapter();
        for (int i = adapter.getCount() - 1; i >= 0; i--) {
            if (station.equals(adapter.getItem(i))) {
                shadowAlertDialog.getShadowListView().performItemClick(i);
                return;
            }
        }
        throw new AssertionError("Can't find station:" + station);
    }
}
