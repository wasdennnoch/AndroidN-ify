package com.android.egg.neko;

import android.app.Activity;
import android.os.Bundle;

public class FoodSelectionActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new NekoDialog(this).show();
    }
}
