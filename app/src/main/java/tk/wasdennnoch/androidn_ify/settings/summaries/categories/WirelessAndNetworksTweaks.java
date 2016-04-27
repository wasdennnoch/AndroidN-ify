package tk.wasdennnoch.androidn_ify.settings.summaries.categories;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiSsid;
import android.text.format.Formatter;

import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.extracted.systemui.MobileDataController;
import tk.wasdennnoch.androidn_ify.utils.ResourceUtils;

public class WirelessAndNetworksTweaks {

    private static final String TAG = "WirelessAndNetworksTweaks";

    public static void hookWifiTile(Object tile, Context context) {
        String summary;
        WifiManager manager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (manager.isWifiEnabled()) {
            //noinspection ResourceType
            WifiInfo wifiInfo = manager.getConnectionInfo();
            if (wifiInfo != null) {
                NetworkInfo.DetailedState state = WifiInfo.getDetailedStateOf(wifiInfo.getSupplicantState());
                if (state == NetworkInfo.DetailedState.CONNECTED || state == NetworkInfo.DetailedState.OBTAINING_IPADDR) {
                    String ssid = wifiInfo.getSSID();
                    if (!ssid.equals(WifiSsid.NONE)) {
                        summary = ssid;
                    } else {
                        summary = ResourceUtils.getInstance().getString(R.string.disconnected);
                    }
                } else {
                    summary = ResourceUtils.getInstance().getString(R.string.disconnected);
                }
            } else {
                summary = ResourceUtils.getInstance().getString(R.string.disconnected);
            }
        } else {
            summary = ResourceUtils.getInstance().getString(R.string.disabled);
        }
        XposedHelpers.setObjectField(tile, "summary", summary);
    }

    public static void hookBluetoothTile(Object tile) {
        String summary = ResourceUtils.getInstance().getString(R.string.disabled);
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        //noinspection ResourceType
        if (bluetoothAdapter.isEnabled()) {
            // TODO check if disconnected (not the same as disabled)
            summary = bluetoothAdapter.getName(); // TODO this returns the devices name
            if (summary == null)
                //noinspection ResourceType
                summary = bluetoothAdapter.getAddress();
        }
        XposedHelpers.setObjectField(tile, "summary", summary);
    }

    public static void hookDataUsageTile(Object tile, Context context) {
        String summary = null;
        try {
            MobileDataController controller = new MobileDataController(context);
            MobileDataController.DataUsageInfo info = controller.getDataUsageInfo();
            if (info != null)
                summary = ResourceUtils.getInstance().getString(R.string.data_usage_summary, Formatter.formatFileSize(context, info.usageLevel));
        } catch (Exception e) {
            XposedHook.logE(TAG, "Error hooking data usage tile", e);
        }
        XposedHelpers.setObjectField(tile, "summary", summary);
    }

}
