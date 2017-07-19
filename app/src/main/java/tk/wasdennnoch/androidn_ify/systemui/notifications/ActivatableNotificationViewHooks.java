package tk.wasdennnoch.androidn_ify.systemui.notifications;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.TimeAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.extracted.systemui.Interpolators;
import tk.wasdennnoch.androidn_ify.extracted.systemui.NotificationUtils;
import tk.wasdennnoch.androidn_ify.systemui.notifications.stack.NotificationStackScrollLayoutHooks;
import tk.wasdennnoch.androidn_ify.utils.ConfigUtils;

public class ActivatableNotificationViewHooks {

    private static final String TAG = "ActivatableNotificationViewHooks";

    private static final String PACKAGE_SYSTEMUI = XposedHook.PACKAGE_SYSTEMUI;
    private static final float BACKGROUND_ALPHA_DIMMED = NotificationStackScrollLayoutHooks.BACKGROUND_ALPHA_DIMMED;
    private static final float DARK_EXIT_SCALE_START = 0.93f;
    private static final int ANIMATION_DURATION_STANDARD = NotificationStackScrollLayoutHooks.ANIMATION_DURATION_STANDARD;
    private static final int BACKGROUND_ANIMATION_LENGTH_MS = 220;
    private static final int ACTIVATE_ANIMATION_LENGTH = 220;
    private static final int DARK_ANIMATION_LENGTH = 170;

    private static Method methodUpdateBackground;
    private static Method methodCancelAppearAnimation;
    private static Method methodStartActivateAnimation;
    private static Method methodFadeDimmedBackground;
    private static Method methodUpdateAppearAnimationAlpha;
    private static Method methodUpdateAppearRect;
    private static Method methodEnableAppearDrawing;
    private static Method methodFadeInFromDark;
    private static Method methodSetContentAlpha;
    private static Field fieldOnActivatedListener;

    private static int mBackgroundNormalVisibility;
    private static int mBackgroundDimmedVisibility;
    private static boolean mDark;

    private static final Interpolator ACTIVATE_INVERSE_INTERPOLATOR
            = new PathInterpolator(0.6f, 0, 0.5f, 1);
    private static final Interpolator ACTIVATE_INVERSE_ALPHA_INTERPOLATOR
            = new PathInterpolator(0, 0, 0.5f, 1);

    public static void hook(ClassLoader classLoader) {
        try {
            if (!ConfigUtils.notifications().enable_notifications_background)
                return;
            final Class classActivatableNotificationView = XposedHelpers.findClass(PACKAGE_SYSTEMUI + ".statusbar.ActivatableNotificationView", classLoader);
            final Class classExpandableOutlineView = XposedHelpers.findClass(PACKAGE_SYSTEMUI + ".statusbar.ExpandableOutlineView", classLoader);

            methodUpdateBackground = XposedHelpers.findMethodBestMatch(classActivatableNotificationView, "updateBackground");
            methodCancelAppearAnimation = XposedHelpers.findMethodBestMatch(classActivatableNotificationView, "cancelAppearAnimation");
            methodStartActivateAnimation = XposedHelpers.findMethodBestMatch(classActivatableNotificationView, "startActivateAnimation", boolean.class);
            methodFadeDimmedBackground = XposedHelpers.findMethodBestMatch(classActivatableNotificationView, "fadeDimmedBackground");
            methodUpdateAppearAnimationAlpha = XposedHelpers.findMethodBestMatch(classActivatableNotificationView, "updateAppearAnimationAlpha");
            methodUpdateAppearRect = XposedHelpers.findMethodBestMatch(classActivatableNotificationView, "updateAppearRect");
            methodEnableAppearDrawing = XposedHelpers.findMethodBestMatch(classActivatableNotificationView, "enableAppearDrawing", boolean.class);
            methodFadeInFromDark = XposedHelpers.findMethodBestMatch(classActivatableNotificationView, "fadeInFromDark", long.class);
            methodSetContentAlpha = XposedHelpers.findMethodBestMatch(classActivatableNotificationView, "setContentAlpha", float.class);
            fieldOnActivatedListener = XposedHelpers.findField(classActivatableNotificationView, "mOnActivatedListener");

            XposedHelpers.findAndHookConstructor(classExpandableOutlineView, Context.class, AttributeSet.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    FrameLayout expandableView = (FrameLayout) param.thisObject;
                    ExpandableOutlineViewHelper e = ExpandableOutlineViewHelper.getInstance(expandableView);
                    e.construct(expandableView);
                }
            });

            XposedHelpers.findAndHookMethod(classActivatableNotificationView, "onFinishInflate", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    FrameLayout expandableView = (FrameLayout) param.thisObject;
                    updateOutlineAlpha(ExpandableOutlineViewHelper.getInstance(expandableView));
                }
            });

            XposedHelpers.findAndHookMethod(classActivatableNotificationView, "updateBackground", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    View mBackgroundNormal = (View) XposedHelpers.getObjectField(param.thisObject, "mBackgroundNormal");
                    if (!XposedHelpers.getBooleanField(param.thisObject, "mDark")) {
                        if (XposedHelpers.getBooleanField(param.thisObject, "mDimmed"))
                            mBackgroundNormal.setVisibility((XposedHelpers.getBooleanField(param.thisObject, "mActivated")
                                    ? View.VISIBLE
                                    : View.INVISIBLE));
                        else
                            XposedHelpers.callMethod(param.thisObject, "makeInactive", false);
                    }
                    setNormalBackgroundVisibilityAmount(param.thisObject, mBackgroundNormal.getVisibility() == View.VISIBLE ? 1.0f : 0.0f);
                }
            });

            XposedHelpers.findAndHookMethod(classActivatableNotificationView, "updateBackgroundTint", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                    updateBackgroundTint((FrameLayout) param.thisObject, false);
                    return null;
                }
            });

            XposedHelpers.findAndHookMethod(classActivatableNotificationView, "enableAppearDrawing", boolean.class, new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                    boolean enable = (boolean) param.args[0];
                    Object expandableView = param.thisObject;
                    ExpandableOutlineViewHelper helper = ExpandableOutlineViewHelper.getInstance(expandableView);
                    if (enable != XposedHelpers.getBooleanField(expandableView, "mDrawingAppearAnimation")) {
                        XposedHelpers.setBooleanField(expandableView, "mDrawingAppearAnimation", enable);
                        if (!enable) {
                            methodSetContentAlpha.invoke(expandableView, 1.0f);
                            XposedHelpers.setFloatField(expandableView, "mAppearAnimationFraction", -1);
                            helper.setOutlineRect(null);
                        }
                    }
                    return null;
                }
            });

            XposedHelpers.findAndHookMethod(classActivatableNotificationView, "cancelAppearAnimation", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (XposedHelpers.getObjectField(param.thisObject, "mAppearAnimator") != null)
                        XposedHelpers.setObjectField(param.thisObject, "mAppearAnimator", null);
                }
            });

            /*XposedHelpers.findAndHookMethod(classActivatableNotificationView, "reset", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    resetBackgroundAlpha(ExpandableOutlineViewHelper.getInstance(param.thisObject));
                }
            });*/

            XposedHelpers.findAndHookMethod(classActivatableNotificationView, "fadeDimmedBackground", fadeDimmedBackground);
            XposedHelpers.findAndHookMethod(classActivatableNotificationView, "fadeInFromDark", long.class, fadeInFromDark);
            XposedHelpers.findAndHookMethod(classActivatableNotificationView, "makeInactive", boolean.class, makeInactive);
            XposedHelpers.findAndHookMethod(classActivatableNotificationView, "startActivateAnimation", boolean.class, startActivateAnimation);
            XposedHelpers.findAndHookMethod(classActivatableNotificationView, "setDark", boolean.class, boolean.class, long.class, setDarkHook);
            XposedHelpers.findAndHookMethod(classActivatableNotificationView, "startAppearAnimation", boolean.class, float.class, long.class, long.class, Runnable.class, startAppearAnimation);
            XposedHelpers.findAndHookMethod(classActivatableNotificationView, "setDimmed", boolean.class, boolean.class, setDimmed);
            XposedHelpers.findAndHookMethod(classActivatableNotificationView, "updateAppearRect", updateAppearRect);
        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error hooking ActivatableNotificationView ", t);
        }
    }

    private static XC_MethodReplacement setDimmed = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            Object expandableView = param.thisObject;
            boolean dimmed = (boolean) param.args[0];
            boolean fade = (boolean) param.args[1];
            //ExpandableOutlineViewHelper helper = ExpandableOutlineViewHelper.getInstance(expandableView);
            if (XposedHelpers.getBooleanField(expandableView, "mDimmed") != dimmed) {
                XposedHelpers.setBooleanField(expandableView, "mDimmed", dimmed);
                //resetBackgroundAlpha(helper);
                if (fade) {
                    methodFadeDimmedBackground.invoke(expandableView);
                } else {
                    updateBackground(expandableView);
                }
            }
            return null;
        }
    };

    private static XC_MethodReplacement startAppearAnimation = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            final FrameLayout expandableView = (FrameLayout) param.thisObject;
            final boolean isAppearing = (boolean) param.args[0];
            float translationDirection = (float) param.args[1];
            long delay = (long) param.args[2];
            long duration = (long) param.args[3];
            final Runnable onFinishedRunnable = (Runnable) param.args[4];
            float mAppearAnimationFraction = XposedHelpers.getFloatField(expandableView, "mAppearAnimationFraction");
            methodCancelAppearAnimation.invoke(expandableView);
            XposedHelpers.setFloatField(expandableView, "mAnimationTranslationY", translationDirection * (int) XposedHelpers.callMethod(expandableView, "getActualHeight"));
            if (mAppearAnimationFraction == -1.0f) {
                // not initialized yet, we start anew
                if (isAppearing) {
                    mAppearAnimationFraction = 0.0f;
                    XposedHelpers.setFloatField(expandableView, "mAppearAnimationTranslation", XposedHelpers.getFloatField(expandableView, "mAnimationTranslationY"));
                } else {
                    mAppearAnimationFraction = 1.0f;
                    XposedHelpers.setFloatField(expandableView, "mAppearAnimationTranslation", 0);
                }
            }
            float targetValue;
            if (isAppearing) {
                XposedHelpers.setObjectField(expandableView, "mCurrentAppearInterpolator", XposedHelpers.getObjectField(expandableView, "mSlowOutFastInInterpolator"));
                XposedHelpers.setObjectField(expandableView, "mCurrentAlphaInterpolator", Interpolators.LINEAR_OUT_SLOW_IN);
                targetValue = 1.0f;
            } else {
                XposedHelpers.setObjectField(expandableView, "mCurrentAppearInterpolator", Interpolators.FAST_OUT_SLOW_IN);
                XposedHelpers.setObjectField(expandableView, "mCurrentAlphaInterpolator", XposedHelpers.getObjectField(expandableView, "mSlowOutLinearInInterpolator"));
                targetValue = 0.0f;
            }
            XposedHelpers.setObjectField(expandableView, "mAppearAnimator", ValueAnimator.ofFloat(mAppearAnimationFraction,
                    targetValue));
            ValueAnimator mAppearAnimator = (ValueAnimator) XposedHelpers.getObjectField(expandableView, "mAppearAnimator");
            mAppearAnimator.setInterpolator(Interpolators.LINEAR);
            mAppearAnimator.setDuration(
                    (long) (duration * Math.abs(mAppearAnimationFraction - targetValue)));
            mAppearAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    XposedHelpers.setObjectField(expandableView, "mAppearAnimationFraction", animation.getAnimatedValue());
                    try {
                        methodUpdateAppearAnimationAlpha.invoke(expandableView);
                        methodUpdateAppearRect.invoke(expandableView);
                    } catch (IllegalAccessException | InvocationTargetException ignore) {
                        XposedHook.logI(TAG, ignore.toString());
                    }
                    expandableView.invalidate();
                }
            });
            if (delay > 0) {
                // we need to apply the initial state already to avoid drawn frames in the wrong state
                methodUpdateAppearAnimationAlpha.invoke(expandableView);
                methodUpdateAppearRect.invoke(expandableView);
                mAppearAnimator.setStartDelay(delay);
            }
            mAppearAnimator.addListener(new AnimatorListenerAdapter() {
                private boolean mWasCancelled;

                @Override
                public void onAnimationEnd(Animator animation) {
                    if (onFinishedRunnable != null) {
                        onFinishedRunnable.run();
                    }
                    if (!mWasCancelled) {
                        try {
                            methodEnableAppearDrawing.invoke(expandableView, false);
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            XposedHook.logI(TAG, e.toString());
                        }
                    }
                }

                @Override
                public void onAnimationStart(Animator animation) {
                    mWasCancelled = false;
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    mWasCancelled = true;
                }
            });
            mAppearAnimator.start();
            return null;
        }
    };

    private static XC_MethodHook setDarkHook = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            Object expandableView = param.thisObject;
            View mBackgroundNormal = (View) XposedHelpers.getObjectField(expandableView, "mBackgroundNormal");
            View mBackgroundDimmed = (View) XposedHelpers.getObjectField(expandableView, "mBackgroundDimmed");
            mBackgroundDimmedVisibility = mBackgroundDimmed.getVisibility();
            mBackgroundNormalVisibility = mBackgroundNormal.getVisibility();
            mDark = XposedHelpers.getBooleanField(expandableView, "mDark");
            XposedHelpers.setBooleanField(expandableView, "mDark", (boolean) param.args[0]);
        }

        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            Object expandableView = param.thisObject;
            boolean dark = (boolean) param.args[0];
            boolean fade = (boolean) param.args[1];
            long delay = (long) param.args[2];
            View mBackgroundNormal = (View) XposedHelpers.getObjectField(expandableView, "mBackgroundNormal");
            View mBackgroundDimmed = (View) XposedHelpers.getObjectField(expandableView, "mBackgroundDimmed");
            mBackgroundDimmed.setVisibility(mBackgroundDimmedVisibility);
            mBackgroundNormal.setVisibility(mBackgroundNormalVisibility);

            if (mDark == dark) {
                return;
            }
            XposedHelpers.setBooleanField(expandableView, "mDark", dark);
            mDark = dark;
            updateBackground(expandableView);
            if (!dark && fade && !mDark) {
                methodFadeInFromDark.invoke(expandableView, delay);
            }
            updateOutlineAlpha(ExpandableOutlineViewHelper.getInstance(expandableView));
        }
    };

    private static XC_MethodReplacement startActivateAnimation = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            final FrameLayout expandableView = (FrameLayout) param.thisObject;
            final boolean reverse = (boolean) param.args[0];
            View mBackgroundNormal = (View) XposedHelpers.getObjectField(expandableView, "mBackgroundNormal");
            if (!expandableView.isAttachedToWindow()) {
                return null;
            }
            int widthHalf = mBackgroundNormal.getWidth()/2;
            int heightHalf = ((int) XposedHelpers.callMethod(mBackgroundNormal, "getActualHeight"))/2;
            float radius = (float) Math.sqrt(widthHalf*widthHalf + heightHalf*heightHalf);
            Animator animator;
            if (reverse) {
                animator = ViewAnimationUtils.createCircularReveal(mBackgroundNormal,
                        widthHalf, heightHalf, radius, 0);
            } else {
                animator = ViewAnimationUtils.createCircularReveal(mBackgroundNormal,
                        widthHalf, heightHalf, 0, radius);
            }
            mBackgroundNormal.setVisibility(View.VISIBLE);
            Interpolator interpolator;
            Interpolator alphaInterpolator;
            if (!reverse) {
                interpolator = Interpolators.LINEAR_OUT_SLOW_IN;
                alphaInterpolator = Interpolators.LINEAR_OUT_SLOW_IN;
            } else {
                interpolator = ACTIVATE_INVERSE_INTERPOLATOR;
                alphaInterpolator = ACTIVATE_INVERSE_ALPHA_INTERPOLATOR;
            }
            animator.setInterpolator(interpolator);
            animator.setDuration(ACTIVATE_ANIMATION_LENGTH);
            if (reverse) {
                mBackgroundNormal.setAlpha(1f);
                animator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                            updateBackground(expandableView);
                    }
                });
                animator.start();
            } else {
                mBackgroundNormal.setAlpha(0.4f);
                animator.start();
            }
            mBackgroundNormal.animate()
                    .alpha(reverse ? 0f : 1f)
                    .setInterpolator(alphaInterpolator)
                    .setUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(ValueAnimator animation) {
                            float animatedFraction = animation.getAnimatedFraction();
                            if (reverse) {
                                animatedFraction = 1.0f - animatedFraction;
                            }
                            setNormalBackgroundVisibilityAmount(expandableView, animatedFraction);
                        }
                    })
                    .setDuration(ACTIVATE_ANIMATION_LENGTH);
            return null;
        }
    };

    private static XC_MethodReplacement fadeInFromDark = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            Object expandableView = param.thisObject;
            long delay = (long) param.args[0];
            ExpandableOutlineViewHelper helper = ExpandableOutlineViewHelper.getInstance(expandableView);
            View mBackgroundNormal = helper.getBackgroundNormal();
            View mBackgroundDimmed = helper.getBackgroundDimmed();
            final View background = XposedHelpers.getBooleanField(expandableView, "mDimmed") ? mBackgroundDimmed : mBackgroundNormal;
            background.setAlpha(0f);
            helper.mBackgroundVisibilityUpdater.onAnimationUpdate(null);
            background.setPivotX(mBackgroundDimmed.getWidth() / 2f);
            background.setPivotY(((int) XposedHelpers.callMethod(expandableView, "getActualHeight")) / 2f);
            background.setScaleX(DARK_EXIT_SCALE_START);
            background.setScaleY(DARK_EXIT_SCALE_START);
            background.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(DARK_ANIMATION_LENGTH)
                    .setStartDelay(delay)
                    .setInterpolator(Interpolators.LINEAR_OUT_SLOW_IN)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationCancel(Animator animation) {
                            // Jump state if we are cancelled
                            background.setScaleX(1f);
                            background.setScaleY(1f);
                            background.setAlpha(1f);
                        }
                    })
                    .setUpdateListener(helper.mBackgroundVisibilityUpdater)
                    .start();
            helper.mFadeInFromDarkAnimator = TimeAnimator.ofFloat(0.0f, 1.0f);
            helper.mFadeInFromDarkAnimator.setDuration(DARK_ANIMATION_LENGTH);
            helper.mFadeInFromDarkAnimator.setStartDelay(delay);
            helper.mFadeInFromDarkAnimator.setInterpolator(Interpolators.LINEAR_OUT_SLOW_IN);
            helper.mFadeInFromDarkAnimator.addListener(helper.mFadeInEndListener);
            helper.mFadeInFromDarkAnimator.addUpdateListener(helper.mUpdateOutlineListener);
            helper.mFadeInFromDarkAnimator.start();
            return null;
        }
    };

    private static XC_MethodReplacement makeInactive = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            Object expandableView = param.thisObject;
            boolean animate = (boolean) param.args[0];
            Object mOnActivatedListener = fieldOnActivatedListener.get(expandableView);
            if (XposedHelpers.getBooleanField(expandableView, "mActivated")) {
                XposedHelpers.setBooleanField(expandableView, "mActivated",  false);
                if (XposedHelpers.getBooleanField(expandableView, "mDimmed")) {
                    if (animate) {
                        methodStartActivateAnimation.invoke(expandableView,  true /* reverse */);
                    } else {
                        updateBackground(expandableView);
                    }
                }
            }
            if (mOnActivatedListener != null) {
                XposedHelpers.callMethod(mOnActivatedListener, "onActivationReset", expandableView);
            }
            ((FrameLayout) expandableView).removeCallbacks((Runnable) XposedHelpers.getObjectField(expandableView, "mTapTimeoutRunnable"));
            return null;
        }
    };

    private static XC_MethodReplacement fadeDimmedBackground = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            final Object expandableView = param.thisObject;
            final ExpandableOutlineViewHelper helper = ExpandableOutlineViewHelper.getInstance(expandableView);
            ValueAnimator mBackgroundAnimator = (ValueAnimator) XposedHelpers.getObjectField(expandableView, "mBackgroundAnimator");
            View mBackgroundNormal = helper.getBackgroundNormal();
            View mBackgroundDimmed = helper.getBackgroundDimmed();
            boolean mActivated = XposedHelpers.getBooleanField(expandableView, "mActivated");
            boolean mDimmed = XposedHelpers.getBooleanField(expandableView, "mDimmed");
            boolean mDark = XposedHelpers.getBooleanField(expandableView, "mDark");
            mBackgroundDimmed.animate().cancel();
            mBackgroundNormal.animate().cancel();
            if (mActivated) {
                updateBackground(expandableView);
                return null;
            }
            if (!mDark) {
                if (mDimmed) {
                    mBackgroundDimmed.setVisibility(View.VISIBLE);
                } else {
                    mBackgroundNormal.setVisibility(View.VISIBLE);
                }
            }
            float startAlpha = mDimmed ? 1f : 0;
            float endAlpha = mDimmed ? 0 : 1f;
            int duration = BACKGROUND_ANIMATION_LENGTH_MS;
            // Check whether there is already a background animation running.
            if (mBackgroundAnimator != null) {
                startAlpha = (Float) mBackgroundAnimator.getAnimatedValue();
                duration = (int) mBackgroundAnimator.getCurrentPlayTime();
                mBackgroundAnimator.removeAllListeners();
                mBackgroundAnimator.cancel();
                if (duration <= 0) {
                    updateBackground(expandableView);
                    return null;
                }
            }
            mBackgroundNormal.setAlpha(startAlpha);
            mBackgroundAnimator =
                    ObjectAnimator.ofFloat(mBackgroundNormal, View.ALPHA, startAlpha, endAlpha);
            mBackgroundAnimator.setInterpolator(Interpolators.FAST_OUT_SLOW_IN);
            mBackgroundAnimator.setDuration(duration);
            mBackgroundAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    updateBackground(expandableView);
                    XposedHelpers.setObjectField(expandableView, "mBackgroundAnimator", null);
                    if (helper.mFadeInFromDarkAnimator == null) {
                        helper.mDimmedBackgroundFadeInAmount = -1;
                    }
                }
            });
            mBackgroundAnimator.addUpdateListener(helper.mBackgroundVisibilityUpdater);
            mBackgroundAnimator.start();
            return null;
        }
    };

    private static final XC_MethodReplacement updateAppearRect = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            View expandableView = (View) param.thisObject;
            ExpandableOutlineViewHelper helper = ExpandableOutlineViewHelper.getInstance(expandableView);
            float mAppearAnimationTranslation;
            float mAppearAnimationFraction = XposedHelpers.getFloatField(expandableView, "mAppearAnimationFraction");
            float mAnimationTranslationY = XposedHelpers.getFloatField(expandableView, "mAnimationTranslationY");
            RectF mAppearAnimationRect = (RectF) XposedHelpers.getObjectField(expandableView, "mAppearAnimationRect");
            Interpolator mCurrentAppearInterpolator = (Interpolator) XposedHelpers.getObjectField(expandableView, "mCurrentAppearInterpolator");

            float inverseFraction = (1.0f - mAppearAnimationFraction);
            float translationFraction = mCurrentAppearInterpolator.getInterpolation(inverseFraction);
            float translateYTotalAmount = translationFraction * mAnimationTranslationY;
            XposedHelpers.setFloatField(expandableView, "mAppearAnimationTranslation", translateYTotalAmount);
            mAppearAnimationTranslation = translateYTotalAmount;

            // handle width animation
            float widthFraction = (inverseFraction - (1.0f - 1.0f))
                    / (1.0f - 0.2f);
            widthFraction = Math.min(1.0f, Math.max(0.0f, widthFraction));
            widthFraction = mCurrentAppearInterpolator.getInterpolation(widthFraction);
            float left = (expandableView.getWidth() * (0.5f - 0.05f / 2.0f) *
                    widthFraction);
            float right = expandableView.getWidth() - left;

            // handle top animation
            float heightFraction = (inverseFraction - (1.0f - 1.0f)) /
                    1.0f;
            heightFraction = Math.max(0.0f, heightFraction);
            heightFraction = mCurrentAppearInterpolator.getInterpolation(heightFraction);

            float top;
            float bottom;
            final int actualHeight = (int) XposedHelpers.callMethod(expandableView, "getActualHeight");
            if (mAnimationTranslationY > 0.0f) {
                bottom = actualHeight - heightFraction * mAnimationTranslationY * 0.1f
                        - translateYTotalAmount;
                top = bottom * heightFraction;
            } else {
                top = heightFraction * (actualHeight + mAnimationTranslationY) * 0.1f -
                        translateYTotalAmount;
                bottom = actualHeight * (1 - heightFraction) + top * heightFraction;
            }
            mAppearAnimationRect.set(left, top, right, bottom);
            helper.setOutlineRect(left, top + mAppearAnimationTranslation, right,
                    bottom + mAppearAnimationTranslation);
            return null;
        }
    };

    protected static void updateOutlineAlpha(ExpandableOutlineViewHelper helper) {
        Object expandableView = helper.mExpandableView;
        if (XposedHelpers.getBooleanField(expandableView, "mDark")) {
            helper.setOutlineAlpha(0f);
            return;
        }
        float alpha = BACKGROUND_ALPHA_DIMMED;
        alpha = (alpha + (1.0f - alpha) * helper.mNormalBackgroundVisibilityAmount);
        alpha *= helper.mShadowAlpha;
        if (helper.mFadeInFromDarkAnimator != null) {
            alpha *= helper.mFadeInFromDarkAnimator.getAnimatedFraction();
        }
        helper.setOutlineAlpha(alpha);
    }

    public static void setNormalBackgroundVisibilityAmount(Object expandableView, float normalBackgroundVisibilityAmount) {
        ExpandableOutlineViewHelper helper = ExpandableOutlineViewHelper.getInstance(expandableView);
        helper.mNormalBackgroundVisibilityAmount = normalBackgroundVisibilityAmount;
        updateOutlineAlpha(helper);
    }

    private static void updateBackgroundTint(final FrameLayout expandableView, boolean animated) {
        final ExpandableOutlineViewHelper helper = ExpandableOutlineViewHelper.getInstance(expandableView);
        View mBackgroundNormal = helper.getBackgroundNormal();
        View mBackgroundDimmed = helper.getBackgroundDimmed();
        ValueAnimator mBackgroundColorAnimator = helper.mBackgroundColorAnimator;
        int mCurrentBackgroundTint = helper.mCurrentBackgroundTint;
        if (mBackgroundColorAnimator != null) {
            mBackgroundColorAnimator.cancel();
        }
        int rippleColor = (int) XposedHelpers.callMethod(expandableView, "getRippleColor");
        XposedHelpers.callMethod(mBackgroundDimmed, "setRippleColor", rippleColor);
        XposedHelpers.callMethod(mBackgroundNormal, "setRippleColor", rippleColor);
        int color = calculateBgColor(expandableView, true);
        if (!animated) {
            setBackgroundTintColor(helper, color);
        } else if (color != mCurrentBackgroundTint) {
            helper.mStartTint = mCurrentBackgroundTint;
            helper.mTargetTint = color;
            mBackgroundColorAnimator = ValueAnimator.ofFloat(0.0f, 1.0f);
            mBackgroundColorAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    int newColor = NotificationUtils.interpolateColors(helper.mStartTint, helper.mTargetTint,
                            animation.getAnimatedFraction());
                    setBackgroundTintColor(helper, newColor);
                }
            });
            mBackgroundColorAnimator.setDuration(ANIMATION_DURATION_STANDARD);
            mBackgroundColorAnimator.setInterpolator(Interpolators.LINEAR);
            mBackgroundColorAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    helper.mBackgroundColorAnimator = null;
                }
            });
            mBackgroundColorAnimator.start();
        }
    }

    private static void setBackgroundTintColor(ExpandableOutlineViewHelper helper, int color) {
        Object expandableView = helper.mExpandableView;
        View mBackgroundNormal = helper.getBackgroundNormal();
        View mBackgroundDimmed = helper.getBackgroundDimmed();
        helper.mCurrentBackgroundTint = color;
        if (color == XposedHelpers.getIntField(expandableView, "mNormalColor")) {
            // We don't need to tint a normal notification
            color = 0;
        }
        XposedHelpers.callMethod(mBackgroundDimmed, "setTint", color);
        XposedHelpers.callMethod(mBackgroundNormal, "setTint", color);
    }

    private static int calculateBgColor(Object expandableView, boolean withTint) {
        int mBgTint = XposedHelpers.getIntField(expandableView, "mBgTint");
        boolean mShowingLegacyBackground = XposedHelpers.getBooleanField(expandableView, "mShowingLegacyBackground");
        boolean mIsBelowSpeedBump = XposedHelpers.getBooleanField(expandableView, "mIsBelowSpeedBump");
        if (withTint && mBgTint != 0) {
            return mBgTint;
        } else if (mShowingLegacyBackground) {
            return XposedHelpers.getIntField(expandableView, "mLegacyColor");
        } else if (mIsBelowSpeedBump) {
            return XposedHelpers.getIntField(expandableView, "mLowPriorityColor");
        } else {
            return XposedHelpers.getIntField(expandableView, "mNormalColor");
        }
    }

    private static void updateBackgroundAlpha(ExpandableOutlineViewHelper helper, float transformationAmount) {
        Object expandableView = helper.mExpandableView;
        View mBackgroundDimmed = helper.getBackgroundDimmed();
        helper.mBgAlpha =  XposedHelpers.getBooleanField(expandableView, "mDimmed") ? transformationAmount : 1f;
        if (helper.mDimmedBackgroundFadeInAmount != -1) {
            helper.mBgAlpha *= helper.mDimmedBackgroundFadeInAmount;
        }
        mBackgroundDimmed.setAlpha(helper.mBgAlpha);
    }

    private static void resetBackgroundAlpha(ExpandableOutlineViewHelper helper) {
        updateBackgroundAlpha(helper, 0f /* transformationAmount */);
    }

    public static void updateBackground(Object expandableView) {
        try {
            methodUpdateBackground.invoke(expandableView);
        } catch (IllegalAccessException | InvocationTargetException ignore) {}
    }
}
