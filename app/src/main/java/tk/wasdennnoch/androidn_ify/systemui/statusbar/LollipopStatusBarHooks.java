package tk.wasdennnoch.androidn_ify.systemui.statusbar;

import android.os.Handler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.systemui.SystemUIHooks;

class LollipopStatusBarHooks extends StatusBarHooks {
    private static final String TAG = "LollipopStatusBarHooks";

    private static final String CLASS_SIGNAL_CONTROLLER = "com.android.systemui.statusbar.policy.NetworkControllerImpl$SignalController";
    private static final String CLASS_MOBILE_SIGNAL_CONTROLLER = "com.android.systemui.statusbar.policy.NetworkControllerImpl$MobileSignalController";
    private static final String CLASS_SIGNAL_CLUSTER_VIEW = "com.android.systemui.statusbar.SignalClusterView";

    LollipopStatusBarHooks(ClassLoader classLoader) {
        super(classLoader);
    }

    @Override
    protected String getSignalControllerClass() {
        return CLASS_SIGNAL_CONTROLLER;
    }

    @Override
    protected String getMobileSignalControllerClass() {
        return CLASS_MOBILE_SIGNAL_CONTROLLER;
    }

    @Override
    protected void hookSetMobileDataIndicators() {
        Class<?> classSignalCluster = XposedHelpers.findClass(CLASS_SIGNAL_CLUSTER_VIEW, mClassLoader);

        XposedBridge.hookAllMethods(classSignalCluster, "setMobileDataIndicators", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                int typeIcon = 2;
                int isTypeIconWide = 5;
                if (mDataDisabled) {
                    param.args[typeIcon] = SystemUIHooks.R_drawable_stat_sys_data_disabled;
                    param.args[isTypeIconWide] = false;
                }
            }
        });
    }

    public void startRunnableDismissingKeyguard(final Runnable runnable) {
        Handler mHandler = (Handler) XposedHelpers.getObjectField(mPhoneStatusBar, "mHandler");
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    XposedHelpers.callMethod(mPhoneStatusBar, "dismissKeyguardThenExecute", createOnDismissAction(runnable), true);
                } catch (Throwable t) {
                    XposedHook.logE(TAG, "Error in startRunnableDismissingKeyguard, executing instantly (" + t.toString() + ")", null);
                    runnable.run();
                }
            }
        });
    }

    private Object createOnDismissAction(final Runnable runnable) {
        Class<?> classOnDismissAction = XposedHelpers.findClass("com.android.keyguard.KeyguardHostView.OnDismissAction", mClassLoader);
        return Proxy.newProxyInstance(mContext.getClassLoader(), new Class<?>[]{classOnDismissAction}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                switch (method.getName()) {
                    case "onDismiss":
                        runnable.run();
                }
                return false;
            }
        });
    }
}
