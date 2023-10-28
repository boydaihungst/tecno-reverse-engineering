package com.android.server.tv.tunerresourcemanager;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
/* loaded from: classes2.dex */
public class CasResource {
    private int mAvailableSessionNum;
    private int mMaxSessionNum;
    private Map<Integer, Integer> mOwnerClientIdsToSessionNum = new HashMap();
    private final int mSystemId;

    /* JADX INFO: Access modifiers changed from: package-private */
    public CasResource(Builder builder) {
        this.mSystemId = builder.mSystemId;
        this.mMaxSessionNum = builder.mMaxSessionNum;
        this.mAvailableSessionNum = builder.mMaxSessionNum;
    }

    public int getSystemId() {
        return this.mSystemId;
    }

    public int getMaxSessionNum() {
        return this.mMaxSessionNum;
    }

    public int getUsedSessionNum() {
        return this.mMaxSessionNum - this.mAvailableSessionNum;
    }

    public boolean isFullyUsed() {
        return this.mAvailableSessionNum == 0;
    }

    public void updateMaxSessionNum(int maxSessionNum) {
        this.mAvailableSessionNum = Math.max(0, this.mAvailableSessionNum + (maxSessionNum - this.mMaxSessionNum));
        this.mMaxSessionNum = maxSessionNum;
    }

    public void setOwner(int ownerId) {
        int sessionNum = this.mOwnerClientIdsToSessionNum.get(Integer.valueOf(ownerId)) == null ? 1 : this.mOwnerClientIdsToSessionNum.get(Integer.valueOf(ownerId)).intValue() + 1;
        this.mOwnerClientIdsToSessionNum.put(Integer.valueOf(ownerId), Integer.valueOf(sessionNum));
        this.mAvailableSessionNum--;
    }

    public void removeOwner(int ownerId) {
        this.mAvailableSessionNum += this.mOwnerClientIdsToSessionNum.get(Integer.valueOf(ownerId)).intValue();
        this.mOwnerClientIdsToSessionNum.remove(Integer.valueOf(ownerId));
    }

    public Set<Integer> getOwnerClientIds() {
        return this.mOwnerClientIdsToSessionNum.keySet();
    }

    public String toString() {
        return "CasResource[systemId=" + this.mSystemId + ", isFullyUsed=" + (this.mAvailableSessionNum == 0) + ", maxSessionNum=" + this.mMaxSessionNum + ", ownerClients=" + ownersMapToString() + "]";
    }

    /* loaded from: classes2.dex */
    public static class Builder {
        protected int mMaxSessionNum;
        private int mSystemId;

        /* JADX INFO: Access modifiers changed from: package-private */
        public Builder(int systemId) {
            this.mSystemId = systemId;
        }

        public Builder maxSessionNum(int maxSessionNum) {
            this.mMaxSessionNum = maxSessionNum;
            return this;
        }

        public CasResource build() {
            CasResource cas = new CasResource(this);
            return cas;
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public String ownersMapToString() {
        StringBuilder string = new StringBuilder("{");
        for (Integer num : this.mOwnerClientIdsToSessionNum.keySet()) {
            int clienId = num.intValue();
            string.append(" clientId=").append(clienId).append(", owns session num=").append(this.mOwnerClientIdsToSessionNum.get(Integer.valueOf(clienId))).append(",");
        }
        return string.append("}").toString();
    }
}
