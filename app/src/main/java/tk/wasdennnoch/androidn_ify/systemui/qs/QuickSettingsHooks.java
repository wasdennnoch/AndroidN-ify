package tk.wasdennnoch.androidn_ify.systemui.qs;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import java.util.Collection;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.extracted.systemui.qs.PagedTileLayout;
import tk.wasdennnoch.androidn_ify.systemui.notifications.StatusBarHeaderHooks;

public class QuickSettingsHooks {

    private static final String TAG = "QuickSettingsHooks";

    private static final String CLASS_QS_PANEL = "com.android.systemui.qs.QSPanel";
    static final String CLASS_QS_DRAG_PANEL = "com.android.systemui.qs.QSDragPanel";

    final Class mHookClass;

    protected Context mContext;
    ViewGroup mQsPanel;
    View mBrightnessView;
    Object mFooter;
    View mDetail;

    private PagedTileLayout mTileLayout;
    private boolean mHookedGetGridHeight = false;
    private int mGridHeight;
    private XC_MethodReplacement
            getGridHeightHook = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            return mGridHeight;
        }
    };

    public static QuickSettingsHooks create(ClassLoader classLoader) {
        try {
            XposedHelpers.findClass(CLASS_QS_DRAG_PANEL, classLoader);
            return new CMQuickSettingsHooks(classLoader);
        } catch (Throwable t) {
            return new QuickSettingsHooks(classLoader);
        }
    }

    QuickSettingsHooks(ClassLoader classLoader) {
        mHookClass = XposedHelpers.findClass(getHookClass(), classLoader);
        hookConstructor();
        hookOnMeasure();
        hookOnLayout();
        hookUpdateResources();
        hookSetTiles();
    }

    protected void hookConstructor() {
        XposedHelpers.findAndHookConstructor(mHookClass, Context.class, AttributeSet.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                mQsPanel = (ViewGroup) param.thisObject;
                mContext = mQsPanel.getContext();
                mBrightnessView = (View) XposedHelpers.getObjectField(param.thisObject, "mBrightnessView");
                mFooter = XposedHelpers.getObjectField(param.thisObject, "mFooter");
                mDetail = (View) XposedHelpers.getObjectField(param.thisObject, "mDetail");
                setupTileLayout();
            }
        });
    }

    private void hookUpdateResources() {
        XposedHelpers.findAndHookMethod(mHookClass, "updateResources", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (mTileLayout != null) {
                    if (mTileLayout.updateResources())
                        StatusBarHeaderHooks.postSetupAnimators();
                    mTileLayout.setColumnCount(XposedHelpers.getIntField(param.thisObject, "mColumns"));
                }
            }
        });
    }

    private void hookSetTiles() {
        XposedHelpers.findAndHookMethod(mHookClass, "setTiles", Collection.class, new XC_MethodHook() {
            @SuppressWarnings("unchecked")
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                List<Object> mRecords = (List<Object>) XposedHelpers.getObjectField(param.thisObject, "mRecords");
                for (Object record : mRecords) {
                    mTileLayout.removeTile(record);
                }
                mTileLayout.removeAll();
            }

            @SuppressWarnings("unchecked")
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                List<Object> mRecords = (List<Object>) XposedHelpers.getObjectField(param.thisObject, "mRecords");
                for (Object record : mRecords) {
                    mTileLayout.addTile(record);
                }
            }
        });
    }

    void setupTileLayout() {
        mTileLayout = new PagedTileLayout(mContext, null);
        mQsPanel.addView(mTileLayout);
    }

    private void hookOnMeasure() {
        XposedHelpers.findAndHookMethod(mHookClass, "onMeasure", int.class, int.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                onMeasure((int) param.args[0], (int) param.args[1]);
                return null;
            }
        });
    }

    private void hookOnLayout() {
        XposedHelpers.findAndHookMethod(mHookClass, "onLayout", boolean.class, int.class, int.class, int.class, int.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                onLayout();
                return null;
            }
        });
    }

    @SuppressWarnings("UnusedParameters")
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int width = View.MeasureSpec.getSize(widthMeasureSpec);
        mBrightnessView.measure(exactly(width), View.MeasureSpec.UNSPECIFIED);
        mTileLayout.measure(exactly(width), View.MeasureSpec.UNSPECIFIED);

        View footerView = (View) XposedHelpers.callMethod(mFooter, "getView");
        footerView.measure(exactly(width), View.MeasureSpec.UNSPECIFIED);

        int h = mBrightnessView.getMeasuredHeight() + mTileLayout.getMeasuredHeight()/* + mPanelPaddingBottom*/;

        if ((boolean) XposedHelpers.callMethod(mFooter, "hasFooter")) {
            h += footerView.getMeasuredHeight();
        }
        if (!mHookedGetGridHeight) {
            try {
                XposedHelpers.setObjectField(mQsPanel, "mGridHeight", h);
            } catch (Throwable t) {
                try {
                    XposedHelpers.findAndHookMethod(mQsPanel.getClass(), "getGridHeight", getGridHeightHook);
                } catch (Throwable ignore) {
                    XposedHook.logW(TAG, "QSPanel#getGridHeight doesn't exist!");
                }
                mHookedGetGridHeight = true;
            }
        }
        // Used to clip header too
        mGridHeight = h;

        // TODO in N getGridHeight() returns getMeasuredHeight(), try that

        mDetail.measure(exactly(width), View.MeasureSpec.UNSPECIFIED);

        if (mDetail.getMeasuredHeight() < h) {
            mDetail.measure(exactly(width), exactly(h));
        }
        if (isShowingDetail() && !isClosingDetail() && isExpanded()) {
            h = mDetail.getMeasuredHeight();
        }

        XposedHelpers.callMethod(mQsPanel, "setMeasuredDimension", width, h);
    }

    protected void onLayout() {
        final int w = mQsPanel.getWidth();

        mBrightnessView.layout(0, 0, w, mBrightnessView.getMeasuredHeight());

        int viewPagerBottom = mBrightnessView.getMeasuredHeight() + mTileLayout.getMeasuredHeight();
        // view pager laid out from top of brightness view to bottom to page through settings
        mTileLayout.layout(0, mBrightnessView.getMeasuredHeight(), w, viewPagerBottom);

        mDetail.layout(0, 0, w, mDetail.getMeasuredHeight());

        if ((boolean) XposedHelpers.callMethod(mFooter, "hasFooter")) {
            View footer = (View) XposedHelpers.callMethod(mFooter, "getView");
            footer.layout(0, mQsPanel.getMeasuredHeight() - footer.getMeasuredHeight(),
                    footer.getMeasuredWidth(), mQsPanel.getMeasuredHeight());
        }

        if (!isShowingDetail() && !isClosingDetail()) {
            mBrightnessView.bringToFront();
        }
    }

    private boolean isShowingDetail() {
        return XposedHelpers.getObjectField(mQsPanel, "mDetailRecord") != null;
    }

    private boolean isClosingDetail() {
        return XposedHelpers.getBooleanField(mQsPanel, "mClosingDetail");
    }

    private boolean isExpanded() {
        return XposedHelpers.getBooleanField(mQsPanel, "mExpanded");
    }

    protected static int exactly(int size) {
        return View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY);
    }

    protected ViewGroup getTileViewFromRecord(Object record) {
        return (ViewGroup) XposedHelpers.getObjectField(record, "tileView");
    }

    protected String getHookClass() {
        return CLASS_QS_PANEL;
    }

    public PagedTileLayout getTileLayout() {
        return mTileLayout;
    }

    public int getGridHeight() {
        return mGridHeight;
    }

    public interface QSTileLayout {
        void addTile(Object tile);

        void removeTile(Object tile);

        int getOffsetTop(Object tile);

        boolean updateResources();
    }
}
