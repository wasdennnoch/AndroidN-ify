package tk.wasdennnoch.androidn_ify.systemui;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Process;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.notifications.qs.BatteryInfoManager;
import tk.wasdennnoch.androidn_ify.ui.SettingsActivity;

public class SystemUIHooks {

    private static final String TAG = "SystemUIHooks";

    private static final String CLASS_SYSTEMUI_APPLICATION = "com.android.systemui.SystemUIApplication";

    public static BatteryInfoManager batteryInfoManager;

    public static void hookSystemUI(ClassLoader classLoader) {

        XposedHelpers.findAndHookMethod(CLASS_SYSTEMUI_APPLICATION, classLoader, "onCreate", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                XposedHook.logD(TAG, "prepareNavigationBarViewHook called");

                Application app = (Application) param.thisObject;
                final Handler handler = new Handler(app.getMainLooper());

                batteryInfoManager = new BatteryInfoManager(app);

                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction(SettingsActivity.ACTION_GENERAL);
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
                            case SettingsActivity.ACTION_KILL_SYSTEMUI:
                                handler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        XposedHook.logD(TAG, "Kill broadcast received, sending kill signal");
                                        Process.sendSignal(Process.myPid(), Process.SIGNAL_KILL);
                                    }
                                }, 100);
                                break;
                        }
                    }
                }, intentFilter);
            }
        });

    }

}
