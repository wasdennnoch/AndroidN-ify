package tk.wasdennnoch.androidn_ify.systemui.notifications.stack;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.extracted.systemui.Interpolators;
import tk.wasdennnoch.androidn_ify.systemui.notifications.NotificationPanelHooks;
import tk.wasdennnoch.androidn_ify.utils.ConfigUtils;
import tk.wasdennnoch.androidn_ify.utils.ResourceUtils;

import static tk.wasdennnoch.androidn_ify.XposedHook.PACKAGE_SYSTEMUI;

public class NotificationStackScrollLayoutHooks {

    public static final int ANIMATION_DURATION_STANDARD = 360;

    private int TAG_ANIMATOR_TRANSLATION_Y;
    private int TAG_END_TRANSLATION_Y;

    private final Class<?> classActivatableNotificationView;
    private final Class<?> classStackStateAnimator;
    private ViewGroup mStackScrollLayout;
    private Context mContext;
    private ResourceUtils mRes;
    private final Paint mBackgroundPaint = new Paint();

    private boolean mAnimationRunning;
    private boolean mAnimationsEnabled = true;
    private boolean mIsExpanded;
    private ViewTreeObserver.OnPreDrawListener mBackgroundUpdater
            = new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
               updateBackground();
               return true;
           }
    };
    private Rect mBackgroundBounds = new Rect();
    private Rect mStartAnimationRect = new Rect();
    private Rect mEndAnimationRect = new Rect();
    private Rect mCurrentBounds = new Rect(-1, -1, -1, -1);
    private boolean mAnimateNextBackgroundBottom;
    private boolean mAnimateNextBackgroundTop;
    private ObjectAnimator mBottomAnimator = null;
    private ObjectAnimator mTopAnimator = null;
    private FrameLayout mFirstVisibleBackgroundChild = null;
    private FrameLayout mLastVisibleBackgroundChild = null;
    private int mTopPadding;
    private float mStackTranslation;
    private PorterDuffXfermode mSrcMode = new PorterDuffXfermode(PorterDuff.Mode.SRC);

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
                if (ConfigUtils.notifications().enable_notifications_background) {
                    Canvas canvas = (Canvas) param.args[0];
                    canvas.drawRect(0, mCurrentBounds.top, mStackScrollLayout.getWidth(), mCurrentBounds.bottom, mBackgroundPaint);
                }
            }
        });
        XposedHelpers.findAndHookMethod(classNotificationStackScrollLayout, "startAnimationToState", new XC_MethodHook() {
            private boolean willUpdateBackground = false;

            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                willUpdateBackground = false;
                boolean mNeedsAnimation = XposedHelpers.getBooleanField(mStackScrollLayout, "mNeedsAnimation");
                if (mNeedsAnimation) {
                    XposedHelpers.callMethod(mStackScrollLayout, "generateChildHierarchyEvents");
                    XposedHelpers.setBooleanField(mStackScrollLayout, "mNeedsAnimation", false);
                }
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
        XposedHelpers.findAndHookMethod(classNotificationStackScrollLayout, "onLayout", boolean.class, int.class, int.class, int.class, int.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                updateFirstAndLastBackgroundViews();
            }
        });
        XposedHelpers.findAndHookMethod(classNotificationStackScrollLayout, "setAnimationsEnabled", boolean.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                mAnimationsEnabled = (boolean) param.args[0];
            }
        });
        XposedHelpers.findAndHookMethod(classNotificationStackScrollLayout, "setIsExpanded", boolean.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                mIsExpanded = (boolean) param.args[0];
            }
        });
        XposedHelpers.findAndHookMethod(classNotificationStackScrollLayout, "setTopPadding", int.class, boolean.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                mTopPadding = (int) param.args[0];
            }
        });
        XposedHelpers.findAndHookMethod(classNotificationStackScrollLayout, "setStackTranslation", float.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                mStackTranslation = (float) param.args[0];
            }
        });
        classActivatableNotificationView = XposedHelpers.findClass("com.android.systemui.statusbar.ActivatableNotificationView", classLoader);
        classStackStateAnimator = XposedHelpers.findClass("com.android.systemui.statusbar.stack.StackStateAnimator", classLoader);
    }

    private void updateFirstAndLastBackgroundViews() {
        FrameLayout firstChild = getFirstChildWithBackground();
        FrameLayout lastChild = getLastChildWithBackground();
        if (mAnimationsEnabled && mIsExpanded) {
            mAnimateNextBackgroundTop = firstChild != mFirstVisibleBackgroundChild;
            mAnimateNextBackgroundBottom = lastChild != mLastVisibleBackgroundChild;
        } else {
            mAnimateNextBackgroundTop = false;
            mAnimateNextBackgroundBottom = false;
        }
        mFirstVisibleBackgroundChild = firstChild;
        mLastVisibleBackgroundChild = lastChild;
    }

    private void initView() {
        TAG_ANIMATOR_TRANSLATION_Y = mContext.getResources().getIdentifier("translation_y_animator_tag", "id", PACKAGE_SYSTEMUI);
        TAG_END_TRANSLATION_Y = mContext.getResources().getIdentifier("translation_y_animator_end_value_tag", "id", PACKAGE_SYSTEMUI);
        mBackgroundPaint.setColor(0xFFEEEEEE);
        mBackgroundPaint.setXfermode(mSrcMode);
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
        /*
        if (!mCurrentBounds.equals(mBackgroundBounds)) {
            mCurrentBounds.set(mBackgroundBounds);
            //mScrimController.setExcludedBackgroundArea(mCurrentBounds);
            mBackground.setBounds(0, mCurrentBounds.top, mStackScrollLayout.getWidth(), mCurrentBounds.bottom);
            mStackScrollLayout.invalidate();
        }
        */
        if (!mCurrentBounds.equals(mBackgroundBounds)) {
            if (mAnimateNextBackgroundTop || mAnimateNextBackgroundBottom || areBoundsAnimating()) {
                startBackgroundAnimation();
            } else {
                mCurrentBounds.set(mBackgroundBounds);
                applyCurrentBackgroundBounds();
            }
        } else {
            if (mBottomAnimator != null) {
                mBottomAnimator.cancel();
            }
            if (mTopAnimator != null) {
                mTopAnimator.cancel();
            }
        }
        mAnimateNextBackgroundBottom = false;
        mAnimateNextBackgroundTop = false;
    }

    private void startBackgroundAnimation() {
        mCurrentBounds.left = mBackgroundBounds.left;
        mCurrentBounds.right = mBackgroundBounds.right;
        startBottomAnimation();
        startTopAnimation();
    }

    private void startTopAnimation() {
        int previousEndValue = mEndAnimationRect.top;
        int newEndValue = mBackgroundBounds.top;
        ObjectAnimator previousAnimator = mTopAnimator;
        if (previousAnimator != null && previousEndValue == newEndValue) {
            return;
        }
        if (!mAnimateNextBackgroundTop) {
            // just a local update was performed
            if (previousAnimator != null) {
                // we need to increase all animation keyframes of the previous animator by the
                // relative change to the end value
                int previousStartValue = mStartAnimationRect.top;
                PropertyValuesHolder[] values = previousAnimator.getValues();
                values[0].setIntValues(previousStartValue, newEndValue);
                mStartAnimationRect.top = previousStartValue;
                mEndAnimationRect.top = newEndValue;
                previousAnimator.setCurrentPlayTime(previousAnimator.getCurrentPlayTime());
                return;
            } else {
                // no new animation needed, let's just apply the value
                setBackgroundTop(newEndValue);
                return;
            }
        }
        if (previousAnimator != null) {
            previousAnimator.cancel();
        }
        ObjectAnimator animator = ObjectAnimator.ofInt(this, "backgroundTop",
                mCurrentBounds.top, newEndValue);
        Interpolator interpolator = Interpolators.FAST_OUT_SLOW_IN;
        animator.setInterpolator(interpolator);
        animator.setDuration(ANIMATION_DURATION_STANDARD);
        // remove the tag when the animation is finished
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mStartAnimationRect.top = -1;
                mEndAnimationRect.top = -1;
                mTopAnimator = null;
            }
        });
        animator.start();
        mStartAnimationRect.top = mCurrentBounds.top;
        mEndAnimationRect.top = newEndValue;
        mTopAnimator = animator;
    }

    private void startBottomAnimation() {
        int previousStartValue = mStartAnimationRect.bottom;
        int previousEndValue = mEndAnimationRect.bottom;
        int newEndValue = mBackgroundBounds.bottom;
        ObjectAnimator previousAnimator = mBottomAnimator;
        if (previousAnimator != null && previousEndValue == newEndValue) {
            return;
        }
        if (!mAnimateNextBackgroundBottom) {
            // just a local update was performed
            if (previousAnimator != null) {
                // we need to increase all animation keyframes of the previous animator by the
                // relative change to the end value
                PropertyValuesHolder[] values = previousAnimator.getValues();
                values[0].setIntValues(previousStartValue, newEndValue);
                mStartAnimationRect.bottom = previousStartValue;
                mEndAnimationRect.bottom = newEndValue;
                previousAnimator.setCurrentPlayTime(previousAnimator.getCurrentPlayTime());
                return;
            } else {
                // no new animation needed, let's just apply the value
                setBackgroundBottom(newEndValue);
                return;
            }
        }
        if (previousAnimator != null) {
            previousAnimator.cancel();
        }
        ObjectAnimator animator = ObjectAnimator.ofInt(this, "backgroundBottom",
                mCurrentBounds.bottom, newEndValue);
        Interpolator interpolator = Interpolators.FAST_OUT_SLOW_IN;
        animator.setInterpolator(interpolator);
        animator.setDuration(ANIMATION_DURATION_STANDARD);
        // remove the tag when the animation is finished
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mStartAnimationRect.bottom = -1;
                mEndAnimationRect.bottom = -1;
                mBottomAnimator = null;
            }
        });
        animator.start();
        mStartAnimationRect.bottom = mCurrentBounds.bottom;
        mEndAnimationRect.bottom = newEndValue;
        mBottomAnimator = animator;
    }

    private void setBackgroundTop(int top) {
        mCurrentBounds.top = top;
        applyCurrentBackgroundBounds();
    }

    public void setBackgroundBottom(int bottom) {
        mCurrentBounds.bottom = bottom;
        applyCurrentBackgroundBounds();
    }

    private void applyCurrentBackgroundBounds() {
        //TODO implement excluded background area in scrim controller
        /*
        if (!mFadingOut) {
            mScrimController.setExcludedBackgroundArea(mCurrentBounds);
        }
        */
        mStackScrollLayout.invalidate();
    }

    private boolean areBoundsAnimating() {
        return mBottomAnimator != null || mTopAnimator != null;
    }

    private void updateBackgroundBounds() {
        mBackgroundBounds.left = (int) mStackScrollLayout.getX();
        mBackgroundBounds.right = (int) (mStackScrollLayout.getX() + mStackScrollLayout.getWidth());
        if (!mIsExpanded) {
            mBackgroundBounds.top = 0;
            mBackgroundBounds.bottom = 0;
        }
        FrameLayout firstView = mFirstVisibleBackgroundChild;
        int top = 0;
        if (firstView != null) {
            int finalTranslationY = (int) getFinalTranslationY(firstView);
            if (mAnimateNextBackgroundTop
                    || mTopAnimator == null && mCurrentBounds.top == finalTranslationY
                    || mTopAnimator != null && mEndAnimationRect.top == finalTranslationY) {
                // we're ending up at the same location as we are now, lets just skip the animation
                top = finalTranslationY;
            } else {
                top = (int) firstView.getTranslationY();
            }
        }
        FrameLayout lastView = mLastVisibleBackgroundChild;
        int bottom = 0;
        if (lastView != null) {
            int finalTranslationY = (int) getFinalTranslationY(lastView);
            int finalHeight = getFinalActualHeight(lastView);
            int finalBottom = finalTranslationY + finalHeight;
            finalBottom = Math.min(finalBottom, mStackScrollLayout.getHeight());
            if (mAnimateNextBackgroundBottom
                    || mBottomAnimator == null && mCurrentBounds.bottom == finalBottom
                    || mBottomAnimator != null && mEndAnimationRect.bottom == finalBottom) {
                // we're ending up at the same location as we are now, lets just skip the animation
                bottom = finalBottom;
            } else {
                bottom = (int) (lastView.getTranslationY() + (int) XposedHelpers.callMethod(lastView, "getActualHeight"));
                bottom = Math.min(bottom, mStackScrollLayout.getHeight());
            }
        } else {
            top = (int) (mTopPadding + mStackTranslation);
            bottom = top;
        }
        if (NotificationPanelHooks.getStatusBarState() != NotificationPanelHooks.STATE_KEYGUARD) {
            mBackgroundBounds.top = (int) Math.max(mTopPadding + mStackTranslation, top);
        } else {
            // otherwise the animation from the shade to the keyguard will jump as it's maxed
            mBackgroundBounds.top = Math.max(0, top);
        }
        mBackgroundBounds.bottom = Math.min(mStackScrollLayout.getHeight(), Math.max(bottom, top));
    }

    private FrameLayout getLastChildWithBackground() {
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

    private FrameLayout getFirstChildWithBackground() {
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
