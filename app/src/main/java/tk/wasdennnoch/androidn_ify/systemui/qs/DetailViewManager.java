package tk.wasdennnoch.androidn_ify.systemui.qs;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.android.internal.logging.MetricsLogger;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;

import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.systemui.notifications.StatusBarHeaderHooks;
import tk.wasdennnoch.androidn_ify.ui.SettingsActivity;
import tk.wasdennnoch.androidn_ify.utils.ConfigUtils;
import tk.wasdennnoch.androidn_ify.utils.ResourceUtils;

public class DetailViewManager {

    private static final String TAG = "DetailViewManager";
    private static final String CLASS_DETAIL_ADAPTER = "com.android.systemui.qs.QSTile$DetailAdapter";

    private static DetailViewManager sInstance;

    private Context mContext;
    private ViewGroup mStatusBarHeaderView;
    private ViewGroup mQsPanel;
    private Button mEditButton;
    private boolean mHasEditPanel;

    private RecyclerView mRecyclerView;
    private Object mEditAdapter;
    private TileAdapter mTileAdapter;

    public static DetailViewManager getInstance() {
        if (sInstance == null)
            throw new IllegalStateException("Must initialize DetailViewManager first");
        return sInstance;
    }

    public static void init(Context context, ViewGroup statusBarHeaderView, ViewGroup qsPanel, Button editButton, boolean hasEditPanel) {
        sInstance = new DetailViewManager(context, statusBarHeaderView, qsPanel, editButton, hasEditPanel);
    }

    private DetailViewManager(Context context, ViewGroup statusBarHeaderView, ViewGroup qsPanel, Button editButton, boolean hasEditPanel) {
        mContext = context;
        mStatusBarHeaderView = statusBarHeaderView;
        mQsPanel = qsPanel;
        mEditButton = editButton;
        mHasEditPanel = hasEditPanel;
    }

    public void saveChanges() {
        mTileAdapter.saveChanges();
    }

    public void showEditView(ArrayList<Object> records) {
        if (records == null) {
            Toast.makeText(mContext, "Couldn't open edit view; mRecords == null", Toast.LENGTH_SHORT).show();
            XposedHook.logE(TAG, "Couldn't open edit view; mRecords == null", null);
            return;
        }
        if (mEditAdapter == null)
            createEditAdapter(records);

        showDetailAdapter(mEditAdapter);
    }

    private void showDetailAdapter(Object adapter) {
        int x = mEditButton.getLeft() + mEditButton.getWidth() / 2;
        int y = mEditButton.getTop() + mEditButton.getHeight() / 2;
        if (mHasEditPanel)
            y += mStatusBarHeaderView.getHeight();
        StatusBarHeaderHooks.mEditing = true;
        if (!ConfigUtils.M) {
            XposedHelpers.callMethod(mQsPanel, "showDetailAdapter", true, adapter);
        } else {
            try {
                XposedHelpers.callMethod(mQsPanel, "showDetailAdapter", true, adapter, new int[]{x, y});
            } catch (Throwable t) { // OOS3
                ClassLoader classLoader = mContext.getClassLoader();
                Class<?> classRemoteSetting = XposedHelpers.findClass(XposedHook.PACKAGE_SYSTEMUI + ".qs.RemoteSetting", classLoader);
                Object remoteSetting = Proxy.newProxyInstance(classLoader, new Class[]{classRemoteSetting}, new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        if (method.getName().equals("getSettingsIntent"))
                            return new Intent(Intent.ACTION_MAIN)
                                    .setClassName("tk.wasdennnoch.androidn_ify", SettingsActivity.class.getName())
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        return null;
                    }
                });
                XposedHelpers.callMethod(mQsPanel, "showDetailAdapter", true, remoteSetting, adapter, new int[]{x, y});
            }
        }
    }

    private void createEditAdapter(ArrayList<Object> records) {
        if (mRecyclerView == null)
            createEditView(records);

        Class<?> classDetailAdapter = XposedHelpers.findClass(CLASS_DETAIL_ADAPTER, mContext.getClassLoader());

        mEditAdapter = Proxy.newProxyInstance(classDetailAdapter.getClassLoader(), new Class<?>[]{classDetailAdapter}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                switch (method.getName()) {
                    case "getTitle":
                        return mContext.getResources().getIdentifier("quick_settings_settings_label", "string", XposedHook.PACKAGE_SYSTEMUI);
                    case "getToggleState":
                        return false;
                    case "getSettingsIntent":
                        return new Intent(Intent.ACTION_MAIN)
                                .setClassName("tk.wasdennnoch.androidn_ify", SettingsActivity.class.getName())
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    case "setToggleState":
                        return null;
                    case "getMetricsCategory":
                        return MetricsLogger.QS_INTENT;
                    case "createDetailView":
                        return mRecyclerView;
                }
                return null;
            }
        });
    }

    private void createEditView(ArrayList<Object> records) {
        // Init tiles list
        mTileAdapter = new TileAdapter(records, mContext, mQsPanel);
        TileTouchCallback callback = new TileTouchCallback();
        ItemTouchHelper mItemTouchHelper = new CustomItemTouchHelper(callback);
        XposedHelpers.setIntField(callback, "mCachedMaxScrollSpeed", ResourceUtils.getInstance(mContext).getDimensionPixelSize(R.dimen.lib_item_touch_helper_max_drag_scroll_per_frame));
        // With this, it's very easy to deal with drag & drop
        GridLayoutManager gridLayoutManager = new GridLayoutManager(mContext, 3);
        gridLayoutManager.setSpanSizeLookup(mTileAdapter.getSizeLookup());
        mRecyclerView = new RecyclerView(mContext);
        mRecyclerView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mRecyclerView.setAdapter(mTileAdapter);
        mRecyclerView.setLayoutManager(gridLayoutManager);
        mRecyclerView.addItemDecoration(mTileAdapter.getItemDecoration());
        /*mRecyclerView.setVerticalScrollBarEnabled(true);
        mRecyclerView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        mRecyclerView.setScrollingTouchSlop(RecyclerView.TOUCH_SLOP_DEFAULT);*/
        // TODO these above have no effect and the grid isn't scrolling smoothly
        // Also a ScrollView seems to be used in the official version
        mRecyclerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN && mRecyclerView.canScrollVertically(1)) {
                    mRecyclerView.requestDisallowInterceptTouchEvent(true);
                }
                return false;
            }
        });
        mTileAdapter.setTileTouchCallback(callback);
        mItemTouchHelper.attachToRecyclerView(mRecyclerView);
    }

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

    public class TileTouchCallback extends ItemTouchHelper.Callback {
        public TileAdapter.TileViewHolder mCurrentDrag;

        @Override
        public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.START | ItemTouchHelper.END;
            if (viewHolder.getItemViewType() == 1) {
                dragFlags = 0;
            }
            return makeMovementFlags(dragFlags, 0);
        }

        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            return mTileAdapter.onItemMove(viewHolder.getAdapterPosition(),
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
                mCurrentDrag = (TileAdapter.TileViewHolder) viewHolder;
                mCurrentDrag.startDrag();
            }
            try {
                mTileAdapter.notifyItemChanged(mTileAdapter.mDividerIndex);
            } catch (Throwable ignore) {

            }
            super.onSelectedChanged(viewHolder, actionState);
        }

        @Override
        public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            ((TileAdapter.TileViewHolder) viewHolder).stopDrag();
            super.clearView(recyclerView, viewHolder);
        }
    }

}
