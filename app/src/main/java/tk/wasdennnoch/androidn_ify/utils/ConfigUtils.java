package tk.wasdennnoch.androidn_ify.utils;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XSharedPreferences;
import tk.wasdennnoch.androidn_ify.XposedHook;

public class ConfigUtils {

    private static final String TAG = "ConfigUtils";

    private static ConfigUtils mInstance;

    private XSharedPreferences mPrefs;
    public SettingsConfig settings;
    public RecentsConfig recents;
    public StatusBarHeaderConfig header;
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
        header = new StatusBarHeaderConfig(mPrefs);
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

    public static StatusBarHeaderConfig header() {
        return getInstance().header;
    }

    public static NotificationsConfig notifications() {
        return getInstance().notifications;
    }

    public static LockscreenConfig lockscreen() {
        return getInstance().lockscreen;
    }

    public class SettingsConfig {
        public boolean enable_summaries;

        public boolean fix_sound_notif_tile;
        public boolean enable_n_platlogo;

        public SettingsConfig(XSharedPreferences prefs) {
            enable_summaries = prefs.getBoolean("enable_settings_summaries", true);
            fix_sound_notif_tile = prefs.getBoolean("fix_sound_notif_tile", false);
            enable_n_platlogo = prefs.getBoolean("enable_n_platlogo", true);
        }
    }

    public class RecentsConfig {
        public boolean double_tap;
        public int double_tap_speed;
        public boolean navigate_recents;
        public boolean force_double_tap;
        public int navigation_delay;
        public boolean large_recents;
        public boolean no_recents_image;

        public RecentsConfig(XSharedPreferences prefs) {
            double_tap = prefs.getBoolean("enable_recents_double_tap", true);
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

    public class StatusBarHeaderConfig {
        public boolean header;
        public boolean keep_qs_panel_background;
        public int qs_tiles_count;
        public boolean battery_tile_show_percentage;
        public boolean alternative_quick_qs_method;
        public boolean new_click_behavior;
        public boolean large_first_row;
        public boolean smaller_header_clock;
        public boolean full_width_volume;
        public boolean hide_tuner_icon;
        public boolean hide_edit_tiles;
        public boolean hide_carrier_label;

        public StatusBarHeaderConfig(XSharedPreferences prefs) {
            header = prefs.getBoolean("enable_notification_header", true);
            keep_qs_panel_background = prefs.getBoolean("keep_qs_panel_background", false);
            qs_tiles_count = prefs.getInt("notification_header_qs_tiles_count", 5);
            battery_tile_show_percentage = prefs.getBoolean("battery_tile_show_percentage", true);
            alternative_quick_qs_method = prefs.getBoolean("alternative_quick_qs_method", false);
            new_click_behavior = prefs.getBoolean("enable_new_tile_click_behavior", true);
            large_first_row = prefs.getBoolean("enable_large_first_row", false);
            smaller_header_clock = prefs.getBoolean("smaller_header_clock", false);
            full_width_volume = prefs.getBoolean("notification_full_width_volume", false);
            hide_tuner_icon = prefs.getBoolean("hide_tuner_icon", false);
            hide_edit_tiles = prefs.getBoolean("hide_edit_tiles", false);
            hide_carrier_label = prefs.getBoolean("hide_carrier_label", false);
        }
    }

    public class NotificationsConfig {
        public boolean enable;

        public boolean change_style;
        public boolean dismiss_button;
        public boolean custom_appname_color;
        public int appname_color;
        public boolean custom_actions_color;
        public int actions_color;

        public List<String> blacklistedApps;

        public NotificationsConfig(XSharedPreferences prefs) {
            change_style = prefs.getBoolean("notification_change_style", true);
            dismiss_button = prefs.getBoolean("notification_dismiss_button", true);
            custom_appname_color = prefs.getBoolean("notifications_custom_color", false);
            appname_color = prefs.getInt("notifications_appname_color", 0);
            custom_actions_color = prefs.getBoolean("notifications_custom_actions_color", false);
            actions_color = prefs.getInt("actions_background_colors", 0);

            enable = (change_style || dismiss_button);
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
