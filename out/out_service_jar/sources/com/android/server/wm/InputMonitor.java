package com.android.server.wm;

import android.app.ThunderbackConfig;
import android.graphics.Region;
import android.os.Handler;
import android.os.IBinder;
import android.os.Trace;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.EventLog;
import android.util.Slog;
import android.view.InputApplicationHandle;
import android.view.InputChannel;
import android.view.InputWindowHandle;
import android.view.SurfaceControl;
import com.android.internal.protolog.ProtoLogGroup;
import com.android.internal.protolog.ProtoLogImpl;
import com.mediatek.server.wm.WmsExt;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.Set;
import java.util.function.Consumer;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public final class InputMonitor {
    private boolean mDisableWallpaperTouchEvents;
    private final DisplayContent mDisplayContent;
    private int mDisplayHeight;
    private final int mDisplayId;
    private boolean mDisplayRemoved;
    private int mDisplayWidth;
    private final Handler mHandler;
    private final SurfaceControl.Transaction mInputTransaction;
    private final WindowManagerService mService;
    private boolean mUpdateInputWindowsImmediately;
    private boolean mUpdateInputWindowsPending;
    private IBinder mInputFocus = null;
    private boolean mUpdateInputWindowsNeeded = true;
    private final Region mTmpRegion = new Region();
    private final ArrayMap<String, InputConsumerImpl> mInputConsumers = new ArrayMap<>();
    private WeakReference<ActivityRecord> mActiveRecentsActivity = null;
    private WeakReference<ActivityRecord> mActiveRecentsLayerRef = null;
    private final UpdateInputWindows mUpdateInputWindows = new UpdateInputWindows();
    private final UpdateInputForAllWindowsConsumer mUpdateInputForAllWindowsConsumer = new UpdateInputForAllWindowsConsumer();

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class UpdateInputWindows implements Runnable {
        private UpdateInputWindows() {
        }

        @Override // java.lang.Runnable
        public void run() {
            synchronized (InputMonitor.this.mService.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    InputMonitor.this.mUpdateInputWindowsPending = false;
                    InputMonitor.this.mUpdateInputWindowsNeeded = false;
                    if (InputMonitor.this.mDisplayRemoved) {
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return;
                    }
                    boolean inDrag = InputMonitor.this.mService.mDragDropController.dragDropActiveLocked();
                    InputMonitor.this.mUpdateInputForAllWindowsConsumer.updateInputWindows(inDrag);
                    WindowManagerService.resetPriorityAfterLockedSection();
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public InputMonitor(WindowManagerService service, DisplayContent displayContent) {
        this.mService = service;
        this.mDisplayContent = displayContent;
        this.mDisplayId = displayContent.getDisplayId();
        this.mInputTransaction = service.mTransactionFactory.get();
        this.mHandler = service.mAnimationHandler;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onDisplayRemoved() {
        this.mHandler.removeCallbacks(this.mUpdateInputWindows);
        this.mHandler.post(new Runnable() { // from class: com.android.server.wm.InputMonitor$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                InputMonitor.this.m8041lambda$onDisplayRemoved$0$comandroidserverwmInputMonitor();
            }
        });
        this.mDisplayRemoved = true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onDisplayRemoved$0$com-android-server-wm-InputMonitor  reason: not valid java name */
    public /* synthetic */ void m8041lambda$onDisplayRemoved$0$comandroidserverwmInputMonitor() {
        this.mService.mTransactionFactory.get().syncInputWindows().apply();
        this.mService.mInputManager.onDisplayRemoved(this.mDisplayId);
    }

    private void addInputConsumer(String name, InputConsumerImpl consumer) {
        this.mInputConsumers.put(name, consumer);
        consumer.linkToDeathRecipient();
        consumer.layout(this.mInputTransaction, this.mDisplayWidth, this.mDisplayHeight);
        updateInputWindowsLw(true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean destroyInputConsumer(String name) {
        if (disposeInputConsumer(this.mInputConsumers.remove(name))) {
            updateInputWindowsLw(true);
            return true;
        }
        return false;
    }

    private boolean disposeInputConsumer(InputConsumerImpl consumer) {
        if (consumer != null) {
            consumer.disposeChannelsLw(this.mInputTransaction);
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public InputConsumerImpl getInputConsumer(String name) {
        return this.mInputConsumers.get(name);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void layoutInputConsumers(int dw, int dh) {
        if (this.mDisplayWidth == dw && this.mDisplayHeight == dh) {
            return;
        }
        this.mDisplayWidth = dw;
        this.mDisplayHeight = dh;
        try {
            Trace.traceBegin(32L, "layoutInputConsumer");
            for (int i = this.mInputConsumers.size() - 1; i >= 0; i--) {
                this.mInputConsumers.valueAt(i).layout(this.mInputTransaction, dw, dh);
            }
        } finally {
            Trace.traceEnd(32L);
        }
    }

    void resetInputConsumers(SurfaceControl.Transaction t) {
        for (int i = this.mInputConsumers.size() - 1; i >= 0; i--) {
            this.mInputConsumers.valueAt(i).hide(t);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void createInputConsumer(IBinder token, String name, InputChannel inputChannel, int clientPid, UserHandle clientUser) {
        if (this.mInputConsumers.containsKey(name)) {
            throw new IllegalStateException("Existing input consumer found with name: " + name + ", display: " + this.mDisplayId);
        }
        InputConsumerImpl consumer = new InputConsumerImpl(this.mService, token, name, inputChannel, clientPid, clientUser, this.mDisplayId);
        char c = 65535;
        switch (name.hashCode()) {
            case -1525776435:
                if (name.equals("recents_animation_input_consumer")) {
                    c = 2;
                    break;
                }
                break;
            case 1024719987:
                if (name.equals("pip_input_consumer")) {
                    c = 1;
                    break;
                }
                break;
            case 1415830696:
                if (name.equals("wallpaper_input_consumer")) {
                    c = 0;
                    break;
                }
                break;
        }
        switch (c) {
            case 0:
                consumer.mWindowHandle.inputConfig |= 32;
                break;
            case 1:
                break;
            case 2:
                consumer.mWindowHandle.inputConfig &= -5;
                break;
            default:
                throw new IllegalArgumentException("Illegal input consumer : " + name + ", display: " + this.mDisplayId);
        }
        addInputConsumer(name, consumer);
    }

    void populateInputWindowHandle(InputWindowHandleWrapper inputWindowHandle, WindowState w) {
        boolean isThundbackWindow = false;
        inputWindowHandle.setInputApplicationHandle(w.mActivityRecord != null ? w.mActivityRecord.getInputApplicationHandle(false) : null);
        inputWindowHandle.setToken(w.mInputChannelToken);
        inputWindowHandle.setDispatchingTimeoutMillis(w.getInputDispatchingTimeoutMillis());
        inputWindowHandle.setTouchOcclusionMode(w.getTouchOcclusionMode());
        inputWindowHandle.setPaused(w.mActivityRecord != null && w.mActivityRecord.paused);
        inputWindowHandle.setWindowToken(w.mClient);
        inputWindowHandle.setName(w.getName());
        int flags = w.mAttrs.flags;
        if (w.mAttrs.isModal()) {
            flags |= 32;
        }
        inputWindowHandle.setLayoutParamsFlags(flags);
        inputWindowHandle.setInputConfigMasked(InputConfigAdapter.getInputConfigFromWindowParams(w.mAttrs.type, flags, w.mAttrs.inputFeatures), InputConfigAdapter.getMask());
        boolean focusable = w.canReceiveKeys() && (this.mService.mPerDisplayFocusEnabled || this.mDisplayContent.isOnTop());
        inputWindowHandle.setFocusable(focusable);
        boolean hasWallpaper = (!this.mDisplayContent.mWallpaperController.isWallpaperTarget(w) || this.mService.mPolicy.isKeyguardShowing() || this.mDisableWallpaperTouchEvents) ? false : true;
        inputWindowHandle.setHasWallpaper(hasWallpaper);
        inputWindowHandle.setSurfaceInset(w.mAttrs.surfaceInsets.left);
        inputWindowHandle.setScaleFactor(w.mGlobalScale != 1.0f ? 1.0f / w.mGlobalScale : 1.0f);
        boolean useSurfaceBoundsAsTouchRegion = false;
        SurfaceControl touchableRegionCrop = null;
        Task task = w.getTask();
        if (task != null) {
            if (ThunderbackConfig.isVersion4() && task.getConfiguration().windowConfiguration.isThunderbackWindow()) {
                isThundbackWindow = true;
            }
            if (task.isOrganized() && task.getWindowingMode() != 1 && !task.inFreeformWindowingMode()) {
                useSurfaceBoundsAsTouchRegion = true;
                if (w.mAttrs.isModal()) {
                    TaskFragment parent = w.getTaskFragment();
                    touchableRegionCrop = parent != null ? parent.getSurfaceControl() : null;
                }
            } else if (task.cropWindowsToRootTaskBounds() && !w.inFreeformWindowingMode() && !isThundbackWindow) {
                touchableRegionCrop = task.getRootTask().getSurfaceControl();
            }
        }
        if (w.isOSFullDialog()) {
            inputWindowHandle.setReplaceTouchableRegionWithCrop(true);
            inputWindowHandle.setTouchableRegionCrop(task.getSurfaceControl());
        } else {
            inputWindowHandle.setReplaceTouchableRegionWithCrop(useSurfaceBoundsAsTouchRegion);
            inputWindowHandle.setTouchableRegionCrop(touchableRegionCrop);
        }
        if (!useSurfaceBoundsAsTouchRegion) {
            w.getSurfaceTouchableRegion(this.mTmpRegion, w.mAttrs);
            inputWindowHandle.setTouchableRegion(this.mTmpRegion);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setUpdateInputWindowsNeededLw() {
        this.mUpdateInputWindowsNeeded = true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateInputWindowsLw(boolean force) {
        if (!force && !this.mUpdateInputWindowsNeeded) {
            return;
        }
        scheduleUpdateInputWindows();
    }

    private void scheduleUpdateInputWindows() {
        if (!this.mDisplayRemoved && !this.mUpdateInputWindowsPending) {
            this.mUpdateInputWindowsPending = true;
            this.mHandler.post(this.mUpdateInputWindows);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateInputWindowsImmediately(SurfaceControl.Transaction t) {
        this.mHandler.removeCallbacks(this.mUpdateInputWindows);
        this.mUpdateInputWindowsImmediately = true;
        this.mUpdateInputWindows.run();
        this.mUpdateInputWindowsImmediately = false;
        t.merge(this.mInputTransaction);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setInputFocusLw(WindowState newWindow, boolean updateInputWindows) {
        if (ProtoLogCache.WM_DEBUG_FOCUS_LIGHT_enabled) {
            String protoLogParam0 = String.valueOf(newWindow);
            long protoLogParam1 = this.mDisplayId;
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_FOCUS_LIGHT, -1438175584, 4, (String) null, new Object[]{protoLogParam0, Long.valueOf(protoLogParam1)});
        }
        IBinder focus = newWindow != null ? newWindow.mInputChannelToken : null;
        if (focus == this.mInputFocus) {
            return;
        }
        if (newWindow != null && newWindow.canReceiveKeys()) {
            newWindow.mToken.paused = false;
        }
        setUpdateInputWindowsNeededLw();
        if (updateInputWindows) {
            updateInputWindowsLw(false);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setActiveRecents(ActivityRecord activity, ActivityRecord layer) {
        boolean clear = activity == null;
        this.mActiveRecentsActivity = clear ? null : new WeakReference<>(activity);
        this.mActiveRecentsLayerRef = clear ? null : new WeakReference<>(layer);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static <T> T getWeak(WeakReference<T> ref) {
        if (ref != null) {
            return ref.get();
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateInputFocusRequest(InputConsumerImpl recentsAnimationInputConsumer) {
        WindowState focus = this.mDisplayContent.mCurrentFocus;
        if (recentsAnimationInputConsumer != null && focus != null) {
            RecentsAnimationController recentsAnimationController = this.mService.getRecentsAnimationController();
            boolean shouldApplyRecentsInputConsumer = (recentsAnimationController != null && recentsAnimationController.shouldApplyInputConsumer(focus.mActivityRecord)) || (getWeak(this.mActiveRecentsActivity) != null && focus.inTransition());
            if (shouldApplyRecentsInputConsumer) {
                requestFocus(recentsAnimationInputConsumer.mWindowHandle.token, recentsAnimationInputConsumer.mName);
                return;
            }
        }
        IBinder focusToken = focus != null ? focus.mInputChannelToken : null;
        if (focusToken == null) {
            if (recentsAnimationInputConsumer != null && recentsAnimationInputConsumer.mWindowHandle != null && this.mInputFocus == recentsAnimationInputConsumer.mWindowHandle.token) {
                return;
            }
            if (this.mDisplayContent.mFocusedApp != null && this.mInputFocus != null) {
                if (ProtoLogCache.WM_DEBUG_FOCUS_LIGHT_enabled) {
                    String protoLogParam0 = String.valueOf(this.mDisplayContent.mFocusedApp.getName());
                    ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_FOCUS_LIGHT, 2001473656, 0, (String) null, new Object[]{protoLogParam0});
                }
                EventLog.writeEvent(62001, "Requesting to set focus to null window", "reason=UpdateInputWindows");
                this.mInputTransaction.removeCurrentInputFocus(this.mDisplayId);
            }
            this.mInputFocus = null;
        } else if (!focus.mWinAnimator.hasSurface() || !focus.mInputWindowHandle.isFocusable()) {
            if (ProtoLogCache.WM_DEBUG_FOCUS_LIGHT_enabled) {
                String protoLogParam02 = String.valueOf(focus);
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_FOCUS_LIGHT, -760764543, 0, (String) null, new Object[]{protoLogParam02});
            }
            this.mInputFocus = null;
        } else {
            requestFocus(focusToken, focus.getName());
        }
    }

    private void requestFocus(IBinder focusToken, String windowName) {
        if (focusToken == this.mInputFocus) {
            return;
        }
        this.mInputFocus = focusToken;
        this.mInputTransaction.setFocusedWindow(focusToken, windowName, this.mDisplayId);
        EventLog.writeEvent(62001, "Focus request " + windowName, "reason=UpdateInputWindows");
        if (ProtoLogCache.WM_DEBUG_FOCUS_LIGHT_enabled) {
            String protoLogParam0 = String.valueOf(windowName);
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_FOCUS_LIGHT, 155482615, 0, (String) null, new Object[]{protoLogParam0});
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setFocusedAppLw(ActivityRecord newApp) {
        this.mService.mInputManager.setFocusedApplication(this.mDisplayId, newApp != null ? newApp.getInputApplicationHandle(true) : null);
    }

    public void pauseDispatchingLw(WindowToken window) {
        if (!window.paused) {
            if (WindowManagerDebugConfig.DEBUG_INPUT) {
                Slog.v(WmsExt.TAG, "Pausing WindowToken " + window);
            }
            window.paused = true;
            updateInputWindowsLw(true);
        }
    }

    public void resumeDispatchingLw(WindowToken window) {
        if (window.paused) {
            if (WindowManagerDebugConfig.DEBUG_INPUT) {
                Slog.v(WmsExt.TAG, "Resuming WindowToken " + window);
            }
            window.paused = false;
            updateInputWindowsLw(true);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(PrintWriter pw, String prefix) {
        Set<String> inputConsumerKeys = this.mInputConsumers.keySet();
        if (!inputConsumerKeys.isEmpty()) {
            pw.println(prefix + "InputConsumers:");
            for (String key : inputConsumerKeys) {
                this.mInputConsumers.get(key).dump(pw, key, prefix);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public final class UpdateInputForAllWindowsConsumer implements Consumer<WindowState> {
        private boolean mAddPipInputConsumerHandle;
        private boolean mAddRecentsAnimationInputConsumerHandle;
        private boolean mAddWallpaperInputConsumerHandle;
        boolean mInDrag;
        InputConsumerImpl mPipInputConsumer;
        InputConsumerImpl mRecentsAnimationInputConsumer;
        InputConsumerImpl mWallpaperInputConsumer;

        private UpdateInputForAllWindowsConsumer() {
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* JADX WARN: Multi-variable type inference failed */
        public void updateInputWindows(boolean inDrag) {
            Trace.traceBegin(32L, "updateInputWindows");
            this.mPipInputConsumer = InputMonitor.this.getInputConsumer("pip_input_consumer");
            this.mWallpaperInputConsumer = InputMonitor.this.getInputConsumer("wallpaper_input_consumer");
            InputConsumerImpl inputConsumer = InputMonitor.this.getInputConsumer("recents_animation_input_consumer");
            this.mRecentsAnimationInputConsumer = inputConsumer;
            this.mAddPipInputConsumerHandle = this.mPipInputConsumer != null;
            this.mAddWallpaperInputConsumerHandle = this.mWallpaperInputConsumer != null;
            this.mAddRecentsAnimationInputConsumerHandle = inputConsumer != null;
            InputMonitor.this.mDisableWallpaperTouchEvents = false;
            this.mInDrag = inDrag;
            InputMonitor inputMonitor = InputMonitor.this;
            inputMonitor.resetInputConsumers(inputMonitor.mInputTransaction);
            ActivityRecord activeRecents = (ActivityRecord) InputMonitor.getWeak(InputMonitor.this.mActiveRecentsActivity);
            if (this.mAddRecentsAnimationInputConsumerHandle && activeRecents != null && activeRecents.getSurfaceControl() != null) {
                WindowContainer layer = (WindowContainer) InputMonitor.getWeak(InputMonitor.this.mActiveRecentsLayerRef);
                WindowContainer layer2 = layer != null ? layer : activeRecents;
                if (layer2.getSurfaceControl() != null) {
                    this.mRecentsAnimationInputConsumer.mWindowHandle.replaceTouchableRegionWithCrop(layer2.getSurfaceControl());
                    this.mRecentsAnimationInputConsumer.show(InputMonitor.this.mInputTransaction, layer2);
                    this.mAddRecentsAnimationInputConsumerHandle = false;
                }
            }
            InputMonitor.this.mDisplayContent.forAllWindows((Consumer<WindowState>) this, true);
            InputMonitor.this.updateInputFocusRequest(this.mRecentsAnimationInputConsumer);
            if (!InputMonitor.this.mUpdateInputWindowsImmediately) {
                InputMonitor.this.mDisplayContent.getPendingTransaction().merge(InputMonitor.this.mInputTransaction);
                InputMonitor.this.mDisplayContent.scheduleAnimation();
            }
            Trace.traceEnd(32L);
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // java.util.function.Consumer
        public void accept(WindowState w) {
            DisplayArea targetDA;
            InputWindowHandleWrapper inputWindowHandle = w.mInputWindowHandle;
            if (w.mInputChannelToken == null || w.mRemoved || !w.canReceiveTouchInput()) {
                if (w.mWinAnimator.hasSurface()) {
                    InputMonitor.populateOverlayInputInfo(inputWindowHandle, w);
                    InputMonitor.setInputWindowInfoIfNeeded(InputMonitor.this.mInputTransaction, w.mWinAnimator.mSurfaceController.mSurfaceControl, inputWindowHandle);
                    return;
                }
                return;
            }
            int privateFlags = w.mAttrs.privateFlags;
            RecentsAnimationController recentsAnimationController = InputMonitor.this.mService.getRecentsAnimationController();
            boolean shouldApplyRecentsInputConsumer = recentsAnimationController != null && recentsAnimationController.shouldApplyInputConsumer(w.mActivityRecord);
            if (this.mAddRecentsAnimationInputConsumerHandle && shouldApplyRecentsInputConsumer && recentsAnimationController.updateInputConsumerForApp(this.mRecentsAnimationInputConsumer.mWindowHandle) && (targetDA = recentsAnimationController.getTargetAppDisplayArea()) != null) {
                this.mRecentsAnimationInputConsumer.reparent(InputMonitor.this.mInputTransaction, targetDA);
                this.mRecentsAnimationInputConsumer.show(InputMonitor.this.mInputTransaction, 2147483645);
                this.mAddRecentsAnimationInputConsumerHandle = false;
            }
            if (w.inPinnedWindowingMode() && this.mAddPipInputConsumerHandle) {
                Task rootTask = w.getTask().getRootTask();
                this.mPipInputConsumer.mWindowHandle.replaceTouchableRegionWithCrop(rootTask.getSurfaceControl());
                DisplayArea targetDA2 = rootTask.getDisplayArea();
                if (targetDA2 != null) {
                    this.mPipInputConsumer.layout(InputMonitor.this.mInputTransaction, rootTask.getBounds());
                    this.mPipInputConsumer.reparent(InputMonitor.this.mInputTransaction, targetDA2);
                    this.mPipInputConsumer.show(InputMonitor.this.mInputTransaction, 2147483646);
                    this.mAddPipInputConsumerHandle = false;
                }
            }
            if (this.mAddWallpaperInputConsumerHandle && w.mAttrs.type == 2013 && w.isVisible()) {
                this.mWallpaperInputConsumer.mWindowHandle.replaceTouchableRegionWithCrop((SurfaceControl) null);
                this.mWallpaperInputConsumer.show(InputMonitor.this.mInputTransaction, w);
                this.mAddWallpaperInputConsumerHandle = false;
            }
            if ((privateFlags & 2048) != 0) {
                InputMonitor.this.mDisableWallpaperTouchEvents = true;
            }
            if (this.mInDrag && w.isVisible() && w.getDisplayContent().isDefaultDisplay) {
                InputMonitor.this.mService.mDragDropController.sendDragStartedIfNeededLocked(w);
            }
            InputMonitor.this.mService.mKeyInterceptionInfoForToken.put(w.mInputChannelToken, w.getKeyInterceptionInfo());
            if (w.mWinAnimator.hasSurface()) {
                InputMonitor.this.populateInputWindowHandle(inputWindowHandle, w);
                InputMonitor.setInputWindowInfoIfNeeded(InputMonitor.this.mInputTransaction, w.mWinAnimator.mSurfaceController.mSurfaceControl, inputWindowHandle);
            }
        }
    }

    static void setInputWindowInfoIfNeeded(SurfaceControl.Transaction t, SurfaceControl sc, InputWindowHandleWrapper inputWindowHandle) {
        if (WindowManagerDebugConfig.DEBUG_INPUT) {
            Slog.d(WmsExt.TAG, "Update InputWindowHandle: " + inputWindowHandle);
        }
        if (inputWindowHandle.isChanged()) {
            inputWindowHandle.applyChangesToSurface(t, sc);
        }
    }

    static void populateOverlayInputInfo(InputWindowHandleWrapper inputWindowHandle, WindowState w) {
        populateOverlayInputInfo(inputWindowHandle);
        inputWindowHandle.setTouchOcclusionMode(w.getTouchOcclusionMode());
    }

    static void populateOverlayInputInfo(InputWindowHandleWrapper inputWindowHandle) {
        inputWindowHandle.setDispatchingTimeoutMillis(0L);
        inputWindowHandle.setFocusable(false);
        inputWindowHandle.setToken(null);
        inputWindowHandle.setScaleFactor(1.0f);
        inputWindowHandle.setLayoutParamsType(2);
        inputWindowHandle.setInputConfigMasked(InputConfigAdapter.getInputConfigFromWindowParams(2, 16, 1), InputConfigAdapter.getMask());
        inputWindowHandle.clearTouchableRegion();
        inputWindowHandle.setTouchableRegionCrop(null);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void setTrustedOverlayInputInfo(SurfaceControl sc, SurfaceControl.Transaction t, int displayId, String name) {
        InputWindowHandleWrapper inputWindowHandle = new InputWindowHandleWrapper(new InputWindowHandle((InputApplicationHandle) null, displayId));
        inputWindowHandle.setName(name);
        inputWindowHandle.setLayoutParamsType(2015);
        inputWindowHandle.setTrustedOverlay(true);
        populateOverlayInputInfo(inputWindowHandle);
        setInputWindowInfoIfNeeded(t, sc, inputWindowHandle);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean isTrustedOverlay(int type) {
        return type == 2039 || type == 2011 || type == 2012 || type == 2027 || type == 2000 || type == 2040 || type == 2019 || type == 2024 || type == 2015 || type == 2034 || type == 2032 || type == 2022 || type == 2031 || type == 2041;
    }
}
