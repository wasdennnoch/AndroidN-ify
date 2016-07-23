package tk.wasdennnoch.androidn_ify.systemui.qs;

import android.content.Context;
import android.view.View;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.systemui.qs.tiles.BatteryTile;
import tk.wasdennnoch.androidn_ify.systemui.qs.tiles.LiveDisplayTile;
import tk.wasdennnoch.androidn_ify.systemui.qs.tiles.QSTile;
import tk.wasdennnoch.androidn_ify.utils.ConfigUtils;
import tk.wasdennnoch.androidn_ify.utils.RomUtils;

public class TilesManager {

    private static final String TAG = "TilesManager";

    private Object mQSTileHost;
    private Context mContext;

    public static List<String> mCustomTileSpecs = new ArrayList<>();
    private Map<String, QSTile> mTiles = new HashMap<>();
    private String mCreateTileViewTileKey;
    public boolean useVolumeTile = false;

    static {
        mCustomTileSpecs.add(BatteryTile.TILE_SPEC);
        if (RomUtils.isCm() && ConfigUtils.M)
            mCustomTileSpecs.add(LiveDisplayTile.TILE_SPEC);
    }

    public static int getLabelResource(String spec) throws Exception {
        if (!mCustomTileSpecs.contains(spec)) throw new Exception("Saved custom tile specs don't contain the spec '" + spec + "'!");
        switch (spec) {
            case BatteryTile.TILE_SPEC:
                return R.string.battery;
            case LiveDisplayTile.TILE_SPEC:
                return R.string.live_display;
        }
        return 0;
    }

    public TilesManager(Object qsTileHost) {
        mQSTileHost = qsTileHost;
        mContext = (Context) XposedHelpers.callMethod(mQSTileHost, "getContext");
        hook();
    }

    public List<String> getCustomTileSpecs() {
        return mCustomTileSpecs;
    }

    public QSTile createTile(String key) {
        switch (key) {
            case BatteryTile.TILE_SPEC:
                return new BatteryTile(this, mQSTileHost, key);
            case LiveDisplayTile.TILE_SPEC:
                return new LiveDisplayTile(this, mQSTileHost, key);
        }
        return new QSTile(this, mQSTileHost, key);
    }

    public synchronized void registerTile(QSTile tile) {
        if (tile == null)
            return;

        String key = tile.getKey();
        if (!mTiles.containsKey(key))
            mTiles.put(key, tile);
    }

    public synchronized void unregisterTile(QSTile tile) {
        if (tile == null)
            return;

        String key = tile.getKey();
        if (mTiles.containsKey(key))
            mTiles.remove(key);
    }

    private void hook() {
        try {
            ClassLoader classLoader = mContext.getClassLoader();

            Class hookClass;
            try {
                hookClass = XposedHelpers.findClass(QSTile.CLASS_INTENT_TILE, classLoader);
            } catch (Throwable ignore) {
                try {
                    hookClass = XposedHelpers.findClass(QSTile.CLASS_VOLUME_TILE, classLoader);
                    XposedHook.logI(TAG, "Using volume tile for custom tiles");
                    useVolumeTile = true;
                } catch (Throwable t) {
                    XposedHook.logE(TAG, "Couldn't find required tile class, aborting hook", null);
                    return;
                }
            }

            XposedHelpers.findAndHookMethod(hookClass, "handleUpdateState",
                    QSTile.CLASS_TILE_STATE, Object.class, new XC_MethodHook() {
                        @SuppressWarnings("SuspiciousMethodCalls")
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            final QSTile tile = mTiles.get(XposedHelpers.getAdditionalInstanceField(param.thisObject, QSTile.TILE_KEY_NAME));
                            if (tile != null) {
                                tile.handleUpdateState(param.args[0], param.args[1]);
                                param.setResult(null);
                            }
                        }
                    });

            XposedHelpers.findAndHookMethod(QSTile.CLASS_QS_TILE, classLoader, "createTileView",
                    Context.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            mCreateTileViewTileKey = (String) XposedHelpers.getAdditionalInstanceField(param.thisObject, QSTile.TILE_KEY_NAME);
                        }
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            if (mCreateTileViewTileKey == null) return;
                            final QSTile tile = mTiles.get(mCreateTileViewTileKey);
                            if (tile != null)
                                tile.onCreateTileView((View)param.getResult());
                            mCreateTileViewTileKey = null;
                        }
                    });

            XposedHelpers.findAndHookMethod(QSTile.CLASS_TILE_VIEW, classLoader, "createIcon",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            if (mCreateTileViewTileKey == null) return;
                            final QSTile tile = mTiles.get(mCreateTileViewTileKey);
                            if (tile != null) {
                                View icon = tile.onCreateIcon();
                                if (icon != null)
                                    param.setResult(icon);
                            }
                        }
                    });

            XposedHelpers.findAndHookMethod(QSTile.CLASS_QS_TILE, classLoader, "handleDestroy",
                    new XC_MethodHook() {
                        @SuppressWarnings("SuspiciousMethodCalls")
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            final QSTile tile = mTiles.get(XposedHelpers.getAdditionalInstanceField(param.thisObject, QSTile.TILE_KEY_NAME));
                            if (tile != null)
                                tile.handleDestroy();
                        }
                    });

            XposedHelpers.findAndHookMethod(hookClass, "handleClick",
                    new XC_MethodHook() {
                        @SuppressWarnings("SuspiciousMethodCalls")
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            final QSTile tile = mTiles.get(XposedHelpers.getAdditionalInstanceField(param.thisObject, QSTile.TILE_KEY_NAME));
                            if (tile != null) {
                                tile.handleClick();
                                param.setResult(null);
                            }
                        }
                    });

            XposedHelpers.findAndHookMethod(hookClass, "handleLongClick",
                    new XC_MethodHook() {
                        @SuppressWarnings("SuspiciousMethodCalls")
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            final QSTile tile = mTiles.get(XposedHelpers.getAdditionalInstanceField(param.thisObject, QSTile.TILE_KEY_NAME));
                            if (tile != null) {
                                tile.handleLongClick();
                                param.setResult(null);
                            }
                        }
                    });

            XposedHelpers.findAndHookMethod(hookClass, "setListening",
                    boolean.class, new XC_MethodHook() {
                        @SuppressWarnings("SuspiciousMethodCalls")
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            final QSTile tile = mTiles.get(XposedHelpers.getAdditionalInstanceField(param.thisObject, QSTile.TILE_KEY_NAME));
                            if (tile != null) {
                                tile.setListening((boolean) param.args[0]);
                                param.setResult(null);
                            }
                        }
                    });

            XposedHelpers.findAndHookMethod(QSTile.CLASS_RESOURCE_ICON, classLoader, "getDrawable",
                    Context.class, new XC_MethodHook() {
                        @SuppressWarnings("SuspiciousMethodCalls")
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            final QSTile tile = mTiles.get(XposedHelpers.getAdditionalInstanceField(param.thisObject, QSTile.TILE_KEY_NAME));
                            if (tile != null) {
                                param.setResult(tile.getResourceIconDrawable());
                            }
                        }
                    });

        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error in hook", t);
        }
    }
}
