/*
 * Copyright (C) 2015 The Android Open Source Project
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

package tk.wasdennnoch.androidn_ify.systemui.notifications;

import android.annotation.Nullable;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.RemotableViewMethod;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Chronometer;
import android.widget.DateTimeView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.TextView;

import java.util.ArrayList;

import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.utils.ResourceUtils;

/**
 * A header of a notification view
 */
@RemoteViews.RemoteView
public class NotificationHeaderView extends ViewGroup {
    public static final int NO_COLOR = -1;
    private final int mChildMinWidth;
    private final int mContentEndMargin;
    private View mAppName;
    private View mHeaderText;
    private OnClickListener mExpandClickListener;
    private HeaderTouchListener mTouchListener = new HeaderTouchListener();
    private ImageView mExpandButton;
    private View mIcon;
    private View mProfileBadge;
    private View mInfo;
    private int mIconColor;
    private int mOriginalNotificationColor;
    private boolean mExpanded;
    private boolean mShowWorkBadgeAtEnd;
    private Drawable mBackground;
    private int mHeaderBackgroundHeight;

    private Resources mModuleRes;

    @SuppressWarnings("deprecation")
    public static NotificationHeaderView newHeader(Context context) {
        ResourceUtils res = ResourceUtils.getInstance(context);

        FrameLayout.LayoutParams headerLp = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, res.getDimensionPixelSize(R.dimen.notification_header_height));
        NotificationHeaderView headerView = new NotificationHeaderView(context);
        headerView.setClipChildren(false);
        headerView.setId(R.id.notification_header);
        headerView.setPadding(
                res.getDimensionPixelSize(R.dimen.notification_content_margin_start),
                res.getDimensionPixelSize(R.dimen.notification_header_padding_top),
                res.getDimensionPixelSize(R.dimen.notification_header_padding_end),
                res.getDimensionPixelSize(R.dimen.notification_header_padding_bottom));
        headerView.setLayoutParams(headerLp);

        int iconSize = res.getDimensionPixelSize(R.dimen.notification_icon_size);
        int iconMarginEnd = res.getDimensionPixelSize(R.dimen.notification_icon_margin_end);

        ViewGroup.MarginLayoutParams iconLp = new MarginLayoutParams(iconSize, iconSize);
        iconLp.setMarginEnd(iconMarginEnd);
        ImageView icon = new ImageView(context);
        icon.setId(R.id.icon);
        icon.setLayoutParams(iconLp);
        headerView.addView(icon);

        MarginLayoutParams appNameLp = new MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        appNameLp.setMarginStart(res.getDimensionPixelSize(R.dimen.notification_app_name_margin_start));
        appNameLp.setMarginEnd(res.getDimensionPixelSize(R.dimen.notification_app_name_margin_end));
        TextView appName = new TextView(context);
        appName.setId(R.id.app_name_text);
        appName.setLayoutParams(appNameLp);
        appName.setSingleLine(true);
        appName.setTextAppearance(context, android.R.style.TextAppearance_Material_Notification_Info);
        headerView.addView(appName);

        TextView headerTextDivider = newDivider(context, res);
        headerTextDivider.setId(R.id.header_text_divider);
        headerView.addView(headerTextDivider);

        int textMargin = res.getDimensionPixelSize(R.dimen.notification_header_text_margin);

        MarginLayoutParams headerTextLp = new MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        headerTextLp.setMargins(textMargin, 0, textMargin, 0);
        TextView headerText = new TextView(context);
        headerText.setId(R.id.header_text);
        headerText.setLayoutParams(headerTextLp);
        headerText.setSingleLine(true);
        headerText.setVisibility(GONE);
        headerText.setTextAppearance(context, android.R.style.TextAppearance_Material_Notification_Info);
        headerView.addView(headerText);

        TextView timeDivider = newDivider(context, res);
        timeDivider.setId(R.id.time_divider);
        headerView.addView(timeDivider);

        MarginLayoutParams timeLp = new MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        timeLp.setMargins(textMargin, 0, textMargin, 0);
        RelativeDateTimeView time = new RelativeDateTimeView(context);
        time.setId(R.id.time);
        time.setLayoutParams(timeLp);
        time.setSingleLine(true);
        time.setVisibility(GONE);
        time.setTextAppearance(context, android.R.style.TextAppearance_Material_Notification_Time);
        time.setGravity(TEXT_ALIGNMENT_CENTER);
        headerView.addView(time);

        MarginLayoutParams chronometerLp = new MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        chronometerLp.setMargins(textMargin, 0, textMargin, 0);
        Chronometer chronometer = new Chronometer(context);
        chronometer.setId(R.id.chronometer);
        chronometer.setLayoutParams(chronometerLp);
        chronometer.setSingleLine(true);
        chronometer.setVisibility(GONE);
        chronometer.setTextAppearance(context, android.R.style.TextAppearance_Material_Notification_Time);
        headerView.addView(chronometer);

        int expandButtonPaddingTop = res.getDimensionPixelSize(R.dimen.notification_header_expand_button_padding_top);

        MarginLayoutParams expandButtonLp = new MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        ImageView expandButton = new ImageView(context);
        expandButton.setId(R.id.expand_button);
        expandButton.setLayoutParams(expandButtonLp);
        expandButton.setVisibility(GONE);
        expandButton.setPadding(0, expandButtonPaddingTop, 0, 0);
        headerView.addView(expandButton);

        headerView.initViews();

        /*
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ViewGroup template = (ViewGroup) inflater.inflate(res.getLayout(R.layout.notification_template_header), null, false);
        int childCount = template.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = template.getChildAt(0);
            template.removeView(child);
            headerView.addView(child);
        }

        String dividerSymbol = res.getString(R.string.notification_header_divider_symbol);
        ((TextView) headerView.findViewById(R.id.header_text_divider)).setText(dividerSymbol);
        ((TextView) headerView.findViewById(R.id.time_divider)).setText(dividerSymbol);
        */

        return headerView;
    }

    @SuppressWarnings("deprecation")
    private static TextView newDivider(Context context, ResourceUtils res) {
        int dividerMargin = res.getDimensionPixelSize(R.dimen.notification_header_text_margin);
        String dividerSymbol = res.getString(R.string.notification_header_divider_symbol);
        MarginLayoutParams dividerLp = new MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        dividerLp.setMargins(dividerMargin, 0, dividerMargin, 0);
        TextView divider = new TextView(context);
        divider.setLayoutParams(dividerLp);
        divider.setVisibility(GONE);
        divider.setTextAppearance(context, android.R.style.TextAppearance_Material_Notification_Info);
        divider.setText(dividerSymbol);
        return divider;
    }

    ViewOutlineProvider mProvider = new ViewOutlineProvider() {
        @Override
        public void getOutline(View view, Outline outline) {
            if (mBackground != null) {
                outline.setRect(0, 0, getWidth(), mHeaderBackgroundHeight);
                outline.setAlpha(1f);
            }
        }
    };
    final AccessibilityDelegate mExpandDelegate = new AccessibilityDelegate() {

        @Override
        public boolean performAccessibilityAction(View host, int action, Bundle args) {
            if (super.performAccessibilityAction(host, action, args)) {
                return true;
            }
            if (action == AccessibilityNodeInfo.ACTION_COLLAPSE
                    || action == AccessibilityNodeInfo.ACTION_EXPAND) {
                mExpandClickListener.onClick(mExpandButton);
                return true;
            }
            return false;
        }

        @Override
        public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo info) {
            super.onInitializeAccessibilityNodeInfo(host, info);
            // Avoid that the button description is also spoken
            info.setClassName(getClass().getName());
            if (mExpanded) {
                info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_COLLAPSE);
            } else {
                info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_EXPAND);
            }
        }
    };

    public NotificationHeaderView(Context context) {
        this(context, null);
    }

    public NotificationHeaderView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NotificationHeaderView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public NotificationHeaderView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        mModuleRes = ResourceUtils.getInstance(context).getResources();

        mChildMinWidth = mModuleRes.getDimensionPixelSize(
                R.dimen.notification_header_shrink_min_width);
        mContentEndMargin = mModuleRes.getDimensionPixelSize(
                R.dimen.notification_content_margin_end);
        mHeaderBackgroundHeight = mModuleRes.getDimensionPixelSize(
                R.dimen.notification_header_background_height);
    }

    private void initViews() {
        mAppName = findViewById(R.id.app_name_text);
        mHeaderText = findViewById(R.id.header_text);
        mExpandButton = (ImageView) findViewById(R.id.expand_button);
        if (mExpandButton != null) {
            mExpandButton.setAccessibilityDelegate(mExpandDelegate);
        }
        mIcon = findViewById(com.android.internal.R.id.icon);
        mProfileBadge = findViewById(R.id.profile_badge);

        post(new Runnable() {
            @Override
            public void run() {
                Rect r = new Rect();
                r.top = 0;
                r.bottom = (int) (32 * getResources().getDisplayMetrics().density);
                r.left = 0;
                r.right = getWidth();

                TouchDelegate touchDelegate = new TouchDelegate(r, mExpandButton);
                setTouchDelegate(touchDelegate);
            }
        });

        updateTouchListener();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        initViews();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int givenWidth = MeasureSpec.getSize(widthMeasureSpec);
        final int givenHeight = MeasureSpec.getSize(heightMeasureSpec);
        int wrapContentWidthSpec = MeasureSpec.makeMeasureSpec(givenWidth,
                MeasureSpec.AT_MOST);
        int wrapContentHeightSpec = MeasureSpec.makeMeasureSpec(givenHeight,
                MeasureSpec.AT_MOST);
        int totalWidth = getPaddingStart() + getPaddingEnd();
        for (int i = 0; i < getChildCount(); i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() == GONE) {
                // We'll give it the rest of the space in the end
                continue;
            }
            final MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();
            int childWidthSpec = getChildMeasureSpec(wrapContentWidthSpec,
                    lp.leftMargin + lp.rightMargin, lp.width);
            int childHeightSpec = getChildMeasureSpec(wrapContentHeightSpec,
                    lp.topMargin + lp.bottomMargin, lp.height);
            child.measure(childWidthSpec, childHeightSpec);
            totalWidth += lp.leftMargin + lp.rightMargin + child.getMeasuredWidth();
        }
        if (totalWidth > givenWidth) {
            int overFlow = totalWidth - givenWidth;
            // We are overflowing, lets shrink the app name first
            final int appWidth = mAppName.getMeasuredWidth();
            if (overFlow > 0 && mAppName.getVisibility() != GONE && appWidth > mChildMinWidth) {
                int newSize = appWidth - Math.min(appWidth - mChildMinWidth, overFlow);
                int childWidthSpec = MeasureSpec.makeMeasureSpec(newSize, MeasureSpec.AT_MOST);
                mAppName.measure(childWidthSpec, wrapContentHeightSpec);
                overFlow -= appWidth - newSize;
            }
            // still overflowing, finaly we shrink the header text
            if (overFlow > 0 && mHeaderText.getVisibility() != GONE) {
                // we're still too big
                final int textWidth = mHeaderText.getMeasuredWidth();
                int newSize = Math.max(0, textWidth - overFlow);
                int childWidthSpec = MeasureSpec.makeMeasureSpec(newSize, MeasureSpec.AT_MOST);
                mHeaderText.measure(childWidthSpec, wrapContentHeightSpec);
            }
        }
        setMeasuredDimension(givenWidth, givenHeight);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int left = getPaddingStart();
        int childCount = getChildCount();
        int ownHeight = getHeight() - getPaddingTop() - getPaddingBottom();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) {
                continue;
            }
            int childHeight = child.getMeasuredHeight();
            MarginLayoutParams params = (MarginLayoutParams) child.getLayoutParams();
            left += params.getMarginStart();
            int right = left + child.getMeasuredWidth();
            int top = (int) (getPaddingTop() + (ownHeight - childHeight) / 2.0f);
            int bottom = top + childHeight;
            int layoutLeft = left;
            int layoutRight = right;
            if (child == mProfileBadge) {
                int paddingEnd = getPaddingEnd();
                if (mShowWorkBadgeAtEnd) {
                    paddingEnd = mContentEndMargin;
                }
                layoutRight = getWidth() - paddingEnd;
                layoutLeft = layoutRight - child.getMeasuredWidth();
            }
            if (getLayoutDirection() == LAYOUT_DIRECTION_RTL) {
                int ltrLeft = layoutLeft;
                layoutLeft = getWidth() - layoutRight;
                layoutRight = getWidth() - ltrLeft;
            }
            child.layout(layoutLeft, top, layoutRight, bottom);
            left = right + params.getMarginEnd();
        }
        updateTouchListener();
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new ViewGroup.MarginLayoutParams(getContext(), attrs);
    }

    /**
     * Set a {@link Drawable} to be displayed as a background on the header.
     */
    public void setHeaderBackgroundDrawable(Drawable drawable) {
        if (drawable != null) {
            setWillNotDraw(false);
            mBackground = drawable;
            mBackground.setCallback(this);
            setOutlineProvider(mProvider);
        } else {
            setWillNotDraw(true);
            mBackground = null;
            setOutlineProvider(null);
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mBackground != null) {
            mBackground.setBounds(0, 0, getWidth(), mHeaderBackgroundHeight);
            mBackground.draw(canvas);
        }
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        return super.verifyDrawable(who) || who == mBackground;
    }

    @SuppressLint("MissingSuperCall")
    @Override
    protected void drawableStateChanged() {
        if (mBackground != null && mBackground.isStateful()) {
            mBackground.setState(getDrawableState());
        }
    }

    private void updateTouchListener() {
        if (mExpandClickListener != null && mIcon != null) {
            mTouchListener.bindTouchRects();
        }
    }

    @Override
    public void setOnClickListener(@Nullable OnClickListener l) {
        mExpandClickListener = l;
        setOnTouchListener(mExpandClickListener != null ? mTouchListener : null);
        mExpandButton.setOnClickListener(mExpandClickListener);
        updateTouchListener();
    }

    @RemotableViewMethod
    public void setOriginalIconColor(int color) {
        mIconColor = color;
    }

    public int getOriginalIconColor() {
        return mIconColor;
    }

    @RemotableViewMethod
    public void setOriginalNotificationColor(int color) {
        mOriginalNotificationColor = color;
    }

    public int getOriginalNotificationColor() {
        return mOriginalNotificationColor;
    }

    @RemotableViewMethod
    public void setExpanded(boolean expanded) {
        mExpanded = expanded;
        updateExpandButton();
    }

    private void updateExpandButton() {
        int drawableId;
        if (mExpanded) {
            drawableId = R.drawable.ic_collapse_notification;
        } else {
            drawableId = R.drawable.ic_expand_notification;
        }
        mExpandButton.setImageDrawable(mModuleRes.getDrawable(drawableId));
        mExpandButton.setColorFilter(mIconColor);
    }

    public void setShowWorkBadgeAtEnd(boolean showWorkBadgeAtEnd) {
        if (showWorkBadgeAtEnd != mShowWorkBadgeAtEnd) {
            setClipToPadding(!showWorkBadgeAtEnd);
            mShowWorkBadgeAtEnd = showWorkBadgeAtEnd;
        }
    }

    public View getWorkProfileIcon() {
        return mProfileBadge;
    }

    public class HeaderTouchListener implements View.OnTouchListener {

        private final ArrayList<Rect> mTouchRects = new ArrayList<>();
        private int mTouchSlop;
        private boolean mTrackGesture;
        private float mDownX;
        private float mDownY;

        public HeaderTouchListener() {
        }

        public void bindTouchRects() {
            mTouchRects.clear();
            addRectAroundViewView(mIcon);
            addRectAroundViewView(mExpandButton);
            addWidthRect();
            mTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();

        }

        private void addWidthRect() {
            Rect r = new Rect();
            r.top = 0;
            r.bottom = (int) (32 * getResources().getDisplayMetrics().density);
            r.left = 0;
            r.right = getWidth();
            mTouchRects.add(r);
        }

        private void addRectAroundViewView(View view) {
            final Rect r = getRectAroundView(view);
            mTouchRects.add(r);
        }

        private Rect getRectAroundView(View view) {
            float size = 48 * getResources().getDisplayMetrics().density;
            final Rect r = new Rect();
            if (view.getVisibility() == GONE) {
                view = getFirstChildNotGone();
                r.left = (int) (view.getLeft() - size / 2.0f);
            } else {
                r.left = (int) ((view.getLeft() + view.getRight()) / 2.0f - size / 2.0f);
            }
            r.top = (int) ((view.getTop() + view.getBottom()) / 2.0f - size / 2.0f);
            r.bottom = (int) (r.top + size);
            r.right = (int) (r.left + size);
            return r;
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            float x = event.getX();
            float y = event.getY();
            switch (event.getActionMasked() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    mTrackGesture = false;
                    if (isInside(x, y)) {
                        mTrackGesture = true;
                        return true;
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (mTrackGesture) {
                        if (Math.abs(mDownX - x) > mTouchSlop
                                || Math.abs(mDownY - y) > mTouchSlop) {
                            mTrackGesture = false;
                        }
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    if (mTrackGesture) {
                        mExpandClickListener.onClick(NotificationHeaderView.this);
                    }
                    break;
            }
            return mTrackGesture;
        }

        private boolean isInside(float x, float y) {
            for (int i = 0; i < mTouchRects.size(); i++) {
                Rect r = mTouchRects.get(i);
                if (r.contains((int) x, (int) y)) {
                    mDownX = x;
                    mDownY = y;
                    return true;
                }
            }
            return false;
        }
    }

    private View getFirstChildNotGone() {
        for (int i = 0; i < getChildCount(); i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                return child;
            }
        }
        return this;
    }

    public ImageView getExpandButton() {
        return mExpandButton;
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    public boolean isInTouchRect(float x, float y) {
        if (mExpandClickListener == null) {
            return false;
        }
        return mTouchListener.isInside(x, y);
    }
}