package com.android.server.security;

import com.android.framework.protobuf.ByteString;
import com.android.internal.org.bouncycastle.asn1.ASN1Boolean;
import com.android.internal.org.bouncycastle.asn1.ASN1Encodable;
import com.android.internal.org.bouncycastle.asn1.ASN1Enumerated;
import com.android.internal.org.bouncycastle.asn1.ASN1InputStream;
import com.android.internal.org.bouncycastle.asn1.ASN1Integer;
import com.android.internal.org.bouncycastle.asn1.ASN1ObjectIdentifier;
import com.android.internal.org.bouncycastle.asn1.ASN1OctetString;
import com.android.internal.org.bouncycastle.asn1.ASN1Sequence;
import com.android.internal.org.bouncycastle.asn1.ASN1Set;
import com.android.internal.org.bouncycastle.asn1.ASN1TaggedObject;
import com.android.internal.org.bouncycastle.asn1.x509.Certificate;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/* loaded from: classes2.dex */
class AndroidKeystoreAttestationVerificationAttributes {
    private static final String ANDROID_KEYMASTER_KEY_DESCRIPTION_EXTENSION_OID = "1.3.6.1.4.1.11129.2.1.17";
    private static final int ATTESTATION_CHALLENGE_INDEX = 4;
    private static final int ATTESTATION_SECURITY_LEVEL_INDEX = 1;
    private static final int ATTESTATION_VERSION_INDEX = 0;
    private static final int HW_AUTH_NONE = 0;
    private static final int HW_ENFORCED_INDEX = 7;
    private static final int KEYMASTER_SECURITY_LEVEL_INDEX = 3;
    private static final int KEYMASTER_UNIQUE_ID_INDEX = 5;
    private static final int KEYMASTER_VERSION_INDEX = 2;
    private static final int KM_SECURITY_LEVEL_SOFTWARE = 0;
    private static final int KM_SECURITY_LEVEL_STRONG_BOX = 2;
    private static final int KM_SECURITY_LEVEL_TRUSTED_ENVIRONMENT = 1;
    private static final int KM_TAG_ALL_APPLICATIONS = 600;
    private static final int KM_TAG_ATTESTATION_APPLICATION_ID = 709;
    private static final int KM_TAG_ATTESTATION_ID_BRAND = 710;
    private static final int KM_TAG_ATTESTATION_ID_DEVICE = 711;
    private static final int KM_TAG_ATTESTATION_ID_PRODUCT = 712;
    private static final int KM_TAG_BOOT_PATCHLEVEL = 719;
    private static final int KM_TAG_NO_AUTH_REQUIRED = 503;
    private static final int KM_TAG_OS_PATCHLEVEL = 706;
    private static final int KM_TAG_OS_VERSION = 705;
    private static final int KM_TAG_ROOT_OF_TRUST = 704;
    private static final int KM_TAG_UNLOCKED_DEVICE_REQUIRED = 509;
    private static final int KM_TAG_VENDOR_PATCHLEVEL = 718;
    private static final int KM_VERIFIED_BOOT_STATE_FAILED = 3;
    private static final int KM_VERIFIED_BOOT_STATE_SELF_SIGNED = 1;
    private static final int KM_VERIFIED_BOOT_STATE_UNVERIFIED = 2;
    private static final int KM_VERIFIED_BOOT_STATE_VERIFIED = 0;
    private static final int PACKAGE_INFO_NAME_INDEX = 0;
    private static final int PACKAGE_INFO_SET_INDEX = 0;
    private static final int PACKAGE_INFO_VERSION_INDEX = 1;
    private static final int PACKAGE_SIGNATURE_SET_INDEX = 1;
    private static final int SW_ENFORCED_INDEX = 6;
    private static final int VERIFIED_BOOT_HASH_INDEX = 3;
    private static final int VERIFIED_BOOT_KEY_INDEX = 0;
    private static final int VERIFIED_BOOT_LOCKED_INDEX = 1;
    private static final int VERIFIED_BOOT_STATE_INDEX = 2;
    private ByteString mAttestationChallenge;
    private boolean mAttestationHardwareBacked;
    private SecurityLevel mAttestationSecurityLevel;
    private Integer mAttestationVersion;
    private String mDeviceBrand;
    private String mDeviceName;
    private String mDeviceProductName;
    private boolean mKeyAllowedForAllApplications;
    private Integer mKeyAuthenticatorType;
    private Integer mKeyBootPatchLevel;
    private Integer mKeyOsPatchLevel;
    private Integer mKeyOsVersion;
    private Boolean mKeyRequiresUnlockedDevice;
    private Integer mKeyVendorPatchLevel;
    private boolean mKeymasterHardwareBacked;
    private SecurityLevel mKeymasterSecurityLevel;
    private ByteString mKeymasterUniqueId;
    private Integer mKeymasterVersion;
    private ByteString mVerifiedBootHash;
    private ByteString mVerifiedBootKey;
    private Boolean mVerifiedBootLocked;
    private VerifiedBootState mVerifiedBootState;
    private Map<String, Long> mApplicationPackageNameVersion = null;
    private List<ByteString> mApplicationCertificateDigests = null;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public enum SecurityLevel {
        SOFTWARE,
        TRUSTED_ENVIRONMENT,
        STRONG_BOX
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public enum VerifiedBootState {
        VERIFIED,
        SELF_SIGNED,
        UNVERIFIED,
        FAILED
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static AndroidKeystoreAttestationVerificationAttributes fromCertificate(X509Certificate x509Certificate) throws Exception {
        return new AndroidKeystoreAttestationVerificationAttributes(x509Certificate);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getAttestationVersion() {
        return this.mAttestationVersion.intValue();
    }

    SecurityLevel getAttestationSecurityLevel() {
        return this.mAttestationSecurityLevel;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isAttestationHardwareBacked() {
        return this.mAttestationHardwareBacked;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getKeymasterVersion() {
        return this.mKeymasterVersion.intValue();
    }

    SecurityLevel getKeymasterSecurityLevel() {
        return this.mKeymasterSecurityLevel;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isKeymasterHardwareBacked() {
        return this.mKeymasterHardwareBacked;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ByteString getAttestationChallenge() {
        return this.mAttestationChallenge;
    }

    ByteString getKeymasterUniqueId() {
        return this.mKeymasterUniqueId;
    }

    String getDeviceBrand() {
        return this.mDeviceBrand;
    }

    String getDeviceName() {
        return this.mDeviceName;
    }

    String getDeviceProductName() {
        return this.mDeviceProductName;
    }

    boolean isKeyAllowedForAllApplications() {
        return this.mKeyAllowedForAllApplications;
    }

    int getKeyAuthenticatorType() {
        Integer num = this.mKeyAuthenticatorType;
        if (num == null) {
            throw new IllegalStateException("KeyAuthenticatorType is not set.");
        }
        return num.intValue();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getKeyBootPatchLevel() {
        Integer num = this.mKeyBootPatchLevel;
        if (num == null) {
            throw new IllegalStateException("KeyBootPatchLevel is not set.");
        }
        return num.intValue();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getKeyOsPatchLevel() {
        Integer num = this.mKeyOsPatchLevel;
        if (num == null) {
            throw new IllegalStateException("KeyOsPatchLevel is not set.");
        }
        return num.intValue();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getKeyVendorPatchLevel() {
        Integer num = this.mKeyVendorPatchLevel;
        if (num == null) {
            throw new IllegalStateException("KeyVendorPatchLevel is not set.");
        }
        return num.intValue();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getKeyOsVersion() {
        Integer num = this.mKeyOsVersion;
        if (num == null) {
            throw new IllegalStateException("KeyOsVersion is not set.");
        }
        return num.intValue();
    }

    boolean isKeyRequiresUnlockedDevice() {
        Boolean bool = this.mKeyRequiresUnlockedDevice;
        if (bool == null) {
            throw new IllegalStateException("KeyRequiresUnlockedDevice is not set.");
        }
        return bool.booleanValue();
    }

    ByteString getVerifiedBootHash() {
        return this.mVerifiedBootHash;
    }

    ByteString getVerifiedBootKey() {
        return this.mVerifiedBootKey;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isVerifiedBootLocked() {
        Boolean bool = this.mVerifiedBootLocked;
        if (bool == null) {
            throw new IllegalStateException("VerifiedBootLocked is not set.");
        }
        return bool.booleanValue();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public VerifiedBootState getVerifiedBootState() {
        return this.mVerifiedBootState;
    }

    Map<String, Long> getApplicationPackageNameVersion() {
        return Collections.unmodifiableMap(this.mApplicationPackageNameVersion);
    }

    List<ByteString> getApplicationCertificateDigests() {
        return Collections.unmodifiableList(this.mApplicationCertificateDigests);
    }

    private AndroidKeystoreAttestationVerificationAttributes(X509Certificate x509Certificate) throws Exception {
        boolean z;
        boolean z2;
        ASN1TaggedObject[] array;
        ASN1TaggedObject[] array2;
        this.mAttestationVersion = null;
        this.mAttestationSecurityLevel = null;
        this.mAttestationHardwareBacked = false;
        this.mKeymasterVersion = null;
        this.mKeymasterSecurityLevel = null;
        this.mKeymasterHardwareBacked = false;
        this.mAttestationChallenge = null;
        this.mKeymasterUniqueId = null;
        this.mDeviceBrand = null;
        this.mDeviceName = null;
        this.mDeviceProductName = null;
        this.mKeyAllowedForAllApplications = false;
        this.mKeyAuthenticatorType = null;
        this.mKeyBootPatchLevel = null;
        this.mKeyOsPatchLevel = null;
        this.mKeyOsVersion = null;
        this.mKeyVendorPatchLevel = null;
        this.mKeyRequiresUnlockedDevice = null;
        this.mVerifiedBootHash = null;
        this.mVerifiedBootKey = null;
        this.mVerifiedBootLocked = null;
        this.mVerifiedBootState = null;
        Certificate certificate = Certificate.getInstance(new ASN1InputStream(x509Certificate.getEncoded()).readObject());
        ASN1Sequence keyAttributes = certificate.getTBSCertificate().getExtensions().getExtensionParsedValue(new ASN1ObjectIdentifier(ANDROID_KEYMASTER_KEY_DESCRIPTION_EXTENSION_OID));
        if (keyAttributes == null) {
            throw new CertificateEncodingException("No attestation extension found in certificate.");
        }
        this.mAttestationVersion = Integer.valueOf(getIntegerFromAsn1(keyAttributes.getObjectAt(0)));
        SecurityLevel securityLevelEnum = getSecurityLevelEnum(keyAttributes.getObjectAt(1));
        this.mAttestationSecurityLevel = securityLevelEnum;
        if (securityLevelEnum != SecurityLevel.TRUSTED_ENVIRONMENT) {
            z = false;
        } else {
            z = true;
        }
        this.mAttestationHardwareBacked = z;
        this.mAttestationChallenge = getOctetsFromAsn1(keyAttributes.getObjectAt(4));
        this.mKeymasterVersion = Integer.valueOf(getIntegerFromAsn1(keyAttributes.getObjectAt(2)));
        this.mKeymasterUniqueId = getOctetsFromAsn1(keyAttributes.getObjectAt(5));
        SecurityLevel securityLevelEnum2 = getSecurityLevelEnum(keyAttributes.getObjectAt(3));
        this.mKeymasterSecurityLevel = securityLevelEnum2;
        if (securityLevelEnum2 != SecurityLevel.TRUSTED_ENVIRONMENT) {
            z2 = false;
        } else {
            z2 = true;
        }
        this.mKeymasterHardwareBacked = z2;
        for (ASN1TaggedObject taggedEntry : keyAttributes.getObjectAt(6).toArray()) {
            switch (taggedEntry.getTagNo()) {
                case KM_TAG_UNLOCKED_DEVICE_REQUIRED /* 509 */:
                    this.mKeyRequiresUnlockedDevice = getBoolFromAsn1(taggedEntry.getObject());
                    break;
                case KM_TAG_ATTESTATION_APPLICATION_ID /* 709 */:
                    parseAttestationApplicationId(getOctetsFromAsn1(taggedEntry.getObject()).toByteArray());
                    break;
            }
        }
        for (ASN1TaggedObject taggedEntry2 : keyAttributes.getObjectAt(7).toArray()) {
            switch (taggedEntry2.getTagNo()) {
                case KM_TAG_NO_AUTH_REQUIRED /* 503 */:
                    this.mKeyAuthenticatorType = 0;
                    break;
                case 600:
                    this.mKeyAllowedForAllApplications = true;
                    break;
                case KM_TAG_ROOT_OF_TRUST /* 704 */:
                    ASN1Sequence rootOfTrust = taggedEntry2.getObject();
                    this.mVerifiedBootKey = getOctetsFromAsn1(rootOfTrust.getObjectAt(0));
                    this.mVerifiedBootLocked = getBoolFromAsn1(rootOfTrust.getObjectAt(1));
                    this.mVerifiedBootState = getVerifiedBootStateEnum(rootOfTrust.getObjectAt(2));
                    if (this.mAttestationVersion.intValue() >= 3) {
                        this.mVerifiedBootHash = getOctetsFromAsn1(rootOfTrust.getObjectAt(3));
                        break;
                    } else {
                        break;
                    }
                case KM_TAG_OS_VERSION /* 705 */:
                    this.mKeyOsVersion = Integer.valueOf(getIntegerFromAsn1(taggedEntry2.getObject()));
                    break;
                case KM_TAG_OS_PATCHLEVEL /* 706 */:
                    this.mKeyOsPatchLevel = Integer.valueOf(getIntegerFromAsn1(taggedEntry2.getObject()));
                    break;
                case KM_TAG_ATTESTATION_ID_BRAND /* 710 */:
                    this.mDeviceBrand = getUtf8FromOctetsFromAsn1(taggedEntry2.getObject());
                    break;
                case KM_TAG_ATTESTATION_ID_DEVICE /* 711 */:
                    this.mDeviceName = getUtf8FromOctetsFromAsn1(taggedEntry2.getObject());
                    break;
                case KM_TAG_ATTESTATION_ID_PRODUCT /* 712 */:
                    this.mDeviceProductName = getUtf8FromOctetsFromAsn1(taggedEntry2.getObject());
                    break;
                case KM_TAG_VENDOR_PATCHLEVEL /* 718 */:
                    this.mKeyVendorPatchLevel = Integer.valueOf(getIntegerFromAsn1(taggedEntry2.getObject()));
                    break;
                case KM_TAG_BOOT_PATCHLEVEL /* 719 */:
                    this.mKeyBootPatchLevel = Integer.valueOf(getIntegerFromAsn1(taggedEntry2.getObject()));
                    break;
            }
        }
    }

    private void parseAttestationApplicationId(byte[] attestationApplicationId) throws Exception {
        ASN1Sequence[] array;
        ASN1Encodable[] array2;
        ASN1Sequence outerSequence = ASN1Sequence.getInstance(new ASN1InputStream(attestationApplicationId).readObject());
        Map<String, Long> packageNameVersion = new HashMap<>();
        ASN1Set packageInfoSet = outerSequence.getObjectAt(0);
        for (ASN1Sequence packageInfoSequence : packageInfoSet.toArray()) {
            packageNameVersion.put(getUtf8FromOctetsFromAsn1(packageInfoSequence.getObjectAt(0)), Long.valueOf(getLongFromAsn1(packageInfoSequence.getObjectAt(1))));
        }
        List<ByteString> certificateDigests = new ArrayList<>();
        ASN1Set certificateDigestSet = outerSequence.getObjectAt(1);
        for (ASN1Encodable certificateDigestEntry : certificateDigestSet.toArray()) {
            certificateDigests.add(getOctetsFromAsn1(certificateDigestEntry));
        }
        this.mApplicationPackageNameVersion = Collections.unmodifiableMap(packageNameVersion);
        this.mApplicationCertificateDigests = Collections.unmodifiableList(certificateDigests);
    }

    private VerifiedBootState getVerifiedBootStateEnum(ASN1Encodable asn1) {
        int verifiedBoot = getEnumFromAsn1(asn1);
        switch (verifiedBoot) {
            case 0:
                return VerifiedBootState.VERIFIED;
            case 1:
                return VerifiedBootState.SELF_SIGNED;
            case 2:
                return VerifiedBootState.UNVERIFIED;
            case 3:
                return VerifiedBootState.FAILED;
            default:
                throw new IllegalArgumentException("Invalid verified boot state.");
        }
    }

    private SecurityLevel getSecurityLevelEnum(ASN1Encodable asn1) {
        int securityLevel = getEnumFromAsn1(asn1);
        switch (securityLevel) {
            case 0:
                return SecurityLevel.SOFTWARE;
            case 1:
                return SecurityLevel.TRUSTED_ENVIRONMENT;
            case 2:
                return SecurityLevel.STRONG_BOX;
            default:
                throw new IllegalArgumentException("Invalid security level.");
        }
    }

    private ByteString getOctetsFromAsn1(ASN1Encodable asn1) {
        return ByteString.copyFrom(((ASN1OctetString) asn1).getOctets());
    }

    private String getUtf8FromOctetsFromAsn1(ASN1Encodable asn1) {
        return new String(((ASN1OctetString) asn1).getOctets(), StandardCharsets.UTF_8);
    }

    private int getIntegerFromAsn1(ASN1Encodable asn1) {
        return ((ASN1Integer) asn1).getValue().intValueExact();
    }

    private long getLongFromAsn1(ASN1Encodable asn1) {
        return ((ASN1Integer) asn1).getValue().longValueExact();
    }

    private int getEnumFromAsn1(ASN1Encodable asn1) {
        return ((ASN1Enumerated) asn1).getValue().intValueExact();
    }

    private Boolean getBoolFromAsn1(ASN1Encodable asn1) {
        if (asn1 instanceof ASN1Boolean) {
            return Boolean.valueOf(((ASN1Boolean) asn1).isTrue());
        }
        return null;
    }
}
