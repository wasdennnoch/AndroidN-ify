package tk.wasdennnoch.androidn_ify.notifications.qs;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.notifications.StatusBarHeaderHooks;
import tk.wasdennnoch.androidn_ify.utils.RomUtils;

public class AvailableTileAdapter extends TileAdapter {

    private Class<?> classQSTileHost;
    private Class<?> classResourceIcon;

    public AvailableTileAdapter(ArrayList<Object> records, Context context, ViewGroup qsPanel) {
        super(context, qsPanel);

        mRecords = new ArrayList<>();

        classQSTileHost = XposedHelpers.findClass(QSTileHostHooks.CLASS_TILE_HOST, mContext.getClassLoader());
        classResourceIcon = XposedHelpers.findClass(QSTile.CLASS_QS_TILE + "$ResourceIcon", mContext.getClassLoader());

        mTileViews = new ArrayList<>();
        List<String> availableTiles = QSTileHostHooks.getAvailableTiles(mContext);

        for (int i = 0; i < records.size(); i++) {
            Object tilerecord = records.get(i);
            Object tile = XposedHelpers.getObjectField(tilerecord, "tile");
            String spec = (String) XposedHelpers.getAdditionalInstanceField(tile, QSTileHostHooks.TILE_SPEC_NAME);
            availableTiles.remove(spec);
        }

        if (RomUtils.isCmBased()) {
            try {
                Class<?> classQSUtils = XposedHelpers.findClass(QSTileHostHooks.CLASS_QS_UTILS, mContext.getClassLoader());
                for (String spec : availableTiles) {
                    if (!(boolean) XposedHelpers.callStaticMethod(classQSUtils, "isStaticQsTile", spec)) {
                        availableTiles.remove(spec);
                    }
                }
            } catch (Throwable ignore) {
                // TODO crashing although the CMSDK is clearly there?
                // Catching is better than crashing the system server (for some reason...)
            }
        }

        for (String spec : availableTiles) {
            addSpec(spec);
        }
    }

    private void addSpec(String spec) {
        if (spec == null) return;

        RelativeLayout.LayoutParams tileViewLp = new RelativeLayout.LayoutParams(mCellWidth, mCellHeight);
        tileViewLp.addRule(RelativeLayout.CENTER_IN_PARENT);

        FakeQSTileView tileView = new FakeQSTileView(mContext);
        tileView.setLayoutParams(tileViewLp);
        tileView.handleStateChanged(getQSTileIcon(spec), getQSTileLabel(spec));
        mTileViews.add(tileView);
        mRecords.add(spec);
    }

    private Drawable getQSTileIcon(String spec) {
        int res;
        try {
            res = (int) XposedHelpers.callStaticMethod(classQSTileHost, "getIconResource", spec);
        } catch (Throwable ignore) {
            res = getIconResourceAosp(spec);
        }
        if (res != 0) {
            Object icon = XposedHelpers.callStaticMethod(classResourceIcon, "get", res);
            return (Drawable) XposedHelpers.callMethod(icon, "getDrawable", mContext);
        } else {
            return mContext.getPackageManager().getDefaultActivityIcon();
        }
    }

    private int getIconResourceAosp(String spec) {
        Resources res = mContext.getResources();
        switch (spec) {
            case "wifi":
                return res.getIdentifier("ic_qs_wifi_full_4", "drawable", PACKAGE_SYSTEMUI);
            case "bt":
                return res.getIdentifier("ic_qs_bluetooth_on", "drawable", PACKAGE_SYSTEMUI);
            case "inversion":
                return res.getIdentifier("ic_invert_colors_enable_animation", "drawable", PACKAGE_SYSTEMUI);
            case "cell":
                return res.getIdentifier("ic_qs_signal_full_4", "drawable", PACKAGE_SYSTEMUI);
            case "airplane":
                return res.getIdentifier("ic_signal_airplane_enable", "drawable", PACKAGE_SYSTEMUI);
            case "dnd":
                return res.getIdentifier("ic_dnd", "drawable", PACKAGE_SYSTEMUI);
            case "rotation":
                return res.getIdentifier("ic_portrait_from_auto_rotate", "drawable", PACKAGE_SYSTEMUI);
            case "flashlight":
                return res.getIdentifier("ic_signal_flashlight_enable", "drawable", PACKAGE_SYSTEMUI);
            case "location":
                return res.getIdentifier("ic_signal_location_enable", "drawable", PACKAGE_SYSTEMUI);
            case "cast":
                return res.getIdentifier("ic_qs_cast_on", "drawable", PACKAGE_SYSTEMUI);
            case "hotspot":
                return res.getIdentifier("ic_hotspot_enable", "drawable", PACKAGE_SYSTEMUI);
        }
        return 0;
    }

    private String getQSTileLabel(String spec) {
        int resource;
        try {
            resource = (int) XposedHelpers.callStaticMethod(classQSTileHost, "getLabelResource", spec);
        } catch (Throwable t) {
            resource = getQSTileLabelAosp(spec);
        }
        if (resource != 0) {
            return mContext.getText(resource).toString();
        } else {
            return spec;
        }
    }

    private int getQSTileLabelAosp(String spec) {
        Resources res = mContext.getResources();
        switch (spec) {
            case "wifi":
                return res.getIdentifier("quick_settings_wifi_label", "string", PACKAGE_SYSTEMUI);
            case "bt":
                return res.getIdentifier("quick_settings_bluetooth_label", "string", PACKAGE_SYSTEMUI);
            case "inversion":
                return res.getIdentifier("quick_settings_inversion_label", "string", PACKAGE_SYSTEMUI);
            case "cell":
                return res.getIdentifier("quick_settings_cellular_detail_title", "string", PACKAGE_SYSTEMUI);
            case "airplane":
                return res.getIdentifier("airplane_mode", "string", PACKAGE_SYSTEMUI);
            case "dnd":
                return res.getIdentifier("quick_settings_dnd_label", "string", PACKAGE_SYSTEMUI);
            case "rotation":
                return res.getIdentifier("quick_settings_rotation_locked_label", "string", PACKAGE_SYSTEMUI);
            case "flashlight":
                return res.getIdentifier("quick_settings_flashlight_label", "string", PACKAGE_SYSTEMUI);
            case "location":
                return res.getIdentifier("quick_settings_location_label", "string", PACKAGE_SYSTEMUI);
            case "cast":
                return res.getIdentifier("quick_settings_cast_title", "string", PACKAGE_SYSTEMUI);
            case "hotspot":
                return res.getIdentifier("quick_settings_hotspot_label", "string", PACKAGE_SYSTEMUI);
        }
        return 0;
    }

    @Override
    public void onItemClick(int position) {
        Record r = new TileAdapter.Record();
        r.spec = (String) mRecords.get(position);
        StatusBarHeaderHooks.mTileAdapter.addRecord(r);

        mRecords.remove(position);
        mTileViews.remove(position);
        notifyItemRemoved(position);
    }

    public void addAdditionalSpec(String spec) {
        int addPosition = mTileViews.size();
        addSpec(spec);
        notifyItemInserted(addPosition);
    }
}
