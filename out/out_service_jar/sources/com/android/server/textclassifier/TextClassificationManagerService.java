package com.android.server.textclassifier;

import android.app.RemoteAction;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.graphics.drawable.Icon;
import android.hardware.audio.common.V2_0.AudioFormat;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.UserHandle;
import android.provider.DeviceConfig;
import android.service.textclassifier.ITextClassifierCallback;
import android.service.textclassifier.ITextClassifierService;
import android.service.textclassifier.TextClassifierService;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.LruCache;
import android.util.Slog;
import android.util.SparseArray;
import android.view.textclassifier.ConversationAction;
import android.view.textclassifier.ConversationActions;
import android.view.textclassifier.SelectionEvent;
import android.view.textclassifier.SystemTextClassifierMetadata;
import android.view.textclassifier.TextClassification;
import android.view.textclassifier.TextClassificationConstants;
import android.view.textclassifier.TextClassificationContext;
import android.view.textclassifier.TextClassificationManager;
import android.view.textclassifier.TextClassificationSessionId;
import android.view.textclassifier.TextClassifierEvent;
import android.view.textclassifier.TextLanguage;
import android.view.textclassifier.TextLinks;
import android.view.textclassifier.TextSelection;
import com.android.internal.content.PackageMonitor;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.FunctionalUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.Preconditions;
import com.android.server.SystemService;
import com.android.server.pm.verify.domain.DomainVerificationLegacySettings;
import com.android.server.textclassifier.FixedSizeQueue;
import com.android.server.textclassifier.TextClassificationManagerService;
import com.android.server.timezonedetector.ServiceConfigAccessor;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
/* loaded from: classes2.dex */
public final class TextClassificationManagerService extends ITextClassifierService.Stub {
    private static final boolean DEBUG = false;
    private static final String LOG_TAG = "TextClassificationManagerService";
    private static final ITextClassifierCallback NO_OP_CALLBACK = new ITextClassifierCallback() { // from class: com.android.server.textclassifier.TextClassificationManagerService.1
        public void onSuccess(Bundle result) {
        }

        public void onFailure() {
        }

        public IBinder asBinder() {
            return null;
        }
    };
    private final Context mContext;
    private final String mDefaultTextClassifierPackage;
    private final Object mLock;
    private final SessionCache mSessionCache;
    private final TextClassificationConstants mSettings;
    private final TextClassifierSettingsListener mSettingsListener;
    private final String mSystemTextClassifierPackage;
    final SparseArray<UserState> mUserStates;

    /* loaded from: classes2.dex */
    public static final class Lifecycle extends SystemService {
        private final TextClassificationManagerService mManagerService;

        public Lifecycle(Context context) {
            super(context);
            this.mManagerService = new TextClassificationManagerService(context);
        }

        @Override // com.android.server.SystemService
        public void onStart() {
            try {
                publishBinderService("textclassification", this.mManagerService);
                this.mManagerService.startListenSettings();
                this.mManagerService.startTrackingPackageChanges();
            } catch (Throwable t) {
                Slog.e(TextClassificationManagerService.LOG_TAG, "Could not start the TextClassificationManagerService.", t);
            }
        }

        @Override // com.android.server.SystemService
        public void onUserStarting(SystemService.TargetUser user) {
            updatePackageStateForUser(user.getUserIdentifier());
            processAnyPendingWork(user.getUserIdentifier());
        }

        @Override // com.android.server.SystemService
        public void onUserUnlocking(SystemService.TargetUser user) {
            updatePackageStateForUser(user.getUserIdentifier());
            processAnyPendingWork(user.getUserIdentifier());
        }

        private void processAnyPendingWork(int userId) {
            synchronized (this.mManagerService.mLock) {
                this.mManagerService.getUserStateLocked(userId).bindIfHasPendingRequestsLocked();
            }
        }

        private void updatePackageStateForUser(int userId) {
            synchronized (this.mManagerService.mLock) {
                this.mManagerService.getUserStateLocked(userId).updatePackageStateLocked();
            }
        }

        @Override // com.android.server.SystemService
        public void onUserStopping(SystemService.TargetUser user) {
            int userId = user.getUserIdentifier();
            synchronized (this.mManagerService.mLock) {
                UserState userState = this.mManagerService.peekUserStateLocked(userId);
                if (userState != null) {
                    userState.cleanupServiceLocked();
                    this.mManagerService.mUserStates.remove(userId);
                }
            }
        }
    }

    private TextClassificationManagerService(Context context) {
        this.mUserStates = new SparseArray<>();
        Context context2 = (Context) Objects.requireNonNull(context);
        this.mContext = context2;
        Object obj = new Object();
        this.mLock = obj;
        this.mSettings = new TextClassificationConstants();
        this.mSettingsListener = new TextClassifierSettingsListener(context2);
        PackageManager packageManager = context2.getPackageManager();
        this.mDefaultTextClassifierPackage = packageManager.getDefaultTextClassifierPackageName();
        this.mSystemTextClassifierPackage = packageManager.getSystemTextClassifierPackageName();
        this.mSessionCache = new SessionCache(obj);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void startListenSettings() {
        this.mSettingsListener.registerObserver();
    }

    void startTrackingPackageChanges() {
        PackageMonitor monitor = new PackageMonitor() { // from class: com.android.server.textclassifier.TextClassificationManagerService.2
            public void onPackageAdded(String packageName, int uid) {
                notifyPackageInstallStatusChange(packageName, true);
            }

            public void onPackageRemoved(String packageName, int uid) {
                notifyPackageInstallStatusChange(packageName, false);
            }

            public void onPackageModified(String packageName) {
                int userId = getChangingUserId();
                synchronized (TextClassificationManagerService.this.mLock) {
                    UserState userState = TextClassificationManagerService.this.getUserStateLocked(userId);
                    ServiceState serviceState = userState.getServiceStateLocked(packageName);
                    if (serviceState != null) {
                        serviceState.onPackageModifiedLocked();
                    }
                }
            }

            private void notifyPackageInstallStatusChange(String packageName, boolean installed) {
                int userId = getChangingUserId();
                synchronized (TextClassificationManagerService.this.mLock) {
                    UserState userState = TextClassificationManagerService.this.getUserStateLocked(userId);
                    ServiceState serviceState = userState.getServiceStateLocked(packageName);
                    if (serviceState != null) {
                        serviceState.onPackageInstallStatusChangeLocked(installed);
                    }
                }
            }
        };
        monitor.register(this.mContext, (Looper) null, UserHandle.ALL, true);
    }

    public void onConnectedStateChanged(int connected) {
    }

    public void onSuggestSelection(final TextClassificationSessionId sessionId, final TextSelection.Request request, final ITextClassifierCallback callback) throws RemoteException {
        Objects.requireNonNull(request);
        Objects.requireNonNull(request.getSystemTextClassifierMetadata());
        handleRequest(request.getSystemTextClassifierMetadata(), true, true, new FunctionalUtils.ThrowingConsumer() { // from class: com.android.server.textclassifier.TextClassificationManagerService$$ExternalSyntheticLambda7
            public final void acceptOrThrow(Object obj) {
                ((ITextClassifierService) obj).onSuggestSelection(sessionId, request, TextClassificationManagerService.wrap(callback));
            }
        }, "onSuggestSelection", callback);
    }

    public void onClassifyText(final TextClassificationSessionId sessionId, final TextClassification.Request request, final ITextClassifierCallback callback) throws RemoteException {
        Objects.requireNonNull(request);
        Objects.requireNonNull(request.getSystemTextClassifierMetadata());
        handleRequest(request.getSystemTextClassifierMetadata(), true, true, new FunctionalUtils.ThrowingConsumer() { // from class: com.android.server.textclassifier.TextClassificationManagerService$$ExternalSyntheticLambda8
            public final void acceptOrThrow(Object obj) {
                ((ITextClassifierService) obj).onClassifyText(sessionId, request, TextClassificationManagerService.wrap(callback));
            }
        }, "onClassifyText", callback);
    }

    public void onGenerateLinks(final TextClassificationSessionId sessionId, final TextLinks.Request request, final ITextClassifierCallback callback) throws RemoteException {
        Objects.requireNonNull(request);
        Objects.requireNonNull(request.getSystemTextClassifierMetadata());
        handleRequest(request.getSystemTextClassifierMetadata(), true, true, new FunctionalUtils.ThrowingConsumer() { // from class: com.android.server.textclassifier.TextClassificationManagerService$$ExternalSyntheticLambda12
            public final void acceptOrThrow(Object obj) {
                ((ITextClassifierService) obj).onGenerateLinks(sessionId, request, callback);
            }
        }, "onGenerateLinks", callback);
    }

    public void onSelectionEvent(final TextClassificationSessionId sessionId, final SelectionEvent event) throws RemoteException {
        Objects.requireNonNull(event);
        Objects.requireNonNull(event.getSystemTextClassifierMetadata());
        handleRequest(event.getSystemTextClassifierMetadata(), true, true, new FunctionalUtils.ThrowingConsumer() { // from class: com.android.server.textclassifier.TextClassificationManagerService$$ExternalSyntheticLambda4
            public final void acceptOrThrow(Object obj) {
                ((ITextClassifierService) obj).onSelectionEvent(sessionId, event);
            }
        }, "onSelectionEvent", NO_OP_CALLBACK);
    }

    public void onTextClassifierEvent(final TextClassificationSessionId sessionId, final TextClassifierEvent event) throws RemoteException {
        Objects.requireNonNull(event);
        TextClassificationContext eventContext = event.getEventContext();
        SystemTextClassifierMetadata systemTcMetadata = eventContext != null ? eventContext.getSystemTextClassifierMetadata() : null;
        handleRequest(systemTcMetadata, true, true, new FunctionalUtils.ThrowingConsumer() { // from class: com.android.server.textclassifier.TextClassificationManagerService$$ExternalSyntheticLambda5
            public final void acceptOrThrow(Object obj) {
                ((ITextClassifierService) obj).onTextClassifierEvent(sessionId, event);
            }
        }, "onTextClassifierEvent", NO_OP_CALLBACK);
    }

    public void onDetectLanguage(final TextClassificationSessionId sessionId, final TextLanguage.Request request, final ITextClassifierCallback callback) throws RemoteException {
        Objects.requireNonNull(request);
        Objects.requireNonNull(request.getSystemTextClassifierMetadata());
        handleRequest(request.getSystemTextClassifierMetadata(), true, true, new FunctionalUtils.ThrowingConsumer() { // from class: com.android.server.textclassifier.TextClassificationManagerService$$ExternalSyntheticLambda0
            public final void acceptOrThrow(Object obj) {
                ((ITextClassifierService) obj).onDetectLanguage(sessionId, request, callback);
            }
        }, "onDetectLanguage", callback);
    }

    public void onSuggestConversationActions(final TextClassificationSessionId sessionId, final ConversationActions.Request request, final ITextClassifierCallback callback) throws RemoteException {
        Objects.requireNonNull(request);
        Objects.requireNonNull(request.getSystemTextClassifierMetadata());
        handleRequest(request.getSystemTextClassifierMetadata(), true, true, new FunctionalUtils.ThrowingConsumer() { // from class: com.android.server.textclassifier.TextClassificationManagerService$$ExternalSyntheticLambda1
            public final void acceptOrThrow(Object obj) {
                ((ITextClassifierService) obj).onSuggestConversationActions(sessionId, request, TextClassificationManagerService.wrap(callback));
            }
        }, "onSuggestConversationActions", callback);
    }

    public void onCreateTextClassificationSession(final TextClassificationContext classificationContext, final TextClassificationSessionId sessionId) throws RemoteException {
        Objects.requireNonNull(sessionId);
        Objects.requireNonNull(classificationContext);
        Objects.requireNonNull(classificationContext.getSystemTextClassifierMetadata());
        synchronized (this.mLock) {
            this.mSessionCache.put(sessionId, classificationContext);
        }
        handleRequest(classificationContext.getSystemTextClassifierMetadata(), true, false, new FunctionalUtils.ThrowingConsumer() { // from class: com.android.server.textclassifier.TextClassificationManagerService$$ExternalSyntheticLambda2
            public final void acceptOrThrow(Object obj) {
                ((ITextClassifierService) obj).onCreateTextClassificationSession(classificationContext, sessionId);
            }
        }, "onCreateTextClassificationSession", NO_OP_CALLBACK);
    }

    public void onDestroyTextClassificationSession(final TextClassificationSessionId sessionId) throws RemoteException {
        int userId;
        boolean useDefaultTextClassifier;
        Objects.requireNonNull(sessionId);
        synchronized (this.mLock) {
            StrippedTextClassificationContext textClassificationContext = this.mSessionCache.get(sessionId);
            if (textClassificationContext != null) {
                userId = textClassificationContext.userId;
            } else {
                userId = UserHandle.getCallingUserId();
            }
            if (textClassificationContext != null) {
                useDefaultTextClassifier = textClassificationContext.useDefaultTextClassifier;
            } else {
                useDefaultTextClassifier = true;
            }
            SystemTextClassifierMetadata sysTcMetadata = new SystemTextClassifierMetadata("", userId, useDefaultTextClassifier);
            handleRequest(sysTcMetadata, false, false, new FunctionalUtils.ThrowingConsumer() { // from class: com.android.server.textclassifier.TextClassificationManagerService$$ExternalSyntheticLambda6
                public final void acceptOrThrow(Object obj) {
                    TextClassificationManagerService.this.m6804x9c82ce65(sessionId, (ITextClassifierService) obj);
                }
            }, "onDestroyTextClassificationSession", NO_OP_CALLBACK);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onDestroyTextClassificationSession$8$com-android-server-textclassifier-TextClassificationManagerService  reason: not valid java name */
    public /* synthetic */ void m6804x9c82ce65(TextClassificationSessionId sessionId, ITextClassifierService service) throws Exception {
        service.onDestroyTextClassificationSession(sessionId);
        this.mSessionCache.m6819x599dac30(sessionId);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public UserState getUserStateLocked(int userId) {
        UserState result = this.mUserStates.get(userId);
        if (result == null) {
            UserState result2 = new UserState(userId);
            this.mUserStates.put(userId, result2);
            return result2;
        }
        return result;
    }

    UserState peekUserStateLocked(int userId) {
        return this.mUserStates.get(userId);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int resolvePackageToUid(String packageName, int userId) {
        if (packageName == null) {
            return -1;
        }
        PackageManager pm = this.mContext.getPackageManager();
        try {
            return pm.getPackageUidAsUser(packageName, userId);
        } catch (PackageManager.NameNotFoundException e) {
            Slog.e(LOG_TAG, "Could not get the UID for " + packageName);
            return -1;
        }
    }

    protected void dump(FileDescriptor fd, PrintWriter fout, String[] args) {
        if (DumpUtils.checkDumpPermission(this.mContext, LOG_TAG, fout)) {
            final IndentingPrintWriter pw = new IndentingPrintWriter(fout, "  ");
            Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.textclassifier.TextClassificationManagerService$$ExternalSyntheticLambda9
                public final void runOrThrow() {
                    TextClassificationManagerService.this.m6803xbc422f58(pw);
                }
            });
            pw.printPair("context", this.mContext);
            pw.println();
            pw.printPair("defaultTextClassifierPackage", this.mDefaultTextClassifierPackage);
            pw.println();
            pw.printPair("systemTextClassifierPackage", this.mSystemTextClassifierPackage);
            pw.println();
            synchronized (this.mLock) {
                int size = this.mUserStates.size();
                pw.print("Number user states: ");
                pw.println(size);
                if (size > 0) {
                    for (int i = 0; i < size; i++) {
                        pw.increaseIndent();
                        UserState userState = this.mUserStates.valueAt(i);
                        pw.printPair("User", Integer.valueOf(this.mUserStates.keyAt(i)));
                        pw.println();
                        userState.dump(pw);
                        pw.decreaseIndent();
                    }
                }
                pw.println("Number of active sessions: " + this.mSessionCache.size());
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$dump$9$com-android-server-textclassifier-TextClassificationManagerService  reason: not valid java name */
    public /* synthetic */ void m6803xbc422f58(IndentingPrintWriter pw) throws Exception {
        ((TextClassificationManager) this.mContext.getSystemService(TextClassificationManager.class)).dump(pw);
    }

    private void handleRequest(SystemTextClassifierMetadata sysTcMetadata, boolean verifyCallingPackage, boolean attemptToBind, final FunctionalUtils.ThrowingConsumer<ITextClassifierService> textClassifierServiceConsumer, String methodName, final ITextClassifierCallback callback) throws RemoteException {
        Objects.requireNonNull(textClassifierServiceConsumer);
        Objects.requireNonNull(methodName);
        Objects.requireNonNull(callback);
        int userId = sysTcMetadata == null ? UserHandle.getCallingUserId() : sysTcMetadata.getUserId();
        String callingPackageName = sysTcMetadata == null ? null : sysTcMetadata.getCallingPackageName();
        boolean useDefaultTextClassifier = sysTcMetadata == null ? true : sysTcMetadata.useDefaultTextClassifier();
        if (verifyCallingPackage) {
            try {
                validateCallingPackage(callingPackageName);
            } catch (Exception e) {
                throw new RemoteException("Invalid request: " + e.getMessage(), e, true, true);
            }
        }
        validateUser(userId);
        synchronized (this.mLock) {
            UserState userState = getUserStateLocked(userId);
            final ServiceState serviceState = userState.getServiceStateLocked(useDefaultTextClassifier);
            if (serviceState == null) {
                Slog.d(LOG_TAG, "No configured system TextClassifierService");
                callback.onFailure();
            } else {
                if (serviceState.isInstalledLocked() && serviceState.isEnabledLocked()) {
                    if (attemptToBind && !serviceState.bindLocked()) {
                        Slog.d(LOG_TAG, "Unable to bind TextClassifierService at " + methodName);
                        callback.onFailure();
                    } else if (serviceState.isBoundLocked()) {
                        if (!serviceState.checkRequestAcceptedLocked(Binder.getCallingUid(), methodName)) {
                            Slog.w(LOG_TAG, String.format("UID %d is not allowed to see the %s request", Integer.valueOf(Binder.getCallingUid()), methodName));
                            callback.onFailure();
                            return;
                        }
                        consumeServiceNoExceptLocked(textClassifierServiceConsumer, serviceState.mService);
                    } else {
                        FixedSizeQueue<PendingRequest> fixedSizeQueue = serviceState.mPendingRequests;
                        FunctionalUtils.ThrowingRunnable throwingRunnable = new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.textclassifier.TextClassificationManagerService$$ExternalSyntheticLambda10
                            public final void runOrThrow() {
                                TextClassificationManagerService.consumeServiceNoExceptLocked(textClassifierServiceConsumer, serviceState.mService);
                            }
                        };
                        Objects.requireNonNull(callback);
                        fixedSizeQueue.add(new PendingRequest(methodName, throwingRunnable, new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.textclassifier.TextClassificationManagerService$$ExternalSyntheticLambda11
                            public final void runOrThrow() {
                                callback.onFailure();
                            }
                        }, callback.asBinder(), this, serviceState, Binder.getCallingUid()));
                    }
                }
                callback.onFailure();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void consumeServiceNoExceptLocked(FunctionalUtils.ThrowingConsumer<ITextClassifierService> textClassifierServiceConsumer, ITextClassifierService service) {
        try {
            textClassifierServiceConsumer.accept(service);
        } catch (Error | RuntimeException e) {
            Slog.e(LOG_TAG, "Exception when consume textClassifierService: " + e);
        }
    }

    private static ITextClassifierCallback wrap(ITextClassifierCallback orig) {
        return new CallbackWrapper(orig);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onTextClassifierServicePackageOverrideChanged(String overriddenPackage) {
        synchronized (this.mLock) {
            int size = this.mUserStates.size();
            for (int i = 0; i < size; i++) {
                UserState userState = this.mUserStates.valueAt(i);
                userState.onTextClassifierServicePackageOverrideChangedLocked(overriddenPackage);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static final class PendingRequest implements IBinder.DeathRecipient {
        private final IBinder mBinder;
        private final String mName;
        private final Runnable mOnServiceFailure;
        private final Runnable mRequest;
        private final TextClassificationManagerService mService;
        private final ServiceState mServiceState;
        private final int mUid;

        PendingRequest(String name, FunctionalUtils.ThrowingRunnable request, FunctionalUtils.ThrowingRunnable onServiceFailure, IBinder binder, TextClassificationManagerService service, ServiceState serviceState, int uid) {
            this.mName = name;
            this.mRequest = TextClassificationManagerService.logOnFailure((FunctionalUtils.ThrowingRunnable) Objects.requireNonNull(request), "handling pending request");
            this.mOnServiceFailure = TextClassificationManagerService.logOnFailure((FunctionalUtils.ThrowingRunnable) Objects.requireNonNull(onServiceFailure), "notifying callback of service failure");
            this.mBinder = binder;
            this.mService = service;
            this.mServiceState = (ServiceState) Objects.requireNonNull(serviceState);
            if (binder != null) {
                try {
                    binder.linkToDeath(this, 0);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            this.mUid = uid;
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            synchronized (this.mService.mLock) {
                removeLocked();
            }
        }

        private void removeLocked() {
            this.mServiceState.mPendingRequests.remove(this);
            IBinder iBinder = this.mBinder;
            if (iBinder != null) {
                iBinder.unlinkToDeath(this, 0);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static Runnable logOnFailure(FunctionalUtils.ThrowingRunnable r, final String opDesc) {
        if (r == null) {
            return null;
        }
        return FunctionalUtils.handleExceptions(r, new Consumer() { // from class: com.android.server.textclassifier.TextClassificationManagerService$$ExternalSyntheticLambda3
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                Throwable th = (Throwable) obj;
                Slog.d(TextClassificationManagerService.LOG_TAG, "Error " + opDesc + ": " + th.getMessage());
            }
        });
    }

    private void validateCallingPackage(String callingPackage) throws PackageManager.NameNotFoundException {
        if (callingPackage != null) {
            int packageUid = this.mContext.getPackageManager().getPackageUidAsUser(callingPackage, UserHandle.getCallingUserId());
            int callingUid = Binder.getCallingUid();
            Preconditions.checkArgument(callingUid == packageUid || callingUid == 1000, "Invalid package name. callingPackage=" + callingPackage + ", callingUid=" + callingUid);
        }
    }

    private void validateUser(int userId) {
        Preconditions.checkArgument(userId != -10000, "Null userId");
        int callingUserId = UserHandle.getCallingUserId();
        if (callingUserId != userId) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL", "Invalid userId. UserId=" + userId + ", CallingUserId=" + callingUserId);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static final class SessionCache {
        private static final int MAX_CACHE_SIZE = 100;
        private final LruCache<TextClassificationSessionId, StrippedTextClassificationContext> mCache = new LruCache<>(100);
        private final Map<TextClassificationSessionId, IBinder.DeathRecipient> mDeathRecipients = new ArrayMap();
        private final Object mLock;

        SessionCache(Object lock) {
            this.mLock = Objects.requireNonNull(lock);
        }

        void put(final TextClassificationSessionId sessionId, TextClassificationContext textClassificationContext) {
            synchronized (this.mLock) {
                this.mCache.put(sessionId, new StrippedTextClassificationContext(textClassificationContext));
                try {
                    IBinder.DeathRecipient deathRecipient = new IBinder.DeathRecipient() { // from class: com.android.server.textclassifier.TextClassificationManagerService$SessionCache$$ExternalSyntheticLambda0
                        @Override // android.os.IBinder.DeathRecipient
                        public final void binderDied() {
                            TextClassificationManagerService.SessionCache.this.m6819x599dac30(sessionId);
                        }
                    };
                    sessionId.getToken().linkToDeath(deathRecipient, 0);
                    this.mDeathRecipients.put(sessionId, deathRecipient);
                } catch (RemoteException e) {
                    Slog.w(TextClassificationManagerService.LOG_TAG, "SessionCache: Failed to link to death", e);
                }
            }
        }

        StrippedTextClassificationContext get(TextClassificationSessionId sessionId) {
            StrippedTextClassificationContext strippedTextClassificationContext;
            Objects.requireNonNull(sessionId);
            synchronized (this.mLock) {
                strippedTextClassificationContext = this.mCache.get(sessionId);
            }
            return strippedTextClassificationContext;
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: remove */
        public void m6819x599dac30(TextClassificationSessionId sessionId) {
            Objects.requireNonNull(sessionId);
            synchronized (this.mLock) {
                IBinder.DeathRecipient deathRecipient = this.mDeathRecipients.get(sessionId);
                if (deathRecipient != null) {
                    sessionId.getToken().unlinkToDeath(deathRecipient, 0);
                }
                this.mDeathRecipients.remove(sessionId);
                this.mCache.remove(sessionId);
            }
        }

        int size() {
            int size;
            synchronized (this.mLock) {
                size = this.mCache.size();
            }
            return size;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static class StrippedTextClassificationContext {
        public final boolean useDefaultTextClassifier;
        public final int userId;

        StrippedTextClassificationContext(TextClassificationContext textClassificationContext) {
            SystemTextClassifierMetadata sysTcMetadata = textClassificationContext.getSystemTextClassifierMetadata();
            this.userId = sysTcMetadata.getUserId();
            this.useDefaultTextClassifier = sysTcMetadata.useDefaultTextClassifier();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public final class UserState {
        private final ServiceState mDefaultServiceState;
        private final ServiceState mSystemServiceState;
        private ServiceState mUntrustedServiceState;
        final int mUserId;

        private UserState(int userId) {
            ServiceState serviceState;
            this.mUserId = userId;
            if (TextUtils.isEmpty(TextClassificationManagerService.this.mDefaultTextClassifierPackage)) {
                serviceState = null;
            } else {
                serviceState = new ServiceState(userId, TextClassificationManagerService.this.mDefaultTextClassifierPackage, true);
            }
            this.mDefaultServiceState = serviceState;
            this.mSystemServiceState = TextUtils.isEmpty(TextClassificationManagerService.this.mSystemTextClassifierPackage) ? null : new ServiceState(userId, TextClassificationManagerService.this.mSystemTextClassifierPackage, true);
        }

        ServiceState getServiceStateLocked(boolean useDefaultTextClassifier) {
            ServiceState serviceState;
            if (useDefaultTextClassifier) {
                return this.mDefaultServiceState;
            }
            final TextClassificationConstants textClassificationConstants = TextClassificationManagerService.this.mSettings;
            Objects.requireNonNull(textClassificationConstants);
            String textClassifierServicePackageOverride = (String) Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.textclassifier.TextClassificationManagerService$UserState$$ExternalSyntheticLambda0
                public final Object getOrThrow() {
                    return textClassificationConstants.getTextClassifierServicePackageOverride();
                }
            });
            if (!TextUtils.isEmpty(textClassifierServicePackageOverride)) {
                if (textClassifierServicePackageOverride.equals(TextClassificationManagerService.this.mDefaultTextClassifierPackage)) {
                    return this.mDefaultServiceState;
                }
                if (textClassifierServicePackageOverride.equals(TextClassificationManagerService.this.mSystemTextClassifierPackage) && (serviceState = this.mSystemServiceState) != null) {
                    return serviceState;
                }
                if (this.mUntrustedServiceState == null) {
                    this.mUntrustedServiceState = new ServiceState(this.mUserId, textClassifierServicePackageOverride, false);
                }
                return this.mUntrustedServiceState;
            }
            ServiceState serviceState2 = this.mSystemServiceState;
            return serviceState2 != null ? serviceState2 : this.mDefaultServiceState;
        }

        void onTextClassifierServicePackageOverrideChangedLocked(String overriddenPackageName) {
            for (ServiceState serviceState : getAllServiceStatesLocked()) {
                serviceState.unbindIfBoundLocked();
            }
            this.mUntrustedServiceState = null;
        }

        void bindIfHasPendingRequestsLocked() {
            for (ServiceState serviceState : getAllServiceStatesLocked()) {
                serviceState.bindIfHasPendingRequestsLocked();
            }
        }

        void cleanupServiceLocked() {
            for (ServiceState serviceState : getAllServiceStatesLocked()) {
                if (serviceState.mConnection != null) {
                    serviceState.mConnection.cleanupService();
                }
            }
        }

        private List<ServiceState> getAllServiceStatesLocked() {
            List<ServiceState> serviceStates = new ArrayList<>();
            ServiceState serviceState = this.mDefaultServiceState;
            if (serviceState != null) {
                serviceStates.add(serviceState);
            }
            ServiceState serviceState2 = this.mSystemServiceState;
            if (serviceState2 != null) {
                serviceStates.add(serviceState2);
            }
            ServiceState serviceState3 = this.mUntrustedServiceState;
            if (serviceState3 != null) {
                serviceStates.add(serviceState3);
            }
            return serviceStates;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public ServiceState getServiceStateLocked(String packageName) {
            for (ServiceState serviceState : getAllServiceStatesLocked()) {
                if (serviceState.mPackageName.equals(packageName)) {
                    return serviceState;
                }
            }
            return null;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void updatePackageStateLocked() {
            for (ServiceState serviceState : getAllServiceStatesLocked()) {
                serviceState.updatePackageStateLocked();
            }
        }

        void dump(IndentingPrintWriter pw) {
            synchronized (TextClassificationManagerService.this.mLock) {
                pw.increaseIndent();
                dump(pw, this.mDefaultServiceState, "Default");
                dump(pw, this.mSystemServiceState, "System");
                dump(pw, this.mUntrustedServiceState, "Untrusted");
                pw.decreaseIndent();
            }
        }

        private void dump(IndentingPrintWriter pw, ServiceState serviceState, String name) {
            synchronized (TextClassificationManagerService.this.mLock) {
                if (serviceState != null) {
                    pw.print(name + ": ");
                    serviceState.dump(pw);
                    pw.println();
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public final class ServiceState {
        private static final int MAX_PENDING_REQUESTS = 20;
        final int mBindServiceFlags;
        boolean mBinding;
        ComponentName mBoundComponentName;
        int mBoundServiceUid;
        final TextClassifierServiceConnection mConnection;
        boolean mEnabled;
        boolean mInstalled;
        final boolean mIsTrusted;
        final String mPackageName;
        final FixedSizeQueue<PendingRequest> mPendingRequests;
        ITextClassifierService mService;
        final int mUserId;

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ void lambda$new$0(PendingRequest request) {
            Slog.w(TextClassificationManagerService.LOG_TAG, String.format("Pending request[%s] is dropped", request.mName));
            request.mOnServiceFailure.run();
        }

        private ServiceState(int userId, String packageName, boolean isTrusted) {
            this.mPendingRequests = new FixedSizeQueue<>(20, new FixedSizeQueue.OnEntryEvictedListener() { // from class: com.android.server.textclassifier.TextClassificationManagerService$ServiceState$$ExternalSyntheticLambda0
                @Override // com.android.server.textclassifier.FixedSizeQueue.OnEntryEvictedListener
                public final void onEntryEvicted(Object obj) {
                    TextClassificationManagerService.ServiceState.lambda$new$0((TextClassificationManagerService.PendingRequest) obj);
                }
            });
            this.mBoundComponentName = null;
            this.mBoundServiceUid = -1;
            this.mUserId = userId;
            this.mPackageName = packageName;
            this.mConnection = new TextClassifierServiceConnection(userId);
            this.mIsTrusted = isTrusted;
            this.mBindServiceFlags = createBindServiceFlags(packageName);
            this.mInstalled = isPackageInstalledForUser();
            this.mEnabled = isServiceEnabledForUser();
        }

        private int createBindServiceFlags(String packageName) {
            if (!packageName.equals(TextClassificationManagerService.this.mDefaultTextClassifierPackage)) {
                int flags = 67108865 | 2097152;
                return flags;
            }
            return AudioFormat.AAC_MAIN;
        }

        private boolean isPackageInstalledForUser() {
            try {
                PackageManager packageManager = TextClassificationManagerService.this.mContext.getPackageManager();
                return packageManager.getPackageInfoAsUser(this.mPackageName, 0, this.mUserId) != null;
            } catch (PackageManager.NameNotFoundException e) {
                return false;
            }
        }

        private boolean isServiceEnabledForUser() {
            PackageManager packageManager = TextClassificationManagerService.this.mContext.getPackageManager();
            Intent intent = new Intent("android.service.textclassifier.TextClassifierService");
            intent.setPackage(this.mPackageName);
            ResolveInfo resolveInfo = packageManager.resolveServiceAsUser(intent, 4, this.mUserId);
            ServiceInfo serviceInfo = resolveInfo == null ? null : resolveInfo.serviceInfo;
            return serviceInfo != null;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void onPackageInstallStatusChangeLocked(boolean installed) {
            this.mInstalled = installed;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void onPackageModifiedLocked() {
            this.mEnabled = isServiceEnabledForUser();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void updatePackageStateLocked() {
            this.mInstalled = isPackageInstalledForUser();
            this.mEnabled = isServiceEnabledForUser();
        }

        boolean isInstalledLocked() {
            return this.mInstalled;
        }

        boolean isEnabledLocked() {
            return this.mEnabled;
        }

        boolean isBoundLocked() {
            return this.mService != null;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void handlePendingRequestsLocked() {
            while (true) {
                PendingRequest request = this.mPendingRequests.poll();
                if (request != null) {
                    if (isBoundLocked()) {
                        if (!checkRequestAcceptedLocked(request.mUid, request.mName)) {
                            Slog.w(TextClassificationManagerService.LOG_TAG, String.format("UID %d is not allowed to see the %s request", Integer.valueOf(request.mUid), request.mName));
                            request.mOnServiceFailure.run();
                        } else {
                            request.mRequest.run();
                        }
                    } else {
                        Slog.d(TextClassificationManagerService.LOG_TAG, "Unable to bind TextClassifierService for PendingRequest " + request.mName);
                        request.mOnServiceFailure.run();
                    }
                    if (request.mBinder != null) {
                        request.mBinder.unlinkToDeath(request, 0);
                    }
                } else {
                    return;
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public boolean bindIfHasPendingRequestsLocked() {
            return !this.mPendingRequests.isEmpty() && bindLocked();
        }

        void unbindIfBoundLocked() {
            if (isBoundLocked()) {
                Slog.v(TextClassificationManagerService.LOG_TAG, "Unbinding " + this.mBoundComponentName + " for " + this.mUserId);
                TextClassificationManagerService.this.mContext.unbindService(this.mConnection);
                this.mConnection.cleanupService();
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public boolean bindLocked() {
            if (isBoundLocked() || this.mBinding) {
                return true;
            }
            long identity = Binder.clearCallingIdentity();
            try {
                ComponentName componentName = getTextClassifierServiceComponent();
                if (componentName != null) {
                    Intent serviceIntent = new Intent("android.service.textclassifier.TextClassifierService").setComponent(componentName);
                    Slog.d(TextClassificationManagerService.LOG_TAG, "Binding to " + serviceIntent.getComponent());
                    boolean willBind = TextClassificationManagerService.this.mContext.bindServiceAsUser(serviceIntent, this.mConnection, this.mBindServiceFlags, UserHandle.of(this.mUserId));
                    if (!willBind) {
                        Slog.e(TextClassificationManagerService.LOG_TAG, "Could not bind to " + componentName);
                    }
                    this.mBinding = willBind;
                    return willBind;
                }
                return false;
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        private ComponentName getTextClassifierServiceComponent() {
            return TextClassifierService.getServiceComponentName(TextClassificationManagerService.this.mContext, this.mPackageName, this.mIsTrusted ? 1048576 : 0);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void dump(IndentingPrintWriter pw) {
            pw.printPair("context", TextClassificationManagerService.this.mContext);
            pw.printPair("userId", Integer.valueOf(this.mUserId));
            synchronized (TextClassificationManagerService.this.mLock) {
                pw.printPair(DomainVerificationLegacySettings.ATTR_PACKAGE_NAME, this.mPackageName);
                pw.printPair("installed", Boolean.valueOf(this.mInstalled));
                pw.printPair(ServiceConfigAccessor.PROVIDER_MODE_ENABLED, Boolean.valueOf(this.mEnabled));
                pw.printPair("boundComponentName", this.mBoundComponentName);
                pw.printPair("isTrusted", Boolean.valueOf(this.mIsTrusted));
                pw.printPair("bindServiceFlags", Integer.valueOf(this.mBindServiceFlags));
                pw.printPair("boundServiceUid", Integer.valueOf(this.mBoundServiceUid));
                pw.printPair("binding", Boolean.valueOf(this.mBinding));
                pw.printPair("numOfPendingRequests", Integer.valueOf(this.mPendingRequests.size()));
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public boolean checkRequestAcceptedLocked(int requestUid, String methodName) {
            if (this.mIsTrusted || requestUid == this.mBoundServiceUid) {
                return true;
            }
            Slog.w(TextClassificationManagerService.LOG_TAG, String.format("[%s] Non-default TextClassifierServices may only see text from the same uid.", methodName));
            return false;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void updateServiceInfoLocked(int userId, ComponentName componentName) {
            int resolvePackageToUid;
            this.mBoundComponentName = componentName;
            if (componentName == null) {
                resolvePackageToUid = -1;
            } else {
                resolvePackageToUid = TextClassificationManagerService.this.resolvePackageToUid(componentName.getPackageName(), userId);
            }
            this.mBoundServiceUid = resolvePackageToUid;
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes2.dex */
        public final class TextClassifierServiceConnection implements ServiceConnection {
            private final int mUserId;

            TextClassifierServiceConnection(int userId) {
                this.mUserId = userId;
            }

            @Override // android.content.ServiceConnection
            public void onServiceConnected(ComponentName name, IBinder service) {
                ITextClassifierService tcService = ITextClassifierService.Stub.asInterface(service);
                try {
                    tcService.onConnectedStateChanged(0);
                } catch (RemoteException e) {
                    Slog.e(TextClassificationManagerService.LOG_TAG, "error in onConnectedStateChanged");
                }
                init(tcService, name);
            }

            @Override // android.content.ServiceConnection
            public void onServiceDisconnected(ComponentName name) {
                Slog.i(TextClassificationManagerService.LOG_TAG, "onServiceDisconnected called with " + name);
                cleanupService();
            }

            @Override // android.content.ServiceConnection
            public void onBindingDied(ComponentName name) {
                Slog.i(TextClassificationManagerService.LOG_TAG, "onBindingDied called with " + name);
                cleanupService();
            }

            @Override // android.content.ServiceConnection
            public void onNullBinding(ComponentName name) {
                Slog.i(TextClassificationManagerService.LOG_TAG, "onNullBinding called with " + name);
                cleanupService();
            }

            void cleanupService() {
                init(null, null);
            }

            private void init(ITextClassifierService service, ComponentName name) {
                synchronized (TextClassificationManagerService.this.mLock) {
                    ServiceState.this.mService = service;
                    ServiceState.this.mBinding = false;
                    ServiceState.this.updateServiceInfoLocked(this.mUserId, name);
                    ServiceState.this.handlePendingRequestsLocked();
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public final class TextClassifierSettingsListener implements DeviceConfig.OnPropertiesChangedListener {
        private final Context mContext;
        private String mServicePackageOverride;

        TextClassifierSettingsListener(Context context) {
            this.mContext = context;
            this.mServicePackageOverride = TextClassificationManagerService.this.mSettings.getTextClassifierServicePackageOverride();
        }

        void registerObserver() {
            DeviceConfig.addOnPropertiesChangedListener("textclassifier", this.mContext.getMainExecutor(), this);
        }

        public void onPropertiesChanged(DeviceConfig.Properties properties) {
            String currentServicePackageOverride = TextClassificationManagerService.this.mSettings.getTextClassifierServicePackageOverride();
            if (TextUtils.equals(currentServicePackageOverride, this.mServicePackageOverride)) {
                return;
            }
            this.mServicePackageOverride = currentServicePackageOverride;
            TextClassificationManagerService.this.onTextClassifierServicePackageOverrideChanged(currentServicePackageOverride);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static final class CallbackWrapper extends ITextClassifierCallback.Stub {
        private final ITextClassifierCallback mWrapped;

        CallbackWrapper(ITextClassifierCallback wrapped) {
            this.mWrapped = (ITextClassifierCallback) Objects.requireNonNull(wrapped);
        }

        public void onSuccess(Bundle result) {
            Parcelable parcelled = TextClassifierService.getResponse(result);
            if (parcelled instanceof TextClassification) {
                rewriteTextClassificationIcons(result);
            } else if (parcelled instanceof ConversationActions) {
                rewriteConversationActionsIcons(result);
            } else if (parcelled instanceof TextSelection) {
                rewriteTextSelectionIcons(result);
            }
            try {
                this.mWrapped.onSuccess(result);
            } catch (RemoteException e) {
                Slog.e(TextClassificationManagerService.LOG_TAG, "Callback error", e);
            }
        }

        private static void rewriteTextSelectionIcons(Bundle result) {
            TextClassification newTextClassification;
            TextSelection textSelection = (TextSelection) TextClassifierService.getResponse(result);
            if (textSelection.getTextClassification() == null || (newTextClassification = rewriteTextClassificationIcons(textSelection.getTextClassification())) == null) {
                return;
            }
            TextClassifierService.putResponse(result, textSelection.toBuilder().setTextClassification(newTextClassification).build());
        }

        private static TextClassification rewriteTextClassificationIcons(TextClassification textClassification) {
            RemoteAction validAction;
            boolean rewrite = false;
            List<RemoteAction> actions = textClassification.getActions();
            int size = actions.size();
            List<RemoteAction> validActions = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                RemoteAction action = actions.get(i);
                if (shouldRewriteIcon(action)) {
                    rewrite = true;
                    validAction = validAction(action);
                } else {
                    validAction = action;
                }
                validActions.add(validAction);
            }
            if (rewrite) {
                return textClassification.toBuilder().clearActions().addActions(validActions).build();
            }
            return null;
        }

        private static void rewriteTextClassificationIcons(Bundle result) {
            TextClassification classification = (TextClassification) TextClassifierService.getResponse(result);
            TextClassification newTextClassification = rewriteTextClassificationIcons(classification);
            if (newTextClassification != null) {
                TextClassifierService.putResponse(result, newTextClassification);
            }
        }

        private static void rewriteConversationActionsIcons(Bundle result) {
            ConversationAction validConvAction;
            ConversationActions convActions = (ConversationActions) TextClassifierService.getResponse(result);
            boolean rewrite = false;
            List<ConversationAction> origConvActions = convActions.getConversationActions();
            int size = origConvActions.size();
            List<ConversationAction> validConvActions = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                ConversationAction convAction = origConvActions.get(i);
                if (shouldRewriteIcon(convAction.getAction())) {
                    rewrite = true;
                    validConvAction = convAction.toBuilder().setAction(validAction(convAction.getAction())).build();
                } else {
                    validConvAction = convAction;
                }
                validConvActions.add(validConvAction);
            }
            if (rewrite) {
                TextClassifierService.putResponse(result, new ConversationActions(validConvActions, convActions.getId()));
            }
        }

        private static RemoteAction validAction(RemoteAction action) {
            RemoteAction newAction = new RemoteAction(changeIcon(action.getIcon()), action.getTitle(), action.getContentDescription(), action.getActionIntent());
            newAction.setEnabled(action.isEnabled());
            newAction.setShouldShowIcon(action.shouldShowIcon());
            return newAction;
        }

        private static boolean shouldRewriteIcon(RemoteAction action) {
            return action != null && action.getIcon().getType() == 2;
        }

        private static Icon changeIcon(Icon icon) {
            Uri uri = IconsUriHelper.getInstance().getContentUri(icon.getResPackage(), icon.getResId());
            return Icon.createWithContentUri(uri);
        }

        public void onFailure() {
            try {
                this.mWrapped.onFailure();
            } catch (RemoteException e) {
                Slog.e(TextClassificationManagerService.LOG_TAG, "Callback error", e);
            }
        }
    }
}
