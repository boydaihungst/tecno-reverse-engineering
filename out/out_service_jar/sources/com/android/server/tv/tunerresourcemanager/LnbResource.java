package com.android.server.tv.tunerresourcemanager;

import com.android.server.tv.tunerresourcemanager.TunerResourceBasic;
/* loaded from: classes2.dex */
public final class LnbResource extends TunerResourceBasic {
    private LnbResource(Builder builder) {
        super(builder);
    }

    public String toString() {
        return "LnbResource[handle=" + this.mHandle + ", isInUse=" + this.mIsInUse + ", ownerClientId=" + this.mOwnerClientId + "]";
    }

    /* loaded from: classes2.dex */
    public static class Builder extends TunerResourceBasic.Builder {
        /* JADX INFO: Access modifiers changed from: package-private */
        public Builder(int handle) {
            super(handle);
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // com.android.server.tv.tunerresourcemanager.TunerResourceBasic.Builder
        public LnbResource build() {
            LnbResource lnb = new LnbResource(this);
            return lnb;
        }
    }
}
