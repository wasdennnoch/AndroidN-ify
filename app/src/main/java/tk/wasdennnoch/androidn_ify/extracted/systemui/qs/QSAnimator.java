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

package tk.wasdennnoch.androidn_ify.extracted.systemui.qs;

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
import tk.wasdennnoch.androidn_ify.misc.SafeRunnable;
import tk.wasdennnoch.androidn_ify.systemui.SystemUIHooks;
import tk.wasdennnoch.androidn_ify.systemui.notifications.StatusBarHeaderHooks;
import tk.wasdennnoch.androidn_ify.systemui.qs.KeyguardMonitor;
import tk.wasdennnoch.androidn_ify.systemui.qs.QSTileHostHooks;
import tk.wasdennnoch.androidn_ify.utils.ConfigUtils;

public class QSAnimator implements KeyguardMonitor.Callback, PagedTileLayout.PageListener, OnLayoutChangeListener,
        OnAttachStateChangeListener, TouchAnimator.Listener {

    private static final float EXPANDED_TILE_DELAY = .86f;

    private final ArrayList<View> mAllViews = new ArrayList<>();
    private final ArrayList<View> mTopFiveQs = new ArrayList<>();
    private final ArrayList<Integer> mTopFiveX = new ArrayList<>();
    private final QuickQSPanel mQuickQsPanel;
    private final ViewGroup mQsPanel;
    private final ViewGroup mQsContainer;
    private KeyguardMonitor mKeyguard;

    private PagedTileLayout mPagedLayout;

    private boolean mOnFirstPage = true;
    private TouchAnimator mFirstPageAnimator;
    private TouchAnimator mFirstPageDelayedAnimator;
    private TouchAnimator mTranslationXAnimator;
    private TouchAnimator mTranslationYAnimator;
    private TouchAnimator mNonfirstPageAnimator;
    private TouchAnimator mBrightnessAnimator;

    private boolean mOnKeyguard;

    private boolean mAllowFancy = ConfigUtils.qs().allow_fancy_qs_transition;
    private boolean mFullRows = true;
    private int mNumQuickTiles = ConfigUtils.qs().qs_tiles_count;
    private float mLastPosition;

    public QSAnimator(ViewGroup container, QuickQSPanel quickPanel, ViewGroup panel) {
        mQsContainer = container;
        mQuickQsPanel = quickPanel;
        mQsPanel = panel;
        mQsPanel.addOnAttachStateChangeListener(this);
        container.addOnLayoutChangeListener(this);
        mPagedLayout = StatusBarHeaderHooks.qsHooks.getTileLayout();
        mPagedLayout.setPageListener(this);
        mKeyguard = QSTileHostHooks.mKeyguard;
    }

    private void setOnKeyguard(boolean onKeyguard) {
        mOnKeyguard = onKeyguard;
        mQuickQsPanel.setVisibility(mOnKeyguard ? View.INVISIBLE : View.VISIBLE);
        if (mOnKeyguard) {
            clearAnimationState();
        }
    }

    @Override
    public void onViewAttachedToWindow(View v) {
        updateAnimators();
        if (mKeyguard == null) {
            mKeyguard = QSTileHostHooks.mKeyguard;
            mQsPanel.post(new SafeRunnable() {
                @Override
                public void runSafe() {
                    mKeyguard.addCallback(QSAnimator.this);
                }
            });
            return;
        }
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

    public int getTileViewX(Object r) {
        List mRecords = mQuickQsPanel.mRecords;
        for (int i = 0; i < mRecords.size() && i < mTopFiveX.size(); i++) {
            if (mRecords.get(i).equals(r)) {
                return mTopFiveX.get(i);
            }
        }
        return 0;
    }

    private void updateAnimators() {
        List<Object> records = StatusBarHeaderHooks.getHeaderQsPanel().getRecords();

        TouchAnimator.Builder firstPageBuilder = new TouchAnimator.Builder();
        TouchAnimator.Builder translationXBuilder = new TouchAnimator.Builder();
        TouchAnimator.Builder translationYBuilder = new TouchAnimator.Builder();

        if (XposedHelpers.callMethod(mQsPanel, "getHost") == null) return;
        int count = 0;
        int[] loc1 = new int[2];
        int[] loc2 = new int[2];
        int lastXDiff = 0;
        int lastX = 0;

        int maxTilesOnPage = mPagedLayout.getFirstPage().getMaxTiles();

        clearAnimationState();
        mAllViews.clear();
        mTopFiveQs.clear();

        mAllViews.add(StatusBarHeaderHooks.qsHooks.getTileLayout());

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
                mTopFiveX.add(loc1[0]);
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
                firstPageBuilder.addFloat(tileView, "alpha", 0, 1);
            }
            mAllViews.add(tileView);
            mAllViews.add(label);
            count++;
        }
        if (mAllowFancy) {
            View brightness = StatusBarHeaderHooks.qsHooks.getBrightnessView();
            if (brightness != null) {
                firstPageBuilder.addFloat(brightness, "translationY", mQsPanel.getHeight(), 0);
                mBrightnessAnimator = new TouchAnimator.Builder()
                        .addFloat(brightness, "alpha", 0, 1)
                        .setStartDelay(.5f)
                        .build();
                mAllViews.add(brightness);
            } else {
                mBrightnessAnimator = null;
            }
            mFirstPageAnimator = firstPageBuilder
                    .setListener(this)
                    .build();
            // Fade in the tiles/labels as we reach the final position.
            mFirstPageDelayedAnimator = new TouchAnimator.Builder()
                    .setStartDelay(EXPANDED_TILE_DELAY)
                    .addFloat(StatusBarHeaderHooks.qsHooks.getTileLayout(), "alpha", 0, 1).build();
            float px = 0;
            float py = 1;
            if (records.size() <= 3) {
                px = 1;
            } else if (records.size() <= 6) {
                px = .4f;
            }
            PathInterpolatorBuilder interpolatorBuilder = new PathInterpolatorBuilder(0, 0, px, py);
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
        int animatedTiles = mNumQuickTiles;
        if (mPagedLayout.getFirstPage().mFirstRowLarge)
            animatedTiles += columnCount - 2;
        return count < ((animatedTiles + columnCount - 1) / columnCount) * columnCount;
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
        if (mOnFirstPage && mAllowFancy && (!StatusBarHeaderHooks.mShowingDetail || position == 0)) {
            mQuickQsPanel.setAlpha(1);
            mFirstPageAnimator.setPosition(position);
            mFirstPageDelayedAnimator.setPosition(position);
            mTranslationXAnimator.setPosition(position);
            mTranslationYAnimator.setPosition(position);
            if (mBrightnessAnimator != null) {
                mBrightnessAnimator.setPosition(position);
            }
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