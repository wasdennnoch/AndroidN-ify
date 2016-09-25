package tk.wasdennnoch.androidn_ify.systemui.notifications.views;

import android.app.Notification;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import java.lang.ref.WeakReference;

import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.systemui.notifications.SensitiveNotificationFilter;
import tk.wasdennnoch.androidn_ify.utils.ResourceUtils;

public class SensitiveFilterButton extends ImageView implements View.OnClickListener, SensitiveNotificationFilter.SensitiveFilterListener {

    private ResourceUtils mRes;
    private SensitiveNotificationFilter mSensitiveFilter;
    private String mPackageName;

    private Drawable mEnabledDrawable;
    private Drawable mDisabledDrawable;

    private boolean mVisible;
    private boolean mSensitive;
    private boolean mEnabled;
    private Object mRow;
    private Runnable mUpdateState;

    public SensitiveFilterButton(Context context) {
        this(context, null);
    }

    public SensitiveFilterButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        mRes = ResourceUtils.getInstance(context);
        mUpdateState = new UpdateState(this);
        setOnClickListener(this);
    }

    public void init(SensitiveNotificationFilter sensitiveFilter, Object sbn, Object row) {
        mSensitiveFilter = sensitiveFilter;
        mPackageName = (String) XposedHelpers.getObjectField(sbn, "pkg");
        mRow = row;

        Notification notification = (Notification) XposedHelpers.getObjectField(sbn, "notification");
        mSensitive = notification.visibility == Notification.VISIBILITY_PRIVATE;

        mSensitiveFilter.addListener(this, mPackageName);
    }

    @Override
    public void onPackageChanged(String pkg, boolean enabled) {
        if (!pkg.equals(mPackageName)) return;
        mEnabled = enabled;
        updateSensitive();
        post(mUpdateState);
    }

    public String getPackageName() {
        return mPackageName;
    }

    @Override
    public void onClick(View v) {
        mSensitiveFilter.togglePackage(mPackageName);
    }

    private void updateState() {
        if (!mVisible) {
            setVisibility(VISIBLE);
            mVisible = true;
        }
        setImageDrawable(mEnabled ? getEnabledDrawable() : getDisabledDrawable());
    }

    private Drawable getEnabledDrawable() {
        if (mEnabledDrawable == null)
            mEnabledDrawable = mRes.getDrawable(R.drawable.ic_lock);
        return mEnabledDrawable;
    }

    private Drawable getDisabledDrawable() {
        if (mDisabledDrawable == null)
            mDisabledDrawable = mRes.getDrawable(R.drawable.ic_lock_open);
        return mDisabledDrawable;
    }

    private void updateSensitive() {
        XposedHelpers.setBooleanField(mRow, "mSensitive", mSensitive && mEnabled);
    }

    private static class UpdateState implements Runnable {

        private WeakReference<SensitiveFilterButton> mButton;

        protected UpdateState(SensitiveFilterButton button) {
            mButton = new WeakReference<>(button);
        }

        @Override
        public void run() {
            SensitiveFilterButton button = mButton.get();
            if (button != null) button.updateState();
        }
    }
}
