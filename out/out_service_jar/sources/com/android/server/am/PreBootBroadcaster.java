package com.android.server.am;

import android.app.ActivityManagerInternal;
import android.app.BroadcastOptions;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.IIntentReceiver;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.UserHandle;
import android.util.Slog;
import com.android.internal.notification.SystemNotificationChannels;
import com.android.internal.util.ProgressReporter;
import com.android.server.LocalServices;
import com.android.server.UiThread;
import java.util.List;
/* loaded from: classes.dex */
public abstract class PreBootBroadcaster extends IIntentReceiver.Stub {
    private static final int MSG_HIDE = 2;
    private static final int MSG_SHOW = 1;
    private static final String TAG = "PreBootBroadcaster";
    private final Intent mIntent;
    private final ProgressReporter mProgress;
    private final boolean mQuiet;
    private final ActivityManagerService mService;
    private final List<ResolveInfo> mTargets;
    private final int mUserId;
    private int mIndex = 0;
    private Handler mHandler = new Handler(UiThread.get().getLooper(), null, true) { // from class: com.android.server.am.PreBootBroadcaster.1
        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            PendingIntent contentIntent;
            Context context = PreBootBroadcaster.this.mService.mContext;
            NotificationManager notifManager = (NotificationManager) context.getSystemService(NotificationManager.class);
            int max = msg.arg1;
            int index = msg.arg2;
            switch (msg.what) {
                case 1:
                    CharSequence title = context.getText(17039669);
                    Intent intent = new Intent();
                    intent.setClassName("com.android.settings", "com.android.settings.HelpTrampoline");
                    intent.putExtra("android.intent.extra.TEXT", "help_url_upgrading");
                    if (context.getPackageManager().resolveActivity(intent, 0) != null) {
                        contentIntent = PendingIntent.getActivity(context, 0, intent, 67108864);
                    } else {
                        contentIntent = null;
                    }
                    Notification notif = new Notification.Builder(PreBootBroadcaster.this.mService.mContext, SystemNotificationChannels.UPDATES).setSmallIcon(17303614).setWhen(0L).setOngoing(true).setTicker(title).setColor(context.getColor(17170460)).setContentTitle(title).setContentIntent(contentIntent).setVisibility(1).setProgress(max, index, false).build();
                    notifManager.notifyAsUser(PreBootBroadcaster.TAG, 13, notif, UserHandle.of(PreBootBroadcaster.this.mUserId));
                    return;
                case 2:
                    notifManager.cancelAsUser(PreBootBroadcaster.TAG, 13, UserHandle.of(PreBootBroadcaster.this.mUserId));
                    return;
                default:
                    return;
            }
        }
    };

    public abstract void onFinished();

    public PreBootBroadcaster(ActivityManagerService service, int userId, ProgressReporter progress, boolean quiet) {
        this.mService = service;
        this.mUserId = userId;
        this.mProgress = progress;
        this.mQuiet = quiet;
        Intent intent = new Intent("android.intent.action.PRE_BOOT_COMPLETED");
        this.mIntent = intent;
        intent.addFlags(33554688);
        this.mTargets = service.mContext.getPackageManager().queryBroadcastReceiversAsUser(intent, 1048576, UserHandle.of(userId));
    }

    public void sendNext() {
        long duration;
        if (this.mIndex < this.mTargets.size()) {
            if (!this.mService.isUserRunning(this.mUserId, 0)) {
                Slog.i(TAG, "User " + this.mUserId + " is no longer running; skipping remaining receivers");
                this.mHandler.obtainMessage(2).sendToTarget();
                onFinished();
                return;
            }
            if (!this.mQuiet) {
                this.mHandler.obtainMessage(1, this.mTargets.size(), this.mIndex).sendToTarget();
            }
            List<ResolveInfo> list = this.mTargets;
            int i = this.mIndex;
            this.mIndex = i + 1;
            ResolveInfo ri = list.get(i);
            ComponentName componentName = ri.activityInfo.getComponentName();
            if (this.mProgress != null) {
                CharSequence label = ri.activityInfo.loadLabel(this.mService.mContext.getPackageManager());
                this.mProgress.setProgress(this.mIndex, this.mTargets.size(), this.mService.mContext.getString(17039663, label));
            }
            Slog.i(TAG, "Pre-boot of " + componentName.toShortString() + " for user " + this.mUserId);
            EventLogTags.writeAmPreBoot(this.mUserId, componentName.getPackageName());
            this.mIntent.setComponent(componentName);
            ActivityManagerInternal amInternal = (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
            if (amInternal == null) {
                duration = 10000;
            } else {
                long duration2 = amInternal.getBootTimeTempAllowListDuration();
                duration = duration2;
            }
            BroadcastOptions bOptions = BroadcastOptions.makeBasic();
            bOptions.setTemporaryAppAllowlist(duration, 0, 201, "");
            synchronized (this.mService) {
                try {
                    try {
                        ActivityManagerService.boostPriorityForLockedSection();
                        this.mService.broadcastIntentLocked(null, null, null, this.mIntent, null, this, 0, null, null, null, null, null, -1, bOptions.toBundle(), true, false, ActivityManagerService.MY_PID, 1000, Binder.getCallingUid(), Binder.getCallingPid(), this.mUserId);
                        ActivityManagerService.resetPriorityAfterLockedSection();
                        return;
                    } catch (Throwable th) {
                        th = th;
                        ActivityManagerService.resetPriorityAfterLockedSection();
                        throw th;
                    }
                } catch (Throwable th2) {
                    th = th2;
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
        }
        this.mHandler.obtainMessage(2).sendToTarget();
        onFinished();
    }

    public void performReceive(Intent intent, int resultCode, String data, Bundle extras, boolean ordered, boolean sticky, int sendingUser) {
        sendNext();
    }
}
