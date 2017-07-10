package tk.wasdennnoch.androidn_ify.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.os.UserHandle;
import android.view.KeyEvent;
import android.view.ViewConfiguration;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.utils.ConfigUtils;

import static android.view.WindowManagerPolicy.ACTION_PASS_TO_USER;

public class AndroidHooks {

    private static final String TAG = "AndroidHooks";
    private static final String ACTION_SCREENSHOT = "tk.wasdennnoch.androidn_ify.action.ACTION_SCREENSHOT";
    private static final String ACTION_VOICE_ASSIST = "android.intent.action.VOICE_ASSIST";
    private static final String PHONE_WINDOW_MANAGER_CLASS = ConfigUtils.M ?
            "com.android.server.policy.PhoneWindowManager" :
            "com.android.internal.policy.impl.PhoneWindowManager";

    private static final int MSG_BACK_LONG_PRESS = 18;
    private static final int MSG_BACK_DELAYED_PRESS = 20;
    private static final int PANIC_PRESS_BACK_COUNT = ConfigUtils.others().panic_presses;
    private static final int MULTI_PRESS_TIMEOUT = ConfigUtils.others().panic_timeout;

    private static Object mPhoneWindowManager;
    private static Handler mHandler;
    private static Context mContext;

    private static final boolean mLongPressOnBackBehavior = ConfigUtils.others().back_press_voice_assist;
    private static final boolean mPanicPressOnBackBehavior = ConfigUtils.others().panic_detection;
    private static boolean mBackKeyHandled;
    private static int mBackKeyPressCounter;

    private static BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case ACTION_SCREENSHOT:
                    try {
                        XposedHelpers.callMethod(mPhoneWindowManager, "takeScreenshot");
                    } catch (Throwable t) {
                        XposedHook.logE(TAG, "Error while taking screenshot", t);
                    }
                    break;
            }
        }
    };

    public static void hook(ClassLoader classLoader) {
        try {
            Class<?> classPhoneWindowManager = XposedHelpers.findClass(PHONE_WINDOW_MANAGER_CLASS, classLoader);
            Class<?> classPolicyHandler = XposedHelpers.findClass(PHONE_WINDOW_MANAGER_CLASS + "$PolicyHandler", classLoader);
            XposedBridge.hookAllMethods(classPhoneWindowManager, "init", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    mPhoneWindowManager = param.thisObject;
                    mHandler = (Handler) XposedHelpers.getObjectField(mPhoneWindowManager, "mHandler");
                    mContext = (Context) XposedHelpers.getObjectField(mPhoneWindowManager, "mContext");
                    Context context = (Context) param.args[0];
                    IntentFilter intentFilter = new IntentFilter();
                    intentFilter.addAction(ACTION_SCREENSHOT);
                    context.registerReceiver(mBroadcastReceiver, intentFilter);
                }
            });
            if (ConfigUtils.others().panic_detection || ConfigUtils.others().back_press_voice_assist) {
                if (!ConfigUtils.L1)
                    return;
                XposedHelpers.findAndHookMethod(classPolicyHandler, "handleMessage", Message.class, handleMessageHook);
                XposedHelpers.findAndHookMethod(classPhoneWindowManager, "interceptKeyBeforeQueueing", KeyEvent.class, int.class, interceptKeyBeforeQueueingHook);
            }
        } catch (Throwable t) {
            XposedHook.logE(TAG, "Crash in android hooks", t);
        }
    }

    private static final XC_MethodHook handleMessageHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            Message msg = (Message) param.args[0];
            switch (msg.what) {
                case MSG_BACK_LONG_PRESS:
                    backLongPress();
                    finishBackKeyPress();
                    break;
                case MSG_BACK_DELAYED_PRESS:
                    backMultiPressAction((Long) msg.obj, msg.arg1);
                    finishBackKeyPress();
                    break;
            }
        }
    };

    private static final XC_MethodHook interceptKeyBeforeQueueingHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            KeyEvent event = (KeyEvent) param.args[0];
            final int keyCode = event.getKeyCode();
            final boolean down = event.getAction() == KeyEvent.ACTION_DOWN;
            if (keyCode == KeyEvent.KEYCODE_BACK)
                if (down) {
                    interceptBackKeyDown();
                } else {
                    boolean handled = interceptBackKeyUp(event);

                    // Don't pass back press to app if we've already handled it
                    if (handled) {
                        param.setResult(~ACTION_PASS_TO_USER);
                    }
                }
        }
    };

    private static void cancelPendingBackKeyAction() {
        if (!mBackKeyHandled) {
            mBackKeyHandled = true;
            mHandler.removeMessages(MSG_BACK_LONG_PRESS);
        }
    }

    private static void backLongPress() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Constructor<?> cons = UserHandle.class.getConstructor(int.class);

        mBackKeyHandled = true;

        if (mLongPressOnBackBehavior) {
            Intent intent = new Intent(ACTION_VOICE_ASSIST);
            XposedHelpers.callMethod(mPhoneWindowManager, "startActivityAsUser", intent, cons.newInstance(-3 /*CURRENT_OR_SELF*/));
        }
    }

    private static void backMultiPressAction(long eventTime, int count) {
        if (count >= PANIC_PRESS_BACK_COUNT) {
            if (mPanicPressOnBackBehavior) {
                XposedHelpers.callMethod(mPhoneWindowManager, "launchHomeFromHotKey");
            }
        }
    }

    private static void interceptBackKeyDown() {
        // Reset back key state for long press
        mBackKeyHandled = false;

        // Cancel multi-press detection timeout.
        if (mPanicPressOnBackBehavior) {
            if (mBackKeyPressCounter != 0
                    && mBackKeyPressCounter < PANIC_PRESS_BACK_COUNT) {
                mHandler.removeMessages(MSG_BACK_DELAYED_PRESS);
            }
        }

        if (mLongPressOnBackBehavior) {
            Message msg = mHandler.obtainMessage(MSG_BACK_LONG_PRESS);
            msg.setAsynchronous(true);
            mHandler.sendMessageDelayed(msg,
                    (long) XposedHelpers.callMethod(ViewConfiguration.get(mContext), "getDeviceGlobalActionKeyTimeout"));
        }
    }

    // returns true if the key was handled and should not be passed to the user
    private static boolean interceptBackKeyUp(KeyEvent event) {
        // Cache handled state
        boolean handled = mBackKeyHandled;

        if (mPanicPressOnBackBehavior) {
            // Check for back key panic press
            ++mBackKeyPressCounter;

            final long eventTime = event.getDownTime();

            if (mBackKeyPressCounter <= PANIC_PRESS_BACK_COUNT) {
                // This could be a multi-press.  Wait a little bit longer to confirm.
                Message msg = mHandler.obtainMessage(MSG_BACK_DELAYED_PRESS,
                        mBackKeyPressCounter, 0, eventTime);
                msg.setAsynchronous(true);
                mHandler.sendMessageDelayed(msg, MULTI_PRESS_TIMEOUT);
            }
        }

        // Reset back long press state
        cancelPendingBackKeyAction();

        return handled;
    }

    private static void finishBackKeyPress() {
        mBackKeyPressCounter = 0;
    }

    public static void sendTakeScreenshot(Context context) {
        Intent intent = new Intent(ACTION_SCREENSHOT);
        context.sendBroadcast(intent);
    }
}
