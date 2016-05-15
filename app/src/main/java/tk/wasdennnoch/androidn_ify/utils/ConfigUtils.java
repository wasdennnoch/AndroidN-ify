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

    public class SettingsConfig {
        public boolean enable_summaries;

        public boolean fix_sound_notif_tile;

        public SettingsConfig(XSharedPreferences prefs) {
            enable_summaries = prefs.getBoolean("enable_settings_summaries", true);
            fix_sound_notif_tile = prefs.getBoolean("fix_sound_notif_tile", false);
        }
    }

    public class RecentsConfig {
        public boolean enable;

        public boolean double_tap;
        public int double_tap_speed;
        public boolean navigate_recents;
        public int navigation_delay;
        public boolean large_recents;
        public boolean no_recents_image;

        public RecentsConfig(XSharedPreferences prefs) {
            double_tap = prefs.getBoolean("enable_recents_double_tap", true);
            double_tap_speed = prefs.getInt("double_tap_speed", 400);
            navigate_recents = prefs.getBoolean("enable_recents_navigation", true);
            navigation_delay = prefs.getInt("recents_navigation_delay", 1000);
            large_recents = prefs.getBoolean("enable_large_recents", true);
            no_recents_image = prefs.getBoolean("no_recents_image", true);

            enable = (double_tap || large_recents);
        }
    }

    public class StatusBarHeaderConfig {
        public boolean header;
        public int qs_tiles_count;
        public boolean new_click_behavior;
        public boolean large_first_row;
        public boolean hide_tuner_icon;
        public boolean hide_edit_tiles;
        public boolean smaller_header_clock;
        public boolean full_width_volume;

        public StatusBarHeaderConfig(XSharedPreferences prefs) {
            header = prefs.getBoolean("enable_notification_header", true);
            qs_tiles_count = prefs.getInt("notification_header_qs_tiles_count", 5);
            new_click_behavior = prefs.getBoolean("enable_new_tile_click_behavior", true);
            large_first_row = prefs.getBoolean("enable_large_first_row", false);
            hide_tuner_icon = prefs.getBoolean("hide_tuner_icon", false);
            hide_edit_tiles = prefs.getBoolean("hide_edit_tiles", false);
            smaller_header_clock = prefs.getBoolean("smaller_header_clock", false);
            full_width_volume = prefs.getBoolean("notification_full_width_volume", false);
        }
    }

    public class NotificationsConfig {
        public boolean enable;

        public boolean change_style;
        public boolean dark_theme;
        public boolean dismiss_button;
        public boolean allow_load_label_with_pm;

        public List<String> blacklistedApps;

        public NotificationsConfig(XSharedPreferences prefs) {
            change_style = prefs.getBoolean("notification_change_style", true);
            dark_theme = prefs.getBoolean("notification_dark_theme", false);
            dismiss_button = prefs.getBoolean("notification_dismiss_button", true);
            allow_load_label_with_pm = prefs.getBoolean("notification_allow_load_label_with_pm", false);

            enable = (change_style || dark_theme || dismiss_button);
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

}
