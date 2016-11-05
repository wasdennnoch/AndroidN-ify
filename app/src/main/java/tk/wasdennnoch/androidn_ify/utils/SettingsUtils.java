package tk.wasdennnoch.androidn_ify.utils;

import android.app.ActivityManager;
import android.content.ContentResolver;
import android.provider.Settings;

import java.lang.reflect.Method;

import de.robv.android.xposed.XposedHelpers;

@SuppressWarnings("WeakerAccess")
public class SettingsUtils {

    private static final Method mPutStringForUser;
    private static final Method mGetStringForUser;
    private static final Method mGetCurrentUser;

    static {
        mPutStringForUser = XposedHelpers.findMethodExact(Settings.Secure.class, "putStringForUser", ContentResolver.class, String.class, String.class, int.class);
        mGetStringForUser = XposedHelpers.findMethodExact(Settings.Secure.class, "getStringForUser", ContentResolver.class, String.class, int.class);
        mGetCurrentUser = XposedHelpers.findMethodExact(ActivityManager.class, "getCurrentUser");
    }

    public static boolean putStringForCurrentUser(ContentResolver resolver, String name, String value) {
        return putStringForUser(resolver, name, value, getCurrentUser());
    }

    public static String getStringForCurrentUser(ContentResolver resolver, String name) {
        return getStringForUser(resolver, name, getCurrentUser());
    }

    public static boolean putStringForUser(ContentResolver resolver, String name, String value, int userHandle) {
        try {
            return (boolean) mPutStringForUser.invoke(Settings.Secure.class, resolver, name, value, userHandle);
        } catch (Exception e) {
            throw new RuntimeException("Error in putStringForUser", e);
        }
    }

    public static String getStringForUser(ContentResolver resolver, String name, int userHandle) {
        try {
            return (String) mGetStringForUser.invoke(Settings.Secure.class, resolver, name, userHandle);
        } catch (Exception e) {
            throw new RuntimeException("Error in getStringForUser", e);
        }
    }

    private static int getCurrentUser() {
        try {
            return (int) mGetCurrentUser.invoke(ActivityManager.class);
        } catch (Exception e) {
            throw new RuntimeException("Error in putStringForUser", e);
        }
    }
}
