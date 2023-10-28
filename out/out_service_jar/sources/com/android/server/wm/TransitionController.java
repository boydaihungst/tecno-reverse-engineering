package com.android.server.wm;

import android.app.ActivityManager;
import android.app.IApplicationThread;
import android.os.IBinder;
import android.os.IRemoteCallback;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.ArrayMap;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import android.window.ITransitionMetricsReporter;
import android.window.ITransitionPlayer;
import android.window.RemoteTransition;
import android.window.TransitionInfo;
import android.window.TransitionRequestInfo;
import com.android.internal.protolog.ProtoLogGroup;
import com.android.internal.protolog.ProtoLogImpl;
import com.android.server.LocalServices;
import com.android.server.statusbar.StatusBarManagerInternal;
import com.android.server.wm.Transition;
import com.android.server.wm.WindowManagerInternal;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.LongConsumer;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class TransitionController {
    private static final int CHANGE_TIMEOUT_MS = 2000;
    private static final int DEFAULT_TIMEOUT_MS = 5000;
    private static final int LEGACY_STATE_IDLE = 0;
    private static final int LEGACY_STATE_READY = 1;
    private static final int LEGACY_STATE_RUNNING = 2;
    private static final boolean SHELL_TRANSITIONS_ROTATION = SystemProperties.getBoolean("persist.wm.debug.shell_transit_rotate", false);
    private static final String TAG = "TransitionController";
    final ActivityTaskManagerService mAtm;
    final TaskSnapshotController mTaskSnapshotController;
    private ITransitionPlayer mTransitionPlayer;
    private IApplicationThread mTransitionPlayerThread;
    final TransitionMetricsReporter mTransitionMetricsReporter = new TransitionMetricsReporter();
    private final ArrayList<WindowManagerInternal.AppTransitionListener> mLegacyListeners = new ArrayList<>();
    private final ArrayList<Transition> mPlayingTransitions = new ArrayList<>();
    final Lock mRunningLock = new Lock();
    private Transition mCollectingTransition = null;
    final StatusBarManagerInternal mStatusBar = (StatusBarManagerInternal) LocalServices.getService(StatusBarManagerInternal.class);
    private final IBinder.DeathRecipient mTransitionPlayerDeath = new IBinder.DeathRecipient() { // from class: com.android.server.wm.TransitionController$$ExternalSyntheticLambda0
        @Override // android.os.IBinder.DeathRecipient
        public final void binderDied() {
            TransitionController.this.m8447lambda$new$0$comandroidserverwmTransitionController();
        }
    };

    /* JADX INFO: Access modifiers changed from: package-private */
    public TransitionController(ActivityTaskManagerService atm, TaskSnapshotController taskSnapshotController) {
        this.mAtm = atm;
        this.mTaskSnapshotController = taskSnapshotController;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$0$com-android-server-wm-TransitionController  reason: not valid java name */
    public /* synthetic */ void m8447lambda$new$0$comandroidserverwmTransitionController() {
        synchronized (this.mAtm.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                for (int i = 0; i < this.mPlayingTransitions.size(); i++) {
                    this.mPlayingTransitions.get(i).cleanUpOnFailure();
                }
                this.mPlayingTransitions.clear();
                this.mTransitionPlayer = null;
                this.mRunningLock.doNotifyLocked();
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Transition createTransition(int type) {
        return createTransition(type, 0);
    }

    private Transition createTransition(int type, int flags) {
        if (this.mTransitionPlayer == null) {
            throw new IllegalStateException("Shell Transitions not enabled");
        }
        if (this.mCollectingTransition != null) {
            throw new IllegalStateException("Simultaneous transition collection not supported yet. Use {@link #createPendingTransition} for explicit queueing.");
        }
        Transition transit = new Transition(type, flags, this, this.mAtm.mWindowManager.mSyncEngine);
        if (ProtoLogCache.WM_DEBUG_WINDOW_TRANSITIONS_enabled) {
            String protoLogParam0 = String.valueOf(transit);
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_WINDOW_TRANSITIONS, 259206414, 0, "Creating Transition: %s", new Object[]{protoLogParam0});
        }
        moveToCollecting(transit);
        return transit;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void moveToCollecting(Transition transition) {
        if (this.mCollectingTransition != null) {
            throw new IllegalStateException("Simultaneous transition collection not supported.");
        }
        this.mCollectingTransition = transition;
        long timeoutMs = transition.mType == 6 ? 2000L : 5000L;
        this.mCollectingTransition.startCollecting(timeoutMs);
        if (ProtoLogCache.WM_DEBUG_WINDOW_TRANSITIONS_enabled) {
            String protoLogParam0 = String.valueOf(this.mCollectingTransition);
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_WINDOW_TRANSITIONS, -1764792832, 0, "Start collecting in Transition: %s", new Object[]{protoLogParam0});
        }
        dispatchLegacyAppTransitionPending();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void registerTransitionPlayer(ITransitionPlayer player, IApplicationThread appThread) {
        try {
            ITransitionPlayer iTransitionPlayer = this.mTransitionPlayer;
            if (iTransitionPlayer != null) {
                if (iTransitionPlayer.asBinder() != null) {
                    this.mTransitionPlayer.asBinder().unlinkToDeath(this.mTransitionPlayerDeath, 0);
                }
                this.mTransitionPlayer = null;
            }
            if (player.asBinder() != null) {
                player.asBinder().linkToDeath(this.mTransitionPlayerDeath, 0);
            }
            this.mTransitionPlayer = player;
            this.mTransitionPlayerThread = appThread;
        } catch (RemoteException e) {
            throw new RuntimeException("Unable to set transition player");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ITransitionPlayer getTransitionPlayer() {
        return this.mTransitionPlayer;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isShellTransitionsEnabled() {
        return this.mTransitionPlayer != null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean useShellTransitionsRotation() {
        return isShellTransitionsEnabled() && SHELL_TRANSITIONS_ROTATION;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isCollecting() {
        return this.mCollectingTransition != null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isCollecting(WindowContainer wc) {
        Transition transition = this.mCollectingTransition;
        return transition != null && transition.mParticipants.contains(wc);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isPlaying() {
        return !this.mPlayingTransitions.isEmpty();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean inTransition() {
        return isCollecting() || isPlaying();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean inTransition(WindowContainer wc) {
        if (isCollecting()) {
            for (WindowContainer p = wc; p != null; p = p.getParent()) {
                if (isCollecting(p)) {
                    return true;
                }
            }
        }
        for (int i = this.mPlayingTransitions.size() - 1; i >= 0; i--) {
            for (WindowContainer p2 = wc; p2 != null; p2 = p2.getParent()) {
                if (this.mPlayingTransitions.get(i).mParticipants.contains(p2)) {
                    return true;
                }
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean inRecentsTransition(WindowContainer wc) {
        Transition transition;
        for (WindowContainer p = wc; p != null && (transition = this.mCollectingTransition) != null; p = p.getParent()) {
            if ((transition.getFlags() & 128) != 0 && this.mCollectingTransition.mParticipants.contains(wc)) {
                return true;
            }
        }
        for (int i = this.mPlayingTransitions.size() - 1; i >= 0; i--) {
            for (WindowContainer p2 = wc; p2 != null; p2 = p2.getParent()) {
                if ((this.mPlayingTransitions.get(i).getFlags() & 128) != 0 && this.mPlayingTransitions.get(i).mParticipants.contains(p2)) {
                    return true;
                }
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isTransitionOnDisplay(DisplayContent dc) {
        Transition transition = this.mCollectingTransition;
        if (transition == null || !transition.isOnDisplay(dc)) {
            for (int i = this.mPlayingTransitions.size() - 1; i >= 0; i--) {
                if (this.mPlayingTransitions.get(i).isOnDisplay(dc)) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isTransientLaunch(ActivityRecord ar) {
        Transition transition = this.mCollectingTransition;
        if (transition == null || !transition.isTransientLaunch(ar)) {
            for (int i = this.mPlayingTransitions.size() - 1; i >= 0; i--) {
                if (this.mPlayingTransitions.get(i).isTransientLaunch(ar)) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getWindowingModeAtStart(WindowContainer wc) {
        Transition transition = this.mCollectingTransition;
        if (transition == null) {
            return wc.getWindowingMode();
        }
        Transition.ChangeInfo ci = transition.mChanges.get(wc);
        if (ci == null) {
            return wc.getWindowingMode();
        }
        return ci.mWindowingMode;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getCollectingTransitionType() {
        Transition transition = this.mCollectingTransition;
        if (transition != null) {
            return transition.mType;
        }
        return 0;
    }

    Transition requestTransitionIfNeeded(int type, WindowContainer trigger) {
        return requestTransitionIfNeeded(type, 0, trigger, trigger);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Transition requestTransitionIfNeeded(int type, int flags, WindowContainer trigger, WindowContainer readyGroupRef) {
        return requestTransitionIfNeeded(type, flags, trigger, readyGroupRef, null, null);
    }

    private static boolean isExistenceType(int type) {
        return type == 1 || type == 2;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Transition requestTransitionIfNeeded(int type, int flags, WindowContainer trigger, WindowContainer readyGroupRef, RemoteTransition remoteTransition, TransitionRequestInfo.DisplayChange displayChange) {
        if (this.mTransitionPlayer == null) {
            return null;
        }
        Transition newTransition = null;
        if (isCollecting()) {
            if (displayChange != null) {
                throw new IllegalArgumentException("Provided displayChange for a non-new request");
            }
            this.mCollectingTransition.setReady(readyGroupRef, false);
            if ((flags & 256) != 0) {
                this.mCollectingTransition.addFlag(flags);
            }
        } else {
            newTransition = requestStartTransition(createTransition(type, flags), trigger != null ? trigger.asTask() : null, remoteTransition, displayChange);
        }
        if (trigger != null) {
            if (isExistenceType(type)) {
                collectExistenceChange(trigger);
            } else {
                collect(trigger);
            }
        }
        return newTransition;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Transition requestStartTransition(Transition transition, Task startTask, RemoteTransition remoteTransition, TransitionRequestInfo.DisplayChange displayChange) {
        try {
            if (ProtoLogCache.WM_DEBUG_WINDOW_TRANSITIONS_enabled) {
                String protoLogParam0 = String.valueOf(transition);
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_WINDOW_TRANSITIONS, 1794249572, 0, "Requesting StartTransition: %s", new Object[]{protoLogParam0});
            }
            ActivityManager.RunningTaskInfo info = null;
            if (startTask != null) {
                info = new ActivityManager.RunningTaskInfo();
                startTask.fillTaskInfo(info);
            }
            this.mTransitionPlayer.requestStartTransition(transition, new TransitionRequestInfo(transition.mType, info, remoteTransition, displayChange));
        } catch (RemoteException e) {
            Slog.e(TAG, "Error requesting transition", e);
            transition.start();
        }
        return transition;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void requestCloseTransitionIfNeeded(WindowContainer<?> wc) {
        if (this.mTransitionPlayer == null) {
            return;
        }
        if (wc.isVisibleRequested()) {
            if (!isCollecting()) {
                requestStartTransition(createTransition(2, 0), wc.asTask(), null, null);
            }
            collectExistenceChange(wc);
            return;
        }
        collect(wc);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void collect(WindowContainer wc) {
        Transition transition = this.mCollectingTransition;
        if (transition == null) {
            return;
        }
        transition.collect(wc);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void collectExistenceChange(WindowContainer wc) {
        Transition transition = this.mCollectingTransition;
        if (transition == null) {
            return;
        }
        transition.collectExistenceChange(wc);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void collectForDisplayChange(final DisplayContent dc, Transition incoming) {
        if (incoming == null) {
            incoming = this.mCollectingTransition;
        }
        if (incoming == null) {
            return;
        }
        final Transition transition = incoming;
        dc.forAllLeafTasks(new Consumer() { // from class: com.android.server.wm.TransitionController$$ExternalSyntheticLambda1
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                TransitionController.lambda$collectForDisplayChange$1(Transition.this, (Task) obj);
            }
        }, true);
        dc.forAllWindows(new Consumer() { // from class: com.android.server.wm.TransitionController$$ExternalSyntheticLambda2
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                TransitionController.this.m8446xe8da0748(dc, transition, (WindowState) obj);
            }
        }, true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$collectForDisplayChange$1(Transition transition, Task task) {
        if (task.isVisible()) {
            transition.collect(task);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$collectForDisplayChange$2$com-android-server-wm-TransitionController  reason: not valid java name */
    public /* synthetic */ void m8446xe8da0748(DisplayContent dc, Transition transition, WindowState w) {
        if (w.mActivityRecord == null && w.isVisible() && !isCollecting(w.mToken) && dc.shouldSyncRotationChange(w)) {
            transition.collect(w.mToken);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setOverrideAnimation(TransitionInfo.AnimationOptions options, IRemoteCallback startCallback, IRemoteCallback finishCallback) {
        Transition transition = this.mCollectingTransition;
        if (transition == null) {
            return;
        }
        transition.setOverrideAnimation(options, startCallback, finishCallback);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setReady(WindowContainer wc, boolean ready) {
        Transition transition = this.mCollectingTransition;
        if (transition == null) {
            return;
        }
        transition.setReady(wc, ready);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setReady(WindowContainer wc) {
        setReady(wc, true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void finishTransition(IBinder token) {
        this.mTransitionMetricsReporter.reportAnimationStart(token, 0L);
        this.mAtm.endLaunchPowerMode(2);
        Transition record = Transition.fromBinder(token);
        if (record == null || !this.mPlayingTransitions.contains(record)) {
            Slog.e(TAG, "Trying to finish a non-playing transition " + token);
            return;
        }
        if (ProtoLogCache.WM_DEBUG_WINDOW_TRANSITIONS_enabled) {
            String protoLogParam0 = String.valueOf(record);
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_WINDOW_TRANSITIONS, -622017164, 0, "Finish Transition: %s", new Object[]{protoLogParam0});
        }
        this.mPlayingTransitions.remove(record);
        if (this.mPlayingTransitions.isEmpty()) {
            setAnimationRunning(false);
        }
        record.finishTransition();
        this.mRunningLock.doNotifyLocked();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void moveToPlaying(Transition transition) {
        if (transition != this.mCollectingTransition) {
            throw new IllegalStateException("Trying to move non-collecting transition to playing");
        }
        this.mCollectingTransition = null;
        if (this.mPlayingTransitions.isEmpty()) {
            setAnimationRunning(true);
        }
        this.mPlayingTransitions.add(transition);
    }

    private void setAnimationRunning(boolean running) {
        IApplicationThread iApplicationThread = this.mTransitionPlayerThread;
        if (iApplicationThread == null) {
            return;
        }
        WindowProcessController wpc = this.mAtm.getProcessController(iApplicationThread);
        if (wpc == null) {
            Slog.w(TAG, "Unable to find process for player thread=" + this.mTransitionPlayerThread);
        } else {
            wpc.setRunningRemoteAnimation(running);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void abort(Transition transition) {
        if (transition != this.mCollectingTransition) {
            throw new IllegalStateException("Too late to abort.");
        }
        transition.abort();
        this.mCollectingTransition = null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setTransientLaunch(ActivityRecord activity, Task restoreBelowTask) {
        Transition transition = this.mCollectingTransition;
        if (transition == null) {
            return;
        }
        transition.setTransientLaunch(activity, restoreBelowTask);
        if (activity.isActivityTypeHomeOrRecents()) {
            this.mCollectingTransition.addFlag(128);
            activity.getTask().setCanAffectSystemUiFlags(false);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void legacyDetachNavigationBarFromApp(IBinder token) {
        Transition transition = Transition.fromBinder(token);
        if (transition == null || !this.mPlayingTransitions.contains(transition)) {
            Slog.e(TAG, "Transition isn't playing: " + token);
        } else {
            transition.legacyRestoreNavigationBarFromApp();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void registerLegacyListener(WindowManagerInternal.AppTransitionListener listener) {
        this.mLegacyListeners.add(listener);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void unregisterLegacyListener(WindowManagerInternal.AppTransitionListener listener) {
        this.mLegacyListeners.remove(listener);
    }

    void dispatchLegacyAppTransitionPending() {
        for (int i = 0; i < this.mLegacyListeners.size(); i++) {
            this.mLegacyListeners.get(i).onAppTransitionPendingLocked();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dispatchLegacyAppTransitionStarting(TransitionInfo info) {
        boolean keyguardGoingAway = info.isKeyguardGoingAway();
        for (int i = 0; i < this.mLegacyListeners.size(); i++) {
            this.mLegacyListeners.get(i).onAppTransitionStartingLocked(keyguardGoingAway, false, 0L, SystemClock.uptimeMillis(), 120L);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dispatchLegacyAppTransitionFinished(ActivityRecord ar) {
        for (int i = 0; i < this.mLegacyListeners.size(); i++) {
            this.mLegacyListeners.get(i).onAppTransitionFinishedLocked(ar.token);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dispatchLegacyAppTransitionCancelled() {
        for (int i = 0; i < this.mLegacyListeners.size(); i++) {
            this.mLegacyListeners.get(i).onAppTransitionCancelledLocked(false);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpDebugLegacy(ProtoOutputStream proto, long fieldId) {
        long token = proto.start(fieldId);
        int state = 0;
        if (!this.mPlayingTransitions.isEmpty()) {
            state = 2;
        } else {
            Transition transition = this.mCollectingTransition;
            if ((transition != null && transition.getLegacyIsReady()) || this.mAtm.mWindowManager.mSyncEngine.hasPendingSyncSets()) {
                state = 1;
            }
        }
        proto.write(1159641169921L, state);
        proto.end(token);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static class TransitionMetricsReporter extends ITransitionMetricsReporter.Stub {
        private final ArrayMap<IBinder, LongConsumer> mMetricConsumers = new ArrayMap<>();

        TransitionMetricsReporter() {
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public void associate(IBinder transitionToken, LongConsumer consumer) {
            synchronized (this.mMetricConsumers) {
                this.mMetricConsumers.put(transitionToken, consumer);
            }
        }

        public void reportAnimationStart(IBinder transitionToken, long startTime) {
            synchronized (this.mMetricConsumers) {
                if (this.mMetricConsumers.isEmpty()) {
                    return;
                }
                LongConsumer c = this.mMetricConsumers.remove(transitionToken);
                if (c != null) {
                    c.accept(startTime);
                }
            }
        }
    }

    /* loaded from: classes2.dex */
    class Lock {
        private int mTransitionWaiters = 0;

        Lock() {
        }

        /* JADX DEBUG: Another duplicated slice has different insns count: {[]}, finally: {[INVOKE] complete} */
        /* JADX INFO: Access modifiers changed from: package-private */
        public void runWhenIdle(long timeout, Runnable r) {
            synchronized (TransitionController.this.mAtm.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    if (!TransitionController.this.inTransition()) {
                        r.run();
                        return;
                    }
                    this.mTransitionWaiters++;
                    WindowManagerService.resetPriorityAfterLockedSection();
                    long startTime = SystemClock.uptimeMillis();
                    long endTime = startTime + timeout;
                    while (true) {
                        synchronized (TransitionController.this.mAtm.mGlobalLock) {
                            try {
                                WindowManagerService.boostPriorityForLockedSection();
                                if (!TransitionController.this.inTransition() || SystemClock.uptimeMillis() > endTime) {
                                    break;
                                }
                            } finally {
                                WindowManagerService.resetPriorityAfterLockedSection();
                            }
                        }
                        synchronized (this) {
                            try {
                                try {
                                    wait(timeout);
                                } catch (InterruptedException e) {
                                    return;
                                }
                            } finally {
                            }
                        }
                    }
                    this.mTransitionWaiters--;
                    r.run();
                    WindowManagerService.resetPriorityAfterLockedSection();
                } finally {
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            }
        }

        void doNotifyLocked() {
            synchronized (this) {
                if (this.mTransitionWaiters > 0) {
                    notifyAll();
                }
            }
        }
    }
}
