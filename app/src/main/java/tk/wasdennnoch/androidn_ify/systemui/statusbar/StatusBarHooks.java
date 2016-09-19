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
    private static final String CLASS_MOBILE_DATA_CONTROLLER_51 = "com.android.systemui.statusbar.policy.MobileDataControllerImpl";
    private static final String CLASS_MOBILE_DATA_CONTROLLER_50 = "com.android.systemui.statusbar.policy.MobileDataController";

    protected final ClassLoader mClassLoader;
    private final Class<?> mPhoneStatusBarClass;
    protected final Class<?> mSignalClusterClass;
    private final Class<?> mMobileDataControllerClass;
    protected boolean mLastDataDisabled = false;
    protected boolean mDataDisabled = false;

    protected Context mContext;
    protected Object mPhoneStatusBar;
    private Handler mHandler;

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
        hookSetMobileDataEnabled();
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
        XposedBridge.hookAllMethods(mSignalClusterClass, "setMobileDataIndicators", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (mDataDisabled) {
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

    protected String getMobileDataControllerClass() {
        return Build.VERSION.SDK_INT >= 22 ? CLASS_MOBILE_DATA_CONTROLLER_51 : CLASS_MOBILE_DATA_CONTROLLER_50;
    }

    public void post(Runnable r) {
        mHandler.post(r);
    }
}
