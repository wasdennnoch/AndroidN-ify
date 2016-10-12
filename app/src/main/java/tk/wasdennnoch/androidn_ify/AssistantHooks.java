package tk.wasdennnoch.androidn_ify;

import android.content.SharedPreferences;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import tk.wasdennnoch.androidn_ify.utils.ConfigUtils;

import static de.robv.android.xposed.XposedBridge.log;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookConstructor;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;

class AssistantHooks {

    private static final String ASSISTANT_PACKAGE = "com.google.android.apps.gsa.assistant";

    public static void hook(ClassLoader classLoader) {
        try {
            if (!ConfigUtils.assistant().enable_assistant) {
                return;
            }

            Class a = findClass(ASSISTANT_PACKAGE + ".a.e", classLoader);

            findAndHookConstructor(a, findClass("com.google.android.apps.gsa.search.core.config.GsaConfigFlags", classLoader), SharedPreferences.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    callMethod(param.thisObject, "aG", true);
                }
            });

            findAndHookMethod(a, "aG", boolean.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    param.args[0] = true;
                }
            });

            findAndHookMethod(a, "pa", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                    return true;
                }
            });

            findAndHookMethod(a, "oZ", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                    return true;
                }
            });
        } catch (Throwable t) {
            log(t);
        }
    }
}