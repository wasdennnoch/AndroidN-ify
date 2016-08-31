package tk.wasdennnoch.androidn_ify.ui.misc;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.XposedHook;

public class UpdateWarningService extends Service {

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        Notification.Builder builder = new Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_stat_n)
                .setContentTitle(getString(R.string.update_warning))
                .setContentText(getString(R.string.update_warning_details))
                .setStyle(new Notification.BigTextStyle().bigText(getString(R.string.update_warning_details)))
                .setPriority(Notification.PRIORITY_MAX)
                .setColor(getResources().getColor(R.color.colorAccent));
        startForeground(10, builder.build());
        sendBroadcast(new Intent(XposedHook.ACTION_MARK_UNSTABLE).setPackage(XposedHook.PACKAGE_SYSTEMUI));
    }
}
