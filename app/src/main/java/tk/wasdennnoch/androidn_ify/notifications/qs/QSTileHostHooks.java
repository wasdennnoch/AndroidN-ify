package tk.wasdennnoch.androidn_ify.notifications.qs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.utils.ConfigUtils;
import tk.wasdennnoch.androidn_ify.utils.RomUtils;

public class QSTileHostHooks {
    public static final String TAG = "QSTileHostHooks";

    public static final String CLASS_TILE_HOST = "com.android.systemui.statusbar.phone.QSTileHost";
    public static final String CLASS_QS_UTILS = "org.cyanogenmod.internal.util.QSUtils";
    public static final String CLASS_QS_CONSTANTS = "org.cyanogenmod.internal.util.QSConstants";
    public static final String TILES_SETTING = "sysui_qs_tiles";
    public static final String TILE_SPEC_NAME = "tileSpec";

    private static TilesManager mTilesManager = null;
    public static List<String> mTileSpecs = null;

    private static Class<?> classQSUtils;
    private static Class<?> classQSConstants;

    protected static Object mTileHost = null;

    private static XC_MethodHook onTuningChangedHook = new XC_MethodHook() {
        @SuppressWarnings("unchecked")
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            // Thanks to GravityBox for this
            if (mTileHost == null)
                mTileHost = param.thisObject;

            if (!TILES_SETTING.equals(param.args[0])) return;

            if (mTilesManager != null) {
                Map<String, Object> tileMap = (Map<String, Object>)
                        XposedHelpers.getObjectField(param.thisObject, "mTiles");
                for (Entry<String, Object> entry : tileMap.entrySet()) {
                    XposedHelpers.callMethod(entry.getValue(), "handleDestroy");
                }
                tileMap.clear();
                ((List<?>) XposedHelpers.getObjectField(param.thisObject, "mTileSpecs")).clear();
            }

            Map<String, Object> tileMap = (Map<String, Object>) XposedHelpers.getObjectField(param.thisObject, "mTiles");
            for (Entry<String, Object> entry : tileMap.entrySet()) {
                XposedHelpers.callMethod(entry.getValue(), "handleDestroy");
            }
            tileMap.clear();
            ((List<?>) XposedHelpers.getObjectField(param.thisObject, "mTileSpecs")).clear();
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            if (!TILES_SETTING.equals(param.args[0])) return;

            if (mTilesManager == null)
                mTilesManager = new TilesManager(param.thisObject);

            List<String> tileSpecs = (List<String>) XposedHelpers.getObjectField(param.thisObject, "mTileSpecs");
            Map<String, Object> tileMap = (Map<String, Object>) XposedHelpers.getObjectField(param.thisObject, "mTiles");

            Context context = (Context) XposedHelpers.callMethod(param.thisObject, "getContext");

            tileSpecs.clear();
            tileSpecs.addAll(getTileSpecs(context));
            tileMap.clear();
            int tileSpecCount = tileSpecs.size();
            for (int i = 0; i < tileSpecCount; i++) {
                String spec = tileSpecs.get(i);
                tileMap.put(spec, createTile(param.thisObject, spec));
            }

            Object mCallback = XposedHelpers.getObjectField(param.thisObject, "mCallback");
            if (mCallback != null) XposedHelpers.callMethod(mCallback, "onTilesChanged");
        }
    };

    // For LP
    private static XC_MethodHook recreateTilesHook = new XC_MethodHook() {
        @SuppressWarnings("unchecked")
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            // Thanks to GravityBox for this
            if (mTileHost == null)
                mTileHost = param.thisObject;

            mTileSpecs = new ArrayList<>(); // Do this since mTileSpecs doesn't exist on LP

            if (mTilesManager != null) {
                Map<String, Object> tileMap = (Map<String, Object>)
                        XposedHelpers.getObjectField(param.thisObject, "mTiles");
                for (Entry<String, Object> entry : tileMap.entrySet()) {
                    XposedHelpers.callMethod(entry.getValue(), "handleDestroy");
                }
                tileMap.clear();
                mTileSpecs.clear();
            }

            Map<String, Object> tileMap = (Map<String, Object>) XposedHelpers.getObjectField(param.thisObject, "mTiles");
            for (Entry<String, Object> entry : tileMap.entrySet()) {
                XposedHelpers.callMethod(entry.getValue(), "handleDestroy");
            }
            tileMap.clear();
            mTileSpecs.clear();
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {

            if (mTilesManager == null)
                mTilesManager = new TilesManager(param.thisObject);

            List<String> tileSpecs = new ArrayList<>(); // Do this since mTileSpecs doesn't exist on LP
            Map<String, Object> tileMap = (Map<String, Object>) XposedHelpers.getObjectField(param.thisObject, "mTiles");

            Context context = (Context) XposedHelpers.callMethod(param.thisObject, "getContext");

            tileSpecs.clear();
            tileSpecs.addAll(getTileSpecs(context));
            tileMap.clear();
            int tileSpecCount = tileSpecs.size();
            for (int i = 0; i < tileSpecCount; i++) {
                String spec = tileSpecs.get(i);
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

    public static List<String> getTileSpecs(Context context) {
        // TODO make this customizable

        /*
        List<String> tileSpecs = new ArrayList<>();
        tileSpecs.add("wifi");
        tileSpecs.add("bt");
        tileSpecs.add("cell");
        tileSpecs.add("battery");
        tileSpecs.add("flashlight");
        tileSpecs.add("rotation");
        tileSpecs.add("airplane");
        tileSpecs.add("cast");
        tileSpecs.add("location");
        */

        loadTileSpecs(context);
        return mTileSpecs;
    }

    public static Object createTile(Object tileHost, String tileSpec) {
        Object tile;
        if (mTilesManager.getCustomTileSpecs().contains(tileSpec)) {
            tile = mTilesManager.createTile(tileSpec).getTile();
        } else {
            tile = XposedHelpers.callMethod(tileHost, "createTile", tileSpec);
        }
        XposedHelpers.setAdditionalInstanceField(tile, TILE_SPEC_NAME, tileSpec);
        return tile;
    }

    public static void hook(ClassLoader classLoader) {
        try {
            Class<?> classTileHost = XposedHelpers.findClass(CLASS_TILE_HOST, classLoader);

            if (RomUtils.isCmBased()) {
                classQSUtils = XposedHelpers.findClass(CLASS_QS_UTILS, classLoader);
                try {
                    classQSConstants = XposedHelpers.findClass(CLASS_QS_CONSTANTS, classLoader);
                } catch (Throwable ignore) {
                }
            }

            if (ConfigUtils.header().enable_qs_editor) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
                    XposedHelpers.findAndHookMethod(classTileHost, "recreateTiles", recreateTilesHook); // On L, this method is void
                else
                    XposedHelpers.findAndHookMethod(classTileHost, "onTuningChanged", String.class, String.class, onTuningChangedHook);

                XposedHelpers.findAndHookMethod(classTileHost, "createTile", String.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (mTilesManager == null) mTilesManager = new TilesManager(param.thisObject);
                        String tileSpec = (String) param.args[0];
                        if (TilesManager.mCustomTileSpecs.contains(tileSpec)) {
                            param.setResult(mTilesManager.createTile(tileSpec).getTile());
                        }
                    }
                });
            }

            if (ConfigUtils.header().hide_edit_tiles) {
                XposedHelpers.findAndHookMethod(classTileHost, "loadTileSpecs", String.class, loadTileSpecsHook);
            }
        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error in hook", t);
        }
    }

    public static void loadTileSpecs(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        List<String> specs = new ArrayList<>();
        try {
            String jsonString = prefs.getString("qs_tiles", getDefaultTilesPref());
            JSONArray jsonArray = new JSONArray(jsonString);
            int appCount = jsonArray.length();
            for (int i = 0; i < appCount; i++) {
                String spec = jsonArray.getString(i);
                specs.add(spec);
            }
        } catch (JSONException e) {
            XposedHook.logE(TAG, "Error loading tile specs", e);
        }
        mTileSpecs = specs;
    }

    @SuppressLint("CommitPrefEdits")
    public static void saveTileSpecs(Context context, List<String> specs) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putString("qs_tiles", new JSONArray(specs).toString());
        editor.commit();
    }

    public static String getDefaultTilesPref() {
        List<String> specs = new ArrayList<>();
        specs.add("wifi");
        specs.add("bt");
        specs.add("cell");
        specs.add("battery");
        specs.add("flashlight");
        specs.add("rotation");
        specs.add("airplane");
        specs.add("cast");
        specs.add("location");
        return new JSONArray(specs).toString();
    }

    @SuppressWarnings("unchecked")
    public static List<String> getAvailableTiles(Context context) {
        List<String> specs = null;
        if (RomUtils.isCmBased()) {
            try {
                specs = (List<String>) XposedHelpers.callStaticMethod(classQSUtils, "getAvailableTiles", context);
            } catch (Throwable t) {
                try {
                    specs = (ArrayList<String>) ((ArrayList<String>) XposedHelpers.getStaticObjectField(classQSConstants, "TILES_AVAILABLE")).clone();
                } catch (Throwable t2) {
                    XposedHook.logW(TAG, "Couldn't fetch available tiles (" + t.getClass().getSimpleName() + " and " + t2.getClass().getSimpleName() + ")");
                }
            }
        }
        if (specs == null) {
            specs = new ArrayList<>();
            specs.add("wifi");
            specs.add("bt");
            specs.add("inversion");
            specs.add("cell");
            specs.add("airplane");

            // DND tile was added only on M!
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.M)
                specs.add("dnd");

            specs.add("rotation");
            specs.add("flashlight");
            specs.add("location");
            specs.add("cast");
            specs.add("hotspot");
            specs.addAll(bruteForceSpecs());
        }
        specs.add("battery");
        specs.remove("edit");
        return specs;
    }

    public static void recreateTiles() {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
                XposedHelpers.callMethod(mTileHost, "recreateTiles");
            else
                XposedHelpers.callMethod(mTileHost, "onTuningChanged", TILES_SETTING, "");
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    private static List<String> bruteForceSpecs() {
        XposedHook.logW(TAG, "Brute forcing tile specs!");
        List<String> specs = new ArrayList<>();
        String[] possibleSpecs = new String[]{/*"notifications", "data", "roaming", "dds", "apn",*/ "profiles", "performance",
                "adb_network", "nfc", "compass", "lockscreen", /*"lte", "visualizer",*/ "volume_panel", "screen_timeout",
                "usb_tether", "heads_up", "ambient_display", "sync", "battery_saver", "caffeine"/*, "edit"*/};
        for (String s : possibleSpecs) {
            try {
                XposedHelpers.callMethod(mTileHost, "createTile", s);
                specs.add(s);
            } catch (Throwable ignore) {
                XposedHook.logD(TAG, "bruteForceSpecs: spec \"" + s + "\" doesn't exist");
                // Not a applicable tile spec
            }
        }
        return specs;
    }

}