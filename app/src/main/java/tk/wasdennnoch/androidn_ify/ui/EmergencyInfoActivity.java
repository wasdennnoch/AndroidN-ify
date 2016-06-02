package tk.wasdennnoch.androidn_ify.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.KeyguardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.text.Html;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.util.Calendar;
import java.util.Date;

import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.ui.preference.SeekBarPreference;
import tk.wasdennnoch.androidn_ify.utils.ThemeUtils;
import tk.wasdennnoch.androidn_ify.utils.UpdateUtils;

public class EmergencyInfoActivity extends Activity {

    private boolean mEditable;

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mEditable = getIntent().hasExtra("editable") && getIntent().getExtras().getBoolean("editable", false);
        if (mEditable) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            ThemeUtils.applyTheme(this, prefs);
            getActionBar().setTitle(getString(R.string.edit_emergency_info_title));
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency_info);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null)
            getFragmentManager().beginTransaction().replace(R.id.fragment, new InfoFragment()).commit();
    }

    @Override
    public void onAttachedToWindow() {
        if (!mEditable) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class InfoFragment extends PreferenceFragment implements Preference.OnPreferenceClickListener, DatePickerDialog.OnDateSetListener, SharedPreferences.OnSharedPreferenceChangeListener
    {

        private EmergencyInfoActivity mActivity;

        private Preference mFullnamePref;
        private Preference mAddressPref;
        private Preference mBirthDatePref;
        private Preference mBloodTypePref;
        private Preference mAllergiesPref;
        private Preference mMedicationsPref;
        private Preference mMedicalNotesPref;
        private Preference mOrganDonorPref;

        @SuppressLint("CommitPrefEdits")
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mActivity = (EmergencyInfoActivity) getActivity();
            //noinspection deprecation
            getPreferenceManager().setSharedPreferencesMode(Context.MODE_WORLD_READABLE);
            addPreferencesFromResource(mActivity.mEditable ? R.xml.infopreferences : R.xml.viewinfoprefernces);

            mFullnamePref = getPreferenceScreen().findPreference("info_fullname");
            mAddressPref = getPreferenceScreen().findPreference("info_address");
            mBirthDatePref = getPreferenceScreen().findPreference("info_birthdate");
            mBloodTypePref = getPreferenceScreen().findPreference("info_bloodtype");
            mAllergiesPref = getPreferenceScreen().findPreference("info_allergies");
            mMedicationsPref = getPreferenceScreen().findPreference("info_medications");
            mMedicalNotesPref = getPreferenceScreen().findPreference("info_medicalnotes");
            mOrganDonorPref = getPreferenceScreen().findPreference("info_organdonor");

            if (mActivity.mEditable) {
                mBirthDatePref.setOnPreferenceClickListener(this);
            }

            SharedPreferences prefs = getPreferenceScreen().getSharedPreferences();

            mFullnamePref.setSummary(prefs.getString("info_fullname", ""));
            mAddressPref.setSummary(prefs.getString("info_address", ""));
            java.text.DateFormat format = DateFormat.getDateFormat(mActivity);
            mBirthDatePref.setSummary(format.format(getBirthDate().getTime()));
            mBloodTypePref.setSummary(prefs.getString("info_bloodtype", getString(R.string.info_unspecified)));
            mAllergiesPref.setSummary(prefs.getString("info_allergies", ""));
            mMedicationsPref.setSummary(prefs.getString("info_medications", ""));
            mMedicalNotesPref.setSummary(prefs.getString("info_medicalnotes", ""));
            mOrganDonorPref.setSummary(prefs.getString("info_organdonor", getString(R.string.info_unspecified)));

        }

        @SuppressWarnings("ConstantConditions")
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = super.onCreateView(inflater, container, savedInstanceState);
            ListView list = (ListView) view.findViewById(android.R.id.list);
            list.setDivider(null);
            return view;
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
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            switch (key) {
                case "info_birthdate":
                    break;
                default:
                    Preference preference = getPreferenceScreen().findPreference(key);
                    if (preference != null)
                        preference.setSummary(getPreferenceScreen().getSharedPreferences().getString(key, ""));
            }
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            if (!mActivity.mEditable)
                return false;
            switch (preference.getKey()) {
                case "info_birthdate":
                    showDateDialog();
                    break;
            }
            return true;
        }

        private void showDateDialog(){
            Calendar c = getBirthDate();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);
            new DatePickerDialog(mActivity, this, year, month, day).show();
        }

        private Calendar getBirthDate() {
            long millis = getPreferenceScreen().getSharedPreferences().getLong("info_birthdate", 0);
            Calendar c = Calendar.getInstance();
            if (millis != 0) {
                c.setTimeInMillis(millis);
            }
            return c;
        }

        @Override
        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
            Calendar date = Calendar.getInstance();
            date.set(year, monthOfYear, dayOfMonth);
            getPreferenceScreen().getSharedPreferences().edit().putLong("info_birthdate", date.getTimeInMillis()).apply();

            java.text.DateFormat format = DateFormat.getDateFormat(mActivity);
            mBirthDatePref.setSummary(format.format(date.getTime()));
        }
    }
}
