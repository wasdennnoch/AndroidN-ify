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

    protected TilesManager mTilesManager;
    protected Object mHost;
    protected Context mContext;
    protected KeyguardMonitor mKeyguard;
    protected String mKey;
    protected Object mTile;
    protected boolean mSecure = false;

    /**
     * ALWAYS CALL {@link #registerCallbacks()} AS THHE LAST LINE OF THE OVERRIDDEN CONSTRUCTOR
     */
    public BaseTile(TilesManager tilesManager, Object host, String key) {
        mTilesManager = tilesManager;
        mHost = host;
        mContext = (Context) XposedHelpers.callMethod(mHost, "getContext");
        mKeyguard = QSTileHostHooks.mKeyguard;
        mKey = key;
    }

    /**
     * ALWAYS CALL THIS METHOD AS THHE LAST LINE OF THE OVERRIDDEN CONSTRUCTOR
     */
    protected void registerCallbacks() {
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

    public void refreshState() {
        try {
            XposedHelpers.callMethod(mTile, "refreshState");
        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error refreshing tile state: ", t);
        }
    }
}
