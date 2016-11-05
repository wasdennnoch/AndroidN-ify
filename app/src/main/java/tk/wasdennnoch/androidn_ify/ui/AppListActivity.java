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
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.ui.misc.AppsAdapter;
import tk.wasdennnoch.androidn_ify.ui.misc.CachedResolveInfo;
import tk.wasdennnoch.androidn_ify.utils.ViewUtils;

@SuppressWarnings("WeakerAccess")
public abstract class AppListActivity extends Activity implements SearchView.OnQueryTextListener, AppsAdapter.AppsAdapterListener, LoadAppInfoTask.OnFinishListener {

    private String mSearchQuery;
    private RecyclerView mRecyclerView;
    private List<CachedResolveInfo> mApps;
    private ProgressBar mProgress;
    private AppsAdapter mAdapter;
    private boolean mIsLoading = true;
    private List<String> mEnabledApps;
    private SharedPreferences mSharedPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        ViewUtils.applyTheme(this, prefs);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_list);

        mIsLoading = true;
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        loadEnabledApps();
        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        mSearchQuery = "";

        setupApps();

        mAdapter = new AppsAdapter(mApps, mEnabledApps, getPackageManager(), this);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);

        mProgress = (ProgressBar) findViewById(R.id.loading);
    }

    public void loadEnabledApps() {
        List<String> apps = new ArrayList<>();
        try {
            String jsonString = mSharedPrefs.getString(getKey(), "[]");
            JSONArray jsonArray = new JSONArray(jsonString);
            int appCount = jsonArray.length();
            for (int i = 0; i < appCount; i++) {
                String app = jsonArray.getString(i);
                apps.add(app);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        mEnabledApps = apps;
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
        getMenuInflater().inflate(R.menu.menu_app_list, menu);
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
        if (mEnabledApps.contains(app.getPackageName())) {
            itemView.mCheckBox.setChecked(false);
            mEnabledApps.remove(app.getPackageName());
        } else {
            itemView.mCheckBox.setChecked(true);
            mEnabledApps.add(app.getPackageName());
            if (mEnabledApps.size() == mApps.size() || app.getPackageName().equals(XposedHook.PACKAGE_OWN)) {
                Toast.makeText(this, "ಠ_ಠ", Toast.LENGTH_LONG).show();
            }
        }
        mAdapter.setEnabledApps(mEnabledApps);
        SharedPreferences.Editor editor = mSharedPrefs.edit();
        editor.putString(getKey(), new JSONArray(mEnabledApps).toString());
        editor.apply();
    }

    protected abstract String getKey();
}
