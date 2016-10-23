package tk.wasdennnoch.androidn_ify.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import de.robv.android.xposed.XSharedPreferences;
import tk.wasdennnoch.androidn_ify.BuildConfig;
import tk.wasdennnoch.androidn_ify.XposedHook;

@SuppressWarnings("WeakerAccess")
public class ConfigUtils {

    private static final String TAG = "ConfigUtils";

    public static final boolean M = Build.VERSION.SDK_INT >= 23;
    public static final boolean L1 = Build.VERSION.SDK_INT >= 22;
    public static boolean EXPERIMENTAL;

    private static ConfigUtils mInstance;
    private final SharedPreferences mPrefs;
    //private final SharedPreferences mPrefsRemote;

    public SettingsConfig settings;
    public RecentsConfig recents;
    public QuickSettingsConfig qs;
    public NotificationsConfig notifications;
    public LockscreenConfig lockscreen;
    public AssistantConfig assistant;

    private ConfigUtils() {
        mInstance = this;
        mPrefs = new XSharedPreferences(XposedHook.class.getPackage().getName());
        //Context context = (Context) XposedHelpers.callMethod(XposedHelpers.callStaticMethod(XposedHelpers.findClass("android.app.ActivityThread", null), "currentActivityThread"), "getSystemContext");
        //mPrefsRemote = new RemotePreferences(context, "tk.wasdennnoch.androidn_ify.PREFERENCES", "tk.wasdennnoch.androidn_ify_preferences");
        reload();
    }

    public static boolean isExperimental(SharedPreferences prefs) {
        return BuildConfig.DEBUG || prefs.getBoolean("enable_experimental_features", false);
    }

    public static boolean showExperimental(SharedPreferences prefs) {
        return BuildConfig.DEBUG || prefs.getBoolean("show_experimental_features", false);
    }

    private void reload() {
        if (mPrefs instanceof XSharedPreferences)
            ((XSharedPreferences) mPrefs).reload();
        loadConfig();
    }

    private void loadConfig() {
        EXPERIMENTAL = isExperimental(mPrefs);
        settings = new SettingsConfig(mPrefs);
        recents = new RecentsConfig(mPrefs);
        qs = new QuickSettingsConfig(mPrefs);
        notifications = new NotificationsConfig(mPrefs);
        lockscreen = new LockscreenConfig(mPrefs);
        assistant = new AssistantConfig(mPrefs);
    }

    public static ConfigUtils getInstance() {
        if (mInstance == null)
            mInstance = new ConfigUtils();
        return mInstance;
    }

    public static SettingsConfig settings() {
        return getInstance().settings;
    }

    public static RecentsConfig recents() {
        return getInstance().recents;
    }

    public static QuickSettingsConfig qs() {
        return getInstance().qs;
    }

    public static NotificationsConfig notifications() {
        return getInstance().notifications;
    }

    public static LockscreenConfig lockscreen() {
        return getInstance().lockscreen;
    }

    public static AssistantConfig assistant() {
        return getInstance().assistant;
    }

    public SharedPreferences getPrefs() {
        return mPrefs;
    }

    public class SettingsConfig {
        public final boolean enable_summaries;

        public final boolean fix_sound_notif_tile;
        public final boolean enable_n_platlogo;
        public final boolean use_namey_mcnameface;
        public final boolean install_source;
        public final boolean n_style_dashboard;
        public final boolean enable_drawer;

        public SettingsConfig(SharedPreferences prefs) {
            enable_summaries = prefs.getBoolean("enable_settings_summaries", true);
            fix_sound_notif_tile = prefs.getBoolean("fix_sound_notif_tile", false);
            enable_n_platlogo = prefs.getBoolean("enable_n_platlogo", true);
            use_namey_mcnameface = prefs.getBoolean("use_namey_mcnameface", false);
            install_source = prefs.getBoolean("enable_install_source", true);
            n_style_dashboard = EXPERIMENTAL && prefs.getBoolean("enable_n_style_settings_dashboard", true);
            enable_drawer = EXPERIMENTAL && prefs.getBoolean("enable_settings_drawer", true);
        }
    }

    public class RecentsConfig {
        public final boolean double_tap;
        public final boolean alternative_method;
        public final int double_tap_speed;
        public boolean navigate_recents;
        public boolean force_double_tap;
        public final int navigation_delay;
        public final boolean large_recents;
        public final boolean no_recents_image;

        public RecentsConfig(SharedPreferences prefs) {
            double_tap = prefs.getBoolean("enable_recents_double_tap", true);
            alternative_method = prefs.getBoolean("alternative_method", false);
            double_tap_speed = prefs.getInt("double_tap_speed", 400);
            navigation_delay = prefs.getInt("recents_navigation_delay", 1000);
            large_recents = prefs.getBoolean("enable_large_recents", true);
            no_recents_image = prefs.getBoolean("no_recents_image", true);
            force_double_tap = false;

            int recents_button_behavior = Integer.parseInt(prefs.getString("recents_button_behavior", "0"));
            switch (recents_button_behavior) {
                case 1:
                    navigate_recents = false;
                    break;
                case 0:
                    force_double_tap = true;
                case 2:
                    navigate_recents = true;
                    break;
            }
        }
    }

    public class QuickSettingsConfig {
        public final boolean header;
        public final boolean keep_header_background;
        public final boolean keep_qs_panel_background;
        public final int qs_tiles_count;
        public final boolean battery_tile_show_percentage;
        public final boolean enable_qs_editor;
        public final boolean allow_fancy_qs_transition;
        public final boolean new_click_behavior;
        public final boolean large_first_row;
        public final boolean hide_tuner_icon;
        public final boolean hide_edit_tiles;
        public final boolean hide_carrier_label;
        public final boolean disable_qs_paging;

        public QuickSettingsConfig(SharedPreferences prefs) {
            header = prefs.getBoolean("enable_notification_header", true);
            qs_tiles_count = prefs.getInt("notification_header_qs_tiles_count", 5);
            battery_tile_show_percentage = prefs.getBoolean("battery_tile_show_percentage", false);
            enable_qs_editor = prefs.getBoolean("enable_qs_editor", true);
            allow_fancy_qs_transition = prefs.getBoolean("allow_fancy_qs_transition", true);
            new_click_behavior = prefs.getBoolean("enable_new_tile_click_behavior", true);
            large_first_row = prefs.getBoolean("enable_large_first_row", false);
            hide_tuner_icon = prefs.getBoolean("hide_tuner_icon", false);
            hide_edit_tiles = prefs.getBoolean("hide_edit_tiles", false);
            hide_carrier_label = prefs.getBoolean("hide_carrier_label", false);
            disable_qs_paging = prefs.getBoolean("disable_qs_paging", false);

            Set<String> keepBgs = prefs.getStringSet("keep_backgrounds", Collections.<String>emptySet());
            keep_header_background = keepBgs.contains("header");
            keep_qs_panel_background = keepBgs.contains("panel");
        }
    }

    public class NotificationsConfig {
        public final boolean change_style;
        public final boolean dismiss_button;
        public final boolean custom_actions_color;
        public final boolean experimental;
        public final boolean allow_direct_reply_on_keyguard;
        public final boolean enable_notifications_background;
        public final boolean enable_data_disabled_indicator;
        public final boolean filter_sensitive_notifications;
        public final int keyguard_max;
        public final int actions_color;

        public List<String> blacklistedApps;
        public List<String> spoofAPIApps;

        public NotificationsConfig(SharedPreferences prefs) {
            change_style = prefs.getBoolean("notification_change_style", true);
            dismiss_button = prefs.getBoolean("notification_dismiss_button", true);
            custom_actions_color = prefs.getBoolean("notifications_custom_actions_color", false);
            experimental = M && EXPERIMENTAL && prefs.getBoolean("notification_experimental", false);
            allow_direct_reply_on_keyguard = prefs.getBoolean("allow_direct_reply_on_keyguard", false);
            enable_notifications_background = M && prefs.getBoolean("enable_notifications_background", true);
            enable_data_disabled_indicator = prefs.getBoolean("enable_data_disabled_indicator", true);
            filter_sensitive_notifications = M && EXPERIMENTAL;
            keyguard_max = prefs.getInt("notification_keyguard_max", 3);
            actions_color = prefs.getInt("actions_background_colors", 0);
        }

        public void loadBlacklistedApps() {
            List<String> apps = new ArrayList<>();
            try {
                String jsonString = mPrefs.getString("notification_blacklist_apps", "[]");
                JSONArray jsonArray = new JSONArray(jsonString);
                int appCount = jsonArray.length();
                for (int i = 0; i < appCount; i++) {
                    String app = jsonArray.getString(i);
                    apps.add(app);
                }
            } catch (JSONException e) {
                XposedHook.logE(TAG, "Error loading blacklisted apps", e);
            }
            blacklistedApps = apps;
        }

        public void loadSpoofAPIApps() {
            List<String> apps = new ArrayList<>();
            try {
                String jsonString = mPrefs.getString("notification_spoof_api_version", "[]");
                JSONArray jsonArray = new JSONArray(jsonString);
                int appCount = jsonArray.length();
                for (int i = 0; i < appCount; i++) {
                    String app = jsonArray.getString(i);
                    apps.add(app);
                }
            } catch (JSONException e) {
                XposedHook.logE(TAG, "Error loading spoof API apps", e);
            }
            spoofAPIApps = apps;
        }
    }

    public class LockscreenConfig {
        public final boolean enable_emergency_info;

        public LockscreenConfig(SharedPreferences prefs) {
            enable_emergency_info = prefs.getBoolean("enable_emergency_info", true);
        }
    }

    public class AssistantConfig {
        public final boolean enable_assistant;
        public final String google_app_hook_configs;

        public AssistantConfig(SharedPreferences prefs) {
            enable_assistant = prefs.getBoolean("enable_assistant", true);
            google_app_hook_configs = prefs.getString("google_app_hook_configs", "[]");
        }
    }

    @SuppressLint("WorldReadableFiles")
    @SuppressWarnings("deprecation")
    public static SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_WORLD_READABLE);
    }

}
