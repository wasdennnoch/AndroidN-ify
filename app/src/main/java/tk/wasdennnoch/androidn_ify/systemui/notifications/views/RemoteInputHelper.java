package tk.wasdennnoch.androidn_ify.systemui.notifications.views;

import android.app.PendingIntent;
import android.app.RemoteInput;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewParent;

import tk.wasdennnoch.androidn_ify.BuildConfig;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.extracted.systemui.RemoteInputView;
import tk.wasdennnoch.androidn_ify.systemui.notifications.NotificationHooks;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.getObjectField;

public class RemoteInputHelper {

    @SuppressWarnings({"PointlessBooleanExpression", "ConstantConditions"})
    public static final boolean DIRECT_REPLY_ENABLED = true;

    public static boolean handleRemoteInput(View view, PendingIntent pendingIntent, RemoteInput[] inputs, Object headsUpEntry) {
        if (inputs == null) {
            return false;
        }
        RemoteInput input = null;
        for (RemoteInput i : inputs) {
            if (i.getAllowFreeFormInput()) {
                input = i;
            }
        }
        if (input == null) {
            return false;
        }
        ViewParent p = view.getParent();
        RemoteInputView riv = null;
        while (p != null) {
            if (p instanceof View) {
                View pv = (View) p;
                if ((boolean) callMethod(pv, "isRootNamespace")) {
                    riv = (RemoteInputView) pv.findViewWithTag(RemoteInputView.VIEW_TAG);
                    break;
                }
            }
            p = p.getParent();
        }
        Object row = null;
        while (p != null) {
            if (p.getClass().getName().equals(XposedHook.PACKAGE_SYSTEMUI + ".statusbar.ExpandableNotificationRow")) {
                row = p;
                break;
            }
            p = p.getParent();
        }
        if (riv == null || row == null) {
            return false;
        }
        callMethod(row, "setUserExpanded", true);
        riv.setVisibility(View.VISIBLE);
        int cx = view.getLeft() + view.getWidth() / 2;
        int cy = view.getTop() + view.getHeight() / 2;
        int w = riv.getWidth();
        int h = riv.getHeight();
        int r = Math.max(
                Math.max(cx + cy, cx + (h - cy)),
                Math.max((w - cx) + cy, (w - cx) + (h - cy)));
        ViewAnimationUtils.createCircularReveal(riv, cx, cy, 0, r)
                .start();
        riv.setPendingIntent(pendingIntent);
        riv.setRemoteInput(inputs, input);
        if (headsUpEntry != null) {
            callMethod(headsUpEntry, "removeAutoRemovalCallbacks");
            riv.setHeadsUpEntry(headsUpEntry);
        }
        riv.focus();
        return true;
    }

    public static void setWindowManagerFocus(boolean focus) {
        NotificationHooks.remoteInputActive = focus;
        if (NotificationHooks.statusBarWindowManager != null)
            callMethod(NotificationHooks.statusBarWindowManager, "apply", getObjectField(NotificationHooks.statusBarWindowManager, "mCurrentState"));
    }
}