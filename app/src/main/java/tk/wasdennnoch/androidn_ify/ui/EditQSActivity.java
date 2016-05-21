package tk.wasdennnoch.androidn_ify.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;

import tk.wasdennnoch.androidn_ify.R;

public class EditQSActivity extends Activity {

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_qs);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        //if (savedInstanceState == null)
        //    getFragmentManager().beginTransaction().replace(R.id.fragment, new InfoFragment()).commit();
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
}