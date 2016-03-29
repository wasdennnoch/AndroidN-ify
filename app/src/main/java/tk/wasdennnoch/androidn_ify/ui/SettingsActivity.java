package tk.wasdennnoch.androidn_ify.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceFragment;

import java.io.File;

import tk.wasdennnoch.androidn_ify.R;

public class SettingsActivity extends Activity {

    //public static final String ACTION_SETTINGS_CHANGED = "an.action.ACTION_SETTINGS_CHANGED";
    //public static final String EXTRA_SETTINGS_FIX_SOUND_NOTIF_TILE = "an.extra.settings.FIX_SOUND_NOTIF_TILE";

    public static final String ACTION_RECENTS_CHANGED = "an.action.ACTION_RECENTS_CHANGED";
    public static final String EXTRA_RECENTS_DOUBLE_TAP_SPEED = "an.extra.recents.DOUBLE_TAP_SPEED";

    public static final String ACTION_GENERAL = "an.action.ACTION_GENERAL";
    public static final String EXTRA_GENERAL_DEBUG_LOG = "an.extra.general.DEBUG_LOG";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bare_settings);
        if (savedInstanceState == null)
            getFragmentManager().beginTransaction().replace(R.id.fragment, new Fragment()).commit();
    }



    public static class Fragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            //noinspection deprecation
            getPreferenceManager().setSharedPreferencesMode(Context.MODE_WORLD_READABLE);
            addPreferencesFromResource(R.xml.preferences);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            switch (key) {
                case "hide_launcher_icon":
                    int mode = prefs.getBoolean("hide_launcher_icon", false) ? PackageManager.COMPONENT_ENABLED_STATE_DISABLED : PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
                    getActivity().getPackageManager().setComponentEnabledSetting(new ComponentName(getActivity(), "tk.wasdennnoch.androidn_ify.SettingsAlias"), mode, PackageManager.DONT_KILL_APP);
                    break;
                default:
                    sendUpdateBroadcast(prefs, key);
                    break;
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onPause() {
            super.onPause();
            getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
            File sharedPrefsDir = new File(getActivity().getFilesDir(), "../shared_prefs");
            File sharedPrefsFile = new File(sharedPrefsDir, getPreferenceManager().getSharedPreferencesName() + ".xml");
            if (sharedPrefsFile.exists()) {
                //noinspection ResultOfMethodCallIgnored
                sharedPrefsFile.setReadable(true, false);
            }
        }

        @SuppressLint("CommitPrefEdits")
        private void sendUpdateBroadcast(SharedPreferences prefs, String key) {
            Intent intent = new Intent();
            switch (key) {
                case "double_tap_speed":
                    intent.setAction(ACTION_RECENTS_CHANGED);
                    intent.putExtra(EXTRA_RECENTS_DOUBLE_TAP_SPEED, prefs.getInt(key, 180));
                    break;
                case "debug_log":
                    intent.setAction(ACTION_GENERAL);
                    intent.putExtra(EXTRA_GENERAL_DEBUG_LOG, prefs.getBoolean(key, false));
                    break;
            }
            if (intent.getAction() != null) {
                prefs.edit().commit();
                getActivity().sendBroadcast(intent);
            }
        }

    }

}
