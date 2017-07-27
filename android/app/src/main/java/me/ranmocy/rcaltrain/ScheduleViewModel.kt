package me.ranmocy.rcaltrain

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.content.Context
import me.ranmocy.rcaltrain.database.ScheduleDao
import me.ranmocy.rcaltrain.database.ScheduleDatabase
import me.ranmocy.rcaltrain.models.ScheduleResult

class ScheduleViewModel : ViewModel() {

    private var results: LiveData<List<ScheduleResult>> = MutableLiveData<List<ScheduleResult>>()
    private var lastDeparture: String? = null
    private var lastDestination: String? = null
    @ScheduleDao.ServiceType private var lastServiceType: Int? = null

    fun getResults(context: Context, departure: String, destination: String, @ScheduleDao.ServiceType serviceType: Int): LiveData<List<ScheduleResult>> {
        if (departure != lastDeparture || destination != lastDestination || serviceType != lastServiceType) {
            lastDeparture = departure
            lastDestination = destination
            lastServiceType = serviceType
            results = ScheduleDatabase.get(context).getResults(departure, destination, serviceType)
        }
        return results
    }
}
