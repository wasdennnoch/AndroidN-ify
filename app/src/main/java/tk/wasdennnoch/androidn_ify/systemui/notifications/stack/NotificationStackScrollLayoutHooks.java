package tk.wasdennnoch.androidn_ify.systemui.notifications.stack;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.systemui.notifications.NotificationPanelHooks;
import tk.wasdennnoch.androidn_ify.utils.ResourceUtils;

import static tk.wasdennnoch.androidn_ify.XposedHook.PACKAGE_SYSTEMUI;

public class NotificationStackScrollLayoutHooks {

    private int TAG_ANIMATOR_TRANSLATION_Y;
    private int TAG_END_TRANSLATION_Y;

    private final Class<?> classActivatableNotificationView;
    private final Class<?> classStackStateAnimator;
    private ViewGroup mStackScrollLayout;
    private Context mContext;
    private ResourceUtils mRes;
    private ColorDrawable mBackground;

    private boolean mAnimationRunning;
    private ViewTreeObserver.OnPreDrawListener mBackgroundUpdater
            = new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
               updateBackground();
               return true;
           }
    };
    private Rect mBackgroundBounds = new Rect();
    private Rect mCurrentBounds = null;

    public NotificationStackScrollLayoutHooks(ClassLoader classLoader) {
        Class classNotificationStackScrollLayout = XposedHelpers.findClass("com.android.systemui.statusbar.stack.NotificationStackScrollLayout", classLoader);
        XposedBridge.hookAllMethods(classNotificationStackScrollLayout, "initView", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                mStackScrollLayout = (ViewGroup) param.thisObject;
                mContext = (Context) param.args[0];
                mRes = ResourceUtils.getInstance(mContext);
                initView();
            }
        });
        XposedHelpers.findAndHookMethod(classNotificationStackScrollLayout, "onDraw", Canvas.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                mBackground.draw((Canvas) param.args[0]);
            }
        });
        XposedHelpers.findAndHookMethod(classNotificationStackScrollLayout, "startAnimationToState", new XC_MethodHook() {
            private boolean willUpdateBackground = false;

            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                willUpdateBackground = false;
                Object mAnimationEvents = XposedHelpers.getObjectField(mStackScrollLayout, "mAnimationEvents");
                boolean isEmpty = (boolean) XposedHelpers.callMethod(mAnimationEvents, "isEmpty");
                boolean isCurrentlyAnimating = (boolean) XposedHelpers.callMethod(mStackScrollLayout, "isCurrentlyAnimating");
                if (!isEmpty || isCurrentlyAnimating) {
                    setAnimationRunning(true);
                    willUpdateBackground = true;
                }
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (willUpdateBackground) {
                    updateBackground();
                    willUpdateBackground = false;
                }
            }
        });
        XposedHelpers.findAndHookMethod(classNotificationStackScrollLayout, "onChildAnimationFinished", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                setAnimationRunning(false);
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                updateBackground();
            }
        });
        XposedHelpers.findAndHookMethod(classNotificationStackScrollLayout, "applyCurrentState", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                setAnimationRunning(false);
                updateBackground();
            }
        });
        classActivatableNotificationView = XposedHelpers.findClass("com.android.systemui.statusbar.ActivatableNotificationView", classLoader);
        classStackStateAnimator = XposedHelpers.findClass("com.android.systemui.statusbar.stack.StackStateAnimator", classLoader);
    }

    private void initView() {
        TAG_ANIMATOR_TRANSLATION_Y = mContext.getResources().getIdentifier("translation_y_animator_tag", "id", PACKAGE_SYSTEMUI);
        TAG_END_TRANSLATION_Y = mContext.getResources().getIdentifier("translation_y_animator_end_value_tag", "id", PACKAGE_SYSTEMUI);
        mBackground = new ColorDrawable(0xFFEEEEEE);
        XposedHelpers.callMethod(mBackground, "setXfermode", new PorterDuffXfermode(PorterDuff.Mode.SRC));
        mStackScrollLayout.setWillNotDraw(false);
    }

    private void updateBackground() {
        //TODO completely implement this
        /*
        if (mAmbientState.isDark()) {
            return;
        }
        */
        updateBackgroundBounds();
        if (!mCurrentBounds.equals(mBackgroundBounds)) {
            mCurrentBounds.set(mBackgroundBounds);
            //mScrimController.setExcludedBackgroundArea(mCurrentBounds);
            mBackground.setBounds(0, mCurrentBounds.top, mStackScrollLayout.getWidth(), mCurrentBounds.bottom);
            mStackScrollLayout.invalidate();
        }
    }

    private void updateBackgroundBounds() {
        FrameLayout firstView = getFirstViewWithBackground();
        int top = 0;
        if (firstView != null) {
            int finalTranslationY = (int) getFinalTranslationY(firstView);
            if (mBackgroundBounds.top == finalTranslationY) {
                // we're ending up at the same location as we are now, lets just skip the animation
                top = finalTranslationY;
            } else {
                top = (int) firstView.getTranslationY();
            }
        }
        FrameLayout lastView = getLastViewWithBackground();
        int bottom = 0;
        if (lastView != null) {
            int finalTranslationY = (int) getFinalTranslationY(lastView);
            int finalHeight = getFinalActualHeight(lastView);
            int finalBottom = finalTranslationY + finalHeight;
            finalBottom = Math.min(finalBottom, mStackScrollLayout.getHeight());
            if (mBackgroundBounds.bottom == finalBottom) {
                // we're ending up at the same location as we are now, lets just skip the animation
                bottom = finalBottom;
            } else {
                bottom = (int) (lastView.getTranslationY() + (int) XposedHelpers.callMethod(lastView, "getActualHeight"));
                bottom = Math.min(bottom, mStackScrollLayout.getHeight());
            }
        } else if (NotificationPanelHooks.getStatusBarState() == NotificationPanelHooks.STATE_KEYGUARD) {
            top = XposedHelpers.getIntField(mStackScrollLayout, "mTopPadding");
            bottom = top;
        }
        mBackgroundBounds.top = top;
        mBackgroundBounds.bottom = bottom;
        mBackgroundBounds.left = (int) mStackScrollLayout.getX();
        mBackgroundBounds.right = (int) (mStackScrollLayout.getX() + mStackScrollLayout.getWidth());
        if (mCurrentBounds == null) {
            mCurrentBounds = new Rect(mBackgroundBounds);
        }
    }

    private FrameLayout getLastViewWithBackground() {
        int childCount = mStackScrollLayout.getChildCount();
        for (int i = childCount - 1; i >= 0; i--) {
            View child = mStackScrollLayout.getChildAt(i);
            if (child.getVisibility() != View.GONE
                    && instanceOf(child, classActivatableNotificationView)) {
                return (FrameLayout) child;
            }
        }
        return null;
    }

    private FrameLayout getFirstViewWithBackground() {
        int childCount = mStackScrollLayout.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = mStackScrollLayout.getChildAt(i);
            if (child.getVisibility() != View.GONE
                    && instanceOf(child, classActivatableNotificationView)) {
                return (FrameLayout) child;
            }
        }
        return null;
    }

    private void setAnimationRunning(boolean animationRunning) {
        if (animationRunning != mAnimationRunning) {
            if (animationRunning) {
                mStackScrollLayout.getViewTreeObserver().addOnPreDrawListener(mBackgroundUpdater);
            } else {
                mStackScrollLayout.getViewTreeObserver().removeOnPreDrawListener(mBackgroundUpdater);
            }
            mAnimationRunning = animationRunning;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T getChildTag(View child, int tag) {
        return (T) child.getTag(tag);
    }

    private float getFinalTranslationY(View view) {
        if (view == null) {
            return 0;
        }
        ValueAnimator yAnimator = getChildTag(view, TAG_ANIMATOR_TRANSLATION_Y);
        if (yAnimator == null) {
            return view.getTranslationY();
        } else {
            return getChildTag(view, TAG_END_TRANSLATION_Y);
        }
    }

    private int getFinalActualHeight(View view) {
        return (int) XposedHelpers.callStaticMethod(classStackStateAnimator, "getFinalActualHeight", view);
    }

    private boolean instanceOf(Object obj, Class<?> objClass) {
        return objClass.isAssignableFrom(obj.getClass());
    }
}
