package tk.wasdennnoch.androidn_ify.systemui.qs.tiles;

import android.annotation.CallSuper;
import android.content.Context;
import android.view.View;

import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.systemui.qs.KeyguardMonitor;
import tk.wasdennnoch.androidn_ify.systemui.qs.QSTileHostHooks;
import tk.wasdennnoch.androidn_ify.systemui.qs.TilesManager;

public abstract class BaseTile implements KeyguardMonitor.Callback {
    private static final String TAG = "BaseTile";
    public static final String TILE_KEY_NAME = "customTileKey";
    public static final String CLASS_TILE_STATE = "com.android.systemui.qs.QSTile.State";

    private TilesManager mTilesManager;
    Object mHost;
    protected Context mContext;
    KeyguardMonitor mKeyguard;
    String mKey;
    Object mTile;
    boolean mSecure = false;

    /**
     * ALWAYS CALL {@link #registerCallbacks()} AS THHE LAST LINE OF THE OVERRIDDEN CONSTRUCTOR
     * If for some reason the tile creation fails the callbacks will already be registered leading to
     * a ghost tile which eats memory and logs a crash when a callback is received. Add the callbacks
     * last so they won't get registered if something crashes.
     */
    BaseTile(TilesManager tilesManager, Object host, String key) {
        mTilesManager = tilesManager;
        mHost = host;
        mContext = (Context) XposedHelpers.callMethod(mHost, "getContext");
        mKeyguard = QSTileHostHooks.mKeyguard;
        mKey = key;
    }

    /**
     * ALWAYS CALL THIS METHOD AS THHE LAST LINE OF THE OVERRIDDEN CONSTRUCTOR
     */
    void registerCallbacks() {
        mKeyguard.addCallback(this);
        mTilesManager.registerTile(this);
    }

    public Object getTile() {
        return mTile;
    }

    public String getKey() {
        return mKey;
    }

    public abstract void handleUpdateState(Object state, Object arg);

    public void onCreateTileView(View tileView) {
        XposedHelpers.setAdditionalInstanceField(tileView, TILE_KEY_NAME, mKey);
    }

    public View onCreateIcon() {
        return null;
    }

    @CallSuper
    public void handleDestroy() {
        setListening(false);
        mTilesManager.unregisterTile(this);
        mTilesManager = null;
        mTile = null;
        mHost = null;
        mContext = null;
        mKeyguard.removeCallback(this);
        mKeyguard = null;
    }

    public Object getDetailAdapter() {
        return null;
    }

    public boolean handleClickInner() {
        handleClick();
        return false;
    }

    public void handleClick() {
    }

    public void handleLongClick() {
    }

    public void setListening(boolean listening) {
    }

    public void setSecure(boolean secure) {
        mSecure = secure;
    }

    @Override
    public void onKeyguardChanged() {
        refreshState();
    }

    void refreshState() {
        try {
            XposedHelpers.callMethod(mTile, "refreshState");
        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error refreshing tile state: ", t);
        }
    }
}
