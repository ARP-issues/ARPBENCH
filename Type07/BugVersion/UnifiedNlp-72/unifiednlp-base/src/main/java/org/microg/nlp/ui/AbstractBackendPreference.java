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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.IBinder;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import org.microg.nlp.Preferences;
import org.microg.nlp.R;

import java.util.ArrayList;
import java.util.List;

import static android.content.Context.BIND_AUTO_CREATE;
import static android.content.Intent.ACTION_VIEW;
import static org.microg.nlp.api.Constants.METADATA_BACKEND_ABOUT_ACTIVITY;
import static org.microg.nlp.api.Constants.METADATA_BACKEND_INIT_ACTIVITY;
import static org.microg.nlp.api.Constants.METADATA_BACKEND_SETTINGS_ACTIVITY;
import static org.microg.nlp.api.Constants.METADATA_BACKEND_SUMMARY;

abstract class AbstractBackendPreference extends DialogPreference {
    private ListView listView;
    private final Adapter adapter;
    private List<BackendInfo> knownBackends;

    AbstractBackendPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);

        setDialogIcon(null);

        adapter = new Adapter();
    }

    @Override
    protected View onCreateDialogView() {
        listView = new ListView(getContext());
        updateBackends();
        return listView;
    }

    private void updateBackends() {
        knownBackends = queryKnownBackends();
        markBackendsAsEnabled();
        resetAdapter();
    }

    List<BackendInfo> queryKnownBackends() {
        return intentToKnownBackends(buildBackendIntent());
    }

    private void resetAdapter() {
        adapter.clear();
        for (BackendInfo backend : knownBackends) {
            adapter.add(backend);
        }
        listView.setAdapter(adapter);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (BackendInfo backend : knownBackends) {
            if (backend.enabled) {
                if (sb.length() != 0) {
                    sb.append("|");
                }
                sb.append(backend.serviceInfo.packageName).append("/")
                        .append(backend.serviceInfo.name);
            }
        }
        return sb.toString();
    }

    void markBackendsAsEnabled() {
        for (String backend : Preferences.splitBackendString(getPersistedString(defaultValue()))) {
            String[] parts = backend.split("/");
            if (parts.length == 2) {
                for (BackendInfo backendInfo : knownBackends) {
                    ServiceInfo serviceInfo = backendInfo.serviceInfo;
                    if (serviceInfo.packageName.equals(parts[0]) &&
                            serviceInfo.name.equals(parts[1])) {
                        backendInfo.enabled = true;
                    }
                }
            }
        }
    }

    List<BackendInfo> intentToKnownBackends(Intent intent) {
        List<BackendInfo> knownBackends = new ArrayList<BackendInfo>();
        List<ResolveInfo> resolveInfos = getContext().getPackageManager()
                .queryIntentServices(intent, PackageManager.GET_META_DATA);
        for (ResolveInfo info : resolveInfos) {
            ServiceInfo serviceInfo = info.serviceInfo;
            String simpleName = String
                    .valueOf(serviceInfo.loadLabel(getContext().getPackageManager()));
            knownBackends.add(new BackendInfo(serviceInfo, simpleName));
        }
        return knownBackends;
    }

    private Intent createExternalIntent(BackendInfo backendInfo, String metaName) {
        Intent intent = new Intent(ACTION_VIEW);
        intent.setPackage(backendInfo.serviceInfo.packageName);
        intent.setClassName(backendInfo.serviceInfo.packageName, backendInfo.getMeta(metaName));
        return intent;
    }

    private class Adapter extends ArrayAdapter<BackendInfo> {

        public Adapter() {
            super(AbstractBackendPreference.this.getContext(), R.layout.backend_list_entry);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v;
            if (convertView != null) {
                v = convertView;
            } else {
                v = ((LayoutInflater) getContext().getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE)).inflate(
                        R.layout.backend_list_entry, parent, false);
            }
            final BackendInfo backend = getItem(position);
            TextView title = (TextView) v.findViewById(android.R.id.text1);
            title.setText(backend.simpleName);
            TextView subtitle = (TextView) v.findViewById(android.R.id.text2);
            if (backend.getMeta(METADATA_BACKEND_SUMMARY) != null) {
                subtitle.setText(backend.serviceInfo.metaData.getString(METADATA_BACKEND_SUMMARY));
                subtitle.setVisibility(View.VISIBLE);
            } else {
                subtitle.setVisibility(View.GONE);
            }
            final CheckBox checkbox = (CheckBox) v.findViewById(R.id.enabled);
            if (checkbox.isChecked() != backend.enabled) {
                checkbox.toggle();
            }
            checkbox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    backend.enabled = checkbox.isChecked();
                    if (backend.enabled) enableBackend(backend);
                }
            });
            configureExternalButton(backend, v.findViewById(android.R.id.button1),
                    METADATA_BACKEND_SETTINGS_ACTIVITY);
            configureExternalButton(backend, v.findViewById(android.R.id.button2),
                    METADATA_BACKEND_ABOUT_ACTIVITY);
            return v;
        }

        private void configureExternalButton(final BackendInfo backend, View button,
                                             final String metaName) {
            if (backend.getMeta(metaName) != null) {
                button.setVisibility(View.VISIBLE);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        getContext().startActivity(createExternalIntent(backend, metaName));
                    }
                });
            } else {
                button.setVisibility(View.GONE);
            }
        }
    }

    protected void enableBackend(BackendInfo backendInfo) {
        if (backendInfo.getMeta(METADATA_BACKEND_INIT_ACTIVITY) != null) {
            getContext().startActivity(createExternalIntent(backendInfo, METADATA_BACKEND_INIT_ACTIVITY));
        } else {
            Intent intent = buildBackendIntent();
            intent.setPackage(backendInfo.serviceInfo.packageName);
            intent.setClassName(backendInfo.serviceInfo.packageName, backendInfo.serviceInfo.name);
            getContext().bindService(intent, new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    Intent i = getBackendInitIntent(service);
                    if (i != null) {
                        getContext().startActivity(i);
                    }
                    getContext().unbindService(this);
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {

                }
            }, BIND_AUTO_CREATE);
        }
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            persistString(toString());
            onValueChanged();
        }
    }

    protected abstract void onValueChanged();

    protected abstract Intent buildBackendIntent();

    protected abstract String defaultValue();

    protected abstract Intent getBackendInitIntent(IBinder service);

    private class BackendInfo {
        private final ServiceInfo serviceInfo;
        private final String simpleName;
        private boolean enabled = false;

        public BackendInfo(ServiceInfo serviceInfo, String simpleName) {
            this.serviceInfo = serviceInfo;
            this.simpleName = simpleName;
        }

        public String getMeta(String metaName) {
            return serviceInfo.metaData != null ? serviceInfo.metaData.getString(metaName) : null;
        }

        @Override
        public String toString() {
            return simpleName;
        }
    }
}
