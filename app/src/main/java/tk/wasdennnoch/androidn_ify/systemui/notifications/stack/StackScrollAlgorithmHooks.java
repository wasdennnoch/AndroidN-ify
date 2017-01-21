package tk.wasdennnoch.androidn_ify.systemui.notifications.stack;

import android.content.Context;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.XposedHook;
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

    private static final Rect mClipBounds = new Rect();
    public static ViewGroup mStackScrollLayout;
    private static float mStackTop = 0;
    private static float mStateTop = 0;

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

    private static Method methodGetViewStateForView;
    private static Method methodGetTopHeadsUpEntry;
    private static Method methodGetInnerHeight;
    private static Method methodGetScrollY;
    private static Method methodGetMinHeight;

    public static void hook(ClassLoader classLoader) {
        try {
            final ConfigUtils config = ConfigUtils.getInstance();

            Class classNotificationStackScrollLayout = XposedHelpers.findClass("com.android.systemui.statusbar.stack.NotificationStackScrollLayout", classLoader);
            XposedBridge.hookAllMethods(classNotificationStackScrollLayout, "initView", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    mStackScrollLayout = (ViewGroup) param.thisObject;
                }
            });

            Class classStackScrollAlgorithm = XposedHelpers.findClass("com.android.systemui.statusbar.stack.StackScrollAlgorithm", classLoader);
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
                /*Class classStackScrollAlgorithmState = XposedHelpers.findClass("com.android.systemui.statusbar.stack.StackScrollAlgorithm.StackScrollAlgorithmState", classLoader);
                Class classStackScrollState = XposedHelpers.findClass("com.android.systemui.statusbar.stack.StackScrollState", classLoader);
                Class classStackViewState = XposedHelpers.findClass("com.android.systemui.statusbar.stack.StackViewState", classLoader);
                Class classAmbientState = XposedHelpers.findClass("com.android.systemui.statusbar.stack.AmbientState", classLoader);
                Class classExpandableNotificationRow = XposedHelpers.findClass("com.android.systemui.statusbar.ExpandableNotificationRow", classLoader);
                Class classExpandableView = XposedHelpers.findClass("com.android.systemui.statusbar.ExpandableView", classLoader);

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

                methodGetViewStateForView = XposedHelpers.findMethodBestMatch(classStackScrollState, "getViewStateForView", View.class);
                methodGetTopHeadsUpEntry = XposedHelpers.findMethodBestMatch(classAmbientState, "getTopHeadsUpEntry");
                methodGetInnerHeight = XposedHelpers.findMethodBestMatch(classAmbientState, "getInnerHeight");
                methodGetScrollY = XposedHelpers.findMethodBestMatch(classAmbientState, "getScrollY");
                methodGetMinHeight = XposedHelpers.findMethodBestMatch(classExpandableView, "getMinHeight");

                XposedBridge.hookAllMethods(classStackScrollAlgorithm, "updateStateForTopStackChild", new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                        Object childViewState = param.args[4];
                        float scrollOffset = (float) param.args[5];
                        updateStateForTopStackChild(childViewState, scrollOffset);
                        return null;
                    }
                });

                XposedBridge.hookAllMethods(classStackScrollAlgorithm, "updateHeadsUpStates", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        Object resultState = param.args[0];
                        Object algorithmState = param.args[1];
                        Object ambientState = param.args[2];
                        int mCollapsedSize = fieldCollapsedSize.getInt(param.thisObject);
                        updateHeadsUpStates(algorithmState, resultState, ambientState, mCollapsedSize);
                    }
                });

                XposedBridge.hookAllMethods(classStackScrollAlgorithm, "findNumberOfItemsInTopStackAndUpdateState", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        isExpansionChanging = fieldIsExpansionChanging.getBoolean(param.thisObject);
                    }
                });*/

                //XposedBridge.hookAllMethods(classStackScrollAlgorithm, "clampPositionToTopStackEnd", XC_MethodReplacement.DO_NOTHING);
                //XposedBridge.hookAllMethods(classStackScrollAlgorithm, "findNumberOfItemsInTopStackAndUpdateState", XC_MethodReplacement.DO_NOTHING); //this causes problems
                /*XposedBridge.hookAllMethods(classStackScrollAlgorithm, "updatePositionsForState", new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {

                        return null;
                    }
                });*/

                XposedHelpers.findAndHookMethod(classNotificationStackScrollLayout, "onInterceptTouchEvent", MotionEvent.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        MotionEvent ev = (MotionEvent) param.args[0];
                        if (ev.getY() < mStateTop)
                            param.setResult(false);
                    }
                });
            }

        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error hooking SystemUI", t);
        }
    }

    /*private void updatePositionsForState(Object resultState,
                                         Object algorithmState, Object ambientState, float mBottomStackSlowDownLength, int mPaddingBetweenElements) {
        List<?> visibleChildren = (List<?>) get(fieldVisibleChildren, algorithmState);

        // The starting position of the bottom stack peek
        float bottomPeekStart = (float)invoke(methodGetInnerHeight, ambientState) - mBottomStackPeekSize;

        // The position where the bottom stack starts.
        float bottomStackStart = bottomPeekStart - mBottomStackSlowDownLength;

        // The y coordinate of the current child.
        float currentYPosition = -getFloat(fieldScrollY, algorithmState);

        int childCount = visibleChildren.size();
        int paddingAfterChild;
        for (int i = 0; i < childCount; i++) {
            Object child = visibleChildren.get(i);
            Object childViewState = invoke(methodGetViewStateForView,child);
            setInt(fieldLocation, childViewState, LOCATION_UNKNOWN);
            paddingAfterChild = mPaddingBetweenElements;
            int childHeight = getMaxAllowedChildHeight(child);
            int minHeight = child.getMinHeight();
            childViewState.yTranslation = currentYPosition;
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
                            bottomStackStart, childViewState, minHeight, ambientState, child);
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
                childViewState.location = StackViewState.LOCATION_MAIN_AREA;
                clampPositionToBottomStackStart(childViewState, childViewState.height, childHeight,
                        ambientState);
            }

            if (i == 0 && ambientState.getScrollY() <= 0) {
                // The first card can get into the bottom stack if it's the only one
                // on the lockscreen which pushes it up. Let's make sure that doesn't happen and
                // it stays at the top
                childViewState.yTranslation = Math.max(0, childViewState.yTranslation);
            }
            currentYPosition = childViewState.yTranslation + childHeight + paddingAfterChild;
            if (currentYPosition <= 0) {
                childViewState.location = StackViewState.LOCATION_HIDDEN_TOP;
            }
            if (childViewState.location == StackViewState.LOCATION_UNKNOWN) {
                Log.wtf(LOG_TAG, "Failed to assign location for child " + i);
            }

            childViewState.yTranslation += ambientState.getTopPadding()
                    + ambientState.getStackTranslation();
        }
    }*/

    private static void updateFirstChildHeight(Object child, Object childViewState, int childHeight, Object ambientState, boolean mIsExpansionChanging, Object mFirstChildWhileExpanding, int mFirstChildMaxHeight) {
        // The starting position of the bottom stack peek
        int bottomPeekStart = (int)invoke(methodGetInnerHeight, ambientState) - mBottomStackPeekSize -
                mCollapseSecondCardPadding + (int)invoke(methodGetScrollY, ambientState);
        // Collapse and expand the first child while the shade is being expanded
        float maxHeight = mIsExpansionChanging && child == mFirstChildWhileExpanding
                ? mFirstChildMaxHeight
                : childHeight;
        setInt(fieldHeight, childViewState, (int) Math.max(Math.min(bottomPeekStart, maxHeight),
                (int)invoke(methodGetMinHeight, child)));
    }

    private static void updateStateForTopStackChild(Object childViewState, float scrollOffset) {
        setFloat(fieldYTranslation, childViewState, scrollOffset);
        setInt(fieldLocation, childViewState, LOCATION_TOP_STACK_HIDDEN);
    }

    private static void updateHeadsUpStates(Object algorithmState, Object resultState, Object ambientState, int mCollapsedSize) {
        List<?> visibleChildren = (List<?>) get(fieldVisibleChildren, algorithmState);
        if (visibleChildren == null || visibleChildren.size() < 1) {
            mStateTop = 0;
            return;
        }
        Object child = visibleChildren.get(0);
        Object childViewState = invoke(methodGetViewStateForView, resultState, child);

        int scrollY = getInt(fieldScrollY, algorithmState);

        Object topHeadsUpEntry = invoke(methodGetTopHeadsUpEntry, ambientState);
        boolean isShadeExpanded = getBoolean(fieldShadeExpanded, ambientState);

        float yTranslation = mCollapsedSize - scrollY;

        if (isShadeExpanded && topHeadsUpEntry != null
                && child != topHeadsUpEntry) {
            yTranslation += getInt(fieldHeadsUpHeight, topHeadsUpEntry) - mCollapsedSize;
        }

        mStateTop = getInt(fieldTopPadding, ambientState)
                + getFloat(fieldStackTranslation, ambientState);
        yTranslation += mStateTop;

        setFloat(fieldYTranslation, childViewState, yTranslation);
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
