package com.android.server;

import android.app.ActivityThread;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.net.Uri;
import android.net.util.NetworkConstants;
import android.os.BatteryStatsInternal;
import android.os.Binder;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.os.ShellCommand;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.KeyValueListParser;
import android.util.Slog;
import com.android.internal.os.AppIdToPackageMap;
import com.android.internal.os.BackgroundThread;
import com.android.internal.os.BinderCallsStats;
import com.android.internal.os.BinderInternal;
import com.android.internal.os.CachedDeviceState;
import com.android.internal.util.DumpUtils;
import com.android.server.timezonedetector.ServiceConfigAccessor;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
/* loaded from: classes.dex */
public class BinderCallsStatsService extends Binder {
    private static final String PERSIST_SYS_BINDER_CALLS_DETAILED_TRACKING = "persist.sys.binder_calls_detailed_tracking";
    private static final String SERVICE_NAME = "binder_calls_stats";
    private static final String TAG = "BinderCallsStatsService";
    private final BinderCallsStats mBinderCallsStats;
    private SettingsObserver mSettingsObserver;
    private final AuthorizedWorkSourceProvider mWorkSourceProvider;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class AuthorizedWorkSourceProvider implements BinderInternal.WorkSourceProvider {
        private ArraySet<Integer> mAppIdTrustlist = new ArraySet<>();

        AuthorizedWorkSourceProvider() {
        }

        public int resolveWorkSourceUid(int untrustedWorkSourceUid) {
            int callingUid = getCallingUid();
            int appId = UserHandle.getAppId(callingUid);
            if (this.mAppIdTrustlist.contains(Integer.valueOf(appId))) {
                boolean isWorkSourceSet = untrustedWorkSourceUid != -1;
                return isWorkSourceSet ? untrustedWorkSourceUid : callingUid;
            }
            return callingUid;
        }

        public void systemReady(Context context) {
            this.mAppIdTrustlist = createAppidTrustlist(context);
        }

        public void dump(PrintWriter pw, AppIdToPackageMap packageMap) {
            pw.println("AppIds of apps that can set the work source:");
            ArraySet<Integer> trustlist = this.mAppIdTrustlist;
            Iterator<Integer> it = trustlist.iterator();
            while (it.hasNext()) {
                Integer appId = it.next();
                pw.println("\t- " + packageMap.mapAppId(appId.intValue()));
            }
        }

        protected int getCallingUid() {
            return Binder.getCallingUid();
        }

        private ArraySet<Integer> createAppidTrustlist(Context context) {
            ArraySet<Integer> trustlist = new ArraySet<>();
            trustlist.add(Integer.valueOf(UserHandle.getAppId(Process.myUid())));
            PackageManager pm = context.getPackageManager();
            String[] permissions = {"android.permission.UPDATE_DEVICE_STATS"};
            List<PackageInfo> packages = pm.getPackagesHoldingPermissions(permissions, 786432);
            int packagesSize = packages.size();
            for (int i = 0; i < packagesSize; i++) {
                PackageInfo pkgInfo = packages.get(i);
                try {
                    int uid = pm.getPackageUid(pkgInfo.packageName, 786432);
                    int appId = UserHandle.getAppId(uid);
                    trustlist.add(Integer.valueOf(appId));
                } catch (PackageManager.NameNotFoundException e) {
                    Slog.e(BinderCallsStatsService.TAG, "Cannot find uid for package name " + pkgInfo.packageName, e);
                }
            }
            return trustlist;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class SettingsObserver extends ContentObserver {
        private final BinderCallsStats mBinderCallsStats;
        private final Context mContext;
        private boolean mEnabled;
        private final KeyValueListParser mParser;
        private final Uri mUri;
        private final AuthorizedWorkSourceProvider mWorkSourceProvider;

        SettingsObserver(Context context, BinderCallsStats binderCallsStats, AuthorizedWorkSourceProvider workSourceProvider) {
            super(BackgroundThread.getHandler());
            Uri uriFor = Settings.Global.getUriFor(BinderCallsStatsService.SERVICE_NAME);
            this.mUri = uriFor;
            this.mParser = new KeyValueListParser(',');
            this.mContext = context;
            context.getContentResolver().registerContentObserver(uriFor, false, this, 0);
            this.mBinderCallsStats = binderCallsStats;
            this.mWorkSourceProvider = workSourceProvider;
            onChange();
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri, int userId) {
            if (this.mUri.equals(uri)) {
                onChange();
            }
        }

        public void onChange() {
            if (!SystemProperties.get(BinderCallsStatsService.PERSIST_SYS_BINDER_CALLS_DETAILED_TRACKING).isEmpty()) {
                return;
            }
            try {
                this.mParser.setString(Settings.Global.getString(this.mContext.getContentResolver(), BinderCallsStatsService.SERVICE_NAME));
            } catch (IllegalArgumentException e) {
                Slog.e(BinderCallsStatsService.TAG, "Bad binder call stats settings", e);
            }
            this.mBinderCallsStats.setDetailedTracking(this.mParser.getBoolean("detailed_tracking", true));
            this.mBinderCallsStats.setSamplingInterval(this.mParser.getInt("sampling_interval", 1000));
            this.mBinderCallsStats.setMaxBinderCallStats(this.mParser.getInt("max_call_stats_count", (int) NetworkConstants.ETHER_MTU));
            this.mBinderCallsStats.setTrackScreenInteractive(this.mParser.getBoolean("track_screen_state", false));
            this.mBinderCallsStats.setTrackDirectCallerUid(this.mParser.getBoolean("track_calling_uid", true));
            this.mBinderCallsStats.setIgnoreBatteryStatus(this.mParser.getBoolean("ignore_battery_status", false));
            this.mBinderCallsStats.setShardingModulo(this.mParser.getInt("sharding_modulo", 1));
            this.mBinderCallsStats.setCollectLatencyData(this.mParser.getBoolean("collect_latency_data", true));
            BinderCallsStats.SettingsObserver.configureLatencyObserver(this.mParser, this.mBinderCallsStats.getLatencyObserver());
            boolean enabled = this.mParser.getBoolean(ServiceConfigAccessor.PROVIDER_MODE_ENABLED, true);
            if (this.mEnabled != enabled) {
                if (enabled) {
                    Binder.setObserver(this.mBinderCallsStats);
                    Binder.setProxyTransactListener(new Binder.PropagateWorkSourceTransactListener());
                    Binder.setWorkSourceProvider(this.mWorkSourceProvider);
                } else {
                    Binder.setObserver(null);
                    Binder.setProxyTransactListener(null);
                    Binder.setWorkSourceProvider(new BinderInternal.WorkSourceProvider() { // from class: com.android.server.BinderCallsStatsService$SettingsObserver$$ExternalSyntheticLambda0
                        public final int resolveWorkSourceUid(int i) {
                            int callingUid;
                            callingUid = Binder.getCallingUid();
                            return callingUid;
                        }
                    });
                }
                this.mEnabled = enabled;
                this.mBinderCallsStats.reset();
                this.mBinderCallsStats.setAddDebugEntries(enabled);
                this.mBinderCallsStats.getLatencyObserver().reset();
            }
        }
    }

    /* loaded from: classes.dex */
    public static class Internal {
        private final BinderCallsStats mBinderCallsStats;

        Internal(BinderCallsStats binderCallsStats) {
            this.mBinderCallsStats = binderCallsStats;
        }

        public void reset() {
            this.mBinderCallsStats.reset();
        }

        public ArrayList<BinderCallsStats.ExportedCallStat> getExportedCallStats() {
            return this.mBinderCallsStats.getExportedCallStats();
        }

        public ArrayMap<String, Integer> getExportedExceptionStats() {
            return this.mBinderCallsStats.getExportedExceptionStats();
        }
    }

    /* loaded from: classes.dex */
    public static class LifeCycle extends SystemService {
        private BinderCallsStats mBinderCallsStats;
        private BinderCallsStatsService mService;
        private AuthorizedWorkSourceProvider mWorkSourceProvider;

        public LifeCycle(Context context) {
            super(context);
        }

        @Override // com.android.server.SystemService
        public void onStart() {
            this.mBinderCallsStats = new BinderCallsStats(new BinderCallsStats.Injector());
            this.mWorkSourceProvider = new AuthorizedWorkSourceProvider();
            this.mService = new BinderCallsStatsService(this.mBinderCallsStats, this.mWorkSourceProvider);
            publishLocalService(Internal.class, new Internal(this.mBinderCallsStats));
            publishBinderService(BinderCallsStatsService.SERVICE_NAME, this.mService);
            boolean detailedTrackingEnabled = SystemProperties.getBoolean(BinderCallsStatsService.PERSIST_SYS_BINDER_CALLS_DETAILED_TRACKING, false);
            if (detailedTrackingEnabled) {
                Slog.i(BinderCallsStatsService.TAG, "Enabled CPU usage tracking for binder calls. Controlled by persist.sys.binder_calls_detailed_tracking or via dumpsys binder_calls_stats --enable-detailed-tracking");
                this.mBinderCallsStats.setDetailedTracking(true);
            }
        }

        @Override // com.android.server.SystemService
        public void onBootPhase(int phase) {
            if (500 == phase) {
                CachedDeviceState.Readonly deviceState = (CachedDeviceState.Readonly) getLocalService(CachedDeviceState.Readonly.class);
                this.mBinderCallsStats.setDeviceState(deviceState);
                final BatteryStatsInternal batteryStatsInternal = (BatteryStatsInternal) getLocalService(BatteryStatsInternal.class);
                this.mBinderCallsStats.setCallStatsObserver(new BinderInternal.CallStatsObserver() { // from class: com.android.server.BinderCallsStatsService.LifeCycle.1
                    public void noteCallStats(int workSourceUid, long incrementalCallCount, Collection<BinderCallsStats.CallStat> callStats) {
                        batteryStatsInternal.noteBinderCallStats(workSourceUid, incrementalCallCount, callStats);
                    }

                    public void noteBinderThreadNativeIds(int[] binderThreadNativeTids) {
                        batteryStatsInternal.noteBinderThreadNativeIds(binderThreadNativeTids);
                    }
                });
                this.mWorkSourceProvider.systemReady(getContext());
                this.mService.systemReady(getContext());
            }
        }
    }

    BinderCallsStatsService(BinderCallsStats binderCallsStats, AuthorizedWorkSourceProvider workSourceProvider) {
        this.mBinderCallsStats = binderCallsStats;
        this.mWorkSourceProvider = workSourceProvider;
    }

    public void systemReady(Context context) {
        this.mSettingsObserver = new SettingsObserver(context, this.mBinderCallsStats, this.mWorkSourceProvider);
    }

    public void reset() {
        Slog.i(TAG, "Resetting stats");
        this.mBinderCallsStats.reset();
    }

    @Override // android.os.Binder
    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (!DumpUtils.checkDumpAndUsageStatsPermission(ActivityThread.currentApplication(), SERVICE_NAME, pw)) {
            return;
        }
        boolean verbose = false;
        int worksourceUid = -1;
        if (args != null) {
            int i = 0;
            while (i < args.length) {
                String arg = args[i];
                if ("-a".equals(arg)) {
                    verbose = true;
                } else if ("-h".equals(arg)) {
                    pw.println("dumpsys binder_calls_stats options:");
                    pw.println("  -a: Verbose");
                    pw.println("  --work-source-uid <UID>: Dump binder calls from the UID");
                    return;
                } else if ("--work-source-uid".equals(arg)) {
                    i++;
                    if (i >= args.length) {
                        throw new IllegalArgumentException("Argument expected after \"" + arg + "\"");
                    }
                    String uidArg = args[i];
                    try {
                        worksourceUid = Integer.parseInt(uidArg);
                    } catch (NumberFormatException e) {
                        pw.println("Invalid UID: " + uidArg);
                        return;
                    }
                } else {
                    continue;
                }
                i++;
            }
            int i2 = args.length;
            if (i2 > 0 && worksourceUid == -1) {
                BinderCallsStatsShellCommand command = new BinderCallsStatsShellCommand(pw);
                int status = command.exec(this, null, FileDescriptor.out, FileDescriptor.err, args);
                if (status == 0) {
                    return;
                }
            }
        }
        this.mBinderCallsStats.dump(pw, AppIdToPackageMap.getSnapshot(), worksourceUid, verbose);
    }

    public int handleShellCommand(ParcelFileDescriptor in, ParcelFileDescriptor out, ParcelFileDescriptor err, String[] args) {
        ShellCommand command = new BinderCallsStatsShellCommand(null);
        int status = command.exec(this, in.getFileDescriptor(), out.getFileDescriptor(), err.getFileDescriptor(), args);
        if (status != 0) {
            command.onHelp();
        }
        return status;
    }

    /* loaded from: classes.dex */
    private class BinderCallsStatsShellCommand extends ShellCommand {
        private final PrintWriter mPrintWriter;

        BinderCallsStatsShellCommand(PrintWriter printWriter) {
            this.mPrintWriter = printWriter;
        }

        public PrintWriter getOutPrintWriter() {
            PrintWriter printWriter = this.mPrintWriter;
            if (printWriter != null) {
                return printWriter;
            }
            return super.getOutPrintWriter();
        }

        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        public int onCommand(String cmd) {
            char c;
            PrintWriter pw = getOutPrintWriter();
            if (cmd == null) {
                return -1;
            }
            switch (cmd.hashCode()) {
                case -1615291473:
                    if (cmd.equals("--reset")) {
                        c = 0;
                        break;
                    }
                    c = 65535;
                    break;
                case -1289263917:
                    if (cmd.equals("--no-sampling")) {
                        c = 3;
                        break;
                    }
                    c = 65535;
                    break;
                case -1237677752:
                    if (cmd.equals("--disable")) {
                        c = 2;
                        break;
                    }
                    c = 65535;
                    break;
                case -534486470:
                    if (cmd.equals("--work-source-uid")) {
                        c = 7;
                        break;
                    }
                    c = 65535;
                    break;
                case -106516359:
                    if (cmd.equals("--dump-worksource-provider")) {
                        c = 6;
                        break;
                    }
                    c = 65535;
                    break;
                case 1101165347:
                    if (cmd.equals("--enable")) {
                        c = 1;
                        break;
                    }
                    c = 65535;
                    break;
                case 1448286703:
                    if (cmd.equals("--disable-detailed-tracking")) {
                        c = 5;
                        break;
                    }
                    c = 65535;
                    break;
                case 2041864970:
                    if (cmd.equals("--enable-detailed-tracking")) {
                        c = 4;
                        break;
                    }
                    c = 65535;
                    break;
                default:
                    c = 65535;
                    break;
            }
            switch (c) {
                case 0:
                    BinderCallsStatsService.this.reset();
                    pw.println("binder_calls_stats reset.");
                    break;
                case 1:
                    Binder.setObserver(BinderCallsStatsService.this.mBinderCallsStats);
                    break;
                case 2:
                    Binder.setObserver(null);
                    break;
                case 3:
                    BinderCallsStatsService.this.mBinderCallsStats.setSamplingInterval(1);
                    break;
                case 4:
                    SystemProperties.set(BinderCallsStatsService.PERSIST_SYS_BINDER_CALLS_DETAILED_TRACKING, "1");
                    BinderCallsStatsService.this.mBinderCallsStats.setDetailedTracking(true);
                    pw.println("Detailed tracking enabled");
                    break;
                case 5:
                    SystemProperties.set(BinderCallsStatsService.PERSIST_SYS_BINDER_CALLS_DETAILED_TRACKING, "");
                    BinderCallsStatsService.this.mBinderCallsStats.setDetailedTracking(false);
                    pw.println("Detailed tracking disabled");
                    break;
                case 6:
                    BinderCallsStatsService.this.mBinderCallsStats.setDetailedTracking(true);
                    BinderCallsStatsService.this.mWorkSourceProvider.dump(pw, AppIdToPackageMap.getSnapshot());
                    break;
                case 7:
                    String uidArg = getNextArgRequired();
                    try {
                        int uid = Integer.parseInt(uidArg);
                        BinderCallsStatsService.this.mBinderCallsStats.recordAllCallsForWorkSourceUid(uid);
                        break;
                    } catch (NumberFormatException e) {
                        pw.println("Invalid UID: " + uidArg);
                        return -1;
                    }
                default:
                    return handleDefaultCommands(cmd);
            }
            return 0;
        }

        public void onHelp() {
            PrintWriter pw = getOutPrintWriter();
            pw.println("binder_calls_stats commands:");
            pw.println("  --reset: Reset stats");
            pw.println("  --enable: Enable tracking binder calls");
            pw.println("  --disable: Disables tracking binder calls");
            pw.println("  --no-sampling: Tracks all calls");
            pw.println("  --enable-detailed-tracking: Enables detailed tracking");
            pw.println("  --disable-detailed-tracking: Disables detailed tracking");
            pw.println("  --work-source-uid <UID>: Track all binder calls from the UID");
        }
    }
}
