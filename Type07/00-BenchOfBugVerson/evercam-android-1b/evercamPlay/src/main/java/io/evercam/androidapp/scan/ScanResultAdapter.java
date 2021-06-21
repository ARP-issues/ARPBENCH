package io.evercam.androidapp.scan;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Locale;

import io.evercam.androidapp.R;
import io.evercam.androidapp.ScanActivity;
import io.evercam.network.discovery.DiscoveredCamera;

public class ScanResultAdapter extends ArrayAdapter<DiscoveredCamera>
{
    private final String TAG = "ScanResultAdapter";

    private ArrayList<DiscoveredCamera> cameras;
    private SparseArray<Drawable> drawableArray;

    public ScanResultAdapter(Context context, int resource, ArrayList<DiscoveredCamera> cameras,
                             SparseArray<Drawable> drawableArray)
    {
        super(context, resource, cameras);
        this.cameras = cameras;
        this.drawableArray = drawableArray;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        View view = convertView;
        if (view == null)
        {
            LayoutInflater layoutInflater = (LayoutInflater)getContext().getSystemService(Context
                    .LAYOUT_INFLATER_SERVICE);
            view = layoutInflater.inflate(R.layout.scan_list_layout, null);
        }
        DiscoveredCamera camera = cameras.get(position);
        if (camera != null)
        {
            ImageView thumbnailImageView = (ImageView) view.findViewById(R.id.camera_img);
            TextView ipTextView = (TextView) view.findViewById(R.id.camera_ip);
            TextView modelTextView = (TextView) view.findViewById(R.id.camera_model);
            TextView vendorLabel = (TextView) view.findViewById(R.id.label_vendor);
            TextView modelLabel = (TextView) view.findViewById(R.id.label_model);
            TextView httpLabel = (TextView) view.findViewById(R.id.label_http);
            TextView rtspLabel = (TextView) view.findViewById(R.id.label_rtsp);
            TextView evercamLabel = (TextView) view.findViewById(R.id.label_evercam);

            updateThumbnailImage(thumbnailImageView, position);
            updateIpAndPort(ipTextView, camera);
            updateVendorAndModel(modelTextView, camera);

            //Update label colors
            markLabelGreen(vendorLabel, camera.hasVendor());
            markLabelGreen(modelLabel, camera.hasModel());
            markLabelGreen(httpLabel, camera.hasHTTP());
            markLabelGreen(rtspLabel, camera.hasRTSP());
            markLabelGreen(evercamLabel, ScanActivity.isCameraAdded(camera));
        }
        return view;
    }

    private void updateThumbnailImage(ImageView imageView, int position)
    {
        if(drawableArray.size() > 0)
        {
            Drawable drawable = drawableArray.get(position);
            if(drawable != null)
            {
                imageView.setVisibility(View.VISIBLE);
                imageView.setImageDrawable(drawable);
            }
            else
            {
                imageView.setVisibility(View.INVISIBLE);
            }
        }
    }

    private void updateIpAndPort(TextView textView, DiscoveredCamera camera)
    {
        String ipTextShowing = camera.getIP();
        if(camera.hasHTTP())
        {
            ipTextShowing = ipTextShowing + ":" + camera.getHttp();
        }
        textView.setText(ipTextShowing);
    }

    private void updateVendorAndModel(TextView textView, DiscoveredCamera camera)
    {
        String vendor = camera.getVendor().toUpperCase(Locale.UK);

        if(camera.hasModel())
        {
            String model = camera.getModel().toUpperCase(Locale.UK);
            if(model.startsWith(vendor))
            {
                textView.setText(model);
            }
            else
            {
                textView.setText(vendor + " " + model);
            }
        }
        else
        {
            textView.setText(vendor);
        }
    }

    private void markLabelGreen(TextView textView, boolean isGreen)
    {
        int colorCode = isGreen ? android.R.color.holo_green_dark : android.R.color.darker_gray;
        textView.setTextColor(getContext().getResources().getColor(colorCode));
    }
}