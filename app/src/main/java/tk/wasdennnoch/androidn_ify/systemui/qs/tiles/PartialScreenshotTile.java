package tk.wasdennnoch.androidn_ify.systemui.qs.tiles;

import android.graphics.drawable.Drawable;
import android.os.Handler;

import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.systemui.notifications.NotificationPanelHooks;
import tk.wasdennnoch.androidn_ify.systemui.qs.TilesManager;
import tk.wasdennnoch.androidn_ify.systemui.screenshot.ScreenshotHooks;
import tk.wasdennnoch.androidn_ify.utils.ResourceUtils;

public class PartialScreenshotTile extends QSTile {

    public static final String TILE_SPEC = "partial_screenshot";

    private Drawable mIcon;
    private String mLabel;
    private Handler mHandler = new Handler();

    private Runnable mTakePartialScreenshotRunnable = new Runnable() {
        @Override
        public void run() {
            ScreenshotHooks.takePartialScreenshot(mContext);
        }
    };

    public PartialScreenshotTile(TilesManager tilesManager, Object host, String key) {
        super(tilesManager, host, key);

        ResourceUtils res = ResourceUtils.getInstance(mContext);
        mIcon = res.getDrawable(R.drawable.ic_crop);
        mLabel = res.getString(R.string.partial_screenshot);
    }

    @Override
    public void handleUpdateState(Object state, Object arg) {
        mState.icon = mIcon;
        mState.label = mLabel;
        super.handleUpdateState(state, arg);
    }

    @Override
    public void handleClick() {
        NotificationPanelHooks.postInstantCollapse(mTakePartialScreenshotRunnable);
    }
}
