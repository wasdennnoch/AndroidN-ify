package tk.wasdennnoch.androidn_ify.utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class LayoutUtils {

    public static void setHeight(View view, int height, ResourceUtils res) {
        setHeight(view, res.getDimensionPixelSize(height));
    }

    public static void setHeight(View view, int height) {
        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        layoutParams.height = height;
        view.setLayoutParams(layoutParams);
    }

    public static void setWidth(View view, int width, ResourceUtils res) {
        setWidth(view, res.getDimensionPixelSize(width));
    }

    public static void setWidth(View view, int width) {
        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        layoutParams.width = width;
        view.setLayoutParams(layoutParams);
    }

    public static View inflate(Context context, int resId) {
        return inflate(context, resId, null);
    }

    public static View inflate(Context context, int resId, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(resId, parent);
    }
}
