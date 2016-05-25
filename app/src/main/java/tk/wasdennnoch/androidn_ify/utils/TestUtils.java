package tk.wasdennnoch.androidn_ify.utils;

import android.view.View;

import java.lang.reflect.Method;

import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.XposedHook;

@SuppressWarnings({"unused", "PointlessBooleanExpression"})
public class TestUtils {

    private static final String TAG = "TestUtils";

    private static final boolean QSTILEVIEW_INIT_SEARCH = false;
    private static final boolean QSTILE_GETSTATE_SEARCH = false;

    public static void searchQSTileView_Init(ClassLoader classLoader) {
        if (!QSTILEVIEW_INIT_SEARCH) return;
        XposedHook.logI(TAG, "TEST START");
        try {
            XposedHook.logI(TAG, "QSTileView init() search");
            try {
                Method[] methods = XposedHelpers.findMethodsByExactParameters(XposedHelpers.findClass("com.android.systemui.qs.QSTileView", classLoader), void.class, View.OnClickListener.class);
                logSearchResult(methods, "with one parameter");
            } catch (Throwable t5) {
                XposedHook.logE(TAG, "Error init() search w/ one param", t5);
            }
            try {
                Method[] methods = XposedHelpers.findMethodsByExactParameters(XposedHelpers.findClass("com.android.systemui.qs.QSTileView", classLoader), void.class, View.OnClickListener.class, View.OnClickListener.class);
                logSearchResult(methods, "with two parameters");
            } catch (Throwable t5) {
                XposedHook.logE(TAG, "Error init() search w/ two params", t5);
            }
            try {
                Method[] methods = XposedHelpers.findMethodsByExactParameters(XposedHelpers.findClass("com.android.systemui.qs.QSTileView", classLoader), void.class, View.OnClickListener.class, View.OnClickListener.class, View.OnClickListener.class);
                logSearchResult(methods, "with three parameters");
            } catch (Throwable t5) {
                XposedHook.logE(TAG, "Error init() search w/ three params", t5);
            }
        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error test", t);
        }
        XposedHook.logI(TAG, "TEST END");
    }

    public static void searchQSTile_GetState(ClassLoader classLoader) {
        if (!QSTILE_GETSTATE_SEARCH) return;
        XposedHook.logI(TAG, "TEST START");
        try {
            XposedHook.logI(TAG, "QSTile getState() search");
            try {
                Method[] methods = XposedHelpers.findMethodsByExactParameters(XposedHelpers.findClass("com.android.systemui.qs.QSTile", classLoader), null);
                logSearchResult(methods, "without any parameters");
            } catch (Throwable t5) {
                XposedHook.logE(TAG, "Error getState() search", t5);
            }
        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error test", t);
        }
        XposedHook.logI(TAG, "TEST END");
    }

    private static void logSearchResult(Method[] methods, String additionalInfo) {
        XposedHook.logI(TAG, "Found " + methods.length + " matches of methods " + additionalInfo);
        for (int i = 0; i < methods.length; i++) {
            XposedHook.logI(TAG, "  " + (i + 1) + ". - " + methods[i].getName());
        }
    }

}
