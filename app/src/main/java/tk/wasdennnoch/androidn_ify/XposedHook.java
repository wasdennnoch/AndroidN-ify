package tk.wasdennnoch.androidn_ify;

import android.os.Build;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.util.Slog;

import com.android.internal.os.BatteryStatsImpl;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import tk.wasdennnoch.androidn_ify.android.AndroidHooks;
import tk.wasdennnoch.androidn_ify.google.AssistantHooks;
import tk.wasdennnoch.androidn_ify.phone.emergency.EmergencyHooks;
import tk.wasdennnoch.androidn_ify.settings.SettingsHooks;
import tk.wasdennnoch.androidn_ify.systemui.SystemUIHooks;
import tk.wasdennnoch.androidn_ify.systemui.notifications.NotificationHooks;
import tk.wasdennnoch.androidn_ify.systemui.notifications.NotificationPanelHooks;
import tk.wasdennnoch.androidn_ify.systemui.notifications.StatusBarHeaderHooks;
import tk.wasdennnoch.androidn_ify.systemui.notifications.stack.StackScrollAlgorithmHooks;
import tk.wasdennnoch.androidn_ify.systemui.qs.tiles.misc.LiveDisplayObserver;
import tk.wasdennnoch.androidn_ify.systemui.recents.doubletap.DoubleTapHwKeys;
import tk.wasdennnoch.androidn_ify.systemui.recents.doubletap.DoubleTapSwKeys;
import tk.wasdennnoch.androidn_ify.systemui.recents.navigate.RecentsNavigation;
import tk.wasdennnoch.androidn_ify.systemui.recents.stack.RecentsStackHooks;
import tk.wasdennnoch.androidn_ify.systemui.screenshot.ScreenshotHooks;
import tk.wasdennnoch.androidn_ify.utils.ConfigUtils;
import tk.wasdennnoch.androidn_ify.utils.PermissionGranter;
import tk.wasdennnoch.androidn_ify.utils.RomUtils;

@SuppressWarnings("WeakerAccess")
public class XposedHook implements IXposedHookLoadPackage, IXposedHookZygoteInit, IXposedHookInitPackageResources {

    private static final String TAG = "XposedHook";
    private static String LOG_FORMAT = "[Android N-ify] %1$s %2$s: %3$s";
    public static final String PACKAGE_ANDROID = "android";
    public static final String PACKAGE_SYSTEMUI = "com.android.systemui";
    public static final String PACKAGE_SETTINGS = "com.android.settings";
    public static final String PACKAGE_PHONE = "com.android.phone";
    public static final String PACKAGE_GOOGLE = "com.google.android.googlequicksearchbox";
    public static final String PACKAGE_OWN = "tk.wasdennnoch.androidn_ify";
    public static final String SETTINGS_OWN = PACKAGE_OWN + ".ui.SettingsActivity";

    public static final String ACTION_MARK_UNSTABLE = "tk.wasdennnoch.androidn_ify.action.ACTION_MARK_UNSTABLE";

    public static boolean debug = false;
    private static String sModulePath;
    private static XSharedPreferences sPrefs;

    public static void markUnstable() {
        LOG_FORMAT = "[Android N-ify] [UNSTABLE] %1$s %2$s: %3$s";
        logE(TAG, "MARK_UNSTABLE", null);
    }

    public static void logE(String tag, String msg, Throwable t) {
        XposedBridge.log(String.format(LOG_FORMAT, "[ERROR]", tag, msg));
        if (t != null)
            XposedBridge.log(t);
    }

    public static void logW(String tag, String msg) {
        XposedBridge.log(String.format(LOG_FORMAT, "[WARNING]", tag, msg));
    }

    public static void logI(String tag, String msg) {
        XposedBridge.log(String.format(LOG_FORMAT, "[INFO]", tag, msg));
    }

    public static void logD(String tag, String msg) {
        if (debug) XposedBridge.log(String.format(LOG_FORMAT, "[DEBUG]", tag, msg));
    }

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        sModulePath = startupParam.modulePath;
        sPrefs = new XSharedPreferences(PACKAGE_OWN);
        RomUtils.init(sPrefs);

        logI(TAG, "Version " + BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")");
        //noinspection PointlessBooleanExpression
        if (BuildConfig.OFFICIAL_BUILD) {
            logI(TAG, "Official Build; Release: " + !BuildConfig.DEBUG + " (" + BuildConfig.BUILD_TYPE + ")");
            if (BuildConfig.DEBUG)
                logI(TAG, "Build Time: " + BuildConfig.BUILD_TIME);
        } else if (BuildConfig.AUTOMATED_BUILD){
            logI(TAG, "Automated Build; Version: " + BuildConfig.BUILD_NUMBER);
            logI(TAG, "Build Time: " + BuildConfig.BUILD_TIME);
            logI(TAG, "Git SHA: " + BuildConfig.GIT_COMMIT);
            logI(TAG, "Git info: \n | " + BuildConfig.GIT_INFO);
        } else {
            logI(TAG, "3rd Party Build; Version: " + BuildConfig.BUILD_NUMBER);
            logI(TAG, "Git SHA: " + BuildConfig.GIT_COMMIT);
            logI(TAG, "Git info: \n | " + BuildConfig.GIT_INFO);
        }
        if (ConfigUtils.isExperimental(sPrefs)) {
            logI(TAG, "Experimental features enabled");
        }

        logI(TAG, "---- Device info ----");
        logI(TAG, "SDK Version: " + Build.VERSION.SDK_INT);
        logI(TAG, "Build ID: " + Build.DISPLAY);
        logI(TAG, "Manufacturer: " + Build.MANUFACTURER);
        logI(TAG, "Brand: " + Build.BRAND);
        logI(TAG, "Model: " + Build.MODEL);

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
                SettingsHooks.hook(lpparam.classLoader);
                break;
            case PACKAGE_SYSTEMUI:
                SystemUIHooks.hookSystemUI(lpparam.classLoader);
                ScreenshotHooks.hook(lpparam.classLoader);
                StatusBarHeaderHooks.hook(lpparam.classLoader);
                NotificationPanelHooks.hook(lpparam.classLoader);
                StackScrollAlgorithmHooks.hook(lpparam.classLoader);
                NotificationHooks.hookSystemUI(lpparam.classLoader);
                RecentsStackHooks.hookSystemUI(lpparam.classLoader);
                RecentsNavigation.hookSystemUI(lpparam.classLoader);
                DoubleTapSwKeys.hook(lpparam.classLoader);
                break;
            case PACKAGE_ANDROID:
                AndroidHooks.hook(lpparam.classLoader);
                DoubleTapHwKeys.hook(lpparam.classLoader);
                LiveDisplayObserver.hook(lpparam.classLoader);
                PermissionGranter.initAndroid(lpparam.classLoader);
                if (!ConfigUtils.M)
                    hookBatteryStats(lpparam.classLoader);
                break;
            case PACKAGE_PHONE:
                EmergencyHooks.hook(lpparam.classLoader);
                break;
            case PACKAGE_OWN:
                XposedHelpers.findAndHookMethod(SETTINGS_OWN, lpparam.classLoader, "isActivated", XC_MethodReplacement.returnConstant(true));
                if (!sPrefs.getBoolean("can_read_prefs", false))
                    XposedHelpers.findAndHookMethod(SETTINGS_OWN, lpparam.classLoader, "isPrefsFileReadable", XC_MethodReplacement.returnConstant(false));
                break;
            case PACKAGE_GOOGLE:
                if (ConfigUtils.M && ConfigUtils.assistant().enable_assistant) {
                    AssistantHooks.hook(lpparam.classLoader);
                }
                break;
        }

        // Has to be hooked in every app as every app creates own instances of the Notification.Builder
        NotificationHooks.hook(lpparam.classLoader);


        // CM Custom Tile API; May implement it later, disabled for now
        if (ConfigUtils.qs().enable_qs_editor) {
            try {
                Class<?> classCMStatusBarManager = XposedHelpers.findClass("cyanogenmod.app.CMStatusBarManager", lpparam.classLoader);
                XposedBridge.hookAllMethods(classCMStatusBarManager, "publishTile", XC_MethodReplacement.DO_NOTHING);
                XposedBridge.hookAllMethods(classCMStatusBarManager, "publishTileAsUser", XC_MethodReplacement.DO_NOTHING);
            } catch (Throwable ignore) {
            }
        }

    }

    private static void hookBatteryStats(ClassLoader classLoader) {
        XposedHelpers.findAndHookMethod("com.android.server.am.BatteryStatsService", classLoader, "getStatisticsStream", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Parcel out = Parcel.obtain();
                BatteryStatsImpl mStats = (BatteryStatsImpl) XposedHelpers.getObjectField(param.thisObject, "mStats");
                mStats.writeToParcel(out, 0);
                byte[] data = out.marshall();
                out.recycle();
                try {
                    param.setResult(XposedHelpers.callStaticMethod(ParcelFileDescriptor.class, "fromData", data, "stats"));
                } catch (Exception e) {
                    Slog.w(TAG, "Unable to create shared memory", e);
                    param.setResult(null);
                }

            }
        });
    }

    @Override
    public void handleInitPackageResources(XC_InitPackageResources.InitPackageResourcesParam resparam) throws Throwable {

        switch (resparam.packageName) {
            case PACKAGE_SETTINGS:
                SettingsHooks.hookRes(resparam, sModulePath);
                break;
            case PACKAGE_SYSTEMUI:
                NotificationHooks.hookResSystemui(resparam, sModulePath);
                StatusBarHeaderHooks.hookResSystemui(resparam, sModulePath);
                RecentsStackHooks.hookResSystemui(resparam, sModulePath);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    RecentsNavigation.hookResSystemui(resparam);
                }
                SystemUIHooks.hookResSystemUI(resparam, sModulePath);
                break;
        }

        // Has to be hooked in every app because every hook only applies to the current process
        ConfigUtils.notifications().loadBlacklistedApps();
        if (!ConfigUtils.notifications().blacklistedApps.contains(resparam.packageName)) {
            NotificationHooks.hookResAndroid(resparam, sModulePath);
        }
    }

    public static String getModulePath() {
        return sModulePath;
    }
}
