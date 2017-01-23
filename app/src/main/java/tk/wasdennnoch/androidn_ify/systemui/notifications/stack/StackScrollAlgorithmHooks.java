package tk.wasdennnoch.androidn_ify.systemui.notifications.stack;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.extracted.systemui.FakeShadowView;
import tk.wasdennnoch.androidn_ify.extracted.systemui.stack.PiecewiseLinearIndentationFunctor;
import tk.wasdennnoch.androidn_ify.extracted.systemui.stack.StackIndentationFunctor;
import tk.wasdennnoch.androidn_ify.utils.ConfigUtils;
import tk.wasdennnoch.androidn_ify.utils.ResourceUtils;

public class StackScrollAlgorithmHooks {

    private static int mBottomStackPeekSize;
    private static int mCollapseSecondCardPadding;
    private static boolean isExpansionChanging;
    private static final String TAG = "StackScrollAlgorithmHooks";

    public static final int LOCATION_UNKNOWN = 0x00;
    public static final int LOCATION_FIRST_CARD = 0x01;
    public static final int LOCATION_TOP_STACK_HIDDEN = 0x02;
    public static final int LOCATION_TOP_STACK_PEEKING = 0x04;
    public static final int LOCATION_MAIN_AREA = 0x08;
    public static final int LOCATION_BOTTOM_STACK_PEEKING = 0x10;
    public static final int LOCATION_BOTTOM_STACK_HIDDEN = 0x20;
    public static final int LOCATION_GONE = 0x40;

    private static final int MAX_ITEMS_IN_BOTTOM_STACK = 3;

    private static final Rect mClipBounds = new Rect();
    public static ViewGroup mStackScrollLayout;
    public static Object mStackScrollAlgorithm;
    private static float mStackTop = 0;
    private static float mStateTop = 0;

    private static int mLayoutMinHeight;

    private static StackIndentationFunctor mBottomStackIndentationFunctor;

    private static Field fieldCollapsedSize;
    private static Field fieldVisibleChildren;
    private static Field fieldScrollY;
    private static Field fieldShadeExpanded;
    private static Field fieldHeadsUpHeight;
    private static Field fieldTopPadding;
    private static Field fieldStackTranslation;
    private static Field fieldYTranslation;
    private static Field fieldLocation;
    private static Field fieldIsExpansionChanging;
    private static Field fieldHeight;
    private static Field fieldBottomStackSlowDownLength;
    private static Field fieldPaddingBetweenElements;

    private static Field fieldTempAlgorithmState;
    private static Field fieldZDistanceBetweenElements;
    private static Field fieldZBasicHeight;
    private static Field fieldIsHeadsUp;
    private static Field fieldClipTopAmount;
    private static Field fieldItemsInBottomStack;
    private static Field fieldPartialInBottom;
    private static Field fieldZTranslation;
    private static Field fieldAlpha;
    private static Field fieldBottomStackPeekSize;
    private static Field fieldIsExpanded;
    private static Field fieldLayoutHeight;
    private static Field fieldTrackingHeadsUp;

    private static Method methodGetViewStateForView;
    private static Method methodGetTopHeadsUpEntry;
    private static Method methodGetInnerHeight;
    private static Method methodGetScrollY;
    private static Method methodGetMinHeight;

    private static Method methodHandleDraggedViews;
    private static Method methodUpdateDimmedActivatedHideSensitive;
    private static Method methodUpdateSpeedBumpState;
    private static Method methodGetNotificationChildrenStates;
    private static Method methodGetSpeedBumpIndex;
    private static Method methodGetTopPadding;
    private static Method methodGetStackTranslation;
    private static Method methodIsTransparent;
    private static Method methodIsPinned;
    private static Method methodGetBottomStackSlowDownLength;
    private static Method methodResetViewStates;
    private static Method methodGetOverScrollAmount;
    private static Method methodGetHostView;
    private static Method methodUpdateNotGoneIndex;
    private static Method methodGetNotificationChildren;
    private static Method methodGetHeight;
    private static Method methodGetIntrinsicHeight;
    private static Method methodGetHeadsUpHeight;
    private static Method methodGetMaxHeadsUpTranslation;
    private static Method methodGetIntrinsicHeightRow;
    private static Method methodAreChildrenExpanded;

    private static Class<?> classNotificationStackScrollLayout;
    private static Class<?> classStackScrollAlgorithm;
    private static Class<?> classStackScrollAlgorithmState;
    private static Class<?> classStackScrollState;
    private static Class<?> classStackViewState;
    private static Class<?> classAmbientState;
    private static Class<?> classExpandableNotificationRow;
    private static Class<?> classExpandableView;
    private static Class<?> classHeadsUpManager;

    public static void hook(ClassLoader classLoader) {
        try {
            final ConfigUtils config = ConfigUtils.getInstance();

            classNotificationStackScrollLayout = XposedHelpers.findClass("com.android.systemui.statusbar.stack.NotificationStackScrollLayout", classLoader);
            XposedBridge.hookAllMethods(classNotificationStackScrollLayout, "initView", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    mStackScrollLayout = (ViewGroup) param.thisObject;
                    mStackScrollAlgorithm = XposedHelpers.getObjectField(mStackScrollLayout, "mStackScrollAlgorithm");
                }
            });

            classStackScrollAlgorithm = XposedHelpers.findClass("com.android.systemui.statusbar.stack.StackScrollAlgorithm", classLoader);
            XposedHelpers.findAndHookMethod(classStackScrollAlgorithm, "initConstants", Context.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    int mZDistanceBetweenElements = ResourceUtils.getInstance((Context) param.args[0])
                            .getDimensionPixelSize(R.dimen.z_distance_between_notifications);
                    int mZBasicHeight = 4 * mZDistanceBetweenElements;
                    XposedHelpers.setIntField(param.thisObject, "mZDistanceBetweenElements", mZDistanceBetweenElements);
                    XposedHelpers.setIntField(param.thisObject, "mZBasicHeight", mZBasicHeight);
                    mBottomStackPeekSize = XposedHelpers.getIntField(param.thisObject, "mBottomStackPeekSize");
                    mCollapseSecondCardPadding = XposedHelpers.getIntField(param.thisObject, "mCollapseSecondCardPadding");
                }
            });

            /*XposedBridge.hookAllMethods(classNotificationStackScrollLayout, "updateChildren", new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            if (((mStackTop == mStateTop || mStackScrollLayout == null)))
                                return;
                            mClipBounds.right = mStackScrollLayout.getWidth();
                            mClipBounds.left = 0;
                            mClipBounds.top = mStackScrollLayout.getTop();
                            mClipBounds.bottom = Integer.MAX_VALUE;
                            mStackScrollLayout.setClipBounds(mClipBounds);
                        }
                    }
            );*/

            if (!config.notifications.experimental) return;

            if (config.notifications.change_style) {
                classStackScrollAlgorithmState = XposedHelpers.findClass("com.android.systemui.statusbar.stack.StackScrollAlgorithm.StackScrollAlgorithmState", classLoader);
                classStackScrollState = XposedHelpers.findClass("com.android.systemui.statusbar.stack.StackScrollState", classLoader);
                classStackViewState = XposedHelpers.findClass("com.android.systemui.statusbar.stack.StackViewState", classLoader);
                classAmbientState = XposedHelpers.findClass("com.android.systemui.statusbar.stack.AmbientState", classLoader);
                classExpandableNotificationRow = XposedHelpers.findClass("com.android.systemui.statusbar.ExpandableNotificationRow", classLoader);
                classExpandableView = XposedHelpers.findClass("com.android.systemui.statusbar.ExpandableView", classLoader);
                classHeadsUpManager = XposedHelpers.findClass("com.android.systemui.statusbar.policy.HeadsUpManager", classLoader);

                //XposedHelpers.findAndHookMethod(classAmbientState, "getInnerHeight", getInnerHeight);

                fieldCollapsedSize = XposedHelpers.findField(classStackScrollAlgorithm, "mCollapsedSize");
                fieldVisibleChildren = XposedHelpers.findField(classStackScrollAlgorithmState, "visibleChildren");
                fieldScrollY = XposedHelpers.findField(classStackScrollAlgorithmState, "scrollY");
                fieldShadeExpanded = XposedHelpers.findField(classAmbientState, "mShadeExpanded");
                fieldHeadsUpHeight = XposedHelpers.findField(classExpandableNotificationRow, "mHeadsUpHeight");
                fieldTopPadding = XposedHelpers.findField(classAmbientState, "mTopPadding");
                fieldStackTranslation = XposedHelpers.findField(classAmbientState, "mStackTranslation");
                fieldYTranslation = XposedHelpers.findField(classStackViewState, "yTranslation");
                fieldLocation = XposedHelpers.findField(classStackViewState, "location");
                fieldIsExpansionChanging = XposedHelpers.findField(classStackScrollAlgorithm, "mIsExpansionChanging");
                fieldHeight = XposedHelpers.findField(classStackViewState, "height");
                fieldBottomStackSlowDownLength = XposedHelpers.findField(classStackScrollAlgorithm, "mBottomStackSlowDownLength");
                fieldPaddingBetweenElements = XposedHelpers.findField(classStackScrollAlgorithm, "mPaddingBetweenElements");

                fieldTempAlgorithmState = XposedHelpers.findField(classStackScrollAlgorithm, "mTempAlgorithmState");
                fieldZDistanceBetweenElements = XposedHelpers.findField(classStackScrollAlgorithm, "mZDistanceBetweenElements");
                fieldZBasicHeight = XposedHelpers.findField(classStackScrollAlgorithm, "mZBasicHeight");
                fieldZTranslation = XposedHelpers.findField(classStackViewState, "zTranslation");
                fieldIsHeadsUp = XposedHelpers.findField(classExpandableNotificationRow, "mIsHeadsUp");
                fieldClipTopAmount = XposedHelpers.findField(classStackViewState, "clipTopAmount");
                fieldItemsInBottomStack = XposedHelpers.findField(classStackScrollAlgorithmState, "itemsInBottomStack");
                fieldPartialInBottom = XposedHelpers.findField(classStackScrollAlgorithmState, "partialInBottom");
                fieldAlpha = XposedHelpers.findField(classStackViewState, "alpha");
                fieldBottomStackPeekSize = XposedHelpers.findField(classStackScrollAlgorithm, "mBottomStackPeekSize");
                fieldIsExpanded = XposedHelpers.findField(classStackScrollAlgorithm, "mIsExpanded");
                fieldLayoutHeight = XposedHelpers.findField(classAmbientState, "mLayoutHeight");

                fieldTrackingHeadsUp = XposedHelpers.findField(classHeadsUpManager, "mTrackingHeadsUp");

                methodGetViewStateForView = XposedHelpers.findMethodBestMatch(classStackScrollState, "getViewStateForView", View.class);
                methodGetTopHeadsUpEntry = XposedHelpers.findMethodBestMatch(classAmbientState, "getTopHeadsUpEntry");
                methodGetInnerHeight = XposedHelpers.findMethodBestMatch(classAmbientState, "getInnerHeight");
                methodGetScrollY = XposedHelpers.findMethodBestMatch(classAmbientState, "getScrollY");

                methodGetMinHeight = XposedHelpers.findMethodBestMatch(classExpandableView, "getMinHeight");

                methodHandleDraggedViews = XposedHelpers.findMethodBestMatch(classStackScrollAlgorithm, "handleDraggedViews", classAmbientState,classStackScrollState, classStackScrollAlgorithmState);
                methodUpdateDimmedActivatedHideSensitive = XposedHelpers.findMethodBestMatch(classStackScrollAlgorithm, "updateDimmedActivatedHideSensitive", classAmbientState,classStackScrollState, classStackScrollAlgorithmState);
                methodUpdateSpeedBumpState = XposedHelpers.findMethodBestMatch(classStackScrollAlgorithm, "updateSpeedBumpState", classStackScrollState, classStackScrollAlgorithmState, int.class);
                methodGetNotificationChildrenStates = XposedHelpers.findMethodBestMatch(classStackScrollAlgorithm, "getNotificationChildrenStates", classStackScrollState, classStackScrollAlgorithmState);
                methodGetBottomStackSlowDownLength = XposedHelpers.findMethodBestMatch(classStackScrollAlgorithm, "getBottomStackSlowDownLength");
                methodUpdateNotGoneIndex = XposedHelpers.findMethodBestMatch(classStackScrollAlgorithm, "updateNotGoneIndex", classStackScrollState, classStackScrollAlgorithmState, int.class, classExpandableView);

                methodGetSpeedBumpIndex = XposedHelpers.findMethodBestMatch(classAmbientState, "getSpeedBumpIndex");
                methodGetTopPadding = XposedHelpers.findMethodBestMatch(classAmbientState, "getTopPadding");
                methodGetStackTranslation = XposedHelpers.findMethodBestMatch(classAmbientState, "getStackTranslation");
                methodGetOverScrollAmount = XposedHelpers.findMethodBestMatch(classAmbientState, "getOverScrollAmount", boolean.class);
                methodGetMaxHeadsUpTranslation = XposedHelpers.findMethodBestMatch(classAmbientState, "getMaxHeadsUpTranslation");

                methodIsTransparent = XposedHelpers.findMethodBestMatch(classExpandableView, "isTransparent");
                methodGetHeight = XposedHelpers.findMethodBestMatch(classExpandableView, "getHeight");
                methodGetIntrinsicHeight = XposedHelpers.findMethodBestMatch(classExpandableView, "getIntrinsicHeight");

                methodIsPinned = XposedHelpers.findMethodBestMatch(classExpandableNotificationRow, "isPinned");
                methodGetNotificationChildren = XposedHelpers.findMethodBestMatch(classExpandableNotificationRow, "getNotificationChildren");
                methodGetHeadsUpHeight = XposedHelpers.findMethodBestMatch(classExpandableNotificationRow, "getHeadsUpHeight");
                methodGetIntrinsicHeightRow = XposedHelpers.findMethodBestMatch(classExpandableNotificationRow, "getIntrinsicHeight");
                methodAreChildrenExpanded = XposedHelpers.findMethodBestMatch(classExpandableNotificationRow, "areChildrenExpanded");

                methodResetViewStates = XposedHelpers.findMethodBestMatch(classStackScrollState, "resetViewStates");
                methodGetHostView = XposedHelpers.findMethodBestMatch(classStackScrollState, "getHostView");

                XposedBridge.hookAllMethods(classStackScrollAlgorithm, "initConstants", initConstantsHook);
                XposedBridge.hookAllMethods(classStackScrollAlgorithm, "getStackScrollState", getStackScrollState);

                /*XposedHelpers.findAndHookMethod(classNotificationStackScrollLayout, "onInterceptTouchEvent", MotionEvent.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        MotionEvent ev = (MotionEvent) param.args[0];
                        if (ev.getY() < mStateTop)
                            param.setResult(false);
                    }
                });*/
            }

        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error hooking SystemUI", t);
        }
    }

    private static int getInnerHeight() {
            return Math.max(getInt(fieldLayoutHeight, classAmbientState) - getInt(fieldTopPadding, classAmbientState), mLayoutMinHeight);
    }

    private static final XC_MethodHook initConstantsHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            int mBottomStackPeekSize = getInt(fieldTempAlgorithmState, mStackScrollLayout);
            setInt(fieldZDistanceBetweenElements, param.thisObject, Math.max(1, ResourceUtils.getInstance().getResources()
                    .getDimensionPixelSize(R.dimen.z_distance_between_notifications)));
            mBottomStackIndentationFunctor = new PiecewiseLinearIndentationFunctor(
                    MAX_ITEMS_IN_BOTTOM_STACK,
                    mBottomStackPeekSize,
                    (int)invoke(methodGetBottomStackSlowDownLength, param.thisObject),
                    0.5f);
        }
    };

    public  static final XC_MethodReplacement getStackScrollState = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            // The state of the local variables are saved in an algorithmState to easily subdivide it
            // into multiple phases.
            Object ambientState = param.args[0];
            Object resultState = param.args[1];
            Object algorithmState = get(fieldTempAlgorithmState, mStackScrollAlgorithm);

            // First we reset the view states to their default values.
            invoke(methodResetViewStates, resultState);

            initAlgorithmState(resultState, algorithmState, ambientState);
            updatePositionsForState(resultState, algorithmState, ambientState);
            updateZValuesForState(resultState, algorithmState, ambientState);
            updateHeadsUpStates(resultState, algorithmState, ambientState);
            invoke(methodHandleDraggedViews, mStackScrollAlgorithm, ambientState, resultState, algorithmState);
            invoke(methodUpdateDimmedActivatedHideSensitive, mStackScrollAlgorithm, ambientState, resultState, algorithmState);
            updateClipping(resultState, algorithmState, ambientState);
            invoke(methodUpdateSpeedBumpState, mStackScrollAlgorithm, resultState, algorithmState, (int)invoke(methodGetSpeedBumpIndex, ambientState));
            invoke(methodGetNotificationChildrenStates, mStackScrollAlgorithm, resultState, algorithmState);
            return null;
        }
    };

    public static void updateClipping(Object resultState,
                                      Object algorithmState, Object ambientState) {
        ArrayList<ViewGroup> visibleChildren = (ArrayList<ViewGroup>) get(fieldVisibleChildren, algorithmState);

        float drawStart = (float) invoke(methodGetTopPadding, ambientState) + (float) invoke(methodGetStackTranslation, ambientState);

        float previousNotificationEnd = 0;
        float previousNotificationStart = 0;

        int childCount = visibleChildren.size();
        for (int i = 0; i < childCount; i++) {
            Object child = visibleChildren.get(i);
            Object state = invoke(methodGetViewStateForView, resultState, child);
            boolean mIsHeadsUp = classExpandableNotificationRow.isInstance(child) && getBoolean(fieldIsHeadsUp, child);
            boolean isTransparent = classExpandableView.isInstance(child) && (boolean)invoke(methodIsTransparent, child);
            if (!mIsHeadsUp) {
                previousNotificationEnd = Math.max(drawStart, previousNotificationEnd);
                previousNotificationStart = Math.max(drawStart, previousNotificationStart);
            }
            float newYTranslation = getFloat(fieldYTranslation, state);
            float newHeight = getFloat(fieldHeight, state);

            float newNotificationEnd = newYTranslation + newHeight;
            boolean isHeadsUp = (classExpandableNotificationRow.isInstance(child))
                    && (boolean) invoke(methodIsPinned, child);
            if (newYTranslation < previousNotificationEnd
                    && (!isHeadsUp || getBoolean(fieldShadeExpanded, ambientState))) {
                // The previous view is overlapping on top, clip!
                float overlapAmount = previousNotificationEnd - newYTranslation;
                setInt(fieldClipTopAmount, state, (int) overlapAmount);
            } else {
                setInt(fieldClipTopAmount, state, 0);
            }
            if (!isTransparent) {
                // Only update the previous values if we are not transparent,
                // otherwise we would clip to a transparent view.
                previousNotificationEnd = newNotificationEnd;
                previousNotificationStart = newYTranslation;

            }
        }
    }

    private static void updateZValuesForState(Object resultState,
                                              Object algorithmState, Object ambientState) {

        int mZDistanceBetweenElements = getInt(fieldZDistanceBetweenElements, mStackScrollAlgorithm);
        int mZBasicHeight = getInt(fieldZBasicHeight, mStackScrollAlgorithm);
        float itemsInBottomStack = getFloat(fieldItemsInBottomStack, algorithmState);
        List<ViewGroup> visibleChildren = (List<ViewGroup>) get(fieldVisibleChildren, algorithmState);
        int childCount = visibleChildren.size();
        float childrenOnTop = 0.0f;
        for (int i = childCount - 1; i >= 0; i--) {
            View child = visibleChildren.get(i);
            boolean mIsHeadsUp = classExpandableNotificationRow.isInstance(child) && getBoolean(fieldIsHeadsUp, child);
            Object childViewState = invoke(methodGetViewStateForView, resultState, child);
            float yTranslation = getFloat(fieldYTranslation, childViewState);
            if (i > (childCount - 1 - itemsInBottomStack)) {
                // We are in the bottom stack
                float numItemsAbove = i - (childCount - 1 - itemsInBottomStack);
                float zSubtraction;
                if (numItemsAbove <= 1.0f) {
                    float factor = 0.2f;
                    // Lets fade in slower to the threshold to make the shadow fade in look nicer
                    if (numItemsAbove <= factor) {
                        zSubtraction = FakeShadowView.SHADOW_SIBLING_TRESHOLD
                                * numItemsAbove * (1.0f / factor);
                    } else {
                        zSubtraction = FakeShadowView.SHADOW_SIBLING_TRESHOLD
                                + (numItemsAbove - factor) * (1.0f / (1.0f - factor))
                                * (mZDistanceBetweenElements
                                - FakeShadowView.SHADOW_SIBLING_TRESHOLD);
                    }
                } else {
                    zSubtraction = numItemsAbove * mZDistanceBetweenElements;
                }
                setFloat(fieldZTranslation, childViewState, mZBasicHeight - zSubtraction);
            } else if (mIsHeadsUp
                    && yTranslation < (float)invoke(methodGetTopPadding, ambientState)
                    + (float)invoke(methodGetStackTranslation, ambientState)) {
                if (childrenOnTop != 0.0f) {
                    childrenOnTop++;
                } else {
                    float overlap = (float)invoke(methodGetTopPadding, ambientState)
                            + (float)invoke(methodGetStackTranslation, ambientState) - getFloat(fieldYTranslation, childViewState);
                    childrenOnTop += Math.min(1.0f, overlap / (getInt(fieldHeight, childViewState)));
                }
                setFloat(fieldZTranslation, childViewState, mZBasicHeight
                        + childrenOnTop * mZDistanceBetweenElements);
            } else {
                setFloat(fieldZTranslation, childViewState, mZBasicHeight);
            }
        }
    }

    private static void initAlgorithmState(Object resultState, Object state,
                                           Object ambientState) {
        ArrayList<?> visibleChildren = (ArrayList<?>)get(fieldVisibleChildren, state);

        setFloat(fieldItemsInBottomStack, state, 0.0f);
        setFloat(fieldPartialInBottom, state, 0.0f);
        float bottomOverScroll = (float)invoke(methodGetOverScrollAmount, ambientState, false /* onTop */);

        int scrollY = (int)invoke(methodGetScrollY, ambientState);

        // Due to the overScroller, the stackscroller can have negative scroll state. This is
        // already accounted for by the top padding and doesn't need an additional adaption
        scrollY = Math.max(0, scrollY);
        setInt(fieldScrollY, state, (int) (scrollY + bottomOverScroll));

        //now init the visible children and update paddings

        ViewGroup hostView = (ViewGroup)invoke(methodGetHostView, resultState);
        int childCount = hostView.getChildCount();
        visibleChildren.clear();
        visibleChildren.ensureCapacity(childCount);
        int notGoneIndex = 0;
        for (int i = 0; i < childCount; i++) {
            View v = hostView.getChildAt(i);
            if (v.getVisibility() != View.GONE) {
                notGoneIndex = (int)invoke(methodUpdateNotGoneIndex, mStackScrollAlgorithm, resultState, state, notGoneIndex, v);
            }
            if (classExpandableNotificationRow.getClass().isInstance(v)){
                View row =  v;

                // handle the notgoneIndex for the children as well
                List<ViewGroup> children =
                        (List<ViewGroup>)invoke(methodGetNotificationChildren, row);
                if ((boolean)invoke(methodAreChildrenExpanded, row) && children != null) {
                    for (ViewGroup childRow : children) {
                        if (childRow.getVisibility() != View.GONE) {
                            Object childState
                                    = invoke(methodGetViewStateForView, resultState, childRow);
                            XposedHelpers.setIntField(childState, "notGoneIndex", notGoneIndex);
                            notGoneIndex++;
                        }
                    }
                }
            }
        }
    }

    private static void updateStateForChildTransitioningInBottom(Object algorithmState,
                                                                 float transitioningPositionStart, Object child, float currentYPosition,
                                                                 Object childViewState, int childHeight) {

        // This is the transitioning element on top of bottom stack, calculate how far we are in.
        setFloat(fieldPartialInBottom, algorithmState, 1.0f - (
                (transitioningPositionStart - currentYPosition) / (childHeight +
                        getInt(fieldPaddingBetweenElements, mStackScrollAlgorithm))));
        // the offset starting at the transitionPosition of the bottom stack
        float offset = mBottomStackIndentationFunctor.getValue(getFloat(fieldPartialInBottom, algorithmState));
        setFloat(fieldItemsInBottomStack, algorithmState, getFloat(fieldItemsInBottomStack, algorithmState) + getFloat(fieldPartialInBottom, algorithmState));
        int newHeight = childHeight;
        if (childHeight > (int)invoke(methodGetHeight, child)) {

            newHeight = (int) Math.max(Math.min(transitioningPositionStart + offset -
                            getInt(fieldPaddingBetweenElements, mStackScrollAlgorithm) - currentYPosition, childHeight),
                    (int)invoke(methodGetHeight, child));
            setInt(fieldHeight, childViewState, newHeight);
        }
        setFloat(fieldYTranslation, childViewState, transitioningPositionStart + offset - newHeight
                - getInt(fieldPaddingBetweenElements, mStackScrollAlgorithm));
        setInt(fieldLocation, childViewState, LOCATION_MAIN_AREA);
    }

    private static void updateStateForChildFullyInBottomStack(Object algorithmState,
                                                              float transitioningPositionStart, Object childViewState,
                                                              int collapsedHeight, Object ambientState) {
        int mPaddingBetweenElements = getInt(fieldPaddingBetweenElements, mStackScrollAlgorithm);
        float currentYPosition;
        setFloat(fieldItemsInBottomStack, algorithmState, getFloat(fieldItemsInBottomStack, algorithmState)+ 1.0f);
        float mItemsInBottomStack = getFloat(fieldItemsInBottomStack, algorithmState);
        if (mItemsInBottomStack < MAX_ITEMS_IN_BOTTOM_STACK) {
            // We are visually entering the bottom stack
            currentYPosition = transitioningPositionStart
                    + mBottomStackIndentationFunctor.getValue(mItemsInBottomStack)
                    - mPaddingBetweenElements;

            setInt(fieldLocation, childViewState, LOCATION_BOTTOM_STACK_PEEKING);
        } else {
            // we are fully inside the stack
            if (mItemsInBottomStack > MAX_ITEMS_IN_BOTTOM_STACK + 2) {
                //XposedHelpers.setBooleanField(childViewState, "hidden", true);
                //XposedHelpers.setFloatField(childViewState, "shadowAlpha", 0.0f);
                setFloat(fieldAlpha, childViewState, 0.0f);

            } else if (mItemsInBottomStack
                    > MAX_ITEMS_IN_BOTTOM_STACK + 1) {
                //XposedHelpers.setFloatField(childViewState, "shadowAlpha", 1.0f - XposedHelpers.getFloatField(algorithmState, "partialInBottom"));
                setFloat(fieldAlpha, childViewState, 1.0f - getFloat(fieldPartialInBottom, algorithmState));
            }
            setInt(fieldLocation, childViewState, LOCATION_BOTTOM_STACK_HIDDEN);
            currentYPosition = (int)invoke(methodGetInnerHeight, ambientState);
        }
        setInt(fieldHeight, childViewState, collapsedHeight);
        setFloat(fieldYTranslation, childViewState, currentYPosition - collapsedHeight);
    }

    private static void clampPositionToBottomStackStart(Object childViewState,
                                                        int childHeight, int minHeight, Object ambientState) {

        int mBottomStackPeekSize = getInt(fieldBottomStackPeekSize, mStackScrollAlgorithm);
        int mBottomStackSlowDownLength = getInt(fieldBottomStackSlowDownLength, mStackScrollAlgorithm);
        int bottomStackStart = (int)invoke(methodGetInnerHeight, ambientState)
                - mBottomStackPeekSize - mBottomStackSlowDownLength;
        int childStart = bottomStackStart - childHeight;
        if (childStart < getFloat(fieldYTranslation, childViewState)) {
            float newHeight = bottomStackStart - getFloat(fieldYTranslation, childViewState);
            if (newHeight < minHeight) {
                newHeight = minHeight;
                setFloat(fieldYTranslation, childViewState, bottomStackStart - minHeight);
            }
            setInt(fieldHeight, childViewState, (int) newHeight);
        }
    }

    private static void updateFirstChildHeight(Object child, Object childViewState, int childHeight, Object ambientState) {

        int mBottomStackPeekSize = getInt(fieldBottomStackPeekSize, mStackScrollAlgorithm);
        int mBottomStackSlowDownLength = getInt(fieldBottomStackSlowDownLength, mStackScrollAlgorithm);

        // The starting position of the bottom stack peek
        int bottomPeekStart = (int)invoke(methodGetInnerHeight, ambientState) - mBottomStackPeekSize -
                mBottomStackSlowDownLength + (int) invoke(methodGetScrollY, ambientState);

        // Collapse and expand the first child while the shade is being expanded

        setInt(fieldHeight, childViewState, (int) Math.max(Math.min(bottomPeekStart, (float) childHeight),
                (int)invoke(methodGetMinHeight, child)));
    }

    private static void updatePositionsForState(Object resultState, Object algorithmState, Object ambientState) {

        List<?> visibleChildren = (List<?>) get(fieldVisibleChildren, algorithmState);

        if (mStackScrollAlgorithm == null) return;

        int mBottomStackPeekSize = getInt(fieldBottomStackPeekSize, mStackScrollAlgorithm);
        int mBottomStackSlowDownLength = getInt(fieldBottomStackSlowDownLength, mStackScrollAlgorithm);

        // The starting position of the bottom stack peek
        float bottomPeekStart = (int)invoke(methodGetInnerHeight, ambientState) - mBottomStackPeekSize;

        // The position where the bottom stack starts.
        float bottomStackStart = bottomPeekStart - mBottomStackSlowDownLength;

        // The y coordinate of the current child.
        float currentYPosition = -(getFloat(fieldScrollY, algorithmState));

        int childCount = visibleChildren.size();
        int paddingAfterChild;
        for (int i = 0; i < childCount; i++) {
            Object child = visibleChildren.get(i);
            Object childViewState = invoke(methodGetViewStateForView, resultState, child);
            setInt(fieldLocation, childViewState, LOCATION_UNKNOWN);
            paddingAfterChild = getInt(fieldPaddingBetweenElements, mStackScrollAlgorithm);
            int childHeight = getMaxAllowedChildHeight((View) child);
            int collapsedHeight = (int) invoke(methodGetMinHeight, child);
            setFloat(fieldYTranslation, childViewState, currentYPosition);
            if (i == 0) {
                updateFirstChildHeight(child, childViewState, childHeight, ambientState);
            }
            // The y position after this element
            float nextYPosition = currentYPosition + childHeight +
                    paddingAfterChild;
            if (nextYPosition >= bottomStackStart) {
                // Case 1:
                // We are in the bottom stack.
                if (currentYPosition >= bottomStackStart) {
                    // According to the regular scroll view we are fully translated out of the
                    // bottom of the screen so we are fully in the bottom stack
                    updateStateForChildFullyInBottomStack(algorithmState,
                            bottomStackStart, childViewState, collapsedHeight, ambientState);
                } else {
                    // According to the regular scroll view we are currently translating out of /
                    // into the bottom of the screen
                    updateStateForChildTransitioningInBottom(algorithmState,
                            bottomStackStart, child, currentYPosition,
                            childViewState, childHeight);
                }
            } else {
                // Case 2:
                // We are in the regular scroll area.
                setInt(fieldLocation, childViewState, LOCATION_MAIN_AREA);
                clampPositionToBottomStackStart(childViewState, (getInt(fieldHeight, childViewState)), childHeight,
                        ambientState);
            }
            if (i == 0 && (int) invoke(methodGetScrollY, ambientState) <= 0) {
                // The first card can get into the bottom stack if it's the only one
                // on the lockscreen which pushes it up. Let's make sure that doesn't happen and
                // it stays at the top
                setFloat(fieldYTranslation, childViewState, Math.max(0, getFloat(fieldYTranslation, childViewState)));
            }
            currentYPosition = getFloat(fieldYTranslation, childViewState) + childHeight + paddingAfterChild;
            if (currentYPosition <= 0) {
                setInt(fieldLocation, childViewState, LOCATION_TOP_STACK_HIDDEN);
            }
            if (getInt(fieldLocation, childViewState) == LOCATION_UNKNOWN) {
                XposedHook.logW(TAG, "Failed to assign location for child " + i);
            }
            float yTranslation = getFloat(fieldYTranslation, childViewState);
            setFloat(fieldYTranslation, childViewState, yTranslation + (float) invoke(methodGetTopPadding, ambientState)
                    + (float) invoke(methodGetStackTranslation, ambientState));
        }
    }

    private static void updateHeadsUpStates(Object resultState,
                                            Object algorithmState, Object ambientState) {
        ArrayList<ViewGroup> visibleChildren = (ArrayList<ViewGroup>) get(fieldVisibleChildren, algorithmState);
        boolean mIsExpanded = getBoolean(fieldIsExpanded, mStackScrollAlgorithm);
        int childCount = visibleChildren.size();
        Object topHeadsUpEntry = null;
        for (int i = 0; i < childCount; i++) {
            View child = visibleChildren.get(i);
            if (!(classExpandableNotificationRow.isInstance(child))) {
                break;
            }
            Object row = child;
            boolean isHeadsUp = classExpandableNotificationRow.isInstance(row) && getBoolean(fieldIsHeadsUp, row);
            if (!isHeadsUp) {
                break;
            }
            Object childState = invoke(methodGetViewStateForView, resultState, row);
            if (topHeadsUpEntry == null) {
                topHeadsUpEntry = row;
                setInt(fieldLocation, childState, LOCATION_FIRST_CARD);
            }
            boolean isTopEntry = topHeadsUpEntry == row;
            float unmodifiedEndLocation = getFloat(fieldYTranslation, childState) + getInt(fieldHeight, childState);
            if (mIsExpanded) {
                // Ensure that the heads up is always visible even when scrolled off
                clampHunToTop(ambientState, row, childState);
                clampHunToMaxTranslation(ambientState, row, childState);
            }
            boolean isPinned = classExpandableView.isInstance(row) && (boolean)invoke(methodIsPinned, row);
            if (isPinned) {
                setFloat(fieldYTranslation, childState, Math.max(getFloat(fieldYTranslation, childState), 0));
                int height = getInt(fieldHeight, childState);
                setInt(fieldHeight, childState, Math.max((int)invoke(methodGetIntrinsicHeightRow, row), height));
                Object topState = invoke(methodGetViewStateForView, resultState, topHeadsUpEntry);
                if (!isTopEntry && (!mIsExpanded
                        || unmodifiedEndLocation < getFloat(fieldYTranslation, topState) + getInt(fieldHeight, topState))) {
                    // Ensure that a headsUp doesn't vertically extend further than the heads-up at
                    // the top most z-position
                    setInt(fieldHeight, childState, (int)invoke(methodGetIntrinsicHeightRow, row));
                    setFloat(fieldYTranslation, childState, getFloat(fieldYTranslation, topState) + getInt(fieldHeight, topState)
                            - getInt(fieldHeight, childState));
                }
            }
        }
    }

    private static int getMaxAllowedChildHeight(View child) {
        if (classExpandableView.isInstance(child)) {
            View expandableView = child;
            return (int)invoke(methodGetIntrinsicHeight, expandableView);
        }
        return child == null? getInt(fieldCollapsedSize, mStackScrollAlgorithm) : child.getHeight();
    }

    private static void clampHunToTop(Object ambientState, Object row,
                                      Object childState) {
        float newTranslation = Math.max(((float)invoke(methodGetTopPadding, ambientState)
                + (float)invoke(methodGetStackTranslation, ambientState)), getFloat(fieldYTranslation, childState));
        setInt(fieldHeight, childState, (int) Math.max(getInt(fieldHeight, childState) - (newTranslation
                - getFloat(fieldYTranslation, childState)), (int)invoke(methodGetIntrinsicHeightRow, row)));//should be getMinHeight but not yet implemented properly
        setFloat(fieldYTranslation, childState, newTranslation);
    }

    private static void clampHunToMaxTranslation(Object ambientState, Object row,
                                                 Object childState) {
        float newTranslation;
        float bottomPosition = (float)invoke(methodGetMaxHeadsUpTranslation, ambientState) - (int)invoke(methodGetMinHeight, row);
        newTranslation = Math.min(getFloat(fieldYTranslation, childState), bottomPosition);
        setInt(fieldHeight, childState, (int) Math.max(getInt(fieldHeight, childState)
                - (getFloat(fieldYTranslation, childState) - newTranslation), (int)invoke(methodGetIntrinsicHeightRow, row)));//same as above
        setFloat(fieldYTranslation, childState, newTranslation);
    }

    private static Object get(Field field, Object object) {
        try {
            return field.get(object);
        } catch (Throwable t) {
            return null;
        }
    }

    private static int getInt(Field field, Object object) {
        try {
            return field.getInt(object);
        } catch (Throwable t) {
            return 0;
        }
    }

    private static float getFloat(Field field, Object object) {
        try {
            return field.getFloat(object);
        } catch (Throwable t) {
            return 0;
        }
    }

    private static void setInt(Field field, Object object, int value) {
        try {
            field.setInt(object, value);
        } catch (Throwable ignore) {

        }
    }

    private static void setFloat(Field field, Object object, float value) {
        try {
            field.setFloat(object, value);
        } catch (Throwable ignore) {

        }
    }

    private static void setBoolean(Field field, Object object, boolean value) {
        try {
            field.setBoolean(object, value);
        } catch (Throwable ignore) {

        }
    }

    private static boolean getBoolean(Field field, Object object) {
        try {
            return field.getBoolean(object);
        } catch (Throwable t) {
            return false;
        }
    }

    private static Object invoke(Method method, Object object, Object... args) {
        try {
            return method.invoke(object, args);
        } catch (Throwable t) {
            return null;
        }
    }
}
