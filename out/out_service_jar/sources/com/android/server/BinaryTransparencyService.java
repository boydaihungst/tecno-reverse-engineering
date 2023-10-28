package com.android.server;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.ModuleInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.ShellCallback;
import android.os.ShellCommand;
import android.os.SystemProperties;
import android.util.PackageUtils;
import android.util.Slog;
import com.android.internal.os.IBinaryTransparencyService;
import com.android.internal.util.FrameworkStatsLog;
import com.android.server.BinaryTransparencyService;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.function.Predicate;
import java.util.stream.Collectors;
/* loaded from: classes.dex */
public class BinaryTransparencyService extends SystemService {
    static final String BINARY_HASH_ERROR = "SHA256HashError";
    private static final String EXTRA_SERVICE = "service";
    static final String SYSPROP_NAME_VBETA_DIGEST = "ro.boot.vbmeta.digest";
    private static final String TAG = "TransparencyService";
    static final String VBMETA_DIGEST_UNAVAILABLE = "vbmeta-digest-unavailable";
    static final String VBMETA_DIGEST_UNINITIALIZED = "vbmeta-digest-uninitialized";
    private HashMap<String, String> mBinaryHashes;
    private HashMap<String, Long> mBinaryLastUpdateTimes;
    private final Context mContext;
    private final BinaryTransparencyServiceImpl mServiceImpl;
    private String mVbmetaDigest;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public final class BinaryTransparencyServiceImpl extends IBinaryTransparencyService.Stub {
        BinaryTransparencyServiceImpl() {
        }

        public String getSignedImageInfo() {
            return BinaryTransparencyService.this.mVbmetaDigest;
        }

        public Map getApexInfo() {
            HashMap results = new HashMap();
            if (!BinaryTransparencyService.this.updateBinaryMeasurements()) {
                Slog.e(BinaryTransparencyService.TAG, "Error refreshing APEX measurements.");
                return results;
            }
            PackageManager pm = BinaryTransparencyService.this.mContext.getPackageManager();
            if (pm == null) {
                Slog.e(BinaryTransparencyService.TAG, "Error obtaining an instance of PackageManager.");
                return results;
            }
            for (PackageInfo packageInfo : BinaryTransparencyService.this.getInstalledApexs()) {
                results.put(packageInfo, BinaryTransparencyService.this.mBinaryHashes.get(packageInfo.packageName));
            }
            return results;
        }

        /* JADX DEBUG: Multi-variable search result rejected for r8v0, resolved type: com.android.server.BinaryTransparencyService$BinaryTransparencyServiceImpl */
        /* JADX WARN: Multi-variable type inference failed */
        /* JADX WARN: Type inference failed for: r0v0, types: [com.android.server.BinaryTransparencyService$BinaryTransparencyServiceImpl$1] */
        public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) throws RemoteException {
            new ShellCommand() { // from class: com.android.server.BinaryTransparencyService.BinaryTransparencyServiceImpl.1
                /* JADX WARN: Code restructure failed: missing block: B:9:0x001c, code lost:
                    if (r2.equals("-a") != false) goto L8;
                 */
                /*
                    Code decompiled incorrectly, please refer to instructions dump.
                */
                private int printSignedImageInfo() {
                    PrintWriter pw = getOutPrintWriter();
                    boolean listAllPartitions = false;
                    while (true) {
                        String opt = getNextOption();
                        boolean z = false;
                        if (opt != null) {
                            switch (opt.hashCode()) {
                                case 1492:
                                    break;
                                default:
                                    z = true;
                                    break;
                            }
                            switch (z) {
                                case false:
                                    listAllPartitions = true;
                                default:
                                    pw.println("ERROR: Unknown option: " + opt);
                                    return 1;
                            }
                        } else {
                            String signedImageInfo = BinaryTransparencyServiceImpl.this.getSignedImageInfo();
                            pw.println("Image Info:");
                            pw.println(Build.FINGERPRINT);
                            pw.println(signedImageInfo);
                            pw.println("");
                            if (listAllPartitions) {
                                PackageManager pm = BinaryTransparencyService.this.mContext.getPackageManager();
                                if (pm == null) {
                                    pw.println("ERROR: Failed to obtain an instance of package manager.");
                                    return -1;
                                }
                                pw.println("Other partitions:");
                                List<Build.Partition> buildPartitions = Build.getFingerprintedPartitions();
                                for (Build.Partition buildPartition : buildPartitions) {
                                    pw.println("Name: " + buildPartition.getName());
                                    pw.println("Fingerprint: " + buildPartition.getFingerprint());
                                    pw.println("Build time (ms): " + buildPartition.getBuildTimeMillis());
                                }
                            }
                            return 0;
                        }
                    }
                }

                private void printModuleDetails(ModuleInfo moduleInfo, PrintWriter pw) {
                    pw.println("--- Module Details ---");
                    pw.println("Module name: " + ((Object) moduleInfo.getName()));
                    pw.println("Module visibility: " + (moduleInfo.isHidden() ? "hidden" : "visible"));
                }

                /* JADX WARN: Code restructure failed: missing block: B:17:0x0048, code lost:
                    if (r3.equals("-v") != false) goto L16;
                 */
                /*
                    Code decompiled incorrectly, please refer to instructions dump.
                */
                private int printAllApexs() {
                    PrintWriter pw = getOutPrintWriter();
                    boolean verbose = false;
                    if (!BinaryTransparencyService.this.updateBinaryMeasurements()) {
                        pw.println("ERROR: Failed to refresh info for APEXs.");
                        return -1;
                    } else if (BinaryTransparencyService.this.mBinaryHashes == null || BinaryTransparencyService.this.mBinaryHashes.size() == 0) {
                        pw.println("ERROR: Unable to obtain apex_info at this time.");
                        return -1;
                    } else {
                        while (true) {
                            String opt = getNextOption();
                            boolean z = false;
                            if (opt != null) {
                                switch (opt.hashCode()) {
                                    case 1513:
                                        break;
                                    default:
                                        z = true;
                                        break;
                                }
                                switch (z) {
                                    case false:
                                        verbose = true;
                                    default:
                                        pw.println("ERROR: Unknown option: " + opt);
                                        return 1;
                                }
                            } else {
                                PackageManager pm = BinaryTransparencyService.this.mContext.getPackageManager();
                                if (pm == null) {
                                    pw.println("ERROR: Failed to obtain an instance of package manager.");
                                    return -1;
                                }
                                pw.println("APEX Info:");
                                for (PackageInfo packageInfo : BinaryTransparencyService.this.getInstalledApexs()) {
                                    String packageName = packageInfo.packageName;
                                    pw.println(packageName + ";" + packageInfo.getLongVersionCode() + ":" + ((String) BinaryTransparencyService.this.mBinaryHashes.get(packageName)).toLowerCase());
                                    if (verbose) {
                                        pw.println("Install location: " + packageInfo.applicationInfo.sourceDir);
                                        pw.println("Last Update Time (ms): " + packageInfo.lastUpdateTime);
                                        try {
                                            ModuleInfo moduleInfo = pm.getModuleInfo(packageInfo.packageName, 0);
                                            pw.println("Is A Module: True");
                                            printModuleDetails(moduleInfo, pw);
                                            pw.println("");
                                        } catch (PackageManager.NameNotFoundException e) {
                                            pw.println("Is A Module: False");
                                            pw.println("");
                                        }
                                    }
                                }
                                return 0;
                            }
                        }
                    }
                }

                /* JADX WARN: Code restructure failed: missing block: B:17:0x0048, code lost:
                    if (r3.equals("-v") != false) goto L16;
                 */
                /*
                    Code decompiled incorrectly, please refer to instructions dump.
                */
                private int printAllModules() {
                    PrintWriter pw = getOutPrintWriter();
                    boolean verbose = false;
                    if (!BinaryTransparencyService.this.updateBinaryMeasurements()) {
                        pw.println("ERROR: Failed to refresh info for Modules.");
                        return -1;
                    } else if (BinaryTransparencyService.this.mBinaryHashes == null || BinaryTransparencyService.this.mBinaryHashes.size() == 0) {
                        pw.println("ERROR: Unable to obtain module_info at this time.");
                        return -1;
                    } else {
                        while (true) {
                            String opt = getNextOption();
                            boolean z = false;
                            if (opt != null) {
                                switch (opt.hashCode()) {
                                    case 1513:
                                        break;
                                    default:
                                        z = true;
                                        break;
                                }
                                switch (z) {
                                    case false:
                                        verbose = true;
                                    default:
                                        pw.println("ERROR: Unknown option: " + opt);
                                        return 1;
                                }
                            } else {
                                PackageManager pm = BinaryTransparencyService.this.mContext.getPackageManager();
                                if (pm == null) {
                                    pw.println("ERROR: Failed to obtain an instance of package manager.");
                                    return -1;
                                }
                                pw.println("Module Info:");
                                for (ModuleInfo module : pm.getInstalledModules(131072)) {
                                    String packageName = module.getPackageName();
                                    try {
                                        PackageInfo packageInfo = pm.getPackageInfo(packageName, 1073741824);
                                        pw.println(packageInfo.packageName + ";" + packageInfo.getLongVersionCode() + ":" + ((String) BinaryTransparencyService.this.mBinaryHashes.get(packageName)).toLowerCase());
                                        if (verbose) {
                                            pw.println("Install location: " + packageInfo.applicationInfo.sourceDir);
                                            printModuleDetails(module, pw);
                                            pw.println("");
                                        }
                                    } catch (PackageManager.NameNotFoundException e) {
                                        pw.println(packageName + ";ERROR:Unable to find PackageInfo for this module.");
                                        if (verbose) {
                                            printModuleDetails(module, pw);
                                            pw.println("");
                                        }
                                    }
                                }
                                return 0;
                            }
                        }
                    }
                }

                public int onCommand(String cmd) {
                    boolean z;
                    if (cmd == null) {
                        return handleDefaultCommands(cmd);
                    }
                    PrintWriter pw = getOutPrintWriter();
                    char c = 65535;
                    switch (cmd.hashCode()) {
                        case 102230:
                            if (cmd.equals("get")) {
                                z = false;
                                break;
                            }
                        default:
                            z = true;
                            break;
                    }
                    switch (z) {
                        case false:
                            String infoType = getNextArg();
                            if (infoType == null) {
                                printHelpMenu();
                                return -1;
                            }
                            switch (infoType.hashCode()) {
                                case -1443097326:
                                    if (infoType.equals("image_info")) {
                                        c = 0;
                                        break;
                                    }
                                    break;
                                case -1195140447:
                                    if (infoType.equals("module_info")) {
                                        c = 2;
                                        break;
                                    }
                                    break;
                                case 1366866347:
                                    if (infoType.equals("apex_info")) {
                                        c = 1;
                                        break;
                                    }
                                    break;
                            }
                            switch (c) {
                                case 0:
                                    return printSignedImageInfo();
                                case 1:
                                    return printAllApexs();
                                case 2:
                                    return printAllModules();
                                default:
                                    pw.println(String.format("ERROR: Unknown info type '%s'", infoType));
                                    return 1;
                            }
                        default:
                            return handleDefaultCommands(cmd);
                    }
                }

                private void printHelpMenu() {
                    PrintWriter pw = getOutPrintWriter();
                    pw.println("Transparency manager (transparency) commands:");
                    pw.println("    help");
                    pw.println("        Print this help text.");
                    pw.println("");
                    pw.println("    get image_info [-a]");
                    pw.println("        Print information about loaded image (firmware). Options:");
                    pw.println("            -a: lists all other identifiable partitions.");
                    pw.println("");
                    pw.println("    get apex_info [-v]");
                    pw.println("        Print information about installed APEXs on device.");
                    pw.println("            -v: lists more verbose information about each APEX");
                    pw.println("");
                    pw.println("    get module_info [-v]");
                    pw.println("        Print information about installed modules on device.");
                    pw.println("            -v: lists more verbose information about each module");
                    pw.println("");
                }

                public void onHelp() {
                    printHelpMenu();
                }
            }.exec(this, in, out, err, args, callback, resultReceiver);
        }
    }

    public BinaryTransparencyService(Context context) {
        super(context);
        this.mContext = context;
        this.mServiceImpl = new BinaryTransparencyServiceImpl();
        this.mVbmetaDigest = VBMETA_DIGEST_UNINITIALIZED;
        this.mBinaryHashes = new HashMap<>();
        this.mBinaryLastUpdateTimes = new HashMap<>();
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        try {
            publishBinderService("transparency", this.mServiceImpl);
            Slog.i(TAG, "Started BinaryTransparencyService");
        } catch (Throwable t) {
            Slog.e(TAG, "Failed to start BinaryTransparencyService.", t);
        }
    }

    @Override // com.android.server.SystemService
    public void onBootPhase(int phase) {
        if (phase == 1000) {
            Slog.i(TAG, "Boot completed. Getting VBMeta Digest.");
            getVBMetaDigestInformation();
            Slog.i(TAG, "Scheduling APEX and Module measurements to be updated.");
            UpdateMeasurementsJobService.scheduleBinaryMeasurements(this.mContext, this);
        }
    }

    /* loaded from: classes.dex */
    public static class UpdateMeasurementsJobService extends JobService {
        private static final int COMPUTE_APEX_MODULE_SHA256_JOB_ID = UpdateMeasurementsJobService.class.hashCode();

        @Override // android.app.job.JobService
        public boolean onStartJob(final JobParameters params) {
            Slog.d(BinaryTransparencyService.TAG, "Job to update binary measurements started.");
            if (params.getJobId() != COMPUTE_APEX_MODULE_SHA256_JOB_ID) {
                return false;
            }
            Executors.defaultThreadFactory().newThread(new Runnable() { // from class: com.android.server.BinaryTransparencyService$UpdateMeasurementsJobService$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    BinaryTransparencyService.UpdateMeasurementsJobService.this.m97xe101905f(params);
                }
            }).start();
            return true;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onStartJob$0$com-android-server-BinaryTransparencyService$UpdateMeasurementsJobService  reason: not valid java name */
        public /* synthetic */ void m97xe101905f(JobParameters params) {
            IBinder b = ServiceManager.getService("transparency");
            IBinaryTransparencyService iBtsService = IBinaryTransparencyService.Stub.asInterface(b);
            try {
                iBtsService.getApexInfo();
                jobFinished(params, false);
            } catch (RemoteException e) {
                Slog.e(BinaryTransparencyService.TAG, "Updating binary measurements was interrupted.", e);
            }
        }

        @Override // android.app.job.JobService
        public boolean onStopJob(JobParameters params) {
            return false;
        }

        static void scheduleBinaryMeasurements(Context context, BinaryTransparencyService service) {
            Slog.i(BinaryTransparencyService.TAG, "Scheduling APEX & Module SHA256 digest computation job");
            JobScheduler jobScheduler = (JobScheduler) context.getSystemService(JobScheduler.class);
            if (jobScheduler == null) {
                Slog.e(BinaryTransparencyService.TAG, "Failed to obtain an instance of JobScheduler.");
                return;
            }
            int i = COMPUTE_APEX_MODULE_SHA256_JOB_ID;
            JobInfo jobInfo = new JobInfo.Builder(i, new ComponentName(context, UpdateMeasurementsJobService.class)).setRequiresDeviceIdle(true).setRequiresCharging(true).build();
            if (jobScheduler.schedule(jobInfo) != 1) {
                Slog.e(BinaryTransparencyService.TAG, "Failed to schedule job to update binary measurements.");
            } else {
                Slog.d(BinaryTransparencyService.TAG, String.format("Job %d to update binary measurements scheduled successfully.", Integer.valueOf(i)));
            }
        }
    }

    private void getVBMetaDigestInformation() {
        String str = SystemProperties.get(SYSPROP_NAME_VBETA_DIGEST, VBMETA_DIGEST_UNAVAILABLE);
        this.mVbmetaDigest = str;
        Slog.d(TAG, String.format("VBMeta Digest: %s", str));
        FrameworkStatsLog.write((int) FrameworkStatsLog.VBMETA_DIGEST_REPORTED, this.mVbmetaDigest);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public List<PackageInfo> getInstalledApexs() {
        List<PackageInfo> results = new ArrayList<>();
        PackageManager pm = this.mContext.getPackageManager();
        if (pm == null) {
            Slog.e(TAG, "Error obtaining an instance of PackageManager.");
            return results;
        }
        List<PackageInfo> allPackages = pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(1073741824L));
        if (allPackages == null) {
            Slog.e(TAG, "Error obtaining installed packages (including APEX)");
            return results;
        }
        return (List) allPackages.stream().filter(new Predicate() { // from class: com.android.server.BinaryTransparencyService$$ExternalSyntheticLambda0
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                boolean z;
                z = ((PackageInfo) obj).isApex;
                return z;
            }
        }).collect(Collectors.toList());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean updateBinaryMeasurements() {
        if (this.mBinaryHashes.size() == 0) {
            Slog.d(TAG, "No apex in cache yet.");
            doFreshBinaryMeasurements();
            return true;
        }
        PackageManager pm = this.mContext.getPackageManager();
        if (pm == null) {
            Slog.e(TAG, "Failed to obtain a valid PackageManager instance.");
            return false;
        }
        byte[] largeFileBuffer = PackageUtils.createLargeFileBuffer();
        for (Map.Entry<String, Long> entry : this.mBinaryLastUpdateTimes.entrySet()) {
            String packageName = entry.getKey();
            try {
                PackageInfo packageInfo = pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(1073741824L));
                long cachedUpdateTime = entry.getValue().longValue();
                if (packageInfo.lastUpdateTime > cachedUpdateTime) {
                    Slog.d(TAG, packageName + " has been updated!");
                    entry.setValue(Long.valueOf(packageInfo.lastUpdateTime));
                    String sha256digest = PackageUtils.computeSha256DigestForLargeFile(packageInfo.applicationInfo.sourceDir, largeFileBuffer);
                    if (sha256digest == null) {
                        Slog.e(TAG, "Failed to compute SHA256sum for file at " + packageInfo.applicationInfo.sourceDir);
                        this.mBinaryHashes.put(packageName, BINARY_HASH_ERROR);
                    } else {
                        this.mBinaryHashes.put(packageName, sha256digest);
                    }
                    if (packageInfo.isApex) {
                        FrameworkStatsLog.write((int) FrameworkStatsLog.APEX_INFO_GATHERED, packageInfo.packageName, packageInfo.getLongVersionCode(), this.mBinaryHashes.get(packageInfo.packageName));
                    }
                }
            } catch (PackageManager.NameNotFoundException e) {
                Slog.e(TAG, "Could not find package with name " + packageName);
            }
        }
        return true;
    }

    private void doFreshBinaryMeasurements() {
        int i;
        String str;
        String str2;
        String str3;
        String str4;
        PackageManager pm = this.mContext.getPackageManager();
        Slog.d(TAG, "Obtained package manager");
        byte[] largeFileBuffer = PackageUtils.createLargeFileBuffer();
        Iterator<PackageInfo> it = getInstalledApexs().iterator();
        while (true) {
            boolean hasNext = it.hasNext();
            i = 2;
            str = "Last update time for %s: %d";
            str2 = BINARY_HASH_ERROR;
            if (!hasNext) {
                break;
            }
            PackageInfo packageInfo = it.next();
            ApplicationInfo appInfo = packageInfo.applicationInfo;
            String sha256digest = PackageUtils.computeSha256DigestForLargeFile(appInfo.sourceDir, largeFileBuffer);
            if (sha256digest == null) {
                Slog.e(TAG, String.format("Failed to compute SHA256 digest for %s", packageInfo.packageName));
                this.mBinaryHashes.put(packageInfo.packageName, BINARY_HASH_ERROR);
            } else {
                this.mBinaryHashes.put(packageInfo.packageName, sha256digest);
            }
            FrameworkStatsLog.write((int) FrameworkStatsLog.APEX_INFO_GATHERED, packageInfo.packageName, packageInfo.getLongVersionCode(), this.mBinaryHashes.get(packageInfo.packageName));
            Slog.d(TAG, String.format("Last update time for %s: %d", packageInfo.packageName, Long.valueOf(packageInfo.lastUpdateTime)));
            this.mBinaryLastUpdateTimes.put(packageInfo.packageName, Long.valueOf(packageInfo.lastUpdateTime));
        }
        for (ModuleInfo module : pm.getInstalledModules(131072)) {
            String packageName = module.getPackageName();
            if (packageName == null) {
                Slog.e(TAG, "ERROR: Encountered null package name for module " + module.getApexModuleName());
            } else if (!this.mBinaryHashes.containsKey(module.getPackageName())) {
                try {
                    PackageInfo packageInfo2 = pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(1073741824L));
                    ApplicationInfo appInfo2 = packageInfo2.applicationInfo;
                    String sha256digest2 = PackageUtils.computeSha256DigestForLargeFile(appInfo2.sourceDir, largeFileBuffer);
                    if (sha256digest2 == null) {
                        Object[] objArr = new Object[1];
                        try {
                            objArr[0] = packageName;
                            Slog.e(TAG, String.format("Failed to compute SHA256 digest for %s", objArr));
                            this.mBinaryHashes.put(packageName, str2);
                        } catch (PackageManager.NameNotFoundException e) {
                            str3 = str;
                            str4 = str2;
                            Slog.e(TAG, "ERROR: Could not obtain PackageInfo for package name: " + packageName);
                            str = str3;
                            str2 = str4;
                            i = 2;
                        }
                    } else {
                        this.mBinaryHashes.put(packageName, sha256digest2);
                    }
                    Object[] objArr2 = new Object[i];
                    try {
                        objArr2[0] = packageName;
                        String str5 = str;
                        try {
                            objArr2[1] = Long.valueOf(packageInfo2.lastUpdateTime);
                            str3 = str5;
                        } catch (PackageManager.NameNotFoundException e2) {
                            str4 = str2;
                            str3 = str5;
                        }
                        try {
                            Slog.d(TAG, String.format(str3, objArr2));
                            str4 = str2;
                        } catch (PackageManager.NameNotFoundException e3) {
                            str4 = str2;
                            Slog.e(TAG, "ERROR: Could not obtain PackageInfo for package name: " + packageName);
                            str = str3;
                            str2 = str4;
                            i = 2;
                        }
                        try {
                            this.mBinaryLastUpdateTimes.put(packageName, Long.valueOf(packageInfo2.lastUpdateTime));
                            str = str3;
                            str2 = str4;
                            i = 2;
                        } catch (PackageManager.NameNotFoundException e4) {
                            Slog.e(TAG, "ERROR: Could not obtain PackageInfo for package name: " + packageName);
                            str = str3;
                            str2 = str4;
                            i = 2;
                        }
                    } catch (PackageManager.NameNotFoundException e5) {
                        str3 = str;
                    }
                } catch (PackageManager.NameNotFoundException e6) {
                    str3 = str;
                    str4 = str2;
                }
            }
        }
    }
}
