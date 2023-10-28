package com.android.server.backup.utils;

import android.content.Context;
import android.content.IIntentReceiver;
import android.content.IIntentSender;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.pm.Signature;
import android.os.Bundle;
import android.os.IBinder;
import android.os.UserHandle;
import android.util.Slog;
import com.android.server.LocalServices;
import com.android.server.backup.BackupManagerService;
import com.android.server.backup.FileMetadata;
import com.android.server.backup.restore.RestoreDeleteObserver;
import com.android.server.backup.restore.RestorePolicy;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
/* loaded from: classes.dex */
public class RestoreUtils {
    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [180=4, 197=8, 93=4, 94=4] */
    /* JADX DEBUG: Failed to insert an additional move for type inference into block B:125:0x0274 */
    /* JADX DEBUG: Failed to insert an additional move for type inference into block B:224:0x0265 */
    /* JADX DEBUG: Failed to insert an additional move for type inference into block B:225:0x0128 */
    /* JADX DEBUG: Failed to insert an additional move for type inference into block B:84:0x01a0 */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Removed duplicated region for block: B:129:0x027c  */
    /* JADX WARN: Removed duplicated region for block: B:175:0x0267 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:187:0x02a3 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:222:0x02be A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Type inference failed for: r1v13 */
    /* JADX WARN: Type inference failed for: r1v14 */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public static boolean installApk(InputStream instream, Context context, RestoreDeleteObserver deleteObserver, HashMap<String, Signature[]> manifestSignatures, HashMap<String, RestorePolicy> packagePolicies, FileMetadata info, String installerPackageName, BytesReadListener bytesReadListener, int userId) {
        String str;
        PackageManager packageManager;
        PackageInstaller installer;
        PackageInstaller.SessionParams params;
        boolean okay;
        Throwable th;
        Throwable th2;
        boolean okay2;
        PackageInfo pkg;
        long toRead;
        String str2;
        boolean okay3 = true;
        String str3 = BackupManagerService.TAG;
        Slog.d(BackupManagerService.TAG, "Installing from backup: " + info.packageName);
        try {
            new LocalIntentReceiver();
            packageManager = context.getPackageManager();
            installer = packageManager.getPackageInstaller();
            params = new PackageInstaller.SessionParams(1);
        } catch (IOException e) {
        }
        try {
            params.setInstallerPackageName(installerPackageName);
            int sessionId = installer.createSession(params);
            try {
                PackageInstaller.Session session = installer.openSession(sessionId);
                try {
                    OutputStream apkStream = session.openWrite(info.packageName, 0L, info.size);
                    try {
                        byte[] buffer = new byte[32768];
                        long size = info.size;
                        while (true) {
                            okay = okay3;
                            if (size <= 0) {
                                break;
                            }
                            try {
                                if (buffer.length < size) {
                                    try {
                                        toRead = buffer.length;
                                    } catch (Throwable th3) {
                                        th = th3;
                                        th2 = th;
                                        if (apkStream != null) {
                                            try {
                                                apkStream.close();
                                            }
                                        }
                                        throw th2;
                                    }
                                } else {
                                    toRead = size;
                                }
                                int didRead = instream.read(buffer, 0, (int) toRead);
                                if (didRead >= 0) {
                                    str2 = str3;
                                    try {
                                        bytesReadListener.onBytesRead(didRead);
                                    } catch (Throwable th4) {
                                        th2 = th4;
                                        if (apkStream != null) {
                                        }
                                        throw th2;
                                    }
                                } else {
                                    str2 = str3;
                                }
                                apkStream.write(buffer, 0, didRead);
                                size -= didRead;
                                str3 = str2;
                                okay3 = okay;
                            } catch (Throwable th5) {
                                th = th5;
                            }
                        }
                        str = bytesReadListener;
                        String str4 = str3;
                        if (apkStream != null) {
                            try {
                                apkStream.close();
                            } catch (Throwable th6) {
                                th = th6;
                                if (session != null) {
                                    try {
                                        session.close();
                                    }
                                }
                                throw th;
                            }
                        }
                        try {
                            session.abandon();
                            if (session != null) {
                                try {
                                    session.close();
                                } catch (Exception e2) {
                                    t = e2;
                                    str = str4;
                                    try {
                                        installer.abandonSession(sessionId);
                                        throw t;
                                    } catch (IOException e3) {
                                        Slog.e(str, "Unable to transcribe restored apk for install");
                                        return false;
                                    }
                                }
                            }
                            Intent result = null;
                            if (1 != 0) {
                                try {
                                    if (packagePolicies.get(info.packageName) != RestorePolicy.ACCEPT) {
                                        return false;
                                    }
                                    return okay;
                                } catch (IOException e4) {
                                    str = str4;
                                    Slog.e(str, "Unable to transcribe restored apk for install");
                                    return false;
                                }
                            }
                            boolean uninstall = false;
                            try {
                                String installedPackageName = result.getStringExtra("android.content.pm.extra.PACKAGE_NAME");
                                try {
                                    if (installedPackageName.equals(info.packageName)) {
                                        str = str4;
                                        try {
                                            try {
                                                pkg = packageManager.getPackageInfoAsUser(info.packageName, 134217728, userId);
                                            } catch (IOException e5) {
                                                Slog.e(str, "Unable to transcribe restored apk for install");
                                                return false;
                                            }
                                        } catch (PackageManager.NameNotFoundException e6) {
                                        }
                                        if ((pkg.applicationInfo.flags & 32768) == 0) {
                                            try {
                                                try {
                                                    Slog.w(str, "Restore stream contains apk of package " + info.packageName + " but it disallows backup/restore");
                                                    okay2 = false;
                                                } catch (PackageManager.NameNotFoundException e7) {
                                                }
                                            } catch (PackageManager.NameNotFoundException e8) {
                                            }
                                        } else {
                                            try {
                                                try {
                                                    Signature[] sigs = manifestSignatures.get(info.packageName);
                                                    PackageManagerInternal pmi = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
                                                    try {
                                                        BackupEligibilityRules eligibilityRules = BackupEligibilityRules.forBackup(packageManager, pmi, userId);
                                                        if (!eligibilityRules.signaturesMatch(sigs, pkg)) {
                                                            Slog.w(str, "Installed app " + info.packageName + " signatures do not match restore manifest");
                                                            okay2 = false;
                                                            uninstall = true;
                                                        } else if (UserHandle.isCore(pkg.applicationInfo.uid) && pkg.applicationInfo.backupAgentName == null) {
                                                            Slog.w(str, "Installed app " + info.packageName + " has restricted uid and no agent");
                                                            okay2 = false;
                                                        } else {
                                                            okay2 = okay;
                                                        }
                                                    } catch (PackageManager.NameNotFoundException e9) {
                                                    }
                                                } catch (PackageManager.NameNotFoundException e10) {
                                                    Slog.w(str, "Install of package " + info.packageName + " succeeded but now not found");
                                                    okay2 = false;
                                                    if (uninstall) {
                                                    }
                                                }
                                                if (uninstall) {
                                                    return okay2;
                                                }
                                                try {
                                                    deleteObserver.reset();
                                                    try {
                                                        packageManager.deletePackage(installedPackageName, deleteObserver, 0);
                                                        deleteObserver.waitForCompletion();
                                                        return okay2;
                                                    } catch (IOException e11) {
                                                    }
                                                } catch (IOException e12) {
                                                }
                                            } catch (IOException e13) {
                                                Slog.e(str, "Unable to transcribe restored apk for install");
                                                return false;
                                            }
                                        }
                                        Slog.w(str, "Install of package " + info.packageName + " succeeded but now not found");
                                        okay2 = false;
                                        if (uninstall) {
                                        }
                                    } else {
                                        try {
                                            str = str4;
                                            Slog.w(str, "Restore stream claimed to include apk for " + info.packageName + " but apk was really " + installedPackageName);
                                            uninstall = true;
                                            okay2 = false;
                                        } catch (IOException e14) {
                                            str = str4;
                                            str = str;
                                            Slog.e(str, "Unable to transcribe restored apk for install");
                                            return false;
                                        }
                                    }
                                    if (uninstall) {
                                    }
                                } catch (IOException e15) {
                                }
                            } catch (IOException e16) {
                                str = str4;
                            }
                        } catch (Throwable th7) {
                            th = th7;
                            if (session != null) {
                            }
                            throw th;
                        }
                    } catch (Throwable th8) {
                        th2 = th8;
                    }
                } catch (Throwable th9) {
                    th = th9;
                }
            } catch (Exception e17) {
                t = e17;
                okay = true;
                str = BackupManagerService.TAG;
            }
        } catch (IOException e18) {
            str = BackupManagerService.TAG;
            Slog.e(str, "Unable to transcribe restored apk for install");
            return false;
        }
        Slog.e(str, "Unable to transcribe restored apk for install");
        return false;
    }

    /* loaded from: classes.dex */
    private static class LocalIntentReceiver {
        private IIntentSender.Stub mLocalSender;
        private final Object mLock;
        private Intent mResult;

        private LocalIntentReceiver() {
            this.mLock = new Object();
            this.mResult = null;
            this.mLocalSender = new IIntentSender.Stub() { // from class: com.android.server.backup.utils.RestoreUtils.LocalIntentReceiver.1
                public void send(int code, Intent intent, String resolvedType, IBinder whitelistToken, IIntentReceiver finishedReceiver, String requiredPermission, Bundle options) {
                    synchronized (LocalIntentReceiver.this.mLock) {
                        LocalIntentReceiver.this.mResult = intent;
                        LocalIntentReceiver.this.mLock.notifyAll();
                    }
                }
            };
        }

        public IntentSender getIntentSender() {
            return new IntentSender(this.mLocalSender);
        }

        public Intent getResult() {
            Intent intent;
            synchronized (this.mLock) {
                while (true) {
                    intent = this.mResult;
                    if (intent == null) {
                        try {
                            this.mLock.wait();
                        } catch (InterruptedException e) {
                        }
                    }
                }
            }
            return intent;
        }
    }
}
