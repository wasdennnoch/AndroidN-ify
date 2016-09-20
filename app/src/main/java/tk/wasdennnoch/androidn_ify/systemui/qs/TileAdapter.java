package tk.wasdennnoch.androidn_ify.systemui.qs;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.support.annotation.NonNull;
import android.support.v7.util.DiffUtil;
import android.support.v7.util.ListUpdateCallback;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.systemui.notifications.StatusBarHeaderHooks;
import tk.wasdennnoch.androidn_ify.utils.ConfigUtils;
import tk.wasdennnoch.androidn_ify.utils.ResourceUtils;

import static tk.wasdennnoch.androidn_ify.systemui.qs.QSTileHostHooks.KEY_EDIT_TILEVIEW;
import static tk.wasdennnoch.androidn_ify.systemui.qs.customize.QSCustomizer.MODE_EDIT_SECURE;
import static tk.wasdennnoch.androidn_ify.systemui.qs.customize.QSCustomizer.MODE_NORMAL;

@SuppressWarnings("WeakerAccess")
public class TileAdapter extends RecyclerView.Adapter<TileAdapter.TileViewHolder> {

    protected static final String PACKAGE_SYSTEMUI = XposedHook.PACKAGE_SYSTEMUI;

    public static final long MOVE_DURATION = 150;
    private static final long DRAG_LENGTH = 100;
    private static final float DRAG_SCALE = 1.2f;
    public static final float TILE_ASPECT = 1.2f;

    public static final String TAG = "TileAdapter";
    private final ItemTouchHelper mItemTouchHelper;
    private final ListUpdateCallback mTileUpdateCallback = new TileUpdateCallback();
    private List<String> mSecureTiles = new ArrayList<>();
    private List<String> mPreviousSpecs = new ArrayList<>();
    private List<String> mTileSpecs = new ArrayList<>();
    protected ArrayList<Object> mRecords = new ArrayList<>();
    protected ArrayList<ViewGroup> mTileViews = new ArrayList<>();
    protected final Context mContext;
    protected final int mCellHeight;
    protected final int mCellWidth;
    private ResourceUtils mRes;
    public int mDividerIndex;

    private int mMode = MODE_NORMAL;

    public TileAdapter.TileViewHolder mCurrentDrag;
    private boolean mIsInvalid = true;

    public TileAdapter(Context context) {
        mContext = context;
        //mQsPanel = qsPanel;

        mCellHeight = mContext.getResources().getDimensionPixelSize(mContext.getResources().getIdentifier("qs_tile_height", "dimen", PACKAGE_SYSTEMUI));
        mCellWidth = (int) (mCellHeight * TILE_ASPECT);

        ItemTouchHelper.Callback mCallbacks = new ItemTouchHelper.Callback() {

            @Override
            public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.START | ItemTouchHelper.END;
                if (mMode == MODE_EDIT_SECURE || viewHolder.getItemViewType() == 1) {
                    dragFlags = 0;
                }
                return makeMovementFlags(dragFlags, 0);
            }

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return onItemMove(viewHolder.getAdapterPosition(),
                        target.getAdapterPosition());
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            }

            @Override
            public boolean isLongPressDragEnabled() {
                return true;
            }

            @Override
            public boolean isItemViewSwipeEnabled() {
                return false;
            }

            @Override
            public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
                if (mCurrentDrag != null) {
                    mCurrentDrag.stopDrag();
                    mCurrentDrag = null;
                }
                if (viewHolder != null) {
                    mCurrentDrag = (TileViewHolder) viewHolder;
                    mCurrentDrag.startDrag();
                }
                try {
                    notifyItemChanged(mDividerIndex);
                } catch (Throwable ignore) {

                }
                super.onSelectedChanged(viewHolder, actionState);
            }

            @Override
            public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                ((TileViewHolder) viewHolder).stopDrag();
                super.clearView(recyclerView, viewHolder);
            }
        };
        mItemTouchHelper = new CustomItemTouchHelper(mCallbacks);
        XposedHelpers.setIntField(mCallbacks, "mCachedMaxScrollSpeed", ResourceUtils.getInstance(mContext).getDimensionPixelSize(R.dimen.lib_item_touch_helper_max_drag_scroll_per_frame));
    }

    private void init(ArrayList<Object> records, Context context) {
        mIsInvalid = false;

        mRes = ResourceUtils.getInstance(context);

        setRecords(records);
        mTileSpecs = convertToSpecs();
        mPreviousSpecs.addAll(mTileSpecs);
        if (ConfigUtils.M && QSTileHostHooks.mSecureTiles != null)
            mSecureTiles.addAll(QSTileHostHooks.mSecureTiles);

        addDivider();
        addAvailableTiles();

        mDividerIndex = mTileViews.indexOf(null);
    }

    public void reInit(ArrayList<Object> records, Context context) {
        mRecords.clear();
        mPreviousSpecs.clear();
        mTileSpecs.clear();
        mTileViews.clear();
        mSecureTiles.clear();
        init(records, context);
    }

    public void setMode(int mode) {
        mMode = mode;
        notifyDataSetChanged();
    }

    public int getMode() {
        return mMode;
    }

    private void addDivider() {
        mTileSpecs.add(null);
        //mRecords.add(null);
        mTileViews.add(null);
    }

    private void addAvailableTiles() {
        // TODO completely remove AvailableTileAdapter
        AvailableTileAdapter mAvailableTileAdapter = new AvailableTileAdapter(mRecords, mContext);
        int count = mAvailableTileAdapter.getItemCount();
        for (int i = 0; i < count; i++) {
            mTileSpecs.add((String) mAvailableTileAdapter.mRecords.get(i));
            mTileViews.add(mAvailableTileAdapter.mTileViews.get(i));
        }
    }

    public void setRecords(ArrayList<Object> records) {
        mTileViews.clear();
        mRecords.clear();

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
        } catch (Throwable t) {// CM13
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
        XposedHelpers.setAdditionalInstanceField(tile, KEY_EDIT_TILEVIEW, tileView);
        mTileViews.add(i, tileView);
    }

    private int getWidth() {
        return StatusBarHeaderHooks.mQsPanel.getWidth() / 3;
    }

    private GridLayoutManager.LayoutParams generateLayoutParams() {
        int i = getWidth();
        return new GridLayoutManager.LayoutParams(i, (int) (getWidth() / TILE_ASPECT));
    }

    @Override
    public TileViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        View itemView;
        if (viewType == 1) {
            ResourceUtils res = ResourceUtils.getInstance(context);
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            itemView = inflater.inflate(res.getLayout(R.layout.qs_customize_divider), parent, false);
        } else {
            itemView = new RelativeLayout(parent.getContext());
            itemView.setLayoutParams(generateLayoutParams());
        }
        return new TileViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(TileViewHolder holder, int position) {
        if (holder.getItemViewType() == 1) {
            TextView textView = (TextView) holder.itemView.findViewById(android.R.id.title);
            int textId;
            if (mCurrentDrag == null) {
                textId = R.string.drag_to_add_tiles;
            } else {
                textId = R.string.drag_to_remove_tiles;
            }
            textView.setText(mRes.getString(textId));
        } else {
            ViewGroup tileView = mTileViews.get(position);
            if (tileView.getParent() != null)
                ((ViewGroup) tileView.getParent()).removeView(tileView);
            holder.setTileView(tileView);
            holder.setBackgroundColor(mMode == MODE_EDIT_SECURE && mSecureTiles.contains(mTileSpecs.get(position)) ? 0x30FFFFFF : 0);
        }
    }

    @Override
    public int getItemCount() {
        return (mMode == MODE_EDIT_SECURE) ? mDividerIndex : mTileViews.size();
    }

    @Override
    public int getItemViewType(int i) {
        return mTileViews.get(i) != null ? 0 : 1;
    }

    public RecyclerView.ItemDecoration getItemDecoration() {
        return mDecoration;
    }

    public GridLayoutManager.SpanSizeLookup getSizeLookup() {
        return mSizeLookup;
    }

    public boolean onItemMove(int fromPosition, int toPosition) {
        if (mMode == MODE_EDIT_SECURE) return false;
        if (fromPosition > mDividerIndex && toPosition > mDividerIndex)
            return false;
        if (fromPosition < mDividerIndex && mDividerIndex < 2)
            return false;
        move(fromPosition, toPosition, mTileViews);
        move(fromPosition, toPosition, mTileSpecs);
        mDividerIndex = mTileViews.indexOf(null);
        notifyItemMoved(fromPosition, toPosition);
        return true;
    }

    @SuppressWarnings("unchecked")
    private void move(int from, int to, List list) {
        int addIndex;
        if (from > to) {
            addIndex = to;
        } else {
            addIndex = to + 1;
        }
        list.add(addIndex, list.get(from));
        int removeIndex = from;
        if (from > to) {
            removeIndex = from + 1;
        }
        list.remove(removeIndex);
    }

    private void addTile(int position) {
        onItemMove(position, mDividerIndex);
    }

    private void removeTile(int position) {
        onItemMove(position, mDividerIndex);
    }

    public ItemTouchHelper getItemTouchHelper() {
        return mItemTouchHelper;
    }

    public void invalidate() {
        mIsInvalid = true;
    }

    public boolean isInvalid() {
        return mIsInvalid;
    }

    public class TileViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        protected RelativeLayout mItemView;
        protected TextView mTextView;

        public TileViewHolder(View itemView) {
            super(itemView);

            if (itemView instanceof RelativeLayout)
                mItemView = (RelativeLayout) itemView;
            else if (itemView instanceof TextView)
                mTextView = (TextView) itemView;
        }

        private void setTileView(ViewGroup tileView) {
            if (mItemView == null) return;
            mItemView.removeAllViews();
            mItemView.addView(tileView);
            tileView.setClickable(true);
            tileView.setOnClickListener(this);
        }

        public void startDrag() {
            if (mItemView == null) return;
            mItemView.animate().setDuration(DRAG_LENGTH).scaleX(DRAG_SCALE).scaleY(DRAG_SCALE);
            try {
                ((View) XposedHelpers.callMethod(mItemView.getChildAt(0), "labelView")).animate().setDuration(100L).alpha(0.0F);
            } catch (Throwable ignore) {
            }
        }

        public void stopDrag() {
            if (mItemView == null) return;
            mItemView.animate().setDuration(DRAG_LENGTH).scaleX(1.0F).scaleY(1.0F);
            try {
                ((View) XposedHelpers.callMethod(mItemView.getChildAt(0), "labelView")).animate().setDuration(100L).alpha(1.0F);
            } catch (Throwable ignore) {
            }
        }

        @Override
        public void onClick(View v) {
            int position = getAdapterPosition();
            if (mMode == MODE_EDIT_SECURE) {
                String spec = mTileSpecs.get(position);
                if (mSecureTiles.contains(spec))
                    mSecureTiles.remove(spec);
                else
                    mSecureTiles.add(spec);
                notifyItemChanged(position);
            } else {
                if (position < mDividerIndex)
                    removeTile(position);
                else
                    addTile(position);
            }
        }

        public void setBackgroundColor(int color) {
            mItemView.setBackgroundColor(color);
        }
    }

    public void saveChanges() {
        saveSecureTiles();
        saveTiles(getAddedTileSpecs(), false);
    }

    public void resetTiles() {
        mSecureTiles.clear();
        saveSecureTiles();
        List<String> newSpces = new ArrayList<>();
        newSpces.add("wifi");
        newSpces.add("cell");
        newSpces.add("battery");
        newSpces.add("dnd");
        newSpces.add("flashlight");
        newSpces.add("rotation");
        newSpces.add("bt");
        newSpces.add("airplane");
        newSpces.add("location");
        saveTiles(newSpces, true);
    }

    private void saveTiles(List<String> tileSpecs, boolean update) {
        XposedHook.logD(TAG, "saveTiles called");
        List<String> oldTiles = update ? getAddedTileSpecs() : QSTileHostHooks.mTileSpecs;
        if (oldTiles == null || !oldTiles.equals(tileSpecs)) {
            if (!compareSpecs(tileSpecs)) invalidate();
            QSTileHostHooks.saveTileSpecs(mContext, tileSpecs);
            if (update) {
                QSTileHostHooks.mTileSpecs = tileSpecs;
                calcDiff(tileSpecs).dispatchUpdatesTo(mTileUpdateCallback);
            }
            return;
        }
        XposedHook.logD(TAG, "saveTiles: No changes to save");
    }

    private void saveSecureTiles() {
        if (!ConfigUtils.M) return;
        XposedHook.logD(TAG, "saveSecureTiles called");
        if (!QSTileHostHooks.mSecureTiles.equals(mSecureTiles)) {
            QSTileHostHooks.saveSecureTileSpecs(mContext, mSecureTiles);
        }
        XposedHook.logD(TAG, "saveSecureTiles: No changes to save");
    }

    private boolean compareSpecs(List<String> newSpecs) {
        if (mPreviousSpecs == null || newSpecs == null) return false;
        int oldSize = mPreviousSpecs.size();
        int newSize = newSpecs.size();
        if (oldSize != newSize) return false;
        if (mPreviousSpecs.equals(newSpecs)) return true;
        for (String spec : mPreviousSpecs)
            if (!newSpecs.contains(spec))
                return false;
        return true;
    }

    private DiffUtil.DiffResult calcDiff(List<String> specs) {
        List<String> newSpecs = new ArrayList<>();
        newSpecs.addAll(specs);
        newSpecs.add(null);
        for (String spec : mTileSpecs) {
            if (spec == null) continue;
            if (newSpecs.contains(spec)) continue;
            newSpecs.add(spec);
        }
        return DiffUtil.calculateDiff(new TileSpecDiffCallback(mTileSpecs, newSpecs));
    }

    public List<String> getAddedTileSpecs() {
        List<String> specs = new ArrayList<>();
        for (String spec : mTileSpecs) {
            if (spec == null) return specs;
            specs.add(spec);
        }
        return specs;
    }

    @NonNull
    private List<String> convertToSpecs() {
        List<String> tileSpecs = new ArrayList<>();
        for (int i = 0; i < mRecords.size(); i++) {
            Object tilerecord = mRecords.get(i);
            if (tilerecord == null) return tileSpecs;
            Object tile = XposedHelpers.getObjectField(tilerecord, "tile");
            tileSpecs.add((String) XposedHelpers.getAdditionalInstanceField(tile, QSTileHostHooks.TILE_SPEC_NAME));
        }
        return tileSpecs;
    }

    public void handleStateChanged(Object qsTile, Object state) {
        ViewGroup tileView = (ViewGroup) XposedHelpers.getAdditionalInstanceField(qsTile, KEY_EDIT_TILEVIEW);
        if (tileView != null) {
            XposedHelpers.callMethod(tileView, "onStateChanged", state);
        }
    }

    private final GridLayoutManager.SpanSizeLookup mSizeLookup = new GridLayoutManager.SpanSizeLookup() {

        public int getSpanSize(int i) {
            return (getItemViewType(i) == 1) ? 3 : 1;
        }
    };

    private final RecyclerView.ItemDecoration mDecoration = new RecyclerView.ItemDecoration() {
        private final ColorDrawable mDrawable = new ColorDrawable(0xff384248);

        @Override
        public void onDraw(Canvas canvas, RecyclerView recyclerview, RecyclerView.State state) {
            int count = recyclerview.getChildCount();
            View child;
            for (int i = 0; i < count; i++) {
                child = recyclerview.getChildAt(i);
                if (recyclerview.getChildViewHolder(child).getAdapterPosition() >= mDividerIndex) {
                    RecyclerView.LayoutParams layoutParams = (RecyclerView.LayoutParams) child.getLayoutParams();
                    int childTop = child.getTop();
                    int topMargin = layoutParams.topMargin;
                    int childTranslationY = Math.round(child.getTranslationY());
                    mDrawable.setBounds(0, childTop + topMargin + childTranslationY, recyclerview.getWidth(), recyclerview.getBottom());
                    mDrawable.draw(canvas);
                }
            }
        }
    };

    private class CustomItemTouchHelper extends ItemTouchHelper {

        public CustomItemTouchHelper(Callback callback) {
            super(callback);
        }

        @Override
        public void attachToRecyclerView(RecyclerView recyclerView) {
            try {
                RecyclerView oldRecyclerView = (RecyclerView) XposedHelpers.getObjectField(this, "mRecyclerView");
                if (oldRecyclerView == recyclerView) {
                    return; // nothing to do
                }
                if (oldRecyclerView != null) {
                    XposedHelpers.findMethodBestMatch(ItemTouchHelper.class, "destroyCallbacks").invoke(this);
                }
                XposedHelpers.setObjectField(this, "mRecyclerView", recyclerView);
                if (recyclerView != null) {
                    XposedHelpers.findMethodBestMatch(ItemTouchHelper.class, "setupCallbacks").invoke(this);
                }
            } catch (Throwable t) {
                XposedHook.logE(TAG, "Error attaching ItemTouchCallback to RecyclerView", t);
            }
        }
    }

    private class TileSpecDiffCallback extends DiffUtil.Callback {
        private List<String> mOldSpecs;
        private List<String> mNewSpecs;

        public TileSpecDiffCallback(List<String> oldSpecs, List<String> newSpecs) {
            mOldSpecs = oldSpecs;
            mNewSpecs = newSpecs;
        }

        @Override
        public int getOldListSize() {
            return mOldSpecs.size();
        }

        @Override
        public int getNewListSize() {
            return mNewSpecs.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            Object oldItem = mOldSpecs.get(oldItemPosition);
            Object newItem = mNewSpecs.get(newItemPosition);
            if (oldItem == null || newItem == null)
                return oldItem == newItem;
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            return areItemsTheSame(oldItemPosition, newItemPosition);
        }
    }

    private class TileUpdateCallback implements ListUpdateCallback {

        @Override
        public void onInserted(int position, int count) {
            XposedHook.logW(TAG, "TileUpdateCallback: onInserted called with position=" + position + " and count=" + count);
        }

        @Override
        public void onRemoved(int position, int count) {
            XposedHook.logW(TAG, "TileUpdateCallback: onRemoved called with position=" + position + " and count=" + count);
        }

        @Override
        public void onMoved(int fromPosition, int toPosition) {
            onItemMove(fromPosition, toPosition);
        }

        @Override
        public void onChanged(int position, int count, Object payload) {
            XposedHook.logW(TAG, "TileUpdateCallback: onChanged called with position=" + position + ", count=" + count + " and payload=" + payload);
        }
    }

}
