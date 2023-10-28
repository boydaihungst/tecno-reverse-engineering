package com.android.server.devicepolicy;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.graphics.drawable.Icon;
import android.hardware.audio.common.V2_0.AudioFormat;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Pair;
import com.android.internal.notification.SystemNotificationChannels;
import com.android.server.devicepolicy.DevicePolicyManagerService;
import com.android.server.utils.Slogf;
import java.io.FileNotFoundException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.security.SecureRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
/* loaded from: classes.dex */
public class RemoteBugreportManager {
    static final String BUGREPORT_MIMETYPE = "application/vnd.android.bugreport";
    private static final String CTL_STOP = "ctl.stop";
    private static final int NOTIFICATION_ID = 678432343;
    private static final String REMOTE_BUGREPORT_SERVICE = "bugreportd";
    private static final long REMOTE_BUGREPORT_TIMEOUT_MILLIS = 600000;
    private final Context mContext;
    private final Handler mHandler;
    private final DevicePolicyManagerService.Injector mInjector;
    private final DevicePolicyManagerService mService;
    private final SecureRandom mRng = new SecureRandom();
    private final AtomicLong mRemoteBugreportNonce = new AtomicLong();
    private final AtomicBoolean mRemoteBugreportServiceIsActive = new AtomicBoolean();
    private final AtomicBoolean mRemoteBugreportSharingAccepted = new AtomicBoolean();
    private final Runnable mRemoteBugreportTimeoutRunnable = new Runnable() { // from class: com.android.server.devicepolicy.RemoteBugreportManager$$ExternalSyntheticLambda0
        @Override // java.lang.Runnable
        public final void run() {
            RemoteBugreportManager.this.m3162x1ce38865();
        }
    };
    private final BroadcastReceiver mRemoteBugreportFinishedReceiver = new BroadcastReceiver() { // from class: com.android.server.devicepolicy.RemoteBugreportManager.1
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.REMOTE_BUGREPORT_DISPATCH".equals(intent.getAction()) && RemoteBugreportManager.this.mRemoteBugreportServiceIsActive.get()) {
                RemoteBugreportManager.this.onBugreportFinished(intent);
            }
        }
    };
    private final BroadcastReceiver mRemoteBugreportConsentReceiver = new BroadcastReceiver() { // from class: com.android.server.devicepolicy.RemoteBugreportManager.2
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            RemoteBugreportManager.this.mInjector.getNotificationManager().cancel("DevicePolicyManager", RemoteBugreportManager.NOTIFICATION_ID);
            if ("com.android.server.action.REMOTE_BUGREPORT_SHARING_ACCEPTED".equals(action)) {
                RemoteBugreportManager.this.onBugreportSharingAccepted();
            } else if ("com.android.server.action.REMOTE_BUGREPORT_SHARING_DECLINED".equals(action)) {
                RemoteBugreportManager.this.onBugreportSharingDeclined();
            }
            RemoteBugreportManager.this.mContext.unregisterReceiver(RemoteBugreportManager.this.mRemoteBugreportConsentReceiver);
        }
    };

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    @interface RemoteBugreportNotificationType {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$0$com-android-server-devicepolicy-RemoteBugreportManager  reason: not valid java name */
    public /* synthetic */ void m3162x1ce38865() {
        if (this.mRemoteBugreportServiceIsActive.get()) {
            onBugreportFailed();
        }
    }

    public RemoteBugreportManager(DevicePolicyManagerService service, DevicePolicyManagerService.Injector injector) {
        this.mService = service;
        this.mInjector = injector;
        this.mContext = service.mContext;
        this.mHandler = service.mHandler;
    }

    private Notification buildNotification(int type) {
        Intent dialogIntent = new Intent("android.settings.SHOW_REMOTE_BUGREPORT_DIALOG");
        dialogIntent.addFlags(268468224);
        dialogIntent.putExtra("android.app.extra.bugreport_notification_type", type);
        ActivityInfo targetInfo = dialogIntent.resolveActivityInfo(this.mContext.getPackageManager(), 1048576);
        if (targetInfo != null) {
            dialogIntent.setComponent(targetInfo.getComponentName());
        } else {
            Slogf.wtf("DevicePolicyManager", "Failed to resolve intent for remote bugreport dialog");
        }
        PendingIntent pendingDialogIntent = PendingIntent.getActivityAsUser(this.mContext, type, dialogIntent, 67108864, null, UserHandle.CURRENT);
        Notification.Builder builder = new Notification.Builder(this.mContext, SystemNotificationChannels.DEVICE_ADMIN).setSmallIcon(17303614).setOngoing(true).setLocalOnly(true).setContentIntent(pendingDialogIntent).setColor(this.mContext.getColor(17170460)).extend(new Notification.TvExtender());
        if (type == 2) {
            builder.setContentTitle(this.mContext.getString(17041507)).setProgress(0, 0, true);
        } else if (type == 1) {
            builder.setContentTitle(this.mContext.getString(17041620)).setProgress(0, 0, true);
        } else if (type == 3) {
            PendingIntent pendingIntentAccept = PendingIntent.getBroadcast(this.mContext, NOTIFICATION_ID, new Intent("com.android.server.action.REMOTE_BUGREPORT_SHARING_ACCEPTED"), AudioFormat.AAC_ADIF);
            PendingIntent pendingIntentDecline = PendingIntent.getBroadcast(this.mContext, NOTIFICATION_ID, new Intent("com.android.server.action.REMOTE_BUGREPORT_SHARING_DECLINED"), AudioFormat.AAC_ADIF);
            builder.addAction(new Notification.Action.Builder((Icon) null, this.mContext.getString(17040132), pendingIntentDecline).build()).addAction(new Notification.Action.Builder((Icon) null, this.mContext.getString(17041502), pendingIntentAccept).build()).setContentTitle(this.mContext.getString(17041504)).setContentText(this.mContext.getString(17041503)).setStyle(new Notification.BigTextStyle().bigText(this.mContext.getString(17041503)));
        }
        return builder.build();
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [225=4] */
    public boolean requestBugreport() {
        long nonce;
        if (this.mRemoteBugreportServiceIsActive.get() || this.mService.getDeviceOwnerRemoteBugreportUriAndHash() != null) {
            Slogf.d("DevicePolicyManager", "Remote bugreport wasn't started because there's already one running");
            return false;
        }
        long callingIdentity = this.mInjector.binderClearCallingIdentity();
        do {
            try {
                nonce = this.mRng.nextLong();
            } catch (RemoteException re) {
                Slogf.e("DevicePolicyManager", "Failed to make remote calls to start bugreportremote service", re);
                return false;
            } finally {
                this.mInjector.binderRestoreCallingIdentity(callingIdentity);
            }
        } while (nonce == 0);
        this.mInjector.getIActivityManager().requestRemoteBugReport(nonce);
        this.mRemoteBugreportNonce.set(nonce);
        this.mRemoteBugreportServiceIsActive.set(true);
        this.mRemoteBugreportSharingAccepted.set(false);
        registerRemoteBugreportReceivers();
        this.mInjector.getNotificationManager().notifyAsUser("DevicePolicyManager", NOTIFICATION_ID, buildNotification(1), UserHandle.ALL);
        this.mHandler.postDelayed(this.mRemoteBugreportTimeoutRunnable, 600000L);
        return true;
    }

    private void registerRemoteBugreportReceivers() {
        try {
            IntentFilter filterFinished = new IntentFilter("android.intent.action.REMOTE_BUGREPORT_DISPATCH", BUGREPORT_MIMETYPE);
            this.mContext.registerReceiver(this.mRemoteBugreportFinishedReceiver, filterFinished, 2);
        } catch (IntentFilter.MalformedMimeTypeException e) {
            Slogf.w("DevicePolicyManager", e, "Failed to set type %s", BUGREPORT_MIMETYPE);
        }
        IntentFilter filterConsent = new IntentFilter();
        filterConsent.addAction("com.android.server.action.REMOTE_BUGREPORT_SHARING_DECLINED");
        filterConsent.addAction("com.android.server.action.REMOTE_BUGREPORT_SHARING_ACCEPTED");
        this.mContext.registerReceiver(this.mRemoteBugreportConsentReceiver, filterConsent);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onBugreportFinished(Intent intent) {
        long nonce = intent.getLongExtra("android.intent.extra.REMOTE_BUGREPORT_NONCE", 0L);
        if (nonce == 0 || this.mRemoteBugreportNonce.get() != nonce) {
            Slogf.w("DevicePolicyManager", "Invalid nonce provided, ignoring " + nonce);
            return;
        }
        this.mHandler.removeCallbacks(this.mRemoteBugreportTimeoutRunnable);
        this.mRemoteBugreportServiceIsActive.set(false);
        Uri bugreportUri = intent.getData();
        String bugreportUriString = null;
        if (bugreportUri != null) {
            bugreportUriString = bugreportUri.toString();
        }
        String bugreportHash = intent.getStringExtra("android.intent.extra.REMOTE_BUGREPORT_HASH");
        if (this.mRemoteBugreportSharingAccepted.get()) {
            shareBugreportWithDeviceOwnerIfExists(bugreportUriString, bugreportHash);
            this.mInjector.getNotificationManager().cancel("DevicePolicyManager", NOTIFICATION_ID);
        } else {
            this.mService.setDeviceOwnerRemoteBugreportUriAndHash(bugreportUriString, bugreportHash);
            this.mInjector.getNotificationManager().notifyAsUser("DevicePolicyManager", NOTIFICATION_ID, buildNotification(3), UserHandle.ALL);
        }
        this.mContext.unregisterReceiver(this.mRemoteBugreportFinishedReceiver);
    }

    private void onBugreportFailed() {
        this.mRemoteBugreportServiceIsActive.set(false);
        this.mInjector.systemPropertiesSet(CTL_STOP, REMOTE_BUGREPORT_SERVICE);
        this.mRemoteBugreportSharingAccepted.set(false);
        this.mService.setDeviceOwnerRemoteBugreportUriAndHash(null, null);
        this.mInjector.getNotificationManager().cancel("DevicePolicyManager", NOTIFICATION_ID);
        Bundle extras = new Bundle();
        extras.putInt("android.app.extra.BUGREPORT_FAILURE_REASON", 0);
        this.mService.sendDeviceOwnerCommand("android.app.action.BUGREPORT_FAILED", extras);
        this.mContext.unregisterReceiver(this.mRemoteBugreportConsentReceiver);
        this.mContext.unregisterReceiver(this.mRemoteBugreportFinishedReceiver);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onBugreportSharingAccepted() {
        this.mRemoteBugreportSharingAccepted.set(true);
        Pair<String, String> uriAndHash = this.mService.getDeviceOwnerRemoteBugreportUriAndHash();
        if (uriAndHash != null) {
            shareBugreportWithDeviceOwnerIfExists((String) uriAndHash.first, (String) uriAndHash.second);
        } else if (this.mRemoteBugreportServiceIsActive.get()) {
            this.mInjector.getNotificationManager().notifyAsUser("DevicePolicyManager", NOTIFICATION_ID, buildNotification(2), UserHandle.ALL);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onBugreportSharingDeclined() {
        if (this.mRemoteBugreportServiceIsActive.get()) {
            this.mInjector.systemPropertiesSet(CTL_STOP, REMOTE_BUGREPORT_SERVICE);
            this.mRemoteBugreportServiceIsActive.set(false);
            this.mHandler.removeCallbacks(this.mRemoteBugreportTimeoutRunnable);
            this.mContext.unregisterReceiver(this.mRemoteBugreportFinishedReceiver);
        }
        this.mRemoteBugreportSharingAccepted.set(false);
        this.mService.setDeviceOwnerRemoteBugreportUriAndHash(null, null);
        this.mService.sendDeviceOwnerCommand("android.app.action.BUGREPORT_SHARING_DECLINED", null);
    }

    private void shareBugreportWithDeviceOwnerIfExists(String bugreportUriString, String bugreportHash) {
        try {
            try {
            } catch (FileNotFoundException e) {
                Bundle extras = new Bundle();
                extras.putInt("android.app.extra.BUGREPORT_FAILURE_REASON", 1);
                this.mService.sendDeviceOwnerCommand("android.app.action.BUGREPORT_FAILED", extras);
            }
            if (bugreportUriString == null) {
                throw new FileNotFoundException();
            }
            Uri bugreportUri = Uri.parse(bugreportUriString);
            this.mService.sendBugreportToDeviceOwner(bugreportUri, bugreportHash);
        } finally {
            this.mRemoteBugreportSharingAccepted.set(false);
            this.mService.setDeviceOwnerRemoteBugreportUriAndHash(null, null);
        }
    }

    public void checkForPendingBugreportAfterBoot() {
        if (this.mService.getDeviceOwnerRemoteBugreportUriAndHash() == null) {
            return;
        }
        IntentFilter filterConsent = new IntentFilter();
        filterConsent.addAction("com.android.server.action.REMOTE_BUGREPORT_SHARING_DECLINED");
        filterConsent.addAction("com.android.server.action.REMOTE_BUGREPORT_SHARING_ACCEPTED");
        this.mContext.registerReceiver(this.mRemoteBugreportConsentReceiver, filterConsent);
        this.mInjector.getNotificationManager().notifyAsUser("DevicePolicyManager", NOTIFICATION_ID, buildNotification(3), UserHandle.ALL);
    }
}
