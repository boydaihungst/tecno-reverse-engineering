package com.android.server.tv.tunerresourcemanager;
/* loaded from: classes2.dex */
public class TunerResourceBasic {
    final int mHandle;
    boolean mIsInUse;
    int mOwnerClientId = -1;

    /* JADX INFO: Access modifiers changed from: package-private */
    public TunerResourceBasic(Builder builder) {
        this.mHandle = builder.mHandle;
    }

    public int getHandle() {
        return this.mHandle;
    }

    public boolean isInUse() {
        return this.mIsInUse;
    }

    public int getOwnerClientId() {
        return this.mOwnerClientId;
    }

    public void setOwner(int ownerClientId) {
        this.mIsInUse = true;
        this.mOwnerClientId = ownerClientId;
    }

    public void removeOwner() {
        this.mIsInUse = false;
        this.mOwnerClientId = -1;
    }

    /* loaded from: classes2.dex */
    public static class Builder {
        private final int mHandle;

        /* JADX INFO: Access modifiers changed from: package-private */
        public Builder(int handle) {
            this.mHandle = handle;
        }

        public TunerResourceBasic build() {
            TunerResourceBasic resource = new TunerResourceBasic(this);
            return resource;
        }
    }
}
