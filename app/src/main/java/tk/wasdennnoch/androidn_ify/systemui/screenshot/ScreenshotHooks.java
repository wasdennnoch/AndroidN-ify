package tk.wasdennnoch.androidn_ify.systemui.screenshot;

import android.app.Service;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.android.AndroidHooks;
import tk.wasdennnoch.androidn_ify.extracted.systemui.ScreenshotSelectorView;

import static android.view.WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;

public class ScreenshotHooks {

    public static final String TAG = "ScreenshotHooks";
    private static final String KEY_PARTIAL_SCREENSHOT = "nify_partial_screenshot";

    private static int x, y, width, height;
    private static Service mService;

    public static void hook(final ClassLoader classLoader) {
        try {
            XposedHelpers.findAndHookConstructor(XposedHook.PACKAGE_SYSTEMUI + ".screenshot.GlobalScreenshot", classLoader, Context.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    FrameLayout mScreenshotLayout = (FrameLayout) XposedHelpers.getObjectField(param.thisObject, "mScreenshotLayout");
                    ScreenshotSelectorView mScreenshotSelectorView = new ScreenshotSelectorView(mScreenshotLayout.getContext());
                    mScreenshotSelectorView.setVisibility(View.GONE);
                    mScreenshotLayout.addView(mScreenshotSelectorView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                    mScreenshotSelectorView.setFocusable(true);
                    mScreenshotSelectorView.setFocusableInTouchMode(true);
                    XposedHelpers.setAdditionalInstanceField(param.thisObject, "mScreenshotSelectorView", mScreenshotSelectorView);
                }
            });

            XposedHelpers.findAndHookMethod(XposedHook.PACKAGE_SYSTEMUI + ".screenshot.GlobalScreenshot", classLoader, "startAnimation", Runnable.class, int.class, int.class, boolean.class, boolean.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    DisplayMetrics mDisplayMetrics = (DisplayMetrics) XposedHelpers.getObjectField(param.thisObject, "mDisplayMetrics");
                    Bitmap mScreenBitmap = (Bitmap) XposedHelpers.getObjectField(param.thisObject, "mScreenBitmap");

                    if (width != 0 && height != 0 && (width != mDisplayMetrics.widthPixels || height != mDisplayMetrics.heightPixels)) {
                        // Crop the screenshot to selected region
                        Bitmap cropped = Bitmap.createBitmap(mScreenBitmap, x, y, width, height);
                        mScreenBitmap.recycle();
                        mScreenBitmap = cropped;
                    }

                    // reset
                    x = 0;
                    y = 0;
                    width = 0;
                    height = 0;

                    // Optimizations
                    mScreenBitmap.setHasAlpha(false);
                    mScreenBitmap.prepareToDraw();

                    XposedHelpers.setObjectField(param.thisObject, "mScreenBitmap", mScreenBitmap);
                }
            });

            XposedHelpers.findAndHookMethod(Service.class, "onCreate", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Service service = (Service) param.thisObject;
                    if (!service.getClass().getName().contains("TakeScreenshotService")) return;
                    mService = service;
                }
            });

            XposedHelpers.findAndHookMethod(XposedHook.PACKAGE_SYSTEMUI + ".screenshot.TakeScreenshotService$1", classLoader, "handleMessage", Message.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!shouldTakePartial(mService)) return;
                    Settings.Global.putInt(mService.getContentResolver(), KEY_PARTIAL_SCREENSHOT, 0);
                    Message msg = (Message) param.args[0];
                    final Messenger callback = msg.replyTo;
                    Runnable finisher = new Runnable() {
                        @Override
                        public void run() {
                            Message reply = Message.obtain(null, 1);
                            try {
                                callback.send(reply);
                            } catch (RemoteException e) {
                                // ignored
                            }
                        }
                    };
                    Context service = (Context) XposedHelpers.getObjectField(param.thisObject, "this$0");
                    Object mScreenshot = XposedHelpers.getObjectField(service, "mScreenshot");
                    if (mScreenshot == null) {
                        mScreenshot = XposedHelpers.newInstance(XposedHelpers.findClass(XposedHook.PACKAGE_SYSTEMUI + ".screenshot.GlobalScreenshot", classLoader), service);
                        XposedHelpers.setObjectField(service, "mScreenshot", mScreenshot);
                    }

                    takeScreenshotPartial(mScreenshot, finisher, msg.arg1 > 0, msg.arg2 > 0);
                    param.setResult(null);
                }
            });

            /*XposedHelpers.findAndHookMethod(XposedHook.PACKAGE_SYSTEMUI + ".screenshot.TakeScreenshotService", classLoader, "onUnbind", Intent.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Object globalScreenshot = XposedHelpers.getObjectField(param.thisObject, "mScreenshot");
                    if (globalScreenshot != null) {
                        final WindowManager mWindowManager = (WindowManager) XposedHelpers.getObjectField(globalScreenshot, "mWindowManager");
                        final FrameLayout mScreenshotLayout = (FrameLayout) XposedHelpers.getObjectField(globalScreenshot, "mScreenshotLayout");
                        final ScreenshotSelectorView mScreenshotSelectorView = (ScreenshotSelectorView) XposedHelpers.getAdditionalInstanceField(globalScreenshot, "mScreenshotSelectorView");
                        if (mScreenshotSelectorView.getSelectionRect() != null) {
                            mWindowManager.removeView(mScreenshotLayout);
                            mScreenshotSelectorView.stopSelection();
                        }
                    }
                    param.setResult(true);
                }
            });*/
        } catch (Throwable t) {
            XposedHook.logE(TAG, "Crash in screenshot hooks", t);
        }
    }

    public static void takePartialScreenshot(Context context) {
        Settings.Global.putInt(context.getContentResolver(), KEY_PARTIAL_SCREENSHOT, 1);
        AndroidHooks.sendTakeScreenshot(context);
    }

    private static boolean shouldTakePartial(Context context) {
        return Settings.Global.getInt(context.getContentResolver(), KEY_PARTIAL_SCREENSHOT, 0) != 0;
    }

    private static void takeScreenshotPartial(final Object globalScreenshot, final Runnable finisher, final boolean statusBarVisible, final boolean navBarVisible) {
        XposedHook.logI(TAG, "Adding view");

        final WindowManager mWindowManager = (WindowManager) XposedHelpers.getObjectField(globalScreenshot, "mWindowManager");
        final WindowManager.LayoutParams mWindowLayoutParams = (WindowManager.LayoutParams) XposedHelpers.getObjectField(globalScreenshot, "mWindowLayoutParams");
        //noinspection WrongConstant
        mWindowLayoutParams.type = TYPE_SYSTEM_ERROR;
        final FrameLayout mScreenshotLayout = (FrameLayout) XposedHelpers.getObjectField(globalScreenshot, "mScreenshotLayout");
        final ScreenshotSelectorView mScreenshotSelectorView = (ScreenshotSelectorView) XposedHelpers.getAdditionalInstanceField(globalScreenshot, "mScreenshotSelectorView");

        mWindowManager.addView(mScreenshotLayout, mWindowLayoutParams);
        mScreenshotSelectorView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                ScreenshotSelectorView view = (ScreenshotSelectorView) v;
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        view.startSelection((int) event.getX(), (int) event.getY());
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        view.updateSelection((int) event.getX(), (int) event.getY());
                        return true;
                    case MotionEvent.ACTION_UP:
                        view.setVisibility(View.GONE);
                        mWindowManager.removeView(mScreenshotLayout);
                        final Rect rect = view.getSelectionRect();
                        if (rect != null) {
                            if (rect.width() != 0 && rect.height() != 0) {
                                // Need mScreenshotLayout to handle it after the view disappears
                                mScreenshotLayout.post(new Runnable() {
                                    public void run() {
                                        x = rect.left;
                                        y = rect.top;
                                        width = rect.width();
                                        height = rect.height();
                                        XposedHelpers.callMethod(globalScreenshot, "takeScreenshot", finisher, statusBarVisible, navBarVisible);
                                    }
                                });
                            }
                        }
                        view.stopSelection();
                        return true;
                }
                return false;
            }
        });

        mScreenshotLayout.post(new Runnable() {
            @Override
            public void run() {
                mScreenshotSelectorView.setVisibility(View.VISIBLE);
                mScreenshotSelectorView.requestFocus();
            }
        });
    }

    private static boolean windowTypeFieldMatches(int value, String type) {
        return value == XposedHelpers.getStaticIntField(WindowManager.LayoutParams.class, type);
    }
}