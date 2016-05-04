package tk.wasdennnoch.androidn_ify.utils;

import android.annotation.DimenRes;
import android.annotation.DrawableRes;
import android.annotation.NonNull;
import android.annotation.StringRes;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

import tk.wasdennnoch.androidn_ify.XposedHook;

public class ResourceUtils {

    private static final String TAG = "ResourceUtils";

    private static Context mContext;
    private static ResourceUtils mInstance;

    private ResourceUtils(Context context) {
        mInstance = this;
        try {
            mContext = context.createPackageContext("tk.wasdennnoch.androidn_ify", Context.CONTEXT_IGNORE_SECURITY);
        } catch (PackageManager.NameNotFoundException e) {
            XposedHook.logE(TAG, "Failed to instantiate package context", e);
        }
    }

    public static ResourceUtils getInstance() {
        return mInstance;
    }

    public static ResourceUtils getInstance(Context context) {
        if (mInstance == null)
            mInstance = new ResourceUtils(context);
        return mInstance;
    }

    public final float getDimension(@DimenRes int resId) {
        return mContext.getResources().getDimension(resId);
    }

    public final int getDimensionPixelSize(@DimenRes int resId) {
        return mContext.getResources().getDimensionPixelSize(resId);
    }

    public final int getColor(@DimenRes int resId) {
        //noinspection deprecation
        return mContext.getResources().getColor(resId);
    }

    @NonNull
    public final Drawable getDrawable(@DrawableRes int resId) {
        //noinspection deprecation
        return mContext.getDrawable(resId);
    }

    @NonNull
    public final String getString(@StringRes int resId) {
        return mContext.getResources().getString(resId);
    }

    @NonNull
    public final String getString(@StringRes int resId, Object... formatArgs) {
        return mContext.getResources().getString(resId, formatArgs);
    }

}


