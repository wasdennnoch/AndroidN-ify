package tk.wasdennnoch.androidn_ify.notifications.qs.tiles;

import android.content.Context;

import com.android.internal.logging.MetricsLogger;

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
    public void handleClick() {
        Context mContext = (Context) getObjectField("mContext");
        Object mDataController = getObjectField("mDataController");
        int metricsCategory = (int) XposedHelpers.callMethod(getTile(), "getMetricsCategory");
        if (NotificationPanelHooks.isCollapsed()) {
            Object mDetailAdapter = getObjectField("mDetailAdapter");
            boolean enabled = (boolean) XposedHelpers.callMethod(mDetailAdapter, "getToggleState");
            MetricsLogger.action(mContext, MetricsLogger.QS_CELLULAR_TOGGLE, !enabled);
            XposedHelpers.callMethod(mDataController, "setMobileDataEnabled", !enabled);
        } else {
            MetricsLogger.action(mContext, metricsCategory);
            if ((boolean) XposedHelpers.callMethod(mDataController, "isMobileDataSupported")) {
                XposedHelpers.callMethod(getTile(), "showDetail", true);
            } else {
                startActivityDismissingKeyguard("DATA_USAGE_SETTINGS");
            }
        }
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
