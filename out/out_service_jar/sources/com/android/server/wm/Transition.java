package com.android.server.wm;

import android.app.ActivityManager;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IRemoteCallback;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.Trace;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Slog;
import android.util.SparseArray;
import android.view.SurfaceControl;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.window.RemoteTransition;
import android.window.TransitionInfo;
import com.android.internal.graphics.ColorUtils;
import com.android.internal.protolog.ProtoLogGroup;
import com.android.internal.protolog.ProtoLogImpl;
import com.android.internal.util.function.pooled.PooledLambda;
import com.android.server.LocalServices;
import com.android.server.inputmethod.InputMethodManagerInternal;
import com.android.server.wm.BLASTSyncEngine;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class Transition extends Binder implements BLASTSyncEngine.TransactionReadyListener {
    private static final String DEFAULT_PACKAGE = "android";
    private static final int STATE_ABORT = 3;
    private static final int STATE_COLLECTING = 0;
    private static final int STATE_PENDING = -1;
    private static final int STATE_PLAYING = 2;
    private static final int STATE_STARTED = 1;
    private static final String TAG = "Transition";
    private static final String TRACE_NAME_PLAY_TRANSITION = "PlayTransition";
    private final TransitionController mController;
    private int mFlags;
    private TransitionInfo.AnimationOptions mOverrideOptions;
    private final BLASTSyncEngine mSyncEngine;
    private ArrayList<WindowContainer> mTargets;
    final int mType;
    private int mSyncId = -1;
    private RemoteTransition mRemoteTransition = null;
    private SurfaceControl.Transaction mStartTransaction = null;
    private SurfaceControl.Transaction mFinishTransaction = null;
    final ArrayMap<WindowContainer, ChangeInfo> mChanges = new ArrayMap<>();
    final ArraySet<WindowContainer> mParticipants = new ArraySet<>();
    private final ArrayList<DisplayContent> mTargetDisplays = new ArrayList<>();
    private final ArraySet<WindowToken> mVisibleAtTransitionEndTokens = new ArraySet<>();
    private ArrayMap<ActivityRecord, Task> mTransientLaunches = null;
    private IRemoteCallback mClientAnimationStartCallback = null;
    private IRemoteCallback mClientAnimationFinishCallback = null;
    private int mState = -1;
    private final ReadyTracker mReadyTracker = new ReadyTracker();
    private boolean mNavBarAttachedToApp = false;
    private int mRecentsDisplayId = -1;
    private boolean mCanPipOnFinish = true;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    @interface TransitionState {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Transition(int type, int flags, TransitionController controller, BLASTSyncEngine syncEngine) {
        this.mType = type;
        this.mFlags = flags;
        this.mController = controller;
        this.mSyncEngine = syncEngine;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void addFlag(int flag) {
        this.mFlags |= flag;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setTransientLaunch(ActivityRecord activity, Task restoreBelow) {
        if (this.mTransientLaunches == null) {
            this.mTransientLaunches = new ArrayMap<>();
        }
        this.mTransientLaunches.put(activity, restoreBelow);
        if (ProtoLogCache.WM_DEBUG_WINDOW_TRANSITIONS_enabled) {
            long protoLogParam0 = this.mSyncId;
            String protoLogParam1 = String.valueOf(activity);
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_WINDOW_TRANSITIONS, -779535710, 1, "Transition %d: Set %s as transient-launch", new Object[]{Long.valueOf(protoLogParam0), protoLogParam1});
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isTransientLaunch(ActivityRecord activity) {
        ArrayMap<ActivityRecord, Task> arrayMap = this.mTransientLaunches;
        return arrayMap != null && arrayMap.containsKey(activity);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Task getTransientLaunchRestoreTarget(WindowContainer container) {
        if (this.mTransientLaunches == null) {
            return null;
        }
        for (int i = 0; i < this.mTransientLaunches.size(); i++) {
            if (this.mTransientLaunches.keyAt(i).isDescendantOf(container)) {
                return this.mTransientLaunches.valueAt(i);
            }
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isOnDisplay(DisplayContent dc) {
        return this.mTargetDisplays.contains(dc);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setSeamlessRotation(WindowContainer wc) {
        ChangeInfo info = this.mChanges.get(wc);
        if (info == null) {
            return;
        }
        info.mFlags |= 1;
    }

    int getSyncId() {
        return this.mSyncId;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getFlags() {
        return this.mFlags;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void startCollecting(long timeoutMs) {
        if (this.mState != -1) {
            throw new IllegalStateException("Attempting to re-use a transition");
        }
        this.mState = 0;
        this.mSyncId = this.mSyncEngine.startSyncSet(this, timeoutMs, TAG);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void start() {
        int i = this.mState;
        if (i < 0) {
            throw new IllegalStateException("Can't start Transition which isn't collecting.");
        }
        if (i >= 1) {
            Slog.w(TAG, "Transition already started: " + this.mSyncId);
        }
        this.mState = 1;
        if (ProtoLogCache.WM_DEBUG_WINDOW_TRANSITIONS_enabled) {
            long protoLogParam0 = this.mSyncId;
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_WINDOW_TRANSITIONS, 996960396, 1, "Starting Transition %d", new Object[]{Long.valueOf(protoLogParam0)});
        }
        applyReady();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void collect(WindowContainer wc) {
        WindowState wallpaper;
        if (this.mState < 0) {
            throw new IllegalStateException("Transition hasn't started collecting.");
        }
        if (this.mSyncId < 0) {
            return;
        }
        if (ProtoLogCache.WM_DEBUG_WINDOW_TRANSITIONS_enabled) {
            long protoLogParam0 = this.mSyncId;
            String protoLogParam1 = String.valueOf(wc);
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_WINDOW_TRANSITIONS, -1567866547, 1, "Collecting in transition %d: %s", new Object[]{Long.valueOf(protoLogParam0), protoLogParam1});
        }
        for (WindowContainer curr = wc.getParent(); curr != null && !this.mChanges.containsKey(curr); curr = curr.getParent()) {
            this.mChanges.put(curr, new ChangeInfo(curr));
            if (isReadyGroup(curr)) {
                this.mReadyTracker.addGroup(curr);
                if (ProtoLogCache.WM_DEBUG_WINDOW_TRANSITIONS_enabled) {
                    long protoLogParam02 = this.mSyncId;
                    String protoLogParam12 = String.valueOf(curr);
                    ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_WINDOW_TRANSITIONS, -1442613680, 1, " Creating Ready-group for Transition %d with root=%s", new Object[]{Long.valueOf(protoLogParam02), protoLogParam12});
                }
            }
        }
        if (this.mParticipants.contains(wc)) {
            return;
        }
        this.mSyncEngine.addToSyncSet(this.mSyncId, wc);
        ChangeInfo info = this.mChanges.get(wc);
        if (info == null) {
            info = new ChangeInfo(wc);
            this.mChanges.put(wc, info);
        }
        this.mParticipants.add(wc);
        if (wc.getDisplayContent() != null && !this.mTargetDisplays.contains(wc.getDisplayContent())) {
            this.mTargetDisplays.add(wc.getDisplayContent());
        }
        if (info.mShowWallpaper && (wallpaper = wc.getDisplayContent().mWallpaperController.getTopVisibleWallpaper()) != null) {
            collect(wallpaper.mToken);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void collectExistenceChange(WindowContainer wc) {
        if (this.mSyncId < 0) {
            return;
        }
        if (ProtoLogCache.WM_DEBUG_WINDOW_TRANSITIONS_enabled) {
            long protoLogParam0 = this.mSyncId;
            String protoLogParam1 = String.valueOf(wc);
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_WINDOW_TRANSITIONS, -354571697, 1, "Existence Changed in transition %d: %s", new Object[]{Long.valueOf(protoLogParam0), protoLogParam1});
        }
        collect(wc);
        this.mChanges.get(wc).mExistenceChanged = true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setKnownConfigChanges(WindowContainer<?> wc, int changes) {
        ChangeInfo changeInfo = this.mChanges.get(wc);
        if (changeInfo != null) {
            changeInfo.mKnownConfigChanges = changes;
        }
    }

    private void sendRemoteCallback(IRemoteCallback callback) {
        if (callback == null) {
            return;
        }
        this.mController.mAtm.mH.sendMessage(PooledLambda.obtainMessage(new Consumer() { // from class: com.android.server.wm.Transition$$ExternalSyntheticLambda0
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((IRemoteCallback) obj).sendResult((Bundle) null);
            }
        }, callback));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setOverrideAnimation(TransitionInfo.AnimationOptions options, IRemoteCallback startCallback, IRemoteCallback finishCallback) {
        if (this.mSyncId < 0) {
            return;
        }
        this.mOverrideOptions = options;
        sendRemoteCallback(this.mClientAnimationStartCallback);
        this.mClientAnimationStartCallback = startCallback;
        this.mClientAnimationFinishCallback = finishCallback;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setReady(WindowContainer wc, boolean ready) {
        if (this.mSyncId < 0) {
            return;
        }
        this.mReadyTracker.setReadyFrom(wc, ready);
        applyReady();
    }

    private void applyReady() {
        if (this.mState < 1) {
            return;
        }
        boolean ready = this.mReadyTracker.allReady();
        if (ProtoLogCache.WM_DEBUG_WINDOW_TRANSITIONS_enabled) {
            long protoLogParam1 = this.mSyncId;
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_WINDOW_TRANSITIONS, -874888131, 7, "Set transition ready=%b %d", new Object[]{Boolean.valueOf(ready), Long.valueOf(protoLogParam1)});
        }
        this.mSyncEngine.setReady(this.mSyncId, ready);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setAllReady() {
        if (this.mSyncId < 0) {
            return;
        }
        this.mReadyTracker.setAllReady();
        applyReady();
    }

    boolean allReady() {
        return this.mReadyTracker.allReady();
    }

    private void buildFinishTransaction(SurfaceControl.Transaction t, SurfaceControl rootLeash) {
        Rect clipRect;
        Point tmpPos = new Point();
        ArraySet<DisplayContent> displays = new ArraySet<>();
        for (int i = this.mTargets.size() - 1; i >= 0; i--) {
            WindowContainer target = this.mTargets.get(i);
            if (target.getParent() != null) {
                SurfaceControl targetLeash = getLeashSurface(target);
                SurfaceControl origParent = getOrigParentSurface(target);
                t.reparent(targetLeash, origParent);
                t.setLayer(targetLeash, target.getLastLayer());
                target.getRelativePosition(tmpPos);
                t.setPosition(targetLeash, tmpPos.x, tmpPos.y);
                if (target.asDisplayContent() != null) {
                    clipRect = null;
                } else if (target.asActivityRecord() != null) {
                    Rect clipRect2 = target.getParent().getRequestedOverrideBounds();
                    clipRect2.offset(-tmpPos.x, -tmpPos.y);
                    clipRect = clipRect2;
                } else {
                    Rect clipRect3 = target.getRequestedOverrideBounds();
                    clipRect3.offset(-tmpPos.x, -tmpPos.y);
                    clipRect = clipRect3;
                }
                t.setCrop(targetLeash, clipRect);
                t.setCornerRadius(targetLeash, 0.0f);
                t.setShadowRadius(targetLeash, 0.0f);
                t.setMatrix(targetLeash, 1.0f, 0.0f, 0.0f, 1.0f);
                if (target.isOrganized() && target.matchParentBounds()) {
                    t.setWindowCrop(targetLeash, -1, -1);
                }
                displays.add(target.getDisplayContent());
            }
        }
        int i2 = displays.size();
        for (int i3 = i2 - 1; i3 >= 0; i3--) {
            if (displays.valueAt(i3) != null) {
                displays.valueAt(i3).assignChildLayers(t);
            }
        }
        if (rootLeash.isValid()) {
            t.reparent(rootLeash, null);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setCanPipOnFinish(boolean canPipOnFinish) {
        this.mCanPipOnFinish = canPipOnFinish;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void finishTransition() {
        if (Trace.isTagEnabled(32L)) {
            Trace.asyncTraceEnd(32L, TRACE_NAME_PLAY_TRANSITION, System.identityHashCode(this));
        }
        this.mFinishTransaction = null;
        this.mStartTransaction = null;
        if (this.mState < 2) {
            throw new IllegalStateException("Can't finish a non-playing transition " + this.mSyncId);
        }
        boolean activitiesWentInvisible = false;
        for (int i = 0; i < this.mParticipants.size(); i++) {
            ActivityRecord ar = this.mParticipants.valueAt(i).asActivityRecord();
            if (ar != null) {
                boolean visibleAtTransitionEnd = this.mVisibleAtTransitionEndTokens.contains(ar);
                if (!visibleAtTransitionEnd && !ar.isVisibleRequested()) {
                    boolean commitVisibility = true;
                    if (this.mCanPipOnFinish && ar.isVisible() && ar.getTask() != null) {
                        if (ar.pictureInPictureArgs != null && ar.pictureInPictureArgs.isAutoEnterEnabled()) {
                            if (this.mTransientLaunches != null) {
                                int j = 0;
                                while (true) {
                                    if (j < this.mTransientLaunches.size()) {
                                        if (!this.mTransientLaunches.keyAt(j).isVisibleRequested()) {
                                            j++;
                                        } else {
                                            ar.supportsEnterPipOnTaskSwitch = true;
                                            break;
                                        }
                                    } else {
                                        break;
                                    }
                                }
                            }
                            this.mController.mAtm.enterPictureInPictureMode(ar, ar.pictureInPictureArgs);
                            commitVisibility = false;
                        } else if (ar.getDeferHidingClient()) {
                            this.mController.mAtm.mTaskSupervisor.mUserLeaving = true;
                            ar.getTaskFragment().startPausing(false, null, "finishTransition");
                            this.mController.mAtm.mTaskSupervisor.mUserLeaving = false;
                        }
                    }
                    if (commitVisibility) {
                        if (ProtoLogCache.WM_DEBUG_WINDOW_TRANSITIONS_enabled) {
                            String protoLogParam0 = String.valueOf(ar);
                            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_WINDOW_TRANSITIONS, -532081937, 0, "  Commit activity becoming invisible: %s", new Object[]{protoLogParam0});
                        }
                        Task task = ar.getTask();
                        if (task != null && !task.isVisibleRequested() && this.mTransientLaunches != null) {
                            this.mController.mTaskSnapshotController.recordTaskSnapshot(task, false);
                        }
                        ar.commitVisibility(false, false, true);
                        activitiesWentInvisible = true;
                    }
                }
                if (this.mChanges.get(ar).mVisible != visibleAtTransitionEnd) {
                    ar.mEnteringAnimation = visibleAtTransitionEnd;
                }
            }
            WallpaperWindowToken wt = this.mParticipants.valueAt(i).asWallpaperToken();
            if (wt != null && !this.mVisibleAtTransitionEndTokens.contains(wt) && !wt.isVisibleRequested()) {
                if (ProtoLogCache.WM_DEBUG_WINDOW_TRANSITIONS_enabled) {
                    String protoLogParam02 = String.valueOf(wt);
                    ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_WINDOW_TRANSITIONS, 691515534, 0, "  Commit wallpaper becoming invisible: %s", new Object[]{protoLogParam02});
                }
                wt.commitVisibility(false);
            }
        }
        for (int i2 = 0; i2 < this.mParticipants.size(); i2++) {
            ActivityRecord ar2 = this.mParticipants.valueAt(i2).asActivityRecord();
            if (ar2 != null) {
                this.mController.dispatchLegacyAppTransitionFinished(ar2);
            }
        }
        if (activitiesWentInvisible) {
            this.mController.mAtm.mTaskSupervisor.scheduleProcessStoppingAndFinishingActivitiesIfNeeded();
        }
        sendRemoteCallback(this.mClientAnimationFinishCallback);
        legacyRestoreNavigationBarFromApp();
        if (this.mRecentsDisplayId != -1) {
            this.mController.mAtm.mRootWindowContainer.getDisplayContent(this.mRecentsDisplayId).getInputMonitor().setActiveRecents(null, null);
        }
        for (int i3 = 0; i3 < this.mTargetDisplays.size(); i3++) {
            DisplayContent dc = this.mTargetDisplays.get(i3);
            AsyncRotationController asyncRotationController = dc.getAsyncRotationController();
            if (asyncRotationController != null && this.mTargets.contains(dc)) {
                asyncRotationController.onTransitionFinished();
            }
            if (this.mTransientLaunches != null) {
                InsetsControlTarget prevImeTarget = dc.getImeTarget(2);
                InsetsControlTarget newImeTarget = null;
                int t = 0;
                while (true) {
                    if (t < this.mTransientLaunches.size()) {
                        if (this.mTransientLaunches.keyAt(t).getDisplayContent() != dc) {
                            t++;
                        } else {
                            newImeTarget = dc.computeImeTarget(true);
                            break;
                        }
                    } else {
                        break;
                    }
                }
                int t2 = this.mRecentsDisplayId;
                if (t2 != -1 && prevImeTarget == newImeTarget) {
                    InputMethodManagerInternal.get().updateImeWindowStatus(false);
                }
            }
            dc.removeImeSurfaceImmediately();
            dc.handleCompleteDeferredRemoval();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void abort() {
        int i = this.mState;
        if (i == 3) {
            return;
        }
        if (i != 0) {
            throw new IllegalStateException("Too late to abort.");
        }
        if (ProtoLogCache.WM_DEBUG_WINDOW_TRANSITIONS_enabled) {
            long protoLogParam0 = this.mSyncId;
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_WINDOW_TRANSITIONS, -863438038, 1, "Aborting Transition: %d", new Object[]{Long.valueOf(protoLogParam0)});
        }
        this.mState = 3;
        this.mSyncEngine.abort(this.mSyncId);
        this.mController.dispatchLegacyAppTransitionCancelled();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setRemoteTransition(RemoteTransition remoteTransition) {
        this.mRemoteTransition = remoteTransition;
    }

    RemoteTransition getRemoteTransition() {
        return this.mRemoteTransition;
    }

    @Override // com.android.server.wm.BLASTSyncEngine.TransactionReadyListener
    public void onTransactionReady(int syncId, SurfaceControl.Transaction transaction) {
        if (syncId != this.mSyncId) {
            Slog.e(TAG, "Unexpected Sync ID " + syncId + ". Expected " + this.mSyncId);
            return;
        }
        if (this.mTargetDisplays.isEmpty()) {
            this.mTargetDisplays.add(this.mController.mAtm.mRootWindowContainer.getDefaultDisplay());
        }
        DisplayContent dc = this.mTargetDisplays.get(0);
        if (this.mState == 3) {
            this.mController.abort(this);
            dc.getPendingTransaction().merge(transaction);
            this.mSyncId = -1;
            this.mOverrideOptions = null;
            return;
        }
        for (int i = this.mParticipants.size() - 1; i >= 0; i--) {
            WindowContainer<?> wc = this.mParticipants.valueAt(i);
            if (isWallpaper(wc) && wc.getDisplayContent() != null) {
                wc.getDisplayContent().mWallpaperController.adjustWallpaperWindows();
            }
        }
        this.mState = 2;
        this.mController.moveToPlaying(this);
        if (dc.isKeyguardLocked()) {
            this.mFlags |= 64;
        }
        ArrayList<WindowContainer> calculateTargets = calculateTargets(this.mParticipants, this.mChanges);
        this.mTargets = calculateTargets;
        TransitionInfo info = calculateTransitionInfo(this.mType, this.mFlags, calculateTargets, this.mChanges);
        TransitionInfo.AnimationOptions animationOptions = this.mOverrideOptions;
        if (animationOptions != null) {
            info.setAnimationOptions(animationOptions);
        }
        handleLegacyRecentsStartBehavior(dc, info);
        handleNonAppWindowsInTransition(dc, this.mType, this.mFlags);
        reportStartReasonsToLogger();
        sendRemoteCallback(this.mClientAnimationStartCallback);
        for (int i2 = this.mParticipants.size() - 1; i2 >= 0; i2--) {
            ActivityRecord ar = this.mParticipants.valueAt(i2).asActivityRecord();
            if (ar != null && ar.mVisibleRequested) {
                transaction.show(ar.getSurfaceControl());
                for (WindowContainer p = ar.getParent(); p != null && !this.mTargets.contains(p); p = p.getParent()) {
                    if (p.getSurfaceControl() != null) {
                        transaction.show(p.getSurfaceControl());
                    }
                }
            }
        }
        for (int i3 = this.mParticipants.size() - 1; i3 >= 0; i3--) {
            WindowContainer wc2 = this.mParticipants.valueAt(i3);
            if (wc2.asWindowToken() != null && wc2.isVisibleRequested()) {
                this.mVisibleAtTransitionEndTokens.add(wc2.asWindowToken());
            }
        }
        if (this.mTransientLaunches == null) {
            for (int i4 = this.mParticipants.size() - 1; i4 >= 0; i4--) {
                ActivityRecord ar2 = this.mParticipants.valueAt(i4).asActivityRecord();
                if (ar2 != null && !ar2.isVisibleRequested() && ar2.getTask() != null && !ar2.getTask().isVisibleRequested()) {
                    this.mController.mTaskSnapshotController.recordTaskSnapshot(ar2.getTask(), false);
                }
            }
        }
        AsyncRotationController controller = dc.getAsyncRotationController();
        if (controller != null && this.mTargets.contains(dc)) {
            controller.setupStartTransaction(transaction);
        }
        this.mStartTransaction = transaction;
        SurfaceControl.Transaction transaction2 = this.mController.mAtm.mWindowManager.mTransactionFactory.get();
        this.mFinishTransaction = transaction2;
        buildFinishTransaction(transaction2, info.getRootLeash());
        if (this.mController.getTransitionPlayer() != null) {
            this.mController.dispatchLegacyAppTransitionStarting(info);
            try {
                if (ProtoLogCache.WM_DEBUG_WINDOW_TRANSITIONS_enabled) {
                    String protoLogParam0 = String.valueOf(info);
                    ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_WINDOW_TRANSITIONS, 1115248873, 0, "Calling onTransitionReady: %s", new Object[]{protoLogParam0});
                }
                this.mController.getTransitionPlayer().onTransitionReady(this, info, transaction, this.mFinishTransaction);
                if (Trace.isTagEnabled(32L)) {
                    Trace.asyncTraceBegin(32L, TRACE_NAME_PLAY_TRANSITION, System.identityHashCode(this));
                }
            } catch (RemoteException e) {
                cleanUpOnFailure();
            }
        } else {
            cleanUpOnFailure();
        }
        this.mSyncId = -1;
        this.mOverrideOptions = null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void cleanUpOnFailure() {
        if (this.mState < 2) {
            return;
        }
        SurfaceControl.Transaction transaction = this.mStartTransaction;
        if (transaction != null) {
            transaction.apply();
        }
        SurfaceControl.Transaction transaction2 = this.mFinishTransaction;
        if (transaction2 != null) {
            transaction2.apply();
        }
        this.mController.finishTransition(this);
    }

    private void handleLegacyRecentsStartBehavior(DisplayContent dc, TransitionInfo info) {
        WindowState navWindow;
        Task task;
        if ((this.mFlags & 128) == 0) {
            return;
        }
        this.mRecentsDisplayId = dc.mDisplayId;
        InputConsumerImpl recentsAnimationInputConsumer = dc.getInputMonitor().getInputConsumer("recents_animation_input_consumer");
        if (recentsAnimationInputConsumer != null) {
            ActivityRecord recentsActivity = null;
            ActivityRecord topActivity = null;
            for (int i = 0; i < info.getChanges().size(); i++) {
                TransitionInfo.Change change = (TransitionInfo.Change) info.getChanges().get(i);
                if (change.getTaskInfo() != null && (task = Task.fromWindowContainerToken(((TransitionInfo.Change) info.getChanges().get(i)).getTaskInfo().token)) != null) {
                    int activityType = change.getTaskInfo().topActivityType;
                    boolean isRecents = activityType == 2 || activityType == 3;
                    if (isRecents && recentsActivity == null) {
                        recentsActivity = task.getTopVisibleActivity();
                    } else if (!isRecents && topActivity == null) {
                        topActivity = task.getTopNonFinishingActivity();
                    }
                }
            }
            if (recentsActivity != null && topActivity != null) {
                recentsAnimationInputConsumer.mWindowHandle.touchableRegion.set(topActivity.getBounds());
                dc.getInputMonitor().setActiveRecents(recentsActivity, topActivity);
            }
        }
        if (!this.mTargetDisplays.get(this.mRecentsDisplayId).isImeAttachedToApp()) {
            InputMethodManagerInternal inputMethodManagerInternal = (InputMethodManagerInternal) LocalServices.getService(InputMethodManagerInternal.class);
            if (inputMethodManagerInternal != null) {
                inputMethodManagerInternal.hideCurrentInputMethod(18);
            }
        } else {
            InputMethodManagerInternal.get().updateImeWindowStatus(true);
        }
        if (!dc.getDisplayPolicy().shouldAttachNavBarToAppDuringTransition() || dc.getAsyncRotationController() != null) {
            return;
        }
        WindowContainer topWC = null;
        for (int i2 = 0; i2 < info.getChanges().size(); i2++) {
            TransitionInfo.Change c = (TransitionInfo.Change) info.getChanges().get(i2);
            if (c.getTaskInfo() != null && c.getTaskInfo().displayId == this.mRecentsDisplayId && c.getTaskInfo().getActivityType() == 1 && (c.getMode() == 2 || c.getMode() == 4)) {
                topWC = WindowContainer.fromBinder(c.getContainer().asBinder());
                break;
            }
        }
        if (topWC == null || topWC.inMultiWindowMode() || (navWindow = dc.getDisplayPolicy().getNavigationBar()) == null || navWindow.mToken == null) {
            return;
        }
        this.mNavBarAttachedToApp = true;
        navWindow.mToken.cancelAnimation();
        SurfaceControl.Transaction t = navWindow.mToken.getPendingTransaction();
        SurfaceControl navSurfaceControl = navWindow.mToken.getSurfaceControl();
        t.reparent(navSurfaceControl, topWC.getSurfaceControl());
        t.show(navSurfaceControl);
        WindowContainer imeContainer = dc.getImeContainer();
        if (imeContainer.isVisible()) {
            t.setRelativeLayer(navSurfaceControl, imeContainer.getSurfaceControl(), 1);
        } else {
            t.setLayer(navSurfaceControl, Integer.MAX_VALUE);
        }
        if (this.mController.mStatusBar != null) {
            this.mController.mStatusBar.setNavigationBarLumaSamplingEnabled(this.mRecentsDisplayId, false);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void legacyRestoreNavigationBarFromApp() {
        if (this.mNavBarAttachedToApp) {
            this.mNavBarAttachedToApp = false;
            if (this.mRecentsDisplayId == -1) {
                Slog.e(TAG, "Reparented navigation bar without a valid display");
                this.mRecentsDisplayId = 0;
            }
            if (this.mController.mStatusBar != null) {
                this.mController.mStatusBar.setNavigationBarLumaSamplingEnabled(this.mRecentsDisplayId, true);
            }
            DisplayContent dc = this.mController.mAtm.mRootWindowContainer.getDisplayContent(this.mRecentsDisplayId);
            WindowState navWindow = dc.getDisplayPolicy().getNavigationBar();
            if (navWindow == null) {
                return;
            }
            navWindow.setSurfaceTranslationY(0);
            WindowToken navToken = navWindow.mToken;
            if (navToken == null) {
                return;
            }
            SurfaceControl.Transaction t = dc.getPendingTransaction();
            WindowContainer parent = navToken.getParent();
            t.setLayer(navToken.getSurfaceControl(), navToken.getLastLayer());
            boolean animate = false;
            int i = 0;
            while (true) {
                if (i < this.mTargets.size()) {
                    Task task = this.mTargets.get(i).asTask();
                    if (task == null || !task.isActivityTypeHomeOrRecents()) {
                        i++;
                    } else {
                        animate = task.isVisibleRequested();
                        break;
                    }
                } else {
                    break;
                }
            }
            if (animate) {
                NavBarFadeAnimationController controller = new NavBarFadeAnimationController(dc);
                controller.fadeWindowToken(true);
                return;
            }
            t.reparent(navToken.getSurfaceControl(), parent.getSurfaceControl());
        }
    }

    private void handleNonAppWindowsInTransition(DisplayContent dc, int transit, int flags) {
        if ((transit == 7 || (flags & 256) != 0) && !WindowManagerService.sEnableRemoteKeyguardGoingAwayAnimation) {
            if ((flags & 4) != 0 && (flags & 2) == 0 && (flags & 8) == 0) {
                Animation anim = this.mController.mAtm.mWindowManager.mPolicy.createKeyguardWallpaperExit((flags & 1) != 0);
                if (anim != null) {
                    anim.scaleCurrentDuration(this.mController.mAtm.mWindowManager.getTransitionAnimationScaleLocked());
                    dc.mWallpaperController.startWallpaperAnimation(anim);
                }
            }
            dc.startKeyguardExitOnNonAppWindows((flags & 4) != 0, (flags & 1) != 0, (flags & 8) != 0);
            if (!WindowManagerService.sEnableRemoteKeyguardGoingAwayAnimation) {
                this.mController.mAtm.mWindowManager.mPolicy.startKeyguardExitAnimation(SystemClock.uptimeMillis(), 0L);
            }
        }
        if ((flags & 64) != 0) {
            this.mController.mAtm.mWindowManager.mPolicy.applyKeyguardOcclusionChange(true);
        }
    }

    private void reportStartReasonsToLogger() {
        ArrayMap<WindowContainer, Integer> reasons = new ArrayMap<>();
        for (int i = this.mParticipants.size() - 1; i >= 0; i--) {
            ActivityRecord r = this.mParticipants.valueAt(i).asActivityRecord();
            if (r != null && r.mVisibleRequested) {
                reasons.put(r, Integer.valueOf((!(r.mStartingData instanceof SplashScreenStartingData) || r.mLastAllReadyAtSync) ? 2 : 1));
            }
        }
        this.mController.mAtm.mTaskSupervisor.getActivityMetricsLogger().notifyTransitionStarting(reasons);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(64);
        sb.append("TransitionRecord{");
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(" id=" + this.mSyncId);
        sb.append(" type=" + WindowManager.transitTypeToString(this.mType));
        sb.append(" flags=" + this.mFlags);
        sb.append('}');
        return sb.toString();
    }

    private static boolean reportIfNotTop(WindowContainer wc) {
        return wc.isOrganized();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean isWallpaper(WindowContainer wc) {
        return wc.asWallpaperToken() != null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean occludesKeyguard(WindowContainer wc) {
        ActivityRecord top;
        ActivityRecord ar = wc.asActivityRecord();
        if (ar != null) {
            return ar.canShowWhenLocked();
        }
        Task t = wc.asTask();
        return (t == null || (top = t.getActivity(new Predicate() { // from class: com.android.server.wm.Transition$$ExternalSyntheticLambda1
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return ((ActivityRecord) obj).isClientVisible();
            }
        })) == null || !top.canShowWhenLocked()) ? false : true;
    }

    private static boolean canPromote(WindowContainer<?> target, Targets targets, ArrayMap<WindowContainer, ChangeInfo> changes) {
        Object obj;
        WindowContainer<?> parent = target.getParent();
        ChangeInfo parentChange = changes.get(parent);
        int i = 1;
        if (parent.canCreateRemoteAnimationTarget() && parentChange != null) {
            if (parentChange.hasChanged(parent)) {
                Object obj2 = null;
                if (!isWallpaper(target)) {
                    int mode = changes.get(target).getTransitMode(target);
                    int i2 = parent.getChildCount() - 1;
                    while (i2 >= 0) {
                        WindowContainer<?> sibling = parent.getChildAt(i2);
                        if (target == sibling) {
                            obj = obj2;
                        } else {
                            if (ProtoLogCache.WM_DEBUG_WINDOW_TRANSITIONS_enabled) {
                                String protoLogParam0 = String.valueOf(sibling);
                                ProtoLogGroup protoLogGroup = ProtoLogGroup.WM_DEBUG_WINDOW_TRANSITIONS;
                                Object[] objArr = new Object[i];
                                objArr[0] = protoLogParam0;
                                ProtoLogImpl.v(protoLogGroup, -703543418, 0, "      check sibling %s", objArr);
                            }
                            ChangeInfo siblingChange = changes.get(sibling);
                            if (siblingChange != null && targets.wasParticipated(sibling)) {
                                int siblingMode = siblingChange.getTransitMode(sibling);
                                if (ProtoLogCache.WM_DEBUG_WINDOW_TRANSITIONS_enabled) {
                                    String protoLogParam02 = String.valueOf(TransitionInfo.modeToString(siblingMode));
                                    ProtoLogGroup protoLogGroup2 = ProtoLogGroup.WM_DEBUG_WINDOW_TRANSITIONS;
                                    Object[] objArr2 = new Object[i];
                                    objArr2[0] = protoLogParam02;
                                    ProtoLogImpl.v(protoLogGroup2, -779095785, 0, "        sibling is a participant with mode %s", objArr2);
                                }
                                if (mode == siblingMode) {
                                    obj = null;
                                } else {
                                    if (ProtoLogCache.WM_DEBUG_WINDOW_TRANSITIONS_enabled) {
                                        String protoLogParam03 = String.valueOf(TransitionInfo.modeToString(mode));
                                        ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_WINDOW_TRANSITIONS, 1469310004, 0, "          SKIP: common mode mismatch. was %s", new Object[]{protoLogParam03});
                                    }
                                    return false;
                                }
                            }
                            if (sibling.isVisibleRequested()) {
                                if (ProtoLogCache.WM_DEBUG_WINDOW_TRANSITIONS_enabled) {
                                    ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_WINDOW_TRANSITIONS, 793568608, 0, "        SKIP: sibling is visible but not part of transition", (Object[]) null);
                                }
                                return false;
                            }
                            obj = null;
                            if (ProtoLogCache.WM_DEBUG_WINDOW_TRANSITIONS_enabled) {
                                String protoLogParam04 = String.valueOf(sibling);
                                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_WINDOW_TRANSITIONS, -1728919185, 0, "        unrelated invisible sibling %s", new Object[]{protoLogParam04});
                            }
                        }
                        i2--;
                        obj2 = obj;
                        i = 1;
                    }
                    return true;
                }
                if (ProtoLogCache.WM_DEBUG_WINDOW_TRANSITIONS_enabled) {
                    ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_WINDOW_TRANSITIONS, -2036671725, 0, "      SKIP: is wallpaper", (Object[]) null);
                }
                return false;
            }
        }
        if (ProtoLogCache.WM_DEBUG_WINDOW_TRANSITIONS_enabled) {
            String protoLogParam05 = String.valueOf("parent can't be target " + parent);
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_WINDOW_TRANSITIONS, 744171317, 0, "      SKIP: %s", new Object[]{protoLogParam05});
        }
        return false;
    }

    private static void tryPromote(Targets targets, ArrayMap<WindowContainer, ChangeInfo> changes) {
        WindowContainer<?> lastNonPromotableParent = null;
        int i = targets.mArray.size() - 1;
        while (i >= 0) {
            WindowContainer<?> target = targets.mArray.valueAt(i);
            if (ProtoLogCache.WM_DEBUG_WINDOW_TRANSITIONS_enabled) {
                String protoLogParam0 = String.valueOf(target);
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_WINDOW_TRANSITIONS, -509601642, 0, "    checking %s", new Object[]{protoLogParam0});
            }
            WindowContainer<?> parent = target.getParent();
            if (parent == lastNonPromotableParent) {
                if (ProtoLogCache.WM_DEBUG_WINDOW_TRANSITIONS_enabled) {
                    ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_WINDOW_TRANSITIONS, 112145970, 0, "      SKIP: its sibling was rejected", (Object[]) null);
                }
            } else if (!canPromote(target, targets, changes)) {
                lastNonPromotableParent = parent;
            } else {
                if (reportIfNotTop(target)) {
                    if (ProtoLogCache.WM_DEBUG_WINDOW_TRANSITIONS_enabled) {
                        String protoLogParam02 = String.valueOf(target);
                        ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_WINDOW_TRANSITIONS, 528150092, 0, "        keep as target %s", new Object[]{protoLogParam02});
                    }
                } else {
                    if (ProtoLogCache.WM_DEBUG_WINDOW_TRANSITIONS_enabled) {
                        String protoLogParam03 = String.valueOf(target);
                        ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_WINDOW_TRANSITIONS, 182319432, 0, "        remove from targets %s", new Object[]{protoLogParam03});
                    }
                    targets.remove(i, target);
                }
                if (targets.mArray.indexOfValue(parent) < 0) {
                    if (ProtoLogCache.WM_DEBUG_WINDOW_TRANSITIONS_enabled) {
                        String protoLogParam04 = String.valueOf(parent);
                        ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_WINDOW_TRANSITIONS, -1452274694, 0, "      CAN PROMOTE: promoting to parent %s", new Object[]{protoLogParam04});
                    }
                    i++;
                    targets.add(parent);
                }
            }
            i--;
        }
    }

    static ArrayList<WindowContainer> calculateTargets(ArraySet<WindowContainer> participants, ArrayMap<WindowContainer, ChangeInfo> changes) {
        if (ProtoLogCache.WM_DEBUG_WINDOW_TRANSITIONS_enabled) {
            String protoLogParam0 = String.valueOf(participants);
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_WINDOW_TRANSITIONS, 184610856, 0, "Start calculating TransitionInfo based on participants: %s", new Object[]{protoLogParam0});
        }
        Targets targets = new Targets();
        for (int i = participants.size() - 1; i >= 0; i--) {
            WindowContainer<?> wc = participants.valueAt(i);
            if (!wc.isAttached()) {
                if (ProtoLogCache.WM_DEBUG_WINDOW_TRANSITIONS_enabled) {
                    String protoLogParam02 = String.valueOf(wc);
                    ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_WINDOW_TRANSITIONS, 1494644409, 0, "  Rejecting as detached: %s", new Object[]{protoLogParam02});
                }
            } else if (wc.asWindowState() == null) {
                ChangeInfo changeInfo = changes.get(wc);
                if (!changeInfo.hasChanged(wc)) {
                    if (ProtoLogCache.WM_DEBUG_WINDOW_TRANSITIONS_enabled) {
                        String protoLogParam03 = String.valueOf(wc);
                        ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_WINDOW_TRANSITIONS, -672355406, 0, "  Rejecting as no-op: %s", new Object[]{protoLogParam03});
                    }
                } else {
                    targets.add(wc);
                }
            }
        }
        if (ProtoLogCache.WM_DEBUG_WINDOW_TRANSITIONS_enabled) {
            String protoLogParam04 = String.valueOf(targets.mArray);
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_WINDOW_TRANSITIONS, -1844540996, 0, "  Initial targets: %s", new Object[]{protoLogParam04});
        }
        tryPromote(targets, changes);
        populateParentChanges(targets, changes);
        ArrayList<WindowContainer> targetList = targets.getListSortedByZ();
        if (ProtoLogCache.WM_DEBUG_WINDOW_TRANSITIONS_enabled) {
            String protoLogParam05 = String.valueOf(targetList);
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_WINDOW_TRANSITIONS, 405146734, 0, "  Final targets: %s", new Object[]{protoLogParam05});
        }
        return targetList;
    }

    private static void populateParentChanges(Targets targets, ArrayMap<WindowContainer, ChangeInfo> changes) {
        ArrayList<WindowContainer<?>> intermediates = new ArrayList<>();
        ArrayList<WindowContainer<?>> targetList = new ArrayList<>(targets.mArray.size());
        for (int i = targets.mArray.size() - 1; i >= 0; i--) {
            targetList.add(targets.mArray.valueAt(i));
        }
        int i2 = targetList.size();
        for (int i3 = i2 - 1; i3 >= 0; i3--) {
            WindowContainer<?> wc = targetList.get(i3);
            boolean skipIntermediateReports = isWallpaper(wc);
            intermediates.clear();
            boolean foundParentInTargets = false;
            WindowContainer<?> p = wc.getParent();
            while (true) {
                if (p == null) {
                    break;
                }
                ChangeInfo parentChange = changes.get(p);
                if (parentChange == null || !parentChange.hasChanged(p)) {
                    break;
                }
                if (p.mRemoteToken != null) {
                    if (parentChange.mParent != null && !skipIntermediateReports) {
                        changes.get(wc).mParent = p;
                        break;
                    } else if (targetList.contains(p)) {
                        if (skipIntermediateReports) {
                            changes.get(wc).mParent = p;
                        } else {
                            intermediates.add(p);
                        }
                        foundParentInTargets = true;
                    } else if (reportIfNotTop(p) && !skipIntermediateReports) {
                        intermediates.add(p);
                    }
                }
                p = p.getParent();
            }
            if (foundParentInTargets && !intermediates.isEmpty()) {
                changes.get(wc).mParent = intermediates.get(0);
                for (int j = 0; j < intermediates.size() - 1; j++) {
                    WindowContainer<?> intermediate = intermediates.get(j);
                    changes.get(intermediate).mParent = intermediates.get(j + 1);
                    targets.add(intermediate);
                }
            }
        }
    }

    private static SurfaceControl getLeashSurface(WindowContainer wc) {
        WindowToken asToken;
        SurfaceControl leash;
        DisplayContent asDC = wc.asDisplayContent();
        if (asDC != null) {
            return asDC.getWindowingLayer();
        }
        return (wc.mTransitionController.useShellTransitionsRotation() || (asToken = wc.asWindowToken()) == null || (leash = asToken.getOrCreateFixedRotationLeash()) == null) ? wc.getSurfaceControl() : leash;
    }

    private static SurfaceControl getOrigParentSurface(WindowContainer wc) {
        if (wc.asDisplayContent() != null) {
            return wc.getSurfaceControl();
        }
        return wc.getParent().getSurfaceControl();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean isReadyGroup(WindowContainer wc) {
        return wc instanceof DisplayContent;
    }

    /* JADX WARN: Code restructure failed: missing block: B:22:0x0050, code lost:
        r6 = r6.getParent();
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    static TransitionInfo calculateTransitionInfo(int type, int flags, ArrayList<WindowContainer> sortedTargets, ArrayMap<WindowContainer, ChangeInfo> changes) {
        WindowContainer<?> topApp;
        WindowContainer<?> ancestor;
        TransitionInfo out = new TransitionInfo(type, flags);
        WindowContainer<?> topApp2 = null;
        int i = 0;
        while (true) {
            if (i >= sortedTargets.size()) {
                break;
            }
            WindowContainer<?> wc = sortedTargets.get(i);
            if (isWallpaper(wc)) {
                i++;
            } else {
                topApp2 = wc;
                break;
            }
        }
        if (topApp2 == null) {
            out.setRootLeash(new SurfaceControl(), 0, 0);
            return out;
        }
        WindowContainer<?> ancestor2 = topApp2.getParent();
        loop1: while (ancestor2 != null) {
            for (int i2 = sortedTargets.size() - 1; i2 >= 0; i2--) {
                WindowContainer wc2 = sortedTargets.get(i2);
                if (isWallpaper(wc2) || wc2.isDescendantOf(ancestor2)) {
                }
            }
        }
        WindowContainer leashReference = sortedTargets.get(0);
        while (leashReference.getParent() != ancestor2) {
            leashReference = leashReference.getParent();
        }
        SurfaceControl rootLeash = leashReference.makeAnimationLeash().setName("Transition Root: " + leashReference.getName()).build();
        SurfaceControl.Transaction t = ancestor2.mWmService.mTransactionFactory.get();
        t.setLayer(rootLeash, leashReference.getLastLayer());
        t.apply();
        t.close();
        out.setRootLeash(rootLeash, ancestor2.getBounds().left, ancestor2.getBounds().top);
        int count = sortedTargets.size();
        int i3 = 0;
        while (i3 < count) {
            WindowContainer target = sortedTargets.get(i3);
            ChangeInfo info = changes.get(target);
            TransitionInfo.Change change = new TransitionInfo.Change(target.mRemoteToken != null ? target.mRemoteToken.toWindowContainerToken() : null, getLeashSurface(target));
            if (info.mParent != null) {
                change.setParent(info.mParent.mRemoteToken.toWindowContainerToken());
            }
            change.setMode(info.getTransitMode(target));
            change.setStartAbsBounds(info.mAbsoluteBounds);
            change.setFlags(info.getChangeFlags(target));
            Task task = target.asTask();
            if (task != null) {
                ActivityManager.RunningTaskInfo tinfo = new ActivityManager.RunningTaskInfo();
                task.fillTaskInfo(tinfo);
                change.setTaskInfo(tinfo);
                topApp = topApp2;
                change.setRotationAnimation(getTaskRotationAnimation(task));
                ActivityRecord topMostActivity = task.getTopMostActivity();
                change.setAllowEnterPip(topMostActivity != null && topMostActivity.checkEnterPictureInPictureAppOpsState());
                ActivityRecord topRunningActivity = task.topRunningActivity();
                if (topRunningActivity != null) {
                    ancestor = ancestor2;
                    WindowContainer<?> ancestor3 = task.mDisplayContent;
                    if (ancestor3 != null && !task.inMultiWindowMode()) {
                        int taskRotation = task.getWindowConfiguration().getDisplayRotation();
                        int activityRotation = topRunningActivity.getWindowConfiguration().getDisplayRotation();
                        if (taskRotation != activityRotation) {
                            change.setEndFixedRotation(activityRotation);
                        }
                    }
                } else {
                    ancestor = ancestor2;
                }
            } else {
                topApp = topApp2;
                ancestor = ancestor2;
                if ((info.mFlags & 1) != 0) {
                    change.setRotationAnimation(3);
                }
            }
            WindowContainer<?> parent = target.getParent();
            Rect bounds = target.getBounds();
            Rect parentBounds = parent.getBounds();
            WindowContainer leashReference2 = leashReference;
            SurfaceControl rootLeash2 = rootLeash;
            change.setEndRelOffset(bounds.left - parentBounds.left, bounds.top - parentBounds.top);
            int backgroundColor = target.getWindowConfiguration().getRotation();
            ActivityRecord activityRecord = target.asActivityRecord();
            if (activityRecord != null) {
                Task arTask = activityRecord.getTask();
                int endRotation = arTask.getTaskDescription().getBackgroundColor();
                change.setBackgroundColor(ColorUtils.setAlphaComponent(endRotation, 255));
                change.setEndAbsBounds(parentBounds);
                if (activityRecord.getRelativeDisplayRotation() != 0 && !activityRecord.mTransitionController.useShellTransitionsRotation()) {
                    backgroundColor = parent.getWindowConfiguration().getRotation();
                } else {
                    backgroundColor = backgroundColor;
                }
            } else {
                change.setEndAbsBounds(bounds);
            }
            change.setRotation(info.mRotation, backgroundColor);
            out.addChange(change);
            i3++;
            topApp2 = topApp;
            ancestor2 = ancestor;
            leashReference = leashReference2;
            rootLeash = rootLeash2;
        }
        WindowManager.LayoutParams animLp = getLayoutParamsForAnimationsStyle(type, sortedTargets);
        if (animLp != null && animLp.type != 3 && animLp.windowAnimations != 0) {
            TransitionInfo.AnimationOptions animOptions = TransitionInfo.AnimationOptions.makeAnimOptionsFromLayoutParameters(animLp);
            out.setAnimationOptions(animOptions);
        }
        return out;
    }

    private static WindowManager.LayoutParams getLayoutParamsForAnimationsStyle(int type, ArrayList<WindowContainer> sortedTargets) {
        ArraySet<Integer> activityTypes = new ArraySet<>();
        Iterator<WindowContainer> it = sortedTargets.iterator();
        while (it.hasNext()) {
            WindowContainer target = it.next();
            if (target.asActivityRecord() != null) {
                activityTypes.add(Integer.valueOf(target.getActivityType()));
            } else if (target.asWindowToken() == null && target.asWindowState() == null) {
                return null;
            }
        }
        if (activityTypes.isEmpty()) {
            return null;
        }
        ActivityRecord animLpActivity = findAnimLayoutParamsActivityRecord(sortedTargets, type, activityTypes);
        WindowState mainWindow = animLpActivity != null ? animLpActivity.findMainWindow() : null;
        if (mainWindow != null) {
            return mainWindow.mAttrs;
        }
        return null;
    }

    private static ActivityRecord findAnimLayoutParamsActivityRecord(List<WindowContainer> sortedTargets, final int transit, final ArraySet<Integer> activityTypes) {
        ActivityRecord result = lookForTopWindowWithFilter(sortedTargets, new Predicate() { // from class: com.android.server.wm.Transition$$ExternalSyntheticLambda2
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return Transition.lambda$findAnimLayoutParamsActivityRecord$1(transit, activityTypes, (ActivityRecord) obj);
            }
        });
        if (result != null) {
            return result;
        }
        ActivityRecord result2 = lookForTopWindowWithFilter(sortedTargets, new Predicate() { // from class: com.android.server.wm.Transition$$ExternalSyntheticLambda3
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return Transition.lambda$findAnimLayoutParamsActivityRecord$2((ActivityRecord) obj);
            }
        });
        if (result2 != null) {
            return result2;
        }
        return lookForTopWindowWithFilter(sortedTargets, new Predicate() { // from class: com.android.server.wm.Transition$$ExternalSyntheticLambda4
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return Transition.lambda$findAnimLayoutParamsActivityRecord$3((ActivityRecord) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$findAnimLayoutParamsActivityRecord$1(int transit, ArraySet activityTypes, ActivityRecord w) {
        return w.getRemoteAnimationDefinition() != null && w.getRemoteAnimationDefinition().hasTransition(transit, activityTypes);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$findAnimLayoutParamsActivityRecord$2(ActivityRecord w) {
        return w.fillsParent() && w.findMainWindow() != null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$findAnimLayoutParamsActivityRecord$3(ActivityRecord w) {
        return w.findMainWindow() != null;
    }

    private static ActivityRecord lookForTopWindowWithFilter(List<WindowContainer> sortedTargets, Predicate<ActivityRecord> filter) {
        ActivityRecord activityRecord;
        for (WindowContainer target : sortedTargets) {
            if (target.asTaskFragment() != null) {
                activityRecord = target.asTaskFragment().getTopNonFinishingActivity();
            } else {
                activityRecord = target.asActivityRecord();
            }
            if (activityRecord != null && filter.test(activityRecord)) {
                return activityRecord;
            }
        }
        return null;
    }

    private static int getTaskRotationAnimation(Task task) {
        WindowState mainWin;
        ActivityRecord top = task.getTopVisibleActivity();
        if (top == null || (mainWin = top.findMainWindow(false)) == null) {
            return -1;
        }
        int anim = mainWin.getRotationAnimationHint();
        if (anim >= 0) {
            return anim;
        }
        int anim2 = mainWin.getAttrs().rotationAnimation;
        if (anim2 != 3) {
            return anim2;
        }
        if (mainWin != task.mDisplayContent.getDisplayPolicy().getTopFullscreenOpaqueWindow() || !top.matchParentBounds()) {
            return -1;
        }
        return mainWin.getAttrs().rotationAnimation;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean getLegacyIsReady() {
        int i = this.mState;
        return (i == 1 || i == 0) && this.mSyncId >= 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static Transition fromBinder(IBinder binder) {
        return (Transition) binder;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static class ChangeInfo {
        private static final int FLAG_NONE = 0;
        private static final int FLAG_SEAMLESS_ROTATION = 1;
        final Rect mAbsoluteBounds;
        boolean mExistenceChanged;
        int mFlags;
        int mKnownConfigChanges;
        WindowContainer mParent;
        int mRotation;
        boolean mShowWallpaper;
        boolean mVisible;
        int mWindowingMode;

        @Retention(RetentionPolicy.SOURCE)
        /* loaded from: classes2.dex */
        @interface Flag {
        }

        ChangeInfo(WindowContainer origState) {
            this.mExistenceChanged = false;
            Rect rect = new Rect();
            this.mAbsoluteBounds = rect;
            this.mRotation = -1;
            this.mFlags = 0;
            this.mVisible = origState.isVisibleRequested();
            this.mWindowingMode = origState.getWindowingMode();
            rect.set(origState.getBounds());
            this.mShowWallpaper = origState.showWallpaper();
            this.mRotation = origState.getWindowConfiguration().getRotation();
        }

        ChangeInfo(boolean visible, boolean existChange) {
            this.mExistenceChanged = false;
            this.mAbsoluteBounds = new Rect();
            this.mRotation = -1;
            this.mFlags = 0;
            this.mVisible = visible;
            this.mExistenceChanged = existChange;
            this.mShowWallpaper = false;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public boolean hasChanged(WindowContainer newState) {
            boolean currVisible = newState.isVisibleRequested();
            boolean z = this.mVisible;
            if (currVisible != z || z) {
                return (currVisible == z && this.mKnownConfigChanges == 0 && (this.mWindowingMode == 0 || newState.getWindowingMode() == this.mWindowingMode) && newState.getBounds().equals(this.mAbsoluteBounds) && this.mRotation == newState.getWindowConfiguration().getRotation()) ? false : true;
            }
            return false;
        }

        int getTransitMode(WindowContainer wc) {
            boolean nowVisible = wc.isVisibleRequested();
            if (nowVisible == this.mVisible) {
                return 6;
            }
            return this.mExistenceChanged ? nowVisible ? 1 : 2 : nowVisible ? 3 : 4;
        }

        int getChangeFlags(WindowContainer wc) {
            int flags = 0;
            if (this.mShowWallpaper || wc.showWallpaper()) {
                flags = 0 | 1;
            }
            if (!wc.fillsParent()) {
                flags |= 4;
            }
            Task task = wc.asTask();
            if (task != null && task.voiceSession != null) {
                flags |= 16;
            }
            if (task != null && task.isTranslucent(null)) {
                flags |= 4;
            }
            ActivityRecord record = wc.asActivityRecord();
            if (record != null) {
                if (record.mUseTransferredAnimation) {
                    flags |= 8;
                }
                if (record.mVoiceInteraction) {
                    flags |= 16;
                }
            }
            DisplayContent dc = wc.asDisplayContent();
            if (dc != null) {
                flags |= 32;
                if (dc.hasAlertWindowSurfaces()) {
                    flags |= 128;
                }
            }
            if (Transition.isWallpaper(wc)) {
                flags |= 2;
            }
            if (Transition.occludesKeyguard(wc)) {
                return flags | 64;
            }
            return flags;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class ReadyTracker {
        private final ArrayMap<WindowContainer, Boolean> mReadyGroups;
        private boolean mReadyOverride;
        private boolean mUsed;

        private ReadyTracker() {
            this.mReadyGroups = new ArrayMap<>();
            this.mUsed = false;
            this.mReadyOverride = false;
        }

        void addGroup(WindowContainer wc) {
            if (this.mReadyGroups.containsKey(wc)) {
                Slog.e(Transition.TAG, "Trying to add a ready-group twice: " + wc);
            } else {
                this.mReadyGroups.put(wc, false);
            }
        }

        void setReadyFrom(WindowContainer wc, boolean ready) {
            this.mUsed = true;
            for (WindowContainer current = wc; current != null; current = current.getParent()) {
                if (Transition.isReadyGroup(current)) {
                    this.mReadyGroups.put(current, Boolean.valueOf(ready));
                    if (ProtoLogCache.WM_DEBUG_WINDOW_TRANSITIONS_enabled) {
                        String protoLogParam1 = String.valueOf(current);
                        String protoLogParam2 = String.valueOf(wc);
                        ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_WINDOW_TRANSITIONS, -1924376693, 3, " Setting Ready-group to %b. group=%s from %s", new Object[]{Boolean.valueOf(ready), protoLogParam1, protoLogParam2});
                        return;
                    }
                    return;
                }
            }
        }

        void setAllReady() {
            if (ProtoLogCache.WM_DEBUG_WINDOW_TRANSITIONS_enabled) {
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_WINDOW_TRANSITIONS, 1670933628, 0, " Setting allReady override", (Object[]) null);
            }
            this.mUsed = true;
            this.mReadyOverride = true;
        }

        boolean allReady() {
            if (ProtoLogCache.WM_DEBUG_WINDOW_TRANSITIONS_enabled) {
                boolean protoLogParam0 = this.mUsed;
                boolean protoLogParam1 = this.mReadyOverride;
                String protoLogParam2 = String.valueOf(groupsToString());
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_WINDOW_TRANSITIONS, 352982444, 15, " allReady query: used=%b override=%b states=[%s]", new Object[]{Boolean.valueOf(protoLogParam0), Boolean.valueOf(protoLogParam1), protoLogParam2});
            }
            boolean protoLogParam02 = this.mUsed;
            if (protoLogParam02) {
                if (this.mReadyOverride) {
                    return true;
                }
                for (int i = this.mReadyGroups.size() - 1; i >= 0; i--) {
                    WindowContainer wc = this.mReadyGroups.keyAt(i);
                    if (wc.isAttached() && wc.isVisibleRequested() && !this.mReadyGroups.valueAt(i).booleanValue()) {
                        return false;
                    }
                }
                return true;
            }
            return false;
        }

        private String groupsToString() {
            StringBuilder b = new StringBuilder();
            for (int i = 0; i < this.mReadyGroups.size(); i++) {
                if (i != 0) {
                    b.append(',');
                }
                b.append(this.mReadyGroups.keyAt(i)).append(':').append(this.mReadyGroups.valueAt(i));
            }
            return b.toString();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class Targets {
        final SparseArray<WindowContainer<?>> mArray;
        private int mDepthFactor;
        private ArrayList<WindowContainer<?>> mRemovedTargets;

        private Targets() {
            this.mArray = new SparseArray<>();
        }

        void add(WindowContainer<?> target) {
            if (this.mDepthFactor == 0) {
                this.mDepthFactor = target.mWmService.mRoot.getTreeWeight() + 1;
            }
            int score = target.getPrefixOrderIndex();
            WindowContainer<?> wc = target;
            while (wc != null) {
                WindowContainer<?> parent = wc.getParent();
                if (parent != null) {
                    score += this.mDepthFactor;
                }
                wc = parent;
            }
            this.mArray.put(score, target);
        }

        void remove(int index, WindowContainer<?> removingTarget) {
            this.mArray.removeAt(index);
            if (this.mRemovedTargets == null) {
                this.mRemovedTargets = new ArrayList<>();
            }
            this.mRemovedTargets.add(removingTarget);
        }

        boolean wasParticipated(WindowContainer<?> wc) {
            ArrayList<WindowContainer<?>> arrayList;
            return this.mArray.indexOfValue(wc) >= 0 || ((arrayList = this.mRemovedTargets) != null && arrayList.contains(wc));
        }

        ArrayList<WindowContainer> getListSortedByZ() {
            SparseArray<WindowContainer<?>> arrayByZ = new SparseArray<>(this.mArray.size());
            for (int i = this.mArray.size() - 1; i >= 0; i--) {
                int zOrder = this.mArray.keyAt(i) % this.mDepthFactor;
                arrayByZ.put(zOrder, this.mArray.valueAt(i));
            }
            ArrayList<WindowContainer> sortedTargets = new ArrayList<>(arrayByZ.size());
            for (int i2 = arrayByZ.size() - 1; i2 >= 0; i2--) {
                sortedTargets.add(arrayByZ.valueAt(i2));
            }
            return sortedTargets;
        }
    }
}
