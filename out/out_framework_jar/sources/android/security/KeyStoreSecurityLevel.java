package android.security;

import android.app.compat.CompatChanges;
import android.hardware.security.keymint.KeyParameter;
import android.os.Binder;
import android.os.RemoteException;
import android.os.ServiceSpecificException;
import android.security.keystore.BackendBusyException;
import android.security.keystore.KeyStoreConnectException;
import android.system.keystore2.AuthenticatorSpec;
import android.system.keystore2.CreateOperationResponse;
import android.system.keystore2.IKeystoreSecurityLevel;
import android.system.keystore2.KeyDescriptor;
import android.system.keystore2.KeyMetadata;
import android.util.Log;
import java.util.Calendar;
import java.util.Collection;
/* loaded from: classes3.dex */
public class KeyStoreSecurityLevel {
    private static final String TAG = "KeyStoreSecurityLevel";
    private final IKeystoreSecurityLevel mSecurityLevel;

    public KeyStoreSecurityLevel(IKeystoreSecurityLevel securityLevel) {
        Binder.allowBlocking(securityLevel.asBinder());
        this.mSecurityLevel = securityLevel;
    }

    private <R> R handleExceptions(CheckedRemoteRequest<R> request) throws KeyStoreException {
        try {
            return request.execute();
        } catch (RemoteException e) {
            Log.e(TAG, "Could not connect to Keystore.", e);
            throw new KeyStoreException(4, "", e.getMessage());
        } catch (ServiceSpecificException e2) {
            throw KeyStore2.getKeyStoreException(e2.errorCode, e2.getMessage());
        }
    }

    public KeyStoreOperation createOperation(KeyDescriptor keyDescriptor, Collection<KeyParameter> args) throws KeyStoreException {
        while (true) {
            try {
                CreateOperationResponse createOperationResponse = this.mSecurityLevel.createOperation(keyDescriptor, (KeyParameter[]) args.toArray(new KeyParameter[args.size()]), false);
                Long challenge = null;
                if (createOperationResponse.operationChallenge != null) {
                    challenge = Long.valueOf(createOperationResponse.operationChallenge.challenge);
                }
                KeyParameter[] parameters = null;
                if (createOperationResponse.parameters != null) {
                    parameters = createOperationResponse.parameters.keyParameter;
                }
                return new KeyStoreOperation(createOperationResponse.iOperation, challenge, parameters);
            } catch (RemoteException e) {
                Log.w(TAG, "Cannot connect to keystore", e);
                throw new KeyStoreConnectException();
            } catch (ServiceSpecificException e2) {
                switch (e2.errorCode) {
                    case 18:
                        long backOffHint = (long) ((Math.random() * 80.0d) + 20.0d);
                        if (CompatChanges.isChangeEnabled(169897160L)) {
                            throw new BackendBusyException(backOffHint);
                        }
                        interruptedPreservingSleep(backOffHint);
                    default:
                        throw KeyStore2.getKeyStoreException(e2.errorCode, e2.getMessage());
                }
            }
        }
    }

    public KeyMetadata generateKey(final KeyDescriptor descriptor, final KeyDescriptor attestationKey, final Collection<KeyParameter> args, final int flags, final byte[] entropy) throws KeyStoreException {
        return (KeyMetadata) handleExceptions(new CheckedRemoteRequest() { // from class: android.security.KeyStoreSecurityLevel$$ExternalSyntheticLambda1
            @Override // android.security.CheckedRemoteRequest
            public final Object execute() {
                return KeyStoreSecurityLevel.this.m3290lambda$generateKey$0$androidsecurityKeyStoreSecurityLevel(descriptor, attestationKey, args, flags, entropy);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$generateKey$0$android-security-KeyStoreSecurityLevel  reason: not valid java name */
    public /* synthetic */ KeyMetadata m3290lambda$generateKey$0$androidsecurityKeyStoreSecurityLevel(KeyDescriptor descriptor, KeyDescriptor attestationKey, Collection args, int flags, byte[] entropy) throws RemoteException {
        return this.mSecurityLevel.generateKey(descriptor, attestationKey, (KeyParameter[]) args.toArray(new KeyParameter[args.size()]), flags, entropy);
    }

    public KeyMetadata importKey(final KeyDescriptor descriptor, final KeyDescriptor attestationKey, final Collection<KeyParameter> args, final int flags, final byte[] keyData) throws KeyStoreException {
        return (KeyMetadata) handleExceptions(new CheckedRemoteRequest() { // from class: android.security.KeyStoreSecurityLevel$$ExternalSyntheticLambda0
            @Override // android.security.CheckedRemoteRequest
            public final Object execute() {
                return KeyStoreSecurityLevel.this.m3291lambda$importKey$1$androidsecurityKeyStoreSecurityLevel(descriptor, attestationKey, args, flags, keyData);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$importKey$1$android-security-KeyStoreSecurityLevel  reason: not valid java name */
    public /* synthetic */ KeyMetadata m3291lambda$importKey$1$androidsecurityKeyStoreSecurityLevel(KeyDescriptor descriptor, KeyDescriptor attestationKey, Collection args, int flags, byte[] keyData) throws RemoteException {
        return this.mSecurityLevel.importKey(descriptor, attestationKey, (KeyParameter[]) args.toArray(new KeyParameter[args.size()]), flags, keyData);
    }

    public KeyMetadata importWrappedKey(KeyDescriptor wrappedKeyDescriptor, final KeyDescriptor wrappingKeyDescriptor, byte[] wrappedKey, final byte[] maskingKey, final Collection<KeyParameter> args, final AuthenticatorSpec[] authenticatorSpecs) throws KeyStoreException {
        final KeyDescriptor keyDescriptor = new KeyDescriptor();
        keyDescriptor.alias = wrappedKeyDescriptor.alias;
        keyDescriptor.nspace = wrappedKeyDescriptor.nspace;
        keyDescriptor.blob = wrappedKey;
        keyDescriptor.domain = wrappedKeyDescriptor.domain;
        return (KeyMetadata) handleExceptions(new CheckedRemoteRequest() { // from class: android.security.KeyStoreSecurityLevel$$ExternalSyntheticLambda2
            @Override // android.security.CheckedRemoteRequest
            public final Object execute() {
                return KeyStoreSecurityLevel.this.m3292lambda$importWrappedKey$2$androidsecurityKeyStoreSecurityLevel(keyDescriptor, wrappingKeyDescriptor, maskingKey, args, authenticatorSpecs);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$importWrappedKey$2$android-security-KeyStoreSecurityLevel  reason: not valid java name */
    public /* synthetic */ KeyMetadata m3292lambda$importWrappedKey$2$androidsecurityKeyStoreSecurityLevel(KeyDescriptor keyDescriptor, KeyDescriptor wrappingKeyDescriptor, byte[] maskingKey, Collection args, AuthenticatorSpec[] authenticatorSpecs) throws RemoteException {
        return this.mSecurityLevel.importWrappedKey(keyDescriptor, wrappingKeyDescriptor, maskingKey, (KeyParameter[]) args.toArray(new KeyParameter[args.size()]), authenticatorSpecs);
    }

    protected static void interruptedPreservingSleep(long millis) {
        boolean wasInterrupted = false;
        Calendar calendar = Calendar.getInstance();
        long target = calendar.getTimeInMillis() + millis;
        while (true) {
            try {
                Thread.sleep(target - calendar.getTimeInMillis());
                break;
            } catch (IllegalArgumentException e) {
            } catch (InterruptedException e2) {
                wasInterrupted = true;
            }
        }
        if (wasInterrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
