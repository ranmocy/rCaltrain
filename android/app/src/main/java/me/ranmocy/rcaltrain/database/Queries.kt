package me.ranmocy.rcaltrain.database

import me.ranmocy.rcaltrain.database.ScheduleDao.*

private const val FROM_STATION_ID = "SELECT id FROM stations WHERE name = :from"
private const val TO_STATION_ID = "SELECT id FROM stations WHERE name = :to"
private const val FROM_SEQUENCE = "SELECT sequence FROM stops WHERE trip_id = trips.id AND station_id in ($FROM_STATION_ID) LIMIT 1"
private const val TO_SEQUENCE = "SELECT sequence FROM stops WHERE trip_id = trips.id AND station_id in ($TO_STATION_ID) LIMIT 1"

// WITH was introduced in sqlite 3.8.3: https://stackoverflow.com/questions/24036310/error-using-sql-with-clause
// sqlite versions: https://stackoverflow.com/questions/2421189/version-of-sqlite-used-in-android
const val QUERY = """
SELECT f.time AS departureTime, t.time AS arrivalTime
  FROM stops AS f, stops AS t
 WHERE f.station_id IN (SELECT id FROM stations WHERE name = :from)
   AND t.station_id IN (SELECT id FROM stations WHERE name = :to)
   AND f.trip_id = t.trip_id
   AND f.trip_id IN
       (SELECT id
          FROM trips
         WHERE ($FROM_SEQUENCE) NOT NULL
           AND ($TO_SEQUENCE) NOT NULL
           AND ($FROM_SEQUENCE) < ($TO_SEQUENCE)
           AND service_id IN
               (SELECT id
                  FROM services
                 WHERE CASE :serviceType
                       WHEN $SERVICE_WEEKDAY THEN weekday
                       WHEN $SERVICE_SATURDAY THEN saturday
                       WHEN $SERVICE_SUNDAY THEN sunday
                       ELSE NULL
                       END
                   AND (id NOT IN (SELECT service_id FROM service_dates WHERE date = :today AND type = 2)
                        AND :today BETWEEN start_date AND end_date
                        OR id IN (SELECT service_id FROM service_dates WHERE date = :today AND type = 1))))
"""
