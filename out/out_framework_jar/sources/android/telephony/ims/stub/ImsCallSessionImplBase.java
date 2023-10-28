package android.telephony.ims.stub;

import android.annotation.SystemApi;
import android.app.PendingIntent$$ExternalSyntheticLambda1;
import android.os.Message;
import android.os.RemoteException;
import android.telephony.ims.ImsCallProfile;
import android.telephony.ims.ImsCallSessionListener;
import android.telephony.ims.ImsStreamMediaProfile;
import android.telephony.ims.ImsVideoCallProvider;
import android.telephony.ims.RtpHeaderExtension;
import android.telephony.ims.aidl.IImsCallSessionListener;
import android.telephony.ims.stub.ImsCallSessionImplBase;
import android.util.ArraySet;
import android.util.Log;
import com.android.ims.internal.IImsCallSession;
import com.android.ims.internal.IImsVideoCallProvider;
import com.android.internal.telephony.util.TelephonyUtils;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
@SystemApi
/* loaded from: classes3.dex */
public class ImsCallSessionImplBase implements AutoCloseable {
    private static final String LOG_TAG = "ImsCallSessionImplBase";
    public static final int USSD_MODE_NOTIFY = 0;
    public static final int USSD_MODE_REQUEST = 1;
    private Executor mExecutor = new PendingIntent$$ExternalSyntheticLambda1();
    private IImsCallSession mServiceImpl = new AnonymousClass1();

    /* loaded from: classes3.dex */
    public static class State {
        public static final int ESTABLISHED = 4;
        public static final int ESTABLISHING = 3;
        public static final int IDLE = 0;
        public static final int INITIATED = 1;
        public static final int INVALID = -1;
        public static final int NEGOTIATING = 2;
        public static final int REESTABLISHING = 6;
        public static final int RENEGOTIATING = 5;
        public static final int TERMINATED = 8;
        public static final int TERMINATING = 7;

        public static String toString(int state) {
            switch (state) {
                case 0:
                    return "IDLE";
                case 1:
                    return "INITIATED";
                case 2:
                    return "NEGOTIATING";
                case 3:
                    return "ESTABLISHING";
                case 4:
                    return "ESTABLISHED";
                case 5:
                    return "RENEGOTIATING";
                case 6:
                    return "REESTABLISHING";
                case 7:
                    return "TERMINATING";
                case 8:
                    return "TERMINATED";
                default:
                    return "UNKNOWN";
            }
        }

        private State() {
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: android.telephony.ims.stub.ImsCallSessionImplBase$1  reason: invalid class name */
    /* loaded from: classes3.dex */
    public class AnonymousClass1 extends IImsCallSession.Stub {
        AnonymousClass1() {
        }

        @Override // com.android.ims.internal.IImsCallSession
        public void close() {
            executeMethodAsync(new Runnable() { // from class: android.telephony.ims.stub.ImsCallSessionImplBase$1$$ExternalSyntheticLambda33
                @Override // java.lang.Runnable
                public final void run() {
                    ImsCallSessionImplBase.AnonymousClass1.this.m4435x872cf5d8();
                }
            }, "close");
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$close$0$android-telephony-ims-stub-ImsCallSessionImplBase$1  reason: not valid java name */
        public /* synthetic */ void m4435x872cf5d8() {
            ImsCallSessionImplBase.this.close();
        }

        @Override // com.android.ims.internal.IImsCallSession
        public String getCallId() {
            return (String) executeMethodAsyncForResult(new Supplier() { // from class: android.telephony.ims.stub.ImsCallSessionImplBase$1$$ExternalSyntheticLambda4
                @Override // java.util.function.Supplier
                public final Object get() {
                    return ImsCallSessionImplBase.AnonymousClass1.this.m4439xdb8e2fb0();
                }
            }, "getCallId");
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$getCallId$1$android-telephony-ims-stub-ImsCallSessionImplBase$1  reason: not valid java name */
        public /* synthetic */ String m4439xdb8e2fb0() {
            return ImsCallSessionImplBase.this.getCallId();
        }

        @Override // com.android.ims.internal.IImsCallSession
        public ImsCallProfile getCallProfile() {
            return (ImsCallProfile) executeMethodAsyncForResult(new Supplier() { // from class: android.telephony.ims.stub.ImsCallSessionImplBase$1$$ExternalSyntheticLambda36
                @Override // java.util.function.Supplier
                public final Object get() {
                    return ImsCallSessionImplBase.AnonymousClass1.this.m4440x64241971();
                }
            }, "getCallProfile");
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$getCallProfile$2$android-telephony-ims-stub-ImsCallSessionImplBase$1  reason: not valid java name */
        public /* synthetic */ ImsCallProfile m4440x64241971() {
            return ImsCallSessionImplBase.this.getCallProfile();
        }

        @Override // com.android.ims.internal.IImsCallSession
        public ImsCallProfile getLocalCallProfile() {
            return (ImsCallProfile) executeMethodAsyncForResult(new Supplier() { // from class: android.telephony.ims.stub.ImsCallSessionImplBase$1$$ExternalSyntheticLambda11
                @Override // java.util.function.Supplier
                public final Object get() {
                    return ImsCallSessionImplBase.AnonymousClass1.this.m4441x70651919();
                }
            }, "getLocalCallProfile");
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$getLocalCallProfile$3$android-telephony-ims-stub-ImsCallSessionImplBase$1  reason: not valid java name */
        public /* synthetic */ ImsCallProfile m4441x70651919() {
            return ImsCallSessionImplBase.this.getLocalCallProfile();
        }

        @Override // com.android.ims.internal.IImsCallSession
        public ImsCallProfile getRemoteCallProfile() {
            return (ImsCallProfile) executeMethodAsyncForResult(new Supplier() { // from class: android.telephony.ims.stub.ImsCallSessionImplBase$1$$ExternalSyntheticLambda29
                @Override // java.util.function.Supplier
                public final Object get() {
                    return ImsCallSessionImplBase.AnonymousClass1.this.m4443x9197db8d();
                }
            }, "getRemoteCallProfile");
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$getRemoteCallProfile$4$android-telephony-ims-stub-ImsCallSessionImplBase$1  reason: not valid java name */
        public /* synthetic */ ImsCallProfile m4443x9197db8d() {
            return ImsCallSessionImplBase.this.getRemoteCallProfile();
        }

        @Override // com.android.ims.internal.IImsCallSession
        public String getProperty(final String name) {
            return (String) executeMethodAsyncForResult(new Supplier() { // from class: android.telephony.ims.stub.ImsCallSessionImplBase$1$$ExternalSyntheticLambda18
                @Override // java.util.function.Supplier
                public final Object get() {
                    return ImsCallSessionImplBase.AnonymousClass1.this.m4442xa9f04830(name);
                }
            }, "getProperty");
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$getProperty$5$android-telephony-ims-stub-ImsCallSessionImplBase$1  reason: not valid java name */
        public /* synthetic */ String m4442xa9f04830(String name) {
            return ImsCallSessionImplBase.this.getProperty(name);
        }

        @Override // com.android.ims.internal.IImsCallSession
        public int getState() {
            return ((Integer) executeMethodAsyncForResult(new Supplier() { // from class: android.telephony.ims.stub.ImsCallSessionImplBase$1$$ExternalSyntheticLambda5
                @Override // java.util.function.Supplier
                public final Object get() {
                    return ImsCallSessionImplBase.AnonymousClass1.this.m4444x2d03fb5b();
                }
            }, "getState")).intValue();
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$getState$6$android-telephony-ims-stub-ImsCallSessionImplBase$1  reason: not valid java name */
        public /* synthetic */ Integer m4444x2d03fb5b() {
            return Integer.valueOf(ImsCallSessionImplBase.this.getState());
        }

        @Override // com.android.ims.internal.IImsCallSession
        public boolean isInCall() {
            return ((Boolean) executeMethodAsyncForResult(new Supplier() { // from class: android.telephony.ims.stub.ImsCallSessionImplBase$1$$ExternalSyntheticLambda28
                @Override // java.util.function.Supplier
                public final Object get() {
                    return ImsCallSessionImplBase.AnonymousClass1.this.m4448xf3ee136e();
                }
            }, "isInCall")).booleanValue();
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$isInCall$7$android-telephony-ims-stub-ImsCallSessionImplBase$1  reason: not valid java name */
        public /* synthetic */ Boolean m4448xf3ee136e() {
            return Boolean.valueOf(ImsCallSessionImplBase.this.isInCall());
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$setListener$8$android-telephony-ims-stub-ImsCallSessionImplBase$1  reason: not valid java name */
        public /* synthetic */ void m4460xa896a65e(IImsCallSessionListener listener) {
            ImsCallSessionImplBase.this.setListener(new ImsCallSessionListener(listener));
        }

        @Override // com.android.ims.internal.IImsCallSession
        public void setListener(final IImsCallSessionListener listener) {
            executeMethodAsync(new Runnable() { // from class: android.telephony.ims.stub.ImsCallSessionImplBase$1$$ExternalSyntheticLambda35
                @Override // java.lang.Runnable
                public final void run() {
                    ImsCallSessionImplBase.AnonymousClass1.this.m4460xa896a65e(listener);
                }
            }, "setListener");
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$setMute$9$android-telephony-ims-stub-ImsCallSessionImplBase$1  reason: not valid java name */
        public /* synthetic */ void m4461x95954204(boolean muted) {
            ImsCallSessionImplBase.this.setMute(muted);
        }

        @Override // com.android.ims.internal.IImsCallSession
        public void setMute(final boolean muted) {
            executeMethodAsync(new Runnable() { // from class: android.telephony.ims.stub.ImsCallSessionImplBase$1$$ExternalSyntheticLambda21
                @Override // java.lang.Runnable
                public final void run() {
                    ImsCallSessionImplBase.AnonymousClass1.this.m4461x95954204(muted);
                }
            }, "setMute");
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$start$10$android-telephony-ims-stub-ImsCallSessionImplBase$1  reason: not valid java name */
        public /* synthetic */ void m4462xcfe9d58f(String callee, ImsCallProfile profile) {
            ImsCallSessionImplBase.this.start(callee, profile);
        }

        @Override // com.android.ims.internal.IImsCallSession
        public void start(final String callee, final ImsCallProfile profile) {
            executeMethodAsync(new Runnable() { // from class: android.telephony.ims.stub.ImsCallSessionImplBase$1$$ExternalSyntheticLambda6
                @Override // java.lang.Runnable
                public final void run() {
                    ImsCallSessionImplBase.AnonymousClass1.this.m4462xcfe9d58f(callee, profile);
                }
            }, "start");
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$startConference$11$android-telephony-ims-stub-ImsCallSessionImplBase$1  reason: not valid java name */
        public /* synthetic */ void m4463x6fc14c94(String[] participants, ImsCallProfile profile) {
            ImsCallSessionImplBase.this.startConference(participants, profile);
        }

        @Override // com.android.ims.internal.IImsCallSession
        public void startConference(final String[] participants, final ImsCallProfile profile) throws RemoteException {
            executeMethodAsync(new Runnable() { // from class: android.telephony.ims.stub.ImsCallSessionImplBase$1$$ExternalSyntheticLambda31
                @Override // java.lang.Runnable
                public final void run() {
                    ImsCallSessionImplBase.AnonymousClass1.this.m4463x6fc14c94(participants, profile);
                }
            }, "startConference");
        }

        @Override // com.android.ims.internal.IImsCallSession
        public void accept(final int callType, final ImsStreamMediaProfile profile) {
            executeMethodAsync(new Runnable() { // from class: android.telephony.ims.stub.ImsCallSessionImplBase$1$$ExternalSyntheticLambda7
                @Override // java.lang.Runnable
                public final void run() {
                    ImsCallSessionImplBase.AnonymousClass1.this.m4434xc3ecb551(callType, profile);
                }
            }, "accept");
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$accept$12$android-telephony-ims-stub-ImsCallSessionImplBase$1  reason: not valid java name */
        public /* synthetic */ void m4434xc3ecb551(int callType, ImsStreamMediaProfile profile) {
            ImsCallSessionImplBase.this.accept(callType, profile);
        }

        @Override // com.android.ims.internal.IImsCallSession
        public void deflect(final String deflectNumber) {
            executeMethodAsync(new Runnable() { // from class: android.telephony.ims.stub.ImsCallSessionImplBase$1$$ExternalSyntheticLambda32
                @Override // java.lang.Runnable
                public final void run() {
                    ImsCallSessionImplBase.AnonymousClass1.this.m4437xf463f765(deflectNumber);
                }
            }, "deflect");
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$deflect$13$android-telephony-ims-stub-ImsCallSessionImplBase$1  reason: not valid java name */
        public /* synthetic */ void m4437xf463f765(String deflectNumber) {
            ImsCallSessionImplBase.this.deflect(deflectNumber);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$reject$14$android-telephony-ims-stub-ImsCallSessionImplBase$1  reason: not valid java name */
        public /* synthetic */ void m4451xd8ad7bdc(int reason) {
            ImsCallSessionImplBase.this.reject(reason);
        }

        @Override // com.android.ims.internal.IImsCallSession
        public void reject(final int reason) {
            executeMethodAsync(new Runnable() { // from class: android.telephony.ims.stub.ImsCallSessionImplBase$1$$ExternalSyntheticLambda25
                @Override // java.lang.Runnable
                public final void run() {
                    ImsCallSessionImplBase.AnonymousClass1.this.m4451xd8ad7bdc(reason);
                }
            }, "reject");
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$transfer$15$android-telephony-ims-stub-ImsCallSessionImplBase$1  reason: not valid java name */
        public /* synthetic */ void m4467x2a0796d1(String number, boolean isConfirmationRequired) {
            ImsCallSessionImplBase.this.transfer(number, isConfirmationRequired);
        }

        @Override // com.android.ims.internal.IImsCallSession
        public void transfer(final String number, final boolean isConfirmationRequired) {
            executeMethodAsync(new Runnable() { // from class: android.telephony.ims.stub.ImsCallSessionImplBase$1$$ExternalSyntheticLambda15
                @Override // java.lang.Runnable
                public final void run() {
                    ImsCallSessionImplBase.AnonymousClass1.this.m4467x2a0796d1(number, isConfirmationRequired);
                }
            }, "transfer");
        }

        @Override // com.android.ims.internal.IImsCallSession
        public void consultativeTransfer(final IImsCallSession transferToSession) {
            executeMethodAsync(new Runnable() { // from class: android.telephony.ims.stub.ImsCallSessionImplBase$1$$ExternalSyntheticLambda12
                @Override // java.lang.Runnable
                public final void run() {
                    ImsCallSessionImplBase.AnonymousClass1.this.m4436x89324939(transferToSession);
                }
            }, "consultativeTransfer");
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$consultativeTransfer$16$android-telephony-ims-stub-ImsCallSessionImplBase$1  reason: not valid java name */
        public /* synthetic */ void m4436x89324939(IImsCallSession transferToSession) {
            ImsCallSessionImplBase otherSession = new ImsCallSessionImplBase();
            otherSession.setServiceImpl(transferToSession);
            ImsCallSessionImplBase.this.transfer(otherSession);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$terminate$17$android-telephony-ims-stub-ImsCallSessionImplBase$1  reason: not valid java name */
        public /* synthetic */ void m4466x1acafcb7(int reason) {
            ImsCallSessionImplBase.this.terminate(reason);
        }

        @Override // com.android.ims.internal.IImsCallSession
        public void terminate(final int reason) {
            executeMethodAsync(new Runnable() { // from class: android.telephony.ims.stub.ImsCallSessionImplBase$1$$ExternalSyntheticLambda13
                @Override // java.lang.Runnable
                public final void run() {
                    ImsCallSessionImplBase.AnonymousClass1.this.m4466x1acafcb7(reason);
                }
            }, "terminate");
        }

        @Override // com.android.ims.internal.IImsCallSession
        public void hold(final ImsStreamMediaProfile profile) {
            executeMethodAsync(new Runnable() { // from class: android.telephony.ims.stub.ImsCallSessionImplBase$1$$ExternalSyntheticLambda10
                @Override // java.lang.Runnable
                public final void run() {
                    ImsCallSessionImplBase.AnonymousClass1.this.m4446x3d679980(profile);
                }
            }, "hold");
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$hold$18$android-telephony-ims-stub-ImsCallSessionImplBase$1  reason: not valid java name */
        public /* synthetic */ void m4446x3d679980(ImsStreamMediaProfile profile) {
            ImsCallSessionImplBase.this.hold(profile);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$resume$19$android-telephony-ims-stub-ImsCallSessionImplBase$1  reason: not valid java name */
        public /* synthetic */ void m4453x65a21713(ImsStreamMediaProfile profile) {
            ImsCallSessionImplBase.this.resume(profile);
        }

        @Override // com.android.ims.internal.IImsCallSession
        public void resume(final ImsStreamMediaProfile profile) {
            executeMethodAsync(new Runnable() { // from class: android.telephony.ims.stub.ImsCallSessionImplBase$1$$ExternalSyntheticLambda16
                @Override // java.lang.Runnable
                public final void run() {
                    ImsCallSessionImplBase.AnonymousClass1.this.m4453x65a21713(profile);
                }
            }, "resume");
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$merge$20$android-telephony-ims-stub-ImsCallSessionImplBase$1  reason: not valid java name */
        public /* synthetic */ void m4450x777adb98() {
            ImsCallSessionImplBase.this.merge();
        }

        @Override // com.android.ims.internal.IImsCallSession
        public void merge() {
            executeMethodAsync(new Runnable() { // from class: android.telephony.ims.stub.ImsCallSessionImplBase$1$$ExternalSyntheticLambda9
                @Override // java.lang.Runnable
                public final void run() {
                    ImsCallSessionImplBase.AnonymousClass1.this.m4450x777adb98();
                }
            }, "merge");
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$update$21$android-telephony-ims-stub-ImsCallSessionImplBase$1  reason: not valid java name */
        public /* synthetic */ void m4468x9152198e(int callType, ImsStreamMediaProfile profile) {
            ImsCallSessionImplBase.this.update(callType, profile);
        }

        @Override // com.android.ims.internal.IImsCallSession
        public void update(final int callType, final ImsStreamMediaProfile profile) {
            executeMethodAsync(new Runnable() { // from class: android.telephony.ims.stub.ImsCallSessionImplBase$1$$ExternalSyntheticLambda34
                @Override // java.lang.Runnable
                public final void run() {
                    ImsCallSessionImplBase.AnonymousClass1.this.m4468x9152198e(callType, profile);
                }
            }, "update");
        }

        @Override // com.android.ims.internal.IImsCallSession
        public void extendToConference(final String[] participants) {
            executeMethodAsync(new Runnable() { // from class: android.telephony.ims.stub.ImsCallSessionImplBase$1$$ExternalSyntheticLambda14
                @Override // java.lang.Runnable
                public final void run() {
                    ImsCallSessionImplBase.AnonymousClass1.this.m4438xb76d8427(participants);
                }
            }, "extendToConference");
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$extendToConference$22$android-telephony-ims-stub-ImsCallSessionImplBase$1  reason: not valid java name */
        public /* synthetic */ void m4438xb76d8427(String[] participants) {
            ImsCallSessionImplBase.this.extendToConference(participants);
        }

        @Override // com.android.ims.internal.IImsCallSession
        public void inviteParticipants(final String[] participants) {
            executeMethodAsync(new Runnable() { // from class: android.telephony.ims.stub.ImsCallSessionImplBase$1$$ExternalSyntheticLambda26
                @Override // java.lang.Runnable
                public final void run() {
                    ImsCallSessionImplBase.AnonymousClass1.this.m4447x64c5b390(participants);
                }
            }, "inviteParticipants");
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$inviteParticipants$23$android-telephony-ims-stub-ImsCallSessionImplBase$1  reason: not valid java name */
        public /* synthetic */ void m4447x64c5b390(String[] participants) {
            ImsCallSessionImplBase.this.inviteParticipants(participants);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$removeParticipants$24$android-telephony-ims-stub-ImsCallSessionImplBase$1  reason: not valid java name */
        public /* synthetic */ void m4452x98273db6(String[] participants) {
            ImsCallSessionImplBase.this.removeParticipants(participants);
        }

        @Override // com.android.ims.internal.IImsCallSession
        public void removeParticipants(final String[] participants) {
            executeMethodAsync(new Runnable() { // from class: android.telephony.ims.stub.ImsCallSessionImplBase$1$$ExternalSyntheticLambda8
                @Override // java.lang.Runnable
                public final void run() {
                    ImsCallSessionImplBase.AnonymousClass1.this.m4452x98273db6(participants);
                }
            }, "removeParticipants");
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$sendDtmf$25$android-telephony-ims-stub-ImsCallSessionImplBase$1  reason: not valid java name */
        public /* synthetic */ void m4454xc6e62cca(char c, Message result) {
            ImsCallSessionImplBase.this.sendDtmf(c, result);
        }

        @Override // com.android.ims.internal.IImsCallSession
        public void sendDtmf(final char c, final Message result) {
            executeMethodAsync(new Runnable() { // from class: android.telephony.ims.stub.ImsCallSessionImplBase$1$$ExternalSyntheticLambda22
                @Override // java.lang.Runnable
                public final void run() {
                    ImsCallSessionImplBase.AnonymousClass1.this.m4454xc6e62cca(c, result);
                }
            }, "sendDtmf");
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$startDtmf$26$android-telephony-ims-stub-ImsCallSessionImplBase$1  reason: not valid java name */
        public /* synthetic */ void m4464x2ef32f4b(char c) {
            ImsCallSessionImplBase.this.startDtmf(c);
        }

        @Override // com.android.ims.internal.IImsCallSession
        public void startDtmf(final char c) {
            executeMethodAsync(new Runnable() { // from class: android.telephony.ims.stub.ImsCallSessionImplBase$1$$ExternalSyntheticLambda19
                @Override // java.lang.Runnable
                public final void run() {
                    ImsCallSessionImplBase.AnonymousClass1.this.m4464x2ef32f4b(c);
                }
            }, "startDtmf");
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$stopDtmf$27$android-telephony-ims-stub-ImsCallSessionImplBase$1  reason: not valid java name */
        public /* synthetic */ void m4465x93a74312() {
            ImsCallSessionImplBase.this.stopDtmf();
        }

        @Override // com.android.ims.internal.IImsCallSession
        public void stopDtmf() {
            executeMethodAsync(new Runnable() { // from class: android.telephony.ims.stub.ImsCallSessionImplBase$1$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    ImsCallSessionImplBase.AnonymousClass1.this.m4465x93a74312();
                }
            }, "stopDtmf");
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$sendUssd$28$android-telephony-ims-stub-ImsCallSessionImplBase$1  reason: not valid java name */
        public /* synthetic */ void m4459xdcc75347(String ussdMessage) {
            ImsCallSessionImplBase.this.sendUssd(ussdMessage);
        }

        @Override // com.android.ims.internal.IImsCallSession
        public void sendUssd(final String ussdMessage) {
            executeMethodAsync(new Runnable() { // from class: android.telephony.ims.stub.ImsCallSessionImplBase$1$$ExternalSyntheticLambda27
                @Override // java.lang.Runnable
                public final void run() {
                    ImsCallSessionImplBase.AnonymousClass1.this.m4459xdcc75347(ussdMessage);
                }
            }, "sendUssd");
        }

        @Override // com.android.ims.internal.IImsCallSession
        public IImsVideoCallProvider getVideoCallProvider() {
            return (IImsVideoCallProvider) executeMethodAsyncForResult(new Supplier() { // from class: android.telephony.ims.stub.ImsCallSessionImplBase$1$$ExternalSyntheticLambda23
                @Override // java.util.function.Supplier
                public final Object get() {
                    return ImsCallSessionImplBase.AnonymousClass1.this.m4445xd34d0eeb();
                }
            }, "getVideoCallProvider");
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$getVideoCallProvider$29$android-telephony-ims-stub-ImsCallSessionImplBase$1  reason: not valid java name */
        public /* synthetic */ IImsVideoCallProvider m4445xd34d0eeb() {
            return ImsCallSessionImplBase.this.getVideoCallProvider();
        }

        @Override // com.android.ims.internal.IImsCallSession
        public boolean isMultiparty() {
            return ((Boolean) executeMethodAsyncForResult(new Supplier() { // from class: android.telephony.ims.stub.ImsCallSessionImplBase$1$$ExternalSyntheticLambda1
                @Override // java.util.function.Supplier
                public final Object get() {
                    return ImsCallSessionImplBase.AnonymousClass1.this.m4449x78716b5e();
                }
            }, "isMultiparty")).booleanValue();
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$isMultiparty$30$android-telephony-ims-stub-ImsCallSessionImplBase$1  reason: not valid java name */
        public /* synthetic */ Boolean m4449x78716b5e() {
            return Boolean.valueOf(ImsCallSessionImplBase.this.isMultiparty());
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$sendRttModifyRequest$31$android-telephony-ims-stub-ImsCallSessionImplBase$1  reason: not valid java name */
        public /* synthetic */ void m4457x85967eab(ImsCallProfile toProfile) {
            ImsCallSessionImplBase.this.sendRttModifyRequest(toProfile);
        }

        @Override // com.android.ims.internal.IImsCallSession
        public void sendRttModifyRequest(final ImsCallProfile toProfile) {
            executeMethodAsync(new Runnable() { // from class: android.telephony.ims.stub.ImsCallSessionImplBase$1$$ExternalSyntheticLambda17
                @Override // java.lang.Runnable
                public final void run() {
                    ImsCallSessionImplBase.AnonymousClass1.this.m4457x85967eab(toProfile);
                }
            }, "sendRttModifyRequest");
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$sendRttModifyResponse$32$android-telephony-ims-stub-ImsCallSessionImplBase$1  reason: not valid java name */
        public /* synthetic */ void m4458xb04a754c(boolean status) {
            ImsCallSessionImplBase.this.sendRttModifyResponse(status);
        }

        @Override // com.android.ims.internal.IImsCallSession
        public void sendRttModifyResponse(final boolean status) {
            executeMethodAsync(new Runnable() { // from class: android.telephony.ims.stub.ImsCallSessionImplBase$1$$ExternalSyntheticLambda30
                @Override // java.lang.Runnable
                public final void run() {
                    ImsCallSessionImplBase.AnonymousClass1.this.m4458xb04a754c(status);
                }
            }, "sendRttModifyResponse");
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$sendRttMessage$33$android-telephony-ims-stub-ImsCallSessionImplBase$1  reason: not valid java name */
        public /* synthetic */ void m4456x8ff85f9b(String rttMessage) {
            ImsCallSessionImplBase.this.sendRttMessage(rttMessage);
        }

        @Override // com.android.ims.internal.IImsCallSession
        public void sendRttMessage(final String rttMessage) {
            executeMethodAsync(new Runnable() { // from class: android.telephony.ims.stub.ImsCallSessionImplBase$1$$ExternalSyntheticLambda24
                @Override // java.lang.Runnable
                public final void run() {
                    ImsCallSessionImplBase.AnonymousClass1.this.m4456x8ff85f9b(rttMessage);
                }
            }, "sendRttMessage");
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$sendRtpHeaderExtensions$34$android-telephony-ims-stub-ImsCallSessionImplBase$1  reason: not valid java name */
        public /* synthetic */ void m4455x566e69ec(List extensions) {
            ImsCallSessionImplBase.this.sendRtpHeaderExtensions(new ArraySet(extensions));
        }

        @Override // com.android.ims.internal.IImsCallSession
        public void sendRtpHeaderExtensions(final List<RtpHeaderExtension> extensions) {
            executeMethodAsync(new Runnable() { // from class: android.telephony.ims.stub.ImsCallSessionImplBase$1$$ExternalSyntheticLambda20
                @Override // java.lang.Runnable
                public final void run() {
                    ImsCallSessionImplBase.AnonymousClass1.this.m4455x566e69ec(extensions);
                }
            }, "sendRtpHeaderExtensions");
        }

        private void executeMethodAsync(final Runnable r, String errorLogName) {
            try {
                CompletableFuture.runAsync(new Runnable() { // from class: android.telephony.ims.stub.ImsCallSessionImplBase$1$$ExternalSyntheticLambda3
                    @Override // java.lang.Runnable
                    public final void run() {
                        TelephonyUtils.runWithCleanCallingIdentity(r);
                    }
                }, ImsCallSessionImplBase.this.mExecutor).join();
            } catch (CancellationException | CompletionException e) {
                Log.w(ImsCallSessionImplBase.LOG_TAG, "ImsCallSessionImplBase Binder - " + errorLogName + " exception: " + e.getMessage());
            }
        }

        private <T> T executeMethodAsyncForResult(final Supplier<T> r, String errorLogName) {
            CompletableFuture<T> future = CompletableFuture.supplyAsync(new Supplier() { // from class: android.telephony.ims.stub.ImsCallSessionImplBase$1$$ExternalSyntheticLambda0
                @Override // java.util.function.Supplier
                public final Object get() {
                    Object runWithCleanCallingIdentity;
                    runWithCleanCallingIdentity = TelephonyUtils.runWithCleanCallingIdentity(r);
                    return runWithCleanCallingIdentity;
                }
            }, ImsCallSessionImplBase.this.mExecutor);
            try {
                return future.get();
            } catch (InterruptedException | ExecutionException e) {
                Log.w(ImsCallSessionImplBase.LOG_TAG, "ImsCallSessionImplBase Binder - " + errorLogName + " exception: " + e.getMessage());
                return null;
            }
        }
    }

    public final void setListener(IImsCallSessionListener listener) throws RemoteException {
        setListener(new ImsCallSessionListener(listener));
    }

    public void setListener(ImsCallSessionListener listener) {
    }

    @Override // java.lang.AutoCloseable
    public void close() {
    }

    public String getCallId() {
        return null;
    }

    public ImsCallProfile getCallProfile() {
        return null;
    }

    public ImsCallProfile getLocalCallProfile() {
        return null;
    }

    public ImsCallProfile getRemoteCallProfile() {
        return null;
    }

    public String getProperty(String name) {
        return null;
    }

    public int getState() {
        return -1;
    }

    public boolean isInCall() {
        return false;
    }

    public void setMute(boolean muted) {
    }

    public void start(String callee, ImsCallProfile profile) {
    }

    public void startConference(String[] participants, ImsCallProfile profile) {
    }

    public void accept(int callType, ImsStreamMediaProfile profile) {
    }

    public void deflect(String deflectNumber) {
    }

    public void reject(int reason) {
    }

    public void transfer(String number, boolean isConfirmationRequired) {
    }

    public void transfer(ImsCallSessionImplBase otherSession) {
    }

    public void terminate(int reason) {
    }

    public void hold(ImsStreamMediaProfile profile) {
    }

    public void resume(ImsStreamMediaProfile profile) {
    }

    public void merge() {
    }

    public void update(int callType, ImsStreamMediaProfile profile) {
    }

    public void extendToConference(String[] participants) {
    }

    public void inviteParticipants(String[] participants) {
    }

    public void removeParticipants(String[] participants) {
    }

    public void sendDtmf(char c, Message result) {
    }

    public void startDtmf(char c) {
    }

    public void stopDtmf() {
    }

    public void sendUssd(String ussdMessage) {
    }

    public IImsVideoCallProvider getVideoCallProvider() {
        ImsVideoCallProvider provider = getImsVideoCallProvider();
        if (provider != null) {
            return provider.getInterface();
        }
        return null;
    }

    public ImsVideoCallProvider getImsVideoCallProvider() {
        return null;
    }

    public boolean isMultiparty() {
        return false;
    }

    public void sendRttModifyRequest(ImsCallProfile toProfile) {
    }

    public void sendRttModifyResponse(boolean status) {
    }

    public void sendRttMessage(String rttMessage) {
    }

    public void sendRtpHeaderExtensions(Set<RtpHeaderExtension> rtpHeaderExtensions) {
    }

    public IImsCallSession getServiceImpl() {
        return this.mServiceImpl;
    }

    public void setServiceImpl(IImsCallSession serviceImpl) {
        this.mServiceImpl = serviceImpl;
    }

    public final void setDefaultExecutor(Executor executor) {
        this.mExecutor = executor;
    }
}
