package com.android.server.wm;

import android.util.proto.ProtoOutputStream;
/* loaded from: classes2.dex */
public interface WindowProcessListener {
    void appDied(String str);

    void clearProfilerIfNeeded();

    void dumpDebug(ProtoOutputStream protoOutputStream, long j);

    long getCpuTime();

    boolean isCached();

    boolean isRemoved();

    void onStartActivity(int i, boolean z, String str, long j);

    void setPendingUiClean(boolean z);

    void setPendingUiCleanAndForceProcessStateUpTo(int i);

    void setRunningRemoteAnimation(boolean z);

    void updateProcessInfo(boolean z, boolean z2, boolean z3);

    void updateServiceConnectionActivities();
}
