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
import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.provider.ContactsContract;

import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.ui.emergency.PreferenceKeys;
import tk.wasdennnoch.androidn_ify.ui.emergency.preferences.EmergencyContactsPreference;

/**
 * Fragment that displays emergency contacts. These contacts can be added or removed.
 */
public class EditEmergencyContactsFragment extends PreferenceFragment {
    /** Result code for contact picker */
    private static final int CONTACT_PICKER_RESULT = 1001;
    /** The category that holds the emergency contacts. */
    private EmergencyContactsPreference mEmergencyContactsPreferenceCategory;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.edit_emergency_contacts);
        mEmergencyContactsPreferenceCategory = (EmergencyContactsPreference)
                findPreference(PreferenceKeys.KEY_EMERGENCY_CONTACTS);
        Preference addEmergencyContact = findPreference(PreferenceKeys.KEY_ADD_CONTACT);
        addEmergencyContact.setOnPreferenceClickListener(new Preference
                .OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                // By using ContactsContract.CommonDataKinds.Phone.CONTENT_URI, the user is
                // presented with a list of contacts, with one entry per phone number.
                // The selected contact is guaranteed to have a name and phone number.
                Intent contactPickerIntent = new Intent(Intent.ACTION_PICK,
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
                startActivityForResult(contactPickerIntent,
                        CONTACT_PICKER_RESULT);
                return true;
            }
        });
    }
    @Override
    public void onResume() {
        super.onResume();
        reloadFromPreference();
    }
    /** Reloads the contacts by reading the value from the shared preferences. */
    public void reloadFromPreference() {
        if (mEmergencyContactsPreferenceCategory != null) {
            mEmergencyContactsPreferenceCategory.reloadFromPreference();
        }
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CONTACT_PICKER_RESULT && resultCode == Activity.RESULT_OK) {
            Uri uri = data.getData();
            mEmergencyContactsPreferenceCategory.addNewEmergencyContact(uri);
        }
    }
    public static Fragment newInstance() {
        return new EditEmergencyContactsFragment();
    }
}