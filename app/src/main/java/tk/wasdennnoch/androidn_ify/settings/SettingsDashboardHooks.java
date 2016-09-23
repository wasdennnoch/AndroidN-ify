package tk.wasdennnoch.androidn_ify.settings;

import android.content.Context;
import android.content.res.XModuleResources;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LayoutInflated;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.settings.misc.DashboardOverlay;
import tk.wasdennnoch.androidn_ify.utils.ViewUtils;

import static tk.wasdennnoch.androidn_ify.XposedHook.PACKAGE_SETTINGS;

public class SettingsDashboardHooks {

    private static final String TAG = "SettingsDashboardHooks";
    private static final String CLASS_DASHBOARD_SUMMARY = "com.android.settings.dashboard.DashboardSummary";
    private static final String CLASS_DASHBOARD_TILE_VIEW = "com.android.settings.dashboard.DashboardTileView";

    public void hook(ClassLoader classLoader) {
        try {
            Class<?> classDashboardSummary = XposedHelpers.findClass(CLASS_DASHBOARD_SUMMARY, classLoader);
            Class<?> classDashboardTileView = XposedHelpers.findClass(CLASS_DASHBOARD_TILE_VIEW, classLoader);

            XposedHelpers.findAndHookMethod(classDashboardSummary, "onCreateView", LayoutInflater.class, ViewGroup.class, Bundle.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    ScrollView scrollView = (ScrollView) param.getResult();
                    LinearLayout layout = (LinearLayout) scrollView.getChildAt(0);
                    scrollView.removeView(layout);
                    FrameLayout container = new FrameLayout(scrollView.getContext());
                    container.addView(layout);
                    DashboardOverlay overlay = new DashboardOverlay(scrollView.getContext());
                    overlay.setDashboardLayout(layout);
                    container.addView(overlay);
                    scrollView.addView(container);
                }
            });

            XposedHelpers.findAndHookConstructor(classDashboardTileView, Context.class, AttributeSet.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    View divider = (View) XposedHelpers.getObjectField(param.thisObject, "mDivider");
                    divider.setVisibility(View.GONE);
                }
            });
            XposedHelpers.findAndHookMethod(classDashboardTileView, "setDividerVisibility", boolean.class, XC_MethodReplacement.DO_NOTHING);
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
