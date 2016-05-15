package tk.wasdennnoch.androidn_ify.notifications.qs;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.utils.ConfigUtils;

public class QSTileHostHooks {
    public static final String TAG = "QSTileHostHooks";

    public static final String CLASS_TILE_HOST = "com.android.systemui.statusbar.phone.QSTileHost";
    public static final String TILES_SETTING = "sysui_qs_tiles";

    private static XC_MethodHook onTuningChangedHook = new XC_MethodHook() {
        @SuppressWarnings("unchecked")
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            // Thanks to GravityBox for this

            if (!TILES_SETTING.equals(param.args[0])) return;

            Map<String, Object> tileMap = (Map<String, Object>) XposedHelpers.getObjectField(param.thisObject, "mTiles");
            for (Entry<String, Object> entry : tileMap.entrySet()) {
                XposedHelpers.callMethod(entry.getValue(), "handleDestroy");
            }
            tileMap.clear();
            ((List<?>)XposedHelpers.getObjectField(param.thisObject, "mTileSpecs")).clear();
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            if (!TILES_SETTING.equals(param.args[0])) return;

            List<String> tileSpecs = (List<String>) XposedHelpers.getObjectField(param.thisObject, "mTileSpecs");
            Map<String, Object> tileMap = (Map<String, Object>) XposedHelpers.getObjectField(param.thisObject, "mTiles");

            tileSpecs.clear();
            tileSpecs.addAll(getTileSpecs());
            tileMap.clear();
            int tileSpecCount = tileSpecs.size();
            for (int i = 0; i < tileSpecCount; i++) {
                String spec = tileSpecs.get(i);
                XposedHook.logD(TAG, "adding tile: " + spec);
                tileMap.put(spec, createTile(param.thisObject, spec));
            }

            Object mCallback = XposedHelpers.getObjectField(param.thisObject, "mCallback");
            if (mCallback != null) XposedHelpers.callMethod(mCallback, "onTilesChanged");
        }
    };

    private static XC_MethodHook loadTileSpecsHook = new XC_MethodHook() {
        @SuppressWarnings("unchecked")
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            List<String> tiles = (List<String>) param.getResult();
            if (tiles.contains("edit")) {
                tiles.remove("edit");
            }
        }
    };

    public static List<String> getTileSpecs() {
        // TODO make this customizable

        List<String> tileSpecs = new ArrayList<>();
        tileSpecs.add("wifi");
        tileSpecs.add("bt");
        tileSpecs.add("cell");
        tileSpecs.add("airplane");
        tileSpecs.add("flashlight");
        tileSpecs.add("rotation");
        //tileSpecs.add("cast");
        tileSpecs.add("location");
        tileSpecs.add("dnd");
        return tileSpecs;
    }

    public static Object createTile(Object tileHost, String tileSpec) {
        return XposedHelpers.callMethod(tileHost, "createTile", tileSpec);
    }

    public static void hook(ClassLoader classLoader) {
        try {
            Class<?> classTileHost = XposedHelpers.findClass(CLASS_TILE_HOST, classLoader);

            //XposedHelpers.findAndHookMethod(classTileHost, "onTuningChanged", String.class, String.class, onTuningChangedHook);
            if (ConfigUtils.header().hide_edit_tiles) {
                XposedHelpers.findAndHookMethod(classTileHost, "loadTileSpecs", String.class, loadTileSpecsHook);
            }
        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error in hook", t);
        }
    }
}