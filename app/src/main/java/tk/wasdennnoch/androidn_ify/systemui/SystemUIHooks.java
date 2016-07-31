package tk.wasdennnoch.androidn_ify.systemui;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Process;
import android.provider.Settings;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.systemui.qs.tiles.helper.BatteryInfoManager;
import tk.wasdennnoch.androidn_ify.ui.SettingsActivity;
import tk.wasdennnoch.androidn_ify.utils.ConfigUtils;
import tk.wasdennnoch.androidn_ify.utils.RomUtils;

public class SystemUIHooks {

    private static final String TAG = "SystemUIHooks";

    private static final String CLASS_SYSTEMUI_APPLICATION = "com.android.systemui.SystemUIApplication";

    public static BatteryInfoManager batteryInfoManager;

    public static void hookSystemUI(ClassLoader classLoader) {

        XposedHelpers.findAndHookMethod(CLASS_SYSTEMUI_APPLICATION, classLoader, "onCreate", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                XposedHook.logD(TAG, "SystemUIApplication created, init");

                ConfigUtils.getInstance().reload(); // Start loading prefs in a background thread to have them ready as quick as possible

                final Application app = (Application) param.thisObject;
                final Handler handler = new Handler(app.getMainLooper());

                batteryInfoManager = new BatteryInfoManager(app);

                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction(SettingsActivity.ACTION_GENERAL);
                intentFilter.addAction(SettingsActivity.ACTION_FIX_INVERSION);
                intentFilter.addAction(SettingsActivity.ACTION_KILL_SYSTEMUI);
                app.registerReceiver(new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        XposedHook.logD(TAG, "Broadcast received, action: " + intent.getAction());
                        switch (intent.getAction()) {
                            case SettingsActivity.ACTION_GENERAL:
                                if (intent.hasExtra(SettingsActivity.EXTRA_GENERAL_DEBUG_LOG)) {
                                    XposedHook.debug = intent.getBooleanExtra(SettingsActivity.EXTRA_GENERAL_DEBUG_LOG, false);
                                    XposedHook.logI(TAG, "Debug log " + (XposedHook.debug ? "enabled" : "disabled"));
                                }
                                break;
                            case SettingsActivity.ACTION_FIX_INVERSION:
                                Settings.Secure.putInt(app.getContentResolver(), Settings.Secure.ACCESSIBILITY_DISPLAY_INVERSION_ENABLED, 0);
                                break;
                            case SettingsActivity.ACTION_KILL_SYSTEMUI:
                                handler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        Process.killProcess(Process.myPid());
                                    }
                                }, 100);
                                break;
                        }
                    }
                }, intentFilter);

                // Give the settings enough time to load in the background
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        XposedHook.debug = ConfigUtils.getInstance().getPrefs().getBoolean("debug_log", false);
                        RomUtils.init(ConfigUtils.getInstance().getPrefs());
                    }
                }, 2000);
            }
        });

    }

}
