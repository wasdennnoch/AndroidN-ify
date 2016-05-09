package tk.wasdennnoch.androidn_ify.utils;

import android.os.SystemProperties;

/**
 * Created by jcdc on 5/9/2016.
 */
public class RomUtils {

    public static boolean isResurrectionRemix(){
        return SystemProperties.get("ro.modversion").toLowerCase().contains("resurrectionremix");
    }

}
