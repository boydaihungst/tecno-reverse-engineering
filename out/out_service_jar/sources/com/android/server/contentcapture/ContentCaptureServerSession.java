package com.android.server.contentcapture;

import android.app.assist.ActivityId;
import android.content.ComponentName;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.service.contentcapture.ContentCaptureService;
import android.service.contentcapture.SnapshotData;
import android.util.LocalLog;
import android.util.Slog;
import android.view.contentcapture.ContentCaptureContext;
import com.android.internal.os.IResultReceiver;
import com.android.internal.util.Preconditions;
import com.android.server.timezonedetector.ServiceConfigAccessor;
import com.android.server.usb.descriptors.UsbACInterface;
import java.io.PrintWriter;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class ContentCaptureServerSession {
    private static final String TAG = ContentCaptureServerSession.class.getSimpleName();
    public final ComponentName appComponentName;
    final IBinder mActivityToken;
    private final ContentCaptureContext mContentCaptureContext;
    private final int mId;
    private final Object mLock;
    private final ContentCapturePerUserService mService;
    private final IResultReceiver mSessionStateReceiver;
    private final int mUid;

    /* JADX INFO: Access modifiers changed from: package-private */
    public ContentCaptureServerSession(Object lock, IBinder activityToken, ActivityId activityId, ContentCapturePerUserService service, ComponentName appComponentName, IResultReceiver sessionStateReceiver, int taskId, int displayId, int sessionId, int uid, int flags) {
        Preconditions.checkArgument(sessionId != 0);
        this.mLock = lock;
        this.mActivityToken = activityToken;
        this.appComponentName = appComponentName;
        this.mService = service;
        this.mId = sessionId;
        this.mUid = uid;
        this.mContentCaptureContext = new ContentCaptureContext(null, activityId, appComponentName, displayId, activityToken, flags);
        this.mSessionStateReceiver = sessionStateReceiver;
        try {
            sessionStateReceiver.asBinder().linkToDeath(new IBinder.DeathRecipient() { // from class: com.android.server.contentcapture.ContentCaptureServerSession$$ExternalSyntheticLambda0
                @Override // android.os.IBinder.DeathRecipient
                public final void binderDied() {
                    ContentCaptureServerSession.this.m2909xc57f69d1();
                }
            }, 0);
        } catch (Exception e) {
            Slog.w(TAG, "could not register DeathRecipient for " + activityToken);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isActivitySession(IBinder activityToken) {
        return this.mActivityToken.equals(activityToken);
    }

    public void notifySessionStartedLocked(IResultReceiver clientReceiver) {
        if (this.mService.mRemoteService == null) {
            Slog.w(TAG, "notifySessionStartedLocked(): no remote service");
        } else {
            this.mService.mRemoteService.onSessionStarted(this.mContentCaptureContext, this.mId, this.mUid, clientReceiver, 2);
        }
    }

    public void setContentCaptureEnabledLocked(boolean enabled) {
        try {
            Bundle extras = new Bundle();
            int i = 1;
            extras.putBoolean(ServiceConfigAccessor.PROVIDER_MODE_ENABLED, true);
            IResultReceiver iResultReceiver = this.mSessionStateReceiver;
            if (!enabled) {
                i = 2;
            }
            iResultReceiver.send(i, extras);
        } catch (RemoteException e) {
            Slog.w(TAG, "Error async reporting result to client: " + e);
        }
    }

    public void sendActivitySnapshotLocked(SnapshotData snapshotData) {
        LocalLog logHistory = this.mService.getMaster().mRequestsHistory;
        if (logHistory != null) {
            logHistory.log("snapshot: id=" + this.mId);
        }
        if (this.mService.mRemoteService == null) {
            Slog.w(TAG, "sendActivitySnapshotLocked(): no remote service");
        } else {
            this.mService.mRemoteService.onActivitySnapshotRequest(this.mId, snapshotData);
        }
    }

    public void removeSelfLocked(boolean notifyRemoteService) {
        try {
            destroyLocked(notifyRemoteService);
        } finally {
            this.mService.removeSessionLocked(this.mId);
        }
    }

    public void destroyLocked(boolean notifyRemoteService) {
        if (this.mService.isVerbose()) {
            Slog.v(TAG, "destroy(notifyRemoteService=" + notifyRemoteService + ")");
        }
        if (notifyRemoteService) {
            if (this.mService.mRemoteService == null) {
                Slog.w(TAG, "destroyLocked(): no remote service");
            } else {
                this.mService.mRemoteService.onSessionFinished(this.mId);
            }
        }
    }

    public void resurrectLocked() {
        RemoteContentCaptureService remoteService = this.mService.mRemoteService;
        if (remoteService == null) {
            Slog.w(TAG, "destroyLocked(: no remote service");
            return;
        }
        if (this.mService.isVerbose()) {
            Slog.v(TAG, "resurrecting " + this.mActivityToken + " on " + remoteService);
        }
        remoteService.onSessionStarted(new ContentCaptureContext(this.mContentCaptureContext, 4), this.mId, this.mUid, this.mSessionStateReceiver, UsbACInterface.FORMAT_II_AC3);
    }

    public void pauseLocked() {
        if (this.mService.isVerbose()) {
            Slog.v(TAG, "pausing " + this.mActivityToken);
        }
        ContentCaptureService.setClientState(this.mSessionStateReceiver, 2052, (IBinder) null);
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: onClientDeath */
    public void m2909xc57f69d1() {
        if (this.mService.isVerbose()) {
            Slog.v(TAG, "onClientDeath(" + this.mActivityToken + "): removing session " + this.mId);
        }
        synchronized (this.mLock) {
            removeSelfLocked(true);
        }
    }

    public void dumpLocked(String prefix, PrintWriter pw) {
        pw.print(prefix);
        pw.print("id: ");
        pw.print(this.mId);
        pw.println();
        pw.print(prefix);
        pw.print("uid: ");
        pw.print(this.mUid);
        pw.println();
        pw.print(prefix);
        pw.print("context: ");
        this.mContentCaptureContext.dump(pw);
        pw.println();
        pw.print(prefix);
        pw.print("activity token: ");
        pw.println(this.mActivityToken);
        pw.print(prefix);
        pw.print("app component: ");
        pw.println(this.appComponentName);
        pw.print(prefix);
        pw.print("has autofill callback: ");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String toShortString() {
        return this.mId + ":" + this.mActivityToken;
    }

    public String toString() {
        return "ContentCaptureSession[id=" + this.mId + ", act=" + this.mActivityToken + "]";
    }
}
