package tk.wasdennnoch.androidn_ify.systemui.qs;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.systemui.SystemUIHooks;
import tk.wasdennnoch.androidn_ify.utils.ResourceUtils;

public class TileLayout extends ViewGroup implements QuickSettingsHooks.QSTileLayout {
    protected final Context mContext;

    protected int mColumns;
    protected int mCellWidth;
    protected int mCellHeight;
    protected int mCellMargin;

    protected final ArrayList<Object> mRecords = new ArrayList<>();
    private int mCellMarginTop;

    public TileLayout(Context context) {
        this(context, null);
    }

    public TileLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        setClipChildren(false);
        setClipToPadding(false);
        setFocusableInTouchMode(true);
        updateResources();
    }

    protected ViewGroup getTileViewFromRecord(Object record) {
        return (ViewGroup) XposedHelpers.getObjectField(record, "tileView");
    }

    @Override
    public int getOffsetTop(Object tile) {
        return getTop();
    }

    public void addTile(Object tile) {
        mRecords.add(tile);
        View tileView = getTileViewFromRecord(tile);
        if (tileView.getParent() != null)
            ((ViewGroup) tileView.getParent()).removeView(tileView);
        addView(tileView);
    }

    @Override
    public void removeTile(Object tile) {
        mRecords.remove(tile);
        removeView(getTileViewFromRecord(tile));
    }

    public void removeAllViews() {
        mRecords.clear();
        super.removeAllViews();
    }

    public boolean updateResources() {
        final Resources res = ResourceUtils.getInstance(mContext).getResources();
        final int columns = Math.max(1, res.getInteger(R.integer.quick_settings_num_columns));
        mCellHeight = res.getDimensionPixelSize(R.dimen.qs_tile_height);
        mCellMargin = res.getDimensionPixelSize(R.dimen.qs_tile_margin);
        mCellMarginTop = res.getDimensionPixelSize(R.dimen.qs_tile_margin_top);
        if (mColumns != columns) {
            mColumns = columns;
            requestLayout();
            return true;
        }
        return false;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int numTiles = mRecords.size();
        final int width = MeasureSpec.getSize(widthMeasureSpec);
        final int rows = (numTiles + mColumns - 1) / mColumns;
        mCellWidth = (width - (mCellMargin * (mColumns + 1))) / mColumns;

        for (Object record : mRecords) {
            View tileView = getTileViewFromRecord(record);
            try {
                XposedHelpers.callMethod(tileView, "setDual", false);
            } catch (Throwable t) { // CM13
                XposedHelpers.callMethod(tileView, "setDual", false, false);
            }
            if (tileView.getVisibility() == GONE) continue;
            tileView.measure(exactly(mCellWidth), exactly(mCellHeight));
        }
        setMeasuredDimension(width,
                (mCellHeight + mCellMargin) * rows + (mCellMarginTop - mCellMargin));
    }

    private static int exactly(int size) {
        return MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int w = getWidth();
        boolean isRtl = getLayoutDirection() == LAYOUT_DIRECTION_RTL;
        int row = 0;
        int column = 0;
        for (int i = 0; i < mRecords.size(); i++, column++) {
            if (column == mColumns) {
                row++;
                column -= mColumns;
            }
            Object record = mRecords.get(i);
            int left = getColumnStart(column);
            final int top = getRowTop(row);
            int right;
            if (isRtl) {
                right = w - left;
                left = right - mCellWidth;
            } else {
                right = left + mCellWidth;
            }
            View tileView = getTileViewFromRecord(record);
            tileView.layout(left, top, right, top + tileView.getMeasuredHeight());
        }
    }

    private int getRowTop(int row) {
        return row * (mCellHeight + mCellMargin) + mCellMarginTop;
    }

    private int getColumnStart(int column) {
        return column * (mCellWidth + mCellMargin) + mCellMargin;
    }
}