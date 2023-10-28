package com.android.server.vcn;

import android.content.Context;
import android.os.Looper;
import java.util.Objects;
/* loaded from: classes2.dex */
public class VcnContext {
    private final Context mContext;
    private final boolean mIsInTestMode;
    private final Looper mLooper;
    private final VcnNetworkProvider mVcnNetworkProvider;

    public VcnContext(Context context, Looper looper, VcnNetworkProvider vcnNetworkProvider, boolean isInTestMode) {
        this.mContext = (Context) Objects.requireNonNull(context, "Missing context");
        this.mLooper = (Looper) Objects.requireNonNull(looper, "Missing looper");
        this.mVcnNetworkProvider = (VcnNetworkProvider) Objects.requireNonNull(vcnNetworkProvider, "Missing networkProvider");
        this.mIsInTestMode = isInTestMode;
    }

    public Context getContext() {
        return this.mContext;
    }

    public Looper getLooper() {
        return this.mLooper;
    }

    public VcnNetworkProvider getVcnNetworkProvider() {
        return this.mVcnNetworkProvider;
    }

    public boolean isInTestMode() {
        return this.mIsInTestMode;
    }

    public void ensureRunningOnLooperThread() {
        if (getLooper().getThread() != Thread.currentThread()) {
            throw new IllegalStateException("Not running on VcnMgmtSvc thread");
        }
    }
}
