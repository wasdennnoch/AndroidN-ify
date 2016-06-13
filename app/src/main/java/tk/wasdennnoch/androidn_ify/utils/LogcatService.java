package tk.wasdennnoch.androidn_ify.utils;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import tk.wasdennnoch.androidn_ify.R;

public class LogcatService extends IntentService {

    private static final String TAG = "LogcatService";

    private static final String ACTION_STOP = "tk.wasdennnoch.androidn_ify.action.logcat.STOP";
    private static final int NOTIF_ONGOING_ID = 55;
    private static final int NOTIF_END_ID = 56;
    private static final int UPDATE_INTERVAL = 1000;

    private String mNotifText;
    private long mTotalLines = 0;
    private boolean mCancelled = false;

    private NotificationManager mNotif;
    private NotificationCompat.Builder mNotifBuilder;

    public LogcatService() {
        super(TAG);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    protected void onHandleIntent(Intent intent) {

        mTotalLines = 0;
        mNotifText = getString(R.string.logcat_notif_ongoing_lines);

        PendingIntent stopPendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_STOP).setPackage(getPackageName()), PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Action stopAction = new NotificationCompat.Action.Builder(R.drawable.ic_stop, getString(R.string.action_stop), stopPendingIntent).build();

        //noinspection deprecation
        mNotifBuilder = new NotificationCompat.Builder(this)
                .setContentTitle(getString(R.string.logcat_notif_ongoing_title))
                .setContentText(getString(R.string.logcat_notif_preparing))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setColor(getResources().getColor(R.color.colorAccent))
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_DEFAULT)
                .addAction(stopAction);
        startForeground(NOTIF_ONGOING_ID, mNotifBuilder.build());

        final Runnable updateRunnable = new Runnable() {
            @Override
            public void run() {
                mNotifBuilder.setContentText(String.format(mNotifText, mTotalLines));
                mNotif.notify(NOTIF_ONGOING_ID, mNotifBuilder.build());
            }
        };
        BroadcastReceiver actionReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action.equals(ACTION_STOP)) {
                    mCancelled = true;
                }
            }
        };
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate(updateRunnable, UPDATE_INTERVAL, UPDATE_INTERVAL, TimeUnit.MILLISECONDS);
        mNotif = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        registerReceiver(actionReceiver, new IntentFilter(ACTION_STOP));

        List<String> commands = new ArrayList<>();
        commands.add("logcat");
        commands.add("-v");
        commands.add("time");

        //noinspection ConstantConditions
        File file = new File(getExternalFilesDir(null).getPath() + "/logcat.txt");
        if (file.exists()) {
            file.delete();
        }

        Process process = null;
        BufferedReader reader = null;
        OutputStreamWriter output = null;
        try {
            file.createNewFile();
            output = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");

            process = Runtime.getRuntime().exec(commands.toArray(new String[commands.size()]));
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()), 1024);

            String line;
            while ((line = reader.readLine()) != null) {
                if (mCancelled) break;

                if (line.length() == 0) continue;

                output.write(line);
                output.write("\n");
                mTotalLines++;
            }
        } catch (IOException e) {
            Log.e(TAG, "Error fetching logcat", e);
        } finally {
            try {
                if (process != null)
                    process.destroy();
                if (reader != null)
                    reader.close();
                if (output != null) {
                    output.flush();
                    output.close();
                }
            } catch (IOException | NullPointerException e) {
                Log.e(TAG, "Error closing logcat streams", e);
            }
        }

        executor.shutdown();
        unregisterReceiver(actionReceiver);
        stopForeground(true);

        Intent shareIntent = new Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file))
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        shareIntent = Intent.createChooser(shareIntent, getString(R.string.action_share));
        PendingIntent sharePendingIntent = PendingIntent.getActivity(this, 0, shareIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Action shareAction = new NotificationCompat.Action.Builder(R.drawable.ic_share, getString(R.string.action_share), sharePendingIntent).build();

        //noinspection deprecation
        NotificationCompat.Builder endNotif = new NotificationCompat.Builder(this)
                .setContentTitle(getString(R.string.logcat_notif_finished_title))
                .setContentText(getString(R.string.logcat_notif_finished_lines, mTotalLines))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setColor(getResources().getColor(R.color.colorAccent))
                .setPriority(Notification.PRIORITY_DEFAULT)
                .addAction(shareAction);
        mNotif.notify(NOTIF_END_ID, endNotif.build());

    }

}
