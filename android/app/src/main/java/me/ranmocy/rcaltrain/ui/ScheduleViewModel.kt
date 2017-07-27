package me.ranmocy.rcaltrain.ui

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Transformations
import android.arch.lifecycle.ViewModel
import android.content.Context
import me.ranmocy.rcaltrain.database.ScheduleDao
import me.ranmocy.rcaltrain.database.ScheduleDatabase
import me.ranmocy.rcaltrain.models.ScheduleResult

class ScheduleViewModel : ViewModel() {

    data class Inputs(val context: Context, val departure: String, val destination: String, @ScheduleDao.ServiceType val serviceType: Int)

    private val lastInputs = MutableLiveData<Inputs>()

    fun updateQuery(context: Context, departure: String, destination: String, @ScheduleDao.ServiceType serviceType: Int): LiveData<List<ScheduleResult>> {
        val v = lastInputs.value
        if (v == null || departure != v.departure || destination != v.destination || serviceType != v.serviceType) {
            lastInputs.value = Inputs(context.applicationContext, departure, destination, serviceType)
        }
        return results
    }

    val results: LiveData<List<ScheduleResult>> =
            Transformations.switchMap(lastInputs) { (context, departure, destination, serviceType) ->
                ScheduleDatabase.get(context).getResults(departure, destination, serviceType)
            }

    private var stationNames: LiveData<List<String>>? = null

    fun getStations(context: Context): LiveData<List<String>> {
        if (stationNames == null) {
            stationNames = ScheduleDatabase.get(context).stationNames
        }
        return stationNames!!
    }
}
