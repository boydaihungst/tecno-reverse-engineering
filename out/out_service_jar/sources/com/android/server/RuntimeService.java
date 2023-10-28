package com.android.server;

import android.content.Context;
import android.os.Binder;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import com.android.i18n.timezone.DebugInfo;
import com.android.i18n.timezone.I18nModuleDebug;
import com.android.i18n.timezone.TimeZoneDataFiles;
import com.android.internal.util.DumpUtils;
import com.android.timezone.distro.DistroException;
import com.android.timezone.distro.DistroVersion;
import com.android.timezone.distro.FileUtils;
import com.android.timezone.distro.TimeZoneDistro;
import defpackage.CompanionAppsPermissions;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
/* loaded from: classes.dex */
public class RuntimeService extends Binder {
    private static final String TAG = "RuntimeService";
    private final Context mContext;

    public RuntimeService(Context context) {
        this.mContext = context;
    }

    @Override // android.os.Binder
    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (!DumpUtils.checkDumpAndUsageStatsPermission(this.mContext, TAG, pw)) {
            return;
        }
        boolean protoFormat = hasOption(args, "--proto");
        ProtoOutputStream proto = null;
        DebugInfo i18nLibraryDebugInfo = I18nModuleDebug.getDebugInfo();
        addTimeZoneApkDebugInfo(i18nLibraryDebugInfo);
        if (protoFormat) {
            proto = new ProtoOutputStream(fd);
            reportTimeZoneInfoProto(i18nLibraryDebugInfo, proto);
        } else {
            reportTimeZoneInfo(i18nLibraryDebugInfo, pw);
        }
        if (protoFormat) {
            proto.flush();
        }
    }

    private static boolean hasOption(String[] args, String arg) {
        for (String opt : args) {
            if (arg.equals(opt)) {
                return true;
            }
        }
        return false;
    }

    private static void addTimeZoneApkDebugInfo(DebugInfo coreLibraryDebugInfo) {
        String versionFileName = TimeZoneDataFiles.getDataTimeZoneFile(TimeZoneDistro.DISTRO_VERSION_FILE_NAME);
        addDistroVersionDebugInfo(versionFileName, "core_library.timezone.source.data_", coreLibraryDebugInfo);
    }

    private static void reportTimeZoneInfo(DebugInfo coreLibraryDebugInfo, PrintWriter pw) {
        pw.println("Core Library Debug Info: ");
        for (DebugInfo.DebugEntry debugEntry : coreLibraryDebugInfo.getDebugEntries()) {
            pw.print(debugEntry.getKey());
            pw.print(": \"");
            pw.print(debugEntry.getStringValue());
            pw.println("\"");
        }
    }

    private static void reportTimeZoneInfoProto(DebugInfo coreLibraryDebugInfo, ProtoOutputStream protoStream) {
        for (DebugInfo.DebugEntry debugEntry : coreLibraryDebugInfo.getDebugEntries()) {
            long entryToken = protoStream.start(CompanionAppsPermissions.APP_PERMISSIONS);
            protoStream.write(CompanionAppsPermissions.AppPermissions.PACKAGE_NAME, debugEntry.getKey());
            protoStream.write(1138166333442L, debugEntry.getStringValue());
            protoStream.end(entryToken);
        }
    }

    private static void addDistroVersionDebugInfo(String distroVersionFileName, String debugKeyPrefix, DebugInfo debugInfo) {
        File file = new File(distroVersionFileName);
        String statusKey = debugKeyPrefix + "status";
        if (file.exists()) {
            try {
                byte[] versionBytes = FileUtils.readBytes(file, DistroVersion.DISTRO_VERSION_FILE_LENGTH);
                DistroVersion distroVersion = DistroVersion.fromBytes(versionBytes);
                String formatVersionString = distroVersion.formatMajorVersion + "." + distroVersion.formatMinorVersion;
                debugInfo.addStringEntry(statusKey, "OK").addStringEntry(debugKeyPrefix + "formatVersion", formatVersionString).addStringEntry(debugKeyPrefix + "rulesVersion", distroVersion.rulesVersion).addStringEntry(debugKeyPrefix + "revision", distroVersion.revision);
                return;
            } catch (DistroException | IOException e) {
                debugInfo.addStringEntry(statusKey, "ERROR");
                debugInfo.addStringEntry(debugKeyPrefix + "exception_class", e.getClass().getName());
                debugInfo.addStringEntry(debugKeyPrefix + "exception_msg", e.getMessage());
                logMessage("Error reading " + file, e);
                return;
            }
        }
        debugInfo.addStringEntry(statusKey, "NOT_FOUND");
    }

    private static void logMessage(String msg, Throwable t) {
        Slog.v(TAG, msg, t);
    }
}
