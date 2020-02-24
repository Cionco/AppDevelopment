package de.fs.fintech.geogame.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import de.fs.fintech.geogame.R;
import de.fs.fintech.geogame.parcelable.LeaderBoardAlphaParcel;

/**
 * Created by nicolaskepper on 15.06.17.
 */

public class LeaderBoardAlphaListAdapter extends ArrayAdapter<LeaderBoardAlphaParcel> {

    private static class ViewHolder {
        private TextView itemViewName;
        private TextView itemViewScore;
    }

    public LeaderBoardAlphaListAdapter(Context context, int textViewResourceId, ArrayList<LeaderBoardAlphaParcel> items) {
        super(context, textViewResourceId, items);
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if(convertView == null) {
            convertView = LayoutInflater.from(this.getContext())
                    .inflate(R.layout.lvi_leaderboard, parent, false);

            viewHolder = new ViewHolder();
            viewHolder.itemViewName = (TextView) convertView.findViewById(R.id.textName);
            viewHolder.itemViewScore = (TextView) convertView.findViewById(R.id.textScore);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        LeaderBoardAlphaParcel item = getItem(position);

        if(item != null) {
            viewHolder.itemViewName.setText(item.name);
            viewHolder.itemViewScore.setText(Integer.toString(item.score));
        }

        return convertView;
    }
}
