package tk.wasdennnoch.androidn_ify.systemui.qs.tiles;

import android.annotation.CallSuper;
import android.content.Intent;
import android.graphics.drawable.Drawable;

import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.systemui.qs.TilesManager;
import tk.wasdennnoch.androidn_ify.utils.ResourceUtils;

public class QSTile extends BaseTile {

    private static final String TAG = "QSTile";
    private static Class<?> resourceIconClass;
    protected final ResourceUtils mResUtils;
    protected final State mState;

    public static final String DUMMY_INTENT = "intent(dummy)";
    public static final String CLASS_INTENT_TILE = "com.android.systemui.qs.tiles.IntentTile";
    public static final String CLASS_VOLUME_TILE = "com.android.systemui.qs.tiles.VolumeTile"; // Used on CM12.1 where IntentTile doesn't exist
    public static final String CLASS_VISUALIZER_TILE = "com.android.systemui.qs.tiles.VisualizerTile"; // To fix a SystemUI crash caused by it
    public static final String CLASS_TILE_VIEW = "com.android.systemui.qs.QSTileView";
    public static final String CLASS_QS_TILE = "com.android.systemui.qs.QSTile";
    public static final String CLASS_RESOURCE_ICON = CLASS_QS_TILE + ".ResourceIcon";

    public QSTile(TilesManager tilesManager, Object host, String key) {
        super(tilesManager, host, key);

        if (!tilesManager.useVolumeTile)
            mTile = XposedHelpers.callStaticMethod(XposedHelpers.findClass(CLASS_INTENT_TILE, mContext.getClassLoader()), "create", mHost, DUMMY_INTENT);
        else
            mTile = XposedHelpers.newInstance(XposedHelpers.findClass(CLASS_VOLUME_TILE, mContext.getClassLoader()), mHost);
        mState = new State(mKey);
        mResUtils = ResourceUtils.getInstance(mContext);
        XposedHelpers.setAdditionalInstanceField(mTile, TILE_KEY_NAME, mKey);
        if (resourceIconClass == null)
            resourceIconClass = getResourceIconClass(mContext.getClassLoader());
        registerCallbacks();
    }

    private static Class<?> getResourceIconClass(ClassLoader classLoader) {
        try {
            return XposedHelpers.findClass(CLASS_QS_TILE + ".ResourceIcon", classLoader);
        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error getting resource icon class:", t);
            return null;
        }
    }

    @CallSuper
    public void handleUpdateState(Object state, Object arg) {
        mState.visible = !mSecure || !mKeyguard.isShowing() || !mKeyguard.isSecure();
        mState.apply(state);
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

    public void showDetail(boolean show) {
        XposedHelpers.callMethod(mTile, "showDetail", show);
    }

    public Drawable getResourceIconDrawable() {
        return mState.icon;
    }

    public static class State {
        private static final String TAG = "QSTile.State";
        public boolean visible;
        public Drawable icon;
        public String label;
        public final boolean autoMirrorDrawable = true;

        private final String mKey;

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
