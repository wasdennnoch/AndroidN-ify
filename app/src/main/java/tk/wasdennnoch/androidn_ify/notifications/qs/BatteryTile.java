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
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.systemui.SystemUIHooks;

public class BatteryTile extends QSTile implements BatteryInfoManager.BatteryStatusListener {

    private BatteryInfoManager.BatteryData mTileBatteryData;
    private BatteryView mBatteryView;

    public BatteryTile(TilesManager tilesManager, Object host, String key) {
        super(tilesManager, host, key);
        SystemUIHooks.batteryInfoManager.registerListener(this);
    }

    @Override
    public void handleUpdateState(Object state, Object arg) {
        mState.label = mResUtils.getString(R.string.battery);
        if (mTileBatteryData != null) {
            if (mTileBatteryData.charging) {
                if (mTileBatteryData.level == 100) {
                    mState.label = mResUtils.getString(R.string.charged);
                } else {
                    mState.label = String.format(mResUtils.getString(R.string.charging), mTileBatteryData.level + "%");
                }
            } else {
                mState.label = mTileBatteryData.level + "%";
            }
        }
        if (mBatteryView != null) {
            mBatteryView.postInvalidate();
        }
        super.handleUpdateState(state, arg);
    }

    @Override
    public View onCreateIcon() {
        FrameLayout iconFrame = new FrameLayout(mContext);
        FrameLayout.LayoutParams batteryLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        batteryLp.gravity = Gravity.CENTER;
        mBatteryView = new BatteryView(mContext);
        mBatteryView.setLayoutParams(batteryLp);
        iconFrame.addView(mBatteryView);
        return iconFrame;
    }

    @Override
    public void onBatteryStatusChanged(BatteryInfoManager.BatteryData batteryData) {
        mTileBatteryData = batteryData;
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
        mTileBatteryData = null;
        mBatteryView = null;
    }

    // A mix of GB BatteryView and AOSP BatteryMeterView
    public class BatteryView extends ImageView implements BatteryInfoManager.BatteryStatusListener {
        private final int[] LEVELS = new int[] { 4, 15, 100 };
        private final int[] COLORS = new int[] { 0xFFFF3300, 0xFFFF3300, 0xFFFFFFFF };
        private static final int BOLT_COLOR = 0xB2000000;
        private static final int FULL = 96;

        private static final float SUBPIXEL = 0.4f;  // inset rects for softer edges

        private static final float BOLT_LEVEL_THRESHOLD = 0.3f;  // opaque bolt below this fraction
        private final int mCriticalLevel;

        private int[] mColors;

        private Paint mFramePaint, mBatteryPaint, mWarningTextPaint, mTextPaint, mBoltPaint;
        private int mButtonHeight;
        private float mTextHeight, mWarningTextHeight;

        private int mHeight;
        private int mWidth;
        private String mWarningString;
        private int mChargeColor;
        private final float[] mBoltPoints;
        private final Path mBoltPath = new Path();

        private final RectF mFrame = new RectF();
        private final RectF mButtonFrame = new RectF();
        private final RectF mBoltFrame = new RectF();

        private final Path mShapePath = new Path();
        private final Path mClipPath = new Path();
        private final Path mTextPath = new Path();

        private BatteryInfoManager.BatteryData mBatteryData;
        private boolean mShowPercent = false;

        public BatteryView(Context context) {
            super(context);

            SystemUIHooks.batteryInfoManager.registerListener(this);

            mWarningString = "!";
            mCriticalLevel = mContext.getResources().getInteger(
                    com.android.internal.R.integer.config_criticalBatteryWarningLevel);

            mFramePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mFramePaint.setDither(true);
            mFramePaint.setStrokeWidth(0);
            mFramePaint.setStyle(Paint.Style.FILL_AND_STROKE);
            mFramePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_ATOP));

            mBatteryPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mBatteryPaint.setDither(true);
            mBatteryPaint.setStrokeWidth(0);
            mBatteryPaint.setStyle(Paint.Style.FILL_AND_STROKE);

            mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            Typeface font = Typeface.create("sans-serif-condensed", Typeface.BOLD);
            mTextPaint.setTypeface(font);
            mTextPaint.setTextAlign(Paint.Align.CENTER);

            mWarningTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            font = Typeface.create("sans-serif", Typeface.BOLD);
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

        public void setShowPercent(boolean showPercent) {
            mShowPercent = showPercent;
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

            int level = mBatteryData.level;

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

            mFramePaint.setColor(COLORS[COLORS.length-1]);
            mFramePaint.setAlpha(102);

            // set the battery charging color
            final int color = mBatteryData.charging ?
                    mChargeColor : getColorForLevel(mBatteryData.level);
            mBatteryPaint.setColor(color);

            if (level >= FULL) {
                drawFrac = 1f;
            } else if (level <= mCriticalLevel) {
                drawFrac = 0f;
            }

            final float levelTop = drawFrac == 1f ? mButtonFrame.top
                    : (mFrame.top + (mFrame.height() * (1f - drawFrac)));

            // define the battery shape
            mShapePath.reset();
            mShapePath.moveTo(mButtonFrame.left, mButtonFrame.top);
            mShapePath.lineTo(mButtonFrame.right, mButtonFrame.top);
            mShapePath.lineTo(mButtonFrame.right, mFrame.top);
            mShapePath.lineTo(mFrame.right, mFrame.top);
            mShapePath.lineTo(mFrame.right, mFrame.bottom);
            mShapePath.lineTo(mFrame.left, mFrame.bottom);
            mShapePath.lineTo(mFrame.left, mFrame.top);
            mShapePath.lineTo(mButtonFrame.left, mFrame.top);
            mShapePath.lineTo(mButtonFrame.left, mButtonFrame.top);

            if (mBatteryData.charging) {
                // define the bolt shape
                final float bl = mFrame.left + mFrame.width() / 4.5f;
                final float bt = mFrame.top + mFrame.height() / 6f;
                final float br = mFrame.right - mFrame.width() / 7f;
                final float bb = mFrame.bottom - mFrame.height() / 10f;
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

                float boltPct = (mBoltFrame.bottom - levelTop) / (mBoltFrame.bottom - mBoltFrame.top);
                boltPct = Math.min(Math.max(boltPct, 0), 1);
                if (boltPct <= BOLT_LEVEL_THRESHOLD) {
                    // draw the bolt if opaque
                    c.drawPath(mBoltPath, mBoltPaint);
                } else {
                    // otherwise cut the bolt out of the overall shape
                    mShapePath.op(mBoltPath, Path.Op.DIFFERENCE);
                }
            }

            // compute percentage text
            boolean pctOpaque = false;
            float pctX = 0, pctY = 0;
            String pctText = null;
            if (!mBatteryData.charging && level > mCriticalLevel && mShowPercent && level < 100) {
                mTextPaint.setColor(getColorForLevel(level));
                mTextPaint.setTextSize(height *
                        (mBatteryData.level == 100 ? 0.38f : 0.5f));
                mTextHeight = -mTextPaint.getFontMetrics().ascent;
                pctText = String.valueOf(level);
                pctX = mWidth * 0.5f;
                pctY = (mHeight + mTextHeight) * 0.47f;
                pctOpaque = levelTop > pctY;
                if (!pctOpaque) {
                    mTextPath.reset();
                    mTextPaint.getTextPath(pctText, 0, pctText.length(), pctX, pctY, mTextPath);
                    // cut the percentage text out of the overall shape
                    mShapePath.op(mTextPath, Path.Op.DIFFERENCE);
                }
            }

            // draw the battery shape background
            c.drawPath(mShapePath, mFramePaint);

            // draw the battery shape, clipped to charging level
            mFrame.top = levelTop;
            mClipPath.reset();
            mClipPath.addRect(mFrame,  Path.Direction.CCW);
            mShapePath.op(mClipPath, Path.Op.INTERSECT);
            c.drawPath(mShapePath, mBatteryPaint);

            if (!mBatteryData.charging) {
                if (level <= mCriticalLevel) {
                    // draw the warning text
                    final float x = mWidth * 0.5f;
                    final float y = (mHeight + mWarningTextHeight) * 0.48f;
                    c.drawText(mWarningString, x, y, mWarningTextPaint);
                } else if (pctOpaque) {
                    // draw the percentage text
                    c.drawText(pctText, pctX, pctY, mTextPaint);
                }
            }
        }

        @Override
        public void onBatteryStatusChanged(BatteryInfoManager.BatteryData batteryData) {
            mBatteryData = batteryData;
            postInvalidate();
        }
    }
}
