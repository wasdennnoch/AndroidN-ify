package tk.wasdennnoch.androidn_ify.settings.summaries;

import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import java.lang.reflect.Method;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.settings.summaries.categories.DeviceTweaks;
import tk.wasdennnoch.androidn_ify.settings.summaries.categories.PersonalTweaks;
import tk.wasdennnoch.androidn_ify.settings.summaries.categories.RomTweaks;
import tk.wasdennnoch.androidn_ify.settings.summaries.categories.SystemTweaks;
import tk.wasdennnoch.androidn_ify.settings.summaries.categories.WirelessAndNetworksTweaks;
import tk.wasdennnoch.androidn_ify.utils.ConfigUtils;
import tk.wasdennnoch.androidn_ify.utils.ResourceUtils;

public class SummaryTweaks {

    private static final String TAG = "SummaryTweaks";
    private static boolean sFixSoundNotifTile;
    //private static Handler sHandler;

    // All tiles. Tiles without a subtitle are commented out to improve performance.
    // They will be removed completely in the near future.

    // WIRELESS and NETWORKS
    private static int wifi_settings;
    private static int bluetooth_settings;
    //private static int sim_settings;
    private static int data_usage_settings;
    //private static int operator_settings;
    //private static int wireless_settings;

    // DEVICE
    //private static int home_settings;
    private static int display_settings;
    private static int notification_settings;
    private static int sound_settings;
    private static int application_settings;
    private static int storage_settings;
    private static int battery_settings;
    private static int manage_memory;
    private static int user_settings;
    //private static int nfc_payment_settings; //TODO this tile. Neither available on my device nor in my whole country, so, yeah.
    //private static int manufacturer_settings;

    // PERSONAL
    private static int location_settings;
    //private static int security_settings;
    //private static int account_settings;
    private static int language_settings;
    //private static int privacy_settings; // TODO last tile: "backup & reset"

    // SYSTEM
    private static int date_time_settings;
    //private static int accessibility_settings;
    //private static int print_settings; // TODO Temporary disabled, takes very long time to process, async required
    //private static int development_settings;
    private static int about_settings;

    // rom specific
    //private static int mobile_networks;
    //private static int main_settings;
    //private static int audiofx_settings;
    //private static int viper_settings;
    //private static int theme_settings;
    //private static int kernel_adiutor;
    private static int display_and_lights_settings;
    //private static int notification_manager;

    //private static int oclick;
    //private static int device_specific_gesture_settings;
    //private static int profile_settings;
    //private static int privacy_settings_cyanogenmod;
    //private static int supersu_settings;

    /*public static void afterOnCreate(XC_MethodHook.MethodHookParam param) {
        sHandler = (Handler) XposedHelpers.getObjectField(param.thisObject, "mHandler");
    }*/

    private static XC_MethodHook loadCategoriesFromResourceHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            afterLoadCategoriesFromResource(param);
        }
    };

    private static XC_MethodHook updateTileViewHook = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            beforeUpdateTileView(param);
        }
    };

    public static void hookMethods(ClassLoader classLoader) {

        Class<?> classSettingsActivity = XposedHelpers.findClass("com.android.settings.SettingsActivity", classLoader);
        Class<?> classDashboardSummary = XposedHelpers.findClass("com.android.settings.dashboard.DashboardSummary", classLoader);

        //XposedHelpers.findAndHookMethod(classSettingsActivity, "onCreate", Bundle.class, onCreateHook);

        if (Build.VERSION.SDK_INT >= 23)
            XposedHelpers.findAndHookMethod(classSettingsActivity, "loadCategoriesFromResource", int.class, List.class, Context.class, loadCategoriesFromResourceHook);
        else
            XposedHelpers.findAndHookMethod(classSettingsActivity, "loadCategoriesFromResource", int.class, List.class, loadCategoriesFromResourceHook);


        try {
            // Stock Android
            XposedHelpers.findAndHookMethod(classDashboardSummary, "updateTileView", Context.class, Resources.class, "com.android.settings.dashboard.DashboardTile", ImageView.class, TextView.class, TextView.class, updateTileViewHook);
        } catch (Throwable t) {
            // CyanogenMod
            try {
                XposedHelpers.findAndHookMethod(classDashboardSummary, "updateTileView", Context.class, Resources.class, "com.android.settings.dashboard.DashboardTile", ImageView.class, TextView.class, TextView.class, Switch.class, updateTileViewHook);
            } catch (Throwable t1) {
                // Touchwiz
                try {
                    XposedHelpers.findAndHookMethod(classDashboardSummary, "updateTileView", Context.class, Resources.class, "com.android.settings.dashboard.DashboardTile", ImageView.class, TextView.class, TextView.class, int.class, updateTileViewHook);
                    XposedHelpers.findAndHookMethod(classSettingsActivity, "updateTileView", Context.class, Resources.class, "com.android.settings.dashboard.DashboardTile", ImageView.class, TextView.class, TextView.class, int.class, updateTileViewHook);
                } catch (Throwable t2) {
                    // Touchwiz
                    try {
                        XposedHelpers.findAndHookMethod(classDashboardSummary, "updateTileView", Context.class, Resources.class, "com.android.settings.dashboard.DashboardTile", ImageView.class, TextView.class, int.class, updateTileViewHook);
                        XposedHelpers.findAndHookMethod(classSettingsActivity, "updateTileView", Context.class, Resources.class, "com.android.settings.dashboard.DashboardTile", ImageView.class, TextView.class, int.class, updateTileViewHook);
                    } catch (Throwable t3) {
                        // Sony
                        try {
                            XposedHelpers.findAndHookMethod(classDashboardSummary, "updateTileView", Context.class, Resources.class, "com.android.settings.dashboard.DashboardTile", ImageView.class, TextView.class, updateTileViewHook);
                        } catch (Throwable t4) {
                            // Other method name
                            try {
                                Method[] updateTileView = XposedHelpers.findMethodsByExactParameters(classDashboardSummary, void.class, Context.class, Resources.class, XposedHelpers.findClass("com.android.settings.dashboard.DashboardTile", classLoader), ImageView.class, TextView.class, TextView.class);
                                XposedHook.logI(TAG, "Found " + updateTileView.length + " matches using findMethodsByExactParameters to find updateTileView");
                                if (updateTileView.length == 1) {
                                    XposedHook.logI(TAG, "Hooking method with name " + updateTileView[0].getName());
                                    XposedHelpers.findAndHookMethod(classDashboardSummary, updateTileView[0].getName(), Context.class, Resources.class, "com.android.settings.dashboard.DashboardTile", ImageView.class, TextView.class, TextView.class, updateTileViewHook);
                                }
                            } catch (Throwable t5) {
                                XposedHook.logE(TAG, "Error hooking updateTileView", t5);
                            }
                        }
                    }
                }
            }
        }
    }


    public static void beforeUpdateTileView(XC_MethodHook.MethodHookParam param) {
        Context context = (Context) param.args[0];
        Object tile = param.args[2];
        setSummary(tile, context);
    }

    public static void afterLoadCategoriesFromResource(XC_MethodHook.MethodHookParam param) {
        try {

            sFixSoundNotifTile = ConfigUtils.settings().fix_sound_notif_tile;

            Context context;
            if (Build.VERSION.SDK_INT >= 23)
                context = (Context) param.args[2];
            else
                context = (Context) param.thisObject; // Surrounding activity

            setupIds(context);
            ResourceUtils.getInstance(context); // Setup instance

        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error in afterLoadCategoriesFromResource", t);
        }
    }

    private static void setupIds(Context context) {
        long startTime = System.currentTimeMillis();

        wifi_settings = getId(context, "wifi_settings");
        bluetooth_settings = getId(context, "bluetooth_settings");
        //sim_settings = getId(context, "sim_settings");
        data_usage_settings = getId(context, "data_usage_settings");
        //operator_settings = getId(context, "operator_settings");
        //wireless_settings = getId(context, "wireless_settings");

        //home_settings = getId(context, "home_settings");
        display_settings = getId(context, "display_settings");
        notification_settings = getId(context, "notification_settings");
        sound_settings = getId(context, "sound_settings");
        application_settings = getId(context, "application_settings");
        storage_settings = getId(context, "storage_settings");
        battery_settings = getId(context, "battery_settings");
        manage_memory = getId(context, "manage_memory");
        user_settings = getId(context, "user_settings");
        //nfc_payment_settings = getId(context, "nfc_payment_settings");
        //manufacturer_settings = getId(context, "manufacturer_settings");

        location_settings = getId(context, "location_settings");
        //security_settings = getId(context, "security_settings");
        //account_settings = getId(context, "account_settings");
        language_settings = getId(context, "language_settings");
        //privacy_settings = getId(context, "privacy_settings");

        date_time_settings = getId(context, "date_time_settings");
        //accessibility_settings = getId(context, "accessibility_settings");
        //print_settings = getId(context, "print_settings");
        //development_settings = getId(context, "development_settings");
        about_settings = getId(context, "about_settings");

        //mobile_networks = getId(context, "mobile_networks");
        //main_settings = getId(context, "main_settings");
        //audiofx_settings = getId(context, "audiofx_settings");
        //viper_settings = getId(context, "viper_settings");
        //theme_settings = getId(context, "theme_settings");
        //kernel_adiutor = getId(context, "kernel_adiutor");
        display_and_lights_settings = getId(context, "display_and_lights_settings");
        //notification_manager = getId(context, "notification_manager");
        //oclick = getId(context, "oclick");
        //device_specific_gesture_settings = getId(context, "device_specific_gesture_settings");
        //profile_settings = getId(context, "profile_settings");
        //privacy_settings_cyanogenmod = getId(context, "privacy_settings_cyanogenmod");
        //supersu_settings = getId(context, "supersu_settings");

        XposedHook.logD(TAG, "Fetching ids took " + (System.currentTimeMillis() - startTime) + "ms");
    }

    private static void setSummary(Object tile, Context context) {
        int id;
        String tileId;
        long startTime;
        id = (int) XposedHelpers.getLongField(tile, "id");
        if (id == -1)
            return;
        startTime = System.currentTimeMillis();
        tileId = "";

        if (id == wifi_settings) {
            tileId = "wifi_settings";
            WirelessAndNetworksTweaks.hookWifiTile(tile, context);
        } else if (id == bluetooth_settings) {
            tileId = "bluetooth_settings";
            WirelessAndNetworksTweaks.hookBluetoothTile(tile);
            //} else if (id == sim_settings) {
            //    XposedHelpers.setObjectField(tile, "summary", "sim_settings");
        } else if (id == data_usage_settings) {
            tileId = "data_usage_settings";
            WirelessAndNetworksTweaks.hookDataUsageTile(tile, context);
            //} else if (id == operator_settings) {
            //    XposedHelpers.setObjectField(tile, "summary", "operator_settings");
            //} else if (id == wireless_settings) {
            //    XposedHelpers.setObjectField(tile, "summary", "wireless_settings");

            //} else if (id == home_settings) {
            //    XposedHelpers.setObjectField(tile, "summary", "home_settings");
        } else if (id == display_settings) {
            tileId = "display_settings";
            DeviceTweaks.hookDisplayTile(tile, context);
        } else if (id == notification_settings) {
            tileId = "notification_settings";
            if (sFixSoundNotifTile)
                DeviceTweaks.hookSoundTile(tile, context);
        } else if (id == sound_settings) {
            tileId = "sound_settings";
            DeviceTweaks.hookSoundTile(tile, context);
        } else if (id == application_settings) {
            tileId = "application_settings";
            DeviceTweaks.hookApplicationTile(tile, context);
        } else if (id == storage_settings) {
            tileId = "storage_settings";
            DeviceTweaks.hookStorageTile(tile, context);
        } else if (id == battery_settings) {
            tileId = "battery_settings";
            DeviceTweaks.hookBatteryTile(tile, context);
        } else if (id == manage_memory) {
            tileId = "manage_memory";
            DeviceTweaks.hookMemoryTile(tile, context);
        } else if (id == user_settings) {
            tileId = "user_settings";
            DeviceTweaks.hookUserTile(tile, context);
            //} else if (id == nfc_payment_settings) {
            //    XposedHelpers.setObjectField(tile, "summary", "nfc_payment_settings");
            //} else if (id == manufacturer_settings) { // In Bliss: last position in wireless and networks
            //    XposedHelpers.setObjectField(tile, "summary", "manufacturer_settings");

        } else if (id == location_settings) {
            tileId = "location_settings";
            PersonalTweaks.hookLocationTile(tile, context);
            //} else if (id == security_settings) {
            //    XposedHelpers.setObjectField(tile, "summary", "security_settings");
            //} else if (id == account_settings) {
            //    XposedHelpers.setObjectField(tile, "summary", "account_settings");
        } else if (id == language_settings) {
            tileId = "language_settings";
            PersonalTweaks.hookLanguageTile(tile);
            //} else if (id == privacy_settings) {
            //    tileId = "privacy_settings";
            //    XposedHelpers.setObjectField(tile, "summary", "privacy_settings");
            // When backup is enabled, it shows the email of the backup account. That's all I know.

        } else if (id == date_time_settings) {
            tileId = "date_time_settings";
            SystemTweaks.hookDateTimeTile(tile);
            //} else if (id == accessibility_settings) {
            //    XposedHelpers.setObjectField(tile, "summary", "accessibility_settings");
            //} else if (id == print_settings) {
            //    tileId = "print_settings";
            //    SystemTweaks.hookPrintTile(tile, context);
            //} else if (id == development_settings) {
            //    XposedHelpers.setObjectField(tile, "summary", "development_settings");
        } else if (id == about_settings) {
            tileId = "about_settings";
            SystemTweaks.hookAboutTile(tile);

            //} else if (id == mobile_networks) {
            //    XposedHelpers.setObjectField(tile, "summary", "mobile_networks");
            //} else if (id == main_settings) {
            //    XposedHelpers.setObjectField(tile, "summary", "main_settings");
            //} else if (id == audiofx_settings) {
            //    XposedHelpers.setObjectField(tile, "summary", "audiofx_settings");
            //} else if (id == viper_settings) {
            //    XposedHelpers.setObjectField(tile, "summary", "viper_settings");
            //} else if (id == theme_settings) {
            //    XposedHelpers.setObjectField(tile, "summary", "theme_settings");
            //} else if (id == kernel_adiutor) {
            //    XposedHelpers.setObjectField(tile, "summary", "kernel_adiutor");
        } else if (id == display_and_lights_settings) {
            tileId = "display_and_lights_settings";
            RomTweaks.hookDisplayAndLightsTile(tile, context);
            //} else if (id == notification_manager) {
            //    XposedHelpers.setObjectField(tile, "summary", "notification_manager");
        }

        if (!tileId.equals(""))
            XposedHook.logD(TAG, "Hooking tile '" + tileId + "' took " + (System.currentTimeMillis() - startTime) + "ms");

    }

    private static int getId(Context context, String name) {
        return context.getResources().getIdentifier(name, "id", context.getPackageName());
    }

}
