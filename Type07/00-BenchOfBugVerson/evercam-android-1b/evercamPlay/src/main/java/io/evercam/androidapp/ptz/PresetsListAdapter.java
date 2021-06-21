package io.evercam.androidapp.ptz;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import io.evercam.PTZPreset;
import io.evercam.androidapp.R;

public class PresetsListAdapter extends ArrayAdapter<PTZPreset>
{
    private List<PTZPreset> presetList;

    public PresetsListAdapter(Context context, int resource, List<PTZPreset> presetList)
    {
        super(context, resource, presetList);
        this.presetList = presetList;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        View view = convertView;
        if (view == null)
        {
            LayoutInflater layoutInflater = (LayoutInflater)getContext().getSystemService(Context
                    .LAYOUT_INFLATER_SERVICE);
            view = layoutInflater.inflate(R.layout.preset_list_item_layout, null);
        }

        PTZPreset preset = presetList.get(position);
        TextView textView = (TextView) view.findViewById(R.id.preset_text_view);
        textView.setText(preset.getName());

        return view;
    }
}
