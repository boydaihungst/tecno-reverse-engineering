package android.os.storage;

import android.os.IVold;
import java.util.List;
import java.util.Set;
/* loaded from: classes2.dex */
public abstract class StorageManagerInternal {

    /* loaded from: classes2.dex */
    public interface CloudProviderChangeListener {
        void onCloudProviderChanged(int i, String str);
    }

    /* loaded from: classes2.dex */
    public interface ResetListener {
        void onReset(IVold iVold);
    }

    public abstract void addResetListener(ResetListener resetListener);

    public abstract void freeCache(String str, long j);

    public abstract int getExternalStorageMountMode(int i, String str);

    public abstract List<String> getPrimaryVolumeIds();

    public abstract boolean hasExternalStorageAccess(int i, String str);

    public abstract boolean hasLegacyExternalStorage(int i);

    public abstract boolean isCeStoragePrepared(int i);

    public abstract boolean isExternalStorageService(int i);

    public abstract boolean isFuseMounted(int i);

    public abstract void markCeStoragePrepared(int i);

    public abstract void onAppOpsChanged(int i, int i2, String str, int i3, int i4);

    public abstract void prepareAppDataAfterInstall(String str, int i);

    public abstract boolean prepareStorageDirs(int i, Set<String> set, String str);

    public abstract void registerCloudProviderChangeListener(CloudProviderChangeListener cloudProviderChangeListener);

    public abstract void resetUser(int i);
}
