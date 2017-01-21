package tk.wasdennnoch.androidn_ify.systemui.notifications;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.extracted.systemui.ExpandableIndicator;
import tk.wasdennnoch.androidn_ify.extracted.systemui.FakeShadowView;
import tk.wasdennnoch.androidn_ify.extracted.systemui.NotificationUtils;
import tk.wasdennnoch.androidn_ify.extracted.systemui.stack.PiecewiseLinearIndentationFunctor;
import tk.wasdennnoch.androidn_ify.extracted.systemui.stack.StackIndentationFunctor;
import tk.wasdennnoch.androidn_ify.misc.SafeOnClickListener;
import tk.wasdennnoch.androidn_ify.systemui.qs.QSContainerHelper;
import tk.wasdennnoch.androidn_ify.systemui.qs.customize.QSCustomizer;
import tk.wasdennnoch.androidn_ify.utils.ConfigUtils;
import tk.wasdennnoch.androidn_ify.utils.ResourceUtils;

@SuppressLint("StaticFieldLeak")
public class NotificationPanelHooks {

    private static final String TAG = "NotificationPanelHooks";

    private static final String PACKAGE_SYSTEMUI = XposedHook.PACKAGE_SYSTEMUI;
    private static final String CLASS_NOTIFICATION_STACK_SCROLL_LAYOUT = "com.android.systemui.statusbar.stack.NotificationStackScrollLayout";
    private static final String CLASS_NOTIFICATION_PANEL_VIEW = "com.android.systemui.statusbar.phone.NotificationPanelView";
    private static final String CLASS_QS_CONTAINER = "com.android.systemui.qs.QSContainer";
    private static final String CLASS_PANEL_VIEW = "com.android.systemui.statusbar.phone.PanelView";
    private static final String CLASS_OBSERVABLE_SCROLL_VIEW = "com.android.systemui.statusbar.phone.ObservableScrollView";
    private static final String CLASS_STACK_SCROLL_ALGORITHM = "com.android.systemui.statusbar.stack.StackScrollAlgorithm";

    private static Class<?> ExpandableNotificationRow;
    private static Class<?> ExpandableView;

    public static final int LOCATION_UNKNOWN = 0x00;
    public static final int LOCATION_FIRST_CARD = 0x01;
    public static final int LOCATION_TOP_STACK_HIDDEN = 0x02;
    public static final int LOCATION_TOP_STACK_PEEKING = 0x04;
    public static final int LOCATION_MAIN_AREA = 0x08;
    public static final int LOCATION_BOTTOM_STACK_PEEKING = 0x10;
    public static final int LOCATION_BOTTOM_STACK_HIDDEN = 0x20;
    /** The view isn't layouted at all. */
    public static final int LOCATION_GONE = 0x40;

    private static final int MAX_ITEMS_IN_BOTTOM_STACK = 3;

    public static final int STATE_SHADE = 0;
    public static final int STATE_KEYGUARD = 1;
    public static final int STATE_SHADE_LOCKED = 2;

    private static ViewGroup mNotificationPanelView;
    private static ViewGroup mScrollView;
    private static ViewGroup mQsContainer;
    private static ViewGroup mHeader;
    private static ViewGroup mQsPanel;
    private static ViewGroup mNotificationStackScroller;

    private static Object mStackScrollAlgorithm;
    private static StackIndentationFunctor mBottomStackIndentationFunctor;

    private static ExpandableIndicator mExpandIndicator;
    private static QSCustomizer mQsCustomizer;
    private static QSContainerHelper mQsContainerHelper;

    private static final List<BarStateCallback> mBarStateCallbacks = new ArrayList<>();

    private static final XC_MethodHook onFinishInflateHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            mNotificationPanelView = (ViewGroup) param.thisObject;
            Context context = mNotificationPanelView.getContext();

            mNotificationPanelView.setClipChildren(false);
            mNotificationPanelView.setClipToPadding(false);
            mHeader = (ViewGroup) XposedHelpers.getObjectField(mNotificationPanelView, "mHeader");
            mHeader.setOnClickListener(null);
            mExpandIndicator = (ExpandableIndicator) mHeader.findViewById(R.id.statusbar_header_expand_indicator);
            mExpandIndicator.setOnClickListener(mExpandIndicatorListener);

            mScrollView = (ViewGroup) XposedHelpers.getObjectField(mNotificationPanelView, "mScrollView");
            mQsContainer = (ViewGroup) XposedHelpers.getObjectField(mNotificationPanelView, "mQsContainer");
            mQsPanel = (ViewGroup) XposedHelpers.getObjectField(mNotificationPanelView, "mQsPanel");
            mNotificationStackScroller = (ViewGroup) XposedHelpers.getObjectField(mNotificationPanelView, "mNotificationStackScroller");

            if (!ConfigUtils.qs().keep_qs_panel_background) {
                View mQsContainer = (View) XposedHelpers.getObjectField(param.thisObject, "mQsContainer");
                try {
                    //noinspection deprecation
                    mQsContainer.setBackgroundColor(context.getResources().getColor(context.getResources().getIdentifier("system_primary_color", "color", PACKAGE_SYSTEMUI)));
                } catch (Throwable t) {
                    XposedHook.logE(TAG, "Couldn't change QS container background color", t);
                }
            }

            FrameLayout.LayoutParams qsCustomizerLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            qsCustomizerLp.gravity = Gravity.CENTER_HORIZONTAL;
            QSCustomizer qsCustomizer = new QSCustomizer(context);
            mNotificationPanelView.addView(qsCustomizer, qsCustomizerLp);

            mQsCustomizer = qsCustomizer;

            if (ConfigUtils.qs().fix_header_space) {
                mQsContainerHelper = new QSContainerHelper(mNotificationPanelView,mQsContainer,mHeader,
                        mQsPanel);

                mNotificationPanelView.requestLayout();
            }
        }
    };

    private static final XC_MethodReplacement onQsExpansionStarted = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            XposedHelpers.callMethod(mNotificationPanelView, "cancelQsAnimation");
            XposedHelpers.callMethod(mNotificationPanelView, "cancelHeightAnimator");
            float height = XposedHelpers.getFloatField(mNotificationPanelView, "mQsExpansionHeight") - (int)param.args[0];
            XposedHelpers.callMethod(mNotificationPanelView, "setQsExpansion",height);
            XposedHelpers.callMethod(mNotificationPanelView, "requestPanelHeightUpdate");
            return null;
        }
    };

    private static final XC_MethodReplacement setVerticalPanelTranslation = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            float translation = (float) param.args[0];
            XposedHelpers.callMethod(mNotificationStackScroller, "setTranslationX", translation);
            XposedHelpers.callMethod(mScrollView, "setTranslationX", translation);
            return null;
        }
    };

    private static final XC_MethodReplacement getTempQsMaxExpansion = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            return XposedHelpers.getIntField(mNotificationPanelView, "mQsMaxExpansionHeight");
        }
    };

    private static final XC_MethodHook onExpandingStartedHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            XposedHelpers.callMethod(mHeader,"setListening", true);
        }
    };

    private static final XC_MethodReplacement onClick = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            if(((View)param.args[0]).getId() == R.id.statusbar_header_expand_indicator) {
                XposedHelpers.callMethod(mNotificationPanelView, "onQsExpansionStarted");
                if (XposedHelpers.getBooleanField(mNotificationPanelView, "mQsExpanded")) {
                    XposedHelpers.callMethod(mNotificationPanelView, "flingSettings", 0, false, null, true);
                }
                else if (XposedHelpers.getBooleanField(mNotificationPanelView, "mQsExpansionEnabled")) {
                    XposedHelpers.callMethod(mNotificationPanelView, "flingSettings", 0, true, null, true);
                }
            }
            return null;
        }
    };

    private static final XC_MethodReplacement shouldQuickSettingsIntercept = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            float x = (float)param.args[0];
            float y = (float)param.args[1];
            float yDiff = (float)param.args[2];

            if (!XposedHelpers.getBooleanField(mNotificationPanelView,"mQsExpansionEnabled") || XposedHelpers.getBooleanField(mNotificationPanelView,"mCollapsedOnDown")) {
                return false;
            }
            View header = XposedHelpers.getBooleanField(mNotificationPanelView,"mKeyguardShowing") ? (View)XposedHelpers.getObjectField(mNotificationPanelView,"mKeyguardStatusBar") : (View)XposedHelpers.getObjectField(mNotificationPanelView, "mHeader");
            View mScrollView = (View)(XposedHelpers.getObjectField(mNotificationPanelView, "mScrollView"));
            boolean onHeader = x >= mScrollView.getX()
                    && x <= mScrollView.getX() + mScrollView.getWidth()
                    && y >= header.getTop() && y <= header.getBottom();
            if (XposedHelpers.getBooleanField(mNotificationPanelView, "mQsExpanded")) {
                return onHeader || (yDiff < 0 && (boolean)XposedHelpers.callMethod(mNotificationPanelView,"isInQsArea", x, y));
            } else {
                return onHeader;
            }
        }
    };

    private static final XC_MethodReplacement updateQsState = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            boolean mQsExpanded = XposedHelpers.getBooleanField(mNotificationPanelView, "mQsExpanded");
            boolean mStackScrollerOverscrolling = XposedHelpers.getBooleanField(mNotificationPanelView, "mStackScrollerOverscrolling");
            boolean mHeaderAnimating = XposedHelpers.getBooleanField(mNotificationPanelView, "mHeaderAnimating");
            boolean mKeyguardShowing = XposedHelpers.getBooleanField(mNotificationPanelView, "mKeyguardShowing");
            boolean mQsExpansionFromOverscroll = XposedHelpers.getBooleanField(mNotificationPanelView, "mQsExpansionFromOverscroll");
            boolean mQsScrimEnabled = XposedHelpers.getBooleanField(mNotificationPanelView, "mQsScrimEnabled");
            Object mKeyguardUserSwitcher = XposedHelpers.getObjectField(mNotificationPanelView, "mKeyguardUserSwitcher");
            int mStatusBarState = XposedHelpers.getIntField(mNotificationPanelView, "mStatusBarState");
            View mQsNavbarScrim = (View)XposedHelpers.getObjectField(mNotificationPanelView, "mQsNavbarScrim");
            boolean expandVisually = mQsExpanded || mStackScrollerOverscrolling || mHeaderAnimating;
            mHeader.setVisibility((mQsExpanded || !mKeyguardShowing || mHeaderAnimating)
                    ? View.VISIBLE
                    : View.INVISIBLE);
            XposedHelpers.callMethod(mHeader, "setExpanded", ((mKeyguardShowing && !mHeaderAnimating)
                    || (mQsExpanded && !mStackScrollerOverscrolling)));
            XposedHelpers.callMethod(mQsPanel, "setVisibility", (expandVisually ? View.VISIBLE : View.INVISIBLE));
            XposedHelpers.callMethod(mQsPanel, "setExpanded", mQsExpanded);
            XposedHelpers.callMethod(mNotificationStackScroller, "setScrollingEnabled", (
                    mStatusBarState != STATE_KEYGUARD && (!mQsExpanded
                            || mQsExpansionFromOverscroll)));
            XposedHelpers.callMethod(mNotificationPanelView, "updateEmptyShadeView");
            mQsNavbarScrim.setVisibility(mStatusBarState == STATE_SHADE && mQsExpanded
                    && !mStackScrollerOverscrolling && mQsScrimEnabled
                    ? View.VISIBLE
                    : View.INVISIBLE);
            if (mKeyguardUserSwitcher != null && mQsExpanded && !mStackScrollerOverscrolling) {
                XposedHelpers.callMethod(mKeyguardUserSwitcher, "hideIfNotSimple", true /* animate */);
            }
            return null;
        }
        };

    private static final XC_MethodHook onTouchEventHook = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            XposedHelpers.setBooleanField(mNotificationStackScroller, "mDelegateToScrollView", false);
            XposedHelpers.setBooleanField(mNotificationStackScroller, "mOnlyScrollingInThisMotion", false);
        }
    };

    private static final XC_MethodHook onInterceptTouchEventHook = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            XposedHelpers.setBooleanField(mNotificationStackScroller, "mInterceptDelegateEnabled", false);
            XposedHelpers.setBooleanField(mNotificationStackScroller, "mOnlyScrollingInThisMotion", false);
        }
    };

    private static final XC_MethodReplacement isScrolledToBottom = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            Object mStatusBar = XposedHelpers.getObjectField(mNotificationPanelView, "mStatusBar");
            int getBarState = (int)XposedHelpers.callMethod(mStatusBar, "getBarState");
            boolean isScrolledToBottom = (boolean)XposedHelpers.callMethod(mNotificationStackScroller, "isScrolledToBottom");
            boolean isInSettings = (boolean)XposedHelpers.callMethod(mNotificationPanelView, "isInSettings");
            if (!isInSettings) {
                return (getBarState == STATE_KEYGUARD)
                        || isScrolledToBottom;
            } else {
                return true;

            }
        }
    };

    private static final XC_MethodHook onScrollTouchHook = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            MotionEvent ev = (MotionEvent)param.args[0];
            boolean mIsBeingDragged = XposedHelpers.getBooleanField(mNotificationStackScroller, "mIsBeingDragged");
            if(ev.getY() < ((int)XposedHelpers.callMethod(mQsContainer, "getBottom")) && !mIsBeingDragged) {
                param.setResult(false);
            }
        }
    };

    private static final XC_MethodReplacement getHeaderTranslation = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            int mQsMinExpansionHeight = XposedHelpers.getIntField(mNotificationPanelView, "mQsMinExpansionHeight");
            float mExpandedHeight = XposedHelpers.getFloatField(mNotificationPanelView, "mExpandedHeight");
            if (getStatusBarState() == STATE_KEYGUARD) {
                return 0;
            }
            float translation = NotificationUtils.interpolate(-mQsMinExpansionHeight, 0, getAppearFraction(mExpandedHeight));
            return Math.min(0, translation);
        }
    };

    private static final XC_MethodReplacement getMaxPanelHeight = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            Object mStatusBar = XposedHelpers.getObjectField(mNotificationPanelView, "mStatusBar");
            int min = XposedHelpers.getIntField(mNotificationPanelView, "mStatusBarMinHeight");
            int mQsMinExpansionHeight = XposedHelpers.getIntField(mNotificationPanelView, "mQsMinExpansionHeight");
            int panelHeightQsExpanded = (int)XposedHelpers.callMethod(mNotificationPanelView, "calculatePanelHeightQsExpanded");
            boolean mQsExpandImmediate = XposedHelpers.getBooleanField(mNotificationPanelView, "mQsExpandImmediate");
            boolean mQsExpanded = XposedHelpers.getBooleanField(mNotificationPanelView, "mQsExpanded");
            boolean mIsExpanding = XposedHelpers.getBooleanField(mNotificationPanelView, "mIsExpanding");
            boolean mQsExpandedWhenExpandingStarted = XposedHelpers.getBooleanField(mNotificationPanelView, "mQsExpandedWhenExpandingStarted");
            if ((int)XposedHelpers.callMethod(mStatusBar, "getBarState") != STATE_KEYGUARD
                    && (int)XposedHelpers.callMethod(mNotificationStackScroller, "getNotGoneChildCount") == 0) {
                int minHeight = (int)((mQsMinExpansionHeight + (float)XposedHelpers.callMethod(mNotificationPanelView, "getOverExpansionAmount")));
                min = Math.max(min, minHeight);
            }
            int maxHeight;
            if (mQsExpandImmediate || mQsExpanded || mIsExpanding && mQsExpandedWhenExpandingStarted) {
                maxHeight = panelHeightQsExpanded;
            } else {
                maxHeight = (int)XposedHelpers.callMethod(mNotificationPanelView, "calculatePanelHeightShade");
            }
            maxHeight = Math.max(maxHeight, min);
            return maxHeight;
        }
    };

    private static final XC_MethodReplacement getPeekHeight = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            int mQsMinExpansionHeight = XposedHelpers.getIntField(mNotificationPanelView, "mQsMinExpansionHeight");
            if ((int)XposedHelpers.callMethod(mNotificationStackScroller, "getNotGoneChildCount") > 0) {
                return XposedHelpers.callMethod(mNotificationStackScroller, "getPeekHeight");
            } else {
                return mQsMinExpansionHeight;
            }
        }
    };

    private static final XC_MethodReplacement setStackHeight = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            float height = (float)param.args[0];
            XposedHelpers.setFloatField(mNotificationStackScroller, "mLastSetStackHeight", height);
            XposedHelpers.callMethod(mNotificationStackScroller, "setIsExpanded", height > 0.0f);
            int stackHeight;
            int mCurrentStackHeight = XposedHelpers.getIntField(mNotificationStackScroller, "mCurrentStackHeight");

            float translationY;
            float appearEndPosition = getAppearEndPosition();
            float appearStartPosition = getAppearStartPosition();
            if (height >= appearEndPosition) {
                translationY = XposedHelpers.getFloatField(mNotificationStackScroller, "mTopPaddingOverflow");
                stackHeight = (int) height;
            } else {
                float appearFraction = getAppearFraction(height);
                if (appearFraction >= 0) {
                    translationY = NotificationUtils.interpolate(getExpandTranslationStart(), 0,
                            appearFraction);
                } else {
                 // This may happen when pushing up a heads up. We linearly push it up from the
                 // start
                 translationY = height - appearStartPosition + getExpandTranslationStart();
                 }
                stackHeight = (int) (height - translationY);
            }
            if (stackHeight != mCurrentStackHeight) {
                XposedHelpers.setIntField(mNotificationStackScroller, "mCurrentStackHeight", stackHeight);
                XposedHelpers.callMethod(mNotificationStackScroller, "updateAlgorithmHeightAndPadding");
                XposedHelpers.callMethod(mNotificationStackScroller, "requestChildrenUpdate");
            }
            XposedHelpers.callMethod(mNotificationStackScroller, "setStackTranslation", translationY);
            return 0;
        }
    };

    private static final XC_MethodReplacement getPeekHeightStackScroller = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            final Object firstChild = XposedHelpers.callMethod(mNotificationStackScroller, "getFirstChildNotGone");
            final int firstChildMinHeight = firstChild != null ? (int)XposedHelpers.callMethod(firstChild, "getMinHeight")
                    : XposedHelpers.getIntField(mNotificationStackScroller, "mCollapsedSize");
            int mIntrinsicPadding = XposedHelpers.getIntField(mNotificationStackScroller, "mIntrinsicPadding");
            int mBottomStackSlowDownHeight = XposedHelpers.getIntField(mNotificationStackScroller, "mBottomStackSlowDownHeight");
            int mBottomStackPeekSize = XposedHelpers.getIntField(mNotificationStackScroller, "mBottomStackPeekSize");
            return mIntrinsicPadding + firstChildMinHeight + mBottomStackPeekSize
                    + mBottomStackSlowDownHeight;
        }
    };

    private static final XC_MethodHook onExpandingFinishedHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            XposedHelpers.setIntField(mNotificationPanelView, "mScrollYOverride", 0);
        }
    };

    private static final XC_MethodReplacement onScrollChanged = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            boolean mQsExpanded = XposedHelpers.getBooleanField(mNotificationPanelView, "mQsExpanded");
            boolean mQsFullyExpanded = XposedHelpers.getBooleanField(mNotificationPanelView, "mQsFullyExpanded");
            XposedHelpers.setIntField(mNotificationPanelView, "mQsMaxExpansionHeight", (int)XposedHelpers.callMethod(mQsContainer, "getDesiredHeight"));
            if (mQsExpanded && mQsFullyExpanded) {
                XposedHelpers.setIntField(mNotificationPanelView, "mQsExpansionHeight", XposedHelpers.getIntField(mNotificationPanelView, "mQsMaxExpansionHeight"));
                XposedHelpers.callMethod(mNotificationPanelView, "requestScrollerTopPaddingUpdate", false /* animate */);
                XposedHelpers.callMethod(mNotificationPanelView, "requestPanelHeightUpdate");
            }
            return null;
        }
    };

    private static final XC_MethodReplacement onHeightUpdated = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            float expandedHeight = (float)param.args[0];
            boolean mQsExpanded = XposedHelpers.getBooleanField(mNotificationPanelView, "mQsExpanded");
            boolean mQsExpandImmediate = XposedHelpers.getBooleanField(mNotificationPanelView, "mQsExpandImmediate");
            boolean mIsExpanding = XposedHelpers.getBooleanField(mNotificationPanelView, "mIsExpanding");
            boolean mQsExpandedWhenExpandingStarted = XposedHelpers.getBooleanField(mNotificationPanelView, "mQsExpandedWhenExpandingStarted");
            boolean mQsTracking = XposedHelpers.getBooleanField(mNotificationPanelView, "mQsTracking");
            boolean mQsExpansionFromOverscroll = XposedHelpers.getBooleanField(mNotificationPanelView, "mQsExpansionFromOverscroll");
            boolean isFullyCollapsed = (boolean)XposedHelpers.callMethod(mNotificationPanelView, "isFullyCollapsed");
            int mQsMinExpansionHeight = XposedHelpers.getIntField(mNotificationPanelView, "mQsMinExpansionHeight");
            Object mQsExpansionAnimator = XposedHelpers.getObjectField(mNotificationPanelView, "mQsExpansionAnimator");

            if (!mQsExpanded || mQsExpandImmediate || mIsExpanding && mQsExpandedWhenExpandingStarted) {
                XposedHelpers.callMethod(mNotificationPanelView, "positionClockAndNotifications");
            }
            if (mQsExpandImmediate || mQsExpanded && !mQsTracking && mQsExpansionAnimator == null
                    && !mQsExpansionFromOverscroll) {
                float t;
                if (isOnKeyguard()) {
                    // On Keyguard, interpolate the QS expansion linearly to the panel expansion
                    t = expandedHeight / (int)XposedHelpers.callMethod(mNotificationPanelView, "getMaxPanelHeight");
                } else {
                    // In Shade, interpolate linearly such that QS is closed whenever panel height is
                    // minimum QS expansion + minStackHeight
                    float panelHeightQsCollapsed = (int)XposedHelpers.callMethod(mNotificationStackScroller, "getIntrinsicPadding")
                            + getLayoutMinHeight();
                    float panelHeightQsExpanded = (int)XposedHelpers.callMethod(mNotificationPanelView, "calculatePanelHeightQsExpanded");
                    t = (expandedHeight - panelHeightQsCollapsed)
                            / (panelHeightQsExpanded - panelHeightQsCollapsed);
                }
                XposedHelpers.callMethod(mNotificationPanelView, "setQsExpansion", mQsMinExpansionHeight
                        + t * ((int)XposedHelpers.callMethod(mNotificationPanelView, "getTempQsMaxExpansion") - mQsMinExpansionHeight));
            }
            XposedHelpers.callMethod(mNotificationPanelView, "updateStackHeight", expandedHeight);
            XposedHelpers.callMethod(mNotificationPanelView, "updateHeader");
            XposedHelpers.callMethod(mNotificationPanelView, "updateUnlockIcon");
            XposedHelpers.callMethod(mNotificationPanelView, "updateNotificationTranslucency");
            XposedHelpers.callMethod(mNotificationPanelView, "updatePanelExpanded");
            XposedHelpers.callMethod(mNotificationStackScroller, "setShadeExpanded", !isFullyCollapsed);
            return null;
        }
    };

    private static final XC_MethodReplacement calculatePanelHeightQsExpanded = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            boolean mShadeEmpty = XposedHelpers.getBooleanField(mNotificationPanelView, "mShadeEmpty");
            int mQsMaxExpansionHeight = XposedHelpers.getIntField(mNotificationPanelView, "mQsMaxExpansionHeight");
            int mTopPaddingAdjustment = XposedHelpers.getIntField(mNotificationPanelView, "mTopPaddingAdjustment");
            Object mQsSizeChangeAnimator = XposedHelpers.getObjectField(mNotificationPanelView, "mQsSizeChangeAnimator");
            Object mClockPositionResult = XposedHelpers.getObjectField(mNotificationPanelView, "mClockPositionResult");
            float notificationHeight = (int)XposedHelpers.callMethod(mNotificationStackScroller, "getHeight")
                    - (int)XposedHelpers.callMethod(mNotificationStackScroller, "getEmptyBottomMargin")
                    - (int)XposedHelpers.callMethod(mNotificationStackScroller, "getTopPadding");

            // When only empty shade view is visible in QS collapsed state, simulate that we would have
            // it in expanded QS state as well so we don't run into troubles when fading the view in/out
            // and expanding/collapsing the whole panel from/to quick settings.
            if ((int)XposedHelpers.callMethod(mNotificationStackScroller, "getNotGoneChildCount") == 0
                    && mShadeEmpty) {
                notificationHeight = (int)XposedHelpers.callMethod(mNotificationStackScroller, "getEmptyShadeViewHeight")
                        + (int)XposedHelpers.callMethod(mNotificationStackScroller, "getBottomStackPeekSize")
                        + XposedHelpers.getIntField(mNotificationStackScroller, "mBottomStackSlowDownHeight");
            }
            int maxQsHeight = mQsMaxExpansionHeight;

            // If an animation is changing the size of the QS panel, take the animated value.
            if (mQsSizeChangeAnimator != null) {
                maxQsHeight = (int) XposedHelpers.callMethod(mQsSizeChangeAnimator, "getAnimatedValue");
            }
            float totalHeight = Math.max(
                    maxQsHeight, getStatusBarState() == STATE_KEYGUARD
                            ? XposedHelpers.getIntField(mClockPositionResult, "stackScrollerPadding") - mTopPaddingAdjustment
                            : 0)
                    + notificationHeight + (float)XposedHelpers.callMethod(mNotificationStackScroller, "getTopPaddingOverflow");

            if (totalHeight > mNotificationStackScroller.getHeight()) {
                float fullyCollapsedHeight = maxQsHeight
                        + getLayoutMinHeight();
                totalHeight = Math.max(fullyCollapsedHeight, (int)XposedHelpers.callMethod(mNotificationStackScroller, "getHeight"));
            }
            return (int)totalHeight;
        }
    };

    private static final XC_MethodReplacement getFadeoutAlpha = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            int mQsMinExpansionHeight = XposedHelpers.getIntField(mNotificationPanelView, "mQsMinExpansionHeight");
            float alpha = ((float)XposedHelpers.callMethod(mNotificationPanelView, "getNotificationsTopY") + getFirstItemMinHeight())
                    / (mQsMinExpansionHeight + (int)XposedHelpers.callMethod(mNotificationStackScroller, "getBottomStackPeekSize")
                    - XposedHelpers.getIntField(mNotificationStackScroller, "mBottomStackSlowDownHeight"));
            alpha = Math.max(0, Math.min(alpha, 1));
            alpha = (float) Math.pow(alpha, 0.75);
            return alpha;
        }
    };

    private static XC_MethodReplacement updateTopPadding = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            float qsHeight = (float)param.args[0];
            float height = (int)XposedHelpers.callMethod(mNotificationStackScroller, "getHeight");
            boolean animate = (boolean)param.args[2];
            boolean ignoreIntrinsicPadding = (boolean)param.args[3];

            int topPadding = (int) qsHeight;
            int minStackHeight = getLayoutMinHeight();
            if (topPadding + minStackHeight > height) {
                XposedHelpers.setFloatField(mNotificationStackScroller, "mTopPaddingOverflow", topPadding + minStackHeight - height);
            } else {
                XposedHelpers.setIntField(mNotificationStackScroller, "mTopPaddingOverflow", 0);
            }
            XposedHelpers.callMethod(mNotificationStackScroller, "setTopPadding", ignoreIntrinsicPadding ? topPadding : XposedHelpers.callMethod(mNotificationStackScroller, "clampPadding", topPadding),
                    animate);
            XposedHelpers.callMethod(mNotificationStackScroller, "setStackHeight", XposedHelpers.getFloatField(mNotificationStackScroller, "mLastSetStackHeight"));
            return null;
        }
    };

    private static final XC_MethodReplacement getMinStackHeight = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            int mBottomStackPeekSize = XposedHelpers.getIntField(mNotificationStackScroller, "mBottomStackPeekSize");
            int mBottomStackSlowDownHeight = XposedHelpers.getIntField(mNotificationStackScroller, "mBottomStackSlowDownHeight");
            int mMaxLayoutHeight = XposedHelpers.getIntField(mNotificationStackScroller, "mMaxLayoutHeight");
            int mIntrinsicPadding = XposedHelpers.getIntField(mNotificationStackScroller, "mIntrinsicPadding");
            int firstChildMinHeight = getFirstChildIntrinsicHeight();
            return Math.min(firstChildMinHeight + mBottomStackPeekSize + mBottomStackSlowDownHeight,
                    mMaxLayoutHeight - mIntrinsicPadding);
        }
    };

    private static final XC_MethodReplacement getEmptyBottomMargin = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            int mMaxLayoutHeight = XposedHelpers.getIntField(mNotificationStackScroller, "mMaxLayoutHeight");
            int mContentHeight = XposedHelpers.getIntField(mNotificationStackScroller, "mContentHeight");
            int mBottomStackPeekSize = XposedHelpers.getIntField(mNotificationStackScroller, "mBottomStackPeekSize");
            int mBottomStackSlowDownHeight = XposedHelpers.getIntField(mNotificationStackScroller, "mBottomStackSlowDownHeight");
            int emptyMargin = mMaxLayoutHeight - mContentHeight - mBottomStackPeekSize
                    - mBottomStackSlowDownHeight;
            return Math.max(emptyMargin, 0);
        }
    };

    private static final XC_MethodReplacement getDismissViewHeight = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            Object mDismissView = XposedHelpers.getObjectField(mNotificationStackScroller, "mDismissView");
            int mPaddingBetweenElements = XposedHelpers.getIntField(mNotificationStackScroller, "mPaddingBetweenElements");
            return (int)XposedHelpers.callMethod(mDismissView, "getHeight") + mPaddingBetweenElements;
        }
    };

    private static final XC_MethodReplacement positionClockAndNotifications = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {

            Object mClockPositionAlgorithm = XposedHelpers.getObjectField(mNotificationPanelView, "mClockPositionAlgorithm");
            Object mStatusBar = XposedHelpers.getObjectField(mNotificationPanelView, "mStatusBar");
            Object mKeyguardStatusView = XposedHelpers.getObjectField(mNotificationPanelView, "mKeyguardStatusView");
            Object mClockPositionResult = XposedHelpers.getObjectField(mNotificationPanelView, "mClockPositionResult");
            Object mClockAnimator = XposedHelpers.getObjectField(mNotificationPanelView, "mClockAnimator");
            boolean animate = (boolean)XposedHelpers.callMethod(mNotificationStackScroller, "isAddOrRemoveAnimationPending");
            int stackScrollerPadding;
            if (getStatusBarState() != STATE_KEYGUARD) {
                stackScrollerPadding = (int)XposedHelpers.callMethod(mHeader, "getCollapsedHeight") + XposedHelpers.getIntField(mNotificationPanelView, "mQsPeekHeight");
                XposedHelpers.setIntField(mNotificationPanelView, "mTopPaddingAdjustment", 0);
            } else {
                XposedHelpers.callMethod(mClockPositionAlgorithm, "setup",
                        XposedHelpers.callMethod(mStatusBar, "getMaxKeyguardNotifications"),
                        XposedHelpers.callMethod(mNotificationPanelView, "getMaxPanelHeight"),
                        XposedHelpers.callMethod(mNotificationPanelView, "getExpandedHeight"),
                        XposedHelpers.callMethod(mNotificationStackScroller, "getNotGoneChildCount"),
                        XposedHelpers.callMethod(mNotificationPanelView, "getHeight"),
                        XposedHelpers.callMethod(mKeyguardStatusView, "getHeight"),
                        XposedHelpers.getFloatField(mNotificationPanelView, "mEmptyDragAmount"));
                XposedHelpers.callMethod(mClockPositionAlgorithm, "run", mClockPositionResult);
                if (animate || mClockAnimator != null) {
                    XposedHelpers.callMethod(mNotificationPanelView, "startClockAnimation", XposedHelpers.getIntField(mClockPositionResult, "clockY"));
                } else {
                    XposedHelpers.callMethod(mKeyguardStatusView, "setY", XposedHelpers.getIntField(mClockPositionResult, "clockY"));
                }
                XposedHelpers.callMethod(mNotificationPanelView, "updateClock", XposedHelpers.getFloatField(mClockPositionResult, "clockAlpha"), XposedHelpers.getFloatField(mClockPositionResult, "clockScale"));
                stackScrollerPadding = XposedHelpers.getIntField(mClockPositionResult, "stackScrollerPadding");
                XposedHelpers.setIntField(mNotificationPanelView, "mTopPaddingAdjustment", XposedHelpers.getIntField(mClockPositionResult, "stackScrollerPaddingAdjustment"));
            }
            XposedHelpers.callMethod(mNotificationStackScroller, "setIntrinsicPadding", stackScrollerPadding);
            XposedHelpers.callMethod(mNotificationPanelView, "requestScrollerTopPaddingUpdate", animate);
            return null;
        }
    };

    private static final XC_MethodReplacement setQsExpansion = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            int mQsMinExpansionHeight = XposedHelpers.getIntField(mNotificationPanelView, "mQsMinExpansionHeight");
            int mQsMaxExpansionHeight = XposedHelpers.getIntField(mNotificationPanelView, "mQsMaxExpansionHeight");
            Object mQsNavbarScrim = XposedHelpers.getObjectField(mNotificationPanelView, "mQsNavbarScrim");
            boolean mQsExpanded = XposedHelpers.getBooleanField(mNotificationPanelView, "mQsExpanded");
            boolean mStackScrollerOverscrolling = XposedHelpers.getBooleanField(mNotificationPanelView, "mStackScrollerOverscrolling");
            boolean mLastAnnouncementWasQuickSettings = XposedHelpers.getBooleanField(mNotificationPanelView, "mLastAnnouncementWasQuickSettings");
            boolean mTracking = XposedHelpers.getBooleanField(mNotificationPanelView, "mTracking");
            boolean isCollapsing = (boolean)XposedHelpers.callMethod(mNotificationPanelView, "isCollapsing");
            boolean mQsScrimEnabled = XposedHelpers.getBooleanField(mNotificationPanelView, "mQsScrimEnabled");
            float height = (float)param.args[0];
            height = Math.min(Math.max(height, mQsMinExpansionHeight), mQsMaxExpansionHeight);
            XposedHelpers.setBooleanField(mNotificationPanelView, "mQsFullyExpanded", height == mQsMaxExpansionHeight && mQsMaxExpansionHeight != 0);
            if (height > mQsMinExpansionHeight && !mQsExpanded && !mStackScrollerOverscrolling) {
                XposedHelpers.callMethod(mNotificationPanelView, "setQsExpanded", true);
            } else if (height <= mQsMinExpansionHeight && mQsExpanded) {
                XposedHelpers.callMethod(mNotificationPanelView, "setQsExpanded", false);
                if (mLastAnnouncementWasQuickSettings && !mTracking && !isCollapsing) {
                    XposedHelpers.callMethod(mNotificationPanelView, "announceForAccessibility", (boolean)XposedHelpers.callMethod(mNotificationPanelView, "getKeyguardOrLockScreenString"));
                    XposedHelpers.setBooleanField(mNotificationPanelView, "mLastAnnouncementWasQuickSettings", false);
                }
            }
            XposedHelpers.setFloatField(mNotificationPanelView, "mQsExpansionHeight", height);
            updateQsExpansion();

            XposedHelpers.callMethod(mNotificationPanelView, "requestScrollerTopPaddingUpdate", false /* animate */);

            if (isOnKeyguard()) {
                XposedHelpers.callMethod(mNotificationPanelView, "updateHeaderKeyguardAlpha");

            }
            if (getStatusBarState() == STATE_SHADE_LOCKED
                    || getStatusBarState() == STATE_KEYGUARD) {
                XposedHelpers.callMethod(mNotificationPanelView, "updateKeyguardBottomAreaAlpha");
            }
            if (getStatusBarState() == STATE_SHADE && mQsExpanded
                    && !mStackScrollerOverscrolling && mQsScrimEnabled) {
                XposedHelpers.callMethod(mQsNavbarScrim, "setAlpha", XposedHelpers.callMethod(mNotificationPanelView, "getQsExpansionFraction"));
            }
            return null;
        }
    };

    private static final XC_MethodReplacement onMeasure = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            // Since we control our own bottom, be whatever size we want.
            // Otherwise the QSPanel ends up with 0 height when the window is only the
            // size of the status bar.
            Object mQsContainer = param.thisObject;
            int widthMeasureSpec = (int)param.args[0];
            int heightMeasureSpec = (int)param.args[1];
            Object mQSPanel = XposedHelpers.getObjectField(mQsContainer, "mQSPanel");
            XposedHelpers.callMethod(mQSPanel, "measure", widthMeasureSpec, View.MeasureSpec.makeMeasureSpec(
                    View.MeasureSpec.getSize(heightMeasureSpec), View.MeasureSpec.UNSPECIFIED));
            int width = (int)XposedHelpers.callMethod(mQSPanel, "getMeasuredWidth");
            int height = XposedHelpers.getIntField((FrameLayout.LayoutParams) XposedHelpers.callMethod(mQSPanel, "getLayoutParams"), "topMargin")
                    + (int)XposedHelpers.callMethod(mQSPanel, "getMeasuredHeight");
            XposedHelpers.callMethod(mQsContainer.getClass().getSuperclass(), "onMeasure", View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
            return null;
        }
    };

    private static int getFirstItemMinHeight() {
        final Object firstChild = XposedHelpers.callMethod(mNotificationStackScroller, "getFirstChildNotGone");
        int mCollapsedSize = XposedHelpers.getIntField(mNotificationStackScroller, "mCollapsedSize");
        return firstChild != null ? (int)XposedHelpers.callMethod(firstChild, "getMinHeight") : mCollapsedSize;
    }

    private static float getExpandTranslationStart() {
        Object mHeadsUpManager = XposedHelpers.getObjectField(mNotificationStackScroller, "mHeadsUpManager");
        int mMaxLayoutHeight = XposedHelpers.getIntField(mNotificationStackScroller, "mMaxLayoutHeight");
        int mIntrinsicPadding = XposedHelpers.getIntField(mNotificationStackScroller, "mIntrinsicPadding");
        int mBottomStackSlowDownHeight = XposedHelpers.getIntField(mNotificationStackScroller, "mBottomStackSlowDownHeight");
        int mBottomStackPeekSize = XposedHelpers.getIntField(mNotificationStackScroller, "mBottomStackPeekSize");
        int mTopPadding = XposedHelpers.getIntField(mNotificationStackScroller, "mTopPadding");
        int startPosition = 0;
        if (!XposedHelpers.getBooleanField(mNotificationStackScroller, "mTrackingHeadsUp") && !(boolean)XposedHelpers.callMethod(mHeadsUpManager, "hasPinnedHeadsUp")) {
            startPosition = - Math.min(getFirstChildIntrinsicHeight(),
                    mMaxLayoutHeight - mIntrinsicPadding - mBottomStackSlowDownHeight
                            - mBottomStackPeekSize);
        }
        return startPosition - mTopPadding;
    }

    /**
     * @return the position from where the appear transition starts when expanding.
     *         Measured in absolute height.
     */
    private static float getAppearStartPosition() {
        Object mHeadsUpManager = XposedHelpers.getObjectField(mNotificationStackScroller, "mHeadsUpManager");
        return XposedHelpers.getBooleanField(mNotificationStackScroller, "mTrackingHeadsUp")
                ? (int)XposedHelpers.callMethod(mHeadsUpManager, "getTopHeadsUpHeight")
                : 0;
    }

    /**
     * @return the position from where the appear transition ends when expanding.
     *         Measured in absolute height.
     */
    private static float getAppearEndPosition() {
        Object mHeadsUpManager = XposedHelpers.getObjectField(mNotificationStackScroller, "mHeadsUpManager");
        boolean mTrackingHeadsUp = XposedHelpers.getBooleanField(mNotificationStackScroller, "mTrackingHeadsUp");
        int mBottomStackPeekSize = XposedHelpers.getIntField(mNotificationStackScroller, "mBottomStackPeekSize");
        int mBottomStackSlowDownHeight = XposedHelpers.getIntField(mNotificationStackScroller, "mBottomStackSlowDownHeight");
        int mTopPadding = XposedHelpers.getIntField(mNotificationStackScroller, "mTopPadding");
        int mIntrinsicPadding = XposedHelpers.getIntField(mNotificationStackScroller, "mIntrinsicPadding");
        int firstItemHeight = mTrackingHeadsUp || (boolean)XposedHelpers.callMethod(mHeadsUpManager, "hasPinnedHeadsUp")
                ? (int)XposedHelpers.callMethod(mHeadsUpManager, "getTopHeadsUpHeight") + mBottomStackPeekSize
                + mBottomStackSlowDownHeight
                : getLayoutMinHeight();
        return firstItemHeight + (isOnKeyguard() ? mTopPadding : mIntrinsicPadding);
    }

    public static int getLayoutMinHeight() {
        int firstChildMinHeight = getFirstChildIntrinsicHeight();
        int mBottomStackPeekSize = XposedHelpers.getIntField(mNotificationStackScroller, "mBottomStackPeekSize");
        int mBottomStackSlowDownHeight = XposedHelpers.getIntField(mNotificationStackScroller, "mBottomStackSlowDownHeight");
        int mTopPadding = XposedHelpers.getIntField(mNotificationStackScroller, "mTopPadding");
        int mMaxLayoutHeight = XposedHelpers.getIntField(mNotificationStackScroller, "mMaxLayoutHeight");
        return Math.min(firstChildMinHeight + mBottomStackPeekSize + mBottomStackSlowDownHeight,
                mMaxLayoutHeight - mTopPadding);
    }

    private static int getFirstChildIntrinsicHeight() {
        final Object firstChild = XposedHelpers.callMethod(mNotificationStackScroller, "getFirstChildNotGone");
        final Object mEmptyShadeView = XposedHelpers.getObjectField(mNotificationStackScroller, "mEmptyShadeView");
        int mCollapsedSize = XposedHelpers.getIntField(mNotificationStackScroller, "mCollapsedSize");
        int mOwnScrollY = XposedHelpers.getIntField(mNotificationStackScroller, "mOwnScrollY");
        int firstChildMinHeight = firstChild != null
                ? (int)XposedHelpers.callMethod(firstChild, "getIntrinsicHeight")
                : mEmptyShadeView != null
                ? (int)XposedHelpers.callMethod(mEmptyShadeView, "getMinHeight")
                : mCollapsedSize;
        if (mOwnScrollY > 0) {
            firstChildMinHeight = Math.max(firstChildMinHeight - mOwnScrollY, mCollapsedSize);
        }
        return firstChildMinHeight;
    }
    /**
     * @param height the height of the panel
     * @return the fraction of the appear animation that has been performed
     */
    public static float getAppearFraction(float height) {
        float appearEndPosition = getAppearEndPosition();
        float appearStartPosition = getAppearStartPosition();
        return (height - appearStartPosition)
                / (appearEndPosition - appearStartPosition);
    }

    private static final XC_MethodHook setBarStateHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            XposedHook.logD(TAG, "setBarStateHook: Setting state to " + (int) param.args[0]);
            //StatusBarHeaderHooks.onSetBarState((int) param.args[0]);
            for (BarStateCallback callback : mBarStateCallbacks) {
                callback.onStateChanged();
            }
        }
    };

    private static XC_MethodHook setVerticalPanelTranslationHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            if (mQsCustomizer != null)
                mQsCustomizer.setTranslationX((float) param.args[0]);
        }
    };

    private static XC_MethodHook initViewHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            Object mNotificationStackScroller = param.thisObject;
            mStackScrollAlgorithm = XposedHelpers.getObjectField(mNotificationStackScroller, "mStackScrollAlgorithm");
        }
    };

    private static void updatePositionsForState(Object resultState, Object algorithmState, Object ambientState) {

        List<?> visibleChildren = (List<?>) XposedHelpers.getObjectField(algorithmState, "visibleChildren");

        if (mStackScrollAlgorithm == null) return;

        int mBottomStackPeekSize = XposedHelpers.getIntField(mStackScrollAlgorithm, "mBottomStackPeekSize");
        int mBottomStackSlowDownLength = XposedHelpers.getIntField(mStackScrollAlgorithm, "mBottomStackSlowDownLength");

        // The starting position of the bottom stack peek
        float bottomPeekStart = (int) XposedHelpers.callMethod(ambientState, "getInnerHeight") - mBottomStackPeekSize;

        // The position where the bottom stack starts.
        float bottomStackStart = bottomPeekStart - mBottomStackSlowDownLength;

        // The y coordinate of the current child.
        float currentYPosition = -(XposedHelpers.getFloatField(algorithmState, "scrollY"));

        int childCount = visibleChildren.size();
        int paddingAfterChild;
        for (int i = 0; i < childCount; i++) {
            Object child = visibleChildren.get(i);
            Object childViewState = XposedHelpers.callMethod(resultState, "getViewStateForView", child);
            XposedHelpers.setIntField(childViewState, "location", LOCATION_UNKNOWN);
            paddingAfterChild = XposedHelpers.getIntField(mStackScrollAlgorithm, "mPaddingBetweenElements");
            int childHeight = getMaxAllowedChildHeight((View) child);
            int collapsedHeight = (int) XposedHelpers.callMethod(child, "getMinHeight");
            XposedHelpers.setFloatField(childViewState, "yTranslation", currentYPosition);
            if (i == 0) {
                updateFirstChildHeight(child, childViewState, childHeight, ambientState);
            }
            // The y position after this element
            float nextYPosition = currentYPosition + childHeight +
                    paddingAfterChild;
            if (nextYPosition >= bottomStackStart) {
                // Case 1:
                // We are in the bottom stack.
                if (currentYPosition >= bottomStackStart) {
                    // According to the regular scroll view we are fully translated out of the
                    // bottom of the screen so we are fully in the bottom stack
                    updateStateForChildFullyInBottomStack(algorithmState,
                            bottomStackStart, childViewState, collapsedHeight, ambientState);
                } else {
                    // According to the regular scroll view we are currently translating out of /
                    // into the bottom of the screen
                    updateStateForChildTransitioningInBottom(algorithmState,
                            bottomStackStart, child, currentYPosition,
                            childViewState, childHeight);
                }
            } else {
                // Case 2:
                // We are in the regular scroll area.
                XposedHelpers.setIntField(childViewState, "location", LOCATION_MAIN_AREA);
                clampPositionToBottomStackStart(childViewState, (XposedHelpers.getIntField(childViewState, "height")), childHeight,
                        ambientState);
            }
            if (i == 0 && (int) XposedHelpers.callMethod(ambientState, "getScrollY") <= 0) {
                // The first card can get into the bottom stack if it's the only one
                // on the lockscreen which pushes it up. Let's make sure that doesn't happen and
                // it stays at the top
                XposedHelpers.setFloatField(childViewState, "yTranslation", Math.max(0, XposedHelpers.getFloatField(childViewState, "yTranslation")));
            }
            currentYPosition = XposedHelpers.getFloatField(childViewState, "yTranslation") + childHeight + paddingAfterChild;
            if (currentYPosition <= 0) {
                XposedHelpers.setIntField(childViewState, "location", LOCATION_TOP_STACK_HIDDEN);
            }
            if (XposedHelpers.getIntField(childViewState, "location") == LOCATION_UNKNOWN) {
                XposedHook.logW(TAG, "Failed to assign location for child " + i);
            }
            float yTranslation = XposedHelpers.getFloatField(childViewState, "yTranslation");
            XposedHelpers.setFloatField(childViewState, "yTranslation", yTranslation + (float) XposedHelpers.callMethod(ambientState, "getTopPadding")
                    + (float) XposedHelpers.callMethod(ambientState, "getStackTranslation"));
        }
    }

    private static final XC_MethodReplacement updateHeader = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            if (getStatusBarState() == STATE_KEYGUARD)
                XposedHelpers.callMethod(mNotificationPanelView, "updateHeaderKeyguardAlpha");
            updateQsExpansion();
            return null;
        }
    };

    private static final XC_MethodHook updateChildrenHook = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            updateScrollStateForAddedChildren();
        }
    };

    private static final XC_MethodHook initConstantsHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            int mBottomStackPeekSize = XposedHelpers.getIntField(param.thisObject, "mBottomStackPeekSize");
            XposedHelpers.setIntField(param.thisObject, "mZDistanceBetweenElements", Math.max(1, ResourceUtils.getInstance().getResources()
                    .getDimensionPixelSize(R.dimen.z_distance_between_notifications)));
            mBottomStackIndentationFunctor = new PiecewiseLinearIndentationFunctor(
                    MAX_ITEMS_IN_BOTTOM_STACK,
                    mBottomStackPeekSize,
                    (int)XposedHelpers.callMethod(param.thisObject, "getBottomStackSlowDownLength"),
                    0.5f);
        }
    };

    public  static final XC_MethodReplacement getStackScrollState = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            // The state of the local variables are saved in an algorithmState to easily subdivide it
            // into multiple phases.
            Object ambientState = param.args[0];
            Object resultState = param.args[1];
            Object algorithmState = XposedHelpers.getObjectField(mStackScrollAlgorithm, "mTempAlgorithmState");

            // First we reset the view states to their default values.
            XposedHelpers.callMethod(resultState, "resetViewStates");

            initAlgorithmState(resultState, algorithmState, ambientState);
            updatePositionsForState(resultState, algorithmState, ambientState);
            updateZValuesForState(resultState, algorithmState, ambientState);
            updateHeadsUpStates(resultState, algorithmState, ambientState);
            XposedHelpers.callMethod(mStackScrollAlgorithm, "handleDraggedViews", ambientState, resultState, algorithmState);
            XposedHelpers.callMethod(mStackScrollAlgorithm, "updateDimmedActivatedHideSensitive", ambientState, resultState, algorithmState);
            updateClipping(resultState, algorithmState, ambientState);
            XposedHelpers.callMethod(mStackScrollAlgorithm, "updateSpeedBumpState", resultState, algorithmState, (int)XposedHelpers.callMethod(ambientState, "getSpeedBumpIndex"));
            XposedHelpers.callMethod(mStackScrollAlgorithm, "getNotificationChildrenStates", resultState, algorithmState);
            return null;
        }
    };

    private static void updateHeadsUpStates(Object resultState,
                                            Object algorithmState, Object ambientState) {
        ArrayList<ViewGroup> visibleChildren = (ArrayList<ViewGroup>) XposedHelpers.getObjectField(algorithmState, "visibleChildren");
        boolean mIsExpanded = XposedHelpers.getBooleanField(mStackScrollAlgorithm, "mIsExpanded");
        int childCount = visibleChildren.size();
        Object topHeadsUpEntry = null;
        for (int i = 0; i < childCount; i++) {
            View child = visibleChildren.get(i);
            if (!(ExpandableNotificationRow.isInstance(child))) {
                break;
            }
            Object row = child;
            boolean isHeadsUp = ExpandableNotificationRow.isInstance(row) && (boolean)XposedHelpers.callMethod(row, "isHeadsUp");
            if (!isHeadsUp) {
                break;
            }
            Object childState = XposedHelpers.callMethod(resultState, "getViewStateForView", row);
            if (topHeadsUpEntry == null) {
                topHeadsUpEntry = row;
                XposedHelpers.setIntField(childState, "location", LOCATION_FIRST_CARD);
            }
            boolean isTopEntry = topHeadsUpEntry == row;
            float unmodifiedEndLocation = XposedHelpers.getFloatField(childState, "yTranslation") + XposedHelpers.getFloatField(childState, "height");
            if (mIsExpanded) {
                // Ensure that the heads up is always visible even when scrolled off
                clampHunToTop(ambientState, row, childState);
                clampHunToMaxTranslation(ambientState, row, childState);
            }
            boolean isPinned = ExpandableView.isInstance(row) && (boolean)XposedHelpers.callMethod(row, "isPinned");
            if (isPinned) {
                XposedHelpers.setFloatField(childState, "yTranslation", Math.max(XposedHelpers.getFloatField(childState, "yTranslation"), 0));
                int height = XposedHelpers.getIntField(childState, "height");
                XposedHelpers.setIntField(childState, "height", Math.max((int)XposedHelpers.callMethod(row, "getIntrinsicHeight"), height));
                Object topState = XposedHelpers.callMethod(resultState, "getViewStateForView", topHeadsUpEntry);
                if (!isTopEntry && (!mIsExpanded
                        || unmodifiedEndLocation < XposedHelpers.getFloatField(topState, "yTranslation") + XposedHelpers.getIntField(topState, "height"))) {
                    // Ensure that a headsUp doesn't vertically extend further than the heads-up at
                    // the top most z-position
                    XposedHelpers.setIntField(childState, "height", (int)XposedHelpers.callMethod(row, "getIntrinsicHeight"));
                    XposedHelpers.setFloatField(childState, "yTranslation", XposedHelpers.getFloatField(topState, "yTranslation") + XposedHelpers.getIntField(topState, "height")
                            - XposedHelpers.getIntField(childState, "height"));
                }
            }
        }
    }

    private static int getMaxAllowedChildHeight(View child) {
        if (ExpandableView.isInstance(child)) {
            View expandableView = child;
            return (int)XposedHelpers.callMethod(expandableView, "getIntrinsicHeight");
        }
        return child == null? XposedHelpers.getIntField(mStackScrollAlgorithm, "mCollapsedSize") : child.getHeight();
    }

    private static void clampHunToTop(Object ambientState, Object row,
                               Object childState) {
        float newTranslation = Math.max(((float)XposedHelpers.callMethod(ambientState, "getTopPadding")
                + (float)XposedHelpers.callMethod(ambientState, "getStackTranslation")), XposedHelpers.getFloatField(childState, "yTranslation"));
        XposedHelpers.setIntField(childState, "height", (int) Math.max(XposedHelpers.getIntField(childState, "height") - (newTranslation
                - XposedHelpers.getFloatField(childState, "yTranslation")), (int)XposedHelpers.callMethod(row, "getHeadsUpHeight")));//should be getMinHeight but not yet implemented properly
        XposedHelpers.setFloatField(childState, "yTranslation", newTranslation);
    }

    private static void clampHunToMaxTranslation(Object ambientState, Object row,
                                          Object childState) {
        float newTranslation;
        float bottomPosition = (float)XposedHelpers.callMethod(ambientState, "getMaxHeadsUpTranslation") - (int)XposedHelpers.callMethod(row, "getMinHeight");
        newTranslation = Math.min(XposedHelpers.getFloatField(childState, "yTranslation"), bottomPosition);
        XposedHelpers.setIntField(childState, "height", (int) Math.max(XposedHelpers.getIntField(childState, "height")
                - (XposedHelpers.getFloatField(childState, "yTranslation") - newTranslation), (int)XposedHelpers.callMethod(row, "getHeadsUpHeight")));//same as above
        XposedHelpers.setFloatField(childState, "yTranslation", newTranslation);
    }

    public static void updateClipping(Object resultState,
                                      Object algorithmState, Object ambientState) {
        ArrayList<ViewGroup> visibleChildren = (ArrayList<ViewGroup>) XposedHelpers.getObjectField(algorithmState, "visibleChildren");

        float drawStart = (float) XposedHelpers.callMethod(ambientState, "getTopPadding") + (float) XposedHelpers.callMethod(ambientState, "getStackTranslation");

        float previousNotificationEnd = 0;
        float previousNotificationStart = 0;

        int childCount = visibleChildren.size();
        for (int i = 0; i < childCount; i++) {
            Object child = visibleChildren.get(i);
            Object state = XposedHelpers.callMethod(resultState, "getViewStateForView", child);
            boolean mIsHeadsUp = ExpandableNotificationRow.isInstance(child) && XposedHelpers.getBooleanField(child, "mIsHeadsUp");
            boolean isTransparent = ExpandableView.isInstance(child) && (boolean)XposedHelpers.callMethod(child, "isTransparent");
            if (!mIsHeadsUp) {
                previousNotificationEnd = Math.max(drawStart, previousNotificationEnd);
                previousNotificationStart = Math.max(drawStart, previousNotificationStart);
            }
            float newYTranslation = XposedHelpers.getFloatField(state, "yTranslation");
            float newHeight = XposedHelpers.getFloatField(state, "height");

            float newNotificationEnd = newYTranslation + newHeight;
            boolean isHeadsUp = (ExpandableNotificationRow.isInstance(child))
                    && (boolean) XposedHelpers.callMethod(child, "isPinned");
            if (newYTranslation < previousNotificationEnd
                    && (!isHeadsUp || (boolean) XposedHelpers.callMethod(ambientState, "isShadeExpanded"))) {
                // The previous view is overlapping on top, clip!
                float overlapAmount = previousNotificationEnd - newYTranslation;
                XposedHelpers.setIntField(state, "clipTopAmount", (int) overlapAmount);
            } else {
                XposedHelpers.setIntField(state, "clipTopAmount", 0);
            }
            if (!isTransparent) {
                // Only update the previous values if we are not transparent,
                // otherwise we would clip to a transparent view.
                previousNotificationEnd = newNotificationEnd;
                previousNotificationStart = newYTranslation;

            }
        }
    }

    private static void updateZValuesForState(Object resultState,
                                       Object algorithmState, Object ambientState) {

        int mZDistanceBetweenElements = XposedHelpers.getIntField(mStackScrollAlgorithm, "mZDistanceBetweenElements");
        int mZBasicHeight = XposedHelpers.getIntField(mStackScrollAlgorithm, "mZBasicHeight");
        float itemsInBottomStack = XposedHelpers.getFloatField(algorithmState, "itemsInBottomStack");
        List<ViewGroup> visibleChildren = (List<ViewGroup>) XposedHelpers.getObjectField(algorithmState, "visibleChildren");
        int childCount = visibleChildren.size();
        float childrenOnTop = 0.0f;
        for (int i = childCount - 1; i >= 0; i--) {
            View child = visibleChildren.get(i);
            boolean mIsHeadsUp = ExpandableNotificationRow.isInstance(child) && XposedHelpers.getBooleanField(child, "mIsHeadsUp");
            Object childViewState = XposedHelpers.callMethod(resultState, "getViewStateForView", child);
            float yTranslation = XposedHelpers.getFloatField(childViewState, "yTranslation");
            if (i > (childCount - 1 - itemsInBottomStack)) {
                // We are in the bottom stack
                float numItemsAbove = i - (childCount - 1 - itemsInBottomStack);
                float zSubtraction;
                if (numItemsAbove <= 1.0f) {
                    float factor = 0.2f;
                    // Lets fade in slower to the threshold to make the shadow fade in look nicer
                    if (numItemsAbove <= factor) {
                        zSubtraction = FakeShadowView.SHADOW_SIBLING_TRESHOLD
                                * numItemsAbove * (1.0f / factor);
                    } else {
                        zSubtraction = FakeShadowView.SHADOW_SIBLING_TRESHOLD
                                + (numItemsAbove - factor) * (1.0f / (1.0f - factor))
                                * (mZDistanceBetweenElements
                                - FakeShadowView.SHADOW_SIBLING_TRESHOLD);
                    }
                } else {
                    zSubtraction = numItemsAbove * mZDistanceBetweenElements;
                }
                XposedHelpers.setFloatField(childViewState, "zTranslation", mZBasicHeight - zSubtraction);
            } else if (mIsHeadsUp
                    && yTranslation < (float)XposedHelpers.callMethod(ambientState, "getTopPadding")
                    + (float)XposedHelpers.callMethod(ambientState, "getStackTranslation")) {
                if (childrenOnTop != 0.0f) {
                    childrenOnTop++;
                } else {
                    float overlap = (float)XposedHelpers.callMethod(ambientState, "getTopPadding")
                            + (float)XposedHelpers.callMethod(ambientState, "getStackTranslation") - XposedHelpers.getFloatField(childViewState, "yTranslation");
                    childrenOnTop += Math.min(1.0f, overlap / (XposedHelpers.getIntField(childViewState, "height")));
                }
                XposedHelpers.setFloatField(childViewState, "zTranslation", mZBasicHeight
                        + childrenOnTop * mZDistanceBetweenElements);
            } else {
                XposedHelpers.setFloatField(childViewState, "zTranslation", mZBasicHeight);
            }
        }
    }

    private static void initAlgorithmState(Object resultState, Object state,
                                           Object ambientState) {
        ArrayList<?> visibleChildren = (ArrayList<?>)XposedHelpers.getObjectField(state, "visibleChildren");

        XposedHelpers.setFloatField(state, "itemsInBottomStack", 0.0f);
        XposedHelpers.setFloatField(state, "partialInBottom", 0.0f);
        float bottomOverScroll = (float)XposedHelpers.callMethod(ambientState, "getOverScrollAmount", false /* onTop */);

        int scrollY = (int)XposedHelpers.callMethod(ambientState, "getScrollY");

        // Due to the overScroller, the stackscroller can have negative scroll state. This is
        // already accounted for by the top padding and doesn't need an additional adaption
        scrollY = Math.max(0, scrollY);
        XposedHelpers.setIntField(state, "scrollY",(int) (scrollY + bottomOverScroll));

        //now init the visible children and update paddings

        ViewGroup hostView = (ViewGroup)XposedHelpers.callMethod(resultState, "getHostView");
        int childCount = hostView.getChildCount();
        visibleChildren.clear();
        visibleChildren.ensureCapacity(childCount);
        int notGoneIndex = 0;
        for (int i = 0; i < childCount; i++) {
            View v = (View) hostView.getChildAt(i);
            if (v.getVisibility() != View.GONE) {
                notGoneIndex = (int)XposedHelpers.callMethod(mStackScrollAlgorithm, "updateNotGoneIndex", resultState, state, notGoneIndex, v);
                }
                /*if (v instanceof expandableClass)*/
                if (ExpandableNotificationRow.getClass().isInstance(v)){
                    Object row = (Object) v;

                    // handle the notgoneIndex for the children as well
                    List<ViewGroup> children =
                            (List<ViewGroup>)XposedHelpers.callMethod(row, "getNotificationChildren");
                    if (XposedHelpers.getBooleanField(row, "isSummaryWithChildren") && children != null) {
                        for (ViewGroup childRow : children) {
                            if (childRow.getVisibility() != View.GONE) {
                                Object childState
                                        = XposedHelpers.callMethod(resultState, "getViewStateForView", childRow);
                                XposedHelpers.setIntField(childState, "notGoneIndex", notGoneIndex);
                                notGoneIndex++;
                            }
                        }
                    }
                }
            }
        }

    private static void updateStateForChildTransitioningInBottom(Object algorithmState,
                                                          float transitioningPositionStart, Object child, float currentYPosition,
                                                          Object childViewState, int childHeight) {

        // This is the transitioning element on top of bottom stack, calculate how far we are in.
        XposedHelpers.setFloatField(algorithmState, "partialInBottom", 1.0f - (
                (transitioningPositionStart - currentYPosition) / (childHeight +
                        XposedHelpers.getIntField(mStackScrollAlgorithm, "mPaddingBetweenElements"))));
        // the offset starting at the transitionPosition of the bottom stack
        float offset = mBottomStackIndentationFunctor.getValue(XposedHelpers.getFloatField(algorithmState, "partialInBottom"));
        XposedHelpers.setFloatField(algorithmState, "itemsInBottomStack", XposedHelpers.getFloatField(algorithmState, "itemsInBottomStack") + XposedHelpers.getFloatField(algorithmState, "partialInBottom"));
        int newHeight = childHeight;
        if (childHeight > (int)XposedHelpers.callMethod(child, "getHeight")) {

            newHeight = (int) Math.max(Math.min(transitioningPositionStart + offset -
                            XposedHelpers.getIntField(mStackScrollAlgorithm, "mPaddingBetweenElements") - currentYPosition, childHeight),
                    (int)XposedHelpers.callMethod(child, "getHeight"));
            XposedHelpers.setIntField(childViewState, "height", newHeight);
        }
        XposedHelpers.setFloatField(childViewState, "yTranslation", transitioningPositionStart + offset - newHeight
                - XposedHelpers.getIntField(mStackScrollAlgorithm, "mPaddingBetweenElements"));
        XposedHelpers.setIntField(childViewState, "location",LOCATION_MAIN_AREA);
    }

    private static void updateStateForChildFullyInBottomStack(Object algorithmState,
                                                       float transitioningPositionStart, Object childViewState,
                                                       int collapsedHeight, Object ambientState) {
        int mPaddingBetweenElements = XposedHelpers.getIntField(mStackScrollAlgorithm, "mPaddingBetweenElements");
        float currentYPosition;
        XposedHelpers.setFloatField(algorithmState, "itemsInBottomStack", XposedHelpers.getFloatField(algorithmState, "itemsInBottomStack")+ 1.0f);
        if (XposedHelpers.getFloatField(algorithmState, "itemsInBottomStack") < MAX_ITEMS_IN_BOTTOM_STACK) {
            // We are visually entering the bottom stack
            currentYPosition = transitioningPositionStart
                    + mBottomStackIndentationFunctor.getValue(XposedHelpers.getFloatField(algorithmState, "itemsInBottomStack"))
                    - mPaddingBetweenElements;

            XposedHelpers.setIntField(childViewState, "location", LOCATION_BOTTOM_STACK_PEEKING);
        } else {
            // we are fully inside the stack
            if (XposedHelpers.getFloatField(algorithmState, "itemsInBottomStack") > MAX_ITEMS_IN_BOTTOM_STACK + 2) {
                //XposedHelpers.setBooleanField(childViewState, "hidden", true);
                //XposedHelpers.setFloatField(childViewState, "shadowAlpha", 0.0f);
                XposedHelpers.setFloatField(childViewState, "alpha", 0.0f);

            } else if (XposedHelpers.getFloatField(algorithmState, "itemsInBottomStack")
                    > MAX_ITEMS_IN_BOTTOM_STACK + 1) {
                //XposedHelpers.setFloatField(childViewState, "shadowAlpha", 1.0f - XposedHelpers.getFloatField(algorithmState, "partialInBottom"));
                XposedHelpers.setFloatField(childViewState, "alpha", 1.0f - XposedHelpers.getFloatField(algorithmState, "partialInBottom"));
            }
            XposedHelpers.setIntField(childViewState, "location", LOCATION_BOTTOM_STACK_HIDDEN);
            currentYPosition = (int)XposedHelpers.callMethod(ambientState, "getInnerHeight");
        }
        XposedHelpers.setIntField(childViewState, "height", collapsedHeight);
        XposedHelpers.setFloatField(childViewState, "yTranslation", currentYPosition - collapsedHeight);
    }

    private static void clampPositionToBottomStackStart(Object childViewState,
                                                 int childHeight, int minHeight, Object ambientState) {

        int mBottomStackPeekSize = XposedHelpers.getIntField(mStackScrollAlgorithm, "mBottomStackPeekSize");
        int mBottomStackSlowDownLength = XposedHelpers.getIntField(mStackScrollAlgorithm, "mBottomStackSlowDownLength");
        int bottomStackStart = (int)XposedHelpers.callMethod(ambientState, "getInnerHeight")
                - mBottomStackPeekSize - mBottomStackSlowDownLength;
        int childStart = bottomStackStart - childHeight;
        if (childStart < XposedHelpers.getFloatField(childViewState, "yTranslation")) {
            float newHeight = bottomStackStart - XposedHelpers.getFloatField(childViewState, "yTranslation");
            if (newHeight < minHeight) {
                newHeight = minHeight;
                XposedHelpers.setFloatField(childViewState, "yTranslation", bottomStackStart - minHeight);
            }
            XposedHelpers.setIntField(childViewState, "height", (int) newHeight);
        }
    }

    private static void updateFirstChildHeight(Object child, Object childViewState, int childHeight, Object ambientState) {

        int mBottomStackPeekSize = XposedHelpers.getIntField(mStackScrollAlgorithm, "mBottomStackPeekSize");
        int mBottomStackSlowDownLength = XposedHelpers.getIntField(mStackScrollAlgorithm, "mBottomStackPeekSize");

        // The starting position of the bottom stack peek
        int bottomPeekStart = (int) XposedHelpers.callMethod(ambientState, "getInnerHeight") - mBottomStackPeekSize -
                mBottomStackSlowDownLength + (int) XposedHelpers.callMethod(ambientState, "getScrollY");

        // Collapse and expand the first child while the shade is being expanded

        XposedHelpers.setIntField(childViewState, "height", (int) Math.max(Math.min(bottomPeekStart, (float) childHeight),
                (int)XposedHelpers.callMethod(child, "getMinHeight")));
    }

    private static void updateScrollStateForAddedChildren() {
        int mPaddingBetweenElements = XposedHelpers.getIntField(mNotificationStackScroller, "mPaddingBetweenElements");
        int mOwnScrollY = XposedHelpers.getIntField(mNotificationStackScroller, "mOwnScrollY");
        ArrayList<View> mChildrenToAddAnimated = (ArrayList<View>)XposedHelpers.getObjectField(mNotificationStackScroller, "mChildrenToAddAnimated");
        if (mChildrenToAddAnimated.isEmpty()) {
            return;
        }
        for (int i = 0; i < (int)XposedHelpers.callMethod(mNotificationStackScroller, "getChildCount"); i++) {
            View child = (View) XposedHelpers.callMethod(mNotificationStackScroller, "getChildAt", i);
            if (mChildrenToAddAnimated.contains(child)) {
                int startingPosition = (int)XposedHelpers.callMethod(mNotificationStackScroller, "getPositionInLinearLayout", child);
                int padding = mPaddingBetweenElements;
                int childHeight = (int)XposedHelpers.callMethod(mNotificationStackScroller, "getIntrinsicHeight", child) + padding;
                if (startingPosition < mOwnScrollY) {
                    // This child starts off screen, so let's keep it offscreen to keep the others visible

                    XposedHelpers.setIntField(mNotificationStackScroller, "mOwnScrollY", mOwnScrollY + childHeight);
                }
            }
        }
        XposedHelpers.callMethod(mNotificationStackScroller, "clampScrollPosition");
    }

    private static void updateQsExpansion() {
        QSContainerHelper.setQsExpansion((float)XposedHelpers.callMethod(mNotificationPanelView, "getQsExpansionFraction"), (float)XposedHelpers.callMethod(mNotificationPanelView, "getHeaderTranslation"));
    }

    private static final View.OnClickListener mExpandIndicatorListener = new SafeOnClickListener() {
        @Override
        public void onClickSafe(View v) {
            // Fixes an issue with the indicator having two backgrounds when layer type is hardware
            mExpandIndicator.setLayerType(View.LAYER_TYPE_NONE, null);
            flingSettings(!mExpandIndicator.isExpanded());
        }
    };

    private static Runnable mRunAfterInstantCollapse;
    private static Runnable mInstantCollapseRunnable = new Runnable() {
        @Override
        public void run() {
            instantCollapse();
            if (mRunAfterInstantCollapse != null)
                mNotificationPanelView.post(mRunAfterInstantCollapse);
        }
    };

    public static void expandWithQs() {
        try {
            if (ConfigUtils.M) {
                XposedHelpers.callMethod(mNotificationPanelView, "expandWithQs");
            } else {
                XposedHelpers.callMethod(mNotificationPanelView, "expand");
            }
        } catch (Throwable ignore) {

        }
    }

    public static boolean isExpanded() {
        return (mExpandIndicator != null && mExpandIndicator.isExpanded());
    }

    public static boolean isCollapsed() {
        return (mExpandIndicator != null && !mExpandIndicator.isExpanded());
    }

    public static void expandIfNecessary() {
        if (mExpandIndicator != null && mNotificationPanelView != null) {
            if (!mExpandIndicator.isExpanded()) {
                flingSettings(true);
            }
        }
    }

    public static void collapseIfNecessary() {
        if (mExpandIndicator != null && mNotificationPanelView != null) {
            if (mExpandIndicator.isExpanded()) {
                flingSettings(false);
            }
        }
    }

    private static void flingSettings(boolean expanded) {
        try {
            XposedHelpers.callMethod(mNotificationPanelView, "flingSettings", new Class[]{float.class, boolean.class, Runnable.class, boolean.class}, 0, expanded, null, true);
        } catch (Throwable t) {
            XposedHelpers.callMethod(mNotificationPanelView, "flingSettings", 0, expanded);
        }
    }

    public static boolean isOnKeyguard() {
        return getStatusBarState() == NotificationPanelHooks.STATE_KEYGUARD;
    }

    public static int getStatusBarState() {
        return XposedHelpers.getIntField(mNotificationPanelView, "mStatusBarState");
    }

    public static void hook(ClassLoader classLoader) {
        try {
            if (ConfigUtils.qs().header) { // Although this is the notification panel everything here is header-related (mainly QS editor)

                Class<?> classNotificationStackScrollLayout = XposedHelpers.findClass(CLASS_NOTIFICATION_STACK_SCROLL_LAYOUT, classLoader);
                Class<?> classStackScrollAlgorithm = XposedHelpers.findClass(CLASS_STACK_SCROLL_ALGORITHM, classLoader);
                Class<?> classNotificationPanelView = XposedHelpers.findClass(CLASS_NOTIFICATION_PANEL_VIEW, classLoader);
                Class<?> classQSContainer = XposedHelpers.findClass(CLASS_QS_CONTAINER, classLoader);
                Class<?> classPanelView = XposedHelpers.findClass(CLASS_PANEL_VIEW, classLoader);
                Class<?> classObservableScrollView = XposedHelpers.findClass(CLASS_OBSERVABLE_SCROLL_VIEW, classLoader);
                ExpandableNotificationRow = XposedHelpers.findClass("com.android.systemui.statusbar.ExpandableNotificationRow", classLoader);
                ExpandableView = XposedHelpers.findClass("com.android.systemui.statusbar.ExpandableView", classLoader);

                XposedHelpers.findAndHookMethod(classNotificationPanelView, "onFinishInflate", onFinishInflateHook);
                XposedHelpers.findAndHookMethod(classNotificationPanelView, "setBarState", int.class, boolean.class, boolean.class, setBarStateHook);
                XposedHelpers.findAndHookMethod(classNotificationPanelView, "requestDisallowInterceptTouchEvent", boolean.class, XC_MethodReplacement.DO_NOTHING);
                XposedHelpers.findAndHookMethod(classNotificationPanelView, "onOverscrolled", float.class, float.class, int.class, XC_MethodReplacement.DO_NOTHING);
                XposedHelpers.findAndHookMethod(classNotificationPanelView, "getTempQsMaxExpansion", getTempQsMaxExpansion);
                XposedHelpers.findAndHookMethod(classNotificationPanelView, "onExpandingStarted", onExpandingStartedHook);
                XposedHelpers.findAndHookMethod(classNotificationPanelView, "onClick", View.class, onClick);
                XposedHelpers.findAndHookMethod(classNotificationPanelView, "setVerticalPanelTranslation", float.class, setVerticalPanelTranslation);
                XposedHelpers.findAndHookMethod(classNotificationPanelView, "onQsExpansionStarted", int.class, onQsExpansionStarted);
                XposedHelpers.findAndHookMethod(classNotificationPanelView, "updateQsState", updateQsState);
                XposedHelpers.findAndHookMethod(classNotificationPanelView, "isScrolledToBottom", isScrolledToBottom);
                XposedHelpers.findAndHookMethod(classNotificationPanelView, "getHeaderTranslation", getHeaderTranslation);
                XposedHelpers.findAndHookMethod(classNotificationPanelView, "getPeekHeight", getPeekHeight);
                XposedHelpers.findAndHookMethod(classNotificationPanelView, "getScrollViewScrollY", XC_MethodReplacement.returnConstant(0));
                XposedHelpers.findAndHookMethod(classNotificationPanelView, "onExpandingFinished", onExpandingFinishedHook);
                XposedHelpers.findAndHookMethod(classNotificationPanelView, "onScrollChanged", onScrollChanged);
                XposedHelpers.findAndHookMethod(classNotificationPanelView, "onHeightUpdated", float.class, onHeightUpdated);
                XposedHelpers.findAndHookMethod(classNotificationPanelView, "calculatePanelHeightQsExpanded", calculatePanelHeightQsExpanded);
                XposedHelpers.findAndHookMethod(classNotificationPanelView, "getMaxPanelHeight", getMaxPanelHeight);
                XposedHelpers.findAndHookMethod(classNotificationPanelView, "getFadeoutAlpha", getFadeoutAlpha);
                /*XposedHelpers.findAndHookMethod(classNotificationPanelView, "animateHeaderSlidingIn", XC_MethodReplacement.DO_NOTHING); //TODO make this work
                XposedHelpers.findAndHookMethod(classNotificationPanelView, "animateHeaderSlidingOut", XC_MethodReplacement.DO_NOTHING);*/
                XposedHelpers.findAndHookMethod(classNotificationPanelView, "updateHeaderShade", XC_MethodReplacement.DO_NOTHING);
                XposedHelpers.findAndHookMethod(classNotificationPanelView, "updateHeader", updateHeader);
                XposedHelpers.findAndHookMethod(classNotificationPanelView, "positionClockAndNotifications", positionClockAndNotifications);
                XposedHelpers.findAndHookMethod(classNotificationPanelView, "setQsExpansion", float.class, setQsExpansion);
                XposedHelpers.findAndHookMethod(classNotificationPanelView, "setQsTranslation", float.class, XC_MethodReplacement.DO_NOTHING);

                XposedHelpers.findAndHookMethod(classNotificationStackScrollLayout, "initView", Context.class, initViewHook);
                XposedHelpers.findAndHookMethod(classNotificationStackScrollLayout, "setScrollView", ViewGroup.class, XC_MethodReplacement.DO_NOTHING);
                XposedHelpers.findAndHookMethod(classNotificationStackScrollLayout, "onTouchEvent", MotionEvent.class, onTouchEventHook);
                XposedHelpers.findAndHookMethod(classNotificationStackScrollLayout, "onInterceptTouchEvent", MotionEvent.class, onInterceptTouchEventHook);
                XposedHelpers.findAndHookMethod(classNotificationStackScrollLayout, "onScrollTouch", MotionEvent.class, onScrollTouchHook);
                XposedHelpers.findAndHookMethod(classNotificationStackScrollLayout, "setStackHeight", float.class, setStackHeight);
                XposedBridge.hookAllMethods(classNotificationStackScrollLayout, "setInterceptDelegateEnabled", XC_MethodReplacement.DO_NOTHING);
                XposedHelpers.findAndHookMethod(classNotificationStackScrollLayout, "getPeekHeight", getPeekHeightStackScroller);
                XposedHelpers.findAndHookMethod(classNotificationStackScrollLayout, "getEmptyBottomMargin", getEmptyBottomMargin);
                XposedHelpers.findAndHookMethod(classNotificationStackScrollLayout, "updateTopPadding", float.class, int.class, boolean.class, boolean.class, updateTopPadding);
                XposedHelpers.findAndHookMethod(classNotificationStackScrollLayout, "getMinStackHeight", getMinStackHeight);
                XposedHelpers.findAndHookMethod(classNotificationStackScrollLayout, "getDismissViewHeight", getDismissViewHeight);
                XposedHelpers.findAndHookMethod(classNotificationStackScrollLayout, "needsHeightAdaption", XC_MethodReplacement.DO_NOTHING);
                XposedHelpers.findAndHookMethod(classNotificationStackScrollLayout, "updateChildren", updateChildrenHook);

                if(ConfigUtils.notifications().experimental) {
                    XposedBridge.hookAllMethods(classStackScrollAlgorithm, "initConstants", initConstantsHook);
                    //XposedBridge.hookAllMethods(classStackScrollAlgorithm, "getStackScrollState", getStackScrollState);// TODO might need some optimization because it currently makes the stackscroller sluggish
                    /*XposedBridge.hookAllMethods(classStackScrollAlgorithm, "updateStateForTopStackChild", XC_MethodReplacement.DO_NOTHING);
                    XposedBridge.hookAllMethods(classStackScrollAlgorithm, "findNumberOfItemsInTopStackAndUpdateState", XC_MethodReplacement.DO_NOTHING);
                    XposedBridge.hookAllMethods(classStackScrollAlgorithm, "updateChildClippingAndBackground", XC_MethodReplacement.DO_NOTHING);
                    XposedBridge.hookAllMethods(classStackScrollAlgorithm, "updateZValuesForState", XC_MethodReplacement.DO_NOTHING);
                    XposedBridge.hookAllMethods(classStackScrollAlgorithm, "onExpansionStarted", XC_MethodReplacement.DO_NOTHING);
                    XposedBridge.hookAllMethods(classStackScrollAlgorithm, "updateFirstChildHeightWhileExpanding", XC_MethodReplacement.DO_NOTHING);
                    XposedBridge.hookAllMethods(classStackScrollAlgorithm, "updateFirstChildMaxSizeToMaxHeight", XC_MethodReplacement.DO_NOTHING);
                    XposedBridge.hookAllMethods(classStackScrollAlgorithm, "onExpansionStopped", XC_MethodReplacement.DO_NOTHING);
                    XposedBridge.hookAllMethods(classStackScrollAlgorithm, "notifyChildrenChanged", XC_MethodReplacement.DO_NOTHING);
                    XposedBridge.hookAllMethods(classStackScrollAlgorithm, "onReset", XC_MethodReplacement.DO_NOTHING);
                    XposedBridge.hookAllMethods(classStackScrollAlgorithm, "clampYTranslation", XC_MethodReplacement.DO_NOTHING);
                    XposedBridge.hookAllMethods(classStackScrollAlgorithm, "clampPositionToTopStackEnd", XC_MethodReplacement.DO_NOTHING);
                    XposedBridge.hookAllMethods(classStackScrollAlgorithm, "clampPositionToBottomStackStart", XC_MethodReplacement.DO_NOTHING);
                    XposedBridge.hookAllMethods(classStackScrollAlgorithm, "clampYTranslation", XC_MethodReplacement.DO_NOTHING);
                    XposedBridge.hookAllMethods(classStackScrollAlgorithm, "updateStateForChildFullyInBottomStack", XC_MethodReplacement.DO_NOTHING);
                    XposedBridge.hookAllMethods(classStackScrollAlgorithm, "updateStateForChildTransitioningInBottom", XC_MethodReplacement.DO_NOTHING);*/
                }

                XposedHelpers.findAndHookMethod(classObservableScrollView, "onScrollChanged", int.class, int.class, int.class, int.class, XC_MethodReplacement.DO_NOTHING);
                XposedHelpers.findAndHookMethod(classObservableScrollView, "onOverScrolled", int.class, int.class, boolean.class, boolean.class, XC_MethodReplacement.DO_NOTHING);
                XposedHelpers.findAndHookMethod(classObservableScrollView, "overScrollBy", int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, boolean.class, XC_MethodReplacement.returnConstant(false));
                //XposedHelpers.findAndHookMethod(classObservableScrollView, "isHandlingTouchEvent", XC_MethodReplacement.returnConstant(false));
                //XposedHelpers.findAndHookMethod(classObservableScrollView, "setTouchEnabled", boolean.class, XC_MethodReplacement.DO_NOTHING);
                //XposedHelpers.findAndHookMethod(classObservableScrollView, "dispatchTouchEvent", MotionEvent.class, XC_MethodReplacement.DO_NOTHING);
                XposedHelpers.findAndHookMethod(classObservableScrollView, "fling", int.class, XC_MethodReplacement.DO_NOTHING);
                //XposedHelpers.findAndHookMethod(classObservableScrollView, "onInterceptTouchEvent", MotionEvent.class, XC_MethodReplacement.DO_NOTHING);
                XposedHelpers.findAndHookMethod(classObservableScrollView, "getMaxScrollY", XC_MethodReplacement.DO_NOTHING);
                XposedHelpers.findAndHookMethod(classObservableScrollView, "isScrolledToBottom", XC_MethodReplacement.DO_NOTHING);
                //XposedHelpers.findAndHookMethod(classObservableScrollView, "dispatchTouchEvent", MotionEvent.class, XC_MethodReplacement.DO_NOTHING);


                XposedHelpers.findAndHookMethod(classNotificationPanelView, "setQsExpansionEnabled", boolean.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        param.args[0] = true;
                    }
                });

                XposedHelpers.findAndHookMethod(classNotificationPanelView, "shouldQuickSettingsIntercept", float.class, float.class, float.class, shouldQuickSettingsIntercept);


                if (ConfigUtils.M)
                    XposedHelpers.findAndHookMethod(classNotificationPanelView, "setVerticalPanelTranslation", float.class, setVerticalPanelTranslationHook);

                XposedHelpers.findAndHookMethod(classPanelView, "schedulePeek", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        XposedHelpers.callMethod(mNotificationPanelView, "setListening", true);
                    }
                });

                XC_MethodHook returnIfCustomizing = new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (mQsCustomizer != null && mQsCustomizer.isCustomizing())
                            param.setResult(false);
                    }
                };

                XposedHelpers.findAndHookMethod(classNotificationPanelView, "onInterceptTouchEvent", MotionEvent.class, returnIfCustomizing);
                XposedHelpers.findAndHookMethod(classNotificationPanelView, "onTouchEvent", MotionEvent.class, returnIfCustomizing);

                if (ConfigUtils.qs().fix_header_space) {
                    XC_MethodReplacement updateQsTranslation = new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                            if (mQsContainerHelper != null)
                                mQsContainerHelper.setQsExpansion((float) XposedHelpers.callMethod(param.thisObject, "getQsExpansionFraction"),
                                        (float) XposedHelpers.callMethod(param.thisObject, "getHeaderTranslation"));
                            return null;
                        }
                    };

                    XposedHelpers.findAndHookMethod(classNotificationPanelView, "setQsTranslation", float.class, updateQsTranslation);

                    XposedHelpers.findAndHookMethod(classQSContainer, "getDesiredHeight", new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                            if (mQsContainerHelper != null)
                                return mQsContainerHelper.getDesiredHeight();
                            return 0;
                        }
                    });

                    XposedHelpers.findAndHookMethod(classQSContainer, "updateBottom", new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                            if (mQsContainerHelper != null)
                                mQsContainerHelper.updateBottom();
                            return null;
                        }
                    });

                    XposedHelpers.findAndHookMethod(classNotificationPanelView, "isQsDetailShowing", new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                            if (mQsContainerHelper != null)
                                return mQsContainerHelper.getQSDetail().isShowingDetail();
                            return null;
                        }
                    });

                    hookOnLayout(classNotificationPanelView, classPanelView);

                    XposedHelpers.findAndHookMethod(classNotificationPanelView, "animateHeaderSlidingIn", new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                            if (mQsContainerHelper != null)
                                mQsContainerHelper.animateHeaderSlidingIn();
                            return null;
                        }
                    });

                    XposedHelpers.findAndHookMethod(classNotificationPanelView, "animateHeaderSlidingOut", new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                            if (mQsContainerHelper != null)
                                mQsContainerHelper.animateHeaderSlidingOut();
                            return null;
                        }
                    });

                    XposedHelpers.findAndHookMethod(classNotificationPanelView, "updateHeaderShade", updateQsTranslation);

                    /*XposedHelpers.findAndHookMethod(classNotificationStackScrollLayout, "onScrollTouch", MotionEvent.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            MotionEvent ev = (MotionEvent) param.args[0];
                            if (mQsContainerHelper != null && ev.getY() < mQsContainerHelper.getBottom())
                                param.setResult(false);
                        }
                    });*/
                }
            }
        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error in hook", t);
        }
    }

    private static void hookOnLayout(final Class<?> classNotificationPanelView, final Class<?> classPanelView) {
        XposedHelpers.findAndHookMethod(classNotificationPanelView, "onLayout", boolean.class, int.class, int.class, int.class, int.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                if (mQsContainerHelper != null)
                    mQsContainerHelper.notificationPanelViewOnLayout(param, classPanelView);
                return null;
            }
        });
    }

    public static void postInstantCollapse(Runnable after) {
        mRunAfterInstantCollapse = after;
        mNotificationPanelView.post(mInstantCollapseRunnable);
    }

    private static void instantCollapse() {
        XposedHelpers.callMethod(mNotificationPanelView, "instantCollapse");
    }

    public static void addBarStateCallback(BarStateCallback callback) {
        mBarStateCallbacks.add(callback);
    }

    public static void removeBarStateCallback(BarStateCallback callback) {
        mBarStateCallbacks.remove(callback);
    }

    static QSCustomizer getQsCustomizer() {
        return mQsCustomizer;
    }

    public interface BarStateCallback {
        void onStateChanged();
    }

    public static void invalidateTileAdapter() {
        if (mQsCustomizer != null)
            mQsCustomizer.invalidateTileAdapter();
    }

    public static void handleStateChanged(Object qsTile, Object state) {
        if (mQsCustomizer != null)
            mQsCustomizer.handleStateChanged(qsTile, state);
    }

    public static void showQsCustomizer(ArrayList<Object> records, boolean animated) {
        if (canShowCustomizer(records))
            mQsCustomizer.show(records, animated);
    }

    public static void showQsCustomizer(ArrayList<Object> records, int x, int y) {
        if (canShowCustomizer(records))
            mQsCustomizer.show(records, x, y);
    }

    private static boolean canShowCustomizer(ArrayList<Object> records) {
        if (records == null) {
            Toast.makeText(StatusBarHeaderHooks.mContext, "Couldn't open edit view; mRecords == null", Toast.LENGTH_SHORT).show();
            XposedHook.logE(TAG, "Couldn't open edit view; mRecords == null", null);
            return false;
        }
        if (mQsCustomizer == null) {
            Toast.makeText(StatusBarHeaderHooks.mContext, "Couldn't open edit view; mQsCustomizer == null", Toast.LENGTH_SHORT).show();
            XposedHook.logE(TAG, "Couldn't open edit view; mQsCustomizer == null", null);
            return false;
        }
        return true;
    }
}
