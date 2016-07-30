package tk.wasdennnoch.androidn_ify.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Process;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.ui.misc.LogcatService;
import tk.wasdennnoch.androidn_ify.ui.preference.DropDownPreference;
import tk.wasdennnoch.androidn_ify.utils.ConfigUtils;
import tk.wasdennnoch.androidn_ify.utils.RomUtils;
import tk.wasdennnoch.androidn_ify.utils.UpdateUtils;
import tk.wasdennnoch.androidn_ify.utils.ViewUtils;

public class SettingsActivity extends Activity implements View.OnClickListener {

    private static final String TAG = "SettingsActivity";

    public static final String ACTION_RECENTS_CHANGED = "tk.wasdennnoch.androidn_ify.action.ACTION_RECENTS_CHANGED";
    public static final String EXTRA_RECENTS_DOUBLE_TAP_SPEED = "extra.recents.DOUBLE_TAP_SPEED";
    public static final String ACTION_FIX_INVERSION = "tk.wasdennnoch.androidn_ify.action.ACTION_FIX_INVERSION";
    public static final String ACTION_GENERAL = "tk.wasdennnoch.androidn_ify.action.ACTION_GENERAL";
    public static final String EXTRA_GENERAL_DEBUG_LOG = "extra.general.DEBUG_LOG";
    public static final String ACTION_KILL_SYSTEMUI = "tk.wasdennnoch.androidn_ify.action.ACTION_KILL_SYSTEMUI";

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        ViewUtils.applyTheme(this, prefs);
        super.onCreate(savedInstanceState);
        RomUtils.init(this);
        setContentView(R.layout.activity_settings);
        if (!isActivated()) {
            getActionBar().setSubtitle(R.string.not_activated);
        } else if (!isPrefsFileReadable()) {
            TextView warning = (TextView) findViewById(R.id.prefs_not_readable_warning);
            warning.setText(Html.fromHtml(getString(R.string.prefs_not_readable)));
            warning.setVisibility(View.VISIBLE);
            warning.setOnClickListener(this);
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

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.prefs_not_readable_warning:
                showDialog(0, R.string.prefs_not_readable_description, true, null);
                break;
        }
    }

    private void showDialog(int titleRes, int contentRes, boolean onlyOk, final Runnable okAction) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setMessage(Html.fromHtml(getString(contentRes)));
        if (titleRes > 0)
            builder.setTitle(titleRes);
        if (!onlyOk)
            builder.setNegativeButton(android.R.string.cancel, null);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (okAction != null)
                    okAction.run();
            }
        });
        View v = builder.show().findViewById(android.R.id.message);
        if (v instanceof TextView)
            ((TextView) v).setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void showRestartSystemUIDialog() {
        showDialog(R.string.restart_systemui, R.string.restart_systemui_message, false, new Runnable() {
            @Override
            public void run() {
                sendBroadcast(new Intent(ACTION_KILL_SYSTEMUI).setPackage(XposedHook.PACKAGE_SYSTEMUI));
                Toast.makeText(SettingsActivity.this, R.string.restart_broadcast_sent, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static class Fragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener, UpdateUtils.UpdateListener {

        @SuppressLint("CommitPrefEdits")
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            //noinspection deprecation
            getPreferenceManager().setSharedPreferencesMode(Context.MODE_WORLD_READABLE);
            addPreferencesFromResource(R.xml.preferences);
            SharedPreferences sharedPreferences = getPreferenceManager().getSharedPreferences();
            if (UpdateUtils.isEnabled(getActivity())) {
                if (sharedPreferences.getBoolean("check_for_updates", true))
                    UpdateUtils.check(getActivity(), this);
            } else {
                PreferenceCategory appCategory = (PreferenceCategory) findPreference("settings_app");
                Preference updatePref = getPreferenceScreen().findPreference("check_for_updates");
                appCategory.removePreference(updatePref);
            }
            // SELinux test, see XposedHook
            sharedPreferences.edit().putBoolean("can_read_prefs", true).commit();
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            switch (key) {
                case "app_dark_theme":
                case "theme_colorPrimary":
                case "force_english":
                    getActivity().recreate();
                    break;
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
        public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
            super.onPreferenceTreeClick(preferenceScreen, preference);
            if (preference instanceof PreferenceScreen) {
                PreferenceScreen screen = (PreferenceScreen) preference;
                if (screen.getDialog() != null)
                    ViewUtils.applyTheme(screen.getDialog(), getActivity(), preference.getSharedPreferences());
                switch (preference.getKey()) {
                    case "settings_recents":
                        DropDownPreference recentsBehaviorPref = (DropDownPreference) screen.findPreference("recents_button_behavior");
                        if (!ConfigUtils.M) {
                            lockPreference(recentsBehaviorPref);
                        } else {
                            final Preference delayPref = screen.findPreference("recents_navigation_delay");
                            if (recentsBehaviorPref.getValue().equals("2")) {
                                delayPref.setEnabled(true);
                            }
                            recentsBehaviorPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                                @Override
                                public boolean onPreferenceChange(Preference preference, Object newValue) {
                                    delayPref.setEnabled(Integer.parseInt((String) newValue) == 2);
                                    return true;
                                }
                            });
                        }
                        break;
                    case "settings_notifications":
                        if (!ConfigUtils.M)
                            lockPreference(screen.findPreference("notification_experimental"));
                        break;
                    case "settings_settings":
                        if (!ConfigUtils.M)
                            lockPreference(screen.findPreference("enable_install_source"));
                        break;
                }
            } else {
                switch (preference.getKey()) {
                    case "fix_stuck_inversion":
                        getActivity().sendBroadcast(new Intent(ACTION_FIX_INVERSION));
                        break;
                    case "share_logcat":
                        ((SettingsActivity) getActivity()).requestLogsPermission(new Runnable() {
                            @Override
                            public void run() {
                                getActivity().startService(new Intent(getActivity(), LogcatService.class));
                            }
                        });
                        break;
                }
            }
            return false;
        }

        private void lockPreference(Preference pref) {
            if (pref == null) return;
            pref.setEnabled(false);
            pref.setSummary(getString(R.string.requires_android_version, "Marshmallow"));
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
                try {
                    Runtime.getRuntime().exec("chmod 664" + sharedPrefsFile.getAbsolutePath());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        @SuppressLint("CommitPrefEdits")
        private void sendUpdateBroadcast(SharedPreferences prefs, String key) {
            Intent intent = new Intent();
            switch (key) {
                case "double_tap_speed":
                    intent.setAction(ACTION_RECENTS_CHANGED);
                    intent.putExtra(EXTRA_RECENTS_DOUBLE_TAP_SPEED, prefs.getInt(key, 400));
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

        @Override
        public void onError(Exception e) {
            Log.e(TAG, "Error fetching updates", e);
        }

        @Override
        public void onFinish(UpdateUtils.UpdateData updateData) {
            Context mContext = getActivity();
            if (mContext == null) return;
            if (updateData.getNumber() > mContext.getResources().getInteger(R.integer.version) && updateData.hasArtifact())
                UpdateUtils.showNotification(updateData, mContext);
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

    private void requestLogsPermission(Runnable action) {
        if (getPackageManager().checkPermission(Manifest.permission.READ_LOGS, getPackageName()) != PackageManager.PERMISSION_GRANTED ||
                getPackageManager().checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE, getPackageName()) != PackageManager.PERMISSION_GRANTED ||
                getPackageManager().checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, getPackageName()) != PackageManager.PERMISSION_GRANTED) {
            final ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setCancelable(false);
            progressDialog.setIndeterminate(true);
            progressDialog.setMessage(getString(R.string.requesting_root));
            showDialog(R.string.permission_required, R.string.logs_permission_description, false, new Runnable() {
                @Override
                public void run() {
                    new AsyncTask<Void, Void, Boolean>() {
                        @Override
                        protected void onPreExecute() {
                            progressDialog.show();
                        }

                        @Override
                        protected Boolean doInBackground(Void... params) {
                            try {
                                java.lang.Process proc = Runtime.getRuntime().exec(new String[]{"su", "-c", "pm grant " + getPackageName() + " " + Manifest.permission.READ_LOGS});
                                int res = proc.waitFor();
                                proc.destroy();
                                if (res != 0)
                                    throw new IOException("Failed to grant READ_LOGS permision with root (exit value: " + res + ")");

                                // When we are here anyways we can request the storage permissions too
                                // All the processes are bad, but I couldn't get it to work with an OutputStream
                                java.lang.Process proc2 = Runtime.getRuntime().exec(new String[]{"su", "-c", "pm grant " + getPackageName() + " " + Manifest.permission.READ_EXTERNAL_STORAGE});
                                int res2 = proc2.waitFor();
                                proc2.destroy();
                                if (res2 != 0)
                                    throw new IOException("Failed to grant READ_EXTERNAL_STORAGE permision with root (exit value: " + res + ")");

                                java.lang.Process proc3 = Runtime.getRuntime().exec(new String[]{"su", "-c", "pm grant " + getPackageName() + " " + Manifest.permission.WRITE_EXTERNAL_STORAGE});
                                int res3 = proc3.waitFor();
                                proc3.destroy();
                                if (res3 != 0)
                                    throw new IOException("Failed to grant WRITE_EXTERNAL_STORAGE permision with root (exit value: " + res + ")");

                            } catch (IOException | InterruptedException e) {
                                Log.e(TAG, "Couldn't grant READ_LOGS permission with root", e);
                                return false;
                            }
                            return true;
                        }

                        @Override
                        protected void onPostExecute(Boolean result) {
                            progressDialog.dismiss();
                            if (result) {
                                Process.sendSignal(Process.myPid(), Process.SIGNAL_KILL);
                            } else {
                                showDialog(0, R.string.root_failed, true, null);
                            }
                        }
                    }.execute();
                }
            });
        } else {
            action.run();
        }
    }

}
