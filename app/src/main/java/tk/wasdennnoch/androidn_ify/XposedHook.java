package tk.wasdennnoch.androidn_ify;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import tk.wasdennnoch.androidn_ify.recents.RecentsHooks;
import tk.wasdennnoch.androidn_ify.settings.SettingsHooks;

public class XposedHook implements IXposedHookLoadPackage {

    public static final boolean debug = false;

    public static final String PACKAGE_ANDROID = "android";
    public static final String PACKAGE_SETTINGS = "com.android.settings";
    //private XSharedPreferences mPrefs = new XSharedPreferences(XposedHook.this.getClass().getPackage().getName());

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {

        if (lpparam.packageName.equals(PACKAGE_SETTINGS)) {

            SettingsHooks.hookLoadCategoriesFromResource(lpparam);

        } else if (lpparam.packageName.equals(PACKAGE_ANDROID)){

            RecentsHooks.hookInterceptKeyBeforeDispatching(lpparam);

        }

    }

    public static void logE(String msg, Throwable t) {
        log("[FATAL ERROR] " + msg);
        if (t != null)
            XposedBridge.log(t);
    }

    public static void log(String msg) {
        XposedBridge.log("[Android N-ify] " + msg);
    }

    public static void logD(String msg) {
        if (debug) log("[DEBUG] " + msg);
    }

}
