package android.security;

import android.hardware.security.keymint.KeyParameter;
import android.os.Binder;
import android.os.RemoteException;
import android.os.ServiceSpecificException;
import android.system.keystore2.IKeystoreOperation;
import android.util.Log;
/* loaded from: classes3.dex */
public class KeyStoreOperation {
    static final String TAG = "KeyStoreOperation";
    private final Long mChallenge;
    private final IKeystoreOperation mOperation;
    private final KeyParameter[] mParameters;

    public KeyStoreOperation(IKeystoreOperation operation, Long challenge, KeyParameter[] parameters) {
        Binder.allowBlocking(operation.asBinder());
        this.mOperation = operation;
        this.mChallenge = challenge;
        this.mParameters = parameters;
    }

    public Long getChallenge() {
        return this.mChallenge;
    }

    public KeyParameter[] getParameters() {
        return this.mParameters;
    }

    private <R> R handleExceptions(CheckedRemoteRequest<R> request) throws KeyStoreException {
        try {
            return request.execute();
        } catch (RemoteException e) {
            Log.e(TAG, "Remote exception while advancing a KeyStoreOperation.", e);
            throw new KeyStoreException(-28, "", e.getMessage());
        } catch (ServiceSpecificException e2) {
            switch (e2.errorCode) {
                case 19:
                    throw new IllegalThreadStateException("Cannot update the same operation concurrently.");
                default:
                    throw KeyStore2.getKeyStoreException(e2.errorCode, e2.getMessage());
            }
        }
    }

    public void updateAad(final byte[] input) throws KeyStoreException {
        handleExceptions(new CheckedRemoteRequest() { // from class: android.security.KeyStoreOperation$$ExternalSyntheticLambda3
            @Override // android.security.CheckedRemoteRequest
            public final Object execute() {
                return KeyStoreOperation.this.m3289lambda$updateAad$0$androidsecurityKeyStoreOperation(input);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$updateAad$0$android-security-KeyStoreOperation  reason: not valid java name */
    public /* synthetic */ Integer m3289lambda$updateAad$0$androidsecurityKeyStoreOperation(byte[] input) throws RemoteException {
        this.mOperation.updateAad(input);
        return 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$update$1$android-security-KeyStoreOperation  reason: not valid java name */
    public /* synthetic */ byte[] m3288lambda$update$1$androidsecurityKeyStoreOperation(byte[] input) throws RemoteException {
        return this.mOperation.update(input);
    }

    public byte[] update(final byte[] input) throws KeyStoreException {
        return (byte[]) handleExceptions(new CheckedRemoteRequest() { // from class: android.security.KeyStoreOperation$$ExternalSyntheticLambda1
            @Override // android.security.CheckedRemoteRequest
            public final Object execute() {
                return KeyStoreOperation.this.m3288lambda$update$1$androidsecurityKeyStoreOperation(input);
            }
        });
    }

    public byte[] finish(final byte[] input, final byte[] signature) throws KeyStoreException {
        return (byte[]) handleExceptions(new CheckedRemoteRequest() { // from class: android.security.KeyStoreOperation$$ExternalSyntheticLambda2
            @Override // android.security.CheckedRemoteRequest
            public final Object execute() {
                return KeyStoreOperation.this.m3287lambda$finish$2$androidsecurityKeyStoreOperation(input, signature);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$finish$2$android-security-KeyStoreOperation  reason: not valid java name */
    public /* synthetic */ byte[] m3287lambda$finish$2$androidsecurityKeyStoreOperation(byte[] input, byte[] signature) throws RemoteException {
        return this.mOperation.finish(input, signature);
    }

    public void abort() throws KeyStoreException {
        handleExceptions(new CheckedRemoteRequest() { // from class: android.security.KeyStoreOperation$$ExternalSyntheticLambda0
            @Override // android.security.CheckedRemoteRequest
            public final Object execute() {
                return KeyStoreOperation.this.m3286lambda$abort$3$androidsecurityKeyStoreOperation();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$abort$3$android-security-KeyStoreOperation  reason: not valid java name */
    public /* synthetic */ Integer m3286lambda$abort$3$androidsecurityKeyStoreOperation() throws RemoteException {
        this.mOperation.abort();
        return 0;
    }
}
