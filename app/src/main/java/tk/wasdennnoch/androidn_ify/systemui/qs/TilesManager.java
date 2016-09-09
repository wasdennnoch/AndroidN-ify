package tk.wasdennnoch.androidn_ify.systemui.qs;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.view.View;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.systemui.BatteryMeterDrawable;
import tk.wasdennnoch.androidn_ify.systemui.qs.tiles.AndroidN_ifyTile;
import tk.wasdennnoch.androidn_ify.systemui.qs.tiles.BatteryTile;
import tk.wasdennnoch.androidn_ify.systemui.qs.tiles.LiveDisplayTile;
import tk.wasdennnoch.androidn_ify.systemui.qs.tiles.QSTile;
import tk.wasdennnoch.androidn_ify.utils.ConfigUtils;
import tk.wasdennnoch.androidn_ify.utils.ResourceUtils;
import tk.wasdennnoch.androidn_ify.utils.RomUtils;

@SuppressWarnings("SameReturnValue")
public class TilesManager {

    private static final String TAG = "TilesManager";

    private final Object mQSTileHost;
    private final Context mContext;

    public static final List<String> mCustomTileSpecs = new ArrayList<>();
    private final Map<String, QSTile> mTiles = new HashMap<>();
    private String mCreateTileViewTileKey;
    public boolean useVolumeTile = false;

    static {
        mCustomTileSpecs.add(AndroidN_ifyTile.TILE_SPEC);
        mCustomTileSpecs.add(BatteryTile.TILE_SPEC);
        if (RomUtils.isCm() && ConfigUtils.M)
            mCustomTileSpecs.add(LiveDisplayTile.TILE_SPEC);
    }

    public static int getLabelResource(String spec) throws Exception {
        if (!mCustomTileSpecs.contains(spec))
            throw new Exception("No label for spec '" + spec + "'!");
        switch (spec) {
            case AndroidN_ifyTile.TILE_SPEC:
                return R.string.app_name;
            case BatteryTile.TILE_SPEC:
                return R.string.battery;
            case LiveDisplayTile.TILE_SPEC:
                return R.string.live_display;
        }
        return 0;
    }

    public static Drawable getIcon(Context context, String spec) throws Exception {
        switch (spec) {
            case AndroidN_ifyTile.TILE_SPEC:
                return ResourceUtils.getInstance(context).getDrawable(R.drawable.ic_stat_n);
            case BatteryTile.TILE_SPEC:
                BatteryMeterDrawable batteryMeterDrawable = new BatteryMeterDrawable(context, new Handler(), context.getResources().getColor( //TODO getColor is deprecated, find a way to use ContextCompat.getColor instead
                        context.getResources().getIdentifier("batterymeter_frame_color", "color", XposedHook.PACKAGE_SYSTEMUI)));
                batteryMeterDrawable.setLevel(100);
                batteryMeterDrawable.onPowerSaveChanged(false);
                batteryMeterDrawable.setShowPercent(false);
                return batteryMeterDrawable;
            default:
                throw new Exception("No icon for spec '" + spec + "'!");
        }
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
            case AndroidN_ifyTile.TILE_SPEC:
                return new AndroidN_ifyTile(this, mQSTileHost, key);
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
                                tile.onCreateTileView((View) param.getResult());
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

            XposedHelpers.findAndHookMethod(QSTile.CLASS_QS_TILE, classLoader, "getDetailAdapter",
                    new XC_MethodHook() {
                        @SuppressWarnings("SuspiciousMethodCalls")
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            final QSTile tile = mTiles.get(XposedHelpers.getAdditionalInstanceField(param.thisObject, QSTile.TILE_KEY_NAME));
                            if (tile != null)
                                param.setResult(tile.getDetailAdapter());
                        }
                    });

            Method clickMethod;
            try {
                clickMethod = XposedHelpers.findMethodExact(hookClass, "handleClick");
            } catch (Throwable t) { // PA
                clickMethod = XposedHelpers.findMethodExact(hookClass, "handleToggleClick");
                XposedHelpers.findAndHookMethod(hookClass, "handleClick", boolean.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        param.args[0] = true; // So that handleToggleClick gets called
                    }
                });
            }
            XposedBridge.hookMethod(clickMethod, new XC_MethodHook() {
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

            Method longClickMethod;
            try {
                longClickMethod = XposedHelpers.findMethodExact(hookClass, "handleLongClick");
            } catch (Throwable t) { // PA
                longClickMethod = XposedHelpers.findMethodExact(hookClass, "handleDetailClick");
            }
            XposedBridge.hookMethod(longClickMethod, new XC_MethodHook() {
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

            try { // Fix a SystemUI crash caused by this tile
                XposedBridge.hookAllMethods(XposedHelpers.findClass(QSTile.CLASS_VISUALIZER_TILE, classLoader), "handleUpdateState", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (XposedHelpers.getObjectField(param.thisObject, "mVisualizer") == null)
                            param.setResult(null);
                    }
                });
            } catch (Throwable ignore) {
            }

        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error in hook", t);
        }
    }
}
