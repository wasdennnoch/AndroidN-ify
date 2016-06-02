/*
 * Copyright (C) 2014 The Android Open Source Project
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
 * limitations under the License
 */

package tk.wasdennnoch.androidn_ify.utils;

import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

/**
 * Utility class to update the font size when the configuration has changed.
 */
public class FontSizeUtils {

    public static final float LARGE_TEXT_SCALE = 1.3f;

    public static void updateFontSize(View parent, int viewId, int dimensId) {
        updateFontSize((TextView) parent.findViewById(viewId), dimensId);
    }

    public static void updateFontSize(TextView v, int dimensId) {
        if (v != null) {
            v.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    v.getResources().getDimensionPixelSize(dimensId));
        }
    }
}