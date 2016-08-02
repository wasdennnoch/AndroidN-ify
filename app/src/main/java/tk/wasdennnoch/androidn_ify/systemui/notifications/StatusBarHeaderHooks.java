package tk.wasdennnoch.androidn_ify.systemui.notifications;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.XModuleResources;
import android.content.res.XResources;
import android.graphics.Outline;
import android.graphics.Rect;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

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
import tk.wasdennnoch.androidn_ify.systemui.qs.DetailViewManager;
import tk.wasdennnoch.androidn_ify.systemui.qs.QSTileHostHooks;
import tk.wasdennnoch.androidn_ify.systemui.qs.QuickQSPanel;
import tk.wasdennnoch.androidn_ify.systemui.qs.tiles.hooks.BluetoothTileHook;
import tk.wasdennnoch.androidn_ify.systemui.qs.tiles.hooks.CellularTileHook;
import tk.wasdennnoch.androidn_ify.systemui.qs.tiles.hooks.WifiTileHook;
import tk.wasdennnoch.androidn_ify.utils.ConfigUtils;
import tk.wasdennnoch.androidn_ify.utils.ResourceUtils;
import tk.wasdennnoch.androidn_ify.utils.RomUtils;

public class StatusBarHeaderHooks {

    private static final String TAG = "StatusBarHeaderHooks";

    private static final int WRAP_CONTENT = ViewGroup.LayoutParams.WRAP_CONTENT;
    private static final String PACKAGE_SYSTEMUI = XposedHook.PACKAGE_SYSTEMUI;

    private static final String CLASS_STATUS_BAR_HEADER_VIEW = "com.android.systemui.statusbar.phone.StatusBarHeaderView";
    private static final String CLASS_LAYOUT_VALUES = CLASS_STATUS_BAR_HEADER_VIEW + "$LayoutValues";
    private static final String CLASS_QS_PANEL = "com.android.systemui.qs.QSPanel";
    private static final String CLASS_QS_DRAG_PANEL = "com.android.systemui.qs.QSDragPanel";
    private static final String CLASS_QS_TILE = "com.android.systemui.qs.QSTile";
    private static final String CLASS_QS_STATE = CLASS_QS_TILE + "$State";
    private static final String CLASS_QS_TILE_VIEW = "com.android.systemui.qs.QSTileView";
    private static final String CLASS_DETAIL_ADAPTER = CLASS_QS_TILE + "$DetailAdapter";
    private static final String CLASS_CIRCLE_PAGE_INDICATOR = "com.viewpagerindicator.CirclePageIndicator";
    private static final String QS_PANEL_INDICATOR = "QSPanelIndicator";

    private static boolean mCollapseAfterHideDatails = false;
    private static boolean mHideTunerIcon = false;
    private static boolean mHideEditTiles = false;
    private static boolean mHideCarrierLabel = false;

    private static TouchAnimator mAlarmTranslation;
    private static TouchAnimator mDateSizeAnimator;
    private static TouchAnimator mFirstHalfAnimator;
    private static TouchAnimator mSecondHalfAnimator;
    private static TouchAnimator mSettingsAlpha;

    public static ViewGroup mStatusBarHeaderView;

    private static View mSystemIconsSuperContainer;
    private static View mDateGroup;
    private static FrameLayout mMultiUserSwitch;
    private static View mClock;
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
    private static View mCustomQSEditButton;
    private static View mCustomQSEditButton2;
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

    public static Button mEditButton;
    private static ResourceUtils mResUtils;

    private static int mBarState = 2;
    public static int mQsPage;

    private static boolean mHasEditPanel = false;
    public static boolean mEditing;
    public static boolean mShowingDetail;
    public static boolean mDisableFancy = false;
    public static boolean mUseDragPanel = false;

    private static float mExpansion = 0;
    private static int mGridHeight = 0;

    private static Class<?> onMeasureHookedClass;

    private static ArrayList<String> mPreviousTiles = new ArrayList<>();
    private static ArrayList<Object> mRecords;

    private static int mOnMeasureUnchagedCount;
    private static XC_MethodHook.Unhook onMeasureUnhook;

    private static final Rect mClipBounds = new Rect();

    private static XC_MethodHook onFinishInflateHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            XposedHook.logD(TAG, "onFinishInflateHook called");

            mStatusBarHeaderView = (ViewGroup) param.thisObject;
            mContext = mStatusBarHeaderView.getContext();
            ResourceUtils res = ResourceUtils.getInstance(mContext);
            ConfigUtils config = ConfigUtils.getInstance();

            try {
                if (!config.qs.keep_header_background) {
                    //noinspection deprecation
                    mStatusBarHeaderView.setBackgroundColor(mContext.getResources().getColor(mContext.getResources().getIdentifier("system_primary_color", "color", PACKAGE_SYSTEMUI)));
                }
            } catch (Throwable t) {
                XposedHook.logE(TAG, "Couldn't change header background color", t);
            }

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
                mSettingsContainer = mSettingsButton;
            }
            mTunerIcon = mSettingsContainer.findViewById(mContext.getResources().getIdentifier("tuner_icon", "id", PACKAGE_SYSTEMUI));
            mHideTunerIcon = config.qs.hide_tuner_icon;
            mHideEditTiles = config.qs.hide_edit_tiles;
            mHideCarrierLabel = config.qs.hide_carrier_label;
            // workaround for a bug where the clock would get a wrong position when opening the detail view
            View dummyClock = new View(mContext);
            dummyClock.setVisibility(View.GONE);
            XposedHelpers.setObjectField(param.thisObject, "mClock", dummyClock);
            try {
                mWeatherContainer = (View) XposedHelpers.getObjectField(param.thisObject, "mWeatherContainer");
            } catch (Throwable ignore) {
            }
            try {
                mCarrierText = (View) XposedHelpers.getObjectField(param.thisObject, "mCarrierText");
            } catch (Throwable ignore) {
            }
            try {
                mTaskManagerButton = (View) XposedHelpers.getObjectField(param.thisObject, "mTaskManagerButton");
            } catch (Throwable ignore) {
            }
            try { // Sony
                mCustomQSEditButton = (View) XposedHelpers.getObjectField(param.thisObject, "mSomcQuickSettings");
            } catch (Throwable i) {
                try { // PA
                    mCustomQSEditButton = (View) XposedHelpers.getObjectField(param.thisObject, "mQsAddButton");
                } catch (Throwable g) {
                    try { // OOS2 & 3
                        mCustomQSEditButton = (View) XposedHelpers.getObjectField(param.thisObject, "mEditModeButton");
                        mCustomQSEditButton2 = (View) XposedHelpers.getObjectField(param.thisObject, "mResetButton");
                        XposedHelpers.setObjectField(param.thisObject, "mEditModeButton", new ImageView(mContext));
                    } catch (Throwable ignore) {
                    }
                }
            }

            try {

                boolean mShowTaskManager = true;
                try {
                    mShowTaskManager = XposedHelpers.getBooleanField(param.thisObject, "mShowTaskManager");
                } catch (Throwable ignore) {
                }

                int rippleRes = mContext.getResources().getIdentifier("ripple_drawable", "drawable", XposedHook.PACKAGE_SYSTEMUI);
                int rightIconHeight = res.getDimensionPixelSize(R.dimen.right_icon_size);
                int rightIconWidth = mTaskManagerButton != null && mShowTaskManager ? res.getDimensionPixelSize(R.dimen.right_icon_width_small) : rightIconHeight;
                int expandIndicatorPadding = res.getDimensionPixelSize(R.dimen.expand_indicator_padding);
                int headerItemsMarginTop = res.getDimensionPixelSize(R.dimen.header_items_margin_top);
                int alarmStatusTextColor = mAlarmStatus.getCurrentTextColor();
                int dateTimeCollapsedSize = res.getDimensionPixelSize(R.dimen.date_time_collapsed_size);
                int dateTimeTextColor = mTime.getCurrentTextColor();
                int dateCollapsedDrawablePadding = res.getDimensionPixelSize(R.dimen.date_collapsed_drawable_padding);
                int dateTimeMarginLeft = res.getDimensionPixelSize(R.dimen.date_time_alarm_group_margin_left);
                Drawable alarmSmall = mContext.getDrawable(mContext.getResources().getIdentifier("ic_access_alarms_small", "drawable", XposedHook.PACKAGE_SYSTEMUI));

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
                mRightContainer = new LinearLayout(mContext);
                mRightContainer.setLayoutParams(rightContainerLp);
                mRightContainer.setGravity(Gravity.CENTER);
                mRightContainer.setOrientation(LinearLayout.HORIZONTAL);
                mRightContainer.setClipChildren(false);

                LinearLayout.LayoutParams multiUserSwitchLp = new LinearLayout.LayoutParams(rightIconWidth, rightIconHeight);
                mMultiUserSwitch.setLayoutParams(multiUserSwitchLp);

                LinearLayout.LayoutParams settingsContainerLp = new LinearLayout.LayoutParams(rightIconWidth, rightIconHeight);
                mSettingsContainer.setLayoutParams(settingsContainerLp);

                LinearLayout.LayoutParams expandIndicatorLp = new LinearLayout.LayoutParams(rightIconHeight, rightIconHeight); // Requires full width
                mExpandIndicator = new ExpandableIndicator(mContext);
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
                mLeftContainer = new LinearLayout(mContext);
                mLeftContainer.setLayoutParams(leftContainerLp);
                mLeftContainer.setOrientation(LinearLayout.VERTICAL);

                RelativeLayout.LayoutParams emergencyCallsOnlyLp = new RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
                emergencyCallsOnlyLp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                mEmergencyCallsOnly.setLayoutParams(emergencyCallsOnlyLp);
                mEmergencyCallsOnly.setTextSize(TypedValue.COMPLEX_UNIT_PX, res.getDimensionPixelSize(R.dimen.emergency_calls_only_text_size));
                mEmergencyCallsOnly.setTextColor(alarmStatusTextColor);
                mEmergencyCallsOnly.setPadding(0, 0, 0, 0);
                mEmergencyCallsOnly.setVisibility(View.GONE);


                RelativeLayout.LayoutParams dateTimeAlarmGroupLp = new RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
                dateTimeAlarmGroupLp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                dateTimeAlarmGroupLp.topMargin = res.getDimensionPixelSize(R.dimen.date_time_alarm_group_margin_top);
                dateTimeAlarmGroupLp.leftMargin = dateTimeMarginLeft;
                mDateTimeAlarmGroup = new LinearLayout(mContext);
                mDateTimeAlarmGroup.setLayoutParams(dateTimeAlarmGroupLp);
                mDateTimeAlarmGroup.setId(View.generateViewId());
                mDateTimeAlarmGroup.setGravity(Gravity.START);
                mDateTimeAlarmGroup.setOrientation(LinearLayout.VERTICAL);
                mDateTimeAlarmGroup.setBaselineAligned(false);

                LinearLayout.LayoutParams alarmStatusLp = new LinearLayout.LayoutParams(WRAP_CONTENT, res.getDimensionPixelSize(R.dimen.alarm_status_height));
                mAlarmStatus.setLayoutParams(alarmStatusLp);
                mAlarmStatus.setGravity(Gravity.TOP);
                mAlarmStatus.setTextSize(TypedValue.COMPLEX_UNIT_PX, dateTimeCollapsedSize);
                mAlarmStatus.setPadding(0, res.getDimensionPixelSize(R.dimen.alarm_status_padding_top), 0, 0);
                mAlarmStatus.setCompoundDrawablePadding(res.getDimensionPixelSize(R.dimen.alarm_status_drawable_padding));
                mAlarmStatus.setCompoundDrawablesWithIntrinsicBounds(alarmSmall, null, null, null);
                mAlarmStatus.setVisibility(View.GONE);
                mAlarmStatus.setBackgroundResource(rippleRes);


                LinearLayout.LayoutParams dateTimeGroupLp = new LinearLayout.LayoutParams(WRAP_CONTENT, res.getDimensionPixelSize(R.dimen.date_time_group_height));
                mDateTimeGroup = new LinearLayout(mContext);
                mDateTimeGroup.setLayoutParams(dateTimeGroupLp);
                mDateTimeGroup.setId(View.generateViewId());
                mDateTimeGroup.setOrientation(LinearLayout.HORIZONTAL);
                mDateTimeGroup.setPivotX(0.0F);
                mDateTimeGroup.setPivotY(0.0F);
                mDateTimeGroup.setBaselineAligned(false);

                LinearLayout.LayoutParams clockLp = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
                mClock.setLayoutParams(clockLp);
                mClock.findViewById(mContext.getResources().getIdentifier("empty_time_view", "id", XposedHook.PACKAGE_SYSTEMUI)).setVisibility(View.GONE);

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
                mAlarmStatusCollapsed = new AlphaOptimizedButton(mContext);
                mAlarmStatusCollapsed.setLayoutParams(alarmStatusCollapsedLp);
                mAlarmStatusCollapsed.setId(View.generateViewId());
                mAlarmStatusCollapsed.setGravity(Gravity.TOP);
                mAlarmStatusCollapsed.setTextColor(alarmStatusTextColor);
                mAlarmStatusCollapsed.setTextSize(TypedValue.COMPLEX_UNIT_PX, dateTimeCollapsedSize);
                mAlarmStatusCollapsed.setClickable(false);
                mAlarmStatusCollapsed.setFocusable(false);
                mAlarmStatusCollapsed.setVisibility(View.GONE);
                mAlarmStatusCollapsed.setCompoundDrawablesWithIntrinsicBounds(alarmSmall, null, null, null);
                mAlarmStatusCollapsed.setBackgroundResource(0);
                mAlarmStatusCollapsed.setPadding(res.getDimensionPixelSize(R.dimen.alarm_status_collapsed_drawable_padding), 0, 0, 0);


                RelativeLayout.LayoutParams headerQsPanelLp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, WRAP_CONTENT);
                mHeaderQsPanel = new QuickQSPanel(mContext);
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

                mStatusBarHeaderView.setOutlineProvider(new ViewOutlineProvider() {
                    @Override
                    public void getOutline(View view, Outline outline) {
                        outline.setRect(mClipBounds);
                    }
                });

            } catch (Throwable t) {
                // :(
                XposedHook.logE(TAG, "Error modifying header layout", t);
                return;
            }

            DetailViewManager.init(mContext, mStatusBarHeaderView, mQsPanel, mEditButton, mHasEditPanel);

            updateResources(mContext);

        }
    };

    private static XC_MethodHook onMeasureHook = new XC_MethodHook() {

        private int gridHeight;
        private int oldGridHeight = -1;

        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            if (mStatusBarHeaderView == null) return;
            try {
                gridHeight = (int) XposedHelpers.callMethod(StatusBarHeaderHooks.mQsPanel, "getGridHeight");
                if (gridHeight == oldGridHeight) {
                    XposedHook.logD(TAG, "onMeasureHook: Grid height unchanged");
                    if (mOnMeasureUnchagedCount > 5) {
                        XposedHook.logD(TAG, "onMeasureHook: Unhook and setup with grid height " + gridHeight);
                        onMeasureUnhook.unhook();
                        onMeasureUnhook = null;
                        mGridHeight = gridHeight;
                        mHeaderQsPanel.setupAnimators(gridHeight);
                    }
                    mOnMeasureUnchagedCount++;
                } else {
                    XposedHook.logD(TAG, "onMeasureHook: Grid height changed to " + gridHeight);
                    mOnMeasureUnchagedCount = 0;
                    oldGridHeight = gridHeight;
                }
            } catch (Throwable t) {
                XposedHook.logE(TAG, "onMeasureHook failed: " + t, null);
            }
        }
    };

    private static XC_MethodHook setExpansionHook = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            float f = (float) param.args[0];
            mExpansion = f;
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
                mExpandIndicator.setExpanded(f > 0.93F);
            } catch (Throwable ignore) {
                // Oh god, a massive spam wall coming right at you, quick, hide!
            }
        }

        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            if (mGridHeight == 0)
                return;
            View view = (View) param.thisObject;
            float height = view.getHeight();
            height += (int) (mGridHeight * mExpansion);
            mClipBounds.set(view.getPaddingLeft(), 0, view.getWidth() - view.getPaddingRight(), (int) height);
            view.setClipBounds(mClipBounds);
            view.invalidateOutline();
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
                if (mHideEditTiles && mCustomQSEditButton != null) {
                    mCustomQSEditButton.setVisibility(View.GONE);
                    if (mCustomQSEditButton2 != null) mCustomQSEditButton2.setVisibility(View.GONE);
                }
                if (mHideCarrierLabel && mCarrierText != null)
                    mCarrierText.setVisibility(View.GONE);
                if (mWeatherContainer != null) {
                    try {
                        mWeatherContainer.setVisibility(mExpanded && XposedHelpers.getBooleanField(mStatusBarHeaderView, "mShowWeather") ? View.VISIBLE : View.INVISIBLE);
                    } catch (Throwable ignored) {
                    }
                }
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
            }
        }
    };

    private static XC_MethodHook setTilesHook = new XC_MethodHook() {
        boolean cancelled = false;

        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            if (mUseDragPanel && !RomUtils.isAicp())
                return; // Causes problem with "Enlarge first row" setting
            if (mHeaderQsPanel != null) { // keep
                // Only set up views if the tiles actually changed
                if (param.args.length == 0) return; // PA already checks itself
                Collection tiles = (Collection) param.args[0];
                ArrayList<String> newTiles = new ArrayList<>();
                for (Object qstile : tiles) {
                    newTiles.add(qstile.getClass().getSimpleName());
                }
                cancelled = false;
                if (mPreviousTiles.equals(newTiles)) {
                    cancelled = true;
                    XposedHook.logD(TAG, "setTilesHook: Cancelling original method");
                    param.setResult(null);
                }
                mPreviousTiles.clear();
                mPreviousTiles.addAll(newTiles);
            }
        }

        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            if (mHeaderQsPanel != null && !cancelled) { // keep
                try {
                    //noinspection unchecked
                    mRecords = (ArrayList<Object>) XposedHelpers.getObjectField(param.thisObject, "mRecords");
                } catch (Throwable t) {
                    try { // OOS2
                        //noinspection unchecked
                        mRecords = (ArrayList<Object>) XposedHelpers.getObjectField(XposedHelpers.getObjectField(param.thisObject, "mGridView"), "mRecords");
                    } catch (Throwable t2) {
                        XposedHook.logE(TAG, "No tile record field found (" + t.getClass().getSimpleName() + " and " + t2.getClass().getSimpleName() + ")", null);
                        return;
                    }
                }
                if (mRecords.size() == 0) {
                    try { // OOS2 again because sometimes mRecords still seems to be in the StatusBarHeaderView (but empty)
                        //noinspection unchecked
                        mRecords = (ArrayList<Object>) XposedHelpers.getObjectField(XposedHelpers.getObjectField(param.thisObject, "mGridView"), "mRecords");
                    } catch (Throwable ignore) {
                    }
                }
                mHeaderQsPanel.setTiles(mRecords);
            }
        }
    };

    private static XC_MethodHook handleStateChangedHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            // This method gets called from two different processes,
            // so we have to check if we are in the right one
            if (mHeaderQsPanel != null) {
                mHeaderQsPanel.handleStateChanged(param.thisObject, XposedHelpers.getObjectField(param.thisObject, "mState"));
            }
        }
    };

    private static XC_MethodHook setupViewsHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            Object mPageIndicator = XposedHelpers.getObjectField(param.thisObject, "mPageIndicator");
            XposedHelpers.setAdditionalInstanceField(mPageIndicator, QS_PANEL_INDICATOR, true);
        }
    };

    private static XC_MethodHook onPageSelectedHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            if ((boolean) XposedHelpers.getAdditionalInstanceField(param.thisObject, QS_PANEL_INDICATOR)) {
                mQsPage = (int) param.args[0];
                mDisableFancy = mQsPage != 0;
            }
        }
    };

    private static void updateResources(Context context) {
        if (mDateTimeGroup == null) {
            return;
        }

        ResourceUtils res = ResourceUtils.getInstance(context);
        float timeCollapsed = res.getDimensionPixelSize(R.dimen.date_time_collapsed_size);
        float timeExpanded;
        switch (ConfigUtils.qs().header_clock_size) {
            case 1:
                timeExpanded = res.getDimensionPixelSize(R.dimen.date_time_expanded_size_small);
                break;
            case 2:
                timeExpanded = res.getDimensionPixelSize(R.dimen.date_time_expanded_size_smaller);
                break;
            case 3:
                timeExpanded = res.getDimensionPixelSize(R.dimen.date_time_expanded_size_tiny);
                break;
            default:
                timeExpanded = res.getDimensionPixelSize(R.dimen.date_time_expanded_size_normal);
                break;
        }
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
        if (mStatusBarHeaderView == null) return;
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
        setEditButtonVisible(!(showingDetail || mBarState != NotificationPanelHooks.STATE_SHADE));
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
            View mDetailDoneButton = (View) XposedHelpers.getObjectField(mQsPanel, "mDetailDoneButton");
            LinearLayout mDetailButtons = (LinearLayout) mDetailDoneButton.getParent();
            mDetailButtons.setVisibility(mEditing ? View.GONE : View.VISIBLE);
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
                DetailViewManager.getInstance().saveChanges();
                QSTileHostHooks.recreateTiles();
            }
        }
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
            try {
                switch (v.getId()) {
                    case R.id.qs_edit:
                        DetailViewManager.getInstance().showEditView(mRecords);
                        break;
                    case R.id.qs_up:
                        XposedHelpers.callMethod(mQsPanel, "announceForAccessibility",
                                mContext.getString(mContext.getResources().getIdentifier("accessibility_desc_quick_settings", "string", PACKAGE_SYSTEMUI)));
                        XposedHelpers.callMethod(mQsPanel, "closeDetail");
                        break;
                }
            } catch (Throwable t) {
                XposedHook.logE(TAG, "Error in onClickListener", t);
            }
        }
    };

    public static void onSetBarState(int state) {
        mBarState = state;
        setEditButtonVisible(state == NotificationPanelHooks.STATE_SHADE);
    }

    private static void setEditButtonVisible(boolean visible) {
        if (mEditButton == null || mQsPanel == null) return;
        FrameLayout.LayoutParams qsPanelLp = (FrameLayout.LayoutParams) mQsPanel.getLayoutParams();
        if (visible) {
            mEditButton.setVisibility(View.VISIBLE);
            qsPanelLp.bottomMargin = getResUtils().getDimensionPixelSize(R.dimen.qs_panel_margin_bottom);
        } else {
            mEditButton.setVisibility(View.GONE);
            qsPanelLp.bottomMargin = 0;
        }
        mQsPanel.setLayoutParams(qsPanelLp);
    }

    public static void hookQSOnMeasure() {
        if (onMeasureHook == null || onMeasureHookedClass == null || onMeasureUnhook != null)
            return;
        XposedHook.logD(TAG, "hookQSOnMeasure called");
        mOnMeasureUnchagedCount = 0;
        onMeasureUnhook = XposedHelpers.findAndHookMethod(onMeasureHookedClass, "onMeasure", int.class, int.class, onMeasureHook);
    }

    public static void hook(ClassLoader classLoader) {
        try {
            if (ConfigUtils.qs().header) {

                Class<?> classStatusBarHeaderView = XposedHelpers.findClass(CLASS_STATUS_BAR_HEADER_VIEW, classLoader);
                Class<?> classLayoutValues = XposedHelpers.findClass(CLASS_LAYOUT_VALUES, classLoader);
                Class<?> classQSPanel = XposedHelpers.findClass(CLASS_QS_PANEL, classLoader);
                Class<?> classQSTile = XposedHelpers.findClass(CLASS_QS_TILE, classLoader);
                Class<?> classQSTileView = XposedHelpers.findClass(CLASS_QS_TILE_VIEW, classLoader);

                try {
                    XposedHelpers.findAndHookMethod(classStatusBarHeaderView, "setEditing", boolean.class, setEditingHook);
                    mHasEditPanel = true;
                } catch (NoSuchMethodError ignore) {
                    mHasEditPanel = false;
                }

                XposedHelpers.findAndHookMethod(classStatusBarHeaderView, "onFinishInflate", onFinishInflateHook);
                XposedHelpers.findAndHookMethod(classStatusBarHeaderView, "setExpansion", float.class, setExpansionHook);
                XposedHelpers.findAndHookMethod(classStatusBarHeaderView, "onConfigurationChanged", Configuration.class, onConfigurationChangedHook);
                XposedHelpers.findAndHookMethod(classStatusBarHeaderView, "updateEverything", updateEverythingHook);
                XposedHelpers.findAndHookMethod(classStatusBarHeaderView, "updateVisibilities", updateVisibilitiesHook);

                try {
                    // Every time you make a typo, the errorists win.
                    XposedHelpers.findAndHookMethod(classLayoutValues, "interpoloate", classLayoutValues, classLayoutValues, float.class, XC_MethodReplacement.DO_NOTHING);
                } catch (Throwable ignore) { // srsly Bliss?
                    XposedHelpers.findAndHookMethod(classLayoutValues, "interpolate", classLayoutValues, classLayoutValues, float.class, XC_MethodReplacement.DO_NOTHING);
                }
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

                XposedHelpers.findAndHookMethod(classStatusBarHeaderView, "onClick", View.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (param.args[0] == mClock) {
                            try {
                                XposedHelpers.callMethod(param.thisObject, "startClockActivity");
                            } catch (Throwable ignore) {
                            }
                        }
                    }
                });

                try {
                    XposedHelpers.findAndHookMethod(classQSPanel, "fireShowingDetail", CLASS_DETAIL_ADAPTER, new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                            handleShowingDetail(param.args[0]);
                            return null;
                        }
                    });
                } catch (Throwable t) {
                    XposedHelpers.findAndHookMethod(classQSPanel, "showDetailAdapter", boolean.class, CLASS_DETAIL_ADAPTER, int[].class, new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                            handleShowingDetail(param.args[0]);
                            return null;
                        }
                    });
                }

                mUseDragPanel = false;
                try {
                    Class<?> classQSDragPanel = XposedHelpers.findClass(CLASS_QS_DRAG_PANEL, classLoader);
                    Class<?> classCirclePageIndicator = XposedHelpers.findClass(CLASS_CIRCLE_PAGE_INDICATOR, classLoader);
                    onMeasureHookedClass = classQSDragPanel;
                    hookQSOnMeasure();
                    XposedHelpers.findAndHookMethod(classQSDragPanel, "setTiles", Collection.class, setTilesHook);
                    XposedHelpers.findAndHookMethod(classQSDragPanel, "setupViews", setupViewsHook);
                    XposedHelpers.findAndHookMethod(classCirclePageIndicator, "onPageSelected", int.class, onPageSelectedHook);
                    mUseDragPanel = true;
                } catch (Throwable ignore) {
                    onMeasureHookedClass = classQSPanel;
                    hookQSOnMeasure();
                    try {
                        XposedHelpers.findAndHookMethod(classQSPanel, "setTiles", Collection.class, setTilesHook);
                    } catch (Throwable t) { // PA
                        XposedHelpers.findAndHookMethod(classQSPanel, "setTiles", setTilesHook);
                    }
                }

                QSTileHostHooks.hook(classLoader);

                boolean firstRowLarge = ConfigUtils.qs().large_first_row;
                if (ConfigUtils.qs().new_click_behavior) {
                    new WifiTileHook(classQSTile, classLoader, (!mUseDragPanel && !firstRowLarge));
                    new BluetoothTileHook(classQSTile, classLoader, (!mUseDragPanel && !firstRowLarge));
                    new CellularTileHook(classQSTile, classLoader);
                }

                XposedHelpers.findAndHookMethod(classQSTile, "handleStateChanged", handleStateChangedHook);

                try { // OOS3
                    XposedHelpers.findAndHookMethod(classQSTileView, "setOverlay", CLASS_QS_TILE + "$Mode", XC_MethodReplacement.DO_NOTHING);
                } catch (Throwable ignore) {
                }

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
                            if (forceAnim) {
                                View iconView = (View) XposedHelpers.getObjectField(param.thisObject, "mIcon");
                                if (iconView instanceof ImageView) {
                                    Drawable icon = ((ImageView) iconView).getDrawable();
                                    if (icon instanceof Animatable) {
                                        ((Animatable) icon).start();
                                        String type = (String) XposedHelpers.getAdditionalInstanceField(param.thisObject, "headerTileRowType");
                                        XposedHook.logD(TAG, "Animating QuickQS icon: " + forceAnim + (type != null ? ("; type: " + type) : ""));
                                    }
                                }
                            }
                        }
                    });
                }

                if (ConfigUtils.qs().enable_qs_editor) {
                    XposedHelpers.findAndHookMethod(PACKAGE_SYSTEMUI + ".settings.BrightnessController", classLoader, "updateIcon", boolean.class, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            View icon = (View) XposedHelpers.getObjectField(param.thisObject, "mIcon");
                            if (icon != null) icon.setVisibility(View.GONE);
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
            if (ConfigUtils.qs().header) {

                XModuleResources modRes = XModuleResources.createInstance(modulePath, resparam.res);

                XResources.DimensionReplacement zero = new XResources.DimensionReplacement(0, TypedValue.COMPLEX_UNIT_DIP);

                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "qs_peek_height", zero);
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "status_bar_header_height", modRes.fwd(R.dimen.status_bar_header_height));
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "status_bar_header_height_expanded", modRes.fwd(R.dimen.status_bar_header_height));
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "qs_emergency_calls_only_text_size", modRes.fwd(R.dimen.emergency_calls_only_text_size));
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "qs_date_collapsed_size", modRes.fwd(R.dimen.date_time_collapsed_size));
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "multi_user_avatar_collapsed_size", modRes.fwd(R.dimen.multi_user_avatar_size));
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "qs_brightness_padding_top", modRes.fwd(R.dimen.brightness_slider_padding_top));
                if (ConfigUtils.M)
                    resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "multi_user_avatar_expanded_size", modRes.fwd(R.dimen.multi_user_avatar_size));

                if (!ConfigUtils.qs().large_first_row) {
                    try {
                        resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "qs_dual_tile_height",
                                new XResources.DimensionReplacement(resparam.res.getDimensionPixelSize(
                                        resparam.res.getIdentifier("qs_tile_height", "dimen", PACKAGE_SYSTEMUI)),
                                        TypedValue.COMPLEX_UNIT_PX));
                        resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "qs_tile_divider_height", zero);
                    } catch (Throwable t) {
                        XposedHook.logE(TAG, "Couldn't change qs_dual_tile_height or qs_tile_divider_height (" + t.getClass().getSimpleName() + ")", null);
                    }
                }

                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "color", "qs_tile_divider", 0x00000000);

                resparam.res.hookLayout(PACKAGE_SYSTEMUI, "layout", "status_bar_expanded_header", new XC_LayoutInflated() {
                    @Override
                    public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                        liparam.view.setElevation(0);
                        liparam.view.setPadding(0, 0, 0, 0);
                        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) liparam.view.getLayoutParams();
                        params.height = ResourceUtils.getInstance(liparam.view.getContext()).getDimensionPixelSize(R.dimen.status_bar_header_height);
                    }
                });

                resparam.res.hookLayout(PACKAGE_SYSTEMUI, "layout", "qs_detail_header", new XC_LayoutInflated() {
                    @Override
                    public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                        LinearLayout layout = (LinearLayout) liparam.view;
                        Context context = layout.getContext();

                        ResourceUtils res = ResourceUtils.getInstance(context);
                        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                        View upButton = inflater.inflate(res.getLayout(R.layout.qs_up_button), null);
                        upButton.setOnClickListener(onClickListener);

                        int padding = context.getResources().getDimensionPixelSize(context.getResources().getIdentifier("qs_panel_padding", "dimen", PACKAGE_SYSTEMUI));

                        layout.addView(upButton, 0);
                        layout.setPadding(0, 0, padding, 0);
                        layout.setGravity(Gravity.CENTER);
                    }
                });

                // Motorola
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
                }

                resparam.res.hookLayout(PACKAGE_SYSTEMUI, "layout", "qs_panel", new XC_LayoutInflated() {
                    @Override
                    public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                        FrameLayout layout = (FrameLayout) liparam.view;
                        Context context = layout.getContext();
                        ResourceUtils res = ResourceUtils.getInstance(context);

                        mQsContainer = layout;

                        layout.setElevation(0);
                        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) layout.getLayoutParams();
                        params.setMarginStart(0);
                        params.setMarginEnd(0);

                        try {
                            mQsPanel = (ViewGroup) layout.getChildAt(0);
                        } catch (Throwable t1) {
                            try { // RR added an ImageView (background) first
                                mQsPanel = (ViewGroup) layout.getChildAt(1);
                                if (mQsPanel == null) throw new Throwable();
                            } catch (Throwable t2) {
                                mQsPanel = (ViewGroup) layout.findViewById(context.getResources().getIdentifier("quick_settings_panel", "id", PACKAGE_SYSTEMUI));
                            }
                        }

                        if (ConfigUtils.qs().enable_qs_editor) {
                            FrameLayout.LayoutParams qsPanelLp = (FrameLayout.LayoutParams) mQsPanel.getLayoutParams();
                            qsPanelLp.bottomMargin = res.getDimensionPixelSize(R.dimen.qs_panel_margin_bottom);
                            mQsPanel.setLayoutParams(qsPanelLp);

                            FrameLayout.LayoutParams buttonLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                            buttonLp.gravity = Gravity.BOTTOM | Gravity.END;
                            Button editBtn = new Button(context);
                            editBtn.setGravity(Gravity.CENTER);
                            editBtn.setLayoutParams(buttonLp);
                            editBtn.setText(res.getString(R.string.qs_edit));
                            editBtn.setTextColor(res.getColor(R.color.edit_btn_text));
                            editBtn.setAllCaps(true);
                            editBtn.setId(R.id.qs_edit);
                            editBtn.setBackground(res.getDrawable(R.drawable.ripple_dismiss_all));
                            editBtn.setOnClickListener(onClickListener);
                            layout.addView(editBtn);

                            mEditButton = editBtn;
                        }
                    }
                });

            }
        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error hooking SystemUI resources", t);
        }
    }

}
