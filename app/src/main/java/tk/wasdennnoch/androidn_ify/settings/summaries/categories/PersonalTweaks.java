package tk.wasdennnoch.androidn_ify.settings.summaries.categories;

import android.content.Context;
import android.location.LocationManager;

import java.util.Locale;

import de.robv.android.xposed.XposedHelpers;

public class PersonalTweaks {

    public static void hookLocationTile(Object tile, Context context) {
        String summary;
        LocationManager lm = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (!gps_enabled && !network_enabled) {
            summary = "Disabled";
        } else if (gps_enabled && network_enabled) {
            summary = "ON / High accuracy";
        } else if (gps_enabled) {
            summary = "ON / Device only";
        } else {
            summary = "ON / Power saving";
        }

        XposedHelpers.setObjectField(tile, "summary", summary);
    }

    public static void hookLanguageTile(Object tile) {
        XposedHelpers.setObjectField(tile, "summary", Locale.getDefault().getDisplayName());
    }

}
