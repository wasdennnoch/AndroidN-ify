package tk.wasdennnoch.androidn_ify.systemui.qs.tiles.hooks;

import android.content.Intent;

import com.android.internal.logging.MetricsLogger;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.systemui.notifications.NotificationPanelHooks;
import tk.wasdennnoch.androidn_ify.utils.ConfigUtils;

public class CellularTileHook extends QSTileHook {

    private static final String CLASS_CELLULAR_TILE = "com.android.systemui.qs.tiles.CellularTile";

    private Object mDataController;

    public CellularTileHook(ClassLoader classLoader) {
        super(classLoader, CLASS_CELLULAR_TILE);
    }

    @Override
    protected void afterConstructor(XC_MethodHook.MethodHookParam param) {
        try {
            mDataController = getObjectField("mDataController");
        } catch (Throwable t) {
            mDataController = getObjectField("mController");
        }
    }

    @Override
    public void handleClick() {
        if (ConfigUtils.M) {
            MetricsLogger.action(mContext, MetricsLogger.QS_CELLULAR);
        }
        if (NotificationPanelHooks.isCollapsed()) {
            // Only toggle
            Object mDetailAdapter = getObjectField("mDetailAdapter");
            boolean enabled = (boolean) XposedHelpers.callMethod(mDetailAdapter, "getToggleState");
            XposedHelpers.callMethod(mDetailAdapter, "setToggleState", !enabled);
        } else {
            boolean dataSupported;
            try {
                dataSupported = (boolean) XposedHelpers.callMethod(mDataController, "isMobileDataSupported");
            } catch (Throwable t) { // Motorola
                dataSupported = (boolean) XposedHelpers.callMethod(mDataController, "isMobileDataSupported", XposedHelpers.getIntField(mThisObject, "mSubId"));
            }
            if (dataSupported) {
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
