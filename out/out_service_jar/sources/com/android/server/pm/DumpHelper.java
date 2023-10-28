package com.android.server.pm;

import android.content.ComponentName;
import android.content.pm.FeatureInfo;
import android.os.Binder;
import android.os.UserHandle;
import android.os.incremental.PerUidReadTimeouts;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.SparseArray;
import android.util.proto.ProtoOutputStream;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.am.HostingRecord;
import com.android.server.pm.permission.PermissionManagerServiceInternal;
import com.android.server.pm.pkg.parsing.ParsingPackageUtils;
import com.android.server.pm.resolution.ComponentResolverApi;
import com.android.server.pm.verify.domain.DomainVerificationManagerInternal;
import com.android.server.pm.verify.domain.proxy.DomainVerificationProxy;
import com.android.server.wm.ActivityTaskManagerInternal;
import com.android.server.wm.ActivityTaskManagerService;
import com.mediatek.server.MtkSystemServer;
import com.mediatek.server.MtkSystemServiceFactory;
import com.mediatek.server.pm.PmsExt;
import dalvik.annotation.optimization.NeverCompile;
import defpackage.CompanionAppsPermissions;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.function.BiConsumer;
/* loaded from: classes2.dex */
final class DumpHelper {
    private final ApexManager mApexManager;
    private final ArrayMap<String, FeatureInfo> mAvailableFeatures;
    private final ChangedPackagesTracker mChangedPackagesTracker;
    private final DomainVerificationManagerInternal mDomainVerificationManager;
    private final PackageInstallerService mInstallerService;
    private final KnownPackages mKnownPackages;
    private final PerUidReadTimeouts[] mPerUidReadTimeouts;
    private final PermissionManagerServiceInternal mPermissionManager;
    private final ArraySet<String> mProtectedBroadcasts;
    private final String mRequiredVerifierPackage;
    private final StorageEventHelper mStorageEventHelper;
    final MtkSystemServer sMtkSystemServerIns = MtkSystemServer.getInstance();
    final PmsExt sPmsExt = MtkSystemServiceFactory.getInstance().makePmsExt();

    /* JADX INFO: Access modifiers changed from: package-private */
    public DumpHelper(PermissionManagerServiceInternal permissionManager, ApexManager apexManager, StorageEventHelper storageEventHelper, DomainVerificationManagerInternal domainVerificationManager, PackageInstallerService installerService, String requiredVerifierPackage, KnownPackages knownPackages, ChangedPackagesTracker changedPackagesTracker, ArrayMap<String, FeatureInfo> availableFeatures, ArraySet<String> protectedBroadcasts, PerUidReadTimeouts[] perUidReadTimeouts) {
        this.mPermissionManager = permissionManager;
        this.mApexManager = apexManager;
        this.mStorageEventHelper = storageEventHelper;
        this.mDomainVerificationManager = domainVerificationManager;
        this.mInstallerService = installerService;
        this.mRequiredVerifierPackage = requiredVerifierPackage;
        this.mKnownPackages = knownPackages;
        this.mChangedPackagesTracker = changedPackagesTracker;
        this.mAvailableFeatures = availableFeatures;
        this.mProtectedBroadcasts = protectedBroadcasts;
        this.mPerUidReadTimeouts = perUidReadTimeouts;
    }

    @NeverCompile
    public void doDump(Computer snapshot, FileDescriptor fd, final PrintWriter pw, String[] args) {
        ArraySet<String> permissionNames;
        String str;
        String packageName;
        ArraySet<String> permissionNames2;
        int i;
        String packageName2;
        String opt;
        DumpState dumpState = new DumpState();
        int opti = 0;
        while (opti < args.length && (opt = args[opti]) != null && opt.length() > 0 && opt.charAt(0) == '-') {
            opti++;
            if (!"-a".equals(opt)) {
                if ("-h".equals(opt)) {
                    printHelp(pw);
                    return;
                } else if ("--checkin".equals(opt)) {
                    dumpState.setCheckIn(true);
                } else if ("--all-components".equals(opt)) {
                    dumpState.setOptionEnabled(2);
                } else if ("-f".equals(opt)) {
                    dumpState.setOptionEnabled(1);
                } else if ("--proto".equals(opt)) {
                    dumpProto(snapshot, fd);
                    return;
                } else {
                    pw.println("Unknown argument: " + opt + "; use -h for help");
                }
            }
        }
        if (opti >= args.length) {
            permissionNames = null;
        } else {
            String cmd = args[opti];
            opti++;
            if (!PackageManagerService.PLATFORM_PACKAGE_NAME.equals(cmd) && !cmd.contains(".")) {
                if ("check-permission".equals(cmd)) {
                    if (opti >= args.length) {
                        pw.println("Error: check-permission missing permission argument");
                        return;
                    }
                    String perm = args[opti];
                    int opti2 = opti + 1;
                    if (opti2 >= args.length) {
                        pw.println("Error: check-permission missing package argument");
                        return;
                    }
                    String pkg = args[opti2];
                    int opti3 = 1 + opti2;
                    int opti4 = Binder.getCallingUid();
                    int user = UserHandle.getUserId(opti4);
                    if (opti3 < args.length) {
                        try {
                            user = Integer.parseInt(args[opti3]);
                        } catch (NumberFormatException e) {
                            pw.println("Error: check-permission user argument is not a number: " + args[opti3]);
                            return;
                        }
                    }
                    pw.println(this.mPermissionManager.checkPermission(perm, snapshot.resolveInternalPackageName(pkg, -1L), user));
                    return;
                }
                if (!"l".equals(cmd) && !"libraries".equals(cmd)) {
                    if (!"f".equals(cmd) && !"features".equals(cmd)) {
                        if (!ActivityTaskManagerService.DUMP_RECENTS_SHORT_CMD.equals(cmd) && !"resolvers".equals(cmd)) {
                            if (!"perm".equals(cmd) && !"permissions".equals(cmd)) {
                                if (ParsingPackageUtils.TAG_PERMISSION.equals(cmd)) {
                                    if (opti >= args.length) {
                                        pw.println("Error: permission requires permission name");
                                        return;
                                    }
                                    ArraySet<String> permissionNames3 = new ArraySet<>();
                                    while (opti < args.length) {
                                        permissionNames3.add(args[opti]);
                                        opti++;
                                    }
                                    dumpState.setDump(448);
                                    permissionNames = permissionNames3;
                                } else {
                                    if (!"pref".equals(cmd) && !"preferred".equals(cmd)) {
                                        if ("preferred-xml".equals(cmd)) {
                                            dumpState.setDump(8192);
                                            if (opti < args.length && "--full".equals(args[opti])) {
                                                dumpState.setFullPreferred(true);
                                                opti++;
                                                permissionNames = null;
                                            } else {
                                                permissionNames = null;
                                            }
                                        } else {
                                            if (!"d".equals(cmd) && !"domain-preferred-apps".equals(cmd)) {
                                                if (!"p".equals(cmd) && !"packages".equals(cmd)) {
                                                    if (!"q".equals(cmd) && !ParsingPackageUtils.TAG_QUERIES.equals(cmd)) {
                                                        if (!"s".equals(cmd) && !"shared-users".equals(cmd)) {
                                                            if (!"prov".equals(cmd) && !"providers".equals(cmd)) {
                                                                if ("m".equals(cmd) || "messages".equals(cmd)) {
                                                                    dumpState.setDump(512);
                                                                } else if ("v".equals(cmd) || "verifiers".equals(cmd)) {
                                                                    dumpState.setDump(2048);
                                                                } else if ("dv".equals(cmd) || "domain-verifier".equals(cmd)) {
                                                                    dumpState.setDump(131072);
                                                                } else if ("version".equals(cmd)) {
                                                                    dumpState.setDump(32768);
                                                                } else if ("k".equals(cmd) || "keysets".equals(cmd)) {
                                                                    dumpState.setDump(16384);
                                                                } else if ("installs".equals(cmd)) {
                                                                    dumpState.setDump(65536);
                                                                } else if ("frozen".equals(cmd)) {
                                                                    dumpState.setDump(524288);
                                                                } else if ("volumes".equals(cmd)) {
                                                                    dumpState.setDump(8388608);
                                                                } else if ("dexopt".equals(cmd)) {
                                                                    dumpState.setDump(1048576);
                                                                } else if ("compiler-stats".equals(cmd)) {
                                                                    dumpState.setDump(2097152);
                                                                } else if ("changes".equals(cmd)) {
                                                                    dumpState.setDump(4194304);
                                                                } else if ("service-permissions".equals(cmd)) {
                                                                    dumpState.setDump(16777216);
                                                                } else if ("known-packages".equals(cmd)) {
                                                                    dumpState.setDump(134217728);
                                                                } else if ("t".equals(cmd) || "timeouts".equals(cmd)) {
                                                                    dumpState.setDump(268435456);
                                                                } else if ("snapshot".equals(cmd)) {
                                                                    dumpState.setDump(536870912);
                                                                    if (opti < args.length) {
                                                                        if ("--full".equals(args[opti])) {
                                                                            dumpState.setBrief(false);
                                                                            opti++;
                                                                            permissionNames = null;
                                                                        } else if ("--brief".equals(args[opti])) {
                                                                            dumpState.setBrief(true);
                                                                            opti++;
                                                                            permissionNames = null;
                                                                        }
                                                                    }
                                                                } else if ("protected-broadcasts".equals(cmd)) {
                                                                    dumpState.setDump(1073741824);
                                                                } else if (this.sPmsExt.dumpCmdHandle(cmd, pw, args, opti)) {
                                                                    return;
                                                                }
                                                                permissionNames = null;
                                                            }
                                                            dumpState.setDump(1024);
                                                            permissionNames = null;
                                                        }
                                                        dumpState.setDump(256);
                                                        if (opti < args.length && "noperm".equals(args[opti])) {
                                                            dumpState.setOptionEnabled(4);
                                                        }
                                                        permissionNames = null;
                                                    }
                                                    dumpState.setDump(67108864);
                                                    permissionNames = null;
                                                }
                                                dumpState.setDump(128);
                                                permissionNames = null;
                                            }
                                            dumpState.setDump(262144);
                                            permissionNames = null;
                                        }
                                    }
                                    dumpState.setDump(4096);
                                    permissionNames = null;
                                }
                            }
                            dumpState.setDump(64);
                            permissionNames = null;
                        }
                        if (opti >= args.length) {
                            dumpState.setDump(60);
                            permissionNames = null;
                        } else {
                            while (opti < args.length) {
                                String name = args[opti];
                                if (ActivityTaskManagerService.DUMP_ACTIVITIES_SHORT_CMD.equals(name) || HostingRecord.HOSTING_TYPE_ACTIVITY.equals(name)) {
                                    dumpState.setDump(4);
                                } else if ("s".equals(name) || HostingRecord.HOSTING_TYPE_SERVICE.equals(name)) {
                                    dumpState.setDump(8);
                                } else if (ActivityTaskManagerService.DUMP_RECENTS_SHORT_CMD.equals(name) || ParsingPackageUtils.TAG_RECEIVER.equals(name)) {
                                    dumpState.setDump(16);
                                } else if ("c".equals(name) || ActivityTaskManagerInternal.ASSIST_KEY_CONTENT.equals(name)) {
                                    dumpState.setDump(32);
                                } else {
                                    pw.println("Error: unknown resolver table type: " + name);
                                    return;
                                }
                                opti++;
                            }
                            permissionNames = null;
                        }
                    }
                    dumpState.setDump(2);
                    permissionNames = null;
                }
                dumpState.setDump(1);
                permissionNames = null;
            }
            dumpState.setTargetPackageName(cmd);
            dumpState.setOptionEnabled(1);
            permissionNames = null;
        }
        String packageName3 = dumpState.getTargetPackageName();
        boolean checkin = dumpState.isCheckIn();
        if (packageName3 != null && snapshot.getPackageStateInternal(packageName3) == null && !this.mApexManager.isApexPackage(packageName3)) {
            pw.println("Unable to find package: " + packageName3);
            return;
        }
        if (checkin) {
            pw.println("vers,1");
        }
        if (!checkin && dumpState.isDumping(32768) && packageName3 == null) {
            snapshot.dump(32768, fd, pw, dumpState);
        }
        if (!checkin) {
            if (dumpState.isDumping(134217728) && packageName3 == null) {
                if (dumpState.onTitlePrinted()) {
                    pw.println();
                }
                IndentingPrintWriter ipw = new IndentingPrintWriter(pw, "  ", 120);
                ipw.println("Known Packages:");
                ipw.increaseIndent();
                int i2 = 0;
                while (i2 <= 18) {
                    String knownPackage = KnownPackages.knownPackageToString(i2);
                    ipw.print(knownPackage);
                    ipw.println(":");
                    String[] pkgNames = this.mKnownPackages.getKnownPackageNames(snapshot, i2, 0);
                    ipw.increaseIndent();
                    if (ArrayUtils.isEmpty(pkgNames)) {
                        ipw.println("none");
                    } else {
                        int length = pkgNames.length;
                        int i3 = 0;
                        while (i3 < length) {
                            ipw.println(pkgNames[i3]);
                            i3++;
                            opti = opti;
                        }
                    }
                    ipw.decreaseIndent();
                    i2++;
                    opti = opti;
                }
                ipw.decreaseIndent();
            }
        }
        if (dumpState.isDumping(2048) && packageName3 == null) {
            String requiredVerifierPackage = this.mRequiredVerifierPackage;
            if (!checkin) {
                if (dumpState.onTitlePrinted()) {
                    pw.println();
                }
                pw.println("Verifiers:");
                pw.print("  Required: ");
                pw.print(requiredVerifierPackage);
                pw.print(" (uid=");
                pw.print(snapshot.getPackageUid(requiredVerifierPackage, 268435456L, 0));
                pw.println(")");
            } else if (requiredVerifierPackage != null) {
                pw.print("vrfy,");
                pw.print(requiredVerifierPackage);
                pw.print(",");
                pw.println(snapshot.getPackageUid(requiredVerifierPackage, 268435456L, 0));
            }
        }
        if (dumpState.isDumping(131072) && packageName3 == null) {
            DomainVerificationProxy proxy = this.mDomainVerificationManager.getProxy();
            ComponentName verifierComponent = proxy.getComponentName();
            if (verifierComponent != null) {
                String verifierPackageName = verifierComponent.getPackageName();
                if (!checkin) {
                    if (dumpState.onTitlePrinted()) {
                        pw.println();
                    }
                    pw.println("Domain Verifier:");
                    pw.print("  Using: ");
                    pw.print(verifierPackageName);
                    pw.print(" (uid=");
                    pw.print(snapshot.getPackageUid(verifierPackageName, 268435456L, 0));
                    pw.println(")");
                } else if (verifierPackageName != null) {
                    pw.print("dv,");
                    pw.print(verifierPackageName);
                    pw.print(",");
                    pw.println(snapshot.getPackageUid(verifierPackageName, 268435456L, 0));
                }
            } else {
                pw.println();
                pw.println("No Domain Verifier available!");
            }
        }
        if (dumpState.isDumping(1) && packageName3 == null) {
            snapshot.dump(1, fd, pw, dumpState);
        }
        if (dumpState.isDumping(2) && packageName3 == null) {
            if (dumpState.onTitlePrinted()) {
                pw.println();
            }
            if (!checkin) {
                pw.println("Features:");
            }
            for (FeatureInfo feat : this.mAvailableFeatures.values()) {
                if (!checkin) {
                    pw.print("  ");
                    pw.print(feat.name);
                    if (feat.version > 0) {
                        pw.print(" version=");
                        pw.print(feat.version);
                    }
                    pw.println();
                } else {
                    pw.print("feat,");
                    pw.print(feat.name);
                    pw.print(",");
                    pw.println(feat.version);
                }
            }
        }
        ComponentResolverApi componentResolver = snapshot.getComponentResolver();
        if (!checkin && dumpState.isDumping(4)) {
            componentResolver.dumpActivityResolvers(pw, dumpState, packageName3);
        }
        if (!checkin && dumpState.isDumping(16)) {
            componentResolver.dumpReceiverResolvers(pw, dumpState, packageName3);
        }
        if (!checkin && dumpState.isDumping(8)) {
            componentResolver.dumpServiceResolvers(pw, dumpState, packageName3);
        }
        if (!checkin && dumpState.isDumping(32)) {
            componentResolver.dumpProviderResolvers(pw, dumpState, packageName3);
        }
        if (!checkin && dumpState.isDumping(4096)) {
            snapshot.dump(4096, fd, pw, dumpState);
        }
        if (!checkin && dumpState.isDumping(8192) && packageName3 == null) {
            snapshot.dump(8192, fd, pw, dumpState);
        }
        if (!checkin && dumpState.isDumping(262144)) {
            snapshot.dump(262144, fd, pw, dumpState);
        }
        if (!checkin && dumpState.isDumping(64)) {
            snapshot.dumpPermissions(pw, packageName3, permissionNames, dumpState);
        }
        if (!checkin && dumpState.isDumping(1024)) {
            componentResolver.dumpContentProviders(snapshot, pw, dumpState, packageName3);
        }
        if (!checkin && dumpState.isDumping(16384)) {
            snapshot.dumpKeySet(pw, packageName3, dumpState);
        }
        if (!dumpState.isDumping(128)) {
            str = "  ";
            packageName = packageName3;
            permissionNames2 = permissionNames;
            i = 67108864;
        } else {
            str = "  ";
            permissionNames2 = permissionNames;
            i = 67108864;
            packageName = packageName3;
            snapshot.dumpPackages(pw, packageName3, permissionNames, dumpState, checkin);
        }
        if (!checkin && dumpState.isDumping(i)) {
            snapshot.dump(i, fd, pw, dumpState);
        }
        if (dumpState.isDumping(256)) {
            snapshot.dumpSharedUsers(pw, packageName, permissionNames2, dumpState, checkin);
        }
        if (checkin) {
            packageName2 = packageName;
        } else if (dumpState.isDumping(4194304)) {
            packageName2 = packageName;
            if (packageName2 == null) {
                if (dumpState.onTitlePrinted()) {
                    pw.println();
                }
                pw.println("Package Changes:");
                this.mChangedPackagesTracker.iterateAll(new BiConsumer() { // from class: com.android.server.pm.DumpHelper$$ExternalSyntheticLambda0
                    @Override // java.util.function.BiConsumer
                    public final void accept(Object obj, Object obj2) {
                        DumpHelper.lambda$doDump$0(pw, (Integer) obj, (SparseArray) obj2);
                    }
                });
            }
        } else {
            packageName2 = packageName;
        }
        if (!checkin && dumpState.isDumping(524288) && packageName2 == null) {
            snapshot.dump(524288, fd, pw, dumpState);
        }
        if (!checkin && dumpState.isDumping(8388608) && packageName2 == null) {
            this.mStorageEventHelper.dumpLoadedVolumes(pw, dumpState);
        }
        if (!checkin && dumpState.isDumping(16777216) && packageName2 == null) {
            componentResolver.dumpServicePermissions(pw, dumpState);
        }
        if (!checkin && dumpState.isDumping(1048576)) {
            snapshot.dump(1048576, fd, pw, dumpState);
        }
        if (!checkin && dumpState.isDumping(2097152)) {
            snapshot.dump(2097152, fd, pw, dumpState);
        }
        if (dumpState.isDumping(512) && packageName2 == null) {
            if (!checkin) {
                if (dumpState.onTitlePrinted()) {
                    pw.println();
                }
                snapshot.dump(512, fd, pw, dumpState);
                pw.println();
                pw.println("Package warning messages:");
                PackageManagerServiceUtils.dumpCriticalInfo(pw, null);
            } else {
                PackageManagerServiceUtils.dumpCriticalInfo(pw, "msg,");
            }
        }
        if (!checkin && dumpState.isDumping(65536) && packageName2 == null) {
            if (dumpState.onTitlePrinted()) {
                pw.println();
            }
            this.mInstallerService.dump(new IndentingPrintWriter(pw, str, 120));
        }
        if (!checkin && dumpState.isDumping(33554432) && (packageName2 == null || this.mApexManager.isApexPackage(packageName2))) {
            this.mApexManager.dump(pw, packageName2);
        }
        if (!checkin && dumpState.isDumping(268435456) && packageName2 == null) {
            if (dumpState.onTitlePrinted()) {
                pw.println();
            }
            pw.println("Per UID read timeouts:");
            pw.println("    Default timeouts flag: " + PackageManagerService.getDefaultTimeouts());
            pw.println("    Known digesters list flag: " + PackageManagerService.getKnownDigestersList());
            pw.println("    Timeouts (" + this.mPerUidReadTimeouts.length + "):");
            PerUidReadTimeouts[] perUidReadTimeoutsArr = this.mPerUidReadTimeouts;
            int length2 = perUidReadTimeoutsArr.length;
            int i4 = 0;
            while (i4 < length2) {
                PerUidReadTimeouts item = perUidReadTimeoutsArr[i4];
                pw.print("        (");
                pw.print("uid=" + item.uid + ", ");
                pw.print("minTimeUs=" + item.minTimeUs + ", ");
                pw.print("minPendingTimeUs=" + item.minPendingTimeUs + ", ");
                pw.print("maxPendingTimeUs=" + item.maxPendingTimeUs);
                pw.println(")");
                i4++;
                perUidReadTimeoutsArr = perUidReadTimeoutsArr;
            }
        }
        if (!checkin && dumpState.isDumping(536870912) && packageName2 == null && dumpState.onTitlePrinted()) {
            pw.println();
        }
        if (!checkin && dumpState.isDumping(1073741824) && packageName2 == null) {
            if (dumpState.onTitlePrinted()) {
                pw.println();
            }
            pw.println("Protected broadcast actions:");
            for (int i5 = 0; i5 < this.mProtectedBroadcasts.size(); i5++) {
                pw.print(str);
                pw.println(this.mProtectedBroadcasts.valueAt(i5));
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$doDump$0(PrintWriter pw, Integer sequenceNumber, SparseArray values) {
        pw.print("  Sequence number=");
        pw.println(sequenceNumber);
        int numChangedPackages = values.size();
        for (int i = 0; i < numChangedPackages; i++) {
            SparseArray<String> changes = (SparseArray) values.valueAt(i);
            pw.print("  User ");
            pw.print(values.keyAt(i));
            pw.println(":");
            int numChanges = changes.size();
            if (numChanges == 0) {
                pw.print("    ");
                pw.println("No packages changed");
            } else {
                for (int j = 0; j < numChanges; j++) {
                    String pkgName = changes.valueAt(j);
                    int userSequenceNumber = changes.keyAt(j);
                    pw.print("    ");
                    pw.print("seq=");
                    pw.print(userSequenceNumber);
                    pw.print(", package=");
                    pw.println(pkgName);
                }
            }
        }
    }

    private void printHelp(PrintWriter pw) {
        pw.println("Package manager dump options:");
        pw.println("  [-h] [-f] [--checkin] [--all-components] [cmd] ...");
        pw.println("    --checkin: dump for a checkin");
        pw.println("    -f: print details of intent filters");
        pw.println("    -h: print this help");
        pw.println("    --all-components: include all component names in package dump");
        pw.println("  cmd may be one of:");
        pw.println("    apex: list active APEXes and APEX session state");
        pw.println("    l[ibraries]: list known shared libraries");
        pw.println("    f[eatures]: list device features");
        pw.println("    k[eysets]: print known keysets");
        pw.println("    r[esolvers] [activity|service|receiver|content]: dump intent resolvers");
        pw.println("    perm[issions]: dump permissions");
        pw.println("    permission [name ...]: dump declaration and use of given permission");
        pw.println("    pref[erred]: print preferred package settings");
        pw.println("    preferred-xml [--full]: print preferred package settings as xml");
        pw.println("    prov[iders]: dump content providers");
        pw.println("    p[ackages]: dump installed packages");
        pw.println("    q[ueries]: dump app queryability calculations");
        pw.println("    s[hared-users]: dump shared user IDs");
        pw.println("    m[essages]: print collected runtime messages");
        pw.println("    v[erifiers]: print package verifier info");
        pw.println("    d[omain-preferred-apps]: print domains preferred apps");
        pw.println("    i[ntent-filter-verifiers]|ifv: print intent filter verifier info");
        pw.println("    t[imeouts]: print read timeouts for known digesters");
        pw.println("    version: print database version info");
        pw.println("    write: write current settings now");
        pw.println("    installs: details about install sessions");
        pw.println("    check-permission <permission> <package> [<user>]: does pkg hold perm?");
        pw.println("    dexopt: dump dexopt state");
        pw.println("    compiler-stats: dump compiler statistics");
        pw.println("    service-permissions: dump permissions required by services");
        pw.println("    snapshot: dump snapshot statistics");
        pw.println("    protected-broadcasts: print list of protected broadcast actions");
        pw.println("    known-packages: dump known packages");
        pw.println("    <package.name>: info about given package");
    }

    private void dumpProto(Computer snapshot, FileDescriptor fd) {
        ProtoOutputStream proto = new ProtoOutputStream(fd);
        long requiredVerifierPackageToken = proto.start(1146756268033L);
        proto.write(CompanionAppsPermissions.AppPermissions.PACKAGE_NAME, this.mRequiredVerifierPackage);
        proto.write(1120986464258L, snapshot.getPackageUid(this.mRequiredVerifierPackage, 268435456L, 0));
        proto.end(requiredVerifierPackageToken);
        DomainVerificationProxy proxy = this.mDomainVerificationManager.getProxy();
        ComponentName verifierComponent = proxy.getComponentName();
        if (verifierComponent != null) {
            String verifierPackageName = verifierComponent.getPackageName();
            long verifierPackageToken = proto.start(1146756268034L);
            proto.write(CompanionAppsPermissions.AppPermissions.PACKAGE_NAME, verifierPackageName);
            proto.write(1120986464258L, snapshot.getPackageUid(verifierPackageName, 268435456L, 0));
            proto.end(verifierPackageToken);
        }
        snapshot.dumpSharedLibrariesProto(proto);
        dumpAvailableFeaturesProto(proto);
        snapshot.dumpPackagesProto(proto);
        snapshot.dumpSharedUsersProto(proto);
        PackageManagerServiceUtils.dumpCriticalInfo(proto);
        proto.flush();
    }

    private void dumpAvailableFeaturesProto(ProtoOutputStream proto) {
        int count = this.mAvailableFeatures.size();
        for (int i = 0; i < count; i++) {
            this.mAvailableFeatures.valueAt(i).dumpDebug(proto, 2246267895812L);
        }
    }
}
