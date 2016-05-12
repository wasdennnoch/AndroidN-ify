package tk.wasdennnoch.androidn_ify.notifications.qs.tiles;

import android.content.Context;

import com.android.internal.logging.MetricsLogger;

import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.notifications.NotificationPanelHooks;

public class BluetoothTileHook extends QSTileHook {

    private static final String CLASS_BLUETOOTH_TILE = "com.android.systemui.qs.tiles.BluetoothTile";

    public BluetoothTileHook(ClassLoader classLoader) {
        super(classLoader, CLASS_BLUETOOTH_TILE);
        hookClick();
        hookLongClick();
    }

    @Override
    public void handleClick() {
        if (NotificationPanelHooks.isCollapsed()) {
            Object mState = getState();
            Object mController = getObjectField("mController");
            boolean enabled = XposedHelpers.getBooleanField(mState, "value");
            MetricsLogger.action((Context) getObjectField("mContext"), (int) XposedHelpers.callMethod(getTile(), "getMetricsCategory"), !enabled);
            XposedHelpers.callMethod(mController, "setBluetoothEnabled", !enabled);
        } else {
            XposedHelpers.callMethod(getTile(), "handleSecondaryClick");
        }
    }

    @Override
    public void handleLongClick() {
        startActivityDismissingKeyguard("BLUETOOTH_SETTINGS");
    }
}
