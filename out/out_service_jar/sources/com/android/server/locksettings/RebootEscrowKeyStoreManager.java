package com.android.server.locksettings;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore2.AndroidKeyStoreLoadStoreParameter;
import android.util.Slog;
import com.android.server.locksettings.recoverablekeystore.KeyStoreProxyImpl;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
/* loaded from: classes.dex */
public class RebootEscrowKeyStoreManager {
    private static final String ANDROID_KEY_STORE_PROVIDER = "AndroidKeystore";
    public static final int KEY_LENGTH = 256;
    private static final int KEY_STORE_NAMESPACE = 120;
    public static final String REBOOT_ESCROW_KEY_STORE_ENCRYPTION_KEY_NAME = "reboot_escrow_key_store_encryption_key";
    private static final String TAG = "RebootEscrowKeyStoreManager";
    private final Object mKeyStoreLock = new Object();

    private SecretKey getKeyStoreEncryptionKeyLocked() {
        try {
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE_PROVIDER);
            keyStore.load(new AndroidKeyStoreLoadStoreParameter(120));
            return (SecretKey) keyStore.getKey(REBOOT_ESCROW_KEY_STORE_ENCRYPTION_KEY_NAME, null);
        } catch (IOException | GeneralSecurityException e) {
            Slog.e(TAG, "Unable to get encryption key from keystore.", e);
            return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public SecretKey getKeyStoreEncryptionKey() {
        SecretKey keyStoreEncryptionKeyLocked;
        synchronized (this.mKeyStoreLock) {
            keyStoreEncryptionKeyLocked = getKeyStoreEncryptionKeyLocked();
        }
        return keyStoreEncryptionKeyLocked;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void clearKeyStoreEncryptionKey() {
        synchronized (this.mKeyStoreLock) {
            try {
                KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE_PROVIDER);
                keyStore.load(new AndroidKeyStoreLoadStoreParameter(120));
                keyStore.deleteEntry(REBOOT_ESCROW_KEY_STORE_ENCRYPTION_KEY_NAME);
            } catch (IOException | GeneralSecurityException e) {
                Slog.e(TAG, "Unable to delete encryption key in keystore.", e);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public SecretKey generateKeyStoreEncryptionKeyIfNeeded() {
        synchronized (this.mKeyStoreLock) {
            SecretKey kk = getKeyStoreEncryptionKeyLocked();
            if (kk != null) {
                return kk;
            }
            try {
                KeyGenerator generator = KeyGenerator.getInstance("AES", KeyStoreProxyImpl.ANDROID_KEY_STORE_PROVIDER);
                KeyGenParameterSpec.Builder parameterSpecBuilder = new KeyGenParameterSpec.Builder(REBOOT_ESCROW_KEY_STORE_ENCRYPTION_KEY_NAME, 3).setKeySize(256).setBlockModes("GCM").setEncryptionPaddings("NoPadding");
                parameterSpecBuilder.setNamespace(120);
                generator.init(parameterSpecBuilder.build());
                return generator.generateKey();
            } catch (GeneralSecurityException e) {
                Slog.e(TAG, "Unable to generate key from keystore.", e);
                return null;
            }
        }
    }
}
