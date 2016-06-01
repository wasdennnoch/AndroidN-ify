package tk.wasdennnoch.androidn_ify.notifications.qs;

import android.content.Context;
import android.view.View;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.XposedHook;

public class TilesManager {

    private static final String TAG = "TilesManager";

    private Object mQSTileHost;
    private Context mContext;

    public static List<String> mCustomTileSpecs;
    private Map<String, QSTile> mTiles;
    private String mCreateTileViewTileKey;

    static {
        mCustomTileSpecs = new ArrayList<>();
        mCustomTileSpecs.add("test");
        mCustomTileSpecs.add("battery");
    }

    public TilesManager(Object qsTileHost) {
        mQSTileHost = qsTileHost;
        mContext = (Context) XposedHelpers.callMethod(mQSTileHost, "getContext");
        mTiles = new HashMap<>();
        hook();
    }

    public List<String> getCustomTileSpecs() {
        return mCustomTileSpecs;
    }

    public QSTile createTile(String key) {
        switch (key) {
            case "battery":
                return new BatteryTile(this, mQSTileHost, key);
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

            XposedHelpers.findAndHookMethod(QSTile.CLASS_INTENT_TILE, classLoader, "handleUpdateState",
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
                            mCreateTileViewTileKey = (String) XposedHelpers
                                    .getAdditionalInstanceField(param.thisObject, QSTile.TILE_KEY_NAME);
                        }
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
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

            XposedHelpers.findAndHookMethod(QSTile.CLASS_INTENT_TILE, classLoader, "handleClick",
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
        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error in hook", t);
        }
    }
}
