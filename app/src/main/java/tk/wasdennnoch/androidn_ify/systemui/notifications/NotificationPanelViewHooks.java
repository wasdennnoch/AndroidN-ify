package tk.wasdennnoch.androidn_ify.systemui.notifications;

import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.extracted.systemui.NotificationUtils;
import tk.wasdennnoch.androidn_ify.systemui.qs.QSContainerHelper;

import static tk.wasdennnoch.androidn_ify.systemui.notifications.NotificationPanelHooks.getStatusBarState;
import static tk.wasdennnoch.androidn_ify.systemui.notifications.NotificationPanelHooks.isOnKeyguard;


public class NotificationPanelViewHooks {

    private static final String TAG = "NotificationPanelViewHooks";

    private static final String PACKAGE_SYSTEMUI = XposedHook.PACKAGE_SYSTEMUI;
    private static final String CLASS_NOTIFICATION_STACK_SCROLL_LAYOUT = PACKAGE_SYSTEMUI + ".statusbar.stack.NotificationStackScrollLayout";
    private static final String CLASS_NOTIFICATION_PANEL_VIEW = PACKAGE_SYSTEMUI + ".statusbar.phone.NotificationPanelView";
    private static final String CLASS_OBSERVABLE_SCROLL_VIEW = PACKAGE_SYSTEMUI + ".statusbar.phone.ObservableScrollView";
    private static final String CLASS_HEADSUP_MANAGER = PACKAGE_SYSTEMUI + ".statusbar.policy.HeadsUpManager";
    private static final String CLASS_STACK_SCROLL_ALGORITHM = PACKAGE_SYSTEMUI + ".statusbar.stack.StackScrollAlgorithm";

    private static final int STATE_SHADE = 0;
    private static final int STATE_KEYGUARD = 1;
    private static final int STATE_SHADE_LOCKED = 2;

    private static ViewGroup mNotificationPanelView;
    private static ViewGroup mScrollView;
    private static ViewGroup mQsContainer;
    private static ViewGroup mHeader;
    private static ViewGroup mQsPanel;
    private static ViewGroup mNotificationStackScroller;

    private static Method methodCancelQsAnimation;
    private static Method methodCancelHeightAnimator;
    private static Method methodSetQsExpansion;
    private static Method methodRequestPanelHeightUpdate;
    private static Method methodOnQsExpansionStarted;
    private static Method methodSetQsExpanded;
    private static Method methodGetMaxPanelHeight;
    private static Method methodGetExpandedHeight;
    private static Method methodGetQsExpansionFraction;
    private static Method methodGetHeaderTranslation;

    private static Method methodGetChildCount;
    private static Method methodGetChildAt;
    private static Method methodGetNotGoneChildCount;
    private static Method methodGetFirstChildNotGone;

    private static Method methodHasPinnedHeadsUp;
    private static Method methodGetTopHeadsUpHeight;
    private static Method methodGetIntrinsicHeight;
    private static Method methodGetPositionInLinearLayout;
    private static Method methodClampScrollPosition;
    private static Method methodIsScrolledToBottom;

    private static Field fieldQsExpansionHeight;
    private static Field fieldQsMinExpansionHeight;
    private static Field fieldQsMaxExpansionHeight;
    private static Field fieldQsExpanded;
    private static Field fieldQsExpandImmediate;
    private static Field fieldQsExpandedWhenExpandingStarted;
    private static Field fieldQsFullyExpanded;
    private static Field fieldStackScrollerOverscrolling;
    private static Field fieldHeaderAnimating;
    private static Field fieldKeyguardShowing;
    private static Field fieldQsExpansionFromOverscroll;
    private static Field fieldQsScrimEnabled;
    private static Field fieldIsExpanding;
    private static Field fieldExpandedHeight;
    private static Field fieldTopPaddingOverflow;
    private static Field fieldTopPadding;
    private static Field fieldTopPaddingAdjustment;
    private static Field fieldTrackingHeadsUp;
    private static Field fieldHeadsUpManager;
    private static Field fieldQsExpansionEnabled;
    private static Field fieldHeader;
    private static Field fieldClockPositionResult;
    private static Field fieldStatusBar;
    private static Field fieldScrollYOverride;

    private static Field fieldBottomStackSlowDownHeight;
    private static Field fieldBottomStackPeekSize;
    private static Field fieldMaxLayoutHeight;
    private static Field fieldIntrinsicPadding;
    private static Field fieldPaddingBetweenElements;
    private static Field fieldOwnScrollY;
    private static Field fieldCollapsedSize;
    private static Field fieldInterceptDelegateEnabled;
    private static Field fieldOnlyScrollingInThisMotion;
    private static Field fieldDelegateToScrollView;


    public static void hook(ClassLoader classLoader) {

        Class<?> classNotificationStackScrollLayout = XposedHelpers.findClass(CLASS_NOTIFICATION_STACK_SCROLL_LAYOUT, classLoader);
        Class<?> classNotificationPanelView = XposedHelpers.findClass(CLASS_NOTIFICATION_PANEL_VIEW, classLoader);
        Class<?> classObservableScrollView = XposedHelpers.findClass(CLASS_OBSERVABLE_SCROLL_VIEW, classLoader);
        Class<?> classHeadsUpManager = XposedHelpers.findClass(CLASS_HEADSUP_MANAGER, classLoader);
        Class<?> classStackScrollAlgorithm = XposedHelpers.findClass(CLASS_STACK_SCROLL_ALGORITHM, classLoader);

        methodCancelQsAnimation = XposedHelpers.findMethodBestMatch(classNotificationPanelView, "cancelQsAnimation");
        methodCancelHeightAnimator = XposedHelpers.findMethodBestMatch(classNotificationPanelView, "cancelHeightAnimator");
        methodSetQsExpansion = XposedHelpers.findMethodBestMatch(classNotificationPanelView, "setQsExpansion", float.class);
        methodRequestPanelHeightUpdate = XposedHelpers.findMethodBestMatch(classNotificationPanelView, "requestPanelHeightUpdate");
        methodOnQsExpansionStarted = XposedHelpers.findMethodBestMatch(classNotificationPanelView, "onQsExpansionStarted");
        methodSetQsExpanded = XposedHelpers.findMethodBestMatch(classNotificationPanelView, "setQsExpanded", boolean.class);
        methodGetMaxPanelHeight = XposedHelpers.findMethodBestMatch(classNotificationPanelView, "getMaxPanelHeight");
        methodGetExpandedHeight = XposedHelpers.findMethodBestMatch(classNotificationPanelView, "getExpandedHeight");
        methodGetQsExpansionFraction = XposedHelpers.findMethodBestMatch(classNotificationPanelView, "getQsExpansionFraction");
        methodGetHeaderTranslation = XposedHelpers.findMethodBestMatch(classNotificationPanelView, "getHeaderTranslation");

        methodGetChildCount = XposedHelpers.findMethodBestMatch(classNotificationStackScrollLayout, "getChildCount");
        methodGetChildAt = XposedHelpers.findMethodBestMatch(classNotificationStackScrollLayout, "getChildAt", int.class);
        methodGetIntrinsicHeight = XposedHelpers.findMethodBestMatch(classNotificationStackScrollLayout, "getIntrinsicHeight", View.class);
        methodGetPositionInLinearLayout = XposedHelpers.findMethodBestMatch(classNotificationStackScrollLayout, "getPositionInLinearLayout", View.class);
        methodClampScrollPosition = XposedHelpers.findMethodBestMatch(classNotificationStackScrollLayout, "clampScrollPosition");
        methodGetNotGoneChildCount = XposedHelpers.findMethodBestMatch(classNotificationStackScrollLayout, "getNotGoneChildCount");
        methodGetFirstChildNotGone = XposedHelpers.findMethodBestMatch(classNotificationStackScrollLayout, "getFirstChildNotGone");
        methodIsScrolledToBottom = XposedHelpers.findMethodBestMatch(classNotificationStackScrollLayout, "isScrolledToBottom");

        methodHasPinnedHeadsUp = XposedHelpers.findMethodBestMatch(classHeadsUpManager, "hasPinnedHeadsUp");
        methodGetTopHeadsUpHeight = XposedHelpers.findMethodBestMatch(classHeadsUpManager, "getTopHeadsUpHeight");

        fieldQsExpansionHeight = XposedHelpers.findField(classNotificationPanelView, "mQsExpansionHeight");
        fieldQsMinExpansionHeight = XposedHelpers.findField(classNotificationPanelView, "mQsMinExpansionHeight");
        fieldQsMaxExpansionHeight = XposedHelpers.findField(classNotificationPanelView, "mQsMaxExpansionHeight");
        fieldQsExpanded = XposedHelpers.findField(classNotificationPanelView, "mQsExpanded");
        fieldQsExpandImmediate = XposedHelpers.findField(classNotificationPanelView, "mQsExpandImmediate");
        fieldQsExpandedWhenExpandingStarted = XposedHelpers.findField(classNotificationPanelView, "mQsExpandedWhenExpandingStarted");
        fieldQsFullyExpanded = XposedHelpers.findField(classNotificationPanelView, "mQsFullyExpanded");
        fieldStackScrollerOverscrolling = XposedHelpers.findField(classNotificationPanelView, "mStackScrollerOverscrolling");
        fieldHeaderAnimating = XposedHelpers.findField(classNotificationPanelView, "mHeaderAnimating");
        fieldKeyguardShowing = XposedHelpers.findField(classNotificationPanelView, "mKeyguardShowing");
        fieldQsExpansionFromOverscroll = XposedHelpers.findField(classNotificationPanelView, "mQsExpansionFromOverscroll");
        fieldQsScrimEnabled = XposedHelpers.findField(classNotificationPanelView, "mQsScrimEnabled");
        fieldIsExpanding = XposedHelpers.findField(classNotificationPanelView, "mIsExpanding");
        fieldExpandedHeight = XposedHelpers.findField(classNotificationPanelView, "mExpandedHeight");
        fieldTopPaddingAdjustment = XposedHelpers.findField(classNotificationPanelView, "mTopPaddingAdjustment");
        fieldQsExpansionEnabled = XposedHelpers.findField(classNotificationPanelView, "mQsExpansionEnabled");
        fieldHeader = XposedHelpers.findField(classNotificationPanelView, "mHeader");
        fieldClockPositionResult = XposedHelpers.findField(classNotificationPanelView, "mClockPositionResult");
        fieldStatusBar = XposedHelpers.findField(classNotificationPanelView, "mStatusBar");
        fieldScrollYOverride = XposedHelpers.findField(classNotificationPanelView, "mScrollYOverride");

        fieldBottomStackSlowDownHeight = XposedHelpers.findField(classNotificationStackScrollLayout, "mBottomStackSlowDownHeight");
        fieldBottomStackPeekSize = XposedHelpers.findField(classNotificationStackScrollLayout, "mBottomStackPeekSize");
        fieldMaxLayoutHeight = XposedHelpers.findField(classNotificationStackScrollLayout, "mMaxLayoutHeight");
        fieldIntrinsicPadding = XposedHelpers.findField(classNotificationStackScrollLayout, "mIntrinsicPadding");
        fieldTopPaddingOverflow = XposedHelpers.findField(classNotificationStackScrollLayout, "mTopPaddingOverflow");
        fieldTopPadding = XposedHelpers.findField(classNotificationStackScrollLayout, "mTopPadding");
        fieldTrackingHeadsUp = XposedHelpers.findField(classNotificationStackScrollLayout, "mTrackingHeadsUp");
        fieldHeadsUpManager = XposedHelpers.findField(classNotificationStackScrollLayout, "mHeadsUpManager");
        fieldPaddingBetweenElements = XposedHelpers.findField(classNotificationStackScrollLayout, "mPaddingBetweenElements");
        fieldOwnScrollY = XposedHelpers.findField(classNotificationStackScrollLayout, "mOwnScrollY");
        fieldCollapsedSize = XposedHelpers.findField(classNotificationStackScrollLayout, "mCollapsedSize");
        fieldInterceptDelegateEnabled = XposedHelpers.findField(classNotificationStackScrollLayout, "mInterceptDelegateEnabled");
        fieldOnlyScrollingInThisMotion = XposedHelpers.findField(classNotificationStackScrollLayout, "mOnlyScrollingInThisMotion");
        fieldDelegateToScrollView = XposedHelpers.findField(classNotificationStackScrollLayout, "mDelegateToScrollView");

        XposedHelpers.findAndHookMethod(classNotificationPanelView, "onFinishInflate", onFinishInflateHook);

        XposedHelpers.findAndHookMethod(classNotificationPanelView, "onOverscrolled", float.class, float.class, int.class, XC_MethodReplacement.DO_NOTHING);
        XposedHelpers.findAndHookMethod(classNotificationPanelView, "getTempQsMaxExpansion", getTempQsMaxExpansionHook);
        XposedHelpers.findAndHookMethod(classNotificationPanelView, "onExpandingStarted", onExpandingStartedHook);
        XposedHelpers.findAndHookMethod(classNotificationPanelView, "onClick", View.class, onClick);
        XposedHelpers.findAndHookMethod(classNotificationPanelView, "setVerticalPanelTranslation", float.class, setVerticalPanelTranslation);
        XposedHelpers.findAndHookMethod(classNotificationPanelView, "onQsExpansionStarted", int.class, onQsExpansionStarted);
        XposedHelpers.findAndHookMethod(classNotificationPanelView, "updateQsState", updateQsState);
        XposedHelpers.findAndHookMethod(classNotificationPanelView, "isScrolledToBottom", isScrolledToBottom);
        XposedHelpers.findAndHookMethod(classNotificationPanelView, "getHeaderTranslation", getHeaderTranslation);
        XposedHelpers.findAndHookMethod(classNotificationPanelView, "getPeekHeight", getPeekHeightHook);
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
        XposedHelpers.findAndHookMethod(classNotificationPanelView, "notifyVisibleChildrenChanged", XC_MethodReplacement.DO_NOTHING);
        XposedHelpers.findAndHookMethod(classNotificationPanelView, "calculateQsTopPadding", calculateQsTopPaddingHook);
        XposedHelpers.findAndHookMethod(classNotificationPanelView, "onQsTouch", MotionEvent.class, onQsTouchHook);
        XposedBridge.hookAllMethods(classNotificationPanelView, "flingSettings", flingSettingsHook);
        XposedHelpers.findAndHookMethod(classNotificationPanelView, "shouldQuickSettingsIntercept", float.class, float.class, float.class, shouldQuickSettingsInterceptHook);

        XposedHelpers.findAndHookMethod(classNotificationStackScrollLayout, "setScrollView", ViewGroup.class, XC_MethodReplacement.DO_NOTHING);
        XposedBridge.hookAllMethods(classNotificationStackScrollLayout, "setInterceptDelegateEnabled", XC_MethodReplacement.DO_NOTHING);
        XposedHelpers.findAndHookMethod(classNotificationStackScrollLayout, "onTouchEvent", MotionEvent.class, onTouchEventHook);
        XposedHelpers.findAndHookMethod(classNotificationStackScrollLayout, "onInterceptTouchEvent", MotionEvent.class, onInterceptTouchEventHook);
        XposedHelpers.findAndHookMethod(classNotificationStackScrollLayout, "onScrollTouch", MotionEvent.class, onScrollTouchHook);
        XposedHelpers.findAndHookMethod(classNotificationStackScrollLayout, "getPeekHeight", getPeekHeightStackScroller);
        XposedHelpers.findAndHookMethod(classNotificationStackScrollLayout, "getEmptyBottomMargin", getEmptyBottomMargin);
        XposedHelpers.findAndHookMethod(classNotificationStackScrollLayout, "getMinStackHeight", getMinStackHeight);
        XposedHelpers.findAndHookMethod(classNotificationStackScrollLayout, "getDismissViewHeight", getDismissViewHeight);
        XposedHelpers.findAndHookMethod(classNotificationStackScrollLayout, "updateChildren", updateChildrenHook);
        XposedHelpers.findAndHookMethod(classNotificationStackScrollLayout, "updateSpeedBumpIndex", int.class, updateSpeedBumpIndex);
        XposedHelpers.findAndHookMethod(classNotificationStackScrollLayout, "setStackHeight", float.class, setStackHeight);

        XposedHelpers.findAndHookMethod(classObservableScrollView, "overScrollBy", int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, boolean.class, XC_MethodReplacement.returnConstant(false));
        XposedHelpers.findAndHookMethod(classObservableScrollView, "fling", int.class, XC_MethodReplacement.DO_NOTHING);
        XposedHelpers.findAndHookMethod(classObservableScrollView, "getMaxScrollY", XC_MethodReplacement.DO_NOTHING);
        XposedHelpers.findAndHookMethod(classObservableScrollView, "isScrolledToBottom", XC_MethodReplacement.DO_NOTHING);
        XposedHelpers.findAndHookMethod(classObservableScrollView, "setBlockFlinging", boolean.class, XC_MethodReplacement.DO_NOTHING);
        XposedHelpers.findAndHookMethod(classObservableScrollView, "onTouchEvent", MotionEvent.class, XC_MethodReplacement.returnConstant(false));

        XposedHelpers.findAndHookMethod(classStackScrollAlgorithm, "notifyChildrenChanged", ViewGroup.class, XC_MethodReplacement.DO_NOTHING);
    }

    private static final XC_MethodHook onFinishInflateHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            mNotificationPanelView = (ViewGroup) param.thisObject;
            mHeader = (ViewGroup) fieldHeader.get(mNotificationPanelView);
            mScrollView = (ViewGroup) XposedHelpers.getObjectField(mNotificationPanelView, "mScrollView");
            mQsContainer = (ViewGroup) XposedHelpers.getObjectField(mNotificationPanelView, "mQsContainer");
            mQsPanel = (ViewGroup) XposedHelpers.getObjectField(mNotificationPanelView, "mQsPanel");
            mNotificationStackScroller = (ViewGroup) XposedHelpers.getObjectField(mNotificationPanelView, "mNotificationStackScroller");
        }
    };

    private static final XC_MethodReplacement onQsExpansionStarted = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            methodCancelQsAnimation.invoke(mNotificationPanelView);
            methodCancelHeightAnimator.invoke(mNotificationPanelView);
            float height = fieldQsExpansionHeight.getFloat(mNotificationPanelView) - (int) param.args[0];
            methodSetQsExpansion.invoke(mNotificationPanelView, height);
            methodRequestPanelHeightUpdate.invoke(mNotificationPanelView);
            return null;
        }
    };

    private static final XC_MethodReplacement setVerticalPanelTranslation = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            float translation = (float) param.args[0];
            mNotificationStackScroller.setTranslationX(translation);
            mScrollView.setTranslationX(translation);
            return null;
        }
    };

    private static final XC_MethodHook getTempQsMaxExpansionHook = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            param.setResult(fieldQsMaxExpansionHeight.getInt(mNotificationPanelView));
        }
    };

    private static final XC_MethodHook onExpandingStartedHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            XposedHelpers.callMethod(mHeader, "setListening", true);
        }
    };

    private static final XC_MethodReplacement onClick = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            if (((View) param.args[0]).getId() == R.id.statusbar_header_expand_indicator) {
                methodOnQsExpansionStarted.invoke(mNotificationPanelView);
                if (fieldQsExpanded.getBoolean(mNotificationPanelView)) {
                    XposedHelpers.callMethod(mNotificationPanelView, "flingSettings", 0, false, null, true);
                } else if (fieldQsExpansionEnabled.getBoolean(mNotificationPanelView)) {
                    XposedHelpers.callMethod(mNotificationPanelView, "flingSettings", 0, true, null, true);
                }
            }
            return null;
        }
    };

    private static final XC_MethodHook shouldQuickSettingsInterceptHook = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            float x = (float) param.args[0];
            float y = (float) param.args[1];
            float yDiff = (float) param.args[2];
            if (!fieldQsExpansionEnabled.getBoolean(mNotificationPanelView) || XposedHelpers.getBooleanField(mNotificationPanelView, "mCollapsedOnDown")) {
                param.setResult(false);
            }
            View header = fieldKeyguardShowing.getBoolean(mNotificationPanelView) ? (View) XposedHelpers.getObjectField(mNotificationPanelView, "mKeyguardStatusBar") : (View) fieldHeader.get(mNotificationPanelView);
            View mScrollView = (View) (XposedHelpers.getObjectField(mNotificationPanelView, "mScrollView"));
            boolean onHeader = x >= mScrollView.getX()
                    && x <= mScrollView.getX() + mScrollView.getWidth()
                    && y >= header.getTop() && y <= header.getBottom();
            if (fieldQsExpanded.getBoolean(mNotificationPanelView)) {
                param.setResult(onHeader || (yDiff < 0 && (boolean) XposedHelpers.callMethod(mNotificationPanelView, "isInQsArea", x, y)));
            } else {
                param.setResult(onHeader);
            }
        }
    };

    private static final XC_MethodReplacement updateQsState = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            boolean mQsExpanded = fieldQsExpanded.getBoolean(mNotificationPanelView);
            boolean mStackScrollerOverscrolling = fieldStackScrollerOverscrolling.getBoolean(mNotificationPanelView);
            boolean mHeaderAnimating = fieldHeaderAnimating.getBoolean(mNotificationPanelView);
            boolean mKeyguardShowing = fieldKeyguardShowing.getBoolean(mNotificationPanelView);
            boolean mQsExpansionFromOverscroll = fieldQsExpansionFromOverscroll.getBoolean(mNotificationPanelView);
            boolean mQsScrimEnabled = fieldQsScrimEnabled.getBoolean(mNotificationPanelView);
            Object mKeyguardUserSwitcher = XposedHelpers.getObjectField(mNotificationPanelView, "mKeyguardUserSwitcher");
            int mStatusBarState = XposedHelpers.getIntField(mNotificationPanelView, "mStatusBarState");
            View mQsNavbarScrim = (View) XposedHelpers.getObjectField(mNotificationPanelView, "mQsNavbarScrim");
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
            fieldDelegateToScrollView.setBoolean(mNotificationStackScroller, false);
            fieldOnlyScrollingInThisMotion.setBoolean(mNotificationStackScroller, false);
        }
    };

    private static final XC_MethodHook onInterceptTouchEventHook = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            fieldInterceptDelegateEnabled.setBoolean(mNotificationStackScroller, false);
            fieldOnlyScrollingInThisMotion.setBoolean(mNotificationStackScroller, false);
        }
    };

    private static final XC_MethodReplacement isScrolledToBottom = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            Object mStatusBar = fieldStatusBar.get(mNotificationPanelView);
            int getBarState = (int) XposedHelpers.callMethod(mStatusBar, "getBarState");
            boolean isScrolledToBottom = (boolean) methodIsScrolledToBottom.invoke(mNotificationStackScroller);
            boolean isInSettings = (boolean) XposedHelpers.callMethod(mNotificationPanelView, "isInSettings");
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
            MotionEvent ev = (MotionEvent) param.args[0];
            boolean mIsBeingDragged = XposedHelpers.getBooleanField(mNotificationStackScroller, "mIsBeingDragged");
            if (ev.getY() < ((int) XposedHelpers.callMethod(mQsContainer, "getBottom")) && !mIsBeingDragged) {
                param.setResult(false);
            }
        }
    };

    private static final XC_MethodReplacement getHeaderTranslation = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            int mQsMinExpansionHeight = fieldQsMinExpansionHeight.getInt(mNotificationPanelView);
            float mExpandedHeight = fieldExpandedHeight.getFloat(mNotificationPanelView);
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
            Object mStatusBar = fieldStatusBar.get(mNotificationPanelView);
            int min = XposedHelpers.getIntField(mNotificationPanelView, "mStatusBarMinHeight");
            int mQsMinExpansionHeight = fieldQsMinExpansionHeight.getInt(mNotificationPanelView);
            int panelHeightQsExpanded = (int) XposedHelpers.callMethod(mNotificationPanelView, "calculatePanelHeightQsExpanded");
            boolean mQsExpandImmediate = fieldQsExpandImmediate.getBoolean(mNotificationPanelView);
            boolean mQsExpanded = fieldQsExpanded.getBoolean(mNotificationPanelView);
            boolean mIsExpanding = fieldIsExpanding.getBoolean(mNotificationPanelView);
            boolean mQsExpandedWhenExpandingStarted = fieldQsExpandedWhenExpandingStarted.getBoolean(mNotificationPanelView);
            if ((int) XposedHelpers.callMethod(mStatusBar, "getBarState") != STATE_KEYGUARD
                    && (int) methodGetNotGoneChildCount.invoke(mNotificationStackScroller) == 0) {
                int minHeight = (int) ((mQsMinExpansionHeight + (float) XposedHelpers.callMethod(mNotificationPanelView, "getOverExpansionAmount")));
                min = Math.max(min, minHeight);
            }
            int maxHeight;
            if (mQsExpandImmediate || mQsExpanded || mIsExpanding && mQsExpandedWhenExpandingStarted) {
                maxHeight = panelHeightQsExpanded;
            } else {
                maxHeight = (int) XposedHelpers.callMethod(mNotificationPanelView, "calculatePanelHeightShade");
            }
            maxHeight = Math.max(maxHeight, min);
            return maxHeight;
        }
    };

    private static final XC_MethodHook getPeekHeightHook = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            int mQsMinExpansionHeight = fieldQsMinExpansionHeight.getInt(mNotificationPanelView);
            if (!((int) methodGetNotGoneChildCount.invoke(mNotificationStackScroller) > 0))
                param.setResult(mQsMinExpansionHeight);
        }
    };

    private static final XC_MethodReplacement setStackHeight = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            float height = (float) param.args[0];
            XposedHelpers.setFloatField(mNotificationStackScroller, "mLastSetStackHeight", height);
            XposedHelpers.callMethod(mNotificationStackScroller, "setIsExpanded", height > 0.0f);
            int stackHeight;
            int mCurrentStackHeight = XposedHelpers.getIntField(mNotificationStackScroller, "mCurrentStackHeight");

            float translationY;
            float appearEndPosition = getAppearEndPosition();
            float appearStartPosition = getAppearStartPosition();
            if (height >= appearEndPosition) {
                translationY = fieldTopPaddingOverflow.getFloat(mNotificationStackScroller);
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
            return null;
        }
    };

    private static final XC_MethodReplacement getPeekHeightStackScroller = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            final Object firstChild = methodGetFirstChildNotGone.invoke(mNotificationStackScroller);
            final int firstChildMinHeight = firstChild != null ? (int) XposedHelpers.callMethod(firstChild, "getMinHeight")
                    : fieldCollapsedSize.getInt(mNotificationStackScroller);
            int mIntrinsicPadding = fieldIntrinsicPadding.getInt(mNotificationStackScroller);
            int mBottomStackSlowDownHeight = fieldBottomStackSlowDownHeight.getInt(mNotificationStackScroller);
            int mBottomStackPeekSize = fieldBottomStackPeekSize.getInt(mNotificationStackScroller);
            return mIntrinsicPadding + firstChildMinHeight + mBottomStackPeekSize
                    + mBottomStackSlowDownHeight;
        }
    };

    private static final XC_MethodHook onExpandingFinishedHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            fieldScrollYOverride.setInt(mNotificationPanelView, 0);
        }
    };

    private static final XC_MethodHook onQsTouchHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            fieldScrollYOverride.setInt(mNotificationPanelView, 0);
        }
    };

    private static final XC_MethodHook flingSettingsHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            fieldScrollYOverride.setInt(mNotificationPanelView, 0);
        }
    };

    private static final XC_MethodReplacement onScrollChanged = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            boolean mQsExpanded = fieldQsExpanded.getBoolean(mNotificationPanelView);
            boolean mQsFullyExpanded = fieldQsFullyExpanded.getBoolean(mNotificationPanelView);
            fieldQsMaxExpansionHeight.setInt(mNotificationPanelView, (int) XposedHelpers.callMethod(mQsContainer, "getDesiredHeight"));
            if (mQsExpanded && mQsFullyExpanded) {
                fieldQsExpansionHeight.setFloat(mNotificationPanelView, fieldQsMaxExpansionHeight.getInt(mNotificationPanelView));
                XposedHelpers.callMethod(mNotificationPanelView, "requestScrollerTopPaddingUpdate", false /* animate */);
                XposedHelpers.callMethod(mNotificationPanelView, "requestPanelHeightUpdate");
            }
            return null;
        }
    };

    private static final XC_MethodReplacement onHeightUpdated = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            float expandedHeight = (float) param.args[0];
            boolean mQsExpanded = fieldQsExpanded.getBoolean(mNotificationPanelView);
            boolean mQsExpandImmediate = fieldQsExpandImmediate.getBoolean(mNotificationPanelView);
            boolean mIsExpanding = fieldIsExpanding.getBoolean(mNotificationPanelView);
            boolean mQsExpandedWhenExpandingStarted = fieldQsExpandedWhenExpandingStarted.getBoolean(mNotificationPanelView);
            boolean mQsTracking = XposedHelpers.getBooleanField(mNotificationPanelView, "mQsTracking");
            boolean mQsExpansionFromOverscroll = fieldQsExpansionFromOverscroll.getBoolean(mNotificationPanelView);
            boolean isFullyCollapsed = (boolean) XposedHelpers.callMethod(mNotificationPanelView, "isFullyCollapsed");
            int mQsMinExpansionHeight = fieldQsMinExpansionHeight.getInt(mNotificationPanelView);
            Object mQsExpansionAnimator = XposedHelpers.getObjectField(mNotificationPanelView, "mQsExpansionAnimator");

            if (!mQsExpanded || mQsExpandImmediate || mIsExpanding && mQsExpandedWhenExpandingStarted) {
                XposedHelpers.callMethod(mNotificationPanelView, "positionClockAndNotifications");
            }
            if (mQsExpandImmediate || mQsExpanded && !mQsTracking && mQsExpansionAnimator == null
                    && !mQsExpansionFromOverscroll) {
                float t;
                if (isOnKeyguard()) {
                    // On Keyguard, interpolate the QS expansion linearly to the panel expansion
                    t = expandedHeight / (int) methodGetMaxPanelHeight.invoke(mNotificationPanelView);
                } else {
                    // In Shade, interpolate linearly such that QS is closed whenever panel height is
                    // minimum QS expansion + minStackHeight
                    float panelHeightQsCollapsed = (int) XposedHelpers.callMethod(mNotificationStackScroller, "getIntrinsicPadding")
                            + getLayoutMinHeight();
                    float panelHeightQsExpanded = (int) XposedHelpers.callMethod(mNotificationPanelView, "calculatePanelHeightQsExpanded");
                    t = (expandedHeight - panelHeightQsCollapsed)
                            / (panelHeightQsExpanded - panelHeightQsCollapsed);
                }
                methodSetQsExpansion.invoke(mNotificationPanelView, mQsMinExpansionHeight
                        + t * ((int) XposedHelpers.callMethod(mNotificationPanelView, "getTempQsMaxExpansion") - mQsMinExpansionHeight));
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
            int mQsMaxExpansionHeight = fieldQsMaxExpansionHeight.getInt(mNotificationPanelView);
            int mTopPaddingAdjustment = fieldTopPaddingAdjustment.getInt(mNotificationPanelView);
            Object mQsSizeChangeAnimator = XposedHelpers.getObjectField(mNotificationPanelView, "mQsSizeChangeAnimator");
            Object mClockPositionResult = fieldClockPositionResult.get(mNotificationPanelView);
            float notificationHeight = (int) XposedHelpers.callMethod(mNotificationStackScroller, "getHeight")
                    - (int) XposedHelpers.callMethod(mNotificationStackScroller, "getEmptyBottomMargin")
                    - (int) XposedHelpers.callMethod(mNotificationStackScroller, "getTopPadding");

            // When only empty shade view is visible in QS collapsed state, simulate that we would have
            // it in expanded QS state as well so we don't run into troubles when fading the view in/out
            // and expanding/collapsing the whole panel from/to quick settings.
            if ((int) methodGetNotGoneChildCount.invoke(mNotificationStackScroller) == 0
                    && mShadeEmpty) {
                notificationHeight = (int) XposedHelpers.callMethod(mNotificationStackScroller, "getEmptyShadeViewHeight")
                        + (int) XposedHelpers.callMethod(mNotificationStackScroller, "getBottomStackPeekSize")
                        + fieldBottomStackSlowDownHeight.getInt(mNotificationStackScroller);
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
                    + notificationHeight + (float) XposedHelpers.callMethod(mNotificationStackScroller, "getTopPaddingOverflow");

            if (totalHeight > mNotificationStackScroller.getHeight()) {
                float fullyCollapsedHeight = maxQsHeight
                        + getLayoutMinHeight();
                totalHeight = Math.max(fullyCollapsedHeight, (int) XposedHelpers.callMethod(mNotificationStackScroller, "getHeight"));
            }
            return (int) totalHeight;
        }
    };

    private static final XC_MethodReplacement getFadeoutAlpha = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            int mQsMinExpansionHeight = fieldQsMinExpansionHeight.getInt(mNotificationPanelView);
            float alpha = ((float) XposedHelpers.callMethod(mNotificationPanelView, "getNotificationsTopY") + getFirstItemMinHeight())
                    / (mQsMinExpansionHeight + (int) XposedHelpers.callMethod(mNotificationStackScroller, "getBottomStackPeekSize")
                    - fieldBottomStackSlowDownHeight.getInt(mNotificationStackScroller));
            alpha = Math.max(0, Math.min(alpha, 1));
            alpha = (float) Math.pow(alpha, 0.75);
            return alpha;
        }
    };

    private static final XC_MethodReplacement getMinStackHeight = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            int mBottomStackPeekSize = fieldBottomStackPeekSize.getInt(mNotificationStackScroller);
            int mBottomStackSlowDownHeight = fieldBottomStackSlowDownHeight.getInt(mNotificationStackScroller);
            int mMaxLayoutHeight = fieldMaxLayoutHeight.getInt(mNotificationStackScroller);
            int mIntrinsicPadding = fieldIntrinsicPadding.getInt(mNotificationStackScroller);
            int firstChildMinHeight = getFirstChildIntrinsicHeight();
            return Math.min(firstChildMinHeight + mBottomStackPeekSize + mBottomStackSlowDownHeight,
                    mMaxLayoutHeight - mIntrinsicPadding);
        }
    };

    private static final XC_MethodReplacement getEmptyBottomMargin = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            int mMaxLayoutHeight = fieldMaxLayoutHeight.getInt(mNotificationStackScroller);
            int mContentHeight = XposedHelpers.getIntField(mNotificationStackScroller, "mContentHeight");
            int mBottomStackPeekSize = fieldBottomStackPeekSize.getInt(mNotificationStackScroller);
            int mBottomStackSlowDownHeight = fieldBottomStackSlowDownHeight.getInt(mNotificationStackScroller);
            int emptyMargin = mMaxLayoutHeight - mContentHeight - mBottomStackPeekSize
                    - mBottomStackSlowDownHeight;
            return Math.max(emptyMargin, 0);
        }
    };

    private static final XC_MethodReplacement getDismissViewHeight = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            Object mDismissView = XposedHelpers.getObjectField(mNotificationStackScroller, "mDismissView");
            int mPaddingBetweenElements = fieldPaddingBetweenElements.getInt(mNotificationStackScroller);
            return (int) XposedHelpers.callMethod(mDismissView, "getHeight") + mPaddingBetweenElements;
        }
    };

    private static final XC_MethodReplacement positionClockAndNotifications = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {

            Object mClockPositionAlgorithm = XposedHelpers.getObjectField(mNotificationPanelView, "mClockPositionAlgorithm");
            Object mStatusBar = fieldStatusBar.get(mNotificationPanelView);
            Object mKeyguardStatusView = XposedHelpers.getObjectField(mNotificationPanelView, "mKeyguardStatusView");
            Object mClockPositionResult = fieldClockPositionResult.get(mNotificationPanelView);
            Object mClockAnimator = XposedHelpers.getObjectField(mNotificationPanelView, "mClockAnimator");
            boolean animate = (boolean) XposedHelpers.callMethod(mNotificationStackScroller, "isAddOrRemoveAnimationPending");
            int stackScrollerPadding;
            if (getStatusBarState() != STATE_KEYGUARD) {
                stackScrollerPadding = (int) XposedHelpers.callMethod(mHeader, "getCollapsedHeight") + XposedHelpers.getIntField(mNotificationPanelView, "mQsPeekHeight");
                fieldTopPaddingAdjustment.setInt(mNotificationPanelView, 0);
            } else {
                try {
                    XposedHelpers.callMethod(mClockPositionAlgorithm, "setup",
                            XposedHelpers.callMethod(mStatusBar, "getMaxKeyguardNotifications"),
                            methodGetMaxPanelHeight.invoke(mNotificationPanelView),
                            methodGetExpandedHeight.invoke(mNotificationPanelView),
                            methodGetNotGoneChildCount.invoke(mNotificationStackScroller),
                            XposedHelpers.callMethod(mNotificationPanelView, "getHeight"),
                            XposedHelpers.callMethod(mKeyguardStatusView, "getHeight"),
                            XposedHelpers.getFloatField(mNotificationPanelView, "mEmptyDragAmount"));
                } catch (NoSuchMethodError e) {//Xperia
                    XposedHelpers.callMethod(mClockPositionAlgorithm, "setup",
                            XposedHelpers.callMethod(mStatusBar, "getMaxKeyguardNotifications"),
                            methodGetMaxPanelHeight.invoke(mNotificationPanelView),
                            methodGetExpandedHeight.invoke(mNotificationPanelView),
                            methodGetNotGoneChildCount.invoke(mNotificationStackScroller),
                            XposedHelpers.callMethod(mNotificationPanelView, "getHeight"),
                            XposedHelpers.callMethod(mKeyguardStatusView, "getHeight"),
                            XposedHelpers.getFloatField(mNotificationPanelView, "mEmptyDragAmount"),
                            0,
                            0);
                }
                XposedHelpers.callMethod(mClockPositionAlgorithm, "run", mClockPositionResult);
                if (animate || mClockAnimator != null) {
                    XposedHelpers.callMethod(mNotificationPanelView, "startClockAnimation", XposedHelpers.getIntField(mClockPositionResult, "clockY"));
                } else {
                    XposedHelpers.callMethod(mKeyguardStatusView, "setY", XposedHelpers.getIntField(mClockPositionResult, "clockY"));
                }
                XposedHelpers.callMethod(mNotificationPanelView, "updateClock", XposedHelpers.getFloatField(mClockPositionResult, "clockAlpha"), XposedHelpers.getFloatField(mClockPositionResult, "clockScale"));
                stackScrollerPadding = XposedHelpers.getIntField(mClockPositionResult, "stackScrollerPadding");
                fieldTopPaddingAdjustment.setInt(mNotificationPanelView, XposedHelpers.getIntField(mClockPositionResult, "stackScrollerPaddingAdjustment"));
            }
            XposedHelpers.callMethod(mNotificationStackScroller, "setIntrinsicPadding", stackScrollerPadding);
            XposedHelpers.callMethod(mNotificationPanelView, "requestScrollerTopPaddingUpdate", animate);
            return null;
        }
    };

    private static final XC_MethodReplacement setQsExpansion = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            int mQsMinExpansionHeight = fieldQsMinExpansionHeight.getInt(mNotificationPanelView);
            int mQsMaxExpansionHeight = fieldQsMaxExpansionHeight.getInt(mNotificationPanelView);
            Object mQsNavbarScrim = XposedHelpers.getObjectField(mNotificationPanelView, "mQsNavbarScrim");
            boolean mQsExpanded = fieldQsExpanded.getBoolean(mNotificationPanelView);
            boolean mStackScrollerOverscrolling = fieldStackScrollerOverscrolling.getBoolean(mNotificationPanelView);
            boolean mLastAnnouncementWasQuickSettings = XposedHelpers.getBooleanField(mNotificationPanelView, "mLastAnnouncementWasQuickSettings");
            boolean mTracking = XposedHelpers.getBooleanField(mNotificationPanelView, "mTracking");
            boolean isCollapsing = (boolean) XposedHelpers.callMethod(mNotificationPanelView, "isCollapsing");
            boolean mQsScrimEnabled = fieldQsScrimEnabled.getBoolean(mNotificationPanelView);
            float height = (float) param.args[0];
            height = Math.min(Math.max(height, mQsMinExpansionHeight), mQsMaxExpansionHeight);
            fieldQsFullyExpanded.setBoolean(mNotificationPanelView, height == mQsMaxExpansionHeight && mQsMaxExpansionHeight != 0);
            if (height > mQsMinExpansionHeight && !mQsExpanded && !mStackScrollerOverscrolling) {
                methodSetQsExpanded.invoke(mNotificationPanelView, true);
            } else if (height <= mQsMinExpansionHeight && mQsExpanded) {
                methodSetQsExpanded.invoke(mNotificationPanelView, false);
                if (mLastAnnouncementWasQuickSettings && !mTracking && !isCollapsing) {
                    XposedHelpers.callMethod(mNotificationPanelView, "announceForAccessibility", (boolean) XposedHelpers.callMethod(mNotificationPanelView, "getKeyguardOrLockScreenString"));
                    XposedHelpers.setBooleanField(mNotificationPanelView, "mLastAnnouncementWasQuickSettings", false);
                }
            }
            fieldQsExpansionHeight.setFloat(mNotificationPanelView, height);
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
                XposedHelpers.callMethod(mQsNavbarScrim, "setAlpha", methodGetQsExpansionFraction.invoke(mNotificationPanelView));
            }
            return null;
        }
    };

    private static final XC_MethodReplacement updateHeader = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            if (getStatusBarState() == STATE_KEYGUARD)
                XposedHelpers.callMethod(mNotificationPanelView, "updateHeaderKeyguardAlpha");
            updateQsExpansion();
            return null;
        }
    };

    private static final XC_MethodHook calculateQsTopPaddingHook = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            fieldScrollYOverride.setInt(mNotificationPanelView, -1);
        }
    };

    private static final XC_MethodHook updateChildrenHook = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            updateScrollStateForAddedChildren();
        }
    };

    private static final XC_MethodReplacement updateSpeedBumpIndex = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            int newIndex = (int) param.args[0];
            Object mAmbientState = XposedHelpers.getObjectField(mNotificationStackScroller, "mAmbientState");
            XposedHelpers.callMethod(mAmbientState, "setSpeedBumpIndex", newIndex);
            return null;
        }
    };

    private static void updateQsExpansion() throws IllegalAccessException, InvocationTargetException {
        QSContainerHelper.setQsExpansion((float) methodGetQsExpansionFraction.invoke(mNotificationPanelView), (float) methodGetHeaderTranslation.invoke(mNotificationPanelView));
    }

    private static int getFirstItemMinHeight() throws IllegalAccessException, InvocationTargetException {
        final Object firstChild = methodGetFirstChildNotGone.invoke(mNotificationStackScroller);
        int mCollapsedSize = fieldCollapsedSize.getInt(mNotificationStackScroller);
        return firstChild != null ? (int) XposedHelpers.callMethod(firstChild, "getMinHeight") : mCollapsedSize;
    }

    private static float getExpandTranslationStart() throws IllegalAccessException, InvocationTargetException {
        Object mHeadsUpManager = fieldHeadsUpManager.get(mNotificationStackScroller);
        int mMaxLayoutHeight = fieldMaxLayoutHeight.getInt(mNotificationStackScroller);
        int mIntrinsicPadding = fieldIntrinsicPadding.getInt(mNotificationStackScroller);
        int mBottomStackSlowDownHeight = fieldBottomStackSlowDownHeight.getInt(mNotificationStackScroller);
        int mBottomStackPeekSize = fieldBottomStackPeekSize.getInt(mNotificationStackScroller);
        int mTopPadding = fieldTopPadding.getInt(mNotificationStackScroller);
        int startPosition = 0;
        if (!fieldTrackingHeadsUp.getBoolean(mNotificationStackScroller) && !(boolean) methodHasPinnedHeadsUp.invoke(mHeadsUpManager)) {
            startPosition = -Math.min(getFirstChildIntrinsicHeight(),
                    mMaxLayoutHeight - mIntrinsicPadding - mBottomStackSlowDownHeight
                            - mBottomStackPeekSize);
        }
        return startPosition - mTopPadding;
    }

    /**
     * @return the position from where the appear transition starts when expanding.
     * Measured in absolute height.
     */
    private static float getAppearStartPosition() throws IllegalAccessException, InvocationTargetException {
        Object mHeadsUpManager = fieldHeadsUpManager.get(mNotificationStackScroller);
        boolean trackingHeadsUp = fieldTrackingHeadsUp.getBoolean(mNotificationStackScroller) || (boolean) methodHasPinnedHeadsUp.invoke(mHeadsUpManager);
        return trackingHeadsUp
                ? (int) methodGetTopHeadsUpHeight.invoke(mHeadsUpManager)
                : 0;
    }

    /**
     * @return the position from where the appear transition ends when expanding.
     * Measured in absolute height.
     */
    private static float getAppearEndPosition() throws IllegalAccessException, InvocationTargetException {
        Object mHeadsUpManager = fieldHeadsUpManager.get(mNotificationStackScroller);
        boolean trackingHeadsUp = fieldTrackingHeadsUp.getBoolean(mNotificationStackScroller) || (boolean) methodHasPinnedHeadsUp.invoke(mHeadsUpManager);
        int mBottomStackPeekSize = fieldBottomStackPeekSize.getInt(mNotificationStackScroller);
        int mBottomStackSlowDownHeight = fieldBottomStackSlowDownHeight.getInt(mNotificationStackScroller);
        int mTopPadding = fieldTopPadding.getInt(mNotificationStackScroller);
        int mIntrinsicPadding = fieldIntrinsicPadding.getInt(mNotificationStackScroller);
        int firstItemHeight = trackingHeadsUp
                ? (int) methodGetTopHeadsUpHeight.invoke(mHeadsUpManager) + mBottomStackPeekSize
                + mBottomStackSlowDownHeight
                : getLayoutMinHeight();
        return firstItemHeight + (isOnKeyguard() ? mTopPadding : mIntrinsicPadding);
    }

    public static int getLayoutMinHeight() throws IllegalAccessException, InvocationTargetException {
        int firstChildMinHeight = getFirstChildIntrinsicHeight();
        int mBottomStackPeekSize = fieldBottomStackPeekSize.getInt(mNotificationStackScroller);
        int mBottomStackSlowDownHeight = fieldBottomStackSlowDownHeight.getInt(mNotificationStackScroller);
        int mIntrinsicPadding = fieldIntrinsicPadding.getInt(mNotificationStackScroller);
        int mMaxLayoutHeight = fieldMaxLayoutHeight.getInt(mNotificationStackScroller);
        return Math.min(firstChildMinHeight + mBottomStackPeekSize + mBottomStackSlowDownHeight,
                mMaxLayoutHeight - mIntrinsicPadding);
    }

    private static int getFirstChildIntrinsicHeight() throws IllegalAccessException, InvocationTargetException {
        final Object firstChild = methodGetFirstChildNotGone.invoke(mNotificationStackScroller);
        final Object mEmptyShadeView = XposedHelpers.getObjectField(mNotificationStackScroller, "mEmptyShadeView");
        int mCollapsedSize = fieldCollapsedSize.getInt(mNotificationStackScroller);
        int mOwnScrollY = fieldOwnScrollY.getInt(mNotificationStackScroller);
        int firstChildMinHeight = firstChild != null
                ? (int) XposedHelpers.callMethod(firstChild, "getIntrinsicHeight")
                : mEmptyShadeView != null
                ? (int) XposedHelpers.callMethod(mEmptyShadeView, "getMinHeight")
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
    private static float getAppearFraction(float height) throws IllegalAccessException, InvocationTargetException {
        float appearEndPosition = getAppearEndPosition();
        float appearStartPosition = getAppearStartPosition();
        return (height - appearStartPosition)
                / (appearEndPosition - appearStartPosition);
    }

    private static void updateScrollStateForAddedChildren() throws IllegalAccessException, InvocationTargetException {
        int mPaddingBetweenElements = fieldPaddingBetweenElements.getInt(mNotificationStackScroller);
        int mOwnScrollY = fieldOwnScrollY.getInt(mNotificationStackScroller);
        ArrayList<View> mChildrenToAddAnimated = (ArrayList<View>) XposedHelpers.getObjectField(mNotificationStackScroller, "mChildrenToAddAnimated");
        if (mChildrenToAddAnimated.isEmpty()) {
            return;
        }
        for (int i = 0; i < (int) methodGetChildCount.invoke(mNotificationStackScroller); i++) {
            View child = (View) methodGetChildAt.invoke(mNotificationStackScroller, i);
            if (mChildrenToAddAnimated.contains(child)) {
                int startingPosition = (int) methodGetPositionInLinearLayout.invoke(mNotificationStackScroller, child);
                int padding = mPaddingBetweenElements;
                int childHeight = (int) methodGetIntrinsicHeight.invoke(mNotificationStackScroller, child) + padding;
                if (startingPosition < mOwnScrollY) {
                    // This child starts off screen, so let's keep it offscreen to keep the others visible

                    fieldOwnScrollY.setInt(mNotificationStackScroller, mOwnScrollY + childHeight);
                }
            }
        }
        methodClampScrollPosition.invoke(mNotificationStackScroller);
    }
}
