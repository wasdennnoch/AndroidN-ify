package tk.wasdennnoch.androidn_ify.systemui.qs.tiles;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;

import com.android.egg.neko.Food;
import com.android.egg.neko.NekoLockedActivity;

import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.systemui.qs.TilesManager;

public class NekoTile extends QSTile {

    public static final String TILE_SPEC = "neko";

    private static final String TAG = "NekoTile";
    public static final String ACTION_UPDATE_STATE = "tk.wasdennnoch.androidn_ify.action.ACTION_NEKO_UPDATE_STATE";
    public static final String EXTRA_ICON = "EXTRA_ICON";
    public static final String EXTRA_NAME = "EXTRA_NAME";

    private int mIcon = R.drawable.food_dish;
    private String mName;

    public NekoTile(TilesManager tilesManager, Object host, String key) {
        super(tilesManager, host, key);

        mName = mResUtils.getResources().getStringArray(R.array.food_names)[0];

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(NekoTile.ACTION_UPDATE_STATE);
        mContext.registerReceiver(mBroadcastReceiver, intentFilter);
    }

    @Override
    public void handleDestroy() {
        mContext.unregisterReceiver(mBroadcastReceiver);
        super.handleDestroy();
    }

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            XposedHook.logD(TAG, "Broadcast received, action: " + intent.getAction());
            switch (intent.getAction()) {
                case ACTION_UPDATE_STATE:
                    updateState(intent);
                    break;
            }
        }
    };

    private void updateState(Intent intent) {
        mIcon = intent.getIntExtra(EXTRA_ICON, 0);
        mName = intent.getStringExtra(EXTRA_NAME);
        refreshState();
    }

    @Override
    public void handleUpdateState(Object state, Object arg) {
        Drawable icon = mResUtils.getDrawable(mIcon);
        icon.setTint(0xFFFFFFFF);
        mState.icon = icon;
        mState.label = mName;
        super.handleUpdateState(state, arg);
    }

    @Override
    public void handleClick() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setClassName("tk.wasdennnoch.androidn_ify",
                NekoLockedActivity.class.getName());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivityDismissingKeyguard(intent);
    }

    public static void sendUpdate(Food food, Context context) {
        Intent intent = new Intent(NekoTile.ACTION_UPDATE_STATE);
        intent.putExtra(NekoTile.EXTRA_ICON, food.getIcon(context));
        intent.putExtra(NekoTile.EXTRA_NAME, food.getName(context));
        context.sendBroadcast(intent);
    }
}
