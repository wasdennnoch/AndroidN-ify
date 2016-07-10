package tk.wasdennnoch.androidn_ify.utils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import tk.wasdennnoch.androidn_ify.XposedHook;

@SuppressWarnings("unused")
public class TestUtils {

    private static final String TAG = "TestUtils";

    public static void logMethods(Class clazz) {
        XposedHook.logI(TAG, "LOGGING ALL METHODS OF CLASS " + clazz.getName());
        Method[] methods = clazz.getDeclaredMethods();
        for (int i = 0; i < methods.length; i++) {
            Method m = methods[i];
            XposedHook.logI(TAG, " " + (i + 1) + ". - " +
                    Modifier.toString(m.getModifiers()) + " " +
                    m.getReturnType().getName() + " " +
                    m.getName() + "([see below])");
            Class[] parameters = m.getParameterTypes();
            for (int j = 0; j < parameters.length; j++) {
                XposedHook.logI(TAG, "    - Parameter " + (j + 1) + ": " + parameters[j].getClass().getName());
            }
        }
        XposedHook.logI(TAG, "METHOD LOG END");
    }

}
