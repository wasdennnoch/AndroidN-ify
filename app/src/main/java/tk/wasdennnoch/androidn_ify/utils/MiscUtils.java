package tk.wasdennnoch.androidn_ify.utils;

import android.content.Context;
import android.content.pm.PackageManager;

public class MiscUtils {

    public static boolean isGBInstalled(Context context) {
        try {
            context.getPackageManager().getPackageInfo(ConfigUtils.M ? "com.ceco.marshmallow.gravitybox" : "com.ceco.lollipop.gravitybox", 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

}
