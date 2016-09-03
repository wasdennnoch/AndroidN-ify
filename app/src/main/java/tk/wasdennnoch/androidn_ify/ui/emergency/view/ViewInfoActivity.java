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
package tk.wasdennnoch.androidn_ify.ui.emergency.view;

import android.app.ActivityManager;
import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.TabLayout;
import android.text.TextUtils;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ViewFlipper;
import com.android.internal.logging.MetricsLogger;
import java.util.ArrayList;

import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.ui.emergency.EmergencyTabActivity;
import tk.wasdennnoch.androidn_ify.ui.emergency.PreferenceKeys;
import tk.wasdennnoch.androidn_ify.ui.emergency.edit.EditInfoActivity;

/**
 * Activity for viewing emergency information.
 */
public class ViewInfoActivity extends EmergencyTabActivity {
    private TextView mPersonalCardLargeItem;
    private SharedPreferences mSharedPreferences;
    private LinearLayout mPersonalCard;
    private ViewFlipper mViewFlipper;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.emergency_view_activity_layout);
        setTaskDescription(new ActivityManager.TaskDescription(getString(R.string.emergency_info)));
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mPersonalCard = (LinearLayout) findViewById(R.id.name_and_dob_linear_layout);
        mPersonalCardLargeItem = (TextView) findViewById(R.id.personal_card_large);
        mViewFlipper = (ViewFlipper) findViewById(R.id.view_flipper);
    }
    @Override
    public void onResume() {
        super.onResume();
        loadName();
        // Update the tabs: new info might have been added/deleted from the edit screen that
        // could lead to adding/removing a fragment
        setupTabs();
        maybeHideTabs();
    }
    private void loadName() {
        String name = mSharedPreferences.getString(PreferenceKeys.KEY_NAME, "");
        if (TextUtils.isEmpty(name)) {
            mPersonalCard.setVisibility(View.GONE);
        } else {
            mPersonalCard.setVisibility(View.VISIBLE);
            mPersonalCardLargeItem.setText(name);
        }
    }
    private void maybeHideTabs() {
        // Show a TextView with "No information provided" if there are no fragments.
        if (getNumberFragments() == 0) {
            mViewFlipper.setDisplayedChild(
                    mViewFlipper.indexOfChild(findViewById(R.id.no_info_text_view)));
        } else {
            mViewFlipper.setDisplayedChild(mViewFlipper.indexOfChild(findViewById(R.id.tabs)));
        }
        TabLayout tabLayout = getTabLayout();
        if (getNumberFragments() <= 1) {
            tabLayout.setVisibility(View.GONE);
        } else {
            tabLayout.setVisibility(View.VISIBLE);
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.view_info_menu, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_edit:
                Intent intent = new Intent(this, EditInfoActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    @Override
    protected ArrayList<Pair<String, Fragment>> setUpFragments() {
        // Return only the fragments that have at least one piece of information set:
        ArrayList<Pair<String, Fragment>> fragments = new ArrayList<>(2);
        if (ViewEmergencyInfoFragment.hasAtLeastOnePreferenceSet(this)) {
            fragments.add(Pair.create(getResources().getString(R.string.tab_title_info),
                    ViewEmergencyInfoFragment.newInstance()));
        }
        if (ViewEmergencyContactsFragment.hasAtLeastOneEmergencyContact(this)) {
            fragments.add(Pair.create(getResources().getString(R.string.tab_title_contacts),
                    ViewEmergencyContactsFragment.newInstance()));
        }
        return fragments;
    }
}