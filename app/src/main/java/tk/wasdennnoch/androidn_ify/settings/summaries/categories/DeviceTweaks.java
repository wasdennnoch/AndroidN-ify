package tk.wasdennnoch.androidn_ify.settings.summaries.categories;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.BatteryManager;
import android.os.BatteryStats;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.os.SystemClock;
import android.os.UserManager;
import android.provider.Settings;
import android.text.format.Formatter;
import com.android.internal.os.BatteryStatsHelper;
import com.android.internal.util.MemInfoReader;

import java.io.File;
import java.text.NumberFormat;
import java.util.List;

import de.robv.android.xposed.XposedHelpers;

public class DeviceTweaks {

    public static void hookDisplayTile(Object tile, Context context) {
        String summary;
        int brightnessMode = Settings.System.getInt(context.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
        summary = "Adaptive brightness is ";
        summary += brightnessMode == Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL ? "OFF" : "ON";
        XposedHelpers.setObjectField(tile, "summary", summary);
    }

    public static void hookNotificationTile(Object tile, Context context) {
        hookApplicationTile(tile, context);
    }

    public static void hookSoundTile(Object tile, Context context) {
        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        int currentVolume = audio.getStreamVolume(AudioManager.STREAM_RING);
        int maxVolume = audio.getStreamMaxVolume(AudioManager.STREAM_RING);
        XposedHelpers.setObjectField(tile, "summary", "Ringer volume at " + formatPercentage(currentVolume, maxVolume));
    }

    public static void hookApplicationTile(Object tile, Context context) {
        final PackageManager pm = context.getPackageManager();
        List<ApplicationInfo> packages = pm.getInstalledApplications(0);
        XposedHelpers.setObjectField(tile, "summary", packages.size() + " apps installed");
    }

    public static void hookStorageTile(Object tile, Context context) {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSizeLong();
        long availableBlocks = stat.getAvailableBlocksLong();
        long totalBlocks = stat.getBlockCountLong();

        long available = availableBlocks * blockSize;
        long total = totalBlocks * blockSize;
        long used = total - available;

        String summary = Formatter.formatFileSize(context, used);
        summary += " of ";
        summary += Formatter.formatFileSize(context, total);
        summary += " used";
        XposedHelpers.setObjectField(tile, "summary", summary);
    }

    public static void hookBatteryTile(Object tile, Context context) {
        Intent batteryIntent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        //noinspection ConstantConditions
        int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
        int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        int plugged = batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        //summary = String.valueOf((int) (((float)level / (float)scale) * 100.0f)) + "% - ";
        //summary = String.valueOf((level * 100) / scale) + "%";
        String summary = formatPercentage(level, scale);
        switch (plugged) {
            case 0:
                BatteryStatsHelper batteryStatsHelper = new BatteryStatsHelper(context, false);
                batteryStatsHelper.create(new Bundle());
                BatteryStats batteryStats = batteryStatsHelper.getStats();
                long remaining = batteryStats.computeBatteryTimeRemaining(SystemClock.elapsedRealtime()); // TODO Fix remaining time
                if (remaining > 0) {
                    summary += " - approx. ";
                    Class<?> Formatter = XposedHelpers.findClass("android.text.format.Formatter", null);
                    summary += XposedHelpers.callStaticMethod(Formatter, "formatShortElapsedTime", context, remaining / 1000);
                    summary += " left";
                }
                break;
            case BatteryManager.BATTERY_PLUGGED_AC:
                summary += " - Charging on AC";
                break;
            case BatteryManager.BATTERY_PLUGGED_USB:
                summary += " - Charging on USB";
                break;
            case BatteryManager.BATTERY_PLUGGED_WIRELESS:
                summary += " - Charging on WIRELESS";
                break;
        }
        XposedHelpers.setObjectField(tile, "summary", summary);
    }

    public static void hookMemoryTile(Object tile, Context context) {

        // Thanks to GravityBox for this snippet!
        ActivityManager mAm = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        MemInfoReader mMemInfoReader = new MemInfoReader();

        mAm.getMemoryInfo(memInfo);
        long secServerMem = 0;//XposedHelpers.getLongField(memInfo, "secondaryServerThreshold");
        mMemInfoReader.readMemInfo();
        long availMem = mMemInfoReader.getFreeSize() + mMemInfoReader.getCachedSize() - secServerMem;
        long totalMem = mMemInfoReader.getTotalSize();

        String summary = "Currently ";
        summary += Formatter.formatShortFileSize(context, totalMem - availMem);
        summary += " of ";
        summary += Formatter.formatShortFileSize(context, totalMem);
        summary += " memory used";

        XposedHelpers.setObjectField(tile, "summary", summary);

    }

    public static void hookUserTile(Object tile, Context context) {
        UserManager userManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
        XposedHelpers.setObjectField(tile, "summary", "Signed in as " + userManager.getUserName());
    }


    private static String formatPercentage(long amount, long total) {
        return formatPercentage(((double) amount) / total);
    }

    private static String formatPercentage(double percentage) {
        return NumberFormat.getPercentInstance().format(percentage);
    }

}
