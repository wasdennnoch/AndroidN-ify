package tk.wasdennnoch.androidn_ify.recents.doubletap;

import android.app.ActivityManager;
import android.content.Context;
import android.hardware.input.InputManager;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.view.KeyEvent;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import tk.wasdennnoch.androidn_ify.XposedHook;

// Huge thanks to Peter Gregus from the GravityBox Project (C3C076@xda) for this code!
public class DoubleTapHwKeys extends DoubleTapBase {

    private static final String TAG = "DoubleTapHwKeys";

    private static final String CLASS_WINDOW_STATE = "android.view.WindowManagerPolicy$WindowState";
    private static final String CLASS_WINDOW_MANAGER_FUNCS = "android.view.WindowManagerPolicy.WindowManagerFuncs";
    private static final String CLASS_IWINDOW_MANAGER = "android.view.IWindowManager";

    private static Object mPhoneWindowManager;
    private static Context mContext;
    private static Handler mHandler;

    private static boolean mRecentsKeyPressed = false;
    private static boolean mIsRecentsLongPressed = false;
    private static boolean mIsRecentsDoubleTap = false;
    private static boolean mWasRecentsDoubleTap = false;
    private static Runnable mRecentsDoubleTapReset = new Runnable() {
        @Override
        public void run() {
            mIsRecentsDoubleTap = false;
            // doubletap timed out and since we blocked default RECENTS key action while waiting for doubletap
            // let's inject it now additionally, but only in case it's not still pressed as we might still be waiting
            // for long-press action
            if (!mRecentsKeyPressed) {
                XposedHook.logD(TAG, "RECENTS key double tap timed out and key not pressed; injecting RECENTS key");
                injectKey(KeyEvent.KEYCODE_APP_SWITCH);
            }
        }
    };
    private static XC_MethodHook initHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            mPhoneWindowManager = param.thisObject;
            mContext = (Context) XposedHelpers.getObjectField(mPhoneWindowManager, "mContext");
            mHandler = (Handler) XposedHelpers.getObjectField(mPhoneWindowManager, "mHandler");
        }
    };
    private static XC_MethodHook interceptKeyBeforeDispatchingHook = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            if ((Boolean) XposedHelpers.callMethod(mPhoneWindowManager, "keyguardOn")) return;

            KeyEvent event = (KeyEvent) param.args[1];
            int keyCode = event.getKeyCode();
            boolean down = event.getAction() == KeyEvent.ACTION_DOWN;
            boolean isFromSystem = (event.getFlags() & KeyEvent.FLAG_FROM_SYSTEM) != 0;
            XposedHook.logD(TAG, "interceptKeyBeforeDispatching: keyCode=" + keyCode +
                    "; isInjected=" + (((Integer) param.args[2] & 0x01000000) != 0) +
                    "; fromSystem=" + isFromSystem);

            if (keyCode == KeyEvent.KEYCODE_APP_SWITCH && isFromSystem && !isTaskLocked()) {

                if (!down) {
                    mRecentsKeyPressed = false;
                    if (mIsRecentsLongPressed) {
                        mIsRecentsLongPressed = false;
                    } else if (event.getRepeatCount() == 0) {
                        if (mIsRecentsDoubleTap) {
                            // we are still waiting for double-tap
                            XposedHook.logD(TAG, "RECENTS doubletap pending. Ignoring.");
                        } else if (!mWasRecentsDoubleTap && !event.isCanceled()) {
                            XposedHook.logD(TAG, "Triggering original DOWN/UP events for RECENTS key");
                            injectKey(KeyEvent.KEYCODE_APP_SWITCH);
                        }
                    }
                } else if (event.getRepeatCount() == 0) {
                    mRecentsKeyPressed = true;
                    mWasRecentsDoubleTap = mIsRecentsDoubleTap;
                    if (mIsRecentsDoubleTap) {
                        switchToLastApp(mContext, mHandler);
                        mHandler.removeCallbacks(mRecentsDoubleTapReset);
                        mIsRecentsDoubleTap = false;
                    } else {
                        mIsRecentsLongPressed = false;
                        mIsRecentsDoubleTap = true;
                        mHandler.postDelayed(mRecentsDoubleTapReset, mDoubletapSpeed);

                    }
                }
                param.setResult(-1);
            }

        }
    };

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {

        Class<?> PhoneWindowManager = XposedHelpers.findClass("com.android.server.policy.PhoneWindowManager", lpparam.classLoader);

        try {
            XposedHelpers.findAndHookMethod(PhoneWindowManager, "init", Context.class, CLASS_IWINDOW_MANAGER, CLASS_WINDOW_MANAGER_FUNCS, initHook);
            XposedHelpers.findAndHookMethod(PhoneWindowManager, "interceptKeyBeforeDispatching", CLASS_WINDOW_STATE, KeyEvent.class, int.class, interceptKeyBeforeDispatchingHook);
        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error hooking init or interceptKeyBeforeDispatching", t);
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

    private static boolean isTaskLocked() {
        return Build.VERSION.SDK_INT >= 23 && getActivityManager(mContext).getLockTaskModeState() != ActivityManager.LOCK_TASK_MODE_NONE;
    }

}
