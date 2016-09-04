package tk.wasdennnoch.androidn_ify.settings.summaries.categories;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
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
    private static BluetoothManager bluetoothManager;

    public static void hookWifiTile(Object tile, Context context) {
        String summary;
        WifiManager manager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (manager.isWifiEnabled()) {
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
        if (bluetoothAdapter.isEnabled()) {
            summary = bluetoothAdapter.getName(); // TODO this returns the devices name
            if (summary == null) {
                summary = bluetoothAdapter.getAddress();
            }
            try { //TODO Why isn't this working?
                String address = bluetoothAdapter.getAddress();
                BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
                int connectionState = bluetoothManager.getConnectionState(device, BluetoothProfile.GATT);
                if (connectionState == BluetoothGatt.STATE_CONNECTED) {
                    summary = "Connected"; //TODO Display the connected device name
                } else if (connectionState == BluetoothGatt.STATE_DISCONNECTED) {
                    summary = "Disconnected";
                } else if (connectionState == BluetoothGatt.STATE_CONNECTING) {
                    summary = "Connected";
                } else if (connectionState == BluetoothGatt.STATE_DISCONNECTING) {
                    summary = "Disconnected";
                }
            } catch (NullPointerException e) {
                XposedHook.logE(TAG, "Error hooking bluetooth tile", e);
            }
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
