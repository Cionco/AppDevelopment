package de.fs.fintech.geogame.adapter;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import de.fs.fintech.geogame.R;
import de.fs.fintech.geogame.data.PortalInfo;
import de.fs.fintech.geogame.parcelable.PortalInfoParcel;

public class PortalListAdapter extends ArrayAdapter<PortalInfoParcel> {

    private static class ViewHolder {
        private TextView itemViewTitle;
        private TextView itemViewDescription;
        private TextView itemViewDist;
    }

    public PortalListAdapter(Context context, int textViewResourceId, ArrayList<PortalInfoParcel> items) {
        super(context, textViewResourceId, items);
    }

    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = LayoutInflater.from(this.getContext())
                    .inflate(R.layout.lvi_portal, parent, false);

            viewHolder = new ViewHolder();
            viewHolder.itemViewTitle = (TextView) convertView.findViewById(R.id.textTitle);
            viewHolder.itemViewDescription = (TextView) convertView.findViewById(R.id.textDescription);
            viewHolder.itemViewDist = (TextView) convertView.findViewById(R.id.textDistance);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        PortalInfoParcel item = getItem(position);
        long distance=120-position; // TODO Position und Abstand zu Portal ermitteln

        if (item!= null) {
            viewHolder.itemViewTitle.setText(item.title);
            if(item.description==null || item.description.length()==0) {
                viewHolder.itemViewDescription.setVisibility(View.INVISIBLE);
            } else {
                viewHolder.itemViewDescription.setVisibility(View.VISIBLE);
                viewHolder.itemViewDescription.setText(item.description);
            }
            viewHolder.itemViewDist.setText(String.format("%d", distance));
        }

        return convertView;
    }
}
