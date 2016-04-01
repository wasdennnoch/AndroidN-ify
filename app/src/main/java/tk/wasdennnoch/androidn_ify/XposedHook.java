package tk.wasdennnoch.androidn_ify;

import android.content.res.XResources;
import android.util.TypedValue;
import android.widget.LinearLayout;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LayoutInflated;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import tk.wasdennnoch.androidn_ify.recents.doubletap.DoubleTapHwKeys;
import tk.wasdennnoch.androidn_ify.recents.doubletap.DoubleTapSwKeys;
import tk.wasdennnoch.androidn_ify.settings.SettingsHooks;

public class XposedHook implements IXposedHookLoadPackage, IXposedHookZygoteInit, IXposedHookInitPackageResources {

    private static final String TAG = "[Android N-ify]";
    public static final String PACKAGE_ANDROID = "android";
    public static final String PACKAGE_SYSTEMUI = "com.android.systemui";
    public static final String PACKAGE_SETTINGS = "com.android.settings";
    public static boolean debug = true;
    //private static String sModulePath;

    private static XSharedPreferences sPrefs = new XSharedPreferences("tk.wasdennnoch.androidn_ify");

    public static void logE(String tag, String msg, Throwable t) {
        XposedBridge.log(TAG + " [ERROR] " + tag + ": " + msg);
        if (t != null)
            XposedBridge.log(t);
    }

    public static void logW(String tag, String msg) {
        XposedBridge.log(TAG + " [WARNING] " + tag + ": " + msg);
    }

    public static void log(String tag, String msg) {
        XposedBridge.log(TAG + " " + tag + ": " + msg);
    }

    public static void logD(String tag, String msg) {
        if (debug) XposedBridge.log(TAG + " [DEBUG] " + tag + ": " + msg);
    }

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        //sModulePath = startupParam.modulePath;
        debug = sPrefs.getBoolean("debug_log", false);
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {

        switch (lpparam.packageName) {
            case PACKAGE_SETTINGS:
                SettingsHooks.hook(lpparam.classLoader, sPrefs);
                break;
            case PACKAGE_SYSTEMUI:
                DoubleTapSwKeys.hook(lpparam.classLoader, sPrefs);
                break;
            case PACKAGE_ANDROID:
                DoubleTapHwKeys.hook(lpparam.classLoader, sPrefs);
                break;
        }

    }

    @Override
    public void handleInitPackageResources(XC_InitPackageResources.InitPackageResourcesParam resparam) throws Throwable {

        if (sPrefs.getBoolean("enable_notification_tweaks", true)) {
            XResources.DimensionReplacement zero = new XResources.DimensionReplacement(0, TypedValue.COMPLEX_UNIT_DIP);
            if (resparam.packageName.equals(PACKAGE_SYSTEMUI)) {
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "qs_peek_height", zero);
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "notification_side_padding", zero);
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "notifications_top_padding", zero);
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "notification_padding", zero);
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "notification_material_rounded_rect_radius", zero);

                //resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "notification_material_rounded_rect_radius_negative", zero);
                //resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "notification_collapse_second_card_padding", new XResources.DimensionReplacement(-2, TypedValue.COMPLEX_UNIT_DIP)); // WRAP_CONTENT
                //resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "notification_children_padding", zero);
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "notification_children_divider_height", new XResources.DimensionReplacement(50, TypedValue.COMPLEX_UNIT_DIP));

                //resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "header_notifications_collide_distance", new XResources.DimensionReplacement(150, TypedValue.COMPLEX_UNIT_DIP)); // Testing

                resparam.res.hookLayout("com.android.systemui", "layout", "status_bar_expanded_header", new XC_LayoutInflated() {
                    @Override
                    public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                        liparam.view.setPadding(0, 0, 0, 0);
                    }
                });
                resparam.res.hookLayout("com.android.systemui", "layout", "qs_panel", new XC_LayoutInflated() {
                    @Override
                    public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                        ((LinearLayout.LayoutParams) liparam.view.getLayoutParams()).setMarginStart(0);
                        ((LinearLayout.LayoutParams) liparam.view.getLayoutParams()).setMarginEnd(0);
                    }
                });
            }

        }

    }

}
