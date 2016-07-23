package tk.wasdennnoch.androidn_ify.utils;

import android.annotation.ColorRes;
import android.annotation.DimenRes;
import android.annotation.DrawableRes;
import android.annotation.LayoutRes;
import android.annotation.NonNull;
import android.annotation.StringRes;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;

import tk.wasdennnoch.androidn_ify.XposedHook;

public class ResourceUtils {

    private static Context mContext;
    private static ResourceUtils mInstance;

    private ResourceUtils(Context context) {
        mInstance = this;
        mContext = createOwnContext(context);
    }

    public static Context createOwnContext(Context context) {
        try {
            return context.createPackageContext(XposedHook.PACKAGE_OWN, Context.CONTEXT_IGNORE_SECURITY);
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException("Failed to instantiate own package context", e);
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

    public final XmlResourceParser getLayout(@LayoutRes int resId) {
        return mContext.getResources().getLayout(resId);
    }

    public final float getDimension(@DimenRes int resId) {
        return mContext.getResources().getDimension(resId);
    }

    public final int getDimensionPixelSize(@DimenRes int resId) {
        return mContext.getResources().getDimensionPixelSize(resId);
    }

    @ColorInt
    public final int getColor(@ColorRes int resId) {
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

    @NonNull
    public CharSequence getText(@StringRes int resId) {
        return mContext.getResources().getText(resId);
    }

    @NonNull
    public Resources getResources() {
        return mContext.getResources();
    }

}


