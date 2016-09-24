/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package tk.wasdennnoch.androidn_ify.settings.misc;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.utils.ResourceUtils;

public class SettingsDrawerAdapter extends BaseAdapter {

    private static final String TAG = "SettingsDrawerAdapter";
    private final ArrayList<Item> mItems = new ArrayList<>();
    private final Activity mActivity;
    private SettingsActivityHelper mHelper;

    public SettingsDrawerAdapter(Activity activity) {
        mActivity = activity;
    }

    void updateCategories() {
        new CategoriesUpdater().execute();
    }

    @SuppressWarnings("unchecked")
    private void updateCategoriesInternal() {
        ResourceUtils res = ResourceUtils.getInstance(mActivity);
        List<Object> categories = SettingsActivityHelper.getDashboardCategories();
        if (categories == null) return;
        mItems.clear();
        // Spacer.
        mItems.add(null);
        Item tile = new Item();
        tile.label = res.getString(R.string.home);
        tile.icon = res.getDrawable(R.drawable.home);
        mItems.add(tile);
        for (int i = 0; i < categories.size(); i++) {
            Item category = new Item();
            category.icon = null;
            Object dashboardCategory = categories.get(i);
            category.label = (CharSequence) XposedHelpers.callMethod(dashboardCategory, "getTitle", mActivity.getResources());
            mItems.add(category);
            List<Object> tiles = (List<Object>) XposedHelpers.getObjectField(dashboardCategory, "tiles");
            for (int j = 0; j < tiles.size(); j++) {
                tile = new Item();
                Object dashboardTile = tiles.get(j);
                tile.label = (CharSequence) XposedHelpers.callMethod(dashboardTile, "getTitle", mActivity.getResources());
                tile.icon = getIcon(dashboardTile);
                tile.tile = dashboardTile;
                mItems.add(tile);
            }
        }
    }

    private Drawable getIcon(Object dashboardTile) {
        int iconRes = XposedHelpers.getIntField(dashboardTile, "iconRes");
        String iconPkg = (String) XposedHelpers.getObjectField(dashboardTile, "iconPkg");
        if (!TextUtils.isEmpty(iconPkg)) {
            try {
                Drawable drawable = mActivity.getPackageManager()
                        .getResourcesForApplication(iconPkg).getDrawable(iconRes, null);
                if (!iconPkg.equals(mActivity.getPackageName()) && drawable != null) {
                    // If this drawable is coming from outside Settings, tint it to match the color.
                    TypedValue tintColorValue = new TypedValue();
                    mActivity.getResources().getValue(mActivity.getResources().getIdentifier("external_tile_icon_tint_color", "color", mActivity.getPackageName()),
                            tintColorValue, true);
                    // If tintColorValue is TYPE_ATTRIBUTE, resolve it
                    if (tintColorValue.type == TypedValue.TYPE_ATTRIBUTE) {
                        mActivity.getTheme().resolveAttribute(tintColorValue.data,
                                tintColorValue, true);
                    }
                    drawable.setTintMode(android.graphics.PorterDuff.Mode.SRC_ATOP);
                    drawable.setTint(tintColorValue.data);
                }
                return drawable;
            } catch (Throwable t) {
                XposedHook.logE(TAG, "Can't get icon for tile", t);
                return null;
            }
        } else if (iconRes > 0) {
            return mActivity.getDrawable(iconRes);
        } else {
            return null;
        }
    }

    public Object getTile(int position) {
        return mItems.get(position) != null ? mItems.get(position).tile : null;
    }

    @Override
    public int getCount() {
        return mItems.size();
    }

    @Override
    public Object getItem(int position) {
        return mItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean isEnabled(int position) {
        return mItems.get(position) != null && mItems.get(position).icon != null;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Item item = mItems.get(position);
        if (item == null) {
            if (convertView == null || convertView.getId() != R.id.spacer) {
                convertView = inflate(mActivity, R.layout.drawer_spacer,
                        parent, false);
            }
            return convertView;
        }
        if (convertView != null && convertView.getId() == R.id.spacer) {
            convertView = null;
        }
        boolean isTile = item.icon != null;
        if (convertView == null || (isTile != (convertView.getId() == R.id.tile_item))) {
            convertView = inflate(mActivity, isTile ? R.layout.drawer_item
                            : R.layout.drawer_category,
                    parent, false);
        }
        if (isTile) {
            ((ImageView) convertView.findViewById(android.R.id.icon)).setImageDrawable(item.icon);
        }
        ((TextView) convertView.findViewById(android.R.id.title)).setText(item.label);
        return convertView;
    }

    private View inflate(Context context, int resource, ViewGroup root, boolean attachToRoot) {
        return LayoutInflater.from(context).inflate(ResourceUtils.getInstance(context).getLayout(resource), root, attachToRoot);
    }

    public void setSettingsActivityHelper(SettingsActivityHelper settingsActivityHelper) {
        mHelper = settingsActivityHelper;
    }

    private static class Item {
        public Drawable icon;
        public CharSequence label;
        public Object tile;
    }

    private class CategoriesUpdater extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            updateCategoriesInternal();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            notifyDataSetChanged();
            if (mHelper != null) mHelper.updateDrawerLock();
        }
    }
}