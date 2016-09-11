package tk.wasdennnoch.androidn_ify.systemui.qs;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.utils.ResourceUtils;

public class TileLayout extends ViewGroup implements QuickSettingsHooks.QSTileLayout {
    protected final Context mContext;

    protected int mColumns;
    protected int mCellWidth;
    protected int mCellHeight;
    protected int mLargeCellWidth;
    protected int mCellMargin;
    protected boolean mFirstRowLarge = false;

    protected final ArrayList<Object> mRecords = new ArrayList<>();
    private int mCellMarginTop;
    private int mPortraitColumns = 3;

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

    protected Object getTileFromRecord(Object record) {
        return XposedHelpers.getObjectField(record, "tile");
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
        Resources res = ResourceUtils.getInstance(mContext).getResources();
        int columns = Math.max(1, res.getInteger(R.integer.quick_settings_num_columns));
        if (mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            columns = mPortraitColumns;
        }
        mLargeCellWidth = res.getDimensionPixelSize(R.dimen.qs_dual_tile_width);
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

    public void setColumns(int columns) {
        if (mPortraitColumns != columns) {
            mPortraitColumns = columns;
            updateResources();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int numTiles = mRecords.size();
        final int width = MeasureSpec.getSize(widthMeasureSpec);
        final int rows = (numTiles + mColumns - 1) / mColumns;
        mCellWidth = (width - (mCellMargin * (mColumns + 1))) / mColumns;

        int row = 0;
        int column = 0;
        for (Object record : mRecords) {
            View tileView = getTileViewFromRecord(record);
            if (tileView.getVisibility() != VISIBLE) continue;
            if (column == getColumns(row)) {
                row++;
                column = 0;
            }
            setDual(record, isDual(row));
            tileView.measure(exactly(getCellWidth(row)), exactly(mCellHeight));
            column++;
        }
        setMeasuredDimension(width,
                (mCellHeight + mCellMargin) * rows + (mCellMarginTop - mCellMargin));
    }

    private void setDual(Object record, boolean dual) {
        View tileView = getTileViewFromRecord(record);
        try {
            XposedHelpers.callMethod(tileView, "setDual", new Class[] {boolean.class}, dual);
        } catch (Throwable t) { // CM13
            Object tile = getTileFromRecord(record);
            XposedHelpers.callMethod(tileView, "setDual", new Class[] {boolean.class, boolean.class}, dual, XposedHelpers.callMethod(tile, "hasDualTargetsDetails"));
        }
    }

    private boolean isDual(int row) {
        return mFirstRowLarge && row == 0;
    }

    private int getColumns(int row) {
        return isDual(row) ? 2 : mColumns;
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
        for (int i = 0; i < mRecords.size(); i++) {
            Object record = mRecords.get(i);
            View tileView = getTileViewFromRecord(record);
            if (tileView.getVisibility() != VISIBLE) continue;
            if (column == getColumns(row)) {
                row++;
                column = 0;
            }
            int left = getColumnStart(row, column);
            final int top = getRowTop(row);
            int right;
            right = left + getCellWidth(row);
            tileView.layout(left, top, right, top + tileView.getMeasuredHeight());
            column++;
        }
    }

    private int getCellWidth(int row) {
        return (mFirstRowLarge && row == 0) ? mLargeCellWidth : mCellWidth;
    }

    private int getRowTop(int row) {
        return row * (mCellHeight + mCellMargin) + mCellMarginTop;
    }

    private int getColumnStart(int row, int column) {
        if (mFirstRowLarge && row == 0) {
            return column * mLargeCellWidth + (column + 1) * (getWidth() - mLargeCellWidth * 2) / (3);
        } else
            return column * (mCellWidth + mCellMargin) + mCellMargin;
    }

    public void setFirstRowLarge(boolean firstRowLarge) {
        mFirstRowLarge = firstRowLarge;
        requestLayout();
    }
}