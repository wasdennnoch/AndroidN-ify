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
import java.util.HashMap;
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

import static tk.wasdennnoch.androidn_ify.systemui.notifications.NotificationPanelHooks.isOnKeyguard;
import static tk.wasdennnoch.androidn_ify.systemui.notifications.NotificationPanelViewHooks.getLayoutMinHeight;

public class StackScrollAlgorithmHooks {

    private static final String TAG = "StackScrollAlgorithmHooks";

    public static final int LOCATION_UNKNOWN = 0x00;
    public static final int LOCATION_FIRST_CARD = 0x01;
    public static final int LOCATION_TOP_STACK_HIDDEN = 0x02;
    public static final int LOCATION_MAIN_AREA = 0x08;

    private static final int MAX_ITEMS_IN_BOTTOM_STACK = 3;

    private static final Rect mClipBounds = new Rect();
    public static ViewGroup mStackScrollLayout;
    public static Object mStackScrollAlgorithm;
    private static float mStackTop = 0;
    private static float mStateTop = 0;

    private static boolean mQsExpanded;

    private static int mLayoutMinHeight = 0;

    private static Field fieldBottomStackIndentationFunctor;

    private static Field fieldQsExpanded;

    private static Field fieldVisibleChildren;
    private static Field fieldScrollY;
    private static Field fieldShadeExpanded;
    private static Field fieldTopPadding;
    private static Field fieldYTranslation;
    private static Field fieldLocation;
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
    private static Field fieldBottomStackPeekSize;
    private static Field fieldIsExpanded;
    private static Field fieldLayoutHeight;
    private static Field fieldCollapsedSize;
    private static Field fieldHostView;
    private static Field fieldStateMap;
    private static Field fieldClearAllTopPadding;

    private static Method methodGetViewStateForView;
    private static Method methodGetInnerHeight;
    private static Method methodGetScrollY;

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
    private static Method methodGetHeight;
    private static Method methodGetIntrinsicHeight;
    private static Method methodGetHeadsUpHeight;
    private static Method methodGetMaxHeadsUpTranslation;
    private static Method methodUpdateStateForChildTransitioningInBottom;
    private static Method methodUpdateStateForChildFullyInBottomStack;
    private static Method methodApplyState;
    private static Method methodPerformVisibilityAnimationDismissView;
    private static Method methodWillBeGoneDismissView;
    private static Method methodPerformVisibilityAnimationEmptyShade;
    private static Method methodWillBeGoneEmptyShade;
    private static Method methodUpdateVisibleChildren;


    private static Class<?> classNotificationStackScrollLayout;
    private static Class<?> classStackScrollAlgorithm;
    private static Class<?> classStackScrollAlgorithmState;
    private static Class<?> classStackScrollState;
    private static Class<?> classStackViewState;
    private static Class<?> classAmbientState;
    private static Class<?> classExpandableNotificationRow;
    private static Class<?> classExpandableView;
    private static Class<?> classDismissView;
    private static Class<?> classEmptyShadeView;
    private static Class<?> classNotificationPanelView;

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
                classDismissView = XposedHelpers.findClass("com.android.systemui.statusbar.DismissView", classLoader);
                classEmptyShadeView = XposedHelpers.findClass("com.android.systemui.statusbar.EmptyShadeView", classLoader);
                classNotificationPanelView = XposedHelpers.findClass("com.android.systemui.statusbar.phone.NotificationPanelView", classLoader);

                fieldQsExpanded = XposedHelpers.findField(classNotificationPanelView, "mQsExpanded");

                fieldCollapsedSize = XposedHelpers.findField(classStackScrollAlgorithm, "mCollapsedSize");
                fieldVisibleChildren = XposedHelpers.findField(classStackScrollAlgorithmState, "visibleChildren");
                fieldScrollY = XposedHelpers.findField(classStackScrollAlgorithmState, "scrollY");
                fieldShadeExpanded = XposedHelpers.findField(classAmbientState, "mShadeExpanded");
                fieldTopPadding = XposedHelpers.findField(classAmbientState, "mTopPadding");
                fieldYTranslation = XposedHelpers.findField(classStackViewState, "yTranslation");
                fieldLocation = XposedHelpers.findField(classStackViewState, "location");
                fieldHeight = XposedHelpers.findField(classStackViewState, "height");
                fieldBottomStackSlowDownLength = XposedHelpers.findField(classStackScrollAlgorithm, "mBottomStackSlowDownLength");
                fieldPaddingBetweenElements = XposedHelpers.findField(classStackScrollAlgorithm, "mPaddingBetweenElements");
                fieldBottomStackIndentationFunctor = XposedHelpers.findField(classStackScrollAlgorithm, "mBottomStackIndentationFunctor");

                fieldTempAlgorithmState = XposedHelpers.findField(classStackScrollAlgorithm, "mTempAlgorithmState");
                fieldZDistanceBetweenElements = XposedHelpers.findField(classStackScrollAlgorithm, "mZDistanceBetweenElements");
                fieldZBasicHeight = XposedHelpers.findField(classStackScrollAlgorithm, "mZBasicHeight");
                fieldZTranslation = XposedHelpers.findField(classStackViewState, "zTranslation");
                fieldIsHeadsUp = XposedHelpers.findField(classExpandableNotificationRow, "mIsHeadsUp");
                fieldClipTopAmount = XposedHelpers.findField(classStackViewState, "clipTopAmount");
                fieldItemsInBottomStack = XposedHelpers.findField(classStackScrollAlgorithmState, "itemsInBottomStack");
                fieldPartialInBottom = XposedHelpers.findField(classStackScrollAlgorithmState, "partialInBottom");
                fieldBottomStackPeekSize = XposedHelpers.findField(classStackScrollAlgorithm, "mBottomStackPeekSize");
                fieldIsExpanded = XposedHelpers.findField(classStackScrollAlgorithm, "mIsExpanded");
                fieldLayoutHeight = XposedHelpers.findField(classAmbientState, "mLayoutHeight");
                fieldHostView = XposedHelpers.findField(classStackScrollState, "mHostView");
                fieldStateMap = XposedHelpers.findField(classStackScrollState, "mStateMap");
                fieldClearAllTopPadding = XposedHelpers.findField(classStackScrollState, "mClearAllTopPadding");

                methodGetViewStateForView = XposedHelpers.findMethodBestMatch(classStackScrollState, "getViewStateForView", View.class);
                methodGetInnerHeight = XposedHelpers.findMethodBestMatch(classAmbientState, "getInnerHeight");
                methodGetScrollY = XposedHelpers.findMethodBestMatch(classAmbientState, "getScrollY");

                methodHandleDraggedViews = XposedHelpers.findMethodBestMatch(classStackScrollAlgorithm, "handleDraggedViews", classAmbientState,classStackScrollState, classStackScrollAlgorithmState);
                methodUpdateDimmedActivatedHideSensitive = XposedHelpers.findMethodBestMatch(classStackScrollAlgorithm, "updateDimmedActivatedHideSensitive", classAmbientState,classStackScrollState, classStackScrollAlgorithmState);
                methodUpdateSpeedBumpState = XposedHelpers.findMethodBestMatch(classStackScrollAlgorithm, "updateSpeedBumpState", classStackScrollState, classStackScrollAlgorithmState, int.class);
                methodGetNotificationChildrenStates = XposedHelpers.findMethodBestMatch(classStackScrollAlgorithm, "getNotificationChildrenStates", classStackScrollState, classStackScrollAlgorithmState);
                methodGetBottomStackSlowDownLength = XposedHelpers.findMethodBestMatch(classStackScrollAlgorithm, "getBottomStackSlowDownLength");
                methodUpdateVisibleChildren = XposedHelpers.findMethodBestMatch(classStackScrollAlgorithm, "updateVisibleChildren", classStackScrollState, classStackScrollAlgorithmState);

                methodGetSpeedBumpIndex = XposedHelpers.findMethodBestMatch(classAmbientState, "getSpeedBumpIndex");
                methodGetTopPadding = XposedHelpers.findMethodBestMatch(classAmbientState, "getTopPadding");
                methodGetStackTranslation = XposedHelpers.findMethodBestMatch(classAmbientState, "getStackTranslation");
                methodGetOverScrollAmount = XposedHelpers.findMethodBestMatch(classAmbientState, "getOverScrollAmount", boolean.class);
                methodGetMaxHeadsUpTranslation = XposedHelpers.findMethodBestMatch(classAmbientState, "getMaxHeadsUpTranslation");

                methodIsTransparent = XposedHelpers.findMethodBestMatch(classExpandableView, "isTransparent");
                methodGetHeight = XposedHelpers.findMethodBestMatch(classExpandableView, "getHeight");
                methodGetIntrinsicHeight = XposedHelpers.findMethodBestMatch(classExpandableView, "getIntrinsicHeight");

                methodIsPinned = XposedHelpers.findMethodBestMatch(classExpandableNotificationRow, "isPinned");
                methodGetHeadsUpHeight = XposedHelpers.findMethodBestMatch(classExpandableNotificationRow, "getHeadsUpHeight");

                methodResetViewStates = XposedHelpers.findMethodBestMatch(classStackScrollState, "resetViewStates");

                methodUpdateStateForChildTransitioningInBottom = XposedHelpers.findMethodBestMatch(classStackScrollAlgorithm, "updateStateForChildTransitioningInBottom",
                        classStackScrollAlgorithmState, float.class, float.class, float.class, classStackViewState, int.class);

                methodUpdateStateForChildFullyInBottomStack = XposedHelpers.findMethodBestMatch(classStackScrollAlgorithm, "updateStateForChildFullyInBottomStack",
                        classStackScrollAlgorithmState, float.class, classStackViewState, int.class, classAmbientState);

                methodApplyState = XposedHelpers.findMethodBestMatch(classStackScrollState, "applyState", classExpandableView, classStackViewState);
                methodPerformVisibilityAnimationDismissView = XposedHelpers.findMethodBestMatch(classDismissView, "performVisibilityAnimation", boolean.class);
                methodWillBeGoneDismissView = XposedHelpers.findMethodBestMatch(classDismissView, "willBeGone");
                methodPerformVisibilityAnimationEmptyShade = XposedHelpers.findMethodBestMatch(classEmptyShadeView, "performVisibilityAnimation", boolean.class);
                methodWillBeGoneEmptyShade = XposedHelpers.findMethodBestMatch(classEmptyShadeView, "willBeGone");

                XposedHelpers.findAndHookMethod(classStackScrollState, "apply", apply);

                XposedHelpers.findAndHookMethod(classAmbientState, "getInnerHeight", getInnerHeight);

                XposedHelpers.findAndHookMethod(classNotificationStackScrollLayout, "updateAlgorithmHeightAndPadding", updateAlgorithmHeightAndPaddingHook);
                XposedHelpers.findAndHookMethod(classNotificationStackScrollLayout, "onLayout", boolean.class, int.class, int.class, int.class, int.class, onLayoutHook);
                XposedHelpers.findAndHookMethod(classNotificationPanelView, "updateQsState", updateQsStateHook);

                XposedBridge.hookAllMethods(classStackScrollAlgorithm, "initConstants", initConstantsHook);
                XposedBridge.hookAllMethods(classStackScrollAlgorithm, "getStackScrollState", getStackScrollState);
                XposedBridge.hookAllMethods(classStackScrollAlgorithm, "clampPositionToTopStackEnd", XC_MethodReplacement.DO_NOTHING);
                XposedBridge.hookAllMethods(classStackScrollAlgorithm, "updateFirstChildMaxSizeToMaxHeight", XC_MethodReplacement.DO_NOTHING);
                XposedBridge.hookAllMethods(classStackScrollAlgorithm, "onExpansionStarted", XC_MethodReplacement.DO_NOTHING);
                XposedBridge.hookAllMethods(classStackScrollAlgorithm, "updateFirstChildHeightWhileExpanding", XC_MethodReplacement.DO_NOTHING);
                XposedBridge.hookAllMethods(classStackScrollAlgorithm, "updateIsSmallScreen", XC_MethodReplacement.DO_NOTHING);
                XposedBridge.hookAllMethods(classStackScrollAlgorithm, "onExpansionStopped", XC_MethodReplacement.DO_NOTHING);
                XposedBridge.hookAllMethods(classStackScrollAlgorithm, "notifyChildrenChanged", XC_MethodReplacement.DO_NOTHING);
                XposedBridge.hookAllMethods(classStackScrollAlgorithm, "updateStateForChildFullyInBottomStack", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        int collapsedHeight = (int)param.args[3];
                        Object childViewState = param.args[2];
                        setInt(fieldHeight, childViewState, collapsedHeight);
                    }
                });
                XposedBridge.hookAllMethods(classStackScrollAlgorithm, "updateStateForChildTransitioningInBottom", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        XposedHelpers.setBooleanField(param.thisObject, "mIsSmallScreen", true);
                    }
                });
            }

        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error hooking SystemUI", t);
        }
    }

    public static void setLayoutMinHeight(int layoutMinHeight) {
        mLayoutMinHeight = layoutMinHeight;
    }

    private static void updateAlgorithmLayoutMinHeight() throws IllegalAccessException {
        setLayoutMinHeight(mQsExpanded && !isOnKeyguard() ? getLayoutMinHeight() : 0);
    }

    public static void setQsExpanded(boolean qsExpanded) throws IllegalAccessException {
        mQsExpanded = qsExpanded;
        updateAlgorithmLayoutMinHeight();
    }

    private static final XC_MethodHook updateQsStateHook = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            setQsExpanded(getBoolean(fieldQsExpanded, param.thisObject));
        }
    };

    private static final XC_MethodReplacement getInnerHeight = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            Object mAmbientState = param.thisObject;
            int mLayoutHeight = getInt(fieldLayoutHeight, mAmbientState);
            int mTopPadding = getInt(fieldTopPadding, mAmbientState);
            return Math.max(mLayoutHeight - mTopPadding, mLayoutMinHeight);
        }
    };

    private static final XC_MethodHook updateAlgorithmHeightAndPaddingHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            updateAlgorithmLayoutMinHeight();
        }
    };

    private static final XC_MethodHook onLayoutHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            updateAlgorithmLayoutMinHeight();
        }
    };

    private static final XC_MethodHook initConstantsHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            int mBottomStackPeekSize = getInt(fieldTempAlgorithmState, mStackScrollLayout);
            setInt(fieldZDistanceBetweenElements, param.thisObject, Math.max(1, ResourceUtils.getInstance().getResources()
                    .getDimensionPixelSize(R.dimen.z_distance_between_notifications)));
            fieldBottomStackIndentationFunctor.set(new PiecewiseLinearIndentationFunctor(
                    MAX_ITEMS_IN_BOTTOM_STACK,
                    mBottomStackPeekSize,
                    (int)invoke(methodGetBottomStackSlowDownLength, param.thisObject),
                    0.5f), param.thisObject);
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

            initAlgorithmState(algorithmState, ambientState);
            invoke(methodUpdateVisibleChildren, mStackScrollAlgorithm, resultState, algorithmState);
            updatePositionsForState(resultState, algorithmState, ambientState);
            updateZValuesForState(resultState, algorithmState, ambientState);
            updateHeadsUpStates(resultState, algorithmState, ambientState);
            invoke(methodHandleDraggedViews, mStackScrollAlgorithm, ambientState, resultState, algorithmState);
            invoke(methodUpdateDimmedActivatedHideSensitive, mStackScrollAlgorithm, ambientState, resultState, algorithmState);
            updateClipping(resultState, algorithmState, ambientState);
            invoke(methodUpdateSpeedBumpState, mStackScrollAlgorithm, resultState, algorithmState, invoke(methodGetSpeedBumpIndex, ambientState));
            invoke(methodGetNotificationChildrenStates, mStackScrollAlgorithm, resultState, algorithmState);
            return null;
        }
    };

    private static final XC_MethodReplacement apply = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            Object stackScrollState = param.thisObject;
            ViewGroup mHostView = (ViewGroup)get(fieldHostView, stackScrollState);
            HashMap<?,?> mStateMap = (HashMap)get(fieldStateMap, stackScrollState);
            int numChildren = mHostView.getChildCount();
            for (int i = 0; i < numChildren; i++) {
                Object child = mHostView.getChildAt(i);
                Object state = mStateMap.get(child);
                if (!(boolean)invoke(methodApplyState, stackScrollState, child, state)) {
                    continue;
                }
                if (classDismissView.isInstance(child)) {
                    boolean willBeGone = (boolean)invoke(methodWillBeGoneDismissView, child);
                    boolean visible = getInt(fieldClipTopAmount, state) < getInt(fieldClearAllTopPadding, stackScrollState);
                    invoke(methodPerformVisibilityAnimationDismissView, child, visible && !willBeGone);
                } else if (classEmptyShadeView.isInstance(child)) {
                    boolean willBeGone = (boolean)invoke(methodWillBeGoneEmptyShade, child);
                    boolean visible = getInt(fieldClipTopAmount, state) <= 0;
                    invoke(methodPerformVisibilityAnimationEmptyShade, child,
                            visible && !willBeGone);
                }
            }
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
            boolean isTransparent = (boolean)invoke(methodIsTransparent, child);
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

    private static void initAlgorithmState(Object state,
                                           Object ambientState) {

        setFloat(fieldItemsInBottomStack, state, 0.0f);
        setFloat(fieldPartialInBottom, state, 0.0f);
        float bottomOverScroll = (float)invoke(methodGetOverScrollAmount, ambientState, false /* onTop */);

        int scrollY = (int)invoke(methodGetScrollY, ambientState);

        // Due to the overScroller, the stackscroller can have negative scroll state. This is
        // already accounted for by the top padding and doesn't need an additional adaption
        scrollY = Math.max(0, scrollY);
        setInt(fieldScrollY, state, (int) (scrollY + bottomOverScroll));
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

    private static void updateFirstChildHeight(Object childViewState, int childHeight, Object ambientState) {

        int mBottomStackPeekSize = getInt(fieldBottomStackPeekSize, mStackScrollAlgorithm);
        int mBottomStackSlowDownLength = getInt(fieldBottomStackSlowDownLength, mStackScrollAlgorithm);

        // The starting position of the bottom stack peek
        int bottomPeekStart = (int)invoke(methodGetInnerHeight, ambientState) - mBottomStackPeekSize -
                mBottomStackSlowDownLength + (int) invoke(methodGetScrollY, ambientState);

        // Collapse and expand the first child while the shade is being expanded

        setInt(fieldHeight, childViewState, (int) Math.max(Math.min(bottomPeekStart, (float) childHeight),
                getInt(fieldCollapsedSize, mStackScrollAlgorithm)));
    }

    private static void updatePositionsForState(Object resultState, Object algorithmState, Object ambientState) {

        int collapsedHeight = getInt(fieldCollapsedSize, mStackScrollAlgorithm);

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
        int paddingAfterChild = getInt(fieldPaddingBetweenElements, mStackScrollAlgorithm);

        for (int i = 0; i < childCount; i++) {
            Object child = visibleChildren.get(i);
            Object childViewState = invoke(methodGetViewStateForView, resultState, child);
            setInt(fieldLocation, childViewState, LOCATION_UNKNOWN);
            int childHeight = getMaxAllowedChildHeight((View) child);
            setFloat(fieldYTranslation, childViewState, currentYPosition);
            if (i == 0) {
                updateFirstChildHeight(childViewState, childHeight, ambientState);
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
                    invoke(methodUpdateStateForChildFullyInBottomStack, mStackScrollAlgorithm, algorithmState,
                            bottomStackStart, childViewState, collapsedHeight, ambientState);
                } else {
                    // According to the regular scroll view we are currently translating out of /
                    // into the bottom of the screen
                    invoke(methodUpdateStateForChildTransitioningInBottom, mStackScrollAlgorithm, algorithmState,
                            bottomStackStart, bottomPeekStart, currentYPosition,
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
                clampHunToTop(ambientState, childState);
                clampHunToMaxTranslation(ambientState, row, childState);
            }
            boolean isPinned = classExpandableView.isInstance(row) && (boolean)invoke(methodIsPinned, row);
            if (isPinned) {
                setFloat(fieldYTranslation, childState, Math.max(getFloat(fieldYTranslation, childState), 0));
                int height = (int)invoke(methodGetHeadsUpHeight, row);
                setInt(fieldHeight, childState, height);
                Object topState = invoke(methodGetViewStateForView, resultState, topHeadsUpEntry);
                if (!isTopEntry && (!mIsExpanded
                        || unmodifiedEndLocation < getFloat(fieldYTranslation, topState) + getInt(fieldHeight, topState))) {
                    // Ensure that a headsUp doesn't vertically extend further than the heads-up at
                    // the top most z-position
                    setInt(fieldHeight, childState, (int)invoke(methodGetHeadsUpHeight, row));
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
        return child == null? getInt(fieldCollapsedSize, mStackScrollAlgorithm) : (int)invoke(methodGetHeight, child);
    }

    private static void clampHunToTop(Object ambientState,
                                      Object childState) {
        float newTranslation = Math.max(((float)invoke(methodGetTopPadding, ambientState)
                + (float)invoke(methodGetStackTranslation, ambientState)), getFloat(fieldYTranslation, childState));
        setInt(fieldHeight, childState, (int) Math.max(getInt(fieldHeight, childState) - (newTranslation
                - getFloat(fieldYTranslation, childState)), getInt(fieldCollapsedSize, mStackScrollAlgorithm)));
        setFloat(fieldYTranslation, childState, newTranslation);
    }

    private static void clampHunToMaxTranslation(Object ambientState, Object row,
                                                 Object childState) {
        float newTranslation;
        float bottomPosition = (float)invoke(methodGetMaxHeadsUpTranslation, ambientState) - getInt(fieldCollapsedSize, mStackScrollAlgorithm);
        newTranslation = Math.min(getFloat(fieldYTranslation, childState), bottomPosition);
        setInt(fieldHeight, childState, Math.max(getInt(fieldHeight, childState), (int)invoke(methodGetHeadsUpHeight, row)));
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
