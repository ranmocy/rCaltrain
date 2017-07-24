package me.ranmocy.rcaltrain.database

import me.ranmocy.rcaltrain.database.ScheduleDao.*

const val QUERY = """
WITH
available_service_ids AS
  (SELECT id FROM services WHERE
    (
      (weekday AND :serviceType = $SERVICE_WEEKDAY)
      AND
      (saturday AND :serviceType = $SERVICE_SATURDAY)
      AND
      (sunday AND :serviceType = $SERVICE_SUNDAY)
    )
    AND
    (
      id IN (SELECT service_id FROM service_dates WHERE date = :now and type = 1)
      OR
      (
        id NOT IN (SELECT service_id FROM service_dates WHERE date = :now and type = 2)
        AND start_date <= :now
        AND end_date >= :now
      )
    )
  )
,

from_station_ids AS
  (SELECT id FROM stations WHERE name = :from)
,

to_station_ids AS
  (SELECT id FROM stations WHERE name = :to)
,

target_trip_ids AS
  (
    SELECT id
    FROM trips
    WHERE service_id in available_service_ids
      AND
        (SELECT sequence
         FROM stops
         WHERE trip_id = trips.id AND station_id in from_station_ids
         LIMIT 1) NOT NULL
      AND
        (SELECT sequence
         FROM stops
         WHERE trip_id = trips.id AND station_id in to_station_ids
         LIMIT 1) NOT NULL
      AND
        (SELECT sequence
         FROM stops
         WHERE trip_id = trips.id AND station_id in from_station_ids
         LIMIT 1)
        <
        (SELECT sequence
         FROM stops
         WHERE trip_id = trips.id AND station_id in to_station_ids
         LIMIT 1)
  )

SELECT f.time AS departureTime, t.time AS arrivalTime
FROM stops as f, stops as t
WHERE f.station_id in from_station_ids
  AND t.station_id in to_station_ids
  AND f.trip_id = t.trip_id
  AND f.trip_id IN target_trip_ids
"""
