package com.android.server.usage;

import android.util.LongArrayQueue;
import java.util.Objects;
/* loaded from: classes2.dex */
class BroadcastEvent {
    private long mIdForResponseEvent;
    private int mSourceUid;
    private String mTargetPackage;
    private int mTargetUserId;
    private final LongArrayQueue mTimestampsMs = new LongArrayQueue();

    /* JADX INFO: Access modifiers changed from: package-private */
    public BroadcastEvent(int sourceUid, String targetPackage, int targetUserId, long idForResponseEvent) {
        this.mSourceUid = sourceUid;
        this.mTargetPackage = targetPackage;
        this.mTargetUserId = targetUserId;
        this.mIdForResponseEvent = idForResponseEvent;
    }

    public int getSourceUid() {
        return this.mSourceUid;
    }

    public String getTargetPackage() {
        return this.mTargetPackage;
    }

    public int getTargetUserId() {
        return this.mTargetUserId;
    }

    public long getIdForResponseEvent() {
        return this.mIdForResponseEvent;
    }

    public LongArrayQueue getTimestampsMs() {
        return this.mTimestampsMs;
    }

    public void addTimestampMs(long timestampMs) {
        this.mTimestampsMs.addLast(timestampMs);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof BroadcastEvent)) {
            return false;
        }
        BroadcastEvent other = (BroadcastEvent) obj;
        if (this.mSourceUid == other.mSourceUid && this.mIdForResponseEvent == other.mIdForResponseEvent && this.mTargetUserId == other.mTargetUserId && this.mTargetPackage.equals(other.mTargetPackage)) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(Integer.valueOf(this.mSourceUid), this.mTargetPackage, Integer.valueOf(this.mTargetUserId), Long.valueOf(this.mIdForResponseEvent));
    }

    public String toString() {
        return "BroadcastEvent {srcUid=" + this.mSourceUid + ",tgtPkg=" + this.mTargetPackage + ",tgtUser=" + this.mTargetUserId + ",id=" + this.mIdForResponseEvent + "}";
    }
}
