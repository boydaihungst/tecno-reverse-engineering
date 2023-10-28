package com.android.server.wm;

import android.graphics.Point;
import android.graphics.Rect;
import android.os.Binder;
import android.os.IBinder;
import android.os.InputConstants;
import android.os.Process;
import android.os.RemoteException;
import android.os.Trace;
import android.util.DisplayMetrics;
import android.util.Slog;
import android.view.BatchedInputEventReceiver;
import android.view.InputApplicationHandle;
import android.view.InputChannel;
import android.view.InputEvent;
import android.view.InputEventReceiver;
import android.view.InputWindowHandle;
import android.view.MotionEvent;
import com.android.internal.policy.TaskResizingAlgorithm;
import com.android.internal.protolog.ProtoLogGroup;
import com.android.internal.protolog.ProtoLogImpl;
import com.transsion.hubcore.server.wm.ITranWindowManagerService;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class TaskPositioner implements IBinder.DeathRecipient {
    private static final boolean DEBUG_ORIENTATION_VIOLATIONS = false;
    public static final float RESIZING_HINT_ALPHA = 0.5f;
    public static final int RESIZING_HINT_DURATION_MS = 0;
    private static final String TAG = "WindowManager";
    private static final String TAG_LOCAL = "TaskPositioner";
    private static Factory sFactory;
    IBinder mClientCallback;
    InputChannel mClientChannel;
    private DisplayContent mDisplayContent;
    InputApplicationHandle mDragApplicationHandle;
    boolean mDragEnded;
    InputWindowHandle mDragWindowHandle;
    private InputEventReceiver mInputEventReceiver;
    private int mMinVisibleHeight;
    private int mMinVisibleWidth;
    private boolean mPreserveOrientation;
    private boolean mResizing;
    private final WindowManagerService mService;
    private float mStartDragX;
    private float mStartDragY;
    private boolean mStartOrientationWasLandscape;
    Task mTask;
    WindowState mWindow;
    private Rect mTmpRect = new Rect();
    private final Rect mWindowOriginalBounds = new Rect();
    private final Rect mWindowDragBounds = new Rect();
    private final Point mMaxVisibleSize = new Point();
    private int mCtrlType = 0;

    TaskPositioner(WindowManagerService service) {
        this.mService = service;
    }

    /* JADX DEBUG: Another duplicated slice has different insns count: {[]}, finally: {[INVOKE] complete} */
    /* JADX INFO: Access modifiers changed from: private */
    public boolean onInputEvent(InputEvent event) {
        ITranWindowManagerService.Instance().onTaskPositionerReceiveInputEvent(this, event);
        if (!(event instanceof MotionEvent) || (event.getSource() & 2) == 0) {
            return false;
        }
        MotionEvent motionEvent = (MotionEvent) event;
        if (this.mDragEnded) {
            return true;
        }
        float newX = motionEvent.getRawX();
        float newY = motionEvent.getRawY();
        switch (motionEvent.getAction()) {
            case 0:
                if (WindowManagerDebugConfig.DEBUG_TASK_POSITIONING) {
                    Slog.w("WindowManager", "ACTION_DOWN @ {" + newX + ", " + newY + "}");
                    break;
                }
                break;
            case 1:
                if (WindowManagerDebugConfig.DEBUG_TASK_POSITIONING) {
                    Slog.w("WindowManager", "ACTION_UP @ {" + newX + ", " + newY + "}");
                }
                this.mDragEnded = true;
                break;
            case 2:
                if (WindowManagerDebugConfig.DEBUG_TASK_POSITIONING) {
                    Slog.w("WindowManager", "ACTION_MOVE @ {" + newX + ", " + newY + "}");
                }
                synchronized (this.mService.mGlobalLock) {
                    try {
                        WindowManagerService.boostPriorityForLockedSection();
                        this.mDragEnded = notifyMoveLocked(newX, newY);
                        this.mTask.getDimBounds(this.mTmpRect);
                    } finally {
                    }
                }
                WindowManagerService.resetPriorityAfterLockedSection();
                if (!this.mTmpRect.equals(this.mWindowDragBounds)) {
                    Trace.traceBegin(32L, "wm.TaskPositioner.resizeTask");
                    this.mService.mAtmService.resizeTask(this.mTask.mTaskId, this.mWindowDragBounds, 1);
                    Trace.traceEnd(32L);
                    break;
                }
                break;
            case 3:
                if (WindowManagerDebugConfig.DEBUG_TASK_POSITIONING) {
                    Slog.w("WindowManager", "ACTION_CANCEL @ {" + newX + ", " + newY + "}");
                }
                this.mDragEnded = true;
                break;
        }
        if (this.mDragEnded) {
            boolean wasResizing = this.mResizing;
            synchronized (this.mService.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    endDragLocked();
                    this.mTask.getDimBounds(this.mTmpRect);
                } finally {
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
            if (wasResizing && !this.mTmpRect.equals(this.mWindowDragBounds)) {
                this.mService.mAtmService.resizeTask(this.mTask.mTaskId, this.mWindowDragBounds, 3);
            }
            this.mService.mTaskPositioningController.finishTaskPositioning();
        }
        return true;
    }

    Rect getWindowDragBounds() {
        return this.mWindowDragBounds;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void register(DisplayContent displayContent, WindowState win) {
        if (WindowManagerDebugConfig.DEBUG_TASK_POSITIONING) {
            Slog.d("WindowManager", "Registering task positioner");
        }
        if (this.mClientChannel != null) {
            Slog.e("WindowManager", "Task positioner already registered");
            return;
        }
        this.mDisplayContent = displayContent;
        this.mClientChannel = this.mService.mInputManager.createInputChannel("WindowManager");
        this.mInputEventReceiver = new BatchedInputEventReceiver.SimpleBatchedInputEventReceiver(this.mClientChannel, this.mService.mAnimationHandler.getLooper(), this.mService.mAnimator.getChoreographer(), new BatchedInputEventReceiver.SimpleBatchedInputEventReceiver.InputEventListener() { // from class: com.android.server.wm.TaskPositioner$$ExternalSyntheticLambda1
            public final boolean onInputEvent(InputEvent inputEvent) {
                boolean onInputEvent;
                onInputEvent = TaskPositioner.this.onInputEvent(inputEvent);
                return onInputEvent;
            }
        });
        this.mDragApplicationHandle = new InputApplicationHandle(new Binder(), "WindowManager", InputConstants.DEFAULT_DISPATCHING_TIMEOUT_MILLIS);
        InputWindowHandle inputWindowHandle = new InputWindowHandle(this.mDragApplicationHandle, displayContent.getDisplayId());
        this.mDragWindowHandle = inputWindowHandle;
        inputWindowHandle.name = "WindowManager";
        this.mDragWindowHandle.token = this.mClientChannel.getToken();
        this.mDragWindowHandle.layoutParamsType = 2016;
        this.mDragWindowHandle.dispatchingTimeoutMillis = InputConstants.DEFAULT_DISPATCHING_TIMEOUT_MILLIS;
        this.mDragWindowHandle.ownerPid = Process.myPid();
        this.mDragWindowHandle.ownerUid = Process.myUid();
        this.mDragWindowHandle.scaleFactor = 1.0f;
        this.mDragWindowHandle.inputConfig = 4;
        this.mDragWindowHandle.touchableRegion.setEmpty();
        if (ProtoLogCache.WM_DEBUG_ORIENTATION_enabled) {
            ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_ORIENTATION, 791468751, 0, (String) null, (Object[]) null);
        }
        this.mDisplayContent.getDisplayRotation().pause();
        this.mService.mTaskPositioningController.showInputSurface(win.getDisplayId());
        Rect displayBounds = this.mTmpRect;
        displayContent.getBounds(displayBounds);
        DisplayMetrics displayMetrics = displayContent.getDisplayMetrics();
        this.mMinVisibleWidth = WindowManagerService.dipToPixel(48, displayMetrics);
        this.mMinVisibleHeight = WindowManagerService.dipToPixel(32, displayMetrics);
        this.mMaxVisibleSize.set(displayBounds.width(), displayBounds.height());
        this.mDragEnded = false;
        try {
            IBinder asBinder = win.mClient.asBinder();
            this.mClientCallback = asBinder;
            asBinder.linkToDeath(this, 0);
            this.mWindow = win;
            this.mTask = win.getTask();
        } catch (RemoteException e) {
            this.mService.mTaskPositioningController.finishTaskPositioning();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void unregister() {
        if (WindowManagerDebugConfig.DEBUG_TASK_POSITIONING) {
            Slog.d("WindowManager", "Unregistering task positioner");
        }
        if (this.mClientChannel == null) {
            Slog.e("WindowManager", "Task positioner not registered");
            return;
        }
        this.mService.mTaskPositioningController.hideInputSurface(this.mDisplayContent.getDisplayId());
        this.mService.mInputManager.removeInputChannel(this.mClientChannel.getToken());
        this.mInputEventReceiver.dispose();
        this.mInputEventReceiver = null;
        this.mClientChannel.dispose();
        this.mClientChannel = null;
        this.mDragWindowHandle = null;
        this.mDragApplicationHandle = null;
        this.mDragEnded = true;
        this.mDisplayContent.getInputMonitor().updateInputWindowsLw(true);
        if (ProtoLogCache.WM_DEBUG_ORIENTATION_enabled) {
            ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_ORIENTATION, 1422781269, 0, (String) null, (Object[]) null);
        }
        this.mDisplayContent.getDisplayRotation().resume();
        this.mDisplayContent = null;
        IBinder iBinder = this.mClientCallback;
        if (iBinder != null) {
            iBinder.unlinkToDeath(this, 0);
        }
        this.mWindow = null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void startDrag(boolean resize, boolean preserveOrientation, float startX, float startY) {
        boolean z;
        if (WindowManagerDebugConfig.DEBUG_TASK_POSITIONING) {
            Slog.d("WindowManager", "startDrag: win=" + this.mWindow + ", resize=" + resize + ", preserveOrientation=" + preserveOrientation + ", {" + startX + ", " + startY + "}");
        }
        final Rect startBounds = this.mTmpRect;
        this.mTask.getBounds(startBounds);
        boolean z2 = false;
        this.mCtrlType = 0;
        this.mStartDragX = startX;
        this.mStartDragY = startY;
        this.mPreserveOrientation = preserveOrientation;
        if (resize) {
            if (startX < startBounds.left) {
                this.mCtrlType |= 1;
            }
            if (startX > startBounds.right) {
                this.mCtrlType |= 2;
            }
            if (startY < startBounds.top) {
                this.mCtrlType |= 4;
            }
            if (startY > startBounds.bottom) {
                this.mCtrlType |= 8;
            }
            if (this.mCtrlType == 0) {
                z = false;
            } else {
                z = true;
            }
            this.mResizing = z;
        }
        if (startBounds.width() >= startBounds.height()) {
            z2 = true;
        }
        this.mStartOrientationWasLandscape = z2;
        this.mWindowOriginalBounds.set(startBounds);
        if (this.mResizing) {
            notifyMoveLocked(startX, startY);
            this.mService.mH.post(new Runnable() { // from class: com.android.server.wm.TaskPositioner$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    TaskPositioner.this.m8381lambda$startDrag$0$comandroidserverwmTaskPositioner(startBounds);
                }
            });
        }
        this.mWindowDragBounds.set(startBounds);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$startDrag$0$com-android-server-wm-TaskPositioner  reason: not valid java name */
    public /* synthetic */ void m8381lambda$startDrag$0$comandroidserverwmTaskPositioner(Rect startBounds) {
        this.mService.mAtmService.resizeTask(this.mTask.mTaskId, startBounds, 3);
    }

    private void endDragLocked() {
        this.mResizing = false;
        this.mTask.setDragResizing(false, 0);
    }

    boolean notifyMoveLocked(float x, float y) {
        if (WindowManagerDebugConfig.DEBUG_TASK_POSITIONING) {
            Slog.d("WindowManager", "notifyMoveLocked: {" + x + "," + y + "}");
        }
        if (this.mCtrlType != 0) {
            resizeDrag(x, y);
            this.mTask.setDragResizing(true, 0);
            return false;
        }
        this.mDisplayContent.getStableRect(this.mTmpRect);
        this.mTmpRect.intersect(this.mTask.getRootTask().getParent().getBounds());
        int nX = (int) x;
        int nY = (int) y;
        if (!this.mTmpRect.contains(nX, nY)) {
            nX = Math.min(Math.max(nX, this.mTmpRect.left), this.mTmpRect.right);
            nY = Math.min(Math.max(nY, this.mTmpRect.top), this.mTmpRect.bottom);
        }
        updateWindowDragBounds(nX, nY, this.mTmpRect);
        return false;
    }

    void resizeDrag(float x, float y) {
        updateDraggedBounds(TaskResizingAlgorithm.resizeDrag(x, y, this.mStartDragX, this.mStartDragY, this.mWindowOriginalBounds, this.mCtrlType, this.mMinVisibleWidth, this.mMinVisibleHeight, this.mMaxVisibleSize, this.mPreserveOrientation, this.mStartOrientationWasLandscape));
    }

    private void updateDraggedBounds(Rect newBounds) {
        this.mWindowDragBounds.set(newBounds);
        checkBoundsForOrientationViolations(this.mWindowDragBounds);
    }

    private void checkBoundsForOrientationViolations(Rect bounds) {
    }

    private void updateWindowDragBounds(int x, int y, Rect rootTaskBounds) {
        int offsetX = Math.round(x - this.mStartDragX);
        int offsetY = Math.round(y - this.mStartDragY);
        this.mWindowDragBounds.set(this.mWindowOriginalBounds);
        int maxLeft = rootTaskBounds.right - this.mMinVisibleWidth;
        int minLeft = (rootTaskBounds.left + this.mMinVisibleWidth) - this.mWindowOriginalBounds.width();
        int minTop = rootTaskBounds.top;
        int maxTop = rootTaskBounds.bottom - this.mMinVisibleHeight;
        this.mWindowDragBounds.offsetTo(Math.min(Math.max(this.mWindowOriginalBounds.left + offsetX, minLeft), maxLeft), Math.min(Math.max(this.mWindowOriginalBounds.top + offsetY, minTop), maxTop));
        if (WindowManagerDebugConfig.DEBUG_TASK_POSITIONING) {
            Slog.d("WindowManager", "updateWindowDragBounds: " + this.mWindowDragBounds);
        }
    }

    public String toShortString() {
        return "WindowManager";
    }

    static void setFactory(Factory factory) {
        sFactory = factory;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static TaskPositioner create(WindowManagerService service) {
        if (sFactory == null) {
            sFactory = new Factory() { // from class: com.android.server.wm.TaskPositioner.1
            };
        }
        return sFactory.create(service);
    }

    @Override // android.os.IBinder.DeathRecipient
    public void binderDied() {
        this.mService.mTaskPositioningController.finishTaskPositioning();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public interface Factory {
        default TaskPositioner create(WindowManagerService service) {
            return new TaskPositioner(service);
        }
    }
}
