package android.telephony.ims.stub;

import android.annotation.SystemApi;
import android.content.Context;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.telephony.ims.RcsClientConfiguration;
import android.telephony.ims.RcsConfig;
import android.telephony.ims.aidl.IImsConfig;
import android.telephony.ims.aidl.IImsConfigCallback;
import android.telephony.ims.aidl.IRcsConfigCallback;
import android.telephony.ims.stub.ImsConfigImplBase;
import android.util.Log;
import com.android.internal.telephony.util.RemoteCallbackListExt;
import com.android.internal.telephony.util.TelephonyUtils;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
@SystemApi
/* loaded from: classes3.dex */
public class ImsConfigImplBase {
    public static final int CONFIG_RESULT_FAILED = 1;
    public static final int CONFIG_RESULT_SUCCESS = 0;
    public static final int CONFIG_RESULT_UNKNOWN = -1;
    private static final String TAG = "ImsConfigImplBase";
    private final RemoteCallbackListExt<IImsConfigCallback> mCallbacks;
    ImsConfigStub mImsConfigStub;
    private final RemoteCallbackListExt<IRcsConfigCallback> mRcsCallbacks;
    private byte[] mRcsConfigData;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes3.dex */
    public @interface SetConfigResult {
    }

    /* loaded from: classes3.dex */
    public static class ImsConfigStub extends IImsConfig.Stub {
        private Executor mExecutor;
        WeakReference<ImsConfigImplBase> mImsConfigImplBaseWeakReference;
        private HashMap<Integer, Integer> mProvisionedIntValue = new HashMap<>();
        private HashMap<Integer, String> mProvisionedStringValue = new HashMap<>();
        private final Object mLock = new Object();

        public ImsConfigStub(ImsConfigImplBase imsConfigImplBase, Executor executor) {
            this.mExecutor = executor;
            this.mImsConfigImplBaseWeakReference = new WeakReference<>(imsConfigImplBase);
        }

        @Override // android.telephony.ims.aidl.IImsConfig
        public void addImsConfigCallback(final IImsConfigCallback c) throws RemoteException {
            final AtomicReference<RemoteException> exceptionRef = new AtomicReference<>();
            executeMethodAsync(new Runnable() { // from class: android.telephony.ims.stub.ImsConfigImplBase$ImsConfigStub$$ExternalSyntheticLambda16
                @Override // java.lang.Runnable
                public final void run() {
                    ImsConfigImplBase.ImsConfigStub.this.m4482x16fb7a35(c, exceptionRef);
                }
            }, "addImsConfigCallback");
            if (exceptionRef.get() != null) {
                Log.d(ImsConfigImplBase.TAG, "ImsConfigImplBase Exception addImsConfigCallback");
                throw exceptionRef.get();
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$addImsConfigCallback$0$android-telephony-ims-stub-ImsConfigImplBase$ImsConfigStub  reason: not valid java name */
        public /* synthetic */ void m4482x16fb7a35(IImsConfigCallback c, AtomicReference exceptionRef) {
            try {
                getImsConfigImpl().addImsConfigCallback(c);
            } catch (RemoteException e) {
                exceptionRef.set(e);
            }
        }

        @Override // android.telephony.ims.aidl.IImsConfig
        public void removeImsConfigCallback(final IImsConfigCallback c) throws RemoteException {
            final AtomicReference<RemoteException> exceptionRef = new AtomicReference<>();
            executeMethodAsync(new Runnable() { // from class: android.telephony.ims.stub.ImsConfigImplBase$ImsConfigStub$$ExternalSyntheticLambda4
                @Override // java.lang.Runnable
                public final void run() {
                    ImsConfigImplBase.ImsConfigStub.this.m4490x8e1e1c91(c, exceptionRef);
                }
            }, "removeImsConfigCallback");
            if (exceptionRef.get() != null) {
                Log.d(ImsConfigImplBase.TAG, "ImsConfigImplBase Exception removeImsConfigCallback");
                throw exceptionRef.get();
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$removeImsConfigCallback$1$android-telephony-ims-stub-ImsConfigImplBase$ImsConfigStub  reason: not valid java name */
        public /* synthetic */ void m4490x8e1e1c91(IImsConfigCallback c, AtomicReference exceptionRef) {
            try {
                getImsConfigImpl().removeImsConfigCallback(c);
            } catch (RemoteException e) {
                exceptionRef.set(e);
            }
        }

        @Override // android.telephony.ims.aidl.IImsConfig
        public int getConfigInt(final int item) throws RemoteException {
            final AtomicReference<RemoteException> exceptionRef = new AtomicReference<>();
            int retVal = ((Integer) executeMethodAsyncForResult(new Supplier() { // from class: android.telephony.ims.stub.ImsConfigImplBase$ImsConfigStub$$ExternalSyntheticLambda8
                @Override // java.util.function.Supplier
                public final Object get() {
                    return ImsConfigImplBase.ImsConfigStub.this.m4484x75405151(item, exceptionRef);
                }
            }, "getConfigInt")).intValue();
            if (exceptionRef.get() != null) {
                Log.d(ImsConfigImplBase.TAG, "ImsConfigImplBase Exception getConfigString");
                throw exceptionRef.get();
            }
            return retVal;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$getConfigInt$2$android-telephony-ims-stub-ImsConfigImplBase$ImsConfigStub  reason: not valid java name */
        public /* synthetic */ Integer m4484x75405151(int item, AtomicReference exceptionRef) {
            int returnVal = -1;
            synchronized (this.mLock) {
                if (this.mProvisionedIntValue.containsKey(Integer.valueOf(item))) {
                    return this.mProvisionedIntValue.get(Integer.valueOf(item));
                }
                try {
                    returnVal = getImsConfigImpl().getConfigInt(item);
                    if (returnVal != -1) {
                        this.mProvisionedIntValue.put(Integer.valueOf(item), Integer.valueOf(returnVal));
                    }
                    return Integer.valueOf(returnVal);
                } catch (RemoteException e) {
                    exceptionRef.set(e);
                    return Integer.valueOf(returnVal);
                }
            }
        }

        @Override // android.telephony.ims.aidl.IImsConfig
        public String getConfigString(final int item) throws RemoteException {
            final AtomicReference<RemoteException> exceptionRef = new AtomicReference<>();
            String retVal = (String) executeMethodAsyncForResult(new Supplier() { // from class: android.telephony.ims.stub.ImsConfigImplBase$ImsConfigStub$$ExternalSyntheticLambda10
                @Override // java.util.function.Supplier
                public final Object get() {
                    return ImsConfigImplBase.ImsConfigStub.this.m4485xc7710678(item, exceptionRef);
                }
            }, "getConfigString");
            if (exceptionRef.get() != null) {
                Log.d(ImsConfigImplBase.TAG, "ImsConfigImplBase Exception getConfigString");
                throw exceptionRef.get();
            }
            return retVal;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$getConfigString$3$android-telephony-ims-stub-ImsConfigImplBase$ImsConfigStub  reason: not valid java name */
        public /* synthetic */ String m4485xc7710678(int item, AtomicReference exceptionRef) {
            String returnVal = null;
            synchronized (this.mLock) {
                if (this.mProvisionedStringValue.containsKey(Integer.valueOf(item))) {
                    returnVal = this.mProvisionedStringValue.get(Integer.valueOf(item));
                } else {
                    try {
                        returnVal = getImsConfigImpl().getConfigString(item);
                        if (returnVal != null) {
                            this.mProvisionedStringValue.put(Integer.valueOf(item), returnVal);
                        }
                    } catch (RemoteException e) {
                        exceptionRef.set(e);
                        return returnVal;
                    }
                }
            }
            return returnVal;
        }

        @Override // android.telephony.ims.aidl.IImsConfig
        public int setConfigInt(final int item, final int value) throws RemoteException {
            final AtomicReference<RemoteException> exceptionRef = new AtomicReference<>();
            int retVal = ((Integer) executeMethodAsyncForResult(new Supplier() { // from class: android.telephony.ims.stub.ImsConfigImplBase$ImsConfigStub$$ExternalSyntheticLambda1
                @Override // java.util.function.Supplier
                public final Object get() {
                    return ImsConfigImplBase.ImsConfigStub.this.m4492xd5d6bc1b(item, value, exceptionRef);
                }
            }, "setConfigInt")).intValue();
            if (exceptionRef.get() != null) {
                Log.d(ImsConfigImplBase.TAG, "ImsConfigImplBase Exception setConfigInt");
                throw exceptionRef.get();
            }
            return retVal;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$setConfigInt$4$android-telephony-ims-stub-ImsConfigImplBase$ImsConfigStub  reason: not valid java name */
        public /* synthetic */ Integer m4492xd5d6bc1b(int item, int value, AtomicReference exceptionRef) {
            int returnVal = -1;
            try {
                synchronized (this.mLock) {
                    this.mProvisionedIntValue.remove(Integer.valueOf(item));
                    returnVal = getImsConfigImpl().setConfig(item, value);
                    if (returnVal == 0) {
                        this.mProvisionedIntValue.put(Integer.valueOf(item), Integer.valueOf(value));
                    } else {
                        Log.d(ImsConfigImplBase.TAG, "Set provision value of " + item + " to " + value + " failed with error code " + returnVal);
                    }
                }
                notifyImsConfigChanged(item, value);
                return Integer.valueOf(returnVal);
            } catch (RemoteException e) {
                exceptionRef.set(e);
                return Integer.valueOf(returnVal);
            }
        }

        @Override // android.telephony.ims.aidl.IImsConfig
        public int setConfigString(final int item, final String value) throws RemoteException {
            final AtomicReference<RemoteException> exceptionRef = new AtomicReference<>();
            int retVal = ((Integer) executeMethodAsyncForResult(new Supplier() { // from class: android.telephony.ims.stub.ImsConfigImplBase$ImsConfigStub$$ExternalSyntheticLambda7
                @Override // java.util.function.Supplier
                public final Object get() {
                    return ImsConfigImplBase.ImsConfigStub.this.m4493xec8ebdaa(item, value, exceptionRef);
                }
            }, "setConfigString")).intValue();
            if (exceptionRef.get() != null) {
                Log.d(ImsConfigImplBase.TAG, "ImsConfigImplBase Exception setConfigInt");
                throw exceptionRef.get();
            }
            return retVal;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$setConfigString$5$android-telephony-ims-stub-ImsConfigImplBase$ImsConfigStub  reason: not valid java name */
        public /* synthetic */ Integer m4493xec8ebdaa(int item, String value, AtomicReference exceptionRef) {
            int returnVal = -1;
            try {
                synchronized (this.mLock) {
                    this.mProvisionedStringValue.remove(Integer.valueOf(item));
                    returnVal = getImsConfigImpl().setConfig(item, value);
                    if (returnVal == 0) {
                        this.mProvisionedStringValue.put(Integer.valueOf(item), value);
                    }
                }
                notifyImsConfigChanged(item, value);
                return Integer.valueOf(returnVal);
            } catch (RemoteException e) {
                exceptionRef.set(e);
                return Integer.valueOf(returnVal);
            }
        }

        @Override // android.telephony.ims.aidl.IImsConfig
        public void updateImsCarrierConfigs(final PersistableBundle bundle) throws RemoteException {
            final AtomicReference<RemoteException> exceptionRef = new AtomicReference<>();
            executeMethodAsync(new Runnable() { // from class: android.telephony.ims.stub.ImsConfigImplBase$ImsConfigStub$$ExternalSyntheticLambda9
                @Override // java.lang.Runnable
                public final void run() {
                    ImsConfigImplBase.ImsConfigStub.this.m4496x5e6ddc7f(bundle, exceptionRef);
                }
            }, "updateImsCarrierConfigs");
            if (exceptionRef.get() != null) {
                Log.d(ImsConfigImplBase.TAG, "ImsConfigImplBase Exception updateImsCarrierConfigs");
                throw exceptionRef.get();
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$updateImsCarrierConfigs$6$android-telephony-ims-stub-ImsConfigImplBase$ImsConfigStub  reason: not valid java name */
        public /* synthetic */ void m4496x5e6ddc7f(PersistableBundle bundle, AtomicReference exceptionRef) {
            try {
                getImsConfigImpl().updateImsCarrierConfigs(bundle);
            } catch (RemoteException e) {
                exceptionRef.set(e);
            }
        }

        private ImsConfigImplBase getImsConfigImpl() throws RemoteException {
            ImsConfigImplBase ref = this.mImsConfigImplBaseWeakReference.get();
            if (ref == null) {
                throw new RemoteException("Fail to get ImsConfigImpl");
            }
            return ref;
        }

        @Override // android.telephony.ims.aidl.IImsConfig
        public void notifyRcsAutoConfigurationReceived(final byte[] config, final boolean isCompressed) throws RemoteException {
            final AtomicReference<RemoteException> exceptionRef = new AtomicReference<>();
            executeMethodAsync(new Runnable() { // from class: android.telephony.ims.stub.ImsConfigImplBase$ImsConfigStub$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    ImsConfigImplBase.ImsConfigStub.this.m4487xd0220354(config, isCompressed, exceptionRef);
                }
            }, "notifyRcsAutoConfigurationReceived");
            if (exceptionRef.get() != null) {
                Log.d(ImsConfigImplBase.TAG, "ImsConfigImplBase Exception notifyRcsAutoConfigurationReceived");
                throw exceptionRef.get();
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$notifyRcsAutoConfigurationReceived$7$android-telephony-ims-stub-ImsConfigImplBase$ImsConfigStub  reason: not valid java name */
        public /* synthetic */ void m4487xd0220354(byte[] config, boolean isCompressed, AtomicReference exceptionRef) {
            try {
                getImsConfigImpl().onNotifyRcsAutoConfigurationReceived(config, isCompressed);
            } catch (RemoteException e) {
                exceptionRef.set(e);
            }
        }

        @Override // android.telephony.ims.aidl.IImsConfig
        public void notifyRcsAutoConfigurationRemoved() throws RemoteException {
            final AtomicReference<RemoteException> exceptionRef = new AtomicReference<>();
            executeMethodAsync(new Runnable() { // from class: android.telephony.ims.stub.ImsConfigImplBase$ImsConfigStub$$ExternalSyntheticLambda13
                @Override // java.lang.Runnable
                public final void run() {
                    ImsConfigImplBase.ImsConfigStub.this.m4488xb5a3aaca(exceptionRef);
                }
            }, "notifyRcsAutoConfigurationRemoved");
            if (exceptionRef.get() != null) {
                Log.d(ImsConfigImplBase.TAG, "ImsConfigImplBase Exception notifyRcsAutoConfigurationRemoved");
                throw exceptionRef.get();
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$notifyRcsAutoConfigurationRemoved$8$android-telephony-ims-stub-ImsConfigImplBase$ImsConfigStub  reason: not valid java name */
        public /* synthetic */ void m4488xb5a3aaca(AtomicReference exceptionRef) {
            try {
                getImsConfigImpl().onNotifyRcsAutoConfigurationRemoved();
            } catch (RemoteException e) {
                exceptionRef.set(e);
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void notifyImsConfigChanged(int item, int value) throws RemoteException {
            getImsConfigImpl().notifyConfigChanged(item, value);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void notifyImsConfigChanged(int item, String value) throws RemoteException {
            getImsConfigImpl().notifyConfigChanged(item, value);
        }

        protected void updateCachedValue(int item, int value) {
            synchronized (this.mLock) {
                this.mProvisionedIntValue.put(Integer.valueOf(item), Integer.valueOf(value));
            }
        }

        protected void updateCachedValue(int item, String value) {
            synchronized (this.mLock) {
                this.mProvisionedStringValue.put(Integer.valueOf(item), value);
            }
        }

        @Override // android.telephony.ims.aidl.IImsConfig
        public void addRcsConfigCallback(final IRcsConfigCallback c) throws RemoteException {
            final AtomicReference<RemoteException> exceptionRef = new AtomicReference<>();
            executeMethodAsync(new Runnable() { // from class: android.telephony.ims.stub.ImsConfigImplBase$ImsConfigStub$$ExternalSyntheticLambda14
                @Override // java.lang.Runnable
                public final void run() {
                    ImsConfigImplBase.ImsConfigStub.this.m4483x26188e19(c, exceptionRef);
                }
            }, "addRcsConfigCallback");
            if (exceptionRef.get() != null) {
                Log.d(ImsConfigImplBase.TAG, "ImsConfigImplBase Exception addRcsConfigCallback");
                throw exceptionRef.get();
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$addRcsConfigCallback$9$android-telephony-ims-stub-ImsConfigImplBase$ImsConfigStub  reason: not valid java name */
        public /* synthetic */ void m4483x26188e19(IRcsConfigCallback c, AtomicReference exceptionRef) {
            try {
                getImsConfigImpl().addRcsConfigCallback(c);
            } catch (RemoteException e) {
                exceptionRef.set(e);
            }
        }

        @Override // android.telephony.ims.aidl.IImsConfig
        public void removeRcsConfigCallback(final IRcsConfigCallback c) throws RemoteException {
            final AtomicReference<RemoteException> exceptionRef = new AtomicReference<>();
            executeMethodAsync(new Runnable() { // from class: android.telephony.ims.stub.ImsConfigImplBase$ImsConfigStub$$ExternalSyntheticLambda15
                @Override // java.lang.Runnable
                public final void run() {
                    ImsConfigImplBase.ImsConfigStub.this.m4491xfaf519f8(c, exceptionRef);
                }
            }, "removeRcsConfigCallback");
            if (exceptionRef.get() != null) {
                Log.d(ImsConfigImplBase.TAG, "ImsConfigImplBase Exception removeRcsConfigCallback");
                throw exceptionRef.get();
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$removeRcsConfigCallback$10$android-telephony-ims-stub-ImsConfigImplBase$ImsConfigStub  reason: not valid java name */
        public /* synthetic */ void m4491xfaf519f8(IRcsConfigCallback c, AtomicReference exceptionRef) {
            try {
                getImsConfigImpl().removeRcsConfigCallback(c);
            } catch (RemoteException e) {
                exceptionRef.set(e);
            }
        }

        @Override // android.telephony.ims.aidl.IImsConfig
        public void triggerRcsReconfiguration() throws RemoteException {
            final AtomicReference<RemoteException> exceptionRef = new AtomicReference<>();
            executeMethodAsync(new Runnable() { // from class: android.telephony.ims.stub.ImsConfigImplBase$ImsConfigStub$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    ImsConfigImplBase.ImsConfigStub.this.m4495x5f32b3cb(exceptionRef);
                }
            }, "triggerRcsReconfiguration");
            if (exceptionRef.get() != null) {
                Log.d(ImsConfigImplBase.TAG, "ImsConfigImplBase Exception triggerRcsReconfiguration");
                throw exceptionRef.get();
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$triggerRcsReconfiguration$11$android-telephony-ims-stub-ImsConfigImplBase$ImsConfigStub  reason: not valid java name */
        public /* synthetic */ void m4495x5f32b3cb(AtomicReference exceptionRef) {
            try {
                getImsConfigImpl().triggerAutoConfiguration();
            } catch (RemoteException e) {
                exceptionRef.set(e);
            }
        }

        @Override // android.telephony.ims.aidl.IImsConfig
        public void setRcsClientConfiguration(final RcsClientConfiguration rcc) throws RemoteException {
            final AtomicReference<RemoteException> exceptionRef = new AtomicReference<>();
            executeMethodAsync(new Runnable() { // from class: android.telephony.ims.stub.ImsConfigImplBase$ImsConfigStub$$ExternalSyntheticLambda11
                @Override // java.lang.Runnable
                public final void run() {
                    ImsConfigImplBase.ImsConfigStub.this.m4494xfdf9f51c(rcc, exceptionRef);
                }
            }, "setRcsClientConfiguration");
            if (exceptionRef.get() != null) {
                Log.d(ImsConfigImplBase.TAG, "ImsConfigImplBase Exception setRcsClientConfiguration");
                throw exceptionRef.get();
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$setRcsClientConfiguration$12$android-telephony-ims-stub-ImsConfigImplBase$ImsConfigStub  reason: not valid java name */
        public /* synthetic */ void m4494xfdf9f51c(RcsClientConfiguration rcc, AtomicReference exceptionRef) {
            try {
                getImsConfigImpl().setRcsClientConfiguration(rcc);
            } catch (RemoteException e) {
                exceptionRef.set(e);
            }
        }

        @Override // android.telephony.ims.aidl.IImsConfig
        public void notifyIntImsConfigChanged(final int item, final int value) throws RemoteException {
            final AtomicReference<RemoteException> exceptionRef = new AtomicReference<>();
            executeMethodAsync(new Runnable() { // from class: android.telephony.ims.stub.ImsConfigImplBase$ImsConfigStub$$ExternalSyntheticLambda12
                @Override // java.lang.Runnable
                public final void run() {
                    ImsConfigImplBase.ImsConfigStub.this.m4486x9e6f2359(item, value, exceptionRef);
                }
            }, "notifyIntImsConfigChanged");
            if (exceptionRef.get() != null) {
                Log.d(ImsConfigImplBase.TAG, "ImsConfigImplBase Exception notifyIntImsConfigChanged");
                throw exceptionRef.get();
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$notifyIntImsConfigChanged$13$android-telephony-ims-stub-ImsConfigImplBase$ImsConfigStub  reason: not valid java name */
        public /* synthetic */ void m4486x9e6f2359(int item, int value, AtomicReference exceptionRef) {
            try {
                notifyImsConfigChanged(item, value);
            } catch (RemoteException e) {
                exceptionRef.set(e);
            }
        }

        @Override // android.telephony.ims.aidl.IImsConfig
        public void notifyStringImsConfigChanged(final int item, final String value) throws RemoteException {
            final AtomicReference<RemoteException> exceptionRef = new AtomicReference<>();
            executeMethodAsync(new Runnable() { // from class: android.telephony.ims.stub.ImsConfigImplBase$ImsConfigStub$$ExternalSyntheticLambda3
                @Override // java.lang.Runnable
                public final void run() {
                    ImsConfigImplBase.ImsConfigStub.this.m4489xa6e44966(item, value, exceptionRef);
                }
            }, "notifyStringImsConfigChanged");
            if (exceptionRef.get() != null) {
                Log.d(ImsConfigImplBase.TAG, "ImsConfigImplBase Exception notifyStringImsConfigChanged");
                throw exceptionRef.get();
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$notifyStringImsConfigChanged$14$android-telephony-ims-stub-ImsConfigImplBase$ImsConfigStub  reason: not valid java name */
        public /* synthetic */ void m4489xa6e44966(int item, String value, AtomicReference exceptionRef) {
            try {
                notifyImsConfigChanged(item, value);
            } catch (RemoteException e) {
                exceptionRef.set(e);
            }
        }

        private void executeMethodAsync(final Runnable r, String errorLogName) throws RemoteException {
            try {
                CompletableFuture.runAsync(new Runnable() { // from class: android.telephony.ims.stub.ImsConfigImplBase$ImsConfigStub$$ExternalSyntheticLambda6
                    @Override // java.lang.Runnable
                    public final void run() {
                        TelephonyUtils.runWithCleanCallingIdentity(r);
                    }
                }, this.mExecutor).join();
            } catch (CancellationException | CompletionException e) {
                Log.w(ImsConfigImplBase.TAG, "ImsConfigImplBase Binder - " + errorLogName + " exception: " + e.getMessage());
                throw new RemoteException(e.getMessage());
            }
        }

        private <T> T executeMethodAsyncForResult(final Supplier<T> r, String errorLogName) throws RemoteException {
            CompletableFuture<T> future = CompletableFuture.supplyAsync(new Supplier() { // from class: android.telephony.ims.stub.ImsConfigImplBase$ImsConfigStub$$ExternalSyntheticLambda5
                @Override // java.util.function.Supplier
                public final Object get() {
                    Object runWithCleanCallingIdentity;
                    runWithCleanCallingIdentity = TelephonyUtils.runWithCleanCallingIdentity(r);
                    return runWithCleanCallingIdentity;
                }
            }, this.mExecutor);
            try {
                return future.get();
            } catch (InterruptedException | ExecutionException e) {
                Log.w(ImsConfigImplBase.TAG, "ImsConfigImplBase Binder - " + errorLogName + " exception: " + e.getMessage());
                throw new RemoteException(e.getMessage());
            }
        }
    }

    public ImsConfigImplBase(Executor executor) {
        this.mCallbacks = new RemoteCallbackListExt<>();
        this.mRcsCallbacks = new RemoteCallbackListExt<>();
        this.mImsConfigStub = new ImsConfigStub(this, executor);
    }

    public ImsConfigImplBase(Context context) {
        this.mCallbacks = new RemoteCallbackListExt<>();
        this.mRcsCallbacks = new RemoteCallbackListExt<>();
        this.mImsConfigStub = new ImsConfigStub(this, null);
    }

    public ImsConfigImplBase() {
        this.mCallbacks = new RemoteCallbackListExt<>();
        this.mRcsCallbacks = new RemoteCallbackListExt<>();
        this.mImsConfigStub = new ImsConfigStub(this, null);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void addImsConfigCallback(IImsConfigCallback c) {
        this.mCallbacks.register(c);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void removeImsConfigCallback(IImsConfigCallback c) {
        this.mCallbacks.unregister(c);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void notifyConfigChanged(final int item, final int value) {
        RemoteCallbackListExt<IImsConfigCallback> remoteCallbackListExt = this.mCallbacks;
        if (remoteCallbackListExt == null) {
            return;
        }
        synchronized (remoteCallbackListExt) {
            this.mCallbacks.broadcastAction(new Consumer() { // from class: android.telephony.ims.stub.ImsConfigImplBase$$ExternalSyntheticLambda4
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ImsConfigImplBase.lambda$notifyConfigChanged$0(item, value, (IImsConfigCallback) obj);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$notifyConfigChanged$0(int item, int value, IImsConfigCallback c) {
        try {
            c.onIntConfigChanged(item, value);
        } catch (RemoteException e) {
            Log.w(TAG, "notifyConfigChanged(int): dead binder in notify, skipping.");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyConfigChanged(final int item, final String value) {
        RemoteCallbackListExt<IImsConfigCallback> remoteCallbackListExt = this.mCallbacks;
        if (remoteCallbackListExt == null) {
            return;
        }
        synchronized (remoteCallbackListExt) {
            this.mCallbacks.broadcastAction(new Consumer() { // from class: android.telephony.ims.stub.ImsConfigImplBase$$ExternalSyntheticLambda3
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ImsConfigImplBase.lambda$notifyConfigChanged$1(item, value, (IImsConfigCallback) obj);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$notifyConfigChanged$1(int item, String value, IImsConfigCallback c) {
        try {
            c.onStringConfigChanged(item, value);
        } catch (RemoteException e) {
            Log.w(TAG, "notifyConfigChanged(string): dead binder in notify, skipping.");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void addRcsConfigCallback(IRcsConfigCallback c) {
        this.mRcsCallbacks.register(c);
        byte[] bArr = this.mRcsConfigData;
        if (bArr != null) {
            try {
                c.onConfigurationChanged(bArr);
            } catch (RemoteException e) {
                Log.w(TAG, "dead binder to call onConfigurationChanged, skipping.");
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void removeRcsConfigCallback(IRcsConfigCallback c) {
        this.mRcsCallbacks.unregister(c);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onNotifyRcsAutoConfigurationReceived(byte[] config, boolean isCompressed) {
        byte[] config2 = isCompressed ? RcsConfig.decompressGzip(config) : config;
        if (Arrays.equals(this.mRcsConfigData, config2)) {
            return;
        }
        this.mRcsConfigData = config2;
        RemoteCallbackListExt<IRcsConfigCallback> remoteCallbackListExt = this.mRcsCallbacks;
        if (remoteCallbackListExt != null) {
            synchronized (remoteCallbackListExt) {
                this.mRcsCallbacks.broadcastAction(new Consumer() { // from class: android.telephony.ims.stub.ImsConfigImplBase$$ExternalSyntheticLambda5
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        ImsConfigImplBase.this.m4477x4b7d1f9f((IRcsConfigCallback) obj);
                    }
                });
            }
        }
        notifyRcsAutoConfigurationReceived(config2, isCompressed);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onNotifyRcsAutoConfigurationReceived$2$android-telephony-ims-stub-ImsConfigImplBase  reason: not valid java name */
    public /* synthetic */ void m4477x4b7d1f9f(IRcsConfigCallback c) {
        try {
            c.onConfigurationChanged(this.mRcsConfigData);
        } catch (RemoteException e) {
            Log.w(TAG, "dead binder in notifyRcsAutoConfigurationReceived, skipping.");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onNotifyRcsAutoConfigurationRemoved() {
        this.mRcsConfigData = null;
        RemoteCallbackListExt<IRcsConfigCallback> remoteCallbackListExt = this.mRcsCallbacks;
        if (remoteCallbackListExt != null) {
            synchronized (remoteCallbackListExt) {
                this.mRcsCallbacks.broadcastAction(new Consumer() { // from class: android.telephony.ims.stub.ImsConfigImplBase$$ExternalSyntheticLambda1
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        ImsConfigImplBase.lambda$onNotifyRcsAutoConfigurationRemoved$3((IRcsConfigCallback) obj);
                    }
                });
            }
        }
        notifyRcsAutoConfigurationRemoved();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$onNotifyRcsAutoConfigurationRemoved$3(IRcsConfigCallback c) {
        try {
            c.onConfigurationReset();
        } catch (RemoteException e) {
            Log.w(TAG, "dead binder in notifyRcsAutoConfigurationRemoved, skipping.");
        }
    }

    public IImsConfig getIImsConfig() {
        return this.mImsConfigStub;
    }

    public final void notifyProvisionedValueChanged(int item, int value) {
        this.mImsConfigStub.updateCachedValue(item, value);
        try {
            this.mImsConfigStub.notifyImsConfigChanged(item, value);
        } catch (RemoteException e) {
            Log.w(TAG, "notifyProvisionedValueChanged(int): Framework connection is dead.");
        }
    }

    public final void notifyProvisionedValueChanged(int item, String value) {
        this.mImsConfigStub.updateCachedValue(item, value);
        try {
            this.mImsConfigStub.notifyImsConfigChanged(item, value);
        } catch (RemoteException e) {
            Log.w(TAG, "notifyProvisionedValueChanged(string): Framework connection is dead.");
        }
    }

    public void notifyRcsAutoConfigurationReceived(byte[] config, boolean isCompressed) {
    }

    public void notifyRcsAutoConfigurationRemoved() {
    }

    public int setConfig(int item, int value) {
        return 1;
    }

    public int setConfig(int item, String value) {
        return 1;
    }

    public int getConfigInt(int item) {
        return -1;
    }

    public String getConfigString(int item) {
        return null;
    }

    public void updateImsCarrierConfigs(PersistableBundle bundle) {
    }

    public void setRcsClientConfiguration(RcsClientConfiguration rcc) {
    }

    public void triggerAutoConfiguration() {
    }

    public final void notifyAutoConfigurationErrorReceived(final int errorCode, final String errorString) {
        RemoteCallbackListExt<IRcsConfigCallback> remoteCallbackListExt = this.mRcsCallbacks;
        if (remoteCallbackListExt == null) {
            return;
        }
        synchronized (remoteCallbackListExt) {
            this.mRcsCallbacks.broadcastAction(new Consumer() { // from class: android.telephony.ims.stub.ImsConfigImplBase$$ExternalSyntheticLambda2
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ImsConfigImplBase.lambda$notifyAutoConfigurationErrorReceived$4(errorCode, errorString, (IRcsConfigCallback) obj);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$notifyAutoConfigurationErrorReceived$4(int errorCode, String errorString, IRcsConfigCallback c) {
        try {
            c.onAutoConfigurationErrorReceived(errorCode, errorString);
        } catch (RemoteException e) {
            Log.w(TAG, "dead binder in notifyAutoConfigurationErrorReceived, skipping.");
        }
    }

    public final void notifyPreProvisioningReceived(final byte[] configXml) {
        RemoteCallbackListExt<IRcsConfigCallback> remoteCallbackListExt = this.mRcsCallbacks;
        if (remoteCallbackListExt == null) {
            return;
        }
        synchronized (remoteCallbackListExt) {
            this.mRcsCallbacks.broadcastAction(new Consumer() { // from class: android.telephony.ims.stub.ImsConfigImplBase$$ExternalSyntheticLambda0
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ImsConfigImplBase.lambda$notifyPreProvisioningReceived$5(configXml, (IRcsConfigCallback) obj);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$notifyPreProvisioningReceived$5(byte[] configXml, IRcsConfigCallback c) {
        try {
            c.onPreProvisioningReceived(configXml);
        } catch (RemoteException e) {
            Log.w(TAG, "dead binder in notifyPreProvisioningReceived, skipping.");
        }
    }

    public final void setDefaultExecutor(Executor executor) {
        if (this.mImsConfigStub.mExecutor == null) {
            this.mImsConfigStub.mExecutor = executor;
        }
    }
}
