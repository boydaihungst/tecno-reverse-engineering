package com.android.server.compat;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;
import com.android.internal.compat.AndroidBuildClassifier;
import com.android.internal.compat.IOverrideValidator;
import com.android.internal.compat.OverrideAllowedState;
/* loaded from: classes.dex */
public class OverrideValidatorImpl extends IOverrideValidator.Stub {
    private AndroidBuildClassifier mAndroidBuildClassifier;
    private CompatConfig mCompatConfig;
    private Context mContext;
    private boolean mForceNonDebuggableFinalBuild = false;

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class SettingsObserver extends ContentObserver {
        SettingsObserver() {
            super(new Handler());
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange) {
            OverrideValidatorImpl overrideValidatorImpl = OverrideValidatorImpl.this;
            overrideValidatorImpl.mForceNonDebuggableFinalBuild = Settings.Global.getInt(overrideValidatorImpl.mContext.getContentResolver(), "force_non_debuggable_final_build_for_compat", 0) == 1;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public OverrideValidatorImpl(AndroidBuildClassifier androidBuildClassifier, Context context, CompatConfig config) {
        this.mAndroidBuildClassifier = androidBuildClassifier;
        this.mContext = context;
        this.mCompatConfig = config;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public OverrideAllowedState getOverrideAllowedStateForRecheck(long changeId, String packageName) {
        return getOverrideAllowedStateInternal(changeId, packageName, true);
    }

    public OverrideAllowedState getOverrideAllowedState(long changeId, String packageName) {
        return getOverrideAllowedStateInternal(changeId, packageName, false);
    }

    private OverrideAllowedState getOverrideAllowedStateInternal(long changeId, String packageName, boolean isRecheck) {
        if (this.mCompatConfig.isLoggingOnly(changeId)) {
            return new OverrideAllowedState(5, -1, -1);
        }
        boolean debuggableBuild = this.mAndroidBuildClassifier.isDebuggableBuild() && !this.mForceNonDebuggableFinalBuild;
        boolean finalBuild = this.mAndroidBuildClassifier.isFinalBuild() || this.mForceNonDebuggableFinalBuild;
        int maxTargetSdk = this.mCompatConfig.maxTargetSdkForChangeIdOptIn(changeId);
        boolean disabled = this.mCompatConfig.isDisabled(changeId);
        if (debuggableBuild) {
            return new OverrideAllowedState(0, -1, -1);
        }
        if (maxTargetSdk >= this.mAndroidBuildClassifier.platformTargetSdk()) {
            return new OverrideAllowedState(6, -1, maxTargetSdk);
        }
        PackageManager packageManager = this.mContext.getPackageManager();
        if (packageManager != null) {
            try {
                ApplicationInfo applicationInfo = packageManager.getApplicationInfo(packageName, 4194304);
                if (this.mCompatConfig.isOverridable(changeId) && (isRecheck || this.mContext.checkCallingOrSelfPermission("android.permission.OVERRIDE_COMPAT_CHANGE_CONFIG_ON_RELEASE_BUILD") == 0)) {
                    return new OverrideAllowedState(0, -1, -1);
                }
                int appTargetSdk = applicationInfo.targetSdkVersion;
                if ((applicationInfo.flags & 2) == 0) {
                    return new OverrideAllowedState(1, -1, -1);
                }
                if (!finalBuild) {
                    return new OverrideAllowedState(0, appTargetSdk, maxTargetSdk);
                }
                if (maxTargetSdk == -1 && !disabled) {
                    return new OverrideAllowedState(2, appTargetSdk, maxTargetSdk);
                }
                if (disabled || appTargetSdk <= maxTargetSdk) {
                    return new OverrideAllowedState(0, appTargetSdk, maxTargetSdk);
                }
                return new OverrideAllowedState(3, appTargetSdk, maxTargetSdk);
            } catch (PackageManager.NameNotFoundException e) {
                return new OverrideAllowedState(4, -1, -1);
            }
        }
        throw new IllegalStateException("No PackageManager!");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void registerContentObserver() {
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("force_non_debuggable_final_build_for_compat"), false, new SettingsObserver());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void forceNonDebuggableFinalForTest(boolean value) {
        this.mForceNonDebuggableFinalBuild = value;
    }
}
