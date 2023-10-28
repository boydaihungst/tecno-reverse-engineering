package com.android.server.pm;

import android.accounts.IAccountManager;
import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.app.role.RoleManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.IIntentReceiver;
import android.content.IIntentSender;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ApplicationInfo;
import android.content.pm.FeatureInfo;
import android.content.pm.IPackageDataObserver;
import android.content.pm.IPackageInstaller;
import android.content.pm.IPackageManager;
import android.content.pm.InstrumentationInfo;
import android.content.pm.ModuleInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.pm.ParceledListSlice;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.SharedLibraryInfo;
import android.content.pm.SuspendDialogInfo;
import android.content.pm.UserInfo;
import android.content.pm.VersionedPackage;
import android.content.pm.dex.DexMetadataHelper;
import android.content.pm.dex.ISnapshotRuntimeProfileCallback;
import android.content.pm.parsing.ApkLite;
import android.content.pm.parsing.ApkLiteParseUtils;
import android.content.pm.parsing.PackageLite;
import android.content.pm.parsing.result.ParseResult;
import android.content.pm.parsing.result.ParseTypeImpl;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.rollback.PackageRollbackInfo;
import android.content.rollback.RollbackInfo;
import android.content.rollback.RollbackManager;
import android.hardware.usb.gadget.V1_2.GadgetFunction;
import android.net.Uri;
import android.net.util.NetworkConstants;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.IUserManager;
import android.os.ParcelFileDescriptor;
import android.os.PersistableBundle;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.ServiceSpecificException;
import android.os.ShellCommand;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.incremental.V4Signature;
import android.permission.PermissionManager;
import android.system.ErrnoException;
import android.system.Os;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.IntArray;
import android.util.PrintWriterPrinter;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.content.InstallLocationUtils;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.FrameworkStatsLog;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.Preconditions;
import com.android.server.FgThread;
import com.android.server.LocalServices;
import com.android.server.SystemConfig;
import com.android.server.UiModeManagerService;
import com.android.server.health.HealthServiceWrapperHidl;
import com.android.server.pm.PackageManagerShellCommandDataLoader;
import com.android.server.pm.UserManagerInternal;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.pm.permission.LegacyPermissionManagerInternal;
import com.android.server.pm.pkg.parsing.ParsingPackageUtils;
import com.android.server.pm.verify.domain.DomainVerificationShell;
import com.android.server.slice.SliceClientPermissions;
import com.android.server.timezonedetector.ServiceConfigAccessor;
import com.android.server.vibrator.VibratorManagerService;
import com.android.server.voiceinteraction.DatabaseHelper;
import com.android.server.wm.ActivityTaskManagerService;
import com.transsion.hubcore.server.pm.ITranPackageManagerService;
import dalvik.system.DexFile;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import libcore.io.IoUtils;
import libcore.io.Streams;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class PackageManagerShellCommand extends ShellCommand {
    private static final String ART_PROFILE_SNAPSHOT_DEBUG_LOCATION = "/data/misc/profman/";
    private static final int DEFAULT_STAGED_READY_TIMEOUT_MS = 60000;
    private static final SecureRandom RANDOM;
    private static final String STDIN_PATH = "-";
    private static final Map<String, Integer> SUPPORTED_PERMISSION_FLAGS;
    private static final List<String> SUPPORTED_PERMISSION_FLAGS_LIST;
    private static final String TAG = "PackageManagerShellCommand";
    private static final Set<String> UNSUPPORTED_INSTALL_CMD_OPTS = Set.of("--multi-package");
    private static final Set<String> UNSUPPORTED_SESSION_CREATE_OPTS = Collections.emptySet();
    boolean mBrief;
    boolean mComponents;
    final Context mContext;
    final DomainVerificationShell mDomainVerificationShell;
    final IPackageManager mInterface;
    final PermissionManager mPermissionManager;
    int mQueryFlags;
    int mTargetUser;
    private final WeakHashMap<String, Resources> mResourceCache = new WeakHashMap<>();
    final LegacyPermissionManagerInternal mLegacyPermissionManager = (LegacyPermissionManagerInternal) LocalServices.getService(LegacyPermissionManagerInternal.class);

    static {
        ArrayMap arrayMap = new ArrayMap();
        SUPPORTED_PERMISSION_FLAGS = arrayMap;
        SUPPORTED_PERMISSION_FLAGS_LIST = List.of("review-required", "revoked-compat", "revoke-when-requested", "user-fixed", "user-set");
        arrayMap.put("user-set", 1);
        arrayMap.put("user-fixed", 2);
        arrayMap.put("revoked-compat", 8);
        arrayMap.put("review-required", 64);
        arrayMap.put("revoke-when-requested", 128);
        RANDOM = new SecureRandom();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public PackageManagerShellCommand(IPackageManager packageManager, Context context, DomainVerificationShell domainVerificationShell) {
        this.mInterface = packageManager;
        this.mPermissionManager = (PermissionManager) context.getSystemService(PermissionManager.class);
        this.mContext = context;
        this.mDomainVerificationShell = domainVerificationShell;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public int onCommand(String cmd) {
        char c;
        if (cmd == null) {
            return handleDefaultCommands(cmd);
        }
        PrintWriter pw = getOutPrintWriter();
        try {
            switch (cmd.hashCode()) {
                case -2102802879:
                    if (cmd.equals("set-harmful-app-warning")) {
                        c = '>';
                        break;
                    }
                    c = 65535;
                    break;
                case -1967190973:
                    if (cmd.equals("install-abandon")) {
                        c = 11;
                        break;
                    }
                    c = 65535;
                    break;
                case -1937348290:
                    if (cmd.equals("get-install-location")) {
                        c = 19;
                        break;
                    }
                    c = 65535;
                    break;
                case -1921557090:
                    if (cmd.equals("delete-dexopt")) {
                        c = 28;
                        break;
                    }
                    c = 65535;
                    break;
                case -1852006340:
                    if (cmd.equals("suspend")) {
                        c = '(';
                        break;
                    }
                    c = 65535;
                    break;
                case -1846646502:
                    if (cmd.equals("get-max-running-users")) {
                        c = '9';
                        break;
                    }
                    c = 65535;
                    break;
                case -1741208611:
                    if (cmd.equals("set-installer")) {
                        c = ';';
                        break;
                    }
                    c = 65535;
                    break;
                case -1534455582:
                    if (cmd.equals("set-silent-updates-policy")) {
                        c = 'G';
                        break;
                    }
                    c = 65535;
                    break;
                case -1440979423:
                    if (cmd.equals("cancel-bg-dexopt-job")) {
                        c = 27;
                        break;
                    }
                    c = 65535;
                    break;
                case -1347307837:
                    if (cmd.equals("has-feature")) {
                        c = '=';
                        break;
                    }
                    c = 65535;
                    break;
                case -1298848381:
                    if (cmd.equals("enable")) {
                        c = '!';
                        break;
                    }
                    c = 65535;
                    break;
                case -1267782244:
                    if (cmd.equals("get-instantapp-resolver")) {
                        c = '<';
                        break;
                    }
                    c = 65535;
                    break;
                case -1231004208:
                    if (cmd.equals("resolve-activity")) {
                        c = 4;
                        break;
                    }
                    c = 65535;
                    break;
                case -1102348235:
                    if (cmd.equals("get-privapp-deny-permissions")) {
                        c = '2';
                        break;
                    }
                    c = 65535;
                    break;
                case -1091400553:
                    if (cmd.equals("get-oem-permissions")) {
                        c = '3';
                        break;
                    }
                    c = 65535;
                    break;
                case -1070704814:
                    if (cmd.equals("get-privapp-permissions")) {
                        c = '1';
                        break;
                    }
                    c = 65535;
                    break;
                case -1032029296:
                    if (cmd.equals("disable-user")) {
                        c = '#';
                        break;
                    }
                    c = 65535;
                    break;
                case -944325712:
                    if (cmd.equals("set-distracting-restriction")) {
                        c = '*';
                        break;
                    }
                    c = 65535;
                    break;
                case -934343034:
                    if (cmd.equals("revoke")) {
                        c = ',';
                        break;
                    }
                    c = 65535;
                    break;
                case -919935069:
                    if (cmd.equals("dump-profiles")) {
                        c = 29;
                        break;
                    }
                    c = 65535;
                    break;
                case -840566949:
                    if (cmd.equals("unhide")) {
                        c = '\'';
                        break;
                    }
                    c = 65535;
                    break;
                case -740352344:
                    if (cmd.equals("install-incremental")) {
                        c = '\n';
                        break;
                    }
                    c = 65535;
                    break;
                case -703497631:
                    if (cmd.equals("bypass-staged-installer-check")) {
                        c = 'E';
                        break;
                    }
                    c = 65535;
                    break;
                case -625596190:
                    if (cmd.equals("uninstall")) {
                        c = 31;
                        break;
                    }
                    c = 65535;
                    break;
                case -539710980:
                    if (cmd.equals("create-user")) {
                        c = '5';
                        break;
                    }
                    c = 65535;
                    break;
                case -458695741:
                    if (cmd.equals("query-services")) {
                        c = 6;
                        break;
                    }
                    c = 65535;
                    break;
                case -444750796:
                    if (cmd.equals("bg-dexopt-job")) {
                        c = 26;
                        break;
                    }
                    c = 65535;
                    break;
                case -440994401:
                    if (cmd.equals("query-receivers")) {
                        c = 7;
                        break;
                    }
                    c = 65535;
                    break;
                case -416698598:
                    if (cmd.equals("get-stagedsessions")) {
                        c = '@';
                        break;
                    }
                    c = 65535;
                    break;
                case -339687564:
                    if (cmd.equals("remove-user")) {
                        c = '6';
                        break;
                    }
                    c = 65535;
                    break;
                case -220055275:
                    if (cmd.equals("set-permission-enforced")) {
                        c = '0';
                        break;
                    }
                    c = 65535;
                    break;
                case -140205181:
                    if (cmd.equals("unsuspend")) {
                        c = ')';
                        break;
                    }
                    c = 65535;
                    break;
                case -132384343:
                    if (cmd.equals("install-commit")) {
                        c = '\r';
                        break;
                    }
                    c = 65535;
                    break;
                case -129863314:
                    if (cmd.equals("install-create")) {
                        c = 14;
                        break;
                    }
                    c = 65535;
                    break;
                case -115000827:
                    if (cmd.equals("default-state")) {
                        c = '%';
                        break;
                    }
                    c = 65535;
                    break;
                case -87258188:
                    if (cmd.equals("move-primary-storage")) {
                        c = 22;
                        break;
                    }
                    c = 65535;
                    break;
                case 3292:
                    if (cmd.equals("gc")) {
                        c = 3;
                        break;
                    }
                    c = 65535;
                    break;
                case 3095028:
                    if (cmd.equals("dump")) {
                        c = 1;
                        break;
                    }
                    c = 65535;
                    break;
                case 3202370:
                    if (cmd.equals("hide")) {
                        c = '&';
                        break;
                    }
                    c = 65535;
                    break;
                case 3322014:
                    if (cmd.equals("list")) {
                        c = 2;
                        break;
                    }
                    c = 65535;
                    break;
                case 3433509:
                    if (cmd.equals("path")) {
                        c = 0;
                        break;
                    }
                    c = 65535;
                    break;
                case 18936394:
                    if (cmd.equals("move-package")) {
                        c = 21;
                        break;
                    }
                    c = 65535;
                    break;
                case 86600360:
                    if (cmd.equals("get-max-users")) {
                        c = '8';
                        break;
                    }
                    c = 65535;
                    break;
                case 93657776:
                    if (cmd.equals("install-streaming")) {
                        c = '\t';
                        break;
                    }
                    c = 65535;
                    break;
                case 94746189:
                    if (cmd.equals("clear")) {
                        c = ' ';
                        break;
                    }
                    c = 65535;
                    break;
                case 98615580:
                    if (cmd.equals("grant")) {
                        c = '+';
                        break;
                    }
                    c = 65535;
                    break;
                case 107262333:
                    if (cmd.equals("install-existing")) {
                        c = 17;
                        break;
                    }
                    c = 65535;
                    break;
                case 139892533:
                    if (cmd.equals("get-harmful-app-warning")) {
                        c = '?';
                        break;
                    }
                    c = 65535;
                    break;
                case 237392952:
                    if (cmd.equals("install-add-session")) {
                        c = 20;
                        break;
                    }
                    c = 65535;
                    break;
                case 287820022:
                    if (cmd.equals("install-remove")) {
                        c = 15;
                        break;
                    }
                    c = 65535;
                    break;
                case 359572742:
                    if (cmd.equals("reset-permissions")) {
                        c = '-';
                        break;
                    }
                    c = 65535;
                    break;
                case 377019320:
                    if (cmd.equals("rollback-app")) {
                        c = 'B';
                        break;
                    }
                    c = 65535;
                    break;
                case 467549856:
                    if (cmd.equals("snapshot-profile")) {
                        c = 30;
                        break;
                    }
                    c = 65535;
                    break;
                case 798023112:
                    if (cmd.equals("install-destroy")) {
                        c = '\f';
                        break;
                    }
                    c = 65535;
                    break;
                case 826473335:
                    if (cmd.equals("uninstall-system-updates")) {
                        c = 'A';
                        break;
                    }
                    c = 65535;
                    break;
                case 925176533:
                    if (cmd.equals("set-user-restriction")) {
                        c = '7';
                        break;
                    }
                    c = 65535;
                    break;
                case 950491699:
                    if (cmd.equals("compile")) {
                        c = 23;
                        break;
                    }
                    c = 65535;
                    break;
                case 1053409810:
                    if (cmd.equals("query-activities")) {
                        c = 5;
                        break;
                    }
                    c = 65535;
                    break;
                case 1124603675:
                    if (cmd.equals("force-dex-opt")) {
                        c = 25;
                        break;
                    }
                    c = 65535;
                    break;
                case 1177857340:
                    if (cmd.equals("trim-caches")) {
                        c = '4';
                        break;
                    }
                    c = 65535;
                    break;
                case 1396442249:
                    if (cmd.equals("clear-permission-flags")) {
                        c = '/';
                        break;
                    }
                    c = 65535;
                    break;
                case 1429366290:
                    if (cmd.equals("set-home-activity")) {
                        c = ':';
                        break;
                    }
                    c = 65535;
                    break;
                case 1538306349:
                    if (cmd.equals("install-write")) {
                        c = 16;
                        break;
                    }
                    c = 65535;
                    break;
                case 1671308008:
                    if (cmd.equals("disable")) {
                        c = '\"';
                        break;
                    }
                    c = 65535;
                    break;
                case 1697997009:
                    if (cmd.equals("disable-until-used")) {
                        c = '$';
                        break;
                    }
                    c = 65535;
                    break;
                case 1738820372:
                    if (cmd.equals("set-permission-flags")) {
                        c = '.';
                        break;
                    }
                    c = 65535;
                    break;
                case 1746695602:
                    if (cmd.equals("set-install-location")) {
                        c = 18;
                        break;
                    }
                    c = 65535;
                    break;
                case 1757370437:
                    if (cmd.equals("bypass-allowed-apex-update-check")) {
                        c = 'F';
                        break;
                    }
                    c = 65535;
                    break;
                case 1783979817:
                    if (cmd.equals("reconcile-secondary-dex-files")) {
                        c = 24;
                        break;
                    }
                    c = 65535;
                    break;
                case 1824799035:
                    if (cmd.equals("log-visibility")) {
                        c = 'D';
                        break;
                    }
                    c = 65535;
                    break;
                case 1858863089:
                    if (cmd.equals("get-moduleinfo")) {
                        c = 'C';
                        break;
                    }
                    c = 65535;
                    break;
                case 1957569947:
                    if (cmd.equals("install")) {
                        c = '\b';
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
                    return runPath();
                case 1:
                    return runDump();
                case 2:
                    return runList();
                case 3:
                    return runGc();
                case 4:
                    return runResolveActivity();
                case 5:
                    return runQueryIntentActivities();
                case 6:
                    return runQueryIntentServices();
                case 7:
                    return runQueryIntentReceivers();
                case '\b':
                    return runInstall();
                case '\t':
                    return runStreamingInstall();
                case '\n':
                    return runIncrementalInstall();
                case 11:
                case '\f':
                    return runInstallAbandon();
                case '\r':
                    return runInstallCommit();
                case 14:
                    return runInstallCreate();
                case 15:
                    return runInstallRemove();
                case 16:
                    return runInstallWrite();
                case 17:
                    return runInstallExisting();
                case 18:
                    return runSetInstallLocation();
                case 19:
                    return runGetInstallLocation();
                case 20:
                    return runInstallAddSession();
                case 21:
                    return runMovePackage();
                case 22:
                    return runMovePrimaryStorage();
                case 23:
                    return runCompile();
                case 24:
                    return runreconcileSecondaryDexFiles();
                case 25:
                    return runForceDexOpt();
                case 26:
                    return runDexoptJob();
                case 27:
                    return cancelBgDexOptJob();
                case 28:
                    return runDeleteDexOpt();
                case 29:
                    return runDumpProfiles();
                case 30:
                    return runSnapshotProfile();
                case 31:
                    return runUninstall();
                case ' ':
                    return runClear();
                case '!':
                    return runSetEnabledSetting(1);
                case '\"':
                    return runSetEnabledSetting(2);
                case '#':
                    return runSetEnabledSetting(3);
                case '$':
                    return runSetEnabledSetting(4);
                case '%':
                    return runSetEnabledSetting(0);
                case '&':
                    return runSetHiddenSetting(true);
                case '\'':
                    return runSetHiddenSetting(false);
                case '(':
                    return runSuspend(true);
                case ')':
                    return runSuspend(false);
                case '*':
                    return runSetDistractingRestriction();
                case '+':
                    return runGrantRevokePermission(true);
                case ',':
                    return runGrantRevokePermission(false);
                case '-':
                    return runResetPermissions();
                case '.':
                    return setOrClearPermissionFlags(true);
                case '/':
                    return setOrClearPermissionFlags(false);
                case '0':
                    return runSetPermissionEnforced();
                case '1':
                    return runGetPrivappPermissions();
                case '2':
                    return runGetPrivappDenyPermissions();
                case '3':
                    return runGetOemPermissions();
                case '4':
                    return runTrimCaches();
                case '5':
                    return runCreateUser();
                case '6':
                    return runRemoveUser();
                case '7':
                    return runSetUserRestriction();
                case '8':
                    return runGetMaxUsers();
                case '9':
                    return runGetMaxRunningUsers();
                case ':':
                    return runSetHomeActivity();
                case ';':
                    return runSetInstaller();
                case '<':
                    return runGetInstantAppResolver();
                case '=':
                    return runHasFeature();
                case '>':
                    return runSetHarmfulAppWarning();
                case '?':
                    return runGetHarmfulAppWarning();
                case '@':
                    return runListStagedSessions();
                case 'A':
                    String packageName = getNextArg();
                    return uninstallSystemUpdates(packageName);
                case 'B':
                    return runRollbackApp();
                case 'C':
                    return runGetModuleInfo();
                case 'D':
                    return runLogVisibility();
                case 'E':
                    return runBypassStagedInstallerCheck();
                case 'F':
                    return runBypassAllowedApexUpdateCheck();
                case 'G':
                    return runSetSilentUpdatesPolicy();
                default:
                    Boolean domainVerificationResult = this.mDomainVerificationShell.runCommand(this, cmd);
                    if (domainVerificationResult != null) {
                        return !domainVerificationResult.booleanValue();
                    }
                    String nextArg = getNextArg();
                    if (nextArg == null) {
                        if (cmd.equalsIgnoreCase("-l")) {
                            return runListPackages(false);
                        }
                        if (cmd.equalsIgnoreCase("-lf")) {
                            return runListPackages(true);
                        }
                    } else if (getNextArg() == null && cmd.equalsIgnoreCase("-p")) {
                        return displayPackageFilePath(nextArg, 0);
                    }
                    return handleDefaultCommands(cmd);
            }
        } catch (RemoteException e) {
            pw.println("Remote exception: " + e);
            return -1;
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code restructure failed: missing block: B:9:0x001c, code lost:
        if (r2.equals("--installed") != false) goto L8;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private int runGetModuleInfo() {
        PrintWriter pw = getOutPrintWriter();
        int flags = 0;
        while (true) {
            String opt = getNextOption();
            boolean z = true;
            if (opt != null) {
                switch (opt.hashCode()) {
                    case 42995713:
                        if (opt.equals("--all")) {
                            z = false;
                            break;
                        }
                        z = true;
                        break;
                    case 517440986:
                        break;
                    default:
                        z = true;
                        break;
                }
                switch (z) {
                    case false:
                        flags |= 131072;
                        break;
                    case true:
                        break;
                    default:
                        pw.println("Error: Unknown option: " + opt);
                        return -1;
                }
            } else {
                String moduleName = getNextArg();
                try {
                    if (moduleName != null) {
                        ModuleInfo m = this.mInterface.getModuleInfo(moduleName, flags);
                        pw.println(m.toString() + " packageName: " + m.getPackageName());
                    } else {
                        List<ModuleInfo> modules = this.mInterface.getInstalledModules(flags);
                        for (ModuleInfo m2 : modules) {
                            pw.println(m2.toString() + " packageName: " + m2.getPackageName());
                        }
                    }
                    return 1;
                } catch (RemoteException e) {
                    pw.println("Failure [" + e.getClass().getName() + " - " + e.getMessage() + "]");
                    return -1;
                }
            }
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code restructure failed: missing block: B:9:0x001c, code lost:
        if (r2.equals("--enable") != false) goto L8;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private int runLogVisibility() {
        PrintWriter pw = getOutPrintWriter();
        boolean enable = true;
        while (true) {
            String opt = getNextOption();
            boolean z = true;
            if (opt != null) {
                switch (opt.hashCode()) {
                    case -1237677752:
                        if (opt.equals("--disable")) {
                            z = false;
                            break;
                        }
                        z = true;
                        break;
                    case 1101165347:
                        break;
                    default:
                        z = true;
                        break;
                }
                switch (z) {
                    case false:
                        enable = false;
                        break;
                    case true:
                        enable = true;
                        break;
                    default:
                        pw.println("Error: Unknown option: " + opt);
                        return -1;
                }
            } else {
                String packageName = getNextArg();
                if (packageName != null) {
                    ((PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class)).setVisibilityLogging(packageName, enable);
                    return 1;
                }
                getErrPrintWriter().println("Error: no package specified");
                return -1;
            }
        }
    }

    private int runBypassStagedInstallerCheck() {
        PrintWriter pw = getOutPrintWriter();
        try {
            this.mInterface.getPackageInstaller().bypassNextStagedInstallerCheck(Boolean.parseBoolean(getNextArg()));
            return 0;
        } catch (RemoteException e) {
            pw.println("Failure [" + e.getClass().getName() + " - " + e.getMessage() + "]");
            return -1;
        }
    }

    private int runBypassAllowedApexUpdateCheck() {
        PrintWriter pw = getOutPrintWriter();
        try {
            this.mInterface.getPackageInstaller().bypassNextAllowedApexUpdateCheck(Boolean.parseBoolean(getNextArg()));
            return 0;
        } catch (RemoteException e) {
            pw.println("Failure [" + e.getClass().getName() + " - " + e.getMessage() + "]");
            return -1;
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    private int uninstallSystemUpdates(String packageName) {
        List<ApplicationInfo> list;
        PrintWriter pw = getOutPrintWriter();
        boolean failedUninstalls = false;
        try {
            IPackageInstaller installer = this.mInterface.getPackageInstaller();
            if (packageName == null) {
                ParceledListSlice<ApplicationInfo> packages = this.mInterface.getInstalledApplications(1056768L, 0);
                list = packages.getList();
            } else {
                List<ApplicationInfo> list2 = new ArrayList<>(1);
                list2.add(this.mInterface.getApplicationInfo(packageName, 1056768L, 0));
                list = list2;
            }
            for (ApplicationInfo info : list) {
                if (info.isUpdatedSystemApp()) {
                    pw.println("Uninstalling updates to " + info.packageName + "...");
                    LocalIntentReceiver receiver = new LocalIntentReceiver();
                    installer.uninstall(new VersionedPackage(info.packageName, info.versionCode), (String) null, 0, receiver.getIntentSender(), 0);
                    Intent result = receiver.getResult();
                    int status = result.getIntExtra("android.content.pm.extra.STATUS", 1);
                    if (status != 0) {
                        failedUninstalls = true;
                        pw.println("Couldn't uninstall package: " + info.packageName);
                    }
                }
            }
            if (failedUninstalls) {
                return 0;
            }
            pw.println("Success");
            return 1;
        } catch (RemoteException e) {
            pw.println("Failure [" + e.getClass().getName() + " - " + e.getMessage() + "]");
            return 0;
        }
    }

    /* JADX WARN: Code restructure failed: missing block: B:9:0x0020, code lost:
        if (r0.equals("--staged-ready-timeout") != false) goto L8;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private int runRollbackApp() throws RemoteException {
        PrintWriter pw = getOutPrintWriter();
        long stagedReadyTimeoutMs = 60000;
        while (true) {
            String opt = getNextOption();
            boolean z = false;
            if (opt != null) {
                switch (opt.hashCode()) {
                    case -158482320:
                        break;
                    default:
                        z = true;
                        break;
                }
                switch (z) {
                    case false:
                        stagedReadyTimeoutMs = Long.parseLong(getNextArgRequired());
                    default:
                        throw new IllegalArgumentException("Unknown option: " + opt);
                }
            } else {
                String packageName = getNextArgRequired();
                if (packageName == null) {
                    pw.println("Error: package name not specified");
                    return 1;
                }
                try {
                    Context shellPackageContext = this.mContext.createPackageContextAsUser(VibratorManagerService.VibratorManagerShellCommand.SHELL_PACKAGE_NAME, 0, Binder.getCallingUserHandle());
                    LocalIntentReceiver receiver = new LocalIntentReceiver();
                    RollbackManager rm = (RollbackManager) shellPackageContext.getSystemService(RollbackManager.class);
                    RollbackInfo rollback = null;
                    for (RollbackInfo r : rm.getAvailableRollbacks()) {
                        Iterator it = r.getPackages().iterator();
                        while (true) {
                            if (it.hasNext()) {
                                PackageRollbackInfo info = (PackageRollbackInfo) it.next();
                                if (packageName.equals(info.getPackageName())) {
                                    rollback = r;
                                }
                            }
                        }
                    }
                    if (rollback == null) {
                        pw.println("No available rollbacks for: " + packageName);
                        return 1;
                    }
                    rm.commitRollback(rollback.getRollbackId(), Collections.emptyList(), receiver.getIntentSender());
                    Intent result = receiver.getResult();
                    int status = result.getIntExtra("android.content.rollback.extra.STATUS", 1);
                    if (status != 0) {
                        pw.println("Failure [" + result.getStringExtra("android.content.rollback.extra.STATUS_MESSAGE") + "]");
                        return 1;
                    } else if (rollback.isStaged() && stagedReadyTimeoutMs > 0) {
                        int committedSessionId = rollback.getCommittedSessionId();
                        return doWaitForStagedSessionReady(committedSessionId, stagedReadyTimeoutMs, pw);
                    } else {
                        pw.println("Success");
                        return 0;
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private void setParamsSize(InstallParams params, List<String> inPaths) {
        if (params.sessionParams.sizeBytes == -1 && !STDIN_PATH.equals(inPaths.get(0))) {
            long sessionSize = 0;
            ParseTypeImpl input = ParseTypeImpl.forDefaultParsing();
            for (String inPath : inPaths) {
                ParcelFileDescriptor fd = openFileForSystem(inPath, ActivityTaskManagerService.DUMP_RECENTS_SHORT_CMD);
                if (fd == null) {
                    getErrPrintWriter().println("Error: Can't open file: " + inPath);
                    throw new IllegalArgumentException("Error: Can't open file: " + inPath);
                }
                try {
                    try {
                        ParseResult<ApkLite> apkLiteResult = ApkLiteParseUtils.parseApkLite(input.reset(), fd.getFileDescriptor(), inPath, 0);
                        if (apkLiteResult.isError()) {
                            throw new IllegalArgumentException("Error: Failed to parse APK file: " + inPath + ": " + apkLiteResult.getErrorMessage(), apkLiteResult.getException());
                        }
                        ApkLite apkLite = (ApkLite) apkLiteResult.getResult();
                        PackageLite pkgLite = new PackageLite((String) null, apkLite.getPath(), apkLite, (String[]) null, (boolean[]) null, (String[]) null, (String[]) null, (String[]) null, (int[]) null, apkLite.getTargetSdkVersion(), (Set[]) null, (Set[]) null);
                        sessionSize += InstallLocationUtils.calculateInstalledSize(pkgLite, params.sessionParams.abiOverride, fd.getFileDescriptor());
                        try {
                            fd.close();
                        } catch (IOException e) {
                        }
                    } catch (IOException e2) {
                        getErrPrintWriter().println("Error: Failed to parse APK file: " + inPath);
                        throw new IllegalArgumentException("Error: Failed to parse APK file: " + inPath, e2);
                    }
                } catch (Throwable th) {
                    try {
                        fd.close();
                    } catch (IOException e3) {
                    }
                    throw th;
                }
            }
            params.sessionParams.setSize(sessionSize);
        }
    }

    private int displayPackageFilePath(String pckg, int userId) throws RemoteException {
        String[] strArr;
        PackageInfo info = this.mInterface.getPackageInfo(pckg, 1073741824L, userId);
        if (info != null && info.applicationInfo != null) {
            PrintWriter pw = getOutPrintWriter();
            pw.print("package:");
            pw.println(info.applicationInfo.sourceDir);
            if (!ArrayUtils.isEmpty(info.applicationInfo.splitSourceDirs)) {
                for (String splitSourceDir : info.applicationInfo.splitSourceDirs) {
                    pw.print("package:");
                    pw.println(splitSourceDir);
                }
            }
            return 0;
        }
        return 1;
    }

    private int runPath() throws RemoteException {
        int userId = 0;
        String option = getNextOption();
        if (option != null && option.equals("--user")) {
            userId = UserHandle.parseUserArg(getNextArgRequired());
        }
        String pkg = getNextArgRequired();
        if (pkg == null) {
            getErrPrintWriter().println("Error: no package specified");
            return 1;
        }
        int translatedUserId = translateUserId(userId, -10000, "runPath");
        return displayPackageFilePath(pkg, translatedUserId);
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    private int runList() throws RemoteException {
        char c;
        PrintWriter pw = getOutPrintWriter();
        String type = getNextArg();
        if (type != null) {
            switch (type.hashCode()) {
                case -1126096540:
                    if (type.equals("staged-sessions")) {
                        c = 7;
                        break;
                    }
                    c = 65535;
                    break;
                case -997447790:
                    if (type.equals("permission-groups")) {
                        c = 5;
                        break;
                    }
                    c = 65535;
                    break;
                case -807062458:
                    if (type.equals("package")) {
                        c = 3;
                        break;
                    }
                    c = 65535;
                    break;
                case -290659267:
                    if (type.equals("features")) {
                        c = 0;
                        break;
                    }
                    c = 65535;
                    break;
                case 3525497:
                    if (type.equals("sdks")) {
                        c = '\b';
                        break;
                    }
                    c = 65535;
                    break;
                case 111578632:
                    if (type.equals(DatabaseHelper.SoundModelContract.KEY_USERS)) {
                        c = '\t';
                        break;
                    }
                    c = 65535;
                    break;
                case 544550766:
                    if (type.equals(ParsingPackageUtils.TAG_INSTRUMENTATION)) {
                        c = 1;
                        break;
                    }
                    c = 65535;
                    break;
                case 750867693:
                    if (type.equals("packages")) {
                        c = 4;
                        break;
                    }
                    c = 65535;
                    break;
                case 812757657:
                    if (type.equals("libraries")) {
                        c = 2;
                        break;
                    }
                    c = 65535;
                    break;
                case 1133704324:
                    if (type.equals("permissions")) {
                        c = 6;
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
                    return runListFeatures();
                case 1:
                    return runListInstrumentation();
                case 2:
                    return runListLibraries();
                case 3:
                case 4:
                    return runListPackages(false);
                case 5:
                    return runListPermissionGroups();
                case 6:
                    return runListPermissions();
                case 7:
                    return runListStagedSessions();
                case '\b':
                    return runListSdks();
                case '\t':
                    ServiceManager.getService("user").shellCommand(getInFileDescriptor(), getOutFileDescriptor(), getErrFileDescriptor(), new String[]{"list"}, getShellCallback(), adoptResultReceiver());
                    return 0;
                default:
                    pw.println("Error: unknown list type '" + type + "'");
                    return -1;
            }
        }
        pw.println("Error: didn't specify type of data to list");
        return -1;
    }

    private int runGc() throws RemoteException {
        Runtime.getRuntime().gc();
        PrintWriter pw = getOutPrintWriter();
        pw.println("Ok");
        return 0;
    }

    private int runListFeatures() throws RemoteException {
        PrintWriter pw = getOutPrintWriter();
        List<FeatureInfo> list = this.mInterface.getSystemAvailableFeatures().getList();
        Collections.sort(list, new Comparator<FeatureInfo>() { // from class: com.android.server.pm.PackageManagerShellCommand.1
            /* JADX DEBUG: Method merged with bridge method */
            @Override // java.util.Comparator
            public int compare(FeatureInfo o1, FeatureInfo o2) {
                if (o1.name == o2.name) {
                    return 0;
                }
                if (o1.name == null) {
                    return -1;
                }
                if (o2.name == null) {
                    return 1;
                }
                return o1.name.compareTo(o2.name);
            }
        });
        int count = list != null ? list.size() : 0;
        for (int p = 0; p < count; p++) {
            FeatureInfo fi = list.get(p);
            pw.print("feature:");
            if (fi.name != null) {
                pw.print(fi.name);
                if (fi.version > 0) {
                    pw.print("=");
                    pw.print(fi.version);
                }
                pw.println();
            } else {
                pw.println("reqGlEsVersion=0x" + Integer.toHexString(fi.reqGlEsVersion));
            }
        }
        return 0;
    }

    private int runListInstrumentation() throws RemoteException {
        boolean z;
        PrintWriter pw = getOutPrintWriter();
        boolean showSourceDir = false;
        String targetPackage = null;
        while (true) {
            try {
                String opt = getNextArg();
                if (opt != null) {
                    switch (opt.hashCode()) {
                        case 1497:
                            if (opt.equals("-f")) {
                                z = false;
                                break;
                            }
                        default:
                            z = true;
                            break;
                    }
                    switch (z) {
                        case false:
                            showSourceDir = true;
                            break;
                        default:
                            if (opt.charAt(0) != '-') {
                                targetPackage = opt;
                                break;
                            } else {
                                pw.println("Error: Unknown option: " + opt);
                                return -1;
                            }
                    }
                } else {
                    List<InstrumentationInfo> list = this.mInterface.queryInstrumentation(targetPackage, 0).getList();
                    Collections.sort(list, new Comparator<InstrumentationInfo>() { // from class: com.android.server.pm.PackageManagerShellCommand.2
                        /* JADX DEBUG: Method merged with bridge method */
                        @Override // java.util.Comparator
                        public int compare(InstrumentationInfo o1, InstrumentationInfo o2) {
                            return o1.targetPackage.compareTo(o2.targetPackage);
                        }
                    });
                    int count = list != null ? list.size() : 0;
                    for (int p = 0; p < count; p++) {
                        InstrumentationInfo ii = list.get(p);
                        pw.print("instrumentation:");
                        if (showSourceDir) {
                            pw.print(ii.sourceDir);
                            pw.print("=");
                        }
                        ComponentName cn = new ComponentName(ii.packageName, ii.name);
                        pw.print(cn.flattenToShortString());
                        pw.print(" (target=");
                        pw.print(ii.targetPackage);
                        pw.println(")");
                    }
                    return 0;
                }
            } catch (RuntimeException ex) {
                pw.println("Error: " + ex.toString());
                return -1;
            }
        }
    }

    private int runListLibraries() throws RemoteException {
        PrintWriter pw = getOutPrintWriter();
        List<String> list = new ArrayList<>();
        String[] rawList = this.mInterface.getSystemSharedLibraryNames();
        for (String str : rawList) {
            list.add(str);
        }
        Collections.sort(list, new Comparator<String>() { // from class: com.android.server.pm.PackageManagerShellCommand.3
            /* JADX DEBUG: Method merged with bridge method */
            @Override // java.util.Comparator
            public int compare(String o1, String o2) {
                if (o1 == o2) {
                    return 0;
                }
                if (o1 == null) {
                    return -1;
                }
                if (o2 == null) {
                    return 1;
                }
                return o1.compareTo(o2);
            }
        });
        int count = list.size();
        for (int p = 0; p < count; p++) {
            String lib = list.get(p);
            pw.print("library:");
            pw.println(lib);
        }
        return 0;
    }

    private int runListPackages(boolean showSourceDir) throws RemoteException {
        return runListPackages(showSourceDir, false);
    }

    private int runListSdks() throws RemoteException {
        return runListPackages(false, true);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [917=4] */
    /* JADX WARN: Removed duplicated region for block: B:121:0x026e  */
    /* JADX WARN: Removed duplicated region for block: B:124:0x0279  */
    /* JADX WARN: Removed duplicated region for block: B:127:0x027e A[ADDED_TO_REGION] */
    /* JADX WARN: Removed duplicated region for block: B:137:0x029b A[ADDED_TO_REGION] */
    /* JADX WARN: Removed duplicated region for block: B:141:0x02ad  */
    /* JADX WARN: Removed duplicated region for block: B:154:0x031c  */
    /* JADX WARN: Removed duplicated region for block: B:157:0x032f  */
    /* JADX WARN: Removed duplicated region for block: B:160:0x0340  */
    /* JADX WARN: Removed duplicated region for block: B:165:0x035a  */
    /* JADX WARN: Removed duplicated region for block: B:168:0x037e A[ADDED_TO_REGION] */
    /* JADX WARN: Removed duplicated region for block: B:234:0x038b A[ADDED_TO_REGION, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private int runListPackages(boolean showSourceDir, boolean showSdks) throws RemoteException {
        PrintWriter pw;
        List<PackageInfo> packages;
        int count;
        boolean z;
        boolean isSystem;
        String filter;
        boolean isEnabled;
        int getFlags;
        boolean listDisabled;
        boolean listEnabled;
        boolean listSystem;
        String name;
        Map<String, List<String>> out;
        List<String> uids;
        String opt;
        char c;
        String opt2;
        char c2;
        String prefix = showSdks ? "sdk:" : "package:";
        PrintWriter pw2 = getOutPrintWriter();
        int uid = -1;
        int defaultUserId = -1;
        boolean showVersionCode = false;
        boolean listApexOnly = false;
        boolean showVersionCode2 = false;
        int defaultUserId2 = 0;
        boolean listInstaller = false;
        boolean showUid = false;
        boolean listSystem2 = false;
        boolean listThirdParty = false;
        boolean listDisabled2 = showSourceDir;
        int getFlags2 = 0;
        while (true) {
            int showSourceDir2 = defaultUserId2;
            try {
                String opt3 = getNextOption();
                if (opt3 == null) {
                    boolean listInstaller2 = showVersionCode2;
                    String filter2 = getNextArg();
                    int[] userIds = {defaultUserId};
                    if (defaultUserId == -1) {
                        UserManagerInternal umi = (UserManagerInternal) LocalServices.getService(UserManagerInternal.class);
                        userIds = umi.getUserIds();
                    }
                    if (showSdks) {
                        getFlags2 |= 67108864;
                    }
                    Map<String, List<String>> out2 = new HashMap<>();
                    int defaultUserId3 = userIds.length;
                    PrintWriter pw3 = pw2;
                    int p = 0;
                    while (p < defaultUserId3) {
                        int i = defaultUserId3;
                        int userId = userIds[p];
                        int[] userIds2 = userIds;
                        int i2 = p;
                        int translatedUserId = translateUserId(userId, 0, "runListPackages");
                        Map<String, List<String>> out3 = out2;
                        boolean showVersionCode3 = showVersionCode;
                        ParceledListSlice<PackageInfo> slice = this.mInterface.getInstalledPackages(getFlags2, translatedUserId);
                        List<PackageInfo> packages2 = slice.getList();
                        int count2 = packages2.size();
                        int p2 = 0;
                        while (p2 < count2) {
                            int translatedUserId2 = translatedUserId;
                            PackageInfo info = packages2.get(p2);
                            StringBuilder stringBuilder = new StringBuilder();
                            if (filter2 != null) {
                                packages = packages2;
                                if (!info.packageName.contains(filter2)) {
                                    filter = filter2;
                                    getFlags = getFlags2;
                                    listDisabled = listSystem2;
                                    listEnabled = listThirdParty;
                                    listSystem = listInstaller;
                                    count = count2;
                                    out = out3;
                                    p2++;
                                    out3 = out;
                                    packages2 = packages;
                                    translatedUserId = translatedUserId2;
                                    count2 = count;
                                    filter2 = filter;
                                    listSystem2 = listDisabled;
                                    listInstaller = listSystem;
                                    listThirdParty = listEnabled;
                                    getFlags2 = getFlags;
                                }
                            } else {
                                packages = packages2;
                            }
                            boolean isApex = info.isApex;
                            count = count2;
                            if (uid == -1 || isApex || info.applicationInfo.uid == uid) {
                                if (isApex) {
                                    z = true;
                                } else {
                                    z = true;
                                    if ((info.applicationInfo.flags & 1) != 0) {
                                        isSystem = true;
                                        if (isApex) {
                                            filter = filter2;
                                            if (info.applicationInfo.enabled) {
                                                isEnabled = z;
                                                if ((!listSystem2 && isEnabled) || ((listThirdParty && !isEnabled) || ((listInstaller && !isSystem) || (showUid && isSystem)))) {
                                                    getFlags = getFlags2;
                                                    listDisabled = listSystem2;
                                                    listEnabled = listThirdParty;
                                                    listSystem = listInstaller;
                                                    out = out3;
                                                } else if (listApexOnly || isApex) {
                                                    String name2 = null;
                                                    if (showSdks) {
                                                        listDisabled = listSystem2;
                                                        listEnabled = listThirdParty;
                                                        listSystem = listInstaller;
                                                        ParceledListSlice<SharedLibraryInfo> libsSlice = this.mInterface.getDeclaredSharedLibraries(info.packageName, getFlags2, userId);
                                                        if (libsSlice == null) {
                                                            getFlags = getFlags2;
                                                            out = out3;
                                                        } else {
                                                            List<SharedLibraryInfo> libs = libsSlice.getList();
                                                            int l = 0;
                                                            int lsize = libs.size();
                                                            while (true) {
                                                                if (l < lsize) {
                                                                    SharedLibraryInfo lib = libs.get(l);
                                                                    ParceledListSlice<SharedLibraryInfo> libsSlice2 = libsSlice;
                                                                    getFlags = getFlags2;
                                                                    if (lib.getType() == 3) {
                                                                        name2 = lib.getName() + ":" + lib.getLongVersion();
                                                                    } else {
                                                                        l++;
                                                                        libsSlice = libsSlice2;
                                                                        getFlags2 = getFlags;
                                                                    }
                                                                } else {
                                                                    getFlags = getFlags2;
                                                                }
                                                            }
                                                            if (name2 == null) {
                                                                out = out3;
                                                            } else {
                                                                name = name2;
                                                            }
                                                        }
                                                    } else {
                                                        getFlags = getFlags2;
                                                        listDisabled = listSystem2;
                                                        listEnabled = listThirdParty;
                                                        listSystem = listInstaller;
                                                        name = info.packageName;
                                                    }
                                                    stringBuilder.append(prefix);
                                                    if (listDisabled2) {
                                                        stringBuilder.append(info.applicationInfo.sourceDir);
                                                        stringBuilder.append("=");
                                                    }
                                                    stringBuilder.append(name);
                                                    if (showVersionCode3) {
                                                        stringBuilder.append(" versionCode:");
                                                        if (info.applicationInfo != null) {
                                                            stringBuilder.append(info.applicationInfo.longVersionCode);
                                                        } else {
                                                            stringBuilder.append(info.getLongVersionCode());
                                                        }
                                                    }
                                                    if (listInstaller2) {
                                                        stringBuilder.append("  installer=");
                                                        stringBuilder.append(this.mInterface.getInstallerPackageName(info.packageName));
                                                    }
                                                    out = out3;
                                                    uids = out.computeIfAbsent(stringBuilder.toString(), new Function() { // from class: com.android.server.pm.PackageManagerShellCommand$$ExternalSyntheticLambda0
                                                        @Override // java.util.function.Function
                                                        public final Object apply(Object obj) {
                                                            return PackageManagerShellCommand.lambda$runListPackages$0((String) obj);
                                                        }
                                                    });
                                                    if (showSourceDir2 == 0 && !isApex) {
                                                        uids.add(String.valueOf(info.applicationInfo.uid));
                                                    }
                                                } else {
                                                    getFlags = getFlags2;
                                                    listDisabled = listSystem2;
                                                    listEnabled = listThirdParty;
                                                    listSystem = listInstaller;
                                                    out = out3;
                                                }
                                            }
                                        } else {
                                            filter = filter2;
                                        }
                                        isEnabled = false;
                                        if (!listSystem2) {
                                        }
                                        if (listApexOnly) {
                                        }
                                        String name22 = null;
                                        if (showSdks) {
                                        }
                                        stringBuilder.append(prefix);
                                        if (listDisabled2) {
                                        }
                                        stringBuilder.append(name);
                                        if (showVersionCode3) {
                                        }
                                        if (listInstaller2) {
                                        }
                                        out = out3;
                                        uids = out.computeIfAbsent(stringBuilder.toString(), new Function() { // from class: com.android.server.pm.PackageManagerShellCommand$$ExternalSyntheticLambda0
                                            @Override // java.util.function.Function
                                            public final Object apply(Object obj) {
                                                return PackageManagerShellCommand.lambda$runListPackages$0((String) obj);
                                            }
                                        });
                                        if (showSourceDir2 == 0) {
                                            uids.add(String.valueOf(info.applicationInfo.uid));
                                        }
                                    }
                                }
                                isSystem = false;
                                if (isApex) {
                                }
                                isEnabled = false;
                                if (!listSystem2) {
                                }
                                if (listApexOnly) {
                                }
                                String name222 = null;
                                if (showSdks) {
                                }
                                stringBuilder.append(prefix);
                                if (listDisabled2) {
                                }
                                stringBuilder.append(name);
                                if (showVersionCode3) {
                                }
                                if (listInstaller2) {
                                }
                                out = out3;
                                uids = out.computeIfAbsent(stringBuilder.toString(), new Function() { // from class: com.android.server.pm.PackageManagerShellCommand$$ExternalSyntheticLambda0
                                    @Override // java.util.function.Function
                                    public final Object apply(Object obj) {
                                        return PackageManagerShellCommand.lambda$runListPackages$0((String) obj);
                                    }
                                });
                                if (showSourceDir2 == 0) {
                                }
                            } else {
                                filter = filter2;
                                getFlags = getFlags2;
                                listDisabled = listSystem2;
                                listEnabled = listThirdParty;
                                listSystem = listInstaller;
                                out = out3;
                            }
                            p2++;
                            out3 = out;
                            packages2 = packages;
                            translatedUserId = translatedUserId2;
                            count2 = count;
                            filter2 = filter;
                            listSystem2 = listDisabled;
                            listInstaller = listSystem;
                            listThirdParty = listEnabled;
                            getFlags2 = getFlags;
                        }
                        boolean listSystem3 = listInstaller;
                        p = i2 + 1;
                        out2 = out3;
                        defaultUserId3 = i;
                        userIds = userIds2;
                        showVersionCode = showVersionCode3;
                        listInstaller = listSystem3;
                    }
                    for (Map.Entry<String, List<String>> entry : out2.entrySet()) {
                        PrintWriter pw4 = pw3;
                        pw4.print(entry.getKey());
                        List<String> uids2 = entry.getValue();
                        if (!uids2.isEmpty()) {
                            pw4.print(" uid:");
                            pw4.print(String.join(",", uids2));
                        }
                        pw4.println();
                        pw3 = pw4;
                    }
                    return 0;
                }
                try {
                    switch (opt3.hashCode()) {
                        case -493830763:
                            opt = opt3;
                            if (opt.equals("--show-versioncode")) {
                                c = '\n';
                                break;
                            }
                            c = 65535;
                            break;
                        case 1446:
                            opt = opt3;
                            if (opt.equals("-3")) {
                                c = '\t';
                                break;
                            }
                            c = 65535;
                            break;
                        case 1480:
                            opt = opt3;
                            if (opt.equals("-U")) {
                                c = 7;
                                break;
                            }
                            c = 65535;
                            break;
                        case 1492:
                            opt = opt3;
                            if (opt.equals("-a")) {
                                c = 2;
                                break;
                            }
                            c = 65535;
                            break;
                        case 1495:
                            opt = opt3;
                            if (opt.equals("-d")) {
                                c = 0;
                                break;
                            }
                            c = 65535;
                            break;
                        case 1496:
                            opt = opt3;
                            if (opt.equals("-e")) {
                                c = 1;
                                break;
                            }
                            c = 65535;
                            break;
                        case 1497:
                            opt2 = opt3;
                            if (opt2.equals("-f")) {
                                opt = opt2;
                                c = 3;
                                break;
                            }
                            opt = opt2;
                            c = 65535;
                            break;
                        case NetworkConstants.ETHER_MTU /* 1500 */:
                            opt2 = opt3;
                            if (opt2.equals("-i")) {
                                c2 = 4;
                                String str = opt2;
                                c = c2;
                                opt = str;
                                break;
                            }
                            opt = opt2;
                            c = 65535;
                            break;
                        case 1503:
                            opt2 = opt3;
                            if (opt2.equals("-l")) {
                                c2 = 5;
                                String str2 = opt2;
                                c = c2;
                                opt = str2;
                                break;
                            }
                            opt = opt2;
                            c = 65535;
                            break;
                        case 1510:
                            opt2 = opt3;
                            if (opt2.equals("-s")) {
                                c2 = 6;
                                String str22 = opt2;
                                c = c2;
                                opt = str22;
                                break;
                            }
                            opt = opt2;
                            c = 65535;
                            break;
                        case 1512:
                            opt2 = opt3;
                            if (opt2.equals("-u")) {
                                c2 = '\b';
                                String str222 = opt2;
                                c = c2;
                                opt = str222;
                                break;
                            }
                            opt = opt2;
                            c = 65535;
                            break;
                        case 43014832:
                            opt2 = opt3;
                            if (opt2.equals("--uid")) {
                                c2 = '\r';
                                String str2222 = opt2;
                                c = c2;
                                opt = str2222;
                                break;
                            }
                            opt = opt2;
                            c = 65535;
                            break;
                        case 1333469547:
                            opt2 = opt3;
                            if (opt2.equals("--user")) {
                                c2 = '\f';
                                String str22222 = opt2;
                                c = c2;
                                opt = str22222;
                                break;
                            }
                            opt = opt2;
                            c = 65535;
                            break;
                        case 1809263575:
                            opt2 = opt3;
                            if (opt2.equals("--apex-only")) {
                                c2 = 11;
                                String str222222 = opt2;
                                c = c2;
                                opt = str222222;
                                break;
                            }
                            opt = opt2;
                            c = 65535;
                            break;
                        default:
                            opt = opt3;
                            c = 65535;
                            break;
                    }
                    switch (c) {
                        case 0:
                            listSystem2 = true;
                            defaultUserId2 = showSourceDir2;
                            break;
                        case 1:
                            listThirdParty = true;
                            defaultUserId2 = showSourceDir2;
                            break;
                        case 2:
                            getFlags2 = getFlags2 | 4202496 | 536870912;
                            defaultUserId2 = showSourceDir2;
                            break;
                        case 3:
                            listDisabled2 = true;
                            defaultUserId2 = showSourceDir2;
                            break;
                        case 4:
                            showVersionCode2 = true;
                            defaultUserId2 = showSourceDir2;
                            break;
                        case 5:
                            defaultUserId2 = showSourceDir2;
                            break;
                        case 6:
                            listInstaller = true;
                            defaultUserId2 = showSourceDir2;
                            break;
                        case 7:
                            defaultUserId2 = 1;
                            break;
                        case '\b':
                            getFlags2 |= 8192;
                            defaultUserId2 = showSourceDir2;
                            break;
                        case '\t':
                            showUid = true;
                            defaultUserId2 = showSourceDir2;
                            break;
                        case '\n':
                            showVersionCode = true;
                            defaultUserId2 = showSourceDir2;
                            break;
                        case 11:
                            getFlags2 |= 1073741824;
                            listApexOnly = true;
                            defaultUserId2 = showSourceDir2;
                            break;
                        case '\f':
                            try {
                                int defaultUserId4 = UserHandle.parseUserArg(getNextArgRequired());
                                defaultUserId = defaultUserId4;
                                defaultUserId2 = showSourceDir2;
                                break;
                            } catch (RuntimeException e) {
                                ex = e;
                                pw = pw2;
                                pw.println("Error: " + ex.toString());
                                return -1;
                            }
                        case '\r':
                            defaultUserId2 = 1;
                            try {
                                uid = Integer.parseInt(getNextArgRequired());
                                break;
                            } catch (RuntimeException e2) {
                                ex = e2;
                                pw = pw2;
                                pw.println("Error: " + ex.toString());
                                return -1;
                            }
                        default:
                            try {
                                pw2.println("Error: Unknown option: " + opt);
                                return -1;
                            } catch (RuntimeException e3) {
                                ex = e3;
                                pw = pw2;
                                pw.println("Error: " + ex.toString());
                                return -1;
                            }
                    }
                } catch (RuntimeException e4) {
                    ex = e4;
                    pw = pw2;
                }
            } catch (RuntimeException e5) {
                ex = e5;
                pw = pw2;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ List lambda$runListPackages$0(String k) {
        return new ArrayList();
    }

    private int runListPermissionGroups() throws RemoteException {
        PrintWriter pw = getOutPrintWriter();
        List<PermissionGroupInfo> pgs = this.mPermissionManager.getAllPermissionGroups(0);
        int count = pgs.size();
        for (int p = 0; p < count; p++) {
            PermissionGroupInfo pgi = pgs.get(p);
            pw.print("permission group:");
            pw.println(pgi.name);
        }
        return 0;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code restructure failed: missing block: B:21:0x0049, code lost:
        if (r6.equals("-d") != false) goto L9;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private int runListPermissions() throws RemoteException {
        PrintWriter pw = getOutPrintWriter();
        boolean labels = false;
        boolean groups = false;
        boolean userOnly = false;
        boolean summary = false;
        boolean dangerousOnly = false;
        while (true) {
            String opt = getNextOption();
            char c = 0;
            if (opt != null) {
                switch (opt.hashCode()) {
                    case 1495:
                        break;
                    case 1497:
                        if (opt.equals("-f")) {
                            c = 1;
                            break;
                        }
                        c = 65535;
                        break;
                    case 1498:
                        if (opt.equals("-g")) {
                            c = 2;
                            break;
                        }
                        c = 65535;
                        break;
                    case 1510:
                        if (opt.equals("-s")) {
                            c = 3;
                            break;
                        }
                        c = 65535;
                        break;
                    case 1512:
                        if (opt.equals("-u")) {
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
                        dangerousOnly = true;
                        break;
                    case 1:
                        labels = true;
                        break;
                    case 2:
                        groups = true;
                        break;
                    case 3:
                        groups = true;
                        labels = true;
                        summary = true;
                        break;
                    case 4:
                        userOnly = true;
                        break;
                    default:
                        pw.println("Error: Unknown option: " + opt);
                        return 1;
                }
            } else {
                ArrayList<String> groupList = new ArrayList<>();
                if (groups) {
                    List<PermissionGroupInfo> infos = this.mPermissionManager.getAllPermissionGroups(0);
                    int count = infos.size();
                    for (int i = 0; i < count; i++) {
                        groupList.add(infos.get(i).name);
                    }
                    groupList.add(null);
                } else {
                    String grp = getNextArg();
                    groupList.add(grp);
                }
                if (dangerousOnly) {
                    pw.println("Dangerous Permissions:");
                    pw.println("");
                    doListPermissions(groupList, groups, labels, summary, 1, 1);
                    if (userOnly) {
                        pw.println("Normal Permissions:");
                        pw.println("");
                        doListPermissions(groupList, groups, labels, summary, 0, 0);
                        return 0;
                    }
                    return 0;
                } else if (userOnly) {
                    pw.println("Dangerous and Normal Permissions:");
                    pw.println("");
                    doListPermissions(groupList, groups, labels, summary, 0, 1);
                    return 0;
                } else {
                    pw.println("All Permissions:");
                    pw.println("");
                    doListPermissions(groupList, groups, labels, summary, -10000, 10000);
                    return 0;
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class SessionDump {
        boolean onlyParent;
        boolean onlyReady;
        boolean onlySessionId;

        private SessionDump() {
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    private boolean setSessionFlag(String flag, SessionDump sessionDump) {
        char c;
        switch (flag.hashCode()) {
            case -2056597429:
                if (flag.equals("--only-parent")) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case -1847964944:
                if (flag.equals("--only-sessionid")) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case 1321081314:
                if (flag.equals("--only-ready")) {
                    c = 1;
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
                sessionDump.onlyParent = true;
                break;
            case 1:
                sessionDump.onlyReady = true;
                break;
            case 2:
                sessionDump.onlySessionId = true;
                break;
            default:
                return false;
        }
        return true;
    }

    private int runListStagedSessions() {
        String opt;
        IndentingPrintWriter pw = new IndentingPrintWriter(getOutPrintWriter(), "  ", 120);
        try {
            SessionDump sessionDump = new SessionDump();
            do {
                opt = getNextOption();
                if (opt == null) {
                    try {
                        List<PackageInstaller.SessionInfo> stagedSessions = this.mInterface.getPackageInstaller().getStagedSessions().getList();
                        printSessionList(pw, stagedSessions, sessionDump);
                        pw.close();
                        return 1;
                    } catch (RemoteException e) {
                        pw.println("Failure [" + e.getClass().getName() + " - " + e.getMessage() + "]");
                        pw.close();
                        return -1;
                    }
                }
            } while (setSessionFlag(opt, sessionDump));
            pw.println("Error: Unknown option: " + opt);
            pw.close();
            return -1;
        } catch (Throwable th) {
            try {
                pw.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
    }

    private void printSessionList(IndentingPrintWriter pw, List<PackageInstaller.SessionInfo> stagedSessions, SessionDump sessionDump) {
        SparseArray<PackageInstaller.SessionInfo> sessionById = new SparseArray<>(stagedSessions.size());
        for (PackageInstaller.SessionInfo session : stagedSessions) {
            sessionById.put(session.getSessionId(), session);
        }
        for (PackageInstaller.SessionInfo session2 : stagedSessions) {
            if (!sessionDump.onlyReady || session2.isStagedSessionReady()) {
                if (session2.getParentSessionId() == -1) {
                    printSession(pw, session2, sessionDump);
                    if (session2.isMultiPackage() && !sessionDump.onlyParent) {
                        pw.increaseIndent();
                        int[] childIds = session2.getChildSessionIds();
                        for (int i = 0; i < childIds.length; i++) {
                            PackageInstaller.SessionInfo childSession = sessionById.get(childIds[i]);
                            if (childSession == null) {
                                if (sessionDump.onlySessionId) {
                                    pw.println(childIds[i]);
                                } else {
                                    pw.println("sessionId = " + childIds[i] + "; not found");
                                }
                            } else {
                                printSession(pw, childSession, sessionDump);
                            }
                        }
                        pw.decreaseIndent();
                    }
                }
            }
        }
    }

    private static void printSession(PrintWriter pw, PackageInstaller.SessionInfo session, SessionDump sessionDump) {
        if (sessionDump.onlySessionId) {
            pw.println(session.getSessionId());
        } else {
            pw.println("sessionId = " + session.getSessionId() + "; appPackageName = " + session.getAppPackageName() + "; isStaged = " + session.isStaged() + "; isReady = " + session.isStagedSessionReady() + "; isApplied = " + session.isStagedSessionApplied() + "; isFailed = " + session.isStagedSessionFailed() + "; errorMsg = " + session.getStagedSessionErrorMessage() + ";");
        }
    }

    private Intent parseIntentAndUser() throws URISyntaxException {
        this.mTargetUser = -2;
        this.mBrief = false;
        this.mComponents = false;
        Intent intent = Intent.parseCommandArgs(this, new Intent.CommandOptionHandler() { // from class: com.android.server.pm.PackageManagerShellCommand.4
            public boolean handleOption(String opt, ShellCommand cmd) {
                if ("--user".equals(opt)) {
                    PackageManagerShellCommand.this.mTargetUser = UserHandle.parseUserArg(cmd.getNextArgRequired());
                    return true;
                } else if ("--brief".equals(opt)) {
                    PackageManagerShellCommand.this.mBrief = true;
                    return true;
                } else if ("--components".equals(opt)) {
                    PackageManagerShellCommand.this.mComponents = true;
                    return true;
                } else if ("--query-flags".equals(opt)) {
                    PackageManagerShellCommand.this.mQueryFlags = Integer.decode(cmd.getNextArgRequired()).intValue();
                    return true;
                } else {
                    return false;
                }
            }
        });
        this.mTargetUser = ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), this.mTargetUser, false, false, null, null);
        return intent;
    }

    private void printResolveInfo(PrintWriterPrinter pr, String prefix, ResolveInfo ri, boolean brief, boolean components) {
        ComponentName comp;
        if (brief || components) {
            if (ri.activityInfo != null) {
                comp = new ComponentName(ri.activityInfo.packageName, ri.activityInfo.name);
            } else if (ri.serviceInfo != null) {
                comp = new ComponentName(ri.serviceInfo.packageName, ri.serviceInfo.name);
            } else if (ri.providerInfo != null) {
                comp = new ComponentName(ri.providerInfo.packageName, ri.providerInfo.name);
            } else {
                comp = null;
            }
            if (comp != null) {
                if (!components) {
                    pr.println(prefix + "priority=" + ri.priority + " preferredOrder=" + ri.preferredOrder + " match=0x" + Integer.toHexString(ri.match) + " specificIndex=" + ri.specificIndex + " isDefault=" + ri.isDefault);
                }
                pr.println(prefix + comp.flattenToShortString());
                return;
            }
        }
        ri.dump(pr, prefix);
    }

    private int runResolveActivity() {
        try {
            Intent intent = parseIntentAndUser();
            try {
                ResolveInfo ri = this.mInterface.resolveIntent(intent, intent.getType(), this.mQueryFlags, this.mTargetUser);
                PrintWriter pw = getOutPrintWriter();
                if (ri == null) {
                    pw.println("No activity found");
                    return 0;
                }
                PrintWriterPrinter pr = new PrintWriterPrinter(pw);
                printResolveInfo(pr, "", ri, this.mBrief, this.mComponents);
                return 0;
            } catch (RemoteException e) {
                throw new RuntimeException("Failed calling service", e);
            }
        } catch (URISyntaxException e2) {
            throw new RuntimeException(e2.getMessage(), e2);
        }
    }

    private int runQueryIntentActivities() {
        try {
            Intent intent = parseIntentAndUser();
            try {
                List<ResolveInfo> result = this.mInterface.queryIntentActivities(intent, intent.getType(), this.mQueryFlags, this.mTargetUser).getList();
                PrintWriter pw = getOutPrintWriter();
                if (result != null && result.size() > 0) {
                    if (!this.mComponents) {
                        pw.print(result.size());
                        pw.println(" activities found:");
                        PrintWriterPrinter pr = new PrintWriterPrinter(pw);
                        for (int i = 0; i < result.size(); i++) {
                            pw.print("  Activity #");
                            pw.print(i);
                            pw.println(":");
                            printResolveInfo(pr, "    ", result.get(i), this.mBrief, this.mComponents);
                        }
                        return 0;
                    }
                    PrintWriterPrinter pr2 = new PrintWriterPrinter(pw);
                    for (int i2 = 0; i2 < result.size(); i2++) {
                        printResolveInfo(pr2, "", result.get(i2), this.mBrief, this.mComponents);
                    }
                    return 0;
                }
                pw.println("No activities found");
                return 0;
            } catch (RemoteException e) {
                throw new RuntimeException("Failed calling service", e);
            }
        } catch (URISyntaxException e2) {
            throw new RuntimeException(e2.getMessage(), e2);
        }
    }

    private int runQueryIntentServices() {
        try {
            Intent intent = parseIntentAndUser();
            try {
                List<ResolveInfo> result = this.mInterface.queryIntentServices(intent, intent.getType(), this.mQueryFlags, this.mTargetUser).getList();
                PrintWriter pw = getOutPrintWriter();
                if (result != null && result.size() > 0) {
                    if (!this.mComponents) {
                        pw.print(result.size());
                        pw.println(" services found:");
                        PrintWriterPrinter pr = new PrintWriterPrinter(pw);
                        for (int i = 0; i < result.size(); i++) {
                            pw.print("  Service #");
                            pw.print(i);
                            pw.println(":");
                            printResolveInfo(pr, "    ", result.get(i), this.mBrief, this.mComponents);
                        }
                        return 0;
                    }
                    PrintWriterPrinter pr2 = new PrintWriterPrinter(pw);
                    for (int i2 = 0; i2 < result.size(); i2++) {
                        printResolveInfo(pr2, "", result.get(i2), this.mBrief, this.mComponents);
                    }
                    return 0;
                }
                pw.println("No services found");
                return 0;
            } catch (RemoteException e) {
                throw new RuntimeException("Failed calling service", e);
            }
        } catch (URISyntaxException e2) {
            throw new RuntimeException(e2.getMessage(), e2);
        }
    }

    private int runQueryIntentReceivers() {
        try {
            Intent intent = parseIntentAndUser();
            try {
                List<ResolveInfo> result = this.mInterface.queryIntentReceivers(intent, intent.getType(), this.mQueryFlags, this.mTargetUser).getList();
                PrintWriter pw = getOutPrintWriter();
                if (result != null && result.size() > 0) {
                    if (!this.mComponents) {
                        pw.print(result.size());
                        pw.println(" receivers found:");
                        PrintWriterPrinter pr = new PrintWriterPrinter(pw);
                        for (int i = 0; i < result.size(); i++) {
                            pw.print("  Receiver #");
                            pw.print(i);
                            pw.println(":");
                            printResolveInfo(pr, "    ", result.get(i), this.mBrief, this.mComponents);
                        }
                        return 0;
                    }
                    PrintWriterPrinter pr2 = new PrintWriterPrinter(pw);
                    for (int i2 = 0; i2 < result.size(); i2++) {
                        printResolveInfo(pr2, "", result.get(i2), this.mBrief, this.mComponents);
                    }
                    return 0;
                }
                pw.println("No receivers found");
                return 0;
            } catch (RemoteException e) {
                throw new RuntimeException("Failed calling service", e);
            }
        } catch (URISyntaxException e2) {
            throw new RuntimeException(e2.getMessage(), e2);
        }
    }

    private int runStreamingInstall() throws RemoteException {
        InstallParams params = makeInstallParams(UNSUPPORTED_INSTALL_CMD_OPTS);
        if (params.sessionParams.dataLoaderParams == null) {
            params.sessionParams.setDataLoaderParams(PackageManagerShellCommandDataLoader.getStreamingDataLoaderParams(this));
        }
        return doRunInstall(params);
    }

    private int runIncrementalInstall() throws RemoteException {
        InstallParams params = makeInstallParams(UNSUPPORTED_INSTALL_CMD_OPTS);
        if (params.sessionParams.dataLoaderParams == null) {
            params.sessionParams.setDataLoaderParams(PackageManagerShellCommandDataLoader.getIncrementalDataLoaderParams(this));
        }
        return doRunInstall(params);
    }

    private int runInstall() throws RemoteException {
        return doRunInstall(makeInstallParams(UNSUPPORTED_INSTALL_CMD_OPTS));
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1496=7, 1498=6, 1499=6, 1500=6] */
    private int doRunInstall(InstallParams params) throws RemoteException {
        int sessionId;
        Throwable th;
        int sessionId2;
        PrintWriter pw = getOutPrintWriter();
        int i = 0;
        while (true) {
            int count = i;
            sessionId = 1;
            if (!Build.IS_DEBUG_ENABLE || SystemProperties.getInt("sys.boot_completed", 0) == 1) {
                break;
            }
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
            }
            if (count % 10 == 1) {
                Slog.d(TAG, "doRunInstall enter loop count:" + count);
            }
            i = count + 1;
            if (count > 300) {
                pw.println("Error: can't install before boot completed");
                return 1;
            }
        }
        boolean isStreaming = params.sessionParams.dataLoaderParams != null;
        boolean isApex = (params.sessionParams.installFlags & 131072) != 0;
        ArrayList<String> args = getRemainingArgs();
        boolean fromStdIn = args.isEmpty() || STDIN_PATH.equals(args.get(0));
        boolean hasSplits = args.size() > 1;
        if (fromStdIn && params.sessionParams.sizeBytes == -1) {
            pw.println("Error: must either specify a package size or an APK file");
            return 1;
        } else if (isApex && hasSplits) {
            pw.println("Error: can't specify SPLIT(s) for APEX");
            return 1;
        } else {
            if (!isStreaming) {
                if (fromStdIn && hasSplits) {
                    pw.println("Error: can't specify SPLIT(s) along with STDIN");
                    return 1;
                } else if (args.isEmpty()) {
                    args.add(STDIN_PATH);
                } else {
                    setParamsSize(params, args);
                }
            }
            int sessionId3 = doCreateSession(params.sessionParams, params.installerPackageName, params.userId);
            try {
                if (isStreaming) {
                    try {
                        sessionId2 = sessionId3;
                        if (doAddFiles(sessionId3, args, params.sessionParams.sizeBytes, isApex) != 0) {
                            if (1 != 0) {
                                try {
                                    doAbandonSession(sessionId2, false);
                                    return 1;
                                } catch (Exception e2) {
                                    return 1;
                                }
                            }
                            return 1;
                        }
                    } catch (Throwable th2) {
                        sessionId = sessionId3;
                        th = th2;
                        if (1 != 0) {
                            try {
                                doAbandonSession(sessionId, false);
                            } catch (Exception e3) {
                            }
                        }
                        throw th;
                    }
                } else {
                    sessionId2 = sessionId3;
                    if (doWriteSplits(sessionId2, args, params.sessionParams.sizeBytes, isApex) != 0) {
                        if (1 != 0) {
                            try {
                                doAbandonSession(sessionId2, false);
                                return 1;
                            } catch (Exception e4) {
                                return 1;
                            }
                        }
                        return 1;
                    }
                }
                if (doCommitSession(sessionId2, false) != 0) {
                    if (1 != 0) {
                        try {
                            doAbandonSession(sessionId2, false);
                            return 1;
                        } catch (Exception e5) {
                            return 1;
                        }
                    }
                    return 1;
                } else if (!params.sessionParams.isStaged || params.stagedReadyTimeoutMs <= 0) {
                    pw.println("Success");
                    if (0 != 0) {
                        try {
                            doAbandonSession(sessionId2, false);
                        } catch (Exception e6) {
                        }
                    }
                    return 0;
                } else {
                    int doWaitForStagedSessionReady = doWaitForStagedSessionReady(sessionId2, params.stagedReadyTimeoutMs, pw);
                    if (0 != 0) {
                        try {
                            doAbandonSession(sessionId2, false);
                        } catch (Exception e7) {
                        }
                    }
                    return doWaitForStagedSessionReady;
                }
            } catch (Throwable th3) {
                th = th3;
            }
        }
    }

    /* JADX WARN: Code restructure failed: missing block: B:25:0x0096, code lost:
        r19.println("Failure [failed to retrieve SessionInfo]");
     */
    /* JADX WARN: Code restructure failed: missing block: B:26:0x009c, code lost:
        return 1;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private int doWaitForStagedSessionReady(int sessionId, long timeoutMs, PrintWriter pw) throws RemoteException {
        Preconditions.checkArgument(timeoutMs > 0);
        PackageInstaller.SessionInfo si = this.mInterface.getPackageInstaller().getSessionInfo(sessionId);
        if (si == null) {
            pw.println("Failure [Unknown session " + sessionId + "]");
            return 1;
        } else if (!si.isStaged()) {
            pw.println("Failure [Session " + sessionId + " is not a staged session]");
            return 1;
        } else {
            long currentTime = System.currentTimeMillis();
            long endTime = currentTime + timeoutMs;
            while (currentTime < endTime && (si == null || (!si.isStagedSessionReady() && !si.isStagedSessionFailed()))) {
                SystemClock.sleep(Math.min(endTime - currentTime, 100L));
                currentTime = System.currentTimeMillis();
                si = this.mInterface.getPackageInstaller().getSessionInfo(sessionId);
            }
            if (!si.isStagedSessionReady() && !si.isStagedSessionFailed()) {
                pw.println("Failure [timed out after " + timeoutMs + " ms]");
                return 1;
            } else if (!si.isStagedSessionReady()) {
                pw.println("Error [" + si.getStagedSessionErrorCode() + "] [" + si.getStagedSessionErrorMessage() + "]");
                return 1;
            } else {
                pw.println("Success. Reboot device to apply staged session");
                return 0;
            }
        }
    }

    private int runInstallAbandon() throws RemoteException {
        int sessionId = Integer.parseInt(getNextArg());
        return doAbandonSession(sessionId, true);
    }

    /* JADX WARN: Code restructure failed: missing block: B:9:0x001e, code lost:
        if (r3.equals("--staged-ready-timeout") != false) goto L8;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private int runInstallCommit() throws RemoteException {
        PrintWriter pw = getOutPrintWriter();
        long stagedReadyTimeoutMs = 60000;
        while (true) {
            String opt = getNextOption();
            boolean z = false;
            if (opt != null) {
                switch (opt.hashCode()) {
                    case -158482320:
                        break;
                    default:
                        z = true;
                        break;
                }
                switch (z) {
                    case false:
                        stagedReadyTimeoutMs = Long.parseLong(getNextArgRequired());
                    default:
                        throw new IllegalArgumentException("Unknown option: " + opt);
                }
            } else {
                int sessionId = Integer.parseInt(getNextArg());
                if (doCommitSession(sessionId, false) != 0) {
                    return 1;
                }
                PackageInstaller.SessionInfo si = this.mInterface.getPackageInstaller().getSessionInfo(sessionId);
                if (si != null && si.isStaged() && stagedReadyTimeoutMs > 0) {
                    return doWaitForStagedSessionReady(sessionId, stagedReadyTimeoutMs, pw);
                }
                pw.println("Success");
                return 0;
            }
        }
    }

    private int runInstallCreate() throws RemoteException {
        PrintWriter pw = getOutPrintWriter();
        InstallParams installParams = makeInstallParams(UNSUPPORTED_SESSION_CREATE_OPTS);
        int sessionId = doCreateSession(installParams.sessionParams, installParams.installerPackageName, installParams.userId);
        pw.println("Success: created install session [" + sessionId + "]");
        return 0;
    }

    private int runInstallWrite() throws RemoteException {
        long sizeBytes = -1;
        while (true) {
            String opt = getNextOption();
            if (opt != null) {
                if (opt.equals("-S")) {
                    sizeBytes = Long.parseLong(getNextArg());
                } else {
                    throw new IllegalArgumentException("Unknown option: " + opt);
                }
            } else {
                int sessionId = Integer.parseInt(getNextArg());
                String splitName = getNextArg();
                String path = getNextArg();
                return doWriteSplit(sessionId, path, sizeBytes, splitName, true);
            }
        }
    }

    private int runInstallAddSession() throws RemoteException {
        PrintWriter pw = getOutPrintWriter();
        int parentSessionId = Integer.parseInt(getNextArg());
        IntArray otherSessionIds = new IntArray();
        while (true) {
            String opt = getNextArg();
            if (opt == null) {
                break;
            }
            otherSessionIds.add(Integer.parseInt(opt));
        }
        if (otherSessionIds.size() == 0) {
            pw.println("Error: At least two sessions are required.");
            return 1;
        }
        return doInstallAddSession(parentSessionId, otherSessionIds.toArray(), true);
    }

    private int runInstallRemove() throws RemoteException {
        PrintWriter pw = getOutPrintWriter();
        int sessionId = Integer.parseInt(getNextArg());
        ArrayList<String> splitNames = getRemainingArgs();
        if (splitNames.isEmpty()) {
            pw.println("Error: split name not specified");
            return 1;
        }
        return doRemoveSplits(sessionId, splitNames, true);
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code restructure failed: missing block: B:15:0x0039, code lost:
        if (r0.equals("--user") != false) goto L9;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private int runInstallExisting() throws RemoteException {
        PrintWriter pw = getOutPrintWriter();
        int installFlags = 4194304;
        boolean waitTillComplete = false;
        int installFlags2 = -2;
        while (true) {
            String opt = getNextOption();
            char c = 0;
            if (opt != null) {
                switch (opt.hashCode()) {
                    case -951415743:
                        if (opt.equals("--instant")) {
                            c = 2;
                            break;
                        }
                        c = 65535;
                        break;
                    case 1051781117:
                        if (opt.equals("--ephemeral")) {
                            c = 1;
                            break;
                        }
                        c = 65535;
                        break;
                    case 1333024815:
                        if (opt.equals("--full")) {
                            c = 3;
                            break;
                        }
                        c = 65535;
                        break;
                    case 1333469547:
                        break;
                    case 1333511957:
                        if (opt.equals("--wait")) {
                            c = 4;
                            break;
                        }
                        c = 65535;
                        break;
                    case 1494514835:
                        if (opt.equals("--restrict-permissions")) {
                            c = 5;
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
                        int userId = UserHandle.parseUserArg(getNextArgRequired());
                        installFlags2 = userId;
                        break;
                    case 1:
                    case 2:
                        installFlags = (installFlags | 2048) & (-16385);
                        break;
                    case 3:
                        installFlags = (installFlags & (-2049)) | 16384;
                        break;
                    case 4:
                        waitTillComplete = true;
                        break;
                    case 5:
                        installFlags = (-4194305) & installFlags;
                        break;
                    default:
                        pw.println("Error: Unknown option: " + opt);
                        return 1;
                }
            } else {
                String packageName = getNextArg();
                if (packageName != null) {
                    int translatedUserId = translateUserId(installFlags2, -10000, "runInstallExisting");
                    if (waitTillComplete) {
                        try {
                            LocalIntentReceiver receiver = new LocalIntentReceiver();
                            IPackageInstaller installer = this.mInterface.getPackageInstaller();
                            pw.println("Installing package " + packageName + " for user: " + translatedUserId);
                            try {
                                installer.installExistingPackage(packageName, installFlags, 0, receiver.getIntentSender(), translatedUserId, (List) null);
                                Intent result = receiver.getResult();
                                int status = result.getIntExtra("android.content.pm.extra.STATUS", 1);
                                pw.println("Received intent for package install");
                                return status == 0 ? 0 : 1;
                            } catch (PackageManager.NameNotFoundException | RemoteException e) {
                                e = e;
                            }
                        } catch (PackageManager.NameNotFoundException | RemoteException e2) {
                            e = e2;
                        }
                    } else {
                        try {
                            int res = this.mInterface.installExistingPackageAsUser(packageName, translatedUserId, installFlags, 0, (List) null);
                            try {
                                if (res == -3) {
                                    throw new PackageManager.NameNotFoundException("Package " + packageName + " doesn't exist");
                                }
                                try {
                                    pw.println("Package " + packageName + " installed for user: " + translatedUserId);
                                    return 0;
                                } catch (PackageManager.NameNotFoundException | RemoteException e3) {
                                    e = e3;
                                }
                            } catch (PackageManager.NameNotFoundException | RemoteException e4) {
                                e = e4;
                            }
                        } catch (PackageManager.NameNotFoundException | RemoteException e5) {
                            e = e5;
                        }
                    }
                    pw.println(e.toString());
                    return 1;
                }
                pw.println("Error: package name not specified");
                return 1;
            }
        }
    }

    private int runSetInstallLocation() throws RemoteException {
        String arg = getNextArg();
        if (arg == null) {
            getErrPrintWriter().println("Error: no install location specified.");
            return 1;
        }
        try {
            int loc = Integer.parseInt(arg);
            if (!this.mInterface.setInstallLocation(loc)) {
                getErrPrintWriter().println("Error: install location has to be a number.");
                return 1;
            }
            return 0;
        } catch (NumberFormatException e) {
            getErrPrintWriter().println("Error: install location has to be a number.");
            return 1;
        }
    }

    private int runGetInstallLocation() throws RemoteException {
        int loc = this.mInterface.getInstallLocation();
        String locStr = "invalid";
        if (loc == 0) {
            locStr = UiModeManagerService.Shell.NIGHT_MODE_STR_AUTO;
        } else if (loc == 1) {
            locStr = "internal";
        } else if (loc == 2) {
            locStr = "external";
        }
        getOutPrintWriter().println(loc + "[" + locStr + "]");
        return 0;
    }

    public int runMovePackage() throws RemoteException {
        String packageName = getNextArg();
        if (packageName == null) {
            getErrPrintWriter().println("Error: package name not specified");
            return 1;
        }
        String volumeUuid = getNextArg();
        if ("internal".equals(volumeUuid)) {
            volumeUuid = null;
        }
        int moveId = this.mInterface.movePackage(packageName, volumeUuid);
        int status = this.mInterface.getMoveStatus(moveId);
        while (!PackageManager.isMoveStatusFinished(status)) {
            SystemClock.sleep(1000L);
            status = this.mInterface.getMoveStatus(moveId);
        }
        if (status == -100) {
            getOutPrintWriter().println("Success");
            return 0;
        }
        getErrPrintWriter().println("Failure [" + status + "]");
        return 1;
    }

    public int runMovePrimaryStorage() throws RemoteException {
        String volumeUuid = getNextArg();
        if ("internal".equals(volumeUuid)) {
            volumeUuid = null;
        }
        int moveId = this.mInterface.movePrimaryStorage(volumeUuid);
        int status = this.mInterface.getMoveStatus(moveId);
        while (!PackageManager.isMoveStatusFinished(status)) {
            SystemClock.sleep(1000L);
            status = this.mInterface.getMoveStatus(moveId);
        }
        if (status == -100) {
            getOutPrintWriter().println("Success");
            return 0;
        }
        getErrPrintWriter().println("Failure [" + status + "]");
        return 1;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    private int runCompile() throws RemoteException {
        String targetCompilerFilter;
        List<String> packageNames;
        boolean clearProfileData;
        boolean allPackages;
        int index;
        List<String> failedPackages;
        String opt;
        boolean performDexOptMode;
        boolean result;
        int reason;
        char c;
        PackageManagerShellCommand packageManagerShellCommand = this;
        PrintWriter pw = getOutPrintWriter();
        boolean checkProfiles = SystemProperties.getBoolean("dalvik.vm.usejitprofiles", false);
        boolean forceCompilation = false;
        boolean allPackages2 = false;
        boolean clearProfileData2 = false;
        String compilerFilter = null;
        String compilationReason = null;
        String checkProfilesRaw = null;
        boolean secondaryDex = false;
        String split = null;
        boolean compileLayouts = false;
        while (true) {
            String nextOption = getNextOption();
            String opt2 = nextOption;
            if (nextOption != null) {
                switch (opt2.hashCode()) {
                    case -1615291473:
                        if (opt2.equals("--reset")) {
                            c = 7;
                            break;
                        }
                        c = 65535;
                        break;
                    case -1614046854:
                        if (opt2.equals("--split")) {
                            c = '\t';
                            break;
                        }
                        c = 65535;
                        break;
                    case 1492:
                        if (opt2.equals("-a")) {
                            c = 0;
                            break;
                        }
                        c = 65535;
                        break;
                    case 1494:
                        if (opt2.equals("-c")) {
                            c = 1;
                            break;
                        }
                        c = 65535;
                        break;
                    case 1497:
                        if (opt2.equals("-f")) {
                            c = 2;
                            break;
                        }
                        c = 65535;
                        break;
                    case 1504:
                        if (opt2.equals("-m")) {
                            c = 3;
                            break;
                        }
                        c = 65535;
                        break;
                    case 1509:
                        if (opt2.equals("-r")) {
                            c = 4;
                            break;
                        }
                        c = 65535;
                        break;
                    case 1269477022:
                        if (opt2.equals("--secondary-dex")) {
                            c = '\b';
                            break;
                        }
                        c = 65535;
                        break;
                    case 1323879247:
                        if (opt2.equals("--compile-layouts")) {
                            c = 5;
                            break;
                        }
                        c = 65535;
                        break;
                    case 1690714782:
                        if (opt2.equals("--check-prof")) {
                            c = 6;
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
                        allPackages2 = true;
                        break;
                    case 1:
                        clearProfileData2 = true;
                        break;
                    case 2:
                        forceCompilation = true;
                        break;
                    case 3:
                        String compilerFilter2 = getNextArgRequired();
                        compilerFilter = compilerFilter2;
                        break;
                    case 4:
                        String compilationReason2 = getNextArgRequired();
                        compilationReason = compilationReason2;
                        break;
                    case 5:
                        compileLayouts = true;
                        break;
                    case 6:
                        String checkProfilesRaw2 = getNextArgRequired();
                        checkProfilesRaw = checkProfilesRaw2;
                        break;
                    case 7:
                        compilationReason = "install";
                        clearProfileData2 = true;
                        forceCompilation = true;
                        break;
                    case '\b':
                        secondaryDex = true;
                        break;
                    case '\t':
                        String split2 = getNextArgRequired();
                        split = split2;
                        break;
                    default:
                        pw.println("Error: Unknown option: " + opt2);
                        return 1;
                }
            } else {
                if (checkProfilesRaw != null) {
                    if ("true".equals(checkProfilesRaw)) {
                        checkProfiles = true;
                    } else if ("false".equals(checkProfilesRaw)) {
                        checkProfiles = false;
                    } else {
                        pw.println("Invalid value for \"--check-prof\". Expected \"true\" or \"false\".");
                        return 1;
                    }
                }
                boolean compilerFilterGiven = compilerFilter != null;
                boolean compilationReasonGiven = compilationReason != null;
                if ((!compilerFilterGiven && !compilationReasonGiven && !compileLayouts) || ((!compilerFilterGiven && compilationReasonGiven && compileLayouts) || ((compilerFilterGiven && !compilationReasonGiven && compileLayouts) || ((compilerFilterGiven && compilationReasonGiven && !compileLayouts) || (compilerFilterGiven && compilationReasonGiven && compileLayouts))))) {
                    pw.println("Must specify exactly one of compilation filter (\"-m\"), compilation reason (\"-r\"), or compile layouts (\"--compile-layouts\")");
                    return 1;
                } else if (allPackages2 && split != null) {
                    pw.println("-a cannot be specified together with --split");
                    return 1;
                } else if (secondaryDex && split != null) {
                    pw.println("--secondary-dex cannot be specified together with --split");
                    return 1;
                } else {
                    String targetCompilerFilter2 = null;
                    if (compilerFilterGiven) {
                        if (!DexFile.isValidCompilerFilter(compilerFilter)) {
                            pw.println("Error: \"" + compilerFilter + "\" is not a valid compilation filter.");
                            return 1;
                        }
                        targetCompilerFilter2 = compilerFilter;
                    }
                    if (compilationReasonGiven) {
                        int reason2 = -1;
                        int i = 0;
                        while (true) {
                            int reason3 = reason2;
                            if (i >= PackageManagerServiceCompilerMapping.REASON_STRINGS.length) {
                                reason = reason3;
                            } else if (!PackageManagerServiceCompilerMapping.REASON_STRINGS[i].equals(compilationReason)) {
                                i++;
                                reason2 = reason3;
                            } else {
                                reason = i;
                            }
                        }
                        if (reason == -1) {
                            pw.println("Error: Unknown compilation reason: " + compilationReason);
                            return 1;
                        }
                        String targetCompilerFilter3 = PackageManagerServiceCompilerMapping.getCompilerFilterForReason(reason);
                        targetCompilerFilter = targetCompilerFilter3;
                    } else {
                        targetCompilerFilter = targetCompilerFilter2;
                    }
                    if (allPackages2) {
                        packageNames = packageManagerShellCommand.mInterface.getAllPackages();
                    } else {
                        String packageName = getNextArg();
                        if (packageName == null) {
                            pw.println("Error: package name not specified");
                            return 1;
                        }
                        packageNames = Collections.singletonList(packageName);
                    }
                    List<String> failedPackages2 = new ArrayList<>();
                    int index2 = 0;
                    for (String packageName2 : packageNames) {
                        String compilationReason3 = compilationReason;
                        if (!clearProfileData2) {
                            clearProfileData = clearProfileData2;
                        } else {
                            clearProfileData = clearProfileData2;
                            packageManagerShellCommand.mInterface.clearApplicationProfileData(packageName2);
                        }
                        if (!allPackages2) {
                            allPackages = allPackages2;
                            index = index2;
                        } else {
                            int index3 = index2 + 1;
                            allPackages = allPackages2;
                            pw.println(index3 + SliceClientPermissions.SliceAuthority.DELIMITER + packageNames.size() + ": " + packageName2);
                            pw.flush();
                            index = index3;
                        }
                        if (compileLayouts) {
                            PackageManagerInternal internal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
                            result = internal.compileLayouts(packageName2);
                            failedPackages = failedPackages2;
                            opt = opt2;
                        } else {
                            if (secondaryDex) {
                                performDexOptMode = packageManagerShellCommand.mInterface.performDexOptSecondary(packageName2, targetCompilerFilter, forceCompilation);
                                failedPackages = failedPackages2;
                                opt = opt2;
                            } else {
                                IPackageManager iPackageManager = packageManagerShellCommand.mInterface;
                                failedPackages = failedPackages2;
                                opt = opt2;
                                performDexOptMode = iPackageManager.performDexOptMode(packageName2, checkProfiles, targetCompilerFilter, forceCompilation, true, split);
                            }
                            result = performDexOptMode;
                        }
                        if (!result) {
                            failedPackages.add(packageName2);
                        }
                        failedPackages2 = failedPackages;
                        index2 = index;
                        compilationReason = compilationReason3;
                        clearProfileData2 = clearProfileData;
                        allPackages2 = allPackages;
                        opt2 = opt;
                        packageManagerShellCommand = this;
                    }
                    List<String> failedPackages3 = failedPackages2;
                    boolean allPackages3 = failedPackages3.isEmpty();
                    if (allPackages3) {
                        pw.println("Success");
                        return 0;
                    } else if (failedPackages3.size() == 1) {
                        pw.println("Failure: package " + failedPackages3.get(0) + " could not be compiled");
                        return 1;
                    } else {
                        pw.print("Failure: the following packages could not be compiled: ");
                        boolean is_first = true;
                        for (String packageName3 : failedPackages3) {
                            if (is_first) {
                                is_first = false;
                            } else {
                                pw.print(", ");
                            }
                            pw.print(packageName3);
                        }
                        pw.println();
                        return 1;
                    }
                }
            }
        }
    }

    private int runreconcileSecondaryDexFiles() throws RemoteException {
        String packageName = getNextArg();
        this.mInterface.reconcileSecondaryDexFiles(packageName);
        return 0;
    }

    public int runForceDexOpt() throws RemoteException {
        this.mInterface.forceDexOpt(getNextArgRequired());
        return 0;
    }

    private int runDexoptJob() throws RemoteException {
        List<String> packageNames = new ArrayList<>();
        while (true) {
            String arg = getNextArg();
            if (arg == null) {
                break;
            }
            packageNames.add(arg);
        }
        boolean result = BackgroundDexOptService.getService().runBackgroundDexoptJob(packageNames.isEmpty() ? null : packageNames);
        getOutPrintWriter().println(result ? "Success" : "Failure");
        return result ? 0 : -1;
    }

    private int cancelBgDexOptJob() throws RemoteException {
        BackgroundDexOptService.getService().cancelBackgroundDexoptJob();
        getOutPrintWriter().println("Success");
        return 0;
    }

    private int runDeleteDexOpt() throws RemoteException {
        PrintWriter pw = getOutPrintWriter();
        String packageName = getNextArg();
        if (TextUtils.isEmpty(packageName)) {
            pw.println("Error: no package name");
            return 1;
        }
        long freedBytes = ((PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class)).deleteOatArtifactsOfPackage(packageName);
        if (freedBytes < 0) {
            pw.println("Error: delete failed");
            return 1;
        }
        pw.println("Success: freed " + freedBytes + " bytes");
        Slog.i(TAG, "delete-dexopt " + packageName + " ,freed " + freedBytes + " bytes");
        return 0;
    }

    /* JADX WARN: Code restructure failed: missing block: B:9:0x001c, code lost:
        if (r2.equals("--dump-classes-and-methods") != false) goto L8;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private int runDumpProfiles() throws RemoteException {
        PrintWriter pw = getOutPrintWriter();
        boolean dumpClassesAndMethods = false;
        while (true) {
            String opt = getNextOption();
            boolean z = false;
            if (opt != null) {
                switch (opt.hashCode()) {
                    case -2026131748:
                        break;
                    default:
                        z = true;
                        break;
                }
                switch (z) {
                    case false:
                        dumpClassesAndMethods = true;
                    default:
                        pw.println("Error: Unknown option: " + opt);
                        return 1;
                }
            } else {
                String packageName = getNextArg();
                this.mInterface.dumpProfiles(packageName, dumpClassesAndMethods);
                return 0;
            }
        }
    }

    /* JADX WARN: Code restructure failed: missing block: B:9:0x0028, code lost:
        if (r3.equals("--code-path") != false) goto L8;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private int runSnapshotProfile() throws RemoteException {
        String codePath;
        String baseCodePath;
        String str;
        PrintWriter pw = getOutPrintWriter();
        String packageName = getNextArg();
        boolean isBootImage = PackageManagerService.PLATFORM_PACKAGE_NAME.equals(packageName);
        String codePath2 = null;
        while (true) {
            String opt = getNextArg();
            boolean z = false;
            if (opt != null) {
                switch (opt.hashCode()) {
                    case -684928411:
                        break;
                    default:
                        z = true;
                        break;
                }
                switch (z) {
                    case false:
                        if (isBootImage) {
                            pw.write("--code-path cannot be used for the boot image.");
                            return -1;
                        }
                        codePath2 = getNextArg();
                    default:
                        pw.write("Unknown arg: " + opt);
                        return -1;
                }
            } else {
                if (isBootImage) {
                    codePath = codePath2;
                    baseCodePath = null;
                } else {
                    PackageInfo packageInfo = this.mInterface.getPackageInfo(packageName, 0L, 0);
                    if (packageInfo == null) {
                        pw.write("Package not found " + packageName);
                        return -1;
                    }
                    String baseCodePath2 = packageInfo.applicationInfo.getBaseCodePath();
                    if (codePath2 != null) {
                        codePath = codePath2;
                        baseCodePath = baseCodePath2;
                    } else {
                        codePath = baseCodePath2;
                        baseCodePath = baseCodePath2;
                    }
                }
                SnapshotRuntimeProfileCallback callback = new SnapshotRuntimeProfileCallback();
                String callingPackage = Binder.getCallingUid() == 0 ? "root" : VibratorManagerService.VibratorManagerShellCommand.SHELL_PACKAGE_NAME;
                int profileType = isBootImage ? 1 : 0;
                if (!this.mInterface.getArtManager().isRuntimeProfilingEnabled(profileType, callingPackage)) {
                    pw.println("Error: Runtime profiling is not enabled");
                    return -1;
                }
                this.mInterface.getArtManager().snapshotRuntimeProfile(profileType, packageName, codePath, callback, callingPackage);
                if (!callback.waitTillDone()) {
                    pw.println("Error: callback not called");
                    return callback.mErrCode;
                }
                try {
                    InputStream inStream = new ParcelFileDescriptor.AutoCloseInputStream(callback.mProfileReadFd);
                    if (!isBootImage && !Objects.equals(baseCodePath, codePath)) {
                        str = STDIN_PATH + new File(codePath).getName();
                        String outputFileSuffix = str;
                        String outputProfilePath = ART_PROFILE_SNAPSHOT_DEBUG_LOCATION + packageName + outputFileSuffix + ".prof";
                        OutputStream outStream = new FileOutputStream(outputProfilePath);
                        Streams.copy(inStream, outStream);
                        outStream.close();
                        Os.chmod(outputProfilePath, FrameworkStatsLog.VBMETA_DIGEST_REPORTED);
                        inStream.close();
                        return 0;
                    }
                    str = "";
                    String outputFileSuffix2 = str;
                    String outputProfilePath2 = ART_PROFILE_SNAPSHOT_DEBUG_LOCATION + packageName + outputFileSuffix2 + ".prof";
                    OutputStream outStream2 = new FileOutputStream(outputProfilePath2);
                    Streams.copy(inStream, outStream2);
                    outStream2.close();
                    Os.chmod(outputProfilePath2, FrameworkStatsLog.VBMETA_DIGEST_REPORTED);
                    inStream.close();
                    return 0;
                } catch (ErrnoException | IOException e) {
                    pw.println("Error when reading the profile fd: " + e.getMessage());
                    e.printStackTrace(pw);
                    return -1;
                }
            }
        }
    }

    private ArrayList<String> getRemainingArgs() {
        ArrayList<String> args = new ArrayList<>();
        while (true) {
            String arg = getNextArg();
            if (arg != null) {
                args.add(arg);
            } else {
                return args;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class SnapshotRuntimeProfileCallback extends ISnapshotRuntimeProfileCallback.Stub {
        private CountDownLatch mDoneSignal;
        private int mErrCode;
        private ParcelFileDescriptor mProfileReadFd;
        private boolean mSuccess;

        private SnapshotRuntimeProfileCallback() {
            this.mSuccess = false;
            this.mErrCode = -1;
            this.mProfileReadFd = null;
            this.mDoneSignal = new CountDownLatch(1);
        }

        public void onSuccess(ParcelFileDescriptor profileReadFd) {
            this.mSuccess = true;
            try {
                this.mProfileReadFd = profileReadFd.dup();
            } catch (IOException e) {
                e.printStackTrace();
            }
            this.mDoneSignal.countDown();
        }

        public void onError(int errCode) {
            this.mSuccess = false;
            this.mErrCode = errCode;
            this.mDoneSignal.countDown();
        }

        boolean waitTillDone() {
            boolean done = false;
            try {
                done = this.mDoneSignal.await(10000000L, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
            }
            return done && this.mSuccess;
        }
    }

    private int runUninstall() throws RemoteException {
        String str;
        int translatedUserId;
        PrintWriter pw = getOutPrintWriter();
        int flags = 0;
        int userId = -1;
        long versionCode = -1;
        while (true) {
            String opt = getNextOption();
            char c = 65535;
            if (opt != null) {
                switch (opt.hashCode()) {
                    case 1502:
                        if (opt.equals("-k")) {
                            c = 0;
                            break;
                        }
                        break;
                    case 1333469547:
                        if (opt.equals("--user")) {
                            c = 1;
                            break;
                        }
                        break;
                    case 1884113221:
                        if (opt.equals("--versionCode")) {
                            c = 2;
                            break;
                        }
                        break;
                }
                switch (c) {
                    case 0:
                        flags |= 1;
                        break;
                    case 1:
                        userId = UserHandle.parseUserArg(getNextArgRequired());
                        break;
                    case 2:
                        long versionCode2 = Long.parseLong(getNextArgRequired());
                        versionCode = versionCode2;
                        break;
                    default:
                        pw.println("Error: Unknown option: " + opt);
                        return 1;
                }
            } else {
                String packageName = getNextArg();
                if (packageName == null) {
                    pw.println("Error: package name not specified");
                    return 1;
                }
                ArrayList<String> splitNames = getRemainingArgs();
                if (!splitNames.isEmpty()) {
                    return runRemoveSplits(packageName, splitNames);
                }
                if (userId == -1) {
                    flags |= 2;
                }
                int translatedUserId2 = translateUserId(userId, 0, "runUninstall");
                LocalIntentReceiver receiver = new LocalIntentReceiver();
                PackageManagerInternal internal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
                if (internal.isApexPackage(packageName)) {
                    str = "]";
                    internal.uninstallApex(packageName, versionCode, translatedUserId2, receiver.getIntentSender(), flags);
                } else {
                    str = "]";
                    if ((flags & 2) != 0) {
                        translatedUserId = translatedUserId2;
                    } else {
                        translatedUserId = translatedUserId2;
                        PackageInfo info = this.mInterface.getPackageInfo(packageName, 67108864L, translatedUserId);
                        if (info == null) {
                            pw.println("Failure [not installed for " + translatedUserId + str);
                            return 1;
                        }
                        boolean isSystem = (info.applicationInfo.flags & 1) != 0;
                        if (isSystem) {
                            flags |= 4;
                        }
                    }
                    this.mInterface.getPackageInstaller().uninstall(new VersionedPackage(packageName, versionCode), (String) null, ITranPackageManagerService.Instance().runUninstall(new VersionedPackage(packageName, versionCode), userId, flags), receiver.getIntentSender(), translatedUserId);
                }
                Intent result = receiver.getResult();
                int status = result.getIntExtra("android.content.pm.extra.STATUS", 1);
                if (status == 0) {
                    pw.println("Success");
                    return 0;
                }
                pw.println("Failure [" + result.getStringExtra("android.content.pm.extra.STATUS_MESSAGE") + str);
                return 1;
            }
        }
    }

    /* JADX DEBUG: Another duplicated slice has different insns count: {[IF]}, finally: {[IF, MOVE_EXCEPTION, INVOKE, MOVE_EXCEPTION] complete} */
    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [2274=4, 2276=4, 2277=4, 2278=4] */
    private int runRemoveSplits(String packageName, Collection<String> splitNames) throws RemoteException {
        PrintWriter pw = getOutPrintWriter();
        PackageInstaller.SessionParams sessionParams = new PackageInstaller.SessionParams(2);
        sessionParams.installFlags = 2 | sessionParams.installFlags;
        sessionParams.appPackageName = packageName;
        int sessionId = doCreateSession(sessionParams, null, -1);
        boolean abandonSession = true;
        try {
            if (doRemoveSplits(sessionId, splitNames, false) != 0) {
                return 1;
            }
            if (doCommitSession(sessionId, false) != 0) {
                if (1 != 0) {
                    try {
                        doAbandonSession(sessionId, false);
                    } catch (Exception e) {
                    }
                }
                return 1;
            }
            abandonSession = false;
            pw.println("Success");
            if (0 != 0) {
                try {
                    doAbandonSession(sessionId, false);
                } catch (Exception e2) {
                }
            }
            return 0;
        } finally {
            if (abandonSession) {
                try {
                    doAbandonSession(sessionId, false);
                } catch (Exception e3) {
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static class ClearDataObserver extends IPackageDataObserver.Stub {
        boolean finished;
        boolean result;

        ClearDataObserver() {
        }

        public void onRemoveCompleted(String packageName, boolean succeeded) throws RemoteException {
            synchronized (this) {
                this.finished = true;
                this.result = succeeded;
                notifyAll();
            }
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code restructure failed: missing block: B:9:0x001e, code lost:
        if (r3.equals("--user") != false) goto L8;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private int runClear() throws RemoteException {
        PrintWriter pw = getOutPrintWriter();
        int userId = 0;
        boolean cacheOnly = false;
        while (true) {
            String opt = getNextOption();
            boolean z = false;
            if (opt != null) {
                switch (opt.hashCode()) {
                    case -2056884041:
                        if (opt.equals("--cache-only")) {
                            z = true;
                            break;
                        }
                        z = true;
                        break;
                    case 1333469547:
                        break;
                    default:
                        z = true;
                        break;
                }
                switch (z) {
                    case false:
                        userId = UserHandle.parseUserArg(getNextArgRequired());
                        break;
                    case true:
                        cacheOnly = true;
                        break;
                    default:
                        pw.println("Error: Unknown option: " + opt);
                        return 1;
                }
            } else {
                String pkg = getNextArg();
                if (pkg == null) {
                    getErrPrintWriter().println("Error: no package specified");
                    return 1;
                }
                int translatedUserId = translateUserId(userId, -10000, "runClear");
                ClearDataObserver obs = new ClearDataObserver();
                if (!cacheOnly) {
                    ActivityManager.getService().clearApplicationUserData(pkg, false, obs, translatedUserId);
                } else {
                    this.mInterface.deleteApplicationCacheFilesAsUser(pkg, translatedUserId, obs);
                }
                synchronized (obs) {
                    while (!obs.finished) {
                        try {
                            obs.wait();
                        } catch (InterruptedException e) {
                        }
                    }
                }
                if (obs.result) {
                    getOutPrintWriter().println("Success");
                    return 0;
                }
                getErrPrintWriter().println("Failed");
                return 1;
            }
        }
    }

    private static String enabledSettingToString(int state) {
        switch (state) {
            case 0:
                return HealthServiceWrapperHidl.INSTANCE_VENDOR;
            case 1:
                return ServiceConfigAccessor.PROVIDER_MODE_ENABLED;
            case 2:
                return ServiceConfigAccessor.PROVIDER_MODE_DISABLED;
            case 3:
                return "disabled-user";
            case 4:
                return "disabled-until-used";
            default:
                return UiModeManagerService.Shell.NIGHT_MODE_STR_UNKNOWN;
        }
    }

    private int runSetEnabledSetting(int state) throws RemoteException {
        int userId = 0;
        String option = getNextOption();
        if (option != null && option.equals("--user")) {
            userId = UserHandle.parseUserArg(getNextArgRequired());
        }
        String pkg = getNextArg();
        if (pkg == null) {
            getErrPrintWriter().println("Error: no package or component specified");
            return 1;
        }
        int translatedUserId = translateUserId(userId, -10000, "runSetEnabledSetting");
        ComponentName cn = ComponentName.unflattenFromString(pkg);
        if (cn == null) {
            this.mInterface.setApplicationEnabledSetting(pkg, state, 0, translatedUserId, "shell:" + Process.myUid());
            getOutPrintWriter().println("Package " + pkg + " new state: " + enabledSettingToString(this.mInterface.getApplicationEnabledSetting(pkg, translatedUserId)));
            return 0;
        }
        this.mInterface.setComponentEnabledSetting(cn, state, 0, translatedUserId);
        getOutPrintWriter().println("Component " + cn.toShortString() + " new state: " + enabledSettingToString(this.mInterface.getComponentEnabledSetting(cn, translatedUserId)));
        return 0;
    }

    private int runSetHiddenSetting(boolean state) throws RemoteException {
        int userId = 0;
        String option = getNextOption();
        if (option != null && option.equals("--user")) {
            userId = UserHandle.parseUserArg(getNextArgRequired());
        }
        String pkg = getNextArg();
        if (pkg == null) {
            getErrPrintWriter().println("Error: no package or component specified");
            return 1;
        }
        int translatedUserId = translateUserId(userId, -10000, "runSetHiddenSetting");
        this.mInterface.setApplicationHiddenSettingAsUser(pkg, state, translatedUserId);
        getOutPrintWriter().println("Package " + pkg + " new hidden state: " + this.mInterface.getApplicationHiddenSettingAsUser(pkg, translatedUserId));
        return 0;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code restructure failed: missing block: B:25:0x0063, code lost:
        if (r3.equals("hide-notifications") != false) goto L24;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private int runSetDistractingRestriction() {
        boolean z;
        PrintWriter pw = getOutPrintWriter();
        int userId = 0;
        int flags = 0;
        while (true) {
            String opt = getNextOption();
            boolean z2 = false;
            if (opt != null) {
                switch (opt.hashCode()) {
                    case 1333015820:
                        if (opt.equals("--flag")) {
                            z = true;
                            break;
                        }
                        z = true;
                        break;
                    case 1333469547:
                        if (opt.equals("--user")) {
                            z = false;
                            break;
                        }
                        z = true;
                        break;
                    default:
                        z = true;
                        break;
                }
                switch (z) {
                    case false:
                        userId = UserHandle.parseUserArg(getNextArgRequired());
                        break;
                    case true:
                        String flag = getNextArgRequired();
                        switch (flag.hashCode()) {
                            case -2125559907:
                                break;
                            case -1852537225:
                                if (flag.equals("hide-from-suggestions")) {
                                    z2 = true;
                                    break;
                                }
                                z2 = true;
                                break;
                            default:
                                z2 = true;
                                break;
                        }
                        switch (z2) {
                            case false:
                                flags |= 2;
                                continue;
                            case true:
                                flags |= 1;
                                continue;
                            default:
                                pw.println("Unrecognized flag: " + flag);
                                return 1;
                        }
                    default:
                        pw.println("Error: Unknown option: " + opt);
                        return 1;
                }
            } else {
                List<String> packageNames = getRemainingArgs();
                if (packageNames.isEmpty()) {
                    pw.println("Error: package name not specified");
                    return 1;
                }
                try {
                    int translatedUserId = translateUserId(userId, -10000, "set-distracting");
                    String[] errored = this.mInterface.setDistractingPackageRestrictionsAsUser((String[]) packageNames.toArray(new String[0]), flags, translatedUserId);
                    if (errored.length > 0) {
                        pw.println("Could not set restriction for: " + Arrays.toString(errored));
                        return 1;
                    }
                    return 0;
                } catch (RemoteException | IllegalArgumentException e) {
                    pw.println(e.toString());
                    return 1;
                }
            }
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code restructure failed: missing block: B:9:0x002d, code lost:
        if (r0.equals("--user") != false) goto L8;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private int runSuspend(boolean suspendedState) {
        SuspendDialogInfo info;
        PrintWriter pw = getOutPrintWriter();
        PersistableBundle appExtras = new PersistableBundle();
        PersistableBundle launcherExtras = new PersistableBundle();
        String dialogMessage = null;
        int userId = 0;
        while (true) {
            String opt = getNextOption();
            char c = 0;
            if (opt != null) {
                switch (opt.hashCode()) {
                    case -39471105:
                        if (opt.equals("--dialogMessage")) {
                            c = 1;
                            break;
                        }
                        c = 65535;
                        break;
                    case 42995488:
                        if (opt.equals("--aed")) {
                            c = 4;
                            break;
                        }
                        c = 65535;
                        break;
                    case 42995496:
                        if (opt.equals("--ael")) {
                            c = 2;
                            break;
                        }
                        c = 65535;
                        break;
                    case 42995503:
                        if (opt.equals("--aes")) {
                            c = 3;
                            break;
                        }
                        c = 65535;
                        break;
                    case 43006059:
                        if (opt.equals("--led")) {
                            c = 7;
                            break;
                        }
                        c = 65535;
                        break;
                    case 43006067:
                        if (opt.equals("--lel")) {
                            c = 5;
                            break;
                        }
                        c = 65535;
                        break;
                    case 43006074:
                        if (opt.equals("--les")) {
                            c = 6;
                            break;
                        }
                        c = 65535;
                        break;
                    case 1333469547:
                        break;
                    default:
                        c = 65535;
                        break;
                }
                switch (c) {
                    case 0:
                        String dialogMessage2 = getNextArgRequired();
                        int userId2 = UserHandle.parseUserArg(dialogMessage2);
                        userId = userId2;
                        break;
                    case 1:
                        String dialogMessage3 = getNextArgRequired();
                        dialogMessage = dialogMessage3;
                        break;
                    case 2:
                    case 3:
                    case 4:
                    case 5:
                    case 6:
                    case 7:
                        String key = getNextArgRequired();
                        String val = getNextArgRequired();
                        if (!suspendedState) {
                            break;
                        } else {
                            PersistableBundle bundleToInsert = opt.startsWith("--a") ? appExtras : launcherExtras;
                            switch (opt.charAt(4)) {
                                case 'd':
                                    bundleToInsert.putDouble(key, Double.valueOf(val).doubleValue());
                                    continue;
                                case 'l':
                                    bundleToInsert.putLong(key, Long.valueOf(val).longValue());
                                    continue;
                                case 's':
                                    bundleToInsert.putString(key, val);
                                    continue;
                            }
                        }
                        break;
                    default:
                        pw.println("Error: Unknown option: " + opt);
                        return 1;
                }
            } else {
                List<String> packageNames = getRemainingArgs();
                if (packageNames.isEmpty()) {
                    pw.println("Error: package name not specified");
                    return 1;
                }
                String callingPackage = Binder.getCallingUid() == 0 ? "root" : VibratorManagerService.VibratorManagerShellCommand.SHELL_PACKAGE_NAME;
                if (!TextUtils.isEmpty(dialogMessage)) {
                    info = new SuspendDialogInfo.Builder().setMessage(dialogMessage).build();
                } else {
                    info = null;
                }
                try {
                    int translatedUserId = translateUserId(userId, -10000, "runSuspend");
                    this.mInterface.setPackagesSuspendedAsUser((String[]) packageNames.toArray(new String[0]), suspendedState, appExtras.size() > 0 ? appExtras : null, launcherExtras.size() > 0 ? launcherExtras : null, info, callingPackage, translatedUserId);
                    for (int i = 0; i < packageNames.size(); i++) {
                        String packageName = packageNames.get(i);
                        pw.println("Package " + packageName + " new suspended state: " + this.mInterface.isPackageSuspendedForUser(packageName, translatedUserId));
                    }
                    return 0;
                } catch (RemoteException | IllegalArgumentException e) {
                    pw.println(e.toString());
                    return 1;
                }
            }
        }
    }

    private int runGrantRevokePermission(boolean grant) throws RemoteException {
        int userId = 0;
        while (true) {
            String opt = getNextOption();
            if (opt == null) {
                break;
            } else if (opt.equals("--user")) {
                userId = UserHandle.parseUserArg(getNextArgRequired());
            }
        }
        String pkg = getNextArg();
        if (pkg == null) {
            getErrPrintWriter().println("Error: no package specified");
            return 1;
        }
        String perm = getNextArg();
        if (perm == null) {
            getErrPrintWriter().println("Error: no permission specified");
            return 1;
        }
        UserHandle translatedUser = UserHandle.of(translateUserId(userId, -10000, "runGrantRevokePermission"));
        if (grant) {
            this.mPermissionManager.grantRuntimePermission(pkg, perm, translatedUser);
            return 0;
        }
        this.mPermissionManager.revokeRuntimePermission(pkg, perm, translatedUser, (String) null);
        return 0;
    }

    private int runResetPermissions() throws RemoteException {
        this.mLegacyPermissionManager.resetRuntimePermissions();
        return 0;
    }

    private int setOrClearPermissionFlags(boolean setFlags) {
        int userId = 0;
        while (true) {
            String opt = getNextOption();
            if (opt == null) {
                break;
            } else if (opt.equals("--user")) {
                userId = UserHandle.parseUserArg(getNextArgRequired());
            }
        }
        String pkg = getNextArg();
        if (pkg == null) {
            getErrPrintWriter().println("Error: no package specified");
            return 1;
        }
        String perm = getNextArg();
        if (perm == null) {
            getErrPrintWriter().println("Error: no permission specified");
            return 1;
        }
        String flagName = getNextArg();
        if (flagName != null) {
            int flagMask = 0;
            String flagName2 = flagName;
            while (flagName2 != null) {
                Map<String, Integer> map = SUPPORTED_PERMISSION_FLAGS;
                if (!map.containsKey(flagName2)) {
                    getErrPrintWriter().println("Error: specified flag " + flagName2 + " is not one of " + SUPPORTED_PERMISSION_FLAGS_LIST);
                    return 1;
                }
                flagMask |= map.get(flagName2).intValue();
                flagName2 = getNextArg();
            }
            UserHandle translatedUser = UserHandle.of(translateUserId(userId, -10000, "runGrantRevokePermission"));
            int flagSet = setFlags ? flagMask : 0;
            this.mPermissionManager.updatePermissionFlags(pkg, perm, flagMask, flagSet, translatedUser);
            return 0;
        }
        getErrPrintWriter().println("Error: no permission flags specified");
        return 1;
    }

    private int runSetPermissionEnforced() throws RemoteException {
        String permission = getNextArg();
        if (permission == null) {
            getErrPrintWriter().println("Error: no permission specified");
            return 1;
        }
        String enforcedRaw = getNextArg();
        if (enforcedRaw == null) {
            getErrPrintWriter().println("Error: no enforcement specified");
            return 1;
        }
        return 0;
    }

    private boolean isVendorApp(String pkg) {
        try {
            PackageInfo info = this.mInterface.getPackageInfo(pkg, 4194304L, 0);
            if (info != null) {
                return info.applicationInfo.isVendor();
            }
            return false;
        } catch (RemoteException e) {
            return false;
        }
    }

    private boolean isProductApp(String pkg) {
        try {
            PackageInfo info = this.mInterface.getPackageInfo(pkg, 4194304L, 0);
            if (info != null) {
                return info.applicationInfo.isProduct();
            }
            return false;
        } catch (RemoteException e) {
            return false;
        }
    }

    private boolean isSystemExtApp(String pkg) {
        try {
            PackageInfo info = this.mInterface.getPackageInfo(pkg, 4194304L, 0);
            if (info != null) {
                return info.applicationInfo.isSystemExt();
            }
            return false;
        } catch (RemoteException e) {
            return false;
        }
    }

    private String getApexPackageNameContainingPackage(String pkg) {
        ApexManager apexManager = ApexManager.getInstance();
        return apexManager.getActiveApexPackageNameContainingPackage(pkg);
    }

    private boolean isApexApp(String pkg) {
        return getApexPackageNameContainingPackage(pkg) != null;
    }

    private int runGetPrivappPermissions() {
        ArraySet<String> privAppPermissions;
        String pkg = getNextArg();
        if (pkg == null) {
            getErrPrintWriter().println("Error: no package specified.");
            return 1;
        }
        if (isVendorApp(pkg)) {
            privAppPermissions = SystemConfig.getInstance().getVendorPrivAppPermissions(pkg);
        } else if (isProductApp(pkg)) {
            privAppPermissions = SystemConfig.getInstance().getProductPrivAppPermissions(pkg);
        } else if (isSystemExtApp(pkg)) {
            privAppPermissions = SystemConfig.getInstance().getSystemExtPrivAppPermissions(pkg);
        } else if (isApexApp(pkg)) {
            String apexName = ApexManager.getInstance().getApexModuleNameForPackageName(getApexPackageNameContainingPackage(pkg));
            privAppPermissions = SystemConfig.getInstance().getApexPrivAppPermissions(apexName, pkg);
        } else if (Build.TRAN_EXTEND_PARTITION_SUPPORT) {
            PackageManagerInternal internal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
            privAppPermissions = getTrPrivPermissions(SystemConfig.getInstance(), internal.getPackage(pkg));
        } else {
            privAppPermissions = SystemConfig.getInstance().getPrivAppPermissions(pkg);
        }
        getOutPrintWriter().println(privAppPermissions == null ? "{}" : privAppPermissions.toString());
        return 0;
    }

    private int runGetPrivappDenyPermissions() {
        ArraySet<String> privAppPermissions;
        String pkg = getNextArg();
        if (pkg == null) {
            getErrPrintWriter().println("Error: no package specified.");
            return 1;
        }
        if (isVendorApp(pkg)) {
            privAppPermissions = SystemConfig.getInstance().getVendorPrivAppDenyPermissions(pkg);
        } else if (isProductApp(pkg)) {
            privAppPermissions = SystemConfig.getInstance().getProductPrivAppDenyPermissions(pkg);
        } else if (isSystemExtApp(pkg)) {
            privAppPermissions = SystemConfig.getInstance().getSystemExtPrivAppDenyPermissions(pkg);
        } else if (isApexApp(pkg)) {
            String apexName = ApexManager.getInstance().getApexModuleNameForPackageName(getApexPackageNameContainingPackage(pkg));
            privAppPermissions = SystemConfig.getInstance().getApexPrivAppDenyPermissions(apexName, pkg);
        } else if (Build.TRAN_EXTEND_PARTITION_SUPPORT) {
            PackageManagerInternal internal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
            privAppPermissions = getTrPrivDenyPermissions(SystemConfig.getInstance(), internal.getPackage(pkg));
        } else {
            privAppPermissions = SystemConfig.getInstance().getPrivAppDenyPermissions(pkg);
        }
        getOutPrintWriter().println(privAppPermissions == null ? "{}" : privAppPermissions.toString());
        return 0;
    }

    private int runGetOemPermissions() {
        String pkg = getNextArg();
        if (pkg == null) {
            getErrPrintWriter().println("Error: no package specified.");
            return 1;
        }
        Map<String, Boolean> oemPermissions = SystemConfig.getInstance().getOemPermissions(pkg);
        if (oemPermissions == null || oemPermissions.isEmpty()) {
            getOutPrintWriter().println("{}");
            return 0;
        }
        oemPermissions.forEach(new BiConsumer() { // from class: com.android.server.pm.PackageManagerShellCommand$$ExternalSyntheticLambda2
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                PackageManagerShellCommand.this.m5582x52a56374((String) obj, (Boolean) obj2);
            }
        });
        return 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$runGetOemPermissions$1$com-android-server-pm-PackageManagerShellCommand  reason: not valid java name */
    public /* synthetic */ void m5582x52a56374(String permission, Boolean granted) {
        getOutPrintWriter().println(permission + " granted:" + granted);
    }

    private ArraySet<String> getTrPrivPermissions(SystemConfig systemConfig, AndroidPackage pkg) {
        if (pkg.getPath() == null) {
            return null;
        }
        boolean trProduct = pkg.getPath().startsWith(Environment.getTrProductDirectory().toPath() + SliceClientPermissions.SliceAuthority.DELIMITER);
        boolean trMi = pkg.getPath().startsWith(Environment.getTrMiDirectory().toPath() + SliceClientPermissions.SliceAuthority.DELIMITER);
        boolean trPreload = pkg.getPath().startsWith(Environment.getTrPreloadDirectory().toPath() + SliceClientPermissions.SliceAuthority.DELIMITER);
        boolean trCompany = pkg.getPath().startsWith(Environment.getTrCompanyDirectory().toPath() + SliceClientPermissions.SliceAuthority.DELIMITER);
        boolean trRegion = pkg.getPath().startsWith(Environment.getTrRegionDirectory().toPath() + SliceClientPermissions.SliceAuthority.DELIMITER);
        boolean trCarrier = pkg.getPath().startsWith(Environment.getTrCarrierDirectory().toPath() + SliceClientPermissions.SliceAuthority.DELIMITER);
        boolean trTheme = pkg.getPath().startsWith(Environment.getTrThemeDirectory().toPath() + SliceClientPermissions.SliceAuthority.DELIMITER);
        if (trProduct) {
            ArraySet<String> permissions = systemConfig.getTrProductPrivAppPermissions(pkg.getPackageName());
            return permissions;
        } else if (trMi) {
            ArraySet<String> permissions2 = systemConfig.getTrMiPrivAppPermissions(pkg.getPackageName());
            return permissions2;
        } else if (trPreload) {
            Slog.w("PackageManagerShellCommandPermissionManagerService", "isInSystemConfigPrivAppPermissions path:" + pkg.getPath());
            ArraySet<String> permissions3 = systemConfig.getTrPreloadPrivAppPermissions(pkg.getPackageName());
            return permissions3;
        } else if (trCompany) {
            ArraySet<String> permissions4 = systemConfig.getTrCompanyPrivAppPermissions(pkg.getPackageName());
            return permissions4;
        } else if (trRegion) {
            ArraySet<String> permissions5 = systemConfig.getTrRegionPrivAppPermissions(pkg.getPackageName());
            return permissions5;
        } else if (trCarrier) {
            ArraySet<String> permissions6 = systemConfig.getTrCarrierPrivAppPermissions(pkg.getPackageName());
            return permissions6;
        } else if (trTheme) {
            ArraySet<String> permissions7 = systemConfig.getTrThemePrivAppPermissions(pkg.getPackageName());
            return permissions7;
        } else {
            ArraySet<String> permissions8 = systemConfig.getPrivAppPermissions(pkg.getPackageName());
            return permissions8;
        }
    }

    private ArraySet<String> getTrPrivDenyPermissions(SystemConfig systemConfig, AndroidPackage pkg) {
        if (pkg.getPath() == null) {
            return null;
        }
        boolean trProduct = pkg.getPath().startsWith(Environment.getTrProductDirectory().toPath() + SliceClientPermissions.SliceAuthority.DELIMITER);
        boolean trMi = pkg.getPath().startsWith(Environment.getTrMiDirectory().toPath() + SliceClientPermissions.SliceAuthority.DELIMITER);
        boolean trPreload = pkg.getPath().startsWith(Environment.getTrPreloadDirectory().toPath() + SliceClientPermissions.SliceAuthority.DELIMITER);
        boolean trCompany = pkg.getPath().startsWith(Environment.getTrCompanyDirectory().toPath() + SliceClientPermissions.SliceAuthority.DELIMITER);
        boolean trRegion = pkg.getPath().startsWith(Environment.getTrRegionDirectory().toPath() + SliceClientPermissions.SliceAuthority.DELIMITER);
        boolean trCarrier = pkg.getPath().startsWith(Environment.getTrCarrierDirectory().toPath() + SliceClientPermissions.SliceAuthority.DELIMITER);
        boolean trTheme = pkg.getPath().startsWith(Environment.getTrThemeDirectory().toPath() + SliceClientPermissions.SliceAuthority.DELIMITER);
        if (trProduct) {
            ArraySet<String> permissions = systemConfig.getTrProductPrivAppDenyPermissions(pkg.getPackageName());
            return permissions;
        } else if (trMi) {
            ArraySet<String> permissions2 = systemConfig.getTrMiPrivAppDenyPermissions(pkg.getPackageName());
            return permissions2;
        } else if (trPreload) {
            ArraySet<String> permissions3 = systemConfig.getTrPreloadPrivAppDenyPermissions(pkg.getPackageName());
            return permissions3;
        } else if (trCompany) {
            ArraySet<String> permissions4 = systemConfig.getTrCompanyPrivAppDenyPermissions(pkg.getPackageName());
            return permissions4;
        } else if (trRegion) {
            ArraySet<String> permissions5 = systemConfig.getTrRegionPrivAppDenyPermissions(pkg.getPackageName());
            return permissions5;
        } else if (trCarrier) {
            ArraySet<String> permissions6 = systemConfig.getTrCarrierPrivAppDenyPermissions(pkg.getPackageName());
            return permissions6;
        } else if (trTheme) {
            ArraySet<String> permissions7 = systemConfig.getTrThemePrivAppDenyPermissions(pkg.getPackageName());
            return permissions7;
        } else {
            ArraySet<String> permissions8 = systemConfig.getPrivAppDenyPermissions(pkg.getPackageName());
            return permissions8;
        }
    }

    private int runTrimCaches() throws RemoteException {
        long multiplier;
        long multiplier2;
        String size;
        String volumeUuid;
        String size2 = getNextArg();
        if (size2 == null) {
            getErrPrintWriter().println("Error: no size specified");
            return 1;
        }
        int len = size2.length();
        char c = size2.charAt(len - 1);
        if (c >= '0' && c <= '9') {
            multiplier2 = 1;
            size = size2;
        } else {
            if (c == 'K' || c == 'k') {
                multiplier = GadgetFunction.NCM;
            } else if (c == 'M' || c == 'm') {
                multiplier = 1048576;
            } else if (c == 'G' || c == 'g') {
                multiplier = 1073741824;
            } else {
                getErrPrintWriter().println("Invalid suffix: " + c);
                return 1;
            }
            multiplier2 = multiplier;
            size = size2.substring(0, len - 1);
        }
        try {
            long sizeVal = Long.parseLong(size) * multiplier2;
            String volumeUuid2 = getNextArg();
            if (!"internal".equals(volumeUuid2)) {
                volumeUuid = volumeUuid2;
            } else {
                volumeUuid = null;
            }
            ClearDataObserver obs = new ClearDataObserver();
            this.mInterface.freeStorageAndNotify(volumeUuid, sizeVal, 2, obs);
            synchronized (obs) {
                while (!obs.finished) {
                    try {
                        obs.wait();
                    } catch (InterruptedException e) {
                    }
                }
            }
            return 0;
        } catch (NumberFormatException e2) {
            getErrPrintWriter().println("Error: expected number at: " + size);
            return 1;
        }
    }

    private static boolean isNumber(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [2967=4] */
    /* JADX WARN: Removed duplicated region for block: B:84:0x01be  */
    /* JADX WARN: Removed duplicated region for block: B:86:0x01db  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public int runCreateUser() throws RemoteException {
        String userType;
        long j;
        boolean preCreateOnly = false;
        int userId = -1;
        String userType2 = null;
        int flags = 0;
        while (true) {
            String opt = getNextOption();
            if (opt == null) {
                String arg = getNextArg();
                if (arg == null && !preCreateOnly) {
                    getErrPrintWriter().println("Error: no user name specified.");
                    return 1;
                }
                if (arg != null && preCreateOnly) {
                    getErrPrintWriter().println("Warning: name is ignored for pre-created users");
                }
                UserInfo info = null;
                IUserManager um = IUserManager.Stub.asInterface(ServiceManager.getService("user"));
                IAccountManager accm = IAccountManager.Stub.asInterface(ServiceManager.getService("account"));
                if (userType2 == null) {
                    String userType3 = UserInfo.getDefaultUserType(flags);
                    userType = userType3;
                } else {
                    userType = userType2;
                }
                Trace.traceBegin(262144L, "shell_runCreateUser");
                try {
                    try {
                    } catch (Throwable th) {
                        th = th;
                    }
                } catch (ServiceSpecificException e) {
                    e = e;
                    j = 262144;
                } catch (Throwable th2) {
                    th = th2;
                    j = 262144;
                }
                try {
                } catch (ServiceSpecificException e2) {
                    e = e2;
                    j = 262144;
                } catch (Throwable th3) {
                    th = th3;
                    j = 262144;
                    Trace.traceEnd(j);
                    throw th;
                }
                if (UserManager.isUserTypeRestricted(userType)) {
                    int parentUserId = userId >= 0 ? userId : 0;
                    info = um.createRestrictedProfileWithThrow(arg, parentUserId);
                    accm.addSharedAccountsFromParentUser(parentUserId, userId, Process.myUid() == 0 ? "root" : VibratorManagerService.VibratorManagerShellCommand.SHELL_PACKAGE_NAME);
                    j = 262144;
                } else if (userId < 0) {
                    info = preCreateOnly ? um.preCreateUserWithThrow(userType) : um.createUserWithThrow(arg, userType, flags);
                    j = 262144;
                } else {
                    j = 262144;
                    try {
                        info = um.createProfileForUserWithThrow(arg, userType, flags, userId, (String[]) null);
                    } catch (ServiceSpecificException e3) {
                        e = e3;
                        getErrPrintWriter().println("Error: " + e);
                        Trace.traceEnd(j);
                        if (info == null) {
                        }
                    }
                }
                Trace.traceEnd(j);
                if (info == null) {
                    getOutPrintWriter().println("Success: created user id " + info.id);
                    return 0;
                }
                getErrPrintWriter().println("Error: couldn't create User.");
                return 1;
            }
            String newUserType = null;
            if ("--profileOf".equals(opt)) {
                userId = translateUserId(UserHandle.parseUserArg(getNextArgRequired()), -1, "runCreateUser");
            } else if ("--managed".equals(opt)) {
                newUserType = "android.os.usertype.profile.MANAGED";
            } else if ("--restricted".equals(opt)) {
                newUserType = "android.os.usertype.full.RESTRICTED";
            } else if ("--guest".equals(opt)) {
                newUserType = "android.os.usertype.full.GUEST";
            } else if ("--demo".equals(opt)) {
                newUserType = "android.os.usertype.full.DEMO";
            } else if ("--ephemeral".equals(opt)) {
                flags |= 256;
            } else if ("--pre-create-only".equals(opt)) {
                preCreateOnly = true;
            } else if (!"--user-type".equals(opt)) {
                getErrPrintWriter().println("Error: unknown option " + opt);
                return 1;
            } else {
                newUserType = getNextArgRequired();
            }
            if (newUserType != null) {
                if (userType2 != null && !userType2.equals(newUserType)) {
                    getErrPrintWriter().println("Error: more than one user type was specified (" + userType2 + " and " + newUserType + ")");
                    return 1;
                }
                userType2 = newUserType;
            }
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code restructure failed: missing block: B:15:0x002e, code lost:
        if (r2.equals("--set-ephemeral-if-in-use") != false) goto L9;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public int runRemoveUser() throws RemoteException {
        boolean setEphemeralIfInUse = false;
        boolean wait = false;
        while (true) {
            String arg = getNextOption();
            char c = 0;
            if (arg != null) {
                switch (arg.hashCode()) {
                    case -1095309356:
                        break;
                    case 1514:
                        if (arg.equals("-w")) {
                            c = 2;
                            break;
                        }
                        c = 65535;
                        break;
                    case 1333511957:
                        if (arg.equals("--wait")) {
                            c = 1;
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
                        setEphemeralIfInUse = true;
                        break;
                    case 1:
                    case 2:
                        wait = true;
                        break;
                    default:
                        getErrPrintWriter().println("Error: unknown option: " + arg);
                        return -1;
                }
            } else {
                String arg2 = getNextArg();
                if (arg2 == null) {
                    getErrPrintWriter().println("Error: no user id specified.");
                    return 1;
                }
                int userId = UserHandle.parseUserArg(arg2);
                IUserManager um = IUserManager.Stub.asInterface(ServiceManager.getService("user"));
                if (setEphemeralIfInUse) {
                    return removeUserWhenPossible(um, userId);
                }
                boolean success = wait ? removeUserAndWait(um, userId) : removeUser(um, userId);
                if (success) {
                    getOutPrintWriter().println("Success: removed user");
                    return 0;
                }
                return 1;
            }
        }
    }

    private boolean removeUser(IUserManager um, int userId) throws RemoteException {
        Slog.i(TAG, "Removing user " + userId);
        if (um.removeUser(userId)) {
            return true;
        }
        getErrPrintWriter().println("Error: couldn't remove user id " + userId);
        return false;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [3069=6] */
    private boolean removeUserAndWait(IUserManager um, final int userId) throws RemoteException {
        Slog.i(TAG, "Removing (and waiting for completion) user " + userId);
        final CountDownLatch waitLatch = new CountDownLatch(1);
        UserManagerInternal.UserLifecycleListener listener = new UserManagerInternal.UserLifecycleListener() { // from class: com.android.server.pm.PackageManagerShellCommand.5
            @Override // com.android.server.pm.UserManagerInternal.UserLifecycleListener
            public void onUserRemoved(UserInfo user) {
                if (userId == user.id) {
                    waitLatch.countDown();
                }
            }
        };
        UserManagerInternal umi = (UserManagerInternal) LocalServices.getService(UserManagerInternal.class);
        umi.addUserLifecycleListener(listener);
        try {
            if (!um.removeUser(userId)) {
                getErrPrintWriter().println("Error: couldn't remove user id " + userId);
                return false;
            }
            boolean awaitSuccess = waitLatch.await(10L, TimeUnit.MINUTES);
            if (awaitSuccess) {
                return true;
            }
            getErrPrintWriter().printf("Error: Remove user %d timed out\n", Integer.valueOf(userId));
            return false;
        } catch (InterruptedException e) {
            getErrPrintWriter().printf("Error: Remove user %d wait interrupted: %s\n", Integer.valueOf(userId), e);
            Thread.currentThread().interrupt();
            return false;
        } finally {
            umi.removeUserLifecycleListener(listener);
        }
    }

    private int removeUserWhenPossible(IUserManager um, int userId) throws RemoteException {
        Slog.i(TAG, "Removing " + userId + " or set as ephemeral if in use.");
        int result = um.removeUserWhenPossible(userId, false);
        switch (result) {
            case 0:
                getOutPrintWriter().printf("Success: user %d removed\n", Integer.valueOf(userId));
                return 0;
            case 1:
                getOutPrintWriter().printf("Success: user %d set as ephemeral\n", Integer.valueOf(userId));
                return 0;
            case 2:
                getOutPrintWriter().printf("Success: user %d is already being removed\n", Integer.valueOf(userId));
                return 0;
            default:
                getErrPrintWriter().printf("Error: couldn't remove or mark ephemeral user id %d\n", Integer.valueOf(userId));
                return 1;
        }
    }

    public int runSetUserRestriction() throws RemoteException {
        boolean value;
        int userId = 0;
        String opt = getNextOption();
        if (opt != null && "--user".equals(opt)) {
            userId = UserHandle.parseUserArg(getNextArgRequired());
        }
        String restriction = getNextArg();
        String arg = getNextArg();
        if ("1".equals(arg)) {
            value = true;
        } else if ("0".equals(arg)) {
            value = false;
        } else {
            getErrPrintWriter().println("Error: valid value not specified");
            return 1;
        }
        int translatedUserId = translateUserId(userId, -10000, "runSetUserRestriction");
        IUserManager um = IUserManager.Stub.asInterface(ServiceManager.getService("user"));
        um.setUserRestriction(restriction, value, translatedUserId);
        return 0;
    }

    public int runGetMaxUsers() {
        getOutPrintWriter().println("Maximum supported users: " + UserManager.getMaxSupportedUsers());
        return 0;
    }

    public int runGetMaxRunningUsers() {
        ActivityManagerInternal activityManagerInternal = (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
        getOutPrintWriter().println("Maximum supported running users: " + activityManagerInternal.getMaxRunningUsers());
        return 0;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class InstallParams {
        String installerPackageName;
        PackageInstaller.SessionParams sessionParams;
        long stagedReadyTimeoutMs;
        int userId;

        private InstallParams() {
            this.userId = -1;
            this.stagedReadyTimeoutMs = 60000L;
        }
    }

    private InstallParams makeInstallParams(Set<String> unsupportedOptions) {
        PackageInstaller.SessionParams sessionParams = new PackageInstaller.SessionParams(1);
        InstallParams params = new InstallParams();
        params.sessionParams = sessionParams;
        sessionParams.installFlags |= 4194304;
        sessionParams.setPackageSource(1);
        boolean replaceExisting = true;
        boolean forceNonStaged = false;
        while (true) {
            String opt = getNextOption();
            if (opt != null) {
                if (unsupportedOptions.contains(opt)) {
                    throw new IllegalArgumentException("Unsupported option " + opt);
                }
                char c = 65535;
                switch (opt.hashCode()) {
                    case -2091380650:
                        if (opt.equals("--install-reason")) {
                            c = 22;
                            break;
                        }
                        break;
                    case -1950997763:
                        if (opt.equals("--force-uuid")) {
                            c = 23;
                            break;
                        }
                        break;
                    case -1816313368:
                        if (opt.equals("--force-non-staged")) {
                            c = 26;
                            break;
                        }
                        break;
                    case -1777984902:
                        if (opt.equals("--dont-kill")) {
                            c = '\b';
                            break;
                        }
                        break;
                    case -1313152697:
                        if (opt.equals("--install-location")) {
                            c = 21;
                            break;
                        }
                        break;
                    case -1137116608:
                        if (opt.equals("--instantapp")) {
                            c = 17;
                            break;
                        }
                        break;
                    case -951415743:
                        if (opt.equals("--instant")) {
                            c = 16;
                            break;
                        }
                        break;
                    case -706813505:
                        if (opt.equals("--referrer")) {
                            c = '\n';
                            break;
                        }
                        break;
                    case -653924786:
                        if (opt.equals("--enable-rollback")) {
                            c = 30;
                            break;
                        }
                        break;
                    case -170474990:
                        if (opt.equals("--multi-package")) {
                            c = 27;
                            break;
                        }
                        break;
                    case -158482320:
                        if (opt.equals("--staged-ready-timeout")) {
                            c = 31;
                            break;
                        }
                        break;
                    case 1477:
                        if (opt.equals("-R")) {
                            c = 1;
                            break;
                        }
                        break;
                    case 1478:
                        if (opt.equals("-S")) {
                            c = '\r';
                            break;
                        }
                        break;
                    case 1495:
                        if (opt.equals("-d")) {
                            c = 5;
                            break;
                        }
                        break;
                    case 1497:
                        if (opt.equals("-f")) {
                            c = 4;
                            break;
                        }
                        break;
                    case 1498:
                        if (opt.equals("-g")) {
                            c = 6;
                            break;
                        }
                        break;
                    case NetworkConstants.ETHER_MTU /* 1500 */:
                        if (opt.equals("-i")) {
                            c = 2;
                            break;
                        }
                        break;
                    case 1507:
                        if (opt.equals("-p")) {
                            c = 11;
                            break;
                        }
                        break;
                    case 1509:
                        if (opt.equals("-r")) {
                            c = 0;
                            break;
                        }
                        break;
                    case 1511:
                        if (opt.equals("-t")) {
                            c = 3;
                            break;
                        }
                        break;
                    case 42995400:
                        if (opt.equals("--abi")) {
                            c = 14;
                            break;
                        }
                        break;
                    case 43010092:
                        if (opt.equals("--pkg")) {
                            c = '\f';
                            break;
                        }
                        break;
                    case 77141024:
                        if (opt.equals("--force-queryable")) {
                            c = 29;
                            break;
                        }
                        break;
                    case 148207464:
                        if (opt.equals("--originating-uri")) {
                            c = '\t';
                            break;
                        }
                        break;
                    case 1051781117:
                        if (opt.equals("--ephemeral")) {
                            c = 15;
                            break;
                        }
                        break;
                    case 1067504745:
                        if (opt.equals("--preload")) {
                            c = 19;
                            break;
                        }
                        break;
                    case 1332870850:
                        if (opt.equals("--apex")) {
                            c = 25;
                            break;
                        }
                        break;
                    case 1333024815:
                        if (opt.equals("--full")) {
                            c = 18;
                            break;
                        }
                        break;
                    case 1333469547:
                        if (opt.equals("--user")) {
                            c = 20;
                            break;
                        }
                        break;
                    case 1494514835:
                        if (opt.equals("--restrict-permissions")) {
                            c = 7;
                            break;
                        }
                        break;
                    case 1507519174:
                        if (opt.equals("--staged")) {
                            c = 28;
                            break;
                        }
                        break;
                    case 2015272120:
                        if (opt.equals("--force-sdk")) {
                            c = 24;
                            break;
                        }
                        break;
                    case 2037590537:
                        if (opt.equals("--skip-verification")) {
                            c = ' ';
                            break;
                        }
                        break;
                }
                switch (c) {
                    case 0:
                    case 24:
                        break;
                    case 1:
                        replaceExisting = false;
                        break;
                    case 2:
                        params.installerPackageName = getNextArg();
                        if (params.installerPackageName != null) {
                            break;
                        } else {
                            throw new IllegalArgumentException("Missing installer package");
                        }
                    case 3:
                        sessionParams.installFlags |= 4;
                        break;
                    case 4:
                        sessionParams.installFlags |= 16;
                        break;
                    case 5:
                        sessionParams.installFlags |= 128;
                        break;
                    case 6:
                        sessionParams.installFlags |= 256;
                        break;
                    case 7:
                        sessionParams.installFlags &= -4194305;
                        break;
                    case '\b':
                        sessionParams.installFlags |= 4096;
                        break;
                    case '\t':
                        sessionParams.originatingUri = Uri.parse(getNextArg());
                        break;
                    case '\n':
                        sessionParams.referrerUri = Uri.parse(getNextArg());
                        break;
                    case 11:
                        sessionParams.mode = 2;
                        sessionParams.appPackageName = getNextArg();
                        if (sessionParams.appPackageName != null) {
                            break;
                        } else {
                            throw new IllegalArgumentException("Missing inherit package name");
                        }
                    case '\f':
                        sessionParams.appPackageName = getNextArg();
                        if (sessionParams.appPackageName != null) {
                            break;
                        } else {
                            throw new IllegalArgumentException("Missing package name");
                        }
                    case '\r':
                        long sizeBytes = Long.parseLong(getNextArg());
                        if (sizeBytes <= 0) {
                            throw new IllegalArgumentException("Size must be positive");
                        }
                        sessionParams.setSize(sizeBytes);
                        break;
                    case 14:
                        sessionParams.abiOverride = checkAbiArgument(getNextArg());
                        break;
                    case 15:
                    case 16:
                    case 17:
                        sessionParams.setInstallAsInstantApp(true);
                        break;
                    case 18:
                        sessionParams.setInstallAsInstantApp(false);
                        break;
                    case 19:
                        sessionParams.setInstallAsVirtualPreload();
                        break;
                    case 20:
                        params.userId = UserHandle.parseUserArg(getNextArgRequired());
                        break;
                    case 21:
                        sessionParams.installLocation = Integer.parseInt(getNextArg());
                        break;
                    case 22:
                        sessionParams.installReason = Integer.parseInt(getNextArg());
                        break;
                    case 23:
                        sessionParams.installFlags |= 512;
                        sessionParams.volumeUuid = getNextArg();
                        if (!"internal".equals(sessionParams.volumeUuid)) {
                            break;
                        } else {
                            sessionParams.volumeUuid = null;
                            break;
                        }
                    case 25:
                        sessionParams.setInstallAsApex();
                        sessionParams.setStaged();
                        break;
                    case 26:
                        forceNonStaged = true;
                        break;
                    case 27:
                        sessionParams.setMultiPackage();
                        break;
                    case 28:
                        sessionParams.setStaged();
                        break;
                    case 29:
                        sessionParams.setForceQueryable();
                        break;
                    case 30:
                        if (params.installerPackageName == null) {
                            params.installerPackageName = VibratorManagerService.VibratorManagerShellCommand.SHELL_PACKAGE_NAME;
                        }
                        sessionParams.installFlags |= 262144;
                        break;
                    case 31:
                        params.stagedReadyTimeoutMs = Long.parseLong(getNextArgRequired());
                        break;
                    case ' ':
                        sessionParams.installFlags |= 524288;
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown option " + opt);
                }
            } else {
                if (replaceExisting) {
                    sessionParams.installFlags |= 2;
                }
                if (forceNonStaged) {
                    sessionParams.isStaged = false;
                }
                return params;
            }
        }
    }

    /* JADX WARN: Code restructure failed: missing block: B:9:0x0020, code lost:
        if (r0.equals("--user") != false) goto L8;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private int runSetHomeActivity() {
        String pkgName;
        RoleManager roleManager;
        UserHandle of;
        Executor executor;
        PrintWriter pw = getOutPrintWriter();
        int userId = 0;
        while (true) {
            String opt = getNextOption();
            boolean z = false;
            if (opt != null) {
                switch (opt.hashCode()) {
                    case 1333469547:
                        break;
                    default:
                        z = true;
                        break;
                }
                switch (z) {
                    case false:
                        userId = UserHandle.parseUserArg(getNextArgRequired());
                    default:
                        pw.println("Error: Unknown option: " + opt);
                        return 1;
                }
            } else {
                String component = getNextArg();
                if (component.indexOf(47) < 0) {
                    pkgName = component;
                } else {
                    ComponentName componentName = component != null ? ComponentName.unflattenFromString(component) : null;
                    if (componentName == null) {
                        pw.println("Error: invalid component name");
                        return 1;
                    }
                    pkgName = componentName.getPackageName();
                }
                int translatedUserId = translateUserId(userId, -10000, "runSetHomeActivity");
                final CompletableFuture<Boolean> future = new CompletableFuture<>();
                try {
                    roleManager = (RoleManager) this.mContext.getSystemService(RoleManager.class);
                    of = UserHandle.of(translatedUserId);
                    executor = FgThread.getExecutor();
                    Objects.requireNonNull(future);
                } catch (Exception e) {
                    e = e;
                }
                try {
                    roleManager.addRoleHolderAsUser("android.app.role.HOME", pkgName, 0, of, executor, new Consumer() { // from class: com.android.server.pm.PackageManagerShellCommand$$ExternalSyntheticLambda1
                        @Override // java.util.function.Consumer
                        public final void accept(Object obj) {
                            future.complete((Boolean) obj);
                        }
                    });
                    boolean success = future.get().booleanValue();
                    if (success) {
                        pw.println("Success");
                        return 0;
                    }
                    pw.println("Error: Failed to set default home.");
                    return 1;
                } catch (Exception e2) {
                    e = e2;
                    pw.println(e.toString());
                    return 1;
                }
            }
        }
    }

    private int runSetInstaller() throws RemoteException {
        String targetPackage = getNextArg();
        String installerPackageName = getNextArg();
        if (targetPackage == null || installerPackageName == null) {
            getErrPrintWriter().println("Must provide both target and installer package names");
            return 1;
        }
        this.mInterface.setInstallerPackageName(targetPackage, installerPackageName);
        getOutPrintWriter().println("Success");
        return 0;
    }

    private int runGetInstantAppResolver() {
        PrintWriter pw = getOutPrintWriter();
        try {
            ComponentName instantAppsResolver = this.mInterface.getInstantAppResolverComponent();
            if (instantAppsResolver == null) {
                return 1;
            }
            pw.println(instantAppsResolver.flattenToString());
            return 0;
        } catch (Exception e) {
            pw.println(e.toString());
            return 1;
        }
    }

    private int runHasFeature() {
        int version;
        PrintWriter err = getErrPrintWriter();
        String featureName = getNextArg();
        if (featureName == null) {
            err.println("Error: expected FEATURE name");
            return 1;
        }
        String versionString = getNextArg();
        if (versionString == null) {
            version = 0;
        } else {
            try {
                version = Integer.parseInt(versionString);
            } catch (RemoteException e) {
                err.println(e.toString());
                return 1;
            } catch (NumberFormatException e2) {
                err.println("Error: illegal version number " + versionString);
                return 1;
            }
        }
        boolean hasFeature = this.mInterface.hasSystemFeature(featureName, version);
        getOutPrintWriter().println(hasFeature);
        if (!hasFeature) {
            return 1;
        }
        return 0;
    }

    private int runDump() {
        String pkg = getNextArg();
        if (pkg == null) {
            getErrPrintWriter().println("Error: no package specified");
            return 1;
        }
        ActivityManager.dumpPackageStateStatic(getOutFileDescriptor(), pkg);
        return 0;
    }

    private int runSetHarmfulAppWarning() throws RemoteException {
        int userId = -2;
        while (true) {
            String opt = getNextOption();
            if (opt != null) {
                if (opt.equals("--user")) {
                    userId = UserHandle.parseUserArg(getNextArgRequired());
                } else {
                    getErrPrintWriter().println("Error: Unknown option: " + opt);
                    return -1;
                }
            } else {
                int translatedUserId = translateUserId(userId, -10000, "runSetHarmfulAppWarning");
                String packageName = getNextArgRequired();
                String warning = getNextArg();
                this.mInterface.setHarmfulAppWarning(packageName, warning, translatedUserId);
                return 0;
            }
        }
    }

    private int runGetHarmfulAppWarning() throws RemoteException {
        int userId = -2;
        while (true) {
            String opt = getNextOption();
            if (opt != null) {
                if (opt.equals("--user")) {
                    userId = UserHandle.parseUserArg(getNextArgRequired());
                } else {
                    getErrPrintWriter().println("Error: Unknown option: " + opt);
                    return -1;
                }
            } else {
                int translatedUserId = translateUserId(userId, -10000, "runGetHarmfulAppWarning");
                String packageName = getNextArgRequired();
                CharSequence warning = this.mInterface.getHarmfulAppWarning(packageName, translatedUserId);
                if (!TextUtils.isEmpty(warning)) {
                    getOutPrintWriter().println(warning);
                    return 0;
                }
                return 1;
            }
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code restructure failed: missing block: B:12:0x0028, code lost:
        if (r4.equals("--throttle-time") != false) goto L9;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private int runSetSilentUpdatesPolicy() {
        PrintWriter pw = getOutPrintWriter();
        String installerPackageName = null;
        Long throttleTimeInSeconds = null;
        boolean reset = false;
        while (true) {
            String opt = getNextOption();
            char c = 1;
            if (opt != null) {
                switch (opt.hashCode()) {
                    case -1615291473:
                        if (opt.equals("--reset")) {
                            c = 2;
                            break;
                        }
                        c = 65535;
                        break;
                    case 771584496:
                        break;
                    case 1002172770:
                        if (opt.equals("--allow-unlimited-silent-updates")) {
                            c = 0;
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
                        installerPackageName = getNextArgRequired();
                        break;
                    case 1:
                        throttleTimeInSeconds = Long.valueOf(Long.parseLong(getNextArgRequired()));
                        break;
                    case 2:
                        reset = true;
                        break;
                    default:
                        pw.println("Error: Unknown option: " + opt);
                        return -1;
                }
            } else if (throttleTimeInSeconds != null && throttleTimeInSeconds.longValue() < 0) {
                pw.println("Error: Invalid value for \"--throttle-time\":" + throttleTimeInSeconds);
                return -1;
            } else {
                try {
                    IPackageInstaller installer = this.mInterface.getPackageInstaller();
                    if (reset) {
                        installer.setAllowUnlimitedSilentUpdates((String) null);
                        installer.setSilentUpdatesThrottleTime(-1L);
                    } else {
                        if (installerPackageName != null) {
                            installer.setAllowUnlimitedSilentUpdates(installerPackageName);
                        }
                        if (throttleTimeInSeconds != null) {
                            installer.setSilentUpdatesThrottleTime(throttleTimeInSeconds.longValue());
                        }
                    }
                    return 1;
                } catch (RemoteException e) {
                    pw.println("Failure [" + e.getClass().getName() + " - " + e.getMessage() + "]");
                    return -1;
                }
            }
        }
    }

    private static String checkAbiArgument(String abi) {
        if (TextUtils.isEmpty(abi)) {
            throw new IllegalArgumentException("Missing ABI argument");
        }
        if (STDIN_PATH.equals(abi)) {
            return abi;
        }
        String[] supportedAbis = Build.SUPPORTED_ABIS;
        for (String supportedAbi : supportedAbis) {
            if (supportedAbi.equals(abi)) {
                return abi;
            }
        }
        throw new IllegalArgumentException("ABI " + abi + " not supported on this device");
    }

    private int translateUserId(int userId, int allUserId, String logContext) {
        boolean allowAll = allUserId != -10000;
        int translatedUserId = ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, allowAll, true, logContext, "pm command");
        return translatedUserId == -1 ? allUserId : translatedUserId;
    }

    private int doCreateSession(PackageInstaller.SessionParams params, String installerPackageName, int userId) throws RemoteException {
        if (userId == -1) {
            params.installFlags |= 64;
        }
        int translatedUserId = translateUserId(userId, 0, "doCreateSession");
        int sessionId = this.mInterface.getPackageInstaller().createSession(params, installerPackageName, (String) null, translatedUserId);
        return sessionId;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [3577=7] */
    private int doAddFiles(int sessionId, ArrayList<String> args, long sessionSizeBytes, boolean isApex) throws RemoteException {
        PackageInstaller.Session session = null;
        try {
            try {
            } catch (Throwable th) {
                e = th;
                IoUtils.closeQuietly(session);
                throw e;
            }
        } catch (IllegalArgumentException e) {
            e = e;
        } catch (Throwable th2) {
            e = th2;
        }
        try {
            session = new PackageInstaller.Session(this.mInterface.getPackageInstaller().openSession(sessionId));
            if (!args.isEmpty()) {
                try {
                    if (!STDIN_PATH.equals(args.get(0))) {
                        Iterator<String> it = args.iterator();
                        while (it.hasNext()) {
                            String arg = it.next();
                            int delimLocation = arg.indexOf(58);
                            if (delimLocation == -1) {
                                processArgForLocalFile(arg, session);
                            } else if (processArgForStdin(arg, session) != 0) {
                                IoUtils.closeQuietly(session);
                                return 1;
                            }
                        }
                        IoUtils.closeQuietly(session);
                        return 0;
                    }
                } catch (IllegalArgumentException e2) {
                    e = e2;
                    getErrPrintWriter().println("Failed to add file(s), reason: " + e);
                    getOutPrintWriter().println("Failure [failed to add file(s)]");
                    IoUtils.closeQuietly(session);
                    return 1;
                }
            }
            String name = "base" + RANDOM.nextInt() + "." + (isApex ? "apex" : "apk");
            PackageManagerShellCommandDataLoader.Metadata metadata = PackageManagerShellCommandDataLoader.Metadata.forStdIn(name);
            session.addFile(0, name, sessionSizeBytes, metadata.toByteArray(), null);
            IoUtils.closeQuietly(session);
            return 0;
        } catch (IllegalArgumentException e3) {
            e = e3;
            getErrPrintWriter().println("Failed to add file(s), reason: " + e);
            getOutPrintWriter().println("Failure [failed to add file(s)]");
            IoUtils.closeQuietly(session);
            return 1;
        } catch (Throwable th3) {
            e = th3;
            IoUtils.closeQuietly(session);
            throw e;
        }
    }

    private int processArgForStdin(String arg, PackageInstaller.Session session) {
        String fileId;
        PackageManagerShellCommandDataLoader.Metadata metadata;
        String[] fileDesc = arg.split(":");
        byte[] signature = null;
        int streamingVersion = 0;
        try {
            if (fileDesc.length < 2) {
                getErrPrintWriter().println("Must specify file name and size");
                return 1;
            }
            String name = fileDesc[0];
            long sizeBytes = Long.parseUnsignedLong(fileDesc[1]);
            if (fileDesc.length > 2 && !TextUtils.isEmpty(fileDesc[2])) {
                fileId = fileDesc[2];
            } else {
                fileId = name;
            }
            if (fileDesc.length > 3) {
                signature = Base64.getDecoder().decode(fileDesc[3]);
            }
            if (fileDesc.length > 4 && ((streamingVersion = Integer.parseUnsignedInt(fileDesc[4])) < 0 || streamingVersion > 1)) {
                getErrPrintWriter().println("Unsupported streaming version: " + streamingVersion);
                return 1;
            } else if (TextUtils.isEmpty(name)) {
                getErrPrintWriter().println("Empty file name in: " + arg);
                return 1;
            } else {
                if (signature != null) {
                    PackageManagerShellCommandDataLoader.Metadata metadata2 = streamingVersion == 0 ? PackageManagerShellCommandDataLoader.Metadata.forDataOnlyStreaming(fileId) : PackageManagerShellCommandDataLoader.Metadata.forStreaming(fileId);
                    try {
                        if (signature.length > 0 && V4Signature.readFrom(signature) == null) {
                            getErrPrintWriter().println("V4 signature is invalid in: " + arg);
                            return 1;
                        }
                        metadata = metadata2;
                    } catch (Exception e) {
                        getErrPrintWriter().println("V4 signature is invalid: " + e + " in " + arg);
                        return 1;
                    }
                } else {
                    PackageManagerShellCommandDataLoader.Metadata metadata3 = PackageManagerShellCommandDataLoader.Metadata.forStdIn(fileId);
                    metadata = metadata3;
                }
                session.addFile(0, name, sizeBytes, metadata.toByteArray(), signature);
                return 0;
            }
        } catch (IllegalArgumentException e2) {
            getErrPrintWriter().println("Unable to parse file parameters: " + arg + ", reason: " + e2);
            return 1;
        }
    }

    private long getFileStatSize(File file) {
        ParcelFileDescriptor pfd = openFileForSystem(file.getPath(), ActivityTaskManagerService.DUMP_RECENTS_SHORT_CMD);
        if (pfd == null) {
            throw new IllegalArgumentException("Error: Can't open file: " + file.getPath());
        }
        try {
            return pfd.getStatSize();
        } finally {
            IoUtils.closeQuietly(pfd);
        }
    }

    private void processArgForLocalFile(String arg, PackageInstaller.Session session) {
        byte[] v4signatureBytes;
        File file = new File(arg);
        String name = file.getName();
        long size = getFileStatSize(file);
        PackageManagerShellCommandDataLoader.Metadata metadata = PackageManagerShellCommandDataLoader.Metadata.forLocalFile(arg);
        String v4SignaturePath = arg + ".idsig";
        ParcelFileDescriptor pfd = openFileForSystem(v4SignaturePath, ActivityTaskManagerService.DUMP_RECENTS_SHORT_CMD);
        try {
            if (pfd != null) {
                try {
                    V4Signature v4signature = V4Signature.readFrom(pfd);
                    byte[] v4signatureBytes2 = v4signature.toByteArray();
                    IoUtils.closeQuietly(pfd);
                    v4signatureBytes = v4signatureBytes2;
                } catch (IOException ex) {
                    Slog.e(TAG, "V4 signature file exists but failed to be parsed.", ex);
                    IoUtils.closeQuietly(pfd);
                }
                session.addFile(0, name, size, metadata.toByteArray(), v4signatureBytes);
            }
            v4signatureBytes = null;
            session.addFile(0, name, size, metadata.toByteArray(), v4signatureBytes);
        } catch (Throwable th) {
            IoUtils.closeQuietly(pfd);
            throw th;
        }
    }

    private int doWriteSplits(int sessionId, ArrayList<String> splitPaths, long sessionSizeBytes, boolean isApex) throws RemoteException {
        String splitName;
        boolean multipleSplits = splitPaths.size() > 1;
        Iterator<String> it = splitPaths.iterator();
        while (it.hasNext()) {
            String splitPath = it.next();
            if (multipleSplits) {
                splitName = new File(splitPath).getName();
            } else {
                splitName = "base." + (isApex ? "apex" : "apk");
            }
            if (doWriteSplit(sessionId, splitPath, sessionSizeBytes, splitName, false) != 0) {
                return 1;
            }
        }
        return 0;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [3738=5, 3742=11] */
    private int doWriteSplit(int sessionId, String inPath, long sizeBytes, String splitName, boolean logSuccess) throws RemoteException {
        PrintWriter pw;
        ParcelFileDescriptor fd;
        long sizeBytes2;
        PackageInstaller.Session session = null;
        try {
        } catch (IOException e) {
            e = e;
        } catch (Throwable th) {
            e = th;
        }
        try {
            PackageInstaller.Session session2 = new PackageInstaller.Session(this.mInterface.getPackageInstaller().openSession(sessionId));
            try {
                pw = getOutPrintWriter();
                if (STDIN_PATH.equals(inPath)) {
                    fd = ParcelFileDescriptor.dup(getInFileDescriptor());
                    sizeBytes2 = sizeBytes;
                } else if (inPath != null) {
                    fd = openFileForSystem(inPath, ActivityTaskManagerService.DUMP_RECENTS_SHORT_CMD);
                    if (fd == null) {
                        IoUtils.closeQuietly(session2);
                        return -1;
                    }
                    long sizeBytes3 = fd.getStatSize();
                    if (sizeBytes3 < 0) {
                        try {
                            getErrPrintWriter().println("Unable to get size of: " + inPath);
                            IoUtils.closeQuietly(session2);
                            return -1;
                        } catch (IOException e2) {
                            e = e2;
                            session = session2;
                            try {
                                getErrPrintWriter().println("Error: failed to write; " + e.getMessage());
                                IoUtils.closeQuietly(session);
                                return 1;
                            } catch (Throwable th2) {
                                e = th2;
                                IoUtils.closeQuietly(session);
                                throw e;
                            }
                        } catch (Throwable th3) {
                            e = th3;
                            session = session2;
                            IoUtils.closeQuietly(session);
                            throw e;
                        }
                    }
                    sizeBytes2 = sizeBytes3;
                } else {
                    fd = ParcelFileDescriptor.dup(getInFileDescriptor());
                    sizeBytes2 = sizeBytes;
                }
            } catch (IOException e3) {
                e = e3;
                session = session2;
            } catch (Throwable th4) {
                e = th4;
                session = session2;
            }
            try {
                if (sizeBytes2 <= 0) {
                    getErrPrintWriter().println("Error: must specify a APK size");
                    IoUtils.closeQuietly(session2);
                    return 1;
                }
                session2.write(splitName, 0L, sizeBytes2, fd);
                if (logSuccess) {
                    pw.println("Success: streamed " + sizeBytes2 + " bytes");
                }
                IoUtils.closeQuietly(session2);
                return 0;
            } catch (IOException e4) {
                e = e4;
                session = session2;
                getErrPrintWriter().println("Error: failed to write; " + e.getMessage());
                IoUtils.closeQuietly(session);
                return 1;
            } catch (Throwable th5) {
                e = th5;
                session = session2;
                IoUtils.closeQuietly(session);
                throw e;
            }
        } catch (IOException e5) {
            e = e5;
            getErrPrintWriter().println("Error: failed to write; " + e.getMessage());
            IoUtils.closeQuietly(session);
            return 1;
        } catch (Throwable th6) {
            e = th6;
            IoUtils.closeQuietly(session);
            throw e;
        }
    }

    private int doInstallAddSession(int parentId, int[] sessionIds, boolean logSuccess) throws RemoteException {
        PrintWriter pw = getOutPrintWriter();
        PackageInstaller.Session session = null;
        try {
            session = new PackageInstaller.Session(this.mInterface.getPackageInstaller().openSession(parentId));
            if (!session.isMultiPackage()) {
                getErrPrintWriter().println("Error: parent session ID is not a multi-package session");
                IoUtils.closeQuietly(session);
                return 1;
            }
            for (int i : sessionIds) {
                session.addChildSessionId(i);
            }
            if (logSuccess) {
                pw.println("Success");
            }
            return 0;
        } finally {
            IoUtils.closeQuietly(session);
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [3789=4] */
    private int doRemoveSplits(int sessionId, Collection<String> splitNames, boolean logSuccess) throws RemoteException {
        PrintWriter pw = getOutPrintWriter();
        PackageInstaller.Session session = null;
        try {
            session = new PackageInstaller.Session(this.mInterface.getPackageInstaller().openSession(sessionId));
            for (String splitName : splitNames) {
                session.removeSplit(splitName);
            }
            if (logSuccess) {
                pw.println("Success");
            }
            return 0;
        } catch (IOException e) {
            pw.println("Error: failed to remove split; " + e.getMessage());
            return 1;
        } finally {
            IoUtils.closeQuietly(session);
        }
    }

    private int doCommitSession(int sessionId, boolean logSuccess) throws RemoteException {
        PrintWriter pw = getOutPrintWriter();
        PackageInstaller.Session session = null;
        try {
            session = new PackageInstaller.Session(this.mInterface.getPackageInstaller().openSession(sessionId));
            if (!session.isMultiPackage() && !session.isStaged()) {
                try {
                    DexMetadataHelper.validateDexPaths(session.getNames());
                } catch (IOException | IllegalStateException e) {
                    pw.println("Warning [Could not validate the dex paths: " + e.getMessage() + "]");
                }
            }
            LocalIntentReceiver receiver = new LocalIntentReceiver();
            session.commit(receiver.getIntentSender());
            if (!session.isStaged()) {
                Intent result = receiver.getResult();
                int status = result.getIntExtra("android.content.pm.extra.STATUS", 1);
                if (status == 0) {
                    if (logSuccess) {
                        pw.println("Success");
                    }
                } else {
                    pw.println("Failure [" + result.getStringExtra("android.content.pm.extra.STATUS_MESSAGE") + "]");
                }
                return status;
            }
            if (logSuccess) {
                pw.println("Success");
            }
            return 0;
        } finally {
            IoUtils.closeQuietly(session);
        }
    }

    private int doAbandonSession(int sessionId, boolean logSuccess) throws RemoteException {
        PrintWriter pw = getOutPrintWriter();
        PackageInstaller.Session session = null;
        try {
            session = new PackageInstaller.Session(this.mInterface.getPackageInstaller().openSession(sessionId));
            session.abandon();
            if (logSuccess) {
                pw.println("Success");
            }
            return 0;
        } finally {
            IoUtils.closeQuietly(session);
        }
    }

    private void doListPermissions(ArrayList<String> groupList, boolean groups, boolean labels, boolean summary, int startProtectionLevel, int endProtectionLevel) throws RemoteException {
        int groupCount;
        List<PermissionInfo> ps;
        String groupName;
        ArrayList<String> arrayList = groupList;
        PrintWriter pw = getOutPrintWriter();
        int groupCount2 = groupList.size();
        int i = 0;
        while (i < groupCount2) {
            String groupName2 = arrayList.get(i);
            String prefix = "";
            if (!groups) {
                groupCount = groupCount2;
            } else {
                if (i > 0) {
                    pw.println("");
                }
                if (groupName2 != null) {
                    PermissionGroupInfo pgi = this.mInterface.getPermissionGroupInfo(groupName2, 0);
                    if (summary) {
                        Resources res = getResources(pgi);
                        if (res != null) {
                            StringBuilder sb = new StringBuilder();
                            groupCount = groupCount2;
                            int groupCount3 = pgi.labelRes;
                            pw.print(sb.append(loadText(pgi, groupCount3, pgi.nonLocalizedLabel)).append(": ").toString());
                        } else {
                            groupCount = groupCount2;
                            pw.print(pgi.name + ": ");
                        }
                    } else {
                        groupCount = groupCount2;
                        pw.println((labels ? "+ " : "") + "group:" + pgi.name);
                        if (labels) {
                            pw.println("  package:" + pgi.packageName);
                            Resources res2 = getResources(pgi);
                            if (res2 != null) {
                                pw.println("  label:" + loadText(pgi, pgi.labelRes, pgi.nonLocalizedLabel));
                                pw.println("  description:" + loadText(pgi, pgi.descriptionRes, pgi.nonLocalizedDescription));
                            }
                        }
                    }
                } else {
                    groupCount = groupCount2;
                    pw.println(((!labels || summary) ? "" : "+ ") + "ungrouped:");
                }
                prefix = "  ";
            }
            List<PermissionInfo> ps2 = this.mPermissionManager.queryPermissionsByGroup(arrayList.get(i), 0);
            int count = ps2 == null ? 0 : ps2.size();
            boolean first = true;
            int p = 0;
            while (p < count) {
                PermissionInfo pi = ps2.get(p);
                if (groups && groupName2 == null && pi.group != null) {
                    ps = ps2;
                    groupName = groupName2;
                } else {
                    int base = pi.protectionLevel & 15;
                    ps = ps2;
                    if (base < startProtectionLevel) {
                        groupName = groupName2;
                    } else if (base > endProtectionLevel) {
                        groupName = groupName2;
                    } else if (summary) {
                        if (first) {
                            first = false;
                        } else {
                            pw.print(", ");
                        }
                        Resources res3 = getResources(pi);
                        if (res3 != null) {
                            pw.print(loadText(pi, pi.labelRes, pi.nonLocalizedLabel));
                        } else {
                            pw.print(pi.name);
                        }
                        groupName = groupName2;
                    } else {
                        pw.println(prefix + (labels ? "+ " : "") + "permission:" + pi.name);
                        if (!labels) {
                            groupName = groupName2;
                        } else {
                            pw.println(prefix + "  package:" + pi.packageName);
                            Resources res4 = getResources(pi);
                            if (res4 != null) {
                                groupName = groupName2;
                                pw.println(prefix + "  label:" + loadText(pi, pi.labelRes, pi.nonLocalizedLabel));
                                pw.println(prefix + "  description:" + loadText(pi, pi.descriptionRes, pi.nonLocalizedDescription));
                            } else {
                                groupName = groupName2;
                            }
                            pw.println(prefix + "  protectionLevel:" + PermissionInfo.protectionToString(pi.protectionLevel));
                        }
                    }
                }
                p++;
                ps2 = ps;
                groupName2 = groupName;
            }
            if (summary) {
                pw.println("");
            }
            i++;
            arrayList = groupList;
            groupCount2 = groupCount;
        }
    }

    private String loadText(PackageItemInfo pii, int res, CharSequence nonLocalized) throws RemoteException {
        Resources r;
        if (nonLocalized != null) {
            return nonLocalized.toString();
        }
        if (res != 0 && (r = getResources(pii)) != null) {
            try {
                return r.getString(res);
            } catch (Resources.NotFoundException e) {
                return null;
            }
        }
        return null;
    }

    private Resources getResources(PackageItemInfo pii) throws RemoteException {
        Resources res = this.mResourceCache.get(pii.packageName);
        if (res != null) {
            return res;
        }
        ApplicationInfo ai = this.mInterface.getApplicationInfo(pii.packageName, 536904192L, 0);
        AssetManager am = new AssetManager();
        am.addAssetPath(ai.publicSourceDir);
        Resources res2 = new Resources(am, null, null);
        this.mResourceCache.put(pii.packageName, res2);
        return res2;
    }

    public void onHelp() {
        PrintWriter pw = getOutPrintWriter();
        pw.println("Package manager (package) commands:");
        pw.println("  help");
        pw.println("    Print this help text.");
        pw.println("");
        pw.println("  path [--user USER_ID] PACKAGE");
        pw.println("    Print the path to the .apk of the given PACKAGE.");
        pw.println("");
        pw.println("  dump PACKAGE");
        pw.println("    Print various system state associated with the given PACKAGE.");
        pw.println("");
        pw.println("  has-feature FEATURE_NAME [version]");
        pw.println("    Prints true and returns exit status 0 when system has a FEATURE_NAME,");
        pw.println("    otherwise prints false and returns exit status 1");
        pw.println("");
        pw.println("  list features");
        pw.println("    Prints all features of the system.");
        pw.println("");
        pw.println("  list instrumentation [-f] [TARGET-PACKAGE]");
        pw.println("    Prints all test packages; optionally only those targeting TARGET-PACKAGE");
        pw.println("    Options:");
        pw.println("      -f: dump the name of the .apk file containing the test package");
        pw.println("");
        pw.println("  list libraries");
        pw.println("    Prints all system libraries.");
        pw.println("");
        pw.println("  list packages [-f] [-d] [-e] [-s] [-3] [-i] [-l] [-u] [-U] ");
        pw.println("      [--show-versioncode] [--apex-only] [--uid UID] [--user USER_ID] [FILTER]");
        pw.println("    Prints all packages; optionally only those whose name contains");
        pw.println("    the text in FILTER.  Options are:");
        pw.println("      -f: see their associated file");
        pw.println("      -a: all known packages (but excluding APEXes)");
        pw.println("      -d: filter to only show disabled packages");
        pw.println("      -e: filter to only show enabled packages");
        pw.println("      -s: filter to only show system packages");
        pw.println("      -3: filter to only show third party packages");
        pw.println("      -i: see the installer for the packages");
        pw.println("      -l: ignored (used for compatibility with older releases)");
        pw.println("      -U: also show the package UID");
        pw.println("      -u: also include uninstalled packages");
        pw.println("      --show-versioncode: also show the version code");
        pw.println("      --apex-only: only show APEX packages");
        pw.println("      --uid UID: filter to only show packages with the given UID");
        pw.println("      --user USER_ID: only list packages belonging to the given user");
        pw.println("");
        pw.println("  list permission-groups");
        pw.println("    Prints all known permission groups.");
        pw.println("");
        pw.println("  list permissions [-g] [-f] [-d] [-u] [GROUP]");
        pw.println("    Prints all known permissions; optionally only those in GROUP.  Options are:");
        pw.println("      -g: organize by group");
        pw.println("      -f: print all information");
        pw.println("      -s: short summary");
        pw.println("      -d: only list dangerous permissions");
        pw.println("      -u: list only the permissions users will see");
        pw.println("");
        pw.println("  list staged-sessions [--only-ready] [--only-sessionid] [--only-parent]");
        pw.println("    Prints all staged sessions.");
        pw.println("      --only-ready: show only staged sessions that are ready");
        pw.println("      --only-sessionid: show only sessionId of each session");
        pw.println("      --only-parent: hide all children sessions");
        pw.println("");
        pw.println("  list users");
        pw.println("    Prints all users.");
        pw.println("");
        pw.println("  resolve-activity [--brief] [--components] [--query-flags FLAGS]");
        pw.println("       [--user USER_ID] INTENT");
        pw.println("    Prints the activity that resolves to the given INTENT.");
        pw.println("");
        pw.println("  query-activities [--brief] [--components] [--query-flags FLAGS]");
        pw.println("       [--user USER_ID] INTENT");
        pw.println("    Prints all activities that can handle the given INTENT.");
        pw.println("");
        pw.println("  query-services [--brief] [--components] [--query-flags FLAGS]");
        pw.println("       [--user USER_ID] INTENT");
        pw.println("    Prints all services that can handle the given INTENT.");
        pw.println("");
        pw.println("  query-receivers [--brief] [--components] [--query-flags FLAGS]");
        pw.println("       [--user USER_ID] INTENT");
        pw.println("    Prints all broadcast receivers that can handle the given INTENT.");
        pw.println("");
        pw.println("  install [-rtfdg] [-i PACKAGE] [--user USER_ID|all|current]");
        pw.println("       [-p INHERIT_PACKAGE] [--install-location 0/1/2]");
        pw.println("       [--install-reason 0/1/2/3/4] [--originating-uri URI]");
        pw.println("       [--referrer URI] [--abi ABI_NAME] [--force-sdk]");
        pw.println("       [--preload] [--instant] [--full] [--dont-kill]");
        pw.println("       [--enable-rollback]");
        pw.println("       [--force-uuid internal|UUID] [--pkg PACKAGE] [-S BYTES]");
        pw.println("       [--apex] [--staged-ready-timeout TIMEOUT]");
        pw.println("       [PATH [SPLIT...]|-]");
        pw.println("    Install an application.  Must provide the apk data to install, either as");
        pw.println("    file path(s) or '-' to read from stdin.  Options are:");
        pw.println("      -R: disallow replacement of existing application");
        pw.println("      -t: allow test packages");
        pw.println("      -i: specify package name of installer owning the app");
        pw.println("      -f: install application on internal flash");
        pw.println("      -d: allow version code downgrade (debuggable packages only)");
        pw.println("      -p: partial application install (new split on top of existing pkg)");
        pw.println("      -g: grant all runtime permissions");
        pw.println("      -S: size in bytes of package, required for stdin");
        pw.println("      --user: install under the given user.");
        pw.println("      --dont-kill: installing a new feature split, don't kill running app");
        pw.println("      --restrict-permissions: don't whitelist restricted permissions at install");
        pw.println("      --originating-uri: set URI where app was downloaded from");
        pw.println("      --referrer: set URI that instigated the install of the app");
        pw.println("      --pkg: specify expected package name of app being installed");
        pw.println("      --abi: override the default ABI of the platform");
        pw.println("      --instant: cause the app to be installed as an ephemeral install app");
        pw.println("      --full: cause the app to be installed as a non-ephemeral full app");
        pw.println("      --install-location: force the install location:");
        pw.println("          0=auto, 1=internal only, 2=prefer external");
        pw.println("      --install-reason: indicates why the app is being installed:");
        pw.println("          0=unknown, 1=admin policy, 2=device restore,");
        pw.println("          3=device setup, 4=user request");
        pw.println("      --force-uuid: force install on to disk volume with given UUID");
        pw.println("      --apex: install an .apex file, not an .apk");
        pw.println("      --staged-ready-timeout: By default, staged sessions wait 60000");
        pw.println("          milliseconds for pre-reboot verification to complete when");
        pw.println("          performing staged install. This flag is used to alter the waiting");
        pw.println("          time. You can skip the waiting time by specifying a TIMEOUT of '0'");
        pw.println("");
        pw.println("  install-existing [--user USER_ID|all|current]");
        pw.println("       [--instant] [--full] [--wait] [--restrict-permissions] PACKAGE");
        pw.println("    Installs an existing application for a new user.  Options are:");
        pw.println("      --user: install for the given user.");
        pw.println("      --instant: install as an instant app");
        pw.println("      --full: install as a full app");
        pw.println("      --wait: wait until the package is installed");
        pw.println("      --restrict-permissions: don't whitelist restricted permissions");
        pw.println("");
        pw.println("  install-create [-lrtsfdg] [-i PACKAGE] [--user USER_ID|all|current]");
        pw.println("       [-p INHERIT_PACKAGE] [--install-location 0/1/2]");
        pw.println("       [--install-reason 0/1/2/3/4] [--originating-uri URI]");
        pw.println("       [--referrer URI] [--abi ABI_NAME] [--force-sdk]");
        pw.println("       [--preload] [--instant] [--full] [--dont-kill]");
        pw.println("       [--force-uuid internal|UUID] [--pkg PACKAGE] [--apex] [-S BYTES]");
        pw.println("       [--multi-package] [--staged]");
        pw.println("    Like \"install\", but starts an install session.  Use \"install-write\"");
        pw.println("    to push data into the session, and \"install-commit\" to finish.");
        pw.println("");
        pw.println("  install-write [-S BYTES] SESSION_ID SPLIT_NAME [PATH|-]");
        pw.println("    Write an apk into the given install session.  If the path is '-', data");
        pw.println("    will be read from stdin.  Options are:");
        pw.println("      -S: size in bytes of package, required for stdin");
        pw.println("");
        pw.println("  install-remove SESSION_ID SPLIT...");
        pw.println("    Mark SPLIT(s) as removed in the given install session.");
        pw.println("");
        pw.println("  install-add-session MULTI_PACKAGE_SESSION_ID CHILD_SESSION_IDs");
        pw.println("    Add one or more session IDs to a multi-package session.");
        pw.println("");
        pw.println("  install-commit SESSION_ID");
        pw.println("    Commit the given active install session, installing the app.");
        pw.println("");
        pw.println("  install-abandon SESSION_ID");
        pw.println("    Delete the given active install session.");
        pw.println("");
        pw.println("  set-install-location LOCATION");
        pw.println("    Changes the default install location.  NOTE this is only intended for debugging;");
        pw.println("    using this can cause applications to break and other undersireable behavior.");
        pw.println("    LOCATION is one of:");
        pw.println("    0 [auto]: Let system decide the best location");
        pw.println("    1 [internal]: Install on internal device storage");
        pw.println("    2 [external]: Install on external media");
        pw.println("");
        pw.println("  get-install-location");
        pw.println("    Returns the current install location: 0, 1 or 2 as per set-install-location.");
        pw.println("");
        pw.println("  move-package PACKAGE [internal|UUID]");
        pw.println("");
        pw.println("  move-primary-storage [internal|UUID]");
        pw.println("");
        pw.println("  uninstall [-k] [--user USER_ID] [--versionCode VERSION_CODE]");
        pw.println("       PACKAGE [SPLIT...]");
        pw.println("    Remove the given package name from the system.  May remove an entire app");
        pw.println("    if no SPLIT names specified, otherwise will remove only the splits of the");
        pw.println("    given app.  Options are:");
        pw.println("      -k: keep the data and cache directories around after package removal.");
        pw.println("      --user: remove the app from the given user.");
        pw.println("      --versionCode: only uninstall if the app has the given version code.");
        pw.println("");
        pw.println("  clear [--user USER_ID] [--cache-only] PACKAGE");
        pw.println("    Deletes data associated with a package. Options are:");
        pw.println("    --user: specifies the user for which we need to clear data");
        pw.println("    --cache-only: a flag which tells if we only need to clear cache data");
        pw.println("");
        pw.println("  enable [--user USER_ID] PACKAGE_OR_COMPONENT");
        pw.println("  disable [--user USER_ID] PACKAGE_OR_COMPONENT");
        pw.println("  disable-user [--user USER_ID] PACKAGE_OR_COMPONENT");
        pw.println("  disable-until-used [--user USER_ID] PACKAGE_OR_COMPONENT");
        pw.println("  default-state [--user USER_ID] PACKAGE_OR_COMPONENT");
        pw.println("    These commands change the enabled state of a given package or");
        pw.println("    component (written as \"package/class\").");
        pw.println("");
        pw.println("  hide [--user USER_ID] PACKAGE_OR_COMPONENT");
        pw.println("  unhide [--user USER_ID] PACKAGE_OR_COMPONENT");
        pw.println("");
        pw.println("  suspend [--user USER_ID] PACKAGE [PACKAGE...]");
        pw.println("    Suspends the specified package(s) (as user).");
        pw.println("");
        pw.println("  unsuspend [--user USER_ID] PACKAGE [PACKAGE...]");
        pw.println("    Unsuspends the specified package(s) (as user).");
        pw.println("");
        pw.println("  set-distracting-restriction [--user USER_ID] [--flag FLAG ...]");
        pw.println("      PACKAGE [PACKAGE...]");
        pw.println("    Sets the specified restriction flags to given package(s) (for user).");
        pw.println("    Flags are:");
        pw.println("      hide-notifications: Hides notifications from this package");
        pw.println("      hide-from-suggestions: Hides this package from suggestions");
        pw.println("        (by the launcher, etc.)");
        pw.println("    Any existing flags are overwritten, which also means that if no flags are");
        pw.println("    specified then all existing flags will be cleared.");
        pw.println("");
        pw.println("  grant [--user USER_ID] PACKAGE PERMISSION");
        pw.println("  revoke [--user USER_ID] PACKAGE PERMISSION");
        pw.println("    These commands either grant or revoke permissions to apps.  The permissions");
        pw.println("    must be declared as used in the app's manifest, be runtime permissions");
        pw.println("    (protection level dangerous), and the app targeting SDK greater than Lollipop MR1.");
        pw.println("");
        pw.println("  set-permission-flags [--user USER_ID] PACKAGE PERMISSION [FLAGS..]");
        pw.println("  clear-permission-flags [--user USER_ID] PACKAGE PERMISSION [FLAGS..]");
        pw.println("    These commands either set or clear permission flags on apps.  The permissions");
        pw.println("    must be declared as used in the app's manifest, be runtime permissions");
        pw.println("    (protection level dangerous), and the app targeting SDK greater than Lollipop MR1.");
        pw.println("    The flags must be one or more of " + SUPPORTED_PERMISSION_FLAGS_LIST);
        pw.println("");
        pw.println("  reset-permissions");
        pw.println("    Revert all runtime permissions to their default state.");
        pw.println("");
        pw.println("  set-permission-enforced PERMISSION [true|false]");
        pw.println("");
        pw.println("  get-privapp-permissions TARGET-PACKAGE");
        pw.println("    Prints all privileged permissions for a package.");
        pw.println("");
        pw.println("  get-privapp-deny-permissions TARGET-PACKAGE");
        pw.println("    Prints all privileged permissions that are denied for a package.");
        pw.println("");
        pw.println("  get-oem-permissions TARGET-PACKAGE");
        pw.println("    Prints all OEM permissions for a package.");
        pw.println("");
        pw.println("  trim-caches DESIRED_FREE_SPACE [internal|UUID]");
        pw.println("    Trim cache files to reach the given free space.");
        pw.println("");
        pw.println("  list users");
        pw.println("    Lists the current users.");
        pw.println("");
        pw.println("  create-user [--profileOf USER_ID] [--managed] [--restricted] [--ephemeral]");
        pw.println("      [--guest] [--pre-create-only] [--user-type USER_TYPE] USER_NAME");
        pw.println("    Create a new user with the given USER_NAME, printing the new user identifier");
        pw.println("    of the user.");
        pw.println("    USER_TYPE is the name of a user type, e.g. android.os.usertype.profile.MANAGED.");
        pw.println("      If not specified, the default user type is android.os.usertype.full.SECONDARY.");
        pw.println("      --managed is shorthand for '--user-type android.os.usertype.profile.MANAGED'.");
        pw.println("      --restricted is shorthand for '--user-type android.os.usertype.full.RESTRICTED'.");
        pw.println("      --guest is shorthand for '--user-type android.os.usertype.full.GUEST'.");
        pw.println("");
        pw.println("  remove-user [--set-ephemeral-if-in-use | --wait] USER_ID");
        pw.println("    Remove the user with the given USER_IDENTIFIER, deleting all data");
        pw.println("    associated with that user.");
        pw.println("      --set-ephemeral-if-in-use: If the user is currently running and");
        pw.println("        therefore cannot be removed immediately, mark the user as ephemeral");
        pw.println("        so that it will be automatically removed when possible (after user");
        pw.println("        switch or reboot)");
        pw.println("      --wait: Wait until user is removed. Ignored if set-ephemeral-if-in-use");
        pw.println("");
        pw.println("  set-user-restriction [--user USER_ID] RESTRICTION VALUE");
        pw.println("");
        pw.println("  get-max-users");
        pw.println("");
        pw.println("  get-max-running-users");
        pw.println("");
        pw.println("  compile [-m MODE | -r REASON] [-f] [-c] [--split SPLIT_NAME]");
        pw.println("          [--reset] [--check-prof (true | false)] (-a | TARGET-PACKAGE)");
        pw.println("    Trigger compilation of TARGET-PACKAGE or all packages if \"-a\".  Options are:");
        pw.println("      -a: compile all packages");
        pw.println("      -c: clear profile data before compiling");
        pw.println("      -f: force compilation even if not needed");
        pw.println("      -m: select compilation mode");
        pw.println("          MODE is one of the dex2oat compiler filters:");
        pw.println("            assume-verified");
        pw.println("            extract");
        pw.println("            verify");
        pw.println("            quicken");
        pw.println("            space-profile");
        pw.println("            space");
        pw.println("            speed-profile");
        pw.println("            speed");
        pw.println("            everything");
        pw.println("      -r: select compilation reason");
        pw.println("          REASON is one of:");
        for (int i = 0; i < PackageManagerServiceCompilerMapping.REASON_STRINGS.length; i++) {
            pw.println("            " + PackageManagerServiceCompilerMapping.REASON_STRINGS[i]);
        }
        pw.println("      --reset: restore package to its post-install state");
        pw.println("      --check-prof (true | false): look at profiles when doing dexopt?");
        pw.println("      --secondary-dex: compile app secondary dex files");
        pw.println("      --split SPLIT: compile only the given split name");
        pw.println("      --compile-layouts: compile layout resources for faster inflation");
        pw.println("");
        pw.println("  force-dex-opt PACKAGE");
        pw.println("    Force immediate execution of dex opt for the given PACKAGE.");
        pw.println("");
        pw.println("  delete-dexopt PACKAGE");
        pw.println("    Delete dex optimization results for the given PACKAGE.");
        pw.println("");
        pw.println("  bg-dexopt-job");
        pw.println("    Execute the background optimizations immediately.");
        pw.println("    Note that the command only runs the background optimizer logic. It may");
        pw.println("    overlap with the actual job but the job scheduler will not be able to");
        pw.println("    cancel it. It will also run even if the device is not in the idle");
        pw.println("    maintenance mode.");
        pw.println("  cancel-bg-dexopt-job");
        pw.println("    Cancels currently running background optimizations immediately.");
        pw.println("    This cancels optimizations run from bg-dexopt-job or from JobScjeduler.");
        pw.println("    Note that cancelling currently running bg-dexopt-job command requires");
        pw.println("    running this command from separate adb shell.");
        pw.println("");
        pw.println("  reconcile-secondary-dex-files TARGET-PACKAGE");
        pw.println("    Reconciles the package secondary dex files with the generated oat files.");
        pw.println("");
        pw.println("  dump-profiles [--dump-classes-and-methods] TARGET-PACKAGE");
        pw.println("    Dumps method/class profile files to");
        pw.println("    /data/misc/profman/TARGET-PACKAGE-primary.prof.txt.");
        pw.println("      --dump-classes-and-methods: passed along to the profman binary to");
        pw.println("        switch to the format used by 'profman --create-profile-from'.");
        pw.println("");
        pw.println("  snapshot-profile TARGET-PACKAGE [--code-path path]");
        pw.println("    Take a snapshot of the package profiles to");
        pw.println("    /data/misc/profman/TARGET-PACKAGE[-code-path].prof");
        pw.println("    If TARGET-PACKAGE=android it will take a snapshot of the boot image");
        pw.println("");
        pw.println("  set-home-activity [--user USER_ID] TARGET-COMPONENT");
        pw.println("    Set the default home activity (aka launcher).");
        pw.println("    TARGET-COMPONENT can be a package name (com.package.my) or a full");
        pw.println("    component (com.package.my/component.name). However, only the package name");
        pw.println("    matters: the actual component used will be determined automatically from");
        pw.println("    the package.");
        pw.println("");
        pw.println("  set-installer PACKAGE INSTALLER");
        pw.println("    Set installer package name");
        pw.println("");
        pw.println("  get-instantapp-resolver");
        pw.println("    Return the name of the component that is the current instant app installer.");
        pw.println("");
        pw.println("  set-harmful-app-warning [--user <USER_ID>] <PACKAGE> [<WARNING>]");
        pw.println("    Mark the app as harmful with the given warning message.");
        pw.println("");
        pw.println("  get-harmful-app-warning [--user <USER_ID>] <PACKAGE>");
        pw.println("    Return the harmful app warning message for the given app, if present");
        pw.println();
        pw.println("  uninstall-system-updates [<PACKAGE>]");
        pw.println("    Removes updates to the given system application and falls back to its");
        pw.println("    /system version. Does nothing if the given package is not a system app.");
        pw.println("    If no package is specified, removes updates to all system applications.");
        pw.println("");
        pw.println("  get-moduleinfo [--all | --installed] [module-name]");
        pw.println("    Displays module info. If module-name is specified only that info is shown");
        pw.println("    By default, without any argument only installed modules are shown.");
        pw.println("      --all: show all module info");
        pw.println("      --installed: show only installed modules");
        pw.println("");
        pw.println("  log-visibility [--enable|--disable] <PACKAGE>");
        pw.println("    Turns on debug logging when visibility is blocked for the given package.");
        pw.println("      --enable: turn on debug logging (default)");
        pw.println("      --disable: turn off debug logging");
        pw.println("");
        pw.println("  set-silent-updates-policy [--allow-unlimited-silent-updates <INSTALLER>]");
        pw.println("                            [--throttle-time <SECONDS>] [--reset]");
        pw.println("    Sets the policies of the silent updates.");
        pw.println("      --allow-unlimited-silent-updates: allows unlimited silent updated");
        pw.println("        installation requests from the installer without the throttle time.");
        pw.println("      --throttle-time: update the silent updates throttle time in seconds.");
        pw.println("      --reset: restore the installer and throttle time to the default, and");
        pw.println("        clear tracks of silent updates in the system.");
        pw.println("");
        this.mDomainVerificationShell.printHelp(pw);
        pw.println("");
        Intent.printIntentArgsHelp(pw, "");
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class LocalIntentReceiver {
        private IIntentSender.Stub mLocalSender;
        private final LinkedBlockingQueue<Intent> mResult;

        private LocalIntentReceiver() {
            this.mResult = new LinkedBlockingQueue<>();
            this.mLocalSender = new IIntentSender.Stub() { // from class: com.android.server.pm.PackageManagerShellCommand.LocalIntentReceiver.1
                public void send(int code, Intent intent, String resolvedType, IBinder whitelistToken, IIntentReceiver finishedReceiver, String requiredPermission, Bundle options) {
                    try {
                        LocalIntentReceiver.this.mResult.offer(intent, 5L, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            };
        }

        public IntentSender getIntentSender() {
            return new IntentSender(this.mLocalSender);
        }

        public Intent getResult() {
            try {
                return this.mResult.take();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
