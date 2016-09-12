package tk.wasdennnoch.androidn_ify.systemui.statusbar;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.systemui.SystemUIHooks;

public class StatusBarHooks {

    private static final String CLASS_SIGNAL_CONTROLLER = "com.android.systemui.statusbar.policy.SignalController";
    private static final String CLASS_MOBILE_SIGNAL_CONTROLLER = "com.android.systemui.statusbar.policy.MobileSignalController";
    private final ClassLoader mClassLoader;
    private final Class<?> mHookClass;
    private boolean mLastDataDisabled = false;
    private boolean mDataDisabled = false;

    public static StatusBarHooks create(ClassLoader classLoader) {
        return new StatusBarHooks(classLoader);
    }

    private StatusBarHooks(ClassLoader classLoader) {
        mClassLoader = classLoader;
        mHookClass = XposedHelpers.findClass(getHookClass(), mClassLoader);
        //hookConstructor();
        hookIsDirty();
        hookUpdateTelephony();
        hookSetMobileDataIndicators();
    }

    private void hookIsDirty() {
        Class<?> classSignalController = XposedHelpers.findClass(CLASS_SIGNAL_CONTROLLER, mClassLoader);

        XposedHelpers.findAndHookMethod(classSignalController, "isDirty", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (mHookClass == null || !mHookClass.isAssignableFrom(param.thisObject.getClass()))
                    return;
                if (mLastDataDisabled != mDataDisabled) {
                    mLastDataDisabled = mDataDisabled;
                    param.setResult(true);
                }
            }
        });
    }

    /*
    private void hookConstructor() {
        Class<?> classConfig = XposedHelpers.findClass("com.android.systemui.statusbar.policy.NetworkControllerImpl.Config", mClassLoader);
        Class<?> classTelephonyManager = XposedHelpers.findClass("android.telephony.TelephonyManager", mClassLoader);
        Class<?> classCallbackHandler = XposedHelpers.findClass("com.android.systemui.statusbar.policy.CallbackHandler", mClassLoader);
        Class<?> classNetworkControllerImpl = XposedHelpers.findClass("com.android.systemui.statusbar.policy.NetworkControllerImpl", mClassLoader);
        Class<?> classSubscriptionInfo = XposedHelpers.findClass("android.telephony.SubscriptionInfo", mClassLoader);
        Class<?> classSubscriptionDefaults = XposedHelpers.findClass("com.android.systemui.statusbar.policy.NetworkControllerImpl.SubscriptionDefaults", mClassLoader);

        XposedHelpers.findAndHookConstructor(mHookClass, Context.class, classConfig, boolean.class, classTelephonyManager, classCallbackHandler,
                classNetworkControllerImpl, classSubscriptionInfo, classSubscriptionDefaults, Looper.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {

                    }
                });
    }
    */

    private void hookUpdateTelephony() {
        XposedHelpers.findAndHookMethod(mHookClass, "updateTelephony", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                mDataDisabled = isDataDisabled(param.thisObject);
            }
        });
    }

    private void hookSetMobileDataIndicators() {
        Class<?> classCallbackHandler = XposedHelpers.findClass("com.android.systemui.statusbar.policy.CallbackHandler", mClassLoader);

        XposedBridge.hookAllMethods(classCallbackHandler, "setMobileDataIndicators", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                int typeIcon = 2;
                int qsTypeIcon = 3;
                if (mDataDisabled) {
                    param.args[typeIcon] = SystemUIHooks.R_drawable_stat_sys_data_disabled;
                    param.args[qsTypeIcon] = SystemUIHooks.R_drawable_ic_qs_data_disabled;
                }
            }
        });
    }

    private boolean isDataDisabled(Object mobileSignalController) {
        Object mPhone = XposedHelpers.getObjectField(mobileSignalController, "mPhone");
        Object mSubscriptionInfo = XposedHelpers.getObjectField(mobileSignalController, "mSubscriptionInfo");
        int subscriptionid = (int) XposedHelpers.callMethod(mSubscriptionInfo, "getSubscriptionId");
        return !((boolean) XposedHelpers.callMethod(mPhone, "getDataEnabled", subscriptionid));
    }

    private String getHookClass() {
        return CLASS_MOBILE_SIGNAL_CONTROLLER;
    }
}
