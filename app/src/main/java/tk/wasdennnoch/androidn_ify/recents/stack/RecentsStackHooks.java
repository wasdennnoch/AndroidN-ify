package tk.wasdennnoch.androidn_ify.recents.stack;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.utils.ConfigUtils;

public class RecentsStackHooks {

    private static final String TAG = "RecentsStackHooks";

    // TODO remove obsolete code snippets?
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

}
