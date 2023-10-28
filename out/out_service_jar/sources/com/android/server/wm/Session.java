package com.android.server.wm;

import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ShortcutServiceInternal;
import android.graphics.Rect;
import android.graphics.Region;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteCallback;
import android.os.RemoteException;
import android.os.Trace;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.MergedConfiguration;
import android.util.Slog;
import android.view.IWindow;
import android.view.IWindowId;
import android.view.IWindowSession;
import android.view.IWindowSessionCallback;
import android.view.InputChannel;
import android.view.InsetsSourceControl;
import android.view.InsetsState;
import android.view.InsetsVisibilities;
import android.view.SurfaceControl;
import android.view.SurfaceSession;
import android.view.WindowManager;
import android.window.ClientWindowFrames;
import android.window.OnBackInvokedCallbackInfo;
import com.android.internal.os.logging.MetricsLoggerWrapper;
import com.android.internal.protolog.ProtoLogGroup;
import com.android.internal.protolog.ProtoLogImpl;
import com.android.server.LocalServices;
import com.mediatek.server.wm.WmsExt;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class Session extends IWindowSession.Stub implements IBinder.DeathRecipient {
    private static final float DEFAULT_REFRESHRATE = 9999.0f;
    private AlertWindowNotification mAlertWindowNotification;
    final IWindowSessionCallback mCallback;
    final boolean mCanAddInternalSystemWindow;
    final boolean mCanCreateSystemApplicationOverlay;
    final boolean mCanHideNonSystemOverlayWindows;
    private final boolean mCanStartTasksFromRecents;
    private final DragDropController mDragDropController;
    private float mLastReportedAnimatorScale;
    private String mPackageName;
    final int mPid;
    private String mRelayoutTag;
    final WindowManagerService mService;
    final boolean mSetsUnrestrictedKeepClearAreas;
    private boolean mShowingAlertWindowNotificationAllowed;
    private final String mStringName;
    SurfaceSession mSurfaceSession;
    final int mUid;
    private String mUpdateViewVisibilityTag;
    private String mUpdateWindowLayoutTag;
    private int mNumWindow = 0;
    private final ArraySet<WindowSurfaceController> mAppOverlaySurfaces = new ArraySet<>();
    private final ArraySet<WindowSurfaceController> mAlertWindowSurfaces = new ArraySet<>();
    private boolean mClientDead = false;
    private final InsetsVisibilities mDummyRequestedVisibilities = new InsetsVisibilities();
    private final InsetsSourceControl[] mDummyControls = new InsetsSourceControl[0];

    public Session(WindowManagerService service, IWindowSessionCallback callback) {
        this.mService = service;
        this.mCallback = callback;
        int callingUid = Binder.getCallingUid();
        this.mUid = callingUid;
        int callingPid = Binder.getCallingPid();
        this.mPid = callingPid;
        this.mLastReportedAnimatorScale = service.getCurrentAnimatorScale();
        this.mCanAddInternalSystemWindow = service.mContext.checkCallingOrSelfPermission("android.permission.INTERNAL_SYSTEM_WINDOW") == 0;
        this.mCanHideNonSystemOverlayWindows = service.mContext.checkCallingOrSelfPermission("android.permission.HIDE_NON_SYSTEM_OVERLAY_WINDOWS") == 0 || service.mContext.checkCallingOrSelfPermission("android.permission.HIDE_OVERLAY_WINDOWS") == 0;
        this.mCanCreateSystemApplicationOverlay = service.mContext.checkCallingOrSelfPermission("android.permission.SYSTEM_APPLICATION_OVERLAY") == 0;
        this.mCanStartTasksFromRecents = service.mContext.checkCallingOrSelfPermission("android.permission.START_TASKS_FROM_RECENTS") == 0;
        this.mSetsUnrestrictedKeepClearAreas = service.mContext.checkCallingOrSelfPermission("android.permission.SET_UNRESTRICTED_KEEP_CLEAR_AREAS") == 0;
        this.mShowingAlertWindowNotificationAllowed = service.mShowAlertWindowNotifications;
        this.mDragDropController = service.mDragDropController;
        StringBuilder sb = new StringBuilder();
        sb.append("Session{");
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(" ");
        sb.append(callingPid);
        if (callingUid < 10000) {
            sb.append(":");
            sb.append(callingUid);
        } else {
            sb.append(":u");
            sb.append(UserHandle.getUserId(callingUid));
            sb.append('a');
            sb.append(UserHandle.getAppId(callingUid));
        }
        sb.append("}");
        this.mStringName = sb.toString();
        try {
            callback.asBinder().linkToDeath(this, 0);
        } catch (RemoteException e) {
        }
    }

    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        try {
            return super.onTransact(code, data, reply, flags);
        } catch (RuntimeException e) {
            if (!(e instanceof SecurityException)) {
                Slog.wtf(WmsExt.TAG, "Window Session Crash", e);
            }
            throw e;
        }
    }

    @Override // android.os.IBinder.DeathRecipient
    public void binderDied() {
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                this.mCallback.asBinder().unlinkToDeath(this, 0);
                this.mClientDead = true;
                killSessionLocked();
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public int addToDisplay(IWindow window, WindowManager.LayoutParams attrs, int viewVisibility, int displayId, InsetsVisibilities requestedVisibilities, InputChannel outInputChannel, InsetsState outInsetsState, InsetsSourceControl[] outActiveControls) {
        return this.mService.addWindow(this, window, attrs, viewVisibility, displayId, UserHandle.getUserId(this.mUid), requestedVisibilities, outInputChannel, outInsetsState, outActiveControls);
    }

    public int addToDisplayAsUser(IWindow window, WindowManager.LayoutParams attrs, int viewVisibility, int displayId, int userId, InsetsVisibilities requestedVisibilities, InputChannel outInputChannel, InsetsState outInsetsState, InsetsSourceControl[] outActiveControls) {
        return this.mService.addWindow(this, window, attrs, viewVisibility, displayId, userId, requestedVisibilities, outInputChannel, outInsetsState, outActiveControls);
    }

    public int addToDisplayWithoutInputChannel(IWindow window, WindowManager.LayoutParams attrs, int viewVisibility, int displayId, InsetsState outInsetsState) {
        return this.mService.addWindow(this, window, attrs, viewVisibility, displayId, UserHandle.getUserId(this.mUid), this.mDummyRequestedVisibilities, null, outInsetsState, this.mDummyControls);
    }

    public void remove(IWindow window) {
        this.mService.removeWindow(this, window);
    }

    public int updateVisibility(IWindow client, WindowManager.LayoutParams attrs, int viewVisibility, MergedConfiguration outMergedConfiguration, SurfaceControl outSurfaceControl, InsetsState outInsetsState, InsetsSourceControl[] outActiveControls) {
        Trace.traceBegin(32L, this.mUpdateViewVisibilityTag);
        int res = this.mService.updateViewVisibility(this, client, attrs, viewVisibility, outMergedConfiguration, outSurfaceControl, outInsetsState, outActiveControls);
        Trace.traceEnd(32L);
        return res;
    }

    public void updateLayout(IWindow window, WindowManager.LayoutParams attrs, int flags, ClientWindowFrames clientFrames, int requestedWidth, int requestedHeight) {
        Trace.traceBegin(32L, this.mUpdateWindowLayoutTag);
        this.mService.updateWindowLayout(this, window, attrs, flags, clientFrames, requestedWidth, requestedHeight);
        Trace.traceEnd(32L);
    }

    public void prepareToReplaceWindows(IBinder appToken, boolean childrenOnly) {
        this.mService.setWillReplaceWindows(appToken, childrenOnly);
    }

    public int relayout(IWindow window, WindowManager.LayoutParams attrs, int requestedWidth, int requestedHeight, int viewFlags, int flags, ClientWindowFrames outFrames, MergedConfiguration mergedConfiguration, SurfaceControl outSurfaceControl, InsetsState outInsetsState, InsetsSourceControl[] outActiveControls, Bundle outSyncSeqIdBundle) {
        Trace.traceBegin(32L, this.mRelayoutTag);
        int res = this.mService.relayoutWindow(this, window, attrs, requestedWidth, requestedHeight, viewFlags, flags, outFrames, mergedConfiguration, outSurfaceControl, outInsetsState, outActiveControls, outSyncSeqIdBundle);
        Trace.traceEnd(32L);
        return res;
    }

    public boolean outOfMemory(IWindow window) {
        return this.mService.outOfMemoryWindow(this, window);
    }

    public void setInsets(IWindow window, int touchableInsets, Rect contentInsets, Rect visibleInsets, Region touchableArea) {
        this.mService.setInsetsWindow(this, window, touchableInsets, contentInsets, visibleInsets, touchableArea);
    }

    public void clearTouchableRegion(IWindow window) {
        this.mService.clearTouchableRegion(this, window);
    }

    public void finishDrawing(IWindow window, SurfaceControl.Transaction postDrawTransaction, int seqId) {
        if (WindowManagerDebugConfig.DEBUG) {
            Slog.v(WmsExt.TAG, "IWindow finishDrawing called for " + window);
        }
        this.mService.finishDrawingWindow(this, window, postDrawTransaction, seqId);
    }

    public void setInTouchMode(boolean mode) {
        this.mService.setInTouchMode(mode);
    }

    public boolean getInTouchMode() {
        return this.mService.getInTouchMode();
    }

    public boolean performHapticFeedback(int effectId, boolean always) {
        long ident = Binder.clearCallingIdentity();
        try {
            return this.mService.mPolicy.performHapticFeedback(this.mUid, this.mPackageName, effectId, always, null);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public IBinder performDrag(IWindow window, int flags, SurfaceControl surface, int touchSource, float touchX, float touchY, float thumbCenterX, float thumbCenterY, ClipData data) {
        int callingUid = Binder.getCallingUid();
        int callingPid = Binder.getCallingPid();
        validateAndResolveDragMimeTypeExtras(data, callingUid, callingPid, this.mPackageName);
        validateDragFlags(flags, callingUid);
        long ident = Binder.clearCallingIdentity();
        try {
            return this.mDragDropController.performDrag(this.mPid, this.mUid, window, flags, surface, touchSource, touchX, touchY, thumbCenterX, thumbCenterY, data);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public boolean dropForAccessibility(IWindow window, int x, int y) {
        long ident = Binder.clearCallingIdentity();
        try {
            return this.mDragDropController.dropForAccessibility(window, x, y);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    void validateDragFlags(int flags, int callingUid) {
        if (callingUid == 1000) {
            throw new IllegalStateException("Need to validate before calling identify is cleared");
        }
        if ((flags & 2048) != 0 && !this.mCanStartTasksFromRecents) {
            throw new SecurityException("Requires START_TASKS_FROM_RECENTS permission");
        }
    }

    void validateAndResolveDragMimeTypeExtras(ClipData data, int callingUid, int callingPid, String callingPackage) {
        if (callingUid == 1000) {
            throw new IllegalStateException("Need to validate before calling identify is cleared");
        }
        ClipDescription desc = data != null ? data.getDescription() : null;
        if (desc == null) {
            return;
        }
        boolean hasActivity = desc.hasMimeType("application/vnd.android.activity");
        boolean hasShortcut = desc.hasMimeType("application/vnd.android.shortcut");
        boolean hasTask = desc.hasMimeType("application/vnd.android.task");
        int appMimeTypeCount = (hasActivity ? 1 : 0) + (hasShortcut ? 1 : 0) + (hasTask ? 1 : 0);
        if (appMimeTypeCount == 0) {
            return;
        }
        if (appMimeTypeCount > 1) {
            throw new IllegalArgumentException("Can not specify more than one of activity, shortcut, or task mime types");
        }
        if (data.getItemCount() == 0) {
            throw new IllegalArgumentException("Unexpected number of items (none)");
        }
        for (int i = 0; i < data.getItemCount(); i++) {
            if (data.getItemAt(i).getIntent() == null) {
                throw new IllegalArgumentException("Unexpected item, expected an intent");
            }
        }
        String str = "android.intent.extra.USER";
        if (hasActivity) {
            long origId = Binder.clearCallingIdentity();
            int i2 = 0;
            while (i2 < data.getItemCount()) {
                try {
                    ClipData.Item item = data.getItemAt(i2);
                    Intent intent = item.getIntent();
                    PendingIntent pi = (PendingIntent) intent.getParcelableExtra("android.intent.extra.PENDING_INTENT");
                    UserHandle user = (UserHandle) intent.getParcelableExtra("android.intent.extra.USER");
                    if (pi == null || user == null) {
                        throw new IllegalArgumentException("Clip data must include the pending intent to launch and its associated user to launch for.");
                    }
                    Intent launchIntent = this.mService.mAmInternal.getIntentForIntentSender(pi.getIntentSender().getTarget());
                    int appMimeTypeCount2 = appMimeTypeCount;
                    try {
                        ActivityInfo info = this.mService.mAtmService.resolveActivityInfoForIntent(launchIntent, null, user.getIdentifier(), callingUid);
                        item.setActivityInfo(info);
                        i2++;
                        appMimeTypeCount = appMimeTypeCount2;
                    } catch (Throwable th) {
                        th = th;
                    }
                    th = th;
                } catch (Throwable th2) {
                    th = th2;
                }
                Binder.restoreCallingIdentity(origId);
                throw th;
            }
            Binder.restoreCallingIdentity(origId);
        } else if (hasShortcut) {
            if (!this.mCanStartTasksFromRecents) {
                throw new SecurityException("Requires START_TASKS_FROM_RECENTS permission");
            } else {
                int i3 = 0;
                while (i3 < data.getItemCount()) {
                    ClipData.Item item2 = data.getItemAt(i3);
                    Intent intent2 = item2.getIntent();
                    String shortcutId = intent2.getStringExtra("android.intent.extra.shortcut.ID");
                    String packageName = intent2.getStringExtra("android.intent.extra.PACKAGE_NAME");
                    UserHandle user2 = (UserHandle) intent2.getParcelableExtra(str);
                    if (!TextUtils.isEmpty(shortcutId)) {
                        if (!TextUtils.isEmpty(packageName) && user2 != null) {
                            ShortcutServiceInternal shortcutService = (ShortcutServiceInternal) LocalServices.getService(ShortcutServiceInternal.class);
                            String str2 = str;
                            ClipDescription desc2 = desc;
                            Intent[] shortcutIntents = shortcutService.createShortcutIntents(UserHandle.getUserId(callingUid), callingPackage, packageName, shortcutId, user2.getIdentifier(), callingPid, callingUid);
                            if (shortcutIntents == null || shortcutIntents.length == 0) {
                                throw new IllegalArgumentException("Invalid shortcut id");
                            }
                            ActivityInfo info2 = this.mService.mAtmService.resolveActivityInfoForIntent(shortcutIntents[0], null, user2.getIdentifier(), callingUid);
                            item2.setActivityInfo(info2);
                            i3++;
                            str = str2;
                            desc = desc2;
                        }
                    }
                    throw new IllegalArgumentException("Clip item must include the package name, shortcut id, and the user to launch for.");
                }
            }
        } else {
            if (hasTask) {
                if (!this.mCanStartTasksFromRecents) {
                    throw new SecurityException("Requires START_TASKS_FROM_RECENTS permission");
                }
                for (int i4 = 0; i4 < data.getItemCount(); i4++) {
                    ClipData.Item item3 = data.getItemAt(i4);
                    int taskId = item3.getIntent().getIntExtra("android.intent.extra.TASK_ID", -1);
                    if (taskId == -1) {
                        throw new IllegalArgumentException("Clip item must include the task id.");
                    }
                    Task task = this.mService.mRoot.anyTaskForId(taskId);
                    if (task == null) {
                        throw new IllegalArgumentException("Invalid task id.");
                    }
                    if (task.getRootActivity() != null) {
                        item3.setActivityInfo(task.getRootActivity().info);
                    } else {
                        ActivityInfo info3 = this.mService.mAtmService.resolveActivityInfoForIntent(task.intent, null, task.mUserId, callingUid);
                        item3.setActivityInfo(info3);
                    }
                }
            }
        }
    }

    public void reportDropResult(IWindow window, boolean consumed) {
        long ident = Binder.clearCallingIdentity();
        try {
            this.mDragDropController.reportDropResult(window, consumed);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void cancelDragAndDrop(IBinder dragToken, boolean skipAnimation) {
        long ident = Binder.clearCallingIdentity();
        try {
            this.mDragDropController.cancelDragAndDrop(dragToken, skipAnimation);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void dragRecipientEntered(IWindow window) {
        this.mDragDropController.dragRecipientEntered(window);
    }

    public void dragRecipientExited(IWindow window) {
        this.mDragDropController.dragRecipientExited(window);
    }

    public boolean startMovingTask(IWindow window, float startX, float startY) {
        if (WindowManagerDebugConfig.DEBUG_TASK_POSITIONING) {
            Slog.d(WmsExt.TAG, "startMovingTask: {" + startX + "," + startY + "}");
        }
        long ident = Binder.clearCallingIdentity();
        try {
            return this.mService.mTaskPositioningController.startMovingTask(window, startX, startY);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void finishMovingTask(IWindow window) {
        if (WindowManagerDebugConfig.DEBUG_TASK_POSITIONING) {
            Slog.d(WmsExt.TAG, "finishMovingTask");
        }
        long ident = Binder.clearCallingIdentity();
        try {
            this.mService.mTaskPositioningController.finishTaskPositioning(window);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void reportSystemGestureExclusionChanged(IWindow window, List<Rect> exclusionRects) {
        long ident = Binder.clearCallingIdentity();
        try {
            this.mService.reportSystemGestureExclusionChanged(this, window, exclusionRects);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void reportKeepClearAreasChanged(IWindow window, List<Rect> restricted, List<Rect> unrestricted) {
        if (!this.mSetsUnrestrictedKeepClearAreas && !unrestricted.isEmpty()) {
            unrestricted = Collections.emptyList();
        }
        long ident = Binder.clearCallingIdentity();
        try {
            this.mService.reportKeepClearAreasChanged(this, window, restricted, unrestricted);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private void actionOnWallpaper(IBinder window, BiConsumer<WallpaperController, WindowState> action) {
        WindowState windowState = this.mService.windowForClientLocked(this, window, true);
        action.accept(windowState.getDisplayContent().mWallpaperController, windowState);
    }

    public void setWallpaperPosition(IBinder window, final float x, final float y, final float xStep, final float yStep) {
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                long ident = Binder.clearCallingIdentity();
                actionOnWallpaper(window, new BiConsumer() { // from class: com.android.server.wm.Session$$ExternalSyntheticLambda5
                    @Override // java.util.function.BiConsumer
                    public final void accept(Object obj, Object obj2) {
                        ((WallpaperController) obj).setWindowWallpaperPosition((WindowState) obj2, x, y, xStep, yStep);
                    }
                });
                Binder.restoreCallingIdentity(ident);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public void setWallpaperZoomOut(IBinder window, final float zoom) {
        if (Float.compare(0.0f, zoom) > 0 || Float.compare(1.0f, zoom) < 0 || Float.isNaN(zoom)) {
            throw new IllegalArgumentException("Zoom must be a valid float between 0 and 1: " + zoom);
        }
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                long ident = Binder.clearCallingIdentity();
                actionOnWallpaper(window, new BiConsumer() { // from class: com.android.server.wm.Session$$ExternalSyntheticLambda3
                    @Override // java.util.function.BiConsumer
                    public final void accept(Object obj, Object obj2) {
                        ((WallpaperController) obj).setWallpaperZoomOut((WindowState) obj2, zoom);
                    }
                });
                Binder.restoreCallingIdentity(ident);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public void setShouldZoomOutWallpaper(IBinder window, final boolean shouldZoom) {
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                actionOnWallpaper(window, new BiConsumer() { // from class: com.android.server.wm.Session$$ExternalSyntheticLambda1
                    @Override // java.util.function.BiConsumer
                    public final void accept(Object obj, Object obj2) {
                        ((WallpaperController) obj).setShouldZoomOutWallpaper((WindowState) obj2, shouldZoom);
                    }
                });
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public void wallpaperOffsetsComplete(final IBinder window) {
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                actionOnWallpaper(window, new BiConsumer() { // from class: com.android.server.wm.Session$$ExternalSyntheticLambda4
                    @Override // java.util.function.BiConsumer
                    public final void accept(Object obj, Object obj2) {
                        WindowState windowState = (WindowState) obj2;
                        ((WallpaperController) obj).wallpaperOffsetsComplete(window);
                    }
                });
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public void setWallpaperDisplayOffset(IBinder window, final int x, final int y) {
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                long ident = Binder.clearCallingIdentity();
                actionOnWallpaper(window, new BiConsumer() { // from class: com.android.server.wm.Session$$ExternalSyntheticLambda2
                    @Override // java.util.function.BiConsumer
                    public final void accept(Object obj, Object obj2) {
                        ((WallpaperController) obj).setWindowWallpaperDisplayOffset((WindowState) obj2, x, y);
                    }
                });
                Binder.restoreCallingIdentity(ident);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public Bundle sendWallpaperCommand(IBinder window, String action, int x, int y, int z, Bundle extras, boolean sync) {
        synchronized (this.mService.mGlobalLock) {
            try {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    long ident = Binder.clearCallingIdentity();
                    try {
                        try {
                            WindowState windowState = this.mService.windowForClientLocked(this, window, true);
                            Bundle sendWindowWallpaperCommand = windowState.getDisplayContent().mWallpaperController.sendWindowWallpaperCommand(windowState, action, x, y, z, extras, sync);
                            Binder.restoreCallingIdentity(ident);
                            WindowManagerService.resetPriorityAfterLockedSection();
                            return sendWindowWallpaperCommand;
                        } catch (Throwable th) {
                            th = th;
                            Binder.restoreCallingIdentity(ident);
                            throw th;
                        }
                    } catch (Throwable th2) {
                        th = th2;
                    }
                } catch (Throwable th3) {
                    th = th3;
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            } catch (Throwable th4) {
                th = th4;
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void wallpaperCommandComplete(final IBinder window, Bundle result) {
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                actionOnWallpaper(window, new BiConsumer() { // from class: com.android.server.wm.Session$$ExternalSyntheticLambda0
                    @Override // java.util.function.BiConsumer
                    public final void accept(Object obj, Object obj2) {
                        WindowState windowState = (WindowState) obj2;
                        ((WallpaperController) obj).wallpaperCommandComplete(window);
                    }
                });
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public void onRectangleOnScreenRequested(IBinder token, Rect rectangle) {
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                long identity = Binder.clearCallingIdentity();
                this.mService.onRectangleOnScreenRequested(token, rectangle);
                Binder.restoreCallingIdentity(identity);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public IWindowId getWindowId(IBinder window) {
        return this.mService.getWindowId(window);
    }

    public void pokeDrawLock(IBinder window) {
        long identity = Binder.clearCallingIdentity();
        try {
            this.mService.pokeDrawLock(this, window);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void updatePointerIcon(IWindow window) {
        long identity = Binder.clearCallingIdentity();
        try {
            this.mService.updatePointerIcon(window);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void updateTapExcludeRegion(IWindow window, Region region) {
        long identity = Binder.clearCallingIdentity();
        try {
            this.mService.updateTapExcludeRegion(window, region);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void updateRequestedVisibilities(IWindow window, InsetsVisibilities visibilities) {
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                WindowState windowState = this.mService.windowForClientLocked(this, window, false);
                if (windowState != null) {
                    windowState.setRequestedVisibilities(visibilities);
                    windowState.getDisplayContent().getInsetsPolicy().onInsetsModified(windowState);
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void windowAddedLocked() {
        if (this.mPackageName == null) {
            WindowProcessController wpc = this.mService.mAtmService.mProcessMap.getProcess(this.mPid);
            if (wpc != null) {
                this.mPackageName = wpc.mInfo.packageName;
                this.mRelayoutTag = "relayoutWindow: " + this.mPackageName;
                this.mUpdateViewVisibilityTag = "updateVisibility: " + this.mPackageName;
                this.mUpdateWindowLayoutTag = "updateLayout: " + this.mPackageName;
            } else {
                Slog.e(WmsExt.TAG, "Unknown process pid=" + this.mPid);
            }
        }
        if (this.mSurfaceSession == null) {
            if (WindowManagerDebugConfig.DEBUG) {
                Slog.v(WmsExt.TAG, "First window added to " + this + ", creating SurfaceSession");
            }
            this.mSurfaceSession = new SurfaceSession();
            if (ProtoLogCache.WM_SHOW_TRANSACTIONS_enabled) {
                String protoLogParam0 = String.valueOf(this.mSurfaceSession);
                ProtoLogImpl.i(ProtoLogGroup.WM_SHOW_TRANSACTIONS, 608694300, 0, (String) null, new Object[]{protoLogParam0});
            }
            this.mService.mSessions.add(this);
            if (this.mLastReportedAnimatorScale != this.mService.getCurrentAnimatorScale()) {
                this.mService.dispatchNewAnimatorScaleLocked(this);
            }
        }
        this.mNumWindow++;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void windowRemovedLocked() {
        this.mNumWindow--;
        killSessionLocked();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onWindowSurfaceVisibilityChanged(WindowSurfaceController surfaceController, boolean visible, int type) {
        boolean changed;
        boolean changed2;
        if (!WindowManager.LayoutParams.isSystemAlertWindowType(type)) {
            return;
        }
        if (!this.mCanAddInternalSystemWindow && !this.mCanCreateSystemApplicationOverlay) {
            if (visible) {
                changed2 = this.mAlertWindowSurfaces.add(surfaceController);
                MetricsLoggerWrapper.logAppOverlayEnter(this.mUid, this.mPackageName, changed2, type, true);
            } else {
                changed2 = this.mAlertWindowSurfaces.remove(surfaceController);
                MetricsLoggerWrapper.logAppOverlayExit(this.mUid, this.mPackageName, changed2, type, true);
            }
            if (changed2) {
                if (this.mAlertWindowSurfaces.isEmpty()) {
                    cancelAlertWindowNotification();
                } else if (this.mAlertWindowNotification == null) {
                    AlertWindowNotification alertWindowNotification = new AlertWindowNotification(this.mService, this.mPackageName);
                    this.mAlertWindowNotification = alertWindowNotification;
                    if (this.mShowingAlertWindowNotificationAllowed) {
                        alertWindowNotification.post();
                    }
                }
            }
        }
        if (type != 2038) {
            return;
        }
        if (visible) {
            changed = this.mAppOverlaySurfaces.add(surfaceController);
            MetricsLoggerWrapper.logAppOverlayEnter(this.mUid, this.mPackageName, changed, type, false);
        } else {
            changed = this.mAppOverlaySurfaces.remove(surfaceController);
            MetricsLoggerWrapper.logAppOverlayExit(this.mUid, this.mPackageName, changed, type, false);
        }
        if (changed) {
            setHasOverlayUi(!this.mAppOverlaySurfaces.isEmpty());
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setShowingAlertWindowNotificationAllowed(boolean allowed) {
        this.mShowingAlertWindowNotificationAllowed = allowed;
        AlertWindowNotification alertWindowNotification = this.mAlertWindowNotification;
        if (alertWindowNotification != null) {
            if (allowed) {
                alertWindowNotification.post();
            } else {
                alertWindowNotification.cancel(false);
            }
        }
    }

    private void killSessionLocked() {
        if (this.mNumWindow > 0 || !this.mClientDead) {
            return;
        }
        this.mService.mSessions.remove(this);
        if (this.mSurfaceSession == null) {
            return;
        }
        if (WindowManagerDebugConfig.DEBUG) {
            Slog.v(WmsExt.TAG, "Last window removed from " + this + ", destroying " + this.mSurfaceSession);
        }
        if (ProtoLogCache.WM_SHOW_TRANSACTIONS_enabled) {
            String protoLogParam0 = String.valueOf(this.mSurfaceSession);
            ProtoLogImpl.i(ProtoLogGroup.WM_SHOW_TRANSACTIONS, -86763148, 0, (String) null, new Object[]{protoLogParam0});
        }
        try {
            this.mSurfaceSession.kill();
        } catch (Exception e) {
            Slog.w(WmsExt.TAG, "Exception thrown when killing surface session " + this.mSurfaceSession + " in session " + this + ": " + e.toString());
        }
        this.mSurfaceSession = null;
        this.mAlertWindowSurfaces.clear();
        this.mAppOverlaySurfaces.clear();
        setHasOverlayUi(false);
        cancelAlertWindowNotification();
    }

    private void setHasOverlayUi(boolean hasOverlayUi) {
        this.mService.mH.obtainMessage(58, this.mPid, hasOverlayUi ? 1 : 0).sendToTarget();
    }

    private void cancelAlertWindowNotification() {
        AlertWindowNotification alertWindowNotification = this.mAlertWindowNotification;
        if (alertWindowNotification == null) {
            return;
        }
        alertWindowNotification.cancel(true);
        this.mAlertWindowNotification = null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(PrintWriter pw, String prefix) {
        pw.print(prefix);
        pw.print("mNumWindow=");
        pw.print(this.mNumWindow);
        pw.print(" mCanAddInternalSystemWindow=");
        pw.print(this.mCanAddInternalSystemWindow);
        pw.print(" mAppOverlaySurfaces=");
        pw.print(this.mAppOverlaySurfaces);
        pw.print(" mAlertWindowSurfaces=");
        pw.print(this.mAlertWindowSurfaces);
        pw.print(" mClientDead=");
        pw.print(this.mClientDead);
        pw.print(" mSurfaceSession=");
        pw.println(this.mSurfaceSession);
        pw.print(prefix);
        pw.print("mPackageName=");
        pw.println(this.mPackageName);
    }

    public String toString() {
        return this.mStringName;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasAlertWindowSurfaces(DisplayContent displayContent) {
        for (int i = this.mAlertWindowSurfaces.size() - 1; i >= 0; i--) {
            WindowSurfaceController surfaceController = this.mAlertWindowSurfaces.valueAt(i);
            if (surfaceController.mAnimator.mWin.getDisplayContent() == displayContent) {
                return true;
            }
        }
        return false;
    }

    public void grantInputChannel(int displayId, SurfaceControl surface, IWindow window, IBinder hostInputToken, int flags, int privateFlags, int type, IBinder focusGrantToken, String inputHandleName, InputChannel outInputChannel) {
        if (hostInputToken == null && !this.mCanAddInternalSystemWindow) {
            throw new SecurityException("Requires INTERNAL_SYSTEM_WINDOW permission");
        }
        if (!this.mCanAddInternalSystemWindow && type != 0) {
            Slog.w(WmsExt.TAG, "Requires INTERNAL_SYSTEM_WINDOW permission if assign type to input");
        }
        long identity = Binder.clearCallingIdentity();
        try {
            WindowManagerService windowManagerService = this.mService;
            int i = this.mUid;
            int i2 = this.mPid;
            boolean z = this.mCanAddInternalSystemWindow;
            windowManagerService.grantInputChannel(this, i, i2, displayId, surface, window, hostInputToken, flags, z ? privateFlags : 0, z ? type : 0, focusGrantToken, inputHandleName, outInputChannel);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void updateInputChannel(IBinder channelToken, int displayId, SurfaceControl surface, int flags, int privateFlags, Region region) {
        long identity = Binder.clearCallingIdentity();
        try {
            this.mService.updateInputChannel(channelToken, displayId, surface, flags, this.mCanAddInternalSystemWindow ? privateFlags : 0, region);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void grantEmbeddedWindowFocus(IWindow callingWindow, IBinder targetInputToken, boolean grantFocus) {
        long identity = Binder.clearCallingIdentity();
        try {
            if (callingWindow == null) {
                if (!this.mCanAddInternalSystemWindow) {
                    throw new SecurityException("Requires INTERNAL_SYSTEM_WINDOW permission");
                }
                this.mService.grantEmbeddedWindowFocus(this, targetInputToken, grantFocus);
            } else {
                this.mService.grantEmbeddedWindowFocus(this, callingWindow, targetInputToken, grantFocus);
            }
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void generateDisplayHash(IWindow window, Rect boundsInWindow, String hashAlgorithm, RemoteCallback callback) {
        long origId = Binder.clearCallingIdentity();
        try {
            this.mService.generateDisplayHash(this, window, boundsInWindow, hashAlgorithm, callback);
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    public void setOnBackInvokedCallbackInfo(IWindow window, OnBackInvokedCallbackInfo callbackInfo) {
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                WindowState windowState = this.mService.windowForClientLocked(this, window, false);
                if (windowState == null) {
                    Slog.e(WmsExt.TAG, "setOnBackInvokedCallback(): No window state for package:" + this.mPackageName);
                } else {
                    windowState.setOnBackInvokedCallbackInfo(callbackInfo);
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public void setRefreshRate(SurfaceControl sc, float refreshRate, int mMSyncScenarioAction, int mMSyncScenarioType, String activityName, String packgeName) {
        if (this.mService.isMsyncLogOn) {
            Slog.d("MSyncForWMS", "SurfaceControl: " + sc + ", refreshRate: " + refreshRate + ", Action: " + mMSyncScenarioAction + ", Type: " + mMSyncScenarioType + ", activityName: " + activityName + ", packgeName: " + packgeName);
        }
        if (sc == null) {
            Slog.d("MSyncForWMS", "mSurfaceControl == null");
        } else if (refreshRate != DEFAULT_REFRESHRATE) {
            Slog.d("MSyncForWMS", "RefreshRate: " + refreshRate);
            setFrameRefreshRate(sc, refreshRate);
        } else {
            float[] remoteArr = this.mService.getWmsExt().getRemoteRefreshRate(mMSyncScenarioType, mMSyncScenarioAction, packgeName, activityName);
            float mRefreshRate = remoteArr[0];
            float mCeiling = remoteArr[1];
            if (mRefreshRate == mCeiling) {
                this.mService.getWmsExt();
                if (mRefreshRate == 999.0f) {
                    return;
                }
            }
            Slog.d("MSyncForWMS", "RefreshRate: " + mRefreshRate + ", Ceiling: " + mCeiling);
            setFrameRefreshRate(sc, mRefreshRate);
            int displayID = this.mService.getDefaultDisplayContentLocked().getDisplayId();
            this.mService.mDisplayManagerInternal.setMsyncFps(displayID, 0.0f, mCeiling);
        }
    }

    private void setFrameRefreshRate(SurfaceControl sc, float refreshRate) {
        SurfaceControl.Transaction transaction = new SurfaceControl.Transaction();
        transaction.setFrameRate(sc, refreshRate, 100, 1);
        transaction.apply();
    }
}
