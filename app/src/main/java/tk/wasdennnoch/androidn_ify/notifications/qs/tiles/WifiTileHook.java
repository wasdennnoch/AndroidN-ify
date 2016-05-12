package tk.wasdennnoch.androidn_ify.notifications.qs.tiles;

import android.content.Context;

import com.android.internal.logging.MetricsLogger;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.notifications.NotificationPanelHooks;

public class WifiTileHook extends QSTileHook {

    private static final String CLASS_WIFI_TILE = "com.android.systemui.qs.tiles.WifiTile";

    public WifiTileHook(ClassLoader classLoader, boolean isCm) {
        super(classLoader, CLASS_WIFI_TILE);
        hookClick();
        hookLongClick();
        if (!isCm) {
            try {
                XposedHelpers.findAndHookMethod(getTileClass(), "supportsDualTargets", XC_MethodReplacement.returnConstant(false));
            } catch (Throwable ignore) {

            }
        }
    }

    @Override
    public void handleClick() {
        if (NotificationPanelHooks.isCollapsed()) {
            Object mState = getState();
            Object mController = getObjectField("mController");
            boolean enabled = XposedHelpers.getBooleanField(mState, "enabled");
            XposedHelpers.callMethod(mState, "copyTo", getObjectField("mStateBeforeClick"));
            MetricsLogger.action((Context) getObjectField("mContext"), (int) XposedHelpers.callMethod(getTile(), "getMetricsCategory"), !enabled);
            XposedHelpers.callMethod(mController, "setWifiEnabled", !enabled);
        } else {
            callSecondaryClick();
        }
    }

    @Override
    public void handleLongClick() {
        startActivityDismissingKeyguard("WIFI_SETTINGS");
    }
}
