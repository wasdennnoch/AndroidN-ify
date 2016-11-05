package tk.wasdennnoch.androidn_ify.systemui.statusbar;

import android.content.res.XModuleResources;

import java.lang.reflect.Array;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.systemui.SystemUIHooks;
import tk.wasdennnoch.androidn_ify.utils.ConfigUtils;
import tk.wasdennnoch.androidn_ify.utils.RomUtils;

class LollipopStatusBarHooks extends StatusBarHooks {

    private static final String TAG = "LollipopStatusBarHooks";

    LollipopStatusBarHooks(ClassLoader classLoader) {
        super(classLoader);
    }

    @Override
    protected void hookSetMobileDataIndicators() {
        XposedBridge.hookAllMethods(mSignalClusterClass, "setMobileDataIndicators", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (mDataDisabled && ConfigUtils.notifications().enable_data_disabled_indicator) {
                    int typeIcon = !RomUtils.isCmBased() ? 2 : 3;
                    int isTypeIconWide = ConfigUtils.L1 && !RomUtils.isCmBased() ? 5 : 6;

                    Class mediaTekIconIdWrapperClass = null;
                    try {
                        mediaTekIconIdWrapperClass = XposedHelpers.findClass("com.mediatek.systemui.ext.IconIdWrapper", mClassLoader);
                        XposedHook.logI(TAG, "Identified MediaTek device");
                    } catch (XposedHelpers.ClassNotFoundError ignore) {
                    }
                    if (mediaTekIconIdWrapperClass != null && mediaTekIconIdWrapperClass.isInstance(param.args[typeIcon])) { // MediaTek devices wrap the id
                        Object iconIdWrapperArray = Array.newInstance(mediaTekIconIdWrapperClass, 1);
                        Array.set(iconIdWrapperArray, 0, XposedHelpers.newInstance(mediaTekIconIdWrapperClass, XModuleResources.createInstance(XposedHook.getModulePath(), null), SystemUIHooks.R_drawable_stat_sys_data_disabled));
                        param.args[typeIcon] = iconIdWrapperArray;
                    } else {
                        param.args[typeIcon] = SystemUIHooks.R_drawable_stat_sys_data_disabled;
                    }
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