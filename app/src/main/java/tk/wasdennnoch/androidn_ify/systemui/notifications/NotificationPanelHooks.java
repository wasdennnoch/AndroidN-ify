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
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.extracted.systemui.ExpandableIndicator;
import tk.wasdennnoch.androidn_ify.misc.SafeOnClickListener;
import tk.wasdennnoch.androidn_ify.systemui.qs.QSContainerHelper;
import tk.wasdennnoch.androidn_ify.systemui.qs.customize.QSCustomizer;
import tk.wasdennnoch.androidn_ify.utils.ConfigUtils;

@SuppressLint("StaticFieldLeak")
public class NotificationPanelHooks {

    private static final String TAG = "NotificationPanelHooks";

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

    private static final XC_MethodHook updateHeaderShade = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            XposedHook.logI(TAG, "getHeaderTranslation" + XposedHelpers.callMethod(mNotificationPanelView, "getHeaderTranslation").toString());
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
            if(ev.getY() < mQsContainer.getBottom() && !mIsBeingDragged) {
                param.setResult(false);
            }
        }
    };

    /*private static final XC_MethodReplacement getHeaderTranslation = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            int mQsMinExpansionHeight = XposedHelpers.getIntField(mNotificationPanelView, "mQsMinExpansionHeight");
            if (getStatusBarState() == STATE_KEYGUARD) {
                return 0;
            }
            float translation = NotificationUtils.interpolate(-mQsMinExpansionHeight, 0, mNotificationStackScroller.getAppearFraction(mExpandedHeight));
            height - appearStartPosition)
            / (appearEndPosition - appearStartPosition);
            return start * (1.0f - amount) + end * amount;
            return Math.min(0, translation);
        }
    }*/

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
                Class<?> classNotificationPanelView = XposedHelpers.findClass(CLASS_NOTIFICATION_PANEL_VIEW, classLoader);
                Class<?> classQSContainer = XposedHelpers.findClass(CLASS_QS_CONTAINER, classLoader);
                Class<?> classPanelView = XposedHelpers.findClass(CLASS_PANEL_VIEW, classLoader);
                Class<?> classObservableScrollView = XposedHelpers.findClass(CLASS_OBSERVABLE_SCROLL_VIEW, classLoader);

                XposedHelpers.findAndHookMethod(classNotificationPanelView, "onFinishInflate", onFinishInflateHook);
                XposedHelpers.findAndHookMethod(classNotificationPanelView, "setBarState", int.class, boolean.class, boolean.class, setBarStateHook);
                XposedHelpers.findAndHookMethod(classNotificationPanelView, "requestDisallowInterceptTouchEvent", boolean.class, XC_MethodReplacement.DO_NOTHING);
                XposedHelpers.findAndHookMethod(classNotificationPanelView, "onOverscrolled", float.class, float.class, int.class, XC_MethodReplacement.DO_NOTHING);
                //XposedHelpers.findAndHookMethod(classNotificationPanelView, "updateHeaderShade", updateHeaderShade);
                XposedHelpers.findAndHookMethod(classNotificationPanelView, "getTempQsMaxExpansion", getTempQsMaxExpansion);
                XposedHelpers.findAndHookMethod(classNotificationPanelView, "onExpandingStarted", onExpandingStartedHook);
                XposedHelpers.findAndHookMethod(classNotificationPanelView, "onClick", View.class, onClick);
                XposedHelpers.findAndHookMethod(classNotificationPanelView, "setVerticalPanelTranslation", float.class, setVerticalPanelTranslation);
                XposedHelpers.findAndHookMethod(classNotificationPanelView, "onQsExpansionStarted", int.class, onQsExpansionStarted);
                XposedHelpers.findAndHookMethod(classNotificationPanelView, "updateQsState", updateQsState);
                XposedHelpers.findAndHookMethod(classNotificationPanelView, "isScrolledToBottom", isScrolledToBottom);

                XposedHelpers.findAndHookMethod(classNotificationStackScrollLayout, "setScrollView", ViewGroup.class, XC_MethodReplacement.DO_NOTHING);
                XposedHelpers.findAndHookMethod(classNotificationStackScrollLayout, "onTouchEvent", MotionEvent.class, onTouchEventHook);
                XposedHelpers.findAndHookMethod(classNotificationStackScrollLayout, "onInterceptTouchEvent", MotionEvent.class, onInterceptTouchEventHook);
                XposedHelpers.findAndHookMethod(classNotificationStackScrollLayout, "onScrollTouch", MotionEvent.class, onScrollTouchHook);

                XposedHelpers.findAndHookMethod(classObservableScrollView, "onScrollChanged", int.class, int.class, int.class, int.class, XC_MethodReplacement.DO_NOTHING);
                XposedHelpers.findAndHookMethod(classObservableScrollView, "onOverScrolled", int.class, int.class, boolean.class, boolean.class, XC_MethodReplacement.DO_NOTHING);
                XposedHelpers.findAndHookMethod(classObservableScrollView, "overScrollBy", int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, boolean.class, XC_MethodReplacement.returnConstant(false));
                XposedHelpers.findAndHookMethod(classObservableScrollView, "isHandlingTouchEvent", XC_MethodReplacement.returnConstant(false));
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

                    /*
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

                    XposedHelpers.findAndHookMethod(classNotificationStackScrollLayout, "onScrollTouch", MotionEvent.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            MotionEvent ev = (MotionEvent) param.args[0];
                            if (mQsContainerHelper != null && ev.getY() < mQsContainerHelper.getBottom())
                                param.setResult(false);
                        }
                    });
                    */
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
