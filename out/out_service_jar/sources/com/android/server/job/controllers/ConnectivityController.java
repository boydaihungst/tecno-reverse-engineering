package com.android.server.job.controllers;

import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkPolicyManager;
import android.net.NetworkRequest;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.UserHandle;
import android.telephony.CellSignalStrength;
import android.telephony.SignalStrength;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.DataUnit;
import android.util.IndentingPrintWriter;
import android.util.Log;
import android.util.Pools;
import android.util.Slog;
import android.util.SparseArray;
import android.util.TimeUtils;
import android.util.proto.ProtoOutputStream;
import com.android.server.JobSchedulerBackgroundThread;
import com.android.server.LocalServices;
import com.android.server.job.JobSchedulerService;
import com.android.server.net.NetworkPolicyManagerInternal;
import com.android.server.net.watchlist.WatchlistLoggingHandler;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
/* loaded from: classes.dex */
public final class ConnectivityController extends RestrictingController implements ConnectivityManager.OnNetworkActiveListener {
    private static final boolean DEBUG;
    private static final int MAX_NETWORK_CALLBACKS = 125;
    private static final long MIN_ADJUST_CALLBACK_INTERVAL_MS = 1000;
    private static final long MIN_STATS_UPDATE_INTERVAL_MS = 30000;
    private static final int MSG_ADJUST_CALLBACKS = 0;
    private static final int MSG_UPDATE_ALL_TRACKED_JOBS = 1;
    private static final String TAG = "JobScheduler.Connectivity";
    private static final int UNBYPASSABLE_BG_BLOCKED_REASONS = -1;
    private static final int UNBYPASSABLE_EJ_BLOCKED_REASONS = -8;
    private static final int UNBYPASSABLE_FOREGROUND_BLOCKED_REASONS = -196616;
    private final ArrayMap<Network, NetworkCapabilities> mAvailableNetworks;
    private final ConnectivityManager mConnManager;
    private final SparseArray<UidDefaultNetworkCallback> mCurrentDefaultNetworkCallbacks;
    private final Pools.Pool<UidDefaultNetworkCallback> mDefaultNetworkCallbackPool;
    private final Handler mHandler;
    private long mLastAllJobUpdateTimeElapsed;
    private long mLastCallbackAdjustmentTimeElapsed;
    private final NetworkPolicyManagerInternal mNetPolicyManagerInternal;
    private final ConnectivityManager.NetworkCallback mNetworkCallback;
    private final SparseArray<ArraySet<JobStatus>> mRequestedWhitelistJobs;
    private final SparseArray<CellSignalStrengthCallback> mSignalStrengths;
    private final List<UidStats> mSortedStats;
    private final SparseArray<ArraySet<JobStatus>> mTrackedJobs;
    private final SparseArray<UidStats> mUidStats;
    private final Comparator<UidStats> mUidStatsComparator;

    static {
        DEBUG = JobSchedulerService.DEBUG || Log.isLoggable(TAG, 3);
    }

    public ConnectivityController(JobSchedulerService service) {
        super(service);
        this.mTrackedJobs = new SparseArray<>();
        this.mRequestedWhitelistJobs = new SparseArray<>();
        this.mAvailableNetworks = new ArrayMap<>();
        this.mCurrentDefaultNetworkCallbacks = new SparseArray<>();
        this.mUidStatsComparator = new Comparator<UidStats>() { // from class: com.android.server.job.controllers.ConnectivityController.1
            private int prioritizeExistenceOver(int threshold, int v1, int v2) {
                if (v1 <= threshold || v2 <= threshold) {
                    if (v1 <= threshold && v2 <= threshold) {
                        return 0;
                    }
                    if (v1 > threshold) {
                        return -1;
                    }
                    return 1;
                }
                return 0;
            }

            /* JADX DEBUG: Method merged with bridge method */
            @Override // java.util.Comparator
            public int compare(UidStats us1, UidStats us2) {
                int runningPriority = prioritizeExistenceOver(0, us1.runningJobs.size(), us2.runningJobs.size());
                if (runningPriority == 0) {
                    int readyWithConnPriority = prioritizeExistenceOver(0, us1.numReadyWithConnectivity, us2.numReadyWithConnectivity);
                    if (readyWithConnPriority == 0) {
                        int reqAvailPriority = prioritizeExistenceOver(0, us1.numRequestedNetworkAvailable, us2.numRequestedNetworkAvailable);
                        if (reqAvailPriority == 0) {
                            int topPriority = prioritizeExistenceOver(39, us1.baseBias, us2.baseBias);
                            if (topPriority == 0) {
                                int ejPriority = prioritizeExistenceOver(0, us1.numEJs, us2.numEJs);
                                if (ejPriority == 0) {
                                    int fgsPriority = prioritizeExistenceOver(34, us1.baseBias, us2.baseBias);
                                    if (fgsPriority != 0) {
                                        return fgsPriority;
                                    }
                                    if (us1.earliestEJEnqueueTime < us2.earliestEJEnqueueTime) {
                                        return -1;
                                    }
                                    if (us1.earliestEJEnqueueTime > us2.earliestEJEnqueueTime) {
                                        return 1;
                                    }
                                    if (us1.baseBias != us2.baseBias) {
                                        return us2.baseBias - us1.baseBias;
                                    }
                                    if (us1.earliestEnqueueTime < us2.earliestEnqueueTime) {
                                        return -1;
                                    }
                                    return us1.earliestEnqueueTime > us2.earliestEnqueueTime ? 1 : 0;
                                }
                                return ejPriority;
                            }
                            return topPriority;
                        }
                        return reqAvailPriority;
                    }
                    return readyWithConnPriority;
                }
                return runningPriority;
            }
        };
        this.mUidStats = new SparseArray<>();
        this.mDefaultNetworkCallbackPool = new Pools.SimplePool(125);
        this.mSortedStats = new ArrayList();
        this.mSignalStrengths = new SparseArray<>();
        ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() { // from class: com.android.server.job.controllers.ConnectivityController.2
            @Override // android.net.ConnectivityManager.NetworkCallback
            public void onAvailable(Network network) {
                if (ConnectivityController.DEBUG) {
                    Slog.v(ConnectivityController.TAG, "onAvailable: " + network);
                }
            }

            @Override // android.net.ConnectivityManager.NetworkCallback
            public void onCapabilitiesChanged(Network network, NetworkCapabilities capabilities) {
                if (ConnectivityController.DEBUG) {
                    Slog.v(ConnectivityController.TAG, "onCapabilitiesChanged: " + network);
                }
                synchronized (ConnectivityController.this.mLock) {
                    NetworkCapabilities oldCaps = (NetworkCapabilities) ConnectivityController.this.mAvailableNetworks.put(network, capabilities);
                    if (oldCaps != null) {
                        maybeUnregisterSignalStrengthCallbackLocked(oldCaps);
                    }
                    maybeRegisterSignalStrengthCallbackLocked(capabilities);
                    ConnectivityController.this.updateTrackedJobsLocked(-1, network);
                    ConnectivityController.this.postAdjustCallbacks();
                }
            }

            @Override // android.net.ConnectivityManager.NetworkCallback
            public void onLost(Network network) {
                if (ConnectivityController.DEBUG) {
                    Slog.v(ConnectivityController.TAG, "onLost: " + network);
                }
                synchronized (ConnectivityController.this.mLock) {
                    NetworkCapabilities capabilities = (NetworkCapabilities) ConnectivityController.this.mAvailableNetworks.remove(network);
                    if (capabilities != null) {
                        maybeUnregisterSignalStrengthCallbackLocked(capabilities);
                    }
                    for (int u = 0; u < ConnectivityController.this.mCurrentDefaultNetworkCallbacks.size(); u++) {
                        UidDefaultNetworkCallback callback = (UidDefaultNetworkCallback) ConnectivityController.this.mCurrentDefaultNetworkCallbacks.valueAt(u);
                        if (Objects.equals(callback.mDefaultNetwork, network)) {
                            callback.mDefaultNetwork = null;
                        }
                    }
                    ConnectivityController.this.updateTrackedJobsLocked(-1, network);
                    ConnectivityController.this.postAdjustCallbacks();
                }
            }

            private void maybeRegisterSignalStrengthCallbackLocked(NetworkCapabilities capabilities) {
                if (!capabilities.hasTransport(0)) {
                    return;
                }
                TelephonyManager telephonyManager = (TelephonyManager) ConnectivityController.this.mContext.getSystemService(TelephonyManager.class);
                Set<Integer> subscriptionIds = capabilities.getSubscriptionIds();
                for (Integer num : subscriptionIds) {
                    int subId = num.intValue();
                    if (ConnectivityController.this.mSignalStrengths.indexOfKey(subId) < 0) {
                        TelephonyManager idTm = telephonyManager.createForSubscriptionId(subId);
                        CellSignalStrengthCallback callback = new CellSignalStrengthCallback();
                        idTm.registerTelephonyCallback(JobSchedulerBackgroundThread.getExecutor(), callback);
                        ConnectivityController.this.mSignalStrengths.put(subId, callback);
                        SignalStrength signalStrength = idTm.getSignalStrength();
                        if (signalStrength != null) {
                            callback.signalStrength = signalStrength.getLevel();
                        }
                    }
                }
            }

            private void maybeUnregisterSignalStrengthCallbackLocked(NetworkCapabilities capabilities) {
                if (!capabilities.hasTransport(0)) {
                    return;
                }
                ArraySet<Integer> activeIds = new ArraySet<>();
                int size = ConnectivityController.this.mAvailableNetworks.size();
                for (int i = 0; i < size; i++) {
                    NetworkCapabilities nc = (NetworkCapabilities) ConnectivityController.this.mAvailableNetworks.valueAt(i);
                    if (nc.hasTransport(0)) {
                        activeIds.addAll(nc.getSubscriptionIds());
                    }
                }
                if (ConnectivityController.DEBUG) {
                    Slog.d(ConnectivityController.TAG, "Active subscription IDs: " + activeIds);
                }
                TelephonyManager telephonyManager = (TelephonyManager) ConnectivityController.this.mContext.getSystemService(TelephonyManager.class);
                Set<Integer> subscriptionIds = capabilities.getSubscriptionIds();
                for (Integer num : subscriptionIds) {
                    int subId = num.intValue();
                    if (!activeIds.contains(Integer.valueOf(subId))) {
                        TelephonyManager idTm = telephonyManager.createForSubscriptionId(subId);
                        CellSignalStrengthCallback callback = (CellSignalStrengthCallback) ConnectivityController.this.mSignalStrengths.removeReturnOld(subId);
                        if (callback != null) {
                            idTm.unregisterTelephonyCallback(callback);
                        } else {
                            Slog.wtf(ConnectivityController.TAG, "Callback for sub " + subId + " didn't exist?!?!");
                        }
                    }
                }
            }
        };
        this.mNetworkCallback = networkCallback;
        this.mHandler = new CcHandler(this.mContext.getMainLooper());
        ConnectivityManager connectivityManager = (ConnectivityManager) this.mContext.getSystemService(ConnectivityManager.class);
        this.mConnManager = connectivityManager;
        this.mNetPolicyManagerInternal = (NetworkPolicyManagerInternal) LocalServices.getService(NetworkPolicyManagerInternal.class);
        NetworkRequest request = new NetworkRequest.Builder().clearCapabilities().build();
        connectivityManager.registerNetworkCallback(request, networkCallback);
    }

    @Override // com.android.server.job.controllers.StateController
    public void maybeStartTrackingJobLocked(JobStatus jobStatus, JobStatus lastJob) {
        if (jobStatus.hasConnectivityConstraint()) {
            UidStats uidStats = getUidStats(jobStatus.getSourceUid(), jobStatus.getSourcePackageName(), false);
            if (wouldBeReadyWithConstraintLocked(jobStatus, 268435456)) {
                uidStats.numReadyWithConnectivity++;
            }
            ArraySet<JobStatus> jobs = this.mTrackedJobs.get(jobStatus.getSourceUid());
            if (jobs == null) {
                jobs = new ArraySet<>();
                this.mTrackedJobs.put(jobStatus.getSourceUid(), jobs);
            }
            jobs.add(jobStatus);
            jobStatus.setTrackingController(2);
            updateConstraintsSatisfied(jobStatus);
        }
    }

    @Override // com.android.server.job.controllers.StateController
    public void prepareForExecutionLocked(JobStatus jobStatus) {
        if (jobStatus.hasConnectivityConstraint()) {
            UidStats uidStats = getUidStats(jobStatus.getSourceUid(), jobStatus.getSourcePackageName(), true);
            uidStats.runningJobs.add(jobStatus);
        }
    }

    @Override // com.android.server.job.controllers.StateController
    public void unprepareFromExecutionLocked(JobStatus jobStatus) {
        if (jobStatus.hasConnectivityConstraint()) {
            UidStats uidStats = getUidStats(jobStatus.getSourceUid(), jobStatus.getSourcePackageName(), true);
            uidStats.runningJobs.remove(jobStatus);
            postAdjustCallbacks();
        }
    }

    @Override // com.android.server.job.controllers.StateController
    public void maybeStopTrackingJobLocked(JobStatus jobStatus, JobStatus incomingJob, boolean forUpdate) {
        if (jobStatus.clearTrackingController(2)) {
            ArraySet<JobStatus> jobs = this.mTrackedJobs.get(jobStatus.getSourceUid());
            if (jobs != null) {
                jobs.remove(jobStatus);
            }
            UidStats uidStats = getUidStats(jobStatus.getSourceUid(), jobStatus.getSourcePackageName(), true);
            uidStats.numReadyWithConnectivity--;
            uidStats.runningJobs.remove(jobStatus);
            maybeRevokeStandbyExceptionLocked(jobStatus);
            postAdjustCallbacks();
        }
    }

    @Override // com.android.server.job.controllers.RestrictingController
    public void startTrackingRestrictedJobLocked(JobStatus jobStatus) {
        if (jobStatus.hasConnectivityConstraint()) {
            updateConstraintsSatisfied(jobStatus);
        }
    }

    @Override // com.android.server.job.controllers.RestrictingController
    public void stopTrackingRestrictedJobLocked(JobStatus jobStatus) {
        if (jobStatus.hasConnectivityConstraint()) {
            updateConstraintsSatisfied(jobStatus);
        }
    }

    private UidStats getUidStats(int uid, String packageName, boolean shouldExist) {
        UidStats us = this.mUidStats.get(uid);
        if (us == null) {
            if (shouldExist) {
                Slog.wtfStack(TAG, "UidStats was null after job for " + packageName + " was registered");
            }
            UidStats us2 = new UidStats(uid);
            this.mUidStats.append(uid, us2);
            return us2;
        }
        return us;
    }

    public boolean isNetworkAvailable(JobStatus job) {
        synchronized (this.mLock) {
            for (int i = 0; i < this.mAvailableNetworks.size(); i++) {
                Network network = this.mAvailableNetworks.keyAt(i);
                NetworkCapabilities capabilities = this.mAvailableNetworks.valueAt(i);
                boolean satisfied = isSatisfied(job, network, capabilities, this.mConstants);
                if (DEBUG) {
                    Slog.v(TAG, "isNetworkAvailable(" + job + ") with network " + network + " and capabilities " + capabilities + ". Satisfied=" + satisfied);
                }
                if (satisfied) {
                    return true;
                }
            }
            return false;
        }
    }

    void requestStandbyExceptionLocked(JobStatus job) {
        int uid = job.getSourceUid();
        boolean isExceptionRequested = isStandbyExceptionRequestedLocked(uid);
        ArraySet<JobStatus> jobs = this.mRequestedWhitelistJobs.get(uid);
        if (jobs == null) {
            jobs = new ArraySet<>();
            this.mRequestedWhitelistJobs.put(uid, jobs);
        }
        if (!jobs.add(job) || isExceptionRequested) {
            if (DEBUG) {
                Slog.i(TAG, "requestStandbyExceptionLocked found exception already requested.");
                return;
            }
            return;
        }
        if (DEBUG) {
            Slog.i(TAG, "Requesting standby exception for UID: " + uid);
        }
        this.mNetPolicyManagerInternal.setAppIdleWhitelist(uid, true);
    }

    boolean isStandbyExceptionRequestedLocked(int uid) {
        ArraySet jobs = this.mRequestedWhitelistJobs.get(uid);
        return jobs != null && jobs.size() > 0;
    }

    @Override // com.android.server.job.controllers.StateController
    public void evaluateStateLocked(JobStatus jobStatus) {
        if (!jobStatus.hasConnectivityConstraint()) {
            return;
        }
        UidStats uidStats = getUidStats(jobStatus.getSourceUid(), jobStatus.getSourcePackageName(), true);
        if (jobStatus.shouldTreatAsExpeditedJob()) {
            if (!jobStatus.isConstraintSatisfied(268435456)) {
                updateConstraintsSatisfied(jobStatus);
            }
        } else if (jobStatus.isRequestedExpeditedJob() && jobStatus.isConstraintSatisfied(268435456)) {
            updateConstraintsSatisfied(jobStatus);
        }
        if (wouldBeReadyWithConstraintLocked(jobStatus, 268435456) && isNetworkAvailable(jobStatus)) {
            if (DEBUG) {
                Slog.i(TAG, "evaluateStateLocked finds job " + jobStatus + " would be ready.");
            }
            uidStats.numReadyWithConnectivity++;
            requestStandbyExceptionLocked(jobStatus);
            return;
        }
        if (DEBUG) {
            Slog.i(TAG, "evaluateStateLocked finds job " + jobStatus + " would not be ready.");
        }
        maybeRevokeStandbyExceptionLocked(jobStatus);
    }

    @Override // com.android.server.job.controllers.StateController
    public void reevaluateStateLocked(int uid) {
        ArraySet<JobStatus> jobs = this.mTrackedJobs.get(uid);
        if (jobs == null) {
            return;
        }
        for (int i = jobs.size() - 1; i >= 0; i--) {
            evaluateStateLocked(jobs.valueAt(i));
        }
    }

    void maybeRevokeStandbyExceptionLocked(JobStatus job) {
        int uid = job.getSourceUid();
        if (!isStandbyExceptionRequestedLocked(uid)) {
            return;
        }
        ArraySet<JobStatus> jobs = this.mRequestedWhitelistJobs.get(uid);
        if (jobs == null) {
            Slog.wtf(TAG, "maybeRevokeStandbyExceptionLocked found null jobs array even though a standby exception has been requested.");
        } else if (!jobs.remove(job) || jobs.size() > 0) {
            if (DEBUG) {
                Slog.i(TAG, "maybeRevokeStandbyExceptionLocked not revoking because there are still " + jobs.size() + " jobs left.");
            }
        } else {
            revokeStandbyExceptionLocked(uid);
        }
    }

    private void revokeStandbyExceptionLocked(int uid) {
        if (DEBUG) {
            Slog.i(TAG, "Revoking standby exception for UID: " + uid);
        }
        this.mNetPolicyManagerInternal.setAppIdleWhitelist(uid, false);
        this.mRequestedWhitelistJobs.remove(uid);
    }

    @Override // com.android.server.job.controllers.StateController
    public void onAppRemovedLocked(String pkgName, int uid) {
        if (this.mService.getPackagesForUidLocked(uid) == null) {
            this.mTrackedJobs.delete(uid);
            UidStats uidStats = (UidStats) this.mUidStats.removeReturnOld(uid);
            unregisterDefaultNetworkCallbackLocked(uid, JobSchedulerService.sElapsedRealtimeClock.millis());
            this.mSortedStats.remove(uidStats);
            registerPendingUidCallbacksLocked();
        }
    }

    @Override // com.android.server.job.controllers.StateController
    public void onUserRemovedLocked(int userId) {
        long nowElapsed = JobSchedulerService.sElapsedRealtimeClock.millis();
        for (int u = this.mUidStats.size() - 1; u >= 0; u--) {
            UidStats uidStats = this.mUidStats.valueAt(u);
            if (UserHandle.getUserId(uidStats.uid) == userId) {
                unregisterDefaultNetworkCallbackLocked(uidStats.uid, nowElapsed);
                this.mSortedStats.remove(uidStats);
                this.mUidStats.removeAt(u);
            }
        }
        postAdjustCallbacks();
    }

    @Override // com.android.server.job.controllers.StateController
    public void onUidBiasChangedLocked(int uid, int prevBias, int newBias) {
        UidStats uidStats = this.mUidStats.get(uid);
        if (uidStats != null && uidStats.baseBias != newBias) {
            uidStats.baseBias = newBias;
            postAdjustCallbacks();
        }
    }

    @Override // com.android.server.job.controllers.StateController
    public void onBatteryStateChangedLocked() {
        this.mHandler.sendEmptyMessage(1);
    }

    private boolean isUsable(NetworkCapabilities capabilities) {
        return capabilities != null && capabilities.hasCapability(21);
    }

    private boolean isInsane(JobStatus jobStatus, Network network, NetworkCapabilities capabilities, JobSchedulerService.Constants constants) {
        long maxJobExecutionTimeMs = this.mService.getMaxJobExecutionTimeMs(jobStatus);
        long minimumChunkBytes = jobStatus.getMinimumNetworkChunkBytes();
        if (minimumChunkBytes != -1) {
            long bandwidthDown = capabilities.getLinkDownstreamBandwidthKbps();
            if (bandwidthDown > 0) {
                long estimatedMillis = (minimumChunkBytes * 1000) / (DataUnit.KIBIBYTES.toBytes(bandwidthDown) / 8);
                if (estimatedMillis > maxJobExecutionTimeMs) {
                    Slog.w(TAG, "Minimum chunk " + minimumChunkBytes + " bytes over " + bandwidthDown + " kbps network would take " + estimatedMillis + "ms and job has " + maxJobExecutionTimeMs + "ms to run; that's insane!");
                    return true;
                }
            }
            long bandwidthUp = capabilities.getLinkUpstreamBandwidthKbps();
            if (bandwidthUp > 0) {
                long bandwidthDown2 = (1000 * minimumChunkBytes) / (DataUnit.KIBIBYTES.toBytes(bandwidthUp) / 8);
                if (bandwidthDown2 > maxJobExecutionTimeMs) {
                    Slog.w(TAG, "Minimum chunk " + minimumChunkBytes + " bytes over " + bandwidthUp + " kbps network would take " + bandwidthDown2 + "ms and job has " + maxJobExecutionTimeMs + "ms to run; that's insane!");
                    return true;
                }
                return false;
            }
            return false;
        } else if (capabilities.hasCapability(11) && this.mService.isBatteryCharging()) {
            return false;
        } else {
            long downloadBytes = jobStatus.getEstimatedNetworkDownloadBytes();
            if (downloadBytes != -1) {
                long bandwidth = capabilities.getLinkDownstreamBandwidthKbps();
                if (bandwidth > 0) {
                    long estimatedMillis2 = (downloadBytes * 1000) / (DataUnit.KIBIBYTES.toBytes(bandwidth) / 8);
                    if (estimatedMillis2 > maxJobExecutionTimeMs) {
                        Slog.w(TAG, "Estimated " + downloadBytes + " download bytes over " + bandwidth + " kbps network would take " + estimatedMillis2 + "ms and job has " + maxJobExecutionTimeMs + "ms to run; that's insane!");
                        return true;
                    }
                }
            }
            long uploadBytes = jobStatus.getEstimatedNetworkUploadBytes();
            if (uploadBytes != -1) {
                long bandwidth2 = capabilities.getLinkUpstreamBandwidthKbps();
                if (bandwidth2 > 0) {
                    long minimumChunkBytes2 = (1000 * uploadBytes) / (DataUnit.KIBIBYTES.toBytes(bandwidth2) / 8);
                    if (minimumChunkBytes2 > maxJobExecutionTimeMs) {
                        Slog.w(TAG, "Estimated " + uploadBytes + " upload bytes over " + bandwidth2 + " kbps network would take " + minimumChunkBytes2 + "ms and job has " + maxJobExecutionTimeMs + "ms to run; that's insane!");
                        return true;
                    }
                    return false;
                }
                return false;
            }
            return false;
        }
    }

    private static boolean isCongestionDelayed(JobStatus jobStatus, Network network, NetworkCapabilities capabilities, JobSchedulerService.Constants constants) {
        return !capabilities.hasCapability(20) && jobStatus.getFractionRunTime() < constants.CONN_CONGESTION_DELAY_FRAC;
    }

    private boolean isStrongEnough(JobStatus jobStatus, NetworkCapabilities capabilities, JobSchedulerService.Constants constants) {
        int priority = jobStatus.getEffectivePriority();
        if (priority >= 400 || !constants.CONN_USE_CELL_SIGNAL_STRENGTH || !capabilities.hasTransport(0) || capabilities.hasTransport(4)) {
            return true;
        }
        int signalStrength = 0;
        Set<Integer> subscriptionIds = capabilities.getSubscriptionIds();
        for (Integer num : subscriptionIds) {
            int subId = num.intValue();
            CellSignalStrengthCallback callback = this.mSignalStrengths.get(subId);
            if (callback != null) {
                signalStrength = Math.max(signalStrength, callback.signalStrength);
            } else {
                Slog.wtf(TAG, "Subscription ID " + subId + " doesn't have a registered callback");
            }
        }
        if (DEBUG) {
            Slog.d(TAG, "Cell signal strength for job=" + signalStrength);
        }
        if (signalStrength <= 1) {
            if (priority > 300) {
                return true;
            }
            if (priority < 300) {
                return false;
            }
            if ((this.mService.isBatteryCharging() && this.mService.isBatteryNotLow()) || jobStatus.getFractionRunTime() > constants.CONN_PREFETCH_RELAX_FRAC) {
                return true;
            }
            return false;
        } else if (signalStrength > 2 || priority >= 200) {
            return true;
        } else {
            if (this.mService.isBatteryCharging() && this.mService.isBatteryNotLow()) {
                return true;
            }
            UidStats uidStats = getUidStats(jobStatus.getSourceUid(), jobStatus.getSourcePackageName(), true);
            return uidStats.runningJobs.contains(jobStatus);
        }
    }

    private static NetworkCapabilities.Builder copyCapabilities(NetworkRequest request) {
        int[] transportTypes;
        int[] capabilities;
        NetworkCapabilities.Builder builder = new NetworkCapabilities.Builder();
        for (int transport : request.getTransportTypes()) {
            builder.addTransportType(transport);
        }
        for (int capability : request.getCapabilities()) {
            builder.addCapability(capability);
        }
        return builder;
    }

    private static boolean isStrictSatisfied(JobStatus jobStatus, Network network, NetworkCapabilities capabilities, JobSchedulerService.Constants constants) {
        if (jobStatus.getEffectiveStandbyBucket() == 5 && (!jobStatus.isConstraintSatisfied(16777216) || !jobStatus.isConstraintSatisfied(134217728))) {
            NetworkCapabilities.Builder builder = copyCapabilities(jobStatus.getJob().getRequiredNetwork());
            builder.addCapability(11);
            return builder.build().satisfiedByNetworkCapabilities(capabilities);
        }
        return jobStatus.getJob().getRequiredNetwork().canBeSatisfiedBy(capabilities);
    }

    private boolean isRelaxedSatisfied(JobStatus jobStatus, Network network, NetworkCapabilities capabilities, JobSchedulerService.Constants constants) {
        if (jobStatus.getJob().isPrefetch() && jobStatus.getStandbyBucket() != 5) {
            long estDownloadBytes = jobStatus.getEstimatedNetworkDownloadBytes();
            if (estDownloadBytes <= 0) {
                return false;
            }
            NetworkCapabilities.Builder builder = copyCapabilities(jobStatus.getJob().getRequiredNetwork());
            builder.removeCapability(11);
            if (builder.build().satisfiedByNetworkCapabilities(capabilities) && jobStatus.getFractionRunTime() > constants.CONN_PREFETCH_RELAX_FRAC) {
                long opportunisticQuotaBytes = this.mNetPolicyManagerInternal.getSubscriptionOpportunisticQuota(network, 1);
                long estUploadBytes = jobStatus.getEstimatedNetworkUploadBytes();
                long estimatedBytes = (estUploadBytes != -1 ? estUploadBytes : 0L) + estDownloadBytes;
                return opportunisticQuotaBytes >= estimatedBytes;
            }
            return false;
        }
        return false;
    }

    boolean isSatisfied(JobStatus jobStatus, Network network, NetworkCapabilities capabilities, JobSchedulerService.Constants constants) {
        if (network == null || capabilities == null || !isUsable(capabilities) || isInsane(jobStatus, network, capabilities, constants) || isCongestionDelayed(jobStatus, network, capabilities, constants) || !isStrongEnough(jobStatus, capabilities, constants)) {
            return false;
        }
        return isStrictSatisfied(jobStatus, network, capabilities, constants) || isRelaxedSatisfied(jobStatus, network, capabilities, constants);
    }

    private void maybeRegisterDefaultNetworkCallbackLocked(JobStatus jobStatus) {
        int sourceUid = jobStatus.getSourceUid();
        if (this.mCurrentDefaultNetworkCallbacks.contains(sourceUid)) {
            return;
        }
        UidStats uidStats = getUidStats(jobStatus.getSourceUid(), jobStatus.getSourcePackageName(), true);
        if (!this.mSortedStats.contains(uidStats)) {
            this.mSortedStats.add(uidStats);
        }
        if (this.mCurrentDefaultNetworkCallbacks.size() >= 125) {
            postAdjustCallbacks();
        } else {
            registerPendingUidCallbacksLocked();
        }
    }

    private void registerPendingUidCallbacksLocked() {
        int numCallbacks = this.mCurrentDefaultNetworkCallbacks.size();
        int numPending = this.mSortedStats.size();
        if (numPending < numCallbacks) {
            Slog.wtf(TAG, "There are more registered callbacks than sorted UIDs: " + numCallbacks + " vs " + numPending);
        }
        for (int i = numCallbacks; i < numPending && i < 125; i++) {
            UidStats uidStats = this.mSortedStats.get(i);
            UidDefaultNetworkCallback callback = (UidDefaultNetworkCallback) this.mDefaultNetworkCallbackPool.acquire();
            if (callback == null) {
                callback = new UidDefaultNetworkCallback();
            }
            callback.setUid(uidStats.uid);
            this.mCurrentDefaultNetworkCallbacks.append(uidStats.uid, callback);
            this.mConnManager.registerDefaultNetworkCallbackForUid(uidStats.uid, callback, this.mHandler);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void postAdjustCallbacks() {
        postAdjustCallbacks(0L);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void postAdjustCallbacks(long delayMs) {
        this.mHandler.sendEmptyMessageDelayed(0, delayMs);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void maybeAdjustRegisteredCallbacksLocked() {
        this.mHandler.removeMessages(0);
        int count = this.mUidStats.size();
        if (count == this.mCurrentDefaultNetworkCallbacks.size()) {
            return;
        }
        long nowElapsed = JobSchedulerService.sElapsedRealtimeClock.millis();
        if (nowElapsed - this.mLastCallbackAdjustmentTimeElapsed < 1000) {
            postAdjustCallbacks(1000L);
            return;
        }
        this.mLastCallbackAdjustmentTimeElapsed = nowElapsed;
        this.mSortedStats.clear();
        for (int u = 0; u < this.mUidStats.size(); u++) {
            UidStats us = this.mUidStats.valueAt(u);
            ArraySet<JobStatus> jobs = this.mTrackedJobs.get(us.uid);
            if (jobs == null || jobs.size() == 0) {
                unregisterDefaultNetworkCallbackLocked(us.uid, nowElapsed);
            } else {
                if (us.lastUpdatedElapsed + 30000 < nowElapsed) {
                    us.earliestEnqueueTime = JobStatus.NO_LATEST_RUNTIME;
                    us.earliestEJEnqueueTime = JobStatus.NO_LATEST_RUNTIME;
                    us.numReadyWithConnectivity = 0;
                    us.numRequestedNetworkAvailable = 0;
                    us.numRegular = 0;
                    us.numEJs = 0;
                    for (int j = 0; j < jobs.size(); j++) {
                        JobStatus job = jobs.valueAt(j);
                        if (wouldBeReadyWithConstraintLocked(job, 268435456)) {
                            us.numReadyWithConnectivity++;
                            if (isNetworkAvailable(job)) {
                                us.numRequestedNetworkAvailable++;
                            }
                            us.earliestEnqueueTime = Math.min(us.earliestEnqueueTime, job.enqueueTime);
                            if (job.shouldTreatAsExpeditedJob() || job.startedAsExpeditedJob) {
                                us.earliestEJEnqueueTime = Math.min(us.earliestEJEnqueueTime, job.enqueueTime);
                            }
                        }
                        if (job.shouldTreatAsExpeditedJob() || job.startedAsExpeditedJob) {
                            us.numEJs++;
                        } else {
                            us.numRegular++;
                        }
                    }
                    us.lastUpdatedElapsed = nowElapsed;
                }
                this.mSortedStats.add(us);
            }
        }
        this.mSortedStats.sort(this.mUidStatsComparator);
        ArraySet<JobStatus> changedJobs = new ArraySet<>();
        for (int i = this.mSortedStats.size() - 1; i >= 0; i--) {
            UidStats us2 = this.mSortedStats.get(i);
            if (i >= 125) {
                if (unregisterDefaultNetworkCallbackLocked(us2.uid, nowElapsed)) {
                    changedJobs.addAll((ArraySet<? extends JobStatus>) this.mTrackedJobs.get(us2.uid));
                }
            } else if (this.mCurrentDefaultNetworkCallbacks.get(us2.uid) == null) {
                UidDefaultNetworkCallback defaultNetworkCallback = (UidDefaultNetworkCallback) this.mDefaultNetworkCallbackPool.acquire();
                if (defaultNetworkCallback == null) {
                    defaultNetworkCallback = new UidDefaultNetworkCallback();
                }
                defaultNetworkCallback.setUid(us2.uid);
                this.mCurrentDefaultNetworkCallbacks.append(us2.uid, defaultNetworkCallback);
                this.mConnManager.registerDefaultNetworkCallbackForUid(us2.uid, defaultNetworkCallback, this.mHandler);
            }
        }
        int i2 = changedJobs.size();
        if (i2 > 0) {
            this.mStateChangedListener.onControllerStateChanged(changedJobs);
        }
    }

    private boolean unregisterDefaultNetworkCallbackLocked(int uid, long nowElapsed) {
        UidDefaultNetworkCallback defaultNetworkCallback = this.mCurrentDefaultNetworkCallbacks.get(uid);
        if (defaultNetworkCallback == null) {
            return false;
        }
        this.mCurrentDefaultNetworkCallbacks.remove(uid);
        this.mConnManager.unregisterNetworkCallback(defaultNetworkCallback);
        this.mDefaultNetworkCallbackPool.release(defaultNetworkCallback);
        defaultNetworkCallback.clear();
        boolean changed = false;
        ArraySet<JobStatus> jobs = this.mTrackedJobs.get(uid);
        if (jobs != null) {
            for (int j = jobs.size() - 1; j >= 0; j--) {
                changed |= updateConstraintsSatisfied(jobs.valueAt(j), nowElapsed, null, null);
            }
        }
        return changed;
    }

    private NetworkCapabilities getNetworkCapabilities(Network network) {
        NetworkCapabilities networkCapabilities;
        if (network == null) {
            return null;
        }
        synchronized (this.mLock) {
            networkCapabilities = this.mAvailableNetworks.get(network);
        }
        return networkCapabilities;
    }

    private Network getNetworkLocked(JobStatus jobStatus) {
        int unbypassableBlockedReasons;
        UidDefaultNetworkCallback defaultNetworkCallback = this.mCurrentDefaultNetworkCallbacks.get(jobStatus.getSourceUid());
        if (defaultNetworkCallback == null) {
            return null;
        }
        UidStats uidStats = this.mUidStats.get(jobStatus.getSourceUid());
        if (uidStats.baseBias >= 30 || (jobStatus.getFlags() & 1) != 0) {
            if (DEBUG) {
                Slog.d(TAG, "Using FG bypass for " + jobStatus.getSourceUid());
            }
            unbypassableBlockedReasons = UNBYPASSABLE_FOREGROUND_BLOCKED_REASONS;
        } else if (jobStatus.shouldTreatAsExpeditedJob() || jobStatus.startedAsExpeditedJob) {
            if (DEBUG) {
                Slog.d(TAG, "Using EJ bypass for " + jobStatus.getSourceUid());
            }
            unbypassableBlockedReasons = -8;
        } else {
            if (DEBUG) {
                Slog.d(TAG, "Using BG bypass for " + jobStatus.getSourceUid());
            }
            unbypassableBlockedReasons = -1;
        }
        if ((defaultNetworkCallback.mBlockedReasons & unbypassableBlockedReasons) != 0) {
            return null;
        }
        return defaultNetworkCallback.mDefaultNetwork;
    }

    private boolean updateConstraintsSatisfied(JobStatus jobStatus) {
        long nowElapsed = JobSchedulerService.sElapsedRealtimeClock.millis();
        UidDefaultNetworkCallback defaultNetworkCallback = this.mCurrentDefaultNetworkCallbacks.get(jobStatus.getSourceUid());
        if (defaultNetworkCallback == null) {
            maybeRegisterDefaultNetworkCallbackLocked(jobStatus);
            return updateConstraintsSatisfied(jobStatus, nowElapsed, null, null);
        }
        Network network = getNetworkLocked(jobStatus);
        NetworkCapabilities capabilities = getNetworkCapabilities(network);
        return updateConstraintsSatisfied(jobStatus, nowElapsed, network, capabilities);
    }

    private boolean updateConstraintsSatisfied(JobStatus jobStatus, long nowElapsed, Network network, NetworkCapabilities capabilities) {
        boolean satisfied = isSatisfied(jobStatus, network, capabilities, this.mConstants);
        boolean changed = jobStatus.setConnectivityConstraintSatisfied(nowElapsed, satisfied);
        jobStatus.network = network;
        if (DEBUG) {
            Slog.i(TAG, "Connectivity " + (changed ? "CHANGED" : "unchanged") + " for " + jobStatus + ": usable=" + isUsable(capabilities) + " satisfied=" + satisfied);
        }
        return changed;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateAllTrackedJobsLocked(boolean allowThrottle) {
        if (allowThrottle) {
            long throttleTimeLeftMs = (this.mLastAllJobUpdateTimeElapsed + this.mConstants.CONN_UPDATE_ALL_JOBS_MIN_INTERVAL_MS) - JobSchedulerService.sElapsedRealtimeClock.millis();
            if (throttleTimeLeftMs > 0) {
                Message msg = this.mHandler.obtainMessage(1, 1, 0);
                this.mHandler.sendMessageDelayed(msg, throttleTimeLeftMs);
                return;
            }
        }
        this.mHandler.removeMessages(1);
        updateTrackedJobsLocked(-1, (Network) null);
        this.mLastAllJobUpdateTimeElapsed = JobSchedulerService.sElapsedRealtimeClock.millis();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateTrackedJobsLocked(int filterUid, Network filterNetwork) {
        ArraySet<JobStatus> changedJobs;
        if (filterUid == -1) {
            changedJobs = new ArraySet<>();
            for (int i = this.mTrackedJobs.size() - 1; i >= 0; i--) {
                if (updateTrackedJobsLocked(this.mTrackedJobs.valueAt(i), filterNetwork)) {
                    changedJobs.addAll((ArraySet<? extends JobStatus>) this.mTrackedJobs.valueAt(i));
                }
            }
        } else if (updateTrackedJobsLocked(this.mTrackedJobs.get(filterUid), filterNetwork)) {
            changedJobs = this.mTrackedJobs.get(filterUid);
        } else {
            changedJobs = null;
        }
        if (changedJobs != null && changedJobs.size() > 0) {
            this.mStateChangedListener.onControllerStateChanged(changedJobs);
        }
    }

    private boolean updateTrackedJobsLocked(ArraySet<JobStatus> jobs, Network filterNetwork) {
        if (jobs == null || jobs.size() == 0) {
            return false;
        }
        UidDefaultNetworkCallback defaultNetworkCallback = this.mCurrentDefaultNetworkCallbacks.get(jobs.valueAt(0).getSourceUid());
        if (defaultNetworkCallback == null) {
            return false;
        }
        long nowElapsed = JobSchedulerService.sElapsedRealtimeClock.millis();
        boolean changed = false;
        for (int i = jobs.size() - 1; i >= 0; i--) {
            JobStatus js = jobs.valueAt(i);
            Network net = getNetworkLocked(js);
            NetworkCapabilities netCap = getNetworkCapabilities(net);
            boolean match = filterNetwork == null || Objects.equals(filterNetwork, net);
            if (match || !Objects.equals(js.network, net)) {
                changed = updateConstraintsSatisfied(js, nowElapsed, net, netCap) | changed;
            }
        }
        return changed;
    }

    @Override // android.net.ConnectivityManager.OnNetworkActiveListener
    public void onNetworkActive() {
        synchronized (this.mLock) {
            for (int i = this.mTrackedJobs.size() - 1; i >= 0; i--) {
                ArraySet<JobStatus> jobs = this.mTrackedJobs.valueAt(i);
                for (int j = jobs.size() - 1; j >= 0; j--) {
                    JobStatus js = jobs.valueAt(j);
                    if (js.isReady()) {
                        if (DEBUG) {
                            Slog.d(TAG, "Running " + js + " due to network activity.");
                        }
                        this.mStateChangedListener.onRunJobNow(js);
                    }
                }
            }
        }
    }

    /* loaded from: classes.dex */
    private class CcHandler extends Handler {
        CcHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            synchronized (ConnectivityController.this.mLock) {
                switch (msg.what) {
                    case 0:
                        synchronized (ConnectivityController.this.mLock) {
                            ConnectivityController.this.maybeAdjustRegisteredCallbacksLocked();
                        }
                        break;
                    case 1:
                        synchronized (ConnectivityController.this.mLock) {
                            boolean z = true;
                            if (msg.arg1 != 1) {
                                z = false;
                            }
                            boolean allowThrottle = z;
                            ConnectivityController.this.updateAllTrackedJobsLocked(allowThrottle);
                        }
                        break;
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class UidDefaultNetworkCallback extends ConnectivityManager.NetworkCallback {
        private int mBlockedReasons;
        private Network mDefaultNetwork;
        private int mUid;

        private UidDefaultNetworkCallback() {
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void setUid(int uid) {
            this.mUid = uid;
            this.mDefaultNetwork = null;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void clear() {
            this.mDefaultNetwork = null;
            this.mUid = -10000;
        }

        @Override // android.net.ConnectivityManager.NetworkCallback
        public void onAvailable(Network network) {
            if (ConnectivityController.DEBUG) {
                Slog.v(ConnectivityController.TAG, "default-onAvailable(" + this.mUid + "): " + network);
            }
        }

        public void onBlockedStatusChanged(Network network, int blockedReasons) {
            if (ConnectivityController.DEBUG) {
                Slog.v(ConnectivityController.TAG, "default-onBlockedStatusChanged(" + this.mUid + "): " + network + " -> " + blockedReasons);
            }
            if (this.mUid == -10000) {
                return;
            }
            synchronized (ConnectivityController.this.mLock) {
                this.mDefaultNetwork = network;
                this.mBlockedReasons = blockedReasons;
                ConnectivityController.this.updateTrackedJobsLocked(this.mUid, network);
            }
        }

        @Override // android.net.ConnectivityManager.NetworkCallback
        public void onLost(Network network) {
            if (ConnectivityController.DEBUG) {
                Slog.v(ConnectivityController.TAG, "default-onLost(" + this.mUid + "): " + network);
            }
            if (this.mUid == -10000) {
                return;
            }
            synchronized (ConnectivityController.this.mLock) {
                if (Objects.equals(this.mDefaultNetwork, network)) {
                    this.mDefaultNetwork = null;
                    ConnectivityController.this.updateTrackedJobsLocked(this.mUid, network);
                    ConnectivityController.this.postAdjustCallbacks(1000L);
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void dumpLocked(IndentingPrintWriter pw) {
            pw.print("UID: ");
            pw.print(this.mUid);
            pw.print("; ");
            if (this.mDefaultNetwork == null) {
                pw.print("No network");
            } else {
                pw.print("Network: ");
                pw.print(this.mDefaultNetwork);
                pw.print(" (blocked=");
                pw.print(NetworkPolicyManager.blockedReasonsToString(this.mBlockedReasons));
                pw.print(")");
            }
            pw.println();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class UidStats {
        public int baseBias;
        public long earliestEJEnqueueTime;
        public long earliestEnqueueTime;
        public long lastUpdatedElapsed;
        public int numEJs;
        public int numReadyWithConnectivity;
        public int numRegular;
        public int numRequestedNetworkAvailable;
        public final ArraySet<JobStatus> runningJobs;
        public final int uid;

        private UidStats(int uid) {
            this.runningJobs = new ArraySet<>();
            this.uid = uid;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void dumpLocked(IndentingPrintWriter pw, long nowElapsed) {
            pw.print("UidStats{");
            pw.print(WatchlistLoggingHandler.WatchlistEventKeys.UID, Integer.valueOf(this.uid));
            pw.print("pri", Integer.valueOf(this.baseBias));
            pw.print("#run", Integer.valueOf(this.runningJobs.size()));
            pw.print("#readyWithConn", Integer.valueOf(this.numReadyWithConnectivity));
            pw.print("#netAvail", Integer.valueOf(this.numRequestedNetworkAvailable));
            pw.print("#EJs", Integer.valueOf(this.numEJs));
            pw.print("#reg", Integer.valueOf(this.numRegular));
            pw.print("earliestEnqueue", Long.valueOf(this.earliestEnqueueTime));
            pw.print("earliestEJEnqueue", Long.valueOf(this.earliestEJEnqueueTime));
            pw.print("updated=");
            TimeUtils.formatDuration(this.lastUpdatedElapsed - nowElapsed, pw);
            pw.println("}");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class CellSignalStrengthCallback extends TelephonyCallback implements TelephonyCallback.SignalStrengthsListener {
        public int signalStrength;

        private CellSignalStrengthCallback() {
            this.signalStrength = 4;
        }

        @Override // android.telephony.TelephonyCallback.SignalStrengthsListener
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            synchronized (ConnectivityController.this.mLock) {
                int newSignalStrength = signalStrength.getLevel();
                if (ConnectivityController.DEBUG) {
                    Slog.d(ConnectivityController.TAG, "Signal strength changing from " + this.signalStrength + " to " + newSignalStrength);
                    for (CellSignalStrength css : signalStrength.getCellSignalStrengths()) {
                        Slog.d(ConnectivityController.TAG, "CSS: " + css.getLevel() + " " + css);
                    }
                }
                if (this.signalStrength == newSignalStrength) {
                    return;
                }
                this.signalStrength = newSignalStrength;
                ConnectivityController.this.mHandler.obtainMessage(1, 1, 0).sendToTarget();
            }
        }
    }

    @Override // com.android.server.job.controllers.StateController
    public void dumpControllerStateLocked(IndentingPrintWriter pw, Predicate<JobStatus> predicate) {
        long nowElapsed = JobSchedulerService.sElapsedRealtimeClock.millis();
        if (this.mRequestedWhitelistJobs.size() > 0) {
            pw.print("Requested standby exceptions:");
            for (int i = 0; i < this.mRequestedWhitelistJobs.size(); i++) {
                pw.print(" ");
                pw.print(this.mRequestedWhitelistJobs.keyAt(i));
                pw.print(" (");
                pw.print(this.mRequestedWhitelistJobs.valueAt(i).size());
                pw.print(" jobs)");
            }
            pw.println();
        }
        if (this.mAvailableNetworks.size() > 0) {
            pw.println("Available networks:");
            pw.increaseIndent();
            for (int i2 = 0; i2 < this.mAvailableNetworks.size(); i2++) {
                pw.print(this.mAvailableNetworks.keyAt(i2));
                pw.print(": ");
                pw.println(this.mAvailableNetworks.valueAt(i2));
            }
            pw.decreaseIndent();
        } else {
            pw.println("No available networks");
        }
        pw.println();
        if (this.mSignalStrengths.size() > 0) {
            pw.println("Subscription ID signal strengths:");
            pw.increaseIndent();
            for (int i3 = 0; i3 < this.mSignalStrengths.size(); i3++) {
                pw.print(this.mSignalStrengths.keyAt(i3));
                pw.print(": ");
                pw.println(this.mSignalStrengths.valueAt(i3).signalStrength);
            }
            pw.decreaseIndent();
        } else {
            pw.println("No cached signal strengths");
        }
        pw.println();
        pw.println("Current default network callbacks:");
        pw.increaseIndent();
        for (int i4 = 0; i4 < this.mCurrentDefaultNetworkCallbacks.size(); i4++) {
            this.mCurrentDefaultNetworkCallbacks.valueAt(i4).dumpLocked(pw);
        }
        pw.decreaseIndent();
        pw.println();
        pw.println("UID Pecking Order:");
        pw.increaseIndent();
        for (int i5 = 0; i5 < this.mSortedStats.size(); i5++) {
            pw.print(i5);
            pw.print(": ");
            this.mSortedStats.get(i5).dumpLocked(pw, nowElapsed);
        }
        pw.decreaseIndent();
        pw.println();
        for (int i6 = 0; i6 < this.mTrackedJobs.size(); i6++) {
            ArraySet<JobStatus> jobs = this.mTrackedJobs.valueAt(i6);
            for (int j = 0; j < jobs.size(); j++) {
                JobStatus js = jobs.valueAt(j);
                if (predicate.test(js)) {
                    pw.print("#");
                    js.printUniqueId(pw);
                    pw.print(" from ");
                    UserHandle.formatUid(pw, js.getSourceUid());
                    pw.print(": ");
                    pw.print(js.getJob().getRequiredNetwork());
                    pw.println();
                }
            }
        }
    }

    @Override // com.android.server.job.controllers.StateController
    public void dumpControllerStateLocked(ProtoOutputStream proto, long fieldId, Predicate<JobStatus> predicate) {
        long token = proto.start(fieldId);
        long mToken = proto.start(1146756268035L);
        for (int i = 0; i < this.mRequestedWhitelistJobs.size(); i++) {
            proto.write(2220498092035L, this.mRequestedWhitelistJobs.keyAt(i));
        }
        for (int i2 = 0; i2 < this.mTrackedJobs.size(); i2++) {
            ArraySet<JobStatus> jobs = this.mTrackedJobs.valueAt(i2);
            for (int j = 0; j < jobs.size(); j++) {
                JobStatus js = jobs.valueAt(j);
                if (predicate.test(js)) {
                    long jsToken = proto.start(2246267895810L);
                    js.writeToShortProto(proto, 1146756268033L);
                    proto.write(1120986464258L, js.getSourceUid());
                    proto.end(jsToken);
                }
            }
        }
        proto.end(mToken);
        proto.end(token);
    }
}
