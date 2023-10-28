package com.android.server.locksettings;

import android.app.admin.PasswordMetrics;
import android.content.Context;
import android.content.pm.UserInfo;
import android.hardware.weaver.V1_0.IWeaver;
import android.hardware.weaver.V1_0.WeaverConfig;
import android.hardware.weaver.V1_0.WeaverReadResponse;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.UserManager;
import android.security.Scrypt;
import android.service.gatekeeper.GateKeeperResponse;
import android.service.gatekeeper.IGateKeeperService;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Slog;
import com.android.internal.util.ArrayUtils;
import com.android.internal.widget.ICheckCredentialProgressCallback;
import com.android.internal.widget.IWeakEscrowTokenRemovedListener;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockscreenCredential;
import com.android.internal.widget.VerifyCredentialResponse;
import com.android.server.locksettings.LockSettingsStorage;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import libcore.util.HexEncoding;
/* loaded from: classes.dex */
public class SyntheticPasswordManager {
    public static final long DEFAULT_HANDLE = 0;
    private static final int INVALID_WEAVER_SLOT = -1;
    private static final String PASSWORD_DATA_NAME = "pwd";
    private static final String PASSWORD_METRICS_NAME = "metrics";
    private static final int PASSWORD_SALT_LENGTH = 16;
    private static final int PASSWORD_SCRYPT_N = 11;
    private static final int PASSWORD_SCRYPT_P = 1;
    private static final int PASSWORD_SCRYPT_R = 3;
    private static final int PASSWORD_TOKEN_LENGTH = 32;
    private static final int SECDISCARDABLE_LENGTH = 16384;
    private static final String SECDISCARDABLE_NAME = "secdis";
    private static final String SP_BLOB_NAME = "spblob";
    private static final String SP_E0_NAME = "e0";
    private static final String SP_HANDLE_NAME = "handle";
    private static final String SP_P1_NAME = "p1";
    private static final boolean SYNTHETIC_PASSWORD_BACKUP_SUPPORT = true;
    private static final byte SYNTHETIC_PASSWORD_LENGTH = 32;
    private static final byte SYNTHETIC_PASSWORD_PASSWORD_BASED = 0;
    private static final byte SYNTHETIC_PASSWORD_STRONG_TOKEN_BASED = 1;
    private static final byte SYNTHETIC_PASSWORD_VERSION_V1 = 1;
    private static final byte SYNTHETIC_PASSWORD_VERSION_V2 = 2;
    private static final byte SYNTHETIC_PASSWORD_VERSION_V3 = 3;
    private static final byte SYNTHETIC_PASSWORD_WEAK_TOKEN_BASED = 2;
    private static final String TAG = "SyntheticPasswordManager";
    static final int TOKEN_TYPE_STRONG = 0;
    static final int TOKEN_TYPE_WEAK = 1;
    private static final String WEAVER_SLOT_NAME = "weaver";
    private static final byte WEAVER_VERSION = 1;
    private final Context mContext;
    private PasswordSlotManager mPasswordSlotManager;
    private LockSettingsStorage mStorage;
    private final UserManager mUserManager;
    private IWeaver mWeaver;
    private WeaverConfig mWeaverConfig;
    private static final byte[] DEFAULT_PASSWORD = "default-password".getBytes();
    private static final byte[] PERSONALISATION_SECDISCARDABLE = "secdiscardable-transform".getBytes();
    private static final byte[] PERSONALIZATION_KEY_STORE_PASSWORD = "keystore-password".getBytes();
    private static final byte[] PERSONALIZATION_USER_GK_AUTH = "user-gk-authentication".getBytes();
    private static final byte[] PERSONALIZATION_SP_GK_AUTH = "sp-gk-authentication".getBytes();
    private static final byte[] PERSONALIZATION_FBE_KEY = "fbe-key".getBytes();
    private static final byte[] PERSONALIZATION_AUTHSECRET_KEY = "authsecret-hal".getBytes();
    private static final byte[] PERSONALIZATION_SP_SPLIT = "sp-split".getBytes();
    private static final byte[] PERSONALIZATION_PASSWORD_HASH = "pw-hash".getBytes();
    private static final byte[] PERSONALIZATION_E0 = "e0-encryption".getBytes();
    private static final byte[] PERSONALISATION_WEAVER_PASSWORD = "weaver-pwd".getBytes();
    private static final byte[] PERSONALISATION_WEAVER_KEY = "weaver-key".getBytes();
    private static final byte[] PERSONALISATION_WEAVER_TOKEN = "weaver-token".getBytes();
    private static final byte[] PERSONALIZATION_PASSWORD_METRICS = "password-metrics".getBytes();
    private static final byte[] PERSONALISATION_CONTEXT = "android-synthetic-password-personalization-context".getBytes();
    protected static final byte[] HEX_ARRAY = "0123456789ABCDEF".getBytes();
    private final RemoteCallbackList<IWeakEscrowTokenRemovedListener> mListeners = new RemoteCallbackList<>();
    private ArrayMap<Integer, ArrayMap<Long, TokenData>> tokenMap = new ArrayMap<>();

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    @interface TokenType {
    }

    native long nativeSidFromPasswordHandle(byte[] bArr);

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class AuthenticationResult {
        public AuthenticationToken authToken;
        public VerifyCredentialResponse gkResponse;

        AuthenticationResult() {
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class AuthenticationToken {
        private byte[] mEncryptedEscrowSplit0;
        private byte[] mEscrowSplit1;
        private byte[] mSyntheticPassword;
        private final byte mVersion;

        /* JADX INFO: Access modifiers changed from: package-private */
        public AuthenticationToken(byte version) {
            this.mVersion = version;
        }

        private byte[] derivePassword(byte[] personalization) {
            return this.mVersion == 3 ? new SP800Derive(this.mSyntheticPassword).withContext(personalization, SyntheticPasswordManager.PERSONALISATION_CONTEXT) : SyntheticPasswordCrypto.personalisedHash(personalization, this.mSyntheticPassword);
        }

        public byte[] deriveKeyStorePassword() {
            return SyntheticPasswordManager.bytesToHex(derivePassword(SyntheticPasswordManager.PERSONALIZATION_KEY_STORE_PASSWORD));
        }

        public byte[] deriveGkPassword() {
            return derivePassword(SyntheticPasswordManager.PERSONALIZATION_SP_GK_AUTH);
        }

        public byte[] deriveDiskEncryptionKey() {
            return derivePassword(SyntheticPasswordManager.PERSONALIZATION_FBE_KEY);
        }

        public byte[] deriveVendorAuthSecret() {
            return derivePassword(SyntheticPasswordManager.PERSONALIZATION_AUTHSECRET_KEY);
        }

        public byte[] derivePasswordHashFactor() {
            return derivePassword(SyntheticPasswordManager.PERSONALIZATION_PASSWORD_HASH);
        }

        public byte[] deriveMetricsKey() {
            return derivePassword(SyntheticPasswordManager.PERSONALIZATION_PASSWORD_METRICS);
        }

        public void setEscrowData(byte[] encryptedEscrowSplit0, byte[] escrowSplit1) {
            this.mEncryptedEscrowSplit0 = encryptedEscrowSplit0;
            this.mEscrowSplit1 = escrowSplit1;
        }

        public void recreateFromEscrow(byte[] escrowSplit0) {
            Objects.requireNonNull(this.mEscrowSplit1);
            Objects.requireNonNull(this.mEncryptedEscrowSplit0);
            recreate(escrowSplit0, this.mEscrowSplit1);
        }

        public void recreateDirectly(byte[] syntheticPassword) {
            this.mSyntheticPassword = Arrays.copyOf(syntheticPassword, syntheticPassword.length);
        }

        static AuthenticationToken create() {
            AuthenticationToken result = new AuthenticationToken((byte) 3);
            byte[] escrowSplit0 = SyntheticPasswordManager.secureRandom(32);
            byte[] escrowSplit1 = SyntheticPasswordManager.secureRandom(32);
            result.recreate(escrowSplit0, escrowSplit1);
            byte[] encrypteEscrowSplit0 = SyntheticPasswordCrypto.encrypt(result.mSyntheticPassword, SyntheticPasswordManager.PERSONALIZATION_E0, escrowSplit0);
            result.setEscrowData(encrypteEscrowSplit0, escrowSplit1);
            return result;
        }

        private void recreate(byte[] escrowSplit0, byte[] escrowSplit1) {
            this.mSyntheticPassword = String.valueOf(HexEncoding.encode(SyntheticPasswordCrypto.personalisedHash(SyntheticPasswordManager.PERSONALIZATION_SP_SPLIT, escrowSplit0, escrowSplit1))).getBytes();
        }

        public byte[] getEscrowSecret() {
            if (this.mEncryptedEscrowSplit0 == null) {
                return null;
            }
            return SyntheticPasswordCrypto.decrypt(this.mSyntheticPassword, SyntheticPasswordManager.PERSONALIZATION_E0, this.mEncryptedEscrowSplit0);
        }

        public byte[] getSyntheticPassword() {
            return this.mSyntheticPassword;
        }

        public byte getVersion() {
            return this.mVersion;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class PasswordData {
        public int credentialType;
        public byte[] passwordHandle;
        byte[] salt;
        byte scryptN;
        byte scryptP;
        byte scryptR;

        PasswordData() {
        }

        public static PasswordData create(int passwordType) {
            PasswordData result = new PasswordData();
            result.scryptN = (byte) 11;
            result.scryptR = (byte) 3;
            result.scryptP = (byte) 1;
            result.credentialType = passwordType;
            result.salt = SyntheticPasswordManager.secureRandom(16);
            return result;
        }

        public static PasswordData fromBytes(byte[] data) {
            PasswordData result = new PasswordData();
            ByteBuffer buffer = ByteBuffer.allocate(data.length);
            buffer.put(data, 0, data.length);
            buffer.flip();
            result.credentialType = buffer.getInt();
            result.scryptN = buffer.get();
            result.scryptR = buffer.get();
            result.scryptP = buffer.get();
            int saltLen = buffer.getInt();
            byte[] bArr = new byte[saltLen];
            result.salt = bArr;
            buffer.get(bArr);
            int handleLen = buffer.getInt();
            if (handleLen > 0) {
                byte[] bArr2 = new byte[handleLen];
                result.passwordHandle = bArr2;
                buffer.get(bArr2);
            } else {
                result.passwordHandle = null;
            }
            return result;
        }

        public byte[] toBytes() {
            int length = this.salt.length + 11 + 4;
            byte[] bArr = this.passwordHandle;
            ByteBuffer buffer = ByteBuffer.allocate(length + (bArr != null ? bArr.length : 0));
            buffer.putInt(this.credentialType);
            buffer.put(this.scryptN);
            buffer.put(this.scryptR);
            buffer.put(this.scryptP);
            buffer.putInt(this.salt.length);
            buffer.put(this.salt);
            byte[] bArr2 = this.passwordHandle;
            if (bArr2 != null && bArr2.length > 0) {
                buffer.putInt(bArr2.length);
                buffer.put(this.passwordHandle);
            } else {
                buffer.putInt(0);
            }
            return buffer.array();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class SyntheticPasswordBlob {
        byte[] mContent;
        byte mType;
        byte mVersion;

        SyntheticPasswordBlob() {
        }

        public static SyntheticPasswordBlob create(byte version, byte type, byte[] content) {
            SyntheticPasswordBlob result = new SyntheticPasswordBlob();
            result.mVersion = version;
            result.mType = type;
            result.mContent = content;
            return result;
        }

        public static SyntheticPasswordBlob fromBytes(byte[] data) {
            SyntheticPasswordBlob result = new SyntheticPasswordBlob();
            result.mVersion = data[0];
            result.mType = data[1];
            result.mContent = Arrays.copyOfRange(data, 2, data.length);
            return result;
        }

        public byte[] toByte() {
            byte[] bArr = this.mContent;
            byte[] blob = new byte[bArr.length + 1 + 1];
            blob[0] = this.mVersion;
            blob[1] = this.mType;
            System.arraycopy(bArr, 0, blob, 2, bArr.length);
            return blob;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class TokenData {
        byte[] aggregatedSecret;
        LockPatternUtils.EscrowTokenStateChangeCallback mCallback;
        int mType;
        byte[] secdiscardableOnDisk;
        byte[] weaverSecret;

        TokenData() {
        }
    }

    public SyntheticPasswordManager(Context context, LockSettingsStorage storage, UserManager userManager, PasswordSlotManager passwordSlotManager) {
        this.mContext = context;
        this.mStorage = storage;
        this.mUserManager = userManager;
        this.mPasswordSlotManager = passwordSlotManager;
    }

    protected IWeaver getWeaverService() throws RemoteException {
        try {
            return IWeaver.getService(true);
        } catch (NoSuchElementException e) {
            Slog.i(TAG, "Device does not support weaver");
            return null;
        }
    }

    public synchronized void initWeaverService() {
        if (this.mWeaver != null) {
            return;
        }
        try {
            this.mWeaverConfig = null;
            IWeaver weaverService = getWeaverService();
            this.mWeaver = weaverService;
            if (weaverService != null) {
                weaverService.getConfig(new IWeaver.getConfigCallback() { // from class: com.android.server.locksettings.SyntheticPasswordManager$$ExternalSyntheticLambda0
                    @Override // android.hardware.weaver.V1_0.IWeaver.getConfigCallback
                    public final void onValues(int i, WeaverConfig weaverConfig) {
                        SyntheticPasswordManager.this.m4601xe20a856c(i, weaverConfig);
                    }
                });
                this.mPasswordSlotManager.refreshActiveSlots(getUsedWeaverSlots());
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "Failed to get weaver service", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$initWeaverService$0$com-android-server-locksettings-SyntheticPasswordManager  reason: not valid java name */
    public /* synthetic */ void m4601xe20a856c(int status, WeaverConfig config) {
        if (status == 0 && config.slots > 0) {
            this.mWeaverConfig = config;
            return;
        }
        Slog.e(TAG, "Failed to get weaver config, status " + status + " slots: " + config.slots);
        this.mWeaver = null;
    }

    private synchronized boolean isWeaverAvailable() {
        boolean z;
        if (this.mWeaver == null) {
            initWeaverService();
        }
        if (this.mWeaver != null) {
            z = this.mWeaverConfig.slots > 0;
        }
        return z;
    }

    private byte[] weaverEnroll(int slot, byte[] key, byte[] value) {
        if (slot == -1 || slot >= this.mWeaverConfig.slots) {
            throw new IllegalArgumentException("Invalid slot for weaver");
        }
        if (key == null) {
            key = new byte[this.mWeaverConfig.keySize];
        } else if (key.length != this.mWeaverConfig.keySize) {
            throw new IllegalArgumentException("Invalid key size for weaver");
        }
        if (value == null) {
            value = secureRandom(this.mWeaverConfig.valueSize);
        }
        try {
            int writeStatus = this.mWeaver.write(slot, toByteArrayList(key), toByteArrayList(value));
            if (writeStatus != 0) {
                Slog.e(TAG, "weaver write failed, slot: " + slot + " status: " + writeStatus);
                return null;
            }
            return value;
        } catch (RemoteException e) {
            Slog.e(TAG, "weaver write failed", e);
            return null;
        }
    }

    private VerifyCredentialResponse weaverVerify(final int slot, byte[] key) {
        if (slot == -1 || slot >= this.mWeaverConfig.slots) {
            throw new IllegalArgumentException("Invalid slot for weaver");
        }
        if (key == null) {
            key = new byte[this.mWeaverConfig.keySize];
        } else if (key.length != this.mWeaverConfig.keySize) {
            throw new IllegalArgumentException("Invalid key size for weaver");
        }
        final VerifyCredentialResponse[] response = new VerifyCredentialResponse[1];
        try {
            this.mWeaver.read(slot, toByteArrayList(key), new IWeaver.readCallback() { // from class: com.android.server.locksettings.SyntheticPasswordManager$$ExternalSyntheticLambda1
                @Override // android.hardware.weaver.V1_0.IWeaver.readCallback
                public final void onValues(int i, WeaverReadResponse weaverReadResponse) {
                    SyntheticPasswordManager.lambda$weaverVerify$1(response, slot, i, weaverReadResponse);
                }
            });
        } catch (RemoteException e) {
            response[0] = VerifyCredentialResponse.ERROR;
            Slog.e(TAG, "weaver read failed, slot: " + slot, e);
        }
        return response[0];
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$weaverVerify$1(VerifyCredentialResponse[] response, int slot, int status, WeaverReadResponse readResponse) {
        switch (status) {
            case 0:
                response[0] = new VerifyCredentialResponse.Builder().setGatekeeperHAT(fromByteArrayList(readResponse.value)).build();
                return;
            case 1:
                response[0] = VerifyCredentialResponse.ERROR;
                Slog.e(TAG, "weaver read failed (FAILED), slot: " + slot);
                return;
            case 2:
                if (readResponse.timeout == 0) {
                    response[0] = VerifyCredentialResponse.ERROR;
                    Slog.e(TAG, "weaver read failed (INCORRECT_KEY), slot: " + slot);
                    return;
                }
                response[0] = VerifyCredentialResponse.fromTimeout(readResponse.timeout);
                Slog.e(TAG, "weaver read failed (INCORRECT_KEY/THROTTLE), slot: " + slot);
                return;
            case 3:
                response[0] = VerifyCredentialResponse.fromTimeout(readResponse.timeout);
                Slog.e(TAG, "weaver read failed (THROTTLE), slot: " + slot);
                return;
            default:
                response[0] = VerifyCredentialResponse.ERROR;
                Slog.e(TAG, "weaver read unknown status " + status + ", slot: " + slot);
                return;
        }
    }

    public void removeUser(IGateKeeperService gatekeeper, int userId) {
        for (Long l : this.mStorage.listSyntheticPasswordHandlesForUser(SP_BLOB_NAME, userId)) {
            long handle = l.longValue();
            destroyWeaverSlot(handle, userId);
            destroySPBlobKey(getKeyName(handle));
        }
        try {
            gatekeeper.clearSecureUserId(fakeUid(userId));
        } catch (RemoteException e) {
            Slog.w(TAG, "Failed to clear SID from gatekeeper");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getCredentialType(long handle, int userId) {
        PasswordData pwd;
        byte[] passwordData = loadState(PASSWORD_DATA_NAME, handle, userId);
        if (passwordData == null) {
            Slog.w(TAG, "getCredentialType: encountered empty password data for user " + userId);
            return -1;
        }
        try {
            pwd = PasswordData.fromBytes(passwordData);
        } catch (BufferUnderflowException e) {
            Slog.e(TAG, "getCredentialType e:", e);
            this.mStorage.copySyntheticPasswordState(userId, handle, PASSWORD_DATA_NAME);
            pwd = PasswordData.fromBytes(loadState(PASSWORD_DATA_NAME, handle, userId));
        }
        return pwd.credentialType;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static int getFrpCredentialType(byte[] payload) {
        if (payload == null) {
            return -1;
        }
        return PasswordData.fromBytes(payload).credentialType;
    }

    public AuthenticationToken newSyntheticPasswordAndSid(IGateKeeperService gatekeeper, byte[] hash, LockscreenCredential credential, int userId) {
        AuthenticationToken result = AuthenticationToken.create();
        if (hash != null) {
            try {
                GateKeeperResponse response = gatekeeper.enroll(userId, hash, credential.getCredential(), result.deriveGkPassword());
                if (response.getResponseCode() != 0) {
                    Slog.w(TAG, "Fail to migrate SID, assuming no SID, user " + userId);
                    clearSidForUser(userId);
                } else {
                    saveSyntheticPasswordHandle(response.getPayload(), userId);
                }
            } catch (RemoteException e) {
                throw new IllegalStateException("Failed to enroll credential duing SP init", e);
            }
        } else {
            clearSidForUser(userId);
        }
        saveEscrowData(result, userId);
        return result;
    }

    public void newSidForUser(IGateKeeperService gatekeeper, AuthenticationToken authToken, int userId) {
        try {
            GateKeeperResponse response = gatekeeper.enroll(userId, (byte[]) null, (byte[]) null, authToken.deriveGkPassword());
            if (response.getResponseCode() != 0) {
                throw new IllegalStateException("Fail to create new SID for user " + userId + " response: " + response.getResponseCode());
            }
            saveSyntheticPasswordHandle(response.getPayload(), userId);
        } catch (RemoteException e) {
            throw new IllegalStateException("Failed to create new SID for user", e);
        }
    }

    public void clearSidForUser(int userId) {
        destroyState(SP_HANDLE_NAME, 0L, userId);
    }

    public boolean hasSidForUser(int userId) {
        return hasState(SP_HANDLE_NAME, 0L, userId);
    }

    private byte[] loadSyntheticPasswordHandle(int userId) {
        return loadState(SP_HANDLE_NAME, 0L, userId);
    }

    private void saveSyntheticPasswordHandle(byte[] spHandle, int userId) {
        saveState(SP_HANDLE_NAME, spHandle, 0L, userId);
    }

    private boolean loadEscrowData(AuthenticationToken authToken, int userId) {
        byte[] e0 = loadState(SP_E0_NAME, 0L, userId);
        byte[] p1 = loadState(SP_P1_NAME, 0L, userId);
        authToken.setEscrowData(e0, p1);
        return (e0 == null || p1 == null) ? false : true;
    }

    private void saveEscrowData(AuthenticationToken authToken, int userId) {
        saveState(SP_E0_NAME, authToken.mEncryptedEscrowSplit0, 0L, userId);
        saveState(SP_P1_NAME, authToken.mEscrowSplit1, 0L, userId);
    }

    public boolean hasEscrowData(int userId) {
        return hasState(SP_E0_NAME, 0L, userId) && hasState(SP_P1_NAME, 0L, userId);
    }

    public void destroyEscrowData(int userId) {
        destroyState(SP_E0_NAME, 0L, userId);
        destroyState(SP_P1_NAME, 0L, userId);
    }

    private int loadWeaverSlot(long handle, int userId) {
        byte[] data = loadState(WEAVER_SLOT_NAME, handle, userId);
        if (data == null || data.length != 5) {
            return -1;
        }
        ByteBuffer buffer = ByteBuffer.allocate(5);
        buffer.put(data, 0, data.length);
        buffer.flip();
        if (buffer.get() != 1) {
            Slog.e(TAG, "Invalid weaver slot version of handle " + handle);
            return -1;
        }
        return buffer.getInt();
    }

    private void saveWeaverSlot(int slot, long handle, int userId) {
        ByteBuffer buffer = ByteBuffer.allocate(5);
        buffer.put((byte) 1);
        buffer.putInt(slot);
        saveState(WEAVER_SLOT_NAME, buffer.array(), handle, userId);
    }

    private void destroyWeaverSlot(long handle, int userId) {
        int slot = loadWeaverSlot(handle, userId);
        destroyState(WEAVER_SLOT_NAME, handle, userId);
        if (slot != -1) {
            Set<Integer> usedSlots = getUsedWeaverSlots();
            if (!usedSlots.contains(Integer.valueOf(slot))) {
                Slog.i(TAG, "Destroy weaver slot " + slot + " for user " + userId);
                weaverEnroll(slot, null, null);
                this.mPasswordSlotManager.markSlotDeleted(slot);
                return;
            }
            Slog.w(TAG, "Skip destroying reused weaver slot " + slot + " for user " + userId);
        }
    }

    private Set<Integer> getUsedWeaverSlots() {
        Map<Integer, List<Long>> slotHandles = this.mStorage.listSyntheticPasswordHandlesForAllUsers(WEAVER_SLOT_NAME);
        HashSet<Integer> slots = new HashSet<>();
        for (Map.Entry<Integer, List<Long>> entry : slotHandles.entrySet()) {
            for (Long handle : entry.getValue()) {
                int slot = loadWeaverSlot(handle.longValue(), entry.getKey().intValue());
                slots.add(Integer.valueOf(slot));
            }
        }
        return slots;
    }

    private int getNextAvailableWeaverSlot() {
        Set<Integer> usedSlots = getUsedWeaverSlots();
        usedSlots.addAll(this.mPasswordSlotManager.getUsedSlots());
        for (int i = 0; i < this.mWeaverConfig.slots; i++) {
            if (!usedSlots.contains(Integer.valueOf(i))) {
                return i;
            }
        }
        throw new IllegalStateException("Run out of weaver slots.");
    }

    public long createPasswordBasedSyntheticPassword(IGateKeeperService gatekeeper, LockscreenCredential credential, AuthenticationToken authToken, int userId) {
        byte[] applicationId;
        long sid;
        long handle = generateHandle();
        PasswordData pwd = PasswordData.create(credential.getType());
        byte[] pwdToken = computePasswordToken(credential, pwd);
        if (isWeaverAvailable()) {
            int weaverSlot = getNextAvailableWeaverSlot();
            Slog.i(TAG, "Weaver enroll password to slot " + weaverSlot + " for user " + userId);
            byte[] weaverSecret = weaverEnroll(weaverSlot, passwordTokenToWeaverKey(pwdToken), null);
            if (weaverSecret == null) {
                throw new IllegalStateException("Fail to enroll user password under weaver " + userId);
            }
            saveWeaverSlot(weaverSlot, handle, userId);
            this.mPasswordSlotManager.markSlotInUse(weaverSlot);
            synchronizeWeaverFrpPassword(pwd, 0, userId, weaverSlot);
            pwd.passwordHandle = null;
            applicationId = transformUnderWeaverSecret(pwdToken, weaverSecret);
            sid = 0;
        } else {
            try {
                gatekeeper.clearSecureUserId(fakeUid(userId));
            } catch (RemoteException e) {
                Slog.w(TAG, "Failed to clear SID from gatekeeper");
            }
            try {
                GateKeeperResponse response = gatekeeper.enroll(fakeUid(userId), (byte[]) null, (byte[]) null, passwordTokenToGkInput(pwdToken));
                if (response.getResponseCode() != 0) {
                    throw new IllegalStateException("Fail to enroll user password when creating SP for user " + userId);
                }
                pwd.passwordHandle = response.getPayload();
                long sid2 = sidFromPasswordHandle(pwd.passwordHandle);
                byte[] applicationId2 = transformUnderSecdiscardable(pwdToken, createSecdiscardable(handle, userId));
                synchronizeFrpPassword(pwd, 0, userId);
                applicationId = applicationId2;
                sid = sid2;
            } catch (RemoteException e2) {
                throw new IllegalStateException("Failed to enroll password for new SP blob", e2);
            }
        }
        saveState(PASSWORD_DATA_NAME, pwd.toBytes(), handle, userId);
        savePasswordMetrics(credential, authToken, handle, userId);
        createSyntheticPasswordBlob(handle, (byte) 0, authToken, applicationId, sid, userId);
        return handle;
    }

    public VerifyCredentialResponse verifyFrpCredential(IGateKeeperService gatekeeper, LockscreenCredential userCredential, ICheckCredentialProgressCallback progressCallback) {
        LockSettingsStorage.PersistentData persistentData = this.mStorage.readPersistentDataBlock();
        if (persistentData.type == 1) {
            PasswordData pwd = PasswordData.fromBytes(persistentData.payload);
            byte[] pwdToken = computePasswordToken(userCredential, pwd);
            try {
                GateKeeperResponse response = gatekeeper.verifyChallenge(fakeUid(persistentData.userId), 0L, pwd.passwordHandle, passwordTokenToGkInput(pwdToken));
                return VerifyCredentialResponse.fromGateKeeperResponse(response);
            } catch (RemoteException e) {
                Slog.e(TAG, "FRP verifyChallenge failed", e);
                return VerifyCredentialResponse.ERROR;
            }
        } else if (persistentData.type == 2) {
            if (!isWeaverAvailable()) {
                Slog.e(TAG, "No weaver service to verify SP-based FRP credential");
                return VerifyCredentialResponse.ERROR;
            }
            byte[] pwdToken2 = computePasswordToken(userCredential, PasswordData.fromBytes(persistentData.payload));
            int weaverSlot = persistentData.userId;
            return weaverVerify(weaverSlot, passwordTokenToWeaverKey(pwdToken2)).stripPayload();
        } else {
            Slog.e(TAG, "persistentData.type must be TYPE_SP or TYPE_SP_WEAVER, but is " + persistentData.type);
            return VerifyCredentialResponse.ERROR;
        }
    }

    public void migrateFrpPasswordLocked(long handle, UserInfo userInfo, int requestedQuality) {
        if (this.mStorage.getPersistentDataBlockManager() != null && LockPatternUtils.userOwnsFrpCredential(this.mContext, userInfo)) {
            PasswordData pwd = PasswordData.fromBytes(loadState(PASSWORD_DATA_NAME, handle, userInfo.id));
            if (pwd.credentialType != -1) {
                int weaverSlot = loadWeaverSlot(handle, userInfo.id);
                if (weaverSlot != -1) {
                    synchronizeWeaverFrpPassword(pwd, requestedQuality, userInfo.id, weaverSlot);
                } else {
                    synchronizeFrpPassword(pwd, requestedQuality, userInfo.id);
                }
            }
        }
    }

    private void synchronizeFrpPassword(PasswordData pwd, int requestedQuality, int userId) {
        if (this.mStorage.getPersistentDataBlockManager() != null && LockPatternUtils.userOwnsFrpCredential(this.mContext, this.mUserManager.getUserInfo(userId))) {
            if (pwd.credentialType != -1) {
                this.mStorage.writePersistentDataBlock(1, userId, requestedQuality, pwd.toBytes());
            } else {
                this.mStorage.writePersistentDataBlock(0, userId, 0, null);
            }
        }
    }

    private void synchronizeWeaverFrpPassword(PasswordData pwd, int requestedQuality, int userId, int weaverSlot) {
        if (this.mStorage.getPersistentDataBlockManager() != null && LockPatternUtils.userOwnsFrpCredential(this.mContext, this.mUserManager.getUserInfo(userId))) {
            if (pwd.credentialType != -1) {
                this.mStorage.writePersistentDataBlock(2, weaverSlot, requestedQuality, pwd.toBytes());
            } else {
                this.mStorage.writePersistentDataBlock(0, 0, 0, null);
            }
        }
    }

    public long createStrongTokenBasedSyntheticPassword(byte[] token, int userId, LockPatternUtils.EscrowTokenStateChangeCallback changeCallback) {
        return createTokenBasedSyntheticPassword(token, 0, userId, changeCallback);
    }

    public long createWeakTokenBasedSyntheticPassword(byte[] token, int userId, LockPatternUtils.EscrowTokenStateChangeCallback changeCallback) {
        return createTokenBasedSyntheticPassword(token, 1, userId, changeCallback);
    }

    public long createTokenBasedSyntheticPassword(byte[] token, int type, int userId, LockPatternUtils.EscrowTokenStateChangeCallback changeCallback) {
        long handle = generateHandle();
        if (!this.tokenMap.containsKey(Integer.valueOf(userId))) {
            this.tokenMap.put(Integer.valueOf(userId), new ArrayMap<>());
        }
        TokenData tokenData = new TokenData();
        tokenData.mType = type;
        byte[] secdiscardable = secureRandom(16384);
        if (isWeaverAvailable()) {
            tokenData.weaverSecret = secureRandom(this.mWeaverConfig.valueSize);
            tokenData.secdiscardableOnDisk = SyntheticPasswordCrypto.encrypt(tokenData.weaverSecret, PERSONALISATION_WEAVER_TOKEN, secdiscardable);
        } else {
            tokenData.secdiscardableOnDisk = secdiscardable;
            tokenData.weaverSecret = null;
        }
        tokenData.aggregatedSecret = transformUnderSecdiscardable(token, secdiscardable);
        tokenData.mCallback = changeCallback;
        this.tokenMap.get(Integer.valueOf(userId)).put(Long.valueOf(handle), tokenData);
        return handle;
    }

    public Set<Long> getPendingTokensForUser(int userId) {
        if (!this.tokenMap.containsKey(Integer.valueOf(userId))) {
            return Collections.emptySet();
        }
        return new ArraySet(this.tokenMap.get(Integer.valueOf(userId)).keySet());
    }

    public boolean removePendingToken(long handle, int userId) {
        return this.tokenMap.containsKey(Integer.valueOf(userId)) && this.tokenMap.get(Integer.valueOf(userId)).remove(Long.valueOf(handle)) != null;
    }

    public boolean activateTokenBasedSyntheticPassword(long handle, AuthenticationToken authToken, int userId) {
        TokenData tokenData;
        if (this.tokenMap.containsKey(Integer.valueOf(userId)) && (tokenData = this.tokenMap.get(Integer.valueOf(userId)).get(Long.valueOf(handle))) != null) {
            if (!loadEscrowData(authToken, userId)) {
                Slog.w(TAG, "User is not escrowable");
                return false;
            }
            if (isWeaverAvailable()) {
                int slot = getNextAvailableWeaverSlot();
                Slog.i(TAG, "Weaver enroll token to slot " + slot + " for user " + userId);
                if (weaverEnroll(slot, null, tokenData.weaverSecret) == null) {
                    Slog.e(TAG, "Failed to enroll weaver secret when activating token");
                    return false;
                }
                saveWeaverSlot(slot, handle, userId);
                this.mPasswordSlotManager.markSlotInUse(slot);
            }
            saveSecdiscardable(handle, tokenData.secdiscardableOnDisk, userId);
            createSyntheticPasswordBlob(handle, getTokenBasedBlobType(tokenData.mType), authToken, tokenData.aggregatedSecret, 0L, userId);
            this.tokenMap.get(Integer.valueOf(userId)).remove(Long.valueOf(handle));
            if (tokenData.mCallback != null) {
                tokenData.mCallback.onEscrowTokenActivated(handle, userId);
                return true;
            }
            return true;
        }
        return false;
    }

    private void createSyntheticPasswordBlob(long handle, byte type, AuthenticationToken authToken, byte[] applicationId, long sid, int userId) {
        byte[] secret;
        if (type == 1 || type == 2) {
            secret = authToken.getEscrowSecret();
        } else {
            secret = authToken.getSyntheticPassword();
        }
        byte[] content = createSPBlob(getKeyName(handle), secret, applicationId, sid);
        byte version = authToken.mVersion == 3 ? (byte) 3 : (byte) 2;
        SyntheticPasswordBlob blob = SyntheticPasswordBlob.create(version, type, content);
        saveState(SP_BLOB_NAME, blob.toByte(), handle, userId);
    }

    /* JADX WARN: Removed duplicated region for block: B:33:0x00d3  */
    /* JADX WARN: Removed duplicated region for block: B:34:0x00ff  */
    /* JADX WARN: Removed duplicated region for block: B:58:0x013d A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public AuthenticationResult unwrapPasswordBasedSyntheticPassword(IGateKeeperService gatekeeper, long handle, LockscreenCredential credential, int userId, ICheckCredentialProgressCallback progressCallback) {
        GateKeeperResponse reenrollResponse;
        byte[] pwdToken;
        long sid;
        byte[] applicationId;
        AuthenticationResult result = new AuthenticationResult();
        PasswordData pwd = PasswordData.fromBytes(loadState(PASSWORD_DATA_NAME, handle, userId));
        if (!credential.checkAgainstStoredType(pwd.credentialType)) {
            Slog.e(TAG, String.format("Credential type mismatch: expected %d actual %d", Integer.valueOf(pwd.credentialType), Integer.valueOf(credential.getType())));
            result.gkResponse = VerifyCredentialResponse.ERROR;
            return result;
        }
        byte[] pwdToken2 = computePasswordToken(credential, pwd);
        int weaverSlot = loadWeaverSlot(handle, userId);
        if (weaverSlot != -1) {
            if (isWeaverAvailable()) {
                result.gkResponse = weaverVerify(weaverSlot, passwordTokenToWeaverKey(pwdToken2));
                if (result.gkResponse.getResponseCode() != 0) {
                    return result;
                }
                sid = 0;
                applicationId = transformUnderWeaverSecret(pwdToken2, result.gkResponse.getGatekeeperHAT());
            } else {
                Slog.e(TAG, "No weaver service to unwrap password based SP");
                result.gkResponse = VerifyCredentialResponse.ERROR;
                return result;
            }
        } else {
            byte[] gkPwdToken = passwordTokenToGkInput(pwdToken2);
            try {
                GateKeeperResponse response = gatekeeper.verifyChallenge(fakeUid(userId), 0L, pwd.passwordHandle, gkPwdToken);
                int responseCode = response.getResponseCode();
                if (responseCode == 0) {
                    result.gkResponse = VerifyCredentialResponse.OK;
                    if (response.getShouldReEnroll()) {
                        try {
                            try {
                                reenrollResponse = gatekeeper.enroll(fakeUid(userId), pwd.passwordHandle, gkPwdToken, gkPwdToken);
                            } catch (RemoteException e) {
                                e = e;
                                Slog.w(TAG, "Fail to invoke gatekeeper.enroll", e);
                                reenrollResponse = GateKeeperResponse.ERROR;
                                if (reenrollResponse.getResponseCode() != 0) {
                                }
                                long sid2 = sidFromPasswordHandle(pwd.passwordHandle);
                                pwdToken2 = pwdToken;
                                sid = sid2;
                                applicationId = transformUnderSecdiscardable(pwdToken2, loadSecdiscardable(handle, userId));
                                if (progressCallback != null) {
                                }
                                result.authToken = unwrapSyntheticPasswordBlob(handle, (byte) 0, applicationId, sid, userId);
                                result.gkResponse = verifyChallenge(gatekeeper, result.authToken, 0L, userId);
                                if (result.authToken != null) {
                                    savePasswordMetrics(credential, result.authToken, handle, userId);
                                }
                                return result;
                            }
                        } catch (RemoteException e2) {
                            e = e2;
                        }
                        if (reenrollResponse.getResponseCode() != 0) {
                            pwd.passwordHandle = reenrollResponse.getPayload();
                            pwd.credentialType = credential.getType();
                            pwdToken = pwdToken2;
                            saveState(PASSWORD_DATA_NAME, pwd.toBytes(), handle, userId);
                            synchronizeFrpPassword(pwd, 0, userId);
                        } else {
                            pwdToken = pwdToken2;
                            Slog.w(TAG, "Fail to re-enroll user password for user " + userId);
                        }
                    } else {
                        pwdToken = pwdToken2;
                    }
                    long sid22 = sidFromPasswordHandle(pwd.passwordHandle);
                    pwdToken2 = pwdToken;
                    sid = sid22;
                    applicationId = transformUnderSecdiscardable(pwdToken2, loadSecdiscardable(handle, userId));
                } else if (responseCode == 1) {
                    result.gkResponse = VerifyCredentialResponse.fromTimeout(response.getTimeout());
                    return result;
                } else {
                    result.gkResponse = VerifyCredentialResponse.ERROR;
                    return result;
                }
            } catch (RemoteException e3) {
                Slog.e(TAG, "gatekeeper verify failed", e3);
                result.gkResponse = VerifyCredentialResponse.ERROR;
                return result;
            }
        }
        if (progressCallback != null) {
            try {
                progressCallback.onCredentialVerified();
            } catch (RemoteException e4) {
                Slog.w(TAG, "progressCallback throws exception", e4);
            }
        }
        result.authToken = unwrapSyntheticPasswordBlob(handle, (byte) 0, applicationId, sid, userId);
        result.gkResponse = verifyChallenge(gatekeeper, result.authToken, 0L, userId);
        if (result.authToken != null && !hasPasswordMetrics(handle, userId)) {
            savePasswordMetrics(credential, result.authToken, handle, userId);
        }
        return result;
    }

    public AuthenticationResult unwrapTokenBasedSyntheticPassword(IGateKeeperService gatekeeper, long handle, byte[] token, int userId) {
        SyntheticPasswordBlob blob = SyntheticPasswordBlob.fromBytes(loadState(SP_BLOB_NAME, handle, userId));
        return unwrapTokenBasedSyntheticPasswordInternal(gatekeeper, handle, blob.mType, token, userId);
    }

    public AuthenticationResult unwrapStrongTokenBasedSyntheticPassword(IGateKeeperService gatekeeper, long handle, byte[] token, int userId) {
        return unwrapTokenBasedSyntheticPasswordInternal(gatekeeper, handle, (byte) 1, token, userId);
    }

    public AuthenticationResult unwrapWeakTokenBasedSyntheticPassword(IGateKeeperService gatekeeper, long handle, byte[] token, int userId) {
        return unwrapTokenBasedSyntheticPasswordInternal(gatekeeper, handle, (byte) 2, token, userId);
    }

    private AuthenticationResult unwrapTokenBasedSyntheticPasswordInternal(IGateKeeperService gatekeeper, long handle, byte type, byte[] token, int userId) {
        byte[] secdiscardable;
        AuthenticationResult result = new AuthenticationResult();
        byte[] secdiscardable2 = loadSecdiscardable(handle, userId);
        int slotId = loadWeaverSlot(handle, userId);
        if (slotId == -1) {
            secdiscardable = secdiscardable2;
        } else if (isWeaverAvailable()) {
            VerifyCredentialResponse response = weaverVerify(slotId, null);
            if (response.getResponseCode() != 0 || response.getGatekeeperHAT() == null) {
                Slog.e(TAG, "Failed to retrieve weaver secret when unwrapping token");
                result.gkResponse = VerifyCredentialResponse.ERROR;
                return result;
            }
            secdiscardable = SyntheticPasswordCrypto.decrypt(response.getGatekeeperHAT(), PERSONALISATION_WEAVER_TOKEN, secdiscardable2);
        } else {
            Slog.e(TAG, "No weaver service to unwrap token based SP");
            result.gkResponse = VerifyCredentialResponse.ERROR;
            return result;
        }
        byte[] applicationId = transformUnderSecdiscardable(token, secdiscardable);
        result.authToken = unwrapSyntheticPasswordBlob(handle, type, applicationId, 0L, userId);
        if (result.authToken != null) {
            result.gkResponse = verifyChallenge(gatekeeper, result.authToken, 0L, userId);
            if (result.gkResponse == null) {
                result.gkResponse = VerifyCredentialResponse.OK;
            }
        } else {
            result.gkResponse = VerifyCredentialResponse.ERROR;
        }
        return result;
    }

    private AuthenticationToken unwrapSyntheticPasswordBlob(long handle, byte type, byte[] applicationId, long sid, int userId) {
        byte[] secret;
        byte[] data = loadState(SP_BLOB_NAME, handle, userId);
        if (data == null) {
            return null;
        }
        SyntheticPasswordBlob blob = SyntheticPasswordBlob.fromBytes(data);
        if (blob.mVersion != 3 && blob.mVersion != 2 && blob.mVersion != 1) {
            throw new IllegalArgumentException("Unknown blob version");
        }
        if (blob.mType != type) {
            throw new IllegalArgumentException("Invalid blob type");
        }
        if (blob.mVersion != 1) {
            secret = decryptSPBlob(getKeyName(handle), blob.mContent, applicationId);
        } else {
            secret = SyntheticPasswordCrypto.decryptBlobV1(getKeyName(handle), blob.mContent, applicationId);
        }
        if (secret == null) {
            Slog.e(TAG, "Fail to decrypt SP for user " + userId);
            return null;
        }
        AuthenticationToken result = new AuthenticationToken(blob.mVersion);
        if (type == 1 || type == 2) {
            if (!loadEscrowData(result, userId)) {
                Slog.e(TAG, "User is not escrowable: " + userId);
                return null;
            }
            result.recreateFromEscrow(secret);
        } else {
            result.recreateDirectly(secret);
        }
        if (blob.mVersion == 1) {
            Slog.i(TAG, "Upgrade v1 SP blob for user " + userId + ", type = " + ((int) type));
            createSyntheticPasswordBlob(handle, type, result, applicationId, sid, userId);
            return result;
        }
        return result;
    }

    public VerifyCredentialResponse verifyChallenge(IGateKeeperService gatekeeper, AuthenticationToken auth, long challenge, int userId) {
        return verifyChallengeInternal(gatekeeper, auth.deriveGkPassword(), challenge, userId);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public VerifyCredentialResponse verifyChallengeInternal(IGateKeeperService gatekeeper, byte[] gatekeeperPassword, long challenge, int userId) {
        GateKeeperResponse response;
        byte[] spHandle = loadSyntheticPasswordHandle(userId);
        if (spHandle == null) {
            return null;
        }
        try {
            GateKeeperResponse response2 = gatekeeper.verifyChallenge(userId, challenge, spHandle, gatekeeperPassword);
            int responseCode = response2.getResponseCode();
            if (responseCode == 0) {
                VerifyCredentialResponse result = new VerifyCredentialResponse.Builder().setGatekeeperHAT(response2.getPayload()).build();
                if (response2.getShouldReEnroll()) {
                    try {
                        response = gatekeeper.enroll(userId, spHandle, spHandle, gatekeeperPassword);
                    } catch (RemoteException e) {
                        Slog.e(TAG, "Failed to invoke gatekeeper.enroll", e);
                        response = GateKeeperResponse.ERROR;
                    }
                    if (response.getResponseCode() != 0) {
                        Slog.w(TAG, "Fail to re-enroll SP handle for user " + userId);
                    } else {
                        saveSyntheticPasswordHandle(response.getPayload(), userId);
                        return verifyChallengeInternal(gatekeeper, gatekeeperPassword, challenge, userId);
                    }
                }
                return result;
            } else if (responseCode == 1) {
                return VerifyCredentialResponse.fromTimeout(response2.getTimeout());
            } else {
                return VerifyCredentialResponse.ERROR;
            }
        } catch (RemoteException e2) {
            Slog.e(TAG, "Fail to verify with gatekeeper " + userId, e2);
            return VerifyCredentialResponse.ERROR;
        }
    }

    public boolean existsHandle(long handle, int userId) {
        return hasState(SP_BLOB_NAME, handle, userId);
    }

    public void destroyTokenBasedSyntheticPassword(long handle, int userId) {
        SyntheticPasswordBlob blob = SyntheticPasswordBlob.fromBytes(loadState(SP_BLOB_NAME, handle, userId));
        destroySyntheticPassword(handle, userId);
        destroyState(SECDISCARDABLE_NAME, handle, userId);
        if (blob.mType == 2) {
            notifyWeakEscrowTokenRemovedListeners(handle, userId);
        }
    }

    public void destroyAllWeakTokenBasedSyntheticPasswords(int userId) {
        List<Long> handles = this.mStorage.listSyntheticPasswordHandlesForUser(SECDISCARDABLE_NAME, userId);
        for (Long l : handles) {
            long handle = l.longValue();
            SyntheticPasswordBlob blob = SyntheticPasswordBlob.fromBytes(loadState(SP_BLOB_NAME, handle, userId));
            if (blob.mType == 2) {
                destroyTokenBasedSyntheticPassword(handle, userId);
            }
        }
    }

    public void destroyPasswordBasedSyntheticPassword(long handle, int userId) {
        destroySyntheticPassword(handle, userId);
        destroyState(SECDISCARDABLE_NAME, handle, userId);
        destroyState(PASSWORD_DATA_NAME, handle, userId);
        destroyState(PASSWORD_METRICS_NAME, handle, userId);
    }

    private void destroySyntheticPassword(long handle, int userId) {
        destroyState(SP_BLOB_NAME, handle, userId);
        destroySPBlobKey(getKeyName(handle));
        if (hasState(WEAVER_SLOT_NAME, handle, userId)) {
            destroyWeaverSlot(handle, userId);
        }
    }

    private byte[] transformUnderWeaverSecret(byte[] data, byte[] secret) {
        byte[] weaverSecret = SyntheticPasswordCrypto.personalisedHash(PERSONALISATION_WEAVER_PASSWORD, secret);
        byte[] result = new byte[data.length + weaverSecret.length];
        System.arraycopy(data, 0, result, 0, data.length);
        System.arraycopy(weaverSecret, 0, result, data.length, weaverSecret.length);
        return result;
    }

    private byte[] transformUnderSecdiscardable(byte[] data, byte[] rawSecdiscardable) {
        byte[] secdiscardable = SyntheticPasswordCrypto.personalisedHash(PERSONALISATION_SECDISCARDABLE, rawSecdiscardable);
        byte[] result = new byte[data.length + secdiscardable.length];
        System.arraycopy(data, 0, result, 0, data.length);
        System.arraycopy(secdiscardable, 0, result, data.length, secdiscardable.length);
        return result;
    }

    private byte[] createSecdiscardable(long handle, int userId) {
        byte[] data = secureRandom(16384);
        saveSecdiscardable(handle, data, userId);
        return data;
    }

    private void saveSecdiscardable(long handle, byte[] secdiscardable, int userId) {
        saveState(SECDISCARDABLE_NAME, secdiscardable, handle, userId);
    }

    private byte[] loadSecdiscardable(long handle, int userId) {
        return loadState(SECDISCARDABLE_NAME, handle, userId);
    }

    private byte getTokenBasedBlobType(int type) {
        switch (type) {
            case 1:
                return (byte) 2;
            default:
                return (byte) 1;
        }
    }

    public PasswordMetrics getPasswordMetrics(AuthenticationToken authToken, long handle, int userId) {
        byte[] decrypted;
        byte[] encrypted = loadState(PASSWORD_METRICS_NAME, handle, userId);
        if (encrypted == null || (decrypted = SyntheticPasswordCrypto.decrypt(authToken.deriveMetricsKey(), new byte[0], encrypted)) == null) {
            return null;
        }
        return VersionedPasswordMetrics.deserialize(decrypted).getMetrics();
    }

    private void savePasswordMetrics(LockscreenCredential credential, AuthenticationToken authToken, long handle, int userId) {
        byte[] encrypted = SyntheticPasswordCrypto.encrypt(authToken.deriveMetricsKey(), new byte[0], new VersionedPasswordMetrics(credential).serialize());
        saveState(PASSWORD_METRICS_NAME, encrypted, handle, userId);
    }

    private boolean hasPasswordMetrics(long handle, int userId) {
        return hasState(PASSWORD_METRICS_NAME, handle, userId);
    }

    private boolean hasState(String stateName, long handle, int userId) {
        return !ArrayUtils.isEmpty(loadState(stateName, handle, userId));
    }

    private byte[] loadState(String stateName, long handle, int userId) {
        return this.mStorage.readSyntheticPasswordState(userId, handle, stateName);
    }

    private void saveState(String stateName, byte[] data, long handle, int userId) {
        this.mStorage.writeSyntheticPasswordState(userId, handle, stateName, data);
    }

    private void destroyState(String stateName, long handle, int userId) {
        this.mStorage.deleteSyntheticPasswordState(userId, handle, stateName);
    }

    protected byte[] decryptSPBlob(String blobKeyName, byte[] blob, byte[] applicationId) {
        return SyntheticPasswordCrypto.decryptBlob(blobKeyName, blob, applicationId);
    }

    protected byte[] createSPBlob(String blobKeyName, byte[] data, byte[] applicationId, long sid) {
        return SyntheticPasswordCrypto.createBlob(blobKeyName, data, applicationId, sid);
    }

    protected void destroySPBlobKey(String keyAlias) {
        SyntheticPasswordCrypto.destroyBlobKey(keyAlias);
    }

    public static long generateHandle() {
        long result;
        SecureRandom rng = new SecureRandom();
        do {
            result = rng.nextLong();
        } while (result == 0);
        return result;
    }

    private int fakeUid(int uid) {
        return 100000 + uid;
    }

    protected static byte[] secureRandom(int length) {
        try {
            return SecureRandom.getInstance("SHA1PRNG").generateSeed(length);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getKeyName(long handle) {
        return String.format("%s%x", "synthetic_password_", Long.valueOf(handle));
    }

    private byte[] computePasswordToken(LockscreenCredential credential, PasswordData data) {
        byte[] password = credential.isNone() ? DEFAULT_PASSWORD : credential.getCredential();
        return scrypt(password, data.salt, 1 << data.scryptN, 1 << data.scryptR, 1 << data.scryptP, 32);
    }

    private byte[] passwordTokenToGkInput(byte[] token) {
        return SyntheticPasswordCrypto.personalisedHash(PERSONALIZATION_USER_GK_AUTH, token);
    }

    private byte[] passwordTokenToWeaverKey(byte[] token) {
        byte[] key = SyntheticPasswordCrypto.personalisedHash(PERSONALISATION_WEAVER_KEY, token);
        if (key.length < this.mWeaverConfig.keySize) {
            throw new IllegalArgumentException("weaver key length too small");
        }
        return Arrays.copyOf(key, this.mWeaverConfig.keySize);
    }

    protected long sidFromPasswordHandle(byte[] handle) {
        return nativeSidFromPasswordHandle(handle);
    }

    protected byte[] scrypt(byte[] password, byte[] salt, int n, int r, int p, int outLen) {
        return new Scrypt().scrypt(password, salt, n, r, p, outLen);
    }

    protected static ArrayList<Byte> toByteArrayList(byte[] data) {
        ArrayList<Byte> result = new ArrayList<>(data.length);
        for (byte b : data) {
            result.add(Byte.valueOf(b));
        }
        return result;
    }

    protected static byte[] fromByteArrayList(ArrayList<Byte> data) {
        byte[] result = new byte[data.size()];
        for (int i = 0; i < data.size(); i++) {
            result[i] = data.get(i).byteValue();
        }
        return result;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static byte[] bytesToHex(byte[] bytes) {
        if (bytes == null) {
            return "null".getBytes();
        }
        byte[] hexBytes = new byte[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 255;
            byte[] bArr = HEX_ARRAY;
            hexBytes[j * 2] = bArr[v >>> 4];
            hexBytes[(j * 2) + 1] = bArr[v & 15];
        }
        return hexBytes;
    }

    public boolean migrateKeyNamespace() {
        boolean success = true;
        Map<Integer, List<Long>> allHandles = this.mStorage.listSyntheticPasswordHandlesForAllUsers(SP_BLOB_NAME);
        for (List<Long> userHandles : allHandles.values()) {
            for (Long l : userHandles) {
                long handle = l.longValue();
                success &= SyntheticPasswordCrypto.migrateLockSettingsKey(getKeyName(handle));
            }
        }
        return success;
    }

    public boolean registerWeakEscrowTokenRemovedListener(IWeakEscrowTokenRemovedListener listener) {
        return this.mListeners.register(listener);
    }

    public boolean unregisterWeakEscrowTokenRemovedListener(IWeakEscrowTokenRemovedListener listener) {
        return this.mListeners.unregister(listener);
    }

    private void notifyWeakEscrowTokenRemovedListeners(long handle, int userId) {
        int i = this.mListeners.beginBroadcast();
        while (i > 0) {
            i--;
            try {
                try {
                    this.mListeners.getBroadcastItem(i).onWeakEscrowTokenRemoved(handle, userId);
                } catch (RemoteException e) {
                    Slog.e(TAG, "Exception while notifying WeakEscrowTokenRemovedListener.", e);
                }
            } finally {
                this.mListeners.finishBroadcast();
            }
        }
    }
}
