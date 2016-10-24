package tk.wasdennnoch.androidn_ify.systemui.statusbar;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import tk.wasdennnoch.androidn_ify.systemui.SystemUIHooks;
import tk.wasdennnoch.androidn_ify.utils.ConfigUtils;
import tk.wasdennnoch.androidn_ify.utils.RomUtils;

class LollipopStatusBarHooks extends StatusBarHooks {

    LollipopStatusBarHooks(ClassLoader classLoader) {
        super(classLoader);
    }

    @Override
    protected void hookSetMobileDataIndicators() {
        XposedBridge.hookAllMethods(mSignalClusterClass, "setMobileDataIndicators", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (mDataDisabled && ConfigUtils.notifications().enable_data_disabled_indicator) {
                    int typeIcon = RomUtils.isCmBased() ? 3 : 2;
                    int isTypeIconWide = ConfigUtils.L1 && !RomUtils.isCmBased() ? 5 : 6;
                    param.args[typeIcon] = SystemUIHooks.R_drawable_stat_sys_data_disabled;
                    if (param.args[isTypeIconWide] instanceof Boolean)
                        param.args[isTypeIconWide] = false;
                    else if (param.args[7] instanceof Boolean) // Mediatek
                        // (boolean visible, int strengthIcon, int mobileTypeId,
                        // int ?, int ?, String ?, String ?,
                        // boolean isTypeIconWide, int subId
                        param.args[7] = false;
                    else if (param.args[8] instanceof Boolean) // Xperia
                        // (boolean mobileVisible, int mobileStrengthId, int mobileActivityId, int mobileTypeId,
                        // int mobileRoamingIconId, String mobileDecription, String mobileTypeDescription, boolean roaming,
                        // boolean isMobileTypeIconWide, int noSimIconId)
                        param.args[8] = false;
                }
            }
        });
    }

}
