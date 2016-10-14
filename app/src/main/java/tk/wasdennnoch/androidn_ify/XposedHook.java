package tk.wasdennnoch.androidn_ify;

import android.content.Context;
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
import tk.wasdennnoch.androidn_ify.utils.ConfigUtils;
import tk.wasdennnoch.androidn_ify.utils.PermissionGranter;
import tk.wasdennnoch.androidn_ify.utils.RomUtils;

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
    public static final String GOOGLE_APP_VERSION_REGEX = "6\\.6\\.1[46]\\.21\\.[a-z]+[0-9]*";

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
        } else {
            logI(TAG, "3rd Party Build; Version: " + BuildConfig.BUILD_NUMBER);
        }
        if (ConfigUtils.isExperimental(sPrefs)) {
            logI(TAG, "Experimental features enabled");
        }

        logDeviceInfo();

        if (!sPrefs.getBoolean("can_read_prefs", false)) {
            // With SELinux enforcing, it might happen that we don't have access
            // to the prefs file. Test this by reading a test key that should be
            // set to true. If it is false, we either can't read the file or the
            // user has never opened the preference screen before.
            logW(TAG, "Can't read prefs file, default values will be applied in hooks!");
        }
        debug = sPrefs.getBoolean("debug_log", false);
    }

    private void logDeviceInfo() {
        logI(TAG, "---- Device info ----");
        logI(TAG, "SDK Version: " + Build.VERSION.SDK_INT);
        logI(TAG, "Build ID: " + Build.DISPLAY);
        logI(TAG, "Manufacturer: " + Build.MANUFACTURER);
        logI(TAG, "Brand: " + Build.BRAND);
        logI(TAG, "Model: " + Build.MODEL);
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {

        switch (lpparam.packageName) {
            case PACKAGE_SETTINGS:
                SettingsHooks.hook(lpparam.classLoader);
                break;
            case PACKAGE_SYSTEMUI:
                SystemUIHooks.hookSystemUI(lpparam.classLoader);
                StatusBarHeaderHooks.hook(lpparam.classLoader);
                NotificationPanelHooks.hook(lpparam.classLoader);
                StackScrollAlgorithmHooks.hook(lpparam.classLoader);
                NotificationHooks.hookSystemUI(lpparam.classLoader);
                RecentsStackHooks.hookSystemUI(lpparam.classLoader);
                RecentsNavigation.hookSystemUI(lpparam.classLoader);
                DoubleTapSwKeys.hook(lpparam.classLoader);
                break;
            case PACKAGE_ANDROID:
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
                // #############################################################################
                // Thanks to XposedGELSettings for the following snippet (https://git.io/vP2Gw):
                Object activityThread = XposedHelpers.callStaticMethod(XposedHelpers.findClass("android.app.ActivityThread", null), "currentActivityThread");
                Context context = (Context) XposedHelpers.callMethod(activityThread, "getSystemContext");
                // #############################################################################
                if (ConfigUtils.M && context.getPackageManager().getPackageInfo(lpparam.packageName, 0).versionName.matches(GOOGLE_APP_VERSION_REGEX)) {
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

        /*ConfigUtils.notifications().loadSpoofAPIApps();
        if (ConfigUtils.notifications().spoofAPIApps.contains(lpparam.packageName)) {
            XposedHelpers.setStaticIntField(Build.VERSION.class, "SDK_INT", 24);
        }*/

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
            NotificationHooks.hookResAndroid(resparam);
        }
    }

}
