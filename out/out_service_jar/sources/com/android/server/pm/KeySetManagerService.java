package com.android.server.pm;

import android.content.pm.parsing.FrameworkParsingPackageUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Base64;
import android.util.LongSparseArray;
import android.util.Slog;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.pm.pkg.PackageStateInternal;
import com.android.server.pm.pkg.SharedUserApi;
import com.android.server.utils.WatchedArrayMap;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.PublicKey;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes2.dex */
public class KeySetManagerService {
    public static final int CURRENT_VERSION = 1;
    public static final int FIRST_VERSION = 1;
    public static final long KEYSET_NOT_FOUND = -1;
    protected static final long PUBLIC_KEY_NOT_FOUND = -1;
    static final String TAG = "KeySetManagerService";
    private long lastIssuedKeyId;
    private long lastIssuedKeySetId;
    protected final LongSparseArray<ArraySet<Long>> mKeySetMapping;
    private final LongSparseArray<KeySetHandle> mKeySets;
    private final WatchedArrayMap<String, PackageSetting> mPackages;
    private final LongSparseArray<PublicKeyHandle> mPublicKeys;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public class PublicKeyHandle {
        private final long mId;
        private final PublicKey mKey;
        private int mRefCount;

        public PublicKeyHandle(long id, PublicKey key) {
            this.mId = id;
            this.mRefCount = 1;
            this.mKey = key;
        }

        private PublicKeyHandle(long id, int refCount, PublicKey key) {
            this.mId = id;
            this.mRefCount = refCount;
            this.mKey = key;
        }

        public long getId() {
            return this.mId;
        }

        public PublicKey getKey() {
            return this.mKey;
        }

        public int getRefCountLPr() {
            return this.mRefCount;
        }

        public void incrRefCountLPw() {
            this.mRefCount++;
        }

        public long decrRefCountLPw() {
            int i = this.mRefCount - 1;
            this.mRefCount = i;
            return i;
        }
    }

    public KeySetManagerService(WatchedArrayMap<String, PackageSetting> packages) {
        this.lastIssuedKeySetId = 0L;
        this.lastIssuedKeyId = 0L;
        this.mKeySets = new LongSparseArray<>();
        this.mPublicKeys = new LongSparseArray<>();
        this.mKeySetMapping = new LongSparseArray<>();
        this.mPackages = packages;
    }

    public KeySetManagerService(KeySetManagerService other, WatchedArrayMap<String, PackageSetting> packages) {
        this.lastIssuedKeySetId = 0L;
        this.lastIssuedKeyId = 0L;
        this.mKeySets = other.mKeySets.clone();
        this.mPublicKeys = other.mPublicKeys.clone();
        this.mKeySetMapping = other.mKeySetMapping.clone();
        this.mPackages = packages;
    }

    public boolean packageIsSignedByLPr(String packageName, KeySetHandle ks) {
        PackageSetting pkg = this.mPackages.get(packageName);
        if (pkg == null) {
            throw new NullPointerException("Invalid package name");
        }
        if (pkg.getKeySetData() == null) {
            throw new NullPointerException("Package has no KeySet data");
        }
        long id = getIdByKeySetLPr(ks);
        if (id == -1) {
            return false;
        }
        ArraySet<Long> pkgKeys = this.mKeySetMapping.get(pkg.getKeySetData().getProperSigningKeySet());
        ArraySet<Long> testKeys = this.mKeySetMapping.get(id);
        return pkgKeys.containsAll(testKeys);
    }

    public boolean packageIsSignedByExactlyLPr(String packageName, KeySetHandle ks) {
        PackageSetting pkg = this.mPackages.get(packageName);
        if (pkg == null) {
            throw new NullPointerException("Invalid package name");
        }
        if (pkg.getKeySetData() == null || pkg.getKeySetData().getProperSigningKeySet() == -1) {
            throw new NullPointerException("Package has no KeySet data");
        }
        long id = getIdByKeySetLPr(ks);
        if (id == -1) {
            return false;
        }
        ArraySet<Long> pkgKeys = this.mKeySetMapping.get(pkg.getKeySetData().getProperSigningKeySet());
        ArraySet<Long> testKeys = this.mKeySetMapping.get(id);
        return pkgKeys.equals(testKeys);
    }

    /* JADX WARN: Removed duplicated region for block: B:21:0x0040  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void assertScannedPackageValid(AndroidPackage pkg) throws PackageManagerException {
        if (pkg == null || pkg.getPackageName() == null) {
            throw new PackageManagerException(-2, "Passed invalid package to keyset validation.");
        }
        ArraySet<PublicKey> signingKeys = pkg.getSigningDetails().getPublicKeys();
        if (signingKeys == null || signingKeys.size() <= 0 || signingKeys.contains(null)) {
            throw new PackageManagerException(-2, "Package has invalid signing-key-set.");
        }
        Map<String, ArraySet<PublicKey>> definedMapping = pkg.getKeySetMapping();
        if (definedMapping != null) {
            if (definedMapping.containsKey(null) || definedMapping.containsValue(null)) {
                throw new PackageManagerException(-2, "Package has null defined key set.");
            }
            for (ArraySet<PublicKey> value : definedMapping.values()) {
                if (value.size() <= 0 || value.contains(null)) {
                    throw new PackageManagerException(-2, "Package has null/no public keys for defined key-sets.");
                }
                while (r4.hasNext()) {
                }
            }
        }
        Set<String> upgradeAliases = pkg.getUpgradeKeySets();
        if (upgradeAliases != null) {
            if (definedMapping == null || !definedMapping.keySet().containsAll(upgradeAliases)) {
                throw new PackageManagerException(-2, "Package has upgrade-key-sets without corresponding definitions.");
            }
        }
    }

    public void addScannedPackageLPw(AndroidPackage pkg) {
        Objects.requireNonNull(pkg, "Attempted to add null pkg to ksms.");
        Objects.requireNonNull(pkg.getPackageName(), "Attempted to add null pkg to ksms.");
        PackageSetting ps = this.mPackages.get(pkg.getPackageName());
        Objects.requireNonNull(ps, "pkg: " + pkg.getPackageName() + "does not have a corresponding entry in mPackages.");
        addSigningKeySetToPackageLPw(ps, pkg.getSigningDetails().getPublicKeys());
        if (pkg.getKeySetMapping() != null) {
            addDefinedKeySetsToPackageLPw(ps, pkg.getKeySetMapping());
            if (pkg.getUpgradeKeySets() != null) {
                addUpgradeKeySetsToPackageLPw(ps, pkg.getUpgradeKeySets());
            }
        }
    }

    void addSigningKeySetToPackageLPw(PackageSetting pkg, ArraySet<PublicKey> signingKeys) {
        long signingKeySetId = pkg.getKeySetData().getProperSigningKeySet();
        if (signingKeySetId != -1) {
            ArraySet<PublicKey> existingKeys = getPublicKeysFromKeySetLPr(signingKeySetId);
            if (existingKeys != null && existingKeys.equals(signingKeys)) {
                return;
            }
            decrementKeySetLPw(signingKeySetId);
        }
        KeySetHandle ks = addKeySetLPw(signingKeys);
        long id = ks.getId();
        pkg.getKeySetData().setProperSigningKeySet(id);
    }

    private long getIdByKeySetLPr(KeySetHandle ks) {
        for (int keySetIndex = 0; keySetIndex < this.mKeySets.size(); keySetIndex++) {
            KeySetHandle value = this.mKeySets.valueAt(keySetIndex);
            if (ks.equals(value)) {
                return this.mKeySets.keyAt(keySetIndex);
            }
        }
        return -1L;
    }

    void addDefinedKeySetsToPackageLPw(PackageSetting pkg, Map<String, ArraySet<PublicKey>> definedMapping) {
        ArrayMap<String, Long> prevDefinedKeySets = pkg.getKeySetData().getAliases();
        Map<String, Long> newKeySetAliases = new ArrayMap<>();
        for (Map.Entry<String, ArraySet<PublicKey>> entry : definedMapping.entrySet()) {
            String alias = entry.getKey();
            ArraySet<PublicKey> pubKeys = entry.getValue();
            if (alias != null && pubKeys != null && pubKeys.size() > 0) {
                KeySetHandle ks = addKeySetLPw(pubKeys);
                newKeySetAliases.put(alias, Long.valueOf(ks.getId()));
            }
        }
        int prevDefSize = prevDefinedKeySets.size();
        for (int i = 0; i < prevDefSize; i++) {
            decrementKeySetLPw(prevDefinedKeySets.valueAt(i).longValue());
        }
        pkg.getKeySetData().removeAllUpgradeKeySets();
        pkg.getKeySetData().setAliases(newKeySetAliases);
    }

    void addUpgradeKeySetsToPackageLPw(PackageSetting pkg, Set<String> upgradeAliases) {
        for (String upgradeAlias : upgradeAliases) {
            pkg.getKeySetData().addUpgradeKeySet(upgradeAlias);
        }
    }

    public KeySetHandle getKeySetByAliasAndPackageNameLPr(String packageName, String alias) {
        PackageSetting p = this.mPackages.get(packageName);
        if (p == null || p.getKeySetData() == null) {
            return null;
        }
        ArrayMap<String, Long> aliases = p.getKeySetData().getAliases();
        Long keySetId = aliases.get(alias);
        if (keySetId == null) {
            throw new IllegalArgumentException("Unknown KeySet alias: " + alias + ", aliases = " + aliases);
        }
        return this.mKeySets.get(keySetId.longValue());
    }

    public boolean isIdValidKeySetId(long id) {
        return this.mKeySets.get(id) != null;
    }

    public boolean shouldCheckUpgradeKeySetLocked(PackageStateInternal oldPs, SharedUserApi sharedUserSetting, int scanFlags) {
        if (oldPs == null || (scanFlags & 512) != 0 || sharedUserSetting != null || !oldPs.getKeySetData().isUsingUpgradeKeySets()) {
            return false;
        }
        long[] upgradeKeySets = oldPs.getKeySetData().getUpgradeKeySets();
        for (int i = 0; i < upgradeKeySets.length; i++) {
            if (!isIdValidKeySetId(upgradeKeySets[i])) {
                Slog.wtf(TAG, "Package " + (oldPs.getPackageName() != null ? oldPs.getPackageName() : "<null>") + " contains upgrade-key-set reference to unknown key-set: " + upgradeKeySets[i] + " reverting to signatures check.");
                return false;
            }
        }
        return true;
    }

    public boolean checkUpgradeKeySetLocked(PackageStateInternal oldPS, AndroidPackage pkg) {
        long[] upgradeKeySets = oldPS.getKeySetData().getUpgradeKeySets();
        for (long j : upgradeKeySets) {
            Set<PublicKey> upgradeSet = getPublicKeysFromKeySetLPr(j);
            if (upgradeSet != null && pkg.getSigningDetails().getPublicKeys().containsAll(upgradeSet)) {
                return true;
            }
        }
        return false;
    }

    public ArraySet<PublicKey> getPublicKeysFromKeySetLPr(long id) {
        ArraySet<Long> pkIds = this.mKeySetMapping.get(id);
        if (pkIds == null) {
            return null;
        }
        ArraySet<PublicKey> mPubKeys = new ArraySet<>();
        int pkSize = pkIds.size();
        for (int i = 0; i < pkSize; i++) {
            mPubKeys.add(this.mPublicKeys.get(pkIds.valueAt(i).longValue()).getKey());
        }
        return mPubKeys;
    }

    public KeySetHandle getSigningKeySetByPackageNameLPr(String packageName) {
        PackageSetting p = this.mPackages.get(packageName);
        if (p == null || p.getKeySetData() == null || p.getKeySetData().getProperSigningKeySet() == -1) {
            return null;
        }
        return this.mKeySets.get(p.getKeySetData().getProperSigningKeySet());
    }

    private KeySetHandle addKeySetLPw(ArraySet<PublicKey> keys) {
        if (keys == null || keys.size() == 0) {
            throw new IllegalArgumentException("Cannot add an empty set of keys!");
        }
        ArraySet<Long> addedKeyIds = new ArraySet<>(keys.size());
        int kSize = keys.size();
        for (int i = 0; i < kSize; i++) {
            addedKeyIds.add(Long.valueOf(addPublicKeyLPw(keys.valueAt(i))));
        }
        long existingKeySetId = getIdFromKeyIdsLPr(addedKeyIds);
        if (existingKeySetId != -1) {
            for (int i2 = 0; i2 < kSize; i2++) {
                decrementPublicKeyLPw(addedKeyIds.valueAt(i2).longValue());
            }
            KeySetHandle ks = this.mKeySets.get(existingKeySetId);
            ks.incrRefCountLPw();
            return ks;
        }
        long id = getFreeKeySetIDLPw();
        KeySetHandle ks2 = new KeySetHandle(id);
        this.mKeySets.put(id, ks2);
        this.mKeySetMapping.put(id, addedKeyIds);
        return ks2;
    }

    private void decrementKeySetLPw(long id) {
        KeySetHandle ks = this.mKeySets.get(id);
        if (ks != null && ks.decrRefCountLPw() <= 0) {
            ArraySet<Long> pubKeys = this.mKeySetMapping.get(id);
            int pkSize = pubKeys.size();
            for (int i = 0; i < pkSize; i++) {
                decrementPublicKeyLPw(pubKeys.valueAt(i).longValue());
            }
            this.mKeySets.delete(id);
            this.mKeySetMapping.delete(id);
        }
    }

    private void decrementPublicKeyLPw(long id) {
        PublicKeyHandle pk = this.mPublicKeys.get(id);
        if (pk != null && pk.decrRefCountLPw() <= 0) {
            this.mPublicKeys.delete(id);
        }
    }

    private long addPublicKeyLPw(PublicKey key) {
        Objects.requireNonNull(key, "Cannot add null public key!");
        long id = getIdForPublicKeyLPr(key);
        if (id != -1) {
            this.mPublicKeys.get(id).incrRefCountLPw();
            return id;
        }
        long id2 = getFreePublicKeyIdLPw();
        this.mPublicKeys.put(id2, new PublicKeyHandle(id2, key));
        return id2;
    }

    private long getIdFromKeyIdsLPr(Set<Long> publicKeyIds) {
        for (int keyMapIndex = 0; keyMapIndex < this.mKeySetMapping.size(); keyMapIndex++) {
            ArraySet<Long> value = this.mKeySetMapping.valueAt(keyMapIndex);
            if (value.equals(publicKeyIds)) {
                return this.mKeySetMapping.keyAt(keyMapIndex);
            }
        }
        return -1L;
    }

    private long getIdForPublicKeyLPr(PublicKey k) {
        String encodedPublicKey = new String(k.getEncoded());
        for (int publicKeyIndex = 0; publicKeyIndex < this.mPublicKeys.size(); publicKeyIndex++) {
            PublicKey value = this.mPublicKeys.valueAt(publicKeyIndex).getKey();
            String encodedExistingKey = new String(value.getEncoded());
            if (encodedPublicKey.equals(encodedExistingKey)) {
                return this.mPublicKeys.keyAt(publicKeyIndex);
            }
        }
        return -1L;
    }

    private long getFreeKeySetIDLPw() {
        long j = this.lastIssuedKeySetId + 1;
        this.lastIssuedKeySetId = j;
        return j;
    }

    private long getFreePublicKeyIdLPw() {
        long j = this.lastIssuedKeyId + 1;
        this.lastIssuedKeyId = j;
        return j;
    }

    public void removeAppKeySetDataLPw(String packageName) {
        PackageSetting pkg = this.mPackages.get(packageName);
        Objects.requireNonNull(pkg, "pkg name: " + packageName + "does not have a corresponding entry in mPackages.");
        long signingKeySetId = pkg.getKeySetData().getProperSigningKeySet();
        decrementKeySetLPw(signingKeySetId);
        ArrayMap<String, Long> definedKeySets = pkg.getKeySetData().getAliases();
        for (int i = 0; i < definedKeySets.size(); i++) {
            decrementKeySetLPw(definedKeySets.valueAt(i).longValue());
        }
        clearPackageKeySetDataLPw(pkg);
    }

    private void clearPackageKeySetDataLPw(PackageSetting pkg) {
        pkg.getKeySetData().setProperSigningKeySet(-1L);
        pkg.getKeySetData().removeAllDefinedKeySets();
        pkg.getKeySetData().removeAllUpgradeKeySets();
    }

    @Deprecated
    public String encodePublicKey(PublicKey k) throws IOException {
        return new String(Base64.encode(k.getEncoded(), 2));
    }

    public void dumpLPr(PrintWriter pw, String packageName, DumpState dumpState) {
        long[] upgradeKeySets;
        String str = packageName;
        boolean printedHeader = false;
        for (Map.Entry<String, PackageSetting> e : this.mPackages.entrySet()) {
            String keySetPackage = e.getKey();
            if (str == null || str.equals(keySetPackage)) {
                if (!printedHeader) {
                    if (dumpState.onTitlePrinted()) {
                        pw.println();
                    }
                    pw.println("Key Set Manager:");
                    printedHeader = true;
                }
                PackageSetting pkg = e.getValue();
                pw.print("  [");
                pw.print(keySetPackage);
                pw.println("]");
                if (pkg.getKeySetData() != null) {
                    boolean printedLabel = false;
                    for (Map.Entry<String, Long> entry : pkg.getKeySetData().getAliases().entrySet()) {
                        if (!printedLabel) {
                            pw.print("      KeySets Aliases: ");
                            printedLabel = true;
                        } else {
                            pw.print(", ");
                        }
                        pw.print(entry.getKey());
                        pw.print('=');
                        pw.print(Long.toString(entry.getValue().longValue()));
                    }
                    if (printedLabel) {
                        pw.println("");
                    }
                    boolean printedLabel2 = false;
                    if (pkg.getKeySetData().isUsingDefinedKeySets()) {
                        ArrayMap<String, Long> definedKeySets = pkg.getKeySetData().getAliases();
                        int dksSize = definedKeySets.size();
                        for (int i = 0; i < dksSize; i++) {
                            if (!printedLabel2) {
                                pw.print("      Defined KeySets: ");
                                printedLabel2 = true;
                            } else {
                                pw.print(", ");
                            }
                            pw.print(Long.toString(definedKeySets.valueAt(i).longValue()));
                        }
                    }
                    if (printedLabel2) {
                        pw.println("");
                    }
                    boolean printedLabel3 = false;
                    long signingKeySet = pkg.getKeySetData().getProperSigningKeySet();
                    pw.print("      Signing KeySets: ");
                    pw.print(Long.toString(signingKeySet));
                    pw.println("");
                    if (pkg.getKeySetData().isUsingUpgradeKeySets()) {
                        for (long keySetId : pkg.getKeySetData().getUpgradeKeySets()) {
                            if (!printedLabel3) {
                                pw.print("      Upgrade KeySets: ");
                                printedLabel3 = true;
                            } else {
                                pw.print(", ");
                            }
                            pw.print(Long.toString(keySetId));
                        }
                    }
                    if (printedLabel3) {
                        pw.println("");
                    }
                }
                str = packageName;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void writeKeySetManagerServiceLPr(TypedXmlSerializer serializer) throws IOException {
        serializer.startTag((String) null, "keyset-settings");
        serializer.attributeInt((String) null, "version", 1);
        writePublicKeysLPr(serializer);
        writeKeySetsLPr(serializer);
        serializer.startTag((String) null, "lastIssuedKeyId");
        serializer.attributeLong((String) null, "value", this.lastIssuedKeyId);
        serializer.endTag((String) null, "lastIssuedKeyId");
        serializer.startTag((String) null, "lastIssuedKeySetId");
        serializer.attributeLong((String) null, "value", this.lastIssuedKeySetId);
        serializer.endTag((String) null, "lastIssuedKeySetId");
        serializer.endTag((String) null, "keyset-settings");
    }

    void writePublicKeysLPr(TypedXmlSerializer serializer) throws IOException {
        serializer.startTag((String) null, "keys");
        for (int pKeyIndex = 0; pKeyIndex < this.mPublicKeys.size(); pKeyIndex++) {
            long id = this.mPublicKeys.keyAt(pKeyIndex);
            PublicKeyHandle pkh = this.mPublicKeys.valueAt(pKeyIndex);
            serializer.startTag((String) null, "public-key");
            serializer.attributeLong((String) null, "identifier", id);
            serializer.attributeBytesBase64((String) null, "value", pkh.getKey().getEncoded());
            serializer.endTag((String) null, "public-key");
        }
        serializer.endTag((String) null, "keys");
    }

    void writeKeySetsLPr(TypedXmlSerializer serializer) throws IOException {
        serializer.startTag((String) null, "keysets");
        for (int keySetIndex = 0; keySetIndex < this.mKeySetMapping.size(); keySetIndex++) {
            long id = this.mKeySetMapping.keyAt(keySetIndex);
            ArraySet<Long> keys = this.mKeySetMapping.valueAt(keySetIndex);
            serializer.startTag((String) null, "keyset");
            serializer.attributeLong((String) null, "identifier", id);
            Iterator<Long> it = keys.iterator();
            while (it.hasNext()) {
                long keyId = it.next().longValue();
                serializer.startTag((String) null, "key-id");
                serializer.attributeLong((String) null, "identifier", keyId);
                serializer.endTag((String) null, "key-id");
            }
            serializer.endTag((String) null, "keyset");
        }
        serializer.endTag((String) null, "keysets");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void readKeySetsLPw(TypedXmlPullParser parser, ArrayMap<Long, Integer> keySetRefCounts) throws XmlPullParserException, IOException {
        int outerDepth = parser.getDepth();
        String recordedVersionStr = parser.getAttributeValue((String) null, "version");
        if (recordedVersionStr == null) {
            while (true) {
                int type = parser.next();
                if (type == 1 || (type == 3 && parser.getDepth() <= outerDepth)) {
                    break;
                }
            }
            for (PackageSetting p : this.mPackages.values()) {
                clearPackageKeySetDataLPw(p);
            }
            return;
        }
        while (true) {
            int type2 = parser.next();
            if (type2 == 1 || (type2 == 3 && parser.getDepth() <= outerDepth)) {
                break;
            } else if (type2 != 3 && type2 != 4) {
                String tagName = parser.getName();
                if (tagName.equals("keys")) {
                    readKeysLPw(parser);
                } else if (tagName.equals("keysets")) {
                    readKeySetListLPw(parser);
                } else if (tagName.equals("lastIssuedKeyId")) {
                    this.lastIssuedKeyId = parser.getAttributeLong((String) null, "value");
                } else if (tagName.equals("lastIssuedKeySetId")) {
                    this.lastIssuedKeySetId = parser.getAttributeLong((String) null, "value");
                }
            }
        }
        addRefCountsFromSavedPackagesLPw(keySetRefCounts);
    }

    void readKeysLPw(TypedXmlPullParser parser) throws XmlPullParserException, IOException {
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type != 1) {
                if (type != 3 || parser.getDepth() > outerDepth) {
                    if (type != 3 && type != 4) {
                        String tagName = parser.getName();
                        if (tagName.equals("public-key")) {
                            readPublicKeyLPw(parser);
                        }
                    }
                } else {
                    return;
                }
            } else {
                return;
            }
        }
    }

    void readKeySetListLPw(TypedXmlPullParser parser) throws XmlPullParserException, IOException {
        int outerDepth = parser.getDepth();
        long currentKeySetId = 0;
        while (true) {
            int type = parser.next();
            if (type != 1) {
                if (type != 3 || parser.getDepth() > outerDepth) {
                    if (type != 3 && type != 4) {
                        String tagName = parser.getName();
                        if (tagName.equals("keyset")) {
                            currentKeySetId = parser.getAttributeLong((String) null, "identifier");
                            this.mKeySets.put(currentKeySetId, new KeySetHandle(currentKeySetId, 0));
                            this.mKeySetMapping.put(currentKeySetId, new ArraySet<>());
                        } else if (tagName.equals("key-id")) {
                            long id = parser.getAttributeLong((String) null, "identifier");
                            this.mKeySetMapping.get(currentKeySetId).add(Long.valueOf(id));
                        }
                    }
                } else {
                    return;
                }
            } else {
                return;
            }
        }
    }

    void readPublicKeyLPw(TypedXmlPullParser parser) throws XmlPullParserException {
        long identifier = parser.getAttributeLong((String) null, "identifier");
        byte[] publicKey = parser.getAttributeBytesBase64((String) null, "value", (byte[]) null);
        PublicKey pub = FrameworkParsingPackageUtils.parsePublicKey(publicKey);
        if (pub != null) {
            PublicKeyHandle pkh = new PublicKeyHandle(identifier, 0, pub);
            this.mPublicKeys.put(identifier, pkh);
        }
    }

    private void addRefCountsFromSavedPackagesLPw(ArrayMap<Long, Integer> keySetRefCounts) {
        int numRefCounts = keySetRefCounts.size();
        for (int i = 0; i < numRefCounts; i++) {
            KeySetHandle ks = this.mKeySets.get(keySetRefCounts.keyAt(i).longValue());
            if (ks == null) {
                Slog.wtf(TAG, "Encountered non-existent key-set reference when reading settings");
            } else {
                ks.setRefCountLPw(keySetRefCounts.valueAt(i).intValue());
            }
        }
        ArraySet<Long> orphanedKeySets = new ArraySet<>();
        int numKeySets = this.mKeySets.size();
        for (int i2 = 0; i2 < numKeySets; i2++) {
            if (this.mKeySets.valueAt(i2).getRefCountLPr() == 0) {
                Slog.wtf(TAG, "Encountered key-set w/out package references when reading settings");
                orphanedKeySets.add(Long.valueOf(this.mKeySets.keyAt(i2)));
            }
            ArraySet<Long> pubKeys = this.mKeySetMapping.valueAt(i2);
            int pkSize = pubKeys.size();
            for (int j = 0; j < pkSize; j++) {
                this.mPublicKeys.get(pubKeys.valueAt(j).longValue()).incrRefCountLPw();
            }
        }
        int numOrphans = orphanedKeySets.size();
        for (int i3 = 0; i3 < numOrphans; i3++) {
            decrementKeySetLPw(orphanedKeySets.valueAt(i3).longValue());
        }
    }
}
