package tk.wasdennnoch.androidn_ify.notifications.qs.tiles;

import android.content.Context;

import com.android.internal.logging.MetricsLogger;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.notifications.NotificationPanelHooks;

public class WifiTileHook {

    private static final String CLASS_WIFI_TILE = "com.android.systemui.qs.tiles.WifiTile";
    private static XC_MethodReplacement handleClickHook = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            if (NotificationPanelHooks.isCollapsed()) {
                Object mState = XposedHelpers.getObjectField(param.thisObject, "mState");
                Object mController = XposedHelpers.getObjectField(param.thisObject, "mController");
                boolean enabled = XposedHelpers.getBooleanField(mState, "enabled");
                XposedHelpers.callMethod(mState, "copyTo", XposedHelpers.getObjectField(param.thisObject, "mStateBeforeClick"));
                MetricsLogger.action((Context) XposedHelpers.getObjectField(param.thisObject, "mContext"), (int) XposedHelpers.callMethod(param.thisObject, "getMetricsCategory"), !enabled);
                XposedHelpers.callMethod(mController, "setWifiEnabled", !enabled);
            } else {
                XposedHelpers.callMethod(param.thisObject, "handleSecondaryClick");
            }
            return null;
        }
    };

    public static void hook(ClassLoader classLoader) {
        try {
            Class<?> classWifiTile = XposedHelpers.findClass(CLASS_WIFI_TILE, classLoader);

            XposedHelpers.findAndHookMethod(classWifiTile, "handleClick", handleClickHook);
        } catch (Exception ignore) {

        }
    }
}
