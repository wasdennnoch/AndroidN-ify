package tk.wasdennnoch.androidn_ify.systemui.notifications;


import android.graphics.Canvas;
import android.graphics.Color;
import android.view.View;


import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.systemui.notifications.stack.NotificationStackScrollLayoutHooks;
import tk.wasdennnoch.androidn_ify.utils.ConfigUtils;

public class ScrimHooks {

    private static final String TAG = "ScrimHooks";
    private static final String PACKAGE_SYSTEMUI = XposedHook.PACKAGE_SYSTEMUI;

    public static void hook(ClassLoader classLoader) {
        if (!ConfigUtils.notifications().enable_notifications_background)
            return;
        try {
            final Class classScrimView = XposedHelpers.findClass(PACKAGE_SYSTEMUI + ".statusbar.ScrimView", classLoader);
            final Class classScrimController = XposedHelpers.findClass(PACKAGE_SYSTEMUI + ".statusbar.phone.ScrimController", classLoader);

            XposedHelpers.findAndHookMethod(classScrimView, "onDraw", Canvas.class, new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                    ScrimHelper helper = ScrimHelper.getInstance(param.thisObject);
                    helper.onDraw((Canvas) param.args[0]);
                    return null;
                }
            });

            XposedHelpers.findAndHookMethod(classScrimView, "setDrawAsSrc", boolean.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    ScrimHelper helper = ScrimHelper.getInstance(param.thisObject);
                    helper.setDrawAsSrc((boolean) param.args[0]);
                }
            });

            XposedHelpers.findAndHookMethod(classScrimView, "setScrimColor", int.class, new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                    int color = (int) param.args[0];
                    View scrimView = (View) param.thisObject;
                    ScrimHelper helper = ScrimHelper.getInstance(scrimView);
                    if (color != XposedHelpers.getIntField(scrimView, "mScrimColor")) {
                        XposedHelpers.setBooleanField(scrimView, "mIsEmpty", Color.alpha(color) == 0);
                        XposedHelpers.setIntField(scrimView, "mScrimColor", color);
                        scrimView.invalidate();
                        if (helper.mChangeRunnable != null) {
                            helper.mChangeRunnable.run();
                        }
                    }
                    return null;
                }
            });

            XposedBridge.hookAllMethods(classScrimController, "updateScrimBehindDrawingMode", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    View mBackDropView = (View) XposedHelpers.getObjectField(param.thisObject, "mBackDropView");
                    boolean asSrc = mBackDropView.getVisibility() != View.VISIBLE && XposedHelpers.getBooleanField(param.thisObject, "mScrimSrcEnabled");
                    NotificationStackScrollLayoutHooks.setDrawBackgroundAsSrc(asSrc);
                }
            });

        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error", t);
        }
    }
}

