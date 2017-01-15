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

package tk.wasdennnoch.androidn_ify.extracted.systemui.qs;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.Animatable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.extracted.systemui.NonInterceptingScrollView;
import tk.wasdennnoch.androidn_ify.extracted.systemui.ResizingSpace;
import tk.wasdennnoch.androidn_ify.systemui.notifications.NotificationPanelHooks;
import tk.wasdennnoch.androidn_ify.utils.ResourceUtils;

import static tk.wasdennnoch.androidn_ify.XposedHook.PACKAGE_SYSTEMUI;
import static tk.wasdennnoch.androidn_ify.systemui.notifications.StatusBarHeaderHooks.transition;

@SuppressLint("ViewConstructor")
public class QSDetail extends LinearLayout {

    private static final String TAG = "QSDetail";
    private static final long FADE_DURATION = 300;

    private final Context mContext;

    private ViewGroup mDetailContent;
    private TextView mDetailSettingsButton;
    private TextView mDetailDoneButton;
    private QSDetailClipper mClipper;
    private Object mDetailAdapter;
    private ViewGroup mQsPanel;

    private View mQsDetailHeader;
    private TextView mQsDetailHeaderTitle;
    private Switch mQsDetailHeaderSwitch;
    private ImageView mQsDetailHeaderProgress;

    private boolean mScanState;
    private boolean mClosingDetail;
    private boolean mFullyExpanded;
    private ViewGroup mHeader;
    private boolean mTriggeredExpand;
    private int mOpenX;
    private int mOpenY;
    private boolean mAnimatingOpen;
    private boolean mSwitchState;

    public QSDetail(Context context, ViewGroup panel, ViewGroup header) {
        super(context);
        mContext = context;
        mQsPanel = panel;
        mHeader = header;

        Resources resources = mContext.getResources();
        //noinspection deprecation
        setBackground(resources.getDrawable(resources.getIdentifier("qs_detail_background", "drawable", PACKAGE_SYSTEMUI)));

        addView(new ResizingSpace(mContext, ViewGroup.LayoutParams.MATCH_PARENT, R.dimen.qs_detail_margin_top));

        View detailHeader = LayoutInflater.from(mContext).inflate(resources.getIdentifier("qs_detail_header", "layout", PACKAGE_SYSTEMUI), null);
        detailHeader.setId(R.id.qs_detail_header);
        addView(detailHeader);

        getLayoutInflater(context).inflate(R.layout.qs_detail, this);

        View detailDoneButton = (View) XposedHelpers.getObjectField(panel, "mDetailDoneButton");
        View detailButtons = (View) detailDoneButton.getParent();
        ((ViewGroup) detailButtons.getParent()).removeView(detailButtons);
        addView(detailButtons);

        setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setPadding(0, 0, 0, ResourceUtils.getInstance(mContext).getDimensionPixelSize(R.dimen.qs_detail_padding_bottom));
        setClickable(true);
        setVisibility(INVISIBLE);
        setOrientation(VERTICAL);

        mDetailContent = (ViewGroup) findViewById(android.R.id.content);
        mDetailSettingsButton = (TextView) findViewById(android.R.id.button2);
        mDetailDoneButton = (TextView) findViewById(android.R.id.button1);

        mQsDetailHeader = findViewById(R.id.qs_detail_header);
        mQsDetailHeaderTitle = (TextView) mQsDetailHeader.findViewById(android.R.id.title);
        mQsDetailHeaderSwitch = (Switch) mQsDetailHeader.findViewById(android.R.id.toggle);
        mQsDetailHeaderProgress = (ImageView) findViewById(R.id.qs_detail_header_progress);

        mQsDetailHeader.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        mQsDetailHeader.setVisibility(VISIBLE);
        mQsDetailHeaderProgress.setImageDrawable(mContext.getDrawable(mContext.getResources().getIdentifier("indeterminate_anim", "drawable", PACKAGE_SYSTEMUI)));
        mQsDetailHeaderProgress.setBackground(mContext.getDrawable(mContext.getResources().getIdentifier("qs_detail_progress_track", "color", PACKAGE_SYSTEMUI)));

        updateDetailText();

        mClipper = new QSDetailClipper(this);

        final OnClickListener doneListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                XposedHelpers.callMethod(mQsPanel, "closeDetail");
            }
        };
        mDetailDoneButton.setOnClickListener(doneListener);
    }

    private LayoutInflater getLayoutInflater(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context).cloneInContext(ResourceUtils.createOwnContext(context));
        inflater.setFactory2(new LayoutInflater.Factory2() {
            @Override
            public View onCreateView(String name, Context context, AttributeSet attrs) {
                if (name.equals(NonInterceptingScrollView.class.getCanonicalName())) {
                    return new NonInterceptingScrollView(context, attrs);
                } else return null;
            }

            @Override
            public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
                return onCreateView(name, context, attrs);
            }
        });
        return inflater;
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        //FontSizeUtils.updateFontSize(mDetailDoneButton, R.dimen.qs_detail_button_text_size);
        //FontSizeUtils.updateFontSize(mDetailSettingsButton, R.dimen.qs_detail_button_text_size);
    }

    public boolean isShowingDetail() {
        return mDetailAdapter != null;
    }

    public void setFullyExpanded(boolean fullyExpanded) {
        mFullyExpanded = fullyExpanded;
    }

    public void setExpanded(boolean qsExpanded) {
        if (!qsExpanded) {
            mTriggeredExpand = false;
        }
    }

    private void updateDetailText() {
        //mDetailDoneButton.setText(R.string.quick_settings_done);
        //mDetailSettingsButton.setText(R.string.quick_settings_more_settings);
        int buttonPadding = ResourceUtils.getInstance(mContext).getDimensionPixelSize(R.dimen.qs_detail_button_padding);
        mDetailDoneButton.setBackground(ResourceUtils.getInstance(mContext).getDrawable(R.drawable.qs_btn_borderless_rect));
        mDetailSettingsButton.setBackground(ResourceUtils.getInstance(mContext).getDrawable(R.drawable.qs_btn_borderless_rect));
        mDetailDoneButton.setPadding(buttonPadding,buttonPadding,buttonPadding,buttonPadding);
        mDetailSettingsButton.setPadding(buttonPadding,buttonPadding,buttonPadding,buttonPadding);
    }

    public void updateResources() {
        updateDetailText();
    }

    public boolean isClosingDetail() {
        return mClosingDetail;
    }

    public void handleShowingDetail(Object r, boolean showingDetail, int x, int y) {
        setClickable(showingDetail);
        Object adapter = null;
        View detailView = null;
        if (showingDetail) {
            adapter = XposedHelpers.getObjectField(r, "detailAdapter");

            // Have to move this bafore getToggleState because of CM Profiles tile crash (#1207, getToggleState makes changes to the detail view)
            detailView = (View) XposedHelpers.callMethod(adapter, "createDetailView", mContext,
                    XposedHelpers.getObjectField(r, "detailView"), mDetailContent);
            XposedHelpers.setObjectField(r, "detailView", detailView);

            mQsDetailHeaderTitle.setText((int) XposedHelpers.callMethod(adapter, "getTitle"));
            final Boolean toggleState = (Boolean) XposedHelpers.callMethod(adapter, "getToggleState");
            if (toggleState == null) {
                mQsDetailHeaderSwitch.setVisibility(INVISIBLE);
                mQsDetailHeader.setClickable(false);
            } else {
                mQsDetailHeaderSwitch.setVisibility(VISIBLE);
                handleToggleStateChanged(toggleState, true);
                mQsDetailHeader.setClickable(true);
                final Object finalAdapter = adapter;
                mQsDetailHeader.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        boolean checked = !mQsDetailHeaderSwitch.isChecked();
                        mQsDetailHeaderSwitch.setChecked(checked);
                        XposedHelpers.callMethod(finalAdapter, "setToggleState", checked);
                    }
                });
            }
            if (!mFullyExpanded) {
                mTriggeredExpand = true;
                NotificationPanelHooks.expandIfNecessary();
            } else {
                mTriggeredExpand = false;
            }
            mOpenX = x;
            mOpenY = y;
        } else {
            // Ensure we collapse into the same point we opened from.
            x = mOpenX;
            y = mOpenY;
            if (mTriggeredExpand) {
                NotificationPanelHooks.collapseIfNecessary();
                mTriggeredExpand = false;
            }
        }

        //noinspection DoubleNegation
        boolean visibleDiff = (mDetailAdapter != null) != showingDetail;
        if (!visibleDiff && mDetailAdapter == adapter) return;  // already in right state
        AnimatorListener listener;
        if (adapter != null) {
            if (detailView == null) throw new IllegalStateException("Must return detail view");

            final Intent settingsIntent = (Intent) XposedHelpers.callMethod(adapter, "getSettingsIntent");
            mDetailSettingsButton.setVisibility(settingsIntent != null ? VISIBLE : GONE);
            mDetailSettingsButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    XposedHelpers.callMethod(XposedHelpers.getObjectField(mQsPanel, "mHost"), "startActivityDismissingKeyguard", settingsIntent);
                }
            });

            mDetailContent.removeAllViews();
            mDetailContent.addView(detailView);
            mDetailAdapter = adapter;
            XposedHelpers.callMethod(mQsPanel, "setDetailRecord", r);
            listener = mHideGridContentWhenDone;
            setVisibility(View.VISIBLE);
            transition(mHeader, false);
        } else {
            mClosingDetail = true;
            mDetailAdapter = null;
            listener = mTeardownDetailWhenDone;
            //mHeader.setVisibility(View.VISIBLE);
            transition(mHeader, true);
            XposedHelpers.callMethod(mQsPanel, "setGridContentVisibility", true);
            XposedHelpers.callMethod(mQsPanel, "fireScanStateChanged", false);
        }
        sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
        if (visibleDiff) {
            mAnimatingOpen = adapter != null;
            if (mFullyExpanded || mDetailAdapter != null) {
                setAlpha(1);
                mClipper.animateCircularClip(x, y, mDetailAdapter != null, listener);
            } else {
                animate().alpha(0)
                        .setDuration(FADE_DURATION)
                        .setListener(listener)
                        .start();
            }
        }
    }

    private void handleToggleStateChanged(boolean state, boolean toggleEnabled) {
        mSwitchState = state;
        if (mAnimatingOpen) {
            return;
        }
        mQsDetailHeaderSwitch.setChecked(state);
        mQsDetailHeader.setEnabled(toggleEnabled);
        mQsDetailHeaderSwitch.setEnabled(toggleEnabled);
    }

    public void onScanStateChanged(final boolean state) {
        post(new Runnable() {
            @Override
            public void run() {
                handleScanStateChanged(state);
            }
        });
    }

    private void handleScanStateChanged(boolean state) {
        if (mScanState == state) return;
        mScanState = state;
        final Animatable anim = (Animatable) mQsDetailHeaderProgress.getDrawable();
        if (state) {
            mQsDetailHeaderProgress.animate().alpha(1f);
            anim.start();
        } else {
            mQsDetailHeaderProgress.animate().alpha(0f);
            anim.stop();
        }
    }

    private void checkPendingAnimations() {
        handleToggleStateChanged(mSwitchState,
                mDetailAdapter != null);
    }

    private final AnimatorListenerAdapter mHideGridContentWhenDone = new AnimatorListenerAdapter() {
        public void onAnimationCancel(Animator animation) {
            // If we have been cancelled, remove the listener so that onAnimationEnd doesn't get
            // called, this will avoid accidentally turning off the grid when we don't want to.
            animation.removeListener(this);
            mAnimatingOpen = false;
            checkPendingAnimations();
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            // Only hide content if still in detail state.
            if (mDetailAdapter != null) {
                XposedHelpers.callMethod(mQsPanel, "setGridContentVisibility", false);
                //mHeader.setVisibility(View.INVISIBLE);
            }
            mAnimatingOpen = false;
            checkPendingAnimations();
        }
    };

    private final AnimatorListenerAdapter mTeardownDetailWhenDone = new AnimatorListenerAdapter() {
        public void onAnimationEnd(Animator animation) {
            mDetailContent.removeAllViews();
            XposedHelpers.callMethod(mQsPanel, "setDetailRecord", (Object) null);
            setVisibility(View.INVISIBLE);
            mClosingDetail = false;
        }
    };
}