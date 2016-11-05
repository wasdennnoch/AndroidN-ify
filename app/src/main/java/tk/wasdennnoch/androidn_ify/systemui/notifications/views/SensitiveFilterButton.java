package tk.wasdennnoch.androidn_ify.systemui.notifications.views;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import java.lang.ref.WeakReference;

import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.systemui.SystemUIHooks;
import tk.wasdennnoch.androidn_ify.systemui.notifications.SensitiveNotificationFilter;
import tk.wasdennnoch.androidn_ify.utils.ResourceUtils;

public class SensitiveFilterButton extends ImageView implements View.OnClickListener {

    private ResourceUtils mRes;
    private SensitiveNotificationFilter mSensitiveFilter;
    private String mPackageName;

    private Drawable mEnabledDrawable;
    private Drawable mDisabledDrawable;

    private boolean mVisible;
    private boolean mEnabled;
    private Runnable mToggleRunnable;
    private Runnable mUpdateState;

    public SensitiveFilterButton(Context context) {
        this(context, null);
    }

    public SensitiveFilterButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        mRes = ResourceUtils.getInstance(context);
        mUpdateState = new UpdateState(this);
        mToggleRunnable = new ToggleRunnable(this);
        setOnClickListener(this);
    }

    public void init(SensitiveNotificationFilter sensitiveFilter, Object sbn) {
        mSensitiveFilter = sensitiveFilter;
        mPackageName = (String) XposedHelpers.getObjectField(sbn, "pkg");
        mEnabled = mSensitiveFilter.isEnabled(mPackageName);
        updateState();
    }

    public String getPackageName() {
        return mPackageName;
    }

    @Override
    public void onClick(View v) {
        SystemUIHooks.startRunnableDismissingKeyguard(mToggleRunnable);
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

    private static class ToggleRunnable implements Runnable {

        private WeakReference<SensitiveFilterButton> mButton;

        protected ToggleRunnable(SensitiveFilterButton button) {
            mButton = new WeakReference<>(button);
        }

        @Override
        public void run() {
            SensitiveFilterButton button = mButton.get();
            if (button != null) {
                button.mEnabled = button.mSensitiveFilter.togglePackage(button.mPackageName);
                button.post(button.mUpdateState);
            }
        }
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
