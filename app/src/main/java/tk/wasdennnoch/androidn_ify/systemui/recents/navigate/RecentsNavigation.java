package tk.wasdennnoch.androidn_ify.systemui.recents.navigate;

import android.app.ActivityManager;
import android.content.Context;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.FrameLayout;

import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LayoutInflated;
import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.utils.ConfigUtils;
import tk.wasdennnoch.androidn_ify.utils.ResourceUtils;

public class RecentsNavigation {

    private static final String PACKAGE_SYSTEMUI = XposedHook.PACKAGE_SYSTEMUI;
    //private static final String CLASS_PHONE_STATUS_BAR = "com.android.systemui.statusbar.phone.PhoneStatusBar";

    private static final String TAG = "RecentsNavigation";
    private static long mStartRecentsActivityTime = 0;
    private static Object mRecentsActivity;
    private static ConfigUtils mConfig;
    private static ClassLoader mClassLoader;
    private static boolean mIsNavigating = false;
    private static int mCurrentIndex = 0;
    private static TaskProgress mCurrentProgress = null;
    private static boolean mBackPressed = false;
    private static boolean mSkipFirstApp = false;

    private static XC_MethodHook startRecentsActivityHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            mStartRecentsActivityTime = SystemClock.elapsedRealtime();
            mIsNavigating = false;
            if (mConfig.recents.double_tap) {
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
    private static boolean mResetScroll;

    public static boolean isDoubleTap() {
        return (mConfig.recents.double_tap && ((SystemClock.elapsedRealtime() - mStartRecentsActivityTime) < mConfig.recents.double_tap_speed));
    }

    private static XC_MethodHook onBackPressedHook = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            mIsNavigating = false;
            mBackPressed = true;
        }
    };

    private static XC_MethodHook resetNavigatingStatus = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            mIsNavigating = false;
        }
    };

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
                    (boolean) XposedHelpers.callMethod(mRecentsView, "unfilterFilteredStacks"))
                return true;
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
        boolean navigateRecents = mConfig.recents.navigate_recents;
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
            List<Object> tasks = (List<Object>) XposedHelpers.callMethod(stack, "getTasks");
            int taskViewCount = taskViews.size();
            int taskCount = tasks.size();
            if (!mIsNavigating && !mBackPressed) {
                mCurrentIndex = taskCount;
                mIsNavigating = true;
                mResetScroll = true;
                mSkipFirstApp = !launchedFromHome;
            }
            if (isDoubleTap && taskViewCount > (doubleTapLaunchIndexBackward - 1)) {
                Object tv = taskViews.get(taskViewCount - doubleTapLaunchIndexBackward);
                Object task = XposedHelpers.callMethod(tv, "getTask");
                XposedHelpers.callMethod(mRecentsView, "onTaskViewClicked", stackView, tv, stack, task, false);
                return true;
            } else {
                if (!mBackPressed && navigateRecents) {
                    return navigateRecents(tasks, taskViews, stackView, stack);
                } else {
                    mBackPressed = false;
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
        }
        return false;
    }

    private static boolean navigateRecents(List<Object> tasks, List<Object> taskViews, Object stackView, Object stack) {
        int taskCount = tasks.size();
        int taskViewCount = taskViews.size();
        if (taskCount < 1) return false;
        if (mCurrentIndex == 0) {
            mCurrentIndex = tasks.size() - 1;
            mResetScroll = true;
        } else {
            mCurrentIndex--;
        }
        if (taskCount <= mCurrentIndex) mCurrentIndex = tasks.size() - 1;
        if (mCurrentProgress != null) {
            mCurrentProgress.stop();
            mCurrentProgress = null;
        }
        Object anchorTask = null;
        for (int i = 0; i < taskViewCount; i++) {
            Object tv = taskViews.get(i);
            Object task = XposedHelpers.callMethod(tv, "getTask");
            if (task == tasks.get(mCurrentIndex)) {
                XposedHook.logD(TAG, "task found");
                Object mStackScroller = XposedHelpers.getObjectField(stackView, "mStackScroller");
                if (mResetScroll) {
                    XposedHelpers.callMethod(mStackScroller, "setStackScroll", 10f);
                    mResetScroll = false;
                }
                if (anchorTask == null)
                    anchorTask = XposedHelpers.callMethod(XposedHelpers.getObjectField(stackView, "mStack"), "getFrontMostTask");
                Object mLayoutAlgorithm = XposedHelpers.getObjectField(stackView, "mLayoutAlgorithm");
                float anchorTaskScroll = (float) XposedHelpers.callMethod(mLayoutAlgorithm, "getStackScrollForTask", anchorTask);
                float curScroll = (float) XposedHelpers.callMethod(mStackScroller, "getStackScroll");
                float newScroll = (float) XposedHelpers.callMethod(mLayoutAlgorithm, "getStackScrollForTask", task);
                XposedHelpers.callMethod(mStackScroller, "setStackScroll", curScroll - Math.abs(newScroll - anchorTaskScroll));
                XposedHelpers.callMethod(mStackScroller, "boundScroll");
                XposedHelpers.callMethod(stackView, "requestSynchronizeStackViewsWithModel", 200);

                FrameLayout taskView = (FrameLayout) taskViews.get(i);

                mCurrentProgress = new TaskProgress(taskView, stackView, stack, task);
                mCurrentProgress.start();

                if (mSkipFirstApp) {
                    mSkipFirstApp = false;
                    navigateRecents(tasks, taskViews, stackView, stack);
                }

                return true;
            }
            anchorTask = task;
        }
        return true;
    }

    private static class TaskProgress implements Animation.AnimationListener {
        private FrameLayout mTaskView;
        private FrameLayout mTaskViewHeader;
        private View mProgressView;
        private ScaleAnimation mScaleAnim;
        private Object mStackView;
        private Object mStack;
        private Object mTask;

        public TaskProgress(FrameLayout taskView, Object stackView, Object stack, Object task) {
            mTaskView = taskView;
            mTaskViewHeader = (FrameLayout) XposedHelpers.getObjectField(mTaskView, "mHeaderView");
            mProgressView = mTaskViewHeader.findViewById(R.id.task_progress);
            mStackView = stackView;
            mStack = stack;
            mTask = task;
        }

        public void start() {
            mScaleAnim = new ScaleAnimation(1, 0, 1, 1);
            mScaleAnim.setDuration(mConfig.recents.navigation_delay);
            mScaleAnim.setInterpolator(new LinearInterpolator());
            mScaleAnim.setRepeatCount(0);
            mScaleAnim.setAnimationListener(this);
            mProgressView.setVisibility(View.VISIBLE);
            mProgressView.startAnimation(mScaleAnim);
        }

        public void stop() {
            if (mScaleAnim != null) {
                mScaleAnim.setAnimationListener(null);
            }
            mProgressView.clearAnimation();
            mProgressView.setVisibility(View.GONE);
        }

        @Override
        public void onAnimationStart(Animation animation) {

        }

        @Override
        public void onAnimationEnd(Animation animation) {
            mProgressView.setVisibility(View.GONE);
            mScaleAnim.setAnimationListener(null);
            if (!mIsNavigating) return;
            mIsNavigating = false;
            Object mRecentsView = XposedHelpers.getObjectField(mRecentsActivity, "mRecentsView");
            XposedHelpers.callMethod(mRecentsView, "onTaskViewClicked", mStackView, mTaskView, mStack, mTask, false);
        }

        @Override
        public void onAnimationRepeat(Animation animation) {

        }
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
            mClassLoader = classLoader;
            if (mConfig.recents.double_tap || mConfig.recents.navigate_recents) {
                Class<?> classRecents = XposedHelpers.findClass("com.android.systemui.recents.Recents", classLoader);
                Class<?> classRecentsActivity = XposedHelpers.findClass("com.android.systemui.recents.RecentsActivity", classLoader);

                XposedHelpers.findAndHookMethod(classRecents, "startRecentsActivity", ActivityManager.RunningTaskInfo.class, boolean.class, startRecentsActivityHook);
                XposedHelpers.findAndHookMethod(classRecentsActivity, "onStart", recentsActivityOnStartHook);
                XposedHelpers.findAndHookMethod(classRecentsActivity, "onBackPressed", onBackPressedHook);
                XposedHelpers.findAndHookMethod(classRecentsActivity, "dismissRecentsToHomeRaw", boolean.class, resetNavigatingStatus);
                XposedHelpers.findAndHookMethod(classRecentsActivity, "dismissRecentsToFocusedTaskOrHome", boolean.class, dismissRecentsToFocusedTaskOrHomeHook);
            }

            /*try {
                XposedHelpers.findAndHookMethod(CLASS_PHONE_STATUS_BAR, classLoader, "prepareNavigationBarView", prepareNavigationBarViewHook);
            } catch (NoSuchMethodError e) {
                // CM takes a boolean parameter
                XposedHelpers.findAndHookMethod(CLASS_PHONE_STATUS_BAR, classLoader, "prepareNavigationBarView", boolean.class, prepareNavigationBarViewHook);
            }*/
        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error hooking SystemUI", t);
        }
    }

    /*private static Context mContext;
    private static Handler mHandler;
    private static XC_MethodHook prepareNavigationBarViewHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            XposedHook.logD(TAG, "prepareNavigationBarViewHook called");

            mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
            mHandler = (Handler) XposedHelpers.getObjectField(param.thisObject, "mHandler");

            mContext.registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            XposedHook.logD(TAG, "Kill broadcast received, sending kill signal");
                            Process.sendSignal(Process.myPid(), Process.SIGNAL_KILL);
                        }
                    }, 100);
                }
            }, new IntentFilter(SettingsActivity.ACTION_KILL_SYSTEMUI));
        }
    };*/

    private static XC_LayoutInflated recents_task_view_header = new XC_LayoutInflated() {
        @Override
        public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
            FrameLayout header = (FrameLayout) liparam.view;
            Context context = header.getContext();
            ResourceUtils res = ResourceUtils.getInstance(context);

            int progressHeight = res.getDimensionPixelSize(R.dimen.task_progress_height);

            FrameLayout.LayoutParams progressLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, progressHeight);
            progressLp.gravity = Gravity.BOTTOM;

            View progress = new View(context);
            progress.setId(R.id.task_progress);
            progress.setLayoutParams(progressLp);
            progress.setBackgroundColor(0xA0FFFFFF);
            progress.setVisibility(View.GONE);
            header.addView(progress);
        }
    };

    @SuppressWarnings("unused")
    public static void hookResSystemui(XC_InitPackageResources.InitPackageResourcesParam resparam, String modulePath) {
        try {
            if (ConfigUtils.notifications().change_style) {
                resparam.res.hookLayout(PACKAGE_SYSTEMUI, "layout", "recents_task_view_header", recents_task_view_header);
            }
        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error hooking SystemUI resources", t);
        }
    }

    public static Object getSystemServicesProxy() {
        Class<?> classRecentsTaskLoader = XposedHelpers.findClass("com.android.systemui.recents.model.RecentsTaskLoader", mClassLoader);
        return XposedHelpers.callMethod(XposedHelpers.callStaticMethod(classRecentsTaskLoader, "getInstance"), "getSystemServicesProxy");
    }

}
