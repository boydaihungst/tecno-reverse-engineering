package com.android.server.media;

import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.media.AudioManager;
import android.media.AudioPlaybackConfiguration;
import android.media.AudioSystem;
import android.media.IRemoteSessionCallback;
import android.media.MediaCommunicationManager;
import android.media.Session2Token;
import android.media.session.IActiveSessionsListener;
import android.media.session.IOnMediaKeyEventDispatchedListener;
import android.media.session.IOnMediaKeyEventSessionChangedListener;
import android.media.session.IOnMediaKeyListener;
import android.media.session.IOnVolumeKeyLongPressListener;
import android.media.session.ISession;
import android.media.session.ISession2TokensListener;
import android.media.session.ISessionCallback;
import android.media.session.ISessionManager;
import android.media.session.MediaSession;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerExemptionManager;
import android.os.PowerManager;
import android.os.Process;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ShellCallback;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.KeyEvent;
import android.view.ViewConfiguration;
import com.android.internal.util.FrameworkStatsLog;
import com.android.server.LocalManagerRegistry;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.Watchdog;
import com.android.server.am.ActivityManagerLocal;
import com.android.server.media.AudioPlayerStateMonitor;
import com.android.server.media.MediaSessionStack;
import com.android.server.vibrator.VibratorManagerService;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
/* loaded from: classes2.dex */
public class MediaSessionService extends SystemService implements Watchdog.Monitor {
    static final boolean DEBUG_KEY_EVENT = true;
    private static final String MEDIA_BUTTON_RECEIVER = "media_button_receiver";
    private static final int MEDIA_KEY_LISTENER_TIMEOUT = 1000;
    private static final int SESSION_CREATION_LIMIT_PER_UID = 100;
    private static final int WAKELOCK_TIMEOUT = 5000;
    private ActivityManagerLocal mActivityManagerLocal;
    private AudioManager mAudioManager;
    private AudioPlayerStateMonitor mAudioPlayerStateMonitor;
    private MediaCommunicationManager mCommunicationManager;
    private final Context mContext;
    private FullUserRecord mCurrentFullUserRecord;
    private MediaKeyDispatcher mCustomMediaKeyDispatcher;
    private MediaSessionPolicyProvider mCustomMediaSessionPolicyProvider;
    private final SparseIntArray mFullUserIds;
    private MediaSessionRecord mGlobalPrioritySession;
    private final MessageHandler mHandler;
    private boolean mHasFeatureLeanback;
    private KeyguardManager mKeyguardManager;
    private final Object mLock;
    private final PowerManager.WakeLock mMediaEventWakeLock;
    private final BroadcastReceiver mNotificationListenerEnabledChangedReceiver;
    private final NotificationManager mNotificationManager;
    private final HandlerThread mRecordThread;
    final RemoteCallbackList<IRemoteSessionCallback> mRemoteVolumeControllers;
    private final MediaCommunicationManager.SessionCallback mSession2TokenCallback;
    private final List<Session2TokensListenerRecord> mSession2TokensListenerRecords;
    private final SessionManagerImpl mSessionManagerImpl;
    private final ArrayList<SessionsListenerRecord> mSessionsListeners;
    private final SparseArray<FullUserRecord> mUserRecords;
    private static final String TAG = "MediaSessionService";
    static final boolean DEBUG = Log.isLoggable(TAG, 3);
    private static final int LONG_PRESS_TIMEOUT = ViewConfiguration.getLongPressTimeout() + 50;
    private static final int MULTI_TAP_TIMEOUT = ViewConfiguration.getMultiPressTimeout();

    public MediaSessionService(Context context) {
        super(context);
        this.mHandler = new MessageHandler();
        this.mLock = new Object();
        this.mRecordThread = new HandlerThread("SessionRecordThread");
        this.mFullUserIds = new SparseIntArray();
        this.mUserRecords = new SparseArray<>();
        this.mSessionsListeners = new ArrayList<>();
        this.mSession2TokensListenerRecords = new ArrayList();
        this.mRemoteVolumeControllers = new RemoteCallbackList<>();
        this.mSession2TokenCallback = new MediaCommunicationManager.SessionCallback() { // from class: com.android.server.media.MediaSessionService.1
            public void onSession2TokenCreated(Session2Token token) {
                if (MediaSessionService.DEBUG) {
                    Log.d(MediaSessionService.TAG, "Session2 is created " + token);
                }
                MediaSessionService mediaSessionService = MediaSessionService.this;
                MediaSession2Record record = new MediaSession2Record(token, mediaSessionService, mediaSessionService.mRecordThread.getLooper(), 0);
                synchronized (MediaSessionService.this.mLock) {
                    FullUserRecord user = MediaSessionService.this.getFullUserRecordLocked(record.getUserId());
                    if (user != null) {
                        user.mPriorityStack.addSession(record);
                    }
                }
            }
        };
        this.mNotificationListenerEnabledChangedReceiver = new BroadcastReceiver() { // from class: com.android.server.media.MediaSessionService.2
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                MediaSessionService.this.updateActiveSessionListeners();
            }
        };
        this.mContext = context;
        this.mSessionManagerImpl = new SessionManagerImpl();
        PowerManager pm = (PowerManager) context.getSystemService(PowerManager.class);
        this.mMediaEventWakeLock = pm.newWakeLock(1, "handleMediaEvent");
        this.mNotificationManager = (NotificationManager) context.getSystemService(NotificationManager.class);
        this.mAudioManager = (AudioManager) context.getSystemService(AudioManager.class);
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        publishBinderService("media_session", this.mSessionManagerImpl);
        Watchdog.getInstance().addMonitor(this);
        this.mKeyguardManager = (KeyguardManager) this.mContext.getSystemService("keyguard");
        AudioPlayerStateMonitor audioPlayerStateMonitor = AudioPlayerStateMonitor.getInstance(this.mContext);
        this.mAudioPlayerStateMonitor = audioPlayerStateMonitor;
        audioPlayerStateMonitor.registerListener(new AudioPlayerStateMonitor.OnAudioPlayerActiveStateChangedListener() { // from class: com.android.server.media.MediaSessionService$$ExternalSyntheticLambda0
            @Override // com.android.server.media.AudioPlayerStateMonitor.OnAudioPlayerActiveStateChangedListener
            public final void onAudioPlayerActiveStateChanged(AudioPlaybackConfiguration audioPlaybackConfiguration, boolean z) {
                MediaSessionService.this.m4790lambda$onStart$0$comandroidservermediaMediaSessionService(audioPlaybackConfiguration, z);
            }
        }, null);
        this.mHasFeatureLeanback = this.mContext.getPackageManager().hasSystemFeature("android.software.leanback");
        updateUser();
        instantiateCustomProvider(this.mContext.getResources().getString(17039405));
        instantiateCustomDispatcher(this.mContext.getResources().getString(17039404));
        this.mRecordThread.start();
        IntentFilter filter = new IntentFilter("android.app.action.NOTIFICATION_LISTENER_ENABLED_CHANGED");
        this.mContext.registerReceiver(this.mNotificationListenerEnabledChangedReceiver, filter);
        this.mActivityManagerLocal = (ActivityManagerLocal) LocalManagerRegistry.getManager(ActivityManagerLocal.class);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onStart$0$com-android-server-media-MediaSessionService  reason: not valid java name */
    public /* synthetic */ void m4790lambda$onStart$0$comandroidservermediaMediaSessionService(AudioPlaybackConfiguration config, boolean isRemoved) {
        if (DEBUG) {
            Log.d(TAG, "Audio playback is changed, config=" + config + ", removed=" + isRemoved);
        }
        if (config.getPlayerType() == 3) {
            return;
        }
        synchronized (this.mLock) {
            FullUserRecord user = getFullUserRecordLocked(UserHandle.getUserHandleForUid(config.getClientUid()).getIdentifier());
            if (user != null) {
                user.mPriorityStack.updateMediaButtonSessionIfNeeded();
            }
        }
    }

    @Override // com.android.server.SystemService
    public void onBootPhase(int phase) {
        super.onBootPhase(phase);
        switch (phase) {
            case SystemService.PHASE_ACTIVITY_MANAGER_READY /* 550 */:
                MediaSessionDeviceConfig.initialize(this.mContext);
                return;
            case 1000:
                MediaCommunicationManager mediaCommunicationManager = (MediaCommunicationManager) this.mContext.getSystemService(MediaCommunicationManager.class);
                this.mCommunicationManager = mediaCommunicationManager;
                mediaCommunicationManager.registerSessionCallback(new HandlerExecutor(this.mHandler), this.mSession2TokenCallback);
                return;
            default:
                return;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isGlobalPriorityActiveLocked() {
        MediaSessionRecord mediaSessionRecord = this.mGlobalPrioritySession;
        return mediaSessionRecord != null && mediaSessionRecord.isActive();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onSessionActiveStateChanged(MediaSessionRecordImpl record) {
        synchronized (this.mLock) {
            FullUserRecord user = getFullUserRecordLocked(record.getUserId());
            if (user == null) {
                Log.w(TAG, "Unknown session updated. Ignoring.");
                return;
            }
            if (record.isSystemPriority()) {
                Log.d(TAG, "Global priority session is updated, active=" + record.isActive());
                user.pushAddressedPlayerChangedLocked();
            } else if (!user.mPriorityStack.contains(record)) {
                Log.w(TAG, "Unknown session updated. Ignoring.");
                return;
            } else {
                user.mPriorityStack.onSessionActiveStateChanged(record);
            }
            this.mHandler.postSessionsChanged(record);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setGlobalPrioritySession(MediaSessionRecord record) {
        synchronized (this.mLock) {
            FullUserRecord user = getFullUserRecordLocked(record.getUserId());
            if (this.mGlobalPrioritySession != record) {
                Log.d(TAG, "Global priority session is changed from " + this.mGlobalPrioritySession + " to " + record);
                this.mGlobalPrioritySession = record;
                if (user != null && user.mPriorityStack.contains(record)) {
                    user.mPriorityStack.removeSession(record);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public List<MediaSessionRecord> getActiveSessionsLocked(int userId) {
        List<MediaSessionRecord> records = new ArrayList<>();
        if (userId == UserHandle.ALL.getIdentifier()) {
            int size = this.mUserRecords.size();
            for (int i = 0; i < size; i++) {
                records.addAll(this.mUserRecords.valueAt(i).mPriorityStack.getActiveSessions(userId));
            }
        } else {
            FullUserRecord user = getFullUserRecordLocked(userId);
            if (user == null) {
                Log.w(TAG, "getSessions failed. Unknown user " + userId);
                return records;
            }
            records.addAll(user.mPriorityStack.getActiveSessions(userId));
        }
        if (isGlobalPriorityActiveLocked() && (userId == UserHandle.ALL.getIdentifier() || userId == this.mGlobalPrioritySession.getUserId())) {
            records.add(0, this.mGlobalPrioritySession);
        }
        return records;
    }

    List<Session2Token> getSession2TokensLocked(int userId) {
        List<Session2Token> list = new ArrayList<>();
        if (userId == UserHandle.ALL.getIdentifier()) {
            int size = this.mUserRecords.size();
            for (int i = 0; i < size; i++) {
                list.addAll(this.mUserRecords.valueAt(i).mPriorityStack.getSession2Tokens(userId));
            }
        } else {
            FullUserRecord user = getFullUserRecordLocked(userId);
            list.addAll(user.mPriorityStack.getSession2Tokens(userId));
        }
        return list;
    }

    public void notifyRemoteVolumeChanged(int flags, MediaSessionRecord session) {
        if (!session.isActive()) {
            return;
        }
        synchronized (this.mLock) {
            int size = this.mRemoteVolumeControllers.beginBroadcast();
            MediaSession.Token token = session.getSessionToken();
            for (int i = size - 1; i >= 0; i--) {
                try {
                    IRemoteSessionCallback cb = this.mRemoteVolumeControllers.getBroadcastItem(i);
                    cb.onVolumeChanged(token, flags);
                } catch (Exception e) {
                    Log.w(TAG, "Error sending volume change.", e);
                }
            }
            this.mRemoteVolumeControllers.finishBroadcast();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onSessionPlaybackStateChanged(MediaSessionRecordImpl record, boolean shouldUpdatePriority) {
        synchronized (this.mLock) {
            FullUserRecord user = getFullUserRecordLocked(record.getUserId());
            if (user != null && user.mPriorityStack.contains(record)) {
                user.mPriorityStack.onPlaybackStateChanged(record, shouldUpdatePriority);
                return;
            }
            Log.d(TAG, "Unknown session changed playback state. Ignoring.");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onSessionPlaybackTypeChanged(MediaSessionRecord record) {
        synchronized (this.mLock) {
            FullUserRecord user = getFullUserRecordLocked(record.getUserId());
            if (user != null && user.mPriorityStack.contains(record)) {
                pushRemoteVolumeUpdateLocked(record.getUserId());
                return;
            }
            Log.d(TAG, "Unknown session changed playback type. Ignoring.");
        }
    }

    @Override // com.android.server.SystemService
    public void onUserStarting(SystemService.TargetUser user) {
        if (DEBUG) {
            Log.d(TAG, "onStartUser: " + user);
        }
        updateUser();
    }

    @Override // com.android.server.SystemService
    public void onUserSwitching(SystemService.TargetUser from, SystemService.TargetUser to) {
        if (DEBUG) {
            Log.d(TAG, "onSwitchUser: " + to);
        }
        updateUser();
    }

    @Override // com.android.server.SystemService
    public void onUserStopped(SystemService.TargetUser targetUser) {
        int userId = targetUser.getUserIdentifier();
        if (DEBUG) {
            Log.d(TAG, "onCleanupUser: " + userId);
        }
        synchronized (this.mLock) {
            FullUserRecord user = getFullUserRecordLocked(userId);
            if (user != null) {
                if (user.mFullUserId == userId) {
                    user.destroySessionsForUserLocked(UserHandle.ALL.getIdentifier());
                    this.mUserRecords.remove(userId);
                } else {
                    user.destroySessionsForUserLocked(userId);
                }
            }
            updateUser();
        }
    }

    @Override // com.android.server.Watchdog.Monitor
    public void monitor() {
        synchronized (this.mLock) {
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void enforcePhoneStatePermission(int pid, int uid) {
        if (this.mContext.checkPermission("android.permission.MODIFY_PHONE_STATE", pid, uid) != 0) {
            throw new SecurityException("Must hold the MODIFY_PHONE_STATE permission.");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onSessionDied(MediaSessionRecordImpl session) {
        synchronized (this.mLock) {
            destroySessionLocked(session);
        }
    }

    private void updateUser() {
        synchronized (this.mLock) {
            UserManager manager = (UserManager) this.mContext.getSystemService("user");
            this.mFullUserIds.clear();
            List<UserHandle> allUsers = manager.getUserHandles(false);
            if (allUsers != null) {
                for (UserHandle user : allUsers) {
                    UserHandle parent = manager.getProfileParent(user);
                    if (parent != null) {
                        this.mFullUserIds.put(user.getIdentifier(), parent.getIdentifier());
                    } else {
                        this.mFullUserIds.put(user.getIdentifier(), user.getIdentifier());
                        if (this.mUserRecords.get(user.getIdentifier()) == null) {
                            this.mUserRecords.put(user.getIdentifier(), new FullUserRecord(user.getIdentifier()));
                        }
                    }
                }
            }
            int currentFullUserId = ActivityManager.getCurrentUser();
            FullUserRecord fullUserRecord = this.mUserRecords.get(currentFullUserId);
            this.mCurrentFullUserRecord = fullUserRecord;
            if (fullUserRecord == null) {
                Log.w(TAG, "Cannot find FullUserInfo for the current user " + currentFullUserId);
                FullUserRecord fullUserRecord2 = new FullUserRecord(currentFullUserId);
                this.mCurrentFullUserRecord = fullUserRecord2;
                this.mUserRecords.put(currentFullUserId, fullUserRecord2);
            }
            this.mFullUserIds.put(currentFullUserId, currentFullUserId);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateActiveSessionListeners() {
        synchronized (this.mLock) {
            for (int i = this.mSessionsListeners.size() - 1; i >= 0; i--) {
                SessionsListenerRecord listener = this.mSessionsListeners.get(i);
                try {
                    String packageName = listener.componentName == null ? null : listener.componentName.getPackageName();
                    enforceMediaPermissions(packageName, listener.pid, listener.uid, listener.userId);
                } catch (SecurityException e) {
                    Log.i(TAG, "ActiveSessionsListener " + listener.componentName + " is no longer authorized. Disconnecting.");
                    this.mSessionsListeners.remove(i);
                    try {
                        listener.listener.onActiveSessionsChanged(new ArrayList());
                    } catch (Exception e2) {
                    }
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void destroySessionLocked(MediaSessionRecordImpl session) {
        if (DEBUG) {
            Log.d(TAG, "Destroying " + session);
        }
        if (session.isClosed()) {
            Log.w(TAG, "Destroying already destroyed session. Ignoring.");
            return;
        }
        FullUserRecord user = getFullUserRecordLocked(session.getUserId());
        if (user != null && (session instanceof MediaSessionRecord)) {
            int uid = session.getUid();
            int sessionCount = user.mUidToSessionCount.get(uid, 0);
            if (sessionCount <= 0) {
                Log.w(TAG, "destroySessionLocked: sessionCount should be positive. sessionCount=" + sessionCount);
            } else {
                user.mUidToSessionCount.put(uid, sessionCount - 1);
            }
        }
        if (this.mGlobalPrioritySession == session) {
            this.mGlobalPrioritySession = null;
            if (session.isActive() && user != null) {
                user.pushAddressedPlayerChangedLocked();
            }
        } else if (user != null) {
            user.mPriorityStack.removeSession(session);
        }
        session.close();
        this.mHandler.postSessionsChanged(session);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void enforcePackageName(String packageName, int uid) {
        if (TextUtils.isEmpty(packageName)) {
            throw new IllegalArgumentException("packageName may not be empty");
        }
        if (uid == 0 || uid == 2000) {
            return;
        }
        PackageManagerInternal packageManagerInternal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        int actualUid = packageManagerInternal.getPackageUid(packageName, 0L, UserHandle.getUserId(uid));
        if (!UserHandle.isSameApp(uid, actualUid)) {
            throw new IllegalArgumentException("packageName does not belong to the calling uid; pkg=" + packageName + ", uid=" + uid);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Removed duplicated region for block: B:15:0x0056  */
    /* JADX WARN: Removed duplicated region for block: B:16:0x0059  */
    /* JADX WARN: Removed duplicated region for block: B:19:0x0060  */
    /* JADX WARN: Removed duplicated region for block: B:22:0x006f A[Catch: all -> 0x00a4, TryCatch #0 {all -> 0x00a4, blocks: (B:3:0x000e, B:5:0x0013, B:7:0x001c, B:12:0x0028, B:17:0x005a, B:20:0x0062, B:22:0x006f, B:24:0x007a), top: B:30:0x000e }] */
    /* JADX WARN: Removed duplicated region for block: B:24:0x007a A[Catch: all -> 0x00a4, TRY_LEAVE, TryCatch #0 {all -> 0x00a4, blocks: (B:3:0x000e, B:5:0x0013, B:7:0x001c, B:12:0x0028, B:17:0x005a, B:20:0x0062, B:22:0x006f, B:24:0x007a), top: B:30:0x000e }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void tempAllowlistTargetPkgIfPossible(int targetUid, String targetPackage, int callingPid, int callingUid, String callingPackage, String reason) {
        boolean canStartFgs;
        long token = Binder.clearCallingIdentity();
        try {
            enforcePackageName(callingPackage, callingUid);
            if (targetUid != callingUid) {
                boolean canAllowWhileInUse = this.mActivityManagerLocal.canAllowWhileInUsePermissionInFgs(callingPid, callingUid, callingPackage);
                if (!canAllowWhileInUse && !this.mActivityManagerLocal.canStartForegroundService(callingPid, callingUid, callingPackage)) {
                    canStartFgs = false;
                    Log.i(TAG, "tempAllowlistTargetPkgIfPossible callingPackage:" + callingPackage + " targetPackage:" + targetPackage + " reason:" + reason + (!canAllowWhileInUse ? " [WIU]" : "") + (canStartFgs ? " [FGS]" : ""));
                    if (canAllowWhileInUse) {
                        this.mActivityManagerLocal.tempAllowWhileInUsePermissionInFgs(targetUid, MediaSessionDeviceConfig.getMediaSessionCallbackFgsWhileInUseTempAllowDurationMs());
                    }
                    if (canStartFgs) {
                        Context userContext = this.mContext.createContextAsUser(UserHandle.of(UserHandle.getUserId(targetUid)), 0);
                        PowerExemptionManager powerExemptionManager = (PowerExemptionManager) userContext.getSystemService(PowerExemptionManager.class);
                        powerExemptionManager.addToTemporaryAllowList(targetPackage, (int) FrameworkStatsLog.APP_BACKGROUND_RESTRICTIONS_INFO__EXEMPTION_REASON__REASON_MEDIA_SESSION_CALLBACK, reason, MediaSessionDeviceConfig.getMediaSessionCallbackFgsAllowlistDurationMs());
                    }
                }
                canStartFgs = true;
                Log.i(TAG, "tempAllowlistTargetPkgIfPossible callingPackage:" + callingPackage + " targetPackage:" + targetPackage + " reason:" + reason + (!canAllowWhileInUse ? " [WIU]" : "") + (canStartFgs ? " [FGS]" : ""));
                if (canAllowWhileInUse) {
                }
                if (canStartFgs) {
                }
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void enforceMediaPermissions(String packageName, int pid, int uid, int resolvedUserId) {
        if (hasStatusBarServicePermission(pid, uid) || hasMediaControlPermission(pid, uid)) {
            return;
        }
        if (packageName == null || !hasEnabledNotificationListener(packageName, UserHandle.getUserHandleForUid(uid), resolvedUserId)) {
            throw new SecurityException("Missing permission to control media.");
        }
    }

    private boolean hasStatusBarServicePermission(int pid, int uid) {
        return this.mContext.checkPermission("android.permission.STATUS_BAR_SERVICE", pid, uid) == 0;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void enforceStatusBarServicePermission(String action, int pid, int uid) {
        if (!hasStatusBarServicePermission(pid, uid)) {
            throw new SecurityException("Only System UI and Settings may " + action);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean hasMediaControlPermission(int pid, int uid) {
        if (uid == 1000 || this.mContext.checkPermission("android.permission.MEDIA_CONTENT_CONTROL", pid, uid) == 0) {
            return true;
        }
        if (DEBUG) {
            Log.d(TAG, "uid(" + uid + ") hasn't granted MEDIA_CONTENT_CONTROL");
            return false;
        }
        return false;
    }

    private boolean hasEnabledNotificationListener(String packageName, UserHandle userHandle, int forUserId) {
        if (userHandle.getIdentifier() != forUserId) {
            return false;
        }
        if (DEBUG) {
            Log.d(TAG, "Checking whether the package " + packageName + " has an enabled notification listener.");
        }
        return this.mNotificationManager.hasEnabledNotificationListener(packageName, userHandle);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public MediaSessionRecord createSessionInternal(int callerPid, int callerUid, int userId, String callerPackageName, ISessionCallback cb, String tag, Bundle sessionInfo) {
        Object obj;
        int policies;
        Object obj2 = this.mLock;
        synchronized (obj2) {
            try {
                try {
                    MediaSessionPolicyProvider mediaSessionPolicyProvider = this.mCustomMediaSessionPolicyProvider;
                    if (mediaSessionPolicyProvider == null) {
                        policies = 0;
                    } else {
                        int policies2 = mediaSessionPolicyProvider.getSessionPoliciesForApplication(callerUid, callerPackageName);
                        policies = policies2;
                    }
                    FullUserRecord user = getFullUserRecordLocked(userId);
                    if (user == null) {
                        Log.w(TAG, "Request from invalid user: " + userId + ", pkg=" + callerPackageName);
                        throw new RuntimeException("Session request from invalid user.");
                    }
                    int sessionCount = user.mUidToSessionCount.get(callerUid, 0);
                    if (sessionCount >= 100 && !hasMediaControlPermission(callerPid, callerUid)) {
                        throw new RuntimeException("Created too many sessions. count=" + sessionCount + ")");
                    }
                    try {
                        obj = obj2;
                        try {
                            try {
                                MediaSessionRecord session = new MediaSessionRecord(callerPid, callerUid, userId, callerPackageName, cb, tag, sessionInfo, this, this.mRecordThread.getLooper(), policies);
                                user.mUidToSessionCount.put(callerUid, sessionCount + 1);
                                user.mPriorityStack.addSession(session);
                                this.mHandler.postSessionsChanged(session);
                                if (DEBUG) {
                                    Log.d(TAG, "Created session for " + callerPackageName + " with tag " + tag);
                                }
                                return session;
                            } catch (RemoteException e) {
                                e = e;
                                throw new RuntimeException("Media Session owner died prematurely.", e);
                            }
                        } catch (Throwable th) {
                            th = th;
                            throw th;
                        }
                    } catch (RemoteException e2) {
                        e = e2;
                    }
                } catch (Throwable th2) {
                    th = th2;
                    obj = obj2;
                }
            } catch (Throwable th3) {
                th = th3;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int findIndexOfSessionsListenerLocked(IActiveSessionsListener listener) {
        for (int i = this.mSessionsListeners.size() - 1; i >= 0; i--) {
            if (this.mSessionsListeners.get(i).listener.asBinder() == listener.asBinder()) {
                return i;
            }
        }
        return -1;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int findIndexOfSession2TokensListenerLocked(ISession2TokensListener listener) {
        for (int i = this.mSession2TokensListenerRecords.size() - 1; i >= 0; i--) {
            if (this.mSession2TokensListenerRecords.get(i).listener.asBinder() == listener.asBinder()) {
                return i;
            }
        }
        return -1;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void pushSession1Changed(int userId) {
        synchronized (this.mLock) {
            FullUserRecord user = getFullUserRecordLocked(userId);
            if (user == null) {
                Log.w(TAG, "pushSession1ChangedOnHandler failed. No user with id=" + userId);
                return;
            }
            List<MediaSessionRecord> records = getActiveSessionsLocked(userId);
            int size = records.size();
            ArrayList<MediaSession.Token> tokens = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                tokens.add(records.get(i).getSessionToken());
            }
            pushRemoteVolumeUpdateLocked(userId);
            for (int i2 = this.mSessionsListeners.size() - 1; i2 >= 0; i2--) {
                SessionsListenerRecord record = this.mSessionsListeners.get(i2);
                if (record.userId == UserHandle.ALL.getIdentifier() || record.userId == userId) {
                    try {
                        record.listener.onActiveSessionsChanged(tokens);
                    } catch (RemoteException e) {
                        Log.w(TAG, "Dead ActiveSessionsListener in pushSessionsChanged, removing", e);
                        this.mSessionsListeners.remove(i2);
                    }
                }
            }
        }
    }

    void pushSession2Changed(int userId) {
        synchronized (this.mLock) {
            List<Session2Token> allSession2Tokens = getSession2TokensLocked(UserHandle.ALL.getIdentifier());
            List<Session2Token> session2Tokens = getSession2TokensLocked(userId);
            for (int i = this.mSession2TokensListenerRecords.size() - 1; i >= 0; i--) {
                Session2TokensListenerRecord listenerRecord = this.mSession2TokensListenerRecords.get(i);
                try {
                    if (listenerRecord.userId == UserHandle.ALL.getIdentifier()) {
                        listenerRecord.listener.onSession2TokensChanged(allSession2Tokens);
                    } else if (listenerRecord.userId == userId) {
                        listenerRecord.listener.onSession2TokensChanged(session2Tokens);
                    }
                } catch (RemoteException e) {
                    Log.w(TAG, "Failed to notify Session2Token change. Removing listener.", e);
                    this.mSession2TokensListenerRecords.remove(i);
                }
            }
        }
    }

    private void pushRemoteVolumeUpdateLocked(int userId) {
        FullUserRecord user = getFullUserRecordLocked(userId);
        if (user == null) {
            Log.w(TAG, "pushRemoteVolumeUpdateLocked failed. No user with id=" + userId);
            return;
        }
        synchronized (this.mLock) {
            int size = this.mRemoteVolumeControllers.beginBroadcast();
            MediaSessionRecordImpl record = user.mPriorityStack.getDefaultRemoteSession(userId);
            if (record instanceof MediaSession2Record) {
                return;
            }
            MediaSession.Token token = record == null ? null : ((MediaSessionRecord) record).getSessionToken();
            for (int i = size - 1; i >= 0; i--) {
                try {
                    IRemoteSessionCallback cb = this.mRemoteVolumeControllers.getBroadcastItem(i);
                    cb.onSessionChanged(token);
                } catch (Exception e) {
                    Log.w(TAG, "Error sending default remote volume.", e);
                }
            }
            this.mRemoteVolumeControllers.finishBroadcast();
        }
    }

    public void onMediaButtonReceiverChanged(MediaSessionRecordImpl record) {
        synchronized (this.mLock) {
            FullUserRecord user = getFullUserRecordLocked(record.getUserId());
            MediaSessionRecordImpl mediaButtonSession = user.mPriorityStack.getMediaButtonSession();
            if (record == mediaButtonSession) {
                user.rememberMediaButtonReceiverLocked(mediaButtonSession);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String getCallingPackageName(int uid) {
        String[] packages = this.mContext.getPackageManager().getPackagesForUid(uid);
        if (packages != null && packages.length > 0) {
            return packages[0];
        }
        return "";
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dispatchVolumeKeyLongPressLocked(KeyEvent keyEvent) {
        if (this.mCurrentFullUserRecord.mOnVolumeKeyLongPressListener == null) {
            return;
        }
        try {
            this.mCurrentFullUserRecord.mOnVolumeKeyLongPressListener.onVolumeKeyLongPress(keyEvent);
        } catch (RemoteException e) {
            Log.w(TAG, "Failed to send " + keyEvent + " to volume key long-press listener");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public FullUserRecord getFullUserRecordLocked(int userId) {
        int fullUserId = this.mFullUserIds.get(userId, -1);
        if (fullUserId < 0) {
            return null;
        }
        return this.mUserRecords.get(fullUserId);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public MediaSessionRecord getMediaSessionRecordLocked(MediaSession.Token sessionToken) {
        FullUserRecord user = getFullUserRecordLocked(UserHandle.getUserHandleForUid(sessionToken.getUid()).getIdentifier());
        if (user != null) {
            return user.mPriorityStack.getMediaSessionRecord(sessionToken);
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void instantiateCustomDispatcher(String componentName) {
        synchronized (this.mLock) {
            this.mCustomMediaKeyDispatcher = null;
            if (componentName != null) {
                try {
                    if (!TextUtils.isEmpty(componentName)) {
                        Class customDispatcherClass = Class.forName(componentName);
                        Constructor constructor = customDispatcherClass.getDeclaredConstructor(Context.class);
                        this.mCustomMediaKeyDispatcher = (MediaKeyDispatcher) constructor.newInstance(this.mContext);
                    }
                } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
                    this.mCustomMediaKeyDispatcher = null;
                    Log.w(TAG, "Encountered problem while using reflection", e);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void instantiateCustomProvider(String componentName) {
        synchronized (this.mLock) {
            this.mCustomMediaSessionPolicyProvider = null;
            if (componentName != null) {
                try {
                    if (!TextUtils.isEmpty(componentName)) {
                        Class customProviderClass = Class.forName(componentName);
                        Constructor constructor = customProviderClass.getDeclaredConstructor(Context.class);
                        this.mCustomMediaSessionPolicyProvider = (MediaSessionPolicyProvider) constructor.newInstance(this.mContext);
                    }
                } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
                    Log.w(TAG, "Encountered problem while using reflection", e);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public final class FullUserRecord implements MediaSessionStack.OnMediaButtonSessionChangedListener {
        private final ContentResolver mContentResolver;
        private final int mFullUserId;
        private MediaButtonReceiverHolder mLastMediaButtonReceiverHolder;
        private IOnMediaKeyListener mOnMediaKeyListener;
        private int mOnMediaKeyListenerUid;
        private IOnVolumeKeyLongPressListener mOnVolumeKeyLongPressListener;
        private int mOnVolumeKeyLongPressListenerUid;
        private final MediaSessionStack mPriorityStack;
        private final HashMap<IBinder, OnMediaKeyEventDispatchedListenerRecord> mOnMediaKeyEventDispatchedListeners = new HashMap<>();
        private final HashMap<IBinder, OnMediaKeyEventSessionChangedListenerRecord> mOnMediaKeyEventSessionChangedListeners = new HashMap<>();
        private final SparseIntArray mUidToSessionCount = new SparseIntArray();

        FullUserRecord(int fullUserId) {
            this.mFullUserId = fullUserId;
            ContentResolver contentResolver = MediaSessionService.this.mContext.createContextAsUser(UserHandle.of(fullUserId), 0).getContentResolver();
            this.mContentResolver = contentResolver;
            this.mPriorityStack = new MediaSessionStack(MediaSessionService.this.mAudioPlayerStateMonitor, this);
            String mediaButtonReceiverInfo = Settings.Secure.getString(contentResolver, MediaSessionService.MEDIA_BUTTON_RECEIVER);
            this.mLastMediaButtonReceiverHolder = MediaButtonReceiverHolder.unflattenFromString(MediaSessionService.this.mContext, mediaButtonReceiverInfo);
        }

        public void destroySessionsForUserLocked(int userId) {
            List<MediaSessionRecord> sessions = this.mPriorityStack.getPriorityList(false, userId);
            for (MediaSessionRecord session : sessions) {
                MediaSessionService.this.destroySessionLocked(session);
            }
        }

        public void addOnMediaKeyEventDispatchedListenerLocked(IOnMediaKeyEventDispatchedListener listener, int uid) {
            IBinder cbBinder = listener.asBinder();
            OnMediaKeyEventDispatchedListenerRecord cr = new OnMediaKeyEventDispatchedListenerRecord(listener, uid);
            this.mOnMediaKeyEventDispatchedListeners.put(cbBinder, cr);
            try {
                cbBinder.linkToDeath(cr, 0);
            } catch (RemoteException e) {
                Log.w(MediaSessionService.TAG, "Failed to add listener", e);
                this.mOnMediaKeyEventDispatchedListeners.remove(cbBinder);
            }
        }

        public void removeOnMediaKeyEventDispatchedListenerLocked(IOnMediaKeyEventDispatchedListener listener) {
            IBinder cbBinder = listener.asBinder();
            OnMediaKeyEventDispatchedListenerRecord cr = this.mOnMediaKeyEventDispatchedListeners.remove(cbBinder);
            cbBinder.unlinkToDeath(cr, 0);
        }

        public void addOnMediaKeyEventSessionChangedListenerLocked(IOnMediaKeyEventSessionChangedListener listener, int uid) {
            IBinder cbBinder = listener.asBinder();
            OnMediaKeyEventSessionChangedListenerRecord cr = new OnMediaKeyEventSessionChangedListenerRecord(listener, uid);
            this.mOnMediaKeyEventSessionChangedListeners.put(cbBinder, cr);
            try {
                cbBinder.linkToDeath(cr, 0);
            } catch (RemoteException e) {
                Log.w(MediaSessionService.TAG, "Failed to add listener", e);
                this.mOnMediaKeyEventSessionChangedListeners.remove(cbBinder);
            }
        }

        public void removeOnMediaKeyEventSessionChangedListener(IOnMediaKeyEventSessionChangedListener listener) {
            IBinder cbBinder = listener.asBinder();
            OnMediaKeyEventSessionChangedListenerRecord cr = this.mOnMediaKeyEventSessionChangedListeners.remove(cbBinder);
            cbBinder.unlinkToDeath(cr, 0);
        }

        public void dumpLocked(PrintWriter pw, String prefix) {
            pw.print(prefix + "Record for full_user=" + this.mFullUserId);
            int size = MediaSessionService.this.mFullUserIds.size();
            for (int i = 0; i < size; i++) {
                if (MediaSessionService.this.mFullUserIds.keyAt(i) != MediaSessionService.this.mFullUserIds.valueAt(i) && MediaSessionService.this.mFullUserIds.valueAt(i) == this.mFullUserId) {
                    pw.print(", profile_user=" + MediaSessionService.this.mFullUserIds.keyAt(i));
                }
            }
            pw.println();
            String indent = prefix + "  ";
            pw.println(indent + "Volume key long-press listener: " + this.mOnVolumeKeyLongPressListener);
            pw.println(indent + "Volume key long-press listener package: " + MediaSessionService.this.getCallingPackageName(this.mOnVolumeKeyLongPressListenerUid));
            pw.println(indent + "Media key listener: " + this.mOnMediaKeyListener);
            pw.println(indent + "Media key listener package: " + MediaSessionService.this.getCallingPackageName(this.mOnMediaKeyListenerUid));
            pw.println(indent + "OnMediaKeyEventDispatchedListener: added " + this.mOnMediaKeyEventDispatchedListeners.size() + " listener(s)");
            for (OnMediaKeyEventDispatchedListenerRecord cr : this.mOnMediaKeyEventDispatchedListeners.values()) {
                pw.println(indent + "  from " + MediaSessionService.this.getCallingPackageName(cr.uid));
            }
            pw.println(indent + "OnMediaKeyEventSessionChangedListener: added " + this.mOnMediaKeyEventSessionChangedListeners.size() + " listener(s)");
            for (OnMediaKeyEventSessionChangedListenerRecord cr2 : this.mOnMediaKeyEventSessionChangedListeners.values()) {
                pw.println(indent + "  from " + MediaSessionService.this.getCallingPackageName(cr2.uid));
            }
            pw.println(indent + "Last MediaButtonReceiver: " + this.mLastMediaButtonReceiverHolder);
            this.mPriorityStack.dump(pw, indent);
        }

        @Override // com.android.server.media.MediaSessionStack.OnMediaButtonSessionChangedListener
        public void onMediaButtonSessionChanged(MediaSessionRecordImpl oldMediaButtonSession, MediaSessionRecordImpl newMediaButtonSession) {
            Log.d(MediaSessionService.TAG, "Media button session is changed to " + newMediaButtonSession);
            synchronized (MediaSessionService.this.mLock) {
                if (oldMediaButtonSession != null) {
                    try {
                        MediaSessionService.this.mHandler.postSessionsChanged(oldMediaButtonSession);
                    } catch (Throwable th) {
                        throw th;
                    }
                }
                if (newMediaButtonSession != null) {
                    rememberMediaButtonReceiverLocked(newMediaButtonSession);
                    MediaSessionService.this.mHandler.postSessionsChanged(newMediaButtonSession);
                }
                pushAddressedPlayerChangedLocked();
            }
        }

        public void rememberMediaButtonReceiverLocked(MediaSessionRecordImpl record) {
            if (record instanceof MediaSession2Record) {
                return;
            }
            MediaSessionRecord sessionRecord = (MediaSessionRecord) record;
            MediaButtonReceiverHolder mediaButtonReceiver = sessionRecord.getMediaButtonReceiver();
            this.mLastMediaButtonReceiverHolder = mediaButtonReceiver;
            String mediaButtonReceiverInfo = mediaButtonReceiver == null ? "" : mediaButtonReceiver.flattenToString();
            Settings.Secure.putString(this.mContentResolver, MediaSessionService.MEDIA_BUTTON_RECEIVER, mediaButtonReceiverInfo);
        }

        private void pushAddressedPlayerChangedLocked(IOnMediaKeyEventSessionChangedListener callback) {
            try {
                MediaSessionRecordImpl mediaButtonSession = getMediaButtonSessionLocked();
                if (mediaButtonSession != null) {
                    if (mediaButtonSession instanceof MediaSessionRecord) {
                        MediaSessionRecord session1 = (MediaSessionRecord) mediaButtonSession;
                        callback.onMediaKeyEventSessionChanged(session1.getPackageName(), session1.getSessionToken());
                    }
                } else if (MediaSessionService.this.mCurrentFullUserRecord.mLastMediaButtonReceiverHolder != null) {
                    String packageName = MediaSessionService.this.mCurrentFullUserRecord.mLastMediaButtonReceiverHolder.getPackageName();
                    callback.onMediaKeyEventSessionChanged(packageName, (MediaSession.Token) null);
                } else {
                    callback.onMediaKeyEventSessionChanged("", (MediaSession.Token) null);
                }
            } catch (RemoteException e) {
                Log.w(MediaSessionService.TAG, "Failed to pushAddressedPlayerChangedLocked", e);
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void pushAddressedPlayerChangedLocked() {
            for (OnMediaKeyEventSessionChangedListenerRecord cr : this.mOnMediaKeyEventSessionChangedListeners.values()) {
                pushAddressedPlayerChangedLocked(cr.callback);
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public MediaSessionRecordImpl getMediaButtonSessionLocked() {
            return MediaSessionService.this.isGlobalPriorityActiveLocked() ? MediaSessionService.this.mGlobalPrioritySession : this.mPriorityStack.getMediaButtonSession();
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* loaded from: classes2.dex */
        public final class OnMediaKeyEventDispatchedListenerRecord implements IBinder.DeathRecipient {
            public final IOnMediaKeyEventDispatchedListener callback;
            public final int uid;

            OnMediaKeyEventDispatchedListenerRecord(IOnMediaKeyEventDispatchedListener callback, int uid) {
                this.callback = callback;
                this.uid = uid;
            }

            @Override // android.os.IBinder.DeathRecipient
            public void binderDied() {
                synchronized (MediaSessionService.this.mLock) {
                    FullUserRecord.this.mOnMediaKeyEventDispatchedListeners.remove(this.callback.asBinder());
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* loaded from: classes2.dex */
        public final class OnMediaKeyEventSessionChangedListenerRecord implements IBinder.DeathRecipient {
            public final IOnMediaKeyEventSessionChangedListener callback;
            public final int uid;

            OnMediaKeyEventSessionChangedListenerRecord(IOnMediaKeyEventSessionChangedListener callback, int uid) {
                this.callback = callback;
                this.uid = uid;
            }

            @Override // android.os.IBinder.DeathRecipient
            public void binderDied() {
                synchronized (MediaSessionService.this.mLock) {
                    FullUserRecord.this.mOnMediaKeyEventSessionChangedListeners.remove(this.callback.asBinder());
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public final class SessionsListenerRecord implements IBinder.DeathRecipient {
        public final ComponentName componentName;
        public final IActiveSessionsListener listener;
        public final int pid;
        public final int uid;
        public final int userId;

        SessionsListenerRecord(IActiveSessionsListener listener, ComponentName componentName, int userId, int pid, int uid) {
            this.listener = listener;
            this.componentName = componentName;
            this.userId = userId;
            this.pid = pid;
            this.uid = uid;
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            synchronized (MediaSessionService.this.mLock) {
                MediaSessionService.this.mSessionsListeners.remove(this);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public final class Session2TokensListenerRecord implements IBinder.DeathRecipient {
        public final ISession2TokensListener listener;
        public final int userId;

        Session2TokensListenerRecord(ISession2TokensListener listener, int userId) {
            this.listener = listener;
            this.userId = userId;
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            synchronized (MediaSessionService.this.mLock) {
                MediaSessionService.this.mSession2TokensListenerRecords.remove(this);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public class SessionManagerImpl extends ISessionManager.Stub {
        private static final String EXTRA_WAKELOCK_ACQUIRED = "android.media.AudioService.WAKELOCK_ACQUIRED";
        private static final int WAKELOCK_RELEASE_ON_FINISHED = 1980;
        private KeyEventWakeLockReceiver mKeyEventReceiver;
        private KeyEventHandler mMediaKeyEventHandler = new KeyEventHandler(0);
        private KeyEventHandler mVolumeKeyEventHandler = new KeyEventHandler(1);

        SessionManagerImpl() {
            this.mKeyEventReceiver = new KeyEventWakeLockReceiver(MediaSessionService.this.mHandler);
        }

        /* JADX DEBUG: Multi-variable search result rejected for r11v0, resolved type: com.android.server.media.MediaSessionService$SessionManagerImpl */
        /* JADX WARN: Multi-variable type inference failed */
        public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
            String str;
            String[] packageNames = MediaSessionService.this.mContext.getPackageManager().getPackagesForUid(Binder.getCallingUid());
            if (packageNames != null && packageNames.length > 0) {
                str = packageNames[0];
            } else {
                str = VibratorManagerService.VibratorManagerShellCommand.SHELL_PACKAGE_NAME;
            }
            String packageName = str;
            new MediaShellCommand(packageName).exec(this, in, out, err, args, callback, resultReceiver);
        }

        public ISession createSession(String packageName, ISessionCallback cb, String tag, Bundle sessionInfo, int userId) throws RemoteException {
            int pid = Binder.getCallingPid();
            int uid = Binder.getCallingUid();
            long token = Binder.clearCallingIdentity();
            try {
                try {
                    MediaSessionService.this.enforcePackageName(packageName, uid);
                    try {
                        int resolvedUserId = handleIncomingUser(pid, uid, userId, packageName);
                        if (cb == null) {
                            throw new IllegalArgumentException("Controller callback cannot be null");
                        }
                        MediaSessionRecord session = MediaSessionService.this.createSessionInternal(pid, uid, resolvedUserId, packageName, cb, tag, sessionInfo);
                        if (session == null) {
                            throw new IllegalStateException("Failed to create a new session record");
                        }
                        ISession sessionBinder = session.getSessionBinder();
                        if (sessionBinder == null) {
                            throw new IllegalStateException("Invalid session record");
                        }
                        Binder.restoreCallingIdentity(token);
                        return sessionBinder;
                    } catch (Exception e) {
                        e = e;
                        Log.w(MediaSessionService.TAG, "Exception in creating a new session", e);
                        throw e;
                    }
                } catch (Throwable th) {
                    e = th;
                    Binder.restoreCallingIdentity(token);
                    throw e;
                }
            } catch (Exception e2) {
                e = e2;
            } catch (Throwable th2) {
                e = th2;
                Binder.restoreCallingIdentity(token);
                throw e;
            }
        }

        public List<MediaSession.Token> getSessions(ComponentName componentName, int userId) {
            int pid = Binder.getCallingPid();
            int uid = Binder.getCallingUid();
            long token = Binder.clearCallingIdentity();
            try {
                int resolvedUserId = verifySessionsRequest(componentName, userId, pid, uid);
                ArrayList<MediaSession.Token> tokens = new ArrayList<>();
                synchronized (MediaSessionService.this.mLock) {
                    List<MediaSessionRecord> records = MediaSessionService.this.getActiveSessionsLocked(resolvedUserId);
                    for (MediaSessionRecord record : records) {
                        tokens.add(record.getSessionToken());
                    }
                }
                return tokens;
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1280=4] */
        public MediaSession.Token getMediaKeyEventSession(String packageName) {
            int pid = Binder.getCallingPid();
            int uid = Binder.getCallingUid();
            UserHandle userHandle = UserHandle.getUserHandleForUid(uid);
            int userId = userHandle.getIdentifier();
            long token = Binder.clearCallingIdentity();
            try {
                MediaSessionService.this.enforcePackageName(packageName, uid);
                MediaSessionService.this.enforceMediaPermissions(packageName, pid, uid, userId);
                synchronized (MediaSessionService.this.mLock) {
                    FullUserRecord user = MediaSessionService.this.getFullUserRecordLocked(userId);
                    if (user == null) {
                        Log.w(MediaSessionService.TAG, "No matching user record to get the media key event session, userId=" + userId);
                        return null;
                    }
                    MediaSessionRecordImpl record = user.getMediaButtonSessionLocked();
                    if (record instanceof MediaSessionRecord) {
                        return ((MediaSessionRecord) record).getSessionToken();
                    }
                    return null;
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1313=5] */
        public String getMediaKeyEventSessionPackageName(String packageName) {
            int pid = Binder.getCallingPid();
            int uid = Binder.getCallingUid();
            UserHandle userHandle = UserHandle.getUserHandleForUid(uid);
            int userId = userHandle.getIdentifier();
            long token = Binder.clearCallingIdentity();
            try {
                MediaSessionService.this.enforcePackageName(packageName, uid);
                MediaSessionService.this.enforceMediaPermissions(packageName, pid, uid, userId);
                synchronized (MediaSessionService.this.mLock) {
                    FullUserRecord user = MediaSessionService.this.getFullUserRecordLocked(userId);
                    if (user == null) {
                        Log.w(MediaSessionService.TAG, "No matching user record to get the media key event session package , userId=" + userId);
                        return "";
                    }
                    MediaSessionRecordImpl record = user.getMediaButtonSessionLocked();
                    if (record instanceof MediaSessionRecord) {
                        return record.getPackageName();
                    } else if (user.mLastMediaButtonReceiverHolder != null) {
                        return user.mLastMediaButtonReceiverHolder.getPackageName();
                    } else {
                        return "";
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1347=4] */
        public void addSessionsListener(IActiveSessionsListener listener, ComponentName componentName, int userId) throws RemoteException {
            if (listener == null) {
                Log.w(MediaSessionService.TAG, "addSessionsListener: listener is null, ignoring");
                return;
            }
            int pid = Binder.getCallingPid();
            int uid = Binder.getCallingUid();
            long token = Binder.clearCallingIdentity();
            try {
                int resolvedUserId = verifySessionsRequest(componentName, userId, pid, uid);
                synchronized (MediaSessionService.this.mLock) {
                    int index = MediaSessionService.this.findIndexOfSessionsListenerLocked(listener);
                    if (index != -1) {
                        Log.w(MediaSessionService.TAG, "ActiveSessionsListener is already added, ignoring");
                        return;
                    }
                    SessionsListenerRecord record = new SessionsListenerRecord(listener, componentName, resolvedUserId, pid, uid);
                    try {
                        listener.asBinder().linkToDeath(record, 0);
                        MediaSessionService.this.mSessionsListeners.add(record);
                    } catch (RemoteException e) {
                        Log.e(MediaSessionService.TAG, "ActiveSessionsListener is dead, ignoring it", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void removeSessionsListener(IActiveSessionsListener listener) throws RemoteException {
            synchronized (MediaSessionService.this.mLock) {
                int index = MediaSessionService.this.findIndexOfSessionsListenerLocked(listener);
                if (index != -1) {
                    SessionsListenerRecord record = (SessionsListenerRecord) MediaSessionService.this.mSessionsListeners.remove(index);
                    try {
                        record.listener.asBinder().unlinkToDeath(record, 0);
                    } catch (Exception e) {
                    }
                }
            }
        }

        public void addSession2TokensListener(ISession2TokensListener listener, int userId) {
            if (listener == null) {
                Log.w(MediaSessionService.TAG, "addSession2TokensListener: listener is null, ignoring");
                return;
            }
            int pid = Binder.getCallingPid();
            int uid = Binder.getCallingUid();
            long token = Binder.clearCallingIdentity();
            try {
                int resolvedUserId = handleIncomingUser(pid, uid, userId, null);
                synchronized (MediaSessionService.this.mLock) {
                    int index = MediaSessionService.this.findIndexOfSession2TokensListenerLocked(listener);
                    if (index >= 0) {
                        Log.w(MediaSessionService.TAG, "addSession2TokensListener: listener is already added, ignoring");
                    } else {
                        MediaSessionService.this.mSession2TokensListenerRecords.add(new Session2TokensListenerRecord(listener, resolvedUserId));
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void removeSession2TokensListener(ISession2TokensListener listener) {
            Binder.getCallingPid();
            Binder.getCallingUid();
            long token = Binder.clearCallingIdentity();
            try {
                synchronized (MediaSessionService.this.mLock) {
                    int index = MediaSessionService.this.findIndexOfSession2TokensListenerLocked(listener);
                    if (index >= 0) {
                        Session2TokensListenerRecord listenerRecord = (Session2TokensListenerRecord) MediaSessionService.this.mSession2TokensListenerRecords.remove(index);
                        try {
                            listenerRecord.listener.asBinder().unlinkToDeath(listenerRecord, 0);
                        } catch (Exception e) {
                        }
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1498=6] */
        public void dispatchMediaKeyEvent(String packageName, boolean asSystemService, KeyEvent keyEvent, boolean needWakeLock) {
            if (keyEvent == null || !KeyEvent.isMediaSessionKey(keyEvent.getKeyCode())) {
                Log.w(MediaSessionService.TAG, "Attempted to dispatch null or non-media key event.");
                return;
            }
            int pid = Binder.getCallingPid();
            int uid = Binder.getCallingUid();
            long token = Binder.clearCallingIdentity();
            try {
                if (MediaSessionService.DEBUG) {
                    try {
                        try {
                            Log.d(MediaSessionService.TAG, "dispatchMediaKeyEvent, pkg=" + packageName + " pid=" + pid + ", uid=" + uid + ", asSystem=" + asSystemService + ", event=" + keyEvent);
                        } catch (Throwable th) {
                            th = th;
                            Binder.restoreCallingIdentity(token);
                            throw th;
                        }
                    } catch (Throwable th2) {
                        th = th2;
                    }
                }
                if (!isUserSetupComplete()) {
                    Log.i(MediaSessionService.TAG, "Not dispatching media key event because user setup is in progress.");
                    Binder.restoreCallingIdentity(token);
                    return;
                }
                synchronized (MediaSessionService.this.mLock) {
                    boolean isGlobalPriorityActive = MediaSessionService.this.isGlobalPriorityActiveLocked();
                    if (isGlobalPriorityActive && uid != 1000) {
                        Log.i(MediaSessionService.TAG, "Only the system can dispatch media key event to the global priority session.");
                        Binder.restoreCallingIdentity(token);
                        return;
                    }
                    if (!isGlobalPriorityActive && MediaSessionService.this.mCurrentFullUserRecord.mOnMediaKeyListener != null) {
                        Log.d(MediaSessionService.TAG, "Send " + keyEvent + " to the media key listener");
                        try {
                            MediaSessionService.this.mCurrentFullUserRecord.mOnMediaKeyListener.onMediaKey(keyEvent, new MediaKeyListenerResultReceiver(packageName, pid, uid, asSystemService, keyEvent, needWakeLock));
                            Binder.restoreCallingIdentity(token);
                            return;
                        } catch (RemoteException e) {
                            Log.w(MediaSessionService.TAG, "Failed to send " + keyEvent + " to the media key listener");
                        }
                    }
                    if (isGlobalPriorityActive) {
                        dispatchMediaKeyEventLocked(packageName, pid, uid, asSystemService, keyEvent, needWakeLock);
                    } else {
                        this.mMediaKeyEventHandler.handleMediaKeyEventLocked(packageName, pid, uid, asSystemService, keyEvent, needWakeLock);
                    }
                    Binder.restoreCallingIdentity(token);
                }
            } catch (Throwable th3) {
                th = th3;
            }
        }

        public boolean dispatchMediaKeyEventToSessionAsSystemService(String packageName, KeyEvent keyEvent, MediaSession.Token sessionToken) {
            int pid = Binder.getCallingPid();
            int uid = Binder.getCallingUid();
            long token = Binder.clearCallingIdentity();
            try {
                synchronized (MediaSessionService.this.mLock) {
                    MediaSessionRecord record = MediaSessionService.this.getMediaSessionRecordLocked(sessionToken);
                    Log.d(MediaSessionService.TAG, "dispatchMediaKeyEventToSessionAsSystemService, pkg=" + packageName + ", pid=" + pid + ", uid=" + uid + ", sessionToken=" + sessionToken + ", event=" + keyEvent + ", session=" + record);
                    if (record == null) {
                        Log.w(MediaSessionService.TAG, "Failed to find session to dispatch key event.");
                        return false;
                    }
                    return record.sendMediaButton(packageName, pid, uid, true, keyEvent, 0, null);
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void addOnMediaKeyEventDispatchedListener(IOnMediaKeyEventDispatchedListener listener) {
            if (listener == null) {
                Log.w(MediaSessionService.TAG, "addOnMediaKeyEventDispatchedListener: listener is null, ignoring");
                return;
            }
            int pid = Binder.getCallingPid();
            int uid = Binder.getCallingUid();
            int userId = UserHandle.getUserHandleForUid(uid).getIdentifier();
            long token = Binder.clearCallingIdentity();
            try {
                if (!MediaSessionService.this.hasMediaControlPermission(pid, uid)) {
                    throw new SecurityException("MEDIA_CONTENT_CONTROL permission is required to  add MediaKeyEventDispatchedListener");
                }
                synchronized (MediaSessionService.this.mLock) {
                    FullUserRecord user = MediaSessionService.this.getFullUserRecordLocked(userId);
                    if (user != null && user.mFullUserId == userId) {
                        user.addOnMediaKeyEventDispatchedListenerLocked(listener, uid);
                        Log.d(MediaSessionService.TAG, "The MediaKeyEventDispatchedListener (" + listener.asBinder() + ") is added by " + MediaSessionService.this.getCallingPackageName(uid));
                        return;
                    }
                    Log.w(MediaSessionService.TAG, "Only the full user can add the listener, userId=" + userId);
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void removeOnMediaKeyEventDispatchedListener(IOnMediaKeyEventDispatchedListener listener) {
            if (listener == null) {
                Log.w(MediaSessionService.TAG, "removeOnMediaKeyEventDispatchedListener: listener is null, ignoring");
                return;
            }
            int pid = Binder.getCallingPid();
            int uid = Binder.getCallingUid();
            int userId = UserHandle.getUserHandleForUid(uid).getIdentifier();
            long token = Binder.clearCallingIdentity();
            try {
                if (!MediaSessionService.this.hasMediaControlPermission(pid, uid)) {
                    throw new SecurityException("MEDIA_CONTENT_CONTROL permission is required to  remove MediaKeyEventDispatchedListener");
                }
                synchronized (MediaSessionService.this.mLock) {
                    FullUserRecord user = MediaSessionService.this.getFullUserRecordLocked(userId);
                    if (user != null && user.mFullUserId == userId) {
                        user.removeOnMediaKeyEventDispatchedListenerLocked(listener);
                        Log.d(MediaSessionService.TAG, "The MediaKeyEventDispatchedListener (" + listener.asBinder() + ") is removed by " + MediaSessionService.this.getCallingPackageName(uid));
                        return;
                    }
                    Log.w(MediaSessionService.TAG, "Only the full user can remove the listener, userId=" + userId);
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void addOnMediaKeyEventSessionChangedListener(IOnMediaKeyEventSessionChangedListener listener, String packageName) {
            if (listener == null) {
                Log.w(MediaSessionService.TAG, "addOnMediaKeyEventSessionChangedListener: listener is null, ignoring");
                return;
            }
            int pid = Binder.getCallingPid();
            int uid = Binder.getCallingUid();
            UserHandle userHandle = UserHandle.getUserHandleForUid(uid);
            int userId = userHandle.getIdentifier();
            long token = Binder.clearCallingIdentity();
            try {
                MediaSessionService.this.enforcePackageName(packageName, uid);
                MediaSessionService.this.enforceMediaPermissions(packageName, pid, uid, userId);
                synchronized (MediaSessionService.this.mLock) {
                    FullUserRecord user = MediaSessionService.this.getFullUserRecordLocked(userId);
                    if (user != null && user.mFullUserId == userId) {
                        user.addOnMediaKeyEventSessionChangedListenerLocked(listener, uid);
                        Log.d(MediaSessionService.TAG, "The MediaKeyEventSessionChangedListener (" + listener.asBinder() + ") is added by " + packageName);
                        return;
                    }
                    Log.w(MediaSessionService.TAG, "Only the full user can add the listener, userId=" + userId);
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void removeOnMediaKeyEventSessionChangedListener(IOnMediaKeyEventSessionChangedListener listener) {
            if (listener == null) {
                Log.w(MediaSessionService.TAG, "removeOnMediaKeyEventSessionChangedListener: listener is null, ignoring");
                return;
            }
            Binder.getCallingPid();
            int uid = Binder.getCallingUid();
            int userId = UserHandle.getUserHandleForUid(uid).getIdentifier();
            long token = Binder.clearCallingIdentity();
            try {
                synchronized (MediaSessionService.this.mLock) {
                    FullUserRecord user = MediaSessionService.this.getFullUserRecordLocked(userId);
                    if (user != null && user.mFullUserId == userId) {
                        user.removeOnMediaKeyEventSessionChangedListener(listener);
                        Log.d(MediaSessionService.TAG, "The MediaKeyEventSessionChangedListener (" + listener.asBinder() + ") is removed by " + MediaSessionService.this.getCallingPackageName(uid));
                        return;
                    }
                    Log.w(MediaSessionService.TAG, "Only the full user can remove the listener, userId=" + userId);
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1723=4] */
        public void setOnVolumeKeyLongPressListener(IOnVolumeKeyLongPressListener listener) {
            int pid = Binder.getCallingPid();
            int uid = Binder.getCallingUid();
            long token = Binder.clearCallingIdentity();
            try {
                if (MediaSessionService.this.mContext.checkPermission("android.permission.SET_VOLUME_KEY_LONG_PRESS_LISTENER", pid, uid) != 0) {
                    throw new SecurityException("Must hold the SET_VOLUME_KEY_LONG_PRESS_LISTENER permission.");
                }
                synchronized (MediaSessionService.this.mLock) {
                    int userId = UserHandle.getUserHandleForUid(uid).getIdentifier();
                    final FullUserRecord user = MediaSessionService.this.getFullUserRecordLocked(userId);
                    if (user != null && user.mFullUserId == userId) {
                        if (user.mOnVolumeKeyLongPressListener != null && user.mOnVolumeKeyLongPressListenerUid != uid) {
                            Log.w(MediaSessionService.TAG, "The volume key long-press listener cannot be reset by another app , mOnVolumeKeyLongPressListener=" + user.mOnVolumeKeyLongPressListenerUid + ", uid=" + uid);
                            return;
                        }
                        user.mOnVolumeKeyLongPressListener = listener;
                        user.mOnVolumeKeyLongPressListenerUid = uid;
                        Log.d(MediaSessionService.TAG, "The volume key long-press listener " + listener + " is set by " + MediaSessionService.this.getCallingPackageName(uid));
                        if (user.mOnVolumeKeyLongPressListener != null) {
                            try {
                                user.mOnVolumeKeyLongPressListener.asBinder().linkToDeath(new IBinder.DeathRecipient() { // from class: com.android.server.media.MediaSessionService.SessionManagerImpl.1
                                    @Override // android.os.IBinder.DeathRecipient
                                    public void binderDied() {
                                        synchronized (MediaSessionService.this.mLock) {
                                            user.mOnVolumeKeyLongPressListener = null;
                                        }
                                    }
                                }, 0);
                            } catch (RemoteException e) {
                                Log.w(MediaSessionService.TAG, "Failed to set death recipient " + user.mOnVolumeKeyLongPressListener);
                                user.mOnVolumeKeyLongPressListener = null;
                            }
                        }
                        return;
                    }
                    Log.w(MediaSessionService.TAG, "Only the full user can set the volume key long-press listener, userId=" + userId);
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1779=4] */
        public void setOnMediaKeyListener(IOnMediaKeyListener listener) {
            int pid = Binder.getCallingPid();
            int uid = Binder.getCallingUid();
            long token = Binder.clearCallingIdentity();
            try {
                if (MediaSessionService.this.mContext.checkPermission("android.permission.SET_MEDIA_KEY_LISTENER", pid, uid) != 0) {
                    throw new SecurityException("Must hold the SET_MEDIA_KEY_LISTENER permission.");
                }
                synchronized (MediaSessionService.this.mLock) {
                    int userId = UserHandle.getUserHandleForUid(uid).getIdentifier();
                    final FullUserRecord user = MediaSessionService.this.getFullUserRecordLocked(userId);
                    if (user != null && user.mFullUserId == userId) {
                        if (user.mOnMediaKeyListener != null && user.mOnMediaKeyListenerUid != uid) {
                            Log.w(MediaSessionService.TAG, "The media key listener cannot be reset by another app. , mOnMediaKeyListenerUid=" + user.mOnMediaKeyListenerUid + ", uid=" + uid);
                            return;
                        }
                        user.mOnMediaKeyListener = listener;
                        user.mOnMediaKeyListenerUid = uid;
                        Log.d(MediaSessionService.TAG, "The media key listener " + user.mOnMediaKeyListener + " is set by " + MediaSessionService.this.getCallingPackageName(uid));
                        if (user.mOnMediaKeyListener != null) {
                            try {
                                user.mOnMediaKeyListener.asBinder().linkToDeath(new IBinder.DeathRecipient() { // from class: com.android.server.media.MediaSessionService.SessionManagerImpl.2
                                    @Override // android.os.IBinder.DeathRecipient
                                    public void binderDied() {
                                        synchronized (MediaSessionService.this.mLock) {
                                            user.mOnMediaKeyListener = null;
                                        }
                                    }
                                }, 0);
                            } catch (RemoteException e) {
                                Log.w(MediaSessionService.TAG, "Failed to set death recipient " + user.mOnMediaKeyListener);
                                user.mOnMediaKeyListener = null;
                            }
                        }
                        return;
                    }
                    Log.w(MediaSessionService.TAG, "Only the full user can set the media key listener, userId=" + userId);
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void dispatchVolumeKeyEvent(String packageName, String opPackageName, boolean asSystemService, KeyEvent keyEvent, int stream, boolean musicOnly) {
            if (keyEvent == null || (keyEvent.getKeyCode() != 24 && keyEvent.getKeyCode() != 25 && keyEvent.getKeyCode() != 164)) {
                Log.w(MediaSessionService.TAG, "Attempted to dispatch null or non-volume key event.");
                return;
            }
            int pid = Binder.getCallingPid();
            int uid = Binder.getCallingUid();
            long token = Binder.clearCallingIdentity();
            Log.d(MediaSessionService.TAG, "dispatchVolumeKeyEvent, pkg=" + packageName + ", opPkg=" + opPackageName + ", pid=" + pid + ", uid=" + uid + ", asSystem=" + asSystemService + ", event=" + keyEvent + ", stream=" + stream + ", musicOnly=" + musicOnly);
            try {
                synchronized (MediaSessionService.this.mLock) {
                    if (MediaSessionService.this.isGlobalPriorityActiveLocked()) {
                        dispatchVolumeKeyEventLocked(packageName, opPackageName, pid, uid, asSystemService, keyEvent, stream, musicOnly);
                    } else {
                        this.mVolumeKeyEventHandler.handleVolumeKeyEventLocked(packageName, pid, uid, asSystemService, keyEvent, opPackageName, stream, musicOnly);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void dispatchVolumeKeyEventLocked(String packageName, String opPackageName, int pid, int uid, boolean asSystemService, KeyEvent keyEvent, int stream, boolean musicOnly) {
            boolean down = keyEvent.getAction() == 0;
            boolean up = keyEvent.getAction() == 1;
            int direction = 0;
            boolean isMute = false;
            switch (keyEvent.getKeyCode()) {
                case 24:
                    direction = 1;
                    break;
                case 25:
                    direction = -1;
                    break;
                case 164:
                    isMute = true;
                    break;
            }
            if (down || up) {
                int flags = 4096;
                if (!musicOnly) {
                    if (up) {
                        flags = 4096 | 20;
                    } else {
                        flags = 4096 | 17;
                    }
                }
                if (direction != 0) {
                    if (up) {
                        direction = 0;
                    }
                    dispatchAdjustVolumeLocked(packageName, opPackageName, pid, uid, asSystemService, stream, direction, flags, musicOnly);
                } else if (isMute && down && keyEvent.getRepeatCount() == 0) {
                    dispatchAdjustVolumeLocked(packageName, opPackageName, pid, uid, asSystemService, stream, 101, flags, musicOnly);
                }
            }
        }

        public void dispatchVolumeKeyEventToSessionAsSystemService(String packageName, String opPackageName, KeyEvent keyEvent, MediaSession.Token sessionToken) {
            int direction;
            int pid = Binder.getCallingPid();
            int uid = Binder.getCallingUid();
            long token = Binder.clearCallingIdentity();
            try {
                synchronized (MediaSessionService.this.mLock) {
                    MediaSessionRecord record = MediaSessionService.this.getMediaSessionRecordLocked(sessionToken);
                    Log.d(MediaSessionService.TAG, "dispatchVolumeKeyEventToSessionAsSystemService, pkg=" + packageName + ", opPkg=" + opPackageName + ", pid=" + pid + ", uid=" + uid + ", sessionToken=" + sessionToken + ", event=" + keyEvent + ", session=" + record);
                    if (record == null) {
                        Log.w(MediaSessionService.TAG, "Failed to find session to dispatch key event, token=" + sessionToken + ". Fallbacks to the default handling.");
                        dispatchVolumeKeyEventLocked(packageName, opPackageName, pid, uid, true, keyEvent, Integer.MIN_VALUE, false);
                        return;
                    }
                    switch (keyEvent.getAction()) {
                        case 0:
                            switch (keyEvent.getKeyCode()) {
                                case 24:
                                    direction = 1;
                                    break;
                                case 25:
                                    direction = -1;
                                    break;
                                case 164:
                                    direction = 101;
                                    break;
                                default:
                                    direction = 0;
                                    break;
                            }
                            record.adjustVolume(packageName, opPackageName, pid, uid, true, direction, 1, false);
                            break;
                        case 1:
                            record.adjustVolume(packageName, opPackageName, pid, uid, true, 0, 4116, false);
                            break;
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void dispatchAdjustVolume(String packageName, String opPackageName, int suggestedStream, int delta, int flags) {
            int pid = Binder.getCallingPid();
            int uid = Binder.getCallingUid();
            long token = Binder.clearCallingIdentity();
            try {
                synchronized (MediaSessionService.this.mLock) {
                    dispatchAdjustVolumeLocked(packageName, opPackageName, pid, uid, false, suggestedStream, delta, flags, false);
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void registerRemoteSessionCallback(IRemoteSessionCallback rvc) {
            int pid = Binder.getCallingPid();
            int uid = Binder.getCallingUid();
            long token = Binder.clearCallingIdentity();
            synchronized (MediaSessionService.this.mLock) {
                MediaSessionService.this.enforceStatusBarServicePermission("listen for volume changes", pid, uid);
                MediaSessionService.this.mRemoteVolumeControllers.register(rvc);
                Binder.restoreCallingIdentity(token);
            }
        }

        public void unregisterRemoteSessionCallback(IRemoteSessionCallback rvc) {
            int pid = Binder.getCallingPid();
            int uid = Binder.getCallingUid();
            long token = Binder.clearCallingIdentity();
            synchronized (MediaSessionService.this.mLock) {
                MediaSessionService.this.enforceStatusBarServicePermission("listen for volume changes", pid, uid);
                MediaSessionService.this.mRemoteVolumeControllers.unregister(rvc);
                Binder.restoreCallingIdentity(token);
            }
        }

        public boolean isGlobalPriorityActive() {
            boolean isGlobalPriorityActiveLocked;
            synchronized (MediaSessionService.this.mLock) {
                isGlobalPriorityActiveLocked = MediaSessionService.this.isGlobalPriorityActiveLocked();
            }
            return isGlobalPriorityActiveLocked;
        }

        public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (MediaServerUtils.checkDumpPermission(MediaSessionService.this.mContext, MediaSessionService.TAG, pw)) {
                pw.println("MEDIA SESSION SERVICE (dumpsys media_session)");
                pw.println();
                synchronized (MediaSessionService.this.mLock) {
                    pw.println(MediaSessionService.this.mSessionsListeners.size() + " sessions listeners.");
                    pw.println("Global priority session is " + MediaSessionService.this.mGlobalPrioritySession);
                    if (MediaSessionService.this.mGlobalPrioritySession != null) {
                        MediaSessionService.this.mGlobalPrioritySession.dump(pw, "  ");
                    }
                    pw.println("User Records:");
                    int count = MediaSessionService.this.mUserRecords.size();
                    for (int i = 0; i < count; i++) {
                        ((FullUserRecord) MediaSessionService.this.mUserRecords.valueAt(i)).dumpLocked(pw, "");
                    }
                    MediaSessionService.this.mAudioPlayerStateMonitor.dump(MediaSessionService.this.mContext, pw, "");
                }
                MediaSessionDeviceConfig.dump(pw, "");
            }
        }

        public boolean isTrusted(String controllerPackageName, int controllerPid, int controllerUid) {
            boolean z;
            int uid = Binder.getCallingUid();
            int userId = UserHandle.getUserHandleForUid(uid).getIdentifier();
            long token = Binder.clearCallingIdentity();
            try {
                if (!MediaSessionService.this.hasMediaControlPermission(controllerPid, controllerUid)) {
                    if (!hasEnabledNotificationListener(userId, controllerPackageName, controllerUid)) {
                        z = false;
                        return z;
                    }
                }
                z = true;
                return z;
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void setCustomMediaKeyDispatcher(String name) {
            MediaSessionService.this.instantiateCustomDispatcher(name);
        }

        public void setCustomMediaSessionPolicyProvider(String name) {
            MediaSessionService.this.instantiateCustomProvider(name);
        }

        public boolean hasCustomMediaKeyDispatcher(String componentName) {
            if (MediaSessionService.this.mCustomMediaKeyDispatcher == null) {
                return false;
            }
            return TextUtils.equals(componentName, MediaSessionService.this.mCustomMediaKeyDispatcher.getClass().getName());
        }

        public boolean hasCustomMediaSessionPolicyProvider(String componentName) {
            if (MediaSessionService.this.mCustomMediaSessionPolicyProvider == null) {
                return false;
            }
            return TextUtils.equals(componentName, MediaSessionService.this.mCustomMediaSessionPolicyProvider.getClass().getName());
        }

        public int getSessionPolicies(MediaSession.Token token) {
            synchronized (MediaSessionService.this.mLock) {
                MediaSessionRecord record = MediaSessionService.this.getMediaSessionRecordLocked(token);
                if (record != null) {
                    return record.getSessionPolicies();
                }
                return 0;
            }
        }

        public void setSessionPolicies(MediaSession.Token token, int policies) {
            long callingIdentityToken = Binder.clearCallingIdentity();
            try {
                synchronized (MediaSessionService.this.mLock) {
                    MediaSessionRecord record = MediaSessionService.this.getMediaSessionRecordLocked(token);
                    FullUserRecord user = MediaSessionService.this.getFullUserRecordLocked(record.getUserId());
                    if (record != null && user != null) {
                        record.setSessionPolicies(policies);
                        user.mPriorityStack.updateMediaButtonSessionBySessionPolicyChange(record);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(callingIdentityToken);
            }
        }

        private int verifySessionsRequest(ComponentName componentName, int userId, int pid, int uid) {
            String packageName = null;
            if (componentName != null) {
                packageName = componentName.getPackageName();
                MediaSessionService.this.enforcePackageName(packageName, uid);
            }
            int resolvedUserId = handleIncomingUser(pid, uid, userId, packageName);
            MediaSessionService.this.enforceMediaPermissions(packageName, pid, uid, resolvedUserId);
            return resolvedUserId;
        }

        private int handleIncomingUser(int pid, int uid, int userId, String packageName) {
            int callingUserId = UserHandle.getUserHandleForUid(uid).getIdentifier();
            if (userId == callingUserId) {
                return userId;
            }
            boolean canInteractAcrossUsersFull = MediaSessionService.this.mContext.checkPermission("android.permission.INTERACT_ACROSS_USERS_FULL", pid, uid) == 0;
            if (canInteractAcrossUsersFull) {
                if (userId == UserHandle.CURRENT.getIdentifier()) {
                    return ActivityManager.getCurrentUser();
                }
                return userId;
            }
            throw new SecurityException("Permission denied while calling from " + packageName + " with user id: " + userId + "; Need to run as either the calling user id (" + callingUserId + "), or with android.permission.INTERACT_ACROSS_USERS_FULL permission");
        }

        private boolean hasEnabledNotificationListener(int callingUserId, String controllerPackageName, int controllerUid) {
            int controllerUserId = UserHandle.getUserHandleForUid(controllerUid).getIdentifier();
            if (callingUserId != controllerUserId) {
                return false;
            }
            try {
                int actualControllerUid = MediaSessionService.this.mContext.getPackageManager().getPackageUidAsUser(controllerPackageName, UserHandle.getUserId(controllerUid));
                if (controllerUid != actualControllerUid) {
                    Log.w(MediaSessionService.TAG, "Failed to check enabled notification listener. Package name and UID doesn't match");
                    return false;
                } else if (MediaSessionService.this.mNotificationManager.hasEnabledNotificationListener(controllerPackageName, UserHandle.getUserHandleForUid(controllerUid))) {
                    return true;
                } else {
                    if (MediaSessionService.DEBUG) {
                        Log.d(MediaSessionService.TAG, controllerPackageName + " (uid=" + controllerUid + ") doesn't have an enabled notification listener");
                    }
                    return false;
                }
            } catch (PackageManager.NameNotFoundException e) {
                Log.w(MediaSessionService.TAG, "Failed to check enabled notification listener. Package name doesn't exist");
                return false;
            }
        }

        private void dispatchAdjustVolumeLocked(final String packageName, final String opPackageName, final int pid, final int uid, final boolean asSystemService, final int suggestedStream, final int direction, final int flags, boolean musicOnly) {
            MediaSessionRecordImpl defaultVolumeSession;
            boolean preferSuggestedStream;
            if (MediaSessionService.this.isGlobalPriorityActiveLocked()) {
                defaultVolumeSession = MediaSessionService.this.mGlobalPrioritySession;
            } else {
                defaultVolumeSession = MediaSessionService.this.mCurrentFullUserRecord.mPriorityStack.getDefaultVolumeSession();
            }
            MediaSessionRecordImpl session = defaultVolumeSession;
            if (isValidLocalStreamType(suggestedStream) && AudioSystem.isStreamActive(suggestedStream, 0)) {
                preferSuggestedStream = true;
            } else {
                preferSuggestedStream = false;
            }
            if (session == null || preferSuggestedStream) {
                Log.d(MediaSessionService.TAG, "Adjusting suggestedStream=" + suggestedStream + " by " + direction + ". flags=" + flags + ", preferSuggestedStream=" + preferSuggestedStream + ", session=" + session);
                if (musicOnly && !AudioSystem.isStreamActive(3, 0)) {
                    Log.d(MediaSessionService.TAG, "Nothing is playing on the music stream. Skipping volume event, flags=" + flags);
                    return;
                } else {
                    MediaSessionService.this.mHandler.post(new Runnable() { // from class: com.android.server.media.MediaSessionService.SessionManagerImpl.3
                        @Override // java.lang.Runnable
                        public void run() {
                            String callingOpPackageName;
                            int callingUid;
                            int callingPid;
                            if (asSystemService) {
                                callingOpPackageName = MediaSessionService.this.mContext.getOpPackageName();
                                callingUid = Process.myUid();
                                callingPid = Process.myPid();
                            } else {
                                callingOpPackageName = opPackageName;
                                callingUid = uid;
                                callingPid = pid;
                            }
                            try {
                                MediaSessionService.this.mAudioManager.adjustSuggestedStreamVolumeForUid(suggestedStream, direction, flags, callingOpPackageName, callingUid, callingPid, MediaSessionService.this.getContext().getApplicationInfo().targetSdkVersion);
                            } catch (IllegalArgumentException | SecurityException e) {
                                Log.e(MediaSessionService.TAG, "Cannot adjust volume: direction=" + direction + ", suggestedStream=" + suggestedStream + ", flags=" + flags + ", packageName=" + packageName + ", uid=" + uid + ", asSystemService=" + asSystemService, e);
                            }
                        }
                    });
                    return;
                }
            }
            Log.d(MediaSessionService.TAG, "Adjusting " + session + " by " + direction + ". flags=" + flags + ", suggestedStream=" + suggestedStream + ", preferSuggestedStream=" + preferSuggestedStream);
            session.adjustVolume(packageName, opPackageName, pid, uid, asSystemService, direction, flags, true);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void dispatchMediaKeyEventLocked(String packageName, int pid, int uid, boolean asSystemService, KeyEvent keyEvent, boolean needWakeLock) {
            MediaSessionRecord session;
            MediaButtonReceiverHolder mediaButtonReceiverHolder;
            PendingIntent pi;
            if (MediaSessionService.this.mCurrentFullUserRecord.getMediaButtonSessionLocked() instanceof MediaSession2Record) {
                return;
            }
            MediaSessionRecord session2 = null;
            MediaButtonReceiverHolder mediaButtonReceiverHolder2 = null;
            if (MediaSessionService.this.mCustomMediaKeyDispatcher != null) {
                MediaSession.Token token = MediaSessionService.this.mCustomMediaKeyDispatcher.getMediaSession(keyEvent, uid, asSystemService);
                if (token != null) {
                    session2 = MediaSessionService.this.getMediaSessionRecordLocked(token);
                }
                if (session2 == null && (pi = MediaSessionService.this.mCustomMediaKeyDispatcher.getMediaButtonReceiver(keyEvent, uid, asSystemService)) != null) {
                    mediaButtonReceiverHolder2 = MediaButtonReceiverHolder.create(MediaSessionService.this.mCurrentFullUserRecord.mFullUserId, pi, "");
                }
            }
            if (session2 == null && mediaButtonReceiverHolder2 == null) {
                MediaSessionRecord session3 = (MediaSessionRecord) MediaSessionService.this.mCurrentFullUserRecord.getMediaButtonSessionLocked();
                if (session3 != null) {
                    session = session3;
                    mediaButtonReceiverHolder = mediaButtonReceiverHolder2;
                } else {
                    session = session3;
                    mediaButtonReceiverHolder = MediaSessionService.this.mCurrentFullUserRecord.mLastMediaButtonReceiverHolder;
                }
            } else {
                session = session2;
                mediaButtonReceiverHolder = mediaButtonReceiverHolder2;
            }
            if (session != null) {
                Log.d(MediaSessionService.TAG, "Sending " + keyEvent + " to " + session);
                if (needWakeLock) {
                    this.mKeyEventReceiver.acquireWakeLockLocked();
                }
                session.sendMediaButton(packageName, pid, uid, asSystemService, keyEvent, needWakeLock ? this.mKeyEventReceiver.mLastTimeoutId : -1, this.mKeyEventReceiver);
                try {
                    for (FullUserRecord.OnMediaKeyEventDispatchedListenerRecord cr : MediaSessionService.this.mCurrentFullUserRecord.mOnMediaKeyEventDispatchedListeners.values()) {
                        cr.callback.onMediaKeyEventDispatched(keyEvent, session.getPackageName(), session.getSessionToken());
                    }
                } catch (RemoteException e) {
                    Log.w(MediaSessionService.TAG, "Failed to send callback", e);
                }
            } else if (mediaButtonReceiverHolder != null) {
                if (needWakeLock) {
                    this.mKeyEventReceiver.acquireWakeLockLocked();
                }
                String callingPackageName = asSystemService ? MediaSessionService.this.mContext.getPackageName() : packageName;
                boolean sent = mediaButtonReceiverHolder.send(MediaSessionService.this.mContext, keyEvent, callingPackageName, needWakeLock ? this.mKeyEventReceiver.mLastTimeoutId : -1, this.mKeyEventReceiver, MediaSessionService.this.mHandler, MediaSessionDeviceConfig.getMediaButtonReceiverFgsAllowlistDurationMs());
                if (sent) {
                    String pkgName = mediaButtonReceiverHolder.getPackageName();
                    for (FullUserRecord.OnMediaKeyEventDispatchedListenerRecord cr2 : MediaSessionService.this.mCurrentFullUserRecord.mOnMediaKeyEventDispatchedListeners.values()) {
                        try {
                            cr2.callback.onMediaKeyEventDispatched(keyEvent, pkgName, (MediaSession.Token) null);
                        } catch (RemoteException e2) {
                            Log.w(MediaSessionService.TAG, "Failed notify key event dispatch, uid=" + cr2.uid, e2);
                        }
                    }
                }
            }
        }

        /* JADX DEBUG: Another duplicated slice has different insns count: {[IF]}, finally: {[IF, IGET, INVOKE, INVOKE] complete} */
        /* JADX INFO: Access modifiers changed from: private */
        public void startVoiceInput(boolean needWakeLock) {
            Intent voiceIntent;
            PowerManager pm = (PowerManager) MediaSessionService.this.mContext.getSystemService("power");
            boolean z = true;
            boolean isLocked = MediaSessionService.this.mKeyguardManager != null && MediaSessionService.this.mKeyguardManager.isKeyguardLocked();
            if (!isLocked && pm.isScreenOn()) {
                voiceIntent = new Intent("android.speech.action.WEB_SEARCH");
                Log.i(MediaSessionService.TAG, "voice-based interactions: about to use ACTION_WEB_SEARCH");
            } else {
                voiceIntent = new Intent("android.speech.action.VOICE_SEARCH_HANDS_FREE");
                if (!isLocked || !MediaSessionService.this.mKeyguardManager.isKeyguardSecure()) {
                    z = false;
                }
                voiceIntent.putExtra("android.speech.extras.EXTRA_SECURE", z);
                Log.i(MediaSessionService.TAG, "voice-based interactions: about to use ACTION_VOICE_SEARCH_HANDS_FREE");
            }
            if (needWakeLock) {
                MediaSessionService.this.mMediaEventWakeLock.acquire();
            }
            try {
                try {
                    voiceIntent.setFlags(276824064);
                    if (MediaSessionService.DEBUG) {
                        Log.d(MediaSessionService.TAG, "voiceIntent: " + voiceIntent);
                    }
                    MediaSessionService.this.mContext.startActivityAsUser(voiceIntent, UserHandle.CURRENT);
                    if (!needWakeLock) {
                        return;
                    }
                } catch (ActivityNotFoundException e) {
                    Log.w(MediaSessionService.TAG, "No activity for search: " + e);
                    if (!needWakeLock) {
                        return;
                    }
                }
                MediaSessionService.this.mMediaEventWakeLock.release();
            } catch (Throwable th) {
                if (needWakeLock) {
                    MediaSessionService.this.mMediaEventWakeLock.release();
                }
                throw th;
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public boolean isVoiceKey(int keyCode) {
            return keyCode == 79 || (!MediaSessionService.this.mHasFeatureLeanback && keyCode == 85);
        }

        private boolean isUserSetupComplete() {
            return Settings.Secure.getIntForUser(MediaSessionService.this.mContext.getContentResolver(), "user_setup_complete", 0, UserHandle.CURRENT.getIdentifier()) != 0;
        }

        private boolean isValidLocalStreamType(int streamType) {
            return streamType >= 0 && streamType <= 5;
        }

        /* loaded from: classes2.dex */
        private class MediaKeyListenerResultReceiver extends ResultReceiver implements Runnable {
            private final boolean mAsSystemService;
            private boolean mHandled;
            private final KeyEvent mKeyEvent;
            private final boolean mNeedWakeLock;
            private final String mPackageName;
            private final int mPid;
            private final int mUid;

            private MediaKeyListenerResultReceiver(String packageName, int pid, int uid, boolean asSystemService, KeyEvent keyEvent, boolean needWakeLock) {
                super(MediaSessionService.this.mHandler);
                MediaSessionService.this.mHandler.postDelayed(this, 1000L);
                this.mPackageName = packageName;
                this.mPid = pid;
                this.mUid = uid;
                this.mAsSystemService = asSystemService;
                this.mKeyEvent = keyEvent;
                this.mNeedWakeLock = needWakeLock;
            }

            @Override // java.lang.Runnable
            public void run() {
                Log.d(MediaSessionService.TAG, "The media key listener is timed-out for " + this.mKeyEvent);
                dispatchMediaKeyEvent();
            }

            @Override // android.os.ResultReceiver
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                if (resultCode == 1) {
                    this.mHandled = true;
                    MediaSessionService.this.mHandler.removeCallbacks(this);
                    return;
                }
                dispatchMediaKeyEvent();
            }

            private void dispatchMediaKeyEvent() {
                if (this.mHandled) {
                    return;
                }
                this.mHandled = true;
                MediaSessionService.this.mHandler.removeCallbacks(this);
                synchronized (MediaSessionService.this.mLock) {
                    if (MediaSessionService.this.isGlobalPriorityActiveLocked()) {
                        SessionManagerImpl.this.dispatchMediaKeyEventLocked(this.mPackageName, this.mPid, this.mUid, this.mAsSystemService, this.mKeyEvent, this.mNeedWakeLock);
                    } else {
                        SessionManagerImpl.this.mMediaKeyEventHandler.handleMediaKeyEventLocked(this.mPackageName, this.mPid, this.mUid, this.mAsSystemService, this.mKeyEvent, this.mNeedWakeLock);
                    }
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* loaded from: classes2.dex */
        public class KeyEventWakeLockReceiver extends ResultReceiver implements Runnable, PendingIntent.OnFinished {
            private final Handler mHandler;
            private int mLastTimeoutId;
            private int mRefCount;

            KeyEventWakeLockReceiver(Handler handler) {
                super(handler);
                this.mRefCount = 0;
                this.mLastTimeoutId = 0;
                this.mHandler = handler;
            }

            public void onTimeout() {
                synchronized (MediaSessionService.this.mLock) {
                    if (this.mRefCount == 0) {
                        return;
                    }
                    this.mLastTimeoutId++;
                    this.mRefCount = 0;
                    releaseWakeLockLocked();
                }
            }

            public void acquireWakeLockLocked() {
                if (this.mRefCount == 0) {
                    MediaSessionService.this.mMediaEventWakeLock.acquire();
                }
                this.mRefCount++;
                this.mHandler.removeCallbacks(this);
                this.mHandler.postDelayed(this, 5000L);
            }

            @Override // java.lang.Runnable
            public void run() {
                onTimeout();
            }

            @Override // android.os.ResultReceiver
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                if (resultCode < this.mLastTimeoutId) {
                    return;
                }
                synchronized (MediaSessionService.this.mLock) {
                    int i = this.mRefCount;
                    if (i > 0) {
                        int i2 = i - 1;
                        this.mRefCount = i2;
                        if (i2 == 0) {
                            releaseWakeLockLocked();
                        }
                    }
                }
            }

            private void releaseWakeLockLocked() {
                MediaSessionService.this.mMediaEventWakeLock.release();
                this.mHandler.removeCallbacks(this);
            }

            @Override // android.app.PendingIntent.OnFinished
            public void onSendFinished(PendingIntent pendingIntent, Intent intent, int resultCode, String resultData, Bundle resultExtras) {
                onReceiveResult(resultCode, null);
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* loaded from: classes2.dex */
        public class KeyEventHandler {
            private static final int KEY_TYPE_MEDIA = 0;
            private static final int KEY_TYPE_VOLUME = 1;
            private boolean mIsLongPressing;
            private int mKeyType;
            private Runnable mLongPressTimeoutRunnable;
            private int mMultiTapCount;
            private int mMultiTapKeyCode;
            private Runnable mMultiTapTimeoutRunnable;
            private KeyEvent mTrackingFirstDownKeyEvent;

            KeyEventHandler(int keyType) {
                this.mKeyType = keyType;
            }

            void handleMediaKeyEventLocked(String packageName, int pid, int uid, boolean asSystemService, KeyEvent keyEvent, boolean needWakeLock) {
                handleKeyEventLocked(packageName, pid, uid, asSystemService, keyEvent, needWakeLock, null, 0, false);
            }

            void handleVolumeKeyEventLocked(String packageName, int pid, int uid, boolean asSystemService, KeyEvent keyEvent, String opPackageName, int stream, boolean musicOnly) {
                handleKeyEventLocked(packageName, pid, uid, asSystemService, keyEvent, false, opPackageName, stream, musicOnly);
            }

            void handleKeyEventLocked(String packageName, int pid, int uid, boolean asSystemService, KeyEvent keyEvent, boolean needWakeLock, String opPackageName, int stream, boolean musicOnly) {
                int overriddenKeyEvents;
                if (keyEvent.isCanceled()) {
                    return;
                }
                if (MediaSessionService.this.mCustomMediaKeyDispatcher != null && MediaSessionService.this.mCustomMediaKeyDispatcher.getOverriddenKeyEvents() != null) {
                    int overriddenKeyEvents2 = MediaSessionService.this.mCustomMediaKeyDispatcher.getOverriddenKeyEvents().get(Integer.valueOf(keyEvent.getKeyCode())).intValue();
                    overriddenKeyEvents = overriddenKeyEvents2;
                } else {
                    overriddenKeyEvents = 0;
                }
                cancelTrackingIfNeeded(packageName, pid, uid, asSystemService, keyEvent, needWakeLock, opPackageName, stream, musicOnly, overriddenKeyEvents);
                if (!needTracking(keyEvent, overriddenKeyEvents)) {
                    if (this.mKeyType == 1) {
                        SessionManagerImpl.this.dispatchVolumeKeyEventLocked(packageName, opPackageName, pid, uid, asSystemService, keyEvent, stream, musicOnly);
                    } else {
                        SessionManagerImpl.this.dispatchMediaKeyEventLocked(packageName, pid, uid, asSystemService, keyEvent, needWakeLock);
                    }
                } else if (isFirstDownKeyEvent(keyEvent)) {
                    this.mTrackingFirstDownKeyEvent = keyEvent;
                    this.mIsLongPressing = false;
                } else {
                    if (isFirstLongPressKeyEvent(keyEvent)) {
                        this.mIsLongPressing = true;
                    }
                    if (this.mIsLongPressing) {
                        handleLongPressLocked(keyEvent, needWakeLock, overriddenKeyEvents);
                    } else if (keyEvent.getAction() == 1) {
                        this.mTrackingFirstDownKeyEvent = null;
                        if (shouldTrackForMultipleTapsLocked(overriddenKeyEvents)) {
                            int i = this.mMultiTapCount;
                            if (i == 0) {
                                this.mMultiTapTimeoutRunnable = createSingleTapRunnable(packageName, pid, uid, asSystemService, keyEvent, needWakeLock, opPackageName, stream, musicOnly, MediaKeyDispatcher.isSingleTapOverridden(overriddenKeyEvents));
                                if (!MediaKeyDispatcher.isSingleTapOverridden(overriddenKeyEvents) || MediaKeyDispatcher.isDoubleTapOverridden(overriddenKeyEvents) || MediaKeyDispatcher.isTripleTapOverridden(overriddenKeyEvents)) {
                                    MediaSessionService.this.mHandler.postDelayed(this.mMultiTapTimeoutRunnable, MediaSessionService.MULTI_TAP_TIMEOUT);
                                    this.mMultiTapCount = 1;
                                    this.mMultiTapKeyCode = keyEvent.getKeyCode();
                                    return;
                                }
                                this.mMultiTapTimeoutRunnable.run();
                                return;
                            } else if (i == 1) {
                                MediaSessionService.this.mHandler.removeCallbacks(this.mMultiTapTimeoutRunnable);
                                this.mMultiTapTimeoutRunnable = createDoubleTapRunnable(packageName, pid, uid, asSystemService, keyEvent, needWakeLock, opPackageName, stream, musicOnly, MediaKeyDispatcher.isSingleTapOverridden(overriddenKeyEvents), MediaKeyDispatcher.isDoubleTapOverridden(overriddenKeyEvents));
                                if (MediaKeyDispatcher.isTripleTapOverridden(overriddenKeyEvents)) {
                                    MediaSessionService.this.mHandler.postDelayed(this.mMultiTapTimeoutRunnable, MediaSessionService.MULTI_TAP_TIMEOUT);
                                    this.mMultiTapCount = 2;
                                    return;
                                }
                                this.mMultiTapTimeoutRunnable.run();
                                return;
                            } else if (i == 2) {
                                MediaSessionService.this.mHandler.removeCallbacks(this.mMultiTapTimeoutRunnable);
                                onTripleTap(keyEvent);
                                return;
                            } else {
                                return;
                            }
                        }
                        dispatchDownAndUpKeyEventsLocked(packageName, pid, uid, asSystemService, keyEvent, needWakeLock, opPackageName, stream, musicOnly);
                    }
                }
            }

            private boolean shouldTrackForMultipleTapsLocked(int overriddenKeyEvents) {
                return MediaKeyDispatcher.isSingleTapOverridden(overriddenKeyEvents) || MediaKeyDispatcher.isDoubleTapOverridden(overriddenKeyEvents) || MediaKeyDispatcher.isTripleTapOverridden(overriddenKeyEvents);
            }

            private void cancelTrackingIfNeeded(String packageName, int pid, int uid, boolean asSystemService, KeyEvent keyEvent, boolean needWakeLock, String opPackageName, int stream, boolean musicOnly, int overriddenKeyEvents) {
                if (this.mTrackingFirstDownKeyEvent == null && this.mMultiTapTimeoutRunnable == null) {
                    return;
                }
                if (isFirstDownKeyEvent(keyEvent)) {
                    if (this.mLongPressTimeoutRunnable != null) {
                        MediaSessionService.this.mHandler.removeCallbacks(this.mLongPressTimeoutRunnable);
                        this.mLongPressTimeoutRunnable.run();
                    }
                    if (this.mMultiTapTimeoutRunnable != null && keyEvent.getKeyCode() != this.mMultiTapKeyCode) {
                        runExistingMultiTapRunnableLocked();
                    }
                    resetLongPressTracking();
                    return;
                }
                KeyEvent keyEvent2 = this.mTrackingFirstDownKeyEvent;
                if (keyEvent2 != null && keyEvent2.getDownTime() == keyEvent.getDownTime() && this.mTrackingFirstDownKeyEvent.getKeyCode() == keyEvent.getKeyCode() && keyEvent.getAction() == 0) {
                    if (isFirstLongPressKeyEvent(keyEvent)) {
                        if (this.mMultiTapTimeoutRunnable != null) {
                            runExistingMultiTapRunnableLocked();
                        }
                        if ((overriddenKeyEvents & 8) == 0) {
                            if (this.mKeyType == 1) {
                                if (MediaSessionService.this.mCurrentFullUserRecord.mOnVolumeKeyLongPressListener == null) {
                                    SessionManagerImpl.this.dispatchVolumeKeyEventLocked(packageName, opPackageName, pid, uid, asSystemService, keyEvent, stream, musicOnly);
                                    this.mTrackingFirstDownKeyEvent = null;
                                }
                            } else if (!SessionManagerImpl.this.isVoiceKey(keyEvent.getKeyCode())) {
                                SessionManagerImpl.this.dispatchMediaKeyEventLocked(packageName, pid, uid, asSystemService, keyEvent, needWakeLock);
                                this.mTrackingFirstDownKeyEvent = null;
                            }
                        }
                    } else if (keyEvent.getRepeatCount() > 1 && !this.mIsLongPressing) {
                        resetLongPressTracking();
                    }
                }
            }

            private boolean needTracking(KeyEvent keyEvent, int overriddenKeyEvents) {
                KeyEvent keyEvent2;
                if (isFirstDownKeyEvent(keyEvent) || ((keyEvent2 = this.mTrackingFirstDownKeyEvent) != null && keyEvent2.getDownTime() == keyEvent.getDownTime() && this.mTrackingFirstDownKeyEvent.getKeyCode() == keyEvent.getKeyCode())) {
                    if (overriddenKeyEvents == 0) {
                        if (this.mKeyType == 1) {
                            if (MediaSessionService.this.mCurrentFullUserRecord.mOnVolumeKeyLongPressListener == null) {
                                return false;
                            }
                        } else if (!SessionManagerImpl.this.isVoiceKey(keyEvent.getKeyCode())) {
                            return false;
                        }
                    }
                    return true;
                }
                return false;
            }

            private void runExistingMultiTapRunnableLocked() {
                MediaSessionService.this.mHandler.removeCallbacks(this.mMultiTapTimeoutRunnable);
                this.mMultiTapTimeoutRunnable.run();
            }

            /* JADX INFO: Access modifiers changed from: private */
            public void resetMultiTapTrackingLocked() {
                this.mMultiTapCount = 0;
                this.mMultiTapTimeoutRunnable = null;
                this.mMultiTapKeyCode = 0;
            }

            private void handleLongPressLocked(KeyEvent keyEvent, boolean needWakeLock, int overriddenKeyEvents) {
                if (MediaSessionService.this.mCustomMediaKeyDispatcher != null && MediaKeyDispatcher.isLongPressOverridden(overriddenKeyEvents)) {
                    MediaSessionService.this.mCustomMediaKeyDispatcher.onLongPress(keyEvent);
                    if (this.mLongPressTimeoutRunnable != null) {
                        MediaSessionService.this.mHandler.removeCallbacks(this.mLongPressTimeoutRunnable);
                    }
                    if (keyEvent.getAction() == 0) {
                        if (this.mLongPressTimeoutRunnable == null) {
                            this.mLongPressTimeoutRunnable = createLongPressTimeoutRunnable(keyEvent);
                        }
                        MediaSessionService.this.mHandler.postDelayed(this.mLongPressTimeoutRunnable, MediaSessionService.LONG_PRESS_TIMEOUT);
                        return;
                    }
                    resetLongPressTracking();
                } else if (this.mKeyType == 1) {
                    if (isFirstLongPressKeyEvent(keyEvent)) {
                        MediaSessionService.this.dispatchVolumeKeyLongPressLocked(this.mTrackingFirstDownKeyEvent);
                    }
                    MediaSessionService.this.dispatchVolumeKeyLongPressLocked(keyEvent);
                } else if (isFirstLongPressKeyEvent(keyEvent) && SessionManagerImpl.this.isVoiceKey(keyEvent.getKeyCode())) {
                    SessionManagerImpl.this.startVoiceInput(needWakeLock);
                    resetLongPressTracking();
                }
            }

            private Runnable createLongPressTimeoutRunnable(final KeyEvent keyEvent) {
                return new Runnable() { // from class: com.android.server.media.MediaSessionService.SessionManagerImpl.KeyEventHandler.1
                    @Override // java.lang.Runnable
                    public void run() {
                        if (MediaSessionService.this.mCustomMediaKeyDispatcher != null) {
                            MediaSessionService.this.mCustomMediaKeyDispatcher.onLongPress(KeyEventHandler.this.createCanceledKeyEvent(keyEvent));
                        }
                        KeyEventHandler.this.resetLongPressTracking();
                    }
                };
            }

            /* JADX INFO: Access modifiers changed from: private */
            public void resetLongPressTracking() {
                this.mTrackingFirstDownKeyEvent = null;
                this.mIsLongPressing = false;
                this.mLongPressTimeoutRunnable = null;
            }

            /* JADX INFO: Access modifiers changed from: private */
            public KeyEvent createCanceledKeyEvent(KeyEvent keyEvent) {
                KeyEvent upEvent = KeyEvent.changeAction(keyEvent, 1);
                return KeyEvent.changeTimeRepeat(upEvent, System.currentTimeMillis(), 0, 32);
            }

            private boolean isFirstLongPressKeyEvent(KeyEvent keyEvent) {
                return (keyEvent.getFlags() & 128) != 0 && keyEvent.getRepeatCount() == 1;
            }

            private boolean isFirstDownKeyEvent(KeyEvent keyEvent) {
                return keyEvent.getAction() == 0 && keyEvent.getRepeatCount() == 0;
            }

            /* JADX INFO: Access modifiers changed from: private */
            public void dispatchDownAndUpKeyEventsLocked(String packageName, int pid, int uid, boolean asSystemService, KeyEvent keyEvent, boolean needWakeLock, String opPackageName, int stream, boolean musicOnly) {
                KeyEvent downEvent = KeyEvent.changeAction(keyEvent, 0);
                if (this.mKeyType == 1) {
                    SessionManagerImpl.this.dispatchVolumeKeyEventLocked(packageName, opPackageName, pid, uid, asSystemService, downEvent, stream, musicOnly);
                    SessionManagerImpl.this.dispatchVolumeKeyEventLocked(packageName, opPackageName, pid, uid, asSystemService, keyEvent, stream, musicOnly);
                    return;
                }
                SessionManagerImpl.this.dispatchMediaKeyEventLocked(packageName, pid, uid, asSystemService, downEvent, needWakeLock);
                SessionManagerImpl.this.dispatchMediaKeyEventLocked(packageName, pid, uid, asSystemService, keyEvent, needWakeLock);
            }

            Runnable createSingleTapRunnable(final String packageName, final int pid, final int uid, final boolean asSystemService, final KeyEvent keyEvent, final boolean needWakeLock, final String opPackageName, final int stream, final boolean musicOnly, final boolean overridden) {
                return new Runnable() { // from class: com.android.server.media.MediaSessionService.SessionManagerImpl.KeyEventHandler.2
                    @Override // java.lang.Runnable
                    public void run() {
                        KeyEventHandler.this.resetMultiTapTrackingLocked();
                        if (overridden) {
                            MediaSessionService.this.mCustomMediaKeyDispatcher.onSingleTap(keyEvent);
                        } else {
                            KeyEventHandler.this.dispatchDownAndUpKeyEventsLocked(packageName, pid, uid, asSystemService, keyEvent, needWakeLock, opPackageName, stream, musicOnly);
                        }
                    }
                };
            }

            Runnable createDoubleTapRunnable(final String packageName, final int pid, final int uid, final boolean asSystemService, final KeyEvent keyEvent, final boolean needWakeLock, final String opPackageName, final int stream, final boolean musicOnly, final boolean singleTapOverridden, final boolean doubleTapOverridden) {
                return new Runnable() { // from class: com.android.server.media.MediaSessionService.SessionManagerImpl.KeyEventHandler.3
                    @Override // java.lang.Runnable
                    public void run() {
                        KeyEventHandler.this.resetMultiTapTrackingLocked();
                        if (doubleTapOverridden) {
                            MediaSessionService.this.mCustomMediaKeyDispatcher.onDoubleTap(keyEvent);
                        } else if (singleTapOverridden) {
                            MediaSessionService.this.mCustomMediaKeyDispatcher.onSingleTap(keyEvent);
                            MediaSessionService.this.mCustomMediaKeyDispatcher.onSingleTap(keyEvent);
                        } else {
                            KeyEventHandler.this.dispatchDownAndUpKeyEventsLocked(packageName, pid, uid, asSystemService, keyEvent, needWakeLock, opPackageName, stream, musicOnly);
                            KeyEventHandler.this.dispatchDownAndUpKeyEventsLocked(packageName, pid, uid, asSystemService, keyEvent, needWakeLock, opPackageName, stream, musicOnly);
                        }
                    }
                };
            }

            private void onTripleTap(KeyEvent keyEvent) {
                resetMultiTapTrackingLocked();
                MediaSessionService.this.mCustomMediaKeyDispatcher.onTripleTap(keyEvent);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public final class MessageHandler extends Handler {
        private static final int MSG_SESSIONS_1_CHANGED = 1;
        private static final int MSG_SESSIONS_2_CHANGED = 2;
        private final SparseArray<Integer> mIntegerCache = new SparseArray<>();

        MessageHandler() {
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    MediaSessionService.this.pushSession1Changed(((Integer) msg.obj).intValue());
                    return;
                case 2:
                    MediaSessionService.this.pushSession2Changed(((Integer) msg.obj).intValue());
                    return;
                default:
                    return;
            }
        }

        public void postSessionsChanged(MediaSessionRecordImpl record) {
            Integer userIdInteger = this.mIntegerCache.get(record.getUserId());
            if (userIdInteger == null) {
                userIdInteger = Integer.valueOf(record.getUserId());
                this.mIntegerCache.put(record.getUserId(), userIdInteger);
            }
            int msg = record instanceof MediaSessionRecord ? 1 : 2;
            removeMessages(msg, userIdInteger);
            obtainMessage(msg, userIdInteger).sendToTarget();
        }
    }
}
