package tk.wasdennnoch.androidn_ify.utils;

import android.annotation.NonNull;
import android.annotation.StringRes;
import android.content.Context;
import android.content.pm.PackageManager;

public class StringUtils {

    private static Context mContext;
    private static StringUtils mInstance;

    public StringUtils(Context context) {
        mInstance = this;
        try {
            mContext = context.createPackageContext("tk.wasdennnoch.androidn_ify", Context.CONTEXT_IGNORE_SECURITY);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static StringUtils getInstance() {
        return mInstance;
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


