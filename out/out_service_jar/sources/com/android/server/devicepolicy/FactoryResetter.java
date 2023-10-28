package com.android.server.devicepolicy;

import android.app.admin.DevicePolicySafetyChecker;
import android.content.Context;
import android.os.Bundle;
import android.os.RecoverySystem;
import android.os.RemoteException;
import android.os.UserManager;
import android.os.storage.StorageManager;
import android.service.persistentdata.PersistentDataBlockManager;
import com.android.internal.os.IResultReceiver;
import com.android.internal.util.Preconditions;
import com.android.server.utils.Slogf;
import java.io.IOException;
import java.util.Objects;
@Deprecated
/* loaded from: classes.dex */
public final class FactoryResetter {
    private static final String TAG = FactoryResetter.class.getSimpleName();
    private final Context mContext;
    private final boolean mForce;
    private final String mReason;
    private final DevicePolicySafetyChecker mSafetyChecker;
    private final boolean mShutdown;
    private final boolean mWipeAdoptableStorage;
    private final boolean mWipeEuicc;
    private final boolean mWipeFactoryResetProtection;

    public boolean factoryReset() throws IOException {
        Preconditions.checkCallAuthorization(this.mContext.checkCallingOrSelfPermission("android.permission.MASTER_CLEAR") == 0);
        com.android.server.FactoryResetter.setFactoryResetting(this.mContext);
        if (this.mSafetyChecker == null) {
            factoryResetInternalUnchecked();
            return true;
        }
        IResultReceiver.Stub stub = new IResultReceiver.Stub() { // from class: com.android.server.devicepolicy.FactoryResetter.1
            public void send(int resultCode, Bundle resultData) throws RemoteException {
                Slogf.i(FactoryResetter.TAG, "Factory reset confirmed by %s, proceeding", FactoryResetter.this.mSafetyChecker);
                try {
                    FactoryResetter.this.factoryResetInternalUnchecked();
                } catch (IOException e) {
                    Slogf.wtf(FactoryResetter.TAG, e, "IOException calling underlying systems", new Object[0]);
                }
            }
        };
        Slogf.i(TAG, "Delaying factory reset until %s confirms", this.mSafetyChecker);
        this.mSafetyChecker.onFactoryReset(stub);
        return false;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder("FactoryResetter[");
        if (this.mReason == null) {
            builder.append("no_reason");
        } else {
            builder.append("reason='").append(this.mReason).append("'");
        }
        if (this.mSafetyChecker != null) {
            builder.append(",hasSafetyChecker");
        }
        if (this.mShutdown) {
            builder.append(",shutdown");
        }
        if (this.mForce) {
            builder.append(",force");
        }
        if (this.mWipeEuicc) {
            builder.append(",wipeEuicc");
        }
        if (this.mWipeAdoptableStorage) {
            builder.append(",wipeAdoptableStorage");
        }
        if (this.mWipeFactoryResetProtection) {
            builder.append(",ipeFactoryResetProtection");
        }
        return builder.append(']').toString();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void factoryResetInternalUnchecked() throws IOException {
        String str = TAG;
        Slogf.i(str, "factoryReset(): reason=%s, shutdown=%b, force=%b, wipeEuicc=%b, wipeAdoptableStorage=%b, wipeFRP=%b", this.mReason, Boolean.valueOf(this.mShutdown), Boolean.valueOf(this.mForce), Boolean.valueOf(this.mWipeEuicc), Boolean.valueOf(this.mWipeAdoptableStorage), Boolean.valueOf(this.mWipeFactoryResetProtection));
        UserManager um = (UserManager) this.mContext.getSystemService(UserManager.class);
        if (!this.mForce && um.hasUserRestriction("no_factory_reset")) {
            throw new SecurityException("Factory reset is not allowed for this user.");
        }
        if (this.mWipeFactoryResetProtection) {
            PersistentDataBlockManager manager = (PersistentDataBlockManager) this.mContext.getSystemService(PersistentDataBlockManager.class);
            if (manager != null) {
                Slogf.w(str, "Wiping factory reset protection");
                manager.wipe();
            } else {
                Slogf.w(str, "No need to wipe factory reset protection");
            }
        }
        if (this.mWipeAdoptableStorage) {
            Slogf.w(str, "Wiping adoptable storage");
            StorageManager sm = (StorageManager) this.mContext.getSystemService(StorageManager.class);
            sm.wipeAdoptableDisks();
        }
        RecoverySystem.rebootWipeUserData(this.mContext, this.mShutdown, this.mReason, this.mForce, this.mWipeEuicc);
    }

    private FactoryResetter(Builder builder) {
        this.mContext = builder.mContext;
        this.mSafetyChecker = builder.mSafetyChecker;
        this.mReason = builder.mReason;
        this.mShutdown = builder.mShutdown;
        this.mForce = builder.mForce;
        this.mWipeEuicc = builder.mWipeEuicc;
        this.mWipeAdoptableStorage = builder.mWipeAdoptableStorage;
        this.mWipeFactoryResetProtection = builder.mWipeFactoryResetProtection;
    }

    public static Builder newBuilder(Context context) {
        return new Builder(context);
    }

    /* loaded from: classes.dex */
    public static final class Builder {
        private final Context mContext;
        private boolean mForce;
        private String mReason;
        private DevicePolicySafetyChecker mSafetyChecker;
        private boolean mShutdown;
        private boolean mWipeAdoptableStorage;
        private boolean mWipeEuicc;
        private boolean mWipeFactoryResetProtection;

        private Builder(Context context) {
            this.mContext = (Context) Objects.requireNonNull(context);
        }

        public Builder setSafetyChecker(DevicePolicySafetyChecker safetyChecker) {
            this.mSafetyChecker = safetyChecker;
            return this;
        }

        public Builder setReason(String reason) {
            this.mReason = (String) Objects.requireNonNull(reason);
            return this;
        }

        public Builder setShutdown(boolean value) {
            this.mShutdown = value;
            return this;
        }

        public Builder setForce(boolean value) {
            this.mForce = value;
            return this;
        }

        public Builder setWipeEuicc(boolean value) {
            this.mWipeEuicc = value;
            return this;
        }

        public Builder setWipeAdoptableStorage(boolean value) {
            this.mWipeAdoptableStorage = value;
            return this;
        }

        public Builder setWipeFactoryResetProtection(boolean value) {
            this.mWipeFactoryResetProtection = value;
            return this;
        }

        public FactoryResetter build() {
            return new FactoryResetter(this);
        }
    }
}
