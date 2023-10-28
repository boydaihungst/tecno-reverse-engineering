package com.android.server.net;

import android.app.ActivityManager;
import android.net.NetworkPolicyManager;
import android.os.PowerExemptionManager;
import android.os.UserHandle;
import android.util.Log;
import android.util.Slog;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.RingBuffer;
import com.android.server.am.ProcessList;
import com.android.server.health.HealthServiceWrapperHidl;
import com.android.server.net.NetworkPolicyManagerService;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Set;
/* loaded from: classes2.dex */
public class NetworkPolicyLogger {
    private static final int EVENT_APP_IDLE_STATE_CHANGED = 8;
    private static final int EVENT_APP_IDLE_WL_CHANGED = 14;
    private static final int EVENT_DEVICE_IDLE_MODE_ENABLED = 7;
    private static final int EVENT_FIREWALL_CHAIN_ENABLED = 12;
    private static final int EVENT_METEREDNESS_CHANGED = 4;
    private static final int EVENT_METERED_ALLOWLIST_CHANGED = 15;
    private static final int EVENT_METERED_DENYLIST_CHANGED = 16;
    private static final int EVENT_NETWORK_BLOCKED = 1;
    private static final int EVENT_PAROLE_STATE_CHANGED = 9;
    private static final int EVENT_POLICIES_CHANGED = 3;
    private static final int EVENT_RESTRICT_BG_CHANGED = 6;
    private static final int EVENT_TEMP_POWER_SAVE_WL_CHANGED = 10;
    private static final int EVENT_TYPE_GENERIC = 0;
    private static final int EVENT_UID_FIREWALL_RULE_CHANGED = 11;
    private static final int EVENT_UID_STATE_CHANGED = 2;
    private static final int EVENT_UPDATE_METERED_RESTRICTED_PKGS = 13;
    private static final int EVENT_USER_STATE_REMOVED = 5;
    private static final int MAX_LOG_SIZE;
    private static final int MAX_NETWORK_BLOCKED_LOG_SIZE;
    private int mDebugUid;
    private final LogBuffer mEventsBuffer;
    private final Object mLock;
    private final LogBuffer mNetworkBlockedBuffer = new LogBuffer(MAX_NETWORK_BLOCKED_LOG_SIZE);
    private final LogBuffer mUidStateChangeBuffer;
    static final String TAG = "NetworkPolicy";
    static final boolean LOGD = Log.isLoggable(TAG, 3);
    static final boolean LOGV = Log.isLoggable(TAG, 2);

    public NetworkPolicyLogger() {
        int i = MAX_LOG_SIZE;
        this.mUidStateChangeBuffer = new LogBuffer(i);
        this.mEventsBuffer = new LogBuffer(i);
        this.mDebugUid = -1;
        this.mLock = new Object();
    }

    static {
        MAX_LOG_SIZE = ActivityManager.isLowRamDeviceStatic() ? 100 : 400;
        MAX_NETWORK_BLOCKED_LOG_SIZE = ActivityManager.isLowRamDeviceStatic() ? 100 : 400;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void networkBlocked(int uid, NetworkPolicyManagerService.UidBlockedState uidBlockedState) {
        synchronized (this.mLock) {
            if (LOGD || uid == this.mDebugUid) {
                Slog.d(TAG, "Blocked state of " + uid + ": " + uidBlockedState);
            }
            if (uidBlockedState == null) {
                this.mNetworkBlockedBuffer.networkBlocked(uid, 0, 0, 0);
            } else {
                this.mNetworkBlockedBuffer.networkBlocked(uid, uidBlockedState.blockedReasons, uidBlockedState.allowedReasons, uidBlockedState.effectiveBlockedReasons);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void uidStateChanged(int uid, int procState, long procStateSeq, int capability) {
        synchronized (this.mLock) {
            if (LOGV || uid == this.mDebugUid) {
                Slog.v(TAG, uid + " state changed to " + ProcessList.makeProcStateString(procState) + ",seq=" + procStateSeq + ",cap=" + ActivityManager.getCapabilitiesSummary(capability));
            }
            this.mUidStateChangeBuffer.uidStateChanged(uid, procState, procStateSeq, capability);
        }
    }

    void event(String msg) {
        synchronized (this.mLock) {
            if (LOGV) {
                Slog.v(TAG, msg);
            }
            this.mEventsBuffer.event(msg);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void uidPolicyChanged(int uid, int oldPolicy, int newPolicy) {
        synchronized (this.mLock) {
            if (LOGV || uid == this.mDebugUid) {
                Slog.v(TAG, getPolicyChangedLog(uid, oldPolicy, newPolicy));
            }
            this.mEventsBuffer.uidPolicyChanged(uid, oldPolicy, newPolicy);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void meterednessChanged(int netId, boolean newMetered) {
        synchronized (this.mLock) {
            if (LOGD || this.mDebugUid != -1) {
                Slog.d(TAG, getMeterednessChangedLog(netId, newMetered));
            }
            this.mEventsBuffer.meterednessChanged(netId, newMetered);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removingUserState(int userId) {
        synchronized (this.mLock) {
            if (LOGD || this.mDebugUid != -1) {
                Slog.d(TAG, getUserRemovedLog(userId));
            }
            this.mEventsBuffer.userRemoved(userId);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void restrictBackgroundChanged(boolean oldValue, boolean newValue) {
        synchronized (this.mLock) {
            if (LOGD || this.mDebugUid != -1) {
                Slog.d(TAG, getRestrictBackgroundChangedLog(oldValue, newValue));
            }
            this.mEventsBuffer.restrictBackgroundChanged(oldValue, newValue);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void deviceIdleModeEnabled(boolean enabled) {
        synchronized (this.mLock) {
            if (LOGD || this.mDebugUid != -1) {
                Slog.d(TAG, getDeviceIdleModeEnabled(enabled));
            }
            this.mEventsBuffer.deviceIdleModeEnabled(enabled);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void appIdleStateChanged(int uid, boolean idle) {
        synchronized (this.mLock) {
            if (LOGD || uid == this.mDebugUid) {
                Slog.d(TAG, getAppIdleChangedLog(uid, idle));
            }
            this.mEventsBuffer.appIdleStateChanged(uid, idle);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void appIdleWlChanged(int uid, boolean isWhitelisted) {
        synchronized (this.mLock) {
            if (LOGD || uid == this.mDebugUid) {
                Slog.d(TAG, getAppIdleWlChangedLog(uid, isWhitelisted));
            }
            this.mEventsBuffer.appIdleWlChanged(uid, isWhitelisted);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void paroleStateChanged(boolean paroleOn) {
        synchronized (this.mLock) {
            if (LOGD || this.mDebugUid != -1) {
                Slog.d(TAG, getParoleStateChanged(paroleOn));
            }
            this.mEventsBuffer.paroleStateChanged(paroleOn);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void tempPowerSaveWlChanged(int appId, boolean added, int reasonCode, String reason) {
        synchronized (this.mLock) {
            if (LOGV || appId == UserHandle.getAppId(this.mDebugUid)) {
                Slog.v(TAG, getTempPowerSaveWlChangedLog(appId, added, reasonCode, reason));
            }
            this.mEventsBuffer.tempPowerSaveWlChanged(appId, added, reasonCode, reason);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void uidFirewallRuleChanged(int chain, int uid, int rule) {
        synchronized (this.mLock) {
            if (LOGV || uid == this.mDebugUid) {
                Slog.v(TAG, getUidFirewallRuleChangedLog(chain, uid, rule));
            }
            this.mEventsBuffer.uidFirewallRuleChanged(chain, uid, rule);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void firewallChainEnabled(int chain, boolean enabled) {
        synchronized (this.mLock) {
            if (LOGD || this.mDebugUid != -1) {
                Slog.d(TAG, getFirewallChainEnabledLog(chain, enabled));
            }
            this.mEventsBuffer.firewallChainEnabled(chain, enabled);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void firewallRulesChanged(int chain, int[] uids, int[] rules) {
        synchronized (this.mLock) {
            String log = "Firewall rules changed for " + getFirewallChainName(chain) + "; uids=" + Arrays.toString(uids) + "; rules=" + Arrays.toString(rules);
            if (LOGD || this.mDebugUid != -1) {
                Slog.d(TAG, log);
            }
            this.mEventsBuffer.event(log);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void meteredRestrictedPkgsChanged(Set<Integer> restrictedUids) {
        synchronized (this.mLock) {
            String log = "Metered restricted uids: " + restrictedUids;
            if (LOGD || this.mDebugUid != -1) {
                Slog.d(TAG, log);
            }
            this.mEventsBuffer.event(log);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void meteredAllowlistChanged(int uid, boolean added) {
        synchronized (this.mLock) {
            if (LOGD || this.mDebugUid == uid) {
                Slog.d(TAG, getMeteredAllowlistChangedLog(uid, added));
            }
            this.mEventsBuffer.meteredAllowlistChanged(uid, added);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void meteredDenylistChanged(int uid, boolean added) {
        synchronized (this.mLock) {
            if (LOGD || this.mDebugUid == uid) {
                Slog.d(TAG, getMeteredDenylistChangedLog(uid, added));
            }
            this.mEventsBuffer.meteredDenylistChanged(uid, added);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setDebugUid(int uid) {
        this.mDebugUid = uid;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpLogs(IndentingPrintWriter pw) {
        synchronized (this.mLock) {
            pw.println();
            pw.println("mEventLogs (most recent first):");
            pw.increaseIndent();
            this.mEventsBuffer.reverseDump(pw);
            pw.decreaseIndent();
            pw.println();
            pw.println("mNetworkBlockedLogs (most recent first):");
            pw.increaseIndent();
            this.mNetworkBlockedBuffer.reverseDump(pw);
            pw.decreaseIndent();
            pw.println();
            pw.println("mUidStateChangeLogs (most recent first):");
            pw.increaseIndent();
            this.mUidStateChangeBuffer.reverseDump(pw);
            pw.decreaseIndent();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static String getPolicyChangedLog(int uid, int oldPolicy, int newPolicy) {
        return "Policy for " + uid + " changed from " + NetworkPolicyManager.uidPoliciesToString(oldPolicy) + " to " + NetworkPolicyManager.uidPoliciesToString(newPolicy);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static String getMeterednessChangedLog(int netId, boolean newMetered) {
        return "Meteredness of netId=" + netId + " changed to " + newMetered;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static String getUserRemovedLog(int userId) {
        return "Remove state for u" + userId;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static String getRestrictBackgroundChangedLog(boolean oldValue, boolean newValue) {
        return "Changed restrictBackground: " + oldValue + "->" + newValue;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static String getDeviceIdleModeEnabled(boolean enabled) {
        return "DeviceIdleMode enabled: " + enabled;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static String getAppIdleChangedLog(int uid, boolean idle) {
        return "App idle state of uid " + uid + ": " + idle;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static String getAppIdleWlChangedLog(int uid, boolean isWhitelisted) {
        return "App idle whitelist state of uid " + uid + ": " + isWhitelisted;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static String getParoleStateChanged(boolean paroleOn) {
        return "Parole state: " + paroleOn;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static String getTempPowerSaveWlChangedLog(int appId, boolean added, int reasonCode, String reason) {
        return "temp-power-save whitelist for " + appId + " changed to: " + added + "; reason=" + PowerExemptionManager.reasonCodeToString(reasonCode) + " <" + reason + ">";
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static String getUidFirewallRuleChangedLog(int chain, int uid, int rule) {
        return String.format("Firewall rule changed: %d-%s-%s", Integer.valueOf(uid), getFirewallChainName(chain), getFirewallRuleName(rule));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static String getFirewallChainEnabledLog(int chain, boolean enabled) {
        return "Firewall chain " + getFirewallChainName(chain) + " state: " + enabled;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static String getMeteredAllowlistChangedLog(int uid, boolean added) {
        return "metered-allowlist for " + uid + " changed to " + added;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static String getMeteredDenylistChangedLog(int uid, boolean added) {
        return "metered-denylist for " + uid + " changed to " + added;
    }

    private static String getFirewallChainName(int chain) {
        switch (chain) {
            case 1:
                return "dozable";
            case 2:
                return "standby";
            case 3:
                return "powersave";
            case 4:
                return "restricted";
            case 5:
                return "low_power_standby";
            default:
                return String.valueOf(chain);
        }
    }

    private static String getFirewallRuleName(int rule) {
        switch (rule) {
            case 0:
                return HealthServiceWrapperHidl.INSTANCE_VENDOR;
            case 1:
                return "allow";
            case 2:
                return "deny";
            default:
                return String.valueOf(rule);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static final class LogBuffer extends RingBuffer<Data> {
        private static final SimpleDateFormat sFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss:SSS");
        private static final Date sDate = new Date();

        public LogBuffer(int capacity) {
            super(Data.class, capacity);
        }

        public void uidStateChanged(int uid, int procState, long procStateSeq, int capability) {
            Data data = (Data) getNextSlot();
            if (data == null) {
                return;
            }
            data.reset();
            data.type = 2;
            data.ifield1 = uid;
            data.ifield2 = procState;
            data.ifield3 = capability;
            data.lfield1 = procStateSeq;
            data.timeStamp = System.currentTimeMillis();
        }

        public void event(String msg) {
            Data data = (Data) getNextSlot();
            if (data == null) {
                return;
            }
            data.reset();
            data.type = 0;
            data.sfield1 = msg;
            data.timeStamp = System.currentTimeMillis();
        }

        public void networkBlocked(int uid, int blockedReasons, int allowedReasons, int effectiveBlockedReasons) {
            Data data = (Data) getNextSlot();
            if (data == null) {
                return;
            }
            data.reset();
            data.type = 1;
            data.ifield1 = uid;
            data.ifield2 = blockedReasons;
            data.ifield3 = allowedReasons;
            data.ifield4 = effectiveBlockedReasons;
            data.timeStamp = System.currentTimeMillis();
        }

        public void uidPolicyChanged(int uid, int oldPolicy, int newPolicy) {
            Data data = (Data) getNextSlot();
            if (data == null) {
                return;
            }
            data.reset();
            data.type = 3;
            data.ifield1 = uid;
            data.ifield2 = oldPolicy;
            data.ifield3 = newPolicy;
            data.timeStamp = System.currentTimeMillis();
        }

        public void meterednessChanged(int netId, boolean newMetered) {
            Data data = (Data) getNextSlot();
            if (data == null) {
                return;
            }
            data.reset();
            data.type = 4;
            data.ifield1 = netId;
            data.bfield1 = newMetered;
            data.timeStamp = System.currentTimeMillis();
        }

        public void userRemoved(int userId) {
            Data data = (Data) getNextSlot();
            if (data == null) {
                return;
            }
            data.reset();
            data.type = 5;
            data.ifield1 = userId;
            data.timeStamp = System.currentTimeMillis();
        }

        public void restrictBackgroundChanged(boolean oldValue, boolean newValue) {
            Data data = (Data) getNextSlot();
            if (data == null) {
                return;
            }
            data.reset();
            data.type = 6;
            data.bfield1 = oldValue;
            data.bfield2 = newValue;
            data.timeStamp = System.currentTimeMillis();
        }

        public void deviceIdleModeEnabled(boolean enabled) {
            Data data = (Data) getNextSlot();
            if (data == null) {
                return;
            }
            data.reset();
            data.type = 7;
            data.bfield1 = enabled;
            data.timeStamp = System.currentTimeMillis();
        }

        public void appIdleStateChanged(int uid, boolean idle) {
            Data data = (Data) getNextSlot();
            if (data == null) {
                return;
            }
            data.reset();
            data.type = 8;
            data.ifield1 = uid;
            data.bfield1 = idle;
            data.timeStamp = System.currentTimeMillis();
        }

        public void appIdleWlChanged(int uid, boolean isWhitelisted) {
            Data data = (Data) getNextSlot();
            if (data == null) {
                return;
            }
            data.reset();
            data.type = 14;
            data.ifield1 = uid;
            data.bfield1 = isWhitelisted;
            data.timeStamp = System.currentTimeMillis();
        }

        public void paroleStateChanged(boolean paroleOn) {
            Data data = (Data) getNextSlot();
            if (data == null) {
                return;
            }
            data.reset();
            data.type = 9;
            data.bfield1 = paroleOn;
            data.timeStamp = System.currentTimeMillis();
        }

        public void tempPowerSaveWlChanged(int appId, boolean added, int reasonCode, String reason) {
            Data data = (Data) getNextSlot();
            if (data == null) {
                return;
            }
            data.reset();
            data.type = 10;
            data.ifield1 = appId;
            data.ifield2 = reasonCode;
            data.bfield1 = added;
            data.sfield1 = reason;
            data.timeStamp = System.currentTimeMillis();
        }

        public void uidFirewallRuleChanged(int chain, int uid, int rule) {
            Data data = (Data) getNextSlot();
            if (data == null) {
                return;
            }
            data.reset();
            data.type = 11;
            data.ifield1 = chain;
            data.ifield2 = uid;
            data.ifield3 = rule;
            data.timeStamp = System.currentTimeMillis();
        }

        public void firewallChainEnabled(int chain, boolean enabled) {
            Data data = (Data) getNextSlot();
            if (data == null) {
                return;
            }
            data.reset();
            data.type = 12;
            data.ifield1 = chain;
            data.bfield1 = enabled;
            data.timeStamp = System.currentTimeMillis();
        }

        public void meteredAllowlistChanged(int uid, boolean added) {
            Data data = (Data) getNextSlot();
            if (data == null) {
                return;
            }
            data.reset();
            data.type = 15;
            data.ifield1 = uid;
            data.bfield1 = added;
            data.timeStamp = System.currentTimeMillis();
        }

        public void meteredDenylistChanged(int uid, boolean added) {
            Data data = (Data) getNextSlot();
            if (data == null) {
                return;
            }
            data.reset();
            data.type = 16;
            data.ifield1 = uid;
            data.bfield1 = added;
            data.timeStamp = System.currentTimeMillis();
        }

        public void reverseDump(IndentingPrintWriter pw) {
            Data[] allData = (Data[]) toArray();
            for (int i = allData.length - 1; i >= 0; i--) {
                if (allData[i] == null) {
                    pw.println("NULL");
                } else {
                    pw.print(formatDate(allData[i].timeStamp));
                    pw.print(" - ");
                    pw.println(getContent(allData[i]));
                }
            }
        }

        public String getContent(Data data) {
            switch (data.type) {
                case 0:
                    return data.sfield1;
                case 1:
                    return data.ifield1 + "-" + NetworkPolicyManagerService.UidBlockedState.toString(data.ifield2, data.ifield3, data.ifield4);
                case 2:
                    return data.ifield1 + ":" + ProcessList.makeProcStateString(data.ifield2) + ":" + ActivityManager.getCapabilitiesSummary(data.ifield3) + ":" + data.lfield1;
                case 3:
                    return NetworkPolicyLogger.getPolicyChangedLog(data.ifield1, data.ifield2, data.ifield3);
                case 4:
                    return NetworkPolicyLogger.getMeterednessChangedLog(data.ifield1, data.bfield1);
                case 5:
                    return NetworkPolicyLogger.getUserRemovedLog(data.ifield1);
                case 6:
                    return NetworkPolicyLogger.getRestrictBackgroundChangedLog(data.bfield1, data.bfield2);
                case 7:
                    return NetworkPolicyLogger.getDeviceIdleModeEnabled(data.bfield1);
                case 8:
                    return NetworkPolicyLogger.getAppIdleChangedLog(data.ifield1, data.bfield1);
                case 9:
                    return NetworkPolicyLogger.getParoleStateChanged(data.bfield1);
                case 10:
                    return NetworkPolicyLogger.getTempPowerSaveWlChangedLog(data.ifield1, data.bfield1, data.ifield2, data.sfield1);
                case 11:
                    return NetworkPolicyLogger.getUidFirewallRuleChangedLog(data.ifield1, data.ifield2, data.ifield3);
                case 12:
                    return NetworkPolicyLogger.getFirewallChainEnabledLog(data.ifield1, data.bfield1);
                case 13:
                default:
                    return String.valueOf(data.type);
                case 14:
                    return NetworkPolicyLogger.getAppIdleWlChangedLog(data.ifield1, data.bfield1);
                case 15:
                    return NetworkPolicyLogger.getMeteredAllowlistChangedLog(data.ifield1, data.bfield1);
                case 16:
                    return NetworkPolicyLogger.getMeteredDenylistChangedLog(data.ifield1, data.bfield1);
            }
        }

        private String formatDate(long millis) {
            Date date = sDate;
            date.setTime(millis);
            return sFormatter.format(date);
        }
    }

    /* loaded from: classes2.dex */
    public static final class Data {
        public boolean bfield1;
        public boolean bfield2;
        public int ifield1;
        public int ifield2;
        public int ifield3;
        public int ifield4;
        public long lfield1;
        public String sfield1;
        public long timeStamp;
        public int type;

        public void reset() {
            this.sfield1 = null;
        }
    }
}
