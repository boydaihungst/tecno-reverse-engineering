package com.android.server.pm;

import android.content.pm.ChangedPackages;
import android.util.SparseArray;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
/* loaded from: classes2.dex */
class ChangedPackagesTracker {
    private int mChangedPackagesSequenceNumber;
    private final Object mLock = new Object();
    private final SparseArray<SparseArray<String>> mUserIdToSequenceToPackage = new SparseArray<>();
    private final SparseArray<Map<String, Integer>> mChangedPackagesSequenceNumbers = new SparseArray<>();

    public ChangedPackages getChangedPackages(int sequenceNumber, int userId) {
        synchronized (this.mLock) {
            if (sequenceNumber >= this.mChangedPackagesSequenceNumber) {
                return null;
            }
            SparseArray<String> changedPackages = this.mUserIdToSequenceToPackage.get(userId);
            if (changedPackages == null) {
                return null;
            }
            List<String> packageNames = new ArrayList<>(this.mChangedPackagesSequenceNumber - sequenceNumber);
            for (int i = sequenceNumber; i < this.mChangedPackagesSequenceNumber; i++) {
                String packageName = changedPackages.get(i);
                if (packageName != null) {
                    packageNames.add(packageName);
                }
            }
            return packageNames.isEmpty() ? null : new ChangedPackages(this.mChangedPackagesSequenceNumber, packageNames);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getSequenceNumber() {
        return this.mChangedPackagesSequenceNumber;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void iterateAll(BiConsumer<Integer, SparseArray<SparseArray<String>>> sequenceNumberAndValues) {
        synchronized (this.mLock) {
            sequenceNumberAndValues.accept(Integer.valueOf(this.mChangedPackagesSequenceNumber), this.mUserIdToSequenceToPackage);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateSequenceNumber(String packageName, int[] userList) {
        synchronized (this.mLock) {
            for (int i = userList.length - 1; i >= 0; i--) {
                int userId = userList[i];
                SparseArray<String> changedPackages = this.mUserIdToSequenceToPackage.get(userId);
                if (changedPackages == null) {
                    changedPackages = new SparseArray<>();
                    this.mUserIdToSequenceToPackage.put(userId, changedPackages);
                }
                Map<String, Integer> sequenceNumbers = this.mChangedPackagesSequenceNumbers.get(userId);
                if (sequenceNumbers == null) {
                    sequenceNumbers = new HashMap();
                    this.mChangedPackagesSequenceNumbers.put(userId, sequenceNumbers);
                }
                Integer sequenceNumber = sequenceNumbers.get(packageName);
                if (sequenceNumber != null) {
                    changedPackages.remove(sequenceNumber.intValue());
                }
                changedPackages.put(this.mChangedPackagesSequenceNumber, packageName);
                sequenceNumbers.put(packageName, Integer.valueOf(this.mChangedPackagesSequenceNumber));
            }
            int i2 = this.mChangedPackagesSequenceNumber;
            this.mChangedPackagesSequenceNumber = i2 + 1;
        }
    }
}
