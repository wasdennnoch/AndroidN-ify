package tk.wasdennnoch.androidn_ify.systemui.notifications;

import android.app.Notification;

import de.robv.android.xposed.XposedHelpers;

public class SensitiveNotificationRow implements SensitiveNotificationFilter.SensitiveFilterListener {

    private String mPackageName;
    private Object mRow;

    private boolean mSensitive;
    private boolean mEnabled;
    private boolean mPrivate;

    public SensitiveNotificationRow(SensitiveNotificationFilter sensitiveFilter, Object row, Object sbn) {
        mPackageName = (String) XposedHelpers.getObjectField(sbn, "pkg");
        mRow = row;
        mEnabled = sensitiveFilter.isEnabled(mPackageName);

        Notification notification = (Notification) XposedHelpers.getObjectField(sbn, "notification");
        mPrivate = notification.visibility == Notification.VISIBILITY_PRIVATE;

        sensitiveFilter.addListener(this, mPackageName);
    }

    @Override
    public void onPackageChanged(String pkg, boolean enabled) {
        if (!pkg.equals(mPackageName)) return;
        mEnabled = enabled;
        updateSensitive();
    }

    public void setSensitive(boolean sensitive) {
        mSensitive = sensitive;
        updateSensitive();
    }

    private void updateSensitive() {
        XposedHelpers.setBooleanField(mRow, "mSensitive", (mEnabled && mPrivate) || mSensitive);
    }
}
