package me.ranmocy.rcaltrain

import android.arch.lifecycle.LifecycleRegistry
import android.arch.lifecycle.LifecycleRegistryOwner
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
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
import me.ranmocy.rcaltrain.database.ScheduleDao
import me.ranmocy.rcaltrain.models.ScheduleResult
import me.ranmocy.rcaltrain.models.Station
import me.ranmocy.rcaltrain.ui.ResultsListAdapter
import me.ranmocy.rcaltrain.ui.StationListAdapter

class HomeActivity : AppCompatActivity(), View.OnClickListener, LifecycleRegistryOwner {

    companion object {
        private val TAG = "HomeActivity"
    }

    private val lifecycleRegistry = LifecycleRegistry(this)
    override fun getLifecycle(): LifecycleRegistry {
        return lifecycleRegistry
    }

    private val preferences: Preferences by lazy { Preferences(this) }
    private val departureView: TextView by lazy { findViewById<TextView>(R.id.input_departure) }
    private val arrivalView: TextView by lazy { findViewById<TextView>(R.id.input_arrival) }
    private val scheduleGroup: RadioGroup by lazy { findViewById<RadioGroup>(R.id.schedule_group) }
    private val nextTrainView: TextView by lazy { findViewById<TextView>(R.id.next_train) }
    private val resultsAdapter: ResultsListAdapter by lazy { ResultsListAdapter(this) }
    private val firebaseAnalytics: FirebaseAnalytics by lazy { FirebaseAnalytics.getInstance(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_home)
        //        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        //        setSupportActionBar(toolbar);

        // Setup result view
        val resultsView = findViewById<ListView>(R.id.results)
        resultsView.adapter = resultsAdapter

        // TODO: check saved station name, invalid it if it's not in our list.
        departureView.text = preferences.lastDepartureStationName
        arrivalView.text = preferences.lastDestinationStationName
        when (preferences.lastScheduleType) {
            ScheduleDao.SERVICE_NOW -> scheduleGroup.check(R.id.btn_now)
            ScheduleDao.SERVICE_WEEKDAY -> scheduleGroup.check(R.id.btn_week)
            ScheduleDao.SERVICE_SATURDAY -> scheduleGroup.check(R.id.btn_sat)
            ScheduleDao.SERVICE_SUNDAY -> scheduleGroup.check(R.id.btn_sun)
        }

        ViewModelProviders.of(this).get(ScheduleViewModel::class.java).results.observe(this, Observer { t ->
            val results = t ?: ArrayList<ScheduleResult>()
            resultsAdapter.setData(results)
        })

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
                preferences.lastScheduleType = ScheduleDao.SERVICE_NOW
                firebaseAnalytics.logEvent(Events.EVENT_ON_CLICK, Events.getClickScheduleEvent(ScheduleDao.SERVICE_NOW))
            }
            R.id.btn_week -> {
                preferences.lastScheduleType = ScheduleDao.SERVICE_WEEKDAY
                firebaseAnalytics.logEvent(Events.EVENT_ON_CLICK, Events.getClickScheduleEvent(ScheduleDao.SERVICE_WEEKDAY))
            }
            R.id.btn_sat -> {
                preferences.lastScheduleType = ScheduleDao.SERVICE_SATURDAY
                firebaseAnalytics.logEvent(Events.EVENT_ON_CLICK, Events.getClickScheduleEvent(ScheduleDao.SERVICE_SATURDAY))
            }
            R.id.btn_sun -> {
                preferences.lastScheduleType = ScheduleDao.SERVICE_SUNDAY
                firebaseAnalytics.logEvent(Events.EVENT_ON_CLICK, Events.getClickScheduleEvent(ScheduleDao.SERVICE_SUNDAY))
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
        if (!DataLoader.isLoaded()) {
            return
        }

        val departure = departureView.text.toString()
        val destination = arrivalView.text.toString()

        // reschedule, update results data
        @ScheduleDao.ServiceType val scheduleType: Int
        when (scheduleGroup.checkedRadioButtonId) {
            -1 -> {
                Log.v(TAG, "No schedule selected, skip.")
                return
            }
            R.id.btn_now -> scheduleType = ScheduleDao.SERVICE_NOW
            R.id.btn_week -> scheduleType = ScheduleDao.SERVICE_WEEKDAY
            R.id.btn_sat -> scheduleType = ScheduleDao.SERVICE_SATURDAY
            R.id.btn_sun -> scheduleType = ScheduleDao.SERVICE_SUNDAY
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
