package tk.wasdennnoch.androidn_ify.systemui.qs;

import android.view.View;
import android.view.ViewGroup;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

class CMQuickSettingsHooks extends QuickSettingsHooks {
    CMQuickSettingsHooks(ClassLoader classLoader) {
        super(classLoader);
    }

    @Override
    protected void hookConstructor() {
        XposedHelpers.findAndHookMethod(mHookClass, "setupViews", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                mQsPanel = (ViewGroup) param.thisObject;
                mContext = mQsPanel.getContext();
                mBrightnessView = (View) XposedHelpers.getObjectField(param.thisObject, "mQsPanelTop");
                mFooter = XposedHelpers.getObjectField(param.thisObject, "mFooter");
                mDetail = (View) XposedHelpers.getObjectField(param.thisObject, "mDetail");
                setupTileLayout();
            }
        });
    }

    @Override
    protected String getHookClass() {
        return CLASS_QS_DRAG_PANEL;
    }
}
