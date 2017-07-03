package tk.wasdennnoch.androidn_ify.systemui.notifications;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.graphics.Outline;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;

import java.util.ArrayList;

import de.robv.android.xposed.XposedHelpers;

public class ExpandableOutlineViewHelper {
    private static final String TAG = "ExpandableOutlineViewHelper";

    private static ArrayList<ExpandableOutlineViewHelper> instances = new ArrayList<>();

    public FrameLayout mExpandableView;

    private boolean mCustomOutline;
    private float mOutlineAlpha = -1f;
    public float mNormalBackgroundVisibilityAmount;
    public float mDimmedBackgroundFadeInAmount = -1;
    public float mShadowAlpha = 1f;
    public float mBgAlpha = 1f;
    public int mCurrentBackgroundTint;
    public int mTargetTint;
    public int mStartTint;
    private final Rect mOutlineRect = new Rect();
    public ValueAnimator mFadeInFromDarkAnimator;
    public ValueAnimator mBackgroundColorAnimator;

    private ViewOutlineProvider mProvider;

    public ValueAnimator.AnimatorUpdateListener mBackgroundVisibilityUpdater
            = new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            ActivatableNotificationViewHooks.setNormalBackgroundVisibilityAmount(mExpandableView, getBackgroundNormal().getAlpha());
            mDimmedBackgroundFadeInAmount = getBackgroundDimmed().getAlpha();
        }
    };

    public AnimatorListenerAdapter mFadeInEndListener = new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
            super.onAnimationEnd(animation);
            mFadeInFromDarkAnimator = null;
            mDimmedBackgroundFadeInAmount = -1;
            ActivatableNotificationViewHooks.updateBackground(mExpandableView);
        }
    };
    public ValueAnimator.AnimatorUpdateListener mUpdateOutlineListener
            = new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            ActivatableNotificationViewHooks.updateOutlineAlpha(ExpandableOutlineViewHelper.this);
        }
    };

    private ExpandableOutlineViewHelper() {
        instances.add(this);
    }

    public static ExpandableOutlineViewHelper getInstance(Object expandableView) {
        for (ExpandableOutlineViewHelper e : instances)
            if (e.mExpandableView.equals(expandableView))
                return e;
        return new ExpandableOutlineViewHelper();
    }

    public void construct(Object expandableView) {
        mExpandableView = (FrameLayout) expandableView;
        mProvider = new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                int translation = (int) mExpandableView.getTranslationX();
                if (!mCustomOutline) {
                    outline.setRect(translation,
                            XposedHelpers.getIntField(mExpandableView, "mClipTopAmount"),
                            mExpandableView.getWidth() + translation,
                            Math.max(XposedHelpers.getIntField(mExpandableView, "mActualHeight"), XposedHelpers.getIntField(mExpandableView, "mClipTopAmount")));
                } else {
                    outline.setRect(mOutlineRect);
                }
                outline.setAlpha(mOutlineAlpha);
            }
        };
        mExpandableView.setOutlineProvider(mProvider);
    }

    public float getOutlineAlpha() {
        return mOutlineAlpha;
    }

    public int getOutlineTranslation() {
        return mCustomOutline ? mOutlineRect.left : (int) mExpandableView.getTranslationX();
    }

    public void updateOutline() {
        if (mCustomOutline) {
            return;
        }
        mExpandableView.setOutlineProvider(mProvider);
    }

    public boolean isOutlineShowing() {
        ViewOutlineProvider op = mExpandableView.getOutlineProvider();
        return op != null;
    }

    protected void setOutlineRect(RectF rect) {
        if (rect != null) {
            setOutlineRect(rect.left, rect.top, rect.right, rect.bottom);
        } else {
            mCustomOutline = false;
            mExpandableView.setClipToOutline(false);
            mExpandableView.invalidateOutline();
        }
    }

    protected void setOutlineRect(float left, float top, float right, float bottom) {
        mCustomOutline = true;
        mExpandableView.setClipToOutline(true);

        mOutlineRect.set((int) left, (int) top, (int) right, (int) bottom);

        // Outlines need to be at least 1 dp
        mOutlineRect.bottom = (int) Math.max(top, mOutlineRect.bottom);
        mOutlineRect.right = (int) Math.max(left, mOutlineRect.right);

        mExpandableView.invalidateOutline();
    }

    public void setShadowAlpha(float shadowAlpha) {
        mShadowAlpha = shadowAlpha;
    }

    protected void setOutlineAlpha(float alpha) {
        if (alpha != mOutlineAlpha) {
            mOutlineAlpha = alpha;
            mExpandableView.invalidateOutline();
        }
    }

    public View getBackgroundNormal() {
        return (View) XposedHelpers.getObjectField(mExpandableView, "mBackgroundNormal");
    }

    public View getBackgroundDimmed() {
        return (View) XposedHelpers.getObjectField(mExpandableView, "mBackgroundDimmed");
    }
}
