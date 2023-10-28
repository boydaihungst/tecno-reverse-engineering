package com.android.server.content;

import android.accounts.Account;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.os.SystemClock;
import android.os.UserHandle;
import android.util.Slog;
import com.android.server.content.SyncStorageEngine;
import com.android.server.policy.PhoneWindowManager;
import com.android.server.slice.SliceClientPermissions;
import java.util.Iterator;
/* loaded from: classes.dex */
public class SyncOperation {
    public static final int NO_JOB_ID = -1;
    public static final int REASON_ACCOUNTS_UPDATED = -2;
    public static final int REASON_BACKGROUND_DATA_SETTINGS_CHANGED = -1;
    public static final int REASON_IS_SYNCABLE = -5;
    public static final int REASON_MASTER_SYNC_AUTO = -7;
    private static String[] REASON_NAMES = {"DataSettingsChanged", "AccountsUpdated", "ServiceChanged", "Periodic", "IsSyncable", "AutoSync", "MasterSyncAuto", "UserStart"};
    public static final int REASON_PERIODIC = -4;
    public static final int REASON_SERVICE_CHANGED = -3;
    public static final int REASON_SYNC_AUTO = -6;
    public static final int REASON_USER_START = -8;
    public static final String TAG = "SyncManager";
    public final boolean allowParallelSyncs;
    public long expectedRuntime;
    public final long flexMillis;
    public final boolean isPeriodic;
    public int jobId;
    public final String key;
    private volatile Bundle mImmutableExtras;
    public final String owningPackage;
    public final int owningUid;
    public final long periodMillis;
    public final int reason;
    int retries;
    boolean scheduleEjAsRegularJob;
    public final int sourcePeriodicId;
    public int syncExemptionFlag;
    public final int syncSource;
    public final SyncStorageEngine.EndPoint target;
    public String wakeLockName;

    public SyncOperation(Account account, int userId, int owningUid, String owningPackage, int reason, int source, String provider, Bundle extras, boolean allowParallelSyncs, int syncExemptionFlag) {
        this(new SyncStorageEngine.EndPoint(account, provider, userId), owningUid, owningPackage, reason, source, extras, allowParallelSyncs, syncExemptionFlag);
    }

    private SyncOperation(SyncStorageEngine.EndPoint info, int owningUid, String owningPackage, int reason, int source, Bundle extras, boolean allowParallelSyncs, int syncExemptionFlag) {
        this(info, owningUid, owningPackage, reason, source, extras, allowParallelSyncs, false, -1, 0L, 0L, syncExemptionFlag);
    }

    public SyncOperation(SyncOperation op, long periodMillis, long flexMillis) {
        this(op.target, op.owningUid, op.owningPackage, op.reason, op.syncSource, op.mImmutableExtras, op.allowParallelSyncs, op.isPeriodic, op.sourcePeriodicId, periodMillis, flexMillis, 0);
    }

    public SyncOperation(SyncStorageEngine.EndPoint info, int owningUid, String owningPackage, int reason, int source, Bundle extras, boolean allowParallelSyncs, boolean isPeriodic, int sourcePeriodicId, long periodMillis, long flexMillis, int syncExemptionFlag) {
        this.target = info;
        this.owningUid = owningUid;
        this.owningPackage = owningPackage;
        this.reason = reason;
        this.syncSource = source;
        this.mImmutableExtras = new Bundle(extras);
        this.allowParallelSyncs = allowParallelSyncs;
        this.isPeriodic = isPeriodic;
        this.sourcePeriodicId = sourcePeriodicId;
        this.periodMillis = periodMillis;
        this.flexMillis = flexMillis;
        this.jobId = -1;
        this.key = toKey();
        this.syncExemptionFlag = syncExemptionFlag;
    }

    public SyncOperation createOneTimeSyncOperation() {
        if (!this.isPeriodic) {
            return null;
        }
        SyncOperation op = new SyncOperation(this.target, this.owningUid, this.owningPackage, this.reason, this.syncSource, this.mImmutableExtras, this.allowParallelSyncs, false, this.jobId, this.periodMillis, this.flexMillis, 0);
        return op;
    }

    public SyncOperation(SyncOperation other) {
        this.target = other.target;
        this.owningUid = other.owningUid;
        this.owningPackage = other.owningPackage;
        this.reason = other.reason;
        this.syncSource = other.syncSource;
        this.allowParallelSyncs = other.allowParallelSyncs;
        this.mImmutableExtras = other.mImmutableExtras;
        this.wakeLockName = other.wakeLockName();
        this.isPeriodic = other.isPeriodic;
        this.sourcePeriodicId = other.sourcePeriodicId;
        this.periodMillis = other.periodMillis;
        this.flexMillis = other.flexMillis;
        this.key = other.key;
        this.syncExemptionFlag = other.syncExemptionFlag;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public PersistableBundle toJobInfoExtras() {
        PersistableBundle jobInfoExtras = new PersistableBundle();
        PersistableBundle syncExtrasBundle = new PersistableBundle();
        Bundle extras = this.mImmutableExtras;
        for (String key : extras.keySet()) {
            Object value = extras.get(key);
            if (value instanceof Account) {
                Account account = (Account) value;
                PersistableBundle accountBundle = new PersistableBundle();
                accountBundle.putString("accountName", account.name);
                accountBundle.putString("accountType", account.type);
                jobInfoExtras.putPersistableBundle("ACCOUNT:" + key, accountBundle);
            } else if (value instanceof Long) {
                syncExtrasBundle.putLong(key, ((Long) value).longValue());
            } else if (value instanceof Integer) {
                syncExtrasBundle.putInt(key, ((Integer) value).intValue());
            } else if (value instanceof Boolean) {
                syncExtrasBundle.putBoolean(key, ((Boolean) value).booleanValue());
            } else if (value instanceof Float) {
                syncExtrasBundle.putDouble(key, ((Float) value).floatValue());
            } else if (value instanceof Double) {
                syncExtrasBundle.putDouble(key, ((Double) value).doubleValue());
            } else if (value instanceof String) {
                syncExtrasBundle.putString(key, (String) value);
            } else if (value == null) {
                syncExtrasBundle.putString(key, null);
            } else {
                Slog.e(TAG, "Unknown extra type.");
            }
        }
        jobInfoExtras.putPersistableBundle("syncExtras", syncExtrasBundle);
        jobInfoExtras.putBoolean("SyncManagerJob", true);
        jobInfoExtras.putString("provider", this.target.provider);
        jobInfoExtras.putString("accountName", this.target.account.name);
        jobInfoExtras.putString("accountType", this.target.account.type);
        jobInfoExtras.putInt("userId", this.target.userId);
        jobInfoExtras.putInt("owningUid", this.owningUid);
        jobInfoExtras.putString("owningPackage", this.owningPackage);
        jobInfoExtras.putInt(PhoneWindowManager.SYSTEM_DIALOG_REASON_KEY, this.reason);
        jobInfoExtras.putInt("source", this.syncSource);
        jobInfoExtras.putBoolean("allowParallelSyncs", this.allowParallelSyncs);
        jobInfoExtras.putInt("jobId", this.jobId);
        jobInfoExtras.putBoolean("isPeriodic", this.isPeriodic);
        jobInfoExtras.putInt("sourcePeriodicId", this.sourcePeriodicId);
        jobInfoExtras.putLong("periodMillis", this.periodMillis);
        jobInfoExtras.putLong("flexMillis", this.flexMillis);
        jobInfoExtras.putLong("expectedRuntime", this.expectedRuntime);
        jobInfoExtras.putInt("retries", this.retries);
        jobInfoExtras.putInt("syncExemptionFlag", this.syncExemptionFlag);
        jobInfoExtras.putBoolean("ejDowngradedToRegular", this.scheduleEjAsRegularJob);
        return jobInfoExtras;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static SyncOperation maybeCreateFromJobExtras(PersistableBundle jobExtras) {
        String str;
        Iterator<String> it;
        if (jobExtras != null && jobExtras.getBoolean("SyncManagerJob", false)) {
            String str2 = "accountName";
            String accountName = jobExtras.getString("accountName");
            String accountType = jobExtras.getString("accountType");
            String provider = jobExtras.getString("provider");
            int userId = jobExtras.getInt("userId", Integer.MAX_VALUE);
            int owningUid = jobExtras.getInt("owningUid");
            String owningPackage = jobExtras.getString("owningPackage");
            int reason = jobExtras.getInt(PhoneWindowManager.SYSTEM_DIALOG_REASON_KEY, Integer.MAX_VALUE);
            int source = jobExtras.getInt("source", Integer.MAX_VALUE);
            boolean allowParallelSyncs = jobExtras.getBoolean("allowParallelSyncs", false);
            boolean isPeriodic = jobExtras.getBoolean("isPeriodic", false);
            int initiatedBy = jobExtras.getInt("sourcePeriodicId", -1);
            long periodMillis = jobExtras.getLong("periodMillis");
            long flexMillis = jobExtras.getLong("flexMillis");
            int syncExemptionFlag = jobExtras.getInt("syncExemptionFlag", 0);
            Bundle extras = new Bundle();
            PersistableBundle syncExtras = jobExtras.getPersistableBundle("syncExtras");
            if (syncExtras != null) {
                extras.putAll(syncExtras);
            }
            Iterator<String> it2 = jobExtras.keySet().iterator();
            while (it2.hasNext()) {
                String key = it2.next();
                if (key == null || !key.startsWith("ACCOUNT:")) {
                    str = str2;
                    it = it2;
                } else {
                    String newKey = key.substring(8);
                    PersistableBundle accountsBundle = jobExtras.getPersistableBundle(key);
                    it = it2;
                    str = str2;
                    Account account = new Account(accountsBundle.getString(str2), accountsBundle.getString("accountType"));
                    extras.putParcelable(newKey, account);
                }
                it2 = it;
                str2 = str;
            }
            Account account2 = new Account(accountName, accountType);
            SyncStorageEngine.EndPoint target = new SyncStorageEngine.EndPoint(account2, provider, userId);
            SyncOperation op = new SyncOperation(target, owningUid, owningPackage, reason, source, extras, allowParallelSyncs, isPeriodic, initiatedBy, periodMillis, flexMillis, syncExemptionFlag);
            op.jobId = jobExtras.getInt("jobId");
            op.expectedRuntime = jobExtras.getLong("expectedRuntime");
            op.retries = jobExtras.getInt("retries");
            op.scheduleEjAsRegularJob = jobExtras.getBoolean("ejDowngradedToRegular");
            return op;
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isConflict(SyncOperation toRun) {
        SyncStorageEngine.EndPoint other = toRun.target;
        return this.target.account.type.equals(other.account.type) && this.target.provider.equals(other.provider) && this.target.userId == other.userId && (!this.allowParallelSyncs || this.target.account.name.equals(other.account.name));
    }

    boolean isReasonPeriodic() {
        return this.reason == -4;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean matchesPeriodicOperation(SyncOperation other) {
        return this.target.matchesSpec(other.target) && SyncManager.syncExtrasEquals(this.mImmutableExtras, other.mImmutableExtras, true) && this.periodMillis == other.periodMillis && this.flexMillis == other.flexMillis;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isDerivedFromFailedPeriodicSync() {
        return this.sourcePeriodicId != -1;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getJobBias() {
        if (isInitialization()) {
            return 20;
        }
        if (isExpedited()) {
            return 10;
        }
        return 0;
    }

    private String toKey() {
        Bundle extras = this.mImmutableExtras;
        StringBuilder sb = new StringBuilder();
        sb.append("provider: ").append(this.target.provider);
        sb.append(" account {name=" + this.target.account.name + ", user=" + this.target.userId + ", type=" + this.target.account.type + "}");
        sb.append(" isPeriodic: ").append(this.isPeriodic);
        sb.append(" period: ").append(this.periodMillis);
        sb.append(" flex: ").append(this.flexMillis);
        sb.append(" extras: ");
        extrasToStringBuilder(extras, sb);
        return sb.toString();
    }

    public String toString() {
        return dump(null, true, null, false);
    }

    public String toSafeString() {
        return dump(null, true, null, true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String dump(PackageManager pm, boolean shorter, SyncAdapterStateFetcher appStates, boolean logSafe) {
        Bundle extras = this.mImmutableExtras;
        StringBuilder sb = new StringBuilder();
        sb.append("JobId=").append(this.jobId).append(" ").append(logSafe ? "***" : this.target.account.name).append(SliceClientPermissions.SliceAuthority.DELIMITER).append(this.target.account.type).append(" u").append(this.target.userId).append(" [").append(this.target.provider).append("] ");
        sb.append(SyncStorageEngine.SOURCES[this.syncSource]);
        if (this.expectedRuntime != 0) {
            sb.append(" ExpectedIn=");
            SyncManager.formatDurationHMS(sb, this.expectedRuntime - SystemClock.elapsedRealtime());
        }
        if (extras.getBoolean("expedited", false)) {
            sb.append(" EXPEDITED");
        }
        if (extras.getBoolean("schedule_as_expedited_job", false)) {
            sb.append(" EXPEDITED-JOB");
            if (this.scheduleEjAsRegularJob) {
                sb.append("(scheduled-as-regular)");
            }
        }
        switch (this.syncExemptionFlag) {
            case 0:
                break;
            case 1:
                sb.append(" STANDBY-EXEMPTED");
                break;
            case 2:
                sb.append(" STANDBY-EXEMPTED(TOP)");
                break;
            default:
                sb.append(" ExemptionFlag=" + this.syncExemptionFlag);
                break;
        }
        sb.append(" Reason=");
        sb.append(reasonToString(pm, this.reason));
        if (this.isPeriodic) {
            sb.append(" (period=");
            SyncManager.formatDurationHMS(sb, this.periodMillis);
            sb.append(" flex=");
            SyncManager.formatDurationHMS(sb, this.flexMillis);
            sb.append(")");
        }
        if (this.retries > 0) {
            sb.append(" Retries=");
            sb.append(this.retries);
        }
        if (!shorter) {
            sb.append(" Owner={");
            UserHandle.formatUid(sb, this.owningUid);
            sb.append(" ");
            sb.append(this.owningPackage);
            if (appStates != null) {
                sb.append(" [");
                sb.append(appStates.getStandbyBucket(UserHandle.getUserId(this.owningUid), this.owningPackage));
                sb.append("]");
                if (appStates.isAppActive(this.owningUid)) {
                    sb.append(" [ACTIVE]");
                }
            }
            sb.append("}");
            if (!extras.keySet().isEmpty()) {
                sb.append(" ");
                extrasToStringBuilder(extras, sb);
            }
        }
        return sb.toString();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static String reasonToString(PackageManager pm, int reason) {
        if (reason < 0) {
            int index = (-reason) - 1;
            String[] strArr = REASON_NAMES;
            if (index >= strArr.length) {
                return String.valueOf(reason);
            }
            return strArr[index];
        } else if (pm != null) {
            String[] packages = pm.getPackagesForUid(reason);
            if (packages != null && packages.length == 1) {
                return packages[0];
            }
            String name = pm.getNameForUid(reason);
            if (name != null) {
                return name;
            }
            return String.valueOf(reason);
        } else {
            return String.valueOf(reason);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isInitialization() {
        return this.mImmutableExtras.getBoolean("initialize", false);
    }

    boolean isExpedited() {
        return this.mImmutableExtras.getBoolean("expedited", false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isUpload() {
        return this.mImmutableExtras.getBoolean("upload", false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void enableTwoWaySync() {
        removeExtra("upload");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasIgnoreBackoff() {
        return this.mImmutableExtras.getBoolean("ignore_backoff", false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void enableBackoff() {
        removeExtra("ignore_backoff");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasDoNotRetry() {
        return this.mImmutableExtras.getBoolean("do_not_retry", false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isNotAllowedOnMetered() {
        return this.mImmutableExtras.getBoolean("allow_metered", false);
    }

    boolean isManual() {
        return this.mImmutableExtras.getBoolean("force", false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isIgnoreSettings() {
        return this.mImmutableExtras.getBoolean("ignore_settings", false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasRequireCharging() {
        return this.mImmutableExtras.getBoolean("require_charging", false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isScheduledAsExpeditedJob() {
        return this.mImmutableExtras.getBoolean("schedule_as_expedited_job", false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isAppStandbyExempted() {
        return this.syncExemptionFlag != 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean areExtrasEqual(Bundle other, boolean includeSyncSettings) {
        return SyncManager.syncExtrasEquals(this.mImmutableExtras, other, includeSyncSettings);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void extrasToStringBuilder(Bundle bundle, StringBuilder sb) {
        if (bundle == null) {
            sb.append("null");
            return;
        }
        sb.append("[");
        for (String key : bundle.keySet()) {
            sb.append(key).append("=").append(bundle.get(key)).append(" ");
        }
        sb.append("]");
    }

    private static String extrasToString(Bundle bundle) {
        StringBuilder sb = new StringBuilder();
        extrasToStringBuilder(bundle, sb);
        return sb.toString();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String wakeLockName() {
        String str = this.wakeLockName;
        if (str != null) {
            return str;
        }
        String str2 = this.target.provider + SliceClientPermissions.SliceAuthority.DELIMITER + this.target.account.type + SliceClientPermissions.SliceAuthority.DELIMITER + this.target.account.name;
        this.wakeLockName = str2;
        return str2;
    }

    public Object[] toEventLog(int event) {
        Object[] logArray = {this.target.provider, Integer.valueOf(event), Integer.valueOf(this.syncSource), Integer.valueOf(this.target.account.name.hashCode())};
        return logArray;
    }

    private void removeExtra(String key) {
        Bundle b = this.mImmutableExtras;
        if (!b.containsKey(key)) {
            return;
        }
        Bundle clone = new Bundle(b);
        clone.remove(key);
        this.mImmutableExtras = clone;
    }

    public Bundle getClonedExtras() {
        return new Bundle(this.mImmutableExtras);
    }

    public String getExtrasAsString() {
        return extrasToString(this.mImmutableExtras);
    }
}
