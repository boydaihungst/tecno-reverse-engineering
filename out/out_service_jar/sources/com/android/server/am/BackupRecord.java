package com.android.server.am;

import android.content.pm.ApplicationInfo;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class BackupRecord {
    public static final int BACKUP_FULL = 1;
    public static final int BACKUP_NORMAL = 0;
    public static final int RESTORE = 2;
    public static final int RESTORE_FULL = 3;
    ProcessRecord app;
    final ApplicationInfo appInfo;
    final int backupMode;
    final int operationType;
    String stringName;
    final int userId;

    /* JADX INFO: Access modifiers changed from: package-private */
    public BackupRecord(ApplicationInfo _appInfo, int _backupMode, int _userId, int _operationType) {
        this.appInfo = _appInfo;
        this.backupMode = _backupMode;
        this.userId = _userId;
        this.operationType = _operationType;
    }

    public String toString() {
        String str = this.stringName;
        if (str != null) {
            return str;
        }
        StringBuilder sb = new StringBuilder(128);
        sb.append("BackupRecord{").append(Integer.toHexString(System.identityHashCode(this))).append(' ').append(this.appInfo.packageName).append(' ').append(this.appInfo.name).append(' ').append(this.appInfo.backupAgentName).append('}');
        String sb2 = sb.toString();
        this.stringName = sb2;
        return sb2;
    }
}
