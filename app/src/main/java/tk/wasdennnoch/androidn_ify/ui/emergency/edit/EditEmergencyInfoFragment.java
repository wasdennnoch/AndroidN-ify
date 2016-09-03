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
import android.app.Fragment;
import android.os.Bundle;
import android.preference.PreferenceFragment;

import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.ui.emergency.PreferenceKeys;
import tk.wasdennnoch.androidn_ify.ui.emergency.ReloadablePreferenceInterface;

/**
 * Fragment that displays personal and medical information.
 */
public class EditEmergencyInfoFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.edit_emergency_info);
    }
    @Override
    public void onResume() {
        super.onResume();
        reloadFromPreference();
    }
    /** Reloads all the preferences by reading the value from the shared preferences. */
    public void reloadFromPreference() {
        for (String preferenceKey : PreferenceKeys.KEYS_EDIT_EMERGENCY_INFO) {
            ReloadablePreferenceInterface preference = (ReloadablePreferenceInterface)
                    findPreference(preferenceKey);
            if (preference != null) {
                preference.reloadFromPreference();
            }
        }
    }
    public static Fragment newInstance() {
        return new EditEmergencyInfoFragment();
    }
}