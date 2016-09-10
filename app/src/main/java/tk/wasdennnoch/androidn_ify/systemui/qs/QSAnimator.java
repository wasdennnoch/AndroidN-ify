/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package tk.wasdennnoch.androidn_ify.systemui.qs;

import android.graphics.Path;
import android.view.View;
import android.view.View.OnAttachStateChangeListener;
import android.view.View.OnLayoutChangeListener;
import android.view.ViewGroup;
import android.view.ViewRootImpl;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.extracted.systemui.PathInterpolatorBuilder;
import tk.wasdennnoch.androidn_ify.extracted.systemui.TouchAnimator;
import tk.wasdennnoch.androidn_ify.systemui.SystemUIHooks;
import tk.wasdennnoch.androidn_ify.systemui.notifications.StatusBarHeaderHooks;
import tk.wasdennnoch.androidn_ify.utils.ConfigUtils;

public class QSAnimator implements KeyguardMonitor.Callback, PagedTileLayout.PageListener, OnLayoutChangeListener,
        OnAttachStateChangeListener, TouchAnimator.Listener {

    private static final String TAG = "QSAnimator";

    public static final float EXPANDED_TILE_DELAY = .7f;
    private static final float LAST_ROW_EXPANDED_DELAY = .86f;

    private final ArrayList<View> mAllViews = new ArrayList<>();
    private final ArrayList<View> mTopFiveQs = new ArrayList<>();
    private final QuickQSPanel mQuickQsPanel;
    private final ViewGroup mQsPanel;
    private final ViewGroup mQsContainer;
    private final KeyguardMonitor mKeyguard;

    private PagedTileLayout mPagedLayout;

    private boolean mOnFirstPage = true;
    private TouchAnimator mFirstPageAnimator;
    private TouchAnimator mFirstPageDelayedAnimator;
    private TouchAnimator mTranslationXAnimator;
    private TouchAnimator mTranslationYAnimator;
    private TouchAnimator mNonfirstPageAnimator;
    private TouchAnimator mLastRowAnimator;

    private boolean mOnKeyguard;

    private boolean mAllowFancy = true;
    private boolean mFullRows = true;
    private int mNumQuickTiles = ConfigUtils.qs().qs_tiles_count;
    private float mLastPosition;

    public QSAnimator(ViewGroup container, QuickQSPanel quickPanel, ViewGroup panel) {
        mQsContainer = container;
        mQuickQsPanel = quickPanel;
        mQsPanel = panel;
        mQsPanel.addOnAttachStateChangeListener(this);
        container.addOnLayoutChangeListener(this);
        mPagedLayout = SystemUIHooks.qsHooks.getTileLayout();
        mPagedLayout.setPageListener(this);
        mKeyguard = QSTileHostHooks.mKeyguard;
    }

    public void setOnKeyguard(boolean onKeyguard) {
        mOnKeyguard = onKeyguard;
        mQuickQsPanel.setVisibility(mOnKeyguard ? View.INVISIBLE : View.VISIBLE);
        if (mOnKeyguard) {
            clearAnimationState();
        }
    }

    @Override
    public void onViewAttachedToWindow(View v) {
        updateAnimators();
        mKeyguard.addCallback(this);
    }

    @Override
    public void onViewDetachedFromWindow(View v) {
        mKeyguard.removeCallback(this);
    }

    @Override
    public void onPageChanged(boolean isFirst) {
        if (mOnFirstPage == isFirst) return;
        if (!isFirst) {
            clearAnimationState();
        }
        mOnFirstPage = isFirst;
    }

    public void updateAnimators() {
        List<Object> records = StatusBarHeaderHooks.getHeaderQsPanel().getRecords();

        TouchAnimator.Builder firstPageBuilder = new TouchAnimator.Builder();
        TouchAnimator.Builder translationXBuilder = new TouchAnimator.Builder();
        TouchAnimator.Builder translationYBuilder = new TouchAnimator.Builder();
        TouchAnimator.Builder lastRowBuilder = new TouchAnimator.Builder();

        if (XposedHelpers.callMethod(mQsPanel, "getHost") == null) return;
        int count = 0;
        int[] loc1 = new int[2];
        int[] loc2 = new int[2];
        int lastXDiff = 0;
        int lastYDiff = 0;
        int lastX = 0;

        int maxTilesOnPage = mPagedLayout.getColumnCount() * mPagedLayout.getRowCount();

        clearAnimationState();
        mAllViews.clear();
        mTopFiveQs.clear();

        mAllViews.add(SystemUIHooks.qsHooks.getTileLayout());

        for (int i = 0; i < records.size(); i++) {
            Object tileRecord = records.get(i);
            final ViewGroup tileView = (ViewGroup) XposedHelpers.getObjectField(tileRecord, "tileView");
            boolean dual = XposedHelpers.getBooleanField(tileView, "mDual");
            final View label = (View) XposedHelpers.getObjectField(tileView, dual ? "mDualLabel" : "mLabel");
            final View tileIcon = findIcon(tileView);
            if (count < mNumQuickTiles && mAllowFancy) {
                // Quick tiles.
                View quickTileView = mQuickQsPanel.getTileView(i);

                lastX = loc1[0];
                getRelativePosition(loc1, quickTileView, StatusBarHeaderHooks.mStatusBarHeaderView);
                getRelativePosition(loc2, tileIcon, mQsPanel);
                final int xDiff = loc2[0] - loc1[0] + ((i < maxTilesOnPage) ? 0 : mPagedLayout.getWidth());
                final int yDiff = loc2[1] - loc1[1] +
                        + mQuickQsPanel.getHeight() + (StatusBarHeaderHooks.mUseDragPanel ? 0 : StatusBarHeaderHooks.mQsContainer.getPaddingTop());

                lastXDiff = loc1[0] - lastX;
                lastYDiff = yDiff;
                // Move the quick tile right from its location to the new one.
                translationXBuilder.addFloat(quickTileView, "translationX", 0, xDiff);
                translationYBuilder.addFloat(quickTileView, "translationY", 0, yDiff);

                // Counteract the parent translation on the tile. So we have a static base to
                // animate the label position off from.
                firstPageBuilder.addFloat(tileView, "translationY", mQsPanel.getHeight(), 0);

                // Move the real tile's label from the quick tile position to its final
                // location.
                translationXBuilder.addFloat(label, "translationX", -xDiff, 0);
                translationYBuilder.addFloat(label, "translationY", -yDiff, 0);

                mTopFiveQs.add(tileIcon);
                mAllViews.add(tileIcon);
                mAllViews.add(quickTileView);
            } else if (mFullRows && isIconInAnimatedRow(count)) {
                // TODO: Refactor some of this, it shares a lot with the above block.
                // Move the last tile position over by the last difference between quick tiles.
                // This makes the extra icons seems as if they are coming from positions in the
                // quick panel.
                loc1[0] += lastXDiff;
                getRelativePosition(loc2, tileIcon, mQsContainer);
                final int xDiff = loc2[0] - loc1[0];
                final int yDiff = loc2[1] - loc1[1];

                firstPageBuilder.addFloat(tileView, "translationY", mQsPanel.getHeight(), 0);
                translationXBuilder.addFloat(tileView, "translationX", -xDiff, 0);
                translationYBuilder.addFloat(label, "translationY", -yDiff, 0);
                translationYBuilder.addFloat(tileIcon, "translationY", -yDiff, 0);

                mAllViews.add(tileIcon);
            } else {
                lastRowBuilder.addFloat(tileView, "alpha", 0, 1);
            }
            mAllViews.add(tileView);
            mAllViews.add(label);
            count++;
        }
        if (mAllowFancy) {
            mFirstPageAnimator = firstPageBuilder
                    .setListener(this)
                    .build();
            // Fade in the tiles/labels as we reach the final position.
            mFirstPageDelayedAnimator = new TouchAnimator.Builder()
                    .setStartDelay(EXPANDED_TILE_DELAY)
                    .addFloat(SystemUIHooks.qsHooks.getTileLayout(), "alpha", 0, 1).build();
            mLastRowAnimator = lastRowBuilder
                    .setStartDelay(LAST_ROW_EXPANDED_DELAY)
                    .build();
            Path path = new Path();
            path.moveTo(0, 0);
            path.cubicTo(0, 0, 0, 1, 1, 1);
            PathInterpolatorBuilder interpolatorBuilder = new PathInterpolatorBuilder(0, 0, 0, 1);
            translationXBuilder.setInterpolator(interpolatorBuilder.getXInterpolator());
            translationYBuilder.setInterpolator(interpolatorBuilder.getYInterpolator());
            mTranslationXAnimator = translationXBuilder.build();
            mTranslationYAnimator = translationYBuilder.build();
        }
        mNonfirstPageAnimator = new TouchAnimator.Builder()
                .addFloat(mQuickQsPanel, "alpha", 1, 0)
                .setListener(mNonFirstPageListener)
                .setEndDelay(.5f)
                .build();
    }

    private View findIcon(ViewGroup view) {
        int children = view.getChildCount();
        for (int i = 0; i < children; i++) {
            View child = view.getChildAt(i);
            if (child.getId() == android.R.id.icon || child instanceof FrameLayout) {
                return child;
            }
        }
        return view;
    }

    private boolean isIconInAnimatedRow(int count) {
        if (mPagedLayout == null) {
            return false;
        }
        final int columnCount = mPagedLayout.getColumnCount();
        return count < ((mNumQuickTiles + columnCount - 1) / columnCount) * columnCount;
    }

    private void getRelativePosition(int[] loc1, View view, View parent) {
        loc1[0] = view.getWidth() / 2;
        loc1[1] = 0;
        getRelativePositionInt(loc1, view, parent);
    }

    private void getRelativePositionInt(int[] loc1, View view, View parent) {
        if (view == parent || view == null) return;
        // Ignore tile pages as they can have some offset we don't want to take into account in
        // RTL.
        if (!(view instanceof PagedTileLayout.TilePage)) {
            loc1[0] += view.getLeft();
            loc1[1] += view.getTop();
        }
        if (!(view.getParent() instanceof ViewRootImpl))
            getRelativePositionInt(loc1, (View) view.getParent(), parent);
    }

    public void setPosition(float position) {
        if (mFirstPageAnimator == null) return;
        if (mOnKeyguard) {
            return;
        }
        mLastPosition = position;
        if (mOnFirstPage && mAllowFancy) {
            mQuickQsPanel.setAlpha(1);
            mFirstPageAnimator.setPosition(position);
            mFirstPageDelayedAnimator.setPosition(position);
            mTranslationXAnimator.setPosition(position);
            mTranslationYAnimator.setPosition(position);
            mLastRowAnimator.setPosition(position);
        } else {
            mNonfirstPageAnimator.setPosition(position);
        }
    }

    @Override
    public void onAnimationAtStart() {
        mQuickQsPanel.setVisibility(View.VISIBLE);
    }

    @Override
    public void onAnimationAtEnd() {
        mQuickQsPanel.setVisibility(View.INVISIBLE);
        final int N = mTopFiveQs.size();
        for (int i = 0; i < N; i++) {
            mTopFiveQs.get(i).setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onAnimationStarted() {
        mQuickQsPanel.setVisibility(mOnKeyguard ? View.INVISIBLE : View.VISIBLE);
        if (mOnFirstPage) {
            final int N = mTopFiveQs.size();
            for (int i = 0; i < N; i++) {
                mTopFiveQs.get(i).setVisibility(View.INVISIBLE);
            }
        }
    }

    private void clearAnimationState() {
        final int N = mAllViews.size();
        mQuickQsPanel.setAlpha(0);
        for (int i = 0; i < N; i++) {
            View v = mAllViews.get(i);
            v.setAlpha(1);
            v.setTranslationX(0);
            v.setTranslationY(0);
        }
        final int N2 = mTopFiveQs.size();
        for (int i = 0; i < N2; i++) {
            mTopFiveQs.get(i).setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft,
                               int oldTop, int oldRight, int oldBottom) {
        //mQsPanel.post(mUpdateAnimators);
    }

    /*
    @Override
    public void onTilesChanged() {
        // Give the QS panels a moment to generate their new tiles, then create all new animators
        // hooked up to the new views.
        mQsPanel.post(mUpdateAnimators);
    }
    */

    private final TouchAnimator.Listener mNonFirstPageListener =
            new TouchAnimator.ListenerAdapter() {
                @Override
                public void onAnimationStarted() {
                    mQuickQsPanel.setVisibility(View.VISIBLE);
                }
            };

    public Runnable mUpdateAnimators = new Runnable() {
        @Override
        public void run() {
            updateAnimators();
            setPosition(mLastPosition);
        }
    };

    @Override
    public void onKeyguardChanged() {
        setOnKeyguard(mKeyguard.isShowing());
    }
}