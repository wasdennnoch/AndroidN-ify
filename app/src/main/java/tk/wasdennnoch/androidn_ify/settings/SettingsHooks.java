package tk.wasdennnoch.androidn_ify.settings;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.UserManager;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.util.Log;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.settings.summaries.SummaryTweaks;
import tk.wasdennnoch.androidn_ify.ui.PlatLogoActivity;
import tk.wasdennnoch.androidn_ify.utils.ConfigUtils;
import tk.wasdennnoch.androidn_ify.utils.ResourceUtils;

public class SettingsHooks {

    private static final String TAG = "SettingsHooks";
    private static final String KEY_APP_DETAILS_CATEGORY = "AppDetailsCategory";
    private static final String KEY_APP_DETAILS = "app_details";

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
            }
            if (config.settings.enable_n_platlogo) {
                Class<?> classDeviceInfoSettings = XposedHelpers.findClass("com.android.settings.DeviceInfoSettings", classLoader);
                XposedHelpers.findAndHookMethod(classDeviceInfoSettings, "onPreferenceTreeClick", PreferenceScreen.class, Preference.class, onPreferenceTreeClickHook);
            }
            if (config.settings.install_source) {
                Class<?> classInstalledAppDetails = XposedHelpers.findClass("com.android.settings.applications.InstalledAppDetails", classLoader);
                if (ConfigUtils.M) {
                    XposedHelpers.findAndHookMethod(classInstalledAppDetails, "onActivityCreated", Bundle.class, onActivityCreatedHook);
                }/* else {
                    XposedHelpers.findAndHookMethod(classInstalledAppDetails, "onCreateView", LayoutInflater.class, ViewGroup.class, Bundle.class, onCreateViewHook);
                }*/
                XposedHelpers.findAndHookMethod(classInstalledAppDetails, "refreshUi", refreshUiHook);
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
                    if (ConfigUtils.M) {
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

    // LP
    /*private static XC_MethodHook onCreateViewHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {

            View view = (View) param.getResult();
            Context context = view.getContext();
            ResourceUtils res = ResourceUtils.getInstance(context);
            LinearLayout allDetails = (LinearLayout) view.findViewById(
                    context.getResources().getIdentifier("all_details", "id", XposedHook.PACKAGE_SETTINGS));

            LinearLayout.LayoutParams panelLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            LinearLayout panel = new LinearLayout(context);
            panel.setLayoutParams(panelLp);
            panel.setOrientation(LinearLayout.VERTICAL);

            LinearLayout.LayoutParams headerLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            TextView header = new TextView(context);
            header.setLayoutParams(headerLp);
            header.setText(res.getString(R.string.store));

        }
    };*/

    // MM
    private static XC_MethodHook onActivityCreatedHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            PreferenceFragment fragment = (PreferenceFragment) param.thisObject;
            if (XposedHelpers.getAdditionalInstanceField(fragment, KEY_APP_DETAILS_CATEGORY) == null) {
                Activity context = fragment.getActivity();
                ResourceUtils res = ResourceUtils.getInstance(context);

                PreferenceScreen root = fragment.getPreferenceScreen();

                PreferenceCategory storeCat = new PreferenceCategory(context);
                storeCat.setTitle(res.getString(R.string.store));
                root.addPreference(storeCat);

                Preference appDetailsPreference = new Preference(context);
                appDetailsPreference.setTitle(res.getString(R.string.app_details));
                appDetailsPreference.setEnabled(false);
                appDetailsPreference.setKey(KEY_APP_DETAILS);
                storeCat.addPreference(appDetailsPreference);

                XposedHelpers.setAdditionalInstanceField(fragment, KEY_APP_DETAILS_CATEGORY, storeCat);
            }
        }
    };

    private static XC_MethodHook refreshUiHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            PreferenceFragment fragment = (PreferenceFragment) param.thisObject;
            PreferenceCategory storeCat = (PreferenceCategory) XposedHelpers.getAdditionalInstanceField(fragment, KEY_APP_DETAILS_CATEGORY);
            if (storeCat == null) return;
            Preference appDetailsPreference = storeCat.findPreference(KEY_APP_DETAILS);
            if (appDetailsPreference == null) return;
            String packageName = (String) XposedHelpers.getObjectField(fragment, "mPackageName");
            PackageManager pm = (PackageManager) XposedHelpers.getObjectField(fragment, "mPm");
            String installerName = pm.getInstallerPackageName(packageName);
            Activity context = fragment.getActivity();
            ResourceUtils res = ResourceUtils.getInstance(context);
            if (installerName != null) {
                try {
                    ApplicationInfo applicationInfo = pm.getApplicationInfo(installerName, 0);
                    String installerLabel = (String) pm.getApplicationLabel(applicationInfo);
                    appDetailsPreference.setSummary(String.format(res.getString(R.string.install_source), installerLabel));
                } catch (Throwable ignore) {
                    appDetailsPreference.setSummary(res.getString(R.string.from_pm));
                }
            } else {
                appDetailsPreference.setSummary(res.getString(R.string.from_pm));
            }
        }
    };

}
