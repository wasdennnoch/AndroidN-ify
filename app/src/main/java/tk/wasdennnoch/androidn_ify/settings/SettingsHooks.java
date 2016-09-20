package tk.wasdennnoch.androidn_ify.settings;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.UserManager;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.internal.os.BatteryStatsHelper;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.extracted.settingslib.BatteryInfo;
import tk.wasdennnoch.androidn_ify.extracted.settingslib.UsageView;
import tk.wasdennnoch.androidn_ify.settings.summaries.SummaryTweaks;
import tk.wasdennnoch.androidn_ify.ui.PlatLogoActivity;
import tk.wasdennnoch.androidn_ify.utils.ConfigUtils;
import tk.wasdennnoch.androidn_ify.utils.ResourceUtils;

public class SettingsHooks {

    private static final String TAG = "SettingsHooks";
    private static final String KEY_APP_DETAILS_CATEGORY = "AppDetailsCategory";
    private static final String KEY_APP_DETAILS = "app_details";
    private static final String KEY_BATTERY_INFO = "battery_info";
    private static final String KEY_CHART = "chart";

    private static final long[] mHits = new long[3];

    public static void hook(ClassLoader classLoader) {
        try {
            ConfigUtils config = ConfigUtils.getInstance();
            config.reload();
            if (config.settings.enable_summaries) {
                SummaryTweaks.hookMethods(classLoader);
                if (ConfigUtils.M) {
                    Class<?> classBatteryHistoryPreference = XposedHelpers.findClass("com.android.settings.fuelgauge.BatteryHistoryPreference", classLoader);

                    XposedHelpers.findAndHookMethod(classBatteryHistoryPreference, "setStats", BatteryStatsHelper.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            Preference preference = (Preference) param.thisObject;
                            BatteryStatsHelper batteryStats = (BatteryStatsHelper) param.args[0];
                            final long elapsedRealtimeUs = SystemClock.elapsedRealtime() * 1000;
                            XposedHelpers.setAdditionalInstanceField(preference, KEY_BATTERY_INFO, BatteryInfo.getBatteryInfo(preference.getContext(), batteryStats.getBatteryBroadcast(),
                                    batteryStats.getStats(), elapsedRealtimeUs));
                        }
                    });

                    XposedHelpers.findAndHookMethod(classBatteryHistoryPreference, "onBindView", View.class, new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                            bindBatteryHistoryPreference((Preference) param.thisObject,(View) param.args[0]);
                            return null;
                        }
                    });
                }
            }
            if (config.settings.enable_n_platlogo) {
                Class<?> classDeviceInfoSettings = XposedHelpers.findClass("com.android.settings.DeviceInfoSettings", classLoader);
                XposedHelpers.findAndHookMethod(classDeviceInfoSettings, "onPreferenceTreeClick", PreferenceScreen.class, Preference.class, onPreferenceTreeClickHook);
            }
            if (config.settings.install_source) {
                Class<?> classInstalledAppDetails = XposedHelpers.findClass("com.android.settings.applications.InstalledAppDetails", classLoader);
                if (ConfigUtils.M) {
                    XposedHelpers.findAndHookMethod(classInstalledAppDetails, "onActivityCreated", Bundle.class, onActivityCreatedHook);
                } else {
                    XposedHelpers.findAndHookMethod(classInstalledAppDetails, "onCreateView", LayoutInflater.class, ViewGroup.class, Bundle.class, onCreateViewHook);
                }
                XposedHelpers.findAndHookMethod(classInstalledAppDetails, "refreshUi", refreshUiHook);
            }
        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error in hook", t);
        }
    }

    @SuppressWarnings("deprecation")
    private static void bindBatteryHistoryPreference(Preference preference, View view) {
        Object field = XposedHelpers.getAdditionalInstanceField(preference, KEY_BATTERY_INFO);
        Object mChart = XposedHelpers.getAdditionalInstanceField(preference, KEY_CHART);
        if (field != null && field instanceof BatteryInfo) {
            if (view.findViewById(R.id.battery_usage) == null) {
                FrameLayout oldLayout = (FrameLayout) view;
                oldLayout.removeAllViews();

                Context context = oldLayout.getContext();
                ResourceUtils res = ResourceUtils.getInstance(context);

                LinearLayout layout = (LinearLayout) LayoutInflater.from(ResourceUtils.createOwnContext(context)).inflate(R.layout.battery_usage_graph, oldLayout, false);

                TypedValue colorAccent = new TypedValue();
                TypedValue textColorSecondary = new TypedValue();
                context.getTheme().resolveAttribute(android.R.attr.colorAccent, colorAccent, true);
                context.getTheme().resolveAttribute(android.R.attr.textColorSecondary, textColorSecondary, true);

                int colorAccentValue;
                try {
                    colorAccentValue = context.getResources().getColor(colorAccent.resourceId);
                } catch (Throwable ignore) {
                    colorAccentValue = 0xFF009688; // default colorAccent for material light
                }
                int textColorSecondaryValue;
                try {
                    textColorSecondaryValue = context.getResources().getColor(textColorSecondary.resourceId);
                } catch (Throwable ignore) {
                    textColorSecondaryValue = 0x8A000000; // default textColorSecondary for material light
                }

                ((TextView) layout.findViewById(R.id.charge)).setTextColor(colorAccentValue);
                ((TextView) layout.findViewById(R.id.estimation)).setTextColor(textColorSecondaryValue);

                if (mChart == null || !(mChart instanceof UsageView)) {
                    LinearLayout.LayoutParams usageViewLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, res.getDimensionPixelSize(R.dimen.battery_usage_height));
                    UsageView usageView = new UsageView(context, textColorSecondaryValue,
                            colorAccentValue, true);
                    usageView.setId(R.id.battery_usage);
                    usageView.setLayoutParams(usageViewLp);
                    usageView.setSideLabels(res.getResources().getTextArray(R.array.battery_labels));
                    usageView.findViewById(R.id.label_group).setAlpha(.7f);
                    mChart = usageView;
                    XposedHelpers.setAdditionalInstanceField(preference, KEY_CHART, usageView);
                } else {
                    UsageView usageView = (UsageView) mChart;
                    if (usageView.getParent() != null) {
                        ((ViewGroup) usageView.getParent()).removeView(usageView);
                    }
                    mChart = usageView;
                }
                layout.addView((View) mChart);

                oldLayout.addView(layout);
            }

            BatteryInfo batteryInfo = (BatteryInfo) field;

            ((TextView) view.findViewById(R.id.charge)).setText(batteryInfo.batteryPercentString);
            ((TextView) view.findViewById(R.id.estimation)).setText(batteryInfo.remainingLabel);
            UsageView usageView = (UsageView) view.findViewById(R.id.battery_usage);
            batteryInfo.bindHistory(usageView);
        }
    }

    private static final XC_MethodHook onPreferenceTreeClickHook = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            Preference preference = (Preference) param.args[1];
            PreferenceFragment fragment = (PreferenceFragment) param.thisObject;

            XposedHook.logD(TAG, "onPreferenceTreeClick " + mHits[0]);

            final String LOG_TAG = "DeviceInfoSettings";
            final String KEY_FIRMWARE_VERSION = "firmware_version";

            if (preference.getKey().equals(KEY_FIRMWARE_VERSION)) {
                System.arraycopy(mHits, 1, mHits, 0, mHits.length - 1);
                mHits[mHits.length - 1] = SystemClock.uptimeMillis();
                if (mHits[0] >= (SystemClock.uptimeMillis() - 500)) {
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
    private static final XC_MethodHook onCreateViewHook = new XC_MethodHook() {
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
            //TextView header = new TextView(new ContextThemeWrapper(context, com.android.internal.R.style.Widget_DeviceDefault_TextView_ListSeparator), null, 0);
            //TextView header = new TextView(context, null, com.android.internal.R.attr.listSeparatorTextViewStyle, com.android.internal.R.style.Widget_DeviceDefault_TextView_ListSeparator);
            TextView header = new TextView(context, null, com.android.internal.R.attr.listSeparatorTextViewStyle);
            header.setLayoutParams(headerLp);
            header.setText(res.getString(R.string.store));

            LinearLayout.LayoutParams contentLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            //int ps = context.getResources().getDimensionPixelSize(attrToDimension(context, com.android.internal.R.attr.listPreferredItemPaddingStart));
            //int pe = context.getResources().getDimensionPixelSize(attrToDimension(context, com.android.internal.R.attr.listPreferredItemPaddingEnd));
            int ps = context.getResources().getDimensionPixelSize(attrToDimension(context, context.getResources().getIdentifier("listPreferredItemPaddingStart", "attr", XposedHook.PACKAGE_ANDROID)));
            int pe = context.getResources().getDimensionPixelSize(attrToDimension(context, context.getResources().getIdentifier("listPreferredItemPaddingEnd", "attr", XposedHook.PACKAGE_ANDROID)));
            TextView content = new TextView(context);
            content.setLayoutParams(contentLp);
            content.setPadding(ps, 0, pe, 0);

            panel.addView(header);
            panel.addView(content);
            allDetails.addView(panel);

            XposedHelpers.setAdditionalInstanceField(param.thisObject, KEY_APP_DETAILS, content);
        }
    };

    private static int attrToDimension(Context context, int attr) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(attr, typedValue, true);
        TypedArray a = context.obtainStyledAttributes(typedValue.resourceId, new int[]{attr});
        int textSize = a.getDimensionPixelSize(0, 0);
        a.recycle();
        return textSize;
    }

    // MM
    private static final XC_MethodHook onActivityCreatedHook = new XC_MethodHook() {
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

    private static final XC_MethodHook refreshUiHook = new XC_MethodHook() {
        @SuppressWarnings("ConstantConditions")
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            if (!(boolean) param.getResult()) return;
            Context context;
            String packageName;
            PackageManager pm;
            Preference appDetailsPreference = null;
            TextView lpText = null;
            if (ConfigUtils.M) {
                PreferenceCategory storeCat = (PreferenceCategory) XposedHelpers.getAdditionalInstanceField(param.thisObject, KEY_APP_DETAILS_CATEGORY);
                if (storeCat == null) return;
                appDetailsPreference = storeCat.findPreference(KEY_APP_DETAILS);
                if (appDetailsPreference == null) return;
                packageName = (String) XposedHelpers.getObjectField(param.thisObject, "mPackageName");
            } else {
                lpText = (TextView) XposedHelpers.getAdditionalInstanceField(param.thisObject, KEY_APP_DETAILS);
                if (lpText == null) return;
                packageName = ((PackageInfo) XposedHelpers.getObjectField(param.thisObject, "mPackageInfo")).packageName;
            }
            context = ((Fragment) param.thisObject).getActivity();
            pm = (PackageManager) XposedHelpers.getObjectField(param.thisObject, "mPm");
            String installerName = pm.getInstallerPackageName(packageName);
            ResourceUtils res = ResourceUtils.getInstance(context);
            String text;
            if (installerName != null) {
                try {
                    ApplicationInfo applicationInfo = pm.getApplicationInfo(installerName, 0);
                    String installerLabel = (String) pm.getApplicationLabel(applicationInfo);
                    text = String.format(res.getString(R.string.install_source), installerLabel);
                } catch (Throwable ignore) {
                    text = res.getString(R.string.from_pm);
                }
            } else {
                text = res.getString(R.string.from_pm);
            }
            if (ConfigUtils.M)
                appDetailsPreference.setSummary(text);
            else
                lpText.setText(text);
        }
    };

}
