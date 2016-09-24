package tk.wasdennnoch.androidn_ify.settings.misc;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.Toolbar;

import com.android.internal.view.menu.ActionMenuItem;

import java.util.List;

import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.utils.ResourceUtils;

public class SettingsActivityHelper implements View.OnClickListener {

    private static final String TAG = "SettingsActivityHelper";
    private static final String CLASS_UTILS = "com.android.settings.Utils";

    private static List<Object> sDashboardCategories;
    private static Class<?> sClassUtils;
    private ActionMenuItem mNavItem;
    private boolean mIsShowingDashboard;

    private Activity mActivity;
    private DrawerLayout mDrawerLayout;
    private SettingsDrawerAdapter mDrawerAdapter;

    private int mFailCount = 0;
    private Runnable mUpdateCategories = new Runnable() {
        @Override
        public void run() {
            new CategoriesUpdater().execute();
        }
    };

    @SuppressWarnings("ConstantConditions")
    public SettingsActivityHelper(Activity activity) {
        mActivity = activity;
        if (sClassUtils == null) {
            sClassUtils = XposedHelpers.findClass(CLASS_UTILS, mActivity.getClassLoader());
        }
        LayoutInflater inflater = LayoutInflater.from(activity);
        inflater.setFactory2(new LayoutInflater.Factory2() {
            @Override
            public View onCreateView(String name, Context context, AttributeSet attrs) {
                if (name.equals(DrawerLayout.class.getCanonicalName())) {
                    return new DrawerLayout(context, attrs);
                } else return null;
            }

            @Override
            public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
                return onCreateView(name, context, attrs);
            }
        });
        mIsShowingDashboard = XposedHelpers.getBooleanField(activity, "mIsShowingDashboard");
        boolean mDisplayHomeAsUpEnabled = XposedHelpers.getBooleanField(activity, "mDisplayHomeAsUpEnabled");
        ViewGroup content = (ViewGroup) XposedHelpers.getObjectField(activity, "mContent");
        mDrawerLayout = (DrawerLayout) inflater.inflate(ResourceUtils.getInstance(activity).getLayout(R.layout.settings_with_drawer), content, false);
        Toolbar toolbar = (Toolbar) mDrawerLayout.findViewById(R.id.action_bar);
        activity.setActionBar(toolbar);
        ActionBar actionBar = activity.getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(mDisplayHomeAsUpEnabled);
        actionBar.setHomeButtonEnabled(mDisplayHomeAsUpEnabled);
        toolbar.setNavigationOnClickListener(this);
        mNavItem = new ActionMenuItem(activity,
                0, android.R.id.home, 0, 0, "");
        if (!mIsShowingDashboard) {
            actionBar.setHomeAsUpIndicator(ResourceUtils.getInstance(activity).getDrawable(R.drawable.ic_menu));
            content = (ViewGroup) content.getParent().getParent();
        }
        ViewGroup parent = (ViewGroup) content.getParent();
        int index = parent.indexOfChild(content);
        parent.removeView(content);
        ((FrameLayout) mDrawerLayout.findViewById(R.id.content_frame)).addView(content);
        parent.addView(mDrawerLayout, index);
        mDrawerAdapter = new SettingsDrawerAdapter(activity);
        mDrawerAdapter.setSettingsActivityHelper(this);
        ListView listView = (ListView) activity.findViewById(R.id.left_drawer);
        listView.setAdapter(mDrawerAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(android.widget.AdapterView<?> parent, View view, int position,
                                    long id) {
                onTileClicked(mDrawerAdapter.getTile(position));
            }
        });
        new CategoriesUpdater().execute();
    }

    protected void onCategoriesChanged() {
        if (sDashboardCategories != null) {
            updateDrawer();
        } else if (mFailCount < 10) {
            mFailCount++;
            mDrawerLayout.postDelayed(mUpdateCategories, 1000);
        }
    }

    public void updateDrawer() {
        if (mDrawerLayout == null) {
            return;
        }
        mDrawerAdapter.updateCategories();
    }

    public void updateDrawerLock() {
        if (mDrawerAdapter.getCount() != 0) {
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        } else {
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        }
    }

    public void openDrawer() {
        if (mDrawerLayout != null) {
            mDrawerLayout.openDrawer(GravityCompat.START);
        }
    }

    public void closeDrawer() {
        if (mDrawerLayout != null) {
            mDrawerLayout.closeDrawers();
        }
    }

    public boolean openTile(Object tile) {
        closeDrawer();
        if (tile == null) {
            mActivity.startActivity(new Intent(Settings.ACTION_SETTINGS).addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TASK));
            return true;
        }
        if (openFragment(tile)) {
            return false;
        }
        try {
            // Show menu on top level items.
            Intent intent = (Intent) XposedHelpers.getObjectField(tile, "intent");
            //tile.intent.putExtra(EXTRA_SHOW_MENU, true);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            mActivity.startActivity(intent);
            //}
        } catch (ActivityNotFoundException e) {
            Intent intent = (Intent) XposedHelpers.getObjectField(tile, "intent");
            XposedHook.logE(TAG, "Couldn't find tile " + intent, e);
        }
        return true;
    }

    private boolean openFragment(Object tile) {
        String fragment = (String) XposedHelpers.getObjectField(tile, "fragment");
        if (fragment != null) {
            Bundle fragmentArguments = (Bundle) XposedHelpers.getObjectField(tile, "fragmentArguments");
            int titleRes = XposedHelpers.getIntField(tile, "titleRes");
            CharSequence title = (CharSequence) XposedHelpers.callMethod(tile, "getTitle", mActivity.getResources());
            XposedHelpers.callStaticMethod(sClassUtils, "startWithFragment", new Class[] {Context.class, String.class, Bundle.class, Fragment.class, int.class, int.class, CharSequence.class},
                    mActivity, fragment, fragmentArguments, null, 0, titleRes, title);
            return true;
        }
        return false;
    }

    protected void onTileClicked(Object tile) {
        if (openTile(tile)) {
            mActivity.finish();
        }
    }

    public static List<Object> getDashboardCategories() {
        return sDashboardCategories;
    }

    @Override
    public void onClick(View v) {
        XposedHook.logI(TAG, "onClick");
        if (!mIsShowingDashboard && mDrawerLayout != null
                && mDrawerAdapter.getCount() != 0) {
            openDrawer();
        } else {
            mActivity.onOptionsItemSelected(mNavItem);
        }
    }

    private class CategoriesUpdater extends AsyncTask<Void, Void, List<Object>> {
        @SuppressWarnings("unchecked")
        @Override
        protected List<Object> doInBackground(Void... params) {
            try {
                if (sDashboardCategories != null) {
                    return sDashboardCategories;
                }
                return (List<Object>) XposedHelpers.callMethod(mActivity, "getDashboardCategories", false);
            } catch (Throwable t) {
                XposedHook.logE(TAG, "Can't update categories", t);
                return null;
            }
        }

        @Override
        protected void onPostExecute(List<Object> dashboardCategories) {
            sDashboardCategories = dashboardCategories;
            onCategoriesChanged();
        }
    }
}
