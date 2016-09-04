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
package tk.wasdennnoch.androidn_ify.ui.emergency.preferences;

import android.content.Context;
import android.preference.EditTextPreference;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import tk.wasdennnoch.androidn_ify.ui.emergency.ReloadablePreferenceInterface;

/**
 * Custom {@link EditTextPreference} that allows us to refresh and update the summary.
 */
public class EmergencyEditTextPreference extends EditTextPreference
        implements ReloadablePreferenceInterface {

    private static final int MAX_LINES = 50;

    public EmergencyEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void reloadFromPreference() {
        setText(getPersistedString(""));
    }

    @Override
    public boolean isNotSet() {
        return TextUtils.isEmpty(getText());
    }

    @Override
    public CharSequence getSummary() {
        String text = getText();
        return TextUtils.isEmpty(text) ? super.getSummary() : text;
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        final TextView summaryView = (TextView) view.findViewById(
                com.android.internal.R.id.summary);
        summaryView.setMaxLines(MAX_LINES);
    }
}
