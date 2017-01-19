package tk.wasdennnoch.androidn_ify.systemui.qs;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.extracted.systemui.Interpolators;
import tk.wasdennnoch.androidn_ify.extracted.systemui.qs.QSDetail;
import tk.wasdennnoch.androidn_ify.systemui.notifications.StatusBarHeaderHooks;
import tk.wasdennnoch.androidn_ify.utils.ConfigUtils;
import tk.wasdennnoch.androidn_ify.utils.ResourceUtils;

@SuppressWarnings("ResourceType")
public class QSContainerHelper {

    private static final String TAG = "QSContainerHelper";

    private static ViewGroup mNotificationPanelView;
    private static ViewGroup mHeader;
    private static ViewGroup mQSContainer;
    private static ViewGroup mQSPanel;
    private static ViewGroup mNotificationStackScroller;
    private static QSDetail mQSDetail;
    private static float mQsExpansion;
    private static int mHeaderHeight;
    private static int mQsTopMargin;

    private static final int CAP_HEIGHT = 1456;
    private static final int FONT_HEIGHT = 2163;
    private Object mKeyguardStatusView;
    private TextView mClockView;
    private static Rect mQsBounds = new Rect();
    private static boolean mKeyguardShowing = false;
    private static boolean mHeaderAnimating;

    public QSContainerHelper(ViewGroup notificationPanelView, ViewGroup qsContainer, ViewGroup header, ViewGroup qsPanel) {
        mNotificationPanelView = notificationPanelView;
        mQSContainer = qsContainer;
        mHeader = header;
        mQSPanel = qsPanel;
        mQSPanel.setClipToPadding(false);
        mQSContainer.setPadding(0, 0, 0, 0);
        mQSDetail = StatusBarHeaderHooks.qsHooks.setupQsDetail(mQSPanel, mHeader);

        //((ViewGroup) mHeader.getParent()).removeView(mHeader);
        //mQSContainer.addView(mHeader);
        mQSContainer.addView(mQSDetail);
        //mQSDetail = (View) XposedHelpers.getObjectField(mQSPanel, "mDetail");

        ResourceUtils res = ResourceUtils.getInstance(qsContainer.getContext());
        mHeaderHeight = res.getDimensionPixelSize(R.dimen.status_bar_header_height);
        mQsTopMargin = res.getDimensionPixelSize(R.dimen.qs_margin_top);
        mQSPanel.setPadding(0, 0, 0, res.getDimensionPixelSize(R.dimen.qs_padding_bottom));

        FrameLayout.LayoutParams qsPanelLp = (FrameLayout.LayoutParams) mQSPanel.getLayoutParams();
        qsPanelLp.setMargins(0, res.getDimensionPixelSize(R.dimen.qs_margin_top), 0, 0);
        qsPanel.setLayoutParams(qsPanelLp);

        ViewGroup notificationContainer = (ViewGroup)mNotificationPanelView.getChildAt(1);
        ViewGroup notificationScrollLayout = (ViewGroup)notificationContainer.getChildAt(1);
        ViewGroup scrollView = (ViewGroup)notificationContainer.getChildAt(0);
        LinearLayout linearLayout = (LinearLayout)scrollView.getChildAt(0);

        linearLayout.removeViewAt(0);
        scrollView.removeView(linearLayout);
        scrollView.addView(mQSContainer);
        ViewGroup.LayoutParams notificationContainerParams = scrollView.getLayoutParams();
        notificationContainerParams.height = FrameLayout.LayoutParams.WRAP_CONTENT;
        notificationContainer.setLayoutParams(notificationContainerParams);
        mNotificationPanelView.removeView(mHeader);
        mQSContainer.addView(mHeader,1);
        mQSPanel.setVisibility(View.INVISIBLE);
        mHeader.setClickable(false);
        mHeader.setFocusable(false);
        mHeader.setActivated(false);
        mHeader.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        scrollView.setClipChildren(false);
        scrollView.setClipToPadding(false);

        setUpOnLayout();
    }

    public static void setQsExpansion(float expansion, float headerTranslation) {
        expansion = Math.max(0, expansion);
        boolean keyguardShowing = XposedHelpers.getBooleanField(mNotificationPanelView, "mKeyguardShowing");
        if (keyguardShowing) {
                expansion = 0;
                XposedHelpers.setFloatField(mNotificationPanelView, "mQsExpansionHeight", expansion);
        }
        mQsExpansion = expansion;
        final float translationScaleY = expansion - 1;
        if (!mHeaderAnimating) {
            XposedHelpers.callMethod(mQSContainer, "setTranslationY", keyguardShowing ? (translationScaleY * mHeader.getHeight())
                    : headerTranslation);
        }
        XposedHelpers.callMethod(mHeader, "setExpansion", mKeyguardShowing ? 1 : expansion);
        mQSPanel.setTranslationY(translationScaleY * mQSPanel.getHeight());
        mQSDetail.setFullyExpanded(expansion == 1);
        //mQSAnimator.setPosition(expansion); //not yet
        // TODO implement this
        //mQSDetail.setTranslationY(keyguardShowing ? 0 : -qsPanelTranslationY);
        updateBottom();

        // Set bounds on the QS panel so it doesn't run over the header.
        mQsBounds.top = (int) (mQSPanel.getHeight() * (1 - expansion));
        mQsBounds.right = mQSPanel.getWidth();
        mQsBounds.bottom = mQSPanel.getHeight();
        mQSPanel.setClipBounds(mQsBounds);
    }

    public static void updateBottom() {
        int height = calculateContainerHeight();
        mQSContainer.setBottom(mQSContainer.getTop() + height);
        //mQSDetail.setBottom(mQSContainer.getTop() + height);
    }

    private static int calculateContainerHeight() {
        int mHeightOverride = XposedHelpers.getIntField(mQSContainer, "mHeightOverride");
        int heightOverride = mHeightOverride != -1 ? mHeightOverride : mQSContainer.getMeasuredHeight() - mHeaderHeight;
        return (int) (mQsExpansion * heightOverride) + mHeaderHeight;
    }

    private void setUpOnLayout() {
        mNotificationStackScroller = (ViewGroup)XposedHelpers.getObjectField(mNotificationPanelView, "mNotificationStackScroller");
        mKeyguardStatusView = XposedHelpers.getObjectField(mNotificationPanelView, "mKeyguardStatusView");
        mClockView = (TextView) XposedHelpers.getObjectField(mNotificationPanelView, "mClockView");
    }

    public void notificationPanelViewOnLayout(XC_MethodHook.MethodHookParam param, Class<?> classPanelView) {
        // TODO Too much work for changing just 2 integers? Maybe we could find a better way

        int left = (int) param.args[1], top = (int) param.args[2], right = (int) param.args[3], bottom = (int) param.args[4];
        FrameLayout notificationPanelView = (FrameLayout) param.thisObject;

        // FrameLayout.onLayout
        XposedHelpers.callMethod(notificationPanelView, "layoutChildren", left, top, right, bottom, false);

        // PanelView.onLayout
        XposedHelpers.callMethod(notificationPanelView, "requestPanelHeightUpdate");
        XposedHelpers.setBooleanField(notificationPanelView, "mHasLayoutedSinceDown", true);
        if (XposedHelpers.getBooleanField(notificationPanelView, "mUpdateFlingOnLayout")) {
            Method abortAnimations = XposedHelpers.findMethodBestMatch(classPanelView, "abortAnimations");
            try {
                abortAnimations.invoke(notificationPanelView);
            } catch (Throwable ignore) {
            }
            XposedHelpers.callMethod(notificationPanelView, "fling", XposedHelpers.getFloatField(notificationPanelView, "mUpdateFlingVelocity"), true);
            XposedHelpers.setBooleanField(notificationPanelView, "mUpdateFlingOnLayout", false);
        }

        // NotificationPanelView.onLayout
        XposedHelpers.callMethod(mKeyguardStatusView, "setPivotX", notificationPanelView.getWidth() / 2);
        XposedHelpers.callMethod(mKeyguardStatusView, "setPivotY", (FONT_HEIGHT - CAP_HEIGHT) / 2048f * mClockView.getTextSize());

        // Calculate quick setting heights.
        int oldMaxHeight = XposedHelpers.getIntField(notificationPanelView, "mQsMaxExpansionHeight");
        int mQsMinExpansionHeight = XposedHelpers.getBooleanField(notificationPanelView, "mKeyguardShowing") ? 0 : mHeaderHeight;
        int mQsMaxExpansionHeight = (int) XposedHelpers.callMethod(mQSContainer, "getDesiredHeight");
        XposedHelpers.setIntField(notificationPanelView, "mQsMinExpansionHeight", mQsMinExpansionHeight);
        XposedHelpers.setIntField(notificationPanelView, "mQsMaxExpansionHeight", mQsMaxExpansionHeight);
        XposedHelpers.callMethod(notificationPanelView, "positionClockAndNotifications");
        boolean mQsExpanded = XposedHelpers.getBooleanField(notificationPanelView, "mQsExpanded");
        if (mQsExpanded && XposedHelpers.getBooleanField(notificationPanelView, "mQsFullyExpanded")) {
            XposedHelpers.setIntField(notificationPanelView, "mQsExpansionHeight", mQsMaxExpansionHeight);
            XposedHelpers.callMethod(notificationPanelView, "requestScrollerTopPaddingUpdate", false);
            if (ConfigUtils.M) {
                XposedHelpers.callMethod(notificationPanelView, "requestPanelHeightUpdate");
                // Size has changed, start an animation.
                if (mQsMaxExpansionHeight != oldMaxHeight) {
                    XposedHelpers.callMethod(notificationPanelView, "startQsSizeChangeAnimation", oldMaxHeight, mQsMaxExpansionHeight);
                }
            }
        } else if (!mQsExpanded) {
            setQsExpansion((float) XposedHelpers.callMethod(param.thisObject, "getQsExpansionFraction"),
                    (float) XposedHelpers.callMethod(param.thisObject, "getHeaderTranslation"));
            if (!ConfigUtils.M) {
                XposedHelpers.callMethod(mNotificationStackScroller, "setStackHeight", (float) XposedHelpers.callMethod(notificationPanelView, "getExpandedHeight"));
                XposedHelpers.callMethod(notificationPanelView, "updateHeader");
            }
        }
        if (ConfigUtils.M) {
            XposedHelpers.callMethod(notificationPanelView, "updateStackHeight", (float) XposedHelpers.callMethod(notificationPanelView, "getExpandedHeight"));
            XposedHelpers.callMethod(notificationPanelView, "updateHeader");
        }
        XposedHelpers.callMethod(mNotificationStackScroller, "updateIsSmallScreen", mHeaderHeight);

        if (ConfigUtils.M) {
            // If we are running a size change animation, the animation takes care of the height of
            // the container. However, if we are not animating, we always need to make the QS container
            // the desired height so when closing the QS detail, it stays smaller after the size change
            // animation is finished but the detail view is still being animated away (this animation
            // takes longer than the size change animation).
            if (XposedHelpers.getObjectField(notificationPanelView, "mQsSizeChangeAnimator") == null) {
                if (mQsMaxExpansionHeight != -1) mQsMaxExpansionHeight -= mHeaderHeight;
                XposedHelpers.callMethod(mQSContainer, "setHeightOverride", mQsMaxExpansionHeight);
            }
            XposedHelpers.callMethod(notificationPanelView, "updateMaxHeadsUpTranslation");
        }
    }

    public int getDesiredHeight() {
        if (mQSDetail.isClosingDetail()) {
            return (int) XposedHelpers.callMethod(mQSPanel, "getGridHeight") + mQsTopMargin + mQSContainer.getPaddingBottom();
        } else {
            return mQSContainer.getMeasuredHeight();
        }
    }

    public void animateHeaderSlidingIn() {
        if (!XposedHelpers.getBooleanField(mNotificationPanelView, "mQsExpanded")) {
            mHeaderAnimating = true;
            mQSContainer.getViewTreeObserver().addOnPreDrawListener(mStartHeaderSlidingIn);
        }
    }

    public void animateHeaderSlidingOut() {
        mHeaderAnimating = true;
        mQSContainer.animate().y(-mHeader.getHeight())
                .setStartDelay(0)
                .setDuration(360)
                .setInterpolator(Interpolators.FAST_OUT_SLOW_IN)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mQSContainer.animate().setListener(null);
                        mHeaderAnimating = false;
                        XposedHelpers.callMethod(mNotificationPanelView, "updateQsState");
                    }
                })
                .start();
    }

    private final ViewTreeObserver.OnPreDrawListener mStartHeaderSlidingIn
            = new ViewTreeObserver.OnPreDrawListener() {
        @Override
        public boolean onPreDraw() {
            mQSContainer.getViewTreeObserver().removeOnPreDrawListener(this);
            mQSContainer.animate()
                    .translationY(0f)
                    .setDuration(448)
                    .setInterpolator(Interpolators.FAST_OUT_SLOW_IN)
                    .setListener(mAnimateHeaderSlidingInListener)
                    .start();
            mQSContainer.setY(-mHeader.getHeight());
            return true;
        }
    };

    private final Animator.AnimatorListener mAnimateHeaderSlidingInListener
            = new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
            mHeaderAnimating = false;
            XposedHelpers.callMethod(mNotificationPanelView, "updateQsState");
        }
    };

    public int getBottom() {
        return mQSContainer.getBottom();
    }

    public QSDetail getQSDetail() {
        return mQSDetail;
    }
}
