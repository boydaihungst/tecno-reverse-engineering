package com.android.server.compat;

import android.app.compat.PackageOverride;
import android.content.pm.ApplicationInfo;
import com.android.internal.compat.AndroidBuildClassifier;
import com.android.internal.compat.CompatibilityChangeInfo;
import com.android.internal.compat.OverrideAllowedState;
import com.android.server.compat.config.Change;
import com.android.server.compat.overrides.ChangeOverrides;
import com.android.server.compat.overrides.OverrideValue;
import com.android.server.compat.overrides.RawOverrideValue;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
/* loaded from: classes.dex */
public final class CompatChange extends CompatibilityChangeInfo {
    static final long CTS_SYSTEM_API_CHANGEID = 149391281;
    static final long CTS_SYSTEM_API_OVERRIDABLE_CHANGEID = 174043039;
    private ConcurrentHashMap<String, Boolean> mEvaluatedOverrides;
    ChangeListener mListener;
    private ConcurrentHashMap<String, PackageOverride> mRawOverrides;

    /* loaded from: classes.dex */
    public interface ChangeListener {
        void onCompatChange(String str);
    }

    public CompatChange(long changeId) {
        this(changeId, null, -1, -1, false, false, null, false);
    }

    public CompatChange(Change change) {
        this(change.getId(), change.getName(), change.getEnableAfterTargetSdk(), change.getEnableSinceTargetSdk(), change.getDisabled(), change.getLoggingOnly(), change.getDescription(), change.getOverridable());
    }

    public CompatChange(long changeId, String name, int enableAfterTargetSdk, int enableSinceTargetSdk, boolean disabled, boolean loggingOnly, String description, boolean overridable) {
        super(Long.valueOf(changeId), name, enableAfterTargetSdk, enableSinceTargetSdk, disabled, loggingOnly, description, overridable);
        this.mListener = null;
        this.mEvaluatedOverrides = new ConcurrentHashMap<>();
        this.mRawOverrides = new ConcurrentHashMap<>();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized void registerListener(ChangeListener listener) {
        if (this.mListener != null) {
            throw new IllegalStateException("Listener for change " + toString() + " already registered.");
        }
        this.mListener = listener;
    }

    private void addPackageOverrideInternal(String pname, boolean enabled) {
        if (getLoggingOnly()) {
            throw new IllegalArgumentException("Can't add overrides for a logging only change " + toString());
        }
        this.mEvaluatedOverrides.put(pname, Boolean.valueOf(enabled));
        notifyListener(pname);
    }

    private void removePackageOverrideInternal(String pname) {
        if (this.mEvaluatedOverrides.remove(pname) != null) {
            notifyListener(pname);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized void addPackageOverride(String packageName, PackageOverride override, OverrideAllowedState allowedState, Long versionCode) {
        if (getLoggingOnly()) {
            throw new IllegalArgumentException("Can't add overrides for a logging only change " + toString());
        }
        this.mRawOverrides.put(packageName, override);
        recheckOverride(packageName, allowedState, versionCode);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized boolean recheckOverride(String packageName, OverrideAllowedState allowedState, Long versionCode) {
        if (packageName == null) {
            return false;
        }
        boolean allowed = allowedState.state == 0;
        if (versionCode != null && this.mRawOverrides.containsKey(packageName) && allowed) {
            int overrideValue = this.mRawOverrides.get(packageName).evaluate(versionCode.longValue());
            switch (overrideValue) {
                case 0:
                    removePackageOverrideInternal(packageName);
                    break;
                case 1:
                    addPackageOverrideInternal(packageName, true);
                    break;
                case 2:
                    addPackageOverrideInternal(packageName, false);
                    break;
            }
            return true;
        }
        removePackageOverrideInternal(packageName);
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized boolean removePackageOverride(String pname, OverrideAllowedState allowedState, Long versionCode) {
        if (this.mRawOverrides.containsKey(pname)) {
            allowedState.enforce(getId(), pname);
            this.mRawOverrides.remove(pname);
            recheckOverride(pname, allowedState, versionCode);
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isEnabled(ApplicationInfo app, AndroidBuildClassifier buildClassifier) {
        Boolean enabled;
        if (app == null) {
            return defaultValue();
        }
        if (app.packageName != null && (enabled = this.mEvaluatedOverrides.get(app.packageName)) != null) {
            return enabled.booleanValue();
        }
        if (getDisabled()) {
            return false;
        }
        if (getEnableSinceTargetSdk() != -1) {
            int compareSdk = Math.min(app.targetSdkVersion, buildClassifier.platformTargetSdk());
            if (compareSdk != app.targetSdkVersion) {
                compareSdk = app.targetSdkVersion;
            }
            return compareSdk >= getEnableSinceTargetSdk();
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean willBeEnabled(String packageName) {
        if (packageName == null) {
            return defaultValue();
        }
        PackageOverride override = this.mRawOverrides.get(packageName);
        if (override != null) {
            switch (override.evaluateForAllVersions()) {
                case 0:
                    return defaultValue();
                case 1:
                    return true;
                case 2:
                    return false;
            }
        }
        return defaultValue();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean defaultValue() {
        return !getDisabled();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized void clearOverrides() {
        this.mRawOverrides.clear();
        this.mEvaluatedOverrides.clear();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized void loadOverrides(ChangeOverrides changeOverrides) {
        if (changeOverrides.getDeferred() != null) {
            for (OverrideValue override : changeOverrides.getDeferred().getOverrideValue()) {
                this.mRawOverrides.put(override.getPackageName(), new PackageOverride.Builder().setEnabled(override.getEnabled()).build());
            }
        }
        if (changeOverrides.getValidated() != null) {
            for (OverrideValue override2 : changeOverrides.getValidated().getOverrideValue()) {
                this.mEvaluatedOverrides.put(override2.getPackageName(), Boolean.valueOf(override2.getEnabled()));
                this.mRawOverrides.put(override2.getPackageName(), new PackageOverride.Builder().setEnabled(override2.getEnabled()).build());
            }
        }
        if (changeOverrides.getRaw() != null) {
            for (RawOverrideValue override3 : changeOverrides.getRaw().getRawOverrideValue()) {
                PackageOverride packageOverride = new PackageOverride.Builder().setMinVersionCode(override3.getMinVersionCode()).setMaxVersionCode(override3.getMaxVersionCode()).setEnabled(override3.getEnabled()).build();
                this.mRawOverrides.put(override3.getPackageName(), packageOverride);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized ChangeOverrides saveOverrides() {
        if (this.mRawOverrides.isEmpty()) {
            return null;
        }
        ChangeOverrides changeOverrides = new ChangeOverrides();
        changeOverrides.setChangeId(getId());
        ChangeOverrides.Raw rawOverrides = new ChangeOverrides.Raw();
        List<RawOverrideValue> rawList = rawOverrides.getRawOverrideValue();
        for (Map.Entry<String, PackageOverride> entry : this.mRawOverrides.entrySet()) {
            RawOverrideValue override = new RawOverrideValue();
            override.setPackageName(entry.getKey());
            override.setMinVersionCode(entry.getValue().getMinVersionCode());
            override.setMaxVersionCode(entry.getValue().getMaxVersionCode());
            override.setEnabled(entry.getValue().isEnabled());
            rawList.add(override);
        }
        changeOverrides.setRaw(rawOverrides);
        ChangeOverrides.Validated validatedOverrides = new ChangeOverrides.Validated();
        List<OverrideValue> validatedList = validatedOverrides.getOverrideValue();
        for (Map.Entry<String, Boolean> entry2 : this.mEvaluatedOverrides.entrySet()) {
            OverrideValue override2 = new OverrideValue();
            override2.setPackageName(entry2.getKey());
            override2.setEnabled(entry2.getValue().booleanValue());
            validatedList.add(override2);
        }
        changeOverrides.setValidated(validatedOverrides);
        return changeOverrides;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("ChangeId(").append(getId());
        if (getName() != null) {
            sb.append("; name=").append(getName());
        }
        if (getEnableSinceTargetSdk() != -1) {
            sb.append("; enableSinceTargetSdk=").append(getEnableSinceTargetSdk());
        }
        if (getDisabled()) {
            sb.append("; disabled");
        }
        if (getLoggingOnly()) {
            sb.append("; loggingOnly");
        }
        if (!this.mEvaluatedOverrides.isEmpty()) {
            sb.append("; packageOverrides=").append(this.mEvaluatedOverrides);
        }
        if (!this.mRawOverrides.isEmpty()) {
            sb.append("; rawOverrides=").append(this.mRawOverrides);
        }
        if (getOverridable()) {
            sb.append("; overridable");
        }
        return sb.append(")").toString();
    }

    private synchronized void notifyListener(String packageName) {
        ChangeListener changeListener = this.mListener;
        if (changeListener != null) {
            changeListener.onCompatChange(packageName);
        }
    }
}
