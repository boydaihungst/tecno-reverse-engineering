package com.android.server;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.RecoverySystem;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.StorageManager;
import android.text.TextUtils;
import android.util.Slog;
import com.android.internal.notification.SystemNotificationChannels;
import com.android.internal.util.FunctionalUtils;
import com.android.server.utils.Slogf;
import java.io.IOException;
import java.util.function.Supplier;
/* loaded from: classes.dex */
public class MasterClearReceiver extends BroadcastReceiver {
    private static final String TAG = "MasterClear";
    private boolean mWipeEsims;
    private boolean mWipeExternalStorage;

    @Override // android.content.BroadcastReceiver
    public void onReceive(final Context context, Intent intent) {
        if (intent.getAction().equals("com.google.android.c2dm.intent.RECEIVE") && !"google.com".equals(intent.getStringExtra("from"))) {
            Slog.w(TAG, "Ignoring master clear request -- not from trusted server.");
            return;
        }
        if ("android.intent.action.MASTER_CLEAR".equals(intent.getAction())) {
            Slog.w(TAG, "The request uses the deprecated Intent#ACTION_MASTER_CLEAR, Intent#ACTION_FACTORY_RESET should be used instead.");
        }
        if (intent.hasExtra("android.intent.extra.FORCE_MASTER_CLEAR")) {
            Slog.w(TAG, "The request uses the deprecated Intent#EXTRA_FORCE_MASTER_CLEAR, Intent#EXTRA_FORCE_FACTORY_RESET should be used instead.");
        }
        String factoryResetPackage = context.getString(17039974);
        if ("android.intent.action.FACTORY_RESET".equals(intent.getAction()) && !TextUtils.isEmpty(factoryResetPackage)) {
            Slog.i(TAG, "Re-directing intent to " + factoryResetPackage);
            intent.setPackage(factoryResetPackage).setComponent(null);
            context.sendBroadcastAsUser(intent, UserHandle.SYSTEM);
            return;
        }
        final boolean shutdown = intent.getBooleanExtra("shutdown", false);
        final String reason = intent.getStringExtra("android.intent.extra.REASON");
        this.mWipeExternalStorage = intent.getBooleanExtra("android.intent.extra.WIPE_EXTERNAL_STORAGE", false);
        this.mWipeEsims = intent.getBooleanExtra("com.android.internal.intent.extra.WIPE_ESIMS", false);
        final boolean forceWipe = intent.getBooleanExtra("android.intent.extra.FORCE_MASTER_CLEAR", false) || intent.getBooleanExtra("android.intent.extra.FORCE_FACTORY_RESET", false);
        final int sendingUserId = getSendingUserId();
        if (sendingUserId != 0 && !UserManager.isHeadlessSystemUserMode()) {
            Slogf.w(TAG, "ACTION_FACTORY_RESET received on a non-system user %d, WIPING THE USER!!", Integer.valueOf(sendingUserId));
            if (!((Boolean) Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.MasterClearReceiver$$ExternalSyntheticLambda1
                public final Object getOrThrow() {
                    return MasterClearReceiver.this.m223lambda$onReceive$0$comandroidserverMasterClearReceiver(context, sendingUserId, reason);
                }
            })).booleanValue()) {
                Slogf.e(TAG, "Failed to wipe user %d", Integer.valueOf(sendingUserId));
                return;
            }
            return;
        }
        Slog.w(TAG, "!!! FACTORY RESET !!!");
        Thread thr = new Thread("Reboot") { // from class: com.android.server.MasterClearReceiver.1
            @Override // java.lang.Thread, java.lang.Runnable
            public void run() {
                try {
                    Slog.i(MasterClearReceiver.TAG, "Calling RecoverySystem.rebootWipeUserData(context, shutdown=" + shutdown + ", reason=" + reason + ", forceWipe=" + forceWipe + ", wipeEsims=" + MasterClearReceiver.this.mWipeEsims + ")");
                    RecoverySystem.rebootWipeUserData(context, shutdown, reason, forceWipe, MasterClearReceiver.this.mWipeEsims);
                    Slog.wtf(MasterClearReceiver.TAG, "Still running after master clear?!");
                } catch (IOException e) {
                    Slog.e(MasterClearReceiver.TAG, "Can't perform master clear/factory reset", e);
                } catch (SecurityException e2) {
                    Slog.e(MasterClearReceiver.TAG, "Can't perform master clear/factory reset", e2);
                }
            }
        };
        if (this.mWipeExternalStorage) {
            Slog.i(TAG, "Wiping external storage on async task");
            new WipeDataTask(context, thr).execute(new Void[0]);
            return;
        }
        Slog.i(TAG, "NOT wiping external storage; starting thread " + thr.getName());
        thr.start();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onReceive$0$com-android-server-MasterClearReceiver  reason: not valid java name */
    public /* synthetic */ Boolean m223lambda$onReceive$0$comandroidserverMasterClearReceiver(Context context, int sendingUserId, String reason) throws Exception {
        return Boolean.valueOf(wipeUser(context, sendingUserId, reason));
    }

    private boolean wipeUser(Context context, int userId, String wipeReason) {
        UserManager userManager = (UserManager) context.getSystemService(UserManager.class);
        int result = userManager.removeUserWhenPossible(UserHandle.of(userId), false);
        if (!UserManager.isRemoveResultSuccessful(result)) {
            Slogf.e(TAG, "Can't remove user %d", Integer.valueOf(userId));
            return false;
        }
        if (getCurrentForegroundUserId() == userId) {
            try {
                if (!ActivityManager.getService().switchUser(0)) {
                    Slogf.w(TAG, "Can't switch from current user %d, user will get removed when it is stopped.", Integer.valueOf(userId));
                }
            } catch (RemoteException e) {
                Slogf.w(TAG, "Can't switch from current user %d, user will get removed when it is stopped.", Integer.valueOf(userId));
            }
        }
        if (userManager.isManagedProfile(userId)) {
            sendWipeProfileNotification(context, wipeReason);
        }
        return true;
    }

    private void sendWipeProfileNotification(Context context, String wipeReason) {
        Notification notification = new Notification.Builder(context, SystemNotificationChannels.DEVICE_ADMIN).setSmallIcon(17301642).setContentTitle(getWorkProfileDeletedTitle(context)).setContentText(wipeReason).setColor(context.getColor(17170460)).setStyle(new Notification.BigTextStyle().bigText(wipeReason)).build();
        ((NotificationManager) context.getSystemService(NotificationManager.class)).notify(1001, notification);
    }

    private String getWorkProfileDeletedTitle(final Context context) {
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(DevicePolicyManager.class);
        return dpm.getResources().getString("Core.WORK_PROFILE_DELETED_TITLE", new Supplier() { // from class: com.android.server.MasterClearReceiver$$ExternalSyntheticLambda0
            @Override // java.util.function.Supplier
            public final Object get() {
                String string;
                string = context.getString(17041782);
                return string;
            }
        });
    }

    private int getCurrentForegroundUserId() {
        try {
            return ActivityManager.getCurrentUser();
        } catch (Exception e) {
            Slogf.e(TAG, "Can't get current user", e);
            return -10000;
        }
    }

    /* loaded from: classes.dex */
    private class WipeDataTask extends AsyncTask<Void, Void, Void> {
        private final Thread mChainedTask;
        private final Context mContext;
        private final ProgressDialog mProgressDialog;

        public WipeDataTask(Context context, Thread chainedTask) {
            this.mContext = context;
            this.mChainedTask = chainedTask;
            this.mProgressDialog = new ProgressDialog(context, 16974861);
        }

        @Override // android.os.AsyncTask
        protected void onPreExecute() {
            this.mProgressDialog.setIndeterminate(true);
            this.mProgressDialog.getWindow().setType(2003);
            this.mProgressDialog.setMessage(this.mContext.getText(17041355));
            this.mProgressDialog.show();
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX INFO: Access modifiers changed from: protected */
        @Override // android.os.AsyncTask
        public Void doInBackground(Void... params) {
            Slog.w(MasterClearReceiver.TAG, "Wiping adoptable disks");
            if (MasterClearReceiver.this.mWipeExternalStorage) {
                StorageManager sm = (StorageManager) this.mContext.getSystemService("storage");
                sm.wipeAdoptableDisks();
                return null;
            }
            return null;
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX INFO: Access modifiers changed from: protected */
        @Override // android.os.AsyncTask
        public void onPostExecute(Void result) {
            this.mProgressDialog.dismiss();
            this.mChainedTask.start();
        }
    }
}
