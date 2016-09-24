package tk.wasdennnoch.androidn_ify.ui;

import android.os.Bundle;
import android.os.PersistableBundle;

import tk.wasdennnoch.androidn_ify.BuildConfig;

public class SpoofAPIActivity extends AppListActivity {
    @SuppressWarnings("PointlessBooleanExpression")
    @Override
    public void onCreate(Bundle savedInstanceState, PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
        finish();
    }

    @Override
    protected String getKey() {
        return "notification_spoof_api_version";
    }
}
