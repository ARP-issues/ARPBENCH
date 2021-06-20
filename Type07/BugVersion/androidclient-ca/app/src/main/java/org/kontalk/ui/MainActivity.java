/*
 * Kontalk Android client
 * Copyright (C) 2017 Kontalk Devteam <devteam@kontalk.org>

 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.kontalk.ui;

import java.io.IOException;

import com.afollestad.assent.Assent;
import com.afollestad.assent.AssentCallback;
import com.afollestad.assent.PermissionResultSet;
import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.content.LocalBroadcastManager;
import android.text.InputType;
import android.widget.CompoundButton;
import android.widget.Toast;

import org.kontalk.Kontalk;
import org.kontalk.R;
import org.kontalk.authenticator.Authenticator;
import org.kontalk.authenticator.LegacyAuthentication;
import org.kontalk.service.msgcenter.MessageCenterService;
import org.kontalk.sync.SyncAdapter;
import org.kontalk.util.MessageUtils;
import org.kontalk.util.Preferences;
import org.kontalk.util.SystemUtils;


/**
 * Abstract main activity.
 * @author Daniele Ricci
 * @version 1.0
 */
public abstract class MainActivity extends ToolbarActivity {

    Dialog mUpgradeProgress;
    BroadcastReceiver mUpgradeReceiver;

    private static final int REQUEST_PERMISSIONS = 100;

    private static final int DIALOG_AUTH_ERROR_WARNING = 1;
    private static final int DIALOG_AUTH_REQUEST_PASSWORD = 2;

    private static final String ACTION_AUTH_ERROR_WARNING = "org.kontalk.AUTH_ERROR_WARN";
    private static final String ACTION_AUTH_REQUEST_PASSWORD = "org.kontalk.AUTH_REQUEST_PASSWORD";

    /**
     * Doesn't really matter because subclasses should not use use setDisplayHomeAsUpEnabled.
     */
    @Override
    protected boolean isNormalUpNavigation() {
        return true;
    }

    /**
     * To be called at the end of subclasses' {@link #onCreate}.
     * @return true if the method handled the situation, false otherwise
     */
    protected boolean afterOnCreate() {
        askPermissions();

        return !xmppUpgrade() && !checkPassword() && (ifHuaweiAlert() || ifDozeAlert());
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Assent.setActivity(this, this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Assent.setActivity(this, this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isFinishing())
            Assent.setActivity(this, null);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Assent.handleResult(permissions, grantResults);
    }

    private void askPermissions() {
        if (!Preferences.isPermissionAsked(Assent.READ_CONTACTS) ||
            !Preferences.isPermissionAsked(Assent.WRITE_CONTACTS)) {

            if (!Assent.isPermissionGranted(Assent.READ_CONTACTS) ||
                !Assent.isPermissionGranted(Assent.WRITE_CONTACTS)) {
                Assent.requestPermissions(new AssentCallback() {
                    @Override
                    public void onPermissionResult(PermissionResultSet result) {
                        if (result.isGranted(Assent.READ_CONTACTS)) {
                            // we finally have contacts, trigger a sync
                            SyncAdapter.requestSync(MainActivity.this, true);
                        }
                        else {
                            new MaterialDialog.Builder(MainActivity.this)
                                .content(R.string.err_contacts_denied)
                                .positiveText(android.R.string.ok)
                                .show();
                        }
                    }
                }, REQUEST_PERMISSIONS, Assent.READ_CONTACTS, Assent.WRITE_CONTACTS);

                Preferences.setPermissionAsked(Assent.READ_CONTACTS);
                Preferences.setPermissionAsked(Assent.WRITE_CONTACTS);
            }
        }

        if (!Preferences.isPermissionAsked(Assent.READ_EXTERNAL_STORAGE) ||
            !Preferences.isPermissionAsked(Assent.WRITE_EXTERNAL_STORAGE)) {

            if (!Assent.isPermissionGranted(Assent.READ_EXTERNAL_STORAGE) ||
                !Assent.isPermissionGranted(Assent.WRITE_EXTERNAL_STORAGE)) {
                Assent.requestPermissions(new AssentCallback() {
                    @Override
                    public void onPermissionResult(PermissionResultSet result) {
                        if (!result.allPermissionsGranted()) {
                            new MaterialDialog.Builder(MainActivity.this)
                                .content(R.string.err_storage_denied)
                                .positiveText(android.R.string.ok)
                                .show();
                        }
                    }
                }, REQUEST_PERMISSIONS, Assent.READ_EXTERNAL_STORAGE, Assent.WRITE_EXTERNAL_STORAGE);

                Preferences.setPermissionAsked(Assent.READ_EXTERNAL_STORAGE);
                Preferences.setPermissionAsked(Assent.WRITE_EXTERNAL_STORAGE);
            }
        }
    }

    private boolean ifDozeAlert() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return false;

        boolean skipMessage = Preferences.isSkipDozeMode();
        if (!skipMessage) {
            PowerManager pwm = (PowerManager) getSystemService(POWER_SERVICE);
            if (!pwm.isIgnoringBatteryOptimizations(getPackageName())) {
                new MaterialDialog.Builder(this)
                    .title(R.string.title_doze_mode)
                    .content(R.string.msg_doze_mode)
                    .positiveText(R.string.btn_request_doze_mode_whitelist)
                    .neutralText(R.string.learn_more)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @RequiresApi(Build.VERSION_CODES.M)
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            requestDozeModeWhitelist();
                        }
                    })
                    .onNeutral(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            SystemUtils.openURL(dialog.getContext(), getString(R.string.help_doze_whitelist));
                        }
                    })
                    .checkBoxPromptRes(R.string.check_dont_show_again, false, new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                            Preferences.setSkipDozeMode(isChecked);
                        }
                    })
                    .show();

                return true;
            }
            else {
                Preferences.setSkipDozeMode(true);
            }
        }
        return false;
    }

    @SuppressLint("BatteryLife")
    @RequiresApi(Build.VERSION_CODES.M)
    void requestDozeModeWhitelist() {
        try {
            startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:" + getPackageName())));
        }
        catch (ActivityNotFoundException e) {
            try {
                startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
                Toast.makeText(this, R.string.msg_request_doze_failed_manual, Toast.LENGTH_LONG).show();
            }
            catch (ActivityNotFoundException e2) {
                // you may as well throw your device out of the window
                startActivity(new Intent(Settings.ACTION_SETTINGS));
                Toast.makeText(this, R.string.msg_request_doze_failed_all, Toast.LENGTH_LONG).show();
            }
        }
    }

    // http://stackoverflow.com/a/35220476/1045199
    private boolean ifHuaweiAlert() {
        boolean skipMessage = Preferences.isSkipHuaweiProtectedApps();
        if (!skipMessage) {
            Intent intent = new Intent();
            intent.setClassName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity");
            if (SystemUtils.isCallable(this, intent)) {
                new MaterialDialog.Builder(this)
                    .title(R.string.title_huawei_protected_apps)
                    .content(R.string.msg_huawei_protected_apps)
                    .positiveText(R.string.btn_huawei_protected_apps)
                    .neutralText(R.string.learn_more)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            startHuaweiProtectedApps();
                        }
                    })
                    .onNeutral(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            SystemUtils.openURL(dialog.getContext(), getString(R.string.help_huawei_protected_apps));
                        }
                    })
                    .checkBoxPromptRes(R.string.check_dont_show_again, false, new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                            Preferences.setSkipHuaweiProtectedApps(isChecked);
                        }
                    })
                    .show();

                return true;
            }
            else {
                Preferences.setSkipHuaweiProtectedApps(true);
            }
        }
        return false;
    }

    void startHuaweiProtectedApps() {
        try {
            String cmd = "am start -n com.huawei.systemmanager/.optimize.process.ProtectActivity";
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                cmd += " --user " + SystemUtils.getUserSerial(this);
            }
            Runtime.getRuntime().exec(cmd);
        }
        catch (IOException ignored) {
        }
    }

    boolean isPasswordValid() {
        try {
            Kontalk.get(MainActivity.this).getPersonalKey();
            return true;
        }
        catch (Exception e) {
            return false;
        }
    }

    private boolean checkPassword() {
        if (Authenticator.getDefaultAccount(this) == null)
            return false;

        if (Kontalk.get(this).getCachedPassphrase() == null || !isPasswordValid()) {
            askForPassword();
            return true;
        }

        return false;
    }

    private void askForPassword() {
        showDialog(DIALOG_AUTH_REQUEST_PASSWORD);
    }

    /** Big upgrade: asymmetric key encryption (for XMPP). */
    private boolean xmppUpgrade() {
        AccountManager am = (AccountManager) getSystemService(Context.ACCOUNT_SERVICE);
        Account account = Authenticator.getDefaultAccount(am);
        if (account != null) {
            if (!Authenticator.hasPersonalKey(am, account)) {
                // first of all, disable offline mode
                Preferences.setOfflineMode(this, false);

                String name = Authenticator.getDefaultDisplayName(this);
                if (name == null || name.length() == 0) {
                    // ask for user name
                    askForPersonalName();
                }
                else {
                    // proceed to upgrade immediately
                    proceedXmppUpgrade(name);
                }

                return true;
            }
        }

        return false;
    }

    private void askForPersonalName() {
        new MaterialDialog.Builder(this)
            .content(R.string.msg_no_name)
            .positiveText(android.R.string.ok)
            .inputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PERSON_NAME)
            .input(R.string.hint_validation_name, 0, false, new MaterialDialog.InputCallback() {
                @Override
                public void onInput(MaterialDialog dialog, CharSequence input) {
                    // no key pair found, generate a new one
                    String name = input.toString();

                    // upgrade account
                    proceedXmppUpgrade(name);
                }
            })
            .onNegative(new MaterialDialog.SingleButtonCallback() {
                @Override
                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction dialogAction) {
                    dialog.cancel();
                }
            })
            .negativeText(android.R.string.cancel)
            .cancelListener(new DialogInterface.OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    new MaterialDialog.Builder(MainActivity.this)
                        .title(R.string.title_no_personal_key)
                        .content(R.string.msg_no_personal_key)
                        .positiveText(android.R.string.ok)
                        .show();
                }
            })
            .show();
    }

    void proceedXmppUpgrade(String name) {
        // start progress dialog
        mUpgradeProgress = new LockedDialog.Builder(this)
            .progress(true, 0)
            .content(R.string.msg_xmpp_upgrading)
            .show();

        // setup operation completed received
        mUpgradeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                LocalBroadcastManager lbm = LocalBroadcastManager
                    .getInstance(getApplicationContext());
                lbm.unregisterReceiver(mUpgradeReceiver);
                mUpgradeReceiver = null;

                // force contact list update
                SyncAdapter.requestSync(MainActivity.this, true);

                if (mUpgradeProgress != null) {
                    mUpgradeProgress.dismiss();
                    mUpgradeProgress = null;
                }
            }
        };
        IntentFilter filter = new IntentFilter(MessageCenterService.ACTION_REGENERATE_KEYPAIR);
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.registerReceiver(mUpgradeReceiver, filter);

        LegacyAuthentication.doUpgrade(getApplicationContext(), name);
    }

    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        switch (id) {
            case DIALOG_AUTH_ERROR_WARNING:
                return new MaterialDialog.Builder(this)
                    .title(R.string.title_auth_error)
                    .content(R.string.msg_auth_error)
                    .positiveText(android.R.string.ok)
                    .build();

            case DIALOG_AUTH_REQUEST_PASSWORD:
                return new MaterialDialog.Builder(this)
                    .title(R.string.title_passphrase_request)
                    .inputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)
                    .input(0, 0, true, new MaterialDialog.InputCallback() {
                        @Override
                        public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {
                            String passphrase = input.toString();
                            // user-entered passphrase is hashed
                            String hashed = MessageUtils.sha1(passphrase);
                            Authenticator.setPassphrase(MainActivity.this, hashed, true);
                            Kontalk.get(MainActivity.this).invalidatePersonalKey();
                            if (isPasswordValid()) {
                                MessageCenterService.start(MainActivity.this);
                            }
                            else {
                                Toast.makeText(MainActivity.this, R.string.err_invalid_passphrase,
                                    Toast.LENGTH_LONG).show();
                            }
                        }
                    })
                    .onNegative(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            finish();
                        }
                    })
                    .negativeText(android.R.string.cancel)
                    .positiveText(android.R.string.ok)
                    .build();

        }

        return super.onCreateDialog(id, args);
    }

    /**
     * Something to handle for the given intent.
     * @param intent
     * @return <code>true</code> if the intent has been handled.
     */
    protected boolean handleIntent(Intent intent) {
        if (intent != null) {
            String action = intent.getAction();

            if (ACTION_AUTH_ERROR_WARNING.equals(action)) {
                showDialog(DIALOG_AUTH_ERROR_WARNING);
                return true;
            }
            else if (ACTION_AUTH_REQUEST_PASSWORD.equals(action)) {
                showDialog(DIALOG_AUTH_REQUEST_PASSWORD);
                return true;
            }
        }

        return false;
    }

    public static Intent authenticationErrorWarning(Context context) {
        Intent i = new Intent(context.getApplicationContext(), ConversationsActivity.class);
        i.setAction(ACTION_AUTH_ERROR_WARNING);
        return i;
    }

    public static Intent passwordRequest(Context context) {
        Intent i = new Intent(context.getApplicationContext(), ConversationsActivity.class);
        i.setAction(ACTION_AUTH_REQUEST_PASSWORD);
        return i;
    }

}
