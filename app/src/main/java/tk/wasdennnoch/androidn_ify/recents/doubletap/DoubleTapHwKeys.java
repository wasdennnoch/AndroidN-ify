package tk.wasdennnoch.androidn_ify.recents.doubletap;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.input.InputManager;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.view.KeyEvent;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.settings.summaries.SummaryTweaks;
import tk.wasdennnoch.androidn_ify.ui.SettingsActivity;

public class DoubleTapHwKeys extends DoubleTapBase {

    private static final String TAG = "DoubleTapHwKeys";

    private static final String CLASS_PHONE_WINDOW_MANAGER;
    private static final String CLASS_WINDOW_STATE = "android.view.WindowManagerPolicy$WindowState";
    private static final String CLASS_WINDOW_MANAGER_FUNCS = "android.view.WindowManagerPolicy.WindowManagerFuncs";
    private static final String CLASS_IWINDOW_MANAGER = "android.view.IWindowManager";

    static {
        CLASS_PHONE_WINDOW_MANAGER = Build.VERSION.SDK_INT >= 23 ? "com.android.server.policy.PhoneWindowManager" : "com.android.internal.policy.impl.PhoneWindowManager";
    }

    private static Object mPhoneWindowManager;
    private static Context mContext;
    private static Handler mHandler;

    private static boolean mWasPressed = false;
    private static Runnable resetPressedState = new Runnable() {
        @Override
        public void run() {
            XposedHook.logD(TAG, "resetPressedState runnable: double-tap timed out, invoking original KeyEvent");
            mWasPressed = false;
            injectKey(KeyEvent.KEYCODE_APP_SWITCH);
        }
    };
    private static BroadcastReceiver sBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            XposedHook.logD(TAG, "Broadcast received: " + intent);
            switch (intent.getAction()) {
                // Needs to be here because the settings don't get informed about changes when
                // they aren't open (the BroadcastReceiver gets unregistered in onDestroy)
                case SettingsActivity.ACTION_SETTINGS_CHANGED:
                    if (intent.hasExtra(SettingsActivity.EXTRA_SETTINGS_FIX_SOUND_NOTIF_TILE))
                        SummaryTweaks.setFixSoundNotifTile(intent.getBooleanExtra(SettingsActivity.EXTRA_SETTINGS_FIX_SOUND_NOTIF_TILE, false));
                    break;
                case SettingsActivity.ACTION_RECENTS_CHANGED:
                    if (intent.hasExtra(SettingsActivity.EXTRA_RECENTS_DOUBLE_TAP_SPEED))
                        mDoubletapSpeed = intent.getIntExtra(SettingsActivity.EXTRA_RECENTS_DOUBLE_TAP_SPEED, 400);
                    break;
                case SettingsActivity.ACTION_GENERAL:
                    if (intent.hasExtra(SettingsActivity.EXTRA_GENERAL_DEBUG_LOG))
                        XposedHook.debug = intent.getBooleanExtra(SettingsActivity.EXTRA_GENERAL_DEBUG_LOG, false);
                    break;
            }
        }
    };

    private static XC_MethodHook initHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            mPhoneWindowManager = param.thisObject;
            mContext = (Context) XposedHelpers.getObjectField(mPhoneWindowManager, "mContext");
            mHandler = (Handler) XposedHelpers.getObjectField(mPhoneWindowManager, "mHandler");
            // No need to unregister this because the system process will last "forever"
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(SettingsActivity.ACTION_SETTINGS_CHANGED);
            intentFilter.addAction(SettingsActivity.ACTION_RECENTS_CHANGED);
            intentFilter.addAction(SettingsActivity.ACTION_GENERAL);
            mContext.registerReceiver(sBroadcastReceiver, intentFilter);
        }
    };
    private static XC_MethodHook interceptKeyBeforeDispatchingHook = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            if ((boolean) XposedHelpers.callMethod(mPhoneWindowManager, "keyguardOn")) return;

            KeyEvent event = (KeyEvent) param.args[1];
            int keyCode = event.getKeyCode();
            boolean down = event.getAction() == KeyEvent.ACTION_DOWN;
            boolean isFromSystem = (event.getFlags() & KeyEvent.FLAG_FROM_SYSTEM) != 0;
            XposedHook.logD(TAG, "interceptKeyBeforeDispatching: keyCode= " + keyCode +
                    "; keyCodeString=" + KeyEvent.keyCodeToString(keyCode) +
                    "; down= " + down +
                    "; repeatCount= " + event.getRepeatCount() +
                    "; isInjected= " + (((Integer) param.args[2] & 0x01000000) != 0) +
                    "; fromSystem= " + isFromSystem);

            if (keyCode == KeyEvent.KEYCODE_APP_SWITCH && isFromSystem && !isTaskLocked(mContext) && down && event.getRepeatCount() == 0) {
                if (!mWasPressed) {
                    mWasPressed = true;
                    mHandler.postDelayed(resetPressedState, mDoubletapSpeed);
                } else {
                    XposedHook.logD(TAG, "Double tap detected");
                    mHandler.removeCallbacks(resetPressedState);
                    mWasPressed = false;
                    switchToLastApp(mContext, mHandler);
                }
                param.setResult(-1);
            }

        }
    };

    public static void hook(ClassLoader classLoader, XSharedPreferences prefs) {
        try {

            Class<?> classPhoneWindowManager = XposedHelpers.findClass(CLASS_PHONE_WINDOW_MANAGER, classLoader);

            XposedHelpers.findAndHookMethod(classPhoneWindowManager, "init", Context.class, CLASS_IWINDOW_MANAGER, CLASS_WINDOW_MANAGER_FUNCS, initHook);

            prefs.reload();
            if (prefs.getBoolean("enable_recents_tweaks", true)) {

                XposedHelpers.findAndHookMethod(classPhoneWindowManager, "interceptKeyBeforeDispatching", CLASS_WINDOW_STATE, KeyEvent.class, int.class, interceptKeyBeforeDispatchingHook);

            }
        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error in hook", t);
        }
    }

    public static void injectKey(final int keyCode) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    final long eventTime = SystemClock.uptimeMillis();
                    final InputManager inputManager = (InputManager) mContext.getSystemService(Context.INPUT_SERVICE);
                    XposedHelpers.callMethod(inputManager, "injectInputEvent",
                            new KeyEvent(eventTime - 50, eventTime - 50, KeyEvent.ACTION_DOWN, keyCode, 0),
                            0);
                    XposedHelpers.callMethod(inputManager, "injectInputEvent",
                            new KeyEvent(eventTime - 50, eventTime - 25, KeyEvent.ACTION_UP, keyCode, 0),
                            0);
                } catch (Throwable t) {
                    XposedHook.logE(TAG, "injectKey failed", t);
                }
            }
        });
    }

}
