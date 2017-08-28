package me.ranmocy.rcaltrain;

import android.content.Context;
import android.support.annotation.RawRes;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import me.ranmocy.rcaltrain.database.ScheduleDatabase;
import me.ranmocy.rcaltrain.database.Service;
import me.ranmocy.rcaltrain.database.ServiceDate;
import me.ranmocy.rcaltrain.database.Station;
import me.ranmocy.rcaltrain.database.Stop;
import me.ranmocy.rcaltrain.database.Trip;
import me.ranmocy.rcaltrain.models.DayTime;

public final class ScheduleLoader {

    private static final String TAG = "DataLoader";

    public static void load(Context context) {
        Log.i(TAG, "Loading data");
        try {
            List<Service> services = loadCalendar(context);
            List<ServiceDate> serviceDates = loadCalendarDates(context);
            List<Station> stations = loadStations(context);
            Routes routes = loadRoutes(context);

            Log.i(TAG, "Finish reading");
            ScheduleDatabase
                    .get(context)
                    .updateData(stations, services, serviceDates, routes.trips, routes.stops);
        } catch (IOException | JSONException e) {
            Log.e(TAG, "Failed loading", e);
            // TODO: show dialog
            throw new RuntimeException(e);
        }
        Log.i(TAG, "Data loaded.");
    }

    /**
     * calendar:
     * service_id => {weekday: bool, saturday: bool, sunday: bool, start_date: date, end_date: date}
     * CT-16APR-Caltrain-Weekday-01 => {weekday: false, saturday: true, sunday: false,
     * start_date: 20160404, end_date: 20190331}
     */
    private static List<Service> loadCalendar(Context context) throws IOException, JSONException {
        List<Service> services = new ArrayList<>();

        JSONObject json = getJSON(context, R.raw.calendar);
        for (Iterator<String> it = json.keys(); it.hasNext(); ) {
            String serviceId = it.next();
            JSONObject s = json.getJSONObject(serviceId);

            boolean weekday = s.getBoolean("weekday");
            boolean saturday = s.getBoolean("saturday");
            boolean sunday = s.getBoolean("sunday");
            Calendar startDate = getDate(s.getInt("start_date"));
            Calendar endDate = getDate(s.getInt("end_date"));
            services.add(new Service(serviceId, weekday, saturday, sunday, startDate, endDate));
        }

        return services;
    }

    /**
     * calendar_dates:
     * service_id => [[date, exception_type]]
     * CT-16APR-Caltrain-Weekday-01 => [[20160530,2]]
     */
    private static List<ServiceDate> loadCalendarDates(Context context) throws IOException,
            JSONException {
        List<ServiceDate> serviceDates = new ArrayList<>();

        JSONObject json = getJSON(context, R.raw.calendar_dates);
        for (Iterator<String> it = json.keys(); it.hasNext(); ) {
            String serviceId = it.next();
            JSONArray dateJSON = json.getJSONArray(serviceId);

            for (int i = 0; i < dateJSON.length(); i++) {
                JSONArray pairJSON = dateJSON.getJSONArray(i);

                Calendar date = getDate(pairJSON.getInt(0));
                int type = pairJSON.getInt(1);
                serviceDates.add(new ServiceDate(serviceId, date, type));
            }
        }

        return serviceDates;
    }

    /**
     * stop_name => [stop_id1, stop_id2]
     * "San Francisco" => [70021, 70022]
     */
    private static List<Station> loadStations(Context context) throws IOException, JSONException {
        List<Station> stations = new ArrayList<>();

        JSONObject json = getJSON(context, R.raw.stops);
        for (Iterator<String> it = json.keys(); it.hasNext(); ) {
            String name = it.next();
            JSONArray ids = json.getJSONArray(name);

            for (int i = 0; i < ids.length(); i++) {
                int id = ids.getInt(i);

                stations.add(new Station(id, name));
            }
        }

        return stations;
    }

    private static final class Routes {
        final List<Trip> trips;
        final List<Stop> stops;

        private Routes(List<Trip> trips, List<Stop> stops) {
            this.trips = trips;
            this.stops = stops;
        }
    }

    /**
     * routes:
     * { route_id => { service_id => { trip_id => [[station_id, arrival_time/departure_time(seconds)
     * ]] } } }
     * { "Bullet" => { "CT-14OCT-XXX" => { "650770-CT-14OCT-XXX" => [[70012, 29700], ...] } } }
     */
    private static Routes loadRoutes(Context context) throws IOException, JSONException {
        List<Trip> tripList = new ArrayList<>();
        List<Stop> stopList = new ArrayList<>();

        JSONObject routes = getJSON(context, R.raw.routes);
        for (Iterator<String> routeIds = routes.keys(); routeIds.hasNext(); ) {
            String routeId = routeIds.next();
            JSONObject services = routes.getJSONObject(routeId);

            for (Iterator<String> serviceIds = services.keys(); serviceIds.hasNext(); ) {
                String serviceId = serviceIds.next();
                JSONObject trips = services.getJSONObject(serviceId);

                for (Iterator<String> tripIds = trips.keys(); tripIds.hasNext(); ) {
                    String tripId = tripIds.next();
                    JSONArray stops = trips.getJSONArray(tripId);

                    tripList.add(new Trip(tripId, serviceId));

                    for (int index = 0; index < stops.length(); index++) {
                        JSONArray stop = stops.getJSONArray(index);

                        int stationId = stop.getInt(0);
                        DayTime time = new DayTime(stop.getLong(1));
                        stopList.add(new Stop(tripId, index, stationId, time));
                    }
                }
            }
        }

        return new Routes(tripList, stopList);
    }

    private static Calendar getDate(int dateInt) {
        int year = dateInt / 10000;
        int month = dateInt / 100 % 100 - 1; // month is 0-based
        int day = dateInt % 100;
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(year, month, day);
        return calendar;
    }

    private static JSONObject getJSON(Context context, @RawRes int resId) throws IOException,
            JSONException {
        return new JSONObject(getFileContent(context, resId));
    }

    private static String getFileContent(Context context, @RawRes int resId) throws IOException {
        InputStream is = context.getResources().openRawResource(resId);
        int size = is.available();
        byte[] buffer = new byte[size];
        is.read(buffer);
        is.close();
        return new String(buffer, "UTF-8");
    }
}
