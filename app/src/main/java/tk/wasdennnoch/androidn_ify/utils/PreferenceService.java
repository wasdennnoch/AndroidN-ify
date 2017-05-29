/*
 * Copyright (C) 2015 The Android Open Source Project
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
 * limitations under the License
 */
package tk.wasdennnoch.androidn_ify.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.ArrayMap;
import android.util.ArraySet;

import com.crossbowffs.remotepreferences.RemotePreferences;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import tk.wasdennnoch.androidn_ify.PreferenceProvider;
import tk.wasdennnoch.androidn_ify.XposedHook;

public class PreferenceService {

    private static final String TAG = "PreferenceService";
    private final Observer mObserver = new Observer();
    private final ArrayMap<Uri, String> mListeningUris = new ArrayMap<>();
    private final HashMap<String, Set<Tunable>> mTunableLookup = new HashMap<>();

    private ContentResolver mContentResolver;
    private Context mContext;
    private SharedPreferences mPrefs;
    private Uri mBaseUri;

    public void start() {
        mContentResolver = mContext.getContentResolver();
        mPrefs = new RemotePreferences(mContext, PreferenceProvider.AUTHORITY, PreferenceProvider.PREF_NAME);
        mBaseUri = Uri.parse("content://" + PreferenceProvider.AUTHORITY)
                .buildUpon().appendPath(PreferenceProvider.PREF_NAME).build();
    }

    public String getValue(String setting) {
        try {
            return mPrefs.getString(setting, null);
        } catch (ClassCastException e) {
            if (!mPrefs.contains(setting)) return null;
            try {
                return "" + mPrefs.getInt(setting, 0);
            } catch (ClassCastException e2) {
                return mPrefs.getBoolean(setting, false) ? "1" : "0";
            }
        }
    }

    public int getValue(String setting, int def) {
        return mPrefs.getInt(setting, def);
    }

    public void addTunable(Tunable tunable, String... keys) {
        for (String key : keys) {
            addTunable(tunable, key);
        }
    }

    private void addTunable(Tunable tunable, String key) {
        if (!mTunableLookup.containsKey(key)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mTunableLookup.put(key, new ArraySet<Tunable>());
            } else {
                mTunableLookup.put(key, new HashSet<Tunable>());
            }
        }
        mTunableLookup.get(key).add(tunable);
        Uri uri = mBaseUri.buildUpon().appendPath(key).build();
        if (!mListeningUris.containsKey(uri)) {
            XposedHook.logD(TAG, "registering observer for " + uri.toString());
            mListeningUris.put(uri, key);
            mContentResolver.registerContentObserver(uri, false, mObserver);
        }
        // Send the first state.
        String value = getValue(key);
        tunable.onTuningChanged(key, value);
    }

    public void removeTunable(Tunable tunable) {
        for (Set<Tunable> list : mTunableLookup.values()) {
            list.remove(tunable);
        }
    }

    protected void reregisterAll() {
        if (mListeningUris.size() == 0) {
            return;
        }
        mContentResolver.unregisterContentObserver(mObserver);
        for (Uri uri : mListeningUris.keySet()) {
            mContentResolver.registerContentObserver(uri, false, mObserver);
        }
    }

    public void reloadSetting(Uri uri) {
        String key = mListeningUris.get(uri);
        Set<Tunable> tunables = mTunableLookup.get(key);
        if (tunables == null) {
            return;
        }
        String value = getValue(key);
        for (Tunable tunable : tunables) {
            tunable.onTuningChanged(key, value);
        }
    }

    private void reloadAll() {
        for (String key : mTunableLookup.keySet()) {
            String value = getValue(key);
            for (Tunable tunable : mTunableLookup.get(key)) {
                tunable.onTuningChanged(key, value);
            }
        }
    }
    
    private static PreferenceService sInstance;

    public static PreferenceService get(Context context) {
        return getStaticService(context);
    }

    private static PreferenceService getStaticService(Context context) {
        if (sInstance == null) {
            sInstance = new PreferenceService();
            sInstance.mContext = context.getApplicationContext();
            sInstance.start();
        }
        return sInstance;
    }

    public static int parseInt(String string, int def) {
        try {
            return Integer.parseInt(string, 10);
        } catch (Throwable t) {
            return def;
        }
    }

    public static boolean parseBoolean(String string, boolean def) {
        if (string == null) return def;
        return !string.equals("0");
    }

    private class Observer extends ContentObserver {
        public Observer() {
            super(new Handler(Looper.getMainLooper()));
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            reloadSetting(uri);
        }
    }

    public interface Tunable {
        void onTuningChanged(String key, String newValue);
    }
}