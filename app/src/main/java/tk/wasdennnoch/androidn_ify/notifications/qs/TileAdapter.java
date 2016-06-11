package tk.wasdennnoch.androidn_ify.notifications.qs;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
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
import tk.wasdennnoch.androidn_ify.notifications.StatusBarHeaderHooks;
import tk.wasdennnoch.androidn_ify.utils.ResourceUtils;

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
    private List<String> mTileSpecs;
    private StatusBarHeaderHooks.TileTouchCallback mTileTouchCallback;
    private ResourceUtils mRes;
    public int mDividerIndex;

    public TileAdapter(Context context, ViewGroup qsPanel) {
        mContext = context;
        mQsPanel = qsPanel;

        mCellHeight = mContext.getResources().getDimensionPixelSize(mContext.getResources().getIdentifier("qs_tile_height", "dimen", PACKAGE_SYSTEMUI));
        mCellWidth = (int)(mCellHeight * TILE_ASPECT);
    }

    public TileAdapter(ArrayList<Object> records, Context context, ViewGroup qsPanel) {
        this(context, qsPanel);
        mRes = ResourceUtils.getInstance(context);
        mTileViews = new ArrayList<>();

        setRecords(records);
        mTileSpecs = convertToSpecs();

        addDivider();
        addAvailableTiles();

        mDividerIndex = mTileViews.indexOf(null);
    }

    private void addDivider() {
        mTileSpecs.add(null);
        //mRecords.add(null);
        mTileViews.add(null);
    }

    private void addAvailableTiles() {
        // TODO completely remove AvailableTileAdapter
        AvailableTileAdapter mAvailableTileAdapter = new AvailableTileAdapter(mRecords, mContext, mQsPanel);
        int count = mAvailableTileAdapter.getItemCount();
        for (int i = 0; i < count; i++) {
            mTileSpecs.add((String) mAvailableTileAdapter.mRecords.get(i));
            mTileViews.add(mAvailableTileAdapter.mTileViews.get(i));
        }
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

    @Override
    public TileViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        View itemView;
        if (viewType == 1) {
            ResourceUtils res = ResourceUtils.getInstance(context);
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            itemView = inflater.inflate(res.getLayout(R.layout.qs_customize_divider), null);
        } else {
            itemView = new RelativeLayout(parent.getContext());
            itemView.setLayoutParams(generateLayoutParams());
        }
        return new TileViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final TileViewHolder holder, int position) {
        if (holder.getItemViewType() == 1) {
            TextView textView = (TextView) holder.itemView.findViewById(android.R.id.title);
            int textId;
            if (mTileTouchCallback.mCurrentDrag == null) {
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
        }
    }

    @Override
    public int getItemCount() {
        return mTileViews.size();
    }

    @Override
    public int getItemViewType(int i)
    {
        return mTileViews.get(i) != null ? 0 : 1;
    }

    public RecyclerView.ItemDecoration getItemDecoration()
    {
        return mDecoration;
    }

    public GridLayoutManager.SpanSizeLookup getSizeLookup() {
        return mSizeLookup;
    }

    public boolean onItemMove(int fromPosition, int toPosition) {
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
    private void move(int i, int j, List list) {
        int k;
        if (i > j) {
            k = j;
        } else {
            k = j + 1;
        }
        list.add(k, list.get(i));
        k = i;
        if (i > j) {
            k = i + 1;
        }
        list.remove(k);
    }

    public void setTileTouchCallback(StatusBarHeaderHooks.TileTouchCallback tileTouchCallback) {
        mTileTouchCallback = tileTouchCallback;
    }

    private void addTile(int position) {
        onItemMove(position, mDividerIndex);
    }

    private void removeTile(int position) {
        onItemMove(position, mDividerIndex);
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
            itemView.animate().setDuration(100L).scaleX(1.2F).scaleY(1.2F);
            try {
                ((View) XposedHelpers.callMethod(((RelativeLayout) itemView).getChildAt(0), "labelView")).animate().setDuration(100L).alpha(0.0F);
            } catch (Throwable ignore) {

            }
        }

        public void stopDrag() {
            if (mItemView == null) return;
            itemView.animate().setDuration(100L).scaleX(1.0F).scaleY(1.0F);
            try {
                ((View) XposedHelpers.callMethod(((RelativeLayout) itemView).getChildAt(0), "labelView")).animate().setDuration(100L).alpha(1.0F);
            } catch (Throwable ignore) {

            }
        }

        @Override
        public void onClick(View v) {
            int position = getAdapterPosition();
            if (position < mDividerIndex)
                removeTile(position);
            else
                addTile(position);
        }
    }

    public boolean saveChanges() {
        List<String> tileSpecs = getAddedTileSpecs();
        if (!QSTileHostHooks.mTileSpecs.equals(tileSpecs)) {
            QSTileHostHooks.saveTileSpecs(mContext, tileSpecs);
            return true;
        } else {
            return false;
        }
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
            if (tilerecord instanceof Record) {
                tileSpecs.add(((Record) tilerecord).spec);
            } else {
                Object tile = XposedHelpers.getObjectField(tilerecord, "tile");
                tileSpecs.add((String) XposedHelpers.getAdditionalInstanceField(tile, QSTileHostHooks.TILE_SPEC_NAME));
            }
        }
        return tileSpecs;
    }

    private final GridLayoutManager.SpanSizeLookup mSizeLookup = new GridLayoutManager.SpanSizeLookup() {

        public int getSpanSize(int i) {
            return (getItemViewType(i) == 1) ? 3 : 1;
        }
    };

    private final RecyclerView.ItemDecoration mDecoration = new RecyclerView.ItemDecoration() {
        private final ColorDrawable mDrawable = new ColorDrawable(0xff384248);

        public void onDraw(Canvas canvas, RecyclerView recyclerview, RecyclerView.State state) {
            label0: {
                super.onDraw(canvas, recyclerview, state);
                int l = recyclerview.getChildCount();
                int j = recyclerview.getWidth();
                int k = recyclerview.getBottom();
                int i = 0;
                View child;
                do {
                    if (i >= l) {
                        break label0;
                    }
                    child = recyclerview.getChildAt(i);
                    if (recyclerview.getChildViewHolder(child).getAdapterPosition() >= mDividerIndex) {
                        break;
                    }
                    i++;
                } while (true);
                RecyclerView.LayoutParams layoutParams = (RecyclerView.LayoutParams) child.getLayoutParams();
                i = child.getTop();
                l = layoutParams.topMargin;
                int i1 = Math.round(ViewCompat.getTranslationY(child));
                mDrawable.setBounds(0, i + l + i1, j, k);
                mDrawable.draw(canvas);
            }
        }
    };

    public class Record {
        public String spec;

        public Record(String s) {
            spec = s;
        }
    }
}
