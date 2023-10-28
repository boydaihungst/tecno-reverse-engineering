package com.android.server.people;

import android.app.ActivityManager;
import android.app.people.ConversationChannel;
import android.app.people.ConversationStatus;
import android.app.people.IConversationListener;
import android.app.people.IPeopleManager;
import android.app.prediction.AppPredictionContext;
import android.app.prediction.AppPredictionSessionId;
import android.app.prediction.AppTarget;
import android.app.prediction.AppTargetEvent;
import android.app.prediction.IPredictionCallback;
import android.content.Context;
import android.content.pm.PackageManagerInternal;
import android.content.pm.ParceledListSlice;
import android.content.pm.ShortcutInfo;
import android.os.Binder;
import android.os.CancellationSignal;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.Slog;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.people.PeopleService;
import com.android.server.people.data.DataManager;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
/* loaded from: classes2.dex */
public class PeopleService extends SystemService {
    private static final String TAG = "PeopleService";
    ConversationListenerHelper mConversationListenerHelper;
    private DataManager mDataManager;
    private PackageManagerInternal mPackageManagerInternal;
    final IBinder mService;

    /* renamed from: -$$Nest$smisSystemOrRoot  reason: not valid java name */
    static /* bridge */ /* synthetic */ boolean m5287$$Nest$smisSystemOrRoot() {
        return isSystemOrRoot();
    }

    public PeopleService(Context context) {
        super(context);
        this.mService = new IPeopleManager.Stub() { // from class: com.android.server.people.PeopleService.1
            public ConversationChannel getConversation(String packageName, int userId, String shortcutId) {
                PeopleService peopleService = PeopleService.this;
                peopleService.enforceSystemRootOrSystemUI(peopleService.getContext(), "get conversation");
                return PeopleService.this.mDataManager.getConversation(packageName, userId, shortcutId);
            }

            public ParceledListSlice<ConversationChannel> getRecentConversations() {
                PeopleService peopleService = PeopleService.this;
                peopleService.enforceSystemRootOrSystemUI(peopleService.getContext(), "get recent conversations");
                return new ParceledListSlice<>(PeopleService.this.mDataManager.getRecentConversations(Binder.getCallingUserHandle().getIdentifier()));
            }

            public void removeRecentConversation(String packageName, int userId, String shortcutId) {
                PeopleService.enforceSystemOrRoot("remove a recent conversation");
                PeopleService.this.mDataManager.removeRecentConversation(packageName, userId, shortcutId, Binder.getCallingUserHandle().getIdentifier());
            }

            public void removeAllRecentConversations() {
                PeopleService.enforceSystemOrRoot("remove all recent conversations");
                PeopleService.this.mDataManager.removeAllRecentConversations(Binder.getCallingUserHandle().getIdentifier());
            }

            public boolean isConversation(String packageName, int userId, String shortcutId) {
                enforceHasReadPeopleDataPermission();
                PeopleService.this.handleIncomingUser(userId);
                return PeopleService.this.mDataManager.isConversation(packageName, userId, shortcutId);
            }

            private void enforceHasReadPeopleDataPermission() throws SecurityException {
                if (PeopleService.this.getContext().checkCallingPermission("android.permission.READ_PEOPLE_DATA") != 0) {
                    throw new SecurityException("Caller doesn't have READ_PEOPLE_DATA permission.");
                }
            }

            public long getLastInteraction(String packageName, int userId, String shortcutId) {
                PeopleService peopleService = PeopleService.this;
                peopleService.enforceSystemRootOrSystemUI(peopleService.getContext(), "get last interaction");
                return PeopleService.this.mDataManager.getLastInteraction(packageName, userId, shortcutId);
            }

            public void addOrUpdateStatus(String packageName, int userId, String conversationId, ConversationStatus status) {
                PeopleService.this.handleIncomingUser(userId);
                PeopleService.this.checkCallerIsSameApp(packageName);
                if (status.getStartTimeMillis() > System.currentTimeMillis()) {
                    throw new IllegalArgumentException("Start time must be in the past");
                }
                PeopleService.this.mDataManager.addOrUpdateStatus(packageName, userId, conversationId, status);
            }

            public void clearStatus(String packageName, int userId, String conversationId, String statusId) {
                PeopleService.this.handleIncomingUser(userId);
                PeopleService.this.checkCallerIsSameApp(packageName);
                PeopleService.this.mDataManager.clearStatus(packageName, userId, conversationId, statusId);
            }

            public void clearStatuses(String packageName, int userId, String conversationId) {
                PeopleService.this.handleIncomingUser(userId);
                PeopleService.this.checkCallerIsSameApp(packageName);
                PeopleService.this.mDataManager.clearStatuses(packageName, userId, conversationId);
            }

            public ParceledListSlice<ConversationStatus> getStatuses(String packageName, int userId, String conversationId) {
                PeopleService.this.handleIncomingUser(userId);
                if (!PeopleService.m5287$$Nest$smisSystemOrRoot()) {
                    PeopleService.this.checkCallerIsSameApp(packageName);
                }
                return new ParceledListSlice<>(PeopleService.this.mDataManager.getStatuses(packageName, userId, conversationId));
            }

            public void registerConversationListener(String packageName, int userId, String shortcutId, IConversationListener listener) {
                PeopleService peopleService = PeopleService.this;
                peopleService.enforceSystemRootOrSystemUI(peopleService.getContext(), "register conversation listener");
                PeopleService.this.mConversationListenerHelper.addConversationListener(new ListenerKey(packageName, Integer.valueOf(userId), shortcutId), listener);
            }

            public void unregisterConversationListener(IConversationListener listener) {
                PeopleService peopleService = PeopleService.this;
                peopleService.enforceSystemRootOrSystemUI(peopleService.getContext(), "unregister conversation listener");
                PeopleService.this.mConversationListenerHelper.removeConversationListener(listener);
            }
        };
        this.mDataManager = new DataManager(context);
        ConversationListenerHelper conversationListenerHelper = new ConversationListenerHelper();
        this.mConversationListenerHelper = conversationListenerHelper;
        this.mDataManager.addConversationsListener(conversationListenerHelper);
    }

    @Override // com.android.server.SystemService
    public void onBootPhase(int phase) {
        if (phase == 500) {
            this.mDataManager.initialize();
        }
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        onStart(false);
    }

    protected void onStart(boolean isForTesting) {
        if (!isForTesting) {
            publishBinderService("people", this.mService);
        }
        publishLocalService(PeopleServiceInternal.class, new LocalService());
        this.mPackageManagerInternal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
    }

    @Override // com.android.server.SystemService
    public void onUserUnlocked(SystemService.TargetUser user) {
        this.mDataManager.onUserUnlocked(user.getUserIdentifier());
    }

    @Override // com.android.server.SystemService
    public void onUserStopping(SystemService.TargetUser user) {
        this.mDataManager.onUserStopping(user.getUserIdentifier());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void enforceSystemOrRoot(String message) {
        if (!isSystemOrRoot()) {
            throw new SecurityException("Only system may " + message);
        }
    }

    private static boolean isSystemOrRoot() {
        int uid = Binder.getCallingUid();
        return UserHandle.isSameApp(uid, 1000) || uid == 0;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int handleIncomingUser(int userId) {
        try {
            return ActivityManager.getService().handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, true, true, "", (String) null);
        } catch (RemoteException e) {
            return userId;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void checkCallerIsSameApp(String pkg) {
        int callingUid = Binder.getCallingUid();
        int callingUserId = UserHandle.getUserId(callingUid);
        if (this.mPackageManagerInternal.getPackageUid(pkg, 0L, callingUserId) != callingUid) {
            throw new SecurityException("Calling uid " + callingUid + " cannot query eventsfor package " + pkg);
        }
    }

    protected void enforceSystemRootOrSystemUI(Context context, String message) {
        if (isSystemOrRoot()) {
            return;
        }
        context.enforceCallingPermission("android.permission.STATUS_BAR_SERVICE", message);
    }

    /* loaded from: classes2.dex */
    public interface ConversationsListener {
        default void onConversationsUpdate(List<ConversationChannel> conversations) {
        }
    }

    /* loaded from: classes2.dex */
    public static class ConversationListenerHelper implements ConversationsListener {
        final RemoteCallbackList<IConversationListener> mListeners = new RemoteCallbackList<>();

        ConversationListenerHelper() {
        }

        public synchronized void addConversationListener(ListenerKey key, IConversationListener listener) {
            this.mListeners.unregister(listener);
            this.mListeners.register(listener, key);
        }

        public synchronized void removeConversationListener(IConversationListener listener) {
            this.mListeners.unregister(listener);
        }

        @Override // com.android.server.people.PeopleService.ConversationsListener
        public void onConversationsUpdate(List<ConversationChannel> conversations) {
            int count = this.mListeners.beginBroadcast();
            if (count == 0) {
                return;
            }
            Map<ListenerKey, ConversationChannel> keyedConversations = new HashMap<>();
            for (ConversationChannel conversation : conversations) {
                keyedConversations.put(getListenerKey(conversation), conversation);
            }
            for (int i = 0; i < count; i++) {
                ListenerKey listenerKey = (ListenerKey) this.mListeners.getBroadcastCookie(i);
                if (keyedConversations.containsKey(listenerKey)) {
                    IConversationListener listener = this.mListeners.getBroadcastItem(i);
                    try {
                        ConversationChannel channel = keyedConversations.get(listenerKey);
                        listener.onConversationUpdate(channel);
                    } catch (RemoteException e) {
                    }
                }
            }
            this.mListeners.finishBroadcast();
        }

        private ListenerKey getListenerKey(ConversationChannel conversation) {
            ShortcutInfo info = conversation.getShortcutInfo();
            return new ListenerKey(info.getPackage(), Integer.valueOf(info.getUserId()), info.getId());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class ListenerKey {
        private final String mPackageName;
        private final String mShortcutId;
        private final Integer mUserId;

        ListenerKey(String packageName, Integer userId, String shortcutId) {
            this.mPackageName = packageName;
            this.mUserId = userId;
            this.mShortcutId = shortcutId;
        }

        public String getPackageName() {
            return this.mPackageName;
        }

        public Integer getUserId() {
            return this.mUserId;
        }

        public String getShortcutId() {
            return this.mShortcutId;
        }

        public boolean equals(Object o) {
            ListenerKey key = (ListenerKey) o;
            return key.getPackageName().equals(this.mPackageName) && key.getUserId() == this.mUserId && key.getShortcutId().equals(this.mShortcutId);
        }

        public int hashCode() {
            return this.mPackageName.hashCode() + this.mUserId.hashCode() + this.mShortcutId.hashCode();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public final class LocalService extends PeopleServiceInternal {
        private Map<AppPredictionSessionId, SessionInfo> mSessions = new ArrayMap();

        LocalService() {
        }

        public void onCreatePredictionSession(AppPredictionContext appPredictionContext, AppPredictionSessionId sessionId) {
            this.mSessions.put(sessionId, new SessionInfo(appPredictionContext, PeopleService.this.mDataManager, sessionId.getUserId(), PeopleService.this.getContext()));
        }

        public void notifyAppTargetEvent(AppPredictionSessionId sessionId, final AppTargetEvent event) {
            runForSession(sessionId, new Consumer() { // from class: com.android.server.people.PeopleService$LocalService$$ExternalSyntheticLambda7
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ((SessionInfo) obj).getPredictor().onAppTargetEvent(event);
                }
            });
        }

        public void notifyLaunchLocationShown(AppPredictionSessionId sessionId, final String launchLocation, final ParceledListSlice targetIds) {
            runForSession(sessionId, new Consumer() { // from class: com.android.server.people.PeopleService$LocalService$$ExternalSyntheticLambda5
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ((SessionInfo) obj).getPredictor().onLaunchLocationShown(launchLocation, targetIds.getList());
                }
            });
        }

        public void sortAppTargets(AppPredictionSessionId sessionId, final ParceledListSlice targets, final IPredictionCallback callback) {
            runForSession(sessionId, new Consumer() { // from class: com.android.server.people.PeopleService$LocalService$$ExternalSyntheticLambda4
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    PeopleService.LocalService.this.m5290x38eb3d3f(targets, callback, (SessionInfo) obj);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$sortAppTargets$3$com-android-server-people-PeopleService$LocalService  reason: not valid java name */
        public /* synthetic */ void m5290x38eb3d3f(ParceledListSlice targets, final IPredictionCallback callback, SessionInfo sessionInfo) {
            sessionInfo.getPredictor().onSortAppTargets(targets.getList(), new Consumer() { // from class: com.android.server.people.PeopleService$LocalService$$ExternalSyntheticLambda3
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    PeopleService.LocalService.this.m5289x1ecfbea0(callback, (List) obj);
                }
            });
        }

        public void registerPredictionUpdates(AppPredictionSessionId sessionId, final IPredictionCallback callback) {
            runForSession(sessionId, new Consumer() { // from class: com.android.server.people.PeopleService$LocalService$$ExternalSyntheticLambda0
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ((SessionInfo) obj).addCallback(callback);
                }
            });
        }

        public void unregisterPredictionUpdates(AppPredictionSessionId sessionId, final IPredictionCallback callback) {
            runForSession(sessionId, new Consumer() { // from class: com.android.server.people.PeopleService$LocalService$$ExternalSyntheticLambda1
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ((SessionInfo) obj).removeCallback(callback);
                }
            });
        }

        public void requestPredictionUpdate(AppPredictionSessionId sessionId) {
            runForSession(sessionId, new Consumer() { // from class: com.android.server.people.PeopleService$LocalService$$ExternalSyntheticLambda2
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ((SessionInfo) obj).getPredictor().onRequestPredictionUpdate();
                }
            });
        }

        public void onDestroyPredictionSession(final AppPredictionSessionId sessionId) {
            runForSession(sessionId, new Consumer() { // from class: com.android.server.people.PeopleService$LocalService$$ExternalSyntheticLambda6
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    PeopleService.LocalService.this.m5288xa0f9994e(sessionId, (SessionInfo) obj);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onDestroyPredictionSession$7$com-android-server-people-PeopleService$LocalService  reason: not valid java name */
        public /* synthetic */ void m5288xa0f9994e(AppPredictionSessionId sessionId, SessionInfo sessionInfo) {
            sessionInfo.onDestroy();
            this.mSessions.remove(sessionId);
        }

        @Override // com.android.server.people.PeopleServiceInternal
        public void pruneDataForUser(int userId, CancellationSignal signal) {
            PeopleService.this.mDataManager.pruneDataForUser(userId, signal);
        }

        @Override // com.android.server.people.PeopleServiceInternal
        public byte[] getBackupPayload(int userId) {
            return PeopleService.this.mDataManager.getBackupPayload(userId);
        }

        @Override // com.android.server.people.PeopleServiceInternal
        public void restore(int userId, byte[] payload) {
            PeopleService.this.mDataManager.restore(userId, payload);
        }

        SessionInfo getSessionInfo(AppPredictionSessionId sessionId) {
            return this.mSessions.get(sessionId);
        }

        private void runForSession(AppPredictionSessionId sessionId, Consumer<SessionInfo> method) {
            SessionInfo sessionInfo = this.mSessions.get(sessionId);
            if (sessionInfo == null) {
                Slog.e(PeopleService.TAG, "Failed to find the session: " + sessionId);
            } else {
                method.accept(sessionInfo);
            }
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX INFO: Access modifiers changed from: private */
        /* renamed from: invokePredictionCallback */
        public void m5289x1ecfbea0(IPredictionCallback callback, List<AppTarget> targets) {
            try {
                callback.onResult(new ParceledListSlice(targets));
            } catch (RemoteException e) {
                Slog.e(PeopleService.TAG, "Failed to calling callback" + e);
            }
        }
    }
}
