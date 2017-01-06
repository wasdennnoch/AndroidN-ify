package tk.wasdennnoch.androidn_ify.ui.misc;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import tk.wasdennnoch.androidn_ify.ui.SettingsActivity;

public class OnUpdateReceiver extends BroadcastReceiver {

    @SuppressWarnings("ConstantConditions")
    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    @Override
    public void onReceive(Context context, Intent intent) {
        if (SettingsActivity.isActivated()) {
            context.startService(new Intent(context, UpdateWarningService.class));
        }
    }
}
