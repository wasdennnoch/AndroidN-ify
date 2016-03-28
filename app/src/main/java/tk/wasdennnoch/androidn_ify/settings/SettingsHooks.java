package tk.wasdennnoch.androidn_ify.settings;

import android.content.Context;
import android.os.Build;

import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.settings.summaries.SummaryTweaks;

public class SettingsHooks {

    private static final String TAG = "SettingsHooks";

    private static XC_MethodHook loadCategoriesFromResourceHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            SummaryTweaks.afterLoadCategoriesFromResource(param);
        }
    };

    public static void hook(ClassLoader classLoader, XSharedPreferences prefs) {
        try {
            prefs.reload();
            if (prefs.getBoolean("enable_settings_tweaks", true)) {

                Class<?> classSettingsActivity = XposedHelpers.findClass("com.android.settings.SettingsActivity", classLoader);

                SummaryTweaks.setFixSoundNotifTile(prefs.getBoolean("fix_sound_notif_tile", false));

                if (Build.VERSION.SDK_INT >= 23)
                    XposedHelpers.findAndHookMethod(classSettingsActivity, "loadCategoriesFromResource", int.class, List.class, Context.class, loadCategoriesFromResourceHook);
                else
                    XposedHelpers.findAndHookMethod(classSettingsActivity, "loadCategoriesFromResource", int.class, List.class, loadCategoriesFromResourceHook);

                // TODO performance testing
                /*XposedHelpers.findAndHookMethod(SettingsActivity, "getDashboardCategories", boolean.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        XposedHook.logD("getDashboardCategories args[0] = "+param.args[0]);
                        param.args[0] = false;
                    }
                });*/

            }
        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error in hook", t);
        }
    }

}
