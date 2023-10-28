package com.android.server.am;

import android.app.ActivityManagerInternal;
import android.app.ActivityOptions;
import android.app.BroadcastOptions;
import android.content.IIntentReceiver;
import android.content.IIntentSender;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerWhitelistManager;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.TransactionTooLargeException;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Slog;
import android.util.TimeUtils;
import com.android.internal.os.IResultReceiver;
import com.android.internal.util.function.pooled.PooledLambda;
import com.android.server.slice.SliceClientPermissions;
import com.android.server.wm.SafeActivityOptions;
import com.transsion.hubcore.server.am.ITranPendingIntentRecord;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.function.Consumer;
/* loaded from: classes.dex */
public final class PendingIntentRecord extends IIntentSender.Stub {
    public static final int FLAG_ACTIVITY_SENDER = 1;
    public static final int FLAG_BROADCAST_SENDER = 2;
    public static final int FLAG_SERVICE_SENDER = 4;
    private static final String TAG = "ActivityManager";
    final PendingIntentController controller;
    final Key key;
    String lastTag;
    String lastTagPrefix;
    private ArrayMap<IBinder, TempAllowListDuration> mAllowlistDuration;
    private RemoteCallbackList<IResultReceiver> mCancelCallbacks;
    String stringName;
    final int uid;
    boolean sent = false;
    boolean canceled = false;
    private ArraySet<IBinder> mAllowBgActivityStartsForActivitySender = new ArraySet<>();
    private ArraySet<IBinder> mAllowBgActivityStartsForBroadcastSender = new ArraySet<>();
    private ArraySet<IBinder> mAllowBgActivityStartsForServiceSender = new ArraySet<>();
    public final WeakReference<PendingIntentRecord> ref = new WeakReference<>(this);

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static final class Key {
        private static final int ODD_PRIME_NUMBER = 37;
        final IBinder activity;
        Intent[] allIntents;
        String[] allResolvedTypes;
        final String featureId;
        final int flags;
        final int hashCode;
        final SafeActivityOptions options;
        final String packageName;
        final int requestCode;
        final Intent requestIntent;
        final String requestResolvedType;
        final int type;
        final int userId;
        final String who;

        /* JADX INFO: Access modifiers changed from: package-private */
        public Key(int _t, String _p, String _featureId, IBinder _a, String _w, int _r, Intent[] _i, String[] _it, int _f, SafeActivityOptions _o, int _userId) {
            this.type = _t;
            this.packageName = _p;
            this.featureId = _featureId;
            this.activity = _a;
            this.who = _w;
            this.requestCode = _r;
            Intent intent = _i != null ? _i[_i.length - 1] : null;
            this.requestIntent = intent;
            String str = _it != null ? _it[_it.length - 1] : null;
            this.requestResolvedType = str;
            this.allIntents = _i;
            this.allResolvedTypes = _it;
            this.flags = _f;
            this.options = _o;
            this.userId = _userId;
            int hash = (((((23 * 37) + _f) * 37) + _r) * 37) + _userId;
            hash = _w != null ? (hash * 37) + _w.hashCode() : hash;
            hash = _a != null ? (hash * 37) + _a.hashCode() : hash;
            hash = intent != null ? (hash * 37) + intent.filterHashCode() : hash;
            this.hashCode = ((((str != null ? (hash * 37) + str.hashCode() : hash) * 37) + (_p != null ? _p.hashCode() : 0)) * 37) + _t;
        }

        public boolean equals(Object otherObj) {
            if (otherObj == null) {
                return false;
            }
            try {
                Key other = (Key) otherObj;
                if (this.type != other.type || this.userId != other.userId || !Objects.equals(this.packageName, other.packageName) || !Objects.equals(this.featureId, other.featureId) || this.activity != other.activity || !Objects.equals(this.who, other.who) || this.requestCode != other.requestCode) {
                    return false;
                }
                Intent intent = this.requestIntent;
                Intent intent2 = other.requestIntent;
                if (intent != intent2) {
                    if (intent != null) {
                        if (!intent.filterEquals(intent2)) {
                            return false;
                        }
                    } else if (intent2 != null) {
                        return false;
                    }
                }
                if (!Objects.equals(this.requestResolvedType, other.requestResolvedType)) {
                    return false;
                }
                if (this.flags != other.flags) {
                    return false;
                }
                return true;
            } catch (ClassCastException e) {
                return false;
            }
        }

        public int hashCode() {
            return this.hashCode;
        }

        public String toString() {
            StringBuilder append = new StringBuilder().append("Key{").append(typeName()).append(" pkg=").append(this.packageName).append(this.featureId != null ? SliceClientPermissions.SliceAuthority.DELIMITER + this.featureId : "").append(" intent=");
            Intent intent = this.requestIntent;
            return append.append(intent != null ? intent.toShortString(false, true, false, false) : "<null>").append(" flags=0x").append(Integer.toHexString(this.flags)).append(" u=").append(this.userId).append("} requestCode=").append(this.requestCode).toString();
        }

        String typeName() {
            int i = this.type;
            switch (i) {
                case 1:
                    return "broadcastIntent";
                case 2:
                    return "startActivity";
                case 3:
                    return "activityResult";
                case 4:
                    return "startService";
                case 5:
                    return "startForegroundService";
                default:
                    return Integer.toString(i);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static final class TempAllowListDuration {
        long duration;
        String reason;
        int reasonCode;
        int type;

        TempAllowListDuration(long _duration, int _type, int _reasonCode, String _reason) {
            this.duration = _duration;
            this.type = _type;
            this.reasonCode = _reasonCode;
            this.reason = _reason;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public PendingIntentRecord(PendingIntentController _controller, Key _k, int _u) {
        this.controller = _controller;
        this.key = _k;
        this.uid = _u;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setAllowlistDurationLocked(IBinder allowlistToken, long duration, int type, int reasonCode, String reason) {
        if (duration > 0) {
            if (this.mAllowlistDuration == null) {
                this.mAllowlistDuration = new ArrayMap<>();
            }
            this.mAllowlistDuration.put(allowlistToken, new TempAllowListDuration(duration, type, reasonCode, reason));
        } else {
            ArrayMap<IBinder, TempAllowListDuration> arrayMap = this.mAllowlistDuration;
            if (arrayMap != null) {
                arrayMap.remove(allowlistToken);
                if (this.mAllowlistDuration.size() <= 0) {
                    this.mAllowlistDuration = null;
                }
            }
        }
        this.stringName = null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setAllowBgActivityStarts(IBinder token, int flags) {
        if (token == null) {
            return;
        }
        if ((flags & 1) != 0) {
            this.mAllowBgActivityStartsForActivitySender.add(token);
        }
        if ((flags & 2) != 0) {
            this.mAllowBgActivityStartsForBroadcastSender.add(token);
        }
        if ((flags & 4) != 0) {
            this.mAllowBgActivityStartsForServiceSender.add(token);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clearAllowBgActivityStarts(IBinder token) {
        if (token == null) {
            return;
        }
        this.mAllowBgActivityStartsForActivitySender.remove(token);
        this.mAllowBgActivityStartsForBroadcastSender.remove(token);
        this.mAllowBgActivityStartsForServiceSender.remove(token);
    }

    public void registerCancelListenerLocked(IResultReceiver receiver) {
        if (this.mCancelCallbacks == null) {
            this.mCancelCallbacks = new RemoteCallbackList<>();
        }
        this.mCancelCallbacks.register(receiver);
    }

    public void unregisterCancelListenerLocked(IResultReceiver receiver) {
        RemoteCallbackList<IResultReceiver> remoteCallbackList = this.mCancelCallbacks;
        if (remoteCallbackList == null) {
            return;
        }
        remoteCallbackList.unregister(receiver);
        if (this.mCancelCallbacks.getRegisteredCallbackCount() <= 0) {
            this.mCancelCallbacks = null;
        }
    }

    public RemoteCallbackList<IResultReceiver> detachCancelListenersLocked() {
        RemoteCallbackList<IResultReceiver> listeners = this.mCancelCallbacks;
        this.mCancelCallbacks = null;
        return listeners;
    }

    public void send(int code, Intent intent, String resolvedType, IBinder allowlistToken, IIntentReceiver finishedReceiver, String requiredPermission, Bundle options) {
        sendInner(code, intent, resolvedType, allowlistToken, finishedReceiver, requiredPermission, null, null, 0, 0, 0, options);
    }

    public int sendWithResult(int code, Intent intent, String resolvedType, IBinder allowlistToken, IIntentReceiver finishedReceiver, String requiredPermission, Bundle options) {
        return sendInner(code, intent, resolvedType, allowlistToken, finishedReceiver, requiredPermission, null, null, 0, 0, 0, options);
    }

    public static boolean isPendingIntentBalAllowedByPermission(ActivityOptions activityOptions) {
        if (activityOptions == null) {
            return false;
        }
        return activityOptions.isPendingIntentBackgroundActivityLaunchAllowedByPermission();
    }

    public static boolean isPendingIntentBalAllowedByCaller(ActivityOptions activityOptions) {
        if (activityOptions == null) {
            return true;
        }
        return isPendingIntentBalAllowedByCaller(activityOptions.toBundle());
    }

    private static boolean isPendingIntentBalAllowedByCaller(Bundle options) {
        if (options == null) {
            return true;
        }
        return options.getBoolean("android.pendingIntent.backgroundActivityAllowed", true);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [555=9, 421=7] */
    /* JADX DEBUG: Failed to insert an additional move for type inference into block B:175:0x0390 */
    /* JADX DEBUG: Failed to insert an additional move for type inference into block B:269:0x030c */
    /* JADX DEBUG: Failed to insert an additional move for type inference into block B:292:0x026f */
    /* JADX DEBUG: Multi-variable search result rejected for r22v0, resolved type: int */
    /* JADX WARN: Can't wrap try/catch for region: R(21:49|50|51|(3:(3:228|229|(21:233|(1:235)|236|237|54|55|56|57|(5:59|(1:61)(1:208)|62|(1:64)(2:202|(1:204)(2:205|(1:207)))|65)(3:209|210|(2:213|(1:215)))|(1:67)(1:201)|68|69|70|(2:72|73)(1:200)|74|(1:199)(1:80)|81|82|83|84|85))|84|85)|53|54|55|56|57|(0)(0)|(0)(0)|68|69|70|(0)(0)|74|(1:76)|199|81|82|83) */
    /* JADX WARN: Code restructure failed: missing block: B:105:0x01f3, code lost:
        r0 = th;
     */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Removed duplicated region for block: B:116:0x0237  */
    /* JADX WARN: Removed duplicated region for block: B:117:0x0239  */
    /* JADX WARN: Removed duplicated region for block: B:122:0x0243 A[Catch: all -> 0x01f3, TRY_ENTER, TRY_LEAVE, TryCatch #11 {all -> 0x01f3, blocks: (B:90:0x017e, B:94:0x0194, B:96:0x01aa, B:103:0x01d1, B:122:0x0243, B:127:0x0255, B:129:0x025f, B:138:0x027a, B:142:0x0287, B:146:0x0298, B:152:0x02aa, B:158:0x02df, B:157:0x02d0, B:97:0x01b2, B:99:0x01b8, B:100:0x01c0, B:102:0x01c6, B:93:0x0192, B:110:0x0203, B:112:0x0212), top: B:275:0x017c, inners: #32 }] */
    /* JADX WARN: Removed duplicated region for block: B:124:0x024f  */
    /* JADX WARN: Removed duplicated region for block: B:137:0x0272  */
    /* JADX WARN: Removed duplicated region for block: B:158:0x02df A[Catch: all -> 0x01f3, TRY_LEAVE, TryCatch #11 {all -> 0x01f3, blocks: (B:90:0x017e, B:94:0x0194, B:96:0x01aa, B:103:0x01d1, B:122:0x0243, B:127:0x0255, B:129:0x025f, B:138:0x027a, B:142:0x0287, B:146:0x0298, B:152:0x02aa, B:158:0x02df, B:157:0x02d0, B:97:0x01b2, B:99:0x01b8, B:100:0x01c0, B:102:0x01c6, B:93:0x0192, B:110:0x0203, B:112:0x0212), top: B:275:0x017c, inners: #32 }] */
    /* JADX WARN: Removed duplicated region for block: B:184:0x03ac  */
    /* JADX WARN: Removed duplicated region for block: B:201:0x0406  */
    /* JADX WARN: Removed duplicated region for block: B:210:0x041e  */
    /* JADX WARN: Removed duplicated region for block: B:280:0x0308 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:297:0x01fb A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:299:0x027a A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:90:0x017e A[Catch: all -> 0x01f3, TRY_ENTER, TryCatch #11 {all -> 0x01f3, blocks: (B:90:0x017e, B:94:0x0194, B:96:0x01aa, B:103:0x01d1, B:122:0x0243, B:127:0x0255, B:129:0x025f, B:138:0x027a, B:142:0x0287, B:146:0x0298, B:152:0x02aa, B:158:0x02df, B:157:0x02d0, B:97:0x01b2, B:99:0x01b8, B:100:0x01c0, B:102:0x01c6, B:93:0x0192, B:110:0x0203, B:112:0x0212), top: B:275:0x017c, inners: #32 }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public int sendInner(int code, Intent intent, String resolvedType, IBinder allowlistToken, IIntentReceiver finishedReceiver, String requiredPermission, IBinder resultTo, String resultWho, int requestCode, int flagsMask, int flagsValues, Bundle options) {
        String resolvedType2;
        SafeActivityOptions mergedOptions;
        TempAllowListDuration duration;
        Intent[] allIntents;
        String[] allResolvedTypes;
        long origId;
        Intent finalIntent;
        Intent finalIntent2;
        Intent finalIntent3;
        Intent finalIntent4;
        int res;
        boolean allowedByToken;
        IBinder bgStartsToken;
        ActivityManagerInternal activityManagerInternal;
        int i;
        boolean z;
        String str;
        String str2;
        boolean z2;
        int res2;
        boolean allowedByToken2;
        IBinder bgStartsToken2;
        ActivityManagerInternal activityManagerInternal2;
        String str3;
        String str4;
        int i2;
        boolean z3;
        boolean z4;
        int sent;
        String resolvedType3;
        if (intent != null) {
            intent.setDefusable(true);
        }
        if (options != null) {
            options.setDefusable(true);
        }
        synchronized (this.controller.mLock) {
            try {
                try {
                    if (this.canceled) {
                        return -96;
                    }
                    this.sent = true;
                    if ((this.key.flags & 1073741824) != 0) {
                        this.controller.cancelIntentSender(this, true);
                    }
                    Intent finalIntent5 = this.key.requestIntent != null ? new Intent(this.key.requestIntent) : new Intent();
                    try {
                        boolean immutable = (this.key.flags & 67108864) != 0;
                        if (immutable) {
                            try {
                                String resolvedType4 = this.key.requestResolvedType;
                                resolvedType2 = resolvedType4;
                            } catch (Throwable th) {
                                th = th;
                                while (true) {
                                    break;
                                    break;
                                }
                                throw th;
                            }
                        } else {
                            if (intent != null) {
                                try {
                                    int changes = finalIntent5.fillIn(intent, this.key.flags);
                                    resolvedType3 = (changes & 2) == 0 ? this.key.requestResolvedType : resolvedType;
                                } catch (Throwable th2) {
                                    th = th2;
                                    while (true) {
                                        try {
                                            break;
                                        } catch (Throwable th3) {
                                            th = th3;
                                        }
                                    }
                                    throw th;
                                }
                            } else {
                                try {
                                    resolvedType3 = this.key.requestResolvedType;
                                } catch (Throwable th4) {
                                    th = th4;
                                    while (true) {
                                        break;
                                        break;
                                    }
                                    throw th;
                                }
                            }
                            int flagsMask2 = flagsMask & (-196);
                            try {
                                finalIntent5.setFlags((finalIntent5.getFlags() & (~flagsMask2)) | (flagsValues & flagsMask2));
                                resolvedType2 = resolvedType3;
                            } catch (Throwable th5) {
                                th = th5;
                                while (true) {
                                    break;
                                    break;
                                }
                                throw th;
                            }
                        }
                        try {
                            ITranPendingIntentRecord.Instance().initMultiWindowFromOption(TAG, this.key.packageName, options);
                            ActivityOptions opts = ActivityOptions.fromBundle(options);
                            if (opts != null) {
                                try {
                                    finalIntent5.addFlags(opts.getPendingIntentLaunchFlags());
                                } catch (Throwable th6) {
                                    th = th6;
                                    while (true) {
                                        break;
                                        break;
                                    }
                                    throw th;
                                }
                            }
                            SafeActivityOptions mergedOptions2 = this.key.options;
                            if (mergedOptions2 == null) {
                                mergedOptions = new SafeActivityOptions(opts);
                            } else {
                                mergedOptions2.setCallerOptions(opts);
                                mergedOptions = mergedOptions2;
                            }
                            try {
                                ArrayMap<IBinder, TempAllowListDuration> arrayMap = this.mAllowlistDuration;
                                if (arrayMap != null) {
                                    try {
                                        TempAllowListDuration duration2 = arrayMap.get(allowlistToken);
                                        duration = duration2;
                                    } catch (Throwable th7) {
                                        th = th7;
                                        while (true) {
                                            break;
                                            break;
                                        }
                                        throw th;
                                    }
                                } else {
                                    duration = null;
                                }
                                try {
                                    try {
                                        try {
                                            if (this.key.type == 2) {
                                                try {
                                                    if (this.key.allIntents != null && this.key.allIntents.length > 1) {
                                                        Intent[] allIntents2 = new Intent[this.key.allIntents.length];
                                                        String[] allResolvedTypes2 = new String[this.key.allIntents.length];
                                                        System.arraycopy(this.key.allIntents, 0, allIntents2, 0, this.key.allIntents.length);
                                                        if (this.key.allResolvedTypes != null) {
                                                            System.arraycopy(this.key.allResolvedTypes, 0, allResolvedTypes2, 0, this.key.allResolvedTypes.length);
                                                        }
                                                        allIntents2[allIntents2.length - 1] = finalIntent5;
                                                        allResolvedTypes2[allResolvedTypes2.length - 1] = resolvedType2;
                                                        allIntents = allIntents2;
                                                        allResolvedTypes = allResolvedTypes2;
                                                        int callingUid = Binder.getCallingUid();
                                                        int callingPid = Binder.getCallingPid();
                                                        origId = Binder.clearCallingIdentity();
                                                        if (duration == null) {
                                                            StringBuilder tag = new StringBuilder(64);
                                                            tag.append("setPendingIntentAllowlistDuration,reason:");
                                                            tag.append(duration.reason == null ? "" : duration.reason);
                                                            tag.append(",pendingintent:");
                                                            UserHandle.formatUid(tag, callingUid);
                                                            tag.append(":");
                                                            if (finalIntent5.getAction() != null) {
                                                                tag.append(finalIntent5.getAction());
                                                            } else if (finalIntent5.getComponent() != null) {
                                                                finalIntent5.getComponent().appendShortString(tag);
                                                            } else if (finalIntent5.getData() != null) {
                                                                tag.append(finalIntent5.getData().toSafeString());
                                                            }
                                                            ActivityManagerInternal activityManagerInternal3 = this.controller.mAmInternal;
                                                            int i3 = this.uid;
                                                            long j = duration.duration;
                                                            int i4 = duration.type;
                                                            activityManagerInternal3.tempAllowlistForPendingIntent(callingPid, callingUid, i3, j, i4, duration.reasonCode, tag.toString());
                                                            finalIntent = i4;
                                                        } else {
                                                            try {
                                                                if (this.key.type == 5 && options != null) {
                                                                    BroadcastOptions brOptions = new BroadcastOptions(options);
                                                                    if (brOptions.getTemporaryAppAllowlistDuration() > 0) {
                                                                        ActivityManagerInternal activityManagerInternal4 = this.controller.mAmInternal;
                                                                        int i5 = this.uid;
                                                                        long temporaryAppAllowlistDuration = brOptions.getTemporaryAppAllowlistDuration();
                                                                        int temporaryAppAllowlistType = brOptions.getTemporaryAppAllowlistType();
                                                                        activityManagerInternal4.tempAllowlistForPendingIntent(callingPid, callingUid, i5, temporaryAppAllowlistDuration, temporaryAppAllowlistType, brOptions.getTemporaryAppAllowlistReasonCode(), brOptions.getTemporaryAppAllowlistReason());
                                                                        finalIntent = temporaryAppAllowlistType;
                                                                    }
                                                                }
                                                            } catch (Throwable th8) {
                                                                th = th8;
                                                                Binder.restoreCallingIdentity(origId);
                                                                throw th;
                                                            }
                                                        }
                                                        boolean sendFinish = finishedReceiver == null;
                                                        int userId = this.key.userId;
                                                        int userId2 = userId != -2 ? this.controller.mUserController.getCurrentOrTargetUserId() : userId;
                                                        boolean allowTrampoline = this.uid == callingUid && this.controller.mAtmInternal.isUidForeground(callingUid) && isPendingIntentBalAllowedByCaller(options);
                                                        switch (this.key.type) {
                                                            case 1:
                                                                finalIntent = finalIntent5;
                                                                try {
                                                                    allowedByToken2 = this.mAllowBgActivityStartsForBroadcastSender.contains(allowlistToken);
                                                                    bgStartsToken2 = allowedByToken2 ? allowlistToken : null;
                                                                    activityManagerInternal2 = this.controller.mAmInternal;
                                                                    str3 = this.key.packageName;
                                                                    str4 = this.key.featureId;
                                                                    i2 = this.uid;
                                                                    z3 = finishedReceiver != null;
                                                                } catch (RuntimeException e) {
                                                                    Slog.w(TAG, "Unable to send startActivity intent", e);
                                                                    finalIntent3 = finalIntent;
                                                                    break;
                                                                }
                                                                if (!allowedByToken2 && !allowTrampoline) {
                                                                    z4 = false;
                                                                    sent = activityManagerInternal2.broadcastIntentInPackage(str3, str4, i2, callingUid, callingPid, finalIntent, resolvedType2, finishedReceiver, code, (String) null, (Bundle) null, requiredPermission, options, z3, false, userId2, z4, bgStartsToken2, (int[]) null);
                                                                    if (sent == 0) {
                                                                        sendFinish = false;
                                                                    }
                                                                    res = 0;
                                                                    finalIntent4 = finalIntent;
                                                                    if (!sendFinish && res != -96) {
                                                                        try {
                                                                            try {
                                                                                try {
                                                                                    finishedReceiver.performReceive(new Intent(finalIntent4), 0, (String) null, (Bundle) null, false, false, this.key.userId);
                                                                                } catch (RemoteException e2) {
                                                                                } catch (Throwable th9) {
                                                                                    th = th9;
                                                                                    Binder.restoreCallingIdentity(origId);
                                                                                    throw th;
                                                                                }
                                                                            } catch (RemoteException e3) {
                                                                            } catch (Throwable th10) {
                                                                                th = th10;
                                                                            }
                                                                        } catch (RemoteException e4) {
                                                                        } catch (Throwable th11) {
                                                                            th = th11;
                                                                        }
                                                                    }
                                                                    Binder.restoreCallingIdentity(origId);
                                                                    return res;
                                                                }
                                                                z4 = true;
                                                                sent = activityManagerInternal2.broadcastIntentInPackage(str3, str4, i2, callingUid, callingPid, finalIntent, resolvedType2, finishedReceiver, code, (String) null, (Bundle) null, requiredPermission, options, z3, false, userId2, z4, bgStartsToken2, (int[]) null);
                                                                if (sent == 0) {
                                                                }
                                                                res = 0;
                                                                finalIntent4 = finalIntent;
                                                                if (!sendFinish) {
                                                                }
                                                                Binder.restoreCallingIdentity(origId);
                                                                return res;
                                                            case 2:
                                                                try {
                                                                    try {
                                                                        try {
                                                                        } catch (RuntimeException e5) {
                                                                            e = e5;
                                                                            Slog.w(TAG, "Unable to send startActivity intent", e);
                                                                            finalIntent3 = finalIntent;
                                                                            res = 0;
                                                                            finalIntent4 = finalIntent3;
                                                                            if (!sendFinish) {
                                                                            }
                                                                            Binder.restoreCallingIdentity(origId);
                                                                            return res;
                                                                        }
                                                                    } catch (RuntimeException e6) {
                                                                        e = e6;
                                                                        finalIntent = finalIntent5;
                                                                    }
                                                                    if (this.key.allIntents != null) {
                                                                        try {
                                                                            if (this.key.allIntents.length > 1) {
                                                                                finalIntent = finalIntent5;
                                                                                try {
                                                                                    res2 = this.controller.mAtmInternal.startActivitiesInPackage(this.uid, callingPid, callingUid, this.key.packageName, this.key.featureId, allIntents, allResolvedTypes, resultTo, mergedOptions, userId2, false, this, this.mAllowBgActivityStartsForActivitySender.contains(allowlistToken));
                                                                                    res = res2;
                                                                                    finalIntent4 = finalIntent;
                                                                                    if (!sendFinish) {
                                                                                    }
                                                                                    Binder.restoreCallingIdentity(origId);
                                                                                    return res;
                                                                                } catch (Throwable th12) {
                                                                                    th = th12;
                                                                                    finalIntent2 = finalIntent;
                                                                                    Binder.restoreCallingIdentity(origId);
                                                                                    throw th;
                                                                                }
                                                                            }
                                                                        } catch (Throwable th13) {
                                                                            th = th13;
                                                                            Binder.restoreCallingIdentity(origId);
                                                                            throw th;
                                                                        }
                                                                    }
                                                                    finalIntent = finalIntent5;
                                                                    res2 = this.controller.mAtmInternal.startActivityInPackage(this.uid, callingPid, callingUid, this.key.packageName, this.key.featureId, finalIntent, resolvedType2, resultTo, resultWho, requestCode, 0, mergedOptions, userId2, null, "PendingIntentRecord", false, this, this.mAllowBgActivityStartsForActivitySender.contains(allowlistToken));
                                                                    res = res2;
                                                                    finalIntent4 = finalIntent;
                                                                    if (!sendFinish) {
                                                                    }
                                                                    Binder.restoreCallingIdentity(origId);
                                                                    return res;
                                                                } catch (Throwable th14) {
                                                                    th = th14;
                                                                }
                                                                break;
                                                            case 3:
                                                                this.controller.mAtmInternal.sendActivityResult(-1, this.key.activity, this.key.who, this.key.requestCode, code, finalIntent5);
                                                                finalIntent3 = finalIntent5;
                                                                res = 0;
                                                                finalIntent4 = finalIntent3;
                                                                if (!sendFinish) {
                                                                }
                                                                Binder.restoreCallingIdentity(origId);
                                                                return res;
                                                            case 4:
                                                            case 5:
                                                                try {
                                                                    allowedByToken = this.mAllowBgActivityStartsForServiceSender.contains(allowlistToken);
                                                                    bgStartsToken = allowedByToken ? allowlistToken : null;
                                                                    activityManagerInternal = this.controller.mAmInternal;
                                                                    i = this.uid;
                                                                    z = this.key.type == 5;
                                                                    str = this.key.packageName;
                                                                    str2 = this.key.featureId;
                                                                } catch (TransactionTooLargeException e7) {
                                                                    finalIntent4 = finalIntent5;
                                                                    res = -96;
                                                                } catch (RuntimeException e8) {
                                                                    Slog.w(TAG, "Unable to send startService intent", e8);
                                                                    finalIntent3 = finalIntent5;
                                                                }
                                                                if (!allowedByToken && !allowTrampoline) {
                                                                    z2 = false;
                                                                    activityManagerInternal.startServiceInPackage(i, finalIntent5, resolvedType2, z, str, str2, userId2, z2, bgStartsToken);
                                                                    finalIntent3 = finalIntent5;
                                                                    res = 0;
                                                                    finalIntent4 = finalIntent3;
                                                                    if (!sendFinish) {
                                                                    }
                                                                    Binder.restoreCallingIdentity(origId);
                                                                    return res;
                                                                }
                                                                z2 = true;
                                                                activityManagerInternal.startServiceInPackage(i, finalIntent5, resolvedType2, z, str, str2, userId2, z2, bgStartsToken);
                                                                finalIntent3 = finalIntent5;
                                                                res = 0;
                                                                finalIntent4 = finalIntent3;
                                                                if (!sendFinish) {
                                                                }
                                                                Binder.restoreCallingIdentity(origId);
                                                                return res;
                                                            default:
                                                                finalIntent3 = finalIntent5;
                                                                res = 0;
                                                                finalIntent4 = finalIntent3;
                                                                if (!sendFinish) {
                                                                }
                                                                Binder.restoreCallingIdentity(origId);
                                                                return res;
                                                        }
                                                    }
                                                } catch (Throwable th15) {
                                                    th = th15;
                                                    while (true) {
                                                        break;
                                                        break;
                                                    }
                                                    throw th;
                                                }
                                            }
                                            switch (this.key.type) {
                                            }
                                        } catch (Throwable th16) {
                                            th = th16;
                                            finalIntent2 = finalIntent;
                                            Binder.restoreCallingIdentity(origId);
                                            throw th;
                                        }
                                        int callingUid2 = Binder.getCallingUid();
                                        int callingPid2 = Binder.getCallingPid();
                                        origId = Binder.clearCallingIdentity();
                                        if (duration == null) {
                                        }
                                        boolean sendFinish2 = finishedReceiver == null;
                                        int userId3 = this.key.userId;
                                        if (userId3 != -2) {
                                        }
                                        boolean allowTrampoline2 = this.uid == callingUid2 && this.controller.mAtmInternal.isUidForeground(callingUid2) && isPendingIntentBalAllowedByCaller(options);
                                    } catch (Throwable th17) {
                                        th = th17;
                                        while (true) {
                                            break;
                                            break;
                                        }
                                        throw th;
                                    }
                                    allIntents = null;
                                    allResolvedTypes = null;
                                } catch (Throwable th18) {
                                    th = th18;
                                }
                            } catch (Throwable th19) {
                                th = th19;
                            }
                        } catch (Throwable th20) {
                            th = th20;
                        }
                    } catch (Throwable th21) {
                        th = th21;
                    }
                } catch (Throwable th22) {
                    th = th22;
                }
            } catch (Throwable th23) {
                th = th23;
            }
        }
    }

    protected void finalize() throws Throwable {
        try {
            if (!this.canceled) {
                this.controller.mH.sendMessage(PooledLambda.obtainMessage(new Consumer() { // from class: com.android.server.am.PendingIntentRecord$$ExternalSyntheticLambda0
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        ((PendingIntentRecord) obj).completeFinalize();
                    }
                }, this));
            }
        } finally {
            super/*java.lang.Object*/.finalize();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void completeFinalize() {
        synchronized (this.controller.mLock) {
            WeakReference<PendingIntentRecord> current = this.controller.mIntentSenderRecords.get(this.key);
            if (current == this.ref) {
                this.controller.mIntentSenderRecords.remove(this.key);
                this.controller.decrementUidStatLocked(this);
            }
        }
    }

    public void dump(PrintWriter pw, String prefix) {
        pw.print(prefix);
        pw.print("uid=");
        pw.print(this.uid);
        pw.print(" packageName=");
        pw.print(this.key.packageName);
        pw.print(" featureId=");
        pw.print(this.key.featureId);
        pw.print(" type=");
        pw.print(this.key.typeName());
        pw.print(" flags=0x");
        pw.println(Integer.toHexString(this.key.flags));
        if (this.key.activity != null || this.key.who != null) {
            pw.print(prefix);
            pw.print("activity=");
            pw.print(this.key.activity);
            pw.print(" who=");
            pw.println(this.key.who);
        }
        if (this.key.requestCode != 0 || this.key.requestResolvedType != null) {
            pw.print(prefix);
            pw.print("requestCode=");
            pw.print(this.key.requestCode);
            pw.print(" requestResolvedType=");
            pw.println(this.key.requestResolvedType);
        }
        if (this.key.requestIntent != null) {
            pw.print(prefix);
            pw.print("requestIntent=");
            pw.println(this.key.requestIntent.toShortString(false, true, true, false));
        }
        if (this.sent || this.canceled) {
            pw.print(prefix);
            pw.print("sent=");
            pw.print(this.sent);
            pw.print(" canceled=");
            pw.println(this.canceled);
        }
        if (this.mAllowlistDuration != null) {
            pw.print(prefix);
            pw.print("allowlistDuration=");
            for (int i = 0; i < this.mAllowlistDuration.size(); i++) {
                if (i != 0) {
                    pw.print(", ");
                }
                TempAllowListDuration entry = this.mAllowlistDuration.valueAt(i);
                pw.print(Integer.toHexString(System.identityHashCode(this.mAllowlistDuration.keyAt(i))));
                pw.print(":");
                TimeUtils.formatDuration(entry.duration, pw);
                pw.print(SliceClientPermissions.SliceAuthority.DELIMITER);
                pw.print(entry.type);
                pw.print(SliceClientPermissions.SliceAuthority.DELIMITER);
                pw.print(PowerWhitelistManager.reasonCodeToString(entry.reasonCode));
                pw.print(SliceClientPermissions.SliceAuthority.DELIMITER);
                pw.print(entry.reason);
            }
            pw.println();
        }
        if (this.mCancelCallbacks != null) {
            pw.print(prefix);
            pw.println("mCancelCallbacks:");
            for (int i2 = 0; i2 < this.mCancelCallbacks.getRegisteredCallbackCount(); i2++) {
                pw.print(prefix);
                pw.print("  #");
                pw.print(i2);
                pw.print(": ");
                pw.println(this.mCancelCallbacks.getRegisteredCallbackItem(i2));
            }
        }
    }

    public String toString() {
        String str = this.stringName;
        if (str != null) {
            return str;
        }
        StringBuilder sb = new StringBuilder(128);
        sb.append("PendingIntentRecord{");
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(' ');
        sb.append(this.key.packageName);
        if (this.key.featureId != null) {
            sb.append('/');
            sb.append(this.key.featureId);
        }
        sb.append(' ');
        sb.append(this.key.typeName());
        if (this.mAllowlistDuration != null) {
            sb.append(" (allowlist: ");
            for (int i = 0; i < this.mAllowlistDuration.size(); i++) {
                if (i != 0) {
                    sb.append(",");
                }
                TempAllowListDuration entry = this.mAllowlistDuration.valueAt(i);
                sb.append(Integer.toHexString(System.identityHashCode(this.mAllowlistDuration.keyAt(i))));
                sb.append(":");
                TimeUtils.formatDuration(entry.duration, sb);
                sb.append(SliceClientPermissions.SliceAuthority.DELIMITER);
                sb.append(entry.type);
                sb.append(SliceClientPermissions.SliceAuthority.DELIMITER);
                sb.append(PowerWhitelistManager.reasonCodeToString(entry.reasonCode));
                sb.append(SliceClientPermissions.SliceAuthority.DELIMITER);
                sb.append(entry.reason);
            }
            sb.append(")");
        }
        sb.append('}');
        String sb2 = sb.toString();
        this.stringName = sb2;
        return sb2;
    }
}
