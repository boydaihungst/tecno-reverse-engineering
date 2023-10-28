package com.android.server.biometrics.log;

import android.content.Context;
import android.hardware.biometrics.IBiometricContextListener;
import android.hardware.biometrics.common.OperationContext;
import android.hardware.display.AmbientDisplayConfiguration;
import android.os.Handler;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Slog;
import com.android.internal.logging.InstanceId;
import com.android.internal.statusbar.ISessionListener;
import com.android.internal.statusbar.IStatusBarService;
import com.android.server.biometrics.log.BiometricContextProvider;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class BiometricContextProvider implements BiometricContext {
    private static final int SESSION_TYPES = 3;
    private static final String TAG = "BiometricContextProvider";
    private static BiometricContextProvider sInstance;
    private final AmbientDisplayConfiguration mAmbientDisplayConfiguration;
    private final Map<OperationContext, Consumer<OperationContext>> mSubscribers = new ConcurrentHashMap();
    private final Map<Integer, InstanceId> mSession = new ConcurrentHashMap();
    private boolean mIsDozing = false;

    /* JADX INFO: Access modifiers changed from: package-private */
    public static BiometricContextProvider defaultProvider(Context context) {
        synchronized (BiometricContextProvider.class) {
            if (sInstance == null) {
                try {
                    sInstance = new BiometricContextProvider(new AmbientDisplayConfiguration(context), IStatusBarService.Stub.asInterface(ServiceManager.getServiceOrThrow("statusbar")), null);
                } catch (ServiceManager.ServiceNotFoundException e) {
                    throw new IllegalStateException("Failed to find required service", e);
                }
            }
        }
        return sInstance;
    }

    BiometricContextProvider(AmbientDisplayConfiguration ambientDisplayConfiguration, IStatusBarService service, Handler handler) {
        this.mAmbientDisplayConfiguration = ambientDisplayConfiguration;
        try {
            service.setBiometicContextListener(new AnonymousClass1(handler));
            service.registerSessionListener(3, new ISessionListener.Stub() { // from class: com.android.server.biometrics.log.BiometricContextProvider.2
                public void onSessionStarted(int sessionType, InstanceId instance) {
                    BiometricContextProvider.this.mSession.put(Integer.valueOf(sessionType), instance);
                }

                public void onSessionEnded(int sessionType, InstanceId instance) {
                    InstanceId id = (InstanceId) BiometricContextProvider.this.mSession.remove(Integer.valueOf(sessionType));
                    if (id != null && instance != null && id.getId() != instance.getId()) {
                        Slog.w(BiometricContextProvider.TAG, "session id mismatch");
                    }
                }
            });
        } catch (RemoteException e) {
            Slog.e(TAG, "Unable to register biometric context listener", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.biometrics.log.BiometricContextProvider$1  reason: invalid class name */
    /* loaded from: classes.dex */
    public class AnonymousClass1 extends IBiometricContextListener.Stub {
        final /* synthetic */ Handler val$handler;

        AnonymousClass1(Handler handler) {
            this.val$handler = handler;
        }

        public void onDozeChanged(boolean isDozing) {
            BiometricContextProvider.this.mIsDozing = isDozing;
            notifyChanged();
        }

        private void notifyChanged() {
            Handler handler = this.val$handler;
            if (handler != null) {
                handler.post(new Runnable() { // from class: com.android.server.biometrics.log.BiometricContextProvider$1$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        BiometricContextProvider.AnonymousClass1.this.m2293xd413bba();
                    }
                });
            } else {
                BiometricContextProvider.this.notifySubscribers();
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$notifyChanged$0$com-android-server-biometrics-log-BiometricContextProvider$1  reason: not valid java name */
        public /* synthetic */ void m2293xd413bba() {
            BiometricContextProvider.this.notifySubscribers();
        }
    }

    @Override // com.android.server.biometrics.log.BiometricContext
    public OperationContext updateContext(OperationContext operationContext, boolean isCryptoOperation) {
        operationContext.isAod = isAod();
        operationContext.isCrypto = isCryptoOperation;
        setFirstSessionId(operationContext);
        return operationContext;
    }

    private void setFirstSessionId(OperationContext operationContext) {
        Integer sessionId = getKeyguardEntrySessionId();
        if (sessionId != null) {
            operationContext.id = sessionId.intValue();
            operationContext.reason = (byte) 2;
            return;
        }
        Integer sessionId2 = getBiometricPromptSessionId();
        if (sessionId2 != null) {
            operationContext.id = sessionId2.intValue();
            operationContext.reason = (byte) 1;
            return;
        }
        operationContext.id = 0;
        operationContext.reason = (byte) 0;
    }

    @Override // com.android.server.biometrics.log.BiometricContext
    public Integer getKeyguardEntrySessionId() {
        InstanceId id = this.mSession.get(1);
        if (id != null) {
            return Integer.valueOf(id.getId());
        }
        return null;
    }

    @Override // com.android.server.biometrics.log.BiometricContext
    public Integer getBiometricPromptSessionId() {
        InstanceId id = this.mSession.get(2);
        if (id != null) {
            return Integer.valueOf(id.getId());
        }
        return null;
    }

    @Override // com.android.server.biometrics.log.BiometricContext
    public boolean isAod() {
        return this.mIsDozing && this.mAmbientDisplayConfiguration.alwaysOnEnabled(-2);
    }

    @Override // com.android.server.biometrics.log.BiometricContext
    public void subscribe(OperationContext context, Consumer<OperationContext> consumer) {
        this.mSubscribers.put(context, consumer);
    }

    @Override // com.android.server.biometrics.log.BiometricContext
    public void unsubscribe(OperationContext context) {
        this.mSubscribers.remove(context);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifySubscribers() {
        this.mSubscribers.forEach(new BiConsumer() { // from class: com.android.server.biometrics.log.BiometricContextProvider$$ExternalSyntheticLambda0
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                BiometricContextProvider.this.m2292x22cd0c76((OperationContext) obj, (Consumer) obj2);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$notifySubscribers$0$com-android-server-biometrics-log-BiometricContextProvider  reason: not valid java name */
    public /* synthetic */ void m2292x22cd0c76(OperationContext context, Consumer consumer) {
        context.isAod = isAod();
        consumer.accept(context);
    }
}
