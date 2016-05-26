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
            Class<?> classQSUtils = XposedHelpers.findClass(QSTileHostHooks.CLASS_QS_UTILS, mContext.getClassLoader());
            for (String spec : availableTiles) {
                if (!(boolean) XposedHelpers.callStaticMethod(classQSUtils, "isStaticQsTile", spec)) {
                    availableTiles.remove(spec);
                }
            }
        }

        for (String spec : availableTiles) {
            addSpec(spec);
        }
    }

    private void addSpec(String spec) {
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
        if (RomUtils.isCmBased()) {
            res = (int) XposedHelpers.callStaticMethod(classQSTileHost, "getIconResource", spec);
        } else {
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
        if (spec.equals("wifi")) return res.getIdentifier("ic_qs_wifi_full_4", "drawable", PACKAGE_SYSTEMUI);
        else if (spec.equals("bt")) return res.getIdentifier("ic_qs_bluetooth_on", "drawable", PACKAGE_SYSTEMUI);
        else if (spec.equals("inversion")) return res.getIdentifier("ic_invert_colors_enable_animation", "drawable", PACKAGE_SYSTEMUI);
        else if (spec.equals("cell")) return res.getIdentifier("ic_qs_signal_full_4", "drawable", PACKAGE_SYSTEMUI);
        else if (spec.equals("airplane")) return res.getIdentifier("ic_signal_airplane_enable", "drawable", PACKAGE_SYSTEMUI);
        else if (spec.equals("dnd")) return res.getIdentifier("ic_dnd", "drawable", PACKAGE_SYSTEMUI);
        else if (spec.equals("rotation")) return res.getIdentifier("ic_portrait_from_auto_rotate", "drawable", PACKAGE_SYSTEMUI);
        else if (spec.equals("flashlight")) return res.getIdentifier("ic_signal_flashlight_enable", "drawable", PACKAGE_SYSTEMUI);
        else if (spec.equals("location")) return res.getIdentifier("ic_signal_location_enable", "drawable", PACKAGE_SYSTEMUI);
        else if (spec.equals("cast")) return res.getIdentifier("ic_qs_cast_on", "drawable", PACKAGE_SYSTEMUI);
        else if (spec.equals("hotspot")) return res.getIdentifier("ic_hotspot_enable", "drawable", PACKAGE_SYSTEMUI);
        return 0;
    }

    private String getQSTileLabel(String spec) {
        int resource = (int) XposedHelpers.callStaticMethod(classQSTileHost, "getLabelResource", spec);
        if (resource != 0) {
            return mContext.getText(resource).toString();
        } else {
            return spec;
        }
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
