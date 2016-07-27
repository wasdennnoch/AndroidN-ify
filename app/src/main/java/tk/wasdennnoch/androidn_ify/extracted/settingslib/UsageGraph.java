package tk.wasdennnoch.androidn_ify.extracted.settingslib;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

public class UsageGraph extends View {

    /*private int mAccentColor;
    private final int mCornerRadius;
    private final Drawable mDivider;
    private final int mDividerSize;
    private final Paint mDottedPaint;
    private final Paint mFillPaint;
    private final Paint mLinePaint;
    private final SparseIntArray mLocalPaths;
    private float mMaxX;
    private float mMaxY;
    private float mMiddleDividerLoc;
    private int mMiddleDividerTint;
    private final Path mPath;
    private final SparseIntArray mPaths;
    private boolean mProjectUp;
    private boolean mShowProjection;
    private final Drawable mTintedDivider;
    private int mTopDividerTint;*/

    public UsageGraph(final Context context, final AttributeSet set) {
        super(context, set);
        /*final int n = -1;
        final float n2 = 100.0f;
        final int antiAlias = 1;
        this.mPath = new Path();
        this.mPaths = new SparseIntArray();
        this.mLocalPaths = new SparseIntArray();
        this.mMaxX = n2;
        this.mMaxY = n2;
        this.mMiddleDividerLoc = 0.5f;
        this.mMiddleDividerTint = n;
        this.mTopDividerTint = n;
        final Resources resources = context.getResources();
        (this.mLinePaint = new Paint()).setStyle(Paint.Style.STROKE);
        this.mLinePaint.setStrokeCap(Paint.Cap.ROUND);
        this.mLinePaint.setStrokeJoin(Paint.Join.ROUND);
        this.mLinePaint.setAntiAlias(antiAlias != 0);
        //this.mCornerRadius = resources.getDimensionPixelSize(R.dimen.usage_graph_line_corner_radius);
        this.mLinePaint.setPathEffect(new CornerPathEffect((float) this.mCornerRadius));
        //this.mLinePaint.setStrokeWidth((float) resources.getDimensionPixelSize(R.dimen.usage_graph_line_width));
        (this.mFillPaint = new Paint(this.mLinePaint)).setStyle(Paint.Style.FILL);
        (this.mDottedPaint = new Paint(this.mLinePaint)).setStyle(Paint.Style.STROKE);
        final float n3 = resources.getDimensionPixelSize(R.dimen.usage_graph_dot_size);
        final float n4 = resources.getDimensionPixelSize(R.dimen.usage_graph_dot_interval);
        this.mDottedPaint.setStrokeWidth(3.0f * n3);
        final Paint mDottedPaint = this.mDottedPaint;
        final float[] array = {n3, 0.0f};
        array[antiAlias] = n4;
        mDottedPaint.setPathEffect(new DashPathEffect(array, 0.0f));
        //noinspection deprecation
        this.mDottedPaint.setColor(context.getResources().getColor(R.color.usage_graph_dots));
        final TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(16843284, typedValue, antiAlias != 0);
        this.mDivider = context.getDrawable(typedValue.resourceId);
        this.mTintedDivider = context.getDrawable(typedValue.resourceId);
        this.mDividerSize = resources.getDimensionPixelSize(R.dimen.usage_graph_divider_size);*/
    }

    /*private void calculateLocalPaths() {
        final int n = -1;
        if (this.getWidth() == 0) {
            return;
        }
        this.mLocalPaths.clear();
        int n2 = 0;
        int n3 = -1;
        for (int i = 0; i < this.mPaths.size(); ++i) {
            final int key = this.mPaths.keyAt(i);
            final int value = this.mPaths.valueAt(i);
            if (value == n) {
                if (i == this.mPaths.size() - 1 && n3 != n) {
                    this.mLocalPaths.put(n2, n3);
                }
                n3 = -1;
                this.mLocalPaths.put(n2 + 1, n);
            } else {
                final int x = this.getX(key);
                final int y = this.getY(value);
                n2 = x;
                if (this.mLocalPaths.size() > 0) {
                    final int key2 = this.mLocalPaths.keyAt(this.mLocalPaths.size() - 1);
                    final int value2 = this.mLocalPaths.valueAt(this.mLocalPaths.size() - 1);
                    if (value2 != n && !this.hasDiff(key2, x) && !this.hasDiff(value2, y)) {
                        n3 = y;
                        continue;
                    }
                }
                this.mLocalPaths.put(x, y);
            }
        }
    }

    private void drawDivider(final int n, final Canvas canvas, final int tint) {
        Drawable drawable = this.mDivider;
        if (tint != -1) {
            this.mTintedDivider.setTint(tint);
            drawable = this.mTintedDivider;
        }
        drawable.setBounds(0, n, canvas.getWidth(), this.mDividerSize + n);
        drawable.draw(canvas);
    }

    private void drawFilledPath(final Canvas canvas) {
        this.mPath.reset();
        float n = this.mLocalPaths.keyAt(0);
        this.mPath.moveTo((float) this.mLocalPaths.keyAt(0), (float) this.mLocalPaths.valueAt(0));
        for (int i = 1; i < this.mLocalPaths.size(); ++i) {
            final int key = this.mLocalPaths.keyAt(i);
            final int value = this.mLocalPaths.valueAt(i);
            if (value == -1) {
                this.mPath.lineTo((float) this.mLocalPaths.keyAt(i - 1), (float) this.getHeight());
                this.mPath.lineTo(n, (float) this.getHeight());
                this.mPath.close();
                ++i;
                if (i < this.mLocalPaths.size()) {
                    n = this.mLocalPaths.keyAt(i);
                    this.mPath.moveTo((float) this.mLocalPaths.keyAt(i), (float) this.mLocalPaths.valueAt(i));
                }
            } else {
                this.mPath.lineTo((float) key, (float) value);
            }
        }
        canvas.drawPath(this.mPath, this.mFillPaint);
    }

    private void drawLinePath(final Canvas canvas) {
        this.mPath.reset();
        this.mPath.moveTo((float) this.mLocalPaths.keyAt(0), (float) this.mLocalPaths.valueAt(0));
        for (int i = 1; i < this.mLocalPaths.size(); ++i) {
            final int key = this.mLocalPaths.keyAt(i);
            final int value = this.mLocalPaths.valueAt(i);
            if (value == -1) {
                ++i;
                if (i < this.mLocalPaths.size()) {
                    this.mPath.moveTo((float) this.mLocalPaths.keyAt(i), (float) this.mLocalPaths.valueAt(i));
                }
            } else {
                this.mPath.lineTo((float) key, (float) value);
            }
        }
        canvas.drawPath(this.mPath, this.mLinePaint);
    }

    private void drawProjection(final Canvas canvas) {
        this.mPath.reset();
        this.mPath.moveTo((float) this.mLocalPaths.keyAt(this.mLocalPaths.size() - 2), (float) this.mLocalPaths.valueAt(this.mLocalPaths.size() - 2));
        final Path mPath = this.mPath;
        final float n = canvas.getWidth();
        int height;
        if (this.mProjectUp) {
            height = 0;
        } else {
            height = canvas.getHeight();
        }
        mPath.lineTo(n, (float) height);
        canvas.drawPath(this.mPath, this.mDottedPaint);
    }

    private int getColor(final int n, final float n2) {
        return ((int) (255.0f * n2) << 24 | 0xFFFFFF) & n;
    }

    private int getX(final float n) {
        return (int) (n / this.mMaxX * this.getWidth());
    }

    private int getY(final float n) {
        return (int) (this.getHeight() * (1.0f - n / this.mMaxY));
    }

    private boolean hasDiff(final int n, final int n2) {
        return Math.abs(n2 - n) >= this.mCornerRadius;
    }

    private void updateGradient() {
        this.mFillPaint.setShader(new LinearGradient(0.0f, 0.0f, 0.0f, (float) this.getHeight(), this.getColor(this.mAccentColor, 0.2f), 0, Shader.TileMode.CLAMP));
    }

    public void addPath(final SparseIntArray sparseIntArray) {
        for (int i = 0; i < sparseIntArray.size(); ++i) {
            this.mPaths.put(sparseIntArray.keyAt(i), sparseIntArray.valueAt(i));
        }
        this.mPaths.put(sparseIntArray.keyAt(sparseIntArray.size() - 1) + 1, -1);
        this.calculateLocalPaths();
        this.postInvalidate();
    }

    void clearPaths() {
        this.mPaths.clear();
    }

    protected void onDraw(final Canvas canvas) {
        if (this.mMiddleDividerLoc != 0.0f) {
            this.drawDivider(0, canvas, this.mTopDividerTint);
        }
        this.drawDivider((int) ((canvas.getHeight() - this.mDividerSize) * this.mMiddleDividerLoc), canvas, this.mMiddleDividerTint);
        this.drawDivider(canvas.getHeight() - this.mDividerSize, canvas, -1);
        if (this.mLocalPaths.size() == 0) {
            return;
        }
        if (this.mShowProjection) {
            this.drawProjection(canvas);
        }
        this.drawFilledPath(canvas);
        this.drawLinePath(canvas);
    }

    protected void onSizeChanged(final int n, final int n2, final int n3, final int n4) {
        super.onSizeChanged(n, n2, n3, n4);
        this.updateGradient();
        this.calculateLocalPaths();
    }

    void setAccentColor(final int mAccentColor) {
        this.mAccentColor = mAccentColor;
        this.mLinePaint.setColor(this.mAccentColor);
        this.updateGradient();
        this.postInvalidate();
    }

    void setMax(final int n, final int n2) {
        this.mMaxX = n;
        this.mMaxY = n2;
    }

    void setShowProjection(final boolean mShowProjection, final boolean mProjectUp) {
        this.mShowProjection = mShowProjection;
        this.mProjectUp = mProjectUp;
        this.postInvalidate();
    }*/

}