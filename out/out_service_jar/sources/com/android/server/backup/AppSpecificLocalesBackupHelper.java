package com.android.server.backup;

import android.app.backup.BlobBackupHelper;
import android.util.Slog;
import com.android.server.LocalServices;
import com.android.server.locales.LocaleManagerInternal;
/* loaded from: classes.dex */
public class AppSpecificLocalesBackupHelper extends BlobBackupHelper {
    private static final int BLOB_VERSION = 1;
    private static final boolean DEBUG = false;
    private static final String KEY_APP_LOCALES = "app_locales";
    private static final String TAG = "AppLocalesBackupHelper";
    private final LocaleManagerInternal mLocaleManagerInternal;
    private final int mUserId;

    public AppSpecificLocalesBackupHelper(int userId) {
        super(1, new String[]{KEY_APP_LOCALES});
        this.mUserId = userId;
        this.mLocaleManagerInternal = (LocaleManagerInternal) LocalServices.getService(LocaleManagerInternal.class);
    }

    protected byte[] getBackupPayload(String key) {
        if (KEY_APP_LOCALES.equals(key)) {
            try {
                byte[] newPayload = this.mLocaleManagerInternal.getBackupPayload(this.mUserId);
                return newPayload;
            } catch (Exception e) {
                Slog.e(TAG, "Couldn't communicate with locale manager", e);
                return null;
            }
        }
        Slog.w(TAG, "Unexpected backup key " + key);
        return null;
    }

    protected void applyRestoredPayload(String key, byte[] payload) {
        if (KEY_APP_LOCALES.equals(key)) {
            try {
                this.mLocaleManagerInternal.stageAndApplyRestoredPayload(payload, this.mUserId);
                return;
            } catch (Exception e) {
                Slog.e(TAG, "Couldn't communicate with locale manager", e);
                return;
            }
        }
        Slog.w(TAG, "Unexpected restore key " + key);
    }
}
