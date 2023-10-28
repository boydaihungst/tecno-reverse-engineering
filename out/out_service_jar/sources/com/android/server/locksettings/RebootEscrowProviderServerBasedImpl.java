package com.android.server.locksettings;

import android.content.Context;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.provider.DeviceConfig;
import android.util.Slog;
import com.android.server.locksettings.ResumeOnRebootServiceProvider;
import java.io.IOException;
import java.util.concurrent.TimeoutException;
import javax.crypto.SecretKey;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class RebootEscrowProviderServerBasedImpl implements RebootEscrowProviderInterface {
    private static final long DEFAULT_SERVER_BLOB_LIFETIME_IN_MILLIS = 600000;
    private static final long DEFAULT_SERVICE_TIMEOUT_IN_SECONDS = SystemProperties.getInt("server_based_service_timeout_in_seconds", 10);
    private static final String TAG = "RebootEscrowProviderServerBased";
    private final Injector mInjector;
    private byte[] mServerBlob;
    private final LockSettingsStorage mStorage;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class Injector {
        private ResumeOnRebootServiceProvider.ResumeOnRebootServiceConnection mServiceConnection;

        Injector(Context context) {
            this.mServiceConnection = null;
            ResumeOnRebootServiceProvider.ResumeOnRebootServiceConnection serviceConnection = new ResumeOnRebootServiceProvider(context).getServiceConnection();
            this.mServiceConnection = serviceConnection;
            if (serviceConnection == null) {
                Slog.e(RebootEscrowProviderServerBasedImpl.TAG, "Failed to resolve resume on reboot server service.");
            }
        }

        Injector(ResumeOnRebootServiceProvider.ResumeOnRebootServiceConnection serviceConnection) {
            this.mServiceConnection = null;
            this.mServiceConnection = serviceConnection;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public ResumeOnRebootServiceProvider.ResumeOnRebootServiceConnection getServiceConnection() {
            return this.mServiceConnection;
        }

        long getServiceTimeoutInSeconds() {
            return DeviceConfig.getLong("ota", "server_based_service_timeout_in_seconds", RebootEscrowProviderServerBasedImpl.DEFAULT_SERVICE_TIMEOUT_IN_SECONDS);
        }

        long getServerBlobLifetimeInMillis() {
            return DeviceConfig.getLong("ota", "server_based_server_blob_lifetime_in_millis", 600000L);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public RebootEscrowProviderServerBasedImpl(Context context, LockSettingsStorage storage) {
        this(storage, new Injector(context));
    }

    RebootEscrowProviderServerBasedImpl(LockSettingsStorage storage, Injector injector) {
        this.mStorage = storage;
        this.mInjector = injector;
    }

    @Override // com.android.server.locksettings.RebootEscrowProviderInterface
    public int getType() {
        return 1;
    }

    @Override // com.android.server.locksettings.RebootEscrowProviderInterface
    public boolean hasRebootEscrowSupport() {
        return this.mInjector.getServiceConnection() != null;
    }

    private byte[] unwrapServerBlob(byte[] serverBlob, SecretKey decryptionKey) throws TimeoutException, RemoteException, IOException {
        ResumeOnRebootServiceProvider.ResumeOnRebootServiceConnection serviceConnection = this.mInjector.getServiceConnection();
        if (serviceConnection == null) {
            Slog.w(TAG, "Had reboot escrow data for users, but resume on reboot server service is unavailable");
            return null;
        }
        byte[] decryptedBlob = AesEncryptionUtil.decrypt(decryptionKey, serverBlob);
        if (decryptedBlob == null) {
            Slog.w(TAG, "Decrypted server blob should not be null");
            return null;
        }
        serviceConnection.bindToService(this.mInjector.getServiceTimeoutInSeconds());
        byte[] escrowKeyBytes = serviceConnection.unwrap(decryptedBlob, this.mInjector.getServiceTimeoutInSeconds());
        serviceConnection.unbindService();
        return escrowKeyBytes;
    }

    @Override // com.android.server.locksettings.RebootEscrowProviderInterface
    public RebootEscrowKey getAndClearRebootEscrowKey(SecretKey decryptionKey) throws IOException {
        if (this.mServerBlob == null) {
            this.mServerBlob = this.mStorage.readRebootEscrowServerBlob();
        }
        this.mStorage.removeRebootEscrowServerBlob();
        if (this.mServerBlob == null) {
            Slog.w(TAG, "Failed to read reboot escrow server blob from storage");
            return null;
        } else if (decryptionKey == null) {
            Slog.w(TAG, "Failed to decrypt the escrow key; decryption key from keystore is null.");
            return null;
        } else {
            Slog.i(TAG, "Loaded reboot escrow server blob from storage");
            try {
                byte[] escrowKeyBytes = unwrapServerBlob(this.mServerBlob, decryptionKey);
                if (escrowKeyBytes == null) {
                    Slog.w(TAG, "Decrypted reboot escrow key bytes should not be null");
                    return null;
                } else if (escrowKeyBytes.length != 32) {
                    Slog.e(TAG, "Decrypted reboot escrow key has incorrect size " + escrowKeyBytes.length);
                    return null;
                } else {
                    return RebootEscrowKey.fromKeyBytes(escrowKeyBytes);
                }
            } catch (RemoteException | TimeoutException e) {
                Slog.w(TAG, "Failed to decrypt the server blob ", e);
                return null;
            }
        }
    }

    @Override // com.android.server.locksettings.RebootEscrowProviderInterface
    public void clearRebootEscrowKey() {
        this.mStorage.removeRebootEscrowServerBlob();
    }

    private byte[] wrapEscrowKey(byte[] escrowKeyBytes, SecretKey encryptionKey) throws TimeoutException, RemoteException, IOException {
        ResumeOnRebootServiceProvider.ResumeOnRebootServiceConnection serviceConnection = this.mInjector.getServiceConnection();
        if (serviceConnection == null) {
            Slog.w(TAG, "Failed to encrypt the reboot escrow key: resume on reboot server service is unavailable");
            return null;
        }
        serviceConnection.bindToService(this.mInjector.getServiceTimeoutInSeconds());
        byte[] serverEncryptedBlob = serviceConnection.wrapBlob(escrowKeyBytes, this.mInjector.getServerBlobLifetimeInMillis(), this.mInjector.getServiceTimeoutInSeconds());
        serviceConnection.unbindService();
        if (serverEncryptedBlob == null) {
            Slog.w(TAG, "Server encrypted reboot escrow key cannot be null");
            return null;
        }
        return AesEncryptionUtil.encrypt(encryptionKey, serverEncryptedBlob);
    }

    @Override // com.android.server.locksettings.RebootEscrowProviderInterface
    public boolean storeRebootEscrowKey(RebootEscrowKey escrowKey, SecretKey encryptionKey) {
        this.mStorage.removeRebootEscrowServerBlob();
        try {
            byte[] wrappedBlob = wrapEscrowKey(escrowKey.getKeyBytes(), encryptionKey);
            if (wrappedBlob == null) {
                Slog.w(TAG, "Failed to encrypt the reboot escrow key");
                return false;
            }
            this.mStorage.writeRebootEscrowServerBlob(wrappedBlob);
            Slog.i(TAG, "Reboot escrow key encrypted and stored.");
            return true;
        } catch (RemoteException | IOException | TimeoutException e) {
            Slog.w(TAG, "Failed to encrypt the reboot escrow key ", e);
            return false;
        }
    }
}
