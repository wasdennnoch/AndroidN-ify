package tk.wasdennnoch.androidn_ify.systemui.notifications;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.extracted.systemui.ExpandableIndicator;
import tk.wasdennnoch.androidn_ify.misc.SafeOnClickListener;
import tk.wasdennnoch.androidn_ify.systemui.qs.customize.QSCustomizer;
import tk.wasdennnoch.androidn_ify.utils.ConfigUtils;

@SuppressWarnings({"SameParameterValue", "WeakerAccess"})
public class NotificationPanelHooks {

    private static final String TAG = "NotificationPanelHooks";

    private static final String PACKAGE_SYSTEMUI = XposedHook.PACKAGE_SYSTEMUI;
    private static final String CLASS_NOTIFICATION_PANEL_VIEW = "com.android.systemui.statusbar.phone.NotificationPanelView";
    private static final String CLASS_PANEL_VIEW = "com.android.systemui.statusbar.phone.PanelView";

    public static final int STATE_KEYGUARD = 1;

    public static ViewGroup mNotificationPanelView;
    private static ExpandableIndicator mExpandIndicator;
    private static QSCustomizer mQsCustomizer;

    private static final List<BarStateCallback> mBarStateCallbacks = new ArrayList<>();

    private static final XC_MethodHook onFinishInflateHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            mNotificationPanelView = (ViewGroup) param.thisObject;
            Context context = mNotificationPanelView.getContext();

            mNotificationPanelView.setClipChildren(false);
            mNotificationPanelView.setClipToPadding(false);
            View mHeader = (View) XposedHelpers.getObjectField(param.thisObject, "mHeader");
            mHeader.setOnClickListener(null);
            mExpandIndicator = (ExpandableIndicator) mHeader.findViewById(R.id.statusbar_header_expand_indicator);
            mExpandIndicator.setOnClickListener(mExpandIndicatorListener);

            if (!ConfigUtils.qs().keep_qs_panel_background) {
                View mQsContainer = (View) XposedHelpers.getObjectField(param.thisObject, "mQsContainer");
                try {
                    //noinspection deprecation
                    mQsContainer.setBackgroundColor(context.getResources().getColor(context.getResources().getIdentifier("system_primary_color", "color", PACKAGE_SYSTEMUI)));
                } catch (Throwable t) {
                    XposedHook.logE(TAG, "Couldn't change QS container background color", t);
                }
            }

            FrameLayout.LayoutParams qsCustomizerLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            QSCustomizer qsCustomizer = new QSCustomizer(context);
            mNotificationPanelView.addView(qsCustomizer, qsCustomizerLp);

            mQsCustomizer = qsCustomizer;
        }
    };

    private static final XC_MethodHook setBarStateHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            XposedHook.logD(TAG, "setBarStateHook: Setting state to " + (int) param.args[0]);
            //StatusBarHeaderHooks.onSetBarState((int) param.args[0]);
            for (BarStateCallback callback : mBarStateCallbacks) {
                callback.onStateChanged();
            }
        }
    };

    private static final View.OnClickListener mExpandIndicatorListener = new SafeOnClickListener() {
        @Override
        public void onClickSafe(View v) {
            // Fixes an issue with the indicator having two backgrounds when layer type is hardware
            mExpandIndicator.setLayerType(View.LAYER_TYPE_NONE, null);
            flingSettings(!mExpandIndicator.isExpanded());
        }
    };

    public static void expandWithQs() {
        try {
            if (ConfigUtils.M) {
                XposedHelpers.callMethod(mNotificationPanelView, "expandWithQs");
            } else {
                XposedHelpers.callMethod(mNotificationPanelView, "expand");
            }
        } catch (Throwable ignore) {

        }
    }

    public static boolean isExpanded() {
        return (mExpandIndicator != null && mExpandIndicator.isExpanded());
    }

    public static boolean isCollapsed() {
        return (mExpandIndicator != null && !mExpandIndicator.isExpanded());
    }

    public static void expandIfNecessary() {
        if (mExpandIndicator != null && mNotificationPanelView != null) {
            if (!mExpandIndicator.isExpanded()) {
                flingSettings(true);
            }
        }
    }

    public static void collapseIfNecessary() {
        if (mExpandIndicator != null && mNotificationPanelView != null) {
            if (mExpandIndicator.isExpanded()) {
                flingSettings(false);
            }
        }
    }

    private static void flingSettings(boolean expanded) {
        try {
            XposedHelpers.callMethod(mNotificationPanelView, "flingSettings", new Class[]{float.class, boolean.class, Runnable.class, boolean.class}, 0, expanded, null, true);
        } catch (Throwable t) {
            XposedHelpers.callMethod(mNotificationPanelView, "flingSettings", 0, expanded);
        }
    }

    public static boolean isOnKeyguard() {
        return getStatusBarState() == NotificationPanelHooks.STATE_KEYGUARD;
    }

    public static int getStatusBarState() {
        return XposedHelpers.getIntField(mNotificationPanelView, "mStatusBarState");
    }

    public static void hook(ClassLoader classLoader) {
        try {
            if (ConfigUtils.qs().header) {

                Class<?> classNotificationPanelView = XposedHelpers.findClass(CLASS_NOTIFICATION_PANEL_VIEW, classLoader);
                Class<?> classPanelView = XposedHelpers.findClass(CLASS_PANEL_VIEW, classLoader);

                //Class<?> classNotificationsQuickSettingsContainer = XposedHelpers.findClass(CLASS_NOTIFICATIONS_QUICK_SETTINGS_CONTAINER, classLoader);

                XposedHelpers.findAndHookMethod(classNotificationPanelView, "onFinishInflate", onFinishInflateHook);
                XposedHelpers.findAndHookMethod(classNotificationPanelView, "setBarState", int.class, boolean.class, boolean.class, setBarStateHook);

                XposedHelpers.findAndHookMethod(classPanelView, "schedulePeek", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        XposedHelpers.callMethod(mNotificationPanelView, "setListening", true);
                    }
                });

                /*
                XposedHelpers.findAndHookMethod(classNotificationsQuickSettingsContainer, "onFinishInflate", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        FrameLayout container = (FrameLayout) param.thisObject;

                        FrameLayout.LayoutParams qsCustomizerLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                        QSCustomizer qsCustomizer = new QSCustomizer(container.getContext());
                        qsCustomizer.setLayoutParams(qsCustomizerLp);
                        //container.addView(qsCustomizer, 2);

                        mQsCustomizer = qsCustomizer;
                    }
                });
                */

                XC_MethodHook returnIfCustomizing = new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (mQsCustomizer != null && mQsCustomizer.isCustomizing())
                            param.setResult(false);
                    }
                };

                XposedHelpers.findAndHookMethod(classNotificationPanelView, "onInterceptTouchEvent", MotionEvent.class, returnIfCustomizing);
                XposedHelpers.findAndHookMethod(classNotificationPanelView, "onTouchEvent", MotionEvent.class, returnIfCustomizing);

            }
        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error in hook", t);
        }
    }

    public static void addBarStateCallback(BarStateCallback callback) {
        mBarStateCallbacks.add(callback);
    }

    public static void removeBarStateCallback(BarStateCallback callback) {
        mBarStateCallbacks.remove(callback);
    }

    public static QSCustomizer getQsCustomizer() {
        return mQsCustomizer;
    }

    public interface BarStateCallback {
        void onStateChanged();
    }

    public static void invalidateTileAdapter() {
        if (mQsCustomizer != null)
            mQsCustomizer.invalidateTileAdapter();
    }

    public static void handleStateChanged(Object qsTile, Object state) {
        if (mQsCustomizer != null)
            mQsCustomizer.handleStateChanged(qsTile, state);
    }

    public static void showQsCustomizer(ArrayList<Object> records, boolean animated) {
        if (canShowCustomizer(records))
            mQsCustomizer.show(records, animated);
    }

    public static void showQsCustomizer(ArrayList<Object> records, int x, int y) {
        if (canShowCustomizer(records))
            mQsCustomizer.show(records, x, y);
    }

    private static boolean canShowCustomizer(ArrayList<Object> records) {
        if (records == null) {
            Toast.makeText(StatusBarHeaderHooks.mContext, "Couldn't open edit view; mRecords == null", Toast.LENGTH_SHORT).show();
            XposedHook.logE(TAG, "Couldn't open edit view; mRecords == null", null);
            return false;
        }
        if (mQsCustomizer == null) {
            Toast.makeText(StatusBarHeaderHooks.mContext, "Couldn't open edit view; mQsCustomizer == null", Toast.LENGTH_SHORT).show();
            XposedHook.logE(TAG, "Couldn't open edit view; mQsCustomizer == null", null);
            return false;
        }
        return true;
    }
}
