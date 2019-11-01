package me.ranmocy.rcaltrain.database;

import android.content.Context;
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
import org.robolectric.util.ReflectionHelpers;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import androidx.room.Room;
import me.ranmocy.rcaltrain.ScheduleLoader;
import me.ranmocy.rcaltrain.database.ScheduleDao.ServiceType;
import me.ranmocy.rcaltrain.models.DayTime;
import me.ranmocy.rcaltrain.models.ScheduleResult;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class ScheduleDatabaseTest {

    private static final Gson GSON = new Gson();

    private Calendar today;
    private DayTime now;
    private ScheduleDatabase db;

    @Before
    public void setup() {
        today = Calendar.getInstance();
        now = new DayTime(60 * 60 * 10 - 1); // 1 second to 10:00
        Context context = RuntimeEnvironment.application;
        synchronized (ScheduleDatabase.class) {
            db = Room
                    .inMemoryDatabaseBuilder(context, ScheduleDatabase.class)
                    .allowMainThreadQueries()
                    .build();
            ReflectionHelpers.setStaticField(ScheduleDatabase.class, "instance", db);
        }

        ScheduleDatabase dd = ScheduleDatabase.get(context);
        assertThat((Boolean) ReflectionHelpers.getField(dd, "mAllowMainThreadQueries")).isTrue();

        // Even app would load it, we load again here to wait for result
        ScheduleLoader.load(context, db);
    }

    @After
    public void cleanup() {
        synchronized (ScheduleDatabase.class) {
            db.close();
            ReflectionHelpers.setStaticField(ScheduleDatabase.class, "instance", null);
        }
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
        assertThat(actual).containsExactly(weekday, weekend).inOrder();
    }

    @Test
    public void test_today() {
        today = Calendar.getInstance();
        testSchedule(ServiceType.SERVICE_NOW);
    }

    @Test
    public void test_whenWeekday() {
        setToday(ServiceType.SERVICE_WEEKDAY);
        testSchedule(ServiceType.SERVICE_WEEKDAY);
        testSchedule(ServiceType.SERVICE_SATURDAY);
        testSchedule(ServiceType.SERVICE_SUNDAY);
    }

    @Test
    public void test_whenSaturday() {
        setToday(ServiceType.SERVICE_SATURDAY);
        testSchedule(ServiceType.SERVICE_WEEKDAY);
        testSchedule(ServiceType.SERVICE_SATURDAY);
        testSchedule(ServiceType.SERVICE_SUNDAY);
    }

    @Test
    public void test_whenSunday() {
        setToday(ServiceType.SERVICE_SUNDAY);
        testSchedule(ServiceType.SERVICE_WEEKDAY);
        testSchedule(ServiceType.SERVICE_SATURDAY);
        testSchedule(ServiceType.SERVICE_SUNDAY);
    }

    @Test
    public void test_now() {
        today.clear();
        today.set(2017, Calendar.OCTOBER, 3);
        assertThat(Converters.fromCalendar(today)).isEqualTo(20171003);
        assertThat(today.get(Calendar.DAY_OF_WEEK)).isEqualTo(Calendar.TUESDAY);

        assertThat(now.toString()).isEqualTo("09:59");

        List<String> stationNames = db.getStationNamesTesting();
        assertThat(stationNames).contains("San Francisco");
        assertThat(stationNames).contains("22nd St");

        List<String> results = mapResults(db.getResultsTesting("San Francisco",
                                                               "22nd St",
                                                               ServiceType.SERVICE_NOW,
                                                               today,
                                                               now));

        assertThat(results)
                .containsExactly("10:00 => 10:04",
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
                                 "00:05 => 00:10")
                .inOrder();
    }

    /**
     * Set today to given type just in the future of real today.
     */
    private void setToday(@ServiceType int type) {
        int day;
        switch (type) {
            case ServiceType.SERVICE_WEEKDAY:
                today.clear();
                today.set(2017, Calendar.OCTOBER, 4);
                day = Calendar.WEDNESDAY;
                break;
            case ServiceType.SERVICE_SATURDAY:
                today.clear();
                today.set(2017, Calendar.OCTOBER, 7);
                day = Calendar.SATURDAY;
                break;
            case ServiceType.SERVICE_SUNDAY:
                today.clear();
                today.set(2017, Calendar.OCTOBER, 8);
                day = Calendar.SUNDAY;
                break;
            case ServiceType.SERVICE_NOW:
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
        InputStream inputStream = ScheduleDatabaseTest.class
                .getClassLoader()
                .getResourceAsStream(filename);
        Scanner s = new Scanner(inputStream).useDelimiter("\\A");
        String result = s.hasNext() ? s.next() : "";
        return GSON.fromJson(result, (new TypeToken<List<ScheduleRow>>() {}).getType());
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

    private void testSchedule(@ServiceType int type) {
        switch (type) {
            case ServiceType.SERVICE_WEEKDAY:
                testSchedule("weekday_nb_tt.json", ServiceType.SERVICE_WEEKDAY);
                testSchedule("weekday_sb_tt.json", ServiceType.SERVICE_WEEKDAY);
                break;
            case ServiceType.SERVICE_SATURDAY:
                testSchedule("weekend_nb_tt.json", ServiceType.SERVICE_SATURDAY);
                testSchedule("weekend_sb_tt.json", ServiceType.SERVICE_SATURDAY);
                break;
            case ServiceType.SERVICE_SUNDAY:
                testSchedule("weekend_nb_tt.json", ServiceType.SERVICE_SUNDAY);
                testSchedule("weekend_sb_tt.json", ServiceType.SERVICE_SUNDAY);
                break;
            case ServiceType.SERVICE_NOW:
                switch (today.get(Calendar.DAY_OF_WEEK)) {
                    case Calendar.MONDAY:
                    case Calendar.TUESDAY:
                    case Calendar.WEDNESDAY:
                    case Calendar.THURSDAY:
                    case Calendar.FRIDAY:
                        testSchedule(ServiceType.SERVICE_WEEKDAY);
                        break;
                    case Calendar.SATURDAY:
                        testSchedule(ServiceType.SERVICE_SATURDAY);
                        break;
                    case Calendar.SUNDAY:
                        testSchedule(ServiceType.SERVICE_SUNDAY);
                        break;
                    default:
                        throw new IllegalStateException("Unknown day of week");
                }
                break;
            default:
                throw new IllegalStateException("Unknown day of week");
        }
    }

    private void testSchedule(String filename, @ServiceType int type) {
        List<ScheduleRow> schedules = getData(filename);
        boolean isSaturday = type == ServiceType.SERVICE_SATURDAY;

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
                    if ("SatOnly".equals(fromStop.service_type) && !isSaturday) {
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
                    expectTimes.add(String.format("%s => %s",
                                                  fixTime(expect.first),
                                                  fixTime(expect.second)));
                }

                // get results
                List<String> resultTimes = mapResults(db.getResultsTesting(fromName,
                                                                           toName,
                                                                           type,
                                                                           today,
                                                                           now));

                assertWithMessage(String.format("(%s -> %s)", fromName, toName))
                        .that(resultTimes)
                        .containsExactlyElementsIn(expectTimes)
                        .inOrder();
            }
        }
    }
}
