package tk.wasdennnoch.androidn_ify.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.v7.graphics.Palette;

public class ColorUtils {

    public static int generateColor(Drawable drawable, int defaultColor) {
        Palette palette = Palette.from(convertToBitmap(drawable, 128, 128)).generate();
        int color = palette.getVibrantColor(defaultColor);
        if (color != defaultColor) return color;
        color = palette.getLightVibrantColor(defaultColor);
        if (color != defaultColor) return color;
        color = palette.getDarkMutedColor(defaultColor);
        if (color != defaultColor) return color;
        color = palette.getLightMutedColor(defaultColor);
        return color;
    }

    public static Bitmap convertToBitmap(Drawable drawable, int widthPixels, int heightPixels) {
        Bitmap bitmap = Bitmap.createBitmap(widthPixels, heightPixels, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, widthPixels, heightPixels);
        drawable.draw(canvas);
        return bitmap;
    }

}
