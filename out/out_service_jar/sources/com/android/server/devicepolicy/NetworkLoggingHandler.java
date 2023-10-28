package com.android.server.devicepolicy;

import android.app.AlarmManager;
import android.app.admin.NetworkEvent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.LongSparseArray;
import android.util.Slog;
import com.android.server.job.controllers.JobStatus;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class NetworkLoggingHandler extends Handler {
    private static final long BATCH_FINALIZATION_TIMEOUT_ALARM_INTERVAL_MS = 1800000;
    private static final long BATCH_FINALIZATION_TIMEOUT_MS = 5400000;
    static final int LOG_NETWORK_EVENT_MSG = 1;
    private static final int MAX_BATCHES = 5;
    private static final int MAX_EVENTS_PER_BATCH = 1200;
    static final String NETWORK_EVENT_KEY = "network_event";
    private static final String NETWORK_LOGGING_TIMEOUT_ALARM_TAG = "NetworkLogging.batchTimeout";
    private static final long RETRIEVED_BATCH_DISCARD_DELAY_MS = 300000;
    private final AlarmManager mAlarmManager;
    private final AlarmManager.OnAlarmListener mBatchTimeoutAlarmListener;
    private final LongSparseArray<ArrayList<NetworkEvent>> mBatches;
    private long mCurrentBatchToken;
    private final DevicePolicyManagerService mDpm;
    private long mId;
    private long mLastFinalizationNanos;
    private long mLastRetrievedBatchToken;
    private ArrayList<NetworkEvent> mNetworkEvents;
    private boolean mPaused;
    private int mTargetUserId;
    private static final String TAG = NetworkLoggingHandler.class.getSimpleName();
    private static final long FORCE_FETCH_THROTTLE_NS = TimeUnit.SECONDS.toNanos(10);

    /* JADX INFO: Access modifiers changed from: package-private */
    public NetworkLoggingHandler(Looper looper, DevicePolicyManagerService dpm, int targetUserId) {
        this(looper, dpm, 0L, targetUserId);
    }

    NetworkLoggingHandler(Looper looper, DevicePolicyManagerService dpm, long id, int targetUserId) {
        super(looper);
        this.mLastFinalizationNanos = -1L;
        this.mBatchTimeoutAlarmListener = new AlarmManager.OnAlarmListener() { // from class: com.android.server.devicepolicy.NetworkLoggingHandler.1
            @Override // android.app.AlarmManager.OnAlarmListener
            public void onAlarm() {
                Bundle notificationExtras;
                Slog.d(NetworkLoggingHandler.TAG, "Received a batch finalization timeout alarm, finalizing " + NetworkLoggingHandler.this.mNetworkEvents.size() + " pending events.");
                synchronized (NetworkLoggingHandler.this) {
                    notificationExtras = NetworkLoggingHandler.this.finalizeBatchAndBuildAdminMessageLocked();
                }
                if (notificationExtras != null) {
                    NetworkLoggingHandler.this.notifyDeviceOwnerOrProfileOwner(notificationExtras);
                }
            }
        };
        this.mNetworkEvents = new ArrayList<>();
        this.mBatches = new LongSparseArray<>(5);
        this.mPaused = false;
        this.mDpm = dpm;
        this.mAlarmManager = dpm.mInjector.getAlarmManager();
        this.mId = id;
        this.mTargetUserId = targetUserId;
    }

    @Override // android.os.Handler
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case 1:
                NetworkEvent networkEvent = (NetworkEvent) msg.getData().getParcelable(NETWORK_EVENT_KEY);
                if (networkEvent != null) {
                    Bundle notificationExtras = null;
                    synchronized (this) {
                        this.mNetworkEvents.add(networkEvent);
                        if (this.mNetworkEvents.size() >= 1200) {
                            notificationExtras = finalizeBatchAndBuildAdminMessageLocked();
                        }
                    }
                    if (notificationExtras != null) {
                        notifyDeviceOwnerOrProfileOwner(notificationExtras);
                        return;
                    }
                    return;
                }
                return;
            default:
                Slog.d(TAG, "NetworkLoggingHandler received an unknown of message.");
                return;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void scheduleBatchFinalization() {
        long when = SystemClock.elapsedRealtime() + BATCH_FINALIZATION_TIMEOUT_MS;
        this.mAlarmManager.setWindow(2, when, 1800000L, NETWORK_LOGGING_TIMEOUT_ALARM_TAG, this.mBatchTimeoutAlarmListener, this);
        Slog.d(TAG, "Scheduled a new batch finalization alarm 5400000ms from now.");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long forceBatchFinalization() {
        synchronized (this) {
            long toWaitNanos = (this.mLastFinalizationNanos + FORCE_FETCH_THROTTLE_NS) - System.nanoTime();
            if (toWaitNanos > 0) {
                return TimeUnit.NANOSECONDS.toMillis(toWaitNanos) + 1;
            }
            Bundle notificationExtras = finalizeBatchAndBuildAdminMessageLocked();
            if (notificationExtras != null) {
                notifyDeviceOwnerOrProfileOwner(notificationExtras);
            }
            return 0L;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized void pause() {
        Slog.d(TAG, "Paused network logging");
        this.mPaused = true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void resume() {
        Bundle notificationExtras = null;
        synchronized (this) {
            if (!this.mPaused) {
                Slog.d(TAG, "Attempted to resume network logging, but logging is not paused.");
                return;
            }
            Slog.d(TAG, "Resumed network logging. Current batch=" + this.mCurrentBatchToken + ", LastRetrievedBatch=" + this.mLastRetrievedBatchToken);
            this.mPaused = false;
            if (this.mBatches.size() > 0 && this.mLastRetrievedBatchToken != this.mCurrentBatchToken) {
                scheduleBatchFinalization();
                notificationExtras = buildAdminMessageLocked();
            }
            if (notificationExtras != null) {
                notifyDeviceOwnerOrProfileOwner(notificationExtras);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized void discardLogs() {
        this.mBatches.clear();
        this.mNetworkEvents = new ArrayList<>();
        Slog.d(TAG, "Discarded all network logs");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public Bundle finalizeBatchAndBuildAdminMessageLocked() {
        this.mLastFinalizationNanos = System.nanoTime();
        Bundle notificationExtras = null;
        if (this.mNetworkEvents.size() > 0) {
            Iterator<NetworkEvent> it = this.mNetworkEvents.iterator();
            while (it.hasNext()) {
                NetworkEvent event = it.next();
                event.setId(this.mId);
                long j = this.mId;
                if (j == JobStatus.NO_LATEST_RUNTIME) {
                    Slog.i(TAG, "Reached maximum id value; wrapping around ." + this.mCurrentBatchToken);
                    this.mId = 0L;
                } else {
                    this.mId = j + 1;
                }
            }
            if (this.mBatches.size() >= 5) {
                this.mBatches.removeAt(0);
            }
            long j2 = this.mCurrentBatchToken + 1;
            this.mCurrentBatchToken = j2;
            this.mBatches.append(j2, this.mNetworkEvents);
            this.mNetworkEvents = new ArrayList<>();
            if (!this.mPaused) {
                notificationExtras = buildAdminMessageLocked();
            }
        } else {
            Slog.d(TAG, "Was about to finalize the batch, but there were no events to send to the DPC, the batchToken of last available batch: " + this.mCurrentBatchToken);
        }
        scheduleBatchFinalization();
        return notificationExtras;
    }

    private Bundle buildAdminMessageLocked() {
        Bundle extras = new Bundle();
        LongSparseArray<ArrayList<NetworkEvent>> longSparseArray = this.mBatches;
        int lastBatchSize = longSparseArray.valueAt(longSparseArray.size() - 1).size();
        extras.putLong("android.app.extra.EXTRA_NETWORK_LOGS_TOKEN", this.mCurrentBatchToken);
        extras.putInt("android.app.extra.EXTRA_NETWORK_LOGS_COUNT", lastBatchSize);
        return extras;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyDeviceOwnerOrProfileOwner(Bundle extras) {
        if (Thread.holdsLock(this)) {
            Slog.wtfStack(TAG, "Shouldn't be called with NetworkLoggingHandler lock held");
            return;
        }
        Slog.d(TAG, "Sending network logging batch broadcast to device owner or profile owner, batchToken: " + extras.getLong("android.app.extra.EXTRA_NETWORK_LOGS_TOKEN", -1L));
        this.mDpm.sendDeviceOwnerOrProfileOwnerCommand("android.app.action.NETWORK_LOGS_AVAILABLE", extras, this.mTargetUserId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized List<NetworkEvent> retrieveFullLogBatch(final long batchToken) {
        int index = this.mBatches.indexOfKey(batchToken);
        if (index < 0) {
            return null;
        }
        postDelayed(new Runnable() { // from class: com.android.server.devicepolicy.NetworkLoggingHandler$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                NetworkLoggingHandler.this.m3153x98f15c3a(batchToken);
            }
        }, 300000L);
        this.mLastRetrievedBatchToken = batchToken;
        return this.mBatches.valueAt(index);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$retrieveFullLogBatch$0$com-android-server-devicepolicy-NetworkLoggingHandler  reason: not valid java name */
    public /* synthetic */ void m3153x98f15c3a(long batchToken) {
        synchronized (this) {
            while (this.mBatches.size() > 0 && this.mBatches.keyAt(0) <= batchToken) {
                this.mBatches.removeAt(0);
            }
        }
    }
}
