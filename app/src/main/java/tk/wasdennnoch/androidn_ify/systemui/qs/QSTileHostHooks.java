package tk.wasdennnoch.androidn_ify.systemui.qs;

import android.annotation.Nullable;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
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
    public static final String KEY_QUICKQS_TILEVIEW = "QuickQS_TileView";
    public static final String KEY_EDIT_TILEVIEW = "Edit_TileView";

    private static TilesManager mTilesManager = null;
    public static List<String> mTileSpecs = null;

    private static Class<?> classQSUtils;
    private static Class<?> classQSConstants;

    protected static Object mTileHost = null;

    // MM
    private static XC_MethodHook onTuningChangedHook = new XC_MethodHook() {
        
        @SuppressWarnings("unchecked")
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            if (mTileHost == null)
                mTileHost = param.thisObject;

            if (!TILES_SETTING.equals(param.args[0])) return;

            if (mTilesManager == null)
                mTilesManager = new TilesManager(mTileHost);

            Context context = (Context) XposedHelpers.callMethod(param.thisObject, "getContext");

            List<String> tileSpecs = (List<String>) XposedHelpers.getObjectField(param.thisObject, "mTileSpecs");
            List<String> newTileSpecs = getTileSpecs(context);
            Map<String, Object> tileMap = (Map<String, Object>) XposedHelpers.getObjectField(param.thisObject, "mTiles");
            Map<String, Object> newTiles = new LinkedHashMap<>();

            tileSpecs.clear();
            tileSpecs.addAll(newTileSpecs);

            for (Map.Entry<String, Object> tile : tileMap.entrySet()) {
                if (!tileSpecs.contains(tile.getKey())) {
                    Object qsTile = tile.getValue();
                    XposedHelpers.removeAdditionalInstanceField(qsTile, KEY_QUICKQS_TILEVIEW);
                    XposedHelpers.removeAdditionalInstanceField(qsTile, KEY_EDIT_TILEVIEW);
                    XposedHelpers.callMethod(qsTile, "handleDestroy");
                }
            }

            int tileSpecCount = tileSpecs.size();
            for (int i = 0; i < tileSpecCount; i++) {
                String spec = tileSpecs.get(i);
                if (tileMap.containsKey(spec)) {
                    newTiles.put(spec, tileMap.get(spec));
                } else {
                    Object tile = createTile(param.thisObject, spec);
                    if (tile != null)
                        newTiles.put(spec, tile);
                }
            }

            tileMap.clear();
            tileMap.putAll(newTiles);

            Object mCallback = XposedHelpers.getObjectField(param.thisObject, "mCallback");
            if (mCallback != null) XposedHelpers.callMethod(mCallback, "onTilesChanged");

            param.setResult(null);
        }
    };

    // LP
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
                Object tile = createTile(param.thisObject, spec);
                if (tile != null)
                    tileMap.put(spec, tile);
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
        loadTileSpecs(context);
        return mTileSpecs;
    }

    @Nullable
    public static Object createTile(Object tileHost, String tileSpec) {
        try {
            Object tile;
            if (mTilesManager.getCustomTileSpecs().contains(tileSpec)) {
                tile = mTilesManager.createTile(tileSpec).getTile();
            } else {
                tile = XposedHelpers.callMethod(tileHost, "createTile", tileSpec);
            }
            XposedHelpers.setAdditionalInstanceField(tile, TILE_SPEC_NAME, tileSpec);
            return tile;
        } catch (Throwable t) {
            XposedHook.logE(TAG, "Couldn't create tile with spec \"" + tileSpec + "\"", t);
        }
        return null;
    }

    public static void hook(ClassLoader classLoader) {
        try {
            Class<?> classTileHost = XposedHelpers.findClass(CLASS_TILE_HOST, classLoader);

            if (ConfigUtils.qs().enable_qs_editor) {

                if (RomUtils.isCmBased()) {
                    try {
                        classQSUtils = XposedHelpers.findClass(CLASS_QS_UTILS, classLoader);
                        classQSConstants = XposedHelpers.findClass(CLASS_QS_CONSTANTS, classLoader);
                    } catch (Throwable ignore) {
                    }
                }

                if (!ConfigUtils.M) {
                    XposedHelpers.findAndHookMethod(classTileHost, "recreateTiles", recreateTilesHook);
                } else {
                    try {
                        XposedHelpers.findAndHookMethod(classTileHost, "onTuningChanged", String.class, String.class, onTuningChangedHook);
                    } catch (Throwable t) { // Candy6
                        try {
                            XposedHelpers.findAndHookMethod(classTileHost, "recreateTiles", recreateTilesHook);
                        } catch (Throwable t2) {
                            XposedHook.logE(TAG, "Couldn't hook recreateTiles / onTuningChanged", null);
                        }
                    }
                }

                XposedHelpers.findAndHookMethod(classTileHost, "createTile", String.class, new XC_MethodHook() {

                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (mTilesManager == null)
                            mTilesManager = new TilesManager(param.thisObject);
                        String tileSpec = (String) param.args[0];
                        if (TilesManager.mCustomTileSpecs.contains(tileSpec)) {
                            param.setResult(mTilesManager.createTile(tileSpec).getTile());
                        }
                    }
                });
            }

            if (ConfigUtils.qs().hide_edit_tiles) {
                try {
                    XposedHelpers.findAndHookMethod(classTileHost, "loadTileSpecs", String.class, loadTileSpecsHook);
                } catch (Throwable t) { // OOS3
                    XposedHelpers.findAndHookMethod(classTileHost, "loadTileSpecs", loadTileSpecsHook);
                }
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
            if (Build.VERSION.SDK_INT >= 23)
                specs.add("dnd");
            specs.add("rotation");
            specs.add("flashlight");
            specs.add("location");
            specs.add("cast");
            specs.add("hotspot");
            specs.addAll(bruteForceSpecs());
        }
        specs.addAll(TilesManager.mCustomTileSpecs);
        specs.remove("edit");
        return specs;
    }

    public static void recreateTiles() {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                XposedHelpers.callMethod(mTileHost, "recreateTiles");
            } else {
                try {
                    XposedHelpers.callMethod(mTileHost, "onTuningChanged", TILES_SETTING, "");
                } catch (Throwable t) { // Candy6
                    XposedHelpers.callMethod(mTileHost, "recreateTiles");
                }
            }
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    private static List<String> bruteForceSpecs() {
        XposedHook.logI(TAG, "Brute forcing tile specs!");
        List<String> specs = new ArrayList<>();
        String[] possibleSpecs = new String[]{"cell1", "cell2", "notifications", "data", "roaming", "dds", "apn", "profiles", "performance",
                "adb_network", "nfc", "compass", "lockscreen", "lte", "volume_panel", "screen_timeout",
                "usb_tether", "heads_up", "ambient_display", "sync", "battery_saver", "caffeine", "music", "next_alarm",
                "ime_selector", "su", "adb", "live_display", "themes", "brightness", "screen_off", "screenshot", "expanded_desktop",
                "reboot", "configurations", "navbar", "appcirclebar", "kernel_adiutor", "screenrecord", "gesture_anywhere",
                "power_menu", "app_picker", "kill_app", "hw_keys", "sound", "pulse", "pie", "float_mode", "nightmode"};
        for (String s : possibleSpecs) {
            if (bruteForceSpec(s)) specs.add(s);
        }
        return specs;
    }

    private static boolean bruteForceSpec(String spec) {
        try {
            XposedHelpers.callMethod(mTileHost, "createTile", spec);
            return true;
        } catch (Throwable ignore) {
            XposedHook.logD(TAG, "bruteForceSpecs: spec \"" + spec + "\" doesn't exist");
            return false;
            // Not an applicable tile spec
        }
    }

}