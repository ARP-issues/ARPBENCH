/*
 * Copyright 2013-2015 microG Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.microg.nlp.ui;

import android.content.Context;
import android.os.Bundle;
import android.preference.Preference;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.preference.PreferenceFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import org.microg.nlp.BuildConfig;
import org.microg.nlp.R;
import org.microg.tools.selfcheck.NlpOsCompatChecks;
import org.microg.tools.selfcheck.NlpStatusChecks;
import org.microg.tools.selfcheck.PermissionCheckGroup;
import org.microg.tools.selfcheck.SelfCheckGroup;
import org.microg.tools.ui.AbstractAboutFragment;
import org.microg.tools.ui.AbstractSelfCheckFragment;

import java.util.List;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.LOLLIPOP_MR1;

public class SettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.content_wrapper, new MyPreferenceFragment()).commit();
    }

    private static boolean isUnifiedNlpAppRelease(Context context) {
        int resId = context.getResources().getIdentifier("is_unifiednlp_app", "bool", context.getPackageName());
        return resId != 0 && context.getResources().getBoolean(resId);
    }

    public static class MyPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            if (isUnifiedNlpAppRelease(getContext())) {
                addPreferencesFromResource(R.xml.nlp_setup_preferences);

                findPreference(getString(R.string.self_check_title))
                        .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                            @Override
                            public boolean onPreferenceClick(Preference preference) {
                                getFragmentManager().beginTransaction()
                                        .addToBackStack("root")
                                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                                        .replace(R.id.content_wrapper, new MySelfCheckFragment())
                                        .commit();
                                return true;
                            }
                        });
            }
            addPreferencesFromResource(R.xml.nlp_preferences);
            if (isUnifiedNlpAppRelease(getContext())) {
                addPreferencesFromResource(R.xml.nlp_about_preferences);

                findPreference(getString(R.string.pref_about_title))
                        .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                            @Override
                            public boolean onPreferenceClick(Preference preference) {
                                getFragmentManager().beginTransaction()
                                        .addToBackStack("root")
                                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                                        .replace(R.id.content_wrapper, new MyAboutFragment())
                                        .commit();
                                return true;
                            }
                        });
            }
        }
    }

    public static class MySelfCheckFragment extends AbstractSelfCheckFragment {

        @Override
        protected void prepareSelfCheckList(List<SelfCheckGroup> checks) {
            if (SDK_INT > LOLLIPOP_MR1) {
                checks.add(new PermissionCheckGroup(ACCESS_COARSE_LOCATION));
            }
            checks.add(new NlpOsCompatChecks());
            checks.add(new NlpStatusChecks());
        }
    }

    public static class MyAboutFragment extends AbstractAboutFragment {

        @Override
        protected String getSummary() {
            String packageName = getContext().getPackageName();
            if (packageName.equals("com.google.android.gms")) {
                return getString(R.string.nlp_version_default);
            } else if (packageName.equals("com.google.android.location")) {
                return getString(R.string.nlp_version_legacy);
            } else if (packageName.equals("org.microg.nlp")) {
                return getString(R.string.nlp_version_custom);
            }
            return null;
        }

        @Override
        protected String getSelfVersion() {
            return BuildConfig.VERSION_NAME;
        }

        @Override
        protected void collectLibraries(List<Library> libraries) {
            libraries.add(new Library("org.microg.nlp.api", "microG UnifiedNlp Api", "Apache License 2.0, Copyright ?? microG Team"));
        }
    }
}
