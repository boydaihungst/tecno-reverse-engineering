package com.android.server.wm;

import android.app.ActivityManager;
import android.app.ActivityThread;
import android.app.ContextImpl;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.RecordingCanvas;
import android.graphics.Rect;
import android.graphics.RenderNode;
import android.hardware.HardwareBuffer;
import android.os.Environment;
import android.os.Handler;
import android.util.ArraySet;
import android.util.Pair;
import android.util.Slog;
import android.view.InsetsState;
import android.view.SurfaceControl;
import android.view.ThreadedRenderer;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.window.TaskSnapshot;
import com.android.internal.graphics.ColorUtils;
import com.android.internal.policy.DecorView;
import com.android.internal.util.ToBooleanFunction;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.wm.TaskSnapshotPersister;
import com.android.server.wm.utils.InsetUtils;
import com.google.android.collect.Sets;
import java.io.File;
import java.io.PrintWriter;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class TaskSnapshotController {
    static final int SNAPSHOT_MODE_APP_THEME = 1;
    static final int SNAPSHOT_MODE_NONE = 2;
    static final int SNAPSHOT_MODE_REAL = 0;
    private static final String TAG = "WindowManager";
    private final TaskSnapshotCache mCache;
    private final float mHighResTaskSnapshotScale;
    private final boolean mIsRunningOnIoT;
    private final boolean mIsRunningOnTv;
    private final TaskSnapshotLoader mLoader;
    private final TaskSnapshotPersister mPersister;
    private final WindowManagerService mService;
    private boolean mTaskSnapshotEnabled;
    private final ArraySet<Task> mSkipClosingAppSnapshotTasks = new ArraySet<>();
    private final ArraySet<Task> mTmpTasks = new ArraySet<>();
    private final Handler mHandler = new Handler();
    private final Rect mTmpRect = new Rect();

    /* JADX INFO: Access modifiers changed from: package-private */
    public TaskSnapshotController(WindowManagerService service) {
        this.mService = service;
        TaskSnapshotPersister taskSnapshotPersister = new TaskSnapshotPersister(service, new TaskSnapshotPersister.DirectoryResolver() { // from class: com.android.server.wm.TaskSnapshotController$$ExternalSyntheticLambda4
            @Override // com.android.server.wm.TaskSnapshotPersister.DirectoryResolver
            public final File getSystemDirectoryForUser(int i) {
                return Environment.getDataSystemCeDirectory(i);
            }
        });
        this.mPersister = taskSnapshotPersister;
        TaskSnapshotLoader taskSnapshotLoader = new TaskSnapshotLoader(taskSnapshotPersister);
        this.mLoader = taskSnapshotLoader;
        this.mCache = new TaskSnapshotCache(service, taskSnapshotLoader);
        this.mIsRunningOnTv = service.mContext.getPackageManager().hasSystemFeature("android.software.leanback");
        this.mIsRunningOnIoT = service.mContext.getPackageManager().hasSystemFeature("android.hardware.type.embedded");
        this.mHighResTaskSnapshotScale = service.mContext.getResources().getFloat(17105079);
        this.mTaskSnapshotEnabled = !service.mContext.getResources().getBoolean(17891597);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void systemReady() {
        this.mPersister.start();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onTransitionStarting(DisplayContent displayContent) {
        handleClosingApps(displayContent.mClosingApps);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyAppVisibilityChanged(ActivityRecord appWindowToken, boolean visible) {
        if (!visible) {
            handleClosingApps(Sets.newArraySet(new ActivityRecord[]{appWindowToken}));
        }
    }

    private void handleClosingApps(ArraySet<ActivityRecord> closingApps) {
        if (shouldDisableSnapshots()) {
            return;
        }
        getClosingTasks(closingApps, this.mTmpTasks);
        snapshotTasks(this.mTmpTasks);
        this.mSkipClosingAppSnapshotTasks.clear();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void addSkipClosingAppSnapshotTasks(Set<Task> tasks) {
        if (shouldDisableSnapshots()) {
            return;
        }
        this.mSkipClosingAppSnapshotTasks.addAll(tasks);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void snapshotTasks(ArraySet<Task> tasks) {
        snapshotTasks(tasks, false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public TaskSnapshot captureTaskSnapshot(Task task, boolean snapshotHome) {
        if (snapshotHome) {
            TaskSnapshot snapshot = snapshotTask(task);
            return snapshot;
        }
        switch (getSnapshotMode(task)) {
            case 0:
                TaskSnapshot snapshot2 = snapshotTask(task);
                return snapshot2;
            case 1:
                TaskSnapshot snapshot3 = drawAppThemeSnapshot(task);
                return snapshot3;
            case 2:
                return null;
            default:
                return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void recordTaskSnapshot(Task task, boolean allowSnapshotHome) {
        boolean snapshotHome = allowSnapshotHome && task.isActivityTypeHome();
        TaskSnapshot snapshot = captureTaskSnapshot(task, snapshotHome);
        if (snapshot == null) {
            return;
        }
        HardwareBuffer buffer = snapshot.getHardwareBuffer();
        if (buffer.getWidth() == 0 || buffer.getHeight() == 0) {
            buffer.close();
            Slog.e("WindowManager", "Invalid task snapshot dimensions " + buffer.getWidth() + "x" + buffer.getHeight());
            return;
        }
        this.mCache.putSnapshot(task, snapshot);
        if (!snapshotHome) {
            this.mPersister.persistSnapshot(task.mTaskId, task.mUserId, snapshot);
            task.onSnapshotChanged(snapshot);
        }
    }

    private void snapshotTasks(ArraySet<Task> tasks, boolean allowSnapshotHome) {
        for (int i = tasks.size() - 1; i >= 0; i--) {
            recordTaskSnapshot(tasks.valueAt(i), allowSnapshotHome);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public TaskSnapshot getSnapshot(int taskId, int userId, boolean restoreFromDisk, boolean isLowResolution) {
        return this.mCache.getSnapshot(taskId, userId, restoreFromDisk, isLowResolution && this.mPersister.enableLowResSnapshots());
    }

    public void clearSnapshotCache() {
        this.mCache.clearRunningCache();
    }

    private ActivityRecord findAppTokenForSnapshot(Task task) {
        return task.getActivity(new Predicate() { // from class: com.android.server.wm.TaskSnapshotController$$ExternalSyntheticLambda3
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return TaskSnapshotController.lambda$findAppTokenForSnapshot$1((ActivityRecord) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$findAppTokenForSnapshot$1(ActivityRecord r) {
        if (r == null || !r.isSurfaceShowing() || r.findMainWindow() == null) {
            return false;
        }
        return r.forAllWindows(new ToBooleanFunction() { // from class: com.android.server.wm.TaskSnapshotController$$ExternalSyntheticLambda2
            public final boolean apply(Object obj) {
                return TaskSnapshotController.lambda$findAppTokenForSnapshot$0((WindowState) obj);
            }
        }, true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$findAppTokenForSnapshot$0(WindowState ws) {
        return ws.mWinAnimator != null && ws.mWinAnimator.getShown() && ws.mWinAnimator.mLastAlpha > 0.0f;
    }

    boolean prepareTaskSnapshot(Task task, int pixelFormat, TaskSnapshot.Builder builder) {
        boolean isWindowTranslucent;
        int i;
        Pair<ActivityRecord, WindowState> result = checkIfReadyToSnapshot(task);
        boolean isTranslucent = false;
        if (result == null) {
            return false;
        }
        ActivityRecord activity = (ActivityRecord) result.first;
        WindowState mainWindow = (WindowState) result.second;
        Rect contentInsets = getSystemBarInsets(mainWindow.getFrame(), mainWindow.getInsetsStateWithVisibilityOverride());
        Rect letterboxInsets = activity.getLetterboxInsets();
        InsetUtils.addInsets(contentInsets, letterboxInsets);
        builder.setIsRealSnapshot(true);
        builder.setId(System.currentTimeMillis());
        builder.setContentInsets(contentInsets);
        builder.setLetterboxInsets(letterboxInsets);
        if (mainWindow.getAttrs().format == -1) {
            isWindowTranslucent = false;
        } else {
            isWindowTranslucent = true;
        }
        boolean isShowWallpaper = mainWindow.hasWallpaper();
        if (pixelFormat == 0) {
            if (this.mPersister.use16BitFormat() && activity.fillsParent() && (!isWindowTranslucent || !isShowWallpaper)) {
                i = 4;
            } else {
                i = 1;
            }
            pixelFormat = i;
        }
        if (PixelFormat.formatHasAlpha(pixelFormat) && (!activity.fillsParent() || isWindowTranslucent)) {
            isTranslucent = true;
        }
        builder.setTopActivityComponent(activity.mActivityComponent);
        builder.setPixelFormat(pixelFormat);
        builder.setIsTranslucent(isTranslucent);
        builder.setOrientation(activity.getTask().getConfiguration().orientation);
        builder.setRotation(activity.getTask().getDisplayContent().getRotation());
        builder.setWindowingMode(task.getWindowingMode());
        builder.setAppearance(getAppearance(task));
        return true;
    }

    Pair<ActivityRecord, WindowState> checkIfReadyToSnapshot(Task task) {
        if (!this.mService.mPolicy.isScreenOn()) {
            if (WindowManagerDebugConfig.DEBUG_SCREENSHOT) {
                Slog.i("WindowManager", "Attempted to take screenshot while display was off.");
            }
            return null;
        }
        ActivityRecord activity = findAppTokenForSnapshot(task);
        if (activity == null) {
            if (WindowManagerDebugConfig.DEBUG_SCREENSHOT) {
                Slog.w("WindowManager", "Failed to take screenshot. No visible windows for " + task);
            }
            return null;
        } else if (activity.hasCommittedReparentToAnimationLeash()) {
            if (WindowManagerDebugConfig.DEBUG_SCREENSHOT) {
                Slog.w("WindowManager", "Failed to take screenshot. App is animating " + activity);
            }
            return null;
        } else {
            WindowState mainWindow = activity.findMainWindow();
            if (mainWindow == null) {
                Slog.w("WindowManager", "Failed to take screenshot. No main window for " + task);
                return null;
            } else if (activity.hasFixedRotationTransform()) {
                if (WindowManagerDebugConfig.DEBUG_SCREENSHOT) {
                    Slog.i("WindowManager", "Skip taking screenshot. App has fixed rotation " + activity);
                }
                return null;
            } else {
                return new Pair<>(activity, mainWindow);
            }
        }
    }

    SurfaceControl.ScreenshotHardwareBuffer createTaskSnapshot(Task task, TaskSnapshot.Builder builder) {
        Point taskSize = new Point();
        SurfaceControl.ScreenshotHardwareBuffer taskSnapshot = createTaskSnapshot(task, this.mHighResTaskSnapshotScale, builder.getPixelFormat(), taskSize, builder);
        builder.setTaskSize(taskSize);
        return taskSnapshot;
    }

    private SurfaceControl.ScreenshotHardwareBuffer createImeSnapshot(Task task, int pixelFormat) {
        if (task.getSurfaceControl() == null) {
            if (WindowManagerDebugConfig.DEBUG_SCREENSHOT) {
                Slog.w("WindowManager", "Failed to take screenshot. No surface control for " + task);
            }
            return null;
        }
        WindowState imeWindow = task.getDisplayContent().mInputMethodWindow;
        if (imeWindow == null || !imeWindow.isWinVisibleLw() || (imeWindow.getAttrs().flags & 8192) != 0) {
            return null;
        }
        Rect bounds = imeWindow.getParentFrame();
        bounds.offsetTo(0, 0);
        SurfaceControl.ScreenshotHardwareBuffer imeBuffer = SurfaceControl.captureLayersExcluding(imeWindow.getSurfaceControl(), bounds, 1.0f, pixelFormat, null);
        return imeBuffer;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public SurfaceControl.ScreenshotHardwareBuffer snapshotImeFromAttachedTask(Task task) {
        int pixelFormat;
        if (checkIfReadyToSnapshot(task) == null) {
            return null;
        }
        if (this.mPersister.use16BitFormat()) {
            pixelFormat = 4;
        } else {
            pixelFormat = 1;
        }
        return createImeSnapshot(task, pixelFormat);
    }

    SurfaceControl.ScreenshotHardwareBuffer createTaskSnapshot(Task task, float scaleFraction, int pixelFormat, Point outTaskSize, TaskSnapshot.Builder builder) {
        SurfaceControl[] excludeLayers;
        if (task.getSurfaceControl() == null) {
            if (WindowManagerDebugConfig.DEBUG_SCREENSHOT) {
                Slog.w("WindowManager", "Failed to take screenshot. No surface control for " + task);
            }
            return null;
        }
        task.getBounds(this.mTmpRect);
        boolean z = false;
        this.mTmpRect.offsetTo(0, 0);
        WindowState imeWindow = task.getDisplayContent().mInputMethodWindow;
        boolean excludeIme = (imeWindow == null || imeWindow.getSurfaceControl() == null || task.getDisplayContent().shouldImeAttachedToApp()) ? false : true;
        WindowState navWindow = task.getDisplayContent().getDisplayPolicy().getNavigationBar();
        boolean excludeNavBar = navWindow != null;
        if (excludeIme && excludeNavBar) {
            excludeLayers = new SurfaceControl[]{imeWindow.getSurfaceControl(), navWindow.getSurfaceControl()};
        } else if (excludeIme || excludeNavBar) {
            excludeLayers = new SurfaceControl[1];
            excludeLayers[0] = excludeIme ? imeWindow.getSurfaceControl() : navWindow.getSurfaceControl();
        } else {
            excludeLayers = new SurfaceControl[0];
        }
        if (!excludeIme && imeWindow != null && imeWindow.isVisible()) {
            z = true;
        }
        builder.setHasImeSurface(z);
        SurfaceControl.ScreenshotHardwareBuffer screenshotBuffer = SurfaceControl.captureLayersExcluding(task.getSurfaceControl(), this.mTmpRect, scaleFraction, pixelFormat, excludeLayers);
        if (outTaskSize != null) {
            outTaskSize.x = this.mTmpRect.width();
            outTaskSize.y = this.mTmpRect.height();
        }
        HardwareBuffer buffer = screenshotBuffer == null ? null : screenshotBuffer.getHardwareBuffer();
        if (isInvalidHardwareBuffer(buffer)) {
            return null;
        }
        return screenshotBuffer;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean isInvalidHardwareBuffer(HardwareBuffer buffer) {
        return buffer == null || buffer.isClosed() || buffer.getWidth() <= 1 || buffer.getHeight() <= 1;
    }

    TaskSnapshot snapshotTask(Task task) {
        return snapshotTask(task, 0);
    }

    TaskSnapshot snapshotTask(Task task, int pixelFormat) {
        SurfaceControl.ScreenshotHardwareBuffer screenshotBuffer;
        TaskSnapshot.Builder builder = new TaskSnapshot.Builder();
        if (prepareTaskSnapshot(task, pixelFormat, builder) && (screenshotBuffer = createTaskSnapshot(task, builder)) != null) {
            builder.setSnapshot(screenshotBuffer.getHardwareBuffer());
            builder.setColorSpace(screenshotBuffer.getColorSpace());
            return builder.build();
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setTaskSnapshotEnabled(boolean enabled) {
        this.mTaskSnapshotEnabled = enabled;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean shouldDisableSnapshots() {
        return this.mIsRunningOnTv || this.mIsRunningOnIoT || !this.mTaskSnapshotEnabled;
    }

    void getClosingTasks(ArraySet<ActivityRecord> closingApps, ArraySet<Task> outClosingTasks) {
        outClosingTasks.clear();
        for (int i = closingApps.size() - 1; i >= 0; i--) {
            ActivityRecord activity = closingApps.valueAt(i);
            Task task = activity.getTask();
            if (task != null) {
                if (isAnimatingByRecents(task)) {
                    this.mSkipClosingAppSnapshotTasks.add(task);
                }
                if (!task.isVisible() && !this.mSkipClosingAppSnapshotTasks.contains(task)) {
                    outClosingTasks.add(task);
                }
            }
        }
    }

    int getSnapshotMode(Task task) {
        ActivityRecord topChild = task.getTopMostActivity();
        if (!task.isActivityTypeStandardOrUndefined() && !task.isActivityTypeAssistant()) {
            return 2;
        }
        if (topChild != null && topChild.shouldUseAppThemeSnapshot()) {
            return 1;
        }
        return 0;
    }

    private TaskSnapshot drawAppThemeSnapshot(Task task) {
        WindowState mainWindow;
        ActivityRecord topChild = task.getTopMostActivity();
        if (topChild == null || (mainWindow = topChild.findMainWindow()) == null) {
            return null;
        }
        int color = ColorUtils.setAlphaComponent(task.getTaskDescription().getBackgroundColor(), 255);
        WindowManager.LayoutParams attrs = mainWindow.getAttrs();
        Rect taskBounds = task.getBounds();
        InsetsState insetsState = mainWindow.getInsetsStateWithVisibilityOverride();
        Rect systemBarInsets = getSystemBarInsets(mainWindow.getFrame(), insetsState);
        SystemBarBackgroundPainter decorPainter = new SystemBarBackgroundPainter(attrs.flags, attrs.privateFlags, attrs.insetsFlags.appearance, task.getTaskDescription(), this.mHighResTaskSnapshotScale, insetsState);
        int taskWidth = taskBounds.width();
        int taskHeight = taskBounds.height();
        float f = this.mHighResTaskSnapshotScale;
        int width = (int) (taskWidth * f);
        int height = (int) (taskHeight * f);
        RenderNode node = RenderNode.create("TaskSnapshotController", null);
        node.setLeftTopRightBottom(0, 0, width, height);
        node.setClipToBounds(false);
        RecordingCanvas c = node.start(width, height);
        c.drawColor(color);
        decorPainter.setInsets(systemBarInsets);
        decorPainter.drawDecors(c);
        node.end(c);
        Bitmap hwBitmap = ThreadedRenderer.createHardwareBitmap(node, width, height);
        if (hwBitmap == null) {
            return null;
        }
        Rect contentInsets = new Rect(systemBarInsets);
        Rect letterboxInsets = topChild.getLetterboxInsets();
        InsetUtils.addInsets(contentInsets, letterboxInsets);
        return new TaskSnapshot(System.currentTimeMillis(), topChild.mActivityComponent, hwBitmap.getHardwareBuffer(), hwBitmap.getColorSpace(), mainWindow.getConfiguration().orientation, mainWindow.getWindowConfiguration().getRotation(), new Point(taskWidth, taskHeight), contentInsets, letterboxInsets, false, false, task.getWindowingMode(), getAppearance(task), false, false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onAppRemoved(ActivityRecord activity) {
        this.mCache.onAppRemoved(activity);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onAppDied(ActivityRecord activity) {
        this.mCache.onAppDied(activity);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyTaskRemovedFromRecents(int taskId, int userId) {
        this.mCache.onTaskRemoved(taskId);
        this.mPersister.onTaskRemovedFromRecents(taskId, userId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeSnapshotCache(int taskId) {
        this.mCache.removeRunningEntry(taskId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeObsoleteTaskFiles(ArraySet<Integer> persistentTaskIds, int[] runningUserIds) {
        this.mPersister.removeObsoleteFiles(persistentTaskIds, runningUserIds);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setPersisterPaused(boolean paused) {
        this.mPersister.setPaused(paused);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void screenTurningOff(final int displayId, final WindowManagerPolicy.ScreenOffListener listener, boolean isTransition) {
        if (shouldDisableSnapshots() || isTransition) {
            listener.onScreenOff();
        } else {
            this.mHandler.post(new Runnable() { // from class: com.android.server.wm.TaskSnapshotController$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    TaskSnapshotController.this.m8384xf2b627f(displayId, listener);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$screenTurningOff$2$com-android-server-wm-TaskSnapshotController  reason: not valid java name */
    public /* synthetic */ void m8384xf2b627f(int displayId, WindowManagerPolicy.ScreenOffListener listener) {
        try {
            synchronized (this.mService.mGlobalLock) {
                WindowManagerService.boostPriorityForLockedSection();
                snapshotForSleeping(displayId);
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        } finally {
            listener.onScreenOff();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void snapshotForSleeping(int displayId) {
        DisplayContent displayContent;
        if (shouldDisableSnapshots() || !this.mService.mDisplayEnabled || (displayContent = this.mService.mRoot.getDisplayContent(displayId)) == null) {
            return;
        }
        this.mTmpTasks.clear();
        displayContent.forAllTasks(new Consumer() { // from class: com.android.server.wm.TaskSnapshotController$$ExternalSyntheticLambda1
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                TaskSnapshotController.this.m8385x8efba8fe((Task) obj);
            }
        });
        boolean allowSnapshotHome = displayId == 0 && this.mService.mPolicy.isKeyguardSecure(this.mService.mCurrentUserId);
        snapshotTasks(this.mTmpTasks, allowSnapshotHome);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$snapshotForSleeping$3$com-android-server-wm-TaskSnapshotController  reason: not valid java name */
    public /* synthetic */ void m8385x8efba8fe(Task task) {
        if (task.isVisible() && !isAnimatingByRecents(task)) {
            this.mTmpTasks.add(task);
        }
    }

    private int getAppearance(Task task) {
        WindowState topFullscreenOpaqueWindow;
        ActivityRecord topFullscreenActivity = task.getTopFullscreenActivity();
        if (topFullscreenActivity != null) {
            topFullscreenOpaqueWindow = topFullscreenActivity.getTopFullscreenOpaqueWindow();
        } else {
            topFullscreenOpaqueWindow = null;
        }
        if (topFullscreenOpaqueWindow != null) {
            return topFullscreenOpaqueWindow.mAttrs.insetsFlags.appearance;
        }
        return 0;
    }

    static Rect getSystemBarInsets(Rect frame, InsetsState state) {
        return state.calculateInsets(frame, WindowInsets.Type.systemBars(), false).toRect();
    }

    private boolean isAnimatingByRecents(Task task) {
        return task.isAnimatingByRecents() || this.mService.mAtmService.getTransitionController().inRecentsTransition(task);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(PrintWriter pw, String prefix) {
        pw.println(prefix + "mHighResTaskSnapshotScale=" + this.mHighResTaskSnapshotScale);
        pw.println(prefix + "mTaskSnapshotEnabled=" + this.mTaskSnapshotEnabled);
        this.mCache.dump(pw, prefix);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static class SystemBarBackgroundPainter {
        private final InsetsState mInsetsState;
        private final int mNavigationBarColor;
        private final Paint mNavigationBarPaint;
        private final float mScale;
        private final int mStatusBarColor;
        private final Paint mStatusBarPaint;
        private final Rect mSystemBarInsets;
        private final int mWindowFlags;
        private final int mWindowPrivateFlags;

        SystemBarBackgroundPainter(int windowFlags, int windowPrivateFlags, int appearance, ActivityManager.TaskDescription taskDescription, float scale, InsetsState insetsState) {
            Paint paint = new Paint();
            this.mStatusBarPaint = paint;
            Paint paint2 = new Paint();
            this.mNavigationBarPaint = paint2;
            this.mSystemBarInsets = new Rect();
            this.mWindowFlags = windowFlags;
            this.mWindowPrivateFlags = windowPrivateFlags;
            this.mScale = scale;
            ContextImpl systemUiContext = ActivityThread.currentActivityThread().getSystemUiContext();
            int semiTransparent = systemUiContext.getColor(17171117);
            int calculateBarColor = DecorView.calculateBarColor(windowFlags, 67108864, semiTransparent, taskDescription.getStatusBarColor(), appearance, 8, taskDescription.getEnsureStatusBarContrastWhenTransparent());
            this.mStatusBarColor = calculateBarColor;
            int calculateBarColor2 = DecorView.calculateBarColor(windowFlags, 134217728, semiTransparent, taskDescription.getNavigationBarColor(), appearance, 16, taskDescription.getEnsureNavigationBarContrastWhenTransparent() && systemUiContext.getResources().getBoolean(17891710));
            this.mNavigationBarColor = calculateBarColor2;
            paint.setColor(calculateBarColor);
            paint2.setColor(calculateBarColor2);
            this.mInsetsState = insetsState;
        }

        void setInsets(Rect systemBarInsets) {
            this.mSystemBarInsets.set(systemBarInsets);
        }

        int getStatusBarColorViewHeight() {
            boolean forceBarBackground = (this.mWindowPrivateFlags & 131072) != 0;
            if (DecorView.STATUS_BAR_COLOR_VIEW_ATTRIBUTES.isVisible(this.mInsetsState, this.mStatusBarColor, this.mWindowFlags, forceBarBackground)) {
                return (int) (this.mSystemBarInsets.top * this.mScale);
            }
            return 0;
        }

        private boolean isNavigationBarColorViewVisible() {
            boolean forceBarBackground = (this.mWindowPrivateFlags & 131072) != 0;
            return DecorView.NAVIGATION_BAR_COLOR_VIEW_ATTRIBUTES.isVisible(this.mInsetsState, this.mNavigationBarColor, this.mWindowFlags, forceBarBackground);
        }

        void drawDecors(Canvas c) {
            drawStatusBarBackground(c, getStatusBarColorViewHeight());
            drawNavigationBarBackground(c);
        }

        void drawStatusBarBackground(Canvas c, int statusBarHeight) {
            if (statusBarHeight > 0 && Color.alpha(this.mStatusBarColor) != 0) {
                int rightInset = (int) (this.mSystemBarInsets.right * this.mScale);
                c.drawRect(0.0f, 0.0f, c.getWidth() - rightInset, statusBarHeight, this.mStatusBarPaint);
            }
        }

        void drawNavigationBarBackground(Canvas c) {
            Rect navigationBarRect = new Rect();
            DecorView.getNavigationBarRect(c.getWidth(), c.getHeight(), this.mSystemBarInsets, navigationBarRect, this.mScale);
            boolean visible = isNavigationBarColorViewVisible();
            if (visible && Color.alpha(this.mNavigationBarColor) != 0 && !navigationBarRect.isEmpty()) {
                c.drawRect(navigationBarRect, this.mNavigationBarPaint);
            }
        }
    }
}
