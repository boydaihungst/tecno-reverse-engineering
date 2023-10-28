package com.android.server.location.gnss;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class GnssSatelliteBlocklistHelper {
    private static final String BLOCKLIST_DELIMITER = ",";
    private static final String TAG = "GnssBlocklistHelper";
    private final GnssSatelliteBlocklistCallback mCallback;
    private final Context mContext;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public interface GnssSatelliteBlocklistCallback {
        void onUpdateSatelliteBlocklist(int[] iArr, int[] iArr2);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public GnssSatelliteBlocklistHelper(Context context, Looper looper, GnssSatelliteBlocklistCallback callback) {
        this.mContext = context;
        this.mCallback = callback;
        ContentObserver contentObserver = new ContentObserver(new Handler(looper)) { // from class: com.android.server.location.gnss.GnssSatelliteBlocklistHelper.1
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                GnssSatelliteBlocklistHelper.this.updateSatelliteBlocklist();
            }
        };
        context.getContentResolver().registerContentObserver(Settings.Global.getUriFor("gnss_satellite_blocklist"), true, contentObserver, -1);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateSatelliteBlocklist() {
        ContentResolver resolver = this.mContext.getContentResolver();
        String blocklist = Settings.Global.getString(resolver, "gnss_satellite_blocklist");
        if (blocklist == null) {
            blocklist = "";
        }
        Log.i(TAG, String.format("Update GNSS satellite blocklist: %s", blocklist));
        try {
            List<Integer> blocklistValues = parseSatelliteBlocklist(blocklist);
            if (blocklistValues.size() % 2 != 0) {
                Log.e(TAG, "blocklist string has odd number of values.Aborting updateSatelliteBlocklist");
                return;
            }
            int length = blocklistValues.size() / 2;
            int[] constellations = new int[length];
            int[] svids = new int[length];
            for (int i = 0; i < length; i++) {
                constellations[i] = blocklistValues.get(i * 2).intValue();
                svids[i] = blocklistValues.get((i * 2) + 1).intValue();
            }
            this.mCallback.onUpdateSatelliteBlocklist(constellations, svids);
        } catch (NumberFormatException e) {
            Log.e(TAG, "Exception thrown when parsing blocklist string.", e);
        }
    }

    static List<Integer> parseSatelliteBlocklist(String blocklist) throws NumberFormatException {
        String[] strings = blocklist.split(BLOCKLIST_DELIMITER);
        List<Integer> parsed = new ArrayList<>(strings.length);
        for (String string : strings) {
            String string2 = string.trim();
            if (!"".equals(string2)) {
                int value = Integer.parseInt(string2);
                if (value < 0) {
                    throw new NumberFormatException("Negative value is invalid.");
                }
                parsed.add(Integer.valueOf(value));
            }
        }
        return parsed;
    }
}
