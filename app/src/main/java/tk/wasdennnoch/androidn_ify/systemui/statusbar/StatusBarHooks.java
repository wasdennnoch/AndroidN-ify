package tk.wasdennnoch.androidn_ify.systemui.statusbar;

import android.content.Context;
import android.os.Build;
import android.os.Handler;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.systemui.SystemUIHooks;
import tk.wasdennnoch.androidn_ify.utils.ConfigUtils;

public class StatusBarHooks {

    private static final String TAG = "StatusBarHooks";

    private static final String CLASS_PHONE_STATUS_BAR = "com.android.systemui.statusbar.phone.PhoneStatusBar";
    private static final String CLASS_SIGNAL_CONTROLLER = "com.android.systemui.statusbar.policy.SignalController";
    private static final String CLASS_SIGNAL_CLUSTER_VIEW = "com.android.systemui.statusbar.SignalClusterView";
    private static final String CLASS_MOBILE_SIGNAL_CONTROLLER = "com.android.systemui.statusbar.policy.MobileSignalController";
    private static final String CLASS_MOBILE_DATA_CONTROLLER_51 = "com.android.systemui.statusbar.policy.MobileDataControllerImpl";
    private static final String CLASS_MOBILE_DATA_CONTROLLER_50 = "com.android.systemui.statusbar.policy.MobileDataController";

    final ClassLoader mClassLoader;
    private final Class<?> mPhoneStatusBarClass;
    final Class<?> mSignalClusterClass;
    private final Class<?> mMobileDataControllerClass;
    private boolean mLastDataDisabled = false;
    boolean mDataDisabled = false;

    protected Context mContext;
    Object mPhoneStatusBar;
    private Handler mHandler;
    private Object mPhone;

    public static StatusBarHooks create(ClassLoader classLoader) {
        if (ConfigUtils.M) {
            return new StatusBarHooks(classLoader);
        } else {
            return new LollipopStatusBarHooks(classLoader);
        }
    }

    StatusBarHooks(ClassLoader classLoader) {
        mClassLoader = classLoader;
        mPhoneStatusBarClass = XposedHelpers.findClass(CLASS_PHONE_STATUS_BAR, mClassLoader);
        mSignalClusterClass = XposedHelpers.findClass(CLASS_SIGNAL_CLUSTER_VIEW, mClassLoader);
        mMobileDataControllerClass = XposedHelpers.findClass(getMobileDataControllerClass(), mClassLoader);
        hookStart();
        if (ConfigUtils.M)
            hookIsDirty();
        hookSetMobileDataIndicators();
        hookUpdateTelephony();
        hookSetMobileDataEnabled();
    }

    private void hookUpdateTelephony() {
        // Hook if possible, to reflect changes faster when toggling mobile data on
        try {
            Class classMobileSignalController = XposedHelpers.findClass(CLASS_MOBILE_SIGNAL_CONTROLLER, mClassLoader);
            XposedHelpers.findAndHookMethod(classMobileSignalController, "updateTelephony", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    mDataDisabled = isDataDisabled(param.thisObject);
                }
            });
        } catch (Throwable ignore) {
            hookMobileDataController();
        }
    }

    private boolean isDataDisabled(Object mobileSignalController) {
        try {
            if (mPhone == null)
                mPhone = XposedHelpers.getObjectField(mobileSignalController, "mPhone");
            Object mSubscriptionInfo = XposedHelpers.getObjectField(mobileSignalController, "mSubscriptionInfo");
            int subscriptionId = (int) XposedHelpers.callMethod(mSubscriptionInfo, "getSubscriptionId");
            return !((boolean) XposedHelpers.callMethod(mPhone, "getDataEnabled", subscriptionId));
        } catch (Throwable t) {
            return false;
        }
    }

    private void hookMobileDataController() {
        XposedHelpers.findAndHookConstructor(mMobileDataControllerClass, Context.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                boolean isDataEnabled = (boolean) XposedHelpers.callMethod(param.thisObject, "isMobileDataEnabled");
                mDataDisabled = !isDataEnabled;
            }
        });
    }

    private void hookStart() {
        XposedHelpers.findAndHookMethod(mPhoneStatusBarClass, "start", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                mPhoneStatusBar = param.thisObject;
                mHandler = (Handler) XposedHelpers.getObjectField(mPhoneStatusBar, "mHandler");
                mContext = (Context) XposedHelpers.getObjectField(mPhoneStatusBar, "mContext");
            }
        });
    }

    private void hookIsDirty() {
        Class<?> classSignalController = XposedHelpers.findClass(CLASS_SIGNAL_CONTROLLER, mClassLoader);
        XposedHelpers.findAndHookMethod(classSignalController, "isDirty", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (mLastDataDisabled != mDataDisabled) {
                    mLastDataDisabled = mDataDisabled;
                    param.setResult(true);
                }
            }
        });
    }

    private void hookSetMobileDataEnabled() {
        XposedHelpers.findAndHookMethod(mMobileDataControllerClass, "setMobileDataEnabled", boolean.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                mDataDisabled = !(boolean) param.args[0];
            }
        });
    }

    protected void hookSetMobileDataIndicators() {
        Class<?> classCallbackHandler = XposedHelpers.findClass("com.android.systemui.statusbar.policy.CallbackHandler", mClassLoader); // QS cellular tile isn't a SignalClusterView
        XposedBridge.hookAllMethods(classCallbackHandler, "setMobileDataIndicators", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (mDataDisabled && ConfigUtils.notifications().enable_data_disabled_indicator) {
                    int typeIcon = 2;
                    int qsTypeIcon = 3;
                    int isWide = 8;
                    param.args[typeIcon] = SystemUIHooks.R_drawable_stat_sys_data_disabled;
                    param.args[qsTypeIcon] = SystemUIHooks.R_drawable_ic_qs_data_disabled;
                    if (param.args[isWide] instanceof Boolean) {
                        param.args[isWide] = false;
                    } else { // Xperia put a load of ints in between
                        param.args[12] = false;
                    }
                }
            }
        });
    }

    public void startRunnableDismissingKeyguard(final Runnable runnable) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    //XposedHelpers.setBooleanField(mPhoneStatusBar, "mLeaveOpenOnKeyguardHide", true);
                    XposedHelpers.callMethod(mPhoneStatusBar, "executeRunnableDismissingKeyguard", runnable, null, false, false);
                } catch (Throwable t) {
                    XposedHook.logE(TAG, "Error in startRunnableDismissingKeyguard, executing instantly (" + t.toString() + ")", null);
                    runnable.run();
                }
            }
        });
    }

    private String getMobileDataControllerClass() {
        return Build.VERSION.SDK_INT >= 22 ? CLASS_MOBILE_DATA_CONTROLLER_51 : CLASS_MOBILE_DATA_CONTROLLER_50;
    }

    public void post(Runnable r) {
        mHandler.post(r);
    }
}
