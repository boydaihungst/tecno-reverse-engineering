package com.android.server.wm;

import android.app.ActivityOptions;
import android.app.IApplicationThread;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Slog;
import android.util.SparseArray;
import android.view.RemoteAnimationAdapter;
import com.android.internal.util.ArrayUtils;
import com.android.server.am.PendingIntentRecord;
import com.android.server.uri.NeededUriGrants;
import com.android.server.wm.ActivityStarter;
import java.io.PrintWriter;
import java.util.List;
import java.util.function.IntFunction;
/* loaded from: classes2.dex */
public class ActivityStartController {
    private static final int DO_PENDING_ACTIVITY_LAUNCHES_MSG = 1;
    private static final String TAG = "ActivityTaskManager";
    boolean mCheckedForSetup;
    private final ActivityStarter.Factory mFactory;
    private ActivityRecord mLastHomeActivityStartRecord;
    private int mLastHomeActivityStartResult;
    private ActivityStarter mLastStarter;
    private final PendingRemoteAnimationRegistry mPendingRemoteAnimationRegistry;
    private final ActivityTaskManagerService mService;
    private final ActivityTaskSupervisor mSupervisor;
    private ActivityRecord[] tmpOutRecord;

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityStartController(ActivityTaskManagerService service) {
        this(service, service.mTaskSupervisor, new ActivityStarter.DefaultFactory(service, service.mTaskSupervisor, new ActivityStartInterceptor(service, service.mTaskSupervisor)));
    }

    ActivityStartController(ActivityTaskManagerService service, ActivityTaskSupervisor supervisor, ActivityStarter.Factory factory) {
        this.tmpOutRecord = new ActivityRecord[1];
        this.mCheckedForSetup = false;
        this.mService = service;
        this.mSupervisor = supervisor;
        this.mFactory = factory;
        factory.setController(this);
        this.mPendingRemoteAnimationRegistry = new PendingRemoteAnimationRegistry(service.mGlobalLock, service.mH);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityStarter obtainStarter(Intent intent, String reason) {
        return this.mFactory.obtain().setIntent(intent).setReason(reason);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onExecutionComplete(ActivityStarter starter) {
        if (this.mLastStarter == null) {
            this.mLastStarter = this.mFactory.obtain();
        }
        this.mLastStarter.set(starter);
        this.mFactory.recycle(starter);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void postStartActivityProcessingForLastStarter(ActivityRecord r, int result, Task targetRootTask) {
        ActivityStarter activityStarter = this.mLastStarter;
        if (activityStarter == null) {
            return;
        }
        activityStarter.postStartActivityProcessing(r, result, targetRootTask);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void startHomeActivity(Intent intent, ActivityInfo aInfo, String reason, TaskDisplayArea taskDisplayArea) {
        ActivityOptions options = ActivityOptions.makeBasic();
        options.setLaunchWindowingMode(1);
        if (!ActivityRecord.isResolverActivity(aInfo.name)) {
            options.setLaunchActivityType(2);
        }
        int displayId = taskDisplayArea.getDisplayId();
        options.setLaunchDisplayId(displayId);
        options.setLaunchTaskDisplayArea(taskDisplayArea.mRemoteToken.toWindowContainerToken());
        this.mSupervisor.beginDeferResume();
        try {
            Task rootHomeTask = taskDisplayArea.getOrCreateRootHomeTask(true);
            this.mSupervisor.endDeferResume();
            this.mLastHomeActivityStartResult = obtainStarter(intent, "startHomeActivity: " + reason).setOutActivity(this.tmpOutRecord).setCallingUid(0).setActivityInfo(aInfo).setActivityOptions(options.toBundle()).execute();
            this.mLastHomeActivityStartRecord = this.tmpOutRecord[0];
            if (rootHomeTask.mInResumeTopActivity) {
                this.mSupervisor.scheduleResumeTopActivities();
            }
        } catch (Throwable th) {
            this.mSupervisor.endDeferResume();
            throw th;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void startSetupActivity() {
        String vers;
        if (this.mCheckedForSetup) {
            return;
        }
        ContentResolver resolver = this.mService.mContext.getContentResolver();
        if (this.mService.mFactoryTest != 1 && Settings.Global.getInt(resolver, "device_provisioned", 0) != 0) {
            this.mCheckedForSetup = true;
            Intent intent = new Intent("android.intent.action.UPGRADE_SETUP");
            List<ResolveInfo> ris = this.mService.mContext.getPackageManager().queryIntentActivities(intent, 1049728);
            if (!ris.isEmpty()) {
                ResolveInfo ri = ris.get(0);
                if (ri.activityInfo.metaData != null) {
                    vers = ri.activityInfo.metaData.getString("android.SETUP_VERSION");
                } else {
                    vers = null;
                }
                if (vers == null && ri.activityInfo.applicationInfo.metaData != null) {
                    vers = ri.activityInfo.applicationInfo.metaData.getString("android.SETUP_VERSION");
                }
                String lastVers = Settings.Secure.getString(resolver, "last_setup_shown");
                if (vers != null && !vers.equals(lastVers)) {
                    intent.setFlags(268435456);
                    intent.setComponent(new ComponentName(ri.activityInfo.packageName, ri.activityInfo.name));
                    obtainStarter(intent, "startSetupActivity").setCallingUid(0).setActivityInfo(ri.activityInfo).execute();
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int checkTargetUser(int targetUserId, boolean validateIncomingUser, int realCallingPid, int realCallingUid, String reason) {
        if (validateIncomingUser) {
            return this.mService.handleIncomingUser(realCallingPid, realCallingUid, targetUserId, reason);
        }
        this.mService.mAmInternal.ensureNotSpecialUser(targetUserId);
        return targetUserId;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public final int startActivityInPackage(int uid, int realCallingPid, int realCallingUid, String callingPackage, String callingFeatureId, Intent intent, String resolvedType, IBinder resultTo, String resultWho, int requestCode, int startFlags, SafeActivityOptions options, int userId, Task inTask, String reason, boolean validateIncomingUser, PendingIntentRecord originatingPendingIntent, boolean allowBackgroundActivityStart) {
        return obtainStarter(intent, reason).setCallingUid(uid).setRealCallingPid(realCallingPid).setRealCallingUid(realCallingUid).setCallingPackage(callingPackage).setCallingFeatureId(callingFeatureId).setResolvedType(resolvedType).setResultTo(resultTo).setResultWho(resultWho).setRequestCode(requestCode).setStartFlags(startFlags).setActivityOptions(options).setUserId(checkTargetUser(userId, validateIncomingUser, realCallingPid, realCallingUid, reason)).setInTask(inTask).setOriginatingPendingIntent(originatingPendingIntent).setAllowBackgroundActivityStart(allowBackgroundActivityStart).execute();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public final int startActivitiesInPackage(int uid, String callingPackage, String callingFeatureId, Intent[] intents, String[] resolvedTypes, IBinder resultTo, SafeActivityOptions options, int userId, boolean validateIncomingUser, PendingIntentRecord originatingPendingIntent, boolean allowBackgroundActivityStart) {
        return startActivitiesInPackage(uid, 0, -1, callingPackage, callingFeatureId, intents, resolvedTypes, resultTo, options, userId, validateIncomingUser, originatingPendingIntent, allowBackgroundActivityStart);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public final int startActivitiesInPackage(int uid, int realCallingPid, int realCallingUid, String callingPackage, String callingFeatureId, Intent[] intents, String[] resolvedTypes, IBinder resultTo, SafeActivityOptions options, int userId, boolean validateIncomingUser, PendingIntentRecord originatingPendingIntent, boolean allowBackgroundActivityStart) {
        return startActivities(null, uid, realCallingPid, realCallingUid, callingPackage, callingFeatureId, intents, resolvedTypes, resultTo, options, checkTargetUser(userId, validateIncomingUser, Binder.getCallingPid(), Binder.getCallingUid(), "startActivityInPackage"), "startActivityInPackage", originatingPendingIntent, allowBackgroundActivityStart);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [487=11, 491=4, 493=9] */
    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Code restructure failed: missing block: B:100:0x0245, code lost:
        r0 = 0;
        r15 = r37;
     */
    /* JADX WARN: Code restructure failed: missing block: B:101:0x0248, code lost:
        r2 = r26;
     */
    /* JADX WARN: Code restructure failed: missing block: B:103:0x024b, code lost:
        if (r0 >= r2.length) goto L201;
     */
    /* JADX WARN: Code restructure failed: missing block: B:104:0x024d, code lost:
        r3 = r2[r0].setResultTo(r15).setOutActivity(r13).execute();
     */
    /* JADX WARN: Code restructure failed: missing block: B:105:0x025b, code lost:
        if (r3 >= 0) goto L125;
     */
    /* JADX WARN: Code restructure failed: missing block: B:106:0x025d, code lost:
        r4 = r0 + 1;
     */
    /* JADX WARN: Code restructure failed: missing block: B:108:0x0262, code lost:
        if (r4 >= r2.length) goto L182;
     */
    /* JADX WARN: Code restructure failed: missing block: B:110:0x0266, code lost:
        r18 = r6;
     */
    /* JADX WARN: Code restructure failed: missing block: B:111:0x0268, code lost:
        r28.mFactory.recycle(r2[r4]);
     */
    /* JADX WARN: Code restructure failed: missing block: B:112:0x026d, code lost:
        r4 = r4 + 1;
        r6 = r18;
     */
    /* JADX WARN: Code restructure failed: missing block: B:113:0x0274, code lost:
        r0 = th;
     */
    /* JADX WARN: Code restructure failed: missing block: B:116:0x027e, code lost:
        r4 = r28.mService.mWindowManager.mStartingSurfaceController;
     */
    /* JADX WARN: Code restructure failed: missing block: B:117:0x0284, code lost:
        if (r38 == null) goto L188;
     */
    /* JADX WARN: Code restructure failed: missing block: B:118:0x0286, code lost:
        r21 = r38.getOriginalOptions();
     */
    /* JADX WARN: Code restructure failed: missing block: B:119:0x028a, code lost:
        r4.endDeferAddStartingWindow(r21);
        r28.mService.continueWindowLayout();
     */
    /* JADX WARN: Code restructure failed: missing block: B:120:0x0294, code lost:
        monitor-exit(r14);
     */
    /* JADX WARN: Code restructure failed: missing block: B:121:0x0295, code lost:
        com.android.server.wm.WindowManagerService.resetPriorityAfterLockedSection();
        android.os.Binder.restoreCallingIdentity(r19);
     */
    /* JADX WARN: Code restructure failed: missing block: B:122:0x029b, code lost:
        return r3;
     */
    /* JADX WARN: Code restructure failed: missing block: B:123:0x029c, code lost:
        r0 = th;
     */
    /* JADX WARN: Code restructure failed: missing block: B:125:0x02a3, code lost:
        r0 = th;
     */
    /* JADX WARN: Code restructure failed: missing block: B:127:0x02ac, code lost:
        r18 = r6;
     */
    /* JADX WARN: Code restructure failed: missing block: B:128:0x02ae, code lost:
        r4 = r13[0];
     */
    /* JADX WARN: Code restructure failed: missing block: B:129:0x02b0, code lost:
        if (r4 == null) goto L129;
     */
    /* JADX WARN: Code restructure failed: missing block: B:131:0x02b6, code lost:
        r6 = r17;
     */
    /* JADX WARN: Code restructure failed: missing block: B:132:0x02b8, code lost:
        if (r4.getUid() != r6) goto L130;
     */
    /* JADX WARN: Code restructure failed: missing block: B:134:0x02bc, code lost:
        r26 = r2;
        r15 = r4.token;
     */
    /* JADX WARN: Code restructure failed: missing block: B:135:0x02c2, code lost:
        r0 = th;
     */
    /* JADX WARN: Code restructure failed: missing block: B:137:0x02c7, code lost:
        r0 = th;
     */
    /* JADX WARN: Code restructure failed: missing block: B:139:0x02ce, code lost:
        r6 = r17;
     */
    /* JADX WARN: Code restructure failed: missing block: B:140:0x02d0, code lost:
        r15 = r37;
     */
    /* JADX WARN: Code restructure failed: missing block: B:142:0x02d6, code lost:
        if (r0 >= (r2.length - 1)) goto L147;
     */
    /* JADX WARN: Code restructure failed: missing block: B:144:0x02e0, code lost:
        r26 = r2;
     */
    /* JADX WARN: Code restructure failed: missing block: B:145:0x02e4, code lost:
        r2[r0 + 1].getIntent().addFlags(268435456);
     */
    /* JADX WARN: Code restructure failed: missing block: B:147:0x02e8, code lost:
        r0 = th;
     */
    /* JADX WARN: Code restructure failed: missing block: B:149:0x02ea, code lost:
        r26 = r2;
     */
    /* JADX WARN: Code restructure failed: missing block: B:150:0x02ec, code lost:
        r0 = r0 + 1;
        r17 = r6;
        r6 = r18;
     */
    /* JADX WARN: Code restructure failed: missing block: B:151:0x02fc, code lost:
        r0 = th;
     */
    /* JADX WARN: Code restructure failed: missing block: B:153:0x0300, code lost:
        r0 = th;
     */
    /* JADX WARN: Code restructure failed: missing block: B:156:0x030c, code lost:
        r0 = r28.mService.mWindowManager.mStartingSurfaceController;
     */
    /* JADX WARN: Code restructure failed: missing block: B:157:0x0312, code lost:
        if (r38 == null) goto L206;
     */
    /* JADX WARN: Code restructure failed: missing block: B:158:0x0314, code lost:
        r21 = r38.getOriginalOptions();
     */
    /* JADX WARN: Code restructure failed: missing block: B:159:0x0318, code lost:
        r0.endDeferAddStartingWindow(r21);
        r28.mService.continueWindowLayout();
     */
    /* JADX WARN: Code restructure failed: missing block: B:160:0x0323, code lost:
        monitor-exit(r14);
     */
    /* JADX WARN: Code restructure failed: missing block: B:161:0x0324, code lost:
        com.android.server.wm.WindowManagerService.resetPriorityAfterLockedSection();
     */
    /* JADX WARN: Code restructure failed: missing block: B:162:0x0327, code lost:
        android.os.Binder.restoreCallingIdentity(r19);
     */
    /* JADX WARN: Code restructure failed: missing block: B:163:0x032b, code lost:
        return 0;
     */
    /* JADX WARN: Code restructure failed: missing block: B:164:0x032c, code lost:
        r0 = th;
     */
    /* JADX WARN: Code restructure failed: missing block: B:166:0x0333, code lost:
        r2 = r28.mService.mWindowManager.mStartingSurfaceController;
     */
    /* JADX WARN: Code restructure failed: missing block: B:167:0x0339, code lost:
        if (r38 != null) goto L144;
     */
    /* JADX WARN: Code restructure failed: missing block: B:168:0x033b, code lost:
        r21 = r38.getOriginalOptions();
     */
    /* JADX WARN: Code restructure failed: missing block: B:169:0x033f, code lost:
        r2.endDeferAddStartingWindow(r21);
        r28.mService.continueWindowLayout();
     */
    /* JADX WARN: Code restructure failed: missing block: B:170:0x034a, code lost:
        throw r0;
     */
    /* JADX WARN: Code restructure failed: missing block: B:171:0x034b, code lost:
        r0 = th;
     */
    /* JADX WARN: Code restructure failed: missing block: B:173:0x0352, code lost:
        monitor-exit(r14);
     */
    /* JADX WARN: Code restructure failed: missing block: B:174:0x0353, code lost:
        com.android.server.wm.WindowManagerService.resetPriorityAfterLockedSection();
     */
    /* JADX WARN: Code restructure failed: missing block: B:175:0x0356, code lost:
        throw r0;
     */
    /* JADX WARN: Code restructure failed: missing block: B:176:0x0357, code lost:
        r0 = th;
     */
    /* JADX WARN: Code restructure failed: missing block: B:178:0x0359, code lost:
        r0 = th;
     */
    /* JADX WARN: Code restructure failed: missing block: B:180:0x035b, code lost:
        r0 = th;
     */
    /* JADX WARN: Code restructure failed: missing block: B:81:0x01d1, code lost:
        r17 = r11;
        r26 = r14;
        r7 = r15;
     */
    /* JADX WARN: Code restructure failed: missing block: B:84:0x01e4, code lost:
        if (r7.size() <= 1) goto L112;
     */
    /* JADX WARN: Code restructure failed: missing block: B:85:0x01e6, code lost:
        r0 = new java.lang.StringBuilder("startActivities: different apps [");
        r12 = r7.size();
        r13 = 0;
     */
    /* JADX WARN: Code restructure failed: missing block: B:86:0x01f3, code lost:
        if (r13 >= r12) goto L110;
     */
    /* JADX WARN: Code restructure failed: missing block: B:87:0x01f5, code lost:
        r14 = r0.append(r7.valueAt(r13));
     */
    /* JADX WARN: Code restructure failed: missing block: B:88:0x0201, code lost:
        if (r13 != (r12 - 1)) goto L108;
     */
    /* JADX WARN: Code restructure failed: missing block: B:89:0x0203, code lost:
        r15 = "]";
     */
    /* JADX WARN: Code restructure failed: missing block: B:90:0x0206, code lost:
        r15 = ", ";
     */
    /* JADX WARN: Code restructure failed: missing block: B:91:0x0208, code lost:
        r14.append(r15);
        r13 = r13 + 1;
     */
    /* JADX WARN: Code restructure failed: missing block: B:92:0x020e, code lost:
        r0.append(" from ").append(r33);
        android.util.Slog.wtf(com.android.server.wm.ActivityStartController.TAG, r0.toString());
     */
    /* JADX WARN: Code restructure failed: missing block: B:97:0x022d, code lost:
        r13 = new com.android.server.wm.ActivityRecord[1];
        r14 = r28.mService.mGlobalLock;
     */
    /* JADX WARN: Code restructure failed: missing block: B:98:0x0233, code lost:
        monitor-enter(r14);
     */
    /* JADX WARN: Code restructure failed: missing block: B:99:0x0234, code lost:
        com.android.server.wm.WindowManagerService.boostPriorityForLockedSection();
        r28.mService.deferWindowLayout();
        r28.mService.mWindowManager.mStartingSurfaceController.beginDeferAddStartingWindow();
     */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Removed duplicated region for block: B:168:0x033b A[Catch: all -> 0x0359, TryCatch #18 {all -> 0x0359, blocks: (B:166:0x0333, B:168:0x033b, B:169:0x033f, B:170:0x034a, B:173:0x0352, B:156:0x030c, B:158:0x0314, B:159:0x0318, B:160:0x0323), top: B:225:0x0233 }] */
    /* JADX WARN: Type inference failed for: r21v7 */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public int startActivities(IApplicationThread caller, int callingUid, int incomingRealCallingPid, int incomingRealCallingUid, String callingPackage, String callingFeatureId, Intent[] intents, String[] resolvedTypes, IBinder resultTo, SafeActivityOptions options, int userId, String reason, PendingIntentRecord originatingPendingIntent, boolean allowBackgroundActivityStart) {
        int callingUid2;
        int callingPid;
        Intent[] intents2;
        ActivityStarter[] starters;
        int i;
        Intent intent;
        NeededUriGrants intentGrants;
        IApplicationThread iApplicationThread = caller;
        if (intents != null) {
            if (resolvedTypes != null) {
                if (intents.length == resolvedTypes.length) {
                    int filterCallingUid = incomingRealCallingPid != 0 ? incomingRealCallingPid : Binder.getCallingPid();
                    int realCallingUid = incomingRealCallingUid != -1 ? incomingRealCallingUid : Binder.getCallingUid();
                    if (callingUid >= 0) {
                        callingPid = -1;
                        callingUid2 = callingUid;
                    } else if (iApplicationThread == null) {
                        callingPid = filterCallingUid;
                        callingUid2 = realCallingUid;
                    } else {
                        callingUid2 = -1;
                        callingPid = -1;
                    }
                    int filterCallingUid2 = ActivityStarter.computeResolveFilterUid(callingUid2, realCallingUid, -10000);
                    SparseArray<String> startingUidPkgs = new SparseArray<>();
                    long origId = Binder.clearCallingIdentity();
                    try {
                        intents2 = (Intent[]) ArrayUtils.filterNotNull(intents, new IntFunction() { // from class: com.android.server.wm.ActivityStartController$$ExternalSyntheticLambda0
                            @Override // java.util.function.IntFunction
                            public final Object apply(int i2) {
                                return ActivityStartController.lambda$startActivities$0(i2);
                            }
                        });
                        try {
                            starters = new ActivityStarter[intents2.length];
                            i = 0;
                        } catch (Throwable th) {
                            th = th;
                        }
                    } catch (Throwable th2) {
                        th = th2;
                    }
                    while (true) {
                        if (i >= intents2.length) {
                            break;
                        }
                        try {
                            Intent intent2 = intents2[i];
                            if (intent2.hasFileDescriptors()) {
                                throw new IllegalArgumentException("File descriptors passed in Intent");
                            }
                            boolean componentSpecified = intent2.getComponent() != null;
                            Intent intent3 = new Intent(intent2);
                            int i2 = i;
                            ActivityStarter[] starters2 = starters;
                            SparseArray<String> startingUidPkgs2 = startingUidPkgs;
                            try {
                                ActivityInfo aInfo = this.mService.mAmInternal.getActivityInfoForUser(this.mSupervisor.resolveActivity(intent3, resolvedTypes[i], 0, null, userId, filterCallingUid2), userId);
                                if (aInfo != null) {
                                    try {
                                        try {
                                            intent = intent3;
                                            try {
                                                NeededUriGrants intentGrants2 = this.mSupervisor.mService.mUgmInternal.checkGrantUriPermissionFromIntent(intent, filterCallingUid2, aInfo.applicationInfo.packageName, UserHandle.getUserId(aInfo.applicationInfo.uid));
                                                if ((aInfo.applicationInfo.privateFlags & 2) != 0) {
                                                    throw new IllegalArgumentException("FLAG_CANT_SAVE_STATE not supported here");
                                                }
                                                startingUidPkgs2.put(aInfo.applicationInfo.uid, aInfo.applicationInfo.packageName);
                                                intentGrants = intentGrants2;
                                            } catch (SecurityException e) {
                                                Slog.d(TAG, "Not allowed to start activity since no uri permission.");
                                                Binder.restoreCallingIdentity(origId);
                                                return -96;
                                            }
                                        } catch (Throwable th3) {
                                            th = th3;
                                        }
                                    } catch (SecurityException e2) {
                                    }
                                } else {
                                    intent = intent3;
                                    intentGrants = null;
                                }
                                boolean top = i2 == intents2.length - 1;
                                SafeActivityOptions checkedOptions = top ? options : 0;
                                Intent[] intents3 = intents2;
                                int filterCallingUid3 = filterCallingUid2;
                                try {
                                    try {
                                        try {
                                            starters2[i2] = obtainStarter(intent, reason).setIntentGrants(intentGrants).setCaller(iApplicationThread).setResolvedType(resolvedTypes[i2]).setActivityInfo(aInfo).setRequestCode(-1).setCallingPid(callingPid).setCallingUid(callingUid2).setCallingPackage(callingPackage).setCallingFeatureId(callingFeatureId).setRealCallingPid(filterCallingUid).setRealCallingUid(realCallingUid).setActivityOptions(checkedOptions).setComponentSpecified(componentSpecified).setAllowPendingRemoteAnimationRegistryLookup(top).setOriginatingPendingIntent(originatingPendingIntent).setAllowBackgroundActivityStart(allowBackgroundActivityStart);
                                            i = i2 + 1;
                                            iApplicationThread = caller;
                                            startingUidPkgs = startingUidPkgs2;
                                            intents2 = intents3;
                                            filterCallingUid2 = filterCallingUid3;
                                            starters = starters2;
                                        } catch (Throwable th4) {
                                            th = th4;
                                            Binder.restoreCallingIdentity(origId);
                                            throw th;
                                        }
                                    } catch (Throwable th5) {
                                        th = th5;
                                        Binder.restoreCallingIdentity(origId);
                                        throw th;
                                    }
                                } catch (Throwable th6) {
                                    th = th6;
                                }
                            } catch (Throwable th7) {
                                th = th7;
                            }
                        } catch (Throwable th8) {
                            th = th8;
                        }
                        Binder.restoreCallingIdentity(origId);
                        throw th;
                    }
                }
                throw new IllegalArgumentException("intents are length different than resolvedTypes");
            }
            throw new NullPointerException("resolvedTypes is null");
        }
        throw new NullPointerException("intents is null");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ Intent[] lambda$startActivities$0(int x$0) {
        return new Intent[x$0];
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int startActivityInTaskFragment(TaskFragment taskFragment, Intent activityIntent, Bundle activityOptions, IBinder resultTo, int callingUid, int callingPid, IBinder errorCallbackToken) {
        ActivityRecord caller = resultTo != null ? ActivityRecord.forTokenLocked(resultTo) : null;
        return obtainStarter(activityIntent, "startActivityInTaskFragment").setActivityOptions(activityOptions).setInTaskFragment(taskFragment).setResultTo(resultTo).setRequestCode(-1).setCallingUid(callingUid).setCallingPid(callingPid).setRealCallingUid(callingUid).setRealCallingPid(callingPid).setUserId(caller != null ? caller.mUserId : this.mService.getCurrentUserId()).setErrorCallbackToken(errorCallbackToken).execute();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void registerRemoteAnimationForNextActivityStart(String packageName, RemoteAnimationAdapter adapter, IBinder launchCookie) {
        this.mPendingRemoteAnimationRegistry.addPendingAnimation(packageName, adapter, launchCookie);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public PendingRemoteAnimationRegistry getPendingRemoteAnimationRegistry() {
        return this.mPendingRemoteAnimationRegistry;
    }

    void dumpLastHomeActivityStartResult(PrintWriter pw, String prefix) {
        pw.print(prefix);
        pw.print("mLastHomeActivityStartResult=");
        pw.println(this.mLastHomeActivityStartResult);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(PrintWriter pw, String prefix, String dumpPackage) {
        ActivityRecord activityRecord;
        boolean dumped = false;
        boolean dump = false;
        boolean dumpPackagePresent = dumpPackage != null;
        ActivityRecord activityRecord2 = this.mLastHomeActivityStartRecord;
        if (activityRecord2 != null && (!dumpPackagePresent || dumpPackage.equals(activityRecord2.packageName))) {
            dumped = true;
            dumpLastHomeActivityStartResult(pw, prefix);
            pw.print(prefix);
            pw.println("mLastHomeActivityStartRecord:");
            this.mLastHomeActivityStartRecord.dump(pw, prefix + "  ", true);
        }
        ActivityStarter activityStarter = this.mLastStarter;
        if (activityStarter != null) {
            if (!dumpPackagePresent || activityStarter.relatedToPackage(dumpPackage) || ((activityRecord = this.mLastHomeActivityStartRecord) != null && dumpPackage.equals(activityRecord.packageName))) {
                dump = true;
            }
            if (dump) {
                if (!dumped) {
                    dumped = true;
                    dumpLastHomeActivityStartResult(pw, prefix);
                }
                pw.print(prefix);
                pw.println("mLastStarter:");
                this.mLastStarter.dump(pw, prefix + "  ");
                if (dumpPackagePresent) {
                    return;
                }
            }
        }
        if (!dumped) {
            pw.print(prefix);
            pw.println("(nothing)");
        }
    }
}
