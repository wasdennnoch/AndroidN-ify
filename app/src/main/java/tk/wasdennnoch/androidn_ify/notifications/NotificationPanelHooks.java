package tk.wasdennnoch.androidn_ify.notifications;

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

    private static final String CLASS_NOTIFICATION_PANEL_VIEW = "com.android.systemui.statusbar.phone.NotificationPanelView";

    private static ViewGroup mNotificationPanelView;
    private static ExpandableIndicator mExpandIndicator;

    private static XC_MethodHook onFinishInflateHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            mNotificationPanelView = (ViewGroup) param.thisObject;
            mNotificationPanelView.setClipChildren(false);
            mNotificationPanelView.setClipToPadding(false);
            View mHeader = (View) XposedHelpers.getObjectField(param.thisObject, "mHeader");
            mHeader.setOnClickListener(null);
            mExpandIndicator = (ExpandableIndicator) mHeader.findViewById(R.id.statusbar_header_expand_indicator);
            mExpandIndicator.setOnClickListener(mExpandIndicatorListener);
        }
    };

    private static View.OnClickListener mExpandIndicatorListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            // Fixes an issue with two backgrounds
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
        XposedHelpers.callMethod(mNotificationPanelView, "flingSettings", 0, expanded);
    }

    public static void hook(ClassLoader classLoader) {
        try {
            if (ConfigUtils.header().header) {

                Class<?> classNotificationPanelView = XposedHelpers.findClass(CLASS_NOTIFICATION_PANEL_VIEW, classLoader);

                XposedHelpers.findAndHookMethod(classNotificationPanelView, "onFinishInflate", onFinishInflateHook);

            }
        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error in hook", t);
        }
    }
}
