package me.ranmocy.rcaltrain;

import android.app.Application;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class rCaltrain extends Application {
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public void onCreate() {
        super.onCreate();
        this.executor.submit(new Runnable() {
            public final void run() {
                ScheduleLoader.load(rCaltrain.this);
            }
        });
    }
}
