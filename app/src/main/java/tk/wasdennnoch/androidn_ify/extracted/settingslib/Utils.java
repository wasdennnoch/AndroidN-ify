package tk.wasdennnoch.androidn_ify.extracted.settingslib;

import android.content.Intent;
import android.content.res.Resources;
import android.os.BatteryManager;

import java.text.NumberFormat;

import tk.wasdennnoch.androidn_ify.R;

public class Utils {

    /** Formats an integer from 0..100 as a percentage. */
    public static String formatPercentage(int percentage) {
        return formatPercentage(((double) percentage) / 100.0);
    }

    /** Formats a double from 0.0..1.0 as a percentage. */
    private static String formatPercentage(double percentage) {
        return NumberFormat.getPercentInstance().format(percentage);
    }

    public static int getBatteryLevel(Intent batteryChangedIntent) {
        int level = batteryChangedIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
        int scale = batteryChangedIntent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        return (level * 100) / scale;
    }

    public static String getBatteryStatus(Resources res, Intent batteryChangedIntent,
                                          boolean shortString) {
        int plugType = batteryChangedIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
        int status = batteryChangedIntent.getIntExtra(BatteryManager.EXTRA_STATUS,
                BatteryManager.BATTERY_STATUS_UNKNOWN);
        String statusString;
        if (status == BatteryManager.BATTERY_STATUS_CHARGING) {
            int resId;
            if (plugType == BatteryManager.BATTERY_PLUGGED_AC) {
                resId = shortString ? R.string.battery_info_status_charging_ac_short
                        : R.string.battery_info_status_charging_ac;
            } else if (plugType == BatteryManager.BATTERY_PLUGGED_USB) {
                resId = shortString ? R.string.battery_info_status_charging_usb_short
                        : R.string.battery_info_status_charging_usb;
            } else if (plugType == BatteryManager.BATTERY_PLUGGED_WIRELESS) {
                resId = shortString ? R.string.battery_info_status_charging_wireless_short
                        : R.string.battery_info_status_charging_wireless;
            } else {
                resId = R.string.battery_info_status_charging;
            }
            statusString = res.getString(resId);
        } else if (status == BatteryManager.BATTERY_STATUS_DISCHARGING) {
            statusString = res.getString(R.string.battery_info_status_discharging);
        } else if (status == BatteryManager.BATTERY_STATUS_NOT_CHARGING) {
            statusString = res.getString(R.string.battery_info_status_not_charging);
        } else if (status == BatteryManager.BATTERY_STATUS_FULL) {
            statusString = res.getString(R.string.battery_info_status_full);
        } else {
            statusString = res.getString(R.string.battery_info_status_unknown);
        }

        return statusString;
    }
}