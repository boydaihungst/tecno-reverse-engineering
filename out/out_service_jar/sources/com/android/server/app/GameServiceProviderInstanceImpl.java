package com.android.server.app;

import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.app.ActivityTaskManager;
import android.app.IActivityManager;
import android.app.IActivityTaskManager;
import android.app.IProcessObserver;
import android.app.TaskStackListener;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Insets;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserHandle;
import android.service.games.CreateGameSessionRequest;
import android.service.games.CreateGameSessionResult;
import android.service.games.GameScreenshotResult;
import android.service.games.GameSessionViewHostConfiguration;
import android.service.games.GameStartedEvent;
import android.service.games.IGameService;
import android.service.games.IGameServiceController;
import android.service.games.IGameSession;
import android.service.games.IGameSessionController;
import android.service.games.IGameSessionService;
import android.text.TextUtils;
import android.util.Slog;
import android.view.SurfaceControl;
import android.view.SurfaceControlViewHost;
import com.android.internal.infra.AndroidFuture;
import com.android.internal.infra.ServiceConnector;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.ScreenshotHelper;
import com.android.server.app.GameServiceProviderInstanceImpl;
import com.android.server.job.controllers.JobStatus;
import com.android.server.wm.ActivityTaskManagerInternal;
import com.android.server.wm.WindowManagerInternal;
import com.android.server.wm.WindowManagerService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class GameServiceProviderInstanceImpl implements GameServiceProviderInstance {
    private static final int CREATE_GAME_SESSION_TIMEOUT_MS = 10000;
    private static final boolean DEBUG = false;
    private static final String TAG = "GameServiceProviderInstance";
    private final IActivityManager mActivityManager;
    private final ActivityManagerInternal mActivityManagerInternal;
    private final IActivityTaskManager mActivityTaskManager;
    private final ActivityTaskManagerInternal mActivityTaskManagerInternal;
    private final Executor mBackgroundExecutor;
    private final Context mContext;
    private final ServiceConnector<IGameService> mGameServiceConnector;
    private final ServiceConnector<IGameSessionService> mGameSessionServiceConnector;
    private final GameTaskInfoProvider mGameTaskInfoProvider;
    private volatile boolean mIsRunning;
    private final ScreenshotHelper mScreenshotHelper;
    private final UserHandle mUserHandle;
    private final WindowManagerInternal mWindowManagerInternal;
    private final WindowManagerService mWindowManagerService;
    private final ServiceConnector.ServiceLifecycleCallbacks<IGameService> mGameServiceLifecycleCallbacks = new ServiceConnector.ServiceLifecycleCallbacks<IGameService>() { // from class: com.android.server.app.GameServiceProviderInstanceImpl.1
        /* JADX DEBUG: Method merged with bridge method */
        public void onConnected(IGameService service) {
            try {
                service.connected(GameServiceProviderInstanceImpl.this.mGameServiceController);
            } catch (RemoteException ex) {
                Slog.w(GameServiceProviderInstanceImpl.TAG, "Failed to send connected event", ex);
            }
        }
    };
    private final ServiceConnector.ServiceLifecycleCallbacks<IGameSessionService> mGameSessionServiceLifecycleCallbacks = new AnonymousClass2();
    private final WindowManagerInternal.TaskSystemBarsListener mTaskSystemBarsVisibilityListener = new WindowManagerInternal.TaskSystemBarsListener() { // from class: com.android.server.app.GameServiceProviderInstanceImpl.3
        @Override // com.android.server.wm.WindowManagerInternal.TaskSystemBarsListener
        public void onTransientSystemBarsVisibilityChanged(int taskId, boolean visible, boolean wereRevealedFromSwipeOnSystemBar) {
            GameServiceProviderInstanceImpl.this.onTransientSystemBarsVisibilityChanged(taskId, visible, wereRevealedFromSwipeOnSystemBar);
        }
    };
    private final TaskStackListener mTaskStackListener = new AnonymousClass4();
    private final IProcessObserver mProcessObserver = new AnonymousClass5();
    private final IGameServiceController mGameServiceController = new AnonymousClass6();
    private final IGameSessionController mGameSessionController = new AnonymousClass7();
    private final Object mLock = new Object();
    private final ConcurrentHashMap<Integer, GameSessionRecord> mGameSessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, String> mPidToPackageMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Integer> mPackageNameToProcessCountMap = new ConcurrentHashMap<>();

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.app.GameServiceProviderInstanceImpl$2  reason: invalid class name */
    /* loaded from: classes.dex */
    public class AnonymousClass2 implements ServiceConnector.ServiceLifecycleCallbacks<IGameSessionService> {
        AnonymousClass2() {
        }

        public void onBinderDied() {
            GameServiceProviderInstanceImpl.this.mBackgroundExecutor.execute(new Runnable() { // from class: com.android.server.app.GameServiceProviderInstanceImpl$2$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    GameServiceProviderInstanceImpl.AnonymousClass2.this.m1575x8011aef9();
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onBinderDied$0$com-android-server-app-GameServiceProviderInstanceImpl$2  reason: not valid java name */
        public /* synthetic */ void m1575x8011aef9() {
            synchronized (GameServiceProviderInstanceImpl.this.mLock) {
                GameServiceProviderInstanceImpl.this.destroyAndClearAllGameSessionsLocked();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.app.GameServiceProviderInstanceImpl$4  reason: invalid class name */
    /* loaded from: classes.dex */
    public class AnonymousClass4 extends TaskStackListener {
        AnonymousClass4() {
        }

        public void onTaskCreated(final int taskId, final ComponentName componentName) throws RemoteException {
            if (componentName == null) {
                return;
            }
            GameServiceProviderInstanceImpl.this.mBackgroundExecutor.execute(new Runnable() { // from class: com.android.server.app.GameServiceProviderInstanceImpl$4$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    GameServiceProviderInstanceImpl.AnonymousClass4.this.m1576x60965e5e(taskId, componentName);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onTaskCreated$0$com-android-server-app-GameServiceProviderInstanceImpl$4  reason: not valid java name */
        public /* synthetic */ void m1576x60965e5e(int taskId, ComponentName componentName) {
            GameServiceProviderInstanceImpl.this.onTaskCreated(taskId, componentName);
        }

        public void onTaskRemoved(final int taskId) throws RemoteException {
            GameServiceProviderInstanceImpl.this.mBackgroundExecutor.execute(new Runnable() { // from class: com.android.server.app.GameServiceProviderInstanceImpl$4$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    GameServiceProviderInstanceImpl.AnonymousClass4.this.m1578xa0f19105(taskId);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onTaskRemoved$1$com-android-server-app-GameServiceProviderInstanceImpl$4  reason: not valid java name */
        public /* synthetic */ void m1578xa0f19105(int taskId) {
            GameServiceProviderInstanceImpl.this.onTaskRemoved(taskId);
        }

        public void onTaskFocusChanged(final int taskId, final boolean focused) {
            GameServiceProviderInstanceImpl.this.mBackgroundExecutor.execute(new Runnable() { // from class: com.android.server.app.GameServiceProviderInstanceImpl$4$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    GameServiceProviderInstanceImpl.AnonymousClass4.this.m1577x20eaf226(taskId, focused);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onTaskFocusChanged$2$com-android-server-app-GameServiceProviderInstanceImpl$4  reason: not valid java name */
        public /* synthetic */ void m1577x20eaf226(int taskId, boolean focused) {
            GameServiceProviderInstanceImpl.this.onTaskFocusChanged(taskId, focused);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.app.GameServiceProviderInstanceImpl$5  reason: invalid class name */
    /* loaded from: classes.dex */
    public class AnonymousClass5 extends IProcessObserver.Stub {
        AnonymousClass5() {
        }

        public void onForegroundActivitiesChanged(final int pid, int uid, boolean fg) {
            GameServiceProviderInstanceImpl.this.mBackgroundExecutor.execute(new Runnable() { // from class: com.android.server.app.GameServiceProviderInstanceImpl$5$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    GameServiceProviderInstanceImpl.AnonymousClass5.this.m1579x4686585e(pid);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onForegroundActivitiesChanged$0$com-android-server-app-GameServiceProviderInstanceImpl$5  reason: not valid java name */
        public /* synthetic */ void m1579x4686585e(int pid) {
            GameServiceProviderInstanceImpl.this.onForegroundActivitiesChanged(pid);
        }

        public void onProcessDied(final int pid, int uid) {
            GameServiceProviderInstanceImpl.this.mBackgroundExecutor.execute(new Runnable() { // from class: com.android.server.app.GameServiceProviderInstanceImpl$5$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    GameServiceProviderInstanceImpl.AnonymousClass5.this.m1580x1c15a62e(pid);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onProcessDied$1$com-android-server-app-GameServiceProviderInstanceImpl$5  reason: not valid java name */
        public /* synthetic */ void m1580x1c15a62e(int pid) {
            GameServiceProviderInstanceImpl.this.onProcessDied(pid);
        }

        public void onForegroundServicesChanged(int pid, int uid, int serviceTypes) {
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.app.GameServiceProviderInstanceImpl$6  reason: invalid class name */
    /* loaded from: classes.dex */
    public class AnonymousClass6 extends IGameServiceController.Stub {
        AnonymousClass6() {
        }

        public void createGameSession(final int taskId) {
            GameServiceProviderInstanceImpl.this.mContext.enforceCallingPermission("android.permission.MANAGE_GAME_ACTIVITY", "createGameSession()");
            GameServiceProviderInstanceImpl.this.mBackgroundExecutor.execute(new Runnable() { // from class: com.android.server.app.GameServiceProviderInstanceImpl$6$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    GameServiceProviderInstanceImpl.AnonymousClass6.this.m1581x781337fc(taskId);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$createGameSession$0$com-android-server-app-GameServiceProviderInstanceImpl$6  reason: not valid java name */
        public /* synthetic */ void m1581x781337fc(int taskId) {
            GameServiceProviderInstanceImpl.this.createGameSession(taskId);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.app.GameServiceProviderInstanceImpl$7  reason: invalid class name */
    /* loaded from: classes.dex */
    public class AnonymousClass7 extends IGameSessionController.Stub {
        AnonymousClass7() {
        }

        public void takeScreenshot(final int taskId, final AndroidFuture gameScreenshotResultFuture) {
            GameServiceProviderInstanceImpl.this.mContext.enforceCallingPermission("android.permission.MANAGE_GAME_ACTIVITY", "takeScreenshot()");
            GameServiceProviderInstanceImpl.this.mBackgroundExecutor.execute(new Runnable() { // from class: com.android.server.app.GameServiceProviderInstanceImpl$7$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    GameServiceProviderInstanceImpl.AnonymousClass7.this.m1583x1ce60cde(taskId, gameScreenshotResultFuture);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$takeScreenshot$0$com-android-server-app-GameServiceProviderInstanceImpl$7  reason: not valid java name */
        public /* synthetic */ void m1583x1ce60cde(int taskId, AndroidFuture gameScreenshotResultFuture) {
            GameServiceProviderInstanceImpl.this.takeScreenshot(taskId, gameScreenshotResultFuture);
        }

        public void restartGame(final int taskId) {
            GameServiceProviderInstanceImpl.this.mContext.enforceCallingPermission("android.permission.MANAGE_GAME_ACTIVITY", "restartGame()");
            GameServiceProviderInstanceImpl.this.mBackgroundExecutor.execute(new Runnable() { // from class: com.android.server.app.GameServiceProviderInstanceImpl$7$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    GameServiceProviderInstanceImpl.AnonymousClass7.this.m1582x2ecc1e43(taskId);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$restartGame$1$com-android-server-app-GameServiceProviderInstanceImpl$7  reason: not valid java name */
        public /* synthetic */ void m1582x2ecc1e43(int taskId) {
            GameServiceProviderInstanceImpl.this.restartGame(taskId);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public GameServiceProviderInstanceImpl(UserHandle userHandle, Executor backgroundExecutor, Context context, GameTaskInfoProvider gameTaskInfoProvider, IActivityManager activityManager, ActivityManagerInternal activityManagerInternal, IActivityTaskManager activityTaskManager, WindowManagerService windowManagerService, WindowManagerInternal windowManagerInternal, ActivityTaskManagerInternal activityTaskManagerInternal, ServiceConnector<IGameService> gameServiceConnector, ServiceConnector<IGameSessionService> gameSessionServiceConnector, ScreenshotHelper screenshotHelper) {
        this.mUserHandle = userHandle;
        this.mBackgroundExecutor = backgroundExecutor;
        this.mContext = context;
        this.mGameTaskInfoProvider = gameTaskInfoProvider;
        this.mActivityManager = activityManager;
        this.mActivityManagerInternal = activityManagerInternal;
        this.mActivityTaskManager = activityTaskManager;
        this.mWindowManagerService = windowManagerService;
        this.mWindowManagerInternal = windowManagerInternal;
        this.mActivityTaskManagerInternal = activityTaskManagerInternal;
        this.mGameServiceConnector = gameServiceConnector;
        this.mGameSessionServiceConnector = gameSessionServiceConnector;
        this.mScreenshotHelper = screenshotHelper;
    }

    @Override // com.android.server.app.GameServiceProviderInstance
    public void start() {
        synchronized (this.mLock) {
            startLocked();
        }
    }

    @Override // com.android.server.app.GameServiceProviderInstance
    public void stop() {
        synchronized (this.mLock) {
            stopLocked();
        }
    }

    private void startLocked() {
        if (this.mIsRunning) {
            return;
        }
        this.mIsRunning = true;
        this.mGameServiceConnector.setServiceLifecycleCallbacks(this.mGameServiceLifecycleCallbacks);
        this.mGameSessionServiceConnector.setServiceLifecycleCallbacks(this.mGameSessionServiceLifecycleCallbacks);
        this.mGameServiceConnector.connect();
        try {
            this.mActivityTaskManager.registerTaskStackListener(this.mTaskStackListener);
        } catch (RemoteException e) {
            Slog.w(TAG, "Failed to register task stack listener", e);
        }
        try {
            this.mActivityManager.registerProcessObserver(this.mProcessObserver);
        } catch (RemoteException e2) {
            Slog.w(TAG, "Failed to register process observer", e2);
        }
        this.mWindowManagerInternal.registerTaskSystemBarsListener(this.mTaskSystemBarsVisibilityListener);
    }

    private void stopLocked() {
        if (!this.mIsRunning) {
            return;
        }
        this.mIsRunning = false;
        try {
            this.mActivityManager.unregisterProcessObserver(this.mProcessObserver);
        } catch (RemoteException e) {
            Slog.w(TAG, "Failed to unregister process observer", e);
        }
        try {
            this.mActivityTaskManager.unregisterTaskStackListener(this.mTaskStackListener);
        } catch (RemoteException e2) {
            Slog.w(TAG, "Failed to unregister task stack listener", e2);
        }
        this.mWindowManagerInternal.unregisterTaskSystemBarsListener(this.mTaskSystemBarsVisibilityListener);
        destroyAndClearAllGameSessionsLocked();
        this.mGameServiceConnector.post(new ServiceConnector.VoidJob() { // from class: com.android.server.app.GameServiceProviderInstanceImpl$$ExternalSyntheticLambda0
            public final void runNoResult(Object obj) {
                ((IGameService) obj).disconnected();
            }
        }).whenComplete(new BiConsumer() { // from class: com.android.server.app.GameServiceProviderInstanceImpl$$ExternalSyntheticLambda1
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                GameServiceProviderInstanceImpl.this.m1573x1506790c((Void) obj, (Throwable) obj2);
            }
        });
        this.mGameSessionServiceConnector.unbind();
        this.mGameServiceConnector.setServiceLifecycleCallbacks((ServiceConnector.ServiceLifecycleCallbacks) null);
        this.mGameSessionServiceConnector.setServiceLifecycleCallbacks((ServiceConnector.ServiceLifecycleCallbacks) null);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$stopLocked$0$com-android-server-app-GameServiceProviderInstanceImpl  reason: not valid java name */
    public /* synthetic */ void m1573x1506790c(Void result, Throwable t) {
        this.mGameServiceConnector.unbind();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onTaskCreated(int taskId, ComponentName componentName) {
        GameTaskInfo taskInfo = this.mGameTaskInfoProvider.get(taskId, componentName);
        if (!taskInfo.mIsGameTask) {
            return;
        }
        synchronized (this.mLock) {
            gameTaskStartedLocked(taskInfo);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onTaskFocusChanged(int taskId, boolean focused) {
        synchronized (this.mLock) {
            onTaskFocusChangedLocked(taskId, focused);
        }
    }

    private void onTaskFocusChangedLocked(int taskId, boolean focused) {
        GameSessionRecord gameSessionRecord = this.mGameSessions.get(Integer.valueOf(taskId));
        if (gameSessionRecord == null) {
            if (focused) {
                maybeCreateGameSessionForFocusedTaskLocked(taskId);
            }
        } else if (gameSessionRecord.getGameSession() == null) {
        } else {
            try {
                gameSessionRecord.getGameSession().onTaskFocusChanged(focused);
            } catch (RemoteException e) {
                Slog.w(TAG, "Failed to notify session of task focus change: " + gameSessionRecord);
            }
        }
    }

    private void maybeCreateGameSessionForFocusedTaskLocked(int taskId) {
        GameTaskInfo taskInfo = this.mGameTaskInfoProvider.get(taskId);
        if (taskInfo == null) {
            Slog.w(TAG, "No task info for focused task: " + taskId);
        } else if (!taskInfo.mIsGameTask) {
        } else {
            gameTaskStartedLocked(taskInfo);
        }
    }

    private void gameTaskStartedLocked(final GameTaskInfo gameTaskInfo) {
        if (!this.mIsRunning) {
            return;
        }
        GameSessionRecord existingGameSessionRecord = this.mGameSessions.get(Integer.valueOf(gameTaskInfo.mTaskId));
        if (existingGameSessionRecord != null) {
            Slog.w(TAG, "Existing game session found for task (id: " + gameTaskInfo.mTaskId + ") creation. Ignoring.");
            return;
        }
        GameSessionRecord gameSessionRecord = GameSessionRecord.awaitingGameSessionRequest(gameTaskInfo.mTaskId, gameTaskInfo.mComponentName);
        this.mGameSessions.put(Integer.valueOf(gameTaskInfo.mTaskId), gameSessionRecord);
        this.mGameServiceConnector.post(new ServiceConnector.VoidJob() { // from class: com.android.server.app.GameServiceProviderInstanceImpl$$ExternalSyntheticLambda4
            public final void runNoResult(Object obj) {
                ((IGameService) obj).gameStarted(new GameStartedEvent(r0.mTaskId, GameTaskInfo.this.mComponentName.getPackageName()));
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onTaskRemoved(int taskId) {
        synchronized (this.mLock) {
            boolean isTaskAssociatedWithGameSession = this.mGameSessions.containsKey(Integer.valueOf(taskId));
            if (isTaskAssociatedWithGameSession) {
                removeAndDestroyGameSessionIfNecessaryLocked(taskId);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onTransientSystemBarsVisibilityChanged(int taskId, boolean visible, boolean wereRevealedFromSwipeOnSystemBar) {
        GameSessionRecord gameSessionRecord;
        IGameSession gameSession;
        if (visible && !wereRevealedFromSwipeOnSystemBar) {
            return;
        }
        synchronized (this.mLock) {
            gameSessionRecord = this.mGameSessions.get(Integer.valueOf(taskId));
        }
        if (gameSessionRecord == null || (gameSession = gameSessionRecord.getGameSession()) == null) {
            return;
        }
        try {
            gameSession.onTransientSystemBarVisibilityFromRevealGestureChanged(visible);
        } catch (RemoteException e) {
            Slog.w(TAG, "Failed to send transient system bars visibility from reveal gesture for task: " + taskId);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void createGameSession(int taskId) {
        synchronized (this.mLock) {
            createGameSessionLocked(taskId);
        }
    }

    private void createGameSessionLocked(final int taskId) {
        if (!this.mIsRunning) {
            return;
        }
        final GameSessionRecord existingGameSessionRecord = this.mGameSessions.get(Integer.valueOf(taskId));
        if (existingGameSessionRecord == null) {
            Slog.w(TAG, "No existing game session record found for task (id: " + taskId + ") creation. Ignoring.");
        } else if (!existingGameSessionRecord.isAwaitingGameSessionRequest()) {
            Slog.w(TAG, "Existing game session for task (id: " + taskId + ") is not awaiting game session request. Ignoring.");
        } else {
            final GameSessionViewHostConfiguration gameSessionViewHostConfiguration = createViewHostConfigurationForTask(taskId);
            if (gameSessionViewHostConfiguration == null) {
                Slog.w(TAG, "Failed to create view host configuration for task (id" + taskId + ") creation. Ignoring.");
                return;
            }
            this.mGameSessions.put(Integer.valueOf(taskId), existingGameSessionRecord.withGameSessionRequested());
            final AndroidFuture<CreateGameSessionResult> createGameSessionResultFuture = new AndroidFuture().orTimeout((long) JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY, TimeUnit.MILLISECONDS).whenCompleteAsync(new BiConsumer() { // from class: com.android.server.app.GameServiceProviderInstanceImpl$$ExternalSyntheticLambda2
                @Override // java.util.function.BiConsumer
                public final void accept(Object obj, Object obj2) {
                    GameServiceProviderInstanceImpl.this.m1571xbea263fe(existingGameSessionRecord, taskId, (CreateGameSessionResult) obj, (Throwable) obj2);
                }
            }, this.mBackgroundExecutor);
            this.mGameSessionServiceConnector.post(new ServiceConnector.VoidJob() { // from class: com.android.server.app.GameServiceProviderInstanceImpl$$ExternalSyntheticLambda3
                public final void runNoResult(Object obj) {
                    GameServiceProviderInstanceImpl.this.m1572xbfd8b6dd(taskId, existingGameSessionRecord, gameSessionViewHostConfiguration, createGameSessionResultFuture, (IGameSessionService) obj);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$createGameSessionLocked$2$com-android-server-app-GameServiceProviderInstanceImpl  reason: not valid java name */
    public /* synthetic */ void m1571xbea263fe(GameSessionRecord existingGameSessionRecord, int taskId, CreateGameSessionResult createGameSessionResult, Throwable exception) {
        if (exception != null || createGameSessionResult == null) {
            Slog.w(TAG, "Failed to create GameSession: " + existingGameSessionRecord, exception);
            synchronized (this.mLock) {
                removeAndDestroyGameSessionIfNecessaryLocked(taskId);
            }
            return;
        }
        synchronized (this.mLock) {
            attachGameSessionLocked(taskId, createGameSessionResult);
        }
        setGameSessionFocusedIfNecessary(taskId, createGameSessionResult.getGameSession());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$createGameSessionLocked$3$com-android-server-app-GameServiceProviderInstanceImpl  reason: not valid java name */
    public /* synthetic */ void m1572xbfd8b6dd(int taskId, GameSessionRecord existingGameSessionRecord, GameSessionViewHostConfiguration gameSessionViewHostConfiguration, AndroidFuture createGameSessionResultFuture, IGameSessionService gameSessionService) throws Exception {
        CreateGameSessionRequest createGameSessionRequest = new CreateGameSessionRequest(taskId, existingGameSessionRecord.getComponentName().getPackageName());
        gameSessionService.create(this.mGameSessionController, createGameSessionRequest, gameSessionViewHostConfiguration, createGameSessionResultFuture);
    }

    private void setGameSessionFocusedIfNecessary(int taskId, IGameSession gameSession) {
        try {
            ActivityTaskManager.RootTaskInfo rootTaskInfo = this.mActivityTaskManager.getFocusedRootTaskInfo();
            if (rootTaskInfo != null && rootTaskInfo.taskId == taskId) {
                gameSession.onTaskFocusChanged(true);
            }
        } catch (RemoteException e) {
            Slog.w(TAG, "Failed to set task focused for ID: " + taskId);
        }
    }

    private void attachGameSessionLocked(int taskId, CreateGameSessionResult createGameSessionResult) {
        GameSessionRecord gameSessionRecord = this.mGameSessions.get(Integer.valueOf(taskId));
        if (gameSessionRecord == null) {
            Slog.w(TAG, "No associated game session record. Destroying id: " + taskId);
            destroyGameSessionDuringAttach(taskId, createGameSessionResult);
        } else if (!gameSessionRecord.isGameSessionRequested()) {
            destroyGameSessionDuringAttach(taskId, createGameSessionResult);
        } else {
            try {
                this.mWindowManagerInternal.addTrustedTaskOverlay(taskId, createGameSessionResult.getSurfacePackage());
                this.mGameSessions.put(Integer.valueOf(taskId), gameSessionRecord.withGameSession(createGameSessionResult.getGameSession(), createGameSessionResult.getSurfacePackage()));
            } catch (IllegalArgumentException e) {
                Slog.w(TAG, "Failed to add task overlay. Destroying id: " + taskId);
                destroyGameSessionDuringAttach(taskId, createGameSessionResult);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void destroyAndClearAllGameSessionsLocked() {
        for (GameSessionRecord gameSessionRecord : this.mGameSessions.values()) {
            destroyGameSessionFromRecordLocked(gameSessionRecord);
        }
        this.mGameSessions.clear();
    }

    private void destroyGameSessionDuringAttach(int taskId, CreateGameSessionResult createGameSessionResult) {
        try {
            createGameSessionResult.getGameSession().onDestroyed();
        } catch (RemoteException e) {
            Slog.w(TAG, "Failed to destroy session: " + taskId);
        }
    }

    private void removeAndDestroyGameSessionIfNecessaryLocked(int taskId) {
        GameSessionRecord gameSessionRecord = this.mGameSessions.remove(Integer.valueOf(taskId));
        if (gameSessionRecord == null) {
            return;
        }
        destroyGameSessionFromRecordLocked(gameSessionRecord);
    }

    private void destroyGameSessionFromRecordLocked(GameSessionRecord gameSessionRecord) {
        SurfaceControlViewHost.SurfacePackage surfacePackage = gameSessionRecord.getSurfacePackage();
        if (surfacePackage != null) {
            try {
                this.mWindowManagerInternal.removeTrustedTaskOverlay(gameSessionRecord.getTaskId(), surfacePackage);
            } catch (IllegalArgumentException e) {
                Slog.i(TAG, "Failed to remove task overlay. This is expected if the task is already destroyed: " + gameSessionRecord);
            }
        }
        IGameSession gameSession = gameSessionRecord.getGameSession();
        if (gameSession != null) {
            try {
                gameSession.onDestroyed();
            } catch (RemoteException ex) {
                Slog.w(TAG, "Failed to destroy session: " + gameSessionRecord, ex);
            }
        }
        if (this.mGameSessions.isEmpty()) {
            this.mGameSessionServiceConnector.unbind();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onForegroundActivitiesChanged(int pid) {
        synchronized (this.mLock) {
            onForegroundActivitiesChangedLocked(pid);
        }
    }

    private void onForegroundActivitiesChangedLocked(int pid) {
        if (this.mPidToPackageMap.containsKey(Integer.valueOf(pid))) {
            return;
        }
        String packageName = this.mActivityManagerInternal.getPackageNameByPid(pid);
        if (TextUtils.isEmpty(packageName) || !gameSessionExistsForPackageNameLocked(packageName)) {
            return;
        }
        this.mPidToPackageMap.put(Integer.valueOf(pid), packageName);
        int processCountForPackage = this.mPackageNameToProcessCountMap.getOrDefault(packageName, 0).intValue() + 1;
        this.mPackageNameToProcessCountMap.put(packageName, Integer.valueOf(processCountForPackage));
        if (processCountForPackage > 0) {
            recreateEndedGameSessionsLocked(packageName);
        }
    }

    private void recreateEndedGameSessionsLocked(String packageName) {
        for (GameSessionRecord gameSessionRecord : this.mGameSessions.values()) {
            if (gameSessionRecord.isGameSessionEndedForProcessDeath() && packageName.equals(gameSessionRecord.getComponentName().getPackageName())) {
                int taskId = gameSessionRecord.getTaskId();
                this.mGameSessions.put(Integer.valueOf(taskId), GameSessionRecord.awaitingGameSessionRequest(taskId, gameSessionRecord.getComponentName()));
                createGameSessionLocked(gameSessionRecord.getTaskId());
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onProcessDied(int pid) {
        synchronized (this.mLock) {
            onProcessDiedLocked(pid);
        }
    }

    private void onProcessDiedLocked(int pid) {
        String packageName = this.mPidToPackageMap.remove(Integer.valueOf(pid));
        if (packageName == null) {
            return;
        }
        Integer oldProcessCountForPackage = this.mPackageNameToProcessCountMap.get(packageName);
        if (oldProcessCountForPackage == null) {
            Slog.w(TAG, "onProcessDiedLocked(): Missing process count for package");
            return;
        }
        int processCountForPackage = oldProcessCountForPackage.intValue() - 1;
        this.mPackageNameToProcessCountMap.put(packageName, Integer.valueOf(processCountForPackage));
        if (processCountForPackage <= 0) {
            endGameSessionsForPackageLocked(packageName);
        }
    }

    private void endGameSessionsForPackageLocked(String packageName) {
        ActivityManager.RunningTaskInfo runningTaskInfo;
        for (GameSessionRecord gameSessionRecord : this.mGameSessions.values()) {
            if (gameSessionRecord.getGameSession() != null && packageName.equals(gameSessionRecord.getComponentName().getPackageName()) && ((runningTaskInfo = this.mGameTaskInfoProvider.getRunningTaskInfo(gameSessionRecord.getTaskId())) == null || !runningTaskInfo.isVisible)) {
                this.mGameSessions.put(Integer.valueOf(gameSessionRecord.getTaskId()), gameSessionRecord.withGameSessionEndedOnProcessDeath());
                destroyGameSessionFromRecordLocked(gameSessionRecord);
            }
        }
    }

    private boolean gameSessionExistsForPackageNameLocked(String packageName) {
        for (GameSessionRecord gameSessionRecord : this.mGameSessions.values()) {
            if (packageName.equals(gameSessionRecord.getComponentName().getPackageName())) {
                return true;
            }
        }
        return false;
    }

    private GameSessionViewHostConfiguration createViewHostConfigurationForTask(int taskId) {
        ActivityManager.RunningTaskInfo runningTaskInfo = this.mGameTaskInfoProvider.getRunningTaskInfo(taskId);
        if (runningTaskInfo == null) {
            return null;
        }
        Rect bounds = runningTaskInfo.configuration.windowConfiguration.getBounds();
        return new GameSessionViewHostConfiguration(runningTaskInfo.displayId, bounds.width(), bounds.height());
    }

    void takeScreenshot(final int taskId, final AndroidFuture callback) {
        synchronized (this.mLock) {
            final GameSessionRecord gameSessionRecord = this.mGameSessions.get(Integer.valueOf(taskId));
            if (gameSessionRecord == null) {
                Slog.w(TAG, "No game session found for id: " + taskId);
                callback.complete(GameScreenshotResult.createInternalErrorResult());
                return;
            }
            SurfaceControlViewHost.SurfacePackage overlaySurfacePackage = gameSessionRecord.getSurfacePackage();
            final SurfaceControl overlaySurfaceControl = overlaySurfacePackage != null ? overlaySurfacePackage.getSurfaceControl() : null;
            this.mBackgroundExecutor.execute(new Runnable() { // from class: com.android.server.app.GameServiceProviderInstanceImpl$$ExternalSyntheticLambda6
                @Override // java.lang.Runnable
                public final void run() {
                    GameServiceProviderInstanceImpl.this.m1574x90e13d66(overlaySurfaceControl, taskId, callback, gameSessionRecord);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$takeScreenshot$5$com-android-server-app-GameServiceProviderInstanceImpl  reason: not valid java name */
    public /* synthetic */ void m1574x90e13d66(SurfaceControl overlaySurfaceControl, int taskId, final AndroidFuture callback, GameSessionRecord gameSessionRecord) {
        SurfaceControl.LayerCaptureArgs.Builder layerCaptureArgsBuilder = new SurfaceControl.LayerCaptureArgs.Builder((SurfaceControl) null);
        if (overlaySurfaceControl != null) {
            SurfaceControl[] excludeLayers = {overlaySurfaceControl};
            layerCaptureArgsBuilder.setExcludeLayers(excludeLayers);
        }
        Bitmap bitmap = this.mWindowManagerService.captureTaskBitmap(taskId, layerCaptureArgsBuilder);
        if (bitmap == null) {
            Slog.w(TAG, "Could not get bitmap for id: " + taskId);
            callback.complete(GameScreenshotResult.createInternalErrorResult());
            return;
        }
        Bundle bundle = ScreenshotHelper.HardwareBitmapBundler.hardwareBitmapToBundle(bitmap);
        ActivityManager.RunningTaskInfo runningTaskInfo = this.mGameTaskInfoProvider.getRunningTaskInfo(taskId);
        if (runningTaskInfo == null) {
            Slog.w(TAG, "Could not get running task info for id: " + taskId);
            callback.complete(GameScreenshotResult.createInternalErrorResult());
        }
        Rect crop = runningTaskInfo.configuration.windowConfiguration.getBounds();
        Consumer<Uri> completionConsumer = new Consumer() { // from class: com.android.server.app.GameServiceProviderInstanceImpl$$ExternalSyntheticLambda5
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                GameServiceProviderInstanceImpl.lambda$takeScreenshot$4(callback, (Uri) obj);
            }
        };
        this.mScreenshotHelper.provideScreenshot(bundle, crop, Insets.NONE, taskId, this.mUserHandle.getIdentifier(), gameSessionRecord.getComponentName(), 5, BackgroundThread.getHandler(), completionConsumer);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$takeScreenshot$4(AndroidFuture callback, Uri uri) {
        if (uri == null) {
            callback.complete(GameScreenshotResult.createInternalErrorResult());
        } else {
            callback.complete(GameScreenshotResult.createSuccessResult());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void restartGame(int taskId) {
        synchronized (this.mLock) {
            GameSessionRecord gameSessionRecord = this.mGameSessions.get(Integer.valueOf(taskId));
            if (gameSessionRecord == null) {
                return;
            }
            String packageName = gameSessionRecord.getComponentName().getPackageName();
            if (packageName == null) {
                return;
            }
            this.mActivityTaskManagerInternal.restartTaskActivityProcessIfVisible(taskId, packageName);
        }
    }
}
