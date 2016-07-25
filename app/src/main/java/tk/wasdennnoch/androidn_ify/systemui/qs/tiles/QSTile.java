package tk.wasdennnoch.androidn_ify.systemui.qs.tiles;

import android.annotation.CallSuper;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.view.View;

import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.systemui.qs.TilesManager;
import tk.wasdennnoch.androidn_ify.utils.ResourceUtils;

public class QSTile {

    private static final String TAG = "QSTile";
    private static Class<?> resourceIconClass;
    private TilesManager mTilesManager;
    private Object mHost;
    private Object mTile;
    private String mKey;
    protected Context mContext;
    protected ResourceUtils mResUtils;
    protected State mState;

    public static final String TILE_KEY_NAME = "customTileKey";
    public static final String DUMMY_INTENT = "intent(dummy)";
    public static final String CLASS_INTENT_TILE = "com.android.systemui.qs.tiles.IntentTile";
    public static final String CLASS_VOLUME_TILE = "com.android.systemui.qs.tiles.VolumeTile"; // Used on CM12.1 where IntentTile doesn't exist
    public static final String CLASS_TILE_STATE = "com.android.systemui.qs.QSTile.State";
    public static final String CLASS_TILE_VIEW = "com.android.systemui.qs.QSTileView";
    public static final String CLASS_QS_TILE = "com.android.systemui.qs.QSTile";
    public static final String CLASS_RESOURCE_ICON = CLASS_QS_TILE + ".ResourceIcon";

    public QSTile(TilesManager tilesManager, Object host, String key) {
        mTilesManager = tilesManager;
        mHost = host;
        mKey = key;
        mState = new State(mKey);
        mContext = (Context) XposedHelpers.callMethod(mHost, "getContext");
        mResUtils = ResourceUtils.getInstance(mContext);
        if (!tilesManager.useVolumeTile)
            mTile = XposedHelpers.callStaticMethod(XposedHelpers.findClass(CLASS_INTENT_TILE, mContext.getClassLoader()), "create", mHost, DUMMY_INTENT);
        else
            mTile = XposedHelpers.newInstance(XposedHelpers.findClass(CLASS_VOLUME_TILE, mContext.getClassLoader()), mHost);
        XposedHelpers.setAdditionalInstanceField(mTile, TILE_KEY_NAME, mKey);
        mTilesManager.registerTile(this);
        if (resourceIconClass == null)
            resourceIconClass = getResourceIconClass(mContext.getClassLoader());
    }

    private static Class<?> getResourceIconClass(ClassLoader classLoader) {
        try {
            return XposedHelpers.findClass(CLASS_QS_TILE + ".ResourceIcon", classLoader);
        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error getting resource icon class:", t);
            return null;
        }
    }

    public Object getTile() {
        return mTile;
    }

    public String getKey() {
        return mKey;
    }

    @CallSuper
    public void handleUpdateState(Object state, Object arg) {
        mState.visible = true;
        mState.apply(state);
    }

    public void onCreateTileView(View tileView) throws Throwable {
        XposedHelpers.setAdditionalInstanceField(tileView, TILE_KEY_NAME, mKey);
    }

    public View onCreateIcon() {
        return null;
    }

    public void refreshState() {
        try {
            XposedHelpers.callMethod(mTile, "refreshState");
        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error refreshing tile state: ", t);
        }
    }

    public void startActivityDismissingKeyguard(String action) {
        startActivityDismissingKeyguard(new Intent(action));
    }

    public void startActivityDismissingKeyguard(Intent intent) {
        try {
            XposedHelpers.callMethod(mHost, "startActivityDismissingKeyguard", intent);
        } catch (Throwable t) {
            try {
                XposedHelpers.callMethod(mHost, "startSettingsActivity", intent);
            } catch (Throwable t2) {
                XposedHook.logE(TAG, "Error starting settings activity", null);
            }
        }
    }

    public void handleClick() {
    }

    public void handleLongClick() {
    }

    public void setListening(boolean listening) {
    }

    @CallSuper
    public void handleDestroy() {
        mTilesManager.unregisterTile(this);
        mTilesManager = null;
        mTile = null;
        mHost = null;
        mContext = null;
    }

    public Drawable getResourceIconDrawable() {
        return mState.icon;
    }

    public static class State {
        private static final String TAG = "QSTile.State";
        public boolean visible;
        public Drawable icon;
        public String label;
        public boolean autoMirrorDrawable = true;

        private String mKey;

        public State(String key) {
            mKey = key;
        }

        public void apply(Object state) {
            XposedHelpers.setBooleanField(state, "visible", visible);
            XposedHelpers.setObjectField(state, "icon", getResourceIcon());
            XposedHelpers.setObjectField(state, "label", label);
            XposedHelpers.setBooleanField(state, "autoMirrorDrawable", autoMirrorDrawable);
        }

        private Object getResourceIcon() {
            if (resourceIconClass == null || icon == null)
                return null;

            try {
                Object resourceIcon = XposedHelpers.callStaticMethod(resourceIconClass, "get", icon.hashCode());
                XposedHelpers.setAdditionalInstanceField(resourceIcon, TILE_KEY_NAME, mKey);
                return resourceIcon;
            } catch (Throwable t) {
                XposedHook.logE(TAG, "Error creating resource icon", t);
                return null;
            }
        }
    }

}
