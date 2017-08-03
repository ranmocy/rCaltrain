package me.ranmocy.rcaltrain.database;

import android.arch.persistence.room.Room;
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
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.Resetter;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Scanner;

import me.ranmocy.rcaltrain.BuildConfig;
import me.ranmocy.rcaltrain.DataLoader;
import me.ranmocy.rcaltrain.models.DayTime;
import me.ranmocy.rcaltrain.models.ScheduleResult;

import static com.google.common.truth.Truth.assertThat;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 17, shadows = {ScheduleDatabaseTest.ShadowScheduleDatabase.class})
public class ScheduleDatabaseTest {

    @Implements(ScheduleDatabase.class)
    public static final class ShadowScheduleDatabase {
        private static ScheduleDatabase instance = null;

        private static final Object LOCK = new Object();

        @Implementation
        public static ScheduleDatabase get(Context context) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = Room
                            .inMemoryDatabaseBuilder(context.getApplicationContext(), ScheduleDatabase.class)
                            .allowMainThreadQueries()
                            .build();
                }
            }
            return instance;
        }

        @Resetter
        static void reset() {
            instance.close();
            instance = null;
        }
    }

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
    public void test_weekdayNB() {
        today.clear();
        today.set(2017, 7/*0-based*/, 2);
        assertThat(Converters.fromCalendar(today)).isEqualTo(20170802);
        assertThat(today.get(Calendar.DAY_OF_WEEK)).isEqualTo(Calendar.WEDNESDAY);

        testSchedule(getData("weekday_nb_tt.json"));
    }

    @Test
    public void test_weekdaySB() {
        today.clear();
        today.set(2017, 7/*0-based*/, 2);
        assertThat(Converters.fromCalendar(today)).isEqualTo(20170802);
        assertThat(today.get(Calendar.DAY_OF_WEEK)).isEqualTo(Calendar.WEDNESDAY);

        testSchedule(getData("weekday_sb_tt.json"));
    }

    @Test
    public void test_saturdayNB() {
        today.clear();
        today.set(2017, 7/*0-based*/, 5);
        assertThat(Converters.fromCalendar(today)).isEqualTo(20170805);
        assertThat(today.get(Calendar.DAY_OF_WEEK)).isEqualTo(Calendar.SATURDAY);

        testSchedule(getData("weekend_nb_tt.json"));
    }

    @Test
    public void test_saturdaySB() {
        today.clear();
        today.set(2017, 7/*0-based*/, 5);
        assertThat(Converters.fromCalendar(today)).isEqualTo(20170805);
        assertThat(today.get(Calendar.DAY_OF_WEEK)).isEqualTo(Calendar.SATURDAY);

        testSchedule(getData("weekend_sb_tt.json"));
    }

    @Test
    public void test_sundayNB() {
        today.clear();
        today.set(2017, 7/*0-based*/, 6);
        assertThat(Converters.fromCalendar(today)).isEqualTo(20170806);
        assertThat(today.get(Calendar.DAY_OF_WEEK)).isEqualTo(Calendar.SUNDAY);

        testSchedule(getData("weekend_nb_tt.json"));
    }

    @Test
    public void test_sundaySB() {
        today.clear();
        today.set(2017, 7/*0-based*/, 6);
        assertThat(Converters.fromCalendar(today)).isEqualTo(20170806);
        assertThat(today.get(Calendar.DAY_OF_WEEK)).isEqualTo(Calendar.SUNDAY);

        testSchedule(getData("weekend_sb_tt.json"));
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
        return GSON.fromJson(result, (new TypeToken<List<ScheduleRow>>() {}).getType());
    }

    // fix time format 24:05 => 00:05
    private String fixTime(String time) {
        String[] split = time.split(":");
        int hours = Integer.parseInt(split[0]) % 24;
        return String.format(Locale.US, "%02d:%s", hours, split[1]);
    }

    private void testSchedule(List<ScheduleRow> schedules) {
        @ScheduleDao.ServiceType int type;
        boolean isSaturday = false;
        switch (today.get(Calendar.DAY_OF_WEEK)) {
            case Calendar.MONDAY:
            case Calendar.TUESDAY:
            case Calendar.WEDNESDAY:
            case Calendar.THURSDAY:
            case Calendar.FRIDAY:
                type = ScheduleDao.SERVICE_WEEKDAY;
                break;
            case Calendar.SATURDAY:
                type = ScheduleDao.SERVICE_SATURDAY;
                isSaturday = true;
                break;
            case Calendar.SUNDAY:
                type = ScheduleDao.SERVICE_SUNDAY;
                break;
            default:
                throw new IllegalStateException("Unknown day of week");
        }

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
                List<ScheduleResult> results = db.getResultsTesting(fromName, toName, type, today, now);
                List<String> resultTimes = new ArrayList<>();
                for (ScheduleResult result : results) {
                    resultTimes.add(result.toString());
                }

                assertThat(resultTimes)
                        .named(String.format("(%s -> %s)", fromName, toName))
                        .isEqualTo(expectTimes);
            }
        }
    }
}
