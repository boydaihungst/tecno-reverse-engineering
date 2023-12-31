package android.app.admin;

import android.content.ComponentName;
import android.content.Intent;
import android.os.UserHandle;
import java.util.List;
/* loaded from: classes.dex */
public abstract class DevicePolicyManagerInternal {

    /* loaded from: classes.dex */
    public interface OnCrossProfileWidgetProvidersChangeListener {
        void onCrossProfileWidgetProvidersChanged(int i, List<String> list);
    }

    public abstract void addOnCrossProfileWidgetProvidersChangeListener(OnCrossProfileWidgetProvidersChangeListener onCrossProfileWidgetProvidersChangeListener);

    public abstract void broadcastIntentToManifestReceivers(Intent intent, UserHandle userHandle, boolean z);

    public abstract boolean canSilentlyInstallPackage(String str, int i);

    public abstract Intent createShowAdminSupportIntent(int i, boolean z);

    public abstract Intent createUserRestrictionSupportIntent(int i, String str);

    public abstract List<String> getAllCrossProfilePackages();

    public abstract List<String> getCrossProfileWidgetProviders(int i);

    public abstract List<String> getDefaultCrossProfilePackages();

    public abstract int getDeviceOwnerUserId();

    /* JADX INFO: Access modifiers changed from: protected */
    public abstract DevicePolicyCache getDevicePolicyCache();

    /* JADX INFO: Access modifiers changed from: protected */
    public abstract DeviceStateCache getDeviceStateCache();

    public abstract CharSequence getPrintingDisabledReasonForUser(int i);

    public abstract ComponentName getProfileOwnerAsUser(int i);

    public abstract ComponentName getProfileOwnerOrDeviceOwnerSupervisionComponent(UserHandle userHandle);

    public abstract boolean isActiveDeviceOwner(int i);

    public abstract boolean isActiveProfileOwner(int i);

    public abstract boolean isActiveSupervisionApp(int i);

    public abstract boolean isDeviceOrProfileOwnerInCallingUser(String str);

    public abstract boolean isUserAffiliatedWithDevice(int i);

    public abstract void reportSeparateProfileChallengeChanged(int i);

    public abstract void resetOp(int i, String str, int i2);

    public abstract boolean supportsResetOp(int i);
}
