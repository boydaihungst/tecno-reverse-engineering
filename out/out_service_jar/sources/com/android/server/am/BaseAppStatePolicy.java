package com.android.server.am;

import android.provider.DeviceConfig;
import com.android.server.am.BaseAppStateTracker;
import java.io.PrintWriter;
/* loaded from: classes.dex */
public abstract class BaseAppStatePolicy<T extends BaseAppStateTracker> {
    protected final boolean mDefaultTrackerEnabled;
    protected final BaseAppStateTracker.Injector<?> mInjector;
    protected final String mKeyTrackerEnabled;
    protected final T mTracker;
    volatile boolean mTrackerEnabled;

    public abstract void onTrackerEnabled(boolean z);

    /* JADX INFO: Access modifiers changed from: package-private */
    public BaseAppStatePolicy(BaseAppStateTracker.Injector<?> injector, T tracker, String keyTrackerEnabled, boolean defaultTrackerEnabled) {
        this.mInjector = injector;
        this.mTracker = tracker;
        this.mKeyTrackerEnabled = keyTrackerEnabled;
        this.mDefaultTrackerEnabled = defaultTrackerEnabled;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateTrackerEnabled() {
        boolean enabled = DeviceConfig.getBoolean("activity_manager", this.mKeyTrackerEnabled, this.mDefaultTrackerEnabled);
        if (enabled != this.mTrackerEnabled) {
            this.mTrackerEnabled = enabled;
            onTrackerEnabled(enabled);
        }
    }

    public void onPropertiesChanged(String name) {
        if (this.mKeyTrackerEnabled.equals(name)) {
            updateTrackerEnabled();
        }
    }

    public int getProposedRestrictionLevel(String packageName, int uid, int maxLevel) {
        return 0;
    }

    public void onSystemReady() {
        updateTrackerEnabled();
    }

    public boolean isEnabled() {
        return this.mTrackerEnabled;
    }

    public int shouldExemptUid(int uid) {
        return this.mTracker.mAppRestrictionController.getBackgroundRestrictionExemptionReason(uid);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(PrintWriter pw, String prefix) {
        pw.print(prefix);
        pw.print(this.mKeyTrackerEnabled);
        pw.print('=');
        pw.println(this.mTrackerEnabled);
    }
}
