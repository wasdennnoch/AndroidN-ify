package tk.wasdennnoch.androidn_ify.statusbar.header;

import android.content.Context;
import android.content.res.Configuration;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.extracted.ExpandableIndicator;
import tk.wasdennnoch.androidn_ify.extracted.TouchAnimator2;
import tk.wasdennnoch.androidn_ify.utils.ResourceUtils;

public class StatusBarHeaderHooks {

    private static final String TAG = "StatusBarHeaderHooks";

    private static final String CLASS_STATUS_BAR_HEADER_VIEW = "com.android.systemui.statusbar.phone.StatusBarHeaderView";
    private static final String CLASS_LAYOUT_VALUES = "com.android.systemui.statusbar.phone.StatusBarHeaderView$LayoutValues";

    private static Class<?> classAlphaOptimizedButton;

    private static TouchAnimator2 mAlarmTranslation;
    private static TouchAnimator2 mDateSizeAnimator;
    private static TouchAnimator2 mFirstHalfAnimator;
    private static TouchAnimator2 mSecondHalfAnimator;
    private static TouchAnimator2 mSettingsAlpha;

    private static RelativeLayout mStatusBarHeaderView;

    private static View mSystemIconsSuperContainer;
    private static View mDateGroup;
    private static View mClock;
    private static FrameLayout mMultiUserSwitch;
    //private static ImageView mMultiUserAvatar;
    private static TextView mDateCollapsed;
    //private static TextView mDateExpanded;
    private static View mSettingsButton;
    private static View mSettingsContainer;
    private static TextView mEmergencyCallsOnly;
    private static TextView mAlarmStatus;

    private static ExpandableIndicator mExpandIndicator;
    private static LinearLayout mDateTimeAlarmGroup;
    private static LinearLayout mDateTimeGroup;
    private static Button mAlarmStatusCollapsed;

    private static XC_MethodHook onFinishInflateHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            XposedHook.logI(TAG, "onFinishInflateHook");

            mStatusBarHeaderView = (RelativeLayout) param.thisObject;
            Context context = mStatusBarHeaderView.getContext();
            ResourceUtils res = ResourceUtils.getInstance(context);

            try {
                mSystemIconsSuperContainer = (View) XposedHelpers.getObjectField(param.thisObject, "mSystemIconsSuperContainer");
                mDateGroup = (View) XposedHelpers.getObjectField(param.thisObject, "mDateGroup");
                mClock = (View) XposedHelpers.getObjectField(param.thisObject, "mClock");
                mMultiUserSwitch = (FrameLayout) XposedHelpers.getObjectField(param.thisObject, "mMultiUserSwitch");
                //mMultiUserAvatar = (ImageView) XposedHelpers.getObjectField(param.thisObject, "mMultiUserAvatar");
                mDateCollapsed = (TextView) XposedHelpers.getObjectField(param.thisObject, "mDateCollapsed");
                //mDateExpanded = (TextView) XposedHelpers.getObjectField(param.thisObject, "mDateExpanded");
                mSettingsButton = (View) XposedHelpers.getObjectField(param.thisObject, "mSettingsButton");
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

                ((ViewGroup) mClock.getParent()).removeView(mClock);
                ((ViewGroup) mMultiUserSwitch.getParent()).removeView(mMultiUserSwitch);
                ((ViewGroup) mDateCollapsed.getParent()).removeView(mDateCollapsed);
                ((ViewGroup) mSettingsContainer.getParent()).removeView(mSettingsContainer);
                ((ViewGroup) mAlarmStatus.getParent()).removeView(mAlarmStatus);

                LinearLayout rightLayout = new LinearLayout(context);
                RelativeLayout.LayoutParams rightLayoutLp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, res.getDimensionPixelSize(R.dimen.right_layout_height));
                rightLayoutLp.topMargin = res.getDimensionPixelSize(R.dimen.right_layout_margin_top);
                rightLayout.setLayoutParams(rightLayoutLp);
                rightLayout.setGravity(Gravity.CENTER);
                rightLayout.setOrientation(LinearLayout.HORIZONTAL);
                rightLayout.setClipChildren(false);
                rightLayout.setClipToPadding(false);

                int iconSize = res.getDimensionPixelSize(R.dimen.right_icon_size);
                LinearLayout.LayoutParams multiUserSwitchLp = new LinearLayout.LayoutParams(iconSize, iconSize);
                mMultiUserSwitch.setLayoutParams(multiUserSwitchLp);

                LinearLayout.LayoutParams settingsContainerLp = new LinearLayout.LayoutParams(iconSize, iconSize);
                mSettingsContainer.setLayoutParams(settingsContainerLp);

                int expandIndicatorPadding = res.getDimensionPixelSize(R.dimen.expand_indicator_padding);
                mExpandIndicator = new ExpandableIndicator(context);
                LinearLayout.LayoutParams expandIndicatorLp = new LinearLayout.LayoutParams(iconSize, iconSize);
                mExpandIndicator.setLayoutParams(expandIndicatorLp);
                mExpandIndicator.setPadding(expandIndicatorPadding, expandIndicatorPadding, expandIndicatorPadding, expandIndicatorPadding);
                mExpandIndicator.setClickable(true);
                mExpandIndicator.setFocusable(true);
                mExpandIndicator.setFocusableInTouchMode(true);
                mExpandIndicator.setCropToPadding(false);


                RelativeLayout.LayoutParams emergencyCallsOnlyLp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                emergencyCallsOnlyLp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                mEmergencyCallsOnly.setLayoutParams(emergencyCallsOnlyLp);
                //noinspection deprecation
                mEmergencyCallsOnly.setTextSize(res.getDimensionPixelSize(R.dimen.qs_emergency_calls_only_text_size));
                mEmergencyCallsOnly.setTextColor(res.getColor(R.color.emergency_calls_only_text_color));
                mEmergencyCallsOnly.setGravity(Gravity.CENTER_VERTICAL);
                mEmergencyCallsOnly.setPadding(0, res.getDimensionPixelSize(R.dimen.emergency_calls_only_padding_top), 0, 0);
                mEmergencyCallsOnly.setVisibility(View.GONE);


                mDateTimeAlarmGroup = new LinearLayout(context);
                RelativeLayout.LayoutParams dateTimeAlarmGroupLp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                dateTimeAlarmGroupLp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                dateTimeAlarmGroupLp.topMargin = res.getDimensionPixelSize(R.dimen.date_time_alarm_group_margin_top);
                mDateTimeAlarmGroup.setLayoutParams(dateTimeAlarmGroupLp);
                mDateTimeAlarmGroup.setId(View.generateViewId());
                mDateTimeAlarmGroup.setGravity(Gravity.START);
                mDateTimeAlarmGroup.setOrientation(LinearLayout.VERTICAL);

                mDateTimeGroup = new LinearLayout(context);
                LinearLayout.LayoutParams dateTimeGroupLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, res.getDimensionPixelSize(R.dimen.date_time_group_height));
                mDateTimeGroup.setLayoutParams(dateTimeGroupLp);
                mDateTimeGroup.setId(View.generateViewId());
                mDateTimeGroup.setOrientation(LinearLayout.HORIZONTAL);
                mDateTimeGroup.setPivotX(0.0F);
                mDateTimeGroup.setPivotY(0.0F);

                LinearLayout.LayoutParams clockLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
                mClock.setLayoutParams(clockLp);

                LinearLayout.LayoutParams dateCollapsedLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                mDateCollapsed.setLayoutParams(dateCollapsedLp);
                mDateCollapsed.setGravity(Gravity.TOP);
                mDateCollapsed.setTextColor(res.getColor(R.color.clock_date_text_color));
                mDateCollapsed.setTextSize(R.dimen.qs_time_collapsed_size);
                mDateCollapsed.setSingleLine();
                mDateCollapsed.setCompoundDrawablePadding(res.getDimensionPixelSize(R.dimen.date_collapsed_drawable_padding));

                LinearLayout.LayoutParams alarmStatusLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, res.getDimensionPixelSize(R.dimen.alarm_status_height));
                mAlarmStatus.setLayoutParams(alarmStatusLp);
                mAlarmStatus.setGravity(Gravity.TOP);
                //noinspection deprecation
                mAlarmStatus.setTextColor(res.getColor(R.color.alarm_status_text_color));
                mAlarmStatus.setTextSize(TypedValue.COMPLEX_UNIT_PX, res.getDimensionPixelSize(R.dimen.qs_date_collapsed_size));
                mAlarmStatus.setPadding(0, res.getDimensionPixelSize(R.dimen.alarm_status_padding_top), 0, 0);
                mAlarmStatus.setCompoundDrawablePadding(res.getDimensionPixelSize(R.dimen.alarm_status_drawable_padding));
                mAlarmStatus.setVisibility(View.GONE);
                TypedValue outValue = new TypedValue();
                context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
                mAlarmStatus.setBackgroundResource(outValue.resourceId);


                mAlarmStatusCollapsed = (Button) XposedHelpers.newInstance(classAlphaOptimizedButton, context);
                LinearLayout.LayoutParams alarmStatusCollapsedLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                mAlarmStatusCollapsed.setLayoutParams(alarmStatusCollapsedLp);
                mAlarmStatusCollapsed.setId(View.generateViewId());
                mAlarmStatusCollapsed.setGravity(Gravity.TOP);
                //noinspection deprecation
                mAlarmStatusCollapsed.setTextColor(res.getColor(R.color.alarm_status_text_color));
                mAlarmStatus.setTextSize(TypedValue.COMPLEX_UNIT_PX, res.getDimensionPixelSize(R.dimen.qs_date_collapsed_size));
                mAlarmStatusCollapsed.setCompoundDrawablePadding(res.getDimensionPixelSize(R.dimen.alarm_status_collapsed_drawable_padding));
                mAlarmStatusCollapsed.setVisibility(View.GONE);
                //TypedValue outValue2 = new TypedValue(); // TODO collapsed icon shouldn't need a ripple background although that's how it's officially implemented
                //mContext.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue2, true);
                //mAlarmStatusCollapsed.setBackgroundResource(outValue2.resourceId);

                rightLayout.addView(mMultiUserSwitch);
                rightLayout.addView(mSettingsContainer);
                rightLayout.addView(mExpandIndicator);
                mDateTimeGroup.addView(mClock);
                mDateTimeGroup.addView(mDateCollapsed);
                mDateTimeGroup.addView(mAlarmStatusCollapsed);
                mDateTimeAlarmGroup.addView(mDateTimeGroup);
                mDateTimeAlarmGroup.addView(mAlarmStatus);
                mStatusBarHeaderView.addView(rightLayout);
                mStatusBarHeaderView.addView(mDateTimeAlarmGroup);

                mStatusBarHeaderView.forceLayout(); // TODO testing
                mStatusBarHeaderView.requestLayout();

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
            if (mAlarmTranslation != null)
                mAlarmTranslation.setPosition(f);
            if (mDateSizeAnimator != null) {
                mDateSizeAnimator.setPosition(f);
                mFirstHalfAnimator.setPosition(f);
                mSecondHalfAnimator.setPosition(f);
                mSettingsAlpha.setPosition(f);
                //updateAlarmVisibilities(); //TODO is this necessary?
            }
            mExpandIndicator.setExpanded(f > 0.93F);
        }
    };
    private static XC_MethodHook onConfigurationChangedHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            XposedHook.logI(TAG, "onConfigurationChangedHook");
            updateResources(((View) param.thisObject).getContext());
        }
    };
    private static XC_MethodHook updateEverythingHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            XposedHook.logI(TAG, "updateEverythingHook");
            updateDateTimePosition(((View) param.thisObject).getContext());
        }
    };
    private static XC_MethodHook updateVisibilitiesHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            XposedHook.logI(TAG, "updateVisibilitiesHook");
            if (mSystemIconsSuperContainer != null) {
                mSystemIconsSuperContainer.setVisibility(View.GONE);
                mDateGroup.setVisibility(View.GONE);
                mDateCollapsed.setVisibility(View.VISIBLE);
                updateAlarmVisibilities();
            } else {
                XposedHook.logI(TAG, "updateVisibilitiesHook: Still null");
            }
        }
    };

    private static void updateResources(Context context) {
        XposedHook.logI(TAG, "updateResources()");
        if (mDateTimeGroup == null) {
            XposedHook.logI(TAG, "updateResources(): Still null");
            return;
        }

        ResourceUtils res = ResourceUtils.getInstance(context);
        float timeCollapsed = res.getDimensionPixelSize(R.dimen.qs_time_collapsed_size);
        float timeExpanded = res.getDimensionPixelSize(R.dimen.qs_time_expanded_size);
        float dateScaleFactor = timeCollapsed / timeExpanded;
        float gearTranslation = res.getDimension(R.dimen.qs_header_gear_translation);

        updateDateTimePosition(context);

        mDateSizeAnimator = new TouchAnimator2.Builder()
                .addFloat(mDateTimeGroup, "scaleX", 1, dateScaleFactor)
                .addFloat(mDateTimeGroup, "scaleY", 1, dateScaleFactor)
                .setStartDelay(0.36F)
                .build();
        mFirstHalfAnimator = new TouchAnimator2.Builder()
                .addFloat(mAlarmStatusCollapsed, "alpha", 1.0F, 0.0F)
                .setEndDelay(0.5F).build();
        mSecondHalfAnimator = new TouchAnimator2.Builder()
                .addFloat(mAlarmStatus, "alpha", 0.0F, 1.0F)
                .addFloat(mEmergencyCallsOnly, "alpha", 0.0F, 1.0F)
                .setStartDelay(0.5F).build();
        mSettingsAlpha = new TouchAnimator2.Builder()
                .addFloat(mSettingsContainer, "translationY", -gearTranslation, 0.0F)
                .addFloat(mMultiUserSwitch, "translationY", -gearTranslation, 0.0F)
                .addFloat(mSettingsButton, "rotation", -90F, 0.0F)
                .addFloat(mSettingsContainer, "alpha", 0.0F, 1.0F)
                .addFloat(mMultiUserSwitch, "alpha", 0.0F, 1.0F)
                .setStartDelay(0.7F).build();

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
                return;
            }
            mDateTimeGroup.setPivotX(mDateTimeGroup.getWidth());
        }
    }

    private static void updateDateTimePosition(Context context) {
        XposedHook.logI(TAG, "updateDateTimePosition()");
        ResourceUtils res = ResourceUtils.getInstance(context);
        float mDateTimeTranslation = res.getDimension(R.dimen.qs_date_anim_translation);
        float mDateTimeAlarmTranslation = res.getDimension(R.dimen.qs_date_alarm_anim_translation);
        TouchAnimator2.Builder builder = new TouchAnimator2.Builder();
        float f;
        if (XposedHelpers.getBooleanField(mStatusBarHeaderView, "mAlarmShowing"))
            f = mDateTimeAlarmTranslation;
        else
            f = mDateTimeTranslation;
        mAlarmTranslation = builder.addFloat(mDateTimeAlarmGroup, "translationY", 0.0F, f).build();
        mAlarmTranslation.setPosition(XposedHelpers.getFloatField(mStatusBarHeaderView, "mCurrentT"));
    }

    private static void updateAlarmVisibilities() {
        XposedHook.logI(TAG, "updateAlarmVisibilities()");
        int v;
        if (XposedHelpers.getBooleanField(mStatusBarHeaderView, "mAlarmShowing")) {
            v = View.VISIBLE;
        } else {
            v = View.INVISIBLE;
        }
        mAlarmStatus.setVisibility(v);
        mAlarmStatusCollapsed.setVisibility(v);
    }

    public static void hook(ClassLoader classLoader, XSharedPreferences prefs) {
        try {
            if (prefs.getBoolean("enable_notification_tweaks", true)) {

                Class<?> classStatusBarHeaderView = XposedHelpers.findClass(CLASS_STATUS_BAR_HEADER_VIEW, classLoader);

                XposedHelpers.findAndHookMethod(classStatusBarHeaderView, "onFinishInflate", onFinishInflateHook);
                XposedHelpers.findAndHookMethod(classStatusBarHeaderView, "setExpansion", float.class, setExpansionHook);
                XposedHelpers.findAndHookMethod(classStatusBarHeaderView, "onConfigurationChanged", Configuration.class, onConfigurationChangedHook);
                XposedHelpers.findAndHookMethod(classStatusBarHeaderView, "updateEverything", updateEverythingHook);
                XposedHelpers.findAndHookMethod(classStatusBarHeaderView, "updateVisibilities", updateVisibilitiesHook);

                // Yes, this is a typo. Not my typo though. A typo in the source code that nobody noticed and that got compiled. ("interpoloate")
                XposedHelpers.findAndHookMethod(CLASS_LAYOUT_VALUES, classLoader, "interpoloate", CLASS_LAYOUT_VALUES, CLASS_LAYOUT_VALUES, float.class, XC_MethodReplacement.DO_NOTHING);
                XposedHelpers.findAndHookMethod(classStatusBarHeaderView, "requestCaptureValues", XC_MethodReplacement.DO_NOTHING);
                XposedHelpers.findAndHookMethod(classStatusBarHeaderView, "applyLayoutValues", CLASS_LAYOUT_VALUES, XC_MethodReplacement.DO_NOTHING);
                XposedHelpers.findAndHookMethod(classStatusBarHeaderView, "captureLayoutValues", CLASS_LAYOUT_VALUES, XC_MethodReplacement.DO_NOTHING);
                XposedHelpers.findAndHookMethod(classStatusBarHeaderView, "updateLayoutValues", float.class, XC_MethodReplacement.DO_NOTHING);
                XposedHelpers.findAndHookMethod(classStatusBarHeaderView, "updateClockCollapsedMargin", XC_MethodReplacement.DO_NOTHING);
                XposedHelpers.findAndHookMethod(classStatusBarHeaderView, "updateHeights", XC_MethodReplacement.DO_NOTHING);
                XposedHelpers.findAndHookMethod(classStatusBarHeaderView, "updateSignalClusterDetachment", XC_MethodReplacement.DO_NOTHING);
                XposedHelpers.findAndHookMethod(classStatusBarHeaderView, "updateSystemIconsLayoutParams", XC_MethodReplacement.DO_NOTHING);
                XposedHelpers.findAndHookMethod(classStatusBarHeaderView, "updateAvatarScale", XC_MethodReplacement.DO_NOTHING);
                XposedHelpers.findAndHookMethod(classStatusBarHeaderView, "updateClockScale", XC_MethodReplacement.DO_NOTHING);
                XposedHelpers.findAndHookMethod(classStatusBarHeaderView, "updateAmPmTranslation", XC_MethodReplacement.DO_NOTHING);
                XposedHelpers.findAndHookMethod(classStatusBarHeaderView, "updateClockLp", XC_MethodReplacement.DO_NOTHING);
                XposedHelpers.findAndHookMethod(classStatusBarHeaderView, "updateMultiUserSwitch", XC_MethodReplacement.DO_NOTHING);

                XposedHelpers.findAndHookMethod(classStatusBarHeaderView, "onLayout", boolean.class, int.class, int.class, int.class, int.class, new XC_MethodHook() {
                    float prevX;

                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        prevX = mAlarmStatus.getX();
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        mAlarmStatus.setX(prevX);
                    }
                });

                classAlphaOptimizedButton = XposedHelpers.findClass("com.android.systemui.statusbar.AlphaOptimizedButton", classLoader);

            }
        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error in hook", t);
        }
    }

}
