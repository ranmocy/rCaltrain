(function(){
  "use strict";

  var $from, $to, $whenList, data = {}, $ = function() { return document.querySelector.apply(document, arguments); };

  function findAll() {
    var nodeList = document.querySelectorAll.apply(document, arguments);
    nodeList.forEach = function(callback) {
      [].forEach.call(nodeList, callback);
    };
    return nodeList;
  }

  function onDocumentReady(callback) {
    if (document.readyState != 'loading'){
      callback();
    } else if (document.addEventListener) {
      document.addEventListener('DOMContentLoaded', callback);
    } else {
      document.attachEvent('onreadystatechange', function() {
        if (document.readyState != 'loading')
          callback();
      });
    }
  }

  function is_defined (obj) {
    return typeof(obj) !== "undefined" && obj !== null;
  }

  function extendObject(destination, source) {
    for (var property in source) {
      if (source.hasOwnProperty(property)) {
        destination[property] = source[property];
      }
    }
    return destination;
  }

  function repeatString(str, num) {
    return (num <= 0) ? "" : str + repeatString(str, num - 1);
  }

  function rjustString(str, width, padding) {
    padding = (padding || " ").substr(0, 1); // one and only one char
    return repeatString(padding, width - str.length) + str;
  }

  function createElement(name, attrs) {
    var $elem = document.createElement(name);
    if (is_defined(attrs)) {
      Object.keys(attrs).forEach(function(attr_name) {
        $elem[attr_name] = attrs[attr_name];
      });
    }
    return $elem;
  }

  function removeAllChildren(elem) {
    while(elem.firstChild) elem.removeChild(elem.firstChild);
    return elem;
  }

  function addClass(elem, className) {
    if (elem.classList)
      elem.classList.add(className);
    else
      elem.className += ' ' + className;
  }

  function removeClass(elem, className) {
    if (elem.classList)
      elem.classList.remove(className);
    else
      elem.className = elem.className.replace(new RegExp('(^|\\b)' + className.split(' ').join('|') + '(\\b|$)', 'gi'), ' ');
  }

  function bindEvent(elem, eventName, handler) {
    if (elem.addEventListener) {
      elem.addEventListener(eventName, handler);
    } else {
      elem.attachEvent('on' + eventName, function(){
        handler.call(elem);
      });
    }
  }

  function getJSON(url, callback) {
    var request = new XMLHttpRequest();
    request.open('GET', url, true);
    request.onreadystatechange = function() {
      if (this.readyState === 4) {
        if (this.status >= 200 && this.status < 400) {
          // Success!
          var data = JSON.parse(this.responseText);
          callback(data);
        } else {
          console.error("Fetch failed!");
        }
      }
    };
    request.send();
    request = null;
  }

  function save_cookies () {
    // Expires in one year
    var expire_str = ";expires=" + new Date(Date.now() + 31536e6).toUTCString();
    document.cookie = "from=" + encodeURIComponent($from.getText()) + expire_str;
    document.cookie = "to=" + encodeURIComponent($to.getText()) + expire_str;
    var $selected = $('.when-button.selected');
    var value = is_defined($selected) ? encodeURIComponent($('.when-button.selected').getAttribute('value')) : "";
    document.cookie = "when=" + value + expire_str;
  }

  function get_cookie(name) {
    var regex = new RegExp('(?:(?:^|.*;\\s*)' + name + '\\s*\\=\\s*([^;]*).*$)|^.*$');
    return decodeURIComponent(document.cookie.replace(regex, "$1"));
  }

  function load_cookies () {
    var from_cookie = get_cookie("from");
    var to_cookie = get_cookie("to");
    var when_cookie = get_cookie("when");
    if (is_defined(from_cookie)) {
      $from.setText(from_cookie);
    }
    if (is_defined(to_cookie)) {
      $to.setText(to_cookie);
    }
    if (is_defined(when_cookie)) {
      var $elem = $('.when-button[value="' + when_cookie + '"]');
      if (is_defined($elem)) {
        addClass($elem, 'selected');
      }
    }
  }

  // now in seconds since the midnight
  function now () {
    var date = new Date();
    return date.getHours() * 60 * 60 +
           date.getMinutes() * 60 +
           date.getSeconds();
  }

  // now date in format YYYYMMDD
  function formatDate (d) {
    // getMonth starts from 0
    return parseInt([d.getFullYear(), d.getMonth() + 1, d.getDate()].map(function(n){
      return rjustString(n.toString(), 2, '0');
    }).join(''));
  }

  function second2str (seconds) {
    var minutes = Math.floor(seconds / 60);
    return [
      Math.floor(minutes / 60) % 24,
      minutes % 60
    ].map(function(item) {
      return rjustString(item.toString(), 2, '0');
    }).join(':');
  }

  function time_relative (from, to) {
    return Math.round((to - from) / 60); // in minute
  }

  function is_now () {
    var $selected = $('.when-button.selected');
    var value = is_defined($selected) ? encodeURIComponent($('.when-button.selected').getAttribute('value')) : "";
    return value === "now";
  }

  var DAY_OF_WEEK_MAP = {
    weekday: 1,
    saturday: 6,
    sunday: 0,
  };

  function get_service_ids (calendar, calendar_dates) {
    var $selected = $('.when-button.selected');
    var target_schedule = is_defined($selected) ? encodeURIComponent($('.when-button.selected').getAttribute('value')) : "";
    var target_date = new Date();
    var today_day_of_week = new Date().getDay(); // getDay is "0 for Sunday"

    if (target_schedule === 'now') {
      // when it's now, keep today's date and migrate target_schedule to real one
      switch (today_day_of_week) {
        case 1: case 2: case 3: case 4: case 5: target_schedule = 'weekday'; break;
        case 6: target_schedule = 'saturday'; break;
        case 0: target_schedule = 'sunday'; break;
        default: console.error('Unknown current day', today_day_of_week); return [];
      }
    } else {
      // when it's not, keep the schedule and migrate date to the next date matching the schedule
      var diff = (DAY_OF_WEEK_MAP[target_schedule] + 7 - today_day_of_week) % 7;
      target_date.setDate(target_date.getDate() + diff);
    }

    var date_str = formatDate(target_date);

    // calendar:
    //   service_id => [monday,tuesday,wednesday,thursday,friday,saturday,sunday,start_date,end_date]
    // calendar_dates:
    //   service_id => [date,exception_type]
    var service_ids = Object.keys(calendar).filter(function(service_id) {
      // check calendar start/end dates
      var item = calendar[service_id];
      return (item.start_date <= date_str) && (date_str <= item.end_date);
    }).filter(function(service_id) {
      // check calendar available days
      return calendar[service_id][target_schedule];
    });

    // consider exceptional days like holidays defined in calendar_dates file
    service_ids = service_ids.filter(function(service_id) {
      // check calendar_dates with exception_type 2 (if any to remove)
      return !(service_id in calendar_dates) ||
        calendar_dates[service_id].filter(function(exception_date) {
          return (exception_date[0] === date_str) && (exception_date[1] === 2);
        }).length === 0;
    }).concat(Object.keys(calendar_dates).filter(function(service_id) {
      // check calendar_dates with exception_type 1 (if any to add)
      return calendar_dates[service_id].filter(function(exception_date) {
        return (exception_date[0] === date_str) && (exception_date[1] === 1);
      }).length !== 0;
    }));

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
        extendObject(availables[route_id], trips);
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
    var info = removeAllChildren($("#info"));
    if (is_now() && is_defined(next_train)) {
      var next_relative = time_relative(now(), next_train.departure_time);
      var $div = createElement('div', {className: 'info', textContent: 'Next train: ' + next_relative + 'min'});
      info.appendChild($div);
    }
  }

  function render_result (trips) {
    var result = removeAllChildren($("#result"));
    trips.forEach(function(trip) {
      var $departure = createElement('span', {className: 'departure', textContent: second2str(trip.departure_time)});
      var $duration = createElement('span', {className: 'duration', textContent: time_relative(trip.departure_time, trip.arrival_time) + ' min'});
      var $arrival = createElement('span', {className: 'arrival', textContent: second2str(trip.arrival_time)});
      var $div = createElement('div', {className: 'trip'});
      $div.appendChild($departure);
      $div.appendChild($duration);
      $div.appendChild($arrival);
      result.appendChild($div);
    });
  }

  function schedule () {
    var stops = data.stops, routes = data.routes,
        calendar = data.calendar, calendar_dates = data.calendar_dates;
    var from_ids = stops[$from.getText()],
        to_ids = stops[$to.getText()],
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
    [$from, $to].forEach(function(c) {
      // when focus, reset input
      c.on("focus", function() {
        c.setText('');
        c.input.Show();
      });
      // when change or complete, schedule
      c.on("change", schedule);
      c.on("complete", schedule);
    });

    $whenList.forEach(function($elem) {
      bindEvent($elem, "click", function() {
        $whenList.forEach(function($elem) {
          removeClass($elem, "selected");
        });
        addClass($elem, "selected");
        schedule();
      });
    });

    bindEvent($("#reverse"), "click", function() {
      var t = $from.getText();
      $from.setText($to.getText());
      $to.setText(t);
      schedule();
    });
  }

  function initialize () {
    // remove 300ms delay for mobiles click
    FastClick.attach(document.body);

    // init inputs elements
    $from = rComplete($('#from'), { placeholder: "Departure" });
    $to = rComplete($('#to'), { placeholder: "Destination" });
    $whenList = findAll('.when-button');

    // generate select options
    var names = Object.keys(data.stops);
    $from.setOptions(names);
    $to.setOptions(names);

    // init
    load_cookies();
    bind_events();
    schedule(); // init schedule

    // Trigger test
    if (window.location.search === '?test=true') {
      test();
    }
  }

  function fetch_data(name_to_path, callback) {
    var data = {};
    Object.keys(name_to_path).forEach(function(name) { data[name] = undefined; });
    Object.keys(name_to_path).forEach(function(name) {
      getJSON(name_to_path[name], function(json) {
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
    onDocumentReady(initialize);
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

        var $test_result = createElement('div', {id: 'test_result'});
        document.documentElement.appendChild($test_result);
        function assert(check, msg) {
          if (!check) {
            var $item = createElement('div', {className: "test_result_item"});
            msg.split("\n").forEach(function(line) {
              $item.appendChild(createElement('div', {textContent: line}));
            });
            $test_result.appendChild($item);
          }
          return check;
        }

        function fixTimeFormat(time_str) {
          var t = time_str.split(":");
          t[0] = t[0] % 24;
          return t.map(function(item) { return rjustString(item.toString(), 2, '0'); }).join(":");
        }

        function formatExpectTime(expect) {
          return "[" + fixTimeFormat(expect[0]) + "=>" + fixTimeFormat(expect[1]) + "]";
        }

        function formatActualTime(actual) {
          return "[" + actual[0] + "=>" + actual[1] + "]";
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

              var expects = [];
              if (assert(from_stops.length === to_stops.length,
                         "from_stops and to_stops have different length:" + from_name + "=>" + to_name)) {
                for (var k = from_stops.length - 1; k >= 0; k--) {
                  var from_stop = from_stops[k];
                  var to_stop = to_stops[k];
                  if (assert(from_stop.service_type === to_stop.service_type,
                             "from_stop and to_stop have different type: " +
                               "schedule:" + schedule_type + ", " +
                               from_name + "(" + from_stop.service_type + ")=>" +
                               to_name + "(" + to_stop.service_type + ")" +
                               "[" + from_stop.time + "=>" + to_stop.time + "]")) {

                    var service_type = from_stop.service_type;
                    if (service_type === 'SatOnly' && schedule_type !== 'saturday') {
                      continue;
                    }
                    if (from_stop.time && to_stop.time) {
                      // since the loop is reversed, insert to first position
                      expects.unshift([from_stop.time, to_stop.time]);
                    }
                  }
                }
              }

              // sort by "depature_time=>arrival_time"
              expects.sort(function(a, b) {
                a = a[0] + "=>" + a[1];
                b = b[0] + "=>" + b[1];
                if (a < b) { return -1; }
                if (a > b) { return 1; }
                return 0;
              });

              var actuals = [];
              for (var l = $result.children.length - 1; l >= 0; l--) {
                var result = $result.children[l];
                actuals.unshift([result.children[0].textContent, result.children[2].textContent]);
              }

              if (assert(expects.length === actuals.length,
                     "expects and actuals have different length:" + from_name + "=>" + to_name +
                     "\nexpects:" + expects.map(formatExpectTime).join(", ") +
                     "\nactuals:" + actuals.map(formatActualTime).join(", "))) {

                for (var m = actuals.length - 1; m >= 0; m--) {
                  var expect_from_text = fixTimeFormat(expects[m][0]);
                  var expect_to_text = fixTimeFormat(expects[m][1]);
                  assert(actuals[m][0] === expect_from_text && actuals[m][1] === expect_to_text,
                         "time mismatch: schedule:" + schedule_type + ", " +
                           from_name + "=>" + to_name +
                           ", expected:(" + expect_from_text + " => " + expect_to_text +
                           "), actual:(" + actuals[m][0] + " => " + actuals[m][1] + ")");
                }
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

        var $total = createElement('div', {textContent: "Total failed:" + $test_result.children.length});
        $test_result.insertBefore($total, $test_result.firstChild);
        console.debug('Finish testing');
      });
    })($from, $to, $whenList, $('#result'));
  }
}());
