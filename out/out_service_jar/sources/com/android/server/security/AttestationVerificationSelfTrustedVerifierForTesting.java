package com.android.server.security;

import android.os.Build;
import android.os.Bundle;
import android.security.keystore.KeyGenParameterSpec;
import android.util.Log;
import android.util.Slog;
import com.android.internal.org.bouncycastle.asn1.ASN1InputStream;
import com.android.internal.org.bouncycastle.asn1.ASN1ObjectIdentifier;
import com.android.internal.org.bouncycastle.asn1.ASN1Sequence;
import com.android.internal.org.bouncycastle.asn1.x509.Certificate;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
/* loaded from: classes2.dex */
class AttestationVerificationSelfTrustedVerifierForTesting {
    private static final String ANDROID_KEYMINT_KEY_DESCRIPTION_EXTENSION_OID = "1.3.6.1.4.1.11129.2.1.17";
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final int ATTESTATION_CHALLENGE_INDEX = 4;
    private static final boolean DEBUG;
    private static final String GOLDEN_ALIAS;
    private static final String TAG = "AVF";
    private static volatile AttestationVerificationSelfTrustedVerifierForTesting sAttestationVerificationSelfTrustedVerifier;
    private final KeyStore mAndroidKeyStore;
    private X509Certificate mGoldenRootCert;
    private final CertificateFactory mCertificateFactory = CertificateFactory.getInstance("X.509");
    private final CertPathValidator mCertPathValidator = CertPathValidator.getInstance("PKIX");

    static {
        DEBUG = Build.IS_DEBUGGABLE && Log.isLoggable(TAG, 2);
        GOLDEN_ALIAS = AttestationVerificationSelfTrustedVerifierForTesting.class.getCanonicalName() + ".Golden";
        sAttestationVerificationSelfTrustedVerifier = null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static AttestationVerificationSelfTrustedVerifierForTesting getInstance() throws Exception {
        if (sAttestationVerificationSelfTrustedVerifier == null) {
            synchronized (AttestationVerificationSelfTrustedVerifierForTesting.class) {
                if (sAttestationVerificationSelfTrustedVerifier == null) {
                    sAttestationVerificationSelfTrustedVerifier = new AttestationVerificationSelfTrustedVerifierForTesting();
                }
            }
        }
        return sAttestationVerificationSelfTrustedVerifier;
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

    private AttestationVerificationSelfTrustedVerifierForTesting() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        this.mAndroidKeyStore = keyStore;
        keyStore.load(null);
        String str = GOLDEN_ALIAS;
        if (!keyStore.containsAlias(str)) {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC", "AndroidKeyStore");
            KeyGenParameterSpec parameterSpec = new KeyGenParameterSpec.Builder(str, 12).setAttestationChallenge(str.getBytes()).setDigests("SHA-256", "SHA-512").build();
            kpg.initialize(parameterSpec);
            kpg.generateKeyPair();
        }
        X509Certificate[] goldenCerts = (X509Certificate[]) ((KeyStore.PrivateKeyEntry) keyStore.getEntry(str, null)).getCertificateChain();
        this.mGoldenRootCert = goldenCerts[goldenCerts.length - 1];
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int verifyAttestation(int localBindingType, Bundle requirements, byte[] attestation) {
        List<X509Certificate> certificates = new ArrayList<>();
        ByteArrayInputStream bis = new ByteArrayInputStream(attestation);
        while (bis.available() > 0) {
            try {
                certificates.add((X509Certificate) this.mCertificateFactory.generateCertificate(bis));
            } catch (CertificateException e) {
                debugVerboseLog("Unable to parse certificates from attestation", e);
                return 2;
            }
        }
        if (localBindingType != 3 || !validateRequirements(requirements) || !checkLeafChallenge(requirements, certificates) || !verifyCertificateChain(certificates)) {
            return 2;
        }
        return 1;
    }

    private boolean verifyCertificateChain(List<X509Certificate> certificates) {
        if (certificates.size() < 2) {
            debugVerboseLog("Certificate chain less than 2 in size.");
            return false;
        }
        try {
            CertPath certificatePath = this.mCertificateFactory.generateCertPath(certificates);
            PKIXParameters validationParams = new PKIXParameters(getTrustAnchors());
            validationParams.setRevocationEnabled(false);
            this.mCertPathValidator.validate(certificatePath, validationParams);
            return true;
        } catch (Throwable t) {
            debugVerboseLog("Invalid certificate chain", t);
            return false;
        }
    }

    private Set<TrustAnchor> getTrustAnchors() {
        return Collections.singleton(new TrustAnchor(this.mGoldenRootCert, null));
    }

    private boolean validateRequirements(Bundle requirements) {
        if (requirements.size() != 1) {
            debugVerboseLog("Requirements does not contain exactly 1 key.");
            return false;
        } else if (requirements.containsKey("localbinding.challenge")) {
            return true;
        } else {
            debugVerboseLog("Requirements does not contain key: localbinding.challenge");
            return false;
        }
    }

    private boolean checkLeafChallenge(Bundle requirements, List<X509Certificate> certificates) {
        try {
            byte[] challenge = getChallengeFromCert(certificates.get(0));
            if (Arrays.equals(requirements.getByteArray("localbinding.challenge"), challenge)) {
                return true;
            }
            debugVerboseLog("Self-Trusted validation failed; challenge mismatch.");
            return false;
        } catch (Throwable t) {
            debugVerboseLog("Unable to parse challenge from certificate.", t);
            return false;
        }
    }

    private byte[] getChallengeFromCert(X509Certificate x509Certificate) throws CertificateEncodingException, IOException {
        Certificate certificate = Certificate.getInstance(new ASN1InputStream(x509Certificate.getEncoded()).readObject());
        ASN1Sequence keyAttributes = certificate.getTBSCertificate().getExtensions().getExtensionParsedValue(new ASN1ObjectIdentifier(ANDROID_KEYMINT_KEY_DESCRIPTION_EXTENSION_OID));
        return keyAttributes.getObjectAt(4).getOctets();
    }
}
