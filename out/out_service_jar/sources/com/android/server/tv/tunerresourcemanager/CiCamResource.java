package com.android.server.tv.tunerresourcemanager;

import com.android.server.tv.tunerresourcemanager.CasResource;
/* loaded from: classes2.dex */
public final class CiCamResource extends CasResource {
    private CiCamResource(Builder builder) {
        super(builder);
    }

    @Override // com.android.server.tv.tunerresourcemanager.CasResource
    public String toString() {
        return "CiCamResource[systemId=" + getSystemId() + ", isFullyUsed=" + isFullyUsed() + ", maxSessionNum=" + getMaxSessionNum() + ", ownerClients=" + ownersMapToString() + "]";
    }

    public int getCiCamId() {
        return getSystemId();
    }

    /* loaded from: classes2.dex */
    public static class Builder extends CasResource.Builder {
        /* JADX INFO: Access modifiers changed from: package-private */
        public Builder(int systemId) {
            super(systemId);
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // com.android.server.tv.tunerresourcemanager.CasResource.Builder
        public Builder maxSessionNum(int maxSessionNum) {
            this.mMaxSessionNum = maxSessionNum;
            return this;
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // com.android.server.tv.tunerresourcemanager.CasResource.Builder
        public CiCamResource build() {
            CiCamResource ciCam = new CiCamResource(this);
            return ciCam;
        }
    }
}
