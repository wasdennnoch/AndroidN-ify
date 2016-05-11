package tk.wasdennnoch.androidn_ify.notifications;

import android.content.Context;
import android.content.res.Configuration;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Space;

import java.util.ArrayList;

import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.utils.ConfigUtils;
import tk.wasdennnoch.androidn_ify.utils.ResourceUtils;

public class QuickQSPanel extends LinearLayout {

    private static final String TAG = "QuickQSPanel";

    private int mMaxTiles;
    protected HeaderTileLayout mTileLayout;
    private ResourceUtils res;
    private ArrayList<Object> mRecords = new ArrayList<>();
    private ArrayList<ViewGroup> mTileViews = new ArrayList<>();

    public QuickQSPanel(Context context) {
        super(context);
        ConfigUtils config = ConfigUtils.getInstance();
        config.reload();
        res = ResourceUtils.getInstance(context);
        mMaxTiles = config.header.qs_tiles_count;
        setOrientation(VERTICAL);
        setPadding(0, res.getDimensionPixelSize(R.dimen.qs_quick_panel_padding_top), 0, res.getDimensionPixelSize(R.dimen.qs_quick_panel_padding_bottom));
        mTileLayout = new HeaderTileLayout(context);
        addView(mTileLayout);
    }

    public void setTiles(ArrayList<Object> tileRecords) {
        XposedHook.logD(TAG, "setTiles tile record count: " + tileRecords.size());
        mTileLayout.removeTiles();
        mTileViews.clear();
        mRecords.clear();
        for (int i = 0; i < mMaxTiles && i < tileRecords.size(); i++) {
            Object tilerecord = tileRecords.get(i);
            mRecords.add(tilerecord);
            mTileLayout.addTile(tilerecord);
        }
    }

    public void handleStateChanged(Object qstile, Object state) {
        for (int i = 0; i < mRecords.size(); i++) {
            Object tilerecord = mRecords.get(i);
            Object tile = XposedHelpers.getObjectField(tilerecord, "tile");
            if (tile == qstile) {
                if (i >= mTileViews.size()) {
                    XposedHook.logD(TAG, "handleStateChanged; tilerecord index greater than or equals to tileViews size; index :" + i + "; views: " + mTileViews.size());
                    return;
                }
                ViewGroup tileView = mTileViews.get(i);
                XposedHelpers.callMethod(tileView, "onStateChanged", state);
                XposedHook.logD(TAG, "handleStateChanged #" + i); // Spam
                /*View iconView = (View) XposedHelpers.getObjectField(tileView, "mIcon");
                if (iconView instanceof ImageView) {
                    Drawable icon = ((ImageView) iconView).getDrawable();
                    if (icon instanceof Animatable) {
                        ((Animatable) icon).start();
                    }
                }*/
            }
        }
    }

    private class HeaderTileLayout extends LinearLayout {

        private final Space mEndSpacer;

        public HeaderTileLayout(Context context) {
            super(context);
            setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
            setOrientation(HORIZONTAL);
            setGravity(16);
            setClipChildren(false);
            setClipToPadding(false);
            mEndSpacer = new Space(context);
            mEndSpacer.setLayoutParams(generateLayoutParams());
            updateDownArrowMargin();
            addView(mEndSpacer);
        }

        public void addTile(Object /*QSPanel.TileRecord*/ tilerecord) {
            XposedHook.logD(TAG, "addTile: original tileView class: " + XposedHelpers.getObjectField(tilerecord, "tileView").getClass().getSimpleName());
            final Object tile = XposedHelpers.getObjectField(tilerecord, "tile");
            ViewGroup tileView = (ViewGroup) XposedHelpers.callMethod(tile, "createTileView", getContext());
            XposedHook.logD(TAG, "addTile: generated tileView class: " + tileView.getClass().getSimpleName());

            View.OnClickListener click = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    XposedHelpers.callMethod(tile, "click");
                }
            };
            View.OnClickListener clickSecondary = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    XposedHelpers.callMethod(tile, "secondaryClick");
                }
            };
            View.OnLongClickListener longClick = new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    XposedHelpers.callMethod(tile, "longClick");
                    return true;
                }
            };
            XposedHelpers.callMethod(tileView, "init", click, clickSecondary, longClick);
            try {
                XposedHelpers.callMethod(tileView, "setDual", false);
            } catch (Throwable t) {
                // CM13
                XposedHelpers.callMethod(tileView, "setDual", false, false);
            }
            //XposedHelpers.callMethod(tileView, "handleStateChanged", XposedHelpers.callMethod(tile, "getState"));
            XposedHelpers.callMethod(tileView, "onStateChanged", XposedHelpers.callMethod(tile, "getState"));

            int children = tileView.getChildCount();
            for (int i = 0; i < children; i++) {
                View child = tileView.getChildAt(i);
                // no complex "getResources().getIdenifier("blah", "blah", "blah")"? I love it!
                // FrameLayout is the container of the signal state icons
                if (child.getId() == android.R.id.icon || child instanceof FrameLayout)
                    child.setVisibility(VISIBLE);
                else
                    child.setVisibility(GONE);
            }

            mTileViews.add(tileView);
            XposedHook.logD(TAG, "addTile: adding tile at #" + (getChildCount() - 1));
            addView(tileView, getChildCount() - 1, generateLayoutParams());
            addView(new Space(getContext()), getChildCount() - 1, generateSpaceParams());
        }

        public void removeTiles() {
            XposedHook.logD(TAG, "Removing all tiles");
            for (int i = 0; i < mMaxTiles && i < mRecords.size(); i++) {
                removeViewAt(0); // Tile
                removeViewAt(0); // Space
            }
        }

        private LayoutParams generateLayoutParams() {
            int i = res.getDimensionPixelSize(R.dimen.qs_quick_tile_size);
            LayoutParams layoutparams = new LayoutParams(i, i);
            layoutparams.gravity = Gravity.CENTER;
            return layoutparams;
        }

        private LayoutParams generateSpaceParams() {
            LayoutParams layoutparams = new LayoutParams(0, res.getDimensionPixelSize(R.dimen.qs_quick_tile_size));
            layoutparams.weight = 1.0F;
            layoutparams.gravity = Gravity.CENTER;
            return layoutparams;
        }

        private void updateDownArrowMargin() {
            LayoutParams layoutparams = (LayoutParams) mEndSpacer.getLayoutParams();
            layoutparams.setMarginStart(res.getDimensionPixelSize(R.dimen.qs_quick_tile_padding));
            mEndSpacer.setLayoutParams(layoutparams);
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
