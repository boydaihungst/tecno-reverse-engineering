package com.android.server.pm.verify.domain.models;

import android.util.ArrayMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
/* loaded from: classes2.dex */
public class DomainVerificationStateMap<ValueType> {
    private static final String TAG = "DomainVerificationStateMap";
    private final ArrayMap<String, ValueType> mPackageNameMap = new ArrayMap<>();
    private final ArrayMap<UUID, ValueType> mDomainSetIdMap = new ArrayMap<>();

    public int size() {
        return this.mPackageNameMap.size();
    }

    public ValueType valueAt(int index) {
        return this.mPackageNameMap.valueAt(index);
    }

    public ValueType get(String packageName) {
        return this.mPackageNameMap.get(packageName);
    }

    public ValueType get(UUID domainSetId) {
        return this.mDomainSetIdMap.get(domainSetId);
    }

    public void put(String packageName, UUID domainSetId, ValueType valueType) {
        if (this.mPackageNameMap.containsKey(packageName)) {
            remove(packageName);
        }
        this.mPackageNameMap.put(packageName, valueType);
        this.mDomainSetIdMap.put(domainSetId, valueType);
    }

    public ValueType remove(String packageName) {
        int index;
        ValueType valueRemoved = this.mPackageNameMap.remove(packageName);
        if (valueRemoved != null && (index = this.mDomainSetIdMap.indexOfValue(valueRemoved)) >= 0) {
            this.mDomainSetIdMap.removeAt(index);
        }
        return valueRemoved;
    }

    public ValueType remove(UUID domainSetId) {
        int index;
        ValueType valueRemoved = this.mDomainSetIdMap.remove(domainSetId);
        if (valueRemoved != null && (index = this.mPackageNameMap.indexOfValue(valueRemoved)) >= 0) {
            this.mPackageNameMap.removeAt(index);
        }
        return valueRemoved;
    }

    public List<String> getPackageNames() {
        return new ArrayList(this.mPackageNameMap.keySet());
    }

    public Collection<ValueType> values() {
        return new ArrayList(this.mPackageNameMap.values());
    }

    public String toString() {
        return "DomainVerificationStateMap{packageNameMap=" + this.mPackageNameMap + ", domainSetIdMap=" + this.mDomainSetIdMap + '}';
    }
}
