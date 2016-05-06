package tk.wasdennnoch.androidn_ify.recents.doubletap;

import android.content.Context;
import android.hardware.input.InputManager;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.view.IWindowManager;
import android.view.KeyEvent;
import android.view.WindowManagerPolicy;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.XposedHook;

public class DoubleTapHwKeys extends DoubleTapBase {

    private static final String TAG = "DoubleTapHwKeys";

    private static final String CLASS_PHONE_WINDOW_MANAGER;

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
            XposedHook.logD(TAG, "Double tap timed out after " + mDoubletapSpeed + "ms, injecting original KeyEvent");
            mWasPressed = false;
            injectKey(KeyEvent.KEYCODE_APP_SWITCH);
        }
    };

    private static XC_MethodHook initHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            mPhoneWindowManager = param.thisObject;
            mContext = (Context) XposedHelpers.getObjectField(mPhoneWindowManager, "mContext");
            mHandler = (Handler) XposedHelpers.getObjectField(mPhoneWindowManager, "mHandler");
            // No need to unregister this because the system process will last "forever"
            registerReceiver(mContext);
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
                    XposedHook.logD(TAG, "HW recents clicked");
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

            XposedHelpers.findAndHookMethod(classPhoneWindowManager, "init", Context.class, IWindowManager.class, WindowManagerPolicy.WindowManagerFuncs.class, initHook);

            prefs.reload();
            loadPrefDoubleTapSpeed(prefs);
            if (prefs.getBoolean("enable_recents_double_tap", true)) {

                XposedHelpers.findAndHookMethod(classPhoneWindowManager, "interceptKeyBeforeDispatching", WindowManagerPolicy.WindowState.class, KeyEvent.class, int.class, interceptKeyBeforeDispatchingHook);

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
