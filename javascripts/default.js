(function(){
  "use strict";

  var from, to, when, data = {};

  function is_defined (obj) {
    return typeof(obj) !== "undefined";
  }

  function save_cookies () {
    $.cookie("from", from.getText());
    $.cookie("to", to.getText());
    $.cookie("when", $('.when-button.selected').val());
  }

  function load_cookies () {
    $.cookie.defaults.expires = 365; // expire in one year
    $.cookie.defaults.path = '/'; // available across the whole site
    if (is_defined($.cookie("from"))) {
      from.setText($.cookie("from"));
    }
    if (is_defined($.cookie("to"))) {
      to.setText($.cookie("to"));
    }
    if (is_defined($.cookie("when"))) {
      $('.when-button[value="' + $.cookie("when") + '"]').addClass('selected');
    }
  }

  String.prototype.repeat = function(num) {
    return (num <= 0) ? "" : this + this.repeat(num - 1);
  };

  String.prototype.rjust = function(width, padding) {
    padding = (padding || " ").substr(0, 1); // one and only one char
    return padding.repeat(width - this.length) + this;
  };

  Object.extend = function(destination, source) {
    for (var property in source) {
      if (source.hasOwnProperty(property)) {
        destination[property] = source[property];
      }
    }
    return destination;
  };

  // now in seconds since the midnight
  function now () {
    var date = new Date();
    return date.getHours() * 60 * 60 +
           date.getMinutes() * 60 +
           date.getSeconds();
  }

  // now date in format YYYYMMDD
  function now_date () {
    var d = new Date();
    // getMonth starts from 0
    return parseInt([d.getFullYear(), d.getMonth() + 1, d.getDate()].map(function(n){
      return n.toString().rjust(2, '0');
    }).join(''));
  }

  function second2str (seconds) {
    var minutes = Math.floor(seconds / 60);
    return [
      Math.floor(minutes / 60) % 24,
      minutes % 60
    ].map(function(item) {
      return item.toString().rjust(2, '0');
    }).join(':');
  }

  function time_relative (from, to) {
    return Math.round((to - from) / 60); // in minute
  }

  function is_now () {
    return $('.when-button.selected').val() === "now";
  }

  function get_service_ids (calendar, calendar_dates) {
    var date = now_date();

    var selected_schedule = $('.when-button.selected').val();
    var target_schedule = selected_schedule;
    if (target_schedule === 'now') {
      // getDay is "0 for Sunday", map to "0 for Monday"
      switch ((new Date().getDay() + 6) % 7) {
        case 5: target_schedule = 'saturday'; break;
        case 6: target_schedule = 'sunday'; break;
        case 0: case 1: case 2: case 3: case 4: target_schedule = 'weekday'; break;
        default: console.error('Unknown current day', (new Date().getDay() + 6) % 7); return [];
      }
    }

    // calendar:
    //   service_id => [monday,tuesday,wednesday,thursday,friday,saturday,sunday,start_date,end_date]
    // calendar_dates:
    //   service_id => [date,exception_type]
    var service_ids = Object.keys(calendar).filter(function(service_id) {
      // check calendar start/end dates
      var item = calendar[service_id];
      return (item.start_date <= date) && (date <= item.end_date);
    }).filter(function(service_id) {
      // check calendar available days
      return calendar[service_id][target_schedule];
    });

    // In now schedule, we consider exceptional days like holidays defined in calendar_dates file
    if (selected_schedule === 'now') {
      service_ids = service_ids.filter(function(service_id) {
        // check calendar_dates with exception_type 2 (if any to remove)
        return !(service_id in calendar_dates) ||
          calendar_dates[service_id].filter(function(exception_date) {
            return (exception_date[0] === date) && (exception_date[1] === 2);
          }).length === 0;
      }).concat(Object.keys(calendar_dates).filter(function(service_id) {
        // check calendar_dates with exception_type 1 (if any to add)
        return calendar_dates[service_id].filter(function(exception_date) {
          return (exception_date[0] === date) && (exception_date[1] === 1);
        }).length !== 0;
      }));
    }

    if (service_ids.length === 0) {
      console.log("Can't get service for now.");
    }
    return service_ids;
  }

  function get_available_services (routes, calendar, calendar_dates) {
    var availables = {};

    get_service_ids(calendar, calendar_dates).forEach(function(service_id) {
      Object.keys(routes).forEach(function(route_id) {
        var services = routes[route_id];
        var trips = services[service_id];

        if (!is_defined(trips)) {
          // this route does not have this service
          return;
        }

        if (!is_defined(availables[route_id])) {
          availables[route_id] = {};
        }
        Object.extend(availables[route_id], trips);
      });
    });

    return availables;
  }

  function search_index (trip_ids, target_ids) {
    return target_ids.map(function(target_id) {
      return trip_ids.indexOf(target_id);
    }).filter(function(index) {
      return index != -1;
    });
  }

  function compare_trip (a, b) {
    return a.departure_time - b.departure_time;
  }

  function get_trips (services, from_ids, to_ids) {
    var result = [];

    Object.keys(services)
      .forEach(function(service_id) {
        var trips = services[service_id];
        Object.keys(trips)
          .forEach(function(trip_id) {
            var trip = trips[trip_id];
            var trip_stop_ids = trip.map(function(t) { return t[0]; });
            var from_indexes = search_index(trip_stop_ids, from_ids);
            var to_indexes = search_index(trip_stop_ids, to_ids);
            if (!is_defined(from_indexes) || !is_defined(to_indexes) ||
                from_indexes.length === 0 || to_indexes.length === 0) {
              return;
            }
            var from_index = Math.min.apply(this, from_indexes);
            var to_index = Math.max.apply(this, to_indexes);
            // must be in order
            if (from_index >= to_index) {
              return;
            }

            if (!is_now() || trip[from_index][1] > now()) {
              result.push({
                departure_time: trip[from_index][1],
                arrival_time: trip[to_index][1]
              });
            }
          });
      });

    return result.sort(compare_trip);
  }

  function render_info (next_train) {
    var info = $("#info").empty();
    if (is_now() && is_defined(next_train)) {
      var next_relative = time_relative(now(), next_train.departure_time);
      info.append('<div class="info">Next train: ' + next_relative + 'min</div>');
    }
  }

  function render_result (trips) {
    var result = $("#result").empty();
    trips.forEach(function(trip) {
      result.append('<div class="trip">' +
                    '<span class="departure">' + second2str(trip.departure_time) + '</span>' +
                    '<span class="duration">' + time_relative(trip.departure_time, trip.arrival_time) + ' min</span>' +
                    '<span class="arrival">' + second2str(trip.arrival_time) + '</span>' +
                    '</div>');
    });
  }

  function schedule () {
    var stops = data.stops, routes = data.routes,
        calendar = data.calendar, calendar_dates = data.calendar_dates;
    var from_ids = stops[from.getText()],
        to_ids = stops[to.getText()],
        services = get_available_services(routes, calendar, calendar_dates);

    // if some input is invalid, just return
    if (!is_defined(from_ids) || !is_defined(to_ids) || !is_defined(services)) {
      return;
    }

    var trips = get_trips(services, from_ids, to_ids);

    save_cookies();
    render_info(trips[0]);
    render_result(trips);
  }

  function bind_events () {
    [from, to].forEach(function(c) {
      // when focus, reset input
      c.on("focus", function() {
        c.setText('');
        c.input.Show();
      });
      // when change or complete, schedule
      c.on("change", schedule);
      c.on("complete", schedule);
    });

    when.each(function(index, elem) {
      $(elem).on("click", function() {
        when.each(function(index, elem) {
          $(elem).removeClass("selected");
        });
        $(elem).addClass("selected");
        schedule();
      });
    });

    $("#reverse").on("click", function() {
      var t = from.getText();
      from.setText(to.getText());
      to.setText(t);
      schedule();
    });
  }

  function initialize () {
    // remove 300ms delay for mobiles click
    FastClick.attach(document.body);

    // init inputs elements
    from = rComplete($('#from')[0], { placeholder: "Departure" });
    to = rComplete($('#to')[0], { placeholder: "Destination" });
    when = $('.when-button');

    // generate select options
    var names = Object.keys(data.stops);
    from.setOptions(names);
    to.setOptions(names);

    // init
    load_cookies();
    bind_events();
    schedule(); // init schedule

    // Trigger test
    if (new URLSearchParams(window.location.search).get('test') === 'true') {
      test();
    }
  }

  function fetch_data(name_to_path, callback) {
    var data = {};
    Object.keys(name_to_path).forEach(function(name) { data[name] = undefined; });
    Object.keys(name_to_path).forEach(function(name) {
      $.getJSON(name_to_path[name], function(json) {
        data[name] = json;
        for (var p in data) {
          if (typeof(data[p]) === 'undefined') {
            // not all finished, ignore
            return;
          }
        }
        callback(data);
      });
    });

  }

  // init after document and data are ready
  fetch_data({
    "calendar": "data/calendar.json",
    "calendar_dates": "data/calendar_dates.json",
    "stops": "data/stops.json",
    "routes": "data/routes.json",
  },
  function(result) {
    data = result;
    $(initialize);
  });


  // test
  function test() {
    (function(from, to, when, $result) {
      console.debug('Fetching test data');
      fetch_data({
        "weekday_NB_TT": "test/weekday_NB_TT.json",
        "weekday_SB_TT": "test/weekday_SB_TT.json",
        "weekend_NB_TT": "test/weekend_NB_TT.json",
        "weekend_SB_TT": "test/weekend_SB_TT.json",
      }, function(test_data) {
        console.debug('Start testing');

        var $test_result = document.createElement('div');
        $test_result.id = 'text_result';
        document.documentElement.appendChild($test_result);
        function assert(check, msg) {
          if (!check) {
            var $t = document.createElement('div');
            $t.textContent = msg;
            $test_result.appendChild($t);
          }
          return check;
        }

        function fixTimeFormat(time_str) {
          var t = time_str.split(":");
          t[0] = t[0] % 24;
          return t.map(function(item) { return item.toString().rjust(2, '0'); }).join(":");
        }

        function runTest(test_datum, schedule_type) {
          for (var i = test_datum.length - 1; i >= 0; i--) {
            var to_name = test_datum[i].name;
            var to_stops = test_datum[i].stop_times;
            if (!assert(to.getOptions().indexOf(to_name) >= 0, "to_name is not in options:" + to_name)) {
              continue;
            }
            to.setText(to_name);

            for (var j = i - 1; j >= 0; j--) {
              var from_name = test_datum[j].name;
              var from_stops = test_datum[j].stop_times;
              if (!assert(from.getOptions().indexOf(from_name) >= 0, "from_name is not in options:" + from_name)) {
                continue;
              }
              from.setText(from_name);

              var expected = [];
              if (assert(from_stops.length === to_stops.length,
                         "from_stops and to_stops have different length:" + from_name + "=>" + to_name)) {
                for (var k = from_stops.length - 1; k >= 0; k--) {
                  var from_stop = from_stops[k];
                  var to_stop = to_stops[k];
                  if (assert(from_stop.service_type === to_stop.service_type,
                             "from_stop and to_stop have different type:" +
                               "schedule:" + schedule_type + ", " +
                               from_name + "(" + from_stop.service_type + ")=>" +
                               to_name + "(" + to_stop.service_type + ")" +
                               "@" + from_stop.time + ":" + to_stop.time)) {

                    var service_type = from_stop.service_type;
                    if (service_type === 'SatOnly' && schedule_type !== 'saturday') {
                      continue;
                    }
                    if (from_stop.time && to_stop.time) {
                      // since the loop is reversed, insert to first position
                      expected.unshift([from_stop.time, to_stop.time]);
                    }
                  }
                }
              }

              // sort by "depature_time=>arrival_time"
              expected.sort(function(a, b) {
                a = a[0] + "=>" + a[1];
                b = b[0] + "=>" + b[1];
                if (a < b) { return -1; }
                if (a > b) { return 1; }
                return 0;
              });

              var results = $result.children;
              if (assert(expected.length === results.length,
                     "expected and results have different length:" + from_name + "=>" + to_name)) {
                for (var l = results.length - 1; l >= 0; l--) {
                  var from_text = results[l].children[0].textContent;
                  var to_text = results[l].children[2].textContent;
                  var expected_from_text = fixTimeFormat(expected[l][0]);
                  var expected_to_text = fixTimeFormat(expected[l][1]);
                  assert(from_text === expected_from_text && to_text === expected_to_text,
                         "time mismatch: schedule:" + schedule_type + ", " +
                           from_name + "=>" + to_name +
                           ", expected:(" + expected_from_text + " => " + expected_to_text +
                           "), actual:(" + from_text + " => " + to_text + ")");
                }
              } else {
                // debugger;
              }
            }
          }
        }

        when[1].click(); // Weekday
        runTest(test_data.weekday_NB_TT, 'weekday');
        runTest(test_data.weekday_SB_TT, 'weekday');

        when[2].click(); // Saturday
        runTest(test_data.weekend_NB_TT, 'saturday');
        runTest(test_data.weekend_SB_TT, 'saturday');

        when[3].click(); // Sunday
        runTest(test_data.weekend_NB_TT, 'sunday');
        runTest(test_data.weekend_SB_TT, 'sunday');

        var $total = document.createElement('div');
        $total.textContent = "Total failed:" + $test_result.children.length;
        $test_result.insertBefore($total, $test_result.firstChild);
        console.debug('Finish testing');
      });
    })(from, to, when, document.querySelector('#result'));
  }
}());
