package tk.wasdennnoch.androidn_ify.notifications.qs;

import android.content.Context;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.notifications.StatusBarHeaderHooks;

public class TileAdapter extends RecyclerView.Adapter<TileAdapter.TileViewHolder> {

    private static final String PACKAGE_SYSTEMUI = XposedHook.PACKAGE_SYSTEMUI;
    private static final float TILE_ASPECT = 1.2f;

    public static final String TAG = "TileAdapter";
    private ArrayList<Object> mRecords;
    private ViewGroup mQsPanel;
    private ArrayList<ViewGroup> mTileViews;
    private Context mContext;
    private int mCellHeight;
    private int mCellWidth;
    private StatusBarHeaderHooks.OnStartDragListener mOnStartDragListener;

    public TileAdapter(ArrayList<Object> records, Context context, ViewGroup qsPanel) {
        mRecords = records;
        mContext = context;
        mQsPanel = qsPanel;
        mTileViews = new ArrayList<>();

        mCellHeight = mContext.getResources().getDimensionPixelSize(mContext.getResources().getIdentifier("qs_tile_height", "dimen", PACKAGE_SYSTEMUI));
        mCellWidth = (int)(mCellHeight * TILE_ASPECT);

        for (int i = 0; i < records.size(); i++) {
            Object tilerecord = mRecords.get(i);
            final Object tile = XposedHelpers.getObjectField(tilerecord, "tile");
            RelativeLayout.LayoutParams tileViewLp = new RelativeLayout.LayoutParams(mCellWidth, mCellHeight);
            tileViewLp.addRule(RelativeLayout.CENTER_IN_PARENT);
            ViewGroup tileView = (ViewGroup) XposedHelpers.callMethod(tile, "createTileView", mContext);
            tileView.setLayoutParams(tileViewLp);
            try {
                XposedHelpers.callMethod(tileView, "setDual", false);
            } catch (Throwable t) {
                // CM13
                XposedHelpers.callMethod(tileView, "setDual", false, false);
            }
            XposedHelpers.callMethod(tileView, "onStateChanged", XposedHelpers.callMethod(tile, "getState"));
            mTileViews.add(i, tileView);
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
            }
        }
    }

    private int getWidth() {
        return mQsPanel.getWidth() / 3;
    }

    private GridLayoutManager.LayoutParams generateLayoutParams() {
        int i = getWidth();
        return new GridLayoutManager.LayoutParams(i, i);
    }

    public void setOnStartDragListener(StatusBarHeaderHooks.OnStartDragListener onStartDragListener) {
        mOnStartDragListener = onStartDragListener;
    }

    @Override
    public TileViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = new RelativeLayout(parent.getContext());
        itemView.setLayoutParams(generateLayoutParams());
        itemView.setBackgroundColor(0xFF0000);
        return new TileViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final TileViewHolder holder, int position) {
        ViewGroup tileView = mTileViews.get(position);
        if (tileView.getParent() != null)
            ((ViewGroup) tileView.getParent()).removeView(tileView);
        holder.mItemView.removeAllViews();
        holder.mItemView.addView(tileView);
    }

    @Override
    public int getItemCount() {
        return mTileViews.size();
    }

    public boolean onItemMove(int fromPosition, int toPosition) {
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(mTileViews, i, i + 1);
                Collections.swap(mRecords, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(mTileViews, i, i - 1);
                Collections.swap(mRecords, i, i - 1);
            }
        }
        notifyItemMoved(fromPosition, toPosition);
        return true;
    }

    protected class TileViewHolder extends RecyclerView.ViewHolder {

        protected RelativeLayout mItemView;

        public TileViewHolder(View itemView) {
            super(itemView);

            mItemView = (RelativeLayout) itemView;
        }
    }

    public boolean saveChanges() {
        List<String> tileSpecs = new ArrayList<>();
        for (int i = 0; i < mRecords.size(); i++) {
            Object tilerecord = mRecords.get(i);
            Object tile = XposedHelpers.getObjectField(tilerecord, "tile");
            tileSpecs.add((String) XposedHelpers.getAdditionalInstanceField(tile, QSTileHostHooks.TILE_SPEC_NAME));
        }
        if (!QSTileHostHooks.mTileSpecs.equals(tileSpecs)) {
            QSTileHostHooks.saveTileSpecs(mContext, tileSpecs);
            return true;
        } else {
            return false;
        }
    }
}
