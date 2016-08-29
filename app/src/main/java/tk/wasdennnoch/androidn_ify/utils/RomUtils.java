package tk.wasdennnoch.androidn_ify.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemProperties;
import android.preference.PreferenceManager;

import de.robv.android.xposed.XSharedPreferences;

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
    }

    // Call only from UI
    @SuppressLint("CommitPrefEdits")
    private static void checkRom() {
        if (sPrefs.contains("rom")) return;
        String rrVersion = SystemProperties.get("ro.rr.version", "");
        if (!"".equals(rrVersion)) {
            sPrefs.edit().putString("rom", "rr").commit();
            return;
        }
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
        return sPrefs.getString("rom", "").equals("cm");
    }

    public static boolean isRr() {
        return sPrefs.getString("rom", "").equals("rr");
    }

    public static boolean isAicp() {
        return sPrefs.getString("rom", "").equals("aicp");
    }

    public static boolean isCmBased() {
        String rom = sPrefs.getString("rom", "");
        switch (rom) {
            case "rr":
            case "aicp":
            case "cm":
                return true;
            default:
                return false;
        }
    }

}
