package tk.wasdennnoch.androidn_ify.recents;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.hardware.input.InputManager;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.widget.Toast;

import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import tk.wasdennnoch.androidn_ify.XposedHook;

public class RecentsHooks {

    private static final String CLASS_WINDOW_STATE = "android.view.WindowManagerPolicy$WindowState";
    private static final String CLASS_WINDOW_MANAGER_FUNCS = "android.view.WindowManagerPolicy.WindowManagerFuncs";
    private static final String CLASS_IWINDOW_MANAGER = "android.view.IWindowManager";

    private static boolean mRecentsKeyPressed = false;
    private static boolean mIsRecentsLongPressed = false;
    private static boolean mIsRecentsDoubleTap = false;
    private static boolean mWasRecentsDoubleTap = false;
    private static int mDoubletapSpeed = 400;
    private static Runnable mRecentsLongPress = new Runnable() {
        @Override
        public void run() {
            if (XposedHook.debug) XposedHook.log("mRecentsLongPress runnable launched");
            mIsRecentsLongPressed = true;
        }
    };
    private static Runnable mRecentsDoubleTapReset = new Runnable() {
        @Override
        public void run() {
            mIsRecentsDoubleTap = false;
            // doubletap timed out and since we blocked default RECENTS key action while waiting for doubletap
            // let's inject it now additionally, but only in case it's not still pressed as we might still be waiting
            // for long-press action
            if (!mRecentsKeyPressed) {
                if (XposedHook.debug) XposedHook.log("RECENTS key double tap timed out and key not pressed; injecting RECENTS key");
                injectKey(KeyEvent.KEYCODE_APP_SWITCH);
            }
        }
    };

    private static Object mPhoneWindowManager;
    private static Context mContext;
    private static Handler mHandler;
    private static ActivityManager mAm;

    public static void hookInterceptKeyBeforeDispatching(XC_LoadPackage.LoadPackageParam lpparam) {

        Class<?> PhoneWindowManager = XposedHelpers.findClass("com.android.server.policy.PhoneWindowManager", lpparam.classLoader);

        try {
            XposedHelpers.findAndHookMethod(PhoneWindowManager, "init", Context.class, CLASS_IWINDOW_MANAGER, CLASS_WINDOW_MANAGER_FUNCS, initHook);
            XposedHelpers.findAndHookMethod(PhoneWindowManager, "interceptKeyBeforeDispatching", CLASS_WINDOW_STATE, KeyEvent.class, int.class, interceptKeyBeforeDispatchingHook);
        } catch (Throwable t) {
            XposedHook.logE("Error hooking init or interceptKeyBeforeDispatching", t);
        }

    }

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
            if (XposedHook.debug) XposedHook.log("interceptKeyBeforeDispatching: keyCode=" + keyCode +
                    "; isInjected=" + (((Integer) param.args[2] & 0x01000000) != 0) +
                    "; fromSystem=" + isFromSystem);

            if (keyCode == KeyEvent.KEYCODE_APP_SWITCH && isFromSystem && !isTaskLocked()) {

                if (!down) {
                    mRecentsKeyPressed = false;
                    mHandler.removeCallbacks(mRecentsLongPress);
                    if (mIsRecentsLongPressed) {
                        mIsRecentsLongPressed = false;
                    } else if (event.getRepeatCount() == 0) {
                        if (mIsRecentsDoubleTap) {
                            // we are still waiting for double-tap
                            if (XposedHook.debug) XposedHook.log("RECENTS doubletap pending. Ignoring.");
                        } else if (!mWasRecentsDoubleTap && !event.isCanceled()) {
                                if (XposedHook.debug) XposedHook.log("Triggering original DOWN/UP events for RECENTS key");
                                injectKey(KeyEvent.KEYCODE_APP_SWITCH);
                        }
                    }
                } else if (event.getRepeatCount() == 0) {
                    mRecentsKeyPressed = true;
                    mWasRecentsDoubleTap = mIsRecentsDoubleTap;
                    if (mIsRecentsDoubleTap) {
                        switchToLastApp();
                        mHandler.removeCallbacks(mRecentsDoubleTapReset);
                        mIsRecentsDoubleTap = false;
                    } else {
                        mIsRecentsLongPressed = false;
                        mIsRecentsDoubleTap = false;
                        mIsRecentsDoubleTap = true;
                        mHandler.postDelayed(mRecentsDoubleTapReset, mDoubletapSpeed);

                    }
                }
                param.setResult(-1);
                return;
            }

        }
    };

    public static void injectKey(final int keyCode) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    final long eventTime = SystemClock.uptimeMillis();
                    final InputManager inputManager = (InputManager)
                            mContext.getSystemService(Context.INPUT_SERVICE);
                    XposedHelpers.callMethod(inputManager, "injectInputEvent",
                            new KeyEvent(eventTime - 50, eventTime - 50, KeyEvent.ACTION_DOWN,
                                    keyCode, 0), 0);
                    XposedHelpers.callMethod(inputManager, "injectInputEvent",
                            new KeyEvent(eventTime - 50, eventTime - 25, KeyEvent.ACTION_UP,
                                    keyCode, 0), 0);
                } catch (Throwable t) {
                    XposedHook.logE("injectKey failed", t);
                }
            }
        });
    }

    private static ActivityManager getActivityManager() {
        if (mAm == null) {
            mAm = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        }
        return mAm;
    }

    private static boolean isTaskLocked() {
        return Build.VERSION.SDK_INT >= 23 && getActivityManager().getLockTaskModeState() != ActivityManager.LOCK_TASK_MODE_NONE;
    }

    private static void switchToLastApp() {
        mHandler.post(
                new Runnable() {
                    @Override
                    public void run() {
                        int lastAppId = 0;
                        int looper = 1;
                        String packageName;
                        final Intent intent = new Intent(Intent.ACTION_MAIN);
                        final ActivityManager am = getActivityManager();
                        String defaultHomePackage = "com.android.launcher";
                        intent.addCategory(Intent.CATEGORY_HOME);
                        final ResolveInfo res = mContext.getPackageManager().resolveActivity(intent, 0);
                        if (res.activityInfo != null && !res.activityInfo.packageName.equals("android")) {
                            defaultHomePackage = res.activityInfo.packageName;
                        }
                        List<ActivityManager.RunningTaskInfo> tasks = am.getRunningTasks(5);
                        // lets get enough tasks to find something to switch to
                        // Note, we'll only get as many as the system currently has - up to 5
                        while ((lastAppId == 0) && (looper < tasks.size())) {
                            packageName = tasks.get(looper).topActivity.getPackageName();
                            if (!packageName.equals(defaultHomePackage) && !packageName.equals("com.android.systemui")) {
                                lastAppId = tasks.get(looper).id;
                            }
                            looper++;
                        }
                        if (lastAppId != 0) {
                            am.moveTaskToFront(lastAppId, ActivityManager.MOVE_TASK_NO_USER_ACTION);
                        } else {
                            Toast.makeText(mContext, "No previous app", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }

}
