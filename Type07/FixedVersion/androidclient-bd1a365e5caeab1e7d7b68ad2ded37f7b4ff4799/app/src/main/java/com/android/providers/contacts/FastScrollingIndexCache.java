/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.providers.contacts;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import org.kontalk.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.kontalk.provider.MyUsers;

/**
 * Cache for the "fast scrolling index".
 *
 * It's a cache from "keys" and "bundles" (see {@link #mCache} for what they are).  The cache
 * content is also persisted in the shared preferences, so it'll survive even if the process
 * is killed or the device reboots.
 *
 * All the content will be invalidated when the provider detects an operation that could potentially
 * change the index.
 *
 * There's no maximum number for cached entries.  It's okay because we store keys and values in
 * a compact form in both the in-memory cache and the preferences.  Also the query in question
 * (the query for contact lists) has relatively low number of variations.
 *
 * This class is thread-safe.
 */
public class FastScrollingIndexCache {
    private static final String TAG = "LetterCountCache";

    private static final String PREFERENCE_KEY = "LetterCountCache";

    /**
     * Separator used for in-memory structure.
     */
    private static final String SEPARATOR = "\u0001";
    private static final Pattern SEPARATOR_PATTERN = Pattern.compile(SEPARATOR);

    /**
     * Separator used for serializing values for preferences.
     */
    private static final String SAVE_SEPARATOR = "\u0002";
    private static final Pattern SAVE_SEPARATOR_PATTERN = Pattern.compile(SAVE_SEPARATOR);

    private final SharedPreferences mPrefs;

    private boolean mPreferenceLoaded;

    /**
     * In-memory cache.
     *
     * It's essentially a map from keys, which are query parameters passed to {@link #get}, to
     * values, which are {@link Bundle}s that will be appended to a {@link Cursor} as extras.
     *
     * However, in order to save memory, we store stringified keys and values in the cache.
     * Key strings are generated by {@link #buildCacheKey} and values are generated by
     * {@link #buildCacheValue}.
     *
     * We store those strings joined with {@link #SAVE_SEPARATOR} as the separator when saving
     * to shared preferences.
     */
    private final Map<String, String> mCache = new HashMap<>();

    private static FastScrollingIndexCache sSingleton;

    public static FastScrollingIndexCache getInstance(Context context) {
        if (sSingleton == null) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            sSingleton = new FastScrollingIndexCache(prefs);
        }
        return sSingleton;
    }

    private FastScrollingIndexCache(SharedPreferences prefs) {
        mPrefs = prefs;
    }

    /**
     * Append a {@link String} to a {@link StringBuilder}.
     *
     * Unlike the original {@link StringBuilder#append}, it does *not* append the string "null" if
     * {@code value} is null.
     */
    private static void appendIfNotNull(StringBuilder sb, Object value) {
        if (value != null) {
            sb.append(value.toString());
        }
    }

    private static String buildCacheKey(Uri queryUri, String selection, String[] selectionArgs,
            String sortOrder, String countExpression) {
        final StringBuilder sb = new StringBuilder();

        appendIfNotNull(sb, queryUri);
        appendIfNotNull(sb, SEPARATOR);
        appendIfNotNull(sb, selection);
        appendIfNotNull(sb, SEPARATOR);
        appendIfNotNull(sb, sortOrder);
        appendIfNotNull(sb, SEPARATOR);
        appendIfNotNull(sb, countExpression);

        if (selectionArgs != null) {
            for (String arg : selectionArgs) {
                appendIfNotNull(sb, SEPARATOR);
                appendIfNotNull(sb, arg);
            }
        }
        return sb.toString();
    }

    private static String buildCacheValue(String[] titles, int[] counts) {
        final StringBuilder sb = new StringBuilder();

        for (int i = 0; i < titles.length; i++) {
            if (i > 0) {
                appendIfNotNull(sb, SEPARATOR);
            }
            appendIfNotNull(sb, titles[i]);
            appendIfNotNull(sb, SEPARATOR);
            appendIfNotNull(sb, Integer.toString(counts[i]));
        }

        return sb.toString();
    }

    /**
     * Creates and returns a {@link Bundle} that is appended to a {@link Cursor} as extras.
     */
    public static Bundle buildExtraBundle(String[] titles, int[] counts) {
        Bundle bundle = new Bundle();
        bundle.putStringArray(MyUsers.Users.EXTRA_INDEX_TITLES, titles);
        bundle.putIntArray(MyUsers.Users.EXTRA_INDEX_COUNTS, counts);
        return bundle;
    }

    private static Bundle buildExtraBundleFromValue(String value) {
        final String[] values;
        if (TextUtils.isEmpty(value)) {
            values = new String[0];
        } else {
            values = SEPARATOR_PATTERN.split(value);
        }

        if ((values.length) % 2 != 0) {
            return null; // malformed
        }

        try {
            final int numTitles = values.length / 2;
            final String[] titles = new String[numTitles];
            final int[] counts = new int[numTitles];

            for (int i = 0; i < numTitles; i++) {
                titles[i] = values[i * 2];
                counts[i] = Integer.parseInt(values[i * 2 + 1]);
            }

            return buildExtraBundle(titles, counts);
        } catch (RuntimeException e) {
            Log.w(TAG, "Failed to parse cached value", e);
            return null; // malformed
        }
    }

    public Bundle get(Uri queryUri, String selection, String[] selectionArgs, String sortOrder,
            String countExpression) {
        synchronized (mCache) {
            ensureLoaded();
            final String key = buildCacheKey(queryUri, selection, selectionArgs, sortOrder,
                    countExpression);
            final String value = mCache.get(key);
            if (value == null) {
                return null;
            }

            final Bundle b = buildExtraBundleFromValue(value);
            if (b == null) {
                // Value was malformed for whatever reason.
                mCache.remove(key);
                save();
            }
            return b;
        }
    }

    /**
     * Put a {@link Bundle} into the cache.  {@link Bundle} MUST be built with
     * {@link #buildExtraBundle(String[], int[])}.
     */
    public void put(Uri queryUri, String selection, String[] selectionArgs, String sortOrder,
            String countExpression, Bundle bundle) {
        synchronized (mCache) {
            ensureLoaded();
            final String key = buildCacheKey(queryUri, selection, selectionArgs, sortOrder,
                    countExpression);
            mCache.put(key, buildCacheValue(
                    bundle.getStringArray(MyUsers.Users.EXTRA_INDEX_TITLES),
                    bundle.getIntArray(MyUsers.Users.EXTRA_INDEX_COUNTS)));
            save();
        }
    }

    @SuppressLint("CommitPrefEdits")
    public void invalidate() {
        synchronized (mCache) {
            mPrefs.edit().remove(PREFERENCE_KEY).commit();
            mCache.clear();
            mPreferenceLoaded = true;
        }
    }

    /**
     * Store the cache to the preferences.
     *
     * We concatenate all key+value pairs into one string and save it.
     */
    private void save() {
        final StringBuilder sb = new StringBuilder();
        for (String key : mCache.keySet()) {
            if (sb.length() > 0) {
                appendIfNotNull(sb, SAVE_SEPARATOR);
            }
            appendIfNotNull(sb, key);
            appendIfNotNull(sb, SAVE_SEPARATOR);
            appendIfNotNull(sb, mCache.get(key));
        }
        mPrefs.edit().putString(PREFERENCE_KEY, sb.toString()).apply();
    }

    private void ensureLoaded() {
        if (mPreferenceLoaded) return;

        // Even when we fail to load, don't retry loading again.
        mPreferenceLoaded = true;

        boolean successfullyLoaded = false;
        try {
            final String savedValue = mPrefs.getString(PREFERENCE_KEY, null);

            if (!TextUtils.isEmpty(savedValue)) {

                final String[] keysAndValues = SAVE_SEPARATOR_PATTERN.split(savedValue);

                if ((keysAndValues.length % 2) != 0) {
                    return; // malformed
                }

                for (int i = 1; i < keysAndValues.length; i += 2) {
                    final String key = keysAndValues[i - 1];
                    final String value = keysAndValues[i];

                    mCache.put(key, value);
                }
            }
            successfullyLoaded = true;
        } catch (RuntimeException e) {
            Log.w(TAG, "Failed to load from preferences", e);
            // But don't crash apps!
        } finally {
            if (!successfullyLoaded) {
                invalidate();
            }
        }
    }
}
