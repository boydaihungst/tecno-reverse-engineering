package com.android.server.wm;

import android.os.IBinder;
/* loaded from: classes2.dex */
public class ActivityAssistInfo {
    private final IBinder mActivityToken;
    private final IBinder mAssistToken;
    private final int mTaskId;

    public ActivityAssistInfo(ActivityRecord activityRecord) {
        this.mActivityToken = activityRecord.token;
        this.mAssistToken = activityRecord.assistToken;
        this.mTaskId = activityRecord.getTask().mTaskId;
    }

    public IBinder getActivityToken() {
        return this.mActivityToken;
    }

    public IBinder getAssistToken() {
        return this.mAssistToken;
    }

    public int getTaskId() {
        return this.mTaskId;
    }
}
