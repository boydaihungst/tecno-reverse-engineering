package com.android.server.companion;

import android.companion.AssociationInfo;
import android.net.MacAddress;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.util.CollectionUtils;
import com.android.server.companion.AssociationStore;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class AssociationStoreImpl implements AssociationStore {
    private static final boolean DEBUG = false;
    private static final String TAG = "CompanionDevice_AssociationStore";
    private final Object mLock = new Object();
    private final Map<Integer, AssociationInfo> mIdMap = new HashMap();
    private final Map<MacAddress, Set<Integer>> mAddressMap = new HashMap();
    private final SparseArray<List<AssociationInfo>> mCachedPerUser = new SparseArray<>();
    private final Set<AssociationStore.OnChangeListener> mListeners = new LinkedHashSet();

    /* JADX INFO: Access modifiers changed from: package-private */
    public void addAssociation(AssociationInfo association) {
        int id = association.getId();
        synchronized (this.mLock) {
            if (this.mIdMap.containsKey(Integer.valueOf(id))) {
                Slog.e(TAG, "Association with id " + id + " already exists.");
                return;
            }
            this.mIdMap.put(Integer.valueOf(id), association);
            MacAddress address = association.getDeviceMacAddress();
            if (address != null) {
                this.mAddressMap.computeIfAbsent(address, new Function() { // from class: com.android.server.companion.AssociationStoreImpl$$ExternalSyntheticLambda1
                    @Override // java.util.function.Function
                    public final Object apply(Object obj) {
                        return AssociationStoreImpl.lambda$addAssociation$0((MacAddress) obj);
                    }
                }).add(Integer.valueOf(id));
            }
            invalidateCacheForUserLocked(association.getUserId());
            broadcastChange(0, association);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ Set lambda$addAssociation$0(MacAddress it) {
        return new HashSet();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateAssociation(AssociationInfo updated) {
        int id = updated.getId();
        synchronized (this.mLock) {
            AssociationInfo current = this.mIdMap.get(Integer.valueOf(id));
            if (current == null) {
                return;
            }
            if (current.equals(updated)) {
                return;
            }
            this.mIdMap.put(Integer.valueOf(id), updated);
            invalidateCacheForUserLocked(current.getUserId());
            MacAddress updatedAddress = updated.getDeviceMacAddress();
            MacAddress currentAddress = current.getDeviceMacAddress();
            boolean macAddressChanged = !Objects.equals(currentAddress, updatedAddress);
            if (macAddressChanged) {
                if (currentAddress != null) {
                    this.mAddressMap.get(currentAddress).remove(Integer.valueOf(id));
                }
                if (updatedAddress != null) {
                    this.mAddressMap.computeIfAbsent(updatedAddress, new Function() { // from class: com.android.server.companion.AssociationStoreImpl$$ExternalSyntheticLambda4
                        @Override // java.util.function.Function
                        public final Object apply(Object obj) {
                            return AssociationStoreImpl.lambda$updateAssociation$1((MacAddress) obj);
                        }
                    }).add(Integer.valueOf(id));
                }
            }
            int changeType = macAddressChanged ? 2 : 3;
            broadcastChange(changeType, updated);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ Set lambda$updateAssociation$1(MacAddress it) {
        return new HashSet();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeAssociation(int id) {
        synchronized (this.mLock) {
            AssociationInfo association = this.mIdMap.remove(Integer.valueOf(id));
            if (association == null) {
                return;
            }
            MacAddress macAddress = association.getDeviceMacAddress();
            if (macAddress != null) {
                this.mAddressMap.get(macAddress).remove(Integer.valueOf(id));
            }
            invalidateCacheForUserLocked(association.getUserId());
            broadcastChange(1, association);
        }
    }

    @Override // com.android.server.companion.AssociationStore
    public Collection<AssociationInfo> getAssociations() {
        List copyOf;
        synchronized (this.mLock) {
            copyOf = List.copyOf(this.mIdMap.values());
        }
        return copyOf;
    }

    @Override // com.android.server.companion.AssociationStore
    public List<AssociationInfo> getAssociationsForUser(int userId) {
        List<AssociationInfo> associationsForUserLocked;
        synchronized (this.mLock) {
            associationsForUserLocked = getAssociationsForUserLocked(userId);
        }
        return associationsForUserLocked;
    }

    @Override // com.android.server.companion.AssociationStore
    public List<AssociationInfo> getAssociationsForPackage(int userId, final String packageName) {
        List<AssociationInfo> associationsForUser = getAssociationsForUser(userId);
        List<AssociationInfo> associationsForPackage = CollectionUtils.filter(associationsForUser, new Predicate() { // from class: com.android.server.companion.AssociationStoreImpl$$ExternalSyntheticLambda0
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                boolean equals;
                equals = ((AssociationInfo) obj).getPackageName().equals(packageName);
                return equals;
            }
        });
        return Collections.unmodifiableList(associationsForPackage);
    }

    @Override // com.android.server.companion.AssociationStore
    public AssociationInfo getAssociationsForPackageWithAddress(final int userId, final String packageName, String macAddress) {
        List<AssociationInfo> associations = getAssociationsByAddress(macAddress);
        return (AssociationInfo) CollectionUtils.find(associations, new Predicate() { // from class: com.android.server.companion.AssociationStoreImpl$$ExternalSyntheticLambda3
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                boolean belongsToPackage;
                belongsToPackage = ((AssociationInfo) obj).belongsToPackage(userId, packageName);
                return belongsToPackage;
            }
        });
    }

    @Override // com.android.server.companion.AssociationStore
    public AssociationInfo getAssociationById(int id) {
        AssociationInfo associationInfo;
        synchronized (this.mLock) {
            associationInfo = this.mIdMap.get(Integer.valueOf(id));
        }
        return associationInfo;
    }

    @Override // com.android.server.companion.AssociationStore
    public List<AssociationInfo> getAssociationsByAddress(String macAddress) {
        MacAddress address = MacAddress.fromString(macAddress);
        synchronized (this.mLock) {
            Set<Integer> ids = this.mAddressMap.get(address);
            if (ids == null) {
                return Collections.emptyList();
            }
            List<AssociationInfo> associations = new ArrayList<>(ids.size());
            for (Integer id : ids) {
                associations.add(this.mIdMap.get(id));
            }
            return Collections.unmodifiableList(associations);
        }
    }

    private List<AssociationInfo> getAssociationsForUserLocked(int userId) {
        List<AssociationInfo> cached = this.mCachedPerUser.get(userId);
        if (cached != null) {
            return cached;
        }
        List<AssociationInfo> associationsForUser = new ArrayList<>();
        for (AssociationInfo association : this.mIdMap.values()) {
            if (association.getUserId() == userId) {
                associationsForUser.add(association);
            }
        }
        List<AssociationInfo> set = Collections.unmodifiableList(associationsForUser);
        this.mCachedPerUser.set(userId, set);
        return set;
    }

    private void invalidateCacheForUserLocked(int userId) {
        this.mCachedPerUser.delete(userId);
    }

    @Override // com.android.server.companion.AssociationStore
    public void registerListener(AssociationStore.OnChangeListener listener) {
        synchronized (this.mListeners) {
            this.mListeners.add(listener);
        }
    }

    @Override // com.android.server.companion.AssociationStore
    public void unregisterListener(AssociationStore.OnChangeListener listener) {
        synchronized (this.mListeners) {
            this.mListeners.remove(listener);
        }
    }

    public void dump(PrintWriter out) {
        out.append("Companion Device Associations: ");
        if (getAssociations().isEmpty()) {
            out.append("<empty>\n");
            return;
        }
        out.append("\n");
        for (AssociationInfo a : getAssociations()) {
            out.append("  ").append((CharSequence) a.toString()).append('\n');
        }
    }

    private void broadcastChange(int changeType, AssociationInfo association) {
        synchronized (this.mListeners) {
            for (AssociationStore.OnChangeListener listener : this.mListeners) {
                listener.onAssociationChanged(changeType, association);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setAssociations(Collection<AssociationInfo> allAssociations) {
        synchronized (this.mLock) {
            setAssociationsLocked(allAssociations);
        }
    }

    private void setAssociationsLocked(Collection<AssociationInfo> associations) {
        clearLocked();
        for (AssociationInfo association : associations) {
            int id = association.getId();
            this.mIdMap.put(Integer.valueOf(id), association);
            MacAddress address = association.getDeviceMacAddress();
            if (address != null) {
                this.mAddressMap.computeIfAbsent(address, new Function() { // from class: com.android.server.companion.AssociationStoreImpl$$ExternalSyntheticLambda2
                    @Override // java.util.function.Function
                    public final Object apply(Object obj) {
                        return AssociationStoreImpl.lambda$setAssociationsLocked$5((MacAddress) obj);
                    }
                }).add(Integer.valueOf(id));
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ Set lambda$setAssociationsLocked$5(MacAddress it) {
        return new HashSet();
    }

    private void clearLocked() {
        this.mIdMap.clear();
        this.mAddressMap.clear();
        this.mCachedPerUser.clear();
    }
}
