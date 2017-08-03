package me.ranmocy.rcaltrain.shadows;

import android.arch.persistence.room.Room;
import android.content.Context;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.Resetter;

import me.ranmocy.rcaltrain.database.ScheduleDatabase;

@Implements(ScheduleDatabase.class)
public final class ShadowScheduleDatabase {
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
