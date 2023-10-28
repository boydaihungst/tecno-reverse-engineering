package com.android.server.pm;

import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageChangeObserver;
import android.content.pm.IPackageManagerNative;
import android.content.pm.IStagedApexObserver;
import android.content.pm.PackageInfo;
import android.content.pm.StagedApexInfo;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Slog;
import java.util.Arrays;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public final class PackageManagerNative extends IPackageManagerNative.Stub {
    private final PackageManagerService mPm;

    /* JADX INFO: Access modifiers changed from: package-private */
    public PackageManagerNative(PackageManagerService pm) {
        this.mPm = pm;
    }

    public void registerPackageChangeObserver(IPackageChangeObserver observer) {
        synchronized (this.mPm.mPackageChangeObservers) {
            try {
                observer.asBinder().linkToDeath(new PackageChangeObserverDeathRecipient(observer), 0);
            } catch (RemoteException e) {
                Log.e("PackageManager", e.getMessage());
            }
            this.mPm.mPackageChangeObservers.add(observer);
            Log.d("PackageManager", "Size of mPackageChangeObservers after registry is " + this.mPm.mPackageChangeObservers.size());
        }
    }

    public void unregisterPackageChangeObserver(IPackageChangeObserver observer) {
        synchronized (this.mPm.mPackageChangeObservers) {
            this.mPm.mPackageChangeObservers.remove(observer);
            Log.d("PackageManager", "Size of mPackageChangeObservers after unregistry is " + this.mPm.mPackageChangeObservers.size());
        }
    }

    public String[] getAllPackages() {
        return (String[]) this.mPm.snapshotComputer().getAllPackages().toArray(new String[0]);
    }

    public String[] getNamesForUids(int[] uids) throws RemoteException {
        String[] names = null;
        String[] results = null;
        if (uids == null) {
            return null;
        }
        try {
            if (uids.length == 0) {
                return null;
            }
            names = this.mPm.snapshotComputer().getNamesForUids(uids);
            results = names != null ? names : new String[uids.length];
            for (int i = results.length - 1; i >= 0; i--) {
                if (results[i] == null) {
                    results[i] = "";
                }
            }
            return results;
        } catch (Throwable t) {
            Slog.e("PackageManager", "uids: " + Arrays.toString(uids));
            Slog.e("PackageManager", "names: " + Arrays.toString(names));
            Slog.e("PackageManager", "results: " + Arrays.toString(results));
            Slog.e("PackageManager", "throwing exception", t);
            throw t;
        }
    }

    public String getInstallerForPackage(String packageName) throws RemoteException {
        Computer snapshot = this.mPm.snapshotComputer();
        String installerName = snapshot.getInstallerPackageName(packageName);
        if (!TextUtils.isEmpty(installerName)) {
            return installerName;
        }
        int callingUser = UserHandle.getUserId(Binder.getCallingUid());
        ApplicationInfo appInfo = snapshot.getApplicationInfo(packageName, 0L, callingUser);
        if (appInfo != null && (appInfo.flags & 1) != 0) {
            return "preload";
        }
        return "";
    }

    public long getVersionCodeForPackage(String packageName) throws RemoteException {
        try {
            int callingUser = UserHandle.getUserId(Binder.getCallingUid());
            PackageInfo pInfo = this.mPm.snapshotComputer().getPackageInfo(packageName, 0L, callingUser);
            if (pInfo != null) {
                return pInfo.getLongVersionCode();
            }
        } catch (Exception e) {
        }
        return 0L;
    }

    public int getTargetSdkVersionForPackage(String packageName) throws RemoteException {
        int targetSdk = this.mPm.snapshotComputer().getTargetSdkVersion(packageName);
        if (targetSdk != -1) {
            return targetSdk;
        }
        throw new RemoteException("Couldn't get targetSdkVersion for package " + packageName);
    }

    public boolean isPackageDebuggable(String packageName) throws RemoteException {
        int callingUser = UserHandle.getCallingUserId();
        ApplicationInfo appInfo = this.mPm.snapshotComputer().getApplicationInfo(packageName, 0L, callingUser);
        if (appInfo != null) {
            return (appInfo.flags & 2) != 0;
        }
        throw new RemoteException("Couldn't get debug flag for package " + packageName);
    }

    public boolean[] isAudioPlaybackCaptureAllowed(String[] packageNames) throws RemoteException {
        int callingUser = UserHandle.getUserId(Binder.getCallingUid());
        Computer snapshot = this.mPm.snapshotComputer();
        boolean[] results = new boolean[packageNames.length];
        for (int i = results.length - 1; i >= 0; i--) {
            ApplicationInfo appInfo = snapshot.getApplicationInfo(packageNames[i], 0L, callingUser);
            results[i] = appInfo != null && appInfo.isAudioPlaybackCaptureAllowed();
        }
        return results;
    }

    public int getLocationFlags(String packageName) throws RemoteException {
        int callingUser = UserHandle.getUserId(Binder.getCallingUid());
        ApplicationInfo appInfo = this.mPm.snapshotComputer().getApplicationInfo(packageName, 0L, callingUser);
        if (appInfo != null) {
            return appInfo.isSystemApp() | (appInfo.isVendor() ? 2 : 0) | (appInfo.isProduct() ? 4 : 0);
        }
        throw new RemoteException("Couldn't get ApplicationInfo for package " + packageName);
    }

    public String getModuleMetadataPackageName() throws RemoteException {
        return this.mPm.getModuleMetadataPackageName();
    }

    public boolean hasSha256SigningCertificate(String packageName, byte[] certificate) throws RemoteException {
        return this.mPm.snapshotComputer().hasSigningCertificate(packageName, certificate, 1);
    }

    public boolean hasSystemFeature(String featureName, int version) {
        return this.mPm.hasSystemFeature(featureName, version);
    }

    public void registerStagedApexObserver(IStagedApexObserver observer) {
        this.mPm.mInstallerService.getStagingManager().registerStagedApexObserver(observer);
    }

    public void unregisterStagedApexObserver(IStagedApexObserver observer) {
        this.mPm.mInstallerService.getStagingManager().unregisterStagedApexObserver(observer);
    }

    public String[] getStagedApexModuleNames() {
        return (String[]) this.mPm.mInstallerService.getStagingManager().getStagedApexModuleNames().toArray(new String[0]);
    }

    public StagedApexInfo getStagedApexInfo(String moduleName) {
        return this.mPm.mInstallerService.getStagingManager().getStagedApexInfo(moduleName);
    }

    /* loaded from: classes2.dex */
    private final class PackageChangeObserverDeathRecipient implements IBinder.DeathRecipient {
        private final IPackageChangeObserver mObserver;

        PackageChangeObserverDeathRecipient(IPackageChangeObserver observer) {
            this.mObserver = observer;
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            synchronized (PackageManagerNative.this.mPm.mPackageChangeObservers) {
                PackageManagerNative.this.mPm.mPackageChangeObservers.remove(this.mObserver);
                Log.d("PackageManager", "Size of mPackageChangeObservers after removing dead observer is " + PackageManagerNative.this.mPm.mPackageChangeObservers.size());
            }
        }
    }
}
