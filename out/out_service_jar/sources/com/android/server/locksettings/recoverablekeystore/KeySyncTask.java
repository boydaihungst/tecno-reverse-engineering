package com.android.server.locksettings.recoverablekeystore;

import android.content.Context;
import android.os.RemoteException;
import android.security.Scrypt;
import android.security.keystore.recovery.KeyChainProtectionParams;
import android.security.keystore.recovery.KeyChainSnapshot;
import android.security.keystore.recovery.KeyDerivationParams;
import android.security.keystore.recovery.WrappedApplicationKey;
import android.util.Log;
import android.util.Pair;
import com.android.internal.util.ArrayUtils;
import com.android.server.locksettings.recoverablekeystore.storage.RecoverableKeyStoreDb;
import com.android.server.locksettings.recoverablekeystore.storage.RecoverySnapshotStorage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertPath;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
/* loaded from: classes.dex */
public class KeySyncTask implements Runnable {
    private static final int LENGTH_PREFIX_BYTES = 4;
    private static final String LOCK_SCREEN_HASH_ALGORITHM = "SHA-256";
    private static final String RECOVERY_KEY_ALGORITHM = "AES";
    private static final int RECOVERY_KEY_SIZE_BITS = 256;
    private static final int SALT_LENGTH_BYTES = 16;
    static final int SCRYPT_PARAM_N = 4096;
    static final int SCRYPT_PARAM_OUTLEN_BYTES = 32;
    static final int SCRYPT_PARAM_P = 1;
    static final int SCRYPT_PARAM_R = 8;
    private static final String TAG = "KeySyncTask";
    private static final int TRUSTED_HARDWARE_MAX_ATTEMPTS = 10;
    private final byte[] mCredential;
    private final int mCredentialType;
    private final boolean mCredentialUpdated;
    private final PlatformKeyManager mPlatformKeyManager;
    private final RecoverableKeyStoreDb mRecoverableKeyStoreDb;
    private final RecoverySnapshotStorage mRecoverySnapshotStorage;
    private final Scrypt mScrypt;
    private final RecoverySnapshotListenersStorage mSnapshotListenersStorage;
    private final TestOnlyInsecureCertificateHelper mTestOnlyInsecureCertificateHelper;
    private final int mUserId;

    public static KeySyncTask newInstance(Context context, RecoverableKeyStoreDb recoverableKeyStoreDb, RecoverySnapshotStorage snapshotStorage, RecoverySnapshotListenersStorage recoverySnapshotListenersStorage, int userId, int credentialType, byte[] credential, boolean credentialUpdated) throws NoSuchAlgorithmException, KeyStoreException, InsecureUserException {
        return new KeySyncTask(recoverableKeyStoreDb, snapshotStorage, recoverySnapshotListenersStorage, userId, credentialType, credential, credentialUpdated, PlatformKeyManager.getInstance(context, recoverableKeyStoreDb), new TestOnlyInsecureCertificateHelper(), new Scrypt());
    }

    KeySyncTask(RecoverableKeyStoreDb recoverableKeyStoreDb, RecoverySnapshotStorage snapshotStorage, RecoverySnapshotListenersStorage recoverySnapshotListenersStorage, int userId, int credentialType, byte[] credential, boolean credentialUpdated, PlatformKeyManager platformKeyManager, TestOnlyInsecureCertificateHelper testOnlyInsecureCertificateHelper, Scrypt scrypt) {
        this.mSnapshotListenersStorage = recoverySnapshotListenersStorage;
        this.mRecoverableKeyStoreDb = recoverableKeyStoreDb;
        this.mUserId = userId;
        this.mCredentialType = credentialType;
        this.mCredential = credential;
        this.mCredentialUpdated = credentialUpdated;
        this.mPlatformKeyManager = platformKeyManager;
        this.mRecoverySnapshotStorage = snapshotStorage;
        this.mTestOnlyInsecureCertificateHelper = testOnlyInsecureCertificateHelper;
        this.mScrypt = scrypt;
    }

    @Override // java.lang.Runnable
    public void run() {
        try {
            synchronized (KeySyncTask.class) {
                syncKeys();
            }
        } catch (Exception e) {
            Log.e(TAG, "Unexpected exception thrown during KeySyncTask", e);
        }
    }

    private void syncKeys() throws RemoteException {
        int generation = this.mPlatformKeyManager.getGenerationId(this.mUserId);
        if (this.mCredentialType == -1) {
            Log.w(TAG, "Credentials are not set for user " + this.mUserId);
            if (generation < 1001000) {
                this.mPlatformKeyManager.invalidatePlatformKey(this.mUserId, generation);
            }
        } else if (isCustomLockScreen()) {
            Log.w(TAG, "Unsupported credential type " + this.mCredentialType + " for user " + this.mUserId);
            if (generation < 1001000) {
                this.mRecoverableKeyStoreDb.invalidateKeysForUserIdOnCustomScreenLock(this.mUserId);
            }
        } else if (this.mPlatformKeyManager.isDeviceLocked(this.mUserId) && this.mUserId == 0) {
            Log.w(TAG, "Can't sync keys for locked user " + this.mUserId);
        } else {
            List<Integer> recoveryAgents = this.mRecoverableKeyStoreDb.getRecoveryAgents(this.mUserId);
            for (Integer num : recoveryAgents) {
                int uid = num.intValue();
                try {
                    syncKeysForAgent(uid);
                } catch (IOException e) {
                    Log.e(TAG, "IOException during sync for agent " + uid, e);
                }
            }
            if (recoveryAgents.isEmpty()) {
                Log.w(TAG, "No recovery agent initialized for user " + this.mUserId);
            }
        }
    }

    private boolean isCustomLockScreen() {
        int i = this.mCredentialType;
        return (i == -1 || i == 1 || i == 3 || i == 4) ? false : true;
    }

    private void syncKeysForAgent(int recoveryAgentUid) throws IOException, RemoteException {
        boolean shouldRecreateCurrentVersion;
        PublicKey publicKey;
        byte[] localLskfHash;
        Map<String, Pair<SecretKey, byte[]>> rawKeysWithMetadata;
        Long counterId;
        KeyDerivationParams keyDerivationParams;
        if (shouldCreateSnapshot(recoveryAgentUid)) {
            shouldRecreateCurrentVersion = false;
        } else {
            boolean shouldRecreateCurrentVersion2 = this.mRecoverableKeyStoreDb.getSnapshotVersion(this.mUserId, recoveryAgentUid) != null && this.mRecoverySnapshotStorage.get(recoveryAgentUid) == null;
            if (shouldRecreateCurrentVersion2) {
                Log.d(TAG, "Recreating most recent snapshot");
                shouldRecreateCurrentVersion = shouldRecreateCurrentVersion2;
            } else {
                Log.d(TAG, "Key sync not needed.");
                return;
            }
        }
        String rootCertAlias = this.mTestOnlyInsecureCertificateHelper.getDefaultCertificateAliasIfEmpty(this.mRecoverableKeyStoreDb.getActiveRootOfTrust(this.mUserId, recoveryAgentUid));
        CertPath certPath = this.mRecoverableKeyStoreDb.getRecoveryServiceCertPath(this.mUserId, recoveryAgentUid, rootCertAlias);
        if (certPath != null) {
            Log.d(TAG, "Using the public key in stored CertPath for syncing");
            publicKey = certPath.getCertificates().get(0).getPublicKey();
        } else {
            Log.d(TAG, "Using the stored raw public key for syncing");
            publicKey = this.mRecoverableKeyStoreDb.getRecoveryServicePublicKey(this.mUserId, recoveryAgentUid);
        }
        if (publicKey != null) {
            byte[] vaultHandle = this.mRecoverableKeyStoreDb.getServerParams(this.mUserId, recoveryAgentUid);
            if (vaultHandle == null) {
                Log.w(TAG, "No device ID set for user " + this.mUserId);
                return;
            }
            if (this.mTestOnlyInsecureCertificateHelper.isTestOnlyCertificateAlias(rootCertAlias)) {
                Log.w(TAG, "Insecure root certificate is used by recovery agent " + recoveryAgentUid);
                if (this.mTestOnlyInsecureCertificateHelper.doesCredentialSupportInsecureMode(this.mCredentialType, this.mCredential)) {
                    Log.w(TAG, "Whitelisted credential is used to generate snapshot by recovery agent " + recoveryAgentUid);
                } else {
                    Log.w(TAG, "Non whitelisted credential is used to generate recovery snapshot by " + recoveryAgentUid + " - ignore attempt.");
                    return;
                }
            }
            boolean useScryptToHashCredential = shouldUseScryptToHashCredential();
            byte[] salt = generateSalt();
            if (useScryptToHashCredential) {
                localLskfHash = hashCredentialsByScrypt(salt, this.mCredential);
            } else {
                byte[] localLskfHash2 = this.mCredential;
                localLskfHash = hashCredentialsBySaltedSha256(salt, localLskfHash2);
            }
            try {
                Map<String, Pair<SecretKey, byte[]>> rawKeysWithMetadata2 = getKeysToSync(recoveryAgentUid);
                if (!this.mTestOnlyInsecureCertificateHelper.isTestOnlyCertificateAlias(rootCertAlias)) {
                    rawKeysWithMetadata = rawKeysWithMetadata2;
                } else {
                    rawKeysWithMetadata = this.mTestOnlyInsecureCertificateHelper.keepOnlyWhitelistedInsecureKeys(rawKeysWithMetadata2);
                }
                try {
                    SecretKey recoveryKey = generateRecoveryKey();
                    try {
                        Map<String, byte[]> encryptedApplicationKeys = KeySyncUtils.encryptKeysWithRecoveryKey(recoveryKey, rawKeysWithMetadata);
                        if (!this.mCredentialUpdated) {
                            Long counterId2 = this.mRecoverableKeyStoreDb.getCounterId(this.mUserId, recoveryAgentUid);
                            if (counterId2 != null) {
                                counterId = counterId2;
                            } else {
                                counterId = Long.valueOf(generateAndStoreCounterId(recoveryAgentUid));
                            }
                        } else {
                            counterId = Long.valueOf(generateAndStoreCounterId(recoveryAgentUid));
                        }
                        byte[] vaultParams = KeySyncUtils.packVaultParams(publicKey, counterId.longValue(), 10, vaultHandle);
                        try {
                            byte[] encryptedRecoveryKey = KeySyncUtils.thmEncryptRecoveryKey(publicKey, localLskfHash, vaultParams, recoveryKey);
                            if (useScryptToHashCredential) {
                                keyDerivationParams = KeyDerivationParams.createScryptParams(salt, 4096);
                            } else {
                                KeyDerivationParams keyDerivationParams2 = KeyDerivationParams.createSha256Params(salt);
                                keyDerivationParams = keyDerivationParams2;
                            }
                            KeyChainProtectionParams keyChainProtectionParams = new KeyChainProtectionParams.Builder().setUserSecretType(100).setLockScreenUiFormat(getUiFormat(this.mCredentialType)).setKeyDerivationParams(keyDerivationParams).setSecret(new byte[0]).build();
                            ArrayList<KeyChainProtectionParams> metadataList = new ArrayList<>();
                            metadataList.add(keyChainProtectionParams);
                            KeyChainSnapshot.Builder keyChainSnapshotBuilder = new KeyChainSnapshot.Builder().setSnapshotVersion(getSnapshotVersion(recoveryAgentUid, shouldRecreateCurrentVersion)).setMaxAttempts(10).setCounterId(counterId.longValue()).setServerParams(vaultHandle).setKeyChainProtectionParams(metadataList).setWrappedApplicationKeys(createApplicationKeyEntries(encryptedApplicationKeys, rawKeysWithMetadata)).setEncryptedRecoveryKeyBlob(encryptedRecoveryKey);
                            try {
                                keyChainSnapshotBuilder.setTrustedHardwareCertPath(certPath);
                                this.mRecoverySnapshotStorage.put(recoveryAgentUid, keyChainSnapshotBuilder.build());
                                this.mSnapshotListenersStorage.recoverySnapshotAvailable(recoveryAgentUid);
                                this.mRecoverableKeyStoreDb.setShouldCreateSnapshot(this.mUserId, recoveryAgentUid, false);
                                return;
                            } catch (CertificateException e) {
                                Log.wtf(TAG, "Cannot serialize CertPath when calling setTrustedHardwareCertPath", e);
                                return;
                            }
                        } catch (InvalidKeyException e2) {
                            Log.e(TAG, "Could not encrypt with recovery key", e2);
                            return;
                        } catch (NoSuchAlgorithmException e3) {
                            Log.wtf(TAG, "SecureBox encrypt algorithms unavailable", e3);
                            return;
                        }
                    } catch (InvalidKeyException | NoSuchAlgorithmException e4) {
                        Log.wtf(TAG, "Should be impossible: could not encrypt application keys with random key", e4);
                        return;
                    }
                } catch (NoSuchAlgorithmException e5) {
                    Log.wtf("AES should never be unavailable", e5);
                    return;
                }
            } catch (BadPlatformKeyException e6) {
                Log.e(TAG, "Loaded keys for same generation ID as platform key, so BadPlatformKeyException should be impossible.", e6);
                return;
            } catch (InsecureUserException e7) {
                Log.e(TAG, "A screen unlock triggered the key sync flow, so user must have lock screen. This should be impossible.", e7);
                return;
            } catch (IOException e8) {
                Log.e(TAG, "Local database error.", e8);
                return;
            } catch (GeneralSecurityException e9) {
                Log.e(TAG, "Failed to load recoverable keys for sync", e9);
                return;
            }
        }
        Log.w(TAG, "Not initialized for KeySync: no public key set. Cancelling task.");
    }

    int getSnapshotVersion(int recoveryAgentUid, boolean shouldRecreateCurrentVersion) throws IOException {
        Long snapshotVersion;
        Long snapshotVersion2 = this.mRecoverableKeyStoreDb.getSnapshotVersion(this.mUserId, recoveryAgentUid);
        if (!shouldRecreateCurrentVersion) {
            snapshotVersion = Long.valueOf(snapshotVersion2 != null ? 1 + snapshotVersion2.longValue() : 1L);
        } else {
            snapshotVersion = Long.valueOf(snapshotVersion2 != null ? snapshotVersion2.longValue() : 1L);
        }
        long updatedRows = this.mRecoverableKeyStoreDb.setSnapshotVersion(this.mUserId, recoveryAgentUid, snapshotVersion.longValue());
        if (updatedRows < 0) {
            Log.e(TAG, "Failed to set the snapshot version in the local DB.");
            throw new IOException("Failed to set the snapshot version in the local DB.");
        }
        return snapshotVersion.intValue();
    }

    private long generateAndStoreCounterId(int recoveryAgentUid) throws IOException {
        long counter = new SecureRandom().nextLong();
        long updatedRows = this.mRecoverableKeyStoreDb.setCounterId(this.mUserId, recoveryAgentUid, counter);
        if (updatedRows < 0) {
            Log.e(TAG, "Failed to set the snapshot version in the local DB.");
            throw new IOException("Failed to set counterId in the local DB.");
        }
        return counter;
    }

    private Map<String, Pair<SecretKey, byte[]>> getKeysToSync(int recoveryAgentUid) throws InsecureUserException, KeyStoreException, UnrecoverableKeyException, NoSuchAlgorithmException, NoSuchPaddingException, BadPlatformKeyException, InvalidKeyException, InvalidAlgorithmParameterException, IOException, RemoteException {
        PlatformDecryptionKey decryptKey = this.mPlatformKeyManager.getDecryptKey(this.mUserId);
        Map<String, WrappedKey> wrappedKeys = this.mRecoverableKeyStoreDb.getAllKeys(this.mUserId, recoveryAgentUid, decryptKey.getGenerationId());
        return WrappedKey.unwrapKeys(decryptKey, wrappedKeys);
    }

    private boolean shouldCreateSnapshot(int recoveryAgentUid) {
        int[] types = this.mRecoverableKeyStoreDb.getRecoverySecretTypes(this.mUserId, recoveryAgentUid);
        if (!ArrayUtils.contains(types, 100)) {
            return false;
        }
        if (this.mCredentialUpdated && this.mRecoverableKeyStoreDb.getSnapshotVersion(this.mUserId, recoveryAgentUid) != null) {
            this.mRecoverableKeyStoreDb.setShouldCreateSnapshot(this.mUserId, recoveryAgentUid, true);
            return true;
        }
        return this.mRecoverableKeyStoreDb.getShouldCreateSnapshot(this.mUserId, recoveryAgentUid);
    }

    static int getUiFormat(int credentialType) {
        if (credentialType == 1) {
            return 3;
        }
        return credentialType == 3 ? 1 : 2;
    }

    private static byte[] generateSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return salt;
    }

    static byte[] hashCredentialsBySaltedSha256(byte[] salt, byte[] credentialsBytes) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(salt.length + credentialsBytes.length + 8);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        byteBuffer.putInt(salt.length);
        byteBuffer.put(salt);
        byteBuffer.putInt(credentialsBytes.length);
        byteBuffer.put(credentialsBytes);
        byte[] bytes = byteBuffer.array();
        try {
            byte[] hash = MessageDigest.getInstance(LOCK_SCREEN_HASH_ALGORITHM).digest(bytes);
            Arrays.fill(bytes, (byte) 0);
            return hash;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] hashCredentialsByScrypt(byte[] salt, byte[] credentials) {
        return this.mScrypt.scrypt(credentials, salt, 4096, 8, 1, 32);
    }

    private static SecretKey generateRecoveryKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(RECOVERY_KEY_ALGORITHM);
        keyGenerator.init(256);
        return keyGenerator.generateKey();
    }

    private static List<WrappedApplicationKey> createApplicationKeyEntries(Map<String, byte[]> encryptedApplicationKeys, Map<String, Pair<SecretKey, byte[]>> originalKeysWithMetadata) {
        ArrayList<WrappedApplicationKey> keyEntries = new ArrayList<>();
        for (String alias : encryptedApplicationKeys.keySet()) {
            keyEntries.add(new WrappedApplicationKey.Builder().setAlias(alias).setEncryptedKeyMaterial(encryptedApplicationKeys.get(alias)).setMetadata((byte[]) originalKeysWithMetadata.get(alias).second).build());
        }
        return keyEntries;
    }

    private boolean shouldUseScryptToHashCredential() {
        int i = this.mCredentialType;
        return i == 4 || i == 3;
    }
}
