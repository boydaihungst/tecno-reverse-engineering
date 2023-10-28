package com.android.server.tv.interactive;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.UserInfo;
import android.graphics.Rect;
import android.media.tv.AdRequest;
import android.media.tv.AdResponse;
import android.media.tv.BroadcastInfoRequest;
import android.media.tv.BroadcastInfoResponse;
import android.media.tv.TvTrackInfo;
import android.media.tv.interactive.AppLinkInfo;
import android.media.tv.interactive.ITvInteractiveAppClient;
import android.media.tv.interactive.ITvInteractiveAppManager;
import android.media.tv.interactive.ITvInteractiveAppManagerCallback;
import android.media.tv.interactive.ITvInteractiveAppService;
import android.media.tv.interactive.ITvInteractiveAppServiceCallback;
import android.media.tv.interactive.ITvInteractiveAppSession;
import android.media.tv.interactive.ITvInteractiveAppSessionCallback;
import android.media.tv.interactive.TvInteractiveAppServiceInfo;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.ArrayMap;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.view.InputChannel;
import android.view.Surface;
import com.android.internal.content.PackageMonitor;
import com.android.server.SystemService;
import com.android.server.utils.Slogf;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
/* loaded from: classes2.dex */
public class TvInteractiveAppManagerService extends SystemService {
    private static final boolean DEBUG = false;
    private static final String TAG = "TvInteractiveAppManagerService";
    private final Context mContext;
    private int mCurrentUserId;
    private boolean mGetServiceListCalled;
    private final Object mLock;
    private final Set<Integer> mRunningProfiles;
    private final UserManager mUserManager;
    private final SparseArray<UserState> mUserStates;

    public TvInteractiveAppManagerService(Context context) {
        super(context);
        this.mLock = new Object();
        this.mCurrentUserId = 0;
        this.mRunningProfiles = new HashSet();
        this.mUserStates = new SparseArray<>();
        this.mGetServiceListCalled = false;
        this.mContext = context;
        this.mUserManager = (UserManager) getContext().getSystemService("user");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void buildTvInteractiveAppServiceListLocked(int userId, String[] updatedPackages) {
        UserState userState = getOrCreateUserStateLocked(userId);
        userState.mPackageSet.clear();
        PackageManager pm = this.mContext.getPackageManager();
        List<ResolveInfo> services = pm.queryIntentServicesAsUser(new Intent("android.media.tv.interactive.TvInteractiveAppService"), 132, userId);
        List<TvInteractiveAppServiceInfo> iAppList = new ArrayList<>();
        for (ResolveInfo ri : services) {
            ServiceInfo si = ri.serviceInfo;
            if (!"android.permission.BIND_TV_INTERACTIVE_APP".equals(si.permission)) {
                Slog.w(TAG, "Skipping TV interactiva app service " + si.name + ": it does not require the permission android.permission.BIND_TV_INTERACTIVE_APP");
            } else {
                try {
                    iAppList.add(new TvInteractiveAppServiceInfo(this.mContext, new ComponentName(si.packageName, si.name)));
                    userState.mPackageSet.add(si.packageName);
                } catch (Exception e) {
                    Slogf.e(TAG, "failed to load TV Interactive App service " + si.name, e);
                }
            }
        }
        Collections.sort(iAppList, Comparator.comparing(new Function() { // from class: com.android.server.tv.interactive.TvInteractiveAppManagerService$$ExternalSyntheticLambda0
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return ((TvInteractiveAppServiceInfo) obj).getId();
            }
        }));
        Map<String, TvInteractiveAppState> iAppMap = new HashMap<>();
        ArrayMap<String, Integer> tiasAppCount = new ArrayMap<>(iAppMap.size());
        for (TvInteractiveAppServiceInfo info : iAppList) {
            String iAppServiceId = info.getId();
            Integer count = tiasAppCount.get(iAppServiceId);
            Integer count2 = Integer.valueOf(count != null ? 1 + count.intValue() : 1);
            tiasAppCount.put(iAppServiceId, count2);
            TvInteractiveAppState iAppState = (TvInteractiveAppState) userState.mIAppMap.get(iAppServiceId);
            if (iAppState == null) {
                iAppState = new TvInteractiveAppState();
            }
            iAppState.mInfo = info;
            iAppState.mUid = getInteractiveAppUid(info);
            iAppState.mComponentName = info.getComponent();
            iAppMap.put(iAppServiceId, iAppState);
            iAppState.mIAppNumber = count2.intValue();
        }
        for (String iAppServiceId2 : iAppMap.keySet()) {
            if (!userState.mIAppMap.containsKey(iAppServiceId2)) {
                notifyInteractiveAppServiceAddedLocked(userState, iAppServiceId2);
            } else if (updatedPackages != null) {
                ComponentName component = iAppMap.get(iAppServiceId2).mInfo.getComponent();
                int length = updatedPackages.length;
                int i = 0;
                while (true) {
                    if (i < length) {
                        String updatedPackage = updatedPackages[i];
                        if (!component.getPackageName().equals(updatedPackage)) {
                            i++;
                        } else {
                            updateServiceConnectionLocked(component, userId);
                            notifyInteractiveAppServiceUpdatedLocked(userState, iAppServiceId2);
                            break;
                        }
                    }
                }
            }
        }
        for (String iAppServiceId3 : userState.mIAppMap.keySet()) {
            if (!iAppMap.containsKey(iAppServiceId3)) {
                ServiceState serviceState = (ServiceState) userState.mServiceStateMap.get(((TvInteractiveAppState) userState.mIAppMap.get(iAppServiceId3)).mInfo.getComponent());
                if (serviceState != null) {
                    abortPendingCreateSessionRequestsLocked(serviceState, iAppServiceId3, userId);
                }
                notifyInteractiveAppServiceRemovedLocked(userState, iAppServiceId3);
            }
        }
        userState.mIAppMap.clear();
        userState.mIAppMap = iAppMap;
    }

    private void notifyInteractiveAppServiceAddedLocked(UserState userState, String iAppServiceId) {
        int n = userState.mCallbacks.beginBroadcast();
        for (int i = 0; i < n; i++) {
            try {
                userState.mCallbacks.getBroadcastItem(i).onInteractiveAppServiceAdded(iAppServiceId);
            } catch (RemoteException e) {
                Slog.e(TAG, "failed to report added Interactive App service to callback", e);
            }
        }
        userState.mCallbacks.finishBroadcast();
    }

    private void notifyInteractiveAppServiceRemovedLocked(UserState userState, String iAppServiceId) {
        int n = userState.mCallbacks.beginBroadcast();
        for (int i = 0; i < n; i++) {
            try {
                userState.mCallbacks.getBroadcastItem(i).onInteractiveAppServiceRemoved(iAppServiceId);
            } catch (RemoteException e) {
                Slog.e(TAG, "failed to report removed Interactive App service to callback", e);
            }
        }
        userState.mCallbacks.finishBroadcast();
    }

    private void notifyInteractiveAppServiceUpdatedLocked(UserState userState, String iAppServiceId) {
        int n = userState.mCallbacks.beginBroadcast();
        for (int i = 0; i < n; i++) {
            try {
                userState.mCallbacks.getBroadcastItem(i).onInteractiveAppServiceUpdated(iAppServiceId);
            } catch (RemoteException e) {
                Slog.e(TAG, "failed to report updated Interactive App service to callback", e);
            }
        }
        userState.mCallbacks.finishBroadcast();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyStateChangedLocked(UserState userState, String iAppServiceId, int type, int state, int err) {
        int n = userState.mCallbacks.beginBroadcast();
        for (int i = 0; i < n; i++) {
            try {
                userState.mCallbacks.getBroadcastItem(i).onStateChanged(iAppServiceId, type, state, err);
            } catch (RemoteException e) {
                Slog.e(TAG, "failed to report RTE state changed", e);
            }
        }
        userState.mCallbacks.finishBroadcast();
    }

    private int getInteractiveAppUid(TvInteractiveAppServiceInfo info) {
        try {
            return getContext().getPackageManager().getApplicationInfo(info.getServiceInfo().packageName, 0).uid;
        } catch (PackageManager.NameNotFoundException e) {
            Slogf.w(TAG, "Unable to get UID for  " + info, e);
            return -1;
        }
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        publishBinderService("tv_interactive_app", new BinderService());
    }

    @Override // com.android.server.SystemService
    public void onBootPhase(int phase) {
        if (phase == 500) {
            registerBroadcastReceivers();
        } else if (phase == 600) {
            synchronized (this.mLock) {
                buildTvInteractiveAppServiceListLocked(this.mCurrentUserId, null);
            }
        }
    }

    private void registerBroadcastReceivers() {
        PackageMonitor monitor = new PackageMonitor() { // from class: com.android.server.tv.interactive.TvInteractiveAppManagerService.1
            private void buildTvInteractiveAppServiceList(String[] packages) {
                int userId = getChangingUserId();
                synchronized (TvInteractiveAppManagerService.this.mLock) {
                    if (TvInteractiveAppManagerService.this.mCurrentUserId == userId || TvInteractiveAppManagerService.this.mRunningProfiles.contains(Integer.valueOf(userId))) {
                        TvInteractiveAppManagerService.this.buildTvInteractiveAppServiceListLocked(userId, packages);
                    }
                }
            }

            public void onPackageUpdateFinished(String packageName, int uid) {
                buildTvInteractiveAppServiceList(new String[]{packageName});
            }

            public void onPackagesAvailable(String[] packages) {
                if (isReplacing()) {
                    buildTvInteractiveAppServiceList(packages);
                }
            }

            public void onPackagesUnavailable(String[] packages) {
                if (isReplacing()) {
                    buildTvInteractiveAppServiceList(packages);
                }
            }

            public void onSomePackagesChanged() {
                if (isReplacing()) {
                    return;
                }
                buildTvInteractiveAppServiceList(null);
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
        this.mContext.registerReceiverAsUser(new BroadcastReceiver() { // from class: com.android.server.tv.interactive.TvInteractiveAppManagerService.2
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if ("android.intent.action.USER_SWITCHED".equals(action)) {
                    TvInteractiveAppManagerService.this.switchUser(intent.getIntExtra("android.intent.extra.user_handle", 0));
                } else if ("android.intent.action.USER_REMOVED".equals(action)) {
                    TvInteractiveAppManagerService.this.removeUser(intent.getIntExtra("android.intent.extra.user_handle", 0));
                } else if ("android.intent.action.USER_STARTED".equals(action)) {
                    int userId = intent.getIntExtra("android.intent.extra.user_handle", 0);
                    TvInteractiveAppManagerService.this.startUser(userId);
                } else if ("android.intent.action.USER_STOPPED".equals(action)) {
                    int userId2 = intent.getIntExtra("android.intent.extra.user_handle", 0);
                    TvInteractiveAppManagerService.this.stopUser(userId2);
                }
            }
        }, UserHandle.ALL, intentFilter, null, null);
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
            buildTvInteractiveAppServiceListLocked(userId, null);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void removeUser(int userId) {
        synchronized (this.mLock) {
            UserState userState = getUserStateLocked(userId);
            if (userState == null) {
                return;
            }
            for (SessionState state : userState.mSessionStateMap.values()) {
                if (state.mSession != null) {
                    try {
                        state.mSession.release();
                    } catch (RemoteException e) {
                        Slog.e(TAG, "error in release", e);
                    }
                }
            }
            userState.mSessionStateMap.clear();
            for (ServiceState serviceState : userState.mServiceStateMap.values()) {
                if (serviceState.mService != null) {
                    if (serviceState.mCallback != null) {
                        try {
                            serviceState.mService.unregisterCallback(serviceState.mCallback);
                        } catch (RemoteException e2) {
                            Slog.e(TAG, "error in unregisterCallback", e2);
                        }
                    }
                    this.mContext.unbindService(serviceState.mConnection);
                }
            }
            userState.mServiceStateMap.clear();
            userState.mIAppMap.clear();
            userState.mPackageSet.clear();
            userState.mClientStateMap.clear();
            userState.mCallbacks.kill();
            this.mRunningProfiles.remove(Integer.valueOf(userId));
            this.mUserStates.remove(userId);
            if (userId == this.mCurrentUserId) {
                switchUser(0);
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
        buildTvInteractiveAppServiceListLocked(userId, null);
    }

    private void releaseSessionOfUserLocked(int userId) {
        UserState userState = getUserStateLocked(userId);
        if (userState == null) {
            return;
        }
        List<SessionState> sessionStatesToRelease = new ArrayList<>();
        for (SessionState sessionState : userState.mSessionStateMap.values()) {
            if (sessionState.mSession != null) {
                sessionStatesToRelease.add(sessionState);
            }
        }
        for (SessionState sessionState2 : sessionStatesToRelease) {
            try {
                sessionState2.mSession.release();
            } catch (RemoteException e) {
                Slog.e(TAG, "error in release", e);
            }
            clearSessionAndNotifyClientLocked(sessionState2);
        }
    }

    private void unbindServiceOfUserLocked(int userId) {
        UserState userState = getUserStateLocked(userId);
        if (userState == null) {
            return;
        }
        Iterator<ComponentName> it = userState.mServiceStateMap.keySet().iterator();
        while (it.hasNext()) {
            ComponentName component = it.next();
            ServiceState serviceState = (ServiceState) userState.mServiceStateMap.get(component);
            if (serviceState != null && serviceState.mSessionTokens.isEmpty()) {
                if (serviceState.mCallback != null) {
                    try {
                        serviceState.mService.unregisterCallback(serviceState.mCallback);
                    } catch (RemoteException e) {
                        Slog.e(TAG, "error in unregisterCallback", e);
                    }
                }
                this.mContext.unbindService(serviceState.mConnection);
                it.remove();
            }
        }
    }

    private void clearSessionAndNotifyClientLocked(SessionState state) {
        if (state.mClient != null) {
            try {
                state.mClient.onSessionReleased(state.mSeq);
            } catch (RemoteException e) {
                Slog.e(TAG, "error in onSessionReleased", e);
            }
        }
        removeSessionStateLocked(state.mSessionToken, state.mUserId);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int resolveCallingUserId(int callingPid, int callingUid, int requestedUserId, String methodName) {
        return ActivityManager.handleIncomingUser(callingPid, callingUid, requestedUserId, false, false, methodName, null);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public UserState getOrCreateUserStateLocked(int userId) {
        UserState userState = getUserStateLocked(userId);
        if (userState == null) {
            UserState userState2 = new UserState(userId);
            this.mUserStates.put(userId, userState2);
            return userState2;
        }
        return userState;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public UserState getUserStateLocked(int userId) {
        return this.mUserStates.get(userId);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public ServiceState getServiceStateLocked(ComponentName component, int userId) {
        UserState userState = getOrCreateUserStateLocked(userId);
        ServiceState serviceState = (ServiceState) userState.mServiceStateMap.get(component);
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

    private SessionState getSessionStateLocked(IBinder sessionToken, int callingUid, UserState userState) {
        SessionState sessionState = (SessionState) userState.mSessionStateMap.get(sessionToken);
        if (sessionState == null) {
            throw new SessionNotFoundException("Session state not found for token " + sessionToken);
        }
        if (callingUid != 1000 && callingUid != sessionState.mCallingUid) {
            throw new SecurityException("Illegal access to the session with token " + sessionToken + " from uid " + callingUid);
        }
        return sessionState;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public ITvInteractiveAppSession getSessionLocked(IBinder sessionToken, int callingUid, int userId) {
        return getSessionLocked(getSessionStateLocked(sessionToken, callingUid, userId));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public ITvInteractiveAppSession getSessionLocked(SessionState sessionState) {
        ITvInteractiveAppSession session = sessionState.mSession;
        if (session == null) {
            throw new IllegalStateException("Session not yet created for token " + sessionState.mSessionToken);
        }
        return session;
    }

    /* loaded from: classes2.dex */
    private final class BinderService extends ITvInteractiveAppManager.Stub {
        private BinderService() {
        }

        public List<TvInteractiveAppServiceInfo> getTvInteractiveAppServiceList(int userId) {
            List<TvInteractiveAppServiceInfo> iAppList;
            int resolvedUserId = TvInteractiveAppManagerService.this.resolveCallingUserId(Binder.getCallingPid(), Binder.getCallingUid(), userId, "getTvInteractiveAppServiceList");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInteractiveAppManagerService.this.mLock) {
                    if (!TvInteractiveAppManagerService.this.mGetServiceListCalled) {
                        TvInteractiveAppManagerService.this.buildTvInteractiveAppServiceListLocked(userId, null);
                        TvInteractiveAppManagerService.this.mGetServiceListCalled = true;
                    }
                    UserState userState = TvInteractiveAppManagerService.this.getOrCreateUserStateLocked(resolvedUserId);
                    iAppList = new ArrayList<>();
                    for (TvInteractiveAppState state : userState.mIAppMap.values()) {
                        iAppList.add(state.mInfo);
                    }
                }
                return iAppList;
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [699=4] */
        public void registerAppLinkInfo(String tiasId, AppLinkInfo appLinkInfo, int userId) {
            int resolvedUserId = TvInteractiveAppManagerService.this.resolveCallingUserId(Binder.getCallingPid(), Binder.getCallingUid(), userId, "registerAppLinkInfo: " + appLinkInfo);
            long identity = Binder.clearCallingIdentity();
            try {
                try {
                } catch (RemoteException e) {
                    Slogf.e(TvInteractiveAppManagerService.TAG, "error in registerAppLinkInfo", e);
                }
                synchronized (TvInteractiveAppManagerService.this.mLock) {
                    UserState userState = TvInteractiveAppManagerService.this.getOrCreateUserStateLocked(resolvedUserId);
                    TvInteractiveAppState iAppState = (TvInteractiveAppState) userState.mIAppMap.get(tiasId);
                    if (iAppState == null) {
                        Slogf.e(TvInteractiveAppManagerService.TAG, "failed to registerAppLinkInfo - unknown TIAS id " + tiasId);
                        return;
                    }
                    ComponentName componentName = iAppState.mInfo.getComponent();
                    ServiceState serviceState = (ServiceState) userState.mServiceStateMap.get(componentName);
                    if (serviceState == null) {
                        ServiceState serviceState2 = new ServiceState(componentName, tiasId, resolvedUserId);
                        serviceState2.addPendingAppLink(appLinkInfo, true);
                        userState.mServiceStateMap.put(componentName, serviceState2);
                        TvInteractiveAppManagerService.this.updateServiceConnectionLocked(componentName, resolvedUserId);
                    } else if (serviceState.mService != null) {
                        serviceState.mService.registerAppLinkInfo(appLinkInfo);
                    } else {
                        serviceState.addPendingAppLink(appLinkInfo, true);
                        TvInteractiveAppManagerService.this.updateServiceConnectionLocked(componentName, resolvedUserId);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [735=4] */
        public void unregisterAppLinkInfo(String tiasId, AppLinkInfo appLinkInfo, int userId) {
            int resolvedUserId = TvInteractiveAppManagerService.this.resolveCallingUserId(Binder.getCallingPid(), Binder.getCallingUid(), userId, "unregisterAppLinkInfo: " + appLinkInfo);
            long identity = Binder.clearCallingIdentity();
            try {
                try {
                } catch (RemoteException e) {
                    Slogf.e(TvInteractiveAppManagerService.TAG, "error in unregisterAppLinkInfo", e);
                }
                synchronized (TvInteractiveAppManagerService.this.mLock) {
                    UserState userState = TvInteractiveAppManagerService.this.getOrCreateUserStateLocked(resolvedUserId);
                    TvInteractiveAppState iAppState = (TvInteractiveAppState) userState.mIAppMap.get(tiasId);
                    if (iAppState == null) {
                        Slogf.e(TvInteractiveAppManagerService.TAG, "failed to unregisterAppLinkInfo - unknown TIAS id " + tiasId);
                        return;
                    }
                    ComponentName componentName = iAppState.mInfo.getComponent();
                    ServiceState serviceState = (ServiceState) userState.mServiceStateMap.get(componentName);
                    if (serviceState == null) {
                        ServiceState serviceState2 = new ServiceState(componentName, tiasId, resolvedUserId);
                        serviceState2.addPendingAppLink(appLinkInfo, false);
                        userState.mServiceStateMap.put(componentName, serviceState2);
                        TvInteractiveAppManagerService.this.updateServiceConnectionLocked(componentName, resolvedUserId);
                    } else if (serviceState.mService != null) {
                        serviceState.mService.unregisterAppLinkInfo(appLinkInfo);
                    } else {
                        serviceState.addPendingAppLink(appLinkInfo, false);
                        TvInteractiveAppManagerService.this.updateServiceConnectionLocked(componentName, resolvedUserId);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [771=4] */
        public void sendAppLinkCommand(String tiasId, Bundle command, int userId) {
            int resolvedUserId = TvInteractiveAppManagerService.this.resolveCallingUserId(Binder.getCallingPid(), Binder.getCallingUid(), userId, "sendAppLinkCommand");
            long identity = Binder.clearCallingIdentity();
            try {
                try {
                } catch (RemoteException e) {
                    Slogf.e(TvInteractiveAppManagerService.TAG, "error in sendAppLinkCommand", e);
                }
                synchronized (TvInteractiveAppManagerService.this.mLock) {
                    UserState userState = TvInteractiveAppManagerService.this.getOrCreateUserStateLocked(resolvedUserId);
                    TvInteractiveAppState iAppState = (TvInteractiveAppState) userState.mIAppMap.get(tiasId);
                    if (iAppState == null) {
                        Slogf.e(TvInteractiveAppManagerService.TAG, "failed to sendAppLinkCommand - unknown TIAS id " + tiasId);
                        return;
                    }
                    ComponentName componentName = iAppState.mInfo.getComponent();
                    ServiceState serviceState = (ServiceState) userState.mServiceStateMap.get(componentName);
                    if (serviceState == null) {
                        ServiceState serviceState2 = new ServiceState(componentName, tiasId, resolvedUserId);
                        serviceState2.addPendingAppLinkCommand(command);
                        userState.mServiceStateMap.put(componentName, serviceState2);
                        TvInteractiveAppManagerService.this.updateServiceConnectionLocked(componentName, resolvedUserId);
                    } else if (serviceState.mService != null) {
                        serviceState.mService.sendAppLinkCommand(command);
                    } else {
                        serviceState.addPendingAppLinkCommand(command);
                        TvInteractiveAppManagerService.this.updateServiceConnectionLocked(componentName, resolvedUserId);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [835=4, 837=6] */
        public void createSession(ITvInteractiveAppClient client, String iAppServiceId, int type, int seq, int userId) {
            ServiceState serviceState;
            int callingUid = Binder.getCallingUid();
            int callingPid = Binder.getCallingPid();
            int resolvedUserId = TvInteractiveAppManagerService.this.resolveCallingUserId(callingPid, callingUid, userId, "createSession");
            long identity = Binder.clearCallingIdentity();
            try {
                try {
                    synchronized (TvInteractiveAppManagerService.this.mLock) {
                        try {
                            if (userId != TvInteractiveAppManagerService.this.mCurrentUserId) {
                                try {
                                    if (!TvInteractiveAppManagerService.this.mRunningProfiles.contains(Integer.valueOf(userId))) {
                                        TvInteractiveAppManagerService.this.sendSessionTokenToClientLocked(client, iAppServiceId, null, null, seq);
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
                            UserState userState = TvInteractiveAppManagerService.this.getOrCreateUserStateLocked(resolvedUserId);
                            TvInteractiveAppState iAppState = (TvInteractiveAppState) userState.mIAppMap.get(iAppServiceId);
                            if (iAppState == null) {
                                Slogf.w(TvInteractiveAppManagerService.TAG, "Failed to find state for iAppServiceId=" + iAppServiceId);
                                TvInteractiveAppManagerService.this.sendSessionTokenToClientLocked(client, iAppServiceId, null, null, seq);
                                Binder.restoreCallingIdentity(identity);
                                return;
                            }
                            ServiceState serviceState2 = (ServiceState) userState.mServiceStateMap.get(iAppState.mComponentName);
                            if (serviceState2 == null) {
                                int i = PackageManager.getApplicationInfoAsUserCached(iAppState.mComponentName.getPackageName(), 0L, resolvedUserId).uid;
                                ServiceState serviceState3 = new ServiceState(iAppState.mComponentName, iAppServiceId, resolvedUserId);
                                userState.mServiceStateMap.put(iAppState.mComponentName, serviceState3);
                                serviceState = serviceState3;
                            } else {
                                serviceState = serviceState2;
                            }
                            if (serviceState.mReconnecting) {
                                TvInteractiveAppManagerService.this.sendSessionTokenToClientLocked(client, iAppServiceId, null, null, seq);
                                Binder.restoreCallingIdentity(identity);
                                return;
                            }
                            Binder binder = new Binder();
                            try {
                                SessionState sessionState = new SessionState(binder, iAppServiceId, type, iAppState.mComponentName, client, seq, callingUid, callingPid, resolvedUserId);
                                userState.mSessionStateMap.put(binder, sessionState);
                                serviceState.mSessionTokens.add(binder);
                                if (serviceState.mService == null) {
                                    TvInteractiveAppManagerService.this.updateServiceConnectionLocked(iAppState.mComponentName, resolvedUserId);
                                } else if (!TvInteractiveAppManagerService.this.createSessionInternalLocked(serviceState.mService, binder, resolvedUserId)) {
                                    TvInteractiveAppManagerService.this.removeSessionStateLocked(binder, resolvedUserId);
                                }
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
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInteractiveAppManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "releaseSession");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInteractiveAppManagerService.this.mLock) {
                    TvInteractiveAppManagerService.this.releaseSessionLocked(sessionToken, callingUid, resolvedUserId);
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void notifyTuned(IBinder sessionToken, Uri channelUri, int userId) {
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInteractiveAppManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "notifyTuned");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInteractiveAppManagerService.this.mLock) {
                    try {
                        SessionState sessionState = TvInteractiveAppManagerService.this.getSessionStateLocked(sessionToken, callingUid, resolvedUserId);
                        TvInteractiveAppManagerService.this.getSessionLocked(sessionState).notifyTuned(channelUri);
                    } catch (RemoteException | SessionNotFoundException e) {
                        Slogf.e(TvInteractiveAppManagerService.TAG, "error in notifyTuned", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void notifyTrackSelected(IBinder sessionToken, int type, String trackId, int userId) {
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInteractiveAppManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "notifyTrackSelected");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInteractiveAppManagerService.this.mLock) {
                    try {
                        SessionState sessionState = TvInteractiveAppManagerService.this.getSessionStateLocked(sessionToken, callingUid, resolvedUserId);
                        TvInteractiveAppManagerService.this.getSessionLocked(sessionState).notifyTrackSelected(type, trackId);
                    } catch (RemoteException | SessionNotFoundException e) {
                        Slogf.e(TvInteractiveAppManagerService.TAG, "error in notifyTrackSelected", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void notifyTracksChanged(IBinder sessionToken, List<TvTrackInfo> tracks, int userId) {
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInteractiveAppManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "notifyTracksChanged");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInteractiveAppManagerService.this.mLock) {
                    try {
                        SessionState sessionState = TvInteractiveAppManagerService.this.getSessionStateLocked(sessionToken, callingUid, resolvedUserId);
                        TvInteractiveAppManagerService.this.getSessionLocked(sessionState).notifyTracksChanged(tracks);
                    } catch (RemoteException | SessionNotFoundException e) {
                        Slogf.e(TvInteractiveAppManagerService.TAG, "error in notifyTracksChanged", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void notifyVideoAvailable(IBinder sessionToken, int userId) {
            int callingUid = Binder.getCallingUid();
            int callingPid = Binder.getCallingPid();
            int resolvedUserId = TvInteractiveAppManagerService.this.resolveCallingUserId(callingPid, callingUid, userId, "notifyVideoAvailable");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInteractiveAppManagerService.this.mLock) {
                    try {
                        SessionState sessionState = TvInteractiveAppManagerService.this.getSessionStateLocked(sessionToken, callingUid, resolvedUserId);
                        TvInteractiveAppManagerService.this.getSessionLocked(sessionState).notifyVideoAvailable();
                    } catch (RemoteException | SessionNotFoundException e) {
                        Slogf.e(TvInteractiveAppManagerService.TAG, "error in notifyVideoAvailable", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void notifyVideoUnavailable(IBinder sessionToken, int reason, int userId) {
            int callingUid = Binder.getCallingUid();
            int callingPid = Binder.getCallingPid();
            int resolvedUserId = TvInteractiveAppManagerService.this.resolveCallingUserId(callingPid, callingUid, userId, "notifyVideoUnavailable");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInteractiveAppManagerService.this.mLock) {
                    try {
                        SessionState sessionState = TvInteractiveAppManagerService.this.getSessionStateLocked(sessionToken, callingUid, resolvedUserId);
                        TvInteractiveAppManagerService.this.getSessionLocked(sessionState).notifyVideoUnavailable(reason);
                    } catch (RemoteException | SessionNotFoundException e) {
                        Slogf.e(TvInteractiveAppManagerService.TAG, "error in notifyVideoUnavailable", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void notifyContentAllowed(IBinder sessionToken, int userId) {
            int callingUid = Binder.getCallingUid();
            int callingPid = Binder.getCallingPid();
            int resolvedUserId = TvInteractiveAppManagerService.this.resolveCallingUserId(callingPid, callingUid, userId, "notifyContentAllowed");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInteractiveAppManagerService.this.mLock) {
                    try {
                        SessionState sessionState = TvInteractiveAppManagerService.this.getSessionStateLocked(sessionToken, callingUid, resolvedUserId);
                        TvInteractiveAppManagerService.this.getSessionLocked(sessionState).notifyContentAllowed();
                    } catch (RemoteException | SessionNotFoundException e) {
                        Slogf.e(TvInteractiveAppManagerService.TAG, "error in notifyContentAllowed", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void notifyContentBlocked(IBinder sessionToken, String rating, int userId) {
            int callingUid = Binder.getCallingUid();
            int callingPid = Binder.getCallingPid();
            int resolvedUserId = TvInteractiveAppManagerService.this.resolveCallingUserId(callingPid, callingUid, userId, "notifyContentBlocked");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInteractiveAppManagerService.this.mLock) {
                    try {
                        SessionState sessionState = TvInteractiveAppManagerService.this.getSessionStateLocked(sessionToken, callingUid, resolvedUserId);
                        TvInteractiveAppManagerService.this.getSessionLocked(sessionState).notifyContentBlocked(rating);
                    } catch (RemoteException | SessionNotFoundException e) {
                        Slogf.e(TvInteractiveAppManagerService.TAG, "error in notifyContentBlocked", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void notifySignalStrength(IBinder sessionToken, int strength, int userId) {
            int callingUid = Binder.getCallingUid();
            int callingPid = Binder.getCallingPid();
            int resolvedUserId = TvInteractiveAppManagerService.this.resolveCallingUserId(callingPid, callingUid, userId, "notifySignalStrength");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInteractiveAppManagerService.this.mLock) {
                    try {
                        SessionState sessionState = TvInteractiveAppManagerService.this.getSessionStateLocked(sessionToken, callingUid, resolvedUserId);
                        TvInteractiveAppManagerService.this.getSessionLocked(sessionState).notifySignalStrength(strength);
                    } catch (RemoteException | SessionNotFoundException e) {
                        Slogf.e(TvInteractiveAppManagerService.TAG, "error in notifySignalStrength", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void startInteractiveApp(IBinder sessionToken, int userId) {
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInteractiveAppManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "startInteractiveApp");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInteractiveAppManagerService.this.mLock) {
                    try {
                        SessionState sessionState = TvInteractiveAppManagerService.this.getSessionStateLocked(sessionToken, callingUid, resolvedUserId);
                        TvInteractiveAppManagerService.this.getSessionLocked(sessionState).startInteractiveApp();
                    } catch (RemoteException | SessionNotFoundException e) {
                        Slogf.e(TvInteractiveAppManagerService.TAG, "error in start", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void stopInteractiveApp(IBinder sessionToken, int userId) {
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInteractiveAppManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "stopInteractiveApp");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInteractiveAppManagerService.this.mLock) {
                    try {
                        SessionState sessionState = TvInteractiveAppManagerService.this.getSessionStateLocked(sessionToken, callingUid, resolvedUserId);
                        TvInteractiveAppManagerService.this.getSessionLocked(sessionState).stopInteractiveApp();
                    } catch (RemoteException | SessionNotFoundException e) {
                        Slogf.e(TvInteractiveAppManagerService.TAG, "error in stop", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void resetInteractiveApp(IBinder sessionToken, int userId) {
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInteractiveAppManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "resetInteractiveApp");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInteractiveAppManagerService.this.mLock) {
                    try {
                        SessionState sessionState = TvInteractiveAppManagerService.this.getSessionStateLocked(sessionToken, callingUid, resolvedUserId);
                        TvInteractiveAppManagerService.this.getSessionLocked(sessionState).resetInteractiveApp();
                    } catch (RemoteException | SessionNotFoundException e) {
                        Slogf.e(TvInteractiveAppManagerService.TAG, "error in reset", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void createBiInteractiveApp(IBinder sessionToken, Uri biIAppUri, Bundle params, int userId) {
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInteractiveAppManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "createBiInteractiveApp");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInteractiveAppManagerService.this.mLock) {
                    try {
                        SessionState sessionState = TvInteractiveAppManagerService.this.getSessionStateLocked(sessionToken, callingUid, resolvedUserId);
                        TvInteractiveAppManagerService.this.getSessionLocked(sessionState).createBiInteractiveApp(biIAppUri, params);
                    } catch (RemoteException | SessionNotFoundException e) {
                        Slogf.e(TvInteractiveAppManagerService.TAG, "error in createBiInteractiveApp", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void destroyBiInteractiveApp(IBinder sessionToken, String biIAppId, int userId) {
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInteractiveAppManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "destroyBiInteractiveApp");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInteractiveAppManagerService.this.mLock) {
                    try {
                        SessionState sessionState = TvInteractiveAppManagerService.this.getSessionStateLocked(sessionToken, callingUid, resolvedUserId);
                        TvInteractiveAppManagerService.this.getSessionLocked(sessionState).destroyBiInteractiveApp(biIAppId);
                    } catch (RemoteException | SessionNotFoundException e) {
                        Slogf.e(TvInteractiveAppManagerService.TAG, "error in destroyBiInteractiveApp", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void setTeletextAppEnabled(IBinder sessionToken, boolean enable, int userId) {
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInteractiveAppManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "setTeletextAppEnabled");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInteractiveAppManagerService.this.mLock) {
                    try {
                        SessionState sessionState = TvInteractiveAppManagerService.this.getSessionStateLocked(sessionToken, callingUid, resolvedUserId);
                        TvInteractiveAppManagerService.this.getSessionLocked(sessionState).setTeletextAppEnabled(enable);
                    } catch (RemoteException | SessionNotFoundException e) {
                        Slogf.e(TvInteractiveAppManagerService.TAG, "error in setTeletextAppEnabled", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void sendCurrentChannelUri(IBinder sessionToken, Uri channelUri, int userId) {
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInteractiveAppManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "sendCurrentChannelUri");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInteractiveAppManagerService.this.mLock) {
                    try {
                        SessionState sessionState = TvInteractiveAppManagerService.this.getSessionStateLocked(sessionToken, callingUid, resolvedUserId);
                        TvInteractiveAppManagerService.this.getSessionLocked(sessionState).sendCurrentChannelUri(channelUri);
                    } catch (RemoteException | SessionNotFoundException e) {
                        Slogf.e(TvInteractiveAppManagerService.TAG, "error in sendCurrentChannelUri", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void sendCurrentChannelLcn(IBinder sessionToken, int lcn, int userId) {
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInteractiveAppManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "sendCurrentChannelLcn");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInteractiveAppManagerService.this.mLock) {
                    try {
                        SessionState sessionState = TvInteractiveAppManagerService.this.getSessionStateLocked(sessionToken, callingUid, resolvedUserId);
                        TvInteractiveAppManagerService.this.getSessionLocked(sessionState).sendCurrentChannelLcn(lcn);
                    } catch (RemoteException | SessionNotFoundException e) {
                        Slogf.e(TvInteractiveAppManagerService.TAG, "error in sendCurrentChannelLcn", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void sendStreamVolume(IBinder sessionToken, float volume, int userId) {
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInteractiveAppManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "sendStreamVolume");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInteractiveAppManagerService.this.mLock) {
                    try {
                        SessionState sessionState = TvInteractiveAppManagerService.this.getSessionStateLocked(sessionToken, callingUid, resolvedUserId);
                        TvInteractiveAppManagerService.this.getSessionLocked(sessionState).sendStreamVolume(volume);
                    } catch (RemoteException | SessionNotFoundException e) {
                        Slogf.e(TvInteractiveAppManagerService.TAG, "error in sendStreamVolume", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void sendTrackInfoList(IBinder sessionToken, List<TvTrackInfo> tracks, int userId) {
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInteractiveAppManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "sendTrackInfoList");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInteractiveAppManagerService.this.mLock) {
                    try {
                        SessionState sessionState = TvInteractiveAppManagerService.this.getSessionStateLocked(sessionToken, callingUid, resolvedUserId);
                        TvInteractiveAppManagerService.this.getSessionLocked(sessionState).sendTrackInfoList(tracks);
                    } catch (RemoteException | SessionNotFoundException e) {
                        Slogf.e(TvInteractiveAppManagerService.TAG, "error in sendTrackInfoList", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void sendCurrentTvInputId(IBinder sessionToken, String inputId, int userId) {
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInteractiveAppManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "sendCurrentTvInputId");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInteractiveAppManagerService.this.mLock) {
                    try {
                        SessionState sessionState = TvInteractiveAppManagerService.this.getSessionStateLocked(sessionToken, callingUid, resolvedUserId);
                        TvInteractiveAppManagerService.this.getSessionLocked(sessionState).sendCurrentTvInputId(inputId);
                    } catch (RemoteException | SessionNotFoundException e) {
                        Slogf.e(TvInteractiveAppManagerService.TAG, "error in sendCurrentTvInputId", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void sendSigningResult(IBinder sessionToken, String signingId, byte[] result, int userId) {
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInteractiveAppManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "sendSigningResult");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInteractiveAppManagerService.this.mLock) {
                    try {
                        SessionState sessionState = TvInteractiveAppManagerService.this.getSessionStateLocked(sessionToken, callingUid, resolvedUserId);
                        TvInteractiveAppManagerService.this.getSessionLocked(sessionState).sendSigningResult(signingId, result);
                    } catch (RemoteException | SessionNotFoundException e) {
                        Slogf.e(TvInteractiveAppManagerService.TAG, "error in sendSigningResult", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void notifyError(IBinder sessionToken, String errMsg, Bundle params, int userId) {
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInteractiveAppManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "notifyError");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInteractiveAppManagerService.this.mLock) {
                    try {
                        SessionState sessionState = TvInteractiveAppManagerService.this.getSessionStateLocked(sessionToken, callingUid, resolvedUserId);
                        TvInteractiveAppManagerService.this.getSessionLocked(sessionState).notifyError(errMsg, params);
                    } catch (RemoteException | SessionNotFoundException e) {
                        Slogf.e(TvInteractiveAppManagerService.TAG, "error in notifyError", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void setSurface(IBinder sessionToken, Surface surface, int userId) {
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInteractiveAppManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "setSurface");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInteractiveAppManagerService.this.mLock) {
                    try {
                        SessionState sessionState = TvInteractiveAppManagerService.this.getSessionStateLocked(sessionToken, callingUid, resolvedUserId);
                        TvInteractiveAppManagerService.this.getSessionLocked(sessionState).setSurface(surface);
                    } catch (RemoteException | SessionNotFoundException e) {
                        Slogf.e(TvInteractiveAppManagerService.TAG, "error in setSurface", e);
                    }
                }
            } finally {
                if (surface != null) {
                    surface.release();
                }
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void dispatchSurfaceChanged(IBinder sessionToken, int format, int width, int height, int userId) {
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInteractiveAppManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "dispatchSurfaceChanged");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInteractiveAppManagerService.this.mLock) {
                    try {
                        SessionState sessionState = TvInteractiveAppManagerService.this.getSessionStateLocked(sessionToken, callingUid, resolvedUserId);
                        TvInteractiveAppManagerService.this.getSessionLocked(sessionState).dispatchSurfaceChanged(format, width, height);
                    } catch (RemoteException | SessionNotFoundException e) {
                        Slogf.e(TvInteractiveAppManagerService.TAG, "error in dispatchSurfaceChanged", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void notifyBroadcastInfoResponse(IBinder sessionToken, BroadcastInfoResponse response, int userId) {
            int callingUid = Binder.getCallingUid();
            int callingPid = Binder.getCallingPid();
            int resolvedUserId = TvInteractiveAppManagerService.this.resolveCallingUserId(callingPid, callingUid, userId, "notifyBroadcastInfoResponse");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInteractiveAppManagerService.this.mLock) {
                    try {
                        SessionState sessionState = TvInteractiveAppManagerService.this.getSessionStateLocked(sessionToken, callingUid, resolvedUserId);
                        TvInteractiveAppManagerService.this.getSessionLocked(sessionState).notifyBroadcastInfoResponse(response);
                    } catch (RemoteException | SessionNotFoundException e) {
                        Slogf.e(TvInteractiveAppManagerService.TAG, "error in notifyBroadcastInfoResponse", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void notifyAdResponse(IBinder sessionToken, AdResponse response, int userId) {
            int callingUid = Binder.getCallingUid();
            int callingPid = Binder.getCallingPid();
            int resolvedUserId = TvInteractiveAppManagerService.this.resolveCallingUserId(callingPid, callingUid, userId, "notifyAdResponse");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInteractiveAppManagerService.this.mLock) {
                    try {
                        SessionState sessionState = TvInteractiveAppManagerService.this.getSessionStateLocked(sessionToken, callingUid, resolvedUserId);
                        TvInteractiveAppManagerService.this.getSessionLocked(sessionState).notifyAdResponse(response);
                    } catch (RemoteException | SessionNotFoundException e) {
                        Slogf.e(TvInteractiveAppManagerService.TAG, "error in notifyAdResponse", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void registerCallback(ITvInteractiveAppManagerCallback callback, int userId) {
            int callingPid = Binder.getCallingPid();
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInteractiveAppManagerService.this.resolveCallingUserId(callingPid, callingUid, userId, "registerCallback");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInteractiveAppManagerService.this.mLock) {
                    UserState userState = TvInteractiveAppManagerService.this.getOrCreateUserStateLocked(resolvedUserId);
                    if (!userState.mCallbacks.register(callback)) {
                        Slog.e(TvInteractiveAppManagerService.TAG, "client process has already died");
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void unregisterCallback(ITvInteractiveAppManagerCallback callback, int userId) {
            int resolvedUserId = TvInteractiveAppManagerService.this.resolveCallingUserId(Binder.getCallingPid(), Binder.getCallingUid(), userId, "unregisterCallback");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInteractiveAppManagerService.this.mLock) {
                    UserState userState = TvInteractiveAppManagerService.this.getOrCreateUserStateLocked(resolvedUserId);
                    userState.mCallbacks.unregister(callback);
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void createMediaView(IBinder sessionToken, IBinder windowToken, Rect frame, int userId) {
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInteractiveAppManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "createMediaView");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInteractiveAppManagerService.this.mLock) {
                    try {
                        TvInteractiveAppManagerService.this.getSessionLocked(sessionToken, callingUid, resolvedUserId).createMediaView(windowToken, frame);
                    } catch (RemoteException | SessionNotFoundException e) {
                        Slog.e(TvInteractiveAppManagerService.TAG, "error in createMediaView", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void relayoutMediaView(IBinder sessionToken, Rect frame, int userId) {
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInteractiveAppManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "relayoutMediaView");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInteractiveAppManagerService.this.mLock) {
                    try {
                        TvInteractiveAppManagerService.this.getSessionLocked(sessionToken, callingUid, resolvedUserId).relayoutMediaView(frame);
                    } catch (RemoteException | SessionNotFoundException e) {
                        Slog.e(TvInteractiveAppManagerService.TAG, "error in relayoutMediaView", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void removeMediaView(IBinder sessionToken, int userId) {
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInteractiveAppManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "removeMediaView");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInteractiveAppManagerService.this.mLock) {
                    try {
                        TvInteractiveAppManagerService.this.getSessionLocked(sessionToken, callingUid, resolvedUserId).removeMediaView();
                    } catch (RemoteException | SessionNotFoundException e) {
                        Slog.e(TvInteractiveAppManagerService.TAG, "error in removeMediaView", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendSessionTokenToClientLocked(ITvInteractiveAppClient client, String iAppServiceId, IBinder sessionToken, InputChannel channel, int seq) {
        try {
            client.onSessionCreated(iAppServiceId, sessionToken, channel, seq);
        } catch (RemoteException e) {
            Slogf.e(TAG, "error in onSessionCreated", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean createSessionInternalLocked(ITvInteractiveAppService service, IBinder sessionToken, int userId) {
        UserState userState = getOrCreateUserStateLocked(userId);
        SessionState sessionState = (SessionState) userState.mSessionStateMap.get(sessionToken);
        InputChannel[] channels = InputChannel.openInputChannelPair(sessionToken.toString());
        boolean created = true;
        try {
            try {
                service.createSession(channels[1], new SessionCallback(sessionState, channels), sessionState.mIAppServiceId, sessionState.mType);
            } catch (RemoteException e) {
                e = e;
                Slogf.e(TAG, "error in createSession", e);
                sendSessionTokenToClientLocked(sessionState.mClient, sessionState.mIAppServiceId, null, null, sessionState.mSeq);
                created = false;
                channels[1].dispose();
                return created;
            }
        } catch (RemoteException e2) {
            e = e2;
        }
        channels[1].dispose();
        return created;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Code restructure failed: missing block: B:12:0x0035, code lost:
        if (r0 == null) goto L9;
     */
    /* JADX WARN: Code restructure failed: missing block: B:14:0x0038, code lost:
        removeSessionStateLocked(r6, r8);
     */
    /* JADX WARN: Code restructure failed: missing block: B:15:0x003b, code lost:
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
                getOrCreateUserStateLocked(userId);
                if (sessionState.mSession != null) {
                    sessionState.mSession.asBinder().unlinkToDeath(sessionState, 0);
                    sessionState.mSession.release();
                }
            } catch (RemoteException | SessionNotFoundException e) {
                Slogf.e(TAG, "error in releaseSession", e);
            }
        } finally {
            if (sessionState != null) {
                sessionState.mSession = null;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void removeSessionStateLocked(IBinder sessionToken, int userId) {
        UserState userState = getOrCreateUserStateLocked(userId);
        SessionState sessionState = (SessionState) userState.mSessionStateMap.remove(sessionToken);
        if (sessionState == null) {
            Slogf.e(TAG, "sessionState null, no more remove session action!");
            return;
        }
        ClientState clientState = (ClientState) userState.mClientStateMap.get(sessionState.mClient.asBinder());
        if (clientState != null) {
            clientState.mSessionTokens.remove(sessionToken);
            if (clientState.isEmpty()) {
                userState.mClientStateMap.remove(sessionState.mClient.asBinder());
                sessionState.mClient.asBinder().unlinkToDeath(clientState, 0);
            }
        }
        ServiceState serviceState = (ServiceState) userState.mServiceStateMap.get(sessionState.mComponent);
        if (serviceState != null) {
            serviceState.mSessionTokens.remove(sessionToken);
        }
        updateServiceConnectionLocked(sessionState.mComponent, userId);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void abortPendingCreateSessionRequestsLocked(ServiceState serviceState, String iAppServiceId, int userId) {
        UserState userState = getOrCreateUserStateLocked(userId);
        List<SessionState> sessionsToAbort = new ArrayList<>();
        for (IBinder sessionToken : serviceState.mSessionTokens) {
            SessionState sessionState = (SessionState) userState.mSessionStateMap.get(sessionToken);
            if (sessionState.mSession == null && (iAppServiceId == null || sessionState.mIAppServiceId.equals(iAppServiceId))) {
                sessionsToAbort.add(sessionState);
            }
        }
        for (SessionState sessionState2 : sessionsToAbort) {
            removeSessionStateLocked(sessionState2.mSessionToken, sessionState2.mUserId);
            sendSessionTokenToClientLocked(sessionState2.mClient, sessionState2.mIAppServiceId, null, null, sessionState2.mSeq);
        }
        updateServiceConnectionLocked(serviceState.mComponent, userId);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateServiceConnectionLocked(ComponentName component, int userId) {
        UserState userState = getOrCreateUserStateLocked(userId);
        ServiceState serviceState = (ServiceState) userState.mServiceStateMap.get(component);
        if (serviceState == null) {
            return;
        }
        boolean z = false;
        if (serviceState.mReconnecting) {
            if (!serviceState.mSessionTokens.isEmpty()) {
                return;
            }
            serviceState.mReconnecting = false;
        }
        boolean shouldBind = (serviceState.mSessionTokens.isEmpty() && serviceState.mPendingAppLinkInfo.isEmpty() && serviceState.mPendingAppLinkCommand.isEmpty()) ? true : true;
        if (serviceState.mService == null && shouldBind) {
            if (serviceState.mBound) {
                return;
            }
            Intent i = new Intent("android.media.tv.interactive.TvInteractiveAppService").setComponent(component);
            serviceState.mBound = this.mContext.bindServiceAsUser(i, serviceState.mConnection, 33554433, new UserHandle(userId));
        } else if (serviceState.mService != null && !shouldBind) {
            this.mContext.unbindService(serviceState.mConnection);
            userState.mServiceStateMap.remove(component);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static final class UserState {
        private final RemoteCallbackList<ITvInteractiveAppManagerCallback> mCallbacks;
        private final Map<IBinder, ClientState> mClientStateMap;
        private Map<String, TvInteractiveAppState> mIAppMap;
        private final Set<String> mPackageSet;
        private final Map<ComponentName, ServiceState> mServiceStateMap;
        private final Map<IBinder, SessionState> mSessionStateMap;
        private final int mUserId;

        private UserState(int userId) {
            this.mIAppMap = new HashMap();
            this.mClientStateMap = new HashMap();
            this.mServiceStateMap = new HashMap();
            this.mSessionStateMap = new HashMap();
            this.mPackageSet = new HashSet();
            this.mCallbacks = new RemoteCallbackList<>();
            this.mUserId = userId;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static final class TvInteractiveAppState {
        private ComponentName mComponentName;
        private int mIAppNumber;
        private String mIAppServiceId;
        private TvInteractiveAppServiceInfo mInfo;
        private int mUid;

        private TvInteractiveAppState() {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public final class SessionState implements IBinder.DeathRecipient {
        private final int mCallingPid;
        private final int mCallingUid;
        private final ITvInteractiveAppClient mClient;
        private final ComponentName mComponent;
        private final String mIAppServiceId;
        private final int mSeq;
        private ITvInteractiveAppSession mSession;
        private final IBinder mSessionToken;
        private final int mType;
        private final int mUserId;

        private SessionState(IBinder sessionToken, String iAppServiceId, int type, ComponentName componentName, ITvInteractiveAppClient client, int seq, int callingUid, int callingPid, int userId) {
            this.mSessionToken = sessionToken;
            this.mIAppServiceId = iAppServiceId;
            this.mComponent = componentName;
            this.mType = type;
            this.mClient = client;
            this.mSeq = seq;
            this.mCallingUid = callingUid;
            this.mCallingPid = callingPid;
            this.mUserId = userId;
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public final class ClientState implements IBinder.DeathRecipient {
        private IBinder mClientToken;
        private final List<IBinder> mSessionTokens = new ArrayList();
        private final int mUserId;

        ClientState(IBinder clientToken, int userId) {
            this.mClientToken = clientToken;
            this.mUserId = userId;
        }

        public boolean isEmpty() {
            return this.mSessionTokens.isEmpty();
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            synchronized (TvInteractiveAppManagerService.this.mLock) {
                UserState userState = TvInteractiveAppManagerService.this.getOrCreateUserStateLocked(this.mUserId);
                ClientState clientState = (ClientState) userState.mClientStateMap.get(this.mClientToken);
                if (clientState != null) {
                    while (clientState.mSessionTokens.size() > 0) {
                        IBinder sessionToken = clientState.mSessionTokens.get(0);
                        TvInteractiveAppManagerService.this.releaseSessionLocked(sessionToken, 1000, this.mUserId);
                        if (clientState.mSessionTokens.contains(sessionToken)) {
                            Slogf.d(TvInteractiveAppManagerService.TAG, "remove sessionToken " + sessionToken + " for " + this.mClientToken);
                            clientState.mSessionTokens.remove(sessionToken);
                        }
                    }
                }
                this.mClientToken = null;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public final class ServiceState {
        private boolean mBound;
        private ServiceCallback mCallback;
        private final ComponentName mComponent;
        private final ServiceConnection mConnection;
        private final String mIAppServiceId;
        private final List<Bundle> mPendingAppLinkCommand;
        private final List<Pair<AppLinkInfo, Boolean>> mPendingAppLinkInfo;
        private boolean mReconnecting;
        private ITvInteractiveAppService mService;
        private final List<IBinder> mSessionTokens;

        private ServiceState(ComponentName component, String tias, int userId) {
            this.mSessionTokens = new ArrayList();
            this.mPendingAppLinkInfo = new ArrayList();
            this.mPendingAppLinkCommand = new ArrayList();
            this.mComponent = component;
            this.mConnection = new InteractiveAppServiceConnection(component, userId);
            this.mIAppServiceId = tias;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void addPendingAppLink(AppLinkInfo info, boolean register) {
            this.mPendingAppLinkInfo.add(Pair.create(info, Boolean.valueOf(register)));
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void addPendingAppLinkCommand(Bundle command) {
            this.mPendingAppLinkCommand.add(command);
        }
    }

    /* loaded from: classes2.dex */
    private final class InteractiveAppServiceConnection implements ServiceConnection {
        private final ComponentName mComponent;
        private final int mUserId;

        private InteractiveAppServiceConnection(ComponentName component, int userId) {
            this.mComponent = component;
            this.mUserId = userId;
        }

        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName component, IBinder service) {
            synchronized (TvInteractiveAppManagerService.this.mLock) {
                UserState userState = TvInteractiveAppManagerService.this.getUserStateLocked(this.mUserId);
                if (userState == null) {
                    TvInteractiveAppManagerService.this.mContext.unbindService(this);
                    return;
                }
                ServiceState serviceState = (ServiceState) userState.mServiceStateMap.get(this.mComponent);
                serviceState.mService = ITvInteractiveAppService.Stub.asInterface(service);
                if (serviceState.mCallback == null) {
                    serviceState.mCallback = new ServiceCallback(this.mComponent, this.mUserId);
                    try {
                        serviceState.mService.registerCallback(serviceState.mCallback);
                    } catch (RemoteException e) {
                        Slog.e(TvInteractiveAppManagerService.TAG, "error in registerCallback", e);
                    }
                }
                if (!serviceState.mPendingAppLinkInfo.isEmpty()) {
                    Iterator<Pair<AppLinkInfo, Boolean>> it = serviceState.mPendingAppLinkInfo.iterator();
                    while (it.hasNext()) {
                        Pair<AppLinkInfo, Boolean> appLinkInfoPair = it.next();
                        long identity = Binder.clearCallingIdentity();
                        try {
                            if (((Boolean) appLinkInfoPair.second).booleanValue()) {
                                serviceState.mService.registerAppLinkInfo((AppLinkInfo) appLinkInfoPair.first);
                            } else {
                                serviceState.mService.unregisterAppLinkInfo((AppLinkInfo) appLinkInfoPair.first);
                            }
                            it.remove();
                        } catch (RemoteException e2) {
                            Slogf.e(TvInteractiveAppManagerService.TAG, "error in notifyAppLinkInfo(" + appLinkInfoPair + ") when onServiceConnected", e2);
                        }
                        Binder.restoreCallingIdentity(identity);
                    }
                }
                if (!serviceState.mPendingAppLinkCommand.isEmpty()) {
                    Iterator<Bundle> it2 = serviceState.mPendingAppLinkCommand.iterator();
                    while (it2.hasNext()) {
                        Bundle command = it2.next();
                        long identity2 = Binder.clearCallingIdentity();
                        try {
                            serviceState.mService.sendAppLinkCommand(command);
                            it2.remove();
                        } catch (RemoteException e3) {
                            Slogf.e(TvInteractiveAppManagerService.TAG, "error in sendAppLinkCommand(" + command + ") when onServiceConnected", e3);
                        }
                        Binder.restoreCallingIdentity(identity2);
                    }
                }
                List<IBinder> tokensToBeRemoved = new ArrayList<>();
                for (IBinder sessionToken : serviceState.mSessionTokens) {
                    if (!TvInteractiveAppManagerService.this.createSessionInternalLocked(serviceState.mService, sessionToken, this.mUserId)) {
                        tokensToBeRemoved.add(sessionToken);
                    }
                }
                for (IBinder sessionToken2 : tokensToBeRemoved) {
                    TvInteractiveAppManagerService.this.removeSessionStateLocked(sessionToken2, this.mUserId);
                }
            }
        }

        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName component) {
            if (!this.mComponent.equals(component)) {
                throw new IllegalArgumentException("Mismatched ComponentName: " + this.mComponent + " (expected), " + component + " (actual).");
            }
            synchronized (TvInteractiveAppManagerService.this.mLock) {
                UserState userState = TvInteractiveAppManagerService.this.getOrCreateUserStateLocked(this.mUserId);
                ServiceState serviceState = (ServiceState) userState.mServiceStateMap.get(this.mComponent);
                if (serviceState != null) {
                    serviceState.mReconnecting = true;
                    serviceState.mBound = false;
                    serviceState.mService = null;
                    serviceState.mCallback = null;
                    TvInteractiveAppManagerService.this.abortPendingCreateSessionRequestsLocked(serviceState, null, this.mUserId);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public final class ServiceCallback extends ITvInteractiveAppServiceCallback.Stub {
        private final ComponentName mComponent;
        private final int mUserId;

        ServiceCallback(ComponentName component, int userId) {
            this.mComponent = component;
            this.mUserId = userId;
        }

        public void onStateChanged(int type, int state, int error) {
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInteractiveAppManagerService.this.mLock) {
                    ServiceState serviceState = TvInteractiveAppManagerService.this.getServiceStateLocked(this.mComponent, this.mUserId);
                    String iAppServiceId = serviceState.mIAppServiceId;
                    UserState userState = TvInteractiveAppManagerService.this.getUserStateLocked(this.mUserId);
                    TvInteractiveAppManagerService.this.notifyStateChangedLocked(userState, iAppServiceId, type, state, error);
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public final class SessionCallback extends ITvInteractiveAppSessionCallback.Stub {
        private final InputChannel[] mInputChannels;
        private final SessionState mSessionState;

        SessionCallback(SessionState sessionState, InputChannel[] channels) {
            this.mSessionState = sessionState;
            this.mInputChannels = channels;
        }

        public void onSessionCreated(ITvInteractiveAppSession session) {
            synchronized (TvInteractiveAppManagerService.this.mLock) {
                this.mSessionState.mSession = session;
                if (session != null && addSessionTokenToClientStateLocked(session)) {
                    TvInteractiveAppManagerService.this.sendSessionTokenToClientLocked(this.mSessionState.mClient, this.mSessionState.mIAppServiceId, this.mSessionState.mSessionToken, this.mInputChannels[0], this.mSessionState.mSeq);
                } else {
                    TvInteractiveAppManagerService.this.removeSessionStateLocked(this.mSessionState.mSessionToken, this.mSessionState.mUserId);
                    TvInteractiveAppManagerService.this.sendSessionTokenToClientLocked(this.mSessionState.mClient, this.mSessionState.mIAppServiceId, null, null, this.mSessionState.mSeq);
                }
                this.mInputChannels[0].dispose();
            }
        }

        public void onLayoutSurface(int left, int top, int right, int bottom) {
            synchronized (TvInteractiveAppManagerService.this.mLock) {
                if (this.mSessionState.mSession == null || this.mSessionState.mClient == null) {
                    return;
                }
                try {
                    this.mSessionState.mClient.onLayoutSurface(left, top, right, bottom, this.mSessionState.mSeq);
                } catch (RemoteException e) {
                    Slogf.e(TvInteractiveAppManagerService.TAG, "error in onLayoutSurface", e);
                }
            }
        }

        public void onBroadcastInfoRequest(BroadcastInfoRequest request) {
            synchronized (TvInteractiveAppManagerService.this.mLock) {
                if (this.mSessionState.mSession == null || this.mSessionState.mClient == null) {
                    return;
                }
                try {
                    this.mSessionState.mClient.onBroadcastInfoRequest(request, this.mSessionState.mSeq);
                } catch (RemoteException e) {
                    Slogf.e(TvInteractiveAppManagerService.TAG, "error in onBroadcastInfoRequest", e);
                }
            }
        }

        public void onRemoveBroadcastInfo(int requestId) {
            synchronized (TvInteractiveAppManagerService.this.mLock) {
                if (this.mSessionState.mSession == null || this.mSessionState.mClient == null) {
                    return;
                }
                try {
                    this.mSessionState.mClient.onRemoveBroadcastInfo(requestId, this.mSessionState.mSeq);
                } catch (RemoteException e) {
                    Slogf.e(TvInteractiveAppManagerService.TAG, "error in onRemoveBroadcastInfo", e);
                }
            }
        }

        public void onCommandRequest(String cmdType, Bundle parameters) {
            synchronized (TvInteractiveAppManagerService.this.mLock) {
                if (this.mSessionState.mSession == null || this.mSessionState.mClient == null) {
                    return;
                }
                try {
                    this.mSessionState.mClient.onCommandRequest(cmdType, parameters, this.mSessionState.mSeq);
                } catch (RemoteException e) {
                    Slogf.e(TvInteractiveAppManagerService.TAG, "error in onCommandRequest", e);
                }
            }
        }

        public void onSetVideoBounds(Rect rect) {
            synchronized (TvInteractiveAppManagerService.this.mLock) {
                if (this.mSessionState.mSession == null || this.mSessionState.mClient == null) {
                    return;
                }
                try {
                    this.mSessionState.mClient.onSetVideoBounds(rect, this.mSessionState.mSeq);
                } catch (RemoteException e) {
                    Slogf.e(TvInteractiveAppManagerService.TAG, "error in onSetVideoBounds", e);
                }
            }
        }

        public void onRequestCurrentChannelUri() {
            synchronized (TvInteractiveAppManagerService.this.mLock) {
                if (this.mSessionState.mSession == null || this.mSessionState.mClient == null) {
                    return;
                }
                try {
                    this.mSessionState.mClient.onRequestCurrentChannelUri(this.mSessionState.mSeq);
                } catch (RemoteException e) {
                    Slogf.e(TvInteractiveAppManagerService.TAG, "error in onRequestCurrentChannelUri", e);
                }
            }
        }

        public void onRequestCurrentChannelLcn() {
            synchronized (TvInteractiveAppManagerService.this.mLock) {
                if (this.mSessionState.mSession == null || this.mSessionState.mClient == null) {
                    return;
                }
                try {
                    this.mSessionState.mClient.onRequestCurrentChannelLcn(this.mSessionState.mSeq);
                } catch (RemoteException e) {
                    Slogf.e(TvInteractiveAppManagerService.TAG, "error in onRequestCurrentChannelLcn", e);
                }
            }
        }

        public void onRequestStreamVolume() {
            synchronized (TvInteractiveAppManagerService.this.mLock) {
                if (this.mSessionState.mSession == null || this.mSessionState.mClient == null) {
                    return;
                }
                try {
                    this.mSessionState.mClient.onRequestStreamVolume(this.mSessionState.mSeq);
                } catch (RemoteException e) {
                    Slogf.e(TvInteractiveAppManagerService.TAG, "error in onRequestStreamVolume", e);
                }
            }
        }

        public void onRequestTrackInfoList() {
            synchronized (TvInteractiveAppManagerService.this.mLock) {
                if (this.mSessionState.mSession == null || this.mSessionState.mClient == null) {
                    return;
                }
                try {
                    this.mSessionState.mClient.onRequestTrackInfoList(this.mSessionState.mSeq);
                } catch (RemoteException e) {
                    Slogf.e(TvInteractiveAppManagerService.TAG, "error in onRequestTrackInfoList", e);
                }
            }
        }

        public void onRequestCurrentTvInputId() {
            synchronized (TvInteractiveAppManagerService.this.mLock) {
                if (this.mSessionState.mSession == null || this.mSessionState.mClient == null) {
                    return;
                }
                try {
                    this.mSessionState.mClient.onRequestCurrentTvInputId(this.mSessionState.mSeq);
                } catch (RemoteException e) {
                    Slogf.e(TvInteractiveAppManagerService.TAG, "error in onRequestCurrentTvInputId", e);
                }
            }
        }

        public void onRequestSigning(String id, String algorithm, String alias, byte[] data) {
            synchronized (TvInteractiveAppManagerService.this.mLock) {
                if (this.mSessionState.mSession == null || this.mSessionState.mClient == null) {
                    return;
                }
                try {
                    this.mSessionState.mClient.onRequestSigning(id, algorithm, alias, data, this.mSessionState.mSeq);
                } catch (RemoteException e) {
                    Slogf.e(TvInteractiveAppManagerService.TAG, "error in onRequestSigning", e);
                }
            }
        }

        public void onAdRequest(AdRequest request) {
            synchronized (TvInteractiveAppManagerService.this.mLock) {
                if (this.mSessionState.mSession == null || this.mSessionState.mClient == null) {
                    return;
                }
                try {
                    this.mSessionState.mClient.onAdRequest(request, this.mSessionState.mSeq);
                } catch (RemoteException e) {
                    Slogf.e(TvInteractiveAppManagerService.TAG, "error in onAdRequest", e);
                }
            }
        }

        public void onSessionStateChanged(int state, int err) {
            synchronized (TvInteractiveAppManagerService.this.mLock) {
                if (this.mSessionState.mSession == null || this.mSessionState.mClient == null) {
                    return;
                }
                try {
                    this.mSessionState.mClient.onSessionStateChanged(state, err, this.mSessionState.mSeq);
                } catch (RemoteException e) {
                    Slogf.e(TvInteractiveAppManagerService.TAG, "error in onSessionStateChanged", e);
                }
            }
        }

        public void onBiInteractiveAppCreated(Uri biIAppUri, String biIAppId) {
            synchronized (TvInteractiveAppManagerService.this.mLock) {
                if (this.mSessionState.mSession == null || this.mSessionState.mClient == null) {
                    return;
                }
                try {
                    this.mSessionState.mClient.onBiInteractiveAppCreated(biIAppUri, biIAppId, this.mSessionState.mSeq);
                } catch (RemoteException e) {
                    Slogf.e(TvInteractiveAppManagerService.TAG, "error in onBiInteractiveAppCreated", e);
                }
            }
        }

        public void onTeletextAppStateChanged(int state) {
            synchronized (TvInteractiveAppManagerService.this.mLock) {
                if (this.mSessionState.mSession == null || this.mSessionState.mClient == null) {
                    return;
                }
                try {
                    this.mSessionState.mClient.onTeletextAppStateChanged(state, this.mSessionState.mSeq);
                } catch (RemoteException e) {
                    Slogf.e(TvInteractiveAppManagerService.TAG, "error in onTeletextAppStateChanged", e);
                }
            }
        }

        private boolean addSessionTokenToClientStateLocked(ITvInteractiveAppSession session) {
            try {
                session.asBinder().linkToDeath(this.mSessionState, 0);
                IBinder clientToken = this.mSessionState.mClient.asBinder();
                UserState userState = TvInteractiveAppManagerService.this.getOrCreateUserStateLocked(this.mSessionState.mUserId);
                ClientState clientState = (ClientState) userState.mClientStateMap.get(clientToken);
                if (clientState == null) {
                    clientState = new ClientState(clientToken, this.mSessionState.mUserId);
                    try {
                        clientToken.linkToDeath(clientState, 0);
                        userState.mClientStateMap.put(clientToken, clientState);
                    } catch (RemoteException e) {
                        Slogf.e(TvInteractiveAppManagerService.TAG, "client process has already died", e);
                        return false;
                    }
                }
                clientState.mSessionTokens.add(this.mSessionState.mSessionToken);
                return true;
            } catch (RemoteException e2) {
                Slogf.e(TvInteractiveAppManagerService.TAG, "session process has already died", e2);
                return false;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class SessionNotFoundException extends IllegalArgumentException {
        SessionNotFoundException(String name) {
            super(name);
        }
    }

    /* loaded from: classes2.dex */
    private static class ClientPidNotFoundException extends IllegalArgumentException {
        ClientPidNotFoundException(String name) {
            super(name);
        }
    }
}
