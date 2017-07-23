package me.ranmocy.rcaltrain.ui

import android.content.Context
import android.database.DataSetObserver
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListAdapter
import android.widget.TextView
import me.ranmocy.rcaltrain.R
import me.ranmocy.rcaltrain.models.Station
import java.util.*

/** Station list adapter. */
class StationListAdapter(context: Context) : ListAdapter {

    private val stations = Station.allStations
    private val observers = ArrayList<DataSetObserver>()
    private val layoutInflater: LayoutInflater = LayoutInflater.from(context)

    override fun areAllItemsEnabled(): Boolean {
        return true
    }

    override fun isEnabled(position: Int): Boolean {
        return true
    }

    override fun registerDataSetObserver(observer: DataSetObserver) {
        observers.add(observer)
    }

    override fun unregisterDataSetObserver(observer: DataSetObserver) {
        observers.remove(observer)
    }

    override fun getCount(): Int {
        return stations.size
    }

    override fun getItem(position: Int): Station {
        return stations[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun hasStableIds(): Boolean {
        return false
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: layoutInflater.inflate(R.layout.station_item, parent, false)
        (view as TextView).text = getItem(position).name
        return view
    }

    override fun getItemViewType(position: Int): Int {
        return 0
    }

    override fun getViewTypeCount(): Int {
        return 1
    }

    override fun isEmpty(): Boolean {
        return stations.isEmpty()
    }
}
