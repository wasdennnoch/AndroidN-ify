package tk.wasdennnoch.androidn_ify.utils;

import android.os.Build;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import de.robv.android.xposed.XSharedPreferences;
import tk.wasdennnoch.androidn_ify.XposedHook;

public class ConfigUtils {

    private static final String TAG = "ConfigUtils";

    public static final boolean M = Build.VERSION.SDK_INT >= 23;

    private static ConfigUtils mInstance;
    private XSharedPreferences mPrefs;

    public SettingsConfig settings;
    public RecentsConfig recents;
    public QuickSettingsConfig qs;
    public NotificationsConfig notifications;
    public LockscreenConfig lockscreen;

    private ConfigUtils() {
        mInstance = this;
        mPrefs = new XSharedPreferences(XposedHook.class.getPackage().getName());
        loadConfig();
    }

    public void reload() {
        mPrefs.reload();
        loadConfig();
    }

    private void loadConfig() {
        settings = new SettingsConfig(mPrefs);
        recents = new RecentsConfig(mPrefs);
        qs = new QuickSettingsConfig(mPrefs);
        notifications = new NotificationsConfig(mPrefs);
        lockscreen = new LockscreenConfig(mPrefs);
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

    public XSharedPreferences getPrefs() {
        return mPrefs;
    }

    public class SettingsConfig {
        public boolean enable_summaries;

        public boolean fix_sound_notif_tile;
        public boolean enable_n_platlogo;
        public boolean use_namey_mcnameface;
        public boolean install_source;

        public SettingsConfig(XSharedPreferences prefs) {
            enable_summaries = prefs.getBoolean("enable_settings_summaries", true);
            fix_sound_notif_tile = prefs.getBoolean("fix_sound_notif_tile", false);
            enable_n_platlogo = prefs.getBoolean("enable_n_platlogo", true);
            use_namey_mcnameface = prefs.getBoolean("use_namey_mcnameface", false);
            install_source = M && prefs.getBoolean("enable_install_source", true);
        }
    }

    public class RecentsConfig {
        public boolean double_tap;
        public boolean alternative_method;
        public int double_tap_speed;
        public boolean navigate_recents;
        public boolean force_double_tap;
        public int navigation_delay;
        public boolean large_recents;
        public boolean no_recents_image;

        public RecentsConfig(XSharedPreferences prefs) {
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
        public boolean header;
        public boolean keep_header_background;
        public boolean keep_qs_panel_background;
        public int qs_tiles_count;
        public boolean battery_tile_show_percentage;
        public boolean enable_qs_editor;
        public boolean allow_fancy_qs_transition;
        public boolean new_click_behavior;
        public boolean large_first_row;
        public int header_clock_size;
        public boolean hide_tuner_icon;
        public boolean hide_edit_tiles;
        public boolean hide_carrier_label;

        public QuickSettingsConfig(XSharedPreferences prefs) {
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
            header_clock_size = Integer.parseInt(prefs.getString("header_clock_size", "0"));

            Set<String> keepBgs = prefs.getStringSet("keep_backgrounds", Collections.<String>emptySet());
            keep_header_background = keepBgs.contains("header");
            keep_qs_panel_background = keepBgs.contains("panel");
        }
    }

    public class NotificationsConfig {
        public boolean change_style;
        public boolean dismiss_button;
        public boolean custom_actions_color;
        public boolean experimental;
        public int keyguard_max;
        public int actions_color;

        public List<String> blacklistedApps;

        public NotificationsConfig(XSharedPreferences prefs) {
            change_style = prefs.getBoolean("notification_change_style", true);
            dismiss_button = prefs.getBoolean("notification_dismiss_button", true);
            custom_actions_color = prefs.getBoolean("notifications_custom_actions_color", false);
            experimental = M && prefs.getBoolean("notification_experimental", false);
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
    }

    public class LockscreenConfig {
        public boolean enable_emergency_info;

        public LockscreenConfig(XSharedPreferences prefs) {
            enable_emergency_info = prefs.getBoolean("enable_emergency_info", false);
        }
    }

}
