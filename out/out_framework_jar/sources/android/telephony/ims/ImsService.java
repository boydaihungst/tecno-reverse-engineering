package android.telephony.ims;

import android.annotation.SystemApi;
import android.app.PendingIntent$$ExternalSyntheticLambda1;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.telephony.ims.ImsService;
import android.telephony.ims.aidl.IImsConfig;
import android.telephony.ims.aidl.IImsMmTelFeature;
import android.telephony.ims.aidl.IImsRcsFeature;
import android.telephony.ims.aidl.IImsRegistration;
import android.telephony.ims.aidl.IImsServiceController;
import android.telephony.ims.aidl.IImsServiceControllerListener;
import android.telephony.ims.aidl.ISipTransport;
import android.telephony.ims.feature.ImsFeature;
import android.telephony.ims.feature.MmTelFeature;
import android.telephony.ims.feature.RcsFeature;
import android.telephony.ims.stub.ImsConfigImplBase;
import android.telephony.ims.stub.ImsFeatureConfiguration;
import android.telephony.ims.stub.ImsRegistrationImplBase;
import android.telephony.ims.stub.SipTransportImplBase;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import com.android.ims.internal.IImsFeatureStatusCallback;
import com.android.internal.telephony.util.TelephonyUtils;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
@SystemApi
/* loaded from: classes3.dex */
public class ImsService extends Service {
    public static final long CAPABILITY_EMERGENCY_OVER_MMTEL = 1;
    public static final long CAPABILITY_SIP_DELEGATE_CREATION = 2;
    private static final String LOG_TAG = "ImsService";
    public static final String SERVICE_INTERFACE = "android.telephony.ims.ImsService";
    private Executor mExecutor;
    private IImsServiceControllerListener mListener;
    public static final long CAPABILITY_MAX_INDEX = Long.numberOfTrailingZeros(2);
    private static final Map<Long, String> CAPABILITIES_LOG_MAP = new HashMap<Long, String>() { // from class: android.telephony.ims.ImsService.1
        {
            put(1L, "EMERGENCY_OVER_MMTEL");
            put(2L, "SIP_DELEGATE_CREATION");
        }
    };
    private final SparseArray<SparseArray<ImsFeature>> mFeaturesBySlot = new SparseArray<>();
    private final SparseArray<SparseBooleanArray> mCreateImsFeatureWithSlotIdFlagMap = new SparseArray<>();
    protected final IBinder mImsServiceController = new AnonymousClass2();

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes3.dex */
    public @interface ImsServiceCapability {
    }

    public ImsService() {
        Executor executor = getExecutor();
        this.mExecutor = executor;
        if (executor == null) {
            this.mExecutor = new PendingIntent$$ExternalSyntheticLambda1();
        }
    }

    /* loaded from: classes3.dex */
    public static class Listener extends IImsServiceControllerListener.Stub {
        @Override // android.telephony.ims.aidl.IImsServiceControllerListener
        public void onUpdateSupportedImsFeatures(ImsFeatureConfiguration c) {
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: android.telephony.ims.ImsService$2  reason: invalid class name */
    /* loaded from: classes3.dex */
    public class AnonymousClass2 extends IImsServiceController.Stub {
        AnonymousClass2() {
        }

        @Override // android.telephony.ims.aidl.IImsServiceController
        public void setListener(IImsServiceControllerListener l) {
            ImsService.this.mListener = l;
        }

        @Override // android.telephony.ims.aidl.IImsServiceController
        public IImsMmTelFeature createMmTelFeature(final int slotId, final int subId) {
            MmTelFeature f = (MmTelFeature) ImsService.this.getImsFeature(slotId, 1);
            if (f == null) {
                return (IImsMmTelFeature) executeMethodAsyncForResult(new Supplier() { // from class: android.telephony.ims.ImsService$2$$ExternalSyntheticLambda6
                    @Override // java.util.function.Supplier
                    public final Object get() {
                        return ImsService.AnonymousClass2.this.m4290lambda$createMmTelFeature$0$androidtelephonyimsImsService$2(slotId, subId);
                    }
                }, "createMmTelFeature");
            }
            return f.getBinder();
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$createMmTelFeature$0$android-telephony-ims-ImsService$2  reason: not valid java name */
        public /* synthetic */ IImsMmTelFeature m4290lambda$createMmTelFeature$0$androidtelephonyimsImsService$2(int slotId, int subId) {
            return ImsService.this.createMmTelFeatureInternal(slotId, subId);
        }

        @Override // android.telephony.ims.aidl.IImsServiceController
        public IImsMmTelFeature createEmergencyOnlyMmTelFeature(final int slotId) {
            MmTelFeature f = (MmTelFeature) ImsService.this.getImsFeature(slotId, 1);
            if (f == null) {
                return (IImsMmTelFeature) executeMethodAsyncForResult(new Supplier() { // from class: android.telephony.ims.ImsService$2$$ExternalSyntheticLambda14
                    @Override // java.util.function.Supplier
                    public final Object get() {
                        return ImsService.AnonymousClass2.this.m4289xa573973d(slotId);
                    }
                }, "createEmergencyOnlyMmTelFeature");
            }
            return f.getBinder();
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$createEmergencyOnlyMmTelFeature$1$android-telephony-ims-ImsService$2  reason: not valid java name */
        public /* synthetic */ IImsMmTelFeature m4289xa573973d(int slotId) {
            return ImsService.this.createEmergencyOnlyMmTelFeatureInternal(slotId);
        }

        @Override // android.telephony.ims.aidl.IImsServiceController
        public IImsRcsFeature createRcsFeature(final int slotId, final int subId) {
            RcsFeature f = (RcsFeature) ImsService.this.getImsFeature(slotId, 2);
            if (f == null) {
                return (IImsRcsFeature) executeMethodAsyncForResult(new Supplier() { // from class: android.telephony.ims.ImsService$2$$ExternalSyntheticLambda11
                    @Override // java.util.function.Supplier
                    public final Object get() {
                        return ImsService.AnonymousClass2.this.m4291lambda$createRcsFeature$2$androidtelephonyimsImsService$2(slotId, subId);
                    }
                }, "createRcsFeature");
            }
            return f.getBinder();
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$createRcsFeature$2$android-telephony-ims-ImsService$2  reason: not valid java name */
        public /* synthetic */ IImsRcsFeature m4291lambda$createRcsFeature$2$androidtelephonyimsImsService$2(int slotId, int subId) {
            return ImsService.this.createRcsFeatureInternal(slotId, subId);
        }

        @Override // android.telephony.ims.aidl.IImsServiceController
        public void addFeatureStatusCallback(final int slotId, final int featureType, final IImsFeatureStatusCallback c) {
            executeMethodAsync(new Runnable() { // from class: android.telephony.ims.ImsService$2$$ExternalSyntheticLambda9
                @Override // java.lang.Runnable
                public final void run() {
                    ImsService.AnonymousClass2.this.m4288x6d674a51(slotId, featureType, c);
                }
            }, "addFeatureStatusCallback");
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$addFeatureStatusCallback$3$android-telephony-ims-ImsService$2  reason: not valid java name */
        public /* synthetic */ void m4288x6d674a51(int slotId, int featureType, IImsFeatureStatusCallback c) {
            ImsService.this.addImsFeatureStatusCallback(slotId, featureType, c);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$removeFeatureStatusCallback$4$android-telephony-ims-ImsService$2  reason: not valid java name */
        public /* synthetic */ void m4300x59f6fa2d(int slotId, int featureType, IImsFeatureStatusCallback c) {
            ImsService.this.removeImsFeatureStatusCallback(slotId, featureType, c);
        }

        @Override // android.telephony.ims.aidl.IImsServiceController
        public void removeFeatureStatusCallback(final int slotId, final int featureType, final IImsFeatureStatusCallback c) {
            executeMethodAsync(new Runnable() { // from class: android.telephony.ims.ImsService$2$$ExternalSyntheticLambda3
                @Override // java.lang.Runnable
                public final void run() {
                    ImsService.AnonymousClass2.this.m4300x59f6fa2d(slotId, featureType, c);
                }
            }, "removeFeatureStatusCallback");
        }

        @Override // android.telephony.ims.aidl.IImsServiceController
        public void removeImsFeature(final int slotId, final int featureType, boolean changeSubId) {
            if (changeSubId && ImsService.this.isImsFeatureCreatedForSlot(slotId, featureType)) {
                Log.w(ImsService.LOG_TAG, "Do not remove Ims feature for compatibility");
                return;
            }
            executeMethodAsync(new Runnable() { // from class: android.telephony.ims.ImsService$2$$ExternalSyntheticLambda4
                @Override // java.lang.Runnable
                public final void run() {
                    ImsService.AnonymousClass2.this.m4301lambda$removeImsFeature$5$androidtelephonyimsImsService$2(slotId, featureType);
                }
            }, "removeImsFeature");
            ImsService.this.setImsFeatureCreatedForSlot(slotId, featureType, false);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$removeImsFeature$5$android-telephony-ims-ImsService$2  reason: not valid java name */
        public /* synthetic */ void m4301lambda$removeImsFeature$5$androidtelephonyimsImsService$2(int slotId, int featureType) {
            ImsService.this.removeImsFeature(slotId, featureType);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$querySupportedImsFeatures$6$android-telephony-ims-ImsService$2  reason: not valid java name */
        public /* synthetic */ ImsFeatureConfiguration m4299x139cd88e() {
            return ImsService.this.querySupportedImsFeatures();
        }

        @Override // android.telephony.ims.aidl.IImsServiceController
        public ImsFeatureConfiguration querySupportedImsFeatures() {
            return (ImsFeatureConfiguration) executeMethodAsyncForResult(new Supplier() { // from class: android.telephony.ims.ImsService$2$$ExternalSyntheticLambda7
                @Override // java.util.function.Supplier
                public final Object get() {
                    return ImsService.AnonymousClass2.this.m4299x139cd88e();
                }
            }, "ImsFeatureConfiguration");
        }

        @Override // android.telephony.ims.aidl.IImsServiceController
        public long getImsServiceCapabilities() {
            return ((Long) executeMethodAsyncForResult(new Supplier() { // from class: android.telephony.ims.ImsService$2$$ExternalSyntheticLambda1
                @Override // java.util.function.Supplier
                public final Object get() {
                    return ImsService.AnonymousClass2.this.m4295x940a2f41();
                }
            }, "getImsServiceCapabilities")).longValue();
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$getImsServiceCapabilities$7$android-telephony-ims-ImsService$2  reason: not valid java name */
        public /* synthetic */ Long m4295x940a2f41() {
            long caps = ImsService.this.getImsServiceCapabilities();
            long sanitizedCaps = ImsService.sanitizeCapabilities(caps);
            if (caps != sanitizedCaps) {
                Log.w(ImsService.LOG_TAG, "removing invalid bits from field: 0x" + Long.toHexString(caps ^ sanitizedCaps));
            }
            return Long.valueOf(sanitizedCaps);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$notifyImsServiceReadyForFeatureCreation$8$android-telephony-ims-ImsService$2  reason: not valid java name */
        public /* synthetic */ void m4298x3f7d0972() {
            ImsService.this.readyForFeatureCreation();
        }

        @Override // android.telephony.ims.aidl.IImsServiceController
        public void notifyImsServiceReadyForFeatureCreation() {
            executeMethodAsync(new Runnable() { // from class: android.telephony.ims.ImsService$2$$ExternalSyntheticLambda15
                @Override // java.lang.Runnable
                public final void run() {
                    ImsService.AnonymousClass2.this.m4298x3f7d0972();
                }
            }, "notifyImsServiceReadyForFeatureCreation");
        }

        @Override // android.telephony.ims.aidl.IImsServiceController
        public IImsConfig getConfig(final int slotId, final int subId) {
            return (IImsConfig) executeMethodAsyncForResult(new Supplier() { // from class: android.telephony.ims.ImsService$2$$ExternalSyntheticLambda10
                @Override // java.util.function.Supplier
                public final Object get() {
                    return ImsService.AnonymousClass2.this.m4294lambda$getConfig$9$androidtelephonyimsImsService$2(slotId, subId);
                }
            }, "getConfig");
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$getConfig$9$android-telephony-ims-ImsService$2  reason: not valid java name */
        public /* synthetic */ IImsConfig m4294lambda$getConfig$9$androidtelephonyimsImsService$2(int slotId, int subId) {
            ImsConfigImplBase c = ImsService.this.getConfigForSubscription(slotId, subId);
            if (c != null) {
                c.setDefaultExecutor(ImsService.this.mExecutor);
                return c.getIImsConfig();
            }
            return null;
        }

        @Override // android.telephony.ims.aidl.IImsServiceController
        public IImsRegistration getRegistration(final int slotId, final int subId) {
            return (IImsRegistration) executeMethodAsyncForResult(new Supplier() { // from class: android.telephony.ims.ImsService$2$$ExternalSyntheticLambda0
                @Override // java.util.function.Supplier
                public final Object get() {
                    return ImsService.AnonymousClass2.this.m4296lambda$getRegistration$10$androidtelephonyimsImsService$2(slotId, subId);
                }
            }, "getRegistration");
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$getRegistration$10$android-telephony-ims-ImsService$2  reason: not valid java name */
        public /* synthetic */ IImsRegistration m4296lambda$getRegistration$10$androidtelephonyimsImsService$2(int slotId, int subId) {
            ImsRegistrationImplBase r = ImsService.this.getRegistrationForSubscription(slotId, subId);
            if (r != null) {
                r.setDefaultExecutor(ImsService.this.mExecutor);
                return r.getBinder();
            }
            return null;
        }

        @Override // android.telephony.ims.aidl.IImsServiceController
        public ISipTransport getSipTransport(final int slotId) {
            return (ISipTransport) executeMethodAsyncForResult(new Supplier() { // from class: android.telephony.ims.ImsService$2$$ExternalSyntheticLambda13
                @Override // java.util.function.Supplier
                public final Object get() {
                    return ImsService.AnonymousClass2.this.m4297lambda$getSipTransport$11$androidtelephonyimsImsService$2(slotId);
                }
            }, "getSipTransport");
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$getSipTransport$11$android-telephony-ims-ImsService$2  reason: not valid java name */
        public /* synthetic */ ISipTransport m4297lambda$getSipTransport$11$androidtelephonyimsImsService$2(int slotId) {
            SipTransportImplBase s = ImsService.this.getSipTransport(slotId);
            if (s != null) {
                s.setDefaultExecutor(ImsService.this.mExecutor);
                return s.getBinder();
            }
            return null;
        }

        @Override // android.telephony.ims.aidl.IImsServiceController
        public void enableIms(final int slotId, final int subId) {
            executeMethodAsync(new Runnable() { // from class: android.telephony.ims.ImsService$2$$ExternalSyntheticLambda5
                @Override // java.lang.Runnable
                public final void run() {
                    ImsService.AnonymousClass2.this.m4293lambda$enableIms$12$androidtelephonyimsImsService$2(slotId, subId);
                }
            }, "enableIms");
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$enableIms$12$android-telephony-ims-ImsService$2  reason: not valid java name */
        public /* synthetic */ void m4293lambda$enableIms$12$androidtelephonyimsImsService$2(int slotId, int subId) {
            ImsService.this.enableImsForSubscription(slotId, subId);
        }

        @Override // android.telephony.ims.aidl.IImsServiceController
        public void disableIms(final int slotId, final int subId) {
            executeMethodAsync(new Runnable() { // from class: android.telephony.ims.ImsService$2$$ExternalSyntheticLambda8
                @Override // java.lang.Runnable
                public final void run() {
                    ImsService.AnonymousClass2.this.m4292lambda$disableIms$13$androidtelephonyimsImsService$2(slotId, subId);
                }
            }, "disableIms");
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$disableIms$13$android-telephony-ims-ImsService$2  reason: not valid java name */
        public /* synthetic */ void m4292lambda$disableIms$13$androidtelephonyimsImsService$2(int slotId, int subId) {
            ImsService.this.disableImsForSubscription(slotId, subId);
        }

        private void executeMethodAsync(final Runnable r, String errorLogName) {
            try {
                CompletableFuture.runAsync(new Runnable() { // from class: android.telephony.ims.ImsService$2$$ExternalSyntheticLambda12
                    @Override // java.lang.Runnable
                    public final void run() {
                        TelephonyUtils.runWithCleanCallingIdentity(r);
                    }
                }, ImsService.this.mExecutor).join();
            } catch (CancellationException | CompletionException e) {
                Log.w(ImsService.LOG_TAG, "ImsService Binder - " + errorLogName + " exception: " + e.getMessage());
            }
        }

        private <T> T executeMethodAsyncForResult(final Supplier<T> r, String errorLogName) {
            CompletableFuture<T> future = CompletableFuture.supplyAsync(new Supplier() { // from class: android.telephony.ims.ImsService$2$$ExternalSyntheticLambda2
                @Override // java.util.function.Supplier
                public final Object get() {
                    Object runWithCleanCallingIdentity;
                    runWithCleanCallingIdentity = TelephonyUtils.runWithCleanCallingIdentity(r);
                    return runWithCleanCallingIdentity;
                }
            }, ImsService.this.mExecutor);
            try {
                return future.get();
            } catch (InterruptedException | ExecutionException e) {
                Log.w(ImsService.LOG_TAG, "ImsService Binder - " + errorLogName + " exception: " + e.getMessage());
                return null;
            }
        }
    }

    @Override // android.app.Service
    public IBinder onBind(Intent intent) {
        if (SERVICE_INTERFACE.equals(intent.getAction())) {
            Log.i(LOG_TAG, "ImsService Bound.");
            return this.mImsServiceController;
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public IImsMmTelFeature createMmTelFeatureInternal(int slotId, int subscriptionId) {
        MmTelFeature f = createMmTelFeatureForSubscription(slotId, subscriptionId);
        if (f != null) {
            setupFeature(f, slotId, 1);
            f.setDefaultExecutor(this.mExecutor);
            return f.getBinder();
        }
        Log.e(LOG_TAG, "createMmTelFeatureInternal: null feature returned.");
        return null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public IImsMmTelFeature createEmergencyOnlyMmTelFeatureInternal(int slotId) {
        MmTelFeature f = createEmergencyOnlyMmTelFeature(slotId);
        if (f != null) {
            setupFeature(f, slotId, 1);
            f.setDefaultExecutor(this.mExecutor);
            return f.getBinder();
        }
        Log.e(LOG_TAG, "createEmergencyOnlyMmTelFeatureInternal: null feature returned.");
        return null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public IImsRcsFeature createRcsFeatureInternal(int slotId, int subI) {
        RcsFeature f = createRcsFeatureForSubscription(slotId, subI);
        if (f != null) {
            f.setDefaultExecutor(this.mExecutor);
            setupFeature(f, slotId, 2);
            return f.getBinder();
        }
        Log.e(LOG_TAG, "createRcsFeatureInternal: null feature returned.");
        return null;
    }

    private void setupFeature(ImsFeature f, int slotId, int featureType) {
        f.initialize(this, slotId);
        addImsFeature(slotId, featureType, f);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void addImsFeatureStatusCallback(int slotId, int featureType, IImsFeatureStatusCallback c) {
        synchronized (this.mFeaturesBySlot) {
            SparseArray<ImsFeature> features = this.mFeaturesBySlot.get(slotId);
            if (features == null) {
                Log.w(LOG_TAG, "Can not add ImsFeatureStatusCallback - no features on slot " + slotId);
                return;
            }
            ImsFeature f = features.get(featureType);
            if (f != null) {
                f.addImsFeatureStatusCallback(c);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void removeImsFeatureStatusCallback(int slotId, int featureType, IImsFeatureStatusCallback c) {
        synchronized (this.mFeaturesBySlot) {
            SparseArray<ImsFeature> features = this.mFeaturesBySlot.get(slotId);
            if (features == null) {
                Log.w(LOG_TAG, "Can not remove ImsFeatureStatusCallback - no features on slot " + slotId);
                return;
            }
            ImsFeature f = features.get(featureType);
            if (f != null) {
                f.removeImsFeatureStatusCallback(c);
            }
        }
    }

    private void addImsFeature(int slotId, int featureType, ImsFeature f) {
        synchronized (this.mFeaturesBySlot) {
            SparseArray<ImsFeature> features = this.mFeaturesBySlot.get(slotId);
            if (features == null) {
                features = new SparseArray<>();
                this.mFeaturesBySlot.put(slotId, features);
            }
            features.put(featureType, f);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void removeImsFeature(int slotId, int featureType) {
        synchronized (this.mFeaturesBySlot) {
            SparseArray<ImsFeature> features = this.mFeaturesBySlot.get(slotId);
            if (features == null) {
                Log.w(LOG_TAG, "Can not remove ImsFeature. No ImsFeatures exist on slot " + slotId);
                return;
            }
            ImsFeature f = features.get(featureType);
            if (f == null) {
                Log.w(LOG_TAG, "Can not remove ImsFeature. No feature with type " + featureType + " exists on slot " + slotId);
                return;
            }
            f.onFeatureRemoved();
            features.remove(featureType);
        }
    }

    public ImsFeature getImsFeature(int slotId, int featureType) {
        synchronized (this.mFeaturesBySlot) {
            SparseArray<ImsFeature> features = this.mFeaturesBySlot.get(slotId);
            if (features == null) {
                return null;
            }
            return features.get(featureType);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setImsFeatureCreatedForSlot(int slotId, int featureType, boolean createdForSlot) {
        synchronized (this.mCreateImsFeatureWithSlotIdFlagMap) {
            getImsFeatureCreatedForSlot(slotId).put(featureType, createdForSlot);
        }
    }

    public boolean isImsFeatureCreatedForSlot(int slotId, int featureType) {
        boolean z;
        synchronized (this.mCreateImsFeatureWithSlotIdFlagMap) {
            z = getImsFeatureCreatedForSlot(slotId).get(featureType);
        }
        return z;
    }

    private SparseBooleanArray getImsFeatureCreatedForSlot(int slotId) {
        SparseBooleanArray createFlag = this.mCreateImsFeatureWithSlotIdFlagMap.get(slotId);
        if (createFlag == null) {
            SparseBooleanArray createFlag2 = new SparseBooleanArray();
            this.mCreateImsFeatureWithSlotIdFlagMap.put(slotId, createFlag2);
            return createFlag2;
        }
        return createFlag;
    }

    public ImsFeatureConfiguration querySupportedImsFeatures() {
        return new ImsFeatureConfiguration();
    }

    public final void onUpdateSupportedImsFeatures(ImsFeatureConfiguration c) throws RemoteException {
        IImsServiceControllerListener iImsServiceControllerListener = this.mListener;
        if (iImsServiceControllerListener == null) {
            throw new IllegalStateException("Framework is not ready");
        }
        iImsServiceControllerListener.onUpdateSupportedImsFeatures(c);
    }

    public long getImsServiceCapabilities() {
        return 0L;
    }

    public void readyForFeatureCreation() {
    }

    public void enableImsForSubscription(int slotId, int subscriptionId) {
        enableIms(slotId);
    }

    public void disableImsForSubscription(int slotId, int subscriptionId) {
        disableIms(slotId);
    }

    @Deprecated
    public void enableIms(int slotId) {
    }

    @Deprecated
    public void disableIms(int slotId) {
    }

    public MmTelFeature createMmTelFeatureForSubscription(int slotId, int subscriptionId) {
        setImsFeatureCreatedForSlot(slotId, 1, true);
        return createMmTelFeature(slotId);
    }

    public RcsFeature createRcsFeatureForSubscription(int slotId, int subscriptionId) {
        setImsFeatureCreatedForSlot(slotId, 2, true);
        return createRcsFeature(slotId);
    }

    public MmTelFeature createEmergencyOnlyMmTelFeature(int slotId) {
        setImsFeatureCreatedForSlot(slotId, 1, true);
        return createMmTelFeature(slotId);
    }

    @Deprecated
    public MmTelFeature createMmTelFeature(int slotId) {
        return null;
    }

    @Deprecated
    public RcsFeature createRcsFeature(int slotId) {
        return null;
    }

    public ImsConfigImplBase getConfigForSubscription(int slotId, int subscriptionId) {
        return getConfig(slotId);
    }

    public ImsRegistrationImplBase getRegistrationForSubscription(int slotId, int subscriptionId) {
        return getRegistration(slotId);
    }

    @Deprecated
    public ImsConfigImplBase getConfig(int slotId) {
        return new ImsConfigImplBase();
    }

    @Deprecated
    public ImsRegistrationImplBase getRegistration(int slotId) {
        return new ImsRegistrationImplBase();
    }

    public SipTransportImplBase getSipTransport(int slotId) {
        return null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static long sanitizeCapabilities(long caps) {
        long filter = (-1) << ((int) (CAPABILITY_MAX_INDEX + 1));
        return caps & (~filter) & (-2);
    }

    public static String getCapabilitiesString(long caps) {
        StringBuffer result = new StringBuffer();
        result.append("capabilities={ ");
        long filter = -1;
        for (long i = 0; (caps & filter) != 0 && i <= 63; i++) {
            long bitToCheck = 1 << ((int) i);
            if ((caps & bitToCheck) != 0) {
                result.append(CAPABILITIES_LOG_MAP.getOrDefault(Long.valueOf(bitToCheck), bitToCheck + "?"));
                result.append(" ");
            }
            filter <<= 1;
        }
        result.append("}");
        return result.toString();
    }

    public Executor getExecutor() {
        return new PendingIntent$$ExternalSyntheticLambda1();
    }
}
