package me.ranmocy.rcaltrain.testing;

import android.arch.persistence.room.Room;
import android.content.Context;

import org.robolectric.util.ReflectionHelpers;

import me.ranmocy.rcaltrain.database.ScheduleDatabase;

public final class DatabaseTesting {
    public static ScheduleDatabase setTestingInstance(Context context) {
        ScheduleDatabase instance = Room
                .inMemoryDatabaseBuilder(context.getApplicationContext(), ScheduleDatabase.class)
                .allowMainThreadQueries()
                .build();
        ReflectionHelpers.setStaticField(ScheduleDatabase.class, "instance", instance);
        return instance;
    }

    public static void reset() {
        ReflectionHelpers.setStaticField(ScheduleDatabase.class, "instance", null);
    }
}
