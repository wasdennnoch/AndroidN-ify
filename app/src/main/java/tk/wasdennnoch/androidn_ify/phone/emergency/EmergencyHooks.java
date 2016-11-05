package tk.wasdennnoch.androidn_ify.phone.emergency;

import android.app.Activity;
import android.os.Bundle;
import android.view.ViewGroup;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.utils.ConfigUtils;

public class EmergencyHooks {

    private static final String TAG = "EmergencyHooks";
    private static final String PACKAGE_PHONE = XposedHook.PACKAGE_PHONE;
    private static final String CLASS_EMERGENCY_DIALER = PACKAGE_PHONE + ".EmergencyDialer";

    public static void hook(ClassLoader classLoader) {
        try {

            if (ConfigUtils.lockscreen().enable_emergency_info) {

                Class classEmergencyDialer = XposedHelpers.findClass(CLASS_EMERGENCY_DIALER, classLoader);

                XposedHelpers.findAndHookMethod(classEmergencyDialer, "onCreate", Bundle.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {

                        Activity activity = (Activity) param.thisObject;

                        ViewGroup top = (ViewGroup) activity.findViewById(activity.getResources().getIdentifier("top", "id", PACKAGE_PHONE));
                        ViewGroup parent = ConfigUtils.M ? (ViewGroup) top.getChildAt(0) : top;

                        new EmergencyButtonWrapper(activity, parent); // No need to store the instance yet

                    }
                });

            }

        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error in hook", t);
        }
    }

}
