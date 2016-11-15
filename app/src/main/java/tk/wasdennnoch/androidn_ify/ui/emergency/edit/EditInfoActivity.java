/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package tk.wasdennnoch.androidn_ify.ui.emergency.edit;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import java.util.ArrayList;

import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.ui.emergency.EmergencyTabActivity;
import tk.wasdennnoch.androidn_ify.ui.emergency.PreferenceKeys;
import tk.wasdennnoch.androidn_ify.ui.emergency.view.ViewInfoActivity;

import static android.view.WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD;

/**
 * Activity for editing emergency information.
 */
@SuppressWarnings("WeakerAccess")
public class EditInfoActivity extends EmergencyTabActivity {
    static final String TAG_WARNING_DIALOG = "warning_dialog";
    static final String TAG_CLEAR_ALL_DIALOG = "clear_all_dialog";
    static final String KEY_LAST_CONSENT_TIME_MS = "last_consent_time_ms";
    static final long ONE_DAY_MS = 24 * 60 * 60 * 1000;
    private static final String ACTION_EDIT_EMERGENCY_CONTACTS =
            "android.emergency.EDIT_EMERGENCY_CONTACTS";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Protect against b/28401242 by enabling ViewInfoActivity.
        // We used to have code that disabled/enabled it and it could have been left in disabled
        // state.
        PackageManager pm = getPackageManager();
        pm.setComponentEnabledSetting(new ComponentName(this, ViewInfoActivity.class),
                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT, PackageManager.DONT_KILL_APP);
        setContentView(R.layout.edit_activity_layout);
        if (ACTION_EDIT_EMERGENCY_CONTACTS.equals(getIntent().getAction())) {
            // Select emergency contacts tab
            selectTab(1);
        }
        getWindow().addFlags(FLAG_DISMISS_KEYGUARD);
        setTaskDescription(new ActivityManager.TaskDescription(getString(R.string.emergency_info)));
    }
    @Override
    public void onResume() {
        super.onResume();
        long lastConsentTimeMs = PreferenceManager.getDefaultSharedPreferences(this)
                .getLong(KEY_LAST_CONSENT_TIME_MS, Long.MAX_VALUE);
        long nowMs = System.currentTimeMillis();
        // Check if at least one day has gone by since the user last gave his constant or if
        // the last consent was in the future (e.g. if the user changed the date).
        if (nowMs - lastConsentTimeMs > ONE_DAY_MS || lastConsentTimeMs > nowMs) {
            showWarningDialog();
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.edit_info_menu, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_clear_all:
                showClearAllDialog();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    @Override
    protected ArrayList<Pair<String, Fragment>> setUpFragments() {
        // Always return the two fragments in edit mode.
        ArrayList<Pair<String, Fragment>> fragments = new ArrayList<>(2);
        fragments.add(Pair.create(getResources().getString(R.string.tab_title_info),
                EditEmergencyInfoFragment.newInstance()));
        fragments.add(Pair.create(getResources().getString(R.string.tab_title_contacts),
                EditEmergencyContactsFragment.newInstance()));
        return fragments;
    }
    private void showWarningDialog() {
        final WarningDialogFragment previousFragment =
                (WarningDialogFragment) getFragmentManager()
                        .findFragmentByTag(EditInfoActivity.TAG_WARNING_DIALOG);
        if (previousFragment == null) {
            DialogFragment newFragment = WarningDialogFragment.newInstance();
            newFragment.setCancelable(false);
            newFragment.show(getFragmentManager(), TAG_WARNING_DIALOG);
        }
    }
    private void showClearAllDialog() {
        final ClearAllDialogFragment previousFragment =
                (ClearAllDialogFragment) getFragmentManager()
                        .findFragmentByTag(EditInfoActivity.TAG_CLEAR_ALL_DIALOG);
        if (previousFragment == null) {
            DialogFragment newFragment = ClearAllDialogFragment.newInstance();
            newFragment.show(getFragmentManager(), TAG_CLEAR_ALL_DIALOG);
        }
    }
    private void onClearAllPreferences() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        for (String key : PreferenceKeys.KEYS_EDIT_EMERGENCY_INFO) {
            sharedPreferences.edit().remove(key).apply();
        }
        sharedPreferences.edit().remove(PreferenceKeys.KEY_EMERGENCY_CONTACTS).apply();
        // Refresh the UI.
        ArrayList<Pair<String, Fragment>> fragments = getFragments();
        EditEmergencyInfoFragment editEmergencyInfoFragment =
                (EditEmergencyInfoFragment) fragments.get(0).second;
        editEmergencyInfoFragment.reloadFromPreference();
        EditEmergencyContactsFragment editEmergencyContactsFragment =
                (EditEmergencyContactsFragment) fragments.get(1).second;
        editEmergencyContactsFragment.reloadFromPreference();
    }
    /**
     * Warning dialog shown to the user each time they go in to the edit info view. Using a {@link
     * DialogFragment} takes care of screen rotation issues.
     */
    public static class WarningDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Dialog dialog = new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.user_emergency_info_title)
                    .setMessage(R.string.user_emergency_info_consent)
                    .setPositiveButton(R.string.emergency_info_continue,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    PreferenceManager.getDefaultSharedPreferences(
                                            getActivity()).edit()
                                            .putLong(KEY_LAST_CONSENT_TIME_MS,
                                                    System.currentTimeMillis()).apply();
                                }
                            })
                    .setNegativeButton(android.R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    getActivity().finish();
                                }
                            })
                    .create();
            dialog.setCanceledOnTouchOutside(false);
            return dialog;
        }
        public static DialogFragment newInstance() {
            return new WarningDialogFragment();
        }
    }
    /**
     * Dialog shown to the user when they tap on the CLEAR ALL menu item. Using a {@link
     * DialogFragment} takes care of screen rotation issues.
     */
    public static class ClearAllDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.clear_all_message)
                    .setPositiveButton(R.string.clear, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog1, int which) {
                            ((EditInfoActivity) getActivity()).onClearAllPreferences();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .create();
        }
        public static DialogFragment newInstance() {
            return new ClearAllDialogFragment();
        }
    }
}