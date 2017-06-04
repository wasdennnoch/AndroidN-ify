package tk.wasdennnoch.androidn_ify.systemui.statusbar;

import android.content.Context;
import android.content.res.XModuleResources;
import android.os.Build;
import android.view.View;

import java.lang.reflect.Array;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.systemui.SystemUIHooks;
import tk.wasdennnoch.androidn_ify.utils.ConfigUtils;

import static tk.wasdennnoch.androidn_ify.XposedHook.PACKAGE_SYSTEMUI;

public class StatusBarHooks {

    private static final String TAG = "StatusBarHooks";

    private static final String CLASS_SIGNAL_CONTROLLER = "com.android.systemui.statusbar.policy.SignalController";
    private static final String CLASS_SIGNAL_CLUSTER_VIEW = "com.android.systemui.statusbar.SignalClusterView";
    private static final String CLASS_MOBILE_SIGNAL_CONTROLLER = "com.android.systemui.statusbar.policy.MobileSignalController";
    private static final String CLASS_MOBILE_DATA_CONTROLLER_51 = "com.android.systemui.statusbar.policy.MobileDataControllerImpl";
    private static final String CLASS_MOBILE_DATA_CONTROLLER_51_MOTO = "com.android.systemui.statusbar.policy.MotorolaMobileDataControllerImpl"; // yeah thx Motorola
    private static final String CLASS_MOBILE_DATA_CONTROLLER_50 = "com.android.systemui.statusbar.policy.MobileDataController";
    private static final String CLASS_NAVIGATION_BAR_TRANSITIONS = "com.android.systemui.statusbar.phone.NavigationBarTransitions";
    private static final String CLASS_NAVIGATION_BAR_VIEW = "com.android.systemui.statusbar.phone.NavigationBarView";

    public static final int LIGHTS_IN_DURATION = 250;
    public static final int LIGHTS_OUT_DURATION = 750;

    ClassLoader mClassLoader;
    Class<?> mSignalClusterClass;
    private Class<?> mMobileDataControllerClass;
    private boolean mLastDataDisabled = false;
    boolean mDataDisabled = false;

    private Object mPhone;

    public static StatusBarHooks create(ClassLoader classLoader) {
        if (ConfigUtils.M) {
            return new StatusBarHooks(classLoader);
        } else {
            return new LollipopStatusBarHooks(classLoader);
        }
    }

    StatusBarHooks(ClassLoader classLoader) {
        try {
            mClassLoader = classLoader;
            mSignalClusterClass = XposedHelpers.findClass(CLASS_SIGNAL_CLUSTER_VIEW, mClassLoader);
            try {
                mMobileDataControllerClass = XposedHelpers.findClass(getMobileDataControllerClass(), mClassLoader);
            } catch (Throwable t) { // Motorola
                try {
                    mMobileDataControllerClass = XposedHelpers.findClass(CLASS_MOBILE_DATA_CONTROLLER_51_MOTO, mClassLoader);
                } catch (Throwable t2) { // Xperia 5.1.1
                    mMobileDataControllerClass = XposedHelpers.findClass(CLASS_MOBILE_DATA_CONTROLLER_50, mClassLoader);
                }
            }
            if (ConfigUtils.M)
                hookIsDirty();
            hookSetMobileDataIndicators();
            hookUpdateTelephony();
            hookSetMobileDataEnabled();
            hookApplyLightsOut();
            if (ConfigUtils.others().slippery_navbar)
                hookNavigationBar();
        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error in <init>", t);
        }
    }

    private void hookNavigationBar() {
        try {
            Class classNavigationBarView = XposedHelpers.findClass(CLASS_NAVIGATION_BAR_VIEW, mClassLoader);
            XposedHelpers.findAndHookMethod(classNavigationBarView, "setSlippery", boolean.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    param.args[0] = true;
                }
            });
        } catch (Throwable t) {
            XposedHook.logE(TAG, "Can't hook NavigationBarView.setSlippery", t);
        }
    }

    private void hookApplyLightsOut() {
        try {
            Class classNavigationBarTransitions = XposedHelpers.findClass(CLASS_NAVIGATION_BAR_TRANSITIONS, mClassLoader);
            XposedHelpers.findAndHookMethod(classNavigationBarTransitions, "applyLightsOut", boolean.class, boolean.class, boolean.class, new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                    boolean lightsOut = (boolean) param.args[0], animate = (boolean) param.args[1], force = (boolean) param.args[2];
                    if (!force && lightsOut == XposedHelpers.getBooleanField(param.thisObject, "mLightsOut")) return null;

                    XposedHelpers.setBooleanField(param.thisObject, "mLightsOut", lightsOut);

                    View layout = (View) XposedHelpers.callMethod(XposedHelpers.getObjectField(param.thisObject, "mView"), "getCurrentView");
                    Context context = layout.getContext();
                    final View navButtons = layout.findViewById(context.getResources().getIdentifier("nav_buttons", "id", PACKAGE_SYSTEMUI));

                    // ok, everyone, stop it right there
                    navButtons.animate().cancel();

                    final float navButtonsAlpha = lightsOut ? 0.5f : 1f;

                    if (!animate) {
                        navButtons.setAlpha(navButtonsAlpha);
                    } else {
                        final int duration = lightsOut ? LIGHTS_OUT_DURATION : LIGHTS_IN_DURATION;
                        navButtons.animate()
                                .alpha(navButtonsAlpha)
                                .setDuration(duration)
                                .start();
                    }
                    return null;
                }
            });
        } catch (Throwable t) {
            XposedHook.logE(TAG, "Can't hook applyLightsOut", t);
        }
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
                boolean isDataEnabled;
                try {
                    isDataEnabled = (boolean) XposedHelpers.callMethod(param.thisObject, "isMobileDataEnabled");
                } catch (Throwable t) {
                    // TODO implement better dual-sim handling?
                    isDataEnabled = (boolean) XposedHelpers.callMethod(param.thisObject, "isMobileDataEnabled",
                            XposedHelpers.callMethod(
                                    XposedHelpers.getObjectField(param.thisObject, "mTelephonyManager"),
                                    "getSubId"));
                }
                mDataDisabled = !isDataEnabled;
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

    private XC_MethodHook setMobileDataEnabledHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            mDataDisabled = !(boolean) param.args[0];
        }
    };
    private XC_MethodHook setMobileDataEnabledHook2 = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            mDataDisabled = !(boolean) param.args[1];
        }
    };
    private void hookSetMobileDataEnabled() {
        try {
            XposedHelpers.findAndHookMethod(mMobileDataControllerClass, "setMobileDataEnabled", boolean.class, setMobileDataEnabledHook);
        } catch (Throwable t) { // multi-sim
            // TODO implement better dual-sim handling
            XposedHelpers.findAndHookMethod(mMobileDataControllerClass, "setMobileDataEnabled", int.class, boolean.class, setMobileDataEnabledHook2);
        }
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

                    Class mediaTekIconIdWrapperClass = null;
                    try {
                        mediaTekIconIdWrapperClass = XposedHelpers.findClass("com.mediatek.systemui.ext.IconIdWrapper", mClassLoader);
                        XposedHook.logI(TAG, "Identified MediaTek device");
                    } catch (XposedHelpers.ClassNotFoundError ignore) {
                    }
                    if (mediaTekIconIdWrapperClass != null && mediaTekIconIdWrapperClass.isInstance(param.args[typeIcon])) { // MediaTek devices wrap the id
                        Object iconIdWrapperArray = Array.newInstance(mediaTekIconIdWrapperClass, 1);
                        Array.set(iconIdWrapperArray, 0, XposedHelpers.newInstance(mediaTekIconIdWrapperClass, XModuleResources.createInstance(XposedHook.getModulePath(), null), SystemUIHooks.R_drawable_stat_sys_data_disabled));
                        param.args[typeIcon] = iconIdWrapperArray;

                        Object iconIdWrapperArray2 = Array.newInstance(mediaTekIconIdWrapperClass, 1);
                        Array.set(iconIdWrapperArray2, 0, XposedHelpers.newInstance(mediaTekIconIdWrapperClass, XModuleResources.createInstance(XposedHook.getModulePath(), null), SystemUIHooks.R_drawable_ic_qs_data_disabled));
                        param.args[qsTypeIcon] = iconIdWrapperArray2;
                    } else {
                        param.args[typeIcon] = SystemUIHooks.R_drawable_stat_sys_data_disabled;
                        param.args[qsTypeIcon] = SystemUIHooks.R_drawable_ic_qs_data_disabled;
                    }
                    if (param.args[isWide] instanceof Boolean) {
                        param.args[isWide] = false;
                    } else if (param.args[isWide] instanceof Boolean) { // Xperia put a load of ints in between
                        param.args[12] = false;
                    } else {
                        XposedHook.logD(TAG, "setMobileDataIndicators: Didn't find isWide param");
                    }
                }
            }
        });
    }

    private String getMobileDataControllerClass() {
        return Build.VERSION.SDK_INT >= 22 ? CLASS_MOBILE_DATA_CONTROLLER_51 : CLASS_MOBILE_DATA_CONTROLLER_50;
    }
}
