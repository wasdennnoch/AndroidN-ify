package tk.wasdennnoch.androidn_ify.notifications.qs;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.notifications.StatusBarHeaderHooks;

public class TileAdapter extends RecyclerView.Adapter<TileAdapter.TileViewHolder> {

    protected static final String PACKAGE_SYSTEMUI = XposedHook.PACKAGE_SYSTEMUI;
    public static final float TILE_ASPECT = 1.2f;

    public static final String TAG = "TileAdapter";
    protected ArrayList<Object> mRecords;
    protected ViewGroup mQsPanel;
    protected ArrayList<ViewGroup> mTileViews;
    protected Context mContext;
    protected int mCellHeight;
    protected int mCellWidth;
    private StatusBarHeaderHooks.OnStartDragListener mOnStartDragListener;

    public TileAdapter(Context context, ViewGroup qsPanel) {
        mContext = context;
        mQsPanel = qsPanel;

        mCellHeight = mContext.getResources().getDimensionPixelSize(mContext.getResources().getIdentifier("qs_tile_height", "dimen", PACKAGE_SYSTEMUI));
        mCellWidth = (int)(mCellHeight * TILE_ASPECT);
    }

    public TileAdapter(ArrayList<Object> records, Context context, ViewGroup qsPanel) {
        this(context, qsPanel);
        mTileViews = new ArrayList<>();

        setRecords(records);
    }

    public void setRecords(ArrayList<Object> records) {
        mTileViews = new ArrayList<>();
        mRecords = new ArrayList<>();

        for (int i = 0; i < records.size(); i++) {
            Object tilerecord = records.get(i);
            final Object tile = XposedHelpers.getObjectField(tilerecord, "tile");
            addTile(i, tile);
            mRecords.add(tilerecord);
        }
    }

    private void addTile(int i, Object tile) {
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
        return new TileViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final TileViewHolder holder, int position) {
        ViewGroup tileView = mTileViews.get(position);
        if (tileView.getParent() != null)
            ((ViewGroup) tileView.getParent()).removeView(tileView);
        holder.setTileView(tileView);
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

    protected class TileViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        protected RelativeLayout mItemView;

        public TileViewHolder(View itemView) {
            super(itemView);

            mItemView = (RelativeLayout) itemView;
        }

        private void setTileView(ViewGroup tileView) {
            mItemView.removeAllViews();
            mItemView.addView(tileView);
            tileView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            onItemClick(getAdapterPosition());
        }
    }

    public void onItemClick(int position) {
        Object tilerecord = mRecords.get(position);
        String spec;
        if (tilerecord instanceof Record) {
            spec = ((Record) tilerecord).spec;
        } else {
            Object tile = XposedHelpers.getObjectField(tilerecord, "tile");
            spec = (String) XposedHelpers.getAdditionalInstanceField(tile, QSTileHostHooks.TILE_SPEC_NAME);
        }
        StatusBarHeaderHooks.mAvailableTileAdapter.addAdditionalSpec(spec);

        mRecords.remove(position);
        mTileViews.remove(position);
        notifyItemRemoved(position);
    }

    public void addRecord(Record record) {
        /*
        int addPosition = mTileViews.size();

        Object tile = QSTileHostHooks.createTile(QSTileHostHooks.mTileHost, record.spec);
        XposedHelpers.callMethod(tile, "refreshState");
        addTile(addPosition, tile);
        mRecords.add(record);
        notifyItemInserted(addPosition);
        */

        List<String> tileSpecs = convertToSpecs();
        tileSpecs.add(tileSpecs.size(), record.spec);
        if (!QSTileHostHooks.mTileSpecs.equals(tileSpecs)) {
            QSTileHostHooks.saveTileSpecs(mContext, tileSpecs);
            QSTileHostHooks.recreateTiles();
        }
    }

    public boolean saveChanges() {
        List<String> tileSpecs = convertToSpecs();
        if (!QSTileHostHooks.mTileSpecs.equals(tileSpecs)) {
            QSTileHostHooks.saveTileSpecs(mContext, tileSpecs);
            return true;
        } else {
            return false;
        }
    }

    @NonNull
    private List<String> convertToSpecs() {
        List<String> tileSpecs = new ArrayList<>();
        for (int i = 0; i < mRecords.size(); i++) {
            Object tilerecord = mRecords.get(i);
            if (tilerecord instanceof Record) {
                tileSpecs.add(((Record) tilerecord).spec);
            } else {
                Object tile = XposedHelpers.getObjectField(tilerecord, "tile");
                tileSpecs.add((String) XposedHelpers.getAdditionalInstanceField(tile, QSTileHostHooks.TILE_SPEC_NAME));
            }
        }
        return tileSpecs;
    }

    public class Record {
        public String spec;
    }
}
