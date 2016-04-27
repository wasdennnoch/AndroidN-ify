package tk.wasdennnoch.androidn_ify.recents.doubletap;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Handler;
import android.widget.Toast;

import java.util.List;

import de.robv.android.xposed.XSharedPreferences;
import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.ui.SettingsActivity;
import tk.wasdennnoch.androidn_ify.utils.ResourceUtils;

public class DoubleTapBase {

    private static final String TAG = "DoubleTapBase";
    private static ActivityManager mAm;
    protected static int mDoubletapSpeed = 180;

    private static BroadcastReceiver sBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            XposedHook.logD(TAG, "Broadcast received, action: " + intent.getAction());
            switch (intent.getAction()) {
                case SettingsActivity.ACTION_RECENTS_CHANGED:
                    if (intent.hasExtra(SettingsActivity.EXTRA_RECENTS_DOUBLE_TAP_SPEED))
                        mDoubletapSpeed = intent.getIntExtra(SettingsActivity.EXTRA_RECENTS_DOUBLE_TAP_SPEED, 180);
                    break;
                case SettingsActivity.ACTION_GENERAL:
                    if (intent.hasExtra(SettingsActivity.EXTRA_GENERAL_DEBUG_LOG))
                        XposedHook.debug = intent.getBooleanExtra(SettingsActivity.EXTRA_GENERAL_DEBUG_LOG, false);
                    break;
            }
        }
    };

    protected static void loadPrefDoubleTapSpeed(XSharedPreferences prefs) {
        mDoubletapSpeed = prefs.getInt("double_tap_speed", 180);
    }

    protected static void registerReceiver(final Context context) {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(SettingsActivity.ACTION_RECENTS_CHANGED);
        intentFilter.addAction(SettingsActivity.ACTION_GENERAL);
        context.registerReceiver(sBroadcastReceiver, intentFilter);
    }

    private static ActivityManager getActivityManager(Context context) {
        if (mAm == null) {
            mAm = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        }
        return mAm;
    }

    static boolean isTaskLocked(Context context) {
        return Build.VERSION.SDK_INT >= 23 && getActivityManager(context).getLockTaskModeState() != ActivityManager.LOCK_TASK_MODE_NONE;
    }

    // Thanks to GravityBox for this code snippet!
    static void switchToLastApp(final Context context, Handler handler) {
        handler.post(
                new Runnable() {
                    @Override
                    public void run() {
                        try { // try-catch to avoid crashing the SystemUI or even the system process
                            int lastAppId = 0;
                            int looper = 1;
                            String packageName;
                            final Intent intent = new Intent(Intent.ACTION_MAIN);
                            final ActivityManager am = getActivityManager(context);
                            String defaultHomePackage = "com.android.launcher";
                            intent.addCategory(Intent.CATEGORY_HOME);
                            final ResolveInfo res = context.getPackageManager().resolveActivity(intent, 0);
                            if (res.activityInfo != null && !res.activityInfo.packageName.equals("android")) {
                                defaultHomePackage = res.activityInfo.packageName;
                            }
                            //noinspection deprecation - Still available to system apps
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
                                Toast.makeText(context, ResourceUtils.getInstance(context).getString(R.string.no_previous_recents), Toast.LENGTH_SHORT).show();
                            }
                            XposedHook.logD(TAG, "App got switched: " + (lastAppId != 0) + "; lastAppId: " + lastAppId);
                        } catch (Throwable t) {
                            XposedHook.logE(TAG, "Error in switchToLastApp", t);
                        }
                    }
                }
        );
    }

}
