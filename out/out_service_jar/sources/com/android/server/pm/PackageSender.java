package com.android.server.pm;

import android.content.IIntentReceiver;
import android.os.Bundle;
import android.util.SparseArray;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public interface PackageSender {
    void notifyPackageAdded(String str, int i);

    void notifyPackageChanged(String str, int i);

    void notifyPackageRemoved(String str, int i);

    void sendPackageAddedForNewUsers(Computer computer, String str, boolean z, boolean z2, int i, int[] iArr, int[] iArr2, int i2);

    void sendPackageBroadcast(String str, String str2, Bundle bundle, int i, String str3, IIntentReceiver iIntentReceiver, int[] iArr, int[] iArr2, SparseArray<int[]> sparseArray, Bundle bundle2);
}
