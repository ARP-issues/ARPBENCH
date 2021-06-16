package org.ligi.passandroid.ui;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.view.MenuItem;

import org.ligi.passandroid.R;

public class PreferenceActivity extends AppCompatActivity {

    public static class GoPrefsFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(final Bundle bundle, final String rootKey) {
            setPreferencesFromResource(R.xml.preferences, rootKey);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.simple_container);
        getSupportFragmentManager().beginTransaction().replace(R.id.container, new GoPrefsFragment()).commit();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }


    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId()==android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

}
