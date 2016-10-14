package tk.wasdennnoch.androidn_ify;

import android.content.SharedPreferences;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import tk.wasdennnoch.androidn_ify.utils.ConfigUtils;

import static de.robv.android.xposed.XposedBridge.log;
import static de.robv.android.xposed.XposedHelpers.findAndHookConstructor;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getObjectField;

class AssistantHooks {

    private static final String GSA_PACKAGE = "com.google.android.apps.gsa";
    private static final String ASSISTANT_PACKAGE = GSA_PACKAGE + ".assistant";

    public static void hook(final ClassLoader classLoader) {
        try {
            if (!ConfigUtils.assistant().enable_assistant) {
                return;
            }

            /*
            findAndHookMethod(ASSISTANT_PACKAGE + ".settings.AssistantSettingsActivity", classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {

                }
            });

            findAndHookMethod(GSA_PACKAGE + ".shared.config.a.b", classLoader, "adY", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    callStaticMethod(findClass(GSA_PACKAGE + ".shared.config.a.c", classLoader), "B", 1518, true);
                    callStaticMethod(findClass(GSA_PACKAGE + ".shared.config.a.c", classLoader), "B", 1519, true);
                }
            });
            //*/

            Class a = findClass(ASSISTANT_PACKAGE + ".a.e", classLoader);

            findAndHookConstructor(a, findClass("com.google.android.apps.gsa.search.core.config.GsaConfigFlags", classLoader), SharedPreferences.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    SharedPreferences prefs = (SharedPreferences) getObjectField(param.thisObject, "bhX");
                    // Enable all prefs
                    prefs.edit().putBoolean("key_opa_eligible", true)
                            .putBoolean("opa_enabled", true)
                            .putBoolean("opa_hotword_enabled", true)
                            .putBoolean("opa_hotword_transition_seen", true).apply();
                }
            });

            // Don't allow to disable pref
            findAndHookMethod(a, "aG", boolean.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    param.args[0] = true;
                }
            });

            // Fake config and build.prop item
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

            // Enable for all languages, regardless of support from Google
            findAndHookMethod(a, "pb", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                    return true;
                }
            });

            // Fake build.prop item globally
            findAndHookMethod(GSA_PACKAGE + ".shared.util.c", classLoader, "v", String.class, boolean.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (param.args[0].toString().equals("ro.opa.eligible_device")) {
                        param.setResult(true);
                    }
                }
            });
        } catch (Throwable t) {
            log(t);
        }
    }
}