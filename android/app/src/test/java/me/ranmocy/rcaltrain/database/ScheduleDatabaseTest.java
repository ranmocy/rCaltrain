package me.ranmocy.rcaltrain.database;

import android.util.Log;
import android.util.Pair;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import me.ranmocy.rcaltrain.BuildConfig;
import me.ranmocy.rcaltrain.DataLoader;
import me.ranmocy.rcaltrain.models.DayTime;
import me.ranmocy.rcaltrain.models.ScheduleResult;
import me.ranmocy.rcaltrain.shadows.ShadowScheduleDatabase;

import static com.google.common.truth.Truth.assertThat;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 17, shadows = {ShadowScheduleDatabase.class})
public class ScheduleDatabaseTest {

    private static final Gson GSON = new Gson();

    private Calendar today;
    private DayTime now;
    private ScheduleDatabase db;

    @Before
    public void setup() {
        today = Calendar.getInstance();
        now = new DayTime(60 * 60 * 10 - 1); // 1 second to 10:00
        db = ScheduleDatabase.get(RuntimeEnvironment.application);
        // Even app would load it, we load again here to wait for result
        DataLoader.Companion.loadDataAlways(RuntimeEnvironment.application);
    }

    @After
    public void clean() {
        db.close();
        ShadowScheduleDatabase.reset();
    }

    @Test
    public void test_sqlite_version() {
        // Robolectric ships with SQLite 3.7.10
        // Android 4.1(16) ships with 3.7.11
        String s = db.getOpenHelper().getReadableDatabase()
                .compileStatement("SELECT sqlite_version();").simpleQueryForString();
        String[] split = s.split("\\.");
        assertThat(split.length).isEqualTo(3);
        int major = Integer.parseInt(split[0]);
        int minor = Integer.parseInt(split[1]);
        int build = Integer.parseInt(split[2]);
        assertThat(major == 3 && minor == 7 && build == 10).isTrue();
    }

    @Test
    public void test_stationName() {
        List<ScheduleRow> schedules = getData("weekday_sb_tt.json");
        List<String> weekday = new ArrayList<>();
        for (ScheduleRow schedule : schedules) {
            weekday.add(schedule.name);
        }
        schedules = getData("weekend_sb_tt.json");
        List<String> weekend = new ArrayList<>();
        for (ScheduleRow schedule : schedules) {
            weekend.add(schedule.name);
        }

        List<String> actual = db.getStationNamesTesting();
        assertThat(actual).containsAllIn(weekday).inOrder();
        assertThat(actual).containsAllIn(weekend).inOrder();
    }

    @Test
    public void test_today() {
        today = Calendar.getInstance();
        testSchedule(ScheduleDao.SERVICE_NOW);
    }

    @Test
    public void test_whenWeekday() {
        setToday(ScheduleDao.SERVICE_WEEKDAY);
        testSchedule(ScheduleDao.SERVICE_WEEKDAY);
        testSchedule(ScheduleDao.SERVICE_SATURDAY);
        testSchedule(ScheduleDao.SERVICE_SUNDAY);
    }

    @Test
    public void test_whenSaturday() {
        setToday(ScheduleDao.SERVICE_SATURDAY);
        testSchedule(ScheduleDao.SERVICE_WEEKDAY);
        testSchedule(ScheduleDao.SERVICE_SATURDAY);
        testSchedule(ScheduleDao.SERVICE_SUNDAY);
    }

    @Test
    public void test_whenSunday() {
        setToday(ScheduleDao.SERVICE_SUNDAY);
        testSchedule(ScheduleDao.SERVICE_WEEKDAY);
        testSchedule(ScheduleDao.SERVICE_SATURDAY);
        testSchedule(ScheduleDao.SERVICE_SUNDAY);
    }

    @Test
    public void test_now() {
        today.clear();
        today.set(2017, 7/*0-based*/, 1);
        assertThat(Converters.fromCalendar(today)).isEqualTo(20170801);
        assertThat(today.get(Calendar.DAY_OF_WEEK)).isEqualTo(Calendar.TUESDAY);

        assertThat(now.toString()).isEqualTo("09:59");

        List<String> results = mapResults(db.getResultsTesting("San Francisco", "22nd St", ScheduleDao.SERVICE_NOW, today, now));

        assertThat(results).containsExactly(
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
                "00:05 => 00:10").inOrder();
    }

    @Test
    public void test() {
        // TODO: MAY HAVE ISSUE HERE
//        setToday(ScheduleDao.SERVICE_SATURDAY);
        assertThat(today.get(Calendar.DAY_OF_WEEK)).isEqualTo(Calendar.FRIDAY);

        assertThat(now.toString()).isEqualTo("09:59");

        List<ScheduleResult> resultsSync = db.scheduleDao().getResultsSync(
                "San Francisco", "22nd St", ScheduleDao.SERVICE_SATURDAY, today, null);
        List<String> results = mapResults(resultsSync);
//        List<String> results = mapResults(db.getResultsTesting(
// "San Francisco", "22nd St", ScheduleDao.SERVICE_SATURDAY, today, now));

        assertThat(results).containsExactly(
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
                "00:05 => 00:10").inOrder();
    }

    /**
     * Set today to given type just in the future of real today.
     */
    private void setToday(@ScheduleDao.ServiceType int type) {
        int day = 0;
        switch (type) {
            case ScheduleDao.SERVICE_WEEKDAY:
                today.clear();
                today.set(2017, 6/*0-based*/, 19);
                day = Calendar.WEDNESDAY;
                break;
            case ScheduleDao.SERVICE_SATURDAY:
                today.clear();
                today.set(2017, 6/*0-based*/, 22);
                day = Calendar.SATURDAY;
                break;
            case ScheduleDao.SERVICE_SUNDAY:
                today.clear();
                today.set(2017, 6/*0-based*/, 23);
                day = Calendar.SUNDAY;
                break;
            case ScheduleDao.SERVICE_NOW:
            default:
                return;
        }
        assertThat(today.get(Calendar.DAY_OF_WEEK)).isEqualTo(day);
        long diff = Calendar.getInstance().getTimeInMillis() - today.getTimeInMillis();
        int days = (int) Math.ceil(TimeUnit.MILLISECONDS.toDays(diff) * 1.0 / 7);
        today.setTimeInMillis(today.getTimeInMillis() + TimeUnit.DAYS.toMillis(days * 7));
        assertThat(today.get(Calendar.DAY_OF_WEEK)).isEqualTo(day);
    }

    private static class StopTime {
        String service_type;
        String time;
    }

    private static class ScheduleRow {
        String name;
        List<StopTime> stop_times;
    }

    private List<ScheduleRow> getData(String filename) {
        InputStream inputStream = ScheduleDatabaseTest.class.getClassLoader().getResourceAsStream(filename);
        Scanner s = new Scanner(inputStream).useDelimiter("\\A");
        String result = s.hasNext() ? s.next() : "";
        return GSON.fromJson(result, (new TypeToken<List<ScheduleRow>>() {
        }).getType());
    }

    // fix time format 24:05 => 00:05
    private String fixTime(String time) {
        String[] split = time.split(":");
        int hours = Integer.parseInt(split[0]) % 24;
        return String.format(Locale.US, "%02d:%s", hours, split[1]);
    }

    private List<String> mapResults(List<ScheduleResult> results) {
        List<String> resultTimes = new ArrayList<>();
        for (ScheduleResult result : results) {
            resultTimes.add(result.toString());
        }
        return resultTimes;
    }

    private void testSchedule(@ScheduleDao.ServiceType int type) {
        switch (type) {
            case ScheduleDao.SERVICE_WEEKDAY:
                testSchedule("weekday_nb_tt.json", ScheduleDao.SERVICE_WEEKDAY);
                testSchedule("weekday_sb_tt.json", ScheduleDao.SERVICE_WEEKDAY);
                break;
            case ScheduleDao.SERVICE_SATURDAY:
                testSchedule("weekend_nb_tt.json", ScheduleDao.SERVICE_SATURDAY);
                testSchedule("weekend_sb_tt.json", ScheduleDao.SERVICE_SATURDAY);
                break;
            case ScheduleDao.SERVICE_SUNDAY:
                testSchedule("weekend_nb_tt.json", ScheduleDao.SERVICE_SUNDAY);
                testSchedule("weekend_sb_tt.json", ScheduleDao.SERVICE_SUNDAY);
                break;
            case ScheduleDao.SERVICE_NOW:
                switch (today.get(Calendar.DAY_OF_WEEK)) {
                    case Calendar.MONDAY:
                    case Calendar.TUESDAY:
                    case Calendar.WEDNESDAY:
                    case Calendar.THURSDAY:
                    case Calendar.FRIDAY:
                        testSchedule(ScheduleDao.SERVICE_WEEKDAY);
                        break;
                    case Calendar.SATURDAY:
                        testSchedule(ScheduleDao.SERVICE_SATURDAY);
                        break;
                    case Calendar.SUNDAY:
                        testSchedule(ScheduleDao.SERVICE_SUNDAY);
                        break;
                    default:
                        throw new IllegalStateException("Unknown day of week");
                }
                break;
            default:
                throw new IllegalStateException("Unknown day of week");
        }
    }

    private void testSchedule(String filename, @ScheduleDao.ServiceType int type) {
        List<ScheduleRow> schedules = getData(filename);
        boolean isSaturday = type == ScheduleDao.SERVICE_SATURDAY;

        for (int i = schedules.size() - 1; i >= 0; i--) {
            ScheduleRow to = schedules.get(i);
            String toName = to.name;
            List<StopTime> toStops = to.stop_times;
            Log.i("test", "Testing to:" + toName);

            for (int j = i - 1; j >= 0; j--) {
                ScheduleRow from = schedules.get(j);
                String fromName = from.name;
                List<StopTime> fromStops = from.stop_times;
                Log.i("test", "Testing to:" + toName + ", from:" + fromName);

                assertThat(fromStops.size()).isEqualTo(toStops.size());

                // get expects
                List<Pair<String, String>> expects = new ArrayList<>();
                for (int k = fromStops.size() - 1; k >= 0; k--) {
                    StopTime fromStop = fromStops.get(k);
                    StopTime toStop = toStops.get(k);
                    assertThat(fromStop.service_type).isEqualTo(toStop.service_type);
                    if (Objects.equals("SatOnly", fromStop.service_type) && !isSaturday) {
                        continue;
                    }
                    if (fromStop.time != null && toStop.time != null) {
                        expects.add(new Pair<>(fromStop.time, toStop.time));
                    }
                }
                Collections.sort(expects, new Comparator<Pair<String, String>>() {
                    @Override
                    public int compare(Pair<String, String> a, Pair<String, String> b) {
                        int i = a.first.compareTo(b.first);
                        return i != 0 ? i : a.second.compareTo(b.second);
                    }
                });
                List<String> expectTimes = new ArrayList<>();
                for (Pair<String, String> expect : expects) {
                    expectTimes.add(String.format("%s => %s", fixTime(expect.first), fixTime(expect.second)));
                }

                // get results
                List<String> resultTimes = mapResults(db.getResultsTesting(fromName, toName, type, today, now));

                if (Objects.equals(fromName, "22nd St") && Objects.equals(toName, "San Francisco")) {
                    assertThat(true).isTrue();
                }
                assertThat(resultTimes)
                        .named(String.format("(%s -> %s)", fromName, toName))
                        .containsExactlyElementsIn(expectTimes)
                        .inOrder();
            }
        }
    }
}
