package tk.wasdennnoch.androidn_ify.systemui.qs;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Space;

import java.util.ArrayList;

import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.extracted.systemui.PathInterpolatorBuilder;
import tk.wasdennnoch.androidn_ify.extracted.systemui.TouchAnimator;
import tk.wasdennnoch.androidn_ify.misc.SafeOnClickListener;
import tk.wasdennnoch.androidn_ify.misc.SafeOnLongClickListener;
import tk.wasdennnoch.androidn_ify.systemui.notifications.NotificationPanelHooks;
import tk.wasdennnoch.androidn_ify.systemui.notifications.StatusBarHeaderHooks;
import tk.wasdennnoch.androidn_ify.systemui.qs.tiles.BatteryTile;
import tk.wasdennnoch.androidn_ify.utils.ConfigUtils;
import tk.wasdennnoch.androidn_ify.utils.ResourceUtils;

import static tk.wasdennnoch.androidn_ify.XposedHook.PACKAGE_SYSTEMUI;
import static tk.wasdennnoch.androidn_ify.systemui.qs.QSTileHostHooks.KEY_QUICKQS_TILEVIEW;

public class QuickQSPanel extends LinearLayout {

    private static final String TAG = "QuickQSPanel";
    private static final float EXPANDED_TILE_DELAY = .7f;
    private static final float LAST_ROW_EXPANDED_DELAY = .86f;

    private int mIconSizePx;
    private int mTileSpacingPx;
    private int mQuickTilePadding;

    private int mMaxTiles;
    private HeaderTileLayout mTileLayout;
    private ResourceUtils mRes;
    private ArrayList<Object> mRecords = new ArrayList<>();
    private ArrayList<View> mIconViews = new ArrayList<>();
    private ArrayList<View> mTopFiveQs = new ArrayList<>();
    private ArrayList<Integer> mTopFiveX = new ArrayList<>();
    private BatteryTile.BatteryView mBatteryView;
    private TouchAnimator mTranslationXAnimator;
    private TouchAnimator mTranslationYAnimator;
    private TouchAnimator mLabelTranslationXAnimator;
    private TouchAnimator mLabelTranslationYAnimator;
    private TouchAnimator mFirstPageAnimator;
    private TouchAnimator mFirstPageDelayedAnimator;
    private TouchAnimator mLastRowAnimator;
    private TouchAnimator mFadeAnimator;
    private float oldPosition = 0;
    private boolean mShowPercent;
    private boolean mAllowFancy;
    private boolean mIsLandscape;
    private float mLastPosition = 0;
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case Intent.ACTION_CONFIGURATION_CHANGED:
                    if (!mAllowFancy) return;
                    updateLandscape();
                    if (mIsLandscape) {
                        showTopFive();
                    } else {
                        onAnimationStarted();
                    }
                    break;
            }
        }
    };

    public QuickQSPanel(Context context) {
        super(context);
        ConfigUtils config = ConfigUtils.getInstance();
        Resources res = context.getResources();
        mIconSizePx = res.getDimensionPixelSize(res.getIdentifier("qs_tile_icon_size", "dimen", PACKAGE_SYSTEMUI));
        mTileSpacingPx = res.getDimensionPixelSize(res.getIdentifier("qs_tile_spacing", "dimen", PACKAGE_SYSTEMUI));
        mRes = ResourceUtils.getInstance(context);
        mQuickTilePadding = mRes.getDimensionPixelSize(R.dimen.qs_quick_tile_padding);
        mMaxTiles = config.qs.qs_tiles_count;
        mShowPercent = config.qs.battery_tile_show_percentage;
        mAllowFancy = config.qs.allow_fancy_qs_transition;
        setOrientation(VERTICAL);
        int m = mRes.getDimensionPixelSize(R.dimen.qs_quick_panel_margin_horizontal);
        setPadding(m, mRes.getDimensionPixelSize(R.dimen.qs_quick_panel_padding_top), m, mRes.getDimensionPixelSize(R.dimen.qs_quick_panel_padding_bottom));
        mTileLayout = new HeaderTileLayout(context);
        addView(mTileLayout);

        mFadeAnimator = new TouchAnimator.Builder()
                .addFloat(this, "alpha", 1.0F, 0.0F)
                .setEndDelay(0.64F).build();

        updateLandscape();
    }

    private void updateLandscape() {
        mIsLandscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
        getContext().registerReceiver(mBroadcastReceiver, filter);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        getContext().unregisterReceiver(mBroadcastReceiver);
    }

    public int getTileViewX(Object r) {
        for (int i = 0; i < mRecords.size() && i < mTopFiveX.size(); i++) {
            if (mRecords.get(i).equals(r)) {
                return mTopFiveX.get(i);
            }
        }
        return 0;
    }

    public void setTiles(ArrayList<Object> tileRecords) {
        XposedHook.logD(TAG, "setTiles tile record count: " + tileRecords.size());
        if (tileRecords.size() == 0) {
            XposedHook.logW(TAG, "setTiles: Empty tileRecord list!");
            return;
        }
        mTileLayout.removeTiles();
        mRecords.clear();
        mIconViews.clear();
        mTranslationXAnimator = null;
        mTranslationYAnimator = null;
        mLabelTranslationXAnimator = null;
        mLabelTranslationYAnimator = null;
        mFirstPageDelayedAnimator = null;
        mLastRowAnimator = null;

        for (int i = 0; i < tileRecords.size(); i++) {
            Object tilerecord = tileRecords.get(i);
            mRecords.add(tilerecord);
            if (i < mMaxTiles)
                mTileLayout.addTile(tilerecord);
        }
        StatusBarHeaderHooks.postSetupAnimators();
    }

    public void handleStateChanged(Object qsTile, Object state) {
        ViewGroup tileView = (ViewGroup) XposedHelpers.getAdditionalInstanceField(qsTile, KEY_QUICKQS_TILEVIEW);
        if (tileView != null) {
            XposedHelpers.callMethod(tileView, "onStateChanged", state);
        }
    }

    public void setupAnimators(int gridHeight) {
        XposedHook.logD(TAG, "setupAnimators called");
        mTopFiveQs.clear();
        mTopFiveX.clear();
        int j = 0;
        int iconViewsCount = mIconViews.size();
        int qsPanelMarginBottom = ResourceUtils.getInstance(getContext()).getDimensionPixelSize(R.dimen.qs_panel_margin_bottom);
        TouchAnimator.Builder translationXBuilder = new TouchAnimator.Builder();
        TouchAnimator.Builder translationYBuilder = new TouchAnimator.Builder();
        TouchAnimator.Builder labelTranslationXBuilder = new TouchAnimator.Builder();
        TouchAnimator.Builder labelTranslationYBuilder = new TouchAnimator.Builder();
        TouchAnimator.Builder firstPageBuilder = new TouchAnimator.Builder();
        TouchAnimator.Builder lastRowBuilder = new TouchAnimator.Builder();
        for (int i = 0; i < mRecords.size(); i++) {
            Object tileRecord = mRecords.get(i);
            final ViewGroup qsTileView = (ViewGroup) XposedHelpers.getObjectField(tileRecord, "tileView");
            if (i < iconViewsCount) {

                View tileView = mIconViews.get(i);

                int ai[] = new int[2];
                int ai1[] = new int[2];

                getRelativePosition(ai, tileView, StatusBarHeaderHooks.mStatusBarHeaderView);
                getRelativePosition(ai1, qsTileView, StatusBarHeaderHooks.mQsPanel);

                int k = ai1[0] - ai[0];
                int i1 = ai1[1] - ai[1] +
                        XposedHelpers.getIntField(qsTileView, "mTilePaddingTopPx") + mTileSpacingPx
                        + getHeight() + (StatusBarHeaderHooks.mUseDragPanel ? 0 : StatusBarHeaderHooks.mQsContainer.getPaddingTop());

                j = ai[0] - j;
                translationXBuilder.addFloat(tileView, "translationX", 0f, (float) k);
                translationYBuilder.addFloat(tileView, "translationY", 0f, (float) i1);

                boolean dual = XposedHelpers.getBooleanField(qsTileView, "mDual");
                View label = (View) XposedHelpers.getObjectField(qsTileView, dual ? "mDualLabel" : "mLabel");

                labelTranslationXBuilder.addFloat(label, "translationX", (float) -k, 0f);
                labelTranslationYBuilder.addFloat(label, "translationY", (float) -i1, 0f);

                firstPageBuilder.addFloat(qsTileView, "translationY", gridHeight + qsPanelMarginBottom, 0f);

                mTopFiveQs.add(findIcon(qsTileView));
                mTopFiveX.add(ai[0]);
            } else {
                lastRowBuilder.addFloat(qsTileView, "alpha", 0f, 1f);
            }
        }

        Path path = new Path();
        path.moveTo(0.0F, 0.0F);
        path.cubicTo(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
        PathInterpolatorBuilder b = new PathInterpolatorBuilder(0.0F, 0.0F, 0.0F, 1.0F);
        translationXBuilder.setInterpolator(b.getXInterpolator());
        translationYBuilder.setInterpolator(b.getYInterpolator());
        labelTranslationXBuilder.setInterpolator(b.getXInterpolator());
        labelTranslationYBuilder.setInterpolator(b.getYInterpolator());

        mTranslationXAnimator = translationXBuilder.build();
        mTranslationYAnimator = translationYBuilder.build();
        mLabelTranslationXAnimator = labelTranslationXBuilder.build();
        mLabelTranslationYAnimator = labelTranslationYBuilder.build();
        mFirstPageAnimator = firstPageBuilder.build();
        mLastRowAnimator = lastRowBuilder
                .setStartDelay(LAST_ROW_EXPANDED_DELAY)
                .build();

        TouchAnimator.Builder firstPageDelayedBuilder = new TouchAnimator.Builder();
        firstPageDelayedBuilder.setStartDelay(EXPANDED_TILE_DELAY);
        firstPageDelayedBuilder.addFloat(StatusBarHeaderHooks.mQsPanel, "alpha", 0f, 1f);
        if (StatusBarHeaderHooks.mDecorLayout != null)
            firstPageDelayedBuilder.addFloat(StatusBarHeaderHooks.mDecorLayout, "alpha", 0f, 1f);
        mFirstPageDelayedAnimator = firstPageDelayedBuilder.build();
        setPosition(mLastPosition);
    }

    public void setPosition(float f) {
        mLastPosition = f;
        if (mAllowFancy) {
            animateFancy(f);
        } else {
            animateFade(f);
        }
    }

    private void animateFade(float f) {
        mFadeAnimator.setPosition(f);
        setVisibility(f < 0.36F ? View.VISIBLE : View.INVISIBLE);
    }

    private void animateFancy(float f) {
        boolean readyToAnimate = !(mTranslationXAnimator == null || mTranslationYAnimator == null
                || mFirstPageDelayedAnimator == null || mFirstPageAnimator == null || mLastRowAnimator == null);
        boolean disableTranslation = StatusBarHeaderHooks.mDisableFancy || mIsLandscape;
        if (!readyToAnimate && (NotificationPanelHooks.getStatusBarState() != NotificationPanelHooks.STATE_KEYGUARD)) {
            return;
        }
        if (!StatusBarHeaderHooks.mShowingDetail || f == 0) {
            if (oldPosition == 1 && f != oldPosition) {
                if (!disableTranslation) {
                    onAnimationStarted();
                } else {
                    setVisibility(VISIBLE);
                }
            }
            if (oldPosition != 1 && f == 1 && !disableTranslation) {
                onAnimationAtEnd();
            }
            if (oldPosition == 0 && f != oldPosition) {
                if (!disableTranslation) {
                    onAnimationStarted();
                } else {
                    setVisibility(VISIBLE);
                }
            }
            if (readyToAnimate) {
                if (!disableTranslation) {
                    mTranslationXAnimator.setPosition(f);
                    mTranslationYAnimator.setPosition(f);
                    mLabelTranslationXAnimator.setPosition(f);
                    mLabelTranslationYAnimator.setPosition(f);
                    mFirstPageAnimator.setPosition(f);
                    mFadeAnimator.setPosition(0);
                    mLastRowAnimator.setPosition(f);
                } else {
                    mTranslationXAnimator.setPosition(0);
                    mTranslationYAnimator.setPosition(0);
                    mLabelTranslationXAnimator.setPosition(1);
                    mLabelTranslationYAnimator.setPosition(1);
                    mFadeAnimator.setPosition(f);
                    mLastRowAnimator.setPosition(1);
                }
                mFirstPageDelayedAnimator.setPosition(f);
                if (mShowPercent && mBatteryView != null) {
                    if (oldPosition < 0.7f && f >= 0.7f) {
                        mBatteryView.setShowPercent(false);
                        mBatteryView.postInvalidate();
                    }
                    if (oldPosition >= 0.7f && f < 0.7f) {
                        mBatteryView.setShowPercent(true);
                        mBatteryView.postInvalidate();
                    }
                }
            }
            oldPosition = f;
        } else {
            mFirstPageDelayedAnimator.setPosition(1);
            if (getVisibility() != INVISIBLE)
                setVisibility(INVISIBLE);
        }
    }

    public static void getRelativePosition(int ai[], View view, View view1) {
        ai[0] = view.getWidth() / 2;
        ai[1] = 0;
        getRelativePositionInt(ai, view, view1);
    }

    private static void getRelativePositionInt(int ai[], View view, View view1) {
        if (view != null && view != view1) {
            ai[0] = (int) ((float) ai[0] + view.getX());
            ai[1] = ai[1] + view.getTop();
            getRelativePositionInt(ai, (View) view.getParent(), view1);
        }
    }

    public void onAnimationAtEnd() {
        setVisibility(INVISIBLE);
        showTopFive();
    }

    private void showTopFive() {
        for (View v : mTopFiveQs)
            v.setVisibility(VISIBLE);
    }

    public void onAnimationStarted() {
        if (NotificationPanelHooks.getStatusBarState() != NotificationPanelHooks.STATE_KEYGUARD) {
            setVisibility(VISIBLE);
            for (View v : mTopFiveQs)
                v.setVisibility(INVISIBLE);
        } else {
            onAnimationAtEnd();
        }
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

    private class HeaderTileLayout extends LinearLayout {

        private final Space mEndSpacer;

        public HeaderTileLayout(Context context) {
            super(context);
            setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
            setOrientation(HORIZONTAL);
            setGravity(Gravity.CENTER_VERTICAL);
            setClipChildren(false);
            setClipToPadding(false);
            mEndSpacer = new Space(context);
            mEndSpacer.setLayoutParams(generateSpacerLayoutParams());
            updateDownArrowMargin();
            addView(mEndSpacer);
        }

        public void addTile(Object tilerecord) {
            final Object tile = XposedHelpers.getObjectField(tilerecord, "tile");
            XposedHook.logD(TAG, "addTile: original tileView class: " +
                    XposedHelpers.getObjectField(tilerecord, "tileView").getClass().getSimpleName() +
                    " for tile " + tile.getClass().getSimpleName());
            ViewGroup tileView = (ViewGroup) XposedHelpers.callMethod(tile, "createTileView", getContext());
            XposedHelpers.setAdditionalInstanceField(tileView, "headerTileRowItem", true);
            XposedHelpers.setAdditionalInstanceField(tileView, "headerTileRowType", tile.getClass().getSimpleName());

            View.OnClickListener click = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        XposedHelpers.callMethod(tile, "click");
                    } catch (Throwable t) {
                        try { // PA
                            XposedHelpers.callMethod(tile, "click", false);
                        } catch (Throwable ignore) {
                        }
                    }
                }
            };
            View.OnClickListener clickSecondary = new SafeOnClickListener() {
                @Override
                public void onClickSafe(View v) {
                    XposedHelpers.callMethod(tile, "secondaryClick");
                }
            };
            View.OnLongClickListener longClick = new SafeOnLongClickListener() {
                @Override
                public boolean onLongClickSafe(View v) {
                    XposedHelpers.callMethod(tile, "longClick");
                    return true;
                }
            };
            try {
                XposedHelpers.callMethod(tileView, "init", click, clickSecondary, longClick);
            } catch (Throwable t) {
                try {
                    XposedHelpers.callMethod(tileView, "init", click, clickSecondary);
                } catch (Throwable t2) {
                    try {
                        XposedHelpers.callMethod(tileView, "initlongClickListener", longClick);
                        XposedHelpers.callMethod(tileView, "init", click, clickSecondary);
                    } catch (Throwable t3) {
                        try {
                            XposedHelpers.callMethod(tileView, "init", click, longClick);
                        } catch (Throwable t4) {
                            XposedHook.logE(TAG, "Couldn't init click listeners", null);
                        }
                    }
                }
            }
            try {
                XposedHelpers.callMethod(tileView, "setDual", false);
            } catch (Throwable t) { // CM13
                XposedHelpers.callMethod(tileView, "setDual", false, false);
            }
            try {
                XposedHelpers.callMethod(tileView, "onStateChanged", XposedHelpers.callMethod(tile, "getState"));
            } catch (Throwable t) {
                XposedHelpers.callMethod(tileView, "onStateChanged", XposedHelpers.getObjectField(tile, "mState"));
            }

            View iconView = null;
            int children = tileView.getChildCount();
            for (int i = 0; i < children; i++) {
                View child = tileView.getChildAt(i);
                // no complex "getResources().getIdenifier("blah", "blah", "blah")"? I love it!
                // FrameLayout is the container of the signal state icons
                if (child.getId() == android.R.id.icon || child instanceof FrameLayout) {
                    child.setVisibility(VISIBLE);
                    iconView = child;
                    if (mShowPercent && iconView instanceof FrameLayout) {
                        if (((FrameLayout) iconView).getChildAt(0) != null) {
                            View frameChild = ((FrameLayout) iconView).getChildAt(0);
                            if (frameChild instanceof BatteryTile.BatteryView) {
                                mBatteryView = (BatteryTile.BatteryView) frameChild;
                                mBatteryView.setShowPercent(true);
                            }
                        }
                    }
                } else {
                    child.setVisibility(GONE);
                }
            }

            XposedHelpers.setAdditionalInstanceField(tile, KEY_QUICKQS_TILEVIEW, tileView);

            int position = getChildCount() - 1;
            if (iconView != null) {
                ((ViewGroup) iconView.getParent()).removeView(iconView);
                addViewToLayout(iconView, position, click, longClick);
            } else {
                addView(tileView, position, generateOriginalLayoutParams());
            }
            addView(new Space(getContext()), position + 1, generateSpaceParams());
        }

        private void addViewToLayout(View view, int position, OnClickListener click, OnLongClickListener longClick) {
            view.setClickable(false);
            RelativeLayout container = new RelativeLayout(view.getContext());
            container.setClickable(true);
            container.setOnClickListener(click);
            container.setOnLongClickListener(longClick);
            container.setBackground(newTileBackground());
            container.addView(view, generateLayoutParams());
            new GlobalLayoutListener(container);
            addView(container, position, generateContainerLayoutParams());
            mIconViews.add(view);
        }

        private class GlobalLayoutListener {
            private View mView;

            protected GlobalLayoutListener(View view) {
                mView = view;
                mView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        mView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        mView.getBackground().setHotspot(mView.getWidth() / 2, mView.getHeight() / 2);
                    }
                });
            }
        }

        public void removeTiles() {
            for (int i = 0; i < mMaxTiles && i < mIconViews.size(); i++) {
                removeViewAt(0); // Tile
                removeViewAt(0); // Space
            }
        }

        private RelativeLayout.LayoutParams generateLayoutParams() {
            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, mIconSizePx);
            lp.addRule(RelativeLayout.CENTER_IN_PARENT);
            return lp;
        }

        private LayoutParams generateOriginalLayoutParams() {
            int i = mRes.getDimensionPixelSize(R.dimen.qs_quick_tile_size);
            LayoutParams layoutparams = new LayoutParams(i, i);
            layoutparams.gravity = Gravity.CENTER;
            return layoutparams;
        }

        private LayoutParams generateSpacerLayoutParams() {
            int i = mRes.getDimensionPixelSize(R.dimen.qs_quick_tile_size);
            LayoutParams layoutparams = new LayoutParams(i, i);
            layoutparams.gravity = Gravity.CENTER;
            return layoutparams;
        }

        private LayoutParams generateContainerLayoutParams() {
            int i = mRes.getDimensionPixelSize(R.dimen.qs_quick_tile_size);
            LayoutParams layoutparams = new LayoutParams(i, i);
            layoutparams.gravity = Gravity.CENTER;
            return layoutparams;
        }

        private LayoutParams generateSpaceParams() {
            LayoutParams layoutparams = new LayoutParams(0, mRes.getDimensionPixelSize(R.dimen.qs_quick_tile_size));
            layoutparams.weight = 1.0F;
            layoutparams.gravity = Gravity.CENTER;
            return layoutparams;
        }

        private void updateDownArrowMargin() {
            LayoutParams layoutparams = (LayoutParams) mEndSpacer.getLayoutParams();
            layoutparams.setMarginStart(mQuickTilePadding);
            mEndSpacer.setLayoutParams(layoutparams);
        }

        private Drawable newTileBackground() {
            final int[] attrs = new int[]{android.R.attr.selectableItemBackgroundBorderless};
            final TypedArray ta = getContext().obtainStyledAttributes(attrs);
            final Drawable d = ta.getDrawable(0);
            ta.recycle();
            return d;
        }

        @Override
        public boolean hasOverlappingRendering() {
            return false;
        }

        @Override
        protected void onConfigurationChanged(Configuration configuration) {
            super.onConfigurationChanged(configuration);
            updateDownArrowMargin();
        }

    }

}
