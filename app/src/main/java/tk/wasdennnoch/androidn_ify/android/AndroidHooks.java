package tk.wasdennnoch.androidn_ify.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.XposedHook;

public class AndroidHooks {

    private static final String TAG = "AndroidHooks";
    private static final String ACTION_SCREENSHOT = "tk.wasdennnoch.androidn_ify.action.ACTION_SCREENSHOT";

    private static Object mPhoneWindowManager;

    private static BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case ACTION_SCREENSHOT:
                    XposedHelpers.callMethod(mPhoneWindowManager, "takeScreenshot");
                    break;
            }
        }
    };

    public static void hook(ClassLoader classLoader) {
        try {
            Class<?> classPhoneWindowManager = XposedHelpers.findClass("com.android.server.policy.PhoneWindowManager", classLoader);
            XposedBridge.hookAllMethods(classPhoneWindowManager, "init", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    mPhoneWindowManager = param.thisObject;

                    Context context = (Context) param.args[0];
                    IntentFilter intentFilter = new IntentFilter();
                    intentFilter.addAction(ACTION_SCREENSHOT);
                    context.registerReceiver(mBroadcastReceiver, intentFilter);
                }
            });
        } catch (Throwable t) {
            XposedHook.logE(TAG, "Crash in screenshot hooks", t);
        }
    }

    public static void sendTakeScreenshot(Context context) {
        Intent intent = new Intent(ACTION_SCREENSHOT);
        context.sendBroadcast(intent);
    }
}
