package com.android.server.devicepolicy;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.hardware.audio.common.V2_0.AudioFormat;
import android.os.Handler;
import android.os.RemoteException;
import android.os.UserHandle;
import android.security.Credentials;
import android.security.KeyChain;
import android.util.PluralsMessageFormatter;
import com.android.internal.notification.SystemNotificationChannels;
import com.android.server.am.AssistDataRequester;
import com.android.server.devicepolicy.DevicePolicyManagerService;
import com.android.server.utils.Slogf;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/* loaded from: classes.dex */
public class CertificateMonitor {
    protected static final int MONITORING_CERT_NOTIFICATION_ID = 33;
    private final Handler mHandler;
    private final DevicePolicyManagerService.Injector mInjector;
    private final BroadcastReceiver mRootCaReceiver;
    private final DevicePolicyManagerService mService;

    public CertificateMonitor(DevicePolicyManagerService service, DevicePolicyManagerService.Injector injector, Handler handler) {
        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { // from class: com.android.server.devicepolicy.CertificateMonitor.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                int userId = intent.getIntExtra("android.intent.extra.user_handle", getSendingUserId());
                CertificateMonitor.this.updateInstalledCertificates(UserHandle.of(userId));
            }
        };
        this.mRootCaReceiver = broadcastReceiver;
        this.mService = service;
        this.mInjector = injector;
        this.mHandler = handler;
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.USER_STARTED");
        filter.addAction("android.intent.action.USER_UNLOCKED");
        filter.addAction("android.security.action.TRUST_STORE_CHANGED");
        filter.setPriority(1000);
        injector.mContext.registerReceiverAsUser(broadcastReceiver, UserHandle.ALL, filter, null, handler);
    }

    public String installCaCert(UserHandle userHandle, byte[] certBuffer) {
        try {
            X509Certificate cert = parseCert(certBuffer);
            byte[] pemCert = Credentials.convertToPem(new Certificate[]{cert});
            try {
                KeyChain.KeyChainConnection keyChainConnection = this.mInjector.keyChainBindAsUser(userHandle);
                String installCaCertificate = keyChainConnection.getService().installCaCertificate(pemCert);
                if (keyChainConnection != null) {
                    keyChainConnection.close();
                }
                return installCaCertificate;
            } catch (RemoteException e) {
                Slogf.e("DevicePolicyManager", "installCaCertsToKeyChain(): ", e);
                return null;
            } catch (InterruptedException e1) {
                Slogf.w("DevicePolicyManager", "installCaCertsToKeyChain(): ", e1);
                Thread.currentThread().interrupt();
                return null;
            }
        } catch (IOException | CertificateException ce) {
            Slogf.e("DevicePolicyManager", "Problem converting cert", ce);
            return null;
        }
    }

    public void uninstallCaCerts(UserHandle userHandle, String[] aliases) {
        try {
            KeyChain.KeyChainConnection keyChainConnection = this.mInjector.keyChainBindAsUser(userHandle);
            for (String str : aliases) {
                try {
                    keyChainConnection.getService().deleteCaCertificate(str);
                } catch (Throwable th) {
                    if (keyChainConnection != null) {
                        try {
                            keyChainConnection.close();
                        } catch (Throwable th2) {
                            th.addSuppressed(th2);
                        }
                    }
                    throw th;
                }
            }
            if (keyChainConnection != null) {
                keyChainConnection.close();
            }
        } catch (RemoteException e) {
            Slogf.e("DevicePolicyManager", "from CaCertUninstaller: ", e);
        } catch (InterruptedException ie) {
            Slogf.w("DevicePolicyManager", "CaCertUninstaller: ", ie);
            Thread.currentThread().interrupt();
        }
    }

    private List<String> getInstalledCaCertificates(UserHandle userHandle) throws RemoteException, RuntimeException {
        try {
            KeyChain.KeyChainConnection conn = this.mInjector.keyChainBindAsUser(userHandle);
            try {
                List<String> list = conn.getService().getUserCaAliases().getList();
                if (conn != null) {
                    conn.close();
                }
                return list;
            } catch (Throwable th) {
                if (conn != null) {
                    try {
                        conn.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                }
                throw th;
            }
        } catch (AssertionError e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e2) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e2);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onCertificateApprovalsChanged$0$com-android-server-devicepolicy-CertificateMonitor  reason: not valid java name */
    public /* synthetic */ void m2925xd0eee361(int userId) {
        updateInstalledCertificates(UserHandle.of(userId));
    }

    public void onCertificateApprovalsChanged(final int userId) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.devicepolicy.CertificateMonitor$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                CertificateMonitor.this.m2925xd0eee361(userId);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateInstalledCertificates(UserHandle userHandle) {
        int userId = userHandle.getIdentifier();
        if (!this.mInjector.getUserManager().isUserUnlocked(userId)) {
            return;
        }
        try {
            List<String> installedCerts = getInstalledCaCertificates(userHandle);
            this.mService.onInstalledCertificatesChanged(userHandle, installedCerts);
            int pendingCertificateCount = installedCerts.size() - this.mService.getAcceptedCaCertificates(userHandle).size();
            if (pendingCertificateCount == 0) {
                this.mInjector.getNotificationManager().cancelAsUser("DevicePolicyManager", 33, userHandle);
                return;
            }
            Notification noti = buildNotification(userHandle, pendingCertificateCount);
            this.mInjector.getNotificationManager().notifyAsUser("DevicePolicyManager", 33, noti, userHandle);
        } catch (RemoteException | RuntimeException e) {
            Slogf.e("DevicePolicyManager", e, "Could not retrieve certificates from KeyChain service for user %d", Integer.valueOf(userId));
        }
    }

    private Notification buildNotification(UserHandle userHandle, int pendingCertificateCount) {
        String contentText;
        int parentUserId;
        int smallIconId;
        try {
            Context userContext = this.mInjector.createContextAsUser(userHandle);
            Resources resources = this.mInjector.getResources();
            int parentUserId2 = userHandle.getIdentifier();
            if (this.mService.m3036x91e56c0c(userHandle.getIdentifier()) != null) {
                contentText = resources.getString(17041553, this.mService.getProfileOwnerName(userHandle.getIdentifier()));
                parentUserId = this.mService.getProfileParentId(userHandle.getIdentifier());
                smallIconId = 17303644;
            } else if (this.mService.getDeviceOwnerUserId() == userHandle.getIdentifier()) {
                contentText = resources.getString(17041553, this.mService.getDeviceOwnerName());
                parentUserId = parentUserId2;
                smallIconId = 17303644;
            } else {
                contentText = resources.getString(17041552);
                parentUserId = parentUserId2;
                smallIconId = 17301642;
            }
            Intent dialogIntent = new Intent("com.android.settings.MONITORING_CERT_INFO");
            dialogIntent.setFlags(268468224);
            dialogIntent.putExtra("android.settings.extra.number_of_certificates", pendingCertificateCount);
            dialogIntent.putExtra("android.intent.extra.USER_ID", userHandle.getIdentifier());
            ActivityInfo targetInfo = dialogIntent.resolveActivityInfo(this.mInjector.getPackageManager(), 1048576);
            if (targetInfo != null) {
                dialogIntent.setComponent(targetInfo.getComponentName());
            }
            PendingIntent notifyIntent = this.mInjector.pendingIntentGetActivityAsUser(userContext, 0, dialogIntent, AudioFormat.DTS_HD, null, UserHandle.of(parentUserId));
            Map<String, Object> arguments = new HashMap<>();
            arguments.put(AssistDataRequester.KEY_RECEIVER_EXTRA_COUNT, Integer.valueOf(pendingCertificateCount));
            return new Notification.Builder(userContext, SystemNotificationChannels.SECURITY).setSmallIcon(smallIconId).setContentTitle(PluralsMessageFormatter.format(resources, arguments, 17041554)).setContentText(contentText).setContentIntent(notifyIntent).setShowWhen(false).setColor(17170460).build();
        } catch (PackageManager.NameNotFoundException e) {
            Slogf.e("DevicePolicyManager", e, "Create context as %s failed", userHandle);
            return null;
        }
    }

    private static X509Certificate parseCert(byte[] certBuffer) throws CertificateException {
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        return (X509Certificate) certFactory.generateCertificate(new ByteArrayInputStream(certBuffer));
    }
}
