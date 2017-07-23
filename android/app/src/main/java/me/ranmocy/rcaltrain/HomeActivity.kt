package me.ranmocy.rcaltrain

import android.os.Bundle
import android.support.annotation.VisibleForTesting
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ListView
import android.widget.RadioGroup
import android.widget.TextView
import com.google.firebase.analytics.FirebaseAnalytics
import me.ranmocy.rcaltrain.models.ScheduleType
import me.ranmocy.rcaltrain.models.Station
import me.ranmocy.rcaltrain.ui.ResultsListAdapter
import me.ranmocy.rcaltrain.ui.StationListAdapter

class HomeActivity : AppCompatActivity(), View.OnClickListener {

    companion object {
        private val TAG = "HomeActivity"
    }

    private val preferences: Preferences by lazy { Preferences(this) }
    private val departureView: TextView by lazy { findViewById(R.id.input_departure) as TextView }
    private val arrivalView: TextView by lazy { findViewById(R.id.input_arrival) as TextView }
    private val scheduleGroup: RadioGroup by lazy { findViewById(R.id.schedule_group) as RadioGroup }
    private val nextTrainView: TextView by lazy { findViewById(R.id.next_train) as TextView }
    private val resultsAdapter: ResultsListAdapter by lazy { ResultsListAdapter(this) }
    private val firebaseAnalytics: FirebaseAnalytics by lazy { FirebaseAnalytics.getInstance(this) }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_home)
        //        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        //        setSupportActionBar(toolbar);

        // Setup result view
        val resultsView = findViewById(R.id.results) as ListView
        resultsView.adapter = resultsAdapter

        // TODO: check saved station name, invalid it if it's not in our list.
        departureView.text = preferences.lastDepartureStationName
        arrivalView.text = preferences.lastDestinationStationName
        when (preferences.lastScheduleType) {
            ScheduleType.NOW -> scheduleGroup.check(R.id.btn_now)
            ScheduleType.WEEKDAY -> scheduleGroup.check(R.id.btn_week)
            ScheduleType.SATURDAY -> scheduleGroup.check(R.id.btn_sat)
            ScheduleType.SUNDAY -> scheduleGroup.check(R.id.btn_sun)
        }

        // Init schedule
        reschedule()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_home, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId


        if (id == R.id.action_settings) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onClick(v: View) {
        Log.v(TAG, "onClick:" + v)
        when (v.id) {
            R.id.input_departure -> {
                showStationSelector(true)
                firebaseAnalytics.logEvent(Events.EVENT_ON_CLICK, Events.clickDepartureEvent)
                return
            }
            R.id.input_arrival -> {
                showStationSelector(false)
                firebaseAnalytics.logEvent(Events.EVENT_ON_CLICK, Events.clickArrivalEvent)
                return
            }
            R.id.switch_btn -> {
                val departureViewText = departureView.text
                val arrivalViewText = arrivalView.text
                departureView.text = arrivalViewText
                arrivalView.text = departureViewText
                preferences.lastDepartureStationName = departureViewText.toString()
                preferences.lastDestinationStationName = arrivalViewText.toString()
                firebaseAnalytics.logEvent(Events.EVENT_ON_CLICK, Events.clickSwitchEvent)
            }
            R.id.btn_now -> {
                preferences.lastScheduleType = ScheduleType.NOW
                firebaseAnalytics.logEvent(Events.EVENT_ON_CLICK, Events.getClickScheduleEvent(ScheduleType.NOW))
            }
            R.id.btn_week -> {
                preferences.lastScheduleType = ScheduleType.WEEKDAY
                firebaseAnalytics.logEvent(Events.EVENT_ON_CLICK, Events.getClickScheduleEvent(ScheduleType.WEEKDAY))
            }
            R.id.btn_sat -> {
                preferences.lastScheduleType = ScheduleType.SATURDAY
                firebaseAnalytics.logEvent(Events.EVENT_ON_CLICK, Events.getClickScheduleEvent(ScheduleType.SATURDAY))
            }
            R.id.btn_sun -> {
                preferences.lastScheduleType = ScheduleType.SUNDAY
                firebaseAnalytics.logEvent(Events.EVENT_ON_CLICK, Events.getClickScheduleEvent(ScheduleType.SUNDAY))
            }
            R.id.schedule_group -> {
                Log.v(TAG, "schedule_group")
                return
            }
            else -> return
        }
        reschedule()
    }

    private fun showStationSelector(isDeparture: Boolean) {
        AlertDialog.Builder(this)
                .setAdapter(StationListAdapter(this)) { dialog, which ->
                    val stationName = Station.allStations[which].name
                    if (isDeparture) {
                        preferences.lastDepartureStationName = stationName
                        departureView.text = stationName
                    } else {
                        preferences.lastDestinationStationName = stationName
                        arrivalView.text = stationName
                    }
                    dialog.dismiss()
                    reschedule()
                }
                .setCancelable(false)
                .show()
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun reschedule() {
        val departure = departureView.text.toString()
        val destination = arrivalView.text.toString()

        // reschedule, update results data
        val scheduleType: ScheduleType
        when (scheduleGroup.checkedRadioButtonId) {
            -1 -> {
                Log.v(TAG, "No schedule selected, skip.")
                return
            }
            R.id.btn_now -> scheduleType = ScheduleType.NOW
            R.id.btn_week -> scheduleType = ScheduleType.WEEKDAY
            R.id.btn_sat -> scheduleType = ScheduleType.SATURDAY
            R.id.btn_sun -> scheduleType = ScheduleType.SUNDAY
            else -> throw RuntimeException("Unexpected schedule selection:" + scheduleGroup.checkedRadioButtonId)
        }

        firebaseAnalytics.logEvent(
                Events.EVENT_SCHEDULE,
                Events.getScheduleEvent(departure, destination, scheduleType))

        resultsAdapter.setData(departure, destination, scheduleType)
        if (scheduleGroup.checkedRadioButtonId == R.id.btn_now) {
            nextTrainView.text = resultsAdapter.nextTime
            nextTrainView.visibility = View.VISIBLE
        } else {
            nextTrainView.visibility = View.GONE
        }
    }
}
