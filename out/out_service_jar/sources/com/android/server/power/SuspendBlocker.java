package com.android.server.power;

import android.util.proto.ProtoOutputStream;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public interface SuspendBlocker {
    void acquire();

    void acquire(String str);

    void dumpDebug(ProtoOutputStream protoOutputStream, long j);

    void release();

    void release(String str);
}
