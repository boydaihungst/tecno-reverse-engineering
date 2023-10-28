package android.app;

import android.annotation.SystemApi;
import android.app.compat.CompatChanges;
import android.os.Bundle;
@SystemApi
/* loaded from: classes.dex */
public class BroadcastOptions extends ComponentOptions {
    public static final long CHANGE_ALWAYS_DISABLED = 210856463;
    public static final long CHANGE_ALWAYS_ENABLED = 209888056;
    public static final long CHANGE_INVALID = Long.MIN_VALUE;
    private static final String KEY_ALLOW_BACKGROUND_ACTIVITY_STARTS = "android:broadcast.allowBackgroundActivityStarts";
    private static final String KEY_DONT_SEND_TO_RESTRICTED_APPS = "android:broadcast.dontSendToRestrictedApps";
    private static final String KEY_ID_FOR_RESPONSE_EVENT = "android:broadcast.idForResponseEvent";
    private static final String KEY_MAX_MANIFEST_RECEIVER_API_LEVEL = "android:broadcast.maxManifestReceiverApiLevel";
    private static final String KEY_MIN_MANIFEST_RECEIVER_API_LEVEL = "android:broadcast.minManifestReceiverApiLevel";
    public static final String KEY_REQUIRE_ALL_OF_PERMISSIONS = "android:broadcast.requireAllOfPermissions";
    private static final String KEY_REQUIRE_COMPAT_CHANGE_ENABLED = "android:broadcast.requireCompatChangeEnabled";
    private static final String KEY_REQUIRE_COMPAT_CHANGE_ID = "android:broadcast.requireCompatChangeId";
    public static final String KEY_REQUIRE_NONE_OF_PERMISSIONS = "android:broadcast.requireNoneOfPermissions";
    private static final String KEY_TEMPORARY_APP_ALLOWLIST_DURATION = "android:broadcast.temporaryAppAllowlistDuration";
    private static final String KEY_TEMPORARY_APP_ALLOWLIST_REASON = "android:broadcast.temporaryAppAllowlistReason";
    private static final String KEY_TEMPORARY_APP_ALLOWLIST_REASON_CODE = "android:broadcast.temporaryAppAllowlistReasonCode";
    private static final String KEY_TEMPORARY_APP_ALLOWLIST_TYPE = "android:broadcast.temporaryAppAllowlistType";
    @Deprecated
    public static final int TEMPORARY_WHITELIST_TYPE_FOREGROUND_SERVICE_ALLOWED = 0;
    @Deprecated
    public static final int TEMPORARY_WHITELIST_TYPE_FOREGROUND_SERVICE_NOT_ALLOWED = 1;
    private boolean mAllowBackgroundActivityStarts;
    private boolean mDontSendToRestrictedApps;
    private long mIdForResponseEvent;
    private int mMaxManifestReceiverApiLevel;
    private int mMinManifestReceiverApiLevel;
    private String[] mRequireAllOfPermissions;
    private boolean mRequireCompatChangeEnabled;
    private long mRequireCompatChangeId;
    private String[] mRequireNoneOfPermissions;
    private long mTemporaryAppAllowlistDuration;
    private String mTemporaryAppAllowlistReason;
    private int mTemporaryAppAllowlistReasonCode;
    private int mTemporaryAppAllowlistType;

    public static BroadcastOptions makeBasic() {
        BroadcastOptions opts = new BroadcastOptions();
        return opts;
    }

    private BroadcastOptions() {
        this.mMinManifestReceiverApiLevel = 0;
        this.mMaxManifestReceiverApiLevel = 10000;
        this.mDontSendToRestrictedApps = false;
        this.mRequireCompatChangeId = Long.MIN_VALUE;
        this.mRequireCompatChangeEnabled = true;
        resetTemporaryAppAllowlist();
    }

    public BroadcastOptions(Bundle opts) {
        super(opts);
        this.mMinManifestReceiverApiLevel = 0;
        this.mMaxManifestReceiverApiLevel = 10000;
        this.mDontSendToRestrictedApps = false;
        this.mRequireCompatChangeId = Long.MIN_VALUE;
        this.mRequireCompatChangeEnabled = true;
        if (opts.containsKey(KEY_TEMPORARY_APP_ALLOWLIST_DURATION)) {
            this.mTemporaryAppAllowlistDuration = opts.getLong(KEY_TEMPORARY_APP_ALLOWLIST_DURATION);
            this.mTemporaryAppAllowlistType = opts.getInt(KEY_TEMPORARY_APP_ALLOWLIST_TYPE);
            this.mTemporaryAppAllowlistReasonCode = opts.getInt(KEY_TEMPORARY_APP_ALLOWLIST_REASON_CODE, 0);
            this.mTemporaryAppAllowlistReason = opts.getString(KEY_TEMPORARY_APP_ALLOWLIST_REASON);
        } else {
            resetTemporaryAppAllowlist();
        }
        this.mMinManifestReceiverApiLevel = opts.getInt(KEY_MIN_MANIFEST_RECEIVER_API_LEVEL, 0);
        this.mMaxManifestReceiverApiLevel = opts.getInt(KEY_MAX_MANIFEST_RECEIVER_API_LEVEL, 10000);
        this.mDontSendToRestrictedApps = opts.getBoolean(KEY_DONT_SEND_TO_RESTRICTED_APPS, false);
        this.mAllowBackgroundActivityStarts = opts.getBoolean(KEY_ALLOW_BACKGROUND_ACTIVITY_STARTS, false);
        this.mRequireAllOfPermissions = opts.getStringArray(KEY_REQUIRE_ALL_OF_PERMISSIONS);
        this.mRequireNoneOfPermissions = opts.getStringArray(KEY_REQUIRE_NONE_OF_PERMISSIONS);
        this.mRequireCompatChangeId = opts.getLong(KEY_REQUIRE_COMPAT_CHANGE_ID, Long.MIN_VALUE);
        this.mRequireCompatChangeEnabled = opts.getBoolean(KEY_REQUIRE_COMPAT_CHANGE_ENABLED, true);
        this.mIdForResponseEvent = opts.getLong(KEY_ID_FOR_RESPONSE_EVENT);
    }

    @Deprecated
    public void setTemporaryAppWhitelistDuration(long duration) {
        setTemporaryAppAllowlist(duration, 0, 0, null);
    }

    public void setTemporaryAppAllowlist(long duration, int type, int reasonCode, String reason) {
        this.mTemporaryAppAllowlistDuration = duration;
        this.mTemporaryAppAllowlistType = type;
        this.mTemporaryAppAllowlistReasonCode = reasonCode;
        this.mTemporaryAppAllowlistReason = reason;
        if (!isTemporaryAppAllowlistSet()) {
            resetTemporaryAppAllowlist();
        }
    }

    private boolean isTemporaryAppAllowlistSet() {
        return this.mTemporaryAppAllowlistDuration > 0 && this.mTemporaryAppAllowlistType != -1;
    }

    private void resetTemporaryAppAllowlist() {
        this.mTemporaryAppAllowlistDuration = 0L;
        this.mTemporaryAppAllowlistType = -1;
        this.mTemporaryAppAllowlistReasonCode = 0;
        this.mTemporaryAppAllowlistReason = null;
    }

    @Override // android.app.ComponentOptions
    @SystemApi(client = SystemApi.Client.PRIVILEGED_APPS)
    public void setPendingIntentBackgroundActivityLaunchAllowed(boolean allowed) {
        super.setPendingIntentBackgroundActivityLaunchAllowed(allowed);
    }

    @Override // android.app.ComponentOptions
    @SystemApi(client = SystemApi.Client.PRIVILEGED_APPS)
    public boolean isPendingIntentBackgroundActivityLaunchAllowed() {
        return super.isPendingIntentBackgroundActivityLaunchAllowed();
    }

    public long getTemporaryAppAllowlistDuration() {
        return this.mTemporaryAppAllowlistDuration;
    }

    public int getTemporaryAppAllowlistType() {
        return this.mTemporaryAppAllowlistType;
    }

    public int getTemporaryAppAllowlistReasonCode() {
        return this.mTemporaryAppAllowlistReasonCode;
    }

    public String getTemporaryAppAllowlistReason() {
        return this.mTemporaryAppAllowlistReason;
    }

    @Deprecated
    public void setMinManifestReceiverApiLevel(int apiLevel) {
        this.mMinManifestReceiverApiLevel = apiLevel;
    }

    @Deprecated
    public int getMinManifestReceiverApiLevel() {
        return this.mMinManifestReceiverApiLevel;
    }

    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    @Deprecated
    public void setMaxManifestReceiverApiLevel(int apiLevel) {
        this.mMaxManifestReceiverApiLevel = apiLevel;
    }

    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    @Deprecated
    public int getMaxManifestReceiverApiLevel() {
        return this.mMaxManifestReceiverApiLevel;
    }

    public void setDontSendToRestrictedApps(boolean dontSendToRestrictedApps) {
        this.mDontSendToRestrictedApps = dontSendToRestrictedApps;
    }

    public boolean isDontSendToRestrictedApps() {
        return this.mDontSendToRestrictedApps;
    }

    public void setBackgroundActivityStartsAllowed(boolean allowBackgroundActivityStarts) {
        this.mAllowBackgroundActivityStarts = allowBackgroundActivityStarts;
    }

    public boolean allowsBackgroundActivityStarts() {
        return this.mAllowBackgroundActivityStarts;
    }

    @SystemApi
    public void setRequireAllOfPermissions(String[] requiredPermissions) {
        this.mRequireAllOfPermissions = requiredPermissions;
    }

    @SystemApi
    public void setRequireNoneOfPermissions(String[] excludedPermissions) {
        this.mRequireNoneOfPermissions = excludedPermissions;
    }

    public void setRequireCompatChange(long changeId, boolean enabled) {
        this.mRequireCompatChangeId = changeId;
        this.mRequireCompatChangeEnabled = enabled;
    }

    public void clearRequireCompatChange() {
        this.mRequireCompatChangeId = Long.MIN_VALUE;
        this.mRequireCompatChangeEnabled = true;
    }

    public long getRequireCompatChangeId() {
        return this.mRequireCompatChangeId;
    }

    public boolean testRequireCompatChange(int uid) {
        long j = this.mRequireCompatChangeId;
        return j == Long.MIN_VALUE || CompatChanges.isChangeEnabled(j, uid) == this.mRequireCompatChangeEnabled;
    }

    @SystemApi
    public void recordResponseEventWhileInBackground(long id) {
        this.mIdForResponseEvent = id;
    }

    public long getIdForResponseEvent() {
        return this.mIdForResponseEvent;
    }

    @Override // android.app.ComponentOptions
    public Bundle toBundle() {
        Bundle b = super.toBundle();
        if (isTemporaryAppAllowlistSet()) {
            b.putLong(KEY_TEMPORARY_APP_ALLOWLIST_DURATION, this.mTemporaryAppAllowlistDuration);
            b.putInt(KEY_TEMPORARY_APP_ALLOWLIST_TYPE, this.mTemporaryAppAllowlistType);
            b.putInt(KEY_TEMPORARY_APP_ALLOWLIST_REASON_CODE, this.mTemporaryAppAllowlistReasonCode);
            b.putString(KEY_TEMPORARY_APP_ALLOWLIST_REASON, this.mTemporaryAppAllowlistReason);
        }
        int i = this.mMinManifestReceiverApiLevel;
        if (i != 0) {
            b.putInt(KEY_MIN_MANIFEST_RECEIVER_API_LEVEL, i);
        }
        int i2 = this.mMaxManifestReceiverApiLevel;
        if (i2 != 10000) {
            b.putInt(KEY_MAX_MANIFEST_RECEIVER_API_LEVEL, i2);
        }
        if (this.mDontSendToRestrictedApps) {
            b.putBoolean(KEY_DONT_SEND_TO_RESTRICTED_APPS, true);
        }
        if (this.mAllowBackgroundActivityStarts) {
            b.putBoolean(KEY_ALLOW_BACKGROUND_ACTIVITY_STARTS, true);
        }
        String[] strArr = this.mRequireAllOfPermissions;
        if (strArr != null) {
            b.putStringArray(KEY_REQUIRE_ALL_OF_PERMISSIONS, strArr);
        }
        String[] strArr2 = this.mRequireNoneOfPermissions;
        if (strArr2 != null) {
            b.putStringArray(KEY_REQUIRE_NONE_OF_PERMISSIONS, strArr2);
        }
        long j = this.mRequireCompatChangeId;
        if (j != Long.MIN_VALUE) {
            b.putLong(KEY_REQUIRE_COMPAT_CHANGE_ID, j);
            b.putBoolean(KEY_REQUIRE_COMPAT_CHANGE_ENABLED, this.mRequireCompatChangeEnabled);
        }
        long j2 = this.mIdForResponseEvent;
        if (j2 != 0) {
            b.putLong(KEY_ID_FOR_RESPONSE_EVENT, j2);
        }
        if (b.isEmpty()) {
            return null;
        }
        return b;
    }
}
