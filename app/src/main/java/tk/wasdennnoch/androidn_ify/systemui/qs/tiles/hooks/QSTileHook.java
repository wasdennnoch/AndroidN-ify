package tk.wasdennnoch.androidn_ify.systemui.qs.tiles.hooks;

import android.content.Context;
import android.content.Intent;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public abstract class QSTileHook {

    protected Class<?> mTileClass;
    protected Context mContext;
    protected Object mThisObject;

    public QSTileHook(ClassLoader classLoader, String className) {
        mTileClass = XposedHelpers.findClass(className, classLoader);
        XposedBridge.hookAllConstructors(mTileClass, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                mThisObject = param.thisObject;
                mContext = (Context) getObjectField("mContext");
                afterConstructor(param);
            }
        });
    }

    protected void afterConstructor(XC_MethodHook.MethodHookParam param) {
    }

    protected abstract Intent getSettingsIntent();

    protected void handleClick() {
    }

    protected void handleLongClick() {
    }

    protected final void hookClick() {
        XposedHelpers.findAndHookMethod(mTileClass, "handleClick", handleClickHook);
    }

    protected final void hookLongClick() {
        //XposedHelpers.findAndHookMethod(mTileClass, "handleLongClick", handleLongClickHook);
    }

    protected final void startSettings() {
        startActivityDismissingKeyguard(getSettingsIntent());
    }

    protected final void startActivityDismissingKeyguard(Intent intent) {
        XposedHelpers.callMethod(XposedHelpers.getObjectField(mThisObject, "mHost"), "startActivityDismissingKeyguard", intent);
    }

    protected final void showDetail(boolean show) {
        XposedHelpers.callMethod(mThisObject, "showDetail", show);
    }

    protected Object getState() {
        return XposedHelpers.getObjectField(mThisObject, "mState");
    }

    protected final Object getObjectField(String name) {
        return XposedHelpers.getObjectField(mThisObject, name);
    }


    protected XC_MethodHook handleClickHook = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            handleClick();
            return null;
        }
    };

    protected XC_MethodHook handleLongClickHook = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            handleLongClick();
            return null;
        }
    };

}
