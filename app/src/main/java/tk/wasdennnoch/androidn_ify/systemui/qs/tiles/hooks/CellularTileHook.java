package tk.wasdennnoch.androidn_ify.systemui.qs.tiles.hooks;

import android.content.Intent;

import com.android.internal.logging.MetricsLogger;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.systemui.notifications.NotificationPanelHooks;

public class CellularTileHook extends QSTileHook {

    private static final String CLASS_CELLULAR_TILE = "com.android.systemui.qs.tiles.CellularTile";

    private Object mDataController;

    public CellularTileHook(Class classQSTile, ClassLoader classLoader) {
        super(classQSTile, classLoader, CLASS_CELLULAR_TILE);
    }

    @Override
    protected void afterConstructor(XC_MethodHook.MethodHookParam param) {
        mDataController = getObjectField("mDataController");
    }

    @Override
    public void handleClick() {
        MetricsLogger.action(mContext, MetricsLogger.QS_CELLULAR);
        if (NotificationPanelHooks.isCollapsed()) {
            // Only toggle
            Object mDetailAdapter = getObjectField("mDetailAdapter");
            boolean enabled = (boolean) XposedHelpers.callMethod(mDetailAdapter, "getToggleState");
            XposedHelpers.callMethod(mDetailAdapter, "setToggleState", !enabled);
        } else {
            if ((boolean) XposedHelpers.callMethod(mDataController, "isMobileDataSupported")) {
                showDetail(true);
            } else {
                startSettings();
            }
        }
    }

    @Override
    public void handleLongClick() {
        startSettings();
    }

    @Override
    protected Intent getSettingsIntent() {
        try {
            return (Intent) XposedHelpers.getObjectField(mThisObject, "DATA_USAGE_SETTINGS");
        } catch (Throwable t) {
            return (Intent) XposedHelpers.getObjectField(mThisObject, "CELLULAR_SETTINGS");
        }
    }

}
