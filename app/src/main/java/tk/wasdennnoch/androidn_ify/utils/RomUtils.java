package tk.wasdennnoch.androidn_ify.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemProperties;
import android.preference.PreferenceManager;

import com.crossbowffs.remotepreferences.RemotePreferences;

import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.systemui.notifications.StatusBarHeaderHooks;

@SuppressWarnings("BooleanMethodIsAlwaysInverted")
public class RomUtils {

    private static SharedPreferences sPrefs;

    // Init from UI
    public static void init(Context context) {
        sPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        checkRom();
    }

    // Init from Xposed
    public static void init(XSharedPreferences prefs) {
        sPrefs = prefs;
        isOxygen35();
    }
    public static void initRemote() {
        Context context = (Context) XposedHelpers.callMethod(XposedHelpers.callStaticMethod(XposedHelpers.findClass("android.app.ActivityThread", null), "currentActivityThread"), "getSystemContext");
        sPrefs = new RemotePreferences(context, "tk.wasdennnoch.androidn_ify.PREFERENCES", "tk.wasdennnoch.androidn_ify_preferences");
    }

    // Call only from UI
    @SuppressLint("CommitPrefEdits")
    private static void checkRom() {
        if (sPrefs.contains("rom")) return;
        String aicpVersion = SystemProperties.get("ro.aicp.version", "");
        if (!aicpVersion.equals("")) {
            sPrefs.edit().putString("rom", "aicp").commit();
            return;
        }
        int cmSdkVersion = SystemProperties.getInt("ro.cm.build.version.plat.sdk", 0);
        if (cmSdkVersion != 0) {
            sPrefs.edit().putString("rom", "cm").commit();
            return;
        }
        sPrefs.edit().putString("rom", "aosp").commit();
    }

    public static boolean isCm() {
        return StatusBarHeaderHooks.mUseDragPanel;
    }

    public static boolean isAicp() {
        return sPrefs.getString("rom", "").equals("aicp");
    }

    public static boolean isOxygen35() {
        return SystemProperties.get("ro.oxygen.version", "").contains("3.5");
    }

    public static boolean isCmBased() {
        String rom = sPrefs.getString("rom", "");
        switch (rom) {
            case "aicp":
            case "cm":
                return true;
            default:
                return false;
        }
    }

}
