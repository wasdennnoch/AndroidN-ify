package tk.wasdennnoch.androidn_ify.systemui.qs;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.ArrayList;

import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.systemui.notifications.StatusBarHeaderHooks;
import tk.wasdennnoch.androidn_ify.utils.ConfigUtils;
import tk.wasdennnoch.androidn_ify.utils.ResourceUtils;

public class PagedTileLayout extends ViewPager implements QuickSettingsHooks.QSTileLayout, View.OnClickListener {

    private static final String TAG = "PagedTileLayout";

    private final ArrayList<Object> mTiles = new ArrayList<>();
    private final ArrayList<TilePage> mPages = new ArrayList<>();
    private final Context mContext;

    private PageIndicator mPageIndicator;

    private int mNumPages;
    private FrameLayout mDecorGroup;
    private PageListener mPageListener;

    private int mPosition;
    private boolean mOffPage;
    private TextView mEditBtn;

    public PagedTileLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        setAdapter(mAdapter);
        addOnPageChangeListener(new OnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                if (mPageIndicator == null) return;
                if (mPageListener != null) {
                    mPageListener.onPageChanged(position == 0);
                }
            }

            @Override
            public void onPageScrolled(int position, float positionOffset,
                                       int positionOffsetPixels) {
                if (mPageIndicator == null) return;
                setCurrentPage(position, positionOffset != 0);
                mPageIndicator.setLocation(position + positionOffset);
                if (mPageListener != null) {
                    mPageListener.onPageChanged(position == 0);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
        setCurrentItem(0);

        addDecorView();
    }

    @Override
    public void setCurrentItem(int item, boolean smoothScroll) {
        super.setCurrentItem(item, smoothScroll);
    }

    protected Object getTileFromRecord(Object record) {
        return XposedHelpers.getObjectField(record, "tile");
    }

    /**
     * Sets individual pages to listening or not.  If offPage it will set
     * the next page after position to listening as well since we are in between
     * pages.
     */
    private void setCurrentPage(int position, boolean offPage) {
        if (mPosition == position && mOffPage == offPage) return;
        // Save the current state.
        mPosition = position;
        mOffPage = offPage;
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    @Override
    public int getOffsetTop(Object tile) {
        final ViewGroup parent = (ViewGroup) ((View) XposedHelpers.getObjectField(tile, "tileView")).getParent();
        if (parent == null) return 0;
        return parent.getTop() + getTop();
    }

    @Override
    public void addTile(Object tile) {
        mTiles.add(tile);
        postDistributeTiles();
    }

    @Override
    public void removeTile(Object tile) {
        if (mTiles.remove(tile)) {
            postDistributeTiles();
        }
    }

    public void setPageListener(PageListener listener) {
        mPageListener = listener;
    }

    private void postDistributeTiles() {
        removeCallbacks(mDistribute);
        post(mDistribute);
    }

    public void removeAll() {
        final int NP = mPages.size();
        for (int i = 0; i < NP; i++) {
            mPages.get(i).removeAllViews();
        }
    }

    private void distributeTiles() {
        XposedHook.logD(TAG, "Distributing tiles");
        removeAll();
        int index = 0;
        final int NT = mTiles.size();
        for (int i = 0; i < NT; i++) {
            Object tile = mTiles.get(i);
            if (mPages.get(index).isFull()) {
                if (++index == mPages.size()) {
                    XposedHook.logD(TAG, "Adding page for "
                            + XposedHelpers.getObjectField(tile, "tile").getClass().getSimpleName());
                    mPages.add(new TilePage(mContext, null));
                }
            }
            XposedHook.logD(TAG, "Adding " + getTileFromRecord(tile).getClass().getSimpleName() + " to " + index);
            mPages.get(index).addTile(tile);
        }
        if (mNumPages != index + 1) {
            mNumPages = index + 1;
            while (mPages.size() > mNumPages) {
                mPages.remove(mPages.size() - 1);
            }
            XposedHook.logD(TAG, "Size: " + mNumPages);
            mPageIndicator.setNumPages(mNumPages);
            setAdapter(mAdapter);
            mAdapter.notifyDataSetChanged();
            setCurrentItem(0, false);
        }
    }

    @Override
    public boolean updateResources() {
        boolean changed = false;
        for (int i = 0; i < mPages.size(); i++) {
            changed |= mPages.get(i).updateResources();
        }
        if (changed) {
            distributeTiles();
        }
        return changed;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        // The ViewPager likes to eat all of the space, instead force it to wrap to the max height
        // of the pages.
        int maxHeight = 0;
        final int N = getChildCount();
        for (int i = 0; i < N; i++) {
            int height = getChildAt(i).getMeasuredHeight();
            if (height > maxHeight) {
                maxHeight = height;
            }
        }
        setMeasuredDimension(getMeasuredWidth(), maxHeight + mDecorGroup.getMeasuredHeight());
    }

    private final Runnable mDistribute = new Runnable() {
        @Override
        public void run() {
            distributeTiles();
        }
    };

    public int getColumnCount() {
        if (mPages.size() == 0) return 0;
        return mPages.get(0).mColumns;
    }

    public void setColumnCount(int columns) {
        // Use module settings when using default column count,
        // override count otherwise (ROM settings)
        if (columns == 3) return;
        XposedHook.logD(TAG, "Overwriting column count to " + columns);
        for (TilePage p : mPages)
            p.setColumns(columns);
    }

    public int getRowCount() {
        if (mPages.size() == 0) return 0;
        return mPages.get(0).mMaxRows;
    }

    @Override
    public void onClick(View v) {
        StatusBarHeaderHooks.onClickEdit(mEditBtn.getLeft() + mEditBtn.getWidth() / 2, getTop() + mDecorGroup.getTop() + mDecorGroup.getHeight() / 2);
    }

    static class TilePage extends TileLayout {
        private int mMaxRows = 3;

        public TilePage(Context context, AttributeSet attrs) {
            super(context, attrs);
            updateResources();
            setClipChildren(false);
            setClipToPadding(false);
        }

        @Override
        public boolean updateResources() {
            final int rows = getRows();
            boolean changed = rows != mMaxRows;
            if (changed) {
                mMaxRows = rows;
                requestLayout();
            }
            return super.updateResources() || changed;
        }

        private int getRows() {
            final Resources res = getContext().getResources();
            if (res.getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                // Always have 3 rows in portrait.
                return 3;
            }
            return Math.max(1, ResourceUtils.getInstance(mContext).getResources().getInteger(R.integer.quick_settings_num_rows));
        }

        public void setMaxRows(int maxRows) {
            mMaxRows = maxRows;
        }

        public boolean isFull() {
            return mRecords.size() >= mColumns * mMaxRows;
        }
    }

    private final PagerAdapter mAdapter = new PagerAdapter() {
        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            XposedHook.logD(TAG, "Destantiating " + position);
            container.removeView((View) object);
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            XposedHook.logD(TAG, "Instantiating " + position);
            ViewGroup view = mPages.get(position);
            container.addView(view);
            return view;
        }

        @Override
        public int getCount() {
            return mNumPages;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }
    };

    public interface PageListener {
        void onPageChanged(boolean isFirst);
    }

    private void addDecorView() {
        ResourceUtils res = ResourceUtils.getInstance(mContext);
        ViewPager.LayoutParams decorLayoutLp = new LayoutParams();
        decorLayoutLp.width = ViewGroup.LayoutParams.MATCH_PARENT;
        decorLayoutLp.height = res.getDimensionPixelSize(R.dimen.qs_panel_decor_height);
        decorLayoutLp.gravity = Gravity.BOTTOM;
        decorLayoutLp.isDecor = true;
        mDecorGroup = new FrameLayout(mContext);
        mDecorGroup.setLayoutParams(decorLayoutLp);

        FrameLayout.LayoutParams pageIndicatorLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pageIndicatorLp.gravity = Gravity.CENTER;
        mPageIndicator = new PageIndicator(mContext);
        mPageIndicator.setLayoutParams(pageIndicatorLp);
        mDecorGroup.addView(mPageIndicator);

        if (ConfigUtils.qs().enable_qs_editor) {
            int editBtnPadding = res.getDimensionPixelSize(R.dimen.qs_edit_padding);

            FrameLayout.LayoutParams editBtnLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            editBtnLp.gravity = Gravity.END;
            mEditBtn = new TextView(mContext);
            mEditBtn.setLayoutParams(editBtnLp);
            mEditBtn.setId(R.id.qs_edit);
            mEditBtn.setMinWidth(res.getDimensionPixelSize(R.dimen.qs_edit_min_width));
            mEditBtn.setText(res.getString(R.string.qs_edit));
            mEditBtn.setTextColor(res.getColor(R.color.edit_btn_text));
            mEditBtn.setFocusable(true);
            mEditBtn.setGravity(Gravity.CENTER);
            mEditBtn.setBackground(res.getDrawable(R.drawable.qs_btn_borderless_rect));
            mEditBtn.setOnClickListener(this);
            mEditBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            mEditBtn.setTextColor(res.getColor(R.color.qs_detail_button));
            mEditBtn.setAllCaps(true);
            mEditBtn.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            mEditBtn.setPadding(editBtnPadding, editBtnPadding, editBtnPadding, editBtnPadding);
            mDecorGroup.addView(mEditBtn);
        }

        addView(mDecorGroup);
        mPages.add(new TilePage(mContext, null));
    }
}