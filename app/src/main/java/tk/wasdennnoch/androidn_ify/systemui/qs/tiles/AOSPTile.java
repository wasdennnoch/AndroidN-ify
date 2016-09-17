package tk.wasdennnoch.androidn_ify.systemui.qs.tiles;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.systemui.SystemUIHooks;
import tk.wasdennnoch.androidn_ify.systemui.qs.TilesManager;

public class AOSPTile extends BaseTile {
    private static final String TAG = "AOSPTile";
    private Class mTileClass;

    private XC_MethodHook.Unhook mHandleUpdateStateHook;
    private XC_MethodHook.Unhook mHandleToggleClickHook;
    private XC_MethodHook.Unhook mHandleClickHook;

    public AOSPTile(TilesManager tilesManager, Object host, String key) {
        super(tilesManager, host, key);
        mTile = XposedHelpers.callMethod(host, "createTile", key);
        mTileClass = mTile.getClass();
        XposedHelpers.setAdditionalInstanceField(mTile, TILE_KEY_NAME, mKey);
        registerCallbacks();
        hook();
    }

    private void hook() {
        try {
            mHandleUpdateStateHook = XposedHelpers.findAndHookMethod(mTileClass, "handleUpdateState",
                    CLASS_TILE_STATE, Object.class, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            if (mKey.equals(XposedHelpers.getAdditionalInstanceField(param.thisObject, TILE_KEY_NAME))) {
                                handleUpdateState(param.args[0], param.args[1]);
                            }
                        }
                    });
            //hookHandleClick();
        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error hooking tile " + mTileClass.getName(), t);
        }
    }

    private void hookHandleClick() {
        Method clickMethod;
        try {
            clickMethod = XposedHelpers.findMethodExact(mTileClass, "handleClick");
        } catch (Throwable t) { // PA
            clickMethod = XposedHelpers.findMethodExact(mTileClass, "handleToggleClick");
            mHandleToggleClickHook = XposedHelpers.findAndHookMethod(mTileClass, "handleClick", boolean.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    param.args[0] = true; // So that handleToggleClick gets called
                }
            });
        }
        mHandleClickHook = XposedBridge.hookMethod(clickMethod, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (mKey.equals(XposedHelpers.getAdditionalInstanceField(param.thisObject, TILE_KEY_NAME))) {
                    if (handleClickInner()) {
                        param.setResult(null);
                    }
                }
            }
        });
    }

    @Override
    public void handleDestroy() {
        super.handleDestroy();
        mHandleUpdateStateHook.unhook();
        //if (mHandleToggleClickHook != null) mHandleToggleClickHook.unhook();
        //mHandleClickHook.unhook();
    }

    @Override
    public void handleUpdateState(Object state, Object arg) {
        boolean visible = !mSecure || !mKeyguard.isShowing() || !mKeyguard.isSecure();
        XposedHelpers.setBooleanField(state, "visible", visible);
    }

    @Override
    public boolean handleClickInner() {
        return false;
        /*
        if (mSecure && mKeyguard.isShowing() && mKeyguard.isSecure()) {
            SystemUIHooks.statusBarHooks.startRunnableDismissingKeyguard(new Runnable() {
                @Override
                public void run()
                {
                    callHandleClick();
                }
            });
            return true;
        } else {
            return false;
        }
        */
    }

    private void callHandleClick() {
        XposedHelpers.callMethod(mTile, "handleClick");
    }
}
