package me.ranmocy.rcaltrain.database;

import android.arch.persistence.room.Room;
import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.Resetter;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import me.ranmocy.rcaltrain.BuildConfig;
import me.ranmocy.rcaltrain.DataLoader;
import me.ranmocy.rcaltrain.models.DayTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
    private static final String STREET = "22nd St";

    private Calendar now;
    private ScheduleDatabase db;

    @Before
    public void setup() {
        DataLoader.Companion.loadDataIfNot(RuntimeEnvironment.application);
        now = Calendar.getInstance();
        db = ScheduleDatabase.get(RuntimeEnvironment.application);
    }

    @Test
    public void test_sqlite_version() {
        String s = db.getOpenHelper().getReadableDatabase()
                .compileStatement("SELECT sqlite_version();").simpleQueryForString();
        String[] split = s.split(".");
        assertEquals(3, split.length);
        int major = Integer.parseInt(split[0]);
        int minor = Integer.parseInt(split[1]);
        int build = Integer.parseInt(split[2]);
        assertTrue(major == 3 && minor == 7 && build == 10);
    }

    @Test
    public void test() {
        db.updateData(
                Arrays.asList(new Station(123, SAN_FRANCISCO), new Station(321, STREET)),
                Collections.singletonList(new Service("s_1", true, false, false, now, now)),
                Collections.<ServiceDate>emptyList(),
                Collections.singletonList(new Trip("t_1", "s_1")),
                Arrays.asList(new Stop("t_1", 1, 123, start), new Stop("t_1", 2, 321, end)));

        List<ScheduleDao.ScheduleResult> results = db.scheduleDao().getResultsSync(SAN_FRANCISCO, STREET, now, ScheduleDao.SERVICE_WEEKDAY);

        assertEquals(1, results.size());
        ScheduleDao.ScheduleResult result = results.get(0);
        assertEquals(start.toSecondsSinceMidnight(), result.departureTime.toSecondsSinceMidnight());
        assertEquals(end.toSecondsSinceMidnight(), result.arrivalTime.toSecondsSinceMidnight());
    }
}
