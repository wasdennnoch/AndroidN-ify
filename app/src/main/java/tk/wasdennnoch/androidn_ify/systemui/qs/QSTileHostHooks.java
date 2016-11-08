package tk.wasdennnoch.androidn_ify.systemui.qs;

import android.annotation.Nullable;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.systemui.notifications.StatusBarHeaderHooks;
import tk.wasdennnoch.androidn_ify.systemui.qs.tiles.NekoTile;
import tk.wasdennnoch.androidn_ify.utils.ConfigUtils;
import tk.wasdennnoch.androidn_ify.utils.MiscUtils;
import tk.wasdennnoch.androidn_ify.utils.SettingsUtils;
import tk.wasdennnoch.androidn_ify.utils.ViewUtils;

@SuppressLint("StaticFieldLeak")
public class QSTileHostHooks {
    public static final String TAG = "QSTileHostHooks";

    static final String CLASS_TILE_HOST = "com.android.systemui.statusbar.phone.QSTileHost";
    private static final String CLASS_CUSTOM_HOST = "com.android.systemui.tuner.QsTuner$CustomHost";
    private static final String CLASS_QS_UTILS_M = "org.cyanogenmod.internal.util.QSUtils";
    private static final String CLASS_QS_CONSTANTS_M = "org.cyanogenmod.internal.util.QSConstants";
    private static final String CLASS_QS_UTILS_L = "com.android.internal.util.cm.QSUtils";
    private static final String CLASS_QS_CONSTANTS_L = "com.android.internal.util.cm.QSConstants";
    private static final String CLASS_TUNER_SERVICE = "com.android.systemui.tuner.TunerService";
    private static final String TILES_SETTING = "sysui_qs_tiles";
    private static final String TILES_SECURE = "sysui_qs_tiles_secure";
    static final String TILE_SPEC_NAME = "tileSpec";
    public static final String KEY_QUICKQS_TILEVIEW = "QuickQS_TileView";
    static final String KEY_EDIT_TILEVIEW = "Edit_TileView";

    private static TilesManager mTilesManager = null;
    static List<String> mTileSpecs = null;
    static List<String> mSecureTiles = new ArrayList<>();

    private static Class<?> classQSUtils;
    private static Class<?> classQSConstants;
    private static Class<?> classCustomHost;

    private static Object mTileHost = null;
    public static KeyguardMonitor mKeyguard;

    private static boolean mIsCm;

    private static final List<String> GB_TILE_KEYS = new ArrayList<>(Arrays.asList(
            "gb_tile_battery",
            "gb_tile_nfc",
            "gb_tile_gps_slimkat",
            "gb_tile_gps_alt",
            "gb_tile_ringer_mode",
            "gb_tile_volume",
            "gb_tile_network_mode",
            "gb_tile_smart_radio",
            "gb_tile_sync",
            "gb_tile_torch",
            "gb_tile_sleep",
            "gb_tile_stay_awake",
            "gb_tile_quickrecord",
            "gb_tile_quickapp",
            "gb_tile_quickapp2",
            "gb_tile_expanded_desktop",
            "gb_tile_screenshot",
            "gb_tile_gravitybox",
            "gb_tile_usb_tether",
            "gb_tile_music",
            "gb_tile_lock_screen",
            "gb_tile_quiet_hours",
            "gb_tile_compass"
    ));

    // MM
    private static final XC_MethodHook onTuningChangedHook = new XC_MethodHook(XC_MethodHook.PRIORITY_HIGHEST) {
        @SuppressWarnings("unchecked")
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            if (classCustomHost != null && classCustomHost.isAssignableFrom(param.thisObject.getClass()))
                return;

            XposedHook.logD(TAG, "onTuningChangedHook#before called with key '" + param.args[0] + "' and newValue '" + param.args[1] + "'");

            if (mTileHost == null)
                mTileHost = param.thisObject;
            if (mKeyguard == null)
                mKeyguard = new KeyguardMonitor((Context) XposedHelpers.getObjectField(param.thisObject, "mContext"), XposedHelpers.getObjectField(param.thisObject, "mKeyguard"));
            if (mTilesManager == null)
                mTilesManager = new TilesManager(mTileHost);

            String newValue = (String) param.args[1];

            if (TILES_SECURE.equals(param.args[0])) {
                mSecureTiles = loadSecureTilesFromList(newValue);
                mTilesManager.onSecureTilesChanged(mSecureTiles);
                param.setResult(null);
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            if (!TILES_SETTING.equals(param.args[0])) return;

            Map<String, Object> mTiles = (Map<String, Object>) XposedHelpers.getObjectField(param.thisObject, "mTiles");
            List<String> mTileSpecs;
            try {
                mTileSpecs = (List<String>) XposedHelpers.getObjectField(param.thisObject, "mTileSpecs"); // TOD
            } catch (Throwable t) { // PA
                Object tileSpecsWrapper = XposedHelpers.callMethod(param.thisObject, "loadTileSpecs");
                mTileSpecs = (List<String>) XposedHelpers.getObjectField(tileSpecsWrapper, "list");
            }

            final LinkedHashMap<String, Object> newTiles = new LinkedHashMap<>();
            for (String tileSpec : mTileSpecs) {
                if (mTiles.containsKey(tileSpec)) {
                    Object tile = mTiles.get(tileSpec);
                    XposedHook.logD(TAG, "Using available tile for spec " + tileSpec + " with class " + tile.getClass().getName());
                    XposedHelpers.setAdditionalInstanceField(tile, TILE_SPEC_NAME, tileSpec);
                    newTiles.put(tileSpec, tile);
                    mTiles.remove(tileSpec);
                } else { // GB already adds all of its tiles even if they aren't selected so we don't have to create them anymore
                    XposedHook.logD(TAG, "Creating new tile for spec " + tileSpec);
                    Object tile = createTile(param.thisObject, tileSpec);
                    if (tile != null)
                        newTiles.put(tileSpec, tile);
                }
            }

            for (Map.Entry<String, Object> tile : mTiles.entrySet()) {
                if (!mTileSpecs.contains(tile.getKey())) {
                    Object qsTile = tile.getValue();
                    // Xposed stores additional keys in a WeakHashMap, so in theory it isn't even necessary to remove them prior to destruction
                    XposedHelpers.removeAdditionalInstanceField(qsTile, KEY_QUICKQS_TILEVIEW);
                    XposedHelpers.removeAdditionalInstanceField(qsTile, KEY_EDIT_TILEVIEW);
                    XposedHelpers.removeAdditionalInstanceField(qsTile, "gbTileKey");
                    XposedHelpers.callMethod(qsTile, "handleDestroy");
                }
            }
            mTiles.clear();
            mTiles.putAll(newTiles);

            Object mCallback = XposedHelpers.getObjectField(param.thisObject, "mCallback");
            if (mCallback != null)
                XposedHelpers.callMethod(mCallback, "onTilesChanged");
        }
    };

    private static XC_MethodReplacement loadTileSpecsHook = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            return loadTileSpecsFromList((String) param.args[0]);
        }
    };

    // LP
    // TODO GB support for LP
    private static final XC_MethodHook recreateTilesHook = new XC_MethodHook(XC_MethodHook.PRIORITY_LOWEST) {
        @SuppressWarnings("unchecked")
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            if (classCustomHost != null && classCustomHost.isAssignableFrom(param.thisObject.getClass()))
                return;

            XposedHook.logD(TAG, "recreateTilesHook called");

            // Thanks to GravityBox for this
            if (mTileHost == null)
                mTileHost = param.thisObject;

            if (mKeyguard == null)
                mKeyguard = new KeyguardMonitor((Context) XposedHelpers.getObjectField(param.thisObject, "mContext"), XposedHelpers.getObjectField(param.thisObject, "mKeyguard"));

            mTileSpecs = new ArrayList<>(); // Do this since mTileSpecs doesn't exist on LP

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
            if (classCustomHost != null && classCustomHost.isAssignableFrom(param.thisObject.getClass()))
                return;
            if (mTilesManager == null)
                mTilesManager = new TilesManager(param.thisObject);

            List<String> tileSpecs = new ArrayList<>(); // Do this since mTileSpecs doesn't exist on LP
            Map<String, Object> tileMap = (Map<String, Object>) XposedHelpers.getObjectField(param.thisObject, "mTiles");
            Context context = (Context) XposedHelpers.callMethod(param.thisObject, "getContext");

            tileSpecs.clear();
            tileSpecs.addAll(loadTileSpecsFromPrefs(context));
            tileMap.clear();

            if (tileSpecs.size() > 0 && tileSpecs.get(0).equals("default")) {
                tileSpecs.remove(0);
                tileSpecs.addAll(TileAdapter.getDefaultTiles());
            }
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

    private static List<String> loadTileSpecsFromList(String tileList) {
        if (tileList == null || tileList.isEmpty()) {
            tileList = TextUtils.join(",", TileAdapter.getDefaultTiles());
            XposedHook.logD(TAG, "loadTileSpecsFromList: Using default tiles because tileList is empty");
        }
        final ArrayList<String> tiles = new ArrayList<>();
        boolean addedDefault = false;
        for (String tile : tileList.split(",")) {
            tile = tile.trim();
            if (tile.isEmpty()) continue;
            if (tile.equals("default")) {
                if (!addedDefault) {
                    tiles.addAll(TileAdapter.getDefaultTiles());
                    XposedHook.logD(TAG, "loadTileSpecsFromList: Adding default tiles");
                    addedDefault = true;
                }
            } else {
                tiles.add(tile);
            }
        }
        if (ConfigUtils.qs().hide_edit_tiles)
            tiles.remove("edit");
        XposedHook.logD(TAG, "loadTileSpecsFromList: Loaded specs '" + TextUtils.join(", ", tiles) + "'");
        mTileSpecs = tiles;
        return tiles;
    }

    private static List<String> loadTileSpecsFromPrefs(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        List<String> tiles = new ArrayList<>();
        try {
            String jsonString = prefs.getString("qs_tiles", getDefaultTilesPref());
            JSONArray jsonArray = new JSONArray(jsonString);
            int appCount = jsonArray.length();
            for (int i = 0; i < appCount; i++) {
                String spec = jsonArray.getString(i);
                tiles.add(spec);
            }
        } catch (JSONException e) {
            XposedHook.logE(TAG, "Error loading tile specs from prefs", e);
        }
        mTileSpecs = tiles;
        return tiles;
    }

    private static List<String> loadSecureTilesFromList(String secureTileList) {
        final ArrayList<String> specs = new ArrayList<>();
        if (secureTileList == null) {
            mSecureTiles = specs;
            return specs;
        }
        for (String tile : secureTileList.split(",")) {
            tile = tile.trim();
            if (tile.isEmpty()) continue;
            specs.add(tile);
        }
        mSecureTiles = specs;
        return specs;
    }

    @Nullable
    private static Object createTile(Object tileHost, String tileSpec) {
        try {
            Object tile;
            if (mTilesManager.getCustomTileSpecs().contains(tileSpec)) {
                tile = mTilesManager.createTile(tileSpec).getTile();
            } else {
                tile = mTilesManager.createAospTile(tileHost, tileSpec).getTile();
            }
            if (tile == null) return null;
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
                mIsCm = false;
                try {
                    if (ConfigUtils.M) {
                        classQSUtils = XposedHelpers.findClass(CLASS_QS_UTILS_M, classLoader);
                        classQSConstants = XposedHelpers.findClass(CLASS_QS_CONSTANTS_M, classLoader);
                    } else {
                        classQSUtils = XposedHelpers.findClass(CLASS_QS_UTILS_L, classLoader);
                        classQSConstants = XposedHelpers.findClass(CLASS_QS_CONSTANTS_L, classLoader);
                    }
                    XposedHelpers.findAndHookMethod(classTileHost, "setEditing", boolean.class, new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                            if ((boolean) param.args[0]) {
                                View settingsButton = StatusBarHeaderHooks.getSettingsButton();
                                int[] loc = new int[2];
                                ViewUtils.getRelativePosition(loc, settingsButton, StatusBarHeaderHooks.mStatusBarHeaderView);
                                StatusBarHeaderHooks.showEditDismissingKeyguard(loc[0] + settingsButton.getWidth() / 2,
                                        loc[1] + settingsButton.getHeight() / 2);
                            }
                            return null;
                        }
                    });
                    mIsCm = true;
                } catch (Throwable ignore) {
                }
                if (ConfigUtils.M) {
                    final Class<?> classTunerService = XposedHelpers.findClass(CLASS_TUNER_SERVICE, classLoader);
                    XposedBridge.hookAllConstructors(classTileHost, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            Object tunerService = XposedHelpers.callStaticMethod(classTunerService, "get", XposedHelpers.getObjectField(param.thisObject, "mContext"));
                            if (mIsCm) // CM QSTileHost doesn't use Settings.Secure, so we need to add it.
                                XposedHelpers.callMethod(tunerService, "addTunable", param.thisObject, TILES_SETTING);
                            try {
                                XposedHelpers.callMethod(tunerService, "addTunable", param.thisObject, TILES_SECURE);
                            } catch (Throwable t) { // Candy6
                                // The Candy QSTileHost is copied from LP, so just ignore this and let the LP code do the magic
                            }
                        }
                    });
                }
                try {
                    classCustomHost = XposedHelpers.findClass(CLASS_CUSTOM_HOST, classLoader);
                } catch (Throwable ignore) {
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
                try {
                    XposedHelpers.findAndHookMethod(classTileHost, "loadTileSpecs", String.class, loadTileSpecsHook);
                } catch (Throwable t) { // OOS3
                    XposedHelpers.findAndHookMethod(classTileHost, "loadTileSpecs", loadTileSpecsHook);
                }

                XposedHelpers.findAndHookMethod(classTileHost, "createTile", String.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (classCustomHost != null && classCustomHost.isAssignableFrom(param.thisObject.getClass()))
                            return;
                        if (mTilesManager == null)
                            mTilesManager = new TilesManager(param.thisObject);
                        String tileSpec = (String) param.args[0];
                        if (TilesManager.mCustomTileSpecs.contains(tileSpec)) {
                            param.setResult(mTilesManager.createTile(tileSpec).getTile());
                        }
                    }
                });
            }
        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error in hook", t);
        }
    }

    @SuppressLint("CommitPrefEdits")
    static void saveTileSpecs(Context context, List<String> specs) {
        if (ConfigUtils.M) {
            SettingsUtils.putStringForCurrentUser(context.getContentResolver(), TILES_SETTING, TextUtils.join(",", specs));
        } else {
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
            editor.putString("qs_tiles", new JSONArray(specs).toString());
            editor.commit();
        }
    }

    // TODO secure tiles on LP
    @SuppressLint("CommitPrefEdits")
    static void saveSecureTileSpecs(Context context, List<String> specs) {
        String s = "";
        for (String sp : specs) s += sp;
        XposedHook.logD(TAG, "saveSecureTileSpecs called with specs: " + s);
        if (ConfigUtils.M) {
            SettingsUtils.putStringForCurrentUser(context.getContentResolver(), TILES_SECURE, TextUtils.join(",", specs));
        } else {
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
            editor.putString("qs_tiles_secure", new JSONArray(specs).toString());
            editor.commit();
        }
    }

    private static String getDefaultTilesPref() {
        return new JSONArray(TileAdapter.getDefaultTiles()).toString();
    }

    @SuppressWarnings("unchecked")
    static List<String> getAvailableTiles(Context context) {
        List<String> specs = new ArrayList<>();
        try { // Get the available tiles from the SystemUI config.xml
            String[] availableSpecs = context.getString(
                    context.getResources().getIdentifier("quick_settings_tiles_default", "string", XposedHook.PACKAGE_SYSTEMUI))
                    .split(",");
            if (ConfigUtils.qs().alternative_qs_loading || (availableSpecs.length == 1 && availableSpecs[0].equals("default")))
                throw new Throwable();
            for (String s : availableSpecs) {
                if (!TextUtils.isEmpty(s))
                    specs.add(s);
            }
            XposedHook.logD(TAG, "Found " + specs.size() + " specs in config.xml");
        } catch (Throwable t) {
            try { // On CM use the QSUtils
                try {
                    specs = (List<String>) XposedHelpers.callStaticMethod(classQSUtils, "getAvailableTiles", context);
                } catch (Throwable t2) {
                    specs = (ArrayList<String>) ((ArrayList<String>) XposedHelpers.getStaticObjectField(classQSConstants, "TILES_AVAILABLE")).clone();
                }
                XposedHook.logD(TAG, "Found " + specs.size() + " tiles in getAvailableTiles / TILES_AVAILABLE");
            } catch (Throwable t2) { // If that fails too try them all
                specs.add("wifi");
                specs.add("bt");
                specs.add("inversion");
                specs.add("cell");
                specs.add("airplane");
                if (ConfigUtils.M)
                    specs.add("dnd");
                specs.add("rotation");
                specs.add("flashlight");
                specs.add("location");
                specs.add("cast");
                specs.add("hotspot");
                specs.addAll(bruteForceSpecs());
            }
        }
        specs.addAll(TilesManager.mCustomTileSpecs);
        specs.remove("edit");
        if (!TilesManager.enableNeko)
            specs.remove(NekoTile.TILE_SPEC);
        if (ConfigUtils.qs().inject_gb_tiles && MiscUtils.isGBInstalled(context)) {
            specs.addAll(GB_TILE_KEYS);
        }
        if (XposedHook.debug) {
            String s = "";
            for (String sp : specs)
                s += sp + ", ";
            s = s.substring(0, s.length() - 2);
            XposedHook.logD(TAG, "getAvailableTiles: Found specs: " + s);
        }
        return specs;
    }

    private static List<String> getCurrentTileSpecs() {
        List<String> specs = new ArrayList<>();
        for (String spec : mTileSpecs) {
            if (spec == null) return specs;
            specs.add(spec);
        }
        return specs;
    }

    public static void addSpec(Context context, String spec) {
        List<String> specs = getCurrentTileSpecs();
        specs.add(spec);
        saveTileSpecs(context, specs);
        if (!ConfigUtils.M)
            recreateTiles();
    }

    public static void recreateTiles() {
        try {
            if (!ConfigUtils.M) {
                XposedHelpers.callMethod(mTileHost, "recreateTiles");
            }
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    private static List<String> bruteForceSpecs() {
        XposedHook.logD(TAG, "Brute forcing tile specs!");
        List<String> specs = new ArrayList<>();
        String[] possibleSpecs = new String[]{"dataconnection", "cell1", "cell2", "notifications", "data", "roaming", "dds", "apn", "profiles",
                "performance", "adb_network", "nfc", "compass", "lockscreen", "lte", "volume_panel", "screen_timeout", "timeout",
                "usb_tether", "heads_up", "ambient_display", "sync", "battery_saver", "caffeine", "music", "next_alarm",
                "ime_selector", "ime", "su", "adb", "live_display", "themes", "brightness", "screen_off", "screenoff", "screenshot",
                "expanded_desktop", "reboot", "configurations", "navbar", "appcirclebar", "kernel_adiutor", "screenrecord",
                "gesture_anywhere", "power_menu", "app_picker", "kill_app", "hw_keys", "sound", "pulse", "pie", "float_mode",
                "nightmode", "immersive", "floating", "halo", "stamina", "datatraffic", "screenmirroring", "throw", "volte",
                "tethering", "detectusbdevice", "audioprofile", "hotknot"};
        for (String s : possibleSpecs) {
            if (bruteForceSpec(s)) specs.add(s);
        }
        XposedHook.logD(TAG, "bruteForceSpecs: found " + specs.size() + " applicable tile specs");
        return specs;
    }

    private static boolean bruteForceSpec(String spec) {
        try {
            XposedHelpers.callMethod(mTileHost, "createTile", spec);
            return true;
        } catch (Throwable ignore) {
            return false;
            // Not an applicable tile spec
        }
    }

}