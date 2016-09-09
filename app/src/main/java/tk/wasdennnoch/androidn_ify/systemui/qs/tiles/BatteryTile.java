package tk.wasdennnoch.androidn_ify.systemui.qs.tiles;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Looper;
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
import tk.wasdennnoch.androidn_ify.systemui.BatteryMeterDrawable;
import tk.wasdennnoch.androidn_ify.systemui.SystemUIHooks;
import tk.wasdennnoch.androidn_ify.systemui.notifications.StatusBarHeaderHooks;
import tk.wasdennnoch.androidn_ify.systemui.qs.DetailViewManager;
import tk.wasdennnoch.androidn_ify.systemui.qs.ResizingSpace;
import tk.wasdennnoch.androidn_ify.systemui.qs.TilesManager;
import tk.wasdennnoch.androidn_ify.systemui.qs.tiles.helper.BatteryInfoManager;
import tk.wasdennnoch.androidn_ify.utils.ConfigUtils;
import tk.wasdennnoch.androidn_ify.utils.ResourceUtils;

public class BatteryTile extends QSTile implements BatteryInfoManager.BatteryStatusListener {

    public static final String TILE_SPEC = "battery";
    private BatteryInfoManager.BatteryData mTileBatteryData;
    private BatteryView mBatteryView;
    private final BatteryDetail mDetail = new BatteryDetail();
    private final Object mBatteryDetail = DetailViewManager.getInstance().createProxy(mDetail);
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
            mState.label = mTileBatteryData.level + "%";
        }
        if (mBatteryView != null) {
            mBatteryView.postInvalidate();
        }
        super.handleUpdateState(state, arg);
    }

    @SuppressWarnings("deprecation")
    private BatteryMeterDrawable newDrawable() {
        return new BatteryMeterDrawable(mContext, new Handler(Looper.getMainLooper()),
                mContext.getResources().getColor(mContext.getResources().getIdentifier("batterymeter_frame_color", "color", XposedHook.PACKAGE_SYSTEMUI)));
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
        if (mDetailShown)
            mDetail.postBindView();
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
        super.setListening(listening);
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
        private final BatteryMeterDrawable mDrawable = newDrawable();

        public BatteryView(Context context) {
            super(context);
            mDrawable.setHasIntrinsicSize(false);
            mDrawable.setShowPercent(ConfigUtils.qs().battery_tile_show_percentage);
            setImageDrawable(mDrawable);

            int padding = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2,
                    getResources().getDisplayMetrics()));
            setPadding(0, padding, 0, padding);

            SystemUIHooks.batteryInfoManager.registerListener(this);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            final Resources res = getResources();
            int width = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 13, res.getDisplayMetrics()));
            setMeasuredDimension(width, MeasureSpec.getSize(heightMeasureSpec));
        }

        @Override
        public void onBatteryStatusChanged(BatteryInfoManager.BatteryData batteryData) {
            mDrawable.onBatteryStatusChanged(batteryData);
        }

        public void setShowPercent(boolean show) {
            mDrawable.setShowPercent(show);
        }

    }

    private final class BatteryDetail implements DetailViewManager.DetailAdapter, View.OnClickListener,
            View.OnAttachStateChangeListener, DetailViewManager.DetailViewAdapter {
        private final BatteryMeterDrawable mDrawable = newDrawable();
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
                //noinspection deprecation
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
            mDrawable.onBatteryLevelChanged(100, false);
            mDrawable.onPowerSaveChanged(true);
            mDrawable.disableShowPercent();
            ((ImageView) mCurrentView.findViewById(android.R.id.icon)).setImageDrawable(mDrawable);
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
