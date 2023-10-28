package com.android.server.power.hint;

import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.app.IUidObserver;
import android.content.Context;
import android.os.Binder;
import android.os.IBinder;
import android.os.IHintManager;
import android.os.IHintSession;
import android.os.Process;
import android.os.RemoteException;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.SparseArray;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.Preconditions;
import com.android.server.FgThread;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.power.hint.HintManagerService;
import com.android.server.utils.Slogf;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
/* loaded from: classes2.dex */
public final class HintManagerService extends SystemService {
    private static final boolean DEBUG = false;
    private static final String TAG = "HintManagerService";
    private final ArrayMap<Integer, ArrayMap<IBinder, ArraySet<AppHintSession>>> mActiveSessions;
    private final ActivityManagerInternal mAmInternal;
    final long mHintSessionPreferredRate;
    private final Object mLock;
    private final NativeWrapper mNativeWrapper;
    final IHintManager.Stub mService;
    final UidObserver mUidObserver;

    public HintManagerService(Context context) {
        this(context, new Injector());
    }

    HintManagerService(Context context, Injector injector) {
        super(context);
        this.mLock = new Object();
        this.mService = new BinderService();
        this.mActiveSessions = new ArrayMap<>();
        NativeWrapper createNativeWrapper = injector.createNativeWrapper();
        this.mNativeWrapper = createNativeWrapper;
        createNativeWrapper.halInit();
        this.mHintSessionPreferredRate = createNativeWrapper.halGetHintSessionPreferredRate();
        this.mUidObserver = new UidObserver();
        this.mAmInternal = (ActivityManagerInternal) Objects.requireNonNull((ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class));
    }

    /* loaded from: classes2.dex */
    static class Injector {
        Injector() {
        }

        NativeWrapper createNativeWrapper() {
            return new NativeWrapper();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isHalSupported() {
        return this.mHintSessionPreferredRate != -1;
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        publishBinderService("performance_hint", this.mService);
    }

    @Override // com.android.server.SystemService
    public void onBootPhase(int phase) {
        if (phase == 500) {
            systemReady();
        }
    }

    private void systemReady() {
        Slogf.v(TAG, "Initializing HintManager service...");
        try {
            ActivityManager.getService().registerUidObserver(this.mUidObserver, 3, -1, (String) null);
        } catch (RemoteException e) {
        }
    }

    /* loaded from: classes2.dex */
    public static class NativeWrapper {
        private static native void nativeCloseHintSession(long j);

        private static native long nativeCreateHintSession(int i, int i2, int[] iArr, long j);

        private static native long nativeGetHintSessionPreferredRate();

        private native void nativeInit();

        private static native void nativePauseHintSession(long j);

        private static native void nativeReportActualWorkDuration(long j, long[] jArr, long[] jArr2);

        private static native void nativeResumeHintSession(long j);

        private static native void nativeUpdateTargetWorkDuration(long j, long j2);

        public void halInit() {
            nativeInit();
        }

        public long halCreateHintSession(int tgid, int uid, int[] tids, long durationNanos) {
            return nativeCreateHintSession(tgid, uid, tids, durationNanos);
        }

        public void halPauseHintSession(long halPtr) {
            nativePauseHintSession(halPtr);
        }

        public void halResumeHintSession(long halPtr) {
            nativeResumeHintSession(halPtr);
        }

        public void halCloseHintSession(long halPtr) {
            nativeCloseHintSession(halPtr);
        }

        public void halUpdateTargetWorkDuration(long halPtr, long targetDurationNanos) {
            nativeUpdateTargetWorkDuration(halPtr, targetDurationNanos);
        }

        public void halReportActualWorkDuration(long halPtr, long[] actualDurationNanos, long[] timeStampNanos) {
            nativeReportActualWorkDuration(halPtr, actualDurationNanos, timeStampNanos);
        }

        public long halGetHintSessionPreferredRate() {
            return nativeGetHintSessionPreferredRate();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public final class UidObserver extends IUidObserver.Stub {
        private final SparseArray<Integer> mProcStatesCache = new SparseArray<>();

        UidObserver() {
        }

        public boolean isUidForeground(int uid) {
            boolean z;
            synchronized (HintManagerService.this.mLock) {
                z = this.mProcStatesCache.get(uid, 6).intValue() <= 6;
            }
            return z;
        }

        public void onUidGone(final int uid, boolean disabled) {
            FgThread.getHandler().post(new Runnable() { // from class: com.android.server.power.hint.HintManagerService$UidObserver$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    HintManagerService.UidObserver.this.m6242x70d24f94(uid);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onUidGone$0$com-android-server-power-hint-HintManagerService$UidObserver  reason: not valid java name */
        public /* synthetic */ void m6242x70d24f94(int uid) {
            synchronized (HintManagerService.this.mLock) {
                ArrayMap<IBinder, ArraySet<AppHintSession>> tokenMap = (ArrayMap) HintManagerService.this.mActiveSessions.get(Integer.valueOf(uid));
                if (tokenMap == null) {
                    return;
                }
                for (int i = tokenMap.size() - 1; i >= 0; i--) {
                    ArraySet<AppHintSession> sessionSet = tokenMap.valueAt(i);
                    for (int j = sessionSet.size() - 1; j >= 0; j--) {
                        sessionSet.valueAt(j).close();
                    }
                }
                this.mProcStatesCache.delete(uid);
            }
        }

        public void onUidActive(int uid) {
        }

        public void onUidIdle(int uid, boolean disabled) {
        }

        public void onUidStateChanged(final int uid, final int procState, long procStateSeq, int capability) {
            FgThread.getHandler().post(new Runnable() { // from class: com.android.server.power.hint.HintManagerService$UidObserver$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    HintManagerService.UidObserver.this.m6243xe30fda6f(uid, procState);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onUidStateChanged$1$com-android-server-power-hint-HintManagerService$UidObserver  reason: not valid java name */
        public /* synthetic */ void m6243xe30fda6f(int uid, int procState) {
            synchronized (HintManagerService.this.mLock) {
                this.mProcStatesCache.put(uid, Integer.valueOf(procState));
                ArrayMap<IBinder, ArraySet<AppHintSession>> tokenMap = (ArrayMap) HintManagerService.this.mActiveSessions.get(Integer.valueOf(uid));
                if (tokenMap == null) {
                    return;
                }
                for (ArraySet<AppHintSession> sessionSet : tokenMap.values()) {
                    Iterator<AppHintSession> it = sessionSet.iterator();
                    while (it.hasNext()) {
                        AppHintSession s = it.next();
                        s.onProcStateChanged();
                    }
                }
            }
        }

        public void onUidCachedChanged(int uid, boolean cached) {
        }

        public void onUidProcAdjChanged(int uid) {
        }
    }

    IHintManager.Stub getBinderServiceInstance() {
        return this.mService;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean checkTidValid(int uid, int tgid, int[] tids) {
        List<Integer> eligiblePids = null;
        if (uid != 1000) {
            eligiblePids = this.mAmInternal.getIsolatedProcesses(uid);
        }
        if (eligiblePids == null) {
            eligiblePids = new ArrayList<>();
        }
        eligiblePids.add(Integer.valueOf(tgid));
        for (int threadId : tids) {
            String[] procStatusKeys = {"Uid:", "Tgid:"};
            long[] output = new long[procStatusKeys.length];
            Process.readProcLines("/proc/" + threadId + "/status", procStatusKeys, output);
            int uidOfThreadId = (int) output[0];
            int pidOfThreadId = (int) output[1];
            if (!eligiblePids.contains(Integer.valueOf(pidOfThreadId)) && uidOfThreadId != uid) {
                return false;
            }
        }
        return true;
    }

    /* loaded from: classes2.dex */
    final class BinderService extends IHintManager.Stub {
        BinderService() {
        }

        public IHintSession createHintSession(IBinder token, int[] tids, long durationNanos) {
            if (HintManagerService.this.isHalSupported()) {
                Objects.requireNonNull(token);
                Objects.requireNonNull(tids);
                Preconditions.checkArgument(tids.length != 0, "tids should not be empty.");
                int callingUid = Binder.getCallingUid();
                int callingTgid = Process.getThreadGroupLeader(Binder.getCallingPid());
                long identity = Binder.clearCallingIdentity();
                try {
                } catch (Throwable th) {
                    th = th;
                }
                try {
                    if (HintManagerService.this.checkTidValid(callingUid, callingTgid, tids)) {
                        long halSessionPtr = HintManagerService.this.mNativeWrapper.halCreateHintSession(callingTgid, callingUid, tids, durationNanos);
                        if (halSessionPtr != 0) {
                            AppHintSession hs = new AppHintSession(callingUid, callingTgid, tids, token, halSessionPtr, durationNanos);
                            synchronized (HintManagerService.this.mLock) {
                                ArrayMap<IBinder, ArraySet<AppHintSession>> tokenMap = (ArrayMap) HintManagerService.this.mActiveSessions.get(Integer.valueOf(callingUid));
                                if (tokenMap == null) {
                                    tokenMap = new ArrayMap<>(1);
                                    HintManagerService.this.mActiveSessions.put(Integer.valueOf(callingUid), tokenMap);
                                }
                                ArraySet<AppHintSession> sessionSet = tokenMap.get(token);
                                if (sessionSet == null) {
                                    sessionSet = new ArraySet<>(1);
                                    tokenMap.put(token, sessionSet);
                                }
                                sessionSet.add(hs);
                            }
                            Binder.restoreCallingIdentity(identity);
                            return hs;
                        }
                        Binder.restoreCallingIdentity(identity);
                        return null;
                    }
                    throw new SecurityException("Some tid doesn't belong to the application");
                } catch (Throwable th2) {
                    th = th2;
                    Binder.restoreCallingIdentity(identity);
                    throw th;
                }
            }
            return null;
        }

        public long getHintSessionPreferredRate() {
            return HintManagerService.this.mHintSessionPreferredRate;
        }

        public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (!DumpUtils.checkDumpPermission(HintManagerService.this.getContext(), HintManagerService.TAG, pw)) {
                return;
            }
            synchronized (HintManagerService.this.mLock) {
                pw.println("HintSessionPreferredRate: " + HintManagerService.this.mHintSessionPreferredRate);
                pw.println("HAL Support: " + HintManagerService.this.isHalSupported());
                pw.println("Active Sessions:");
                for (int i = 0; i < HintManagerService.this.mActiveSessions.size(); i++) {
                    pw.println("Uid " + ((Integer) HintManagerService.this.mActiveSessions.keyAt(i)).toString() + ":");
                    ArrayMap<IBinder, ArraySet<AppHintSession>> tokenMap = (ArrayMap) HintManagerService.this.mActiveSessions.valueAt(i);
                    for (int j = 0; j < tokenMap.size(); j++) {
                        ArraySet<AppHintSession> sessionSet = tokenMap.valueAt(j);
                        for (int k = 0; k < sessionSet.size(); k++) {
                            pw.println("  Session:");
                            sessionSet.valueAt(k).dump(pw, "    ");
                        }
                    }
                }
            }
        }
    }

    /* loaded from: classes2.dex */
    final class AppHintSession extends IHintSession.Stub implements IBinder.DeathRecipient {
        protected long mHalSessionPtr;
        protected final int mPid;
        protected long mTargetDurationNanos;
        protected final int[] mThreadIds;
        protected final IBinder mToken;
        protected final int mUid;
        protected boolean mUpdateAllowed = true;

        protected AppHintSession(int uid, int pid, int[] threadIds, IBinder token, long halSessionPtr, long durationNanos) {
            this.mUid = uid;
            this.mPid = pid;
            this.mToken = token;
            this.mThreadIds = threadIds;
            this.mHalSessionPtr = halSessionPtr;
            this.mTargetDurationNanos = durationNanos;
            updateHintAllowed();
            try {
                token.linkToDeath(this, 0);
            } catch (RemoteException e) {
                HintManagerService.this.mNativeWrapper.halCloseHintSession(this.mHalSessionPtr);
                throw new IllegalStateException("Client already dead", e);
            }
        }

        boolean updateHintAllowed() {
            boolean allowed;
            synchronized (HintManagerService.this.mLock) {
                allowed = HintManagerService.this.mUidObserver.isUidForeground(this.mUid);
                if (allowed && !this.mUpdateAllowed) {
                    resume();
                }
                if (!allowed && this.mUpdateAllowed) {
                    pause();
                }
                this.mUpdateAllowed = allowed;
            }
            return allowed;
        }

        public void updateTargetWorkDuration(long targetDurationNanos) {
            synchronized (HintManagerService.this.mLock) {
                if (this.mHalSessionPtr != 0 && updateHintAllowed()) {
                    Preconditions.checkArgument(targetDurationNanos > 0, "Expected the target duration to be greater than 0.");
                    HintManagerService.this.mNativeWrapper.halUpdateTargetWorkDuration(this.mHalSessionPtr, targetDurationNanos);
                    this.mTargetDurationNanos = targetDurationNanos;
                }
            }
        }

        public void reportActualWorkDuration(long[] actualDurationNanos, long[] timeStampNanos) {
            synchronized (HintManagerService.this.mLock) {
                if (this.mHalSessionPtr != 0 && updateHintAllowed()) {
                    Preconditions.checkArgument(actualDurationNanos.length != 0, "the count of hint durations shouldn't be 0.");
                    Preconditions.checkArgument(actualDurationNanos.length == timeStampNanos.length, "The length of durations and timestamps should be the same.");
                    for (int i = 0; i < actualDurationNanos.length; i++) {
                        if (actualDurationNanos[i] <= 0) {
                            throw new IllegalArgumentException(String.format("durations[%d]=%d should be greater than 0", Integer.valueOf(i), Long.valueOf(actualDurationNanos[i])));
                        }
                    }
                    HintManagerService.this.mNativeWrapper.halReportActualWorkDuration(this.mHalSessionPtr, actualDurationNanos, timeStampNanos);
                }
            }
        }

        public void close() {
            synchronized (HintManagerService.this.mLock) {
                if (this.mHalSessionPtr == 0) {
                    return;
                }
                HintManagerService.this.mNativeWrapper.halCloseHintSession(this.mHalSessionPtr);
                this.mHalSessionPtr = 0L;
                this.mToken.unlinkToDeath(this, 0);
                ArrayMap<IBinder, ArraySet<AppHintSession>> tokenMap = (ArrayMap) HintManagerService.this.mActiveSessions.get(Integer.valueOf(this.mUid));
                if (tokenMap == null) {
                    Slogf.w(HintManagerService.TAG, "UID %d is not present in active session map", Integer.valueOf(this.mUid));
                    return;
                }
                ArraySet<AppHintSession> sessionSet = tokenMap.get(this.mToken);
                if (sessionSet == null) {
                    Slogf.w(HintManagerService.TAG, "Token %s is not present in token map", this.mToken.toString());
                    return;
                }
                sessionSet.remove(this);
                if (sessionSet.isEmpty()) {
                    tokenMap.remove(this.mToken);
                }
                if (tokenMap.isEmpty()) {
                    HintManagerService.this.mActiveSessions.remove(Integer.valueOf(this.mUid));
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void onProcStateChanged() {
            updateHintAllowed();
        }

        private void pause() {
            synchronized (HintManagerService.this.mLock) {
                if (this.mHalSessionPtr == 0) {
                    return;
                }
                HintManagerService.this.mNativeWrapper.halPauseHintSession(this.mHalSessionPtr);
            }
        }

        private void resume() {
            synchronized (HintManagerService.this.mLock) {
                if (this.mHalSessionPtr == 0) {
                    return;
                }
                HintManagerService.this.mNativeWrapper.halResumeHintSession(this.mHalSessionPtr);
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void dump(PrintWriter pw, String prefix) {
            synchronized (HintManagerService.this.mLock) {
                pw.println(prefix + "SessionPID: " + this.mPid);
                pw.println(prefix + "SessionUID: " + this.mUid);
                pw.println(prefix + "SessionTIDs: " + Arrays.toString(this.mThreadIds));
                pw.println(prefix + "SessionTargetDurationNanos: " + this.mTargetDurationNanos);
                pw.println(prefix + "SessionAllowed: " + updateHintAllowed());
            }
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            close();
        }
    }
}
