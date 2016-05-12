package tk.wasdennnoch.androidn_ify.notifications.qs.tiles;

import android.content.Context;

import com.android.internal.logging.MetricsLogger;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.notifications.NotificationPanelHooks;

public class BluetoothTileHook {

    private static final String CLASS_BLUETOOTH_TILE = "com.android.systemui.qs.tiles.BluetoothTile";
    private static XC_MethodReplacement handleClickHook = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            if (NotificationPanelHooks.isCollapsed()) {
                Object mState = XposedHelpers.getObjectField(param.thisObject, "mState");
                Object mController = XposedHelpers.getObjectField(param.thisObject, "mController");
                boolean enabled = XposedHelpers.getBooleanField(mState, "value");
                MetricsLogger.action((Context) XposedHelpers.getObjectField(param.thisObject, "mContext"), (int) XposedHelpers.callMethod(param.thisObject, "getMetricsCategory"), !enabled);
                XposedHelpers.callMethod(mController, "setBluetoothEnabled", !enabled);
            } else {
                XposedHelpers.callMethod(param.thisObject, "handleSecondaryClick");
            }
            return null;
        }
    };

    public static void hook(ClassLoader classLoader) {
        try {
            Class<?> classBluetoothTile = XposedHelpers.findClass(CLASS_BLUETOOTH_TILE, classLoader);

            XposedHelpers.findAndHookMethod(classBluetoothTile, "handleClick", handleClickHook);
        } catch (Exception ignore) {

        }
    }
}
