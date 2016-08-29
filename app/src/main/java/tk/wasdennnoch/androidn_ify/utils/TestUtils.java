package tk.wasdennnoch.androidn_ify.utils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import de.robv.android.xposed.XposedBridge;
import tk.wasdennnoch.androidn_ify.XposedHook;

@SuppressWarnings("unused")
public class TestUtils {

    private static final String TAG = "TestUtils";

    public static void logMethods(Class clazz) {
        XposedHook.logI(TAG, "LOGGING ALL METHODS OF CLASS " + clazz.getSimpleName());
        Method[] methods = clazz.getDeclaredMethods();
        for (int i = 0; i < methods.length; i++) {
            Method m = methods[i];
            String log = " " + (i + 1) + ". - " +
                    Modifier.toString(m.getModifiers()) + " " +
                    m.getReturnType().getSimpleName() + " " +
                    m.getName() + "(";
            Class[] parameters = m.getParameterTypes();
            for (int j = 0; j < parameters.length; j++) {
                log += parameters[j].getSimpleName();
                if (j != parameters.length - 1)
                    log += ", ";
            }
            log += ");";
            XposedBridge.log(log);
        }
        XposedHook.logI(TAG, "METHOD LOG END");
    }

}
