package tk.wasdennnoch.androidn_ify.phone.emergency;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.ui.EmergencyInfoActivity;
import tk.wasdennnoch.androidn_ify.utils.ResourceUtils;

public class EmergencyButtonWrapper implements View.OnClickListener {

    private static final String TAG = "EmergencyButton";

    private static final long HIDE_DELAY = 3000;
    private static final int RIPPLE_DURATION = 600;
    private static final long RIPPLE_PAUSE = 1000;

    private final Interpolator mFastOutLinearInInterpolator;
    private Context mContext;

    private FrameLayout mLayout;
    private Button mButton;
    private ViewGroup mSelectedContainer;
    private TextView mSelectedLabel;
    private View mRippleView;
    private TextView mLaunchHint;

    private boolean mHiding;

    public EmergencyButtonWrapper(Context context, ViewGroup parent) {
        mContext = context;
        mFastOutLinearInInterpolator = AnimationUtils.loadInterpolator(context, android.R.interpolator.fast_out_linear_in);
        ResourceUtils res = ResourceUtils.getInstance(context);
        mLayout = (FrameLayout) LayoutInflater.from(context).inflate(
                res.getLayout(R.layout.emergency_button),
                parent,
                false); // We don't want the button to be the last view in its parent but the first, so add it later
        parent.addView(mLayout, 0);
        mButton = (Button) mLayout.findViewById(R.id.button);
        mSelectedContainer = (ViewGroup) mLayout.findViewById(R.id.selected_container);
        mSelectedLabel = (TextView) mLayout.findViewById(R.id.selected_label);
        mRippleView = mLayout.findViewById(R.id.ripple_view);
        mLaunchHint = (TextView) mLayout.findViewById(R.id.launch_hint);
        mButton.setOnClickListener(this);
        mSelectedContainer.setOnClickListener(this);

        // Need to set text here because otherwise it would use the Dialer's resources
        mButton.setText(res.getString(R.string.emergency_info));
        mLaunchHint.setText(res.getString(R.string.emergency_info_launch_hint));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button:
                XposedHook.logI("androidn_ify", "click button");
                if (((AccessibilityManager) mContext.getSystemService(Context.ACCESSIBILITY_SERVICE)).isTouchExplorationEnabled()) {
                    startEmergencyActivity();
                } else {
                    revealTheButton((Button) v);
                }
                break;
            case R.id.selected_container:
                XposedHook.logI("androidn_ify", "click select container");
                if (!mHiding) {
                    startEmergencyActivity();
                }
                break;
        }
    }

    private void startEmergencyActivity() {
        try {
            XposedHook.logI("androidn_ify", "try");
            mContext.startActivity(new Intent("tk.wasdennnoch.androidn_ify.ui.EmergencyInfoActivity")
                    .setClassName("tk.wasdennnoch.androidn_ify", EmergencyInfoActivity.class.getName()).setFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS));
        } catch (Exception e) {
            XposedHook.logI("androidn_ify", "catch");
            XposedHook.logE(TAG, "Unable to start emergency activity!", e);
        }
    }

    private void revealTheButton(Button v) {
        XposedHook.logI("androidn_ify", "reveal");
        mSelectedContainer.setVisibility(View.VISIBLE);
        int centerX = v.getLeft() + v.getWidth() / 2;
        int centerY = v.getTop() + v.getHeight() / 2;
        Animator reveal = ViewAnimationUtils.createCircularReveal( // TODO work out why this isn't working, same for hide
                mSelectedContainer,
                centerX,
                centerY,
                0,
                Math.max(centerX, mSelectedContainer.getWidth() - centerX)
                        + Math.max(centerY, mSelectedContainer.getHeight() - centerY));
        reveal.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mButton.setVisibility(View.INVISIBLE);
            }
        });
        reveal.start();

        //animateHintText(mSelectedLabel, v, reveal);
        animateHintText(mLaunchHint, v, reveal);

        mSelectedLabel.setText(v.getText());
        mLayout.postDelayed(mHideRunnable, HIDE_DELAY);
        mLayout.postDelayed(mRippleRunnable, RIPPLE_PAUSE / 2);

        // Transfer focus from the originally clicked button to the expanded button.
        mSelectedContainer.requestFocus();
    }

    private void animateHintText(View selectedView, View v, Animator reveal) {
        selectedView.setTranslationX(
                (v.getLeft() + v.getWidth() / 2 - mSelectedContainer.getWidth() / 2) / 5);
        selectedView.animate()
                .setDuration(reveal.getDuration() / 3)
                .setStartDelay(reveal.getDuration() / 5)
                .translationX(0)
                .setInterpolator(mFastOutLinearInInterpolator)
                .start();
    }

    private void hideTheButton() {
        XposedHook.logI("androidn_ify", "hide");
        if (mHiding || mSelectedContainer.getVisibility() != View.VISIBLE) {
            return;
        }

        mHiding = true;

        mLayout.removeCallbacks(mHideRunnable);

        View v = mButton;
        v.setVisibility(View.VISIBLE);
        int centerX = v.getLeft() + v.getWidth() / 2;
        int centerY = v.getTop() + v.getHeight() / 2;
        Animator reveal = ViewAnimationUtils.createCircularReveal(
                mSelectedContainer,
                centerX,
                centerY,
                Math.max(centerX, mSelectedContainer.getWidth() - centerX)
                        + Math.max(centerY, mSelectedContainer.getHeight() - centerY),
                0);
        reveal.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mSelectedContainer.setVisibility(View.INVISIBLE);
                mLayout.removeCallbacks(mRippleRunnable);
                mHiding = false;
            }
        });
        reveal.start();

        // Transfer focus back to the originally clicked button.
        if (mSelectedContainer.isFocused()) {
            v.requestFocus();
        }
    }

    private void startRipple() {
        XposedHook.logI("androidn_ify", "ripple");
        final View ripple = mRippleView;
        ripple.animate().cancel();
        ripple.setVisibility(View.VISIBLE);
        Animator reveal = ViewAnimationUtils.createCircularReveal(
                ripple,
                ripple.getLeft() + ripple.getWidth() / 2,
                ripple.getTop() + ripple.getHeight() / 2,
                0,
                ripple.getWidth() / 2);
        reveal.setDuration(RIPPLE_DURATION);
        reveal.start();

        ripple.setAlpha(0);
        ripple.animate().alpha(1).setDuration(RIPPLE_DURATION / 2)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        ripple.animate().alpha(0).setDuration(RIPPLE_DURATION / 2)
                                .withEndAction(new Runnable() {
                                    @Override
                                    public void run() {
                                        ripple.setVisibility(View.INVISIBLE);
                                        mLayout.postDelayed(mRippleRunnable, RIPPLE_PAUSE);
                                    }
                                }).start();
                    }
                }).start();
    }

    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            if (!mLayout.isAttachedToWindow()) return;
            hideTheButton();
        }
    };

    private final Runnable mRippleRunnable = new Runnable() {
        @Override
        public void run() {
            if (!mLayout.isAttachedToWindow()) return;
            startRipple();
        }
    };

}
