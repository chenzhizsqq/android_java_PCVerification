package com.dean.convenient.ui.view.navigation;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.dean.convenient.ui.R;
import com.dean.convenient.ui.view.navigation.model.NavigationItemModel;

import java.util.List;

/**
 * Created by dean on 2017/4/11.
 */
public class NavigationListAdapter extends BaseAdapter {

    private Context context;
    private List<NavigationItemModel> navigationItemModels;

    public NavigationListAdapter(Context context, List<NavigationItemModel> navigationItemModels) {
        this.context = context;
        this.navigationItemModels = navigationItemModels;
    }

    @Override
    public int getCount() {
        return navigationItemModels == null ? 0 : navigationItemModels.size();
    }

    @Override
    public Object getItem(int position) {
        return navigationItemModels.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        NavigationItemModel navigationItemModel = navigationItemModels.get(position);

        ViewHolder viewHolder;

        if (convertView == null || convertView.getTag() == null) {
            viewHolder = new ViewHolder();
            convertView = LayoutInflater.from(context).inflate(R.layout.adapter_navigation_item, null);

            viewHolder.imageView = (ImageView) convertView.findViewById(R.id.imageView);
            viewHolder.intentImageView = (ImageView) convertView.findViewById(R.id.intentImageView);
            viewHolder.nameTextView = (TextView) convertView.findViewById(R.id.nameTextView);

            convertView.setTag(viewHolder);
        } else
            viewHolder = (ViewHolder) convertView.getTag();

        viewHolder.imageView.setImageResource(navigationItemModel.getResId());
        viewHolder.nameTextView.setText(navigationItemModel.getName());
        final Intent intent = navigationItemModel.getIntent();
        if (intent != null) {
            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    context.startActivity(intent);
                }
            });
        }

        return convertView;
    }

    class ViewHolder {
        ImageView imageView, intentImageView;
        TextView nameTextView;
    }

}
