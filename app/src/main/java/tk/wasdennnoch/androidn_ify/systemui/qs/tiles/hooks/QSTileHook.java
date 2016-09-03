package tk.wasdennnoch.androidn_ify.systemui.qs.tiles.hooks;

import android.content.Context;
import android.content.Intent;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

@SuppressWarnings({"SameParameterValue", "UnusedParameters"})
public abstract class QSTileHook {

    private final QSTileHook mHook;
    private final Class<?> mTileClass;
    Context mContext;
    Object mThisObject;

    QSTileHook(ClassLoader classLoader, String className) {
        mHook = this;
        mTileClass = XposedHelpers.findClass(className, classLoader);
        XposedBridge.hookAllConstructors(mTileClass, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                mThisObject = param.thisObject;
                mContext = (Context) getObjectField("mContext");
                XposedHelpers.setAdditionalInstanceField(mThisObject, "hookingClass", mHook.getClass().getSimpleName());
                afterConstructor(param);
            }
        });
        try {
            XposedHelpers.findAndHookMethod(mTileClass, "handleClick", handleClickHook);
        } catch (Throwable t) { // PA
            XposedHelpers.findAndHookMethod(mTileClass, "handleToggleClick", handleClickHook);
        }
        try {
            XposedHelpers.findAndHookMethod(mTileClass, "handleLongClick", handleLongClickHook);
        } catch (Throwable t) { // PA
            try {
                XposedHelpers.findAndHookMethod(mTileClass, "handleDetailClick", handleLongClickHook);
            } catch (Throwable ignore) { // Not implemented, let maybeHandleLongClick do the job
            }
        }
    }

    void setDualTargets() {
        try {
            XposedHelpers.findAndHookMethod(mTileClass, "supportsDualTargets", XC_MethodReplacement.returnConstant(false));
        } catch (Throwable ignore) {
        }
    }

    void afterConstructor(XC_MethodHook.MethodHookParam param) {
    }

    protected abstract Intent getSettingsIntent();

    void handleClick() {
    }

    void handleLongClick() {
    }

    final void startSettings() {
        startActivityDismissingKeyguard(getSettingsIntent());
    }

    private void startActivityDismissingKeyguard(Intent intent) {
        Object host = XposedHelpers.getObjectField(mThisObject, "mHost");
        try {
            XposedHelpers.callMethod(host, "startActivityDismissingKeyguard", intent);
        } catch (Throwable t) { // CM 12.1
            XposedHelpers.callMethod(host, "startSettingsActivity", intent);
        }
    }

    final void showDetail(boolean show) {
        XposedHelpers.callMethod(mThisObject, "showDetail", show);
    }

    Object getState() {
        return XposedHelpers.getObjectField(mThisObject, "mState");
    }

    final Object getObjectField(String name) {
        return XposedHelpers.getObjectField(mThisObject, name);
    }

    private final XC_MethodHook handleClickHook = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            handleClick();
            return null;
        }
    };

    private final XC_MethodHook handleLongClickHook = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            handleLongClick();
            return null;
        }
    };

    public boolean maybeHandleLongClick(Object tile) {
        String clazz = (String) XposedHelpers.getAdditionalInstanceField(tile, "hookingClass");
        if (clazz != null && clazz.equals(getClass().getSimpleName())) {
            handleLongClick();
            return true;
        }
        return false;
    }

}
