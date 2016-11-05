package tk.wasdennnoch.androidn_ify.systemui.qs.tiles.hooks;

import android.content.Intent;
import android.provider.Settings;

import com.android.internal.logging.MetricsLogger;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.systemui.notifications.NotificationPanelHooks;
import tk.wasdennnoch.androidn_ify.utils.ConfigUtils;

public class WifiTileHook extends QSTileHook {

    private static final String CLASS_WIFI_TILE = "com.android.systemui.qs.tiles.WifiTile";

    private Object mController;
    private Object mWifiController;

    public WifiTileHook(ClassLoader classLoader, boolean firstRowSmall) {
        super(classLoader, CLASS_WIFI_TILE);
        if (firstRowSmall) setDualTargets();
    }

    @Override
    protected void afterConstructor(XC_MethodHook.MethodHookParam param) {
        mController = getObjectField("mController");
        if (ConfigUtils.M)
            mWifiController = getObjectField("mWifiController");
    }

    @Override
    public void handleClick() {
        Object mState = getState();
        boolean enabled = XposedHelpers.getBooleanField(mState, "enabled");
        if (NotificationPanelHooks.isCollapsed()) {
            XposedHelpers.callMethod(mState, "copyTo", getObjectField("mStateBeforeClick"));
            if (ConfigUtils.M) {
                MetricsLogger.action(mContext, MetricsLogger.QS_WIFI, !enabled);
            }
            XposedHelpers.callMethod(mController, "setWifiEnabled", !enabled);
        } else {
            if (ConfigUtils.M && !(boolean) XposedHelpers.callMethod(mWifiController, "canConfigWifi")) {
                startSettings();
            } else {
                if (!enabled) {
                    XposedHelpers.callMethod(mController, "setWifiEnabled", true);
                    XposedHelpers.setBooleanField(mState, "enabled", true);
                }
                showDetail(true);
            }
        }
    }

    @Override
    public void handleLongClick() {
        startSettings();
    }

    @Override
    protected Intent getSettingsIntent() {
        return new Intent(Settings.ACTION_WIFI_SETTINGS);
    }

}
