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
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.OverScroller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.extracted.systemui.FakeShadowView;
import tk.wasdennnoch.androidn_ify.extracted.systemui.Interpolators;
import tk.wasdennnoch.androidn_ify.systemui.notifications.NotificationPanelHooks;
import tk.wasdennnoch.androidn_ify.utils.ConfigUtils;
import tk.wasdennnoch.androidn_ify.utils.ResourceUtils;

import static tk.wasdennnoch.androidn_ify.XposedHook.PACKAGE_SYSTEMUI;

public class NotificationStackScrollLayoutHooks implements View.OnApplyWindowInsetsListener {
    private static final String TAG = "NotificationStackScrollLayoutHooks";

    private static final int ANIMATION_DURATION_STANDARD = 360;

    private int TAG_ANIMATOR_TRANSLATION_Y;
    private int TAG_END_TRANSLATION_Y;

    private Class<?> classActivatableNotificationView;
    private Class<?> classStackStateAnimator;
    private ViewGroup mStackScrollLayout;
    private Context mContext;
    private ResourceUtils mRes;
    private final Paint mBackgroundPaint = new Paint();
    private OverScroller mScroller;
    private Object mAmbientState;
    private Object mSwipeHelper;
    private ArrayList<View> mDraggedViews;

    private boolean mDisallowDismissInThisMotion;
    private boolean mAnimationRunning;
    private boolean mAnimationsEnabled = true;
    private boolean mDontClampNextScroll;
    private boolean mContinuousShadowUpdate;
    private boolean mIsExpanded;
    private ViewTreeObserver.OnPreDrawListener mBackgroundUpdater
            = new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
               updateBackground();
               return true;
           }
    };
    private ViewTreeObserver.OnPreDrawListener mShadowUpdater
            = new ViewTreeObserver.OnPreDrawListener() {

        @Override
        public boolean onPreDraw() {
            updateViewShadows();
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
    private int mBottomInset = 0;
    private int mTopPadding;
    private float mStackTranslation;
    private View mForcedScroll = null;
    private PorterDuffXfermode mSrcMode = new PorterDuffXfermode(PorterDuff.Mode.SRC);
    private ArrayList<View> mTmpSortedChildren = new ArrayList<>();
    private Comparator<View> mViewPositionComparator = new Comparator<View>() {
        @Override
        public int compare(View view, View otherView) {
            float endY = view.getTranslationY() + XposedHelpers.getIntField(view, "mActualHeight");
            float otherEndY = otherView.getTranslationY() + XposedHelpers.getIntField(otherView, "mActualHeight");
            if (endY < otherEndY) {
                return -1;
            } else if (endY > otherEndY) {
                return 1;
            } else {
                // The two notifications end at the same location
                return 0;
            }
        }
    };

    public NotificationStackScrollLayoutHooks(ClassLoader classLoader) {
        try {
            Class classNotificationStackScrollLayout = XposedHelpers.findClass("com.android.systemui.statusbar.stack.NotificationStackScrollLayout", classLoader);
            XposedBridge.hookAllMethods(classNotificationStackScrollLayout, "initView", new XC_MethodHook() {
                @SuppressWarnings("unchecked")
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    mStackScrollLayout = (ViewGroup) param.thisObject;
                    mScroller = (OverScroller) XposedHelpers.getObjectField(param.thisObject, "mScroller");
                    mAmbientState = XposedHelpers.getObjectField(param.thisObject, "mAmbientState");
                    mDraggedViews = (ArrayList<View>) XposedHelpers.getObjectField(mAmbientState, "mDraggedViews");
                    mSwipeHelper = XposedHelpers.getObjectField(param.thisObject, "mSwipeHelper");
                    mContext = (Context) param.args[0];
                    mRes = ResourceUtils.getInstance(mContext);
                    initView();
                    hookSwipeHelper();
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
                        updateViewShadows();
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
                    updateViewShadows();
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
            XposedHelpers.findAndHookMethod(classNotificationStackScrollLayout, "updateSwipeProgress", View.class, boolean.class, float.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    param.setResult(true); // Don't fade out the notification
                }
            });
            XposedHelpers.findAndHookMethod(classNotificationStackScrollLayout, "onChildSnappedBack", View.class, new XC_MethodHook() {
                @SuppressWarnings("SuspiciousMethodCalls")
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    mDraggedViews.remove(param.args[0]);
                    updateContinuousShadowDrawing();
                }
            });
            XposedHelpers.findAndHookMethod(classNotificationStackScrollLayout, "onBeginDrag", View.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    updateContinuousShadowDrawing();
                }
            });
            XposedHelpers.findAndHookMethod(classNotificationStackScrollLayout, "updateSpeedBumpIndex", int.class, new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                    int newIndex = (int) param.args[0];
                    XposedHelpers.callMethod(mAmbientState, "setSpeedBumpIndex", newIndex);
                    return null;
                }
            });
            XposedHelpers.findAndHookMethod(classNotificationStackScrollLayout, "initDownStates", MotionEvent.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    MotionEvent ev = (MotionEvent) param.args[0];
                    if (ev.getAction() == MotionEvent.ACTION_DOWN)
                        mDisallowDismissInThisMotion = false;
                }
            });
            XposedHelpers.findAndHookMethod(classNotificationStackScrollLayout, "onScrollTouch", MotionEvent.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    mForcedScroll = null;
                }
            });
            XposedHelpers.findAndHookMethod(classNotificationStackScrollLayout, "updateChildren", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    updateForcedScroll();
                }
            });
            XposedHelpers.findAndHookMethod(classNotificationStackScrollLayout, "computeScroll", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!mScroller.isFinished()) {
                        mDontClampNextScroll = false;
                    }
                }
            });
            XposedHelpers.findAndHookMethod(classNotificationStackScrollLayout, "overScrollBy", int.class, int.class,
                    int.class, int.class,
                    int.class, int.class,
                    int.class, int.class,
                    boolean.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            if (mDontClampNextScroll) {
                                int range = (int) param.args[5];
                                range = Math.max(range, getOwnScrollY());
                                param.args[5] = range;
                            }
                        }
                    });
            XposedBridge.hookAllMethods(classNotificationStackScrollLayout, "setSpeedBumpView", XC_MethodReplacement.DO_NOTHING);
            XposedHelpers.findAndHookMethod(classNotificationStackScrollLayout, "updateSpeedBump", boolean.class, XC_MethodReplacement.DO_NOTHING);
            classActivatableNotificationView = XposedHelpers.findClass("com.android.systemui.statusbar.ActivatableNotificationView", classLoader);
            classStackStateAnimator = XposedHelpers.findClass("com.android.systemui.statusbar.stack.StackStateAnimator", classLoader);
        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error hooking NotificationStackScrollLayout", t);
        }
    }

    private void hookSwipeHelper() {
        Class classSwipeHelper = mSwipeHelper.getClass();
        XC_MethodHook touchEventHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (mDisallowDismissInThisMotion && param.thisObject == mSwipeHelper)
                    param.setResult(false);
            }
        };
        XposedHelpers.findAndHookMethod(classSwipeHelper, "onTouchEvent", MotionEvent.class, touchEventHook);
        XposedHelpers.findAndHookMethod(classSwipeHelper, "onInterceptTouchEvent", MotionEvent.class, touchEventHook);
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
        mStackScrollLayout.setOnApplyWindowInsetsListener(NotificationStackScrollLayoutHooks.this);
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
            updateContinuousShadowDrawing();
        }
    }

    private void updateContinuousShadowDrawing() {
        boolean continuousShadowUpdate = mAnimationRunning
                || !mDraggedViews.isEmpty();
        if (continuousShadowUpdate != mContinuousShadowUpdate) {
            if (continuousShadowUpdate) {
                mStackScrollLayout.getViewTreeObserver().addOnPreDrawListener(mShadowUpdater);
            } else {
                mStackScrollLayout.getViewTreeObserver().removeOnPreDrawListener(mShadowUpdater);
            }
            mContinuousShadowUpdate = continuousShadowUpdate;
        }
    }

    @SuppressWarnings("ConstantConditions")
    private void updateViewShadows() {
        // we need to work around an issue where the shadow would not cast between siblings when
        // their z difference is between 0 and 0.1

        // Lefts first sort by Z difference
        for (int i = 0; i < mStackScrollLayout.getChildCount(); i++) {
            View child = mStackScrollLayout.getChildAt(i);
            if (child.getVisibility() != View.GONE) {
                mTmpSortedChildren.add(child);
            }
        }
        Collections.sort(mTmpSortedChildren, mViewPositionComparator);

        // Now lets update the shadow for the views
        View previous = null;
        for (int i = 0; i < mTmpSortedChildren.size(); i++) {
            View expandableView = mTmpSortedChildren.get(i);
            float translationZ = expandableView.getTranslationZ();
            float otherZ = previous == null ? translationZ : previous.getTranslationZ();
            float diff = otherZ - translationZ;
            if (diff <= 0.0f || diff >= FakeShadowView.SHADOW_SIBLING_TRESHOLD) {
                // There is no fake shadow to be drawn
                setFakeShadowIntensity(expandableView, 0.0f, 0.0f, 0, 0);
            } else {
                float yLocation = previous.getTranslationY() + XposedHelpers.getIntField(previous, "mActualHeight") -
                        expandableView.getTranslationY();
                setFakeShadowIntensity(expandableView,
                        diff / FakeShadowView.SHADOW_SIBLING_TRESHOLD,
                        1, (int) yLocation,
                        getOutlineTranslation(previous));
            }
            previous = expandableView;
        }

        mTmpSortedChildren.clear();
    }

    private int getOutlineTranslation(View expandableOutlineView) {
        try {
            if (XposedHelpers.getBooleanField(expandableOutlineView, "mCustomOutline")) {
                return (int) expandableOutlineView.getTranslationX();
            } else {
                Rect mOutlineRect = (Rect) XposedHelpers.getObjectField(expandableOutlineView, "mOutlineRect");
                return mOutlineRect.left;
            }
        } catch (Throwable t) {
            return 0;
        }
    }

    private void setFakeShadowIntensity(View activatableNotificationView, float shadowIntensity, float outlineAlpha, int shadowYEnd,
                                       int outlineTranslation) {
        FakeShadowView mFakeShadow = (FakeShadowView) activatableNotificationView.findViewById(R.id.fake_shadow);
        if (mFakeShadow != null)
            mFakeShadow.setFakeShadowTranslationZ(shadowIntensity * (activatableNotificationView.getTranslationZ()
                            + FakeShadowView.SHADOW_SIBLING_TRESHOLD), outlineAlpha, shadowYEnd,
                    outlineTranslation);
    }

    public void requestDisallowDismiss() {
        mDisallowDismissInThisMotion = true;
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

    private void updateForcedScroll() {
        if (mForcedScroll != null && (!mForcedScroll.hasFocus()
                || !mForcedScroll.isAttachedToWindow())) {
            mForcedScroll = null;
        }
        if (mForcedScroll != null) {
            View expandableView = mForcedScroll;
            int positionInLinearLayout = getPositionInLinearLayout(expandableView);
            int targetScroll = targetScrollForView(expandableView, positionInLinearLayout);
            int outOfViewScroll = positionInLinearLayout + getIntrinsicHeight(expandableView);

            targetScroll = Math.max(0, Math.min(targetScroll, getScrollRange()));

            // Only apply the scroll if we're scrolling the view upwards, or the view is so far up
            // that it is not visible anymore.
            int mOwnScrollY = getOwnScrollY();
            if (mOwnScrollY < targetScroll || outOfViewScroll < mOwnScrollY) {
                setOwnScrollY(mOwnScrollY);
            }
        }
    }

    public void lockScrollTo(View v) {
        if (mForcedScroll == v) {
            return;
        }
        mForcedScroll = v;
        scrollTo(v);
    }

    private boolean scrollTo(View v) {
        int positionInLinearLayout = getPositionInLinearLayout(v);
        int targetScroll = targetScrollForView(v, positionInLinearLayout);
        int outOfViewScroll = positionInLinearLayout + getIntrinsicHeight(v);

        // Only apply the scroll if we're scrolling the view upwards, or the view is so far up
        // that it is not visible anymore.
        int mOwnScrollY = getOwnScrollY();
        if (mOwnScrollY < targetScroll || outOfViewScroll < mOwnScrollY) {
            mScroller.startScroll(getScrollX(), mOwnScrollY, 0, targetScroll - mOwnScrollY);
            dontReportNextOverScroll();
            mStackScrollLayout.postInvalidateOnAnimation();
            return true;
        }
        return false;
    }

    @Override
    public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
        mBottomInset = insets.getSystemWindowInsetBottom();

        int range = getScrollRange();
        if (getOwnScrollY() > range) {
            // HACK: We're repeatedly getting staggered insets here while the IME is
            // animating away. To work around that we'll wait until things have settled.
            mStackScrollLayout.removeCallbacks(mReclamp);
            mStackScrollLayout.postDelayed(mReclamp, 50);
        } else if (mForcedScroll != null) {
            // The scroll was requested before we got the actual inset - in case we need
            // to scroll up some more do so now.
            scrollTo(mForcedScroll);
        }
        return insets;
    }

    private int targetScrollForView(View v, int positionInLinearLayout) {
        return positionInLinearLayout + getIntrinsicHeight(v) +
                getImeInset() - mStackScrollLayout.getHeight() + mTopPadding;
    }

    private int getImeInset() {
        return Math.max(0, mBottomInset - (mStackScrollLayout.getRootView().getHeight() - mStackScrollLayout.getHeight()));
    }

    private int getScrollX() {
        return XposedHelpers.getIntField(mStackScrollLayout, "mScrollX");
    }

    private int getOwnScrollY() {
        return XposedHelpers.getIntField(mStackScrollLayout, "mOwnScrollY");
    }

    private void setOwnScrollY(int ownScrollY) {
        XposedHelpers.setIntField(mStackScrollLayout, "mOwnScrollY", ownScrollY);
    }

    private Runnable mReclamp = new Runnable() {
        @Override
        public void run() {
            int range = getScrollRange();
            int mOwnScrollY = getOwnScrollY();
            mScroller.startScroll(getScrollX(), mOwnScrollY, 0, range - mOwnScrollY);
            dontReportNextOverScroll();
            mDontClampNextScroll = true;
            mStackScrollLayout.postInvalidateOnAnimation();
        }
    };

    private void dontReportNextOverScroll() {
        XposedHelpers.setBooleanField(mStackScrollLayout, "mDontReportNextOverScroll", true);
    }

    private int getIntrinsicHeight(View v) {
        return (int) XposedHelpers.callMethod(v, "getIntrinsicHeight");
    }

    private int getPositionInLinearLayout(View v) {
        return (int) XposedHelpers.callMethod(mStackScrollLayout, "getPositionInLinearLayout", v);
    }

    private int getScrollRange() {
        return (int) XposedHelpers.callMethod(mStackScrollLayout, "getScrollRange");
    }
}
