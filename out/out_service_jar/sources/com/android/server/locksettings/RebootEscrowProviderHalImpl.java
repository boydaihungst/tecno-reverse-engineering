package com.android.server.locksettings;

import android.hardware.rebootescrow.IRebootEscrow;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.ServiceSpecificException;
import android.util.Slog;
import java.util.NoSuchElementException;
import javax.crypto.SecretKey;
/* loaded from: classes.dex */
class RebootEscrowProviderHalImpl implements RebootEscrowProviderInterface {
    private static final String TAG = "RebootEscrowProviderHal";
    private final Injector mInjector;

    /* loaded from: classes.dex */
    static class Injector {
        Injector() {
        }

        public IRebootEscrow getRebootEscrow() {
            try {
                return IRebootEscrow.Stub.asInterface(ServiceManager.getService("android.hardware.rebootescrow.IRebootEscrow/default"));
            } catch (NoSuchElementException e) {
                Slog.i(RebootEscrowProviderHalImpl.TAG, "Device doesn't implement RebootEscrow HAL");
                return null;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public RebootEscrowProviderHalImpl() {
        this.mInjector = new Injector();
    }

    RebootEscrowProviderHalImpl(Injector injector) {
        this.mInjector = injector;
    }

    @Override // com.android.server.locksettings.RebootEscrowProviderInterface
    public int getType() {
        return 0;
    }

    @Override // com.android.server.locksettings.RebootEscrowProviderInterface
    public boolean hasRebootEscrowSupport() {
        return this.mInjector.getRebootEscrow() != null;
    }

    @Override // com.android.server.locksettings.RebootEscrowProviderInterface
    public RebootEscrowKey getAndClearRebootEscrowKey(SecretKey decryptionKey) {
        IRebootEscrow rebootEscrow = this.mInjector.getRebootEscrow();
        if (rebootEscrow == null) {
            Slog.w(TAG, "Had reboot escrow data for users, but RebootEscrow HAL is unavailable");
            return null;
        }
        try {
            byte[] escrowKeyBytes = rebootEscrow.retrieveKey();
            if (escrowKeyBytes == null) {
                Slog.w(TAG, "Had reboot escrow data for users, but could not retrieve key");
                return null;
            } else if (escrowKeyBytes.length != 32) {
                Slog.e(TAG, "IRebootEscrow returned key of incorrect size " + escrowKeyBytes.length);
                return null;
            } else {
                int zero = 0;
                for (byte b : escrowKeyBytes) {
                    zero |= b;
                }
                if (zero == 0) {
                    Slog.w(TAG, "IRebootEscrow returned an all-zeroes key");
                    return null;
                }
                rebootEscrow.storeKey(new byte[32]);
                return RebootEscrowKey.fromKeyBytes(escrowKeyBytes);
            }
        } catch (RemoteException e) {
            Slog.w(TAG, "Could not retrieve escrow data");
            return null;
        } catch (ServiceSpecificException e2) {
            Slog.w(TAG, "Got service-specific exception: " + e2.errorCode);
            return null;
        }
    }

    @Override // com.android.server.locksettings.RebootEscrowProviderInterface
    public void clearRebootEscrowKey() {
        IRebootEscrow rebootEscrow = this.mInjector.getRebootEscrow();
        if (rebootEscrow == null) {
            return;
        }
        try {
            rebootEscrow.storeKey(new byte[32]);
        } catch (RemoteException | ServiceSpecificException e) {
            Slog.w(TAG, "Could not call RebootEscrow HAL to shred key");
        }
    }

    @Override // com.android.server.locksettings.RebootEscrowProviderInterface
    public boolean storeRebootEscrowKey(RebootEscrowKey escrowKey, SecretKey encryptionKey) {
        IRebootEscrow rebootEscrow = this.mInjector.getRebootEscrow();
        if (rebootEscrow == null) {
            Slog.w(TAG, "Escrow marked as ready, but RebootEscrow HAL is unavailable");
            return false;
        }
        try {
            rebootEscrow.storeKey(escrowKey.getKeyBytes());
            Slog.i(TAG, "Reboot escrow key stored with RebootEscrow HAL");
            return true;
        } catch (RemoteException | ServiceSpecificException e) {
            Slog.e(TAG, "Failed escrow secret to RebootEscrow HAL", e);
            return false;
        }
    }
}
