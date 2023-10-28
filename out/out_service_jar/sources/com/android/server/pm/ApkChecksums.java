package com.android.server.pm;

import android.content.Context;
import android.content.pm.ApkChecksum;
import android.content.pm.Checksum;
import android.content.pm.IOnChecksumsReadyListener;
import android.content.pm.PackageManagerInternal;
import android.content.pm.Signature;
import android.content.pm.parsing.ApkLiteParseUtils;
import android.content.pm.parsing.result.ParseResult;
import android.content.pm.parsing.result.ParseTypeImpl;
import android.os.Environment;
import android.os.FileUtils;
import android.os.Handler;
import android.os.SystemClock;
import android.os.incremental.IncrementalManager;
import android.os.incremental.IncrementalStorage;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Pair;
import android.util.Slog;
import android.util.apk.ApkSignatureSchemeV2Verifier;
import android.util.apk.ApkSignatureSchemeV3Verifier;
import android.util.apk.ApkSignatureSchemeV4Verifier;
import android.util.apk.ApkSignatureVerifier;
import android.util.apk.ApkSigningBlockUtils;
import android.util.apk.ByteBufferFactory;
import android.util.apk.SignatureInfo;
import android.util.apk.SignatureNotFoundException;
import android.util.apk.VerityBuilder;
import com.android.internal.security.VerityUtils;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.wm.ActivityTaskManagerService;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.security.DigestException;
import java.security.InvalidParameterException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import sun.security.pkcs.PKCS7;
import sun.security.pkcs.SignerInfo;
/* loaded from: classes2.dex */
public class ApkChecksums {
    static final String ALGO_MD5 = "MD5";
    static final String ALGO_SHA1 = "SHA1";
    static final String ALGO_SHA256 = "SHA256";
    static final String ALGO_SHA512 = "SHA512";
    private static final String DIGESTS_FILE_EXTENSION = ".digests";
    private static final String DIGESTS_SIGNATURE_FILE_EXTENSION = ".signature";
    private static final Certificate[] EMPTY_CERTIFICATE_ARRAY = new Certificate[0];
    static final int MAX_BUFFER_SIZE = 131072;
    static final int MIN_BUFFER_SIZE = 4096;
    private static final long PROCESS_REQUIRED_CHECKSUMS_DELAY_MILLIS = 1000;
    private static final long PROCESS_REQUIRED_CHECKSUMS_TIMEOUT_MILLIS = 86400000;
    static final String TAG = "ApkChecksums";

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static class Injector {
        private final Producer<Context> mContext;
        private final Producer<Handler> mHandlerProducer;
        private final Producer<IncrementalManager> mIncrementalManagerProducer;
        private final Producer<PackageManagerInternal> mPackageManagerInternalProducer;

        /* JADX INFO: Access modifiers changed from: package-private */
        /* loaded from: classes2.dex */
        public interface Producer<T> {
            T produce();
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public Injector(Producer<Context> context, Producer<Handler> handlerProducer, Producer<IncrementalManager> incrementalManagerProducer, Producer<PackageManagerInternal> packageManagerInternalProducer) {
            this.mContext = context;
            this.mHandlerProducer = handlerProducer;
            this.mIncrementalManagerProducer = incrementalManagerProducer;
            this.mPackageManagerInternalProducer = packageManagerInternalProducer;
        }

        public Context getContext() {
            return this.mContext.produce();
        }

        public Handler getHandler() {
            return this.mHandlerProducer.produce();
        }

        public IncrementalManager getIncrementalManager() {
            return this.mIncrementalManagerProducer.produce();
        }

        public PackageManagerInternal getPackageManagerInternal() {
            return this.mPackageManagerInternalProducer.produce();
        }
    }

    public static String buildDigestsPathForApk(String codePath) {
        if (!ApkLiteParseUtils.isApkPath(codePath)) {
            throw new IllegalStateException("Code path is not an apk " + codePath);
        }
        return codePath.substring(0, codePath.length() - ".apk".length()) + DIGESTS_FILE_EXTENSION;
    }

    public static String buildSignaturePathForDigests(String digestsPath) {
        return digestsPath + DIGESTS_SIGNATURE_FILE_EXTENSION;
    }

    public static boolean isDigestOrDigestSignatureFile(File file) {
        String name = file.getName();
        return name.endsWith(DIGESTS_FILE_EXTENSION) || name.endsWith(DIGESTS_SIGNATURE_FILE_EXTENSION);
    }

    public static File findDigestsForFile(File targetFile) {
        String digestsPath = buildDigestsPathForApk(targetFile.getAbsolutePath());
        File digestsFile = new File(digestsPath);
        if (digestsFile.exists()) {
            return digestsFile;
        }
        return null;
    }

    public static File findSignatureForDigests(File digestsFile) {
        String signaturePath = buildSignaturePathForDigests(digestsFile.getAbsolutePath());
        File signatureFile = new File(signaturePath);
        if (signatureFile.exists()) {
            return signatureFile;
        }
        return null;
    }

    public static void writeChecksums(OutputStream os, Checksum[] checksums) throws IOException {
        DataOutputStream dos = new DataOutputStream(os);
        try {
            for (Checksum checksum : checksums) {
                Checksum.writeToStream(dos, checksum);
            }
            dos.close();
        } catch (Throwable th) {
            try {
                dos.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
    }

    private static Checksum[] readChecksums(File file) throws IOException {
        InputStream is = new FileInputStream(file);
        try {
            Checksum[] readChecksums = readChecksums(is);
            is.close();
            return readChecksums;
        } catch (Throwable th) {
            try {
                is.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
    }

    public static Checksum[] readChecksums(InputStream is) throws IOException {
        DataInputStream dis = new DataInputStream(is);
        try {
            ArrayList<Checksum> checksums = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                try {
                    checksums.add(Checksum.readFromStream(dis));
                } catch (EOFException e) {
                }
            }
            Checksum[] checksumArr = (Checksum[]) checksums.toArray(new Checksum[checksums.size()]);
            dis.close();
            return checksumArr;
        } catch (Throwable th) {
            try {
                dis.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
    }

    public static Certificate[] verifySignature(Checksum[] checksums, byte[] signature) throws NoSuchAlgorithmException, IOException, SignatureException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            writeChecksums(os, checksums);
            byte[] blob = os.toByteArray();
            os.close();
            PKCS7 pkcs7 = new PKCS7(signature);
            Certificate[] certs = pkcs7.getCertificates();
            if (certs == null || certs.length == 0) {
                throw new SignatureException("Signature missing certificates");
            }
            SignerInfo[] signerInfos = pkcs7.verify(blob);
            if (signerInfos == null || signerInfos.length == 0) {
                throw new SignatureException("Verification failed");
            }
            ArrayList<Certificate> certificates = new ArrayList<>(signerInfos.length);
            for (SignerInfo signerInfo : signerInfos) {
                ArrayList<X509Certificate> chain = signerInfo.getCertificateChain(pkcs7);
                if (chain == null) {
                    throw new SignatureException("Verification passed, but certification chain is empty.");
                }
                certificates.addAll(chain);
            }
            return (Certificate[]) certificates.toArray(new Certificate[certificates.size()]);
        } catch (Throwable th) {
            try {
                os.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
    }

    public static void getChecksums(List<Pair<String, File>> filesToChecksum, int optional, int required, String installerPackageName, Certificate[] trustedInstallers, IOnChecksumsReadyListener onChecksumsReadyListener, Injector injector) {
        List<Map<Integer, ApkChecksum>> result = new ArrayList<>(filesToChecksum.size());
        int size = filesToChecksum.size();
        for (int i = 0; i < size; i++) {
            String split = (String) filesToChecksum.get(i).first;
            File file = (File) filesToChecksum.get(i).second;
            Map<Integer, ApkChecksum> checksums = new ArrayMap<>();
            result.add(checksums);
            try {
                getAvailableApkChecksums(split, file, optional | required, installerPackageName, trustedInstallers, checksums, injector);
            } catch (Throwable e) {
                Slog.e(TAG, "Preferred checksum calculation error", e);
            }
        }
        long startTime = SystemClock.uptimeMillis();
        processRequiredChecksums(filesToChecksum, result, required, onChecksumsReadyListener, injector, startTime);
    }

    /*  JADX ERROR: JadxOverflowException in pass: LoopRegionVisitor
        jadx.core.utils.exceptions.JadxOverflowException: LoopRegionVisitor.assignOnlyInLoop endless recursion
        	at jadx.core.utils.ErrorsCounter.addError(ErrorsCounter.java:56)
        	at jadx.core.utils.ErrorsCounter.error(ErrorsCounter.java:30)
        	at jadx.core.dex.attributes.nodes.NotificationAttrNode.addError(NotificationAttrNode.java:18)
        */
    /* JADX INFO: Access modifiers changed from: private */
    public static void processRequiredChecksums(final java.util.List<android.util.Pair<java.lang.String, java.io.File>> r19, final java.util.List<java.util.Map<java.lang.Integer, android.content.pm.ApkChecksum>> r20, final int r21, final android.content.pm.IOnChecksumsReadyListener r22, final com.android.server.pm.ApkChecksums.Injector r23, final long r24) {
        /*
            r9 = r19
            r10 = r21
            long r0 = android.os.SystemClock.uptimeMillis()
            long r0 = r0 - r24
            r2 = 86400000(0x5265c00, double:4.2687272E-316)
            int r0 = (r0 > r2 ? 1 : (r0 == r2 ? 0 : -1))
            if (r0 < 0) goto L13
            r0 = 1
            goto L14
        L13:
            r0 = 0
        L14:
            r11 = r0
            java.util.ArrayList r0 = new java.util.ArrayList
            r0.<init>()
            r12 = r0
            r0 = 0
            int r13 = r19.size()
            r14 = r0
        L21:
            java.lang.String r15 = "ApkChecksums"
            if (r14 >= r13) goto Lb0
            java.lang.Object r0 = r9.get(r14)
            android.util.Pair r0 = (android.util.Pair) r0
            java.lang.Object r0 = r0.first
            r7 = r0
            java.lang.String r7 = (java.lang.String) r7
            java.lang.Object r0 = r9.get(r14)
            android.util.Pair r0 = (android.util.Pair) r0
            java.lang.Object r0 = r0.second
            r8 = r0
            java.io.File r8 = (java.io.File) r8
            r6 = r20
            java.lang.Object r0 = r6.get(r14)
            r5 = r0
            java.util.Map r5 = (java.util.Map) r5
            if (r11 == 0) goto L53
            if (r10 == 0) goto L49
            goto L53
        L49:
            r16 = r11
            r17 = r13
            r18 = r14
            r11 = r5
            r13 = r7
            r14 = r8
            goto L8d
        L53:
            r4 = r23
            boolean r0 = needToWait(r8, r10, r5, r4)     // Catch: java.lang.Throwable -> L97
            if (r0 == 0) goto L81
            android.os.Handler r0 = r23.getHandler()     // Catch: java.lang.Throwable -> L97
            com.android.server.pm.ApkChecksums$$ExternalSyntheticLambda0 r3 = new com.android.server.pm.ApkChecksums$$ExternalSyntheticLambda0     // Catch: java.lang.Throwable -> L97
            r1 = r3
            r2 = r19
            r9 = r3
            r3 = r20
            r4 = r21
            r16 = r11
            r11 = r5
            r5 = r22
            r6 = r23
            r17 = r13
            r18 = r14
            r13 = r7
            r14 = r8
            r7 = r24
            r1.<init>()     // Catch: java.lang.Throwable -> L95
            r1 = 1000(0x3e8, double:4.94E-321)
            r0.postDelayed(r9, r1)     // Catch: java.lang.Throwable -> L95
            return
        L81:
            r16 = r11
            r17 = r13
            r18 = r14
            r11 = r5
            r13 = r7
            r14 = r8
            getRequiredApkChecksums(r13, r14, r10, r11)     // Catch: java.lang.Throwable -> L95
        L8d:
            java.util.Collection r0 = r11.values()     // Catch: java.lang.Throwable -> L95
            r12.addAll(r0)     // Catch: java.lang.Throwable -> L95
            goto La6
        L95:
            r0 = move-exception
            goto La1
        L97:
            r0 = move-exception
            r16 = r11
            r17 = r13
            r18 = r14
            r11 = r5
            r13 = r7
            r14 = r8
        La1:
            java.lang.String r1 = "Required checksum calculation error"
            android.util.Slog.e(r15, r1, r0)
        La6:
            int r14 = r18 + 1
            r9 = r19
            r11 = r16
            r13 = r17
            goto L21
        Lb0:
            r16 = r11
            r17 = r13
            r18 = r14
            r1 = r22
            r1.onChecksumsReady(r12)     // Catch: android.os.RemoteException -> Lbc
            goto Lc2
        Lbc:
            r0 = move-exception
            r2 = r0
            r0 = r2
            android.util.Slog.w(r15, r0)
        Lc2:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.pm.ApkChecksums.processRequiredChecksums(java.util.List, java.util.List, int, android.content.pm.IOnChecksumsReadyListener, com.android.server.pm.ApkChecksums$Injector, long):void");
    }

    private static void getAvailableApkChecksums(String split, File file, int types, String installerPackageName, Certificate[] trustedInstallers, Map<Integer, ApkChecksum> checksums, Injector injector) {
        Map<Integer, ApkChecksum> v2v3checksums;
        ApkChecksum checksum;
        String filePath = file.getAbsolutePath();
        if (isRequired(1, types, checksums) && (checksum = extractHashFromFS(split, filePath)) != null) {
            checksums.put(Integer.valueOf(checksum.getType()), checksum);
        }
        if ((isRequired(32, types, checksums) || isRequired(64, types, checksums)) && (v2v3checksums = extractHashFromV2V3Signature(split, filePath, types)) != null) {
            checksums.putAll(v2v3checksums);
        }
        getInstallerChecksums(split, file, types, installerPackageName, trustedInstallers, checksums, injector);
    }

    private static void getInstallerChecksums(String split, File file, int types, String installerPackageName, Certificate[] trustedInstallers, Map<Integer, ApkChecksum> checksums, Injector injector) {
        File digestsFile;
        Signature[] certs;
        Signature[] pastCerts;
        int i;
        if (TextUtils.isEmpty(installerPackageName)) {
            return;
        }
        if ((trustedInstallers != null && trustedInstallers.length == 0) || (digestsFile = findDigestsForFile(file)) == null) {
            return;
        }
        File signatureFile = findSignatureForDigests(digestsFile);
        try {
            Checksum[] digests = readChecksums(digestsFile);
            if (signatureFile == null) {
                AndroidPackage installer = injector.getPackageManagerInternal().getPackage(installerPackageName);
                if (installer == null) {
                    Slog.e(TAG, "Installer package not found.");
                    return;
                } else {
                    certs = installer.getSigningDetails().getSignatures();
                    pastCerts = installer.getSigningDetails().getPastSigningCertificates();
                }
            } else {
                Certificate[] certificates = verifySignature(digests, Files.readAllBytes(signatureFile.toPath()));
                if (certificates != null && certificates.length != 0) {
                    certs = new Signature[certificates.length];
                    int size = certificates.length;
                    for (int i2 = 0; i2 < size; i2++) {
                        certs[i2] = new Signature(certificates[i2].getEncoded());
                    }
                    pastCerts = null;
                }
                Slog.e(TAG, "Error validating signature");
                return;
            }
            try {
                if (certs != null && certs.length != 0) {
                    if (certs[0] != null) {
                        byte[] trustedCertBytes = certs[0].toByteArray();
                        Set<Signature> trusted = convertToSet(trustedInstallers);
                        if (trusted != null && !trusted.isEmpty()) {
                            Signature trustedCert = isTrusted(certs, trusted);
                            if (trustedCert == null) {
                                trustedCert = isTrusted(pastCerts, trusted);
                            }
                            if (trustedCert == null) {
                                return;
                            }
                            trustedCertBytes = trustedCert.toByteArray();
                        }
                        for (Checksum digest : digests) {
                            ApkChecksum system = checksums.get(Integer.valueOf(digest.getType()));
                            if (system != null && !Arrays.equals(system.getValue(), digest.getValue())) {
                                throw new InvalidParameterException("System digest " + digest.getType() + " mismatch, can't bind installer-provided digests to the APK.");
                            }
                        }
                        int length = digests.length;
                        int i3 = 0;
                        while (i3 < length) {
                            Checksum digest2 = digests[i3];
                            try {
                                if (isRequired(digest2.getType(), types, checksums)) {
                                    i = length;
                                    checksums.put(Integer.valueOf(digest2.getType()), new ApkChecksum(split, digest2, installerPackageName, trustedCertBytes));
                                } else {
                                    i = length;
                                }
                                i3++;
                                length = i;
                            } catch (IOException e) {
                                e = e;
                                Slog.e(TAG, "Error reading .digests or .signature", e);
                                return;
                            } catch (InvalidParameterException | NoSuchAlgorithmException | SignatureException e2) {
                                e = e2;
                                Slog.e(TAG, "Error validating digests. Invalid digests will be removed", e);
                                try {
                                    Files.deleteIfExists(digestsFile.toPath());
                                    if (signatureFile != null) {
                                        Files.deleteIfExists(signatureFile.toPath());
                                        return;
                                    }
                                    return;
                                } catch (IOException e3) {
                                    return;
                                }
                            } catch (CertificateEncodingException e4) {
                                e = e4;
                                Slog.e(TAG, "Error encoding trustedInstallers", e);
                                return;
                            }
                        }
                        return;
                    }
                }
                Slog.e(TAG, "Can't obtain certificates.");
            } catch (IOException e5) {
                e = e5;
            } catch (InvalidParameterException | NoSuchAlgorithmException | SignatureException e6) {
                e = e6;
            } catch (CertificateEncodingException e7) {
                e = e7;
            }
        } catch (IOException e8) {
            e = e8;
        } catch (InvalidParameterException | NoSuchAlgorithmException | SignatureException e9) {
            e = e9;
        } catch (CertificateEncodingException e10) {
            e = e10;
        }
    }

    private static boolean needToWait(File file, int types, Map<Integer, ApkChecksum> checksums, Injector injector) throws IOException {
        if (isRequired(1, types, checksums) || isRequired(2, types, checksums) || isRequired(4, types, checksums) || isRequired(8, types, checksums) || isRequired(16, types, checksums) || isRequired(32, types, checksums) || isRequired(64, types, checksums)) {
            String filePath = file.getAbsolutePath();
            if (IncrementalManager.isIncrementalPath(filePath)) {
                IncrementalManager manager = injector.getIncrementalManager();
                if (manager == null) {
                    Slog.e(TAG, "IncrementalManager is missing.");
                    return false;
                }
                IncrementalStorage storage = manager.openStorage(filePath);
                if (storage != null) {
                    return true ^ storage.isFileFullyLoaded(filePath);
                }
                Slog.e(TAG, "IncrementalStorage is missing for a path on IncFs: " + filePath);
                return false;
            }
            return false;
        }
        return false;
    }

    private static void getRequiredApkChecksums(String split, File file, int types, Map<Integer, ApkChecksum> checksums) {
        String filePath = file.getAbsolutePath();
        if (isRequired(1, types, checksums)) {
            try {
                byte[] generatedRootHash = VerityBuilder.generateFsVerityRootHash(filePath, (byte[]) null, new ByteBufferFactory() { // from class: com.android.server.pm.ApkChecksums.1
                    public ByteBuffer create(int capacity) {
                        return ByteBuffer.allocate(capacity);
                    }
                });
                checksums.put(1, new ApkChecksum(split, 1, verityHashForFile(file, generatedRootHash)));
            } catch (IOException | DigestException | NoSuchAlgorithmException e) {
                Slog.e(TAG, "Error calculating WHOLE_MERKLE_ROOT_4K_SHA256", e);
            }
        }
        calculateChecksumIfRequested(checksums, split, file, types, 2);
        calculateChecksumIfRequested(checksums, split, file, types, 4);
        calculateChecksumIfRequested(checksums, split, file, types, 8);
        calculateChecksumIfRequested(checksums, split, file, types, 16);
        calculatePartialChecksumsIfRequested(checksums, split, file, types);
    }

    private static boolean isRequired(int type, int types, Map<Integer, ApkChecksum> checksums) {
        return ((types & type) == 0 || checksums.containsKey(Integer.valueOf(type))) ? false : true;
    }

    private static Set<Signature> convertToSet(Certificate[] array) throws CertificateEncodingException {
        if (array == null) {
            return null;
        }
        Set<Signature> set = new ArraySet<>(array.length);
        for (Certificate item : array) {
            set.add(new Signature(item.getEncoded()));
        }
        return set;
    }

    private static Signature isTrusted(Signature[] signatures, Set<Signature> trusted) {
        if (signatures == null) {
            return null;
        }
        for (Signature signature : signatures) {
            if (trusted.contains(signature)) {
                return signature;
            }
        }
        return null;
    }

    private static boolean containsFile(File dir, String filePath) {
        if (dir == null) {
            return false;
        }
        return FileUtils.contains(dir.getAbsolutePath(), filePath);
    }

    private static ApkChecksum extractHashFromFS(String split, String filePath) {
        byte[] verityHash;
        if (!containsFile(Environment.getProductDirectory(), filePath) && (verityHash = VerityUtils.getFsverityRootHash(filePath)) != null) {
            return new ApkChecksum(split, 1, verityHash);
        }
        try {
            ApkSignatureSchemeV4Verifier.VerifiedSigner signer = ApkSignatureSchemeV4Verifier.extractCertificates(filePath);
            byte[] rootHash = (byte[]) signer.contentDigests.getOrDefault(3, null);
            if (rootHash != null) {
                return new ApkChecksum(split, 1, verityHashForFile(new File(filePath), rootHash));
            }
        } catch (SignatureNotFoundException e) {
        } catch (SecurityException e2) {
            Slog.e(TAG, "V4 signature error", e2);
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static byte[] verityHashForFile(File file, byte[] rootHash) {
        try {
            ByteBuffer buffer = ByteBuffer.allocate(256);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            buffer.put((byte) 1);
            buffer.put((byte) 1);
            buffer.put((byte) 12);
            buffer.put((byte) 0);
            buffer.putInt(0);
            buffer.putLong(file.length());
            buffer.put(rootHash);
            for (int i = 0; i < 208; i++) {
                buffer.put((byte) 0);
            }
            buffer.flip();
            MessageDigest md = MessageDigest.getInstance(ALGO_SHA256);
            md.update(buffer);
            return md.digest();
        } catch (NoSuchAlgorithmException e) {
            Slog.e(TAG, "Device does not support MessageDigest algorithm", e);
            return null;
        }
    }

    private static Map<Integer, ApkChecksum> extractHashFromV2V3Signature(String split, String filePath, int types) {
        byte[] hash;
        byte[] hash2;
        Map<Integer, byte[]> contentDigests = null;
        ParseTypeImpl input = ParseTypeImpl.forDefaultParsing();
        ParseResult<ApkSignatureVerifier.SigningDetailsWithDigests> result = ApkSignatureVerifier.verifySignaturesInternal(input, filePath, 2, false);
        if (result.isError()) {
            if (!(result.getException() instanceof SignatureNotFoundException)) {
                Slog.e(TAG, "Signature verification error", result.getException());
            }
        } else {
            contentDigests = ((ApkSignatureVerifier.SigningDetailsWithDigests) result.getResult()).contentDigests;
        }
        if (contentDigests == null) {
            return null;
        }
        Map<Integer, ApkChecksum> checksums = new ArrayMap<>();
        if ((types & 32) != 0 && (hash2 = contentDigests.getOrDefault(1, null)) != null) {
            checksums.put(32, new ApkChecksum(split, 32, hash2));
        }
        if ((types & 64) != 0 && (hash = contentDigests.getOrDefault(2, null)) != null) {
            checksums.put(64, new ApkChecksum(split, 64, hash));
        }
        return checksums;
    }

    private static String getMessageDigestAlgoForChecksumKind(int type) throws NoSuchAlgorithmException {
        switch (type) {
            case 2:
                return ALGO_MD5;
            case 4:
                return ALGO_SHA1;
            case 8:
                return ALGO_SHA256;
            case 16:
                return ALGO_SHA512;
            default:
                throw new NoSuchAlgorithmException("Invalid checksum type: " + type);
        }
    }

    private static void calculateChecksumIfRequested(Map<Integer, ApkChecksum> checksums, String split, File file, int required, int type) {
        byte[] checksum;
        if ((required & type) != 0 && !checksums.containsKey(Integer.valueOf(type)) && (checksum = getApkChecksum(file, type)) != null) {
            checksums.put(Integer.valueOf(type), new ApkChecksum(split, type, checksum));
        }
    }

    private static byte[] getApkChecksum(File file, int type) {
        int bufferSize = (int) Math.max(4096L, Math.min(131072L, file.length()));
        try {
            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[bufferSize];
            String algo = getMessageDigestAlgoForChecksumKind(type);
            MessageDigest md = MessageDigest.getInstance(algo);
            while (true) {
                int nread = fis.read(buffer);
                if (nread != -1) {
                    md.update(buffer, 0, nread);
                } else {
                    byte[] digest = md.digest();
                    fis.close();
                    return digest;
                }
            }
        } catch (IOException e) {
            Slog.e(TAG, "Error reading " + file.getAbsolutePath() + " to compute hash.", e);
            return null;
        } catch (NoSuchAlgorithmException e2) {
            Slog.e(TAG, "Device does not support MessageDigest algorithm", e2);
            return null;
        }
    }

    private static int[] getContentDigestAlgos(boolean needSignatureSha256, boolean needSignatureSha512) {
        if (needSignatureSha256 && needSignatureSha512) {
            return new int[]{1, 2};
        }
        return needSignatureSha256 ? new int[]{1} : new int[]{2};
    }

    private static int getChecksumKindForContentDigestAlgo(int contentDigestAlgo) {
        switch (contentDigestAlgo) {
            case 1:
                return 32;
            case 2:
                return 64;
            default:
                return -1;
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [835=4] */
    private static void calculatePartialChecksumsIfRequested(Map<Integer, ApkChecksum> checksums, String split, File file, int required) {
        boolean needSignatureSha512 = false;
        boolean needSignatureSha256 = ((required & 32) == 0 || checksums.containsKey(32)) ? false : true;
        if ((required & 64) != 0 && !checksums.containsKey(64)) {
            needSignatureSha512 = true;
        }
        if (!needSignatureSha256 && !needSignatureSha512) {
            return;
        }
        try {
            try {
                RandomAccessFile raf = new RandomAccessFile(file, ActivityTaskManagerService.DUMP_RECENTS_SHORT_CMD);
                SignatureInfo signatureInfo = null;
                try {
                    try {
                        try {
                            signatureInfo = ApkSignatureSchemeV3Verifier.findSignature(raf);
                        } catch (SignatureNotFoundException e) {
                            try {
                                signatureInfo = ApkSignatureSchemeV2Verifier.findSignature(raf);
                            } catch (SignatureNotFoundException e2) {
                            }
                        }
                        if (signatureInfo == null) {
                            Slog.e(TAG, "V2/V3 signatures not found in " + file.getAbsolutePath());
                            raf.close();
                            return;
                        }
                        int[] digestAlgos = getContentDigestAlgos(needSignatureSha256, needSignatureSha512);
                        byte[][] digests = ApkSigningBlockUtils.computeContentDigestsPer1MbChunk(digestAlgos, raf.getFD(), signatureInfo);
                        int size = digestAlgos.length;
                        for (int i = 0; i < size; i++) {
                            int checksumKind = getChecksumKindForContentDigestAlgo(digestAlgos[i]);
                            if (checksumKind != -1) {
                                try {
                                    checksums.put(Integer.valueOf(checksumKind), new ApkChecksum(split, checksumKind, digests[i]));
                                } catch (Throwable th) {
                                    th = th;
                                    Throwable th2 = th;
                                    raf.close();
                                    throw th2;
                                }
                            }
                        }
                        raf.close();
                    } catch (Throwable th3) {
                        th = th3;
                        Throwable th22 = th;
                        raf.close();
                        throw th22;
                    }
                } catch (IOException | DigestException e3) {
                    e = e3;
                    Slog.e(TAG, "Error computing hash.", e);
                }
            } catch (IOException | DigestException e4) {
                e = e4;
            }
        } catch (IOException | DigestException e5) {
            e = e5;
        }
    }
}
