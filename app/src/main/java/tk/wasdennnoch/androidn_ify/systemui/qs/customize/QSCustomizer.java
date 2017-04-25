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
import android.content.res.Configuration;
import android.content.res.Resources;
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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toolbar;
import android.widget.Toolbar.OnMenuItemClickListener;

import java.util.ArrayList;

import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.extracted.systemui.qs.QSDetailClipper;
import tk.wasdennnoch.androidn_ify.misc.SafeRunnable;
import tk.wasdennnoch.androidn_ify.systemui.notifications.NotificationPanelHooks;
import tk.wasdennnoch.androidn_ify.systemui.notifications.StatusBarHeaderHooks;
import tk.wasdennnoch.androidn_ify.systemui.qs.QSTileHostHooks;
import tk.wasdennnoch.androidn_ify.systemui.qs.TileAdapter;
import tk.wasdennnoch.androidn_ify.ui.AddTileActivity;
import tk.wasdennnoch.androidn_ify.utils.ConfigUtils;
import tk.wasdennnoch.androidn_ify.utils.ResourceUtils;
import tk.wasdennnoch.androidn_ify.utils.ViewUtils;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

/**
 * Allows full-screen customization of QS, through show() and hide().
 * <p/>
 * This adds itself to the status bar window, so it can appear on top of quick settings and
 * *someday* do fancy animations to get into/out of it.
 */
public class QSCustomizer extends LinearLayout implements OnMenuItemClickListener, View.OnClickListener {

    private static final int MENU_ADD_BROADCAST_TILE = 1;
    private static final int MENU_RESET = 2;
    private static final int MENU_SECURE_TILES = 3;
    private static final int MENU_CM_SETTINGS = 4;

    public static final int MODE_NORMAL = 0;
    public static final int MODE_EDIT_SECURE = 1;
    public static final int MODE_CM_SETTINGS = 2;

    private final Context mContext;
    private final QSDetailClipper mClipper;
    private final Point mSizePoint = new Point();
    private final int mColor;
    private final LinearLayout mListContainer;
    private final TextView mTitle;
    private final String[] mTitles;

    private boolean mHasNavBar;
    private boolean isShown;
    private final RecyclerView mRecyclerView;
    private final TileAdapter mTileAdapter;
    private final Toolbar mToolbar;
    private final View mDoneButton;
    private boolean mCustomizing;

    private final int mNavigationBarSize;
    private final int mNotificationPanelWidth;
    private int mMode = MODE_NORMAL;
    private View mCmSettings;

    @SuppressWarnings("deprecation")
    public QSCustomizer(Context context) {
        super(context, null);

        mContext = context;
        Context ownContext = ResourceUtils.createOwnContext(mContext);

        ResourceUtils res = ResourceUtils.getInstance(mContext);
        Resources resources = mContext.getResources();
        mHasNavBar = true;
        mColor = resources.getColor(resources.getIdentifier("system_primary_color", "color", XposedHook.PACKAGE_SYSTEMUI));
        mNavigationBarSize = resources.getDimensionPixelSize(resources.getIdentifier("navigation_bar_size", "dimen", XposedHook.PACKAGE_SYSTEMUI));
        mNotificationPanelWidth = context.getResources().getIdentifier("notification_panel_width", "dimen", XposedHook.PACKAGE_SYSTEMUI);

        setGravity(Gravity.CENTER_HORIZONTAL);
        setOrientation(VERTICAL);
        setVisibility(GONE);
        setBackground(resources.getDrawable(resources.getIdentifier("qs_detail_background", "drawable", XposedHook.PACKAGE_SYSTEMUI)));
        mClipper = new QSDetailClipper(this);

        LayoutInflater.from(ownContext).inflate(R.layout.qs_customize_panel_content, this);

        LayoutParams recyclerViewLp = new LayoutParams(MATCH_PARENT, MATCH_PARENT);
        RecyclerView recyclerView = new RecyclerView(context);
        recyclerView.setId(android.R.id.list);
        recyclerView.setLayoutParams(recyclerViewLp);
        recyclerView.setVerticalScrollBarEnabled(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            recyclerView.setScrollIndicators(SCROLL_INDICATOR_TOP);
        }
        mListContainer = (LinearLayout) findViewById(R.id.list_containter);
        mListContainer.addView(recyclerView);

        mTitle = (TextView) findViewById(R.id.title);
        mTitles = new String[] {
                res.getString(R.string.hide_tiles_on_lockscreen),
                res.getString(R.string.cm_qs_settings)
        };

        mToolbar = (Toolbar) findViewById(R.id.action_bar);
        TypedValue value = new TypedValue();
        mContext.getTheme().resolveAttribute(android.R.attr.homeAsUpIndicator, value, true);
        mToolbar.setNavigationIcon(
                resources.getDrawable(value.resourceId, mContext.getTheme()));
        mToolbar.setNavigationOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                hide((int) v.getX() + v.getWidth() / 2, (int) v.getY() + v.getHeight() / 2);
            }
        });
        mToolbar.setOnMenuItemClickListener(this);
        Menu menu = mToolbar.getMenu();
        menu.add(Menu.NONE, MENU_RESET, 0, res.getString(R.string.reset_tiles));
        if (ConfigUtils.M) {
            menu.add(Menu.NONE, MENU_ADD_BROADCAST_TILE, 1, res.getString(R.string.add_custom_tile));
            menu.add(Menu.NONE, MENU_SECURE_TILES, 1, res.getString(R.string.hide_tiles_on_lockscreen));
            try {
                mCmSettings = inflateCmSettings(context);
                menu.add(Menu.NONE, MENU_CM_SETTINGS, 1, res.getString(R.string.cm_qs_settings));
            } catch (Throwable ignore) {
            }
        }
        mToolbar.setTitle(R.string.qs_edit);

        mDoneButton = findViewById(R.id.done_button);
        mDoneButton.setOnClickListener(this);

        mRecyclerView = (RecyclerView) findViewById(android.R.id.list);
        setBottomMargin(mNavigationBarSize);
        mTileAdapter = new TileAdapter(mContext);
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

    private View inflateCmSettings(Context context) {
        View cmSettings = View.inflate(context, context.getResources().getIdentifier("qs_settings", "layout", XposedHook.PACKAGE_SYSTEMUI), null);
        LinearLayout.LayoutParams cmSettingsLp = new LayoutParams(MATCH_PARENT, MATCH_PARENT);
        cmSettings.setLayoutParams(cmSettingsLp);
        return cmSettings;
    }

    public void invalidateTileAdapter() {
        mTileAdapter.invalidate();
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
        if (ConfigUtils.qs().fix_header_space && !ConfigUtils.qs().reconfigure_notification_panel) {
            this.bringToFront();
            this.setVisibility(View.VISIBLE);
        }
    }

    private void initShow(ArrayList<Object> records) {
        if (mTileAdapter.isInvalid()) {
            mTileAdapter.reInit(records, mContext);
        }

        setMode(MODE_NORMAL);

        if (!isShown) {
            isShown = true;
            setVisibility(VISIBLE);
            NotificationPanelHooks.addBarStateCallback(mBarStateCallback);
        }
    }

    public boolean onBackPressed() {
        if (!mCustomizing) return false;

        if (mMode != MODE_NORMAL)
            setMode(MODE_NORMAL);
        else
            hideCircular();
        return true;
    }

    public void hide(boolean animated) {
        if (!isShown) return;
        saveAndHide();
        if (animated) {
            animate().alpha(1)
                    .withEndAction(mHideAnimationListener)
                    .start();
        } else {
            mHideAnimationListener.run();
        }
        if (ConfigUtils.qs().fix_header_space && !ConfigUtils.qs().reconfigure_notification_panel)
            this.setVisibility(View.INVISIBLE);
    }

    public void hideCircular() {
        hide(getWidth() / 2, getHeight() / 2);
    }

    public void hide(int x, int y) {
        if (isShown) {
            saveAndHide();
            mClipper.animateCircularClip(x, y, false, mCollapseAnimationListener);
        }
    }

    public void saveAndHide() {
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
            case MENU_RESET:
                mTileAdapter.resetTiles();
                break;
            case MENU_ADD_BROADCAST_TILE:
                showAddBroadcastTile();
                break;
            case MENU_SECURE_TILES:
                setMode(MODE_EDIT_SECURE);
                break;
            case MENU_CM_SETTINGS:
                setMode(MODE_CM_SETTINGS);
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
        if (!ConfigUtils.M)
            QSTileHostHooks.recreateTiles();
    }

    public void handleStateChanged(Object qsTile, Object state) {
        mTileAdapter.handleStateChanged(qsTile, state);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        getDisplay().getRealSize(mSizePoint);
        int maxWidth = MeasureSpec.getSize(widthMeasureSpec);
        try {
            maxWidth = getResources().getDimensionPixelSize(getResources().getIdentifier("notification_panel_width", "dimen", XposedHook.PACKAGE_SYSTEMUI));
        } catch (Throwable ignore) {
        }
        if (maxWidth == MATCH_PARENT) maxWidth = widthMeasureSpec;
        super.onMeasure(MeasureSpec.makeMeasureSpec(maxWidth, MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(mSizePoint.y, MeasureSpec.EXACTLY));
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
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        int width = getResources().getDimensionPixelSize(mNotificationPanelWidth);
        ViewUtils.setWidth(mRecyclerView, width);

        if (!mHasNavBar) return;
        boolean shouldShow = newConfig.smallestScreenWidthDp >= 600
                || newConfig.orientation != Configuration.ORIENTATION_LANDSCAPE;
        setBottomMargin(shouldShow ? mNavigationBarSize : 0);
        findViewById(R.id.nav_bar_background).setVisibility(shouldShow ? VISIBLE : GONE);
        mRecyclerView.setAdapter(mTileAdapter);
    }

    private void setBottomMargin(int margin) {
        ViewUtils.setMarginBottom(mRecyclerView, margin);
    }

    public void setHasNavBar(boolean hasNavBar) {
        mHasNavBar = hasNavBar;
        if (!mHasNavBar) {
            setBottomMargin(0);
            findViewById(R.id.nav_bar_background).setVisibility(GONE);
        }
    }

    @Override
    public void onClick(View view) {
        setMode(MODE_NORMAL);
    }

    private void setMode(int mode) {
        if (mMode == mode) return;
        mMode = mode;
        onModeChanged();

        boolean normal = mode == MODE_NORMAL;
        transition(mToolbar, normal);
        transition(mDoneButton, !normal);
    }

    private void onModeChanged() {
        mListContainer.removeAllViews();
        mListContainer.addView(getCurrentView());
        mTitle.setText(getTitle());

        getCurrentView();
    }

    private String getTitle() {
        switch (mMode) {
            case MODE_CM_SETTINGS:
                return mTitles[1];
            case MODE_EDIT_SECURE:
                return mTitles[0];
            default:
                return "";
        }
    }

    private View getCurrentView() {
        switch (mMode) {
            case MODE_CM_SETTINGS:
                return mCmSettings;
            case MODE_EDIT_SECURE:
            default:
                mTileAdapter.setMode(mMode);
                return mRecyclerView;
        }
    }

    private static void transition(final View v, final boolean in) {
        if (v.getVisibility() == (in ? VISIBLE : INVISIBLE)) return;
        if (in) {
            v.bringToFront();
            v.setVisibility(View.VISIBLE);
        }
        if (v.hasOverlappingRendering()) {
            v.animate().withLayer();
        }
        v.animate()
                .alpha(in ? 1 : 0)
                .withEndAction(new SafeRunnable() {
                    @Override
                    public void runSafe() {
                        if (!in) {
                            v.setVisibility(View.INVISIBLE);
                        }
                    }
                })
                .start();
    }
}