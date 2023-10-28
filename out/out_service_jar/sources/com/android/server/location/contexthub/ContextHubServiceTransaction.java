package com.android.server.location.contexthub;

import android.hardware.location.ContextHubTransaction;
import android.hardware.location.NanoAppState;
import java.util.List;
import java.util.concurrent.TimeUnit;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public abstract class ContextHubServiceTransaction {
    private boolean mIsComplete;
    private final Long mNanoAppId;
    private final String mPackage;
    private final int mTransactionId;
    private final int mTransactionType;

    /* JADX INFO: Access modifiers changed from: package-private */
    public abstract int onTransact();

    /* JADX INFO: Access modifiers changed from: package-private */
    public ContextHubServiceTransaction(int id, int type, String packageName) {
        this.mIsComplete = false;
        this.mTransactionId = id;
        this.mTransactionType = type;
        this.mNanoAppId = null;
        this.mPackage = packageName;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ContextHubServiceTransaction(int id, int type, long nanoAppId, String packageName) {
        this.mIsComplete = false;
        this.mTransactionId = id;
        this.mTransactionType = type;
        this.mNanoAppId = Long.valueOf(nanoAppId);
        this.mPackage = packageName;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onTransactionComplete(int result) {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onQueryResponse(int result, List<NanoAppState> nanoAppStateList) {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getTransactionId() {
        return this.mTransactionId;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getTransactionType() {
        return this.mTransactionType;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getTimeout(TimeUnit unit) {
        switch (this.mTransactionType) {
            case 0:
                return unit.convert(30L, TimeUnit.SECONDS);
            default:
                return unit.convert(5L, TimeUnit.SECONDS);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setComplete() {
        this.mIsComplete = true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isComplete() {
        return this.mIsComplete;
    }

    public String toString() {
        String out = ContextHubTransaction.typeToString(this.mTransactionType, true) + " (";
        if (this.mNanoAppId != null) {
            out = out + "appId = 0x" + Long.toHexString(this.mNanoAppId.longValue()) + ", ";
        }
        return out + "package = " + this.mPackage + ")";
    }
}
