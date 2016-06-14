package tk.wasdennnoch.androidn_ify.notifications.qs;

import android.content.Context;
import android.content.Intent;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.utils.RomUtils;

public class LiveDisplayObserver {

    public static final String LIVE_DISPLAY_MODE_CHANGED = "tk.wasdennnoch.androidn_ify.action.LIVE_DISPLAY_MODE_CHANGED";
    public static final String EXTRA_LIVE_DISPLAY_MODE = "extra.livedisplay.LIVE_DISPLAY_MODE";
    private static int mCurrentMode = 0;

    public static void hook(ClassLoader classLoader) {
        if (RomUtils.isCmBased()) {
            XposedHelpers.findAndHookMethod("org.cyanogenmod.platform.internal.display.LiveDisplayService", classLoader, "publishCustomTile", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    mCurrentMode = (Integer) XposedHelpers.callMethod(param.thisObject, "getCurrentModeIndex");
                    Intent intent = new Intent();
                    intent.setAction(LIVE_DISPLAY_MODE_CHANGED);
                    intent.putExtra(EXTRA_LIVE_DISPLAY_MODE, mCurrentMode);
                    Context context = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                    context.sendBroadcast(intent);
                }
            });
        }
    }
}
