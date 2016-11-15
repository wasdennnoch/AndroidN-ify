package tk.wasdennnoch.androidn_ify.utils;

import android.content.Context;
import android.content.pm.PackageManager;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class MiscUtils {

    public static boolean isGBInstalled(Context context) {
        try {
            context.getPackageManager().getPackageInfo(ConfigUtils.M ? "com.ceco.marshmallow.gravitybox" : "com.ceco.lollipop.gravitybox", 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static String readInputStream(InputStream stream) throws IOException {
        StringBuilder result = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        String line;
        while ((line = reader.readLine()) != null) {
            result.append(line);
        }
        return result.toString();
    }

    public static JSONArray checkValidJSONArray(String json) throws JSONException {
        return new JSONArray(json.replace(" ", ""));
    }
}