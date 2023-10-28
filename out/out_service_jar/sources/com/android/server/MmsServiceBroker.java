package com.android.server;

import android.app.AppOpsManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Slog;
import com.android.internal.telephony.IMms;
import com.android.server.UiModeManagerService;
import com.android.server.uri.NeededUriGrants;
import com.android.server.uri.UriGrantsManagerInternal;
import java.util.List;
/* loaded from: classes.dex */
public class MmsServiceBroker extends SystemService {
    private static final int MSG_TRY_CONNECTING = 1;
    private static final long RETRY_DELAY_ON_DISCONNECTION_MS = 3000;
    private static final long SERVICE_CONNECTION_WAIT_TIME_MS = 4000;
    private static final String TAG = "MmsServiceBroker";
    private volatile AppOpsManager mAppOpsManager;
    private ServiceConnection mConnection;
    private final Handler mConnectionHandler;
    private Context mContext;
    private volatile PackageManager mPackageManager;
    private volatile IMms mService;
    private final IMms mServiceStubForFailure;
    private volatile TelephonyManager mTelephonyManager;
    private static final ComponentName MMS_SERVICE_COMPONENT = new ComponentName("com.android.mms.service", "com.android.mms.service.MmsService");
    private static final Uri FAKE_SMS_SENT_URI = Uri.parse("content://sms/sent/0");
    private static final Uri FAKE_MMS_SENT_URI = Uri.parse("content://mms/sent/0");
    private static final Uri FAKE_SMS_DRAFT_URI = Uri.parse("content://sms/draft/0");
    private static final Uri FAKE_MMS_DRAFT_URI = Uri.parse("content://mms/draft/0");

    public MmsServiceBroker(Context context) {
        super(context);
        this.mAppOpsManager = null;
        this.mPackageManager = null;
        this.mTelephonyManager = null;
        this.mConnectionHandler = new Handler() { // from class: com.android.server.MmsServiceBroker.1
            @Override // android.os.Handler
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        MmsServiceBroker.this.tryConnecting();
                        return;
                    default:
                        Slog.e(MmsServiceBroker.TAG, "Unknown message");
                        return;
                }
            }
        };
        this.mConnection = new ServiceConnection() { // from class: com.android.server.MmsServiceBroker.2
            @Override // android.content.ServiceConnection
            public void onServiceConnected(ComponentName name, IBinder service) {
                Slog.i(MmsServiceBroker.TAG, "MmsService connected");
                synchronized (MmsServiceBroker.this) {
                    MmsServiceBroker.this.mService = IMms.Stub.asInterface(Binder.allowBlocking(service));
                    MmsServiceBroker.this.notifyAll();
                }
            }

            @Override // android.content.ServiceConnection
            public void onServiceDisconnected(ComponentName name) {
                Slog.i(MmsServiceBroker.TAG, "MmsService unexpectedly disconnected");
                synchronized (MmsServiceBroker.this) {
                    MmsServiceBroker.this.mService = null;
                    MmsServiceBroker.this.notifyAll();
                }
                MmsServiceBroker.this.mConnectionHandler.sendMessageDelayed(MmsServiceBroker.this.mConnectionHandler.obtainMessage(1), 3000L);
            }
        };
        this.mServiceStubForFailure = new IMms() { // from class: com.android.server.MmsServiceBroker.3
            public IBinder asBinder() {
                return null;
            }

            public void sendMessage(int subId, String callingPkg, Uri contentUri, String locationUrl, Bundle configOverrides, PendingIntent sentIntent, long messageId, String attributionTag) throws RemoteException {
                returnPendingIntentWithError(sentIntent);
            }

            public void downloadMessage(int subId, String callingPkg, String locationUrl, Uri contentUri, Bundle configOverrides, PendingIntent downloadedIntent, long messageId, String attributionTag) throws RemoteException {
                returnPendingIntentWithError(downloadedIntent);
            }

            public Uri importTextMessage(String callingPkg, String address, int type, String text, long timestampMillis, boolean seen, boolean read) throws RemoteException {
                return null;
            }

            public Uri importMultimediaMessage(String callingPkg, Uri contentUri, String messageId, long timestampSecs, boolean seen, boolean read) throws RemoteException {
                return null;
            }

            public boolean deleteStoredMessage(String callingPkg, Uri messageUri) throws RemoteException {
                return false;
            }

            public boolean deleteStoredConversation(String callingPkg, long conversationId) throws RemoteException {
                return false;
            }

            public boolean updateStoredMessageStatus(String callingPkg, Uri messageUri, ContentValues statusValues) throws RemoteException {
                return false;
            }

            public boolean archiveStoredConversation(String callingPkg, long conversationId, boolean archived) throws RemoteException {
                return false;
            }

            public Uri addTextMessageDraft(String callingPkg, String address, String text) throws RemoteException {
                return null;
            }

            public Uri addMultimediaMessageDraft(String callingPkg, Uri contentUri) throws RemoteException {
                return null;
            }

            public void sendStoredMessage(int subId, String callingPkg, Uri messageUri, Bundle configOverrides, PendingIntent sentIntent) throws RemoteException {
                returnPendingIntentWithError(sentIntent);
            }

            public void setAutoPersisting(String callingPkg, boolean enabled) throws RemoteException {
            }

            public boolean getAutoPersisting() throws RemoteException {
                return false;
            }

            private void returnPendingIntentWithError(PendingIntent pendingIntent) {
                try {
                    pendingIntent.send(MmsServiceBroker.this.mContext, 1, (Intent) null);
                } catch (PendingIntent.CanceledException e) {
                    Slog.e(MmsServiceBroker.TAG, "Failed to return pending intent result", e);
                }
            }
        };
        this.mContext = context;
        this.mService = null;
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        publishBinderService("imms", new BinderService());
    }

    public void systemRunning() {
        Slog.i(TAG, "Delay connecting to MmsService until an API is called");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void tryConnecting() {
        Slog.i(TAG, "Connecting to MmsService");
        synchronized (this) {
            if (this.mService != null) {
                Slog.d(TAG, "Already connected");
                return;
            }
            Intent intent = new Intent();
            intent.setComponent(MMS_SERVICE_COMPONENT);
            try {
                if (!this.mContext.bindService(intent, this.mConnection, 1)) {
                    Slog.e(TAG, "Failed to bind to MmsService");
                }
            } catch (SecurityException e) {
                Slog.e(TAG, "Forbidden to bind to MmsService", e);
            }
        }
    }

    private IMms getOrConnectService() {
        synchronized (this) {
            if (this.mService != null) {
                return this.mService;
            }
            Slog.w(TAG, "MmsService not connected. Try connecting...");
            Handler handler = this.mConnectionHandler;
            handler.sendMessage(handler.obtainMessage(1));
            long shouldEnd = SystemClock.elapsedRealtime() + SERVICE_CONNECTION_WAIT_TIME_MS;
            for (long waitTime = SERVICE_CONNECTION_WAIT_TIME_MS; waitTime > 0; waitTime = shouldEnd - SystemClock.elapsedRealtime()) {
                try {
                    wait(waitTime);
                } catch (InterruptedException e) {
                    Slog.w(TAG, "Connection wait interrupted", e);
                }
                if (this.mService != null) {
                    return this.mService;
                }
            }
            Slog.e(TAG, "Can not connect to MmsService (timed out)");
            return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public IMms getServiceGuarded() {
        IMms service = getOrConnectService();
        if (service != null) {
            return service;
        }
        return this.mServiceStubForFailure;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public AppOpsManager getAppOpsManager() {
        if (this.mAppOpsManager == null) {
            this.mAppOpsManager = (AppOpsManager) this.mContext.getSystemService("appops");
        }
        return this.mAppOpsManager;
    }

    private PackageManager getPackageManager() {
        if (this.mPackageManager == null) {
            this.mPackageManager = this.mContext.getPackageManager();
        }
        return this.mPackageManager;
    }

    private TelephonyManager getTelephonyManager() {
        if (this.mTelephonyManager == null) {
            this.mTelephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
        }
        return this.mTelephonyManager;
    }

    private String getCallingPackageName() {
        String[] packages = getPackageManager().getPackagesForUid(Binder.getCallingUid());
        if (packages != null && packages.length > 0) {
            return packages[0];
        }
        return UiModeManagerService.Shell.NIGHT_MODE_STR_UNKNOWN;
    }

    /* loaded from: classes.dex */
    private final class BinderService extends IMms.Stub {
        private static final String PHONE_PACKAGE_NAME = "com.android.phone";

        private BinderService() {
        }

        public void sendMessage(int subId, String callingPkg, Uri contentUri, String locationUrl, Bundle configOverrides, PendingIntent sentIntent, long messageId, String attributionTag) throws RemoteException {
            Slog.d(MmsServiceBroker.TAG, "sendMessage() by " + callingPkg);
            MmsServiceBroker.this.mContext.enforceCallingPermission("android.permission.SEND_SMS", "Send MMS message");
            if (MmsServiceBroker.this.getAppOpsManager().noteOp(20, Binder.getCallingUid(), callingPkg, attributionTag, (String) null) != 0) {
                Slog.e(MmsServiceBroker.TAG, callingPkg + " is not allowed to call sendMessage()");
                return;
            }
            MmsServiceBroker.this.getServiceGuarded().sendMessage(subId, callingPkg, adjustUriForUserAndGrantPermission(contentUri, "android.service.carrier.CarrierMessagingService", 1, subId), locationUrl, configOverrides, sentIntent, messageId, attributionTag);
        }

        public void downloadMessage(int subId, String callingPkg, String locationUrl, Uri contentUri, Bundle configOverrides, PendingIntent downloadedIntent, long messageId, String attributionTag) throws RemoteException {
            Slog.d(MmsServiceBroker.TAG, "downloadMessage() by " + callingPkg);
            MmsServiceBroker.this.mContext.enforceCallingPermission("android.permission.RECEIVE_MMS", "Download MMS message");
            if (MmsServiceBroker.this.getAppOpsManager().noteOp(18, Binder.getCallingUid(), callingPkg, attributionTag, (String) null) != 0) {
                Slog.e(MmsServiceBroker.TAG, callingPkg + " is not allowed to call downloadMessage()");
                return;
            }
            MmsServiceBroker.this.getServiceGuarded().downloadMessage(subId, callingPkg, locationUrl, adjustUriForUserAndGrantPermission(contentUri, "android.service.carrier.CarrierMessagingService", 3, subId), configOverrides, downloadedIntent, messageId, attributionTag);
        }

        public Uri importTextMessage(String callingPkg, String address, int type, String text, long timestampMillis, boolean seen, boolean read) throws RemoteException {
            if (MmsServiceBroker.this.getAppOpsManager().noteOp(15, Binder.getCallingUid(), callingPkg, (String) null, (String) null) != 0) {
                return MmsServiceBroker.FAKE_SMS_SENT_URI;
            }
            return MmsServiceBroker.this.getServiceGuarded().importTextMessage(callingPkg, address, type, text, timestampMillis, seen, read);
        }

        public Uri importMultimediaMessage(String callingPkg, Uri contentUri, String messageId, long timestampSecs, boolean seen, boolean read) throws RemoteException {
            if (MmsServiceBroker.this.getAppOpsManager().noteOp(15, Binder.getCallingUid(), callingPkg, (String) null, (String) null) != 0) {
                return MmsServiceBroker.FAKE_MMS_SENT_URI;
            }
            return MmsServiceBroker.this.getServiceGuarded().importMultimediaMessage(callingPkg, contentUri, messageId, timestampSecs, seen, read);
        }

        public boolean deleteStoredMessage(String callingPkg, Uri messageUri) throws RemoteException {
            if (MmsServiceBroker.this.getAppOpsManager().noteOp(15, Binder.getCallingUid(), callingPkg, (String) null, (String) null) != 0) {
                return false;
            }
            return MmsServiceBroker.this.getServiceGuarded().deleteStoredMessage(callingPkg, messageUri);
        }

        public boolean deleteStoredConversation(String callingPkg, long conversationId) throws RemoteException {
            if (MmsServiceBroker.this.getAppOpsManager().noteOp(15, Binder.getCallingUid(), callingPkg, (String) null, (String) null) != 0) {
                return false;
            }
            return MmsServiceBroker.this.getServiceGuarded().deleteStoredConversation(callingPkg, conversationId);
        }

        public boolean updateStoredMessageStatus(String callingPkg, Uri messageUri, ContentValues statusValues) throws RemoteException {
            if (MmsServiceBroker.this.getAppOpsManager().noteOp(15, Binder.getCallingUid(), callingPkg, (String) null, (String) null) != 0) {
                return false;
            }
            return MmsServiceBroker.this.getServiceGuarded().updateStoredMessageStatus(callingPkg, messageUri, statusValues);
        }

        public boolean archiveStoredConversation(String callingPkg, long conversationId, boolean archived) throws RemoteException {
            if (MmsServiceBroker.this.getAppOpsManager().noteOp(15, Binder.getCallingUid(), callingPkg, (String) null, (String) null) != 0) {
                return false;
            }
            return MmsServiceBroker.this.getServiceGuarded().archiveStoredConversation(callingPkg, conversationId, archived);
        }

        public Uri addTextMessageDraft(String callingPkg, String address, String text) throws RemoteException {
            if (MmsServiceBroker.this.getAppOpsManager().noteOp(15, Binder.getCallingUid(), callingPkg, (String) null, (String) null) != 0) {
                return MmsServiceBroker.FAKE_SMS_DRAFT_URI;
            }
            return MmsServiceBroker.this.getServiceGuarded().addTextMessageDraft(callingPkg, address, text);
        }

        public Uri addMultimediaMessageDraft(String callingPkg, Uri contentUri) throws RemoteException {
            if (MmsServiceBroker.this.getAppOpsManager().noteOp(15, Binder.getCallingUid(), callingPkg, (String) null, (String) null) != 0) {
                return MmsServiceBroker.FAKE_MMS_DRAFT_URI;
            }
            return MmsServiceBroker.this.getServiceGuarded().addMultimediaMessageDraft(callingPkg, contentUri);
        }

        public void sendStoredMessage(int subId, String callingPkg, Uri messageUri, Bundle configOverrides, PendingIntent sentIntent) throws RemoteException {
            if (MmsServiceBroker.this.getAppOpsManager().noteOp(20, Binder.getCallingUid(), callingPkg, (String) null, (String) null) != 0) {
                return;
            }
            MmsServiceBroker.this.getServiceGuarded().sendStoredMessage(subId, callingPkg, messageUri, configOverrides, sentIntent);
        }

        public void setAutoPersisting(String callingPkg, boolean enabled) throws RemoteException {
            if (MmsServiceBroker.this.getAppOpsManager().noteOp(15, Binder.getCallingUid(), callingPkg, (String) null, (String) null) != 0) {
                return;
            }
            MmsServiceBroker.this.getServiceGuarded().setAutoPersisting(callingPkg, enabled);
        }

        public boolean getAutoPersisting() throws RemoteException {
            return MmsServiceBroker.this.getServiceGuarded().getAutoPersisting();
        }

        private Uri adjustUriForUserAndGrantPermission(Uri contentUri, String action, int permission, int subId) {
            Uri contentUri2;
            Intent grantIntent = new Intent();
            grantIntent.setData(contentUri);
            grantIntent.setFlags(permission);
            int callingUid = Binder.getCallingUid();
            int callingUserId = UserHandle.getCallingUserId();
            if (callingUserId == 0) {
                contentUri2 = contentUri;
            } else {
                contentUri2 = ContentProvider.maybeAddUserId(contentUri, callingUserId);
            }
            long token = Binder.clearCallingIdentity();
            try {
                UriGrantsManagerInternal ugm = (UriGrantsManagerInternal) LocalServices.getService(UriGrantsManagerInternal.class);
                NeededUriGrants needed = ugm.checkGrantUriPermissionFromIntent(grantIntent, callingUid, PHONE_PACKAGE_NAME, 0);
                ugm.grantUriPermissionUncheckedFromIntent(needed, null);
                try {
                    Intent intent = new Intent(action);
                    TelephonyManager telephonyManager = (TelephonyManager) MmsServiceBroker.this.mContext.getSystemService("phone");
                    try {
                        List<String> carrierPackages = telephonyManager.getCarrierPackageNamesForIntentAndPhone(intent, MmsServiceBroker.this.getPhoneIdFromSubId(subId));
                        if (carrierPackages != null && carrierPackages.size() == 1) {
                            NeededUriGrants carrierNeeded = ugm.checkGrantUriPermissionFromIntent(grantIntent, callingUid, carrierPackages.get(0), 0);
                            ugm.grantUriPermissionUncheckedFromIntent(carrierNeeded, null);
                        }
                        Binder.restoreCallingIdentity(token);
                        return contentUri2;
                    } catch (Throwable th) {
                        th = th;
                        Binder.restoreCallingIdentity(token);
                        throw th;
                    }
                } catch (Throwable th2) {
                    th = th2;
                    Binder.restoreCallingIdentity(token);
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int getPhoneIdFromSubId(int subId) {
        SubscriptionInfo info;
        SubscriptionManager subManager = (SubscriptionManager) this.mContext.getSystemService("telephony_subscription_service");
        if (subManager == null || (info = subManager.getActiveSubscriptionInfo(subId)) == null) {
            return -1;
        }
        return info.getSimSlotIndex();
    }
}
