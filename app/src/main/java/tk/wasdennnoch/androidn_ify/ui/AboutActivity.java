package tk.wasdennnoch.androidn_ify.ui;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.UserManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import tk.wasdennnoch.androidn_ify.BuildConfig;
import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.utils.ConfigUtils;
import tk.wasdennnoch.androidn_ify.utils.UpdateUtils;
import tk.wasdennnoch.androidn_ify.utils.ViewUtils;

public class AboutActivity extends Activity implements UpdateUtils.UpdateListener {

    private TextView mUpdateText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        ViewUtils.applyTheme(this, prefs);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String version = String.format(getString(R.string.about_version), pInfo.versionName, pInfo.versionCode);
            if (BuildConfig.AUTOMATED_BUILD)
                version += "\nThis is an automated build. Bugs are expected.";
            ((TextView) findViewById(R.id.version)).setText(version);
        } catch (PackageManager.NameNotFoundException e) {
            findViewById(R.id.version).setVisibility(View.GONE);
        }

        findViewById(R.id.github).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://github.com/wasdennnoch/AndroidN-ify")));
            }
        });
        findViewById(R.id.xda).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://forum.xda-developers.com/xposed/modules/xposed-android-n-ify-features-t3345091")));
            }
        });
        findViewById(R.id.icon).setOnClickListener(new View.OnClickListener() {
            private final long[] mHits = new long[3];

            @Override
            public void onClick(View v) {
                System.arraycopy(mHits, 1, mHits, 0, mHits.length - 1);
                mHits[mHits.length - 1] = SystemClock.uptimeMillis();
                if (mHits[0] >= (SystemClock.uptimeMillis() - 500)) {
                    if (ConfigUtils.M) {
                        UserManager um = (UserManager) getSystemService(Context.USER_SERVICE);
                        if (um.hasUserRestriction(UserManager.DISALLOW_FUN)) {
                            Log.d("androidn_ify", "Sorry, no fun for you!");
                            return;
                        }
                    }
                    startActivity(new Intent(AboutActivity.this, PlatLogoActivity.class));
                }
            }
        });

        checkForUpdates();
    }

    private void checkForUpdates() {
        if (UpdateUtils.isEnabled()) {
            mUpdateText = (TextView) findViewById(R.id.updates);
            mUpdateText.setText(R.string.checking_for_update);
            mUpdateText.setVisibility(View.VISIBLE);
            UpdateUtils.check(this, this);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onError(Exception e) {
        mUpdateText.setText(R.string.check_for_update_fail);
    }

    @Override
    public void onFinish(UpdateUtils.UpdateData updateData) {
        if (updateData.getNumber() > BuildConfig.BUILD_NUMBER && updateData.hasArtifact()) {
            mUpdateText.setText(String.format(getString(R.string.update_notification), updateData.getNumber()));
            UpdateUtils.showNotification(updateData, this);
        } else {
            mUpdateText.setText(R.string.no_updates);
        }
    }
}
