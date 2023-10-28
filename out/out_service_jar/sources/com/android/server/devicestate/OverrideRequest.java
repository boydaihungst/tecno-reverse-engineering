package com.android.server.devicestate;

import android.os.IBinder;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class OverrideRequest {
    private final int mFlags;
    private final int mPid;
    private final int mRequestedState;
    private final IBinder mToken;

    /* JADX INFO: Access modifiers changed from: package-private */
    public OverrideRequest(IBinder token, int pid, int requestedState, int flags) {
        this.mToken = token;
        this.mPid = pid;
        this.mRequestedState = requestedState;
        this.mFlags = flags;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public IBinder getToken() {
        return this.mToken;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getPid() {
        return this.mPid;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getRequestedState() {
        return this.mRequestedState;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getFlags() {
        return this.mFlags;
    }
}
