package com.android.server.job.controllers;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.UserHandle;
import android.util.ArraySet;
import android.util.IndentingPrintWriter;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArrayMap;
import android.util.proto.ProtoOutputStream;
import com.android.server.job.JobSchedulerService;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
/* loaded from: classes.dex */
public class ComponentController extends StateController {
    private static final boolean DEBUG;
    private static final String TAG = "JobScheduler.Component";
    private final BroadcastReceiver mBroadcastReceiver;
    private final ComponentStateUpdateFunctor mComponentStateUpdateFunctor;
    private final SparseArrayMap<ComponentName, ServiceInfo> mServiceInfoCache;

    static {
        DEBUG = JobSchedulerService.DEBUG || Log.isLoggable(TAG, 3);
    }

    public ComponentController(JobSchedulerService service) {
        super(service);
        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { // from class: com.android.server.job.controllers.ComponentController.1
            /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                char c;
                String action = intent.getAction();
                if (action == null) {
                    Slog.wtf(ComponentController.TAG, "Intent action was null");
                    return;
                }
                switch (action.hashCode()) {
                    case -742246786:
                        if (action.equals("android.intent.action.USER_STOPPED")) {
                            c = 3;
                            break;
                        }
                        c = 65535;
                        break;
                    case 172491798:
                        if (action.equals("android.intent.action.PACKAGE_CHANGED")) {
                            c = 1;
                            break;
                        }
                        c = 65535;
                        break;
                    case 833559602:
                        if (action.equals("android.intent.action.USER_UNLOCKED")) {
                            c = 2;
                            break;
                        }
                        c = 65535;
                        break;
                    case 1544582882:
                        if (action.equals("android.intent.action.PACKAGE_ADDED")) {
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
                        if (intent.getBooleanExtra("android.intent.extra.REPLACING", false)) {
                            Uri uri = intent.getData();
                            String pkg = uri != null ? uri.getSchemeSpecificPart() : null;
                            if (pkg != null) {
                                int pkgUid = intent.getIntExtra("android.intent.extra.UID", -1);
                                int userId = UserHandle.getUserId(pkgUid);
                                ComponentController.this.updateComponentStateForPackage(userId, pkg);
                                return;
                            }
                            return;
                        }
                        return;
                    case 1:
                        Uri uri2 = intent.getData();
                        String pkg2 = uri2 != null ? uri2.getSchemeSpecificPart() : null;
                        String[] changedComponents = intent.getStringArrayExtra("android.intent.extra.changed_component_name_list");
                        if (pkg2 != null && changedComponents != null && changedComponents.length > 0) {
                            int pkgUid2 = intent.getIntExtra("android.intent.extra.UID", -1);
                            int userId2 = UserHandle.getUserId(pkgUid2);
                            ComponentController.this.updateComponentStateForPackage(userId2, pkg2);
                            return;
                        }
                        return;
                    case 2:
                    case 3:
                        int userId3 = intent.getIntExtra("android.intent.extra.user_handle", 0);
                        ComponentController.this.updateComponentStateForUser(userId3);
                        return;
                    default:
                        return;
                }
            }
        };
        this.mBroadcastReceiver = broadcastReceiver;
        this.mServiceInfoCache = new SparseArrayMap<>();
        this.mComponentStateUpdateFunctor = new ComponentStateUpdateFunctor();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.PACKAGE_ADDED");
        filter.addAction("android.intent.action.PACKAGE_CHANGED");
        filter.addDataScheme("package");
        this.mContext.registerReceiverAsUser(broadcastReceiver, UserHandle.ALL, filter, null, null);
        IntentFilter userFilter = new IntentFilter();
        userFilter.addAction("android.intent.action.USER_UNLOCKED");
        userFilter.addAction("android.intent.action.USER_STOPPED");
        this.mContext.registerReceiverAsUser(broadcastReceiver, UserHandle.ALL, userFilter, null, null);
    }

    @Override // com.android.server.job.controllers.StateController
    public void maybeStartTrackingJobLocked(JobStatus jobStatus, JobStatus lastJob) {
        updateComponentEnabledStateLocked(jobStatus);
    }

    @Override // com.android.server.job.controllers.StateController
    public void maybeStopTrackingJobLocked(JobStatus jobStatus, JobStatus incomingJob, boolean forUpdate) {
    }

    @Override // com.android.server.job.controllers.StateController
    public void onAppRemovedLocked(String packageName, int uid) {
        clearComponentsForPackageLocked(UserHandle.getUserId(uid), packageName);
    }

    @Override // com.android.server.job.controllers.StateController
    public void onUserRemovedLocked(int userId) {
        this.mServiceInfoCache.delete(userId);
    }

    private ServiceInfo getServiceInfoLocked(JobStatus jobStatus) {
        ServiceInfo si;
        ComponentName service = jobStatus.getServiceComponent();
        int userId = jobStatus.getUserId();
        if (this.mServiceInfoCache.contains(userId, service)) {
            return (ServiceInfo) this.mServiceInfoCache.get(userId, service);
        }
        try {
            si = this.mContext.createContextAsUser(UserHandle.of(userId), 0).getPackageManager().getServiceInfo(service, 268435456);
        } catch (PackageManager.NameNotFoundException e) {
            if (this.mService.areUsersStartedLocked(jobStatus)) {
                Slog.e(TAG, "Job exists for non-existent package: " + service.getPackageName());
            }
            si = null;
        }
        this.mServiceInfoCache.add(userId, service, si);
        return si;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean updateComponentEnabledStateLocked(JobStatus jobStatus) {
        ServiceInfo service = getServiceInfoLocked(jobStatus);
        if (DEBUG && service == null) {
            Slog.v(TAG, jobStatus.toShortString() + " component not present");
        }
        ServiceInfo ogService = jobStatus.serviceInfo;
        jobStatus.serviceInfo = service;
        return !Objects.equals(ogService, service);
    }

    private void clearComponentsForPackageLocked(int userId, String pkg) {
        int uIdx = this.mServiceInfoCache.indexOfKey(userId);
        for (int c = this.mServiceInfoCache.numElementsForKey(userId) - 1; c >= 0; c--) {
            ComponentName cn = (ComponentName) this.mServiceInfoCache.keyAt(uIdx, c);
            if (cn.getPackageName().equals(pkg)) {
                this.mServiceInfoCache.delete(userId, cn);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateComponentStateForPackage(final int userId, final String pkg) {
        synchronized (this.mLock) {
            clearComponentsForPackageLocked(userId, pkg);
            updateComponentStatesLocked(new Predicate() { // from class: com.android.server.job.controllers.ComponentController$$ExternalSyntheticLambda0
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return ComponentController.lambda$updateComponentStateForPackage$0(userId, pkg, (JobStatus) obj);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$updateComponentStateForPackage$0(int userId, String pkg, JobStatus jobStatus) {
        return jobStatus.getUserId() == userId && jobStatus.getServiceComponent().getPackageName().equals(pkg);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateComponentStateForUser(final int userId) {
        synchronized (this.mLock) {
            this.mServiceInfoCache.delete(userId);
            updateComponentStatesLocked(new Predicate() { // from class: com.android.server.job.controllers.ComponentController$$ExternalSyntheticLambda1
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return ComponentController.lambda$updateComponentStateForUser$1(userId, (JobStatus) obj);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$updateComponentStateForUser$1(int userId, JobStatus jobStatus) {
        return jobStatus.getUserId() == userId;
    }

    private void updateComponentStatesLocked(Predicate<JobStatus> filter) {
        this.mComponentStateUpdateFunctor.reset();
        this.mService.getJobStore().forEachJob(filter, this.mComponentStateUpdateFunctor);
        if (this.mComponentStateUpdateFunctor.mChangedJobs.size() > 0) {
            this.mStateChangedListener.onControllerStateChanged(this.mComponentStateUpdateFunctor.mChangedJobs);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public final class ComponentStateUpdateFunctor implements Consumer<JobStatus> {
        final ArraySet<JobStatus> mChangedJobs = new ArraySet<>();

        ComponentStateUpdateFunctor() {
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // java.util.function.Consumer
        public void accept(JobStatus jobStatus) {
            if (ComponentController.this.updateComponentEnabledStateLocked(jobStatus)) {
                this.mChangedJobs.add(jobStatus);
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void reset() {
            this.mChangedJobs.clear();
        }
    }

    @Override // com.android.server.job.controllers.StateController
    public void dumpControllerStateLocked(IndentingPrintWriter pw, Predicate<JobStatus> predicate) {
        for (int u = 0; u < this.mServiceInfoCache.numMaps(); u++) {
            int userId = this.mServiceInfoCache.keyAt(u);
            for (int p = 0; p < this.mServiceInfoCache.numElementsForKey(userId); p++) {
                ComponentName componentName = (ComponentName) this.mServiceInfoCache.keyAt(u, p);
                pw.print(userId);
                pw.print("-");
                pw.print(componentName);
                pw.print(": ");
                pw.print(this.mServiceInfoCache.valueAt(u, p));
                pw.println();
            }
        }
    }

    @Override // com.android.server.job.controllers.StateController
    public void dumpControllerStateLocked(ProtoOutputStream proto, long fieldId, Predicate<JobStatus> predicate) {
    }
}
