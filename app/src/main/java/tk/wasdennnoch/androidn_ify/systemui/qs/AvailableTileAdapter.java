package tk.wasdennnoch.androidn_ify.systemui.qs;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.systemui.qs.tiles.QSTile;
import tk.wasdennnoch.androidn_ify.systemui.qs.tiles.views.FakeQSTileView;
import tk.wasdennnoch.androidn_ify.utils.ResourceUtils;
import tk.wasdennnoch.androidn_ify.utils.RomUtils;

public class AvailableTileAdapter extends TileAdapter {

    private static final String CLASS_QS_TUNER = "com.android.systemui.tuner.QsTuner";

    private Class<?> classQSTileHost;
    private Class<?> classResourceIcon;
    private Class<?> classQSTuner;

    public AvailableTileAdapter(ArrayList<Object> records, Context context, ViewGroup qsPanel) {
        super(context, qsPanel);

        mRecords = new ArrayList<>();

        classQSTileHost = XposedHelpers.findClass(QSTileHostHooks.CLASS_TILE_HOST, mContext.getClassLoader());
        classResourceIcon = XposedHelpers.findClass(QSTile.CLASS_QS_TILE + "$ResourceIcon", mContext.getClassLoader());
        try {
            classQSTuner = XposedHelpers.findClass(CLASS_QS_TUNER, mContext.getClassLoader());
        } catch (Throwable ignore) {
            // CM
        }

        mTileViews = new ArrayList<>();
        List<String> availableTiles = QSTileHostHooks.getAvailableTiles(mContext);

        for (int i = 0; i < records.size(); i++) {
            Object tilerecord = records.get(i);
            Object tile = XposedHelpers.getObjectField(tilerecord, "tile");
            String spec = (String) XposedHelpers.getAdditionalInstanceField(tile, QSTileHostHooks.TILE_SPEC_NAME);
            availableTiles.remove(spec);
        }

        if (RomUtils.isCmBased()) {
            try {
                Class<?> classQSUtils = XposedHelpers.findClass(QSTileHostHooks.CLASS_QS_UTILS, mContext.getClassLoader());
                for (String spec : availableTiles) {
                    if (!(boolean) XposedHelpers.callStaticMethod(classQSUtils, "isStaticQsTile", spec) && !TilesManager.mCustomTileSpecs.contains(spec)) {
                        availableTiles.remove(spec);
                    }
                }
            } catch (Throwable t) {
                XposedHook.logW(TAG, "Couldn't determine static tiles (" + t.getClass().getSimpleName() + ")");
                // TODO crashing although the CMSDK is clearly there?
            }
        }

        for (String spec : availableTiles) {
            addSpec(spec);
        }
    }

    private void addSpec(String spec) {
        if (spec == null) return;

        RelativeLayout.LayoutParams tileViewLp = new RelativeLayout.LayoutParams(mCellWidth, mCellHeight);
        tileViewLp.addRule(RelativeLayout.CENTER_IN_PARENT);

        FakeQSTileView tileView = new FakeQSTileView(mContext);
        tileView.setLayoutParams(tileViewLp);
        tileView.handleStateChanged(getQSTileIcon(spec), getQSTileLabel(spec));
        mTileViews.add(tileView);
        mRecords.add(spec);
    }

    @Override
    public int getItemCount() {
        return mTileViews.size();
    }

    private Drawable getQSTileIcon(String spec) {
        int res;
        try {
            res = (int) XposedHelpers.callStaticMethod(classQSTileHost, "getIconResource", spec);
        } catch (Throwable ignore) {
            try {
                res = getIconResource(spec);
            } catch (Throwable ignore2) {
                res = getIconResourceAosp(spec);
            }
        }
        if (res != 0) {
            Object icon = XposedHelpers.callStaticMethod(classResourceIcon, "get", res);
            return (Drawable) XposedHelpers.callMethod(icon, "getDrawable", mContext);
        } else {
            return mContext.getPackageManager().getDefaultActivityIcon();
        }
    }

    private int getIconResourceAosp(String spec) {
        Resources res = mContext.getResources();
        switch (spec) {
            case "wifi": return res.getIdentifier("ic_qs_wifi_full_4", "drawable", PACKAGE_SYSTEMUI);
            case "bt": return res.getIdentifier("ic_qs_bluetooth_on", "drawable", PACKAGE_SYSTEMUI);
            case "inversion": return res.getIdentifier("ic_invert_colors_enable_animation", "drawable", PACKAGE_SYSTEMUI);
            case "cell": return res.getIdentifier("ic_qs_signal_full_4", "drawable", PACKAGE_SYSTEMUI);
            case "airplane": return res.getIdentifier("ic_signal_airplane_enable", "drawable", PACKAGE_SYSTEMUI);
            case "dnd": return res.getIdentifier("ic_dnd", "drawable", PACKAGE_SYSTEMUI);
            case "rotation": return res.getIdentifier("ic_portrait_from_auto_rotate", "drawable", PACKAGE_SYSTEMUI);
            case "flashlight": return res.getIdentifier("ic_signal_flashlight_enable", "drawable", PACKAGE_SYSTEMUI);
            case "location": return res.getIdentifier("ic_signal_location_enable", "drawable", PACKAGE_SYSTEMUI);
            case "cast": return res.getIdentifier("ic_qs_cast_on", "drawable", PACKAGE_SYSTEMUI);
            case "hotspot": return res.getIdentifier("ic_hotspot_enable", "drawable", PACKAGE_SYSTEMUI);
        }
        return 0;
    }

    private int getIconResource(String spec) {
        Resources res = mContext.getResources();
        //noinspection IfCanBeSwitch
        if (spec.equals("wifi")) return res.getIdentifier("ic_qs_wifi_full_3", "drawable", PACKAGE_SYSTEMUI);
        else if (spec.equals("bt")) return res.getIdentifier("ic_qs_bluetooth_connected", "drawable", PACKAGE_SYSTEMUI);
        else if (spec.equals("inversion")) return res.getIdentifier("ic_invert_colors_enable", "drawable", PACKAGE_SYSTEMUI);
        else if (spec.equals("cell") || spec.equals("cell1") || spec.equals("cell2")) return res.getIdentifier("ic_qs_signal_full_3", "drawable", PACKAGE_SYSTEMUI);
        else if (spec.equals("airplane")) return res.getIdentifier("ic_signal_airplane_enable", "drawable", PACKAGE_SYSTEMUI);
        else if (spec.equals("dnd")) return res.getIdentifier("ic_qs_dnd_on", "drawable", PACKAGE_SYSTEMUI);
        else if (spec.equals("rotation")) return res.getIdentifier("ic_portrait_from_auto_rotate", "drawable", PACKAGE_SYSTEMUI);
        else if (spec.equals("flashlight")) return res.getIdentifier("ic_signal_flashlight_enable", "drawable", PACKAGE_SYSTEMUI);
        else if (spec.equals("location")) return res.getIdentifier("ic_signal_location_enable", "drawable", PACKAGE_SYSTEMUI);
        else if (spec.equals("cast")) return res.getIdentifier("ic_qs_cast_on", "drawable", PACKAGE_SYSTEMUI);
        else if (spec.equals("hotspot")) return res.getIdentifier("ic_hotspot_enable", "drawable", PACKAGE_SYSTEMUI);
        else if (spec.equals("adb_network")) return res.getIdentifier("ic_qs_network_adb_on", "drawable", PACKAGE_SYSTEMUI);
        else if (spec.equals("compass")) return res.getIdentifier("ic_qs_compass_on", "drawable", PACKAGE_SYSTEMUI);
        else if (spec.equals("nfc")) return res.getIdentifier("ic_qs_nfc_on", "drawable", PACKAGE_SYSTEMUI);
        else if (spec.equals("profiles")) return res.getIdentifier("ic_qs_profiles_on", "drawable", PACKAGE_SYSTEMUI);
        else if (spec.equals("sync")) return res.getIdentifier("ic_qs_sync_on", "drawable", PACKAGE_SYSTEMUI);
        else if (spec.equals("volume_panel")) return res.getIdentifier("ic_qs_volume_panel", "drawable", PACKAGE_SYSTEMUI);
        else if (spec.equals("usb_tether")) return res.getIdentifier("ic_qs_usb_tether_on", "drawable", PACKAGE_SYSTEMUI);
        else if (spec.equals("screen_timeout")) return res.getIdentifier("ic_qs_screen_timeout_short_avd", "drawable", PACKAGE_SYSTEMUI);
        else if (spec.equals("performance")) return res.getIdentifier("ic_qs_perf_profile", "drawable", PACKAGE_SYSTEMUI);
        else if (spec.equals("ambient_display")) return res.getIdentifier("ic_qs_ambientdisplay_on", "drawable", PACKAGE_SYSTEMUI);
        else if (spec.equals("live_display")) return res.getIdentifier("ic_livedisplay_auto", "drawable", PACKAGE_SYSTEMUI);
        else if (spec.equals("music")) return res.getIdentifier("ic_qs_media_play", "drawable", PACKAGE_SYSTEMUI);
        else if (spec.equals("brightness")) return res.getIdentifier("ic_qs_brightness_auto_on", "drawable", PACKAGE_SYSTEMUI);
        else if (spec.equals("screen_off")) return res.getIdentifier("ic_qs_power", "drawable", PACKAGE_SYSTEMUI);
        else if (spec.equals("screenshot")) return res.getIdentifier("ic_qs_screenshot", "drawable", PACKAGE_SYSTEMUI);
        else if (spec.equals("expanded_desktop")) return res.getIdentifier("ic_qs_expanded_desktop", "drawable", PACKAGE_SYSTEMUI);
        else if (spec.equals("reboot")) return res.getIdentifier("ic_qs_reboot", "drawable", PACKAGE_SYSTEMUI);
        else if (spec.equals("configurations")) return res.getIdentifier("ic_rr_tools", "drawable", PACKAGE_SYSTEMUI);
        else if (spec.equals("heads_up")) return res.getIdentifier("ic_qs_heads_up_on", "drawable", PACKAGE_SYSTEMUI);
        else if (spec.equals("lte")) return res.getIdentifier("ic_qs_lte_on", "drawable", PACKAGE_SYSTEMUI);
        else if (spec.equals("themes")) return res.getIdentifier("ic_qs_themes", "drawable", PACKAGE_SYSTEMUI);
        else if (spec.equals("navbar")) return res.getIdentifier("ic_qs_navbar_on", "drawable", PACKAGE_SYSTEMUI);
        else if (spec.equals("appcirclebar")) return res.getIdentifier("ic_qs_appcirclebar_on", "drawable", PACKAGE_SYSTEMUI);
        else if (spec.equals("kernel_adiutor")) return res.getIdentifier("ic_qs_kernel_adiutor", "drawable", PACKAGE_SYSTEMUI);
        else if (spec.equals("screenrecord")) return res.getIdentifier("ic_qs_screenrecord", "drawable", PACKAGE_SYSTEMUI);
        else if (spec.equals("gesture_anywhere")) return res.getIdentifier("ic_qs_gestures_on", "drawable", PACKAGE_SYSTEMUI);
        else if (spec.equals("battery_saver")) return res.getIdentifier("ic_qs_battery_saver_on", "drawable", PACKAGE_SYSTEMUI);
        else if (spec.equals("power_menu")) return res.getIdentifier("ic_qs_power_menu", "drawable", PACKAGE_SYSTEMUI);
        else if (spec.equals("app_picker")) return res.getIdentifier("ic_qs_app_picker", "drawable", PACKAGE_SYSTEMUI);
        else if (spec.equals("kill_app")) return res.getIdentifier("ic_app_kill", "drawable", PACKAGE_SYSTEMUI);
        else if (spec.equals("caffeine")) return res.getIdentifier("ic_qs_caffeine_on", "drawable", PACKAGE_SYSTEMUI);
        else if (spec.equals("hw_keys")) return res.getIdentifier("ic_qs_hwkeys_on", "drawable", PACKAGE_SYSTEMUI);
        else if (spec.equals("sound")) return res.getIdentifier("ic_qs_ringer_silent", "drawable", PACKAGE_SYSTEMUI);
        else if (spec.equals("lockscreen")) return res.getIdentifier("ic_qs_lock_screen_on", "drawable", PACKAGE_SYSTEMUI);
        else if (spec.equals("pulse")) return res.getIdentifier("ic_qs_pulse", "drawable", PACKAGE_SYSTEMUI);
        else if (spec.equals("pie")) return res.getIdentifier("ic_qs_pie", "drawable", PACKAGE_SYSTEMUI);
        else if (spec.equals("float_mode")) return res.getIdentifier("ic_qs_floating_on", "drawable", PACKAGE_SYSTEMUI);
        else if (spec.equals("visualizer")) return res.getIdentifier("ic_qs_visualizer_static", "drawable", PACKAGE_SYSTEMUI);
        return 0;
    }

    private String getQSTileLabel(String spec) {
        int resource;
        try {
            resource = TilesManager.getLabelResource(spec);
        } catch (Throwable t) {
            try {
                resource = (int) XposedHelpers.callStaticMethod(classQSTileHost, "getLabelResource", spec);
            } catch (Throwable t2) {
                try { // RR
                    resource = (int) XposedHelpers.callStaticMethod(classQSTuner, "getLabelResource", spec);
                } catch (Throwable t3) {
                    resource = getQSTileLabelAosp(spec);
                }
            }
        }
        if (resource != 0) {
            try {
                return mContext.getText(resource).toString();
            } catch (Throwable t) {
                return ResourceUtils.getInstance(mContext).getText(resource).toString();
            }
        } else {
            return spec;
        }
    }

    private int getQSTileLabelAosp(String spec) {
        Resources res = mContext.getResources();
        switch (spec) {
            case "wifi": return res.getIdentifier("quick_settings_wifi_label", "string", PACKAGE_SYSTEMUI);
            case "bt": return res.getIdentifier("quick_settings_bluetooth_label", "string", PACKAGE_SYSTEMUI);
            case "inversion": return res.getIdentifier("quick_settings_inversion_label", "string", PACKAGE_SYSTEMUI);
            case "cell": return res.getIdentifier("quick_settings_cellular_detail_title", "string", PACKAGE_SYSTEMUI);
            case "airplane": return res.getIdentifier("airplane_mode", "string", PACKAGE_SYSTEMUI);
            case "dnd": return res.getIdentifier("quick_settings_dnd_label", "string", PACKAGE_SYSTEMUI);
            case "rotation": return res.getIdentifier("quick_settings_rotation_locked_label", "string", PACKAGE_SYSTEMUI);
            case "flashlight": return res.getIdentifier("quick_settings_flashlight_label", "string", PACKAGE_SYSTEMUI);
            case "location": return res.getIdentifier("quick_settings_location_label", "string", PACKAGE_SYSTEMUI);
            case "cast": return res.getIdentifier("quick_settings_cast_title", "string", PACKAGE_SYSTEMUI);
            case "hotspot": return res.getIdentifier("quick_settings_hotspot_label", "string", PACKAGE_SYSTEMUI);
        }
        return 0;
    }
}
