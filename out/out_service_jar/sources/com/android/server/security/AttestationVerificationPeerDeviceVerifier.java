package com.android.server.security;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Slog;
import com.android.server.security.AndroidKeystoreAttestationVerificationAttributes;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXCertPathChecker;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.chrono.ChronoLocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.json.JSONObject;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class AttestationVerificationPeerDeviceVerifier {
    private static final boolean DEBUG;
    private static final int MAX_PATCH_AGE_MONTHS = 12;
    private static final String TAG = "AVF";
    private CertPathValidator mCertPathValidator;
    private CertificateFactory mCertificateFactory;
    private final Context mContext;
    private final boolean mRevocationEnabled;
    private final LocalDate mTestLocalPatchDate;
    private final LocalDate mTestSystemDate;
    private final Set<TrustAnchor> mTrustAnchors;

    static {
        DEBUG = Build.IS_DEBUGGABLE && Log.isLoggable(TAG, 2);
    }

    private static void debugVerboseLog(String str, Throwable t) {
        if (DEBUG) {
            Slog.v(TAG, str, t);
        }
    }

    private static void debugVerboseLog(String str) {
        if (DEBUG) {
            Slog.v(TAG, str);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public AttestationVerificationPeerDeviceVerifier(Context context) throws Exception {
        this.mContext = (Context) Objects.requireNonNull(context);
        this.mCertificateFactory = CertificateFactory.getInstance("X.509");
        this.mCertPathValidator = CertPathValidator.getInstance("PKIX");
        this.mTrustAnchors = getTrustAnchors();
        this.mRevocationEnabled = true;
        this.mTestSystemDate = null;
        this.mTestLocalPatchDate = null;
    }

    AttestationVerificationPeerDeviceVerifier(Context context, Set<TrustAnchor> trustAnchors, boolean revocationEnabled, LocalDate systemDate, LocalDate localPatchDate) throws Exception {
        this.mContext = (Context) Objects.requireNonNull(context);
        this.mCertificateFactory = CertificateFactory.getInstance("X.509");
        this.mCertPathValidator = CertPathValidator.getInstance("PKIX");
        this.mTrustAnchors = trustAnchors;
        this.mRevocationEnabled = revocationEnabled;
        this.mTestSystemDate = systemDate;
        this.mTestLocalPatchDate = localPatchDate;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int verifyAttestation(int localBindingType, Bundle requirements, byte[] attestation) {
        if (this.mCertificateFactory == null) {
            debugVerboseLog("Was unable to initialize CertificateFactory onCreate.");
            return 2;
        } else if (this.mCertPathValidator == null) {
            debugVerboseLog("Was unable to initialize CertPathValidator onCreate.");
            return 2;
        } else {
            try {
                List<X509Certificate> certificates = getCertificates(attestation);
                if (certificates.isEmpty()) {
                    debugVerboseLog("Attestation contains no certificates.");
                    return 2;
                }
                X509Certificate leafNode = certificates.get(0);
                if (validateRequirements(localBindingType, requirements) && validateCertificateChain(certificates) && checkCertificateAttributes(leafNode, localBindingType, requirements)) {
                    return 1;
                }
                return 2;
            } catch (CertificateException e) {
                debugVerboseLog("Unable to parse attestation certificates.", e);
                return 2;
            }
        }
    }

    private List<X509Certificate> getCertificates(byte[] attestation) throws CertificateException {
        List<X509Certificate> certificates = new ArrayList<>();
        ByteArrayInputStream bis = new ByteArrayInputStream(attestation);
        while (bis.available() > 0) {
            certificates.add((X509Certificate) this.mCertificateFactory.generateCertificate(bis));
        }
        return certificates;
    }

    private boolean validateRequirements(int localBindingType, Bundle requirements) {
        if (requirements.size() != 1) {
            debugVerboseLog("Requirements does not contain exactly 1 key.");
            return false;
        } else if (localBindingType != 2 && localBindingType != 3) {
            debugVerboseLog("Binding type is not supported: " + localBindingType);
            return false;
        } else if (localBindingType == 2 && !requirements.containsKey("localbinding.public_key")) {
            debugVerboseLog("Requirements does not contain key: localbinding.public_key");
            return false;
        } else if (localBindingType != 3 || requirements.containsKey("localbinding.challenge")) {
            return true;
        } else {
            debugVerboseLog("Requirements does not contain key: localbinding.challenge");
            return false;
        }
    }

    private boolean validateCertificateChain(List<X509Certificate> certificates) {
        if (certificates.size() < 2) {
            debugVerboseLog("Certificate chain less than 2 in size.");
            return false;
        }
        try {
            CertPath certificatePath = this.mCertificateFactory.generateCertPath(certificates);
            PKIXParameters validationParams = new PKIXParameters(this.mTrustAnchors);
            if (this.mRevocationEnabled) {
                PKIXCertPathChecker checker = new AndroidRevocationStatusListChecker();
                validationParams.addCertPathChecker(checker);
            }
            validationParams.setRevocationEnabled(false);
            this.mCertPathValidator.validate(certificatePath, validationParams);
            return true;
        } catch (Throwable t) {
            debugVerboseLog("Invalid certificate chain.", t);
            return false;
        }
    }

    private Set<TrustAnchor> getTrustAnchors() throws CertPathValidatorException {
        String[] trustAnchorResources;
        Set<TrustAnchor> modifiableSet = new HashSet<>();
        try {
            for (String certString : getTrustAnchorResources()) {
                modifiableSet.add(new TrustAnchor((X509Certificate) this.mCertificateFactory.generateCertificate(new ByteArrayInputStream(getCertificateBytes(certString))), null));
            }
            return Collections.unmodifiableSet(modifiableSet);
        } catch (CertificateException e) {
            e.printStackTrace();
            throw new CertPathValidatorException("Invalid trust anchor certificate.", e);
        }
    }

    private byte[] getCertificateBytes(String certString) {
        String formattedCertString = certString.replaceAll("\\s+", "\n");
        return formattedCertString.replaceAll("-BEGIN\\nCERTIFICATE-", "-BEGIN CERTIFICATE-").replaceAll("-END\\nCERTIFICATE-", "-END CERTIFICATE-").getBytes(StandardCharsets.UTF_8);
    }

    private String[] getTrustAnchorResources() {
        return this.mContext.getResources().getStringArray(17236207);
    }

    private boolean checkCertificateAttributes(X509Certificate leafCertificate, int localBindingType, Bundle requirements) {
        try {
            AndroidKeystoreAttestationVerificationAttributes attestationAttributes = AndroidKeystoreAttestationVerificationAttributes.fromCertificate(leafCertificate);
            if (attestationAttributes.getAttestationVersion() < 3) {
                debugVerboseLog("Attestation version is not at least 3 (Keymaster 4).");
                return false;
            } else if (attestationAttributes.getKeymasterVersion() < 4) {
                debugVerboseLog("Keymaster version is not at least 4.");
                return false;
            } else if (attestationAttributes.getKeyOsVersion() < 100000) {
                debugVerboseLog("Android OS version is not 10+.");
                return false;
            } else if (!attestationAttributes.isAttestationHardwareBacked()) {
                debugVerboseLog("Key is not HW backed.");
                return false;
            } else if (!attestationAttributes.isKeymasterHardwareBacked()) {
                debugVerboseLog("Keymaster is not HW backed.");
                return false;
            } else if (attestationAttributes.getVerifiedBootState() != AndroidKeystoreAttestationVerificationAttributes.VerifiedBootState.VERIFIED) {
                debugVerboseLog("Boot state not Verified.");
                return false;
            } else {
                try {
                    if (!attestationAttributes.isVerifiedBootLocked()) {
                        debugVerboseLog("Verified boot state is not locked.");
                        return false;
                    } else if (!isValidPatchLevel(attestationAttributes.getKeyOsPatchLevel())) {
                        debugVerboseLog("OS patch level is not within valid range.");
                        return false;
                    } else if (!isValidPatchLevel(attestationAttributes.getKeyBootPatchLevel())) {
                        debugVerboseLog("Boot patch level is not within valid range.");
                        return false;
                    } else if (!isValidPatchLevel(attestationAttributes.getKeyVendorPatchLevel())) {
                        debugVerboseLog("Vendor patch level is not within valid range.");
                        return false;
                    } else if (!isValidPatchLevel(attestationAttributes.getKeyBootPatchLevel())) {
                        debugVerboseLog("Boot patch level is not within valid range.");
                        return false;
                    } else if (localBindingType == 2 && !Arrays.equals(requirements.getByteArray("localbinding.public_key"), leafCertificate.getPublicKey().getEncoded())) {
                        debugVerboseLog("Provided public key does not match leaf certificate public key.");
                        return false;
                    } else if (localBindingType == 3 && !Arrays.equals(requirements.getByteArray("localbinding.challenge"), attestationAttributes.getAttestationChallenge().toByteArray())) {
                        debugVerboseLog("Provided challenge does not match leaf certificate challenge.");
                        return false;
                    } else {
                        return true;
                    }
                } catch (IllegalStateException e) {
                    debugVerboseLog("VerifiedBootLocked is not set.", e);
                    return false;
                }
            }
        } catch (Throwable t) {
            debugVerboseLog("Could not get ParsedAttestationAttributes from Certificate.", t);
            return false;
        }
    }

    private boolean isValidPatchLevel(int patchLevel) {
        LocalDate currentDate = this.mTestSystemDate;
        if (currentDate == null) {
            currentDate = LocalDate.now(ZoneId.systemDefault());
        }
        try {
            LocalDate localPatchDate = this.mTestLocalPatchDate;
            if (localPatchDate == null) {
                localPatchDate = LocalDate.parse(Build.VERSION.SECURITY_PATCH);
            }
            if (ChronoUnit.MONTHS.between(localPatchDate, currentDate) > 12) {
                return true;
            }
            String remoteDeviceDateStr = String.valueOf(patchLevel);
            if (remoteDeviceDateStr.length() == 6 || remoteDeviceDateStr.length() == 8) {
                int patchYear = Integer.parseInt(remoteDeviceDateStr.substring(0, 4));
                int patchMonth = Integer.parseInt(remoteDeviceDateStr.substring(4, 6));
                LocalDate remotePatchDate = LocalDate.of(patchYear, patchMonth, 1);
                return remotePatchDate.compareTo((ChronoLocalDate) localPatchDate) > 0 ? ChronoUnit.MONTHS.between(localPatchDate, remotePatchDate) <= 12 : remotePatchDate.compareTo((ChronoLocalDate) localPatchDate) >= 0 || ChronoUnit.MONTHS.between(remotePatchDate, localPatchDate) <= 12;
            }
            debugVerboseLog("Patch level is not in format YYYYMM or YYYYMMDD");
            return false;
        } catch (Throwable th) {
            debugVerboseLog("Build.VERSION.SECURITY_PATCH: " + Build.VERSION.SECURITY_PATCH + " is not in format YYYY-MM-DD");
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public final class AndroidRevocationStatusListChecker extends PKIXCertPathChecker {
        private static final String REASON_PROPERTY_KEY = "reason";
        private static final String STATUS_PROPERTY_KEY = "status";
        private static final String TOP_LEVEL_JSON_PROPERTY_KEY = "entries";
        private JSONObject mJsonStatusMap;
        private String mStatusUrl;

        private AndroidRevocationStatusListChecker() {
        }

        @Override // java.security.cert.PKIXCertPathChecker, java.security.cert.CertPathChecker
        public void init(boolean forward) throws CertPathValidatorException {
            String revocationListUrl = getRevocationListUrl();
            this.mStatusUrl = revocationListUrl;
            if (revocationListUrl == null || revocationListUrl.isEmpty()) {
                throw new CertPathValidatorException("R.string.vendor_required_attestation_revocation_list_url is empty.");
            }
            this.mJsonStatusMap = getStatusMap(this.mStatusUrl);
        }

        @Override // java.security.cert.PKIXCertPathChecker, java.security.cert.CertPathChecker
        public boolean isForwardCheckingSupported() {
            return false;
        }

        @Override // java.security.cert.PKIXCertPathChecker
        public Set<String> getSupportedExtensions() {
            return null;
        }

        @Override // java.security.cert.PKIXCertPathChecker
        public void check(Certificate cert, Collection<String> unresolvedCritExts) throws CertPathValidatorException {
            X509Certificate x509Certificate = (X509Certificate) cert;
            String serialNumber = x509Certificate.getSerialNumber().toString(16);
            if (serialNumber == null) {
                throw new CertPathValidatorException("Certificate serial number can not be null.");
            }
            if (this.mJsonStatusMap.has(serialNumber)) {
                try {
                    JSONObject revocationStatus = this.mJsonStatusMap.getJSONObject(serialNumber);
                    String status = revocationStatus.getString(STATUS_PROPERTY_KEY);
                    String reason = revocationStatus.getString("reason");
                    throw new CertPathValidatorException("Invalid certificate with serial number " + serialNumber + " has status " + status + " because reason " + reason);
                } catch (Throwable th) {
                    throw new CertPathValidatorException("Unable get properties for certificate with serial number " + serialNumber);
                }
            }
        }

        private JSONObject getStatusMap(String stringUrl) throws CertPathValidatorException {
            try {
                URL url = new URL(stringUrl);
                try {
                    InputStream inputStream = url.openStream();
                    JSONObject statusListJson = new JSONObject(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8));
                    JSONObject jSONObject = statusListJson.getJSONObject(TOP_LEVEL_JSON_PROPERTY_KEY);
                    if (inputStream != null) {
                        inputStream.close();
                    }
                    return jSONObject;
                } catch (Throwable t) {
                    throw new CertPathValidatorException("Unable to parse revocation status from " + this.mStatusUrl, t);
                }
            } catch (Throwable t2) {
                throw new CertPathValidatorException("Unable to get revocation status from " + this.mStatusUrl, t2);
            }
        }

        private String getRevocationListUrl() {
            return AttestationVerificationPeerDeviceVerifier.this.mContext.getResources().getString(17041692);
        }
    }
}
