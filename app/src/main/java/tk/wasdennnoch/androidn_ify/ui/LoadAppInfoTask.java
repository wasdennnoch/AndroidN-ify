package tk.wasdennnoch.androidn_ify.ui;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.AsyncTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import tk.wasdennnoch.androidn_ify.utils.CachedResolveInfo;

public class LoadAppInfoTask extends AsyncTask<Object, Void, List<CachedResolveInfo>> {

    private OnFinishListener mListener;

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected List<CachedResolveInfo> doInBackground(Object... args) {
        mListener = (OnFinishListener) args[1];
        Context context = (Context) args[1];
        PackageManager packageManager = context.getPackageManager();
        List<CachedResolveInfo> mCachedAppsInfo = new ArrayList<>();
        List<ResolveInfo> mAppsInfo;
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        mAppsInfo = packageManager.queryIntentActivities(mainIntent, 0);
        Collections.sort(mAppsInfo, new ResolveInfo.DisplayNameComparator(packageManager));
        for (ResolveInfo app : mAppsInfo) {
            CachedResolveInfo cachedResolveInfo = new CachedResolveInfo();
            cachedResolveInfo.setIcon(app.loadIcon(packageManager));
            cachedResolveInfo.setLabel(app.loadLabel(packageManager));
            cachedResolveInfo.setResolveInfo(app);
            mCachedAppsInfo.add(cachedResolveInfo);
        }
        return mCachedAppsInfo;
    }

    protected void onPostExecute(List<CachedResolveInfo> apps) {
        mListener.onFinish(apps);
    }

    public interface OnFinishListener {
        void onFinish(List<CachedResolveInfo> apps);
    }
}