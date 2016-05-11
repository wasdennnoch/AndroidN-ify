package tk.wasdennnoch.androidn_ify.utils;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import tk.wasdennnoch.androidn_ify.R;

public class DownloadService extends IntentService {

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    @SuppressWarnings("unused")
    public DownloadService(String name) {
        super(name);
    }

    @SuppressWarnings("unused")
    public DownloadService() {
        super("DownloadService");
    }

    @SuppressWarnings({"deprecation", "ConstantConditions"})
    @Override
    protected void onHandleIntent(Intent intent) {
        String url = intent.getStringExtra("url");
        int number = intent.getIntExtra("number", 0);
        boolean hasArtifact = intent.getBooleanExtra("hasartifact", false);

        UpdateUtils.UpdateData updateData = new UpdateUtils.UpdateData(number, hasArtifact, url);

        Notification.Builder mNotificationBuider = new Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_launcher_big)
                .setContentTitle(getString(R.string.update_notification_downloading))
                .setContentText(String.format(getString(R.string.update_notification_downloading_details), updateData.getNumber()))
                .setColor(getResources().getColor(R.color.colorAccent))
                .setProgress(0, 0, true);

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(0);

        startForeground(1, mNotificationBuider.build());

        if (download(url)) {
            Log.d("update", getExternalFilesDir(null).getPath() + "/update.apk");

            Intent installIntent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
            installIntent.setData(Uri.parse("file:" + getExternalFilesDir(null).getPath() + "/update.apk"));
            installIntent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true);
            installIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            startActivity(installIntent);
        } else {
            Notification.Builder builder = new Notification.Builder(this)
                    .setSmallIcon(R.drawable.ic_launcher_big)
                    .setContentTitle(getString(R.string.update_notification_download_fail))
                    .setContentText(String.format(getString(R.string.update_notification_download_fail_details), updateData.getNumber()))
                    .setColor(getResources().getColor(R.color.colorAccent));

            mNotificationManager.notify(2, builder.build());
        }

        stopForeground(true);
    }

    @SuppressWarnings({"ConstantConditions", "ResultOfMethodCallIgnored"})
    private boolean download(String urlString) {
        Context mContext = this;
        InputStream input = null;
        OutputStream output = null;
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.connect();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return false;
            }

            int fileLength = connection.getContentLength();

            String path = mContext.getExternalFilesDir(null).getPath() + "/update.apk";
            File file = (new File(path));
            if(file.exists()) {
                file.delete();
            }
            file.createNewFile();

            // download the file
            input = connection.getInputStream();
            output = new FileOutputStream(path);

            byte data[] = new byte[4096];
            int count;

            Log.d("dl", "downloading " + fileLength + " bytes to " + path);

            while ((count = input.read(data)) != -1) {
                output.write(data, 0, count);
            }

            Log.d("dl", "download complete");
        } catch (Exception ignore) {
            return false;
        } finally {
            try {
                if (output != null)
                    output.close();
                if (input != null)
                    input.close();
            } catch (IOException ignored) {
            }

            if (connection != null)
                connection.disconnect();
        }
        return true;
    }
}
