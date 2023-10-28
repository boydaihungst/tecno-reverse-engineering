package android.telephony.ims.feature;

import android.annotation.SystemApi;
import android.inputmethodservice.navigationbar.NavigationBarInflaterView;
import android.os.Bundle;
import android.os.Message;
import android.os.RemoteException;
import android.telephony.ims.ImsCallProfile;
import android.telephony.ims.ImsReasonInfo;
import android.telephony.ims.RtpHeaderExtensionType;
import android.telephony.ims.aidl.IImsCapabilityCallback;
import android.telephony.ims.aidl.IImsMmTelFeature;
import android.telephony.ims.aidl.IImsMmTelListener;
import android.telephony.ims.aidl.IImsSmsListener;
import android.telephony.ims.feature.ImsFeature;
import android.telephony.ims.feature.MmTelFeature;
import android.telephony.ims.stub.ImsCallSessionImplBase;
import android.telephony.ims.stub.ImsEcbmImplBase;
import android.telephony.ims.stub.ImsMultiEndpointImplBase;
import android.telephony.ims.stub.ImsSmsImplBase;
import android.telephony.ims.stub.ImsUtImplBase;
import android.util.ArraySet;
import android.util.Log;
import com.android.ims.internal.IImsCallSession;
import com.android.ims.internal.IImsEcbm;
import com.android.ims.internal.IImsMultiEndpoint;
import com.android.ims.internal.IImsUt;
import com.android.internal.telephony.util.TelephonyUtils;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
/* loaded from: classes3.dex */
public class MmTelFeature extends ImsFeature {
    @SystemApi
    public static final String EXTRA_IS_UNKNOWN_CALL = "android.telephony.ims.feature.extra.IS_UNKNOWN_CALL";
    @SystemApi
    public static final String EXTRA_IS_USSD = "android.telephony.ims.feature.extra.IS_USSD";
    private static final String LOG_TAG = "MmTelFeature";
    @SystemApi
    public static final int PROCESS_CALL_CSFB = 1;
    @SystemApi
    public static final int PROCESS_CALL_IMS = 0;
    private Executor mExecutor;
    private final IImsMmTelFeature mImsMMTelBinder = new AnonymousClass1();
    private IImsMmTelListener mListener;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes3.dex */
    public @interface ProcessCallResult {
    }

    @SystemApi
    public MmTelFeature() {
    }

    @SystemApi
    public MmTelFeature(Executor executor) {
        this.mExecutor = executor;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: android.telephony.ims.feature.MmTelFeature$1  reason: invalid class name */
    /* loaded from: classes3.dex */
    public class AnonymousClass1 extends IImsMmTelFeature.Stub {
        AnonymousClass1() {
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$setListener$0$android-telephony-ims-feature-MmTelFeature$1  reason: not valid java name */
        public /* synthetic */ void m4415x61bf370f(IImsMmTelListener l) {
            MmTelFeature.this.setListener(l);
        }

        @Override // android.telephony.ims.aidl.IImsMmTelFeature
        public void setListener(final IImsMmTelListener l) {
            executeMethodAsyncNoException(new Runnable() { // from class: android.telephony.ims.feature.MmTelFeature$1$$ExternalSyntheticLambda16
                @Override // java.lang.Runnable
                public final void run() {
                    MmTelFeature.AnonymousClass1.this.m4415x61bf370f(l);
                }
            }, "setListener");
        }

        @Override // android.telephony.ims.aidl.IImsMmTelFeature
        public int getFeatureState() throws RemoteException {
            return ((Integer) executeMethodAsyncForResult(new Supplier() { // from class: android.telephony.ims.feature.MmTelFeature$1$$ExternalSyntheticLambda13
                @Override // java.util.function.Supplier
                public final Object get() {
                    return MmTelFeature.AnonymousClass1.this.m4406x24719253();
                }
            }, "getFeatureState")).intValue();
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$getFeatureState$1$android-telephony-ims-feature-MmTelFeature$1  reason: not valid java name */
        public /* synthetic */ Integer m4406x24719253() {
            return Integer.valueOf(MmTelFeature.this.getFeatureState());
        }

        @Override // android.telephony.ims.aidl.IImsMmTelFeature
        public ImsCallProfile createCallProfile(final int callSessionType, final int callType) throws RemoteException {
            return (ImsCallProfile) executeMethodAsyncForResult(new Supplier() { // from class: android.telephony.ims.feature.MmTelFeature$1$$ExternalSyntheticLambda22
                @Override // java.util.function.Supplier
                public final Object get() {
                    return MmTelFeature.AnonymousClass1.this.m4403xc3b1efd4(callSessionType, callType);
                }
            }, "createCallProfile");
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$createCallProfile$2$android-telephony-ims-feature-MmTelFeature$1  reason: not valid java name */
        public /* synthetic */ ImsCallProfile m4403xc3b1efd4(int callSessionType, int callType) {
            return MmTelFeature.this.createCallProfile(callSessionType, callType);
        }

        @Override // android.telephony.ims.aidl.IImsMmTelFeature
        public void changeOfferedRtpHeaderExtensionTypes(final List<RtpHeaderExtensionType> types) throws RemoteException {
            executeMethodAsync(new Runnable() { // from class: android.telephony.ims.feature.MmTelFeature$1$$ExternalSyntheticLambda23
                @Override // java.lang.Runnable
                public final void run() {
                    MmTelFeature.AnonymousClass1.this.m4402x6f49fcfe(types);
                }
            }, "changeOfferedRtpHeaderExtensionTypes");
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$changeOfferedRtpHeaderExtensionTypes$3$android-telephony-ims-feature-MmTelFeature$1  reason: not valid java name */
        public /* synthetic */ void m4402x6f49fcfe(List types) {
            MmTelFeature.this.changeOfferedRtpHeaderExtensionTypes(new ArraySet(types));
        }

        @Override // android.telephony.ims.aidl.IImsMmTelFeature
        public IImsCallSession createCallSession(final ImsCallProfile profile) throws RemoteException {
            final AtomicReference<RemoteException> exceptionRef = new AtomicReference<>();
            IImsCallSession result = (IImsCallSession) executeMethodAsyncForResult(new Supplier() { // from class: android.telephony.ims.feature.MmTelFeature$1$$ExternalSyntheticLambda6
                @Override // java.util.function.Supplier
                public final Object get() {
                    return MmTelFeature.AnonymousClass1.this.m4404x8527ee85(profile, exceptionRef);
                }
            }, "createCallSession");
            if (exceptionRef.get() != null) {
                throw exceptionRef.get();
            }
            return result;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$createCallSession$4$android-telephony-ims-feature-MmTelFeature$1  reason: not valid java name */
        public /* synthetic */ IImsCallSession m4404x8527ee85(ImsCallProfile profile, AtomicReference exceptionRef) {
            try {
                return MmTelFeature.this.createCallSessionInterface(profile);
            } catch (RemoteException e) {
                exceptionRef.set(e);
                return null;
            }
        }

        @Override // android.telephony.ims.aidl.IImsMmTelFeature
        public int shouldProcessCall(final String[] numbers) {
            Integer result = (Integer) executeMethodAsyncForResultNoException(new Supplier() { // from class: android.telephony.ims.feature.MmTelFeature$1$$ExternalSyntheticLambda7
                @Override // java.util.function.Supplier
                public final Object get() {
                    return MmTelFeature.AnonymousClass1.this.m4418xd7fbd266(numbers);
                }
            }, "shouldProcessCall");
            if (result != null) {
                return result.intValue();
            }
            return 1;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$shouldProcessCall$5$android-telephony-ims-feature-MmTelFeature$1  reason: not valid java name */
        public /* synthetic */ Integer m4418xd7fbd266(String[] numbers) {
            return Integer.valueOf(MmTelFeature.this.shouldProcessCall(numbers));
        }

        @Override // android.telephony.ims.aidl.IImsMmTelFeature
        public IImsUt getUtInterface() throws RemoteException {
            final AtomicReference<RemoteException> exceptionRef = new AtomicReference<>();
            IImsUt result = (IImsUt) executeMethodAsyncForResult(new Supplier() { // from class: android.telephony.ims.feature.MmTelFeature$1$$ExternalSyntheticLambda19
                @Override // java.util.function.Supplier
                public final Object get() {
                    return MmTelFeature.AnonymousClass1.this.m4409xe65e0701(exceptionRef);
                }
            }, "getUtInterface");
            if (exceptionRef.get() != null) {
                throw exceptionRef.get();
            }
            return result;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$getUtInterface$6$android-telephony-ims-feature-MmTelFeature$1  reason: not valid java name */
        public /* synthetic */ IImsUt m4409xe65e0701(AtomicReference exceptionRef) {
            try {
                return MmTelFeature.this.getUtInterface();
            } catch (RemoteException e) {
                exceptionRef.set(e);
                return null;
            }
        }

        @Override // android.telephony.ims.aidl.IImsMmTelFeature
        public IImsEcbm getEcbmInterface() throws RemoteException {
            final AtomicReference<RemoteException> exceptionRef = new AtomicReference<>();
            IImsEcbm result = (IImsEcbm) executeMethodAsyncForResult(new Supplier() { // from class: android.telephony.ims.feature.MmTelFeature$1$$ExternalSyntheticLambda14
                @Override // java.util.function.Supplier
                public final Object get() {
                    return MmTelFeature.AnonymousClass1.this.m4405xfa8b772a(exceptionRef);
                }
            }, "getEcbmInterface");
            if (exceptionRef.get() != null) {
                throw exceptionRef.get();
            }
            return result;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$getEcbmInterface$7$android-telephony-ims-feature-MmTelFeature$1  reason: not valid java name */
        public /* synthetic */ IImsEcbm m4405xfa8b772a(AtomicReference exceptionRef) {
            try {
                return MmTelFeature.this.getEcbmInterface();
            } catch (RemoteException e) {
                exceptionRef.set(e);
                return null;
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$setUiTtyMode$8$android-telephony-ims-feature-MmTelFeature$1  reason: not valid java name */
        public /* synthetic */ void m4417xcdad5f1d(int uiTtyMode, Message onCompleteMessage) {
            MmTelFeature.this.setUiTtyMode(uiTtyMode, onCompleteMessage);
        }

        @Override // android.telephony.ims.aidl.IImsMmTelFeature
        public void setUiTtyMode(final int uiTtyMode, final Message onCompleteMessage) throws RemoteException {
            executeMethodAsync(new Runnable() { // from class: android.telephony.ims.feature.MmTelFeature$1$$ExternalSyntheticLambda5
                @Override // java.lang.Runnable
                public final void run() {
                    MmTelFeature.AnonymousClass1.this.m4417xcdad5f1d(uiTtyMode, onCompleteMessage);
                }
            }, "setUiTtyMode");
        }

        @Override // android.telephony.ims.aidl.IImsMmTelFeature
        public IImsMultiEndpoint getMultiEndpointInterface() throws RemoteException {
            final AtomicReference<RemoteException> exceptionRef = new AtomicReference<>();
            IImsMultiEndpoint result = (IImsMultiEndpoint) executeMethodAsyncForResult(new Supplier() { // from class: android.telephony.ims.feature.MmTelFeature$1$$ExternalSyntheticLambda20
                @Override // java.util.function.Supplier
                public final Object get() {
                    return MmTelFeature.AnonymousClass1.this.m4407xbed0b4db(exceptionRef);
                }
            }, "getMultiEndpointInterface");
            if (exceptionRef.get() != null) {
                throw exceptionRef.get();
            }
            return result;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$getMultiEndpointInterface$9$android-telephony-ims-feature-MmTelFeature$1  reason: not valid java name */
        public /* synthetic */ IImsMultiEndpoint m4407xbed0b4db(AtomicReference exceptionRef) {
            try {
                return MmTelFeature.this.getMultiEndpointInterface();
            } catch (RemoteException e) {
                exceptionRef.set(e);
                return null;
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$queryCapabilityStatus$10$android-telephony-ims-feature-MmTelFeature$1  reason: not valid java name */
        public /* synthetic */ Integer m4412x3e242ba6() {
            return Integer.valueOf(MmTelFeature.this.queryCapabilityStatus().mCapabilities);
        }

        @Override // android.telephony.ims.aidl.IImsMmTelFeature
        public int queryCapabilityStatus() {
            Integer result = (Integer) executeMethodAsyncForResultNoException(new Supplier() { // from class: android.telephony.ims.feature.MmTelFeature$1$$ExternalSyntheticLambda24
                @Override // java.util.function.Supplier
                public final Object get() {
                    return MmTelFeature.AnonymousClass1.this.m4412x3e242ba6();
                }
            }, "queryCapabilityStatus");
            if (result != null) {
                return result.intValue();
            }
            return 0;
        }

        @Override // android.telephony.ims.aidl.IImsMmTelFeature
        public void addCapabilityCallback(final IImsCapabilityCallback c) {
            executeMethodAsyncNoException(new Runnable() { // from class: android.telephony.ims.feature.MmTelFeature$1$$ExternalSyntheticLambda18
                @Override // java.lang.Runnable
                public final void run() {
                    MmTelFeature.AnonymousClass1.this.m4400xfebcf531(c);
                }
            }, "addCapabilityCallback");
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$addCapabilityCallback$11$android-telephony-ims-feature-MmTelFeature$1  reason: not valid java name */
        public /* synthetic */ void m4400xfebcf531(IImsCapabilityCallback c) {
            MmTelFeature.this.addCapabilityCallback(c);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$removeCapabilityCallback$12$android-telephony-ims-feature-MmTelFeature$1  reason: not valid java name */
        public /* synthetic */ void m4413x15882d4d(IImsCapabilityCallback c) {
            MmTelFeature.this.removeCapabilityCallback(c);
        }

        @Override // android.telephony.ims.aidl.IImsMmTelFeature
        public void removeCapabilityCallback(final IImsCapabilityCallback c) {
            executeMethodAsyncNoException(new Runnable() { // from class: android.telephony.ims.feature.MmTelFeature$1$$ExternalSyntheticLambda9
                @Override // java.lang.Runnable
                public final void run() {
                    MmTelFeature.AnonymousClass1.this.m4413x15882d4d(c);
                }
            }, "removeCapabilityCallback");
        }

        @Override // android.telephony.ims.aidl.IImsMmTelFeature
        public void changeCapabilitiesConfiguration(final CapabilityChangeRequest request, final IImsCapabilityCallback c) {
            executeMethodAsyncNoException(new Runnable() { // from class: android.telephony.ims.feature.MmTelFeature$1$$ExternalSyntheticLambda8
                @Override // java.lang.Runnable
                public final void run() {
                    MmTelFeature.AnonymousClass1.this.m4401x1e3c3c1(request, c);
                }
            }, "changeCapabilitiesConfiguration");
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$changeCapabilitiesConfiguration$13$android-telephony-ims-feature-MmTelFeature$1  reason: not valid java name */
        public /* synthetic */ void m4401x1e3c3c1(CapabilityChangeRequest request, IImsCapabilityCallback c) {
            MmTelFeature.this.requestChangeEnabledCapabilities(request, c);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$queryCapabilityConfiguration$14$android-telephony-ims-feature-MmTelFeature$1  reason: not valid java name */
        public /* synthetic */ void m4411xe6fb7bc0(int capability, int radioTech, IImsCapabilityCallback c) {
            MmTelFeature.this.queryCapabilityConfigurationInternal(capability, radioTech, c);
        }

        @Override // android.telephony.ims.aidl.IImsMmTelFeature
        public void queryCapabilityConfiguration(final int capability, final int radioTech, final IImsCapabilityCallback c) {
            executeMethodAsyncNoException(new Runnable() { // from class: android.telephony.ims.feature.MmTelFeature$1$$ExternalSyntheticLambda12
                @Override // java.lang.Runnable
                public final void run() {
                    MmTelFeature.AnonymousClass1.this.m4411xe6fb7bc0(capability, radioTech, c);
                }
            }, "queryCapabilityConfiguration");
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$setSmsListener$15$android-telephony-ims-feature-MmTelFeature$1  reason: not valid java name */
        public /* synthetic */ void m4416xd8b11cb4(IImsSmsListener l) {
            MmTelFeature.this.setSmsListener(l);
        }

        @Override // android.telephony.ims.aidl.IImsMmTelFeature
        public void setSmsListener(final IImsSmsListener l) {
            executeMethodAsyncNoException(new Runnable() { // from class: android.telephony.ims.feature.MmTelFeature$1$$ExternalSyntheticLambda17
                @Override // java.lang.Runnable
                public final void run() {
                    MmTelFeature.AnonymousClass1.this.m4416xd8b11cb4(l);
                }
            }, "setSmsListener");
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$sendSms$16$android-telephony-ims-feature-MmTelFeature$1  reason: not valid java name */
        public /* synthetic */ void m4414lambda$sendSms$16$androidtelephonyimsfeatureMmTelFeature$1(int token, int messageRef, String format, String smsc, boolean retry, byte[] pdu) {
            MmTelFeature.this.sendSms(token, messageRef, format, smsc, retry, pdu);
        }

        @Override // android.telephony.ims.aidl.IImsMmTelFeature
        public void sendSms(final int token, final int messageRef, final String format, final String smsc, final boolean retry, final byte[] pdu) {
            executeMethodAsyncNoException(new Runnable() { // from class: android.telephony.ims.feature.MmTelFeature$1$$ExternalSyntheticLambda10
                @Override // java.lang.Runnable
                public final void run() {
                    MmTelFeature.AnonymousClass1.this.m4414lambda$sendSms$16$androidtelephonyimsfeatureMmTelFeature$1(token, messageRef, format, smsc, retry, pdu);
                }
            }, "sendSms");
        }

        @Override // android.telephony.ims.aidl.IImsMmTelFeature
        public void acknowledgeSms(final int token, final int messageRef, final int result) {
            executeMethodAsyncNoException(new Runnable() { // from class: android.telephony.ims.feature.MmTelFeature$1$$ExternalSyntheticLambda21
                @Override // java.lang.Runnable
                public final void run() {
                    MmTelFeature.AnonymousClass1.this.m4398xe29221a4(token, messageRef, result);
                }
            }, "acknowledgeSms");
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$acknowledgeSms$17$android-telephony-ims-feature-MmTelFeature$1  reason: not valid java name */
        public /* synthetic */ void m4398xe29221a4(int token, int messageRef, int result) {
            MmTelFeature.this.acknowledgeSms(token, messageRef, result);
        }

        @Override // android.telephony.ims.aidl.IImsMmTelFeature
        public void acknowledgeSmsReport(final int token, final int messageRef, final int result) {
            executeMethodAsyncNoException(new Runnable() { // from class: android.telephony.ims.feature.MmTelFeature$1$$ExternalSyntheticLambda4
                @Override // java.lang.Runnable
                public final void run() {
                    MmTelFeature.AnonymousClass1.this.m4399x5bd1dd17(token, messageRef, result);
                }
            }, "acknowledgeSmsReport");
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$acknowledgeSmsReport$18$android-telephony-ims-feature-MmTelFeature$1  reason: not valid java name */
        public /* synthetic */ void m4399x5bd1dd17(int token, int messageRef, int result) {
            MmTelFeature.this.acknowledgeSmsReport(token, messageRef, result);
        }

        @Override // android.telephony.ims.aidl.IImsMmTelFeature
        public String getSmsFormat() {
            return (String) executeMethodAsyncForResultNoException(new Supplier() { // from class: android.telephony.ims.feature.MmTelFeature$1$$ExternalSyntheticLambda11
                @Override // java.util.function.Supplier
                public final Object get() {
                    return MmTelFeature.AnonymousClass1.this.m4408x7a0908ff();
                }
            }, "getSmsFormat");
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$getSmsFormat$19$android-telephony-ims-feature-MmTelFeature$1  reason: not valid java name */
        public /* synthetic */ String m4408x7a0908ff() {
            return MmTelFeature.this.getSmsFormat();
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onSmsReady$20$android-telephony-ims-feature-MmTelFeature$1  reason: not valid java name */
        public /* synthetic */ void m4410x15d258b8() {
            MmTelFeature.this.onSmsReady();
        }

        @Override // android.telephony.ims.aidl.IImsMmTelFeature
        public void onSmsReady() {
            executeMethodAsyncNoException(new Runnable() { // from class: android.telephony.ims.feature.MmTelFeature$1$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    MmTelFeature.AnonymousClass1.this.m4410x15d258b8();
                }
            }, "onSmsReady");
        }

        private void executeMethodAsync(final Runnable r, String errorLogName) throws RemoteException {
            try {
                CompletableFuture.runAsync(new Runnable() { // from class: android.telephony.ims.feature.MmTelFeature$1$$ExternalSyntheticLambda3
                    @Override // java.lang.Runnable
                    public final void run() {
                        TelephonyUtils.runWithCleanCallingIdentity(r);
                    }
                }, MmTelFeature.this.mExecutor).join();
            } catch (CancellationException | CompletionException e) {
                Log.w(MmTelFeature.LOG_TAG, "MmTelFeature Binder - " + errorLogName + " exception: " + e.getMessage());
                throw new RemoteException(e.getMessage());
            }
        }

        private void executeMethodAsyncNoException(final Runnable r, String errorLogName) {
            try {
                CompletableFuture.runAsync(new Runnable() { // from class: android.telephony.ims.feature.MmTelFeature$1$$ExternalSyntheticLambda15
                    @Override // java.lang.Runnable
                    public final void run() {
                        TelephonyUtils.runWithCleanCallingIdentity(r);
                    }
                }, MmTelFeature.this.mExecutor).join();
            } catch (CancellationException | CompletionException e) {
                Log.w(MmTelFeature.LOG_TAG, "MmTelFeature Binder - " + errorLogName + " exception: " + e.getMessage());
            }
        }

        private <T> T executeMethodAsyncForResult(final Supplier<T> r, String errorLogName) throws RemoteException {
            CompletableFuture<T> future = CompletableFuture.supplyAsync(new Supplier() { // from class: android.telephony.ims.feature.MmTelFeature$1$$ExternalSyntheticLambda2
                @Override // java.util.function.Supplier
                public final Object get() {
                    Object runWithCleanCallingIdentity;
                    runWithCleanCallingIdentity = TelephonyUtils.runWithCleanCallingIdentity(r);
                    return runWithCleanCallingIdentity;
                }
            }, MmTelFeature.this.mExecutor);
            try {
                return future.get();
            } catch (InterruptedException | ExecutionException e) {
                Log.w(MmTelFeature.LOG_TAG, "MmTelFeature Binder - " + errorLogName + " exception: " + e.getMessage());
                throw new RemoteException(e.getMessage());
            }
        }

        private <T> T executeMethodAsyncForResultNoException(final Supplier<T> r, String errorLogName) {
            CompletableFuture<T> future = CompletableFuture.supplyAsync(new Supplier() { // from class: android.telephony.ims.feature.MmTelFeature$1$$ExternalSyntheticLambda0
                @Override // java.util.function.Supplier
                public final Object get() {
                    Object runWithCleanCallingIdentity;
                    runWithCleanCallingIdentity = TelephonyUtils.runWithCleanCallingIdentity(r);
                    return runWithCleanCallingIdentity;
                }
            }, MmTelFeature.this.mExecutor);
            try {
                return future.get();
            } catch (InterruptedException | ExecutionException e) {
                Log.w(MmTelFeature.LOG_TAG, "MmTelFeature Binder - " + errorLogName + " exception: " + e.getMessage());
                return null;
            }
        }
    }

    /* loaded from: classes3.dex */
    public static class MmTelCapabilities extends ImsFeature.Capabilities {
        public static final int CAPABILITY_TYPE_CALL_COMPOSER = 16;
        public static final int CAPABILITY_TYPE_MAX = 17;
        public static final int CAPABILITY_TYPE_NONE = 0;
        public static final int CAPABILITY_TYPE_SMS = 8;
        public static final int CAPABILITY_TYPE_UT = 4;
        public static final int CAPABILITY_TYPE_VIDEO = 2;
        public static final int CAPABILITY_TYPE_VOICE = 1;

        @Retention(RetentionPolicy.SOURCE)
        /* loaded from: classes3.dex */
        public @interface MmTelCapability {
        }

        @SystemApi
        public MmTelCapabilities() {
        }

        @SystemApi
        @Deprecated
        public MmTelCapabilities(ImsFeature.Capabilities c) {
            this.mCapabilities = c.mCapabilities;
        }

        @SystemApi
        public MmTelCapabilities(int capabilities) {
            super(capabilities);
        }

        @Override // android.telephony.ims.feature.ImsFeature.Capabilities
        @SystemApi
        public final void addCapabilities(int capabilities) {
            super.addCapabilities(capabilities);
        }

        @Override // android.telephony.ims.feature.ImsFeature.Capabilities
        @SystemApi
        public final void removeCapabilities(int capability) {
            super.removeCapabilities(capability);
        }

        @Override // android.telephony.ims.feature.ImsFeature.Capabilities
        public final boolean isCapable(int capabilities) {
            return super.isCapable(capabilities);
        }

        @Override // android.telephony.ims.feature.ImsFeature.Capabilities
        public String toString() {
            return "MmTel Capabilities - [Voice: " + isCapable(1) + " Video: " + isCapable(2) + " UT: " + isCapable(4) + " SMS: " + isCapable(8) + " CALL_COMPOSER: " + isCapable(16) + NavigationBarInflaterView.SIZE_MOD_END;
        }
    }

    /* loaded from: classes3.dex */
    public static class Listener extends IImsMmTelListener.Stub {
        @Override // android.telephony.ims.aidl.IImsMmTelListener
        public void onIncomingCall(IImsCallSession c, Bundle extras) {
        }

        @Override // android.telephony.ims.aidl.IImsMmTelListener
        public void onRejectedCall(ImsCallProfile callProfile, ImsReasonInfo reason) {
        }

        @Override // android.telephony.ims.aidl.IImsMmTelListener
        public void onVoiceMessageCountUpdate(int count) {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setListener(IImsMmTelListener listener) {
        synchronized (this.mLock) {
            this.mListener = listener;
            if (listener != null) {
                onFeatureReady();
            }
        }
    }

    private IImsMmTelListener getListener() {
        IImsMmTelListener iImsMmTelListener;
        synchronized (this.mLock) {
            iImsMmTelListener = this.mListener;
        }
        return iImsMmTelListener;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // android.telephony.ims.feature.ImsFeature
    @SystemApi
    public final MmTelCapabilities queryCapabilityStatus() {
        return new MmTelCapabilities(super.queryCapabilityStatus());
    }

    @SystemApi
    public final void notifyCapabilitiesStatusChanged(MmTelCapabilities c) {
        if (c == null) {
            throw new IllegalArgumentException("MmTelCapabilities must be non-null!");
        }
        super.notifyCapabilitiesStatusChanged((ImsFeature.Capabilities) c);
    }

    @SystemApi
    public final void notifyIncomingCall(ImsCallSessionImplBase c, Bundle extras) {
        if (c == null || extras == null) {
            throw new IllegalArgumentException("ImsCallSessionImplBase and Bundle can not be null.");
        }
        IImsMmTelListener listener = getListener();
        if (listener == null) {
            throw new IllegalStateException("Session is not available.");
        }
        try {
            listener.onIncomingCall(c.getServiceImpl(), extras);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @SystemApi
    public final void notifyRejectedCall(ImsCallProfile callProfile, ImsReasonInfo reason) {
        if (callProfile == null || reason == null) {
            throw new IllegalArgumentException("ImsCallProfile and ImsReasonInfo must not be null.");
        }
        IImsMmTelListener listener = getListener();
        if (listener == null) {
            throw new IllegalStateException("Session is not available.");
        }
        try {
            listener.onRejectedCall(callProfile, reason);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public final void notifyIncomingCallSession(IImsCallSession c, Bundle extras) {
        IImsMmTelListener listener = getListener();
        if (listener == null) {
            throw new IllegalStateException("Session is not available.");
        }
        try {
            listener.onIncomingCall(c, extras);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @SystemApi
    public final void notifyVoiceMessageCountUpdate(int count) {
        IImsMmTelListener listener = getListener();
        if (listener == null) {
            throw new IllegalStateException("Session is not available.");
        }
        try {
            listener.onVoiceMessageCountUpdate(count);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override // android.telephony.ims.feature.ImsFeature
    @SystemApi
    public boolean queryCapabilityConfiguration(int capability, int radioTech) {
        return false;
    }

    @Override // android.telephony.ims.feature.ImsFeature
    @SystemApi
    public void changeEnabledCapabilities(CapabilityChangeRequest request, ImsFeature.CapabilityCallbackProxy c) {
    }

    @SystemApi
    public ImsCallProfile createCallProfile(int callSessionType, int callType) {
        return null;
    }

    @SystemApi
    public void changeOfferedRtpHeaderExtensionTypes(Set<RtpHeaderExtensionType> extensionTypes) {
    }

    public IImsCallSession createCallSessionInterface(ImsCallProfile profile) throws RemoteException {
        ImsCallSessionImplBase s = createCallSession(profile);
        if (s != null) {
            s.setDefaultExecutor(this.mExecutor);
            return s.getServiceImpl();
        }
        return null;
    }

    @SystemApi
    public ImsCallSessionImplBase createCallSession(ImsCallProfile profile) {
        return null;
    }

    @SystemApi
    public int shouldProcessCall(String[] numbers) {
        return 0;
    }

    protected IImsUt getUtInterface() throws RemoteException {
        ImsUtImplBase utImpl = getUt();
        if (utImpl != null) {
            utImpl.setDefaultExecutor(this.mExecutor);
            return utImpl.getInterface();
        }
        return null;
    }

    protected IImsEcbm getEcbmInterface() throws RemoteException {
        ImsEcbmImplBase ecbmImpl = getEcbm();
        if (ecbmImpl != null) {
            ecbmImpl.setDefaultExecutor(this.mExecutor);
            return ecbmImpl.getImsEcbm();
        }
        return null;
    }

    public IImsMultiEndpoint getMultiEndpointInterface() throws RemoteException {
        ImsMultiEndpointImplBase multiendpointImpl = getMultiEndpoint();
        if (multiendpointImpl != null) {
            multiendpointImpl.setDefaultExecutor(this.mExecutor);
            return multiendpointImpl.getIImsMultiEndpoint();
        }
        return null;
    }

    @SystemApi
    public ImsUtImplBase getUt() {
        return new ImsUtImplBase();
    }

    @SystemApi
    public ImsEcbmImplBase getEcbm() {
        return new ImsEcbmImplBase();
    }

    @SystemApi
    public ImsMultiEndpointImplBase getMultiEndpoint() {
        return new ImsMultiEndpointImplBase();
    }

    @SystemApi
    public void setUiTtyMode(int mode, Message onCompleteMessage) {
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setSmsListener(IImsSmsListener listener) {
        getSmsImplementation().registerSmsListener(listener);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendSms(int token, int messageRef, String format, String smsc, boolean isRetry, byte[] pdu) {
        getSmsImplementation().sendSms(token, messageRef, format, smsc, isRetry, pdu);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void acknowledgeSms(int token, int messageRef, int result) {
        getSmsImplementation().acknowledgeSms(token, messageRef, result);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void acknowledgeSmsReport(int token, int messageRef, int result) {
        getSmsImplementation().acknowledgeSmsReport(token, messageRef, result);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onSmsReady() {
        getSmsImplementation().onReady();
    }

    @SystemApi
    public ImsSmsImplBase getSmsImplementation() {
        return new ImsSmsImplBase();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String getSmsFormat() {
        return getSmsImplementation().getSmsFormat();
    }

    @Override // android.telephony.ims.feature.ImsFeature
    @SystemApi
    public void onFeatureRemoved() {
    }

    @Override // android.telephony.ims.feature.ImsFeature
    @SystemApi
    public void onFeatureReady() {
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // android.telephony.ims.feature.ImsFeature
    public final IImsMmTelFeature getBinder() {
        return this.mImsMMTelBinder;
    }

    public final void setDefaultExecutor(Executor executor) {
        if (this.mExecutor == null) {
            this.mExecutor = executor;
        }
    }
}
