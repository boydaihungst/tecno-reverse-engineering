package com.android.server.voiceinteraction;

import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.app.ActivityThread;
import android.app.AppGlobals;
import android.app.role.OnRoleHoldersChangedListener;
import android.app.role.RoleManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.ShortcutServiceInternal;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.hardware.soundtrigger.IRecognitionStatusCallback;
import android.hardware.soundtrigger.KeyphraseMetadata;
import android.hardware.soundtrigger.SoundTrigger;
import android.media.AudioFormat;
import android.media.permission.Identity;
import android.media.permission.IdentityContext;
import android.media.permission.PermissionUtil;
import android.media.permission.SafeCloseable;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.PersistableBundle;
import android.os.Process;
import android.os.RemoteCallback;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.SharedMemory;
import android.os.ShellCallback;
import android.os.UserHandle;
import android.provider.Settings;
import android.service.voice.IMicrophoneHotwordDetectionVoiceInteractionCallback;
import android.service.voice.IVoiceInteractionSession;
import android.service.voice.VoiceInteractionManagerInternal;
import android.service.voice.VoiceInteractionServiceInfo;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import android.util.Slog;
import com.android.internal.app.IHotwordRecognitionStatusCallback;
import com.android.internal.app.IVoiceActionCheckCallback;
import com.android.internal.app.IVoiceInteractionManagerService;
import com.android.internal.app.IVoiceInteractionSessionListener;
import com.android.internal.app.IVoiceInteractionSessionShowCallback;
import com.android.internal.app.IVoiceInteractionSoundTriggerSession;
import com.android.internal.app.IVoiceInteractor;
import com.android.internal.content.PackageMonitor;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.FunctionalUtils;
import com.android.server.FgThread;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.UiThread;
import com.android.server.pm.UserManagerInternal;
import com.android.server.pm.permission.LegacyPermissionManagerInternal;
import com.android.server.slice.SliceClientPermissions;
import com.android.server.soundtrigger.SoundTriggerInternal;
import com.android.server.utils.TimingsTraceAndSlog;
import com.android.server.voiceinteraction.VoiceInteractionManagerService;
import com.android.server.wm.ActivityTaskManagerInternal;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
/* loaded from: classes2.dex */
public class VoiceInteractionManagerService extends SystemService {
    static final boolean DEBUG = false;
    static final String TAG = "VoiceInteractionManager";
    final ActivityManagerInternal mAmInternal;
    final ActivityTaskManagerInternal mAtmInternal;
    final Context mContext;
    final DatabaseHelper mDbHelper;
    final ArrayMap<Integer, VoiceInteractionManagerServiceStub.SoundTriggerSession> mLoadedKeyphraseIds;
    final ContentResolver mResolver;
    private final VoiceInteractionManagerServiceStub mServiceStub;
    ShortcutServiceInternal mShortcutServiceInternal;
    SoundTriggerInternal mSoundTriggerInternal;
    final UserManagerInternal mUserManagerInternal;
    private final RemoteCallbackList<IVoiceInteractionSessionListener> mVoiceInteractionSessionListeners;

    public VoiceInteractionManagerService(Context context) {
        super(context);
        this.mLoadedKeyphraseIds = new ArrayMap<>();
        this.mVoiceInteractionSessionListeners = new RemoteCallbackList<>();
        this.mContext = context;
        this.mResolver = context.getContentResolver();
        this.mDbHelper = new DatabaseHelper(context);
        this.mServiceStub = new VoiceInteractionManagerServiceStub();
        this.mAmInternal = (ActivityManagerInternal) Objects.requireNonNull((ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class));
        this.mAtmInternal = (ActivityTaskManagerInternal) Objects.requireNonNull((ActivityTaskManagerInternal) LocalServices.getService(ActivityTaskManagerInternal.class));
        this.mUserManagerInternal = (UserManagerInternal) Objects.requireNonNull((UserManagerInternal) LocalServices.getService(UserManagerInternal.class));
        LegacyPermissionManagerInternal permissionManagerInternal = (LegacyPermissionManagerInternal) LocalServices.getService(LegacyPermissionManagerInternal.class);
        permissionManagerInternal.setVoiceInteractionPackagesProvider(new LegacyPermissionManagerInternal.PackagesProvider() { // from class: com.android.server.voiceinteraction.VoiceInteractionManagerService.1
            @Override // com.android.server.pm.permission.LegacyPermissionManagerInternal.PackagesProvider
            public String[] getPackages(int userId) {
                VoiceInteractionManagerService.this.mServiceStub.initForUser(userId);
                ComponentName interactor = VoiceInteractionManagerService.this.mServiceStub.getCurInteractor(userId);
                if (interactor != null) {
                    return new String[]{interactor.getPackageName()};
                }
                return null;
            }
        });
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        publishBinderService("voiceinteraction", this.mServiceStub);
        publishLocalService(VoiceInteractionManagerInternal.class, new LocalService());
        this.mAmInternal.setVoiceInteractionManagerProvider(new ActivityManagerInternal.VoiceInteractionManagerProvider() { // from class: com.android.server.voiceinteraction.VoiceInteractionManagerService.2
            public void notifyActivityEventChanged() {
                VoiceInteractionManagerService.this.mServiceStub.notifyActivityEventChanged();
            }
        });
    }

    @Override // com.android.server.SystemService
    public void onBootPhase(int phase) {
        if (500 == phase) {
            this.mShortcutServiceInternal = (ShortcutServiceInternal) Objects.requireNonNull((ShortcutServiceInternal) LocalServices.getService(ShortcutServiceInternal.class));
            this.mSoundTriggerInternal = (SoundTriggerInternal) LocalServices.getService(SoundTriggerInternal.class);
        } else if (phase == 600) {
            this.mServiceStub.systemRunning(isSafeMode());
        }
    }

    @Override // com.android.server.SystemService
    public boolean isUserSupported(SystemService.TargetUser user) {
        return user.isFull();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isUserSupported(UserInfo user) {
        return user.isFull();
    }

    @Override // com.android.server.SystemService
    public void onUserStarting(SystemService.TargetUser user) {
        this.mServiceStub.initForUser(user.getUserIdentifier());
    }

    @Override // com.android.server.SystemService
    public void onUserUnlocking(SystemService.TargetUser user) {
        this.mServiceStub.initForUser(user.getUserIdentifier());
        this.mServiceStub.switchImplementationIfNeeded(false);
    }

    @Override // com.android.server.SystemService
    public void onUserSwitching(SystemService.TargetUser from, SystemService.TargetUser to) {
        this.mServiceStub.switchUser(to.getUserIdentifier());
    }

    /* loaded from: classes2.dex */
    class LocalService extends VoiceInteractionManagerInternal {
        LocalService() {
        }

        public void startLocalVoiceInteraction(IBinder callingActivity, Bundle options) {
            VoiceInteractionManagerService.this.mServiceStub.startLocalVoiceInteraction(callingActivity, options);
        }

        public boolean supportsLocalVoiceInteraction() {
            return VoiceInteractionManagerService.this.mServiceStub.supportsLocalVoiceInteraction();
        }

        public void stopLocalVoiceInteraction(IBinder callingActivity) {
            VoiceInteractionManagerService.this.mServiceStub.stopLocalVoiceInteraction(callingActivity);
        }

        public boolean hasActiveSession(String packageName) {
            VoiceInteractionSessionConnection session;
            VoiceInteractionManagerServiceImpl impl = VoiceInteractionManagerService.this.mServiceStub.mImpl;
            if (impl == null || (session = impl.mActiveSession) == null) {
                return false;
            }
            return TextUtils.equals(packageName, session.mSessionComponentName.getPackageName());
        }

        public String getVoiceInteractorPackageName(IBinder callingVoiceInteractor) {
            VoiceInteractionSessionConnection session;
            IVoiceInteractor voiceInteractor;
            VoiceInteractionManagerServiceImpl impl = VoiceInteractionManagerService.this.mServiceStub.mImpl;
            if (impl == null || (session = impl.mActiveSession) == null || (voiceInteractor = session.mInteractor) == null || voiceInteractor.asBinder() != callingVoiceInteractor) {
                return null;
            }
            return session.mSessionComponentName.getPackageName();
        }

        public VoiceInteractionManagerInternal.HotwordDetectionServiceIdentity getHotwordDetectionServiceIdentity() {
            HotwordDetectionConnection hotwordDetectionConnection;
            VoiceInteractionManagerServiceImpl impl = VoiceInteractionManagerService.this.mServiceStub.mImpl;
            if (impl == null || (hotwordDetectionConnection = impl.mHotwordDetectionConnection) == null) {
                return null;
            }
            return hotwordDetectionConnection.mIdentity;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public class VoiceInteractionManagerServiceStub extends IVoiceInteractionManagerService.Stub {
        private int mCurUser;
        private boolean mCurUserSupported;
        private final boolean mEnableService;
        volatile VoiceInteractionManagerServiceImpl mImpl;
        PackageMonitor mPackageMonitor = new AnonymousClass2();
        private boolean mSafeMode;
        private boolean mTemporarilyDisabled;

        VoiceInteractionManagerServiceStub() {
            this.mEnableService = shouldEnableService(VoiceInteractionManagerService.this.mContext);
            new RoleObserver(VoiceInteractionManagerService.this.mContext.getMainExecutor());
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public void handleUserStop(String packageName, int userHandle) {
            synchronized (this) {
                ComponentName curInteractor = getCurInteractor(userHandle);
                if (curInteractor != null && packageName.equals(curInteractor.getPackageName())) {
                    Slog.d(VoiceInteractionManagerService.TAG, "switchImplementation for user stop.");
                    switchImplementationIfNeededLocked(true);
                }
            }
        }

        public IVoiceInteractionSoundTriggerSession createSoundTriggerSessionAsOriginator(Identity originatorIdentity, IBinder client) {
            boolean forHotwordDetectionService;
            IVoiceInteractionSoundTriggerSession session;
            Objects.requireNonNull(originatorIdentity);
            synchronized (this) {
                enforceIsCurrentVoiceInteractionService();
                forHotwordDetectionService = (this.mImpl == null || this.mImpl.mHotwordDetectionConnection == null) ? false : true;
            }
            if (forHotwordDetectionService) {
                originatorIdentity.uid = Binder.getCallingUid();
                originatorIdentity.pid = Binder.getCallingPid();
                session = new SoundTriggerSessionPermissionsDecorator(createSoundTriggerSessionForSelfIdentity(client), VoiceInteractionManagerService.this.mContext, originatorIdentity);
            } else {
                SafeCloseable ignored = PermissionUtil.establishIdentityDirect(originatorIdentity);
                try {
                    IVoiceInteractionSoundTriggerSession session2 = new SoundTriggerSession(VoiceInteractionManagerService.this.mSoundTriggerInternal.attach(client));
                    if (ignored != null) {
                        ignored.close();
                    }
                    session = session2;
                } catch (Throwable th) {
                    if (ignored != null) {
                        try {
                            ignored.close();
                        } catch (Throwable th2) {
                            th.addSuppressed(th2);
                        }
                    }
                    throw th;
                }
            }
            return new SoundTriggerSessionBinderProxy(session);
        }

        private IVoiceInteractionSoundTriggerSession createSoundTriggerSessionForSelfIdentity(final IBinder client) {
            final Identity identity = new Identity();
            identity.uid = Process.myUid();
            identity.pid = Process.myPid();
            identity.packageName = ActivityThread.currentOpPackageName();
            return (IVoiceInteractionSoundTriggerSession) Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.voiceinteraction.VoiceInteractionManagerService$VoiceInteractionManagerServiceStub$$ExternalSyntheticLambda3
                public final Object getOrThrow() {
                    return VoiceInteractionManagerService.VoiceInteractionManagerServiceStub.this.m7600xf791260c(identity, client);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$createSoundTriggerSessionForSelfIdentity$0$com-android-server-voiceinteraction-VoiceInteractionManagerService$VoiceInteractionManagerServiceStub  reason: not valid java name */
        public /* synthetic */ SoundTriggerSession m7600xf791260c(Identity identity, IBinder client) throws Exception {
            SafeCloseable ignored = IdentityContext.create(identity);
            try {
                SoundTriggerSession soundTriggerSession = new SoundTriggerSession(VoiceInteractionManagerService.this.mSoundTriggerInternal.attach(client));
                if (ignored != null) {
                    ignored.close();
                }
                return soundTriggerSession;
            } catch (Throwable th) {
                if (ignored != null) {
                    try {
                        ignored.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                }
                throw th;
            }
        }

        void startLocalVoiceInteraction(final IBinder token, Bundle options) {
            if (this.mImpl == null) {
                return;
            }
            final int callingUid = Binder.getCallingUid();
            long caller = Binder.clearCallingIdentity();
            try {
                this.mImpl.showSessionLocked(options, 16, new IVoiceInteractionSessionShowCallback.Stub() { // from class: com.android.server.voiceinteraction.VoiceInteractionManagerService.VoiceInteractionManagerServiceStub.1
                    public void onFailed() {
                    }

                    public void onShown() {
                        synchronized (VoiceInteractionManagerServiceStub.this) {
                            if (VoiceInteractionManagerServiceStub.this.mImpl != null) {
                                VoiceInteractionManagerServiceStub.this.mImpl.grantImplicitAccessLocked(callingUid, null);
                            }
                        }
                        VoiceInteractionManagerService.this.mAtmInternal.onLocalVoiceInteractionStarted(token, VoiceInteractionManagerServiceStub.this.mImpl.mActiveSession.mSession, VoiceInteractionManagerServiceStub.this.mImpl.mActiveSession.mInteractor);
                    }
                }, token);
            } finally {
                Binder.restoreCallingIdentity(caller);
            }
        }

        public void stopLocalVoiceInteraction(IBinder callingActivity) {
            if (this.mImpl == null) {
                return;
            }
            long caller = Binder.clearCallingIdentity();
            try {
                this.mImpl.finishLocked(callingActivity, true);
            } finally {
                Binder.restoreCallingIdentity(caller);
            }
        }

        public boolean supportsLocalVoiceInteraction() {
            if (this.mImpl == null) {
                return false;
            }
            return this.mImpl.supportsLocalVoiceInteraction();
        }

        void notifyActivityEventChanged() {
            synchronized (this) {
                if (this.mImpl == null) {
                    return;
                }
                Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.voiceinteraction.VoiceInteractionManagerService$VoiceInteractionManagerServiceStub$$ExternalSyntheticLambda2
                    public final void runOrThrow() {
                        VoiceInteractionManagerService.VoiceInteractionManagerServiceStub.this.m7601x33b31b1d();
                    }
                });
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$notifyActivityEventChanged$1$com-android-server-voiceinteraction-VoiceInteractionManagerService$VoiceInteractionManagerServiceStub  reason: not valid java name */
        public /* synthetic */ void m7601x33b31b1d() throws Exception {
            this.mImpl.notifyActivityEventChangedLocked();
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            try {
                return super.onTransact(code, data, reply, flags);
            } catch (RuntimeException e) {
                if (!(e instanceof SecurityException)) {
                    Slog.wtf(VoiceInteractionManagerService.TAG, "VoiceInteractionManagerService Crash", e);
                }
                throw e;
            }
        }

        public void initForUser(int userHandle) {
            TimingsTraceAndSlog t = null;
            initForUserNoTracing(userHandle);
            if (0 != 0) {
                t.traceEnd();
            }
        }

        private void initForUserNoTracing(int userHandle) {
            String curInteractorStr = Settings.Secure.getStringForUser(VoiceInteractionManagerService.this.mContext.getContentResolver(), "voice_interaction_service", userHandle);
            ComponentName curRecognizer = getCurRecognizer(userHandle);
            VoiceInteractionServiceInfo curInteractorInfo = null;
            if (curInteractorStr == null && curRecognizer != null && this.mEnableService && (curInteractorInfo = findAvailInteractor(userHandle, curRecognizer.getPackageName())) != null) {
                curRecognizer = null;
            }
            String forceInteractorPackage = getForceVoiceInteractionServicePackage(VoiceInteractionManagerService.this.mContext.getResources());
            if (forceInteractorPackage != null && (curInteractorInfo = findAvailInteractor(userHandle, forceInteractorPackage)) != null) {
                curRecognizer = null;
            }
            if (!this.mEnableService && curInteractorStr != null && !TextUtils.isEmpty(curInteractorStr)) {
                setCurInteractor(null, userHandle);
                curInteractorStr = "";
            }
            if (curRecognizer != null) {
                IPackageManager pm = AppGlobals.getPackageManager();
                ServiceInfo interactorInfo = null;
                ServiceInfo recognizerInfo = null;
                ComponentName curInteractor = !TextUtils.isEmpty(curInteractorStr) ? ComponentName.unflattenFromString(curInteractorStr) : null;
                try {
                    recognizerInfo = pm.getServiceInfo(curRecognizer, 786560L, userHandle);
                    if (recognizerInfo != null) {
                        RecognitionServiceInfo rsi = RecognitionServiceInfo.parseInfo(VoiceInteractionManagerService.this.mContext.getPackageManager(), recognizerInfo);
                        if (!TextUtils.isEmpty(rsi.getParseError())) {
                            Log.w(VoiceInteractionManagerService.TAG, "Parse error in getAvailableServices: " + rsi.getParseError());
                        }
                        if (!rsi.isSelectableAsDefault()) {
                            recognizerInfo = null;
                        }
                    }
                    if (curInteractor != null) {
                        interactorInfo = pm.getServiceInfo(curInteractor, 786432L, userHandle);
                    }
                } catch (RemoteException e) {
                }
                if (recognizerInfo != null && (curInteractor == null || interactorInfo != null)) {
                    return;
                }
            }
            if (curInteractorInfo == null && this.mEnableService && !"".equals(curInteractorStr)) {
                curInteractorInfo = findAvailInteractor(userHandle, null);
            }
            if (curInteractorInfo != null) {
                setCurInteractor(new ComponentName(curInteractorInfo.getServiceInfo().packageName, curInteractorInfo.getServiceInfo().name), userHandle);
            } else {
                setCurInteractor(null, userHandle);
            }
            initRecognizer(userHandle);
        }

        public void initRecognizer(int userHandle) {
            ComponentName curRecognizer = findAvailRecognizer(null, userHandle);
            if (curRecognizer != null) {
                setCurRecognizer(curRecognizer, userHandle);
            }
        }

        private boolean shouldEnableService(Context context) {
            if (getForceVoiceInteractionServicePackage(context.getResources()) != null) {
                return true;
            }
            return context.getPackageManager().hasSystemFeature("android.software.voice_recognizers");
        }

        private String getForceVoiceInteractionServicePackage(Resources res) {
            String interactorPackage = res.getString(17039976);
            if (TextUtils.isEmpty(interactorPackage)) {
                return null;
            }
            return interactorPackage;
        }

        public void systemRunning(boolean safeMode) {
            this.mSafeMode = safeMode;
            this.mPackageMonitor.register(VoiceInteractionManagerService.this.mContext, BackgroundThread.getHandler().getLooper(), UserHandle.ALL, true);
            new SettingsObserver(UiThread.getHandler());
            synchronized (this) {
                setCurrentUserLocked(ActivityManager.getCurrentUser());
                switchImplementationIfNeededLocked(false);
            }
        }

        private void setCurrentUserLocked(int userHandle) {
            this.mCurUser = userHandle;
            UserInfo userInfo = VoiceInteractionManagerService.this.mUserManagerInternal.getUserInfo(this.mCurUser);
            this.mCurUserSupported = VoiceInteractionManagerService.this.isUserSupported(userInfo);
        }

        public void switchUser(final int userHandle) {
            FgThread.getHandler().post(new Runnable() { // from class: com.android.server.voiceinteraction.VoiceInteractionManagerService$VoiceInteractionManagerServiceStub$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    VoiceInteractionManagerService.VoiceInteractionManagerServiceStub.this.m7602x8c324c2b(userHandle);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$switchUser$2$com-android-server-voiceinteraction-VoiceInteractionManagerService$VoiceInteractionManagerServiceStub  reason: not valid java name */
        public /* synthetic */ void m7602x8c324c2b(int userHandle) {
            synchronized (this) {
                setCurrentUserLocked(userHandle);
                switchImplementationIfNeededLocked(false);
            }
        }

        void switchImplementationIfNeeded(boolean force) {
            synchronized (this) {
                switchImplementationIfNeededLocked(force);
            }
        }

        void switchImplementationIfNeededLocked(boolean force) {
            if (!this.mCurUserSupported) {
                if (this.mImpl != null) {
                    this.mImpl.shutdownLocked();
                    setImplLocked(null);
                    return;
                }
                return;
            }
            TimingsTraceAndSlog t = null;
            switchImplementationIfNeededNoTracingLocked(force);
            if (0 != 0) {
                t.traceEnd();
            }
        }

        void switchImplementationIfNeededNoTracingLocked(boolean force) {
            if (!this.mSafeMode) {
                String curService = Settings.Secure.getStringForUser(VoiceInteractionManagerService.this.mResolver, "voice_interaction_service", this.mCurUser);
                ComponentName serviceComponent = null;
                ServiceInfo serviceInfo = null;
                if (curService != null && !curService.isEmpty()) {
                    try {
                        serviceComponent = ComponentName.unflattenFromString(curService);
                        serviceInfo = AppGlobals.getPackageManager().getServiceInfo(serviceComponent, 0L, this.mCurUser);
                    } catch (RemoteException | RuntimeException e) {
                        Slog.wtf(VoiceInteractionManagerService.TAG, "Bad voice interaction service name " + curService, e);
                        serviceComponent = null;
                        serviceInfo = null;
                    }
                }
                boolean hasComponent = (serviceComponent == null || serviceInfo == null) ? false : true;
                if (VoiceInteractionManagerService.this.mUserManagerInternal.isUserUnlockingOrUnlocked(this.mCurUser)) {
                    if (!hasComponent) {
                        VoiceInteractionManagerService.this.mShortcutServiceInternal.setShortcutHostPackage(VoiceInteractionManagerService.TAG, (String) null, this.mCurUser);
                        VoiceInteractionManagerService.this.mAtmInternal.setAllowAppSwitches(VoiceInteractionManagerService.TAG, -1, this.mCurUser);
                    } else {
                        VoiceInteractionManagerService.this.mShortcutServiceInternal.setShortcutHostPackage(VoiceInteractionManagerService.TAG, serviceComponent.getPackageName(), this.mCurUser);
                        VoiceInteractionManagerService.this.mAtmInternal.setAllowAppSwitches(VoiceInteractionManagerService.TAG, serviceInfo.applicationInfo.uid, this.mCurUser);
                    }
                }
                if (force || this.mImpl == null || this.mImpl.mUser != this.mCurUser || !this.mImpl.mComponent.equals(serviceComponent)) {
                    unloadAllKeyphraseModels();
                    if (this.mImpl != null) {
                        this.mImpl.shutdownLocked();
                    }
                    if (hasComponent) {
                        setImplLocked(new VoiceInteractionManagerServiceImpl(VoiceInteractionManagerService.this.mContext, UiThread.getHandler(), this, this.mCurUser, serviceComponent));
                        this.mImpl.startLocked();
                        return;
                    }
                    setImplLocked(null);
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public List<ResolveInfo> queryInteractorServices(int user, String packageName) {
            return VoiceInteractionManagerService.this.mContext.getPackageManager().queryIntentServicesAsUser(new Intent("android.service.voice.VoiceInteractionService").setPackage(packageName), 786560, user);
        }

        VoiceInteractionServiceInfo findAvailInteractor(int user, String packageName) {
            List<ResolveInfo> available = queryInteractorServices(user, packageName);
            int numAvailable = available.size();
            if (numAvailable == 0) {
                Slog.w(VoiceInteractionManagerService.TAG, "no available voice interaction services found for user " + user);
                return null;
            }
            VoiceInteractionServiceInfo foundInfo = null;
            for (int i = 0; i < numAvailable; i++) {
                ServiceInfo cur = available.get(i).serviceInfo;
                if ((cur.applicationInfo.flags & 1) != 0) {
                    VoiceInteractionServiceInfo info = new VoiceInteractionServiceInfo(VoiceInteractionManagerService.this.mContext.getPackageManager(), cur);
                    if (info.getParseError() != null) {
                        Slog.w(VoiceInteractionManagerService.TAG, "Bad interaction service " + cur.packageName + SliceClientPermissions.SliceAuthority.DELIMITER + cur.name + ": " + info.getParseError());
                    } else if (foundInfo == null) {
                        foundInfo = info;
                    } else {
                        Slog.w(VoiceInteractionManagerService.TAG, "More than one voice interaction service, picking first " + new ComponentName(foundInfo.getServiceInfo().packageName, foundInfo.getServiceInfo().name) + " over " + new ComponentName(cur.packageName, cur.name));
                    }
                }
            }
            return foundInfo;
        }

        ComponentName getCurInteractor(int userHandle) {
            String curInteractor = Settings.Secure.getStringForUser(VoiceInteractionManagerService.this.mContext.getContentResolver(), "voice_interaction_service", userHandle);
            if (TextUtils.isEmpty(curInteractor)) {
                return null;
            }
            return ComponentName.unflattenFromString(curInteractor);
        }

        void setCurInteractor(ComponentName comp, int userHandle) {
            Settings.Secure.putStringForUser(VoiceInteractionManagerService.this.mContext.getContentResolver(), "voice_interaction_service", comp != null ? comp.flattenToShortString() : "", userHandle);
        }

        ComponentName findAvailRecognizer(String prefPackage, int userHandle) {
            if (prefPackage == null) {
                prefPackage = getDefaultRecognizer();
            }
            List<RecognitionServiceInfo> available = RecognitionServiceInfo.getAvailableServices(VoiceInteractionManagerService.this.mContext, userHandle);
            if (available.size() == 0) {
                Slog.w(VoiceInteractionManagerService.TAG, "no available voice recognition services found for user " + userHandle);
                return null;
            }
            List<RecognitionServiceInfo> nonSelectableAsDefault = removeNonSelectableAsDefault(available);
            if (available.size() == 0) {
                Slog.w(VoiceInteractionManagerService.TAG, "No selectableAsDefault recognition services found for user " + userHandle + ". Falling back to non selectableAsDefault ones.");
                available = nonSelectableAsDefault;
            }
            int numAvailable = available.size();
            if (prefPackage != null) {
                for (int i = 0; i < numAvailable; i++) {
                    ServiceInfo serviceInfo = available.get(i).getServiceInfo();
                    if (prefPackage.equals(serviceInfo.packageName)) {
                        return new ComponentName(serviceInfo.packageName, serviceInfo.name);
                    }
                }
            }
            if (numAvailable > 1) {
                Slog.w(VoiceInteractionManagerService.TAG, "more than one voice recognition service found, picking first");
            }
            ServiceInfo serviceInfo2 = available.get(0).getServiceInfo();
            return new ComponentName(serviceInfo2.packageName, serviceInfo2.name);
        }

        private List<RecognitionServiceInfo> removeNonSelectableAsDefault(List<RecognitionServiceInfo> services) {
            List<RecognitionServiceInfo> nonSelectableAsDefault = new ArrayList<>();
            for (int i = services.size() - 1; i >= 0; i--) {
                if (!services.get(i).isSelectableAsDefault()) {
                    nonSelectableAsDefault.add(services.remove(i));
                }
            }
            return nonSelectableAsDefault;
        }

        public String getDefaultRecognizer() {
            String recognizer = VoiceInteractionManagerService.this.mContext.getString(17039406);
            if (TextUtils.isEmpty(recognizer)) {
                return null;
            }
            return recognizer;
        }

        ComponentName getCurRecognizer(int userHandle) {
            String curRecognizer = Settings.Secure.getStringForUser(VoiceInteractionManagerService.this.mContext.getContentResolver(), "voice_recognition_service", userHandle);
            if (TextUtils.isEmpty(curRecognizer)) {
                return null;
            }
            return ComponentName.unflattenFromString(curRecognizer);
        }

        void setCurRecognizer(ComponentName comp, int userHandle) {
            Settings.Secure.putStringForUser(VoiceInteractionManagerService.this.mContext.getContentResolver(), "voice_recognition_service", comp != null ? comp.flattenToShortString() : "", userHandle);
        }

        ComponentName getCurAssistant(int userHandle) {
            String curAssistant = Settings.Secure.getStringForUser(VoiceInteractionManagerService.this.mContext.getContentResolver(), "assistant", userHandle);
            if (TextUtils.isEmpty(curAssistant)) {
                return null;
            }
            return ComponentName.unflattenFromString(curAssistant);
        }

        void resetCurAssistant(int userHandle) {
            Settings.Secure.putStringForUser(VoiceInteractionManagerService.this.mContext.getContentResolver(), "assistant", null, userHandle);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public void forceRestartHotwordDetector() {
            this.mImpl.forceRestartHotwordDetector();
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public void setDebugHotwordLogging(boolean logging) {
            synchronized (this) {
                if (this.mImpl == null) {
                    Slog.w(VoiceInteractionManagerService.TAG, "setTemporaryLogging without running voice interaction service");
                } else {
                    this.mImpl.setDebugHotwordLoggingLocked(logging);
                }
            }
        }

        public void showSession(Bundle args, int flags) {
            synchronized (this) {
                enforceIsCurrentVoiceInteractionService();
                long caller = Binder.clearCallingIdentity();
                this.mImpl.showSessionLocked(args, flags, null, null);
                Binder.restoreCallingIdentity(caller);
            }
        }

        public boolean deliverNewSession(IBinder token, IVoiceInteractionSession session, IVoiceInteractor interactor) {
            boolean deliverNewSessionLocked;
            synchronized (this) {
                if (this.mImpl == null) {
                    throw new SecurityException("deliverNewSession without running voice interaction service");
                }
                long caller = Binder.clearCallingIdentity();
                deliverNewSessionLocked = this.mImpl.deliverNewSessionLocked(token, session, interactor);
                Binder.restoreCallingIdentity(caller);
            }
            return deliverNewSessionLocked;
        }

        public boolean showSessionFromSession(IBinder token, Bundle sessionArgs, int flags) {
            synchronized (this) {
                if (this.mImpl == null) {
                    Slog.w(VoiceInteractionManagerService.TAG, "showSessionFromSession without running voice interaction service");
                    return false;
                }
                long caller = Binder.clearCallingIdentity();
                boolean showSessionLocked = this.mImpl.showSessionLocked(sessionArgs, flags, null, null);
                Binder.restoreCallingIdentity(caller);
                return showSessionLocked;
            }
        }

        public boolean hideSessionFromSession(IBinder token) {
            synchronized (this) {
                if (this.mImpl == null) {
                    Slog.w(VoiceInteractionManagerService.TAG, "hideSessionFromSession without running voice interaction service");
                    return false;
                }
                long caller = Binder.clearCallingIdentity();
                boolean hideSessionLocked = this.mImpl.hideSessionLocked();
                Binder.restoreCallingIdentity(caller);
                return hideSessionLocked;
            }
        }

        public int startVoiceActivity(IBinder token, Intent intent, String resolvedType, String callingFeatureId) {
            synchronized (this) {
                if (this.mImpl == null) {
                    Slog.w(VoiceInteractionManagerService.TAG, "startVoiceActivity without running voice interaction service");
                    return -96;
                }
                int callingPid = Binder.getCallingPid();
                int callingUid = Binder.getCallingUid();
                long caller = Binder.clearCallingIdentity();
                ActivityInfo activityInfo = intent.resolveActivityInfo(VoiceInteractionManagerService.this.mContext.getPackageManager(), 131072);
                if (activityInfo != null) {
                    int activityUid = activityInfo.applicationInfo.uid;
                    this.mImpl.grantImplicitAccessLocked(activityUid, intent);
                } else {
                    Slog.w(VoiceInteractionManagerService.TAG, "Cannot find ActivityInfo in startVoiceActivity.");
                }
                int startVoiceActivityLocked = this.mImpl.startVoiceActivityLocked(callingFeatureId, callingPid, callingUid, token, intent, resolvedType);
                Binder.restoreCallingIdentity(caller);
                return startVoiceActivityLocked;
            }
        }

        public int startAssistantActivity(IBinder token, Intent intent, String resolvedType, String callingFeatureId) {
            synchronized (this) {
                if (this.mImpl == null) {
                    Slog.w(VoiceInteractionManagerService.TAG, "startAssistantActivity without running voice interaction service");
                    return -96;
                }
                int callingPid = Binder.getCallingPid();
                int callingUid = Binder.getCallingUid();
                long caller = Binder.clearCallingIdentity();
                int startAssistantActivityLocked = this.mImpl.startAssistantActivityLocked(callingFeatureId, callingPid, callingUid, token, intent, resolvedType);
                Binder.restoreCallingIdentity(caller);
                return startAssistantActivityLocked;
            }
        }

        public void requestDirectActions(IBinder token, int taskId, IBinder assistToken, RemoteCallback cancellationCallback, RemoteCallback resultCallback) {
            synchronized (this) {
                if (this.mImpl == null) {
                    Slog.w(VoiceInteractionManagerService.TAG, "requestDirectActions without running voice interaction service");
                    resultCallback.sendResult((Bundle) null);
                    return;
                }
                long caller = Binder.clearCallingIdentity();
                this.mImpl.requestDirectActionsLocked(token, taskId, assistToken, cancellationCallback, resultCallback);
                Binder.restoreCallingIdentity(caller);
            }
        }

        public void performDirectAction(IBinder token, String actionId, Bundle arguments, int taskId, IBinder assistToken, RemoteCallback cancellationCallback, RemoteCallback resultCallback) {
            synchronized (this) {
                try {
                    try {
                        if (this.mImpl == null) {
                            Slog.w(VoiceInteractionManagerService.TAG, "performDirectAction without running voice interaction service");
                            resultCallback.sendResult((Bundle) null);
                            return;
                        }
                        long caller = Binder.clearCallingIdentity();
                        this.mImpl.performDirectActionLocked(token, actionId, arguments, taskId, assistToken, cancellationCallback, resultCallback);
                        Binder.restoreCallingIdentity(caller);
                    } catch (Throwable th) {
                        th = th;
                        throw th;
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
            }
        }

        public void setKeepAwake(IBinder token, boolean keepAwake) {
            synchronized (this) {
                if (this.mImpl == null) {
                    Slog.w(VoiceInteractionManagerService.TAG, "setKeepAwake without running voice interaction service");
                    return;
                }
                long caller = Binder.clearCallingIdentity();
                this.mImpl.setKeepAwakeLocked(token, keepAwake);
                Binder.restoreCallingIdentity(caller);
            }
        }

        public void closeSystemDialogs(IBinder token) {
            synchronized (this) {
                if (this.mImpl == null) {
                    Slog.w(VoiceInteractionManagerService.TAG, "closeSystemDialogs without running voice interaction service");
                    return;
                }
                long caller = Binder.clearCallingIdentity();
                this.mImpl.closeSystemDialogsLocked(token);
                Binder.restoreCallingIdentity(caller);
            }
        }

        public void finish(IBinder token) {
            synchronized (this) {
                if (this.mImpl == null) {
                    Slog.w(VoiceInteractionManagerService.TAG, "finish without running voice interaction service");
                    return;
                }
                long caller = Binder.clearCallingIdentity();
                this.mImpl.finishLocked(token, false);
                Binder.restoreCallingIdentity(caller);
            }
        }

        public void setDisabledShowContext(int flags) {
            synchronized (this) {
                if (this.mImpl == null) {
                    Slog.w(VoiceInteractionManagerService.TAG, "setDisabledShowContext without running voice interaction service");
                    return;
                }
                int callingUid = Binder.getCallingUid();
                long caller = Binder.clearCallingIdentity();
                this.mImpl.setDisabledShowContextLocked(callingUid, flags);
                Binder.restoreCallingIdentity(caller);
            }
        }

        public int getDisabledShowContext() {
            synchronized (this) {
                if (this.mImpl == null) {
                    Slog.w(VoiceInteractionManagerService.TAG, "getDisabledShowContext without running voice interaction service");
                    return 0;
                }
                int callingUid = Binder.getCallingUid();
                long caller = Binder.clearCallingIdentity();
                int disabledShowContextLocked = this.mImpl.getDisabledShowContextLocked(callingUid);
                Binder.restoreCallingIdentity(caller);
                return disabledShowContextLocked;
            }
        }

        public int getUserDisabledShowContext() {
            synchronized (this) {
                if (this.mImpl == null) {
                    Slog.w(VoiceInteractionManagerService.TAG, "getUserDisabledShowContext without running voice interaction service");
                    return 0;
                }
                int callingUid = Binder.getCallingUid();
                long caller = Binder.clearCallingIdentity();
                int userDisabledShowContextLocked = this.mImpl.getUserDisabledShowContextLocked(callingUid);
                Binder.restoreCallingIdentity(caller);
                return userDisabledShowContextLocked;
            }
        }

        public void setDisabled(boolean disabled) {
            enforceCallingPermission("android.permission.ACCESS_VOICE_INTERACTION_SERVICE");
            synchronized (this) {
                if (this.mTemporarilyDisabled == disabled) {
                    return;
                }
                this.mTemporarilyDisabled = disabled;
                if (disabled) {
                    Slog.i(VoiceInteractionManagerService.TAG, "setDisabled(): temporarily disabling and hiding current session");
                    try {
                        hideCurrentSession();
                    } catch (RemoteException e) {
                        Log.w(VoiceInteractionManagerService.TAG, "Failed to call hideCurrentSession", e);
                    }
                } else {
                    Slog.i(VoiceInteractionManagerService.TAG, "setDisabled(): re-enabling");
                }
            }
        }

        public void startListeningVisibleActivityChanged(IBinder token) {
            synchronized (this) {
                if (this.mImpl == null) {
                    Slog.w(VoiceInteractionManagerService.TAG, "startListeningVisibleActivityChanged without running voice interaction service");
                    return;
                }
                long caller = Binder.clearCallingIdentity();
                this.mImpl.startListeningVisibleActivityChangedLocked(token);
                Binder.restoreCallingIdentity(caller);
            }
        }

        public void stopListeningVisibleActivityChanged(IBinder token) {
            synchronized (this) {
                if (this.mImpl == null) {
                    Slog.w(VoiceInteractionManagerService.TAG, "stopListeningVisibleActivityChanged without running voice interaction service");
                    return;
                }
                long caller = Binder.clearCallingIdentity();
                this.mImpl.stopListeningVisibleActivityChangedLocked(token);
                Binder.restoreCallingIdentity(caller);
            }
        }

        public void updateState(Identity voiceInteractorIdentity, PersistableBundle options, SharedMemory sharedMemory, IHotwordRecognitionStatusCallback callback, int detectorType) {
            enforceCallingPermission("android.permission.MANAGE_HOTWORD_DETECTION");
            synchronized (this) {
                enforceIsCurrentVoiceInteractionService();
                if (this.mImpl == null) {
                    Slog.w(VoiceInteractionManagerService.TAG, "updateState without running voice interaction service");
                    return;
                }
                voiceInteractorIdentity.uid = Binder.getCallingUid();
                voiceInteractorIdentity.pid = Binder.getCallingPid();
                long caller = Binder.clearCallingIdentity();
                this.mImpl.updateStateLocked(voiceInteractorIdentity, options, sharedMemory, callback, detectorType);
                Binder.restoreCallingIdentity(caller);
            }
        }

        public void shutdownHotwordDetectionService() {
            synchronized (this) {
                enforceIsCurrentVoiceInteractionService();
                if (this.mImpl == null) {
                    Slog.w(VoiceInteractionManagerService.TAG, "shutdownHotwordDetectionService without running voice interaction service");
                    return;
                }
                long caller = Binder.clearCallingIdentity();
                this.mImpl.shutdownHotwordDetectionServiceLocked();
                Binder.restoreCallingIdentity(caller);
            }
        }

        public void startListeningFromMic(AudioFormat audioFormat, IMicrophoneHotwordDetectionVoiceInteractionCallback callback) throws RemoteException {
            enforceCallingPermission("android.permission.RECORD_AUDIO");
            enforceCallingPermission("android.permission.CAPTURE_AUDIO_HOTWORD");
            synchronized (this) {
                enforceIsCurrentVoiceInteractionService();
                if (this.mImpl == null) {
                    Slog.w(VoiceInteractionManagerService.TAG, "startListeningFromMic without running voice interaction service");
                    return;
                }
                long caller = Binder.clearCallingIdentity();
                this.mImpl.startListeningFromMicLocked(audioFormat, callback);
                Binder.restoreCallingIdentity(caller);
            }
        }

        public void startListeningFromExternalSource(ParcelFileDescriptor audioStream, AudioFormat audioFormat, PersistableBundle options, IMicrophoneHotwordDetectionVoiceInteractionCallback callback) throws RemoteException {
            synchronized (this) {
                enforceIsCurrentVoiceInteractionService();
                if (this.mImpl == null) {
                    Slog.w(VoiceInteractionManagerService.TAG, "startListeningFromExternalSource without running voice interaction service");
                    return;
                }
                long caller = Binder.clearCallingIdentity();
                this.mImpl.startListeningFromExternalSourceLocked(audioStream, audioFormat, options, callback);
                Binder.restoreCallingIdentity(caller);
            }
        }

        public void stopListeningFromMic() throws RemoteException {
            synchronized (this) {
                enforceIsCurrentVoiceInteractionService();
                if (this.mImpl == null) {
                    Slog.w(VoiceInteractionManagerService.TAG, "stopListeningFromMic without running voice interaction service");
                    return;
                }
                long caller = Binder.clearCallingIdentity();
                this.mImpl.stopListeningFromMicLocked();
                Binder.restoreCallingIdentity(caller);
            }
        }

        public void triggerHardwareRecognitionEventForTest(SoundTrigger.KeyphraseRecognitionEvent event, IHotwordRecognitionStatusCallback callback) throws RemoteException {
            enforceCallingPermission("android.permission.RECORD_AUDIO");
            enforceCallingPermission("android.permission.CAPTURE_AUDIO_HOTWORD");
            synchronized (this) {
                enforceIsCurrentVoiceInteractionService();
                if (this.mImpl == null) {
                    Slog.w(VoiceInteractionManagerService.TAG, "triggerHardwareRecognitionEventForTest without running voice interaction service");
                    return;
                }
                long caller = Binder.clearCallingIdentity();
                this.mImpl.triggerHardwareRecognitionEventForTestLocked(event, callback);
                Binder.restoreCallingIdentity(caller);
            }
        }

        public SoundTrigger.KeyphraseSoundModel getKeyphraseSoundModel(int keyphraseId, String bcp47Locale) {
            enforceCallerAllowedToEnrollVoiceModel();
            if (bcp47Locale == null) {
                throw new IllegalArgumentException("Illegal argument(s) in getKeyphraseSoundModel");
            }
            int callingUserId = UserHandle.getCallingUserId();
            long caller = Binder.clearCallingIdentity();
            try {
                return VoiceInteractionManagerService.this.mDbHelper.getKeyphraseSoundModel(keyphraseId, callingUserId, bcp47Locale);
            } finally {
                Binder.restoreCallingIdentity(caller);
            }
        }

        public int updateKeyphraseSoundModel(SoundTrigger.KeyphraseSoundModel model) {
            enforceCallerAllowedToEnrollVoiceModel();
            if (model == null) {
                throw new IllegalArgumentException("Model must not be null");
            }
            long caller = Binder.clearCallingIdentity();
            try {
                if (VoiceInteractionManagerService.this.mDbHelper.updateKeyphraseSoundModel(model)) {
                    synchronized (this) {
                        if (this.mImpl != null && this.mImpl.mService != null) {
                            this.mImpl.notifySoundModelsChangedLocked();
                        }
                    }
                    return 0;
                }
                return Integer.MIN_VALUE;
            } finally {
                Binder.restoreCallingIdentity(caller);
            }
        }

        /* JADX DEBUG: Finally have unexpected throw blocks count: 2, expect 1 */
        public int deleteKeyphraseSoundModel(int keyphraseId, String bcp47Locale) {
            int unloadStatus;
            enforceCallerAllowedToEnrollVoiceModel();
            if (bcp47Locale == null) {
                throw new IllegalArgumentException("Illegal argument(s) in deleteKeyphraseSoundModel");
            }
            int callingUserId = UserHandle.getCallingUserId();
            long caller = Binder.clearCallingIdentity();
            try {
                SoundTriggerSession session = VoiceInteractionManagerService.this.mLoadedKeyphraseIds.get(Integer.valueOf(keyphraseId));
                if (session != null && (unloadStatus = session.unloadKeyphraseModel(keyphraseId)) != 0) {
                    Slog.w(VoiceInteractionManagerService.TAG, "Unable to unload keyphrase sound model:" + unloadStatus);
                }
                boolean deleted = VoiceInteractionManagerService.this.mDbHelper.deleteKeyphraseSoundModel(keyphraseId, callingUserId, bcp47Locale);
                int i = deleted ? 0 : Integer.MIN_VALUE;
                if (deleted) {
                    synchronized (this) {
                        if (this.mImpl != null && this.mImpl.mService != null) {
                            this.mImpl.notifySoundModelsChangedLocked();
                        }
                        VoiceInteractionManagerService.this.mLoadedKeyphraseIds.remove(Integer.valueOf(keyphraseId));
                    }
                }
                Binder.restoreCallingIdentity(caller);
                return i;
            } catch (Throwable th) {
                if (0 != 0) {
                    synchronized (this) {
                        if (this.mImpl != null && this.mImpl.mService != null) {
                            this.mImpl.notifySoundModelsChangedLocked();
                        }
                        VoiceInteractionManagerService.this.mLoadedKeyphraseIds.remove(Integer.valueOf(keyphraseId));
                    }
                }
                Binder.restoreCallingIdentity(caller);
                throw th;
            }
        }

        public boolean isEnrolledForKeyphrase(int keyphraseId, String bcp47Locale) {
            synchronized (this) {
                enforceIsCurrentVoiceInteractionService();
            }
            if (bcp47Locale == null) {
                throw new IllegalArgumentException("Illegal argument(s) in isEnrolledForKeyphrase");
            }
            int callingUserId = UserHandle.getCallingUserId();
            long caller = Binder.clearCallingIdentity();
            try {
                SoundTrigger.KeyphraseSoundModel model = VoiceInteractionManagerService.this.mDbHelper.getKeyphraseSoundModel(keyphraseId, callingUserId, bcp47Locale);
                return model != null;
            } finally {
                Binder.restoreCallingIdentity(caller);
            }
        }

        /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1480=4] */
        public KeyphraseMetadata getEnrolledKeyphraseMetadata(String keyphrase, String bcp47Locale) {
            SoundTrigger.Keyphrase[] keyphrases;
            synchronized (this) {
                enforceIsCurrentVoiceInteractionService();
            }
            if (bcp47Locale != null) {
                int callingUserId = UserHandle.getCallingUserId();
                long caller = Binder.clearCallingIdentity();
                try {
                    SoundTrigger.KeyphraseSoundModel model = VoiceInteractionManagerService.this.mDbHelper.getKeyphraseSoundModel(keyphrase, callingUserId, bcp47Locale);
                    if (model == null) {
                        return null;
                    }
                    for (SoundTrigger.Keyphrase phrase : model.getKeyphrases()) {
                        if (keyphrase.equals(phrase.getText())) {
                            ArraySet<Locale> locales = new ArraySet<>();
                            locales.add(phrase.getLocale());
                            return new KeyphraseMetadata(phrase.getId(), phrase.getText(), locales, phrase.getRecognitionModes());
                        }
                    }
                    return null;
                } finally {
                    Binder.restoreCallingIdentity(caller);
                }
            }
            throw new IllegalArgumentException("Illegal argument(s) in isEnrolledForKeyphrase");
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes2.dex */
        public class SoundTriggerSession implements IVoiceInteractionSoundTriggerSession {
            final SoundTriggerInternal.Session mSession;
            private IHotwordRecognitionStatusCallback mSessionExternalCallback;
            private IRecognitionStatusCallback mSessionInternalCallback;

            SoundTriggerSession(SoundTriggerInternal.Session session) {
                this.mSession = session;
            }

            public SoundTrigger.ModuleProperties getDspModuleProperties() {
                SoundTrigger.ModuleProperties moduleProperties;
                synchronized (VoiceInteractionManagerServiceStub.this) {
                    VoiceInteractionManagerServiceStub.this.enforceIsCurrentVoiceInteractionService();
                    long caller = Binder.clearCallingIdentity();
                    moduleProperties = this.mSession.getModuleProperties();
                    Binder.restoreCallingIdentity(caller);
                }
                return moduleProperties;
            }

            public int startRecognition(int keyphraseId, String bcp47Locale, IHotwordRecognitionStatusCallback callback, SoundTrigger.RecognitionConfig recognitionConfig, boolean runInBatterySaverMode) {
                synchronized (VoiceInteractionManagerServiceStub.this) {
                    VoiceInteractionManagerServiceStub.this.enforceIsCurrentVoiceInteractionService();
                    if (callback == null || recognitionConfig == null || bcp47Locale == null) {
                        throw new IllegalArgumentException("Illegal argument(s) in startRecognition");
                    }
                    if (runInBatterySaverMode) {
                        VoiceInteractionManagerServiceStub.this.enforceCallingPermission("android.permission.SOUND_TRIGGER_RUN_IN_BATTERY_SAVER");
                    }
                }
                int callingUserId = UserHandle.getCallingUserId();
                long caller = Binder.clearCallingIdentity();
                try {
                    SoundTrigger.KeyphraseSoundModel soundModel = VoiceInteractionManagerService.this.mDbHelper.getKeyphraseSoundModel(keyphraseId, callingUserId, bcp47Locale);
                    if (soundModel != null && soundModel.getUuid() != null && soundModel.getKeyphrases() != null) {
                        synchronized (VoiceInteractionManagerServiceStub.this) {
                            VoiceInteractionManagerService.this.mLoadedKeyphraseIds.put(Integer.valueOf(keyphraseId), this);
                            if (this.mSessionExternalCallback == null || this.mSessionInternalCallback == null || callback.asBinder() != this.mSessionExternalCallback.asBinder()) {
                                this.mSessionInternalCallback = VoiceInteractionManagerServiceStub.this.createSoundTriggerCallbackLocked(callback);
                                this.mSessionExternalCallback = callback;
                            }
                        }
                        return this.mSession.startRecognition(keyphraseId, soundModel, this.mSessionInternalCallback, recognitionConfig, runInBatterySaverMode);
                    }
                    Slog.w(VoiceInteractionManagerService.TAG, "No matching sound model found in startRecognition");
                    return Integer.MIN_VALUE;
                } finally {
                    Binder.restoreCallingIdentity(caller);
                }
            }

            public int stopRecognition(int keyphraseId, IHotwordRecognitionStatusCallback callback) {
                IRecognitionStatusCallback soundTriggerCallback;
                synchronized (VoiceInteractionManagerServiceStub.this) {
                    VoiceInteractionManagerServiceStub.this.enforceIsCurrentVoiceInteractionService();
                    if (this.mSessionExternalCallback != null && this.mSessionInternalCallback != null && callback.asBinder() == this.mSessionExternalCallback.asBinder()) {
                        soundTriggerCallback = this.mSessionInternalCallback;
                        this.mSessionExternalCallback = null;
                        this.mSessionInternalCallback = null;
                    }
                    soundTriggerCallback = VoiceInteractionManagerServiceStub.this.createSoundTriggerCallbackLocked(callback);
                    Slog.w(VoiceInteractionManagerService.TAG, "stopRecognition() called with a different callback thanstartRecognition()");
                    this.mSessionExternalCallback = null;
                    this.mSessionInternalCallback = null;
                }
                long caller = Binder.clearCallingIdentity();
                try {
                    return this.mSession.stopRecognition(keyphraseId, soundTriggerCallback);
                } finally {
                    Binder.restoreCallingIdentity(caller);
                }
            }

            public int setParameter(int keyphraseId, int modelParam, int value) {
                synchronized (VoiceInteractionManagerServiceStub.this) {
                    VoiceInteractionManagerServiceStub.this.enforceIsCurrentVoiceInteractionService();
                }
                long caller = Binder.clearCallingIdentity();
                try {
                    return this.mSession.setParameter(keyphraseId, modelParam, value);
                } finally {
                    Binder.restoreCallingIdentity(caller);
                }
            }

            public int getParameter(int keyphraseId, int modelParam) {
                synchronized (VoiceInteractionManagerServiceStub.this) {
                    VoiceInteractionManagerServiceStub.this.enforceIsCurrentVoiceInteractionService();
                }
                long caller = Binder.clearCallingIdentity();
                try {
                    return this.mSession.getParameter(keyphraseId, modelParam);
                } finally {
                    Binder.restoreCallingIdentity(caller);
                }
            }

            public SoundTrigger.ModelParamRange queryParameter(int keyphraseId, int modelParam) {
                synchronized (VoiceInteractionManagerServiceStub.this) {
                    VoiceInteractionManagerServiceStub.this.enforceIsCurrentVoiceInteractionService();
                }
                long caller = Binder.clearCallingIdentity();
                try {
                    return this.mSession.queryParameter(keyphraseId, modelParam);
                } finally {
                    Binder.restoreCallingIdentity(caller);
                }
            }

            public IBinder asBinder() {
                throw new UnsupportedOperationException("This object isn't intended to be used as a Binder.");
            }

            /* JADX INFO: Access modifiers changed from: private */
            public int unloadKeyphraseModel(int keyphraseId) {
                long caller = Binder.clearCallingIdentity();
                try {
                    return this.mSession.unloadKeyphraseModel(keyphraseId);
                } finally {
                    Binder.restoreCallingIdentity(caller);
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public synchronized void unloadAllKeyphraseModels() {
            for (int i = 0; i < VoiceInteractionManagerService.this.mLoadedKeyphraseIds.size(); i++) {
                int id = VoiceInteractionManagerService.this.mLoadedKeyphraseIds.keyAt(i).intValue();
                SoundTriggerSession session = VoiceInteractionManagerService.this.mLoadedKeyphraseIds.valueAt(i);
                int status = session.unloadKeyphraseModel(id);
                if (status != 0) {
                    Slog.w(VoiceInteractionManagerService.TAG, "Failed to unload keyphrase " + id + ":" + status);
                }
            }
            VoiceInteractionManagerService.this.mLoadedKeyphraseIds.clear();
        }

        public ComponentName getActiveServiceComponentName() {
            ComponentName componentName;
            synchronized (this) {
                componentName = this.mImpl != null ? this.mImpl.mComponent : null;
            }
            return componentName;
        }

        public boolean showSessionForActiveService(Bundle args, int sourceFlags, IVoiceInteractionSessionShowCallback showCallback, IBinder activityToken) {
            enforceCallingPermission("android.permission.ACCESS_VOICE_INTERACTION_SERVICE");
            synchronized (this) {
                if (this.mImpl == null) {
                    Slog.w(VoiceInteractionManagerService.TAG, "showSessionForActiveService without running voice interactionservice");
                    return false;
                } else if (this.mTemporarilyDisabled) {
                    Slog.i(VoiceInteractionManagerService.TAG, "showSessionForActiveService(): ignored while temporarily disabled");
                    return false;
                } else {
                    long caller = Binder.clearCallingIdentity();
                    boolean showSessionLocked = this.mImpl.showSessionLocked(args, sourceFlags | 1 | 2, showCallback, activityToken);
                    Binder.restoreCallingIdentity(caller);
                    return showSessionLocked;
                }
            }
        }

        public void hideCurrentSession() throws RemoteException {
            enforceCallingPermission("android.permission.ACCESS_VOICE_INTERACTION_SERVICE");
            if (this.mImpl == null) {
                return;
            }
            long caller = Binder.clearCallingIdentity();
            try {
                if (this.mImpl.mActiveSession != null && this.mImpl.mActiveSession.mSession != null) {
                    try {
                        this.mImpl.mActiveSession.mSession.closeSystemDialogs();
                    } catch (RemoteException e) {
                        Log.w(VoiceInteractionManagerService.TAG, "Failed to call closeSystemDialogs", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(caller);
            }
        }

        public void launchVoiceAssistFromKeyguard() {
            enforceCallingPermission("android.permission.ACCESS_VOICE_INTERACTION_SERVICE");
            synchronized (this) {
                if (this.mImpl == null) {
                    Slog.w(VoiceInteractionManagerService.TAG, "launchVoiceAssistFromKeyguard without running voice interactionservice");
                    return;
                }
                long caller = Binder.clearCallingIdentity();
                this.mImpl.launchVoiceAssistFromKeyguard();
                Binder.restoreCallingIdentity(caller);
            }
        }

        public boolean isSessionRunning() {
            boolean z;
            enforceCallingPermission("android.permission.ACCESS_VOICE_INTERACTION_SERVICE");
            synchronized (this) {
                z = (this.mImpl == null || this.mImpl.mActiveSession == null) ? false : true;
            }
            return z;
        }

        public boolean activeServiceSupportsAssist() {
            boolean z;
            enforceCallingPermission("android.permission.ACCESS_VOICE_INTERACTION_SERVICE");
            synchronized (this) {
                z = (this.mImpl == null || this.mImpl.mInfo == null || !this.mImpl.mInfo.getSupportsAssist()) ? false : true;
            }
            return z;
        }

        public boolean activeServiceSupportsLaunchFromKeyguard() throws RemoteException {
            boolean z;
            enforceCallingPermission("android.permission.ACCESS_VOICE_INTERACTION_SERVICE");
            synchronized (this) {
                z = (this.mImpl == null || this.mImpl.mInfo == null || !this.mImpl.mInfo.getSupportsLaunchFromKeyguard()) ? false : true;
            }
            return z;
        }

        public void onLockscreenShown() {
            enforceCallingPermission("android.permission.ACCESS_VOICE_INTERACTION_SERVICE");
            synchronized (this) {
                if (this.mImpl == null) {
                    return;
                }
                long caller = Binder.clearCallingIdentity();
                if (this.mImpl.mActiveSession != null && this.mImpl.mActiveSession.mSession != null) {
                    try {
                        this.mImpl.mActiveSession.mSession.onLockscreenShown();
                    } catch (RemoteException e) {
                        Log.w(VoiceInteractionManagerService.TAG, "Failed to call onLockscreenShown", e);
                    }
                }
                Binder.restoreCallingIdentity(caller);
            }
        }

        public void registerVoiceInteractionSessionListener(IVoiceInteractionSessionListener listener) {
            enforceCallingPermission("android.permission.ACCESS_VOICE_INTERACTION_SERVICE");
            synchronized (this) {
                VoiceInteractionManagerService.this.mVoiceInteractionSessionListeners.register(listener);
            }
        }

        public void getActiveServiceSupportedActions(List<String> voiceActions, IVoiceActionCheckCallback callback) {
            enforceCallingPermission("android.permission.ACCESS_VOICE_INTERACTION_SERVICE");
            synchronized (this) {
                if (this.mImpl == null) {
                    try {
                        callback.onComplete((List) null);
                    } catch (RemoteException e) {
                    }
                    return;
                }
                long caller = Binder.clearCallingIdentity();
                this.mImpl.getActiveServiceSupportedActions(voiceActions, callback);
                Binder.restoreCallingIdentity(caller);
            }
        }

        public void onSessionShown() {
            synchronized (this) {
                int size = VoiceInteractionManagerService.this.mVoiceInteractionSessionListeners.beginBroadcast();
                for (int i = 0; i < size; i++) {
                    IVoiceInteractionSessionListener listener = VoiceInteractionManagerService.this.mVoiceInteractionSessionListeners.getBroadcastItem(i);
                    try {
                        listener.onVoiceSessionShown();
                    } catch (RemoteException e) {
                        Slog.e(VoiceInteractionManagerService.TAG, "Error delivering voice interaction open event.", e);
                    }
                }
                VoiceInteractionManagerService.this.mVoiceInteractionSessionListeners.finishBroadcast();
            }
        }

        public void onSessionHidden() {
            synchronized (this) {
                int size = VoiceInteractionManagerService.this.mVoiceInteractionSessionListeners.beginBroadcast();
                for (int i = 0; i < size; i++) {
                    IVoiceInteractionSessionListener listener = VoiceInteractionManagerService.this.mVoiceInteractionSessionListeners.getBroadcastItem(i);
                    try {
                        listener.onVoiceSessionHidden();
                    } catch (RemoteException e) {
                        Slog.e(VoiceInteractionManagerService.TAG, "Error delivering voice interaction closed event.", e);
                    }
                }
                VoiceInteractionManagerService.this.mVoiceInteractionSessionListeners.finishBroadcast();
            }
        }

        public void setSessionWindowVisible(IBinder token, final boolean visible) {
            synchronized (this) {
                if (this.mImpl == null) {
                    Slog.w(VoiceInteractionManagerService.TAG, "setSessionWindowVisible called without running voice interaction service");
                    return;
                }
                if (this.mImpl.mActiveSession != null && token == this.mImpl.mActiveSession.mToken) {
                    long caller = Binder.clearCallingIdentity();
                    VoiceInteractionManagerService.this.mVoiceInteractionSessionListeners.broadcast(new Consumer() { // from class: com.android.server.voiceinteraction.VoiceInteractionManagerService$VoiceInteractionManagerServiceStub$$ExternalSyntheticLambda1
                        @Override // java.util.function.Consumer
                        public final void accept(Object obj) {
                            VoiceInteractionManagerService.VoiceInteractionManagerServiceStub.lambda$setSessionWindowVisible$3(visible, (IVoiceInteractionSessionListener) obj);
                        }
                    });
                    Binder.restoreCallingIdentity(caller);
                    return;
                }
                Slog.w(VoiceInteractionManagerService.TAG, "setSessionWindowVisible does not match active session");
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ void lambda$setSessionWindowVisible$3(boolean visible, IVoiceInteractionSessionListener listener) {
            try {
                listener.onVoiceSessionWindowVisibilityChanged(visible);
            } catch (RemoteException e) {
                Slog.e(VoiceInteractionManagerService.TAG, "Error delivering window visibility event to listener.", e);
            }
        }

        public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (DumpUtils.checkDumpPermission(VoiceInteractionManagerService.this.mContext, VoiceInteractionManagerService.TAG, pw)) {
                synchronized (this) {
                    pw.println("VOICE INTERACTION MANAGER (dumpsys voiceinteraction)");
                    pw.println("  mEnableService: " + this.mEnableService);
                    pw.println("  mTemporarilyDisabled: " + this.mTemporarilyDisabled);
                    pw.println("  mCurUser: " + this.mCurUser);
                    pw.println("  mCurUserSupported: " + this.mCurUserSupported);
                    VoiceInteractionManagerService.this.dumpSupportedUsers(pw, "  ");
                    VoiceInteractionManagerService.this.mDbHelper.dump(pw);
                    if (this.mImpl == null) {
                        pw.println("  (No active implementation)");
                    } else {
                        this.mImpl.dumpLocked(fd, pw, args);
                    }
                }
                VoiceInteractionManagerService.this.mSoundTriggerInternal.dump(fd, pw, args);
            }
        }

        /* JADX DEBUG: Multi-variable search result rejected for r8v0, resolved type: com.android.server.voiceinteraction.VoiceInteractionManagerService$VoiceInteractionManagerServiceStub */
        /* JADX WARN: Multi-variable type inference failed */
        public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
            new VoiceInteractionManagerServiceShellCommand(VoiceInteractionManagerService.this.mServiceStub).exec(this, in, out, err, args, callback, resultReceiver);
        }

        public void setUiHints(Bundle hints) {
            synchronized (this) {
                enforceIsCurrentVoiceInteractionService();
                int size = VoiceInteractionManagerService.this.mVoiceInteractionSessionListeners.beginBroadcast();
                for (int i = 0; i < size; i++) {
                    IVoiceInteractionSessionListener listener = VoiceInteractionManagerService.this.mVoiceInteractionSessionListeners.getBroadcastItem(i);
                    try {
                        listener.onSetUiHints(hints);
                    } catch (RemoteException e) {
                        Slog.e(VoiceInteractionManagerService.TAG, "Error delivering UI hints.", e);
                    }
                }
                VoiceInteractionManagerService.this.mVoiceInteractionSessionListeners.finishBroadcast();
            }
        }

        private boolean isCallerHoldingPermission(String permission) {
            return VoiceInteractionManagerService.this.mContext.checkCallingOrSelfPermission(permission) == 0;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void enforceCallingPermission(String permission) {
            if (!isCallerHoldingPermission(permission)) {
                throw new SecurityException("Caller does not hold the permission " + permission);
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void enforceIsCurrentVoiceInteractionService() {
            if (!isCallerCurrentVoiceInteractionService()) {
                throw new SecurityException("Caller is not the current voice interaction service");
            }
        }

        private void enforceCallerAllowedToEnrollVoiceModel() {
            if (isCallerHoldingPermission("android.permission.KEYPHRASE_ENROLLMENT_APPLICATION")) {
                return;
            }
            enforceCallingPermission("android.permission.MANAGE_VOICE_KEYPHRASES");
            enforceIsCurrentVoiceInteractionService();
        }

        private boolean isCallerCurrentVoiceInteractionService() {
            return this.mImpl != null && this.mImpl.mInfo.getServiceInfo().applicationInfo.uid == Binder.getCallingUid();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void setImplLocked(VoiceInteractionManagerServiceImpl impl) {
            this.mImpl = impl;
            VoiceInteractionManagerService.this.mAtmInternal.notifyActiveVoiceInteractionServiceChanged(getActiveServiceComponentName());
        }

        /* JADX INFO: Access modifiers changed from: private */
        public IRecognitionStatusCallback createSoundTriggerCallbackLocked(IHotwordRecognitionStatusCallback callback) {
            if (this.mImpl == null) {
                return null;
            }
            return this.mImpl.createSoundTriggerCallbackLocked(callback);
        }

        /* loaded from: classes2.dex */
        class RoleObserver implements OnRoleHoldersChangedListener {
            private PackageManager mPm;
            private RoleManager mRm;

            RoleObserver(Executor executor) {
                this.mPm = VoiceInteractionManagerService.this.mContext.getPackageManager();
                RoleManager roleManager = (RoleManager) VoiceInteractionManagerService.this.mContext.getSystemService(RoleManager.class);
                this.mRm = roleManager;
                roleManager.addOnRoleHoldersChangedListenerAsUser(executor, this, UserHandle.ALL);
                if (this.mRm.isRoleAvailable("android.app.role.ASSISTANT")) {
                    UserHandle currentUser = UserHandle.of(((ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class)).getCurrentUserId());
                    onRoleHoldersChanged("android.app.role.ASSISTANT", currentUser);
                }
            }

            public void onRoleHoldersChanged(String roleName, UserHandle user) {
                if (!roleName.equals("android.app.role.ASSISTANT")) {
                    return;
                }
                List<String> roleHolders = this.mRm.getRoleHoldersAsUser(roleName, user);
                int userId = user.getIdentifier();
                if (roleHolders.isEmpty()) {
                    Settings.Secure.putStringForUser(VoiceInteractionManagerService.this.getContext().getContentResolver(), "assistant", "", userId);
                    Settings.Secure.putStringForUser(VoiceInteractionManagerService.this.getContext().getContentResolver(), "voice_interaction_service", "", userId);
                    return;
                }
                String pkg = roleHolders.get(0);
                for (ResolveInfo resolveInfo : VoiceInteractionManagerServiceStub.this.queryInteractorServices(userId, pkg)) {
                    ServiceInfo serviceInfo = resolveInfo.serviceInfo;
                    VoiceInteractionServiceInfo voiceInteractionServiceInfo = new VoiceInteractionServiceInfo(this.mPm, serviceInfo);
                    if (voiceInteractionServiceInfo.getSupportsAssist()) {
                        String serviceComponentName = serviceInfo.getComponentName().flattenToShortString();
                        if (voiceInteractionServiceInfo.getRecognitionService() == null) {
                            Slog.e(VoiceInteractionManagerService.TAG, "The RecognitionService must be set to avoid boot loop on earlier platform version. Also make sure that this is a valid RecognitionService when running on Android 11 or earlier.");
                            serviceComponentName = "";
                        }
                        Settings.Secure.putStringForUser(VoiceInteractionManagerService.this.getContext().getContentResolver(), "assistant", serviceComponentName, userId);
                        Settings.Secure.putStringForUser(VoiceInteractionManagerService.this.getContext().getContentResolver(), "voice_interaction_service", serviceComponentName, userId);
                        return;
                    }
                }
                List<ResolveInfo> activities = this.mPm.queryIntentActivitiesAsUser(new Intent("android.intent.action.ASSIST").setPackage(pkg), 851968, userId);
                Iterator<ResolveInfo> it = activities.iterator();
                if (it.hasNext()) {
                    ResolveInfo resolveInfo2 = it.next();
                    ActivityInfo activityInfo = resolveInfo2.activityInfo;
                    Settings.Secure.putStringForUser(VoiceInteractionManagerService.this.getContext().getContentResolver(), "assistant", activityInfo.getComponentName().flattenToShortString(), userId);
                    Settings.Secure.putStringForUser(VoiceInteractionManagerService.this.getContext().getContentResolver(), "voice_interaction_service", "", userId);
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* loaded from: classes2.dex */
        public class SettingsObserver extends ContentObserver {
            SettingsObserver(Handler handler) {
                super(handler);
                ContentResolver resolver = VoiceInteractionManagerService.this.mContext.getContentResolver();
                resolver.registerContentObserver(Settings.Secure.getUriFor("voice_interaction_service"), false, this, -1);
            }

            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                synchronized (VoiceInteractionManagerServiceStub.this) {
                    VoiceInteractionManagerServiceStub.this.switchImplementationIfNeededLocked(false);
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void resetServicesIfNoRecognitionService(ComponentName serviceComponent, int userHandle) {
            for (ResolveInfo resolveInfo : queryInteractorServices(userHandle, serviceComponent.getPackageName())) {
                VoiceInteractionServiceInfo serviceInfo = new VoiceInteractionServiceInfo(VoiceInteractionManagerService.this.mContext.getPackageManager(), resolveInfo.serviceInfo);
                if (serviceInfo.getSupportsAssist() && serviceInfo.getRecognitionService() == null) {
                    Slog.e(VoiceInteractionManagerService.TAG, "The RecognitionService must be set to avoid boot loop on earlier platform version. Also make sure that this is a valid RecognitionService when running on Android 11 or earlier.");
                    setCurInteractor(null, userHandle);
                    resetCurAssistant(userHandle);
                }
            }
        }

        /* renamed from: com.android.server.voiceinteraction.VoiceInteractionManagerService$VoiceInteractionManagerServiceStub$2  reason: invalid class name */
        /* loaded from: classes2.dex */
        class AnonymousClass2 extends PackageMonitor {
            AnonymousClass2() {
            }

            public boolean onHandleForceStop(Intent intent, String[] packages, int uid, boolean doit) {
                boolean hitRec;
                boolean hitRec2;
                int userHandle = UserHandle.getUserId(uid);
                ComponentName curInteractor = VoiceInteractionManagerServiceStub.this.getCurInteractor(userHandle);
                ComponentName curRecognizer = VoiceInteractionManagerServiceStub.this.getCurRecognizer(userHandle);
                int length = packages.length;
                int i = 0;
                while (true) {
                    if (i >= length) {
                        hitRec = false;
                        hitRec2 = false;
                        break;
                    }
                    String pkg = packages[i];
                    if (curInteractor != null && pkg.equals(curInteractor.getPackageName())) {
                        hitRec = false;
                        hitRec2 = true;
                        break;
                    } else if (curRecognizer == null || !pkg.equals(curRecognizer.getPackageName())) {
                        i++;
                    } else {
                        hitRec = true;
                        hitRec2 = false;
                        break;
                    }
                }
                if (hitRec2 && doit) {
                    synchronized (VoiceInteractionManagerServiceStub.this) {
                        Slog.i(VoiceInteractionManagerService.TAG, "Force stopping current voice interactor: " + VoiceInteractionManagerServiceStub.this.getCurInteractor(userHandle));
                        VoiceInteractionManagerServiceStub.this.unloadAllKeyphraseModels();
                        if (VoiceInteractionManagerServiceStub.this.mImpl != null) {
                            VoiceInteractionManagerServiceStub.this.mImpl.shutdownLocked();
                            VoiceInteractionManagerServiceStub.this.setImplLocked(null);
                        }
                        VoiceInteractionManagerServiceStub.this.setCurInteractor(null, userHandle);
                        VoiceInteractionManagerServiceStub.this.setCurRecognizer(null, userHandle);
                        VoiceInteractionManagerServiceStub.this.resetCurAssistant(userHandle);
                        VoiceInteractionManagerServiceStub.this.initForUser(userHandle);
                        VoiceInteractionManagerServiceStub.this.switchImplementationIfNeededLocked(true);
                        Context context = VoiceInteractionManagerService.this.getContext();
                        ((RoleManager) context.getSystemService(RoleManager.class)).clearRoleHoldersAsUser("android.app.role.ASSISTANT", 0, UserHandle.of(userHandle), context.getMainExecutor(), new Consumer() { // from class: com.android.server.voiceinteraction.VoiceInteractionManagerService$VoiceInteractionManagerServiceStub$2$$ExternalSyntheticLambda0
                            @Override // java.util.function.Consumer
                            public final void accept(Object obj) {
                                VoiceInteractionManagerService.VoiceInteractionManagerServiceStub.AnonymousClass2.lambda$onHandleForceStop$0((Boolean) obj);
                            }
                        });
                    }
                } else if (hitRec && doit) {
                    synchronized (VoiceInteractionManagerServiceStub.this) {
                        Slog.i(VoiceInteractionManagerService.TAG, "Force stopping current voice recognizer: " + VoiceInteractionManagerServiceStub.this.getCurRecognizer(userHandle));
                        VoiceInteractionManagerServiceStub.this.setCurInteractor(null, userHandle);
                        VoiceInteractionManagerServiceStub.this.initRecognizer(userHandle);
                    }
                }
                return hitRec2 || hitRec;
            }

            /* JADX INFO: Access modifiers changed from: package-private */
            public static /* synthetic */ void lambda$onHandleForceStop$0(Boolean successful) {
                if (!successful.booleanValue()) {
                    Slog.e(VoiceInteractionManagerService.TAG, "Failed to clear default assistant for force stop");
                }
            }

            public void onHandleUserStop(Intent intent, int userHandle) {
            }

            public void onPackageModified(String pkgName) {
                if (VoiceInteractionManagerServiceStub.this.mCurUser != getChangingUserId() || isPackageAppearing(pkgName) != 0) {
                    return;
                }
                VoiceInteractionManagerServiceStub voiceInteractionManagerServiceStub = VoiceInteractionManagerServiceStub.this;
                if (voiceInteractionManagerServiceStub.getCurRecognizer(voiceInteractionManagerServiceStub.mCurUser) == null) {
                    VoiceInteractionManagerServiceStub voiceInteractionManagerServiceStub2 = VoiceInteractionManagerServiceStub.this;
                    voiceInteractionManagerServiceStub2.initRecognizer(voiceInteractionManagerServiceStub2.mCurUser);
                }
                String curInteractorStr = Settings.Secure.getStringForUser(VoiceInteractionManagerService.this.mContext.getContentResolver(), "voice_interaction_service", VoiceInteractionManagerServiceStub.this.mCurUser);
                VoiceInteractionManagerServiceStub voiceInteractionManagerServiceStub3 = VoiceInteractionManagerServiceStub.this;
                ComponentName curInteractor = voiceInteractionManagerServiceStub3.getCurInteractor(voiceInteractionManagerServiceStub3.mCurUser);
                if (curInteractor == null && !"".equals(curInteractorStr)) {
                    VoiceInteractionManagerServiceStub voiceInteractionManagerServiceStub4 = VoiceInteractionManagerServiceStub.this;
                    VoiceInteractionServiceInfo availInteractorInfo = voiceInteractionManagerServiceStub4.findAvailInteractor(voiceInteractionManagerServiceStub4.mCurUser, pkgName);
                    if (availInteractorInfo != null) {
                        ComponentName availInteractor = new ComponentName(availInteractorInfo.getServiceInfo().packageName, availInteractorInfo.getServiceInfo().name);
                        VoiceInteractionManagerServiceStub voiceInteractionManagerServiceStub5 = VoiceInteractionManagerServiceStub.this;
                        voiceInteractionManagerServiceStub5.setCurInteractor(availInteractor, voiceInteractionManagerServiceStub5.mCurUser);
                    }
                } else if (didSomePackagesChange()) {
                    if (curInteractor != null && pkgName.equals(curInteractor.getPackageName())) {
                        VoiceInteractionManagerServiceStub.this.switchImplementationIfNeeded(true);
                    }
                } else if (curInteractor != null && isComponentModified(curInteractor.getClassName())) {
                    VoiceInteractionManagerServiceStub.this.switchImplementationIfNeeded(true);
                }
            }

            public void onSomePackagesChanged() {
                int userHandle = getChangingUserId();
                synchronized (VoiceInteractionManagerServiceStub.this) {
                    ComponentName curInteractor = VoiceInteractionManagerServiceStub.this.getCurInteractor(userHandle);
                    ComponentName curRecognizer = VoiceInteractionManagerServiceStub.this.getCurRecognizer(userHandle);
                    ComponentName curAssistant = VoiceInteractionManagerServiceStub.this.getCurAssistant(userHandle);
                    if (curRecognizer == null) {
                        if (anyPackagesAppearing()) {
                            VoiceInteractionManagerServiceStub.this.initRecognizer(userHandle);
                        }
                    } else if (curInteractor != null) {
                        if (isPackageDisappearing(curInteractor.getPackageName()) == 3) {
                            VoiceInteractionManagerServiceStub.this.setCurInteractor(null, userHandle);
                            VoiceInteractionManagerServiceStub.this.setCurRecognizer(null, userHandle);
                            VoiceInteractionManagerServiceStub.this.resetCurAssistant(userHandle);
                            VoiceInteractionManagerServiceStub.this.initForUser(userHandle);
                            return;
                        }
                        if (isPackageAppearing(curInteractor.getPackageName()) != 0) {
                            VoiceInteractionManagerServiceStub.this.resetServicesIfNoRecognitionService(curInteractor, userHandle);
                            if (VoiceInteractionManagerServiceStub.this.mImpl != null && curInteractor.getPackageName().equals(VoiceInteractionManagerServiceStub.this.mImpl.mComponent.getPackageName())) {
                                VoiceInteractionManagerServiceStub.this.switchImplementationIfNeededLocked(true);
                            }
                        }
                    } else {
                        if (curAssistant != null) {
                            if (isPackageDisappearing(curAssistant.getPackageName()) == 3) {
                                VoiceInteractionManagerServiceStub.this.setCurInteractor(null, userHandle);
                                VoiceInteractionManagerServiceStub.this.setCurRecognizer(null, userHandle);
                                VoiceInteractionManagerServiceStub.this.resetCurAssistant(userHandle);
                                VoiceInteractionManagerServiceStub.this.initForUser(userHandle);
                                return;
                            } else if (isPackageAppearing(curAssistant.getPackageName()) != 0) {
                                VoiceInteractionManagerServiceStub.this.resetServicesIfNoRecognitionService(curAssistant, userHandle);
                            }
                        }
                        int change = isPackageDisappearing(curRecognizer.getPackageName());
                        if (change != 3 && change != 2) {
                            if (isPackageModified(curRecognizer.getPackageName())) {
                                VoiceInteractionManagerServiceStub voiceInteractionManagerServiceStub = VoiceInteractionManagerServiceStub.this;
                                voiceInteractionManagerServiceStub.setCurRecognizer(voiceInteractionManagerServiceStub.findAvailRecognizer(curRecognizer.getPackageName(), userHandle), userHandle);
                            }
                        }
                        VoiceInteractionManagerServiceStub voiceInteractionManagerServiceStub2 = VoiceInteractionManagerServiceStub.this;
                        voiceInteractionManagerServiceStub2.setCurRecognizer(voiceInteractionManagerServiceStub2.findAvailRecognizer(null, userHandle), userHandle);
                    }
                }
            }
        }
    }
}
