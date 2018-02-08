package com.v.smartassistant.sidescreen.GridUtils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.v.smartassistant.sidescreen.R;

import org.askerov.dynamicgrid.BaseDynamicGridAdapter;

import java.util.List;


public class CheeseDynamicAdapter extends BaseDynamicGridAdapter {
    public Context mContext;

    public CheeseDynamicAdapter(Context context, List<?> items, int columnCount) {
        super(context, items, columnCount);
        mContext = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        CheeseViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_grid, null);
            holder = new CheeseViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (CheeseViewHolder) convertView.getTag();
        }
        LabelInfo info = (LabelInfo) getItem(position);
        holder.mTitle.setText(info.getTitle());
        holder.mImage.setImageResource(info.getDrawable());
        return convertView;
    }

    private class CheeseViewHolder {
        private TextView mTitle;
        private ImageView mImage;

        private CheeseViewHolder(View view) {
            mTitle = view.findViewById(R.id.item_title);
            mImage = view.findViewById(R.id.item_img);
        }
    }

}