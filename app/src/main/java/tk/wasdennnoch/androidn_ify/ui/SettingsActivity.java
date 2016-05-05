package tk.wasdennnoch.androidn_ify.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.io.File;

import tk.wasdennnoch.androidn_ify.R;

public class SettingsActivity extends Activity {

    public static final String ACTION_RECENTS_CHANGED = "tk.wasdennnoch.androidn_ify.action.ACTION_RECENTS_CHANGED";
    public static final String EXTRA_RECENTS_DOUBLE_TAP_SPEED = "extra.recents.DOUBLE_TAP_SPEED";
    public static final String ACTION_GENERAL = "tk.wasdennnoch.androidn_ify.action.ACTION_GENERAL";
    public static final String EXTRA_GENERAL_DEBUG_LOG = "extra.general.DEBUG_LOG";
    public static final String ACTION_KILL_SYSTEMUI = "tk.wasdennnoch.androidn_ify.action.ACTION_KILL_SYSTEMUI";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        //noinspection ConstantConditions
        if (isActivated() && !isPrefsFileReadable()) {
            findViewById(R.id.prefs_not_readable_warning).setVisibility(View.VISIBLE);
        }
        if (savedInstanceState == null)
            getFragmentManager().beginTransaction().replace(R.id.fragment, new Fragment()).commit();
    }

    private boolean isActivated() {
        return false;
    }

    private boolean isPrefsFileReadable() {
        return true;
    }


    public static class Fragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            //noinspection deprecation
            getPreferenceManager().setSharedPreferencesMode(Context.MODE_WORLD_READABLE);
            addPreferencesFromResource(R.xml.preferences);
            // SELinux test, see XposedHook
            getPreferenceManager().getSharedPreferences().edit().putBoolean("can_read_prefs", true).commit();
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

        @SuppressLint("SetWorldReadable")
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.restart_systemui:
                showRestartSystemUIDialog();
                return true;
            case R.id.about:
                startActivity(new Intent(this, AboutActivity.class));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showRestartSystemUIDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.restart_systemui)
                .setMessage(R.string.restart_systemui_message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        sendBroadcast(new Intent(ACTION_KILL_SYSTEMUI));
                        Toast.makeText(SettingsActivity.this, R.string.restart_broadcast_sent, Toast.LENGTH_SHORT).show();
                    }
                }).show();
    }

}
