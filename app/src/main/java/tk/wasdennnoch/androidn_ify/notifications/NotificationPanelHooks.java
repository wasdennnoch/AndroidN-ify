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

            View mQsContainer = (View) XposedHelpers.getObjectField(param.thisObject, "mQsContainer");
            try {
                //noinspection deprecation
                mQsContainer.setBackgroundColor(context.getResources().getColor(context.getResources().getIdentifier("system_primary_color", "color", PACKAGE_SYSTEMUI)));
            } catch (Throwable t) {
                XposedHook.logE(TAG, "Couldn't change QS container background color", t);
            }
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
