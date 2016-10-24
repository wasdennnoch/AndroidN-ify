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

package tk.wasdennnoch.androidn_ify.extracted.systemui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * An ImageView which supports an attribute specifying whether it has overlapping rendering
 * commands and therefore does not need a layer when alpha is changed.
 */
public class AlphaOptimizedImageView extends ImageView {

    public AlphaOptimizedImageView(Context context) {
        this(context, null /* attrs */);
    }

    public AlphaOptimizedImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0 /* defStyleAttr */);
    }

    public AlphaOptimizedImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0 /* defStyleRes */);
    }

    public AlphaOptimizedImageView(Context context, AttributeSet attrs, int defStyleAttr,
                                   int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }
}