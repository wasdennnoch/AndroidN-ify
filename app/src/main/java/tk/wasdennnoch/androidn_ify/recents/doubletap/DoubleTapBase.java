package tk.wasdennnoch.androidn_ify.recents.doubletap;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Handler;
import android.widget.Toast;

import java.util.List;

import tk.wasdennnoch.androidn_ify.XposedHook;

public class DoubleTapBase {

    private static final String TAG = "DoubleTapBase";
    protected static int mDoubletapSpeed = 400;
    private static ActivityManager mAm;

    protected static ActivityManager getActivityManager(Context context) {
        if (mAm == null) {
            mAm = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        }
        return mAm;
    }

    protected static void switchToLastApp(final Context context, Handler handler) {
        handler.post(
                new Runnable() {
                    @Override
                    public void run() {
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
                            Toast.makeText(context, "No previous app", Toast.LENGTH_SHORT).show();
                        }
                        XposedHook.logD(TAG, "App switched");
                    }
                }
        );
    }

}
