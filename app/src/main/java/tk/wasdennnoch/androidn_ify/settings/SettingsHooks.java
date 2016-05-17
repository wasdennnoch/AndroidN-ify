package tk.wasdennnoch.androidn_ify.settings;

import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.settings.summaries.SummaryTweaks;
import tk.wasdennnoch.androidn_ify.utils.ConfigUtils;

public class SettingsHooks {

    private static final String TAG = "SettingsHooks";

    /*private static XC_MethodHook onCreateHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            SummaryTweaks.afterOnCreate(param);
        }
    };*/

    public static void hook(ClassLoader classLoader) {
        try {
            ConfigUtils config = ConfigUtils.getInstance();
            config.reload();
            if (config.settings.enable_summaries) {

                SummaryTweaks.hookMethods(classLoader);

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
