package tk.wasdennnoch.androidn_ify.systemui.qs.tiles.hooks;

import android.content.Context;
import android.content.Intent;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public abstract class QSTileHook {

    private String mClassName;
    protected Class<?> mTileClass;
    protected Context mContext;
    protected Object mThisObject;

    public QSTileHook(Class classQSTile, ClassLoader classLoader, String className) {
        mClassName = className;
        mTileClass = XposedHelpers.findClass(className, classLoader);
        XposedBridge.hookAllConstructors(mTileClass, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                mThisObject = param.thisObject;
                mContext = (Context) getObjectField("mContext");
                XposedHelpers.setAdditionalInstanceField(mThisObject, "hookedClass", mClassName);
                afterConstructor(param);
            }
        });
        XposedHelpers.findAndHookMethod(mTileClass, "handleClick", handleClickHook);
        try {
            XposedHelpers.findAndHookMethod(mTileClass, "handleLongClick", handleLongClickHook);
        } catch (Throwable t) {
            XposedHelpers.findAndHookMethod(classQSTile, "handleLongClick", handleLongClickHook2);
        }
    }

    protected void setDualTargets() {
        try {
            XposedHelpers.findAndHookMethod(mTileClass, "supportsDualTargets", XC_MethodReplacement.returnConstant(false));
        } catch (Throwable ignore) {
        }
    }

    protected void afterConstructor(XC_MethodHook.MethodHookParam param) {
    }

    protected abstract Intent getSettingsIntent();

    protected void handleClick() {
    }

    protected void handleLongClick() {
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

    protected XC_MethodHook handleLongClickHook2 = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            String clazz = (String) XposedHelpers.getAdditionalInstanceField(mThisObject, "hookedClass");
            if (clazz != null && clazz.equals(mClassName)) {
                param.setResult(null);
                handleLongClick();
            }
        }
    };

}
