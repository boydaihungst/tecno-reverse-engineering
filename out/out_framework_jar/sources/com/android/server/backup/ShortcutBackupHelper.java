package com.android.server.backup;

import android.app.backup.BlobBackupHelper;
import android.content.pm.IShortcutService;
import android.os.ServiceManager;
import android.util.Slog;
/* loaded from: classes4.dex */
public class ShortcutBackupHelper extends BlobBackupHelper {
    private static final int BLOB_VERSION = 1;
    private static final String KEY_USER_FILE = "shortcutuser.xml";
    private static final String TAG = "ShortcutBackupAgent";

    public ShortcutBackupHelper() {
        super(1, KEY_USER_FILE);
    }

    private IShortcutService getShortcutService() {
        return IShortcutService.Stub.asInterface(ServiceManager.getService("shortcut"));
    }

    @Override // android.app.backup.BlobBackupHelper
    protected byte[] getBackupPayload(String key) {
        char c;
        switch (key.hashCode()) {
            case -792920646:
                if (key.equals(KEY_USER_FILE)) {
                    c = 0;
                    break;
                }
            default:
                c = 65535;
                break;
        }
        switch (c) {
            case 0:
                try {
                    return getShortcutService().getBackupPayload(0);
                } catch (Exception e) {
                    Slog.wtf(TAG, "Backup failed", e);
                    return null;
                }
            default:
                Slog.w(TAG, "Unknown key: " + key);
                return null;
        }
    }

    @Override // android.app.backup.BlobBackupHelper
    protected void applyRestoredPayload(String key, byte[] payload) {
        char c;
        switch (key.hashCode()) {
            case -792920646:
                if (key.equals(KEY_USER_FILE)) {
                    c = 0;
                    break;
                }
            default:
                c = 65535;
                break;
        }
        switch (c) {
            case 0:
                try {
                    getShortcutService().applyRestore(payload, 0);
                    return;
                } catch (Exception e) {
                    Slog.wtf(TAG, "Restore failed", e);
                    return;
                }
            default:
                Slog.w(TAG, "Unknown key: " + key);
                return;
        }
    }
}
