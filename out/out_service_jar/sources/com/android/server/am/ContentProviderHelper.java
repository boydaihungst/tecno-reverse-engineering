package com.android.server.am;

import android.app.AppGlobals;
import android.app.ContentProviderHolder;
import android.app.IApplicationThread;
import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.Context;
import android.content.IContentProvider;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PathPermission;
import android.content.pm.ProviderInfo;
import android.content.pm.UserInfo;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteCallback;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.Trace;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.ArrayUtils;
import com.android.server.LocalServices;
import com.android.server.RescueParty;
import com.android.server.am.ActivityManagerService;
import com.android.server.pm.PackageManagerService;
import com.android.server.pm.UserManagerInternal;
import com.android.server.vibrator.VibratorManagerService;
import com.transsion.hubcore.server.am.ITranActivityManagerService;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
/* loaded from: classes.dex */
public class ContentProviderHelper {
    private static final int[] PROCESS_STATE_STATS_FORMAT = {32, 544, 10272};
    private static final String TAG = "ContentProviderHelper";
    private final ArrayList<ContentProviderRecord> mLaunchingProviders = new ArrayList<>();
    private final long[] mProcessStateStatsLongs = new long[1];
    private final ProviderMap mProviderMap;
    private final ActivityManagerService mService;
    private boolean mSystemProvidersInstalled;

    /* JADX INFO: Access modifiers changed from: package-private */
    public ContentProviderHelper(ActivityManagerService service, boolean createProviderMap) {
        this.mService = service;
        this.mProviderMap = createProviderMap ? new ProviderMap(service) : null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ProviderMap getProviderMap() {
        return this.mProviderMap;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ContentProviderHolder getContentProvider(IApplicationThread caller, String callingPackage, String name, int userId, boolean stable) {
        this.mService.enforceNotIsolatedCaller("getContentProvider");
        if (Process.isSdkSandboxUid(Binder.getCallingUid())) {
            Slog.w(TAG, "Sdk sandbox process " + Binder.getCallingUid() + " is accessing content provider " + name + ". This access will most likely be blocked in the future");
        }
        if (caller == null) {
            String msg = "null IApplicationThread when getting content provider " + name;
            Slog.w(TAG, msg);
            throw new SecurityException(msg);
        }
        int callingUid = Binder.getCallingUid();
        if (callingPackage != null && this.mService.mAppOpsService.checkPackage(callingUid, callingPackage) != 0) {
            throw new SecurityException("Given calling package " + callingPackage + " does not match caller's uid " + callingUid);
        }
        return getContentProviderImpl(caller, name, null, callingUid, callingPackage, null, stable, userId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ContentProviderHolder getContentProviderExternal(String name, int userId, IBinder token, String tag) {
        this.mService.enforceCallingPermission("android.permission.ACCESS_CONTENT_PROVIDERS_EXTERNALLY", "Do not have permission in call getContentProviderExternal()");
        return getContentProviderExternalUnchecked(name, token, Binder.getCallingUid(), tag != null ? tag : "*external*", this.mService.mUserController.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, false, 2, "getContentProvider", null));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ContentProviderHolder getContentProviderExternalUnchecked(String name, IBinder token, int callingUid, String callingTag, int userId) {
        return getContentProviderImpl(null, name, token, callingUid, null, callingTag, true, userId);
    }

    private boolean shouldCrossDualApp(String packageName, int userId) {
        if (((UserManagerInternal) LocalServices.getService(UserManagerInternal.class)).isDualProfile(userId)) {
            long origId = Binder.clearCallingIdentity();
            try {
                try {
                    if (AppGlobals.getPackageManager().getPackageInfo(packageName, 0L, userId) == null) {
                        Binder.restoreCallingIdentity(origId);
                        return true;
                    }
                } catch (RemoteException e) {
                    Slog.w(TAG, "Failed get " + packageName + " info", e);
                }
                Binder.restoreCallingIdentity(origId);
                return false;
            }
        }
        return false;
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Unreachable block: B:407:0x096c
        	at jadx.core.dex.visitors.blocks.BlockProcessor.checkForUnreachableBlocks(BlockProcessor.java:81)
        	at jadx.core.dex.visitors.blocks.BlockProcessor.processBlocksTree(BlockProcessor.java:47)
        	at jadx.core.dex.visitors.blocks.BlockProcessor.visit(BlockProcessor.java:39)
        */
    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [594=9, 649=24, 683=4, 684=4, 387=6, 484=5] */
    private android.app.ContentProviderHolder getContentProviderImpl(android.app.IApplicationThread r51, java.lang.String r52, android.os.IBinder r53, int r54, java.lang.String r55, java.lang.String r56, boolean r57, int r58) {
        /*
            r50 = this;
            r15 = r50
            r14 = r51
            r13 = r52
            r11 = r54
            r1 = r58
            r26 = 0
            r2 = 0
            r3 = 0
            r25 = r58
            com.android.server.am.ActivityManagerService r12 = r15.mService
            monitor-enter(r12)
            com.android.server.am.ActivityManagerService.boostPriorityForLockedSection()     // Catch: java.lang.Throwable -> Le67
            long r4 = android.os.SystemClock.uptimeMillis()     // Catch: java.lang.Throwable -> Le67
            r8 = r4
            r4 = 0
            if (r14 == 0) goto L65
            com.android.server.am.ActivityManagerService r5 = r15.mService     // Catch: java.lang.Throwable -> L5a
            com.android.server.am.ProcessRecord r5 = r5.getRecordForAppLOSP(r14)     // Catch: java.lang.Throwable -> L5a
            r4 = r5
            if (r4 == 0) goto L29
            r10 = r4
            goto L66
        L29:
            java.lang.SecurityException r5 = new java.lang.SecurityException     // Catch: java.lang.Throwable -> L5a
            java.lang.StringBuilder r6 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L5a
            r6.<init>()     // Catch: java.lang.Throwable -> L5a
            java.lang.String r7 = "Unable to find app for caller "
            java.lang.StringBuilder r6 = r6.append(r7)     // Catch: java.lang.Throwable -> L5a
            java.lang.StringBuilder r6 = r6.append(r14)     // Catch: java.lang.Throwable -> L5a
            java.lang.String r7 = " (pid="
            java.lang.StringBuilder r6 = r6.append(r7)     // Catch: java.lang.Throwable -> L5a
            int r7 = android.os.Binder.getCallingPid()     // Catch: java.lang.Throwable -> L5a
            java.lang.StringBuilder r6 = r6.append(r7)     // Catch: java.lang.Throwable -> L5a
            java.lang.String r7 = ") when getting content provider "
            java.lang.StringBuilder r6 = r6.append(r7)     // Catch: java.lang.Throwable -> L5a
            java.lang.StringBuilder r6 = r6.append(r13)     // Catch: java.lang.Throwable -> L5a
            java.lang.String r6 = r6.toString()     // Catch: java.lang.Throwable -> L5a
            r5.<init>(r6)     // Catch: java.lang.Throwable -> L5a
            throw r5     // Catch: java.lang.Throwable -> L5a
        L5a:
            r0 = move-exception
            r9 = r11
            r34 = r12
            r4 = r14
            r14 = r15
            r15 = r13
            r13 = r1
            r1 = r0
            goto Le74
        L65:
            r10 = r4
        L66:
            r4 = 1
            java.lang.String r5 = "getContentProviderImpl: getProviderByName"
            r15.checkTime(r8, r5)     // Catch: java.lang.Throwable -> Le67
            com.android.server.am.ProviderMap r5 = r15.mProviderMap     // Catch: java.lang.Throwable -> Le67
            com.android.server.am.ContentProviderRecord r5 = r5.getProviderByName(r13, r1)     // Catch: java.lang.Throwable -> Le67
            if (r5 == 0) goto L1e9
            com.transsion.hubcore.griffin.ITranGriffinFeature r6 = com.transsion.hubcore.griffin.ITranGriffinFeature.Instance()     // Catch: java.lang.Throwable -> L1d6
            boolean r6 = r6.isGriffinSupport()     // Catch: java.lang.Throwable -> L1d6
            if (r6 == 0) goto L1e9
            com.transsion.hubcore.griffin.ITranGriffinFeature r6 = com.transsion.hubcore.griffin.ITranGriffinFeature.Instance()     // Catch: java.lang.Throwable -> L1d6
            boolean r6 = r6.isGriffinDebugOpen()     // Catch: java.lang.Throwable -> L1d6
            if (r6 == 0) goto L1e9
            int r6 = android.os.Binder.getCallingUid()     // Catch: java.lang.Throwable -> L1d6
            int r7 = android.os.Binder.getCallingPid()     // Catch: java.lang.Throwable -> L1d6
            if (r10 == 0) goto Lad
            r16 = r2
            android.content.pm.ApplicationInfo r2 = r10.info     // Catch: java.lang.Throwable -> La0
            if (r2 == 0) goto L9d
            android.content.pm.ApplicationInfo r2 = r10.info     // Catch: java.lang.Throwable -> La0
            java.lang.String r2 = r2.packageName     // Catch: java.lang.Throwable -> La0
            goto Lb1
        L9d:
            java.lang.String r2 = ""
            goto Lb1
        La0:
            r0 = move-exception
            r9 = r11
            r34 = r12
            r4 = r14
            r14 = r15
            r2 = r16
            r15 = r13
            r13 = r1
            r1 = r0
            goto Le74
        Lad:
            r16 = r2
            java.lang.String r2 = ""
        Lb1:
            if (r10 == 0) goto Lc7
            r17 = r3
            java.lang.String r3 = r10.processName     // Catch: java.lang.Throwable -> Lb8
            goto Lcc
        Lb8:
            r0 = move-exception
            r9 = r11
            r34 = r12
            r4 = r14
            r14 = r15
            r2 = r16
            r3 = r17
            r15 = r13
            r13 = r1
            r1 = r0
            goto Le74
        Lc7:
            r17 = r3
            java.lang.String r3 = "unknown"
        Lcc:
            r18 = r52
            r19 = r4
            android.content.ComponentName r4 = r5.name     // Catch: java.lang.Throwable -> L1b0
            java.lang.String r4 = r4.getPackageName()     // Catch: java.lang.Throwable -> L1b0
            r20 = r8
            android.content.ComponentName r8 = r5.name     // Catch: java.lang.Throwable -> L1b0
            java.lang.String r8 = r8.getClassName()     // Catch: java.lang.Throwable -> L1b0
            int r9 = r5.uid     // Catch: java.lang.Throwable -> L1b0
            android.content.pm.ProviderInfo r11 = r5.info     // Catch: java.lang.Throwable -> L1b0
            java.lang.String r11 = r11.processName     // Catch: java.lang.Throwable -> L1b0
            com.android.server.am.ProcessRecord r14 = r5.launchingApp     // Catch: java.lang.Throwable -> L1b0
            if (r14 == 0) goto Lfb
            r14 = 1
            r22 = r14
            com.android.server.am.ProcessRecord r14 = r5.launchingApp     // Catch: java.lang.Throwable -> L2b3
            int r14 = r14.mPid     // Catch: java.lang.Throwable -> L2b3
            r23 = 0
            r13 = r23
            r49 = r22
            r22 = r10
            r10 = r14
            r14 = r49
            goto L11f
        Lfb:
            com.android.server.am.ProcessRecord r14 = r5.proc     // Catch: java.lang.Throwable -> L1b0
            if (r14 == 0) goto L112
            r14 = 0
            r22 = r14
            com.android.server.am.ProcessRecord r14 = r5.proc     // Catch: java.lang.Throwable -> L2b3
            int r14 = r14.mPid     // Catch: java.lang.Throwable -> L2b3
            r23 = 0
            r13 = r23
            r49 = r22
            r22 = r10
            r10 = r14
            r14 = r49
            goto L11f
        L112:
            r14 = 0
            r22 = 0
            r23 = 1
            r13 = r23
            r49 = r22
            r22 = r10
            r10 = r49
        L11f:
            java.lang.String r1 = "Griffin/getContentProviderImpl"
            r23 = r5
            java.lang.StringBuilder r5 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L1b0
            r5.<init>()     // Catch: java.lang.Throwable -> L1b0
            r24 = r1
            java.lang.String r1 = "caller:"
            java.lang.StringBuilder r1 = r5.append(r1)     // Catch: java.lang.Throwable -> L1b0
            java.lang.StringBuilder r1 = r1.append(r2)     // Catch: java.lang.Throwable -> L1b0
            java.lang.String r5 = ",uid="
            java.lang.StringBuilder r1 = r1.append(r5)     // Catch: java.lang.Throwable -> L1b0
            java.lang.StringBuilder r1 = r1.append(r6)     // Catch: java.lang.Throwable -> L1b0
            java.lang.String r5 = ",pid="
            java.lang.StringBuilder r1 = r1.append(r5)     // Catch: java.lang.Throwable -> L1b0
            java.lang.StringBuilder r1 = r1.append(r7)     // Catch: java.lang.Throwable -> L1b0
            java.lang.String r5 = ",proc="
            java.lang.StringBuilder r1 = r1.append(r5)     // Catch: java.lang.Throwable -> L1b0
            java.lang.StringBuilder r1 = r1.append(r3)     // Catch: java.lang.Throwable -> L1b0
            java.lang.String r5 = ",authority="
            java.lang.StringBuilder r1 = r1.append(r5)     // Catch: java.lang.Throwable -> L1b0
            r5 = r18
            java.lang.StringBuilder r1 = r1.append(r5)     // Catch: java.lang.Throwable -> L1b0
            r18 = r2
            java.lang.String r2 = ",provider:"
            java.lang.StringBuilder r1 = r1.append(r2)     // Catch: java.lang.Throwable -> L1b0
            java.lang.StringBuilder r1 = r1.append(r4)     // Catch: java.lang.Throwable -> L1b0
            java.lang.String r2 = ",name="
            java.lang.StringBuilder r1 = r1.append(r2)     // Catch: java.lang.Throwable -> L1b0
            java.lang.StringBuilder r1 = r1.append(r8)     // Catch: java.lang.Throwable -> L1b0
            java.lang.String r2 = ",uid="
            java.lang.StringBuilder r1 = r1.append(r2)     // Catch: java.lang.Throwable -> L1b0
            java.lang.StringBuilder r1 = r1.append(r9)     // Catch: java.lang.Throwable -> L1b0
            java.lang.String r2 = ",proc="
            java.lang.StringBuilder r1 = r1.append(r2)     // Catch: java.lang.Throwable -> L1b0
            java.lang.StringBuilder r1 = r1.append(r11)     // Catch: java.lang.Throwable -> L1b0
            java.lang.String r2 = ",needStartProc="
            java.lang.StringBuilder r1 = r1.append(r2)     // Catch: java.lang.Throwable -> L1b0
            java.lang.StringBuilder r1 = r1.append(r13)     // Catch: java.lang.Throwable -> L1b0
            java.lang.String r2 = ",isLaunching="
            java.lang.StringBuilder r1 = r1.append(r2)     // Catch: java.lang.Throwable -> L1b0
            java.lang.StringBuilder r1 = r1.append(r14)     // Catch: java.lang.Throwable -> L1b0
            java.lang.String r2 = ",pid="
            java.lang.StringBuilder r1 = r1.append(r2)     // Catch: java.lang.Throwable -> L1b0
            java.lang.StringBuilder r1 = r1.append(r10)     // Catch: java.lang.Throwable -> L1b0
            java.lang.String r1 = r1.toString()     // Catch: java.lang.Throwable -> L1b0
            r2 = r24
            android.util.Slog.d(r2, r1)     // Catch: java.lang.Throwable -> L1b0
            goto L1f5
        L1b0:
            r0 = move-exception
            r4 = r51
            r9 = r54
            r13 = r58
            r1 = r0
            r34 = r12
            r14 = r15
            r2 = r16
            r3 = r17
            r15 = r52
            goto Le74
        L1c3:
            r0 = move-exception
            r17 = r3
            r4 = r51
            r9 = r54
            r13 = r58
            r1 = r0
            r34 = r12
            r14 = r15
            r2 = r16
            r15 = r52
            goto Le74
        L1d6:
            r0 = move-exception
            r16 = r2
            r17 = r3
            r4 = r51
            r9 = r54
            r13 = r58
            r1 = r0
            r34 = r12
            r14 = r15
            r15 = r52
            goto Le74
        L1e9:
            r16 = r2
            r17 = r3
            r19 = r4
            r23 = r5
            r20 = r8
            r22 = r10
        L1f5:
            r2 = 0
            if (r23 != 0) goto L2d9
            r1 = r58
            if (r1 == 0) goto L2d6
            com.android.server.am.ProviderMap r3 = r15.mProviderMap     // Catch: java.lang.Throwable -> L2c4
            r13 = r52
            com.android.server.am.ContentProviderRecord r3 = r3.getProviderByName(r13, r2)     // Catch: java.lang.Throwable -> L2b3
            r5 = r3
            if (r5 == 0) goto L2aa
            android.content.pm.ProviderInfo r3 = r5.info     // Catch: java.lang.Throwable -> L2b3
            com.android.server.am.ActivityManagerService r4 = r15.mService     // Catch: java.lang.Throwable -> L29a
            java.lang.String r6 = r3.processName     // Catch: java.lang.Throwable -> L29a
            android.content.pm.ApplicationInfo r7 = r3.applicationInfo     // Catch: java.lang.Throwable -> L29a
            java.lang.String r8 = r3.name     // Catch: java.lang.Throwable -> L29a
            int r9 = r3.flags     // Catch: java.lang.Throwable -> L29a
            boolean r4 = r4.isSingleton(r6, r7, r8, r9)     // Catch: java.lang.Throwable -> L29a
            if (r4 == 0) goto L238
            com.android.server.am.ActivityManagerService r4 = r15.mService     // Catch: java.lang.Throwable -> L29a
            if (r22 != 0) goto L222
            r6 = r54
            r10 = r22
            goto L226
        L222:
            r10 = r22
            int r6 = r10.uid     // Catch: java.lang.Throwable -> L29a
        L226:
            android.content.pm.ApplicationInfo r7 = r3.applicationInfo     // Catch: java.lang.Throwable -> L29a
            int r7 = r7.uid     // Catch: java.lang.Throwable -> L29a
            boolean r4 = r4.isValidSingletonCall(r6, r7)     // Catch: java.lang.Throwable -> L29a
            if (r4 == 0) goto L23a
            r1 = 0
            r4 = 0
            r14 = r1
            r27 = r4
            r11 = r5
            goto L2e6
        L238:
            r10 = r22
        L23a:
            boolean r4 = android.content.ContentProvider.isAuthorityRedirectedForCloneProfile(r52)     // Catch: java.lang.Throwable -> L29a
            if (r4 == 0) goto L264
            java.lang.Class<com.android.server.pm.UserManagerInternal> r4 = com.android.server.pm.UserManagerInternal.class
            java.lang.Object r4 = com.android.server.LocalServices.getService(r4)     // Catch: java.lang.Throwable -> L29a
            com.android.server.pm.UserManagerInternal r4 = (com.android.server.pm.UserManagerInternal) r4     // Catch: java.lang.Throwable -> L29a
            android.content.pm.UserInfo r6 = r4.getUserInfo(r1)     // Catch: java.lang.Throwable -> L29a
            if (r6 == 0) goto L25c
            boolean r7 = r6.isCloneProfile()     // Catch: java.lang.Throwable -> L29a
            if (r7 == 0) goto L25c
            int r7 = r4.getProfileParentId(r1)     // Catch: java.lang.Throwable -> L29a
            r1 = r7
            r7 = 0
            r4 = r7
            goto L25e
        L25c:
            r4 = r19
        L25e:
            r14 = r1
            r27 = r4
            r11 = r5
            goto L2e6
        L264:
            android.content.pm.ApplicationInfo r4 = r3.applicationInfo     // Catch: java.lang.Throwable -> L29a
            java.lang.String r4 = r4.packageName     // Catch: java.lang.Throwable -> L29a
            boolean r4 = r15.shouldCrossDualApp(r4, r1)     // Catch: java.lang.Throwable -> L29a
            if (r4 == 0) goto L292
            java.lang.Class<com.android.server.pm.UserManagerInternal> r4 = com.android.server.pm.UserManagerInternal.class
            java.lang.Object r4 = com.android.server.LocalServices.getService(r4)     // Catch: java.lang.Throwable -> L29a
            com.android.server.pm.UserManagerInternal r4 = (com.android.server.pm.UserManagerInternal) r4     // Catch: java.lang.Throwable -> L29a
            android.content.pm.UserInfo r6 = r4.getUserInfo(r1)     // Catch: java.lang.Throwable -> L29a
            if (r6 == 0) goto L28a
            boolean r7 = r6.isDualProfile()     // Catch: java.lang.Throwable -> L29a
            if (r7 == 0) goto L28a
            int r7 = r4.getProfileParentId(r1)     // Catch: java.lang.Throwable -> L29a
            r1 = r7
            r7 = 0
            r4 = r7
            goto L28c
        L28a:
            r4 = r19
        L28c:
            r14 = r1
            r27 = r4
            r11 = r5
            goto L2e6
        L292:
            r5 = 0
            r3 = 0
            r14 = r1
            r11 = r5
            r27 = r19
            goto L2e6
        L29a:
            r0 = move-exception
            r4 = r51
            r9 = r54
            r2 = r3
            r34 = r12
            r14 = r15
            r3 = r17
            r15 = r13
            r13 = r1
            r1 = r0
            goto Le74
        L2aa:
            r10 = r22
            r14 = r1
            r11 = r5
            r3 = r16
            r27 = r19
            goto L2e6
        L2b3:
            r0 = move-exception
            r4 = r51
            r9 = r54
            r34 = r12
            r14 = r15
            r2 = r16
            r3 = r17
            r15 = r13
            r13 = r1
            r1 = r0
            goto Le74
        L2c4:
            r0 = move-exception
            r4 = r51
            r9 = r54
            r13 = r1
            r34 = r12
            r14 = r15
            r2 = r16
            r3 = r17
            r15 = r52
            r1 = r0
            goto Le74
        L2d6:
            r13 = r52
            goto L2dd
        L2d9:
            r13 = r52
            r1 = r58
        L2dd:
            r10 = r22
            r14 = r1
            r3 = r16
            r27 = r19
            r11 = r23
        L2e6:
            r1 = 0
            r8 = 1
            if (r11 == 0) goto L359
            com.android.server.am.ProcessRecord r4 = r11.proc     // Catch: java.lang.Throwable -> L346
            if (r4 == 0) goto L359
            com.android.server.am.ProcessRecord r4 = r11.proc     // Catch: java.lang.Throwable -> L346
            boolean r4 = r4.isKilled()     // Catch: java.lang.Throwable -> L346
            if (r4 != 0) goto L2f8
            r4 = r8
            goto L2f9
        L2f8:
            r4 = r2
        L2f9:
            com.android.server.am.ProcessRecord r5 = r11.proc     // Catch: java.lang.Throwable -> L334
            boolean r5 = r5.isKilled()     // Catch: java.lang.Throwable -> L334
            if (r5 == 0) goto L32f
            com.android.server.am.ProcessRecord r5 = r11.proc     // Catch: java.lang.Throwable -> L334
            boolean r5 = r5.isKilledByAm()     // Catch: java.lang.Throwable -> L334
            if (r5 == 0) goto L32f
            java.lang.String r5 = "ContentProviderHelper"
            java.lang.StringBuilder r6 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L334
            r6.<init>()     // Catch: java.lang.Throwable -> L334
            com.android.server.am.ProcessRecord r7 = r11.proc     // Catch: java.lang.Throwable -> L334
            java.lang.String r7 = r7.toString()     // Catch: java.lang.Throwable -> L334
            java.lang.StringBuilder r6 = r6.append(r7)     // Catch: java.lang.Throwable -> L334
            java.lang.String r7 = " was killed by AM but isn't really dead"
            java.lang.StringBuilder r6 = r6.append(r7)     // Catch: java.lang.Throwable -> L334
            java.lang.String r6 = r6.toString()     // Catch: java.lang.Throwable -> L334
            android.util.Slog.w(r5, r6)     // Catch: java.lang.Throwable -> L334
            com.android.server.am.ProcessRecord r5 = r11.proc     // Catch: java.lang.Throwable -> L334
            r1 = r5
            r28 = r1
            r29 = r4
            goto L35d
        L32f:
            r28 = r1
            r29 = r4
            goto L35d
        L334:
            r0 = move-exception
            r9 = r54
            r1 = r0
            r2 = r3
            r3 = r4
            r34 = r12
            r4 = r51
            r49 = r15
            r15 = r13
            r13 = r14
            r14 = r49
            goto Le74
        L346:
            r0 = move-exception
            r4 = r51
            r9 = r54
            r1 = r0
            r2 = r3
            r34 = r12
            r3 = r17
            r49 = r15
            r15 = r13
            r13 = r14
            r14 = r49
            goto Le74
        L359:
            r28 = r1
            r29 = r17
        L35d:
            r6 = 0
            r9 = 0
            if (r29 == 0) goto L5f8
            android.content.pm.ProviderInfo r1 = r11.info     // Catch: java.lang.Throwable -> L5e4
            r3 = r1
            if (r10 == 0) goto L391
            boolean r1 = r11.canRunHere(r10)     // Catch: java.lang.Throwable -> L3bf
            if (r1 == 0) goto L391
            android.content.ComponentName r1 = r11.name     // Catch: java.lang.Throwable -> L3bf
            java.lang.String r7 = r1.flattenToShortString()     // Catch: java.lang.Throwable -> L3bf
            r1 = r50
            r2 = r10
            r4 = r54
            r5 = r14
            r6 = r27
            r13 = r9
            r22 = r10
            r30 = r20
            r10 = r8
            r8 = r30
            r1.checkAssociationAndPermissionLocked(r2, r3, r4, r5, r6, r7, r8)     // Catch: java.lang.Throwable -> L3bf
            android.app.ContentProviderHolder r1 = r11.newHolder(r13, r10)     // Catch: java.lang.Throwable -> L3bf
            r1.provider = r13     // Catch: java.lang.Throwable -> L3bf
            monitor-exit(r12)     // Catch: java.lang.Throwable -> L3bf
            com.android.server.am.ActivityManagerService.resetPriorityAfterLockedSection()
            return r1
        L391:
            r13 = r9
            r22 = r10
            r30 = r20
            r10 = r8
            android.content.pm.IPackageManager r1 = android.app.AppGlobals.getPackageManager()     // Catch: java.lang.Throwable -> L3bf android.os.RemoteException -> L3d0
            r9 = r13
            r13 = r52
            android.content.pm.ProviderInfo r1 = r1.resolveContentProvider(r13, r6, r14)     // Catch: java.lang.Throwable -> L3aa android.os.RemoteException -> L3bd
            if (r1 != 0) goto L3a9
            monitor-exit(r12)     // Catch: java.lang.Throwable -> L3aa
            com.android.server.am.ActivityManagerService.resetPriorityAfterLockedSection()
            return r9
        L3a9:
            goto L3d4
        L3aa:
            r0 = move-exception
            r4 = r51
            r9 = r54
            r1 = r0
            r2 = r3
            r34 = r12
            r3 = r29
            r49 = r15
            r15 = r13
            r13 = r14
            r14 = r49
            goto Le74
        L3bd:
            r0 = move-exception
            goto L3d4
        L3bf:
            r0 = move-exception
            r4 = r51
            r9 = r54
            r1 = r0
            r2 = r3
            r34 = r12
            r13 = r14
            r14 = r15
            r3 = r29
            r15 = r52
            goto Le74
        L3d0:
            r0 = move-exception
            r9 = r13
            r13 = r52
        L3d4:
            android.content.ComponentName r1 = r11.name     // Catch: java.lang.Throwable -> L5cd
            java.lang.String r1 = r1.flattenToShortString()     // Catch: java.lang.Throwable -> L5cd
            r4 = r50
            r5 = r22
            r7 = r6
            r6 = r3
            r7 = r54
            r8 = r14
            r58 = r3
            r3 = r9
            r9 = r27
            r32 = r22
            r10 = r1
            r1 = r54
            r33 = r11
            r34 = r12
            r11 = r30
            r4.checkAssociationAndPermissionLocked(r5, r6, r7, r8, r9, r10, r11)     // Catch: java.lang.Throwable -> L5ba
            long r4 = android.os.Binder.clearCallingIdentity()     // Catch: java.lang.Throwable -> L5ba
            r11 = r4
            java.lang.String r4 = "getContentProviderImpl: incProviderCountLocked"
            r9 = r30
            r15.checkTime(r9, r4)     // Catch: java.lang.Throwable -> L58c
            r21 = 1
            com.android.server.am.ActivityManagerService r4 = r15.mService     // Catch: java.lang.Throwable -> L58c
            com.android.server.am.ProcessList r4 = r4.mProcessList     // Catch: java.lang.Throwable -> L58c
            r8 = r13
            r13 = r50
            r7 = r51
            r6 = r14
            r14 = r32
            r5 = r15
            r15 = r33
            r16 = r53
            r17 = r54
            r18 = r55
            r19 = r56
            r20 = r57
            r22 = r9
            r24 = r4
            com.android.server.am.ContentProviderConnection r4 = r13.incProviderCountLocked(r14, r15, r16, r17, r18, r19, r20, r21, r22, r24, r25)     // Catch: java.lang.Throwable -> L57e
            r14 = r5
            r5 = r4
            java.lang.String r4 = "getContentProviderImpl: before updateOomAdj"
            r14.checkTime(r9, r4)     // Catch: java.lang.Throwable -> L56f
            r13 = r33
            com.android.server.am.ProcessRecord r4 = r13.proc     // Catch: java.lang.Throwable -> L562
            com.android.server.am.ProcessStateRecord r4 = r4.mState     // Catch: java.lang.Throwable -> L562
            int r4 = r4.getVerifiedAdj()     // Catch: java.lang.Throwable -> L562
            r15 = r4
            com.android.server.am.ActivityManagerService r4 = r14.mService     // Catch: java.lang.Throwable -> L562
            com.android.server.am.ProcessRecord r2 = r13.proc     // Catch: java.lang.Throwable -> L562
            java.lang.String r3 = "updateOomAdj_getProvider"
            boolean r2 = r4.updateOomAdjLocked(r2, r3)     // Catch: java.lang.Throwable -> L562
            if (r2 == 0) goto L466
            com.android.server.am.ProcessRecord r3 = r13.proc     // Catch: java.lang.Throwable -> L458
            com.android.server.am.ProcessStateRecord r3 = r3.mState     // Catch: java.lang.Throwable -> L458
            int r3 = r3.getSetAdj()     // Catch: java.lang.Throwable -> L458
            if (r15 == r3) goto L466
            com.android.server.am.ProcessRecord r3 = r13.proc     // Catch: java.lang.Throwable -> L458
            boolean r3 = r14.isProcessAliveLocked(r3)     // Catch: java.lang.Throwable -> L458
            if (r3 != 0) goto L466
            r2 = 0
            goto L466
        L458:
            r0 = move-exception
            r1 = r0
            r26 = r5
            r36 = r6
            r37 = r9
            r3 = r29
            r35 = r32
            goto L5a7
        L466:
            android.content.pm.ProviderInfo r3 = r13.info     // Catch: java.lang.Throwable -> L562
            java.lang.String r3 = r3.packageName     // Catch: java.lang.Throwable -> L562
            r4 = r32
            r14.maybeUpdateProviderUsageStatsLocked(r4, r3, r8)     // Catch: java.lang.Throwable -> L555
            java.lang.String r3 = "getContentProviderImpl: after updateOomAdj"
            r14.checkTime(r9, r3)     // Catch: java.lang.Throwable -> L555
            boolean r3 = com.android.server.am.ActivityManagerDebugConfig.DEBUG_PROVIDER     // Catch: java.lang.Throwable -> L555
            if (r3 == 0) goto L4af
            java.lang.String r3 = "ContentProviderHelper"
            java.lang.StringBuilder r1 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L4a1
            r1.<init>()     // Catch: java.lang.Throwable -> L4a1
            r17 = r6
            java.lang.String r6 = "Adjust success: "
            java.lang.StringBuilder r1 = r1.append(r6)     // Catch: java.lang.Throwable -> L493
            java.lang.StringBuilder r1 = r1.append(r2)     // Catch: java.lang.Throwable -> L493
            java.lang.String r1 = r1.toString()     // Catch: java.lang.Throwable -> L493
            android.util.Slog.i(r3, r1)     // Catch: java.lang.Throwable -> L493
            goto L4b1
        L493:
            r0 = move-exception
            r1 = r0
            r35 = r4
            r26 = r5
            r37 = r9
            r36 = r17
            r3 = r29
            goto L5a7
        L4a1:
            r0 = move-exception
            r1 = r0
            r35 = r4
            r26 = r5
            r36 = r6
            r37 = r9
            r3 = r29
            goto L5a7
        L4af:
            r17 = r6
        L4b1:
            if (r2 != 0) goto L52a
            java.lang.String r1 = "ContentProviderHelper"
            java.lang.StringBuilder r3 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L51c
            r3.<init>()     // Catch: java.lang.Throwable -> L51c
            java.lang.String r6 = "Existing provider "
            java.lang.StringBuilder r3 = r3.append(r6)     // Catch: java.lang.Throwable -> L51c
            android.content.ComponentName r6 = r13.name     // Catch: java.lang.Throwable -> L51c
            java.lang.String r6 = r6.flattenToShortString()     // Catch: java.lang.Throwable -> L51c
            java.lang.StringBuilder r3 = r3.append(r6)     // Catch: java.lang.Throwable -> L51c
            java.lang.String r6 = " is crashing; detaching "
            java.lang.StringBuilder r3 = r3.append(r6)     // Catch: java.lang.Throwable -> L51c
            java.lang.StringBuilder r3 = r3.append(r4)     // Catch: java.lang.Throwable -> L51c
            java.lang.String r3 = r3.toString()     // Catch: java.lang.Throwable -> L51c
            android.util.Slog.wtf(r1, r3)     // Catch: java.lang.Throwable -> L51c
            r1 = 0
            r3 = 0
            r6 = r4
            r4 = r50
            r35 = r6
            r36 = r17
            r6 = r13
            r7 = r53
            r8 = r57
            r37 = r9
            r9 = r1
            r10 = r3
            boolean r1 = r4.decProviderCountLocked(r5, r6, r7, r8, r9, r10)     // Catch: java.lang.Throwable -> L54d
            if (r1 != 0) goto L50f
        L4f4:
            android.os.Binder.restoreCallingIdentity(r11)     // Catch: java.lang.Throwable -> L4fd
            monitor-exit(r34)     // Catch: java.lang.Throwable -> L4fd
            com.android.server.am.ActivityManagerService.resetPriorityAfterLockedSection()
            r3 = 0
            return r3
        L4fd:
            r0 = move-exception
            r4 = r51
            r15 = r52
            r9 = r54
            r2 = r58
            r1 = r0
            r26 = r5
            r3 = r29
            r13 = r36
            goto Le74
        L50f:
            r3 = 0
            r26 = 0
            com.android.server.am.ProcessRecord r4 = r13.proc     // Catch: java.lang.Throwable -> L518
            r1 = r4
            r28 = r1
            goto L543
        L518:
            r0 = move-exception
            r1 = r0
            goto L5a7
        L51c:
            r0 = move-exception
            r35 = r4
            r37 = r9
            r36 = r17
            r1 = r0
            r26 = r5
            r3 = r29
            goto L5a7
        L52a:
            r35 = r4
            r37 = r9
            r36 = r17
            com.android.server.am.ProcessRecord r1 = r13.proc     // Catch: java.lang.Throwable -> L54d
            com.android.server.am.ProcessStateRecord r1 = r1.mState     // Catch: java.lang.Throwable -> L54d
            com.android.server.am.ProcessRecord r3 = r13.proc     // Catch: java.lang.Throwable -> L54d
            com.android.server.am.ProcessStateRecord r3 = r3.mState     // Catch: java.lang.Throwable -> L54d
            int r3 = r3.getSetAdj()     // Catch: java.lang.Throwable -> L54d
            r1.setVerifiedAdj(r3)     // Catch: java.lang.Throwable -> L54d
            r26 = r5
            r3 = r29
        L543:
            android.os.Binder.restoreCallingIdentity(r11)     // Catch: java.lang.Throwable -> L5ac
            r2 = r58
            r15 = r3
            r12 = r28
            goto L607
        L54d:
            r0 = move-exception
            r1 = r0
            r26 = r5
            r3 = r29
            goto L5a7
        L555:
            r0 = move-exception
            r35 = r4
            r36 = r6
            r37 = r9
            r1 = r0
            r26 = r5
            r3 = r29
            goto L5a7
        L562:
            r0 = move-exception
            r36 = r6
            r37 = r9
            r35 = r32
            r1 = r0
            r26 = r5
            r3 = r29
            goto L5a7
        L56f:
            r0 = move-exception
            r36 = r6
            r37 = r9
            r35 = r32
            r13 = r33
            r1 = r0
            r26 = r5
            r3 = r29
            goto L5a7
        L57e:
            r0 = move-exception
            r14 = r5
            r36 = r6
            r37 = r9
            r35 = r32
            r13 = r33
            r1 = r0
            r3 = r29
            goto L5a7
        L58c:
            r0 = move-exception
            r37 = r9
            r36 = r14
            r14 = r15
            r35 = r32
            r13 = r33
            r1 = r0
            r3 = r29
            goto L5a7
        L59a:
            r0 = move-exception
            r36 = r14
            r14 = r15
            r37 = r30
            r35 = r32
            r13 = r33
            r1 = r0
            r3 = r29
        L5a7:
            android.os.Binder.restoreCallingIdentity(r11)     // Catch: java.lang.Throwable -> L5ac
            throw r1     // Catch: java.lang.Throwable -> L5ac
        L5ac:
            r0 = move-exception
            r4 = r51
            r15 = r52
            r9 = r54
            r2 = r58
            r1 = r0
            r13 = r36
            goto Le74
        L5ba:
            r0 = move-exception
            r36 = r14
            r14 = r15
            r4 = r51
            r15 = r52
            r9 = r54
            r2 = r58
            r1 = r0
            r3 = r29
            r13 = r36
            goto Le74
        L5cd:
            r0 = move-exception
            r58 = r3
            r34 = r12
            r36 = r14
            r14 = r15
            r4 = r51
            r15 = r52
            r9 = r54
            r2 = r58
            r1 = r0
            r3 = r29
            r13 = r36
            goto Le74
        L5e4:
            r0 = move-exception
            r34 = r12
            r36 = r14
            r14 = r15
            r4 = r51
            r15 = r52
            r9 = r54
            r1 = r0
            r2 = r3
            r3 = r29
            r13 = r36
            goto Le74
        L5f8:
            r35 = r10
            r13 = r11
            r34 = r12
            r36 = r14
            r14 = r15
            r37 = r20
            r2 = r3
            r12 = r28
            r15 = r29
        L607:
            if (r15 != 0) goto Lc0f
            java.lang.String r1 = "getContentProviderImpl: before resolveContentProvider"
            r10 = r37
            r14.checkTime(r10, r1)     // Catch: android.os.RemoteException -> L633 java.lang.Throwable -> L639
            android.content.pm.IPackageManager r1 = android.app.AppGlobals.getPackageManager()     // Catch: android.os.RemoteException -> L633 java.lang.Throwable -> L639
            r3 = 3072(0xc00, double:1.518E-320)
            r8 = r52
            r5 = r36
            android.content.pm.ProviderInfo r1 = r1.resolveContentProvider(r8, r3, r5)     // Catch: java.lang.Throwable -> L626 android.os.RemoteException -> L631
            r2 = r1
            java.lang.String r1 = "getContentProviderImpl: after resolveContentProvider"
            r14.checkTime(r10, r1)     // Catch: java.lang.Throwable -> L626 android.os.RemoteException -> L631
            r9 = r2
            goto L64f
        L626:
            r0 = move-exception
            r4 = r51
            r9 = r54
            r1 = r0
            r13 = r5
            r3 = r15
            r15 = r8
            goto Le74
        L631:
            r0 = move-exception
            goto L64e
        L633:
            r0 = move-exception
            r8 = r52
            r5 = r36
            goto L64e
        L639:
            r0 = move-exception
            r5 = r36
            r4 = r51
            r9 = r54
            r1 = r0
            r13 = r5
            r3 = r15
            r15 = r52
            goto Le74
        L647:
            r0 = move-exception
            r8 = r52
            r5 = r36
            r10 = r37
        L64e:
            r9 = r2
        L64f:
            if (r9 != 0) goto L663
            monitor-exit(r34)     // Catch: java.lang.Throwable -> L657
            com.android.server.am.ActivityManagerService.resetPriorityAfterLockedSection()
            r3 = 0
            return r3
        L657:
            r0 = move-exception
            r4 = r51
            r1 = r0
            r13 = r5
            r2 = r9
            r3 = r15
            r9 = r54
            r15 = r8
            goto Le74
        L663:
            r3 = 0
            com.android.server.am.ActivityManagerService r1 = r14.mService     // Catch: java.lang.Throwable -> Lbfd
            java.lang.String r2 = r9.processName     // Catch: java.lang.Throwable -> Lbfd
            android.content.pm.ApplicationInfo r4 = r9.applicationInfo     // Catch: java.lang.Throwable -> Lbfd
            java.lang.String r6 = r9.name     // Catch: java.lang.Throwable -> Lbfd
            int r7 = r9.flags     // Catch: java.lang.Throwable -> Lbfd
            boolean r1 = r1.isSingleton(r2, r4, r6, r7)     // Catch: java.lang.Throwable -> Lbfd
            if (r1 == 0) goto L68b
            com.android.server.am.ActivityManagerService r1 = r14.mService     // Catch: java.lang.Throwable -> L657
            r7 = r35
            if (r7 != 0) goto L67d
            r2 = r54
            goto L67f
        L67d:
            int r2 = r7.uid     // Catch: java.lang.Throwable -> L657
        L67f:
            android.content.pm.ApplicationInfo r4 = r9.applicationInfo     // Catch: java.lang.Throwable -> L657
            int r4 = r4.uid     // Catch: java.lang.Throwable -> L657
            boolean r1 = r1.isValidSingletonCall(r2, r4)     // Catch: java.lang.Throwable -> L657
            if (r1 == 0) goto L68d
            r1 = 1
            goto L68e
        L68b:
            r7 = r35
        L68d:
            r1 = 0
        L68e:
            r16 = r1
            if (r16 == 0) goto L695
            r1 = 0
            r6 = r1
            goto L696
        L695:
            r6 = r5
        L696:
            com.android.server.am.ActivityManagerService r1 = r14.mService     // Catch: java.lang.Throwable -> Lbe8
            android.content.pm.ApplicationInfo r2 = r9.applicationInfo     // Catch: java.lang.Throwable -> Lbe8
            android.content.pm.ApplicationInfo r1 = r1.getAppInfoForUser(r2, r6)     // Catch: java.lang.Throwable -> Lbe8
            r9.applicationInfo = r1     // Catch: java.lang.Throwable -> Lbe8
            java.lang.String r1 = "getContentProviderImpl: got app info for user"
            r14.checkTime(r10, r1)     // Catch: java.lang.Throwable -> Lbe8
            if (r16 != 0) goto L6aa
            r17 = 1
            goto L6ac
        L6aa:
            r17 = 0
        L6ac:
            r5 = r54
            r1 = r50
            r4 = 0
            r2 = r7
            r58 = r15
            r15 = r3
            r3 = r9
            r15 = r4
            r4 = r54
            r5 = r6
            r33 = r13
            r13 = r6
            r6 = r17
            r39 = r7
            r7 = r52
            r15 = r9
            r8 = r10
            r1.checkAssociationAndPermissionLocked(r2, r3, r4, r5, r6, r7, r8)     // Catch: java.lang.Throwable -> Lbd6
            com.android.server.am.ActivityManagerService r1 = r14.mService     // Catch: java.lang.Throwable -> Lbd6
            boolean r1 = r1.mProcessesReady     // Catch: java.lang.Throwable -> Lbd6
            if (r1 != 0) goto L6ef
            java.lang.String r1 = r15.processName     // Catch: java.lang.Throwable -> L6e2
            java.lang.String r2 = "system"
            boolean r1 = r1.equals(r2)     // Catch: java.lang.Throwable -> L6e2
            if (r1 == 0) goto L6da
            goto L6ef
        L6da:
            java.lang.IllegalArgumentException r1 = new java.lang.IllegalArgumentException     // Catch: java.lang.Throwable -> L6e2
            java.lang.String r2 = "Attempt to launch content provider before system ready"
            r1.<init>(r2)     // Catch: java.lang.Throwable -> L6e2
            throw r1     // Catch: java.lang.Throwable -> L6e2
        L6e2:
            r0 = move-exception
            r4 = r51
            r9 = r54
            r3 = r58
            r1 = r0
            r2 = r15
            r15 = r52
            goto Le74
        L6ef:
            monitor-enter(r50)     // Catch: java.lang.Throwable -> Lbd6
            boolean r1 = r14.mSystemProvidersInstalled     // Catch: java.lang.Throwable -> Lbb5
            if (r1 != 0) goto L739
            android.content.pm.ApplicationInfo r1 = r15.applicationInfo     // Catch: java.lang.Throwable -> L729
            boolean r1 = r1.isSystemApp()     // Catch: java.lang.Throwable -> L729
            if (r1 == 0) goto L739
            java.lang.String r1 = "system"
            java.lang.String r2 = r15.processName     // Catch: java.lang.Throwable -> L729
            boolean r1 = r1.equals(r2)     // Catch: java.lang.Throwable -> L729
            if (r1 != 0) goto L708
            goto L739
        L708:
            java.lang.IllegalStateException r1 = new java.lang.IllegalStateException     // Catch: java.lang.Throwable -> L729
            java.lang.StringBuilder r2 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L729
            r2.<init>()     // Catch: java.lang.Throwable -> L729
            java.lang.String r3 = "Cannot access system provider: '"
            java.lang.StringBuilder r2 = r2.append(r3)     // Catch: java.lang.Throwable -> L729
            java.lang.String r3 = r15.authority     // Catch: java.lang.Throwable -> L729
            java.lang.StringBuilder r2 = r2.append(r3)     // Catch: java.lang.Throwable -> L729
            java.lang.String r3 = "' before system providers are installed!"
            java.lang.StringBuilder r2 = r2.append(r3)     // Catch: java.lang.Throwable -> L729
            java.lang.String r2 = r2.toString()     // Catch: java.lang.Throwable -> L729
            r1.<init>(r2)     // Catch: java.lang.Throwable -> L729
            throw r1     // Catch: java.lang.Throwable -> L729
        L729:
            r0 = move-exception
            r1 = r0
            r37 = r10
            r29 = r12
            r30 = r13
            r24 = r15
            r19 = r39
            r15 = r52
            goto Lbc3
        L739:
            monitor-exit(r50)     // Catch: java.lang.Throwable -> Lbb5
            com.android.server.am.ActivityManagerService r1 = r14.mService     // Catch: java.lang.Throwable -> Lbd6
            com.android.server.am.UserController r1 = r1.mUserController     // Catch: java.lang.Throwable -> Lbd6
            r2 = 0
            boolean r1 = r1.isUserRunning(r13, r2)     // Catch: java.lang.Throwable -> Lbd6
            if (r1 != 0) goto L791
            java.lang.String r1 = "ContentProviderHelper"
            java.lang.StringBuilder r2 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L6e2
            r2.<init>()     // Catch: java.lang.Throwable -> L6e2
            java.lang.String r3 = "Unable to launch app "
            java.lang.StringBuilder r2 = r2.append(r3)     // Catch: java.lang.Throwable -> L6e2
            android.content.pm.ApplicationInfo r3 = r15.applicationInfo     // Catch: java.lang.Throwable -> L6e2
            java.lang.String r3 = r3.packageName     // Catch: java.lang.Throwable -> L6e2
            java.lang.StringBuilder r2 = r2.append(r3)     // Catch: java.lang.Throwable -> L6e2
            java.lang.String r3 = "/"
            java.lang.StringBuilder r2 = r2.append(r3)     // Catch: java.lang.Throwable -> L6e2
            android.content.pm.ApplicationInfo r3 = r15.applicationInfo     // Catch: java.lang.Throwable -> L6e2
            int r3 = r3.uid     // Catch: java.lang.Throwable -> L6e2
            java.lang.StringBuilder r2 = r2.append(r3)     // Catch: java.lang.Throwable -> L6e2
            java.lang.String r3 = " for provider "
            java.lang.StringBuilder r2 = r2.append(r3)     // Catch: java.lang.Throwable -> L6e2
            r9 = r52
            java.lang.StringBuilder r2 = r2.append(r9)     // Catch: java.lang.Throwable -> L7b8
            java.lang.String r3 = ": user "
            java.lang.StringBuilder r2 = r2.append(r3)     // Catch: java.lang.Throwable -> L7b8
            java.lang.StringBuilder r2 = r2.append(r13)     // Catch: java.lang.Throwable -> L7b8
            java.lang.String r3 = " is stopped"
            java.lang.StringBuilder r2 = r2.append(r3)     // Catch: java.lang.Throwable -> L7b8
            java.lang.String r2 = r2.toString()     // Catch: java.lang.Throwable -> L7b8
            android.util.Slog.w(r1, r2)     // Catch: java.lang.Throwable -> L7b8
            monitor-exit(r34)     // Catch: java.lang.Throwable -> L7b8
            com.android.server.am.ActivityManagerService.resetPriorityAfterLockedSection()
            r1 = 0
            return r1
        L791:
            r9 = r52
            android.content.ComponentName r1 = new android.content.ComponentName     // Catch: java.lang.Throwable -> Lbae
            java.lang.String r2 = r15.packageName     // Catch: java.lang.Throwable -> Lbae
            java.lang.String r3 = r15.name     // Catch: java.lang.Throwable -> Lbae
            r1.<init>(r2, r3)     // Catch: java.lang.Throwable -> Lbae
            r8 = r1
            java.lang.String r1 = "getContentProviderImpl: before getProviderByClass"
            r14.checkTime(r10, r1)     // Catch: java.lang.Throwable -> Lbae
            com.android.server.am.ProviderMap r1 = r14.mProviderMap     // Catch: java.lang.Throwable -> Lbae
            com.android.server.am.ContentProviderRecord r1 = r1.getProviderByClass(r8, r13)     // Catch: java.lang.Throwable -> Lbae
            java.lang.String r2 = "getContentProviderImpl: after getProviderByClass"
            r14.checkTime(r10, r2)     // Catch: java.lang.Throwable -> Lbae
            if (r1 == 0) goto L7c4
            com.android.server.am.ProcessRecord r2 = r1.proc     // Catch: java.lang.Throwable -> L7b8
            if (r12 != r2) goto L7b6
            if (r12 == 0) goto L7b6
            goto L7c4
        L7b6:
            r2 = 0
            goto L7c5
        L7b8:
            r0 = move-exception
            r4 = r51
            r3 = r58
            r1 = r0
            r2 = r15
            r15 = r9
            r9 = r54
            goto Le74
        L7c4:
            r2 = 1
        L7c5:
            r28 = r2
            if (r28 == 0) goto L851
            long r2 = android.os.Binder.clearCallingIdentity()     // Catch: java.lang.Throwable -> L7b8
            com.android.server.am.ActivityManagerService r4 = r14.mService     // Catch: java.lang.Throwable -> L7b8
            android.content.Context r4 = r4.mContext     // Catch: java.lang.Throwable -> L7b8
            r7 = r39
            boolean r4 = r14.requestTargetProviderPermissionsReviewIfNeededLocked(r15, r7, r13, r4)     // Catch: java.lang.Throwable -> L7b8
            if (r4 != 0) goto L7df
            monitor-exit(r34)     // Catch: java.lang.Throwable -> L7b8
            com.android.server.am.ActivityManagerService.resetPriorityAfterLockedSection()
            r4 = 0
            return r4
        L7df:
            java.lang.String r4 = "getContentProviderImpl: before getApplicationInfo"
            r14.checkTime(r10, r4)     // Catch: java.lang.Throwable -> L841 android.os.RemoteException -> L84a
            android.content.pm.IPackageManager r4 = android.app.AppGlobals.getPackageManager()     // Catch: java.lang.Throwable -> L841 android.os.RemoteException -> L84a
            android.content.pm.ApplicationInfo r5 = r15.applicationInfo     // Catch: java.lang.Throwable -> L841 android.os.RemoteException -> L84a
            java.lang.String r5 = r5.packageName     // Catch: java.lang.Throwable -> L841 android.os.RemoteException -> L84a
            r32 = r7
            r6 = 1024(0x400, double:5.06E-321)
            android.content.pm.ApplicationInfo r4 = r4.getApplicationInfo(r5, r6, r13)     // Catch: java.lang.Throwable -> L83c android.os.RemoteException -> L83f
            java.lang.String r5 = "getContentProviderImpl: after getApplicationInfo"
            r14.checkTime(r10, r5)     // Catch: java.lang.Throwable -> L83c android.os.RemoteException -> L83f
            if (r4 != 0) goto L81f
            java.lang.String r5 = "ContentProviderHelper"
            java.lang.StringBuilder r6 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L83c android.os.RemoteException -> L83f
            r6.<init>()     // Catch: java.lang.Throwable -> L83c android.os.RemoteException -> L83f
            java.lang.String r7 = "No package info for content provider "
            java.lang.StringBuilder r6 = r6.append(r7)     // Catch: java.lang.Throwable -> L83c android.os.RemoteException -> L83f
            java.lang.String r7 = r15.name     // Catch: java.lang.Throwable -> L83c android.os.RemoteException -> L83f
            java.lang.StringBuilder r6 = r6.append(r7)     // Catch: java.lang.Throwable -> L83c android.os.RemoteException -> L83f
            java.lang.String r6 = r6.toString()     // Catch: java.lang.Throwable -> L83c android.os.RemoteException -> L83f
            android.util.Slog.w(r5, r6)     // Catch: java.lang.Throwable -> L83c android.os.RemoteException -> L83f
            android.os.Binder.restoreCallingIdentity(r2)     // Catch: java.lang.Throwable -> L7b8
            monitor-exit(r34)     // Catch: java.lang.Throwable -> L7b8
            com.android.server.am.ActivityManagerService.resetPriorityAfterLockedSection()
            r5 = 0
            return r5
        L81f:
            com.android.server.am.ActivityManagerService r5 = r14.mService     // Catch: java.lang.Throwable -> L83c android.os.RemoteException -> L83f
            android.content.pm.ApplicationInfo r22 = r5.getAppInfoForUser(r4, r13)     // Catch: java.lang.Throwable -> L83c android.os.RemoteException -> L83f
            com.android.server.am.ContentProviderRecord r4 = new com.android.server.am.ContentProviderRecord     // Catch: java.lang.Throwable -> L83c android.os.RemoteException -> L83f
            com.android.server.am.ActivityManagerService r5 = r14.mService     // Catch: java.lang.Throwable -> L83c android.os.RemoteException -> L83f
            r19 = r4
            r20 = r5
            r21 = r15
            r23 = r8
            r24 = r16
            r19.<init>(r20, r21, r22, r23, r24)     // Catch: java.lang.Throwable -> L83c android.os.RemoteException -> L83f
            r1 = r4
            android.os.Binder.restoreCallingIdentity(r2)     // Catch: java.lang.Throwable -> L7b8
            r7 = r1
            goto L854
        L83c:
            r0 = move-exception
            r4 = r0
            goto L845
        L83f:
            r0 = move-exception
            goto L84d
        L841:
            r0 = move-exception
            r32 = r7
            r4 = r0
        L845:
            android.os.Binder.restoreCallingIdentity(r2)     // Catch: java.lang.Throwable -> L7b8
            throw r4     // Catch: java.lang.Throwable -> L7b8
        L84a:
            r0 = move-exception
            r32 = r7
        L84d:
            android.os.Binder.restoreCallingIdentity(r2)     // Catch: java.lang.Throwable -> L7b8
            goto L853
        L851:
            r32 = r39
        L853:
            r7 = r1
        L854:
            java.lang.String r1 = "getContentProviderImpl: now have ContentProviderRecord"
            r14.checkTime(r10, r1)     // Catch: java.lang.Throwable -> Lbae
            if (r32 == 0) goto L870
            r6 = r32
            boolean r1 = r7.canRunHere(r6)     // Catch: java.lang.Throwable -> L7b8
            if (r1 == 0) goto L86e
            r1 = 0
            r5 = 1
            android.app.ContentProviderHolder r1 = r7.newHolder(r1, r5)     // Catch: java.lang.Throwable -> L7b8
            monitor-exit(r34)     // Catch: java.lang.Throwable -> L7b8
            com.android.server.am.ActivityManagerService.resetPriorityAfterLockedSection()
            return r1
        L86e:
            r5 = 1
            goto L873
        L870:
            r6 = r32
            r5 = 1
        L873:
            boolean r1 = com.android.server.am.ActivityManagerDebugConfig.DEBUG_PROVIDER     // Catch: java.lang.Throwable -> Lbae
            if (r1 == 0) goto L8c4
            java.lang.String r1 = "ContentProviderHelper"
            java.lang.StringBuilder r2 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L7b8
            r2.<init>()     // Catch: java.lang.Throwable -> L7b8
            java.lang.String r3 = "LAUNCHING REMOTE PROVIDER (myuid "
            java.lang.StringBuilder r2 = r2.append(r3)     // Catch: java.lang.Throwable -> L7b8
            if (r6 == 0) goto L88d
            int r3 = r6.uid     // Catch: java.lang.Throwable -> L7b8
            java.lang.Integer r3 = java.lang.Integer.valueOf(r3)     // Catch: java.lang.Throwable -> L7b8
            goto L88e
        L88d:
            r3 = 0
        L88e:
            java.lang.StringBuilder r2 = r2.append(r3)     // Catch: java.lang.Throwable -> L7b8
            java.lang.String r3 = " pruid "
            java.lang.StringBuilder r2 = r2.append(r3)     // Catch: java.lang.Throwable -> L7b8
            android.content.pm.ApplicationInfo r3 = r7.appInfo     // Catch: java.lang.Throwable -> L7b8
            int r3 = r3.uid     // Catch: java.lang.Throwable -> L7b8
            java.lang.StringBuilder r2 = r2.append(r3)     // Catch: java.lang.Throwable -> L7b8
            java.lang.String r3 = "): "
            java.lang.StringBuilder r2 = r2.append(r3)     // Catch: java.lang.Throwable -> L7b8
            android.content.pm.ProviderInfo r3 = r7.info     // Catch: java.lang.Throwable -> L7b8
            java.lang.String r3 = r3.name     // Catch: java.lang.Throwable -> L7b8
            java.lang.StringBuilder r2 = r2.append(r3)     // Catch: java.lang.Throwable -> L7b8
            java.lang.String r3 = " callers="
            java.lang.StringBuilder r2 = r2.append(r3)     // Catch: java.lang.Throwable -> L7b8
            r3 = 6
            java.lang.String r3 = android.os.Debug.getCallers(r3)     // Catch: java.lang.Throwable -> L7b8
            java.lang.StringBuilder r2 = r2.append(r3)     // Catch: java.lang.Throwable -> L7b8
            java.lang.String r2 = r2.toString()     // Catch: java.lang.Throwable -> L7b8
            android.util.Slog.w(r1, r2)     // Catch: java.lang.Throwable -> L7b8
        L8c4:
            java.util.ArrayList<com.android.server.am.ContentProviderRecord> r1 = r14.mLaunchingProviders     // Catch: java.lang.Throwable -> Lbae
            int r1 = r1.size()     // Catch: java.lang.Throwable -> Lbae
            r4 = r1
            r1 = 0
            r3 = r1
        L8cd:
            if (r3 >= r4) goto L8db
            java.util.ArrayList<com.android.server.am.ContentProviderRecord> r1 = r14.mLaunchingProviders     // Catch: java.lang.Throwable -> L7b8
            java.lang.Object r1 = r1.get(r3)     // Catch: java.lang.Throwable -> L7b8
            if (r1 != r7) goto L8d8
            goto L8db
        L8d8:
            int r3 = r3 + 1
            goto L8cd
        L8db:
            if (r3 < r4) goto Lb27
            long r1 = android.os.Binder.clearCallingIdentity()     // Catch: java.lang.Throwable -> Lb19
            r19 = r1
            android.content.pm.ApplicationInfo r1 = r7.appInfo     // Catch: java.lang.Throwable -> Lb08
            java.lang.String r1 = r1.packageName     // Catch: java.lang.Throwable -> Lb08
            r2 = r55
            boolean r1 = android.text.TextUtils.equals(r1, r2)     // Catch: java.lang.Throwable -> Lb08
            if (r1 != 0) goto L8fc
            com.android.server.am.ActivityManagerService r1 = r14.mService     // Catch: java.lang.Throwable -> L919
            android.app.usage.UsageStatsManagerInternal r1 = r1.mUsageStatsService     // Catch: java.lang.Throwable -> L919
            android.content.pm.ApplicationInfo r5 = r7.appInfo     // Catch: java.lang.Throwable -> L919
            java.lang.String r5 = r5.packageName     // Catch: java.lang.Throwable -> L919
            r2 = 31
            r1.reportEvent(r5, r13, r2)     // Catch: java.lang.Throwable -> L919
        L8fc:
            java.lang.String r1 = "getContentProviderImpl: before set stopped state"
            r14.checkTime(r10, r1)     // Catch: android.os.RemoteException -> L915 java.lang.Throwable -> L919 java.lang.IllegalArgumentException -> L927
            android.content.pm.IPackageManager r1 = android.app.AppGlobals.getPackageManager()     // Catch: android.os.RemoteException -> L915 java.lang.Throwable -> L919 java.lang.IllegalArgumentException -> L927
            android.content.pm.ApplicationInfo r2 = r7.appInfo     // Catch: android.os.RemoteException -> L915 java.lang.Throwable -> L919 java.lang.IllegalArgumentException -> L927
            java.lang.String r2 = r2.packageName     // Catch: android.os.RemoteException -> L915 java.lang.Throwable -> L919 java.lang.IllegalArgumentException -> L927
            r5 = 0
            r1.setPackageStoppedState(r2, r5, r13)     // Catch: android.os.RemoteException -> L915 java.lang.Throwable -> L919 java.lang.IllegalArgumentException -> L927
            java.lang.String r1 = "getContentProviderImpl: after set stopped state"
            r14.checkTime(r10, r1)     // Catch: android.os.RemoteException -> L915 java.lang.Throwable -> L919 java.lang.IllegalArgumentException -> L927
            r21 = r3
            goto L970
        L915:
            r0 = move-exception
            r21 = r3
            goto L96f
        L919:
            r0 = move-exception
            r1 = r0
            r21 = r3
            r22 = r4
            r24 = r6
            r30 = r12
            r31 = r13
            goto Lb14
        L927:
            r0 = move-exception
            r1 = r0
            java.lang.String r2 = "ContentProviderHelper"
            java.lang.StringBuilder r5 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L95e
            r5.<init>()     // Catch: java.lang.Throwable -> L95e
            r21 = r3
            java.lang.String r3 = "Failed trying to unstop package "
            java.lang.StringBuilder r3 = r5.append(r3)     // Catch: java.lang.Throwable -> L952
            android.content.pm.ApplicationInfo r5 = r7.appInfo     // Catch: java.lang.Throwable -> L952
            java.lang.String r5 = r5.packageName     // Catch: java.lang.Throwable -> L952
            java.lang.StringBuilder r3 = r3.append(r5)     // Catch: java.lang.Throwable -> L952
            java.lang.String r5 = ": "
            java.lang.StringBuilder r3 = r3.append(r5)     // Catch: java.lang.Throwable -> L952
            java.lang.StringBuilder r3 = r3.append(r1)     // Catch: java.lang.Throwable -> L952
            java.lang.String r3 = r3.toString()     // Catch: java.lang.Throwable -> L952
            android.util.Slog.w(r2, r3)     // Catch: java.lang.Throwable -> L952
            goto L970
        L952:
            r0 = move-exception
            r1 = r0
            r22 = r4
            r24 = r6
            r30 = r12
            r31 = r13
            goto Lb14
        L95e:
            r0 = move-exception
            r1 = r0
            r21 = r3
            r22 = r4
            r24 = r6
            r30 = r12
            r31 = r13
            goto Lb14
        L96c:
            r0 = move-exception
            r21 = r3
        L96f:
        L970:
            java.lang.String r1 = "getContentProviderImpl: looking for process record"
            r14.checkTime(r10, r1)     // Catch: java.lang.Throwable -> Lafd
            com.android.server.am.ActivityManagerService r1 = r14.mService     // Catch: java.lang.Throwable -> Lafd
            java.lang.String r2 = r15.processName     // Catch: java.lang.Throwable -> Lafd
            android.content.pm.ApplicationInfo r3 = r7.appInfo     // Catch: java.lang.Throwable -> Lafd
            int r3 = r3.uid     // Catch: java.lang.Throwable -> Lafd
            com.android.server.am.ProcessRecord r1 = r1.getProcessRecordLocked(r2, r3)     // Catch: java.lang.Throwable -> Lafd
            r5 = r1
            if (r5 == 0) goto L9e5
            android.app.IApplicationThread r1 = r5.getThread()     // Catch: java.lang.Throwable -> L9d9
            r2 = r1
            if (r1 == 0) goto L9d6
            boolean r1 = r5.isKilled()     // Catch: java.lang.Throwable -> L9d9
            if (r1 != 0) goto L9d3
            boolean r1 = com.android.server.am.ActivityManagerDebugConfig.DEBUG_PROVIDER     // Catch: java.lang.Throwable -> L9d9
            if (r1 == 0) goto L9b0
            java.lang.String r1 = "ContentProviderHelper"
            java.lang.StringBuilder r3 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L9d9
            r3.<init>()     // Catch: java.lang.Throwable -> L9d9
            r22 = r4
            java.lang.String r4 = "Installing in existing process "
            java.lang.StringBuilder r3 = r3.append(r4)     // Catch: java.lang.Throwable -> L9f1
            java.lang.StringBuilder r3 = r3.append(r5)     // Catch: java.lang.Throwable -> L9f1
            java.lang.String r3 = r3.toString()     // Catch: java.lang.Throwable -> L9f1
            android.util.Slog.d(r1, r3)     // Catch: java.lang.Throwable -> L9f1
            goto L9b2
        L9b0:
            r22 = r4
        L9b2:
            com.android.server.am.ProcessProviderRecord r1 = r5.mProviders     // Catch: java.lang.Throwable -> L9f1
            java.lang.String r3 = r15.name     // Catch: java.lang.Throwable -> L9f1
            boolean r3 = r1.hasProvider(r3)     // Catch: java.lang.Throwable -> L9f1
            if (r3 != 0) goto L9cb
            java.lang.String r3 = "getContentProviderImpl: scheduling install"
            r14.checkTime(r10, r3)     // Catch: java.lang.Throwable -> L9f1
            java.lang.String r3 = r15.name     // Catch: java.lang.Throwable -> L9f1
            r1.installProvider(r3, r7)     // Catch: java.lang.Throwable -> L9f1
            r2.scheduleInstallProvider(r15)     // Catch: android.os.RemoteException -> L9ca java.lang.Throwable -> L9f1
            goto L9cb
        L9ca:
            r0 = move-exception
        L9cb:
            r24 = r6
            r30 = r12
            r31 = r13
            goto Lad0
        L9d3:
            r22 = r4
            goto L9e7
        L9d6:
            r22 = r4
            goto L9e7
        L9d9:
            r0 = move-exception
            r1 = r0
            r22 = r4
            r24 = r6
            r30 = r12
            r31 = r13
            goto Lb14
        L9e5:
            r22 = r4
        L9e7:
            com.transsion.hubcore.server.am.ITranActivityManagerService r1 = com.transsion.hubcore.server.am.ITranActivityManagerService.Instance()     // Catch: java.lang.Throwable -> Laf4
            if (r6 == 0) goto L9fb
            com.android.server.am.ProcessRecord$MyProcessWrapper r2 = r6.processWrapper     // Catch: java.lang.Throwable -> L9f1
            r3 = r2
            goto L9fc
        L9f1:
            r0 = move-exception
            r1 = r0
            r24 = r6
            r30 = r12
            r31 = r13
            goto Lb14
        L9fb:
            r3 = 0
        L9fc:
            r2 = r7
            r4 = r52
            r23 = r5
            r5 = r6
            r24 = r6
            r6 = r15
            boolean r1 = r1.limitStartProcessForProvider(r2, r3, r4, r5, r6)     // Catch: java.lang.Throwable -> Laed
            if (r1 == 0) goto La15
        La0c:
            android.os.Binder.restoreCallingIdentity(r19)     // Catch: java.lang.Throwable -> L7b8
            monitor-exit(r34)     // Catch: java.lang.Throwable -> L7b8
            com.android.server.am.ActivityManagerService.resetPriorityAfterLockedSection()
            r1 = 0
            return r1
        La15:
            java.lang.String r1 = "getContentProviderImpl: before start process"
            r14.checkTime(r10, r1)     // Catch: java.lang.Throwable -> Laed
            com.android.server.am.ActivityManagerService r1 = r14.mService     // Catch: java.lang.Throwable -> Laed
            com.mediatek.server.am.AmsExt r1 = r1.mAmsExt     // Catch: java.lang.Throwable -> Laed
            android.content.pm.ApplicationInfo r2 = r15.applicationInfo     // Catch: java.lang.Throwable -> Laed
            java.lang.String r2 = r2.packageName     // Catch: java.lang.Throwable -> Laed
            java.lang.String r3 = "content provider"
            java.lang.String r4 = r15.name     // Catch: java.lang.Throwable -> Laed
            r6 = r54
            java.lang.String r1 = r1.onReadyToStartComponent(r2, r6, r3, r4)     // Catch: java.lang.Throwable -> Laed
            if (r1 == 0) goto La50
            java.lang.String r2 = "skipped"
            boolean r2 = r1.equals(r2)     // Catch: java.lang.Throwable -> La48
            if (r2 == 0) goto La50
            java.lang.String r2 = "ContentProviderHelper"
            java.lang.String r3 = "suppress app launched for content provider"
            android.util.Slog.d(r2, r3)     // Catch: java.lang.Throwable -> La48
            r2 = 0
            r29 = r1
            r5 = r2
            r30 = r12
            r31 = r13
            goto La85
        La48:
            r0 = move-exception
            r1 = r0
            r30 = r12
            r31 = r13
            goto Lb14
        La50:
            com.android.server.am.ActivityManagerService r2 = r14.mService     // Catch: java.lang.Throwable -> Laed
            java.lang.String r3 = r15.processName     // Catch: java.lang.Throwable -> Laed
            android.content.pm.ApplicationInfo r4 = r7.appInfo     // Catch: java.lang.Throwable -> Laed
            r43 = 0
            r44 = 0
            com.android.server.am.HostingRecord r5 = new com.android.server.am.HostingRecord     // Catch: java.lang.Throwable -> Laed
            r29 = r1
            java.lang.String r1 = "content provider"
            android.content.ComponentName r6 = new android.content.ComponentName     // Catch: java.lang.Throwable -> Laed
            r30 = r12
            android.content.pm.ApplicationInfo r12 = r15.applicationInfo     // Catch: java.lang.Throwable -> Lae8
            java.lang.String r12 = r12.packageName     // Catch: java.lang.Throwable -> Lae8
            r31 = r13
            java.lang.String r13 = r15.name     // Catch: java.lang.Throwable -> Lae5
            r6.<init>(r12, r13)     // Catch: java.lang.Throwable -> Lae5
            r5.<init>(r1, r6)     // Catch: java.lang.Throwable -> Lae5
            r46 = 0
            r47 = 0
            r48 = 0
            r40 = r2
            r41 = r3
            r42 = r4
            r45 = r5
            com.android.server.am.ProcessRecord r1 = r40.startProcessLocked(r41, r42, r43, r44, r45, r46, r47, r48)     // Catch: java.lang.Throwable -> Lae5
            r5 = r1
        La85:
            java.lang.String r1 = "getContentProviderImpl: after start process"
            r14.checkTime(r10, r1)     // Catch: java.lang.Throwable -> Lae5
            if (r5 != 0) goto Lad0
            java.lang.String r1 = "ContentProviderHelper"
            java.lang.StringBuilder r2 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> Lae5
            r2.<init>()     // Catch: java.lang.Throwable -> Lae5
            java.lang.String r3 = "Unable to launch app "
            java.lang.StringBuilder r2 = r2.append(r3)     // Catch: java.lang.Throwable -> Lae5
            android.content.pm.ApplicationInfo r3 = r15.applicationInfo     // Catch: java.lang.Throwable -> Lae5
            java.lang.String r3 = r3.packageName     // Catch: java.lang.Throwable -> Lae5
            java.lang.StringBuilder r2 = r2.append(r3)     // Catch: java.lang.Throwable -> Lae5
            java.lang.String r3 = "/"
            java.lang.StringBuilder r2 = r2.append(r3)     // Catch: java.lang.Throwable -> Lae5
            android.content.pm.ApplicationInfo r3 = r15.applicationInfo     // Catch: java.lang.Throwable -> Lae5
            int r3 = r3.uid     // Catch: java.lang.Throwable -> Lae5
            java.lang.StringBuilder r2 = r2.append(r3)     // Catch: java.lang.Throwable -> Lae5
            java.lang.String r3 = " for provider "
            java.lang.StringBuilder r2 = r2.append(r3)     // Catch: java.lang.Throwable -> Lae5
            java.lang.StringBuilder r2 = r2.append(r9)     // Catch: java.lang.Throwable -> Lae5
            java.lang.String r3 = ": process is bad"
            java.lang.StringBuilder r2 = r2.append(r3)     // Catch: java.lang.Throwable -> Lae5
            java.lang.String r2 = r2.toString()     // Catch: java.lang.Throwable -> Lae5
            android.util.Slog.w(r1, r2)     // Catch: java.lang.Throwable -> Lae5
            android.os.Binder.restoreCallingIdentity(r19)     // Catch: java.lang.Throwable -> Lb3e
            monitor-exit(r34)     // Catch: java.lang.Throwable -> Lb3e
            com.android.server.am.ActivityManagerService.resetPriorityAfterLockedSection()
            r1 = 0
            return r1
        Lad0:
            r7.launchingApp = r5     // Catch: java.lang.Throwable -> Lae5
            com.transsion.hubcore.server.am.ITranActivityManagerService r1 = com.transsion.hubcore.server.am.ITranActivityManagerService.Instance()     // Catch: java.lang.Throwable -> Lae5
            com.android.server.am.ProcessRecord r2 = r7.launchingApp     // Catch: java.lang.Throwable -> Lae5
            r1.onProviderAdd(r2)     // Catch: java.lang.Throwable -> Lae5
            java.util.ArrayList<com.android.server.am.ContentProviderRecord> r1 = r14.mLaunchingProviders     // Catch: java.lang.Throwable -> Lae5
            r1.add(r7)     // Catch: java.lang.Throwable -> Lae5
            android.os.Binder.restoreCallingIdentity(r19)     // Catch: java.lang.Throwable -> Lb3e
            goto Lb31
        Lae5:
            r0 = move-exception
            r1 = r0
            goto Lb14
        Lae8:
            r0 = move-exception
            r31 = r13
            r1 = r0
            goto Lb14
        Laed:
            r0 = move-exception
            r30 = r12
            r31 = r13
            r1 = r0
            goto Lb14
        Laf4:
            r0 = move-exception
            r24 = r6
            r30 = r12
            r31 = r13
            r1 = r0
            goto Lb14
        Lafd:
            r0 = move-exception
            r22 = r4
            r24 = r6
            r30 = r12
            r31 = r13
            r1 = r0
            goto Lb14
        Lb08:
            r0 = move-exception
            r21 = r3
            r22 = r4
            r24 = r6
            r30 = r12
            r31 = r13
            r1 = r0
        Lb14:
            android.os.Binder.restoreCallingIdentity(r19)     // Catch: java.lang.Throwable -> Lb3e
            throw r1     // Catch: java.lang.Throwable -> Lb3e
        Lb19:
            r0 = move-exception
            r31 = r13
            r4 = r51
            r3 = r58
            r1 = r0
            r2 = r15
            r15 = r9
            r9 = r54
            goto Le74
        Lb27:
            r21 = r3
            r22 = r4
            r24 = r6
            r30 = r12
            r31 = r13
        Lb31:
            java.lang.String r1 = "getContentProviderImpl: updating data structures"
            r14.checkTime(r10, r1)     // Catch: java.lang.Throwable -> Lb9b
            if (r28 == 0) goto Lb4c
            com.android.server.am.ProviderMap r1 = r14.mProviderMap     // Catch: java.lang.Throwable -> Lb3e
            r1.putProviderByClass(r8, r7)     // Catch: java.lang.Throwable -> Lb3e
            goto Lb4c
        Lb3e:
            r0 = move-exception
            r4 = r51
            r3 = r58
            r1 = r0
            r2 = r15
            r13 = r31
            r15 = r9
            r9 = r54
            goto Le74
        Lb4c:
            com.android.server.am.ProviderMap r1 = r14.mProviderMap     // Catch: java.lang.Throwable -> Lb9b
            r1.putProviderByName(r9, r7)     // Catch: java.lang.Throwable -> Lb9b
            r12 = 0
            com.android.server.am.ActivityManagerService r1 = r14.mService     // Catch: java.lang.Throwable -> Lb9b
            com.android.server.am.ProcessList r13 = r1.mProcessList     // Catch: java.lang.Throwable -> Lb9b
            r1 = r50
            r2 = r24
            r3 = r7
            r4 = r53
            r5 = r54
            r6 = r55
            r20 = r7
            r19 = r24
            r7 = r56
            r23 = r8
            r8 = r57
            r24 = r15
            r15 = r9
            r9 = r12
            r37 = r10
            r29 = r30
            r12 = r13
            r30 = r31
            r13 = r25
            com.android.server.am.ContentProviderConnection r1 = r1.incProviderCountLocked(r2, r3, r4, r5, r6, r7, r8, r9, r10, r12, r13)     // Catch: java.lang.Throwable -> Lbc5
            if (r1 == 0) goto Lb92
            r3 = 1
            r1.waiting = r3     // Catch: java.lang.Throwable -> Lb82
            goto Lb93
        Lb82:
            r0 = move-exception
            r4 = r51
            r9 = r54
            r3 = r58
            r26 = r1
            r2 = r24
            r13 = r30
            r1 = r0
            goto Le74
        Lb92:
            r3 = 1
        Lb93:
            r11 = r20
            r2 = r24
            r5 = r30
            goto Lc20
        Lb9b:
            r0 = move-exception
            r24 = r15
            r30 = r31
            r15 = r9
            r4 = r51
            r9 = r54
            r3 = r58
            r1 = r0
            r2 = r24
            r13 = r30
            goto Le74
        Lbae:
            r0 = move-exception
            r30 = r13
            r24 = r15
            r15 = r9
            goto Lbdd
        Lbb5:
            r0 = move-exception
            r37 = r10
            r29 = r12
            r30 = r13
            r24 = r15
            r19 = r39
            r15 = r52
            r1 = r0
        Lbc3:
            monitor-exit(r50)     // Catch: java.lang.Throwable -> Lbd3
            throw r1     // Catch: java.lang.Throwable -> Lbc5
        Lbc5:
            r0 = move-exception
            r4 = r51
            r9 = r54
            r3 = r58
            r1 = r0
            r2 = r24
            r13 = r30
            goto Le74
        Lbd3:
            r0 = move-exception
            r1 = r0
            goto Lbc3
        Lbd6:
            r0 = move-exception
            r30 = r13
            r24 = r15
            r15 = r52
        Lbdd:
            r4 = r51
            r9 = r54
            r3 = r58
            r1 = r0
            r2 = r24
            goto Le74
        Lbe8:
            r0 = move-exception
            r30 = r6
            r24 = r9
            r58 = r15
            r15 = r8
            r4 = r51
            r9 = r54
            r3 = r58
            r1 = r0
            r2 = r24
            r13 = r30
            goto Le74
        Lbfd:
            r0 = move-exception
            r24 = r9
            r58 = r15
            r15 = r8
            r4 = r51
            r9 = r54
            r3 = r58
            r1 = r0
            r13 = r5
            r2 = r24
            goto Le74
        Lc0f:
            r29 = r12
            r33 = r13
            r58 = r15
            r19 = r35
            r5 = r36
            r3 = 1
            r15 = r52
            r1 = r26
            r11 = r33
        Lc20:
            java.lang.String r4 = "getContentProviderImpl: done!"
            r6 = r37
            r14.checkTime(r6, r4)     // Catch: java.lang.Throwable -> Le58
            com.android.server.am.ActivityManagerService r4 = r14.mService     // Catch: java.lang.Throwable -> Le58
            android.content.pm.ApplicationInfo r8 = r2.applicationInfo     // Catch: java.lang.Throwable -> Le58
            int r8 = r8.uid     // Catch: java.lang.Throwable -> Le58
            int r8 = android.os.UserHandle.getAppId(r8)     // Catch: java.lang.Throwable -> Le58
            r9 = r54
            r10 = 0
            r4.grantImplicitAccess(r5, r10, r9, r8)     // Catch: java.lang.Throwable -> Le54
            r4 = r51
            if (r4 == 0) goto Lcca
            monitor-enter(r11)     // Catch: java.lang.Throwable -> Lcc1
            android.content.IContentProvider r8 = r11.provider     // Catch: java.lang.Throwable -> Lcbd
            if (r8 != 0) goto Lcb2
            com.android.server.am.ProcessRecord r8 = r11.launchingApp     // Catch: java.lang.Throwable -> Lcbd
            if (r8 != 0) goto Lc98
            java.lang.String r3 = "ContentProviderHelper"
            java.lang.StringBuilder r8 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> Lcbd
            r8.<init>()     // Catch: java.lang.Throwable -> Lcbd
            java.lang.String r10 = "Unable to launch app "
            java.lang.StringBuilder r8 = r8.append(r10)     // Catch: java.lang.Throwable -> Lcbd
            android.content.pm.ApplicationInfo r10 = r2.applicationInfo     // Catch: java.lang.Throwable -> Lcbd
            java.lang.String r10 = r10.packageName     // Catch: java.lang.Throwable -> Lcbd
            java.lang.StringBuilder r8 = r8.append(r10)     // Catch: java.lang.Throwable -> Lcbd
            java.lang.String r10 = "/"
            java.lang.StringBuilder r8 = r8.append(r10)     // Catch: java.lang.Throwable -> Lcbd
            android.content.pm.ApplicationInfo r10 = r2.applicationInfo     // Catch: java.lang.Throwable -> Lcbd
            int r10 = r10.uid     // Catch: java.lang.Throwable -> Lcbd
            java.lang.StringBuilder r8 = r8.append(r10)     // Catch: java.lang.Throwable -> Lcbd
            java.lang.String r10 = " for provider "
            java.lang.StringBuilder r8 = r8.append(r10)     // Catch: java.lang.Throwable -> Lcbd
            java.lang.StringBuilder r8 = r8.append(r15)     // Catch: java.lang.Throwable -> Lcbd
            java.lang.String r10 = ": launching app became null"
            java.lang.StringBuilder r8 = r8.append(r10)     // Catch: java.lang.Throwable -> Lcbd
            java.lang.String r8 = r8.toString()     // Catch: java.lang.Throwable -> Lcbd
            android.util.Slog.w(r3, r8)     // Catch: java.lang.Throwable -> Lcbd
            android.content.pm.ApplicationInfo r3 = r2.applicationInfo     // Catch: java.lang.Throwable -> Lcbd
            int r3 = r3.uid     // Catch: java.lang.Throwable -> Lcbd
            int r3 = android.os.UserHandle.getUserId(r3)     // Catch: java.lang.Throwable -> Lcbd
            android.content.pm.ApplicationInfo r8 = r2.applicationInfo     // Catch: java.lang.Throwable -> Lcbd
            java.lang.String r8 = r8.packageName     // Catch: java.lang.Throwable -> Lcbd
            android.content.pm.ApplicationInfo r10 = r2.applicationInfo     // Catch: java.lang.Throwable -> Lcbd
            int r10 = r10.uid     // Catch: java.lang.Throwable -> Lcbd
            com.android.server.am.EventLogTags.writeAmProviderLostProcess(r3, r8, r10, r15)     // Catch: java.lang.Throwable -> Lcbd
            monitor-exit(r11)     // Catch: java.lang.Throwable -> Lcbd
            monitor-exit(r34)     // Catch: java.lang.Throwable -> Lcc1
            com.android.server.am.ActivityManagerService.resetPriorityAfterLockedSection()
            r3 = 0
            return r3
        Lc98:
            if (r1 == 0) goto Lc9c
            r1.waiting = r3     // Catch: java.lang.Throwable -> Lcbd
        Lc9c:
            com.android.server.am.ActivityManagerService r3 = r14.mService     // Catch: java.lang.Throwable -> Lcbd
            com.android.server.am.ActivityManagerService$MainHandler r3 = r3.mHandler     // Catch: java.lang.Throwable -> Lcbd
            r8 = 73
            android.os.Message r3 = r3.obtainMessage(r8)     // Catch: java.lang.Throwable -> Lcbd
            r3.obj = r11     // Catch: java.lang.Throwable -> Lcbd
            com.android.server.am.ActivityManagerService r8 = r14.mService     // Catch: java.lang.Throwable -> Lcbd
            com.android.server.am.ActivityManagerService$MainHandler r8 = r8.mHandler     // Catch: java.lang.Throwable -> Lcbd
            int r10 = android.content.ContentResolver.CONTENT_PROVIDER_READY_TIMEOUT_MILLIS     // Catch: java.lang.Throwable -> Lcbd
            long r12 = (long) r10     // Catch: java.lang.Throwable -> Lcbd
            r8.sendMessageDelayed(r3, r12)     // Catch: java.lang.Throwable -> Lcbd
        Lcb2:
            monitor-exit(r11)     // Catch: java.lang.Throwable -> Lcbd
            r3 = 0
            android.app.ContentProviderHolder r3 = r11.newHolder(r1, r3)     // Catch: java.lang.Throwable -> Lcc1
            monitor-exit(r34)     // Catch: java.lang.Throwable -> Lcc1
            com.android.server.am.ActivityManagerService.resetPriorityAfterLockedSection()
            return r3
        Lcbd:
            r0 = move-exception
            r3 = r0
            monitor-exit(r11)     // Catch: java.lang.Throwable -> Lcbd
            throw r3     // Catch: java.lang.Throwable -> Lcc1
        Lcc1:
            r0 = move-exception
            r3 = r58
            r26 = r1
            r13 = r5
            r1 = r0
            goto Le74
        Lcca:
            monitor-exit(r34)     // Catch: java.lang.Throwable -> Le52
            com.android.server.am.ActivityManagerService.resetPriorityAfterLockedSection()
            long r6 = android.os.SystemClock.uptimeMillis()
            int r8 = android.content.ContentResolver.CONTENT_PROVIDER_READY_TIMEOUT_MILLIS
            long r12 = (long) r8
            long r6 = r6 + r12
            r8 = 0
            monitor-enter(r11)
        Lcd8:
            android.content.IContentProvider r10 = r11.provider     // Catch: java.lang.Throwable -> Le47
            if (r10 != 0) goto Ldb4
            com.android.server.am.ProcessRecord r10 = r11.launchingApp     // Catch: java.lang.Throwable -> Ldaa
            if (r10 != 0) goto Ld38
            java.lang.String r3 = "ContentProviderHelper"
            java.lang.StringBuilder r10 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> Ld30
            r10.<init>()     // Catch: java.lang.Throwable -> Ld30
            java.lang.String r12 = "Unable to launch app "
            java.lang.StringBuilder r10 = r10.append(r12)     // Catch: java.lang.Throwable -> Ld30
            android.content.pm.ApplicationInfo r12 = r2.applicationInfo     // Catch: java.lang.Throwable -> Ld30
            java.lang.String r12 = r12.packageName     // Catch: java.lang.Throwable -> Ld30
            java.lang.StringBuilder r10 = r10.append(r12)     // Catch: java.lang.Throwable -> Ld30
            java.lang.String r12 = "/"
            java.lang.StringBuilder r10 = r10.append(r12)     // Catch: java.lang.Throwable -> Ld30
            android.content.pm.ApplicationInfo r12 = r2.applicationInfo     // Catch: java.lang.Throwable -> Ld30
            int r12 = r12.uid     // Catch: java.lang.Throwable -> Ld30
            java.lang.StringBuilder r10 = r10.append(r12)     // Catch: java.lang.Throwable -> Ld30
            java.lang.String r12 = " for provider "
            java.lang.StringBuilder r10 = r10.append(r12)     // Catch: java.lang.Throwable -> Ld30
            java.lang.StringBuilder r10 = r10.append(r15)     // Catch: java.lang.Throwable -> Ld30
            java.lang.String r12 = ": launching app became null"
            java.lang.StringBuilder r10 = r10.append(r12)     // Catch: java.lang.Throwable -> Ld30
            java.lang.String r10 = r10.toString()     // Catch: java.lang.Throwable -> Ld30
            android.util.Slog.w(r3, r10)     // Catch: java.lang.Throwable -> Ld30
            android.content.pm.ApplicationInfo r3 = r2.applicationInfo     // Catch: java.lang.Throwable -> Ld30
            int r3 = r3.uid     // Catch: java.lang.Throwable -> Ld30
            int r3 = android.os.UserHandle.getUserId(r3)     // Catch: java.lang.Throwable -> Ld30
            android.content.pm.ApplicationInfo r10 = r2.applicationInfo     // Catch: java.lang.Throwable -> Ld30
            java.lang.String r10 = r10.packageName     // Catch: java.lang.Throwable -> Ld30
            android.content.pm.ApplicationInfo r12 = r2.applicationInfo     // Catch: java.lang.Throwable -> Ld30
            int r12 = r12.uid     // Catch: java.lang.Throwable -> Ld30
            com.android.server.am.EventLogTags.writeAmProviderLostProcess(r3, r10, r12, r15)     // Catch: java.lang.Throwable -> Ld30
            monitor-exit(r11)     // Catch: java.lang.Throwable -> Ld30
            r3 = 0
            return r3
        Ld30:
            r0 = move-exception
            r13 = r58
            r16 = r2
            r2 = r0
            goto Le4d
        Ld38:
            long r12 = android.os.SystemClock.uptimeMillis()     // Catch: java.lang.Throwable -> Ld97 java.lang.InterruptedException -> Lda0
            long r12 = r6 - r12
            r3 = 0
            long r12 = java.lang.Math.max(r3, r12)     // Catch: java.lang.InterruptedException -> Ld94 java.lang.Throwable -> Ld97
            boolean r16 = com.android.server.am.ActivityManagerDebugConfig.DEBUG_MU     // Catch: java.lang.InterruptedException -> Ld94 java.lang.Throwable -> Ld97
            if (r16 == 0) goto Ld7c
            java.lang.String r3 = "ActivityManager_MU"
            java.lang.StringBuilder r4 = new java.lang.StringBuilder     // Catch: java.lang.InterruptedException -> Ld94 java.lang.Throwable -> Ld97
            r4.<init>()     // Catch: java.lang.InterruptedException -> Ld94 java.lang.Throwable -> Ld97
            java.lang.String r10 = "Waiting to start provider "
            java.lang.StringBuilder r4 = r4.append(r10)     // Catch: java.lang.InterruptedException -> Ld94 java.lang.Throwable -> Ld97
            java.lang.StringBuilder r4 = r4.append(r11)     // Catch: java.lang.InterruptedException -> Ld94 java.lang.Throwable -> Ld97
            java.lang.String r10 = " launchingApp="
            java.lang.StringBuilder r4 = r4.append(r10)     // Catch: java.lang.InterruptedException -> Ld94 java.lang.Throwable -> Ld97
            com.android.server.am.ProcessRecord r10 = r11.launchingApp     // Catch: java.lang.InterruptedException -> Ld94 java.lang.Throwable -> Ld97
            java.lang.StringBuilder r4 = r4.append(r10)     // Catch: java.lang.InterruptedException -> Ld94 java.lang.Throwable -> Ld97
            java.lang.String r10 = " for "
            java.lang.StringBuilder r4 = r4.append(r10)     // Catch: java.lang.InterruptedException -> Ld94 java.lang.Throwable -> Ld97
            java.lang.StringBuilder r4 = r4.append(r12)     // Catch: java.lang.InterruptedException -> Ld94 java.lang.Throwable -> Ld97
            java.lang.String r10 = " ms"
            java.lang.StringBuilder r4 = r4.append(r10)     // Catch: java.lang.InterruptedException -> Ld94 java.lang.Throwable -> Ld97
            java.lang.String r4 = r4.toString()     // Catch: java.lang.InterruptedException -> Ld94 java.lang.Throwable -> Ld97
            android.util.Slog.v(r3, r4)     // Catch: java.lang.InterruptedException -> Ld94 java.lang.Throwable -> Ld97
        Ld7c:
            if (r1 == 0) goto Ld82
            r3 = 1
            r1.waiting = r3     // Catch: java.lang.Throwable -> Ld97 java.lang.InterruptedException -> Lda0
            goto Ld83
        Ld82:
            r3 = 1
        Ld83:
            r11.wait(r12)     // Catch: java.lang.Throwable -> Ld97 java.lang.InterruptedException -> Lda0
            android.content.IContentProvider r4 = r11.provider     // Catch: java.lang.Throwable -> Ld97 java.lang.InterruptedException -> Lda0
            if (r4 != 0) goto Ld91
            r8 = 1
            if (r1 == 0) goto Ldb4
            r3 = 0
            r1.waiting = r3     // Catch: java.lang.Throwable -> Ldaa
            goto Ldb4
        Ld91:
            if (r1 == 0) goto Lda6
            goto Lda3
        Ld94:
            r0 = move-exception
            r3 = 1
            goto Lda1
        Ld97:
            r0 = move-exception
            r3 = r0
            if (r1 == 0) goto Ld9e
            r4 = 0
            r1.waiting = r4     // Catch: java.lang.Throwable -> Ldaa
        Ld9e:
            throw r3     // Catch: java.lang.Throwable -> Ldaa
        Lda0:
            r0 = move-exception
        Lda1:
            if (r1 == 0) goto Lda6
        Lda3:
            r4 = 0
            r1.waiting = r4     // Catch: java.lang.Throwable -> Ldaa
        Lda6:
            r4 = r51
            goto Lcd8
        Ldaa:
            r0 = move-exception
            r4 = r51
            r13 = r58
            r16 = r2
            r2 = r0
            goto Le4d
        Ldb4:
            monitor-exit(r11)     // Catch: java.lang.Throwable -> Le43
            if (r8 == 0) goto Le3b
            java.lang.String r3 = "unknown"
            r4 = r51
            if (r4 == 0) goto Lddf
            com.android.server.am.ActivityManagerService r10 = r14.mService
            com.android.server.am.ActivityManagerGlobalLock r10 = r10.mProcLock
            monitor-enter(r10)
            com.android.server.am.ActivityManagerService.boostPriorityForProcLockedSection()     // Catch: java.lang.Throwable -> Ldd8
            com.android.server.am.ActivityManagerService r12 = r14.mService     // Catch: java.lang.Throwable -> Ldd8
            com.android.server.am.ProcessList r12 = r12.mProcessList     // Catch: java.lang.Throwable -> Ldd8
            com.android.server.am.ProcessRecord r12 = r12.getLRURecordForAppLOSP(r4)     // Catch: java.lang.Throwable -> Ldd8
            if (r12 == 0) goto Ldd3
            java.lang.String r13 = r12.processName     // Catch: java.lang.Throwable -> Ldd8
            r3 = r13
        Ldd3:
            monitor-exit(r10)     // Catch: java.lang.Throwable -> Ldd8
            com.android.server.am.ActivityManagerService.resetPriorityAfterProcLockedSection()
            goto Lddf
        Ldd8:
            r0 = move-exception
            r12 = r0
            monitor-exit(r10)     // Catch: java.lang.Throwable -> Ldd8
            com.android.server.am.ActivityManagerService.resetPriorityAfterProcLockedSection()
            throw r12
        Lddf:
            java.lang.String r10 = "ContentProviderHelper"
            java.lang.StringBuilder r12 = new java.lang.StringBuilder
            r12.<init>()
            java.lang.String r13 = "Timeout waiting for provider "
            java.lang.StringBuilder r12 = r12.append(r13)
            android.content.pm.ApplicationInfo r13 = r2.applicationInfo
            java.lang.String r13 = r13.packageName
            java.lang.StringBuilder r12 = r12.append(r13)
            java.lang.String r13 = "/"
            java.lang.StringBuilder r12 = r12.append(r13)
            android.content.pm.ApplicationInfo r13 = r2.applicationInfo
            int r13 = r13.uid
            java.lang.StringBuilder r12 = r12.append(r13)
            java.lang.String r13 = " for provider "
            java.lang.StringBuilder r12 = r12.append(r13)
            java.lang.StringBuilder r12 = r12.append(r15)
            java.lang.String r13 = " providerRunning="
            java.lang.StringBuilder r12 = r12.append(r13)
            r13 = r58
            java.lang.StringBuilder r12 = r12.append(r13)
            r16 = r2
            java.lang.String r2 = " caller="
            java.lang.StringBuilder r2 = r12.append(r2)
            java.lang.StringBuilder r2 = r2.append(r3)
            java.lang.String r12 = "/"
            java.lang.StringBuilder r2 = r2.append(r12)
            int r12 = android.os.Binder.getCallingUid()
            java.lang.StringBuilder r2 = r2.append(r12)
            java.lang.String r2 = r2.toString()
            android.util.Slog.d(r10, r2)
            r2 = 0
            return r2
        Le3b:
            r16 = r2
            r2 = 0
            android.app.ContentProviderHolder r2 = r11.newHolder(r1, r2)
            return r2
        Le43:
            r0 = move-exception
            r4 = r51
            goto Le48
        Le47:
            r0 = move-exception
        Le48:
            r13 = r58
            r16 = r2
            r2 = r0
        Le4d:
            monitor-exit(r11)     // Catch: java.lang.Throwable -> Le4f
            throw r2
        Le4f:
            r0 = move-exception
            r2 = r0
            goto Le4d
        Le52:
            r0 = move-exception
            goto Le5d
        Le54:
            r0 = move-exception
            r4 = r51
            goto Le5d
        Le58:
            r0 = move-exception
            r4 = r51
            r9 = r54
        Le5d:
            r13 = r58
            r16 = r2
            r26 = r1
            r3 = r13
            r1 = r0
            r13 = r5
            goto Le74
        Le67:
            r0 = move-exception
            r16 = r2
            r17 = r3
            r9 = r11
            r34 = r12
            r4 = r14
            r14 = r15
            r15 = r13
            r13 = r1
            r1 = r0
        Le74:
            monitor-exit(r34)     // Catch: java.lang.Throwable -> Le79
            com.android.server.am.ActivityManagerService.resetPriorityAfterLockedSection()
            throw r1
        Le79:
            r0 = move-exception
            r1 = r0
            goto Le74
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.am.ContentProviderHelper.getContentProviderImpl(android.app.IApplicationThread, java.lang.String, android.os.IBinder, int, java.lang.String, java.lang.String, boolean, int):android.app.ContentProviderHolder");
    }

    private void checkAssociationAndPermissionLocked(ProcessRecord callingApp, ProviderInfo cpi, int callingUid, int userId, boolean checkUser, String cprName, long startTime) {
        String msg = checkContentProviderAssociation(callingApp, callingUid, cpi);
        if (msg != null) {
            throw new SecurityException("Content provider lookup " + cprName + " failed: association not allowed with package " + msg);
        }
        checkTime(startTime, "getContentProviderImpl: before checkContentProviderPermission");
        String msg2 = checkContentProviderPermission(cpi, Binder.getCallingPid(), Binder.getCallingUid(), userId, checkUser, callingApp != null ? callingApp.toString() : null);
        if (msg2 != null) {
            throw new SecurityException(msg2);
        }
        checkTime(startTime, "getContentProviderImpl: after checkContentProviderPermission");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void publishContentProviders(IApplicationThread caller, List<ContentProviderHolder> providers) {
        int size;
        boolean providersPublished;
        ComponentName comp;
        if (providers == null) {
            return;
        }
        this.mService.enforceNotIsolatedOrSdkSandboxCaller("publishContentProviders");
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                ProcessRecord r = this.mService.getRecordForAppLOSP(caller);
                if (ActivityManagerDebugConfig.DEBUG_MU && r != null) {
                    Slog.v("ActivityManager_MU", "ProcessRecord uid = " + r.uid);
                }
                if (r == null) {
                    throw new SecurityException("Unable to find app for caller " + caller + " (pid=" + Binder.getCallingPid() + ") when publishing content providers");
                }
                long origId = Binder.clearCallingIdentity();
                boolean providersPublished2 = false;
                int i = 0;
                int size2 = providers.size();
                while (i < size2) {
                    ContentProviderHolder src = providers.get(i);
                    if (src == null || src.info == null) {
                        size = size2;
                    } else if (src.provider == null) {
                        size = size2;
                    } else {
                        ContentProviderRecord dst = r.mProviders.getProvider(src.info.name);
                        if (dst == null) {
                            size = size2;
                        } else {
                            if (ActivityManagerDebugConfig.DEBUG_MU) {
                                Slog.v("ActivityManager_MU", "ContentProviderRecord uid = " + dst.uid);
                            }
                            boolean providersPublished3 = true;
                            ComponentName comp2 = new ComponentName(dst.info.packageName, dst.info.name);
                            this.mProviderMap.putProviderByClass(comp2, dst);
                            String[] names = dst.info.authority.split(";");
                            int j = 0;
                            while (j < names.length) {
                                this.mProviderMap.putProviderByName(names[j], dst);
                                j++;
                                size2 = size2;
                            }
                            size = size2;
                            int numLaunching = this.mLaunchingProviders.size();
                            boolean wasInLaunchingProviders = false;
                            int j2 = 0;
                            while (true) {
                                providersPublished = providersPublished3;
                                if (j2 >= numLaunching) {
                                    break;
                                }
                                if (this.mLaunchingProviders.get(j2) != dst) {
                                    comp = comp2;
                                } else {
                                    this.mLaunchingProviders.remove(j2);
                                    comp = comp2;
                                    ITranActivityManagerService.Instance().onProviderRemove(dst.launchingApp);
                                    wasInLaunchingProviders = true;
                                    j2--;
                                    numLaunching--;
                                }
                                j2++;
                                providersPublished3 = providersPublished;
                                comp2 = comp;
                            }
                            if (wasInLaunchingProviders) {
                                this.mService.mHandler.removeMessages(73, dst);
                                this.mService.mHandler.removeMessages(57, r);
                            }
                            r.addPackage(dst.info.applicationInfo.packageName, dst.info.applicationInfo.longVersionCode, this.mService.mProcessStats);
                            synchronized (dst) {
                                dst.provider = src.provider;
                                dst.setProcess(r);
                                dst.notifyAll();
                                dst.onProviderPublishStatusLocked(true);
                            }
                            dst.mRestartCount = 0;
                            if (hasProviderConnectionLocked(r)) {
                                r.mProfile.addHostingComponentType(64);
                            }
                            providersPublished2 = providersPublished;
                        }
                    }
                    i++;
                    size2 = size;
                }
                if (providersPublished2) {
                    this.mService.updateOomAdjLocked(r, "updateOomAdj_getProvider");
                    int size3 = providers.size();
                    for (int i2 = 0; i2 < size3; i2++) {
                        ContentProviderHolder src2 = providers.get(i2);
                        if (src2 != null && src2.info != null && src2.provider != null) {
                            maybeUpdateProviderUsageStatsLocked(r, src2.info.packageName, src2.info.authority);
                        }
                    }
                }
                Binder.restoreCallingIdentity(origId);
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeContentProvider(IBinder connection, boolean stable) {
        this.mService.enforceNotIsolatedOrSdkSandboxCaller("removeContentProvider");
        long ident = Binder.clearCallingIdentity();
        try {
            try {
                ContentProviderConnection conn = (ContentProviderConnection) connection;
                if (conn == null) {
                    throw new NullPointerException("connection is null");
                }
                ActivityManagerService.traceBegin(64L, "removeContentProvider: ", (conn.provider == null || conn.provider.info == null) ? "" : conn.provider.info.authority);
                synchronized (this.mService) {
                    ActivityManagerService.boostPriorityForLockedSection();
                    decProviderCountLocked(conn, null, null, stable, true, true);
                }
                ActivityManagerService.resetPriorityAfterLockedSection();
                Trace.traceEnd(64L);
                Binder.restoreCallingIdentity(ident);
            } catch (ClassCastException e) {
                String msg = "removeContentProvider: " + connection + " not a ContentProviderConnection";
                Slog.w(TAG, msg);
                throw new IllegalArgumentException(msg);
            }
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(ident);
            throw th;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeContentProviderExternalAsUser(String name, IBinder token, int userId) {
        this.mService.enforceCallingPermission("android.permission.ACCESS_CONTENT_PROVIDERS_EXTERNALLY", "Do not have permission in call removeContentProviderExternal()");
        long ident = Binder.clearCallingIdentity();
        try {
            removeContentProviderExternalUnchecked(name, token, userId);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeContentProviderExternalUnchecked(String name, IBinder token, int userId) {
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                ContentProviderRecord cpr = this.mProviderMap.getProviderByName(name, userId);
                if (cpr == null) {
                    if (ActivityManagerDebugConfig.DEBUG_ALL) {
                        Slog.v(TAG, name + " content provider not found in providers list");
                    }
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                ComponentName comp = new ComponentName(cpr.info.packageName, cpr.info.name);
                ContentProviderRecord localCpr = this.mProviderMap.getProviderByClass(comp, userId);
                if (localCpr.hasExternalProcessHandles()) {
                    if (localCpr.removeExternalProcessHandleLocked(token)) {
                        this.mService.updateOomAdjLocked(localCpr.proc, "updateOomAdj_removeProvider");
                    } else {
                        Slog.e(TAG, "Attempt to remove content provider " + localCpr + " with no external reference for token: " + token + ".");
                    }
                } else {
                    Slog.e(TAG, "Attempt to remove content provider: " + localCpr + " with no external references.");
                }
                ActivityManagerService.resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean refContentProvider(IBinder connection, int stable, int unstable) {
        try {
            ContentProviderConnection conn = (ContentProviderConnection) connection;
            if (conn == null) {
                throw new NullPointerException("connection is null");
            }
            ActivityManagerService.traceBegin(64L, "refContentProvider: ", (conn.provider == null || conn.provider.info == null) ? "" : conn.provider.info.authority);
            try {
                conn.adjustCounts(stable, unstable);
                return !conn.dead;
            } finally {
                Trace.traceEnd(64L);
            }
        } catch (ClassCastException e) {
            String msg = "refContentProvider: " + connection + " not a ContentProviderConnection";
            Slog.w(TAG, msg);
            throw new IllegalArgumentException(msg);
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [990=6] */
    /* JADX INFO: Access modifiers changed from: package-private */
    public void unstableProviderDied(IBinder connection) {
        IContentProvider provider;
        try {
            ContentProviderConnection conn = (ContentProviderConnection) connection;
            if (conn == null) {
                throw new NullPointerException("connection is null");
            }
            ActivityManagerService.traceBegin(64L, "unstableProviderDied: ", (conn.provider == null || conn.provider.info == null) ? "" : conn.provider.info.authority);
            try {
                synchronized (this.mService) {
                    ActivityManagerService.boostPriorityForLockedSection();
                    provider = conn.provider.provider;
                }
                ActivityManagerService.resetPriorityAfterLockedSection();
                if (provider == null) {
                    return;
                }
                if (provider.asBinder().pingBinder()) {
                    synchronized (this.mService) {
                        ActivityManagerService.boostPriorityForLockedSection();
                        Slog.w(TAG, "unstableProviderDied: caller " + Binder.getCallingUid() + " says " + conn + " died, but we don't agree");
                    }
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                synchronized (this.mService) {
                    ActivityManagerService.boostPriorityForLockedSection();
                    if (conn.provider.provider != provider) {
                        ActivityManagerService.resetPriorityAfterLockedSection();
                        return;
                    }
                    ProcessRecord proc = conn.provider.proc;
                    if (proc != null && proc.getThread() != null) {
                        this.mService.reportUidInfoMessageLocked(TAG, "Process " + proc.processName + " (pid " + proc.getPid() + ") early provider death", proc.info.uid);
                        long token = Binder.clearCallingIdentity();
                        try {
                            this.mService.appDiedLocked(proc, "unstable content provider");
                            ActivityManagerService.resetPriorityAfterLockedSection();
                            return;
                        } finally {
                            Binder.restoreCallingIdentity(token);
                        }
                    }
                    ActivityManagerService.resetPriorityAfterLockedSection();
                }
            } finally {
                Trace.traceEnd(64L);
            }
        } catch (ClassCastException e) {
            String msg = "refContentProvider: " + connection + " not a ContentProviderConnection";
            Slog.w(TAG, msg);
            throw new IllegalArgumentException(msg);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void appNotRespondingViaProvider(IBinder connection) {
        this.mService.enforceCallingPermission("android.permission.REMOVE_TASKS", "appNotRespondingViaProvider()");
        ContentProviderConnection conn = (ContentProviderConnection) connection;
        if (conn == null) {
            Slog.w(TAG, "ContentProviderConnection is null");
            return;
        }
        ActivityManagerService.traceBegin(64L, "appNotRespondingViaProvider: ", (conn.provider == null || conn.provider.info == null) ? "" : conn.provider.info.authority);
        try {
            ProcessRecord host = conn.provider.proc;
            if (host == null) {
                Slog.w(TAG, "Failed to find hosting ProcessRecord");
            } else {
                this.mService.mAnrHelper.appNotResponding(host, "ContentProvider not responding");
            }
        } finally {
            Trace.traceEnd(64L);
        }
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Unreachable block: B:50:0x00c0
        	at jadx.core.dex.visitors.blocks.BlockProcessor.checkForUnreachableBlocks(BlockProcessor.java:81)
        	at jadx.core.dex.visitors.blocks.BlockProcessor.processBlocksTree(BlockProcessor.java:47)
        	at jadx.core.dex.visitors.blocks.BlockProcessor.visit(BlockProcessor.java:39)
        */
    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1088=10, 1089=5, 1080=8, 1081=5, 1084=5, 1085=5] */
    @java.lang.Deprecated
    java.lang.String getProviderMimeType(android.net.Uri r20, int r21) {
        /*
            r19 = this;
            r7 = r19
            r8 = r20
            java.lang.String r9 = "ContentProviderHelper"
            com.android.server.am.ActivityManagerService r0 = r7.mService
            java.lang.String r1 = "getProviderMimeType"
            r0.enforceNotIsolatedCaller(r1)
            java.lang.String r10 = r20.getAuthority()
            int r11 = android.os.Binder.getCallingUid()
            int r12 = android.os.Binder.getCallingPid()
            r0 = 0
            r2 = 0
            com.android.server.am.ActivityManagerService r3 = r7.mService
            com.android.server.am.UserController r3 = r3.mUserController
            r4 = r21
            int r13 = r3.unsafeConvertIncomingUser(r4)
            boolean r3 = r7.canClearIdentity(r12, r11, r13)
            if (r3 == 0) goto L35
            r2 = 1
            long r0 = android.os.Binder.clearCallingIdentity()
            r14 = r0
            r16 = r2
            goto L38
        L35:
            r14 = r0
            r16 = r2
        L38:
            r17 = 0
            r3 = 0
            r6 = 0
            java.lang.String r5 = "*getmimetype*"
            r1 = r19
            r2 = r10
            r4 = r11
            r18 = r11
            r11 = r6
            r6 = r13
            android.app.ContentProviderHolder r0 = r1.getContentProviderExternalUnchecked(r2, r3, r4, r5, r6)     // Catch: java.lang.Throwable -> Lb9 java.lang.Exception -> Lbc android.os.RemoteException -> Lbe
            r1 = r0
            if (r1 == 0) goto La1
            android.os.IBinder r0 = r1.connection     // Catch: java.lang.Throwable -> L94 java.lang.Exception -> L99 android.os.RemoteException -> L9d
            r2 = r0
            android.content.pm.ProviderInfo r0 = r1.info     // Catch: java.lang.Throwable -> L94 java.lang.Exception -> L99 android.os.RemoteException -> L9d
            android.content.ComponentName r0 = r0.getComponentName()     // Catch: java.lang.Throwable -> L94 java.lang.Exception -> L99 android.os.RemoteException -> L9d
            r3 = r0
            com.android.server.am.ContentProviderHelper$1 r0 = new com.android.server.am.ContentProviderHelper$1     // Catch: java.lang.Throwable -> L94 java.lang.Exception -> L99 android.os.RemoteException -> L9d
            r0.<init>()     // Catch: java.lang.Throwable -> L94 java.lang.Exception -> L99 android.os.RemoteException -> L9d
            r4 = r0
            com.android.server.am.ActivityManagerService r0 = r7.mService     // Catch: java.lang.Throwable -> L94 java.lang.Exception -> L99 android.os.RemoteException -> L9d
            com.android.server.am.ActivityManagerService$MainHandler r0 = r0.mHandler     // Catch: java.lang.Throwable -> L94 java.lang.Exception -> L99 android.os.RemoteException -> L9d
            r5 = 1000(0x3e8, double:4.94E-321)
            r0.postDelayed(r4, r5)     // Catch: java.lang.Throwable -> L94 java.lang.Exception -> L99 android.os.RemoteException -> L9d
            android.content.IContentProvider r0 = r1.provider     // Catch: java.lang.Throwable -> L8a
            java.lang.String r0 = r0.getType(r8)     // Catch: java.lang.Throwable -> L8a
            com.android.server.am.ActivityManagerService r5 = r7.mService     // Catch: java.lang.Throwable -> L94 java.lang.Exception -> L99 android.os.RemoteException -> L9d
            com.android.server.am.ActivityManagerService$MainHandler r5 = r5.mHandler     // Catch: java.lang.Throwable -> L94 java.lang.Exception -> L99 android.os.RemoteException -> L9d
            r5.removeCallbacks(r4)     // Catch: java.lang.Throwable -> L94 java.lang.Exception -> L99 android.os.RemoteException -> L9d
            if (r16 != 0) goto L79
            long r14 = android.os.Binder.clearCallingIdentity()
        L79:
            if (r1 == 0) goto L85
            r7.removeContentProviderExternalUnchecked(r10, r11, r13)     // Catch: java.lang.Throwable -> L7f
            goto L85
        L7f:
            r0 = move-exception
            r5 = r0
            android.os.Binder.restoreCallingIdentity(r14)
            throw r5
        L85:
            android.os.Binder.restoreCallingIdentity(r14)
            return r0
        L8a:
            r0 = move-exception
            com.android.server.am.ActivityManagerService r5 = r7.mService     // Catch: java.lang.Throwable -> L94 java.lang.Exception -> L99 android.os.RemoteException -> L9d
            com.android.server.am.ActivityManagerService$MainHandler r5 = r5.mHandler     // Catch: java.lang.Throwable -> L94 java.lang.Exception -> L99 android.os.RemoteException -> L9d
            r5.removeCallbacks(r4)     // Catch: java.lang.Throwable -> L94 java.lang.Exception -> L99 android.os.RemoteException -> L9d
            throw r0     // Catch: java.lang.Throwable -> L94 java.lang.Exception -> L99 android.os.RemoteException -> L9d
        L94:
            r0 = move-exception
            r17 = r1
            goto L12b
        L99:
            r0 = move-exception
            r17 = r1
            goto Lc9
        L9d:
            r0 = move-exception
            r17 = r1
            goto Lfc
        La1:
            if (r16 != 0) goto La7
            long r14 = android.os.Binder.clearCallingIdentity()
        La7:
            if (r1 == 0) goto Lb3
            r7.removeContentProviderExternalUnchecked(r10, r11, r13)     // Catch: java.lang.Throwable -> Lad
            goto Lb3
        Lad:
            r0 = move-exception
            r2 = r0
            android.os.Binder.restoreCallingIdentity(r14)
            throw r2
        Lb3:
            android.os.Binder.restoreCallingIdentity(r14)
            return r11
        Lb9:
            r0 = move-exception
            goto L12b
        Lbc:
            r0 = move-exception
            goto Lc9
        Lbe:
            r0 = move-exception
            goto Lfc
        Lc0:
            r0 = move-exception
            r18 = r11
            r11 = r6
            goto L12b
        Lc5:
            r0 = move-exception
            r18 = r11
            r11 = r6
        Lc9:
            r1 = r0
            java.lang.StringBuilder r0 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> Lb9
            r0.<init>()     // Catch: java.lang.Throwable -> Lb9
            java.lang.String r2 = "Exception while determining type of "
            java.lang.StringBuilder r0 = r0.append(r2)     // Catch: java.lang.Throwable -> Lb9
            java.lang.StringBuilder r0 = r0.append(r8)     // Catch: java.lang.Throwable -> Lb9
            java.lang.String r0 = r0.toString()     // Catch: java.lang.Throwable -> Lb9
            android.util.Log.w(r9, r0, r1)     // Catch: java.lang.Throwable -> Lb9
            if (r16 != 0) goto Le7
            long r14 = android.os.Binder.clearCallingIdentity()
        Le7:
            if (r17 == 0) goto Lf3
            r7.removeContentProviderExternalUnchecked(r10, r11, r13)     // Catch: java.lang.Throwable -> Led
            goto Lf3
        Led:
            r0 = move-exception
            r2 = r0
            android.os.Binder.restoreCallingIdentity(r14)
            throw r2
        Lf3:
            android.os.Binder.restoreCallingIdentity(r14)
            return r11
        Lf8:
            r0 = move-exception
            r18 = r11
            r11 = r6
        Lfc:
            r1 = r0
            java.lang.StringBuilder r0 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> Lb9
            r0.<init>()     // Catch: java.lang.Throwable -> Lb9
            java.lang.String r2 = "Content provider dead retrieving "
            java.lang.StringBuilder r0 = r0.append(r2)     // Catch: java.lang.Throwable -> Lb9
            java.lang.StringBuilder r0 = r0.append(r8)     // Catch: java.lang.Throwable -> Lb9
            java.lang.String r0 = r0.toString()     // Catch: java.lang.Throwable -> Lb9
            android.util.Log.w(r9, r0, r1)     // Catch: java.lang.Throwable -> Lb9
            if (r16 != 0) goto L11a
            long r14 = android.os.Binder.clearCallingIdentity()
        L11a:
            if (r17 == 0) goto L126
            r7.removeContentProviderExternalUnchecked(r10, r11, r13)     // Catch: java.lang.Throwable -> L120
            goto L126
        L120:
            r0 = move-exception
            r2 = r0
            android.os.Binder.restoreCallingIdentity(r14)
            throw r2
        L126:
            android.os.Binder.restoreCallingIdentity(r14)
            return r11
        L12b:
            if (r16 != 0) goto L131
            long r14 = android.os.Binder.clearCallingIdentity()
        L131:
            if (r17 == 0) goto L13d
            r7.removeContentProviderExternalUnchecked(r10, r11, r13)     // Catch: java.lang.Throwable -> L137
            goto L13d
        L137:
            r0 = move-exception
            r1 = r0
            android.os.Binder.restoreCallingIdentity(r14)
            throw r1
        L13d:
            android.os.Binder.restoreCallingIdentity(r14)
            throw r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.am.ContentProviderHelper.getProviderMimeType(android.net.Uri, int):java.lang.String");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void getProviderMimeTypeAsync(Uri uri, int userId, final RemoteCallback resultCallback) {
        this.mService.enforceNotIsolatedCaller("getProviderMimeTypeAsync");
        final String name = uri.getAuthority();
        int callingUid = Binder.getCallingUid();
        int callingPid = Binder.getCallingPid();
        final int safeUserId = this.mService.mUserController.unsafeConvertIncomingUser(userId);
        long ident = canClearIdentity(callingPid, callingUid, userId) ? Binder.clearCallingIdentity() : 0L;
        try {
            try {
                ContentProviderHolder holder = getContentProviderExternalUnchecked(name, null, callingUid, "*getmimetype*", safeUserId);
                if (holder != null) {
                    holder.provider.getTypeAsync(uri, new RemoteCallback(new RemoteCallback.OnResultListener() { // from class: com.android.server.am.ContentProviderHelper$$ExternalSyntheticLambda0
                        public final void onResult(Bundle bundle) {
                            ContentProviderHelper.this.m1420x17528303(name, safeUserId, resultCallback, bundle);
                        }
                    }));
                } else {
                    resultCallback.sendResult(Bundle.EMPTY);
                }
            } catch (RemoteException e) {
                Log.w(TAG, "Content provider dead retrieving " + uri, e);
                resultCallback.sendResult(Bundle.EMPTY);
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getProviderMimeTypeAsync$0$com-android-server-am-ContentProviderHelper  reason: not valid java name */
    public /* synthetic */ void m1420x17528303(String name, int safeUserId, RemoteCallback resultCallback, Bundle result) {
        long identity = Binder.clearCallingIdentity();
        try {
            removeContentProviderExternalUnchecked(name, null, safeUserId);
            Binder.restoreCallingIdentity(identity);
            resultCallback.sendResult(result);
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(identity);
            throw th;
        }
    }

    private boolean canClearIdentity(int callingPid, int callingUid, int userId) {
        return UserHandle.getUserId(callingUid) == userId || ActivityManagerService.checkComponentPermission("android.permission.INTERACT_ACROSS_USERS", callingPid, callingUid, -1, true) == 0 || ActivityManagerService.checkComponentPermission("android.permission.INTERACT_ACROSS_USERS_FULL", callingPid, callingUid, -1, true) == 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String checkContentProviderAccess(String authority, int userId) {
        UserManagerInternal umInternal;
        UserInfo userInfo;
        UserManagerInternal umInternal2;
        UserInfo userInfo2;
        boolean checkUser = true;
        if (userId == -1) {
            this.mService.mContext.enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL", TAG);
            userId = UserHandle.getCallingUserId();
        }
        if (ContentProvider.isAuthorityRedirectedForCloneProfile(authority) && (userInfo2 = (umInternal2 = (UserManagerInternal) LocalServices.getService(UserManagerInternal.class)).getUserInfo(userId)) != null && userInfo2.isCloneProfile()) {
            userId = umInternal2.getProfileParentId(userId);
            checkUser = false;
        }
        ProviderInfo cpi = null;
        try {
            cpi = AppGlobals.getPackageManager().resolveContentProvider(authority, 790016L, userId);
        } catch (RemoteException e) {
        }
        if (cpi == null) {
            return "Failed to find provider " + authority + " for user " + userId + "; expected to find a valid ContentProvider for this authority";
        }
        if (shouldCrossDualApp(cpi.applicationInfo.packageName, userId) && (userInfo = (umInternal = (UserManagerInternal) LocalServices.getService(UserManagerInternal.class)).getUserInfo(userId)) != null && userInfo.isDualProfile()) {
            userId = umInternal.getProfileParentId(userId);
            checkUser = false;
        }
        int callingPid = Binder.getCallingPid();
        synchronized (this.mService.mPidsSelfLocked) {
            ProcessRecord r = this.mService.mPidsSelfLocked.get(callingPid);
            if (r == null) {
                return "Failed to find PID " + callingPid;
            }
            String appName = r.toString();
            return checkContentProviderPermission(cpi, callingPid, Binder.getCallingUid(), userId, checkUser, appName);
        }
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Unreachable block: B:64:0x00c7
        	at jadx.core.dex.visitors.blocks.BlockProcessor.checkForUnreachableBlocks(BlockProcessor.java:81)
        	at jadx.core.dex.visitors.blocks.BlockProcessor.processBlocksTree(BlockProcessor.java:47)
        	at jadx.core.dex.visitors.blocks.BlockProcessor.visit(BlockProcessor.java:39)
        */
    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1237=4, 1240=4, 1244=5, 1245=6, 1246=6, 1249=12, 1250=6] */
    int checkContentProviderUriPermission(android.net.Uri r18, int r19, int r20, int r21) {
        /*
            r17 = this;
            r7 = r17
            r8 = r18
            r9 = r19
            r10 = r20
            com.android.server.am.ActivityManagerService r0 = r7.mService
            com.android.server.wm.ActivityTaskManagerService r0 = r0.mActivityTaskManager
            com.android.server.wm.WindowManagerGlobalLock r0 = r0.getGlobalLock()
            boolean r0 = java.lang.Thread.holdsLock(r0)
            java.lang.String r11 = "ContentProviderHelper"
            r12 = -1
            if (r0 == 0) goto L24
            java.lang.IllegalStateException r0 = new java.lang.IllegalStateException
            java.lang.String r1 = "Unable to check Uri permission because caller is holding WM lock; assuming permission denied"
            r0.<init>(r1)
            android.util.Slog.wtf(r11, r0)
            return r12
        L24:
            java.lang.String r13 = r18.getAuthority()
            long r14 = android.os.Binder.clearCallingIdentity()
            r16 = 0
            r3 = 0
            r6 = 0
            java.lang.String r5 = "*checkContentProviderUriPermission*"
            r1 = r17
            r2 = r13
            r4 = r20
            r12 = r6
            r6 = r19
            android.app.ContentProviderHolder r0 = r1.getContentProviderExternalUnchecked(r2, r3, r4, r5, r6)     // Catch: java.lang.Throwable -> Lba java.lang.Exception -> Lbf android.os.RemoteException -> Lc3
            r1 = r0
            if (r1 == 0) goto La5
            java.lang.Class<android.content.pm.PackageManagerInternal> r0 = android.content.pm.PackageManagerInternal.class
            java.lang.Object r0 = com.android.server.LocalServices.getService(r0)     // Catch: java.lang.Throwable -> L92 java.lang.Exception -> L99 android.os.RemoteException -> L9f
            android.content.pm.PackageManagerInternal r0 = (android.content.pm.PackageManagerInternal) r0     // Catch: java.lang.Throwable -> L92 java.lang.Exception -> L99 android.os.RemoteException -> L9f
            r2 = r0
            int r0 = android.os.Binder.getCallingUid()     // Catch: java.lang.Throwable -> L92 java.lang.Exception -> L99 android.os.RemoteException -> L9f
            com.android.server.pm.parsing.pkg.AndroidPackage r0 = r2.getPackage(r0)     // Catch: java.lang.Throwable -> L92 java.lang.Exception -> L99 android.os.RemoteException -> L9f
            r3 = r0
            if (r3 != 0) goto L69
        L57:
            if (r1 == 0) goto L63
            r7.removeContentProviderExternalUnchecked(r13, r12, r9)     // Catch: java.lang.Throwable -> L5d
            goto L63
        L5d:
            r0 = move-exception
            r4 = r0
            android.os.Binder.restoreCallingIdentity(r14)
            throw r4
        L63:
            android.os.Binder.restoreCallingIdentity(r14)
            r4 = -1
            return r4
        L69:
            android.content.AttributionSource r0 = new android.content.AttributionSource     // Catch: java.lang.Throwable -> L92 java.lang.Exception -> L99 android.os.RemoteException -> L9f
            java.lang.String r4 = r3.getPackageName()     // Catch: java.lang.Throwable -> L92 java.lang.Exception -> L99 android.os.RemoteException -> L9f
            r0.<init>(r10, r4, r12)     // Catch: java.lang.Throwable -> L92 java.lang.Exception -> L99 android.os.RemoteException -> L9f
            r4 = r0
            android.content.IContentProvider r0 = r1.provider     // Catch: java.lang.Throwable -> L92 java.lang.Exception -> L99 android.os.RemoteException -> L9f
            r5 = r21
            int r0 = r0.checkUriPermission(r4, r8, r10, r5)     // Catch: java.lang.Throwable -> L8c java.lang.Exception -> L8e android.os.RemoteException -> L90
            if (r1 == 0) goto L87
            r7.removeContentProviderExternalUnchecked(r13, r12, r9)     // Catch: java.lang.Throwable -> L81
            goto L87
        L81:
            r0 = move-exception
            r6 = r0
            android.os.Binder.restoreCallingIdentity(r14)
            throw r6
        L87:
            android.os.Binder.restoreCallingIdentity(r14)
            return r0
        L8c:
            r0 = move-exception
            goto L95
        L8e:
            r0 = move-exception
            goto L9c
        L90:
            r0 = move-exception
            goto La2
        L92:
            r0 = move-exception
            r5 = r21
        L95:
            r16 = r1
            goto L129
        L99:
            r0 = move-exception
            r5 = r21
        L9c:
            r16 = r1
            goto Ld0
        L9f:
            r0 = move-exception
            r5 = r21
        La2:
            r16 = r1
            goto Lfe
        La5:
            r5 = r21
            if (r1 == 0) goto Lb3
            r7.removeContentProviderExternalUnchecked(r13, r12, r9)     // Catch: java.lang.Throwable -> Lad
            goto Lb3
        Lad:
            r0 = move-exception
            r2 = r0
            android.os.Binder.restoreCallingIdentity(r14)
            throw r2
        Lb3:
            android.os.Binder.restoreCallingIdentity(r14)
            r2 = -1
            return r2
        Lba:
            r0 = move-exception
            r5 = r21
            goto L129
        Lbf:
            r0 = move-exception
            r5 = r21
            goto Ld0
        Lc3:
            r0 = move-exception
            r5 = r21
            goto Lfe
        Lc7:
            r0 = move-exception
            r5 = r21
            r12 = r6
            goto L129
        Lcc:
            r0 = move-exception
            r5 = r21
            r12 = r6
        Ld0:
            r1 = r0
            java.lang.StringBuilder r0 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L128
            r0.<init>()     // Catch: java.lang.Throwable -> L128
            java.lang.String r2 = "Exception while determining type of "
            java.lang.StringBuilder r0 = r0.append(r2)     // Catch: java.lang.Throwable -> L128
            java.lang.StringBuilder r0 = r0.append(r8)     // Catch: java.lang.Throwable -> L128
            java.lang.String r0 = r0.toString()     // Catch: java.lang.Throwable -> L128
            android.util.Log.w(r11, r0, r1)     // Catch: java.lang.Throwable -> L128
            if (r16 == 0) goto Lf4
            r7.removeContentProviderExternalUnchecked(r13, r12, r9)     // Catch: java.lang.Throwable -> Lee
            goto Lf4
        Lee:
            r0 = move-exception
            r2 = r0
            android.os.Binder.restoreCallingIdentity(r14)
            throw r2
        Lf4:
            android.os.Binder.restoreCallingIdentity(r14)
            r2 = -1
            return r2
        Lfa:
            r0 = move-exception
            r5 = r21
            r12 = r6
        Lfe:
            r1 = r0
            java.lang.StringBuilder r0 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L128
            r0.<init>()     // Catch: java.lang.Throwable -> L128
            java.lang.String r2 = "Content provider dead retrieving "
            java.lang.StringBuilder r0 = r0.append(r2)     // Catch: java.lang.Throwable -> L128
            java.lang.StringBuilder r0 = r0.append(r8)     // Catch: java.lang.Throwable -> L128
            java.lang.String r0 = r0.toString()     // Catch: java.lang.Throwable -> L128
            android.util.Log.w(r11, r0, r1)     // Catch: java.lang.Throwable -> L128
            if (r16 == 0) goto L122
            r7.removeContentProviderExternalUnchecked(r13, r12, r9)     // Catch: java.lang.Throwable -> L11c
            goto L122
        L11c:
            r0 = move-exception
            r2 = r0
            android.os.Binder.restoreCallingIdentity(r14)
            throw r2
        L122:
            android.os.Binder.restoreCallingIdentity(r14)
            r2 = -1
            return r2
        L128:
            r0 = move-exception
        L129:
            if (r16 == 0) goto L135
            r7.removeContentProviderExternalUnchecked(r13, r12, r9)     // Catch: java.lang.Throwable -> L12f
            goto L135
        L12f:
            r0 = move-exception
            r1 = r0
            android.os.Binder.restoreCallingIdentity(r14)
            throw r1
        L135:
            android.os.Binder.restoreCallingIdentity(r14)
            throw r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.am.ContentProviderHelper.checkContentProviderUriPermission(android.net.Uri, int, int, int):int");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void processContentProviderPublishTimedOutLocked(ProcessRecord app) {
        cleanupAppInLaunchingProvidersLocked(app, true);
        this.mService.mProcessList.removeProcessLocked(app, false, true, 7, 0, "timeout publishing content providers");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public List<ProviderInfo> generateApplicationProvidersLocked(ProcessRecord app) {
        ContentProviderRecord cpr;
        try {
            List<ProviderInfo> providers = AppGlobals.getPackageManager().queryContentProviders(app.processName, app.uid, 268438528L, (String) null).getList();
            if (providers == null) {
                return null;
            }
            if (ActivityManagerDebugConfig.DEBUG_MU) {
                Slog.v("ActivityManager_MU", "generateApplicationProvidersLocked, app.info.uid = " + app.uid);
            }
            int numProviders = providers.size();
            ProcessProviderRecord pr = app.mProviders;
            pr.ensureProviderCapacity(pr.numberOfProviders() + numProviders);
            int i = 0;
            while (i < numProviders) {
                ProviderInfo cpi = providers.get(i);
                boolean singleton = this.mService.isSingleton(cpi.processName, cpi.applicationInfo, cpi.name, cpi.flags);
                if (singleton && app.userId != 0) {
                    providers.remove(i);
                    numProviders--;
                    i--;
                } else {
                    boolean isInstantApp = cpi.applicationInfo.isInstantApp();
                    boolean splitInstalled = cpi.splitName == null || ArrayUtils.contains(cpi.applicationInfo.splitNames, cpi.splitName);
                    if (isInstantApp && !splitInstalled) {
                        providers.remove(i);
                        numProviders--;
                        i--;
                    } else {
                        ComponentName comp = new ComponentName(cpi.packageName, cpi.name);
                        ContentProviderRecord cpr2 = this.mProviderMap.getProviderByClass(comp, app.userId);
                        if (cpr2 == null) {
                            cpr = new ContentProviderRecord(this.mService, cpi, app.info, comp, singleton);
                            this.mProviderMap.putProviderByClass(comp, cpr);
                        } else {
                            cpr = cpr2;
                        }
                        if (ActivityManagerDebugConfig.DEBUG_MU) {
                            Slog.v("ActivityManager_MU", "generateApplicationProvidersLocked, cpi.uid = " + cpr.uid);
                        }
                        pr.installProvider(cpi.name, cpr);
                        if (!cpi.multiprocess || !PackageManagerService.PLATFORM_PACKAGE_NAME.equals(cpi.packageName)) {
                            app.addPackage(cpi.applicationInfo.packageName, cpi.applicationInfo.longVersionCode, this.mService.mProcessStats);
                        }
                        this.mService.notifyPackageUse(cpi.applicationInfo.packageName, 4);
                    }
                }
                i++;
            }
            if (providers.isEmpty()) {
                return null;
            }
            return providers;
        } catch (RemoteException e) {
            return null;
        }
    }

    /* loaded from: classes.dex */
    private final class DevelopmentSettingsObserver extends ContentObserver {
        private final ComponentName mBugreportStorageProvider;
        private final Uri mUri;

        DevelopmentSettingsObserver() {
            super(ContentProviderHelper.this.mService.mHandler);
            Uri uriFor = Settings.Global.getUriFor("development_settings_enabled");
            this.mUri = uriFor;
            this.mBugreportStorageProvider = new ComponentName(VibratorManagerService.VibratorManagerShellCommand.SHELL_PACKAGE_NAME, "com.android.shell.BugreportStorageProvider");
            ContentProviderHelper.this.mService.mContext.getContentResolver().registerContentObserver(uriFor, false, this, -1);
            onChange();
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri, int userId) {
            if (this.mUri.equals(uri)) {
                onChange();
            }
        }

        private void onChange() {
            boolean enabled = Settings.Global.getInt(ContentProviderHelper.this.mService.mContext.getContentResolver(), "development_settings_enabled", Build.IS_ENG ? 1 : 0) != 0;
            ContentProviderHelper.this.mService.mContext.getPackageManager().setComponentEnabledSetting(this.mBugreportStorageProvider, enabled ? 1 : 0, 0);
        }
    }

    public final void installSystemProviders() {
        List<ProviderInfo> providers;
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                ProcessRecord app = (ProcessRecord) this.mService.mProcessList.getProcessNamesLOSP().get(HostingRecord.HOSTING_TYPE_SYSTEM, 1000);
                providers = generateApplicationProvidersLocked(app);
                if (providers != null) {
                    for (int i = providers.size() - 1; i >= 0; i--) {
                        ProviderInfo pi = providers.get(i);
                        if ((pi.applicationInfo.flags & 1) == 0) {
                            Slog.w(TAG, "Not installing system proc provider " + pi.name + ": not system .apk");
                            providers.remove(i);
                        }
                    }
                }
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
        if (providers != null) {
            this.mService.mSystemThread.installSystemProviders(providers);
        }
        synchronized (this) {
            this.mSystemProvidersInstalled = true;
        }
        this.mService.mConstants.start(this.mService.mContext.getContentResolver());
        this.mService.mCoreSettingsObserver = new CoreSettingsObserver(this.mService);
        this.mService.mActivityTaskManager.installSystemProviders();
        new DevelopmentSettingsObserver();
        SettingsToPropertiesMapper.start(this.mService.mContext.getContentResolver());
        this.mService.mOomAdjuster.initSettings();
        RescueParty.onSettingsProviderPublished(this.mService.mContext);
        ITranActivityManagerService.Instance().onInstallSystemProviders(this.mService.mContext);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void installEncryptionUnawareProviders(int userId) {
        synchronized (this.mService.mProcLock) {
            try {
                ActivityManagerService.boostPriorityForProcLockedSection();
                ArrayMap<String, SparseArray<ProcessRecord>> pmap = this.mService.mProcessList.getProcessNamesLOSP().getMap();
                int numProc = pmap.size();
                for (int iProc = 0; iProc < numProc; iProc++) {
                    SparseArray<ProcessRecord> apps = pmap.valueAt(iProc);
                    int numApps = apps.size();
                    for (int iApp = 0; iApp < numApps; iApp++) {
                        final ProcessRecord app = apps.valueAt(iApp);
                        if (app.userId == userId && app.getThread() != null && !app.isUnlocked()) {
                            app.getPkgList().forEachPackage(new Consumer() { // from class: com.android.server.am.ContentProviderHelper$$ExternalSyntheticLambda3
                                @Override // java.util.function.Consumer
                                public final void accept(Object obj) {
                                    ContentProviderHelper.this.m1421x7077911e(app, (String) obj);
                                }
                            });
                        }
                    }
                }
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterProcLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterProcLockedSection();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$installEncryptionUnawareProviders$1$com-android-server-am-ContentProviderHelper  reason: not valid java name */
    public /* synthetic */ void m1421x7077911e(ProcessRecord app, String pkgName) {
        ProviderInfo[] providerInfoArr;
        boolean userMatch;
        try {
            try {
                PackageInfo pkgInfo = AppGlobals.getPackageManager().getPackageInfo(pkgName, 262152L, app.userId);
                IApplicationThread thread = app.getThread();
                if (pkgInfo != null && !ArrayUtils.isEmpty(pkgInfo.providers)) {
                    for (ProviderInfo pi : pkgInfo.providers) {
                        boolean splitInstalled = true;
                        boolean processMatch = Objects.equals(pi.processName, app.processName) || pi.multiprocess;
                        try {
                            if (this.mService.isSingleton(pi.processName, pi.applicationInfo, pi.name, pi.flags) && app.userId != 0) {
                                userMatch = false;
                                boolean isInstantApp = pi.applicationInfo.isInstantApp();
                                if (pi.splitName != null && !ArrayUtils.contains(pi.applicationInfo.splitNames, pi.splitName)) {
                                    splitInstalled = false;
                                }
                                if (processMatch || !userMatch || (isInstantApp && !splitInstalled)) {
                                    Log.v(TAG, "Skipping " + pi);
                                } else {
                                    Log.v(TAG, "Installing " + pi);
                                    thread.scheduleInstallProvider(pi);
                                }
                            }
                            userMatch = true;
                            boolean isInstantApp2 = pi.applicationInfo.isInstantApp();
                            if (pi.splitName != null) {
                                splitInstalled = false;
                            }
                            if (processMatch) {
                            }
                            Log.v(TAG, "Skipping " + pi);
                        } catch (RemoteException e) {
                            return;
                        }
                    }
                }
            } catch (RemoteException e2) {
            }
        } catch (RemoteException e3) {
        }
    }

    private ContentProviderConnection incProviderCountLocked(ProcessRecord r, ContentProviderRecord cpr, IBinder externalProcessToken, int callingUid, String callingPackage, String callingTag, boolean stable, boolean updateLru, long startTime, ProcessList processList, int expectedUserId) {
        if (r == null) {
            cpr.addExternalProcessHandleLocked(externalProcessToken, callingUid, callingTag);
            return null;
        }
        ProcessProviderRecord pr = r.mProviders;
        int size = pr.numberOfProviderConnections();
        for (int i = 0; i < size; i++) {
            ContentProviderConnection conn = pr.getProviderConnectionAt(i);
            if (conn.provider == cpr) {
                conn.incrementCount(stable);
                return conn;
            }
        }
        ContentProviderConnection conn2 = new ContentProviderConnection(cpr, r, callingPackage, expectedUserId);
        conn2.startAssociationIfNeeded();
        conn2.initializeCount(stable);
        cpr.connections.add(conn2);
        if (cpr.proc != null) {
            cpr.proc.mProfile.addHostingComponentType(64);
        }
        pr.addProviderConnection(conn2);
        this.mService.startAssociationLocked(r.uid, r.processName, r.mState.getCurProcState(), cpr.uid, cpr.appInfo.longVersionCode, cpr.name, cpr.info.processName);
        if (updateLru && cpr.proc != null) {
            if (r.mState.getSetAdj() <= 250) {
                checkTime(startTime, "getContentProviderImpl: before updateLruProcess");
                processList.updateLruProcessLocked(cpr.proc, false, null);
                checkTime(startTime, "getContentProviderImpl: after updateLruProcess");
            }
        }
        return conn2;
    }

    private boolean decProviderCountLocked(final ContentProviderConnection conn, ContentProviderRecord cpr, IBinder externalProcessToken, final boolean stable, boolean enforceDelay, final boolean updateOomAdj) {
        if (conn == null) {
            cpr.removeExternalProcessHandleLocked(externalProcessToken);
            return false;
        } else if (conn.totalRefCount() > 1) {
            conn.decrementCount(stable);
            return false;
        } else {
            if (enforceDelay) {
                BackgroundThread.getHandler().postDelayed(new Runnable() { // from class: com.android.server.am.ContentProviderHelper$$ExternalSyntheticLambda2
                    @Override // java.lang.Runnable
                    public final void run() {
                        ContentProviderHelper.this.m1419x7c036e84(conn, stable, updateOomAdj);
                    }
                }, 5000L);
            } else {
                m1419x7c036e84(conn, stable, updateOomAdj);
            }
            return true;
        }
    }

    private boolean hasProviderConnectionLocked(ProcessRecord proc) {
        for (int i = proc.mProviders.numberOfProviders() - 1; i >= 0; i--) {
            if (!proc.mProviders.getProviderAt(i).connections.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: handleProviderRemoval */
    public void m1419x7c036e84(ContentProviderConnection conn, boolean stable, boolean updateOomAdj) {
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                if (conn != null && conn.provider != null && conn.decrementCount(stable) == 0) {
                    ContentProviderRecord cpr = conn.provider;
                    conn.stopAssociation();
                    cpr.connections.remove(conn);
                    if (cpr.proc != null && !hasProviderConnectionLocked(cpr.proc)) {
                        cpr.proc.mProfile.clearHostingComponentType(64);
                    }
                    conn.client.mProviders.removeProviderConnection(conn);
                    if (conn.client.mState.getSetProcState() < 15 && cpr.proc != null) {
                        cpr.proc.mProviders.setLastProviderTime(SystemClock.uptimeMillis());
                    }
                    this.mService.stopAssociationLocked(conn.client.uid, conn.client.processName, cpr.uid, cpr.appInfo.longVersionCode, cpr.name, cpr.info.processName);
                    if (updateOomAdj) {
                        this.mService.updateOomAdjLocked(conn.provider.proc, "updateOomAdj_removeProvider");
                    }
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                ActivityManagerService.resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    private String checkContentProviderPermission(ProviderInfo cpi, int callingPid, int callingUid, int userId, boolean checkUser, String appName) {
        boolean checkedGrants;
        int userId2;
        String suffix;
        if (!checkUser) {
            checkedGrants = false;
            userId2 = userId;
        } else {
            int tmpTargetUserId = this.mService.mUserController.unsafeConvertIncomingUser(userId);
            if (tmpTargetUserId == UserHandle.getUserId(callingUid)) {
                checkedGrants = false;
            } else if (this.mService.mUgmInternal.checkAuthorityGrants(callingUid, cpi, tmpTargetUserId, checkUser)) {
                return null;
            } else {
                checkedGrants = true;
            }
            userId2 = this.mService.mUserController.handleIncomingUser(callingPid, callingUid, userId, false, 0, "checkContentProviderPermissionLocked " + cpi.authority, null);
            if (userId2 != tmpTargetUserId) {
                checkedGrants = false;
            }
        }
        if (ActivityManagerService.checkComponentPermission(cpi.readPermission, callingPid, callingUid, cpi.applicationInfo.uid, cpi.exported) == 0 || ActivityManagerService.checkComponentPermission(cpi.writePermission, callingPid, callingUid, cpi.applicationInfo.uid, cpi.exported) == 0) {
            return null;
        }
        PathPermission[] pps = cpi.pathPermissions;
        if (pps != null) {
            int i = pps.length;
            while (i > 0) {
                i--;
                PathPermission pp = pps[i];
                String pprperm = pp.getReadPermission();
                if (pprperm != null && ActivityManagerService.checkComponentPermission(pprperm, callingPid, callingUid, cpi.applicationInfo.uid, cpi.exported) == 0) {
                    return null;
                }
                String ppwperm = pp.getWritePermission();
                if (ppwperm != null && ActivityManagerService.checkComponentPermission(ppwperm, callingPid, callingUid, cpi.applicationInfo.uid, cpi.exported) == 0) {
                    return null;
                }
            }
        }
        if (!checkedGrants && this.mService.mUgmInternal.checkAuthorityGrants(callingUid, cpi, userId2, checkUser)) {
            return null;
        }
        if (appName != null && appName.contains("com.transsion.resolver/")) {
            return null;
        }
        if (!cpi.exported) {
            suffix = " that is not exported from UID " + cpi.applicationInfo.uid;
        } else {
            String suffix2 = cpi.readPermission;
            if ("android.permission.MANAGE_DOCUMENTS".equals(suffix2)) {
                suffix = " requires that you obtain access using ACTION_OPEN_DOCUMENT or related APIs";
            } else {
                suffix = " requires " + cpi.readPermission + " or " + cpi.writePermission;
            }
        }
        String msg = "Permission Denial: opening provider " + cpi.name + " from " + (appName != null ? appName : "(null)") + " (pid=" + callingPid + ", uid=" + callingUid + ")" + suffix;
        Slog.w(TAG, msg);
        return msg;
    }

    private String checkContentProviderAssociation(final ProcessRecord callingApp, int callingUid, final ProviderInfo cpi) {
        if (callingApp == null) {
            if (this.mService.validateAssociationAllowedLocked(cpi.packageName, cpi.applicationInfo.uid, null, callingUid)) {
                return null;
            }
            return "<null>";
        }
        String r = (String) callingApp.getPkgList().searchEachPackage(new Function() { // from class: com.android.server.am.ContentProviderHelper$$ExternalSyntheticLambda1
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return ContentProviderHelper.this.m1418x427ea544(callingApp, cpi, (String) obj);
            }
        });
        return r;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$checkContentProviderAssociation$3$com-android-server-am-ContentProviderHelper  reason: not valid java name */
    public /* synthetic */ String m1418x427ea544(ProcessRecord callingApp, ProviderInfo cpi, String pkgName) {
        if (!this.mService.validateAssociationAllowedLocked(pkgName, callingApp.uid, cpi.packageName, cpi.applicationInfo.uid)) {
            return cpi.packageName;
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ProviderInfo getProviderInfoLocked(String authority, int userId, int pmFlags) {
        ContentProviderRecord cpr = this.mProviderMap.getProviderByName(authority, userId);
        if (cpr != null) {
            return cpr.info;
        }
        try {
            return AppGlobals.getPackageManager().resolveContentProvider(authority, pmFlags | 2048, userId);
        } catch (RemoteException e) {
            return null;
        }
    }

    private void maybeUpdateProviderUsageStatsLocked(ProcessRecord app, String providerPkgName, String authority) {
        UserState userState;
        if (app == null || app.mState.getCurProcState() > 6 || (userState = this.mService.mUserController.getStartedUserState(app.userId)) == null) {
            return;
        }
        long now = SystemClock.elapsedRealtime();
        Long lastReported = userState.mProviderLastReportedFg.get(authority);
        if (lastReported == null || lastReported.longValue() < now - 60000) {
            if (this.mService.mSystemReady) {
                this.mService.mUsageStatsService.reportContentProviderUsage(authority, providerPkgName, app.userId);
            }
            userState.mProviderLastReportedFg.put(authority, Long.valueOf(now));
        }
    }

    private boolean isProcessAliveLocked(ProcessRecord proc) {
        int pid = proc.getPid();
        if (pid <= 0) {
            if (ActivityManagerDebugConfig.DEBUG_OOM_ADJ) {
                Slog.d("ActivityManager", "Process hasn't started yet: " + proc);
            }
            return false;
        }
        String procStatFile = "/proc/" + pid + "/stat";
        long[] jArr = this.mProcessStateStatsLongs;
        jArr[0] = 0;
        if (!Process.readProcFile(procStatFile, PROCESS_STATE_STATS_FORMAT, null, jArr, null)) {
            if (ActivityManagerDebugConfig.DEBUG_OOM_ADJ) {
                Slog.d("ActivityManager", "UNABLE TO RETRIEVE STATE FOR " + procStatFile);
            }
            return false;
        }
        long state = this.mProcessStateStatsLongs[0];
        if (ActivityManagerDebugConfig.DEBUG_OOM_ADJ) {
            Slog.d("ActivityManager", "RETRIEVED STATE FOR " + procStatFile + ": " + ((char) state));
        }
        return (state == 90 || state == 88 || state == 120 || state == 75 || Process.getUidForPid(pid) != proc.uid) ? false : true;
    }

    /* loaded from: classes.dex */
    private static final class StartActivityRunnable implements Runnable {
        private final Context mContext;
        private final Intent mIntent;
        private final UserHandle mUserHandle;

        StartActivityRunnable(Context context, Intent intent, UserHandle userHandle) {
            this.mContext = context;
            this.mIntent = intent;
            this.mUserHandle = userHandle;
        }

        @Override // java.lang.Runnable
        public void run() {
            this.mContext.startActivityAsUser(this.mIntent, this.mUserHandle);
        }
    }

    private boolean requestTargetProviderPermissionsReviewIfNeededLocked(ProviderInfo cpi, ProcessRecord r, int userId, Context context) {
        boolean callerForeground = true;
        if (this.mService.getPackageManagerInternal().isPermissionsReviewRequired(cpi.packageName, userId)) {
            if (r != null && r.mState.getSetSchedGroup() == 0) {
                callerForeground = false;
            }
            if (!callerForeground) {
                Slog.w(TAG, "u" + userId + " Instantiating a provider in package " + cpi.packageName + " requires a permissions review");
                return false;
            }
            Intent intent = new Intent("android.intent.action.REVIEW_PERMISSIONS");
            intent.addFlags(276824064);
            intent.putExtra("android.intent.extra.PACKAGE_NAME", cpi.packageName);
            if (ActivityManagerDebugConfig.DEBUG_PERMISSIONS_REVIEW) {
                Slog.i(TAG, "u" + userId + " Launching permission review for package " + cpi.packageName);
            }
            UserHandle userHandle = new UserHandle(userId);
            this.mService.mHandler.post(new StartActivityRunnable(context, intent, userHandle));
            return false;
        }
        return true;
    }

    /* JADX DEBUG: Another duplicated slice has different insns count: {[MOVE, CONST]}, finally: {[MOVE] complete} */
    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Removed duplicated region for block: B:80:0x0024 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public boolean removeDyingProviderLocked(ProcessRecord proc, ContentProviderRecord cpr, boolean always) {
        boolean always2;
        boolean always3;
        boolean always4;
        boolean inLaunching = this.mLaunchingProviders.contains(cpr);
        boolean z = true;
        if (inLaunching && !always) {
            int i = cpr.mRestartCount + 1;
            cpr.mRestartCount = i;
            if (i > 3) {
                always2 = true;
                int i2 = 0;
                if (inLaunching || always2) {
                    synchronized (cpr) {
                        try {
                            ITranActivityManagerService.Instance().onProviderRemove(cpr.launchingApp);
                            cpr.launchingApp = null;
                            cpr.notifyAll();
                            cpr.onProviderPublishStatusLocked(false);
                            this.mService.mHandler.removeMessages(73, cpr);
                        } finally {
                            th = th;
                            while (true) {
                                try {
                                    break;
                                } catch (Throwable th) {
                                    th = th;
                                }
                            }
                        }
                    }
                    int userId = UserHandle.getUserId(cpr.uid);
                    if (this.mProviderMap.getProviderByClass(cpr.name, userId) == cpr) {
                        this.mProviderMap.removeProviderByClass(cpr.name, userId);
                    }
                    String[] names = cpr.info.authority.split(";");
                    for (int j = 0; j < names.length; j++) {
                        if (this.mProviderMap.getProviderByName(names[j], userId) == cpr) {
                            this.mProviderMap.removeProviderByName(names[j], userId);
                        }
                    }
                }
                int i3 = cpr.connections.size() - 1;
                while (i3 >= 0) {
                    ContentProviderConnection conn = cpr.connections.get(i3);
                    if (!conn.waiting || !inLaunching || always2) {
                        ProcessRecord capp = conn.client;
                        IApplicationThread thread = capp.getThread();
                        conn.dead = z;
                        if (conn.stableCount() > 0) {
                            int pid = capp.getPid();
                            if (!capp.isPersistent() && thread != null && pid != 0 && pid != ActivityManagerService.MY_PID) {
                                capp.killLocked("depends on provider " + cpr.name.flattenToShortString() + " in dying proc " + (proc != null ? proc.processName : "??") + " (adj " + (proc != null ? Integer.valueOf(proc.mState.getSetAdj()) : "??") + ")", 12, i2, z);
                            }
                            always4 = always2;
                        } else if (thread != null && conn.provider.provider != null) {
                            try {
                                thread.unstableProviderDied(conn.provider.provider.asBinder());
                            } catch (RemoteException e) {
                            }
                            cpr.connections.remove(i3);
                            if (cpr.proc != null && !hasProviderConnectionLocked(cpr.proc)) {
                                cpr.proc.mProfile.clearHostingComponentType(64);
                            }
                            if (!conn.client.mProviders.removeProviderConnection(conn)) {
                                always4 = always2;
                            } else {
                                always4 = always2;
                                this.mService.stopAssociationLocked(capp.uid, capp.processName, cpr.uid, cpr.appInfo.longVersionCode, cpr.name, cpr.info.processName);
                            }
                        }
                    }
                    i3--;
                    always2 = always4;
                    z = true;
                    i2 = 0;
                }
                if (inLaunching && always3) {
                    ITranActivityManagerService.Instance().onProviderRemove(cpr.launchingApp);
                    this.mLaunchingProviders.remove(cpr);
                    cpr.mRestartCount = 0;
                    return false;
                }
                return inLaunching;
            }
        }
        always2 = always;
        int i22 = 0;
        if (inLaunching) {
        }
        synchronized (cpr) {
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean checkAppInLaunchingProvidersLocked(ProcessRecord app) {
        for (int i = this.mLaunchingProviders.size() - 1; i >= 0; i--) {
            ContentProviderRecord cpr = this.mLaunchingProviders.get(i);
            if (cpr.launchingApp == app) {
                return true;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean cleanupAppInLaunchingProvidersLocked(ProcessRecord app, boolean alwaysBad) {
        boolean restart = false;
        for (int i = this.mLaunchingProviders.size() - 1; i >= 0; i--) {
            ContentProviderRecord cpr = this.mLaunchingProviders.get(i);
            if (cpr.launchingApp == app) {
                int i2 = cpr.mRestartCount + 1;
                cpr.mRestartCount = i2;
                if (i2 > 3) {
                    alwaysBad = true;
                }
                if (!alwaysBad && !app.mErrorState.isBad() && cpr.hasConnectionOrHandle()) {
                    restart = true;
                } else {
                    removeDyingProviderLocked(app, cpr, true);
                }
            }
        }
        return restart;
    }

    void cleanupLaunchingProvidersLocked() {
        for (int i = this.mLaunchingProviders.size() - 1; i >= 0; i--) {
            ContentProviderRecord cpr = this.mLaunchingProviders.get(i);
            if (cpr.connections.size() <= 0 && !cpr.hasExternalProcessHandles()) {
                synchronized (cpr) {
                    cpr.launchingApp = null;
                    cpr.notifyAll();
                }
            }
        }
    }

    private void checkTime(long startTime, String where) {
        long now = SystemClock.uptimeMillis();
        if (now - startTime > 50) {
            Slog.w(TAG, "Slow operation: " + (now - startTime) + "ms so far, now at " + where);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpProvidersLocked(FileDescriptor fd, PrintWriter pw, String[] args, int opti, boolean dumpAll, String dumpPackage) {
        ActivityManagerService.ItemMatcher matcher = new ActivityManagerService.ItemMatcher();
        matcher.build(args, opti);
        pw.println("ACTIVITY MANAGER CONTENT PROVIDERS (dumpsys activity providers)");
        boolean needSep = this.mProviderMap.dumpProvidersLocked(pw, dumpAll, dumpPackage);
        boolean printedAnything = needSep;
        if (this.mLaunchingProviders.size() > 0) {
            boolean printed = false;
            for (int i = this.mLaunchingProviders.size() - 1; i >= 0; i--) {
                ContentProviderRecord r = this.mLaunchingProviders.get(i);
                if (dumpPackage == null || dumpPackage.equals(r.name.getPackageName())) {
                    if (!printed) {
                        if (needSep) {
                            pw.println();
                        }
                        needSep = true;
                        pw.println("  Launching content providers:");
                        printed = true;
                        printedAnything = true;
                    }
                    pw.print("  Launching #");
                    pw.print(i);
                    pw.print(": ");
                    pw.println(r);
                }
            }
        }
        if (!printedAnything) {
            pw.println("  (nothing)");
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean dumpProvider(FileDescriptor fd, PrintWriter pw, String name, String[] args, int opti, boolean dumpAll) {
        return this.mProviderMap.dumpProvider(fd, pw, name, args, opti, dumpAll);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean dumpProviderProto(FileDescriptor fd, PrintWriter pw, String name, String[] args) {
        return this.mProviderMap.dumpProviderProto(fd, pw, name, args);
    }
}
