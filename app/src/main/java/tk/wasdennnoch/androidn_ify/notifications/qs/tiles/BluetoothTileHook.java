package tk.wasdennnoch.androidn_ify.notifications.qs.tiles;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.notifications.NotificationPanelHooks;

public class BluetoothTileHook extends QSTileHook {

    private static final String CLASS_BLUETOOTH_TILE = "com.android.systemui.qs.tiles.BluetoothTile";

    public BluetoothTileHook(ClassLoader classLoader, boolean firstRowSmall) {
        super(classLoader, CLASS_BLUETOOTH_TILE);
        hookClick();
        hookLongClick();
        if (firstRowSmall) {
            try {
                XposedHelpers.findAndHookMethod(getTileClass(), "supportsDualTargets", XC_MethodReplacement.returnConstant(false));
            } catch (Throwable ignore) {

            }
        }
    }

    @Override
    public boolean handleClick() {
        if (!NotificationPanelHooks.isCollapsed()) {
            callSecondaryClick();
            return true;
        }
        return false;
    }

    @Override
    public void handleLongClick() {
        startActivityDismissingKeyguard("BLUETOOTH_SETTINGS");
    }
}
