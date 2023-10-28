package android.telephony.ims.stub;

import android.annotation.SystemApi;
import android.app.PendingIntent$$ExternalSyntheticLambda1;
import android.os.Bundle;
import android.os.RemoteException;
import android.telephony.ims.ImsUtListener;
import android.telephony.ims.stub.ImsUtImplBase;
import android.util.Log;
import com.android.ims.internal.IImsUt;
import com.android.ims.internal.IImsUtListener;
import com.android.internal.telephony.util.TelephonyUtils;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
@SystemApi
/* loaded from: classes3.dex */
public class ImsUtImplBase {
    public static final int CALL_BARRING_ALL = 7;
    public static final int CALL_BARRING_ALL_INCOMING = 1;
    public static final int CALL_BARRING_ALL_OUTGOING = 2;
    public static final int CALL_BARRING_ANONYMOUS_INCOMING = 6;
    public static final int CALL_BARRING_INCOMING_ALL_SERVICES = 9;
    public static final int CALL_BARRING_OUTGOING_ALL_SERVICES = 8;
    public static final int CALL_BARRING_OUTGOING_INTL = 3;
    public static final int CALL_BARRING_OUTGOING_INTL_EXCL_HOME = 4;
    public static final int CALL_BARRING_SPECIFIC_INCOMING_CALLS = 10;
    public static final int CALL_BLOCKING_INCOMING_WHEN_ROAMING = 5;
    public static final int INVALID_RESULT = -1;
    private static final String TAG = "ImsUtImplBase";
    private Executor mExecutor = new PendingIntent$$ExternalSyntheticLambda1();
    private final IImsUt.Stub mServiceImpl = new AnonymousClass1();

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes3.dex */
    public @interface CallBarringMode {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: android.telephony.ims.stub.ImsUtImplBase$1  reason: invalid class name */
    /* loaded from: classes3.dex */
    public class AnonymousClass1 extends IImsUt.Stub {
        private final Object mLock = new Object();
        private ImsUtListener mUtListener;

        AnonymousClass1() {
        }

        @Override // com.android.ims.internal.IImsUt
        public void close() throws RemoteException {
            executeMethodAsync(new Runnable() { // from class: android.telephony.ims.stub.ImsUtImplBase$1$$ExternalSyntheticLambda5
                @Override // java.lang.Runnable
                public final void run() {
                    ImsUtImplBase.AnonymousClass1.this.m4519lambda$close$0$androidtelephonyimsstubImsUtImplBase$1();
                }
            }, "close");
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$close$0$android-telephony-ims-stub-ImsUtImplBase$1  reason: not valid java name */
        public /* synthetic */ void m4519lambda$close$0$androidtelephonyimsstubImsUtImplBase$1() {
            ImsUtImplBase.this.close();
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$queryCallBarring$1$android-telephony-ims-stub-ImsUtImplBase$1  reason: not valid java name */
        public /* synthetic */ Integer m4524x5f5c43db(int cbType) {
            return Integer.valueOf(ImsUtImplBase.this.queryCallBarring(cbType));
        }

        @Override // com.android.ims.internal.IImsUt
        public int queryCallBarring(final int cbType) throws RemoteException {
            return ((Integer) executeMethodAsyncForResult(new Supplier() { // from class: android.telephony.ims.stub.ImsUtImplBase$1$$ExternalSyntheticLambda7
                @Override // java.util.function.Supplier
                public final Object get() {
                    return ImsUtImplBase.AnonymousClass1.this.m4524x5f5c43db(cbType);
                }
            }, "queryCallBarring")).intValue();
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$queryCallForward$2$android-telephony-ims-stub-ImsUtImplBase$1  reason: not valid java name */
        public /* synthetic */ Integer m4526x64239a78(int condition, String number) {
            return Integer.valueOf(ImsUtImplBase.this.queryCallForward(condition, number));
        }

        @Override // com.android.ims.internal.IImsUt
        public int queryCallForward(final int condition, final String number) throws RemoteException {
            return ((Integer) executeMethodAsyncForResult(new Supplier() { // from class: android.telephony.ims.stub.ImsUtImplBase$1$$ExternalSyntheticLambda13
                @Override // java.util.function.Supplier
                public final Object get() {
                    return ImsUtImplBase.AnonymousClass1.this.m4526x64239a78(condition, number);
                }
            }, "queryCallForward")).intValue();
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$queryCallWaiting$3$android-telephony-ims-stub-ImsUtImplBase$1  reason: not valid java name */
        public /* synthetic */ Integer m4527x28161d4f() {
            return Integer.valueOf(ImsUtImplBase.this.queryCallWaiting());
        }

        @Override // com.android.ims.internal.IImsUt
        public int queryCallWaiting() throws RemoteException {
            return ((Integer) executeMethodAsyncForResult(new Supplier() { // from class: android.telephony.ims.stub.ImsUtImplBase$1$$ExternalSyntheticLambda17
                @Override // java.util.function.Supplier
                public final Object get() {
                    return ImsUtImplBase.AnonymousClass1.this.m4527x28161d4f();
                }
            }, "queryCallWaiting")).intValue();
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$queryCLIR$4$android-telephony-ims-stub-ImsUtImplBase$1  reason: not valid java name */
        public /* synthetic */ Integer m4521lambda$queryCLIR$4$androidtelephonyimsstubImsUtImplBase$1() {
            return Integer.valueOf(ImsUtImplBase.this.queryCLIR());
        }

        @Override // com.android.ims.internal.IImsUt
        public int queryCLIR() throws RemoteException {
            return ((Integer) executeMethodAsyncForResult(new Supplier() { // from class: android.telephony.ims.stub.ImsUtImplBase$1$$ExternalSyntheticLambda15
                @Override // java.util.function.Supplier
                public final Object get() {
                    return ImsUtImplBase.AnonymousClass1.this.m4521lambda$queryCLIR$4$androidtelephonyimsstubImsUtImplBase$1();
                }
            }, "queryCLIR")).intValue();
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$queryCLIP$5$android-telephony-ims-stub-ImsUtImplBase$1  reason: not valid java name */
        public /* synthetic */ Integer m4520lambda$queryCLIP$5$androidtelephonyimsstubImsUtImplBase$1() {
            return Integer.valueOf(ImsUtImplBase.this.queryCLIP());
        }

        @Override // com.android.ims.internal.IImsUt
        public int queryCLIP() throws RemoteException {
            return ((Integer) executeMethodAsyncForResult(new Supplier() { // from class: android.telephony.ims.stub.ImsUtImplBase$1$$ExternalSyntheticLambda4
                @Override // java.util.function.Supplier
                public final Object get() {
                    return ImsUtImplBase.AnonymousClass1.this.m4520lambda$queryCLIP$5$androidtelephonyimsstubImsUtImplBase$1();
                }
            }, "queryCLIP")).intValue();
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$queryCOLR$6$android-telephony-ims-stub-ImsUtImplBase$1  reason: not valid java name */
        public /* synthetic */ Integer m4523lambda$queryCOLR$6$androidtelephonyimsstubImsUtImplBase$1() {
            return Integer.valueOf(ImsUtImplBase.this.queryCOLR());
        }

        @Override // com.android.ims.internal.IImsUt
        public int queryCOLR() throws RemoteException {
            return ((Integer) executeMethodAsyncForResult(new Supplier() { // from class: android.telephony.ims.stub.ImsUtImplBase$1$$ExternalSyntheticLambda18
                @Override // java.util.function.Supplier
                public final Object get() {
                    return ImsUtImplBase.AnonymousClass1.this.m4523lambda$queryCOLR$6$androidtelephonyimsstubImsUtImplBase$1();
                }
            }, "queryCOLR")).intValue();
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$queryCOLP$7$android-telephony-ims-stub-ImsUtImplBase$1  reason: not valid java name */
        public /* synthetic */ Integer m4522lambda$queryCOLP$7$androidtelephonyimsstubImsUtImplBase$1() {
            return Integer.valueOf(ImsUtImplBase.this.queryCOLP());
        }

        @Override // com.android.ims.internal.IImsUt
        public int queryCOLP() throws RemoteException {
            return ((Integer) executeMethodAsyncForResult(new Supplier() { // from class: android.telephony.ims.stub.ImsUtImplBase$1$$ExternalSyntheticLambda12
                @Override // java.util.function.Supplier
                public final Object get() {
                    return ImsUtImplBase.AnonymousClass1.this.m4522lambda$queryCOLP$7$androidtelephonyimsstubImsUtImplBase$1();
                }
            }, "queryCOLP")).intValue();
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$transact$8$android-telephony-ims-stub-ImsUtImplBase$1  reason: not valid java name */
        public /* synthetic */ Integer m4529lambda$transact$8$androidtelephonyimsstubImsUtImplBase$1(Bundle ssInfo) {
            return Integer.valueOf(ImsUtImplBase.this.transact(ssInfo));
        }

        @Override // com.android.ims.internal.IImsUt
        public int transact(final Bundle ssInfo) throws RemoteException {
            return ((Integer) executeMethodAsyncForResult(new Supplier() { // from class: android.telephony.ims.stub.ImsUtImplBase$1$$ExternalSyntheticLambda20
                @Override // java.util.function.Supplier
                public final Object get() {
                    return ImsUtImplBase.AnonymousClass1.this.m4529lambda$transact$8$androidtelephonyimsstubImsUtImplBase$1(ssInfo);
                }
            }, "transact")).intValue();
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$updateCallBarring$9$android-telephony-ims-stub-ImsUtImplBase$1  reason: not valid java name */
        public /* synthetic */ Integer m4534xf1c0c6e(int cbType, int action, String[] barrList) {
            return Integer.valueOf(ImsUtImplBase.this.updateCallBarring(cbType, action, barrList));
        }

        @Override // com.android.ims.internal.IImsUt
        public int updateCallBarring(final int cbType, final int action, final String[] barrList) throws RemoteException {
            return ((Integer) executeMethodAsyncForResult(new Supplier() { // from class: android.telephony.ims.stub.ImsUtImplBase$1$$ExternalSyntheticLambda10
                @Override // java.util.function.Supplier
                public final Object get() {
                    return ImsUtImplBase.AnonymousClass1.this.m4534xf1c0c6e(cbType, action, barrList);
                }
            }, "updateCallBarring")).intValue();
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$updateCallForward$10$android-telephony-ims-stub-ImsUtImplBase$1  reason: not valid java name */
        public /* synthetic */ Integer m4537xe15ec1a0(int action, int condition, String number, int serviceClass, int timeSeconds) {
            return Integer.valueOf(ImsUtImplBase.this.updateCallForward(action, condition, number, serviceClass, timeSeconds));
        }

        @Override // com.android.ims.internal.IImsUt
        public int updateCallForward(final int action, final int condition, final String number, final int serviceClass, final int timeSeconds) throws RemoteException {
            return ((Integer) executeMethodAsyncForResult(new Supplier() { // from class: android.telephony.ims.stub.ImsUtImplBase$1$$ExternalSyntheticLambda0
                @Override // java.util.function.Supplier
                public final Object get() {
                    return ImsUtImplBase.AnonymousClass1.this.m4537xe15ec1a0(action, condition, number, serviceClass, timeSeconds);
                }
            }, "updateCallForward")).intValue();
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$updateCallWaiting$11$android-telephony-ims-stub-ImsUtImplBase$1  reason: not valid java name */
        public /* synthetic */ Integer m4538xc4084087(boolean enable, int serviceClass) {
            return Integer.valueOf(ImsUtImplBase.this.updateCallWaiting(enable, serviceClass));
        }

        @Override // com.android.ims.internal.IImsUt
        public int updateCallWaiting(final boolean enable, final int serviceClass) throws RemoteException {
            return ((Integer) executeMethodAsyncForResult(new Supplier() { // from class: android.telephony.ims.stub.ImsUtImplBase$1$$ExternalSyntheticLambda2
                @Override // java.util.function.Supplier
                public final Object get() {
                    return ImsUtImplBase.AnonymousClass1.this.m4538xc4084087(enable, serviceClass);
                }
            }, "updateCallWaiting")).intValue();
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$updateCLIR$12$android-telephony-ims-stub-ImsUtImplBase$1  reason: not valid java name */
        public /* synthetic */ Integer m4531lambda$updateCLIR$12$androidtelephonyimsstubImsUtImplBase$1(int clirMode) {
            return Integer.valueOf(ImsUtImplBase.this.updateCLIR(clirMode));
        }

        @Override // com.android.ims.internal.IImsUt
        public int updateCLIR(final int clirMode) throws RemoteException {
            return ((Integer) executeMethodAsyncForResult(new Supplier() { // from class: android.telephony.ims.stub.ImsUtImplBase$1$$ExternalSyntheticLambda11
                @Override // java.util.function.Supplier
                public final Object get() {
                    return ImsUtImplBase.AnonymousClass1.this.m4531lambda$updateCLIR$12$androidtelephonyimsstubImsUtImplBase$1(clirMode);
                }
            }, "updateCLIR")).intValue();
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$updateCLIP$13$android-telephony-ims-stub-ImsUtImplBase$1  reason: not valid java name */
        public /* synthetic */ Integer m4530lambda$updateCLIP$13$androidtelephonyimsstubImsUtImplBase$1(boolean enable) {
            return Integer.valueOf(ImsUtImplBase.this.updateCLIP(enable));
        }

        @Override // com.android.ims.internal.IImsUt
        public int updateCLIP(final boolean enable) throws RemoteException {
            return ((Integer) executeMethodAsyncForResult(new Supplier() { // from class: android.telephony.ims.stub.ImsUtImplBase$1$$ExternalSyntheticLambda14
                @Override // java.util.function.Supplier
                public final Object get() {
                    return ImsUtImplBase.AnonymousClass1.this.m4530lambda$updateCLIP$13$androidtelephonyimsstubImsUtImplBase$1(enable);
                }
            }, "updateCLIP")).intValue();
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$updateCOLR$14$android-telephony-ims-stub-ImsUtImplBase$1  reason: not valid java name */
        public /* synthetic */ Integer m4533lambda$updateCOLR$14$androidtelephonyimsstubImsUtImplBase$1(int presentation) {
            return Integer.valueOf(ImsUtImplBase.this.updateCOLR(presentation));
        }

        @Override // com.android.ims.internal.IImsUt
        public int updateCOLR(final int presentation) throws RemoteException {
            return ((Integer) executeMethodAsyncForResult(new Supplier() { // from class: android.telephony.ims.stub.ImsUtImplBase$1$$ExternalSyntheticLambda1
                @Override // java.util.function.Supplier
                public final Object get() {
                    return ImsUtImplBase.AnonymousClass1.this.m4533lambda$updateCOLR$14$androidtelephonyimsstubImsUtImplBase$1(presentation);
                }
            }, "updateCOLR")).intValue();
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$updateCOLP$15$android-telephony-ims-stub-ImsUtImplBase$1  reason: not valid java name */
        public /* synthetic */ Integer m4532lambda$updateCOLP$15$androidtelephonyimsstubImsUtImplBase$1(boolean enable) {
            return Integer.valueOf(ImsUtImplBase.this.updateCOLP(enable));
        }

        @Override // com.android.ims.internal.IImsUt
        public int updateCOLP(final boolean enable) throws RemoteException {
            return ((Integer) executeMethodAsyncForResult(new Supplier() { // from class: android.telephony.ims.stub.ImsUtImplBase$1$$ExternalSyntheticLambda9
                @Override // java.util.function.Supplier
                public final Object get() {
                    return ImsUtImplBase.AnonymousClass1.this.m4532lambda$updateCOLP$15$androidtelephonyimsstubImsUtImplBase$1(enable);
                }
            }, "updateCOLP")).intValue();
        }

        @Override // com.android.ims.internal.IImsUt
        public void setListener(final IImsUtListener listener) throws RemoteException {
            executeMethodAsync(new Runnable() { // from class: android.telephony.ims.stub.ImsUtImplBase$1$$ExternalSyntheticLambda19
                @Override // java.lang.Runnable
                public final void run() {
                    ImsUtImplBase.AnonymousClass1.this.m4528lambda$setListener$16$androidtelephonyimsstubImsUtImplBase$1(listener);
                }
            }, "setListener");
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$setListener$16$android-telephony-ims-stub-ImsUtImplBase$1  reason: not valid java name */
        public /* synthetic */ void m4528lambda$setListener$16$androidtelephonyimsstubImsUtImplBase$1(IImsUtListener listener) {
            ImsUtListener imsUtListener = this.mUtListener;
            if (imsUtListener != null && !imsUtListener.getListenerInterface().asBinder().isBinderAlive()) {
                Log.w(ImsUtImplBase.TAG, "setListener: discarding dead Binder");
                this.mUtListener = null;
            }
            ImsUtListener imsUtListener2 = this.mUtListener;
            if (imsUtListener2 != null && listener != null && Objects.equals(imsUtListener2.getListenerInterface().asBinder(), listener.asBinder())) {
                return;
            }
            if (listener == null) {
                this.mUtListener = null;
            } else if (listener != null && this.mUtListener == null) {
                this.mUtListener = new ImsUtListener(listener);
            } else {
                Log.w(ImsUtImplBase.TAG, "setListener is being called when there is already an active listener");
                this.mUtListener = new ImsUtListener(listener);
            }
            ImsUtImplBase.this.setListener(this.mUtListener);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$queryCallBarringForServiceClass$17$android-telephony-ims-stub-ImsUtImplBase$1  reason: not valid java name */
        public /* synthetic */ Integer m4525x843c150a(int cbType, int serviceClass) {
            return Integer.valueOf(ImsUtImplBase.this.queryCallBarringForServiceClass(cbType, serviceClass));
        }

        @Override // com.android.ims.internal.IImsUt
        public int queryCallBarringForServiceClass(final int cbType, final int serviceClass) throws RemoteException {
            return ((Integer) executeMethodAsyncForResult(new Supplier() { // from class: android.telephony.ims.stub.ImsUtImplBase$1$$ExternalSyntheticLambda8
                @Override // java.util.function.Supplier
                public final Object get() {
                    return ImsUtImplBase.AnonymousClass1.this.m4525x843c150a(cbType, serviceClass);
                }
            }, "queryCallBarringForServiceClass")).intValue();
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$updateCallBarringForServiceClass$18$android-telephony-ims-stub-ImsUtImplBase$1  reason: not valid java name */
        public /* synthetic */ Integer m4535x6fc46604(int cbType, int action, String[] barrList, int serviceClass) {
            return Integer.valueOf(ImsUtImplBase.this.updateCallBarringForServiceClass(cbType, action, barrList, serviceClass));
        }

        @Override // com.android.ims.internal.IImsUt
        public int updateCallBarringForServiceClass(final int cbType, final int action, final String[] barrList, final int serviceClass) throws RemoteException {
            return ((Integer) executeMethodAsyncForResult(new Supplier() { // from class: android.telephony.ims.stub.ImsUtImplBase$1$$ExternalSyntheticLambda21
                @Override // java.util.function.Supplier
                public final Object get() {
                    return ImsUtImplBase.AnonymousClass1.this.m4535x6fc46604(cbType, action, barrList, serviceClass);
                }
            }, "updateCallBarringForServiceClass")).intValue();
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$updateCallBarringWithPassword$19$android-telephony-ims-stub-ImsUtImplBase$1  reason: not valid java name */
        public /* synthetic */ Integer m4536x7d22d0d6(int cbType, int action, String[] barrList, int serviceClass, String password) {
            return Integer.valueOf(ImsUtImplBase.this.updateCallBarringWithPassword(cbType, action, barrList, serviceClass, password));
        }

        @Override // com.android.ims.internal.IImsUt
        public int updateCallBarringWithPassword(final int cbType, final int action, final String[] barrList, final int serviceClass, final String password) throws RemoteException {
            return ((Integer) executeMethodAsyncForResult(new Supplier() { // from class: android.telephony.ims.stub.ImsUtImplBase$1$$ExternalSyntheticLambda3
                @Override // java.util.function.Supplier
                public final Object get() {
                    return ImsUtImplBase.AnonymousClass1.this.m4536x7d22d0d6(cbType, action, barrList, serviceClass, password);
                }
            }, "updateCallBarringWithPassword")).intValue();
        }

        private void executeMethodAsync(final Runnable r, String errorLogName) throws RemoteException {
            try {
                CompletableFuture.runAsync(new Runnable() { // from class: android.telephony.ims.stub.ImsUtImplBase$1$$ExternalSyntheticLambda6
                    @Override // java.lang.Runnable
                    public final void run() {
                        TelephonyUtils.runWithCleanCallingIdentity(r);
                    }
                }, ImsUtImplBase.this.mExecutor).join();
            } catch (CancellationException | CompletionException e) {
                Log.w(ImsUtImplBase.TAG, "ImsUtImplBase Binder - " + errorLogName + " exception: " + e.getMessage());
                throw new RemoteException(e.getMessage());
            }
        }

        private <T> T executeMethodAsyncForResult(final Supplier<T> r, String errorLogName) throws RemoteException {
            CompletableFuture<T> future = CompletableFuture.supplyAsync(new Supplier() { // from class: android.telephony.ims.stub.ImsUtImplBase$1$$ExternalSyntheticLambda16
                @Override // java.util.function.Supplier
                public final Object get() {
                    Object runWithCleanCallingIdentity;
                    runWithCleanCallingIdentity = TelephonyUtils.runWithCleanCallingIdentity(r);
                    return runWithCleanCallingIdentity;
                }
            }, ImsUtImplBase.this.mExecutor);
            try {
                return future.get();
            } catch (InterruptedException | ExecutionException e) {
                Log.w(ImsUtImplBase.TAG, "ImsUtImplBase Binder - " + errorLogName + " exception: " + e.getMessage());
                throw new RemoteException(e.getMessage());
            }
        }
    }

    public void close() {
    }

    public int queryCallBarring(int cbType) {
        return -1;
    }

    public int queryCallBarringForServiceClass(int cbType, int serviceClass) {
        return -1;
    }

    public int queryCallForward(int condition, String number) {
        return -1;
    }

    public int queryCallWaiting() {
        return -1;
    }

    public int queryCLIR() {
        return queryClir();
    }

    public int queryCLIP() {
        return queryClip();
    }

    public int queryCOLR() {
        return queryColr();
    }

    public int queryCOLP() {
        return queryColp();
    }

    public int queryClir() {
        return -1;
    }

    public int queryClip() {
        return -1;
    }

    public int queryColr() {
        return -1;
    }

    public int queryColp() {
        return -1;
    }

    public int transact(Bundle ssInfo) {
        return -1;
    }

    public int updateCallBarring(int cbType, int action, String[] barrList) {
        return -1;
    }

    public int updateCallBarringForServiceClass(int cbType, int action, String[] barrList, int serviceClass) {
        return -1;
    }

    public int updateCallBarringWithPassword(int cbType, int action, String[] barrList, int serviceClass, String password) {
        return -1;
    }

    public int updateCallForward(int action, int condition, String number, int serviceClass, int timeSeconds) {
        return 0;
    }

    public int updateCallWaiting(boolean enable, int serviceClass) {
        return -1;
    }

    public int updateCLIR(int clirMode) {
        return updateClir(clirMode);
    }

    public int updateCLIP(boolean enable) {
        return updateClip(enable);
    }

    public int updateCOLR(int presentation) {
        return updateColr(presentation);
    }

    public int updateCOLP(boolean enable) {
        return updateColp(enable);
    }

    public int updateClir(int clirMode) {
        return -1;
    }

    public int updateClip(boolean enable) {
        return -1;
    }

    public int updateColr(int presentation) {
        return -1;
    }

    public int updateColp(boolean enable) {
        return -1;
    }

    public void setListener(ImsUtListener listener) {
    }

    public IImsUt getInterface() {
        return this.mServiceImpl;
    }

    public final void setDefaultExecutor(Executor executor) {
        this.mExecutor = executor;
    }
}
