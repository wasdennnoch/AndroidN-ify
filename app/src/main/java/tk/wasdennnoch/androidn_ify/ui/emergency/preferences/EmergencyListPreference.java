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
import android.content.res.TypedArray;
import android.preference.ListPreference;
import android.support.annotation.Nullable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.TtsSpan;
import android.util.AttributeSet;

import com.android.internal.annotations.VisibleForTesting;

import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.ui.emergency.ReloadablePreferenceInterface;

/**
 * Custom {@link ListPreference} that allows us to refresh and update the summary.
 */
public class EmergencyListPreference extends ListPreference
        implements ReloadablePreferenceInterface {
    @Nullable
    private CharSequence[] mContentDescriptions;

    public EmergencyListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.EmergencyListPreference);
        mContentDescriptions =
                a.getTextArray(R.styleable.EmergencyListPreference_entryContentDescriptions);
        a.recycle();

        if (mContentDescriptions != null) {
            // Override entries with accessible entries.
            setEntries(createAccessibleEntries(getEntries(), mContentDescriptions));
        }
    }

    @Override
    public void reloadFromPreference() {
        setValue(getPersistedString(""));
    }

    @Override
    public boolean isNotSet() {
        return TextUtils.isEmpty(getValue());
    }

    @Override
    public CharSequence getSummary() {
        final String value = getValue();
        int index = findIndexOfValue(value);
        if (index < 0) {
            return super.getSummary();
        } else {
            return getEntry();
        }
    }

    private static CharSequence[] createAccessibleEntries(CharSequence entries[],
                                                          CharSequence[] contentDescriptions) {
        CharSequence[] accessibleEntries = new CharSequence[entries.length];
        for (int i = 0; i < entries.length; i++) {
            accessibleEntries[i] = createAccessibleSequence(entries[i], contentDescriptions[i]);
        }
        return accessibleEntries;
    }

    private static SpannableString createAccessibleSequence(CharSequence displayText,
                                                            CharSequence accessibleText) {
        SpannableString str = new SpannableString(displayText);
        str.setSpan(new TtsSpan.TextBuilder((String) accessibleText).build(), 0,
                displayText.length(),
                Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        return str;
    }
}
