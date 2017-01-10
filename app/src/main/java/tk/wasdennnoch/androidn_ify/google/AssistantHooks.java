package tk.wasdennnoch.androidn_ify.google;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.utils.ConfigUtils;

import static de.robv.android.xposed.XposedBridge.hookAllConstructors;
import static de.robv.android.xposed.XposedBridge.log;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getObjectField;

public class AssistantHooks {

    private static final String GSA_PACKAGE = "com.google.android.apps.gsa";
    private static final String ASSISTANT_PACKAGE = GSA_PACKAGE + ".assistant";

    private static final String KEY_VERSION = "version";
    private static final String KEY_ASSISTANT_CLASS = "assistant_class";
    private static final String KEY_PREFS = "prefs";
    private static final String KEY_SET_PREFS = "set_prefs";
    private static final String KEY_FAKE_CONFIG = "fake_config";
    private static final String KEY_FAKE_CONFIG_TWO = "fake_config_two";
    private static final String KEY_ENABLE_LANGS = "enable_langs";

    public static void hook(final ClassLoader classLoader) {
        try {
            ((XSharedPreferences) ConfigUtils.getInstance().getPrefs()).reload();
            // #############################################################################
            // Thanks to XposedGELSettings for the following snippet (https://git.io/vP2Gw):
            Object activityThread = XposedHelpers.callStaticMethod(XposedHelpers.findClass("android.app.ActivityThread", null), "currentActivityThread");
            Context context = (Context) XposedHelpers.callMethod(activityThread, "getSystemContext");
            // #############################################################################
            String googleVersionName = context.getPackageManager().getPackageInfo(XposedHook.PACKAGE_GOOGLE, 0).versionName;

            JSONArray hookConfigs = new JSONArray(ConfigUtils.assistant().google_app_hook_configs);
            JSONObject activeHooksTemp = null;
            for (int i = 0; i < hookConfigs.length(); i++) {
                if (hookConfigs.optInt(i, -1) != -1)
                    continue;
                if (googleVersionName.matches(hookConfigs.getJSONObject(i).optString(KEY_VERSION))) {
                    activeHooksTemp = hookConfigs.getJSONObject(i);
                }
            }
            if (activeHooksTemp == null) {
                return;
            }
            final JSONObject activeHooks = activeHooksTemp;

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

            Class a = findClass(ASSISTANT_PACKAGE + activeHooks.optString(KEY_ASSISTANT_CLASS, ".a.e"), classLoader);

            hookAllConstructors(a, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    SharedPreferences prefs = (SharedPreferences) getObjectField(param.thisObject, activeHooks.optString(KEY_PREFS, "bhX"));
                    // Enable all prefs
                    prefs.edit().putBoolean("key_opa_eligible", true)
                            .putBoolean("opa_enabled", true)
                            .putBoolean("opa_hotword_enabled", true)
                            .putBoolean("opa_hotword_transition_seen", true).apply();
                }
            });

            // Don't allow to disable pref
            findAndHookMethod(a, activeHooks.optString(KEY_SET_PREFS, "aG"), boolean.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    param.args[0] = true;
                }
            });

            // Fake config and build.prop item
            findAndHookMethod(a, activeHooks.optString(KEY_FAKE_CONFIG, "pa"), new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                    return true;
                }
            });

            findAndHookMethod(a, activeHooks.optString(KEY_FAKE_CONFIG_TWO, "oZ"), new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                    return true;
                }
            });

            // Enable for all languages, regardless of support from Google
            findAndHookMethod(a, activeHooks.optString(KEY_ENABLE_LANGS, "pb"), new XC_MethodReplacement() {
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
