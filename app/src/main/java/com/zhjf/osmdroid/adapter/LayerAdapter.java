package com.zhjf.osmdroid.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.zhjf.osmdroid.R;
import com.zhjf.osmdroid.overlay.TileLayer;
import com.zhjf.osmdroid.overlay.VectorLayer;

import org.osmdroid.views.overlay.Overlay;

import java.util.HashMap;
import java.util.List;

/**
 * Created by Administrator on 2017/11/17.
 */

public class LayerAdapter extends BaseAdapter {
    private VisibleChangeCallback visibleChangeCallback;
    private List<Overlay> overlays;
    private LayoutInflater inflater;

    private HashMap<String, Boolean> states = new HashMap<String, Boolean>();//记录所有radiobutton被点击的状态

    public LayerAdapter(List<Overlay> overlays, Context context, VisibleChangeCallback visibleChangeCallback) {
        this.overlays = overlays;
        this.inflater = LayoutInflater.from(context);
        this.visibleChangeCallback = visibleChangeCallback;
        for (int i = 0; i < overlays.size(); i++)
            states.put(String.valueOf(i), false);
    }

    @Override
    public int getCount() {
        return overlays == null ? 0 : overlays.size();
    }

    @Override
    public Object getItem(int position) {
        return overlays.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        //加载布局为一个视图
        View view = inflater.inflate(R.layout.layout_layer_item, null);
        Overlay overlay = (Overlay) getItem(position);
        if (overlay instanceof TileLayer) {
            TileLayer tileLayer = (TileLayer) overlay;
            ((TextView) view.findViewById(R.id.layer_item_layername)).setText(tileLayer.getTilePath().getName());
            ((TextView) view.findViewById(R.id.layer_item_layertype)).setText(tileLayer.getTilePath().getName());
            setIsVisible((CheckBox) view.findViewById(R.id.layer_item_visible), tileLayer.isEnabled(), tileLayer, visibleChangeCallback);
            setIsEnable((CheckBox) view.findViewById(R.id.layer_item_edit), false);
        } else if (overlay instanceof VectorLayer) {
            VectorLayer vectorLayer = (VectorLayer) overlay;
            if (!vectorLayer.getName().equals("temp")) {
                ((TextView) view.findViewById(R.id.layer_item_layername)).setText(vectorLayer.getName());
                ((TextView) view.findViewById(R.id.layer_item_layertype)).setText(vectorLayer.getGeometryType().getName());
                setIsVisible((CheckBox) view.findViewById(R.id.layer_item_visible), vectorLayer.isEnabled(), vectorLayer, visibleChangeCallback);
                setIsEdit((CheckBox) view.findViewById(R.id.layer_item_edit), vectorLayer, view, position, visibleChangeCallback);
            }
        }
        boolean res = false;
        //判断当前位置的radiobutton点击状态
        if (getStates(position) == null || getStates(position) == false) {
            res = false;
            setStates(position, false);
        } else {
            res = true;
        }
        return view;
    }

    //用于在activity中重置所有的radiobutton的状态
    public void clearStates(int position, boolean isChecked) {
        // 重置，确保最多只有一项被选中
        for (String key : states.keySet()) {
            states.put(key, false);
        }
        states.put(String.valueOf(position), isChecked);
    }

    //设置状态值
    public void setStates(int position, boolean isChecked) {
        states.put(String.valueOf(position), false);
    }

    //用于获取状态值
    public Boolean getStates(int position) {
        return states.get(String.valueOf(position));
    }

    public void setIsVisible(CheckBox checkBox, boolean visible, final Overlay layer, final VisibleChangeCallback visibleChangeCallback) {
        checkBox.setChecked(visible);
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (layer instanceof TileLayer)
                    visibleChangeCallback.setVisible(layer, isChecked);
                if (layer instanceof VectorLayer)
                    visibleChangeCallback.setVisible(layer, isChecked);
            }
        });
    }

    public void setIsEdit(final CheckBox checkBox, final Overlay layer, final View view, final int position, final VisibleChangeCallback visibleChangeCallback) {
        checkBox.setChecked(getStates(position));
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (layer instanceof VectorLayer) {
                    transfer(view, position, isChecked);
                    visibleChangeCallback.setEditable(layer, isChecked);
                }
            }
        });
    }

    private void transfer(View view, int position, boolean isChecked) {
        CheckBox checkBox = (CheckBox) view.findViewById(R.id.layer_item_edit);
        //每次选择一个item时都要清除所有的状态，防止出现多个被选中
        clearStates(position, isChecked);
        checkBox.setChecked(getStates(position));
        //刷新数据，调用getView刷新ListView
        this.notifyDataSetChanged();

    }

    public void setIsEnable(CheckBox checkBox, boolean b) {
        checkBox.setEnabled(b);
    }

    public interface VisibleChangeCallback {
        void setVisible(Overlay name, Boolean isVisible);

        void setEditable(Overlay name, Boolean isEditable);
    }
}
