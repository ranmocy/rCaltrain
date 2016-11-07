package me.ranmocy.rcaltrain.ui;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import me.ranmocy.rcaltrain.R;
import me.ranmocy.rcaltrain.Scheduler;
import me.ranmocy.rcaltrain.models.DayTime;
import me.ranmocy.rcaltrain.models.ScheduleResult;
import me.ranmocy.rcaltrain.models.ScheduleType;
import me.ranmocy.rcaltrain.models.Service;
import me.ranmocy.rcaltrain.models.Station;
import me.ranmocy.rcaltrain.models.Trip;

/**
 * ListAdapter that shows scheduling result.
 */
public class ResultsListAdapter extends BaseAdapter implements ListAdapter {

    private final LayoutInflater layoutInflater;
    private final List<ScheduleResult> resultList = new ArrayList<>();

    public ResultsListAdapter(Context context) {
        this.layoutInflater = LayoutInflater.from(context);
    }

    public void setData(String fromName, String toName, ScheduleType scheduleType) {
        resultList.clear();
        resultList.addAll(Scheduler.schedule(fromName, toName, scheduleType));
        notifyDataSetChanged();
    }

    public String getNextTime() {
        if (resultList.isEmpty()) {
            return "Oops, no train for today!";
        }
        long nextTrainInMinutes = DayTime.now().toInMinutes(resultList.get(0).getDepartureTime());
        return String.format(Locale.getDefault(), "Next train in %d min", nextTrainInMinutes);
    }

    @Override
    public int getCount() {
        return resultList.size();
    }

    @Override
    public ScheduleResult getItem(int position) {
        return resultList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = layoutInflater.inflate(R.layout.result_item, parent, false);
            holder = new ViewHolder();
            holder.departureView = (TextView) convertView.findViewById(R.id.departure_time);
            holder.arrivalView = (TextView) convertView.findViewById(R.id.arrival_time);
            holder.intervalView = (TextView) convertView.findViewById(R.id.interval_time);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        ScheduleResult result = getItem(position);
        holder.departureView.setText(result.getDepartureTimeString());
        holder.arrivalView.setText(result.getArrivalTimeString());
        holder.intervalView.setText(result.getIntervalTimeString());
        return convertView;
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    private static class ViewHolder {
        TextView departureView;
        TextView arrivalView;
        TextView intervalView;
    }

}
