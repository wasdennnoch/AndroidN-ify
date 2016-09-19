package tk.wasdennnoch.androidn_ify.systemui.statusbar;

import android.content.Context;
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
    private static final String CLASS_MOBILE_SIGNAL_CONTROLLER = "com.android.systemui.statusbar.policy.MobileSignalController";
    protected final ClassLoader mClassLoader;
    private final Class<?> mPhoneStatusBarClass;
    private final Class<?> mMobileSignalControllerClass;
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
        mPhoneStatusBarClass = XposedHelpers.findClass(getPhoneStatusBarClass(), mClassLoader);
        mMobileSignalControllerClass = XposedHelpers.findClass(getMobileSignalControllerClass(), mClassLoader);
        //hookConstructor();
        hookStart();
        hookIsDirty();
        hookUpdateTelephony();
        hookSetMobileDataIndicators();
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
        Class<?> classSignalController = XposedHelpers.findClass(getSignalControllerClass(), mClassLoader);

        XposedHelpers.findAndHookMethod(classSignalController, "isDirty", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (mMobileSignalControllerClass == null || !mMobileSignalControllerClass.isAssignableFrom(param.thisObject.getClass()))
                    return;
                if (mLastDataDisabled != mDataDisabled) {
                    mLastDataDisabled = mDataDisabled;
                    param.setResult(true);
                }
            }
        });
    }

    protected String getSignalControllerClass() {
        return CLASS_SIGNAL_CONTROLLER;
    }

    /*
    private void hookConstructor() {
        Class<?> classConfig = XposedHelpers.findClass("com.android.systemui.statusbar.policy.NetworkControllerImpl.Config", mClassLoader);
        Class<?> classTelephonyManager = XposedHelpers.findClass("android.telephony.TelephonyManager", mClassLoader);
        Class<?> classCallbackHandler = XposedHelpers.findClass("com.android.systemui.statusbar.policy.CallbackHandler", mClassLoader);
        Class<?> classNetworkControllerImpl = XposedHelpers.findClass("com.android.systemui.statusbar.policy.NetworkControllerImpl", mClassLoader);
        Class<?> classSubscriptionInfo = XposedHelpers.findClass("android.telephony.SubscriptionInfo", mClassLoader);
        Class<?> classSubscriptionDefaults = XposedHelpers.findClass("com.android.systemui.statusbar.policy.NetworkControllerImpl.SubscriptionDefaults", mClassLoader);

        XposedHelpers.findAndHookConstructor(mMobileSignalControllerClass, Context.class, classConfig, boolean.class, classTelephonyManager, classCallbackHandler,
                classNetworkControllerImpl, classSubscriptionInfo, classSubscriptionDefaults, Looper.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {

                    }
                });
    }
    */

    private void hookUpdateTelephony() {
        XposedHelpers.findAndHookMethod(mMobileSignalControllerClass, "updateTelephony", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                mDataDisabled = isDataDisabled(param.thisObject);
            }
        });
    }

    protected void hookSetMobileDataIndicators() {
        Class<?> classCallbackHandler = XposedHelpers.findClass("com.android.systemui.statusbar.policy.CallbackHandler", mClassLoader);

        XposedBridge.hookAllMethods(classCallbackHandler, "setMobileDataIndicators", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (mDataDisabled) {
                    int typeIcon = 2;
                    int qsTypeIcon = 3;
                    int isWide = 8;
                    param.args[typeIcon] = SystemUIHooks.R_drawable_stat_sys_data_disabled;
                    param.args[qsTypeIcon] = SystemUIHooks.R_drawable_ic_qs_data_disabled;
                    try {
                        param.args[isWide] = false;
                    } catch (Throwable t) { // Xperia
                        param.args[12] = false;
                    }
                }
            }
        });
    }

    private boolean isDataDisabled(Object mobileSignalController) {
        try {
            Object mPhone = XposedHelpers.getObjectField(mobileSignalController, "mPhone");
            Object mSubscriptionInfo = XposedHelpers.getObjectField(mobileSignalController, "mSubscriptionInfo");
            int subscriptionId = (int) XposedHelpers.callMethod(mSubscriptionInfo, "getSubscriptionId");
            return !((boolean) XposedHelpers.callMethod(mPhone, "getDataEnabled", subscriptionId));
        } catch (Throwable t) {
            return false;
        }
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

    protected String getMobileSignalControllerClass() {
        return CLASS_MOBILE_SIGNAL_CONTROLLER;
    }

    private String getPhoneStatusBarClass() {
        return CLASS_PHONE_STATUS_BAR;
    }

    public void post(Runnable r) {
        mHandler.post(r);
    }
}
