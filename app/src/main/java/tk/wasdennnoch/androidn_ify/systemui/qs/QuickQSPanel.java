package tk.wasdennnoch.androidn_ify.systemui.qs;

import android.content.Context;
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
import tk.wasdennnoch.androidn_ify.systemui.notifications.NotificationPanelHooks;
import tk.wasdennnoch.androidn_ify.systemui.notifications.StatusBarHeaderHooks;
import tk.wasdennnoch.androidn_ify.systemui.qs.tiles.BatteryTile;
import tk.wasdennnoch.androidn_ify.utils.ConfigUtils;
import tk.wasdennnoch.androidn_ify.utils.ResourceUtils;

import static tk.wasdennnoch.androidn_ify.XposedHook.PACKAGE_SYSTEMUI;
import static tk.wasdennnoch.androidn_ify.systemui.qs.QSTileHostHooks.KEY_QUICKQS_TILEVIEW;

public class QuickQSPanel extends LinearLayout {

    private static final String TAG = "QuickQSPanel";

    private int mIconSizePx;
    private int mTileSpacingPx;
    private int mQuickTilePadding;

    private int mMaxTiles;
    private HeaderTileLayout mTileLayout;
    private ResourceUtils mRes;
    private ArrayList<Object> mRecords = new ArrayList<>();
    private ArrayList<View> mIconViews = new ArrayList<>();
    private ArrayList<View> mTopFiveQs = new ArrayList<>();
    private BatteryTile.BatteryView mBatteryView;
    private TouchAnimator mTranslationXAnimator;
    private TouchAnimator mTranslationYAnimator;
    private TouchAnimator mTranslationXAnimator2;
    private TouchAnimator mTranslationYAnimator2;
    private TouchAnimator mFirstPageDelayedAnimator;
    private TouchAnimator mTopFiveQsAnimator;
    private TouchAnimator mFadeAnimator;
    private float oldPosition = 0;
    private boolean mShowPercent;
    private boolean mAllowFancy;

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
        mFirstPageDelayedAnimator = null;
        mTopFiveQsAnimator = null;
        for (int i = 0; i < mMaxTiles && i < tileRecords.size(); i++) {
            Object tilerecord = tileRecords.get(i);
            mRecords.add(tilerecord);
            mTileLayout.addTile(tilerecord);
        }
        StatusBarHeaderHooks.hookQSOnMeasure();
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
        int j = 0;
        TouchAnimator.Builder builder = new TouchAnimator.Builder();
        TouchAnimator.Builder builder1 = new TouchAnimator.Builder();
        TouchAnimator.Builder builder2 = new TouchAnimator.Builder();
        TouchAnimator.Builder builder3 = new TouchAnimator.Builder();
        TouchAnimator.Builder builder4 = new TouchAnimator.Builder();
        TouchAnimator.Builder builder5 = new TouchAnimator.Builder();
        for (int i = 0; i < mIconViews.size(); i++) {
            Object tilerecord = mRecords.get(i);
            View tileView = mIconViews.get(i);
            final ViewGroup qsTileView = (ViewGroup) XposedHelpers.getObjectField(tilerecord, "tileView");

            int ai[] = new int[2];
            int ai1[] = new int[2];

            getRelativePosition(ai, tileView, StatusBarHeaderHooks.mStatusBarHeaderView);
            getRelativePosition(ai1, qsTileView, StatusBarHeaderHooks.mQsPanel);

            int k = ai1[0] - ai[0];
            int i1 = ai1[1] - ai[1] +
                    XposedHelpers.getIntField(qsTileView, "mTilePaddingTopPx") + mTileSpacingPx
                    + getHeight() + (StatusBarHeaderHooks.mUseDragPanel ? 0 : StatusBarHeaderHooks.mQsContainer.getPaddingTop());

            j = ai[0] - j;
            builder.addFloat(tileView, "translationX", 0f, (float) k);
            builder1.addFloat(tileView, "translationY", 0f, (float) i1);

            builder4.addFloat(qsTileView, "translationX", (float) -k, 0f);
            builder5.addFloat(qsTileView, "translationY", gridHeight - StatusBarHeaderHooks.mQsContainer.getPaddingBottom(), 0f);

            mTopFiveQs.add(findIcon(qsTileView));
        }

        Path path = new Path();
        path.moveTo(0.0F, 0.0F);
        path.cubicTo(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
        PathInterpolatorBuilder b = new PathInterpolatorBuilder(0.0F, 0.0F, 0.0F, 1.0F);
        builder.setInterpolator(b.getXInterpolator());
        builder1.setInterpolator(b.getYInterpolator());

        builder2.setStartDelay(0.86f);
        builder2.addFloat(StatusBarHeaderHooks.mQsPanel, "alpha", 0f, 1f);
        if (StatusBarHeaderHooks.mEditButton != null)
            builder2.addFloat(StatusBarHeaderHooks.mEditButton, "alpha", 0f, 1f);
        builder3.setStartDelay(0.9f);
        mTranslationXAnimator = builder.build();
        mTranslationYAnimator = builder1.build();
        mFirstPageDelayedAnimator = builder2.build();
        mTopFiveQsAnimator = builder3.build();
        mTranslationXAnimator2 = builder4.build();
        mTranslationYAnimator2 = builder5.build();
    }

    public void setPosition(float f) {
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
        boolean readyToAnimate = !(mTranslationXAnimator == null || mTranslationYAnimator == null || mFirstPageDelayedAnimator == null || mTopFiveQsAnimator == null);
        boolean disableTranslation = StatusBarHeaderHooks.mDisableFancy;
        if (!readyToAnimate && (NotificationPanelHooks.getStatusBarState() != NotificationPanelHooks.STATE_KEYGUARD)) {
            XposedHook.logD(TAG, "animateFancy: not ready to animate; not on kg: " + (NotificationPanelHooks.getStatusBarState() != NotificationPanelHooks.STATE_KEYGUARD));
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
                    mTranslationXAnimator2.setPosition(f);
                    mTranslationYAnimator2.setPosition(f);
                    mFadeAnimator.setPosition(0);
                } else {
                    mTranslationXAnimator.setPosition(0);
                    mTranslationYAnimator.setPosition(0);
                    mFadeAnimator.setPosition(f);
                }
                mFirstPageDelayedAnimator.setPosition(f);
                mTopFiveQsAnimator.setPosition(f);
                if (mBatteryView != null) {
                    if (mShowPercent && oldPosition < 0.7f && f >= 0.7f) {
                        mBatteryView.setShowPercent(false);
                        mBatteryView.postInvalidate();
                    }
                    if (mShowPercent && oldPosition >= 0.7f && f < 0.7f) {
                        mBatteryView.setShowPercent(true);
                        mBatteryView.postInvalidate();
                    }
                }
            } else {
                XposedHook.logD(TAG, "animateFancy: not ready to animate");
            }
            oldPosition = f;
        } else {
            if (getVisibility() != INVISIBLE)
                setVisibility(INVISIBLE);
        }
    }

    private void getRelativePosition(int ai[], View view, View view1) {
        ai[0] = view.getWidth() / 2;
        ai[1] = 0;
        getRelativePositionInt(ai, view, view1);
    }

    private void getRelativePositionInt(int ai[], View view, View view1) {
        if (view != null && view != view1) {
            ai[0] = (int) ((float) ai[0] + view.getX());
            ai[1] = ai[1] + view.getTop();
            getRelativePositionInt(ai, (View) view.getParent(), view1);
        }
    }

    public void onAnimationAtEnd() {
        setVisibility(INVISIBLE);
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
            View.OnClickListener clickSecondary = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        XposedHelpers.callMethod(tile, "secondaryClick");
                    } catch (Throwable ignore) {
                    }
                }
            };
            View.OnLongClickListener longClick = new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    try {
                        XposedHelpers.callMethod(tile, "longClick");
                    } catch (Throwable ignore) {
                    }
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
