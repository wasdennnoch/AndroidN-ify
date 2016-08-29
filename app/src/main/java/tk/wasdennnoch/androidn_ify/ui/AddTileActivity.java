package tk.wasdennnoch.androidn_ify.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.XposedHook;

public class AddTileActivity extends Activity implements View.OnClickListener {
    public static final String ACTION_ADD_TILE = "tk.wasdennnoch.androidn_ify.action.ACTION_ADD_TILE";
    public static final String EXTRA_TILE_SPEC = "extra.TILE_SPEC";

    private int mColor = 0xff263238;
    private TextView mWarning;
    private EditText mSpec;
    //private int mColor = 0xff000000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_tile);

        mWarning = (TextView) findViewById(R.id.warning);
        mSpec = (EditText) findViewById(R.id.spec);

        findViewById(R.id.dim).setOnClickListener(this);
        findViewById(R.id.qs_up).setOnClickListener(this);
        findViewById(R.id.add).setOnClickListener(this);

        updateBackgroundColor();
    }

    private void updateBackgroundColor() {
        Intent intent = getIntent();
        if (intent.hasExtra("color")) {
            mColor = intent.getIntExtra("color", mColor);
        }

        findViewById(R.id.toolbar).setBackgroundColor(mColor);
        findViewById(R.id.background).setBackgroundColor(mColor);

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(mColor);
        window.setNavigationBarColor(0x00000000);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.add:
                addTile();
                break;
            case R.id.qs_up:
            case R.id.dim:
                close();
                break;
        }
    }

    public void close() {
        sendBroadcast(new Intent(ACTION_ADD_TILE).setPackage(XposedHook.PACKAGE_SYSTEMUI));
        finish();
    }

    private void addTile() {
        String spec = mSpec.getText().toString().trim();
        if (spec.length() > 0) {
            sendBroadcast(new Intent(ACTION_ADD_TILE).putExtra(EXTRA_TILE_SPEC, "intent(" + spec + ")").setPackage(XposedHook.PACKAGE_SYSTEMUI));
            finish();
        } else {
            mWarning.setText(R.string.spec_shouldnt_be_blank);
        }
    }
}
