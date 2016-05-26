package tk.wasdennnoch.androidn_ify.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemProperties;
import android.preference.PreferenceManager;
import android.util.Log;

import de.robv.android.xposed.XSharedPreferences;
import tk.wasdennnoch.androidn_ify.XposedHook;

public class RomUtils {
    private static SharedPreferences sPrefs;
    private static boolean isReadOnly = false;

    // Init from UI
    public static void init(Context context) {
        sPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        checkRom();
    }

    // Init from Xposed
    public static void init(XSharedPreferences prefs) {
        sPrefs = prefs;
        isReadOnly = true;
        checkRom();
    }

    @SuppressLint("CommitPrefEdits")
    private static void checkRom() {
        if (isReadOnly) return;
        if (sPrefs.contains("rom")) return;
        String rrVersion = SystemProperties.get("ro.rr.version", "");
        if (!"".equals(rrVersion)) {
            sPrefs.edit().putString("rom", "rr").commit();
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

    public static boolean isCmBased() {
        String rom = sPrefs.getString("rom", "");
        switch (rom) {
            case "rr":
            case "cm":
                return true;
            default:
                return false;
        }
    }

}
