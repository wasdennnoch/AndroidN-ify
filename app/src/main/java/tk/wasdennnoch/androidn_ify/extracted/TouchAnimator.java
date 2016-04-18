// Decompiled by Jad v1.5.8e. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/kpdus/jad.html
// Decompiler options: braces fieldsfirst space lnc

package tk.wasdennnoch.androidn_ify.extracted;

import android.util.FloatProperty;
import android.util.MathUtils;
import android.util.Property;
import android.view.View;
import android.view.animation.Interpolator;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class TouchAnimator {

    public static class Builder {

        private float mEndDelay;
        private Interpolator mInterpolator;
        private Listener mListener;
        private float mStartDelay;
        private List<View> mTargets;
        private List<KeyframeSet> mValues;

        private void add(View obj, KeyframeSet keyframeset) {
            mTargets.add(obj);
            mValues.add(keyframeset);
        }

        private static Property<View, Float> getProperty(Object obj, String s, Class class1) {
            if (obj instanceof View) {
                if (s.equals("translationX")) {
                    return View.TRANSLATION_X;
                }
                if (s.equals("translationY")) {
                    return View.TRANSLATION_Y;
                }
                if (s.equals("translationZ")) {
                    return View.TRANSLATION_Z;
                }
                if (s.equals("alpha")) {
                    return View.ALPHA;
                }
                if (s.equals("rotation")) {
                    return View.ROTATION;
                }
                if (s.equals("x")) {
                    return View.X;
                }
                if (s.equals("y")) {
                    return View.Y;
                }
                if (s.equals("scaleX")) {
                    return View.SCALE_X;
                }
                if (s.equals("scaleY")) {
                    return View.SCALE_Y;
                }
            }
            if ((obj instanceof TouchAnimator) && "position".equals(s)) {
                return POSITION;
            } else {
                return Property.of(obj.getClass(), class1, s);
            }
        }

        public Builder addFloat(View obj, String s, float af[]) {
            add(obj, KeyframeSet.ofFloat(getProperty(obj, s, Float.TYPE), af));
            return this;
        }

        public TouchAnimator build() {
            return new TouchAnimator(mTargets.toArray(new View[mTargets.size()]), mValues.toArray(new KeyframeSet[mValues.size()]), mStartDelay, mEndDelay, mInterpolator, mListener, null);
        }

        public Builder setEndDelay(float f) {
            mEndDelay = f;
            return this;
        }

        public Builder setInterpolator(Interpolator interpolator) {
            mInterpolator = interpolator;
            return this;
        }

        public Builder setListener(Listener listener) {
            mListener = listener;
            return this;
        }

        public Builder setStartDelay(float f) {
            mStartDelay = f;
            return this;
        }

        public Builder() {
            mTargets = new ArrayList<>();
            mValues = new ArrayList<>();
        }
    }

    private static class FloatKeyframeSet extends KeyframeSet {

        private final Property<View, Float> mProperty;
        private final float mValues[];

        protected void interpolate(int i, float f, View obj) {
            float f1 = mValues[i - 1];
            float f2 = mValues[i];
            mProperty.set(obj, (f2 - f1) * f + f1);
        }

        public FloatKeyframeSet(Property<View, Float> property, float af[]) {
            super(af.length);
            mProperty = property;
            mValues = af;
        }
    }

    private static abstract class KeyframeSet {

        private final float mFrameWidth;
        private final int mSize;

        public static KeyframeSet ofFloat(Property<View, Float> property, float af[]) {
            return new FloatKeyframeSet(property, af);
        }

        protected abstract void interpolate(int i, float f, View obj);

        void setValue(float f, View obj) {
            int i;
            for (i = 1; i < mSize - 1 && f > mFrameWidth; i++) {
            }
            interpolate(i, f / mFrameWidth, obj);
        }

        public KeyframeSet(int i) {
            mSize = i;
            mFrameWidth = 1.0F / (float) (i - 1);
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
    private final KeyframeSet mKeyframeSets[];
    private final Listener mListener;
    private final float mSpan;
    private final float mStartDelay;
    private final View mTargets[];
    private float mLastT;

    private TouchAnimator(View targets[], KeyframeSet keyframeset[], float startDelay, float endDelay, Interpolator interpolator, Listener listener) {
        mLastT = -1F;
        mTargets = targets;
        mKeyframeSets = keyframeset;
        mStartDelay = startDelay;
        mSpan = 1.0F - endDelay - mStartDelay;
        mInterpolator = interpolator;
        mListener = listener;
    }

    TouchAnimator(View targets[], KeyframeSet keyframeset[], float startDelay, float endDelay, Interpolator interpolator, Listener listener, TouchAnimator touchanimator) {
        this(targets, keyframeset, startDelay, endDelay, interpolator, listener);
    }

    public void setPosition(float f) {
        float f1 = MathUtils.constrain((f - mStartDelay) / mSpan, 0.0F, 1.0F);
        f = f1;
        if (mInterpolator != null) {
            f = mInterpolator.getInterpolation(f1);
        }
        if (f == mLastT) {
            return;
        }
        if (mListener == null) {
            for (int i = 0; i < mTargets.length; i++) {
                mKeyframeSets[i].setValue(f, mTargets[i]);
            }
        } else {
            if (f != 1.0F) {
                if (f == 0.0F) {
                    mListener.onAnimationAtStart();
                } else if (mLastT <= 0.0F || mLastT == 1.0F) {
                    mListener.onAnimationStarted();
                }
                mLastT = f;
            } else {
                mListener.onAnimationAtEnd();
            }
        }
    }


    /*public void setPosition(float f) {
        float f1 = MathUtils.constrain((f - mStartDelay) / mSpan, 0.0F, 1.0F);
        f = f1;
        if (mInterpolator != null) {
            f = mInterpolator.getInterpolation(f1);
        }
        if (f == mLastT) {
            return;
        }
        if (mListener == null)goto _L2;else goto _L1
        _L1:
        if (f != 1.0F)goto _L4;else goto _L3
        _L3:
        mListener.onAnimationAtEnd();
        _L6:
        mLastT = f;
        _L2:
        for (int i = 0; i < mTargets.length; i++) {
            mKeyframeSets[i].setValue(f, mTargets[i]);
        }

        break; // Loop/switch isn't completed
        _L4:
        if (f == 0.0F) {
            mListener.onAnimationAtStart();
        } else if (mLastT <= 0.0F || mLastT == 1.0F) {
            mListener.onAnimationStarted();
        }
        if (true)goto _L6;else goto _L5
        _L5:
    }*/

}
