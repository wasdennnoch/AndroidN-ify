package tk.wasdennnoch.androidn_ify;

import android.content.res.XModuleResources;
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
import tk.wasdennnoch.androidn_ify.statusbar.header.StatusBarHeaderHooks;

public class XposedHook implements IXposedHookLoadPackage, IXposedHookZygoteInit, IXposedHookInitPackageResources {

    private static final String LOG_FORMAT = "[Android N-ify] %1$s %2$s: %3$s";
    public static final String PACKAGE_ANDROID = "android";
    public static final String PACKAGE_SYSTEMUI = "com.android.systemui";
    public static final String PACKAGE_SETTINGS = "com.android.settings";
    public static boolean debug = true;
    private static String sModulePath;

    private static XSharedPreferences sPrefs = new XSharedPreferences(XposedHook.class.getPackage().getName());

    public static void logE(String tag, String msg, Throwable t) {
        XposedBridge.log(String.format(LOG_FORMAT, "[ERROR]", tag, msg));
        if (t != null)
            XposedBridge.log(t);
    }

    public static void logW(String tag, String msg) {
        XposedBridge.log(String.format(LOG_FORMAT, "[WARNING]", tag, msg));
    }

    @SuppressWarnings("unused")
    public static void logI(String tag, String msg) {
        XposedBridge.log(String.format(LOG_FORMAT, "[INFO]", tag, msg));
    }

    public static void logD(String tag, String msg) {
        if (debug) XposedBridge.log(String.format(LOG_FORMAT, "[DEBUG]", tag, msg));
    }

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        sModulePath = startupParam.modulePath;
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
                StatusBarHeaderHooks.hook(lpparam.classLoader, sPrefs);
                break;
            case PACKAGE_ANDROID:
                DoubleTapHwKeys.hook(lpparam.classLoader, sPrefs);
                break;
        }

    }

    @Override
    public void handleInitPackageResources(XC_InitPackageResources.InitPackageResourcesParam resparam) throws Throwable {

        if (resparam.packageName.equals(PACKAGE_SYSTEMUI)) {
            if (sPrefs.getBoolean("enable_notification_tweaks", true)) {

                XModuleResources modRes = XModuleResources.createInstance(sModulePath, resparam.res);

                XResources.DimensionReplacement zero = new XResources.DimensionReplacement(0, TypedValue.COMPLEX_UNIT_DIP);

                // Notifications
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "qs_peek_height", zero);
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "notification_side_padding", zero);
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "notifications_top_padding", zero);
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "notification_padding", zero);
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "notification_material_rounded_rect_radius", zero);

                // Panel
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "status_bar_header_height", new XResources.DimensionReplacement(72, TypedValue.COMPLEX_UNIT_DIP));
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "status_bar_header_height_expanded", new XResources.DimensionReplacement(96, TypedValue.COMPLEX_UNIT_DIP));

                // Multi user switch
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "multi_user_switch_width_collapsed", new XResources.DimensionReplacement(48, TypedValue.COMPLEX_UNIT_DIP));
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "multi_user_switch_width_expanded", new XResources.DimensionReplacement(48, TypedValue.COMPLEX_UNIT_DIP));
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "multi_user_avatar_collapsed_size", new XResources.DimensionReplacement(24, TypedValue.COMPLEX_UNIT_DIP));
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "multi_user_avatar_expanded_size", new XResources.DimensionReplacement(24, TypedValue.COMPLEX_UNIT_DIP));

                resparam.res.hookLayout("com.android.systemui", "layout", "status_bar_expanded_header", new XC_LayoutInflated() {
                    @Override
                    public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                        liparam.view.setElevation(0);
                        liparam.view.setPadding(0, 0, 0, 0);
                    }
                });
                resparam.res.hookLayout("com.android.systemui", "layout", "qs_panel", new XC_LayoutInflated() {
                    @Override
                    public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                        liparam.view.setElevation(0);
                        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) liparam.view.getLayoutParams();
                        params.setMarginStart(0);
                        params.setMarginEnd(0);
                    }
                });

                // Colors
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "color", "qs_tile_divider", 0x00FFFFFF);

                // Drawables
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "drawable", "notification_header_bg", modRes.fwd(R.drawable.replacement_notification_header_bg));

                if (resparam.packageName.equals(PACKAGE_SYSTEMUI)) {
                    if (sPrefs.getBoolean("dark_theme_toggle", true)) {

                        resparam.res.setReplacement(PACKAGE_SYSTEMUI, "drawable", "notification_material_bg", modRes.fwd(R.drawable.replacement_notification_material_dark_bg));
                        resparam.res.setReplacement(PACKAGE_SYSTEMUI, "drawable", "notification_material_bg_dim", modRes.fwd(R.drawable.replacement_notification_material_dark_bg_dim));
                    } else {

                        resparam.res.setReplacement(PACKAGE_SYSTEMUI, "drawable", "notification_material_bg", modRes.fwd(R.drawable.replacement_notification_material_bg));
                        resparam.res.setReplacement(PACKAGE_SYSTEMUI, "drawable", "notification_material_bg_dim", modRes.fwd(R.drawable.replacement_notification_material_bg_dim));
                    }
                }
            }
        }
    }
}
