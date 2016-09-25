package tk.wasdennnoch.androidn_ify.ui;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.UserManager;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import tk.wasdennnoch.androidn_ify.BuildConfig;
import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.utils.ConfigUtils;
import tk.wasdennnoch.androidn_ify.utils.UpdateUtils;
import tk.wasdennnoch.androidn_ify.utils.ViewUtils;

public class AboutActivity extends Activity implements UpdateUtils.UpdateListener, View.OnClickListener {

    private TextView mUpdateText;
    private boolean mExperimental;
    private boolean mShowExperimental;
    private int mHitCountdown = 7;
    private Toast mHitToast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = ConfigUtils.getPreferences(this);
        ViewUtils.applyTheme(this, prefs);

        mExperimental = ConfigUtils.isExperimental(prefs);
        mShowExperimental = ConfigUtils.showExperimental(prefs);

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
        setupIcon();

        checkForUpdates();
    }

    private void setupIcon() {
        ImageView im = (ImageView) findViewById(R.id.icon);
        Drawable N = im.getDrawable();
        im.setImageDrawable(null);
        im.setBackground(new RippleDrawable(
                ColorStateList.valueOf(0xFFFFFFFF),
                N,
                null));
        im.setOnClickListener(this);
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
            UpdateUtils.showNotification(updateData, this, mExperimental);
        } else {
            mUpdateText.setText(R.string.no_updates);
        }
    }

    @SuppressLint("CommitPrefEdits")
    @Override
    public void onClick(View v) {
        if (mHitToast != null) mHitToast.cancel();
        if (mShowExperimental) {
            mHitToast = Toast.makeText(this, R.string.show_experimental_on_already, Toast.LENGTH_SHORT);
            return;
        }
        mHitCountdown--;
        if (mHitCountdown == 0) {
            mShowExperimental = !mShowExperimental;
            ConfigUtils.getPreferences(this).edit().putBoolean("show_experimental_features", mShowExperimental).commit();
            mHitToast = Toast.makeText(this, R.string.show_experimental_on, Toast.LENGTH_SHORT);
        } else if (mHitCountdown < 5) {
            mHitToast = Toast.makeText(this, getResources().getQuantityString(R.plurals.enable_experimental_countdown, mHitCountdown, mHitCountdown), Toast.LENGTH_SHORT);
        }
        if (mHitToast != null)
            mHitToast.show();
    }
}
