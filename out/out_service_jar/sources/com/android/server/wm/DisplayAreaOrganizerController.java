package com.android.server.wm;

import android.content.pm.ParceledListSlice;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Slog;
import android.view.SurfaceControl;
import android.window.DisplayAreaAppearedInfo;
import android.window.IDisplayAreaOrganizer;
import android.window.IDisplayAreaOrganizerController;
import android.window.WindowContainerToken;
import com.android.internal.protolog.ProtoLogGroup;
import com.android.internal.protolog.ProtoLogImpl;
import com.android.server.wm.DisplayArea;
import com.android.server.wm.DisplayAreaOrganizerController;
import com.transsion.hubcore.server.wm.ITranDisplayArea;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
/* loaded from: classes2.dex */
public class DisplayAreaOrganizerController extends IDisplayAreaOrganizerController.Stub {
    private static final String TAG = "DisplayAreaOrganizerController";
    private final WindowManagerGlobalLock mGlobalLock;
    final ActivityTaskManagerService mService;
    private int mNextTaskDisplayAreaFeatureId = 20002;
    private final HashMap<Integer, DisplayAreaOrganizerState> mOrganizersByFeatureIds = new HashMap<>();
    private boolean mFromMultiwindow = false;

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class DeathRecipient implements IBinder.DeathRecipient {
        int mFeature;
        IDisplayAreaOrganizer mOrganizer;

        DeathRecipient(IDisplayAreaOrganizer organizer, int feature) {
            this.mOrganizer = organizer;
            this.mFeature = feature;
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            synchronized (DisplayAreaOrganizerController.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    IDisplayAreaOrganizer featureOrganizer = DisplayAreaOrganizerController.this.getOrganizerByFeature(this.mFeature);
                    if (featureOrganizer != null) {
                        IBinder organizerBinder = featureOrganizer.asBinder();
                        if (!organizerBinder.equals(this.mOrganizer.asBinder()) && organizerBinder.isBinderAlive()) {
                            Slog.d(DisplayAreaOrganizerController.TAG, "Dead organizer replaced for feature=" + this.mFeature);
                            WindowManagerService.resetPriorityAfterLockedSection();
                            return;
                        }
                        ((DisplayAreaOrganizerState) DisplayAreaOrganizerController.this.mOrganizersByFeatureIds.remove(Integer.valueOf(this.mFeature))).destroy();
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class DisplayAreaOrganizerState {
        private final DeathRecipient mDeathRecipient;
        private final IDisplayAreaOrganizer mOrganizer;

        DisplayAreaOrganizerState(IDisplayAreaOrganizer organizer, int feature) {
            this.mOrganizer = organizer;
            DeathRecipient deathRecipient = new DeathRecipient(organizer, feature);
            this.mDeathRecipient = deathRecipient;
            try {
                organizer.asBinder().linkToDeath(deathRecipient, 0);
            } catch (RemoteException e) {
            }
        }

        void destroy() {
            final IBinder organizerBinder = this.mOrganizer.asBinder();
            DisplayAreaOrganizerController.this.mService.mRootWindowContainer.forAllDisplayAreas(new Consumer() { // from class: com.android.server.wm.DisplayAreaOrganizerController$DisplayAreaOrganizerState$$ExternalSyntheticLambda0
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    DisplayAreaOrganizerController.DisplayAreaOrganizerState.this.m7898xc705459b(organizerBinder, (DisplayArea) obj);
                }
            });
            organizerBinder.unlinkToDeath(this.mDeathRecipient, 0);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$destroy$0$com-android-server-wm-DisplayAreaOrganizerController$DisplayAreaOrganizerState  reason: not valid java name */
        public /* synthetic */ void m7898xc705459b(IBinder organizerBinder, DisplayArea da) {
            if (da.mOrganizer != null && da.mOrganizer.asBinder().equals(organizerBinder)) {
                if (da.isTaskDisplayArea() && da.asTaskDisplayArea().mCreatedByOrganizer) {
                    DisplayAreaOrganizerController.this.deleteTaskDisplayArea(da.asTaskDisplayArea());
                    return;
                }
                da.setOrganizer(null);
                if (DisplayAreaOrganizerController.this.mFromMultiwindow) {
                    da.mDisplayContent.updateImeParent();
                    DisplayAreaOrganizerController.this.mFromMultiwindow = false;
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public DisplayAreaOrganizerController(ActivityTaskManagerService atm) {
        this.mService = atm;
        this.mGlobalLock = atm.mGlobalLock;
    }

    private void enforceTaskPermission(String func) {
        ActivityTaskManagerService.enforceTaskPermission(func);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public IDisplayAreaOrganizer getOrganizerByFeature(int featureId) {
        DisplayAreaOrganizerState state = this.mOrganizersByFeatureIds.get(Integer.valueOf(featureId));
        if (state != null) {
            return state.mOrganizer;
        }
        return null;
    }

    public ParceledListSlice<DisplayAreaAppearedInfo> registerImeOrganizer(final IDisplayAreaOrganizer organizer, final int feature, final boolean dettachImeWithActivity) {
        enforceTaskPermission("registerOrganizer()");
        long uid = Binder.getCallingUid();
        long origId = Binder.clearCallingIdentity();
        try {
            try {
                synchronized (this.mGlobalLock) {
                    try {
                        WindowManagerService.boostPriorityForLockedSection();
                        if (ProtoLogCache.WM_DEBUG_WINDOW_ORGANIZER_enabled) {
                            try {
                                String protoLogParam0 = String.valueOf(organizer.asBinder());
                                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_WINDOW_ORGANIZER, 251812577, 4, (String) null, new Object[]{protoLogParam0, Long.valueOf(uid)});
                            } catch (Throwable th) {
                                th = th;
                                try {
                                    WindowManagerService.resetPriorityAfterLockedSection();
                                    throw th;
                                } catch (Throwable th2) {
                                    th = th2;
                                    Binder.restoreCallingIdentity(origId);
                                    throw th;
                                }
                            }
                        }
                        if (this.mOrganizersByFeatureIds.get(Integer.valueOf(feature)) != null) {
                            if (this.mOrganizersByFeatureIds.get(Integer.valueOf(feature)).mOrganizer.asBinder().isBinderAlive()) {
                                throw new IllegalStateException("Replacing existing organizer currently unsupported");
                            }
                            this.mOrganizersByFeatureIds.remove(Integer.valueOf(feature)).destroy();
                            Slog.d(TAG, "Replacing dead organizer for feature=" + feature);
                        }
                        try {
                            DisplayAreaOrganizerState state = new DisplayAreaOrganizerState(organizer, feature);
                            final List<DisplayAreaAppearedInfo> displayAreaInfos = new ArrayList<>();
                            this.mService.mRootWindowContainer.forAllDisplays(new Consumer() { // from class: com.android.server.wm.DisplayAreaOrganizerController$$ExternalSyntheticLambda2
                                @Override // java.util.function.Consumer
                                public final void accept(Object obj) {
                                    DisplayAreaOrganizerController.this.m7896xaac5c61c(feature, displayAreaInfos, organizer, dettachImeWithActivity, (DisplayContent) obj);
                                }
                            });
                            this.mOrganizersByFeatureIds.put(Integer.valueOf(feature), state);
                            ParceledListSlice<DisplayAreaAppearedInfo> parceledListSlice = new ParceledListSlice<>(displayAreaInfos);
                            WindowManagerService.resetPriorityAfterLockedSection();
                            Binder.restoreCallingIdentity(origId);
                            return parceledListSlice;
                        } catch (Throwable th3) {
                            th = th3;
                            WindowManagerService.resetPriorityAfterLockedSection();
                            throw th;
                        }
                    } catch (Throwable th4) {
                        th = th4;
                    }
                }
            } catch (Throwable th5) {
                th = th5;
            }
        } catch (Throwable th6) {
            th = th6;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$registerImeOrganizer$1$com-android-server-wm-DisplayAreaOrganizerController  reason: not valid java name */
    public /* synthetic */ void m7896xaac5c61c(final int feature, final List displayAreaInfos, final IDisplayAreaOrganizer organizer, boolean dettachImeWithActivity, DisplayContent dc) {
        if (!dc.isTrusted()) {
            if (ProtoLogCache.WM_DEBUG_WINDOW_ORGANIZER_enabled) {
                long protoLogParam0 = dc.getDisplayId();
                ProtoLogImpl.w(ProtoLogGroup.WM_DEBUG_WINDOW_ORGANIZER, 1699269281, 1, (String) null, new Object[]{Long.valueOf(protoLogParam0)});
                return;
            }
            return;
        }
        dc.forAllDisplayAreas(new Consumer() { // from class: com.android.server.wm.DisplayAreaOrganizerController$$ExternalSyntheticLambda4
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                DisplayAreaOrganizerController.this.m7895x90aa477d(feature, displayAreaInfos, organizer, (DisplayArea) obj);
            }
        });
        if (feature == 7 && dc.isDefaultDisplay) {
            dc.mDettachImeWithActivity = dettachImeWithActivity;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$registerImeOrganizer$0$com-android-server-wm-DisplayAreaOrganizerController  reason: not valid java name */
    public /* synthetic */ void m7895x90aa477d(int feature, List displayAreaInfos, IDisplayAreaOrganizer organizer, DisplayArea da) {
        if (da.mFeatureId != feature) {
            return;
        }
        displayAreaInfos.add(organizeDisplayArea(organizer, da, "DisplayAreaOrganizerController.registerOrganizer"));
    }

    public ParceledListSlice<DisplayAreaAppearedInfo> registerOrganizer(IDisplayAreaOrganizer organizer, int feature) {
        return registerImeOrganizer(organizer, feature, false);
    }

    public void unregisterOrganizer(final IDisplayAreaOrganizer organizer) {
        enforceTaskPermission("unregisterTaskOrganizer()");
        long uid = Binder.getCallingUid();
        long origId = Binder.clearCallingIdentity();
        try {
            try {
                try {
                    synchronized (this.mGlobalLock) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            if (ProtoLogCache.WM_DEBUG_WINDOW_ORGANIZER_enabled) {
                                String protoLogParam0 = String.valueOf(organizer.asBinder());
                                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_WINDOW_ORGANIZER, 1149424314, 4, (String) null, new Object[]{protoLogParam0, Long.valueOf(uid)});
                            }
                            this.mOrganizersByFeatureIds.entrySet().removeIf(new Predicate() { // from class: com.android.server.wm.DisplayAreaOrganizerController$$ExternalSyntheticLambda3
                                @Override // java.util.function.Predicate
                                public final boolean test(Object obj) {
                                    return DisplayAreaOrganizerController.lambda$unregisterOrganizer$2(organizer, (Map.Entry) obj);
                                }
                            });
                            WindowManagerService.resetPriorityAfterLockedSection();
                            Binder.restoreCallingIdentity(origId);
                        } catch (Throwable th) {
                            th = th;
                            WindowManagerService.resetPriorityAfterLockedSection();
                            throw th;
                        }
                    }
                } catch (Throwable th2) {
                    th = th2;
                    Binder.restoreCallingIdentity(origId);
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
            }
        } catch (Throwable th4) {
            th = th4;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$unregisterOrganizer$2(IDisplayAreaOrganizer organizer, Map.Entry entry) {
        boolean matches = ((DisplayAreaOrganizerState) entry.getValue()).mOrganizer.asBinder().equals(organizer.asBinder());
        if (matches) {
            ((DisplayAreaOrganizerState) entry.getValue()).destroy();
        }
        return matches;
    }

    public DisplayAreaAppearedInfo createTaskDisplayArea(IDisplayAreaOrganizer organizer, int displayId, final int parentFeatureId, String name) {
        TaskDisplayArea parentTda;
        TaskDisplayArea tda;
        enforceTaskPermission("createTaskDisplayArea()");
        long uid = Binder.getCallingUid();
        long origId = Binder.clearCallingIdentity();
        try {
            try {
                synchronized (this.mGlobalLock) {
                    try {
                        WindowManagerService.boostPriorityForLockedSection();
                        if (ProtoLogCache.WM_DEBUG_WINDOW_ORGANIZER_enabled) {
                            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_WINDOW_ORGANIZER, -948446688, 1, (String) null, new Object[]{Long.valueOf(uid)});
                        }
                        DisplayContent display = this.mService.mRootWindowContainer.getDisplayContent(displayId);
                        if (display != null) {
                            if (display.isTrusted()) {
                                RootDisplayArea parentRoot = (RootDisplayArea) display.getItemFromDisplayAreas(new Function() { // from class: com.android.server.wm.DisplayAreaOrganizerController$$ExternalSyntheticLambda0
                                    @Override // java.util.function.Function
                                    public final Object apply(Object obj) {
                                        return DisplayAreaOrganizerController.lambda$createTaskDisplayArea$3(parentFeatureId, (DisplayArea) obj);
                                    }
                                });
                                if (parentRoot == null) {
                                    parentTda = (TaskDisplayArea) display.getItemFromTaskDisplayAreas(new Function() { // from class: com.android.server.wm.DisplayAreaOrganizerController$$ExternalSyntheticLambda1
                                        @Override // java.util.function.Function
                                        public final Object apply(Object obj) {
                                            return DisplayAreaOrganizerController.lambda$createTaskDisplayArea$4(parentFeatureId, (TaskDisplayArea) obj);
                                        }
                                    });
                                } else {
                                    parentTda = null;
                                }
                                if (parentRoot == null && parentTda == null) {
                                    throw new IllegalArgumentException("Can't find a parent DisplayArea with featureId=" + parentFeatureId);
                                }
                                int taskDisplayAreaFeatureId = this.mNextTaskDisplayAreaFeatureId;
                                this.mNextTaskDisplayAreaFeatureId = taskDisplayAreaFeatureId + 1;
                                DisplayAreaOrganizerState state = new DisplayAreaOrganizerState(organizer, taskDisplayAreaFeatureId);
                                if (parentRoot != null) {
                                    tda = createTaskDisplayArea(parentRoot, name, taskDisplayAreaFeatureId);
                                } else {
                                    tda = createTaskDisplayArea(parentTda, name, taskDisplayAreaFeatureId);
                                }
                                DisplayAreaAppearedInfo tdaInfo = organizeDisplayArea(organizer, tda, "DisplayAreaOrganizerController.createTaskDisplayArea");
                                this.mOrganizersByFeatureIds.put(Integer.valueOf(taskDisplayAreaFeatureId), state);
                                WindowManagerService.resetPriorityAfterLockedSection();
                                Binder.restoreCallingIdentity(origId);
                                return tdaInfo;
                            }
                            throw new IllegalArgumentException("createTaskDisplayArea untrusted displayId=" + displayId);
                        }
                        throw new IllegalArgumentException("createTaskDisplayArea unknown displayId=" + displayId);
                    } catch (Throwable th) {
                        th = th;
                        try {
                            WindowManagerService.resetPriorityAfterLockedSection();
                            throw th;
                        } catch (Throwable th2) {
                            th = th2;
                            Binder.restoreCallingIdentity(origId);
                            throw th;
                        }
                    }
                }
            } catch (Throwable th3) {
                th = th3;
            }
        } catch (Throwable th4) {
            th = th4;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ RootDisplayArea lambda$createTaskDisplayArea$3(int parentFeatureId, DisplayArea da) {
        if (da.asRootDisplayArea() != null && da.mFeatureId == parentFeatureId) {
            return da.asRootDisplayArea();
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ TaskDisplayArea lambda$createTaskDisplayArea$4(int parentFeatureId, TaskDisplayArea taskDisplayArea) {
        if (taskDisplayArea.mFeatureId == parentFeatureId) {
            return taskDisplayArea;
        }
        return null;
    }

    public void deleteTaskDisplayArea(WindowContainerToken token) {
        enforceTaskPermission("deleteTaskDisplayArea()");
        long uid = Binder.getCallingUid();
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                WindowManagerService.boostPriorityForLockedSection();
                if (ProtoLogCache.WM_DEBUG_WINDOW_ORGANIZER_enabled) {
                    ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_WINDOW_ORGANIZER, -597091183, 1, (String) null, new Object[]{Long.valueOf(uid)});
                }
                WindowContainer wc = WindowContainer.fromBinder(token.asBinder());
                if (wc == null || wc.asTaskDisplayArea() == null) {
                    throw new IllegalArgumentException("Can't resolve TaskDisplayArea from token");
                }
                TaskDisplayArea taskDisplayArea = wc.asTaskDisplayArea();
                if (!taskDisplayArea.mCreatedByOrganizer) {
                    throw new IllegalArgumentException("Attempt to delete TaskDisplayArea not created by organizer TaskDisplayArea=" + taskDisplayArea);
                }
                this.mService.deferWindowLayout();
                this.mOrganizersByFeatureIds.remove(Integer.valueOf(taskDisplayArea.mFeatureId)).destroy();
                this.mService.continueWindowLayout();
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onDisplayAreaAppeared(IDisplayAreaOrganizer organizer, DisplayArea da) {
        if (ProtoLogCache.WM_DEBUG_WINDOW_ORGANIZER_enabled) {
            String protoLogParam0 = String.valueOf(da.getName());
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_WINDOW_ORGANIZER, -1980468143, 0, (String) null, new Object[]{protoLogParam0});
        }
        try {
            SurfaceControl outSurfaceControl = new SurfaceControl(da.getSurfaceControl(), "DisplayAreaOrganizerController.onDisplayAreaAppeared");
            organizer.onDisplayAreaAppeared(da.getDisplayAreaInfo(), outSurfaceControl);
        } catch (RemoteException e) {
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onDisplayAreaVanished(IDisplayAreaOrganizer organizer, DisplayArea da) {
        if (ProtoLogCache.WM_DEBUG_WINDOW_ORGANIZER_enabled) {
            String protoLogParam0 = String.valueOf(da.getName());
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_WINDOW_ORGANIZER, 487621047, 0, (String) null, new Object[]{protoLogParam0});
        }
        if (!organizer.asBinder().isBinderAlive()) {
            Slog.d(TAG, "Organizer died before sending onDisplayAreaVanished");
            return;
        }
        try {
            organizer.onDisplayAreaVanished(da.getDisplayAreaInfo());
        } catch (RemoteException e) {
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onDisplayAreaInfoChanged(IDisplayAreaOrganizer organizer, DisplayArea da) {
        if (ProtoLogCache.WM_DEBUG_WINDOW_ORGANIZER_enabled) {
            String protoLogParam0 = String.valueOf(da.getName());
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_WINDOW_ORGANIZER, 174572959, 0, (String) null, new Object[]{protoLogParam0});
        }
        if (checkOrganizerIsRegistering(organizer)) {
            return;
        }
        try {
            organizer.onDisplayAreaInfoChanged(da.getDisplayAreaInfo());
        } catch (RemoteException e) {
        }
    }

    private DisplayAreaAppearedInfo organizeDisplayArea(IDisplayAreaOrganizer organizer, DisplayArea displayArea, String callsite) {
        displayArea.setOrganizer(organizer, true);
        return new DisplayAreaAppearedInfo(displayArea.getDisplayAreaInfo(), new SurfaceControl(displayArea.getSurfaceControl(), callsite));
    }

    private TaskDisplayArea createTaskDisplayArea(final RootDisplayArea root, String name, int taskDisplayAreaFeatureId) {
        TaskDisplayArea taskDisplayArea = new TaskDisplayArea(root.mDisplayContent, root.mWmService, name, taskDisplayAreaFeatureId, true);
        DisplayArea topTaskContainer = (DisplayArea) root.getItemFromDisplayAreas(new Function() { // from class: com.android.server.wm.DisplayAreaOrganizerController$$ExternalSyntheticLambda5
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return DisplayAreaOrganizerController.lambda$createTaskDisplayArea$5(RootDisplayArea.this, (DisplayArea) obj);
            }
        });
        if (topTaskContainer == null) {
            throw new IllegalStateException("Root must either contain TDA or DAG root=" + root);
        }
        WindowContainer parent = topTaskContainer.getParent();
        int index = parent.mChildren.indexOf(topTaskContainer) + 1;
        parent.addChild(taskDisplayArea, index);
        return taskDisplayArea;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ DisplayArea lambda$createTaskDisplayArea$5(RootDisplayArea root, DisplayArea da) {
        if (da.mType != DisplayArea.Type.ANY) {
            return null;
        }
        RootDisplayArea rootDA = da.getRootDisplayArea();
        if (rootDA == root || rootDA == da) {
            return da;
        }
        return null;
    }

    private TaskDisplayArea createTaskDisplayArea(TaskDisplayArea parentTda, String name, int taskDisplayAreaFeatureId) {
        TaskDisplayArea taskDisplayArea = new TaskDisplayArea(parentTda.mDisplayContent, parentTda.mWmService, name, taskDisplayAreaFeatureId, true);
        parentTda.addChild(taskDisplayArea, Integer.MAX_VALUE);
        return taskDisplayArea;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void deleteTaskDisplayArea(TaskDisplayArea taskDisplayArea) {
        taskDisplayArea.setOrganizer(null);
        this.mService.mRootWindowContainer.mTaskSupervisor.beginDeferResume();
        try {
            Task lastReparentedRootTask = taskDisplayArea.remove();
            this.mService.mRootWindowContainer.mTaskSupervisor.endDeferResume();
            taskDisplayArea.removeImmediately();
            if (lastReparentedRootTask != null) {
                lastReparentedRootTask.resumeNextFocusAfterReparent();
            }
        } catch (Throwable th) {
            this.mService.mRootWindowContainer.mTaskSupervisor.endDeferResume();
            throw th;
        }
    }

    public DisplayAreaAppearedInfo createMultiWindowTaskDisplayAreaV3(IDisplayAreaOrganizer organizer, int displayId, final int parentFeatureId, String name, int multiWindowState) {
        TaskDisplayArea parentTda;
        TaskDisplayArea tda;
        enforceTaskPermission("createTaskDisplayArea()");
        long uid = Binder.getCallingUid();
        long origId = Binder.clearCallingIdentity();
        try {
            try {
                synchronized (this.mGlobalLock) {
                    try {
                        WindowManagerService.boostPriorityForLockedSection();
                        if (ProtoLogCache.WM_DEBUG_WINDOW_ORGANIZER_enabled) {
                            try {
                            } catch (Throwable th) {
                                th = th;
                            }
                            try {
                                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_WINDOW_ORGANIZER, -948446688, 1, (String) null, new Object[]{Long.valueOf(uid)});
                            } catch (Throwable th2) {
                                th = th2;
                                try {
                                    WindowManagerService.resetPriorityAfterLockedSection();
                                    throw th;
                                } catch (Throwable th3) {
                                    th = th3;
                                    Binder.restoreCallingIdentity(origId);
                                    throw th;
                                }
                            }
                        }
                        DisplayContent display = this.mService.mRootWindowContainer.getDisplayContent(displayId);
                        if (display != null) {
                            if (display.isTrusted()) {
                                RootDisplayArea parentRoot = (RootDisplayArea) display.getItemFromDisplayAreas(new Function() { // from class: com.android.server.wm.DisplayAreaOrganizerController$$ExternalSyntheticLambda6
                                    @Override // java.util.function.Function
                                    public final Object apply(Object obj) {
                                        return DisplayAreaOrganizerController.lambda$createMultiWindowTaskDisplayAreaV3$6(parentFeatureId, (DisplayArea) obj);
                                    }
                                });
                                if (parentRoot == null) {
                                    parentTda = (TaskDisplayArea) display.getItemFromTaskDisplayAreas(new Function() { // from class: com.android.server.wm.DisplayAreaOrganizerController$$ExternalSyntheticLambda7
                                        @Override // java.util.function.Function
                                        public final Object apply(Object obj) {
                                            return DisplayAreaOrganizerController.lambda$createMultiWindowTaskDisplayAreaV3$7(parentFeatureId, (TaskDisplayArea) obj);
                                        }
                                    });
                                } else {
                                    parentTda = null;
                                }
                                if (parentRoot == null && parentTda == null) {
                                    throw new IllegalArgumentException("Can't find a parent DisplayArea with featureId=" + parentFeatureId);
                                }
                                int taskDisplayAreaFeatureId = this.mNextTaskDisplayAreaFeatureId;
                                this.mNextTaskDisplayAreaFeatureId = taskDisplayAreaFeatureId + 1;
                                DisplayAreaOrganizerState state = new DisplayAreaOrganizerState(organizer, taskDisplayAreaFeatureId);
                                if (parentRoot != null) {
                                    tda = createTaskDisplayArea(parentRoot, name, taskDisplayAreaFeatureId);
                                } else {
                                    tda = createTaskDisplayArea(parentTda, name, taskDisplayAreaFeatureId);
                                }
                                DisplayAreaAppearedInfo tdaInfo = organizeDisplayArea(organizer, tda, "DisplayAreaOrganizerController.createTaskDisplayArea");
                                this.mOrganizersByFeatureIds.put(Integer.valueOf(taskDisplayAreaFeatureId), state);
                                ITranDisplayArea.Instance().setMultiWindow(tda, true, tdaInfo, multiWindowState);
                                SurfaceControl.Transaction ta = tda.mWmService.mTransactionFactory.get();
                                ta.setTrustedOverlay(tda.getSurfaceControl(), true);
                                ta.setMultiWindowLayer(tda.getSurfaceControl(), true);
                                ta.apply();
                                WindowManagerService.resetPriorityAfterLockedSection();
                                Binder.restoreCallingIdentity(origId);
                                return tdaInfo;
                            }
                            throw new IllegalArgumentException("createTaskDisplayArea untrusted displayId=" + displayId);
                        }
                        throw new IllegalArgumentException("createTaskDisplayArea unknown displayId=" + displayId);
                    } catch (Throwable th4) {
                        th = th4;
                    }
                }
            } catch (Throwable th5) {
                th = th5;
            }
        } catch (Throwable th6) {
            th = th6;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ RootDisplayArea lambda$createMultiWindowTaskDisplayAreaV3$6(int parentFeatureId, DisplayArea da) {
        if (da.asRootDisplayArea() != null && da.mFeatureId == parentFeatureId) {
            return da.asRootDisplayArea();
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ TaskDisplayArea lambda$createMultiWindowTaskDisplayAreaV3$7(int parentFeatureId, TaskDisplayArea taskDisplayArea) {
        if (taskDisplayArea.mFeatureId == parentFeatureId) {
            return taskDisplayArea;
        }
        return null;
    }

    public DisplayAreaAppearedInfo createMultiWindowTaskDisplayArea(IDisplayAreaOrganizer organizer, int displayId, int parentFeatureId, String name) {
        DisplayAreaAppearedInfo tdaInfo = createTaskDisplayArea(organizer, displayId, parentFeatureId, name);
        if (tdaInfo != null) {
            int displayAreaId = this.mService.mWindowManager.getNextTranMultiDisplayAreaId();
            tdaInfo.setTranMultiDisplayAreaId(displayAreaId);
        }
        return tdaInfo;
    }

    public void unregisterImeOrganizer(IDisplayAreaOrganizer organizer) {
        unregisterOrganizer(organizer);
        this.mFromMultiwindow = true;
    }

    boolean checkOrganizerIsRegistering(IDisplayAreaOrganizer organizer) {
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                for (Integer key : this.mOrganizersByFeatureIds.keySet()) {
                    if (this.mOrganizersByFeatureIds.get(key).mOrganizer == organizer) {
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return true;
                    }
                }
                Slog.d(TAG, "FIXME: Can't find " + organizer + "");
                WindowManagerService.resetPriorityAfterLockedSection();
                return false;
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }
}
