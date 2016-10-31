/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.egg.neko;

import android.support.annotation.Nullable;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.os.Bundle;
import android.view.WindowManager;

import com.android.internal.logging.MetricsLogger;

import tk.wasdennnoch.androidn_ify.systemui.qs.tiles.*;
import tk.wasdennnoch.androidn_ify.systemui.qs.tiles.NekoTile;

public class NekoLockedActivity extends Activity implements OnDismissListener {

    private NekoDialog mDialog;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PrefState prefs = new PrefState(this);
        if (prefs.getFoodState() != 0) {
            prefs.setFoodState(0);
            NekoService.cancelJob(this);
            NekoTile.sendUpdate(new Food(0), this);
            finish();
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

            mDialog = new NekoDialog(this);
            mDialog.setOnDismissListener(this);
            mDialog.show();
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        finish();
    }
}
