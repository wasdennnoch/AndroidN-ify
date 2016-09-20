/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package tk.wasdennnoch.androidn_ify.extracted.systemui;

import android.content.Context;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.widget.ImageView;

import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.utils.ResourceUtils;

public class ExpandableIndicator extends ImageView {

    private boolean mExpanded;
    private ResourceUtils mRes;

    public ExpandableIndicator(Context context) {
        super(context);
        mRes = ResourceUtils.getInstance(context);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        final int res = getDrawableResourceId(mExpanded);
        setImageDrawable(mRes.getDrawable(res));
        setContentDescription(getContentDescription(mExpanded));
    }

    public void setExpanded(boolean expanded) {
        if (expanded == mExpanded) return;
        mExpanded = expanded;
        final int res = getDrawableResourceId(!mExpanded);
        // workaround to reset drawable
        //noinspection ConstantConditions
        final AnimatedVectorDrawable avd = (AnimatedVectorDrawable) mRes
                .getDrawable(res).getConstantState().newDrawable();
        setImageDrawable(avd);
        avd.start();
        setContentDescription(getContentDescription(expanded));
    }

    public boolean isExpanded() {
        return mExpanded;
    }

    private int getDrawableResourceId(boolean expanded) {
        return expanded ? R.drawable.ic_volume_collapse_animation
                : R.drawable.ic_volume_expand_animation;
    }

    private String getContentDescription(boolean expanded) {
        return expanded ? ResourceUtils.getInstance(getContext()).getString(R.string.accessibility_quick_settings_collapse)
                : ResourceUtils.getInstance(getContext()).getString(R.string.accessibility_quick_settings_expand);
    }
}