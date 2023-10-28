package android.security.identity;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes3.dex */
public class CredstoreResultData extends ResultData {
    byte[] mStaticAuthenticationData = null;
    byte[] mAuthenticatedData = null;
    byte[] mMessageAuthenticationCode = null;
    private Map<String, Map<String, EntryData>> mData = new LinkedHashMap();

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes3.dex */
    public static class EntryData {
        int mStatus;
        byte[] mValue;

        EntryData(byte[] value, int status) {
            this.mValue = value;
            this.mStatus = status;
        }
    }

    CredstoreResultData() {
    }

    @Override // android.security.identity.ResultData
    public byte[] getAuthenticatedData() {
        return this.mAuthenticatedData;
    }

    @Override // android.security.identity.ResultData
    public byte[] getMessageAuthenticationCode() {
        return this.mMessageAuthenticationCode;
    }

    @Override // android.security.identity.ResultData
    public byte[] getStaticAuthenticationData() {
        return this.mStaticAuthenticationData;
    }

    @Override // android.security.identity.ResultData
    public Collection<String> getNamespaces() {
        return Collections.unmodifiableCollection(this.mData.keySet());
    }

    @Override // android.security.identity.ResultData
    public Collection<String> getEntryNames(String namespaceName) {
        Map<String, EntryData> innerMap = this.mData.get(namespaceName);
        if (innerMap == null) {
            return null;
        }
        return Collections.unmodifiableCollection(innerMap.keySet());
    }

    @Override // android.security.identity.ResultData
    public Collection<String> getRetrievedEntryNames(String namespaceName) {
        Map<String, EntryData> innerMap = this.mData.get(namespaceName);
        if (innerMap == null) {
            return null;
        }
        LinkedList<String> result = new LinkedList<>();
        for (Map.Entry<String, EntryData> entry : innerMap.entrySet()) {
            if (entry.getValue().mStatus == 0) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    private EntryData getEntryData(String namespaceName, String name) {
        Map<String, EntryData> innerMap = this.mData.get(namespaceName);
        if (innerMap == null) {
            return null;
        }
        return innerMap.get(name);
    }

    @Override // android.security.identity.ResultData
    public int getStatus(String namespaceName, String name) {
        EntryData value = getEntryData(namespaceName, name);
        if (value == null) {
            return 2;
        }
        return value.mStatus;
    }

    @Override // android.security.identity.ResultData
    public byte[] getEntry(String namespaceName, String name) {
        EntryData value = getEntryData(namespaceName, name);
        if (value == null) {
            return null;
        }
        return value.mValue;
    }

    /* loaded from: classes3.dex */
    static class Builder {
        private CredstoreResultData mResultData;

        /* JADX INFO: Access modifiers changed from: package-private */
        public Builder(byte[] staticAuthenticationData, byte[] authenticatedData, byte[] messageAuthenticationCode) {
            CredstoreResultData credstoreResultData = new CredstoreResultData();
            this.mResultData = credstoreResultData;
            credstoreResultData.mStaticAuthenticationData = staticAuthenticationData;
            this.mResultData.mAuthenticatedData = authenticatedData;
            this.mResultData.mMessageAuthenticationCode = messageAuthenticationCode;
        }

        private Map<String, EntryData> getOrCreateInnerMap(String namespaceName) {
            Map<String, EntryData> innerMap = (Map) this.mResultData.mData.get(namespaceName);
            if (innerMap == null) {
                LinkedHashMap linkedHashMap = new LinkedHashMap();
                this.mResultData.mData.put(namespaceName, linkedHashMap);
                return linkedHashMap;
            }
            return innerMap;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public Builder addEntry(String namespaceName, String name, byte[] value) {
            Map<String, EntryData> innerMap = getOrCreateInnerMap(namespaceName);
            innerMap.put(name, new EntryData(value, 0));
            return this;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public Builder addErrorStatus(String namespaceName, String name, int status) {
            Map<String, EntryData> innerMap = getOrCreateInnerMap(namespaceName);
            innerMap.put(name, new EntryData(null, status));
            return this;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public CredstoreResultData build() {
            return this.mResultData;
        }
    }
}
