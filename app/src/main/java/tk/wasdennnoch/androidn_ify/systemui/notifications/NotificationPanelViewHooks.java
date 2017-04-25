package tk.wasdennnoch.androidn_ify.systemui.notifications;

import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Objects;

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

/**
 * Created by victo on 4/20/2017.
 */

public class NotificationPanelViewHooks {

    private static final String TAG = "NotificationPanelViewHooks";

    private static final String PACKAGE_SYSTEMUI = XposedHook.PACKAGE_SYSTEMUI;
    private static final String CLASS_NOTIFICATION_STACK_SCROLL_LAYOUT = "com.android.systemui.statusbar.stack.NotificationStackScrollLayout";
    private static final String CLASS_NOTIFICATION_PANEL_VIEW = "com.android.systemui.statusbar.phone.NotificationPanelView";
    private static final String CLASS_QS_CONTAINER = "com.android.systemui.qs.QSContainer";
    private static final String CLASS_PANEL_VIEW = "com.android.systemui.statusbar.phone.PanelView";
    private static final String CLASS_OBSERVABLE_SCROLL_VIEW = "com.android.systemui.statusbar.phone.ObservableScrollView";

    public static final int STATE_SHADE = 0;
    public static final int STATE_KEYGUARD = 1;
    public static final int STATE_SHADE_LOCKED = 2;

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

    private static Field fieldBottomStackSlowDownHeight;
    private static Field fieldBottomStackPeekSize;
    private static Field fieldMaxLayoutHeight;
    private static Field fieldIntrinsicPadding;

    public static void hook(ClassLoader classLoader){

        Class<?> classNotificationStackScrollLayout = XposedHelpers.findClass(CLASS_NOTIFICATION_STACK_SCROLL_LAYOUT, classLoader);
        Class<?> classNotificationPanelView = XposedHelpers.findClass(CLASS_NOTIFICATION_PANEL_VIEW, classLoader);
        Class<?> classQSContainer = XposedHelpers.findClass(CLASS_QS_CONTAINER, classLoader);
        Class<?> classPanelView = XposedHelpers.findClass(CLASS_PANEL_VIEW, classLoader);
        Class<?> classObservableScrollView = XposedHelpers.findClass(CLASS_OBSERVABLE_SCROLL_VIEW, classLoader);

        methodCancelQsAnimation = XposedHelpers.findMethodBestMatch(classNotificationPanelView, "cancelQsAnimation");
        methodCancelHeightAnimator = XposedHelpers.findMethodBestMatch(classNotificationPanelView, "cancelHeightAnimator");
        methodSetQsExpansion = XposedHelpers.findMethodBestMatch(classNotificationPanelView, "setQsExpansion", float.class);
        methodRequestPanelHeightUpdate = XposedHelpers.findMethodBestMatch(classNotificationPanelView, "requestPanelHeightUpdate");
        methodOnQsExpansionStarted = XposedHelpers.findMethodBestMatch(classNotificationPanelView, "onQsExpansionStarted");

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

        fieldBottomStackSlowDownHeight = XposedHelpers.findField(classNotificationStackScrollLayout, "mBottomStackSlowDownHeight");
        fieldBottomStackPeekSize = XposedHelpers.findField(classNotificationStackScrollLayout, "mBottomStackPeekSize");
        fieldMaxLayoutHeight = XposedHelpers.findField(classNotificationStackScrollLayout, "mMaxLayoutHeight");
        fieldIntrinsicPadding = XposedHelpers.findField(classNotificationStackScrollLayout, "mIntrinsicPadding");

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

        //XposedHelpers.findAndHookMethod(classNotificationStackScrollLayout, "onViewAddedInternal", View.class, onViewAddedInternal);

        XposedHelpers.findAndHookMethod(classObservableScrollView, "overScrollBy", int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, boolean.class, XC_MethodReplacement.returnConstant(false));
        XposedHelpers.findAndHookMethod(classObservableScrollView, "fling", int.class, XC_MethodReplacement.DO_NOTHING);
        XposedHelpers.findAndHookMethod(classObservableScrollView, "getMaxScrollY", XC_MethodReplacement.DO_NOTHING);
        XposedHelpers.findAndHookMethod(classObservableScrollView, "isScrolledToBottom", XC_MethodReplacement.DO_NOTHING);
        XposedHelpers.findAndHookMethod(classObservableScrollView, "setBlockFlinging", boolean.class, XC_MethodReplacement.DO_NOTHING);
        XposedHelpers.findAndHookMethod(classObservableScrollView, "onTouchEvent", MotionEvent.class, XC_MethodReplacement.returnConstant(false));
    }

    private static final XC_MethodHook onFinishInflateHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            mNotificationPanelView = (ViewGroup) param.thisObject;
            mHeader = (ViewGroup) XposedHelpers.getObjectField(mNotificationPanelView, "mHeader");
            mScrollView = (ViewGroup) XposedHelpers.getObjectField(mNotificationPanelView, "mScrollView");
            mQsContainer = (ViewGroup) XposedHelpers.getObjectField(mNotificationPanelView, "mQsContainer");
            mQsPanel = (ViewGroup) XposedHelpers.getObjectField(mNotificationPanelView, "mQsPanel");
            mNotificationStackScroller = (ViewGroup) XposedHelpers.getObjectField(mNotificationPanelView, "mNotificationStackScroller");
        }
    };

    /**
     * START
     */
    private static final XC_MethodReplacement onQsExpansionStarted = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            methodCancelQsAnimation.invoke(mNotificationPanelView);
            methodCancelHeightAnimator.invoke(mNotificationPanelView);
            float height = fieldQsExpansionHeight.getFloat(mNotificationPanelView) - (int)param.args[0];
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
            XposedHelpers.callMethod(mHeader,"setListening", true);
        }
    };

    private static final XC_MethodReplacement onClick = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            if(((View)param.args[0]).getId() == R.id.statusbar_header_expand_indicator) {
                methodOnQsExpansionStarted.invoke(mNotificationPanelView);
                if (fieldQsExpanded.getBoolean(mNotificationPanelView)) {
                    XposedHelpers.callMethod(mNotificationPanelView, "flingSettings", 0, false, null, true);
                }
                else if (XposedHelpers.getBooleanField(mNotificationPanelView, "mQsExpansionEnabled")) {
                    XposedHelpers.callMethod(mNotificationPanelView, "flingSettings", 0, true, null, true);
                }
            }
            return null;
        }
    };

    private static final XC_MethodHook shouldQuickSettingsInterceptHook = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            float x = (float)param.args[0];
            float y = (float)param.args[1];
            float yDiff = (float)param.args[2];
            if (!XposedHelpers.getBooleanField(mNotificationPanelView,"mQsExpansionEnabled") || XposedHelpers.getBooleanField(mNotificationPanelView,"mCollapsedOnDown")) {
                param.setResult(false);
            }
            View header = fieldKeyguardShowing.getBoolean(mNotificationPanelView) ? (View)XposedHelpers.getObjectField(mNotificationPanelView,"mKeyguardStatusBar") : (View)XposedHelpers.getObjectField(mNotificationPanelView, "mHeader");
            View mScrollView = (View)(XposedHelpers.getObjectField(mNotificationPanelView, "mScrollView"));
            boolean onHeader = x >= mScrollView.getX()
                    && x <= mScrollView.getX() + mScrollView.getWidth()
                    && y >= header.getTop() && y <= header.getBottom();
            if (fieldQsExpanded.getBoolean(mNotificationPanelView)) {
                param.setResult(onHeader || (yDiff < 0 && (boolean)XposedHelpers.callMethod(mNotificationPanelView,"isInQsArea", x, y)));
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
            int mQsMinExpansionHeight = fieldQsMinExpansionHeight.getInt(mNotificationPanelView);
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
            int mQsMinExpansionHeight = fieldQsMinExpansionHeight.getInt(mNotificationPanelView);
            int panelHeightQsExpanded = (int)XposedHelpers.callMethod(mNotificationPanelView, "calculatePanelHeightQsExpanded");
            boolean mQsExpandImmediate = fieldQsExpandImmediate.getBoolean(mNotificationPanelView);
            boolean mQsExpanded = fieldQsExpanded.getBoolean(mNotificationPanelView);
            boolean mIsExpanding = fieldIsExpanding.getBoolean(mNotificationPanelView);
            boolean mQsExpandedWhenExpandingStarted = fieldQsExpandedWhenExpandingStarted.getBoolean(mNotificationPanelView);
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

    private static final XC_MethodHook getPeekHeightHook = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            int mQsMinExpansionHeight = fieldQsMinExpansionHeight.getInt(mNotificationPanelView);
            if(!((int)XposedHelpers.callMethod(mNotificationStackScroller, "getNotGoneChildCount") > 0))
                param.setResult(mQsMinExpansionHeight);
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
            return null;
        }
    };

    private static final XC_MethodReplacement getPeekHeightStackScroller = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            final Object firstChild = XposedHelpers.callMethod(mNotificationStackScroller, "getFirstChildNotGone");
            final int firstChildMinHeight = firstChild != null ? (int)XposedHelpers.callMethod(firstChild, "getMinHeight")
                    : XposedHelpers.getIntField(mNotificationStackScroller, "mCollapsedSize");
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
            XposedHelpers.setIntField(mNotificationPanelView, "mScrollYOverride", 0);
        }
    };

    private static final XC_MethodHook onQsTouchHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            XposedHelpers.setIntField(mNotificationPanelView, "mScrollYOverride", 0);;
        }
    };

    private static final XC_MethodHook flingSettingsHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            XposedHelpers.setIntField(mNotificationPanelView, "mScrollYOverride", 0);;
        }
    };

    private static final XC_MethodReplacement onScrollChanged = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            boolean mQsExpanded = fieldQsExpanded.getBoolean(mNotificationPanelView);
            boolean mQsFullyExpanded = fieldQsFullyExpanded.getBoolean(mNotificationPanelView);
            fieldQsMaxExpansionHeight.setInt(mNotificationPanelView, (int)XposedHelpers.callMethod(mQsContainer, "getDesiredHeight"));
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
            float expandedHeight = (float)param.args[0];
            boolean mQsExpanded = fieldQsExpanded.getBoolean(mNotificationPanelView);
            boolean mQsExpandImmediate = fieldQsExpandImmediate.getBoolean(mNotificationPanelView);
            boolean mIsExpanding = fieldIsExpanding.getBoolean(mNotificationPanelView);
            boolean mQsExpandedWhenExpandingStarted = fieldQsExpandedWhenExpandingStarted.getBoolean(mNotificationPanelView);
            boolean mQsTracking = XposedHelpers.getBooleanField(mNotificationPanelView, "mQsTracking");
            boolean mQsExpansionFromOverscroll = fieldQsExpansionFromOverscroll.getBoolean(mNotificationPanelView);
            boolean isFullyCollapsed = (boolean)XposedHelpers.callMethod(mNotificationPanelView, "isFullyCollapsed");
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
                methodSetQsExpansion.invoke(mNotificationPanelView, mQsMinExpansionHeight
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
            int mQsMaxExpansionHeight = fieldQsMaxExpansionHeight.getInt(mNotificationPanelView);
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
            int mQsMinExpansionHeight = fieldQsMinExpansionHeight.getInt(mNotificationPanelView);
            float alpha = ((float)XposedHelpers.callMethod(mNotificationPanelView, "getNotificationsTopY") + getFirstItemMinHeight())
                    / (mQsMinExpansionHeight + (int)XposedHelpers.callMethod(mNotificationStackScroller, "getBottomStackPeekSize")
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
            int mQsMinExpansionHeight = fieldQsMinExpansionHeight.getInt(mNotificationPanelView);
            int mQsMaxExpansionHeight = fieldQsMaxExpansionHeight.getInt(mNotificationPanelView);
            Object mQsNavbarScrim = XposedHelpers.getObjectField(mNotificationPanelView, "mQsNavbarScrim");
            boolean mQsExpanded = fieldQsExpanded.getBoolean(mNotificationPanelView);
            boolean mStackScrollerOverscrolling = fieldStackScrollerOverscrolling.getBoolean(mNotificationPanelView);
            boolean mLastAnnouncementWasQuickSettings = XposedHelpers.getBooleanField(mNotificationPanelView, "mLastAnnouncementWasQuickSettings");
            boolean mTracking = XposedHelpers.getBooleanField(mNotificationPanelView, "mTracking");
            boolean isCollapsing = (boolean)XposedHelpers.callMethod(mNotificationPanelView, "isCollapsing");
            boolean mQsScrimEnabled = fieldQsScrimEnabled.getBoolean(mNotificationPanelView);
            float height = (float)param.args[0];
            height = Math.min(Math.max(height, mQsMinExpansionHeight), mQsMaxExpansionHeight);
            fieldQsFullyExpanded.setBoolean(mNotificationPanelView, height == mQsMaxExpansionHeight && mQsMaxExpansionHeight != 0);
            if (height > mQsMinExpansionHeight && !mQsExpanded && !mStackScrollerOverscrolling) {
                XposedHelpers.callMethod(mNotificationPanelView, "setQsExpanded", true);
            } else if (height <= mQsMinExpansionHeight && mQsExpanded) {
                XposedHelpers.callMethod(mNotificationPanelView, "setQsExpanded", false);
                if (mLastAnnouncementWasQuickSettings && !mTracking && !isCollapsing) {
                    XposedHelpers.callMethod(mNotificationPanelView, "announceForAccessibility", (boolean)XposedHelpers.callMethod(mNotificationPanelView, "getKeyguardOrLockScreenString"));
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
                XposedHelpers.callMethod(mQsNavbarScrim, "setAlpha", XposedHelpers.callMethod(mNotificationPanelView, "getQsExpansionFraction"));
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
            XposedHelpers.setIntField(mNotificationPanelView, "mScrollYOverride", -1);
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
            int newIndex = (int)param.args[0];
            Object mAmbientState = XposedHelpers.getObjectField(mNotificationStackScroller, "mAmbientState");
            XposedHelpers.callMethod(mAmbientState, "setSpeedBumpIndex", newIndex);
            return null;
        }
    };

    private static final XC_MethodReplacement onViewAddedInternal = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            Object child = param.args[0];
            XposedHelpers.callMethod(mNotificationStackScroller, "updateHideSensitiveForChild", child);
            XposedHelpers.callMethod(child, "setOnHeightChangedListener", this);
            XposedHelpers.callMethod(mNotificationStackScroller, "generateAddAnimation", child, false /* fromMoreCard */);
            XposedHelpers.callMethod(mNotificationStackScroller, "updateAnimationState", child);
            XposedHelpers.callMethod(mNotificationStackScroller, "updateChronometerForChild", child);
            return null;
        }
    };

    private static void updateQsExpansion() {
        QSContainerHelper.setQsExpansion((float)XposedHelpers.callMethod(mNotificationPanelView, "getQsExpansionFraction"), (float)XposedHelpers.callMethod(mNotificationPanelView, "getHeaderTranslation"));
    }

    private static int getFirstItemMinHeight() {
        final Object firstChild = XposedHelpers.callMethod(mNotificationStackScroller, "getFirstChildNotGone");
        int mCollapsedSize = XposedHelpers.getIntField(mNotificationStackScroller, "mCollapsedSize");
        return firstChild != null ? (int)XposedHelpers.callMethod(firstChild, "getMinHeight") : mCollapsedSize;
    }

    private static float getExpandTranslationStart() throws IllegalAccessException{
        Object mHeadsUpManager = XposedHelpers.getObjectField(mNotificationStackScroller, "mHeadsUpManager");
        int mMaxLayoutHeight = fieldMaxLayoutHeight.getInt(mNotificationStackScroller);
        int mIntrinsicPadding = fieldIntrinsicPadding.getInt(mNotificationStackScroller);
        int mBottomStackSlowDownHeight = fieldBottomStackSlowDownHeight.getInt(mNotificationStackScroller);
        int mBottomStackPeekSize = fieldBottomStackPeekSize.getInt(mNotificationStackScroller);
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
        boolean trackingHeadsUp = XposedHelpers.getBooleanField(mNotificationStackScroller, "mTrackingHeadsUp") || (boolean)XposedHelpers.callMethod(mHeadsUpManager, "hasPinnedHeadsUp");
        return trackingHeadsUp
                ? (int)XposedHelpers.callMethod(mHeadsUpManager, "getTopHeadsUpHeight")
                : 0;
    }

    /**
     * @return the position from where the appear transition ends when expanding.
     *         Measured in absolute height.
     */
    private static float getAppearEndPosition() throws IllegalAccessException{
        Object mHeadsUpManager = XposedHelpers.getObjectField(mNotificationStackScroller, "mHeadsUpManager");
        boolean trackingHeadsUp = XposedHelpers.getBooleanField(mNotificationStackScroller, "mTrackingHeadsUp") || (boolean)XposedHelpers.callMethod(mHeadsUpManager, "hasPinnedHeadsUp");
        int mBottomStackPeekSize = fieldBottomStackPeekSize.getInt(mNotificationStackScroller);
        int mBottomStackSlowDownHeight = fieldBottomStackSlowDownHeight.getInt(mNotificationStackScroller);
        int mTopPadding = XposedHelpers.getIntField(mNotificationStackScroller, "mTopPadding");
        int mIntrinsicPadding = fieldIntrinsicPadding.getInt(mNotificationStackScroller);
        int firstItemHeight = trackingHeadsUp || (boolean)XposedHelpers.callMethod(mHeadsUpManager, "hasPinnedHeadsUp")
                ? (int)XposedHelpers.callMethod(mHeadsUpManager, "getTopHeadsUpHeight") + mBottomStackPeekSize
                    + mBottomStackSlowDownHeight
                : getLayoutMinHeight();
        return firstItemHeight + (isOnKeyguard() ? mTopPadding : mIntrinsicPadding);
    }

    public static int getLayoutMinHeight() throws IllegalAccessException{
        int firstChildMinHeight = getFirstChildIntrinsicHeight();
        int mBottomStackPeekSize = fieldBottomStackPeekSize.getInt(mNotificationStackScroller);
        int mBottomStackSlowDownHeight = fieldBottomStackSlowDownHeight.getInt(mNotificationStackScroller);
        int mIntrinsicPadding = XposedHelpers.getIntField(mNotificationStackScroller, "mIntrinsicPadding");
        int mMaxLayoutHeight = fieldMaxLayoutHeight.getInt(mNotificationStackScroller);
        return Math.min(firstChildMinHeight + mBottomStackPeekSize + mBottomStackSlowDownHeight,
                mMaxLayoutHeight - mIntrinsicPadding);
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
    public static float getAppearFraction(float height) throws IllegalAccessException {
        float appearEndPosition = getAppearEndPosition();
        float appearStartPosition = getAppearStartPosition();
        return (height - appearStartPosition)
                / (appearEndPosition - appearStartPosition);
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
    /**
     *END
     */
}
