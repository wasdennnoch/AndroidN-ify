/*
 * Copyright (C) 2014 The Android Open Source Project
 * Copyright (C) 2016 MrWasdennnoch@XDA
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
package tk.wasdennnoch.androidn_ify.ui.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import java.util.ArrayList;

import tk.wasdennnoch.androidn_ify.R;

public class DropDownPreference extends Preference {

    private Context mContext;
    private String mSummary;
    private ArrayAdapter<String> mAdapter;
    private Spinner mSpinner;
    private ArrayList<String> mValues = new ArrayList<>();
    private String mValue;

    private int mSelectedPosition = -1;

    @SuppressWarnings("unused")
    public DropDownPreference(Context context) {
        this(context, null);
    }

    public DropDownPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mAdapter = new ArrayAdapter<>(mContext, android.R.layout.simple_spinner_dropdown_item);

        mSpinner = new Spinner(mContext);

        mSpinner.setVisibility(View.INVISIBLE);
        mSpinner.setAdapter(mAdapter);
        mSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                setSelectedItem(position, true);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // noop
            }
        });
        setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                mSpinner.performClick();
                return true;
            }
        });

        // Support XML specification like ListPreferences do.
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.DropDownPreference);
        mSummary = a.getString(R.styleable.DropDownPreference_android_summary);
        CharSequence[] entries = a.getTextArray(R.styleable.DropDownPreference_android_entries);
        CharSequence[] values = a.getTextArray(R.styleable.DropDownPreference_android_entryValues);
        if (entries != null && values != null) {
            for (int i = 0; i < entries.length; i++) {
                addItem(entries[i].toString(), values[i].toString());
            }
        }
        a.recycle();
    }

    public void setSelectedItem(int position) {
        setSelectedItem(position, false);
    }

    public void setSelectedItem(int position, boolean fromSpinner) {
        if (fromSpinner && position == mSelectedPosition) {
            return;
        }
        String value = mValues.get(position);
        if (!callChangeListener(value)) {
            return;
        }
        mValue = value;
        mSpinner.setSelection(position);
        mSelectedPosition = mSpinner.getSelectedItemPosition();
        if (isPersistent()) {
            persistString(value);
        }
        notifyChanged();
        boolean disableDependents = value == null;
        notifyDependencyChange(disableDependents);
    }

    public void setSelectedValue(String value) {
        int i = mValues.indexOf(value);
        if (i > -1) {
            setSelectedItem(i);
        }
    }

    public String getValue() {
        return mValue;
    }

    public void addItem(String caption, String value) {
        mAdapter.add(caption);
        mValues.add(value);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        if (view.equals(mSpinner.getParent())) return;
        if (mSpinner.getParent() != null) {
            ((ViewGroup) mSpinner.getParent()).removeView(mSpinner);
        }
        ViewGroup vg = (ViewGroup) view;
        vg.addView(mSpinner, 0);
        ViewGroup.LayoutParams lp = mSpinner.getLayoutParams();
        lp.width = 0;
        mSpinner.setLayoutParams(lp);
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        setSelectedValue(restoreValue ? getPersistedString(mValue) : (String) defaultValue);
    }

    @Override
    public void setSummary(CharSequence summary) {
        super.setSummary(summary);
        if (summary == null && mSummary != null || summary != null && !summary.equals(mSummary)) {
            mSummary = summary == null ? null : summary.toString();
        }
    }

    @Override
    public CharSequence getSummary() {
        if (mSummary == null) {
            return super.getSummary();
        } else {
            final CharSequence entry = mAdapter.getItem(mValues.indexOf(mValue));
            return String.format(mSummary, entry == null ? "" : entry);
        }
    }

}
