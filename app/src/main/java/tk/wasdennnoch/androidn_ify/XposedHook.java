package tk.wasdennnoch.androidn_ify;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import tk.wasdennnoch.androidn_ify.recents.doubletap.DoubleTapHwKeys;
import tk.wasdennnoch.androidn_ify.recents.doubletap.DoubleTapSwKeys;
import tk.wasdennnoch.androidn_ify.settings.SettingsHooks;

public class XposedHook implements IXposedHookLoadPackage {

    public static final boolean debug = true;

    public static final String PACKAGE_ANDROID = "android";
    public static final String PACKAGE_SYSTEMUI = "com.android.systemui";
    public static final String PACKAGE_SETTINGS = "com.android.settings";
    //private XSharedPreferences mPrefs = new XSharedPreferences(XposedHook.this.getClass().getPackage().getName());

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {

        switch (lpparam.packageName) {
            case PACKAGE_SETTINGS:
                SettingsHooks.hookLoadCategoriesFromResource(lpparam);
                break;
            case PACKAGE_SYSTEMUI:
                DoubleTapSwKeys.hook(lpparam);
                break;
            case PACKAGE_ANDROID:
                DoubleTapHwKeys.hook(lpparam);
                break;
        }

    }

    public static void logE(String tag, String msg, Throwable t) {
        XposedBridge.log("[Android N-ify] [FATAL ERROR] " + tag + ": " + msg);
        if (t != null)
            XposedBridge.log(t);
    }

    public static void log(String tag, String msg) {
        XposedBridge.log("[Android N-ify] " + tag + ": " + msg);
    }

    public static void logD(String tag, String msg) {
        if (debug) XposedBridge.log("[Android N-ify] [DEBUG] " + tag + ": " + msg);
    }

}
