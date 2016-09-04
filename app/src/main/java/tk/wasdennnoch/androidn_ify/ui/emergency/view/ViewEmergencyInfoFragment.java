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
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.ui.emergency.PreferenceKeys;
import tk.wasdennnoch.androidn_ify.ui.emergency.ReloadablePreferenceInterface;

/**
 * Fragment that displays personal and medical information.
 */
public class ViewEmergencyInfoFragment extends PreferenceFragment {
    /** A list with all the preferences. */
    private final List<Preference> mPreferences = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.view_emergency_info);

        for (String preferenceKey : PreferenceKeys.KEYS_VIEW_EMERGENCY_INFO) {
            Preference preference = findPreference(preferenceKey);
            mPreferences.add(preference);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        for (Preference preference : mPreferences) {
            ReloadablePreferenceInterface reloadablePreference =
                    (ReloadablePreferenceInterface) preference;
            reloadablePreference.reloadFromPreference();
            if (reloadablePreference.isNotSet()) {
                getPreferenceScreen().removePreference(preference);
            } else {
                // Note: this preference won't be added it if it already exists.
                getPreferenceScreen().addPreference(preference);
            }
        }
    }

    public static Fragment newInstance() {
        return new ViewEmergencyInfoFragment();
    }

    /** Returns true if there is at least one preference set. */
    public static boolean hasAtLeastOnePreferenceSet(Context context) {
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(context);
        for (String key : PreferenceKeys.KEYS_VIEW_EMERGENCY_INFO) {
            if (!TextUtils.isEmpty(sharedPreferences.getString(key, ""))) {
                return true;
            }
        }
        return false;
    }
}
