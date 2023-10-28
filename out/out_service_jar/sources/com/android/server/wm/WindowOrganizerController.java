package com.android.server.wm;

import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.IApplicationThread;
import android.app.ThunderbackConfig;
import android.app.WindowConfiguration;
import android.content.ActivityNotFoundException;
import android.content.IIntentReceiver;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.AndroidRuntimeException;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Slog;
import android.view.RemoteAnimationAdapter;
import android.view.SurfaceControl;
import android.window.IDisplayAreaOrganizerController;
import android.window.ITaskFragmentOrganizer;
import android.window.ITaskFragmentOrganizerController;
import android.window.ITaskOrganizerController;
import android.window.ITransitionMetricsReporter;
import android.window.ITransitionPlayer;
import android.window.IWindowContainerTransactionCallback;
import android.window.IWindowOrganizerController;
import android.window.TaskFragmentCreationParams;
import android.window.WindowContainerTransaction;
import com.android.internal.protolog.ProtoLogGroup;
import com.android.internal.protolog.ProtoLogImpl;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.function.TriConsumer;
import com.android.internal.util.function.pooled.PooledLambda;
import com.android.server.LocalServices;
import com.android.server.pm.LauncherAppsService;
import com.android.server.wm.BLASTSyncEngine;
import com.transsion.hubcore.server.wm.ITranTask;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.function.Predicate;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class WindowOrganizerController extends IWindowOrganizerController.Stub implements BLASTSyncEngine.TransactionReadyListener {
    static final int CONTROLLABLE_CONFIGS = 536882176;
    static final int CONTROLLABLE_WINDOW_CONFIGS = 3;
    private static final String TAG = "WindowOrganizerController";
    private static final int TRANSACT_EFFECTS_CLIENT_CONFIG = 1;
    private static final int TRANSACT_EFFECTS_LIFECYCLE = 2;
    final DisplayAreaOrganizerController mDisplayAreaOrganizerController;
    private final WindowManagerGlobalLock mGlobalLock;
    private final ActivityTaskManagerService mService;
    final TaskFragmentOrganizerController mTaskFragmentOrganizerController;
    final TaskOrganizerController mTaskOrganizerController;
    TransitionController mTransitionController;
    private final HashMap<Integer, IWindowContainerTransactionCallback> mTransactionCallbacksByPendingSyncId = new HashMap<>();
    final ArrayMap<IBinder, TaskFragment> mLaunchTaskFragments = new ArrayMap<>();

    /* JADX INFO: Access modifiers changed from: package-private */
    public WindowOrganizerController(ActivityTaskManagerService atm) {
        this.mService = atm;
        this.mGlobalLock = atm.mGlobalLock;
        this.mTaskOrganizerController = new TaskOrganizerController(atm);
        this.mDisplayAreaOrganizerController = new DisplayAreaOrganizerController(atm);
        this.mTaskFragmentOrganizerController = new TaskFragmentOrganizerController(atm);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setWindowManager(WindowManagerService wms) {
        TransitionController transitionController = new TransitionController(this.mService, wms.mTaskSnapshotController);
        this.mTransitionController = transitionController;
        transitionController.registerLegacyListener(wms.mActivityManagerAppTransitionNotifier);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public TransitionController getTransitionController() {
        return this.mTransitionController;
    }

    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        try {
            return super.onTransact(code, data, reply, flags);
        } catch (RuntimeException e) {
            throw ActivityTaskManagerService.logAndRethrowRuntimeExceptionOnTransact(TAG, e);
        }
    }

    public void applyTransaction(WindowContainerTransaction t) {
        if (t == null) {
            throw new IllegalArgumentException("Null transaction passed to applyTransaction");
        }
        enforceTaskPermission("applyTransaction()", t);
        CallerInfo caller = new CallerInfo();
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                WindowManagerService.boostPriorityForLockedSection();
                applyTransaction(t, -1, null, caller);
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public int applySyncTransaction(final WindowContainerTransaction t, IWindowContainerTransactionCallback callback) {
        if (t == null) {
            throw new IllegalArgumentException("Null transaction passed to applySyncTransaction");
        }
        enforceTaskPermission("applySyncTransaction()");
        final CallerInfo caller = new CallerInfo();
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                WindowManagerService.boostPriorityForLockedSection();
                if (callback == null) {
                    applyTransaction(t, -1, null, caller);
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return -1;
                }
                final BLASTSyncEngine.SyncGroup syncGroup = prepareSyncWithOrganizer(callback);
                final int syncId = syncGroup.mSyncId;
                if (!this.mService.mWindowManager.mSyncEngine.hasActiveSync()) {
                    this.mService.mWindowManager.mSyncEngine.startSyncSet(syncGroup);
                    applyTransaction(t, syncId, null, caller);
                    setSyncReady(syncId);
                } else {
                    this.mService.mWindowManager.mSyncEngine.queueSyncSet(new Runnable() { // from class: com.android.server.wm.WindowOrganizerController$$ExternalSyntheticLambda1
                        @Override // java.lang.Runnable
                        public final void run() {
                            WindowOrganizerController.this.m8516xe006797e(syncGroup);
                        }
                    }, new Runnable() { // from class: com.android.server.wm.WindowOrganizerController$$ExternalSyntheticLambda2
                        @Override // java.lang.Runnable
                        public final void run() {
                            WindowOrganizerController.this.m8517xdf90137f(t, syncId, caller);
                        }
                    });
                }
                WindowManagerService.resetPriorityAfterLockedSection();
                return syncId;
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$applySyncTransaction$0$com-android-server-wm-WindowOrganizerController  reason: not valid java name */
    public /* synthetic */ void m8516xe006797e(BLASTSyncEngine.SyncGroup syncGroup) {
        this.mService.mWindowManager.mSyncEngine.startSyncSet(syncGroup);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$applySyncTransaction$1$com-android-server-wm-WindowOrganizerController  reason: not valid java name */
    public /* synthetic */ void m8517xdf90137f(WindowContainerTransaction t, int syncId, CallerInfo caller) {
        applyTransaction(t, syncId, null, caller);
        setSyncReady(syncId);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [316=4] */
    public IBinder startTransition(int type, IBinder transitionToken, WindowContainerTransaction t) {
        WindowContainerTransaction wct;
        enforceTaskPermission("startTransition()");
        final CallerInfo caller = new CallerInfo();
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                WindowManagerService.boostPriorityForLockedSection();
                Transition transition = Transition.fromBinder(transitionToken);
                if (this.mTransitionController.getTransitionPlayer() == null && transition == null) {
                    Slog.w(TAG, "Using shell transitions API for legacy transitions.");
                    if (t != null) {
                        applyTransaction(t, -1, null, caller);
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return null;
                    }
                    throw new IllegalArgumentException("Can't use legacy transitions in compatibility mode with no WCT.");
                }
                final boolean needsSetReady = transition == null && t != null;
                final WindowContainerTransaction wct2 = t != null ? t : new WindowContainerTransaction();
                if (transition != null) {
                    wct = wct2;
                } else if (type < 0) {
                    throw new IllegalArgumentException("Can't create transition with no type");
                } else {
                    if (this.mService.mWindowManager.mSyncEngine.hasActiveSync()) {
                        Slog.w(TAG, "startTransition() while one is already collecting.");
                        final Transition nextTransition = new Transition(type, 0, this.mTransitionController, this.mService.mWindowManager.mSyncEngine);
                        if (ProtoLogCache.WM_DEBUG_WINDOW_TRANSITIONS_enabled) {
                            String protoLogParam0 = String.valueOf(nextTransition);
                            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_WINDOW_TRANSITIONS, 1667162379, 0, "Creating Pending Transition: %s", new Object[]{protoLogParam0});
                        }
                        this.mService.mWindowManager.mSyncEngine.queueSyncSet(new Runnable() { // from class: com.android.server.wm.WindowOrganizerController$$ExternalSyntheticLambda3
                            @Override // java.lang.Runnable
                            public final void run() {
                                WindowOrganizerController.this.m8518x382f2f48(nextTransition);
                            }
                        }, new Runnable() { // from class: com.android.server.wm.WindowOrganizerController$$ExternalSyntheticLambda4
                            @Override // java.lang.Runnable
                            public final void run() {
                                WindowOrganizerController.this.m8519x37b8c949(nextTransition, wct2, caller, needsSetReady);
                            }
                        });
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return nextTransition;
                    }
                    wct = wct2;
                    transition = this.mTransitionController.createTransition(type);
                }
                transition.start();
                applyTransaction(wct, -1, transition, caller);
                if (needsSetReady) {
                    transition.setAllReady();
                }
                WindowManagerService.resetPriorityAfterLockedSection();
                return transition;
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$startTransition$2$com-android-server-wm-WindowOrganizerController  reason: not valid java name */
    public /* synthetic */ void m8518x382f2f48(Transition nextTransition) {
        this.mTransitionController.moveToCollecting(nextTransition);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$startTransition$3$com-android-server-wm-WindowOrganizerController  reason: not valid java name */
    public /* synthetic */ void m8519x37b8c949(Transition nextTransition, WindowContainerTransaction wct, CallerInfo caller, boolean needsSetReady) {
        nextTransition.start();
        applyTransaction(wct, -1, nextTransition, caller);
        if (needsSetReady) {
            nextTransition.setAllReady();
        }
    }

    public int startLegacyTransition(int type, RemoteAnimationAdapter adapter, IWindowContainerTransactionCallback callback, WindowContainerTransaction t) {
        enforceTaskPermission("startLegacyTransition()");
        CallerInfo caller = new CallerInfo();
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                WindowManagerService.boostPriorityForLockedSection();
                if (type < 0) {
                    throw new IllegalArgumentException("Can't create transition with no type");
                }
                if (this.mTransitionController.getTransitionPlayer() != null) {
                    throw new IllegalArgumentException("Can't use legacy transitions in when shell transitions are enabled.");
                }
                DisplayContent dc = this.mService.mRootWindowContainer.getDisplayContent(0);
                if (!dc.mAppTransition.isTransitionSet()) {
                    adapter.setCallingPidUid(caller.mPid, caller.mUid);
                    dc.prepareAppTransition(type);
                    dc.mAppTransition.overridePendingAppTransitionRemote(adapter, true);
                    int syncId = startSyncWithOrganizer(callback);
                    applyTransaction(t, syncId, null, caller);
                    setSyncReady(syncId);
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return syncId;
                }
                WindowManagerService.resetPriorityAfterLockedSection();
                return -1;
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public int finishTransition(IBinder transitionToken, WindowContainerTransaction t, IWindowContainerTransactionCallback callback) {
        int syncId;
        enforceTaskPermission("finishTransition()");
        CallerInfo caller = new CallerInfo();
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                WindowManagerService.boostPriorityForLockedSection();
                if (t != null && callback != null) {
                    int syncId2 = startSyncWithOrganizer(callback);
                    syncId = syncId2;
                } else {
                    syncId = -1;
                }
                Transition transition = Transition.fromBinder(transitionToken);
                if (t != null) {
                    applyTransaction(t, syncId, null, caller, transition);
                }
                getTransitionController().finishTransition(transitionToken);
                if (syncId >= 0) {
                    setSyncReady(syncId);
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
            return syncId;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private void applyTransaction(WindowContainerTransaction t, int syncId, Transition transition, CallerInfo caller) {
        applyTransaction(t, syncId, transition, caller, null);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [521=4] */
    /* JADX DEBUG: Failed to insert an additional move for type inference into block B:150:0x015b */
    /* JADX DEBUG: Multi-variable search result rejected for r3v11, resolved type: com.android.server.wm.WindowContainer */
    /* JADX DEBUG: Multi-variable search result rejected for r3v22, resolved type: com.android.server.wm.WindowContainer */
    /* JADX DEBUG: Multi-variable search result rejected for r3v23, resolved type: com.android.server.wm.WindowContainer */
    /* JADX DEBUG: Multi-variable search result rejected for r3v3, resolved type: com.android.server.wm.WindowContainer */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r3v5 */
    private void applyTransaction(WindowContainerTransaction t, int syncId, Transition transition, CallerInfo caller, Transition finishTransition) {
        boolean z;
        String str;
        String str2;
        String str3;
        String str4;
        ArraySet<WindowContainer> haveConfigChanges;
        WindowContainer wc;
        boolean z2;
        int i;
        int i2;
        int hopSize;
        List<WindowContainerTransaction.HierarchyOp> hops;
        String str5;
        String str6;
        ArraySet<WindowContainer> haveConfigChanges2;
        int effects = 0;
        boolean z3 = false;
        if (ProtoLogCache.WM_DEBUG_WINDOW_ORGANIZER_enabled) {
            long protoLogParam0 = syncId;
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_WINDOW_ORGANIZER, 906215061, 1, (String) null, new Object[]{Long.valueOf(protoLogParam0)});
        }
        this.mService.deferWindowLayout();
        this.mService.mTaskSupervisor.setDeferRootVisibilityUpdate(true);
        if (transition != null) {
            try {
                DisplayContent dc = DisplayRotation.getDisplayFromTransition(transition);
                if (dc != null && transition.mChanges.get(dc).hasChanged(dc)) {
                    dc.mTransitionController.collectForDisplayChange(dc, transition);
                    dc.sendNewConfiguration();
                    effects = 0 | 2;
                }
            } catch (Throwable th) {
                th = th;
                z = false;
                this.mService.mTaskSupervisor.setDeferRootVisibilityUpdate(z);
                this.mService.continueWindowLayout();
                throw th;
            }
        }
        ArraySet<WindowContainer> haveConfigChanges3 = new ArraySet<>();
        Iterator<Map.Entry<IBinder, WindowContainerTransaction.Change>> entries = t.getChanges().entrySet().iterator();
        while (true) {
            boolean hasNext = entries.hasNext();
            str = "Attempt to operate on detached container: ";
            str2 = TAG;
            if (!hasNext) {
                break;
            }
            Map.Entry<IBinder, WindowContainerTransaction.Change> entry = entries.next();
            WindowContainer wc2 = WindowContainer.fromBinder(entry.getKey());
            if (wc2 != null && wc2.isAttached()) {
                if (syncId >= 0) {
                    addToSyncSet(syncId, wc2);
                }
                if (transition != null) {
                    transition.collect(wc2);
                }
                if (finishTransition != null && (entry.getValue().getChangeMask() & 64) != 0) {
                    finishTransition.setCanPipOnFinish(false);
                }
                int containerEffect = applyWindowContainerChange(wc2, entry.getValue(), t.getErrorCallbackToken());
                effects |= containerEffect;
                if ((effects & 2) == 0 && (containerEffect & 1) != 0) {
                    haveConfigChanges3.add(wc2);
                }
            }
            Slog.e(TAG, "Attempt to operate on detached container: " + wc2);
        }
        List<WindowContainerTransaction.HierarchyOp> hops2 = t.getHierarchyOps();
        int hopSize2 = hops2.size();
        if (hopSize2 > 0) {
            boolean isInLockTaskMode = this.mService.isInLockTaskMode();
            int i3 = 0;
            int effects2 = effects;
            int hopSize3 = hopSize2;
            while (i3 < hopSize3) {
                try {
                    i2 = i3;
                    hopSize = hopSize3;
                    hops = hops2;
                    str5 = str2;
                    str6 = str;
                    haveConfigChanges2 = haveConfigChanges3;
                } catch (Throwable th2) {
                    th = th2;
                    z = z3;
                }
                try {
                    effects2 |= applyHierarchyOp(hops2.get(i3), effects2, syncId, transition, isInLockTaskMode, caller, t.getErrorCallbackToken(), t.getTaskFragmentOrganizer(), finishTransition);
                    i3 = i2 + 1;
                    hopSize3 = hopSize;
                    hops2 = hops;
                    str2 = str5;
                    str = str6;
                    haveConfigChanges3 = haveConfigChanges2;
                    z3 = false;
                } catch (Throwable th3) {
                    th = th3;
                    z = false;
                    this.mService.mTaskSupervisor.setDeferRootVisibilityUpdate(z);
                    this.mService.continueWindowLayout();
                    throw th;
                }
            }
            str3 = str2;
            str4 = str;
            haveConfigChanges = haveConfigChanges3;
            effects = effects2;
            wc = hopSize3;
        } else {
            str3 = TAG;
            str4 = "Attempt to operate on detached container: ";
            haveConfigChanges = haveConfigChanges3;
            wc = hopSize2;
        }
        try {
            for (Map.Entry<IBinder, WindowContainerTransaction.Change> entry2 : t.getChanges().entrySet()) {
                wc = WindowContainer.fromBinder(entry2.getKey());
                if (wc != 0 && wc.isAttached()) {
                    Task task = wc.asTask();
                    Rect surfaceBounds = entry2.getValue().getBoundsChangeSurfaceBounds();
                    if (task != null && task.isAttached() && surfaceBounds != null) {
                        if (!task.isOrganized()) {
                            Task parent = task.getParent() != null ? task.getParent().asTask() : null;
                            if (parent == null || !parent.mCreatedByOrganizer) {
                                throw new IllegalArgumentException("Can't manipulate non-organized task surface " + task);
                            }
                        }
                        SurfaceControl.Transaction sft = new SurfaceControl.Transaction();
                        SurfaceControl sc = task.getSurfaceControl();
                        sft.setPosition(sc, surfaceBounds.left, surfaceBounds.top);
                        if (surfaceBounds.isEmpty()) {
                            sft.setWindowCrop(sc, null);
                        } else {
                            sft.setWindowCrop(sc, surfaceBounds.width(), surfaceBounds.height());
                        }
                        task.setMainWindowSizeChangeTransaction(sft);
                    }
                }
                String str7 = str4;
                String str8 = str3;
                Slog.e(str8, str7 + wc);
                str4 = str7;
                str3 = str8;
            }
        } catch (Throwable th4) {
            th = th4;
            z = false;
        }
        try {
            if ((effects & 2) != 0) {
                z2 = false;
                this.mService.mTaskSupervisor.setDeferRootVisibilityUpdate(false);
                i = 1;
                this.mService.mRootWindowContainer.ensureActivitiesVisible(null, 0, true);
                this.mService.mRootWindowContainer.resumeFocusedTasksTopActivities();
            } else {
                z2 = false;
                i = 1;
                if ((effects & 1) != 0) {
                    Consumer<ActivityRecord> obtainConsumer = PooledLambda.obtainConsumer(new TriConsumer() { // from class: com.android.server.wm.WindowOrganizerController$$ExternalSyntheticLambda10
                        public final void accept(Object obj, Object obj2, Object obj3) {
                            ((ActivityRecord) obj).ensureActivityConfiguration(((Integer) obj2).intValue(), ((Boolean) obj3).booleanValue());
                        }
                    }, PooledLambda.__(ActivityRecord.class), 0, true);
                    try {
                        int i4 = haveConfigChanges.size() - 1;
                        while (i4 >= 0) {
                            ArraySet<WindowContainer> haveConfigChanges4 = haveConfigChanges;
                            try {
                                haveConfigChanges4.valueAt(i4).forAllActivities(obtainConsumer);
                                i4--;
                                haveConfigChanges = haveConfigChanges4;
                            } catch (Throwable th5) {
                                th = th5;
                                obtainConsumer.recycle();
                                throw th;
                            }
                        }
                        obtainConsumer.recycle();
                    } catch (Throwable th6) {
                        th = th6;
                    }
                }
            }
            if ((effects & 1) == 0) {
                this.mService.addWindowLayoutReasons(i);
            }
            this.mService.mTaskSupervisor.setDeferRootVisibilityUpdate(z2);
            this.mService.continueWindowLayout();
        } catch (Throwable th7) {
            th = th7;
            z = wc;
            this.mService.mTaskSupervisor.setDeferRootVisibilityUpdate(z);
            this.mService.continueWindowLayout();
            throw th;
        }
    }

    private int applyChanges(WindowContainer<?> container, WindowContainerTransaction.Change change, IBinder errorCallbackToken) {
        int configMask = change.getConfigSetMask() & CONTROLLABLE_CONFIGS;
        int windowMask = change.getWindowSetMask() & 3;
        int effects = 0;
        int windowingMode = change.getWindowingMode();
        int multiWindowMode = change.getMultiWindowMode();
        int multiWindowId = change.getMultiWindowId();
        int inLargeScreen = change.getInLargeScreen();
        if (configMask != 0) {
            adjustBoundsForMinDimensionsIfNeeded(container, change, errorCallbackToken);
            TranFoldWMCustody.instance().applyWindowOrganizerConfigChanges(container, change, configMask, windowMask);
            if (windowingMode > -1 && windowingMode != container.getWindowingMode()) {
                container.getRequestedOverrideConfiguration().setTo(change.getConfiguration(), configMask, windowMask);
            } else {
                Configuration c = new Configuration(container.getRequestedOverrideConfiguration());
                c.setTo(change.getConfiguration(), configMask, windowMask);
                container.onRequestedOverrideConfigurationChanged(c);
            }
            effects = 0 | 1;
            if (windowMask != 0 && container.isEmbedded()) {
                effects |= 2;
            }
        }
        if ((change.getChangeMask() & 1) != 0 && container.setFocusable(change.getFocusable())) {
            effects |= 2;
        }
        if (ThunderbackConfig.isVersion4()) {
            if (multiWindowMode > -1) {
                container.setMultiWindowMode(multiWindowMode);
            }
            if (multiWindowId > -1) {
                container.setMultiWindowId(multiWindowId);
            }
            if (inLargeScreen > -1) {
                container.setInLargeScreen(inLargeScreen);
            }
        }
        if (windowingMode > -1) {
            if (this.mService.isInLockTaskMode() && WindowConfiguration.inMultiWindowMode(windowingMode) && !container.isEmbedded()) {
                throw new UnsupportedOperationException("Not supported to set multi-window windowing mode during locked task mode.");
            }
            int prevMode = container.getWindowingMode();
            container.setWindowingMode(windowingMode);
            if (prevMode != container.getWindowingMode()) {
                return effects | 2;
            }
            return effects;
        }
        return effects;
    }

    private void adjustBoundsForMinDimensionsIfNeeded(WindowContainer<?> container, WindowContainerTransaction.Change change, IBinder errorCallbackToken) {
        TaskFragment taskFragment = container.asTaskFragment();
        if (taskFragment == null || !taskFragment.isEmbedded() || (change.getWindowSetMask() & 1) == 0) {
            return;
        }
        WindowConfiguration winConfig = change.getConfiguration().windowConfiguration;
        Rect bounds = winConfig.getBounds();
        Point minDimensions = taskFragment.calculateMinDimension();
        if (bounds.width() < minDimensions.x || bounds.height() < minDimensions.y) {
            sendMinimumDimensionViolation(taskFragment, minDimensions, errorCallbackToken, "setBounds:" + bounds);
            winConfig.setBounds(new Rect());
        }
    }

    private int applyTaskChanges(Task tr, WindowContainerTransaction.Change c) {
        int effects = 0;
        SurfaceControl.Transaction t = c.getBoundsChangeTransaction();
        if ((c.getChangeMask() & 8) != 0 && tr.setForceHidden(2, c.getHidden())) {
            effects = 2;
        }
        int childWindowingMode = c.getActivityWindowingMode();
        if (childWindowingMode > -1) {
            tr.setActivityWindowingMode(childWindowingMode);
        }
        if (t != null) {
            tr.setMainWindowSizeChangeTransaction(t);
        }
        Rect enterPipBounds = c.getEnterPipBounds();
        if (enterPipBounds != null) {
            tr.mDisplayContent.mPinnedTaskController.setEnterPipBounds(enterPipBounds);
        }
        return effects;
    }

    private int applyDisplayAreaChanges(DisplayArea displayArea, final WindowContainerTransaction.Change c) {
        final int[] effects = new int[1];
        if ((c.getChangeMask() & 32) != 0 && displayArea.setIgnoreOrientationRequest(c.getIgnoreOrientationRequest())) {
            effects[0] = effects[0] | 2;
        }
        displayArea.forAllTasks(new Consumer() { // from class: com.android.server.wm.WindowOrganizerController$$ExternalSyntheticLambda12
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                WindowOrganizerController.lambda$applyDisplayAreaChanges$4(c, effects, obj);
            }
        });
        return effects[0];
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$applyDisplayAreaChanges$4(WindowContainerTransaction.Change c, int[] effects, Object task) {
        Task tr = (Task) task;
        if ((c.getChangeMask() & 8) != 0 && tr.setForceHidden(2, c.getHidden())) {
            effects[0] = 2 | effects[0];
        }
    }

    /* JADX DEBUG: Multi-variable search result rejected for r3v40, resolved type: android.os.Bundle */
    /* JADX DEBUG: Multi-variable search result rejected for r6v71, resolved type: java.lang.String */
    /* JADX WARN: Multi-variable type inference failed */
    private int applyHierarchyOp(final WindowContainerTransaction.HierarchyOp hop, int effects, int syncId, Transition transition, boolean isInLockTaskMode, final CallerInfo caller, IBinder errorCallbackToken, ITaskFragmentOrganizer organizer, Transition finishTransition) {
        String str;
        String str2;
        String str3;
        int i;
        WindowContainer windowContainer;
        int effects2;
        ActivityRecord bottomActivity;
        final TaskFragment tf2;
        TaskFragment companion;
        WindowContainer newParent;
        WindowContainer windowContainer2;
        ActivityOptions makeBasic;
        WindowContainer container;
        Task restoreAt;
        int type = hop.getType();
        switch (type) {
            case 3:
                str = TAG;
                str2 = "Attempt to operate on unknown or detached container: ";
                str3 = "Attempt to operate on detached container: ";
                i = 0;
                windowContainer = null;
                WindowContainer wc = WindowContainer.fromBinder(hop.getContainer());
                Task task = wc != null ? wc.asTask() : null;
                if (task != null) {
                    task.getDisplayArea().setLaunchRootTask(task, hop.getWindowingModes(), hop.getActivityTypes());
                    effects2 = effects;
                    break;
                } else {
                    throw new IllegalArgumentException("Cannot set non-task as launch root: " + wc);
                }
            case 4:
                str = TAG;
                str2 = "Attempt to operate on unknown or detached container: ";
                str3 = "Attempt to operate on detached container: ";
                i = 0;
                windowContainer = null;
                effects2 = effects | setAdjacentRootsHierarchyOp(hop);
                break;
            case 6:
                str = TAG;
                str2 = "Attempt to operate on unknown or detached container: ";
                str3 = "Attempt to operate on detached container: ";
                i = 0;
                windowContainer = null;
                WindowContainer wc2 = WindowContainer.fromBinder(hop.getContainer());
                Task task2 = wc2 != null ? wc2.asTask() : null;
                boolean clearRoot = hop.getToTop();
                if (task2 == null) {
                    throw new IllegalArgumentException("Cannot set non-task as launch root: " + wc2);
                }
                if (!task2.mCreatedByOrganizer) {
                    throw new UnsupportedOperationException("Cannot set non-organized task as adjacent flag root: " + wc2);
                }
                if (task2.getAdjacentTaskFragment() == null && !clearRoot) {
                    throw new UnsupportedOperationException("Cannot set non-adjacent task as adjacent flag root: " + wc2);
                }
                task2.getDisplayArea().setLaunchAdjacentFlagRootTask(clearRoot ? null : task2);
                effects2 = effects;
                break;
            case 7:
                str = TAG;
                str2 = "Attempt to operate on unknown or detached container: ";
                str3 = "Attempt to operate on detached container: ";
                i = 0;
                windowContainer = null;
                TaskFragmentCreationParams taskFragmentCreationOptions = hop.getTaskFragmentCreationOptions();
                createTaskFragment(taskFragmentCreationOptions, errorCallbackToken, caller);
                effects2 = effects;
                break;
            case 8:
                str = TAG;
                str3 = "Attempt to operate on detached container: ";
                i = 0;
                windowContainer = null;
                WindowContainer wc3 = WindowContainer.fromBinder(hop.getContainer());
                if (wc3 == null || !wc3.isAttached()) {
                    str2 = "Attempt to operate on unknown or detached container: ";
                    Slog.e(str, str2 + wc3);
                } else {
                    TaskFragment taskFragment = wc3.asTaskFragment();
                    if (taskFragment == null || taskFragment.asTask() != null) {
                        throw new IllegalArgumentException("Can only delete organized TaskFragment, but not Task.");
                    }
                    if (isInLockTaskMode && (bottomActivity = taskFragment.getActivity(new Predicate() { // from class: com.android.server.wm.WindowOrganizerController$$ExternalSyntheticLambda6
                        @Override // java.util.function.Predicate
                        public final boolean test(Object obj) {
                            return WindowOrganizerController.lambda$applyHierarchyOp$5((ActivityRecord) obj);
                        }
                    }, false)) != null && this.mService.getLockTaskController().activityBlockedFromFinish(bottomActivity)) {
                        Slog.w(str, "Skip removing TaskFragment due in lock task mode.");
                        sendTaskFragmentOperationFailure(organizer, errorCallbackToken, new IllegalStateException("Not allow to delete task fragment in lock task mode."));
                        str2 = "Attempt to operate on unknown or detached container: ";
                    } else {
                        effects2 = effects | deleteTaskFragment(taskFragment, organizer, errorCallbackToken);
                        str2 = "Attempt to operate on unknown or detached container: ";
                        break;
                    }
                }
                effects2 = effects;
                break;
            case 9:
                IBinder fragmentToken = hop.getContainer();
                TaskFragment tf = this.mLaunchTaskFragments.get(fragmentToken);
                if (tf == null) {
                    Throwable exception = new IllegalArgumentException("Not allowed to operate with invalid fragment token");
                    sendTaskFragmentOperationFailure(organizer, errorCallbackToken, exception);
                    str = TAG;
                    str2 = "Attempt to operate on unknown or detached container: ";
                    str3 = "Attempt to operate on detached container: ";
                    i = 0;
                    windowContainer = null;
                } else if (tf.isEmbeddedTaskFragmentInPip()) {
                    Throwable exception2 = new IllegalArgumentException("Not allowed to start activity in PIP TaskFragment");
                    sendTaskFragmentOperationFailure(organizer, errorCallbackToken, exception2);
                    str = TAG;
                    str2 = "Attempt to operate on unknown or detached container: ";
                    str3 = "Attempt to operate on detached container: ";
                    i = 0;
                    windowContainer = null;
                } else {
                    Intent activityIntent = hop.getActivityIntent();
                    Bundle activityOptions = hop.getLaunchOptions();
                    ActivityStartController activityStartController = this.mService.getActivityStartController();
                    IBinder callingActivity = hop.getCallingActivity();
                    int i2 = caller.mUid;
                    int i3 = caller.mPid;
                    windowContainer = null;
                    str = TAG;
                    i = 0;
                    str3 = "Attempt to operate on detached container: ";
                    int result = activityStartController.startActivityInTaskFragment(tf, activityIntent, activityOptions, callingActivity, i2, i3, errorCallbackToken);
                    if (!ActivityManager.isStartResultSuccessful(result)) {
                        sendTaskFragmentOperationFailure(organizer, errorCallbackToken, convertStartFailureToThrowable(result, activityIntent));
                        str2 = "Attempt to operate on unknown or detached container: ";
                    } else {
                        effects2 = effects | 2;
                        str2 = "Attempt to operate on unknown or detached container: ";
                        break;
                    }
                }
                effects2 = effects;
                break;
            case 10:
                IBinder fragmentToken2 = hop.getNewParent();
                IBinder activityToken = hop.getContainer();
                ActivityRecord activity = ActivityRecord.forTokenLocked(activityToken);
                if (activity == null) {
                    activity = this.mTaskFragmentOrganizerController.getReparentActivityFromTemporaryToken(organizer, activityToken);
                }
                TaskFragment parent = this.mLaunchTaskFragments.get(fragmentToken2);
                if (parent != null && activity != null) {
                    if (!parent.isEmbeddedTaskFragmentInPip()) {
                        if (parent.isAllowedToEmbedActivity(activity) != 0) {
                            Throwable exception3 = new SecurityException("The task fragment is not allowed to embed the given activity.");
                            sendTaskFragmentOperationFailure(organizer, errorCallbackToken, exception3);
                            str = TAG;
                            str2 = "Attempt to operate on unknown or detached container: ";
                            str3 = "Attempt to operate on detached container: ";
                            i = 0;
                            windowContainer = null;
                        } else if (parent.getTask() != activity.getTask()) {
                            Throwable exception4 = new SecurityException("The reparented activity is not in the same Task as the target TaskFragment.");
                            sendTaskFragmentOperationFailure(organizer, errorCallbackToken, exception4);
                            str = TAG;
                            str2 = "Attempt to operate on unknown or detached container: ";
                            str3 = "Attempt to operate on detached container: ";
                            i = 0;
                            windowContainer = null;
                        } else {
                            activity.reparent(parent, Integer.MAX_VALUE);
                            effects2 = effects | 2;
                            str = TAG;
                            str2 = "Attempt to operate on unknown or detached container: ";
                            str3 = "Attempt to operate on detached container: ";
                            i = 0;
                            windowContainer = null;
                            break;
                        }
                    } else {
                        Throwable exception5 = new IllegalArgumentException("Not allowed to reparent activity to PIP TaskFragment");
                        sendTaskFragmentOperationFailure(organizer, errorCallbackToken, exception5);
                        str = TAG;
                        str2 = "Attempt to operate on unknown or detached container: ";
                        str3 = "Attempt to operate on detached container: ";
                        i = 0;
                        windowContainer = null;
                    }
                    effects2 = effects;
                    break;
                }
                Throwable exception6 = new IllegalArgumentException("Not allowed to operate with invalid fragment token or activity.");
                sendTaskFragmentOperationFailure(organizer, errorCallbackToken, exception6);
                str = TAG;
                str2 = "Attempt to operate on unknown or detached container: ";
                str3 = "Attempt to operate on detached container: ";
                i = 0;
                windowContainer = null;
                effects2 = effects;
                break;
            case 13:
                IBinder fragmentToken3 = hop.getContainer();
                IBinder adjacentFragmentToken = hop.getAdjacentRoot();
                TaskFragment tf1 = this.mLaunchTaskFragments.get(fragmentToken3);
                if (adjacentFragmentToken != null) {
                    tf2 = this.mLaunchTaskFragments.get(adjacentFragmentToken);
                } else {
                    tf2 = null;
                }
                if (tf1 != null && (adjacentFragmentToken == null || tf2 != null)) {
                    if (!tf1.isEmbeddedTaskFragmentInPip() && (tf2 == null || !tf2.isEmbeddedTaskFragmentInPip())) {
                        tf1.setAdjacentTaskFragment(tf2, false);
                        int effects3 = effects | 2;
                        if (tf2 == null && tf1.getDisplayContent().mFocusedApp != null) {
                            if (tf1.hasChild(tf1.getDisplayContent().mFocusedApp) && !tf1.shouldBeVisible(null)) {
                                tf1.getDisplayContent().setFocusedApp(null);
                            }
                        }
                        Bundle bundle = hop.getLaunchOptions();
                        WindowContainerTransaction.TaskFragmentAdjacentParams adjacentParams = bundle != null ? new WindowContainerTransaction.TaskFragmentAdjacentParams(bundle) : null;
                        if (adjacentParams != null) {
                            tf1.setDelayLastActivityRemoval(adjacentParams.shouldDelayPrimaryLastActivityRemoval());
                            if (tf2 != null) {
                                tf2.setDelayLastActivityRemoval(adjacentParams.shouldDelaySecondaryLastActivityRemoval());
                                if (tf2.getParent() != null) {
                                    tf2.getParent().moveChildAdjacentWithChild(tf2, new Predicate() { // from class: com.android.server.wm.WindowOrganizerController$$ExternalSyntheticLambda7
                                        @Override // java.util.function.Predicate
                                        public final boolean test(Object obj) {
                                            return WindowOrganizerController.lambda$applyHierarchyOp$6(TaskFragment.this, obj);
                                        }
                                    }, true);
                                }
                            }
                        }
                        str = TAG;
                        str2 = "Attempt to operate on unknown or detached container: ";
                        str3 = "Attempt to operate on detached container: ";
                        effects2 = effects3;
                        i = 0;
                        windowContainer = null;
                        break;
                    }
                    Throwable exception7 = new IllegalArgumentException("Not allowed to set adjacent on TaskFragment in PIP Task");
                    sendTaskFragmentOperationFailure(organizer, errorCallbackToken, exception7);
                    str = TAG;
                    str2 = "Attempt to operate on unknown or detached container: ";
                    str3 = "Attempt to operate on detached container: ";
                    i = 0;
                    windowContainer = null;
                    effects2 = effects;
                    break;
                }
                Throwable exception8 = new IllegalArgumentException("Not allowed to set adjacent on invalid fragment tokens");
                sendTaskFragmentOperationFailure(organizer, errorCallbackToken, exception8);
                str = TAG;
                str2 = "Attempt to operate on unknown or detached container: ";
                str3 = "Attempt to operate on detached container: ";
                i = 0;
                windowContainer = null;
                effects2 = effects;
                break;
            case 18:
                TaskFragment tf3 = this.mLaunchTaskFragments.get(hop.getContainer());
                if (tf3 == null || !tf3.isAttached()) {
                    Slog.e(TAG, "Attempt to operate on detached container: " + tf3);
                    str = TAG;
                    str2 = "Attempt to operate on unknown or detached container: ";
                    str3 = "Attempt to operate on detached container: ";
                    i = 0;
                    windowContainer = null;
                } else {
                    ActivityRecord curFocus = tf3.getDisplayContent().mFocusedApp;
                    if (curFocus != null && curFocus.getTaskFragment() == tf3) {
                        Slog.d(TAG, "The requested TaskFragment already has the focus.");
                        str = TAG;
                        str2 = "Attempt to operate on unknown or detached container: ";
                        str3 = "Attempt to operate on detached container: ";
                        i = 0;
                        windowContainer = null;
                    } else if (curFocus != null && curFocus.getTask() != tf3.getTask()) {
                        Slog.d(TAG, "The Task of the requested TaskFragment doesn't have focus.");
                        str = TAG;
                        str2 = "Attempt to operate on unknown or detached container: ";
                        str3 = "Attempt to operate on detached container: ";
                        i = 0;
                        windowContainer = null;
                    } else {
                        ActivityRecord targetFocus = tf3.getTopResumedActivity();
                        if (targetFocus == null) {
                            Slog.d(TAG, "There is no resumed activity in the requested TaskFragment.");
                            str = TAG;
                            str2 = "Attempt to operate on unknown or detached container: ";
                            str3 = "Attempt to operate on detached container: ";
                            i = 0;
                            windowContainer = null;
                        } else {
                            tf3.getDisplayContent().setFocusedApp(targetFocus);
                            str = TAG;
                            str2 = "Attempt to operate on unknown or detached container: ";
                            str3 = "Attempt to operate on detached container: ";
                            i = 0;
                            windowContainer = null;
                        }
                    }
                }
                effects2 = effects;
                break;
            case 22:
                IBinder fragmentToken4 = hop.getContainer();
                IBinder companionToken = hop.getCompanionContainer();
                TaskFragment fragment = this.mLaunchTaskFragments.get(fragmentToken4);
                if (companionToken != null) {
                    companion = this.mLaunchTaskFragments.get(companionToken);
                } else {
                    companion = null;
                }
                if (fragment == null || !fragment.isAttached()) {
                    Throwable exception9 = new IllegalArgumentException("Not allowed to set companion on invalid fragment tokens");
                    sendTaskFragmentOperationFailure(organizer, errorCallbackToken, exception9);
                    str = TAG;
                    str2 = "Attempt to operate on unknown or detached container: ";
                    str3 = "Attempt to operate on detached container: ";
                    i = 0;
                    windowContainer = null;
                } else {
                    fragment.setCompanionTaskFragment(companion);
                    str = TAG;
                    str2 = "Attempt to operate on unknown or detached container: ";
                    str3 = "Attempt to operate on detached container: ";
                    i = 0;
                    windowContainer = null;
                }
                effects2 = effects;
                break;
            default:
                str = TAG;
                str2 = "Attempt to operate on unknown or detached container: ";
                str3 = "Attempt to operate on detached container: ";
                i = 0;
                windowContainer = null;
                if (isInLockTaskMode) {
                    Slog.w(str, "Skip applying hierarchy operation " + hop + " while in lock task mode");
                    return effects;
                }
                effects2 = effects;
                break;
        }
        switch (type) {
            case 0:
            case 1:
                String str4 = str;
                WindowContainer wc4 = WindowContainer.fromBinder(hop.getContainer());
                if (wc4 == null || !wc4.isAttached()) {
                    Slog.e(str4, str3 + wc4);
                    return effects2;
                }
                if (syncId >= 0) {
                    addToSyncSet(syncId, wc4);
                }
                if (transition != null) {
                    transition.collect(wc4);
                    if (hop.isReparent()) {
                        if (wc4.getParent() != null) {
                            transition.collect(wc4.getParent());
                        }
                        if (hop.getNewParent() != null) {
                            WindowContainer parentWc = WindowContainer.fromBinder(hop.getNewParent());
                            if (parentWc == null) {
                                Slog.e(str4, "Can't resolve parent window from token");
                                return effects2;
                            }
                            transition.collect(parentWc);
                        }
                    }
                }
                return effects2 | sanitizeAndApplyHierarchyOp(wc4, hop);
            case 2:
                return effects2 | reparentChildrenTasksHierarchyOp(hop, transition, syncId);
            case 3:
            case 4:
            case 6:
            case 7:
            case 8:
            case 9:
            case 10:
            case 13:
            default:
                return effects2;
            case 5:
                this.mService.mAmInternal.enforceCallingPermission("android.permission.START_TASKS_FROM_RECENTS", "launchTask HierarchyOp");
                Bundle launchOpts = hop.getLaunchOptions();
                final int taskId = launchOpts.getInt("android:transaction.hop.taskId");
                launchOpts.remove("android:transaction.hop.taskId");
                final SafeActivityOptions safeOptions = SafeActivityOptions.fromBundle(launchOpts, caller.mPid, caller.mUid);
                waitAsyncStart(new IntSupplier() { // from class: com.android.server.wm.WindowOrganizerController$$ExternalSyntheticLambda8
                    @Override // java.util.function.IntSupplier
                    public final int getAsInt() {
                        return WindowOrganizerController.this.m8514xe3110df8(caller, taskId, safeOptions);
                    }
                });
                return effects2;
            case 11:
                WindowContainer oldParent = WindowContainer.fromBinder(hop.getContainer());
                if (hop.getNewParent() != null) {
                    newParent = WindowContainer.fromBinder(hop.getNewParent());
                } else {
                    newParent = windowContainer;
                }
                if (oldParent != null && oldParent.asTaskFragment() != null && oldParent.isAttached()) {
                    reparentTaskFragment(oldParent.asTaskFragment(), newParent, organizer, errorCallbackToken);
                    return effects2 | 2;
                }
                Slog.e(str, str2 + oldParent);
                return effects2;
            case 12:
                int i4 = i;
                if (hop.getActivityIntent() != null) {
                    windowContainer2 = hop.getActivityIntent().resolveTypeIfNeeded(this.mService.mContext.getContentResolver());
                } else {
                    windowContainer2 = windowContainer;
                }
                final String resolvedType = windowContainer2;
                ActivityOptions activityOptions2 = null;
                if (hop.getPendingIntent().isActivity()) {
                    if (hop.getLaunchOptions() != null) {
                        makeBasic = new ActivityOptions(hop.getLaunchOptions());
                    } else {
                        makeBasic = ActivityOptions.makeBasic();
                    }
                    activityOptions2 = makeBasic;
                    activityOptions2.setCallerDisplayId(i4);
                }
                final Bundle options = activityOptions2 != null ? activityOptions2.toBundle() : windowContainer;
                waitAsyncStart(new IntSupplier() { // from class: com.android.server.wm.WindowOrganizerController$$ExternalSyntheticLambda9
                    @Override // java.util.function.IntSupplier
                    public final int getAsInt() {
                        return WindowOrganizerController.this.m8515xe29aa7f9(hop, resolvedType, options);
                    }
                });
                return effects2;
            case 14:
                Bundle launchOpts2 = hop.getLaunchOptions();
                String callingPackage = launchOpts2.getString("android:transaction.hop.shortcut_calling_package");
                launchOpts2.remove("android:transaction.hop.shortcut_calling_package");
                LauncherAppsService.LauncherAppsServiceInternal launcherApps = (LauncherAppsService.LauncherAppsServiceInternal) LocalServices.getService(LauncherAppsService.LauncherAppsServiceInternal.class);
                launcherApps.startShortcut(caller.mUid, caller.mPid, callingPackage, hop.getShortcutInfo().getPackage(), null, hop.getShortcutInfo().getId(), null, launchOpts2, hop.getShortcutInfo().getUserId());
                return effects2;
            case 15:
                if (finishTransition != null && (container = WindowContainer.fromBinder(hop.getContainer())) != null) {
                    Task thisTask = container.asActivityRecord() != null ? container.asActivityRecord().getTask() : container.asTask();
                    if (thisTask != null && (restoreAt = finishTransition.getTransientLaunchRestoreTarget(container)) != null) {
                        TaskDisplayArea taskDisplayArea = thisTask.getTaskDisplayArea();
                        taskDisplayArea.moveRootTaskBehindRootTask(thisTask.getRootTask(), restoreAt);
                        return effects2;
                    }
                    return effects2;
                }
                return effects2;
            case 16:
                Rect insetsProviderWindowContainer = hop.getInsetsProviderFrame();
                WindowContainer receiverWindowContainer = WindowContainer.fromBinder(hop.getContainer());
                receiverWindowContainer.addLocalRectInsetsSourceProvider(insetsProviderWindowContainer, hop.getInsetsTypes());
                return effects2;
            case 17:
                WindowContainer.fromBinder(hop.getContainer()).removeLocalInsetsSourceProvider(hop.getInsetsTypes());
                return effects2;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$applyHierarchyOp$5(ActivityRecord a) {
        return !a.finishing;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$applyHierarchyOp$6(TaskFragment tf2, Object child) {
        return child.getClass() == tf2.getClass();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$applyHierarchyOp$7$com-android-server-wm-WindowOrganizerController  reason: not valid java name */
    public /* synthetic */ int m8514xe3110df8(CallerInfo caller, int taskId, SafeActivityOptions safeOptions) {
        return this.mService.mTaskSupervisor.startActivityFromRecents(caller.mPid, caller.mUid, taskId, safeOptions);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$applyHierarchyOp$8$com-android-server-wm-WindowOrganizerController  reason: not valid java name */
    public /* synthetic */ int m8515xe29aa7f9(WindowContainerTransaction.HierarchyOp hop, String resolvedType, Bundle options) {
        return this.mService.mAmInternal.sendIntentSender(hop.getPendingIntent().getTarget(), hop.getPendingIntent().getWhitelistToken(), 0, hop.getActivityIntent(), resolvedType, (IIntentReceiver) null, (String) null, options);
    }

    private void sendMinimumDimensionViolation(TaskFragment taskFragment, Point minDimensions, IBinder errorCallbackToken, String reason) {
        if (taskFragment == null || taskFragment.getTaskFragmentOrganizer() == null) {
            return;
        }
        Throwable exception = new SecurityException("The task fragment's bounds:" + taskFragment.getBounds() + " does not satisfy minimum dimensions:" + minDimensions + " " + reason);
        sendTaskFragmentOperationFailure(taskFragment.getTaskFragmentOrganizer(), errorCallbackToken, exception);
    }

    private void waitAsyncStart(final IntSupplier startActivity) {
        final Integer[] starterResult = {null};
        this.mService.mH.post(new Runnable() { // from class: com.android.server.wm.WindowOrganizerController$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                WindowOrganizerController.this.m8520x8bdcc26d(starterResult, startActivity);
            }
        });
        while (starterResult[0] == null) {
            try {
                this.mGlobalLock.wait();
            } catch (InterruptedException e) {
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$waitAsyncStart$9$com-android-server-wm-WindowOrganizerController  reason: not valid java name */
    public /* synthetic */ void m8520x8bdcc26d(Integer[] starterResult, IntSupplier startActivity) {
        try {
            starterResult[0] = Integer.valueOf(startActivity.getAsInt());
        } catch (Throwable t) {
            starterResult[0] = -96;
            Slog.w(TAG, t);
        }
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                this.mGlobalLock.notifyAll();
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    private int sanitizeAndApplyHierarchyOp(WindowContainer container, WindowContainerTransaction.HierarchyOp hop) {
        WindowContainer newParent;
        Task task = container.asTask();
        if (task == null) {
            throw new IllegalArgumentException("Invalid container in hierarchy op");
        }
        DisplayContent dc = task.getDisplayContent();
        if (dc == null) {
            Slog.w(TAG, "Container is no longer attached: " + task);
            return 0;
        }
        if (hop.isReparent()) {
            boolean isNonOrganizedRootableTask = task.isRootTask() || task.getParent().asTask().mCreatedByOrganizer;
            if (isNonOrganizedRootableTask) {
                if (hop.getNewParent() == null) {
                    newParent = dc.getDefaultTaskDisplayArea();
                } else {
                    newParent = WindowContainer.fromBinder(hop.getNewParent());
                }
                if (newParent == null) {
                    Slog.e(TAG, "Can't resolve parent window from token");
                    return 0;
                } else if (task.getParent() != newParent) {
                    if (newParent.asTaskDisplayArea() != null) {
                        executeAppTransition(task, newParent);
                        task.reparent(newParent.asTaskDisplayArea(), hop.getToTop());
                        return 2;
                    } else if (newParent.asTask() != null) {
                        if (newParent.inMultiWindowMode() && task.isLeafTask()) {
                            if (newParent.inPinnedWindowingMode()) {
                                Slog.w(TAG, "Can't support moving a task to another PIP window... newParent=" + newParent + " task=" + task);
                                return 0;
                            } else if (!task.supportsMultiWindowInDisplayArea(newParent.asTask().getDisplayArea())) {
                                Slog.w(TAG, "Can't support task that doesn't support multi-window mode in multi-window mode... newParent=" + newParent + " task=" + task);
                                return 0;
                            }
                        }
                        task.reparent((Task) newParent, hop.getToTop() ? Integer.MAX_VALUE : Integer.MIN_VALUE, false, "sanitizeAndApplyHierarchyOp");
                        return 2;
                    } else {
                        throw new RuntimeException("Can only reparent task to another task or taskDisplayArea, but not " + newParent);
                    }
                } else {
                    Task rootTask = (newParent == null || (newParent instanceof TaskDisplayArea)) ? task.getRootTask() : newParent;
                    task.getDisplayArea().positionChildAt(hop.getToTop() ? Integer.MAX_VALUE : Integer.MIN_VALUE, rootTask, false);
                    return 2;
                }
            }
            throw new RuntimeException("Reparenting leaf Tasks is not supported now. " + task);
        }
        if (ITranTask.Instance().isAdjacentTaskEnable() && task.getParent().asTaskDisplayArea() != null && !hop.getToTop()) {
            ITranTask.Instance().hookResetAdjacentTask();
        }
        task.getParent().positionChildAt(hop.getToTop() ? Integer.MAX_VALUE : Integer.MIN_VALUE, task, false);
        return 2;
    }

    private int reparentChildrenTasksHierarchyOp(final WindowContainerTransaction.HierarchyOp hop, Transition transition, int syncId) {
        WindowContainer<?> currentParent;
        WindowContainer<?> newParent;
        final TaskDisplayArea newParentTda;
        WindowOrganizerController windowOrganizerController = this;
        WindowContainer<?> currentParent2 = hop.getContainer() != null ? WindowContainer.fromBinder(hop.getContainer()) : null;
        WindowContainer<?> newParent2 = hop.getNewParent() != null ? WindowContainer.fromBinder(hop.getNewParent()) : null;
        if (currentParent2 == null && newParent2 == null) {
            throw new IllegalArgumentException("reparentChildrenTasksHierarchyOp: " + hop);
        }
        if (currentParent2 == null) {
            currentParent = newParent2.asTask().getDisplayContent().getDefaultTaskDisplayArea();
            newParent = newParent2;
        } else if (newParent2 != null) {
            currentParent = currentParent2;
            newParent = newParent2;
        } else if (currentParent2.asTask().getDisplayContent() == null) {
            Slog.w(TAG, "currentParent contains displayContent is null, so force change to rootWindowContainer displayarea");
            currentParent = currentParent2;
            newParent = windowOrganizerController.mService.mRootWindowContainer.getDefaultTaskDisplayArea();
        } else {
            currentParent = currentParent2;
            newParent = currentParent2.asTask().getDisplayContent().getDefaultTaskDisplayArea();
        }
        if (currentParent == newParent) {
            Slog.e(TAG, "reparentChildrenTasksHierarchyOp parent not changing: " + hop);
            return 0;
        } else if (!currentParent.isAttached()) {
            Slog.e(TAG, "reparentChildrenTasksHierarchyOp currentParent detached=" + currentParent + " hop=" + hop);
            return 0;
        } else if (!newParent.isAttached()) {
            Slog.e(TAG, "reparentChildrenTasksHierarchyOp newParent detached=" + newParent + " hop=" + hop);
            return 0;
        } else if (newParent.inPinnedWindowingMode()) {
            Slog.e(TAG, "reparentChildrenTasksHierarchyOp newParent in PIP=" + newParent + " hop=" + hop);
            return 0;
        } else {
            final boolean newParentInMultiWindow = newParent.inMultiWindowMode();
            if (newParent.asTask() != null) {
                newParentTda = newParent.asTask().getDisplayArea();
            } else {
                newParentTda = newParent.asTaskDisplayArea();
            }
            final WindowContainer<?> finalCurrentParent = currentParent;
            Slog.i(TAG, "reparentChildrenTasksHierarchyOp currentParent=" + currentParent + " newParent=" + newParent + " hop=" + hop);
            final ArrayList<Task> tasksToReparent = new ArrayList<>();
            currentParent.forAllTasks(new Predicate() { // from class: com.android.server.wm.WindowOrganizerController$$ExternalSyntheticLambda11
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return WindowOrganizerController.lambda$reparentChildrenTasksHierarchyOp$10(WindowContainer.this, newParentInMultiWindow, newParentTda, hop, tasksToReparent, (Task) obj);
                }
            });
            int count = tasksToReparent.size();
            int i = 0;
            while (i < count) {
                Task task = tasksToReparent.get(i);
                if (syncId >= 0) {
                    windowOrganizerController.addToSyncSet(syncId, task);
                }
                if (transition != null) {
                    transition.collect(task);
                }
                if (!(newParent instanceof TaskDisplayArea)) {
                    task.reparent((Task) newParent, hop.getToTop() ? Integer.MAX_VALUE : Integer.MIN_VALUE, false, "processChildrenTaskReparentHierarchyOp");
                } else {
                    if (ITranTask.Instance().isAdjacentTaskEnable()) {
                        ITranTask.Instance().hookLookupAdjacentTask(task);
                    }
                    if (ThunderbackConfig.isVersion4()) {
                        windowOrganizerController.executeAppTransition(task, newParent);
                    }
                    task.reparent((TaskDisplayArea) newParent, hop.getToTop());
                }
                i++;
                windowOrganizerController = this;
            }
            if (transition != null) {
                transition.collect(newParent);
                return 2;
            }
            return 2;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$reparentChildrenTasksHierarchyOp$10(WindowContainer finalCurrentParent, boolean newParentInMultiWindow, TaskDisplayArea newParentTda, WindowContainerTransaction.HierarchyOp hop, ArrayList tasksToReparent, Task task) {
        Slog.i(TAG, " Processing task=" + task);
        if (task.mCreatedByOrganizer || task.getParent() != finalCurrentParent) {
            return false;
        }
        if (newParentInMultiWindow && !task.supportsMultiWindowInDisplayArea(newParentTda)) {
            Slog.e(TAG, "reparentChildrenTasksHierarchyOp non-resizeable task to multi window, task=" + task);
            return false;
        } else if (ArrayUtils.contains(hop.getActivityTypes(), task.getActivityType()) && ArrayUtils.contains(hop.getWindowingModes(), task.getWindowingMode())) {
            if (hop.getToTop()) {
                tasksToReparent.add(0, task);
            } else {
                tasksToReparent.add(task);
            }
            return hop.getReparentTopOnly() && tasksToReparent.size() == 1;
        } else {
            return false;
        }
    }

    private int setAdjacentRootsHierarchyOp(WindowContainerTransaction.HierarchyOp hop) {
        TaskFragment root1 = WindowContainer.fromBinder(hop.getContainer()).asTaskFragment();
        TaskFragment root2 = WindowContainer.fromBinder(hop.getAdjacentRoot()).asTaskFragment();
        if (!root1.mCreatedByOrganizer || !root2.mCreatedByOrganizer) {
            throw new IllegalArgumentException("setAdjacentRootsHierarchyOp: Not created by organizer root1=" + root1 + " root2=" + root2);
        }
        if (root1.isEmbeddedTaskFragmentInPip() || root2.isEmbeddedTaskFragmentInPip()) {
            Slog.e(TAG, "Attempt to set adjacent TaskFragment in PIP Task");
            return 0;
        }
        root1.setAdjacentTaskFragment(root2, hop.getMoveAdjacentTogether());
        return 2;
    }

    private void sanitizeWindowContainer(WindowContainer wc) {
        if (!(wc instanceof TaskFragment) && !(wc instanceof DisplayArea)) {
            throw new RuntimeException("Invalid token in task fragment or displayArea transaction");
        }
    }

    private int applyWindowContainerChange(WindowContainer wc, WindowContainerTransaction.Change c, IBinder errorCallbackToken) {
        sanitizeWindowContainer(wc);
        if (wc.asTaskFragment() != null && wc.asTaskFragment().isEmbeddedTaskFragmentInPip()) {
            return 0;
        }
        int effects = applyChanges(wc, c, errorCallbackToken);
        if (wc instanceof DisplayArea) {
            return effects | applyDisplayAreaChanges(wc.asDisplayArea(), c);
        }
        if (wc instanceof Task) {
            return effects | applyTaskChanges(wc.asTask(), c);
        }
        return effects;
    }

    public ITaskOrganizerController getTaskOrganizerController() {
        enforceTaskPermission("getTaskOrganizerController()");
        return this.mTaskOrganizerController;
    }

    public IDisplayAreaOrganizerController getDisplayAreaOrganizerController() {
        enforceTaskPermission("getDisplayAreaOrganizerController()");
        return this.mDisplayAreaOrganizerController;
    }

    public ITaskFragmentOrganizerController getTaskFragmentOrganizerController() {
        return this.mTaskFragmentOrganizerController;
    }

    private BLASTSyncEngine.SyncGroup prepareSyncWithOrganizer(IWindowContainerTransactionCallback callback) {
        BLASTSyncEngine.SyncGroup s = this.mService.mWindowManager.mSyncEngine.prepareSyncSet(this, "");
        this.mTransactionCallbacksByPendingSyncId.put(Integer.valueOf(s.mSyncId), callback);
        return s;
    }

    int startSyncWithOrganizer(IWindowContainerTransactionCallback callback) {
        BLASTSyncEngine.SyncGroup s = prepareSyncWithOrganizer(callback);
        this.mService.mWindowManager.mSyncEngine.startSyncSet(s);
        return s.mSyncId;
    }

    void setSyncReady(int id) {
        if (ProtoLogCache.WM_DEBUG_WINDOW_ORGANIZER_enabled) {
            long protoLogParam0 = id;
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_WINDOW_ORGANIZER, -930893991, 1, (String) null, new Object[]{Long.valueOf(protoLogParam0)});
        }
        this.mService.mWindowManager.mSyncEngine.setReady(id);
    }

    void addToSyncSet(int syncId, WindowContainer wc) {
        this.mService.mWindowManager.mSyncEngine.addToSyncSet(syncId, wc);
    }

    @Override // com.android.server.wm.BLASTSyncEngine.TransactionReadyListener
    public void onTransactionReady(int syncId, SurfaceControl.Transaction t) {
        if (ProtoLogCache.WM_DEBUG_WINDOW_ORGANIZER_enabled) {
            long protoLogParam0 = syncId;
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_WINDOW_ORGANIZER, -497620140, 1, (String) null, new Object[]{Long.valueOf(protoLogParam0)});
        }
        IWindowContainerTransactionCallback callback = this.mTransactionCallbacksByPendingSyncId.get(Integer.valueOf(syncId));
        try {
            callback.onTransactionReady(syncId, t);
        } catch (RemoteException e) {
            t.apply();
        }
        this.mTransactionCallbacksByPendingSyncId.remove(Integer.valueOf(syncId));
    }

    public void registerTransitionPlayer(ITransitionPlayer player) {
        enforceTaskPermission("registerTransitionPlayer()");
        int callerPid = Binder.getCallingPid();
        int callerUid = Binder.getCallingUid();
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                WindowManagerService.boostPriorityForLockedSection();
                WindowProcessController wpc = this.mService.getProcessController(callerPid, callerUid);
                IApplicationThread appThread = null;
                if (wpc != null) {
                    appThread = wpc.getThread();
                }
                this.mTransitionController.registerTransitionPlayer(player, appThread);
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public ITransitionMetricsReporter getTransitionMetricsReporter() {
        return this.mTransitionController.mTransitionMetricsReporter;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean configurationsAreEqualForOrganizer(Configuration newConfig, Configuration oldConfig) {
        int winCfgChanges;
        if (oldConfig == null) {
            return false;
        }
        int cfgChanges = newConfig.diff(oldConfig);
        if ((536870912 & cfgChanges) != 0) {
            winCfgChanges = (int) newConfig.windowConfiguration.diff(oldConfig.windowConfiguration, true);
        } else {
            winCfgChanges = 0;
        }
        if ((winCfgChanges & 3) == 0) {
            cfgChanges &= -536870913;
        }
        return (CONTROLLABLE_CONFIGS & cfgChanges) == 0;
    }

    private void enforceTaskPermission(String func) {
        ActivityTaskManagerService.enforceTaskPermission(func);
    }

    private void enforceTaskPermission(String func, WindowContainerTransaction t) {
        if (t == null || t.getTaskFragmentOrganizer() == null) {
            enforceTaskPermission(func);
        } else {
            enforceOperationsAllowedForTaskFragmentOrganizer(func, t);
        }
    }

    private void enforceOperationsAllowedForTaskFragmentOrganizer(String func, WindowContainerTransaction t) {
        ITaskFragmentOrganizer organizer = t.getTaskFragmentOrganizer();
        for (Map.Entry<IBinder, WindowContainerTransaction.Change> entry : t.getChanges().entrySet()) {
            WindowContainer wc = WindowContainer.fromBinder(entry.getKey());
            enforceTaskFragmentOrganized(func, wc, organizer);
            enforceTaskFragmentConfigChangeAllowed(func, wc, entry.getValue(), organizer);
        }
        List<WindowContainerTransaction.HierarchyOp> hops = t.getHierarchyOps();
        for (int i = hops.size() - 1; i >= 0; i--) {
            WindowContainerTransaction.HierarchyOp hop = hops.get(i);
            int type = hop.getType();
            switch (type) {
                case 1:
                case 8:
                    enforceTaskFragmentOrganized(func, WindowContainer.fromBinder(hop.getContainer()), organizer);
                    break;
                case 4:
                    enforceTaskFragmentOrganized(func, WindowContainer.fromBinder(hop.getContainer()), organizer);
                    enforceTaskFragmentOrganized(func, WindowContainer.fromBinder(hop.getAdjacentRoot()), organizer);
                    break;
                case 7:
                case 9:
                case 10:
                case 13:
                case 18:
                    break;
                case 11:
                    enforceTaskFragmentOrganized(func, WindowContainer.fromBinder(hop.getContainer()), organizer);
                    if (hop.getNewParent() != null) {
                        enforceTaskFragmentOrganized(func, WindowContainer.fromBinder(hop.getNewParent()), organizer);
                        break;
                    } else {
                        break;
                    }
                case 22:
                    enforceTaskFragmentOrganized(func, hop.getContainer(), organizer);
                    if (hop.getCompanionContainer() != null) {
                        enforceTaskFragmentOrganized(func, hop.getCompanionContainer(), organizer);
                        break;
                    } else {
                        break;
                    }
                default:
                    String msg = "Permission Denial: " + func + " from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " trying to apply a hierarchy change that is not allowed for TaskFragmentOrganizer=" + organizer;
                    Slog.w(TAG, msg);
                    throw new SecurityException(msg);
            }
        }
    }

    private void enforceTaskFragmentOrganized(String func, WindowContainer wc, ITaskFragmentOrganizer organizer) {
        if (wc == null) {
            Slog.e(TAG, "Attempt to operate on window that no longer exists");
            return;
        }
        TaskFragment tf = wc.asTaskFragment();
        if (tf == null || !tf.hasTaskFragmentOrganizer(organizer)) {
            String msg = "Permission Denial: " + func + " from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " trying to modify window container not belonging to the TaskFragmentOrganizer=" + organizer;
            Slog.w(TAG, msg);
            throw new SecurityException(msg);
        }
    }

    private void enforceTaskFragmentOrganized(String func, IBinder fragmentToken, ITaskFragmentOrganizer organizer) {
        Objects.requireNonNull(fragmentToken);
        TaskFragment tf = this.mLaunchTaskFragments.get(fragmentToken);
        if (tf != null && !tf.hasTaskFragmentOrganizer(organizer)) {
            String msg = "Permission Denial: " + func + " from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " trying to modify TaskFragment not belonging to the TaskFragmentOrganizer=" + organizer;
            Slog.w(TAG, msg);
            throw new SecurityException(msg);
        }
    }

    private void enforceTaskFragmentConfigChangeAllowed(String func, WindowContainer wc, WindowContainerTransaction.Change change, ITaskFragmentOrganizer organizer) {
        if (wc == null) {
            Slog.e(TAG, "Attempt to operate on task fragment that no longer exists");
        } else if (change == null) {
        } else {
            int changeMask = change.getChangeMask();
            if (changeMask != 0) {
                String msg = "Permission Denial: " + func + " from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " trying to apply changes of " + changeMask + " to TaskFragment TaskFragmentOrganizer=" + organizer;
                Slog.w(TAG, msg);
                throw new SecurityException(msg);
            } else if (wc.asTaskFragment().isAllowedToBeEmbeddedInTrustedMode()) {
            } else {
                WindowContainer wcParent = wc.getParent();
                if (wcParent == null) {
                    Slog.e(TAG, "Attempt to apply config change on task fragment that has no parent");
                    return;
                }
                Configuration requestedConfig = change.getConfiguration();
                Configuration parentConfig = wcParent.getConfiguration();
                if (parentConfig.screenWidthDp < requestedConfig.screenWidthDp || parentConfig.screenHeightDp < requestedConfig.screenHeightDp || parentConfig.smallestScreenWidthDp < requestedConfig.smallestScreenWidthDp) {
                    String msg2 = "Permission Denial: " + func + " from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " trying to apply screen width/height greater than parent's for non-trusted host, TaskFragmentOrganizer=" + organizer;
                    Slog.w(TAG, msg2);
                    throw new SecurityException(msg2);
                } else if (change.getWindowSetMask() == 0) {
                } else {
                    WindowConfiguration requestedWindowConfig = requestedConfig.windowConfiguration;
                    WindowConfiguration parentWindowConfig = parentConfig.windowConfiguration;
                    if (!requestedWindowConfig.getBounds().isEmpty() && !parentWindowConfig.getBounds().contains(requestedWindowConfig.getBounds())) {
                        String msg3 = "Permission Denial: " + func + " from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " trying to apply bounds outside of parent for non-trusted host, TaskFragmentOrganizer=" + organizer;
                        Slog.w(TAG, msg3);
                        throw new SecurityException(msg3);
                    } else if (requestedWindowConfig.getAppBounds() != null && !requestedWindowConfig.getAppBounds().isEmpty() && parentWindowConfig.getAppBounds() != null && !parentWindowConfig.getAppBounds().contains(requestedWindowConfig.getAppBounds())) {
                        String msg4 = "Permission Denial: " + func + " from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " trying to apply app bounds outside of parent for non-trusted host, TaskFragmentOrganizer=" + organizer;
                        Slog.w(TAG, msg4);
                        throw new SecurityException(msg4);
                    }
                }
            }
        }
    }

    void createTaskFragment(TaskFragmentCreationParams creationParams, IBinder errorCallbackToken, CallerInfo caller) {
        ActivityRecord ownerActivity = ActivityRecord.forTokenLocked(creationParams.getOwnerToken());
        ITaskFragmentOrganizer organizer = ITaskFragmentOrganizer.Stub.asInterface(creationParams.getOrganizer().asBinder());
        if (ownerActivity == null || ownerActivity.getTask() == null) {
            Throwable exception = new IllegalArgumentException("Not allowed to operate with invalid ownerToken");
            sendTaskFragmentOperationFailure(organizer, errorCallbackToken, exception);
        } else if (!ownerActivity.isResizeable()) {
            IllegalArgumentException exception2 = new IllegalArgumentException("Not allowed to operate with non-resizable owner Activity");
            sendTaskFragmentOperationFailure(organizer, errorCallbackToken, exception2);
        } else {
            Task ownerTask = ownerActivity.getTask();
            if ((ownerTask.effectiveUid != ownerActivity.getUid() || ownerTask.effectiveUid != caller.mUid) && !IWindowOrganizerControllerLice.instance().onCreateTaskFragment(ownerActivity, 1)) {
                Throwable exception3 = new SecurityException("Not allowed to operate with the ownerToken while the root activity of the target task belong to the different app");
                sendTaskFragmentOperationFailure(organizer, errorCallbackToken, exception3);
            } else if (ownerTask.inPinnedWindowingMode()) {
                Throwable exception4 = new IllegalArgumentException("Not allowed to create TaskFragment in PIP Task");
                sendTaskFragmentOperationFailure(organizer, errorCallbackToken, exception4);
            } else if (ownerTask.getNonFinishingActivityCount() == 0) {
                Slog.d(TAG, "Not allowed to create TaskFragment in finishing task.");
            } else {
                TaskFragment taskFragment = new TaskFragment(this.mService, creationParams.getFragmentToken(), true);
                taskFragment.setTaskFragmentOrganizer(creationParams.getOrganizer(), ownerActivity.getUid(), ownerActivity.info.processName);
                ownerTask.addChild(taskFragment, Integer.MAX_VALUE);
                taskFragment.setWindowingMode(creationParams.getWindowingMode());
                taskFragment.setBounds(creationParams.getInitialBounds());
                this.mLaunchTaskFragments.put(creationParams.getFragmentToken(), taskFragment);
            }
        }
    }

    void reparentTaskFragment(TaskFragment oldParent, WindowContainer<?> newParent, ITaskFragmentOrganizer organizer, IBinder errorCallbackToken) {
        final TaskFragment newParentTF;
        if (newParent == null) {
            newParentTF = oldParent.getTask();
        } else {
            newParentTF = newParent.asTaskFragment();
        }
        if (newParentTF == null) {
            Throwable exception = new IllegalArgumentException("Not allowed to operate with invalid container");
            sendTaskFragmentOperationFailure(organizer, errorCallbackToken, exception);
            return;
        }
        if (newParentTF.getTaskFragmentOrganizer() != null) {
            boolean isEmbeddingDisallowed = oldParent.forAllActivities(new Predicate() { // from class: com.android.server.wm.WindowOrganizerController$$ExternalSyntheticLambda5
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return WindowOrganizerController.lambda$reparentTaskFragment$11(TaskFragment.this, (ActivityRecord) obj);
                }
            });
            if (isEmbeddingDisallowed) {
                Throwable exception2 = new SecurityException("The new parent is not allowed to embed the activities.");
                sendTaskFragmentOperationFailure(organizer, errorCallbackToken, exception2);
                return;
            }
        }
        boolean isEmbeddingDisallowed2 = newParentTF.isEmbeddedTaskFragmentInPip();
        if (isEmbeddingDisallowed2 || oldParent.isEmbeddedTaskFragmentInPip()) {
            Throwable exception3 = new SecurityException("Not allow to reparent in TaskFragment in PIP Task.");
            sendTaskFragmentOperationFailure(organizer, errorCallbackToken, exception3);
        } else if (newParentTF.getTask() != oldParent.getTask()) {
            Throwable exception4 = new SecurityException("The new parent is not in the same Task as the old parent.");
            sendTaskFragmentOperationFailure(organizer, errorCallbackToken, exception4);
        } else {
            while (oldParent.hasChild()) {
                oldParent.getChildAt(0).reparent(newParentTF, Integer.MAX_VALUE);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$reparentTaskFragment$11(TaskFragment newParentTF, ActivityRecord activity) {
        return newParentTF.isAllowedToEmbedActivity(activity) != 0;
    }

    private int deleteTaskFragment(TaskFragment taskFragment, ITaskFragmentOrganizer organizer, IBinder errorCallbackToken) {
        int index = this.mLaunchTaskFragments.indexOfValue(taskFragment);
        if (index < 0) {
            Throwable exception = new IllegalArgumentException("Not allowed to operate with invalid taskFragment");
            sendTaskFragmentOperationFailure(organizer, errorCallbackToken, exception);
            return 0;
        } else if (taskFragment.isEmbeddedTaskFragmentInPip() && taskFragment.getTopNonFinishingActivity() != null) {
            Throwable exception2 = new IllegalArgumentException("Not allowed to delete TaskFragment in PIP Task");
            sendTaskFragmentOperationFailure(organizer, errorCallbackToken, exception2);
            return 0;
        } else {
            this.mLaunchTaskFragments.removeAt(index);
            taskFragment.remove(true, "deleteTaskFragment");
            return 2;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public TaskFragment getTaskFragment(IBinder tfToken) {
        return this.mLaunchTaskFragments.get(tfToken);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void cleanUpEmbeddedTaskFragment(TaskFragment taskFragment) {
        this.mLaunchTaskFragments.remove(taskFragment.getFragmentToken());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static class CallerInfo {
        final int mPid = Binder.getCallingPid();
        final int mUid = Binder.getCallingUid();

        CallerInfo() {
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void sendTaskFragmentOperationFailure(ITaskFragmentOrganizer organizer, IBinder errorCallbackToken, Throwable exception) {
        if (organizer == null) {
            throw new IllegalArgumentException("Not allowed to operate with invalid organizer");
        }
        this.mService.mTaskFragmentOrganizerController.onTaskFragmentError(organizer, errorCallbackToken, exception);
    }

    private Throwable convertStartFailureToThrowable(int result, Intent intent) {
        switch (result) {
            case -96:
                return new AndroidRuntimeException("Activity could not be started for " + intent + " with error code : " + result);
            case -95:
            case -93:
            default:
                return new AndroidRuntimeException("Start activity failed with error code : " + result + " when starting " + intent);
            case -94:
                return new SecurityException("Permission denied and not allowed to start activity " + intent);
            case -92:
            case -91:
                return new ActivityNotFoundException("No Activity found to handle " + intent);
        }
    }

    private void executeAppTransition(Task task, WindowContainer newParent) {
        DisplayContent defaultDisplay = this.mService.mRootWindowContainer.getDefaultDisplay();
        if (task != null && newParent != null) {
            if ((task.getConfiguration().windowConfiguration.isThunderbackWindow() || newParent.getConfiguration().windowConfiguration.isThunderbackWindow()) && defaultDisplay != null) {
                boolean isReparentFromMultiWin = task.getParent().asTaskDisplayArea() != null ? task.getParent().asTaskDisplayArea().isMultiWindow() : false;
                boolean isReparentToMultiWin = newParent.asTaskDisplayArea() != null ? newParent.asTaskDisplayArea().isMultiWindow() : false;
                if (newParent.asTaskDisplayArea() == defaultDisplay.getDefaultTaskDisplayArea() && isReparentFromMultiWin) {
                    defaultDisplay.prepareAppTransition(33);
                    ActivityRecord thunderbackTopActivity = task.getTopNonFinishingActivity();
                    if (thunderbackTopActivity != null) {
                        thunderbackTopActivity.prepareOpenningAction();
                    }
                    defaultDisplay.executeAppTransition();
                } else if (task.getParent().asTaskDisplayArea() == defaultDisplay.getDefaultTaskDisplayArea() && isReparentToMultiWin && this.mService.mRootWindowContainer.needMultiWindowAnimation()) {
                    defaultDisplay.prepareAppTransition(32);
                    defaultDisplay.executeAppTransition();
                }
            }
        }
    }
}
