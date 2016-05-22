package tk.wasdennnoch.androidn_ify.notifications.qs.tiles;

import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.notifications.NotificationPanelHooks;

public class CellularTileHook extends QSTileHook {

    private static final String CLASS_CELLULAR_TILE = "com.android.systemui.qs.tiles.CellularTile";

    public CellularTileHook(ClassLoader classLoader) {
        super(classLoader, CLASS_CELLULAR_TILE);
        hookClick();
        hookLongClick();
    }

    @Override
    public boolean handleClick() {
        if (NotificationPanelHooks.isCollapsed()) {
            Object mDetailAdapter = getObjectField("mDetailAdapter");
            boolean enabled = (boolean) XposedHelpers.callMethod(mDetailAdapter, "getToggleState");
            XposedHelpers.callMethod(mDetailAdapter, "setToggleState", !enabled);
            return true;
        }
        return false;
    }

    @Override
    public void handleLongClick() {
        try {
            startActivityDismissingKeyguard(XposedHelpers.getObjectField(getTile(), "DATA_USAGE_SETTINGS"));
        } catch (Throwable t) {
            startActivityDismissingKeyguard(XposedHelpers.getObjectField(getTile(), "CELLULAR_SETTINGS"));
        }
    }
}
