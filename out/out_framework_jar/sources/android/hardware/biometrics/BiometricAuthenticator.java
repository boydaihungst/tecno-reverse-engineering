package android.hardware.biometrics;

import android.os.Parcelable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
/* loaded from: classes.dex */
public interface BiometricAuthenticator {
    public static final int TYPE_ANY_BIOMETRIC = 14;
    public static final int TYPE_CREDENTIAL = 1;
    public static final int TYPE_FACE = 8;
    public static final int TYPE_FINGERPRINT = 2;
    public static final int TYPE_IRIS = 4;
    public static final int TYPE_NONE = 0;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    public @interface Modality {
    }

    /* loaded from: classes.dex */
    public static abstract class Identifier implements Parcelable {
        private CharSequence mAppPkgName;
        private int mBiometricId;
        private long mDeviceId;
        private CharSequence mName;
        private int mSubUserId;

        public Identifier() {
        }

        public Identifier(CharSequence name, int biometricId, long deviceId) {
            this.mName = name;
            this.mBiometricId = biometricId;
            this.mDeviceId = deviceId;
            this.mAppPkgName = "";
            this.mSubUserId = 0;
        }

        public Identifier(CharSequence name, int biometricId, long deviceId, CharSequence apppkgname, int userId) {
            this.mName = name;
            this.mBiometricId = biometricId;
            this.mDeviceId = deviceId;
            if (apppkgname == null) {
                this.mAppPkgName = "";
            } else {
                this.mAppPkgName = apppkgname;
            }
            this.mSubUserId = userId;
        }

        public CharSequence getName() {
            return this.mName;
        }

        public int getBiometricId() {
            return this.mBiometricId;
        }

        public long getDeviceId() {
            return this.mDeviceId;
        }

        public void setName(CharSequence name) {
            this.mName = name;
        }

        public void setDeviceId(long deviceId) {
            this.mDeviceId = deviceId;
        }

        public CharSequence getAppPkgName() {
            return this.mAppPkgName;
        }

        public void setAppPkgName(CharSequence apppkgname) {
            this.mAppPkgName = apppkgname;
        }

        public int getSubUserId() {
            return this.mSubUserId;
        }

        public void setSubUserId(int userId) {
            this.mSubUserId = userId;
        }
    }

    /* loaded from: classes.dex */
    public static class AuthenticationResult {
        private int mAuthenticationType;
        private CryptoObject mCryptoObject;
        private Identifier mIdentifier;
        private int mUserId;

        public AuthenticationResult() {
        }

        public AuthenticationResult(CryptoObject crypto, int authenticationType, Identifier identifier, int userId) {
            this.mCryptoObject = crypto;
            this.mAuthenticationType = authenticationType;
            this.mIdentifier = identifier;
            this.mUserId = userId;
        }

        public CryptoObject getCryptoObject() {
            return this.mCryptoObject;
        }

        public int getAuthenticationType() {
            return this.mAuthenticationType;
        }

        public Identifier getId() {
            return this.mIdentifier;
        }

        public int getUserId() {
            return this.mUserId;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class AuthenticationCallback {
        public void onAuthenticationError(int errorCode, CharSequence errString) {
        }

        public void onAuthenticationHelp(int helpCode, CharSequence helpString) {
        }

        public void onAuthenticationFailed() {
        }

        public void onAuthenticationAcquired(int acquireInfo) {
        }
    }
}
