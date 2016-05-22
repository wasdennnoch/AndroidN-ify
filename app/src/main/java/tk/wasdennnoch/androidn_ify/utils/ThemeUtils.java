package tk.wasdennnoch.androidn_ify.utils;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;

import java.util.Locale;

import tk.wasdennnoch.androidn_ify.R;

public class ThemeUtils {

    @SuppressWarnings({"ConstantConditions", "deprecation"})
    public static void applyTheme(Activity activity, SharedPreferences prefs) {
        if (prefs.getBoolean("app_dark_theme", false))
            activity.setTheme(R.style.DarkTheme);
        int colorPrimary = prefs.getInt("theme_colorPrimary", activity.getResources().getColor(R.color.colorPrimary));
        float[] hsv = new float[3];
        Color.colorToHSV(colorPrimary, hsv);
        hsv[2] *= 0.8f;
        int colorPrimaryDark = Color.HSVToColor(hsv);
        activity.getWindow().setStatusBarColor(colorPrimaryDark);
        activity.getActionBar().setBackgroundDrawable(new ColorDrawable(colorPrimary));
        Locale locale = prefs.getBoolean("force_english", false) ? Locale.ENGLISH : Locale.getDefault();
        //Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        activity.getResources().updateConfiguration(config, null);
    }

    @SuppressWarnings({"ConstantConditions", "deprecation"})
    public static void applyTheme(Dialog dialog, Context context, SharedPreferences prefs) {
        int colorPrimary = prefs.getInt("theme_colorPrimary", context.getResources().getColor(R.color.colorPrimary));
        float[] hsv = new float[3];
        Color.colorToHSV(colorPrimary, hsv);
        hsv[2] *= 0.8f;
        int colorPrimaryDark = Color.HSVToColor(hsv);
        dialog.getWindow().setStatusBarColor(colorPrimaryDark);
        dialog.getActionBar().setBackgroundDrawable(new ColorDrawable(colorPrimary));
    }

}
