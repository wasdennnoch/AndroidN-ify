package tk.wasdennnoch.androidn_ify.recents.navigate;

import android.app.ActivityManager;
import android.os.SystemClock;

import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.utils.ConfigUtils;

public class RecentsNavigation {

    private static final String TAG = "RecentsNavigation";
    private static long mStartRecentsActivityTime = 0;
    private static Object mRecentsActivity;
    private static ConfigUtils mConfig;
    private static ClassLoader mClassLoader;

    private static XC_MethodHook startRecentsActivityHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            mStartRecentsActivityTime = SystemClock.elapsedRealtime();
            if(mConfig.recents.double_tap) {
                XposedHelpers.setLongField(param.thisObject, "mLastToggleTime", 0);
            }
        }
    };
    private static XC_MethodHook recentsActivityOnStartHook = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            mRecentsActivity = param.thisObject;
        }
    };

    public static boolean isDoubleTap() {
        return (mConfig.recents.double_tap && ((SystemClock.elapsedRealtime() - mStartRecentsActivityTime) < mConfig.recents.double_tap_speed));
    }

    private static XC_MethodHook dismissRecentsToFocusedTaskOrHomeHook = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            return dismissRecentsToFocusedTaskOrHome((boolean) param.args[0]);
        }
    };

    public static boolean dismissRecentsToFocusedTaskOrHome(boolean checkFilteredStackState) {
        Object ssp = getSystemServicesProxy();
        Object mRecentsView = XposedHelpers.getObjectField(mRecentsActivity, "mRecentsView");
        if (isRecentsTopMost(ssp, getTopMostTask(ssp))) {
            // If we currently have filtered stacks, then unfilter those first
            if (checkFilteredStackState &&
                    (boolean) XposedHelpers.callMethod(mRecentsView, "unfilterFilteredStacks")) return true;
            // If we have a focused Task, launch that Task now
            if (launchFocusedTask()) return true;
            // If we launched from Home, then return to Home
            if (XposedHelpers.getBooleanField(XposedHelpers.getObjectField(mRecentsActivity, "mConfig"), "launchedFromHome")) {
                dismissRecentsToHomeRaw(true);
                return true;
            }
            // Otherwise, try and return to the Task that Recents was launched from
            if ((boolean) XposedHelpers.callMethod(mRecentsView, "launchPreviousTask")) return true;
            // If none of the other cases apply, then just go Home
            dismissRecentsToHomeRaw(true);
            return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public static boolean launchFocusedTask() {
        XposedHook.logD(TAG, "launchFocusedTask");
        boolean isDoubleTap = isDoubleTap();
        boolean launchedFromHome = XposedHelpers.getBooleanField(XposedHelpers.getObjectField(mRecentsActivity, "mConfig"), "launchedFromHome");
        int doubleTapLaunchIndexBackward = (launchedFromHome) ? 1 : 2;
        Object mRecentsView = XposedHelpers.getObjectField(mRecentsActivity, "mRecentsView");
        // Get the first stack view
        List<Object> stackViews = (List<Object>) XposedHelpers.callMethod(mRecentsView, "getTaskStackViews");
        int stackCount = stackViews.size();
        for (int i = 0; i < stackCount; i++) {
            Object stackView = stackViews.get(i);
            Object stack = XposedHelpers.callMethod(stackView, "getStack");
            // Iterate the stack views and try and find the focused task
            List<Object> taskViews = (List<Object>) XposedHelpers.callMethod(stackView, "getTaskViews");
            int taskViewCount = taskViews.size();
            if (isDoubleTap && taskViewCount > (doubleTapLaunchIndexBackward - 1)) {
                Object tv = taskViews.get(taskViewCount - doubleTapLaunchIndexBackward);
                Object task = XposedHelpers.callMethod(tv, "getTask");
                XposedHelpers.callMethod(mRecentsView, "onTaskViewClicked", stackView, tv, stack, task, false);
                return true;
            } else {
                for (int j = 0; j < taskViewCount; j++) {
                    Object tv = taskViews.get(j);
                    Object task = XposedHelpers.callMethod(tv, "getTask");
                    if ((boolean) XposedHelpers.callMethod(tv, "isFocusedTask")) {
                        XposedHelpers.callMethod(mRecentsView, "onTaskViewClicked", stackView, tv, stack, task, false);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean isRecentsTopMost(Object ssp, Object topMostTask) {
        return (boolean) XposedHelpers.callMethod(ssp, "isRecentsTopMost", topMostTask, null);
    }

    public static Object getTopMostTask(Object ssp) {
        return XposedHelpers.callMethod(ssp, "getTopMostTask");
    }

    public static void dismissRecentsToHomeRaw(boolean animated) {
        XposedHelpers.callMethod(mRecentsActivity, "dismissRecentsToHomeRaw", animated);
    }

    public static void hookSystemUI(ClassLoader classLoader) {
        try {
            mConfig = ConfigUtils.getInstance();
            if (mConfig.recents.double_tap || mConfig.recents.navigate_recents) {
                mClassLoader = classLoader;

                Class<?> classRecents = XposedHelpers.findClass("com.android.systemui.recents.Recents", classLoader);
                Class<?> classRecentsActivity = XposedHelpers.findClass("com.android.systemui.recents.RecentsActivity", classLoader);

                XposedHelpers.findAndHookMethod(classRecents, "startRecentsActivity", ActivityManager.RunningTaskInfo.class, boolean.class, startRecentsActivityHook);
                XposedHelpers.findAndHookMethod(classRecentsActivity, "onStart", recentsActivityOnStartHook);
                XposedHelpers.findAndHookMethod(classRecentsActivity, "dismissRecentsToFocusedTaskOrHome", boolean.class, dismissRecentsToFocusedTaskOrHomeHook);

            }
        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error hooking SystemUI", t);
        }
    }

    public static Object getSystemServicesProxy() {
        Class<?> classRecentsTaskLoader = XposedHelpers.findClass("com.android.systemui.recents.model.RecentsTaskLoader", mClassLoader);
        return XposedHelpers.callMethod(XposedHelpers.callStaticMethod(classRecentsTaskLoader, "getInstance"), "getSystemServicesProxy");
    }

}
