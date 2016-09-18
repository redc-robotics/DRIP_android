package io.github.funkynoodles.testsensor.adapter;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import io.github.funkynoodles.testsensor.R;
import io.github.funkynoodles.testsensor.model.DrawerItem;

/**
 * Created by Louis on 9/13/2016.
 * Adapter for drawer
 */
public class DrawerListAdapter extends BaseAdapter{

    private Context context;
    private ArrayList<DrawerItem> drawerItems;

    public DrawerListAdapter(Context context, ArrayList<DrawerItem> drawerItems){
        this.context = context;
        this.drawerItems = drawerItems;
    }

    @Override
    public int getCount() {
        return drawerItems.size();
    }

    @Override
    public Object getItem(int position) {
        return drawerItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater mInflater = (LayoutInflater)
                    context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            convertView = mInflater.inflate(R.layout.drawer_list_item, null);
        }

        TextView txtTitle = (TextView) convertView.findViewById(R.id.title);

        txtTitle.setText(drawerItems.get(position).getTitle());

        // displaying count
        // check whether it set visible or not
        /*if(drawerItems.get(position).getCounterVisibility()){
            txtCount.setText(drawerItems.get(position).getCount());
        }else{
            // hide the counter view
            txtCount.setVisibility(View.GONE);
        }*/

        return convertView;
    }
}
