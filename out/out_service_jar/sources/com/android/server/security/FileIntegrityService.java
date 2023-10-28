package com.android.server.security;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.PackageManagerInternal;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.os.UserHandle;
import android.security.IFileIntegrityService;
import android.util.Slog;
import com.android.internal.security.VerityUtils;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
/* loaded from: classes2.dex */
public class FileIntegrityService extends SystemService {
    private static final String TAG = "FileIntegrityService";
    private static CertificateFactory sCertFactory;
    private final IBinder mService;
    private Collection<X509Certificate> mTrustedCertificates;

    public FileIntegrityService(Context context) {
        super(context);
        this.mTrustedCertificates = new ArrayList();
        this.mService = new IFileIntegrityService.Stub() { // from class: com.android.server.security.FileIntegrityService.1
            public boolean isApkVeritySupported() {
                return VerityUtils.isFsVeritySupported();
            }

            public boolean isAppSourceCertificateTrusted(byte[] certificateBytes, String packageName) {
                checkCallerPermission(packageName);
                try {
                    if (!VerityUtils.isFsVeritySupported()) {
                        return false;
                    }
                    if (certificateBytes == null) {
                        Slog.w(FileIntegrityService.TAG, "Received a null certificate");
                        return false;
                    }
                    return FileIntegrityService.this.mTrustedCertificates.contains(FileIntegrityService.toCertificate(certificateBytes));
                } catch (CertificateException e) {
                    Slog.e(FileIntegrityService.TAG, "Failed to convert the certificate: " + e);
                    return false;
                }
            }

            private void checkCallerPermission(String packageName) {
                int callingUid = Binder.getCallingUid();
                int callingUserId = UserHandle.getUserId(callingUid);
                PackageManagerInternal packageManager = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
                int packageUid = packageManager.getPackageUid(packageName, 0L, callingUserId);
                if (callingUid != packageUid) {
                    throw new SecurityException("Calling uid " + callingUid + " does not own package " + packageName);
                }
                if (FileIntegrityService.this.getContext().checkCallingPermission("android.permission.INSTALL_PACKAGES") == 0) {
                    return;
                }
                AppOpsManager appOpsManager = (AppOpsManager) FileIntegrityService.this.getContext().getSystemService(AppOpsManager.class);
                int mode = appOpsManager.checkOpNoThrow(66, callingUid, packageName);
                if (mode != 0) {
                    throw new SecurityException("Caller should have INSTALL_PACKAGES or REQUEST_INSTALL_PACKAGES");
                }
            }
        };
        try {
            sCertFactory = CertificateFactory.getInstance("X.509");
        } catch (CertificateException e) {
            Slog.wtf(TAG, "Cannot get an instance of X.509 certificate factory");
        }
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        loadAllCertificates();
        publishBinderService("file_integrity", this.mService);
    }

    private void loadAllCertificates() {
        loadCertificatesFromDirectory(Environment.getRootDirectory().toPath().resolve("etc/security/fsverity"));
        loadCertificatesFromDirectory(Environment.getProductDirectory().toPath().resolve("etc/security/fsverity"));
    }

    private void loadCertificatesFromDirectory(Path path) {
        try {
            File[] files = path.toFile().listFiles();
            if (files == null) {
                return;
            }
            for (File cert : files) {
                byte[] certificateBytes = Files.readAllBytes(cert.toPath());
                if (certificateBytes == null) {
                    Slog.w(TAG, "The certificate file is empty, ignoring " + cert);
                } else {
                    collectCertificate(certificateBytes);
                }
            }
        } catch (IOException e) {
            Slog.wtf(TAG, "Failed to load fs-verity certificate from " + path, e);
        }
    }

    private void collectCertificate(byte[] bytes) {
        try {
            this.mTrustedCertificates.add(toCertificate(bytes));
        } catch (CertificateException e) {
            Slog.e(TAG, "Invalid certificate, ignored: " + e);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static X509Certificate toCertificate(byte[] bytes) throws CertificateException {
        Certificate certificate = sCertFactory.generateCertificate(new ByteArrayInputStream(bytes));
        if (!(certificate instanceof X509Certificate)) {
            throw new CertificateException("Expected to contain an X.509 certificate");
        }
        return (X509Certificate) certificate;
    }
}
