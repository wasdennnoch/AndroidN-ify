package tk.wasdennnoch.androidn_ify.extracted.systemui;

import android.util.FloatProperty;
import android.util.MathUtils;
import android.util.Property;
import android.view.View;
import android.view.animation.Interpolator;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class TouchAnimator {

    private static final FloatProperty POSITION = new FloatProperty("position") {
        public Float get(TouchAnimator touchanimator) {
            return touchanimator.mLastT;
        }

        public Object get(Object obj) {
            return get((TouchAnimator) obj);
        }

        public void setValue(TouchAnimator touchanimator, float f) {
            touchanimator.setPosition(f);
        }

        public void setValue(Object obj, float f) {
            setValue((TouchAnimator) obj, f);
        }
    };
    private final Interpolator mInterpolator;
    private final KeyframeSet[] mKeyframeSets;
    private float mLastT = -1.0F;
    private final Listener mListener;
    private final float mSpan;
    private final float mStartDelay;
    private final View[] mTargets;

    private TouchAnimator(View targets[], KeyframeSet keyframeset[], float startDelay, float endDelay, Interpolator interpolator, Listener listener) {
        mTargets = targets;
        mKeyframeSets = keyframeset;
        mStartDelay = startDelay;
        mSpan = (1.0F - endDelay - mStartDelay);
        mInterpolator = interpolator;
        mListener = listener;
    }

    public void setPosition(float paramFloat) {
        float f = MathUtils.constrain((paramFloat - mStartDelay) / mSpan, 0.0F, 1.0F);
        paramFloat = f;
        if (mInterpolator != null) {
            paramFloat = mInterpolator.getInterpolation(f);
        }
        if (paramFloat == mLastT) {
            return;
        }
        if (mListener != null) {
            if (paramFloat != 1.0F) {
                if (paramFloat == 0.0F) {
                    mListener.onAnimationAtStart();
                } else if (mLastT <= 0.0F || mLastT == 1.0F) {
                    mListener.onAnimationStarted();
                }
                mLastT = paramFloat;
            } else {
                mListener.onAnimationAtEnd();
            }
        }
        for (int i = 0; i < mTargets.length; i++) {
            mKeyframeSets[i].setValue(paramFloat, mTargets[i]);
        }
    }

    public static class Builder {
        private float mEndDelay;
        private Interpolator mInterpolator;
        private Listener mListener;
        private float mStartDelay;
        private List<View> mTargets = new ArrayList<>();
        private List<KeyframeSet> mValues = new ArrayList<>();

        private void add(View paramObject, KeyframeSet paramKeyframeSet) {
            mTargets.add(paramObject);
            mValues.add(paramKeyframeSet);
        }

        private static Property<View, Float> getProperty(View paramObject, String paramString, Class<?> paramClass) {
            if (paramString.equals("translationX")) {
                return View.TRANSLATION_X;
            }
            if (paramString.equals("translationY")) {
                return View.TRANSLATION_Y;
            }
            if (paramString.equals("translationZ")) {
                return View.TRANSLATION_Z;
            }
            if (paramString.equals("alpha")) {
                return View.ALPHA;
            }
            if (paramString.equals("rotation")) {
                return View.ROTATION;
            }
            if (paramString.equals("x")) {
                return View.X;
            }
            if (paramString.equals("y")) {
                return View.Y;
            }
            if (paramString.equals("scaleX")) {
                return View.SCALE_X;
            }
            if (paramString.equals("scaleY")) {
                return View.SCALE_Y;
            }
            return null;
        }

        public Builder addFloat(View paramObject, String paramString, float... paramVarArgs) {
            add(paramObject, KeyframeSet.ofFloat(getProperty(paramObject, paramString, Float.TYPE), paramVarArgs));
            return this;
        }

        public TouchAnimator build() {
            return new TouchAnimator(mTargets.toArray(new View[mTargets.size()]), mValues.toArray(new KeyframeSet[mValues.size()]), mStartDelay, mEndDelay, mInterpolator, mListener);
        }

        public Builder setEndDelay(float paramFloat) {
            mEndDelay = paramFloat;
            return this;
        }

        public Builder setInterpolator(Interpolator paramInterpolator) {
            mInterpolator = paramInterpolator;
            return this;
        }

        public Builder setListener(Listener paramListener) {
            mListener = paramListener;
            return this;
        }

        public Builder setStartDelay(float paramFloat) {
            mStartDelay = paramFloat;
            return this;
        }
    }

    private static class FloatKeyframeSet extends KeyframeSet {
        private final Property<View, Float> mProperty;
        private final float[] mValues;

        public FloatKeyframeSet(Property<View, Float> paramProperty, float[] paramArrayOfFloat) {
            super(paramArrayOfFloat.length);
            mProperty = paramProperty;
            mValues = paramArrayOfFloat;
        }

        protected void interpolate(int paramInt, float paramFloat, View paramObject) {
            float f1 = mValues[(paramInt - 1)];
            float f2 = mValues[paramInt];
            mProperty.set(paramObject, (f2 - f1) * paramFloat + f1);
        }
    }

    private static abstract class KeyframeSet {
        private final float mFrameWidth;
        private final int mSize;

        public KeyframeSet(int paramInt) {
            mSize = paramInt;
            mFrameWidth = (1.0F / (paramInt - 1));
        }

        public static KeyframeSet ofFloat(Property<View, Float> paramProperty, float... paramVarArgs) {
            return new FloatKeyframeSet(paramProperty, paramVarArgs);
        }

        protected abstract void interpolate(int paramInt, float paramFloat, View paramObject);

        void setValue(float paramFloat, View paramObject) {
            int i = 1;
            while ((i < mSize - 1) && (paramFloat > mFrameWidth)) {
                i += 1;
            }
            interpolate(i, paramFloat / mFrameWidth, paramObject);
        }
    }

    public interface Listener {
        void onAnimationAtEnd();

        void onAnimationAtStart();

        void onAnimationStarted();
    }

    public static class ListenerAdapter implements Listener {
        public void onAnimationAtEnd() {
        }

        public void onAnimationAtStart() {
        }

        public void onAnimationStarted() {
        }
    }
}
