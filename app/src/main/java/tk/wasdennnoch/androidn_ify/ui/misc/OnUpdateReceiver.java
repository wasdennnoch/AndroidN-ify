package tk.wasdennnoch.androidn_ify.ui.misc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class OnUpdateReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        context.startService(new Intent(context, UpdateWarningService.class));
    }
}
