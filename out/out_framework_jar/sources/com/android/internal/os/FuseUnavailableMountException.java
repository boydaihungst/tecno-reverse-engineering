package com.android.internal.os;
/* loaded from: classes4.dex */
public class FuseUnavailableMountException extends Exception {
    public FuseUnavailableMountException(int mountId) {
        super("AppFuse mount point " + mountId + " is unavailable");
    }
}
