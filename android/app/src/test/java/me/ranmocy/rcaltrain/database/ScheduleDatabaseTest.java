package me.ranmocy.rcaltrain.database;

import android.arch.persistence.room.Room;
import android.content.Context;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import me.ranmocy.rcaltrain.BuildConfig;
import me.ranmocy.rcaltrain.DataLoader;
import me.ranmocy.rcaltrain.models.DayTime;
import me.ranmocy.rcaltrain.models.ScheduleResult;

import static com.google.common.truth.Truth.assertThat;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, shadows = {ScheduleDatabaseTest.ShadowScheduleDatabase.class})
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
        public static void reset() {
            instance.close();
            instance = null;
        }
    }

    private static final DayTime start = new DayTime(100);
    private static final DayTime end = new DayTime(200);
    private static final String SAN_FRANCISCO = "San Francisco";
    private static final String STREET22 = "22nd St";

    private Calendar today;
    private DayTime now;
    private ScheduleDatabase db;

    @Before
    public void setup() {
        today = Calendar.getInstance();
        now = new DayTime(60 * 60 * 10 - 1); // 1 second to 10:00
        db = ScheduleDatabase.get(RuntimeEnvironment.application);
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
    public void test_fakeData() {
        db.updateData(
                Arrays.asList(new Station(123, SAN_FRANCISCO), new Station(321, STREET22)),
                Collections.singletonList(new Service("s_1", true, false, false, today, today)),
                Collections.<ServiceDate>emptyList(),
                Collections.singletonList(new Trip("t_1", "s_1")),
                Arrays.asList(new Stop("t_1", 1, 123, start), new Stop("t_1", 2, 321, end)));

        List<ScheduleResult> results = db.getResultsTesting(
                SAN_FRANCISCO, STREET22, ScheduleDao.SERVICE_WEEKDAY, today, now);

        assertThat(results).hasSize(1);
        ScheduleResult result = results.get(0);
        assertThat(result.departureTime.toSecondsSinceMidnight()).isEqualTo(start.toSecondsSinceMidnight());
        assertThat(result.arrivalTime.toSecondsSinceMidnight()).isEqualTo(end.toSecondsSinceMidnight());
    }

    @Test
    public void test_realData_weekday() {
        today.clear();
        today.set(2017, 6/*0-based*/, 24);
        assertThat(Converters.fromCalendar(today)).isEqualTo(20170724);

        // Even app would load it, we load again here to wait for result
        DataLoader.Companion.loadDataAlways(RuntimeEnvironment.application);

        List<ScheduleResult> results = db.getResultsTesting(
                SAN_FRANCISCO, STREET22, ScheduleDao.SERVICE_WEEKDAY, today, now);

        assertThat(mapDeparture(results)).containsExactly(
                455, 525, 605, 615, 635, 645, 659, 705, 715, 735, 745, 759, 805, 815, 835, 845, 900,
                1000, 1100, 1200, 1300, 1400, 1500, 1632, 1732, 1832, 1930, 2030, 2130, 2240, 2405);
        assertThat(mapArrival(results)).containsExactly(
                459, 529, 609, 619, 639, 651, 703, 710, 719, 739, 751, 803, 810, 819, 839, 849, 905,
                1004, 1104, 1204, 1304, 1404, 1504, 1636, 1736, 1836, 1934, 2034, 2134, 2244, 2410);
    }

    @Test
    public void test_realData_saturday() {
        today.clear();
        today.set(2017, 6/*0-based*/, 29);
        assertThat(Converters.fromCalendar(today)).isEqualTo(20170729);

        // Even app would load it, we load again here to wait for result
        DataLoader.Companion.loadDataAlways(RuntimeEnvironment.application);

        List<ScheduleResult> results = db.getResultsTesting(
                SAN_FRANCISCO, STREET22, ScheduleDao.SERVICE_SATURDAY, today, now);

        assertThat(mapDeparture(results)).containsExactly(
                807, 937, 1107, 1237, 1407, 1537, 1707, 1837, 2007, 2137, 2251, 2405);
        assertThat(mapArrival(results)).containsExactly(
                811, 941, 1111, 1241, 1411, 1541, 1711, 1841, 2011, 2141, 2255, 2410);
    }

    @Test
    public void test_realData_sunday() {
        today.clear();
        today.set(2017, 6/*0-based*/, 30);
        assertThat(Converters.fromCalendar(today)).isEqualTo(20170730);

        // Even app would load it, we load again here to wait for result
        DataLoader.Companion.loadDataAlways(RuntimeEnvironment.application);

        List<ScheduleResult> results = db.getResultsTesting(
                SAN_FRANCISCO, STREET22, ScheduleDao.SERVICE_SUNDAY, today, now);

        assertThat(mapDeparture(results)).containsExactly(
                807, 937, 1107, 1237, 1407, 1537, 1707, 1837, 2007, 2137);
        assertThat(mapArrival(results)).containsExactly(
                811, 941, 1111, 1241, 1411, 1541, 1711, 1841, 2011, 2141);
    }

    @Test
    public void test_realData_now() {
        today.clear();
        today.set(2017, 6/*0-based*/, 24);
        assertThat(Converters.fromCalendar(today)).isEqualTo(20170724);

        assertThat(now.toString()).isEqualTo("09:59");

        // Even app would load it, we load again here to wait for result
        DataLoader.Companion.loadDataAlways(RuntimeEnvironment.application);

        List<ScheduleResult> results = db.getResultsTesting(
                SAN_FRANCISCO, STREET22, ScheduleDao.SERVICE_NOW, today, now);

        assertThat(mapDeparture(results)).containsExactly(
                1000, 1100, 1200, 1300, 1400, 1500, 1632, 1732, 1832, 1930, 2030, 2130, 2240, 2405);
        assertThat(mapArrival(results)).containsExactly(
                1004, 1104, 1204, 1304, 1404, 1504, 1636, 1736, 1836, 1934, 2034, 2134, 2244, 2410);
    }

    private static List<Integer> mapDeparture(List<ScheduleResult> results) {
        List<Integer> list = new ArrayList<>();
        for (ScheduleResult result : results) {
            list.add(formatTime(result.departureTime));
        }
        return list;
    }

    private static List<Integer> mapArrival(List<ScheduleResult> results) {
        List<Integer> list = new ArrayList<>();
        for (ScheduleResult result : results) {
            list.add(formatTime(result.arrivalTime));
        }
        return list;
    }

    private static int formatTime(DayTime dayTime) {
        long seconds = dayTime.toSecondsSinceMidnight();
        long hours = seconds / 60 / 60;
        long minutes = seconds / 60 % 60;
        return (int) (hours * 100 + minutes);
    }
}
