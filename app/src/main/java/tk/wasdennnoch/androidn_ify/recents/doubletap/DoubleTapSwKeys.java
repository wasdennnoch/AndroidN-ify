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
    private static final String CLASS_NAVIGATION_BAR_VIEW = "com.android.systemui.statusbar.phone.NavigationBarView";

    private static Context mContext;
    private static Handler mHandler;
    private static Object mNavigationBarView;
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
            mNavigationBarView = XposedHelpers.getObjectField(param.thisObject, "mNavigationBarView");
            sRecentsButton = (View) XposedHelpers.callMethod(mNavigationBarView, "getRecentsButton");

            registerReceiver(mContext);
        }
    };
    private static XC_MethodHook setOnClickListenerHook = new XC_MethodHook() {
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
                        if (!isTaskLocked(mContext))
                            switchToLastApp(mContext, mHandler);
                    }
                }
            };
        }
    };
    private static XC_MethodHook reorientHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            if (mNavigationBarView != null) {
                XposedHook.logD(TAG, "Updating sRecentsButton in reorient()");
                sRecentsButton = (View) XposedHelpers.callMethod(mNavigationBarView, "getRecentsButton");
            } else {
                XposedHook.logD(TAG, "Skipped updating sRecentsButton in reorient() because mNavigationBarView is null");
            }
        }
    };

    public static void hook(ClassLoader classLoader, XSharedPreferences prefs) {
        try {
            prefs.reload();
            loadPrefDoubleTapSpeed(prefs);
            if (prefs.getBoolean("enable_recents_tweaks", true)) {
                XposedHelpers.findAndHookMethod(CLASS_PHONE_STATUS_BAR, classLoader, "prepareNavigationBarView", prepareNavigationBarViewHook);
                // I'm hooking every view in the SystemUI here... but hey, it's working.
                XposedHelpers.findAndHookMethod(View.class, "setOnClickListener", View.OnClickListener.class, setOnClickListenerHook);
                XposedHelpers.findAndHookMethod(CLASS_NAVIGATION_BAR_VIEW, classLoader, "reorient", reorientHook);
            }
        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error in hook", t);
        }
    }

}
