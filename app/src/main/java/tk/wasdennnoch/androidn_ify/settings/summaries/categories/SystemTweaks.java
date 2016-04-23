package tk.wasdennnoch.androidn_ify.settings.summaries.categories;

import android.content.Context;
import android.os.Build;
import android.print.PrintManager;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.utils.ResourceUtils;

public class SystemTweaks {

    public static void hookDateTimeTile(Object tile) {
        TimeZone tz = TimeZone.getDefault();
        Calendar cal = GregorianCalendar.getInstance(tz);
        int offsetInMillis = tz.getOffset(cal.getTimeInMillis());
        String offset = String.format(Locale.getDefault(), "%02d:%02d", Math.abs(offsetInMillis / 3600000), Math.abs((offsetInMillis / 60000) % 60));
        offset = "GMT" + (offsetInMillis >= 0 ? "+" : "-") + offset;
        XposedHelpers.setObjectField(tile, "summary", offset);
    }

    public static void hookPrintTile(Object tile, Context context) {
        PrintManager pm = (PrintManager) context.getSystemService(Context.PRINT_SERVICE);
        PrintManager publicPm = (PrintManager) XposedHelpers.callMethod(pm, "getGlobalPrintManagerForUser", -2);
        if (publicPm != null) {
            XposedHelpers.setObjectField(tile, "summary", ResourceUtils.getInstance().getString(R.string.print_summary, publicPm.getPrintJobs().size()));
        }
    }

    public static void hookAboutTile(Object tile) {
        XposedHelpers.setObjectField(tile, "summary", "Android " + Build.VERSION.RELEASE);
    }

}
