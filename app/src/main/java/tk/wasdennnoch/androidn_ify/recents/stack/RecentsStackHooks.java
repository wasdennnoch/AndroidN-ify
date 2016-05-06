package tk.wasdennnoch.androidn_ify.recents.stack;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.XposedHook;

public class RecentsStackHooks {

    private static final String TAG = "RectnesStackHooks";

    public static void hookSystemUI(ClassLoader classLoader, XSharedPreferences prefs) {
        try {
            if (prefs.getBoolean("enable_large_recents", true)) {
                Class classTaskStackViewLayoutAlgorithm = XposedHelpers.findClass("com.android.systemui.recents.views.TaskStackViewLayoutAlgorithm", classLoader);

                XposedHelpers.findAndHookMethod(classTaskStackViewLayoutAlgorithm, "curveProgressToScale", float.class, XC_MethodReplacement.returnConstant(1f));
            }
        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error hooking SystemUI", t);
        }
    }

}
