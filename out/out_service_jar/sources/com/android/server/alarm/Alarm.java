package com.android.server.alarm;

import android.app.AlarmManager;
import android.app.IAlarmListener;
import android.app.PendingIntent;
import android.os.Bundle;
import android.os.WorkSource;
import android.util.IndentingPrintWriter;
import android.util.TimeUtils;
import android.util.proto.ProtoOutputStream;
import com.android.server.alarm.AlarmManagerService;
import com.android.server.pm.pkg.parsing.ParsingPackageUtils;
import defpackage.CompanionAppsPermissions;
import java.text.SimpleDateFormat;
import java.util.Date;
/* loaded from: classes.dex */
public class Alarm {
    public static final int APP_STANDBY_POLICY_INDEX = 1;
    public static final int BATTERY_SAVER_POLICY_INDEX = 3;
    public static final int DEVICE_IDLE_POLICY_INDEX = 2;
    static final int EXACT_ALLOW_REASON_ALLOW_LIST = 1;
    static final int EXACT_ALLOW_REASON_COMPAT = 2;
    static final int EXACT_ALLOW_REASON_NOT_APPLICABLE = -1;
    static final int EXACT_ALLOW_REASON_PERMISSION = 0;
    static final int EXACT_ALLOW_REASON_POLICY_PERMISSION = 3;
    public static final int NUM_POLICIES = 5;
    public static final int REQUESTER_POLICY_INDEX = 0;
    public static final int TARE_POLICY_INDEX = 4;
    public final AlarmManager.AlarmClockInfo alarmClock;
    public int count;
    public final int creatorUid;
    public final int flags;
    public final IAlarmListener listener;
    public final String listenerTag;
    public int mExactAllowReason;
    public Bundle mIdleOptions;
    private long mMaxWhenElapsed;
    private long[] mPolicyWhenElapsed;
    public boolean mUsingReserveQuota;
    private long mWhenElapsed;
    public final PendingIntent operation;
    public final long origWhen;
    public final String packageName;
    public AlarmManagerService.PriorityClass priorityClass;
    public final long repeatInterval;
    public final String sourcePackage;
    public final String statsTag;
    public final int type;
    public final int uid;
    public final boolean wakeup;
    public final long windowLength;
    public final WorkSource workSource;

    public Alarm(int type, long when, long requestedWhenElapsed, long windowLength, long interval, PendingIntent op, IAlarmListener rec, String listenerTag, WorkSource ws, int flags, AlarmManager.AlarmClockInfo info, int uid, String pkgName, Bundle idleOptions, int exactAllowReason) {
        this.type = type;
        this.origWhen = when;
        this.wakeup = type == 2 || type == 0;
        long[] jArr = new long[5];
        this.mPolicyWhenElapsed = jArr;
        jArr[0] = requestedWhenElapsed;
        this.mWhenElapsed = requestedWhenElapsed;
        this.windowLength = windowLength;
        this.mMaxWhenElapsed = AlarmManagerService.clampPositive(requestedWhenElapsed + windowLength);
        this.repeatInterval = interval;
        this.operation = op;
        this.listener = rec;
        this.listenerTag = listenerTag;
        this.statsTag = makeTag(op, listenerTag, type);
        this.workSource = ws;
        this.flags = flags;
        this.alarmClock = info;
        this.uid = uid;
        this.packageName = pkgName;
        this.mIdleOptions = idleOptions;
        this.mExactAllowReason = exactAllowReason;
        this.sourcePackage = op != null ? op.getCreatorPackage() : pkgName;
        this.creatorUid = op != null ? op.getCreatorUid() : uid;
        this.mUsingReserveQuota = false;
    }

    public static String makeTag(PendingIntent pi, String tag, int type) {
        String alarmString = (type == 2 || type == 0) ? "*walarm*:" : "*alarm*:";
        return pi != null ? pi.getTag(alarmString) : alarmString + tag;
    }

    public boolean matches(PendingIntent pi, IAlarmListener rec) {
        PendingIntent pendingIntent = this.operation;
        if (pendingIntent != null) {
            return pendingIntent.equals(pi);
        }
        return rec != null && this.listener.asBinder().equals(rec.asBinder());
    }

    public boolean matches(String packageName) {
        return packageName.equals(this.sourcePackage);
    }

    long getPolicyElapsed(int policyIndex) {
        return this.mPolicyWhenElapsed[policyIndex];
    }

    public long getRequestedElapsed() {
        return this.mPolicyWhenElapsed[0];
    }

    public long getWhenElapsed() {
        return this.mWhenElapsed;
    }

    public long getMaxWhenElapsed() {
        return this.mMaxWhenElapsed;
    }

    public boolean setPolicyElapsed(int policyIndex, long policyElapsed) {
        this.mPolicyWhenElapsed[policyIndex] = policyElapsed;
        return updateWhenElapsed();
    }

    private boolean updateWhenElapsed() {
        long oldWhenElapsed = this.mWhenElapsed;
        this.mWhenElapsed = 0L;
        for (int i = 0; i < 5; i++) {
            this.mWhenElapsed = Math.max(this.mWhenElapsed, this.mPolicyWhenElapsed[i]);
        }
        long oldMaxWhenElapsed = this.mMaxWhenElapsed;
        long maxRequestedElapsed = AlarmManagerService.clampPositive(this.mPolicyWhenElapsed[0] + this.windowLength);
        long max = Math.max(maxRequestedElapsed, this.mWhenElapsed);
        this.mMaxWhenElapsed = max;
        return (oldWhenElapsed == this.mWhenElapsed && oldMaxWhenElapsed == max) ? false : true;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append("Alarm{");
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(" type ");
        sb.append(this.type);
        sb.append(" origWhen ");
        sb.append(this.origWhen);
        sb.append(" whenElapsed ");
        sb.append(getWhenElapsed());
        sb.append(" ");
        sb.append(this.sourcePackage);
        sb.append('}');
        return sb.toString();
    }

    private static String policyIndexToString(int index) {
        switch (index) {
            case 0:
                return "requester";
            case 1:
                return "app_standby";
            case 2:
                return "device_idle";
            case 3:
                return "battery_saver";
            case 4:
                return "tare";
            default:
                return "--unknown(" + index + ")--";
        }
    }

    private static String exactReasonToString(int reason) {
        switch (reason) {
            case -1:
                return "N/A";
            case 0:
                return ParsingPackageUtils.TAG_PERMISSION;
            case 1:
                return "allow-listed";
            case 2:
                return "compat";
            case 3:
                return "policy_permission";
            default:
                return "--unknown--";
        }
    }

    public static String typeToString(int type) {
        switch (type) {
            case 0:
                return "RTC_WAKEUP";
            case 1:
                return "RTC";
            case 2:
                return "ELAPSED_WAKEUP";
            case 3:
                return "ELAPSED";
            default:
                return "--unknown--";
        }
    }

    public void dump(IndentingPrintWriter ipw, long nowELAPSED, SimpleDateFormat sdf) {
        int i = this.type;
        boolean z = true;
        if (i != 1 && i != 0) {
            z = false;
        }
        boolean isRtc = z;
        ipw.print("tag=");
        ipw.println(this.statsTag);
        ipw.print("type=");
        ipw.print(typeToString(this.type));
        ipw.print(" origWhen=");
        if (isRtc) {
            ipw.print(sdf.format(new Date(this.origWhen)));
        } else {
            TimeUtils.formatDuration(this.origWhen, nowELAPSED, ipw);
        }
        ipw.print(" window=");
        TimeUtils.formatDuration(this.windowLength, ipw);
        if (this.mExactAllowReason != -1) {
            ipw.print(" exactAllowReason=");
            ipw.print(exactReasonToString(this.mExactAllowReason));
        }
        ipw.print(" repeatInterval=");
        ipw.print(this.repeatInterval);
        ipw.print(" count=");
        ipw.print(this.count);
        ipw.print(" flags=0x");
        ipw.println(Integer.toHexString(this.flags));
        ipw.print("policyWhenElapsed:");
        for (int i2 = 0; i2 < 5; i2++) {
            ipw.print(" " + policyIndexToString(i2) + "=");
            TimeUtils.formatDuration(this.mPolicyWhenElapsed[i2], nowELAPSED, ipw);
        }
        ipw.println();
        ipw.print("whenElapsed=");
        TimeUtils.formatDuration(getWhenElapsed(), nowELAPSED, ipw);
        ipw.print(" maxWhenElapsed=");
        TimeUtils.formatDuration(this.mMaxWhenElapsed, nowELAPSED, ipw);
        if (this.mUsingReserveQuota) {
            ipw.print(" usingReserveQuota=true");
        }
        ipw.println();
        if (this.alarmClock != null) {
            ipw.println("Alarm clock:");
            ipw.print("  triggerTime=");
            ipw.println(sdf.format(new Date(this.alarmClock.getTriggerTime())));
            ipw.print("  showIntent=");
            ipw.println(this.alarmClock.getShowIntent());
        }
        if (this.operation != null) {
            ipw.print("operation=");
            ipw.println(this.operation);
        }
        if (this.listener != null) {
            ipw.print("listener=");
            ipw.println(this.listener.asBinder());
        }
        if (this.mIdleOptions != null) {
            ipw.print("idle-options=");
            ipw.println(this.mIdleOptions.toString());
        }
    }

    public void dumpDebug(ProtoOutputStream proto, long fieldId, long nowElapsed) {
        long token = proto.start(fieldId);
        proto.write(CompanionAppsPermissions.AppPermissions.PACKAGE_NAME, this.statsTag);
        proto.write(CompanionMessage.TYPE, this.type);
        proto.write(1112396529667L, getWhenElapsed() - nowElapsed);
        proto.write(1112396529668L, this.windowLength);
        proto.write(1112396529669L, this.repeatInterval);
        proto.write(1120986464262L, this.count);
        proto.write(1120986464263L, this.flags);
        AlarmManager.AlarmClockInfo alarmClockInfo = this.alarmClock;
        if (alarmClockInfo != null) {
            alarmClockInfo.dumpDebug(proto, 1146756268040L);
        }
        PendingIntent pendingIntent = this.operation;
        if (pendingIntent != null) {
            pendingIntent.dumpDebug(proto, 1146756268041L);
        }
        IAlarmListener iAlarmListener = this.listener;
        if (iAlarmListener != null) {
            proto.write(1138166333450L, iAlarmListener.asBinder().toString());
        }
        proto.end(token);
    }
}
