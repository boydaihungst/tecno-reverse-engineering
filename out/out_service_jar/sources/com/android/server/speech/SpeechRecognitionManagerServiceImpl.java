package com.android.server.speech;

import android.app.AppGlobals;
import android.content.AttributionSource;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.permission.PermissionManager;
import android.speech.IRecognitionListener;
import android.speech.IRecognitionService;
import android.speech.IRecognitionServiceManagerCallback;
import android.speech.IRecognitionSupportCallback;
import android.util.Slog;
import com.android.server.infra.AbstractPerUserSystemService;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public final class SpeechRecognitionManagerServiceImpl extends AbstractPerUserSystemService<SpeechRecognitionManagerServiceImpl, SpeechRecognitionManagerService> {
    private static final int MAX_CONCURRENT_CONNECTIONS_BY_CLIENT = 10;
    private static final String TAG = SpeechRecognitionManagerServiceImpl.class.getSimpleName();
    private final Object mLock;
    private final Map<Integer, Set<RemoteSpeechRecognitionService>> mRemoteServicesByUid;

    /* JADX INFO: Access modifiers changed from: package-private */
    public SpeechRecognitionManagerServiceImpl(SpeechRecognitionManagerService master, Object lock, int userId) {
        super(master, lock, userId);
        this.mLock = new Object();
        this.mRemoteServicesByUid = new HashMap();
    }

    @Override // com.android.server.infra.AbstractPerUserSystemService
    protected ServiceInfo newServiceInfoLocked(ComponentName serviceComponent) throws PackageManager.NameNotFoundException {
        try {
            return AppGlobals.getPackageManager().getServiceInfo(serviceComponent, 128L, this.mUserId);
        } catch (RemoteException e) {
            throw new PackageManager.NameNotFoundException("Could not get service for " + serviceComponent);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.infra.AbstractPerUserSystemService
    public boolean updateLocked(boolean disabled) {
        boolean enabledChanged = super.updateLocked(disabled);
        return enabledChanged;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void createSessionLocked(ComponentName componentName, final IBinder clientToken, boolean onDevice, final IRecognitionServiceManagerCallback callback) {
        ComponentName serviceComponent;
        if (((SpeechRecognitionManagerService) this.mMaster).debug) {
            Slog.i(TAG, String.format("#createSessionLocked, component=%s, onDevice=%s", componentName, Boolean.valueOf(onDevice)));
        }
        if (!onDevice) {
            serviceComponent = componentName;
        } else {
            ComponentName serviceComponent2 = getOnDeviceComponentNameLocked();
            serviceComponent = serviceComponent2;
        }
        if (serviceComponent == null) {
            if (((SpeechRecognitionManagerService) this.mMaster).debug) {
                Slog.i(TAG, "Service component is undefined, responding with error.");
            }
            tryRespondWithError(callback, 5);
            return;
        }
        final int creatorCallingUid = Binder.getCallingUid();
        final RemoteSpeechRecognitionService service = createService(creatorCallingUid, serviceComponent);
        if (service != null) {
            final IBinder.DeathRecipient deathRecipient = new IBinder.DeathRecipient() { // from class: com.android.server.speech.SpeechRecognitionManagerServiceImpl$$ExternalSyntheticLambda0
                @Override // android.os.IBinder.DeathRecipient
                public final void binderDied() {
                    SpeechRecognitionManagerServiceImpl.this.m6580x86fa21(creatorCallingUid, service);
                }
            };
            try {
                clientToken.linkToDeath(deathRecipient, 0);
                service.connect().thenAccept(new Consumer() { // from class: com.android.server.speech.SpeechRecognitionManagerServiceImpl$$ExternalSyntheticLambda1
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        SpeechRecognitionManagerServiceImpl.this.m6581xf4167e62(callback, service, creatorCallingUid, clientToken, deathRecipient, (IRecognitionService) obj);
                    }
                });
                return;
            } catch (RemoteException e) {
                handleClientDeath(creatorCallingUid, service, true);
                return;
            }
        }
        tryRespondWithError(callback, 10);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$createSessionLocked$0$com-android-server-speech-SpeechRecognitionManagerServiceImpl  reason: not valid java name */
    public /* synthetic */ void m6580x86fa21(int creatorCallingUid, RemoteSpeechRecognitionService service) {
        handleClientDeath(creatorCallingUid, service, true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$createSessionLocked$1$com-android-server-speech-SpeechRecognitionManagerServiceImpl  reason: not valid java name */
    public /* synthetic */ void m6581xf4167e62(IRecognitionServiceManagerCallback callback, final RemoteSpeechRecognitionService service, final int creatorCallingUid, final IBinder clientToken, final IBinder.DeathRecipient deathRecipient, IRecognitionService binderService) {
        if (binderService != null) {
            try {
                callback.onSuccess(new IRecognitionService.Stub() { // from class: com.android.server.speech.SpeechRecognitionManagerServiceImpl.1
                    public void startListening(Intent recognizerIntent, IRecognitionListener listener, AttributionSource attributionSource) throws RemoteException {
                        attributionSource.enforceCallingUid();
                        if (!attributionSource.isTrusted(((SpeechRecognitionManagerService) SpeechRecognitionManagerServiceImpl.this.mMaster).getContext())) {
                            attributionSource = ((PermissionManager) ((SpeechRecognitionManagerService) SpeechRecognitionManagerServiceImpl.this.mMaster).getContext().getSystemService(PermissionManager.class)).registerAttributionSource(attributionSource);
                        }
                        service.startListening(recognizerIntent, listener, attributionSource);
                    }

                    public void stopListening(IRecognitionListener listener) throws RemoteException {
                        service.stopListening(listener);
                    }

                    public void cancel(IRecognitionListener listener, boolean isShutdown) throws RemoteException {
                        service.cancel(listener, isShutdown);
                        if (isShutdown) {
                            SpeechRecognitionManagerServiceImpl.this.handleClientDeath(creatorCallingUid, service, false);
                            clientToken.unlinkToDeath(deathRecipient, 0);
                        }
                    }

                    public void checkRecognitionSupport(Intent recognizerIntent, IRecognitionSupportCallback callback2) {
                        service.checkRecognitionSupport(recognizerIntent, callback2);
                    }

                    public void triggerModelDownload(Intent recognizerIntent) {
                        service.triggerModelDownload(recognizerIntent);
                    }
                });
                return;
            } catch (RemoteException e) {
                Slog.e(TAG, "Error creating a speech recognition session", e);
                tryRespondWithError(callback, 5);
                return;
            }
        }
        tryRespondWithError(callback, 5);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleClientDeath(int callingUid, RemoteSpeechRecognitionService service, boolean invokeCancel) {
        if (invokeCancel) {
            service.shutdown();
        }
        removeService(callingUid, service);
    }

    private ComponentName getOnDeviceComponentNameLocked() {
        String serviceName = getComponentNameLocked();
        if (((SpeechRecognitionManagerService) this.mMaster).debug) {
            Slog.i(TAG, "Resolved component name: " + serviceName);
        }
        if (serviceName == null) {
            if (((SpeechRecognitionManagerService) this.mMaster).verbose) {
                Slog.v(TAG, "ensureRemoteServiceLocked(): no service component name.");
                return null;
            }
            return null;
        }
        return ComponentName.unflattenFromString(serviceName);
    }

    private RemoteSpeechRecognitionService createService(int callingUid, final ComponentName serviceComponent) {
        synchronized (this.mLock) {
            Set<RemoteSpeechRecognitionService> servicesForClient = this.mRemoteServicesByUid.get(Integer.valueOf(callingUid));
            if (servicesForClient != null && servicesForClient.size() >= 10) {
                return null;
            }
            if (servicesForClient != null) {
                Optional<RemoteSpeechRecognitionService> existingService = servicesForClient.stream().filter(new Predicate() { // from class: com.android.server.speech.SpeechRecognitionManagerServiceImpl$$ExternalSyntheticLambda2
                    @Override // java.util.function.Predicate
                    public final boolean test(Object obj) {
                        boolean equals;
                        equals = ((RemoteSpeechRecognitionService) obj).getServiceComponentName().equals(serviceComponent);
                        return equals;
                    }
                }).findFirst();
                if (existingService.isPresent()) {
                    if (((SpeechRecognitionManagerService) this.mMaster).debug) {
                        Slog.i(TAG, "Reused existing connection to " + serviceComponent);
                    }
                    return existingService.get();
                }
            }
            if (serviceComponent != null && !componentMapsToRecognitionService(serviceComponent)) {
                return null;
            }
            RemoteSpeechRecognitionService service = new RemoteSpeechRecognitionService(getContext(), serviceComponent, getUserId(), callingUid);
            Set<RemoteSpeechRecognitionService> valuesByCaller = this.mRemoteServicesByUid.computeIfAbsent(Integer.valueOf(callingUid), new Function() { // from class: com.android.server.speech.SpeechRecognitionManagerServiceImpl$$ExternalSyntheticLambda3
                @Override // java.util.function.Function
                public final Object apply(Object obj) {
                    return SpeechRecognitionManagerServiceImpl.lambda$createService$3((Integer) obj);
                }
            });
            valuesByCaller.add(service);
            if (((SpeechRecognitionManagerService) this.mMaster).debug) {
                Slog.i(TAG, "Creating a new connection to " + serviceComponent);
            }
            return service;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ Set lambda$createService$3(Integer key) {
        return new HashSet();
    }

    private boolean componentMapsToRecognitionService(ComponentName serviceComponent) {
        long identityToken = Binder.clearCallingIdentity();
        try {
            List<ResolveInfo> resolveInfos = getContext().getPackageManager().queryIntentServicesAsUser(new Intent("android.speech.RecognitionService"), 0, getUserId());
            if (resolveInfos == null) {
                return false;
            }
            for (ResolveInfo ri : resolveInfos) {
                if (ri.serviceInfo != null && serviceComponent.equals(ri.serviceInfo.getComponentName())) {
                    return true;
                }
            }
            Slog.w(TAG, "serviceComponent is not RecognitionService: " + serviceComponent);
            return false;
        } finally {
            Binder.restoreCallingIdentity(identityToken);
        }
    }

    private void removeService(int callingUid, RemoteSpeechRecognitionService service) {
        synchronized (this.mLock) {
            Set<RemoteSpeechRecognitionService> valuesByCaller = this.mRemoteServicesByUid.get(Integer.valueOf(callingUid));
            if (valuesByCaller != null) {
                valuesByCaller.remove(service);
            }
        }
    }

    private static void tryRespondWithError(IRecognitionServiceManagerCallback callback, int errorCode) {
        try {
            callback.onError(errorCode);
        } catch (RemoteException e) {
            Slog.w(TAG, "Failed to respond with error");
        }
    }
}
