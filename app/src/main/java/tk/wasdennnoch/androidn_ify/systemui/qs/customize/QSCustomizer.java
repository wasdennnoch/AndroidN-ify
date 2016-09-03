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
package tk.wasdennnoch.androidn_ify.systemui.qs.customize;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.os.Build;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toolbar;
import android.widget.Toolbar.OnMenuItemClickListener;

import java.util.ArrayList;

import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.systemui.notifications.NotificationPanelHooks;
import tk.wasdennnoch.androidn_ify.systemui.notifications.StatusBarHeaderHooks;
import tk.wasdennnoch.androidn_ify.systemui.qs.QSDetailClipper;
import tk.wasdennnoch.androidn_ify.systemui.qs.QSTileHostHooks;
import tk.wasdennnoch.androidn_ify.systemui.qs.TileAdapter;
import tk.wasdennnoch.androidn_ify.ui.AddTileActivity;
import tk.wasdennnoch.androidn_ify.utils.ResourceUtils;
import tk.wasdennnoch.androidn_ify.utils.ViewUtils;

/**
 * Allows full-screen customization of QS, through show() and hide().
 *
 * This adds itself to the status bar window, so it can appear on top of quick settings and
 * *someday* do fancy animations to get into/out of it.
 */
public class QSCustomizer extends LinearLayout implements OnMenuItemClickListener, TileAdapter.QSPanelWidthListener {

    private static final int MENU_ADD_BROADCAST_TILE = Menu.FIRST;
    private final Context mContext;
    private final Context mOwnContext;
    private final QSDetailClipper mClipper;
    private final Point mSizePoint = new Point();
    private final int mColor;

    private boolean isShown;
    private final RecyclerView mRecyclerView;
    private final TileAdapter mTileAdapter;
    private final Toolbar mToolbar;
    private boolean mCustomizing;
    private boolean mTileAdapterIsInvalid = true;

    private int mLastX = 0;
    private int mLastY = 0;
    private final int mNavigationBarSize;

    public QSCustomizer(Context context) {
        super(context, null);

        mContext = context;
        mOwnContext = ResourceUtils.createOwnContext(mContext);

        ResourceUtils res = ResourceUtils.getInstance(mContext);
        mColor = res.getColor(R.color.m_blue_grey_900);
        mNavigationBarSize = res.getDimensionPixelSize(R.dimen.navigation_bar_size);

        setGravity(Gravity.CENTER_HORIZONTAL);
        setOrientation(VERTICAL);
        setVisibility(GONE);
        setBackground(res.getDrawable(R.drawable.qs_customizer_background));
        mClipper = new QSDetailClipper(this);

        LayoutInflater.from(mOwnContext).inflate(R.layout.qs_customize_panel_content, this);

        LayoutParams recyclerViewLp = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        RecyclerView recyclerView = new RecyclerView(context);
        recyclerView.setId(android.R.id.list);
        recyclerView.setLayoutParams(recyclerViewLp);
        recyclerView.setVerticalScrollBarEnabled(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            recyclerView.setScrollIndicators(SCROLL_INDICATOR_TOP);
        }
        ((LinearLayout) findViewById(R.id.list_containter)).addView(recyclerView);

        mToolbar = (Toolbar) findViewById(R.id.action_bar);
        TypedValue value = new TypedValue();
        mContext.getTheme().resolveAttribute(android.R.attr.homeAsUpIndicator, value, true);
        mToolbar.setNavigationIcon(
                getResources().getDrawable(value.resourceId, mContext.getTheme()));
        mToolbar.setNavigationOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                hide((int) v.getX() + v.getWidth() / 2, (int) v.getY() + v.getHeight() / 2);
            }
        });
        mToolbar.setOnMenuItemClickListener(this);
        mToolbar.getMenu().add(Menu.NONE, MENU_ADD_BROADCAST_TILE, 0,
                res.getString(R.string.add_custom_tile));
        mToolbar.setTitle(R.string.qs_edit);

        mRecyclerView = (RecyclerView) findViewById(android.R.id.list);
        setBottomMargin(mNavigationBarSize);
        mTileAdapter = new TileAdapter(mContext);
        mTileAdapter.setWidthListener(this);
        mRecyclerView.setAdapter(mTileAdapter);
        mTileAdapter.getItemTouchHelper().attachToRecyclerView(mRecyclerView);
        GridLayoutManager layout = new GridLayoutManager(getContext(), 3);
        layout.setSpanSizeLookup(mTileAdapter.getSizeLookup());
        mRecyclerView.setLayoutManager(layout);
        mRecyclerView.addItemDecoration(mTileAdapter.getItemDecoration());
        DefaultItemAnimator animator = new DefaultItemAnimator();
        animator.setMoveDuration(TileAdapter.MOVE_DURATION);
        mRecyclerView.setItemAnimator(animator);
    }

    public void invalidateTileAdapter() {
        mTileAdapterIsInvalid = true;
    }

    public void show(ArrayList<Object> records, boolean animated) {
        if (isShown) return;
        initShow(records);
        if (animated) {
            setAlpha(0);
            animate().alpha(1)
                    .withEndAction(mShowAnimationListener)
                    .start();
        } else {
            setAlpha(1);
        }
    }

    public void show(ArrayList<Object> records, int x, int y) {
        if (isShown) return;
        initShow(records);
        setAlpha(1);
        mClipper.animateCircularClip(x, y, true, mExpandAnimationListener);
        mLastX = x;
        mLastY = y;
    }

    private void initShow(ArrayList<Object> records) {
        if (mTileAdapterIsInvalid) {
            mTileAdapterIsInvalid = false;
            mTileAdapter.reInit(records, mContext);
        }

        if (!isShown) {
            isShown = true;
            setVisibility(VISIBLE);
            NotificationPanelHooks.addBarStateCallback(mBarStateCallback);
        }
    }

    public void hide(boolean animated) {
        if (!isShown) return;
        saveAndHide();
        if (animated) {
            animate().alpha(1)
                    .withEndAction(mHideAnimationListener)
                    .start();
        }
    }

    public void hideCircular() {
        hide(mLastX, mLastY);
    }

    private void hide(int x, int y) {
        if (isShown) {
            saveAndHide();
            mClipper.animateCircularClip(x, y, false, mCollapseAnimationListener);
        }
    }

    private void saveAndHide() {
        isShown = false;
        mToolbar.dismissPopupMenus();
        setCustomizing(false);
        save();
        NotificationPanelHooks.removeBarStateCallback(mBarStateCallback);
    }

    private void setCustomizing(boolean customizing) {
        mCustomizing = customizing;
    }

    public boolean isCustomizing() {
        return mCustomizing;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_ADD_BROADCAST_TILE:
                showAddBroadcastTile();
                break;
        }
        return false;
    }

    private void showAddBroadcastTile() {
        hide(true);
        Object qsTileHost = XposedHelpers.getObjectField(StatusBarHeaderHooks.mQsPanel, "mHost");
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setClassName("tk.wasdennnoch.androidn_ify", AddTileActivity.class.getName());
        intent.putExtra("color", mColor);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK
                | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        try {
            XposedHelpers.callMethod(qsTileHost, "startActivityDismissingKeyguard", intent);
        } catch (Throwable t) {
            try {
                XposedHelpers.callMethod(qsTileHost, "startSettingsActivity", intent);
            } catch (Throwable t2) {
                XposedHook.logE("QSCustomizer", "Error starting settings activity", null);
            }
        }
    }

    private void save() {
        mTileAdapter.saveChanges();
        QSTileHostHooks.recreateTiles();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        getDisplay().getRealSize(mSizePoint);
        setMeasuredDimension(widthMeasureSpec, MeasureSpec.makeMeasureSpec(mSizePoint.y, MeasureSpec.EXACTLY));
    }

    private final NotificationPanelHooks.BarStateCallback mBarStateCallback = new NotificationPanelHooks.BarStateCallback() {
        @Override
        public void onStateChanged() {
            if (NotificationPanelHooks.getStatusBarState() == NotificationPanelHooks.STATE_KEYGUARD) {
                hide(false);
            }
        }
    };

    private final AnimatorListener mExpandAnimationListener = new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
            setCustomizing(true);
        }
    };

    private final Runnable mShowAnimationListener = new Runnable() {
        @Override
        public void run() {
            setCustomizing(true);
        }
    };

    private final Runnable mHideAnimationListener = new Runnable() {
        @Override
        public void run() {
            if (!isShown)
                setVisibility(View.GONE);
            mRecyclerView.setAdapter(mTileAdapter);
        }
    };

    private final AnimatorListener mCollapseAnimationListener = new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
            if (!isShown)
                setVisibility(View.GONE);
            mRecyclerView.setAdapter(mTileAdapter);
        }

        @Override
        public void onAnimationCancel(Animator animation) {
            if (!isShown)
                setVisibility(View.GONE);
        }
    };

    @Override
    public void onWidthChanged(int width) {
        ViewUtils.setWidth(mRecyclerView, width);
        getDisplay().getRealSize(mSizePoint);
        boolean portrait = mSizePoint.y >= mSizePoint.x;
        setBottomMargin(portrait ? mNavigationBarSize : 0);
        findViewById(R.id.nav_bar_background).setVisibility(portrait ? VISIBLE : GONE);
    }

    private void setBottomMargin(int margin) {
        ViewUtils.setMarginBottom(mRecyclerView, margin);
    }
}