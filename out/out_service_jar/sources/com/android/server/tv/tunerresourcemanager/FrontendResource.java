package com.android.server.tv.tunerresourcemanager;

import com.android.server.tv.tunerresourcemanager.TunerResourceBasic;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
/* loaded from: classes2.dex */
public final class FrontendResource extends TunerResourceBasic {
    private final int mExclusiveGroupId;
    private Set<Integer> mExclusiveGroupMemberHandles;
    private final int mType;

    private FrontendResource(Builder builder) {
        super(builder);
        this.mExclusiveGroupMemberHandles = new HashSet();
        this.mType = builder.mType;
        this.mExclusiveGroupId = builder.mExclusiveGroupId;
    }

    public int getType() {
        return this.mType;
    }

    public int getExclusiveGroupId() {
        return this.mExclusiveGroupId;
    }

    public Set<Integer> getExclusiveGroupMemberFeHandles() {
        return this.mExclusiveGroupMemberHandles;
    }

    public void addExclusiveGroupMemberFeHandle(int handle) {
        this.mExclusiveGroupMemberHandles.add(Integer.valueOf(handle));
    }

    public void addExclusiveGroupMemberFeHandles(Collection<Integer> handles) {
        this.mExclusiveGroupMemberHandles.addAll(handles);
    }

    public void removeExclusiveGroupMemberFeId(int handle) {
        this.mExclusiveGroupMemberHandles.remove(Integer.valueOf(handle));
    }

    public String toString() {
        return "FrontendResource[handle=" + this.mHandle + ", type=" + this.mType + ", exclusiveGId=" + this.mExclusiveGroupId + ", exclusiveGMemeberHandles=" + this.mExclusiveGroupMemberHandles + ", isInUse=" + this.mIsInUse + ", ownerClientId=" + this.mOwnerClientId + "]";
    }

    /* loaded from: classes2.dex */
    public static class Builder extends TunerResourceBasic.Builder {
        private int mExclusiveGroupId;
        private int mType;

        /* JADX INFO: Access modifiers changed from: package-private */
        public Builder(int handle) {
            super(handle);
        }

        public Builder type(int type) {
            this.mType = type;
            return this;
        }

        public Builder exclusiveGroupId(int exclusiveGroupId) {
            this.mExclusiveGroupId = exclusiveGroupId;
            return this;
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // com.android.server.tv.tunerresourcemanager.TunerResourceBasic.Builder
        public FrontendResource build() {
            FrontendResource frontendResource = new FrontendResource(this);
            return frontendResource;
        }
    }
}
