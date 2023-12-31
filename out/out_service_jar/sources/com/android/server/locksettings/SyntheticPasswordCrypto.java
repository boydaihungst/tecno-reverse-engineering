package com.android.server.locksettings;

import android.app.ActivityManager;
import android.os.Debug;
import android.os.RemoteException;
import android.security.AndroidKeyStoreMaintenance;
import android.security.keystore.KeyProtection;
import android.security.keystore2.AndroidKeyStoreLoadStoreParameter;
import android.system.keystore2.KeyDescriptor;
import android.util.Log;
import android.util.Slog;
import com.android.server.locksettings.recoverablekeystore.KeyStoreProxyImpl;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidParameterSpecException;
import java.util.Arrays;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
/* loaded from: classes.dex */
public class SyntheticPasswordCrypto {
    private static final int AES_KEY_LENGTH = 32;
    private static final byte[] APPLICATION_ID_PERSONALIZATION = "application-id".getBytes();
    private static final int DEFAULT_TAG_LENGTH_BITS = 128;
    private static final int PROFILE_KEY_IV_SIZE = 12;
    private static final String TAG = "SyntheticPasswordCrypto";
    private static final int USER_AUTHENTICATION_VALIDITY = 15;

    private static byte[] decrypt(SecretKey key, byte[] blob) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
        if (blob == null) {
            return null;
        }
        byte[] iv = Arrays.copyOfRange(blob, 0, 12);
        byte[] ciphertext = Arrays.copyOfRange(blob, 12, blob.length);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(2, key, new GCMParameterSpec(128, iv));
        return cipher.doFinal(ciphertext);
    }

    private static byte[] encrypt(SecretKey key, byte[] blob) throws IOException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, InvalidParameterSpecException {
        if (blob == null) {
            return null;
        }
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(1, key);
        byte[] ciphertext = cipher.doFinal(blob);
        byte[] iv = cipher.getIV();
        if (iv.length != 12) {
            throw new IllegalArgumentException("Invalid iv length: " + iv.length);
        }
        GCMParameterSpec spec = (GCMParameterSpec) cipher.getParameters().getParameterSpec(GCMParameterSpec.class);
        if (spec.getTLen() != 128) {
            throw new IllegalArgumentException("Invalid tag length: " + spec.getTLen());
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(iv);
        outputStream.write(ciphertext);
        return outputStream.toByteArray();
    }

    public static byte[] encrypt(byte[] keyBytes, byte[] personalisation, byte[] message) {
        byte[] keyHash = personalisedHash(personalisation, keyBytes);
        SecretKeySpec key = new SecretKeySpec(Arrays.copyOf(keyHash, 32), "AES");
        try {
            return encrypt(key, message);
        } catch (IOException | InvalidKeyException | NoSuchAlgorithmException | InvalidParameterSpecException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException e) {
            Slog.e(TAG, "Failed to encrypt", e);
            return null;
        }
    }

    public static byte[] decrypt(byte[] keyBytes, byte[] personalisation, byte[] ciphertext) {
        byte[] keyHash = personalisedHash(personalisation, keyBytes);
        SecretKeySpec key = new SecretKeySpec(Arrays.copyOf(keyHash, 32), "AES");
        try {
            return decrypt(key, ciphertext);
        } catch (InvalidAlgorithmParameterException | InvalidKeyException | NoSuchAlgorithmException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException e) {
            Slog.e(TAG, "Failed to decrypt", e);
            return null;
        }
    }

    public static byte[] decryptBlobV1(String keyAlias, byte[] blob, byte[] applicationId) {
        try {
            KeyStore keyStore = getKeyStore();
            SecretKey decryptionKey = (SecretKey) keyStore.getKey(keyAlias, null);
            if (decryptionKey == null) {
                throw new IllegalStateException("SP key is missing: " + keyAlias);
            }
            byte[] intermediate = decrypt(applicationId, APPLICATION_ID_PERSONALIZATION, blob);
            return decrypt(decryptionKey, intermediate);
        } catch (Exception e) {
            Slog.e(TAG, "Failed to decrypt V1 blob", e);
            throw new IllegalStateException("Failed to decrypt blob", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static String androidKeystoreProviderName() {
        return KeyStoreProxyImpl.ANDROID_KEY_STORE_PROVIDER;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static int keyNamespace() {
        return 103;
    }

    private static KeyStore getKeyStore() throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
        KeyStore keyStore = KeyStore.getInstance(androidKeystoreProviderName());
        keyStore.load(new AndroidKeyStoreLoadStoreParameter(keyNamespace()));
        return keyStore;
    }

    public static byte[] decryptBlob(String keyAlias, byte[] blob, byte[] applicationId) {
        try {
            KeyStore keyStore = getKeyStore();
            SecretKey decryptionKey = (SecretKey) keyStore.getKey(keyAlias, null);
            if (decryptionKey == null) {
                throw new IllegalStateException("SP key is missing: " + keyAlias);
            }
            byte[] intermediate = decrypt(decryptionKey, blob);
            return decrypt(applicationId, APPLICATION_ID_PERSONALIZATION, intermediate);
        } catch (IOException | InvalidAlgorithmParameterException | InvalidKeyException | KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException | CertificateException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException e) {
            Slog.e(TAG, "Failed to decrypt blob", e);
            throw new IllegalStateException("Failed to decrypt blob", e);
        }
    }

    public static byte[] createBlob(String keyAlias, byte[] data, byte[] applicationId, long sid) {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(256, new SecureRandom());
            SecretKey secretKey = keyGenerator.generateKey();
            KeyStore keyStore = getKeyStore();
            KeyProtection.Builder builder = new KeyProtection.Builder(2).setBlockModes("GCM").setEncryptionPaddings("NoPadding").setCriticalToDeviceEncryption(true);
            if (sid != 0) {
                builder.setUserAuthenticationRequired(true).setBoundToSpecificSecureUserId(sid).setUserAuthenticationValidityDurationSeconds(15);
            }
            if (secretKey == null) {
                Slog.e(TAG, "createBlob secretKey is null");
            }
            Slog.d(TAG, "createBlob Callers=" + Debug.getCallers(5) + "set keyAlias:" + keyAlias);
            keyStore.setEntry(keyAlias, new KeyStore.SecretKeyEntry(secretKey), builder.build());
            byte[] intermediate = encrypt(applicationId, APPLICATION_ID_PERSONALIZATION, data);
            return encrypt(secretKey, intermediate);
        } catch (IOException | InvalidKeyException | KeyStoreException | NoSuchAlgorithmException | CertificateException | InvalidParameterSpecException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException e) {
            Slog.e(TAG, "Failed to create blob", e);
            throw new IllegalStateException("Failed to encrypt blob", e);
        }
    }

    public static void destroyBlobKey(String keyAlias) {
        try {
            KeyStore keyStore = getKeyStore();
            keyStore.deleteEntry(keyAlias);
            Slog.i(TAG, "SP key deleted: " + keyAlias);
            Slog.d(TAG, Log.getStackTraceString(new Throwable()));
            try {
                ActivityManager.getService().startTNE("0xffffff0e", 2L, 0, "SyntheticPassword");
            } catch (RemoteException e) {
                Slog.e(TAG, "destroyBlobKey error");
            }
        } catch (IOException | KeyStoreException | NoSuchAlgorithmException | CertificateException e2) {
            Slog.e(TAG, "Failed to destroy blob", e2);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public static byte[] personalisedHash(byte[] personalisation, byte[]... message) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            if (personalisation.length > 128) {
                throw new IllegalArgumentException("Personalisation too long");
            }
            digest.update(Arrays.copyOf(personalisation, 128));
            for (byte[] data : message) {
                digest.update(data);
            }
            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("NoSuchAlgorithmException for SHA-512", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean migrateLockSettingsKey(String alias) {
        KeyDescriptor legacyKey = new KeyDescriptor();
        legacyKey.domain = 0;
        legacyKey.nspace = -1L;
        legacyKey.alias = alias;
        KeyDescriptor newKey = new KeyDescriptor();
        newKey.domain = 2;
        newKey.nspace = keyNamespace();
        newKey.alias = alias;
        Slog.i(TAG, "Migrating key " + alias);
        int err = AndroidKeyStoreMaintenance.migrateKeyNamespace(legacyKey, newKey);
        if (err == 0) {
            return true;
        }
        if (err == 7) {
            Slog.i(TAG, "Key does not exist");
            return true;
        } else if (err != 20) {
            Slog.e(TAG, String.format("Failed to migrate key: %d", Integer.valueOf(err)));
            return false;
        } else {
            Slog.i(TAG, "Key already exists");
            return true;
        }
    }
}
