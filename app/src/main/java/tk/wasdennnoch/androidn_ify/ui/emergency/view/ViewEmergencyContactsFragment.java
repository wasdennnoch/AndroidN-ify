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
package tk.wasdennnoch.androidn_ify.ui.emergency.view;

import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.widget.ListView;

import java.util.Collections;

import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.ui.emergency.PreferenceKeys;
import tk.wasdennnoch.androidn_ify.ui.emergency.preferences.EmergencyContactsPreference;

/**
 * Fragment that displays emergency contacts.
 */
public class ViewEmergencyContactsFragment extends PreferenceFragment {
    /** The category that holds the emergency contacts. */
    private EmergencyContactsPreference mEmergencyContactsPreference;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.view_emergency_contacts);
        mEmergencyContactsPreference = (EmergencyContactsPreference)
                findPreference(PreferenceKeys.KEY_EMERGENCY_CONTACTS);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Set custom dividers
        ListView list = (ListView) getView().findViewById(android.R.id.list);
        list.setDivider(getResources().getDrawable(R.drawable.view_contact_divider));
    }


    @Override
    public void onResume() {
        super.onResume();
        mEmergencyContactsPreference.reloadFromPreference();
    }

    public static Fragment newInstance() {
        return new ViewEmergencyContactsFragment();
    }

    /** Returns true if there is at least one valid (still existing) emergency contact. */
    public static boolean hasAtLeastOneEmergencyContact(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String emergencyContactsString = "";
        try {
            emergencyContactsString = prefs.getString(PreferenceKeys.KEY_EMERGENCY_CONTACTS, "");
        } catch (ClassCastException e) {
            // Protect against b/28194605: We used to store the contacts using a string set.
            // If it is a string set, ignore its value. If it is not a string set it will throw
            // a ClassCastException
            prefs.getStringSet(
                    PreferenceKeys.KEY_EMERGENCY_CONTACTS,
                    Collections.<String>emptySet());
        }

        return !EmergencyContactsPreference.deserializeAndFilter(
                PreferenceKeys.KEY_EMERGENCY_CONTACTS,
                context,
                emergencyContactsString).isEmpty();
    }
}
