package com.zhjf.osmdroid.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.zhjf.osmdroid.R;
import com.zhjf.osmdroid.entity.AttributeEntity;

import java.util.List;

/**
 * Created by Administrator on 2017/11/22.
 */

public class AttributeListAdapter extends BaseAdapter {
    private List<AttributeEntity> attributeEntities;
    private LayoutInflater inflater;


    public AttributeListAdapter(Context context, List<AttributeEntity> attributeEntities) {
        this.attributeEntities = attributeEntities;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return attributeEntities.size();
    }

    @Override
    public Object getItem(int i) {
        return attributeEntities.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View convertView, ViewGroup viewGroup) {
        View view = inflater.inflate(R.layout.layout_attribute_item, null);
        ((TextView) view.findViewById(R.id.attribute_item_name)).setText(attributeEntities.get(i).getName());
        ((TextView) view.findViewById(R.id.attribute_item_type)).setText(attributeEntities.get(i).getType().getName());
        return view;
    }
}
