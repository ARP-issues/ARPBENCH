package io.evercam.androidapp;

import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import io.evercam.Auth;
import io.evercam.CameraBuilder;
import io.evercam.Defaults;
import io.evercam.EvercamException;
import io.evercam.Model;
import io.evercam.PatchCameraBuilder;
import io.evercam.Vendor;
import io.evercam.androidapp.custom.CustomToast;
import io.evercam.androidapp.custom.CustomedDialog;
import io.evercam.androidapp.dto.AppData;
import io.evercam.androidapp.dto.EvercamCamera;
import io.evercam.androidapp.tasks.AddCameraTask;
import io.evercam.androidapp.tasks.PatchCameraTask;
import io.evercam.androidapp.tasks.PortCheckTask;
import io.evercam.androidapp.tasks.TestSnapshotTask;
import io.evercam.androidapp.utils.Commons;
import io.evercam.androidapp.utils.Constants;
import io.evercam.androidapp.utils.DataCollector;
import io.evercam.androidapp.video.VideoActivity;
import io.evercam.network.discovery.DiscoveredCamera;

public class AddEditCameraActivity extends ParentAppCompatActivity
{
    private final String TAG = "AddEditCameraActivity";
    private LinearLayout cameraIdLayout;
    private TextView cameraIdTextView;
    private EditText cameraNameEdit;
    private Spinner vendorSpinner;
    private Spinner modelSpinner;
    private EditText usernameEdit;
    private EditText passwordEdit;
    private EditText externalHostEdit;
    private EditText externalHttpEdit;
    private EditText externalRtspEdit;
    private EditText jpgUrlEdit;
    private EditText rtspUrlEdit;
    private TextView mHttpStatusTextView;
    private TextView mRtspStatusTextView;
    private LinearLayout jpgUrlLayout;
    private LinearLayout rtspUrlLayout;
    private Button addEditButton;
    private TreeMap<String, String> vendorMap;
    private TreeMap<String, String> vendorMapIdAsKey;
    private TreeMap<String, String> modelMap;

    private DiscoveredCamera discoveredCamera;
    private EvercamCamera cameraEdit;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_add_camera);

        setUpDefaultToolbar();

        Bundle bundle = getIntent().getExtras();
        // Edit Camera
        if(bundle != null && bundle.containsKey(Constants.KEY_IS_EDIT))
        {
            EvercamPlayApplication.sendScreenAnalytics(this,
                    getString(R.string.screen_edit_camera));
            cameraEdit = VideoActivity.evercamCamera;

            updateTitleText(R.string.title_edit_camera);
        }
        else
        // Add Camera
        {
            EvercamPlayApplication.sendScreenAnalytics(this, getString(R.string.screen_add_camera));

            // Get camera object from video activity before initial screen
            discoveredCamera = (DiscoveredCamera) getIntent().getSerializableExtra("camera");
        }

        // Initial UI elements
        initialScreen();

        fillDiscoveredCameraDetails(discoveredCamera);

        if(cameraEdit == null)
        {
            //Populate name and IP only when adding camera
            autoPopulateCameraName();
            autoPopulateExternalIP();
        }

        fillEditCameraDetails(cameraEdit);
    }

    @Override
    public void onBackPressed()
    {
        showConfirmQuitIfAddingCamera();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch(item.getItemId())
        {
            case android.R.id.home:
                showConfirmQuitIfAddingCamera();
                return true;
        }
        return true;
    }

    private void showConfirmQuitIfAddingCamera()
    {
        //If edit camera
        if(addEditButton.getText().equals(getString(R.string.save_changes)))
        {
            setResult(Constants.RESULT_FALSE);
            super.onBackPressed();
        }
        //If add camera
        else
        {
            String cameraName = cameraNameEdit.getText().toString();
            String username = usernameEdit.getText().toString();
            String password = passwordEdit.getText().toString();
            String externalHost = externalHostEdit.getText().toString();
            String externalHttp = externalHttpEdit.getText().toString();
            String externalRtsp = externalRtspEdit.getText().toString();
            String jpgUrl = jpgUrlEdit.getText().toString();
            String rtspUrl = rtspUrlEdit.getText().toString();
            if(!(cameraName.isEmpty() && username.isEmpty() && password
                    .isEmpty() && externalHost.isEmpty() && externalHttp.isEmpty() &&
                    externalRtsp.isEmpty() && jpgUrl.isEmpty() && rtspUrl.isEmpty()))
            {
                CustomedDialog.getConfirmCancelAddCameraDialog(this).show();
            }
            else
            {
                setResult(Constants.RESULT_FALSE);
                super.onBackPressed();
            }
        }
    }

    private void initialScreen()
    {
        cameraIdLayout = (LinearLayout) findViewById(R.id.add_camera_id_layout);
        cameraIdTextView = (TextView) findViewById(R.id.add_id_txt_view);
        cameraNameEdit = (EditText) findViewById(R.id.add_name_edit);
        vendorSpinner = (Spinner) findViewById(R.id.vendor_spinner);
        modelSpinner = (Spinner) findViewById(R.id.model_spinner);
        final ImageView vendorLogoImageView = (ImageView) findViewById(R.id.vendor_logo_image_view);
        final ImageView modelThumbnailImageView = (ImageView) findViewById(R.id.model_thumbnail_image_view);
        ImageView modelExplanationImageButton = (ImageView) findViewById(R.id.model_explanation_btn);
        usernameEdit = (EditText) findViewById(R.id.add_username_edit);
        passwordEdit = (EditText) findViewById(R.id.add_password_edit);
        externalHostEdit = (EditText) findViewById(R.id.add_external_host_edit);
        externalHttpEdit = (EditText) findViewById(R.id.add_external_http_edit);
        externalRtspEdit = (EditText) findViewById(R.id.add_external_rtsp_edit);
        jpgUrlEdit = (EditText) findViewById(R.id.add_jpg_edit);
        rtspUrlEdit = (EditText) findViewById(R.id.add_rtsp_edit);
        mHttpStatusTextView = (TextView) findViewById(R.id.port_status_text_http);
        mRtspStatusTextView = (TextView) findViewById(R.id.port_status_text_rtsp);
        jpgUrlLayout = (LinearLayout) findViewById(R.id.add_jpg_url_layout);
        rtspUrlLayout = (LinearLayout) findViewById(R.id.add_rtsp_url_layout);
        addEditButton = (Button) findViewById(R.id.button_add_edit_camera);
        Button testButton = (Button) findViewById(R.id.button_test_snapshot);

        if(cameraEdit != null)
        {
            addEditButton.setText(getString(R.string.save_changes));
            cameraIdLayout.setVisibility(View.VISIBLE);
        }
        else
        {
            cameraIdLayout.setVisibility(View.GONE);
            addEditButton.setText(getString(R.string.finish_and_add));
        }
        buildVendorSpinner(null, null);
        buildModelSpinner(null, null);

        new RequestVendorListTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        vendorSpinner.setOnItemSelectedListener(new OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int
                    position, long id)
            {
                if(position == 0)
                {
                    vendorLogoImageView.setImageResource(android.R.color.transparent);
                    buildModelSpinner(new ArrayList<Model>(), null);
                }
                else
                {
                    String vendorName = vendorSpinner.getSelectedItem().toString();
                    String vendorId = vendorMap.get(vendorName).toLowerCase(Locale.UK);

                    if(!vendorName.equals(getString(R.string.vendor_other)))
                    {
                        //Update vendor logo when vendor is selected
                        Picasso.with(AddEditCameraActivity.this).load(Vendor.getLogoUrl(vendorId)
                        ).placeholder(android.R.color.transparent).into(vendorLogoImageView);

                        new RequestModelListTask(vendorId).executeOnExecutor(AsyncTask
                                .THREAD_POOL_EXECUTOR);
                    }
                    else
                    {
                        modelSpinner.setEnabled(false);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent)
            {
            }
        });

        modelSpinner.setOnItemSelectedListener(new OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView,
                                       int position, long id)
            {
                String vendorId = getVendorIdFromSpinner();
                String modelName = getModelNameFromSpinner();
                String modelId = getModelIdFromSpinner();

                // Do not update camera defaults in edit screen.
                if(cameraEdit == null)
                {
                    if(position == 0)
                    {
                        clearDefaults();
                    }
                    else
                    {
                        new RequestDefaultsTask(vendorId, modelName).executeOnExecutor(AsyncTask
                                .THREAD_POOL_EXECUTOR);
                    }
                }

                //For all situations, the logo & thumbnail should update when selected
                if(position == 0)
                {
                    modelThumbnailImageView.setImageResource(R.drawable.thumbnail_placeholder);
                }
                else
                {
                    //Update model logo when model is selected
                    Picasso.with(AddEditCameraActivity.this)
                            .load(Model.getThumbnailUrl(vendorId, modelId))
                            .placeholder(R.drawable.thumbnail_placeholder)
                            .into(modelThumbnailImageView);
                }

                showUrlEndings(position == 0);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent)
            {
            }
        });

        modelExplanationImageButton.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                CustomedDialog.getMessageDialog(AddEditCameraActivity.this, R.string
                        .msg_model_explanation).show();
            }
        });

        externalHttpEdit.setOnFocusChangeListener(new View.OnFocusChangeListener()
        {
            @Override
            public void onFocusChange(View view, boolean hasFocus)
            {
                if(hasFocus)
                {
                    setUpRtspTextChangeListener(externalHttpEdit, mHttpStatusTextView);
                }
                else
                {
                    checkPort(PortCheckTask.PortType.HTTP);
                }
            }
        });

        externalRtspEdit.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus)
            {
                if(hasFocus)
                {
                    setUpRtspTextChangeListener(externalRtspEdit, mRtspStatusTextView);
                }
                else
                {
                    checkPort(PortCheckTask.PortType.RTSP);
                }
            }
        });

        externalHostEdit.setOnFocusChangeListener(new View.OnFocusChangeListener()
        {
            @Override
            public void onFocusChange(View v, boolean hasFocus)
            {
                if(hasFocus)
                {
                    setUpRtspTextChangeListener(externalHostEdit,
                            mRtspStatusTextView, mHttpStatusTextView);
                }
                else
                {
                    checkPort(PortCheckTask.PortType.HTTP);
                    checkPort(PortCheckTask.PortType.RTSP);
                }
            }
        });

        jpgUrlEdit.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v)
            {
                if(!jpgUrlEdit.isFocusable())
                {
                    CustomedDialog.getMessageDialog(AddEditCameraActivity.this, R.string.msg_url_ending_not_editable).show();
                }
            }
        });

        addEditButton.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {

                String externalHost = externalHostEdit.getText().toString();
                if(Commons.isLocalIp(externalHost))
                {
                    CustomedDialog.getStandardAlertDialog(AddEditCameraActivity.this, new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            performAddEdit();
                        }
                    }, R.string.msg_local_ip_warning).show();
                }
                else
                {
                    performAddEdit();
                }
            }
        });

        testButton.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                String externalHost = externalHostEdit.getText().toString();
                if(Commons.isLocalIp(externalHost))
                {
                    CustomedDialog.getStandardAlertDialog(AddEditCameraActivity.this, new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            launchTestSnapshot();
                        }
                    }, R.string.msg_local_ip_warning).show();
                }
                else
                {
                    launchTestSnapshot();
                }
            }
        });
    }

    private void checkPort(PortCheckTask.PortType type)
    {
        String ipText = externalHostEdit.getText().toString();

        if(!ipText.isEmpty())
        {
            if(type == PortCheckTask.PortType.HTTP)
            {
                String httpText = externalHttpEdit.getText().toString();
                if(!httpText.isEmpty())
                {
                    launchPortCheckTask(ipText, httpText, type);
                }
            }
            else if(type == PortCheckTask.PortType.RTSP)
            {
                String rtspText = externalRtspEdit.getText().toString();
                if(!rtspText.isEmpty())
                {
                    launchPortCheckTask(ipText, rtspText, type);
                }
            }
        }
    }

    private void launchPortCheckTask(String ip, String port, PortCheckTask.PortType type)
    {
        new PortCheckTask(ip, port, this, type)
                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * Clear the status text view when the text in EditText gets changed for the first time
     *
     * @param editText The EditText view to add text change listener
     * @param textViews The status text view(s) list to clear after text changes
     */
    private void setUpRtspTextChangeListener(EditText editText, final TextView... textViews)
    {
        editText.addTextChangedListener(new TextWatcher()
        {

            boolean isFirstTimeChange = true;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable editable)
            {
                if(isFirstTimeChange)
                {
                    for(TextView textView : textViews)
                    {
                        clearPortStatusView(textView);
                        isFirstTimeChange = false;
                    }
                }
            }
        });
    }

    private void clearPortStatusView(TextView textView)
    {
        textView.setVisibility(View.GONE);
    }

    private void updatePortStatusView(TextView textView, boolean isPortOpen)
    {
        textView.setVisibility(View.VISIBLE);
        textView.setText(isPortOpen ? R.string.port_is_open : R.string.port_is_closed);
        textView.setTextColor(isPortOpen ? getResources().getColor(R.color.mint_green) :
                getResources().getColor(R.color.orange_red));
    }

    public void updateHttpPortStatus(boolean isOpen)
    {
        updatePortStatusView(mHttpStatusTextView, isOpen);
    }

    public void updateRtspPortStatus(boolean isOpen)
    {
        updatePortStatusView(mRtspStatusTextView, isOpen);
    }

    private void performAddEdit()
    {
        if(addEditButton.getText().equals(getString(R.string.save_changes)))
        {
            PatchCameraBuilder patchCameraBuilder = buildPatchCameraWithLocalCheck();
            if(patchCameraBuilder != null)
            {
                new PatchCameraTask(patchCameraBuilder.build(),
                        AddEditCameraActivity.this).executeOnExecutor(AsyncTask
                        .THREAD_POOL_EXECUTOR);
            }
            else
            {
                Log.e(TAG, "Camera to patch is null");
            }
        }
        else
        {
            CameraBuilder cameraBuilder = buildCameraWithLocalCheck();
            if(cameraBuilder != null)
            {
                boolean isFromScan = discoveredCamera != null;
                new AddCameraTask(cameraBuilder.build(), AddEditCameraActivity.this,
                        isFromScan).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
            else
            {
                Log.e(TAG, "Camera to add is null");
            }
        }
    }

    private void fillDiscoveredCameraDetails(DiscoveredCamera camera)
    {
        if(camera != null)
        {
            Log.d(TAG, camera.toString());
            if(camera.hasExternalIp())
            {
                externalHostEdit.setText(camera.getExternalIp());
            }
            if(camera.hasExternalHttp())
            {
                externalHttpEdit.setText(String.valueOf(camera.getExthttp()));
            }
            if(camera.hasExternalRtsp())
            {
                externalRtspEdit.setText(String.valueOf(camera.getExtrtsp()));
            }
            if(camera.hasName())
            {
                //The maximum camera name length is 24
                String cameraName = camera.getName();
                if(cameraName.length() > 24)
                {
                    cameraName = cameraName.substring(0, 23);
                }
                cameraNameEdit.setText(cameraName);
            }
            else
            {
                cameraNameEdit.setText((camera.getVendor() + " " + camera.getModel()).toUpperCase());
            }
        }
    }

    /**
     * Auto populate camera name as 'Camera + number'
     */
    private void autoPopulateCameraName()
    {
        if(cameraNameEdit.getText().toString().isEmpty())
        {
            int number = 1;
            boolean matches = true;
            String cameraName;

            while(matches)
            {
                boolean duplicate = false;

                cameraName = "Camera " + number;
                for(EvercamCamera evercamCamera : AppData.evercamCameraList)
                {
                    if(evercamCamera.getName().equals(cameraName))
                    {
                        duplicate = true;
                        break;
                    }
                }

                if(duplicate)
                {
                    number ++;
                }
                else
                {
                    matches = false;
                }
            }

            cameraNameEdit.setText("Camera " + number);
        }
    }

    private void autoPopulateExternalIP()
    {
        /**
         * Auto populate IP as external IP address if on WiFi
         */
        if(new DataCollector(this).isConnectedWifi())
        {
            if(externalHostEdit.getText().toString().isEmpty())
            {

                new AsyncTask<Void, Void, String>()
                {
                    @Override
                    protected String doInBackground(Void... params)
                    {
                        return io.evercam.network.discovery.NetworkInfo.getExternalIP();
                    }

                    @Override
                    protected void onPostExecute(String externalIp)
                    {
                        externalHostEdit.setText(externalIp);
                    }
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        }
    }

    private void fillEditCameraDetails(EvercamCamera camera)
    {
        if(camera != null)
        {
            showUrlEndings(!camera.hasModel());

            // Log.d(TAG, cameraEdit.toString());
            cameraIdTextView.setText(camera.getCameraId());
            cameraNameEdit.setText(camera.getName());
            usernameEdit.setText(camera.getUsername());
            passwordEdit.setText(camera.getPassword());
            jpgUrlEdit.setText(camera.getJpgPath());
            rtspUrlEdit.setText(camera.getH264Path());
            externalHostEdit.setText(camera.getExternalHost());
            int externalHttp = camera.getExternalHttp();
            int externalRtsp = camera.getExternalRtsp();
            if(externalHttp != 0)
            {
                externalHttpEdit.setText(String.valueOf(externalHttp));
            }
            if(externalRtsp != 0)
            {
                externalRtspEdit.setText(String.valueOf(externalRtsp));
            }
        }
    }

    private void showUrlEndings(boolean show)
    {
        jpgUrlLayout.setVisibility(show ? View.VISIBLE : View.GONE);
        rtspUrlLayout.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    /**
     * Read and validate user input for add camera.
     */
    private CameraBuilder buildCameraWithLocalCheck()
    {
        String cameraName = cameraNameEdit.getText().toString();

        if(cameraName.isEmpty())
        {
            CustomToast.showInCenter(this, getString(R.string.name_required));
            return null;
        }

        CameraBuilder cameraBuilder = new CameraBuilder(cameraName, false);

        String vendorId = getVendorIdFromSpinner();
        if(!vendorId.isEmpty())
        {
            cameraBuilder.setVendor(vendorId);
        }

        String modelId = getModelIdFromSpinner();
        if(!modelId.isEmpty())
        {
            cameraBuilder.setModel(modelId);
        }

        String username = usernameEdit.getText().toString();
        if(!username.isEmpty())
        {
            cameraBuilder.setCameraUsername(username);
        }

        String password = passwordEdit.getText().toString();
        if(!password.isEmpty())
        {
            cameraBuilder.setCameraPassword(password);
        }

        String externalHost = externalHostEdit.getText().toString();
        if(externalHost.isEmpty())
        {
            CustomToast.showInCenter(this, getString(R.string.host_required));
            return null;
        }
        else
        {
            cameraBuilder.setExternalHost(externalHost);

            String externalHttp = externalHttpEdit.getText().toString();
            if(!externalHttp.isEmpty())
            {
                int externalHttpInt = getPortIntByString(externalHttp);
                if(externalHttpInt != 0)
                {
                    cameraBuilder.setExternalHttpPort(externalHttpInt);
                }
                else
                {
                    return null;
                }
            }

            String externalRtsp = externalRtspEdit.getText().toString();
            if(!externalRtsp.isEmpty())
            {
                int externalRtspInt = getPortIntByString(externalRtsp);
                if(externalRtspInt != 0)
                {
                    cameraBuilder.setExternalRtspPort(externalRtspInt);
                }
                else
                {
                    return null;
                }
            }
        }

        String jpgUrl = buildUrlEndingWithSlash(jpgUrlEdit.getText().toString());
        if(!jpgUrl.isEmpty())
        {
            cameraBuilder.setJpgUrl(jpgUrl);
        }

        String rtspUrl = buildUrlEndingWithSlash(rtspUrlEdit.getText().toString());
        if(!rtspUrl.isEmpty())
        {
            cameraBuilder.setH264Url(rtspUrl);
        }

        //Attach additional info for discovered camera as well
        if(discoveredCamera != null)
        {
            cameraBuilder.setInternalHost(discoveredCamera.getIP());

            if(discoveredCamera.hasMac())
            {
                cameraBuilder.setMacAddress(discoveredCamera.getMAC());
            }

            if(discoveredCamera.hasHTTP())
            {
                cameraBuilder.setInternalHttpPort(discoveredCamera.getHttp());
            }

            if(discoveredCamera.hasRTSP())
            {
                cameraBuilder.setInternalRtspPort(discoveredCamera.getRtsp());
            }
        }

        return cameraBuilder;
    }

    /**
     * Convert port string to port int, show error toast if port number is not valid,
     *
     * @return int port number, if port is not valid, return 0.
     */
    private int getPortIntByString(String portString)
    {
        try
        {
            int portInt = Integer.valueOf(portString);
            if(portInt > 0)
            {
                if(portInt <= 65535)
                {
                    return portInt;
                }
                else
                {
                    CustomToast.showInCenter(this, getString(R.string.msg_port_range_error));
                    return 0;
                }
            }
            else
            {
                CustomToast.showInCenter(this, getString(R.string.msg_port_range_error));
                return 0;
            }
        }
        catch(NumberFormatException e)
        {
            CustomToast.showInCenter(this, getString(R.string.msg_port_range_error));
            return 0;
        }
    }

    /**
     * Read and validate user input for edit camera.
     */
    private PatchCameraBuilder buildPatchCameraWithLocalCheck()
    {
        PatchCameraBuilder patchCameraBuilder = new PatchCameraBuilder(cameraEdit.getCameraId());

        String cameraName = cameraNameEdit.getText().toString();
        if(cameraName.isEmpty())
        {
            CustomToast.showInCenter(this, getString(R.string.name_required));
            return null;
        }
        else if(!cameraName.equals(cameraEdit.getName()))
        {
            patchCameraBuilder.setName(cameraName);
        }

        String vendorId = getVendorIdFromSpinner();
        patchCameraBuilder.setVendor(vendorId);

        String modelName = getModelIdFromSpinner();
        patchCameraBuilder.setModel(modelName);

        String username = usernameEdit.getText().toString();
        String password = passwordEdit.getText().toString();
        if(!username.equals(cameraEdit.getUsername()) || !password.equals(cameraEdit.getPassword()))
        {
            patchCameraBuilder.setCameraUsername(username);
            patchCameraBuilder.setCameraPassword(password);
        }

        String externalHost = externalHostEdit.getText().toString();
        if(externalHost.isEmpty())
        {
            CustomToast.showInCenter(this, getString(R.string.host_required));
            return null;
        }
        else
        {
            patchCameraBuilder.setExternalHost(externalHost);

            String externalHttp = externalHttpEdit.getText().toString();
            if(!externalHttp.isEmpty())
            {
                int externalHttpInt = getPortIntByString(externalHttp);
                if(externalHttpInt != 0)
                {
                    patchCameraBuilder.setExternalHttpPort(externalHttpInt);
                }
                else
                {
                    return null;
                }
            }

            String externalRtsp = externalRtspEdit.getText().toString();
            if(!externalRtsp.isEmpty())
            {
                int externalRtspInt = getPortIntByString(externalRtsp);
                if(externalRtspInt != 0)
                {
                    patchCameraBuilder.setExternalRtspPort(externalRtspInt);
                }
                else
                {
                    return null;
                }
            }
        }

        String jpgUrl = buildUrlEndingWithSlash(jpgUrlEdit.getText().toString());
        if(!jpgUrl.equals(cameraEdit.getJpgPath()))
        {
            patchCameraBuilder.setJpgUrl(jpgUrl);
        }

        String rtspUrl = buildUrlEndingWithSlash(rtspUrlEdit.getText().toString());
        if(!rtspUrl.equals(cameraEdit.getH264Path()))
        {
            patchCameraBuilder.setH264Url(rtspUrl);
        }

        return patchCameraBuilder;
    }

    private void buildVendorSpinner(ArrayList<Vendor> vendorList, String selectedVendor)
    {
        if(vendorMap == null)
        {
            vendorMap = new TreeMap<>();
        }

        if(vendorMapIdAsKey == null)
        {
            vendorMapIdAsKey = new TreeMap<>();
        }

        if(vendorList != null)
        {
            for(Vendor vendor : vendorList)
            {
                try
                {
                    vendorMap.put(vendor.getName(), vendor.getId());
                    vendorMapIdAsKey.put(vendor.getId(), vendor.getName());
                }
                catch(EvercamException e)
                {
                    Log.e(TAG, e.toString());
                }
            }
        }

        Set<String> set = vendorMap.keySet();
        String[] vendorArray = Commons.joinStringArray(new String[]{getResources().getString(R
                .string.select_vendor)}, set.toArray(new String[0]));
        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, vendorArray);
        spinnerArrayAdapter.setDropDownViewResource(R.layout.spinner);

        int selectedPosition = 0;
        if(discoveredCamera != null)
        {
            if(discoveredCamera.hasVendor())
            {
                String vendorId = discoveredCamera.getVendor();
                String vendorName = vendorMapIdAsKey.get(vendorId);
                selectedPosition = spinnerArrayAdapter.getPosition(vendorName);
            }
        }
        if(selectedVendor != null)
        {
            selectedPosition = spinnerArrayAdapter.getPosition(selectedVendor);
        }
        vendorSpinner.setAdapter(spinnerArrayAdapter);

        if(selectedPosition != 0)
        {
            vendorSpinner.setSelection(selectedPosition);
        }
    }

    private void buildModelSpinner(ArrayList<Model> modelList, String selectedModel)
    {
        if(selectedModel != null && !selectedModel.isEmpty())
        {
            selectedModel = selectedModel.toLowerCase(Locale.UK);
        }
        if(modelMap == null)
        {
            modelMap = new TreeMap<>();
        }
        modelMap.clear();

        if(modelList == null)
        {
            modelSpinner.setEnabled(false);
        }
        else
        {
            if(modelList.size() == 0)
            {
                modelSpinner.setEnabled(false);
            }
            else
            {
                modelSpinner.setEnabled(true);

                for(Model model : modelList)
                {
                    try
                    {
                        modelMap.put(model.getId(),model.getName());
                    }
                    catch(EvercamException e)
                    {
                        Log.e(TAG, e.toString());
                    }
                }
            }
        }
        Collection<String> modelNameCollection = modelMap.values();

        String[] fullModelArray = Commons.joinStringArray(new String[]{getResources().getString(R
                .string.select_model)}, modelNameCollection.toArray(new String[0]));
        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, fullModelArray);
        spinnerArrayAdapter.setDropDownViewResource(R.layout.spinner);
        modelSpinner.setAdapter(spinnerArrayAdapter);

        int selectedPosition = 0;
        if(selectedModel != null)
        {
            if(modelMap.get(selectedModel) != null)
            {
                String selectedModelName = modelMap.get(selectedModel);
                selectedPosition = spinnerArrayAdapter.getPosition(selectedModelName);
            }
        }
        if(selectedPosition != 0)
        {
            modelSpinner.setSelection(selectedPosition);
        }
        else
        {
            modelSpinner.setSelection(spinnerArrayAdapter.getPosition(getString(R.string
                    .model_default)));
        }
    }

    private void fillDefaults(Model model)
    {
        try
        {
            // FIXME: Sometimes vendor with no default model, contains default
            // jpg url.
            // TODO: Consider if no default values associated, clear defaults
            // that has been filled.
            Defaults defaults = model.getDefaults();
            Auth basicAuth = defaults.getAuth(Auth.TYPE_BASIC);
            if(basicAuth != null)
            {
                usernameEdit.setText(basicAuth.getUsername());
                passwordEdit.setText(basicAuth.getPassword());
            }
            jpgUrlEdit.setText(defaults.getJpgURL());
            rtspUrlEdit.setText(defaults.getH264URL());

            if(!model.getName().equals(Model.DEFAULT_MODEL_NAME) && !jpgUrlEdit.getText().toString().isEmpty())
            {
                //If user specified a specific model, make it not editable
                jpgUrlEdit.setFocusable(false);
                jpgUrlEdit.setClickable(true);
            }
            else
            {
                //For default model or
                jpgUrlEdit.setFocusable(true);
                jpgUrlEdit.setClickable(true);
                jpgUrlEdit.setFocusableInTouchMode(true);
            }
        }
        catch(EvercamException e)
        {
            Log.e(TAG, "Fill defaults: " + e.toString());
        }
    }

    private void clearDefaults()
    {
        usernameEdit.setText("");
        passwordEdit.setText("");
        jpgUrlEdit.setText("");
        rtspUrlEdit.setText("");

        //Make it editable when defaults are cleared
        jpgUrlEdit.setFocusable(true);
        jpgUrlEdit.setClickable(true);
        jpgUrlEdit.setFocusableInTouchMode(true);
    }

    private String getVendorIdFromSpinner()
    {
        String vendorName = vendorSpinner.getSelectedItem().toString();
        if(vendorName.equals(getString(R.string.select_vendor)))
        {
            return "";
        }
        else
        {
            return vendorMap.get(vendorName).toLowerCase(Locale.UK);
        }

    }

    private String getModelIdFromSpinner()
    {
        String modelName = modelSpinner.getSelectedItem().toString();
        if(modelName.equals(getString(R.string.select_model)))
        {
            return "";
        }
        else
        {
            for (Map.Entry<String, String> entry : modelMap.entrySet())
            {
                if(entry.getValue().equals(modelName))
                {
                    return entry.getKey();
                }
            }
        }
        return "";
    }

    private String getModelNameFromSpinner()
    {
        String modelName = modelSpinner.getSelectedItem().toString();
        if(modelName.equals(getString(R.string.select_model)))
        {
            return "";
        }
        else
        {
            return modelName;
        }
    }

    public static String buildUrlEndingWithSlash(String originalUrl)
    {
        String jpgUrl = "";
        if(originalUrl != null && !originalUrl.equals(""))
        {
            if(!originalUrl.startsWith("/"))
            {
                jpgUrl = "/" + originalUrl;
            }
            else
            {
                jpgUrl = originalUrl;
            }
        }
        return jpgUrl;
    }

    private void launchTestSnapshot()
    {
        String externalHost = externalHostEdit.getText().toString();

        if(externalHost.isEmpty())
        {
            CustomToast.showInCenter(this, getString(R.string.host_required));
        }
        else
        {
            final String username = usernameEdit.getText().toString();
            final String password = passwordEdit.getText().toString();
            String jpgUrlString = jpgUrlEdit.getText().toString();
            final String jpgUrl = buildUrlEndingWithSlash(jpgUrlString);

            String externalUrl = getExternalUrl();
            if(externalUrl != null)
            {
                new TestSnapshotTask(externalUrl, jpgUrl, username, password,
                        AddEditCameraActivity.this).executeOnExecutor(AsyncTask
                        .THREAD_POOL_EXECUTOR);
            }
        }
    }

    /**
     * Check external HTTP port is filled or not and return external URL with
     * snapshot ending.
     */
    private String getExternalUrl()
    {
        String externalHost = externalHostEdit.getText().toString();
        String externalHttp = externalHttpEdit.getText().toString();
        if(externalHttp.isEmpty())
        {
            CustomToast.showInCenter(this, getString(R.string.external_http_required));
            return null;
        }
        else
        {
            int externalHttpInt = getPortIntByString(externalHttp);
            if(externalHttpInt != 0)
            {
                return getString(R.string.prefix_http) + externalHost + ":" + externalHttp;
            }
            else
            {
                return null;
            }
        }
    }

    class RequestVendorListTask extends AsyncTask<Void, Void, ArrayList<Vendor>>
    {

        @Override
        protected void onPostExecute(ArrayList<Vendor> vendorList)
        {
            if(vendorList != null)
            {
                // If the camera has vendor, show as selected in spinner
                if(cameraEdit != null && !cameraEdit.getVendor().isEmpty())
                {
                    buildVendorSpinner(vendorList, cameraEdit.getVendor());
                }
                else
                {
                    buildVendorSpinner(vendorList, null);
                }
            }
            else
            {
                Log.e(TAG, "Vendor list is null");
            }
        }

        @Override
        protected ArrayList<Vendor> doInBackground(Void... params)
        {
            try
            {
                return Vendor.getAll();
            }
            catch(EvercamException e)
            {
                Log.e(TAG, e.toString());
            }
            return null;
        }
    }

    class RequestModelListTask extends AsyncTask<Void, Void, ArrayList<Model>>
    {
        private String vendorId;

        public RequestModelListTask(String vendorId)
        {
            this.vendorId = vendorId;
        }

        @Override
        protected ArrayList<Model> doInBackground(Void... params)
        {
            try
            {
                return Model.getAllByVendorId(vendorId);
            }
            catch(EvercamException e)
            {
                EvercamPlayApplication.sendCaughtException(AddEditCameraActivity.this,
                        e.toString() + " " + "with vendor id: " + vendorId);
                Log.e(TAG, e.toString());
            }
            return null;
        }

        @Override
        protected void onPostExecute(ArrayList<Model> modelList)
        {
            if(modelList != null)
            {
                if(cameraEdit != null && !cameraEdit.getModel().isEmpty())
                {
                    buildModelSpinner(modelList, cameraEdit.getModel());
                }
                else if(discoveredCamera != null && discoveredCamera.hasModel())
                {
                    buildModelSpinner(modelList, discoveredCamera.getModel());
                }
                else
                {
                    buildModelSpinner(modelList, null);
                }
            }
        }
    }

    class RequestDefaultsTask extends AsyncTask<Void, Void, Model>
    {
        private String vendorId;
        private String modelName;

        public RequestDefaultsTask(String vendorId, String modelName)
        {
            this.vendorId = vendorId;
            this.modelName = modelName;
        }

        @Override
        protected void onPreExecute()
        {
            clearDefaults();
        }

        @Override
        protected Model doInBackground(Void... params)
        {
            try
            {
                ArrayList<Model> modelList = Model.getAll(modelName, vendorId);
                if(modelList.size() > 0)
                {
                    return modelList.get(0);
                }
            }
            catch(EvercamException e)
            {
                Log.e(TAG, e.toString());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Model model)
        {
            if(model != null)
            {
                fillDefaults(model);
            }
        }
    }
}
