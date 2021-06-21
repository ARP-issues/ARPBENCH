package io.evercam.androidapp;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.splunk.mint.Mint;

import java.util.ArrayList;

import io.evercam.androidapp.utils.Constants;
import io.evercam.androidapp.utils.PrefsManager;
import io.evercam.androidapp.utils.PropertyReader;

public class CameraPrefsActivity extends AppCompatActivity
{
    private static int screenWidth = 0;
    private PropertyReader propertyReader;
    private static final String TAG = "CameraPrefsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings_layout);

        Toolbar toolbar = (Toolbar) findViewById(R.id.tool_bar);
        toolbar.setBackgroundColor(getResources().getColor(R.color.dark_gray_background));
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        propertyReader = new PropertyReader(this);
        if(Constants.isAppTrackingEnabled)
        {
            if(propertyReader.isPropertyExist(PropertyReader
                    .KEY_SPLUNK_MINT))
            {
                String bugSenseCode = propertyReader.getPropertyStr(PropertyReader
                        .KEY_SPLUNK_MINT);
                Mint.initAndStartSession(this, bugSenseCode);
            }
        }

        screenWidth = CamerasActivity.readScreenWidth(this);

        getFragmentManager().beginTransaction().replace(R.id.content_frame,
                new MyPreferenceFragment()).commit();
        this.setDefaultKeyMode(DEFAULT_KEYS_DISABLE);
    }

    @Override
    public void onStart()
    {
        super.onStart();

        if(Constants.isAppTrackingEnabled)
        {
            if(propertyReader.isPropertyExist(PropertyReader.KEY_SPLUNK_MINT))
            {
                Mint.startSession(this);
            }
        }
    }

    @Override
    public void onStop()
    {
        super.onStop();


        if(Constants.isAppTrackingEnabled)
        {
            if(propertyReader.isPropertyExist(PropertyReader.KEY_SPLUNK_MINT))
            {
                Mint.closeSession(this);
            }
        }
    }

    @Override
    protected void onRestart()
    {
        super.onRestart();
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle item selection
        switch(item.getItemId())
        {
            case android.R.id.home:

                this.finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public static class MyPreferenceFragment extends PreferenceFragment
    {
        public MyPreferenceFragment()
        {
            // super();
        }

        @Override
        public void onCreate(final Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.main_preference);
            setCameraNumbersForScreen(screenWidth);
            setUpSleepTime();
        }

        private void setCameraNumbersForScreen(int screenWidth)
        {
            int maxCamerasPerRow = 3;
            if(screenWidth != 0)
            {
                maxCamerasPerRow = screenWidth / 350;
            }
            if(maxCamerasPerRow == 0)
            {
                maxCamerasPerRow = 1;
            }
            ArrayList<String> cameraNumberArrayList = new ArrayList<String>();
            for(int index = 1; index <= maxCamerasPerRow; index++)
            {
                cameraNumberArrayList.add(String.valueOf(index));
            }
            CharSequence[] charNumberValues = cameraNumberArrayList.toArray(new
                    CharSequence[cameraNumberArrayList.size()]);
            final ListPreference interfaceList = (ListPreference)
                    getPreferenceManager().findPreference(PrefsManager.KEY_CAMERA_PER_ROW);
            interfaceList.setEntries(charNumberValues);
            interfaceList.setEntryValues(charNumberValues);
            interfaceList.setSummary(interfaceList.getValue());
            interfaceList.setOnPreferenceChangeListener(new OnPreferenceChangeListener()
            {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue)
                {
                    interfaceList.setSummary(newValue.toString());
                    return true;
                }
            });
        }

        private void setUpSleepTime()
        {
            final ListPreference sleepListPrefs = (ListPreference)
                    getPreferenceManager().findPreference(PrefsManager.KEY_AWAKE_TIME);
            sleepListPrefs.setSummary(getSummary(sleepListPrefs.getEntry() + ""));
            sleepListPrefs.setOnPreferenceChangeListener(new OnPreferenceChangeListener()
            {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue)
                {
                    int index = sleepListPrefs.findIndexOfValue(newValue.toString());
                    String entry = sleepListPrefs.getEntries()[index].toString();
                    sleepListPrefs.setSummary(getSummary(entry));
                    return true;
                }
            });
        }

        private String getSummary(String entry)
        {
            if(entry.equals(getString(R.string.prefs_never)))
            {
                return entry;
            }
            else
            {
                return getString(R.string.summary_awake_time_prefix) + " " + entry + " " +
                        getString(R.string.summary_awake_time_suffix);
            }
        }
    }
}