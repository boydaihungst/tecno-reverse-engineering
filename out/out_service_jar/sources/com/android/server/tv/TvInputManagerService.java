package com.android.server.tv;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.UserInfo;
import android.graphics.Rect;
import android.hardware.hdmi.HdmiDeviceInfo;
import android.media.PlaybackParams;
import android.media.tv.AdRequest;
import android.media.tv.AdResponse;
import android.media.tv.AitInfo;
import android.media.tv.BroadcastInfoRequest;
import android.media.tv.BroadcastInfoResponse;
import android.media.tv.DvbDeviceInfo;
import android.media.tv.ITvInputClient;
import android.media.tv.ITvInputHardware;
import android.media.tv.ITvInputHardwareCallback;
import android.media.tv.ITvInputManager;
import android.media.tv.ITvInputManagerCallback;
import android.media.tv.ITvInputService;
import android.media.tv.ITvInputServiceCallback;
import android.media.tv.ITvInputSession;
import android.media.tv.ITvInputSessionCallback;
import android.media.tv.TunedInfo;
import android.media.tv.TvContentRating;
import android.media.tv.TvContentRatingSystemInfo;
import android.media.tv.TvContract;
import android.media.tv.TvInputHardwareInfo;
import android.media.tv.TvInputInfo;
import android.media.tv.TvStreamConfig;
import android.media.tv.TvTrackInfo;
import android.media.tv.tunerresourcemanager.TunerResourceManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.view.InputChannel;
import android.view.Surface;
import com.android.internal.content.PackageMonitor;
import com.android.internal.os.SomeArgs;
import com.android.internal.util.CollectionUtils;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.FrameworkStatsLog;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.IoThread;
import com.android.server.SystemService;
import com.android.server.UiModeManagerService;
import com.android.server.am.HostingRecord;
import com.android.server.pm.PackageManagerService;
import com.android.server.slice.SliceClientPermissions;
import com.android.server.tv.TvInputHardwareManager;
import dalvik.annotation.optimization.NeverCompile;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/* loaded from: classes2.dex */
public final class TvInputManagerService extends SystemService {
    private static final int APP_TAG_SELF = 0;
    private static final boolean DEBUG = false;
    private static final String DVB_DIRECTORY = "/dev/dvb";
    private static final String PERMISSION_ACCESS_WATCHED_PROGRAMS = "com.android.providers.tv.permission.ACCESS_WATCHED_PROGRAMS";
    private static final String TAG = "TvInputManagerService";
    private final ActivityManager mActivityManager;
    private final Context mContext;
    private int mCurrentUserId;
    private final Object mLock;
    private final Set<Integer> mRunningProfiles;
    private final Map<String, SessionState> mSessionIdToSessionStateMap;
    private final TvInputHardwareManager mTvInputHardwareManager;
    private final UserManager mUserManager;
    private final SparseArray<UserState> mUserStates;
    private final WatchLogHandler mWatchLogHandler;
    private static final Pattern sFrontEndDevicePattern = Pattern.compile("^dvb([0-9]+)\\.frontend([0-9]+)$");
    private static final Pattern sAdapterDirPattern = Pattern.compile("^adapter([0-9]+)$");
    private static final Pattern sFrontEndInAdapterDirPattern = Pattern.compile("^frontend([0-9]+)$");

    public TvInputManagerService(Context context) {
        super(context);
        Object obj = new Object();
        this.mLock = obj;
        this.mCurrentUserId = 0;
        this.mRunningProfiles = new HashSet();
        this.mUserStates = new SparseArray<>();
        this.mSessionIdToSessionStateMap = new HashMap();
        this.mContext = context;
        this.mWatchLogHandler = new WatchLogHandler(context.getContentResolver(), IoThread.get().getLooper());
        this.mTvInputHardwareManager = new TvInputHardwareManager(context, new HardwareListener());
        this.mActivityManager = (ActivityManager) getContext().getSystemService(HostingRecord.HOSTING_TYPE_ACTIVITY);
        this.mUserManager = (UserManager) getContext().getSystemService("user");
        synchronized (obj) {
            getOrCreateUserStateLocked(this.mCurrentUserId);
        }
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        publishBinderService("tv_input", new BinderService());
    }

    @Override // com.android.server.SystemService
    public void onBootPhase(int phase) {
        if (phase == 500) {
            registerBroadcastReceivers();
        } else if (phase == 600) {
            synchronized (this.mLock) {
                buildTvInputListLocked(this.mCurrentUserId, null);
                buildTvContentRatingSystemListLocked(this.mCurrentUserId);
            }
        }
        this.mTvInputHardwareManager.onBootPhase(phase);
    }

    @Override // com.android.server.SystemService
    public void onUserUnlocking(SystemService.TargetUser user) {
        synchronized (this.mLock) {
            if (this.mCurrentUserId != user.getUserIdentifier()) {
                return;
            }
            buildTvInputListLocked(this.mCurrentUserId, null);
            buildTvContentRatingSystemListLocked(this.mCurrentUserId);
        }
    }

    private void registerBroadcastReceivers() {
        PackageMonitor monitor = new PackageMonitor() { // from class: com.android.server.tv.TvInputManagerService.1
            private void buildTvInputList(String[] packages) {
                int userId = getChangingUserId();
                synchronized (TvInputManagerService.this.mLock) {
                    if (TvInputManagerService.this.mCurrentUserId == userId || TvInputManagerService.this.mRunningProfiles.contains(Integer.valueOf(userId))) {
                        TvInputManagerService.this.buildTvInputListLocked(userId, packages);
                        TvInputManagerService.this.buildTvContentRatingSystemListLocked(userId);
                    }
                }
            }

            public void onPackageUpdateFinished(String packageName, int uid) {
                buildTvInputList(new String[]{packageName});
            }

            public void onPackagesAvailable(String[] packages) {
                if (isReplacing()) {
                    buildTvInputList(packages);
                }
            }

            public void onPackagesUnavailable(String[] packages) {
                if (isReplacing()) {
                    buildTvInputList(packages);
                }
            }

            public void onSomePackagesChanged() {
                if (isReplacing()) {
                    return;
                }
                buildTvInputList(null);
            }

            public boolean onPackageChanged(String packageName, int uid, String[] components) {
                return true;
            }
        };
        monitor.register(this.mContext, (Looper) null, UserHandle.ALL, true);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.USER_SWITCHED");
        intentFilter.addAction("android.intent.action.USER_REMOVED");
        intentFilter.addAction("android.intent.action.USER_STARTED");
        intentFilter.addAction("android.intent.action.USER_STOPPED");
        this.mContext.registerReceiverAsUser(new BroadcastReceiver() { // from class: com.android.server.tv.TvInputManagerService.2
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if ("android.intent.action.USER_SWITCHED".equals(action)) {
                    TvInputManagerService.this.switchUser(intent.getIntExtra("android.intent.extra.user_handle", 0));
                } else if ("android.intent.action.USER_REMOVED".equals(action)) {
                    TvInputManagerService.this.removeUser(intent.getIntExtra("android.intent.extra.user_handle", 0));
                } else if ("android.intent.action.USER_STARTED".equals(action)) {
                    int userId = intent.getIntExtra("android.intent.extra.user_handle", 0);
                    TvInputManagerService.this.startUser(userId);
                } else if ("android.intent.action.USER_STOPPED".equals(action)) {
                    int userId2 = intent.getIntExtra("android.intent.extra.user_handle", 0);
                    TvInputManagerService.this.stopUser(userId2);
                }
            }
        }, UserHandle.ALL, intentFilter, null, null);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean hasHardwarePermission(PackageManager pm, ComponentName component) {
        return pm.checkPermission("android.permission.TV_INPUT_HARDWARE", component.getPackageName()) == 0;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void buildTvInputListLocked(int userId, String[] updatedPackages) {
        int intValue;
        PackageManager pm;
        UserState userState = getOrCreateUserStateLocked(userId);
        userState.packageSet.clear();
        PackageManager pm2 = this.mContext.getPackageManager();
        List<ResolveInfo> services = pm2.queryIntentServicesAsUser(new Intent("android.media.tv.TvInputService"), 132, userId);
        List<TvInputInfo> inputList = new ArrayList<>();
        List<ComponentName> hardwareComponents = new ArrayList<>();
        for (ResolveInfo ri : services) {
            ServiceInfo si = ri.serviceInfo;
            if (!"android.permission.BIND_TV_INPUT".equals(si.permission)) {
                Slog.w(TAG, "Skipping TV input " + si.name + ": it does not require the permission android.permission.BIND_TV_INPUT");
            } else {
                ComponentName component = new ComponentName(si.packageName, si.name);
                if (hasHardwarePermission(pm2, component)) {
                    hardwareComponents.add(component);
                    ServiceState serviceState = (ServiceState) userState.serviceStateMap.get(component);
                    if (serviceState == null) {
                        userState.serviceStateMap.put(component, new ServiceState(component, userId));
                        updateServiceConnectionLocked(component, userId);
                    } else {
                        inputList.addAll(serviceState.hardwareInputMap.values());
                    }
                } else {
                    try {
                        TvInputInfo info = new TvInputInfo.Builder(this.mContext, ri).build();
                        inputList.add(info);
                    } catch (Exception e) {
                        Slog.e(TAG, "failed to load TV input " + si.name, e);
                    }
                }
                userState.packageSet.add(si.packageName);
            }
        }
        Collections.sort(inputList, Comparator.comparing(new Function() { // from class: com.android.server.tv.TvInputManagerService$$ExternalSyntheticLambda0
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return ((TvInputInfo) obj).getId();
            }
        }));
        Map<String, TvInputState> inputMap = new HashMap<>();
        ArrayMap<String, Integer> tisInputCount = new ArrayMap<>(inputMap.size());
        for (TvInputInfo info2 : inputList) {
            String inputId = info2.getId();
            Integer count = tisInputCount.get(inputId);
            if (count == null) {
                Integer num = 1;
                intValue = num.intValue();
            } else {
                intValue = count.intValue() + 1;
            }
            Integer count2 = Integer.valueOf(intValue);
            tisInputCount.put(inputId, count2);
            TvInputState inputState = (TvInputState) userState.inputMap.get(inputId);
            if (inputState != null) {
                pm = pm2;
            } else {
                pm = pm2;
                inputState = new TvInputState();
            }
            inputState.info = info2;
            inputState.uid = getInputUid(info2);
            inputMap.put(inputId, inputState);
            inputState.inputNumber = count2.intValue();
            pm2 = pm;
        }
        for (String inputId2 : inputMap.keySet()) {
            if (!userState.inputMap.containsKey(inputId2)) {
                notifyInputAddedLocked(userState, inputId2);
            } else if (updatedPackages != null) {
                ComponentName component2 = inputMap.get(inputId2).info.getComponent();
                int length = updatedPackages.length;
                int i = 0;
                while (true) {
                    if (i < length) {
                        String updatedPackage = updatedPackages[i];
                        if (!component2.getPackageName().equals(updatedPackage)) {
                            i++;
                        } else {
                            updateServiceConnectionLocked(component2, userId);
                            notifyInputUpdatedLocked(userState, inputId2);
                            break;
                        }
                    }
                }
            }
        }
        for (String inputId3 : userState.inputMap.keySet()) {
            if (!inputMap.containsKey(inputId3)) {
                ServiceState serviceState2 = (ServiceState) userState.serviceStateMap.get(((TvInputState) userState.inputMap.get(inputId3)).info.getComponent());
                if (serviceState2 != null) {
                    abortPendingCreateSessionRequestsLocked(serviceState2, inputId3, userId);
                }
                notifyInputRemovedLocked(userState, inputId3);
            }
        }
        Iterator<ServiceState> it = userState.serviceStateMap.values().iterator();
        while (it.hasNext()) {
            ServiceState serviceState3 = it.next();
            if (serviceState3.isHardware && !hardwareComponents.contains(serviceState3.component)) {
                it.remove();
            }
        }
        userState.inputMap.clear();
        userState.inputMap = inputMap;
    }

    private int getInputUid(TvInputInfo info) {
        try {
            return getContext().getPackageManager().getApplicationInfo(info.getServiceInfo().packageName, 0).uid;
        } catch (PackageManager.NameNotFoundException e) {
            Slog.w(TAG, "Unable to get UID for  " + info, e);
            return -1;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void buildTvContentRatingSystemListLocked(int userId) {
        UserState userState = getOrCreateUserStateLocked(userId);
        userState.contentRatingSystemList.clear();
        PackageManager pm = this.mContext.getPackageManager();
        Intent intent = new Intent("android.media.tv.action.QUERY_CONTENT_RATING_SYSTEMS");
        for (ResolveInfo resolveInfo : pm.queryBroadcastReceivers(intent, 128)) {
            ActivityInfo receiver = resolveInfo.activityInfo;
            Bundle metaData = receiver.metaData;
            if (metaData != null) {
                int xmlResId = metaData.getInt("android.media.tv.metadata.CONTENT_RATING_SYSTEMS");
                if (xmlResId == 0) {
                    Slog.w(TAG, "Missing meta-data 'android.media.tv.metadata.CONTENT_RATING_SYSTEMS' on receiver " + receiver.packageName + SliceClientPermissions.SliceAuthority.DELIMITER + receiver.name);
                } else {
                    userState.contentRatingSystemList.add(TvContentRatingSystemInfo.createTvContentRatingSystemInfo(xmlResId, receiver.applicationInfo));
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void startUser(int userId) {
        synchronized (this.mLock) {
            if (userId != this.mCurrentUserId && !this.mRunningProfiles.contains(Integer.valueOf(userId))) {
                UserInfo userInfo = this.mUserManager.getUserInfo(userId);
                UserInfo parentInfo = this.mUserManager.getProfileParent(userId);
                if (userInfo.isProfile() && parentInfo != null && parentInfo.id == this.mCurrentUserId) {
                    startProfileLocked(userId);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void stopUser(int userId) {
        synchronized (this.mLock) {
            if (userId == this.mCurrentUserId) {
                switchUser(ActivityManager.getCurrentUser());
                return;
            }
            releaseSessionOfUserLocked(userId);
            unbindServiceOfUserLocked(userId);
            this.mRunningProfiles.remove(Integer.valueOf(userId));
        }
    }

    private void startProfileLocked(int userId) {
        this.mRunningProfiles.add(Integer.valueOf(userId));
        buildTvInputListLocked(userId, null);
        buildTvContentRatingSystemListLocked(userId);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void switchUser(int userId) {
        synchronized (this.mLock) {
            if (this.mCurrentUserId == userId) {
                return;
            }
            UserInfo userInfo = this.mUserManager.getUserInfo(userId);
            if (userInfo.isProfile()) {
                Slog.w(TAG, "cannot switch to a profile!");
                return;
            }
            for (Integer num : this.mRunningProfiles) {
                int runningId = num.intValue();
                releaseSessionOfUserLocked(runningId);
                unbindServiceOfUserLocked(runningId);
            }
            this.mRunningProfiles.clear();
            releaseSessionOfUserLocked(this.mCurrentUserId);
            unbindServiceOfUserLocked(this.mCurrentUserId);
            this.mCurrentUserId = userId;
            buildTvInputListLocked(userId, null);
            buildTvContentRatingSystemListLocked(userId);
            this.mWatchLogHandler.obtainMessage(3, getContentResolverForUser(userId)).sendToTarget();
        }
    }

    /* JADX DEBUG: Another duplicated slice has different insns count: {[IF]}, finally: {[IF, INVOKE] complete} */
    private void releaseSessionOfUserLocked(int userId) {
        UserState userState = getUserStateLocked(userId);
        if (userState == null) {
            return;
        }
        List<SessionState> sessionStatesToRelease = new ArrayList<>();
        for (SessionState sessionState : userState.sessionStateMap.values()) {
            if (sessionState.session != null && !sessionState.isRecordingSession) {
                sessionStatesToRelease.add(sessionState);
            }
        }
        boolean notifyInfoUpdated = false;
        for (SessionState sessionState2 : sessionStatesToRelease) {
            try {
                try {
                    sessionState2.session.release();
                    sessionState2.currentChannel = null;
                    if (sessionState2.isCurrent) {
                        sessionState2.isCurrent = false;
                        notifyInfoUpdated = true;
                    }
                } catch (RemoteException e) {
                    Slog.e(TAG, "error in release", e);
                    if (!notifyInfoUpdated) {
                    }
                }
                if (!notifyInfoUpdated) {
                    clearSessionAndNotifyClientLocked(sessionState2);
                }
                notifyCurrentChannelInfosUpdatedLocked(userState);
                clearSessionAndNotifyClientLocked(sessionState2);
            } catch (Throwable th) {
                if (notifyInfoUpdated) {
                    notifyCurrentChannelInfosUpdatedLocked(userState);
                }
                throw th;
            }
        }
    }

    private void unbindServiceOfUserLocked(int userId) {
        UserState userState = getUserStateLocked(userId);
        if (userState == null) {
            return;
        }
        Iterator<ComponentName> it = userState.serviceStateMap.keySet().iterator();
        while (it.hasNext()) {
            ComponentName component = it.next();
            ServiceState serviceState = (ServiceState) userState.serviceStateMap.get(component);
            if (serviceState != null && serviceState.sessionTokens.isEmpty()) {
                if (serviceState.callback != null) {
                    try {
                        serviceState.service.unregisterCallback(serviceState.callback);
                    } catch (RemoteException e) {
                        Slog.e(TAG, "error in unregisterCallback", e);
                    }
                }
                this.mContext.unbindService(serviceState.connection);
                it.remove();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void clearSessionAndNotifyClientLocked(SessionState state) {
        if (state.client != null) {
            try {
                state.client.onSessionReleased(state.seq);
            } catch (RemoteException e) {
                Slog.e(TAG, "error in onSessionReleased", e);
            }
        }
        UserState userState = getOrCreateUserStateLocked(state.userId);
        for (SessionState sessionState : userState.sessionStateMap.values()) {
            if (state.sessionToken == sessionState.hardwareSessionToken) {
                releaseSessionLocked(sessionState.sessionToken, 1000, state.userId);
                try {
                    sessionState.client.onSessionReleased(sessionState.seq);
                } catch (RemoteException e2) {
                    Slog.e(TAG, "error in onSessionReleased", e2);
                }
            }
        }
        removeSessionStateLocked(state.sessionToken, state.userId);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [625=4] */
    /* JADX INFO: Access modifiers changed from: private */
    public void removeUser(int userId) {
        synchronized (this.mLock) {
            UserState userState = getUserStateLocked(userId);
            if (userState == null) {
                return;
            }
            boolean notifyInfoUpdated = false;
            for (SessionState state : userState.sessionStateMap.values()) {
                if (state.session != null) {
                    try {
                        state.session.release();
                        state.currentChannel = null;
                        if (state.isCurrent) {
                            state.isCurrent = false;
                            notifyInfoUpdated = true;
                        }
                        if (!notifyInfoUpdated) {
                        }
                    } catch (RemoteException e) {
                        Slog.e(TAG, "error in release", e);
                        if (!notifyInfoUpdated) {
                        }
                    }
                    notifyCurrentChannelInfosUpdatedLocked(userState);
                }
            }
            userState.sessionStateMap.clear();
            for (ServiceState serviceState : userState.serviceStateMap.values()) {
                if (serviceState.service != null) {
                    if (serviceState.callback != null) {
                        try {
                            serviceState.service.unregisterCallback(serviceState.callback);
                        } catch (RemoteException e2) {
                            Slog.e(TAG, "error in unregisterCallback", e2);
                        }
                    }
                    this.mContext.unbindService(serviceState.connection);
                }
            }
            userState.serviceStateMap.clear();
            userState.inputMap.clear();
            userState.packageSet.clear();
            userState.contentRatingSystemList.clear();
            userState.clientStateMap.clear();
            userState.mCallbacks.kill();
            userState.mainSessionToken = null;
            this.mRunningProfiles.remove(Integer.valueOf(userId));
            this.mUserStates.remove(userId);
            if (userId == this.mCurrentUserId) {
                switchUser(0);
            }
        }
    }

    private ContentResolver getContentResolverForUser(int userId) {
        Context context;
        UserHandle user = new UserHandle(userId);
        try {
            context = this.mContext.createPackageContextAsUser(PackageManagerService.PLATFORM_PACKAGE_NAME, 0, user);
        } catch (PackageManager.NameNotFoundException e) {
            Slog.e(TAG, "failed to create package context as user " + user);
            context = this.mContext;
        }
        return context.getContentResolver();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public UserState getOrCreateUserStateLocked(int userId) {
        UserState userState = getUserStateLocked(userId);
        if (userState == null) {
            UserState userState2 = new UserState(this.mContext, userId);
            this.mUserStates.put(userId, userState2);
            return userState2;
        }
        return userState;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public ServiceState getServiceStateLocked(ComponentName component, int userId) {
        UserState userState = getOrCreateUserStateLocked(userId);
        ServiceState serviceState = (ServiceState) userState.serviceStateMap.get(component);
        if (serviceState == null) {
            throw new IllegalStateException("Service state not found for " + component + " (userId=" + userId + ")");
        }
        return serviceState;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public SessionState getSessionStateLocked(IBinder sessionToken, int callingUid, int userId) {
        UserState userState = getOrCreateUserStateLocked(userId);
        return getSessionStateLocked(sessionToken, callingUid, userState);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public SessionState getSessionStateLocked(IBinder sessionToken, int callingUid, UserState userState) {
        SessionState sessionState = (SessionState) userState.sessionStateMap.get(sessionToken);
        if (sessionState == null) {
            throw new SessionNotFoundException("Session state not found for token " + sessionToken);
        }
        if (callingUid != 1000 && callingUid != sessionState.callingUid) {
            throw new SecurityException("Illegal access to the session with token " + sessionToken + " from uid " + callingUid);
        }
        return sessionState;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public ITvInputSession getSessionLocked(IBinder sessionToken, int callingUid, int userId) {
        return getSessionLocked(getSessionStateLocked(sessionToken, callingUid, userId));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public ITvInputSession getSessionLocked(SessionState sessionState) {
        ITvInputSession session = sessionState.session;
        if (session == null) {
            throw new IllegalStateException("Session not yet created for token " + sessionState.sessionToken);
        }
        return session;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int resolveCallingUserId(int callingPid, int callingUid, int requestedUserId, String methodName) {
        return ActivityManager.handleIncomingUser(callingPid, callingUid, requestedUserId, false, false, methodName, null);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateServiceConnectionLocked(ComponentName component, int userId) {
        boolean shouldBind;
        UserState userState = getOrCreateUserStateLocked(userId);
        ServiceState serviceState = (ServiceState) userState.serviceStateMap.get(component);
        if (serviceState == null) {
            return;
        }
        boolean z = false;
        if (serviceState.reconnecting) {
            if (!serviceState.sessionTokens.isEmpty()) {
                return;
            }
            serviceState.reconnecting = false;
        }
        if (userId == this.mCurrentUserId || this.mRunningProfiles.contains(Integer.valueOf(userId))) {
            if (!serviceState.sessionTokens.isEmpty() || serviceState.isHardware) {
                z = true;
            }
            shouldBind = z;
        } else {
            shouldBind = !serviceState.sessionTokens.isEmpty();
        }
        if (serviceState.service == null && shouldBind) {
            if (serviceState.bound) {
                return;
            }
            Intent i = new Intent("android.media.tv.TvInputService").setComponent(component);
            serviceState.bound = this.mContext.bindServiceAsUser(i, serviceState.connection, 33554433, new UserHandle(userId));
        } else if (serviceState.service != null && !shouldBind) {
            this.mContext.unbindService(serviceState.connection);
            userState.serviceStateMap.remove(component);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void abortPendingCreateSessionRequestsLocked(ServiceState serviceState, String inputId, int userId) {
        UserState userState = getOrCreateUserStateLocked(userId);
        List<SessionState> sessionsToAbort = new ArrayList<>();
        for (IBinder sessionToken : serviceState.sessionTokens) {
            SessionState sessionState = (SessionState) userState.sessionStateMap.get(sessionToken);
            if (sessionState.session == null && (inputId == null || sessionState.inputId.equals(inputId))) {
                sessionsToAbort.add(sessionState);
            }
        }
        for (SessionState sessionState2 : sessionsToAbort) {
            removeSessionStateLocked(sessionState2.sessionToken, sessionState2.userId);
            sendSessionTokenToClientLocked(sessionState2.client, sessionState2.inputId, null, null, sessionState2.seq);
        }
        updateServiceConnectionLocked(serviceState.component, userId);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean createSessionInternalLocked(ITvInputService service, IBinder sessionToken, int userId) {
        UserState userState = getOrCreateUserStateLocked(userId);
        SessionState sessionState = (SessionState) userState.sessionStateMap.get(sessionToken);
        InputChannel[] channels = InputChannel.openInputChannelPair(sessionToken.toString());
        SessionCallback sessionCallback = new SessionCallback(sessionState, channels);
        boolean created = true;
        try {
            if (sessionState.isRecordingSession) {
                service.createRecordingSession(sessionCallback, sessionState.inputId, sessionState.sessionId);
            } else {
                service.createSession(channels[1], sessionCallback, sessionState.inputId, sessionState.sessionId);
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "error in createSession", e);
            sendSessionTokenToClientLocked(sessionState.client, sessionState.inputId, null, null, sessionState.seq);
            created = false;
        }
        channels[1].dispose();
        return created;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendSessionTokenToClientLocked(ITvInputClient client, String inputId, IBinder sessionToken, InputChannel channel, int seq) {
        try {
            client.onSessionCreated(inputId, sessionToken, channel, seq);
        } catch (RemoteException e) {
            Slog.e(TAG, "error in onSessionCreated", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Code restructure failed: missing block: B:18:0x004d, code lost:
        if (r0 == null) goto L15;
     */
    /* JADX WARN: Code restructure failed: missing block: B:20:0x0050, code lost:
        removeSessionStateLocked(r6, r8);
     */
    /* JADX WARN: Code restructure failed: missing block: B:21:0x0053, code lost:
        return r0;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public SessionState releaseSessionLocked(IBinder sessionToken, int callingUid, int userId) {
        SessionState sessionState = null;
        try {
            try {
                sessionState = getSessionStateLocked(sessionToken, callingUid, userId);
                UserState userState = getOrCreateUserStateLocked(userId);
                if (sessionState.session != null) {
                    if (sessionToken == userState.mainSessionToken) {
                        setMainLocked(sessionToken, false, callingUid, userId);
                    }
                    sessionState.session.asBinder().unlinkToDeath(sessionState, 0);
                    sessionState.session.release();
                }
                sessionState.currentChannel = null;
                if (sessionState.isCurrent) {
                    sessionState.isCurrent = false;
                    notifyCurrentChannelInfosUpdatedLocked(userState);
                }
            } catch (RemoteException | SessionNotFoundException e) {
                Slog.e(TAG, "error in releaseSession", e);
            }
        } finally {
            if (sessionState != null) {
                sessionState.session = null;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void removeSessionStateLocked(IBinder sessionToken, int userId) {
        UserState userState = getOrCreateUserStateLocked(userId);
        if (sessionToken == userState.mainSessionToken) {
            userState.mainSessionToken = null;
        }
        SessionState sessionState = (SessionState) userState.sessionStateMap.remove(sessionToken);
        if (sessionState == null) {
            Slog.e(TAG, "sessionState null, no more remove session action!");
            return;
        }
        ClientState clientState = (ClientState) userState.clientStateMap.get(sessionState.client.asBinder());
        if (clientState != null) {
            clientState.sessionTokens.remove(sessionToken);
            if (clientState.isEmpty()) {
                userState.clientStateMap.remove(sessionState.client.asBinder());
                sessionState.client.asBinder().unlinkToDeath(clientState, 0);
            }
        }
        this.mSessionIdToSessionStateMap.remove(sessionState.sessionId);
        ServiceState serviceState = (ServiceState) userState.serviceStateMap.get(sessionState.componentName);
        if (serviceState != null) {
            serviceState.sessionTokens.remove(sessionToken);
        }
        updateServiceConnectionLocked(sessionState.componentName, userId);
        SomeArgs args = SomeArgs.obtain();
        args.arg1 = sessionToken;
        args.arg2 = Long.valueOf(System.currentTimeMillis());
        this.mWatchLogHandler.obtainMessage(2, args).sendToTarget();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setMainLocked(IBinder sessionToken, boolean isMain, int callingUid, int userId) {
        try {
            SessionState sessionState = getSessionStateLocked(sessionToken, callingUid, userId);
            if (sessionState.hardwareSessionToken != null) {
                sessionState = getSessionStateLocked(sessionState.hardwareSessionToken, 1000, userId);
            }
            ServiceState serviceState = getServiceStateLocked(sessionState.componentName, userId);
            if (!serviceState.isHardware) {
                return;
            }
            ITvInputSession session = getSessionLocked(sessionState);
            session.setMain(isMain);
            if (sessionState.isMainSession != isMain) {
                UserState userState = getUserStateLocked(userId);
                sessionState.isMainSession = isMain;
                notifyCurrentChannelInfosUpdatedLocked(userState);
            }
        } catch (RemoteException | SessionNotFoundException e) {
            Slog.e(TAG, "error in setMain", e);
        }
    }

    private void notifyInputAddedLocked(UserState userState, String inputId) {
        int n = userState.mCallbacks.beginBroadcast();
        for (int i = 0; i < n; i++) {
            try {
                userState.mCallbacks.getBroadcastItem(i).onInputAdded(inputId);
            } catch (RemoteException e) {
                Slog.e(TAG, "failed to report added input to callback", e);
            }
        }
        userState.mCallbacks.finishBroadcast();
    }

    private void notifyInputRemovedLocked(UserState userState, String inputId) {
        int n = userState.mCallbacks.beginBroadcast();
        for (int i = 0; i < n; i++) {
            try {
                userState.mCallbacks.getBroadcastItem(i).onInputRemoved(inputId);
            } catch (RemoteException e) {
                Slog.e(TAG, "failed to report removed input to callback", e);
            }
        }
        userState.mCallbacks.finishBroadcast();
    }

    private void notifyInputUpdatedLocked(UserState userState, String inputId) {
        int n = userState.mCallbacks.beginBroadcast();
        for (int i = 0; i < n; i++) {
            try {
                userState.mCallbacks.getBroadcastItem(i).onInputUpdated(inputId);
            } catch (RemoteException e) {
                Slog.e(TAG, "failed to report updated input to callback", e);
            }
        }
        userState.mCallbacks.finishBroadcast();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyInputStateChangedLocked(UserState userState, String inputId, int state, ITvInputManagerCallback targetCallback) {
        if (targetCallback == null) {
            int n = userState.mCallbacks.beginBroadcast();
            for (int i = 0; i < n; i++) {
                try {
                    userState.mCallbacks.getBroadcastItem(i).onInputStateChanged(inputId, state);
                } catch (RemoteException e) {
                    Slog.e(TAG, "failed to report state change to callback", e);
                }
            }
            userState.mCallbacks.finishBroadcast();
            return;
        }
        try {
            targetCallback.onInputStateChanged(inputId, state);
        } catch (RemoteException e2) {
            Slog.e(TAG, "failed to report state change to callback", e2);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyCurrentChannelInfosUpdatedLocked(UserState userState) {
        int n = userState.mCallbacks.beginBroadcast();
        for (int i = 0; i < n; i++) {
            try {
                ITvInputManagerCallback callback = userState.mCallbacks.getBroadcastItem(i);
                Pair<Integer, Integer> pidUid = (Pair) userState.callbackPidUidMap.get(callback);
                if (this.mContext.checkPermission("android.permission.ACCESS_TUNED_INFO", ((Integer) pidUid.first).intValue(), ((Integer) pidUid.second).intValue()) == 0) {
                    List<TunedInfo> infos = getCurrentTunedInfosInternalLocked(userState, ((Integer) pidUid.first).intValue(), ((Integer) pidUid.second).intValue());
                    callback.onCurrentTunedInfosUpdated(infos);
                }
            } catch (RemoteException e) {
                Slog.e(TAG, "failed to report updated current channel infos to callback", e);
            }
        }
        userState.mCallbacks.finishBroadcast();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateTvInputInfoLocked(UserState userState, TvInputInfo inputInfo) {
        String inputId = inputInfo.getId();
        TvInputState inputState = (TvInputState) userState.inputMap.get(inputId);
        if (inputState == null) {
            Slog.e(TAG, "failed to set input info - unknown input id " + inputId);
            return;
        }
        inputState.info = inputInfo;
        inputState.uid = getInputUid(inputInfo);
        int n = userState.mCallbacks.beginBroadcast();
        for (int i = 0; i < n; i++) {
            try {
                userState.mCallbacks.getBroadcastItem(i).onTvInputInfoUpdated(inputInfo);
            } catch (RemoteException e) {
                Slog.e(TAG, "failed to report updated input info to callback", e);
            }
        }
        userState.mCallbacks.finishBroadcast();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setStateLocked(String inputId, int state, int userId) {
        UserState userState = getOrCreateUserStateLocked(userId);
        TvInputState inputState = (TvInputState) userState.inputMap.get(inputId);
        if (inputState == null) {
            Slog.e(TAG, "failed to setStateLocked - unknown input id " + inputId);
            return;
        }
        ServiceState serviceState = (ServiceState) userState.serviceStateMap.get(inputState.info.getComponent());
        int oldState = inputState.state;
        inputState.state = state;
        if ((serviceState == null || serviceState.service != null || (serviceState.sessionTokens.isEmpty() && !serviceState.isHardware)) && oldState != state) {
            notifyInputStateChangedLocked(userState, inputId, state, null);
        }
    }

    /* loaded from: classes2.dex */
    private final class BinderService extends ITvInputManager.Stub {
        private BinderService() {
        }

        public List<TvInputInfo> getTvInputList(int userId) {
            List<TvInputInfo> inputList;
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), Binder.getCallingUid(), userId, "getTvInputList");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    UserState userState = TvInputManagerService.this.getOrCreateUserStateLocked(resolvedUserId);
                    inputList = new ArrayList<>();
                    for (TvInputState state : userState.inputMap.values()) {
                        inputList.add(state.info);
                    }
                }
                return inputList;
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public TvInputInfo getTvInputInfo(String inputId, int userId) {
            TvInputInfo tvInputInfo;
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), Binder.getCallingUid(), userId, "getTvInputInfo");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    UserState userState = TvInputManagerService.this.getOrCreateUserStateLocked(resolvedUserId);
                    TvInputState state = (TvInputState) userState.inputMap.get(inputId);
                    tvInputInfo = state == null ? null : state.info;
                }
                return tvInputInfo;
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void updateTvInputInfo(TvInputInfo inputInfo, int userId) {
            String inputInfoPackageName = inputInfo.getServiceInfo().packageName;
            String callingPackageName = getCallingPackageName();
            if (!TextUtils.equals(inputInfoPackageName, callingPackageName) && TvInputManagerService.this.mContext.checkCallingPermission("android.permission.WRITE_SECURE_SETTINGS") != 0) {
                throw new IllegalArgumentException("calling package " + callingPackageName + " is not allowed to change TvInputInfo for " + inputInfoPackageName);
            }
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), Binder.getCallingUid(), userId, "updateTvInputInfo");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    UserState userState = TvInputManagerService.this.getOrCreateUserStateLocked(resolvedUserId);
                    TvInputManagerService.this.updateTvInputInfoLocked(userState, inputInfo);
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        private String getCallingPackageName() {
            String[] packages = TvInputManagerService.this.mContext.getPackageManager().getPackagesForUid(Binder.getCallingUid());
            if (packages != null && packages.length > 0) {
                return packages[0];
            }
            return UiModeManagerService.Shell.NIGHT_MODE_STR_UNKNOWN;
        }

        public int getTvInputState(String inputId, int userId) {
            int i;
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), Binder.getCallingUid(), userId, "getTvInputState");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    UserState userState = TvInputManagerService.this.getOrCreateUserStateLocked(resolvedUserId);
                    TvInputState state = (TvInputState) userState.inputMap.get(inputId);
                    i = state == null ? 0 : state.state;
                }
                return i;
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public List<String> getAvailableExtensionInterfaceNames(String inputId, int userId) {
            ServiceState serviceState;
            ensureTisExtensionInterfacePermission();
            int callingUid = Binder.getCallingUid();
            int callingPid = Binder.getCallingPid();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(callingPid, callingUid, userId, "getAvailableExtensionInterfaceNames");
            long identity = Binder.clearCallingIdentity();
            ITvInputService service = null;
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    UserState userState = TvInputManagerService.this.getOrCreateUserStateLocked(resolvedUserId);
                    TvInputState inputState = (TvInputState) userState.inputMap.get(inputId);
                    if (inputState != null && (serviceState = (ServiceState) userState.serviceStateMap.get(inputState.info.getComponent())) != null && serviceState.isHardware && serviceState.service != null) {
                        service = serviceState.service;
                    }
                }
                if (service != null) {
                    try {
                        List<String> interfaces = new ArrayList<>();
                        for (String name : CollectionUtils.emptyIfNull(service.getAvailableExtensionInterfaceNames())) {
                            String permission = service.getExtensionInterfacePermission(name);
                            if (permission == null || TvInputManagerService.this.mContext.checkPermission(permission, callingPid, callingUid) == 0) {
                                interfaces.add(name);
                            }
                        }
                        return interfaces;
                    } catch (RemoteException e) {
                        Slog.e(TvInputManagerService.TAG, "error in getAvailableExtensionInterfaceNames or getExtensionInterfacePermission", e);
                    }
                }
                return new ArrayList();
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public IBinder getExtensionInterface(String inputId, String name, int userId) {
            ServiceState serviceState;
            ensureTisExtensionInterfacePermission();
            int callingUid = Binder.getCallingUid();
            int callingPid = Binder.getCallingPid();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(callingPid, callingUid, userId, "getExtensionInterface");
            long identity = Binder.clearCallingIdentity();
            ITvInputService service = null;
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    UserState userState = TvInputManagerService.this.getOrCreateUserStateLocked(resolvedUserId);
                    TvInputState inputState = (TvInputState) userState.inputMap.get(inputId);
                    if (inputState != null && (serviceState = (ServiceState) userState.serviceStateMap.get(inputState.info.getComponent())) != null && serviceState.isHardware && serviceState.service != null) {
                        service = serviceState.service;
                    }
                }
                if (service != null) {
                    try {
                        String permission = service.getExtensionInterfacePermission(name);
                        if (permission == null || TvInputManagerService.this.mContext.checkPermission(permission, callingPid, callingUid) == 0) {
                            return service.getExtensionInterface(name);
                        }
                    } catch (RemoteException e) {
                        Slog.e(TvInputManagerService.TAG, "error in getExtensionInterfacePermission or getExtensionInterface", e);
                    }
                }
                return null;
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public List<TvContentRatingSystemInfo> getTvContentRatingSystemList(int userId) {
            List<TvContentRatingSystemInfo> list;
            if (TvInputManagerService.this.mContext.checkCallingPermission("android.permission.READ_CONTENT_RATING_SYSTEMS") != 0) {
                throw new SecurityException("The caller does not have permission to read content rating systems");
            }
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), Binder.getCallingUid(), userId, "getTvContentRatingSystemList");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    UserState userState = TvInputManagerService.this.getOrCreateUserStateLocked(resolvedUserId);
                    list = userState.contentRatingSystemList;
                }
                return list;
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void sendTvInputNotifyIntent(Intent intent, int userId) {
            if (TvInputManagerService.this.mContext.checkCallingPermission("android.permission.NOTIFY_TV_INPUTS") != 0) {
                throw new SecurityException("The caller: " + getCallingPackageName() + " doesn't have permission: android.permission.NOTIFY_TV_INPUTS");
            }
            if (TextUtils.isEmpty(intent.getPackage())) {
                throw new IllegalArgumentException("Must specify package name to notify.");
            }
            String action = intent.getAction();
            char c = 65535;
            switch (action.hashCode()) {
                case -160295064:
                    if (action.equals("android.media.tv.action.WATCH_NEXT_PROGRAM_BROWSABLE_DISABLED")) {
                        c = 1;
                        break;
                    }
                    break;
                case 1568780589:
                    if (action.equals("android.media.tv.action.PREVIEW_PROGRAM_BROWSABLE_DISABLED")) {
                        c = 0;
                        break;
                    }
                    break;
                case 2011523553:
                    if (action.equals("android.media.tv.action.PREVIEW_PROGRAM_ADDED_TO_WATCH_NEXT")) {
                        c = 2;
                        break;
                    }
                    break;
            }
            switch (c) {
                case 0:
                    if (intent.getLongExtra("android.media.tv.extra.PREVIEW_PROGRAM_ID", -1L) < 0) {
                        throw new IllegalArgumentException("Invalid preview program ID.");
                    }
                    break;
                case 1:
                    if (intent.getLongExtra("android.media.tv.extra.WATCH_NEXT_PROGRAM_ID", -1L) < 0) {
                        throw new IllegalArgumentException("Invalid watch next program ID.");
                    }
                    break;
                case 2:
                    if (intent.getLongExtra("android.media.tv.extra.PREVIEW_PROGRAM_ID", -1L) < 0) {
                        throw new IllegalArgumentException("Invalid preview program ID.");
                    }
                    if (intent.getLongExtra("android.media.tv.extra.WATCH_NEXT_PROGRAM_ID", -1L) < 0) {
                        throw new IllegalArgumentException("Invalid watch next program ID.");
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Invalid TV input notifying action: " + intent.getAction());
            }
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), Binder.getCallingUid(), userId, "sendTvInputNotifyIntent");
            long identity = Binder.clearCallingIdentity();
            try {
                TvInputManagerService.this.getContext().sendBroadcastAsUser(intent, new UserHandle(resolvedUserId));
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void registerCallback(ITvInputManagerCallback callback, int userId) {
            int callingPid = Binder.getCallingPid();
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(callingPid, callingUid, userId, "registerCallback");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    UserState userState = TvInputManagerService.this.getOrCreateUserStateLocked(resolvedUserId);
                    if (!userState.mCallbacks.register(callback)) {
                        Slog.e(TvInputManagerService.TAG, "client process has already died");
                    } else {
                        userState.callbackPidUidMap.put(callback, Pair.create(Integer.valueOf(callingPid), Integer.valueOf(callingUid)));
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void unregisterCallback(ITvInputManagerCallback callback, int userId) {
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), Binder.getCallingUid(), userId, "unregisterCallback");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    UserState userState = TvInputManagerService.this.getOrCreateUserStateLocked(resolvedUserId);
                    userState.mCallbacks.unregister(callback);
                    userState.callbackPidUidMap.remove(callback);
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public boolean isParentalControlsEnabled(int userId) {
            boolean isParentalControlsEnabled;
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), Binder.getCallingUid(), userId, "isParentalControlsEnabled");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    UserState userState = TvInputManagerService.this.getOrCreateUserStateLocked(resolvedUserId);
                    isParentalControlsEnabled = userState.persistentDataStore.isParentalControlsEnabled();
                }
                return isParentalControlsEnabled;
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void setParentalControlsEnabled(boolean enabled, int userId) {
            ensureParentalControlsPermission();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), Binder.getCallingUid(), userId, "setParentalControlsEnabled");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    UserState userState = TvInputManagerService.this.getOrCreateUserStateLocked(resolvedUserId);
                    userState.persistentDataStore.setParentalControlsEnabled(enabled);
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public boolean isRatingBlocked(String rating, int userId) {
            boolean isRatingBlocked;
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), Binder.getCallingUid(), userId, "isRatingBlocked");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    UserState userState = TvInputManagerService.this.getOrCreateUserStateLocked(resolvedUserId);
                    isRatingBlocked = userState.persistentDataStore.isRatingBlocked(TvContentRating.unflattenFromString(rating));
                }
                return isRatingBlocked;
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public List<String> getBlockedRatings(int userId) {
            List<String> ratings;
            TvContentRating[] blockedRatings;
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), Binder.getCallingUid(), userId, "getBlockedRatings");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    UserState userState = TvInputManagerService.this.getOrCreateUserStateLocked(resolvedUserId);
                    ratings = new ArrayList<>();
                    for (TvContentRating rating : userState.persistentDataStore.getBlockedRatings()) {
                        ratings.add(rating.flattenToString());
                    }
                }
                return ratings;
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void addBlockedRating(String rating, int userId) {
            ensureParentalControlsPermission();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), Binder.getCallingUid(), userId, "addBlockedRating");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    UserState userState = TvInputManagerService.this.getOrCreateUserStateLocked(resolvedUserId);
                    userState.persistentDataStore.addBlockedRating(TvContentRating.unflattenFromString(rating));
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void removeBlockedRating(String rating, int userId) {
            ensureParentalControlsPermission();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), Binder.getCallingUid(), userId, "removeBlockedRating");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    UserState userState = TvInputManagerService.this.getOrCreateUserStateLocked(resolvedUserId);
                    userState.persistentDataStore.removeBlockedRating(TvContentRating.unflattenFromString(rating));
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        private void ensureParentalControlsPermission() {
            if (TvInputManagerService.this.mContext.checkCallingPermission("android.permission.MODIFY_PARENTAL_CONTROLS") != 0) {
                throw new SecurityException("The caller does not have parental controls permission");
            }
        }

        /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1567=4, 1569=6] */
        public void createSession(ITvInputClient client, String inputId, boolean isRecordingSession, int seq, int userId) {
            ServiceState serviceState;
            int callingUid = Binder.getCallingUid();
            int callingPid = Binder.getCallingPid();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(callingPid, callingUid, userId, "createSession");
            long identity = Binder.clearCallingIdentity();
            String uniqueSessionId = UUID.randomUUID().toString();
            try {
                try {
                    synchronized (TvInputManagerService.this.mLock) {
                        try {
                            if (userId != TvInputManagerService.this.mCurrentUserId) {
                                try {
                                    if (!TvInputManagerService.this.mRunningProfiles.contains(Integer.valueOf(userId)) && !isRecordingSession) {
                                        TvInputManagerService.this.sendSessionTokenToClientLocked(client, inputId, null, null, seq);
                                        Binder.restoreCallingIdentity(identity);
                                        return;
                                    }
                                } catch (Throwable th) {
                                    th = th;
                                    try {
                                        throw th;
                                    } catch (Throwable th2) {
                                        th = th2;
                                        Binder.restoreCallingIdentity(identity);
                                        throw th;
                                    }
                                }
                            }
                            UserState userState = TvInputManagerService.this.getOrCreateUserStateLocked(resolvedUserId);
                            TvInputState inputState = (TvInputState) userState.inputMap.get(inputId);
                            if (inputState == null) {
                                Slog.w(TvInputManagerService.TAG, "Failed to find input state for inputId=" + inputId);
                                TvInputManagerService.this.sendSessionTokenToClientLocked(client, inputId, null, null, seq);
                                Binder.restoreCallingIdentity(identity);
                                return;
                            }
                            TvInputInfo info = inputState.info;
                            ServiceState serviceState2 = (ServiceState) userState.serviceStateMap.get(info.getComponent());
                            if (serviceState2 == null) {
                                int i = PackageManager.getApplicationInfoAsUserCached(info.getComponent().getPackageName(), 0L, resolvedUserId).uid;
                                ServiceState serviceState3 = new ServiceState(info.getComponent(), resolvedUserId);
                                userState.serviceStateMap.put(info.getComponent(), serviceState3);
                                serviceState = serviceState3;
                            } else {
                                serviceState = serviceState2;
                            }
                            if (serviceState.reconnecting) {
                                TvInputManagerService.this.sendSessionTokenToClientLocked(client, inputId, null, null, seq);
                                Binder.restoreCallingIdentity(identity);
                                return;
                            }
                            Binder binder = new Binder();
                            try {
                                SessionState sessionState = new SessionState(binder, info.getId(), info.getComponent(), isRecordingSession, client, seq, callingUid, callingPid, resolvedUserId, uniqueSessionId);
                                userState.sessionStateMap.put(binder, sessionState);
                                TvInputManagerService.this.mSessionIdToSessionStateMap.put(uniqueSessionId, sessionState);
                                serviceState.sessionTokens.add(binder);
                                if (serviceState.service == null) {
                                    TvInputManagerService.this.updateServiceConnectionLocked(info.getComponent(), resolvedUserId);
                                } else if (!TvInputManagerService.this.createSessionInternalLocked(serviceState.service, binder, resolvedUserId)) {
                                    TvInputManagerService.this.removeSessionStateLocked(binder, resolvedUserId);
                                }
                                TvInputManagerService.this.logTuneStateChanged(1, sessionState, inputState);
                                Binder.restoreCallingIdentity(identity);
                            } catch (Throwable th3) {
                                th = th3;
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

        public void releaseSession(IBinder sessionToken, int userId) {
            SessionState sessionState;
            UserState userState;
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "releaseSession");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    sessionState = TvInputManagerService.this.releaseSessionLocked(sessionToken, callingUid, resolvedUserId);
                    userState = TvInputManagerService.this.getUserStateLocked(userId);
                }
                if (sessionState != null) {
                    TvInputState tvInputState = TvInputManagerService.getTvInputState(sessionState, userState);
                    TvInputManagerService.this.logTuneStateChanged(4, sessionState, tvInputState);
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void setMainSession(IBinder sessionToken, int userId) {
            if (TvInputManagerService.this.mContext.checkCallingPermission("android.permission.CHANGE_HDMI_CEC_ACTIVE_SOURCE") != 0) {
                throw new SecurityException("The caller does not have CHANGE_HDMI_CEC_ACTIVE_SOURCE permission");
            }
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "setMainSession");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    UserState userState = TvInputManagerService.this.getOrCreateUserStateLocked(resolvedUserId);
                    if (userState.mainSessionToken == sessionToken) {
                        return;
                    }
                    IBinder oldMainSessionToken = userState.mainSessionToken;
                    userState.mainSessionToken = sessionToken;
                    if (sessionToken != null) {
                        TvInputManagerService.this.setMainLocked(sessionToken, true, callingUid, userId);
                    }
                    if (oldMainSessionToken != null) {
                        TvInputManagerService.this.setMainLocked(oldMainSessionToken, false, 1000, userId);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1680=4] */
        public void setSurface(IBinder sessionToken, Surface surface, int userId) {
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "setSurface");
            long identity = Binder.clearCallingIdentity();
            SessionState sessionState = null;
            UserState userState = null;
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    try {
                        userState = TvInputManagerService.this.getUserStateLocked(userId);
                        sessionState = TvInputManagerService.this.getSessionStateLocked(sessionToken, callingUid, resolvedUserId);
                        if (sessionState.hardwareSessionToken == null) {
                            TvInputManagerService.this.getSessionLocked(sessionState).setSurface(surface);
                        } else {
                            TvInputManagerService.this.getSessionLocked(sessionState.hardwareSessionToken, 1000, resolvedUserId).setSurface(surface);
                        }
                        boolean isVisible = surface == null;
                        if (sessionState.isVisible != isVisible) {
                            sessionState.isVisible = isVisible;
                            TvInputManagerService.this.notifyCurrentChannelInfosUpdatedLocked(userState);
                        }
                    } catch (RemoteException | SessionNotFoundException e) {
                        Slog.e(TvInputManagerService.TAG, "error in setSurface", e);
                    }
                }
            } finally {
                if (surface != null) {
                    surface.release();
                }
                if (sessionState != null) {
                    state = surface != null ? 2 : 3;
                    TvInputManagerService.this.logTuneStateChanged(state, sessionState, TvInputManagerService.getTvInputState(sessionState, userState));
                }
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void dispatchSurfaceChanged(IBinder sessionToken, int format, int width, int height, int userId) {
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "dispatchSurfaceChanged");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    try {
                        SessionState sessionState = TvInputManagerService.this.getSessionStateLocked(sessionToken, callingUid, resolvedUserId);
                        TvInputManagerService.this.getSessionLocked(sessionState).dispatchSurfaceChanged(format, width, height);
                        if (sessionState.hardwareSessionToken != null) {
                            TvInputManagerService.this.getSessionLocked(sessionState.hardwareSessionToken, 1000, resolvedUserId).dispatchSurfaceChanged(format, width, height);
                        }
                    } catch (RemoteException | SessionNotFoundException e) {
                        Slog.e(TvInputManagerService.TAG, "error in dispatchSurfaceChanged", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void setVolume(IBinder sessionToken, float volume, int userId) {
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "setVolume");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    try {
                        SessionState sessionState = TvInputManagerService.this.getSessionStateLocked(sessionToken, callingUid, resolvedUserId);
                        TvInputManagerService.this.getSessionLocked(sessionState).setVolume(volume);
                        if (sessionState.hardwareSessionToken != null) {
                            ITvInputSession sessionLocked = TvInputManagerService.this.getSessionLocked(sessionState.hardwareSessionToken, 1000, resolvedUserId);
                            float f = 0.0f;
                            if (volume > 0.0f) {
                                f = 1.0f;
                            }
                            sessionLocked.setVolume(f);
                        }
                    } catch (RemoteException | SessionNotFoundException e) {
                        Slog.e(TvInputManagerService.TAG, "error in setVolume", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1791=4] */
        public void tune(IBinder sessionToken, Uri channelUri, Bundle params, int userId) {
            UserState userState;
            SessionState sessionState;
            int callingUid = Binder.getCallingUid();
            int callingPid = Binder.getCallingPid();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(callingPid, callingUid, userId, "tune");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    try {
                        TvInputManagerService.this.getSessionLocked(sessionToken, callingUid, resolvedUserId).tune(channelUri, params);
                        userState = TvInputManagerService.this.getOrCreateUserStateLocked(resolvedUserId);
                        sessionState = TvInputManagerService.this.getSessionStateLocked(sessionToken, callingUid, userState);
                        if (!sessionState.isCurrent || !Objects.equals(sessionState.currentChannel, channelUri)) {
                            sessionState.isCurrent = true;
                            sessionState.currentChannel = channelUri;
                            TvInputManagerService.this.notifyCurrentChannelInfosUpdatedLocked(userState);
                        }
                    } catch (RemoteException | SessionNotFoundException e) {
                        Slog.e(TvInputManagerService.TAG, "error in tune", e);
                    }
                    if (TvContract.isChannelUriForPassthroughInput(channelUri)) {
                        return;
                    }
                    if (sessionState.isRecordingSession) {
                        return;
                    }
                    TvInputManagerService.this.logTuneStateChanged(5, sessionState, TvInputManagerService.getTvInputState(sessionState, userState));
                    SomeArgs args = SomeArgs.obtain();
                    args.arg1 = sessionState.componentName.getPackageName();
                    args.arg2 = Long.valueOf(System.currentTimeMillis());
                    args.arg3 = Long.valueOf(ContentUris.parseId(channelUri));
                    args.arg4 = params;
                    args.arg5 = sessionToken;
                    TvInputManagerService.this.mWatchLogHandler.obtainMessage(1, args).sendToTarget();
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void unblockContent(IBinder sessionToken, String unblockedRating, int userId) {
            ensureParentalControlsPermission();
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "unblockContent");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    try {
                        TvInputManagerService.this.getSessionLocked(sessionToken, callingUid, resolvedUserId).unblockContent(unblockedRating);
                    } catch (RemoteException | SessionNotFoundException e) {
                        Slog.e(TvInputManagerService.TAG, "error in unblockContent", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void setCaptionEnabled(IBinder sessionToken, boolean enabled, int userId) {
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "setCaptionEnabled");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    try {
                        TvInputManagerService.this.getSessionLocked(sessionToken, callingUid, resolvedUserId).setCaptionEnabled(enabled);
                    } catch (RemoteException | SessionNotFoundException e) {
                        Slog.e(TvInputManagerService.TAG, "error in setCaptionEnabled", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void selectTrack(IBinder sessionToken, int type, String trackId, int userId) {
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "selectTrack");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    try {
                        TvInputManagerService.this.getSessionLocked(sessionToken, callingUid, resolvedUserId).selectTrack(type, trackId);
                    } catch (RemoteException | SessionNotFoundException e) {
                        Slog.e(TvInputManagerService.TAG, "error in selectTrack", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void setInteractiveAppNotificationEnabled(IBinder sessionToken, boolean enabled, int userId) {
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "setInteractiveAppNotificationEnabled");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    try {
                        TvInputManagerService.this.getSessionLocked(sessionToken, callingUid, resolvedUserId).setInteractiveAppNotificationEnabled(enabled);
                    } catch (RemoteException | SessionNotFoundException e) {
                        Slog.e(TvInputManagerService.TAG, "error in setInteractiveAppNotificationEnabled", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void sendAppPrivateCommand(IBinder sessionToken, String command, Bundle data, int userId) {
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "sendAppPrivateCommand");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    try {
                        TvInputManagerService.this.getSessionLocked(sessionToken, callingUid, resolvedUserId).appPrivateCommand(command, data);
                    } catch (RemoteException | SessionNotFoundException e) {
                        Slog.e(TvInputManagerService.TAG, "error in appPrivateCommand", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void createOverlayView(IBinder sessionToken, IBinder windowToken, Rect frame, int userId) {
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "createOverlayView");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    try {
                        TvInputManagerService.this.getSessionLocked(sessionToken, callingUid, resolvedUserId).createOverlayView(windowToken, frame);
                    } catch (RemoteException | SessionNotFoundException e) {
                        Slog.e(TvInputManagerService.TAG, "error in createOverlayView", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void relayoutOverlayView(IBinder sessionToken, Rect frame, int userId) {
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "relayoutOverlayView");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    try {
                        TvInputManagerService.this.getSessionLocked(sessionToken, callingUid, resolvedUserId).relayoutOverlayView(frame);
                    } catch (RemoteException | SessionNotFoundException e) {
                        Slog.e(TvInputManagerService.TAG, "error in relayoutOverlayView", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void removeOverlayView(IBinder sessionToken, int userId) {
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "removeOverlayView");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    try {
                        TvInputManagerService.this.getSessionLocked(sessionToken, callingUid, resolvedUserId).removeOverlayView();
                    } catch (RemoteException | SessionNotFoundException e) {
                        Slog.e(TvInputManagerService.TAG, "error in removeOverlayView", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void timeShiftPlay(IBinder sessionToken, Uri recordedProgramUri, int userId) {
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "timeShiftPlay");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    try {
                        TvInputManagerService.this.getSessionLocked(sessionToken, callingUid, resolvedUserId).timeShiftPlay(recordedProgramUri);
                    } catch (RemoteException | SessionNotFoundException e) {
                        Slog.e(TvInputManagerService.TAG, "error in timeShiftPlay", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void timeShiftPause(IBinder sessionToken, int userId) {
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "timeShiftPause");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    try {
                        TvInputManagerService.this.getSessionLocked(sessionToken, callingUid, resolvedUserId).timeShiftPause();
                    } catch (RemoteException | SessionNotFoundException e) {
                        Slog.e(TvInputManagerService.TAG, "error in timeShiftPause", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void timeShiftResume(IBinder sessionToken, int userId) {
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "timeShiftResume");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    try {
                        TvInputManagerService.this.getSessionLocked(sessionToken, callingUid, resolvedUserId).timeShiftResume();
                    } catch (RemoteException | SessionNotFoundException e) {
                        Slog.e(TvInputManagerService.TAG, "error in timeShiftResume", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void timeShiftSeekTo(IBinder sessionToken, long timeMs, int userId) {
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "timeShiftSeekTo");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    try {
                        TvInputManagerService.this.getSessionLocked(sessionToken, callingUid, resolvedUserId).timeShiftSeekTo(timeMs);
                    } catch (RemoteException | SessionNotFoundException e) {
                        Slog.e(TvInputManagerService.TAG, "error in timeShiftSeekTo", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void timeShiftSetPlaybackParams(IBinder sessionToken, PlaybackParams params, int userId) {
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "timeShiftSetPlaybackParams");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    try {
                        TvInputManagerService.this.getSessionLocked(sessionToken, callingUid, resolvedUserId).timeShiftSetPlaybackParams(params);
                    } catch (RemoteException | SessionNotFoundException e) {
                        Slog.e(TvInputManagerService.TAG, "error in timeShiftSetPlaybackParams", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void timeShiftEnablePositionTracking(IBinder sessionToken, boolean enable, int userId) {
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "timeShiftEnablePositionTracking");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    try {
                        TvInputManagerService.this.getSessionLocked(sessionToken, callingUid, resolvedUserId).timeShiftEnablePositionTracking(enable);
                    } catch (RemoteException | SessionNotFoundException e) {
                        Slog.e(TvInputManagerService.TAG, "error in timeShiftEnablePositionTracking", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void startRecording(IBinder sessionToken, Uri programUri, Bundle params, int userId) {
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "startRecording");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    try {
                        TvInputManagerService.this.getSessionLocked(sessionToken, callingUid, resolvedUserId).startRecording(programUri, params);
                    } catch (RemoteException | SessionNotFoundException e) {
                        Slog.e(TvInputManagerService.TAG, "error in startRecording", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void stopRecording(IBinder sessionToken, int userId) {
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "stopRecording");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    try {
                        TvInputManagerService.this.getSessionLocked(sessionToken, callingUid, resolvedUserId).stopRecording();
                    } catch (RemoteException | SessionNotFoundException e) {
                        Slog.e(TvInputManagerService.TAG, "error in stopRecording", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void pauseRecording(IBinder sessionToken, Bundle params, int userId) {
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "pauseRecording");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    try {
                        TvInputManagerService.this.getSessionLocked(sessionToken, callingUid, resolvedUserId).pauseRecording(params);
                    } catch (RemoteException | SessionNotFoundException e) {
                        Slog.e(TvInputManagerService.TAG, "error in pauseRecording", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void resumeRecording(IBinder sessionToken, Bundle params, int userId) {
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "resumeRecording");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    try {
                        TvInputManagerService.this.getSessionLocked(sessionToken, callingUid, resolvedUserId).resumeRecording(params);
                    } catch (RemoteException | SessionNotFoundException e) {
                        Slog.e(TvInputManagerService.TAG, "error in resumeRecording", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public List<TvInputHardwareInfo> getHardwareList() throws RemoteException {
            if (TvInputManagerService.this.mContext.checkCallingPermission("android.permission.TV_INPUT_HARDWARE") != 0) {
                return null;
            }
            long identity = Binder.clearCallingIdentity();
            try {
                return TvInputManagerService.this.mTvInputHardwareManager.getHardwareList();
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public ITvInputHardware acquireTvInputHardware(int deviceId, ITvInputHardwareCallback callback, TvInputInfo info, int userId, String tvInputSessionId, int priorityHint) throws RemoteException {
            if (TvInputManagerService.this.mContext.checkCallingPermission("android.permission.TV_INPUT_HARDWARE") != 0) {
                return null;
            }
            long identity = Binder.clearCallingIdentity();
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "acquireTvInputHardware");
            try {
                return TvInputManagerService.this.mTvInputHardwareManager.acquireHardware(deviceId, callback, info, callingUid, resolvedUserId, tvInputSessionId, priorityHint);
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void releaseTvInputHardware(int deviceId, ITvInputHardware hardware, int userId) throws RemoteException {
            if (TvInputManagerService.this.mContext.checkCallingPermission("android.permission.TV_INPUT_HARDWARE") != 0) {
                return;
            }
            long identity = Binder.clearCallingIdentity();
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "releaseTvInputHardware");
            try {
                TvInputManagerService.this.mTvInputHardwareManager.releaseHardware(deviceId, hardware, callingUid, resolvedUserId);
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public List<DvbDeviceInfo> getDvbDeviceList() throws RemoteException {
            int i;
            List<DvbDeviceInfo> unmodifiableList;
            File devDirectory;
            boolean dvbDirectoryFound;
            if (TvInputManagerService.this.mContext.checkCallingPermission("android.permission.DVB_DEVICE") != 0) {
                throw new SecurityException("Requires DVB_DEVICE permission");
            }
            long identity = Binder.clearCallingIdentity();
            try {
                ArrayList<DvbDeviceInfo> deviceInfosFromPattern1 = new ArrayList<>();
                File devDirectory2 = new File("/dev");
                boolean dvbDirectoryFound2 = false;
                String[] list = devDirectory2.list();
                int length = list.length;
                int i2 = 0;
                while (true) {
                    i = 1;
                    if (i2 >= length) {
                        break;
                    }
                    String fileName = list[i2];
                    Matcher matcher = TvInputManagerService.sFrontEndDevicePattern.matcher(fileName);
                    if (matcher.find()) {
                        int adapterId = Integer.parseInt(matcher.group(1));
                        int deviceId = Integer.parseInt(matcher.group(2));
                        deviceInfosFromPattern1.add(new DvbDeviceInfo(adapterId, deviceId));
                    }
                    if (TextUtils.equals("dvb", fileName)) {
                        dvbDirectoryFound2 = true;
                    }
                    i2++;
                }
                if (!dvbDirectoryFound2) {
                    return Collections.unmodifiableList(deviceInfosFromPattern1);
                }
                File dvbDirectory = new File(TvInputManagerService.DVB_DIRECTORY);
                ArrayList<DvbDeviceInfo> deviceInfosFromPattern2 = new ArrayList<>();
                String[] list2 = dvbDirectory.list();
                int length2 = list2.length;
                int i3 = 0;
                while (i3 < length2) {
                    String fileNameInDvb = list2[i3];
                    Matcher adapterMatcher = TvInputManagerService.sAdapterDirPattern.matcher(fileNameInDvb);
                    if (!adapterMatcher.find()) {
                        devDirectory = devDirectory2;
                        dvbDirectoryFound = dvbDirectoryFound2;
                    } else {
                        int adapterId2 = Integer.parseInt(adapterMatcher.group(i));
                        File adapterDirectory = new File("/dev/dvb/" + fileNameInDvb);
                        String[] list3 = adapterDirectory.list();
                        int length3 = list3.length;
                        int i4 = 0;
                        while (i4 < length3) {
                            String fileNameInAdapter = list3[i4];
                            File devDirectory3 = devDirectory2;
                            boolean dvbDirectoryFound3 = dvbDirectoryFound2;
                            Matcher frontendMatcher = TvInputManagerService.sFrontEndInAdapterDirPattern.matcher(fileNameInAdapter);
                            if (frontendMatcher.find()) {
                                int deviceId2 = Integer.parseInt(frontendMatcher.group(1));
                                deviceInfosFromPattern2.add(new DvbDeviceInfo(adapterId2, deviceId2));
                            }
                            i4++;
                            devDirectory2 = devDirectory3;
                            dvbDirectoryFound2 = dvbDirectoryFound3;
                        }
                        devDirectory = devDirectory2;
                        dvbDirectoryFound = dvbDirectoryFound2;
                    }
                    i3++;
                    devDirectory2 = devDirectory;
                    dvbDirectoryFound2 = dvbDirectoryFound;
                    i = 1;
                }
                if (deviceInfosFromPattern2.isEmpty()) {
                    unmodifiableList = Collections.unmodifiableList(deviceInfosFromPattern1);
                } else {
                    unmodifiableList = Collections.unmodifiableList(deviceInfosFromPattern2);
                }
                return unmodifiableList;
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public ParcelFileDescriptor openDvbDevice(DvbDeviceInfo info, int deviceType) throws RemoteException {
            boolean dvbDeviceFound;
            String deviceFileName;
            int i;
            File devDirectory;
            boolean dvbDeviceFound2;
            if (TvInputManagerService.this.mContext.checkCallingPermission("android.permission.DVB_DEVICE") != 0) {
                throw new SecurityException("Requires DVB_DEVICE permission");
            }
            File devDirectory2 = new File("/dev");
            boolean dvbDeviceFound3 = false;
            String[] list = devDirectory2.list();
            int length = list.length;
            int i2 = 0;
            while (true) {
                if (i2 >= length) {
                    dvbDeviceFound = dvbDeviceFound3;
                    break;
                }
                String fileName = list[i2];
                if (!TextUtils.equals("dvb", fileName)) {
                    devDirectory = devDirectory2;
                } else {
                    File dvbDirectory = new File(TvInputManagerService.DVB_DIRECTORY);
                    String[] list2 = dvbDirectory.list();
                    int length2 = list2.length;
                    int i3 = 0;
                    while (true) {
                        if (i3 >= length2) {
                            devDirectory = devDirectory2;
                            break;
                        }
                        String fileNameInDvb = list2[i3];
                        Matcher adapterMatcher = TvInputManagerService.sAdapterDirPattern.matcher(fileNameInDvb);
                        if (!adapterMatcher.find()) {
                            dvbDeviceFound2 = dvbDeviceFound3;
                            devDirectory = devDirectory2;
                        } else {
                            dvbDeviceFound2 = dvbDeviceFound3;
                            File adapterDirectory = new File("/dev/dvb/" + fileNameInDvb);
                            String[] list3 = adapterDirectory.list();
                            int length3 = list3.length;
                            int i4 = 0;
                            while (i4 < length3) {
                                String fileNameInAdapter = list3[i4];
                                devDirectory = devDirectory2;
                                Matcher frontendMatcher = TvInputManagerService.sFrontEndInAdapterDirPattern.matcher(fileNameInAdapter);
                                if (!frontendMatcher.find()) {
                                    i4++;
                                    devDirectory2 = devDirectory;
                                } else {
                                    dvbDeviceFound3 = true;
                                    break;
                                }
                            }
                            devDirectory = devDirectory2;
                        }
                        dvbDeviceFound3 = dvbDeviceFound2;
                        if (dvbDeviceFound3) {
                            break;
                        }
                        i3++;
                        devDirectory2 = devDirectory;
                    }
                }
                if (!dvbDeviceFound3) {
                    i2++;
                    devDirectory2 = devDirectory;
                } else {
                    dvbDeviceFound = dvbDeviceFound3;
                    break;
                }
            }
            long identity = Binder.clearCallingIdentity();
            try {
                switch (deviceType) {
                    case 0:
                        deviceFileName = String.format(dvbDeviceFound ? "/dev/dvb/adapter%d/demux%d" : "/dev/dvb%d.demux%d", Integer.valueOf(info.getAdapterId()), Integer.valueOf(info.getDeviceId()));
                        break;
                    case 1:
                        deviceFileName = String.format(dvbDeviceFound ? "/dev/dvb/adapter%d/dvr%d" : "/dev/dvb%d.dvr%d", Integer.valueOf(info.getAdapterId()), Integer.valueOf(info.getDeviceId()));
                        break;
                    case 2:
                        deviceFileName = String.format(dvbDeviceFound ? "/dev/dvb/adapter%d/frontend%d" : "/dev/dvb%d.frontend%d", Integer.valueOf(info.getAdapterId()), Integer.valueOf(info.getDeviceId()));
                        break;
                    default:
                        throw new IllegalArgumentException("Invalid DVB device: " + deviceType);
                }
                try {
                    File file = new File(deviceFileName);
                    if (2 == deviceType) {
                        i = 805306368;
                    } else {
                        i = 268435456;
                    }
                    return ParcelFileDescriptor.open(file, i);
                } catch (FileNotFoundException e) {
                    return null;
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public List<TvStreamConfig> getAvailableTvStreamConfigList(String inputId, int userId) throws RemoteException {
            ensureCaptureTvInputPermission();
            long identity = Binder.clearCallingIdentity();
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "getAvailableTvStreamConfigList");
            try {
                return TvInputManagerService.this.mTvInputHardwareManager.getAvailableTvStreamConfigList(inputId, callingUid, resolvedUserId);
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public boolean captureFrame(String inputId, Surface surface, TvStreamConfig config, int userId) throws RemoteException {
            String hardwareInputId;
            ensureCaptureTvInputPermission();
            long identity = Binder.clearCallingIdentity();
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "captureFrame");
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    try {
                        UserState userState = TvInputManagerService.this.getOrCreateUserStateLocked(resolvedUserId);
                        if (userState.inputMap.get(inputId) == null) {
                            Slog.e(TvInputManagerService.TAG, "input not found for " + inputId);
                            return false;
                        }
                        Iterator it = userState.sessionStateMap.values().iterator();
                        while (true) {
                            if (!it.hasNext()) {
                                hardwareInputId = null;
                                break;
                            }
                            SessionState sessionState = (SessionState) it.next();
                            if (sessionState.inputId.equals(inputId) && sessionState.hardwareSessionToken != null) {
                                String hardwareInputId2 = ((SessionState) userState.sessionStateMap.get(sessionState.hardwareSessionToken)).inputId;
                                hardwareInputId = hardwareInputId2;
                                break;
                            }
                        }
                        try {
                            return TvInputManagerService.this.mTvInputHardwareManager.captureFrame(hardwareInputId != null ? hardwareInputId : inputId, surface, config, callingUid, resolvedUserId);
                        } catch (Throwable th) {
                            th = th;
                            throw th;
                        }
                    } catch (Throwable th2) {
                        th = th2;
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [2421=4] */
        public boolean isSingleSessionActive(int userId) throws RemoteException {
            ensureCaptureTvInputPermission();
            long identity = Binder.clearCallingIdentity();
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "isSingleSessionActive");
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    UserState userState = TvInputManagerService.this.getOrCreateUserStateLocked(resolvedUserId);
                    boolean z = true;
                    if (userState.sessionStateMap.size() == 1) {
                        return true;
                    }
                    if (userState.sessionStateMap.size() == 2) {
                        SessionState[] sessionStates = (SessionState[]) userState.sessionStateMap.values().toArray(new SessionState[2]);
                        if (sessionStates[0].hardwareSessionToken == null && sessionStates[1].hardwareSessionToken == null) {
                            z = false;
                        }
                        return z;
                    }
                    return false;
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        private void ensureCaptureTvInputPermission() {
            if (TvInputManagerService.this.mContext.checkCallingPermission("android.permission.CAPTURE_TV_INPUT") != 0) {
                throw new SecurityException("Requires CAPTURE_TV_INPUT permission");
            }
        }

        public void requestChannelBrowsable(Uri channelUri, int userId) throws RemoteException {
            String callingPackageName = getCallingPackageName();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), Binder.getCallingUid(), userId, "requestChannelBrowsable");
            long identity = Binder.clearCallingIdentity();
            try {
                Intent intent = new Intent("android.media.tv.action.CHANNEL_BROWSABLE_REQUESTED");
                List<ResolveInfo> list = TvInputManagerService.this.getContext().getPackageManager().queryBroadcastReceivers(intent, 0);
                if (list != null) {
                    for (ResolveInfo info : list) {
                        String receiverPackageName = info.activityInfo.packageName;
                        intent.putExtra("android.media.tv.extra.CHANNEL_ID", ContentUris.parseId(channelUri));
                        intent.putExtra("android.media.tv.extra.PACKAGE_NAME", callingPackageName);
                        intent.setPackage(receiverPackageName);
                        TvInputManagerService.this.getContext().sendBroadcastAsUser(intent, new UserHandle(resolvedUserId));
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void requestBroadcastInfo(IBinder sessionToken, BroadcastInfoRequest request, int userId) {
            int callingUid = Binder.getCallingUid();
            int callingPid = Binder.getCallingPid();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(callingPid, callingUid, userId, "requestBroadcastInfo");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    try {
                        SessionState sessionState = TvInputManagerService.this.getSessionStateLocked(sessionToken, callingUid, resolvedUserId);
                        TvInputManagerService.this.getSessionLocked(sessionState).requestBroadcastInfo(request);
                    } catch (RemoteException | SessionNotFoundException e) {
                        Slog.e(TvInputManagerService.TAG, "error in requestBroadcastInfo", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void removeBroadcastInfo(IBinder sessionToken, int requestId, int userId) {
            int callingUid = Binder.getCallingUid();
            int callingPid = Binder.getCallingPid();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(callingPid, callingUid, userId, "removeBroadcastInfo");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    try {
                        SessionState sessionState = TvInputManagerService.this.getSessionStateLocked(sessionToken, callingUid, resolvedUserId);
                        TvInputManagerService.this.getSessionLocked(sessionState).removeBroadcastInfo(requestId);
                    } catch (RemoteException | SessionNotFoundException e) {
                        Slog.e(TvInputManagerService.TAG, "error in removeBroadcastInfo", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void requestAd(IBinder sessionToken, AdRequest request, int userId) {
            int callingUid = Binder.getCallingUid();
            int callingPid = Binder.getCallingPid();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(callingPid, callingUid, userId, "requestAd");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    try {
                        SessionState sessionState = TvInputManagerService.this.getSessionStateLocked(sessionToken, callingUid, resolvedUserId);
                        TvInputManagerService.this.getSessionLocked(sessionState).requestAd(request);
                    } catch (RemoteException | SessionNotFoundException e) {
                        Slog.e(TvInputManagerService.TAG, "error in requestAd", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public int getClientPid(String sessionId) {
            ensureTunerResourceAccessPermission();
            long identity = Binder.clearCallingIdentity();
            int clientPid = -1;
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    try {
                        clientPid = getClientPidLocked(sessionId);
                    } catch (ClientPidNotFoundException e) {
                        Slog.e(TvInputManagerService.TAG, "error in getClientPid", e);
                    }
                }
                return clientPid;
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public int getClientPriority(int useCase, String sessionId) {
            ensureTunerResourceAccessPermission();
            int callingPid = Binder.getCallingPid();
            long identity = Binder.clearCallingIdentity();
            int clientPid = -1;
            if (sessionId != null) {
                try {
                    synchronized (TvInputManagerService.this.mLock) {
                        try {
                            clientPid = getClientPidLocked(sessionId);
                        } catch (ClientPidNotFoundException e) {
                            Slog.e(TvInputManagerService.TAG, "error in getClientPriority", e);
                        }
                    }
                } catch (Throwable th) {
                    Binder.restoreCallingIdentity(identity);
                    throw th;
                }
            } else {
                clientPid = callingPid;
            }
            TunerResourceManager trm = (TunerResourceManager) TvInputManagerService.this.mContext.getSystemService("tv_tuner_resource_mgr");
            int clientPriority = trm.getClientPriority(useCase, clientPid);
            Binder.restoreCallingIdentity(identity);
            return clientPriority;
        }

        public List<TunedInfo> getCurrentTunedInfos(int userId) {
            List<TunedInfo> currentTunedInfosInternalLocked;
            if (TvInputManagerService.this.mContext.checkCallingPermission("android.permission.ACCESS_TUNED_INFO") != 0) {
                throw new SecurityException("The caller does not have access tuned info permission");
            }
            int callingPid = Binder.getCallingPid();
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(callingPid, callingUid, userId, "getTvCurrentChannelInfos");
            synchronized (TvInputManagerService.this.mLock) {
                UserState userState = TvInputManagerService.this.getOrCreateUserStateLocked(resolvedUserId);
                currentTunedInfosInternalLocked = TvInputManagerService.this.getCurrentTunedInfosInternalLocked(userState, callingPid, callingUid);
            }
            return currentTunedInfosInternalLocked;
        }

        public void addHardwareDevice(int deviceId) {
            TvInputHardwareInfo info = new TvInputHardwareInfo.Builder().deviceId(deviceId).type(9).audioType(0).audioAddress("0").hdmiPortId(0).build();
            TvInputManagerService.this.mTvInputHardwareManager.onDeviceAvailable(info, null);
        }

        public void removeHardwareDevice(int deviceId) {
            TvInputManagerService.this.mTvInputHardwareManager.onDeviceUnavailable(deviceId);
        }

        private int getClientPidLocked(String sessionId) throws ClientPidNotFoundException {
            if (TvInputManagerService.this.mSessionIdToSessionStateMap.get(sessionId) == null) {
                throw new ClientPidNotFoundException("Client Pid not found with sessionId " + sessionId);
            }
            return ((SessionState) TvInputManagerService.this.mSessionIdToSessionStateMap.get(sessionId)).callingPid;
        }

        private void ensureTunerResourceAccessPermission() {
            if (TvInputManagerService.this.mContext.checkCallingPermission("android.permission.TUNER_RESOURCE_ACCESS") != 0) {
                throw new SecurityException("Requires TUNER_RESOURCE_ACCESS permission");
            }
        }

        private void ensureTisExtensionInterfacePermission() {
            if (TvInputManagerService.this.mContext.checkCallingPermission("android.permission.TIS_EXTENSION_INTERFACE") != 0) {
                throw new SecurityException("Requires TIS_EXTENSION_INTERFACE permission");
            }
        }

        @NeverCompile
        protected void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
            IndentingPrintWriter pw = new IndentingPrintWriter(writer, "  ");
            if (DumpUtils.checkDumpPermission(TvInputManagerService.this.mContext, TvInputManagerService.TAG, pw)) {
                synchronized (TvInputManagerService.this.mLock) {
                    pw.println("User Ids (Current user: " + TvInputManagerService.this.mCurrentUserId + "):");
                    pw.increaseIndent();
                    for (int i = 0; i < TvInputManagerService.this.mUserStates.size(); i++) {
                        pw.println(Integer.valueOf(TvInputManagerService.this.mUserStates.keyAt(i)));
                    }
                    pw.decreaseIndent();
                    for (int i2 = 0; i2 < TvInputManagerService.this.mUserStates.size(); i2++) {
                        int userId = TvInputManagerService.this.mUserStates.keyAt(i2);
                        UserState userState = TvInputManagerService.this.getOrCreateUserStateLocked(userId);
                        pw.println("UserState (" + userId + "):");
                        pw.increaseIndent();
                        pw.println("inputMap: inputId -> TvInputState");
                        pw.increaseIndent();
                        for (Map.Entry<String, TvInputState> entry : userState.inputMap.entrySet()) {
                            pw.println(entry.getKey() + ": " + entry.getValue());
                        }
                        pw.decreaseIndent();
                        pw.println("packageSet:");
                        pw.increaseIndent();
                        for (String packageName : userState.packageSet) {
                            pw.println(packageName);
                        }
                        pw.decreaseIndent();
                        pw.println("clientStateMap: ITvInputClient -> ClientState");
                        pw.increaseIndent();
                        for (Map.Entry<IBinder, ClientState> entry2 : userState.clientStateMap.entrySet()) {
                            ClientState client = entry2.getValue();
                            pw.println(entry2.getKey() + ": " + client);
                            pw.increaseIndent();
                            pw.println("sessionTokens:");
                            pw.increaseIndent();
                            for (IBinder token : client.sessionTokens) {
                                pw.println("" + token);
                            }
                            pw.decreaseIndent();
                            pw.println("clientTokens: " + client.clientToken);
                            pw.println("userId: " + client.userId);
                            pw.decreaseIndent();
                        }
                        pw.decreaseIndent();
                        pw.println("serviceStateMap: ComponentName -> ServiceState");
                        pw.increaseIndent();
                        for (Map.Entry<ComponentName, ServiceState> entry3 : userState.serviceStateMap.entrySet()) {
                            ServiceState service = entry3.getValue();
                            pw.println(entry3.getKey() + ": " + service);
                            pw.increaseIndent();
                            pw.println("sessionTokens:");
                            pw.increaseIndent();
                            for (IBinder token2 : service.sessionTokens) {
                                pw.println("" + token2);
                            }
                            pw.decreaseIndent();
                            pw.println("service: " + service.service);
                            pw.println("callback: " + service.callback);
                            pw.println("bound: " + service.bound);
                            pw.println("reconnecting: " + service.reconnecting);
                            pw.decreaseIndent();
                        }
                        pw.decreaseIndent();
                        pw.println("sessionStateMap: ITvInputSession -> SessionState");
                        pw.increaseIndent();
                        for (Map.Entry<IBinder, SessionState> entry4 : userState.sessionStateMap.entrySet()) {
                            SessionState session = entry4.getValue();
                            pw.println(entry4.getKey() + ": " + session);
                            pw.increaseIndent();
                            pw.println("inputId: " + session.inputId);
                            pw.println("sessionId: " + session.sessionId);
                            pw.println("client: " + session.client);
                            pw.println("seq: " + session.seq);
                            pw.println("callingUid: " + session.callingUid);
                            pw.println("callingPid: " + session.callingPid);
                            pw.println("userId: " + session.userId);
                            pw.println("sessionToken: " + session.sessionToken);
                            pw.println("session: " + session.session);
                            pw.println("hardwareSessionToken: " + session.hardwareSessionToken);
                            pw.decreaseIndent();
                        }
                        pw.decreaseIndent();
                        pw.println("mCallbacks:");
                        pw.increaseIndent();
                        int n = userState.mCallbacks.beginBroadcast();
                        for (int j = 0; j < n; j++) {
                            pw.println(userState.mCallbacks.getRegisteredCallbackItem(j));
                        }
                        userState.mCallbacks.finishBroadcast();
                        pw.decreaseIndent();
                        pw.println("mainSessionToken: " + userState.mainSessionToken);
                        pw.decreaseIndent();
                    }
                }
                TvInputManagerService.this.mTvInputHardwareManager.dump(fd, writer, args);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static TvInputState getTvInputState(SessionState sessionState, UserState userState) {
        if (userState != null) {
            return (TvInputState) userState.inputMap.get(sessionState.inputId);
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public List<TunedInfo> getCurrentTunedInfosInternalLocked(UserState userState, int callingPid, int callingUid) {
        Integer appTag;
        int appType;
        List<TunedInfo> channelInfos = new ArrayList<>();
        boolean watchedProgramsAccess = hasAccessWatchedProgramsPermission(callingPid, callingUid);
        for (SessionState state : userState.sessionStateMap.values()) {
            if (state.isCurrent) {
                if (state.callingUid == callingUid) {
                    appTag = 0;
                    appType = 1;
                } else {
                    appTag = (Integer) userState.mAppTagMap.get(Integer.valueOf(state.callingUid));
                    if (appTag == null) {
                        int i = userState.mNextAppTag;
                        userState.mNextAppTag = i + 1;
                        appTag = Integer.valueOf(i);
                        userState.mAppTagMap.put(Integer.valueOf(state.callingUid), appTag);
                    }
                    if (isSystemApp(state.componentName.getPackageName())) {
                        appType = 2;
                    } else {
                        appType = 3;
                    }
                }
                channelInfos.add(new TunedInfo(state.inputId, watchedProgramsAccess ? state.currentChannel : null, state.isRecordingSession, state.isVisible, state.isMainSession, appType, appTag.intValue()));
            }
        }
        return channelInfos;
    }

    private boolean hasAccessWatchedProgramsPermission(int callingPid, int callingUid) {
        return this.mContext.checkPermission(PERMISSION_ACCESS_WATCHED_PROGRAMS, callingPid, callingUid) == 0;
    }

    private boolean isSystemApp(String pkg) {
        try {
            return (this.mContext.getPackageManager().getApplicationInfo(pkg, 0).flags & 1) != 0;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void logTuneStateChanged(int state, SessionState sessionState, TvInputState inputState) {
        int inputType;
        int inputId;
        int hdmiPort;
        int tisUid = -1;
        if (inputState == null) {
            inputType = 0;
            inputId = 0;
            hdmiPort = 0;
        } else {
            tisUid = inputState.uid;
            int inputType2 = inputState.info.getType();
            if (inputType2 == 0) {
                inputType2 = 1;
            }
            int inputId2 = inputState.inputNumber;
            HdmiDeviceInfo hdmiDeviceInfo = inputState.info.getHdmiDeviceInfo();
            if (hdmiDeviceInfo == null) {
                inputType = inputType2;
                inputId = inputId2;
                hdmiPort = 0;
            } else {
                int hdmiPort2 = hdmiDeviceInfo.getPortId();
                inputType = inputType2;
                inputId = inputId2;
                hdmiPort = hdmiPort2;
            }
        }
        FrameworkStatsLog.write((int) FrameworkStatsLog.TIF_TUNE_CHANGED, new int[]{sessionState.callingUid, tisUid}, new String[]{"tif_player", "tv_input_service"}, state, sessionState.sessionId, inputType, inputId, hdmiPort);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static final class UserState {
        private final Map<ITvInputManagerCallback, Pair<Integer, Integer>> callbackPidUidMap;
        private final Map<IBinder, ClientState> clientStateMap;
        private final List<TvContentRatingSystemInfo> contentRatingSystemList;
        private Map<String, TvInputState> inputMap;
        private final Map<Integer, Integer> mAppTagMap;
        private final RemoteCallbackList<ITvInputManagerCallback> mCallbacks;
        private int mNextAppTag;
        private IBinder mainSessionToken;
        private final Set<String> packageSet;
        private final PersistentDataStore persistentDataStore;
        private final Map<ComponentName, ServiceState> serviceStateMap;
        private final Map<IBinder, SessionState> sessionStateMap;

        private UserState(Context context, int userId) {
            this.inputMap = new HashMap();
            this.packageSet = new HashSet();
            this.contentRatingSystemList = new ArrayList();
            this.clientStateMap = new HashMap();
            this.serviceStateMap = new HashMap();
            this.sessionStateMap = new HashMap();
            this.mCallbacks = new RemoteCallbackList<>();
            this.callbackPidUidMap = new HashMap();
            this.mainSessionToken = null;
            this.mAppTagMap = new HashMap();
            this.mNextAppTag = 1;
            this.persistentDataStore = new PersistentDataStore(context, userId);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public final class ClientState implements IBinder.DeathRecipient {
        private IBinder clientToken;
        private final List<IBinder> sessionTokens = new ArrayList();
        private final int userId;

        ClientState(IBinder clientToken, int userId) {
            this.clientToken = clientToken;
            this.userId = userId;
        }

        public boolean isEmpty() {
            return this.sessionTokens.isEmpty();
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            synchronized (TvInputManagerService.this.mLock) {
                UserState userState = TvInputManagerService.this.getOrCreateUserStateLocked(this.userId);
                ClientState clientState = (ClientState) userState.clientStateMap.get(this.clientToken);
                if (clientState != null) {
                    while (clientState.sessionTokens.size() > 0) {
                        IBinder sessionToken = clientState.sessionTokens.get(0);
                        TvInputManagerService.this.releaseSessionLocked(sessionToken, 1000, this.userId);
                        if (clientState.sessionTokens.contains(sessionToken)) {
                            Slog.d(TvInputManagerService.TAG, "remove sessionToken " + sessionToken + " for " + this.clientToken);
                            clientState.sessionTokens.remove(sessionToken);
                        }
                    }
                }
                this.clientToken = null;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public final class ServiceState {
        private boolean bound;
        private ServiceCallback callback;
        private final ComponentName component;
        private final ServiceConnection connection;
        private final Map<String, TvInputInfo> hardwareInputMap;
        private final boolean isHardware;
        private boolean reconnecting;
        private ITvInputService service;
        private final List<IBinder> sessionTokens;

        private ServiceState(ComponentName component, int userId) {
            this.sessionTokens = new ArrayList();
            this.hardwareInputMap = new HashMap();
            this.component = component;
            this.connection = new InputServiceConnection(component, userId);
            this.isHardware = TvInputManagerService.hasHardwarePermission(TvInputManagerService.this.mContext.getPackageManager(), component);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static final class TvInputState {
        private TvInputInfo info;
        private int inputNumber;
        private int state;
        private int uid;

        private TvInputState() {
            this.state = 0;
        }

        public String toString() {
            return "info: " + this.info + "; state: " + this.state;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public final class SessionState implements IBinder.DeathRecipient {
        private final int callingPid;
        private final int callingUid;
        private final ITvInputClient client;
        private final ComponentName componentName;
        private Uri currentChannel;
        private IBinder hardwareSessionToken;
        private final String inputId;
        private boolean isCurrent;
        private boolean isMainSession;
        private final boolean isRecordingSession;
        private boolean isVisible;
        private final int seq;
        private ITvInputSession session;
        private final String sessionId;
        private final IBinder sessionToken;
        private final int userId;

        private SessionState(IBinder sessionToken, String inputId, ComponentName componentName, boolean isRecordingSession, ITvInputClient client, int seq, int callingUid, int callingPid, int userId, String sessionId) {
            this.isCurrent = false;
            this.currentChannel = null;
            this.isVisible = false;
            this.isMainSession = false;
            this.sessionToken = sessionToken;
            this.inputId = inputId;
            this.componentName = componentName;
            this.isRecordingSession = isRecordingSession;
            this.client = client;
            this.seq = seq;
            this.callingUid = callingUid;
            this.callingPid = callingPid;
            this.userId = userId;
            this.sessionId = sessionId;
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            synchronized (TvInputManagerService.this.mLock) {
                this.session = null;
                TvInputManagerService.this.clearSessionAndNotifyClientLocked(this);
            }
        }
    }

    /* loaded from: classes2.dex */
    private final class InputServiceConnection implements ServiceConnection {
        private final ComponentName mComponent;
        private final int mUserId;

        private InputServiceConnection(ComponentName component, int userId) {
            this.mComponent = component;
            this.mUserId = userId;
        }

        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName component, IBinder service) {
            synchronized (TvInputManagerService.this.mLock) {
                UserState userState = TvInputManagerService.this.getUserStateLocked(this.mUserId);
                if (userState == null) {
                    TvInputManagerService.this.mContext.unbindService(this);
                    return;
                }
                ServiceState serviceState = (ServiceState) userState.serviceStateMap.get(this.mComponent);
                serviceState.service = ITvInputService.Stub.asInterface(service);
                if (serviceState.isHardware && serviceState.callback == null) {
                    serviceState.callback = new ServiceCallback(this.mComponent, this.mUserId);
                    try {
                        serviceState.service.registerCallback(serviceState.callback);
                    } catch (RemoteException e) {
                        Slog.e(TvInputManagerService.TAG, "error in registerCallback", e);
                    }
                }
                List<IBinder> tokensToBeRemoved = new ArrayList<>();
                for (IBinder sessionToken : serviceState.sessionTokens) {
                    if (!TvInputManagerService.this.createSessionInternalLocked(serviceState.service, sessionToken, this.mUserId)) {
                        tokensToBeRemoved.add(sessionToken);
                    }
                }
                for (IBinder sessionToken2 : tokensToBeRemoved) {
                    TvInputManagerService.this.removeSessionStateLocked(sessionToken2, this.mUserId);
                }
                for (TvInputState inputState : userState.inputMap.values()) {
                    if (inputState.info.getComponent().equals(component) && inputState.state != 0) {
                        TvInputManagerService.this.notifyInputStateChangedLocked(userState, inputState.info.getId(), inputState.state, null);
                    }
                }
                if (serviceState.isHardware) {
                    serviceState.hardwareInputMap.clear();
                    for (TvInputHardwareInfo hardware : TvInputManagerService.this.mTvInputHardwareManager.getHardwareList()) {
                        try {
                            serviceState.service.notifyHardwareAdded(hardware);
                        } catch (RemoteException e2) {
                            Slog.e(TvInputManagerService.TAG, "error in notifyHardwareAdded", e2);
                        }
                    }
                    for (HdmiDeviceInfo device : TvInputManagerService.this.mTvInputHardwareManager.getHdmiDeviceList()) {
                        try {
                            serviceState.service.notifyHdmiDeviceAdded(device);
                        } catch (RemoteException e3) {
                            Slog.e(TvInputManagerService.TAG, "error in notifyHdmiDeviceAdded", e3);
                        }
                    }
                }
            }
        }

        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName component) {
            if (!this.mComponent.equals(component)) {
                throw new IllegalArgumentException("Mismatched ComponentName: " + this.mComponent + " (expected), " + component + " (actual).");
            }
            synchronized (TvInputManagerService.this.mLock) {
                UserState userState = TvInputManagerService.this.getOrCreateUserStateLocked(this.mUserId);
                ServiceState serviceState = (ServiceState) userState.serviceStateMap.get(this.mComponent);
                if (serviceState != null) {
                    serviceState.reconnecting = true;
                    serviceState.bound = false;
                    serviceState.service = null;
                    serviceState.callback = null;
                    TvInputManagerService.this.abortPendingCreateSessionRequestsLocked(serviceState, null, this.mUserId);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public final class ServiceCallback extends ITvInputServiceCallback.Stub {
        private final ComponentName mComponent;
        private final int mUserId;

        ServiceCallback(ComponentName component, int userId) {
            this.mComponent = component;
            this.mUserId = userId;
        }

        private void ensureHardwarePermission() {
            if (TvInputManagerService.this.mContext.checkCallingPermission("android.permission.TV_INPUT_HARDWARE") != 0) {
                throw new SecurityException("The caller does not have hardware permission");
            }
        }

        private void ensureValidInput(TvInputInfo inputInfo) {
            if (inputInfo.getId() == null || !this.mComponent.equals(inputInfo.getComponent())) {
                throw new IllegalArgumentException("Invalid TvInputInfo");
            }
        }

        private void addHardwareInputLocked(TvInputInfo inputInfo) {
            ServiceState serviceState = TvInputManagerService.this.getServiceStateLocked(this.mComponent, this.mUserId);
            serviceState.hardwareInputMap.put(inputInfo.getId(), inputInfo);
            TvInputManagerService.this.buildTvInputListLocked(this.mUserId, null);
        }

        public void addHardwareInput(int deviceId, TvInputInfo inputInfo) {
            ensureHardwarePermission();
            ensureValidInput(inputInfo);
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    TvInputManagerService.this.mTvInputHardwareManager.addHardwareInput(deviceId, inputInfo);
                    addHardwareInputLocked(inputInfo);
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void addHdmiInput(int id, TvInputInfo inputInfo) {
            ensureHardwarePermission();
            ensureValidInput(inputInfo);
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    TvInputManagerService.this.mTvInputHardwareManager.addHdmiInput(id, inputInfo);
                    addHardwareInputLocked(inputInfo);
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void removeHardwareInput(String inputId) {
            ensureHardwarePermission();
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    ServiceState serviceState = TvInputManagerService.this.getServiceStateLocked(this.mComponent, this.mUserId);
                    boolean removed = serviceState.hardwareInputMap.remove(inputId) != null;
                    if (removed) {
                        TvInputManagerService.this.buildTvInputListLocked(this.mUserId, null);
                        TvInputManagerService.this.mTvInputHardwareManager.removeHardwareInput(inputId);
                    } else {
                        Slog.e(TvInputManagerService.TAG, "failed to remove input " + inputId);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public final class SessionCallback extends ITvInputSessionCallback.Stub {
        private final InputChannel[] mChannels;
        private final SessionState mSessionState;

        SessionCallback(SessionState sessionState, InputChannel[] channels) {
            this.mSessionState = sessionState;
            this.mChannels = channels;
        }

        public void onSessionCreated(ITvInputSession session, IBinder hardwareSessionToken) {
            synchronized (TvInputManagerService.this.mLock) {
                this.mSessionState.session = session;
                this.mSessionState.hardwareSessionToken = hardwareSessionToken;
                if (session != null && addSessionTokenToClientStateLocked(session)) {
                    TvInputManagerService.this.sendSessionTokenToClientLocked(this.mSessionState.client, this.mSessionState.inputId, this.mSessionState.sessionToken, this.mChannels[0], this.mSessionState.seq);
                } else {
                    TvInputManagerService.this.removeSessionStateLocked(this.mSessionState.sessionToken, this.mSessionState.userId);
                    TvInputManagerService.this.sendSessionTokenToClientLocked(this.mSessionState.client, this.mSessionState.inputId, null, null, this.mSessionState.seq);
                }
                this.mChannels[0].dispose();
            }
        }

        private boolean addSessionTokenToClientStateLocked(ITvInputSession session) {
            try {
                session.asBinder().linkToDeath(this.mSessionState, 0);
                IBinder clientToken = this.mSessionState.client.asBinder();
                UserState userState = TvInputManagerService.this.getOrCreateUserStateLocked(this.mSessionState.userId);
                ClientState clientState = (ClientState) userState.clientStateMap.get(clientToken);
                if (clientState == null) {
                    clientState = new ClientState(clientToken, this.mSessionState.userId);
                    try {
                        clientToken.linkToDeath(clientState, 0);
                        userState.clientStateMap.put(clientToken, clientState);
                    } catch (RemoteException e) {
                        Slog.e(TvInputManagerService.TAG, "client process has already died", e);
                        return false;
                    }
                }
                clientState.sessionTokens.add(this.mSessionState.sessionToken);
                return true;
            } catch (RemoteException e2) {
                Slog.e(TvInputManagerService.TAG, "session process has already died", e2);
                return false;
            }
        }

        public void onChannelRetuned(Uri channelUri) {
            synchronized (TvInputManagerService.this.mLock) {
                if (this.mSessionState.session == null || this.mSessionState.client == null) {
                    return;
                }
                try {
                    this.mSessionState.client.onChannelRetuned(channelUri, this.mSessionState.seq);
                    if (!this.mSessionState.isCurrent || !Objects.equals(this.mSessionState.currentChannel, channelUri)) {
                        UserState userState = TvInputManagerService.this.getOrCreateUserStateLocked(this.mSessionState.userId);
                        this.mSessionState.isCurrent = true;
                        this.mSessionState.currentChannel = channelUri;
                        TvInputManagerService.this.notifyCurrentChannelInfosUpdatedLocked(userState);
                    }
                } catch (RemoteException e) {
                    Slog.e(TvInputManagerService.TAG, "error in onChannelRetuned", e);
                }
            }
        }

        public void onTracksChanged(List<TvTrackInfo> tracks) {
            synchronized (TvInputManagerService.this.mLock) {
                if (this.mSessionState.session == null || this.mSessionState.client == null) {
                    return;
                }
                try {
                    this.mSessionState.client.onTracksChanged(tracks, this.mSessionState.seq);
                } catch (RemoteException e) {
                    Slog.e(TvInputManagerService.TAG, "error in onTracksChanged", e);
                }
            }
        }

        public void onTrackSelected(int type, String trackId) {
            synchronized (TvInputManagerService.this.mLock) {
                if (this.mSessionState.session == null || this.mSessionState.client == null) {
                    return;
                }
                try {
                    this.mSessionState.client.onTrackSelected(type, trackId, this.mSessionState.seq);
                } catch (RemoteException e) {
                    Slog.e(TvInputManagerService.TAG, "error in onTrackSelected", e);
                }
            }
        }

        public void onVideoAvailable() {
            synchronized (TvInputManagerService.this.mLock) {
                if (this.mSessionState.session != null && this.mSessionState.client != null) {
                    SessionState sessionState = this.mSessionState;
                    TvInputManagerService tvInputManagerService = TvInputManagerService.this;
                    TvInputState tvInputState = TvInputManagerService.getTvInputState(sessionState, tvInputManagerService.getUserStateLocked(tvInputManagerService.mCurrentUserId));
                    try {
                        this.mSessionState.client.onVideoAvailable(this.mSessionState.seq);
                        TvInputManagerService.this.logTuneStateChanged(6, this.mSessionState, tvInputState);
                    } catch (RemoteException e) {
                        Slog.e(TvInputManagerService.TAG, "error in onVideoAvailable", e);
                    }
                }
            }
        }

        public void onVideoUnavailable(int reason) {
            synchronized (TvInputManagerService.this.mLock) {
                if (this.mSessionState.session != null && this.mSessionState.client != null) {
                    SessionState sessionState = this.mSessionState;
                    TvInputManagerService tvInputManagerService = TvInputManagerService.this;
                    TvInputState tvInputState = TvInputManagerService.getTvInputState(sessionState, tvInputManagerService.getUserStateLocked(tvInputManagerService.mCurrentUserId));
                    try {
                        this.mSessionState.client.onVideoUnavailable(reason, this.mSessionState.seq);
                        int loggedReason = TvInputManagerService.getVideoUnavailableReasonForStatsd(reason);
                        TvInputManagerService.this.logTuneStateChanged(loggedReason, this.mSessionState, tvInputState);
                    } catch (RemoteException e) {
                        Slog.e(TvInputManagerService.TAG, "error in onVideoUnavailable", e);
                    }
                }
            }
        }

        public void onContentAllowed() {
            synchronized (TvInputManagerService.this.mLock) {
                if (this.mSessionState.session == null || this.mSessionState.client == null) {
                    return;
                }
                try {
                    this.mSessionState.client.onContentAllowed(this.mSessionState.seq);
                } catch (RemoteException e) {
                    Slog.e(TvInputManagerService.TAG, "error in onContentAllowed", e);
                }
            }
        }

        public void onContentBlocked(String rating) {
            synchronized (TvInputManagerService.this.mLock) {
                if (this.mSessionState.session == null || this.mSessionState.client == null) {
                    return;
                }
                try {
                    this.mSessionState.client.onContentBlocked(rating, this.mSessionState.seq);
                } catch (RemoteException e) {
                    Slog.e(TvInputManagerService.TAG, "error in onContentBlocked", e);
                }
            }
        }

        public void onLayoutSurface(int left, int top, int right, int bottom) {
            synchronized (TvInputManagerService.this.mLock) {
                if (this.mSessionState.session == null || this.mSessionState.client == null) {
                    return;
                }
                try {
                    this.mSessionState.client.onLayoutSurface(left, top, right, bottom, this.mSessionState.seq);
                } catch (RemoteException e) {
                    Slog.e(TvInputManagerService.TAG, "error in onLayoutSurface", e);
                }
            }
        }

        public void onSessionEvent(String eventType, Bundle eventArgs) {
            synchronized (TvInputManagerService.this.mLock) {
                if (this.mSessionState.session == null || this.mSessionState.client == null) {
                    return;
                }
                try {
                    this.mSessionState.client.onSessionEvent(eventType, eventArgs, this.mSessionState.seq);
                } catch (RemoteException e) {
                    Slog.e(TvInputManagerService.TAG, "error in onSessionEvent", e);
                }
            }
        }

        public void onTimeShiftStatusChanged(int status) {
            synchronized (TvInputManagerService.this.mLock) {
                if (this.mSessionState.session == null || this.mSessionState.client == null) {
                    return;
                }
                try {
                    this.mSessionState.client.onTimeShiftStatusChanged(status, this.mSessionState.seq);
                } catch (RemoteException e) {
                    Slog.e(TvInputManagerService.TAG, "error in onTimeShiftStatusChanged", e);
                }
            }
        }

        public void onTimeShiftStartPositionChanged(long timeMs) {
            synchronized (TvInputManagerService.this.mLock) {
                if (this.mSessionState.session == null || this.mSessionState.client == null) {
                    return;
                }
                try {
                    this.mSessionState.client.onTimeShiftStartPositionChanged(timeMs, this.mSessionState.seq);
                } catch (RemoteException e) {
                    Slog.e(TvInputManagerService.TAG, "error in onTimeShiftStartPositionChanged", e);
                }
            }
        }

        public void onTimeShiftCurrentPositionChanged(long timeMs) {
            synchronized (TvInputManagerService.this.mLock) {
                if (this.mSessionState.session == null || this.mSessionState.client == null) {
                    return;
                }
                try {
                    this.mSessionState.client.onTimeShiftCurrentPositionChanged(timeMs, this.mSessionState.seq);
                } catch (RemoteException e) {
                    Slog.e(TvInputManagerService.TAG, "error in onTimeShiftCurrentPositionChanged", e);
                }
            }
        }

        public void onAitInfoUpdated(AitInfo aitInfo) {
            synchronized (TvInputManagerService.this.mLock) {
                if (this.mSessionState.session == null || this.mSessionState.client == null) {
                    return;
                }
                try {
                    this.mSessionState.client.onAitInfoUpdated(aitInfo, this.mSessionState.seq);
                } catch (RemoteException e) {
                    Slog.e(TvInputManagerService.TAG, "error in onAitInfoUpdated", e);
                }
            }
        }

        public void onSignalStrength(int strength) {
            synchronized (TvInputManagerService.this.mLock) {
                if (this.mSessionState.session == null || this.mSessionState.client == null) {
                    return;
                }
                try {
                    this.mSessionState.client.onSignalStrength(strength, this.mSessionState.seq);
                } catch (RemoteException e) {
                    Slog.e(TvInputManagerService.TAG, "error in onSignalStrength", e);
                }
            }
        }

        public void onTuned(Uri channelUri) {
            synchronized (TvInputManagerService.this.mLock) {
                if (this.mSessionState.session == null || this.mSessionState.client == null) {
                    return;
                }
                try {
                    this.mSessionState.client.onTuned(channelUri, this.mSessionState.seq);
                } catch (RemoteException e) {
                    Slog.e(TvInputManagerService.TAG, "error in onTuned", e);
                }
            }
        }

        public void onRecordingStopped(Uri recordedProgramUri) {
            synchronized (TvInputManagerService.this.mLock) {
                if (this.mSessionState.session == null || this.mSessionState.client == null) {
                    return;
                }
                try {
                    this.mSessionState.client.onRecordingStopped(recordedProgramUri, this.mSessionState.seq);
                } catch (RemoteException e) {
                    Slog.e(TvInputManagerService.TAG, "error in onRecordingStopped", e);
                }
            }
        }

        public void onError(int error) {
            synchronized (TvInputManagerService.this.mLock) {
                if (this.mSessionState.session == null || this.mSessionState.client == null) {
                    return;
                }
                try {
                    this.mSessionState.client.onError(error, this.mSessionState.seq);
                } catch (RemoteException e) {
                    Slog.e(TvInputManagerService.TAG, "error in onError", e);
                }
            }
        }

        public void onBroadcastInfoResponse(BroadcastInfoResponse response) {
            synchronized (TvInputManagerService.this.mLock) {
                if (this.mSessionState.session == null || this.mSessionState.client == null) {
                    return;
                }
                try {
                    this.mSessionState.client.onBroadcastInfoResponse(response, this.mSessionState.seq);
                } catch (RemoteException e) {
                    Slog.e(TvInputManagerService.TAG, "error in onBroadcastInfoResponse", e);
                }
            }
        }

        public void onAdResponse(AdResponse response) {
            synchronized (TvInputManagerService.this.mLock) {
                if (this.mSessionState.session == null || this.mSessionState.client == null) {
                    return;
                }
                try {
                    this.mSessionState.client.onAdResponse(response, this.mSessionState.seq);
                } catch (RemoteException e) {
                    Slog.e(TvInputManagerService.TAG, "error in onAdResponse", e);
                }
            }
        }
    }

    static int getVideoUnavailableReasonForStatsd(int reason) {
        int loggedReason = reason + 100;
        if (loggedReason < 100 || loggedReason > 118) {
            return 100;
        }
        return loggedReason;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public UserState getUserStateLocked(int userId) {
        return this.mUserStates.get(userId);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static final class WatchLogHandler extends Handler {
        static final int MSG_LOG_WATCH_END = 2;
        static final int MSG_LOG_WATCH_START = 1;
        static final int MSG_SWITCH_CONTENT_RESOLVER = 3;
        private ContentResolver mContentResolver;

        WatchLogHandler(ContentResolver contentResolver, Looper looper) {
            super(looper);
            this.mContentResolver = contentResolver;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    SomeArgs args = (SomeArgs) msg.obj;
                    String packageName = (String) args.arg1;
                    long watchStartTime = ((Long) args.arg2).longValue();
                    long channelId = ((Long) args.arg3).longValue();
                    Bundle tuneParams = (Bundle) args.arg4;
                    IBinder sessionToken = (IBinder) args.arg5;
                    ContentValues values = new ContentValues();
                    values.put("package_name", packageName);
                    values.put("watch_start_time_utc_millis", Long.valueOf(watchStartTime));
                    values.put("channel_id", Long.valueOf(channelId));
                    if (tuneParams != null) {
                        values.put("tune_params", encodeTuneParams(tuneParams));
                    }
                    values.put("session_token", sessionToken.toString());
                    try {
                        this.mContentResolver.insert(TvContract.WatchedPrograms.CONTENT_URI, values);
                    } catch (IllegalArgumentException ex) {
                        Slog.w(TvInputManagerService.TAG, "error in insert db for MSG_LOG_WATCH_START", ex);
                    }
                    args.recycle();
                    return;
                case 2:
                    SomeArgs args2 = (SomeArgs) msg.obj;
                    IBinder sessionToken2 = (IBinder) args2.arg1;
                    long watchEndTime = ((Long) args2.arg2).longValue();
                    ContentValues values2 = new ContentValues();
                    values2.put("watch_end_time_utc_millis", Long.valueOf(watchEndTime));
                    values2.put("session_token", sessionToken2.toString());
                    try {
                        this.mContentResolver.insert(TvContract.WatchedPrograms.CONTENT_URI, values2);
                    } catch (IllegalArgumentException ex2) {
                        Slog.w(TvInputManagerService.TAG, "error in insert db for MSG_LOG_WATCH_END", ex2);
                    }
                    args2.recycle();
                    return;
                case 3:
                    this.mContentResolver = (ContentResolver) msg.obj;
                    return;
                default:
                    Slog.w(TvInputManagerService.TAG, "unhandled message code: " + msg.what);
                    return;
            }
        }

        private String encodeTuneParams(Bundle tuneParams) {
            StringBuilder builder = new StringBuilder();
            Set<String> keySet = tuneParams.keySet();
            Iterator<String> it = keySet.iterator();
            while (it.hasNext()) {
                String key = it.next();
                Object value = tuneParams.get(key);
                if (value != null) {
                    builder.append(replaceEscapeCharacters(key));
                    builder.append("=");
                    builder.append(replaceEscapeCharacters(value.toString()));
                    if (it.hasNext()) {
                        builder.append(", ");
                    }
                }
            }
            return builder.toString();
        }

        private String replaceEscapeCharacters(String src) {
            char[] charArray;
            StringBuilder builder = new StringBuilder();
            for (char ch : src.toCharArray()) {
                if ("%=,".indexOf(ch) >= 0) {
                    builder.append('%');
                }
                builder.append(ch);
            }
            return builder.toString();
        }
    }

    /* loaded from: classes2.dex */
    private final class HardwareListener implements TvInputHardwareManager.Listener {
        private HardwareListener() {
        }

        @Override // com.android.server.tv.TvInputHardwareManager.Listener
        public void onStateChanged(String inputId, int state) {
            synchronized (TvInputManagerService.this.mLock) {
                TvInputManagerService tvInputManagerService = TvInputManagerService.this;
                tvInputManagerService.setStateLocked(inputId, state, tvInputManagerService.mCurrentUserId);
            }
        }

        @Override // com.android.server.tv.TvInputHardwareManager.Listener
        public void onHardwareDeviceAdded(TvInputHardwareInfo info) {
            synchronized (TvInputManagerService.this.mLock) {
                TvInputManagerService tvInputManagerService = TvInputManagerService.this;
                UserState userState = tvInputManagerService.getOrCreateUserStateLocked(tvInputManagerService.mCurrentUserId);
                for (ServiceState serviceState : userState.serviceStateMap.values()) {
                    if (serviceState.isHardware && serviceState.service != null) {
                        try {
                            serviceState.service.notifyHardwareAdded(info);
                        } catch (RemoteException e) {
                            Slog.e(TvInputManagerService.TAG, "error in notifyHardwareAdded", e);
                        }
                    }
                }
            }
        }

        @Override // com.android.server.tv.TvInputHardwareManager.Listener
        public void onHardwareDeviceRemoved(TvInputHardwareInfo info) {
            synchronized (TvInputManagerService.this.mLock) {
                TvInputManagerService tvInputManagerService = TvInputManagerService.this;
                UserState userState = tvInputManagerService.getOrCreateUserStateLocked(tvInputManagerService.mCurrentUserId);
                for (ServiceState serviceState : userState.serviceStateMap.values()) {
                    if (serviceState.isHardware && serviceState.service != null) {
                        try {
                            serviceState.service.notifyHardwareRemoved(info);
                        } catch (RemoteException e) {
                            Slog.e(TvInputManagerService.TAG, "error in notifyHardwareRemoved", e);
                        }
                    }
                }
            }
        }

        @Override // com.android.server.tv.TvInputHardwareManager.Listener
        public void onHdmiDeviceAdded(HdmiDeviceInfo deviceInfo) {
            synchronized (TvInputManagerService.this.mLock) {
                TvInputManagerService tvInputManagerService = TvInputManagerService.this;
                UserState userState = tvInputManagerService.getOrCreateUserStateLocked(tvInputManagerService.mCurrentUserId);
                for (ServiceState serviceState : userState.serviceStateMap.values()) {
                    if (serviceState.isHardware && serviceState.service != null) {
                        try {
                            serviceState.service.notifyHdmiDeviceAdded(deviceInfo);
                        } catch (RemoteException e) {
                            Slog.e(TvInputManagerService.TAG, "error in notifyHdmiDeviceAdded", e);
                        }
                    }
                }
            }
        }

        @Override // com.android.server.tv.TvInputHardwareManager.Listener
        public void onHdmiDeviceRemoved(HdmiDeviceInfo deviceInfo) {
            synchronized (TvInputManagerService.this.mLock) {
                TvInputManagerService tvInputManagerService = TvInputManagerService.this;
                UserState userState = tvInputManagerService.getOrCreateUserStateLocked(tvInputManagerService.mCurrentUserId);
                for (ServiceState serviceState : userState.serviceStateMap.values()) {
                    if (serviceState.isHardware && serviceState.service != null) {
                        try {
                            serviceState.service.notifyHdmiDeviceRemoved(deviceInfo);
                        } catch (RemoteException e) {
                            Slog.e(TvInputManagerService.TAG, "error in notifyHdmiDeviceRemoved", e);
                        }
                    }
                }
            }
        }

        @Override // com.android.server.tv.TvInputHardwareManager.Listener
        public void onHdmiDeviceUpdated(String inputId, HdmiDeviceInfo deviceInfo) {
            Integer state;
            synchronized (TvInputManagerService.this.mLock) {
                switch (deviceInfo.getDevicePowerStatus()) {
                    case 0:
                        state = 0;
                        break;
                    case 1:
                    case 2:
                    case 3:
                        state = 1;
                        break;
                    default:
                        state = null;
                        break;
                }
                if (state != null) {
                    TvInputManagerService.this.setStateLocked(inputId, state.intValue(), TvInputManagerService.this.mCurrentUserId);
                }
                TvInputManagerService tvInputManagerService = TvInputManagerService.this;
                UserState userState = tvInputManagerService.getOrCreateUserStateLocked(tvInputManagerService.mCurrentUserId);
                for (ServiceState serviceState : userState.serviceStateMap.values()) {
                    if (serviceState.isHardware && serviceState.service != null) {
                        try {
                            serviceState.service.notifyHdmiDeviceUpdated(deviceInfo);
                        } catch (RemoteException e) {
                            Slog.e(TvInputManagerService.TAG, "error in notifyHdmiDeviceUpdated", e);
                        }
                    }
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class SessionNotFoundException extends IllegalArgumentException {
        public SessionNotFoundException(String name) {
            super(name);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class ClientPidNotFoundException extends IllegalArgumentException {
        public ClientPidNotFoundException(String name) {
            super(name);
        }
    }
}
