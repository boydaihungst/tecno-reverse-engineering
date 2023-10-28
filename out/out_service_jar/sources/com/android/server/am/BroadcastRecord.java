package com.android.server.am;

import android.app.ActivityManagerInternal;
import android.app.BroadcastOptions;
import android.app.compat.CompatChanges;
import android.content.ComponentName;
import android.content.IIntentReceiver;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.IBinder;
import android.os.SystemClock;
import android.os.UserHandle;
import android.util.PrintWriterPrinter;
import android.util.Slog;
import android.util.SparseArray;
import android.util.TimeUtils;
import android.util.proto.ProtoOutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class BroadcastRecord extends Binder {
    static final int APP_RECEIVE = 1;
    static final int CALL_DONE_RECEIVE = 3;
    static final int CALL_IN_RECEIVE = 2;
    static final int DELIVERY_DELIVERED = 1;
    static final int DELIVERY_PENDING = 0;
    static final int DELIVERY_SKIPPED = 2;
    static final int DELIVERY_TIMEOUT = 3;
    static final int IDLE = 0;
    static final int WAITING_SERVICES = 4;
    static AtomicInteger sNextToken = new AtomicInteger(1);
    final boolean allowBackgroundActivityStarts;
    int anrCount;
    final int appOp;
    final ProcessRecord callerApp;
    final String callerFeatureId;
    final boolean callerInstantApp;
    final String callerPackage;
    final int callingPid;
    final int callingUid;
    ProcessRecord curApp;
    ComponentName curComponent;
    BroadcastFilter curFilter;
    ActivityInfo curReceiver;
    boolean deferred;
    final int[] delivery;
    long dispatchClockTime;
    long dispatchRealTime;
    long dispatchTime;
    final long[] duration;
    long enqueueClockTime;
    long enqueueRealTime;
    long enqueueTime;
    final String[] excludedPackages;
    final String[] excludedPermissions;
    long finishTime;
    final boolean initialSticky;
    final Intent intent;
    final IBinder mBackgroundActivityStartsToken;
    int manifestCount;
    int manifestSkipCount;
    int nextReceiver;
    final BroadcastOptions options;
    final boolean ordered;
    BroadcastQueue queue;
    IBinder receiver;
    long receiverTime;
    final List receivers;
    final String[] requiredPermissions;
    final String resolvedType;
    boolean resultAbort;
    int resultCode;
    String resultData;
    Bundle resultExtras;
    IIntentReceiver resultTo;
    int splitCount;
    int splitToken;
    int state;
    final boolean sticky;
    final ComponentName targetComp;
    boolean timeoutExempt;
    final int userId;

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(PrintWriter pw, String prefix, SimpleDateFormat sdf) {
        long now = SystemClock.uptimeMillis();
        pw.print(prefix);
        pw.print(this);
        pw.print(" to user ");
        pw.println(this.userId);
        pw.print(prefix);
        pw.println(this.intent.toInsecureString());
        ComponentName componentName = this.targetComp;
        if (componentName != null && componentName != this.intent.getComponent()) {
            pw.print(prefix);
            pw.print("  targetComp: ");
            pw.println(this.targetComp.toShortString());
        }
        Bundle bundle = this.intent.getExtras();
        if (bundle != null) {
            pw.print(prefix);
            pw.print("  extras: ");
            pw.println(bundle.toString());
        }
        pw.print(prefix);
        pw.print("caller=");
        pw.print(this.callerPackage);
        pw.print(" ");
        ProcessRecord processRecord = this.callerApp;
        pw.print(processRecord != null ? processRecord.toShortString() : "null");
        pw.print(" pid=");
        pw.print(this.callingPid);
        pw.print(" uid=");
        pw.println(this.callingUid);
        String[] strArr = this.requiredPermissions;
        if ((strArr != null && strArr.length > 0) || this.appOp != -1) {
            pw.print(prefix);
            pw.print("requiredPermissions=");
            pw.print(Arrays.toString(this.requiredPermissions));
            pw.print("  appOp=");
            pw.println(this.appOp);
        }
        String[] strArr2 = this.excludedPermissions;
        if (strArr2 != null && strArr2.length > 0) {
            pw.print(prefix);
            pw.print("excludedPermissions=");
            pw.print(Arrays.toString(this.excludedPermissions));
        }
        String[] strArr3 = this.excludedPackages;
        if (strArr3 != null && strArr3.length > 0) {
            pw.print(prefix);
            pw.print("excludedPackages=");
            pw.print(Arrays.toString(this.excludedPackages));
        }
        if (this.options != null) {
            pw.print(prefix);
            pw.print("options=");
            pw.println(this.options.toBundle());
        }
        pw.print(prefix);
        pw.print("enqueueClockTime=");
        pw.print(sdf.format(new Date(this.enqueueClockTime)));
        pw.print(" dispatchClockTime=");
        pw.println(sdf.format(new Date(this.dispatchClockTime)));
        pw.print(prefix);
        pw.print("dispatchTime=");
        TimeUtils.formatDuration(this.dispatchTime, now, pw);
        pw.print(" (");
        TimeUtils.formatDuration(this.dispatchTime - this.enqueueTime, pw);
        pw.print(" since enq)");
        if (this.finishTime != 0) {
            pw.print(" finishTime=");
            TimeUtils.formatDuration(this.finishTime, now, pw);
            pw.print(" (");
            TimeUtils.formatDuration(this.finishTime - this.dispatchTime, pw);
            pw.print(" since disp)");
        } else {
            pw.print(" receiverTime=");
            TimeUtils.formatDuration(this.receiverTime, now, pw);
        }
        pw.println("");
        if (this.anrCount != 0) {
            pw.print(prefix);
            pw.print("anrCount=");
            pw.println(this.anrCount);
        }
        if (this.resultTo != null || this.resultCode != -1 || this.resultData != null) {
            pw.print(prefix);
            pw.print("resultTo=");
            pw.print(this.resultTo);
            pw.print(" resultCode=");
            pw.print(this.resultCode);
            pw.print(" resultData=");
            pw.println(this.resultData);
        }
        if (this.resultExtras != null) {
            pw.print(prefix);
            pw.print("resultExtras=");
            pw.println(this.resultExtras);
        }
        if (this.resultAbort || this.ordered || this.sticky || this.initialSticky) {
            pw.print(prefix);
            pw.print("resultAbort=");
            pw.print(this.resultAbort);
            pw.print(" ordered=");
            pw.print(this.ordered);
            pw.print(" sticky=");
            pw.print(this.sticky);
            pw.print(" initialSticky=");
            pw.println(this.initialSticky);
        }
        if (this.nextReceiver != 0 || this.receiver != null) {
            pw.print(prefix);
            pw.print("nextReceiver=");
            pw.print(this.nextReceiver);
            pw.print(" receiver=");
            pw.println(this.receiver);
        }
        if (this.curFilter != null) {
            pw.print(prefix);
            pw.print("curFilter=");
            pw.println(this.curFilter);
        }
        if (this.curReceiver != null) {
            pw.print(prefix);
            pw.print("curReceiver=");
            pw.println(this.curReceiver);
        }
        if (this.curApp != null) {
            pw.print(prefix);
            pw.print("curApp=");
            pw.println(this.curApp);
            pw.print(prefix);
            pw.print("curComponent=");
            ComponentName componentName2 = this.curComponent;
            pw.println(componentName2 != null ? componentName2.toShortString() : "--");
            ActivityInfo activityInfo = this.curReceiver;
            if (activityInfo != null && activityInfo.applicationInfo != null) {
                pw.print(prefix);
                pw.print("curSourceDir=");
                pw.println(this.curReceiver.applicationInfo.sourceDir);
            }
        }
        int i = this.state;
        if (i != 0) {
            String stateStr = " (?)";
            switch (i) {
                case 1:
                    stateStr = " (APP_RECEIVE)";
                    break;
                case 2:
                    stateStr = " (CALL_IN_RECEIVE)";
                    break;
                case 3:
                    stateStr = " (CALL_DONE_RECEIVE)";
                    break;
                case 4:
                    stateStr = " (WAITING_SERVICES)";
                    break;
            }
            pw.print(prefix);
            pw.print("state=");
            pw.print(this.state);
            pw.println(stateStr);
        }
        List list = this.receivers;
        int N = list != null ? list.size() : 0;
        String p2 = prefix + "  ";
        PrintWriterPrinter printer = new PrintWriterPrinter(pw);
        if (Build.IS_DEBUG_ENABLE) {
            Slog.d("BroadcastRecord", "dumpLocked start N = " + N + ", intent: " + this.intent);
        }
        int i2 = 0;
        while (i2 < N) {
            Object o = this.receivers.get(i2);
            pw.print(prefix);
            switch (this.delivery[i2]) {
                case 0:
                    pw.print("Pending");
                    break;
                case 1:
                    pw.print("Deliver");
                    break;
                case 2:
                    pw.print("Skipped");
                    break;
                case 3:
                    pw.print("Timeout");
                    break;
                default:
                    pw.print("???????");
                    break;
            }
            pw.print(" ");
            long now2 = now;
            TimeUtils.formatDuration(this.duration[i2], pw);
            pw.print(" #");
            pw.print(i2);
            pw.print(": ");
            if (o instanceof BroadcastFilter) {
                pw.println(o);
                ((BroadcastFilter) o).dumpBrief(pw, p2);
            } else if (o instanceof ResolveInfo) {
                pw.println("(manifest)");
                ((ResolveInfo) o).dump(printer, p2, 0);
            } else {
                pw.println(o);
            }
            i2++;
            now = now2;
        }
        if (Build.IS_DEBUG_ENABLE) {
            Slog.d("BroadcastRecord", "dumpLocked end N = " + N + ", intent: " + this.intent);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public BroadcastRecord(BroadcastQueue _queue, Intent _intent, ProcessRecord _callerApp, String _callerPackage, String _callerFeatureId, int _callingPid, int _callingUid, boolean _callerInstantApp, String _resolvedType, String[] _requiredPermissions, String[] _excludedPermissions, String[] _excludedPackages, int _appOp, BroadcastOptions _options, List _receivers, IIntentReceiver _resultTo, int _resultCode, String _resultData, Bundle _resultExtras, boolean _serialized, boolean _sticky, boolean _initialSticky, int _userId, boolean allowBackgroundActivityStarts, IBinder backgroundActivityStartsToken, boolean timeoutExempt) {
        if (_intent == null) {
            throw new NullPointerException("Can't construct with a null intent");
        }
        this.queue = _queue;
        this.intent = _intent;
        this.targetComp = _intent.getComponent();
        this.callerApp = _callerApp;
        this.callerPackage = _callerPackage;
        this.callerFeatureId = _callerFeatureId;
        this.callingPid = _callingPid;
        this.callingUid = _callingUid;
        this.callerInstantApp = _callerInstantApp;
        this.resolvedType = _resolvedType;
        this.requiredPermissions = _requiredPermissions;
        this.excludedPermissions = _excludedPermissions;
        this.excludedPackages = _excludedPackages;
        this.appOp = _appOp;
        this.options = _options;
        this.receivers = _receivers;
        int[] iArr = new int[_receivers != null ? _receivers.size() : 0];
        this.delivery = iArr;
        this.duration = new long[iArr.length];
        this.resultTo = _resultTo;
        this.resultCode = _resultCode;
        this.resultData = _resultData;
        this.resultExtras = _resultExtras;
        this.ordered = _serialized;
        this.sticky = _sticky;
        this.initialSticky = _initialSticky;
        this.userId = _userId;
        this.nextReceiver = 0;
        this.state = 0;
        this.allowBackgroundActivityStarts = allowBackgroundActivityStarts;
        this.mBackgroundActivityStartsToken = backgroundActivityStartsToken;
        this.timeoutExempt = timeoutExempt;
    }

    private BroadcastRecord(BroadcastRecord from, Intent newIntent) {
        this.intent = newIntent;
        this.targetComp = newIntent.getComponent();
        this.callerApp = from.callerApp;
        this.callerPackage = from.callerPackage;
        this.callerFeatureId = from.callerFeatureId;
        this.callingPid = from.callingPid;
        this.callingUid = from.callingUid;
        this.callerInstantApp = from.callerInstantApp;
        this.ordered = from.ordered;
        this.sticky = from.sticky;
        this.initialSticky = from.initialSticky;
        this.userId = from.userId;
        this.resolvedType = from.resolvedType;
        this.requiredPermissions = from.requiredPermissions;
        this.excludedPermissions = from.excludedPermissions;
        this.excludedPackages = from.excludedPackages;
        this.appOp = from.appOp;
        this.options = from.options;
        this.receivers = from.receivers;
        this.delivery = from.delivery;
        this.duration = from.duration;
        this.resultTo = from.resultTo;
        this.enqueueTime = from.enqueueTime;
        this.enqueueRealTime = from.enqueueRealTime;
        this.enqueueClockTime = from.enqueueClockTime;
        this.dispatchTime = from.dispatchTime;
        this.dispatchRealTime = from.dispatchRealTime;
        this.dispatchClockTime = from.dispatchClockTime;
        this.receiverTime = from.receiverTime;
        this.finishTime = from.finishTime;
        this.resultCode = from.resultCode;
        this.resultData = from.resultData;
        this.resultExtras = from.resultExtras;
        this.resultAbort = from.resultAbort;
        this.nextReceiver = from.nextReceiver;
        this.receiver = from.receiver;
        this.state = from.state;
        this.anrCount = from.anrCount;
        this.manifestCount = from.manifestCount;
        this.manifestSkipCount = from.manifestSkipCount;
        this.queue = from.queue;
        this.allowBackgroundActivityStarts = from.allowBackgroundActivityStarts;
        this.mBackgroundActivityStartsToken = from.mBackgroundActivityStartsToken;
        this.timeoutExempt = from.timeoutExempt;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public BroadcastRecord splitRecipientsLocked(int slowAppUid, int startingAt) {
        ArrayList splitReceivers = null;
        int i = startingAt;
        while (i < this.receivers.size()) {
            Object o = this.receivers.get(i);
            if (getReceiverUid(o) == slowAppUid) {
                if (splitReceivers == null) {
                    splitReceivers = new ArrayList();
                }
                splitReceivers.add(o);
                if (Build.IS_DEBUG_ENABLE) {
                    Thread.currentThread();
                    boolean isHoldLock = Thread.holdsLock(this.queue.mService);
                    if (!isHoldLock) {
                        Slog.d("BroadcastRecord", "current thread don't hold amsllock ,this way is " + Debug.getCaller());
                    }
                }
                this.receivers.remove(i);
            } else {
                i++;
            }
        }
        if (splitReceivers == null) {
            return null;
        }
        BroadcastRecord split = new BroadcastRecord(this.queue, this.intent, this.callerApp, this.callerPackage, this.callerFeatureId, this.callingPid, this.callingUid, this.callerInstantApp, this.resolvedType, this.requiredPermissions, this.excludedPermissions, this.excludedPackages, this.appOp, this.options, splitReceivers, this.resultTo, this.resultCode, this.resultData, this.resultExtras, this.ordered, this.sticky, this.initialSticky, this.userId, this.allowBackgroundActivityStarts, this.mBackgroundActivityStartsToken, this.timeoutExempt);
        split.enqueueTime = this.enqueueTime;
        split.enqueueRealTime = this.enqueueRealTime;
        split.enqueueClockTime = this.enqueueClockTime;
        split.splitToken = this.splitToken;
        return split;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public SparseArray<BroadcastRecord> splitDeferredBootCompletedBroadcastLocked(ActivityManagerInternal activityManagerInternal, int deferType) {
        SparseArray<BroadcastRecord> ret = new SparseArray<>();
        if (deferType == 0) {
            return ret;
        }
        if (this.receivers == null) {
            return ret;
        }
        String action = this.intent.getAction();
        if (!"android.intent.action.LOCKED_BOOT_COMPLETED".equals(action) && !"android.intent.action.BOOT_COMPLETED".equals(action)) {
            return ret;
        }
        SparseArray<List<Object>> uid2receiverList = new SparseArray<>();
        for (int i = this.receivers.size() - 1; i >= 0; i--) {
            Object receiver = this.receivers.get(i);
            int uid = getReceiverUid(receiver);
            if (deferType != 1) {
                if ((deferType & 2) != 0 && activityManagerInternal.getRestrictionLevel(uid) < 50) {
                }
                if ((deferType & 4) != 0 && !CompatChanges.isChangeEnabled(203704822L, uid)) {
                }
            }
            if (Build.IS_DEBUG_ENABLE) {
                Thread.currentThread();
                boolean isHoldLock = Thread.holdsLock(this.queue.mService);
                if (!isHoldLock) {
                    Slog.d("BroadcastRecord", "current thread don't hold amsllock ,this way is " + Debug.getCaller());
                }
            }
            this.receivers.remove(i);
            List<Object> receiverList = uid2receiverList.get(uid);
            if (receiverList != null) {
                receiverList.add(0, receiver);
            } else {
                ArrayList<Object> splitReceivers = new ArrayList<>();
                splitReceivers.add(0, receiver);
                uid2receiverList.put(uid, splitReceivers);
            }
        }
        int uidSize = uid2receiverList.size();
        for (int i2 = 0; i2 < uidSize; i2++) {
            BroadcastRecord br = new BroadcastRecord(this.queue, this.intent, this.callerApp, this.callerPackage, this.callerFeatureId, this.callingPid, this.callingUid, this.callerInstantApp, this.resolvedType, this.requiredPermissions, this.excludedPermissions, this.excludedPackages, this.appOp, this.options, uid2receiverList.valueAt(i2), null, this.resultCode, this.resultData, this.resultExtras, this.ordered, this.sticky, this.initialSticky, this.userId, this.allowBackgroundActivityStarts, this.mBackgroundActivityStartsToken, this.timeoutExempt);
            br.enqueueTime = this.enqueueTime;
            br.enqueueRealTime = this.enqueueRealTime;
            br.enqueueClockTime = this.enqueueClockTime;
            ret.put(uid2receiverList.keyAt(i2), br);
        }
        return ret;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getReceiverUid(Object receiver) {
        if (receiver instanceof BroadcastFilter) {
            return ((BroadcastFilter) receiver).owningUid;
        }
        return ((ResolveInfo) receiver).activityInfo.applicationInfo.uid;
    }

    public BroadcastRecord maybeStripForHistory() {
        if (!this.intent.canStripForHistory()) {
            return this;
        }
        return new BroadcastRecord(this, this.intent.maybeStripForHistory());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean cleanupDisabledPackageReceiversLocked(String packageName, Set<String> filterByClasses, int userId, boolean doit) {
        List list = this.receivers;
        if (list == null) {
            return false;
        }
        boolean cleanupAllUsers = userId == -1;
        int i = this.userId;
        boolean sendToAllUsers = i == -1;
        if (i == userId || cleanupAllUsers || sendToAllUsers) {
            boolean didSomething = false;
            for (int i2 = list.size() - 1; i2 >= 0; i2--) {
                Object o = this.receivers.get(i2);
                if (o instanceof ResolveInfo) {
                    ActivityInfo info = ((ResolveInfo) o).activityInfo;
                    boolean sameComponent = packageName == null || (info.applicationInfo.packageName.equals(packageName) && (filterByClasses == null || filterByClasses.contains(info.name)));
                    if (sameComponent && (cleanupAllUsers || UserHandle.getUserId(info.applicationInfo.uid) == userId)) {
                        if (!doit) {
                            return true;
                        }
                        didSomething = true;
                        if (Build.IS_DEBUG_ENABLE) {
                            Thread.currentThread();
                            boolean isHoldLock = Thread.holdsLock(this.queue.mService);
                            if (!isHoldLock) {
                                Slog.d("BroadcastRecord", "current thread don't hold amsllock ,this way is " + Debug.getCaller());
                            }
                        }
                        this.receivers.remove(i2);
                        int i3 = this.nextReceiver;
                        if (i2 < i3) {
                            this.nextReceiver = i3 - 1;
                        }
                    }
                }
            }
            int i4 = this.nextReceiver;
            this.nextReceiver = Math.min(i4, this.receivers.size());
            return didSomething;
        }
        return false;
    }

    public String toString() {
        return "BroadcastRecord{" + Integer.toHexString(System.identityHashCode(this)) + " u" + this.userId + " " + this.intent.getAction() + "}";
    }

    public void dumpDebug(ProtoOutputStream proto, long fieldId) {
        long token = proto.start(fieldId);
        proto.write(CompanionMessage.MESSAGE_ID, this.userId);
        proto.write(1138166333442L, this.intent.getAction());
        proto.end(token);
    }
}
