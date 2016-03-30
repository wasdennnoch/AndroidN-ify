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

    private static boolean sOnClickHooked = false;
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
            final Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");

            registerReceiver(mContext);

            if (!isTaskLocked(mContext)) {
                final Handler mHandler = (Handler) XposedHelpers.getObjectField(param.thisObject, "mHandler");
                sOriginalRecentsClickListener = (View.OnClickListener) XposedHelpers.getObjectField(param.thisObject, "mRecentsClickListener");
                final Object mNavigationBarView = XposedHelpers.getObjectField(param.thisObject, "mNavigationBarView");
                sRecentsButton = (View) XposedHelpers.callMethod(mNavigationBarView, "getRecentsButton");

                if (!sOnClickHooked) {
                    // I'm basically hooking every view in the SystemUI here... but hey, it's working.
                    //                             |      KeyButtonView      /   ImageView   /     View     |
                    XposedHelpers.findAndHookMethod(sRecentsButton.getClass().getSuperclass().getSuperclass(), "setOnClickListener", View.OnClickListener.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            final View.OnClickListener original = (View.OnClickListener) param.args[0];
                            param.args[0] = new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    if (v != sRecentsButton) {
                                        XposedHook.logD(TAG, "Other button clicked");
                                        original.onClick(v);
                                        return;
                                    }
                                    XposedHook.logD(TAG, "SW recents clicked");
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
                            };
                        }
                    });
                    XposedHelpers.findAndHookMethod(mNavigationBarView.getClass(), "reorient", new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            XposedHook.logD(TAG, "Updating sRecentsButton in reorient()");
                            sRecentsButton = (View) XposedHelpers.callMethod(mNavigationBarView, "getRecentsButton");
                        }
                    });
                    sOnClickHooked = true;
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
