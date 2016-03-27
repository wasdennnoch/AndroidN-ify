package tk.wasdennnoch.androidn_ify.recents.doubletap;

import android.os.Handler;
import android.view.View;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import tk.wasdennnoch.androidn_ify.XposedHook;

public class DoubleTapSwKeys extends DoubleTapBase {

    private static final String TAG = "DoubleTapSwKeys";

    private static final String CLASS_NAVIGATION_BAR_VIEW = "com.android.systemui.statusbar.phone.NavigationBarView";
    private static final String CLASS_NAVBAR_EDITOR = "com.android.systemui.statusbar.phone.NavbarEditor";

    private static Object NAVBAR_RECENT;
    private static boolean mWasPressed = false;
    private static XC_MethodHook updateButtonListenersHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(final MethodHookParam param) throws Throwable {

            final Handler mHandler = (Handler) XposedHelpers.getObjectField(param.thisObject, "mHandler");
            final View.OnClickListener mRecentsClickListener = (View.OnClickListener) XposedHelpers.getObjectField(param, "mRecentsClickListener");
            View mCurrentView = (View) XposedHelpers.getObjectField(param.thisObject, "mCurrentView");
            final View recentView = mCurrentView.findViewWithTag(NAVBAR_RECENT);

            final Runnable resetPressed = new Runnable() {
                @Override
                public void run() {
                    XposedHook.logD(TAG, "resetPressed runnable: Invoking original mRecentsClickListener");
                    mWasPressed = false;
                    mRecentsClickListener.onClick(recentView);
                }
            };

            if (recentView != null) {
                recentView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!mWasPressed) {
                            mWasPressed = true;
                            mHandler.postDelayed(resetPressed, mDoubletapSpeed);
                        } else {
                            mHandler.removeCallbacks(resetPressed);
                            switchToLastApp(recentView.getContext(), mHandler);
                        }
                    }
                });
            }

        }
    };

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {

        Class<?> NavigationBarView = XposedHelpers.findClass(CLASS_NAVIGATION_BAR_VIEW, lpparam.classLoader);
        Class<?> NavbarEditor = XposedHelpers.findClass(CLASS_NAVBAR_EDITOR, lpparam.classLoader);

        try {
            XposedHelpers.findAndHookMethod(NavigationBarView, "updateButtonListeners", updateButtonListenersHook);
            NAVBAR_RECENT = XposedHelpers.getStaticObjectField(NavbarEditor, "NAVBAR_RECENT");
        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error hooking updateButtonListeners or getting NAVBAR_RECENT", t);
        }

    }

}
