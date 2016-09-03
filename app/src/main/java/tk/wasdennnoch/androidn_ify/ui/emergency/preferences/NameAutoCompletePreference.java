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
import android.os.UserManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.ArrayAdapter;

import tk.wasdennnoch.androidn_ify.ui.emergency.ReloadablePreferenceInterface;

/**
 * {@link AutoCompleteEditTextPreference} that prepopulates the edit text view with the name of the
 * user provided in settings.
 */
public class NameAutoCompletePreference extends AutoCompleteEditTextPreference implements
        ReloadablePreferenceInterface {
    public NameAutoCompletePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        getAutoCompleteTextView().setAdapter(createAdapter());
    }

    private ArrayAdapter createAdapter() {
        UserManager userManager =
                (UserManager) getContext().getSystemService(Context.USER_SERVICE);
        String[] autocompleteSuggestions = {userManager.getUserName()};
        return new ArrayAdapter<>(getContext(),
                android.R.layout.simple_dropdown_item_1line, autocompleteSuggestions);
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
}
