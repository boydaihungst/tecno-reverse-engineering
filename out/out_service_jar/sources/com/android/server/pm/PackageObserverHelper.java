package com.android.server.pm;

import android.content.pm.PackageManagerInternal;
import android.util.ArraySet;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class PackageObserverHelper {
    private final Object mLock = new Object();
    private ArraySet<PackageManagerInternal.PackageListObserver> mActiveSnapshot = new ArraySet<>();

    public void addObserver(PackageManagerInternal.PackageListObserver observer) {
        synchronized (this.mLock) {
            ArraySet<PackageManagerInternal.PackageListObserver> set = new ArraySet<>(this.mActiveSnapshot);
            set.add(observer);
            this.mActiveSnapshot = set;
        }
    }

    public void removeObserver(PackageManagerInternal.PackageListObserver observer) {
        synchronized (this.mLock) {
            ArraySet<PackageManagerInternal.PackageListObserver> set = new ArraySet<>(this.mActiveSnapshot);
            set.remove(observer);
            this.mActiveSnapshot = set;
        }
    }

    public void notifyAdded(String packageName, int uid) {
        ArraySet<PackageManagerInternal.PackageListObserver> observers;
        synchronized (this.mLock) {
            observers = this.mActiveSnapshot;
        }
        int size = observers.size();
        for (int index = 0; index < size; index++) {
            observers.valueAt(index).onPackageAdded(packageName, uid);
        }
    }

    public void notifyChanged(String packageName, int uid) {
        ArraySet<PackageManagerInternal.PackageListObserver> observers;
        synchronized (this.mLock) {
            observers = this.mActiveSnapshot;
        }
        int size = observers.size();
        for (int index = 0; index < size; index++) {
            observers.valueAt(index).onPackageChanged(packageName, uid);
        }
    }

    public void notifyRemoved(String packageName, int uid) {
        ArraySet<PackageManagerInternal.PackageListObserver> observers;
        synchronized (this.mLock) {
            observers = this.mActiveSnapshot;
        }
        int size = observers.size();
        for (int index = 0; index < size; index++) {
            observers.valueAt(index).onPackageRemoved(packageName, uid);
        }
    }
}
