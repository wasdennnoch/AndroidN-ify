package tk.wasdennnoch.androidn_ify.notifications;

import android.content.res.XModuleResources;
import android.content.res.XResources;
import android.util.TypedValue;

import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.XposedHook;

public class NotificationsHooks {
    
    //private static final String PACKAGE_ANDROID = XposedHook.PACKAGE_ANDROID;
    private static final String PACKAGE_SYSTEMUI = XposedHook.PACKAGE_SYSTEMUI;

    private static final String TAG = "NotificationsHooks";

    public static void hookResSystemui(XC_InitPackageResources.InitPackageResourcesParam resparam, XSharedPreferences prefs, String modulePath) {
        try {
            if (prefs.getBoolean("enable_notification_tweaks", true)) {

                XModuleResources modRes = XModuleResources.createInstance(modulePath, resparam.res);
                XResources.DimensionReplacement zero = new XResources.DimensionReplacement(0, TypedValue.COMPLEX_UNIT_DIP);

                // Notifications
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "notification_side_padding", zero);
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "notifications_top_padding", zero);
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "notification_padding", zero);
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "notification_material_rounded_rect_radius", zero);
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "speed_bump_height", zero);

                // Drawables
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "drawable", "notification_header_bg", modRes.fwd(R.drawable.replacement_notification_header_bg));
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "drawable", "notification_guts_bg", modRes.fwd(R.drawable.replacement_notification_guts_bg));

                if (prefs.getBoolean("notification_dark_theme", false)) {
                    resparam.res.setReplacement(PACKAGE_SYSTEMUI, "drawable", "notification_material_bg", modRes.fwd(R.drawable.replacement_notification_material_bg_dark));
                    resparam.res.setReplacement(PACKAGE_SYSTEMUI, "drawable", "notification_material_bg_dim", modRes.fwd(R.drawable.replacement_notification_material_bg_dim_dark));
                    resparam.res.setReplacement(PACKAGE_SYSTEMUI, "color", "notification_material_background_low_priority_color", modRes.fwd(R.color.notification_material_background_low_priority_color_dark));
                    resparam.res.setReplacement(PACKAGE_SYSTEMUI, "color", "notification_material_background_media_default_color", modRes.fwd(R.color.notification_material_background_media_default_color_dark));
                    resparam.res.setReplacement(PACKAGE_SYSTEMUI, "color", "notification_ripple_color_low_priority", modRes.fwd(R.color.notification_ripple_color_low_priority_dark));
                } else {
                    resparam.res.setReplacement(PACKAGE_SYSTEMUI, "drawable", "notification_material_bg", modRes.fwd(R.drawable.replacement_notification_material_bg));
                    resparam.res.setReplacement(PACKAGE_SYSTEMUI, "drawable", "notification_material_bg_dim", modRes.fwd(R.drawable.replacement_notification_material_bg_dim));
                }
                
            }
        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error hooking SystemUI resources", t);
        }
    }

    public static void hookResAndroid(XC_InitPackageResources.InitPackageResourcesParam resparam, XSharedPreferences prefs) {
        /*try {
            if (prefs.getBoolean("enable_notification_tweaks", true)) {

                //TODO Notification styling in the future

                resparam.res.hookLayout(PACKAGE_ANDROID, "layout", "notification_action_list", new XC_LayoutInflated() {
                    @Override
                    public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                        XposedHook.logI(TAG, "notification_action_list");
                    }
                });
                resparam.res.hookLayout(PACKAGE_ANDROID, "layout", "notification_intruder_content", new XC_LayoutInflated() {
                    @Override
                    public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                        XposedHook.logI(TAG, "notification_intruder_content");
                    }
                });
                resparam.res.hookLayout(PACKAGE_ANDROID, "layout", "notification_material_action", new XC_LayoutInflated() {
                    @Override
                    public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                        XposedHook.logI(TAG, "notification_material_action");
                    }
                });
                resparam.res.hookLayout(PACKAGE_ANDROID, "layout", "notification_material_action_list", new XC_LayoutInflated() {
                    @Override
                    public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                        XposedHook.logI(TAG, "notification_material_action_list");
                    }
                });
                resparam.res.hookLayout(PACKAGE_ANDROID, "layout", "notification_material_action_tombstone", new XC_LayoutInflated() {
                    @Override
                    public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                        XposedHook.logI(TAG, "notification_material_action_tombstone");
                    }
                });
                resparam.res.hookLayout(PACKAGE_ANDROID, "layout", "notification_material_media_action", new XC_LayoutInflated() {
                    @Override
                    public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                        XposedHook.logI(TAG, "notification_material_media_action");
                    }
                });
                resparam.res.hookLayout(PACKAGE_ANDROID, "layout", "notification_template_icon_group", new XC_LayoutInflated() {
                    @Override
                    public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                        XposedHook.logI(TAG, "notification_template_icon_group");
                    }
                });
                resparam.res.hookLayout(PACKAGE_ANDROID, "layout", "notification_template_material_base", new XC_LayoutInflated() {
                    @Override
                    public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                        XposedHook.logI(TAG, "notification_template_material_base");
                    }
                });
                resparam.res.hookLayout(PACKAGE_ANDROID, "layout", "notification_template_material_big_base", new XC_LayoutInflated() {
                    @Override
                    public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                        XposedHook.logI(TAG, "notification_template_material_big_base");
                    }
                });
                resparam.res.hookLayout(PACKAGE_ANDROID, "layout", "notification_template_material_big_media", new XC_LayoutInflated() {
                    @Override
                    public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                        XposedHook.logI(TAG, "notification_template_material_big_media");
                    }
                });
                resparam.res.hookLayout(PACKAGE_ANDROID, "layout", "notification_template_material_big_media_narrow", new XC_LayoutInflated() {
                    @Override
                    public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                        XposedHook.logI(TAG, "notification_template_material_big_media_narrow");
                    }
                });
                resparam.res.hookLayout(PACKAGE_ANDROID, "layout", "notification_template_material_big_picture", new XC_LayoutInflated() {
                    @Override
                    public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                        XposedHook.logI(TAG, "notification_template_material_big_picture");
                    }
                });
                resparam.res.hookLayout(PACKAGE_ANDROID, "layout", "notification_template_material_big_text", new XC_LayoutInflated() {
                    @Override
                    public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                        XposedHook.logI(TAG, "notification_template_material_big_text");
                    }
                });
                resparam.res.hookLayout(PACKAGE_ANDROID, "layout", "notification_template_material_inbox", new XC_LayoutInflated() {
                    @Override
                    public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                        XposedHook.logI(TAG, "notification_template_material_inbox");
                    }
                });
                resparam.res.hookLayout(PACKAGE_ANDROID, "layout", "notification_template_material_media", new XC_LayoutInflated() {
                    @Override
                    public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                        XposedHook.logI(TAG, "notification_template_material_media");
                    }
                });
                resparam.res.hookLayout(PACKAGE_ANDROID, "layout", "notification_template_part_chronometer", new XC_LayoutInflated() {
                    @Override
                    public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                        XposedHook.logI(TAG, "notification_template_part_chronometer");
                    }
                });
                resparam.res.hookLayout(PACKAGE_ANDROID, "layout", "notification_template_part_line1", new XC_LayoutInflated() {
                    @Override
                    public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                        XposedHook.logI(TAG, "notification_template_part_line1");
                    }
                });
                resparam.res.hookLayout(PACKAGE_ANDROID, "layout", "notification_template_part_line2", new XC_LayoutInflated() {
                    @Override
                    public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                        XposedHook.logI(TAG, "notification_template_part_line2");
                    }
                });
                resparam.res.hookLayout(PACKAGE_ANDROID, "layout", "notification_template_part_line3", new XC_LayoutInflated() {
                    @Override
                    public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                        XposedHook.logI(TAG, "notification_template_part_line3");
                    }
                });
                resparam.res.hookLayout(PACKAGE_ANDROID, "layout", "notification_template_part_time", new XC_LayoutInflated() {
                    @Override
                    public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                        XposedHook.logI(TAG, "notification_template_part_time");
                    }
                });
                resparam.res.hookLayout(PACKAGE_ANDROID, "layout", "notification_template_progressbar", new XC_LayoutInflated() {
                    @Override
                    public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                        XposedHook.logI(TAG, "notification_template_progressbar");
                    }
                });

            }
        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error hooking framework resources", t);
        }*/
    }

}
