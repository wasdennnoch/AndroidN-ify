package tk.wasdennnoch.androidn_ify.systemui.qs.tiles;

import android.content.Intent;
import android.graphics.drawable.Drawable;

import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.systemui.qs.TilesManager;
import tk.wasdennnoch.androidn_ify.ui.SettingsActivity;

public class AndroidN_ifyTile extends QSTile {

    public static final String TILE_SPEC = "androidn_ify";
    private final Drawable mIcon;
    private final String mLabel;

    public AndroidN_ifyTile(TilesManager tilesManager, Object host, String key) {
        super(tilesManager, host, key);
        mIcon = mResUtils.getDrawable(R.drawable.ic_stat_n);
        mLabel = mResUtils.getString(R.string.app_name);
    }

    @Override
    public void handleUpdateState(Object state, Object arg) {
        mState.icon = mIcon;
        mState.label = mLabel;
        super.handleUpdateState(state, arg);
    }

    @Override
    public void handleClick() {
        handleLongClick();
    }

    @Override
    public void handleLongClick() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setClassName("tk.wasdennnoch.androidn_ify", SettingsActivity.class.getName());
        startActivityDismissingKeyguard(intent);
    }
}
