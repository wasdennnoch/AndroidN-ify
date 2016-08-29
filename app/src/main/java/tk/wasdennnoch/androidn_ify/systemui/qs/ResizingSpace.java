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

package tk.wasdennnoch.androidn_ify.systemui.qs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;

import tk.wasdennnoch.androidn_ify.utils.ResourceUtils;

public class ResizingSpace extends View {

    private final int mWidth;
    private final int mHeight;

    public ResizingSpace(Context context, int width, int height) {
        super(context, null);
        if (getVisibility() == VISIBLE) {
            setVisibility(INVISIBLE);
        }
        mWidth = width;
        mHeight = height;
        setLayoutParams(new ViewGroup.MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        updateDimens();
    }

    public ResizingSpace(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (getVisibility() == VISIBLE) {
            setVisibility(INVISIBLE);
        }
        TypedArray a = context.obtainStyledAttributes(attrs, com.android.internal.R.styleable.ViewGroup_Layout);
        mWidth = a.getResourceId(com.android.internal.R.styleable.ViewGroup_Layout_layout_width, 0);
        mHeight = a.getResourceId(com.android.internal.R.styleable.ViewGroup_Layout_layout_height, 0);
        a.recycle();
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateDimens();
    }

    private void updateDimens() {
        LayoutParams params = getLayoutParams();
        boolean changed = false;
        Resources res = ResourceUtils.getInstance(getContext()).getResources();
        if (mWidth > 0) {
            int width = res.getDimensionPixelOffset(mWidth);
            if (width != params.width) {
                params.width = width;
                changed = true;
            }
        }
        if (mHeight > 0) {
            int height = res.getDimensionPixelOffset(mHeight);
            if (height != params.height) {
                params.height = height;
                changed = true;
            }
        }
        if (changed) {
            setLayoutParams(params);
        }
    }

    /**
     * Draw nothing.
     *
     * @param canvas an unused parameter.
     */
    @SuppressLint("MissingSuperCall")
    @Override
    public void draw(Canvas canvas) {
    }

    /**
     * Compare to: {@link View#getDefaultSize(int, int)}
     * If mode is AT_MOST, return the child size instead of the parent size
     * (unless it is too big).
     */
    private static int getDefaultSize2(int size, int measureSpec) {
        int result = size;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        switch (specMode) {
            case MeasureSpec.UNSPECIFIED:
                result = size;
                break;
            case MeasureSpec.AT_MOST:
                result = Math.min(size, specSize);
                break;
            case MeasureSpec.EXACTLY:
                result = specSize;
                break;
        }
        return result;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(
                getDefaultSize2(getSuggestedMinimumWidth(), widthMeasureSpec),
                getDefaultSize2(getSuggestedMinimumHeight(), heightMeasureSpec));
    }

}