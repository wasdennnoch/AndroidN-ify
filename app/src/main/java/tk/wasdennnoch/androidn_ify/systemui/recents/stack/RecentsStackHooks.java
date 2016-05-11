package tk.wasdennnoch.androidn_ify.systemui.recents.stack;

import android.content.res.XModuleResources;

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

            }
        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error hooking SystemUI", t);
        }
    }

    public static void hookResSystemui(XC_InitPackageResources.InitPackageResourcesParam resparam, String modulePath) {
        try {
            ConfigUtils config = ConfigUtils.getInstance();
            if (config.recents.no_recents_image) {
                XModuleResources modRes = XModuleResources.createInstance(modulePath, resparam.res);
                try {
                    resparam.res.setReplacement(PACKAGE_SYSTEMUI, "layout", "recents_empty", modRes.fwd(R.layout.recents_empty));
                } catch (Throwable t) {
                    // AICP
                    resparam.res.setReplacement(PACKAGE_SYSTEMUI, "layout", "no_recents_holder", modRes.fwd(R.layout.recents_empty));
                }
            }
        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error hooking SystemUI resources", t);
        }
    }

}
