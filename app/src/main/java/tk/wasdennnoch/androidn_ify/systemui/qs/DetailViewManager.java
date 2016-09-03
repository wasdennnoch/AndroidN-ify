package tk.wasdennnoch.androidn_ify.systemui.qs;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.logging.MetricsLogger;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;

import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.systemui.notifications.NotificationPanelHooks;
import tk.wasdennnoch.androidn_ify.systemui.notifications.StatusBarHeaderHooks;
import tk.wasdennnoch.androidn_ify.ui.AddTileActivity;
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
    private boolean mHasEditPanel;

    public static DetailViewManager getInstance() {
        if (sInstance == null)
            throw new IllegalStateException("Must initialize DetailViewManager first");
        return sInstance;
    }

    public static void init(Context context, ViewGroup statusBarHeaderView, ViewGroup qsPanel, TextView editButton, boolean hasEditPanel) {
        sInstance = new DetailViewManager(context, statusBarHeaderView, qsPanel, editButton, hasEditPanel);
    }

    private DetailViewManager(Context context, ViewGroup statusBarHeaderView, ViewGroup qsPanel, TextView editButton, boolean hasEditPanel) {
        mContext = context;
        mStatusBarHeaderView = statusBarHeaderView;
        mQsPanel = qsPanel;
        mHasEditPanel = hasEditPanel;
    }

    private void showDetailAdapter(Object adapter, int x, int y) {
        if (mHasEditPanel)
            y += mStatusBarHeaderView.getHeight();
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

    public Object createProxy(final DetailAdapter adapter) {
        Class<?> classDetailAdapter = XposedHelpers.findClass(CLASS_DETAIL_ADAPTER, mContext.getClassLoader());
        return Proxy.newProxyInstance(mContext.getClassLoader(), new Class<?>[]{classDetailAdapter}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                switch (method.getName()) {
                    case "getTitle":
                        return adapter.getTitle();
                    case "getToggleState":
                        return adapter.getToggleState();
                    case "createDetailView":
                        return adapter.createDetailView((Context) args[0], (View) args[1], (ViewGroup) args[2]);
                    case "getSettingsIntent":
                        return adapter.getSettingsIntent();
                    case "setToggleState":
                        adapter.setToggleState((boolean) args[0]);
                        return null;
                    case "getMetricsCategory":
                        return adapter.getMetricsCategory();
                }
                return null;
            }
        });
    }

    public DetailViewAdapter getDetailViewAdapter(View detailView) {
        if (detailView != null && detailView instanceof DetailViewAdapter) {
            return (DetailViewAdapter) detailView;
        }
        return null;
    }

    public interface DetailAdapter {
        int getTitle();

        Boolean getToggleState();

        DetailViewAdapter createDetailView(Context context, View convertView, ViewGroup parent);

        Intent getSettingsIntent();

        void setToggleState(boolean state);

        int getMetricsCategory();
    }

    public interface DetailViewAdapter {
        boolean hasRightButton();

        int getRightButtonResId();

        void handleRightButtonClick();
    }

    public static class DetailFrameLayout extends FrameLayout implements DetailViewAdapter {
        private DetailViewAdapter mAdapter;

        public DetailFrameLayout(Context context, DetailViewAdapter adapter) {
            super(context);
            mAdapter = adapter;
        }

        @Override
        public boolean hasRightButton() {
            return mAdapter.hasRightButton();
        }

        @Override
        public int getRightButtonResId() {
            return mAdapter.getRightButtonResId();
        }

        @Override
        public void handleRightButtonClick() {
            mAdapter.handleRightButtonClick();
        }
    }

}
