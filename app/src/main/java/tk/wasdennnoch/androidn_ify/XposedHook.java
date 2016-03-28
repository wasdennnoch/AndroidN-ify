package tk.wasdennnoch.androidn_ify;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import tk.wasdennnoch.androidn_ify.recents.doubletap.DoubleTapHwKeys;
import tk.wasdennnoch.androidn_ify.recents.doubletap.DoubleTapSwKeys;
import tk.wasdennnoch.androidn_ify.settings.SettingsHooks;

public class XposedHook implements IXposedHookLoadPackage, IXposedHookZygoteInit {

    private static final String TAG = "[Android N-ify]";
    public static final String PACKAGE_ANDROID = "android";
    public static final String PACKAGE_SYSTEMUI = "com.android.systemui";
    public static final String PACKAGE_SETTINGS = "com.android.settings";
    public static boolean debug = true;

    private static XSharedPreferences sPrefs;

    public static void logE(String tag, String msg, Throwable t) {
        XposedBridge.log(TAG + " [ERROR] " + tag + ": " + msg);
        if (t != null)
            XposedBridge.log(t);
    }

    public static void logW(String tag, String msg) {
        XposedBridge.log(TAG + " [WARNING] " + tag + ": " + msg);
    }

    public static void log(String tag, String msg) {
        XposedBridge.log(TAG + " " + tag + ": " + msg);
    }

    public static void logD(String tag, String msg) {
        if (debug) XposedBridge.log(TAG + " [DEBUG] " + tag + ": " + msg);
    }

    public static XSharedPreferences getPrefs() {
        if (sPrefs == null)
            sPrefs = new XSharedPreferences("tk.wasdennnoch.androidn_ify");
        return sPrefs;
    }

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        debug = getPrefs().getBoolean("debug_log", false);
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {

        switch (lpparam.packageName) {
            case PACKAGE_SETTINGS:
                SettingsHooks.hook(lpparam.classLoader, getPrefs());
                break;
            case PACKAGE_SYSTEMUI:
                DoubleTapSwKeys.hook(lpparam.classLoader, getPrefs());
                break;
            case PACKAGE_ANDROID:
                DoubleTapHwKeys.hook(lpparam.classLoader, getPrefs());
                break;
        }

    }

}
