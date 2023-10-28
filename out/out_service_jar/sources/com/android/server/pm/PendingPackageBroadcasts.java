package com.android.server.pm;

import android.util.ArrayMap;
import android.util.SparseArray;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
/* loaded from: classes2.dex */
public final class PendingPackageBroadcasts {
    private final Object mLock = new PackageManagerTracedLock();
    final SparseArray<ArrayMap<String, ArrayList<String>>> mUidMap = new SparseArray<>(2);

    public boolean hasPackage(int userId, String packageName) {
        boolean z;
        synchronized (this.mLock) {
            ArrayMap<String, ArrayList<String>> packages = this.mUidMap.get(userId);
            z = packages != null && packages.containsKey(packageName);
        }
        return z;
    }

    public void put(int userId, String packageName, ArrayList<String> components) {
        synchronized (this.mLock) {
            ArrayMap<String, ArrayList<String>> packages = getOrAllocate(userId);
            packages.put(packageName, components);
        }
    }

    public void addComponent(int userId, String packageName, String componentClassName) {
        synchronized (this.mLock) {
            ArrayList<String> components = getOrAllocate(userId, packageName);
            if (!components.contains(componentClassName)) {
                components.add(componentClassName);
            }
        }
    }

    public void addComponents(int userId, String packageName, List<String> componentClassNames) {
        synchronized (this.mLock) {
            ArrayList<String> components = getOrAllocate(userId, packageName);
            for (int index = 0; index < componentClassNames.size(); index++) {
                String componentClassName = componentClassNames.get(index);
                if (!components.contains(componentClassName)) {
                    components.add(componentClassName);
                }
            }
        }
    }

    public void remove(int userId, String packageName) {
        synchronized (this.mLock) {
            ArrayMap<String, ArrayList<String>> packages = this.mUidMap.get(userId);
            if (packages != null) {
                packages.remove(packageName);
            }
        }
    }

    public void remove(int userId) {
        synchronized (this.mLock) {
            this.mUidMap.remove(userId);
        }
    }

    public SparseArray<ArrayMap<String, ArrayList<String>>> copiedMap() {
        SparseArray<ArrayMap<String, ArrayList<String>>> copy;
        synchronized (this.mLock) {
            copy = new SparseArray<>();
            for (int userIdIndex = 0; userIdIndex < this.mUidMap.size(); userIdIndex++) {
                ArrayMap<String, ArrayList<String>> packages = this.mUidMap.valueAt(userIdIndex);
                ArrayMap<String, ArrayList<String>> packagesCopy = new ArrayMap<>();
                for (int packagesIndex = 0; packagesIndex < packages.size(); packagesIndex++) {
                    packagesCopy.put(packages.keyAt(packagesIndex), new ArrayList<>(packages.valueAt(packagesIndex)));
                }
                copy.put(this.mUidMap.keyAt(userIdIndex), packagesCopy);
            }
        }
        return copy;
    }

    public void clear() {
        synchronized (this.mLock) {
            this.mUidMap.clear();
        }
    }

    private ArrayMap<String, ArrayList<String>> getOrAllocate(int userId) {
        ArrayMap<String, ArrayList<String>> map;
        synchronized (this.mLock) {
            map = this.mUidMap.get(userId);
            if (map == null) {
                map = new ArrayMap<>();
                this.mUidMap.put(userId, map);
            }
        }
        return map;
    }

    private ArrayList<String> getOrAllocate(int userId, String packageName) {
        ArrayList<String> computeIfAbsent;
        synchronized (this.mLock) {
            ArrayMap<String, ArrayList<String>> map = this.mUidMap.get(userId);
            if (map == null) {
                map = new ArrayMap<>();
                this.mUidMap.put(userId, map);
            }
            computeIfAbsent = map.computeIfAbsent(packageName, new Function() { // from class: com.android.server.pm.PendingPackageBroadcasts$$ExternalSyntheticLambda0
                @Override // java.util.function.Function
                public final Object apply(Object obj) {
                    return PendingPackageBroadcasts.lambda$getOrAllocate$0((String) obj);
                }
            });
        }
        return computeIfAbsent;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ ArrayList lambda$getOrAllocate$0(String k) {
        return new ArrayList();
    }
}
