package tk.wasdennnoch.androidn_ify.notifications;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.extracted.systemui.ExpandableIndicator;
import tk.wasdennnoch.androidn_ify.utils.ConfigUtils;

public class NotificationPanelHooks {

    private static final String TAG = "NotificationPanelHooks";

    private static final String PACKAGE_SYSTEMUI = XposedHook.PACKAGE_SYSTEMUI;
    private static final String CLASS_NOTIFICATION_PANEL_VIEW = "com.android.systemui.statusbar.phone.NotificationPanelView";
    private static final String CLASS_PANEL_VIEW = "com.android.systemui.statusbar.phone.PanelView";

    public static final int STATE_SHADE = 0;
    public static final int STATE_KEYGUARD = 1;

    private static ViewGroup mNotificationPanelView;
    private static ExpandableIndicator mExpandIndicator;

    private static XC_MethodHook onFinishInflateHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            mNotificationPanelView = (ViewGroup) param.thisObject;
            Context context = mNotificationPanelView.getContext();

            mNotificationPanelView.setClipChildren(false);
            mNotificationPanelView.setClipToPadding(false);
            View mHeader = (View) XposedHelpers.getObjectField(param.thisObject, "mHeader");
            mHeader.setOnClickListener(null);
            mExpandIndicator = (ExpandableIndicator) mHeader.findViewById(R.id.statusbar_header_expand_indicator);
            mExpandIndicator.setOnClickListener(mExpandIndicatorListener);

            if (!ConfigUtils.header().keep_qs_panel_background) {
                View mQsContainer = (View) XposedHelpers.getObjectField(param.thisObject, "mQsContainer");
                try {
                    //noinspection deprecation
                    mQsContainer.setBackgroundColor(context.getResources().getColor(context.getResources().getIdentifier("system_primary_color", "color", PACKAGE_SYSTEMUI)));
                } catch (Throwable t) {
                    XposedHook.logE(TAG, "Couldn't change QS container background color", t);
                }
            }
        }
    };

    private static XC_MethodHook setBarStateHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            StatusBarHeaderHooks.onSetBarState((int) param.args[0]);
        }
    };

    private static View.OnClickListener mExpandIndicatorListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            // Fixes an issue with the indicator having two backgrounds when layer type is hardware
            mExpandIndicator.setLayerType(View.LAYER_TYPE_NONE, null);
            flingSettings(!mExpandIndicator.isExpanded());
        }
    };

    @SuppressWarnings("unused")
    public static boolean isExpanded() {
        return (mExpandIndicator != null && mExpandIndicator.isExpanded());
    }

    public static boolean isCollapsed() {
        return (mExpandIndicator != null && !mExpandIndicator.isExpanded());
    }

    public static void expandIfNecessary() {
        if (mExpandIndicator == null || mNotificationPanelView == null) return;
        if (!mExpandIndicator.isExpanded()) flingSettings(true);
    }

    public static void collapseIfNecessary() {
        if (mExpandIndicator == null || mNotificationPanelView == null) return;
        if (mExpandIndicator.isExpanded()) flingSettings(false);
    }

    public static void flingSettings(boolean expanded) {
        try {
            XposedHelpers.callMethod(mNotificationPanelView, "flingSettings", new Class[]{float.class, boolean.class, Runnable.class, boolean.class}, 0, expanded, null, true);
        } catch (Throwable t) {
            XposedHelpers.callMethod(mNotificationPanelView, "flingSettings", 0, expanded);
        }
    }

    public static int getStatusBarState() {
        return XposedHelpers.getIntField(mNotificationPanelView, "mStatusBarState");
    }

    public static void hook(ClassLoader classLoader) {
        try {
            if (ConfigUtils.header().header) {

                Class<?> classNotificationPanelView = XposedHelpers.findClass(CLASS_NOTIFICATION_PANEL_VIEW, classLoader);
                Class<?> classPanelView = XposedHelpers.findClass(CLASS_PANEL_VIEW, classLoader);

                XposedHelpers.findAndHookMethod(classNotificationPanelView, "onFinishInflate", onFinishInflateHook);
                XposedHelpers.findAndHookMethod(classNotificationPanelView, "setBarState", int.class, boolean.class, boolean.class, setBarStateHook);

                XposedHelpers.findAndHookMethod(classPanelView, "schedulePeek", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        XposedHelpers.callMethod(mNotificationPanelView, "setListening", true);
                    }
                });

            }
        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error in hook", t);
        }
    }
}
