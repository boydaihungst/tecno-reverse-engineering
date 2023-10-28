package com.android.server.incident;

import android.app.AppOpsManager;
import android.app.BroadcastOptions;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.IIncidentAuthListener;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.util.Log;
import com.android.server.pm.pkg.parsing.ParsingPackageUtils;
import com.android.server.wm.ActivityTaskManagerInternal;
import com.android.server.wm.ActivityTaskManagerService;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class PendingReports {
    static final String TAG = "IncidentCompanionService";
    private final AppOpsManager mAppOpsManager;
    private final Context mContext;
    private final Handler mHandler;
    private final Object mLock;
    private int mNextPendingId;
    private final PackageManager mPackageManager;
    private final ArrayList<PendingReportRec> mPending;
    private final RequestQueue mRequestQueue;

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class PendingReportRec {
        public long addedRealtime;
        public long addedWalltime;
        public String callingPackage;
        public int flags;
        public int id;
        public IIncidentAuthListener listener;
        public String receiverClass;
        public String reportId;

        PendingReportRec(String callingPackage, String receiverClass, String reportId, int flags, IIncidentAuthListener listener) {
            int i = PendingReports.this.mNextPendingId;
            PendingReports.this.mNextPendingId = i + 1;
            this.id = i;
            this.callingPackage = callingPackage;
            this.flags = flags;
            this.listener = listener;
            this.addedRealtime = SystemClock.elapsedRealtime();
            this.addedWalltime = System.currentTimeMillis();
            this.receiverClass = receiverClass;
            this.reportId = reportId;
        }

        Uri getUri() {
            Uri.Builder builder = new Uri.Builder().scheme(ActivityTaskManagerInternal.ASSIST_KEY_CONTENT).authority("android.os.IncidentManager").path("/pending").appendQueryParameter("id", Integer.toString(this.id)).appendQueryParameter("pkg", this.callingPackage).appendQueryParameter("flags", Integer.toString(this.flags)).appendQueryParameter("t", Long.toString(this.addedWalltime));
            String str = this.receiverClass;
            if (str != null && str.length() > 0) {
                builder.appendQueryParameter(ParsingPackageUtils.TAG_RECEIVER, this.receiverClass);
            }
            String str2 = this.reportId;
            if (str2 != null && str2.length() > 0) {
                builder.appendQueryParameter(ActivityTaskManagerService.DUMP_RECENTS_SHORT_CMD, this.reportId);
            }
            return builder.build();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public PendingReports(Context context) {
        Handler handler = new Handler();
        this.mHandler = handler;
        this.mRequestQueue = new RequestQueue(handler);
        this.mLock = new Object();
        this.mPending = new ArrayList<>();
        this.mNextPendingId = 1;
        this.mContext = context;
        this.mPackageManager = context.getPackageManager();
        this.mAppOpsManager = (AppOpsManager) context.getSystemService(AppOpsManager.class);
    }

    public void authorizeReport(final int callingUid, final String callingPackage, final String receiverClass, final String reportId, final int flags, final IIncidentAuthListener listener) {
        this.mRequestQueue.enqueue(listener.asBinder(), true, new Runnable() { // from class: com.android.server.incident.PendingReports$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                PendingReports.this.m3895xd84e8a7b(callingUid, callingPackage, receiverClass, reportId, flags, listener);
            }
        });
    }

    public void cancelAuthorization(final IIncidentAuthListener listener) {
        this.mRequestQueue.enqueue(listener.asBinder(), false, new Runnable() { // from class: com.android.server.incident.PendingReports$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                PendingReports.this.m3897xffda4af8(listener);
            }
        });
    }

    public List<String> getPendingReports() {
        ArrayList<String> result;
        synchronized (this.mLock) {
            int size = this.mPending.size();
            result = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                result.add(this.mPending.get(i).getUri().toString());
            }
        }
        return result;
    }

    public void approveReport(String uri) {
        synchronized (this.mLock) {
            PendingReportRec rec = findAndRemovePendingReportRecLocked(uri);
            if (rec == null) {
                Log.e(TAG, "confirmApproved: Couldn't find record for uri: " + uri);
                return;
            }
            sendBroadcast();
            Log.i(TAG, "Approved report: " + uri);
            try {
                rec.listener.onReportApproved();
            } catch (RemoteException ex) {
                Log.w(TAG, "Failed calling back for approval for: " + uri, ex);
            }
        }
    }

    public void denyReport(String uri) {
        synchronized (this.mLock) {
            PendingReportRec rec = findAndRemovePendingReportRecLocked(uri);
            if (rec == null) {
                Log.e(TAG, "confirmDenied: Couldn't find record for uri: " + uri);
                return;
            }
            sendBroadcast();
            Log.i(TAG, "Denied report: " + uri);
            try {
                rec.listener.onReportDenied();
            } catch (RemoteException ex) {
                Log.w(TAG, "Failed calling back for denial for: " + uri, ex);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        if (args.length == 0) {
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            synchronized (this.mLock) {
                int size = this.mPending.size();
                writer.println("mPending: (" + size + ")");
                for (int i = 0; i < size; i++) {
                    PendingReportRec entry = this.mPending.get(i);
                    writer.println(String.format("  %11d %s: %s", Long.valueOf(entry.addedRealtime), df.format(new Date(entry.addedWalltime)), entry.getUri().toString()));
                }
            }
        }
    }

    public void onBootCompleted() {
        this.mRequestQueue.start();
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: authorizeReportImpl */
    public void m3895xd84e8a7b(int callingUid, String callingPackage, String receiverClass, String reportId, int flags, final IIncidentAuthListener listener) {
        PendingReportRec rec;
        if (callingUid != 0 && !isPackageInUid(callingUid, callingPackage)) {
            Log.w(TAG, "Calling uid " + callingUid + " doesn't match package " + callingPackage);
            denyReportBeforeAddingRec(listener, callingPackage);
            return;
        }
        final int primaryUser = getAndValidateUser();
        if (primaryUser == -10000) {
            denyReportBeforeAddingRec(listener, callingPackage);
            return;
        }
        final ComponentName receiver = getApproverComponent(primaryUser);
        if (receiver == null) {
            denyReportBeforeAddingRec(listener, callingPackage);
            return;
        }
        synchronized (this.mLock) {
            rec = new PendingReportRec(callingPackage, receiverClass, reportId, flags, listener);
            this.mPending.add(rec);
        }
        try {
            listener.asBinder().linkToDeath(new IBinder.DeathRecipient() { // from class: com.android.server.incident.PendingReports$$ExternalSyntheticLambda2
                @Override // android.os.IBinder.DeathRecipient
                public final void binderDied() {
                    PendingReports.this.m3896x714a7ef9(listener, receiver, primaryUser);
                }
            }, 0);
        } catch (RemoteException e) {
            Log.e(TAG, "Remote died while trying to register death listener: " + rec.getUri());
            cancelReportImpl(listener, receiver, primaryUser);
        }
        sendBroadcast(receiver, primaryUser);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$authorizeReportImpl$2$com-android-server-incident-PendingReports  reason: not valid java name */
    public /* synthetic */ void m3896x714a7ef9(IIncidentAuthListener listener, ComponentName receiver, int primaryUser) {
        Log.i(TAG, "Got death notification listener=" + listener);
        cancelReportImpl(listener, receiver, primaryUser);
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: cancelReportImpl */
    public void m3897xffda4af8(IIncidentAuthListener listener) {
        int primaryUser = getAndValidateUser();
        ComponentName receiver = getApproverComponent(primaryUser);
        if (primaryUser != -10000 && receiver != null) {
            cancelReportImpl(listener, receiver, primaryUser);
        }
    }

    private void cancelReportImpl(IIncidentAuthListener listener, ComponentName receiver, int primaryUser) {
        synchronized (this.mLock) {
            removePendingReportRecLocked(listener);
        }
        sendBroadcast(receiver, primaryUser);
    }

    private void sendBroadcast() {
        ComponentName receiver;
        int primaryUser = getAndValidateUser();
        if (primaryUser == -10000 || (receiver = getApproverComponent(primaryUser)) == null) {
            return;
        }
        sendBroadcast(receiver, primaryUser);
    }

    private void sendBroadcast(ComponentName receiver, int primaryUser) {
        Intent intent = new Intent("android.intent.action.PENDING_INCIDENT_REPORTS_CHANGED");
        intent.setComponent(receiver);
        intent.addFlags(268435456);
        intent.addFlags(16777216);
        BroadcastOptions options = BroadcastOptions.makeBasic();
        options.setBackgroundActivityStartsAllowed(true);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.getUserHandleForUid(primaryUser), "android.permission.APPROVE_INCIDENT_REPORTS", options.toBundle());
    }

    private PendingReportRec findAndRemovePendingReportRecLocked(String uriString) {
        Uri uri = Uri.parse(uriString);
        try {
            String idStr = uri.getQueryParameter("id");
            int id = Integer.parseInt(idStr);
            Iterator<PendingReportRec> i = this.mPending.iterator();
            while (i.hasNext()) {
                PendingReportRec rec = i.next();
                if (rec.id == id) {
                    i.remove();
                    return rec;
                }
            }
            return null;
        } catch (NumberFormatException e) {
            Log.w(TAG, "Can't parse id from: " + uriString);
            return null;
        }
    }

    private void removePendingReportRecLocked(IIncidentAuthListener listener) {
        Iterator<PendingReportRec> i = this.mPending.iterator();
        while (i.hasNext()) {
            PendingReportRec rec = i.next();
            if (rec.listener.asBinder() == listener.asBinder()) {
                Log.i(TAG, "  ...Removed PendingReportRec index=" + i + ": " + rec.getUri());
                i.remove();
            }
        }
    }

    private void denyReportBeforeAddingRec(IIncidentAuthListener listener, String pkg) {
        try {
            listener.onReportDenied();
        } catch (RemoteException ex) {
            Log.w(TAG, "Failed calling back for denial for " + pkg, ex);
        }
    }

    private int getAndValidateUser() {
        return IncidentCompanionService.getAndValidateUser(this.mContext);
    }

    private ComponentName getApproverComponent(int userId) {
        Intent intent = new Intent("android.intent.action.PENDING_INCIDENT_REPORTS_CHANGED");
        List<ResolveInfo> matches = this.mPackageManager.queryBroadcastReceiversAsUser(intent, 1835008, userId);
        if (matches.size() == 1) {
            return matches.get(0).getComponentInfo().getComponentName();
        }
        Log.w(TAG, "Didn't find exactly one BroadcastReceiver to handle android.intent.action.PENDING_INCIDENT_REPORTS_CHANGED. The report will be denied. size=" + matches.size() + ": matches=" + matches);
        return null;
    }

    private boolean isPackageInUid(int uid, String packageName) {
        try {
            this.mAppOpsManager.checkPackage(uid, packageName);
            return true;
        } catch (SecurityException e) {
            return false;
        }
    }
}
