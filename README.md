rCaltrain - [A Better Caltrain Timetable](http://rcaltrain.com/)
=========

<p align="left">
  <a href='https://travis-ci.org/ranmocy/rCaltrain'>
    <img src='https://travis-ci.org/ranmocy/rCaltrain.svg?branch=master'/>
  </a>
  <a href='https://play.google.com/store/apps/details?id=me.ranmocy.rcaltrain'>
    <img height="30px" alt='Get it on Google Play' src='play_store.png'/>
  </a>
</p>

Calculation data are from [Caltrain Developer](http://www.caltrain.com/developer.html).

Calculation results are tested against [Caltrain Weekday Timetable](http://www.caltrain.com/schedules/weekdaytimetable.html) and [Caltrain Weekend Timetable](http://www.caltrain.com/schedules/weekend-timetable.html).

# Screenshot

<p align="center">
  <img width="300" src="design/screenshot.png?raw=true" title="Screenshot" alt="Screenshot" />
</p>

# Schedules

Regarding your selected departure and destination stations:

1. **Now** shows all services available today since now. This option considers holidays or any exceptional day defined in the official timetable data.
2. **Weekday** shows all services available on weekdays of current week.
3. **Saturday** shows all services available at Saturday of current week.
4. **Sunday** shows all services available at Sunday of current week.

# Privacy

This app doesn't require any special permission, and it doesn't access to any user's data.

# Dev Dependencies

* [UglifyCSS](https://github.com/fmarcia/UglifyCSS): `npm install uglifycss -g`
* [UglifyJS2](https://github.com/mishoo/UglifyJS2): `npm install uglify-js -g`
* [PhantomJS](http://phantomjs.org/): `brew install phantomjs`
* Ruby gems: `bundle`

# Size

HTML: 2.0KB
CSS: 1.4KB
JS: 9.1KB
JSON: 14.358KB
PNG: 86.951KB
Total: 113.751KB

As of [2017-04-15 22:04:33 -0700](https://github.com/ranmocy/rCaltrain/commit/a10b1b714501630cb3f1b2bbea02ec176e8d8ca4)

# TODO

1. Add test for Android version
2. Fix iOS version for new data starts from 2016/04/04
