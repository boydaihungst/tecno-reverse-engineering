package com.android.server.wifi;

import android.annotation.SystemApi;
import android.os.SystemService;
import android.util.Log;
import java.util.NoSuchElementException;
@SystemApi(client = SystemApi.Client.SYSTEM_SERVER)
/* loaded from: classes2.dex */
public class SupplicantManager {
    private static final String TAG = "SupplicantManager";
    private static final String WPA_SUPPLICANT_DAEMON_NAME = "wpa_supplicant";

    private SupplicantManager() {
    }

    public static void start() {
        try {
            SystemService.start(WPA_SUPPLICANT_DAEMON_NAME);
        } catch (RuntimeException e) {
            throw new NoSuchElementException("Failed to start Supplicant");
        }
    }

    public static void stop() {
        try {
            SystemService.stop(WPA_SUPPLICANT_DAEMON_NAME);
        } catch (RuntimeException e) {
            Log.w(TAG, "Failed to stop Supplicant", e);
        }
    }
}
