/*
 * Copyright (C) 2014 The Android Open Source Project
 * Copyright (C) 2015 The CyanogenMod Project
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

package tk.wasdennnoch.androidn_ify.notifications.qs;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.util.MathUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;

import java.util.Objects;

import tk.wasdennnoch.androidn_ify.utils.FontSizeUtils;

/** View that represents a standard quick settings tile. **/
public class FakeQSTileView extends ViewGroup {
    private static final Typeface CONDENSED = Typeface.create("sans-serif-condensed",
            Typeface.NORMAL);

    private static final String PACKAGE_SYSTEMUI = "com.android.systemui";

    protected final Context mContext;
    private final View mIcon;
    private final int mIconSizePx;
    private final int mTileSpacingPx;
    private int mTilePaddingTopPx;
    private final int mTilePaddingBelowIconPx;
    private final View mTopBackgroundView;

    private TextView mLabel;
    private boolean mDual;
    private Drawable mTileBackground;
    private RippleDrawable mRipple;

    public FakeQSTileView(Context context) {
        super(context);

        mContext = context;
        final Resources res = context.getResources();
        mIconSizePx = res.getDimensionPixelSize(res.getIdentifier("qs_tile_icon_size", "dimen", PACKAGE_SYSTEMUI));
        mTileSpacingPx = res.getDimensionPixelSize(res.getIdentifier("qs_tile_spacing", "dimen", PACKAGE_SYSTEMUI));
        mTilePaddingBelowIconPx =  res.getDimensionPixelSize(res.getIdentifier("qs_tile_padding_below_icon", "dimen", PACKAGE_SYSTEMUI));
        mTileBackground = newTileBackground();
        recreateLabel();
        setClipChildren(false);

        mTopBackgroundView = new View(context);
        mTopBackgroundView.setId(View.generateViewId());
        addView(mTopBackgroundView);

        mIcon = createIcon();
        addView(mIcon);

        setClickable(true);
        updateTopPadding();
        setId(View.generateViewId());

        setDual(false);
    }

    private void updateTopPadding() {
        Resources res = getResources();
        int padding = res.getDimensionPixelSize(res.getIdentifier("qs_tile_padding_top", "dimen", PACKAGE_SYSTEMUI));
        int largePadding = res.getDimensionPixelSize(res.getIdentifier("qs_tile_padding_top_large_text", "dimen", PACKAGE_SYSTEMUI));
        float largeFactor = (MathUtils.constrain(getResources().getConfiguration().fontScale,
                1.0f, FontSizeUtils.LARGE_TEXT_SCALE) - 1f) / (FontSizeUtils.LARGE_TEXT_SCALE - 1f);
        mTilePaddingTopPx = Math.round((1 - largeFactor) * padding + largeFactor * largePadding);
        requestLayout();
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateTopPadding();
        FontSizeUtils.updateFontSize(mLabel, getResources().getIdentifier("qs_tile_text_size", "dimen", PACKAGE_SYSTEMUI));
    }

    @SuppressWarnings("deprecation")
    private void recreateLabel() {
        CharSequence labelText = null;
        if (mLabel != null && mLabel.isAttachedToWindow()) {
            labelText = mLabel.getText();
            removeView(mLabel);
        }
        final Resources res = mContext.getResources();
        if (mLabel == null) {
            mLabel = new TextView(mContext);
            mLabel.setTextColor(res.getColor(res.getIdentifier("qs_tile_text", "color", PACKAGE_SYSTEMUI)));
            mLabel.setGravity(Gravity.CENTER_HORIZONTAL);
            mLabel.setMinLines(2);
            mLabel.setPadding(0, 0, 0, 0);
            mLabel.setTypeface(CONDENSED);
            mLabel.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    res.getDimensionPixelSize(res.getIdentifier("qs_tile_text_size", "dimen", PACKAGE_SYSTEMUI)));
            mLabel.setClickable(false);
            mLabel.setFocusable(false);
        }
        if (labelText != null) {
            mLabel.setText(labelText);
        }
        addView(mLabel);
    }

    public boolean setDual(boolean dual) {
        final boolean changed = dual != mDual;
        mDual = dual;
        if (changed) {
            recreateLabel();
        }

        mTopBackgroundView.setOnClickListener(null);
        mTopBackgroundView.setOnLongClickListener(null);
        setTileBackground();
        mTopBackgroundView.setClickable(dual);
        mTopBackgroundView.setFocusable(dual);
        setClickable(!dual);
        setFocusable(!dual);
        mTopBackgroundView.setVisibility(dual ? VISIBLE : GONE);

        if (changed) {
            getParent().requestLayout();
        }
        postInvalidate();
        return changed;
    }

    protected void setTileBackground() {
        if (mTileBackground instanceof RippleDrawable) {
            setRipple((RippleDrawable) mTileBackground);
        } else {
            setRipple(null);
        }
        mTopBackgroundView.setBackground(mDual ? mTileBackground : null);
        setBackground(!mDual ? mTileBackground : null);
    }

    private void setRipple(RippleDrawable tileBackground) {
        mRipple = tileBackground;
        if (getWidth() != 0 && mRipple != null) {
            updateRippleSize(getWidth(), getHeight());
        }
    }

    protected View createIcon() {
        final ImageView icon = new ImageView(mContext);
        icon.setId(android.R.id.icon);
        icon.setScaleType(ScaleType.CENTER_INSIDE);
        return icon;
    }

    public Drawable newTileBackground() {
        final int[] attrs = new int[] { android.R.attr.selectableItemBackgroundBorderless };
        final TypedArray ta = mContext.obtainStyledAttributes(attrs);
        final Drawable d = ta.getDrawable(0);
        ta.recycle();
        return d;
    }

    private View labelView() {
        return mLabel;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int w = MeasureSpec.getSize(widthMeasureSpec);
        final int h = MeasureSpec.getSize(heightMeasureSpec);
        final int iconSpec = exactly(mIconSizePx);
        mIcon.measure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.AT_MOST), iconSpec);
        labelView().measure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(h, MeasureSpec.AT_MOST));
        int heightSpec = exactly(
                mIconSizePx + mTilePaddingBelowIconPx + mTilePaddingTopPx);
        mTopBackgroundView.measure(widthMeasureSpec, heightSpec);
        setMeasuredDimension(w, h);
    }

    private static int exactly(int size) {
        return MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int w = getMeasuredWidth();
        final int h = getMeasuredHeight();

        layout(mTopBackgroundView, 0, mTileSpacingPx);

        int top = 0;
        top += mTileSpacingPx;
        top += mTilePaddingTopPx;
        final int iconLeft = (w - mIcon.getMeasuredWidth()) / 2;
        layout(mIcon, iconLeft, top);
        if (mRipple != null) {
            updateRippleSize(w, h);

        }
        top = mIcon.getBottom();
        top += mTilePaddingBelowIconPx;
        layout(labelView(), 0, top);
    }

    private void updateRippleSize(int width, int height) {
        // center the touch feedback on the center of the icon, and dial it down a bit
        final int cx = width / 2;
        final int cy = mDual ? mIcon.getTop() + mIcon.getHeight() : height / 2;
        final int rad = (int)(mIcon.getHeight() * 1.25f);
        mRipple.setHotspotBounds(cx - rad, cy - rad, cx + rad, cy + rad);
    }

    private static void layout(View child, int left, int top) {
        child.layout(left, top, left + child.getMeasuredWidth(), top + child.getMeasuredHeight());
    }

    @SuppressWarnings("deprecation")
    protected void handleStateChanged(Drawable d, String label) {
        if (mIcon instanceof ImageView) {
            setIcon((ImageView) mIcon, d);
        }
        mLabel.setText(label);
        if (!mLabel.isEnabled()) {
            mLabel.setEnabled(true);
            mLabel.setTextColor(mContext.getResources().getColor(getResources().getIdentifier("qs_tile_text", "color", PACKAGE_SYSTEMUI)));
        }
    }

    protected void setIcon(ImageView iv, Drawable d) {
        int tagId = getResources().getIdentifier("qs_icon_tag", "id", PACKAGE_SYSTEMUI);
        if (!Objects.equals(d, iv.getTag(tagId))) {
            iv.setImageDrawable(d);
            iv.setTag(tagId, d);
        }
        if (!iv.isEnabled()) {
            iv.setEnabled(true);
            iv.setColorFilter(null);
        }
    }
}