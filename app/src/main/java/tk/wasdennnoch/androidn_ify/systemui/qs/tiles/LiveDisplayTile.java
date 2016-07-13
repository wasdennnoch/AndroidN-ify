package tk.wasdennnoch.androidn_ify.systemui.qs.tiles;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.UserHandle;

import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.systemui.qs.TilesManager;
import tk.wasdennnoch.androidn_ify.systemui.qs.tiles.helper.LiveDisplayObserver;
import tk.wasdennnoch.androidn_ify.utils.ResourceUtils;

public class LiveDisplayTile extends QSTile {

    public static final String TILE_SPEC = "live_display";
    private static final String TAG = "LiveDisplayTile";
    private String[] mTileEntries;
    private int[] mTileEntryIconRes;
    private ResourceUtils mResUtils;
    private int mCurrentMode = 0;

    public LiveDisplayTile(TilesManager tilesManager, Object host, String key) {
        super(tilesManager, host, key);
        mResUtils = ResourceUtils.getInstance(mContext);
        updateCustomTileEntries();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(LiveDisplayObserver.LIVE_DISPLAY_MODE_CHANGED);
        mContext.registerReceiver(mBroadcastReceiver, intentFilter);
    }

    @Override
    public void handleUpdateState(Object state, Object arg) {
        mState.icon = mResUtils.getDrawable(mTileEntryIconRes[mCurrentMode]);
        mState.label = mTileEntries[mCurrentMode];
        super.handleUpdateState(state, arg);
    }

    @Override
    public void handleClick() {
        try {
            getCustomTileNextModePendingIntent().send();
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void handleLongClick() {
        startActivityDismissingKeyguard("cyanogenmod.settings.LIVEDISPLAY_SETTINGS");
    }

    @Override
    public void handleDestroy() {
        mContext.unregisterReceiver(mBroadcastReceiver);
        super.handleDestroy();
    }

    private PendingIntent getCustomTileNextModePendingIntent() {
        Intent i = new Intent("cyanogenmod.hardware.NEXT_LIVEDISPLAY_MODE");
        return (PendingIntent) XposedHelpers.callStaticMethod(PendingIntent.class, "getBroadcastAsUser",
                mContext, 0, i, PendingIntent.FLAG_UPDATE_CURRENT, XposedHelpers.getStaticObjectField(UserHandle.class, "CURRENT"));
    }

    private void updateCustomTileEntries() {
        Resources res = mResUtils.getResources();
        mTileEntries = res.getStringArray(R.array.live_display_entries);
        mTileEntryIconRes = new int[] {
            R.drawable.ic_livedisplay_auto,
            R.drawable.ic_livedisplay_off,
            R.drawable.ic_livedisplay_day,
            R.drawable.ic_livedisplay_night,
            R.drawable.ic_livedisplay_outdoor
        };
    }

    public void onModeChanged(int mode) {
        mCurrentMode = mode;
        refreshState();
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            XposedHook.logD(TAG, "Broadcast received, action: " + intent.getAction());
            switch (intent.getAction()) {
                case LiveDisplayObserver.LIVE_DISPLAY_MODE_CHANGED:
                    if (intent.hasExtra(LiveDisplayObserver.EXTRA_LIVE_DISPLAY_MODE))
                        onModeChanged(intent.getIntExtra(LiveDisplayObserver.EXTRA_LIVE_DISPLAY_MODE, 0));
                    break;
            }
        }
    };
}
