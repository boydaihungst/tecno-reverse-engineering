package com.android.server.locksettings.recoverablekeystore;

import android.app.KeyguardManager;
import android.content.Context;
import android.os.RemoteException;
import android.security.GateKeeper;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProtection;
import android.service.gatekeeper.IGateKeeperService;
import android.util.Log;
import com.android.server.locksettings.recoverablekeystore.storage.RecoverableKeyStoreDb;
import com.android.server.slice.SliceClientPermissions;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Locale;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
/* loaded from: classes.dex */
public class PlatformKeyManager {
    private static final String DECRYPT_KEY_ALIAS_SUFFIX = "decrypt";
    private static final String ENCRYPT_KEY_ALIAS_SUFFIX = "encrypt";
    private static final byte[] GCM_INSECURE_NONCE_BYTES = new byte[12];
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final String KEY_ALGORITHM = "AES";
    private static final String KEY_ALIAS_PREFIX = "com.android.server.locksettings.recoverablekeystore/platform/";
    private static final int KEY_SIZE_BITS = 256;
    private static final String KEY_WRAP_CIPHER_ALGORITHM = "AES/GCM/NoPadding";
    static final int MIN_GENERATION_ID_FOR_UNLOCKED_DEVICE_REQUIRED = 1001000;
    private static final String TAG = "PlatformKeyManager";
    private static final int USER_AUTHENTICATION_VALIDITY_DURATION_SECONDS = 15;
    private final Context mContext;
    private final RecoverableKeyStoreDb mDatabase;
    private final KeyStoreProxy mKeyStore;

    public static PlatformKeyManager getInstance(Context context, RecoverableKeyStoreDb database) throws KeyStoreException, NoSuchAlgorithmException {
        return new PlatformKeyManager(context.getApplicationContext(), new KeyStoreProxyImpl(getAndLoadAndroidKeyStore()), database);
    }

    PlatformKeyManager(Context context, KeyStoreProxy keyStore, RecoverableKeyStoreDb database) {
        this.mKeyStore = keyStore;
        this.mContext = context;
        this.mDatabase = database;
    }

    public int getGenerationId(int userId) {
        return this.mDatabase.getPlatformKeyGenerationId(userId);
    }

    public boolean isDeviceLocked(int userId) {
        return ((KeyguardManager) this.mContext.getSystemService(KeyguardManager.class)).isDeviceLocked(userId);
    }

    public void invalidatePlatformKey(int userId, int generationId) {
        if (generationId != -1) {
            try {
                this.mKeyStore.deleteEntry(getEncryptAlias(userId, generationId));
                this.mKeyStore.deleteEntry(getDecryptAlias(userId, generationId));
            } catch (KeyStoreException e) {
            }
        }
    }

    void regenerate(int userId) throws NoSuchAlgorithmException, KeyStoreException, IOException, RemoteException {
        int nextId;
        int generationId = getGenerationId(userId);
        if (generationId == -1) {
            nextId = 1;
        } else {
            invalidatePlatformKey(userId, generationId);
            nextId = generationId + 1;
        }
        Math.max(generationId, (int) MIN_GENERATION_ID_FOR_UNLOCKED_DEVICE_REQUIRED);
        generateAndLoadKey(userId, nextId);
    }

    public PlatformEncryptionKey getEncryptKey(int userId) throws KeyStoreException, UnrecoverableKeyException, NoSuchAlgorithmException, IOException, RemoteException {
        init(userId);
        try {
            getDecryptKeyInternal(userId);
            return getEncryptKeyInternal(userId);
        } catch (UnrecoverableKeyException e) {
            Log.i(TAG, String.format(Locale.US, "Regenerating permanently invalid Platform key for user %d.", Integer.valueOf(userId)));
            regenerate(userId);
            return getEncryptKeyInternal(userId);
        }
    }

    private PlatformEncryptionKey getEncryptKeyInternal(int userId) throws KeyStoreException, UnrecoverableKeyException, NoSuchAlgorithmException {
        int generationId = getGenerationId(userId);
        String alias = getEncryptAlias(userId, generationId);
        if (!isKeyLoaded(userId, generationId)) {
            throw new UnrecoverableKeyException("KeyStore doesn't contain key " + alias);
        }
        SecretKey key = (SecretKey) this.mKeyStore.getKey(alias, null);
        return new PlatformEncryptionKey(generationId, key);
    }

    public PlatformDecryptionKey getDecryptKey(int userId) throws KeyStoreException, UnrecoverableKeyException, NoSuchAlgorithmException, IOException, RemoteException {
        init(userId);
        try {
            PlatformDecryptionKey decryptionKey = getDecryptKeyInternal(userId);
            ensureDecryptionKeyIsValid(userId, decryptionKey);
            return decryptionKey;
        } catch (UnrecoverableKeyException e) {
            Log.i(TAG, String.format(Locale.US, "Regenerating permanently invalid Platform key for user %d.", Integer.valueOf(userId)));
            regenerate(userId);
            return getDecryptKeyInternal(userId);
        }
    }

    private PlatformDecryptionKey getDecryptKeyInternal(int userId) throws KeyStoreException, UnrecoverableKeyException, NoSuchAlgorithmException {
        int generationId = getGenerationId(userId);
        String alias = getDecryptAlias(userId, generationId);
        if (!isKeyLoaded(userId, generationId)) {
            throw new UnrecoverableKeyException("KeyStore doesn't contain key " + alias);
        }
        SecretKey key = (SecretKey) this.mKeyStore.getKey(alias, null);
        return new PlatformDecryptionKey(generationId, key);
    }

    private void ensureDecryptionKeyIsValid(int userId, PlatformDecryptionKey decryptionKey) throws UnrecoverableKeyException {
        try {
            Cipher.getInstance(KEY_WRAP_CIPHER_ALGORITHM).init(4, decryptionKey.getKey(), new GCMParameterSpec(128, GCM_INSECURE_NONCE_BYTES));
        } catch (KeyPermanentlyInvalidatedException e) {
            Log.e(TAG, String.format(Locale.US, "The platform key for user %d became invalid.", Integer.valueOf(userId)));
            throw new UnrecoverableKeyException(e.getMessage());
        } catch (InvalidAlgorithmParameterException | InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException e2) {
        }
    }

    void init(int userId) throws KeyStoreException, NoSuchAlgorithmException, IOException, RemoteException {
        int generationId;
        int generationId2 = getGenerationId(userId);
        if (isKeyLoaded(userId, generationId2)) {
            Log.i(TAG, String.format(Locale.US, "Platform key generation %d exists already.", Integer.valueOf(generationId2)));
            return;
        }
        if (generationId2 == -1) {
            Log.i(TAG, "Generating initial platform key generation ID.");
            generationId = 1;
        } else {
            Log.w(TAG, String.format(Locale.US, "Platform generation ID was %d but no entry was present in AndroidKeyStore. Generating fresh key.", Integer.valueOf(generationId2)));
            generationId = generationId2 + 1;
        }
        generateAndLoadKey(userId, Math.max(generationId, (int) MIN_GENERATION_ID_FOR_UNLOCKED_DEVICE_REQUIRED));
    }

    private String getEncryptAlias(int userId, int generationId) {
        return KEY_ALIAS_PREFIX + userId + SliceClientPermissions.SliceAuthority.DELIMITER + generationId + SliceClientPermissions.SliceAuthority.DELIMITER + ENCRYPT_KEY_ALIAS_SUFFIX;
    }

    private String getDecryptAlias(int userId, int generationId) {
        return KEY_ALIAS_PREFIX + userId + SliceClientPermissions.SliceAuthority.DELIMITER + generationId + SliceClientPermissions.SliceAuthority.DELIMITER + DECRYPT_KEY_ALIAS_SUFFIX;
    }

    private void setGenerationId(int userId, int generationId) throws IOException {
        this.mDatabase.setPlatformKeyGenerationId(userId, generationId);
    }

    private boolean isKeyLoaded(int userId, int generationId) throws KeyStoreException {
        return this.mKeyStore.containsAlias(getEncryptAlias(userId, generationId)) && this.mKeyStore.containsAlias(getDecryptAlias(userId, generationId));
    }

    IGateKeeperService getGateKeeperService() {
        return GateKeeper.getService();
    }

    private void generateAndLoadKey(int userId, int generationId) throws NoSuchAlgorithmException, KeyStoreException, IOException, RemoteException {
        String encryptAlias = getEncryptAlias(userId, generationId);
        String decryptAlias = getDecryptAlias(userId, generationId);
        SecretKey secretKey = generateAesKey();
        KeyProtection.Builder decryptionKeyProtection = new KeyProtection.Builder(2).setBlockModes("GCM").setEncryptionPaddings("NoPadding");
        if (userId == 0) {
            decryptionKeyProtection.setUnlockedDeviceRequired(true);
        }
        this.mKeyStore.setEntry(decryptAlias, new KeyStore.SecretKeyEntry(secretKey), decryptionKeyProtection.build());
        this.mKeyStore.setEntry(encryptAlias, new KeyStore.SecretKeyEntry(secretKey), new KeyProtection.Builder(1).setBlockModes("GCM").setEncryptionPaddings("NoPadding").build());
        setGenerationId(userId, generationId);
    }

    private static SecretKey generateAesKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(KEY_ALGORITHM);
        keyGenerator.init(256);
        return keyGenerator.generateKey();
    }

    private static KeyStore getAndLoadAndroidKeyStore() throws KeyStoreException {
        KeyStore keyStore = KeyStore.getInstance(KeyStoreProxyImpl.ANDROID_KEY_STORE_PROVIDER);
        try {
            keyStore.load(null);
            return keyStore;
        } catch (IOException | NoSuchAlgorithmException | CertificateException e) {
            throw new KeyStoreException("Unable to load keystore.", e);
        }
    }
}
