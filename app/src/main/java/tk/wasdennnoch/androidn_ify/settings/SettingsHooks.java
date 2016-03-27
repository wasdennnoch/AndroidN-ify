package tk.wasdennnoch.androidn_ify.settings;

import android.content.Context;
import android.os.Build;

import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
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

    public static void hookLoadCategoriesFromResource(XC_LoadPackage.LoadPackageParam lpparam) {

        Class<?> SettingsActivity = XposedHelpers.findClass("com.android.settings.SettingsActivity", lpparam.classLoader);

        try {
            if (Build.VERSION.SDK_INT >= 23)
                XposedHelpers.findAndHookMethod(SettingsActivity, "loadCategoriesFromResource", int.class, List.class, Context.class, loadCategoriesFromResourceHook);
            else
                XposedHelpers.findAndHookMethod(SettingsActivity, "loadCategoriesFromResource", int.class, List.class, loadCategoriesFromResourceHook);

            /*XposedHelpers.findAndHookMethod(SettingsActivity, "getDashboardCategories", boolean.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    XposedHook.logD("getDashboardCategories args[0] = "+param.args[0]);
                    param.args[0] = false;
                }
            });*/

        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error hooking loadCategoriesFromResource", t);
        }


    }

}
