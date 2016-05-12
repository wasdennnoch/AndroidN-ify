package tk.wasdennnoch.androidn_ify.notifications.qs.tiles;

import android.content.Context;

import com.android.internal.logging.MetricsLogger;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.notifications.NotificationPanelHooks;

public class CellularTileHook {

    private static final String CLASS_CELLULAR_TILE = "com.android.systemui.qs.tiles.CellularTile";
    private static Class<?> mClassCellularTile;
    private static XC_MethodReplacement handleClickHook = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
            Object mDataController = XposedHelpers.getObjectField(param.thisObject, "mDataController");
            int metricsCategory = (int) XposedHelpers.callMethod(param.thisObject, "getMetricsCategory");
            if (NotificationPanelHooks.isCollapsed()) {
                Object mDetailAdapter = XposedHelpers.getObjectField(param.thisObject, "mDetailAdapter");
                boolean enabled = (boolean) XposedHelpers.callMethod(mDetailAdapter, "getToggleState");
                MetricsLogger.action(mContext, MetricsLogger.QS_CELLULAR_TOGGLE, !enabled);
                XposedHelpers.callMethod(mDataController, "setMobileDataEnabled", !enabled);
            } else {
                MetricsLogger.action(mContext, metricsCategory);
                if ((boolean) XposedHelpers.callMethod(mDataController, "isMobileDataSupported")) {
                    XposedHelpers.callMethod(param.thisObject, "showDetail", true);
                } else {
                    Object mHost = XposedHelpers.getObjectField(param.thisObject, "mHost");
                    XposedHelpers.callMethod(mHost, "startActivityDismissingKeyguard", XposedHelpers.getStaticObjectField(mClassCellularTile, "DATA_USAGE_SETTINGS"));
                }
            }
            return null;
        }
    };

    public static void hook(ClassLoader classLoader) {
        try {
            mClassCellularTile = XposedHelpers.findClass(CLASS_CELLULAR_TILE, classLoader);

            XposedHelpers.findAndHookMethod(mClassCellularTile, "handleClick", handleClickHook);
        } catch (Exception ignore) {

        }
    }
}
