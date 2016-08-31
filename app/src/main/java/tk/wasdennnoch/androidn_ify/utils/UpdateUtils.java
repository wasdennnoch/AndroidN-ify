package tk.wasdennnoch.androidn_ify.utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.ui.misc.DownloadService;

public class UpdateUtils {

    public static boolean check(Context context, UpdateListener listener) {
        if (!isEnabled(context)) return false;
        if (!isConnected(context)) return false;
        new CheckUpdateTask(context).execute(context.getString(R.string.updater_url), listener);
        return true;
    }

    public static boolean isEnabled(Context context) {
        return context.getResources().getBoolean(R.bool.enable_updater);
    }

    public static boolean isConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    public interface UpdateListener {
        void onError(Exception e);
        void onFinish(UpdateData data);
    }

    @SuppressWarnings("deprecation")
    public static void showNotification(UpdateUtils.UpdateData updateData, Context context) {
        Intent downloadIntent = new Intent(context, DownloadService.class);
        downloadIntent.putExtra("url", updateData.getArtifactUrl());
        downloadIntent.putExtra("number", updateData.getNumber());
        downloadIntent.putExtra("hasartifact", updateData.hasArtifact());
        PendingIntent intent = PendingIntent.getService(context, 0, downloadIntent, 0);

        Notification.Action downloadAction = new Notification.Action.Builder(R.drawable.arrow_down,
                context.getString(R.string.update_notification_download), intent)
                .build();

        String content = String.format(context.getString(R.string.update_notification), updateData.getNumber());

        Notification.Builder notificationBuider = new Notification.Builder(context)
                .setSmallIcon(R.drawable.ic_stat_n)
                .setContentTitle(context.getString(R.string.update_notification_title))
                .setContentText(content)
                .setColor(context.getResources().getColor(R.color.colorAccent));
        notificationBuider.setPriority(Notification.PRIORITY_HIGH).setVibrate(new long[0]);
        notificationBuider.addAction(downloadAction);

        List<String> changes = updateData.getChanges();
        if (changes.size() != 0) {
            Notification.InboxStyle style = new Notification.InboxStyle();
            style.addLine(content);
            for (String change : changes) {
                style.addLine(" - " + change);
            }
            notificationBuider.setStyle(style);
        }

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(0, notificationBuider.build());
    }

    public static class CheckUpdateTask extends AsyncTask<Object, Void, String> {

        HttpURLConnection urlConnection;
        Context mContext;
        UpdateListener mListener;
        Exception mException;

        public CheckUpdateTask(Context context) {
            mContext = context;
        }

        @Override
        protected String doInBackground(Object... params) {
            StringBuilder result = new StringBuilder();

            if (params[1] != null && params[1] instanceof UpdateListener) mListener = (UpdateListener) params[1];

            try {
                URL url = new URL((String) params[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());

                BufferedReader reader = new BufferedReader(new InputStreamReader(in));

                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
            } catch (Exception e) {
                mException = e;
            } finally {
                urlConnection.disconnect();
            }

            return result.toString();
        }

        @Override
        protected void onPostExecute(String s) {
            if (mException != null) {
                mListener.onError(mException);
            } else {
                try {
                    UpdateData updateData = UpdateData.fromJson(new JSONObject(s));
                    mListener.onFinish(updateData);
                } catch (JSONException e) {
                    mListener.onError(e);
                }
            }
        }
    }

    public static class UpdateData {
        private int number;
        private boolean hasArtifact;
        private String artifactUrl;
        private final List<String> changes;

        public UpdateData(int number, boolean hasArtifact, String artifactUrl, List<String> changes) {
            this.number = number;
            this.hasArtifact = hasArtifact;
            this.artifactUrl = artifactUrl;
            this.changes = changes;
        }

        public static UpdateData fromJson(JSONObject jsonObject) throws JSONException {
            int number = jsonObject.getInt("number");
            String url = jsonObject.getString("url");
            String artifactUrl = "";
            List<String> changes = new ArrayList<>();
            boolean hasArtifact = false;
            JSONArray artifacts = jsonObject.getJSONArray("artifacts");
            int artifactCount = artifacts.length();
            if (artifactCount == 1) {
                hasArtifact = true;
                JSONObject artifact = artifacts.getJSONObject(0);
                artifactUrl = url + "artifact/" + artifact.getString("relativePath");
            }
            JSONObject changeSet = jsonObject.getJSONObject("changeSet");
            if (changeSet != null) {
                JSONArray items = changeSet.getJSONArray("items");
                if (items != null) {
                    for (int i = 0; i < items.length(); i++) {
                        JSONObject item = items.getJSONObject(i);
                        changes.add(item.getString("msg"));
                    }
                }
            }
            return new UpdateData(number, hasArtifact, artifactUrl, changes);
        }

        public String getArtifactUrl() {
            return artifactUrl;
        }

        public boolean hasArtifact() {
            return hasArtifact;
        }

        public int getNumber() {
            return number;
        }

        public List<String> getChanges() { return changes; }
    }
}
