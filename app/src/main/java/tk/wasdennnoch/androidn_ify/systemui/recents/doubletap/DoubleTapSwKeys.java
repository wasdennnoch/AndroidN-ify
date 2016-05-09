package tk.wasdennnoch.androidn_ify.systemui.recents.doubletap;

import android.content.Context;
import android.os.Handler;
import android.view.View;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.utils.ConfigUtils;

public class DoubleTapSwKeys extends DoubleTapBase {

    private static final String TAG = "DoubleTapSwKeys";

    private static final String CLASS_PHONE_STATUS_BAR = "com.android.systemui.statusbar.phone.PhoneStatusBar";

    private static Context mContext;
    private static Handler mHandler;
    private static boolean sWasPressed = false;
    private static View.OnClickListener sOriginalRecentsClickListener;
    private static View sRecentsButton;
    private static Runnable sResetPressedStateRunnable = new Runnable() {
        @Override
        public void run() {
            XposedHook.logD(TAG, "Double tap timed out after " + mDoubletapSpeed + "ms, invoking original mRecentsClickListener");
            sWasPressed = false;
            sOriginalRecentsClickListener.onClick(sRecentsButton);
        }
    };

    private static XC_MethodHook prepareNavigationBarViewHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            XposedHook.logD(TAG, "prepareNavigationBarViewHook called");

            mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
            mHandler = (Handler) XposedHelpers.getObjectField(param.thisObject, "mHandler");
            sOriginalRecentsClickListener = (View.OnClickListener) XposedHelpers.getObjectField(param.thisObject, "mRecentsClickListener");
            Object navigationBarView = XposedHelpers.getObjectField(param.thisObject, "mNavigationBarView");
            sRecentsButton = (View) XposedHelpers.callMethod(navigationBarView, "getRecentsButton");

            sRecentsButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    XposedHook.logD(TAG, "SW recents clicked");
                    if (!sWasPressed) {
                        sWasPressed = true;
                        mHandler.postDelayed(sResetPressedStateRunnable, mDoubletapSpeed);
                    } else {
                        XposedHook.logD(TAG, "Double tap detected");
                        mHandler.removeCallbacks(sResetPressedStateRunnable);
                        sWasPressed = false;
                        if (!isTaskLocked(mContext))
                            switchToLastApp(mContext, mHandler);
                    }
                }
            });

            registerReceiver(mContext, false);
        }
    };

    public static void hook(ClassLoader classLoader) {
        try {
            ConfigUtils config = ConfigUtils.getInstance();
            config.reload();
            loadPrefDoubleTapSpeed();
            if (config.recents.double_tap) {
                try {
                    XposedHelpers.findAndHookMethod(CLASS_PHONE_STATUS_BAR, classLoader, "prepareNavigationBarView", prepareNavigationBarViewHook);
                } catch (NoSuchMethodError e) {
                    // CM takes a boolean parameter
                    XposedHelpers.findAndHookMethod(CLASS_PHONE_STATUS_BAR, classLoader, "prepareNavigationBarView", boolean.class, prepareNavigationBarViewHook);
                }
            }
        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error in hook", t);
        }
    }

}
