package tk.wasdennnoch.androidn_ify.recents.doubletap;

import android.content.Context;
import android.os.Handler;
import android.view.View;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.XposedHook;

public class DoubleTapSwKeys extends DoubleTapBase {

    private static final String TAG = "DoubleTapSwKeys";

    private static final String CLASS_PHONE_STATUS_BAR = "com.android.systemui.statusbar.phone.PhoneStatusBar";

    private static boolean sWasPressed = false;
    private static View.OnClickListener sOriginalRecentsClickListener;
    private static View sRecentsButton;
    private static Runnable sResetPressedStateRunnable = new Runnable() {
        @Override
        public void run() {
            XposedHook.logD(TAG, "resetPressedState runnable: Invoking original mRecentsClickListener");
            sWasPressed = false;
            sOriginalRecentsClickListener.onClick(sRecentsButton);
        }
    };

    private static XC_MethodHook prepareNavigationBarViewHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            final Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");

            registerReceiver(mContext);

            if (!isTaskLocked(mContext)) {
                final Handler mHandler = (Handler) XposedHelpers.getObjectField(param.thisObject, "mHandler");
                sOriginalRecentsClickListener = (View.OnClickListener) XposedHelpers.getObjectField(param.thisObject, "mRecentsClickListener");
                Object mNavigationBarView = XposedHelpers.getObjectField(param.thisObject, "mNavigationBarView");
                sRecentsButton = (View) XposedHelpers.callMethod(mNavigationBarView, "getRecentsButton");
                if (sRecentsButton != null) {
                    sRecentsButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (!sWasPressed) {
                                sWasPressed = true;
                                mHandler.postDelayed(sResetPressedStateRunnable, mDoubletapSpeed);
                            } else {
                                XposedHook.logD(TAG, "Double tap detected");
                                mHandler.removeCallbacks(sResetPressedStateRunnable);
                                sWasPressed = false;
                                switchToLastApp(mContext, mHandler);
                            }
                        }
                    });
                }
            }
        }
    };

    public static void hook(ClassLoader classLoader, XSharedPreferences prefs) {

        try {

            prefs.reload();
            loadPrefDoubleTapSpeed(prefs);
            if (prefs.getBoolean("enable_recents_tweaks", true)) {

                Class<?> classPhoneStatusBar = XposedHelpers.findClass(CLASS_PHONE_STATUS_BAR, classLoader);
                XposedHelpers.findAndHookMethod(classPhoneStatusBar, "prepareNavigationBarView", prepareNavigationBarViewHook);
            }

        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error in hook", t);
        }

    }

}
