package tk.wasdennnoch.androidn_ify.settings;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LayoutInflated;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.settings.misc.SettingsActivityHelper;
import tk.wasdennnoch.androidn_ify.utils.ViewUtils;

import static tk.wasdennnoch.androidn_ify.XposedHook.PACKAGE_SETTINGS;

public class SettingsDrawerHooks {

    private static final String TAG = "SettingsDashboardHooks";
    private static final String CLASS_SETTINGS_ACTIVITY = "com.android.settings.SettingsActivity";

    public void hook(ClassLoader classLoader) {
        try {
            Class<?> classSettingsActivity = XposedHelpers.findClass(CLASS_SETTINGS_ACTIVITY, classLoader);

            XposedHelpers.findAndHookMethod(classSettingsActivity, "onCreate", Bundle.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    Activity activity = (Activity) param.thisObject;
                    activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                    activity.requestWindowFeature(Window.FEATURE_NO_TITLE);
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    new SettingsActivityHelper((Activity) param.thisObject);
                }
            });
        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error while hooking settings dashboard", t);
        }
    }

    public void hookRes(XC_InitPackageResources.InitPackageResourcesParam resparam, String modulePath) {
        try {
            //XModuleResources modRes = XModuleResources.createInstance(modulePath, resparam.res);
            resparam.res.hookLayout(PACKAGE_SETTINGS, "layout", "dashboard_category", new XC_LayoutInflated() {
                @Override
                public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                    LinearLayout layout = (LinearLayout) liparam.view;
                    layout.setElevation(0);
                    ViewUtils.setMarginBottom(layout, 0);

                    Context context = layout.getContext();

                    TypedValue textColorSecondary = new TypedValue();
                    context.getTheme().resolveAttribute(android.R.attr.textColorSecondary, textColorSecondary, true);
                    int textColorSecondaryValue = context.getResources().getColor(textColorSecondary.resourceId);

                    TextView title = (TextView) layout.getChildAt(0);
                    title.setTextColor(textColorSecondaryValue);
                }
            });

        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error while hooking settings dashboard res", t);
        }
    }
}
