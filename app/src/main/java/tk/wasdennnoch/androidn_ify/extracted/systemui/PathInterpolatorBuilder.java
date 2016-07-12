package tk.wasdennnoch.androidn_ify.extracted.systemui;

import android.graphics.Path;
import android.view.animation.Interpolator;

import de.robv.android.xposed.XposedHelpers;

public class PathInterpolatorBuilder {

    private static class PathInterpolator implements Interpolator {

        private final float mX[];
        private final float mY[];

        @Override
        public float getInterpolation(float f) {
            if (f <= 0.0F) {
                return 0.0F;
            }
            if (f >= 1.0F) {
                return 1.0F;
            }
            int i = 0;
            int j;
            for (j = mX.length - 1; j - i > 1;) {
                int k = (i + j) / 2;
                if (f < mX[k]) {
                    j = k;
                } else {
                    i = k;
                }
            }

            float f1 = mX[j] - mX[i];
            if (f1 == 0.0F) {
                return mY[i];
            } else {
                f = (f - mX[i]) / f1;
                f1 = mY[i];
                return (mY[j] - f1) * f + f1;
            }
        }

        private PathInterpolator(float af[], float af1[]) {
            mX = af;
            mY = af1;
        }

        PathInterpolator(float af[], float af1[], PathInterpolator pathinterpolator) {
            this(af, af1);
        }

    }

    private float mDist[];
    private float mX[];
    private float mY[];

    public PathInterpolatorBuilder(float f, float f1, float f2, float f3) {
        initCubic(f, f1, f2, f3);
    }

    private void initCubic(float f, float f1, float f2, float f3) {
        Path path = new Path();
        path.moveTo(0.0F, 0.0F);
        path.cubicTo(f, f1, f2, f3, 1.0F, 1.0F);
        initPath(path);
    }

    private void initPath(Path p) {
        float[] path = (float[]) XposedHelpers.callMethod(p, "approximate", 0.002F);
        int l;
        // Decomplied code surely is beautiful.
        for (l = path.length / 3; path[1] != 0.0F || path[2] != 0.0F || path[path.length - 2] != 1.0F || path[path.length - 1] != 1.0F;) {
            throw new IllegalArgumentException("The Path must start at (0,0) and end at (1,1)");
        }

        mX = new float[l];
        mY = new float[l];
        mDist = new float[l];
        float f1 = 0.0F;
        float f = 0.0F;
        int i = 0;
        for (int k = 0; i < l; k++) {
            int i1 = k + 1;
            float f2 = path[k];
            k = i1 + 1;
            float f3 = path[i1];
            float f4 = path[k];
            if (f2 == f && f3 != f1) {
                throw new IllegalArgumentException("The Path cannot have discontinuity in the X axis.");
            }
            if (f3 < f1) {
                throw new IllegalArgumentException("The Path cannot loop back on itself.");
            }
            mX[i] = f3;
            mY[i] = f4;
            if (i > 0) {
                f = mX[i] - mX[i - 1];
                f1 = mY[i] - mY[i - 1];
                f = (float)Math.sqrt(f * f + f1 * f1);
                mDist[i] = mDist[i - 1] + f;
            }
            f1 = f3;
            f = f2;
            i++;
        }

        f = mDist[mDist.length - 1];
        for (int j = 0; j < l; j++) {
            path = mDist;
            path[j] = path[j] / f;
        }

    }

    public Interpolator getXInterpolator() {
        return new PathInterpolator(mDist, mX, null);
    }

    public Interpolator getYInterpolator() {
        return new PathInterpolator(mDist, mY, null);
    }
}
