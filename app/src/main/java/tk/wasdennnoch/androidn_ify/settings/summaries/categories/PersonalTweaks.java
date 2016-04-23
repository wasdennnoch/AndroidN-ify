package tk.wasdennnoch.androidn_ify.settings.summaries.categories;

import android.content.Context;
import android.location.LocationManager;

import java.util.Locale;

import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.utils.ResourceUtils;

public class PersonalTweaks {

    public static void hookLocationTile(Object tile, Context context) {
        String summary;
        LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (!gps_enabled && !network_enabled) {
            summary = ResourceUtils.getInstance().getString(R.string.disabled);
        } else if (gps_enabled && network_enabled) {
            summary = ResourceUtils.getInstance().getString(R.string.location_on_high);
        } else if (gps_enabled) {
            summary = ResourceUtils.getInstance().getString(R.string.location_on_device);
        } else {
            summary = ResourceUtils.getInstance().getString(R.string.location_on_power);
        }

        XposedHelpers.setObjectField(tile, "summary", summary);
    }

    public static void hookLanguageTile(Object tile) {
        XposedHelpers.setObjectField(tile, "summary", Locale.getDefault().getDisplayName());
    }

}
