/*
 * Copyright (C) 2016 The Android Open Source Project
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
package tk.wasdennnoch.androidn_ify.ui.emergency;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.res.Configuration;
import android.support.annotation.LayoutRes;
import android.support.design.widget.TabLayout;
import android.support.design.widget.TabLayout.TabLayoutOnPageChangeListener;
import android.support.design.widget.TabLayout.ViewPagerOnTabSelectedListener;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Pair;
import android.view.MenuItem;
import android.widget.Toolbar;
import com.android.internal.annotations.VisibleForTesting;
import java.util.ArrayList;

import tk.wasdennnoch.androidn_ify.R;

/**
 * An activity uses a tab layout to separate personal and medical information
 * from emergency contacts.
 */
public abstract class EmergencyTabActivity extends Activity {
    private ViewPagerAdapter mTabsAdapter;
    private TabLayout mTabLayout;
    private ArrayList<Pair<String, Fragment>> mFragments;
    @Override
    protected void onResume() {
        super.onResume();
        int display_mode = getResources().getConfiguration().orientation;
        if (display_mode == Configuration.ORIENTATION_PORTRAIT) {
            mTabLayout.setTabMode(TabLayout.MODE_FIXED);
            mTabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
        }
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button.
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    /** Returns the index of the currently selected tab. */
    @VisibleForTesting
    public int getSelectedTabPosition() {
        return mTabLayout.getSelectedTabPosition();
    }
    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        super.setContentView(layoutResID);
        setupTabs();
        Toolbar toolbar = (Toolbar) findViewById(R.id.action_bar);
        setActionBar(toolbar);
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }
    /** Selects the tab at index {@code selectedTabIndex}. */
    public void selectTab(int selectedTabIndex) {
        if (mTabLayout != null && selectedTabIndex >= 0 &&
                selectedTabIndex < mTabLayout.getTabCount()) {
            mTabLayout.getTabAt(selectedTabIndex).select();
        }
    }
    protected void setupTabs() {
        mFragments = setUpFragments();
        mTabLayout = (TabLayout) findViewById(R.id.sliding_tabs);
        if (mTabsAdapter == null) {
            // The viewpager that will host the section contents.
            ViewPager viewPager = (ViewPager) findViewById(R.id.view_pager);
            mTabsAdapter = new ViewPagerAdapter(getFragmentManager());
            viewPager.setAdapter(mTabsAdapter);
            mTabLayout.setTabsFromPagerAdapter(mTabsAdapter);
            // Set a listener via setOnTabSelectedListener(OnTabSelectedListener) to be notified
            // when any tab's selection state has been changed.
            mTabLayout.setOnTabSelectedListener(
                    new TabLayout.ViewPagerOnTabSelectedListener(viewPager));
            // Use a TabLayout.TabLayoutOnPageChangeListener to forward the scroll and selection
            // changes to this layout
            viewPager.addOnPageChangeListener(new TabLayoutOnPageChangeListener(mTabLayout));
        } else {
            mTabsAdapter.notifyDataSetChanged();
            mTabLayout.setTabsFromPagerAdapter(mTabsAdapter);
        }
    }
    public TabLayout getTabLayout() {
        return mTabLayout;
    }
    /** Return the fragments. */
    public ArrayList<Pair<String, Fragment>> getFragments() {
        return mFragments;
    }
    /** Return number of fragments to show in the tabs. */
    public int getNumberFragments() {
        return mFragments.size();
    }
    /** Returns the fragments to show in the tabs. */
    protected abstract ArrayList<Pair<String, Fragment>> setUpFragments();
    /** The adapter used to handle the two fragments. */
    private class ViewPagerAdapter extends FragmentStatePagerAdapter {
        public ViewPagerAdapter(FragmentManager fm) {
            super(fm);
        }
        @Override
        public Fragment getItem(int position) {
            return mFragments.get(position).second;
        }
        @Override
        public int getCount() {
            return mFragments.size();
        }
        @Override
        public CharSequence getPageTitle(int position) {
            return mFragments.get(position).first;
        }
        @Override
        public int getItemPosition(Object object) {
            // The default implementation assumes that items will never change position and always
            // returns POSITION_UNCHANGED. This is how you can specify that the positions can change
            return FragmentStatePagerAdapter.POSITION_NONE;
        }
    }
}