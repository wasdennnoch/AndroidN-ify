package tk.wasdennnoch.androidn_ify.extracted.systemui.qs;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
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
import java.util.List;

import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.misc.SafeOnClickListener;
import tk.wasdennnoch.androidn_ify.misc.SafeOnLongClickListener;
import tk.wasdennnoch.androidn_ify.systemui.notifications.StatusBarHeaderHooks;
import tk.wasdennnoch.androidn_ify.systemui.qs.tiles.BatteryTile;
import tk.wasdennnoch.androidn_ify.utils.ConfigUtils;
import tk.wasdennnoch.androidn_ify.utils.ResourceUtils;

import static tk.wasdennnoch.androidn_ify.XposedHook.PACKAGE_SYSTEMUI;
import static tk.wasdennnoch.androidn_ify.systemui.qs.QSTileHostHooks.KEY_QUICKQS_TILEVIEW;

public class QuickQSPanel extends LinearLayout {

    private static final String TAG = "QuickQSPanel";

    private final int mIconSizePx;
    private final int mQuickTilePadding;

    private final int mMaxTiles;
    private final HeaderTileLayout mTileLayout;
    private final ResourceUtils mRes;
    protected final ArrayList<Object> mRecords = new ArrayList<>();
    private final ArrayList<View> mIconViews = new ArrayList<>();
    private final boolean mShowPercent;

    public QuickQSPanel(Context context) {
        super(context);
        ConfigUtils config = ConfigUtils.getInstance();
        Resources res = context.getResources();
        mIconSizePx = res.getDimensionPixelSize(res.getIdentifier("qs_tile_icon_size", "dimen", PACKAGE_SYSTEMUI));
        mRes = ResourceUtils.getInstance(context);
        mQuickTilePadding = mRes.getDimensionPixelSize(R.dimen.qs_quick_tile_padding);
        mMaxTiles = config.qs.qs_tiles_count;
        mShowPercent = config.qs.battery_tile_show_percentage;
        setOrientation(VERTICAL);
        int m = mRes.getDimensionPixelSize(R.dimen.qs_quick_panel_margin_horizontal);
        setPadding(m, mRes.getDimensionPixelSize(R.dimen.qs_quick_panel_padding_top), m, mRes.getDimensionPixelSize(R.dimen.qs_quick_panel_padding_bottom));
        mTileLayout = new HeaderTileLayout(context);
        addView(mTileLayout);
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

        for (int i = 0; i < tileRecords.size(); i++) {
            Object tilerecord = tileRecords.get(i);
            mRecords.add(tilerecord);
            if (i < mMaxTiles)
                mTileLayout.addTile(tilerecord);
        }
        if (StatusBarHeaderHooks.mQsAnimator == null)
            StatusBarHeaderHooks.createQsAnimator();
        StatusBarHeaderHooks.postSetupAnimators();
    }

    public void handleStateChanged(Object qsTile, Object state) {
        ViewGroup tileView = (ViewGroup) XposedHelpers.getAdditionalInstanceField(qsTile, KEY_QUICKQS_TILEVIEW);
        if (tileView != null) {
            XposedHelpers.callMethod(tileView, "onStateChanged", state);
        }
    }

    public List<Object> getRecords() {
        return mRecords;
    }

    public View getTileView(int i) {
        return mIconViews.get(i);
    }

    private class HeaderTileLayout extends LinearLayout {

        public HeaderTileLayout(Context context) {
            super(context);
            setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
            setOrientation(HORIZONTAL);
            setGravity(Gravity.CENTER_VERTICAL);
            setClipChildren(false);
            setClipToPadding(false);
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
                try {
                    XposedHelpers.callMethod(tileView, "setDual", false, false);
                } catch (Throwable ignore) {
                    // Other ROMs
                }
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
                                BatteryTile.BatteryView batteryView = (BatteryTile.BatteryView) frameChild;
                                batteryView.setShowPercent(true);
                            }
                        }
                    }
                } else {
                    child.setVisibility(GONE);
                }
            }

            XposedHelpers.setAdditionalInstanceField(tile, KEY_QUICKQS_TILEVIEW, tileView);

            if (getChildCount() != 0) {
                // Add a spacer.
                addView(new Space(getContext()), getChildCount(), generateSpaceParams());
            }
            if (iconView != null) {
                ((ViewGroup) iconView.getParent()).removeView(iconView);
                addViewToLayout(iconView, getChildCount(), click, longClick);
            } else {
                addView(tileView, getChildCount(), generateOriginalLayoutParams());
            }
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

        @SuppressWarnings("WeakerAccess")
        private class GlobalLayoutListener {
            private final View mView;

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

    }

}
