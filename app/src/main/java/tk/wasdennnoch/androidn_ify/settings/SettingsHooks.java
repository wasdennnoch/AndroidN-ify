package tk.wasdennnoch.androidn_ify.settings;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;
import android.os.UserManager;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.util.Log;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.settings.summaries.SummaryTweaks;
import tk.wasdennnoch.androidn_ify.ui.PlatLogoActivity;
import tk.wasdennnoch.androidn_ify.utils.ConfigUtils;

public class SettingsHooks {

    private static final String TAG = "SettingsHooks";

    private static long[] mHits = new long[3];

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
            if (config.settings.enable_n_platlogo) {
                Class<?> classDeviceInfoSettings = XposedHelpers.findClass("com.android.settings.DeviceInfoSettings", classLoader);
                XposedHelpers.findAndHookMethod(classDeviceInfoSettings, "onPreferenceTreeClick", PreferenceScreen.class, Preference.class, onPreferenceTreeClickHook);
            }
        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error in hook", t);
        }
    }

    private static XC_MethodHook onPreferenceTreeClickHook = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            Preference preference = (Preference) param.args[1];
            PreferenceFragment fragment = (PreferenceFragment) param.thisObject;

            XposedHook.logI(TAG, "onPreferenceTreeClick" + mHits[0]);

            final String LOG_TAG = "DeviceInfoSettings";
            final String KEY_FIRMWARE_VERSION = "firmware_version";

            if (preference.getKey().equals(KEY_FIRMWARE_VERSION)) {
                System.arraycopy(mHits, 1, mHits, 0, mHits.length-1);
                mHits[mHits.length-1] = SystemClock.uptimeMillis();
                if (mHits[0] >= (SystemClock.uptimeMillis()-500)) {
                    if (Build.VERSION.SDK_INT >= 23) {
                        UserManager um = (UserManager) fragment.getActivity().getSystemService(Context.USER_SERVICE);
                        if (um.hasUserRestriction(UserManager.DISALLOW_FUN)) {
                            Log.d(LOG_TAG, "Sorry, no fun for you!");
                            param.setResult(false);
                        }
                    }

                    Intent intent = new Intent(Intent.ACTION_MAIN);
                    intent.setClassName("tk.wasdennnoch.androidn_ify",
                            PlatLogoActivity.class.getName());

                    try {
                        fragment.startActivity(intent);
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "Unable to start activity " + intent.toString());
                    }
                    param.setResult(true);
                }
            }
        }
    };

}
