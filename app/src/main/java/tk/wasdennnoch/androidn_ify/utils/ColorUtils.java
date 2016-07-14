package tk.wasdennnoch.androidn_ify.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.v7.graphics.Palette;

public class ColorUtils {

    public static int generateColor(Context context, Drawable drawable, int defaultColor) {
        int color = Palette.from(convertToBitmap(drawable, 128, 128)).generate().getVibrantColor(defaultColor);
        if(color != defaultColor) return color;
        color = Palette.from(convertToBitmap(drawable, 128, 128)).generate().getLightVibrantColor(defaultColor);
        if(color != defaultColor) return color;
        color = Palette.from(convertToBitmap(drawable, 128, 128)).generate().getDarkMutedColor(defaultColor);
        if(color != defaultColor) return color;
        color = Palette.from(convertToBitmap(drawable, 128, 128)).generate().getLightMutedColor(defaultColor);
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
