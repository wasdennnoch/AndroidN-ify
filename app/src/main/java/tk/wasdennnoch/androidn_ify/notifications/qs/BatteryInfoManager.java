/*
 * Copyright (C) 2015 Peter Gregus for GravityBox Project (C3C076@xda)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tk.wasdennnoch.androidn_ify.notifications.qs;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.PowerManager;

import java.util.ArrayList;

public class BatteryInfoManager extends BroadcastReceiver {
    private BatteryData mBatteryData;
    private ArrayList<BatteryStatusListener> mListeners;

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
            updateBatteryInfo(intent);
        }
    }

    public class BatteryData {
        public boolean charging;
        public int level;
        public int powerSource;
        public int temperature;
        public int voltage;
        public boolean isPowerSaving;

        @SuppressWarnings("CloneDoesntCallSuperClone")
        public BatteryData clone() {
            BatteryData bd = new BatteryData();
            bd.charging = this.charging;
            bd.level = this.level;
            bd.powerSource = this.powerSource;
            bd.temperature = this.temperature;
            bd.voltage = this.voltage;
            bd.isPowerSaving = this.isPowerSaving;
            return bd;
        }

        public String toString() {
            return "charging="+this.charging+"; level="+this.level+
                    "; powerSource="+this.powerSource+
                    "; temperature="+this.temperature+
                    "; voltage="+this.voltage+
                    "; isPowerSaving="+this.isPowerSaving;

        }
    }

    public interface BatteryStatusListener {
        void onBatteryStatusChanged(BatteryData batteryData);
    }

    public BatteryInfoManager(Context context) {
        mBatteryData = new BatteryData();
        mListeners = new ArrayList<>();
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        mBatteryData.isPowerSaving = pm.isPowerSaveMode();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
        context.registerReceiver(this, intentFilter);
    }

    public void registerListener(BatteryStatusListener listener) {
        if (listener == null) return;
        synchronized(mListeners) {
            if (!mListeners.contains(listener)) {
                mListeners.add(listener);
                listener.onBatteryStatusChanged(mBatteryData);
            }
        }
    }

    public void unregisterListener(BatteryStatusListener listener) {
        if (listener == null) return;
        synchronized(mListeners) {
            if (mListeners.contains(listener)) {
                mListeners.remove(listener);
            }
        }
    }

    private void notifyListeners() {
        synchronized(mListeners) {
            for (BatteryStatusListener listener : mListeners) {
                listener.onBatteryStatusChanged(mBatteryData.clone());
            }
        }
    }

    private void updateBatteryInfo(Intent intent) {
        if (intent == null) return;

        int newLevel = (int)(100f
                * intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
                / intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100));
        int newPowerSource = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
        boolean newCharging = newPowerSource != 0;
        int newTemp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);
        int newVoltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0);

        if (mBatteryData.level != newLevel || mBatteryData.charging != newCharging ||
                mBatteryData.powerSource != newPowerSource ||
                mBatteryData.temperature != newTemp ||
                mBatteryData.voltage != newVoltage) {

            mBatteryData.level = newLevel;
            mBatteryData.charging = newCharging;
            mBatteryData.powerSource = newPowerSource;
            mBatteryData.temperature = newTemp;
            mBatteryData.voltage = newVoltage;

            notifyListeners();
        }
    }

    private void updatePowerSavingInfo(boolean enabled) {
        if (mBatteryData.isPowerSaving != enabled) {
            mBatteryData.isPowerSaving = enabled;
            notifyListeners();
        }
    }
}