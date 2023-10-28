package com.android.server.locksettings.recoverablekeystore;

import android.app.PendingIntent;
import android.content.Context;
import android.os.Binder;
import android.os.RemoteException;
import android.os.ServiceSpecificException;
import android.os.UserHandle;
import android.security.keystore.recovery.KeyChainProtectionParams;
import android.security.keystore.recovery.KeyChainSnapshot;
import android.security.keystore.recovery.RecoveryCertPath;
import android.security.keystore.recovery.WrappedApplicationKey;
import android.util.ArrayMap;
import android.util.Log;
import com.android.internal.util.HexDump;
import com.android.server.locksettings.recoverablekeystore.certificate.CertParsingException;
import com.android.server.locksettings.recoverablekeystore.certificate.CertUtils;
import com.android.server.locksettings.recoverablekeystore.certificate.CertValidationException;
import com.android.server.locksettings.recoverablekeystore.certificate.CertXml;
import com.android.server.locksettings.recoverablekeystore.certificate.SigXml;
import com.android.server.locksettings.recoverablekeystore.storage.ApplicationKeyStorage;
import com.android.server.locksettings.recoverablekeystore.storage.CleanupManager;
import com.android.server.locksettings.recoverablekeystore.storage.RecoverableKeyStoreDb;
import com.android.server.locksettings.recoverablekeystore.storage.RecoverySessionStorage;
import com.android.server.locksettings.recoverablekeystore.storage.RecoverySnapshotStorage;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertPath;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.crypto.AEADBadTagException;
/* loaded from: classes.dex */
public class RecoverableKeyStoreManager {
    private static final long SYNC_DELAY_MILLIS = 2000;
    private static final String TAG = "RecoverableKeyStoreMgr";
    private static RecoverableKeyStoreManager mInstance;
    private final ApplicationKeyStorage mApplicationKeyStorage;
    private final CleanupManager mCleanupManager;
    private final Context mContext;
    private final RecoverableKeyStoreDb mDatabase;
    private final ScheduledExecutorService mExecutorService;
    private final RecoverySnapshotListenersStorage mListenersStorage;
    private final PlatformKeyManager mPlatformKeyManager;
    private final RecoverableKeyGenerator mRecoverableKeyGenerator;
    private final RecoverySessionStorage mRecoverySessionStorage;
    private final RecoverySnapshotStorage mSnapshotStorage;
    private final TestOnlyInsecureCertificateHelper mTestCertHelper;

    public static synchronized RecoverableKeyStoreManager getInstance(Context context) {
        RecoverableKeyStoreManager recoverableKeyStoreManager;
        synchronized (RecoverableKeyStoreManager.class) {
            if (mInstance == null) {
                RecoverableKeyStoreDb db = RecoverableKeyStoreDb.newInstance(context);
                try {
                    try {
                        PlatformKeyManager platformKeyManager = PlatformKeyManager.getInstance(context, db);
                        ApplicationKeyStorage applicationKeyStorage = ApplicationKeyStorage.getInstance();
                        RecoverySnapshotStorage snapshotStorage = RecoverySnapshotStorage.newInstance();
                        CleanupManager cleanupManager = CleanupManager.getInstance(context.getApplicationContext(), snapshotStorage, db, applicationKeyStorage);
                        mInstance = new RecoverableKeyStoreManager(context.getApplicationContext(), db, new RecoverySessionStorage(), Executors.newScheduledThreadPool(1), snapshotStorage, new RecoverySnapshotListenersStorage(), platformKeyManager, applicationKeyStorage, new TestOnlyInsecureCertificateHelper(), cleanupManager);
                    } catch (NoSuchAlgorithmException e) {
                        throw new RuntimeException(e);
                    }
                } catch (KeyStoreException e2) {
                    throw new ServiceSpecificException(22, e2.getMessage());
                }
            }
            recoverableKeyStoreManager = mInstance;
        }
        return recoverableKeyStoreManager;
    }

    RecoverableKeyStoreManager(Context context, RecoverableKeyStoreDb recoverableKeyStoreDb, RecoverySessionStorage recoverySessionStorage, ScheduledExecutorService executorService, RecoverySnapshotStorage snapshotStorage, RecoverySnapshotListenersStorage listenersStorage, PlatformKeyManager platformKeyManager, ApplicationKeyStorage applicationKeyStorage, TestOnlyInsecureCertificateHelper testOnlyInsecureCertificateHelper, CleanupManager cleanupManager) {
        this.mContext = context;
        this.mDatabase = recoverableKeyStoreDb;
        this.mRecoverySessionStorage = recoverySessionStorage;
        this.mExecutorService = executorService;
        this.mListenersStorage = listenersStorage;
        this.mSnapshotStorage = snapshotStorage;
        this.mPlatformKeyManager = platformKeyManager;
        this.mApplicationKeyStorage = applicationKeyStorage;
        this.mTestCertHelper = testOnlyInsecureCertificateHelper;
        this.mCleanupManager = cleanupManager;
        cleanupManager.verifyKnownUsers();
        try {
            this.mRecoverableKeyGenerator = RecoverableKeyGenerator.newInstance(recoverableKeyStoreDb);
        } catch (NoSuchAlgorithmException e) {
            Log.wtf(TAG, "AES keygen algorithm not available. AOSP must support this.", e);
            throw new ServiceSpecificException(22, e.getMessage());
        }
    }

    void initRecoveryService(String rootCertificateAlias, byte[] recoveryServiceCertFile) throws RemoteException {
        long updatedCertPathRows;
        checkRecoverKeyStorePermission();
        int userId = UserHandle.getCallingUserId();
        int uid = Binder.getCallingUid();
        String rootCertificateAlias2 = this.mTestCertHelper.getDefaultCertificateAliasIfEmpty(rootCertificateAlias);
        if (!this.mTestCertHelper.isValidRootCertificateAlias(rootCertificateAlias2)) {
            throw new ServiceSpecificException(28, "Invalid root certificate alias");
        }
        String activeRootAlias = this.mDatabase.getActiveRootOfTrust(userId, uid);
        if (activeRootAlias == null) {
            Log.d(TAG, "Root of trust for recovery agent + " + uid + " is assigned for the first time to " + rootCertificateAlias2);
        } else if (!activeRootAlias.equals(rootCertificateAlias2)) {
            Log.i(TAG, "Root of trust for recovery agent " + uid + " is changed to " + rootCertificateAlias2 + " from  " + activeRootAlias);
        }
        long updatedRows = this.mDatabase.setActiveRootOfTrust(userId, uid, rootCertificateAlias2);
        if (updatedRows < 0) {
            throw new ServiceSpecificException(22, "Failed to set the root of trust in the local DB.");
        }
        try {
            CertXml certXml = CertXml.parse(recoveryServiceCertFile);
            long newSerial = certXml.getSerial();
            Long oldSerial = this.mDatabase.getRecoveryServiceCertSerial(userId, uid, rootCertificateAlias2);
            if (oldSerial == null || oldSerial.longValue() < newSerial || this.mTestCertHelper.isTestOnlyCertificateAlias(rootCertificateAlias2)) {
                Log.i(TAG, "Updating the certificate with the new serial number " + newSerial);
                X509Certificate rootCert = this.mTestCertHelper.getRootCertificate(rootCertificateAlias2);
                try {
                    Log.d(TAG, "Getting and validating a random endpoint certificate");
                    CertPath certPath = certXml.getRandomEndpointCert(rootCert);
                    try {
                        Log.d(TAG, "Saving the randomly chosen endpoint certificate to database");
                        updatedCertPathRows = this.mDatabase.setRecoveryServiceCertPath(userId, uid, rootCertificateAlias2, certPath);
                    } catch (CertificateEncodingException e) {
                        e = e;
                    }
                    try {
                        if (updatedCertPathRows <= 0) {
                            if (updatedCertPathRows < 0) {
                                throw new ServiceSpecificException(22, "Failed to set the certificate path in the local DB.");
                            }
                            return;
                        }
                        long updatedCertSerialRows = this.mDatabase.setRecoveryServiceCertSerial(userId, uid, rootCertificateAlias2, newSerial);
                        if (updatedCertSerialRows < 0) {
                            throw new ServiceSpecificException(22, "Failed to set the certificate serial number in the local DB.");
                        }
                        if (this.mDatabase.getSnapshotVersion(userId, uid) == null) {
                            Log.i(TAG, "This is a certificate change. Snapshot didn't exist");
                        } else {
                            this.mDatabase.setShouldCreateSnapshot(userId, uid, true);
                            Log.i(TAG, "This is a certificate change. Snapshot must be updated");
                        }
                        long updatedCounterIdRows = this.mDatabase.setCounterId(userId, uid, new SecureRandom().nextLong());
                        if (updatedCounterIdRows < 0) {
                            Log.e(TAG, "Failed to set the counter id in the local DB.");
                        }
                    } catch (CertificateEncodingException e2) {
                        e = e2;
                        Log.e(TAG, "Failed to encode CertPath", e);
                        throw new ServiceSpecificException(25, e.getMessage());
                    }
                } catch (CertValidationException e3) {
                    Log.e(TAG, "Invalid endpoint cert", e3);
                    throw new ServiceSpecificException(28, e3.getMessage());
                }
            } else if (oldSerial.longValue() == newSerial) {
                Log.i(TAG, "The cert file serial number is the same, so skip updating.");
            } else {
                Log.e(TAG, "The cert file serial number is older than the one in database.");
                throw new ServiceSpecificException(29, "The cert file serial number is older than the one in database.");
            }
        } catch (CertParsingException e4) {
            Log.d(TAG, "Failed to parse the input as a cert file: " + HexDump.toHexString(recoveryServiceCertFile));
            throw new ServiceSpecificException(25, e4.getMessage());
        }
    }

    public void initRecoveryServiceWithSigFile(String rootCertificateAlias, byte[] recoveryServiceCertFile, byte[] recoveryServiceSigFile) throws RemoteException {
        checkRecoverKeyStorePermission();
        String rootCertificateAlias2 = this.mTestCertHelper.getDefaultCertificateAliasIfEmpty(rootCertificateAlias);
        Objects.requireNonNull(recoveryServiceCertFile, "recoveryServiceCertFile is null");
        Objects.requireNonNull(recoveryServiceSigFile, "recoveryServiceSigFile is null");
        try {
            SigXml sigXml = SigXml.parse(recoveryServiceSigFile);
            X509Certificate rootCert = this.mTestCertHelper.getRootCertificate(rootCertificateAlias2);
            try {
                sigXml.verifyFileSignature(rootCert, recoveryServiceCertFile);
                initRecoveryService(rootCertificateAlias2, recoveryServiceCertFile);
            } catch (CertValidationException e) {
                Log.d(TAG, "The signature over the cert file is invalid. Cert: " + HexDump.toHexString(recoveryServiceCertFile) + " Sig: " + HexDump.toHexString(recoveryServiceSigFile));
                throw new ServiceSpecificException(28, e.getMessage());
            }
        } catch (CertParsingException e2) {
            Log.d(TAG, "Failed to parse the sig file: " + HexDump.toHexString(recoveryServiceSigFile));
            throw new ServiceSpecificException(25, e2.getMessage());
        }
    }

    public KeyChainSnapshot getKeyChainSnapshot() throws RemoteException {
        checkRecoverKeyStorePermission();
        int uid = Binder.getCallingUid();
        KeyChainSnapshot snapshot = this.mSnapshotStorage.get(uid);
        if (snapshot == null) {
            throw new ServiceSpecificException(21);
        }
        return snapshot;
    }

    public void setSnapshotCreatedPendingIntent(PendingIntent intent) throws RemoteException {
        checkRecoverKeyStorePermission();
        int uid = Binder.getCallingUid();
        this.mListenersStorage.setSnapshotListener(uid, intent);
    }

    public void setServerParams(byte[] serverParams) throws RemoteException {
        checkRecoverKeyStorePermission();
        int userId = UserHandle.getCallingUserId();
        int uid = Binder.getCallingUid();
        byte[] currentServerParams = this.mDatabase.getServerParams(userId, uid);
        if (Arrays.equals(serverParams, currentServerParams)) {
            Log.v(TAG, "Not updating server params - same as old value.");
            return;
        }
        long updatedRows = this.mDatabase.setServerParams(userId, uid, serverParams);
        if (updatedRows < 0) {
            throw new ServiceSpecificException(22, "Database failure trying to set server params.");
        }
        if (currentServerParams == null) {
            Log.i(TAG, "Initialized server params.");
        } else if (this.mDatabase.getSnapshotVersion(userId, uid) != null) {
            this.mDatabase.setShouldCreateSnapshot(userId, uid, true);
            Log.i(TAG, "Updated server params. Snapshot must be updated");
        } else {
            Log.i(TAG, "Updated server params. Snapshot didn't exist");
        }
    }

    public void setRecoveryStatus(String alias, int status) throws RemoteException {
        checkRecoverKeyStorePermission();
        Objects.requireNonNull(alias, "alias is null");
        long updatedRows = this.mDatabase.setRecoveryStatus(Binder.getCallingUid(), alias, status);
        if (updatedRows < 0) {
            throw new ServiceSpecificException(22, "Failed to set the key recovery status in the local DB.");
        }
    }

    public Map<String, Integer> getRecoveryStatus() throws RemoteException {
        checkRecoverKeyStorePermission();
        return this.mDatabase.getStatusForAllKeys(Binder.getCallingUid());
    }

    public void setRecoverySecretTypes(int[] secretTypes) throws RemoteException {
        checkRecoverKeyStorePermission();
        Objects.requireNonNull(secretTypes, "secretTypes is null");
        int userId = UserHandle.getCallingUserId();
        int uid = Binder.getCallingUid();
        int[] currentSecretTypes = this.mDatabase.getRecoverySecretTypes(userId, uid);
        if (Arrays.equals(secretTypes, currentSecretTypes)) {
            Log.v(TAG, "Not updating secret types - same as old value.");
            return;
        }
        long updatedRows = this.mDatabase.setRecoverySecretTypes(userId, uid, secretTypes);
        if (updatedRows < 0) {
            throw new ServiceSpecificException(22, "Database error trying to set secret types.");
        }
        if (currentSecretTypes.length == 0) {
            Log.i(TAG, "Initialized secret types.");
            return;
        }
        Log.i(TAG, "Updated secret types. Snapshot pending.");
        if (this.mDatabase.getSnapshotVersion(userId, uid) != null) {
            this.mDatabase.setShouldCreateSnapshot(userId, uid, true);
            Log.i(TAG, "Updated secret types. Snapshot must be updated");
            return;
        }
        Log.i(TAG, "Updated secret types. Snapshot didn't exist");
    }

    public int[] getRecoverySecretTypes() throws RemoteException {
        checkRecoverKeyStorePermission();
        return this.mDatabase.getRecoverySecretTypes(UserHandle.getCallingUserId(), Binder.getCallingUid());
    }

    byte[] startRecoverySession(String sessionId, byte[] verifierPublicKey, byte[] vaultParams, byte[] vaultChallenge, List<KeyChainProtectionParams> secrets) throws RemoteException {
        checkRecoverKeyStorePermission();
        int uid = Binder.getCallingUid();
        if (secrets.size() != 1) {
            throw new UnsupportedOperationException("Only a single KeyChainProtectionParams is supported");
        }
        try {
            PublicKey publicKey = KeySyncUtils.deserializePublicKey(verifierPublicKey);
            if (!publicKeysMatch(publicKey, vaultParams)) {
                throw new ServiceSpecificException(28, "The public keys given in verifierPublicKey and vaultParams do not match.");
            }
            byte[] keyClaimant = KeySyncUtils.generateKeyClaimant();
            byte[] kfHash = secrets.get(0).getSecret();
            this.mRecoverySessionStorage.add(uid, new RecoverySessionStorage.Entry(sessionId, kfHash, keyClaimant, vaultParams));
            Log.i(TAG, "Received VaultParams for recovery: " + HexDump.toHexString(vaultParams));
            try {
                byte[] thmKfHash = KeySyncUtils.calculateThmKfHash(kfHash);
                return KeySyncUtils.encryptRecoveryClaim(publicKey, vaultParams, vaultChallenge, thmKfHash, keyClaimant);
            } catch (InvalidKeyException e) {
                throw new ServiceSpecificException(25, e.getMessage());
            } catch (NoSuchAlgorithmException e2) {
                Log.wtf(TAG, "SecureBox algorithm missing. AOSP must support this.", e2);
                throw new ServiceSpecificException(22, e2.getMessage());
            }
        } catch (InvalidKeySpecException e3) {
            throw new ServiceSpecificException(25, e3.getMessage());
        }
    }

    public byte[] startRecoverySessionWithCertPath(String sessionId, String rootCertificateAlias, RecoveryCertPath verifierCertPath, byte[] vaultParams, byte[] vaultChallenge, List<KeyChainProtectionParams> secrets) throws RemoteException {
        checkRecoverKeyStorePermission();
        String rootCertificateAlias2 = this.mTestCertHelper.getDefaultCertificateAliasIfEmpty(rootCertificateAlias);
        Objects.requireNonNull(sessionId, "invalid session");
        Objects.requireNonNull(verifierCertPath, "verifierCertPath is null");
        Objects.requireNonNull(vaultParams, "vaultParams is null");
        Objects.requireNonNull(vaultChallenge, "vaultChallenge is null");
        Objects.requireNonNull(secrets, "secrets is null");
        try {
            CertPath certPath = verifierCertPath.getCertPath();
            try {
                CertUtils.validateCertPath(this.mTestCertHelper.getRootCertificate(rootCertificateAlias2), certPath);
                byte[] verifierPublicKey = certPath.getCertificates().get(0).getPublicKey().getEncoded();
                if (verifierPublicKey == null) {
                    Log.e(TAG, "Failed to encode verifierPublicKey");
                    throw new ServiceSpecificException(25, "Failed to encode verifierPublicKey");
                }
                return startRecoverySession(sessionId, verifierPublicKey, vaultParams, vaultChallenge, secrets);
            } catch (CertValidationException e) {
                Log.e(TAG, "Failed to validate the given cert path", e);
                throw new ServiceSpecificException(28, e.getMessage());
            }
        } catch (CertificateException e2) {
            throw new ServiceSpecificException(25, e2.getMessage());
        }
    }

    public Map<String, String> recoverKeyChainSnapshot(String sessionId, byte[] encryptedRecoveryKey, List<WrappedApplicationKey> applicationKeys) throws RemoteException {
        checkRecoverKeyStorePermission();
        int userId = UserHandle.getCallingUserId();
        int uid = Binder.getCallingUid();
        RecoverySessionStorage.Entry sessionEntry = this.mRecoverySessionStorage.get(uid, sessionId);
        try {
            if (sessionEntry == null) {
                throw new ServiceSpecificException(24, String.format(Locale.US, "Application uid=%d does not have pending session '%s'", Integer.valueOf(uid), sessionId));
            }
            try {
                byte[] recoveryKey = decryptRecoveryKey(sessionEntry, encryptedRecoveryKey);
                Map<String, byte[]> keysByAlias = recoverApplicationKeys(recoveryKey, applicationKeys);
                return importKeyMaterials(userId, uid, keysByAlias);
            } catch (KeyStoreException e) {
                throw new ServiceSpecificException(22, e.getMessage());
            }
        } finally {
            sessionEntry.destroy();
            this.mRecoverySessionStorage.remove(uid);
        }
    }

    private Map<String, String> importKeyMaterials(int userId, int uid, Map<String, byte[]> keysByAlias) throws KeyStoreException {
        ArrayMap<String, String> grantAliasesByAlias = new ArrayMap<>(keysByAlias.size());
        for (String alias : keysByAlias.keySet()) {
            this.mApplicationKeyStorage.setSymmetricKeyEntry(userId, uid, alias, keysByAlias.get(alias));
            String grantAlias = getAlias(userId, uid, alias);
            Log.i(TAG, String.format(Locale.US, "Import %s -> %s", alias, grantAlias));
            grantAliasesByAlias.put(alias, grantAlias);
        }
        return grantAliasesByAlias;
    }

    private String getAlias(int userId, int uid, String alias) {
        return this.mApplicationKeyStorage.getGrantAlias(userId, uid, alias);
    }

    public void closeSession(String sessionId) throws RemoteException {
        checkRecoverKeyStorePermission();
        Objects.requireNonNull(sessionId, "invalid session");
        this.mRecoverySessionStorage.remove(Binder.getCallingUid(), sessionId);
    }

    public void removeKey(String alias) throws RemoteException {
        checkRecoverKeyStorePermission();
        Objects.requireNonNull(alias, "alias is null");
        int uid = Binder.getCallingUid();
        int userId = UserHandle.getCallingUserId();
        boolean wasRemoved = this.mDatabase.removeKey(uid, alias);
        if (wasRemoved) {
            this.mDatabase.setShouldCreateSnapshot(userId, uid, true);
            this.mApplicationKeyStorage.deleteEntry(userId, uid, alias);
        }
    }

    @Deprecated
    public String generateKey(String alias) throws RemoteException {
        return generateKeyWithMetadata(alias, null);
    }

    public String generateKeyWithMetadata(String alias, byte[] metadata) throws RemoteException {
        checkRecoverKeyStorePermission();
        Objects.requireNonNull(alias, "alias is null");
        int uid = Binder.getCallingUid();
        int userId = UserHandle.getCallingUserId();
        try {
            PlatformEncryptionKey encryptionKey = this.mPlatformKeyManager.getEncryptKey(userId);
            try {
                byte[] secretKey = this.mRecoverableKeyGenerator.generateAndStoreKey(encryptionKey, userId, uid, alias, metadata);
                this.mApplicationKeyStorage.setSymmetricKeyEntry(userId, uid, alias, secretKey);
                return getAlias(userId, uid, alias);
            } catch (RecoverableKeyStorageException | InvalidKeyException | KeyStoreException e) {
                throw new ServiceSpecificException(22, e.getMessage());
            }
        } catch (IOException | KeyStoreException | UnrecoverableKeyException e2) {
            throw new ServiceSpecificException(22, e2.getMessage());
        } catch (NoSuchAlgorithmException e3) {
            throw new RuntimeException(e3);
        }
    }

    @Deprecated
    public String importKey(String alias, byte[] keyBytes) throws RemoteException {
        return importKeyWithMetadata(alias, keyBytes, null);
    }

    public String importKeyWithMetadata(String alias, byte[] keyBytes, byte[] metadata) throws RemoteException {
        checkRecoverKeyStorePermission();
        Objects.requireNonNull(alias, "alias is null");
        Objects.requireNonNull(keyBytes, "keyBytes is null");
        if (keyBytes.length != 32) {
            Log.e(TAG, "The given key for import doesn't have the required length 256");
            throw new ServiceSpecificException(27, "The given key does not contain 256 bits.");
        }
        int uid = Binder.getCallingUid();
        int userId = UserHandle.getCallingUserId();
        try {
            PlatformEncryptionKey encryptionKey = this.mPlatformKeyManager.getEncryptKey(userId);
            try {
                this.mRecoverableKeyGenerator.importKey(encryptionKey, userId, uid, alias, keyBytes, metadata);
                this.mApplicationKeyStorage.setSymmetricKeyEntry(userId, uid, alias, keyBytes);
                return getAlias(userId, uid, alias);
            } catch (RecoverableKeyStorageException | InvalidKeyException | KeyStoreException e) {
                throw new ServiceSpecificException(22, e.getMessage());
            }
        } catch (IOException | KeyStoreException | UnrecoverableKeyException e2) {
            throw new ServiceSpecificException(22, e2.getMessage());
        } catch (NoSuchAlgorithmException e3) {
            throw new RuntimeException(e3);
        }
    }

    public String getKey(String alias) throws RemoteException {
        checkRecoverKeyStorePermission();
        Objects.requireNonNull(alias, "alias is null");
        int uid = Binder.getCallingUid();
        int userId = UserHandle.getCallingUserId();
        return getAlias(userId, uid, alias);
    }

    private byte[] decryptRecoveryKey(RecoverySessionStorage.Entry sessionEntry, byte[] encryptedClaimResponse) throws RemoteException, ServiceSpecificException {
        try {
            byte[] locallyEncryptedKey = KeySyncUtils.decryptRecoveryClaimResponse(sessionEntry.getKeyClaimant(), sessionEntry.getVaultParams(), encryptedClaimResponse);
            try {
                return KeySyncUtils.decryptRecoveryKey(sessionEntry.getLskfHash(), locallyEncryptedKey);
            } catch (InvalidKeyException e) {
                Log.e(TAG, "Got InvalidKeyException during decrypting recovery key", e);
                throw new ServiceSpecificException(26, "Failed to decrypt recovery key " + e.getMessage());
            } catch (NoSuchAlgorithmException e2) {
                throw new ServiceSpecificException(22, e2.getMessage());
            } catch (AEADBadTagException e3) {
                Log.e(TAG, "Got AEADBadTagException during decrypting recovery key", e3);
                throw new ServiceSpecificException(26, "Failed to decrypt recovery key " + e3.getMessage());
            }
        } catch (InvalidKeyException e4) {
            Log.e(TAG, "Got InvalidKeyException during decrypting recovery claim response", e4);
            throw new ServiceSpecificException(26, "Failed to decrypt recovery key " + e4.getMessage());
        } catch (NoSuchAlgorithmException e5) {
            throw new ServiceSpecificException(22, e5.getMessage());
        } catch (AEADBadTagException e6) {
            Log.e(TAG, "Got AEADBadTagException during decrypting recovery claim response", e6);
            throw new ServiceSpecificException(26, "Failed to decrypt recovery key " + e6.getMessage());
        }
    }

    private Map<String, byte[]> recoverApplicationKeys(byte[] recoveryKey, List<WrappedApplicationKey> applicationKeys) throws RemoteException {
        HashMap<String, byte[]> keyMaterialByAlias = new HashMap<>();
        for (WrappedApplicationKey applicationKey : applicationKeys) {
            String alias = applicationKey.getAlias();
            byte[] encryptedKeyMaterial = applicationKey.getEncryptedKeyMaterial();
            byte[] keyMetadata = applicationKey.getMetadata();
            try {
                byte[] keyMaterial = KeySyncUtils.decryptApplicationKey(recoveryKey, encryptedKeyMaterial, keyMetadata);
                keyMaterialByAlias.put(alias, keyMaterial);
            } catch (InvalidKeyException e) {
                Log.e(TAG, "Got InvalidKeyException during decrypting application key with alias: " + alias, e);
                throw new ServiceSpecificException(26, "Failed to recover key with alias '" + alias + "': " + e.getMessage());
            } catch (NoSuchAlgorithmException e2) {
                Log.wtf(TAG, "Missing SecureBox algorithm. AOSP required to support this.", e2);
                throw new ServiceSpecificException(22, e2.getMessage());
            } catch (AEADBadTagException e3) {
                Log.e(TAG, "Got AEADBadTagException during decrypting application key with alias: " + alias, e3);
            }
        }
        if (!applicationKeys.isEmpty() && keyMaterialByAlias.isEmpty()) {
            Log.e(TAG, "Failed to recover any of the application keys.");
            throw new ServiceSpecificException(26, "Failed to recover any of the application keys.");
        }
        return keyMaterialByAlias;
    }

    public void lockScreenSecretAvailable(int storedHashType, byte[] credential, int userId) {
        try {
            this.mExecutorService.schedule(KeySyncTask.newInstance(this.mContext, this.mDatabase, this.mSnapshotStorage, this.mListenersStorage, userId, storedHashType, credential, false), SYNC_DELAY_MILLIS, TimeUnit.MILLISECONDS);
        } catch (InsecureUserException e) {
            Log.wtf(TAG, "Impossible - insecure user, but user just entered lock screen", e);
        } catch (KeyStoreException e2) {
            Log.e(TAG, "Key store error encountered during recoverable key sync", e2);
        } catch (NoSuchAlgorithmException e3) {
            Log.wtf(TAG, "Should never happen - algorithm unavailable for KeySync", e3);
        }
    }

    public void lockScreenSecretChanged(int storedHashType, byte[] credential, int userId) {
        try {
            this.mExecutorService.schedule(KeySyncTask.newInstance(this.mContext, this.mDatabase, this.mSnapshotStorage, this.mListenersStorage, userId, storedHashType, credential, true), SYNC_DELAY_MILLIS, TimeUnit.MILLISECONDS);
        } catch (InsecureUserException e) {
            Log.e(TAG, "InsecureUserException during lock screen secret update", e);
        } catch (KeyStoreException e2) {
            Log.e(TAG, "Key store error encountered during recoverable key sync", e2);
        } catch (NoSuchAlgorithmException e3) {
            Log.wtf(TAG, "Should never happen - algorithm unavailable for KeySync", e3);
        }
    }

    private void checkRecoverKeyStorePermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.RECOVER_KEYSTORE", "Caller " + Binder.getCallingUid() + " doesn't have RecoverKeyStore permission.");
        int userId = UserHandle.getCallingUserId();
        int uid = Binder.getCallingUid();
        this.mCleanupManager.registerRecoveryAgent(userId, uid);
    }

    private boolean publicKeysMatch(PublicKey publicKey, byte[] vaultParams) {
        byte[] encodedPublicKey = SecureBox.encodePublicKey(publicKey);
        return Arrays.equals(encodedPublicKey, Arrays.copyOf(vaultParams, encodedPublicKey.length));
    }
}
