package tk.wasdennnoch.androidn_ify.recents.doubletap;

import android.os.Handler;
import android.view.View;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.XposedHook;

public class DoubleTapSwKeys extends DoubleTapBase {

    private static final String TAG = "DoubleTapSwKeys";

    private static final String CLASS_NAVIGATION_BAR_VIEW = "com.android.systemui.statusbar.phone.NavigationBarView";
    private static final String CLASS_NAVBAR_EDITOR = "com.android.systemui.statusbar.phone.NavbarEditor";

    private static Object NAVBAR_RECENT;
    private static boolean mWasPressed = false;
    private static View.OnClickListener mRecentsClickListener;
    private static View recentView;
    private static Runnable resetPressedState = new Runnable() {
        @Override
        public void run() {
            XposedHook.logD(TAG, "resetPressedState runnable: Invoking original mRecentsClickListener");
            mWasPressed = false;
            mRecentsClickListener.onClick(recentView);
        }
    };
    private static XC_MethodHook updateButtonListenersHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(final MethodHookParam param) throws Throwable {

            final View mCurrentView = (View) XposedHelpers.getObjectField(param.thisObject, "mCurrentView");

            if (!isTaskLocked(mCurrentView.getContext())) {

                final Handler mHandler = (Handler) XposedHelpers.getObjectField(param.thisObject, "mHandler");
                mRecentsClickListener = (View.OnClickListener) XposedHelpers.getObjectField(param.thisObject, "mRecentsClickListener");
                recentView = mCurrentView.findViewWithTag(NAVBAR_RECENT);

                if (recentView != null) {
                    recentView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (!mWasPressed) {
                                mWasPressed = true;
                                mHandler.postDelayed(resetPressedState, mDoubletapSpeed);
                            } else {
                                XposedHook.logD(TAG, "Double tap detected");
                                mHandler.removeCallbacks(resetPressedState);
                                mWasPressed = false;
                                switchToLastApp(mCurrentView.getContext(), mHandler);
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
            if (prefs.getBoolean("enable_recents_tweaks", true)) {

                Class<?> classNavigationBarView = XposedHelpers.findClass(CLASS_NAVIGATION_BAR_VIEW, classLoader);
                Class<?> classNavbarEditor = XposedHelpers.findClass(CLASS_NAVBAR_EDITOR, classLoader);

                NAVBAR_RECENT = XposedHelpers.getStaticObjectField(classNavbarEditor, "NAVBAR_RECENT");
                XposedHelpers.findAndHookMethod(classNavigationBarView, "updateButtonListeners", updateButtonListenersHook);

            }

        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error in hook", t);
        }

    }

}
