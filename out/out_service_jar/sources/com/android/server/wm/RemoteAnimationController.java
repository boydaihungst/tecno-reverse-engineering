package com.android.server.wm;

import android.app.ActivityTaskManager;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import android.view.IRemoteAnimationFinishedCallback;
import android.view.RemoteAnimationAdapter;
import android.view.RemoteAnimationTarget;
import android.view.SurfaceControl;
import com.android.internal.protolog.ProtoLogGroup;
import com.android.internal.protolog.ProtoLogImpl;
import com.android.internal.util.FastPrintWriter;
import com.android.server.wm.SurfaceAnimator;
import com.transsion.hubcore.server.wm.ITranRemoteAnimationController;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.function.Consumer;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class RemoteAnimationController implements IBinder.DeathRecipient {
    private static final String TAG = "WindowManager";
    private static final long TIMEOUT_MS = 10000;
    private boolean mCanceled;
    private final DisplayContent mDisplayContent;
    private FinishedCallback mFinishedCallback;
    private final Handler mHandler;
    private boolean mIsForThunderback;
    private boolean mIsRunning;
    private boolean mLinkedToDeathOfRunner;
    private Runnable mOnRemoteAnimationReady;
    private final RemoteAnimationAdapter mRemoteAnimationAdapter;
    private final WindowManagerService mService;
    private final ArrayList<RemoteAnimationRecord> mPendingAnimations = new ArrayList<>();
    private final ArrayList<WallpaperAnimationAdapter> mPendingWallpaperAnimations = new ArrayList<>();
    final ArrayList<NonAppWindowAnimationAdapter> mPendingNonAppAnimations = new ArrayList<>();
    private final Runnable mTimeoutRunnable = new Runnable() { // from class: com.android.server.wm.RemoteAnimationController$$ExternalSyntheticLambda2
        @Override // java.lang.Runnable
        public final void run() {
            RemoteAnimationController.this.m8178lambda$new$0$comandroidserverwmRemoteAnimationController();
        }
    };

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$0$com-android-server-wm-RemoteAnimationController  reason: not valid java name */
    public /* synthetic */ void m8178lambda$new$0$comandroidserverwmRemoteAnimationController() {
        cancelAnimation("timeoutRunnable");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public RemoteAnimationController(WindowManagerService service, DisplayContent displayContent, RemoteAnimationAdapter remoteAnimationAdapter, Handler handler) {
        this.mService = service;
        this.mDisplayContent = displayContent;
        this.mRemoteAnimationAdapter = remoteAnimationAdapter;
        this.mHandler = handler;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public RemoteAnimationRecord createRemoteAnimationRecord(WindowContainer windowContainer, Point position, Rect localBounds, Rect endBounds, Rect startBounds) {
        if (ProtoLogCache.WM_DEBUG_REMOTE_ANIMATIONS_enabled) {
            String protoLogParam0 = String.valueOf(windowContainer);
            ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_REMOTE_ANIMATIONS, 2022422429, 0, (String) null, new Object[]{protoLogParam0});
        }
        RemoteAnimationRecord adapters = new RemoteAnimationRecord(windowContainer, position, localBounds, endBounds, startBounds);
        this.mPendingAnimations.add(adapters);
        return adapters;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setOnRemoteAnimationReady(Runnable onRemoteAnimationReady) {
        this.mOnRemoteAnimationReady = onRemoteAnimationReady;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void goodToGo(final int transit) {
        if (ProtoLogCache.WM_DEBUG_REMOTE_ANIMATIONS_enabled) {
            ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_REMOTE_ANIMATIONS, 873914452, 0, (String) null, (Object[]) null);
        }
        if (this.mCanceled) {
            if (ProtoLogCache.WM_DEBUG_REMOTE_ANIMATIONS_enabled) {
                ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_REMOTE_ANIMATIONS, 269976641, 0, (String) null, (Object[]) null);
            }
            onAnimationFinished();
            invokeAnimationCancelled("already_cancelled");
            return;
        }
        this.mHandler.postDelayed(this.mTimeoutRunnable, this.mService.getCurrentAnimatorScale() * 10000.0f);
        this.mFinishedCallback = new FinishedCallback(this);
        final RemoteAnimationTarget[] appTargets = createAppAnimations();
        if (appTargets.length == 0 && !AppTransition.isKeyguardOccludeTransitOld(transit)) {
            if (ProtoLogCache.WM_DEBUG_REMOTE_ANIMATIONS_enabled) {
                long protoLogParam0 = this.mPendingAnimations.size();
                ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_REMOTE_ANIMATIONS, -1777196134, 1, (String) null, new Object[]{Long.valueOf(protoLogParam0)});
            }
            onAnimationFinished();
            invokeAnimationCancelled("no_app_targets");
            return;
        }
        Runnable runnable = this.mOnRemoteAnimationReady;
        if (runnable != null) {
            runnable.run();
            this.mOnRemoteAnimationReady = null;
        }
        final RemoteAnimationTarget[] wallpaperTargets = createWallpaperAnimations();
        final RemoteAnimationTarget[] nonAppTargets = createNonAppWindowAnimations(transit);
        this.mService.mAnimator.addAfterPrepareSurfacesRunnable(new Runnable() { // from class: com.android.server.wm.RemoteAnimationController$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                RemoteAnimationController.this.m8177x58c85769(transit, appTargets, wallpaperTargets, nonAppTargets);
            }
        });
        setRunningRemoteAnimation(true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$goodToGo$1$com-android-server-wm-RemoteAnimationController  reason: not valid java name */
    public /* synthetic */ void m8177x58c85769(int transit, RemoteAnimationTarget[] appTargets, RemoteAnimationTarget[] wallpaperTargets, RemoteAnimationTarget[] nonAppTargets) {
        try {
            linkToDeathOfRunner();
            if (ProtoLogCache.WM_DEBUG_REMOTE_ANIMATIONS_enabled) {
                String protoLogParam0 = String.valueOf(AppTransition.appTransitionOldToString(transit));
                long protoLogParam1 = appTargets.length;
                long protoLogParam2 = wallpaperTargets.length;
                long protoLogParam3 = nonAppTargets.length;
                ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_REMOTE_ANIMATIONS, 35398067, 84, (String) null, new Object[]{protoLogParam0, Long.valueOf(protoLogParam1), Long.valueOf(protoLogParam2), Long.valueOf(protoLogParam3)});
            }
            if (Build.IS_DEBUG_ENABLE) {
                Slog.d("WindowManager", "goodToGo(): onAnimationStart transit = " + AppTransition.appTransitionOldToString(transit) + ", apps = " + appTargets.length + ", wallpapers = " + wallpaperTargets.length + ", nonApps = " + nonAppTargets.length);
            }
            ActivityTaskManager.getService().boostSceneStart(6);
            this.mRemoteAnimationAdapter.getRunner().onAnimationStart(transit, appTargets, wallpaperTargets, nonAppTargets, this.mFinishedCallback);
        } catch (RemoteException e) {
            Slog.e("WindowManager", "Failed to start remote animation", e);
            onAnimationFinished();
        }
        if (ProtoLogImpl.isEnabled(ProtoLogGroup.WM_DEBUG_REMOTE_ANIMATIONS)) {
            if (ProtoLogCache.WM_DEBUG_REMOTE_ANIMATIONS_enabled) {
                ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_REMOTE_ANIMATIONS, -2012562539, 0, (String) null, (Object[]) null);
            }
            writeStartDebugStatement();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void cancelAnimation(String reason) {
        if (ProtoLogCache.WM_DEBUG_REMOTE_ANIMATIONS_enabled) {
            String protoLogParam0 = String.valueOf(reason);
            ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_REMOTE_ANIMATIONS, 1891501279, 0, (String) null, new Object[]{protoLogParam0});
        }
        synchronized (this.mService.getWindowManagerLock()) {
            if (this.mCanceled) {
                return;
            }
            this.mCanceled = true;
            onAnimationFinished();
            invokeAnimationCancelled(reason);
        }
    }

    private void writeStartDebugStatement() {
        if (ProtoLogCache.WM_DEBUG_REMOTE_ANIMATIONS_enabled) {
            ProtoLogImpl.i(ProtoLogGroup.WM_DEBUG_REMOTE_ANIMATIONS, 1964565370, 0, (String) null, (Object[]) null);
        }
        StringWriter sw = new StringWriter();
        PrintWriter fastPrintWriter = new FastPrintWriter(sw);
        for (int i = this.mPendingAnimations.size() - 1; i >= 0; i--) {
            this.mPendingAnimations.get(i).mAdapter.dump(fastPrintWriter, "");
        }
        fastPrintWriter.close();
        if (ProtoLogCache.WM_DEBUG_REMOTE_ANIMATIONS_enabled) {
            String protoLogParam0 = String.valueOf(sw.toString());
            ProtoLogImpl.i(ProtoLogGroup.WM_DEBUG_REMOTE_ANIMATIONS, 835814848, 0, (String) null, new Object[]{protoLogParam0});
        }
    }

    private RemoteAnimationTarget[] createAppAnimations() {
        if (ProtoLogCache.WM_DEBUG_REMOTE_ANIMATIONS_enabled) {
            ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_REMOTE_ANIMATIONS, -653156702, 0, (String) null, (Object[]) null);
        }
        ArrayList<RemoteAnimationTarget> targets = new ArrayList<>();
        for (int i = this.mPendingAnimations.size() - 1; i >= 0; i--) {
            RemoteAnimationRecord wrappers = this.mPendingAnimations.get(i);
            RemoteAnimationTarget target = wrappers.createRemoteAnimationTarget();
            if (target != null) {
                if (ProtoLogCache.WM_DEBUG_REMOTE_ANIMATIONS_enabled) {
                    String protoLogParam0 = String.valueOf(wrappers.mWindowContainer);
                    ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_REMOTE_ANIMATIONS, -1248645819, 0, (String) null, new Object[]{protoLogParam0});
                }
                targets.add(target);
            } else {
                if (ProtoLogCache.WM_DEBUG_REMOTE_ANIMATIONS_enabled) {
                    String protoLogParam02 = String.valueOf(wrappers.mWindowContainer);
                    ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_REMOTE_ANIMATIONS, 638429464, 0, (String) null, new Object[]{protoLogParam02});
                }
                if (wrappers.mAdapter != null && wrappers.mAdapter.mCapturedFinishCallback != null) {
                    wrappers.mAdapter.mCapturedFinishCallback.onAnimationFinished(wrappers.mAdapter.mAnimationType, wrappers.mAdapter);
                }
                if (wrappers.mThumbnailAdapter != null && wrappers.mThumbnailAdapter.mCapturedFinishCallback != null) {
                    wrappers.mThumbnailAdapter.mCapturedFinishCallback.onAnimationFinished(wrappers.mThumbnailAdapter.mAnimationType, wrappers.mThumbnailAdapter);
                }
                this.mPendingAnimations.remove(i);
            }
        }
        return (RemoteAnimationTarget[]) targets.toArray(new RemoteAnimationTarget[targets.size()]);
    }

    private RemoteAnimationTarget[] createWallpaperAnimations() {
        if (ProtoLogCache.WM_DEBUG_REMOTE_ANIMATIONS_enabled) {
            ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_REMOTE_ANIMATIONS, 594260577, 0, (String) null, (Object[]) null);
        }
        return WallpaperAnimationAdapter.startWallpaperAnimations(this.mDisplayContent, this.mRemoteAnimationAdapter.getDuration(), this.mRemoteAnimationAdapter.getStatusBarTransitionDelay(), new Consumer() { // from class: com.android.server.wm.RemoteAnimationController$$ExternalSyntheticLambda0
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                RemoteAnimationController.this.m8176x1ed78d25((WallpaperAnimationAdapter) obj);
            }
        }, this.mPendingWallpaperAnimations);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$createWallpaperAnimations$2$com-android-server-wm-RemoteAnimationController  reason: not valid java name */
    public /* synthetic */ void m8176x1ed78d25(WallpaperAnimationAdapter adapter) {
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                this.mPendingWallpaperAnimations.remove(adapter);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    private RemoteAnimationTarget[] createNonAppWindowAnimations(int transit) {
        if (ProtoLogCache.WM_DEBUG_REMOTE_ANIMATIONS_enabled) {
            ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_REMOTE_ANIMATIONS, -1834214907, 0, (String) null, (Object[]) null);
        }
        return NonAppWindowAnimationAdapter.startNonAppWindowAnimations(this.mService, this.mDisplayContent, transit, this.mRemoteAnimationAdapter.getDuration(), this.mRemoteAnimationAdapter.getStatusBarTransitionDelay(), this.mPendingNonAppAnimations);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onAnimationFinished() {
        if (ProtoLogCache.WM_DEBUG_REMOTE_ANIMATIONS_enabled) {
            long protoLogParam0 = this.mPendingAnimations.size();
            ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_REMOTE_ANIMATIONS, -1497837552, 1, (String) null, new Object[]{Long.valueOf(protoLogParam0)});
        }
        this.mHandler.removeCallbacks(this.mTimeoutRunnable);
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                this.mIsRunning = false;
                unlinkToDeathOfRunner();
                releaseFinishedCallback();
                this.mService.openSurfaceTransaction();
                try {
                    if (ProtoLogCache.WM_DEBUG_REMOTE_ANIMATIONS_enabled) {
                        Object[] objArr = null;
                        ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_REMOTE_ANIMATIONS, 557227556, 0, (String) null, (Object[]) null);
                    }
                    for (int i = this.mPendingAnimations.size() - 1; i >= 0; i--) {
                        RemoteAnimationRecord adapters = this.mPendingAnimations.get(i);
                        if (adapters.mAdapter != null && adapters.mAdapter.mCapturedFinishCallback != null) {
                            adapters.mAdapter.mCapturedFinishCallback.onAnimationFinished(adapters.mAdapter.mAnimationType, adapters.mAdapter);
                        }
                        if (adapters.mThumbnailAdapter != null && adapters.mThumbnailAdapter.mCapturedFinishCallback != null) {
                            adapters.mThumbnailAdapter.mCapturedFinishCallback.onAnimationFinished(adapters.mThumbnailAdapter.mAnimationType, adapters.mThumbnailAdapter);
                        }
                        this.mPendingAnimations.remove(i);
                        if (ProtoLogCache.WM_DEBUG_REMOTE_ANIMATIONS_enabled) {
                            String protoLogParam02 = String.valueOf(adapters.mWindowContainer);
                            ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_REMOTE_ANIMATIONS, 972354148, 0, (String) null, new Object[]{protoLogParam02});
                        }
                    }
                    for (int i2 = this.mPendingWallpaperAnimations.size() - 1; i2 >= 0; i2--) {
                        WallpaperAnimationAdapter adapter = this.mPendingWallpaperAnimations.get(i2);
                        adapter.getLeashFinishedCallback().onAnimationFinished(adapter.getLastAnimationType(), adapter);
                        this.mPendingWallpaperAnimations.remove(i2);
                        if (ProtoLogCache.WM_DEBUG_REMOTE_ANIMATIONS_enabled) {
                            String protoLogParam03 = String.valueOf(adapter.getToken());
                            ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_REMOTE_ANIMATIONS, -853404763, 0, (String) null, new Object[]{protoLogParam03});
                        }
                    }
                    for (int i3 = this.mPendingNonAppAnimations.size() - 1; i3 >= 0; i3--) {
                        NonAppWindowAnimationAdapter adapter2 = this.mPendingNonAppAnimations.get(i3);
                        adapter2.getLeashFinishedCallback().onAnimationFinished(adapter2.getLastAnimationType(), adapter2);
                        this.mPendingNonAppAnimations.remove(i3);
                        if (ProtoLogCache.WM_DEBUG_REMOTE_ANIMATIONS_enabled) {
                            String protoLogParam04 = String.valueOf(adapter2.getWindowContainer());
                            ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_REMOTE_ANIMATIONS, 1931178855, 0, (String) null, new Object[]{protoLogParam04});
                        }
                    }
                    this.mService.closeSurfaceTransaction("RemoteAnimationController#finished");
                    Consumer<ActivityRecord> updateActivities = new Consumer() { // from class: com.android.server.wm.RemoteAnimationController$$ExternalSyntheticLambda3
                        @Override // java.util.function.Consumer
                        public final void accept(Object obj) {
                            ((ActivityRecord) obj).setDropInputForAnimation(false);
                        }
                    };
                    this.mDisplayContent.forAllActivities(updateActivities);
                } catch (Exception e) {
                    Slog.e("WindowManager", "Failed to finish remote animation", e);
                    throw e;
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        setRunningRemoteAnimation(false);
        ITranRemoteAnimationController.Instance().updateRotation(this.mIsForThunderback, this.mService, "WindowManager");
        if (ProtoLogCache.WM_DEBUG_REMOTE_ANIMATIONS_enabled) {
            ProtoLogImpl.i(ProtoLogGroup.WM_DEBUG_REMOTE_ANIMATIONS, 248210157, 0, (String) null, (Object[]) null);
        }
        try {
            ActivityTaskManager.getService().boostSceneEnd(6);
        } catch (Exception e2) {
            Slog.e("WindowManager", "Failed to boost remote animation", e2);
        }
    }

    private void invokeAnimationCancelled(String reason) {
        if (ProtoLogCache.WM_DEBUG_REMOTE_ANIMATIONS_enabled) {
            String protoLogParam0 = String.valueOf(reason);
            ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_REMOTE_ANIMATIONS, 1891501279, 0, (String) null, new Object[]{protoLogParam0});
        }
        boolean isKeyguardOccluded = this.mDisplayContent.isKeyguardOccluded();
        try {
            this.mRemoteAnimationAdapter.getRunner().onAnimationCancelled(isKeyguardOccluded);
        } catch (RemoteException e) {
            Slog.e("WindowManager", "Failed to notify cancel", e);
        }
        this.mOnRemoteAnimationReady = null;
    }

    private void releaseFinishedCallback() {
        FinishedCallback finishedCallback = this.mFinishedCallback;
        if (finishedCallback != null) {
            finishedCallback.release();
            this.mFinishedCallback = null;
        }
    }

    private void setRunningRemoteAnimation(boolean running) {
        int pid = this.mRemoteAnimationAdapter.getCallingPid();
        int uid = this.mRemoteAnimationAdapter.getCallingUid();
        this.mIsRunning = running;
        if (pid == 0) {
            throw new RuntimeException("Calling pid of remote animation was null");
        }
        WindowProcessController wpc = this.mService.mAtmService.getProcessController(pid, uid);
        if (wpc == null) {
            Slog.w("WindowManager", "Unable to find process with pid=" + pid + " uid=" + uid);
        } else {
            wpc.setRunningRemoteAnimation(running);
        }
    }

    private void linkToDeathOfRunner() throws RemoteException {
        if (!this.mLinkedToDeathOfRunner) {
            this.mRemoteAnimationAdapter.getRunner().asBinder().linkToDeath(this, 0);
            this.mLinkedToDeathOfRunner = true;
        }
    }

    private void unlinkToDeathOfRunner() {
        if (this.mLinkedToDeathOfRunner) {
            this.mRemoteAnimationAdapter.getRunner().asBinder().unlinkToDeath(this, 0);
            this.mLinkedToDeathOfRunner = false;
        }
    }

    @Override // android.os.IBinder.DeathRecipient
    public void binderDied() {
        cancelAnimation("binderDied");
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static final class FinishedCallback extends IRemoteAnimationFinishedCallback.Stub {
        RemoteAnimationController mOuter;

        FinishedCallback(RemoteAnimationController outer) {
            this.mOuter = outer;
        }

        public void onAnimationFinished() throws RemoteException {
            if (ProtoLogCache.WM_DEBUG_REMOTE_ANIMATIONS_enabled) {
                String protoLogParam0 = String.valueOf(this.mOuter);
                ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_REMOTE_ANIMATIONS, -2024464438, 0, (String) null, new Object[]{protoLogParam0});
            }
            long token = Binder.clearCallingIdentity();
            try {
                RemoteAnimationController remoteAnimationController = this.mOuter;
                if (remoteAnimationController != null) {
                    remoteAnimationController.onAnimationFinished();
                    this.mOuter = null;
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        void release() {
            if (ProtoLogCache.WM_DEBUG_REMOTE_ANIMATIONS_enabled) {
                String protoLogParam0 = String.valueOf(this.mOuter);
                ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_REMOTE_ANIMATIONS, -2109864870, 0, (String) null, new Object[]{protoLogParam0});
            }
            this.mOuter = null;
        }
    }

    /* loaded from: classes2.dex */
    public class RemoteAnimationRecord {
        RemoteAnimationAdapterWrapper mAdapter;
        private int mMode = 2;
        final Rect mStartBounds;
        RemoteAnimationTarget mTarget;
        RemoteAnimationAdapterWrapper mThumbnailAdapter;
        final WindowContainer mWindowContainer;

        RemoteAnimationRecord(WindowContainer windowContainer, Point endPos, Rect localBounds, Rect endBounds, Rect startBounds) {
            this.mThumbnailAdapter = null;
            this.mWindowContainer = windowContainer;
            if (startBounds == null) {
                this.mAdapter = new RemoteAnimationAdapterWrapper(this, endPos, localBounds, endBounds, new Rect());
                this.mStartBounds = null;
                return;
            }
            Rect rect = new Rect(startBounds);
            this.mStartBounds = rect;
            this.mAdapter = new RemoteAnimationAdapterWrapper(this, endPos, localBounds, endBounds, rect);
            if (RemoteAnimationController.this.mRemoteAnimationAdapter.getChangeNeedsSnapshot()) {
                Rect thumbnailLocalBounds = new Rect(startBounds);
                thumbnailLocalBounds.offsetTo(0, 0);
                this.mThumbnailAdapter = new RemoteAnimationAdapterWrapper(this, new Point(0, 0), thumbnailLocalBounds, startBounds, new Rect());
            }
        }

        RemoteAnimationTarget createRemoteAnimationTarget() {
            RemoteAnimationAdapterWrapper remoteAnimationAdapterWrapper = this.mAdapter;
            if (remoteAnimationAdapterWrapper == null || remoteAnimationAdapterWrapper.mCapturedFinishCallback == null || this.mAdapter.mCapturedLeash == null) {
                return null;
            }
            RemoteAnimationTarget createRemoteAnimationTarget = this.mWindowContainer.createRemoteAnimationTarget(this);
            this.mTarget = createRemoteAnimationTarget;
            return createRemoteAnimationTarget;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public void setMode(int mode) {
            this.mMode = mode;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public int getMode() {
            return this.mMode;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public boolean hasAnimatingParent() {
            for (int i = RemoteAnimationController.this.mDisplayContent.mChangingContainers.size() - 1; i >= 0; i--) {
                if (this.mWindowContainer.isDescendantOf(RemoteAnimationController.this.mDisplayContent.mChangingContainers.valueAt(i))) {
                    return true;
                }
            }
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public class RemoteAnimationAdapterWrapper implements AnimationAdapter {
        private int mAnimationType;
        private SurfaceAnimator.OnAnimationFinishedCallback mCapturedFinishCallback;
        SurfaceControl mCapturedLeash;
        final Rect mEndBounds;
        final Rect mLocalBounds;
        final Point mPosition;
        private final RemoteAnimationRecord mRecord;
        final Rect mStartBounds;

        RemoteAnimationAdapterWrapper(RemoteAnimationRecord record, Point position, Rect localBounds, Rect endBounds, Rect startBounds) {
            Point point = new Point();
            this.mPosition = point;
            Rect rect = new Rect();
            this.mEndBounds = rect;
            Rect rect2 = new Rect();
            this.mStartBounds = rect2;
            this.mRecord = record;
            point.set(position.x, position.y);
            this.mLocalBounds = localBounds;
            rect.set(endBounds);
            rect2.set(startBounds);
        }

        @Override // com.android.server.wm.AnimationAdapter
        public boolean getShowWallpaper() {
            return false;
        }

        @Override // com.android.server.wm.AnimationAdapter
        public void startAnimation(SurfaceControl animationLeash, SurfaceControl.Transaction t, int type, SurfaceAnimator.OnAnimationFinishedCallback finishCallback) {
            if (ProtoLogCache.WM_DEBUG_REMOTE_ANIMATIONS_enabled) {
                ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_REMOTE_ANIMATIONS, -1596995693, 0, (String) null, (Object[]) null);
            }
            if (this.mStartBounds.isEmpty()) {
                t.setPosition(animationLeash, this.mPosition.x, this.mPosition.y);
                t.setWindowCrop(animationLeash, this.mEndBounds.width(), this.mEndBounds.height());
            } else {
                t.setPosition(animationLeash, (this.mPosition.x + this.mStartBounds.left) - this.mEndBounds.left, (this.mPosition.y + this.mStartBounds.top) - this.mEndBounds.top);
                t.setWindowCrop(animationLeash, this.mStartBounds.width(), this.mStartBounds.height());
            }
            this.mCapturedLeash = animationLeash;
            this.mCapturedFinishCallback = finishCallback;
            this.mAnimationType = type;
        }

        @Override // com.android.server.wm.AnimationAdapter
        public void onAnimationCancelled(SurfaceControl animationLeash) {
            if (this.mRecord.mAdapter == this) {
                this.mRecord.mAdapter = null;
            } else {
                this.mRecord.mThumbnailAdapter = null;
            }
            if (this.mRecord.mAdapter == null && this.mRecord.mThumbnailAdapter == null) {
                RemoteAnimationController.this.mPendingAnimations.remove(this.mRecord);
            }
            if (RemoteAnimationController.this.mPendingAnimations.isEmpty()) {
                RemoteAnimationController.this.cancelAnimation("allAppAnimationsCanceled");
            }
        }

        @Override // com.android.server.wm.AnimationAdapter
        public long getDurationHint() {
            return RemoteAnimationController.this.mRemoteAnimationAdapter.getDuration();
        }

        @Override // com.android.server.wm.AnimationAdapter
        public long getStatusBarTransitionsStartTime() {
            return SystemClock.uptimeMillis() + RemoteAnimationController.this.mRemoteAnimationAdapter.getStatusBarTransitionDelay();
        }

        @Override // com.android.server.wm.AnimationAdapter
        public void dump(PrintWriter pw, String prefix) {
            pw.print(prefix);
            pw.print("container=");
            pw.println(this.mRecord.mWindowContainer);
            if (this.mRecord.mTarget != null) {
                pw.print(prefix);
                pw.println("Target:");
                this.mRecord.mTarget.dump(pw, prefix + "  ");
                return;
            }
            pw.print(prefix);
            pw.println("Target: null");
        }

        @Override // com.android.server.wm.AnimationAdapter
        public void dumpDebug(ProtoOutputStream proto) {
            long token = proto.start(1146756268034L);
            if (this.mRecord.mTarget != null) {
                this.mRecord.mTarget.dumpDebug(proto, 1146756268033L);
            }
            proto.end(token);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isRunning(RemoteAnimationAdapter adapter) {
        if (adapter == this.mRemoteAnimationAdapter) {
            return this.mIsRunning;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setIsForThunderback() {
        this.mIsForThunderback = true;
    }
}
