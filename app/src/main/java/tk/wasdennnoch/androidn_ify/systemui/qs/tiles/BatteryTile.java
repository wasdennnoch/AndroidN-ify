package tk.wasdennnoch.androidn_ify.systemui.qs.tiles;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Handler;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Checkable;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.android.internal.logging.MetricsLogger;

import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.extracted.settingslib.BatteryInfo;
import tk.wasdennnoch.androidn_ify.extracted.settingslib.UsageView;
import tk.wasdennnoch.androidn_ify.systemui.SystemUIHooks;
import tk.wasdennnoch.androidn_ify.systemui.notifications.StatusBarHeaderHooks;
import tk.wasdennnoch.androidn_ify.systemui.qs.DetailViewManager;
import tk.wasdennnoch.androidn_ify.systemui.qs.ResizingSpace;
import tk.wasdennnoch.androidn_ify.systemui.qs.TilesManager;
import tk.wasdennnoch.androidn_ify.systemui.qs.tiles.helper.BatteryInfoManager;
import tk.wasdennnoch.androidn_ify.utils.ResourceUtils;

public class BatteryTile extends QSTile implements BatteryInfoManager.BatteryStatusListener {

    public static final String TILE_SPEC = "battery";
    private BatteryInfoManager.BatteryData mTileBatteryData;
    private BatteryView mBatteryView;
    private final Object mBatteryDetail = DetailViewManager.getInstance().createProxy(new BatteryDetail());
    private boolean mListening;
    private boolean mDetailShown;

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
        if (mListening)
            refreshState();
    }

    @Override
    public void handleClick() {
        showDetail(true);
    }

    @Override
    public void handleLongClick() {
        startActivityDismissingKeyguard(Intent.ACTION_POWER_USAGE_SUMMARY);
    }

    @Override
    public Object getDetailAdapter() {
        return mBatteryDetail;
    }

    @Override
    public void setListening(boolean listening) {
        mListening = listening;
        if (mListening) {
            refreshState();
            mBatteryView.postInvalidate();
        }
    }

    @Override
    public void handleDestroy() {
        SystemUIHooks.batteryInfoManager.unregisterListener(this);
        SystemUIHooks.batteryInfoManager.unregisterListener(mBatteryView);
        mTileBatteryData = null;
        mBatteryView = null;
        super.handleDestroy();
    }

    // A mix of GB BatteryView and AOSP BatteryMeterView
    public class BatteryView extends ImageView implements BatteryInfoManager.BatteryStatusListener {
        private final int[] LEVELS = new int[]{4, 15, 100};
        private final int[] COLORS = new int[]{0xFFFF3300, 0xFFFF3300, 0xFFFFFFFF};
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
            mCriticalLevel = 4;

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
            COLORS[COLORS.length - 1] = mainColor;

            final int N = LEVELS.length;
            mColors = new int[2 * N];
            for (int i = 0; i < N; i++) {
                mColors[2 * i] = LEVELS[i];
                mColors[2 * i + 1] = COLORS[i];
            }

            mWarningTextPaint.setColor(COLORS[0]);
            mBatteryPaint.setColor(mainColor);
            mChargeColor = mainColor;
            invalidate();
        }

        public void setShowPercent(boolean showPercent) {
            mShowPercent = showPercent;
        }

        private float[] loadBoltPoints() {
            final int[] pts = new int[]{73, 0, 392, 0, 201, 259, 442, 259, 4, 703, 157, 334, 0, 334};
            int maxX = 0, maxY = 0;
            for (int i = 0; i < pts.length; i += 2) {
                maxX = Math.max(maxX, pts[i]);
                maxY = Math.max(maxY, pts[i + 1]);
            }
            final float[] ptsF = new float[pts.length];
            for (int i = 0; i < pts.length; i += 2) {
                ptsF[i] = (float) pts[i] / maxX;
                ptsF[i + 1] = (float) pts[i + 1] / maxY;
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
            for (int i = 0; i < mColors.length; i += 2) {
                thresh = mColors[i];
                color = mColors[i + 1];
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

            mFramePaint.setColor(0x4DFFFFFF);

            // set the battery charging color
            final int color = mBatteryData.charging ?
                    mChargeColor : getColorForLevel(mBatteryData.level);
            mBatteryPaint.setColor(color);
            mBoltPaint.setColor(color);

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

            boolean willDrawBolt = false;
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
                    // draw the bolt later
                    willDrawBolt = true;
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
            mClipPath.addRect(mFrame, Path.Direction.CCW);
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
            } else if(willDrawBolt) {
                // draw the bolt
                c.drawPath(mBoltPath, mBoltPaint);
            }
        }

        @Override
        public void onBatteryStatusChanged(BatteryInfoManager.BatteryData batteryData) {
            mBatteryData = batteryData;
            if (mListening)
                postInvalidate();
        }

    }

    private final class BatteryDetail implements DetailViewManager.DetailAdapter, View.OnClickListener,
            View.OnAttachStateChangeListener, DetailViewManager.DetailViewAdapter {
        private View mCurrentView;

        @Override
        public int getTitle() {
            return StatusBarHeaderHooks.R_string_battery_panel_title;
        }

        @Override
        public Boolean getToggleState() {
            return null;
        }

        @Override
        public DetailViewManager.DetailViewAdapter createDetailView(Context context, View convertView, ViewGroup parent) {
            if (convertView == null || !(convertView instanceof DetailViewManager.DetailFrameLayout)) {
                DetailViewManager.DetailFrameLayout frameLayout = new DetailViewManager.DetailFrameLayout(context, this);

                ResourceUtils res = ResourceUtils.getInstance(context);

                LinearLayout layout = (LinearLayout) LayoutInflater.from(ResourceUtils.createOwnContext(mContext)).inflate(R.layout.battery_detail, parent,
                        false);
                layout.addView(new ResizingSpace(context, 0, R.dimen.battery_detail_graph_space_top), 1);

                LinearLayout.LayoutParams usageViewLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, res.getDimensionPixelSize(R.dimen.battery_usage_height));
                usageViewLp.setMarginStart(res.getDimensionPixelSize(R.dimen.battery_usage_margin_start));
                usageViewLp.setMarginEnd(res.getDimensionPixelSize(R.dimen.battery_usage_margin_end));
                UsageView usageView = new UsageView(context, 0x66FFFFFF,
                        context.getResources().getColor(context.getResources().getIdentifier("system_accent_color", "color", XposedHook.PACKAGE_SYSTEMUI)));
                usageView.setId(R.id.battery_usage);
                usageView.setLayoutParams(usageViewLp);
                usageView.setSideLabels(res.getResources().getTextArray(R.array.battery_labels));
                layout.addView(usageView, 2);

                layout.addView(new ResizingSpace(context, 0, R.dimen.battery_detail_graph_space_bottom), 3);

                frameLayout.addView(layout);
                convertView = frameLayout;
            }
            mCurrentView = convertView;
            mCurrentView.addOnAttachStateChangeListener(this);
            bindView();
            return (DetailViewManager.DetailViewAdapter) convertView;
        }

        private void postBindView() {
            if (mCurrentView == null) return;
            mCurrentView.post(new Runnable() {
                @Override
                public void run() {
                    bindView();
                }
            });
        }

        private void bindView() {
            if (mCurrentView == null) {
                return;
            }
            /*
            mDrawable.onBatteryLevelChanged(100, false, false);
            mDrawable.onPowerSaveChanged(true);
            mDrawable.disableShowPercent();
            ((ImageView) mCurrentView.findViewById(android.R.id.icon)).setImageDrawable(mDrawable);
            */
            Checkable checkbox = (Checkable) mCurrentView.findViewById(android.R.id.toggle);
            checkbox.setChecked(SystemUIHooks.batteryInfoManager.isPowerSaveMode());
            BatteryInfo.getBatteryInfo(mContext, new BatteryInfo.Callback() {
                @Override
                public void onBatteryInfoLoaded(BatteryInfo info) {
                    if (mCurrentView != null) {
                        bindBatteryInfo(info);
                    }
                }
            });
            final TextView batterySaverTitle =
                    (TextView) mCurrentView.findViewById(android.R.id.title);
            final TextView batterySaverSummary =
                    (TextView) mCurrentView.findViewById(android.R.id.summary);
            if (mTileBatteryData.charging) {
                mCurrentView.findViewById(R.id.switch_container).setAlpha(.7f);
                batterySaverTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                batterySaverTitle.setText(mResUtils.getString(R.string.battery_detail_charging_summary));
                mCurrentView.findViewById(android.R.id.toggle).setVisibility(View.GONE);
                mCurrentView.findViewById(R.id.switch_container).setClickable(false);
            } else {
                mCurrentView.findViewById(R.id.switch_container).setAlpha(1);
                batterySaverTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                batterySaverTitle.setText(mResUtils.getString(R.string.battery_detail_switch_title));
                batterySaverSummary.setText(mResUtils.getString(R.string.battery_detail_switch_summary));
                mCurrentView.findViewById(android.R.id.toggle).setVisibility(View.VISIBLE);
                mCurrentView.findViewById(R.id.switch_container).setClickable(true);
                mCurrentView.findViewById(R.id.switch_container).setOnClickListener(this);
            }
        }

        private void bindBatteryInfo(BatteryInfo info) {
            SpannableStringBuilder builder = new SpannableStringBuilder();
            builder.append(info.batteryPercentString, new RelativeSizeSpan(2.6f),
                    Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            if (info.remainingLabel != null) {
                if (mResUtils.getResources().getBoolean(R.bool.quick_settings_wide)) {
                    builder.append(' ');
                } else {
                    builder.append('\n');
                }
                builder.append(info.remainingLabel);
            }
            ((TextView) mCurrentView.findViewById(R.id.charge_and_estimation)).setText(builder);

            info.bindHistory((UsageView) mCurrentView.findViewById(R.id.battery_usage));
        }

        @Override
        public void onClick(View v) {
            SystemUIHooks.batteryInfoManager.togglePowerSaving();
            ((Switch) mCurrentView.findViewById(android.R.id.toggle)).setChecked(SystemUIHooks.batteryInfoManager.isPowerSaveMode());
        }

        @Override
        public Intent getSettingsIntent() {
            return new Intent(Intent.ACTION_POWER_USAGE_SUMMARY);
        }

        @Override
        public void setToggleState(boolean state) {
            // No toggle state.
        }

        @Override
        public int getMetricsCategory() {
            return MetricsLogger.QS_INTENT;
        }

        @Override
        public boolean hasRightButton() {
            return false;
        }

        @Override
        public int getRightButtonResId() {
            return 0;
        }

        @Override
        public void handleRightButtonClick() {

        }

        @Override
        public void onViewAttachedToWindow(View v) {
            if (!mDetailShown) {
                mDetailShown = true;
                v.getContext().registerReceiver(mReceiver,
                        new IntentFilter(Intent.ACTION_TIME_TICK));
            }
        }

        @Override
        public void onViewDetachedFromWindow(View v) {
            if (mDetailShown) {
                mDetailShown = false;
                v.getContext().unregisterReceiver(mReceiver);
            }
        }

        private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                postBindView();
            }
        };
    }
}
