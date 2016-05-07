package tk.wasdennnoch.androidn_ify;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import tk.wasdennnoch.androidn_ify.notifications.NotificationPanelHooks;
import tk.wasdennnoch.androidn_ify.notifications.NotificationsHooks;
import tk.wasdennnoch.androidn_ify.notifications.StatusBarHeaderHooks;
import tk.wasdennnoch.androidn_ify.recents.doubletap.DoubleTapHwKeys;
import tk.wasdennnoch.androidn_ify.recents.doubletap.DoubleTapSwKeys;
import tk.wasdennnoch.androidn_ify.recents.stack.RecentsStackHooks;
import tk.wasdennnoch.androidn_ify.settings.SettingsHooks;

/**
 * Right now it's impossible to explicitly use classes of the hooked package
 * (e.g. <code>com.android.systemui.statusbar.policy.KeyButtonView</code>) because those
 * application classes aren't loaded yet when the method <code>handleLoadPackage</code>
 * gets called. Refection is used to find classes and methods, which forces ART to index
 * every class in the <code>CLASSPATH</code> variable. When indexing a hook class that
 * implements classes of the hooked package, a <code>NoClassDefFoundError</code> will be
 * thrown as those classes aren't stored in the <code>CLASSPATH</code> yet. This forces
 * us to work with standard framework classes and reflection. I hope this is sort of
 * understandable.
 *
 * @see <a href="https://github.com/rovo89/XposedBridge/issues/57">https://github.com/rovo89/XposedBridge/issues/57</a>
 */
public class XposedHook implements IXposedHookLoadPackage, IXposedHookZygoteInit, IXposedHookInitPackageResources {


    private static final String TAG = "XposedHook";
    private static final String LOG_FORMAT = "[Android N-ify] %1$s %2$s: %3$s";
    public static final String PACKAGE_ANDROID = "android";
    public static final String PACKAGE_SYSTEMUI = "com.android.systemui";
    public static final String PACKAGE_SETTINGS = "com.android.settings";
    public static final String PACKAGE_OWN = "tk.wasdennnoch.androidn_ify";
    public static final String SETTINGS_OWN = PACKAGE_OWN + ".ui.SettingsActivity";
    public static boolean debug = false;
    private static String sModulePath;

    private static XSharedPreferences sPrefs;

    public static void logE(String tag, String msg, Throwable t) {
        XposedBridge.log(String.format(LOG_FORMAT, "[ERROR]", tag, msg));
        if (t != null)
            XposedBridge.log(t);
    }

    public static void logW(String tag, String msg) {
        XposedBridge.log(String.format(LOG_FORMAT, "[WARNING]", tag, msg));
    }

    @SuppressWarnings("unused")
    public static void logI(String tag, String msg) {
        XposedBridge.log(String.format(LOG_FORMAT, "[INFO]", tag, msg));
    }

    public static void logD(String tag, String msg) {
        if (debug) XposedBridge.log(String.format(LOG_FORMAT, "[DEBUG]", tag, msg));
    }

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        sModulePath = startupParam.modulePath;
        sPrefs = new XSharedPreferences(XposedHook.class.getPackage().getName());
        if (!sPrefs.getBoolean("can_read_prefs", false)) {
            // With SELinux enforcing, it might happen that we don't have access
            // to the prefs file. Test this by reading a test key that should be
            // set to true. If it is false, we either can't read the file or the
            // user has never opened the preference screen before.
            logW(TAG, "Can't read prefs file, default values will be applied in hooks!");
        }
        debug = sPrefs.getBoolean("debug_log", false);
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {

        switch (lpparam.packageName) {
            case PACKAGE_SETTINGS:
                SettingsHooks.hook(lpparam.classLoader, sPrefs);
                break;
            case PACKAGE_SYSTEMUI:
                DoubleTapSwKeys.hook(lpparam.classLoader, sPrefs);
                StatusBarHeaderHooks.hook(lpparam.classLoader, sPrefs);
                NotificationPanelHooks.hook(lpparam.classLoader, sPrefs);
                NotificationsHooks.hookSystemUI(lpparam.classLoader, sPrefs);
                RecentsStackHooks.hookSystemUI(lpparam.classLoader, sPrefs);
                break;
            case PACKAGE_ANDROID:
                DoubleTapHwKeys.hook(lpparam.classLoader, sPrefs);
                break;
            case PACKAGE_OWN:
                XposedHelpers.findAndHookMethod(SETTINGS_OWN, lpparam.classLoader, "isActivated", XC_MethodReplacement.returnConstant(true));
                if (!sPrefs.getBoolean("can_read_prefs", false))
                    XposedHelpers.findAndHookMethod(SETTINGS_OWN, lpparam.classLoader, "isPrefsFileReadable", XC_MethodReplacement.returnConstant(false));
                break;
        }

        // Has to be hooked in every app as every app creates own instances of the Notification.Builder
        NotificationsHooks.hook(lpparam.classLoader, sPrefs);

    }

    @Override
    public void handleInitPackageResources(XC_InitPackageResources.InitPackageResourcesParam resparam) throws Throwable {

        switch (resparam.packageName) {
            case PACKAGE_SYSTEMUI:
                NotificationsHooks.hookResSystemui(resparam, sPrefs, sModulePath);
                StatusBarHeaderHooks.hookResSystemui(resparam, sPrefs, sModulePath);
                break;
        }

        // Has too be hooked in every app too for some reason, probably
        // because every hook only applies to the current process
        NotificationsHooks.hookResAndroid(resparam, sPrefs);

    }

}
