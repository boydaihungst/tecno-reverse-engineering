package com.android.server.am;

import android.app.IApplicationThread;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.hardware.audio.common.V2_0.AudioFormat;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerExemptionManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.Slog;
import android.util.TimeUtils;
import android.util.proto.ProtoOutputStream;
import android.util.proto.ProtoUtils;
import com.android.internal.app.procstats.ServiceState;
import com.android.server.LocalServices;
import com.android.server.am.ActivityManagerService;
import com.android.server.notification.NotificationManagerInternal;
import com.android.server.uri.NeededUriGrants;
import com.android.server.uri.UriPermissionOwner;
import com.transsion.hubcore.griffin.lib.app.TranAppInfo;
import com.transsion.hubcore.server.am.ITranServiceRecord;
import defpackage.CompanionAppsPermissions;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
/* loaded from: classes.dex */
public final class ServiceRecord extends Binder implements ComponentName.WithComponentName {
    static final int MAX_DELIVERY_COUNT = 3;
    static final int MAX_DONE_EXECUTING_COUNT = 6;
    private static final String TAG = "ActivityManager";
    boolean allowlistManager;
    final ActivityManagerService ams;
    ProcessRecord app;
    ApplicationInfo appInfo;
    final ArrayMap<Intent.FilterComparison, IntentBindRecord> bindings;
    boolean callStart;
    private final ArrayMap<IBinder, ArrayList<ConnectionRecord>> connections;
    int crashCount;
    final long createRealTime;
    boolean createdFromFg;
    final String definingPackageName;
    final int definingUid;
    boolean delayed;
    boolean delayedStop;
    final ArrayList<StartItem> deliveredStarts;
    long destroyTime;
    boolean destroying;
    boolean executeFg;
    int executeNesting;
    long executingStart;
    final boolean exported;
    long fgDisplayTime;
    boolean fgRequired;
    boolean fgWaiting;
    int foregroundId;
    Notification foregroundNoti;
    int foregroundServiceType;
    public final TranAppInfo grifAppInfo;
    final ComponentName instanceName;
    final Intent.FilterComparison intent;
    boolean isForeground;
    boolean isNotAppComponentUsage;
    final boolean isSdkSandbox;
    ProcessRecord isolationHostProc;
    long lastActivity;
    private int lastStartId;
    long lastTopAlmostPerceptibleBindRequestUptimeMs;
    int mAllowStartForeground;
    int mAllowStartForegroundAtEntering;
    boolean mAllowWhileInUsePermissionInFgs;
    boolean mAllowWhileInUsePermissionInFgsAtEntering;
    private ProcessRecord mAppForAllowingBgActivityStartsByStart;
    private List<IBinder> mBgActivityStartsByStartOriginatingTokens;
    private Runnable mCleanUpAllowBgActivityStartsByStartCallback;
    long mEarliestRestartTime;
    long mFgsEnterTime;
    long mFgsExitTime;
    boolean mFgsHasNotificationPermission;
    boolean mFgsNotificationDeferred;
    boolean mFgsNotificationShown;
    boolean mFgsNotificationWasDeferred;
    String mInfoAllowStartForeground;
    ActivityManagerService.FgsTempAllowListItem mInfoTempFgsAllowListReason;
    private boolean mIsAllowedBgActivityStartsByBinding;
    private boolean mIsAllowedBgActivityStartsByStart;
    boolean mKeepWarming;
    long mLastSetFgsRestrictionTime;
    boolean mLoggedInfoAllowStartForeground;
    ApplicationInfo mRecentCallerApplicationInfo;
    String mRecentCallingPackage;
    int mRecentCallingUid;
    long mRestartSchedulingTime;
    int mStartForegroundCount;
    final ComponentName name;
    long nextRestartTime;
    final String packageName;
    int pendingConnectionGroup;
    int pendingConnectionImportance;
    final ArrayList<StartItem> pendingStarts;
    final String permission;
    final String processName;
    int restartCount;
    long restartDelay;
    long restartTime;
    ServiceState restartTracker;
    final Runnable restarter;
    final String sdkSandboxClientAppPackage;
    final int sdkSandboxClientAppUid;
    final ServiceInfo serviceInfo;
    final String shortInstanceName;
    boolean startRequested;
    long startingBgTimeout;
    boolean stopIfKilled;
    String stringName;
    int totalRestartCount;
    ServiceState tracker;
    final int userId;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class StartItem {
        final int callingId;
        long deliveredTime;
        int deliveryCount;
        int doneExecutingCount;
        final int id;
        final Intent intent;
        final NeededUriGrants neededGrants;
        final ServiceRecord sr;
        String stringName;
        final boolean taskRemoved;
        UriPermissionOwner uriPermissions;

        /* JADX INFO: Access modifiers changed from: package-private */
        public StartItem(ServiceRecord _sr, boolean _taskRemoved, int _id, Intent _intent, NeededUriGrants _neededGrants, int _callingId) {
            this.sr = _sr;
            this.taskRemoved = _taskRemoved;
            this.id = _id;
            this.intent = _intent;
            this.neededGrants = _neededGrants;
            this.callingId = _callingId;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public UriPermissionOwner getUriPermissionsLocked() {
            if (this.uriPermissions == null) {
                this.uriPermissions = new UriPermissionOwner(this.sr.ams.mUgmInternal, this);
            }
            return this.uriPermissions;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public void removeUriPermissionsLocked() {
            UriPermissionOwner uriPermissionOwner = this.uriPermissions;
            if (uriPermissionOwner != null) {
                uriPermissionOwner.removeUriPermissions();
                this.uriPermissions = null;
            }
        }

        public void dumpDebug(ProtoOutputStream proto, long fieldId, long now) {
            long token = proto.start(fieldId);
            proto.write(CompanionMessage.MESSAGE_ID, this.id);
            ProtoUtils.toDuration(proto, 1146756268034L, this.deliveredTime, now);
            proto.write(1120986464259L, this.deliveryCount);
            proto.write(1120986464260L, this.doneExecutingCount);
            Intent intent = this.intent;
            if (intent != null) {
                intent.dumpDebug(proto, 1146756268037L, true, true, true, false);
            }
            NeededUriGrants neededUriGrants = this.neededGrants;
            if (neededUriGrants != null) {
                neededUriGrants.dumpDebug(proto, 1146756268038L);
            }
            UriPermissionOwner uriPermissionOwner = this.uriPermissions;
            if (uriPermissionOwner != null) {
                uriPermissionOwner.dumpDebug(proto, 1146756268039L);
            }
            proto.end(token);
        }

        public String toString() {
            String str = this.stringName;
            if (str != null) {
                return str;
            }
            StringBuilder sb = new StringBuilder(128);
            sb.append("ServiceRecord{").append(Integer.toHexString(System.identityHashCode(this.sr))).append(' ').append(this.sr.shortInstanceName).append(" StartItem ").append(Integer.toHexString(System.identityHashCode(this))).append(" id=").append(this.id).append('}');
            String sb2 = sb.toString();
            this.stringName = sb2;
            return sb2;
        }
    }

    void dumpStartList(PrintWriter pw, String prefix, List<StartItem> list, long now) {
        int N = list.size();
        for (int i = 0; i < N; i++) {
            StartItem si = list.get(i);
            pw.print(prefix);
            pw.print("#");
            pw.print(i);
            pw.print(" id=");
            pw.print(si.id);
            if (now != 0) {
                pw.print(" dur=");
                TimeUtils.formatDuration(si.deliveredTime, now, pw);
            }
            if (si.deliveryCount != 0) {
                pw.print(" dc=");
                pw.print(si.deliveryCount);
            }
            if (si.doneExecutingCount != 0) {
                pw.print(" dxc=");
                pw.print(si.doneExecutingCount);
            }
            pw.println("");
            pw.print(prefix);
            pw.print("  intent=");
            if (si.intent != null) {
                pw.println(si.intent.toString());
            } else {
                pw.println("null");
            }
            if (si.neededGrants != null) {
                pw.print(prefix);
                pw.print("  neededGrants=");
                pw.println(si.neededGrants);
            }
            if (si.uriPermissions != null) {
                si.uriPermissions.dump(pw, prefix);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Code restructure failed: missing block: B:51:0x01d2, code lost:
        if (r1 == 0) goto L47;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void dumpDebug(ProtoOutputStream proto, long fieldId) {
        long token = proto.start(fieldId);
        proto.write(CompanionAppsPermissions.AppPermissions.PACKAGE_NAME, this.shortInstanceName);
        proto.write(1133871366146L, this.app != null);
        ProcessRecord processRecord = this.app;
        if (processRecord != null) {
            proto.write(1120986464259L, processRecord.getPid());
        }
        Intent.FilterComparison filterComparison = this.intent;
        if (filterComparison != null) {
            filterComparison.getIntent().dumpDebug(proto, 1146756268036L, false, true, false, false);
        }
        proto.write(1138166333445L, this.packageName);
        proto.write(1138166333446L, this.processName);
        proto.write(1138166333447L, this.permission);
        long now = SystemClock.uptimeMillis();
        long nowReal = SystemClock.elapsedRealtime();
        if (this.appInfo != null) {
            long appInfoToken = proto.start(1146756268040L);
            proto.write(CompanionAppsPermissions.AppPermissions.PACKAGE_NAME, this.appInfo.sourceDir);
            if (!Objects.equals(this.appInfo.sourceDir, this.appInfo.publicSourceDir)) {
                proto.write(1138166333442L, this.appInfo.publicSourceDir);
            }
            proto.write(1138166333443L, this.appInfo.dataDir);
            proto.end(appInfoToken);
        }
        ProcessRecord processRecord2 = this.app;
        if (processRecord2 != null) {
            processRecord2.dumpDebug(proto, 1146756268041L);
        }
        ProcessRecord processRecord3 = this.isolationHostProc;
        if (processRecord3 != null) {
            processRecord3.dumpDebug(proto, 1146756268042L);
        }
        proto.write(1133871366155L, this.allowlistManager);
        proto.write(1133871366156L, this.delayed);
        if (this.isForeground || this.foregroundId != 0) {
            long fgToken = proto.start(1146756268045L);
            proto.write(CompanionMessage.MESSAGE_ID, this.foregroundId);
            this.foregroundNoti.dumpDebug(proto, 1146756268034L);
            proto.end(fgToken);
        }
        ProtoUtils.toDuration(proto, 1146756268046L, this.createRealTime, nowReal);
        ProtoUtils.toDuration(proto, 1146756268047L, this.startingBgTimeout, now);
        ProtoUtils.toDuration(proto, 1146756268048L, this.lastActivity, now);
        ProtoUtils.toDuration(proto, 1146756268049L, this.restartTime, now);
        proto.write(1133871366162L, this.createdFromFg);
        proto.write(1133871366171L, this.mAllowWhileInUsePermissionInFgs);
        if (this.startRequested || this.delayedStop || this.lastStartId != 0) {
            long startToken = proto.start(1146756268051L);
            proto.write(1133871366145L, this.startRequested);
            proto.write(1133871366146L, this.delayedStop);
            proto.write(1133871366147L, this.stopIfKilled);
            proto.write(1120986464261L, this.lastStartId);
            proto.end(startToken);
        }
        if (this.executeNesting != 0) {
            long executNestingToken = proto.start(1146756268052L);
            proto.write(CompanionMessage.MESSAGE_ID, this.executeNesting);
            proto.write(1133871366146L, this.executeFg);
            ProtoUtils.toDuration(proto, 1146756268035L, this.executingStart, now);
            proto.end(executNestingToken);
        }
        if (this.destroying || this.destroyTime != 0) {
            ProtoUtils.toDuration(proto, 1146756268053L, this.destroyTime, now);
        }
        if (this.crashCount == 0 && this.restartCount == 0) {
            long j = this.nextRestartTime;
            if (j - this.mRestartSchedulingTime == 0) {
            }
        }
        long crashToken = proto.start(1146756268054L);
        proto.write(CompanionMessage.MESSAGE_ID, this.restartCount);
        ProtoUtils.toDuration(proto, 1146756268034L, this.nextRestartTime - this.mRestartSchedulingTime, now);
        ProtoUtils.toDuration(proto, 1146756268035L, this.nextRestartTime, now);
        proto.write(1120986464260L, this.crashCount);
        proto.end(crashToken);
        if (this.deliveredStarts.size() > 0) {
            int N = this.deliveredStarts.size();
            for (int i = 0; i < N; i++) {
                this.deliveredStarts.get(i).dumpDebug(proto, 2246267895831L, now);
            }
        }
        if (this.pendingStarts.size() > 0) {
            int N2 = this.pendingStarts.size();
            for (int i2 = 0; i2 < N2; i2++) {
                this.pendingStarts.get(i2).dumpDebug(proto, 2246267895832L, now);
            }
        }
        if (this.bindings.size() > 0) {
            int N3 = this.bindings.size();
            for (int i3 = 0; i3 < N3; i3++) {
                IntentBindRecord b = this.bindings.valueAt(i3);
                b.dumpDebug(proto, 2246267895833L);
            }
        }
        if (this.connections.size() > 0) {
            int N4 = this.connections.size();
            for (int conni = 0; conni < N4; conni++) {
                ArrayList<ConnectionRecord> c = this.connections.valueAt(conni);
                for (int i4 = 0; i4 < c.size(); i4++) {
                    c.get(i4).dumpDebug(proto, 2246267895834L);
                }
            }
        }
        proto.end(token);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Code restructure failed: missing block: B:56:0x0276, code lost:
        if (r3 == 0) goto L53;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void dump(PrintWriter pw, String prefix) {
        pw.print(prefix);
        pw.print("intent={");
        pw.print(this.intent.getIntent().toShortString(false, true, false, false));
        pw.println('}');
        pw.print(prefix);
        pw.print("packageName=");
        pw.println(this.packageName);
        pw.print(prefix);
        pw.print("processName=");
        pw.println(this.processName);
        if (this.permission != null) {
            pw.print(prefix);
            pw.print("permission=");
            pw.println(this.permission);
        }
        long now = SystemClock.uptimeMillis();
        long nowReal = SystemClock.elapsedRealtime();
        if (this.appInfo != null) {
            pw.print(prefix);
            pw.print("baseDir=");
            pw.println(this.appInfo.sourceDir);
            if (!Objects.equals(this.appInfo.sourceDir, this.appInfo.publicSourceDir)) {
                pw.print(prefix);
                pw.print("resDir=");
                pw.println(this.appInfo.publicSourceDir);
            }
            pw.print(prefix);
            pw.print("dataDir=");
            pw.println(this.appInfo.dataDir);
        }
        pw.print(prefix);
        pw.print("app=");
        pw.println(this.app);
        if (this.isolationHostProc != null) {
            pw.print(prefix);
            pw.print("isolationHostProc=");
            pw.println(this.isolationHostProc);
        }
        if (this.allowlistManager) {
            pw.print(prefix);
            pw.print("allowlistManager=");
            pw.println(this.allowlistManager);
        }
        if (this.mIsAllowedBgActivityStartsByBinding) {
            pw.print(prefix);
            pw.print("mIsAllowedBgActivityStartsByBinding=");
            pw.println(this.mIsAllowedBgActivityStartsByBinding);
        }
        if (this.mIsAllowedBgActivityStartsByStart) {
            pw.print(prefix);
            pw.print("mIsAllowedBgActivityStartsByStart=");
            pw.println(this.mIsAllowedBgActivityStartsByStart);
        }
        pw.print(prefix);
        pw.print("allowWhileInUsePermissionInFgs=");
        pw.println(this.mAllowWhileInUsePermissionInFgs);
        pw.print(prefix);
        pw.print("recentCallingPackage=");
        pw.println(this.mRecentCallingPackage);
        pw.print(prefix);
        pw.print("recentCallingUid=");
        pw.println(this.mRecentCallingUid);
        pw.print(prefix);
        pw.print("allowStartForeground=");
        pw.println(PowerExemptionManager.reasonCodeToString(this.mAllowStartForeground));
        pw.print(prefix);
        pw.print("startForegroundCount=");
        pw.println(this.mStartForegroundCount);
        pw.print(prefix);
        pw.print("infoAllowStartForeground=");
        pw.println(this.mInfoAllowStartForeground);
        if (this.delayed) {
            pw.print(prefix);
            pw.print("delayed=");
            pw.println(this.delayed);
        }
        if (this.isForeground || this.foregroundId != 0) {
            pw.print(prefix);
            pw.print("isForeground=");
            pw.print(this.isForeground);
            pw.print(" foregroundId=");
            pw.print(this.foregroundId);
            pw.print(" foregroundNoti=");
            pw.println(this.foregroundNoti);
        }
        pw.print(prefix);
        pw.print("createTime=");
        TimeUtils.formatDuration(this.createRealTime, nowReal, pw);
        pw.print(" startingBgTimeout=");
        TimeUtils.formatDuration(this.startingBgTimeout, now, pw);
        pw.println();
        pw.print(prefix);
        pw.print("lastActivity=");
        TimeUtils.formatDuration(this.lastActivity, now, pw);
        pw.print(" restartTime=");
        TimeUtils.formatDuration(this.restartTime, now, pw);
        pw.print(" createdFromFg=");
        pw.println(this.createdFromFg);
        if (this.pendingConnectionGroup != 0) {
            pw.print(prefix);
            pw.print(" pendingConnectionGroup=");
            pw.print(this.pendingConnectionGroup);
            pw.print(" Importance=");
            pw.println(this.pendingConnectionImportance);
        }
        if (this.startRequested || this.delayedStop || this.lastStartId != 0) {
            pw.print(prefix);
            pw.print("startRequested=");
            pw.print(this.startRequested);
            pw.print(" delayedStop=");
            pw.print(this.delayedStop);
            pw.print(" stopIfKilled=");
            pw.print(this.stopIfKilled);
            pw.print(" callStart=");
            pw.print(this.callStart);
            pw.print(" lastStartId=");
            pw.println(this.lastStartId);
        }
        if (this.executeNesting != 0) {
            pw.print(prefix);
            pw.print("executeNesting=");
            pw.print(this.executeNesting);
            pw.print(" executeFg=");
            pw.print(this.executeFg);
            pw.print(" executingStart=");
            TimeUtils.formatDuration(this.executingStart, now, pw);
            pw.println();
        }
        if (this.destroying || this.destroyTime != 0) {
            pw.print(prefix);
            pw.print("destroying=");
            pw.print(this.destroying);
            pw.print(" destroyTime=");
            TimeUtils.formatDuration(this.destroyTime, now, pw);
            pw.println();
        }
        if (this.crashCount == 0 && this.restartCount == 0) {
            long j = this.nextRestartTime;
            if (j - this.mRestartSchedulingTime == 0) {
            }
        }
        pw.print(prefix);
        pw.print("restartCount=");
        pw.print(this.restartCount);
        pw.print(" restartDelay=");
        TimeUtils.formatDuration(this.nextRestartTime - this.mRestartSchedulingTime, now, pw);
        pw.print(" nextRestartTime=");
        TimeUtils.formatDuration(this.nextRestartTime, now, pw);
        pw.print(" crashCount=");
        pw.println(this.crashCount);
        if (this.deliveredStarts.size() > 0) {
            pw.print(prefix);
            pw.println("Delivered Starts:");
            dumpStartList(pw, prefix, this.deliveredStarts, now);
        }
        if (this.pendingStarts.size() > 0) {
            pw.print(prefix);
            pw.println("Pending Starts:");
            dumpStartList(pw, prefix, this.pendingStarts, 0L);
        }
        if (this.bindings.size() > 0) {
            pw.print(prefix);
            pw.println("Bindings:");
            for (int i = 0; i < this.bindings.size(); i++) {
                IntentBindRecord b = this.bindings.valueAt(i);
                pw.print(prefix);
                pw.print("* IntentBindRecord{");
                pw.print(Integer.toHexString(System.identityHashCode(b)));
                if ((b.collectFlags() & 1) != 0) {
                    pw.append(" CREATE");
                }
                pw.println("}:");
                b.dumpInService(pw, prefix + "  ");
            }
        }
        if (this.connections.size() > 0) {
            pw.print(prefix);
            pw.println("All Connections:");
            for (int conni = 0; conni < this.connections.size(); conni++) {
                ArrayList<ConnectionRecord> c = this.connections.valueAt(conni);
                for (int i2 = 0; i2 < c.size(); i2++) {
                    pw.print(prefix);
                    pw.print("  ");
                    pw.println(c.get(i2));
                }
            }
        }
    }

    ServiceRecord(ActivityManagerService ams, ComponentName name, ComponentName instanceName, String definingPackageName, int definingUid, Intent.FilterComparison intent, ServiceInfo sInfo, boolean callerIsFg, Runnable restarter) {
        this(ams, name, instanceName, definingPackageName, definingUid, intent, sInfo, callerIsFg, restarter, null, 0, null);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ServiceRecord(ActivityManagerService ams, ComponentName name, ComponentName instanceName, String definingPackageName, int definingUid, Intent.FilterComparison intent, ServiceInfo sInfo, boolean callerIsFg, Runnable restarter, String sdkSandboxProcessName, int sdkSandboxClientAppUid, String sdkSandboxClientAppPackage) {
        this.bindings = new ArrayMap<>();
        this.connections = new ArrayMap<>();
        this.mBgActivityStartsByStartOriginatingTokens = new ArrayList();
        this.mFgsEnterTime = 0L;
        this.mFgsExitTime = 0L;
        this.mAllowStartForeground = -1;
        this.mAllowStartForegroundAtEntering = -1;
        this.deliveredStarts = new ArrayList<>();
        this.pendingStarts = new ArrayList<>();
        this.ams = ams;
        this.name = name;
        this.instanceName = instanceName;
        this.shortInstanceName = instanceName.flattenToShortString();
        this.definingPackageName = definingPackageName;
        this.definingUid = definingUid;
        this.intent = intent;
        this.serviceInfo = sInfo;
        this.appInfo = sInfo.applicationInfo;
        String str = sInfo.applicationInfo.packageName;
        this.packageName = str;
        this.isSdkSandbox = sdkSandboxProcessName != null;
        this.sdkSandboxClientAppUid = sdkSandboxClientAppUid;
        this.sdkSandboxClientAppPackage = sdkSandboxClientAppPackage;
        if ((sInfo.flags & 2) != 0) {
            this.processName = sInfo.processName + ":" + instanceName.getClassName();
        } else if (sdkSandboxProcessName != null) {
            this.processName = sdkSandboxProcessName;
        } else {
            this.processName = sInfo.processName;
        }
        this.permission = sInfo.permission;
        this.exported = sInfo.exported;
        this.restarter = restarter;
        this.createRealTime = SystemClock.elapsedRealtime();
        this.lastActivity = SystemClock.uptimeMillis();
        this.userId = UserHandle.getUserId(this.appInfo.uid);
        this.createdFromFg = callerIsFg;
        updateKeepWarmLocked();
        updateFgsHasNotificationPermission();
        this.grifAppInfo = ITranServiceRecord.Instance().initServiceRecord(str);
    }

    public ServiceState getTracker() {
        ServiceState serviceState = this.tracker;
        if (serviceState != null) {
            return serviceState;
        }
        if ((this.serviceInfo.applicationInfo.flags & 8) == 0) {
            ServiceState serviceState2 = this.ams.mProcessStats.getServiceState(this.serviceInfo.packageName, this.serviceInfo.applicationInfo.uid, this.serviceInfo.applicationInfo.longVersionCode, this.serviceInfo.processName, this.serviceInfo.name);
            this.tracker = serviceState2;
            serviceState2.applyNewOwner(this);
        }
        return this.tracker;
    }

    public void forceClearTracker() {
        ServiceState serviceState = this.tracker;
        if (serviceState != null) {
            serviceState.clearCurrentOwner(this, true);
            this.tracker = null;
        }
    }

    public void makeRestarting(int memFactor, long now) {
        if (this.restartTracker == null) {
            if ((this.serviceInfo.applicationInfo.flags & 8) == 0) {
                this.restartTracker = this.ams.mProcessStats.getServiceState(this.serviceInfo.packageName, this.serviceInfo.applicationInfo.uid, this.serviceInfo.applicationInfo.longVersionCode, this.serviceInfo.processName, this.serviceInfo.name);
            }
            if (this.restartTracker == null) {
                return;
            }
        }
        this.restartTracker.setRestarting(true, memFactor, now);
    }

    public void setProcess(ProcessRecord proc, IApplicationThread thread, int pid, UidRecord uidRecord) {
        if (proc != null) {
            ProcessRecord processRecord = this.mAppForAllowingBgActivityStartsByStart;
            if (processRecord != null && processRecord != proc) {
                processRecord.removeAllowBackgroundActivityStartsToken(this);
                this.ams.mHandler.removeCallbacks(this.mCleanUpAllowBgActivityStartsByStartCallback);
            }
            boolean z = this.mIsAllowedBgActivityStartsByStart;
            this.mAppForAllowingBgActivityStartsByStart = z ? proc : null;
            if (z || this.mIsAllowedBgActivityStartsByBinding) {
                proc.addOrUpdateAllowBackgroundActivityStartsToken(this, getExclusiveOriginatingToken());
            } else {
                proc.removeAllowBackgroundActivityStartsToken(this);
            }
        }
        ProcessRecord processRecord2 = this.app;
        if (processRecord2 != null && processRecord2 != proc) {
            if (!this.mIsAllowedBgActivityStartsByStart) {
                processRecord2.removeAllowBackgroundActivityStartsToken(this);
            }
            this.app.mServices.updateBoundClientUids();
            this.app.mServices.updateHostingComonentTypeForBindingsLocked();
        }
        this.app = proc;
        if (this.pendingConnectionGroup > 0 && proc != null) {
            ProcessServiceRecord psr = proc.mServices;
            psr.setConnectionService(this);
            psr.setConnectionGroup(this.pendingConnectionGroup);
            psr.setConnectionImportance(this.pendingConnectionImportance);
            this.pendingConnectionImportance = 0;
            this.pendingConnectionGroup = 0;
        }
        for (int conni = this.connections.size() - 1; conni >= 0; conni--) {
            ArrayList<ConnectionRecord> cr = this.connections.valueAt(conni);
            for (int i = 0; i < cr.size(); i++) {
                ConnectionRecord conn = cr.get(i);
                if (proc != null) {
                    conn.startAssociationIfNeeded();
                } else {
                    conn.stopAssociation();
                }
            }
        }
        if (proc != null) {
            proc.mServices.updateBoundClientUids();
            proc.mServices.updateHostingComonentTypeForBindingsLocked();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ArrayMap<IBinder, ArrayList<ConnectionRecord>> getConnections() {
        if (this.connections.size() != 0 && this.connections.size() % 500 == 0) {
            Slog.d(TAG, "service connections size = " + this.connections.size() + ", this = " + this);
        }
        return this.connections;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void addConnection(IBinder binder, ConnectionRecord c) {
        ArrayList<ConnectionRecord> clist = this.connections.get(binder);
        if (clist == null) {
            clist = new ArrayList<>();
            this.connections.put(binder, clist);
        }
        clist.add(c);
        ProcessRecord processRecord = this.app;
        if (processRecord != null) {
            processRecord.mServices.addBoundClientUid(c.clientUid);
            this.app.mProfile.addHostingComponentType(512);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeConnection(IBinder binder) {
        this.connections.remove(binder);
        ProcessRecord processRecord = this.app;
        if (processRecord != null) {
            processRecord.mServices.updateBoundClientUids();
            this.app.mServices.updateHostingComonentTypeForBindingsLocked();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean canStopIfKilled(boolean isStartCanceled) {
        return this.startRequested && (this.stopIfKilled || isStartCanceled) && this.pendingStarts.isEmpty();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateIsAllowedBgActivityStartsByBinding() {
        boolean isAllowedByBinding = false;
        for (int conni = this.connections.size() - 1; conni >= 0; conni--) {
            ArrayList<ConnectionRecord> cr = this.connections.valueAt(conni);
            int i = 0;
            while (true) {
                if (i < cr.size()) {
                    if ((cr.get(i).flags & 1048576) == 0) {
                        i++;
                    } else {
                        isAllowedByBinding = true;
                        break;
                    }
                } else {
                    break;
                }
            }
            if (isAllowedByBinding) {
                break;
            }
        }
        setAllowedBgActivityStartsByBinding(isAllowedByBinding);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setAllowedBgActivityStartsByBinding(boolean newValue) {
        this.mIsAllowedBgActivityStartsByBinding = newValue;
        updateParentProcessBgActivityStartsToken();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void allowBgActivityStartsOnServiceStart(IBinder originatingToken) {
        this.mBgActivityStartsByStartOriginatingTokens.add(originatingToken);
        setAllowedBgActivityStartsByStart(true);
        ProcessRecord processRecord = this.app;
        if (processRecord != null) {
            this.mAppForAllowingBgActivityStartsByStart = processRecord;
        }
        if (this.mCleanUpAllowBgActivityStartsByStartCallback == null) {
            this.mCleanUpAllowBgActivityStartsByStartCallback = new Runnable() { // from class: com.android.server.am.ServiceRecord$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    ServiceRecord.this.m1499xf3f955ea();
                }
            };
        }
        this.ams.mHandler.postDelayed(this.mCleanUpAllowBgActivityStartsByStartCallback, this.ams.mConstants.SERVICE_BG_ACTIVITY_START_TIMEOUT);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$allowBgActivityStartsOnServiceStart$0$com-android-server-am-ServiceRecord  reason: not valid java name */
    public /* synthetic */ void m1499xf3f955ea() {
        synchronized (this.ams) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                this.mBgActivityStartsByStartOriginatingTokens.remove(0);
                if (!this.mBgActivityStartsByStartOriginatingTokens.isEmpty()) {
                    if (this.mIsAllowedBgActivityStartsByStart) {
                        ProcessRecord processRecord = this.mAppForAllowingBgActivityStartsByStart;
                        if (processRecord != null) {
                            processRecord.addOrUpdateAllowBackgroundActivityStartsToken(this, getExclusiveOriginatingToken());
                        }
                    } else {
                        Slog.wtf(TAG, "Service callback to revoke bg activity starts by service start triggered but mIsAllowedBgActivityStartsByStart = false. This should never happen.");
                    }
                } else {
                    ProcessRecord processRecord2 = this.app;
                    ProcessRecord processRecord3 = this.mAppForAllowingBgActivityStartsByStart;
                    if (processRecord2 == processRecord3) {
                        setAllowedBgActivityStartsByStart(false);
                    } else if (processRecord3 != null) {
                        processRecord3.removeAllowBackgroundActivityStartsToken(this);
                    }
                    this.mAppForAllowingBgActivityStartsByStart = null;
                }
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
    }

    private void setAllowedBgActivityStartsByStart(boolean newValue) {
        this.mIsAllowedBgActivityStartsByStart = newValue;
        updateParentProcessBgActivityStartsToken();
    }

    private void updateParentProcessBgActivityStartsToken() {
        ProcessRecord processRecord = this.app;
        if (processRecord == null) {
            return;
        }
        if (this.mIsAllowedBgActivityStartsByStart || this.mIsAllowedBgActivityStartsByBinding) {
            processRecord.addOrUpdateAllowBackgroundActivityStartsToken(this, getExclusiveOriginatingToken());
        } else {
            processRecord.removeAllowBackgroundActivityStartsToken(this);
        }
    }

    private IBinder getExclusiveOriginatingToken() {
        if (this.mIsAllowedBgActivityStartsByBinding || this.mBgActivityStartsByStartOriginatingTokens.isEmpty()) {
            return null;
        }
        IBinder firstToken = this.mBgActivityStartsByStartOriginatingTokens.get(0);
        int n = this.mBgActivityStartsByStartOriginatingTokens.size();
        for (int i = 1; i < n; i++) {
            IBinder token = this.mBgActivityStartsByStartOriginatingTokens.get(i);
            if (token != firstToken) {
                return null;
            }
        }
        return firstToken;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateKeepWarmLocked() {
        this.mKeepWarming = this.ams.mConstants.KEEP_WARMING_SERVICES.contains(this.name) && (this.ams.mUserController.getCurrentUserId() == this.userId || this.ams.mUserController.isCurrentProfile(this.userId) || this.ams.isSingleton(this.processName, this.appInfo, this.instanceName.getClassName(), this.serviceInfo.flags));
    }

    public AppBindRecord retrieveAppBindingLocked(Intent intent, ProcessRecord app) {
        Intent.FilterComparison filter = new Intent.FilterComparison(intent);
        IntentBindRecord i = this.bindings.get(filter);
        if (i == null) {
            i = new IntentBindRecord(this, filter);
            this.bindings.put(filter, i);
        }
        AppBindRecord a = i.apps.get(app);
        if (a != null) {
            return a;
        }
        AppBindRecord a2 = new AppBindRecord(this, i, app);
        i.apps.put(app, a2);
        return a2;
    }

    public boolean hasAutoCreateConnections() {
        for (int conni = this.connections.size() - 1; conni >= 0; conni--) {
            ArrayList<ConnectionRecord> cr = this.connections.valueAt(conni);
            for (int i = 0; i < cr.size(); i++) {
                if ((cr.get(i).flags & 1) != 0) {
                    return true;
                }
            }
        }
        return false;
    }

    public void updateAllowlistManager() {
        this.allowlistManager = false;
        for (int conni = this.connections.size() - 1; conni >= 0; conni--) {
            ArrayList<ConnectionRecord> cr = this.connections.valueAt(conni);
            for (int i = 0; i < cr.size(); i++) {
                if ((cr.get(i).flags & 16777216) != 0) {
                    this.allowlistManager = true;
                    return;
                }
            }
        }
    }

    public void resetRestartCounter() {
        this.restartCount = 0;
        this.restartDelay = 0L;
        this.restartTime = 0L;
        this.mEarliestRestartTime = 0L;
        this.mRestartSchedulingTime = 0L;
    }

    public StartItem findDeliveredStart(int id, boolean taskRemoved, boolean remove) {
        int N = this.deliveredStarts.size();
        for (int i = 0; i < N; i++) {
            StartItem si = this.deliveredStarts.get(i);
            if (si.id == id && si.taskRemoved == taskRemoved) {
                if (remove) {
                    this.deliveredStarts.remove(i);
                }
                return si;
            }
        }
        return null;
    }

    public int getLastStartId() {
        return this.lastStartId;
    }

    public int makeNextStartId() {
        int i = this.lastStartId + 1;
        this.lastStartId = i;
        if (i < 1) {
            this.lastStartId = 1;
        }
        return this.lastStartId;
    }

    private void updateFgsHasNotificationPermission() {
        final String localPackageName = this.packageName;
        final int appUid = this.appInfo.uid;
        this.ams.mHandler.post(new Runnable() { // from class: com.android.server.am.ServiceRecord.1
            @Override // java.lang.Runnable
            public void run() {
                NotificationManagerInternal nm = (NotificationManagerInternal) LocalServices.getService(NotificationManagerInternal.class);
                if (nm == null) {
                    return;
                }
                ServiceRecord.this.mFgsHasNotificationPermission = nm.areNotificationsEnabledForPackage(localPackageName, appUid);
            }
        });
    }

    public void postNotification() {
        if (this.isForeground && this.foregroundNoti != null && this.app != null) {
            final int appUid = this.appInfo.uid;
            final int appPid = this.app.getPid();
            final String localPackageName = this.packageName;
            final int localForegroundId = this.foregroundId;
            final Notification _foregroundNoti = this.foregroundNoti;
            if (ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE) {
                Slog.d(TAG, "Posting notification " + _foregroundNoti + " for foreground service " + this);
            }
            this.ams.mHandler.post(new Runnable() { // from class: com.android.server.am.ServiceRecord.2
                /* JADX WARN: Removed duplicated region for block: B:20:0x0122  */
                /* JADX WARN: Removed duplicated region for block: B:31:0x016a A[Catch: RuntimeException -> 0x01af, TryCatch #3 {RuntimeException -> 0x01af, blocks: (B:18:0x0114, B:21:0x0124, B:27:0x0146, B:28:0x0163, B:29:0x0164, B:31:0x016a, B:32:0x0191, B:33:0x01ae), top: B:44:0x0114 }] */
                /* JADX WARN: Removed duplicated region for block: B:32:0x0191 A[Catch: RuntimeException -> 0x01af, TryCatch #3 {RuntimeException -> 0x01af, blocks: (B:18:0x0114, B:21:0x0124, B:27:0x0146, B:28:0x0163, B:29:0x0164, B:31:0x016a, B:32:0x0191, B:33:0x01ae), top: B:44:0x0114 }] */
                @Override // java.lang.Runnable
                /*
                    Code decompiled incorrectly, please refer to instructions dump.
                */
                public void run() {
                    Notification localForegroundNoti;
                    CharSequence appName;
                    NotificationManagerInternal nm = (NotificationManagerInternal) LocalServices.getService(NotificationManagerInternal.class);
                    if (nm != null) {
                        ServiceRecord.this.mFgsHasNotificationPermission = nm.areNotificationsEnabledForPackage(localPackageName, appUid);
                        Notification localForegroundNoti2 = _foregroundNoti;
                        try {
                        } catch (RuntimeException e) {
                            e = e;
                        }
                        try {
                            if (localForegroundNoti2.getSmallIcon() == null) {
                                Slog.v(ServiceRecord.TAG, "Attempted to start a foreground service (" + ServiceRecord.this.shortInstanceName + ") with a broken notification (no icon: " + localForegroundNoti2 + ")");
                                CharSequence appName2 = ServiceRecord.this.appInfo.loadLabel(ServiceRecord.this.ams.mContext.getPackageManager());
                                if (appName2 != null) {
                                    appName = appName2;
                                } else {
                                    appName = ServiceRecord.this.appInfo.packageName;
                                }
                                try {
                                    Context ctx = ServiceRecord.this.ams.mContext.createPackageContextAsUser(ServiceRecord.this.appInfo.packageName, 0, new UserHandle(ServiceRecord.this.userId));
                                    Notification.Builder notiBuilder = new Notification.Builder(ctx, localForegroundNoti2.getChannelId());
                                    notiBuilder.setSmallIcon(ServiceRecord.this.appInfo.icon);
                                    notiBuilder.setFlag(64, true);
                                    Intent runningIntent = new Intent("android.settings.APPLICATION_DETAILS_SETTINGS");
                                    runningIntent.setData(Uri.fromParts("package", ServiceRecord.this.appInfo.packageName, null));
                                    PendingIntent pi = PendingIntent.getActivityAsUser(ServiceRecord.this.ams.mContext, 0, runningIntent, AudioFormat.DTS_HD, null, UserHandle.of(ServiceRecord.this.userId));
                                    notiBuilder.setColor(ServiceRecord.this.ams.mContext.getColor(17170460));
                                    notiBuilder.setContentTitle(ServiceRecord.this.ams.mContext.getString(17039691, appName));
                                    notiBuilder.setContentText(ServiceRecord.this.ams.mContext.getString(17039690, appName));
                                    notiBuilder.setContentIntent(pi);
                                    localForegroundNoti = notiBuilder.build();
                                } catch (PackageManager.NameNotFoundException e2) {
                                }
                                if (nm.getNotificationChannel(localPackageName, appUid, localForegroundNoti.getChannelId()) == null) {
                                    int targetSdkVersion = 27;
                                    try {
                                        ApplicationInfo applicationInfo = ServiceRecord.this.ams.mContext.getPackageManager().getApplicationInfoAsUser(ServiceRecord.this.appInfo.packageName, 0, ServiceRecord.this.userId);
                                        targetSdkVersion = applicationInfo.targetSdkVersion;
                                    } catch (PackageManager.NameNotFoundException e3) {
                                    }
                                    if (targetSdkVersion >= 27) {
                                        throw new RuntimeException("invalid channel for service notification: " + ServiceRecord.this.foregroundNoti);
                                    }
                                }
                                if (localForegroundNoti.getSmallIcon() != null) {
                                    throw new RuntimeException("invalid service notification: " + ServiceRecord.this.foregroundNoti);
                                }
                                String str = localPackageName;
                                nm.enqueueNotification(str, str, appUid, appPid, null, localForegroundId, localForegroundNoti, ServiceRecord.this.userId);
                                ServiceRecord.this.foregroundNoti = localForegroundNoti;
                                ServiceRecord serviceRecord = ServiceRecord.this;
                                serviceRecord.signalForegroundServiceNotification(serviceRecord.packageName, ServiceRecord.this.appInfo.uid, localForegroundId, false);
                                return;
                            }
                            if (nm.getNotificationChannel(localPackageName, appUid, localForegroundNoti.getChannelId()) == null) {
                            }
                            if (localForegroundNoti.getSmallIcon() != null) {
                            }
                        } catch (RuntimeException e4) {
                            e = e4;
                            localForegroundNoti2 = localForegroundNoti;
                            Slog.w(ServiceRecord.TAG, "Error showing notification for service", e);
                            ServiceRecord.this.ams.mServices.killMisbehavingService(record, appUid, appPid, localPackageName, 3);
                            return;
                        }
                        localForegroundNoti = localForegroundNoti2;
                    }
                }
            });
        }
    }

    public void cancelNotification() {
        final String localPackageName = this.packageName;
        final int localForegroundId = this.foregroundId;
        final int appUid = this.appInfo.uid;
        ProcessRecord processRecord = this.app;
        final int appPid = processRecord != null ? processRecord.getPid() : 0;
        this.ams.mHandler.post(new Runnable() { // from class: com.android.server.am.ServiceRecord.3
            @Override // java.lang.Runnable
            public void run() {
                NotificationManagerInternal nm = (NotificationManagerInternal) LocalServices.getService(NotificationManagerInternal.class);
                if (nm == null) {
                    return;
                }
                try {
                    String str = localPackageName;
                    nm.cancelNotification(str, str, appUid, appPid, null, localForegroundId, ServiceRecord.this.userId);
                } catch (RuntimeException e) {
                    Slog.w(ServiceRecord.TAG, "Error canceling notification for service", e);
                }
                ServiceRecord serviceRecord = ServiceRecord.this;
                serviceRecord.signalForegroundServiceNotification(serviceRecord.packageName, ServiceRecord.this.appInfo.uid, localForegroundId, true);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void signalForegroundServiceNotification(String packageName, int uid, int foregroundId, boolean canceling) {
        synchronized (this.ams) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                for (int i = this.ams.mForegroundServiceStateListeners.size() - 1; i >= 0; i--) {
                    this.ams.mForegroundServiceStateListeners.get(i).onForegroundServiceNotificationUpdated(packageName, this.appInfo.uid, foregroundId, canceling);
                }
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
    }

    public void stripForegroundServiceFlagFromNotification() {
        final int localForegroundId = this.foregroundId;
        final int localUserId = this.userId;
        final String localPackageName = this.packageName;
        this.ams.mHandler.post(new Runnable() { // from class: com.android.server.am.ServiceRecord.4
            @Override // java.lang.Runnable
            public void run() {
                NotificationManagerInternal nmi = (NotificationManagerInternal) LocalServices.getService(NotificationManagerInternal.class);
                if (nmi == null) {
                    return;
                }
                nmi.removeForegroundServiceFlagFromNotification(localPackageName, localForegroundId, localUserId);
            }
        });
    }

    public void clearDeliveredStartsLocked() {
        for (int i = this.deliveredStarts.size() - 1; i >= 0; i--) {
            this.deliveredStarts.get(i).removeUriPermissionsLocked();
        }
        this.deliveredStarts.clear();
    }

    public String toString() {
        String str = this.stringName;
        if (str != null) {
            return str;
        }
        StringBuilder sb = new StringBuilder(128);
        sb.append("ServiceRecord{").append(Integer.toHexString(System.identityHashCode(this))).append(" u").append(this.userId).append(' ').append(this.shortInstanceName).append('}');
        String sb2 = sb.toString();
        this.stringName = sb2;
        return sb2;
    }

    public ComponentName getComponentName() {
        return this.name;
    }
}
