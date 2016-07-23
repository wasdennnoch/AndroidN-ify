package tk.wasdennnoch.androidn_ify.utils;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Locale;

import tk.wasdennnoch.androidn_ify.R;

public class ViewUtils {

    public static final float LARGE_TEXT_SCALE = 1.3f;

    public static void setHeight(View view, int height) {
        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        layoutParams.height = height;
        view.setLayoutParams(layoutParams);
    }

    public static void setMarginEnd(View view, int margin) {
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        lp.rightMargin = margin;
        view.setLayoutParams(lp);
    }

    public static void updateFontSize(TextView v, int dimensId) {
        if (v != null) {
            v.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    v.getResources().getDimensionPixelSize(dimensId));
        }
    }


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
        if (prefs.getBoolean("force_english", false)) {
            Configuration config = activity.getResources().getConfiguration();
            config.locale = Locale.ENGLISH;
            activity.getResources().updateConfiguration(config, null);
        }
    }

    @SuppressWarnings({"ConstantConditions", "deprecation"})
    public static void applyTheme(Dialog dialog, Context context, SharedPreferences prefs) {
        int colorPrimary = prefs.getInt("theme_colorPrimary", context.getResources().getColor(R.color.colorPrimary));
        float[] hsv = new float[3];
        Color.colorToHSV(colorPrimary, hsv);
        hsv[2] *= 0.8f;
        int colorPrimaryDark = Color.HSVToColor(hsv);
        dialog.getWindow().setStatusBarColor(colorPrimaryDark);
        try {
            dialog.getActionBar().setBackgroundDrawable(new ColorDrawable(colorPrimary));
        } catch (NullPointerException ignore) {
        }
    }

}
