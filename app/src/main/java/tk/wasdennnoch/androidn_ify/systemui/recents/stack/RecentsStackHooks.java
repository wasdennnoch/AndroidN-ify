package tk.wasdennnoch.androidn_ify.systemui.recents.stack;

import android.content.Context;
import android.content.res.XModuleResources;
import android.content.res.XResources;
import android.util.TypedValue;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.utils.ConfigUtils;

public class RecentsStackHooks {

    private static final String TAG = "RecentsStackHooks";
    private static final String PACKAGE_SYSTEMUI = XposedHook.PACKAGE_SYSTEMUI;

    // TODO find a way to change recent apps height
    /*
    private static XC_MethodHook computeRectsHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            Rect mStackRect = (Rect) XposedHelpers.getObjectField(param.thisObject, "mStackRect");
            Rect mTaskRect = (Rect) XposedHelpers.getObjectField(param.thisObject, "mTaskRect");

            int size = mStackRect.width();
            int sizeY = mStackRect.height();
            int left = mStackRect.left + (mStackRect.width() - size) / 2;
            mTaskRect.set(left, mStackRect.top,
                    left + size, mStackRect.top + sizeY);

            // Update the affiliation offsets
            float visibleTaskPct = 0.5f;
            //mWithinAffiliationOffset = mConfig.taskBarHeight;
            //mBetweenAffiliationOffset = (int) (visibleTaskPct * mTaskRect.height());
        }
    };

    private static XC_MethodHook computeStackRectsHook = new XC_MethodHook() {
        @SuppressWarnings("unchecked")
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            ArrayList<Rect> bounds = (ArrayList<Rect>) param.getResult();
            for (Rect bound : bounds) {
                int top = bound.top;
                int bottom = bound.bottom;
                int height = bottom - top;
                bound.bottom = top + (int) (height * 1.5f);
            }
        }
    };
    */

    public static void hookSystemUI(ClassLoader classLoader) {
        try {
            if (ConfigUtils.recents().large_recents) {
                Class<?> classTaskStackViewLayoutAlgorithm = XposedHelpers.findClass("com.android.systemui.recents.views.TaskStackViewLayoutAlgorithm", classLoader);
                //Class<?> classRecentsViewLayoutAlgorithm  = XposedHelpers.findClass("com.android.systemui.recents.views.RecentsViewLayoutAlgorithm ", classLoader);

                XposedHelpers.findAndHookMethod(classTaskStackViewLayoutAlgorithm, "curveProgressToScale", float.class, XC_MethodReplacement.returnConstant(1f));
                /*
                XposedHelpers.findAndHookMethod(classTaskStackViewLayoutAlgorithm, "computeRects", int.class, int.class, Rect.class, computeRectsHook);
                XposedHelpers.findAndHookMethod(classRecentsViewLayoutAlgorithm, "computeStackRects", List.class, Rect.class, computeStackRectsHook);
                */

                // Bliss...
                XposedHelpers.findAndHookMethod("com.android.systemui.recents.RecentsConfiguration", classLoader, "update", Context.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            XposedHelpers.setIntField(param.thisObject, "taskViewRoundedCornerRadiusPx",
                                    (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, ((Context) param.args[0]).getResources().getDisplayMetrics()));
                        } catch (Throwable ignore) {
                        }
                    }
                });
            }
        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error hooking SystemUI", t);
        }
    }

    public static void hookResSystemui(XC_InitPackageResources.InitPackageResourcesParam resparam, String modulePath) {
        try {
            ConfigUtils config = ConfigUtils.getInstance();
            XModuleResources modRes = XModuleResources.createInstance(modulePath, resparam.res);
            if (config.recents.no_recents_image) {
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "recents_stack_top_padding", modRes.fwd(R.dimen.recents_stack_top_padding));
                try {
                    resparam.res.setReplacement(PACKAGE_SYSTEMUI, "layout", "recents_empty", modRes.fwd(R.layout.recents_empty));
                } catch (Throwable t) {
                    // AICP
                    resparam.res.setReplacement(PACKAGE_SYSTEMUI, "layout", "no_recents_holder", modRes.fwd(R.layout.recents_empty));
                }
            }
            if (config.recents.large_recents) {
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "recents_stack_top_padding", modRes.fwd(R.dimen.recents_stack_top_padding));
                // srsly bliss? 12 dp looks ugly as hell...
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "recents_task_view_rounded_corners_radius", new XResources.DimensionReplacement(2, TypedValue.COMPLEX_UNIT_DIP));
            }
        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error hooking SystemUI resources", t);
        }
    }

}
