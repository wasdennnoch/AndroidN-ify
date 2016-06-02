package tk.wasdennnoch.androidn_ify.notifications;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.XModuleResources;
import android.content.res.XResources;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Process;
import android.support.annotation.Nullable;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import com.android.internal.logging.MetricsLogger;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LayoutInflated;
import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.extracted.systemui.AlphaOptimizedButton;
import tk.wasdennnoch.androidn_ify.extracted.systemui.ExpandableIndicator;
import tk.wasdennnoch.androidn_ify.extracted.systemui.TouchAnimator;
import tk.wasdennnoch.androidn_ify.notifications.qs.AvailableTileAdapter;
import tk.wasdennnoch.androidn_ify.notifications.qs.QSTileHostHooks;
import tk.wasdennnoch.androidn_ify.notifications.qs.TileAdapter;
import tk.wasdennnoch.androidn_ify.notifications.qs.tiles.BluetoothTileHook;
import tk.wasdennnoch.androidn_ify.notifications.qs.tiles.CellularTileHook;
import tk.wasdennnoch.androidn_ify.notifications.qs.tiles.WifiTileHook;
import tk.wasdennnoch.androidn_ify.ui.SettingsActivity;
import tk.wasdennnoch.androidn_ify.utils.ConfigUtils;
import tk.wasdennnoch.androidn_ify.utils.ResourceUtils;

public class StatusBarHeaderHooks {

    private static final String TAG = "StatusBarHeaderHooks";

    private static final int WRAP_CONTENT = ViewGroup.LayoutParams.WRAP_CONTENT;
    private static final String PACKAGE_SYSTEMUI = XposedHook.PACKAGE_SYSTEMUI;
    private static final String CLASS_STATUS_BAR_HEADER_VIEW = "com.android.systemui.statusbar.phone.StatusBarHeaderView";
    private static final String CLASS_LAYOUT_VALUES = CLASS_STATUS_BAR_HEADER_VIEW + "$LayoutValues";
    private static final String CLASS_QS_DRAG_PANEL = "com.android.systemui.qs.QSDragPanel";
    private static final String CLASS_QS_PANEL = "com.android.systemui.qs.QSPanel";
    private static final String CLASS_QS_TILE = "com.android.systemui.qs.QSTile";
    private static final String CLASS_QS_STATE = CLASS_QS_TILE + "$State";
    private static final String CLASS_QS_TILE_VIEW = "com.android.systemui.qs.QSTileView";
    private static final String CLASS_DETAIL_ADAPTER = CLASS_QS_TILE + "$DetailAdapter";

    private static boolean mHasEditPanel = false;
    private static boolean mCollapseAfterHideDatails = false;
    private static boolean mHideTunerIcon = false;
    private static boolean mHideEditTiles = false;
    private static boolean mHideCarrierLabel = false;

    private static TouchAnimator mAlarmTranslation;
    private static TouchAnimator mDateSizeAnimator;
    private static TouchAnimator mFirstHalfAnimator;
    private static TouchAnimator mSecondHalfAnimator;
    private static TouchAnimator mSettingsAlpha;

    public static RelativeLayout mStatusBarHeaderView;

    private static View mSystemIconsSuperContainer;
    private static View mDateGroup;
    private static FrameLayout mMultiUserSwitch;
    private static TextView mDateCollapsed;
    private static TextView mDateExpanded;
    private static View mSettingsButton;
    private static View mSettingsContainer;
    private static View mQsDetailHeader;
    private static TextView mQsDetailHeaderTitle;
    private static Switch mQsDetailHeaderSwitch;
    private static TextView mAlarmStatus;
    private static TextView mEditTileDoneText;
    private static View mTunerIcon;
    private static View mWeatherContainer;
    private static View mTaskManagerButton;
    private static View mSomcQuickSettings;
    private static View mCarrierText = null;

    private static ExpandableIndicator mExpandIndicator;
    private static LinearLayout mDateTimeAlarmGroup;
    private static LinearLayout mDateTimeGroup;
    private static LinearLayout mLeftContainer;
    private static LinearLayout mRightContainer;
    private static Button mAlarmStatusCollapsed;

    private static QuickQSPanel mHeaderQsPanel;
    public static ViewGroup mQsPanel;
    public static ViewGroup mQsContainer;

    private static Context mContext;

    private static Object mEditAdapter;
    private static NestedScrollView mEditView;
    private static RecyclerView mRecyclerView;
    private static RecyclerView mSecondRecyclerView;
    public static Button mEditButton;
    public static TileAdapter mTileAdapter;
    public static AvailableTileAdapter mAvailableTileAdapter;
    private static ItemTouchHelper mItemTouchHelper;
    private static ResourceUtils mResUtils;

    private static int mBarState = 2;

    private static boolean mEditing;
    public static boolean mShowingDetail;

    private static ArrayList<Object> mRecords;

    private static XC_MethodHook onFinishInflateHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {

            XposedHook.logD(TAG, "SystemUI PID: " + Process.myPid());

            mStatusBarHeaderView = (RelativeLayout) param.thisObject;
            Context context = mStatusBarHeaderView.getContext();
            ResourceUtils res = ResourceUtils.getInstance(context);
            ConfigUtils config = ConfigUtils.getInstance();

            mContext = mStatusBarHeaderView.getContext();

            try {
                //noinspection deprecation
                mStatusBarHeaderView.setBackgroundColor(context.getResources().getColor(context.getResources().getIdentifier("system_primary_color", "color", PACKAGE_SYSTEMUI)));
            } catch (Throwable t) {
                XposedHook.logE(TAG, "Couldn't change header background color", t);
            }

            View mClock;
            TextView mTime;
            TextView mAmPm;
            TextView mEmergencyCallsOnly;
            try {
                mSystemIconsSuperContainer = (View) XposedHelpers.getObjectField(param.thisObject, "mSystemIconsSuperContainer");
                mDateGroup = (View) XposedHelpers.getObjectField(param.thisObject, "mDateGroup");
                mClock = (View) XposedHelpers.getObjectField(param.thisObject, "mClock");
                mTime = (TextView) XposedHelpers.getObjectField(param.thisObject, "mTime");
                mAmPm = (TextView) XposedHelpers.getObjectField(param.thisObject, "mAmPm");
                mMultiUserSwitch = (FrameLayout) XposedHelpers.getObjectField(param.thisObject, "mMultiUserSwitch");
                mDateCollapsed = (TextView) XposedHelpers.getObjectField(param.thisObject, "mDateCollapsed");
                mDateExpanded = (TextView) XposedHelpers.getObjectField(param.thisObject, "mDateExpanded");
                mSettingsButton = (View) XposedHelpers.getObjectField(param.thisObject, "mSettingsButton");
                mQsDetailHeader = (View) XposedHelpers.getObjectField(param.thisObject, "mQsDetailHeader");
                mQsDetailHeaderTitle = (TextView) XposedHelpers.getObjectField(param.thisObject, "mQsDetailHeaderTitle");
                mQsDetailHeaderSwitch = (Switch) XposedHelpers.getObjectField(param.thisObject, "mQsDetailHeaderSwitch");
                mEmergencyCallsOnly = (TextView) XposedHelpers.getObjectField(param.thisObject, "mEmergencyCallsOnly");
                mAlarmStatus = (TextView) XposedHelpers.getObjectField(param.thisObject, "mAlarmStatus");
                if (mHasEditPanel) {
                    mEditTileDoneText = (TextView) XposedHelpers.getObjectField(param.thisObject, "mEditTileDoneText");
                }
            } catch (Throwable t) {
                XposedHook.logE(TAG, "Couldn't find required views, aborting", t);
                return;
            }
            // Separate try-catch for settings button as some ROMs removed the container around it
            try {
                mSettingsContainer = (View) XposedHelpers.getObjectField(param.thisObject, "mSettingsContainer");
            } catch (Throwable t) {
                XposedHook.logD(TAG, "No mSettingsContainer view (" + t.getClass().getSimpleName() + ")");
                mSettingsContainer = mSettingsButton;
            }
            mTunerIcon = mSettingsContainer.findViewById(context.getResources().getIdentifier("tuner_icon", "id", PACKAGE_SYSTEMUI));
            mHideTunerIcon = config.header.hide_tuner_icon;
            mHideEditTiles = config.header.hide_edit_tiles;
            mHideCarrierLabel = config.header.hide_carrier_label;
            View dummyClock = new View(context);
            dummyClock.setVisibility(View.GONE);
            XposedHelpers.setObjectField(param.thisObject, "mClock", dummyClock);
            try {
                mWeatherContainer = (View) XposedHelpers.getObjectField(param.thisObject, "mWeatherContainer");
            } catch (Throwable t) {
                XposedHook.logD(TAG, "No mWeatherContainer view (" + t.getClass().getSimpleName() + ")");
            }
            try {
                mCarrierText = (View) XposedHelpers.getObjectField(param.thisObject, "mCarrierText");
            } catch (Throwable t) {
                XposedHook.logD(TAG, "No mCarrierText view (" + t.getClass().getSimpleName() + ")");
            }
            try {
                mTaskManagerButton = (View) XposedHelpers.getObjectField(param.thisObject, "mTaskManagerButton");
            } catch (Throwable t) {
                XposedHook.logD(TAG, "No mTaskManagerButton view (" + t.getClass().getSimpleName() + ")");
            }
            try {
                mSomcQuickSettings = (View) XposedHelpers.getObjectField(param.thisObject, "mSomcQuickSettings");
            } catch (Throwable t) {
                XposedHook.logD(TAG, "No mSomcQuickSettings view (" + t.getClass().getSimpleName() + ")");
                try { // OOS2
                    mSomcQuickSettings = (View) XposedHelpers.getObjectField(param.thisObject, "mEditModeButton");
                } catch (Throwable t2) {
                    XposedHook.logD(TAG, "No mEditModeButton view (" + t2.getClass().getSimpleName() + ")");
                }
            }

            try {

                boolean mShowTaskManager = true;
                try {
                    mShowTaskManager = XposedHelpers.getBooleanField(param.thisObject, "mShowTaskManager");
                } catch (Throwable ignore) {
                }

                int rippleRes = context.getResources().getIdentifier("ripple_drawable", "drawable", XposedHook.PACKAGE_SYSTEMUI);
                int rightIconHeight = res.getDimensionPixelSize(R.dimen.right_icon_size);
                int rightIconWidth = mTaskManagerButton != null && mShowTaskManager ? res.getDimensionPixelSize(R.dimen.right_icon_width_small) : rightIconHeight;
                int expandIndicatorPadding = res.getDimensionPixelSize(R.dimen.expand_indicator_padding);
                int headerItemsMarginTop = res.getDimensionPixelSize(R.dimen.header_items_margin_top);
                int alarmStatusTextColor = res.getColor(R.color.alarm_status_text_color);
                int dateTimeCollapsedSize = res.getDimensionPixelSize(R.dimen.date_time_collapsed_size);
                int dateTimeTextColor = res.getColor(R.color.date_time_text_color);
                int dateCollapsedDrawablePadding = res.getDimensionPixelSize(R.dimen.date_collapsed_drawable_padding);
                int dateTimeMarginLeft = res.getDimensionPixelSize(R.dimen.date_time_alarm_group_margin_left);
                Drawable alarmSmall = context.getDrawable(context.getResources().getIdentifier("ic_access_alarms_small", "drawable", XposedHook.PACKAGE_SYSTEMUI));

                ((ViewGroup) mClock.getParent()).removeView(mClock);
                ((ViewGroup) mMultiUserSwitch.getParent()).removeView(mMultiUserSwitch);
                ((ViewGroup) mDateCollapsed.getParent()).removeView(mDateCollapsed);
                ((ViewGroup) mSettingsContainer.getParent()).removeView(mSettingsContainer);
                ((ViewGroup) mAlarmStatus.getParent()).removeView(mAlarmStatus);
                ((ViewGroup) mEmergencyCallsOnly.getParent()).removeView(mEmergencyCallsOnly);

                RelativeLayout.LayoutParams rightContainerLp = new RelativeLayout.LayoutParams(WRAP_CONTENT, res.getDimensionPixelSize(R.dimen.right_layout_height));
                rightContainerLp.addRule(RelativeLayout.ALIGN_PARENT_END);
                rightContainerLp.rightMargin = res.getDimensionPixelSize(R.dimen.right_layout_margin_right);
                rightContainerLp.topMargin = res.getDimensionPixelSize(R.dimen.right_layout_margin_top);
                mRightContainer = new LinearLayout(context);
                mRightContainer.setLayoutParams(rightContainerLp);
                mRightContainer.setGravity(Gravity.CENTER);
                mRightContainer.setOrientation(LinearLayout.HORIZONTAL);
                mRightContainer.setClipChildren(false);

                LinearLayout.LayoutParams multiUserSwitchLp = new LinearLayout.LayoutParams(rightIconWidth, rightIconHeight);
                mMultiUserSwitch.setLayoutParams(multiUserSwitchLp);

                LinearLayout.LayoutParams settingsContainerLp = new LinearLayout.LayoutParams(rightIconWidth, rightIconHeight);
                mSettingsContainer.setLayoutParams(settingsContainerLp);

                LinearLayout.LayoutParams expandIndicatorLp = new LinearLayout.LayoutParams(rightIconHeight, rightIconHeight); // Requires full width
                mExpandIndicator = new ExpandableIndicator(context);
                mExpandIndicator.setLayoutParams(expandIndicatorLp);
                mExpandIndicator.setPadding(expandIndicatorPadding, expandIndicatorPadding, expandIndicatorPadding, expandIndicatorPadding);
                mExpandIndicator.setClickable(true);
                mExpandIndicator.setFocusable(true);
                mExpandIndicator.setFocusableInTouchMode(false);
                mExpandIndicator.setCropToPadding(false);
                mExpandIndicator.setBackgroundResource(rippleRes);
                mExpandIndicator.setId(R.id.statusbar_header_expand_indicator);


                RelativeLayout.LayoutParams leftContainerLp = new RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
                leftContainerLp.addRule(RelativeLayout.ALIGN_PARENT_START);
                leftContainerLp.leftMargin = dateTimeMarginLeft;
                leftContainerLp.topMargin = headerItemsMarginTop;
                mLeftContainer = new LinearLayout(context);
                mLeftContainer.setLayoutParams(leftContainerLp);
                mLeftContainer.setOrientation(LinearLayout.VERTICAL);

                RelativeLayout.LayoutParams emergencyCallsOnlyLp = new RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
                emergencyCallsOnlyLp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                mEmergencyCallsOnly.setLayoutParams(emergencyCallsOnlyLp);
                //noinspection deprecation
                mEmergencyCallsOnly.setTextSize(TypedValue.COMPLEX_UNIT_PX, res.getDimensionPixelSize(R.dimen.emergency_calls_only_text_size));
                mEmergencyCallsOnly.setTextColor(res.getColor(R.color.emergency_calls_only_text_color));
                mEmergencyCallsOnly.setPadding(0, 0, 0, 0);
                mEmergencyCallsOnly.setVisibility(View.GONE);


                RelativeLayout.LayoutParams dateTimeAlarmGroupLp = new RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
                dateTimeAlarmGroupLp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                dateTimeAlarmGroupLp.topMargin = res.getDimensionPixelSize(R.dimen.date_time_alarm_group_margin_top);
                dateTimeAlarmGroupLp.leftMargin = dateTimeMarginLeft;
                mDateTimeAlarmGroup = new LinearLayout(context);
                mDateTimeAlarmGroup.setLayoutParams(dateTimeAlarmGroupLp);
                mDateTimeAlarmGroup.setId(View.generateViewId());
                mDateTimeAlarmGroup.setGravity(Gravity.START);
                mDateTimeAlarmGroup.setOrientation(LinearLayout.VERTICAL);
                mDateTimeAlarmGroup.setBaselineAligned(false);

                LinearLayout.LayoutParams alarmStatusLp = new LinearLayout.LayoutParams(WRAP_CONTENT, res.getDimensionPixelSize(R.dimen.alarm_status_height));
                mAlarmStatus.setLayoutParams(alarmStatusLp);
                mAlarmStatus.setGravity(Gravity.TOP);
                //noinspection deprecation
                mAlarmStatus.setTextColor(alarmStatusTextColor);
                mAlarmStatus.setTextSize(TypedValue.COMPLEX_UNIT_PX, dateTimeCollapsedSize);
                mAlarmStatus.setPadding(0, res.getDimensionPixelSize(R.dimen.alarm_status_padding_top), 0, 0);
                mAlarmStatus.setCompoundDrawablePadding(res.getDimensionPixelSize(R.dimen.alarm_status_drawable_padding));
                mAlarmStatus.setCompoundDrawablesWithIntrinsicBounds(alarmSmall, null, null, null);
                mAlarmStatus.setVisibility(View.GONE);
                mAlarmStatus.setBackgroundResource(rippleRes);


                LinearLayout.LayoutParams dateTimeGroupLp = new LinearLayout.LayoutParams(WRAP_CONTENT, res.getDimensionPixelSize(R.dimen.date_time_group_height));
                mDateTimeGroup = new LinearLayout(context);
                mDateTimeGroup.setLayoutParams(dateTimeGroupLp);
                mDateTimeGroup.setId(View.generateViewId());
                mDateTimeGroup.setOrientation(LinearLayout.HORIZONTAL);
                mDateTimeGroup.setPivotX(0.0F);
                mDateTimeGroup.setPivotY(0.0F);
                mDateTimeGroup.setBaselineAligned(false);

                LinearLayout.LayoutParams clockLp = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
                mClock.setLayoutParams(clockLp);
                mClock.findViewById(context.getResources().getIdentifier("empty_time_view", "id", XposedHook.PACKAGE_SYSTEMUI)).setVisibility(View.GONE);

                mTime.setTextColor(dateTimeTextColor);
                mTime.setTextSize(TypedValue.COMPLEX_UNIT_PX, dateTimeCollapsedSize);
                mAmPm.setTextColor(dateTimeTextColor);
                mAmPm.setTextSize(TypedValue.COMPLEX_UNIT_PX, dateTimeCollapsedSize);
                mAmPm.setPadding(0, 0, dateCollapsedDrawablePadding, 0);

                LinearLayout.LayoutParams dateCollapsedLp = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
                mDateCollapsed.setLayoutParams(dateCollapsedLp);
                mDateCollapsed.setGravity(Gravity.TOP);
                mDateCollapsed.setTextColor(dateTimeTextColor);
                mDateCollapsed.setTextSize(TypedValue.COMPLEX_UNIT_PX, dateTimeCollapsedSize);
                mDateCollapsed.setCompoundDrawablesWithIntrinsicBounds(res.getDrawable(R.drawable.header_dot), null, null, null);
                mDateCollapsed.setCompoundDrawablePadding(dateCollapsedDrawablePadding);

                LinearLayout.LayoutParams alarmStatusCollapsedLp = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
                mAlarmStatusCollapsed = new AlphaOptimizedButton(context);
                mAlarmStatusCollapsed.setLayoutParams(alarmStatusCollapsedLp);
                mAlarmStatusCollapsed.setId(View.generateViewId());
                mAlarmStatusCollapsed.setGravity(Gravity.TOP);
                //noinspection deprecation
                mAlarmStatusCollapsed.setTextColor(alarmStatusTextColor);
                mAlarmStatusCollapsed.setTextSize(TypedValue.COMPLEX_UNIT_PX, dateTimeCollapsedSize);
                mAlarmStatusCollapsed.setClickable(false);
                mAlarmStatusCollapsed.setFocusable(false);
                mAlarmStatusCollapsed.setVisibility(View.GONE);
                //noinspection deprecation
                mAlarmStatusCollapsed.setCompoundDrawablesWithIntrinsicBounds(alarmSmall, null, null, null);
                mAlarmStatusCollapsed.setBackgroundResource(0);
                mAlarmStatusCollapsed.setPadding(res.getDimensionPixelSize(R.dimen.alarm_status_collapsed_drawable_padding), 0, 0, 0);


                RelativeLayout.LayoutParams headerQsPanelLp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, WRAP_CONTENT);
                mHeaderQsPanel = new QuickQSPanel(context);
                mHeaderQsPanel.setLayoutParams(headerQsPanelLp);
                mHeaderQsPanel.setClipChildren(false);
                mHeaderQsPanel.setClipToPadding(false);


                if (mWeatherContainer != null) {
                    RelativeLayout.LayoutParams weatherContainerLp = new RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
                    weatherContainerLp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                    weatherContainerLp.addRule(RelativeLayout.ALIGN_PARENT_END);
                    weatherContainerLp.topMargin = headerItemsMarginTop;
                    mWeatherContainer.setLayoutParams(weatherContainerLp);
                }
                if (mCarrierText != null) {
                    ((ViewGroup) mCarrierText.getParent()).removeView(mCarrierText);
                    RelativeLayout.LayoutParams carrierTextLp = new RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
                    mCarrierText.setLayoutParams(carrierTextLp);
                    mCarrierText.setPadding(0, 0, 0, 0);
                }
                if (mTaskManagerButton != null) {
                    ((ViewGroup) mTaskManagerButton.getParent()).removeView(mTaskManagerButton);
                    LinearLayout.LayoutParams taskManagerButtonLp = new LinearLayout.LayoutParams(rightIconWidth, rightIconHeight);
                    mTaskManagerButton.setLayoutParams(taskManagerButtonLp);
                }


                if (mCarrierText != null)
                    mLeftContainer.addView(mCarrierText);
                mLeftContainer.addView(mEmergencyCallsOnly);
                if (mTaskManagerButton != null)
                    mRightContainer.addView(mTaskManagerButton);
                mRightContainer.addView(mMultiUserSwitch);
                mRightContainer.addView(mSettingsContainer);
                mRightContainer.addView(mExpandIndicator);
                mDateTimeGroup.addView(mClock);
                mDateTimeGroup.addView(mDateCollapsed);
                mDateTimeGroup.addView(mAlarmStatusCollapsed);
                mDateTimeAlarmGroup.addView(mDateTimeGroup);
                mDateTimeAlarmGroup.addView(mAlarmStatus);
                mStatusBarHeaderView.addView(mLeftContainer);
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
                    mHeaderQsPanel.setPosition(f);
                }
                //mHeaderQsPanel.setVisibility(f < 0.36F ? View.VISIBLE : View.INVISIBLE);
                mExpandIndicator.setExpanded(f > 0.93F);
            } catch (Throwable ignore) {
                // Oh god, a massive spam wall coming right at you, quick, hide!
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
                boolean mExpanded = XposedHelpers.getBooleanField(param.thisObject, "mExpanded");
                mSystemIconsSuperContainer.setVisibility(View.GONE);
                mDateExpanded.setVisibility(View.GONE);
                mDateGroup.setVisibility(View.GONE);
                mDateCollapsed.setVisibility(View.VISIBLE);
                updateAlarmVisibilities();
                mMultiUserSwitch.setVisibility(mExpanded ? View.VISIBLE : View.INVISIBLE);
                mAlarmStatus.setVisibility(mExpanded && XposedHelpers.getBooleanField(mStatusBarHeaderView, "mAlarmShowing") ? View.VISIBLE : View.INVISIBLE);
                if (mHideTunerIcon && mTunerIcon != null) mTunerIcon.setVisibility(View.INVISIBLE);
                if (mHideEditTiles && mSomcQuickSettings != null)
                    mSomcQuickSettings.setVisibility(View.INVISIBLE);
                if (mHideCarrierLabel && mCarrierText != null)
                    mCarrierText.setVisibility(View.GONE);
                if (mWeatherContainer != null) {
                    try {
                        mWeatherContainer.setVisibility(mExpanded && XposedHelpers.getBooleanField(mStatusBarHeaderView, "mShowWeather") ? View.VISIBLE : View.INVISIBLE);
                    } catch (Throwable ignored) {
                    }
                }
            } else {
                XposedHook.logD(TAG, "updateVisibilitiesHook: mSystemIconsSuperContainer is still null");
            }
        }
    };
    private static XC_MethodHook setEditingHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            boolean editing = (boolean) param.args[0];
            boolean shouldShowViews = !editing && XposedHelpers.getBooleanField(param.thisObject, "mExpanded");
            if (mDateTimeAlarmGroup != null) {
                mDateTimeAlarmGroup.setVisibility(editing ? View.INVISIBLE : View.VISIBLE);
                mMultiUserSwitch.setVisibility(shouldShowViews ? View.VISIBLE : View.INVISIBLE);
                mSettingsContainer.setVisibility(shouldShowViews ? View.VISIBLE : View.INVISIBLE);
                mExpandIndicator.setVisibility(editing ? View.INVISIBLE : View.VISIBLE);
            } else {
                XposedHook.logD(TAG, "setEditingHook: mDateTimeAlarmGroup is still null");
            }
        }
    };

    private static XC_MethodHook setTilesHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            XposedHook.logD(TAG, "setTilesHook PID: " + Process.myPid());
            // This method gets called from two different processes,
            // so we have to check if we are in the right one
            mQsPanel = (ViewGroup) param.thisObject;
            if (mHeaderQsPanel != null) {
                try {
                    //noinspection unchecked
                    mRecords = (ArrayList<Object>) XposedHelpers.getObjectField(param.thisObject, "mRecords");
                } catch (Throwable t) {
                    try {
                        //noinspection unchecked
                        mRecords = (ArrayList<Object>) XposedHelpers.getObjectField(XposedHelpers.getObjectField(param.thisObject, "mGridView"), "mRecords");
                    } catch (Throwable t2) {
                        XposedHook.logE(TAG, "No tile record field found (" + t.getClass().getSimpleName() + " and " + t2.getClass().getSimpleName() + ")", null);
                        return;
                    }
                }
                if (mRecords.size() == 0) {
                    try {
                        //noinspection unchecked
                        mRecords = (ArrayList<Object>) XposedHelpers.getObjectField(XposedHelpers.getObjectField(param.thisObject, "mGridView"), "mRecords");
                    } catch (Throwable ignore) {
                    }
                }
                if (mTileAdapter != null) {
                    mTileAdapter.setRecords(mRecords);
                    mTileAdapter.notifyDataSetChanged();
                }
                mHeaderQsPanel.setTiles(mRecords);
            }
        }
    };

    private static XC_MethodHook handleStateChangedHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            XposedHook.logD(TAG, "handleStateChangedHook PID: " + Process.myPid());
            // This method gets called from two different processes,
            // so we have to check if we are in the right one
            if (mHeaderQsPanel != null) {
                mHeaderQsPanel.handleStateChanged(param.thisObject, XposedHelpers.getObjectField(param.thisObject, "mState"));
            }
            if (mTileAdapter != null) {
                mTileAdapter.handleStateChanged(param.thisObject, XposedHelpers.getObjectField(param.thisObject, "mState"));
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
        float timeExpanded;
        if (ConfigUtils.header().smaller_header_clock)
            timeExpanded = res.getDimensionPixelSize(R.dimen.date_time_expanded_size_small);
        else
            timeExpanded = res.getDimensionPixelSize(R.dimen.date_time_expanded_size);
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
                .setStartDelay(0.5F).build();
        TouchAnimator.Builder settingsAlphaBuilder = new TouchAnimator.Builder()
                .addFloat(mSettingsContainer, "translationY", -gearTranslation, 0.0F)
                .addFloat(mMultiUserSwitch, "translationY", -gearTranslation, 0.0F)
                .addFloat(mSettingsButton, "rotation", -90F, 0.0F)
                .addFloat(mSettingsContainer, "alpha", 0.0F, 1.0F)
                .addFloat(mMultiUserSwitch, "alpha", 0.0F, 1.0F)
                .addFloat(mLeftContainer, "alpha", 0.0F, 1.0F)
                .setStartDelay(0.7F);
        if (mWeatherContainer != null) {
            settingsAlphaBuilder
                    .addFloat(mWeatherContainer, "translationY", -gearTranslation, 0.0F)
                    .addFloat(mWeatherContainer, "alpha", 0.0F, 1.0F);
        }
        if (mTaskManagerButton != null) {
            settingsAlphaBuilder
                    .addFloat(mTaskManagerButton, "translationY", -gearTranslation, 0.0F)
                    .addFloat(mTaskManagerButton, "alpha", 0.0F, 1.0F);
        }
        if (mSomcQuickSettings != null)
            settingsAlphaBuilder.addFloat(mSomcQuickSettings, "alpha", 0.0F, 1.0F);
        mSettingsAlpha = settingsAlphaBuilder.build();

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
        // Fixes an issue with the indicator having two backgrounds when layer type is hardware
        mExpandIndicator.setLayerType(View.LAYER_TYPE_NONE, null);
        transition(mDateTimeAlarmGroup, !showingDetail);
        transition(mRightContainer, !showingDetail);
        transition(mExpandIndicator, !showingDetail);
        mEditButton.setVisibility((showingDetail || mBarState != NotificationPanelHooks.STATE_SHADE) ? View.GONE : View.VISIBLE);
        if (mWeatherContainer != null) {
            try {
                if (XposedHelpers.getBooleanField(mStatusBarHeaderView, "mShowWeather"))
                    transition(mWeatherContainer, !showingDetail);
            } catch (Throwable ignored) {
            }
        }
        transition(mQsDetailHeader, showingDetail);
        mShowingDetail = showingDetail;
        XposedHelpers.setBooleanField(mStatusBarHeaderView, "mShowingDetail", showingDetail);
        if (showingDetail) {
            mCollapseAfterHideDatails = NotificationPanelHooks.isCollapsed();
            NotificationPanelHooks.expandIfNecessary();
            try {
                mQsDetailHeaderTitle.setText((int) XposedHelpers.callMethod(detail, "getTitle"));
            } catch (Throwable t) {
                Context context = mQsDetailHeaderTitle.getContext();
                Class<?> classQSTile = XposedHelpers.findClass(CLASS_QS_TILE, context.getClassLoader());
                mQsDetailHeaderTitle.setText((String) XposedHelpers.callStaticMethod(classQSTile, "getDetailAdapterTitle", context, detail));
            }
            final Boolean toggleState = (Boolean) XposedHelpers.callMethod(detail, "getToggleState");
            if (mEditing || toggleState == null) {
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
            if (mHasEditPanel) {
                if ((int) XposedHelpers.callMethod(detail, "getTitle")
                        == mQsDetailHeader.getResources().getIdentifier("quick_settings_edit_label", "string", PACKAGE_SYSTEMUI)) {
                    mEditTileDoneText.setVisibility(View.VISIBLE);
                } else {
                    mEditTileDoneText.setVisibility(View.GONE);
                }
            }
            if (mEditing) {
                mQsDetailHeaderTitle.setText(getResUtils().getString(R.string.qs_edit_detail));
            }
        } else {
            if (mCollapseAfterHideDatails) NotificationPanelHooks.collapseIfNecessary();
            mQsDetailHeader.setClickable(false);
            if (mEditing) {
                mEditing = false;
                mTileAdapter.saveChanges();
                QSTileHostHooks.recreateTiles();
            }
        }
        FrameLayout.LayoutParams qsPanelLp = (FrameLayout.LayoutParams) mQsPanel.getLayoutParams();
        qsPanelLp.bottomMargin = (showingDetail) ? 0 : getResUtils().getDimensionPixelSize(R.dimen.qs_panel_margin_bottom);
        mQsPanel.setLayoutParams(qsPanelLp);
    }

    private static ResourceUtils getResUtils() {
        if (mResUtils == null)
            mResUtils = ResourceUtils.getInstance(mContext);

        return mResUtils;
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

    private static View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.qs_edit:
                    showEdit();
                    break;
            }
        }
    };

    private static void showEdit() {
        if (mEditAdapter == null)
            createEditAdapter();

        int x = mEditButton.getLeft() + mEditButton.getWidth() / 2;
        int y = mEditButton.getTop() + mEditButton.getHeight() / 2;
        if (mHasEditPanel)
            y += mStatusBarHeaderView.getHeight();
        mEditing = true;
        XposedHelpers.callMethod(mQsPanel, "showDetailAdapter", true, mEditAdapter, new int[] {x, y});
    }

    private static void createEditAdapter() {
        if (mEditView == null)
            createEditView();

        Class<?> classDetailAdapter = XposedHelpers.findClass(CLASS_DETAIL_ADAPTER, mContext.getClassLoader());

        mEditAdapter = Proxy.newProxyInstance(classDetailAdapter.getClassLoader(), new Class<?>[]{classDetailAdapter}, new InvocationHandler() {

            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if(method.getName().equals("getTitle")){
                    return mContext.getResources().getIdentifier("quick_settings_settings_label", "string", PACKAGE_SYSTEMUI);
                } else if (method.getName().equals("getToggleState")) {
                    return false;
                } else if (method.getName().equals("getSettingsIntent")) {
                    Intent intent = new Intent(mContext, SettingsActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    return intent;
                } else if (method.getName().equals("setToggleState")) {
                    return null;
                } else if (method.getName().equals("getMetricsCategory")) {
                    return MetricsLogger.QS_INTENT;
                } else if (method.getName().equals("createDetailView")) {
                    return mEditView;
                }
                return null;
            }
        });
    }

    private static void createEditView() {
        ResourceUtils res = ResourceUtils.getInstance(mContext);

        LinearLayout linearLayout = new LinearLayout(mContext);
        linearLayout.setLayoutParams(new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setClipChildren(false);

        mEditView = new NestedScrollView(mContext);
        mEditView.addView(linearLayout);
        mEditView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mEditView.setFillViewport(true);

        // Init tiles list
        mTileAdapter = new TileAdapter(mRecords, mContext, mQsPanel);
        TileTouchCallback callback = new TileTouchCallback();
        mItemTouchHelper = new CustomItemTouchHelper(callback);
        XposedHelpers.setIntField(callback, "mCachedMaxScrollSpeed", res.getDimensionPixelSize(R.dimen.lib_item_touch_helper_max_drag_scroll_per_frame));
        // With this, it's very easy to deal with drag & drop
        mRecyclerView = new RecyclerView(mContext);
        mRecyclerView.setLayoutManager(new GridLayoutManager(mContext, 3));
        mRecyclerView.setAdapter(mTileAdapter);
        mRecyclerView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        mRecyclerView.setNestedScrollingEnabled(false);
        mRecyclerView.setClipChildren(false);
        mTileAdapter.setOnStartDragListener(callback);

        // Init available tiles list
        mAvailableTileAdapter = new AvailableTileAdapter(mRecords, mContext, mQsPanel);
        mSecondRecyclerView = new RecyclerView(mContext);
        mSecondRecyclerView.setLayoutManager(new GridLayoutManager(mContext, 3));
        mSecondRecyclerView.setAdapter(mAvailableTileAdapter);
        mSecondRecyclerView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        mSecondRecyclerView.setNestedScrollingEnabled(false);
        mSecondRecyclerView.setBackground(new ColorDrawable(0xFF384248));
        mRecyclerView.setClipChildren(false);

        mItemTouchHelper.attachToRecyclerView(mRecyclerView);

        linearLayout.addView(mRecyclerView);
        linearLayout.addView(mSecondRecyclerView);
    }

    public static void onSetBarState(int state) {
        mBarState = state;
        if (mEditButton != null) {
            int visibility = (state == NotificationPanelHooks.STATE_SHADE) ? View.VISIBLE : View.GONE;
            mEditButton.setVisibility(visibility);
        }
    }

    private static class CustomItemTouchHelper extends ItemTouchHelper {

        /**
         * Creates an ItemTouchHelper that will work with the given Callback.
         * <p>
         * You can attach ItemTouchHelper to a RecyclerView via
         * {@link #attachToRecyclerView(RecyclerView)}. Upon attaching, it will add an item decoration,
         * an onItemTouchListener and a Child attach / detach listener to the RecyclerView.
         *
         * @param callback The Callback which controls the behavior of this touch helper.
         */
        public CustomItemTouchHelper(Callback callback) {
            super(callback);
        }

        @Override
        public void attachToRecyclerView(@Nullable RecyclerView recyclerView) {
            try {
                RecyclerView oldRecyclerView = (RecyclerView) XposedHelpers.getObjectField(this, "mRecyclerView");
                if (oldRecyclerView == recyclerView) {
                    return; // nothing to do
                }
                if (oldRecyclerView != null) {
                    XposedHelpers.findMethodBestMatch(ItemTouchHelper.class, "destroyCallbacks").invoke(this);
                }
                XposedHelpers.setObjectField(this, "mRecyclerView", recyclerView);
                if (recyclerView != null) {
                    XposedHelpers.findMethodBestMatch(ItemTouchHelper.class, "setupCallbacks").invoke(this);
                }
            } catch (Throwable t) {
                XposedHook.logE(TAG, "Error attaching ItemTouchCallback to RecyclerView", t);
            }
        }
    }

    public interface OnStartDragListener {

        void onStartDrag(RecyclerView.ViewHolder viewHolder);
    }

    private static class TileTouchCallback extends ItemTouchHelper.Callback implements OnStartDragListener {

        @Override
        public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.START | ItemTouchHelper.END;
            return makeMovementFlags(dragFlags, 0);
        }

        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            return mTileAdapter.onItemMove(viewHolder.getAdapterPosition(),
                    target.getAdapterPosition());
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {

        }

        @Override
        public boolean isLongPressDragEnabled() {
            return true;
        }

        @Override
        public boolean isItemViewSwipeEnabled() {
            return false;
        }

        @Override
        public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
            mItemTouchHelper.startDrag(viewHolder);
        }

        @Override
        public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
            if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                float scale = 1.1f;
                viewHolder.itemView.setScaleX(scale);
                viewHolder.itemView.setScaleY(scale);
                try {
                    ((View) XposedHelpers.callMethod(((RelativeLayout) viewHolder.itemView).getChildAt(0), "labelView")).setVisibility(View.GONE);
                } catch (Throwable ignore) {
                    // Not an important thing
                }
            }
            super.onSelectedChanged(viewHolder, actionState);
        }

        @Override
        public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            super.clearView(recyclerView, viewHolder);

            float scale = 1f;
            viewHolder.itemView.setScaleX(scale);
            viewHolder.itemView.setScaleY(scale);
            try {
                ((View) XposedHelpers.callMethod(((RelativeLayout) viewHolder.itemView).getChildAt(0), "labelView")).setVisibility(View.VISIBLE);
            } catch (Throwable ignore) {
                // Not an important thing
            }
        }
    }

    public static void hook(ClassLoader classLoader) {
        try {
            if (ConfigUtils.header().header) {

                Class<?> classStatusBarHeaderView = XposedHelpers.findClass(CLASS_STATUS_BAR_HEADER_VIEW, classLoader);
                Class<?> classLayoutValues = XposedHelpers.findClass(CLASS_LAYOUT_VALUES, classLoader);
                Class<?> classQSPanel = XposedHelpers.findClass(CLASS_QS_PANEL, classLoader);
                Class<?> classQSTile = XposedHelpers.findClass(CLASS_QS_TILE, classLoader);
                Class<?> classQSTileView = XposedHelpers.findClass(CLASS_QS_TILE_VIEW, classLoader);

                try {
                    XposedHelpers.findAndHookMethod(classStatusBarHeaderView, "setEditing", boolean.class, setEditingHook);
                    mHasEditPanel = true;
                } catch (NoSuchMethodError e) {
                    mHasEditPanel = false;
                }

                XposedHelpers.findAndHookMethod(classStatusBarHeaderView, "onFinishInflate", onFinishInflateHook);
                XposedHelpers.findAndHookMethod(classStatusBarHeaderView, "setExpansion", float.class, setExpansionHook);
                XposedHelpers.findAndHookMethod(classStatusBarHeaderView, "onConfigurationChanged", Configuration.class, onConfigurationChangedHook);
                XposedHelpers.findAndHookMethod(classStatusBarHeaderView, "updateEverything", updateEverythingHook);
                XposedHelpers.findAndHookMethod(classStatusBarHeaderView, "updateVisibilities", updateVisibilitiesHook);

                // Every time you make a typo, the errorists win.
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
                XposedHelpers.findAndHookMethod(classStatusBarHeaderView, "updateAmPmTranslation", XC_MethodReplacement.DO_NOTHING);
                XposedHelpers.findAndHookMethod(classStatusBarHeaderView, "updateClockLp", XC_MethodReplacement.DO_NOTHING);
                XposedHelpers.findAndHookMethod(classStatusBarHeaderView, "updateMultiUserSwitch", XC_MethodReplacement.DO_NOTHING);
                XposedHelpers.findAndHookMethod(classStatusBarHeaderView, "setClipping", float.class, XC_MethodReplacement.DO_NOTHING);

                XposedHelpers.findAndHookMethod(classStatusBarHeaderView, "onLayout", boolean.class, int.class, int.class, int.class, int.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        mAlarmStatus.setX(0);
                    }
                });

                XposedHelpers.findAndHookMethod(classQSPanel, "fireShowingDetail", CLASS_DETAIL_ADAPTER, new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                        handleShowingDetail(param.args[0]);
                        return null;
                    }
                });

                boolean isCm = false;

                try {
                    Class<?> classQSDragPanel = XposedHelpers.findClass(CLASS_QS_DRAG_PANEL, classLoader);
                    XposedHelpers.findAndHookMethod(classQSDragPanel, "setTiles", Collection.class, setTilesHook);
                    isCm = true;
                } catch (Throwable ignore) {
                    XposedHelpers.findAndHookMethod(classQSPanel, "setTiles", Collection.class, setTilesHook);
                }

                QSTileHostHooks.hook(classLoader);
                
                boolean firstRowLarge = ConfigUtils.header().large_first_row;
                if (ConfigUtils.header().new_click_behavior) {
                    new WifiTileHook(classLoader, (!isCm && !firstRowLarge));
                    new BluetoothTileHook(classLoader, (!isCm && !firstRowLarge));
                    new CellularTileHook(classLoader);
                }

                XposedHelpers.findAndHookMethod(classQSTile, "handleStateChanged", handleStateChangedHook);

                if (Build.VERSION.SDK_INT >= 22) {
                    XposedHelpers.findAndHookMethod(classQSTileView, "setIcon", ImageView.class, CLASS_QS_STATE, new XC_MethodHook() {
                        boolean forceAnim = false;

                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            View iv = (View) param.args[0];
                            Object headerItem = XposedHelpers.getAdditionalInstanceField(param.thisObject, "headerTileRowItem");
                            forceAnim = headerItem != null && (boolean) headerItem &&
                                    !Objects.equals(XposedHelpers.getObjectField(param.args[1], "icon"),
                                            iv.getTag(iv.getResources().getIdentifier("qs_icon_tag", "id", PACKAGE_SYSTEMUI)));
                        }

                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            XposedHook.logD(TAG, "Animating QuickQS icon: " + forceAnim);
                            if (forceAnim) {
                                View iconView = (View) XposedHelpers.getObjectField(param.thisObject, "mIcon");
                                if (iconView instanceof ImageView) {
                                    Drawable icon = ((ImageView) iconView).getDrawable();
                                    if (icon instanceof Animatable) {
                                        ((Animatable) icon).start();
                                    }
                                }
                            }
                        }
                    });
                }

            }
        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error in hook", t);
        }
    }

    public static void hookResSystemui(XC_InitPackageResources.InitPackageResourcesParam resparam, String modulePath) {
        try {
            if (ConfigUtils.header().header) {

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

                if (!ConfigUtils.header().large_first_row) {
                    try {
                        resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "qs_dual_tile_height",
                                new XResources.DimensionReplacement(resparam.res.getDimensionPixelSize(
                                        resparam.res.getIdentifier("qs_tile_height", "dimen", PACKAGE_SYSTEMUI)),
                                        TypedValue.COMPLEX_UNIT_PX));
                    } catch (Throwable t) {
                        XposedHook.logE(TAG, "Couldn't change qs_dual_tile_height (" + t.getClass().getSimpleName() + ")", null);
                    }
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

                // For Motorola stock roms only
                try {
                    resparam.res.hookLayout(PACKAGE_SYSTEMUI, "layout", "zz_moto_status_bar_expanded_header", new XC_LayoutInflated() {
                        @Override
                        public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                            liparam.view.setElevation(0);
                            liparam.view.setPadding(0, 0, 0, 0);
                            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) liparam.view.getLayoutParams();
                            params.height = ResourceUtils.getInstance(liparam.view.getContext()).getDimensionPixelSize(R.dimen.status_bar_header_height);
                        }
                    });
                } catch (Throwable ignore) {
                    // Don't do anything here
                }

                resparam.res.hookLayout(PACKAGE_SYSTEMUI, "layout", "qs_panel", new XC_LayoutInflated() {
                    @Override
                    public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                        liparam.view.setElevation(0);
                        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) liparam.view.getLayoutParams();
                        params.setMarginStart(0);
                        params.setMarginEnd(0);

                        FrameLayout layout = (FrameLayout) liparam.view;
                        Context context = layout.getContext();
                        ResourceUtils res = ResourceUtils.getInstance(context);

                        mQsContainer = layout;

                        View qsPanel = layout.getChildAt(0);
                        FrameLayout.LayoutParams qsPanelLp = (FrameLayout.LayoutParams) qsPanel.getLayoutParams();
                        qsPanelLp.bottomMargin = res.getDimensionPixelSize(R.dimen.qs_panel_margin_bottom);
                        qsPanel.setLayoutParams(qsPanelLp);

                        FrameLayout.LayoutParams buttonLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                        buttonLp.gravity = Gravity.BOTTOM | Gravity.END;
                        Button editBtn = new Button(context);
                        editBtn.setGravity(Gravity.CENTER);
                        editBtn.setLayoutParams(buttonLp);
                        editBtn.setText(res.getString(R.string.qs_edit));
                        editBtn.setAllCaps(true);
                        editBtn.setId(R.id.qs_edit);
                        editBtn.setBackground(res.getDrawable(R.drawable.ripple_dismiss_all));
                        editBtn.setOnClickListener(onClickListener);
                        layout.addView(editBtn);

                        mEditButton = editBtn;
                    }
                });

            }
        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error hooking SystemUI resources", t);
        }
    }

}
