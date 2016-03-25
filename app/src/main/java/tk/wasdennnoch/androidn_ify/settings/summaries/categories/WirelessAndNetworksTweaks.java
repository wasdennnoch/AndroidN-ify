package tk.wasdennnoch.androidn_ify.settings.summaries.categories;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;

import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.extracted.MobileDataController;

public class WirelessAndNetworksTweaks {

    public static void hookWifiTile(Object tile, Context context) {
        String summary = "Disabled";
        WifiManager manager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (manager.isWifiEnabled()) {
            //noinspection ResourceType
            WifiInfo wifiInfo = manager.getConnectionInfo();
            if (wifiInfo != null) {
                NetworkInfo.DetailedState state = WifiInfo.getDetailedStateOf(wifiInfo.getSupplicantState());
                if (state == NetworkInfo.DetailedState.CONNECTED || state == NetworkInfo.DetailedState.OBTAINING_IPADDR) {
                    summary = wifiInfo.getSSID();
                }
            } else {
                summary = "Disconnected";
            }
        }
        XposedHelpers.setObjectField(tile, "summary", summary);
    }

    public static void hookBluetoothTile(Object tile) {
        String summary = "Disabled";
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
                summary = Formatter.formatFileSize(context, info.usageLevel) + " of data used";
        } catch (Exception ignored) {
        }
        XposedHelpers.setObjectField(tile, "summary", summary);
    }

}
