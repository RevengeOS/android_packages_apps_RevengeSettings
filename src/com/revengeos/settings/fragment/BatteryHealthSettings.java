/*
 * Copyright (C) 2020 RevengeOS
 *
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

package com.revengeos.settings.fragment;

import android.annotation.Nullable;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.Handler;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.util.Log;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.fuelgauge.PowerGaugePreference;
import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.SettingsPreferenceFragment;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.Integer;
import java.util.ArrayList;
import java.util.List;

/**
 * Settings screen for battery health
 */
public class BatteryHealthSettings extends SettingsPreferenceFragment implements OnPreferenceChangeListener {

    private static final String TAG = "BatteryHealthSettings";
    private static final String KEY_CURRENT_BATTERY_CAPACITY = "current_battery_capacity";
    private static final String KEY_DESIGNED_BATTERY_CAPACITY = "designed_battery_capacity";
    private static final String KEY_BATTERY_CURRENT = "battery_current";

    private static final String FILENAME_BATTERY_DESIGN_CAPACITY =
            "/sys/class/power_supply/bms/charge_full_design";
    private static final String FILENAME_BATTERY_CURRENT_CAPACITY =
            "/sys/class/power_supply/bms/charge_full";

    private static final String FILENAME_BATTERY_CURRENT = 
            "/sys/class/power_supply/bms/current_now";

    PowerGaugePreference mCurrentBatteryCapacity;
    PowerGaugePreference mDesignedBatteryCapacity;
    PowerGaugePreference mBatteryCurrent;

    Handler mHandler;
    PowerConnectionReceiver powerConnectionReceiver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.battery_health);

        mCurrentBatteryCapacity = (PowerGaugePreference) findPreference(
                KEY_CURRENT_BATTERY_CAPACITY);
        mDesignedBatteryCapacity = (PowerGaugePreference) findPreference(
                KEY_DESIGNED_BATTERY_CAPACITY);
        mBatteryCurrent= (PowerGaugePreference) findPreference(
                KEY_BATTERY_CURRENT);
        
        mCurrentBatteryCapacity.setSubtitle(parseBatteryCurrentData(FILENAME_BATTERY_CURRENT_CAPACITY, "mAh"));
        mDesignedBatteryCapacity.setSubtitle(parseBatteryCurrentData(FILENAME_BATTERY_DESIGN_CAPACITY, "mAh"));
    }
    
    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.REVENGEOS;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
        mHandler = new Handler();
        final Runnable r = new Runnable() {
            public void run() {
                mBatteryCurrent.setSubtitle(parseBatteryCurrentData(FILENAME_BATTERY_CURRENT, "mA"));
                mHandler.postDelayed(this, 1000);
            }
        };
        mHandler.postDelayed(r, 1000);

        powerConnectionReceiver = new PowerConnectionReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        getContext().registerReceiver(powerConnectionReceiver, filter);
    }

    @Override
    public void onPause() {
        super.onPause();
        mHandler.removeCallbacksAndMessages(null);
        getContext().unregisterReceiver(powerConnectionReceiver);
    }

    private String parseBatteryCurrentData(String file, String unit) {
        try {
            return Integer.parseInt(readLine(file)) / 1000 + unit;
        } catch (IOException ioe) {
            Log.e(TAG, "Cannot read battery data from "
                    + file, ioe);
        } catch (NumberFormatException nfe) {
            Log.e(TAG, "Read a badly formatted battery data from "
                    + file, nfe);
        }
        return getResources().getString(R.string.status_unavailable);
    }

    /**
    * Reads a line from the specified file.
    *
    * @param filename The file to read from.
    * @return The first line up to 256 characters, or <code>null</code> if file is empty.
    * @throws IOException If the file couldn't be read.
    */
    @Nullable
    private String readLine(String filename) throws IOException {
        final BufferedReader reader = new BufferedReader(new FileReader(filename), 256);
        try {
            return reader.readLine();
        } finally {
            reader.close();
        }
    }

    public class PowerConnectionReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_POWER_CONNECTED)) {
                mBatteryCurrent.setSummary(getResources().getString(R.string.battery_charging_rate));
            } else if (intent.getAction().equals(Intent.ACTION_POWER_DISCONNECTED)) {
                mBatteryCurrent.setSummary(getResources().getString(R.string.battery_discharging_rate));
            }
        }
    }
}
