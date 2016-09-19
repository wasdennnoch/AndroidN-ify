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
import tk.wasdennnoch.androidn_ify.utils.RomUtils;

class LollipopStatusBarHooks extends StatusBarHooks {

    private static final String TAG = "LollipopStatusBarHooks";

    LollipopStatusBarHooks(ClassLoader classLoader) {
        super(classLoader);
    }

    @Override
    protected void hookSetMobileDataIndicators() {
        XposedBridge.hookAllMethods(mSignalClusterClass, "setMobileDataIndicators", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (mDataDisabled) {
                    int typeIcon = RomUtils.isCmBased() ? 3 : 2;
                    int isTypeIconWide = 6;
                    param.args[typeIcon] = SystemUIHooks.R_drawable_stat_sys_data_disabled;
                    param.args[isTypeIconWide] = false;
                }
            }
        });
    }

    @Override
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
