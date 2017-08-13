package me.ranmocy.rcaltrain.ui

import android.content.Context
import android.database.DataSetObserver
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ListAdapter
import android.widget.TextView
import me.ranmocy.rcaltrain.R
import java.util.*

/** Station list adapter. */
class StationListAdapter(context: Context) : BaseAdapter(), ListAdapter {

    private val stationNames = ArrayList<String>()
    private val observers = ArrayList<DataSetObserver>()
    private val layoutInflater: LayoutInflater = LayoutInflater.from(context)

    fun setData(data: List<String>) {
        stationNames.clear()
        stationNames.addAll(data)
        notifyDataSetChanged()
    }

    fun getData(): List<String> {
        return stationNames
    }

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
        return stationNames.size
    }

    override fun getItem(position: Int): String {
        return stationNames[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun hasStableIds(): Boolean {
        return false
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: layoutInflater.inflate(R.layout.station_item, parent, false)
        (view as TextView).text = getItem(position)
        return view
    }

    override fun getItemViewType(position: Int): Int {
        return 0
    }

    override fun getViewTypeCount(): Int {
        return 1
    }

    override fun isEmpty(): Boolean {
        return stationNames.isEmpty()
    }
}
