package tk.wasdennnoch.androidn_ify.systemui.qs.tiles;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.systemui.qs.TilesManager;

public class AOSPTile extends BaseTile {
    private static final String TAG = "AOSPTile";
    private Class mTileClass;

    private XC_MethodHook.Unhook mHandleUpdateStateHook;

    public AOSPTile(TilesManager tilesManager, Object host, String key) {
        super(tilesManager, host, key);
        mTile = XposedHelpers.callMethod(host, "createTile", key);
        mTileClass = mTile.getClass();
        XposedHelpers.setAdditionalInstanceField(mTile, TILE_KEY_NAME, mKey);

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
        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error hooking tile " + mTileClass.getName(), t);
        }
    }

    @Override
    public void handleDestroy() {
        super.handleDestroy();
        mHandleUpdateStateHook.unhook();
    }

    @Override
    public void handleUpdateState(Object state, Object arg) {
        boolean visible = !mSecure || !mKeyguard.isShowing() || !mKeyguard.isSecure();
        XposedHelpers.setBooleanField(state, "visible", visible);
    }
}
