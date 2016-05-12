package tk.wasdennnoch.androidn_ify.notifications.qs.tiles;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;

public class QSTileHook {
    private Class<?> mTileClass;
    private Object mThisObject;

    public QSTileHook(ClassLoader classLoader, String className) {
        mTileClass = XposedHelpers.findClass(className, classLoader);
    }

    public void startActivityDismissingKeyguard(String intentFieldName) {
        XposedHelpers.callMethod(XposedHelpers.getObjectField(getTile(), "mHost"), "startActivityDismissingKeyguard", XposedHelpers.getStaticObjectField(mTileClass, intentFieldName));
    }

    public void startActivityDismissingKeyguard(Object intent) {
        XposedHelpers.callMethod(XposedHelpers.getObjectField(getTile(), "mHost"), "startActivityDismissingKeyguard", intent);
    }

    public void handleClick() {

    }

    public void handleLongClick() {

    }

    public void hookClick() {
        XposedHelpers.findAndHookMethod(mTileClass, "handleClick", handleClickHook);
    }

    public void hookLongClick() {
        //XposedHelpers.findAndHookMethod(mTileClass, "handleLongClick", handleLongClickHook);
    }

    public void callSecondaryClick() {
        XposedHelpers.callMethod(getTile(), "handleSecondaryClick");
    }

    public Object getState() {
        return XposedHelpers.getObjectField(getTile(), "mState");
    }

    public Object getTile() {
        return mThisObject;
    }

    public Object getObjectField(String name) {
        return XposedHelpers.getObjectField(getTile(), name);
    }

    private XC_MethodReplacement handleClickHook = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            mThisObject = param.thisObject;
            handleClick();
            return null;
        }
    };

    private XC_MethodReplacement handleLongClickHook = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            mThisObject = param.thisObject;
            handleLongClick();
            return null;
        }
    };
}
