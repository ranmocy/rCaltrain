package me.ranmocy.rcaltrain.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ListAdapter
import android.widget.TextView
import me.ranmocy.rcaltrain.R
import me.ranmocy.rcaltrain.models.DayTime
import me.ranmocy.rcaltrain.models.ScheduleResult
import java.util.*

/** ListAdapter that shows scheduling result. */
class ResultsListAdapter(context: Context) : BaseAdapter(), ListAdapter {

    private val layoutInflater: LayoutInflater = LayoutInflater.from(context)
    private val resultList = ArrayList<ScheduleResult>()

    fun setData(results: List<ScheduleResult>) {
        resultList.clear()
        resultList.addAll(results)
        notifyDataSetChanged()
    }

    val nextTime: String
        get() {
            if (resultList.isEmpty()) {
                return "Oops, no train for today!"
            }
            val nextTrainInMinutes = DayTime.now().toInMinutes(resultList[0].departureTime)
            return String.format(Locale.getDefault(), "Next train in %d min", nextTrainInMinutes)
        }

    override fun getCount(): Int {
        return resultList.size
    }

    override fun getItem(position: Int): ScheduleResult {
        return resultList[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun hasStableIds(): Boolean {
        return false
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View
        val holder: ViewHolder
        if (convertView == null) {
            view = layoutInflater.inflate(R.layout.result_item, parent, false)!!
            holder = ViewHolder()
            holder.departureView = view.findViewById<TextView>(R.id.departure_time)
            holder.arrivalView = view.findViewById<TextView>(R.id.arrival_time)
            holder.intervalView = view.findViewById<TextView>(R.id.interval_time)
            view.tag = holder
        } else {
            view = convertView
            holder = view.tag as ViewHolder
        }
        val result = getItem(position)
        holder.departureView!!.text = result.departureTime.toString()
        holder.arrivalView!!.text = result.arrivalTime.toString()
        holder.intervalView!!.text = result.intervalTimeString
        return view
    }

    override fun getItemViewType(position: Int): Int {
        return 0
    }

    override fun getViewTypeCount(): Int {
        return 1
    }

    private class ViewHolder {
        internal var departureView: TextView? = null
        internal var arrivalView: TextView? = null
        internal var intervalView: TextView? = null
    }

}
