package com.android.server.notification;

import android.os.ParcelFileDescriptor;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import com.android.server.notification.NotificationManagerService;
import defpackage.CompanionAppsPermissions;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
/* loaded from: classes2.dex */
public class PulledStats {
    static final String TAG = "PulledStats";
    private long mTimePeriodEndMs;
    private final long mTimePeriodStartMs;
    private List<String> mUndecoratedPackageNames = new ArrayList();

    public PulledStats(long startMs) {
        this.mTimePeriodStartMs = startMs;
        this.mTimePeriodEndMs = startMs;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ParcelFileDescriptor toParcelFileDescriptor(final int report) throws IOException {
        final ParcelFileDescriptor[] fds = ParcelFileDescriptor.createPipe();
        switch (report) {
            case 1:
                Thread thr = new Thread("NotificationManager pulled metric output") { // from class: com.android.server.notification.PulledStats.1
                    @Override // java.lang.Thread, java.lang.Runnable
                    public void run() {
                        try {
                            FileOutputStream fout = new ParcelFileDescriptor.AutoCloseOutputStream(fds[1]);
                            ProtoOutputStream proto = new ProtoOutputStream(fout);
                            PulledStats.this.writeToProto(report, proto);
                            proto.flush();
                            fout.close();
                        } catch (IOException e) {
                            Slog.w(PulledStats.TAG, "Failure writing pipe", e);
                        }
                    }
                };
                thr.start();
                break;
            default:
                Slog.w(TAG, "Unknown pulled stats request: " + report);
                break;
        }
        return fds[0];
    }

    public long endTimeMs() {
        return this.mTimePeriodEndMs;
    }

    public void dump(int report, PrintWriter pw, NotificationManagerService.DumpFilter filter) {
        switch (report) {
            case 1:
                pw.print("  Packages with undecordated notifications (");
                pw.print(this.mTimePeriodStartMs);
                pw.print(" - ");
                pw.print(this.mTimePeriodEndMs);
                pw.println("):");
                if (this.mUndecoratedPackageNames.size() == 0) {
                    pw.println("    none");
                    return;
                }
                for (String pkg : this.mUndecoratedPackageNames) {
                    if (!filter.filtered || pkg.equals(filter.pkgFilter)) {
                        pw.println("    " + pkg);
                    }
                }
                return;
            default:
                pw.println("Unknown pulled stats request: " + report);
                return;
        }
    }

    void writeToProto(int report, ProtoOutputStream proto) {
        switch (report) {
            case 1:
                for (String pkg : this.mUndecoratedPackageNames) {
                    long token = proto.start(CompanionAppsPermissions.APP_PERMISSIONS);
                    proto.write(CompanionAppsPermissions.AppPermissions.PACKAGE_NAME, pkg);
                    proto.end(token);
                }
                return;
            default:
                Slog.w(TAG, "Unknown pulled stats request: " + report);
                return;
        }
    }

    public void addUndecoratedPackage(String packageName, long timestampMs) {
        this.mUndecoratedPackageNames.add(packageName);
        this.mTimePeriodEndMs = Math.max(this.mTimePeriodEndMs, timestampMs);
    }
}
