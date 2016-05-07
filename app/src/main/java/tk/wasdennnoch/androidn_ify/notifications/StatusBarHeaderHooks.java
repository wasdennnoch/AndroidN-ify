package tk.wasdennnoch.androidn_ify.notifications;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.XModuleResources;
import android.content.res.XResources;
import android.os.Process;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LayoutInflated;
import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.extracted.systemui.AlphaOptimizedButton;
import tk.wasdennnoch.androidn_ify.extracted.systemui.ExpandableIndicator;
import tk.wasdennnoch.androidn_ify.extracted.systemui.TouchAnimator;
import tk.wasdennnoch.androidn_ify.utils.ResourceUtils;

public class StatusBarHeaderHooks {

    private static final String TAG = "StatusBarHeaderHooks";

    private static final String PACKAGE_SYSTEMUI = XposedHook.PACKAGE_SYSTEMUI;
    private static final String CLASS_STATUS_BAR_HEADER_VIEW = "com.android.systemui.statusbar.phone.StatusBarHeaderView";
    private static final String CLASS_LAYOUT_VALUES = CLASS_STATUS_BAR_HEADER_VIEW + "$LayoutValues";
    private static final String CLASS_QS_PANEL = "com.android.systemui.qs.QSPanel";
    private static final String CLASS_DETAIL_ADAPTER = "com.android.systemui.qs.QSTile$DetailAdapter";

    private static TouchAnimator mAlarmTranslation;
    private static TouchAnimator mDateSizeAnimator;
    private static TouchAnimator mFirstHalfAnimator;
    private static TouchAnimator mSecondHalfAnimator;
    private static TouchAnimator mSettingsAlpha;
    private static TouchAnimator mQuickQSAnimator;

    private static RelativeLayout mStatusBarHeaderView;

    private static View mSystemIconsSuperContainer;
    private static View mDateGroup;
    private static FrameLayout mMultiUserSwitch;
    private static TextView mDateCollapsed;
    private static View mSettingsButton;
    private static View mSettingsContainer;
    private static View mQsDetailHeader;
    private static TextView mQsDetailHeaderTitle;
    private static Switch mQsDetailHeaderSwitch;
    private static TextView mEmergencyCallsOnly;
    private static TextView mAlarmStatus;

    private static ExpandableIndicator mExpandIndicator;
    private static LinearLayout mDateTimeAlarmGroup;
    private static LinearLayout mDateTimeGroup;
    private static LinearLayout mRightContainer;
    private static Button mAlarmStatusCollapsed;

    private static QuickQSPanel mHeaderQsPanel;

    private static XC_MethodHook onFinishInflateHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {

            XposedHook.logD(TAG, "SystemUI PID: " + Process.myPid());

            mStatusBarHeaderView = (RelativeLayout) param.thisObject;
            Context context = mStatusBarHeaderView.getContext();
            ResourceUtils res = ResourceUtils.getInstance(context);

            try {
                //noinspection deprecation
                mStatusBarHeaderView.setBackgroundColor(context.getResources().getColor(context.getResources().getIdentifier("system_primary_color", "color", PACKAGE_SYSTEMUI)));
            } catch (Throwable t) {
                XposedHook.logE(TAG, "Couldn't change header background color", t);
            }

            View mClock;
            TextView mTime;
            TextView mAmPm;
            try {
                mSystemIconsSuperContainer = (View) XposedHelpers.getObjectField(param.thisObject, "mSystemIconsSuperContainer");
                mDateGroup = (View) XposedHelpers.getObjectField(param.thisObject, "mDateGroup");
                mClock = (View) XposedHelpers.getObjectField(param.thisObject, "mClock");
                mTime = (TextView) XposedHelpers.getObjectField(param.thisObject, "mTime");
                mAmPm = (TextView) XposedHelpers.getObjectField(param.thisObject, "mAmPm");
                mMultiUserSwitch = (FrameLayout) XposedHelpers.getObjectField(param.thisObject, "mMultiUserSwitch");
                mDateCollapsed = (TextView) XposedHelpers.getObjectField(param.thisObject, "mDateCollapsed");
                mSettingsButton = (View) XposedHelpers.getObjectField(param.thisObject, "mSettingsButton");
                mQsDetailHeader = (View) XposedHelpers.getObjectField(param.thisObject, "mQsDetailHeader");
                mQsDetailHeaderTitle = (TextView) XposedHelpers.getObjectField(param.thisObject, "mQsDetailHeaderTitle");
                mQsDetailHeaderSwitch = (Switch) XposedHelpers.getObjectField(param.thisObject, "mQsDetailHeaderSwitch");
                mEmergencyCallsOnly = (TextView) XposedHelpers.getObjectField(param.thisObject, "mEmergencyCallsOnly");
                mAlarmStatus = (TextView) XposedHelpers.getObjectField(param.thisObject, "mAlarmStatus");
            } catch (Throwable t) {
                // try-catch for every single view would be overkill, I'll sort them by fail count after the release.
                // TODO fail-count sorting in multiple try-catches
                // Another problem would be that I can't move a view when I need the ID of another view that I couldn't find.
                // That would efficiently break most of the repositioning.
                XposedHook.logE(TAG, "Couldn't find required views, aborting", t);
                return;
            }
            // Separate try-catch for settings button as some ROMs removed the container around it
            try {
                mSettingsContainer = (View) XposedHelpers.getObjectField(param.thisObject, "mSettingsContainer");
            } catch (Throwable t) {
                mSettingsContainer = mSettingsButton;
            }

            try {

                int rippleRes = context.getResources().getIdentifier("ripple_drawable", "drawable", XposedHook.PACKAGE_SYSTEMUI);
                int iconSize = res.getDimensionPixelSize(R.dimen.right_icon_size);
                int expandIndicatorPadding = res.getDimensionPixelSize(R.dimen.expand_indicator_padding);
                int quickQSHorizontalMargin = res.getDimensionPixelSize(R.dimen.qs_quick_panel_margin_horizontal);

                ((ViewGroup) mClock.getParent()).removeView(mClock);
                ((ViewGroup) mMultiUserSwitch.getParent()).removeView(mMultiUserSwitch);
                ((ViewGroup) mDateCollapsed.getParent()).removeView(mDateCollapsed);
                ((ViewGroup) mSettingsContainer.getParent()).removeView(mSettingsContainer);
                ((ViewGroup) mAlarmStatus.getParent()).removeView(mAlarmStatus);

                RelativeLayout.LayoutParams rightContainerLp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, res.getDimensionPixelSize(R.dimen.right_layout_height));
                rightContainerLp.addRule(RelativeLayout.ALIGN_PARENT_END);
                rightContainerLp.rightMargin = res.getDimensionPixelSize(R.dimen.right_layout_margin_right);
                rightContainerLp.topMargin = res.getDimensionPixelSize(R.dimen.right_layout_margin_top);
                mRightContainer = new LinearLayout(context);
                mRightContainer.setLayoutParams(rightContainerLp);
                mRightContainer.setGravity(Gravity.CENTER);
                mRightContainer.setOrientation(LinearLayout.HORIZONTAL);
                mRightContainer.setClipChildren(false);

                LinearLayout.LayoutParams multiUserSwitchLp = new LinearLayout.LayoutParams(iconSize, iconSize);
                mMultiUserSwitch.setLayoutParams(multiUserSwitchLp);

                LinearLayout.LayoutParams settingsContainerLp = new LinearLayout.LayoutParams(iconSize, iconSize);
                mSettingsContainer.setLayoutParams(settingsContainerLp);

                LinearLayout.LayoutParams expandIndicatorLp = new LinearLayout.LayoutParams(iconSize, iconSize);
                mExpandIndicator = new ExpandableIndicator(context);
                mExpandIndicator.setLayoutParams(expandIndicatorLp);
                mExpandIndicator.setPadding(expandIndicatorPadding, expandIndicatorPadding, expandIndicatorPadding, expandIndicatorPadding);
                mExpandIndicator.setClickable(true);
                mExpandIndicator.setFocusable(true);
                mExpandIndicator.setFocusableInTouchMode(false);
                mExpandIndicator.setCropToPadding(false);
                mExpandIndicator.setBackgroundResource(rippleRes);
                mExpandIndicator.setId(R.id.statusbar_header_expand_indicator);


                RelativeLayout.LayoutParams emergencyCallsOnlyLp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                emergencyCallsOnlyLp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                //emergencyCallsOnlyLp.leftMargin = res.getDimensionPixelSize(R.dimen.header_horizontal_margin);
                emergencyCallsOnlyLp.topMargin = res.getDimensionPixelSize(R.dimen.emergency_calls_only_margin_top);
                mEmergencyCallsOnly.setLayoutParams(emergencyCallsOnlyLp);
                //noinspection deprecation
                mEmergencyCallsOnly.setTextSize(TypedValue.COMPLEX_UNIT_PX, res.getDimensionPixelSize(R.dimen.emergency_calls_only_text_size));
                mEmergencyCallsOnly.setTextColor(res.getColor(R.color.emergency_calls_only_text_color));
                mEmergencyCallsOnly.setVisibility(View.GONE);


                RelativeLayout.LayoutParams dateTimeAlarmGroupLp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                dateTimeAlarmGroupLp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                dateTimeAlarmGroupLp.topMargin = res.getDimensionPixelSize(R.dimen.date_time_alarm_group_margin_top);
                dateTimeAlarmGroupLp.leftMargin = res.getDimensionPixelSize(R.dimen.date_time_alarm_group_margin_left);
                mDateTimeAlarmGroup = new LinearLayout(context);
                mDateTimeAlarmGroup.setLayoutParams(dateTimeAlarmGroupLp);
                mDateTimeAlarmGroup.setId(View.generateViewId());
                mDateTimeAlarmGroup.setGravity(Gravity.START);
                mDateTimeAlarmGroup.setOrientation(LinearLayout.VERTICAL);
                mDateTimeAlarmGroup.setBaselineAligned(false);

                LinearLayout.LayoutParams alarmStatusLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, res.getDimensionPixelSize(R.dimen.alarm_status_height));
                mAlarmStatus.setLayoutParams(alarmStatusLp);
                mAlarmStatus.setGravity(Gravity.TOP);
                //noinspection deprecation
                mAlarmStatus.setTextColor(res.getColor(R.color.alarm_status_text_color));
                mAlarmStatus.setTextSize(TypedValue.COMPLEX_UNIT_PX, res.getDimensionPixelSize(R.dimen.date_time_collapsed_size));
                mAlarmStatus.setPadding(0, res.getDimensionPixelSize(R.dimen.alarm_status_padding_top), 0, 0);
                mAlarmStatus.setCompoundDrawablePadding(res.getDimensionPixelSize(R.dimen.alarm_status_drawable_padding));
                mAlarmStatus.setCompoundDrawablesWithIntrinsicBounds(context.getDrawable(context.getResources().getIdentifier("ic_access_alarms_small", "drawable", XposedHook.PACKAGE_SYSTEMUI)), null, null, null);
                mAlarmStatus.setVisibility(View.GONE);
                mAlarmStatus.setBackgroundResource(rippleRes);


                LinearLayout.LayoutParams dateTimeGroupLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, res.getDimensionPixelSize(R.dimen.date_time_group_height));
                mDateTimeGroup = new LinearLayout(context);
                mDateTimeGroup.setLayoutParams(dateTimeGroupLp);
                mDateTimeGroup.setId(View.generateViewId());
                mDateTimeGroup.setOrientation(LinearLayout.HORIZONTAL);
                mDateTimeGroup.setPivotX(0.0F);
                mDateTimeGroup.setPivotY(0.0F);
                mDateTimeGroup.setBaselineAligned(false);

                LinearLayout.LayoutParams clockLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                mClock.setLayoutParams(clockLp);
                mClock.findViewById(context.getResources().getIdentifier("empty_time_view", "id", XposedHook.PACKAGE_SYSTEMUI)).setVisibility(View.GONE);

                mTime.setTextColor(res.getColor(R.color.clock_date_text_color));
                mTime.setTextSize(TypedValue.COMPLEX_UNIT_PX, res.getDimensionPixelSize(R.dimen.date_time_collapsed_size));
                mAmPm.setTextColor(res.getColor(R.color.clock_date_text_color));
                mAmPm.setTextSize(TypedValue.COMPLEX_UNIT_PX, res.getDimensionPixelSize(R.dimen.date_time_collapsed_size));
                mAmPm.setPadding(0, 0, res.getDimensionPixelSize(R.dimen.date_collapsed_drawable_padding), 0);

                LinearLayout.LayoutParams dateCollapsedLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                mDateCollapsed.setLayoutParams(dateCollapsedLp);
                mDateCollapsed.setGravity(Gravity.TOP);
                mDateCollapsed.setTextColor(res.getColor(R.color.clock_date_text_color));
                mDateCollapsed.setTextSize(TypedValue.COMPLEX_UNIT_PX, res.getDimensionPixelSize(R.dimen.date_time_collapsed_size));
                mDateCollapsed.setCompoundDrawablesWithIntrinsicBounds(res.getDrawable(R.drawable.header_dot), null, null, null);
                mDateCollapsed.setCompoundDrawablePadding(res.getDimensionPixelSize(R.dimen.date_collapsed_drawable_padding));

                LinearLayout.LayoutParams alarmStatusCollapsedLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                mAlarmStatusCollapsed = new AlphaOptimizedButton(context);
                mAlarmStatusCollapsed.setLayoutParams(alarmStatusCollapsedLp);
                mAlarmStatusCollapsed.setId(View.generateViewId());
                mAlarmStatusCollapsed.setGravity(Gravity.TOP);
                //noinspection deprecation
                mAlarmStatusCollapsed.setTextColor(res.getColor(R.color.alarm_status_text_color));
                mAlarmStatusCollapsed.setTextSize(TypedValue.COMPLEX_UNIT_PX, res.getDimensionPixelSize(R.dimen.date_time_collapsed_size));
                mAlarmStatusCollapsed.setClickable(false);
                mAlarmStatusCollapsed.setFocusable(false);
                mAlarmStatusCollapsed.setVisibility(View.GONE);
                //noinspection deprecation
                mAlarmStatusCollapsed.setCompoundDrawablesWithIntrinsicBounds(context.getDrawable(context.getResources().getIdentifier("ic_access_alarms_small", "drawable", XposedHook.PACKAGE_SYSTEMUI)), null, null, null);
                mAlarmStatusCollapsed.setBackgroundResource(0);
                mAlarmStatusCollapsed.setPadding(res.getDimensionPixelSize(R.dimen.alarm_status_collapsed_drawable_padding), 0, 0, 0);


                LinearLayout.LayoutParams headerQsPanelLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                mHeaderQsPanel = new QuickQSPanel(context);
                mHeaderQsPanel.setLayoutParams(headerQsPanelLp);
                mHeaderQsPanel.setPadding(quickQSHorizontalMargin, res.getDimensionPixelSize(R.dimen.qs_quick_panel_padding_top), quickQSHorizontalMargin, res.getDimensionPixelSize(R.dimen.qs_quick_panel_padding_bottom));
                mHeaderQsPanel.setClipChildren(false);
                mHeaderQsPanel.setClipToPadding(false);


                mRightContainer.addView(mMultiUserSwitch);
                mRightContainer.addView(mSettingsContainer);
                mRightContainer.addView(mExpandIndicator);
                mDateTimeGroup.addView(mClock);
                mDateTimeGroup.addView(mDateCollapsed);
                mDateTimeGroup.addView(mAlarmStatusCollapsed);
                mDateTimeAlarmGroup.addView(mDateTimeGroup);
                mDateTimeAlarmGroup.addView(mAlarmStatus);
                mStatusBarHeaderView.addView(mRightContainer);
                mStatusBarHeaderView.addView(mDateTimeAlarmGroup);
                mStatusBarHeaderView.addView(mHeaderQsPanel);
                mStatusBarHeaderView.setClipChildren(false);
                mStatusBarHeaderView.setClipToPadding(false);

            } catch (Throwable t) {
                // :(
                XposedHook.logE(TAG, "Error modifying the layout", t);
                return;
            }

            updateResources(context);

            mSystemIconsSuperContainer.setVisibility(View.GONE);
            mDateGroup.setVisibility(View.GONE);

        }
    };
    private static XC_MethodHook setExpansionHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            float f = (float) param.args[0];
            try {
                if (mAlarmTranslation != null)
                    mAlarmTranslation.setPosition(f);
                if (mDateSizeAnimator != null) {
                    mDateSizeAnimator.setPosition(f);
                    mFirstHalfAnimator.setPosition(f);
                    mSecondHalfAnimator.setPosition(f);
                    mSettingsAlpha.setPosition(f);
                    mQuickQSAnimator.setPosition(f);
                }
                mHeaderQsPanel.setVisibility(f < 0.36F ? View.VISIBLE : View.INVISIBLE);
                mExpandIndicator.setExpanded(f > 0.93F);
            } catch (Throwable t) {
                XposedHook.logE(TAG, "Error setting expansion values", t);
            }
        }
    };
    private static XC_MethodHook onConfigurationChangedHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            updateResources(((View) param.thisObject).getContext());
        }
    };
    private static XC_MethodHook updateEverythingHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            updateDateTimePosition(((View) param.thisObject).getContext());
        }
    };
    private static XC_MethodHook updateVisibilitiesHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            if (mSystemIconsSuperContainer != null) {
                mSystemIconsSuperContainer.setVisibility(View.GONE);
                mDateGroup.setVisibility(View.GONE);
                mDateCollapsed.setVisibility(View.VISIBLE);
                updateAlarmVisibilities();
                mMultiUserSwitch.setVisibility(XposedHelpers.getBooleanField(param.thisObject, "mExpanded") ? View.VISIBLE : View.INVISIBLE);
            } else {
                XposedHook.logD(TAG, "updateVisibilitiesHook: mSystemIconsSuperContainer is still null");
            }
        }
    };

    private static XC_MethodHook setTilesHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            XposedHook.logD(TAG, "setTilesHook PID: " + Process.myPid());
            // This method gets called from two different processes,
            // so we have to check if we are in the right one
            if (mHeaderQsPanel != null) {
                //noinspection unchecked
                final ArrayList<Object> mRecords = (ArrayList<Object>) XposedHelpers.getObjectField(param.thisObject, "mRecords");
                mHeaderQsPanel.setTiles(mRecords);
            }
        }
    };
    private static XC_MethodHook drawTileHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            XposedHook.logD(TAG, "drawTileHook PID: " + Process.myPid());
            // This method gets called from two different processes,
            // so we have to check if we are in the right one
            if (mHeaderQsPanel != null) {
                mHeaderQsPanel.drawTile(param.args[0], param.args[1]); // TODO no icon animation
            }
        }
    };


    private static void updateResources(Context context) {
        if (mDateTimeGroup == null) {
            XposedHook.logD(TAG, "updateResources(): mDateTimeGroup is still null");
            return;
        }

        ResourceUtils res = ResourceUtils.getInstance(context);
        float timeCollapsed = res.getDimensionPixelSize(R.dimen.date_time_collapsed_size);
        float timeExpanded = res.getDimensionPixelSize(R.dimen.date_time_expanded_size);
        float dateScaleFactor = timeExpanded / timeCollapsed;
        float gearTranslation = res.getDimension(R.dimen.settings_gear_translation);

        updateDateTimePosition(context);

        mDateSizeAnimator = new TouchAnimator.Builder()
                .addFloat(mDateTimeGroup, "scaleX", 1, dateScaleFactor)
                .addFloat(mDateTimeGroup, "scaleY", 1, dateScaleFactor)
                .setStartDelay(0.36F)
                .build();
        mFirstHalfAnimator = new TouchAnimator.Builder()
                .addFloat(mAlarmStatusCollapsed, "alpha", 1.0F, 0.0F)
                .setEndDelay(0.5F).build();
        mSecondHalfAnimator = new TouchAnimator.Builder()
                .addFloat(mAlarmStatus, "alpha", 0.0F, 1.0F)
                .addFloat(mEmergencyCallsOnly, "alpha", 0.0F, 1.0F)
                .setStartDelay(0.5F).build();
        mSettingsAlpha = new TouchAnimator.Builder()
                .addFloat(mSettingsContainer, "translationY", -gearTranslation, 0.0F)
                .addFloat(mMultiUserSwitch, "translationY", -gearTranslation, 0.0F)
                .addFloat(mSettingsButton, "rotation", -90F, 0.0F)
                .addFloat(mSettingsContainer, "alpha", 0.0F, 1.0F)
                .addFloat(mMultiUserSwitch, "alpha", 0.0F, 1.0F)
                .setStartDelay(0.7F).build();
        mQuickQSAnimator = new TouchAnimator.Builder()
                .addFloat(mHeaderQsPanel, "alpha", 1.0F, 0.0F)
                .setEndDelay(0.64F).build();

        boolean rtl = (boolean) XposedHelpers.callMethod(mStatusBarHeaderView.getLayoutParams(), "isLayoutRtl");
        if (rtl && mDateTimeGroup.getWidth() == 0) {
            if (mDateTimeGroup.getWidth() == 0) {
                mDateTimeGroup.addOnLayoutChangeListener(new android.view.View.OnLayoutChangeListener() {
                    @Override
                    public void onLayoutChange(View view, int j, int k, int l, int i1, int j1, int k1, int l1, int i2) {
                        mDateTimeGroup.setPivotX(mStatusBarHeaderView.getWidth());
                        mDateTimeGroup.removeOnLayoutChangeListener(this);
                    }
                });
            } else {
                mDateTimeGroup.setPivotX(mDateTimeGroup.getWidth());
            }
        }
    }

    private static void updateDateTimePosition(Context context) {
        ResourceUtils res = ResourceUtils.getInstance(context);
        float mDateTimeTranslation = res.getDimension(R.dimen.date_anim_translation);
        float mDateTimeAlarmTranslation = res.getDimension(R.dimen.date_alarm_anim_translation);
        TouchAnimator.Builder builder = new TouchAnimator.Builder();
        float f;
        if (XposedHelpers.getBooleanField(mStatusBarHeaderView, "mAlarmShowing"))
            f = mDateTimeAlarmTranslation;
        else
            f = mDateTimeTranslation;
        mAlarmTranslation = builder.addFloat(mDateTimeAlarmGroup, "translationY", 0.0F, f).build();
        mAlarmTranslation.setPosition(XposedHelpers.getFloatField(mStatusBarHeaderView, "mCurrentT"));
    }

    private static void updateAlarmVisibilities() {
        int v;
        if (XposedHelpers.getBooleanField(mStatusBarHeaderView, "mAlarmShowing")) {
            v = View.VISIBLE;
        } else {
            v = View.INVISIBLE;
        }
        if (mAlarmStatus != null)
            mAlarmStatus.setVisibility(v);
        if (mAlarmStatusCollapsed != null)
            mAlarmStatusCollapsed.setVisibility(v);
    }

    private static void handleShowingDetail(final Object detail) {
        final boolean showingDetail = detail != null;
        transition(mDateTimeAlarmGroup, !showingDetail);
        transition(mRightContainer, !showingDetail);
        transition(mExpandIndicator, !showingDetail);
        transition(mQsDetailHeader, showingDetail);
        XposedHelpers.setBooleanField(mStatusBarHeaderView, "mShowingDetail", showingDetail);
        if (showingDetail) {
            mQsDetailHeaderTitle.setText((int) XposedHelpers.callMethod(detail, "getTitle"));
            final Boolean toggleState = (Boolean) XposedHelpers.callMethod(detail, "getToggleState");
            if (toggleState == null) {
                mQsDetailHeaderSwitch.setVisibility(View.INVISIBLE);
                mQsDetailHeader.setClickable(false);
            } else {
                mQsDetailHeaderSwitch.setVisibility(View.VISIBLE);
                mQsDetailHeaderSwitch.setChecked(toggleState);
                mQsDetailHeader.setClickable(true);
                mQsDetailHeader.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        boolean checked = !mQsDetailHeaderSwitch.isChecked();
                        mQsDetailHeaderSwitch.setChecked(checked);
                        XposedHelpers.callMethod(detail, "setToggleState", checked);
                    }
                });
            }
        } else {
            mQsDetailHeader.setClickable(false);
        }
    }

    private static void transition(final View v, final boolean in) {
        if (in) {
            v.bringToFront();
            v.setVisibility(View.VISIBLE);
        }
        if (v.hasOverlappingRendering()) {
            v.animate().withLayer();
        }
        v.animate()
                .alpha(in ? 1 : 0)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        if (!in) {
                            v.setVisibility(View.INVISIBLE);
                        }
                        try {
                            XposedHelpers.setBooleanField(mStatusBarHeaderView, "mDetailTransitioning", false);
                        } catch (Throwable ignore) {
                            // Not in LP
                        }
                    }
                })
                .start();
    }

    public static void hook(ClassLoader classLoader, XSharedPreferences prefs) {
        try {
            if (prefs.getBoolean("enable_notification_tweaks", true)) {

                Class<?> classStatusBarHeaderView = XposedHelpers.findClass(CLASS_STATUS_BAR_HEADER_VIEW, classLoader);
                Class<?> classQSPanel = XposedHelpers.findClass(CLASS_QS_PANEL, classLoader);

                XposedHelpers.findAndHookMethod(classStatusBarHeaderView, "onFinishInflate", onFinishInflateHook);
                XposedHelpers.findAndHookMethod(classStatusBarHeaderView, "setExpansion", float.class, setExpansionHook);
                XposedHelpers.findAndHookMethod(classStatusBarHeaderView, "onConfigurationChanged", Configuration.class, onConfigurationChangedHook);
                XposedHelpers.findAndHookMethod(classStatusBarHeaderView, "updateEverything", updateEverythingHook);
                XposedHelpers.findAndHookMethod(classStatusBarHeaderView, "updateVisibilities", updateVisibilitiesHook);

                Class<?> classLayoutValues = XposedHelpers.findClass(CLASS_LAYOUT_VALUES, classLoader);
                // Yes, this is a typo. Not my typo though. A typo in the source code that nobody noticed and that got compiled. ("interpoloate")
                XposedHelpers.findAndHookMethod(classLayoutValues, "interpoloate", classLayoutValues, classLayoutValues, float.class, XC_MethodReplacement.DO_NOTHING);
                XposedHelpers.findAndHookMethod(classStatusBarHeaderView, "requestCaptureValues", XC_MethodReplacement.DO_NOTHING);
                XposedHelpers.findAndHookMethod(classStatusBarHeaderView, "applyLayoutValues", classLayoutValues, XC_MethodReplacement.DO_NOTHING);
                XposedHelpers.findAndHookMethod(classStatusBarHeaderView, "captureLayoutValues", classLayoutValues, XC_MethodReplacement.DO_NOTHING);
                XposedHelpers.findAndHookMethod(classStatusBarHeaderView, "updateLayoutValues", float.class, XC_MethodReplacement.DO_NOTHING);
                XposedHelpers.findAndHookMethod(classStatusBarHeaderView, "updateClockCollapsedMargin", XC_MethodReplacement.DO_NOTHING);
                XposedHelpers.findAndHookMethod(classStatusBarHeaderView, "updateHeights", XC_MethodReplacement.DO_NOTHING);
                XposedHelpers.findAndHookMethod(classStatusBarHeaderView, "updateSignalClusterDetachment", XC_MethodReplacement.DO_NOTHING);
                XposedHelpers.findAndHookMethod(classStatusBarHeaderView, "updateSystemIconsLayoutParams", XC_MethodReplacement.DO_NOTHING);
                XposedHelpers.findAndHookMethod(classStatusBarHeaderView, "updateAvatarScale", XC_MethodReplacement.DO_NOTHING);
                XposedHelpers.findAndHookMethod(classStatusBarHeaderView, "updateClockScale", XC_MethodReplacement.DO_NOTHING);
                //XposedHelpers.findAndHookMethod(classStatusBarHeaderView, "updateAmPmTranslation", XC_MethodReplacement.DO_NOTHING);
                XposedHelpers.findAndHookMethod(classStatusBarHeaderView, "updateClockLp", XC_MethodReplacement.DO_NOTHING);
                XposedHelpers.findAndHookMethod(classStatusBarHeaderView, "updateMultiUserSwitch", XC_MethodReplacement.DO_NOTHING);

                XposedHelpers.findAndHookMethod(classStatusBarHeaderView, "onLayout", boolean.class, int.class, int.class, int.class, int.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        mAlarmStatus.setX(0);
                    }
                });

                // TODO find better way of managing header view transitions on showing detail
                XposedHelpers.findAndHookMethod(classQSPanel, "fireShowingDetail", CLASS_DETAIL_ADAPTER, new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                        handleShowingDetail(param.args[0]);
                        return null;
                    }
                });


                //XposedHelpers.findAndHookMethod(classQSPanel, "setTiles", Collection.class, setTilesHook);
                XposedBridge.hookAllMethods(classQSPanel, "setTiles", setTilesHook);
                XposedBridge.hookAllMethods(classQSPanel, "drawTile", drawTileHook);

            }
        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error in hook", t);
        }
    }

    public static void hookResSystemui(XC_InitPackageResources.InitPackageResourcesParam resparam, XSharedPreferences prefs, String modulePath) {
        try {
            if (prefs.getBoolean("enable_notification_tweaks", true)) {

                XModuleResources modRes = XModuleResources.createInstance(modulePath, resparam.res);

                XResources.DimensionReplacement zero = new XResources.DimensionReplacement(0, TypedValue.COMPLEX_UNIT_DIP);

                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "qs_peek_height", zero);
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "status_bar_header_height", modRes.fwd(R.dimen.status_bar_header_height));
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "status_bar_header_height_expanded", modRes.fwd(R.dimen.status_bar_header_height));
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "qs_emergency_calls_only_text_size", modRes.fwd(R.dimen.emergency_calls_only_text_size));
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "qs_date_collapsed_size", modRes.fwd(R.dimen.date_time_collapsed_size));
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "multi_user_avatar_collapsed_size", modRes.fwd(R.dimen.multi_user_avatar_size));
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "qs_brightness_padding_top", modRes.fwd(R.dimen.brightness_slider_padding_top));
                try {
                    resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "multi_user_avatar_expanded_size", modRes.fwd(R.dimen.multi_user_avatar_size));
                } catch (Throwable ignore) {
                    // Not in LP
                }

                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "color", "qs_tile_divider", 0x00FFFFFF);

                resparam.res.hookLayout(PACKAGE_SYSTEMUI, "layout", "status_bar_expanded_header", new XC_LayoutInflated() {
                    @Override
                    public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                        liparam.view.setElevation(0);
                        liparam.view.setPadding(0, 0, 0, 0);
                        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) liparam.view.getLayoutParams();
                        params.height = ResourceUtils.getInstance(liparam.view.getContext()).getDimensionPixelSize(R.dimen.status_bar_header_height);
                    }
                });
                resparam.res.hookLayout(PACKAGE_SYSTEMUI, "layout", "qs_panel", new XC_LayoutInflated() {
                    @Override
                    public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                        liparam.view.setElevation(0);
                        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) liparam.view.getLayoutParams();
                        params.setMarginStart(0);
                        params.setMarginEnd(0);
                    }
                });

            }
        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error hooking SystemUI resources", t);
        }
    }

}
