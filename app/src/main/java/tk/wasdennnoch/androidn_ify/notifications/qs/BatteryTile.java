package tk.wasdennnoch.androidn_ify.notifications.qs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;

import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.systemui.SystemUIHooks;

public class BatteryTile extends QSTile implements BatteryInfoManager.BatteryStatusListener {

    private BatteryInfoManager.BatteryData mBatteryData;
    private BatteryView mBatteryView;

    public BatteryTile(TilesManager tilesManager, Object host, String key) {
        super(tilesManager, host, key);
    }

    @Override
    public void handleUpdateState(Object state, Object arg) {
        mState.label = "Battery";
        if (mBatteryData != null) {
            if (mBatteryData.charging) {
                if (mBatteryData.level == 100) {
                    mState.label = mResUtils.getString(R.string.charged);
                } else {
                    mState.label = String.format(mResUtils.getString(R.string.charging), mBatteryData.level + "%");
                }
            } else {
                mState.label = mBatteryData.level + "%";
            }
        }
        SystemUIHooks.batteryInfoManager.registerListener(this);
        super.handleUpdateState(state, arg);
    }

    @Override
    public View onCreateIcon() {
        mBatteryView = new BatteryView(mContext);
        return mBatteryView;
    }

    @Override
    public void onBatteryStatusChanged(BatteryInfoManager.BatteryData batteryData) {
        mBatteryData = batteryData;
        refreshState();
    }

    @Override
    public void handleClick() {
        startActivityDismissingKeyguard(Intent.ACTION_POWER_USAGE_SUMMARY);
        super.handleClick();
    }

    @Override
    public void handleDestroy() {
        super.handleDestroy();
        SystemUIHooks.batteryInfoManager.unregisterListener(this);
        mBatteryData = null;
        mBatteryView = null;
    }

    // GB BatteryView
    private class BatteryView extends ImageView {
        private final int[] LEVELS = new int[] { 4, 15, 100 };
        private final int[] COLORS = new int[] { 0xFFFF3300, 0xFFFF3300, 0xFFFFFFFF };
        private static final int BOLT_COLOR = 0xB2000000;
        private static final int FULL = 96;
        private static final int EMPTY = 4;

        private static final float SUBPIXEL = 0.4f;  // inset rects for softer edges

        private int[] mColors;

        private Paint mFramePaint, mBatteryPaint, mWarningTextPaint, mBoltPaint;
        private int mButtonHeight;
        private float mWarningTextHeight;

        private int mHeight;
        private int mWidth;
        private String mWarningString;
        private int mChargeColor;
        private final float[] mBoltPoints;
        private final Path mBoltPath = new Path();

        private final RectF mFrame = new RectF();
        private final RectF mButtonFrame = new RectF();
        private final RectF mClipFrame = new RectF();
        private final Rect mBoltFrame = new Rect();

        public BatteryView(Context context) {
            super(context);

            mWarningString = "!";

            mFramePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mFramePaint.setDither(true);
            mFramePaint.setStrokeWidth(0);
            mFramePaint.setStyle(Paint.Style.FILL_AND_STROKE);
            mFramePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_ATOP));

            mBatteryPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mBatteryPaint.setDither(true);
            mBatteryPaint.setStrokeWidth(0);
            mBatteryPaint.setStyle(Paint.Style.FILL_AND_STROKE);

            mWarningTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            Typeface font = Typeface.create("sans-serif", Typeface.BOLD);
            mWarningTextPaint.setTypeface(font);
            mWarningTextPaint.setTextAlign(Paint.Align.CENTER);

            mBoltPaint = new Paint();
            mBoltPaint.setAntiAlias(true);
            mBoltPoints = loadBoltPoints();
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);

            setColor(Color.WHITE);

            setId(android.R.id.icon);
            setScaleType(ScaleType.CENTER_INSIDE);

            int padding = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2,
                    getResources().getDisplayMetrics()));
            this.setPadding(0, padding, 0, padding);
        }

        private void setColor(int mainColor) {
            COLORS[COLORS.length-1] = mainColor;

            final int N = LEVELS.length;
            mColors = new int[2*N];
            for (int i=0; i<N; i++) {
                mColors[2*i] = LEVELS[i];
                mColors[2*i+1] = COLORS[i];
            }

            mWarningTextPaint.setColor(COLORS[0]);
            mBoltPaint.setColor(BOLT_COLOR);
            mChargeColor = mainColor;
            invalidate();
        }

        private float[] loadBoltPoints() {
            final int[] pts = new int[] { 73,0,392,0,201,259,442,259,4,703,157,334,0,334 };
            int maxX = 0, maxY = 0;
            for (int i = 0; i < pts.length; i += 2) {
                maxX = Math.max(maxX, pts[i]);
                maxY = Math.max(maxY, pts[i + 1]);
            }
            final float[] ptsF = new float[pts.length];
            for (int i = 0; i < pts.length; i += 2) {
                ptsF[i] = (float)pts[i] / maxX;
                ptsF[i + 1] = (float)pts[i + 1] / maxY;
            }
            return ptsF;
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            final Resources res = getResources();
            int width = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 13, res.getDisplayMetrics()));
            setMeasuredDimension(width, MeasureSpec.getSize(heightMeasureSpec));
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            mHeight = h;
            mWidth = w;
            mWarningTextPaint.setTextSize(h * 0.75f);
            mWarningTextHeight = -mWarningTextPaint.getFontMetrics().ascent;
        }

        private int getColorForLevel(int percent) {
            int thresh, color = 0;
            for (int i=0; i<mColors.length; i+=2) {
                thresh = mColors[i];
                color = mColors[i+1];
                if (percent <= thresh) return color;
            }
            return color;
        }

        @SuppressLint("MissingSuperCall")
        @Override
        public void draw(Canvas c) {
            if (mBatteryData == null || mBatteryData.level < 0) return;

            float drawFrac = (float) mBatteryData.level / 100f;
            final int pt = getPaddingTop();
            final int pl = getPaddingLeft();
            final int pr = getPaddingRight();
            final int pb = getPaddingBottom();
            int height = mHeight - pt - pb;
            int width = mWidth - pl - pr;

            mButtonHeight = (int) (height * 0.12f);

            mFrame.set(0, 0, width, height);
            mFrame.offset(pl, pt);

            mButtonFrame.set(
                    mFrame.left + width * 0.25f,
                    mFrame.top,
                    mFrame.right - width * 0.25f,
                    mFrame.top + mButtonHeight + 5 /*cover frame border of intersecting area*/);

            mButtonFrame.top += SUBPIXEL;
            mButtonFrame.left += SUBPIXEL;
            mButtonFrame.right -= SUBPIXEL;

            mFrame.top += mButtonHeight;
            mFrame.left += SUBPIXEL;
            mFrame.top += SUBPIXEL;
            mFrame.right -= SUBPIXEL;
            mFrame.bottom -= SUBPIXEL;

            // first, draw the battery shape
            mFramePaint.setColor(COLORS[COLORS.length-1]);
            mFramePaint.setAlpha(102);
            c.drawRect(mFrame, mFramePaint);

            // fill 'er up
            final int color = mBatteryData.charging ?
                    mChargeColor : getColorForLevel(mBatteryData.level);
            mBatteryPaint.setColor(color);

            if (mBatteryData.level >= FULL) {
                drawFrac = 1f;
            } else if (mBatteryData.level <= EMPTY) {
                drawFrac = 0f;
            }

            c.drawRect(mButtonFrame, drawFrac == 1f ? mBatteryPaint : mFramePaint);

            mClipFrame.set(mFrame);
            mClipFrame.top += (mFrame.height() * (1f - drawFrac));

            c.save(Canvas.CLIP_SAVE_FLAG);
            c.clipRect(mClipFrame);
            c.drawRect(mFrame, mBatteryPaint);
            c.restore();

            if (mBatteryData.charging) {
                // draw the bolt
                final int bl = (int)(mFrame.left + mFrame.width() / 4.5f);
                final int bt = (int)(mFrame.top + mFrame.height() / 6f);
                final int br = (int)(mFrame.right - mFrame.width() / 7f);
                final int bb = (int)(mFrame.bottom - mFrame.height() / 10f);
                if (mBoltFrame.left != bl || mBoltFrame.top != bt
                        || mBoltFrame.right != br || mBoltFrame.bottom != bb) {
                    mBoltFrame.set(bl, bt, br, bb);
                    mBoltPath.reset();
                    mBoltPath.moveTo(
                            mBoltFrame.left + mBoltPoints[0] * mBoltFrame.width(),
                            mBoltFrame.top + mBoltPoints[1] * mBoltFrame.height());
                    for (int i = 2; i < mBoltPoints.length; i += 2) {
                        mBoltPath.lineTo(
                                mBoltFrame.left + mBoltPoints[i] * mBoltFrame.width(),
                                mBoltFrame.top + mBoltPoints[i + 1] * mBoltFrame.height());
                    }
                    mBoltPath.lineTo(
                            mBoltFrame.left + mBoltPoints[0] * mBoltFrame.width(),
                            mBoltFrame.top + mBoltPoints[1] * mBoltFrame.height());
                }
                c.drawPath(mBoltPath, mBoltPaint);
            } else if (mBatteryData.level <= EMPTY) {
                final float x = mWidth * 0.5f;
                final float y = (mHeight + mWarningTextHeight) * 0.48f;
                c.drawText(mWarningString, x, y, mWarningTextPaint);
            }
        }
    }
}
