package tk.wasdennnoch.androidn_ify.ui;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.SearchView;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.utils.CachedResolveInfo;

public class BlacklistAppActivity extends Activity implements SearchView.OnQueryTextListener, AppsAdapter.AppsAdapterListener, LoadAppInfoTask.OnFinishListener {

    private String mSearchQuery;
    private RecyclerView mRecyclerView;
    private List<CachedResolveInfo> mApps;
    private ProgressBar mProgress;
    private AppsAdapter mAdapter;
    private boolean mIsLoading = true;
    private List<String> mBlacklistedApps;
    private SharedPreferences mSharedPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blacklist_app);

        mIsLoading = true;
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        loadBlacklistedApps();
        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        mSearchQuery = "";

        setupApps();

        mAdapter = new AppsAdapter(mApps, mBlacklistedApps, getPackageManager(), this);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);

        mProgress = (ProgressBar) findViewById(R.id.loading);
    }

    public void loadBlacklistedApps() {
        List<String> apps = new ArrayList<>();
        try {
            String jsonString = mSharedPrefs.getString("notification_blacklist_apps", "[]");
            JSONArray jsonArray = new JSONArray(jsonString);
            int appCount = jsonArray.length();
            for (int i = 0; i < appCount; i++) {
                String app = jsonArray.getString(i);
                apps.add(app);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        mBlacklistedApps = apps;
    }

    private void setupApps() {
        mApps = new ArrayList<>();
        new LoadAppInfoTask().execute(mApps, this);
    }

    @Override
    public void onFinish(List<CachedResolveInfo> apps) {
        mIsLoading = false;
        mApps = apps;
        mAdapter.setApps(mApps);
        mAdapter.notifyDataSetChanged();
        mProgress.setVisibility(View.GONE);
        mRecyclerView.setVisibility(View.VISIBLE);
        onSearch(mSearchQuery);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_blacklist_app, menu);
        MenuItem searchMenu = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchMenu.getActionView();
        searchView.setOnQueryTextListener(this);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        mSearchQuery = newText;
        return onSearch(newText);
    }

    private boolean onSearch(String newText) {
        if (mIsLoading) return false;
        if (newText.length() < 1) {
            if (mAdapter.getApps().size() != mApps.size()) {
                mAdapter.setApps(mApps);
                mAdapter.notifyDataSetChanged();
                return true;
            }
            return false;
        }
        mAdapter.setApps(search(mApps, newText));
        mAdapter.notifyDataSetChanged();
        return false;
    }

    public static List<CachedResolveInfo> search(List<CachedResolveInfo> apps, String query) {
        query = query.toLowerCase();
        List<CachedResolveInfo> searchApps = new ArrayList<>();
        for (CachedResolveInfo app : apps) {
            if (app.search(query)) searchApps.add(app);
        }
        return searchApps;
    }

    @Override
    public void onSelect(int adapterPosition, AppsAdapter.AppViewHolder itemView) {
        CachedResolveInfo app = mAdapter.getApps().get(adapterPosition);
        if (mBlacklistedApps.contains(app.getPackageName())) {
            itemView.mCheckBox.setChecked(false);
            mBlacklistedApps.remove(app.getPackageName());
        } else {
            itemView.mCheckBox.setChecked(true);
            mBlacklistedApps.add(app.getPackageName());
        }
        mAdapter.setBlacklistedApps(mBlacklistedApps);
        SharedPreferences.Editor editor = mSharedPrefs.edit();
        editor.putString("notification_blacklist_apps", new JSONArray(mBlacklistedApps).toString());
        editor.apply();
    }
}
