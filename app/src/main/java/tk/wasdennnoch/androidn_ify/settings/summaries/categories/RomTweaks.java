package tk.wasdennnoch.androidn_ify.settings.summaries.categories;

import android.content.Context;

public class RomTweaks {

    public static void hookDisplayAndLightsTile(Object tile, Context context) {
        DeviceTweaks.hookDisplayTile(tile, context);
    }

}
